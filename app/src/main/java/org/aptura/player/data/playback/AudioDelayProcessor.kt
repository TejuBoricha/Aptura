@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
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
 * Only 16-bit PCM is processed; any other format makes the processor inactive (it never breaks playback).
 */
class AudioDelayProcessor : BaseAudioProcessor() {

    @Volatile
    private var requestedDelayMs: Long = 0L

    private var sampleRate = 0
    private var frameSize = 0

    /** Net frames inserted (+) or dropped (-) since the last flush. */
    private var appliedFrames = 0L

    var delayMs: Long
        get() = requestedDelayMs
        set(value) {
            requestedDelayMs = value.coerceIn(-MAX_DELAY_MS, MAX_DELAY_MS)
        }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
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
