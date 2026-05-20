-- =========================================================================
-- Supabase pgvector 활성화 및 고속 하이브리드 벡터 검색 스키마 설정
-- =========================================================================

-- 1. pgvector 확장 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. 기존의 품목 룰 테이블에 512차원(CLIP 표준) 임베딩 컬럼 추가
ALTER TABLE waste_disposal_rules 
ADD COLUMN IF NOT EXISTS embedding vector(512);

-- 3. 코사인 유사도 검색을 위한 Stored Procedure(RPC) 정의
-- 사용자가 전송한 이미지 임베딩 벡터와 데이터베이스에 저장된 품목 임베딩 간의 유사도를 계산하여
-- 해당 시군구 조례에 맞춰 필터링한 뒤 코사인 유사도 순서로 상위 N개 결과를 리턴합니다.
CREATE OR REPLACE FUNCTION match_waste_items (
  query_embedding vector(512),
  match_threshold float,
  match_count int,
  user_sigungu_code varchar(10)
)
RETURNS TABLE (
  id bigint,
  item_name varchar,
  category varchar,
  disposal_method text,
  disposal_time varchar,
  similarity float
)
LANGUAGE plpgsql
AS $$
BEGIN
  RETURN QUERY
  SELECT
    w.id,
    w.item_name,
    w.category,
    w.disposal_method,
    w.disposal_time,
    -- (1 - 코사인 거리) 연산으로 코사인 유사도(Cosine Similarity)를 구합니다.
    1 - (w.embedding <=> query_embedding) AS similarity
  FROM waste_disposal_rules w
  WHERE w.sigungu_code = user_sigungu_code
    AND 1 - (w.embedding <=> query_embedding) > match_threshold
  ORDER BY w.embedding <=> query_embedding ASC -- 거리가 가장 짧은(유사도가 가장 높은) 순서대로 정렬
  LIMIT match_count;
END;
$$;
