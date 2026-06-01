#!/usr/bin/env python3
"""UTF-8 stdout for Windows terminals. Usage: python scripts/peek_item.py 마우스 노트북"""
from __future__ import annotations

import sqlite3
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

db = sqlite3.connect(Path(__file__).resolve().parents[1] / "app/src/main/assets/wasteguide.sqlite3")
for name in sys.argv[1:] or ["마우스", "노트북"]:
    r = db.execute(
        "SELECT item_id, item_name, primary_category, discharge_method, feature_text FROM app_item_rule WHERE item_name=?",
        (name,),
    ).fetchone()
    print(name, "=>", r)
