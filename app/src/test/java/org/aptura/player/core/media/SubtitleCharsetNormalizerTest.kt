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

    @Test
    fun twoImportsWithSameName_doNotOverwriteEachOther() {
        val cacheDir = tempFolder.newFolder("cache_collision")
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

        // File 1 with name "English.srt" and Content 1
        val file1 = tempFolder.newFile("file1_English.srt")
        val content1 = "1\n00:00:01,000 --> 00:00:02,000\nSubtitle One"
        file1.writeBytes(bom + content1.toByteArray(StandardCharsets.UTF_8))

        // File 2 with same file name when normalized ("English.srt") and Content 2
        val subFolder = tempFolder.newFolder("other_folder")
        val file2 = File(subFolder, "file1_English.srt")
        val content2 = "1\n00:00:01,000 --> 00:00:02,000\nSubtitle Two"
        file2.writeBytes(bom + content2.toByteArray(StandardCharsets.UTF_8))

        val result1 = SubtitleCharsetNormalizer.normalizeToUtf8(file1, cacheDir)
        val result2 = SubtitleCharsetNormalizer.normalizeToUtf8(file2, cacheDir)

        assertTrue("First result exists", result1.exists())
        assertTrue("Second result exists", result2.exists())
        assertTrue("Files have distinct paths in per-import dirs", result1.canonicalPath != result2.canonicalPath)
        assertEquals(content1, result1.readText(StandardCharsets.UTF_8))
        assertEquals(content2, result2.readText(StandardCharsets.UTF_8))
    }

    @Test
    fun twoByteImportsWithSameDisplayName_doNotOverwriteEachOther() {
        val cacheDir = tempFolder.newFolder("cache_byte_collision")
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

        val contentA = "1\n00:00:01,000 --> 00:00:02,000\nContent A"
        val contentB = "1\n00:00:01,000 --> 00:00:02,000\nContent B"

        val bytesA = bom + contentA.toByteArray(StandardCharsets.UTF_8)
        val bytesB = bom + contentB.toByteArray(StandardCharsets.UTF_8)

        val fileA = SubtitleCharsetNormalizer.normalizeBytesToUtf8File(bytesA, "English.srt", cacheDir)
        val fileB = SubtitleCharsetNormalizer.normalizeBytesToUtf8File(bytesB, "English.srt", cacheDir)

        org.junit.Assert.assertNotNull(fileA)
        org.junit.Assert.assertNotNull(fileB)
        assertTrue(fileA!!.exists())
        assertTrue(fileB!!.exists())
        assertTrue("Separate import folders", fileA.canonicalPath != fileB.canonicalPath)
        assertEquals(contentA, fileA.readText(StandardCharsets.UTF_8))
        assertEquals(contentB, fileB.readText(StandardCharsets.UTF_8))
    }

    @Test
    fun pruneStaleImports_removesDirectoriesOlderThanCutoff() {
        val cacheDir = tempFolder.newFolder("cache_prune")
        val root = File(cacheDir, SubtitleCharsetNormalizer.NORMALIZED_DIR_NAME).apply { mkdirs() }

        val staleDir = File(root, "norm_stale").apply { mkdirs() }
        File(staleDir, "old.srt").writeText("old")
        staleDir.setLastModified(1_000_000L)

        val freshDir = File(root, "norm_fresh").apply { mkdirs() }
        File(freshDir, "fresh.srt").writeText("fresh")
        val now = 1_000_000L + SubtitleCharsetNormalizer.STALE_IMPORT_MS + 5_000L
        freshDir.setLastModified(now)

        SubtitleCharsetNormalizer.pruneStaleImports(root, nowMs = now)

        org.junit.Assert.assertFalse("Stale dir pruned", staleDir.exists())
        assertTrue("Fresh dir preserved", freshDir.exists())
    }

    private fun findFixture(relativePath: String): File {
        val direct = File(relativePath)
        if (direct.exists()) return direct
        val parent = File("..", relativePath)
        if (parent.exists()) return parent
        error("Fixture not found at $relativePath or ../$relativePath (user.dir=${System.getProperty("user.dir")})")
    }

    @Test
    fun normalizeToUtf8_cjkGb18030Fixture_transcodesToCleanUtf8() {
        val cacheDir = tempFolder.newFolder("cache_gb18030")
        val fixture = findFixture("fixtures/adv_sub/encodings/adv_sub_enc_gb18030.srt")

        val result = SubtitleCharsetNormalizer.normalizeToUtf8(fixture, cacheDir)
        assertTrue(result.exists())
        val text = result.readText(StandardCharsets.UTF_8)
        assertTrue("Must contain decoded Chinese text (DATA-11)", text.contains("GB18030 中文字幕测试"))
    }

    @Test
    fun normalizeToUtf8_cjkShiftJisFixture_transcodesToCleanUtf8() {
        val cacheDir = tempFolder.newFolder("cache_sjis")
        val fixture = findFixture("fixtures/adv_sub/encodings/adv_sub_enc_shift_jis.srt")

        val result = SubtitleCharsetNormalizer.normalizeToUtf8(fixture, cacheDir)
        assertTrue(result.exists())
        val text = result.readText(StandardCharsets.UTF_8)
        assertTrue("Must contain decoded Japanese text (DATA-11)", text.contains("Shift-JIS 日本語字幕テスト"))
    }

    @Test
    fun normalizeToUtf8_cyrillicKoi8RFixture_transcodesToCleanUtf8() {
        val cacheDir = tempFolder.newFolder("cache_koi8r")
        val fixture = findFixture("fixtures/adv_sub/encodings/adv_sub_enc_koi8_r.srt")

        val result = SubtitleCharsetNormalizer.normalizeToUtf8(fixture, cacheDir)
        assertTrue(result.exists())
        val text = result.readText(StandardCharsets.UTF_8)
        assertTrue("Must contain decoded Russian Cyrillic text (DATA-11)", text.contains("KOI8-R Русский субтитр"))
    }

    @Test
    fun normalizeToUtf8_utf16WithoutBomFixtures_transcodeToCleanUtf8() {
        val cacheDir = tempFolder.newFolder("cache_utf16_nobom")
        val beFixture = findFixture("fixtures/adv_sub/encodings/adv_sub_enc_utf16_be.srt")
        val leFixture = findFixture("fixtures/adv_sub/encodings/adv_sub_enc_utf16_le.srt")

        val beResult = SubtitleCharsetNormalizer.normalizeToUtf8(beFixture, cacheDir)
        val leResult = SubtitleCharsetNormalizer.normalizeToUtf8(leFixture, cacheDir)

        assertTrue(beResult.readText(StandardCharsets.UTF_8).contains("UTF-16 Big Endian"))
        assertTrue(leResult.readText(StandardCharsets.UTF_8).contains("UTF-16 Little Endian"))
    }

    @Test
    fun normalizeToUtf8_utf32WithoutBomFixtures_transcodeToCleanUtf8() {
        val cacheDir = tempFolder.newFolder("cache_utf32_nobom")
        val beFixture = findFixture("fixtures/adv_sub/encodings/adv_sub_enc_utf32_be.srt")
        val leFixture = findFixture("fixtures/adv_sub/encodings/adv_sub_enc_utf32_le.srt")

        val beResult = SubtitleCharsetNormalizer.normalizeToUtf8(beFixture, cacheDir)
        val leResult = SubtitleCharsetNormalizer.normalizeToUtf8(leFixture, cacheDir)

        assertTrue(beResult.readText(StandardCharsets.UTF_8).contains("UTF-32 Big Endian"))
        assertTrue(leResult.readText(StandardCharsets.UTF_8).contains("UTF-32 Little Endian"))
    }

    @Test
    fun normalizeToUtf8_withExplicitCharsetOverride_usesSpecifiedCharset() {
        val cacheDir = tempFolder.newFolder("cache_override")
        val file = tempFolder.newFile("arabic.srt")
        val arabicText = "1\n00:00:01,000 --> 00:00:03,000\nمرحبا بك في مشغل الفيديو"
        val cp1256 = Charset.forName("windows-1256")
        file.writeBytes(arabicText.toByteArray(cp1256))

        val result = SubtitleCharsetNormalizer.normalizeToUtf8(file, cacheDir, overrideCharset = cp1256)

        assertTrue(result.exists())
        assertEquals(arabicText, result.readText(StandardCharsets.UTF_8))
    }
}
