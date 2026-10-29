package org.aptura.player.core.media

import android.content.ComponentCallbacks2
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Decoded thumbnails are what lets a returning grid paint its pictures in the first frame. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThumbnailBitmapCacheTest {

    @After
    fun clear() = ThumbnailBitmapCache.clear()

    private fun bitmap(width: Int = 4, height: Int = 4): ImageBitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()

    @Test
    fun aStoredPicture_isReturned_andAMissingOneIsNot() {
        val picture = bitmap()
        ThumbnailBitmapCache.put("thumb_a_1.jpg", picture)

        assertEquals(picture, ThumbnailBitmapCache["thumb_a_1.jpg"])
        assertNull(ThumbnailBitmapCache["thumb_b_1.jpg"])
    }

    @Test
    fun aNewerFileForTheSameVideo_isADifferentKey_soAStalePictureCannotBeShown() {
        val key1 = ThumbnailRepository.getThumbnailFileName("content://video/1", 1000L)
        val key2 = ThumbnailRepository.getThumbnailFileName("content://video/1", 2000L)
        ThumbnailBitmapCache.put(key1, bitmap())

        assertNotNull(ThumbnailBitmapCache[key1])
        assertNull("the replaced file must not be served the old picture", ThumbnailBitmapCache[key2])
    }

    @Test
    fun removingAVideo_forgetsEverySizeOfItsPicture_andOnlyThat() {
        val mine1 = ThumbnailRepository.getThumbnailFileName("content://video/1", 1000L)
        val mine2 = ThumbnailRepository.getThumbnailFileName("content://video/1", 2000L)
        val other = ThumbnailRepository.getThumbnailFileName("content://video/2", 1000L)
        listOf(mine1, mine2, other).forEach { ThumbnailBitmapCache.put(it, bitmap()) }

        ThumbnailBitmapCache.removeForVideo("content://video/1")

        assertNull(ThumbnailBitmapCache[mine1])
        assertNull(ThumbnailBitmapCache[mine2])
        assertNotNull(ThumbnailBitmapCache[other])
    }

    @Test
    fun theCacheIsBoundedByBytes_andDropsTheLeastRecentlyUsed() {
        // Each 1000 x 1000 picture is 4 MB; the budget is at most 48 MB, so 40 of them cannot all fit.
        repeat(40) { index -> ThumbnailBitmapCache.put("thumb_$index.jpg", bitmap(1000, 1000)) }

        assertNull("the oldest picture was evicted", ThumbnailBitmapCache["thumb_0.jpg"])
        assertNotNull("the newest picture is kept", ThumbnailBitmapCache["thumb_39.jpg"])
        assertTrue(ThumbnailBitmapCache.maxBytes() <= 48 * 1024 * 1024)
    }

    @Test
    fun whenAndroidAsksForMemory_theCacheGivesBackAccordingToHowTightItIs() {
        repeat(4) { ThumbnailBitmapCache.put("thumb_$it.jpg", bitmap(1000, 1000)) }

        ThumbnailBitmapCache.trim(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE)
        assertNotNull("a moderate squeeze keeps the pictures", ThumbnailBitmapCache["thumb_3.jpg"])

        ThumbnailBitmapCache.trim(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        assertNull("in the background everything goes", ThumbnailBitmapCache["thumb_3.jpg"])
    }
}
