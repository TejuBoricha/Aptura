package org.aptura.player

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.performClick
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.aptura.player.data.playback.Media3PlaybackController
import org.aptura.player.data.playback.PlayerManager
import org.aptura.player.data.playback.PlayerParts
import org.aptura.player.data.scanner.MediaScanScheduler
import org.aptura.player.data.settings.ApturaSettingsRepository
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * Plays a real video, which nothing in this repository did before.
 *
 * Every other test either runs on the JVM, where no codec exists, or opens the app and looks at the screen. So a build
 * in which no video started still passed the whole gate; the first person to find out was the owner, on a phone. These
 * tests put a four-second H.264 and AAC clip into MediaStore and require the player to be playing it, with the position
 * moving, both when the player is driven directly and when the clip is picked in the library like a person would.
 *
 * When the player does not start, the failure message says what the player was doing (state, play-when-ready,
 * suppression reason, error, position) instead of only "timed out", because the next question is always why.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackSmokeTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var clipUri: Uri? = null
    private var hevcUri: Uri? = null

    private class SeedRule : TestRule {
        override fun apply(base: Statement, description: Description) = object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                instrumentation.uiAutomation.grantRuntimePermission(
                    instrumentation.targetContext.packageName,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
                runBlocking { ApturaSettingsRepository(instrumentation.targetContext).setOnboardingCompleted(true) }
                base.evaluate()
            }
        }
    }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(SeedRule()).around(composeRule)

    @Before
    fun putClipsInMediaStore() {
        clipUri = insertClip(CLIP_NAME, "smoke_clip.mp4")
        hevcUri = insertClip(HEVC_NAME, "smoke_hevc.mp4")
    }

    private fun insertClip(displayName: String, asset: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Checks")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore refused to create $displayName")
        resolver.openOutputStream(uri)!!.use { out ->
            InstrumentationRegistry.getInstrumentation().context.assets.open(asset).use { it.copyTo(out) }
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
        return uri
    }

    @After
    fun removeClips() {
        runCatching { PlayerManager.activeController?.player?.let { p -> onMain { p.stop() } } }
        listOfNotNull(clipUri, hevcUri).forEach { runCatching { context.contentResolver.delete(it, null, null) } }
    }

    private fun item(uri: Uri?, title: String) = DomainMediaItem(
        uri = uri.toString(),
        title = title,
        durationMs = 3_000L,
        sizeBytes = 50_000L,
        mimeType = "video/mp4"
    )

    @Test
    fun theController_playsAnH264Video() {
        val controller = PlayerManager.getOrCreate(context)
        runBlocking(Dispatchers.Main) { controller.playQueue(listOf(item(clipUri, "zqprobe clip")), 0) }

        assertPlays(controller, "the controller, one H.264 video")
    }

    @Test
    fun theController_playsAnHevcVideo() {
        val controller = PlayerManager.getOrCreate(context)
        runBlocking(Dispatchers.Main) { controller.playQueue(listOf(item(hevcUri, "zqhevc clip")), 0) }

        assertPlays(controller, "the controller, one HEVC video")
    }

    /** A real library holds hundreds of videos and a tap queues all of them: the owner's phone showed 558. */
    @Test
    fun aQueueOfHundredsStartedInTheMiddle_plays() {
        val controller = PlayerManager.getOrCreate(context)
        val queue = List(558) { index -> item(if (index % 2 == 0) clipUri else hevcUri, "queue item $index") }
        runBlocking(Dispatchers.Main) { controller.playQueue(queue, 300) }

        assertPlays(controller, "the controller, 558 items starting at 301")
        val index = onMain { controller.player.currentMediaItemIndex }
        if (index != 300) fail("The queue started at item ${index + 1}, not 301.")
    }

    /**
     * The owner's day on the Samsung S24: a video is watched, the player is left, another is picked. The first started
     * in under a second; every later one sat in "buffering" at 0:00.
     */
    @Test
    fun aSecondVideoPickedAfterTheFirstWasLeft_plays() {
        val controller = PlayerManager.getOrCreate(context)
        runBlocking(Dispatchers.Main) { controller.playQueue(listOf(item(clipUri, "zqprobe clip")), 0) }
        assertPlays(controller, "the first video", expectedMediaId = clipUri.toString())

        onMain { controller.stopForDismissal() } // what leaving the player does
        assertEquals("the decoder is let go when the player is left", Player.STATE_IDLE, onMain { controller.player.playbackState })
        val queue = List(7) { index -> item(if (index % 2 == 1) hevcUri else clipUri, "queue item $index") }
        runBlocking(Dispatchers.Main) { controller.playQueue(queue, 5) } // item 6 of 7, as on the phone

        assertPlays(controller, "a queue of 7 started at item 6 after the first video was left", expectedMediaId = hevcUri.toString())
    }

    @Test
    fun twoVideosPickedOneAfterTheOtherInTheLibrary_bothPlay() {
        MediaScanScheduler.scheduleImmediateScan(context)
        val first = hasText(CLIP_WORD, substring = true, ignoreCase = true)
        val second = hasText(HEVC_WORD, substring = true, ignoreCase = true)
        waitForNode(first, 30_000)
        waitForNode(second, 30_000)
        val controller = PlayerManager.getOrCreate(context)

        composeRule.onAllNodes(first)[0].performClick()
        assertPlays(controller, "the first tap", expectedMediaId = clipUri.toString())

        androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        // Leaving the player lets its decoder go before the picture's surface is destroyed.
        waitFor(5_000, "the decoder to be let go on leaving the player") { controller.player.playbackState == Player.STATE_IDLE }
        // Leaving the player turns the screen back upright, which recreates the activity: for a moment there is no screen.
        waitForNode(second, 15_000)
        composeRule.onAllNodes(second)[0].performClick()
        assertPlays(controller, "a second tap, after going back from the first video", expectedMediaId = hevcUri.toString())
    }

    @Test
    fun aVideoPickedInTheLibrary_plays() {
        MediaScanScheduler.scheduleImmediateScan(context)

        val card = hasText(CLIP_WORD, substring = true, ignoreCase = true)
        waitForNode(card, 30_000)
        composeRule.onAllNodes(card)[0].performClick()

        val controller = PlayerManager.getOrCreate(context)
        // First the queue has to reach the player at all; then it has to play. Two messages, because they are two bugs.
        val queued = System.currentTimeMillis() + 10_000
        while (onMain { controller.player.mediaItemCount } == 0 && System.currentTimeMillis() < queued) Thread.sleep(100)
        if (onMain { controller.player.mediaItemCount } == 0) {
            fail("Tapping the video in the library never gave the player a queue. ${screenAndLog()}")
        }
        assertPlays(controller, "a tap on the card in the library")
    }


    // ---- a player that wedges is replaced -------------------------------------------------------------------------

    /** A source that never delivers a byte: the player sits in "buffering" for ever, as the owner's did. */
    @UnstableApi
    private class NeverDeliversDataSource : BaseDataSource(/* isNetwork = */ false) {
        private val closed = java.util.concurrent.CountDownLatch(1)

        override fun open(dataSpec: DataSpec): Long {
            transferInitializing(dataSpec)
            try {
                closed.await()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw java.io.InterruptedIOException()
            }
            throw java.io.IOException("closed")
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = C.RESULT_END_OF_INPUT
        override fun getUri(): Uri? = null
        override fun close() = closed.countDown()
    }

    @UnstableApi
    private fun stuckPlayer(): ExoPlayer = onMain {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DataSource.Factory { NeverDeliversDataSource() }))
            .build()
    }

    @UnstableApi
    private fun recoveringController(first: ExoPlayer, made: MutableList<ExoPlayer>, fresh: () -> ExoPlayer): Media3PlaybackController = onMain {
        Media3PlaybackController(
            player = first,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
            enablePositionTicker = false,
            playerFactory = { PlayerParts(fresh().also { made += it }) }
        )
    }

    /** Waits for a node, and through the moments when the activity is being recreated and the screen is not there to ask. */
    private fun waitForNode(matcher: androidx.compose.ui.test.SemanticsMatcher, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val found = runCatching { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
            if (found) return
            Thread.sleep(200)
        }
        fail("Waited ${timeoutMs / 1000} s for a node on screen. ${screenAndLog()}")
    }

    private fun waitFor(timeoutMs: Long, what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (onMain(condition)) return
            Thread.sleep(200)
        }
        fail("Waited ${timeoutMs / 1000} s for $what. ${appRecord()}")
    }

    /**
     * The player the owner's phone had: it was asked to play and never did. Nothing can repair it, so it must be
     * replaced, and the video must then play on the new one, with the rest of the queue behind it.
     */
    @UnstableApi
    @Test
    fun aWedgedPlayer_isReplaced_andTheVideoPlaysOnTheNewOne() {
        val made = mutableListOf<ExoPlayer>()
        val first = stuckPlayer()
        val controller = recoveringController(first, made) { ExoPlayer.Builder(context).build() }
        try {
            val queue = List(3) { index -> item(if (index == 1) hevcUri else clipUri, "wedged queue item $index") }
            runBlocking(Dispatchers.Main) { controller.playQueue(queue, 1) }

            assertPlays(controller, "a wedged player that has to be replaced", expectedMediaId = hevcUri.toString(), timeoutMs = 25_000)
            assertTrue("the wedged player was not let go", onMain { first.isReleased })
            assertNotSame(first, onMain { controller.player })
            assertEquals("one replacement was enough", 1, made.size)
            waitFor(10_000, "the rest of the queue to follow the video") { controller.player.mediaItemCount == 3 }
            assertEquals("the video that is playing is still the one that was picked", 1, onMain { controller.player.currentMediaItemIndex })
            assertEquals(3, controller.playbackQueue.value.items.size)
            assertEquals(1, controller.playbackQueue.value.currentIndex)

            // After that, every start begins on a new player, so the stall does not wait to happen again.
            val before = onMain { controller.player }
            runBlocking(Dispatchers.Main) { controller.playQueue(listOf(item(clipUri, "the next one")), 0) }
            assertNotSame(before, onMain { controller.player })
            assertPlays(controller, "the next video, on a new player", expectedMediaId = clipUri.toString())
        } finally {
            runBlocking(Dispatchers.Main) { controller.release() }
        }
    }

    /** If nothing works, say so: a message, not a black screen, and a Retry that starts afresh. */
    @UnstableApi
    @Test
    fun aPlayerThatStaysWedged_endsInAMessage_andRetryStartsAgain() {
        val made = mutableListOf<ExoPlayer>()
        val first = stuckPlayer()
        val controller = recoveringController(first, made) { stuckPlayer() }
        try {
            runBlocking(Dispatchers.Main) { controller.playQueue(listOf(item(clipUri, "never starts")), 0) }

            waitFor(30_000, "the message that playback did not start") {
                (controller.playbackState.value as? DomainPlaybackState.Error)?.errorCode == "PLAYBACK_STALLED"
            }
            assertEquals("two fresh players were tried", 2, made.size)
            assertTrue(onMain { first.isReleased })
            assertTrue("the first fresh player was let go when the second took over", onMain { made[0].isReleased })

            runBlocking(Dispatchers.Main) { controller.retryPlayback() }
            assertEquals("a retry starts on a new player", 3, made.size)
            assertTrue("the message goes away when it is retried", controller.playbackState.value !is DomainPlaybackState.Error)
        } finally {
            runBlocking(Dispatchers.Main) { controller.release() }
        }
    }

    private fun findPlayerView(view: View): PlayerView? = when (view) {
        is PlayerView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findPlayerView(view.getChildAt(it)) }
        else -> null
    }

    /** The picture and the lock screen belong to the player in use, so they must move with it. */
    @UnstableApi
    @Test
    fun aPlayerReplacedWhileTheScreenShowsIt_isFollowedByTheScreen() {
        MediaScanScheduler.scheduleImmediateScan(context)
        val card = hasText(CLIP_WORD, substring = true, ignoreCase = true)
        waitForNode(card, 30_000)
        composeRule.onAllNodes(card)[0].performClick()
        val controller = PlayerManager.getOrCreate(context)
        assertPlays(controller, "a tap on the card in the library")

        val before = onMain { controller.player }
        assertTrue("no way to make a new player", onMain { controller.startOnFreshPlayer("a test", software = false) })
        assertNotSame(before, onMain { controller.player })
        assertPlays(controller, "the replacement while the player screen is showing")

        waitFor(8_000, "the picture to follow the new player") {
            findPlayerView(composeRule.activity.window.decorView)?.player === controller.player
        }
        assertSame(controller.player, onMain { findPlayerView(composeRule.activity.window.decorView)?.player })
    }

    /** What the screen and the app's own log said, for the failure message. */
    private fun screenAndLog(): String {
        val texts = composeRule.onAllNodes(hasText("", substring = true)).fetchSemanticsNodes()
            .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text } }
            .filter { it.isNotBlank() }.take(40)
        val log = runCatching {
            val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("logcat -d -t 400 -s ApturaDiagnostics:* ExoPlayerImpl:* MediaCodec:* AndroidRuntime:E")
            java.io.FileInputStream(pfd.fileDescriptor).bufferedReader().use { it.readText() }.takeLast(3_000)
        }.getOrDefault("(no log)")
        return "On screen: $texts. Log: $log"
    }

    /** Waits for the player to be playing with the position moving, and fails with what the player was doing. */
    private fun assertPlays(controller: Media3PlaybackController, how: String, expectedMediaId: String? = null, timeoutMs: Long = PLAY_TIMEOUT_MS) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last = ""
        while (System.currentTimeMillis() < deadline) {
            // The controller may replace a wedged player, so ask it for the one in use every time.
            val snapshot = onMain { Snapshot.of(controller.player) }
            last = snapshot.describe()
            val rightVideo = expectedMediaId == null || snapshot.mediaId == expectedMediaId
            if (rightVideo && snapshot.isPlaying && snapshot.positionMs >= MIN_POSITION_MS) return
            Thread.sleep(200)
        }
        fail("No video started playing through $how within ${timeoutMs / 1000} s. The player was: $last. " +
            "The app's own record: ${appRecord()}. ${screenAndLog()}")
    }

    /** The last things the app wrote to its diagnostic record: the player's steps and threads, if it stalled. */
    private fun appRecord(): String =
        org.aptura.player.core.diagnostics.ApturaDiagnostics.ringBuffer.getAllEvents().takeLast(6)
            .joinToString(" // ") { "${it.code.eventName} ${it.payload}" }

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    private data class Snapshot(
        val state: Int,
        val playWhenReady: Boolean,
        val isPlaying: Boolean,
        val suppression: Int,
        val positionMs: Long,
        val items: Int,
        val index: Int,
        val mediaId: String?,
        val error: String?
    ) {
        fun describe() = "state=${stateName(state)} playWhenReady=$playWhenReady isPlaying=$isPlaying " +
            "suppression=$suppression position=${positionMs}ms item=${index + 1}/$items error=${error ?: "none"}"

        companion object {
            fun of(player: Player) = Snapshot(
                state = player.playbackState,
                playWhenReady = player.playWhenReady,
                isPlaying = player.isPlaying,
                suppression = player.playbackSuppressionReason,
                positionMs = player.currentPosition,
                items = player.mediaItemCount,
                index = player.currentMediaItemIndex,
                mediaId = player.currentMediaItem?.mediaId,
                error = player.playerError?.let { "${it.errorCodeName}: ${it.message}" }
            )

            private fun stateName(state: Int) = when (state) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> state.toString()
            }
        }
    }

    private companion object {
        const val CLIP_NAME = "zqprobe_clip.mp4"
        const val CLIP_WORD = "zqprobe"
        const val HEVC_WORD = "zqhevc"
        const val HEVC_NAME = "zqhevc_clip.mp4"
        const val PLAY_TIMEOUT_MS = 20_000L
        const val MIN_POSITION_MS = 700L
    }
}
