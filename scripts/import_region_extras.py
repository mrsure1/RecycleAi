#!/usr/bin/env python3
"""Import region contacts + MOIS disposal schedules into app SQLite DBs."""

from __future__ import annotations

import argparse
import json
import sqlite3
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB_PATHS = [
    ROOT / "app/src/main/assets/wasteguide.sqlite3",
    ROOT / "data/wasteguide_dictionary.sqlite3",
]
CONTACTS_JSON = ROOT / "data/region_contacts.json"
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


def fetch_mois_for_code(api_key: str, sigungu_code: str) -> list[dict]:
    import requests

    params = f"?ServiceKey={api_key}&pageNo=1&numOfRows=100&type=JSON&sigungu_cd={sigungu_code}"
    resp = requests.get(MOIS_API + params, timeout=20)
    if resp.status_code != 200:
        return []
    data = resp.json()
    items = data.get("response", {}).get("body", {}).get("items", {}).get("item", [])
    if isinstance(items, dict):
        items = [items]
    return items if items else []


def import_mois(conn: sqlite3.Connection, api_key: str, now: str, limit: int | None, delay: float) -> int:
    import requests  # noqa: F401 — ensure dependency present

    if limit:
        sql = """
            SELECT DISTINCT region_code FROM app_region_ordinance
            WHERE region_code IS NOT NULL AND trim(region_code) != ''
            ORDER BY region_code
            LIMIT ?
        """
        codes = [r[0] for r in conn.execute(sql, (limit,)).fetchall()]
    else:
        sql = """
            SELECT DISTINCT region_code FROM app_region_ordinance
            WHERE region_code IS NOT NULL AND trim(region_code) != ''
            ORDER BY region_code
        """
        codes = [r[0] for r in conn.execute(sql).fetchall()]
    conn.execute("DELETE FROM app_mois_disposal")
    inserted = 0
    for i, code in enumerate(codes):
        print(f"[{i+1}/{len(codes)}] MOIS {code}")
        try:
            items = fetch_mois_for_code(api_key, code)
        except Exception as e:
            print(f"  skip: {e}")
            time.sleep(delay)
            continue
        for item in items:
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
                    code,
                    item.get("CTPV_NM", ""),
                    item.get("SGG_NM", ""),
                    cat,
                    dm or None,
                    dt or None,
                    now,
                ),
            )
            inserted += 1
        time.sleep(delay)
    return inserted


def patch_db(path: Path, api_key: str, fetch_mois: bool, limit: int | None, delay: float) -> None:
    if not path.exists():
        print(f"skip missing {path}")
        return
    now = utc_now()
    conn = sqlite3.connect(path)
    conn.executescript(EXTRAS_SCHEMA)
    contacts = import_contacts(conn, now)
    mois = 0
    if fetch_mois and api_key:
        mois = import_mois(conn, api_key, now, limit, delay)
    else:
        print("MOIS fetch skipped (no API key or --skip-mois)")
    conn.execute(
        "INSERT OR REPLACE INTO meta(key,value) VALUES (?,?)",
        ("region_extras_imported_at", now),
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
    print(f"{path.name}: contacts={contacts}, mois_rows={mois}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--skip-mois", action="store_true")
    parser.add_argument("--limit", type=int, default=None, help="Limit MOIS regions (debug)")
    parser.add_argument("--delay", type=float, default=0.5)
    args = parser.parse_args()
    api_key = load_api_key()
    fetch_mois = not args.skip_mois and bool(api_key)
    if not args.skip_mois and not api_key:
        print("WASTE_OPEN_API_KEY not in local.properties — contacts only")
    for db in DB_PATHS:
        patch_db(db, api_key, fetch_mois, args.limit, args.delay)


if __name__ == "__main__":
    main()
