import sqlite3
import json
from datetime import datetime

conn = sqlite3.connect('app/src/main/assets/wasteguide.sqlite3')
cursor = conn.cursor()

# Create app_common_guide table
cursor.execute("""
CREATE TABLE IF NOT EXISTS app_common_guide (
    guide_id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    subtitle TEXT,
    description TEXT NOT NULL,
    table_headers TEXT,
    table_rows TEXT,
    cta_label TEXT,
    cta_action TEXT,
    updated_at TEXT NOT NULL
);
""")

# Prepare E-순환거버넌스 data
guide_id = 'ecycle'
title = '폐가전제품 배출 방법 (무상 방문 수거)'
subtitle = '정부 운영 E-순환거버넌스'
description = "TV, 냉장고, 세탁기, 에어컨 등 폐가전제품은 정부에서 운영하는 'E-순환거버넌스'를 통해 전액 무료로 내놓으실 수 있습니다. 무겁게 집 밖으로 나를 필요 없이 수거 기사님이 집 안까지 방문하여 수거해 갑니다."

table_headers = json.dumps(["분류", "수거 기준 품목", "배출 팁"], ensure_ascii=False)
table_rows = json.dumps([
    ["단일 수거 가능", "냉장고, 세탁기, 에어컨, TV, 러닝머신, 전자레인지 등 대형 가전", "1개만 버려도 무상 방문 수거 가능"],
    ["다량 수거 가능", "PC 본체, 모니터, 노트북, 가습기, 헤어드라이어, 청소기 등 소형 가전", "5개 이상 동시에 배출할 때 방문 수거 가능"]
], ensure_ascii=False)

cta_label = '1599-0903 전화 접수 (E-순환거버넌스)'
cta_action = 'tel:15990903'
updated_at = datetime.now().isoformat()

cursor.execute("""
INSERT OR REPLACE INTO app_common_guide 
(guide_id, title, subtitle, description, table_headers, table_rows, cta_label, cta_action, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
""", (guide_id, title, subtitle, description, table_headers, table_rows, cta_label, cta_action, updated_at))

conn.commit()

# Verify insertion
cursor.execute("SELECT guide_id, title, description FROM app_common_guide WHERE guide_id = 'ecycle'")
row = cursor.fetchone()
print(f"Inserted Successfully: {row}")

conn.close()
