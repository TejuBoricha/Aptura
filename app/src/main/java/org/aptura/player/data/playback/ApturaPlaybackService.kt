@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.aptura.player.MainActivity

/**
 * Foreground MediaSessionService for Aptura Video Player.
 * Keeps playback active during backgrounding, navigation, and screen-off.
 * Invariant: Media playback notification is platform-exempt from POST_NOTIFICATIONS (FR-PLAT-002, W10).
 * Never post non-media notifications from this service.
 */
class ApturaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** Set when the user leaves for good (task swiped away with nothing playing): the player goes with the service. */
    private var releasePlayerOnDestroy = false

    override fun onCreate() {
        super.onCreate()

        val controller = PlayerManager.getOrCreate(applicationContext)
        val player = controller.player

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(ApturaSessionCallback())
            .build()

        // Register the session with the service. Without this nothing connects a controller, so Media3 never posts the
        // media notification and the service never enters the foreground: background playback had no notification,
        // no lock-screen controls and could be killed at will (audit P0-03).
        addSession(mediaSession!!)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // If playback is not active when app is swiped away from Recents, stop foreground service. This is the one
        // exit that also releases the player, because nobody is left to use it.
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            releasePlayerOnDestroy = true
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        // Android stops this service when playback has been paused in the background for a while. The activity, its
        // ViewModel and Navigation still hold the controller they got from PlayerManager, so the player must outlive
        // the service: releasing it here left Play dead and new videos failing until the process was killed (audit
        // STAB-01). MainActivity.onStart starts the service again when the app returns.
        if (releasePlayerOnDestroy) {
            PlayerManager.release()
        }
        super.onDestroy()
    }

    private class ApturaSessionCallback : MediaSession.Callback
}
