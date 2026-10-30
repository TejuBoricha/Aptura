package org.aptura.player.ui.library

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import kotlinx.coroutines.Dispatchers
import org.aptura.player.core.media.ThumbnailBitmapCache
import org.aptura.player.core.media.ThumbnailRepository
import org.aptura.player.theme.ApturaTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A thumbnail that is already decoded must be in the very first frame (a returning grid used to show an empty tile for
 * a frame or two per cell, which read as flicker); one that is not must stand in with the film glyph and fill in later.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoThumbnailTest {

    @get:Rule
    val composeRule = createComposeRule()

    @After
    fun clear() = ThumbnailBitmapCache.clear()

    private fun show(uri: String, modified: Long) {
        composeRule.setContent {
            CompositionLocalProvider(LocalThumbnailDecodeContext provides Dispatchers.Main.immediate) {
                ApturaTheme { VideoThumbnail(uri, modified, "Arrival") }
            }
        }
    }

    @Test
    fun aPictureAlreadyInMemory_isOnScreenInTheFirstFrame() {
        val key = ThumbnailRepository.getThumbnailFileName("content://video/arrival", 100L)
        ThumbnailBitmapCache.put(key, Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888).asImageBitmap())

        show("content://video/arrival", 100L)

        // No waiting for idle: the picture is part of the first composition.
        composeRule.onNodeWithContentDescription("Arrival").assertExists()
    }

    @Test
    fun aPictureThatCannotBeRead_leavesOnlyTheGlyph_andNeverAnImage() {
        show("content://video/not-a-real-video", 100L)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Arrival").assertDoesNotExist()
    }
}
