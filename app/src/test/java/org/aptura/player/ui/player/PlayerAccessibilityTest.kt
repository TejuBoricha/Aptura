package org.aptura.player.ui.player

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit P1-10 / P1-11 on the REAL player screen, on a 360 dp phone (the width where the title used to collapse to
 * "lo…"): the seek bar is an adjustable control for TalkBack, every control has a 48 dp touch target, and the title
 * keeps the room it needs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class PlayerAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val queue = (1..3).map {
        DomainMediaItem(uri = "content://media/$it", title = "Video $it", durationMs = 120_000L, sizeBytes = 1L, mimeType = "video/mp4")
    }

    private class Calls {
        val scrub = mutableListOf<String>()
        val relativeSeeks = mutableListOf<Long>()
        var aspectToggles = 0
        var speedSheetOpens = 0
    }

    private fun show(
        calls: Calls = Calls(),
        title: String = "Show.S01E01",
        queueItems: List<DomainMediaItem> = queue,
        speed: Float = 1.0f
    ): Calls {
        composeRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = title,
                        durationMs = 120_000L,
                        currentPositionMs = 30_000L,
                        isPlaying = true,
                        isControlsVisible = true,
                        queueItems = queueItems,
                        playbackSpeed = speed,
                        hasNext = true,
                        hasPrevious = true
                    ),
                    player = null,
                    onBackClick = {},
                    onPlayPauseClick = {},
                    onSeekRelative = { calls.relativeSeeks += it },
                    onScrubStart = { calls.scrub += "start:$it" },
                    onScrubMove = { calls.scrub += "move:$it" },
                    onScrubEnd = { calls.scrub += "end" },
                    onControlsVisibilityToggle = {},
                    onLockToggle = {},
                    onSpeedSelected = {},
                    onResizeModeToggle = { calls.aspectToggles++ },
                    onSpeedSheetVisible = { if (it) calls.speedSheetOpens++ },
                    onAudioSheetVisible = {},
                    onSubtitleSheetVisible = {},
                    onAudioTrackSelected = {},
                    onSubtitleTrackSelected = {},
                    onVolumePercentChanged = {},
                    onBrightnessPercentChanged = {},
                    onRetryPlayback = {}
                )
            }
        }
        return calls
    }

    private fun SemanticsNodeInteraction.assertTouchTarget48(): SemanticsNodeInteraction =
        assertTouchTarget(composeRule.density)

    private fun countWithDescription(description: String): Int =
        composeRule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().size

    // ---- seek bar ----------------------------------------------------------------------------------------------

    @Test
    fun seekBar_reportsItsRangeAndPositionToTalkBack() {
        show()

        val node = composeRule.onNodeWithContentDescription("Seek bar").fetchSemanticsNode()

        val range = node.config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(30_000f, range.current, 0.5f)
        assertEquals(0f, range.range.start, 0f)
        assertEquals(120_000f, range.range.endInclusive, 0.5f)
        assertEquals("0:30 of 2:00", node.config[SemanticsProperties.StateDescription])
    }

    @Test
    fun seekBar_canBeAdjustedByAccessibilityServices() {
        val calls = show()

        composeRule.onNodeWithContentDescription("Seek bar")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(75_000f) }

        assertEquals(listOf("start:75000", "move:75000", "end"), calls.scrub)
    }

    @Test
    fun seekBar_ignoresRequestsOutsideTheVideo() {
        val calls = show()

        composeRule.onNodeWithContentDescription("Seek bar")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(9_999_999f) }

        assertEquals("clamped to the end, never past it", listOf("start:120000", "move:120000", "end"), calls.scrub)
    }

    @Test
    fun seekBar_offersRewindAndForwardAsCustomActions() {
        val calls = show()
        val actions = composeRule.onNodeWithContentDescription("Seek bar")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]

        assertEquals(listOf("Rewind 10 seconds", "Forward 10 seconds"), actions.map { it.label })
        actions.first { it.label.startsWith("Forward") }.action()
        actions.first { it.label.startsWith("Rewind") }.action()
        assertEquals(listOf(10_000L, -10_000L), calls.relativeSeeks)
    }

    // ---- touch targets ------------------------------------------------------------------------------------------

    @Test
    fun everyPlayerControl_hasAtLeast48dpTouchTarget() {
        show()

        listOf(
            "Back", "More options", "Playback Queue",
            "Previous Video", "Rewind 10s", "Pause", "Forward 10s", "Next Video",
            "Lock Controls", "Subtitles", "Audio Track", "Advanced Settings",
            "Play mode: play in order"
        ).forEach { description ->
            composeRule.onNodeWithContentDescription(description).assertTouchTarget48()
        }
    }

    @Test
    fun theSeekBar_isTallEnoughToHit() {
        show()
        composeRule.onNodeWithContentDescription("Seek bar").assertTouchTarget(composeRule.density, minWidthDp = 0f)
    }

    // ---- the 360 dp top bar -------------------------------------------------------------------------------------

    @Test
    fun onAPhone_theTitleKeepsAtLeast40PercentOfTheWidth_evenWithAQueueChip() {
        show(title = "Show.S01E01 The Long Title")

        // 360 dp wide screen: the title gets at least 144 dp
        composeRule.onNodeWithText("Show.S01E01 The Long Title").assertWidthIsAtLeast(144.dp)
    }

    @Test
    fun onAPhone_aShortTitleIsNotCutOff() {
        show(title = "Show.S01E01")

        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("Show.S01E01").fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)

        assertFalse("the title was truncated", layouts.first().hasVisualOverflow)
    }

    @Test
    fun onAPhone_secondaryActionsLiveInAMenu_andStillWork() {
        val calls = show()

        // not squeezed into the bar...
        assertEquals(0, countWithDescription("Aspect Ratio"))
        // ...but one tap away
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Aspect ratio").assertTouchTarget48().performClick()

        assertEquals(1, calls.aspectToggles)
    }

    @Test
    fun onAPhone_aNonNormalSpeedIsASuffixOnTheTitleLine_notAFloatingChip() {
        show(title = "Show.S01E01", speed = 1.5f)

        val suffix = composeRule.onNodeWithContentDescription("Playback speed 1.5x")
        suffix.assertTouchTarget48()
        val suffixBounds = suffix.fetchSemanticsNode().boundsInRoot
        val titleBounds = composeRule.onNodeWithText("Show.S01E01").fetchSemanticsNode().boundsInRoot

        assertTrue("the suffix follows the title on the same line: $suffixBounds vs $titleBounds", suffixBounds.left >= titleBounds.right - 1f)
        assertTrue(
            "the suffix is vertically within the title row, not on a line of its own",
            suffixBounds.center.y in titleBounds.top..titleBounds.bottom
        )
        assertEquals("the old floating chip is gone", 0, countWithDescription("Speed"))
    }

    @Test
    fun onAPhone_tappingTheSpeedSuffix_opensTheSpeedSheet() {
        val calls = show(speed = 1.5f)

        composeRule.onNodeWithContentDescription("Playback speed 1.5x").performClick()

        assertEquals(1, calls.speedSheetOpens)
    }

    @Test
    fun onAPhone_theSuffixCostsTheTitleOnlyItsOwnWidth() {
        // At 360 dp the title region is about 156 dp. The 48 dp suffix target comes out of that, so a long title keeps
        // at least 100 dp (two readable lines) rather than being squeezed to a few letters.
        show(title = "Show.S01E01 The Long Title", speed = 1.5f)

        composeRule.onNodeWithText("Show.S01E01 The Long Title").assertWidthIsAtLeast(100.dp)
    }

    @Test
    fun onAPhone_normalSpeedShowsNoSuffix() {
        show()

        val anySpeedLabel = composeRule.onAllNodes(hasContentDescription("Playback speed", substring = true)).fetchSemanticsNodes()
        assertEquals(0, anySpeedLabel.size)
    }

    @Test
    fun theMenuOffersEveryActionThatLeftTheBar() {
        show()
        composeRule.onNodeWithContentDescription("More options").performClick()

        listOf("Playback speed (1.0x)", "Aspect ratio", "Screen orientation", "Picture in picture").forEach { label ->
            composeRule.onNodeWithText(label).assertTouchTarget(composeRule.density, minWidthDp = 0f)
        }
    }
}
