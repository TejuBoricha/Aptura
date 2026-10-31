package org.aptura.player.ui.onboarding

import android.view.ViewConfiguration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.LibraryDetail
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.library.videoMetadataLines
import org.aptura.player.ui.visual.captureScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The welcome guide's demonstrations (points 3 and 4 of the owner's list): the library page and the player page must
 * show the app's real parts, with real pictures and no placeholders, and must not behave like the screens they picture.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class GuideDemosTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var pageProgress by mutableFloatStateOf(1f)

    private fun show(motion: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        // A demonstration that plays never lets Compose go idle, so a playing one is stepped by hand from the start.
        if (motion) composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.DARK) {
                CompositionLocalProvider(LocalGuideMotion provides motion) {
                    GuidePageScope(progress = { pageProgress }) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) { content() }
                    }
                }
            }
        }
        if (motion) composeRule.mainClock.advanceTimeBy(50) else composeRule.waitForIdle()
    }

    private fun boundsOf(text: String) =
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    // ---- the library ---------------------------------------------------------------------------------------------

    @Test
    fun theLibraryPicture_isBuiltFromTheRealHeaderFolderBarAndCards() {
        show(motion = false) { LibraryDemo() }

        composeRule.onNodeWithText("Search videos", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("All videos", useUnmergedTree = true).assertExists()
        SampleLibrary.folders.forEach { folder ->
            assertTrue(
                "the folder bar has a chip for $folder",
                composeRule.onAllNodesWithText(folder, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            )
        }
        GuideSamples.forEach { sample ->
            composeRule.onNodeWithText(sample.card.title, useUnmergedTree = true).assertExists()
            // The small print under each title is what the library's own formatter writes for its standard detail.
            val line = videoMetadataLines(sample.card, LibraryDetail.STANDARD).single()
            composeRule.onNodeWithText(line, useUnmergedTree = true).assertExists()
            composeRule.onNodeWithText(sample.card.durationFormatted, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun everyVideoInThePicture_showsADrawnFilmFrame_inGridAndInList() {
        show(motion = false) { LibraryDemo() }

        fun assertEveryCardHasAFrame(layout: String) {
            GuideSamples.forEach { sample ->
                val frames = composeRule.onAllNodesWithTag(guideStillTag(sample.still!!), useUnmergedTree = true).fetchSemanticsNodes()
                assertTrue("$layout: ${sample.card.title} has no film frame (the library's blank placeholder shows instead)", frames.isNotEmpty())
            }
            val total = composeRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag("guide_still_0") or androidx.compose.ui.test.hasTestTag("guide_still_2") or
                    androidx.compose.ui.test.hasTestTag("guide_still_3") or androidx.compose.ui.test.hasTestTag("guide_still_4"),
                useUnmergedTree = true
            ).fetchSemanticsNodes().size
            assertEquals("$layout: one film frame for each of the ${GuideSamples.size} videos", GuideSamples.size, total)
        }
        assertEveryCardHasAFrame("grid")

        composeRule.onNodeWithText("List").performClick()
        composeRule.waitForIdle()
        assertEveryCardHasAFrame("list")
    }

    @Test
    fun theLayoutButtons_switchBetweenGridAndList() {
        show(motion = false) { LibraryDemo() }

        val gridFirst = boundsOf("Beach day")
        val gridSecond = boundsOf("Lecture 04")
        assertEquals("grid: two cards share a row", gridFirst.top, gridSecond.top, 2f)
        assertTrue("grid: the second card is to the right", gridSecond.left > gridFirst.left + 40f)

        composeRule.onNodeWithText("List").performClick()
        composeRule.waitForIdle()

        val listFirst = boundsOf("Beach day")
        val listSecond = boundsOf("Lecture 04")
        assertEquals("list: rows share a left edge", listFirst.left, listSecond.left, 2f)
        assertTrue("list: the second row is below the first", listSecond.top > listFirst.top + 40f)

        composeRule.onNodeWithText("Grid").performClick()
        composeRule.waitForIdle()
        assertEquals(boundsOf("Beach day").top, boundsOf("Lecture 04").top, 2f)
    }

    @Test
    fun theLibraryPicture_isAPicture_soTouchingItOpensNothing() {
        show(motion = false) { LibraryDemo() }

        // The real search field would take focus and raise the keyboard; the real menu button would open its menu.
        composeRule.onNodeWithText("Search videos", useUnmergedTree = true).performClick()
        composeRule.onNode(hasSetTextAction(), useUnmergedTree = true).assertIsNotFocused()

        composeRule.onNodeWithContentDescription("More actions for Beach day", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Video details", useUnmergedTree = true).assertDoesNotExist()
    }

    // ---- the player ----------------------------------------------------------------------------------------------

    @Test
    fun theResting_playerPicture_showsBothIndicators_andNamesTheHalves() {
        show(motion = false) { PlayerDemo() }

        composeRule.onNodeWithText("Brightness", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Volume", useUnmergedTree = true).assertExists()
        // One indicator for each half, the player's own gauges.
        composeRule.onNodeWithText("69%", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("55%", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Left half: brightness", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun thePlayerPicture_playsThroughEachGestureWhileInView_andHoldsStillWhenAway() {
        pageProgress = 0f
        show(motion = true) { PlayerDemo() }

        fun caption(step: PlayerDemoStep) =
            composeRule.onNodeWithText(step.caption(), useUnmergedTree = true)

        // Away from the page it does not run: it rests on its first gesture.
        composeRule.mainClock.advanceTimeBy(3_000)
        caption(PlayerDemoStep.BRIGHTNESS).assertExists()
        caption(PlayerDemoStep.VOLUME).assertDoesNotExist()

        // Once the page is centred it plays each gesture in turn, a little over two seconds each.
        pageProgress = 1f
        composeRule.mainClock.advanceTimeBy(200)
        caption(PlayerDemoStep.BRIGHTNESS).assertExists()
        composeRule.mainClock.advanceTimeBy(2_700)
        caption(PlayerDemoStep.VOLUME).assertExists()
        composeRule.mainClock.advanceTimeBy(2_600)
        caption(PlayerDemoStep.DOUBLE_TAP).assertExists()
        composeRule.mainClock.advanceTimeBy(2_600)
        caption(PlayerDemoStep.SCRUB).assertExists()
        composeRule.mainClock.advanceTimeBy(2_600)
        caption(PlayerDemoStep.BRIGHTNESS).assertExists()

        // Swiped away mid-way: it stops where it is instead of running behind another page.
        composeRule.mainClock.advanceTimeBy(2_700)
        caption(PlayerDemoStep.VOLUME).assertExists()
        pageProgress = 0.2f
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.mainClock.advanceTimeBy(6_000)
        caption(PlayerDemoStep.VOLUME).assertExists()
        caption(PlayerDemoStep.SCRUB).assertDoesNotExist()
    }

    @Test
    fun doubleTapInterval_staysBelowPlatformTimeout() {
        val totalMs = STEP_MS
        val gapMs = ((DOUBLE_TAP_SECOND_AT - DOUBLE_TAP_FIRST_AT) * totalMs).toLong()
        val platformTimeoutMs = ViewConfiguration.getDoubleTapTimeout().toLong()
        assertTrue(
            "double tap interval ($gapMs ms) must be inside platform double-tap window ($platformTimeoutMs ms)",
            gapMs < platformTimeoutMs
        )
        assertTrue("double tap interval ($gapMs ms) must be positive and distinct", gapMs > 50)
    }

    @Test
    fun playerDemo_reflectsCustomSeekDelta() {
        assertEquals(
            "Double-tap either side to jump 30 seconds",
            PlayerDemoStep.DOUBLE_TAP.caption(30)
        )
        pageProgress = 1f
        show(motion = true) { PlayerDemo(seekDeltaSeconds = 30) }
        // Step 1 BRIGHTNESS (2,550 ms) + Step 2 VOLUME (2,550 ms) -> Step 3 DOUBLE_TAP begins at 5,100 ms
        composeRule.mainClock.advanceTimeBy(5_300)
        composeRule.onNodeWithText("Double-tap either side to jump 30 seconds", useUnmergedTree = true).assertExists()
        // Overlay HUD appears after second tap at t >= 0.24 (~504 ms into step)
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.onNodeWithText("+30s", substring = true, useUnmergedTree = true).assertExists()
    }

    @Config(fontScale = 2.0f)
    @Test
    fun at200PercentFont_playerDemoCaption_doesNotClip() {
        show(motion = false) { PlayerDemo() }
        val node = composeRule.onNodeWithText("Left half: brightness", substring = true, useUnmergedTree = true)
        node.assertExists()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        val minHeightPx = with(composeRule.density) { 40.dp.toPx() }
        assertTrue(
            "caption height at 200% font (${bounds.height}px) must grow beyond the 40dp minimum floor (${minHeightPx}px) to prevent clipping",
            bounds.height > minHeightPx
        )
    }

    @Test
    fun theGestureGauges_followTheFinger() {
        pageProgress = 1f
        show(motion = true) { PlayerDemo() }

        // The brightness gauge rises from 35% to 80% over the first gesture.
        composeRule.mainClock.advanceTimeBy(100)
        val early = percentShown()
        composeRule.mainClock.advanceTimeBy(1_900)
        val late = percentShown()
        assertTrue("brightness rose from $early% to $late%", late > early + 20)
        assertNotEquals(early, late)

        // The volume gauge rises over the second gesture.
        composeRule.mainClock.advanceTimeBy(700)
        val earlyVolume = percentShown()
        composeRule.mainClock.advanceTimeBy(1_800)
        val lateVolume = percentShown()
        assertTrue("volume rose from $earlyVolume% to $lateVolume%", lateVolume > earlyVolume + 15)

        // Step 4 scrub caption appears.
        composeRule.mainClock.advanceTimeBy(3_400)
        composeRule.onNodeWithText("Swipe left or right to scrub through the video", useUnmergedTree = true).assertExists()
    }

    private fun percentShown(): Int {
        for (percent in 30..85) {
            if (composeRule.onAllNodesWithText("$percent%", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return percent
        }
        error("no gauge is showing a percentage")
    }
}
