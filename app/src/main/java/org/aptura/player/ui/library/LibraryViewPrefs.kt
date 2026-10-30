package org.aptura.player.ui.library

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.FolderLayout
import org.aptura.player.domain.model.LibraryDensity
import org.aptura.player.domain.model.LibraryDetail
import org.aptura.player.domain.model.LibraryLayout

/** How the library is shown to this user. Persisted in settings; the screen only reads it and reports changes. */
@Immutable
data class LibraryViewPrefs(
    val layout: LibraryLayout = LibraryLayout.GRID,
    val density: LibraryDensity = LibraryDensity.COMFORTABLE,
    val detail: LibraryDetail = LibraryDetail.STANDARD,
    val folderLayout: FolderLayout = FolderLayout.LIST
)

/**
 * The measurements behind each layout and size, in one place so the grid, the list and the previews agree.
 *
 * Sizes are minimum cell widths for an adaptive grid: the grid fits as many cells as it can, so the same choice gives
 * two columns on a phone and five on a tablet, instead of a column count that would be wrong on one of them.
 */
internal object LibraryMetrics {

    /** The smallest a grid cell may be. "Large" is a single big card on a phone, which is the point of it. */
    fun gridMinCell(density: LibraryDensity, width: Dp, isWide: Boolean): Dp = when (density) {
        // 100 dp keeps three columns on a 360 dp phone (3 x 100 + 2 x 10 fits 328); 132 keeps two on a 320 dp one (140
        // collapsed it to one column).
        LibraryDensity.COMPACT -> if (width >= 1200.dp) 190.dp else if (isWide) 160.dp else 100.dp
        LibraryDensity.COMFORTABLE -> if (width >= 1200.dp) 240.dp else if (isWide) 200.dp else 132.dp
        LibraryDensity.LARGE -> if (width >= 1200.dp) 340.dp else if (isWide) 280.dp else 190.dp
    }

    /** Rows are one column on a phone and as many as fit on a tablet. */
    fun listMinCell(density: LibraryDensity): Dp = when (density) {
        LibraryDensity.COMPACT -> 320.dp
        LibraryDensity.COMFORTABLE -> 340.dp
        LibraryDensity.LARGE -> 380.dp
    }

    /** The picture beside the text in a list row. */
    fun listThumbnailWidth(density: LibraryDensity): Dp = when (density) {
        LibraryDensity.COMPACT -> 96.dp
        LibraryDensity.COMFORTABLE -> 132.dp
        LibraryDensity.LARGE -> 172.dp
    }

    fun gridColumnGap(density: LibraryDensity, isWide: Boolean): Dp = when (density) {
        LibraryDensity.COMPACT -> if (isWide) 14.dp else 10.dp
        LibraryDensity.COMFORTABLE -> if (isWide) 20.dp else 14.dp
        LibraryDensity.LARGE -> if (isWide) 24.dp else 16.dp
    }

    fun gridRowGap(density: LibraryDensity): Dp = when (density) {
        LibraryDensity.COMPACT -> 12.dp
        LibraryDensity.COMFORTABLE -> 18.dp
        LibraryDensity.LARGE -> 22.dp
    }

    /** Rows sit closer than cards, because each is already a short, full-width strip. */
    fun listRowGap(density: LibraryDensity): Dp = when (density) {
        LibraryDensity.COMPACT -> 4.dp
        LibraryDensity.COMFORTABLE -> 8.dp
        LibraryDensity.LARGE -> 12.dp
    }
}
