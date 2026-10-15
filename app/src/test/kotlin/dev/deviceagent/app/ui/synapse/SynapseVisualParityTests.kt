package dev.deviceagent.app.ui.synapse

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import dev.deviceagent.app.ui.AgentTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real Compose tree, native Android canvas, fixed phone viewport. No reference image is rendered. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h746dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SynapseVisualParityTests {
    @get:Rule val compose = createComposeRule()
    private fun capture(pages: IntRange) {
        var page by mutableIntStateOf(pages.first)
        var root: View? = null
        compose.setContent {
            val view = LocalView.current
            SideEffect { root = view.rootView }
            key(page) {
                var state by remember { mutableStateOf(DemoState()) }
                AgentTheme("Light") {
                    SynapseWorkspace(state,{ state = reduceDemo(state,it) },referenceRoutes[page-1],"",remember { SnackbarHostState() },"Light",{},false,{}, {}, initialDemo = true) { _,_,_ -> error("Visual fixtures must stay in Demo") }
                }
            }
        }
        pages.forEach { n ->
            compose.runOnIdle { page = n }
            compose.waitForIdle()
            val bitmap = compose.runOnIdle {
                val v = requireNotNull(root)
                Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888).also { v.draw(Canvas(it)) }
            }
            val file = File("build/outputs/visual-parity/screen_${n.toString().padStart(2,'0')}.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            assertTrue("Rendered screen $n",file.length() > 3000)
        }
    }
    @Test fun agentEntry() = capture(1..2)
    @Test fun agentPlanning() = capture(3..5)
    @Test fun agentExecution() = capture(6..8)
    @Test fun activityOverview() = capture(9..11)
    @Test fun activityDetails() = capture(12..13)
    @Test fun deviceOverview() = capture(14..16)
    @Test fun deviceTools() = capture(17..20)
    @Test fun settingsProviders() = capture(21..24)
    @Test fun settingsPolicies() = capture(25..28)
    @Test fun settingsPreferences() = capture(29..31)
}
