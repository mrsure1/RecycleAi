# TRD — RecycleAI (Technical Requirements)

| 항목 | 내용 |
|------|------|
| 버전 | Production Ready |
| PRD | `PRD.md` |
| 갱신 | 2026-05-27 |

---

## 1. 아키텍처 요약

**온디바이스 SQLite + CameraX + ML Kit + Geocoder + Gemini + (빌드 타임) MOIS Open API**

- 앱은 **서버·Supabase·PostgREST 없음**
- 카드 SSOT: 번들 SQLite `app_*`
- Gemini: 이미지 → 한글 키워드만; 표시 문구는 DB Grounding

---

## 2. 클라이언트 (Android)

### 2.1 모듈 구조

| 패키지 | 책임 |
|--------|------|
| `app.trashai.vision` | CameraX, ML Kit, Overlay, 바운딩 박스 |
| `app.trashai.ui` | Compose UI, CardSheet, InfoSheet, Tokens |
| `app.trashai.data` | `WasteGuideDb`, `RegionExtrasLoader` |
| `app.trashai.gemini` | `GeminiClient` |
| `app.trashai.supabase` | **레거시 패키지명** — `SupabaseVectorClient`는 Gemini/로컬 휴리스틱 래퍼만 제공 |
| `app.trashai` | `AppState`, `MainActivity`, Clarification |

### 2.2 비전 파이프라인

1. CameraX 프레임 → ML Kit Object Detection (바운딩 박스)  
2. 탭/드래그 → JPEG 크롭  
3. `TrashAiConfig.USE_LOCAL_VECTOR_SEARCH == true` → ML Kit 라벨 휴리스틱  
4. else → `GeminiClient.classifyTrashKeywords` → `app_search_keyword` / 품목 조회  
5. `CardComposer` 경로로 `app_item_rule` + 지역 extras 조합

### 2.3 제스처·UX 가드레일

- Touch slop으로 탭/드래그 분기  
- 32px 미만 드래그 자동 취소  
- 핀치 줌: `transformable` + `Modifier.layout` 동적 높이

### 2.4 의존성 (핵심)

- `androidx.camera:*`
- `com.google.mlkit:object-detection`
- OkHttp + Kotlin Serialization (Gemini REST)
- SQLite (Room 없이 raw/API 래퍼 `WasteGuideDb`)

### 2.5 빌드 설정

`local.properties`:

```properties
GEMINI_API_KEY=...
```

`BuildConfig.GEMINI_API_KEY`만 앱에 주입. **SUPABASE_* 필드 없음.**

---

## 3. 데이터·빌드 파이프라인 (PC)

### 3.1 스크립트

| 스크립트 | 역할 |
|----------|------|
| `complete_wasteguide_db.py` | 크롤 오케스트레이션 |
| `wasteguide_crawler.py` | 품목사전 |
| `wasteguide_region_law_crawler.py` | 지자체 조례 |
| `finalize_app_db.py` | `app_*` 테이블 생성 |
| `build_region_mois_map.py` | 5↔7 지역 코드 매핑 JSON |
| `import_region_extras.py` | MOIS API → `app_mois_disposal`, 문의처 |

### 3.2 MOIS 연동

- API: `https://apis.data.go.kr/1741000/household_waste_info/info`
- 키: `WASTE_OPEN_API_KEY` (`local.properties`, **파이프라인 전용**)
- 매핑: `data/region_mois_code_map.json` (엑셀 `data/*.xlsx`, 휴리스틱 `5자리+00`, `region_mois_code_overrides.json`)
- 적재 키: `app_mois_disposal.sigungu_code` = wasteguide **5자리** `region_code`

### 3.3 SQLite (앱 번들)

경로: `app/src/main/assets/wasteguide.sqlite3`

런타임 복사 후 `WasteGuideDb.open(context)`로 읽기 전용 조회.

---

## 4. 데이터 플로우

```text
[카메라]
   → ML Kit 박스
   → 크롭 JPEG
   → (로컬 키워드 | Gemini 키워드)
   → item_id / 검색 후보
   → Geocoder → region_code
   → app_item_rule + app_region_ordinance
   → RegionExtrasLoader → app_mois_disposal, app_region_contact
   → UI Card
```

---

## 5. API·외부 서비스 (런타임)

| 서비스 | 용도 | 필수 |
|--------|------|------|
| Gemini `generateContent` | 이미지→한글 키워드 | 스캔 폴백 시 |
| Geocoder | GPS→행정구역 | 위치 표시 |
| Dialer `tel:` | 지자체·E-순환 | 선택 |

**미사용**: Supabase, 자체 REST, MOIS HTTP(앱 내)

---

## 6. 보안·프라이버시

- 카드 문구: 공공 출처만; Gemini 출력은 검색용 키워드로만 사용  
- 프레임: 필요 최소 크롭만 Gemini 전송  
- 바텀시트: 출처·「AI 보조」 표기 (PRD B-6)

---

## 7. 성능·품질

- ML Kit: 온디바이스, 저지연  
- DB 조회: 메인 스레드 회피 (`Dispatchers.IO`)  
- Gemini: 타임아웃 15s/60s (OkHttp)

---

## 8. 배포·환경

```bash
./gradlew assembleDebug   # 또는 release
```

DB 갱신 후 **앱 재빌드**로 assets 교체.

---

## 9. 미결·확장

- `SupabaseVectorClient` → `VisionSearchClient` 등 이름 정리 (기능 변경 없음)  
- STT Clarification (P1)  
- iOS: Domain/Data 경계 유지, CameraX → AVFoundation
