package dev.deviceagent.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.deviceagent.app.Tone

private val LightColors = lightColorScheme(
    primary = Color(0xFF2457C5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0A1F52),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002E6B),
    primaryContainer = Color(0xFF0F3E94),
    onPrimaryContainer = Color(0xFFDCE6FF),
)

/** Follows the system light/dark setting (and the wallpaper colours on Android 12+). */
@Composable
fun AgentTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/** Container / content colours for a status. Green = good, amber = needs attention, red = problem, blue = information. */
@Composable
internal fun toneColors(tone: Tone): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return when (tone) {
        Tone.SUCCESS -> if (dark) Color(0xFF16351E) to Color(0xFFB7F0C2) else Color(0xFFDDF5E1) to Color(0xFF0F5B22)
        Tone.WARNING -> if (dark) Color(0xFF3D2F0B) to Color(0xFFFFDFA0) else Color(0xFFFFF0CC) to Color(0xFF6B4A00)
        Tone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Tone.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
}
