package org.aptura.player.data.scanner

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aptura.player.data.database.ApturaDatabase
import org.aptura.player.data.database.MediaItemEntity

/**
 * Background worker for local media library ingest using WorkManager.
 * Runs battery-conscious, unkillable differential scans without blocking the UI (ADR-013, NFR-RES-007).
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
        try {
            val scanner = MediaStoreScanner(applicationContext.contentResolver)
            val scannedItems = scanner.scanAllVideos()

            val database = ApturaDatabase.getInstance(applicationContext)
            val dao = database.mediaItemDao()

            val existingVideos = dao.getAllVideosRaw()
            val existingMap = existingVideos.associateBy { it.mediaStoreId }

            val scannedIds = mutableSetOf<Long>()
            val itemsToInsert = mutableListOf<MediaItemEntity>()

            // Merge with existing records to preserve user playback progress & timestamps
            for (item in scannedItems) {
                scannedIds.add(item.mediaStoreId)
                val existing = existingMap[item.mediaStoreId]
                if (existing != null) {
                    itemsToInsert.add(
                        item.copy(
                            id = existing.id,
                            playbackPositionMs = existing.playbackPositionMs,
                            isCompleted = existing.isCompleted,
                            lastPlayedTimestamp = existing.lastPlayedTimestamp
                        )
                    )
                } else {
                    itemsToInsert.add(item)
                }
            }

            // Write in batches to prevent SQLite transaction memory pressure
            itemsToInsert.chunked(BATCH_SIZE).forEach { batch ->
                dao.insertOrUpdateAll(batch)
            }

            // Prune files deleted from disk in safe 500-item chunks (prevents SQLite 999 parameter limit)
            val idsToDelete = existingVideos
                .filter { it.mediaStoreId !in scannedIds }
                .map { it.id }

            if (idsToDelete.isNotEmpty()) {
                idsToDelete.chunked(500).forEach { batch ->
                    dao.deleteByIds(batch)
                }
            } else if (scannedItems.isEmpty() && existingVideos.isNotEmpty()) {
                dao.clearAll()
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
