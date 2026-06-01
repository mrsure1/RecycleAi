package app.trashai.update

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google Play 인앱 업데이트(In-App Updates)를 관리합니다.
 *
 * 방식: Flexible(유연한) 업데이트
 *  - 새 버전이 있으면 백그라운드에서 다운로드하고, 사용자는 앱을 계속 사용합니다.
 *  - 다운로드가 끝나면 [isDownloadReady] 가 true 가 되어 "다시 시작" 안내를 띄웁니다.
 *  - 사용자가 [completeUpdate] 를 누르면 설치 후 앱이 재시작됩니다.
 *
 * 주의:
 *  - 인앱 업데이트는 **Google Play로 설치된 앱**에서만 동작합니다.
 *    (Android Studio 직접 설치/사이드로딩에서는 동작하지 않으며, 내부 테스트 트랙 등으로 확인)
 *  - 강제 업데이트가 필요하면 [AppUpdateType.IMMEDIATE] 로 바꾸면 됩니다.
 */
class InAppUpdateManager(private val activity: Activity) {
    private val manager = AppUpdateManagerFactory.create(activity)
    private var listener: InstallStateUpdatedListener? = null

    /** 다운로드가 끝나 설치(앱 재시작)만 남은 상태인지 여부. Compose에서 관찰합니다. */
    var isDownloadReady by mutableStateOf(false)
        private set

    /** 업데이트가 있으면 Flexible 다운로드 플로를 시작합니다. */
    fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                if (available && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    registerListener()
                    runCatching {
                        manager.startUpdateFlowForResult(
                            info,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        )
                    }.onFailure { Log.w(TAG, "업데이트 플로 시작 실패: ${it.message}") }
                } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    // 이전에 받아둔 업데이트가 설치 대기 중인 경우
                    isDownloadReady = true
                }
            }
            .addOnFailureListener { Log.w(TAG, "업데이트 확인 실패: ${it.message}") }
    }

    /** onResume 등에서 호출 — 자리를 비운 사이 다운로드가 끝났으면 설치 안내를 다시 띄웁니다. */
    fun onResume() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                isDownloadReady = true
            }
        }
    }

    /** 다운로드된 업데이트를 설치하고 앱을 재시작합니다. */
    fun completeUpdate() {
        manager.completeUpdate()
    }

    fun dismissDownloadReady() {
        isDownloadReady = false
    }

    private fun registerListener() {
        if (listener != null) return
        listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                isDownloadReady = true
            }
        }.also { manager.registerListener(it) }
    }

    fun unregister() {
        listener?.let { manager.unregisterListener(it) }
        listener = null
    }

    private companion object {
        const val TAG = "InAppUpdateManager"
    }
}
