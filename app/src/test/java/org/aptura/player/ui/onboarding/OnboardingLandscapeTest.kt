package org.aptura.player.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The guide on a phone held sideways. The footer's dots, button and Back used to be stacked and took half of a 411 dp
 * screen, leaving the page a slot too small to show its picture. In landscape they share one row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-xhdpi")
class OnboardingLandscapeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var limited = 0
    private var requested = 0

    private fun show(hasStorageAccess: Boolean = false) {
        composeRule.setContent {
            ApturaTheme {
                OnboardingScreen(
                    hasStorageAccess = hasStorageAccess,
                    onRequestPermissionClick = { requested++ },
                    onContinueWithoutPermissionClick = { limited++ },
                    animateDemos = false
                )
            }
        }
    }

    @Test
    fun theFooter_takesAFifthOfTheScreen_notHalf() {
        show()
        val footer = composeRule.onNodeWithTag("guide_footer").getUnclippedBoundsInRoot()
        assertTrue("the footer is ${footer.height} tall on a 411 dp screen", footer.height.value <= 411f * 0.25f)
    }

    @Test
    fun everyFooterControl_isStillThereAndWorks() {
        show()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertDoesNotExist()

        composeRule.onNodeWithText("Continue").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Find it, then pick up where you left off").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Everything stays on your device").assertIsDisplayed()

        composeRule.onNodeWithText("Skip").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Allow access").performClick()
        composeRule.onNodeWithText("Continue with limited access").performClick()
        assertEquals(1, requested)
        assertEquals(1, limited)
    }
}
