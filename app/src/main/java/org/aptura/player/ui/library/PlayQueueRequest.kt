package org.aptura.player.ui.library

import org.aptura.player.domain.model.DomainMediaItem

/** A request to start playback: the queue (in display order) and which entry to start on. */
data class PlayQueueRequest(
    val items: List<DomainMediaItem>,
    val startIndex: Int
)

/**
 * Builds the playback queue for a click in the library grid / details pane.
 *
 * The *library's own* domain items must be passed through untouched: they carry the saved resume
 * position, completion flag and per-video preferences (speed, aspect, decoder, delays). Re-creating
 * items from title/uri/duration (as the navigation layer used to) silently discarded all of that, so
 * "resume where you left off" never worked from the grid (audit P0-04).
 */
fun buildPlayQueueRequest(
    clicked: DomainMediaItem,
    visibleItems: List<DomainMediaItem>
): PlayQueueRequest {
    // Files that no longer exist on storage cannot be played. Leaving them in the queue turned "Next" (and the
    // lock-screen / headset next button) into a "Storage Inaccessible" error screen.
    val playable = visibleItems.filter { !it.isMissing || it.uri == clicked.uri }
    val index = playable.indexOfFirst { it.uri == clicked.uri }
    return if (index >= 0) {
        PlayQueueRequest(items = playable, startIndex = index)
    } else {
        PlayQueueRequest(items = listOf(clicked), startIndex = 0)
    }
}
