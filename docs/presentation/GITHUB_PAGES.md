# HTML 발표 — GitHub Pages

`main` 브랜치에 푸시하면 Actions가 `docs/` 폴더를 Pages로 배포합니다.

## 공유 URL

| 용도 | 주소 |
|------|------|
| **발표 (메인)** | https://mrsure1.github.io/TrashAi/presentation/web/index.html |
| 짧은 진입 (리다이렉트) | https://mrsure1.github.io/TrashAi/ |

## 로컬 미리보기

```powershell
cd docs/presentation
python -m http.server 8080
# http://localhost:8080/web/index.html
```

## 배포 확인

1. GitHub 저장소 → **Actions** → `Deploy presentation (GitHub Pages)` 성공 여부
2. **Settings → Pages** → Source: **GitHub Actions**
3. 시크릿 창에서 위 URL 열기 (12·25번 영상 재생 확인)

## 필요한 경로

- `docs/presentation/web/index.html`
- `docs/presentation/assets/*` (png, mp4)

`../assets/` 상대 경로는 Pages에서 `presentation/assets/`로 해석됩니다.
