# RecycleAI — 터미널 한글 깨짐 방지 (PowerShell에서 dot-source 또는 Agent가 매 명령 전 실행)
chcp 65001 | Out-Null
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$env:PYTHONIOENCODING = 'utf-8'
$env:PYTHONUTF8 = '1'
