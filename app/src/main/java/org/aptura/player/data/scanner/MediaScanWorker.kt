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

            val existingIds = mutableListOf<Long>()
            val itemsToInsert = mutableListOf<MediaItemEntity>()

            // Merge with existing records to preserve user playback progress & timestamps
            for (item in scannedItems) {
                existingIds.add(item.mediaStoreId)
                val existing = dao.getVideoByMediaStoreId(item.mediaStoreId)
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

            // Prune files deleted from disk
            if (existingIds.isNotEmpty()) {
                dao.deleteRemovedVideos(existingIds)
            } else {
                dao.clearAll()
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
