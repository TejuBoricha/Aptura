package org.aptura.player.core.media

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThumbnailRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ThumbnailRepository.clearCache(context)
    }

    @Test
    fun testCacheKey_isDeterministicAndCollisionResistant() {
        val uri1 = "content://media/external/video/media/101"
        val uri2 = "content://media/external/video/media/102"

        val key1a = ThumbnailRepository.getCacheKey(uri1)
        val key1b = ThumbnailRepository.getCacheKey(uri1)
        val key2 = ThumbnailRepository.getCacheKey(uri2)

        assertEquals("Same URI must produce identical cache keys", key1a, key1b)
        assertNotEquals("Distinct URIs must produce distinct cache keys", key1a, key2)
        assertEquals("Cache key length must be 16 characters", 16, key1a.length)
    }

    @Test
    fun testThumbnailFileName_formatsWithDateModified() {
        val uri = "content://media/external/video/media/42"
        val dateModified = 1729000000L
        val fileName = ThumbnailRepository.getThumbnailFileName(uri, dateModified)

        assertTrue(fileName.startsWith("thumb_"))
        assertTrue(fileName.endsWith("_1729000000.jpg"))
    }

    @Test
    fun testGetCachedThumbnailFile_returnsNullWhenMissing() {
        val uri = "content://media/external/video/media/999"
        val cached = ThumbnailRepository.getCachedThumbnailFile(context, uri, 1000L)
        assertNull("Missing thumbnail must return null for fast-path check", cached)
    }

    @Test
    fun testSaveBitmapToCache_persistsFileAndAllowsFastPathRetrieval() {
        val uri = "content://media/external/video/media/555"
        val dateModified = 1500L
        val targetFile = ThumbnailRepository.getThumbnailFile(context, uri, dateModified)

        assertFalse(targetFile.exists())

        val bitmap = Bitmap.createBitmap(32, 18, Bitmap.Config.ARGB_8888)
        val savedFile = ThumbnailRepository.saveBitmapToCache(
            context = context,
            targetFile = targetFile,
            bitmap = bitmap,
            uri = uri,
            dateModifiedSec = dateModified
        )

        assertNotNull("Saved file must not be null", savedFile)
        assertTrue("Target file must exist on disk", targetFile.exists())
        assertTrue("Saved thumbnail must have positive length", targetFile.length() > 0L)

        val retrieved = ThumbnailRepository.getCachedThumbnailFile(context, uri, dateModified)
        assertNotNull("getCachedThumbnailFile must return saved file", retrieved)
        assertEquals(targetFile.absolutePath, retrieved?.absolutePath)
    }

    @Test
    fun testCacheInvalidation_whenDateModifiedChanges() {
        val uri = "content://media/external/video/media/777"
        val oldDate = 1000L
        val newDate = 2000L

        val oldTargetFile = ThumbnailRepository.getThumbnailFile(context, uri, oldDate)
        val newTargetFile = ThumbnailRepository.getThumbnailFile(context, uri, newDate)

        // Save thumbnail with old modification date
        val bitmap1 = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        ThumbnailRepository.saveBitmapToCache(context, oldTargetFile, bitmap1, uri, oldDate)
        assertTrue(oldTargetFile.exists())

        // Save thumbnail with new modification date (simulates edited/replaced video)
        val bitmap2 = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        ThumbnailRepository.saveBitmapToCache(context, newTargetFile, bitmap2, uri, newDate)

        assertTrue("New thumbnail must exist", newTargetFile.exists())
        assertFalse("Stale thumbnail with older timestamp must be purged", oldTargetFile.exists())
    }

    @Test
    fun testCacheSizeBytes_andClearCache() {
        val uri1 = "content://media/external/video/media/1"
        val uri2 = "content://media/external/video/media/2"

        val file1 = ThumbnailRepository.getThumbnailFile(context, uri1, 100L)
        val file2 = ThumbnailRepository.getThumbnailFile(context, uri2, 100L)

        val b1 = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val b2 = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)

        ThumbnailRepository.saveBitmapToCache(context, file1, b1, uri1, 100L)
        ThumbnailRepository.saveBitmapToCache(context, file2, b2, uri2, 100L)

        val totalSize = ThumbnailRepository.getCacheSizeBytes(context)
        assertEquals(file1.length() + file2.length(), totalSize)
        assertTrue(totalSize > 0L)

        // Clear cache
        ThumbnailRepository.clearCache(context)
        assertEquals(0L, ThumbnailRepository.getCacheSizeBytes(context))
        assertFalse(file1.exists())
        assertFalse(file2.exists())
    }

    @Test
    fun testDeleteThumbnail_removesOnlyTargetUri() {
        val uri1 = "content://media/external/video/media/10"
        val uri2 = "content://media/external/video/media/20"

        val file1 = ThumbnailRepository.getThumbnailFile(context, uri1, 500L)
        val file2 = ThumbnailRepository.getThumbnailFile(context, uri2, 500L)

        val b1 = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val b2 = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)

        ThumbnailRepository.saveBitmapToCache(context, file1, b1, uri1, 500L)
        ThumbnailRepository.saveBitmapToCache(context, file2, b2, uri2, 500L)

        assertTrue(file1.exists())
        assertTrue(file2.exists())

        ThumbnailRepository.deleteThumbnail(context, uri1)

        assertFalse("Target thumbnail must be deleted", file1.exists())
        assertTrue("Other thumbnail must remain intact", file2.exists())
    }
}
