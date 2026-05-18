import sqlite3
import sys

# Windows 환경 콘솔 출력 인코딩 설정
sys.stdout.reconfigure(encoding='utf-8')

def check_goyang():
    db_path = "wasteguide_dictionary.sqlite3"
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    print("=== 1. 고양시 관련 조례(app_region_ordinance) 검색 ===")
    cursor.execute("""
        SELECT region_id, region_code, sido_name, sigungu_name, ordinance_title, ordinance_text, app_summary
        FROM app_region_ordinance
        WHERE sido_name LIKE '%고양%' OR sigungu_name LIKE '%고양%' OR ordinance_text LIKE '%고양%'
    """)
    rows = cursor.fetchall()
    print(f"검색된 고양시 조례 개수: {len(rows)}")
    for r in rows:
        print(f"ID: {r[0]}, Code: {r[1]}, Sido: {r[2]}, Sigungu: {r[3]}, Title: {r[4]}")
        print(f"--- Ordinance Text (상위 500자) ---")
        print(r[5][:500] if r[5] else "None")
        print(f"--- App Summary ---")
        print(r[6])
        print("="*50)
        
    print("\n=== 2. 요일, 시간 관련 키워드 검색 (app_region_ordinance 전체 또는 고양시) ===")
    cursor.execute("""
        SELECT region_id, sido_name, sigungu_name, ordinance_title
        FROM app_region_ordinance
        WHERE ordinance_text LIKE '%요일%' OR ordinance_text LIKE '%시간%' OR ordinance_text LIKE '%배출일%' OR ordinance_text LIKE '%시부터%'
    """)
    time_rows = cursor.fetchall()
    print(f"'요일', '시간' 등이 포함된 조례 개수(전체 지역 중): {len(time_rows)}")
    for r in time_rows:
        if '고양' in (r[1] or '') or '고양' in (r[2] or ''):
            print(f"[고양시 포함됨!] ID: {r[0]}, Sido: {r[1]}, Sigungu: {r[2]}, Title: {r[3]}")
        else:
            # 너무 많을 수 있으니 일부만 출력
            pass

    print("\n=== 3. app_item_rule 테이블에서 요일/시간 관련 내용 검색 ===")
    cursor.execute("""
        SELECT item_id, item_name, discharge_method, app_summary
        FROM app_item_rule
        WHERE discharge_method LIKE '%요일%' OR discharge_method LIKE '%시간%' OR discharge_method LIKE '%고양%'
    """)
    item_rows = cursor.fetchall()
    print(f"app_item_rule 중 요일/시간/고양 키워드 포함 개수: {len(item_rows)}")
    for r in item_rows[:10]: # 10개만 출력
        print(f"Item ID: {r[0]}, Name: {r[1]}")
        # 일치하는 텍스트 부분 확인
        print(f"Method snippet: {r[2][:300]}...")
        print("-" * 30)

    conn.close()

if __name__ == "__main__":
    check_goyang()
