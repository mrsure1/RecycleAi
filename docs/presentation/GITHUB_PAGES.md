# HTML 발표 — GitHub Pages

`main` 브랜치에 푸시하면 Actions가 `docs/` 폴더를 Pages로 배포합니다.

## 공유 URL

| 용도 | 주소 |
|------|------|
| **발표 (메인)** | https://mrsure1.github.io/RecycleAi/presentation/web/index.html |
| 짧은 진입 | https://mrsure1.github.io/RecycleAi/ |

### Pages 소스별 동작

| Settings 폴더 | `https://…/RecycleAi/` 에서 보이는 것 |
|---------------|--------------------------------------|
| **`/docs` (권장)** | `docs/index.html` → 발표로 자동 이동 |
| **`/(root)`** | 루트 `index.html` → `docs/presentation/web/…` 로 이동 (README만 보이면 루트 배포 + Jekyll — `.nojekyll`·`index.html` push 필요) |

**지금 README만 보인다면** Pages가 `/(root)` 이고 발표 경로는 아래입니다.  
https://mrsure1.github.io/RecycleAi/docs/presentation/web/index.html

## 로컬 미리보기

```powershell
cd docs/presentation
python -m http.server 8080
# http://localhost:8080/web/index.html
```

## 배포 확인

1. 저장소 **Settings → Pages** → **Build and deployment**
   - **Source: GitHub Actions** (필수 — `Deploy from a branch`만 켜 두면 Actions 배포가 404)
2. **Actions** → `Deploy presentation (GitHub Pages)` → 최근 실행에 **초록 체크**
   - 실패(빨간 X)면 로그에서 `Pages` / `configure-pages` 오류 확인
   - 수동 재배포: Actions → 해당 워크플로 → **Run workflow**
3. 성공 후 1~2분 뒤 URL 열기 (12·25번 영상 재생 확인)

## 404가 날 때

| 증상 | 원인 | 조치 |
|------|------|------|
| `github.io/RecycleAi/` 전부 404 | Pages 미활성 또는 배포 실패 | Settings → Pages → Source **GitHub Actions** 선택 후 워크플로 재실행 |
| Actions는 성공인데 404 | 예전 Source가 branch `/docs` 등으로 충돌 | Pages에서 Source를 **GitHub Actions**로만 맞춘 뒤 재배포 |
| 로컬만 되고 온라인만 404 | `main`에 `docs/` 미푸시 | `git push origin main` (아래 로컬 미반영 목록 확인) |

저장소가 **Private**이면 무료 플랜에서는 GitHub Pages가 제한될 수 있습니다. 발표 공유용이면 **Public** 저장소를 권장합니다.

## 필요한 경로

- `docs/presentation/web/index.html`
- `docs/presentation/assets/*` (png, mp4)

`../assets/` 상대 경로는 Pages에서 `presentation/assets/`로 해석됩니다.
