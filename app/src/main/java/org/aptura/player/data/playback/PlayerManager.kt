@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

/**
 * Thread-safe provider for the active Media3 player session and domain PlaybackController bridge.
 * Configures movie audio attributes, automatic audio focus, and audio-becoming-noisy handling.
 */
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
        val codecSelector = ApturaMediaCodecSelector {
            controllerInstance?.currentDecoderStrategy ?: 0
        }
        var textRendererRef: ApturaTextRenderer? = null
        val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(appContext) {
            override fun buildTextRenderers(
                context: Context,
                output: androidx.media3.exoplayer.text.TextOutput,
                outputLooper: android.os.Looper,
                extensionRendererMode: Int,
                out: java.util.ArrayList<androidx.media3.exoplayer.Renderer>
            ) {
                val renderer = ApturaTextRenderer(output, outputLooper)
                textRendererRef = renderer
                out.add(renderer)
            }
        }
            .setEnableDecoderFallback(true)
            .setMediaCodecSelector(codecSelector)

        val player = ExoPlayer.Builder(appContext, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val controller = Media3PlaybackController(
            player = player,
            apturaTextRenderer = textRendererRef
        )
        controllerInstance = controller

        try {
            val serviceIntent = Intent(appContext, ApturaPlaybackService::class.java)
            appContext.startService(serviceIntent)
        } catch (_: Throwable) {
            // Service will be started when activity is in foreground
        }

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
