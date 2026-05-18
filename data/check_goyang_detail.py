import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8')

def check_goyang_detail():
    db_path = "wasteguide_dictionary.sqlite3"
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    cursor.execute("""
        SELECT region_id, sigungu_name, ordinance_text
        FROM app_region_ordinance
        WHERE sigungu_name LIKE '%고양%'
    """)
    rows = cursor.fetchall()
    
    for r in rows:
        print(f"=== {r[1]} (ID: {r[0]}) 조례 전문 분석 ===")
        text = r[2]
        lines = text.split('\n')
        print(f"총 라인 수: {len(lines)}")
        
        # 요일, 시간, 배출 관련 라인 찾기
        match_lines = [line for line in lines if any(k in line for k in ['요일', '시간', '일몰', '일출', '배출'])]
        print(f"배출/요일/시간 관련 주요 문장 (총 {len(match_lines)}개):")
        for idx, line in enumerate(match_lines):
            print(f"  {idx+1}. {line.strip()}")
            
        print("="*50)
        
    conn.close()

if __name__ == "__main__":
    check_goyang_detail()
