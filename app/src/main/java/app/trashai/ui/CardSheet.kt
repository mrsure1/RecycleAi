package app.trashai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun ItemRuleBody(
    rule: ItemRule, 
    regionLabel: String? = null, 
    commonGuide: CommonGuide? = null,
    regionOrdinance: app.trashai.data.RegionOrdinance? = null
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
        .take(5)
        
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Sp6)
    ) {
        steps.forEachIndexed { i, step ->
            StepColumn(
                number = i + 1, 
                body = step, 
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(Tokens.Radius12),
                        spotColor = Color(0x1A000000),
                        ambientColor = Color(0x0A000000)
                    )
                    .background(Tokens.Surface, RoundedCornerShape(Tokens.Radius12))
                    .border(1.dp, Tokens.Divider, RoundedCornerShape(Tokens.Radius12))
                    .padding(horizontal = Tokens.Sp16, vertical = Tokens.Sp12)
            )
        }
    }

    // ---- Region Time and Location (Sleek Navy Banner) ----------------------------------------------
    if (isRegionalOverride) {
        Spacer(Modifier.height(Tokens.Sp12))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius12))
                .background(Tokens.SurfaceMuted)
                .border(1.dp, Tokens.Divider, RoundedCornerShape(Tokens.Radius12))
                .padding(Tokens.Sp16)
        ) {
            val ordSummary = regionOrdinance?.appSummary
            val timeText = if (ordSummary != null && ordSummary.contains("일몰")) {
                "⏰ 배출 시간: 일몰 후부터 일출 전까지 배출"
            } else {
                "⏰ 배출 시간: 해당 지자체 안내 시간 참조"
            }
            val placeText = if (ordSummary != null && ordSummary.contains("지정된 장소")) {
                "🗑️ 배출 장소: 내 집 앞 또는 지정된 거점 수거 장소"
            } else {
                "🗑️ 배출 장소: 정해진 배출 수거함 또는 문 앞"
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = timeText,
                    color = Tokens.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = placeText,
                    color = Tokens.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
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

@Composable
private fun StepColumn(number: Int, body: String, modifier: Modifier = Modifier) {
    val bulletRegex = Regex("^[•\\-*·\\d.]+\\s*")
    val lines = body.split('\n')
        .map { it.trim().replaceFirst(bulletRegex, "") }
        .filter { it.isNotEmpty() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Tokens.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                color = Tokens.Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(Tokens.Sp8))
        
        Column(modifier = Modifier.weight(1f)) {
            if (lines.size <= 1) {
                Text(
                    text = lines.firstOrNull() ?: "",
                    color = Tokens.TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Tokens.Sp4)
                ) {
                    lines.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                color = Tokens.Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(12.dp)
                            )
                            Text(
                                text = line,
                                color = Tokens.TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Start,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
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
                Text(
                    text = lines.firstOrNull() ?: "",
                    fontSize = Tokens.CaptionSize,
                    color = Tokens.TextPrimary,
                    lineHeight = 16.sp
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
                            Text(
                                text = line,
                                fontSize = Tokens.CaptionSize,
                                color = Tokens.TextPrimary,
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(1f)
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
        Text(
            guide.description,
            fontSize = 13.sp,
            color = Tokens.TextSecondary,
            lineHeight = 20.sp
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

        if (guide.ctaLabel != null && guide.ctaAction != null) {
            val context = LocalContext.current
            Button(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                        data = android.net.Uri.parse(guide.ctaAction)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Tokens.Primary),
                shape = RoundedCornerShape(Tokens.Radius12),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Outlined.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Tokens.Sp8))
                Text(guide.ctaLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
