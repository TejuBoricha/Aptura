package org.aptura.player.ui.library

import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.NaturalOrder
import org.aptura.player.domain.model.cleanDisplayTitle

/** What the grid is actually sorted by when the user has not picked a sort: newest first overall, episode order in a folder. */
fun automaticSortOrder(selectedFolder: String?): LibrarySortOrder =
    if (selectedFolder != null) LibrarySortOrder.TITLE_ASC else LibrarySortOrder.DATE_ADDED_DESC

private val byNaturalTitle: Comparator<DomainMediaItem> =
    Comparator { a, b -> NaturalOrder.compare(cleanDisplayTitle(a.title), cleanDisplayTitle(b.title)) }

/**
 * Orders library items. Every order falls back to the natural title, so items that tie (a whole season downloaded in
 * the same second, say) always appear in a stable, readable order instead of an arbitrary one.
 */
fun List<DomainMediaItem>.sortedFor(order: LibrarySortOrder): List<DomainMediaItem> = when (order) {
    LibrarySortOrder.TITLE_ASC -> sortedWith(byNaturalTitle)
    LibrarySortOrder.DATE_ADDED_DESC -> sortedWith(compareByDescending<DomainMediaItem> { it.dateAddedSec }.then(byNaturalTitle))
    LibrarySortOrder.LAST_PLAYED_DESC -> sortedWith(compareByDescending<DomainMediaItem> { it.lastPlayedTimestamp }.then(byNaturalTitle))
    LibrarySortOrder.DURATION_DESC -> sortedWith(compareByDescending<DomainMediaItem> { it.durationMs }.then(byNaturalTitle))
    LibrarySortOrder.SIZE_DESC -> sortedWith(compareByDescending<DomainMediaItem> { it.sizeBytes }.then(byNaturalTitle))
}

/** A video is "in progress" once it has been watched a little and is not (nearly) finished. */
private const val MIN_RESUME_POSITION_MS = 10_000L
private const val FINISHED_FRACTION = 0.95f
const val CONTINUE_WATCHING_LIMIT = 10

/** Videos to offer under "Continue watching": started, not finished, still on disk; most recently played first. */
fun List<DomainMediaItem>.continueWatching(limit: Int = CONTINUE_WATCHING_LIMIT): List<DomainMediaItem> =
    asSequence()
        .filter { item ->
            !item.isMissing &&
                !item.isCompleted &&
                item.durationMs > 0L &&
                item.playbackPositionMs >= MIN_RESUME_POSITION_MS &&
                item.playbackPositionMs.toFloat() / item.durationMs < FINISHED_FRACTION
        }
        .sortedWith(compareByDescending<DomainMediaItem> { it.lastPlayedTimestamp }.then(byNaturalTitle))
        .take(limit)
        .toList()
