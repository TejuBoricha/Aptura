package org.aptura.player.core.media

import android.content.ComponentCallbacks2
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decoded thumbnails, kept in memory so a grid that comes back on screen (closing the player, a rotation, scrolling
 * back up) paints its pictures in the very first frame.
 *
 * The images are already cached on disk as small JPEGs, but decoding even a 480 x 270 JPEG is work that cannot happen
 * while composing, so without this every returning cell showed an empty tile for a frame or two and then popped its
 * picture in: the whole grid appeared to flicker. Keys are the thumbnail file names, which already carry the video's
 * modification time, so a replaced file can never show a stale picture.
 */
internal object ThumbnailBitmapCache {

    private const val MIN_BYTES = 8 * 1024 * 1024
    private const val MAX_BYTES = 48 * 1024 * 1024

    private val cache = object : LruCache<String, ImageBitmap>(budgetBytes()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * BYTES_PER_PIXEL
    }

    operator fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }

    /** Forgets every size of one video's thumbnail, after the video or its thumbnail was deleted. */
    fun removeForVideo(uri: String) {
        val prefix = "thumb_${ThumbnailRepository.getCacheKey(uri)}_"
        cache.snapshot().keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    fun clear() {
        cache.evictAll()
    }

    /** Gives memory back when Android asks: all of it once the app is in the background, half when it is tight. */
    fun trim(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> cache.evictAll()
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> cache.trimToSize(cache.maxSize() / 2)
        }
    }

    /** Test hook: how many bytes the cache may hold. */
    internal fun maxBytes(): Int = cache.maxSize()

    private const val BYTES_PER_PIXEL = 4

    private fun budgetBytes(): Int = (Runtime.getRuntime().maxMemory() / 16)
        .coerceIn(MIN_BYTES.toLong(), MAX_BYTES.toLong())
        .toInt()
}
