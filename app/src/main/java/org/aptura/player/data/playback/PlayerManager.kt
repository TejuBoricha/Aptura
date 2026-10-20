package org.aptura.player.data.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Thread-safe provider for the active Media3 player session and domain PlaybackController bridge.
 * Configures movie audio attributes, automatic audio focus, and audio-becoming-noisy handling.
 */
@OptIn(UnstableApi::class)
object PlayerManager {

    @Volatile
    private var controllerInstance: Media3PlaybackController? = null

    @Synchronized
    fun getOrCreate(context: Context): Media3PlaybackController {
        val existing = controllerInstance
        if (existing != null) {
            return existing
        }

        val appContext = context.applicationContext
        val player = ExoPlayer.Builder(appContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val controller = Media3PlaybackController(player)
        controllerInstance = controller
        return controller
    }

    @Synchronized
    fun release() {
        controllerInstance?.let { controller ->
            controller.player.release()
            controllerInstance = null
        }
    }
}
