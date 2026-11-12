package org.aptura.player.ui.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerKeyHandlerTest {

    private var playPauseCalls = 0
    private var seekRelativeDelta: Long? = null
    private var seekToFractionValue: Float? = null
    private var seekToStartCalls = 0
    private var volumeStepDelta: Int? = null
    private var toggleMuteCalls = 0
    private var toggleResizeModeCalls = 0
    private var toggleSubtitlesCalls = 0
    private var skipNextCalls = 0
    private var skipPreviousCalls = 0
    private var dismissActiveSheetCalls = 0
    private var dismissActiveSheetResult = false
    private var backCalls = 0
    private var triggerLockHintCalls = 0

    private val actions = PlayerKeyActions(
        onPlayPause = { playPauseCalls++ },
        onSeekRelative = { seekRelativeDelta = it },
        onSeekToFraction = { seekToFractionValue = it },
        onSeekToStart = { seekToStartCalls++ },
        onStepVolume = { volumeStepDelta = it },
        onToggleMute = { toggleMuteCalls++ },
        onToggleResizeMode = { toggleResizeModeCalls++ },
        onToggleSubtitles = { toggleSubtitlesCalls++ },
        onSkipNext = { skipNextCalls++ },
        onSkipPrevious = { skipPreviousCalls++ },
        onDismissActiveSheet = {
            dismissActiveSheetCalls++
            dismissActiveSheetResult
        },
        onBack = { backCalls++ },
        onTriggerLockHint = { triggerLockHintCalls++ }
    )

    private fun downEvent(keyCode: Int): KeyEvent {
        return KeyEvent(AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, keyCode, 0))
    }

    private fun upEvent(keyCode: Int): KeyEvent {
        return KeyEvent(AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_UP, keyCode, 0))
    }

    @Test
    fun keyUpEvent_isNotConsumed() {
        val consumed = handlePlayerKeyEvent(
            event = upEvent(AndroidKeyEvent.KEYCODE_SPACE),
            isLocked = false,
            isAnySheetVisible = false,
            actions = actions
        )
        assertFalse(consumed)
        assertEquals(0, playPauseCalls)
    }

    @Test
    fun spacebarAndEnterAndDpadCenter_triggerPlayPause() {
        listOf(
            AndroidKeyEvent.KEYCODE_SPACE,
            AndroidKeyEvent.KEYCODE_ENTER,
            AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
            AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            AndroidKeyEvent.KEYCODE_K
        ).forEach { keyCode ->
            val prevCalls = playPauseCalls
            val consumed = handlePlayerKeyEvent(
                event = downEvent(keyCode),
                isLocked = false,
                isAnySheetVisible = false,
                actions = actions
            )
            assertTrue("Expected keyCode $keyCode to be consumed", consumed)
            assertEquals(prevCalls + 1, playPauseCalls)
        }
    }

    @Test
    fun leftArrowAndJ_seekBackward() {
        listOf(
            AndroidKeyEvent.KEYCODE_DPAD_LEFT,
            AndroidKeyEvent.KEYCODE_MEDIA_REWIND,
            AndroidKeyEvent.KEYCODE_J
        ).forEach { keyCode ->
            seekRelativeDelta = null
            val consumed = handlePlayerKeyEvent(
                event = downEvent(keyCode),
                isLocked = false,
                isAnySheetVisible = false,
                seekStepMs = 15_000L,
                actions = actions
            )
            assertTrue(consumed)
            assertEquals(-15_000L, seekRelativeDelta)
        }
    }

    @Test
    fun rightArrowAndL_seekForward() {
        listOf(
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
            AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            AndroidKeyEvent.KEYCODE_L
        ).forEach { keyCode ->
            seekRelativeDelta = null
            val consumed = handlePlayerKeyEvent(
                event = downEvent(keyCode),
                isLocked = false,
                isAnySheetVisible = false,
                seekStepMs = 12_000L,
                actions = actions
            )
            assertTrue(consumed)
            assertEquals(12_000L, seekRelativeDelta)
        }
    }

    @Test
    fun upAndDownArrows_stepVolume() {
        val upConsumed = handlePlayerKeyEvent(
            event = downEvent(AndroidKeyEvent.KEYCODE_DPAD_UP),
            isLocked = false,
            isAnySheetVisible = false,
            actions = actions
        )
        assertTrue(upConsumed)
        assertEquals(1, volumeStepDelta)

        val downConsumed = handlePlayerKeyEvent(
            event = downEvent(AndroidKeyEvent.KEYCODE_DPAD_DOWN),
            isLocked = false,
            isAnySheetVisible = false,
            actions = actions
        )
        assertTrue(downConsumed)
        assertEquals(-1, volumeStepDelta)
    }

    @Test
    fun mAndVolumeMute_toggleMute() {
        listOf(AndroidKeyEvent.KEYCODE_M, AndroidKeyEvent.KEYCODE_VOLUME_MUTE).forEach { keyCode ->
            val prevCalls = toggleMuteCalls
            val consumed = handlePlayerKeyEvent(
                event = downEvent(keyCode),
                isLocked = false,
                isAnySheetVisible = false,
                actions = actions
            )
            assertTrue(consumed)
            assertEquals(prevCalls + 1, toggleMuteCalls)
        }
    }

    @Test
    fun fKey_togglesResizeMode() {
        val consumed = handlePlayerKeyEvent(
            event = downEvent(AndroidKeyEvent.KEYCODE_F),
            isLocked = false,
            isAnySheetVisible = false,
            actions = actions
        )
        assertTrue(consumed)
        assertEquals(1, toggleResizeModeCalls)
    }

    @Test
    fun cKey_togglesSubtitles() {
        val consumed = handlePlayerKeyEvent(
            event = downEvent(AndroidKeyEvent.KEYCODE_C),
            isLocked = false,
            isAnySheetVisible = false,
            actions = actions
        )
        assertTrue(consumed)
        assertEquals(1, toggleSubtitlesCalls)
    }

    @Test
    fun nAndPKeys_skipNextAndPrevious() {
        val nextConsumed = handlePlayerKeyEvent(
            event = downEvent(AndroidKeyEvent.KEYCODE_N),
            isLocked = false,
            isAnySheetVisible = false,
            actions = actions
        )
        assertTrue(nextConsumed)
        assertEquals(1, skipNextCalls)

        val prevConsumed = handlePlayerKeyEvent(
            event = downEvent(AndroidKeyEvent.KEYCODE_P),
            isLocked = false,
            isAnySheetVisible = false,
            actions = actions
        )
        assertTrue(prevConsumed)
        assertEquals(1, skipPreviousCalls)
    }

    @Test
    fun digitKeys_seekToFraction() {
        val testPairs = listOf(
            AndroidKeyEvent.KEYCODE_0 to 0.0f,
            AndroidKeyEvent.KEYCODE_1 to 0.1f,
            AndroidKeyEvent.KEYCODE_5 to 0.5f,
            AndroidKeyEvent.KEYCODE_9 to 0.9f
        )
        testPairs.forEach { (keyCode, expectedFraction) ->
            seekToFractionValue = null
            val consumed = handlePlayerKeyEvent(
                event = downEvent(keyCode),
                isLocked = false,
                isAnySheetVisible = false,
                actions = actions
            )
            assertTrue(consumed)
            assertEquals(expectedFraction, seekToFractionValue ?: 0f, 0.001f)
        }
    }

    @Test
    fun homeKey_seeksToStart() {
        val consumed = handlePlayerKeyEvent(
            event = downEvent(AndroidKeyEvent.KEYCODE_MOVE_HOME),
            isLocked = false,
            isAnySheetVisible = false,
            actions = actions
        )
        assertTrue(consumed)
        assertEquals(1, seekToStartCalls)
    }

    @Test
    fun escape_whenSheetIsVisible_dismissesSheetAndDoesNotGoBack() {
        dismissActiveSheetResult = true
        val consumed = handlePlayerKeyEvent(
            event = downEvent(AndroidKeyEvent.KEYCODE_ESCAPE),
            isLocked = false,
            isAnySheetVisible = true,
            actions = actions
        )
        assertTrue(consumed)
        assertEquals(1, dismissActiveSheetCalls)
        assertEquals(0, backCalls)
    }

    @Test
    fun escape_whenNoSheetIsVisible_navigatesBack() {
        dismissActiveSheetResult = false
        val consumed = handlePlayerKeyEvent(
            event = downEvent(AndroidKeyEvent.KEYCODE_ESCAPE),
            isLocked = false,
            isAnySheetVisible = false,
            actions = actions
        )
        assertTrue(consumed)
        assertEquals(1, dismissActiveSheetCalls)
        assertEquals(1, backCalls)
    }

    @Test
    fun alphanumericKeys_whenSheetIsVisible_areIgnoredToPreventFightingTextFields() {
        listOf(
            AndroidKeyEvent.KEYCODE_SPACE,
            AndroidKeyEvent.KEYCODE_F,
            AndroidKeyEvent.KEYCODE_M,
            AndroidKeyEvent.KEYCODE_C,
            AndroidKeyEvent.KEYCODE_J,
            AndroidKeyEvent.KEYCODE_K,
            AndroidKeyEvent.KEYCODE_L,
            AndroidKeyEvent.KEYCODE_N,
            AndroidKeyEvent.KEYCODE_P,
            AndroidKeyEvent.KEYCODE_1
        ).forEach { keyCode ->
            val consumed = handlePlayerKeyEvent(
                event = downEvent(keyCode),
                isLocked = false,
                isAnySheetVisible = true,
                actions = actions
            )
            assertFalse("Expected keyCode $keyCode to NOT be consumed when sheet is visible", consumed)
        }
    }

    @Test
    fun lockedMode_triggersLockHint_andDoesNotExecuteActions() {
        val consumed = handlePlayerKeyEvent(
            event = downEvent(AndroidKeyEvent.KEYCODE_SPACE),
            isLocked = true,
            isAnySheetVisible = false,
            actions = actions
        )
        assertTrue(consumed)
        assertEquals(1, triggerLockHintCalls)
        assertEquals(0, playPauseCalls)
    }
}
