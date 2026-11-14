@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.core.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Plays audio through the bundled FFmpeg libraries, on a real device, in a process where nothing else could decode it.
 *
 * Every other test either runs on the JVM, where the native libraries cannot load, or plays through Android's own codecs,
 * so a build whose extension had silently gone missing (an R8 rename, a missing ABI, a library that does not load) passed
 * the whole gate. These tests build a player whose ONLY renderer is [FfmpegAudioRenderer]: if a file reaches the end with
 * decoded buffers rendered, FFmpeg decoded it, and the decoder's own name ("ffmpegLavc63.1.102-pcm_alaw") says which one.
 *
 * The fixtures in assets/ffmpeg are one second of a 440 Hz sine at 8 kHz, mono, made with
 * `ffmpeg -f lavfi -i sine=frequency=440:sample_rate=8000:duration=1 -ac 1 -map_metadata -1 -fflags +bitexact
 * -flags:a +bitexact -c:a <alac|pcm_alaw|pcm_mulaw>`.
 *
 * This runs in `connected` (API 35) and in the nightly (API 26 and API 36, the 32-bit and 64-bit libraries among them).
 */
@RunWith(AndroidJUnit4::class)
class FfmpegAudioPlaybackTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    // ---- the library -----------------------------------------------------------------------------------------------

    @Test
    fun theNativeLibraries_loadOnThisDevice() {
        assertTrue("FfmpegLibrary.isAvailable() is false: the FFmpeg libraries did not load on this device", FfmpegLibrary.isAvailable())
        val version = FfmpegLibrary.getVersion()
        // FfmpegLibrary reports libavcodec's identification string. The pinned bundle is FFmpeg n9.0.2 (DEC-023), whose
        // libavcodec is 63.1.102 (libavcodec/version.h and RELEASE in the bundle's own source tarball). A different library
        // that happened to load under these names would not say so.
        assertEquals("Lavc63.1.102", version)
    }

    @Test
    fun theBundle_decodesExactlyTheApprovedFormats() {
        // The three decoders in tools/ffmpeg/approved_decoders.txt (spec/11B D2)...
        for (mime in listOf(MimeTypes.AUDIO_ALAC, MimeTypes.AUDIO_ALAW, MimeTypes.AUDIO_MLAW)) {
            assertTrue("$mime should be decoded by the bundle", FfmpegLibrary.supportsFormat(mime))
        }
        // ...and nothing the policy left to the device or excluded: this is what the shipped libraries really contain, not a list.
        val excluded = listOf(
            MimeTypes.AUDIO_AAC, MimeTypes.AUDIO_AC3, MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC, MimeTypes.AUDIO_DTS,
            MimeTypes.AUDIO_DTS_HD, MimeTypes.AUDIO_TRUEHD, MimeTypes.AUDIO_MPEG, MimeTypes.AUDIO_FLAC, MimeTypes.AUDIO_VORBIS,
            MimeTypes.AUDIO_OPUS, MimeTypes.AUDIO_AMR_NB, MimeTypes.AUDIO_AMR_WB,
            // Never a video decoder (ADR-010): FfmpegLibrary maps these two to FFmpeg's h264 and hevc decoders.
            MimeTypes.VIDEO_H264, MimeTypes.VIDEO_H265
        )
        for (mime in excluded) {
            assertFalse("$mime must not be decoded by the bundle", FfmpegLibrary.supportsFormat(mime))
        }
    }

    @Test
    fun theAppsOwnRenderersFactory_findsTheExtensionByReflection() {
        // PlayerManager enables extension renderers; ExoPlayer then loads this class by name, which R8 cannot see.
        val renderers = onMain {
            val player = ExoPlayer.Builder(
                context,
                DefaultRenderersFactory(context).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            ).build()
            try {
                (0 until player.rendererCount).map { player.getRenderer(it).javaClass.name }
            } finally {
                player.release()
            }
        }
        assertTrue(
            "no FfmpegAudioRenderer among $renderers",
            renderers.contains("androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer")
        )
    }

    // ---- real playback through FFmpeg -----------------------------------------------------------------------------

    @Test
    fun aG711ALawFile_isDecodedByFfmpeg_andPlaysToTheEnd() = assertDecodedByFfmpeg("tone_alaw.wav", "pcm_alaw", MimeTypes.AUDIO_ALAW)

    @Test
    fun aG711MuLawFile_isDecodedByFfmpeg_andPlaysToTheEnd() = assertDecodedByFfmpeg("tone_mulaw.wav", "pcm_mulaw", MimeTypes.AUDIO_MLAW)

    @Test
    fun anAlacFile_isDecodedByFfmpeg_andPlaysToTheEnd() = assertDecodedByFfmpeg("tone_alac.m4a", "alac", MimeTypes.AUDIO_ALAC)

    private fun assertDecodedByFfmpeg(asset: String, codec: String, mime: String) {
        val evidence = playWithOnlyTheFfmpegRenderer(asset)

        assertEquals("playback did not reach the end: $evidence", Player.STATE_ENDED, evidence.finalState)
        assertEquals("the player reported an error: $evidence", null, evidence.error)
        assertEquals("the track was not read as $mime: $evidence", mime, evidence.inputMimeType)
        assertTrue(
            "no FFmpeg decoder for $codec was initialised: $evidence",
            evidence.decoderNames.any { it.startsWith("ffmpeg") && it.endsWith("-$codec") }
        )
        assertTrue("FFmpeg rendered no decoded audio: $evidence", evidence.renderedOutputBuffers > 0)
        assertTrue("FFmpeg consumed no input: $evidence", evidence.inputBuffers > 0)
    }

    private data class Evidence(
        val finalState: Int,
        val error: String?,
        val inputMimeType: String?,
        val decoderNames: List<String>,
        val renderedOutputBuffers: Int,
        val inputBuffers: Int
    )

    /** Plays [asset] with a player whose only renderer is FFmpeg's, and returns what happened. */
    private fun playWithOnlyTheFfmpegRenderer(asset: String): Evidence {
        val file = File(context.cacheDir, asset).also { target ->
            InstrumentationRegistry.getInstrumentation().context.assets.open("ffmpeg/$asset").use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
        }
        val decoderNames = CopyOnWriteArrayList<String>()
        val inputMimeTypes = CopyOnWriteArrayList<String>()
        var counters: DecoderCounters? = null

        val player = onMain {
            val onlyFfmpeg = RenderersFactory { handler, _, audioListener, _, _ ->
                arrayOf<Renderer>(FfmpegAudioRenderer(handler, audioListener, DefaultAudioSink.Builder(context).build()))
            }
            ExoPlayer.Builder(context, onlyFfmpeg).build().apply {
                addAnalyticsListener(object : AnalyticsListener {
                    override fun onAudioEnabled(eventTime: AnalyticsListener.EventTime, decoderCounters: DecoderCounters) {
                        counters = decoderCounters
                    }

                    override fun onAudioDecoderInitialized(
                        eventTime: AnalyticsListener.EventTime,
                        decoderName: String,
                        initializedTimestampMs: Long,
                        initializationDurationMs: Long
                    ) {
                        decoderNames += decoderName
                    }

                    override fun onAudioInputFormatChanged(
                        eventTime: AnalyticsListener.EventTime,
                        format: Format,
                        decoderReuseEvaluation: DecoderReuseEvaluation?
                    ) {
                        format.sampleMimeType?.let { inputMimeTypes += it }
                    }
                })
                setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                prepare()
                playWhenReady = true
            }
        }

        try {
            val deadline = System.currentTimeMillis() + PLAY_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline) {
                val (state, error) = onMain { player.playbackState to player.playerError }
                if (state == Player.STATE_ENDED || error != null) break
                Thread.sleep(100)
            }
            return onMain {
                counters?.ensureUpdated()
                Evidence(
                    finalState = player.playbackState,
                    error = player.playerError?.let { "${it.errorCodeName}: ${it.message}" },
                    inputMimeType = inputMimeTypes.firstOrNull(),
                    decoderNames = decoderNames.toList(),
                    renderedOutputBuffers = counters?.renderedOutputBufferCount ?: 0,
                    inputBuffers = counters?.queuedInputBufferCount ?: 0
                )
            }
        } finally {
            onMain { player.release() }
            file.delete()
        }
    }

    private companion object {
        const val PLAY_TIMEOUT_MS = 20_000L
    }
}
