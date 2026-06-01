#!/usr/bin/env python3
"""
RecycleAI 시연 나레이션 WAV 생성 (Supertonic 3 프리셋)

사용법:
  python scratch/make_narration.py --text "한 줄 문장"
  python scratch/make_narration.py --file scratch/narration_script.txt
  python scratch/make_narration.py --file script.txt --voice F2 --out demo/narration.wav

프리셋 목소리: M1~M5 (남성 톤), F1~F5 (여성 톤) — 기본 M1
"""

from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUT = ROOT / "scratch" / "narration.wav"


def main() -> None:
    parser = argparse.ArgumentParser(description="Supertonic 3 한국어 나레이션 생성")
    parser.add_argument("--text", type=str, help="읽을 문장/문단")
    parser.add_argument("--file", type=Path, help="UTF-8 원고 파일 경로")
    parser.add_argument(
        "--voice",
        default="M1",
        help="프리셋: M1~M5, F1~F5 (기본 M1)",
    )
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help="출력 WAV 경로")
    parser.add_argument("--speed", type=float, default=1.0, help="말하기 속도 (1.0=보통)")
    args = parser.parse_args()

    if args.file:
        text = args.file.read_text(encoding="utf-8").strip()
    elif args.text:
        text = args.text.strip()
    else:
        parser.error("--text 또는 --file 중 하나는 필수입니다.")

    if not text:
        raise SystemExit("원고가 비어 있습니다.")

    from supertonic import TTS

    print("모델 로딩 중… (첫 실행 후에는 캐시 사용)")
    tts = TTS(auto_download=True)
    style = tts.get_voice_style(args.voice)

    print(f"합성 중… voice={args.voice}, lang=ko, 글자 수={len(text)}")
    wav, dur = tts.synthesize(text, voice_style=style, lang="ko", speed=args.speed)

    out = args.out
    out.parent.mkdir(parents=True, exist_ok=True)
    tts.save_audio(wav, str(out))

    seconds = float(dur[0]) if hasattr(dur, "__len__") else float(dur)
    print(f"완료: {out}")
    print(f"길이: {seconds:.2f}초")


if __name__ == "__main__":
    main()
