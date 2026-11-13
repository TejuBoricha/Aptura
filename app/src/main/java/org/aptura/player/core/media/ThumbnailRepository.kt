package org.aptura.player.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.os.CancellationSignal
import android.util.LruCache
import android.util.Size
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

/**
 * Manages persistent, hardware-accelerated, disk-cached thumbnails for video media (audit P1-13, PERF-04).
 *
 * Coil's default [coil3.video.VideoFrameDecoder] does not disk-cache decoded frames for local file/content URIs,
 * resulting in repeated [MediaMetadataRetriever] frame extractions on every LazyGrid scroll or memory eviction.
 *
 * This repository generates thumbnails once, compresses them to lightweight 16:9 JPEGs (~15-30 KB),
 * and persists them in [context.cacheDir]/thumbnails keyed by URI hash and modification timestamp.
 * If the source video file is modified or replaced on disk, the cache key automatically invalidates.
 * Concurrency is bounded via [limitedParallelism], extraction is guarded by a timeout, and disk usage
 * is bounded via LRU eviction.
 */
object ThumbnailRepository {

    private const val THUMBNAIL_DIR_NAME = "thumbnails"
    private const val THUMB_WIDTH = 480
    private const val THUMB_HEIGHT = 270
    private const val JPEG_QUALITY = 85
    private const val FRAME_OFFSET_US = 2_000_000L // 2.0s offset avoids initial black title cards
    private const val MAX_FAILED_KEYS = 512
    private const val URI_KEY_CACHE_SIZE = 4096
    private const val TRIM_INTERVAL_SAVES = 16

    const val MAX_PARALLEL_EXTRACTIONS = 3
    const val DEFAULT_EXTRACTION_TIMEOUT_MS = 5_000L
    const val MAX_DISK_CACHE_BYTES = 50L * 1024L * 1024L // 50 MB LRU cap

    @OptIn(ExperimentalCoroutinesApi::class)
    val defaultExtractionDispatcher: CoroutineDispatcher =
        Dispatchers.IO.limitedParallelism(MAX_PARALLEL_EXTRACTIONS)

    private val HEX_DIGITS = "0123456789abcdef".toCharArray()
    private val sha256Digest: ThreadLocal<MessageDigest> = ThreadLocal.withInitial {
        MessageDigest.getInstance("SHA-256")
    }
    private val uriKeyCache = LruCache<String, String>(URI_KEY_CACHE_SIZE)
    private val saveCount = AtomicInteger(0)

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
        uriKeyCache.get(uri)?.let { return it }
        val digest = sha256Digest.get()!!
        digest.reset()
        val hash = digest.digest(uri.toByteArray(Charsets.UTF_8))
        val chars = CharArray(16)
        for (i in 0 until 8) {
            val b = hash[i].toInt() and 0xFF
            chars[i * 2] = HEX_DIGITS[b ushr 4]
            chars[i * 2 + 1] = HEX_DIGITS[b and 0x0F]
        }
        return String(chars).also { uriKeyCache.put(uri, it) }
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
        return if (file.exists() && file.length() > 0L) {
            file
        } else {
            null
        }
    }

    /**
     * Retrieves an existing cached thumbnail or generates, compresses, and saves a new one to disk.
     * Uses platform hardware acceleration ([android.content.ContentResolver.loadThumbnail]) on API 29+
     * with graceful fallback to [MediaMetadataRetriever].
     *
     * Guarded by [defaultExtractionDispatcher] (`limitedParallelism(3)`), [timeoutMs] (`withTimeoutOrNull`),
     * and [trimDiskCacheToLimit] LRU eviction (audit PERF-04).
     */
    suspend fun getOrCreateThumbnail(
        context: Context,
        uri: String,
        dateModifiedSec: Long,
        dispatcher: CoroutineDispatcher = defaultExtractionDispatcher,
        timeoutMs: Long = DEFAULT_EXTRACTION_TIMEOUT_MS,
        extractor: suspend (Context, String) -> Bitmap? = { ctx, u -> extractThumbnailBitmap(ctx, u) }
    ): File? = withContext(dispatcher) {
        val fileName = getThumbnailFileName(uri, dateModifiedSec)
        val targetFile = File(getThumbnailDir(context), fileName)
        if (targetFile.exists() && targetFile.length() > 0L) {
            return@withContext targetFile
        }
        if (failedExtractionKeys.contains(fileName)) {
            return@withContext null
        }

        val bitmap = withTimeoutOrNull(timeoutMs) {
            extractor(context, uri)
        }
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

    @VisibleForTesting
    fun isMarkedFailed(uri: String, dateModifiedSec: Long): Boolean =
        failedExtractionKeys.contains(getThumbnailFileName(uri, dateModifiedSec))

    private suspend fun extractThumbnailBitmap(context: Context, uriString: String): Bitmap? {
        val uri = try {
            uriString.toUri()
        } catch (_: Throwable) {
            return null
        }

        // 1. Android 10+ (API 29+) platform hardware-accelerated MediaStore thumbnail generator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uriString.startsWith("content://")) {
            val signal = CancellationSignal()
            val handle = coroutineContext[Job]?.invokeOnCompletion { signal.cancel() }
            try {
                return context.contentResolver.loadThumbnail(uri, Size(THUMB_WIDTH, THUMB_HEIGHT), signal)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                coroutineContext.ensureActive()
                // Fall back to MediaMetadataRetriever when active and loadThumbnail did not return a frame
            } finally {
                handle?.dispose()
            }
        }

        coroutineContext.ensureActive()

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
                if (saveCount.incrementAndGet() % TRIM_INTERVAL_SAVES == 0) {
                    trimDiskCacheToLimit(parent, MAX_DISK_CACHE_BYTES, preserveFile = targetFile)
                }
                return targetFile
            }
        } catch (_: Throwable) {
            tempFile.delete()
        }
        return null
    }

    /**
     * Enforces an LRU disk cache cap ([maxBytes]) on [dir] by evicting the least-recently-modified
     * thumbnail files first while always preserving [preserveFile] (audit PERF-04).
     */
    @VisibleForTesting
    fun trimDiskCacheToLimit(
        dir: File,
        maxBytes: Long = MAX_DISK_CACHE_BYTES,
        preserveFile: File? = null
    ) {
        try {
            if (!dir.exists() || maxBytes <= 0L) return
            val files = dir.listFiles { file ->
                file.isFile && file.name.startsWith("thumb_") && file.name.endsWith(".jpg")
            } ?: return
            var totalBytes = files.sumOf { it.length() }
            if (totalBytes <= maxBytes) return

            val sortedOldestFirst = files.sortedWith(
                compareBy<File> { it.lastModified() }.thenBy { it.name }
            )
            for (candidate in sortedOldestFirst) {
                if (totalBytes <= maxBytes) break
                if (preserveFile != null && candidate.absolutePath == preserveFile.absolutePath) continue
                val len = candidate.length()
                if (candidate.delete()) {
                    totalBytes -= len
                }
            }
        } catch (_: Throwable) {}
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
        ThumbnailBitmapCache.removeForVideo(uriString)
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
            File(context.cacheDir, SubtitleZipExtractor.EXTRACT_DIR_NAME),
            File(context.cacheDir, "subtitles_normalized")
        )
        // Walks into sub-folders: a zip import keeps its subtitle in a folder of its own under subtitles_extracted.
        return dirs.sumOf { dir ->
            if (!dir.exists()) 0L
            else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
    }

    /**
     * Clears all cached thumbnails and temporary subtitle files from disk and purges memory/disk caches from Coil.
     */
    fun clearCache(context: Context) {
        saveCount.set(0)
        failedExtractionKeys.clear()
        ThumbnailBitmapCache.clear()
        val dirs = listOf(
            File(context.cacheDir, THUMBNAIL_DIR_NAME),
            File(context.cacheDir, SubtitleZipExtractor.EXTRACT_DIR_NAME),
            File(context.cacheDir, "subtitles_normalized")
        )
        dirs.forEach { dir ->
            if (dir.exists()) {
                // Recursive: a plain delete() fails on a zip import's folder, which is not empty.
                dir.listFiles()?.forEach { it.deleteRecursively() }
            }
        }
        try {
            val imageLoader = coil3.SingletonImageLoader.get(context)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (_: Throwable) {}
    }
}
