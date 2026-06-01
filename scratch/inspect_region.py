import sqlite3
from pathlib import Path

c = sqlite3.connect(Path("app/src/main/assets/wasteguide.sqlite3"))
for t in ["region", "app_region_ordinance"]:
    print(t, c.execute(f"PRAGMA table_info({t})").fetchall())
    print(c.execute(f"SELECT * FROM {t} LIMIT 2").fetchall())
