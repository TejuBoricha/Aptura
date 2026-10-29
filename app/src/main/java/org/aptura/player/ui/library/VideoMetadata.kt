package org.aptura.player.ui.library

import org.aptura.player.domain.model.LibraryDetail
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The lines of small print under a video's title, for the chosen level of detail. One function for the grid card and
 * the list row, so the two can never disagree about what a level means.
 *
 *  - [LibraryDetail.TITLE_ONLY]: nothing; the title says it.
 *  - [LibraryDetail.STANDARD]: size and folder, as before.
 *  - [LibraryDetail.DETAILED]: picture quality and size, then the folder, then when it was added. One fact per line,
 *    so that on a narrow card nothing is cut off in the middle of a date.
 */
internal fun videoMetadataLines(
    card: VideoCardUiModel,
    detail: LibraryDetail,
    nowMillis: Long = System.currentTimeMillis()
): List<String> = when (detail) {
    LibraryDetail.TITLE_ONLY -> emptyList()
    LibraryDetail.STANDARD -> listOf("${card.fileSizeFormatted}  ·  ${card.folderName}")
    LibraryDetail.DETAILED -> {
        val quality = MediaFormatters.qualityLabel(card.rawItem.width, card.rawItem.height)
        val first = listOfNotNull(quality, card.fileSizeFormatted).joinToString("  ·  ")
        val added = formatAddedDate(card.rawItem.dateAddedSec, nowMillis)
        listOfNotNull(first, card.folderName, added?.let { "Added $it" })
    }
}

/**
 * "Oct 29" for this year, "Oct 29, 2023" for an earlier one, "Today" and "Yesterday" for those, null when the library
 * has no date for the file. Times are the device's own, like the rest of the library.
 */
internal fun formatAddedDate(
    seconds: Long,
    nowMillis: Long,
    locale: Locale = Locale.getDefault()
): String? {
    if (seconds <= 0L) return null
    val then = Calendar.getInstance().apply { timeInMillis = seconds * 1000L }
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    fun Calendar.dayKey() = get(Calendar.YEAR) * 1000 + get(Calendar.DAY_OF_YEAR)
    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        then.dayKey() == now.dayKey() -> "today"
        then.dayKey() == yesterday.dayKey() -> "yesterday"
        then.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> SimpleDateFormat("MMM d", locale).format(Date(seconds * 1000L))
        else -> SimpleDateFormat("MMM d, yyyy", locale).format(Date(seconds * 1000L))
    }
}
