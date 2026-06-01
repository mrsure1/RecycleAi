# ♻️ RecycleAI (리사이클AI)

> **카메라로 비추기만 하세요. AI가 우리 동네 기준으로 분리배출을 안내합니다.**

Android 네이티브(Compose) 앱입니다. 실시간 카메라로 물건을 인식하고, **번들 SQLite**에 담긴 공공 품목·지자체 조례·배출 일정으로 안내 카드를 만듭니다. 애매한 경우에만 **Google Gemini**로 사진에서 한글 키워드를 뽑은 뒤, 같은 DB로 다시 매칭합니다.

---

## 핵심 아키텍처

```text
[CameraX + ML Kit]  녹색(자동) / 주황(드래그) 바운딩 박스
        │
        ├─► 로컬 SQLite — app_item_rule, app_search_keyword
        │
        └─► Gemini (선택) — 이미지 → 한글 키워드 → SQLite Grounding
                │
                ▼
[Geocoder + 번들 SQLite]
  app_region_ordinance · app_mois_disposal · app_region_contact · app_common_guide
        │
        ▼
[바텀시트 카드 · 핀치 줌 · 전화 연결]
```

- **런타임 네트워크**: Gemini API만 (스캔 폴백). 품목·조례·배출 요일·문의처는 **오프라인 DB**.
- **백엔드 서버 없음**: Supabase·PostgREST·자체 API 미사용.

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 실시간 스캔 | ML Kit 객체 감지, 터치·드래그로 분석 대상 지정 |
| 지역 맞춤 | GPS → Geocoder → 해당 시·군·구 조례·MOIS 배출 요일 |
| 하이브리드 매칭 | 로컬 품목 DB 우선, 미매칭 시 Gemini |
| 폐가전 | E-순환거버넌스 안내·`1599-0903` 통화 |
| 접근성 | 핀치 줌, Clarification(품목 정정) |

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| 앱 | Kotlin, Jetpack Compose, CameraX, ML Kit |
| 온디바이스 DB | SQLite (`WasteGuideDb`, `assets/wasteguide.sqlite3`) |
| 비전 폴백 | `GeminiClient` (`GEMINI_API_KEY`) |
| 데이터 파이프라인 | Python 3 (`scripts/`, `data/`) |

---

## 빠른 시작

### 1. `local.properties` (앱)

```properties
GEMINI_API_KEY=your-gemini-key
```

스캔·Gemini 폴백에 **필수**입니다.

### 2. 품목·조례 DB

```bash
pip install -r requirements.txt
python scripts/complete_wasteguide_db.py --skip-crawl
```

### 3. 지역 배출 일정·문의처 (assets 반영)

```properties
# local.properties — 파이프라인용
WASTE_OPEN_API_KEY=your-data-go-kr-key
```

```bash
python scripts/build_region_mois_map.py
python scripts/import_region_extras.py
```

산출물: `app/src/main/assets/wasteguide.sqlite3`

### 4. Android 빌드

```bash
./gradlew assembleDebug
```

---

## 문서

| 문서 | 내용 |
|------|------|
| [`PRD.md`](PRD.md) | 제품 요구사항 |
| [`trd.md`](trd.md) | 기술 요구사항 |
| [`docs/architecture.md`](docs/architecture.md) | 레이어·데이터 플로우 |
| [`problem.md`](problem.md) | 문제 정의·페르소나 |
| [`docs/public_data_integration_guide.md`](docs/public_data_integration_guide.md) | MOIS API·지역 코드 매핑 |
| [`docs/admin_operations_guide.md`](docs/admin_operations_guide.md) | 광고 표시·AI 스캔 횟수제한 운영 안내 |
| [`docs/광고_업데이트_셋업가이드.md`](docs/광고_업데이트_셋업가이드.md) | 광고·인앱 업데이트 셋업 & 배포(AAB) 가이드 |
| [**HTML 발표 (GitHub Pages)**](https://mrsure1.github.io/TrashAi/presentation/web/index.html) | 슬라이드쇼 · 시연·광고 영상 포함 |
| [`CLAUDE.md`](CLAUDE.md) | 저장소 작업 가이드 |

---

## 데이터 출처

- 품목·조례: [생활폐기물 분리배출 누리집](https://wasteguide.or.kr)
- 배출 요일·시간: 행정안전부 생활쓰기물 배출정보 Open API (빌드 시 SQLite 적재)
- 폐가전: E-순환거버넌스

---

## 버전

**v1.6 (2026-05)** — 오프라인 MOIS 일정, Gemini-only 클라우드, ML Kit + Compose Production Ready
