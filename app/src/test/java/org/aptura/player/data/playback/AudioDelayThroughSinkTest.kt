@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.audio.AudioSink
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAudioTrack
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Audit FUNC-04. [AudioDelayProcessorTest] drives the processor alone, so it cannot see what Media3's real
 * `DefaultAudioSink` does around it. These tests drive that real sink, built by the same [buildAudioSink] the player
 * uses, and read the bytes it hands to the `AudioTrack`.
 *
 * The signal is a constant non-zero level, so every all-zero frame in the output is silence the delay inserted. That
 * makes the offset countable even after Media3's speed change (Sonic) has stretched the audio.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioDelayThroughSinkTest {

    private val sampleRate = 48_000
    private val level: Short = 1_000

    private val written = ByteArrayOutputStream()
    private val listener = ShadowAudioTrack.OnAudioDataWrittenListener { _, data, _ -> written.write(data) }

    private lateinit var processor: AudioDelayProcessor
    private lateinit var sink: AudioSink
    private var timeUs = 0L

    @Before
    fun setUp() {
        ShadowAudioTrack.addAudioDataListener(listener)
        startSink()
    }

    @After
    fun tearDown() {
        sink.reset()
        ShadowAudioTrack.removeAudioDataListener(listener)
    }

    /** A new sink and processor, as when playback starts; what an earlier run wrote is forgotten. */
    private fun startSink() {
        written.reset()
        timeUs = 0L
        val context: Context = ApplicationProvider.getApplicationContext()
        processor = AudioDelayProcessor()
        sink = buildAudioSink(context, processor, enableFloatOutput = false, enableAudioTrackPlaybackParams = false)
        sink.configure(
            Format.Builder()
                .setSampleMimeType(MimeTypes.AUDIO_RAW)
                .setPcmEncoding(C.ENCODING_PCM_16BIT)
                .setChannelCount(2)
                .setSampleRate(sampleRate)
                .build(),
            /* specifiedBufferSize = */ 0,
            /* outputChannels = */ null
        )
        sink.play()
    }

    /** Plays [seconds] of the constant signal through the sink, the way the renderer feeds it. */
    private fun play(seconds: Double) {
        var remaining = (seconds * sampleRate).toInt()
        while (remaining > 0) {
            val frames = minOf(remaining, sampleRate / 10) // 100 ms buffers
            val buffer = ByteBuffer.allocate(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
            repeat(frames) { buffer.putShort(level).putShort(level) }
            buffer.flip()
            var spins = 0
            while (!sink.handleBuffer(buffer, timeUs, 1)) {
                assertTrue("the sink stopped accepting audio", ++spins < 10_000)
            }
            timeUs += frames * 1_000_000L / sampleRate
            remaining -= frames
        }
    }

    /**
     * Drains whatever the processors still hold into the AudioTrack. It does not wait for `isEnded`: that needs the
     * track's playback head to catch up with what was written, and the Robolectric track never plays anything.
     */
    private fun finish() {
        repeat(20) { sink.playToEndOfStream() }
    }

    private fun speed(value: Float) = sink.setPlaybackParameters(PlaybackParameters(value))

    /** A seek: the player flushes the sink and the next audio comes from a new position. */
    private fun seek(toSeconds: Long) {
        sink.flush()
        timeUs = toSeconds * 1_000_000L
    }

    /** Frames the sink has handed to the AudioTrack since [fromByte], as (left sample) values. */
    private fun frames(fromByte: Int = 0): List<Short> {
        val bytes = written.toByteArray()
        val buf = ByteBuffer.wrap(bytes, fromByte, bytes.size - fromByte).order(ByteOrder.LITTLE_ENDIAN)
        val out = ArrayList<Short>((bytes.size - fromByte) / 4)
        while (buf.remaining() >= 4) {
            out += buf.short
            buf.short
        }
        return out
    }

    private fun silenceFrames(fromByte: Int = 0) = frames(fromByte).count { it == 0.toShort() }
    private fun leadingSilence(fromByte: Int = 0) = frames(fromByte).takeWhile { it == 0.toShort() }.size

    private fun ms(frames: Int) = frames * 1000.0 / sampleRate

    // Sonic cross-fades where silence meets sound, so a count can be off by a pitch period (about 20 ms).
    private val slackFrames = 1_500

    private fun assertAboutMs(message: String, expectedMs: Long, actualFrames: Int) {
        val expectedFrames = (expectedMs * sampleRate / 1000).toInt()
        assertTrue(
            "$message: expected about $expectedMs ms (${expectedFrames} frames), got ${"%.0f".format(ms(actualFrames))} ms ($actualFrames frames)",
            kotlin.math.abs(actualFrames - expectedFrames) <= slackFrames
        )
    }

    /**
     * Harness check: with no delay and no speed change, every frame fed in reaches the AudioTrack and none is added.
     * Media3's sink ramps the volume up from zero over the first ~20 ms after a start, to avoid a pop, so the first
     * frame reads as silence and the next few are quiet. Past that, the signal is exactly what went in.
     */
    @Test
    fun withNoDelay_everyFrameReachesTheTrack_unchanged() {
        play(1.0)
        finish()

        val all = frames()
        assertEquals(sampleRate, all.size)
        assertTrue("silence beyond the sink's own ramp-up: ${leadingSilence()} frames", leadingSilence() <= 1)
        assertTrue("the signal changed after the ramp-up", all.drop(2_000).all { it == level })
    }

    @Test
    fun withNoSpeedChange_theDelayIsApplied_once() {
        processor.delayMs = 300
        play(2.0)
        finish()

        assertAboutMs("delay at normal speed", 300, silenceFrames())
    }

    @Test
    fun speedChanges_doNotApplyTheDelayAgain() {
        processor.delayMs = 300
        play(1.0)
        speed(2f)   // one flush of the pipeline, no seek
        play(1.0)
        speed(1f)   // and another
        play(1.0)
        finish()

        assertAboutMs("delay after two speed changes", 300, silenceFrames())
    }

    @Test
    fun theDelay_isRealTime_atDoubleSpeed() {
        speed(2f)
        processor.delayMs = 300
        play(2.0)
        finish()

        // The person set 300 ms and hears 300 ms, not 150 ms, whatever the speed is.
        assertAboutMs("delay at 2x", 300, leadingSilence())
    }

    /** The whole stretch of playback used by the negative-delay test; returns the frames the AudioTrack received. */
    private fun framesForTwoSpeedChanges(delayMs: Long): Int {
        sink.reset()
        startSink()
        processor.delayMs = delayMs
        play(1.0); speed(2f); play(1.0); speed(1f); play(1.0)
        finish()
        return written.size() / 4
    }

    @Test
    fun aNegativeDelay_dropsAudioOnce_acrossSpeedChanges() {
        // Reference: the same playback with no delay, to read off how much audio a -100 ms offset removed.
        val reference = framesForTwoSpeedChanges(0)
        val withDelay = framesForTwoSpeedChanges(-100)

        assertAboutMs("audio dropped by -100 ms", 100, reference - withDelay)
    }

    // --- Guards: a seek must still start the offset again from the new position ---

    @Test
    fun aSeek_appliesTheFullDelayAgain() {
        processor.delayMs = 300
        play(1.0)
        val seekAt = written.size()
        seek(60)
        play(1.0)
        finish()

        assertAboutMs("delay after a seek", 300, leadingSilence(seekAt))
    }

    @Test
    fun aSeek_afterSpeedChanges_appliesTheFullDelayAgain() {
        processor.delayMs = 300
        play(1.0)
        speed(2f)
        play(0.5)
        speed(1f)
        play(0.5)
        val seekAt = written.size()
        seek(60)
        play(1.0)
        finish()

        assertAboutMs("delay after speed changes and a seek", 300, leadingSilence(seekAt))
    }
}
