# RecycleAI 광고 영상 — Higgsfield 제작 가이드

Higgsfield MCP로 앱 광고 영상을 만들 때 사용하는 프롬프트·설정·크레딧 안내입니다.

## 현재 상태 (2026-05-31)

| 항목 | 내용 |
|------|------|
| **활성 파일** | `recycle_ad.mp4` — Higgsfield **한국어 립싱크 일체 생성** (음성 덧씌우기 없음) |
| 최신 job | `cd9bcdf1-3efe-45ca-888b-efcf15dfc00a` (예쁜 여성 + 대사 3줄 고정) |
| 백업 | `recycle_ad_lipsync_ko.mp4` · `recycle_ad_muxed_bad_lipsync.mp4` (음성만 합성본, 립싱크 불일치) |
| ⚠️ 주의 | **외부 나레이션을 영상에 mux 하면 립싱크가 깨집니다.** 반드시 생성본 그대로 사용 |
| 발표 연동 | **12번** `recycle_demo.mp4` · **Q&A 직전(웹 26번)** `recycle_ad.mp4` |

### 백업 파일

| 파일 | 설명 |
|------|------|
| `recycle_ad_en_original.mp4` | 1차 Higgsfield (영어 나레이션) |
| `recycle_ad_hf_ko_ugc.mp4` | 2차 Higgsfield 원본 다운로드 |
| `recycle_ad_tts_dub.mp4` | 로컬 Supertonic 한국어 더빙 (립싱크 없음) |

### 1차 생성 (참고)

| 항목 | 내용 |
|------|------|
| job ID | `8a2c0e90-d5d3-409e-ad08-782a02b19367` |
| 모델 | `marketing_studio_video` (기본) |
| 결과 | 영어 나레이션 → `recycle_ad_en_original.mp4` |

### 크레딧 (2026-05-31 기준)

| 구분 | 크레딧 |
|------|--------|
| 충전 후 잔액 | **208** |
| 1차 `marketing_studio_video` | **−75** |
| 2차 UGC 한국어 재생성 | **−75** |
| Kling 3.0 + 오디오 (미제출, 오류) | **0** |
| **현재 잔액** | **58** (Starter) |
| 광고 생성 합계 | **150** |

이전에 업로드한 참조 이미지 media_id는 아래 표 참고.

### 업로드된 미디어 (Higgsfield에 이미 등록됨)

| 파일 | 용도 | media_id |
|------|------|----------|
| `assets/recycle_logo2.png` | 로고 | `c3985ec7-7b2e-4077-b748-95ce09fc0b16` |
| `assets/recycleAI_01.png` | 카메라 스캔 화면 | `05b4ea8c-d244-4792-833d-45bbd0b3ceaa` |

---

## 추천: Marketing Studio 광고 (앱 홍보용)

Cursor에서 Higgsfield 플러그인 연결 후, 채팅에 아래처럼 요청하면 됩니다.

```text
Higgsfield로 RecycleAI 앱 광고 영상 만들어줘.
- model: marketing_studio_video
- 9:16, 15초
- webproduct: RecycleAI (분리수거 안내 앱)
- 참조 이미지: 05b4ea8c-d244-4792-833d-45bbd0b3ceaa
```

### 영문 프롬프트 (복사용)

```text
15-second vertical mobile app ad for RecycleAI, a Korean recycling guide app for seniors and families.

Scene 1: Middle-aged person in a bright kitchen, confused while holding a plastic cup and food packaging.
Scene 2: Opens smartphone, RecycleAI app camera scans the item; green detection box and item label appear.
Scene 3: Bottom card shows local disposal rules in Korean UI — pickup day, location, rinse instructions; large readable text.
Scene 4: Pinch-zoom on the card for accessibility; warm trustworthy tone.
Scene 5: End card — RecycleAI logo, tagline "카메라만 비추면, 우리 동네 분리배출".

Style: clean emerald green brand, authentic UGC, soft daylight, no horror, no clutter. Korean home context.
```

### 한국어 나레이션 (별도 TTS 합성 시)

`scratch/narration_script.txt` 또는 짧은 버전:

```text
복잡한 쓰레기 분리배출, 이제 리사이클AI로 쉽게 해결하세요.
카메라만 비추면 우리 동네 기준으로 알려드립니다.
RecycleAI — 이제 분리수거, 카메라만 비추면 됩니다.
```

로컬 TTS: `python scratch/make_narration.py --file scratch/narration_script.txt --voice F1 --speed 1.15`

---

## Higgsfield 웹에서 직접 만들기

1. [Higgsfield](https://higgsfield.ai) 로그인
2. **Marketing Studio** → **Web product** 또는 **Product** 선택
3. 이미지 업로드: `docs/presentation/assets/recycle_logo2.png`, `recycleAI_01.png`
4. 제목: `RecycleAI` / 부제: `카메라만 비추면 우리 동네 분리배출`
5. 프리셋: **UGC** 또는 **Tutorial** (앱 설명형)
6. 비율 **9:16** (숏폼·스토어·SNS), 길이 **12~15초**
7. 위 영문 프롬프트 붙여넣기 → 생성

---

## 생성 후 프로젝트에 넣기

1. 다운로드한 mp4를 아래에 저장:

```text
docs/presentation/assets/recycle_ad.mp4
```

2. 발표 슬라이드·웹에 넣으려면 채팅에서:

```text
recycle_ad.mp4를 발표 슬라이드 12번 폰 프레임에 넣어줘
```

(또는 `recycle_demo.mp4` 파일명으로 저장해 기존 시연 슬라이드와 통일)

---

## 크레딧·플랜

- Cursor: **Settings → Tools & MCPs → Higgsfield** (로그인 상태 확인)
- 크레딧 부족 시 Higgsfield에서 **Credit Top-up** 또는 **Upgrade**
- 생성 전 비용 확인: `generate_video`에 `get_cost: true` (Marketing Studio / Seedance 지원)

---

## 대안 (크레딧 없을 때)

- 이미 만든 **시연 영상** `recycle_demo.mp4`를 광고·발표에 그대로 사용
- **Supertonic 3** 로 나레이션만 추가 (`scratch/make_narration.py`)
- Canva / CapCut 등에서 스크린샷 + 나레이션으로 15초 숏폼 편집
