import sqlite3
from pathlib import Path

c = sqlite3.connect(Path(__file__).resolve().parents[1] / "app/src/main/assets/wasteguide.sqlite3")
rows = c.execute(
    "SELECT region_code, sigungu_name FROM app_region_ordinance WHERE region_code IS NOT NULL LIMIT 15"
).fetchall()
print("sample", rows)
print("count", c.execute("SELECT COUNT(*) FROM app_region_ordinance").fetchone()[0])
c.close()
