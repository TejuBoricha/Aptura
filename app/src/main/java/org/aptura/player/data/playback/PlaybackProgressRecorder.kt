package org.aptura.player.data.playback

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.aptura.player.domain.model.DomainPlaybackQueue
import org.aptura.player.domain.model.DomainPlaybackState
import kotlin.math.abs

/**
 * Saves how far the person got in each video for as long as the process is playing one, with or without a screen
 * (audit FUNC-05).
 *
 * Progress used to be saved only by the screen's view model. Swipe the task out of Recents while a video plays and the
 * foreground service keeps the sound going, but the view model is gone: nothing wrote the position again, a video
 * finished in the background was never marked complete, and a later process kill lost where the person was. This
 * recorder is owned by [PlayerManager] next to the player and reads only the player's own state and queue.
 *
 * It saves in the same places the view model does:
 *  - a checkpoint every [checkpointIntervalMs] of playback;
 *  - where playback stopped, when it pauses;
 *  - the video that was left, under its own name, when another one becomes current;
 *  - a finished video, marked complete, at its full length.
 *
 * The view model still saves too, for now. The two write the same values for the same video, so the overlap is
 * harmless, and it lets this recorder be trusted on a device before the view model's own writes are removed.
 *
 * Everything is handled by one collector, so the fields below need no locking even on a multi-threaded dispatcher.
 */
class PlaybackProgressRecorder(
    private val playbackState: StateFlow<DomainPlaybackState>,
    private val playbackQueue: StateFlow<DomainPlaybackQueue>,
    private val save: suspend (uri: String, positionMs: Long, isCompleted: Boolean) -> Unit,
    private val checkpointIntervalMs: Long = CHECKPOINT_INTERVAL_MS
) {
    private sealed interface Event {
        class Queue(val queue: DomainPlaybackQueue) : Event
        class State(val state: DomainPlaybackState) : Event
    }

    // The video being played, where it was last seen, and what has been written for it.
    private var currentUri: String? = null
    private var lastPositionMs = 0L
    private var lastSavedPositionMs = 0L
    private var wasPlaying = false
    private var currentFinished = false

    /** Starts recording in [scope]. The job lives as long as the scope; cancel it when the player is released. */
    fun start(scope: CoroutineScope): Job = scope.launch {
        merge(
            playbackQueue.map<DomainPlaybackQueue, Event> { Event.Queue(it) },
            playbackState.map<DomainPlaybackState, Event> { Event.State(it) }
        ).collect { event ->
            when (event) {
                is Event.Queue -> onQueue(event.queue)
                is Event.State -> onState(event.state)
            }
        }
    }

    private suspend fun onQueue(queue: DomainPlaybackQueue) {
        val uri = queue.currentItem?.uri
        if (uri == currentUri) return
        // Leaving a video: keep where it stopped, under its own name, before anything is attributed to the next one.
        // A video that ended was already saved as complete; an ordinary save must not undo that.
        val left = currentUri
        if (left != null && !currentFinished && lastPositionMs > 0L) {
            persist(left, lastPositionMs, isCompleted = false)
        }
        currentUri = uri
        lastPositionMs = 0L
        lastSavedPositionMs = 0L
        wasPlaying = false
        currentFinished = false
    }

    private suspend fun onState(state: DomainPlaybackState) {
        val uri = currentUri ?: return
        when (state) {
            is DomainPlaybackState.Ready -> {
                val position = state.positionMs
                lastPositionMs = position
                if (state.isPlaying) {
                    if (position > 0L && abs(position - lastSavedPositionMs) >= checkpointIntervalMs) {
                        persist(uri, position, isCompleted = false)
                        lastSavedPositionMs = position
                    }
                } else if (wasPlaying) {
                    // Paused by the notification, a headset, the sleep timer: nothing else will save this.
                    if (position > 0L) persist(uri, position, isCompleted = false)
                    lastSavedPositionMs = position
                }
                wasPlaying = state.isPlaying
            }
            is DomainPlaybackState.Ended -> {
                if (!currentFinished && state.durationMs > 0L) {
                    persist(uri, state.durationMs, isCompleted = true)
                    currentFinished = true
                    lastPositionMs = state.durationMs
                    lastSavedPositionMs = state.durationMs
                }
                wasPlaying = false
            }
            is DomainPlaybackState.Buffering -> {
                // A seek lands here first: where the person moved to is where they would resume.
                if (state.positionMs > 0L) lastPositionMs = state.positionMs
            }
            is DomainPlaybackState.Idle, is DomainPlaybackState.Error -> Unit
        }
    }

    /** A failed write (a busy or closed database) must not end the recording: the next checkpoint tries again. */
    private suspend fun persist(uri: String, positionMs: Long, isCompleted: Boolean) {
        try {
            save(uri, positionMs, isCompleted)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Progress is a convenience, not data worth stopping playback or the recorder for.
        }
    }

    companion object {
        const val CHECKPOINT_INTERVAL_MS = 5_000L
    }
}
