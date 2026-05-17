package app.trashai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trashai.data.ItemRule

@Composable
fun ItemRuleBody(rule: ItemRule, regionLabel: String? = null) {
    // ---- Title row -----------------------------------------------------------
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Tokens.PrimaryGreenTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Recycling,
                contentDescription = null,
                tint = Tokens.PrimaryGreen,
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
                lineHeight = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp),
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
    if (steps.size <= 2) {
        // Case 1: 1~2개일 때 가로 배치 (중간에 화살표 표시)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Sp8)
        ) {
            steps.forEachIndexed { i, step ->
                StepColumn(
                    number = i + 1, 
                    body = step, 
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(Tokens.Radius12),
                            spotColor = Color(0x1A000000),
                            ambientColor = Color(0x0A000000)
                        )
                        .background(Tokens.Surface, RoundedCornerShape(Tokens.Radius12))
                        .border(1.dp, Tokens.Divider, RoundedCornerShape(Tokens.Radius12))
                        .padding(Tokens.Sp16)
                )
                if (i < steps.size - 1) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "다음 단계",
                        tint = Tokens.PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    } else {
        // Case 2: 3개 이상일 때 세로 배치 (각각 중간에 화살표 표시)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Sp8)
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
                        .padding(Tokens.Sp16)
                )
                if (i < steps.size - 1) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "다음 단계",
                        tint = Tokens.PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    // ---- Region Time and Location ----------------------------------------------
    if (isRegionalOverride) {
        Spacer(Modifier.height(Tokens.Sp12))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius12))
                .background(Color.White.copy(alpha = 0.6f)) // Subtle Glassmorphism
                .border(1.dp, Color.White, RoundedCornerShape(Tokens.Radius12))
                .padding(Tokens.Sp16)
        ) {
            Text(
                text = "⏰ 배출 시간: 해당 지자체(또는 아파트) 안내 시간 참조\n🗑️ 배출 장소: 정해진 배출 수거함 또는 문 앞",
                color = Tokens.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    rule.featureText?.let {
        Spacer(Modifier.height(Tokens.Sp12))
        InfoBlock(icon = Icons.Outlined.EnergySavingsLeaf, title = "특징", body = it, accent = Tokens.PrimaryGreenSoft, iconTint = Tokens.PrimaryGreen)
    }
    rule.cautionText?.let {
        Spacer(Modifier.height(Tokens.Sp8))
        InfoBlock(icon = Icons.Outlined.WarningAmber, title = "주의", body = it, accent = Tokens.Warning, iconTint = Tokens.WarningText)
    }

    Spacer(Modifier.height(Tokens.Sp16))
    
    // ---- Source & Attribution (Sleek but subtle) ----------------------------
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
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = app.trashai.R.drawable.mois_logo),
            contentDescription = "행정안전부 로고",
            modifier = Modifier.height(14.dp)
        )
        Spacer(Modifier.width(Tokens.Sp6))
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = app.trashai.R.drawable.gov_logo),
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
            .background(Tokens.PrimaryGreenTint, CircleShape)
            .border(0.5.dp, Tokens.PrimaryGreen.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.EnergySavingsLeaf,
            contentDescription = null,
            tint = Tokens.PrimaryGreen,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = Tokens.TagSize,
            fontWeight = FontWeight.SemiBold,
            color = Tokens.PrimaryGreen,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Tokens.PrimaryGreen,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(Tokens.Sp6))
        Text(
            text = text,
            fontSize = Tokens.SectionSize,
            fontWeight = FontWeight.Bold,
            color = Tokens.PrimaryGreen,
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
                .background(Tokens.PrimaryGreenSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                color = Tokens.PrimaryGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(Tokens.Sp8))
        
        Column(modifier = Modifier.weight(1f)) {
            if (lines.size <= 1) {
                // Case 1: Single item - No dot, just text
                Text(
                    text = lines.firstOrNull() ?: "",
                    color = Tokens.TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Bold
                )
            } else {
                // Case 2: Multiple items - Perfect hanging indent with fixed bullet width
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
                            color = Tokens.PrimaryGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(12.dp) // Fixed width for bullet to force indent
                        )
                        Text(
                            text = line,
                            color = Tokens.TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f) // Text stays in its own column
                        )
                    }
                }
            }
        } // closes if-else
    } // closes Column(modifier = Modifier.weight(1f))
} // closes StepColumn Row
} // closes StepColumn function

@Composable
private fun InfoBlock(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            // Clean up DB text: remove existing bullets (•, -, *, ·) or numbers (1.) from start
            val bulletRegex = Regex("^[•\\-*·\\d.]+\\s*")
            val lines = body.split('\n')
                .map { it.trim().replaceFirst(bulletRegex, "") }
                .filter { it.isNotEmpty() }

            if (lines.size <= 1) {
                // Case 1: Single item - No bullet, just text
                Text(
                    text = lines.firstOrNull() ?: "",
                    fontSize = Tokens.CaptionSize,
                    color = Tokens.TextPrimary,
                    lineHeight = 16.sp
                )
            } else {
                // Case 2: Multiple items - Bulleted list with hanging indent
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun ItemRuleBodyPreview() {
    app.trashai.data.ItemRule(
        itemId = "test1",
        itemName = "투명 페트병",
        primaryCategory = "플라스틱",
        dischargeMethod = "1. 내용물을 비우고 물로 헹굽니다.\n2. 라벨을 제거합니다.\n3. 찌그러뜨려 뚜껑을 닫습니다.",
        featureText = "무색 투명한 생수, 음료수병만 해당됩니다.",
        cautionText = "유색 플라스틱이나 커피컵은 일반 플라스틱으로 배출하세요.",
        appSummary = "깨끗이 씻어서 배출",
        sourceName = "고양시청",
        sourceUrl = ""
    ).let { rule ->
        Column(modifier = Modifier.padding(Tokens.Sp16)) {
            ItemRuleBody(rule = rule, regionLabel = "고양시 일산동구")
        }
    }
}
