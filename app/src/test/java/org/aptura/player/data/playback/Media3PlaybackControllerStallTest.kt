@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.core.diagnostics.EventCode
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * A video that was asked to play and never does (the owner's Samsung S24: a paused, black player, queue 558, no error,
 * nothing to say why). The player must try the software decoder once and then say plainly that playback did not start,
 * and must leave alone everything that is merely slow, paused or held back by the system.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Media3PlaybackControllerStallTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** A player that does what it is told about play-when-ready but only reports "playing" when the test says so. */
    private class StuckPlayer : InvocationHandler {
        val listeners = mutableListOf<Player.Listener>()
        var playWhenReady = false
        var isPlaying = false
        var state = Player.STATE_IDLE
        var suppression = Player.PLAYBACK_SUPPRESSION_REASON_NONE
        var itemCount = 0
        var item: MediaItem? = null
        var error: PlaybackException? = null
        var stops = 0
        var prepares = 0

        fun startPlaying() {
            state = Player.STATE_READY
            isPlaying = true
            listeners.forEach { it.onPlaybackStateChanged(Player.STATE_READY) }
            listeners.forEach { it.onIsPlayingChanged(true) }
        }

        fun changeState(newState: Int) {
            state = newState
            listeners.forEach { it.onPlaybackStateChanged(newState) }
        }

        override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? = when (method.name) {
            "addListener" -> { (args?.get(0) as? Player.Listener)?.let { listeners.add(it) }; null }
            "removeListener" -> { listeners.remove(args?.get(0)); null }
            "getPlayWhenReady" -> playWhenReady
            "isPlaying" -> isPlaying
            "getPlaybackState" -> state
            "getPlaybackSuppressionReason" -> suppression
            "getMediaItemCount" -> itemCount
            "getCurrentMediaItem" -> item
            "getPlayerError" -> error
            "setMediaItems" -> {
                val list = args?.get(0) as? List<*>
                itemCount = list?.size ?: 0
                item = list?.getOrNull(0) as? MediaItem
                null
            }
            "prepare" -> { prepares++; changeState(Player.STATE_BUFFERING); null }
            "stop" -> { stops++; isPlaying = false; changeState(Player.STATE_IDLE); null }
            "play" -> { playWhenReady = true; listeners.forEach { it.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }; null }
            "pause" -> { playWhenReady = false; isPlaying = false; listeners.forEach { it.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }; null }
            "getDuration" -> 60_000L
            "getPlaybackParameters" -> androidx.media3.common.PlaybackParameters(1f)
            "getTrackSelectionParameters" -> androidx.media3.common.TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
            "getCurrentTracks" -> androidx.media3.common.Tracks.EMPTY
            else -> when (method.returnType) {
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Boolean.TYPE -> false
                java.lang.Float.TYPE -> 0f
                java.lang.Double.TYPE -> 0.0
                else -> null
            }
        }
    }

    private val video = DomainMediaItem(uri = "content://media/1", title = "A video", durationMs = 60_000L, sizeBytes = 1_000_000L, mimeType = "video/mp4")

    private fun controllerWith(handler: StuckPlayer): Media3PlaybackController {
        val player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java), handler) as Player
        return Media3PlaybackController(
            player = player,
            scope = scope,
            mainDispatcher = dispatcher,
            enablePositionTicker = false
        )
    }

    private val window = PLAYBACK_START_TIMEOUT_MS + 1

    @Test
    fun aVideoThatNeverStarts_triesTheSoftwareDecoderOnce_thenSaysPlaybackDidNotStart() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        assertEquals("nothing happens before the wait is over", 0, player.stops)

        advanceTimeBy(window)
        runCurrent()
        assertEquals("the software decoder is tried once", 1, player.stops)
        assertEquals(2, controller.currentDecoderStrategy)
        assertTrue(controller.isCompatibilityMode.value)
        assertFalse("not an error yet, there is something left to try", controller.playbackState.value is DomainPlaybackState.Error)

        advanceTimeBy(window)
        runCurrent()
        val state = controller.playbackState.value
        assertTrue("then it says so: $state", state is DomainPlaybackState.Error)
        assertEquals("PLAYBACK_STALLED", (state as DomainPlaybackState.Error).errorCode)
        assertEquals("only one try", 1, player.stops)
        assertTrue("the reason names what the player was doing", controller.failureReason.value.orEmpty().contains("state=BUFFERING"))
    }

    @Test
    fun aVideoThatStartsInTime_isLeftAlone() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        advanceTimeBy(3_000)
        player.startPlaying()
        advanceTimeBy(window * 3)
        runCurrent()

        assertEquals(0, player.stops)
        assertEquals(0, controller.currentDecoderStrategy)
        assertFalse(controller.playbackState.value is DomainPlaybackState.Error)
    }

    @Test
    fun aPlayerThatAPersonPaused_isNotStuck() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        controller.pause()
        advanceTimeBy(window * 3)
        runCurrent()

        assertEquals(0, player.stops)
        assertFalse(controller.playbackState.value is DomainPlaybackState.Error)
    }

    @Test
    fun aPlayerTheSystemIsHoldingBack_isNotStuck() = runTest(dispatcher) {
        val player = StuckPlayer().apply { suppression = Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS }
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        advanceTimeBy(window * 3)
        runCurrent()

        assertEquals(0, player.stops)
        assertFalse(controller.playbackState.value is DomainPlaybackState.Error)
    }

    @Test
    fun aPlayerThatFailedWithAnError_isLeftToTheErrorHandling() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        player.error = PlaybackException("boom", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        player.changeState(Player.STATE_IDLE)
        advanceTimeBy(window * 3)
        runCurrent()

        assertEquals("the watchdog does not pile a second recovery on an error", 0, player.stops)
    }

    @Test
    fun retryingAfterTheMessage_startsANewWait() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        advanceTimeBy(window)
        runCurrent()
        advanceTimeBy(window)
        runCurrent()
        assertTrue(controller.playbackState.value is DomainPlaybackState.Error)

        controller.retryPlayback()
        runCurrent()
        assertFalse("the message goes away when it is retried", controller.playbackState.value is DomainPlaybackState.Error)
        player.startPlaying()
        advanceTimeBy(window * 2)
        runCurrent()
        assertFalse(controller.playbackState.value is DomainPlaybackState.Error)
    }

    @Test
    fun theReportCarriesWhereThePlayerStopped_theStepsAndTheThreads() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        ApturaDiagnostics.ringBuffer.clear()

        controller.playQueue(listOf(video), 0)
        runCurrent()
        advanceTimeBy(window)
        runCurrent()

        val stall = ApturaDiagnostics.ringBuffer.getAllEvents()
            .last { it.code == EventCode.EV_011 && it.payload["errorCode"] == "PLAYBACK_STALLED" }
        assertTrue("the steps are there: ${stall.payload}", stall.payload["trace"].orEmpty().contains("requested items=1 start=0"))
        assertTrue("the threads are there: ${stall.payload}", stall.payload.containsKey("threads"))
        assertTrue(stall.payload["reason"].orEmpty().contains("buffered="))
        assertTrue(stall.payload["reason"].orEmpty().contains("loading="))
    }
}
