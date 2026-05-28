#!/usr/bin/env python3
"""Import region contacts + MOIS disposal schedules into app SQLite DBs (offline-first)."""

from __future__ import annotations

import argparse
import json
import sqlite3
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from mois_mapping import mois_item_matches_region, region_match_key

ROOT = Path(__file__).resolve().parents[1]
DB_PATHS = [
    ROOT / "app/src/main/assets/wasteguide.sqlite3",
    ROOT / "data/wasteguide_dictionary.sqlite3",
]
CONTACTS_JSON = ROOT / "data/region_contacts.json"
MAP_PATH = ROOT / "data/region_mois_code_map.json"
LOCAL_PROPS = ROOT / "local.properties"
MOIS_API = "https://apis.data.go.kr/1741000/household_waste_info/info"

EXTRAS_SCHEMA = """
CREATE TABLE IF NOT EXISTS app_region_contact (
  region_code TEXT PRIMARY KEY,
  sigungu_name TEXT,
  dept_name TEXT NOT NULL,
  phone TEXT NOT NULL,
  tel_uri TEXT,
  source_name TEXT,
  source_url TEXT,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS app_region_mois_map (
  region_code TEXT PRIMARY KEY,
  mois_sigungu_cd TEXT NOT NULL,
  sido_name TEXT,
  sigungu_name TEXT,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS app_mois_disposal (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sigungu_code TEXT NOT NULL,
  sido_name TEXT,
  sigungu_name TEXT,
  category TEXT NOT NULL,
  disposal_method TEXT,
  disposal_time TEXT,
  source_name TEXT NOT NULL DEFAULT '행정안전부 생활쓰기물 배출정보 API',
  updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_app_mois_sigungu ON app_mois_disposal(sigungu_code);
CREATE INDEX IF NOT EXISTS idx_app_region_contact_name ON app_region_contact(sigungu_name);
"""


def utc_now() -> str:
    from datetime import datetime, timezone

    return datetime.now(timezone.utc).isoformat()


def load_api_key() -> str:
    if not LOCAL_PROPS.exists():
        return ""
    props = {}
    for line in LOCAL_PROPS.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip().strip('"')
    return props.get("WASTE_OPEN_API_KEY", "")


def load_mois_map() -> dict[str, dict]:
    if not MAP_PATH.exists():
        raise FileNotFoundError(
            f"{MAP_PATH} missing. Run: python scripts/build_region_mois_map.py"
        )
    data = json.loads(MAP_PATH.read_text(encoding="utf-8"))
    return {e["region_code"]: e for e in data.get("entries", [])}


def import_mois_map_table(conn: sqlite3.Connection, mapping: dict[str, dict], now: str) -> int:
    conn.execute("DELETE FROM app_region_mois_map")
    for rc, row in mapping.items():
        conn.execute(
            """
            INSERT INTO app_region_mois_map
            (region_code, mois_sigungu_cd, sido_name, sigungu_name, updated_at)
            VALUES (?,?,?,?,?)
            """,
            (rc, row["mois_sigungu_cd"], row.get("sido_name"), row.get("sigungu_name"), now),
        )
    return len(mapping)


def normalize_phone(phone: str) -> tuple[str, str]:
    digits = "".join(c for c in phone if c.isdigit())
    tel_uri = f"tel:{digits}" if digits else ""
    return phone.strip(), tel_uri


def import_contacts(conn: sqlite3.Connection, now: str) -> int:
    rows = json.loads(CONTACTS_JSON.read_text(encoding="utf-8"))
    conn.execute("DELETE FROM app_region_contact")
    n = 0
    for row in rows:
        phone, tel_uri = normalize_phone(row["phone"])
        conn.execute(
            """
            INSERT INTO app_region_contact
            (region_code, sigungu_name, dept_name, phone, tel_uri, source_name, source_url, updated_at)
            VALUES (?,?,?,?,?,?,?,?)
            """,
            (
                str(row["region_code"]),
                row.get("sigungu_name"),
                row["dept_name"],
                phone,
                tel_uri,
                row.get("source_name", "지자체 공식 안내"),
                row.get("source_url"),
                now,
            ),
        )
        n += 1
    return n


def mois_category(item: dict) -> str:
    lf = item.get("LF_WST_EMSN_MTHD", "") or ""
    rc = item.get("RCYCL_EMSN_MTHD", "") or ""
    if "FOD_WST" in lf or "음식물" in lf:
        return "음식물쓰레기"
    if rc or "RCYCL" in lf or "재활용" in rc:
        return "재활용품"
    return "생활폐기물"


def mois_time(item: dict) -> str:
    dow = (item.get("LF_WST_EMSN_DOW") or item.get("RCYCL_EMSN_DOW") or "").strip()
    begin = (item.get("LF_WST_EMSN_BGNG_TM") or item.get("RCYCL_EMSN_BGNG_TM") or "").strip()
    end = (item.get("LF_WST_EMSN_END_TM") or item.get("RCYCL_EMSN_END_TM") or "").strip()
    parts = [p for p in [dow, f"{begin}~{end}" if begin or end else ""] if p]
    return " ".join(parts).strip()


def mois_method(item: dict, category: str) -> str:
    if category == "재활용품":
        return (item.get("RCYCL_EMSN_MTHD") or item.get("LF_WST_EMSN_MTHD") or "").strip()
    return (item.get("LF_WST_EMSN_MTHD") or "").strip()


def fetch_mois_for_code(api_key: str, mois_sigungu_cd: str) -> list[dict]:
    import requests

    params = f"?ServiceKey={api_key}&pageNo=1&numOfRows=100&type=JSON&sigungu_cd={mois_sigungu_cd}"
    resp = requests.get(MOIS_API + params, timeout=20)
    if resp.status_code != 200:
        return []
    data = resp.json()
    items = data.get("response", {}).get("body", {}).get("items", {}).get("item", [])
    if isinstance(items, dict):
        items = [items]
    return items if items else []


def import_mois(
    conn: sqlite3.Connection,
    api_key: str,
    mapping: dict[str, dict],
    now: str,
    limit: int | None,
    delay: float,
) -> int:
    import requests  # noqa: F401

    codes = sorted(mapping.keys())
    if limit:
        codes = codes[:limit]
    conn.execute("DELETE FROM app_mois_disposal")
    inserted = 0
    skipped_region = 0
    for i, region_code in enumerate(codes):
        meta = mapping[region_code]
        mois_cd = meta["mois_sigungu_cd"]
        sido = meta.get("sido_name", "")
        sigungu = meta.get("sigungu_name", "")
        print(f"[{i+1}/{len(codes)}] MOIS {region_code} -> {mois_cd} ({sido} {sigungu})")
        try:
            items = fetch_mois_for_code(api_key, mois_cd)
        except Exception as e:
            print(f"  skip: {e}")
            time.sleep(delay)
            continue
        region_rows = 0
        for item in items:
            if not mois_item_matches_region(item, sido, sigungu):
                continue
            cat = mois_category(item)
            dt = mois_time(item)
            dm = mois_method(item, cat)
            if not dt and not dm:
                continue
            conn.execute(
                """
                INSERT INTO app_mois_disposal
                (sigungu_code, sido_name, sigungu_name, category, disposal_method, disposal_time, updated_at)
                VALUES (?,?,?,?,?,?,?)
                """,
                (
                    region_code,
                    item.get("CTPV_NM", sido),
                    item.get("SGG_NM", sigungu),
                    cat,
                    dm or None,
                    dt or None,
                    now,
                ),
            )
            inserted += 1
            region_rows += 1
        if region_rows == 0:
            skipped_region += 1
            print("  warn: no rows after region validation")
        time.sleep(delay)
    if skipped_region:
        print(f"regions with zero MOIS rows: {skipped_region}")
    return inserted


def patch_db(
    path: Path,
    api_key: str,
    mapping: dict[str, dict],
    fetch_mois: bool,
    limit: int | None,
    delay: float,
) -> None:
    if not path.exists():
        print(f"skip missing {path}")
        return
    now = utc_now()
    conn = sqlite3.connect(path)
    conn.executescript(EXTRAS_SCHEMA)
    map_rows = import_mois_map_table(conn, mapping, now)
    contacts = import_contacts(conn, now)
    mois = 0
    if fetch_mois and api_key:
        mois = import_mois(conn, api_key, mapping, now, limit, delay)
    else:
        print("MOIS API fetch skipped (no API key or --skip-mois)")
    conn.execute(
        "INSERT OR REPLACE INTO meta(key,value) VALUES (?,?)",
        ("region_extras_imported_at", now),
    )
    conn.execute(
        "INSERT OR REPLACE INTO meta(key,value) VALUES (?,?)",
        ("app_region_mois_map_count", str(map_rows)),
    )
    conn.execute(
        "INSERT OR REPLACE INTO meta(key,value) VALUES (?,?)",
        ("app_region_contact_count", str(contacts)),
    )
    conn.execute(
        "INSERT OR REPLACE INTO meta(key,value) VALUES (?,?)",
        ("app_mois_disposal_count", str(mois)),
    )
    conn.commit()
    conn.close()
    print(f"{path.name}: map={map_rows}, contacts={contacts}, mois_rows={mois}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--skip-mois", action="store_true", help="Import map+contacts only")
    parser.add_argument("--skip-map-build", action="store_true", help="Use existing JSON map")
    parser.add_argument("--limit", type=int, default=None, help="Limit MOIS regions (debug)")
    parser.add_argument("--delay", type=float, default=0.5)
    args = parser.parse_args()

    if not args.skip_map_build and not MAP_PATH.exists():
        import subprocess

        subprocess.run(
            [sys.executable, str(ROOT / "scripts/build_region_mois_map.py")],
            check=True,
            cwd=ROOT,
        )

    mapping = load_mois_map()
    if not mapping:
        raise SystemExit("region_mois_code_map.json has no entries")

    api_key = load_api_key()
    fetch_mois = not args.skip_mois and bool(api_key)
    if not args.skip_mois and not api_key:
        print("WASTE_OPEN_API_KEY not in local.properties — map+contacts only")
    for db in DB_PATHS:
        patch_db(db, api_key, mapping, fetch_mois, args.limit, args.delay)


if __name__ == "__main__":
    main()
