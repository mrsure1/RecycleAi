import re
import sqlite3
from pathlib import Path

DB = Path(__file__).resolve().parents[1] / "app/src/main/assets/wasteguide.sqlite3"
c = sqlite3.connect(DB)
rows = c.execute(
    """
    SELECT sigungu_name, ordinance_text
    FROM app_region_ordinance
    WHERE ordinance_text LIKE '%전화%'
    LIMIT 3
    """
).fetchall()
for name, text in rows:
    print("=" * 50, name)
    for line in (text or "").splitlines():
        if "전화" in line or re.search(r"0\d", line):
            print(line[:200])
c.close()
