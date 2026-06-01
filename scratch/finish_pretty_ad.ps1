# Higgsfield 예쁜 여성 모델 영상 + 마지막 UGC 한국어 나레이션 합성
# 사용: 완료된 mp4 URL을 $VideoUrl에 넣고 실행
param(
    [Parameter(Mandatory = $true)]
    [string]$VideoUrl
)
$Root = Split-Path $PSScriptRoot -Parent
$ff = python -c "import imageio_ffmpeg; print(imageio_ffmpeg.get_ffmpeg_exe())"
$raw = Join-Path $Root "docs\presentation\assets\recycle_ad_pretty_raw.mp4"
$aud = Join-Path $Root "scratch\recycle_ad_hf_ko_audio.m4a"
$out = Join-Path $Root "docs\presentation\assets\recycle_ad.mp4"
Invoke-WebRequest -Uri $VideoUrl -OutFile $raw -UseBasicParsing
Copy-Item $out (Join-Path $Root "docs\presentation\assets\recycle_ad_before_pretty.mp4") -Force -ErrorAction SilentlyContinue
& $ff -y -i $raw -i $aud -map 0:v:0 -map 1:a:0 -c:v copy -c:a copy $out
Write-Host "완료: $out"
