@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.app.Application
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
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
 * Regression for audit finding STAB-01: Android stops the idle [ApturaPlaybackService] after playback has been paused
 * in the background for a while. The activity, its ViewModel and `Navigation.kt` keep the controller they got from
 * [PlayerManager] for as long as the process lives, so the player has to outlive the service. When the service used
 * to release it, every later Play did nothing and new videos failed until the process was killed.
 *
 * These run a real service and a real ExoPlayer under Robolectric, because a fake player cannot show that the real
 * one was released.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaybackServiceLifecycleTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        // PlayerManager is a process-wide singleton: leave nothing behind for the next test.
        PlayerManager.release()
    }

    /** Services the app started so far, oldest first, and forgets them so the next check sees only new starts. */
    private fun drainStartedServices(): List<String> {
        val shadow = shadowOf(app)
        val names = mutableListOf<String>()
        while (true) {
            val intent = shadow.nextStartedService ?: break
            names += intent.component?.className.orEmpty()
        }
        return names
    }

    @Test
    fun systemStoppingTheIdleService_leavesThePlayerUsable() {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer
        val service = Robolectric.buildService(ApturaPlaybackService::class.java).create()

        // What the system does to an idle background service: destroy it. The player is not told anything.
        service.destroy()

        assertFalse("the service released the player the activity still holds", player.isReleased)
        assertNotNull("the process lost its player", PlayerManager.activeController)
        assertSame("a held controller no longer matches the process player", controller, PlayerManager.activeController)
    }

    @Test
    fun returningToTheApp_startsThePlaybackServiceAgain() {
        PlayerManager.getOrCreate(app)
        Robolectric.buildService(ApturaPlaybackService::class.java).create().destroy()
        drainStartedServices()

        PlayerManager.ensureServiceRunning(app)

        assertEquals(listOf(ApturaPlaybackService::class.java.name), drainStartedServices())
    }

    @Test
    fun ensureServiceRunning_withoutAPlayer_startsNothing() {
        PlayerManager.release()
        drainStartedServices()

        PlayerManager.ensureServiceRunning(app)

        assertTrue("a service was started with no player to serve", drainStartedServices().isEmpty())
    }

    @Test
    fun leavingForGood_releasesThePlayerWithTheService() {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer
        val service = Robolectric.buildService(ApturaPlaybackService::class.java).create()

        // Task swiped away from Recents with nothing playing: the service stops itself, and nobody is left to use the player.
        service.get().onTaskRemoved(null)
        service.destroy()

        assertTrue("the player of a user who left was never released", player.isReleased)
        assertNull(PlayerManager.activeController)
    }
}
