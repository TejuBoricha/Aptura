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
        var clearVideoSurfaceCalls = 0

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
            "clearVideoSurface" -> { clearVideoSurfaceCalls++; null }
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
    // --- Audit FUNC-09: the start watchdog guards the start of a video, not its middle ---

    private val secondVideo = DomainMediaItem(uri = "content://media/2", title = "Another video", durationMs = 60_000L, sizeBytes = 1_000_000L, mimeType = "video/mp4")

    /** Playing, then buffering: what a slow network share or a long seek looks like from the player. */
    private fun StuckPlayer.startThenBuffer() {
        startPlaying()
        isPlaying = false
        changeState(Player.STATE_BUFFERING)
        listeners.forEach { it.onIsPlayingChanged(false) }
    }

    @Test
    fun aVideoThatBuffersAfterItHasStarted_isNotStuck_andIsNeverSwitchedToSoftware() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.playQueue(listOf(video), 0)
        runCurrent()

        player.startThenBuffer()
        advanceTimeBy(window * 3)
        runCurrent()

        assertEquals("a long buffer is not a failed start", 0, player.stops)
        assertEquals(0, controller.currentDecoderStrategy)
        assertFalse(controller.isCompatibilityMode.value)
        assertFalse(controller.playbackState.value is DomainPlaybackState.Error)
    }

    @Test
    fun aNextVideoThatNeverStarts_isStillWatched() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.playQueue(listOf(video, secondVideo), 0)
        runCurrent()
        player.startThenBuffer() // the first video played, then it ended and the player moved on and is not playing
        player.listeners.forEach {
            it.onMediaItemTransition(
                MediaItem.Builder().setMediaId(secondVideo.uri).build(),
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
            )
        }
        advanceTimeBy(window)
        runCurrent()

        assertEquals("the new video gets its own start wait", 1, player.stops)
        assertEquals(2, controller.currentDecoderStrategy)
    }

    @Test
    fun aVideoRestartedByChangingTheDecoder_thatNeverComesBack_isStillWatched() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.playQueue(listOf(video), 0)
        runCurrent()
        player.startPlaying()
        advanceTimeBy(1_000)

        controller.setDecoderStrategy(2) // stops and prepares again; the picture never returns
        advanceTimeBy(window)
        runCurrent()

        val state = controller.playbackState.value
        assertTrue("it says so instead of staying a silent black player: $state", state is DomainPlaybackState.Error)
        assertEquals("PLAYBACK_STALLED", (state as DomainPlaybackState.Error).errorCode)
    }

    @Test
    fun theSameVideoLoadedAgain_thatNeverStarts_isStillWatched() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.playQueue(listOf(video), 0)
        runCurrent()
        player.startPlaying()
        advanceTimeBy(1_000)

        // The person picks the same video again: a new request, so a new wait, though it is the same item.
        player.isPlaying = false
        player.listeners.forEach { it.onIsPlayingChanged(false) }
        controller.playQueue(listOf(video), 0)
        runCurrent()
        advanceTimeBy(window)
        runCurrent()

        assertEquals(1, player.stops)
        assertEquals(2, controller.currentDecoderStrategy)
    }
    // --- Audit FUNC-10: "Hardware" means hardware; only "Auto" may fall back to software by itself ---

    private fun decoderFailure() = PlaybackException("decoder", null, PlaybackException.ERROR_CODE_DECODER_INIT_FAILED)

    @Test
    fun withHardwareOnly_aVideoThatNeverStarts_isNotSwitchedToSoftware_itIsReportedAtOnce() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.setDecoderStrategy(1) // the person chose Hardware
        controller.playQueue(listOf(video), 0)
        runCurrent()

        advanceTimeBy(window)
        runCurrent()

        assertEquals("the software decoder is not tried behind the person's back", 0, player.stops)
        assertEquals(1, controller.currentDecoderStrategy)
        assertFalse(controller.isCompatibilityMode.value)
        val state = controller.playbackState.value
        assertTrue("it says so straight away: $state", state is DomainPlaybackState.Error)
        assertEquals("PLAYBACK_STALLED", (state as DomainPlaybackState.Error).errorCode)
        assertTrue(
            "the first suggestion is to choose Software, since it was not tried: ${state.suggestedAction}",
            state.suggestedAction.orEmpty().contains("Software")
        )
    }

    @Test
    fun withHardwareOnly_aDecoderFailure_isNotSwitchedToSoftware() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.setDecoderStrategy(1)
        controller.playQueue(listOf(video), 0)
        runCurrent()

        player.error = decoderFailure()
        player.listeners.forEach { it.onPlayerError(player.error!!) }
        runCurrent()

        assertEquals(0, player.stops)
        assertEquals(1, controller.currentDecoderStrategy)
        assertFalse(controller.isCompatibilityMode.value)
        assertTrue(controller.playbackState.value is DomainPlaybackState.Error)
    }

    @Test
    fun withAuto_aDecoderFailure_stillFallsBackToSoftwareOnce() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player) // Auto
        controller.playQueue(listOf(video), 0)
        runCurrent()

        player.error = decoderFailure()
        player.listeners.forEach { it.onPlayerError(player.error!!) }
        runCurrent()

        assertEquals("Auto is allowed to recover by itself", 1, player.stops)
        assertEquals(2, controller.currentDecoderStrategy)
        assertTrue(controller.isCompatibilityMode.value)
    }

    @Test
    fun runtimeCheckError_doesNotClearVideoSurface_andRetryPreservesSurface() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.playQueue(listOf(video), 0)
        runCurrent()

        val runtimeError = PlaybackException("Surface detached", null, PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK)
        player.error = runtimeError
        player.listeners.forEach { it.onPlayerError(runtimeError) }
        runCurrent()

        assertEquals("Surface must NOT be cleared on runtime check error", 0, player.clearVideoSurfaceCalls)

        val preparesBeforeRetry = player.prepares
        controller.retryPlayback()
        runCurrent()

        assertEquals("retryPlayback prepares the player", preparesBeforeRetry + 1, player.prepares)
        assertTrue("retryPlayback starts playback", player.playWhenReady)
        assertEquals("surface remains intact after retry", 0, player.clearVideoSurfaceCalls)
    }
}
