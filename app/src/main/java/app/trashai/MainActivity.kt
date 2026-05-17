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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.EnergySavingsLeaf
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trashai.ui.ItemRuleBody
import app.trashai.ui.InfoSheetContent
import app.trashai.ui.Tokens
import app.trashai.clarify.ClarificationChips
import app.trashai.vision.CameraScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    TrashAiApp()
                }
            }
        }
    }
}

@Composable
private fun TrashAiApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember { AppState(context.applicationContext) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.preloadDb() }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.fetchRegionFromGps()
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        // ---- Top half: camera (or captured image when result is up) -------
        Box(modifier = Modifier.weight(1.1f).fillMaxWidth()) {
            CameraScreen(
                onCaptureBytes = { bytes -> scope.launch { viewModel.onCapture(bytes) } },
                capturedJpeg = state.lastCapturedJpeg,
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
                IconChip(
                    icon = Icons.Outlined.Chat,
                    label = "AI 묻기",
                    onClick = { viewModel.startAskUser() },
                )
            }

            // "다시 스캔" premium button, bottom-left when a captured image is showing
            if (state.lastCapturedJpeg != null) {
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
                    // Detection-style Icon Box with Brackets & Camera
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. Camera icon maximized (Blackish)
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF1B1B1B),
                            modifier = Modifier.size(20.dp)
                        )
                        
                        // 2. Neon Brackets expanded to absolute edges
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
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
                            
                            // Green Glow (Neon)
                            drawPath(p, color = Tokens.NeonGreen, style = Stroke(width = strokeOuter, cap = StrokeCap.Round))
                            // White Highlight
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
                .weight(1f)
                .fillMaxWidth(),
            color = Tokens.Surface,
            shape = RoundedCornerShape(topStart = Tokens.Radius24, topEnd = Tokens.Radius24),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 3f)
                offset = if (scale == 1f) Offset.Zero else offset + panChange
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .padding(Tokens.Sp24)
                ) {
                    BottomCardContent(
                        state = state,
                        onPickItem = { viewModel.pickItem(it) },
                        onConfirmYes = { viewModel.confirmYes() },
                        onConfirmNo = { viewModel.confirmNo() },
                        onAskAi = { viewModel.startAskUser() },
                        onSubmitText = { viewModel.submitUserText(it) },
                        onDismiss = { viewModel.dismissSheet() },
                        onShowInfo = { viewModel.showInfo(it) },
                    )
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
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            when (val s = state.sheetState) {
                SheetState.Idle -> IdleCardContent(onShowInfo)

                is SheetState.Loading -> AnimatedLoadingScreen(s.message)

                is SheetState.Item -> {
                    ItemRuleBody(s.rule, regionLabel = state.regionLabel)
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Tokens.PrimaryGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Tokens.PrimaryGreen)
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(Tokens.Sp8))
                        Text("AI에게 직접 설명하기", fontWeight = FontWeight.SemiBold)
                    }
                }

                is SheetState.Confirming -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Tokens.PrimaryGreenSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = Tokens.PrimaryGreen, modifier = Modifier.size(20.dp))
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
                        s.rule.appSummary?.let { Text(it, color = Tokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                        s.rule.dischargeMethod?.let {
                            Spacer(Modifier.height(Tokens.Sp6))
                            Text(it, color = Tokens.TextSecondary, fontSize = Tokens.CaptionSize, lineHeight = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(Tokens.Sp16))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onConfirmNo) { Text("아니에요", color = Tokens.TextSecondary, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.width(Tokens.Sp8))
                        Button(
                            onClick = onConfirmYes,
                            colors = ButtonDefaults.buttonColors(containerColor = Tokens.PrimaryGreen),
                            shape = RoundedCornerShape(Tokens.Radius12),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(Tokens.Sp8))
                            Text("네, 맞아요", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is SheetState.AskUser -> AskUserContent(s, onSubmitText, onDismiss)

                is SheetState.Info -> InfoSheetContent(s.initialTab, onDismiss)

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
                    val tintColor = if (isInfo) Tokens.PrimaryGreen else Tokens.Danger
                    val bgColor = if (isInfo) Tokens.PrimaryGreenSoft else Color(0xFFFFEBEE)
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

        // Floating Scroll Arrow Button (첫 화면 Idle 상태일 때 표시, 스크롤 위치에 따라 방향 전환)
        if (state.sheetState == SheetState.Idle) {
            val isAtBottom = scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 20
            val infiniteTransition = rememberInfiniteTransition(label = "floating_arrow")
            val arrowOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = if (isAtBottom) -10f else 10f, // 위로 향할 때는 위쪽으로 바운스!
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "arrowOffset"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 8.dp)
                    .graphicsLayer { translationY = arrowOffset },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (isAtBottom) {
                                scrollState.animateScrollTo(0)
                            } else {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }
                    },
                    containerColor = Tokens.PrimaryGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
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
    
    // 1. Fast & Smooth Rotation for the Progress Arc
    val arcAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arcAngle"
    )

    // 2. Breathing Glow Scale for central text
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    // 3. Dot Indicator Animation (0, 1, 2 점멸)
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

    // 톤앤매너에서 벗어난 눈에 띄고 화려한 네온 컬러 조합
    val neonCyan = Color(0xFF00E5FF)
    val electricBlue = Color(0xFF2979FF)
    val hotMagenta = Color(0xFFFF007F)
    val neonGreen = Color(0xFF00E676)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            // Background Canvas for Dashed Track, Rotating Arc, and Particles
            androidx.compose.foundation.Canvas(modifier = Modifier.size(130.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.width / 2) * 0.75f
                
                // 1. Dashed Track Ring (점선 눈금 트랙)
                drawCircle(
                    color = neonCyan.copy(alpha = 0.25f),
                    radius = radius,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    ),
                    center = center
                )
                
                // 2. Rotating Progress Arc (빙글빙글 도는 굵고 화려한 아크)
                val arcCutout = 240f // 240도 길이의 아크
                rotate(degrees = arcAngle) {
                    drawArc(
                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                            colors = listOf(hotMagenta, electricBlue, neonCyan, hotMagenta),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = arcCutout,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        topLeft = Offset(center.x - radius, center.y - radius)
                    )

                    // 3. Glowing Dot at the leading edge of the arc
                    val leadingAngle = Math.toRadians(arcCutout.toDouble()).toFloat()
                    val dotX = center.x + radius * kotlin.math.cos(leadingAngle)
                    val dotY = center.y + radius * kotlin.math.sin(leadingAngle)
                    
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

                // 4. Floating / Rotating Particles (주변 파티클 점들)
                rotate(degrees = -arcAngle * 0.5f) {
                    val pRadius1 = radius * 1.25f
                    val pRadius2 = radius * 1.4f
                    // Particle 1 (Top Right)
                    drawCircle(
                        color = hotMagenta,
                        radius = 2.5f.dp.toPx(),
                        center = Offset(center.x + pRadius1 * 0.7f, center.y - pRadius1 * 0.7f)
                    )
                    // Particle 2 (Bottom Left)
                    drawCircle(
                        color = neonCyan,
                        radius = 3.dp.toPx(),
                        center = Offset(center.x - pRadius2 * 0.8f, center.y + pRadius2 * 0.5f)
                    )
                    // Particle 3 (Top Left)
                    drawCircle(
                        color = neonGreen,
                        radius = 2.dp.toPx(),
                        center = Offset(center.x - pRadius1 * 0.6f, center.y - pRadius1 * 0.8f)
                    )
                }
            }
            
            // Central Big "AI" Text (첨부 이미지처럼 중앙에 큰 AI 텍스트)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Tokens.Surface.copy(alpha = 0.95f))
                    .border(2.dp, neonCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = neonGreen,
                    modifier = Modifier.graphicsLayer {
                        scaleX = breathingScale
                        scaleY = breathingScale
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // AI Analyzing Text ("AI 분석중")
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Tokens.TextPrimary,
            letterSpacing = 0.5.sp
        )
        
        Spacer(Modifier.height(4.dp))
        
        // Subtitle ("잠시만 기다려 주세요...")
        Text(
            text = "잠시만 기다려 주세요...",
            fontSize = 13.sp,
            color = Tokens.TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(16.dp))

        // Bottom 3 Dots Indicator (첨부 이미지의 하단 점멸 도트)
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
                        .background(if (isActive) neonGreen else Tokens.Divider)
                )
            }
        }
    }
}

@Composable
private fun IdleCardContent(onShowInfo: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Header with Professional Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = Tokens.Sp16)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(Tokens.Radius12))
                    .background(Tokens.PrimaryGreenSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Tokens.PrimaryGreen,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(Tokens.Sp16))
            Column {
                Text(
                    "AI 분석 대기 중",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Tokens.TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "사물을 카메라 중심에 맞춰주세요",
                    fontSize = 13.sp,
                    color = Tokens.TextSecondary,
                )
            }
        }

        // 2. Structured Instruction Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius16))
                .background(Tokens.SurfaceMuted)
                .padding(Tokens.Sp16)
        ) {
            Text(
                "사용 가이드",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Tokens.TextPrimary,
                modifier = Modifier.padding(bottom = Tokens.Sp12)
            )
            
            val steps = listOf(
                "인식" to "사물 주위에 녹색 박스가 나타납니다.",
                "선택" to "박스를 탭하거나 영역을 직접 드래그하세요.",
                "분석" to "AI가 재질을 판별하고 배출법을 안내합니다."
            )
            
            steps.forEachIndexed { index, (label, desc) ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.width(60.dp),
                        color = Tokens.PrimaryGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = desc,
                        modifier = Modifier.weight(1f),
                        color = Tokens.TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
                if (index < steps.size - 1) {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Tokens.Divider.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(Modifier.height(Tokens.Sp16))

        // Intuitive Legal Document Buttons (개인정보 처리방침, 이용약관 2개만 표시)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Tokens.Sp8)
        ) {
            val legalItems = listOf(
                "개인정보 처리방침" to Icons.Outlined.Shield,
                "이용 약관" to Icons.Outlined.Article
            )
            legalItems.forEach { (title, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Tokens.Radius12))
                        .background(Tokens.SurfaceMuted)
                        .clickable { onShowInfo(title) }
                        .padding(horizontal = Tokens.Sp16, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Tokens.PrimaryGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Tokens.Sp12))
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Tokens.TextPrimary, modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = Tokens.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(Tokens.Sp24))
        Text(
            text = "© 2026 RecycleAI. All rights reserved.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                    .background(Tokens.PrimaryGreenSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Tokens.PrimaryGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(Tokens.Sp12))
            Column {
                Text("AI에게 설명하기", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Tokens.TextPrimary)
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
                            text = if (isAi) "AI" else "ME",
                            color = if (isAi) Tokens.PrimaryGreen else Tokens.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp).padding(top = 2.dp)
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
                unfocusedContainerColor = Tokens.SurfaceMuted,
                focusedBorderColor = Tokens.PrimaryGreen,
                unfocusedBorderColor = Color.Transparent,
            )
        )
        Spacer(Modifier.height(Tokens.Sp16))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Tokens.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(Tokens.Sp8))
            Button(
                onClick = {
                    val v = text.trim()
                    if (v.isNotEmpty()) {
                        text = ""
                        onSubmit(v)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Tokens.PrimaryGreen),
                shape = RoundedCornerShape(Tokens.Radius12),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Tokens.Sp8))
                Text("보내기", fontWeight = FontWeight.Bold)
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
                tint = Tokens.PrimaryGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(Tokens.Sp8))
            Text("정보가 실제와 다른가요?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Tokens.TextPrimary)
        }
        Spacer(Modifier.height(Tokens.Sp8))
        Text(
            "재질이나 상태를 묘사해주시면 AI가 다시 안내해 드립니다.",
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
                focusedBorderColor = Tokens.PrimaryGreen,
                unfocusedBorderColor = Color.Transparent,
            ),
            trailingIcon = {
                IconButton(onClick = {
                    if (text.isNotBlank()) {
                        val v = text
                        text = ""
                        onSubmit(v)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "다시 묻기", tint = Tokens.PrimaryGreen)
                }
            }
        )
    }
}
