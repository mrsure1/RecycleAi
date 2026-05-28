# PRD — RecycleAI (리사이클AI)

| 항목 | 내용 |
|------|------|
| 제품명 | RecycleAI · 스마트 재활용 배출 가이드 |
| 상태 | Production Ready (Android) |
| 플랫폼 | Android (Jetpack Compose) |
| 관련 문서 | `problem.md`, `trd.md`, `docs/architecture.md` |

---

## 1. 배경 및 목표

### 1.1 문제

이사·출장·여행 등으로 **거주 지자체의 분리수거 규칙**이 바뀌면, 사용자는 “어디에, 무엇을, 어떻게” 버릴지 헷갈립니다. 텍스트 검색·PDF·타 지역 기준 앱은 느리고, **지금 손에 든 물건**과 **지금 서 있는 동네**에 맞지 않는 경우가 많습니다.

### 1.2 비전

카메라 스캔 + **GPS 기반 지자체 규칙** + **픽토그램 가이드**를 한 화면(핀치 줌 바텀시트)에 제공해, 분리수거 직전에 확신을 줍니다.

### 1.3 목표 (Goals)

| ID | 목표 |
|----|------|
| G-1 | GPS로 현재 시·군·구를 표시하고, 해당 지역 조례·배출 요일을 **오프라인 DB**에서 조회 |
| G-2 | ML Kit 실시간 감지 + 로컬 품목 DB 1차 매칭 + Gemini 폴백 |
| G-3 | 녹색(자동)·주황(사용자 드래그) 바운딩 박스로 직관적 조작 |
| G-4 | 카드 문구는 **SQLite `app_*`만** 사용 (LLM 원문 직접 노출 금지) |
| G-5 | 폐가전 스캔 시 E-순환 무상 수거 안내·전화 연결 |

### 1.4 비목표 (Non-Goals)

- 과태료 부과·CCTV 연동 등 행정 집행 시스템
- 앱 내 수거 기사 배차·결제
- 서버 백엔드·계정·클라우드 DB 동기화 (런타임)

---

## 2. 사용자·시나리오

### 2.1 페르소나

- 지역이 바뀐 **60대 이상** 및 일반 스마트폰 사용자
- 큰 글씨·핀치 줌·그림 안내 선호 (`problem.md` 박정호 케이스)

### 2.2 시나리오

1. **일반 재활용**: 페트병 스캔 → 초록 박스 터치 → 비우기·라벨 제거 픽토그램·배출 요일 확인  
2. **커스텀 영역**: 여러 물건 겹침 → 주황 드래그 → 「이 영역 분석」  
3. **폐가전**: 선풍기 스캔 → E-순환 무상 수거 기준·`1599-0903`  
4. **품목 정정**: 불확실 시 Clarification 칩·대화로 `item_id` 확정  
5. **접근성**: 바텀시트 핀치 줌으로 법적 고지까지 스크롤

---

## 3. 기능 요구사항

### 3.1 헤더 (Location Pill)

| ID | 요구사항 | 우선순위 |
|----|----------|----------|
| H-1 | Geocoder 기반 지자체 표시 (예: 고양시 일산서구) | P0 |
| H-2 | 위치 새로고침 | P0 |
| H-3 | 「AI 묻기」 진입 (Clarification) | P0 |
| H-4 | 글래스모피즘 다크 테마 | P1 |

### 3.2 카메라·비전

| ID | 요구사항 | 우선순위 |
|----|----------|----------|
| M-1 | CameraX 전체 화면 프리뷰 | P0 |
| M-2 | ML Kit 자동 감지 → `NeonGreen` 박스 | P0 |
| M-3 | 드래그 영역 → `NeonOrange` + 「이 영역 분석」 | P0 |
| M-4 | 로컬 DB + Gemini 하이브리드 매칭 | P0 |
| M-5 | 「다시 스캔」 | P0 |

### 3.3 바텀시트

| ID | 요구사항 | 우선순위 |
|----|----------|----------|
| B-1 | 핀치 줌·스크롤 (`Modifier.layout` 높이 보정) | P0 |
| B-2 | Idle: 사용 가이드·팁·법적 고지 | P0 |
| B-3 | 결과: `app_item_rule` + `app_region_ordinance` + `app_mois_disposal` + 문의처 | P0 |
| B-4 | 픽토그램 배출 단계 | P0 |
| B-5 | E-순환 모듈 | P0 |
| B-6 | 출처·AI 보조 표기 | P0 |

### 3.4 Clarification

| ID | 요구사항 | 우선순위 |
|----|----------|----------|
| C-1 | 낮은 신뢰도·사용자 정정 시 활성화 | P0 |
| C-2 | 후보 칩 + `app_search_keyword` 검색 | P0 |
| C-3 | 텍스트·「AI에게 설명」 | P0 |

---

## 4. UX / UI

### 4.1 디자인 토큰 (`Tokens.kt`)

| 토큰 | 값 | 용도 |
|------|-----|------|
| `RecycleGreen` | `#10B981` | 브랜드 |
| `NeonGreen` | `#7CFF6B` | AI 자동 박스 |
| `NeonOrange` | `#FF6D00` | 사용자 드래그 박스 |
| `Primary` | `#1E293B` | 다크 네이비 테마 |

### 4.2 상태

`Scanning` · `Locked` · `Uncertain` · `Clarifying` · `GuidanceReady` (구현: `AppState` / `SheetState`)

---

## 5. 데이터·콘텐츠

```text
[ PC 빌드 타임 ]
  wasteguide 크롤 → finalize_app_db.py → app_item_rule, app_region_ordinance
  build_region_mois_map.py → region_mois_code_map.json
  import_region_extras.py → app_mois_disposal, app_region_contact
        │
        ▼
  app/src/main/assets/wasteguide.sqlite3

[ 앱 런타임 ]
  SQLite만 조회 + Gemini(키워드 추출만)
```

| 테이블 | 역할 |
|--------|------|
| `app_item_rule` | 품목 카드 카피 |
| `app_region_ordinance` | 지자체 조례 |
| `app_mois_disposal` | 배출 요일·시간 (키: 5자리 `region_code`) |
| `app_region_contact` | 지자체 전화 |
| `app_search_keyword` | Clarification 검색 |
| `app_common_guide` | E-순환 등 |

---

## 6. 성공 지표

- 첫 실행 후 **첫 스캔→카드 도달** 비율
- 커스텀 드래그 사용 비율
- 폐가전 → E-순환 안내 노출 후 전화/링크 탭 비율
- 고령 사용자 핀치 줌·스크롤 완료율

---

## 7. 리스크·완화

| 리스크 | 완화 |
|--------|------|
| Gemini 한도·오프라인 | 로컬 DB 1차, 키 없으면 로컬 휴리스틱 |
| MOIS 커버리지 공백 | 조례 카드 폴백, `region_mois_code_overrides.json` |
| 지역 코드 불일치 | 엑셀·휴리스틱 5→7 매핑 + 수동 override |

---

## 8. 로드맵

1. **현재**: Android Production Ready, MOIS·조례 번들, Play 출시 준비  
2. **다음**: MOIS 전국 커버리지, 이사 모드(출신↔현재 비교), iOS 검토  
3. **장기**: 품목 DB 확장, STT, 지자체 B2G 파일럿
