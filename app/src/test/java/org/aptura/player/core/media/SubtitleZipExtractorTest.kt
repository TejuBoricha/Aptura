package org.aptura.player.core.media

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubtitleZipExtractorTest {

    private lateinit var context: Context

    private val extractRoot get() = File(context.cacheDir, SubtitleZipExtractor.EXTRACT_DIR_NAME)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        extractRoot.deleteRecursively()
    }

    /** A zip of [entries] (name to size in bytes of zero-filled content): tiny to store, large to unpack. */
    private fun zeroFilledZip(entries: List<Pair<String, Long>>): ByteArrayInputStream {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val chunk = ByteArray(64 * 1024)
            for ((name, size) in entries) {
                zos.putNextEntry(ZipEntry(name))
                var written = 0L
                while (written < size) {
                    val n = minOf(chunk.size.toLong(), size - written).toInt()
                    zos.write(chunk, 0, n)
                    written += n
                }
                zos.closeEntry()
            }
        }
        return ByteArrayInputStream(baos.toByteArray())
    }

    private fun filesUnderRoot(): List<File> = extractRoot.walkTopDown().filter { it.isFile }.toList()

    private fun createZipStream(entries: Map<String, String>): ByteArrayInputStream {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for ((name, content) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
        }
        return ByteArrayInputStream(baos.toByteArray())
    }

    @Test
    fun extractSubtitleFromStream_singleSrtInZip_extractsSuccessfully() {
        val srtContent = """
            1
            00:00:01,000 --> 00:00:04,000
            Hello from zip subtitle
        """.trimIndent()

        val zipStream = createZipStream(mapOf("Dune.2024.srt" to srtContent))
        val result = SubtitleZipExtractor.extractSubtitleFromStream(context, zipStream)

        assertTrue(result is ZipExtractionResult.Success)
        val success = result as ZipExtractionResult.Success
        assertEquals("Dune.2024.srt", success.entryName)
        assertEquals("application/x-subrip", success.mimeType)
        assertTrue(success.extractedFile.exists())
        assertEquals(srtContent, success.extractedFile.readText())
    }

    @Test
    fun extractSubtitleFromStream_zipWithNoSubtitles_returnsFailure() {
        val zipStream = createZipStream(mapOf(
            "readme.txt" to "This is a readme file.",
            "cover.jpg" to "fake-image-bytes"
        ))
        val result = SubtitleZipExtractor.extractSubtitleFromStream(context, zipStream)

        assertTrue(result is ZipExtractionResult.Failure)
        val failure = result as ZipExtractionResult.Failure
        assertTrue(failure.reason.contains("no recognized subtitle files"))
    }

    @Test
    fun extractSubtitleFromStream_multipleSubtitles_prefersEnglish() {
        val zipStream = createZipStream(mapOf(
            "readme.txt" to "info",
            "movie.es.srt" to "Spanish subtitle",
            "movie.en.srt" to "English subtitle"
        ))
        val result = SubtitleZipExtractor.extractSubtitleFromStream(context, zipStream)

        assertTrue(result is ZipExtractionResult.Success)
        val success = result as ZipExtractionResult.Success
        assertEquals("movie.en.srt", success.entryName)
        assertEquals("English subtitle", success.extractedFile.readText())
    }

    @Test
    fun extractSubtitleFromStream_ttmlInZip_extractsWithTtmlMimeType() {
        val ttmlContent = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="00:00:01.000" end="00:00:03.000">Hello TTML</p></div></body></tt>"""
        val zipStream = createZipStream(mapOf("captions.ttml" to ttmlContent))
        val result = SubtitleZipExtractor.extractSubtitleFromStream(context, zipStream)

        assertTrue(result is ZipExtractionResult.Success)
        val success = result as ZipExtractionResult.Success
        assertEquals("captions.ttml", success.entryName)
        assertEquals("application/ttml+xml", success.mimeType)
    }

    @Test
    fun extractSubtitleFromStream_exceedsMaxBytes_abortsWithFailure() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("huge_bomb.srt"))
            val zeroChunk = ByteArray(64 * 1024)
            val totalNeeded = SubtitleZipExtractor.MAX_SUBTITLE_BYTES + 1024
            var written = 0L
            while (written < totalNeeded) {
                val toWrite = minOf(zeroChunk.size.toLong(), totalNeeded - written).toInt()
                zos.write(zeroChunk, 0, toWrite)
                written += toWrite
            }
            zos.closeEntry()
        }
        val zipStream = ByteArrayInputStream(baos.toByteArray())
        val result = SubtitleZipExtractor.extractSubtitleFromStream(context, zipStream)

        assertTrue(result is ZipExtractionResult.Failure)
        val failure = result as ZipExtractionResult.Failure
        assertTrue(failure.reason.contains("exceeds maximum allowed size"))
    }

    // ---- SEC-02: the archive as a whole is bounded, not only each subtitle -------------------------------------------

    @Test
    fun anArchiveWithTooManyEntries_isRefused() {
        val entries = (0..SubtitleZipExtractor.MAX_ENTRIES).associate { "note_$it.txt" to "x" }
        val result = SubtitleZipExtractor.extractSubtitleFromStream(context, createZipStream(entries))

        assertTrue("expected a refusal, got $result", result is ZipExtractionResult.Failure)
        assertTrue((result as ZipExtractionResult.Failure).reason, result.reason.contains("too many files"))
    }

    @Test
    fun anArchiveWithExactlyTheEntryLimit_isStillAccepted() {
        val entries = (1 until SubtitleZipExtractor.MAX_ENTRIES).associate { "note_$it.txt" to "x" } + ("movie.srt" to "1")
        val result = SubtitleZipExtractor.extractSubtitleFromStream(context, createZipStream(entries))

        assertTrue("a pack right at the limit must still import: $result", result is ZipExtractionResult.Success)
    }

    @Test
    fun bytesInEntriesThatAreSkipped_countTowardTheTotal() {
        // Skipped entries are still unpacked by ZipInputStream to reach the next one: CPU a zip bomb can spend for free.
        val big = SubtitleZipExtractor.MAX_SUBTITLE_BYTES * 2
        val result = SubtitleZipExtractor.extractSubtitleFromStream(
            context,
            zeroFilledZip(listOf("a.bin" to big, "b.bin" to big, "c.bin" to big, "movie.srt" to 10L))
        )

        assertTrue("expected a refusal, got $result", result is ZipExtractionResult.Failure)
        assertTrue((result as ZipExtractionResult.Failure).reason, result.reason.contains("unpacks to more than"))
    }

    @Test
    fun manySubtitleEntries_cannotAddUpPastTheTotal() {
        val each = SubtitleZipExtractor.MAX_SUBTITLE_BYTES - 1024
        val names = (1..5).map { "movie.lang$it.srt" to each }
        val result = SubtitleZipExtractor.extractSubtitleFromStream(context, zeroFilledZip(names))

        assertTrue("expected a refusal, got $result", result is ZipExtractionResult.Failure)
        assertTrue((result as ZipExtractionResult.Failure).reason, result.reason.contains("unpacks to more than"))
        assertEquals("a refused import leaves nothing on disk", emptyList<File>(), filesUnderRoot())
    }

    @Test
    fun anArchiveJustUnderTheTotal_isStillAccepted() {
        val result = SubtitleZipExtractor.extractSubtitleFromStream(
            context,
            zeroFilledZip(listOf("filler.bin" to SubtitleZipExtractor.MAX_TOTAL_BYTES - 4096, "movie.srt" to 1024L))
        )

        assertTrue("an archive under the cap must import: $result", result is ZipExtractionResult.Success)
    }

    // ---- SEC-02: what stays on disk -----------------------------------------------------------------------------------

    @Test
    fun onlyTheChosenSubtitle_isLeftOnDisk() {
        val result = SubtitleZipExtractor.extractSubtitleFromStream(
            context,
            createZipStream(mapOf("movie.es.srt" to "es", "movie.fr.srt" to "fr", "movie.en.srt" to "en", "movie.de.srt" to "de"))
        ) as ZipExtractionResult.Success

        assertEquals("en", result.extractedFile.readText())
        assertEquals(listOf(result.extractedFile), filesUnderRoot())
    }

    @Test
    fun aFailedImport_leavesNothingOnDisk() {
        val result = SubtitleZipExtractor.extractSubtitleFromStream(context, createZipStream(mapOf("readme.txt" to "no subtitles")))

        assertTrue(result is ZipExtractionResult.Failure)
        assertEquals(emptyList<File>(), filesUnderRoot())
        assertEquals("not even an empty folder", emptyList<File>(), extractRoot.listFiles()?.toList().orEmpty())
    }

    @Test
    fun twoArchivesWithTheSameEntryName_doNotOverwriteEachOther() {
        val first = SubtitleZipExtractor.extractSubtitleFromStream(context, createZipStream(mapOf("English.srt" to "from the first zip")))
            as ZipExtractionResult.Success
        val second = SubtitleZipExtractor.extractSubtitleFromStream(context, createZipStream(mapOf("English.srt" to "from the second zip")))
            as ZipExtractionResult.Success

        assertEquals("the first import is still what the player may be reading", "from the first zip", first.extractedFile.readText())
        assertEquals("from the second zip", second.extractedFile.readText())
        assertFalse(first.extractedFile == second.extractedFile)
    }

    @Test
    fun importsOlderThanTheStaleAge_areRemoved_andRecentOnesStay() {
        val old = File(extractRoot, "import_old").apply { mkdirs() }
        File(old, "Old.srt").writeText("old")
        old.setLastModified(System.currentTimeMillis() - SubtitleZipExtractor.STALE_IMPORT_MS - 60_000)
        val recent = File(extractRoot, "import_recent").apply { mkdirs() }
        File(recent, "Recent.srt").writeText("recent")
        recent.setLastModified(System.currentTimeMillis() - 60_000)

        SubtitleZipExtractor.extractSubtitleFromStream(context, createZipStream(mapOf("New.srt" to "new")))

        assertFalse("a stale import should be pruned", old.exists())
        assertTrue("a recent import may still be in use", File(recent, "Recent.srt").exists())
    }

    @Test
    fun aZipSlipEntryName_neverWritesOutsideTheImportFolder() {
        val result = SubtitleZipExtractor.extractSubtitleFromStream(
            context,
            createZipStream(mapOf("../../evil.srt" to "x", "..\\..\\evil2.srt" to "y"))
        )

        assertTrue("an entry's directory part is dropped, so the file is written inside: $result", result is ZipExtractionResult.Success)
        val written = (result as ZipExtractionResult.Success).extractedFile.canonicalPath
        assertTrue("$written is outside ${extractRoot.canonicalPath}", written.startsWith(extractRoot.canonicalPath + File.separator))
        assertFalse(File(context.cacheDir, "evil.srt").exists())
        assertFalse(File(context.cacheDir.parentFile, "evil.srt").exists())
    }
}
