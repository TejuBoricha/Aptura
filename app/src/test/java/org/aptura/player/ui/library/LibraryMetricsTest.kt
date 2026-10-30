package org.aptura.player.ui.library

import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.LibraryDensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** How many cards fit across at each size, which is the whole meaning of "Compact", "Comfortable" and "Large". */
class LibraryMetricsTest {

    /** Columns an adaptive grid makes: as many cells of at least [min] as fit between the gutters and gaps. */
    private fun columns(widthDp: Int, min: androidx.compose.ui.unit.Dp, gutter: Int, gap: Int): Int {
        val available = widthDp - 2 * gutter
        return maxOf(1, (available + gap) / (min.value.toInt() + gap))
    }

    @Test
    fun onAPhone_compactFitsThreeAcross_comfortableTwo_largeOne() {
        // 360 dp wide, 16 dp gutters, as on a common phone in portrait.
        fun cols(density: LibraryDensity) = columns(
            360, LibraryMetrics.gridMinCell(density, 360.dp, isWide = false), 16,
            LibraryMetrics.gridColumnGap(density, isWide = false).value.toInt()
        )

        assertEquals(3, cols(LibraryDensity.COMPACT))
        assertEquals(2, cols(LibraryDensity.COMFORTABLE))
        assertEquals(1, cols(LibraryDensity.LARGE))
    }

    @Test
    fun onASmallPhone_comfortableStillFitsTwo() {
        // The 140 dp minimum this replaced collapsed a 320 dp phone to one huge column.
        val gap = LibraryMetrics.gridColumnGap(LibraryDensity.COMFORTABLE, isWide = false).value.toInt()
        assertEquals(2, columns(320, LibraryMetrics.gridMinCell(LibraryDensity.COMFORTABLE, 320.dp, false), 16, gap))
    }

    @Test
    fun theSizesGrowFromCompactToLarge_everywhere() {
        listOf(360.dp to false, 700.dp to true, 1280.dp to true).forEach { (width, wide) ->
            val compact = LibraryMetrics.gridMinCell(LibraryDensity.COMPACT, width, wide)
            val comfortable = LibraryMetrics.gridMinCell(LibraryDensity.COMFORTABLE, width, wide)
            val large = LibraryMetrics.gridMinCell(LibraryDensity.LARGE, width, wide)
            assertTrue("$width: $compact < $comfortable < $large", compact < comfortable && comfortable < large)
        }
    }

    @Test
    fun listPicturesGrowWithTheSize_andRowsAreOneColumnOnAPhone() {
        val widths = LibraryDensity.entries.map { LibraryMetrics.listThumbnailWidth(it) }
        assertEquals(widths.sortedBy { it.value }, widths)
        LibraryDensity.entries.forEach { density ->
            val cols = columns(360, LibraryMetrics.listMinCell(density), 16, 20)
            assertEquals("$density rows are one column on a 360 dp phone", 1, cols)
        }
    }
}
