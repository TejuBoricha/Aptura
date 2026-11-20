@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.core.media

import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import org.aptura.player.domain.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ChapterExtractorTest {

    @Test
    fun testExtractFromMetadata_extractsAndSortsId3Chapters() {
        val frame1 = ChapterFrame("ch2", 15_000, 30_000, 0, 0, arrayOf(
            TextInformationFrame("TIT2", "Action Scene", listOf("Action Scene"))
        ))
        val frame2 = ChapterFrame("ch1", 0, 15_000, 0, 0, arrayOf(
            TextInformationFrame("TIT2", "Introduction", listOf("Introduction"))
        ))
        val metadata = Metadata(frame1, frame2)

        val chapters = ChapterExtractor.extractFromMetadata(metadata)

        assertEquals(2, chapters.size)
        assertEquals("ch1", chapters[0].id)
        assertEquals("Introduction", chapters[0].title)
        assertEquals(0L, chapters[0].startTimeMs)
        assertEquals(15_000L, chapters[0].endTimeMs)

        assertEquals("ch2", chapters[1].id)
        assertEquals("Action Scene", chapters[1].title)
        assertEquals(15_000L, chapters[1].startTimeMs)
        assertEquals(30_000L, chapters[1].endTimeMs)
    }

    @Test
    fun testParseMp4ChplPayload_version0() {
        val out = ByteArrayOutputStream()
        // Header: version (1 byte = 0), flags (3 bytes = 0), reserved (1 byte = 0), count (1 byte = 2)
        out.write(0) // version
        out.write(byteArrayOf(0, 0, 0)) // flags
        out.write(0) // reserved
        out.write(2) // count = 2 chapters

        // Chapter 1: 0 ms -> 0 in 100ns units
        val bb1 = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(0L)
        out.write(bb1.array())
        val title1 = "Opening Credits".toByteArray(Charsets.UTF_8)
        out.write(title1.size)
        out.write(title1)

        // Chapter 2: 90,000 ms -> 90_000 * 10_000 = 900_000_000 in 100ns units
        val bb2 = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(900_000_000L)
        out.write(bb2.array())
        val title2 = "The Journey Begins".toByteArray(Charsets.UTF_8)
        out.write(title2.size)
        out.write(title2)

        val chapters = mutableListOf<Chapter>()
        ChapterExtractor.parseMp4ChplPayload(out.toByteArray(), chapters)

        assertEquals(2, chapters.size)
        assertEquals("Opening Credits", chapters[0].title)
        assertEquals(0L, chapters[0].startTimeMs)

        assertEquals("The Journey Begins", chapters[1].title)
        assertEquals(90_000L, chapters[1].startTimeMs)
    }

    @Test
    fun testParseMp4ChplPayload_version1() {
        val out = ByteArrayOutputStream()
        // Header: version (1 byte = 1), flags (3 bytes = 0), reserved (4 bytes = 0), count (4 bytes = 1)
        out.write(1) // version
        out.write(byteArrayOf(0, 0, 0)) // flags
        out.write(byteArrayOf(0, 0, 0, 0)) // reserved 4 bytes
        val countBuf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(1)
        out.write(countBuf.array())

        // Chapter: 45,000 ms -> 450_000_000L
        val bb = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(450_000_000L)
        out.write(bb.array())
        val title = "Intermission".toByteArray(Charsets.UTF_8)
        out.write(title.size)
        out.write(title)

        val chapters = mutableListOf<Chapter>()
        ChapterExtractor.parseMp4ChplPayload(out.toByteArray(), chapters)

        assertEquals(1, chapters.size)
        assertEquals("Intermission", chapters[0].title)
        assertEquals(45_000L, chapters[0].startTimeMs)
    }

    @Test
    fun testParseMkvChaptersPayload_extractsChapterAtom() {
        val out = ByteArrayOutputStream()

        // ChapterAtom payload:
        val atomPayload = ByteArrayOutputStream()

        // 1. TimeStart (ID: 0x91, Size: 4, value: 5,000,000,000 ns = 5,000 ms)
        atomPayload.write(0x91)
        atomPayload.write(0x84) // EBML size 4 encoded as 0x80 | 4
        atomPayload.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(5_000_000).array()) // 5M * 1000 = 5s

        // 2. Display element (ID: 0x80)
        val displayPayload = ByteArrayOutputStream()
        // ChapString (ID: 0x85, Size: 8, "Act One")
        val strBytes = "Act One".toByteArray(Charsets.UTF_8)
        displayPayload.write(0x85)
        displayPayload.write(0x80 or strBytes.size)
        displayPayload.write(strBytes)

        val displayBytes = displayPayload.toByteArray()
        atomPayload.write(0x80)
        atomPayload.write(0x80 or displayBytes.size)
        atomPayload.write(displayBytes)

        val atomBytes = atomPayload.toByteArray()

        // Wrap inside EditionEntry (ID: 0x45B9)
        out.write(0x45)
        out.write(0xB9)
        val editionSize = atomBytes.size + 2 // 0xB6 + size byte + atomBytes
        out.write(0x80 or editionSize)

        // ChapterAtom (ID: 0xB6)
        out.write(0xB6)
        out.write(0x80 or atomBytes.size)
        out.write(atomBytes)

        val chapters = mutableListOf<Chapter>()
        ChapterExtractor.parseMkvChaptersPayload(out.toByteArray(), chapters)

        assertEquals(1, chapters.size)
        assertEquals("Act One", chapters[0].title)
        assertEquals(5L, chapters[0].startTimeMs) // 5,000,000 ns = 5 ms
    }
}
