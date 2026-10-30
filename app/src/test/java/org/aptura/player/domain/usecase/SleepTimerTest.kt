package org.aptura.player.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.aptura.player.domain.model.DomainPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)
    private val playbackState = MutableStateFlow<DomainPlaybackState>(
        DomainPlaybackState.Ready(0L, 60_000L, isPlaying = true, playbackSpeed = 1f)
    )

    private var paused = false
    private var volume = 1f
    private var stopAfterCurrentItem = false

    private fun createTimer(
        clock: () -> Long = { testDispatcher.scheduler.currentTime }
    ): SleepTimer = SleepTimer(
        scope = scope,
        clock = clock,
        playbackState = playbackState,
        pause = { paused = true },
        setVolume = { volume = it },
        setStopAfterCurrentItem = { stopAfterCurrentItem = it }
    )

    @Test
    fun start_countsDown_fadesSound_andPausesAtDeadline() = runTest(testDispatcher) {
        val timer = createTimer()
        timer.start(1) // 60,000 ms
        runCurrent()

        assertEquals(SleepTimerState.Counting(60_000L), timer.state.value)
        assertEquals(1f, volume, 0f)
        assertFalse(paused)

        // Advance to 50s (fade starts at deadline - 10s = 50s)
        advanceTimeBy(49_500L)
        runCurrent()
        assertEquals(1f, volume, 0f)

        advanceTimeBy(5_000L) // 54.5s
        runCurrent()
        assertTrue("Volume should be fading", volume in 0.4f..0.6f)
        assertFalse(paused)

        advanceTimeBy(6_000L) // 60.5s -> deadline reached
        runCurrent()
        assertTrue("Should be paused", paused)
        assertEquals(SleepTimerState.Expired, timer.state.value)
        assertEquals("Volume restored to 1.0f after pause", 1f, volume, 0f)
    }

    @Test
    fun extend_whileRunning_movesDeadline() = runTest(testDispatcher) {
        val timer = createTimer()
        timer.start(1) // 60s
        runCurrent()

        advanceTimeBy(55_000L) // in fade
        runCurrent()
        assertTrue(volume < 1f)

        timer.extend(5) // + 300s = 360s
        runCurrent()
        assertEquals(SleepTimerState.Counting(360_000L), timer.state.value)
        assertEquals("Volume restored to 1.0f on extend", 1f, volume, 0f)
        assertFalse(paused)

        advanceTimeBy(300_000L) // at 355s
        runCurrent()
        assertTrue("Fading again near new deadline", volume < 1f)

        advanceTimeBy(10_000L) // at 365s
        runCurrent()
        assertTrue(paused)
        assertEquals(SleepTimerState.Expired, timer.state.value)
    }

    @Test
    fun extend_whenExpired_startsFreshTimerFromNow() = runTest(testDispatcher) {
        val timer = createTimer()
        timer.start(1)
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(SleepTimerState.Expired, timer.state.value)

        timer.extend(5) // from currentTime (60_000) + 300_000 = 360_000
        runCurrent()
        assertEquals(SleepTimerState.Counting(360_000L), timer.state.value)

        advanceTimeBy(300_000L)
        runCurrent()
        assertEquals(SleepTimerState.Expired, timer.state.value)
    }

    @Test
    fun cancel_restoresVolume_andTurnsOff() = runTest(testDispatcher) {
        val timer = createTimer()
        timer.start(1)
        advanceTimeBy(55_000L) // in fade
        runCurrent()
        assertTrue(volume < 1f)

        timer.cancel()
        runCurrent()
        assertEquals(SleepTimerState.Off, timer.state.value)
        assertEquals(1f, volume, 0f)
        assertFalse(paused)
    }

    @Test
    fun dismissOffer_clearsExpiredState() = runTest(testDispatcher) {
        val timer = createTimer()
        timer.start(1)
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(SleepTimerState.Expired, timer.state.value)

        timer.dismissOffer()
        runCurrent()
        assertEquals(SleepTimerState.Off, timer.state.value)
    }

    @Test
    fun stopAtEndOfItem_armsPlayer_andDisarmsWhenItemEnds() = runTest(testDispatcher) {
        val timer = createTimer()
        timer.stopAtEndOfItem()
        runCurrent()

        assertEquals(SleepTimerState.AtEndOfItem, timer.state.value)
        assertTrue(stopAfterCurrentItem)

        // Item ends
        playbackState.value = DomainPlaybackState.Ended(60_000L)
        runCurrent()

        assertEquals(SleepTimerState.StoppedAtEndOfItem, timer.state.value)
        assertFalse("Should disarm after current item ends", stopAfterCurrentItem)

        // Next item starts playing
        playbackState.value = DomainPlaybackState.Ready(0L, 60_000L, isPlaying = true, playbackSpeed = 1f)
        runCurrent()

        assertEquals(SleepTimerState.Off, timer.state.value)
    }

    @Test
    fun cancel_whileStopAtEndOfItem_disarmsPlayer() = runTest(testDispatcher) {
        val timer = createTimer()
        timer.stopAtEndOfItem()
        runCurrent()
        assertTrue(stopAfterCurrentItem)

        timer.cancel()
        runCurrent()
        assertEquals(SleepTimerState.Off, timer.state.value)
        assertFalse(stopAfterCurrentItem)
    }
}
