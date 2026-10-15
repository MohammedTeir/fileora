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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.Tone

/*
 * Visual language: calm, neutral and content-first, like a modern chat app.
 * - One blue accent (light: soft blue-white, dark: deep navy); green/amber/red are reserved for status.
 * - Flat surfaces separated by hairline borders, not tinted fills or shadows.
 * - Large rounded shapes, normal letter spacing, generous line height.
 * Wallpaper-based dynamic colour is intentionally NOT used: it tinted every screen lavender and made the
 * app look different on every phone.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF0867F9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = Color(0xFF0A2A66),
    secondary = Color(0xFF4C6A9C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0ECFF),
    onSecondaryContainer = Color(0xFF0A2A66),
    tertiary = Color(0xFF7A5AF8),
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF35205C),
    background = Color(0xFFF8FAFF),
    onBackground = Color(0xFF0B1530),
    surface = Color(0xFFF8FAFF),
    onSurface = Color(0xFF0B1530),
    surfaceVariant = Color(0xFFE6EEFB),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceTint = Color(0xFF0867F9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEDF3FD),
    surfaceContainerHigh = Color(0xFFE6EEFB),
    surfaceContainerHighest = Color(0xFFDCE7F8),
    outline = Color(0xFF8A9BB8),
    outlineVariant = Color(0xFFE4EBF5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4C9BFF),
    onPrimary = Color(0xFF001B44),
    primaryContainer = Color(0xFF123C80),
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = Color(0xFF9DB6E3),
    onSecondary = Color(0xFF0A1F44),
    secondaryContainer = Color(0xFF14305F),
    onSecondaryContainer = Color(0xFFD6E6FF),
    tertiary = Color(0xFFB69CFF),
    tertiaryContainer = Color(0xFF483563),
    onTertiaryContainer = Color(0xFFF0DBFF),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE8F0FF),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE8F0FF),
    surfaceVariant = Color(0xFF0F2447),
    onSurfaceVariant = Color(0xFF93A8CC),
    surfaceTint = Color(0xFF4C9BFF),
    surfaceContainerLowest = Color(0xFF040F22),
    surfaceContainerLow = Color(0xFF1A1C22),
    surfaceContainer = Color(0xFF20232B),
    surfaceContainerHigh = Color(0xFF282C36),
    surfaceContainerHighest = Color(0xFF313642),
    outline = Color(0xFF4A6290),
    outlineVariant = Color(0xFF1D3A6B),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(textDirection = TextDirection.Content, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = 0.sp)

/** Material's default tracking (0.5sp on body text) looked wide and dated; this sets it to zero. */
private val AppTypography = Typography(
    headlineLarge = style(32, 40, FontWeight.Medium),
    headlineMedium = style(24, 28, FontWeight.Bold),
    headlineSmall = style(22, 28, FontWeight.Medium),
    titleLarge = style(20, 26, FontWeight.Bold),
    titleMedium = style(14, 20, FontWeight.SemiBold),
    titleSmall = style(12, 17, FontWeight.SemiBold),
    bodyLarge = style(14, 20),
    bodyMedium = style(12, 17),
    bodySmall = style(11, 15),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(11, 16, FontWeight.Medium),
)

@Composable
fun AgentTheme(appearance: String = "System", content: @Composable () -> Unit) {
    val colors: ColorScheme = if (appearance == "Dark" || (appearance == "System" && isSystemInDarkTheme())) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
}

/** Container / content colours for a status. Green = good, amber = needs attention, red = problem, neutral = information. */
@Composable
internal fun toneColors(tone: Tone): Pair<Color, Color> {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (tone) {
        Tone.SUCCESS -> if (dark) Color(0xFF1C2E21) to Color(0xFFB7E4C2) else Color(0xFFEAF6EC) to Color(0xFF14532D)
        Tone.WARNING -> if (dark) Color(0xFF35290E) to Color(0xFFF3D898) else Color(0xFFFDF3DC) to Color(0xFF6B4A00)
        Tone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Tone.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
}
