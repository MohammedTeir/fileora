package dev.deviceagent.app.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Display preferences only. These never change execution policy or Android permissions. */
@Immutable
data class SynapseAppearance(
    val accent: String = "Blue",
    val customAccent: String = "0867F9",
    val textSize: String = "Small",
    val font: String = "System default",
    val density: String = "Comfortable",
    val details: Boolean = true,
    val icons: Boolean = true,
    val highContrast: Boolean = false,
) {
    val fontMultiplier: Float
        get() =
            when (textSize) {
                "Medium" -> 1.12f
                "Large" -> 1.25f
                "Extra large" -> 1.4f
                else -> 1f
            }

    val accentColor: Color
        get() =
            when (accent) {
                "Purple" -> Color(0xFF7C3AED)
                "Green" -> Color(0xFF00B84A)
                "Orange" -> Color(0xFFF59E0B)
                "Red" -> Color(0xFFF21A31)
                "Pink" -> Color(0xFFF72EA6)
                "Custom" ->
                    Color(0xFF000000 or (customAccent.toLongOrNull(16)?.and(0xFFFFFF) ?: 0x0867F9))
                else -> Color(0xFF0867F9)
            }
}

val LocalSynapseAppearance = staticCompositionLocalOf { SynapseAppearance() }
val LocalUpdateSynapseAppearance = staticCompositionLocalOf<(SynapseAppearance) -> Unit> { {} }
