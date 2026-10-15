package dev.deviceagent.app.ui.synapse

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dev.deviceagent.app.ui.SynapseAppearance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SynapseWorkspaceViewModel(application: Application, private val saved: SavedStateHandle) :
    AndroidViewModel(application) {
    private val preferences =
        application.getSharedPreferences("synapse_workspace", Context.MODE_PRIVATE)
    private val mutableDemo = MutableStateFlow(DemoStateCodec.decode(saved["demo_snapshot"]))
    val demo = mutableDemo.asStateFlow()
    private val mutableAppearance =
        MutableStateFlow(
            saved.get<String>("appearance")
                ?: preferences.getString("appearance", "System")
                ?: "System"
        )
    val appearance = mutableAppearance.asStateFlow()
    private val mutableReducedMotion =
        MutableStateFlow(
            saved.get<Boolean>("reduced_motion") ?: preferences.getBoolean("reduced_motion", false)
        )
    val reducedMotion = mutableReducedMotion.asStateFlow()

    private val mutableDisplay =
        MutableStateFlow(
            SynapseAppearance(
                accent = preferences.getString("display_accent", "Blue") ?: "Blue",
                customAccent = preferences.getString("display_custom_accent", "0867F9") ?: "0867F9",
                textSize = preferences.getString("display_text_size", "Small") ?: "Small",
                font = preferences.getString("display_font", "System default") ?: "System default",
                density = preferences.getString("display_density", "Comfortable") ?: "Comfortable",
                details = preferences.getBoolean("display_details", true),
                icons = preferences.getBoolean("display_icons", true),
                highContrast = preferences.getBoolean("display_high_contrast", false),
            )
        )
    val display = mutableDisplay.asStateFlow()

    fun display(value: SynapseAppearance) {
        mutableDisplay.value = value
        preferences
            .edit()
            .putString("display_accent", value.accent)
            .putString("display_custom_accent", value.customAccent)
            .putString("display_text_size", value.textSize)
            .putString("display_font", value.font)
            .putString("display_density", value.density)
            .putBoolean("display_details", value.details)
            .putBoolean("display_icons", value.icons)
            .putBoolean("display_high_contrast", value.highContrast)
            .apply()
    }

    fun dispatch(action: DemoAction) {
        mutableDemo.value = reduceDemo(mutableDemo.value, action)
        saved["demo_snapshot"] = DemoStateCodec.encode(mutableDemo.value)
    }

    fun resetDemo() {
        mutableDemo.value = DemoState()
        saved["demo_snapshot"] = DemoStateCodec.encode(mutableDemo.value)
    }

    fun appearance(value: String) {
        mutableAppearance.value = value
        saved["appearance"] = value
        preferences.edit().putString("appearance", value).apply()
    }

    fun reducedMotion(value: Boolean) {
        mutableReducedMotion.value = value
        saved["reduced_motion"] = value
        preferences.edit().putBoolean("reduced_motion", value).apply()
    }
}
