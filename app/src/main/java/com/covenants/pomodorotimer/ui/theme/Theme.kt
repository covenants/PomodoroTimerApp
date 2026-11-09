package com.covenants.pomodorotimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.covenants.pomodorotimer.core.model.AppSettings
import com.covenants.pomodorotimer.core.model.ThemeChoice

data class OrbitColors(
    val background: Color,
    val backgroundEnd: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val danger: Color,
    val star: Color,
    val isDark: Boolean,
)

private val MidnightColors = OrbitColors(
    background = Color(0xFF0A1530),
    backgroundEnd = Color(0xFF0C1B3B),
    surface = Color(0xFF152546),
    surfaceElevated = Color(0xFF1A2C52),
    border = Color(0xFF273A61),
    primary = Color(0xFFA6BDFF),
    onPrimary = Color(0xFF101C3A),
    accent = Color(0xFF79E8D7),
    text = Color(0xFFF6F8FF),
    muted = Color(0xFF95A6C9),
    faint = Color(0xFF64759B),
    danger = Color(0xFFFFB5CC),
    star = Color(0xFFB9C9FF),
    isDark = true,
)

private val WarmPaperColors = OrbitColors(
    background = Color(0xFFF7F3EC),
    backgroundEnd = Color(0xFFF1EBE1),
    surface = Color(0xFFFCFAF8),
    surfaceElevated = Color(0xFFFFFFFF),
    border = Color(0xFFE4DDD2),
    primary = Color(0xFF6D7EC8),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF288F83),
    text = Color(0xFF29334B),
    muted = Color(0xFF6B7488),
    faint = Color(0xFF9AA0AE),
    danger = Color(0xFFB84A62),
    star = Color(0xFF6D7EC8),
    isDark = false,
)

private val AuroraColors = OrbitColors(
    background = Color(0xFF061A21),
    backgroundEnd = Color(0xFF0A2530),
    surface = Color(0xFF0F2A33),
    surfaceElevated = Color(0xFF14343F),
    border = Color(0xFF1E4450),
    primary = Color(0xFF9FD8FF),
    onPrimary = Color(0xFF06212A),
    accent = Color(0xFF8CF5C8),
    text = Color(0xFFF1FFFA),
    muted = Color(0xFF9BB9B8),
    faint = Color(0xFF638C8B),
    danger = Color(0xFFFFB5C8),
    star = Color(0xFFB7F5E4),
    isDark = true,
)

private val HighContrastMidnight = MidnightColors.copy(
    border = Color(0xFF5573A8),
    text = Color(0xFFFFFFFF),
    muted = Color(0xFFC9D5EE),
    faint = Color(0xFF8EA0C4),
    primary = Color(0xFFC9D6FF),
    accent = Color(0xFF9BFFEF),
)

private val HighContrastPaper = WarmPaperColors.copy(
    border = Color(0xFF8A8274),
    text = Color(0xFF101827),
    muted = Color(0xFF3E4658),
    primary = Color(0xFF4657A8),
    accent = Color(0xFF0F6B60),
)

private val HighContrastAurora = AuroraColors.copy(
    border = Color(0xFF4E8492),
    text = Color(0xFFFFFFFF),
    muted = Color(0xFFC7E2DE),
    primary = Color(0xFFC4E9FF),
    accent = Color(0xFFB6FFE2),
)

val LocalOrbitColors = staticCompositionLocalOf { MidnightColors }

val OrbitShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

private val OrbitTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.6.sp,
    ),
)

@Composable
fun PomodoroTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val colors = when (settings.theme) {
        ThemeChoice.MIDNIGHT -> if (settings.highContrast) HighContrastMidnight else MidnightColors
        ThemeChoice.WARM_PAPER -> if (settings.highContrast) HighContrastPaper else WarmPaperColors
        ThemeChoice.AURORA -> if (settings.highContrast) HighContrastAurora else AuroraColors
    }
    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            secondary = colors.accent,
            onSecondary = colors.onPrimary,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.muted,
            outline = colors.border,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            secondary = colors.accent,
            onSecondary = colors.onPrimary,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.muted,
            outline = colors.border,
            error = colors.danger,
        )
    }
    CompositionLocalProvider(LocalOrbitColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = OrbitTypography,
            shapes = OrbitShapes,
            content = content,
        )
    }
}
