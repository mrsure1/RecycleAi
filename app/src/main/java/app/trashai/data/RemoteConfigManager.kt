package app.trashai.data

import android.content.Context

/**
 * 차후 Firebase Remote Config 연동 시 한 줄의 변경만으로 대체 가능한 원격 설정 제어 매니저입니다.
 * 비로그인 사용자 환경을 기본으로 설계되었습니다.
 */
object RemoteConfigManager {

    /**
     * 일일 AI 스캔 한도 제한 기능 활성화 여부입니다.
     * 출시 초기에는 무료 배포로 유저를 검증하기 위해 false로 설정하며,
     * 차후 광고 기능을 얹을 때 원격으로 true로 전환할 수 있는 스위치 역할을 모사합니다.
     */
    var limitEnabled: Boolean = false
        private set

    /**
     * 하루에 무료로 제공할 최대 AI 카메라 스캔 횟수입니다.
     */
    var dailyScanLimit: Int = 5
        private set

    /**
     * Firebase Remote Config의 설정값을 가져와 동기화하는 함수 시뮬레이터입니다.
     * 현재는 로컬 디폴트 값을 사용하고, 나중에 Firebase 연동 시 내부 구현만 API 호출로 교체합니다.
     */
    fun fetchAndActivate(context: Context, onComplete: () -> Unit = {}) {
        // TODO: FirebaseRemoteConfig.getInstance() 로 대체 가능
        // 현재는 시뮬레이션용 로컬 디폴트 설정을 고수합니다.
        limitEnabled = false // 기본값: 비활성화 (무제한)
        dailyScanLimit = 5   // 기본값: 일일 5회
        onComplete()
    }

    /**
     * 테스트 및 시뮬레이션을 위해 원격 설정 플래그를 강제로 변경할 수 있게 제공하는 제어용 메서드입니다.
     */
    fun setSimulatedConfig(enabled: Boolean, limit: Int) {
        limitEnabled = enabled
        dailyScanLimit = limit
    }
}
