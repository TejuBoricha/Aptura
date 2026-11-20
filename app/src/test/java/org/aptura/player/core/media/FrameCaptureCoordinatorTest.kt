package org.aptura.player.core.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FrameCaptureCoordinatorTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun transformBitmap_zeroRotationAndUnmirrored_returnsSameBitmap() {
        val original = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val transformed = FrameCaptureCoordinator.transformBitmap(original, 0, false)
        assertEquals(original, transformed)
    }

    @Test
    fun transformBitmap_90Degrees_swapsDimensions() {
        val original = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val transformed = FrameCaptureCoordinator.transformBitmap(original, 90, false)
        assertEquals(50, transformed.width)
        assertEquals(100, transformed.height)
    }

    @Test
    fun transformBitmap_180Degrees_keepsDimensions() {
        val original = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val transformed = FrameCaptureCoordinator.transformBitmap(original, 180, false)
        assertEquals(100, transformed.width)
        assertEquals(50, transformed.height)
    }

    @Test
    fun transformBitmap_270Degrees_swapsDimensions() {
        val original = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val transformed = FrameCaptureCoordinator.transformBitmap(original, 270, false)
        assertEquals(50, transformed.width)
        assertEquals(100, transformed.height)
    }

    @Test
    fun transformBitmap_mirrored_keepsDimensions() {
        val original = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val transformed = FrameCaptureCoordinator.transformBitmap(original, 0, true)
        assertEquals(100, transformed.width)
        assertEquals(50, transformed.height)
    }

    @Test
    fun saveBitmapToGallery_savesSuccessfullyAndReturnsUri() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val savedUri = FrameCaptureCoordinator.saveBitmapToGallery(context, bitmap, "test_video")
        assertNotNull(savedUri)
    }

    @Test
    fun captureAndSaveFrame_invalidUri_returnsNullSafelyWithoutCrashing() = runTest {
        val invalidUri = Uri.parse("file:///non/existent/video.mp4")
        val result = FrameCaptureCoordinator.captureAndSaveFrame(
            context = context,
            videoUri = invalidUri,
            positionMs = 1000L,
            videoTitle = "missing"
        )
        assertNull(result)
    }
}
