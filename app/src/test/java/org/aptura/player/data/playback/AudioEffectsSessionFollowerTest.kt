package org.aptura.player.data.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit FUNC-05: the sound effects (equalizer, bass, virtualizer, loudness) are bound to the player's audio session,
 * but the screen's view model owned them and released them when the screen went. Swipe the task away while a video
 * plays and the sound went on without them. These tests run the follower with no screen and no view model, from the
 * player's audio session alone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AudioEffectsSessionFollowerTest {

    @Test
    fun theEffectsAreAttached_toThePlayersSession_withNoScreenAtAll() = runTest(StandardTestDispatcher()) {
        val backend = FakeBackend()
        val sessions = MutableStateFlow(0)
        AudioEffectsSessionFollower(sessions, AudioEnhancementController(backend)).start(backgroundScope)
        runCurrent()
        assertTrue("no session yet, so nothing is attached", backend.equalizers.isEmpty())

        sessions.value = 7
        runCurrent()

        assertEquals(listOf(7), backend.sessions)
        assertFalse(backend.equalizers.single().released)
    }

    @Test
    fun whenThePlayerGetsANewSession_theEffectsMoveWithIt_andTheOldOnesAreReleased() = runTest(StandardTestDispatcher()) {
        val backend = FakeBackend()
        val sessions = MutableStateFlow(7)
        AudioEffectsSessionFollower(sessions, AudioEnhancementController(backend)).start(backgroundScope)
        runCurrent()

        sessions.value = 9 // the audio track was re-created while nobody is looking at the screen
        runCurrent()

        assertEquals(listOf(7, 9), backend.sessions)
        assertTrue("the old session's effects are released", backend.equalizers[0].released)
        assertFalse("the new session's effects stay on", backend.equalizers[1].released)
    }

    @Test
    fun theEffectsKeepTheListenersChoices_afterAMove() = runTest(StandardTestDispatcher()) {
        val backend = FakeBackend()
        val sessions = MutableStateFlow(7)
        val effects = AudioEnhancementController(backend)
        effects.setBassBoost(60)
        AudioEffectsSessionFollower(sessions, effects).start(backgroundScope)
        runCurrent()

        sessions.value = 9
        runCurrent()

        assertTrue("the bass boost the person chose is applied to the new session too", backend.bassBoosts[1].on)
    }

    @Test
    fun aSessionOfZeroOrLess_attachesNothing() = runTest(StandardTestDispatcher()) {
        val backend = FakeBackend()
        val sessions = MutableStateFlow(0)
        AudioEffectsSessionFollower(sessions, AudioEnhancementController(backend)).start(backgroundScope)

        sessions.value = -1
        runCurrent()

        assertTrue(backend.equalizers.isEmpty())
    }
}
