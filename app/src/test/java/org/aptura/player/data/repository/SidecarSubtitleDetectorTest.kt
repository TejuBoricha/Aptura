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

    // ---- Regression for audit P0-10 -------------------------------------------------------------------

    /** The ViewModel hands over the cleaned, extension-less title ("Show.S01E02"), not the file name. */
    @Test
    fun episodeTitleWithoutExtension_pairsOnlyThatEpisode() {
        val dir = tempFolder.newFolder("series")
        listOf("E01", "E02", "E03").forEach { ep ->
            File(dir, "Show.S01$ep.mkv").createNewFile()
            writeValidSrt(File(dir, "Show.S01$ep.srt"))
        }
        val episode2 = File(dir, "Show.S01E02.mkv")

        val results = SidecarSubtitleDetector.detectSidecars(
            context,
            Uri.fromFile(episode2),
            videoDisplayName = "Show.S01E02" // exactly what PlayerUiState.videoTitle contains
        )

        assertEquals(listOf("Show.S01E02.srt"), results.map { it.displayName })
        assertTrue("the exact match is the one that auto-enables", results.single().isDefault)
    }

    @Test
    fun dottedMovieTitle_isNotChoppedAtItsLastDot() {
        val dir = tempFolder.newFolder("dotted")
        val video = File(dir, "Spider.Man.2002.mkv").apply { createNewFile() }
        writeValidSrt(File(dir, "Spider.Man.2002.srt"))
        writeValidSrt(File(dir, "Spider.Man.2002.en.srt"))
        writeValidSrt(File(dir, "Spider.Man.2.2004.srt")) // a different film

        val results = SidecarSubtitleDetector.detectSidecars(context, Uri.fromFile(video), "Spider.Man.2002")

        assertEquals(setOf("Spider.Man.2002.srt", "Spider.Man.2002.en.srt"), results.map { it.displayName }.toSet())
        assertEquals("only one track auto-enables", 1, results.count { it.isDefault })
        assertTrue(results.first { it.isDefault }.isExactMatch)
    }

    @Test
    fun longerEpisodeNumbersOrSuffixes_doNotMatch() {
        val dir = tempFolder.newFolder("prefixes")
        val video = File(dir, "Show.S01E01.mkv").apply { createNewFile() }
        writeValidSrt(File(dir, "Show.S01E011.srt"))
        writeValidSrt(File(dir, "Show.S01E01x.srt"))
        writeValidSrt(File(dir, "Show.S01E01.Remastered.srt"))

        assertTrue(SidecarSubtitleDetector.detectSidecars(context, Uri.fromFile(video)).isEmpty())
    }

    @Test
    fun languageAndRoleTags_areLabelled_andOnlyPlainEnglishAutoEnables() {
        val dir = tempFolder.newFolder("tags")
        val video = File(dir, "Film.mkv").apply { createNewFile() }
        listOf("Film.en.srt", "Film.pt-BR.srt", "Film.en.forced.srt", "Film.fr.sdh.srt")
            .forEach { writeValidSrt(File(dir, it)) }

        val results = SidecarSubtitleDetector.detectSidecars(context, Uri.fromFile(video))
        val labels = results.associate { it.displayName to it.languageLabel }

        assertEquals("English", labels["Film.en.srt"])
        assertEquals("Portuguese", labels["Film.pt-BR.srt"])
        assertEquals("English · Forced", labels["Film.en.forced.srt"])
        assertEquals("French · SDH", labels["Film.fr.sdh.srt"])
        assertEquals(listOf("Film.en.srt"), results.filter { it.isDefault }.map { it.displayName })
    }

    @Test
    fun stripVideoExtension_onlyStripsRealVideoExtensions() {
        assertEquals("Spider.Man.2002", SidecarSubtitleDetector.stripVideoExtension("Spider.Man.2002.mkv"))
        assertEquals("Show.S01E01", SidecarSubtitleDetector.stripVideoExtension("Show.S01E01"))
        assertEquals("clip", SidecarSubtitleDetector.stripVideoExtension("clip.MP4"))
    }
}
