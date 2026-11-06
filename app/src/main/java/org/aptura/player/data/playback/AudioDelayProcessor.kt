@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessorChain
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.nio.ByteBuffer

/**
 * Real audio/video sync offset (audit P0-08: the "lip-sync" stepper used to be stored and displayed but never
 * applied to the audio).
 *
 *  - **Positive delay** (+150 ms, "Bluetooth buds"): inserts silence, so the sound arrives *later* than the picture.
 *  - **Negative delay**: drops samples, so the sound arrives *earlier*.
 *
 * Because the player's media clock follows the audio sink, shifting the audio stream shifts it relative to the
 * video, which is exactly the offset the user asked for. The delay can change while playing; a flush (seek) restarts
 * the accounting so the offset is re-applied from the new position.
 *
 * Media3 also flushes the processors when the playback speed changes, with no seek: the stream goes on. The sink
 * announces that through [DelayAfterSpeedChain], which calls [keepOffsetAcrossNextFlush] first, so that flush keeps
 * the offset instead of applying it a second time (audit FUNC-04).
 *
 * Only 16-bit PCM is processed; any other format makes the processor inactive (it never breaks playback).
 */
class AudioDelayProcessor : BaseAudioProcessor() {

    @Volatile
    private var requestedDelayMs: Long = 0L

    private var sampleRate = 0
    private var frameSize = 0

    /** Net frames inserted (+) or dropped (-) since the last flush. */
    private var appliedFrames = 0L

    /** True between [keepOffsetAcrossNextFlush] and the flush it announces. */
    @Volatile
    private var keepOffsetOnNextFlush = false

    var delayMs: Long
        get() = requestedDelayMs
        set(value) {
            requestedDelayMs = value.coerceIn(-MAX_DELAY_MS, MAX_DELAY_MS)
        }

    /**
     * Says that the next [flush] is not a seek. The audio goes on from where it was, so what has been inserted or
     * dropped already stays applied. It covers one flush only; the one after it starts again.
     */
    fun keepOffsetAcrossNextFlush() {
        keepOffsetOnNextFlush = true
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        keepOffsetOnNextFlush = false
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.sampleRate <= 0) {
            if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
                org.aptura.player.core.diagnostics.ApturaDiagnostics.logEvent(
                    code = org.aptura.player.core.diagnostics.EventCode.EV_014,
                    payload = mapOf(
                        "action" to "bypass_audio_delay",
                        "encoding" to inputAudioFormat.encoding.toString(),
                        "reason" to "unsupported_pcm_format"
                    )
                )
            }
            return AudioFormat.NOT_SET // inactive: pass the stream through untouched
        }
        sampleRate = inputAudioFormat.sampleRate
        frameSize = inputAudioFormat.bytesPerFrame
        appliedFrames = 0L
        return inputAudioFormat
    }

    override fun onFlush() {
        if (keepOffsetOnNextFlush) {
            keepOffsetOnNextFlush = false
            return
        }
        appliedFrames = 0L
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputBytes = inputBuffer.remaining()
        if (inputBytes == 0) return

        val targetFrames = requestedDelayMs * sampleRate / 1000L
        val deltaFrames = targetFrames - appliedFrames

        var silenceBytes = 0
        var dropBytes = 0
        if (deltaFrames > 0) {
            silenceBytes = (deltaFrames * frameSize).coerceAtMost(MAX_SILENCE_BYTES_PER_CALL.toLong()).toInt()
            silenceBytes -= silenceBytes % frameSize
        } else if (deltaFrames < 0) {
            val wanted = (-deltaFrames * frameSize).coerceAtMost(inputBytes.toLong()).toInt()
            dropBytes = wanted - wanted % frameSize
        }
        appliedFrames += (silenceBytes - dropBytes) / frameSize

        val keepBytes = inputBytes - dropBytes
        val output = replaceOutputBuffer(silenceBytes + keepBytes)
        if (silenceBytes > 0) output.put(ByteArray(silenceBytes))

        inputBuffer.position(inputBuffer.position() + dropBytes)
        val originalLimit = inputBuffer.limit()
        inputBuffer.limit(inputBuffer.position() + keepBytes)
        output.put(inputBuffer)
        inputBuffer.limit(originalLimit)
        output.flip()
    }

    companion object {
        const val MAX_DELAY_MS = 10_000L
        private const val MAX_SILENCE_BYTES_PER_CALL = 4 * 1024 * 1024
    }
}

/**
 * Media3's standard audio processors (silence skipping, then the speed change), followed by [delay].
 *
 * The delay goes last for two reasons (audit FUNC-04):
 *  - Sound the person hears is what comes out of the speed change. A delay placed before it is stretched with the
 *    audio, so 300 ms set became 150 ms at 2x.
 *  - The sink flushes its processors when the speed changes, with no seek. This chain says so to [delay] first, so
 *    that flush does not apply the delay a second time.
 *
 * Only for the sink's own speed change (Sonic). With `setEnableAudioTrackPlaybackParams(true)` the speed would be
 * applied by the AudioTrack after every processor, and the delay would be stretched again. The app does not use it.
 */
internal class DelayAfterSpeedChain(private val delay: AudioDelayProcessor) : AudioProcessorChain {

    private val standard = DefaultAudioSink.DefaultAudioProcessorChain()
    private val processors: Array<AudioProcessor> = standard.audioProcessors + delay

    override fun getAudioProcessors(): Array<AudioProcessor> = processors

    override fun applyPlaybackParameters(playbackParameters: PlaybackParameters): PlaybackParameters {
        // The sink flushes its processors straight after this call. It is a new speed, not a seek.
        delay.keepOffsetAcrossNextFlush()
        return standard.applyPlaybackParameters(playbackParameters)
    }

    override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean): Boolean =
        standard.applySkipSilenceEnabled(skipSilenceEnabled)

    override fun getMediaDuration(playoutDuration: Long): Long = standard.getMediaDuration(playoutDuration)

    override fun getSkippedOutputFrameCount(): Long = standard.skippedOutputFrameCount
}

/**
 * The audio sink the player uses: Media3's [DefaultAudioSink] with [processor] applied by [DelayAfterSpeedChain].
 * The one place the processor is wired in, so a test can drive the same sink the player gets.
 */
internal fun buildAudioSink(
    context: Context,
    processor: AudioDelayProcessor,
    enableFloatOutput: Boolean,
    enableAudioTrackPlaybackParams: Boolean
): AudioSink = DefaultAudioSink.Builder(context)
    .setEnableFloatOutput(enableFloatOutput)
    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
    .setAudioProcessorChain(DelayAfterSpeedChain(processor))
    .build()
