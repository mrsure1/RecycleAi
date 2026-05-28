"""Shared helpers: wasteguide 5-digit region_code ↔ MOIS API 7-digit sigungu_cd."""

from __future__ import annotations

import re
import unicodedata


def normalize_sido(name: str) -> str:
    s = unicodedata.normalize("NFKC", name or "").strip()
    for token in ("특별자치도", "특별시", "광역시", "특별자치시"):
        s = s.replace(token, "")
    return s.replace(" ", "")


def normalize_sigungu(name: str) -> str:
    s = unicodedata.normalize("NFKC", name or "").strip()
    s = s.replace(" ", "")
    # "고양시 덕양구" → "고양시덕양구" and alias "덕양구"
    return s


def sigungu_aliases(name: str) -> set[str]:
    base = normalize_sigungu(name)
    aliases = {base}
    parts = re.split(r"(시|군|구)", base)
    if len(parts) >= 3:
        tail = "".join(parts[-3:])
        if tail:
            aliases.add(tail)
    return aliases


def region_match_key(sido: str, sigungu: str) -> tuple[str, frozenset[str]]:
    return normalize_sido(sido), frozenset(sigungu_aliases(sigungu))


def mois_item_matches_region(
    item: dict,
    expected_sido: str,
    expected_sigungu: str,
) -> bool:
    """Reject MOIS rows that clearly belong to another municipality."""
    api_sido = normalize_sido(item.get("CTPV_NM", "") or "")
    api_sgg = normalize_sigungu(item.get("SGG_NM", "") or "")
    exp_sido = normalize_sido(expected_sido)
    exp_aliases = sigungu_aliases(expected_sigungu)
    if exp_sido and api_sido and exp_sido not in api_sido and api_sido not in exp_sido:
        return False
    if not api_sgg:
        return True
    return any(a in api_sgg or api_sgg in a for a in exp_aliases)
