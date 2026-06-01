# Windows 터미널 한글 깨짐 방지

증상: `콺`, `Ȱ⹰` 처럼 한글이 깨져 보임 (CP949 vs UTF-8).

## 이 저장소에 포함된 설정

1. **`.vscode/settings.json`** — Cursor / VS Code / 워크스페이스를 연 IDE에서  
   새 터미널을 열면 **UTF-8(코드 페이지 65001)** + Python UTF-8 모드가 자동 적용됩니다.

2. **`scripts/utf8_env.ps1`** — Agent·수동 PowerShell에서 한 번 실행:
   ```powershell
   . d:\MrSure\RecycleAi\scripts\utf8_env.ps1
   python scripts/peek_item.py 마우스
   ```

3. **Python 스크립트** — `peek_item.py` 등은 stdout을 UTF-8로 재설정합니다.

## Antigravity / IDE를 껐다 켠 뒤

- **사용자 전역 설정**만 바꿨다면 IDE마다·재설치 시 초기화될 수 있습니다.
- **이 프로젝트의 `.vscode/settings.json`** 은 Git에 있으므로 `RecycleAi` 폴더를 열면 다시 적용됩니다.
- 터미널 프로필이 `PowerShell` 기본으로 돌아가면: 터미널 패널 ▼ → **RecycleAI UTF-8 PowerShell** 선택.

## Cursor 전역 설정 (선택, 모든 프로젝트)

`%APPDATA%\Cursor\User\settings.json` 에 이미 비슷한 항목이 있으면 유지하고, 없으면:

```json
"terminal.integrated.env.windows": {
  "PYTHONIOENCODING": "utf-8",
  "PYTHONUTF8": "1"
}
```

## 확인

```powershell
chcp
python -c "print('마우스 테스트')"
```

`Active code page: 65001` 이고 한글이 정상이면 OK.
