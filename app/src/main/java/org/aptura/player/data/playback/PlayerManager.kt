@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import org.aptura.player.data.repository.AndroidSidecarSubtitleResolver
import java.io.ByteArrayOutputStream

/**
 * Thread-safe provider for the active Media3 player session and domain PlaybackController bridge.
 * Configures movie audio attributes, automatic audio focus, audio-becoming-noisy handling, the real audio-delay
 * processor, and the codec selector that implements the Auto / Hardware / Software decoder strategies.
 */
object PlayerManager {

    @Volatile
    private var controllerInstance: Media3PlaybackController? = null

    val activeController: Media3PlaybackController?
        get() = controllerInstance

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
        val audioDelayProcessor = AudioDelayProcessor()
        val renderersFactory = object : DefaultRenderersFactory(appContext) {
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

            // Routes decoded PCM through AudioDelayProcessor so the lip-sync control really shifts the audio.
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(audioDelayProcessor))
                .build()
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
            apturaTextRenderer = textRendererRef,
            sidecarResolver = AndroidSidecarSubtitleResolver(appContext),
            artworkProvider = { uri -> loadArtwork(appContext, uri) },
            audioDelayProcessor = audioDelayProcessor
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

    /** Lock-screen / notification artwork: a real video frame thumbnail (Android 10+). */
    private fun loadArtwork(context: Context, mediaUri: String): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val bitmap = context.contentResolver.loadThumbnail(Uri.parse(mediaUri), Size(512, 288), null)
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.toByteArray()
            }
        } catch (_: Throwable) {
            null
        }
    }

    @Synchronized
    fun release() {
        controllerInstance?.let { controller ->
            controller.player.release()
            controllerInstance = null
        }
    }
}
