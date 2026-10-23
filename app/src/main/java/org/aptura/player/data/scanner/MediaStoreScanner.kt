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
        val selection = "${MediaStore.Video.Media.DURATION} > 0 " +
            "AND ${MediaStore.Video.Media.SIZE} > 1024 " +
            "AND (${MediaStore.Video.Media.MIME_TYPE} LIKE 'video/%' " +
            "OR ${MediaStore.Video.Media.MIME_TYPE} IN ('application/x-matroska', 'application/ogg', 'application/mp4', 'application/vnd.rn-realmedia'))"

        val cursor: Cursor? = contentResolver.query(
            uri,
            PROJECTION,
            selection,
            null,
            sortOrder
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndex(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val titleCol = c.getColumnIndex(MediaStore.Video.Media.TITLE)
            val durCol = c.getColumnIndex(MediaStore.Video.Media.DURATION)
            val sizeCol = c.getColumnIndex(MediaStore.Video.Media.SIZE)
            val mimeCol = c.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
            val widthCol = c.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightCol = c.getColumnIndex(MediaStore.Video.Media.HEIGHT)
            val dateAddCol = c.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
            val dateModCol = c.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
            val bucketCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val mediaStoreId = if (idCol >= 0) c.getLong(idCol) else 0L
                val displayName = if (nameCol >= 0 && !c.isNull(nameCol)) c.getString(nameCol) else "Video_$mediaStoreId"
                val title = if (titleCol >= 0 && !c.isNull(titleCol)) c.getString(titleCol) else displayName
                val duration = (if (durCol >= 0 && !c.isNull(durCol)) c.getLong(durCol) else 0L).coerceAtLeast(0L)
                val size = (if (sizeCol >= 0 && !c.isNull(sizeCol)) c.getLong(sizeCol) else 0L).coerceAtLeast(0L)
                val mimeType = if (mimeCol >= 0 && !c.isNull(mimeCol)) c.getString(mimeCol) else null
                val width = (if (widthCol >= 0 && !c.isNull(widthCol)) c.getInt(widthCol) else 0).coerceAtLeast(0)
                val height = (if (heightCol >= 0 && !c.isNull(heightCol)) c.getInt(heightCol) else 0).coerceAtLeast(0)
                val dateAdded = (if (dateAddCol >= 0 && !c.isNull(dateAddCol)) c.getLong(dateAddCol) else 0L).coerceAtLeast(0L)
                val dateModified = (if (dateModCol >= 0 && !c.isNull(dateModCol)) c.getLong(dateModCol) else 0L).coerceAtLeast(0L)
                val folder = if (bucketCol >= 0 && !c.isNull(bucketCol)) c.getString(bucketCol) else "Internal Storage"

                val contentUri = ContentUris.withAppendedId(uri, mediaStoreId).toString()

                // Filter out non-video artifacts, 0-byte placeholders or corrupt fragments
                if (size > 1024L && duration > 0L) {
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
