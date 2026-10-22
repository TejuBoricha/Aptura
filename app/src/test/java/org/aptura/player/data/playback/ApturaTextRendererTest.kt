@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.text.TextOutput
import org.junit.Assert.assertEquals
import org.junit.Test

class ApturaTextRendererTest {

    private val noOpOutput = TextOutput { _: CueGroup -> }

    @Test
    fun defaultSubtitleDelay_isZero() {
        val renderer = ApturaTextRenderer(noOpOutput, null)
        assertEquals(0L, renderer.subtitleDelayMs)
        assertEquals(5_000_000L, renderer.computeAdjustedPositionUs(5_000_000L))
    }

    @Test
    fun positiveSubtitleDelay_shiftsPositionEarlierToDelayCues() {
        val renderer = ApturaTextRenderer(noOpOutput, null)
        // Subtitle delay +1000ms: subtitles appear 1000ms later in real time.
        // At video position 5000ms (5_000_000us), query cues for 4000ms (4_000_000us).
        renderer.subtitleDelayMs = 1000L
        assertEquals(4_000_000L, renderer.computeAdjustedPositionUs(5_000_000L))
    }

    @Test
    fun negativeSubtitleDelay_shiftsPositionLaterToAdvanceCues() {
        val renderer = ApturaTextRenderer(noOpOutput, null)
        // Subtitle delay -500ms: subtitles appear 500ms earlier in real time.
        // At video position 5000ms (5_000_000us), query cues for 5500ms (5_500_000us).
        renderer.subtitleDelayMs = -500L
        assertEquals(5_500_000L, renderer.computeAdjustedPositionUs(5_000_000L))
    }

    @Test
    fun arbitraryFineGrainedOffsets_accurateToMicrosecond() {
        val renderer = ApturaTextRenderer(noOpOutput, null)
        renderer.subtitleDelayMs = 250L
        assertEquals(1_750_000L, renderer.computeAdjustedPositionUs(2_000_000L))

        renderer.subtitleDelayMs = -50L
        assertEquals(2_050_000L, renderer.computeAdjustedPositionUs(2_000_000L))
    }
}
