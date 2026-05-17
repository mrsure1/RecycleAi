-- =========================================================================
-- PostgreSQL RecycleAI 통합 데이터베이스 스키마 및 초기화 스크립트
-- =========================================================================

-- 1. UUID 및 PostGIS 확장이 필요하다면 활성화 (선택 사항)
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- CREATE EXTENSION IF NOT EXISTS "postgis";

-- =========================================================================
-- 2. 회원 및 메인 서비스 테이블 (예시)
-- =========================================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    -- 회원이 주로 활동하는 지역의 시군구 코드 (행정표준코드 10자리)
    sigungu_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- 3. 생활쓰레기 배출정보 전용 테이블 (행정안전부 공공데이터 API)
-- =========================================================================
CREATE TABLE IF NOT EXISTS waste_disposal_rules (
    id BIGSERIAL PRIMARY KEY,
    sido_code VARCHAR(10) NOT NULL,       -- 시도 행정표준코드 (예: '1100000000')
    sigungu_code VARCHAR(10) NOT NULL,    -- 시군구 행정표준코드 (예: '1168000000')
    sido_name VARCHAR(50) NOT NULL,       -- 시도명
    sigungu_name VARCHAR(50) NOT NULL,    -- 시군구명
    
    category VARCHAR(50),                 -- 대분류 (일반, 음식물, 재활용 등)
    item_name VARCHAR(100) NOT NULL,      -- 품목명 (페트병, 형광등 등)
    
    disposal_method TEXT,                 -- 배출 방법 및 규격
    disposal_time VARCHAR(255),           -- 배출 요일 및 시간
    
    -- JSONB 컬럼: API에서 받은 원본 JSON 전체를 저장해두면 추후 파싱 로직 변경 시 유용함
    raw_api_data JSONB,                   
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 무중단 업데이트(Blue-Green)를 위한 임시 테이블
CREATE TABLE IF NOT EXISTS waste_disposal_rules_temp (
    LIKE waste_disposal_rules INCLUDING ALL
);

-- =========================================================================
-- 4. 필수 인덱스 생성 (초고속 검색 최적화)
-- =========================================================================

-- (1) 지역(시군구) + 품목명 복합 인덱스: "우리 동네에서 OOO 어떻게 버려요?" 검색 최적화
CREATE INDEX IF NOT EXISTS idx_rules_sigungu_item 
ON waste_disposal_rules (sigungu_code, item_name);

-- (2) 임시 테이블에도 동일한 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_rules_temp_sigungu_item 
ON waste_disposal_rules_temp (sigungu_code, item_name);

-- (3) 카테고리별 모아보기 단일 인덱스
CREATE INDEX IF NOT EXISTS idx_rules_category 
ON waste_disposal_rules (category);

CREATE INDEX IF NOT EXISTS idx_rules_temp_category 
ON waste_disposal_rules_temp (category);

-- =========================================================================
-- 5. 동기화 스위칭 프로시저 (참고용 예시)
-- 실제로는 파이썬이나 서버 백엔드 코드에서 아래 트랜잭션을 실행합니다.
-- =========================================================================
/*
BEGIN;
-- 1. _temp 테이블에 API 데이터 100% INSERT 완료 후
-- 2. 테이블 스위칭 (0.01초 소요)
ALTER TABLE waste_disposal_rules RENAME TO waste_disposal_rules_old;
ALTER TABLE waste_disposal_rules_temp RENAME TO waste_disposal_rules;
-- 3. 구형 데이터 삭제 및 새로운 temp 테이블 준비
DROP TABLE waste_disposal_rules_old;
CREATE TABLE waste_disposal_rules_temp (LIKE waste_disposal_rules INCLUDING ALL);
COMMIT;
*/
