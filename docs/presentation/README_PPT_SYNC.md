# HTML 발표 → PowerPoint 동기화

## PPT Agent 스킬

- **skills.sh:** [anthropics/skills/pptx](https://skills.sh/anthropics/skills/pptx) (전역 설치됨)
- **Cursor:** 채팅에 `@pptx` · 상세: [SKILL_PPTX.md](./SKILL_PPTX.md)

## 웹 공유 (GitHub Pages)

- **발표 URL:** https://mrsure1.github.io/RecycleAi/presentation/web/index.html
- 설정·배포: [GITHUB_PAGES.md](./GITHUB_PAGES.md)

## 어떤 파일을 쓸까?

| 용도 | 파일 | 생성 명령 |
|------|------|-----------|
| **글자·표 수정** (수업·발표 원고 편집) | `RecycleAI_Project_Deck_editable.pptx` | `python docs/presentation/sync_html_to_pptx.py --mode text` |
| **HTML과 화면 동일** (네온·글래스 디자인) | `RecycleAI_Project_Deck.pptx` | `python docs/presentation/sync_html_to_pptx.py` |

`RecycleAI_Project_Deck.pptx`(visual)는 슬라이드마다 **PNG 한 장**이라 PowerPoint에서 문장을 고칠 수 없습니다.  
편집이 필요하면 **editable** 파일을 사용하세요.

## 생성 방법

```powershell
cd d:\MrSure\RecycleAi

# 편집 가능 (권장: 원고 수정·교수님 피드백 반영)
python docs/presentation/sync_html_to_pptx.py --mode text

# HTML 픽셀 동기화 (이미지 슬라이드)
python docs/presentation/sync_html_to_pptx.py

# 동일 (text + Fade)
python docs/presentation/build_presentation.py --text --animate
```

## 모드 비교

| 모드 | 결과 파일 | 편집 | 애니메이션 | 영상 |
|------|-----------|------|------------|------|
| **text** | `RecycleAI_Project_Deck_editable.pptx` | 텍스트·표·도형 | 슬라이드 Fade | 12·25번 MP4 삽입 |
| **visual** (기본) | `RecycleAI_Project_Deck.pptx` | 불가 (PNG) | HTML CSS 캡처 | `pptx_embed_videos.py`로 12·25 재구성 |

영상 슬라이드만 visual PPT에서 다시 고치기:

```powershell
python docs/presentation/pptx_embed_videos.py
```

**PowerPoint「복구」가 뜨거나 영상이 안 될 때**

- `pptx_embed_videos.py`를 같은 파일에 여러 번 돌리면 예전 `p:timing`이 남아 손상될 수 있었습니다. 스크립트는 timing을 지운 뒤 다시 넣습니다.
- **복구된 사본**은 영상 재생 정보가 빠지는 경우가 많습니다. PowerPoint를 모두 닫고 `RecycleAI_Project_Deck_with_videos.pptx`를 직접 여세요.
- 그래도 복구가 뜨면: `sync_html_to_pptx.py`(visual) 후 `pptx_embed_videos.py` 실행.

## visual 모드 동작

1. `web/index.html`을 로컬 서버로 띄움 (`/assets` 포함)
2. Playwright가 슬라이드마다 `goToSlide(n)` 후 애니메이션 대기
3. 1280×720 PNG 캡처 → PPT 전체 화면 삽입 (27페이지 마지막은 `#closing-overlay` 포함 캡처)
4. 슬라이드 전환 **Fade**

캡처 PNG: `docs/presentation/captures/` (gitignore)

## 한계

- PowerPoint는 CSS `aurora`·`float`·파티클을 네이티브로 재현하지 못함 → **visual**이 디자인에 가장 가깝습니다.
- **editable**은 레이아웃이 단순(녹색 헤더·불릿)하지만 수정·영상 재생에 유리합니다.
- visual 모드에서 `<video>`는 캡처 시 **한 프레임**만 보일 수 있습니다. 시연·광고 재생은 **HTML** 또는 **editable PPT**를 사용하세요.
