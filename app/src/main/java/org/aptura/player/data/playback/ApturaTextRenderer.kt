@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.os.Looper
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.text.SubtitleDecoderFactory
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer

/**
 * Custom TextRenderer interceptor that supports real-time subtitle synchronization delay (SCR-010e, FR-SUB-005).
 * Shifting positionUs dynamically allows instantaneous subtitle advancement or delay without
 * restarting playback or reloading external media streams.
 *
 * Math:
 * - Positive subtitleDelayMs (e.g. +1000ms): Subtitles appear later than original timestamps.
 *   When video is at positionUs, we query the text renderer at (positionUs - 1000ms).
 * - Negative subtitleDelayMs (e.g. -1000ms): Subtitles appear earlier than original timestamps.
 *   When video is at positionUs, we query the text renderer at (positionUs + 1000ms).
 */
class ApturaTextRenderer(
    output: TextOutput,
    outputLooper: Looper?,
    decoderFactory: SubtitleDecoderFactory = SubtitleDecoderFactory.DEFAULT,
    private val delegate: TextRenderer = TextRenderer(output, outputLooper, decoderFactory)
) : Renderer by delegate {

    @Volatile
    var subtitleDelayMs: Long = 0L

    fun computeAdjustedPositionUs(positionUs: Long): Long {
        val delayUs = subtitleDelayMs * 1000L
        return positionUs - delayUs
    }

    override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
        delegate.render(computeAdjustedPositionUs(positionUs), elapsedRealtimeUs)
    }

    override fun release() {
        delegate.release()
    }
}
