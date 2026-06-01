#!/usr/bin/env python3
"""Higgsfield 완료 URL로 예쁜 여성 영상 + 마지막 UGC 한국어 음성 합성."""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path
from urllib.request import urlretrieve

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "docs" / "presentation" / "assets"
RAW = ASSETS / "recycle_ad_pretty_raw.mp4"
AUDIO = ROOT / "scratch" / "recycle_ad_hf_ko_audio.m4a"
OUT = ASSETS / "recycle_ad.mp4"


def ffmpeg() -> str:
    import imageio_ffmpeg

    return imageio_ffmpeg.get_ffmpeg_exe()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("url", help="Higgsfield 완료 mp4 URL")
    args = parser.parse_args()
    if not AUDIO.is_file():
        sys.exit(f"음성 없음: {AUDIO} — 먼저 마지막 UGC에서 추출하세요.")

    print(f"다운로드: {args.url}")
    urlretrieve(args.url, RAW)

    if OUT.is_file():
        backup = ASSETS / "recycle_ad_before_pretty.mp4"
        backup.write_bytes(OUT.read_bytes())
        print(f"백업: {backup}")

    ff = ffmpeg()
    cmd = [
        ff,
        "-y",
        "-i",
        str(RAW),
        "-i",
        str(AUDIO),
        "-map",
        "0:v:0",
        "-map",
        "1:a:0",
        "-c:v",
        "copy",
        "-c:a",
        "copy",
        str(OUT),
    ]
    subprocess.run(cmd, check=True)
    print(f"완료: {OUT}")


if __name__ == "__main__":
    main()
