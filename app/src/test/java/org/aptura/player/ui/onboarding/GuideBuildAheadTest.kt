package org.aptura.player.ui.onboarding

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.ui.visual.captureScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Going back and forth through the welcome guide felt laggy (the owner's report). Each page is a screen's worth of real
 * composables, and the pager built a page on the frame a swipe began and threw it away when the page was left: measured
 * with a scripted Next/Back lap, every page turn had one frame of 40 to 130 ms among frames of 2 to 5 ms. The pages are
 * now built behind the first one once it has settled, and kept.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class GuideBuildAheadTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show() {
        composeRule.setContent {
            ApturaTheme {
                OnboardingScreen(onRequestPermissionClick = {}, onContinueWithoutPermissionClick = {}, animateDemos = false)
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
    }

    private fun letTimePass(ms: Long) {
        composeRule.mainClock.advanceTimeBy(ms)
        composeRule.waitForIdle()
    }

    private fun turn(button: String) {
        composeRule.onNodeWithText(button).performClick()
        letTimePass(1_000)
    }

    @Test
    fun theFirstPageComesFirst_theOthersAreBuiltBehindItOnceItHasSettled() {
        show()
        letTimePass(500)
        composeRule.onNodeWithTag(GUIDE_LIBRARY_DEMO_TAG).assertDoesNotExist()

        letTimePass(GUIDE_BUILD_AHEAD_START_MS + GUIDE_BUILD_AHEAD_STEP_MS * 5)
        composeRule.onNodeWithTag(GUIDE_LIBRARY_DEMO_TAG).assertExists()
        composeRule.onNodeWithTag(GUIDE_PLAYER_DEMO_TAG).assertExists()
    }

    @Test
    fun aPageThatWasLeft_isStillThereWhenYouComeBack_withWhatYouDidOnIt() {
        show()
        letTimePass(GUIDE_BUILD_AHEAD_START_MS + GUIDE_BUILD_AHEAD_STEP_MS * 5)
        turn("Continue") // the library page
        composeRule.onNodeWithText("List").performClick()
        letTimePass(500)
        composeRule.onNodeWithContentDescription("A picture of the library", substring = true).assertExists()

        turn("Continue")
        turn("Continue")
        turn("Back")
        turn("Back")

        // Had the page been thrown away, the choice would be back on Grid.
        composeRule.onNodeWithContentDescription("as a list", substring = true).assertExists()
    }

    // ---- pictures that move only on the page in front -------------------------------------------------------------

    private var progress by mutableFloatStateOf(0f)

    @Test
    fun aPageIsActiveOnlyWhileItIsCloseToTheFront() {
        composeRule.setContent {
            GuidePageScope(progress = { progress }) {
                Text(if (rememberGuidePageActive()) "active" else "resting")
            }
        }
        composeRule.onNodeWithText("resting").assertExists()

        progress = 0.8f
        composeRule.waitForIdle()
        composeRule.onNodeWithText("active").assertExists()

        progress = 0.2f
        composeRule.waitForIdle()
        composeRule.onNodeWithText("resting").assertExists()
    }

    /** The pixels of the rings' area, as a list, so that two moments can be compared. */
    private fun ringsPixels(): List<Int> {
        val screen = composeRule.captureScreen()
        val out = mutableListOf<Int>()
        for (gx in 0 until 40) for (gy in 0 until 14) {
            out += screen.colorAt(screen.width * gx / 40, screen.height / 6 + gy * 8).value.toLong().hashCode()
        }
        return out
    }

    @Test
    fun theWelcomeRingsSpreadOnlyWhileTheirPageIsInFront() {
        composeRule.setContent {
            ApturaTheme {
                GuidePageScope(progress = { progress }) { WelcomeHero() }
            }
        }

        progress = 0f // a page away, built behind the front one
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(300)
        val before = ringsPixels()
        composeRule.mainClock.advanceTimeBy(1_300)
        assertEquals("the rings moved on a page that is not in front", before, ringsPixels())

        progress = 1f // in front
        composeRule.mainClock.advanceTimeBy(300)
        val front = ringsPixels()
        composeRule.mainClock.advanceTimeBy(1_300)
        assertNotEquals("the rings did not move on the page in front", front, ringsPixels())
    }
}
