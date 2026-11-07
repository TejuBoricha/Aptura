package org.aptura.player.data.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Keeps the sound effects on the player's audio session for as long as the player lives, with or without a screen
 * (audit FUNC-05).
 *
 * The equalizer, bass boost, virtualizer and loudness effects are bound to the player's audio session, not to a
 * screen. They used to be owned by the screen's view model, which released them when the screen went, so swiping the
 * task away while a video played left the sound going without them. [PlayerManager] now owns the effects beside the
 * player, and this follower moves them to the new session whenever the player gets one (the audio track is re-created,
 * the decoder changes), which the view model used to do only while it was alive.
 *
 * [AudioEnhancementController.attachToSession] ignores a session it already holds and one that is not valid, and
 * releases the old session's effects when it moves, so following every value is safe.
 */
class AudioEffectsSessionFollower(
    private val sessionIds: StateFlow<Int>,
    private val effects: AudioEnhancementController
) {
    /** Starts following in [scope]. The job lives as long as the scope; cancel it when the player is released. */
    fun start(scope: CoroutineScope): Job = scope.launch {
        sessionIds.collect { sessionId -> effects.attachToSession(sessionId) }
    }
}
