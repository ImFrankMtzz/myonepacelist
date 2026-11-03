package com.myonepacelist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.myonepacelist.R
import com.myonepacelist.data.progress.ThemeMode

/** Semantic color tokens. Screens use these, never raw hex values. */
@Immutable
data class Palette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val nav: Color,
    val border: Color,
    val track: Color,
    val text: Color,
    val textSoft: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val accentDim: Color,
    val error: Color,
)

val NightPalette = Palette(
    isDark = true,
    bg = Color(0xFF0E1116), surface = Color(0xFF161B22), surface2 = Color(0xFF1E2530), nav = Color(0xFF12161D),
    border = Color(0xFF262D3A), track = Color(0xFF262D3A),
    text = Color(0xFFE8ECF1), textSoft = Color(0xFFC4CBD5), textMuted = Color(0xFF9AA4B2),
    accent = Color(0xFFF4B740), onAccent = Color(0xFF1A1205), accentContainer = Color(0xFF3A2F14), accentDim = Color(0xFF8A6A2A),
    error = Color(0xFFF2555A),
)

val ParchmentPalette = Palette(
    isDark = false,
    bg = Color(0xFFFAF8F3), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFF1EEE6), nav = Color(0xFFF4F1EA),
    border = Color(0xFFE4DFD3), track = Color(0xFFE4DFD3),
    text = Color(0xFF14181F), textSoft = Color(0xFF3D4652), textMuted = Color(0xFF5A6472),
    accent = Color(0xFF9A6700), onAccent = Color(0xFFFFFFFF), accentContainer = Color(0xFFF6E7C4), accentDim = Color(0xFFD9B66A),
    error = Color(0xFFB42318),
)

val LocalPalette = staticCompositionLocalOf { NightPalette }

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val Inter = FontFamily(listOf(400, 500, 600, 700).map { variable(R.font.inter, it) })
val SpaceGrotesk = FontFamily(listOf(500, 600, 700).map { variable(R.font.space_grotesk, it) })

private val AppTypography = Typography().run {
    fun TextStyle.display(size: Int, weight: Int = 700, tracking: Double = -0.3) =
        copy(fontFamily = SpaceGrotesk, fontSize = size.sp, fontWeight = FontWeight(weight), letterSpacing = tracking.sp, lineHeight = (size * 1.2).sp)
    fun TextStyle.body(size: Int, weight: Int = 400, line: Double = 1.5) =
        copy(fontFamily = Inter, fontSize = size.sp, fontWeight = FontWeight(weight), lineHeight = (size * line).sp, letterSpacing = 0.sp)
    copy(
        displayLarge = displayLarge.display(44),
        displayMedium = displayMedium.display(34),
        displaySmall = displaySmall.display(30, tracking = -0.4),
        headlineLarge = headlineLarge.display(28),
        headlineMedium = headlineMedium.display(26),
        headlineSmall = headlineSmall.display(24, 600),
        titleLarge = titleLarge.display(20, 700, -0.2),
        titleMedium = titleMedium.display(18, 600, 0.0),
        titleSmall = titleSmall.display(16, 600, 0.0),
        bodyLarge = bodyLarge.body(16),
        bodyMedium = bodyMedium.body(14),
        bodySmall = bodySmall.body(13, line = 1.4),
        labelLarge = labelLarge.body(16, 600, 1.25),
        labelMedium = labelMedium.body(14, 500, 1.3),
        labelSmall = labelSmall.body(12, 500, 1.3),
    )
}

@Composable
fun MyOnePaceTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val p = if (dark) NightPalette else ParchmentPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.accentContainer, onPrimaryContainer = p.accent,
            background = p.bg, onBackground = p.text, surface = p.surface, onSurface = p.text,
            surfaceVariant = p.surface2, onSurfaceVariant = p.textMuted, surfaceContainer = p.surface,
            surfaceContainerHigh = p.surface2, surfaceContainerLow = p.bg, outline = p.border, outlineVariant = p.border,
            secondaryContainer = p.accentContainer, onSecondaryContainer = p.accent, error = p.error,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.accentContainer, onPrimaryContainer = p.accent,
            background = p.bg, onBackground = p.text, surface = p.surface, onSurface = p.text,
            surfaceVariant = p.surface2, onSurfaceVariant = p.textMuted, surfaceContainer = p.surface,
            surfaceContainerHigh = p.surface2, surfaceContainerLow = p.bg, outline = p.border, outlineVariant = p.border,
            secondaryContainer = p.accentContainer, onSecondaryContainer = p.accent, error = p.error,
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

/** Shorthand: `Tokens.accent` inside composables. */
val Tokens: Palette
    @Composable get() = LocalPalette.current
