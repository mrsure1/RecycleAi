# RecycleAI 관리자 운영 안내

이 문서는 배포 후 광고 표시와 AI 스캔 횟수제한을 운영자가 쉽게 관리하기 위한 안내입니다.

## 1. 광고 배너 표시 관리

현재 분리수거 결과 카드의 배너 광고는 `CardSheet.kt`에서 제어합니다.

```kotlin
private const val SHOW_AD_BANNER = false
```

위치:

```text
app/src/main/java/app/trashai/ui/CardSheet.kt
```

### 광고를 보이게 하는 방법

`SHOW_AD_BANNER`를 `true`로 바꿉니다.

```kotlin
private const val SHOW_AD_BANNER = true
```

그러면 `ItemRuleBody` 내부의 `BannerAdView(...)`가 렌더링되고, 하단 결과 카드에 배너가 표시됩니다.

### 광고를 숨기는 방법

`SHOW_AD_BANNER`를 `false`로 바꿉니다.

```kotlin
private const val SHOW_AD_BANNER = false
```

광고 컴포넌트와 테스트 ID는 코드에 남아 있지만, 화면에는 배너가 나타나지 않습니다.

### 광고 ID 교체

모든 광고 단위 ID는 한 파일에서 관리합니다.

```text
app/src/main/java/app/trashai/ads/AdIds.kt
```

이 파일은 **디버그 빌드에서는 Google 공식 테스트 ID**, **릴리스 빌드에서는 실제 ID**를 자동으로 사용하도록 `BuildConfig.DEBUG`로 분기합니다. 개발·테스트 중 실제 광고를 클릭하면 AdMob 계정이 정지될 수 있으므로 이 분기를 그대로 유지하는 것을 권장합니다.

실서비스 배포 전, AdMob 콘솔에서 **배너 1개 + 보상형 1개**의 광고 단위를 만들어 발급된 ID를 아래 `PROD_*` 상수에 붙여넣습니다.

```kotlin
// AdIds.kt
private const val PROD_BANNER = "ca-app-pub-9847865524181124/0000000000"   // ← 발급받은 배너 ID
private const val PROD_REWARDED = "ca-app-pub-9847865524181124/0000000000" // ← 발급받은 보상형 ID
```

- 배너는 `CardSheet.kt`의 `BannerAdView(adUnitId = AdIds.banner)`에서 사용합니다.
- 보상형은 `RewardedAdManager`가 `AdIds.rewarded`로 로드/표시합니다.
- 앱 ID(`ca-app-pub-...~...`)는 `AndroidManifest.xml`의 `APPLICATION_ID` 메타데이터에 있습니다.

### 배너 광고 크기 참고

현재 앱의 `BannerAdView`는 AdMob 기본 배너 크기를 사용합니다.

```kotlin
setAdSize(com.google.android.gms.ads.AdSize.BANNER)
```

대표적인 AdMob 배너 크기는 다음과 같습니다.

| AdMob 크기 | 일반 크기 | 참고 |
|---|---:|---|
| `BANNER` | `320 x 50` | 현재 앱 기본값. 결과 카드 안에 넣기 가장 무난 |
| `LARGE_BANNER` | `320 x 100` | 더 잘 보이지만 결과 안내 영역을 더 많이 차지 |
| `MEDIUM_RECTANGLE` | `300 x 250` | 수익성은 좋을 수 있으나 카드 UX를 크게 밀어냄 |
| `FULL_BANNER` | `468 x 60` | 태블릿·가로 화면에 적합 |
| `LEADERBOARD` | `728 x 90` | 태블릿·대형 화면용 |
| Adaptive Banner | 화면 폭 기준 자동 계산 | 모바일 앱에서 장기적으로 권장 |

RecycleAI 결과 카드에는 안내 문구·전화번호·버튼이 함께 들어가므로 기본 `BANNER(320 x 50)`가 가장 안전합니다. 화면 폭에 더 자연스럽게 맞추고 싶다면 추후 Adaptive Banner로 교체할 수 있습니다.

## 2. AI 스캔 횟수제한 관리

횟수제한은 Firebase Remote Config 값으로 관리합니다.

위치:

```text
app/src/main/java/app/trashai/data/RemoteConfigManager.kt
app/src/main/java/app/trashai/data/ScanLimitManager.kt
```

### Remote Config 키

| 키 | 타입 | 기본값 | 설명 |
|---|---:|---:|---|
| `limit_enabled` | Boolean | `false` | 일일 스캔 횟수제한 사용 여부 |
| `daily_scan_limit` | Number | `5` | 하루 무료 AI 스캔 제공 횟수 |

### 횟수제한을 켜는 방법

Firebase Console → Remote Config에서 다음처럼 설정합니다.

```text
limit_enabled = true
daily_scan_limit = 5
```

앱은 실행 시 `RemoteConfigManager.fetchAndActivate(...)`를 호출해 최신 설정을 가져옵니다. `limit_enabled`가 `true`이면 메인 화면에 남은 분석 기회 배지가 표시되고, 사용자가 하루 한도를 넘으면 보상형 광고 안내 팝업이 표시됩니다.

### 횟수제한을 끄는 방법

```text
limit_enabled = false
```

이 경우 `ScanLimitManager.canScanToday(...)`가 항상 `true`를 반환하므로 스캔 제한이 적용되지 않습니다. 남은 횟수 배지도 숨겨집니다.

### 하루 제공 횟수 변경

예를 들어 하루 10회로 늘리려면:

```text
limit_enabled = true
daily_scan_limit = 10
```

사용자별 누적 횟수는 기기 로컬 `SharedPreferences`에 날짜별로 저장됩니다. 날짜가 바뀌면 새 날짜 키를 사용하므로 자동으로 일일 카운트가 초기화되는 구조입니다.

## 3. 보상형 광고 충전 흐름

실제 AdMob `RewardedAd` SDK와 연결돼 있습니다. 흐름은 다음과 같습니다.

1. 사용자가 AI 스캔을 시도합니다.
2. `ScanLimitManager.canScanToday(context)`가 오늘 사용 횟수와 `daily_scan_limit`를 비교합니다.
3. 한도 내이면 스캔을 진행하고 성공 횟수를 증가시킵니다.
4. 한도 초과이면 `SheetState.AdLimitReached` 충전 팝업을 표시합니다.
5. 앱은 한도 화면 진입 시 `RewardedAdManager.preload(...)`로 광고를 미리 로드합니다.
6. "광고 보고 N회 충전하기"를 누르면 `RewardedAdManager.showWhenReady(...)`가 광고를 표시합니다. 로딩이 끝나지 않았으면 로딩 화면을 보여준 뒤 자동으로 표시합니다.
7. **영상 시청을 끝까지 완료한 경우에만** `onReward` 콜백 → `AppState.refillAndReturnToCamera()`가 호출되어 로컬 카운트를 충전하고 카메라 초기화면으로 돌아갑니다.
8. 광고를 불러오지 못했거나 보상 없이 닫으면 충전하지 않고 안내 문구를 표시합니다.

관련 코드:

```text
app/src/main/java/app/trashai/ads/RewardedAdManager.kt   # 로드/표시/보상 콜백
app/src/main/java/app/trashai/MainActivity.kt            # AdLimitReachedContent 버튼 연결
app/src/main/java/app/trashai/AppState.kt                # refillAndReturnToCamera
```

참고: 보상형 광고는 항상 채워지지(fill) 않을 수 있습니다. 광고가 준비되지 않으면 사용자는 "텍스트로 검색하기"로 계속 이용할 수 있습니다.

## 4. 인앱 업데이트 (앱 안에서 업데이트 안내)

새 버전 출시 시, 앱 안에서 사용자에게 업데이트를 안내하고 설치하도록 Google Play **In-App Updates**를 사용합니다.

위치:

```text
app/src/main/java/app/trashai/update/InAppUpdateManager.kt
app/src/main/java/app/trashai/MainActivity.kt   # checkForUpdate / onResume / 다이얼로그 연결
```

### 동작 방식 (Flexible)

1. 앱 실행 시 `InAppUpdateManager.checkForUpdate(...)`가 Play에 새 버전이 있는지 확인합니다.
2. 새 버전이 있으면 **백그라운드에서 다운로드**되고, 사용자는 앱을 계속 사용합니다.
3. 다운로드가 끝나면 `isDownloadReady`가 true가 되어 **"업데이트 준비 완료 — 지금 다시 시작"** 다이얼로그가 뜹니다.
4. 사용자가 "지금 다시 시작"을 누르면 `completeUpdate()`로 설치 후 앱이 재시작됩니다. "나중에"를 누르면 다음 기회에 다시 안내합니다.

### 새 버전을 내보내는 방법

1. `app/build.gradle.kts`에서 `versionCode`를 **반드시 올리고**(예: 1 → 2), `versionName`도 갱신합니다.
2. 릴리스 AAB를 빌드해 Google Play Console에 업로드/출시합니다.
3. Play에 새 `versionCode`가 게시되면, 사용자의 앱이 위 흐름에 따라 자동으로 업데이트를 안내합니다.

### 강제(즉시) 업데이트로 바꾸려면

필수 업데이트가 필요하면 `InAppUpdateManager`의 `AppUpdateType.FLEXIBLE`을 `AppUpdateType.IMMEDIATE`로 바꿉니다. 이 경우 전체 화면 차단형으로 업데이트를 완료해야 앱을 계속 쓸 수 있습니다.

### 중요 — 테스트 방법

- 인앱 업데이트는 **Google Play로 설치된 앱에서만 동작**합니다. Android Studio로 직접 설치하거나 APK 사이드로딩한 경우에는 동작하지 않습니다.
- 확인하려면 Play Console의 **내부 테스트(Internal testing)** 또는 **내부 앱 공유(Internal app sharing)** 트랙으로 두 개 이상의 `versionCode`를 올려 테스트합니다.

## 5. 배포 전 점검표

- `SHOW_AD_BANNER` 값이 의도한 상태인지 확인합니다.
- `AdIds.kt`의 `PROD_BANNER`, `PROD_REWARDED`가 실제 발급 ID로 교체됐는지 확인합니다. (기본 `0000000000` 플레이스홀더 잔존 여부 확인)
- 릴리스 빌드에서 테스트 ID가 나가지 않는지(`BuildConfig.DEBUG` 분기) 확인합니다.
- `limit_enabled` 기본값이 출시 정책과 맞는지 확인합니다.
- `daily_scan_limit` 값이 지나치게 낮아 초기 사용 경험을 해치지 않는지 확인합니다.
- 보상형 광고 시나리오를 각각 테스트합니다: 광고 미로드, 로딩 후 자동 표시, 중도 이탈(보상 없음), 시청 완료(충전).
- 개인정보 처리방침에 광고 SDK, 카메라, 위치 권한 사용 목적을 반영합니다.
