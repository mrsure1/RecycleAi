# 📘 생활쓰레기 배출정보 Open API 통합 아키텍처 가이드

본 문서는 행정안전부 공공데이터포털에서 제공하는 **'생활쓰레기 배출정보 Open API (JSON 포맷)'**를 기존 운영/개발 중인 RecycleAI 시스템에 안정적으로 통합하기 위한 데이터베이스 아키텍처 및 연동 가이드입니다. 비전공자 및 실무 개발자 모두가 명확히 이해하고 실행할 수 있도록 설계되었습니다.

---

## 1. 테이블 분리 및 권장 스키마 (Table Schema)

기존의 회원(`users`)이나 메인 서비스 테이블과 공공데이터가 섞이면 데이터베이스 관리와 백업이 복잡해집니다. 따라서 공공데이터 전용 테이블인 `waste_disposal_rules`를 독립적으로 구축하여 **모듈화(Decoupling)**하는 것을 권장합니다.

### 📊 권장 테이블 스키마 (`waste_disposal_rules`)

| 컬럼명 (Column) | 데이터 타입 (Type) | 제약 조건 및 설명 (비전공자 맞춤 안내) |
| :--- | :--- | :--- |
| `id` | BIGINT | **PK (기본키), Auto Increment**: 각 배출 규정을 구분하는 고유 식별 번호 |
| `sido_code` | VARCHAR(10) | **시도 행정표준코드**: 예) `1100000000` (서울특별시) |
| `sigungu_code` | VARCHAR(10) | **시군구 행정표준코드 (FK 권장)**: 예) `1168000000` (강남구) - 지역 매핑의 핵심 키 |
| `sido_name` | VARCHAR(50) | 시도명: 예) 서울특별시 (API 응답 원본 백업 및 직관적 조회용) |
| `sigungu_name`| VARCHAR(50) | 시군구명: 예) 강남구 |
| `category` | VARCHAR(50) | 대분류: 예) 일반쓰레기, 음식물쓰레기, 재활용품, 대형폐기물 |
| `item_name` | VARCHAR(100) | **품목명**: 예) 페트병, 건전지, 종이팩, 형광등 |
| `disposal_method` | TEXT | **배출 방법 및 규격**: 예) 내용물을 비우고 라벨을 제거한 후 찌그러뜨려 배출 |
| `disposal_time` | VARCHAR(100) | **배출 요일 및 시간**: 예) 매주 월, 수, 금 20:00 ~ 24:00 |
| `created_at` | TIMESTAMP | 최초 적재 일시 |
| `updated_at` | TIMESTAMP | 최근 갱신 일시 |

---

## 2. 지역 데이터 연동 (공통 분모 및 매핑 팁)

지자체마다 표기가 제각각이므로 **이름만으로 조인하면 깨집니다.** RecycleAI는 두 코드 체계를 씁니다.

| 체계 | 예시 | 출처 | 앱 키 |
|------|------|------|--------|
| wasteguide 5자리 | `41281`, `11680` | `app_region_ordinance.region_code` | **런타임 조회 키** (`app_mois_disposal.sigungu_code`) |
| MOIS API 7자리 | `4128100`, `1168000` | Open API `sigungu_cd` | **import 시에만** (`data/region_mois_code_map.json`) |

빌드: `python scripts/build_region_mois_map.py` → `python scripts/import_region_extras.py`  
수동 보정: `data/region_mois_code_overrides.json`

### 🛠️ 매핑 아키텍처 다이어그램 (ASCII Art)

```
+------------------------------------+
|            users (회원)             |
+------------------------------------+
| id (PK)                            |
| name                               |
| sigungu_code (FK: '1168000000')    |
+------------------------------------+
                 |
                 | 1:N 관계 (행정표준코드로 조인)
                 v
+------------------------------------+
|    waste_disposal_rules (배출정보)  |
+------------------------------------+
| id (PK)                            |
| sigungu_code (Index: '1168000000') |
| item_name (Index: '페트병')          |
| disposal_method                    |
+------------------------------------+
```

* **RecycleAI 연동**: 앱은 GPS → Geocoder → `region_code`(5자리) → `SELECT … FROM app_mois_disposal WHERE sigungu_code = ?`. **런타임에 MOIS HTTP를 호출하지 않습니다.**

---

## 3. 데이터 최신화 (Sync) 전략 비교: TRUNCATE vs UPSERT

공공데이터는 주기적(월 1회 또는 분기 1회)으로 지자체 규정이 바뀔 때마다 업데이트됩니다. 이때 어떤 방식으로 DB를 갱신할지 명확한 판단 기준을 제시합니다.

```
+-------------------------------------------------------------------------------+
|                       공공데이터 동기화(Sync) 전략 선택 가이드                    |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [질문] 데이터 갱신 주기가 어떻게 되나요?                                        |
|    |                                                                          |
|    +---> 주 1회 / 월 1회 주기적 배치 갱신 (전체 10만 건 이하)                    |
|    |       |                                                                  |
|    |       v                                                                  |
|    |     [전략 A] 임시 테이블 적재 후 스위칭 (Blue-Green RENAME)                  |
|    |     (장점: 고스트 데이터 청소 완벽, 무중단 서비스 가능)                         |
|    |                                                                          |
|    +---> 실시간 / 일 단위 부분 갱신 (대용량 데이터)                               |
|            |                                                                  |
|            v                                                                  |
|          [전략 B] UPSERT (Insert on Duplicate Key Update)                     |
|          (장점: DB I/O 부하 최소화 / 단점: 삭제된 과거 규정이 남을 수 있음)           |
+-------------------------------------------------------------------------------+
```

### 💡 전문가 추천 전략: [임시 테이블 적재 후 스위칭 (Blue-Green 방식)]

기존 데이터를 무작정 `TRUNCATE`(전체 삭제)하고 `INSERT`하면, 데이터를 넣는 수 초~수 분 동안 사용자가 앱을 켰을 때 **"데이터가 없습니다"**라고 뜨는 서비스 장애(Downtime)가 발생합니다.

* **무중단 3단계 동기화 프로세스**:
  1. `waste_disposal_rules_temp` (임시 테이블)을 생성하고 새 API 데이터를 100% 적재합니다.
  2. 적재가 완료되면 단 0.01초 만에 테이블 이름을 맞바꿉니다:
     ```sql
     RENAME TABLE waste_disposal_rules TO waste_disposal_rules_old,
                  waste_disposal_rules_temp TO waste_disposal_rules;
     ```
  3. 구형 테이블(`_old`)을 삭제(`DROP`)합니다. 이 방식을 사용하면 **서비스 중단 없이 깨끗하게 최신 데이터로 갈아엎을 수 있으며, 지자체에서 삭제한 과거 규정(찌꺼기 데이터)도 완벽하게 청소됩니다.**

---

## 4. 인덱스 (Index) 추천 및 검색 최적화

사용자가 앱에서 가장 자주 하는 검색 패턴은 **"우리 동네(시군구)에서 특정 품목(페트병) 어떻게 버려요?"**입니다. 데이터가 수십만 건으로 늘어나도 0.01초 이내의 초고속 조회를 보장하기 위해 아래의 인덱스 설정을 강력히 권장합니다.

### ⚡ 필수 인덱스 구성

1. **복합 인덱스 (Composite Index) - 검색 속도 100배 향상**
   ```sql
   CREATE INDEX idx_sigungu_item ON waste_disposal_rules (sigungu_code, item_name);
   ```
   * **원리**: `sigungu_code`로 전국의 데이터 중 해당 구의 데이터(예: 강남구 500건)만 1차로 추려내고, 그 안에서 `item_name`(`페트병`)을 탐색하므로 데이터베이스가 전체 테이블을 뒤지는(Table Scan) 부하가 전혀 발생하지 않습니다.

2. **단일 인덱스 (Single Index) - 카테고리별 모아보기용**
   ```sql
   CREATE INDEX idx_category ON waste_disposal_rules (category);
   ```
   * **원리**: 앱의 탭 메뉴에서 "재활용품 모아보기", "대형폐기물 모아보기" 등을 누를 때 즉각적인 리스트 렌더링을 지원합니다.

---

## 5. RecycleAI DB 전략 (최종)

RecycleAI는 **서버 DB 없이** 아래 조합만 사용합니다.

| 계층 | 엔진 | 역할 |
|------|------|------|
| PC 빌드 | SQLite (`data/wasteguide_dictionary.sqlite3`) | 크롤·MOIS import·정제 |
| 앱 런타임 | SQLite (`assets/wasteguide.sqlite3`) | 품목·조례·배출 일정·문의처 **단일 진실 소스** |
| 클라우드 (선택) | Gemini API | 이미지→한글 키워드만; 카드 문구는 SQLite |

MOIS Open API는 `scripts/import_region_extras.py`가 **빌드 시** 호출하고, 결과를 `app_mois_disposal`에 넣은 뒤 앱에 번들합니다.
