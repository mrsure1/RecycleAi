import re
import sqlite3
from pathlib import Path

DB = Path(__file__).resolve().parents[1] / "app/src/main/assets/wasteguide.sqlite3"
PHONE_RE = re.compile(r"(0\d{1,2}[-\s]?\d{3,4}[-\s]?\d{4}|1599[-\s]?\d{4}|1\d{3}[-\s]?\d{4})")

c = sqlite3.connect(DB)
c.row_factory = sqlite3.Row
print("=== Tables / columns (phone-related names) ===")
for (t,) in c.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"):
    for cid, name, typ, *rest in c.execute(f"PRAGMA table_info([{t}])"):
        if re.search(r"phone|tel|contact|call|fax|문의", name, re.I):
            print(f"  {t}.{name}")

print("\n=== Phone numbers found in text fields ===")
checks = [
    ("app_item_rule", ["discharge_method", "feature_text", "caution_text", "app_summary"]),
    ("app_region_ordinance", ["ordinance_text", "app_summary"]),
    ("app_common_guide", ["description", "cta_label", "cta_action"]),
    ("region_ordinance", ["ordinance_text"]),
]
by_region = {}
for table, cols in checks:
    for col in cols:
        try:
            rows = c.execute(f"SELECT * FROM [{table}]").fetchall()
            for row in rows:
                d = dict(row)
                text = str(d.get(col) or "")
                phones = PHONE_RE.findall(text)
                if phones:
                    key = (table, col)
                    print(f"\n[{table}.{col}]")
                    if table == "app_region_ordinance":
                        print(f"  region: {d.get('sido_name')} {d.get('sigungu_name')} | phones: {set(phones)}")
                        by_region.setdefault(d.get("sigungu_name"), set()).update(phones)
                    elif table == "app_item_rule":
                        print(f"  item: {d.get('item_name')} | phones: {set(phones)}")
                    elif table == "app_common_guide":
                        print(f"  guide: {d.get('guide_id')} | phones: {set(phones)} cta={d.get('cta_action')}")
                    else:
                        print(f"  phones: {set(phones)}")
        except sqlite3.OperationalError:
            pass

print(f"\n=== Regions in app_region_ordinance with explicit phone in ordinance: {len(by_region)} ===")
for sigungu, phones in sorted(by_region.items())[:15]:
    print(f"  {sigungu}: {phones}")
if len(by_region) > 15:
    print(f"  ... and {len(by_region)-15} more")

# count items with 1599
n1599 = c.execute(
    "SELECT COUNT(*) FROM app_item_rule WHERE discharge_method LIKE '%1599%' OR feature_text LIKE '%1599%'"
).fetchone()[0]
print(f"\n=== app_item_rule rows mentioning 1599: {n1599} ===")

print("\n=== Keyword hits in app_region_ordinance ===")
for pat in ["%전화%", "%문의%", "%상담%", "%콜센터%", "%환경과%"]:
    n = c.execute(
        f"SELECT COUNT(*) FROM app_region_ordinance WHERE ordinance_text LIKE ? OR app_summary LIKE ?",
        (pat, pat),
    ).fetchone()[0]
    print(f"  {pat}: {n} rows")

row = c.execute(
    "SELECT sigungu_name, ordinance_text FROM app_region_ordinance WHERE sigungu_name LIKE '%일산동구%' LIMIT 1"
).fetchone()
if row:
    text = row[1] or ""
    found = PHONE_RE.findall(text)
    print(f"  일산동구 ordinance phones in full text: {set(found) or '(none)'}")
    print(f"  '전화' in text: {'전화' in text}")

c.close()
