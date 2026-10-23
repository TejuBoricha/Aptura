package org.aptura.player.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Manages persistent, hardware-accelerated, disk-cached thumbnails for video media (audit P1-13).
 *
 * Coil's default [coil3.video.VideoFrameDecoder] does not disk-cache decoded frames for local file/content URIs,
 * resulting in repeated [MediaMetadataRetriever] frame extractions on every LazyGrid scroll or memory eviction.
 *
 * This repository generates thumbnails once, compresses them to lightweight 16:9 JPEGs (~15-30 KB),
 * and persists them in [context.cacheDir]/thumbnails keyed by URI hash and modification timestamp.
 * If the source video file is modified or replaced on disk, the cache key automatically invalidates.
 */
object ThumbnailRepository {

    private const val THUMBNAIL_DIR_NAME = "thumbnails"
    private const val THUMB_WIDTH = 480
    private const val THUMB_HEIGHT = 270
    private const val JPEG_QUALITY = 85
    private const val FRAME_OFFSET_US = 2_000_000L // 2.0s offset avoids initial black title cards
    private const val MAX_FAILED_KEYS = 512

    /** Session-scoped negative cache so un-decodable files do not re-trigger MediaMetadataRetriever on every scroll. */
    private val failedExtractionKeys = java.util.Collections.synchronizedSet( LinkedHashSet<String>() )

    fun getThumbnailDir(context: Context): File {
        val dir = File(context.cacheDir, THUMBNAIL_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @VisibleForTesting
    fun getCacheKey(uri: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    @VisibleForTesting
    fun getThumbnailFileName(uri: String, dateModifiedSec: Long): String {
        val key = getCacheKey(uri)
        return "thumb_${key}_${dateModifiedSec}.jpg"
    }

    fun getThumbnailFile(context: Context, uri: String, dateModifiedSec: Long): File {
        return File(getThumbnailDir(context), getThumbnailFileName(uri, dateModifiedSec))
    }

    /**
     * Fast-path synchronous check. Returns the cached thumbnail [File] if it already exists on disk
     * with positive size, or null if it must be generated.
     */
    fun getCachedThumbnailFile(context: Context, uri: String, dateModifiedSec: Long): File? {
        val file = getThumbnailFile(context, uri, dateModifiedSec)
        return if (file.exists() && file.length() > 0L) file else null
    }

    /**
     * Retrieves an existing cached thumbnail or generates, compresses, and saves a new one to disk.
     * Uses platform hardware acceleration ([android.content.ContentResolver.loadThumbnail]) on API 29+
     * with graceful fallback to [MediaMetadataRetriever].
     */
    suspend fun getOrCreateThumbnail(
        context: Context,
        uri: String,
        dateModifiedSec: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): File? = withContext(dispatcher) {
        val fileName = getThumbnailFileName(uri, dateModifiedSec)
        val targetFile = File(getThumbnailDir(context), fileName)
        if (targetFile.exists() && targetFile.length() > 0L) {
            return@withContext targetFile
        }
        if (failedExtractionKeys.contains(fileName)) {
            return@withContext null
        }

        val bitmap = extractThumbnailBitmap(context, uri)
        if (bitmap == null) {
            synchronized(failedExtractionKeys) {
                if (failedExtractionKeys.size >= MAX_FAILED_KEYS) {
                    failedExtractionKeys.iterator().let { if (it.hasNext()) { it.next(); it.remove() } }
                }
                failedExtractionKeys.add(fileName)
            }
            return@withContext null
        }

        saveBitmapToCache(context, targetFile, bitmap, uri, dateModifiedSec)
    }

    private fun extractThumbnailBitmap(context: Context, uriString: String): Bitmap? {
        val uri = try {
            Uri.parse(uriString)
        } catch (_: Throwable) {
            return null
        }

        // 1. Android 10+ (API 29+) platform hardware-accelerated MediaStore thumbnail generator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uriString.startsWith("content://")) {
            try {
                return context.contentResolver.loadThumbnail(uri, Size(THUMB_WIDTH, THUMB_HEIGHT), null)
            } catch (_: Throwable) {
                // Fall back to MediaMetadataRetriever
            }
        }

        // 2. MediaMetadataRetriever fallback (API < 29 or SAF / file / direct streams)
        val retriever = MediaMetadataRetriever()
        return try {
            if (uriString.startsWith("content://")) {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(uriString)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    FRAME_OFFSET_US,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    THUMB_WIDTH,
                    THUMB_HEIGHT
                )
                    ?: retriever.getScaledFrameAtTime(
                        0L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        THUMB_WIDTH,
                        THUMB_HEIGHT
                    )
                    ?: retriever.frameAtTime
            } else {
                retriever.getFrameAtTime(FRAME_OFFSET_US, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            }
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }
    }

    @VisibleForTesting
    fun saveBitmapToCache(
        context: Context,
        targetFile: File,
        bitmap: Bitmap,
        uri: String,
        dateModifiedSec: Long
    ): File? {
        val parent = targetFile.parentFile ?: getThumbnailDir(context)
        if (!parent.exists()) {
            parent.mkdirs()
        }
        val tempFile = File(parent, "${targetFile.name}.${System.currentTimeMillis()}.tmp")
        try {
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }
            try {
                bitmap.recycle()
            } catch (_: Throwable) {}

            val renamed = tempFile.renameTo(targetFile)
            if (renamed || (tempFile.copyTo(targetFile, overwrite = true).also { tempFile.delete() }.exists())) {
                purgeStaleThumbnails(context, uri, dateModifiedSec)
                return targetFile
            }
        } catch (_: Throwable) {
            tempFile.delete()
        }
        return null
    }

    /**
     * Purges older thumbnails for this URI when the file modification timestamp has changed.
     */
    private fun purgeStaleThumbnails(context: Context, uri: String, currentModifiedSec: Long) {
        try {
            val dir = getThumbnailDir(context)
            val prefix = "thumb_${getCacheKey(uri)}_"
            val currentName = getThumbnailFileName(uri, currentModifiedSec)
            dir.listFiles { _, name -> name.startsWith(prefix) && name != currentName }
                ?.forEach { it.delete() }
        } catch (_: Throwable) {}
    }

    /**
     * Deletes any thumbnail associated with [uriString].
     */
    fun deleteThumbnail(context: Context, uriString: String) {
        try {
            val dir = getThumbnailDir(context)
            val prefix = "thumb_${getCacheKey(uriString)}_"
            dir.listFiles { _, name -> name.startsWith(prefix) }
                ?.forEach { it.delete() }
        } catch (_: Throwable) {}
    }

    /**
     * Returns total disk space used by cached thumbnails and normalized/extracted subtitle cache in bytes.
     */
    fun getCacheSizeBytes(context: Context): Long {
        val dirs = listOf(
            File(context.cacheDir, THUMBNAIL_DIR_NAME),
            File(context.cacheDir, "subtitles_extracted"),
            File(context.cacheDir, "subtitles_normalized")
        )
        return dirs.sumOf { dir ->
            if (!dir.exists()) 0L
            else dir.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
        }
    }

    /**
     * Clears all cached thumbnails and temporary subtitle files from disk and purges memory/disk caches from Coil.
     */
    fun clearCache(context: Context) {
        failedExtractionKeys.clear()
        val dirs = listOf(
            File(context.cacheDir, THUMBNAIL_DIR_NAME),
            File(context.cacheDir, "subtitles_extracted"),
            File(context.cacheDir, "subtitles_normalized")
        )
        dirs.forEach { dir ->
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
        }
        try {
            val imageLoader = coil3.SingletonImageLoader.get(context)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (_: Throwable) {}
    }
}
