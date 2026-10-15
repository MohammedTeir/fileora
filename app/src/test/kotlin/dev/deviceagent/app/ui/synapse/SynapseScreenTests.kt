package dev.deviceagent.app.ui.synapse

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.deviceagent.app.ui.AgentTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SynapseScreenTests {
    @get:Rule val compose = createComposeRule()
    private var renderedView: View? = null

    @Composable
    private fun TrackRoot() {
        val view = LocalView.current
        SideEffect { renderedView = view.rootView }
    }

    @Test
    fun everyDemoRouteRendersAndHasAllPreviewStates() {
        var route by mutableStateOf(SynapseRoute.WELCOME)
        var preview by mutableStateOf(PreviewState.READY)
        compose.setContent {
            TrackRoot()
            AgentTheme("Light") {
                Surface(Modifier.fillMaxSize()) {
                    key(route, preview) {
                        PreviewPane(route, preview, { preview = PreviewState.READY }) {
                            DemoRouteContent(route, DemoState(), {}, { route = it })
                        }
                    }
                }
            }
        }
        SynapseRoute.entries.forEach { target ->
            PreviewState.entries.forEach { state ->
                compose.runOnIdle {
                    route = target
                    preview = state
                }
                compose.waitForIdle()
                when (state) {
                    PreviewState.LOADING ->
                        compose.onNodeWithText("Show ready state").assertExists()
                    PreviewState.EMPTY -> compose.onNodeWithText("Load demo content").assertExists()
                    PreviewState.ERROR -> compose.onNodeWithText("Retry sample").assertExists()
                    PreviewState.SUCCESS -> compose.onNodeWithText("Success preview").assertExists()
                    PreviewState.OFFLINE -> compose.onNodeWithText("Offline preview").assertExists()
                    else -> compose.onRoot().assertExists()
                }
            }
            if (
                target in
                    listOf(
                        SynapseRoute.AGENT,
                        SynapseRoute.DEVICE,
                        SynapseRoute.RECOVERY,
                        SynapseRoute.PROVIDERS,
                    )
            ) {
                compose.runOnIdle { preview = PreviewState.READY }
                screenshot(target.name.lowercase())
            }
        }
    }

    @Test
    fun destructiveApprovalRequiresConsentAndStartsTheReviewedPlan() {
        var state by mutableStateOf(reduceDemo(DemoState(), DemoAction.IncludeTrash(true)))
        var route by mutableStateOf(SynapseRoute.APPROVAL)
        compose.setContent {
            TrackRoot()
            AgentTheme {
                Surface(Modifier.fillMaxSize()) {
                    key(route) {
                        DemoRouteContent(
                            route,
                            state,
                            { state = reduceDemo(state, it) },
                            { route = it },
                        )
                    }
                }
            }
        }
        compose
            .onNodeWithText("Approve 27 trash operations & run")
            .performScrollTo()
            .assertIsNotEnabled()
        compose
            .onNodeWithText("I reviewed the selected trash operations")
            .performScrollTo()
            .performClick()
        compose
            .onNodeWithText("Approve 27 trash operations & run")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        compose.runOnIdle {
            assertEquals(SynapseRoute.MONITOR, route)
            assertEquals(645, state.task.total)
            assertEquals(state.planRevision, state.approvedRevision)
        }
        compose.onNodeWithText("0 of 645 verified").assertExists()
    }

    @Test
    fun packageFlowDoesNotDeclareSuccessBeforeCapabilityProbe() {
        var state by
            mutableStateOf(reduceDemo(DemoState(), DemoAction.PackageSelected("ImageMagick")))
        compose.setContent {
            TrackRoot()
            AgentTheme {
                Surface(Modifier.fillMaxSize()) {
                    DemoRouteContent(
                        SynapseRoute.PACKAGE_DETAIL,
                        state,
                        { state = reduceDemo(state, it) },
                        {},
                    )
                }
            }
        }
        compose.onNodeWithText("Approve installation").performScrollTo().performClick()
        listOf(
                "download verified artifact",
                "check checksum & source",
                "install in userspace",
                "probe binary & capability",
            )
            .forEach { step ->
                compose.onNodeWithText("Capability verified").assertDoesNotExist()
                compose.onNodeWithText("Simulate $step").performScrollTo().performClick()
            }
        compose.onNodeWithText("Capability verified").assertExists()
        compose.runOnIdle { assertTrue("ImageMagick" in state.installed) }
    }

    @Test
    fun navigationAndDemoModeSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            TrackRoot()
            val snapshot = rememberSaveable { mutableStateOf(DemoStateCodec.encode(DemoState())) }
            val s = DemoStateCodec.decode(snapshot.value)
            AgentTheme {
                SynapseWorkspace(
                    s,
                    { snapshot.value = DemoStateCodec.encode(reduceDemo(s, it)) },
                    SynapseRoute.AGENT,
                    "Live model",
                    remember { SnackbarHostState() },
                    "System",
                    {},
                    false,
                    {},
                    {},
                ) { route, _, _ ->
                    Text("Live ${route.title}")
                }
            }
        }
        compose.onNodeWithText("Try demo").performClick()
        screenshot("workspace-agent")
        compose.onNodeWithText("Device").performClick()
        compose.onNodeWithText("Demo data · simulated actions").assertExists()
        compose.onNodeWithText("Device information").assertExists()
        screenshot("workspace-device")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Demo data · simulated actions").assertExists()
        compose.onNodeWithText("Device information").assertExists()
        compose.onNodeWithText("Go live").performClick()
        compose.onNodeWithText("Live Your device").assertExists()
        compose.onNodeWithText("Demo data · simulated actions").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w1100dp-h900dp")
    fun expandedDarkRtlLayoutAndLargeTextRemainNavigable() {
        compose.setContent {
            TrackRoot()
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, 1.5f),
            ) {
                AgentTheme("Dark") {
                    SynapseWorkspace(
                        DemoState(),
                        {},
                        SynapseRoute.AGENT,
                        "Live model",
                        remember { SnackbarHostState() },
                        "Dark",
                        {},
                        true,
                        {},
                        {},
                    ) { _, _, _ ->
                        Text("Live content")
                    }
                }
            }
        }
        compose.onNodeWithText("Try demo").performClick()
        compose.onNodeWithText("Workspace context").assertExists()
        compose.onNodeWithText("Device").performClick()
        compose.onNodeWithText("Device information").assertExists()
        screenshot("device-expanded-dark-rtl-large-text")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val file = File("build/outputs/synapse-screenshots/$name.png")
        requireNotNull(file.parentFile).mkdirs()
        // PixelCopy captureToImage requires a real display callback. Draw the native
        // Robolectric view tree directly so rendering is deterministic in headless CI.
        val bitmap =
            compose.runOnIdle {
                val view = requireNotNull(renderedView)
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
                    view.draw(Canvas(it))
                }
            }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 1000)
    }
}
