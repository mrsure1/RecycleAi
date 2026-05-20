import sqlite3

db_path = "app/src/main/assets/wasteguide.sqlite3"
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

new_discharge = (
    "· 카페 일회용컵은 재질(플라스틱 또는 종이)에 따라 구분하여 배출합니다.\n"
    "· [플라스틱 컵] : 빨대나 뚜껑을 분리하고 내용물을 물로 깨끗이 헹군 후 플라스틱 수거함으로 배출합니다.\n"
    "· [종이 컵] : 내부의 방수 코팅막으로 인해 일반 종이류와 함께 섞이면 재활용되지 않습니다. 내용물을 비우고 물로 헹군 뒤 종이팩(우유팩 등) 수거함으로 분리 배출하시고, 전용 수거함이 없다면 종량제 봉투(일반쓰레기)로 배출해야 합니다."
)

new_feature = (
    "· 카페 일회용컵과 같은 일반 페트재질의 플라스틱 용기는 무색페트병과 달리 포장재 제품에 따라 다른 플라스틱이 혼합되는 경우가 있습니다.\n"
    "· 이러한 용기가 무색페트병과 함께 재활용할 시 재생원료의 품질이 떨어질 수 있으므로 반드시 일반페트 용기와 무색페트병은 구분하여 배출하여야 합니다.\n"
    "· 일회용컵 보증금 대상 컵인 경우, 판매 매장(프랜차이즈 등)에 반납 시 보증금(300원 등)을 돌려받으실 수 있습니다."
)

new_caution = (
    "· 내용물을 깨끗이 비우고 물로 헹군 후 배출해야 합니다.\n"
    "· 플라스틱 빨대, 종이 홀더(슬리브), 비닐 덮개 등 컵 본체와 재질이 다른 부속품은 반드시 분리하여 별도로 배출하십시오."
)

cursor.execute("""
    UPDATE app_item_rule 
    SET discharge_method = ?, feature_text = ?, caution_text = ?
    WHERE item_id = '111'
""", (new_discharge, new_feature, new_caution))

conn.commit()
print("Successfully updated item 111 in SQLite Database.")
conn.close()
