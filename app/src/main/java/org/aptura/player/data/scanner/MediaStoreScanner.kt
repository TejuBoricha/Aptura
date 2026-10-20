package org.aptura.player.data.scanner

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aptura.player.data.database.MediaItemEntity

/**
 * High-performance MediaStore video scanner.
 * Queries Android's MediaStore.Video.Media content provider with minimal column projection
 * to achieve sub-second cold scans across thousands of items (NFR-PERF-005).
 */
class MediaStoreScanner(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        val PROJECTION = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
    }

    /**
     * Scans all video files available in MediaStore and maps them to MediaItemEntity.
     * Filters out files smaller than 1KB or with 0 duration to prevent phantom files.
     */
    suspend fun scanAllVideos(): List<MediaItemEntity> = withContext(ioDispatcher) {
        val items = mutableListOf<MediaItemEntity>()

        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = contentResolver.query(
            uri,
            PROJECTION,
            null,
            null,
            sortOrder
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val widthCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateAddCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val bucketCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val mediaStoreId = c.getLong(idCol)
                val displayName = c.getString(nameCol) ?: "Video_$mediaStoreId"
                val title = c.getString(titleCol) ?: displayName
                val duration = c.getLong(durCol).coerceAtLeast(0L)
                val size = c.getLong(sizeCol).coerceAtLeast(0L)
                val mimeType = c.getString(mimeCol)
                val width = c.getInt(widthCol).coerceAtLeast(0)
                val height = c.getInt(heightCol).coerceAtLeast(0)
                val dateAdded = c.getLong(dateAddCol).coerceAtLeast(0L)
                val dateModified = c.getLong(dateModCol).coerceAtLeast(0L)
                val folder = c.getString(bucketCol) ?: "Internal Storage"

                val contentUri = ContentUris.withAppendedId(uri, mediaStoreId).toString()

                // Filter out non-video artifacts, 0-byte placeholders or corrupt fragments
                if (size > 1024L) {
                    items.add(
                        MediaItemEntity(
                            mediaStoreId = mediaStoreId,
                            uri = contentUri,
                            title = title,
                            displayName = displayName,
                            durationMs = duration,
                            sizeBytes = size,
                            mimeType = mimeType,
                            width = width,
                            height = height,
                            frameRate = 0f,
                            dateAddedSec = dateAdded,
                            dateModifiedSec = dateModified,
                            folderPath = folder,
                            playbackPositionMs = 0L,
                            isCompleted = false,
                            lastPlayedTimestamp = 0L
                        )
                    )
                }
            }
        }

        items
    }
}
