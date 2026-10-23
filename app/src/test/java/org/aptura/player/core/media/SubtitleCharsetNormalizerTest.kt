package org.aptura.player.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class SubtitleCharsetNormalizerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun normalizeToUtf8_cleanUtf8File_returnsOriginalFile() {
        val cacheDir = tempFolder.newFolder("cache")
        val file = tempFolder.newFile("sample.srt")
        file.writeText("1\n00:00:01,000 --> 00:00:03,000\nClean text.", StandardCharsets.UTF_8)

        val result = SubtitleCharsetNormalizer.normalizeToUtf8(file, cacheDir)

        assertEquals(file.absolutePath, result.absolutePath)
    }

    @Test
    fun normalizeToUtf8_utf8WithBom_stripsBomSuccessfully() {
        val cacheDir = tempFolder.newFolder("cache")
        val file = tempFolder.newFile("bom.srt")
        val content = "1\n00:00:01,000 --> 00:00:03,000\nHello"
        val bomBytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + content.toByteArray(StandardCharsets.UTF_8)
        file.writeBytes(bomBytes)

        val result = SubtitleCharsetNormalizer.normalizeToUtf8(file, cacheDir)

        assertTrue(result.exists())
        assertEquals(content, result.readText(StandardCharsets.UTF_8))
        // Verify output file does not start with BOM
        val resultBytes = result.readBytes()
        assertTrue(resultBytes[0] != 0xEF.toByte())
    }

    @Test
    fun normalizeToUtf8_windows1252File_transcodesToCleanUtf8() {
        val cacheDir = tempFolder.newFolder("cache")
        val file = tempFolder.newFile("spanish_win1252.srt")
        val spanishText = "1\n00:00:01,000 --> 00:00:03,000\n¿Dónde está el baño?"
        file.writeBytes(spanishText.toByteArray(Charset.forName("windows-1252")))

        val result = SubtitleCharsetNormalizer.normalizeToUtf8(file, cacheDir)

        assertTrue(result.exists())
        assertEquals(spanishText, result.readText(StandardCharsets.UTF_8))
    }

    @Test
    fun normalizeToUtf8_utf16LeWithBom_transcodesToCleanUtf8WithoutLeadingBomChar() {
        val cacheDir = tempFolder.newFolder("cache")
        val file = tempFolder.newFile("utf16le.srt")
        val content = "1\n00:00:01,000 --> 00:00:03,000\nUTF-16 subtitle line"
        val bomBytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + content.toByteArray(StandardCharsets.UTF_16LE)
        file.writeBytes(bomBytes)

        val result = SubtitleCharsetNormalizer.normalizeToUtf8(file, cacheDir)

        assertTrue(result.exists())
        val normalizedText = result.readText(StandardCharsets.UTF_8)
        assertEquals(content, normalizedText)
        assertTrue(!normalizedText.startsWith("\uFEFF"))
    }
}
