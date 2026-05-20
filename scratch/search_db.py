import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8')

db_path = "app/src/main/assets/wasteguide.sqlite3"
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# 스키마 컬럼명을 확인하기 위해 PRD 구조 적용
cursor.execute("SELECT item_id, item_name, primary_category, discharge_method, feature_text, caution_text FROM app_item_rule WHERE item_id = '111'")
row = cursor.fetchone()
if row:
    print(f"ID: {row[0]}")
    print(f"Name: {row[1]}")
    print(f"Category: {row[2]}")
    print(f"Discharge Method: {row[3]}")
    print(f"Feature Text: {row[4]}")
    print(f"Caution Text: {row[5]}")
else:
    print("Item 111 not found.")

conn.close()
