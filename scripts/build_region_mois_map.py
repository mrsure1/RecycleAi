#!/usr/bin/env python3
"""
Build data/region_mois_code_map.json: wasteguide region_code (5-digit) → MOIS API sigungu_cd (7-digit).

Priority: region_mois_code_overrides.json → data/*.xlsx name match → heuristic pad (5+00).
"""

from __future__ import annotations

import argparse
import json
import sqlite3
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from mois_mapping import normalize_sido, region_match_key
from mois_excel_name import excel_label_matches_ordinance

ROOT = Path(__file__).resolve().parents[1]
MAP_PATH = ROOT / "data" / "region_mois_code_map.json"
OVERRIDES_PATH = ROOT / "data" / "region_mois_code_overrides.json"
DB = ROOT / "data/wasteguide_dictionary.sqlite3"


def heuristic_mois_cd(region_code: str) -> str:
    rc = str(region_code).strip()
    if len(rc) == 5:
        return rc + "00"
    if len(rc) == 7:
        return rc
    return rc.zfill(7)


def ordinance_regions(conn: sqlite3.Connection) -> list[dict]:
    rows = conn.execute(
        """
        SELECT DISTINCT region_code, sido_name, sigungu_name
        FROM app_region_ordinance
        WHERE region_code IS NOT NULL AND trim(region_code) != ''
        ORDER BY region_code
        """
    ).fetchall()
    return [
        {"region_code": r[0], "sido_name": r[1] or "", "sigungu_name": r[2] or ""}
        for r in rows
    ]


def map_from_excel(regions: list[dict]) -> dict[str, dict] | None:
    import glob

    try:
        import pandas as pd
    except ImportError:
        return None
    files = glob.glob(str(ROOT / "data" / "*.xlsx"))
    if not files:
        return None
    df = pd.read_excel(files[0], header=0)
    if "자치단체 코드" not in df.columns or "자치단체명" not in df.columns:
        return None
    df = df[~df["자치단체 코드"].astype(str).str.contains("_ALL", na=False)]
    entries: dict[str, dict] = {}
    for reg in regions:
        rc = reg["region_code"]
        sido = reg["sido_name"]
        sigungu = reg["sigungu_name"]
        sigungu_tail = (sigungu.split()[-1] if sigungu else "").replace(" ", "")
        sido_short = normalize_sido(sido)[:2] if sido else ""
        for _, row in df.iterrows():
            name = str(row["자치단체명"]).replace(" ", "")
            code = str(row["자치단체 코드"]).strip()
            if len(code) < 7 or "_ALL" in code:
                continue
            if sigungu_tail and sigungu_tail not in name:
                continue
            if sido_short and sido_short not in name:
                continue
            entries[rc] = {
                "region_code": rc,
                "mois_sigungu_cd": code,
                "sido_name": sido,
                "sigungu_name": sigungu,
                "mapping_method": "mois_excel_name_match",
            }
            break
    return entries if entries else None


def load_overrides() -> dict[str, str]:
    if not OVERRIDES_PATH.exists():
        return {}
    data = json.loads(OVERRIDES_PATH.read_text(encoding="utf-8"))
    raw = data.get("entries", data)
    if not isinstance(raw, dict):
        return {}
    return {str(k): str(v) for k, v in raw.items() if not str(k).startswith("_")}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.parse_args()

    if not DB.exists():
        raise SystemExit(f"Missing DB: {DB}. Run finalize_app_db.py first.")

    conn = sqlite3.connect(DB)
    regions = ordinance_regions(conn)
    conn.close()

    overrides = load_overrides()
    excel_entries = map_from_excel(regions) or {}
    if excel_entries:
        print(f"Excel mapping: {len(excel_entries)} regions from data/*.xlsx")

    entries: list[dict] = []
    methods: dict[str, int] = defaultdict(int)

    for reg in regions:
        rc = reg["region_code"]
        method = "heuristic_pad_5_to_7"
        mois_cd = overrides.get(rc) or heuristic_mois_cd(rc)

        if rc in excel_entries:
            ex = excel_entries[rc]
            mois_cd = ex["mois_sigungu_cd"]
            method = ex["mapping_method"]
        elif rc in overrides:
            method = "manual_override"

        entries.append(
            {
                "region_code": rc,
                "mois_sigungu_cd": mois_cd,
                "sido_name": reg["sido_name"],
                "sigungu_name": reg["sigungu_name"],
                "mapping_method": method,
            }
        )
        methods[method] += 1

    out = {
        "version": 3,
        "source": "excel" if excel_entries else "heuristic_pad_5_to_7",
        "excel_mapped": len(excel_entries),
        "total_regions": len(regions),
        "mapped": len(entries),
        "mapping_methods": dict(methods),
        "entries": entries,
    }
    MAP_PATH.parent.mkdir(parents=True, exist_ok=True)
    MAP_PATH.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {MAP_PATH}: {len(entries)} regions, methods={dict(methods)}")


if __name__ == "__main__":
    main()
