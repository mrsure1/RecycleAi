import os
import sys
import torch
from transformers import AutoTokenizer, CLIPTextModelWithProjection
from supabase import create_client, Client

def get_config():
    """local.properties에서 환경변수 로드"""
    config = {}
    try:
        # 상위 디렉터리 또는 현재 디렉터리에서 local.properties 검색
        paths = ["../local.properties", "local.properties"]
        for p in paths:
            if os.path.exists(p):
                with open(p, "r", encoding="utf-8") as f:
                    for line in f:
                        if "=" in line and not line.startswith("#"):
                            key, val = line.strip().split("=", 1)
                            config[key] = val.strip('"')
                break
    except Exception as e:
        print("local.properties 로드 실패:", e)
    return config

def main():
    config = get_config()
    supabase_url = config.get("SUPABASE_URL")
    supabase_key = config.get("SUPABASE_SERVICE_ROLE_KEY") # INSERT/UPDATE를 위해 service_role 키 사용

    if not supabase_url or not supabase_key:
        print("[Error] Supabase 환경 변수가 누락되었습니다. local.properties를 확인하세요.")
        return

    print("[Info] Supabase 클라이언트 초기화 중...")
    supabase: Client = create_client(supabase_url, supabase_key)

    # 1. CLIP 멀티모달 임베딩 텍스트 인코더 모델 로드
    # Xenova/clip-vit-base-patch32와 완벽 호환되는 512차원 표준 모델 사용
    model_name = "openai/clip-vit-base-patch32"
    print(f"[Info] CLIP Text Encoder 모델({model_name}) 로드 중... (최초 로드 시 다운로드에 수 분이 걸릴 수 있습니다.)")
    
    try:
        tokenizer = AutoTokenizer.from_pretrained(model_name)
        model = CLIPTextModelWithProjection.from_pretrained(model_name)
    except Exception as e:
        print(f"[Error] 모델 로드에 실패했습니다: {e}")
        print("💡 팁: pip install transformers torch 가 설치되어 있는지 확인하세요.")
        return

    # 2. 임베딩이 비어 있는 데이터 가져오기
    print("[Info] 임베딩이 필요한 데이터 조회 중...")
    try:
        # embedding 컬럼이 null인 룰 행들 조회
        response = supabase.table("waste_disposal_rules")\
            .select("id, item_name, category, disposal_method")\
            .is_("embedding", "null")\
            .execute()
        
        rows = response.data
        if not rows:
            print("[Success] 모든 데이터의 임베딩이 이미 채워져 있습니다. 작업을 스킵합니다.")
            return
        
        print(f"[Info] 총 {len(rows)}개의 아이템에 대해 임베딩을 생성합니다.")
    except Exception as e:
        print(f"[Error] 데이터 조회 실패: {e}")
        return

    # 3. 데이터 루프를 돌며 임베딩 생성 및 업데이트
    success_count = 0
    for i, row in enumerate(rows):
        row_id = row["id"]
        item_name = row["item_name"]
        category = row["category"] or ""
        method = row["disposal_method"] or ""

        # 임베딩할 문장 구성 (품목명과 세부 배출 방법을 융합하여 의미를 풍부하게 함)
        text_content = f"품목: {item_name}. 분류: {category}. 배출 가이드: {method}"
        
        try:
            # 텍스트 토큰화 및 임베딩 추출
            inputs = tokenizer(text_content, padding=True, truncation=True, return_tensors="pt")
            with torch.no_grad():
                outputs = model(**inputs)
                # 512차원 정규화된 텍스트 임베딩 벡터 추출 (projection layer 통과됨)
                text_embeds = outputs.text_embeds[0]
                # 단위 벡터로 정규화 (코사인 유사도 연산 최적화)
                text_embeds = text_embeds / text_embeds.norm(p=2, keepdim=True)
                vector_list = text_embeds.tolist()

            # Supabase DB 업데이트
            supabase.table("waste_disposal_rules")\
                .update({"embedding": vector_list})\
                .eq("id", row_id)\
                .execute()

            success_count += 1
            if success_count % 10 == 0 or i == len(rows) - 1:
                print(f"[{i+1}/{len(rows)}] '{item_name}' 임베딩 생성 및 DB 저장 완료 ({success_count}건 성공)")

        except Exception as e:
            print(f"[Error] '{item_name}'(ID: {row_id}) 처리 중 에러 발생: {e}")

    print(f"\n[작업 완료] 총 {success_count}건의 임베딩 업데이트가 성공적으로 마무리되었습니다.")

if __name__ == "__main__":
    main()
