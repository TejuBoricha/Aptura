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
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * A video that was asked to play and never does (the owner's Samsung S24: a paused, black player, queue 558, no error,
 * nothing to say why). That must not be possible to end in: the video is started again on a brand-new player, then on
 * a brand-new player with the software decoder, and only when both fail does the player say so. Everything that is
 * merely slow, paused or held back by the system is left alone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Media3PlaybackControllerStallTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** What happened to which player, in order, across every player a test makes. */
    private val events = mutableListOf<String>()

    /** A player that does what it is told about play-when-ready but only reports "playing" when the test says so. */
    private inner class StuckPlayer(val name: String = "player") : InvocationHandler {
        val listeners = mutableListOf<Player.Listener>()
        var playWhenReady = false
        var isPlaying = false
        var state = Player.STATE_IDLE
        var suppression = Player.PLAYBACK_SUPPRESSION_REASON_NONE
        val items = mutableListOf<MediaItem>()
        var index = 0
        var position = 0L
        var error: PlaybackException? = null
        var stops = 0
        var prepares = 0
        var released = false
        var buffered = 0L
        var loading = false
        val calls = mutableListOf<String>()

        fun startPlaying() {
            state = Player.STATE_READY
            isPlaying = true
            listeners.toList().forEach { it.onPlaybackStateChanged(Player.STATE_READY) }
            listeners.toList().forEach { it.onIsPlayingChanged(true) }
        }

        fun changeState(newState: Int) {
            state = newState
            listeners.toList().forEach { it.onPlaybackStateChanged(newState) }
        }

        fun fail(error: PlaybackException) {
            this.error = error
            listeners.toList().forEach { it.onPlayerError(error) }
        }

        private fun note(call: String) {
            calls += call
            events += "$name:$call"
        }

        override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? = when (method.name) {
            "addListener" -> { (args?.get(0) as? Player.Listener)?.let { listeners.add(it) }; null }
            "removeListener" -> { listeners.remove(args?.get(0)); null }
            "getPlayWhenReady" -> playWhenReady
            "isPlaying" -> isPlaying
            "getPlaybackState" -> state
            "getPlaybackSuppressionReason" -> suppression
            "getMediaItemCount" -> items.size
            "getCurrentMediaItem" -> items.getOrNull(index)
            "getCurrentMediaItemIndex" -> index
            "getMediaItemAt" -> items[args!![0] as Int]
            "hasNextMediaItem" -> index < items.lastIndex
            "hasPreviousMediaItem" -> index > 0
            "getNextMediaItemIndex" -> if (index < items.lastIndex) index + 1 else -1
            "getPreviousMediaItemIndex" -> if (index > 0) index - 1 else -1
            "getCurrentPosition" -> position
            "getPlayerError" -> error
            "getBufferedPosition" -> buffered
            "isLoading" -> loading
            "setMediaItems" -> {
                val list = args?.get(0) as List<*>
                items.clear()
                items.addAll(list.filterIsInstance<MediaItem>())
                index = (args.getOrNull(1) as? Int) ?: 0
                position = (args.getOrNull(2) as? Long) ?: 0L
                note("setMediaItems(${items.size} at $index, ${position}ms)")
                null
            }
            "addMediaItems" -> {
                val at = if (args!!.size == 2) args[0] as Int else items.size
                val list = (args.last() as List<*>).filterIsInstance<MediaItem>()
                items.addAll(at, list)
                if (at <= index && items.size > list.size) index += list.size
                note("addMediaItems(${list.size} at $at)")
                null
            }
            "seekToNextMediaItem" -> { note("seekToNextMediaItem"); index++; null }
            "prepare" -> { prepares++; note("prepare"); changeState(Player.STATE_BUFFERING); null }
            "stop" -> { stops++; note("stop"); isPlaying = false; changeState(Player.STATE_IDLE); null }
            "release" -> { released = true; note("release"); null }
            "play" -> { playWhenReady = true; note("play"); listeners.toList().forEach { it.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }; null }
            "pause" -> { note("pause"); playWhenReady = false; isPlaying = false; listeners.toList().forEach { it.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }; null }
            "setTrackSelectionParameters" -> { note("setTrackSelectionParameters"); null }
            "setPlaybackParameters" -> { note("setPlaybackParameters"); null }
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

    private fun proxyOf(handler: StuckPlayer) = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java), handler) as Player

    private fun video(n: Int) = DomainMediaItem(uri = "content://media/$n", title = "Video $n", durationMs = 60_000L, sizeBytes = 1_000_000L, mimeType = "video/mp4")

    private val video = video(1)

    /** Every player the controller asked its factory for, in order. */
    private val made = mutableListOf<StuckPlayer>()

    private fun controllerWith(handler: StuckPlayer, withFactory: Boolean = true): Media3PlaybackController =
        Media3PlaybackController(
            player = proxyOf(handler),
            scope = scope,
            mainDispatcher = dispatcher,
            enablePositionTicker = false,
            playerFactory = if (withFactory) {
                {
                    val fresh = StuckPlayer("fresh${made.size + 1}")
                    made += fresh
                    PlayerParts(proxyOf(fresh))
                }
            } else null
        )

    private val rung = PLAYBACK_START_RUNG_MS + 1

    /** The handler behind the player the controller is using now. */
    private fun liveHandler(controller: Media3PlaybackController): StuckPlayer =
        (Proxy.getInvocationHandler(controller.player) as StuckPlayer)

    @Test
    fun aVideoThatNeverStarts_isStartedAgainOnAFreshPlayer_thenOnAFreshPlayerWithTheSoftwareDecoder_thenReported() = runTest(dispatcher) {
        val first = StuckPlayer("first")
        val controller = controllerWith(first)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        assertEquals("nothing happens before the wait is over", 0, made.size)

        advanceTimeBy(rung)
        runCurrent()
        assertEquals("a fresh player is made", 1, made.size)
        assertNotSame(first, liveHandler(controller))
        assertTrue("the stuck one is let go", first.released)
        assertEquals("the new one is asked for the same video", 1, made[0].items.size)
        assertEquals(1, made[0].prepares)
        assertTrue(made[0].playWhenReady)
        assertEquals("hardware first", 0, controller.currentDecoderStrategy)
        assertFalse(controller.playbackState.value is DomainPlaybackState.Error)

        advanceTimeBy(rung)
        runCurrent()
        assertEquals("then another, with the software decoder", 2, made.size)
        assertEquals(2, controller.currentDecoderStrategy)
        assertTrue(controller.isCompatibilityMode.value)
        assertTrue(made[0].released)
        assertFalse("not an error yet, there is nothing left to try only after this", controller.playbackState.value is DomainPlaybackState.Error)

        advanceTimeBy(rung)
        runCurrent()
        val state = controller.playbackState.value
        assertTrue("then it says so: $state", state is DomainPlaybackState.Error)
        assertEquals("PLAYBACK_STALLED", (state as DomainPlaybackState.Error).errorCode)
        assertEquals("nothing more is made once it has said so", 2, made.size)
        assertTrue("the reason names what the player was doing", controller.failureReason.value.orEmpty().contains("state=BUFFERING"))
    }

    @Test
    fun theNewPlayerCarriesTheSettingsOver_andTheSessionIsToldBeforeTheOldOneIsReleased() = runTest(dispatcher) {
        val first = StuckPlayer("first")
        val controller = controllerWith(first)
        controller.addPlayerReplacedListener { events += "told" }

        controller.playQueue(listOf(video), 0)
        runCurrent()
        events.clear()
        advanceTimeBy(rung)
        runCurrent()

        assertTrue("the track choices are carried over: ${made[0].calls}", made[0].calls.contains("setTrackSelectionParameters"))
        assertTrue("so is the speed: ${made[0].calls}", made[0].calls.contains("setPlaybackParameters"))
        val told = events.indexOf("told")
        val released = events.indexOf("first:release")
        assertTrue("told=$told released=$released in $events", told in 0 until released)
        assertSame("the screen follows the new player", controller.player, controller.currentPlayer.value)
    }

    @Test
    fun aVideoThatStartsInTime_isLeftAlone() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        advanceTimeBy(3_000)
        player.startPlaying()
        advanceTimeBy(rung * 6)
        runCurrent()

        assertEquals(0, made.size)
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
        advanceTimeBy(rung * 6)
        runCurrent()

        assertEquals(0, made.size)
        assertFalse(controller.playbackState.value is DomainPlaybackState.Error)
    }

    @Test
    fun aPlayerTheSystemIsHoldingBack_isNotStuck() = runTest(dispatcher) {
        val player = StuckPlayer().apply { suppression = Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS }
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        advanceTimeBy(rung * 6)
        runCurrent()

        assertEquals(0, made.size)
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
        advanceTimeBy(rung * 6)
        runCurrent()

        assertEquals("the watchdog does not pile a second recovery on an error", 0, made.size)
    }

    @Test
    fun aPlayerThatTimedOut_isReplacedAtOnce() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        player.fail(PlaybackException("detaching the surface timed out", null, PlaybackException.ERROR_CODE_TIMEOUT))
        runCurrent()

        assertEquals("a player that timed out cannot be repaired, only replaced", 1, made.size)
        assertTrue(player.released)
    }

    @Test
    fun withoutAWayToMakeAPlayer_theStuckOneIsRestartedInPlace_andTheLadderStillEnds() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player, withFactory = false)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        advanceTimeBy(rung)
        runCurrent()
        assertEquals(1, player.stops)
        advanceTimeBy(rung)
        runCurrent()
        assertEquals(2, player.stops)
        assertEquals(2, controller.currentDecoderStrategy)
        advanceTimeBy(rung)
        runCurrent()

        assertEquals("PLAYBACK_STALLED", (controller.playbackState.value as DomainPlaybackState.Error).errorCode)
    }

    @Test
    fun retryingAfterTheMessage_startsOnAFreshPlayer_andEndsAgainAfterOneMoreTry() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        repeat(3) { advanceTimeBy(rung); runCurrent() }
        assertTrue(controller.playbackState.value is DomainPlaybackState.Error)
        val made1 = made.size

        controller.retryPlayback()
        runCurrent()
        assertFalse("the message goes away when it is retried", controller.playbackState.value is DomainPlaybackState.Error)
        assertEquals("a retry starts on a new player", made1 + 1, made.size)

        advanceTimeBy(rung)
        runCurrent()
        assertEquals("one more try, then", made1 + 2, made.size)
        advanceTimeBy(rung)
        runCurrent()
        assertTrue(controller.playbackState.value is DomainPlaybackState.Error)
    }

    @Test
    fun aRetryThatPlays_endsTheStall() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)

        controller.playQueue(listOf(video), 0)
        runCurrent()
        repeat(3) { advanceTimeBy(rung); runCurrent() }
        controller.retryPlayback()
        runCurrent()
        liveHandler(controller).startPlaying()
        advanceTimeBy(rung * 3)
        runCurrent()

        assertFalse(controller.playbackState.value is DomainPlaybackState.Error)
    }

    @Test
    fun aStallThatAFreshStartCured_makesEveryLaterStartFreshAndHoldsBackTheRestOfTheQueue() = runTest(dispatcher) {
        val first = StuckPlayer("first")
        val controller = controllerWith(first)

        controller.playQueue(listOf(video(1)), 0)
        runCurrent()
        advanceTimeBy(rung)
        runCurrent()
        liveHandler(controller).startPlaying() // the fresh player started it
        runCurrent()
        assertEquals(1, made.size)

        // The next video, picked from a folder of five.
        val folder = (1..5).map { video(it) }
        controller.playQueue(folder, 3)
        runCurrent()

        assertEquals("a new player before the video is loaded", 2, made.size)
        val player = made[1]
        assertEquals("only the video asked for is loaded at first: ${player.calls}", 1, player.items.size)
        assertTrue(player.calls.contains("setMediaItems(1 at 0, 0ms)"))
        val queue = controller.playbackQueue.value
        assertEquals("the queue is still all five", 5, queue.items.size)
        assertEquals("and still on the video that was picked", 3, queue.currentIndex)
        assertEquals(true, queue.nextAvailable)
        assertEquals(true, queue.previousAvailable)

        player.startPlaying()
        advanceTimeBy(QUEUE_RESTORE_AFTER_MS + 1)
        runCurrent()

        assertEquals("the rest follows once it plays", 5, player.items.size)
        assertEquals("the playing video did not move", "content://media/4", player.items[player.index].mediaId)
        assertEquals(3, controller.playbackQueue.value.currentIndex)
    }

    @Test
    fun theRestOfTheQueue_isLoadedBeforeAnythingThatNeedsIt() = runTest(dispatcher) {
        val first = StuckPlayer("first")
        val controller = controllerWith(first)
        controller.playQueue(listOf(video(1)), 0)
        runCurrent()
        advanceTimeBy(rung)
        runCurrent()
        liveHandler(controller).startPlaying()
        runCurrent()

        controller.playQueue((1..5).map { video(it) }, 1)
        runCurrent()
        val player = made[1]
        player.startPlaying()
        runCurrent()
        assertEquals("not yet: the video has only just started", 1, player.items.size)

        controller.skipToNext()

        val added = player.calls.indexOfFirst { it.startsWith("addMediaItems") }
        val skipped = player.calls.indexOf("seekToNextMediaItem")
        assertTrue("the rest was loaded first: ${player.calls}", added in 0 until skipped)
        assertEquals(5, player.items.size)
    }


    @Test
    fun loadingThatKeepsArriving_isGivenTimeToFinish_andThenReplacedAnyway() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.playQueue(listOf(video), 0)
        runCurrent()
        player.loading = true

        // A slow disk: a little more arrives before every check, and there is never enough to start.
        repeat(MAX_SLOW_LOAD_WAITS) { n ->
            player.buffered = 600L * (n + 1)
            advanceTimeBy(rung)
            runCurrent()
            assertEquals("a slow load is not a frozen player (wait ${n + 1})", 0, made.size)
        }
        player.buffered = 600L * (MAX_SLOW_LOAD_WAITS + 1)
        advanceTimeBy(rung)
        runCurrent()
        assertEquals("but waiting is not for ever", 1, made.size)
    }

    @Test
    fun aPlayerThatHasEnoughLoadedAndStillDoesNotStart_isFrozen_andReplacedAtOnce() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.playQueue(listOf(video), 0)
        runCurrent()
        player.loading = true
        player.buffered = ENOUGH_LOADED_TO_START_MS + 500

        advanceTimeBy(rung)
        runCurrent()

        assertEquals(1, made.size)
    }

    @Test
    fun loadingThatStoppedArriving_isFrozen() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.playQueue(listOf(video), 0)
        runCurrent()
        player.loading = true
        player.buffered = 600L

        advanceTimeBy(rung)
        runCurrent()
        assertEquals("some data came in, so one more wait", 0, made.size)
        advanceTimeBy(rung)
        runCurrent()
        assertEquals("nothing more came in", 1, made.size)
    }

    @Test
    fun leavingThePlayer_letsTheDecoderGo_andStartsNoRecovery() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        controller.playQueue(listOf(video), 0)
        runCurrent()
        player.startPlaying()
        player.calls.clear()

        controller.stopForDismissal()
        advanceTimeBy(rung * 6)
        runCurrent()

        val paused = player.calls.indexOf("pause")
        val stopped = player.calls.indexOf("stop")
        assertTrue("paused then stopped, in that order: ${player.calls}", paused in 0 until stopped)
        assertEquals("the player is kept, only its decoder is let go", false, player.released)
        assertEquals("a player that was left is not a stalled one", 0, made.size)
        assertFalse(controller.playbackState.value is DomainPlaybackState.Error)
    }

    @Test
    fun theReportCarriesWhereThePlayerStopped_theStepsAndTheThreads() = runTest(dispatcher) {
        val player = StuckPlayer()
        val controller = controllerWith(player)
        ApturaDiagnostics.ringBuffer.clear()

        controller.playQueue(listOf(video), 0)
        runCurrent()
        advanceTimeBy(rung)
        runCurrent()

        val recovery = ApturaDiagnostics.ringBuffer.getAllEvents()
            .last { it.code == EventCode.EV_010 && it.payload["strategy"] == "stall_recovery" }
        assertTrue("the steps are there: ${recovery.payload}", recovery.payload["trace"].orEmpty().contains("requested items=1 start=0"))
        assertTrue("the threads are there: ${recovery.payload}", recovery.payload.containsKey("threads"))
        assertTrue(recovery.payload["reason"].orEmpty().contains("buffered="))
        assertTrue(recovery.payload["reason"].orEmpty().contains("loading="))

        repeat(2) { advanceTimeBy(rung); runCurrent() }
        val stall = ApturaDiagnostics.ringBuffer.getAllEvents()
            .last { it.code == EventCode.EV_011 && it.payload["errorCode"] == "PLAYBACK_STALLED" }
        assertTrue("the final report has the steps too: ${stall.payload}", stall.payload.containsKey("trace"))
    }
}
