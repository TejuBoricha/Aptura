package org.aptura.player.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coordinates in-memory keyframe thumbnail previews for seek-bar scrubbing (FEAT-09).
 * Caches extracted thumbnails by video URI and time bucket to ensure instantaneous,
 * zero-jank frame rendering while scrubbing.
 */
object ScrubPreviewCoordinator {

    private const val PREVIEW_WIDTH = 160
    private const val PREVIEW_HEIGHT = 90
    private const val BUCKET_MS = 2000L
    private const val MAX_CACHE_ENTRIES = 40

    private val cache = LruCache<String, Bitmap>(MAX_CACHE_ENTRIES)

    @VisibleForTesting
    fun cacheKey(uri: String, positionMs: Long): String {
        val bucket = (positionMs.coerceAtLeast(0L) / BUCKET_MS) * BUCKET_MS
        return "${uri}_$bucket"
    }

    fun getCachedFrame(uri: String, positionMs: Long): Bitmap? {
        return cache.get(cacheKey(uri, positionMs))
    }

    suspend fun loadPreviewFrame(
        context: Context,
        videoUri: Uri,
        positionMs: Long
    ): Bitmap? = withContext(Dispatchers.IO) {
        val uriString = videoUri.toString()
        val key = cacheKey(uriString, positionMs)
        cache.get(key)?.let { return@withContext it }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val timeUs = positionMs.coerceAtLeast(0L) * 1000L
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    PREVIEW_WIDTH,
                    PREVIEW_HEIGHT
                ) ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } else {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } ?: return@withContext null

            cache.put(key, bitmap)
            bitmap
        } catch (_: Throwable) {
            null
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }

    fun clear() {
        cache.evictAll()
    }
}
