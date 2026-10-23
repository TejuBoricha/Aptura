package org.aptura.player.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleFileValidatorTest {

    @Test
    fun isBinaryOrImage_detectsPngMagicBytes() {
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)
        assertTrue(SubtitleFileValidator.isBinaryOrImage(pngHeader, pngHeader.size))
    }

    @Test
    fun isBinaryOrImage_detectsJpegMagicBytes() {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10)
        assertTrue(SubtitleFileValidator.isBinaryOrImage(jpegHeader, jpegHeader.size))
    }

    @Test
    fun isBinaryOrImage_detectsGifMagicBytes() {
        val gifHeader = "GIF89a".toByteArray(Charsets.US_ASCII)
        assertTrue(SubtitleFileValidator.isBinaryOrImage(gifHeader, gifHeader.size))
    }

    @Test
    fun isBinaryOrImage_detectsPdfMagicBytes() {
        val pdfHeader = "%PDF-1.5".toByteArray(Charsets.US_ASCII)
        assertTrue(SubtitleFileValidator.isBinaryOrImage(pdfHeader, pdfHeader.size))
    }

    @Test
    fun isBinaryOrImage_detectsZipMagicBytes() {
        val zipHeader = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 0x03, 0x04)
        assertTrue(SubtitleFileValidator.isBinaryOrImage(zipHeader, zipHeader.size))
    }

    @Test
    fun isBinaryOrImage_detectsNullBytesInBinaryData() {
        val binaryDataWithNull = byteArrayOf(0x31, 0x0A, 0x00, 0x30, 0x30)
        assertTrue(SubtitleFileValidator.isBinaryOrImage(binaryDataWithNull, binaryDataWithNull.size))
    }

    @Test
    fun isBinaryOrImage_allowsValidSrtText() {
        val srtContent = """
            1
            00:00:01,000 --> 00:00:04,000
            Hello, this is a subtitle line.
        """.trimIndent().toByteArray(Charsets.UTF_8)
        assertFalse(SubtitleFileValidator.isBinaryOrImage(srtContent, srtContent.size))
    }

    @Test
    fun isBinaryOrImage_allowsValidWebVttText() {
        val vttContent = """
            WEBVTT

            00:01.000 --> 00:04.000
            Welcome to the video.
        """.trimIndent().toByteArray(Charsets.UTF_8)
        assertFalse(SubtitleFileValidator.isBinaryOrImage(vttContent, vttContent.size))
    }

    @Test
    fun isBinaryOrImage_allowsValidAssText() {
        val assContent = """
            [Script Info]
            Title: Sample Subtitle
            ScriptType: v4.00+
        """.trimIndent().toByteArray(Charsets.UTF_8)
        assertFalse(SubtitleFileValidator.isBinaryOrImage(assContent, assContent.size))
    }

    @Test
    fun isBinaryOrImage_allowsUtf16LeAndUtf16BeBomSubtitlesDespiteZeroBytes() {
        val text = "1\n00:00:01,000 --> 00:00:04,000\nUTF-16 Subtitle"
        val utf16Le = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)
        val utf16Be = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + text.toByteArray(Charsets.UTF_16BE)

        assertFalse(SubtitleFileValidator.isBinaryOrImage(utf16Le, utf16Le.size))
        assertFalse(SubtitleFileValidator.isBinaryOrImage(utf16Be, utf16Be.size))
    }
}
