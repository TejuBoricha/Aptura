package org.aptura.player.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SidecarSubtitleDetectorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun writeValidSrt(file: File) {
        file.writeText("""
            1
            00:00:01,000 --> 00:00:03,000
            Valid subtitle text
        """.trimIndent())
    }

    @Test
    fun detectSidecars_exactMatchingSrt_detectsSuccessfully() {
        val mediaDir = tempFolder.newFolder("movies")
        val videoFile = File(mediaDir, "Inception.mp4").apply { createNewFile() }
        val srtFile = File(mediaDir, "Inception.srt")
        writeValidSrt(srtFile)

        val results = SidecarSubtitleDetector.detectSidecars(context, Uri.fromFile(videoFile))

        assertEquals(1, results.size)
        val first = results.first()
        assertEquals("Inception.srt", first.displayName)
        assertEquals("application/x-subrip", first.mimeType)
        assertTrue(first.isDefault)
    }

    @Test
    fun detectSidecars_languageSuffixes_detectsAndMapsLanguageLabels() {
        val mediaDir = tempFolder.newFolder("movies2")
        val videoFile = File(mediaDir, "Dune.mkv").apply { createNewFile() }
        val srtEn = File(mediaDir, "Dune.en.srt")
        val srtEs = File(mediaDir, "Dune.es.srt")
        writeValidSrt(srtEn)
        writeValidSrt(srtEs)

        val results = SidecarSubtitleDetector.detectSidecars(context, Uri.fromFile(videoFile))

        assertEquals(2, results.size)
        val enMatch = results.find { it.languageCode == "en" }
        val esMatch = results.find { it.languageCode == "es" }

        assertNotNull(enMatch)
        assertEquals("English", enMatch?.languageLabel)

        assertNotNull(esMatch)
        assertEquals("Spanish", esMatch?.languageLabel)
    }

    @Test
    fun detectSidecars_inSubsSubdirectory_detectsSuccessfully() {
        val mediaDir = tempFolder.newFolder("movies3")
        val videoFile = File(mediaDir, "Interstellar.mp4").apply { createNewFile() }
        val subsDir = File(mediaDir, "subs").apply { mkdir() }
        val srtFile = File(subsDir, "Interstellar.srt")
        writeValidSrt(srtFile)

        val results = SidecarSubtitleDetector.detectSidecars(context, Uri.fromFile(videoFile))

        assertEquals(1, results.size)
        assertEquals("Interstellar.srt", results.first().displayName)
    }

    @Test
    fun detectSidecars_fuzzyDelimiterMatching_detectsSuccessfully() {
        val mediaDir = tempFolder.newFolder("movies4")
        val videoFile = File(mediaDir, "Gladiator_2000_Extended.mp4").apply { createNewFile() }
        val srtFile = File(mediaDir, "Gladiator.2000.Extended.en.srt")
        writeValidSrt(srtFile)

        val results = SidecarSubtitleDetector.detectSidecars(context, Uri.fromFile(videoFile))

        assertEquals(1, results.size)
        assertEquals("Gladiator.2000.Extended.en.srt", results.first().displayName)
        assertEquals("English", results.first().languageLabel)
    }

    @Test
    fun normalizeTitle_replacesPunctuationWithSpaces() {
        assertEquals("gladiator 2000 extended", SidecarSubtitleDetector.normalizeTitle("Gladiator_2000.Extended"))
        assertEquals("movie title 1080p", SidecarSubtitleDetector.normalizeTitle("movie-title..1080p"))
    }
}
