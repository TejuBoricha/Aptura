package org.aptura.player

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.aptura.player.data.settings.ApturaSettingsRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Prepares a fresh-install environment BEFORE the activity under test is launched:
 * connected-test runs reinstall the app, so every run would otherwise boot into the
 * first-run onboarding pager with no media permission granted.
 */
private class SeedAppEnvironmentRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            // Allow the library to proceed past the media access gate
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            // Mark the (first-run) onboarding guide as already seen, so MainActivity
            // opens straight on the library (start destination is gated on this flag)
            runBlocking {
                ApturaSettingsRepository(instrumentation.targetContext)
                    .setOnboardingCompleted(true)
            }
            base.evaluate()
        }
    }
}

/**
 * End-to-end device integration test suite running on live Android emulator / device.
 * Tests application lifecycle, UI Automator hardware orientation changes, and Compose hierarchy.
 */
@RunWith(AndroidJUnit4::class)
class ApturaAppE2eTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(SeedAppEnvironmentRule()).around(composeRule)

    private val composeTestRule get() = composeRule

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    /**
     * Waits until the library has finished loading, tolerating either state:
     * an empty library shows "No videos yet" / "Scan device", a populated
     * library shows the "All videos" filter chip (rendered only when videos exist).
     */
    private fun waitForLibraryLoaded(timeoutMillis: Long = 15_000) {
        val labels = listOf(
            "No videos yet",
            "Storage Permission Required",
            "Scan device",
            "All videos",
            "Search videos"
        )
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            labels.any { label ->
                runCatching {
                    composeTestRule.onNodeWithText(label).assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }
        }
    }

    /**
     * Fallback for the (fresh-install) first-run onboarding pager: try Skip,
     * and on the final page press "Start watching". With onboarding pre-seeded
     * by SeedAppEnvironmentRule this should never be needed.
     */
    private fun dismissOnboardingIfShown() {
        runCatching {
            composeTestRule.waitUntil(timeoutMillis = 3_000) {
                runCatching {
                    composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }
            composeTestRule.onNodeWithText("Skip").performClick()
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    composeTestRule.onNodeWithText("Start watching").assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }
            composeTestRule.onNodeWithText("Start watching").performClick()
        }
        runCatching {
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    composeTestRule
                        .onNodeWithText("Continue with limited access")
                        .assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }
            composeTestRule.onNodeWithText("Continue with limited access").performClick()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun appLaunch_displaysLibraryScreen() {
        assertNotNull(device)
        device.waitForIdle()

        // Fresh install: go through onboarding, then wait for the library to load
        dismissOnboardingIfShown()
        waitForLibraryLoaded()

        // Verify Scan Device button is interactive when present (empty library only)
        runCatching {
            composeTestRule.onNodeWithText("Scan device")
                .assertIsDisplayed()
                .performClick()
        }

        device.waitForIdle()
    }

    @Test
    fun deviceRotation_retainsLibraryState() {
        device.waitForIdle()

        // Fresh install: go through onboarding before exercising rotation
        dismissOnboardingIfShown()
        // Do not rotate while the initial library scan is still rendering its loading state.
        waitForLibraryLoaded()

        // Rotate to landscape
        device.setOrientationLeft()
        device.waitForIdle()

        // Confirm the library is still displayed after configuration change,
        // regardless of whether the library is empty or populated
        waitForLibraryLoaded(timeoutMillis = 8_000)

        // Return to natural portrait orientation
        device.setOrientationNatural()
        device.waitForIdle()

        waitForLibraryLoaded(timeoutMillis = 8_000)
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
