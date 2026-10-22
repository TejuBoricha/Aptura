package org.aptura.player.data.scanner

import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.core.diagnostics.EventCode
import org.aptura.player.data.database.MediaItemEntity
import kotlin.math.abs

/**
 * File Reconciliation Engine adhering to spec/04 FR-LIB-018 and FR-LIB-021.
 *
 * Detects moved, renamed, unmounted, or restored video files during library scans.
 * Ensures zero data loss: user watch progress, completion state, and bookmarks
 * are never silently wiped when an external drive/SD card is unplugged or a file is renamed.
 */
object FileReconciliationEngine {

    data class ReconciliationResult(
        val itemsToUpsert: List<MediaItemEntity>,
        val idsToDelete: List<Long>,
        val movedCount: Int,
        val missingCount: Int,
        val restoredCount: Int,
        val newCount: Int
    )

    /**
     * Reconciles newly scanned video items against currently persisted database items.
     *
     * @param scannedItems Items discovered during the current scan (MediaStore + SAF trees)
     * @param existingItems Items currently recorded in the Room database
     */
    fun reconcile(
        scannedItems: List<MediaItemEntity>,
        existingItems: List<MediaItemEntity>
    ): ReconciliationResult {
        val itemsToUpsert = mutableListOf<MediaItemEntity>()
        val idsToDelete = mutableListOf<Long>()

        var movedCount = 0
        var missingCount = 0
        var restoredCount = 0
        var newCount = 0

        // Index scanned items by URI and MediaStore ID
        val scannedByUri = scannedItems.associateBy { it.uri }.toMutableMap()
        val scannedById = scannedItems.associateBy { it.mediaStoreId }.toMutableMap()

        // Track scanned items that have been paired with an existing item
        val matchedScannedUris = mutableSetOf<String>()
        val unmatchedExisting = mutableListOf<MediaItemEntity>()

        // Pass 1: Direct Matching (exact URI or MediaStore ID)
        for (existing in existingItems) {
            val matchedScanned = scannedByUri[existing.uri] ?: scannedById[existing.mediaStoreId]

            if (matchedScanned != null) {
                matchedScannedUris.add(matchedScanned.uri)

                val wasMissing = existing.isMissing
                if (wasMissing) {
                    restoredCount++
                }

                // Preserve user progress, bookmarks relation, and completion state
                itemsToUpsert.add(
                    matchedScanned.copy(
                        id = existing.id,
                        playbackPositionMs = existing.playbackPositionMs,
                        isCompleted = existing.isCompleted,
                        lastPlayedTimestamp = existing.lastPlayedTimestamp,
                        isMissing = false
                    )
                )
            } else {
                unmatchedExisting.add(existing)
            }
        }

        // Remaining scanned items that haven't been matched directly
        val remainingScanned = scannedItems.filter { it.uri !in matchedScannedUris }.toMutableList()

        // Pass 2: Heuristic Matching for Renamed / Moved Files (FR-LIB-018)
        // Matches by identical file size and duration within ±1000ms
        for (existing in unmatchedExisting) {
            val candidateIndex = remainingScanned.indexOfFirst { candidate ->
                candidate.sizeBytes > 0L &&
                    candidate.sizeBytes == existing.sizeBytes &&
                    abs(candidate.durationMs - existing.durationMs) <= 1000L
            }

            if (candidateIndex >= 0) {
                val candidate = remainingScanned.removeAt(candidateIndex)
                matchedScannedUris.add(candidate.uri)
                movedCount++

                // Transfer progress from existing item to new location/name
                itemsToUpsert.add(
                    candidate.copy(
                        id = existing.id,
                        playbackPositionMs = existing.playbackPositionMs,
                        isCompleted = existing.isCompleted,
                        lastPlayedTimestamp = existing.lastPlayedTimestamp,
                        isMissing = false
                    )
                )

                ApturaDiagnostics.logEvent(
                    code = EventCode.EV_006,
                    payload = mapOf(
                        "reconciliation" to "file_moved_or_renamed",
                        "oldTitle" to existing.title,
                        "newTitle" to candidate.title,
                        "sizeBytes" to existing.sizeBytes.toString()
                    )
                )
            } else {
                // Pass 3: Unmatched existing files are marked as missing (FR-LIB-018, FR-LIB-021)
                // NEVER silently delete; preserve bookmarks and progress for when the storage reconnects
                if (!existing.isMissing) {
                    missingCount++
                    itemsToUpsert.add(existing.copy(isMissing = true))
                }
            }
        }

        // Pass 4: Remaining scanned items are genuinely brand new files
        for (scanned in remainingScanned) {
            newCount++
            itemsToUpsert.add(scanned.copy(isMissing = false))
        }

        return ReconciliationResult(
            itemsToUpsert = itemsToUpsert,
            idsToDelete = idsToDelete,
            movedCount = movedCount,
            missingCount = missingCount,
            restoredCount = restoredCount,
            newCount = newCount
        )
    }
}
