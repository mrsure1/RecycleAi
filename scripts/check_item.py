import sqlite3

conn = sqlite3.connect('app/src/main/assets/wasteguide.sqlite3')
cursor = conn.cursor()

cursor.execute("SELECT item_id, item_name, primary_category FROM app_item_rule WHERE item_name LIKE '%노트북%' OR categories LIKE '%노트북%'")
rows = cursor.fetchall()

print("=== 노트북 검색 결과 ===")
for r in rows:
    print(r)

conn.close()
