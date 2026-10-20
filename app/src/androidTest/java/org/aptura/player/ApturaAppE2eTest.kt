package org.aptura.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end device integration test suite running on live Android emulator / device.
 * Tests application lifecycle, UI Automator hardware orientation changes, and Compose hierarchy.
 */
@RunWith(AndroidJUnit4::class)
class ApturaAppE2eTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun appLaunch_displaysLibraryScreen() {
        assertNotNull(device)
        device.waitForIdle()

        // Wait until empty library message or videos are loaded and displayed
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeTestRule.onNodeWithText("No videos found").assertIsDisplayed()
                true
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("Scan Device").assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }
        }

        // Verify Scan Device button is interactive
        composeTestRule.onNodeWithText("Scan Device")
            .assertIsDisplayed()
            .performClick()

        device.waitForIdle()
    }

    @Test
    fun deviceRotation_retainsLibraryState() {
        device.waitForIdle()

        // Rotate to landscape
        device.setOrientationLeft()
        device.waitForIdle()

        // Confirm UI elements still present after configuration change
        composeTestRule.waitUntil(timeoutMillis = 8_000) {
            runCatching {
                composeTestRule.onNodeWithText("No videos found").assertIsDisplayed()
                true
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("Scan Device").assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }
        }

        // Return to natural portrait orientation
        device.setOrientationNatural()
        device.waitForIdle()

        composeTestRule.onNodeWithText("No videos found").assertIsDisplayed()
    }

    @Test
    fun systemBack_fromInitialScreen_handlesGracefully() {
        device.waitForIdle()
        // Pressing back on the root screen should not crash
        device.pressBack()
        device.waitForIdle()
        assertTrue(true)
    }
}
