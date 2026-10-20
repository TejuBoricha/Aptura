package org.aptura.player.data.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackState
import org.aptura.player.domain.model.PlayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Unit tests for Media3PlaybackController.
 * Verifies Media3 event mapping into domain states, seek navigation rules, and track selections.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Media3PlaybackControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class MockPlayerState : InvocationHandler {
        val listeners = mutableListOf<Player.Listener>()
        var currentPosition: Long = 0L
        var duration: Long = 60000L
        var playbackState: Int = Player.STATE_IDLE
        var isPlaying: Boolean = false
        var playbackParameters = PlaybackParameters(1.0f)
        var repeatMode: Int = Player.REPEAT_MODE_OFF
        var shuffleModeEnabled: Boolean = false
        var lastSeekPosition: Long? = null
        var currentMediaItem: MediaItem? = null

        fun fireStateChange(newState: Int) {
            playbackState = newState
            listeners.forEach { it.onPlaybackStateChanged(newState) }
        }

        fun fireIsPlayingChange(playing: Boolean) {
            isPlaying = playing
            listeners.forEach { it.onIsPlayingChanged(playing) }
        }

        fun fireError(exception: PlaybackException) {
            listeners.forEach { it.onPlayerError(exception) }
        }

        override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
            return when (method.name) {
                "addListener" -> {
                    (args?.get(0) as? Player.Listener)?.let { listeners.add(it) }
                    null
                }
                "removeListener" -> {
                    listeners.remove(args?.get(0))
                    null
                }
                "getCurrentPosition" -> currentPosition
                "getDuration" -> duration
                "getPlaybackState" -> playbackState
                "isPlaying" -> isPlaying
                "getPlaybackParameters" -> playbackParameters
                "setPlaybackParameters" -> {
                    playbackParameters = args?.get(0) as PlaybackParameters
                    listeners.forEach { it.onPlaybackParametersChanged(playbackParameters) }
                    null
                }
                "setPlaybackSpeed" -> {
                    val speed = args?.get(0) as Float
                    playbackParameters = PlaybackParameters(speed)
                    listeners.forEach { it.onPlaybackParametersChanged(playbackParameters) }
                    null
                }
                "seekTo" -> {
                    lastSeekPosition = args?.get(0) as Long
                    currentPosition = lastSeekPosition ?: 0L
                    null
                }
                "setMediaItem" -> {
                    currentMediaItem = args?.get(0) as MediaItem
                    null
                }
                "prepare" -> null
                "play" -> {
                    isPlaying = true
                    fireIsPlayingChange(true)
                    null
                }
                "pause" -> {
                    isPlaying = false
                    fireIsPlayingChange(false)
                    null
                }
                "setRepeatMode" -> {
                    repeatMode = args?.get(0) as Int
                    null
                }
                "setShuffleModeEnabled" -> {
                    shuffleModeEnabled = args?.get(0) as Boolean
                    null
                }
                "release" -> null
                else -> null
            }
        }
    }

    private fun createMockPlayer(): Pair<Player, MockPlayerState> {
        val state = MockPlayerState()
        val player = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
            state
        ) as Player
        return Pair(player, state)
    }

    private val sampleItem1 = DomainMediaItem(
        uri = "content://media/1",
        title = "Track 1",
        durationMs = 60000L,
        sizeBytes = 1048576L,
        mimeType = "video/mp4"
    )

    private val sampleItem2 = DomainMediaItem(
        uri = "content://media/2",
        title = "Track 2",
        durationMs = 120000L,
        sizeBytes = 2097152L,
        mimeType = "video/mp4"
    )

    @Test
    fun playbackState_mapsMedia3StatesAccurately() = runTest(testDispatcher) {
        val (player, state) = createMockPlayer()
        val controller = Media3PlaybackController(player, testScope)

        controller.playbackState.test {
            // Initial state is Idle
            assertEquals(DomainPlaybackState.Idle, awaitItem())

            // Transition to Buffering
            state.fireStateChange(Player.STATE_BUFFERING)
            val buffering = awaitItem()
            assertTrue(buffering is DomainPlaybackState.Buffering)

            // Transition to Ready + Playing
            state.playbackState = Player.STATE_READY
            state.currentPosition = 15000L
            state.duration = 60000L
            state.fireIsPlayingChange(true)
            val ready = awaitItem()
            assertTrue(ready is DomainPlaybackState.Ready)
            val readyState = ready as DomainPlaybackState.Ready
            assertEquals(15000L, readyState.positionMs)
            assertEquals(60000L, readyState.durationMs)
            assertTrue(readyState.isPlaying)

            // Transition to Ended
            state.fireStateChange(Player.STATE_ENDED)
            val ended = awaitItem()
            assertTrue(ended is DomainPlaybackState.Ended)
            assertEquals(60000L, (ended as DomainPlaybackState.Ended).durationMs)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun skipToPrevious_restartsCurrentItem_whenPastThreeSeconds() = runTest(testDispatcher) {
        val (player, state) = createMockPlayer()
        val controller = Media3PlaybackController(player, testScope)

        controller.playMediaItem(sampleItem1)
        state.currentPosition = 5000L // Past 3000ms threshold

        controller.skipToPrevious()
        assertEquals(0L, state.lastSeekPosition)
    }

    @Test
    fun trackSelection_updatesSubtitleState() = runTest(testDispatcher) {
        val (player, _) = createMockPlayer()
        val controller = Media3PlaybackController(player, testScope)

        controller.trackSelection.test {
            val initial = awaitItem()
            assertFalse(initial.subtitlesEnabled)

            // Enable subtitle track 2
            controller.selectSubtitleTrack(2)
            val enabled = awaitItem()
            assertTrue(enabled.subtitlesEnabled)
            assertEquals(2, enabled.selectedSubtitleTrackIndex)

            // Disable subtitles
            controller.selectSubtitleTrack(null)
            val disabled = awaitItem()
            assertFalse(disabled.subtitlesEnabled)
            assertEquals(null, disabled.selectedSubtitleTrackIndex)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setPlaybackSpeed_clampsWithinBounds() = runTest(testDispatcher) {
        val (player, state) = createMockPlayer()
        val controller = Media3PlaybackController(player, testScope)

        // Normal speed
        controller.setPlaybackSpeed(1.5f)
        assertEquals(1.5f, state.playbackParameters.speed, 0.01f)

        // Below minimum clamp 0.1f
        controller.setPlaybackSpeed(0.01f)
        assertEquals(0.1f, state.playbackParameters.speed, 0.01f)

        // Above maximum clamp 4.0f
        controller.setPlaybackSpeed(10.0f)
        assertEquals(4.0f, state.playbackParameters.speed, 0.01f)
    }

    @Test
    fun setPlayMode_updatesRepeatAndShuffleOnPlayer() = runTest(testDispatcher) {
        val (player, state) = createMockPlayer()
        val controller = Media3PlaybackController(player, testScope)

        controller.setPlayMode(PlayMode.REPEAT_ONE)
        assertEquals(Player.REPEAT_MODE_ONE, state.repeatMode)
        assertFalse(state.shuffleModeEnabled)

        controller.setPlayMode(PlayMode.SHUFFLE)
        assertEquals(Player.REPEAT_MODE_OFF, state.repeatMode)
        assertTrue(state.shuffleModeEnabled)
    }
}
