# HTML 발표 → PowerPoint 동기화

## 웹 공유 (GitHub Pages)

- **발표 URL:** https://mrsure1.github.io/TrashAi/presentation/web/index.html
- 설정·배포: [GITHUB_PAGES.md](./GITHUB_PAGES.md)

## 생성 방법

```powershell
cd d:\MrSure\RecycleAi
python docs/presentation/build_presentation.py
```

또는

```powershell
python docs/presentation/sync_html_to_pptx.py              # visual (기본)
python docs/presentation/sync_html_to_pptx.py --mode text  # 편집 가능 텍스트
python docs/presentation/build_presentation.py --text --animate  # 텍스트 + 슬라이드 Fade
```

## 모드 비교

| 모드 | 결과 | 애니메이션 | 영상 재생 |
|------|------|------------|-----------|
| **visual** (기본) | `RecycleAI_Project_Deck.pptx` | HTML CSS와 동일(캡처) | **12·25페이지** 영상만 재구성(비율 유지·슬라이드쇼 ▶) |

영상 슬라이드만 다시 고치기:

```powershell
python docs/presentation/pptx_embed_videos.py
```

(캡처 PNG 위에 영상을 덧붙이지 않습니다 → 스피커·플레이 UI 겹침 방지)

**PowerPoint「복구」가 뜨거나 영상이 안 될 때**

- `pptx_embed_videos.py`를 같은 파일에 여러 번 돌리면 예전 `p:timing`이 남아 손상될 수 있었습니다. 스크립트는 이제 timing을 지운 뒤 다시 넣습니다.
- **복구된 사본**은 영상 재생 정보가 빠지는 경우가 많습니다. PowerPoint를 모두 닫고 새로 생성된 `RecycleAI_Project_Deck_with_videos.pptx`를 직접 여세요.
- 그래도 복구가 뜨면: `python docs/presentation/sync_html_to_pptx.py`로 전체를 다시 만든 뒤 `pptx_embed_videos.py` 실행.
| **text** | 동일 파일명 | 슬라이드 Fade + (선택) 도형 등장 | `recycle_demo`/`recycle_ad` 삽입 |

## visual 모드 동작

1. `web/index.html`을 로컬 서버로 띄움 (`/assets` 포함)
2. Playwright가 슬라이드마다 `goToSlide(n)` 후 애니메이션 대기(인트로 3.8초, 그 외 2.2초)
3. 1280×720 PNG 캡처 → PPT 전체 화면 삽입
4. 슬라이드 전환 **Fade** (HTML 0.7s 전환에 대응)

캡처 PNG: `docs/presentation/captures/` (gitignore)

## 한계

- PowerPoint는 CSS `aurora`·`float`·파티클을 네이티브로 재현하지 못함 → **visual 모드**가 가장 가깝습니다.
- visual 모드에서 `<video>`는 재생 중이 아닌 **한 프레임**으로 보일 수 있습니다. 시연·광고는 **HTML 발표** 또는 **text 모드 PPT**를 사용하세요.
