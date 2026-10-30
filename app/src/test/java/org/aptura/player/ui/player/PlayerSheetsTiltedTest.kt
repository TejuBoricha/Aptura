package org.aptura.player.ui.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * "Unable to pull the media player options when the phone is tilted ... it's for any options" (Samsung S24, sideways).
 *
 * Every options sheet of the player, on a screen the shape of a tilted phone: it can be pulled up when it opens half way
 * (or is already all the way up), it never swings above its top, and a flick down closes it. The title of each sheet is
 * the thing to watch, frame by frame.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h370dp-xhdpi")
class PlayerSheetsTiltedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent { ApturaTheme { content() } }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1_500)
    }

    private fun show(sheet: PlayerSheet) = showContent {
        PlayerScreen(
            uiState = PlayerUiState(
                videoTitle = "Arrival",
                durationMs = 6_960_000L,
                currentPositionMs = 1_000_000L,
                isPlaying = false,
                isControlsVisible = false,
                activeSheet = sheet
            ),
            player = null,
            onBackClick = {}, onPlayPauseClick = {}, onSeekRelative = {}, onScrubStart = {}, onScrubMove = {},
            onScrubEnd = {}, onControlsVisibilityToggle = {}, onLockToggle = {}, onSpeedSelected = {},
            onResizeModeToggle = {}, onSpeedSheetVisible = {}, onAudioSheetVisible = {},
            onSubtitleSheetVisible = {}, onAudioTrackSelected = {}, onSubtitleTrackSelected = {},
            onVolumePercentChanged = {}, onBrightnessPercentChanged = {}, onRetryPlayback = {}
        )
    }

    /** The compatibility sheet, on its own: inside the full player screen the test cannot tell its title from the screen behind it. */
    private fun showCompatibility() = showContent {
        CompatibilityExplainerSheet(
            failedCodecName = "c2.exynos.hevc.decoder", failureReason = "The hardware decoder stopped answering",
            activeDecoderPath = "Software", currentStrategy = 2, onStrategySelected = {}, onDismissRequest = {}
        )
    }

    /** The title of the sheet is the lowest node with that text (the same words can appear on the screen behind it). */
    private fun sheetTitleIndex(title: String): Int =
        composeRule.onAllNodesWithText(title).fetchSemanticsNodes().indices
            .maxByOrNull { composeRule.onAllNodesWithText(title)[it].getUnclippedBoundsInRoot().top.value } ?: 0

    private fun titleTop(title: String): Float =
        composeRule.onAllNodesWithText(title)[sheetTitleIndex(title)].getUnclippedBoundsInRoot().top.value

    private fun pullUp(title: String) {
        composeRule.mainClock.autoAdvance = false
        composeRule.onAllNodesWithText(title)[sheetTitleIndex(title)].performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(0f, -30.dp.toPx()), delayMillis = 16) }
            up()
        }
    }

    /** Flicked down on the sheet's handle: it is always on screen, however far the content has scrolled. */
    private fun flickDown() {
        composeRule.mainClock.autoAdvance = false
        composeRule.onNode(hasContentDescription("Drag handle", substring = true)).performTouchInput {
            down(center)
            repeat(8) { moveBy(Offset(0f, 40.dp.toPx()), delayMillis = 12) }
            up()
        }
    }

    private fun frames(title: String, count: Int): List<Float> = List(count) {
        composeRule.mainClock.advanceTimeBy(16)
        composeRule.waitForIdle()
        titleTop(title)
    }

    private fun check(title: String, open: () -> Unit) {
        open()
        val before = titleTop(title)

        pullUp(title)
        val rising = frames(title, 70)
        val after = rising.last()
        assertTrue(
            "$title: the sheet could not be pulled up (title at $before dp, then $after dp: $rising)",
            after < before - 15f || (before <= 60f && rising.max() - rising.min() <= 1f) || (rising.min() < before - 10f && after <= rising.min() + 1f)
        )
        assertTrue("$title: it swung above where it stops and came back: $rising", rising.min() >= after - 1f)

        // A sheet that opens half way is closed in two flicks (all the way down, then away).
        flickDown()
        frames(title, 60)
        flickDown()
        val falling = frames(title, 120)
        val handleTop = composeRule.onNode(hasContentDescription("Drag handle", substring = true)).getUnclippedBoundsInRoot().top.value
        val everyTitle = composeRule.onAllNodesWithText(title).fetchSemanticsNodes().indices.map { composeRule.onAllNodesWithText(title)[it].getUnclippedBoundsInRoot().top.value }
        assertTrue("$title: flicks down did not close the sheet (title at ${falling.last()} dp; started at $before; while pulling up: ${rising.filterIndexed { i, _ -> i % 5 == 0 }}; every node with that text: $everyTitle; frames: ${falling.filterIndexed { i, _ -> i % 10 == 0 }})", (falling.last() > 370f || handleTop >= 369f))
    }

    @Test fun subtitles() = check("Subtitles") { show(PlayerSheet.Subtitle) }
    @Test fun audio() = check("Audio") { show(PlayerSheet.Audio) }
    @Test fun speed() = check("Playback speed") { show(PlayerSheet.Speed) }
    @Test fun playbackTools() = check("Playback tools") { show(PlayerSheet.Advanced) }
    @Test fun softwareDecoding() = check("Software decoding") { showCompatibility() }
}
