package org.aptura.player.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.theme.ApturaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for audit P1-03: "Skip" was drawn underneath the full-screen pager, so real touches never
 * reached it. `performClick()` injects an actual pointer event at the node's position (hit-tested), so an
 * occluded button fails this test exactly like it failed on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun skip_jumpsToThePermissionPage() {
        composeRule.setContent {
            ApturaTheme {
                OnboardingScreen(
                    onRequestPermissionClick = {},
                    onContinueWithoutPermissionClick = {}
                )
            }
        }
        composeRule.onNodeWithText("Absolute Privacy").assertIsDisplayed()

        composeRule.onNodeWithText("Skip").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Access Your Library").assertIsDisplayed()
        composeRule.onNodeWithText("Grant Media Access").assertIsDisplayed()
    }

    @Test
    fun continueButton_stillAdvancesOnePage() {
        composeRule.setContent {
            ApturaTheme {
                OnboardingScreen(onRequestPermissionClick = {}, onContinueWithoutPermissionClick = {})
            }
        }
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Precision Playback").assertIsDisplayed()
    }
}
