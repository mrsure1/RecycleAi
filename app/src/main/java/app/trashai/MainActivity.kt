package app.trashai

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trashai.clarify.ClarificationChips
import app.trashai.ui.CommonGuideSection
import app.trashai.ui.InfoSheetContent
import app.trashai.ui.ItemRuleBody
import app.trashai.ui.PinchZoomScrollColumn
import app.trashai.ui.RegionOfficialInfoSection
import app.trashai.ui.TextWithDialablePhones
import app.trashai.ui.Tokens
import app.trashai.vision.CameraScreen
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val inAppUpdate by lazy { app.trashai.update.InAppUpdateManager(this) }

    // 인앱 업데이트 플로 결과 런처 (사용자가 취소해도 다음 실행 때 다시 안내)
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // AdMob SDK 초기화 후 보상형 광고 미리 로드
        com.google.android.gms.ads.MobileAds.initialize(this) {
            app.trashai.ads.RewardedAdManager.preload(applicationContext)
        }

        // 새 버전이 있으면 백그라운드 다운로드(Flexible) 시작
        inAppUpdate.checkForUpdate(updateLauncher)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    TrashAiApp(updateManager = inAppUpdate)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 자리를 비운 사이 다운로드가 끝났다면 설치 안내를 다시 띄움
        inAppUpdate.onResume()
    }

    override fun onDestroy() {
        inAppUpdate.unregister()
        super.onDestroy()
    }
}

@Composable
private fun TrashAiApp(updateManager: app.trashai.update.InAppUpdateManager? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember { AppState(context.applicationContext) }
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val keepResultOnNextStop = remember { mutableStateOf(false) }

    // 인앱 업데이트 다운로드가 끝나면 "다시 시작" 안내를 표시
    if (updateManager?.isDownloadReady == true) {
        UpdateReadyDialog(
            onRestart = { updateManager.completeUpdate() },
            onLater = { updateManager.dismissDownloadReady() },
        )
    }

    // 백그라운드/종료 후 재개 시 분석 결과·캡처 이미지를 초기화해 새 스캔부터 시작
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (keepResultOnNextStop.value) {
                    keepResultOnNextStop.value = false
                } else {
                    viewModel.resetScanSession()
                }
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                // 백그라운드에서 홈 화면을 보거나 다른 일을 하다가 앱으로 복귀했을 때 Firebase 최신 설정을 실시간 동기화
                app.trashai.data.RemoteConfigManager.fetchAndActivate(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.fetchRegionFromGps()
    }

    LaunchedEffect(Unit) { 
        viewModel.preloadDb() 
        // 앱 시작 시 Firebase Remote Config 서버에서 최신 설정값(한도 제한 여부 등)을 즉시 Fetch합니다.
        app.trashai.data.RemoteConfigManager.fetchAndActivate(context)
        locationPermLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val isResultView = state.sheetState is SheetState.Item ||
        state.sheetState is SheetState.Clarify ||
        state.sheetState is SheetState.Confirming ||
        state.sheetState is SheetState.AdLimitReached
    val topWeight by animateFloatAsState(
        targetValue = if (isResultView) 0.25f else 1.0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "topWeight"
    )
    val bottomWeight by animateFloatAsState(
        targetValue = if (isResultView) 0.75f else 1.0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "bottomWeight"
    )

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        // ---- Top half: camera (or captured image when result is up) -------
        Box(modifier = Modifier.weight(topWeight).fillMaxWidth()) {
            CameraScreen(
                onCaptureBytes = { bytes, label -> scope.launch { viewModel.onCapture(bytes, label) } },
                capturedJpeg = state.lastCapturedJpeg,
                sigunguCode = state.regionOrdinance?.regionCode ?: "1100000000",
            )

            // Top status bar — pin pill (left) + AI ask (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = Tokens.Sp16, end = Tokens.Sp16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Sp8),
            ) {
                LocationPill(
                    label = state.regionLabel,
                    loading = state.regionLoading,
                    onTap = {
                        locationPermLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                
                // 원격 한도 설정 활성화 시에만 고급스러운 글래스모피즘 남은 횟수 배지 노출
                if (app.trashai.data.RemoteConfigManager.limitEnabled) {
                    val todayCount = app.trashai.data.ScanLimitManager.getTodayScanCount(context)
                    val limit = app.trashai.data.RemoteConfigManager.dailyScanLimit
                    val remaining = (limit - todayCount).coerceAtLeast(0)
                    
                    RemainingScanPill(
                        remaining = remaining,
                        limit = limit
                    )
                }

                IconChip(
                    icon = Icons.Outlined.Chat,
                    label = "인공지능 묻기",
                    onClick = { viewModel.startAskUser() },
                )
            }

            // "다시 스캔" premium button, bottom-left when not in Idle state
            if (state.sheetState !is SheetState.Idle) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Tokens.Sp16)
                        .clip(RoundedCornerShape(Tokens.Radius24))
                        .background(Tokens.Scrim)
                        .clickable { viewModel.dismissSheet() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF1B1B1B),
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width; val h = size.height
                            val len = 10.dp.toPx()
                            val rad = 4.dp.toPx()
                            val strokeOuter = 4f
                            val strokeInner = 2f
                            
                            val p = Path().apply {
                                // TL
                                moveTo(0f, len); lineTo(0f, rad); quadraticBezierTo(0f, 0f, rad, 0f); lineTo(len, 0f)
                                // TR
                                moveTo(w - len, 0f); lineTo(w - rad, 0f); quadraticBezierTo(w, 0f, w, rad); lineTo(w, len)
                                // BR
                                moveTo(w, h - len); lineTo(w, h - rad); quadraticBezierTo(w, h, w - rad, h); lineTo(w - len, h)
                                // BL
                                moveTo(len, h); lineTo(rad, h); quadraticBezierTo(0f, h, 0f, h - rad); lineTo(0f, h - len)
                            }
                            
                            drawPath(p, color = Tokens.NeonGreen, style = Stroke(width = strokeOuter, cap = StrokeCap.Round))
                            drawPath(p, color = Color.White, style = Stroke(width = strokeInner, cap = StrokeCap.Round))
                        }
                    }
                    Spacer(Modifier.width(Tokens.Sp8))
                    Text(
                        "다시 스캔",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }

        // ---- Bottom half: info card --------------------------------------
        Surface(
            modifier = Modifier
                .weight(bottomWeight)
                .fillMaxWidth(),
            color = Tokens.Surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = Tokens.Radius24, topEnd = Tokens.Radius24),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            var sheetScrollState by remember { mutableStateOf<ScrollState?>(null) }
            var suppressScrollShrink by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                PinchZoomScrollColumn(
                    modifier = Modifier.fillMaxSize(),
                    resetKey = state.sheetState,
                    onScrollState = { sheetScrollState = it },
                    onZoomedChange = { suppressScrollShrink = it },
                ) {
                    Column(Modifier.padding(Tokens.Sp24)) {
                        BottomCardContent(
                            state = state,
                            onPickItem = { viewModel.pickItem(it) },
                            onConfirmYes = { viewModel.confirmYes() },
                            onConfirmNo = { viewModel.confirmNo() },
                            onAskAi = { viewModel.startAskUser() },
                            onSubmitText = { viewModel.submitUserText(it) },
                            onDismiss = { viewModel.dismissSheet() },
                            onShowInfo = { viewModel.showInfo(it) },
                            onTabChange = { scope.launch { sheetScrollState?.scrollTo(0) } },
                            onRefill = { _, _ -> viewModel.refillAndReturnToCamera() },
                            onBeforeDial = { keepResultOnNextStop.value = true },
                            scrollValue = if (suppressScrollShrink) {
                                0
                            } else {
                                sheetScrollState?.value ?: 0
                            },
                        )
                    }
                }

                // Floating Scroll Arrow Button (화면 하단 고정 배치)
                if (state.sheetState == SheetState.Idle) {
                    val scrollState = sheetScrollState
                    val isAtBottom = scrollState != null &&
                        scrollState.maxValue > 0 &&
                        scrollState.value >= scrollState.maxValue - 20
                    val infiniteTransition = rememberInfiniteTransition(label = "floating_arrow")
                    val arrowOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = if (isAtBottom) -10f else 10f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "arrowOffset"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .graphicsLayer { translationY = arrowOffset },
                        contentAlignment = Alignment.Center
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    val s = sheetScrollState ?: return@launch
                                    if (isAtBottom) {
                                        s.animateScrollTo(0)
                                    } else {
                                        s.animateScrollTo(s.maxValue)
                                    }
                                }
                            },
                            containerColor = Tokens.Primary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isAtBottom) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = if (isAtBottom) "위로 스크롤" else "아래로 스크롤",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPill(
    label: String,
    loading: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Tokens.Radius24))
            .background(Tokens.Scrim)
            .clickable(onClick = onTap)
            .padding(horizontal = Tokens.Sp12, vertical = Tokens.Sp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(Tokens.Sp6))
        Text(
            text = if (loading) "위치 가져오는 중…" else label,
            color = Color.White,
            fontSize = Tokens.CaptionSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
        )
        Spacer(Modifier.width(Tokens.Sp8))
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "위치 갱신",
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun IconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Tokens.Radius24))
            .background(Tokens.Scrim)
            .clickable(onClick = onClick)
            .padding(horizontal = Tokens.Sp12, vertical = Tokens.Sp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(Tokens.Sp6))
        Text(label, color = Color.White, fontSize = Tokens.TagSize, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BottomCardContent(
    state: AppUiState,
    onPickItem: (String) -> Unit,
    onConfirmYes: () -> Unit,
    onConfirmNo: () -> Unit,
    onAskAi: () -> Unit,
    onSubmitText: (String) -> Unit,
    onDismiss: () -> Unit,
    onShowInfo: (String) -> Unit,
    onTabChange: () -> Unit = {},
    onRefill: (ByteArray, String?) -> Unit,
    onBeforeDial: () -> Unit = {},
    scrollValue: Int = 0,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (val s = state.sheetState) {
            SheetState.Idle -> IdleCardContent(onShowInfo)

            is SheetState.Loading -> AnimatedLoadingScreen(s.message)

            is SheetState.AdLimitReached -> {
                AdLimitReachedContent(
                    jpegBytes = s.jpegBytes,
                    rawLabel = s.rawLabel,
                    onRefill = { onRefill(s.jpegBytes, s.rawLabel) },
                    onSearchManually = onAskAi
                )
            }

            is SheetState.Item -> {
                ItemRuleBody(
                    s.rule,
                    regionLabel = state.regionLabel,
                    commonGuide = s.commonGuide,
                    regionOrdinance = state.regionOrdinance,
                    regionExtras = state.regionExtras,
                    scrollValue = scrollValue,
                    onBeforeDial = onBeforeDial,
                )
                if (s.alternates.isNotEmpty()) {
                    Spacer(Modifier.height(Tokens.Sp16))
                    ClarificationChips(
                        candidates = s.alternates,
                        onPick = { onPickItem(it.itemId) },
                        headline = "다른 후보",
                        hint = null,
                    )
                }
                Spacer(Modifier.height(Tokens.Sp24))
                CorrectionInput(onSubmit = onSubmitText)
            }

            is SheetState.Clarify -> {
                ClarificationChips(
                    candidates = s.candidates,
                    onPick = { onPickItem(it.itemId) },
                )
                Spacer(Modifier.height(Tokens.Sp16))
                OutlinedButton(
                    onClick = onAskAi,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Tokens.Radius12),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Tokens.Primary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Tokens.Primary)
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Tokens.Sp8))
                    Text("인공지능에게 직접 설명하기", fontWeight = FontWeight.SemiBold)
                }
            }

            is SheetState.Confirming -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Tokens.PrimarySoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = Tokens.Primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(Tokens.Sp12))
                    Column {
                        Text("이게 맞나요?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Tokens.TextPrimary)
                        Spacer(Modifier.height(Tokens.Sp4))
                        Text("${s.sourceLabel} · ${s.rule.itemName}", fontSize = Tokens.CaptionSize, color = Tokens.TextSecondary)
                    }
                }
                Spacer(Modifier.height(Tokens.Sp16))

                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.Radius12)).background(Tokens.SurfaceMuted).padding(Tokens.Sp16)
                ) {
                    // '이게 맞나요?' 화면에서는 요약된 분리방법 1가지만 단순 노출
                    val displayText = s.rule.appSummary ?: s.rule.dischargeMethod ?: "분리배출 정보가 없습니다."
                    TextWithDialablePhones(
                        text = displayText,
                        style = androidx.compose.ui.text.TextStyle(
                            color = Tokens.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp,
                        ),
                        onBeforeDial = onBeforeDial,
                    )
                }
                Spacer(Modifier.height(Tokens.Sp16))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onConfirmNo) { Text("아니에요", color = Tokens.TextSecondary, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.width(Tokens.Sp8))
                    Button(
                        onClick = onConfirmYes,
                        colors = ButtonDefaults.buttonColors(containerColor = Tokens.Primary),
                        shape = RoundedCornerShape(Tokens.Radius12),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(Tokens.Sp8))
                        Text("네, 맞아요", fontWeight = FontWeight.Bold)
                    }
                }
            }

            is SheetState.AskUser -> AskUserContent(s, onSubmitText, onDismiss)

            is SheetState.Info -> InfoSheetContent(s.initialTab, onDismiss, onTabChange)

            is SheetState.Empty -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = Color(0xFFB26A00), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(Tokens.Sp12))
                    Column {
                        Text("매칭되는 품목이 없어요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Tokens.TextPrimary)
                        Spacer(Modifier.height(Tokens.Sp4))
                        Text(s.detail, fontSize = Tokens.CaptionSize, color = Tokens.TextSecondary)
                    }
                }
                Spacer(Modifier.height(Tokens.Sp24))
                CorrectionInput(onSubmit = onSubmitText)
            }

            is SheetState.Error -> {
                val isInfo = s.message.startsWith("✅")
                val tintColor = if (isInfo) Tokens.Primary else Tokens.DangerText
                val bgColor = if (isInfo) Tokens.PrimarySoft else Tokens.Danger
                val icon = if (isInfo) Icons.Outlined.Info else Icons.Outlined.ErrorOutline

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(bgColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(Tokens.Sp12))
                    Text(if (isInfo) "정보" else "오류", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = tintColor)
                }
                Spacer(Modifier.height(Tokens.Sp16))
                Text(s.message, color = Tokens.TextPrimary, fontSize = Tokens.BodySize)
            }
        }
    }
}

@Composable
private fun CardHeader(title: String, subtitle: String?, accent: Color = Tokens.TextPrimary) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent)
    subtitle?.let {
        Spacer(Modifier.height(Tokens.Sp4))
        Text(it, fontSize = Tokens.CaptionSize, color = Tokens.TextSecondary)
    }
}

@Composable
private fun AnimatedLoadingScreen(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_premium_loading")
    
    val arcAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arcAngle"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    val dotProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotProgress"
    )
    val dotIndex = dotProgress.toInt()

    // 다크 네이비 테마에 어울리는 고급스럽고 사이버틱한 블루/네이비 네온 컬러 조합
    val neonCyan = Color(0xFF00E5FF)
    val electricBlue = Tokens.Accent
    val darkNavy = Tokens.Primary
    val neonGreen = Tokens.NeonGreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            Canvas(modifier = Modifier.size(130.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.width / 2) * 0.75f
                
                drawCircle(
                    color = neonCyan.copy(alpha = 0.25f),
                    radius = radius,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    ),
                    center = center
                )
                
                val arcCutout = 240f
                rotate(degrees = arcAngle) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(darkNavy, electricBlue, neonCyan, darkNavy),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = arcCutout,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center.x - radius, center.y - radius)
                    )

                    val leadingAngle = Math.toRadians(arcCutout.toDouble()).toFloat()
                    val dotX = center.x + radius * cos(leadingAngle)
                    val dotY = center.y + radius * sin(leadingAngle)
                    
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                    drawCircle(
                        color = neonCyan.copy(alpha = 0.6f),
                        radius = 10.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                rotate(degrees = -arcAngle * 0.5f) {
                    val pRadius1 = radius * 1.25f
                    val pRadius2 = radius * 1.4f
                    drawCircle(
                        color = electricBlue,
                        radius = 2.5f.dp.toPx(),
                        center = Offset(center.x + pRadius1 * 0.7f, center.y - pRadius1 * 0.7f)
                    )
                    drawCircle(
                        color = neonCyan,
                        radius = 3.dp.toPx(),
                        center = Offset(center.x - pRadius2 * 0.8f, center.y + pRadius2 * 0.5f)
                    )
                    drawCircle(
                        color = neonGreen,
                        radius = 2.dp.toPx(),
                        center = Offset(center.x - pRadius1 * 0.6f, center.y - pRadius1 * 0.8f)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Tokens.Surface.copy(alpha = 0.95f))
                    .border(2.dp, neonCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "인공지능",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Tokens.Primary,
                    modifier = Modifier.graphicsLayer {
                        scaleX = breathingScale
                        scaleY = breathingScale
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Tokens.TextPrimary,
            letterSpacing = 0.5.sp
        )
        
        Spacer(Modifier.height(4.dp))
        
        Text(
            text = "잠시만 기다려 주세요...",
            fontSize = 13.sp,
            color = Tokens.TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val isActive = index == dotIndex
                Box(
                    modifier = Modifier
                        .size(if (isActive) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Tokens.Primary else Tokens.Divider)
                )
            }
        }
    }
}

@Composable
private fun IdleCardContent(onShowInfo: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Premium Header Banner (AI 분석 대기 중)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius16))
                .background(Tokens.PrimarySoft)
                .border(1.dp, Tokens.Divider, RoundedCornerShape(Tokens.Radius16))
                .padding(Tokens.Sp16)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(Tokens.Radius12))
                    .background(Tokens.RecycleGreenSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Recycling,
                    contentDescription = null,
                    tint = Tokens.RecycleGreen,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(Tokens.Sp16))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "인공지능 분석 대기 중",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Tokens.TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "사물을 카메라 중심에 맞춰주세요",
                    fontSize = 12.sp,
                    color = Tokens.TextSecondary,
                    lineHeight = 16.sp
                )
            }
            // Pulse Indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Tokens.RecycleGreen)
            )
        }

        Spacer(Modifier.height(Tokens.Sp20))

        // 2. Professional 3-Step Process Guide (사용 가이드 - 앞에 책/가이드 아이콘 추가)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = Tokens.Sp12, start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = Tokens.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(Tokens.Sp6))
            Text(
                "사용 가이드",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Tokens.TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius16))
                .background(Tokens.Surface)
                .border(1.dp, Tokens.Divider, RoundedCornerShape(Tokens.Radius16)),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            val steps = listOf(
                Triple("1단계 : 인식", "사물 주위에 녹색 박스가 나타납니다.", Icons.Outlined.CenterFocusWeak),
                Triple("2단계 : 선택", "초록색 박스를 터치하거나, 버릴 물건을 손가락으로 직접 화면에 대고 네모나게 그려보세요.\n(사용자가 선택한 네모는 주황색 박스입니다)", Icons.Outlined.TouchApp),
                Triple("3단계 : 분석", "인공지능이 재질을 판별하고 배출법을 안내합니다.", Icons.Outlined.Analytics)
            )

            steps.forEachIndexed { index, (title, desc, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Tokens.Surface)
                        .padding(Tokens.Sp16),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Tokens.PrimarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = Tokens.Primary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(Tokens.Sp12))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Tokens.TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (title.startsWith("2단계")) {
                                buildAnnotatedString {
                                    val target = "주황색"
                                    val startIndex = desc.indexOf(target)
                                    if (startIndex >= 0) {
                                        append(desc.substring(0, startIndex))
                                        withStyle(style = SpanStyle(color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)) {
                                            append(target)
                                        }
                                        append(desc.substring(startIndex + target.length))
                                    } else {
                                        append(desc)
                                    }
                                }
                            } else {
                                AnnotatedString(desc)
                            },
                            fontSize = 15.sp,
                            color = Tokens.TextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }
                if (index < steps.size - 1) {
                    HorizontalDivider(color = Tokens.Divider)
                }
            }
        }

        Spacer(Modifier.height(Tokens.Sp16))

        // 3. Quick Tip Banner (직관적이고 쉬운 어투)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius12))
                .background(Tokens.AccentSoft)
                .border(1.dp, Tokens.Accent.copy(alpha = 0.2f), RoundedCornerShape(Tokens.Radius12))
                .padding(Tokens.Sp16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = Tokens.Accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Tokens.Sp12))
            Text(
                "팁: 헷갈리거나 복잡한 쓰레기는 우측 상단의 '인공지능 묻기' 버튼을 눌러 직접 질문해보세요.",
                fontSize = 15.sp,
                color = Tokens.TextPrimary,
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(Tokens.Sp24))

        // 4. Elegant Legal Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius12))
                .background(Tokens.SurfaceMuted)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val legalItems = listOf(
                "개인정보 처리방침" to Icons.Outlined.Shield,
                "이용 약관" to Icons.Outlined.Article
            )
            legalItems.forEach { (title, icon) ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Tokens.Radius8))
                        .clickable { onShowInfo(title) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Tokens.TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Tokens.Sp8))
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Tokens.TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(Tokens.Sp24))
        Text(
            text = "© 2026 RecycleAI. All rights reserved.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = Tokens.TextSecondary.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AskUserContent(
    s: SheetState.AskUser,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Tokens.PrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Tokens.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(Tokens.Sp12))
            Column {
                Text("인공지능에게 설명하기", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Tokens.TextPrimary)
                Spacer(Modifier.height(Tokens.Sp4))
                Text(s.prompt, fontSize = Tokens.CaptionSize, color = Tokens.TextSecondary)
            }
        }
        Spacer(Modifier.height(Tokens.Sp16))

        if (s.history.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Tokens.Radius12))
                    .background(Tokens.SurfaceMuted)
                    .padding(Tokens.Sp12),
                verticalArrangement = Arrangement.spacedBy(Tokens.Sp8)
            ) {
                for (turn in s.history.takeLast(4)) {
                    val isAi = turn.from == SheetState.Speaker.Ai
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = if (isAi) "인공지능" else "나",
                            color = if (isAi) Tokens.Primary else Tokens.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(48.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = turn.text,
                            color = Tokens.TextPrimary,
                            fontSize = Tokens.CaptionSize,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(Tokens.Sp12))
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("예: '뚜껑이 안 빠지는 화장품 통'", color = Tokens.TextSecondary, fontSize = Tokens.BodySize) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(Tokens.Radius16),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Tokens.Surface,
                unfocusedContainerColor = Tokens.Surface, // 비포커스 시에도 깔끔하게 흰색 배경 유지
                focusedBorderColor = Tokens.Primary,
                unfocusedBorderColor = Tokens.Divider, // 테두리를 명확하게 구분할 수 있도록 설정
            ),
            trailingIcon = {
                val v = text.trim()
                if (v.isNotEmpty()) {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            text = ""
                            onSubmit(v)
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .background(Tokens.Primary, RoundedCornerShape(percent = 50))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "보내기",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        )
        Spacer(Modifier.height(Tokens.Sp8))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Tokens.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CorrectionInput(onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius16))
            .background(Tokens.SurfaceMuted)
            .padding(Tokens.Sp16)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Tokens.Primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(Tokens.Sp8))
            Text("정보가 실제와 다른가요?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Tokens.TextPrimary)
        }
        Spacer(Modifier.height(Tokens.Sp8))
        Text(
            "재질이나 상태를 묘사해주시면 인공지능이 다시 안내해 드립니다.",
            fontSize = Tokens.CaptionSize,
            color = Tokens.TextSecondary
        )
        Spacer(Modifier.height(Tokens.Sp12))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("예: 컵 본체는 종이재질이야", color = Tokens.TextSecondary, fontSize = Tokens.CaptionSize) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            shape = RoundedCornerShape(Tokens.Radius12),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Tokens.Surface,
                unfocusedContainerColor = Tokens.Surface,
                focusedBorderColor = Tokens.Primary,
                unfocusedBorderColor = Tokens.Divider, // 테두리를 명확하게 구분할 수 있도록 설정
            ),
            trailingIcon = {
                IconButton(onClick = {
                    if (text.isNotBlank()) {
                        val v = text
                        text = ""
                        onSubmit(v)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "다시 묻기", tint = Tokens.Primary)
                }
            }
        )
    }
}

@Composable
private fun UpdateReadyDialog(
    onRestart: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = {
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null,
                tint = Tokens.RecycleGreen,
            )
        },
        title = { Text("업데이트 준비 완료", fontWeight = FontWeight.ExtraBold) },
        text = {
            Text("새 버전 다운로드가 끝났어요. 지금 다시 시작하면 최신 버전으로 업데이트됩니다.")
        },
        confirmButton = {
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Tokens.RecycleGreen),
            ) {
                Text("지금 다시 시작", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text("나중에") }
        },
    )
}

private fun android.content.Context.findActivityOrNull(): android.app.Activity? {
    var ctx: android.content.Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * 일일 스캔 횟수 제한에 도달했을 때 일반 안내와 확실히 구분되는 충전 팝업형 카드입니다.
 */
@Composable
private fun AdLimitReachedContent(
    jpegBytes: ByteArray,
    rawLabel: String?,
    onRefill: () -> Unit,
    onSearchManually: () -> Unit
) {
    val context = LocalContext.current
    var isLoadingAd by remember { mutableStateOf(false) }
    var adFailed by remember { mutableStateOf(false) }
    val limit = app.trashai.data.RemoteConfigManager.dailyScanLimit

    // 화면 진입 시 보상형 광고를 미리 로드해 둡니다.
    LaunchedEffect(Unit) {
        app.trashai.ads.RewardedAdManager.preload(context.applicationContext)
    }

    val watchAd: () -> Unit = {
        val activity = context.findActivityOrNull()
        if (activity == null) {
            onRefill()
        } else {
            adFailed = false
            app.trashai.ads.RewardedAdManager.showWhenReady(
                activity = activity,
                onReward = {
                    isLoadingAd = false
                    onRefill()
                },
                onUnavailable = {
                    isLoadingAd = false
                    adFailed = true
                },
                onWaiting = { isLoadingAd = true },
            )
        }
    }

    if (isLoadingAd) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius24))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                    )
                )
                .padding(Tokens.Sp24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = Tokens.NeonGreen,
                strokeWidth = 5.dp,
                trackColor = Color.White.copy(alpha = 0.18f)
            )
            Spacer(Modifier.height(Tokens.Sp16))
            Text(
                text = "광고 불러오는 중",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(Tokens.Sp4))
            Text(
                text = "잠시만 기다려 주세요. 광고 시청이 끝나면 분석 기회 ${limit}회가 충전됩니다.",
                fontSize = Tokens.CaptionSize,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Tokens.Sp8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFFBEB), Color(0xFFFFF7ED))
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFF59E0B).copy(alpha = 0.38f),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .padding(Tokens.Sp20),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = Tokens.Sp12, vertical = Tokens.Sp6),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = Tokens.WarningText,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(Tokens.Sp6))
                            Text(
                                text = "오늘 무료 분석 0회 남음",
                                color = Tokens.WarningText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(Tokens.Sp16))

                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFFBBF24), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    Spacer(Modifier.height(Tokens.Sp16))

                    Text(
                        text = "AI 분석 기회를 모두 사용했어요",
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Tokens.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Tokens.Sp8))
                    Text(
                        text = "광고를 한 번 시청하면 오늘 분석 기회 ${limit}회가 다시 충전되고, 카메라 화면에서 새로 스캔할 수 있습니다.",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Tokens.TextSecondary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(Tokens.Sp20))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Tokens.Radius16))
                            .background(Color.White.copy(alpha = 0.82f))
                            .padding(Tokens.Sp12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LimitStat(label = "오늘 제공", value = "${limit}회")
                        LimitDivider()
                        LimitStat(label = "현재 남음", value = "0회", valueColor = Tokens.DangerText)
                        LimitDivider()
                        LimitStat(label = "광고 후", value = "${limit}회", valueColor = Tokens.RecycleGreen)
                    }
                }
            }

            Spacer(Modifier.height(Tokens.Sp16))

            Button(
                onClick = watchAd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(Tokens.Radius16),
                contentPadding = PaddingValues(horizontal = Tokens.Sp16)
            ) {
                Icon(
                    imageVector = Icons.Outlined.OndemandVideo,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(Tokens.Sp8))
                Text(
                    "광고 보고 ${limit}회 충전하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            if (adFailed) {
                Spacer(Modifier.height(Tokens.Sp8))
                Text(
                    text = "지금은 광고를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.",
                    fontSize = 12.sp,
                    color = Tokens.DangerText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(Tokens.Sp8))

            OutlinedButton(
                onClick = onSearchManually,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(Tokens.Radius16),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Tokens.Primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, Tokens.Divider)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Tokens.Sp8))
                Text("사진 대신 텍스트로 검색하기", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(Tokens.Sp8))

            Text(
                text = "텍스트 검색은 무료로 계속 이용할 수 있어요.",
                fontSize = 12.sp,
                color = Tokens.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RowScope.LimitStat(
    label: String,
    value: String,
    valueColor: Color = Tokens.TextPrimary,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Tokens.TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 17.sp,
            color = valueColor,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun LimitDivider() {
    Box(
        modifier = Modifier
            .height(34.dp)
            .width(1.dp)
            .background(Tokens.Divider),
    )
}

/**
 * 한도 제한 활성화 시 노출되는 글래스모피즘(Glassmorphism) 스타일의 남은 횟수 표시 배지입니다.
 * 
 * [나중에 디자인을 마음대로 수정하고 싶을 때 가이드]
 * 1. 배경 투명도(Alpha): White.copy(alpha = 0.12f) 부분의 수치를 0.0f(완전투명) ~ 1.0f(불투명) 사이로 조절하세요.
 * 2. 빛반사 테두리 선명도: border(0.5.dp, Color.White.copy(alpha = 0.25f)) 의 두께(dp)와 투명도(alpha)를 조절하세요.
 * 3. 둥글기 강도(Rounded Corners): RoundedCornerShape(Tokens.Radius24) 의 토큰 값을 취향대로 변경하세요.
 * 4. 텍스트 색상 및 스타일: Color.White, fontSize = 11.sp, FontWeight.ExtraBold 부분을 원하시는 값으로 자유롭게 수정 가능합니다.
 * 5. 아이콘 테마 및 색상: tint 속성을 통해 잔여 횟수가 1회 이하로 떨어졌을 때 경고 색상(WarningText)으로 분기되는 규칙을 변경할 수 있습니다.
 */
@Composable
private fun RemainingScanPill(
    remaining: Int,
    limit: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Tokens.Radius24))
            .background(Color(0xFFFFC0CB).copy(alpha = 0.15f))
            .border(0.5.dp, Color(0xFFFFC0CB).copy(alpha = 0.3f), RoundedCornerShape(Tokens.Radius24))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Analytics,
            contentDescription = null,
            tint = if (remaining <= 1) Tokens.WarningText else Tokens.NeonGreen,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "오늘 분석 기회: ${remaining}회",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        )
    }
}
