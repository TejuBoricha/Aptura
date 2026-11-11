@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import org.aptura.player.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Direct unit tests for [ApturaPlaybackService] covering MediaSession initialization, session activity wiring,
 * and foreground service lifecycle guarantees under task removal (audit QA-04).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApturaPlaybackServiceTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        PlayerManager.release()
    }

    @Test
    fun onCreate_initializesMediaSession_withProcessPlayer_andShowPlayerActivityIntent() {
        val controller = PlayerManager.getOrCreate(app)
        val serviceController = Robolectric.buildService(ApturaPlaybackService::class.java).create()
        val service = serviceController.get()

        val session = service.currentMediaSession
        assertNotNull("mediaSession must be initialized on onCreate", session)
        assertSame("mediaSession must wrap the active process player", controller.player, session?.player)

        val sessionActivity = session?.sessionActivity
        assertNotNull("sessionActivity PendingIntent must not be null", sessionActivity)

        val shadowPendingIntent = shadowOf(sessionActivity)
        val intent = shadowPendingIntent.savedIntent
        assertNotNull("PendingIntent must contain a saved intent", intent)
        assertEquals(MainActivity.ACTION_SHOW_PLAYER, intent.action)
        assertEquals(MainActivity::class.java.name, intent.component?.className)

        serviceController.destroy()
    }

    @Test
    fun onTaskRemoved_whilePlaybackIsActive_keepsServiceAlive_andPreservesPlayer() {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer
        player.setMediaItem(MediaItem.fromUri("content://media/test_active.mp4"))
        player.playWhenReady = true

        val serviceController = Robolectric.buildService(ApturaPlaybackService::class.java).create()
        val service = serviceController.get()

        service.onTaskRemoved(null)

        assertFalse(
            "service must not call stopSelf when swiped away while playback is active",
            shadowOf(service).isStoppedBySelf
        )

        serviceController.destroy()

        assertFalse("player must survive service destruction when task removal did not request release", player.isReleased)
        assertNotNull("process active controller must remain valid", PlayerManager.activeController)
    }

    @Test
    fun onTaskRemoved_whenPlaybackIsPaused_stopsSelf_andReleasesPlayerOnDestroy() {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer
        player.setMediaItem(MediaItem.fromUri("content://media/test_paused.mp4"))
        player.playWhenReady = false

        val serviceController = Robolectric.buildService(ApturaPlaybackService::class.java).create()
        val service = serviceController.get()

        service.onTaskRemoved(null)

        assertTrue(
            "service must call stopSelf when swiped away while playback is paused",
            shadowOf(service).isStoppedBySelf
        )

        serviceController.destroy()

        assertTrue("player must be released on destroy when task was swiped away while paused", player.isReleased)
        assertNull("PlayerManager active controller must be cleared", PlayerManager.activeController)
    }

    @Test
    fun onTaskRemoved_whenQueueIsEmpty_stopsSelf_andReleasesPlayerOnDestroy() {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer
        player.clearMediaItems()
        player.playWhenReady = true // playWhenReady is true, but queue is empty

        val serviceController = Robolectric.buildService(ApturaPlaybackService::class.java).create()
        val service = serviceController.get()

        service.onTaskRemoved(null)

        assertTrue(
            "service must call stopSelf when swiped away with an empty queue",
            shadowOf(service).isStoppedBySelf
        )

        serviceController.destroy()

        assertTrue("player must be released on destroy when task was swiped away with empty queue", player.isReleased)
        assertNull("PlayerManager active controller must be cleared", PlayerManager.activeController)
    }

    @Test
    fun onDestroy_withoutTaskRemoved_clearsMediaSession_butPreservesPlayerForApp() {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer

        val serviceController = Robolectric.buildService(ApturaPlaybackService::class.java).create()
        val service = serviceController.get()

        val session = service.currentMediaSession
        assertNotNull(session)

        serviceController.destroy()

        assertNull("mediaSession must be null after onDestroy", service.currentMediaSession)
        assertFalse("player must outlive service destruction when not dismissed by task removal", player.isReleased)
        assertSame(controller, PlayerManager.activeController)
    }
}
