package org.aptura.player.data.scanner

import android.content.Context
import android.net.Uri
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
                allScanned.addAll(mediaStoreScanner.scanAllVideos())
            }

            // 2. Scan each persisted SAF folder tree (differential metadata skip for unchanged files)
            if (persistedFolders.isNotEmpty()) {
                val safScanner = SafTreeScanner(applicationContext)
                for (folderUriStr in persistedFolders) {
                    try {
                        val treeUri = Uri.parse(folderUriStr)
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

            // 3. Reconcile scanned items against existing database items
            val reconciliation = FileReconciliationEngine.reconcile(
                scannedItems = allScanned,
                existingItems = existingVideos
            )

            // Separate genuinely missing external/progress-bearing items from deleted internal MediaStore files with zero user state
            val (staleInternalToDelete, itemsToUpsert) = reconciliation.itemsToUpsert.partition { item ->
                item.isMissing &&
                    hasStorage &&
                    item.uri.startsWith("content://media/") &&
                    item.playbackPositionMs == 0L &&
                    !item.isCompleted &&
                    item.customTitle.isNullOrBlank()
            }
            val allIdsToDelete = reconciliation.idsToDelete +
                staleInternalToDelete.mapNotNull { it.id.takeIf { id -> id > 0L } } +
                existingVideos.filter { item ->
                    item.isMissing &&
                        hasStorage &&
                        item.uri.startsWith("content://media/") &&
                        item.playbackPositionMs == 0L &&
                        !item.isCompleted &&
                        item.customTitle.isNullOrBlank() &&
                        allScanned.none { scanned -> scanned.uri == item.uri || scanned.mediaStoreId == item.mediaStoreId }
                }.mapNotNull { it.id.takeIf { id -> id > 0L } }

            // 4. Atomic transaction for database upsert, bookmark remapping, and obsolete deletion
            database.withTransaction {
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
            }

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
