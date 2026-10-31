package org.aptura.player.ui.player

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audio-only mode, with the controls showing. The owner's phone showed the transport buttons drawn on top of the
 * headphone circle and over the video's title: the panel and the button cluster were both centred on the screen. The
 * panel now lives between the top bar and the cluster, and these tests hold that at every window shape a phone has.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioOnlyLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(fontScale: Float = 1f) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ApturaTheme {
                    PlayerScreen(
                        uiState = PlayerUiState(
                            videoTitle = "VID-20260928-WA0034",
                            durationMs = 51_000L,
                            currentPositionMs = 8_000L,
                            isPlaying = true,
                            isAudioOnly = true,
                            isControlsVisible = true,
                            hasNext = true,
                            hasPrevious = true
                        ),
                        player = null,
                        onBackClick = {}, onPlayPauseClick = {}, onSeekRelative = {}, onScrubStart = {}, onScrubMove = {},
                        onScrubEnd = {}, onControlsVisibilityToggle = {}, onLockToggle = {}, onSpeedSelected = {},
                        onResizeModeToggle = {}, onSpeedSheetVisible = {}, onAudioSheetVisible = {},
                        onSubtitleSheetVisible = {}, onAudioTrackSelected = {}, onSubtitleTrackSelected = {},
                        onVolumePercentChanged = {}, onBrightnessPercentChanged = {}, onRetryPlayback = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun DpRect.toRect() = Rect(left.value, top.value, right.value, bottom.value)

    private fun overlap(a: Rect, b: Rect) = a.intersect(b).let { it.width > 1f && it.height > 1f }

    private fun assertNothingCoversThePanel(windowWidth: Float = 411f, windowHeight: Float = 891f) {
        val panel = composeRule.onNodeWithTag(AUDIO_ONLY_PANEL_TAG).getUnclippedBoundsInRoot().toRect()
        assertTrue(
            "the panel at $panel must stay within window ($windowWidth x $windowHeight)",
            panel.left >= 0f && panel.top >= 0f && panel.right <= windowWidth && panel.bottom <= windowHeight
        )
        assertTrue("the panel at $panel must have non-zero size", panel.width > 0f && panel.height > 0f)

        val pieces = mapOf(
            "the title" to composeRule.onNode(hasText("VID-20260928-WA0034") and hasAnyAncestor(hasTestTag(AUDIO_ONLY_PANEL_TAG)))
                .getUnclippedBoundsInRoot().toRect(),
            "the Show video button" to composeRule.onNodeWithText("Show video").getUnclippedBoundsInRoot().toRect()
        )
        val controls = listOf("Back", "Previous Video", "Pause", "Next Video").associateWith {
            composeRule.onNodeWithContentDescription(it).getUnclippedBoundsInRoot().toRect()
        }
        controls.forEach { (name, bounds) ->
            assertTrue(
                "the $name control at $bounds must stay within window ($windowWidth x $windowHeight)",
                bounds.left >= 0f && bounds.top >= 0f && bounds.right <= windowWidth && bounds.bottom <= windowHeight
            )
            assertTrue("the $name control at $bounds must have non-zero size", bounds.width > 0f && bounds.height > 0f)
            assertTrue("the $name control at $bounds covers the audio-only panel at $panel", !overlap(panel, bounds))
            pieces.forEach { (piece, rect) ->
                assertTrue("the $name control at $bounds covers $piece at $rect", !overlap(rect, bounds))
            }
        }
    }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test
    fun portraitPhone() {
        show()
        assertNothingCoversThePanel(411f, 891f)
    }

    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    @Test
    fun smallPortraitPhone() {
        show()
        assertNothingCoversThePanel(320f, 640f)
    }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test
    fun portraitPhoneWithLargeText() {
        show(fontScale = 1.6f)
        assertNothingCoversThePanel(411f, 891f)
    }

    @Config(qualifiers = "w891dp-h411dp-xhdpi")
    @Test
    fun sidewaysPhone() {
        show()
        assertNothingCoversThePanel(891f, 411f)
    }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test
    fun smallSidewaysPhone() {
        show()
        assertNothingCoversThePanel(740f, 360f)
    }
}
