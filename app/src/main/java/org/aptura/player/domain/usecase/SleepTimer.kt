package org.aptura.player.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aptura.player.domain.model.DomainPlaybackState

/** What the sleep timer (FR-CTRL-009) is doing. */
sealed interface SleepTimerState {
    data object Off : SleepTimerState

    /** Counting down to one monotonic deadline; the sound fades out over the last [SleepTimer.FADE_MS]. */
    data class Counting(val endsAtElapsedRealtime: Long) : SleepTimerState

    /** The countdown paused playback. The player offers "+5 min" until playback resumes or the video changes. */
    data object Expired : SleepTimerState

    /** "At the end of this video": playback pauses when the playing item ends, even when it is set to repeat. */
    data object AtEndOfItem : SleepTimerState

    /**
     * "At the end of this video" has paused playback. Shown as off; it lets whoever handles the end of the item see
     * that the timer stopped it (no Up Next) and becomes [Off] as soon as playback moves on.
     */
    data object StoppedAtEndOfItem : SleepTimerState
}

/**
 * The sleep timer, owned by the playback controller rather than by a screen.
 *
 * It used to run in the player ViewModel's scope. Swiping the app out of Recents destroys the activity and clears
 * the ViewModel, but `ApturaPlaybackService` keeps playing, so the timer died while the sound went on all night
 * (Galaxy S24 device test, 2026-10-29). This object lives as long as the player: [scope] must be the controller's,
 * never a screen's.
 *
 * Pure Kotlin: every effect on the player goes through the injected functions, and [clock] is a monotonic clock
 * (`SystemClock.elapsedRealtime` in the app, the virtual scheduler's time in tests). Call it from the thread
 * [scope] runs on.
 */
class SleepTimer(
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val playbackState: StateFlow<DomainPlaybackState>,
    private val pause: suspend () -> Unit,
    /** The player's own gain, 0..1, under the system volume. */
    private val setVolume: suspend (Float) -> Unit,
    /** Arms or disarms the player's pause at the end of the playing item. */
    private val setStopAfterCurrentItem: suspend (Boolean) -> Unit
) {
    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var job: Job? = null
    private var closed = false

    /** Pauses playback [minutes] from now, fading the sound out first. Replaces whatever was set. */
    fun start(minutes: Int) {
        count(clock() + minutes * MINUTE_MS)
    }

    /**
     * "+5 min". A running countdown keeps its deadline and gains [minutes]; an expired one starts a fresh [minutes]
     * from now. Resuming playback after expiry is the caller's choice, not the timer's. Does nothing otherwise.
     */
    fun extend(minutes: Int) {
        when (val current = _state.value) {
            is SleepTimerState.Counting -> count(current.endsAtElapsedRealtime + minutes * MINUTE_MS)
            SleepTimerState.Expired -> count(clock() + minutes * MINUTE_MS)
            else -> Unit
        }
    }

    /** Pauses when the playing item ends; Up Next does not start. Replaces a countdown. */
    fun stopAtEndOfItem() {
        if (_state.value == SleepTimerState.AtEndOfItem) return
        replaceRunningTimer()
        _state.value = SleepTimerState.AtEndOfItem
        job = scope.launch {
            setStopAfterCurrentItem(true)
            // An item that has already ended is not "this video": wait for the next one to start first.
            playbackState.first { it !is DomainPlaybackState.Ended }
            playbackState.first { it is DomainPlaybackState.Ended }
            // One-shot: the player has paused, so it must not stop after every later item too.
            setStopAfterCurrentItem(false)
            _state.value = SleepTimerState.StoppedAtEndOfItem
            playbackState.first { it !is DomainPlaybackState.Ended }
            _state.value = SleepTimerState.Off
        }
    }

    /** Turns the timer off. A fade in progress is undone; the player stops arming its end-of-item pause. */
    fun cancel() {
        replaceRunningTimer()
        _state.value = SleepTimerState.Off
    }

    /** Drops the "+5 min" offer; does nothing unless the countdown has fired. */
    fun dismissOffer() {
        _state.compareAndSet(SleepTimerState.Expired, SleepTimerState.Off)
    }

    /** Stops the timer for good, with no further calls to the player (it is being released). */
    fun close() {
        closed = true
        job?.cancel()
        job = null
        _state.value = SleepTimerState.Off
    }

    /**
     * The deadline, not a running total, is the source of truth. `delay` guarantees only a minimum, so subtracting
     * each tick let every late wake-up shorten the sleep by that much. Here the pause is one deadline and the fade
     * paces itself against the same deadline; nothing ticks for the UI, which derives its countdown from
     * [SleepTimerState.Counting.endsAtElapsedRealtime] while it is on screen.
     */
    private fun count(endsAt: Long) {
        replaceRunningTimer()
        _state.value = SleepTimerState.Counting(endsAt)
        job = scope.launch {
            try {
                val fadeStart = endsAt - FADE_MS
                val untilFade = fadeStart - clock()
                if (untilFade > 0L) delay(untilFade)
                for (step in 1..FADE_STEPS) {
                    // Re-anchor every step to the deadline so a late wake-up is caught up, not added on.
                    val wait = fadeStart + step * FADE_STEP_MS - clock()
                    if (wait > 0L) delay(wait)
                    setVolume(1f - step / FADE_STEPS.toFloat())
                }
                pause()
                // Offer a way out instead of a dead end: the HUD and the sheet both point at "+5 min".
                _state.value = SleepTimerState.Expired
            } finally {
                // Paused, cancelled or extended: the next play starts at full volume.
                if (!closed) withContext(NonCancellable) { setVolume(1f) }
            }
        }
    }

    private fun replaceRunningTimer() {
        val wasArmedAtEndOfItem = _state.value == SleepTimerState.AtEndOfItem
        job?.cancel()
        job = null
        if (wasArmedAtEndOfItem) scope.launch { setStopAfterCurrentItem(false) }
    }

    companion object {
        const val FADE_MS = 10_000L
        const val FADE_STEPS = 10
        const val FADE_STEP_MS = FADE_MS / FADE_STEPS
        private const val MINUTE_MS = 60_000L
    }
}
