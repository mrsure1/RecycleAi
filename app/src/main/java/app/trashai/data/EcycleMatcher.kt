package app.trashai.data

/**
 * E-순환거버넌스 무상 수거 안내([app_common_guide] ecycle) 노출 여부.
 * 품목사전 본문에 이미 E-순환·생활가전폐기물 안내가 있으면 우선 매칭합니다.
 */
object EcycleMatcher {

    private val nameKeywords = listOf(
        "TV", "냉장고", "세탁기", "에어컨", "전자레인지", "청소기", "컴퓨터", "모니터",
        "노트북", "선풍기", "가습기", "헤어드라이어", "헤어드라이", "드라이어", "러닝머신",
        "마우스", "키보드", "스피커", "프린터", "공유기", "태블릿", "스마트폰", "휴대폰",
        "전화기", "전기밥솥", "토스터", "오븐", "정수기", "공기청정기", "제습기", "전기히터",
        "PC", "본체", "데스크탑", "노트", "배터리",
    )

    /** 품목사전·조례 본문에 자주 쓰이는 E-순환·소형전기전자제품 표현 */
    private val textMarkers = listOf(
        "E-순환",
        "e-순환",
        "1599-0903",
        "15990903",
        "무상방문수거",
        "생활가전폐기물",
        "소형전기전자제품",
        "소형 전기전자",
        "폐가전",
        "전기·전자제품",
        "전기전자제품",
        "환경성보장제도",
    )

    fun showsEcycleGuide(rule: ItemRule): Boolean {
        val cat = rule.primaryCategory.orEmpty()
        if (cat.contains("가전") || cat.contains("폐가전")) return true

        val blob = buildString {
            append(rule.itemName)
            rule.dischargeMethod?.let { append(it) }
            rule.featureText?.let { append(it) }
            rule.appSummary?.let { append(it) }
            append(cat)
        }
        if (textMarkers.any { blob.contains(it, ignoreCase = true) }) return true

        return nameKeywords.any { keyword ->
            rule.itemName.contains(keyword, ignoreCase = true)
        }
    }
}
