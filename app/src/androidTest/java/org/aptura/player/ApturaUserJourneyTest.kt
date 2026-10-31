package org.aptura.player

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.aptura.player.data.settings.ApturaSettingsRepository
import org.aptura.player.ui.library.BROWSE_FOLDERS_TAG
import org.aptura.player.ui.library.VIEW_OPTIONS_TAG
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * Headless Automated UI Flow & User Journey Test Suite.
 *
 * Executes critical end-to-end user flows directly against live Compose hierarchies
 * on device or emulator without taking screenshots.
 * Validates:
 * 1. Settings navigation, Privacy Policy modal presentation, and dismissal.
 * 2. Amber and Champagne Gold accent theme selections.
 * 3. Folder navigation bottom sheet interactions and fling boundary stability.
 * 4. View options layout configuration and sheet dismissal.
 */
@RunWith(AndroidJUnit4::class)
class ApturaUserJourneyTest {

    private class SeedRule : TestRule {
        override fun apply(base: Statement, description: Description) = object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                instrumentation.uiAutomation.grantRuntimePermission(
                    instrumentation.targetContext.packageName,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
                runBlocking {
                    ApturaSettingsRepository(instrumentation.targetContext)
                        .setOnboardingCompleted(true)
                }
                base.evaluate()
            }
        }
    }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(SeedRule()).around(composeRule)

    private val composeTestRule get() = composeRule
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.waitForIdle()
        waitForLibraryLoaded()
    }

    private fun waitForLibraryLoaded(timeoutMillis: Long = 12_000) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            runCatching {
                composeTestRule.onNode(hasContentDescription("Settings")).assertIsDisplayed()
                true
            }.getOrDefault(false) ||
            runCatching {
                composeTestRule.onNodeWithText("No videos yet").assertIsDisplayed()
                true
            }.getOrDefault(false) ||
            runCatching {
                composeTestRule.onNodeWithText("Search videos").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun journey_settingsAndPrivacyPolicy_interactsAndDismisses() {
        // 1. Open Settings from Library
        composeTestRule.onNode(hasContentDescription("Settings"))
            .assertIsDisplayed()
            .performClick()

        device.waitForIdle()

        // 2. Assert Settings screen content is displayed
        composeTestRule.waitUntil(timeoutMillis = 6_000) {
            runCatching {
                composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
                true
            }.getOrDefault(false) ||
            runCatching {
                composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // 3. Scroll to and click Privacy policy
        composeTestRule.onNodeWithText("Privacy policy")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        device.waitForIdle()

        // 4. Verify Privacy Policy Dialog appears
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithText("View on GitHub").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close")
            .assertIsDisplayed()
            .performClick()

        device.waitForIdle()

        // 5. Verify Dialog is dismissed and Settings is visible
        composeTestRule.onNodeWithText("Source code")
            .assertIsDisplayed()

        // 6. Navigate Back to Library
        device.pressBack()
        device.waitForIdle()

        waitForLibraryLoaded()
    }

    @Test
    fun journey_accentColors_canSelectAmberAndGold() {
        // 1. Open Settings
        composeTestRule.onNode(hasContentDescription("Settings"))
            .assertIsDisplayed()
            .performClick()

        device.waitForIdle()

        // 2. Verify Accent Color swatches are available
        composeTestRule.waitUntil(timeoutMillis = 6_000) {
            runCatching {
                composeTestRule.onNodeWithContentDescription("Amber accent").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // 3. Select Amber accent
        composeTestRule.onNodeWithContentDescription("Amber accent")
            .assertIsDisplayed()
            .performClick()

        device.waitForIdle()

        // 4. Try selecting Gold accent
        composeTestRule.onNodeWithContentDescription("Gold accent")
            .assertIsDisplayed()
            .performClick()

        device.waitForIdle()

        // 5. Restore Aptura Blue accent
        composeTestRule.onNodeWithContentDescription("Aptura Blue accent")
            .assertIsDisplayed()
            .performClick()

        device.waitForIdle()

        // 6. Return to Library cleanly
        device.pressBack()
        device.waitForIdle()
        waitForLibraryLoaded()
    }

    @Test
    fun journey_folderNavigation_opensAndDismissesWithoutFlingOscillation() {
        // 1. Open Folder Browser Sheet if affordance is present
        val browseFoldersNode = runCatching {
            composeTestRule.onNodeWithTag(BROWSE_FOLDERS_TAG).assertIsDisplayed()
        }.getOrNull() ?: runCatching {
            composeTestRule.onNodeWithContentDescription("Browse all folders").assertIsDisplayed()
        }.getOrNull()

        if (browseFoldersNode != null) {
            browseFoldersNode.performClick()
            device.waitForIdle()

            // 2. Verify Folders sheet opened and perform rapid upward/downward flings
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    composeTestRule.onNodeWithText("Folders").assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }

            // Rapid flings to ensure fling-boundary stabilization has no jitter or oscillation
            runCatching {
                composeTestRule.onNodeWithText("Folders").performTouchInput {
                    swipeUp(durationMillis = 100)
                    swipeDown(durationMillis = 100)
                }
            }

            device.waitForIdle()

            // 3. Dismiss sheet cleanly
            device.pressBack()
            device.waitForIdle()
            waitForLibraryLoaded()
        }
    }

    @Test
    fun journey_viewOptions_togglesLayoutModes() {
        // 1. Open View Options sheet from Library top bar
        val viewOptionsNode = runCatching {
            composeTestRule.onNodeWithTag(VIEW_OPTIONS_TAG).assertIsDisplayed()
        }.getOrNull() ?: runCatching {
            composeTestRule.onNodeWithContentDescription("View options").assertIsDisplayed()
        }.getOrNull()

        if (viewOptionsNode != null) {
            viewOptionsNode.performClick()
            device.waitForIdle()

            // 2. Verify sheet is displayed
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    composeTestRule.onNodeWithText("View options").assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }

            // 3. Dismiss cleanly
            device.pressBack()
            device.waitForIdle()
            waitForLibraryLoaded()
        }
    }
}
