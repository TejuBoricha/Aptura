@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Drives the REAL Media3 AudioProcessor contract (configure -> flush -> queueInput -> getOutput) with PCM that carries
 * recognisable sample values, and checks where those samples end up in time. This is the actual behaviour the
 * audio-sync control promises (audit P0-08: it used to do nothing).
 */
class AudioDelayProcessorTest {

    private val sampleRate = 48_000
    private val format = AudioFormat(sampleRate, 2, C.ENCODING_PCM_16BIT)

    /** 1 frame = 4 bytes (stereo 16-bit). Each frame's LEFT sample is its frame index + 1 so it can be tracked. */
    private fun numberedPcm(startFrame: Int, frames: Int): ByteBuffer {
        val b = ByteBuffer.allocate(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            b.putShort((startFrame + i + 1).toShort())
            b.putShort(0)
        }
        b.flip()
        return b
    }

    private fun ByteBuffer.leftSamples(): List<Int> {
        val dup = duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val out = mutableListOf<Int>()
        while (dup.remaining() >= 4) {
            out += dup.short.toInt()
            dup.short
        }
        return out
    }

    private fun processor(delayMs: Long): AudioDelayProcessor = AudioDelayProcessor().apply {
        configure(format)
        flush()
        this.delayMs = delayMs
    }

    private fun AudioDelayProcessor.feed(startFrame: Int, frames: Int): List<Int> {
        queueInput(numberedPcm(startFrame, frames))
        return output.leftSamples()
    }

    private val AudioDelayProcessor.output: ByteBuffer get() = getOutput()

    @Test
    fun zeroDelay_passesAudioThroughUntouched() {
        val p = processor(0)
        assertEquals((1..100).toList(), p.feed(0, 100))
    }

    @Test
    fun positiveDelay_insertsSilenceBeforeTheAudio_soSoundArrivesLater() {
        val p = processor(100) // +100 ms = 4800 frames of silence
        val out = p.feed(0, 480)

        assertEquals(4800 + 480, out.size)
        assertTrue("first 100 ms must be silence", out.take(4800).all { it == 0 })
        assertEquals((1..480).toList(), out.drop(4800))
    }

    @Test
    fun negativeDelay_dropsTheStartOfTheAudio_soSoundArrivesEarlier() {
        val p = processor(-10) // -10 ms = 480 frames dropped
        val out = p.feed(0, 1000)

        assertEquals(1000 - 480, out.size)
        assertEquals(481, out.first()) // frame #481 (value 481) is now the first sample played
    }

    @Test
    fun negativeDelay_largerThanOneBuffer_continuesDroppingAcrossBuffers() {
        val p = processor(-10) // 480 frames to drop, buffers of 200 frames
        val first = p.feed(0, 200)
        val second = p.feed(200, 200)
        val third = p.feed(400, 200)

        assertTrue(first.isEmpty())
        assertTrue(second.isEmpty())
        assertEquals(120, third.size)           // only 80 of the third buffer's frames were still to drop
        assertEquals(481, third.first())
    }

    @Test
    fun delayChangedWhilePlaying_appliesOnlyTheDifference() {
        val p = processor(50) // 2400 silence
        val first = p.feed(0, 480)
        assertEquals(2400 + 480, first.size)

        p.delayMs = 150       // +100 ms more = 4800 additional silence frames
        val second = p.feed(480, 480)
        assertEquals(4800 + 480, second.size)
        assertTrue(second.take(4800).all { it == 0 })

        p.delayMs = 150       // unchanged: pure pass-through again
        val third = p.feed(960, 480)
        assertEquals(480, third.size)
    }

    @Test
    fun flush_afterSeek_reappliesTheFullOffsetFromTheNewPosition() {
        val p = processor(20)
        p.feed(0, 480)
        p.flush() // seek
        val afterSeek = p.feed(48_000, 480)

        assertEquals(960 + 480, afterSeek.size) // 20 ms = 960 frames of silence again
        assertTrue(afterSeek.take(960).all { it == 0 })
    }

    @Test
    fun nonPcm16Input_leavesTheProcessorInactive_soPlaybackIsNeverBroken() {
        val p = AudioDelayProcessor()
        p.configure(AudioFormat(sampleRate, 2, C.ENCODING_PCM_FLOAT))
        assertFalse(p.isActive)
        val event = org.aptura.player.core.diagnostics.ApturaDiagnostics.ringBuffer.getAllEvents()
            .find { it.code == org.aptura.player.core.diagnostics.EventCode.EV_014 }
        assertNotNull(event)
        assertEquals("bypass_audio_delay", event?.payload?.get("action"))
    }

    @Test
    fun delayIsClampedToASaneRange() {
        val p = AudioDelayProcessor()
        p.delayMs = 10_000_000
        assertEquals(AudioDelayProcessor.MAX_DELAY_MS, p.delayMs)
        p.delayMs = -10_000_000
        assertEquals(-AudioDelayProcessor.MAX_DELAY_MS, p.delayMs)
    }
}
