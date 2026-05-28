"""Parse MOIS 엑셀-style 자치단체명 and match to wasteguide (sido, sigungu)."""

from __future__ import annotations

import re

from mois_mapping import normalize_sido, normalize_sigungu, region_match_key, sigungu_aliases


def parse_mois_excel_label(label: str) -> tuple[str, str] | None:
    """
    Examples:
      강원강릉시 -> (강원특별자치도|강원, 강릉시)  — caller matches via region_match_key
      경기고양시덕양구 -> (경기, 고양시 덕양구)
      서울특별시 -> (서울, '')  — skip sigungu-level
    """
    raw = (label or "").replace(" ", "").strip()
    if not raw or "_ALL" in raw:
        return None
    if raw.endswith("전체"):
        return None

    # 시도-only rows (no trailing 구/군/시 at end as sigungu)
    sido_markers = ("특별자치도", "특별자치시", "특별시", "광역시", "도")
    for m in sido_markers:
        if raw.endswith(m) and not re.search(r"(시|군|구)$", raw[len(m) :]):
            return raw, ""

    # ...시 / ...군 / ...구 at end
    m = re.match(r"^(.+?)((?:[가-힣]+시)+(?:[가-힣]+구)?|[가-힣]+군|[가-힣]+구)$", raw)
    if not m:
        return None
    prefix, tail = m.group(1), m.group(2)

    # prefix often = abbreviated sido (강원, 경기, 서울특별시)
    sido = prefix
    if "시" in tail and "구" in tail:
        # 고양시덕양구 -> 고양시 덕양구
        parts = re.findall(r"[가-힣]+(?:시|군|구)", tail)
        if len(parts) >= 2:
            sigungu = f"{parts[0]} {parts[1]}"
        else:
            sigungu = tail
    else:
        sigungu = tail

    # Expand abbreviated sido for matching
    if sido == "강원":
        sido = "강원특별자치도"
    elif sido == "전북":
        sido = "전북특별자치도"
    elif sido == "제주":
        sido = "제주특별자치도"
    elif sido == "세종":
        sido = "세종특별자치시"
    elif sido in ("서울", "서울특별"):
        sido = "서울특별시"
    elif sido == "경기":
        sido = "경기도"
    elif len(sido) <= 3 and sido.endswith("도") is False and "시" not in sido:
        sido = sido + "도"

    return sido, sigungu


def excel_label_matches_ordinance(label: str, sido: str, sigungu: str) -> bool:
    parsed = parse_mois_excel_label(label)
    if not parsed:
        return False
    psido, psigungu = parsed
    if not psigungu:
        return False
    return region_match_key(psido, psigungu) == region_match_key(sido, sigungu)
