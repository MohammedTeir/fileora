package dev.deviceagent.app.ui.synapse

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
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
