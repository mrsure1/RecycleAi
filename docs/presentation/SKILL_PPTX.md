# PPT Agent 스킬 (`@pptx`)

[skills.sh/anthropics/skills/pptx](https://skills.sh/anthropics/skills/pptx) — **128K+ installs** (Anthropic 공식)

## 설치 (이미 적용됨)

```powershell
npx skills add anthropics/skills@pptx -g -y
```

경로: `%USERPROFILE%\.agents\skills\pptx\`

## Cursor에서 쓰는 법

채팅에 **`@pptx`** 를 붙이거나, `docs/presentation/*.py`·`.pptx` 편집 시 규칙 `pptx-recycleai`가 자동 연결됩니다.

예시:

- `@pptx 12페이지 영상 레이아웃만 다시 맞춰줘`
- `@pptx RecycleAI_Project_Deck_with_videos.pptx 슬라이드 텍스트만 추출해줘`

## 이 프로젝트와 함께

| 우선 | 용도 |
|------|------|
| `sync_html_to_pptx.py` | HTML → PPT (1페이지=정적 프로 타이틀, 2~26=캡처) |
| `pptx_embed_videos.py` | 12·25 영상 슬라이드 (타이밍·포스터·전체화면 버튼) |
| `@pptx` 스킬 | 새 덱 디자인, 템플릿 편집, OOXML 수준 수정, 품질 검토 |

## 스킬 도구 (스킬 폴더 내)

```powershell
pip install "markitdown[pptx]" python-pptx playwright
playwright install chromium
python docs/presentation/sync_html_to_pptx.py
```

PPT 1페이지는 `#slide-1 .slide-pro`(네이비·틸, 정적 레이아웃)만 캡처합니다. HTML 발표 1페이지는 시네마틱 인트로를 그대로 사용합니다.

```powershell
pip install "markitdown[pptx]" python-pptx
python %USERPROFILE%\.agents\skills\pptx\scripts\thumbnail.py docs\presentation\RecycleAI_Project_Deck_with_videos.pptx
```

재설치:

```powershell
npx skills add anthropics/skills@pptx -g -y
```
