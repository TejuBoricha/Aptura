package org.aptura.player.data.scanner

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.core.diagnostics.EventCode
import org.aptura.player.core.media.ThumbnailRepository
import org.aptura.player.core.platform.PermissionHelper
import org.aptura.player.data.database.ApturaDatabase
import org.aptura.player.data.database.MediaItemEntity
import org.aptura.player.data.settings.ApturaSettingsRepository

/**
 * Background worker for local media library ingest using WorkManager.
 * Ingests MediaStore videos and persisted SAF document trees, applying FileReconciliationEngine
 * to preserve user progress, bookmarks, and detect moved/renamed/unmounted files (FR-LIB-004, FR-LIB-018, FR-LIB-021).
 */
class MediaScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG_ONE_TIME_SCAN = "aptura_media_scan_one_time"
        const val TAG_PERIODIC_SCAN = "aptura_media_scan_periodic"
        private const val BATCH_SIZE = 250

        /**
         * Fills in durations for MediaStore rows the platform has not probed yet
         * (fresh screen recordings/downloads land with DURATION null).
         *
         * Two resolutions, both cheap:
         *  - A row whose previous scan already knows the duration and whose file size is unchanged
         *    carries that duration forward (no platform API call, no duration regression to 0:00).
         *  - A genuinely unknown row is measured once with MediaMetadataRetriever. Failed probes keep
         *    durationMs = 0 (the grid hides an unknown badge) and retry on the next scan only.
         */
        internal fun resolveMissingDurations(
            context: Context,
            items: List<MediaItemEntity>,
            existingByUri: Map<String, MediaItemEntity>
        ): List<MediaItemEntity> {
            // Fast path: every row already carries a duration — nothing to resolve.
            val needsResolution = items.any { it.durationMs <= 0L && it.sizeBytes > 1024L }
            if (!needsResolution) {
                return items
            }

            var retriever: android.media.MediaMetadataRetriever? = null
            try {
                return items.map { item ->
                    if (item.durationMs > 0L || item.sizeBytes <= 1024L) {
                        return@map item
                    }
                    val existing = existingByUri[item.uri]
                    if (existing != null && existing.durationMs > 0L) {
                        // Same size = unchanged file: carry the previously measured duration forward.
                        return@map if (existing.sizeBytes == item.sizeBytes) {
                            item.copy(durationMs = existing.durationMs)
                        } else {
                            item // Replaced content: probe below instead of trusting the stale value.
                        }
                    }
                    if (retriever == null) {
                        retriever = android.media.MediaMetadataRetriever()
                    }
                    try {
                        retriever.setDataSource(context, item.uri.toUri())
                        val durationMs = retriever
                            .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L
                        if (durationMs > 0L) item.copy(durationMs = durationMs) else item
                    } catch (_: Throwable) {
                        // Unreadable/corrupt file: keep durationMs = 0 and show it without a badge.
                        item
                    }
                }
            } finally {
                try {
                    retriever?.release()
                } catch (_: Throwable) {}
            }
        }

        /**
         * Merges one scan's findings into the library in a single transaction: reconciles them against the stored rows,
         * writes the rows and moved bookmarks, and deletes the rows that are really gone. Returns the rows it wrote.
         *
         * [startSnapshot] is what the library held when the scan began. The scan has been reading the storage since,
         * often for minutes, and the person has kept using the app: finishing, renaming, hiding or deleting videos. So the
         * merge is not made against that snapshot. It reads the rows again inside the transaction that writes them, and
         * the person's changes win (audit DATA-05). The snapshot only tells which rows were removed meanwhile.
         */
        internal suspend fun persistScan(
            database: ApturaDatabase,
            scanned: List<MediaItemEntity>,
            startSnapshot: List<MediaItemEntity>,
            hasStorage: Boolean
        ): List<MediaItemEntity> {
            val dao = database.mediaItemDao()

            // One transaction holds the write lock from the read to the last write, so nothing the person does can land in
            // between and then be overwritten.
            return database.withTransaction {
                // The library as it is now, not as it was when the scan began.
                val current = dao.getAllVideosRaw()

                // A row that was there when the scan began and is gone now was removed meanwhile (a deletion, a removed
                // folder). The scan saw its file before that, so it must not bring the row back. If the file is still
                // there, the next scan will find it.
                val currentUris = current.mapTo(HashSet()) { it.uri }
                val removed = startSnapshot.filter { it.uri !in currentUris }
                val removedUris = removed.mapTo(HashSet()) { it.uri }
                val removedStoreIds = removed.mapTo(HashSet()) { it.mediaStoreId }
                val found = scanned.filterNot { it.uri in removedUris || it.mediaStoreId in removedStoreIds }

                // 3. Reconcile scanned items against the rows as they are now
                val reconciliation = FileReconciliationEngine.reconcile(
                    scannedItems = found,
                    existingItems = current
                )

                // Separate genuinely missing external/progress-bearing items from deleted internal MediaStore files with no
                // user data at all. "No user data" means every column the person owns (audit DATA-04), see
                // isPurgeableMissingRow. It is decided from the current rows, so a video the person has just played is not
                // taken for one with no data.
                val (staleInternalToDelete, itemsToUpsert) = reconciliation.itemsToUpsert.partition { item ->
                    isPurgeableMissingRow(item, hasStorage)
                }
                val allIdsToDelete = reconciliation.idsToDelete +
                    staleInternalToDelete.mapNotNull { it.id.takeIf { id -> id > 0L } } +
                    current.filter { item ->
                        isPurgeableMissingRow(item, hasStorage) &&
                            found.none { file -> file.uri == item.uri || file.mediaStoreId == item.mediaStoreId }
                    }.mapNotNull { it.id.takeIf { id -> id > 0L } }

                // 4. Upsert, bookmark remapping and obsolete deletion
                if (itemsToUpsert.isNotEmpty()) {
                    itemsToUpsert.chunked(BATCH_SIZE).forEach { batch ->
                        dao.insertOrUpdateAll(batch)
                    }
                }

                // 4b. Keep bookmarks attached to files whose URI changed (moved/renamed SAF documents)
                val bookmarkDao = database.bookmarkDao()
                reconciliation.uriRemaps.forEach { (oldUri, newUri) ->
                    bookmarkDao.remapMediaUri(oldUri, newUri)
                }

                // 5. Delete obsolete IDs if any were explicitly flagged
                if (allIdsToDelete.isNotEmpty()) {
                    allIdsToDelete.distinct().chunked(500).forEach { batch ->
                        dao.deleteByIds(batch)
                    }
                }

                itemsToUpsert
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val hasStorage = PermissionHelper.hasStoragePermission(applicationContext)

        val settingsRepo = ApturaSettingsRepository(applicationContext)
        val persistedFolders = try {
            settingsRepo.settingsFlow.first().persistedSafFolders
        } catch (_: Throwable) {
            emptySet()
        }

        // If user has neither platform storage permission nor any SAF custom folder, fail early
        if (!hasStorage && persistedFolders.isEmpty()) {
            return@withContext Result.failure()
        }

        try {
            val database = ApturaDatabase.getInstance(applicationContext)
            val dao = database.mediaItemDao()
            val existingVideos = dao.getAllVideosRaw()
            val existingByUri = existingVideos.associateBy { it.uri }

            val allScanned = mutableListOf<MediaItemEntity>()

            // 1. Scan Android MediaStore if platform permission is granted
            if (hasStorage) {
                val mediaStoreScanner = MediaStoreScanner(applicationContext.contentResolver)
                val mediaStoreItems = mediaStoreScanner.scanAllVideos()
                // MediaStore leaves DURATION null until some app probes a freshly written file.
                // Carry known durations forward (unchanged files) and measure the rest once, so
                // new videos appear complete instead of 0:00, and old ones never regress.
                allScanned.addAll(
                    resolveMissingDurations(applicationContext, mediaStoreItems, existingByUri)
                )
            }

            // 2. Scan each persisted SAF folder tree (differential metadata skip for unchanged files)
            if (persistedFolders.isNotEmpty()) {
                val safScanner = SafTreeScanner(applicationContext)
                for (folderUriStr in persistedFolders) {
                    try {
                        val treeUri = folderUriStr.toUri()
                        val safItems = safScanner.scanTree(treeUri, existingByUri)
                        // Deduplicate SAF items against already-scanned MediaStore items so overlapping folders do not create duplicate cards
                        val deduplicatedSaf = safItems.filterNot { saf ->
                            allScanned.any { existing ->
                                existing.uri == saf.uri || (
                                    existing.sizeBytes > 0L &&
                                        existing.sizeBytes == saf.sizeBytes &&
                                        existing.displayName.equals(saf.displayName, ignoreCase = true)
                                    )
                            }
                        }
                        allScanned.addAll(deduplicatedSaf)
                    } catch (_: Throwable) {
                        // Skip unreadable or revoked SAF tree without failing overall scan
                    }
                }
            }

            // 3. Merge the findings into the library, in one transaction (see persistScan).
            val itemsToUpsert = persistScan(database, allScanned, existingVideos, hasStorage)

            // 4c. Pre-warm persistent thumbnail disk cache for first batch of scanned items
            val preheatItems = itemsToUpsert.take(24)
            for (item in preheatItems) {
                try {
                    ThumbnailRepository.getOrCreateThumbnail(
                        context = applicationContext,
                        uri = item.uri,
                        dateModifiedSec = item.dateModifiedSec
                    )
                } catch (_: Throwable) {}
            }

            Result.success()
        } catch (e: Exception) {
            ApturaDiagnostics.logEvent(
                code = EventCode.EV_006,
                payload = mapOf(
                    "stage" to "MediaScanWorker",
                    "error" to e.javaClass.simpleName
                )
            )
            Result.failure()
        }
    }
}
