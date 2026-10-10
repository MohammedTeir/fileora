package dev.deviceagent.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.Tone

/*
 * Visual language: calm, neutral and content-first, like a modern chat app.
 * - One near-black accent instead of a coloured one; colour is reserved for status (green/amber/red).
 * - Flat surfaces separated by hairline borders, not tinted fills or shadows.
 * - Large rounded shapes, normal letter spacing, generous line height.
 * Wallpaper-based dynamic colour is intentionally NOT used: it tinted every screen lavender and made the
 * app look different on every phone.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A1A19),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFEEEA),
    onPrimaryContainer = Color(0xFF1A1A19),
    secondary = Color(0xFF5F5E5A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9E8E3),
    onSecondaryContainer = Color(0xFF1A1A19),
    tertiary = Color(0xFF5F5E5A),
    background = Color(0xFFFAFAF8),
    onBackground = Color(0xFF1A1A19),
    surface = Color(0xFFFAFAF8),
    onSurface = Color(0xFF1A1A19),
    surfaceVariant = Color(0xFFEFEEEA),
    onSurfaceVariant = Color(0xFF6B6A66),
    surfaceTint = Color(0xFF1A1A19),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF5F4F0),
    surfaceContainerHigh = Color(0xFFF1F0EC),
    surfaceContainerHighest = Color(0xFFEAE9E4),
    outline = Color(0xFF8F8E89),
    outlineVariant = Color(0xFFE3E2DD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFECECEA),
    onPrimary = Color(0xFF1A1A19),
    primaryContainer = Color(0xFF353534),
    onPrimaryContainer = Color(0xFFECECEA),
    secondary = Color(0xFFB5B4AF),
    onSecondary = Color(0xFF1A1A19),
    secondaryContainer = Color(0xFF3A3A39),
    onSecondaryContainer = Color(0xFFECECEA),
    tertiary = Color(0xFFB5B4AF),
    background = Color(0xFF1F1F1E),
    onBackground = Color(0xFFECECEA),
    surface = Color(0xFF1F1F1E),
    onSurface = Color(0xFFECECEA),
    surfaceVariant = Color(0xFF2E2E2D),
    onSurfaceVariant = Color(0xFFA3A29E),
    surfaceTint = Color(0xFFECECEA),
    surfaceContainerLowest = Color(0xFF1A1A19),
    surfaceContainerLow = Color(0xFF262625),
    surfaceContainer = Color(0xFF2B2B2A),
    surfaceContainerHigh = Color(0xFF30302F),
    surfaceContainerHighest = Color(0xFF3A3A39),
    outline = Color(0xFF7E7D79),
    outlineVariant = Color(0xFF3D3D3B),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = 0.sp)

/** Material's default tracking (0.5sp on body text) looked wide and dated; this sets it to zero. */
private val AppTypography = Typography(
    headlineMedium = style(28, 36, FontWeight.Medium),
    headlineSmall = style(24, 32, FontWeight.Medium),
    titleLarge = style(21, 28, FontWeight.Medium),
    titleMedium = style(16, 24, FontWeight.Medium),
    titleSmall = style(15, 22, FontWeight.Medium),
    bodyLarge = style(16, 24),
    bodyMedium = style(15, 22),
    bodySmall = style(13, 19),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(11, 16, FontWeight.Medium),
)

@Composable
fun AgentTheme(content: @Composable () -> Unit) {
    val colors: ColorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
}

/** Container / content colours for a status. Green = good, amber = needs attention, red = problem, neutral = information. */
@Composable
internal fun toneColors(tone: Tone): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return when (tone) {
        Tone.SUCCESS -> if (dark) Color(0xFF1C2E21) to Color(0xFFB7E4C2) else Color(0xFFEAF6EC) to Color(0xFF14532D)
        Tone.WARNING -> if (dark) Color(0xFF35290E) to Color(0xFFF3D898) else Color(0xFFFDF3DC) to Color(0xFF6B4A00)
        Tone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Tone.INFO -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }
}
