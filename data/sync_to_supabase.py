import os
import requests
import json
from supabase import create_client, Client

def get_config():
    """local.properties에서 환경변수 로드"""
    config = {}
    try:
        with open("../local.properties", "r", encoding="utf-8") as f:
            for line in f:
                if "=" in line and not line.startswith("#"):
                    key, val = line.strip().split("=", 1)
                    config[key] = val.strip('"')
    except FileNotFoundError:
        print("local.properties 파일을 찾을 수 없습니다.")
    return config

def main():
    config = get_config()
    api_key = config.get("WASTE_OPEN_API_KEY")
    supabase_url = config.get("SUPABASE_URL")
    # 주의: 데이터를 쓰기(INSERT) 위해서는 보안상 anon_key가 아닌 service_role_key가 필요합니다!
    supabase_key = config.get("SUPABASE_SERVICE_ROLE_KEY") 
    
    if not api_key or not supabase_url or not supabase_key:
        print("[Error] 필수 키가 누락되었습니다.")
        return

    supabase: Client = create_client(supabase_url, supabase_key)
    print("[Success] Supabase 클라이언트 초기화 완료")

    # 1. 엑셀 파일 찾기 및 읽기
    import glob
    import pandas as pd
    import time
    
    excel_files = glob.glob("*.xlsx")
    if not excel_files:
        print("[Error] 지역코드 엑셀 파일을 찾을 수 없습니다.")
        return
        
    df = pd.read_excel(excel_files[0], header=0)
    
    # '_ALL'이 포함되지 않은 7자리 지역코드만 필터링 (시군구)
    df_sigungu = df[~df['자치단체 코드'].astype(str).str.contains('_ALL')]
    
    total_regions = len(df_sigungu)
    print(f"[Info] 총 {total_regions}개의 시군구 지역 코드를 발견했습니다. 적재 파이프라인을 가동합니다...")

    api_url = "https://apis.data.go.kr/1741000/household_waste_info/info"
    
    success_count = 0
    fail_count = 0
    
    for index, row in df_sigungu.iterrows():
        sigungu_cd = str(row['자치단체 코드']).strip()
        region_name = row['자치단체명']
        
        print(f"[{index+1}/{total_regions}] {region_name}({sigungu_cd}) 데이터 수집 중...")
        
        params = f"?ServiceKey={api_key}&pageNo=1&numOfRows=100&type=JSON&sigungu_cd={sigungu_cd}"
        
        try:
            response = requests.get(api_url + params, timeout=15)
            if response.status_code == 200:
                data = response.json()
                items = data.get("response", {}).get("body", {}).get("items", {}).get("item", [])
                
                if not items:
                    print(f"  [Skip] {region_name} 데이터 없음 (API 미제공)")
                    continue
                    
                insert_batch = []
                for item in items:
                    category = "분리배출"
                    if "FOD_WST" in item.get("LF_WST_EMSN_MTHD", "") or "음식물" in item.get("LF_WST_EMSN_MTHD", ""):
                        category = "음식물쓰레기"
                    elif "RCYCL" in item.get("RCYCL_EMSN_MTHD", ""):
                        category = "재활용품"
                    
                    insert_batch.append({
                        "sido_code": "", 
                        "sigungu_code": sigungu_cd,
                        "sido_name": item.get("CTPV_NM", ""),
                        "sigungu_name": item.get("SGG_NM", ""),
                        "category": category,
                        "item_name": "종합 안내", 
                        "disposal_method": item.get("LF_WST_EMSN_MTHD", ""),
                        "disposal_time": f"{item.get('LF_WST_EMSN_DOW', '')} {item.get('LF_WST_EMSN_BGNG_TM', '')}~{item.get('LF_WST_EMSN_END_TM', '')}",
                        "raw_api_data": item
                    })
                
                if insert_batch:
                    supabase.table("waste_disposal_rules").insert(insert_batch).execute()
                    success_count += len(insert_batch)
                    print(f"  [Success] {region_name} 데이터 {len(insert_batch)}건 적재 완료")
            else:
                print(f"  [Error] {region_name} API 호출 실패: {response.status_code}")
                fail_count += 1
                
        except Exception as e:
            print(f"  [Error] {region_name} 에러 발생: {str(e)}")
            fail_count += 1
            
        # 서버 과부하 및 차단 방지를 위한 0.5초 대기
        time.sleep(0.5)

    print(f"\n[파이프라인 종료] 총 {success_count}건의 데이터가 성공적으로 적재되었습니다. (실패 지역: {fail_count}건)")

if __name__ == "__main__":
    main()
