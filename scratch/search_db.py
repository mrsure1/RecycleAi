import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8')

db_path = "app/src/main/assets/wasteguide.sqlite3"
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

output_lines = []

# 1. '노트북' 혹은 '컴퓨터'가 포함된 품목 검색
output_lines.append("--- 노트북/컴퓨터 app_item_rule 검색 ---")
cursor.execute("SELECT item_id, item_name FROM app_item_rule WHERE item_name LIKE '%노트북%' OR item_name LIKE '%컴퓨터%' OR item_name LIKE '%가전%' LIMIT 100")
rows = cursor.fetchall()
for r in rows:
    output_lines.append(f"ID: {r[0]} | Name: {r[1]}")

output_lines.append("\n--- 노트북/컴퓨터 app_search_keyword 검색 ---")
cursor.execute("""
    SELECT k.target_id, r.item_name, k.keyword, k.weight 
    FROM app_search_keyword k
    JOIN app_item_rule r ON r.item_id = k.target_id
    WHERE k.keyword LIKE '%노트북%' OR k.keyword LIKE '%컴퓨터%' OR k.keyword LIKE '%가전%'
    LIMIT 100
""")
rows = cursor.fetchall()
for r in rows:
    output_lines.append(f"ID: {r[0]} | Item: {r[1]} | Keyword: {r[2]} | Weight: {r[3]}")

# 결과를 파일에 기록
with open("scratch/db_notebook_output.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(output_lines))

print("Done! Written to scratch/db_notebook_output.txt")
conn.close()
