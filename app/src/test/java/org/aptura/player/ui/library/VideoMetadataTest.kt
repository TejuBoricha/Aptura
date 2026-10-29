package org.aptura.player.ui.library

import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.LibraryDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale

/** What each level of detail says under a video's title. */
class VideoMetadataTest {

    private val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 29, 12, 0, 0) }.timeInMillis

    private fun card(width: Int = 1920, height: Int = 1080, addedSec: Long = now / 1000 - 10 * 86_400L): VideoCardUiModel {
        val item = DomainMediaItem(
            uri = "content://media/1",
            title = "Arrival",
            durationMs = 6_960_000L,
            sizeBytes = 3_100_000_000L,
            mimeType = "video/mp4",
            width = width,
            height = height,
            dateAddedSec = addedSec,
            folderPath = "Movies"
        )
        return VideoCardUiModel(
            uri = item.uri,
            title = item.title,
            durationFormatted = "1:56:00",
            fileSizeFormatted = "2.9 GB",
            folderName = "Movies",
            rawItem = item
        )
    }

    @Test
    fun titleOnly_saysNothingBeyondTheTitle() {
        assertEquals(emptyList<String>(), videoMetadataLines(card(), LibraryDetail.TITLE_ONLY, now))
    }

    @Test
    fun standard_isTheSizeAndTheFolder() {
        assertEquals(listOf("2.9 GB  ·  Movies"), videoMetadataLines(card(), LibraryDetail.STANDARD, now))
    }

    @Test
    fun detailed_addsQualityAndTheDateAdded_oneFactPerLine() {
        val lines = videoMetadataLines(card(), LibraryDetail.DETAILED, now)

        assertEquals("1080p  ·  2.9 GB", lines[0])
        assertEquals("Movies", lines[1])
        assertEquals("Added Oct 19", lines[2])
    }

    @Test
    fun detailed_leavesOutWhatIsNotKnown() {
        val lines = videoMetadataLines(card(width = 0, height = 0, addedSec = 0L), LibraryDetail.DETAILED, now)

        assertEquals(listOf("2.9 GB", "Movies"), lines)
    }

    @Test
    fun theDateAdded_isRelativeForRecentDaysAndCarriesTheYearWhenOld() {
        val dayMillis = 86_400_000L
        assertEquals("today", formatAddedDate(now / 1000, now, Locale.US))
        assertEquals("yesterday", formatAddedDate((now - dayMillis) / 1000, now, Locale.US))
        assertEquals("Oct 19", formatAddedDate((now - 10 * dayMillis) / 1000, now, Locale.US))
        assertEquals("Jan 5, 2024", formatAddedDate(
            Calendar.getInstance().apply { set(2024, Calendar.JANUARY, 5, 12, 0, 0) }.timeInMillis / 1000, now, Locale.US
        ))
        assertNull(formatAddedDate(0L, now, Locale.US))
    }
}
