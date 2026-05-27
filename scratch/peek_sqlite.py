import sqlite3
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATHS = [
    ROOT / "app/src/main/assets/wasteguide.sqlite3",
    ROOT / "data/wasteguide_dictionary.sqlite3",
]

def peek(path: Path) -> None:
    if not path.exists():
        print(f"MISSING: {path}")
        return
    print(f"\n{'='*60}")
    print(f"{path.relative_to(ROOT)}  ({path.stat().st_size / 1024 / 1024:.2f} MB)")
    print("=" * 60)
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    tables = [
        r[0]
        for r in conn.execute(
            "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
        ).fetchall()
    ]
    for t in tables:
        n = conn.execute(f"SELECT COUNT(*) FROM [{t}]").fetchone()[0]
        print(f"  {t}: {n:,} rows")
    conn.close()


def samples(path: Path) -> None:
    if not path.exists():
        return
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    print(f"\n--- Samples from {path.name} ---\n")

    for label, sql in [
        ("app_item_rule (5 rows)", """
            SELECT item_id, item_name, primary_category,
                   substr(discharge_method,1,80) AS discharge,
                   substr(app_summary,1,60) AS summary
            FROM app_item_rule ORDER BY item_name LIMIT 5
        """),
        ("app_search_keyword (10 rows)", """
            SELECT keyword, target_id, weight FROM app_search_keyword
            ORDER BY weight DESC LIMIT 10
        """),
        ("app_region_ordinance (5 rows)", """
            SELECT region_id, sido_name, sigungu_name,
                   substr(app_summary,1,100) AS summary
            FROM app_region_ordinance LIMIT 5
        """),
        ("app_common_guide", """
            SELECT guide_id, title, substr(description,1,80) AS desc
            FROM app_common_guide
        """),
        ("meta", "SELECT key, value FROM meta ORDER BY key"),
    ]:
        try:
            rows = conn.execute(sql).fetchall()
            if not rows:
                continue
            print(f"### {label}")
            cols = rows[0].keys()
            print(" | ".join(cols))
            print("-" * 72)
            for r in rows:
                print(" | ".join(str(r[c])[:50] for c in cols))
            print()
        except sqlite3.OperationalError as e:
            print(f"### {label} — skip ({e})\n")

    # Raw tables if present (pipeline DB)
    for t in ("dictionary_item", "region_ordinance"):
        try:
            n = conn.execute(f"SELECT COUNT(*) FROM [{t}]").fetchone()[0]
            if n:
                print(f"### {t} (raw, 3 rows)")
                rows = conn.execute(f"SELECT * FROM [{t}] LIMIT 3").fetchall()
                if rows:
                    cols = rows[0].keys()
                    print(" | ".join(cols))
                    for r in rows:
                        print(" | ".join(str(r[c])[:40] for c in cols))
                print()
        except sqlite3.OperationalError:
            pass

    conn.close()


def export_json(path: Path, out_path: Path) -> None:
    import json

    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    out = {
        "file": str(path.relative_to(ROOT)),
        "tables": {},
        "meta": [dict(r) for r in conn.execute("SELECT key, value FROM meta ORDER BY key")],
        "samples": {},
    }
    for t in (
        "app_item_rule",
        "app_region_ordinance",
        "app_search_keyword",
        "app_common_guide",
    ):
        try:
            out["tables"][t] = conn.execute(f"SELECT COUNT(*) FROM [{t}]").fetchone()[0]
        except sqlite3.OperationalError:
            continue
    out["samples"]["items_pet"] = [
        dict(r)
        for r in conn.execute(
            """
            SELECT item_id, item_name, primary_category, discharge_method, app_summary
            FROM app_item_rule
            WHERE item_name LIKE '%페트%' OR item_name LIKE '%플라스틱%'
            LIMIT 3
            """
        )
    ]
    out["samples"]["regions_goyang"] = [
        dict(r)
        for r in conn.execute(
            """
            SELECT region_id, sido_name, sigungu_name, region_code, app_summary
            FROM app_region_ordinance
            WHERE sigungu_name LIKE '%고양%' OR sigungu_name LIKE '%일산%'
            LIMIT 5
            """
        )
    ]
    try:
        out["samples"]["common_guide"] = [
            dict(r) for r in conn.execute("SELECT * FROM app_common_guide")
        ]
    except sqlite3.OperationalError:
        pass
    conn.close()
    out_path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    for p in PATHS:
        peek(p)
    app_db = ROOT / "app/src/main/assets/wasteguide.sqlite3"
    export_json(app_db, ROOT / "scratch/db_samples.json")
    samples(app_db)
    data_db = ROOT / "data/wasteguide_dictionary.sqlite3"
    if data_db.exists():
        samples(data_db)
