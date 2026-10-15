package dev.deviceagent.app.ui.synapse

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import dev.deviceagent.app.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h746dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SynapseV3InteractionTests {
    @get:Rule val compose = createComposeRule()

    @Test
    fun everyReferenceIsReachableFromDirectoryAndBackReturnsToAgent() {
        compose.setContent {
            AgentTheme("Light") {
                SynapseWorkspace(
                    DemoState(),
                    {},
                    SynapseRoute.AGENT,
                    "",
                    remember { SnackbarHostState() },
                    "Light",
                    {},
                    false,
                    {},
                    {},
                    initialDemo = true,
                ) { _, _, _ ->
                    error("Unexpected live operation")
                }
            }
        }
        referenceRoutes.forEachIndexed { index, route ->
            compose.onNodeWithContentDescription("Workspace menu").performClick()
            compose.onNodeWithText("All screens").performClick()
            compose
                .onNodeWithText("${(index+1).toString().padStart(2,'0')} · ${route.title}")
                .performScrollTo()
                .performClick()
            compose.onNodeWithText("Every screen, within reach").assertDoesNotExist()
            compose.onNodeWithTag("route-${route.name}").assertExists()
            if (route != SynapseRoute.AGENT) {
                compose.onNodeWithContentDescription("Back").performClick()
                compose.onNodeWithTag("route-AGENT").assertExists()
            }
        }
    }

    @Test
    fun rootEnablingRequiresConfirmationAndDetectionLossRevokesIt() {
        var state by mutableStateOf(DemoState())
        compose.setContent {
            AgentTheme {
                Surface(Modifier.fillMaxSize()) {
                    SecurityCatalogDemo(state, { state = reduceDemo(state, it) }, {})
                }
            }
        }
        compose.onNodeWithText("Enable demo root backend").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(state.rootEnabled) }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertFalse(state.rootEnabled) }
        compose.onNodeWithText("Enable demo root backend").performScrollTo().performClick()
        compose.onNodeWithText("Enable demo backend").performClick()
        compose.runOnIdle { assertTrue(state.rootEnabled) }
        compose.onNodeWithText("Detected  ⌄").performScrollTo().performClick()
        compose.onNodeWithText("Unavailable").performClick()
        compose.runOnIdle { assertFalse(state.rootEnabled) }
        compose.onNodeWithText("Enable demo root backend").assertIsNotEnabled()
    }

    @Test
    fun demoCredentialFormRejectsKeyLikeInputAndSavesOnlySampleState() {
        var state by mutableStateOf(DemoState().copy(credentialStored = false))
        compose.setContent {
            AgentTheme {
                Surface(Modifier.fillMaxSize()) {
                    CredentialCatalogDemo(state, { state = reduceDemo(state, it) }, {})
                }
            }
        }
        compose.onNodeWithText("+ Add credential").performScrollTo().performClick()
        compose.onNodeWithText("Save sample").assertIsNotEnabled()
        compose.onNodeWithText("Demo alias").performTextInput("sk-example-not-a-secret")
        compose.onNodeWithText("Save sample").assertIsNotEnabled()
        compose.onNodeWithText("Demo alias").performTextReplacement("Personal demo")
        compose.onNodeWithText("Save sample").performClick()
        compose.runOnIdle {
            assertTrue(state.credentialStored)
            assertFalse(state.connectionTested)
        }
        compose.onNodeWithText("demo-placeholder (not a key)").assertDoesNotExist()
        compose.onNodeWithText("Reveal sample").performScrollTo().performClick()
        compose.onNodeWithText("demo-placeholder (not a key)").assertExists()
    }

    @Test
    fun memoryEditorValidatesAndPersistsPreferenceWithoutChangingObservedFacts() {
        var state by mutableStateOf(DemoState())
        val facts = state.memories.filter { it.observed }
        compose.setContent {
            AgentTheme {
                Surface(Modifier.fillMaxSize()) {
                    MemoryCatalogDemo(state, { state = reduceDemo(state, it) }, {})
                }
            }
        }
        compose.onNodeWithText("Add preference").performScrollTo().performClick()
        compose.onNodeWithText("Save preference").assertIsNotEnabled()
        compose
            .onNode(hasText("Preference") and hasSetTextAction())
            .performTextInput("Keep source filenames")
        compose.onNodeWithText("Save preference").performClick()
        compose.runOnIdle {
            assertTrue(state.memories.any { it.text == "Keep source filenames" && !it.observed })
            assertEquals(facts, state.memories.filter { it.observed })
        }
        compose.onNodeWithText("Clear preferences").performScrollTo().performClick()
        compose.onNodeWithText("Forget").performClick()
        compose.runOnIdle { assertEquals(facts, state.memories) }
    }

    @Test
    fun sampleFolderFormRejectsInvalidAndDuplicateNames() {
        compose.setContent {
            AgentTheme {
                Surface(Modifier.fillMaxSize()) {
                    DemoRouteContent(SynapseRoute.SANDBOX, DemoState(), {}, {})
                }
            }
        }
        compose.onNodeWithText("+ New folder").performScrollTo().performClick()
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
        compose.onNode(hasText("Name") and hasSetTextAction()).performTextInput("nested/path")
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
        compose
            .onNode(hasText("Name") and hasSetTextAction())
            .performTextReplacement("Research notes")
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("Research notes").performScrollTo().assertExists()
        compose.onNodeWithText("+ New folder").performScrollTo().performClick()
        compose.onNode(hasText("Name") and hasSetTextAction()).performTextInput("Research notes")
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
    }

    @Test
    fun appearanceChoicesApplyToThemeAndSurviveViewModelRecreation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val first = SynapseWorkspaceViewModel(app, SavedStateHandle())
        var state by mutableStateOf(first.display.value)
        var theme by mutableStateOf("Light")
        var motion by mutableStateOf(false)
        var fontScale = 0f
        compose.setContent {
            CompositionLocalProvider(
                LocalSynapseAppearance provides state,
                LocalUpdateSynapseAppearance provides
                    { value ->
                        state = value
                        first.display(value)
                    },
            ) {
                AgentTheme(theme) {
                    fontScale =
                        androidx.compose.material3.MaterialTheme.typography.bodyMedium.fontSize
                            .value
                    Surface(Modifier.fillMaxSize()) {
                        AppearanceDesign(theme, { theme = it }, motion, { motion = it })
                    }
                }
            }
        }
        compose.onNodeWithText("Purple").performScrollTo().performClick()
        compose.onNodeWithText("Medium").performScrollTo().performClick()
        compose.onNodeWithText("High contrast mode").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("Purple", state.accent)
            assertEquals(12f * 1.12f, fontScale, .01f)
            assertTrue(state.highContrast)
            assertEquals(state, SynapseWorkspaceViewModel(app, SavedStateHandle()).display.value)
        }
    }
}
