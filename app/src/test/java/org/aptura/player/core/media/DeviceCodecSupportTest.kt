package org.aptura.player.core.media

import org.junit.Assert.assertEquals
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
}
