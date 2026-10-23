package org.aptura.player.core.media

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubtitleZipExtractorTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

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
}
