package org.aptura.player.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking

/** Labels shown in the subtitle sheet / toast must be human-readable for both sidecar shapes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidSidecarSubtitleResolverTest {

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
    fun exactMatchWithoutLanguage_labelIsPlainFileName() {
        val dir = tempFolder.newFolder("exact")
        val video = File(dir, "Show.S01E03.mkv").apply { createNewFile() }
        writeValidSrt(File(dir, "Show.S01E03.srt"))

        val subs = runBlocking { AndroidSidecarSubtitleResolver(context).resolve(video.toURI().toString()) }

        assertEquals(1, subs.size)
        // Regression: this used to read "External (Sidecar) (Show.S01E03.srt)".
        assertEquals("Show.S01E03.srt", subs.first().label)
    }

    @Test
    fun languageTaggedSidecar_labelKeepsLanguageAndFileName() {
        val dir = tempFolder.newFolder("tagged")
        val video = File(dir, "Film.mkv").apply { createNewFile() }
        writeValidSrt(File(dir, "Film.en.srt"))

        val subs = runBlocking { AndroidSidecarSubtitleResolver(context).resolve(video.toURI().toString()) }

        assertEquals(1, subs.size)
        assertEquals("English (Film.en.srt)", subs.first().label)
        assertEquals("en", subs.first().language)
    }
}
