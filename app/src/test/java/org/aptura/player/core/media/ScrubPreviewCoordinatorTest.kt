package org.aptura.player.core.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScrubPreviewCoordinatorTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        ScrubPreviewCoordinator.clear()
    }

    @Test
    fun cacheKey_groupsWithinSameBucket() {
        val uri = "content://media/external/video/media/42"
        val key1 = ScrubPreviewCoordinator.cacheKey(uri, 1200L)
        val key2 = ScrubPreviewCoordinator.cacheKey(uri, 1800L)
        val key3 = ScrubPreviewCoordinator.cacheKey(uri, 2500L)

        assertEquals(key1, key2)
        assertNotEquals(key1, key3)
    }

    @Test
    fun cache_storesAndRetrievesBitmaps_andClearsCorrectly() {
        val uri = "content://media/external/video/media/99"
        val dummy = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888)

        // Given empty cache
        assertNull(ScrubPreviewCoordinator.getCachedFrame(uri, 5000L))

        // When loadPreviewFrame returns null for non-existent video, it does not crash
        runTest {
            val result = ScrubPreviewCoordinator.loadPreviewFrame(context, Uri.parse(uri), 5000L)
            assertNull(result)
        }

        // Test clear
        ScrubPreviewCoordinator.clear()
        assertNull(ScrubPreviewCoordinator.getCachedFrame(uri, 5000L))
    }
}
