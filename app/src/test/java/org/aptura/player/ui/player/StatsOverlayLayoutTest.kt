package org.aptura.player.ui.player

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import org.aptura.player.domain.model.PlaybackStatsInfo
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The stats panel with the controls showing. It hangs from the top right and the transport buttons are centred, so on a
 * phone held sideways it sat on the forward and next buttons and the seek bar, hid them and swallowed their taps. The
 * buttons now move aside, and the panel stays clear of the seek bar; these tests hold that at every window shape.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatsOverlayLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(fontScale: Float = 1f) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ApturaTheme {
                    PlayerScreen(
                        uiState = PlayerUiState(
                            videoTitle = "Blade Runner 2049",
                            durationMs = 9_840_000L,
                            currentPositionMs = 2_100_000L,
                            isPlaying = true,
                            isControlsVisible = true,
                            hasNext = true,
                            hasPrevious = true,
                            isStatsOverlayVisible = true,
                            playbackStats = PlaybackStatsInfo(
                                videoDecoderName = "c2.qti.avc.decoder",
                                videoCodec = "video/avc",
                                videoResolution = "1920x1080",
                                frameRate = 29.97f,
                                videoBitrateKbps = 4500,
                                audioDecoderName = "c2.android.aac.decoder",
                                audioCodec = "audio/mp4a-latm",
                                audioChannels = "Stereo (2 ch)",
                                audioSampleRateHz = 48000,
                                audioBitrateKbps = 192
                            )
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

    /** Everything the viewer can press or drag, except the panel's own buttons, with the panel's room kept clear. */
    private fun assertPanelCoversNothing(windowWidth: Float, windowHeight: Float) {
        val panel = composeRule.onNodeWithTag(STATS_TAG).getUnclippedBoundsInRoot().toRect()
        assertTrue(
            "the panel at $panel leaves the window",
            panel.left >= 0f && panel.top >= 0f && panel.right <= windowWidth && panel.bottom <= windowHeight
        )
        assertTrue("the panel at $panel must have non-zero size", panel.width > 0f && panel.height > 0f)

        val outsidePanel = hasClickAction() and !hasTestTag(STATS_TAG) and !hasAnyAncestor(hasTestTag(STATS_TAG))
        val controls = composeRule.onAllNodes(outsidePanel)
        val count = controls.fetchSemanticsNodes().size
        assertTrue("expected the player's buttons to be found, found $count", count >= 8)
        for (index in 0 until count) {
            val bounds = controls[index].getUnclippedBoundsInRoot().toRect()
            assertTrue(
                "control $index at $bounds must be contained within the window",
                bounds.left >= 0f && bounds.top >= 0f && bounds.right <= windowWidth && bounds.bottom <= windowHeight
            )
            assertTrue("control $index at $bounds must have non-zero size", bounds.width > 0f && bounds.height > 0f)
            assertTrue("a control at $bounds is covered by the stats panel at $panel", !overlap(panel, bounds))
        }
        // The seek bar is a drawing with semantics rather than a button, so it is named.
        val seekBar = composeRule.onNodeWithContentDescription("Seek bar").getUnclippedBoundsInRoot().toRect()
        assertTrue(
            "the seek bar at $seekBar must be on screen",
            seekBar.left >= 0f && seekBar.top >= 0f && seekBar.right <= windowWidth && seekBar.bottom <= windowHeight
        )
        assertTrue("the seek bar at $seekBar is covered by the stats panel at $panel", !overlap(panel, seekBar))
    }

    @Config(qualifiers = "w891dp-h411dp-xhdpi")
    @Test
    fun aSidewaysPhone() {
        show()
        assertPanelCoversNothing(891f, 411f)
    }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test
    fun aSmallSidewaysPhone() {
        show()
        assertPanelCoversNothing(740f, 360f)
    }

    @Config(qualifiers = "w1024dp-h600dp-xhdpi")
    @Test
    fun aSmallTabletSideways() {
        show()
        assertPanelCoversNothing(1024f, 600f)
    }

    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    @Test
    fun aTabletSideways() {
        show()
        assertPanelCoversNothing(1280f, 800f)
    }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test
    fun anUprightPhone() {
        show()
        assertPanelCoversNothing(411f, 891f)
    }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test
    fun anUprightPhoneWithLargeText() {
        show(fontScale = 1.6f)
        assertPanelCoversNothing(411f, 891f)
    }

    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    @Test
    fun aSmallUprightPhone() {
        show()
        assertPanelCoversNothing(320f, 640f)
    }
}
