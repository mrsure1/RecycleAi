package app.trashai.data

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * Firebase Remote Config를 연동하여 서버 측의 설정을 실시간으로 적용하는 원격 설정 매니저입니다.
 * 비로그인 사용자 기기에서도 오프라인 캐싱 및 백그라운드 Fetch 방식으로 안정적으로 동작합니다.
 */
object RemoteConfigManager {

    /**
     * 일일 AI 스캔 한도 제한 기능 활성화 여부입니다.
     * Firebase Remote Config 서버의 'limit_enabled' 변수값과 동기화됩니다.
     */
    var limitEnabled: Boolean = false
        private set

    /**
     * 하루에 무료로 제공할 최대 AI 카메라 스캔 횟수입니다.
     * Firebase Remote Config 서버의 'daily_scan_limit' 변수값과 동기화됩니다.
     */
    var dailyScanLimit: Int = 5
        private set

    /**
     * Firebase Remote Config 서버에 연결하여 최신 설정값을 가져오고 동기화합니다.
     */
    fun fetchAndActivate(context: Context, onComplete: () -> Unit = {}) {
        val config = FirebaseRemoteConfig.getInstance()
        
        // 개발 및 테스트의 실시간 반영을 위해 최소 Fetch 주기를 0초로 설정합니다.
        // (주의: 프로덕션 배포 시에는 API 제한 방지를 위해 적절한 값(예: 1~12시간)으로 늘려주세요.)
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0)
            .build()
        config.setConfigSettingsAsync(settings)

        // 오프라인 상태나 Fetch 실패 시 적용할 로컬 디폴트 값을 설정해 둡니다.
        val defaults = mapOf(
            "limit_enabled" to false,
            "daily_scan_limit" to 5
        )
        config.setDefaultsAsync(defaults)

        config.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 서버의 최신 설정값 반영
                    limitEnabled = config.getBoolean("limit_enabled")
                    dailyScanLimit = config.getLong("daily_scan_limit").toInt()
                }
                onComplete()
            }
    }

    /**
     * 테스트 및 시뮬레이션을 위해 원격 설정 플래그를 강제로 변경할 수 있게 제공하는 제어용 메서드입니다.
     */
    fun setSimulatedConfig(enabled: Boolean, limit: Int) {
        limitEnabled = enabled
        dailyScanLimit = limit
    }
}
