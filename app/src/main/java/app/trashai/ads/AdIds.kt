package app.trashai.ads

import app.trashai.BuildConfig

/**
 * 광고 단위 ID를 한곳에서 관리합니다.
 *
 * - 디버그 빌드: Google 공식 테스트 ID를 사용합니다. (개발 중 실제 광고 클릭은 AdMob 계정 정지 사유)
 * - 릴리스 빌드: AdMob 콘솔에서 발급받은 실제 광고 단위 ID를 사용합니다.
 *
 * 실제 ID 교체 방법:
 *   1) AdMob 콘솔 → 앱 → Ad units → Add ad unit
 *   2) Banner 1개, Rewarded 1개 생성 후 발급된 ID를 아래 PROD_* 상수에 붙여넣기
 *   3) 앱 ID(ca-app-pub-...~...)는 AndroidManifest.xml 의 APPLICATION_ID 메타데이터에 있습니다.
 */
object AdIds {
    // Google 공식 테스트 광고 단위 ID (수정 금지)
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    // AdMob 콘솔에서 발급받은 실제 광고 단위 ID
    private const val PROD_BANNER = "ca-app-pub-9847865524181124/6430260715"
    private const val PROD_REWARDED = "ca-app-pub-9847865524181124/9442447187"

    /** 결과 카드 본문에 노출되는 배너 광고 단위 ID */
    val banner: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER else PROD_BANNER

    /** 무료 분석 횟수 충전을 위한 보상형 광고 단위 ID */
    val rewarded: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED else PROD_REWARDED
}
