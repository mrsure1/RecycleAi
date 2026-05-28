# -*- coding: utf-8 -*-
import sqlite3

DB_PATH = r'data/wasteguide_dictionary.sqlite3'
conn = sqlite3.connect(DB_PATH)
conn.row_factory = sqlite3.Row
cursor = conn.cursor()

print("=" * 60)
print("1. 카페 일회용컵 관련 app_item_rule 검색")
print("=" * 60)
cursor.execute("""
    SELECT item_id, item_name, primary_category, discharge_method, caution_text, app_summary
    FROM app_item_rule
    WHERE item_name LIKE '%일회용컵%' OR item_name LIKE '%카페%컵%'
       OR (item_name LIKE '%컵%' AND item_name LIKE '%일회용%')
    ORDER BY item_name
""")
rows = cursor.fetchall()
print(f"검색 결과: {len(rows)}건")
for r in rows:
    print(f"\n  [item_id={r['item_id']}] {r['item_name']} / 카테고리: {r['primary_category']}")
    print(f"  요약: {r['app_summary']}")
    print(f"  배출방법: {r['discharge_method'][:400] if r['discharge_method'] else '없음'}")
    print(f"  주의사항: {r['caution_text'][:200] if r['caution_text'] else '없음'}")

print()
print("=" * 60)
print("2. 제주도 지역 코드 및 MOIS 연결 코드 확인")
print("=" * 60)
cursor.execute("""
    SELECT r.region_id, r.region_name, r.region_code, m.mois_sigungu_cd, m.sigungu_name
    FROM region r
    LEFT JOIN app_region_mois_map m ON r.region_code = m.region_code
    WHERE r.region_name LIKE '%제주%'
    ORDER BY r.region_name
""")
rows = cursor.fetchall()
for r in rows:
    print(f"  [{r['region_id']}] {r['region_name']} (지역코드:{r['region_code']}, MOIS코드:{r['mois_sigungu_cd']}, MOIS이름:{r['sigungu_name']})")

print()
print("=" * 60)
print("3. 제주도 MOIS 배출정보 전체 카테고리 목록")
print("=" * 60)
cursor.execute("""
    SELECT DISTINCT m.sigungu_name, m.category, m.disposal_time
    FROM app_mois_disposal m
    WHERE m.sido_name LIKE '%제주%'
    ORDER BY m.sigungu_name, m.category
""")
rows = cursor.fetchall()
print(f"전체: {len(rows)}건")
for r in rows:
    print(f"  {r['sigungu_name']} | {r['category']} | {r['disposal_time']}")

print()
print("=" * 60)
print("4. 제주도 MOIS 배출정보 상세 (플라스틱, 종이 관련)")
print("=" * 60)
cursor.execute("""
    SELECT m.sido_name, m.sigungu_name, m.category, m.disposal_method, m.disposal_time
    FROM app_mois_disposal m
    WHERE m.sido_name LIKE '%제주%'
      AND (m.category LIKE '%플라스틱%' OR m.category LIKE '%종이%' OR m.category LIKE '%컵%'
           OR m.category LIKE '%음료%' OR m.category LIKE '%일회용%')
    ORDER BY m.sigungu_name, m.category
""")
rows = cursor.fetchall()
print(f"검색 결과: {len(rows)}건")
for r in rows:
    print(f"\n  [{r['sido_name']} {r['sigungu_name']}] 분류: {r['category']}")
    print(f"  배출방법: {r['disposal_method']}")
    print(f"  배출시간: {r['disposal_time']}")

print()
print("=" * 60)
print("5. 제주도 조례(ordinance) 전체 내용")
print("=" * 60)
cursor.execute("""
    SELECT region_id, sido_name, sigungu_name, app_summary, ordinance_text
    FROM app_region_ordinance
    WHERE sido_name LIKE '%제주%'
    ORDER BY sigungu_name
""")
rows = cursor.fetchall()
for r in rows:
    print(f"\n  [{r['region_id']}] {r['sido_name']} {r['sigungu_name']}")
    print(f"  조례 요약: {r['app_summary'][:500] if r['app_summary'] else '없음'}")
    print(f"  조례 원문(500자): {r['ordinance_text'][:500] if r['ordinance_text'] else '없음'}")

conn.close()
