package app.trashai.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Tokens {
    // Brand (Dark Navy Theme)
    val Primary = Color(0xFF0F172A) // Slate 900 (Dark Navy)
    val PrimarySoft = Color(0xFFF1F5F9) // Slate 100 (Soft background)
    val PrimaryTint = Color(0x1A0F172A) // 10% alpha of Primary
    val Accent = Color(0xFF3B82F6) // Blue 500 (Point Accent)
    val AccentSoft = Color(0xFFEFF6FF) // Blue 50 (Soft Accent background)

    // Recycle Green (For recycle icons across all pages)
    val RecycleGreen = Color(0xFF10B981) // Emerald 500 (Vibrant, professional green)
    val RecycleGreenSoft = Color(0xFFECFDF5) // Emerald 50 (Soft green background)

    // Surfaces
    val Background = Color(0xFFF8FAFC) // Slate 50
    val Surface = Color.White
    val SurfaceMuted = Color(0xFFF1F5F9) // Slate 100
    val Divider = Color(0xFFE2E8F0) // Slate 200

    // Text
    val TextPrimary = Color(0xFF0F172A) // Slate 900
    val TextSecondary = Color(0xFF64748B) // Slate 500
    val TextOnDark = Color.White

    // Status
    val Warning = Color(0xFFFEF3C7) // Amber 100
    val WarningText = Color(0xFFB45309) // Amber 700
    val Danger = Color(0xFFFEE2E2) // Red 100
    val DangerText = Color(0xFFB91C1C) // Red 700

    // Camera HUD (Preserving original Neon Green for bounding boxes & HUD)
    val Scrim = Color(0x66000000)
    val ScrimStrong = Color(0xCC000000)
    val NeonGreen = Color(0xFF7CFF6B)

    // AI & Premium Animations (Refined to match Navy/Sleek Theme)
    val AiGradientStart = Color(0xFF0F172A)
    val AiGradientEnd = Color(0xFF3B82F6)
    val AiCardBg = Color(0xFF0F172A)
    val AiText = Color(0xFFF8FAFC)

    // Spacing
    val Sp4 = 4.dp
    val Sp6 = 6.dp
    val Sp8 = 8.dp
    val Sp12 = 12.dp
    val Sp16 = 16.dp
    val Sp20 = 20.dp
    val Sp24 = 24.dp
    val Sp32 = 32.dp

    // Radii
    val Radius8 = 8.dp
    val Radius12 = 12.dp
    val Radius16 = 16.dp
    val Radius24 = 24.dp
    val Radius28 = 28.dp // For bottom sheet

    // Typography
    val TitleSize = 20.sp
    val SubtitleSize = 14.sp
    val SectionSize = 16.sp
    val BodySize = 15.sp
    val CaptionSize = 13.sp
    val TagSize = 12.sp
}
