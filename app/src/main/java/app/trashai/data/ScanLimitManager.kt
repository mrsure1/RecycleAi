package app.trashai.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SharedPreferences를 이용하여 비로그인 사용자 기기의 로컬 AI 스캔 사용 횟수를 트래킹하고 한도를 판정하는 매니저입니다.
 */
object ScanLimitManager {
    private const val PREFS_NAME = "recycle_ai_limit_prefs"
    private const val KEY_PREFIX = "scan_count_"

    /**
     * 오늘 날짜 문자열을 구합니다. (yyyy-MM-dd 형식)
     */
    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * 오늘 날짜의 SharedPreferences 키값을 구합니다.
     */
    private fun getTodayKey(): String {
        return KEY_PREFIX + getTodayDateString()
    }

    /**
     * 오늘 날짜의 누적 AI 스캔 성공 횟수를 조회합니다.
     */
    fun getTodayScanCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(getTodayKey(), 0)
    }

    /**
     * 오늘 날짜의 누적 AI 스캔 성공 횟수를 1 증가시킵니다.
     */
    fun incrementScanCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getTodayKey()
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    /**
     * 보상형 광고 시청이 완료된 후 사용자의 오늘 일일 한도를 추가 충전해 줍니다.
     * 로컬 카운트에서 충전 수량만큼 감산(차감)하여 남은 기회를 벌어주는 방식입니다.
     */
    fun refillScanCount(context: Context, refillAmount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getTodayKey()
        val current = prefs.getInt(key, 0)
        val newValue = (current - refillAmount).coerceAtLeast(0)
        prefs.edit().putInt(key, newValue).apply()
    }

    /**
     * 사용자가 오늘 추가로 AI 스캔을 수행할 수 있는지 한도를 대조합니다.
     */
    fun canScanToday(context: Context): Boolean {
        // Remote Config를 통해 한도 기능이 활성화되지 않은 상태라면 상시 true를 반환
        if (!RemoteConfigManager.limitEnabled) return true

        val count = getTodayScanCount(context)
        return count < RemoteConfigManager.dailyScanLimit
    }

    /**
     * 테스트 및 시뮬레이션을 위해 오늘 누적 횟수를 특정 값으로 리셋해주는 디버깅용 메서드입니다.
     */
    fun resetTodayCount(context: Context, count: Int = 0) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(getTodayKey(), count).apply()
    }
}
