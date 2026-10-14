package dev.deviceagent.app.ui.synapse

import android.app.Application
import android.os.Environment
import android.os.Process
import android.os.storage.StorageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import dev.deviceagent.app.MainActivity
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowEnvironment
import org.robolectric.shadows.ShadowStorageManager
import org.robolectric.shadows.StorageVolumeBuilder

/** Exercise the real activity/ViewModel wiring without credentials or device mutations. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SynapseActivityTests {
    // A real Android device has a primary volume. Robolectric starts with none;
    // register one before the Activity checks Android's All Files Access state.
    @get:Rule(order = 0)
    val storage =
        object : ExternalResource() {
            override fun before() {
                val app = ApplicationProvider.getApplicationContext<Application>()
                val path = File(app.cacheDir, "primary-storage").apply { mkdirs() }
                ShadowEnvironment.setExternalStorageDirectory(path.toPath())
                ShadowEnvironment.addExternalDir(path.absolutePath)
                val manager = app.getSystemService(StorageManager::class.java)
                Shadow.extract<ShadowStorageManager>(manager)
                    .addStorageVolume(
                        StorageVolumeBuilder(
                                "primary",
                                path,
                                "Test storage",
                                Process.myUserHandle(),
                                Environment.MEDIA_MOUNTED,
                            )
                            .setIsPrimary(true)
                            .setIsEmulated(true)
                            .build()
                    )
            }
        }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshInstallCanExploreDemoThenReturnToLiveDevice() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Try demo").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Try demo").performClick()
        compose.onNodeWithText("Demo data · simulated actions").assertExists()
        compose.onNodeWithText("Agent").performClick()
        compose.onNodeWithText("What can we do today?").assertExists()
        compose.onNodeWithText("Device").performClick()
        compose.onNodeWithText("Know your environment").assertExists()
        compose.onNodeWithText("Go live").performClick()
        compose.onNodeWithText("Environment overview").assertExists()
        compose.onNodeWithText("Demo data · simulated actions").assertDoesNotExist()
    }
}
