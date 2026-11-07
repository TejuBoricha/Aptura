package org.aptura.player.data.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackQueue
import org.aptura.player.domain.model.DomainPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit FUNC-05: progress used to be saved only by the screen's view model, so once the task was swiped away and the
 * sound went on, nothing wrote it again. The recorder lives with the player, and these tests run it with no screen
 * and no view model at all, from the player's own state and queue.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackProgressRecorderTest {

    private data class Saved(val uri: String, val positionMs: Long, val isCompleted: Boolean)

    private fun video(n: Int) = DomainMediaItem(
        uri = "content://media/$n",
        title = "Video $n",
        durationMs = 60_000L,
        sizeBytes = 1000L,
        mimeType = "video/mp4",
        dateAddedSec = 1000L + n,
        folderPath = "Movies"
    )

    private fun playing(positionMs: Long) =
        DomainPlaybackState.Ready(positionMs = positionMs, durationMs = 60_000L, isPlaying = true)

    private fun paused(positionMs: Long) =
        DomainPlaybackState.Ready(positionMs = positionMs, durationMs = 60_000L, isPlaying = false)

    private class Rig(
        val state: MutableStateFlow<DomainPlaybackState> = MutableStateFlow(DomainPlaybackState.Idle),
        val queue: MutableStateFlow<DomainPlaybackQueue> = MutableStateFlow(DomainPlaybackQueue())
    ) {
        val saved = mutableListOf<Saved>()
        var failNextSave = false
    }

    private fun TestScope.start(rig: Rig, firstVideo: Int = 1, queueSize: Int = 3): PlaybackProgressRecorder {
        rig.queue.value = DomainPlaybackQueue(items = (1..queueSize).map { video(it) }, currentIndex = firstVideo - 1)
        val recorder = PlaybackProgressRecorder(
            playbackState = rig.state,
            playbackQueue = rig.queue,
            save = { uri, position, completed ->
                if (rig.failNextSave) {
                    rig.failNextSave = false
                    error("the database was busy")
                }
                rig.saved += Saved(uri, position, completed)
            }
        )
        recorder.start(backgroundScope)
        runCurrent()
        return recorder
    }

    @Test
    fun whilePlaying_itSavesACheckpointEveryFiveSeconds_andNotBefore() = runTest(StandardTestDispatcher()) {
        val rig = Rig()
        start(rig)

        rig.state.value = playing(1_000L); runCurrent()
        rig.state.value = playing(4_999L); runCurrent()
        assertTrue("nothing is saved inside the first five seconds", rig.saved.isEmpty())

        rig.state.value = playing(5_000L); runCurrent()
        assertEquals(listOf(Saved("content://media/1", 5_000L, false)), rig.saved)

        rig.state.value = playing(9_000L); runCurrent()
        assertEquals("four more seconds is not a checkpoint yet", 1, rig.saved.size)

        rig.state.value = playing(10_000L); runCurrent()
        assertEquals(Saved("content://media/1", 10_000L, false), rig.saved.last())
    }

    @Test
    fun pausing_savesWhereItStopped() = runTest(StandardTestDispatcher()) {
        val rig = Rig()
        start(rig)

        rig.state.value = playing(2_000L); runCurrent()
        rig.state.value = paused(2_500L); runCurrent()

        assertEquals(listOf(Saved("content://media/1", 2_500L, false)), rig.saved)
    }

    @Test
    fun endingAVideo_marksItCompleted_atItsLength() = runTest(StandardTestDispatcher()) {
        val rig = Rig()
        start(rig)

        rig.state.value = playing(59_000L); runCurrent()
        rig.state.value = DomainPlaybackState.Ended(60_000L); runCurrent()

        assertEquals(Saved("content://media/1", 60_000L, true), rig.saved.last())
    }

    @Test
    fun movingToAnotherVideo_savesTheOneLeft_underItsOwnName() = runTest(StandardTestDispatcher()) {
        val rig = Rig()
        start(rig)
        rig.state.value = playing(3_000L); runCurrent()

        // Next, by the notification or a headset: the queue moves on, then the new video reports its own position.
        rig.queue.value = rig.queue.value.copy(currentIndex = 1); runCurrent()
        rig.state.value = playing(0L); runCurrent()

        assertEquals(
            "the video that was left keeps where it stopped, and the new one is not credited with it",
            listOf(Saved("content://media/1", 3_000L, false)),
            rig.saved
        )
    }

    @Test
    fun aVideoThatEnded_keepsItsCompletion_whenTheQueueMovesOn() = runTest(StandardTestDispatcher()) {
        val rig = Rig()
        start(rig)
        rig.state.value = playing(59_000L); runCurrent()
        rig.state.value = DomainPlaybackState.Ended(60_000L); runCurrent()

        // Up Next, or the person, moves on to the next video.
        rig.queue.value = rig.queue.value.copy(currentIndex = 1); runCurrent()
        rig.state.value = playing(0L); runCurrent()

        assertEquals(
            "the 59 s checkpoint, then completion; leaving the finished video must not add a save as unfinished",
            listOf(Saved("content://media/1", 59_000L, false), Saved("content://media/1", 60_000L, true)),
            rig.saved
        )
    }

    @Test
    fun theNewVideo_startsItsOwnFiveSecondCount() = runTest(StandardTestDispatcher()) {
        val rig = Rig()
        start(rig)
        rig.state.value = playing(20_000L); runCurrent() // a checkpoint for video 1
        rig.queue.value = rig.queue.value.copy(currentIndex = 1); runCurrent()
        rig.saved.clear()

        rig.state.value = playing(1_000L); runCurrent()
        assertTrue(rig.saved.isEmpty())
        rig.state.value = playing(5_000L); runCurrent()

        assertEquals(listOf(Saved("content://media/2", 5_000L, false)), rig.saved)
    }

    @Test
    fun idleBufferingAndErrors_saveNothing() = runTest(StandardTestDispatcher()) {
        val rig = Rig()
        start(rig)

        rig.state.value = DomainPlaybackState.Buffering(30_000L); runCurrent()
        rig.state.value = DomainPlaybackState.Error("X", "failed", isRecoverable = true); runCurrent()
        rig.state.value = DomainPlaybackState.Idle; runCurrent()

        assertTrue(rig.saved.isEmpty())
    }

    @Test
    fun aPositionOfZero_isNeverSaved() = runTest(StandardTestDispatcher()) {
        val rig = Rig()
        start(rig)

        rig.state.value = playing(0L); runCurrent()
        rig.state.value = paused(0L); runCurrent()

        assertTrue("a video that has not started has no progress to keep", rig.saved.isEmpty())
    }

    @Test
    fun aFailedSave_doesNotStopTheRecording() = runTest(StandardTestDispatcher()) {
        val rig = Rig()
        start(rig)
        rig.failNextSave = true

        rig.state.value = playing(5_000L); runCurrent() // this save fails
        rig.state.value = playing(10_000L); runCurrent()

        assertEquals(
            "the next checkpoint is still written",
            listOf(Saved("content://media/1", 10_000L, false)),
            rig.saved
        )
    }
}
