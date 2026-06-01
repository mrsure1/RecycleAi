import sqlite3
import sys

# 표준 출력을 UTF-8로 설정
sys.stdout.reconfigure(encoding='utf-8')

db_path = "d:/MrSure/RecycleAi/app/src/main/assets/wasteguide.sqlite3"
conn = sqlite3.connect(db_path)
cur = conn.cursor()

output_file = "d:/MrSure/RecycleAi/scratch/goyang_result.txt"

with open(output_file, "w", encoding="utf-8") as f:
    f.write("=== [1] app_region_ordinance (고양시 검색) ===\n")
    cur.execute("""
        SELECT region_code, sido_name, sigungu_name, ordinance_title, app_summary 
        FROM app_region_ordinance 
        WHERE sigungu_name LIKE '%고양%' OR sido_name LIKE '%고양%'
    """)
    ordinances = cur.fetchall()
    for row in ordinances:
        f.write(f"코드: {row[0]} | 지역: {row[1]} {row[2]} | 제목: {row[3]}\n")
        if row[4]:
            f.write(f"요약: {row[4]}\n")
        f.write("-" * 50 + "\n")

    f.write("\n=== [2] app_mois_disposal (고양시 검색) ===\n")
    cur.execute("""
        SELECT sigungu_code, sido_name, sigungu_name, category, disposal_time, disposal_method 
        FROM app_mois_disposal 
        WHERE sigungu_name LIKE '%고양%' OR sido_name LIKE '%고양%'
    """)
    disposals = cur.fetchall()
    if not disposals:
        f.write("행안부 배출일정 데이터 없음\n")
    for row in disposals:
        f.write(f"코드: {row[0]} | 지역: {row[1]} {row[2]} | 분류: {row[3]}\n")
        f.write(f"배출시간: {row[4]}\n")
        f.write(f"배출방법: {row[5]}\n")
        f.write("-" * 50 + "\n")

conn.close()
print("Success")
