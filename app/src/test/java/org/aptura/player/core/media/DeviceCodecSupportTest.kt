package org.aptura.player.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The decoder report is user-facing support data; its labels and honesty notes must stay stable. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceCodecSupportTest {

    @Test
    fun mimeTypes_getHumanLabels() {
        assertEquals("H.264 / AVC", DeviceCodecSupport.labelFor("video/avc"))
        assertEquals("H.265 / HEVC", DeviceCodecSupport.labelFor("video/hevc"))
        assertEquals("VP9", DeviceCodecSupport.labelFor("video/x-vnd.on2.vp9"))
        assertEquals("AV1", DeviceCodecSupport.labelFor("video/av01"))
        assertEquals("DTS", DeviceCodecSupport.labelFor("audio/vnd.dts"))
        // Unknown MIME types stay readable instead of disappearing.
        assertEquals("Xyzzy", DeviceCodecSupport.labelFor("video/xyzzy"))
    }

    @Test
    fun report_isSafeOnDevicesWithoutMediaCodecData() {
        val report = DeviceCodecSupport.report()

        assertTrue(report.deviceLabel.contains("Android"))
        assertTrue(report.toPlainText().contains("Video decoders"))
        assertTrue(report.toPlainText().contains("Audio decoders"))
    }

    @Test
    fun report_statesTheNoBundledVideoDecoderPolicy() {
        val text = DeviceCodecSupport.report().toPlainText()

        assertTrue(text.contains("audio only"))
        assertTrue(text.contains("No software video decoders are bundled"))
        assertTrue(CodecSupportReport.FFMPEG_NOTE.contains("external player"))
    }

    /**
     * The bundle holds exactly the decoders in tools/ffmpeg/approved_decoders.txt (spec/11B D2, DEC-020). The note is what a
     * person reads about it, so it must name those and must not say the bundle decodes anything else.
     */
    @Test
    fun ffmpegNote_namesOnlyWhatTheBundleDecodes() {
        val note = CodecSupportReport.FFMPEG_NOTE
        assertTrue(note.contains("ALAC"))
        assertTrue(note.contains("A-law"))
        assertTrue(note.contains("mu-law"))
        // Formats that are left to the device's own decoders are named only as such, in the sentence that says so.
        val leftToTheDevice = note.substringAfter("Every other format")
        for (format in listOf("AAC", "AC-3", "E-AC-3", "DTS", "TrueHD", "FLAC", "MP3")) {
            assertFalse("$format must only appear as decoded by the device", note.substringBefore("Every other format").contains(format))
            assertTrue("$format should be named as the device's own", leftToTheDevice.contains(format))
        }
        assertTrue(note.contains("decoded by this device itself"))
    }

    @Test
    fun ffmpegStatus_saysWhetherTheExtensionLoaded() {
        val empty = CodecSupportReport("device", emptyList(), emptyList())
        assertEquals("FFmpeg audio extension: not loaded", empty.ffmpegStatusLine())
        assertTrue(empty.toPlainText().contains("FFmpeg audio extension: not loaded"))

        val loaded = CodecSupportReport("device", emptyList(), emptyList(), ffmpegVersion = "Lavc63.1.102")
        assertEquals("FFmpeg audio extension: loaded (libavcodec 63.1.102)", loaded.ffmpegStatusLine())
        assertTrue(loaded.toPlainText().contains("loaded (libavcodec 63.1.102)"))
    }
}
