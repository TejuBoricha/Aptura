package org.aptura.player.ui.player

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import org.aptura.player.ui.library.MediaFormatters
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit I18N-02: in Arabic the player half mirrored. The transport row put Rewind on the right and Forward on the left, the
 * elapsed and total times swapped sides, and the gesture zones, which are a plain `Row`, swapped too: a double tap on the
 * left half fast-forwarded. A video's position runs left to right in every language and the gestures are named for the
 * side of the glass they sit on, so the player lays itself out left to right whatever the system layout direction is.
 *
 * Every test runs the same screen in a right-to-left environment, as a phone set to Arabic or Hebrew provides it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerRtlLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val seekCalls = mutableListOf<Pair<Long, SeekTapSide?>>()

    private fun render(direction: LayoutDirection, uiState: PlayerUiState) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                ApturaTheme {
                    PlayerScreen(
                        uiState = uiState,
                        player = null,
                        onBackClick = {},
                        onPlayPauseClick = {},
                        onSeekRelativeWithSide = { delta, side -> seekCalls += delta to side },
                        onScrubStart = {},
                        onScrubMove = {},
                        onScrubEnd = {},
                        onControlsVisibilityToggle = {},
                        onLockToggle = {},
                        onSpeedSelected = {},
                        onResizeModeToggle = {},
                        onSpeedSheetVisible = {},
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
        }
    }

    private fun controlsOn() = PlayerUiState(
        videoTitle = "Film",
        durationMs = 3_725_000L,
        currentPositionMs = 125_000L,
        isPlaying = true,
        isControlsVisible = true,
        hasPrevious = true,
        hasNext = true
    )

    private fun centerX(description: String): Float =
        composeTestRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.center.x

    private fun rootWidth(): Float = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot.width

    // ---- the transport row and the times ---------------------------------------------------------------------------

    @Test
    fun transportButtons_keepTheirOrder_inRightToLeft() {
        render(LayoutDirection.Rtl, controlsOn())

        val xs = listOf("Previous Video", "Rewind 10s", "Pause", "Forward 10s", "Next Video").map(::centerX)
        assertEquals("Previous, Rewind, Play, Forward, Next must run left to right: $xs", xs.sorted(), xs)
    }

    @Test
    fun elapsedTime_isOnTheLeftAndTotalOnTheRight_inRightToLeft() {
        render(LayoutDirection.Rtl, controlsOn())

        val elapsed = composeTestRule.onNodeWithText(MediaFormatters.formatDuration(125_000L)).fetchSemanticsNode().boundsInRoot.center.x
        val total = composeTestRule.onNodeWithText(MediaFormatters.formatDuration(3_725_000L)).fetchSemanticsNode().boundsInRoot.center.x
        assertTrue("elapsed ($elapsed) must be left of the total ($total)", elapsed < total)
    }

    // ---- the gestures and their feedback ---------------------------------------------------------------------------

    @Test
    fun doubleTapOnTheLeftHalf_rewinds_andOnTheRightHalf_fastForwards_inRightToLeft() {
        render(LayoutDirection.Rtl, controlsOn().copy(isControlsVisible = false))

        composeTestRule.onNodeWithContentDescription("Video").performTouchInput { doubleClick(Offset(width * 0.25f, height * 0.5f)) }
        composeTestRule.waitForIdle()
        assertEquals("a double tap on the left half must rewind: $seekCalls", listOf(-10_000L to SeekTapSide.LEFT), seekCalls)

        seekCalls.clear()
        composeTestRule.onNodeWithContentDescription("Video").performTouchInput { doubleClick(Offset(width * 0.75f, height * 0.5f)) }
        composeTestRule.waitForIdle()
        assertEquals("a double tap on the right half must fast-forward: $seekCalls", listOf(10_000L to SeekTapSide.RIGHT), seekCalls)
    }

    @Test
    fun brightnessGauge_isOnTheLeftAndVolumeOnTheRight_inRightToLeft() {
        render(LayoutDirection.Rtl, controlsOn().copy(isControlsVisible = false, brightnessPercent = 60, volumePercent = 40))
        composeTestRule.waitForIdle()

        val brightness = centerX("Brightness")
        val volume = centerX("Volume")
        val half = rootWidth() / 2f
        assertTrue("brightness ($brightness) must sit on the left half (< $half)", brightness < half)
        assertTrue("volume ($volume) must sit on the right half (> $half)", volume > half)
    }

    // ---- guards: the same checks in left-to-right, so the measurements above are known to be sound -------------------

    @Test
    fun theSameChecks_passInLeftToRight() {
        render(LayoutDirection.Ltr, controlsOn())

        val xs = listOf("Previous Video", "Rewind 10s", "Pause", "Forward 10s", "Next Video").map(::centerX)
        assertEquals(xs.sorted(), xs)
        val elapsed = composeTestRule.onNodeWithText(MediaFormatters.formatDuration(125_000L)).fetchSemanticsNode().boundsInRoot.center.x
        val total = composeTestRule.onNodeWithText(MediaFormatters.formatDuration(3_725_000L)).fetchSemanticsNode().boundsInRoot.center.x
        assertTrue(elapsed < total)
    }
}
