package app.trashai.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trashai.data.CommonGuide
import app.trashai.data.ItemRule
import app.trashai.data.MoisDisposalRule
import app.trashai.data.RegionContact
import app.trashai.data.RegionExtras

@Composable
fun ItemRuleBody(
    rule: ItemRule, 
    regionLabel: String? = null, 
    commonGuide: CommonGuide? = null,
    regionOrdinance: app.trashai.data.RegionOrdinance? = null,
    regionExtras: RegionExtras = RegionExtras(),
    scrollValue: Int = 0,
) {
    // ---- Title row -----------------------------------------------------------
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Tokens.RecycleGreenSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Recycling,
                contentDescription = null,
                tint = Tokens.RecycleGreen,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(Tokens.Sp12))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                rule.itemName,
                fontSize = Tokens.TitleSize,
                fontWeight = FontWeight.Bold,
                color = Tokens.TextPrimary,
                lineHeight = 26.sp,
            )
            rule.primaryCategory?.let {
                Spacer(Modifier.height(Tokens.Sp4))
                Text(it, fontSize = Tokens.SubtitleSize, color = Tokens.TextSecondary)
            }
        }
        regionLabel?.let { RegionBadge(it) }
    }

    Spacer(Modifier.height(Tokens.Sp16))

    // ---- 배출 안내 (지역/공통 분기) --------------------------------------------------
    val isRegionalOverride = regionLabel != null && regionLabel != "위치 확인 중..."
    
    if (isRegionalOverride) {
        SectionHeader(icon = Icons.Outlined.LocationOn, text = "$regionLabel 맞춤 분리방법")
    } else {
        SectionHeader(icon = Icons.Outlined.EnergySavingsLeaf, text = "전국 공통 분류방법")
    }

    Spacer(Modifier.height(Tokens.Sp8))

    val dischargeText = rule.dischargeMethod ?: rule.appSummary ?: "분리배출 정보가 없습니다."

    val steps = dischargeText
        .split('.', '。', '\n')
        .map { it.trim() }
        .filter { it.length >= 2 }
        .distinctBy { it.replace(Regex("[\\s.,ㆍ·#?!~@@]"), "") }
        .take(5)

    // 스크롤 진행량(0 ~ 300px)에 따른 동적 크기 보간 인자 계산
    val fraction = (1f - (scrollValue.toFloat() / 300f)).coerceIn(0f, 1f)
    val cardGap = (6 + (6 * fraction)).dp
        
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(cardGap)
    ) {
        steps.forEachIndexed { i, step ->
            StepCard(
                number = i + 1, 
                body = step, 
                fraction = fraction
            )
        }
    }

    if (isRegionalOverride) {
        Spacer(Modifier.height(Tokens.Sp12))
        RegionOfficialInfoSection(
            regionLabel = regionLabel,
            regionOrdinance = regionOrdinance,
            regionExtras = regionExtras,
        )
    }

    // ---- E-순환거버넌스 무상 수거 안내 (DB 기반 CommonGuide 출력) ----------------------------------------
    if (commonGuide != null) {
        Spacer(Modifier.height(Tokens.Sp16))
        CommonGuideSection(guide = commonGuide)
    }

    rule.featureText?.let {
        Spacer(Modifier.height(Tokens.Sp12))
        InfoBlock(icon = Icons.Outlined.EnergySavingsLeaf, title = "특징", body = it, accent = Tokens.PrimarySoft, iconTint = Tokens.Primary)
    }
    rule.cautionText?.let {
        Spacer(Modifier.height(Tokens.Sp8))
        InfoBlock(icon = Icons.Outlined.WarningAmber, title = "주의", body = it, accent = Tokens.Warning, iconTint = Tokens.WarningText)
    }

    Spacer(Modifier.height(Tokens.Sp16))
    
    // ---- Source & Attribution (Sleek Navy Theme) ----------------------------
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Tokens.Radius8))
            .background(Tokens.SurfaceMuted.copy(alpha = 0.5f))
            .padding(horizontal = Tokens.Sp8, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "정보 제공 : ",
            fontSize = 11.sp,
            color = Tokens.TextSecondary.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        Image(
            painter = painterResource(id = app.trashai.R.drawable.mois_logo),
            contentDescription = "행정안전부 로고",
            modifier = Modifier.height(14.dp)
        )
        Spacer(Modifier.width(Tokens.Sp6))
        Image(
            painter = painterResource(id = app.trashai.R.drawable.gov_logo),
            contentDescription = "기후에너지환경부 로고",
            modifier = Modifier.height(14.dp)
        )
        Spacer(Modifier.width(Tokens.Sp6))
        Text(
            "생활폐기물 분리배출 누리집",
            fontSize = 11.sp,
            color = Tokens.TextSecondary,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(Modifier.height(Tokens.Sp24))

    // ---- AdMob Banner Placeholder (차후 광고 탑재 지면) -------------------------
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(Tokens.Radius8))
            .background(Tokens.SurfaceMuted)
            .border(1.dp, Tokens.Divider.copy(alpha = 0.5f), RoundedCornerShape(Tokens.Radius8)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = Tokens.TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(Tokens.Sp8))
            Text(
                "광고 지면",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Tokens.TextSecondary.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun RegionOfficialInfoSection(
    regionLabel: String?,
    regionOrdinance: app.trashai.data.RegionOrdinance?,
    regionExtras: RegionExtras,
) {
    val context = LocalContext.current
    val moisLines = regionExtras.moisSchedules.filter { it.hasSchedule }
    val contact = regionExtras.contact
    val ordSummary = regionOrdinance?.appSummary

    val hasMois = moisLines.isNotEmpty()
    val hasContact = contact != null
    val hasOrdinanceHint = ordSummary != null &&
        (ordSummary.contains("일몰") || ordSummary.contains("지정된 장소"))

    if (!hasMois && !hasContact && !hasOrdinanceHint) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius12))
            .background(Tokens.SurfaceMuted)
            .border(1.dp, Tokens.Divider, RoundedCornerShape(Tokens.Radius12))
            .padding(Tokens.Sp16),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Tokens.Sp12)) {
            SectionHeader(
                icon = Icons.Outlined.Schedule,
                text = "${regionLabel ?: "현재 지역"} 공식 배출 안내",
            )

            if (hasMois) {
                Text(
                    "출처: 행정안전부 생활쓰레기 배출정보",
                    fontSize = 11.sp,
                    color = Tokens.TextSecondary.copy(alpha = 0.85f),
                )
                moisLines.forEach { line -> MoisScheduleLine(line) }
            }

            if (!hasMois && hasOrdinanceHint) {
                if (ordSummary!!.contains("일몰")) {
                    RegionHintLine(Icons.Outlined.Schedule, "배출 시간: 일몰 후부터 일출 전까지 배출")
                }
                if (ordSummary.contains("지정된 장소")) {
                    RegionHintLine(Icons.Outlined.Place, "배출 장소: 지정된 장소 또는 거점 수거")
                }
            }

            if (hasContact) {
                val c = contact!!
                Spacer(Modifier.height(Tokens.Sp4))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Tokens.Radius12))
                        .background(Tokens.PrimarySoft)
                        .clickable(enabled = !c.telUri.isNullOrBlank()) {
                            c.telUri?.let { uri ->
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(uri)))
                            }
                        }
                        .padding(horizontal = Tokens.Sp16, vertical = Tokens.Sp12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Phone, null, tint = Tokens.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Tokens.Sp12))
                    Column(Modifier.weight(1f)) {
                        Text(c.deptName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Tokens.TextPrimary)
                        Text(c.phone, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Tokens.Primary)
                        c.sourceName?.let {
                            Text(it, fontSize = 11.sp, color = Tokens.TextSecondary)
                        }
                    }
                    if (!c.telUri.isNullOrBlank()) {
                        Text("전화", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Tokens.Primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoisScheduleLine(rule: MoisDisposalRule) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            rule.category,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Tokens.Primary,
        )
        rule.disposalTime?.let {
            Text(
                "⏰ $it",
                fontSize = 13.sp,
                color = Tokens.TextPrimary,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        rule.disposalMethod?.takeIf { it.isNotBlank() }?.let {
            Text(
                it.take(120) + if (it.length > 120) "…" else "",
                fontSize = 12.sp,
                color = Tokens.TextSecondary,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun RegionHintLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Tokens.TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(Tokens.Sp8))
        Text(text, color = Tokens.TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

// ---------------------------------------------------------------------------
// Subcomponents
// ---------------------------------------------------------------------------

@Composable
private fun RegionBadge(label: String) {
    Row(
        modifier = Modifier
            .background(Tokens.PrimaryTint, CircleShape)
            .border(0.5.dp, Tokens.Primary.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.EnergySavingsLeaf,
            contentDescription = null,
            tint = Tokens.Primary,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = Tokens.TagSize,
            fontWeight = FontWeight.SemiBold,
            color = Tokens.Primary,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Tokens.Primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(Tokens.Sp6))
        Text(
            text = text,
            fontSize = Tokens.SectionSize,
            fontWeight = FontWeight.Bold,
            color = Tokens.Primary,
        )
    }
}

private fun getStepIcon(text: String): ImageVector {
    val clean = text.replace(" ", "")
    return when {
        clean.contains("헹구") || clean.contains("씻으") || clean.contains("세척") || clean.contains("물로") || clean.contains("비우") -> {
            Icons.Outlined.WaterDrop
        }
        clean.contains("떼") || clean.contains("제거") || clean.contains("분리") || clean.contains("뜯") || clean.contains("컷") || clean.contains("벗기") || clean.contains("비닐") -> {
            Icons.Outlined.ContentCut
        }
        clean.contains("접") || clean.contains("압착") || clean.contains("찌그러") || clean.contains("부수") || clean.contains("붑") || clean.contains("납작") || clean.contains("밟") -> {
            Icons.Outlined.Compress
        }
        clean.contains("버리") || clean.contains("배출") || clean.contains("넣으") || clean.contains("담으") || clean.contains("제출") || clean.contains("투입") || clean.contains("배송") -> {
            Icons.Outlined.DeleteOutline
        }
        clean.contains("묶") || clean.contains("묶어") || clean.contains("끈") || clean.contains("포장") -> {
            Icons.Outlined.ShoppingBag
        }
        else -> Icons.Outlined.CheckCircleOutline
    }
}

@Composable
private fun StepCard(
    number: Int,
    body: String,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val bulletRegex = Regex("^[•\\-*·\\d.]+\\s*")
    val cleanBody = body.trim().replaceFirst(bulletRegex, "")

    val fontSize = (13 + (7 * fraction)).sp
    val iconSize = (28 + (28 * fraction)).dp
    val verticalPadding = (8 + (10 * fraction)).dp
    val horizontalPadding = (10 + (8 * fraction)).dp
    val numFontSize = (14 + (8 * fraction)).sp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = (2 + (4 * fraction)).dp,
                shape = RoundedCornerShape(Tokens.Radius12),
                spotColor = Color(0x1A000000)
            )
            .background(
                if (number == 1 && fraction > 0.3f) Tokens.PrimarySoft.copy(alpha = 0.5f)
                else Tokens.Surface,
                RoundedCornerShape(Tokens.Radius12)
            )
            .border(
                width = (1 + (0.5f * fraction)).dp,
                color = if (number == 1 && fraction > 0.3f) Tokens.Primary else Tokens.Divider,
                shape = RoundedCornerShape(Tokens.Radius12)
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 둥근 원 안에 해당 단계 아이콘을 반투명하게 배치하고, 그 위에 단계 숫자를 겹쳐서 공간활용 극대화
        val stepIcon = getStepIcon(cleanBody)
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(
                    if (number == 1) Tokens.Primary
                    else Tokens.PrimarySoft
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = stepIcon,
                contentDescription = null,
                tint = if (number == 1) Color.White.copy(alpha = 0.25f) else Tokens.Primary.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxSize(0.6f)
            )
            Text(
                text = "$number",
                color = if (number == 1) Color.White else Tokens.Primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = numFontSize,
            )
        }
        
        Spacer(Modifier.width(Tokens.Sp12))

        // 2. 가로 폭이 시원하게 극대화된 본문 텍스트
        TextWithDialablePhones(
            text = cleanBody,
            modifier = Modifier.weight(1f),
            style = androidx.compose.ui.text.TextStyle(
                color = Tokens.TextPrimary,
                fontSize = fontSize,
                lineHeight = fontSize * 1.35f,
                fontWeight = if (fraction > 0.5f) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Start,
            ),
        )
    }
}

@Composable
private fun InfoBlock(
    icon: ImageVector,
    title: String,
    body: String,
    accent: Color,
    iconTint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent, RoundedCornerShape(Tokens.Radius12))
            .border(1.dp, iconTint.copy(alpha = 0.15f), RoundedCornerShape(Tokens.Radius12))
            .padding(Tokens.Sp16),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(Tokens.Sp8))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = Tokens.CaptionSize,
                fontWeight = FontWeight.SemiBold,
                color = iconTint,
            )
            val bulletRegex = Regex("^[•\\-*·\\d.]+\\s*")
            val lines = body.split('\n')
                .map { it.trim().replaceFirst(bulletRegex, "") }
                .filter { it.isNotEmpty() }

            if (lines.size <= 1) {
                TextWithDialablePhones(
                    text = lines.firstOrNull() ?: "",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = Tokens.CaptionSize,
                        color = Tokens.TextPrimary,
                        lineHeight = 16.sp,
                    ),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lines.forEach { line ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "•",
                                fontSize = Tokens.CaptionSize,
                                color = iconTint.copy(alpha = 0.5f),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            TextWithDialablePhones(
                                text = line,
                                modifier = Modifier.weight(1f),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = Tokens.CaptionSize,
                                    color = Tokens.TextPrimary,
                                    lineHeight = 16.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommonGuideSection(guide: CommonGuide) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius16))
            .background(Tokens.PrimarySoft)
            .border(1.dp, Tokens.Divider, RoundedCornerShape(Tokens.Radius16))
            .padding(Tokens.Sp16)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.LocalShipping,
                contentDescription = null,
                tint = Tokens.Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(Tokens.Sp8))
            Text(
                guide.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Tokens.TextPrimary
            )
        }
        Spacer(Modifier.height(Tokens.Sp8))
        TextWithDialablePhones(
            text = guide.description,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                color = Tokens.TextSecondary,
                lineHeight = 20.sp,
            ),
        )
        Spacer(Modifier.height(Tokens.Sp16))
        
        if (guide.tableHeaders != null && guide.tableRows != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Tokens.Radius12))
                    .background(Tokens.Surface)
                    .border(1.dp, Tokens.Divider, RoundedCornerShape(Tokens.Radius12))
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Tokens.SurfaceMuted)
                        .padding(Tokens.Sp12),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    guide.tableHeaders.forEachIndexed { index, header ->
                        val weight = if (index == 1) 0.4f else 0.3f
                        Text(header, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Tokens.TextSecondary, modifier = Modifier.weight(weight))
                    }
                }
                HorizontalDivider(color = Tokens.Divider)
                
                // Rows
                guide.tableRows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Tokens.Sp12),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEachIndexed { colIndex, cell ->
                            val weight = if (colIndex == 1) 0.4f else 0.3f
                            val color = when (colIndex) {
                                0 -> Tokens.Primary
                                2 -> Tokens.Accent
                                else -> Tokens.TextSecondary
                            }
                            val fontWeight = if (colIndex != 1) FontWeight.Bold else FontWeight.Normal
                            Text(
                                text = cell, 
                                fontWeight = fontWeight, 
                                fontSize = 12.sp, 
                                color = color, 
                                modifier = Modifier.weight(weight).padding(horizontal = if (colIndex == 1) 4.dp else 0.dp)
                            )
                        }
                    }
                    if (rowIndex < guide.tableRows.size - 1) {
                        HorizontalDivider(color = Tokens.Divider)
                    }
                }
            }
            Spacer(Modifier.height(Tokens.Sp16))
        }

        val context = LocalContext.current
        val dialUri = guide.ctaAction?.takeIf { it.isNotBlank() }
            ?: if (guide.guideId == "ecycle") ECYCLE_DIAL_URI else null
        val dialLabel = guide.ctaLabel?.takeIf { it.isNotBlank() }
            ?: if (dialUri != null) "$ECYCLE_DISPLAY_NUMBER 전화 접수 (E-순환거버넌스)" else null
        if (dialUri != null && dialLabel != null) {
            Button(
                onClick = { dialPhoneNumber(context, dialUri) },
                colors = ButtonDefaults.buttonColors(containerColor = Tokens.Primary),
                shape = RoundedCornerShape(Tokens.Radius12),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Icon(Icons.Outlined.Phone, contentDescription = "전화 걸기", tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Tokens.Sp8))
                Text(dialLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun ItemRuleBodyPreview() {
    val sampleRule = ItemRule(
        itemId = "test1",
        itemName = "세탁기",
        primaryCategory = "대형 가전",
        dischargeMethod = "1. 대형 폐가전은 무상 방문 수거 서비스를 이용하세요.\n2. 집 밖으로 배출할 필요 없이 기사님이 직접 방문합니다.",
        featureText = "정부 지원 E-순환거버넌스 대상 품목",
        cautionText = "소형 가전은 5개 이상 모아서 배출해야 방문 수거가 가능합니다.",
        appSummary = "무상 방문 수거 이용 가능",
        sourceName = "고양시청",
        sourceUrl = ""
    )
    val sampleGuide = CommonGuide(
        guideId = "ecycle",
        title = "폐가전제품 배출 방법 (무상 방문 수거)",
        subtitle = "정부 운영 E-순환거버넌스",
        description = "TV, 냉장고, 세탁기, 에어컨 등 폐가전제품은 정부에서 운영하는 'E-순환거버넌스'를 통해 전액 무료로 내놓으실 수 있습니다. 무겁게 집 밖으로 나를 필요 없이 수거 기사님이 집 안까지 방문하여 수거해 갑니다.",
        tableHeaders = listOf("분류", "수거 기준 품목", "배출 팁"),
        tableRows = listOf(
            listOf("단일 수거 가능", "냉장고, 세탁기, 에어컨, TV, 러닝머신, 전자레인지 등 대형 가전", "1개만 버려도 무상 방문 수거 가능"),
            listOf("다량 수거 가능", "PC 본체, 모니터, 노트북, 가습기, 헤어드라이어, 청소기 등 소형 가전", "5개 이상 동시에 배출할 때 방문 수거 가능")
        ),
        ctaLabel = "1599-0903 전화 접수 (E-순환거버넌스)",
        ctaAction = "tel:15990903"
    )

    Column(modifier = Modifier.padding(Tokens.Sp16)) {
        ItemRuleBody(rule = sampleRule, regionLabel = "고양시 일산동구", commonGuide = sampleGuide)
    }
}
