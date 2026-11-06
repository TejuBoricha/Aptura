@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit FUNC-02: an imported subtitle never appeared. ExoPlayer replaces a playlist item "in place" when the item's
 * current media source says it can take the new one, and a change to subtitles alone said yes, so no subtitle source
 * was ever built. These tests ask the production [DefaultMediaSourceFactory], the class the player uses, exactly that
 * question. No media needs to be decoded.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaItemSubtitlesTest {

    private val factory = DefaultMediaSourceFactory(ApplicationProvider.getApplicationContext<android.content.Context>())

    private fun plain() = MediaItem.Builder().setUri("file:///storage/movie.mp4").setMediaId("file:///storage/movie.mp4").build()

    private fun sub(name: String, flags: Int = 0) =
        MediaItem.SubtitleConfiguration.Builder(Uri.parse("file:///storage/$name"))
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage("en")
            .setLabel(name)
            .setSelectionFlags(flags)
            .build()

    @Test
    fun addingASubtitleToAnItemWithoutOneForcesARebuild() {
        val source = factory.createMediaSource(plain())
        val withSubtitle = plain().withSubtitleConfigurations(listOf(sub("a.srt")))

        assertFalse("the new subtitle would be dropped", source.canUpdateMediaItem(withSubtitle))
    }

    @Test
    fun addingASecondSubtitleForcesARebuild() {
        val one = plain().withSubtitleConfigurations(listOf(sub("a.srt")))
        val two = plain().withSubtitleConfigurations(listOf(sub("a.srt"), sub("b.srt")))
        val source = factory.createMediaSource(one)

        assertFalse(source.canUpdateMediaItem(two))
    }

    @Test
    fun removingSubtitlesForcesARebuild() {
        val withSubtitle = plain().withSubtitleConfigurations(listOf(sub("a.srt")))
        val source = factory.createMediaSource(withSubtitle)

        assertFalse(source.canUpdateMediaItem(plain().withSubtitleConfigurations(emptyList())))
    }

    @Test
    fun theSameSubtitlesAreAnInPlaceUpdate() {
        val one = plain().withSubtitleConfigurations(listOf(sub("a.srt")))
        val again = plain().withSubtitleConfigurations(listOf(sub("a.srt")))
        val source = factory.createMediaSource(one)

        assertTrue("nothing changed, so there is nothing to rebuild", source.canUpdateMediaItem(again))
    }

    @Test
    fun theSubtitleItselfIsStillConfiguredOnTheItem() {
        val item = plain().withSubtitleConfigurations(listOf(sub("a.srt", flags = 1)))
        val configs = item.localConfiguration!!.subtitleConfigurations

        assertEquals(1, configs.size)
        assertEquals("a.srt", configs[0].label)
        assertEquals(1, configs[0].selectionFlags)
        assertEquals(MimeTypes.APPLICATION_SUBRIP, configs[0].mimeType)
    }

    @Test
    fun keyIsStableForTheSameSubtitlesAndDifferentOtherwise() {
        assertNull(subtitleRebuildKey(emptyList()))
        assertEquals(subtitleRebuildKey(listOf(sub("a.srt"))), subtitleRebuildKey(listOf(sub("a.srt"))))
        assertNotEquals(subtitleRebuildKey(listOf(sub("a.srt"))), subtitleRebuildKey(listOf(sub("b.srt"))))
        assertNotEquals(subtitleRebuildKey(listOf(sub("a.srt", 0))), subtitleRebuildKey(listOf(sub("a.srt", 1))))
    }
}
