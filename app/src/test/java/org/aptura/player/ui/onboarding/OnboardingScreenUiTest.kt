package org.aptura.player.ui.onboarding

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The welcome guide. `performClick()` injects a real pointer event at the node's position (hit-tested), so a button
 * drawn underneath something else fails here exactly like it did on a device (audit P1-03: "Skip" sat under the pager).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var permissionRequests = 0
    private var limitedModeChosen = 0
    private var finished = 0
    private var seen = 0

    private fun show(hasStorageAccess: Boolean = false) {
        composeRule.setContent {
            ApturaTheme {
                OnboardingScreen(
                    hasStorageAccess = hasStorageAccess,
                    onRequestPermissionClick = { permissionRequests++ },
                    onContinueWithoutPermissionClick = { limitedModeChosen++ },
                    onFinishClick = { finished++ },
                    onReachedLastPage = { seen++ }
                )
            }
        }
    }

    private fun next() {
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.waitForIdle()
    }

    // ---- navigation ------------------------------------------------------------------------------------------------

    @Test
    fun skip_jumpsToTheAccessPage() {
        show()
        composeRule.onNodeWithText("Everything stays on your device").assertIsDisplayed()

        composeRule.onNodeWithText("Skip").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Access Your Library").assertIsDisplayed()
        composeRule.onNodeWithText("Grant Media Access").assertIsDisplayed()
    }

    @Test
    fun continueButton_advancesOnePage() {
        show()

        next()

        composeRule.onNodeWithText("Find it, then pick up where you left off").assertIsDisplayed()
    }

    @Test
    fun back_returnsToThePreviousPage_andIsAbsentOnTheFirst() {
        show()
        composeRule.onNodeWithText("Back").assertDoesNotExist()

        next()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Everything stays on your device").assertIsDisplayed()
    }

    @Test
    fun theIndicator_saysWhichPageYouAreOn() {
        show()
        composeRule.onNodeWithContentDescription("Page 1 of 5").assertExists()

        next()

        composeRule.onNodeWithContentDescription("Page 2 of 5").assertExists()
    }

    // ---- content: the guide must describe what the app really has ----------------------------------------------------

    @Test
    fun everyPage_carriesItsContent() {
        show()
        composeRule.onNodeWithText("No internet permission").assertExists()
        composeRule.onNodeWithText("No accounts, ads or analytics").assertExists()

        next()
        composeRule.onNodeWithText("Search, filter and sort").assertExists()
        composeRule.onNodeWithText("Continue Watching").assertExists()
        composeRule.onNodeWithText("The ⋮ menu on any video").assertExists()

        next()
        // The map is one illustration for TalkBack, described in a single sentence rather than as loose fragments.
        composeRule.onNodeWithContentDescription("brightness", substring = true, ignoreCase = true).assertExists()
        composeRule.onNodeWithContentDescription("volume", substring = true, ignoreCase = true).assertExists()
        ExtraGestures.forEach { (name, _) -> composeRule.onNodeWithText(name).assertExists() }

        next()
        GuideTools.forEach { tool -> composeRule.onAllNodesWithText(tool.title).assertCountEquals(1) }
    }

    @Test
    fun theToolsList_hasNoDuplicatesAndNoBlanks() {
        assertEquals("tool titles must be unique", GuideTools.size, GuideTools.map { it.title }.toSet().size)
        assertTrue(GuideTools.all { it.title.isNotBlank() && it.detail.isNotBlank() })
        // The grid is two columns; an odd count would leave a hole.
        assertEquals(0, GuideTools.size % 2)
    }

    // ---- the last page depends on whether access is already granted ---------------------------------------------------

    @Test
    fun withoutAccess_theLastPageAsksForIt_orOffersLimitedMode() {
        show(hasStorageAccess = false)
        composeRule.onNodeWithText("Skip").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Grant Media Access").performClick()
        composeRule.onNodeWithText("Continue without access (Limited Mode)").performClick()

        assertEquals(1, permissionRequests)
        assertEquals(1, limitedModeChosen)
        composeRule.onNodeWithText("Start watching").assertDoesNotExist()
    }

    @Test
    fun withAccess_theLastPageJustLetsYouStart_andNeverAsksAgain() {
        show(hasStorageAccess = true)
        composeRule.onNodeWithText("Skip").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("You're all set").assertIsDisplayed()
        composeRule.onNodeWithText("Grant Media Access").assertDoesNotExist()
        composeRule.onNodeWithText("Continue without access (Limited Mode)").assertDoesNotExist()

        composeRule.onNodeWithText("Start watching").performClick()

        assertEquals(1, finished)
        assertEquals(0, permissionRequests)
    }

    // ---- the guide counts as seen once its last page is shown ---------------------------------------------------------

    @Test
    fun reachingTheLastPage_marksTheGuideAsSeen_exactlyOnce() {
        show()
        assertEquals("not seen on page 1", 0, seen)

        composeRule.onNodeWithText("Skip").performClick()
        composeRule.waitForIdle()

        assertEquals(1, seen)
    }
}
