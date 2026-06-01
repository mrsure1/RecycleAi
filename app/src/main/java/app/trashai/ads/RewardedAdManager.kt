package app.trashai.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * 보상형 광고(Rewarded Ad)를 로드/표시하고 시청 완료 보상을 전달합니다.
 *
 * 사용 흐름:
 *   - 앱 시작 또는 한도 도달 화면 진입 시 [preload] 로 미리 로드해 둡니다.
 *   - 사용자가 "광고 보기"를 누르면 [showWhenReady] 를 호출합니다.
 *   - 영상 시청을 끝까지 완료하면 onReward 가 호출됩니다. (이때만 충전)
 */
object RewardedAdManager {
    private const val TAG = "RewardedAdManager"

    private var rewardedAd: RewardedAd? = null
    private var loading = false
    private var onLoadResult: (() -> Unit)? = null

    val isReady: Boolean
        get() = rewardedAd != null

    /** 보상형 광고를 미리 로드합니다. 이미 로드됐거나 로딩 중이면 무시합니다. */
    fun preload(context: Context) {
        if (rewardedAd != null || loading) return
        loading = true
        RewardedAd.load(
            context.applicationContext,
            AdIds.rewarded,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    loading = false
                    onLoadResult?.invoke()
                    onLoadResult = null
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "보상형 광고 로드 실패: ${error.message}")
                    rewardedAd = null
                    loading = false
                    onLoadResult?.invoke()
                    onLoadResult = null
                }
            },
        )
    }

    /**
     * 광고가 준비돼 있으면 즉시 표시하고, 로딩 중이면 로드 완료 후 자동으로 표시합니다.
     *
     * @param onReward     영상 시청을 끝까지 완료해 보상 조건을 충족했을 때 (충전 실행)
     * @param onUnavailable 광고를 불러오지 못했거나 보상 없이 닫힌 경우 (충전하지 않음)
     * @param onWaiting    광고 로딩을 기다리는 동안 (로딩 UI 표시용)
     */
    fun showWhenReady(
        activity: Activity,
        onReward: () -> Unit,
        onUnavailable: () -> Unit,
        onWaiting: () -> Unit = {},
    ) {
        if (isReady) {
            show(activity, onReward, onUnavailable)
            return
        }
        onWaiting()
        onLoadResult = {
            if (isReady) show(activity, onReward, onUnavailable) else onUnavailable()
        }
        preload(activity)
    }

    private fun show(
        activity: Activity,
        onReward: () -> Unit,
        onUnavailable: () -> Unit,
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            preload(activity)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload(activity)
                if (earned) onReward() else onUnavailable()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "보상형 광고 표시 실패: ${error.message}")
                rewardedAd = null
                preload(activity)
                onUnavailable()
            }
        }
        ad.show(activity) { earned = true }
    }
}
