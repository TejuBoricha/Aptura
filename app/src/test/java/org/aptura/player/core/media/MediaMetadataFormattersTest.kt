package org.aptura.player.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Audit QA-04: `MediaMetadataInspector` had no tests. These are its pure formatters, which build the codec, bitrate,
 * channel and sample-rate text in the details sheet. They pin what the person reads today, so a change to one of them is
 * a visible, reviewed change and not an accident.
 */
class MediaMetadataFormattersTest {

    // --- Video codec names ---

    @Test
    fun knownVideoMimeTypes_haveReadableNames() {
        val expected = mapOf(
            "video/avc" to "H.264 / AVC",
            "video/hevc" to "H.265 / HEVC",
            "video/dolby-vision" to "Dolby Vision",
            "video/x-vnd.on2.vp9" to "Google VP9",
            "video/x-vnd.on2.vp8" to "Google VP8",
            "video/av01" to "AOMedia AV1",
            "video/mpeg2" to "MPEG-2",
            "video/wvc1" to "VC-1 / WMV",
            "video/3gpp" to "H.263"
        )
        expected.forEach { (mime, name) -> assertEquals(mime, name, MediaMetadataInspector.formatMimeToCodecName(mime)) }
    }

    @Test
    fun aVideoMimeTypeIsMatchedWhateverItsCase_andAnUnknownOneIsShownAsItsSubtype() {
        assertEquals("H.265 / HEVC", MediaMetadataInspector.formatMimeToCodecName("VIDEO/HEVC"))
        assertEquals("X-FOO", MediaMetadataInspector.formatMimeToCodecName("video/x-foo"))
    }

    // --- Audio codec names ---

    @Test
    fun knownAudioMimeTypes_haveReadableNames() {
        val expected = mapOf(
            "audio/mp4a-latm" to "AAC",
            "audio/ac3" to "Dolby Digital (AC-3)",
            "audio/eac3" to "Dolby Digital Plus (E-AC-3)",
            "audio/eac3-joc" to "Dolby Digital Plus (E-AC-3)",
            "audio/vnd.dts" to "DTS",
            "audio/vnd.dts.hd" to "DTS-HD",
            "audio/true-hd" to "Dolby TrueHD",
            "audio/opus" to "Opus",
            "audio/flac" to "FLAC (Lossless)",
            "audio/mpeg" to "MP3",
            "audio/vorbis" to "Vorbis",
            "audio/raw" to "PCM"
        )
        expected.forEach { (mime, name) -> assertEquals(mime, name, MediaMetadataInspector.formatMimeToAudioCodecName(mime)) }
    }

    @Test
    fun anUnknownAudioMimeType_isShownAsItsSubtype() {
        assertEquals("X-UNKNOWN", MediaMetadataInspector.formatMimeToAudioCodecName("audio/x-unknown"))
    }

    // --- Subtitle format names ---

    @Test
    fun knownSubtitleMimeTypes_haveReadableNames() {
        val expected = mapOf(
            "application/x-subrip" to "SubRip (SRT)",
            "text/vtt" to "WebVTT",
            "text/x-ssa" to "SubStation Alpha (ASS)",
            "application/ttml+xml" to "Timed Text (TTML)",
            "application/pgs" to "Blu-ray PGS",
            "application/vobsub" to "DVD VobSub",
            "application/dvbsubs" to "DVB Subtitles"
        )
        expected.forEach { (mime, name) -> assertEquals(mime, name, MediaMetadataInspector.formatMimeToSubtitleName(mime)) }
    }

    @Test
    fun anUnknownSubtitleMimeType_isShownAsItsSubtype() {
        assertEquals("FOO", MediaMetadataInspector.formatMimeToSubtitleName("text/foo"))
    }

    // --- Bitrate ---

    @Test
    fun aBitrateOfZeroOrLess_isUnknown() {
        assertEquals("Unknown", MediaMetadataInspector.formatBitrate(0L))
        assertEquals("Unknown", MediaMetadataInspector.formatBitrate(-5L))
    }

    @Test
    fun aBitrate_usesTheLargestUnitThatFits() {
        assertEquals("999 bps", MediaMetadataInspector.formatBitrate(999L))
        assertEquals("1 kbps", MediaMetadataInspector.formatBitrate(1_000L))
        assertEquals("128 kbps", MediaMetadataInspector.formatBitrate(128_000L))
        assertEquals("1.0 Mbps", MediaMetadataInspector.formatBitrate(1_000_000L))
        assertEquals("8.5 Mbps", MediaMetadataInspector.formatBitrate(8_500_000L))
    }

    // --- Channels ---

    @Test
    fun commonChannelCounts_haveTheirUsualNames() {
        assertEquals("Mono (1.0 ch)", MediaMetadataInspector.formatChannels(1))
        assertEquals("Stereo (2.0 ch)", MediaMetadataInspector.formatChannels(2))
        assertEquals("5.1 Surround (6 ch)", MediaMetadataInspector.formatChannels(6))
        assertEquals("7.1 Surround (8 ch)", MediaMetadataInspector.formatChannels(8))
    }

    @Test
    fun anUncommonChannelCount_isShownAsANumber() {
        assertEquals("3 channels", MediaMetadataInspector.formatChannels(3))
        assertEquals("12 channels", MediaMetadataInspector.formatChannels(12))
    }

    // --- Sample rate ---

    @Test
    fun aSampleRate_isShownInKilohertzWithOneDecimal() {
        assertEquals("44.1 kHz", MediaMetadataInspector.formatSampleRate(44_100))
        assertEquals("48.0 kHz", MediaMetadataInspector.formatSampleRate(48_000))
        assertEquals("96.0 kHz", MediaMetadataInspector.formatSampleRate(96_000))
    }

    @Test
    fun aSampleRateOfZeroOrLess_isUnknown() {
        assertEquals("Unknown", MediaMetadataInspector.formatSampleRate(0))
        assertEquals("Unknown", MediaMetadataInspector.formatSampleRate(-1))
    }
}
