@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.util.Size
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.TsExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.aptura.player.data.database.ApturaDatabase
import org.aptura.player.data.repository.AndroidSidecarSubtitleResolver
import org.aptura.player.data.repository.MediaRepositoryImpl
import java.io.ByteArrayOutputStream

/**
 * Thread-safe provider for the active Media3 player session and domain PlaybackController bridge.
 * Configures movie audio attributes, automatic audio focus, audio-becoming-noisy handling, the real audio-delay
 * processor, and the codec selector that implements the Auto / Hardware / Software decoder strategies.
 */
object PlayerManager {

    @Volatile
    private var controllerInstance: Media3PlaybackController? = null

    /** Work that must live exactly as long as the player does, with or without a screen (audit FUNC-05). */
    private val processScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Saves how far the person got in each video. */
    private var progressRecorderJob: Job? = null

    /**
     * The equalizer, bass boost, virtualizer and loudness effects, bound to the player's audio session. They live here,
     * beside the player, so the sound keeps them when the screen is swiped away. Screens borrow this controller.
     */
    private var audioEnhancementInstance: AudioEnhancementController? = null
    private var effectsFollowerJob: Job? = null

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
            ): AudioSink = buildAudioSink(
                context = context,
                processor = audioDelayProcessor,
                enableFloatOutput = enableFloatOutput,
                enableAudioTrackPlaybackParams = enableAudioTrackPlaybackParams
            )
        }
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
            .setMediaCodecSelector(codecSelector)

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)

        // Offline-only DataSource boundary (DEC-012, SEC-09): network playback throws IOException immediately.
        val offlineBaseDataSourceFactory = DataSource.Factory {
            object : DataSource {
                override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {}
                override fun open(dataSpec: androidx.media3.datasource.DataSpec): Long {
                    throw java.io.IOException("Network playback is forbidden. Aptura is strictly offline-only (DEC-012).")
                }
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int = -1
                override fun getUri(): android.net.Uri? = null
                override fun close() {}
            }
        }
        val localDataSourceFactory = DefaultDataSource.Factory(appContext, offlineBaseDataSourceFactory)

        val player = ExoPlayer.Builder(appContext, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(localDataSourceFactory, extractorsFactory))
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

        // The screen's view model also saves progress, but it dies with the screen. This one lives with the player, so
        // the sound that goes on after the task is swiped away still has its position kept.
        val database = ApturaDatabase.getInstance(appContext)
        val repository = MediaRepositoryImpl(database.mediaItemDao(), database.bookmarkDao(), appContext)
        progressRecorderJob = PlaybackProgressRecorder(
            playbackState = controller.playbackState,
            playbackQueue = controller.playbackQueue,
            save = repository::savePlaybackPosition
        ).start(processScope)

        // The effects follow the player's audio session from here on, whoever is looking at the screen.
        val enhancement = AudioEnhancementController()
        audioEnhancementInstance = enhancement
        effectsFollowerJob = AudioEffectsSessionFollower(controller.audioSessionIds, enhancement).start(processScope)

        ensureServiceRunning(appContext)

        return controller
    }

    /**
     * The effects controller that belongs to the player, created together with it. The screen's view model borrows it
     * and must not release it (`PlayerViewModel` is built with `ownsAudioEnhancement = false` for it).
     */
    @Synchronized
    fun getOrCreateAudioEnhancement(context: Context): AudioEnhancementController {
        getOrCreate(context)
        return checkNotNull(audioEnhancementInstance) { "the player was created without its effects" }
    }

    /**
     * Starts [ApturaPlaybackService] when the process has a player, so the player has its media session and
     * notification. Android stops that service after playback has been paused in the background for a while, but the
     * player outlives it (audit STAB-01), so the activity calls this on every start to bring the service back.
     * Starting a service that is already running does nothing.
     */
    fun ensureServiceRunning(context: Context) {
        if (controllerInstance == null) return
        val appContext = context.applicationContext
        try {
            appContext.startService(Intent(appContext, ApturaPlaybackService::class.java))
        } catch (_: IllegalStateException) {
            // The app is in the background, where Android refuses to start a service. The next start retries.
        } catch (_: SecurityException) {
            // Same: the system declined, and the player keeps working without a notification until the next start.
        }
    }

    /** Lock-screen / notification artwork: a real video frame thumbnail (Android 10+). */
    private fun loadArtwork(context: Context, mediaUri: String): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val bitmap = context.contentResolver.loadThumbnail(mediaUri.toUri(), Size(512, 288), null)
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
        progressRecorderJob?.cancel()
        progressRecorderJob = null
        effectsFollowerJob?.cancel()
        effectsFollowerJob = null
        // Effects are a limited system resource and belong to the player being released.
        audioEnhancementInstance?.releaseEffects()
        audioEnhancementInstance = null
        controllerInstance?.let { controller ->
            controller.player.release()
            controllerInstance = null
        }
    }
}
