package org.aptura.player.ui.player

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Encapsulates player keyboard and D-pad action callbacks (audit A11Y-02).
 */
data class PlayerKeyActions(
    val onPlayPause: () -> Unit,
    val onSeekRelative: (Long) -> Unit,
    val onSeekToFraction: (Float) -> Unit = {},
    val onSeekToStart: () -> Unit = {},
    val onStepVolume: (Int) -> Unit,
    val onToggleMute: () -> Unit,
    val onToggleResizeMode: () -> Unit,
    val onToggleSubtitles: () -> Unit = {},
    val onSkipNext: () -> Unit = {},
    val onSkipPrevious: () -> Unit = {},
    val onDismissActiveSheet: () -> Boolean,
    val onBack: () -> Unit,
    val onTriggerLockHint: () -> Unit = {}
)

/**
 * Handles hardware keyboard, D-pad, and Chromebook key events for the player (audit A11Y-02).
 *
 * Supported Mappings:
 * - Space, Enter, NumPadEnter, DpadCenter, K, MediaPlayPause: Play / Pause
 * - Left arrow, DpadLeft, MediaRewind, J: Seek backward by [seekStepMs]
 * - Right arrow, DpadRight, MediaFastForward, L: Seek forward by [seekStepMs]
 * - Up arrow, DpadUp: Volume up
 * - Down arrow, DpadDown: Volume down
 * - M, VolumeMute: Mute / unmute
 * - F: Toggle aspect ratio / resize mode
 * - C: Toggle subtitles on / off
 * - N, MediaNext: Skip next
 * - P, MediaPrevious: Skip previous
 * - 0..9, NumPad0..NumPad9: Seek to 0%..90% of duration
 * - Home: Seek to start (0 ms)
 * - Escape, Back: Dismiss active sheet or navigate back
 *
 * When [isLocked] is true, key events trigger the lock hint instead.
 * When an active sheet is showing ([isAnySheetVisible] is true), alphanumeric keys are ignored
 * to prevent fighting with text input fields (e.g. search, timing, bookmark notes).
 *
 * @return True if the key event was consumed, false otherwise.
 */
fun handlePlayerKeyEvent(
    event: KeyEvent,
    isLocked: Boolean,
    isAnySheetVisible: Boolean,
    seekStepMs: Long = 10_000L,
    actions: PlayerKeyActions
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    // Lock mode: show lock hint and swallow interactive media keys
    if (isLocked) {
        if (event.key != Key.Back && event.key != Key.Escape) {
            actions.onTriggerLockHint()
            return true
        }
        return false
    }

    // Escape / Back key: dismiss active sheet first, or navigate back
    if (event.key == Key.Escape || event.key == Key.Back) {
        if (actions.onDismissActiveSheet()) {
            return true
        }
        actions.onBack()
        return true
    }

    // When an active sheet is open, do not steal alphanumeric typing
    if (isAnySheetVisible) {
        return false
    }

    val stepMs = seekStepMs.coerceAtLeast(5000L)

    return when (event.key) {
        Key.Spacebar, Key.Enter, Key.NumPadEnter, Key.DirectionCenter, Key.MediaPlayPause, Key.K -> {
            actions.onPlayPause()
            true
        }
        Key.MediaPlay -> {
            actions.onPlayPause()
            true
        }
        Key.MediaPause, Key.MediaStop -> {
            actions.onPlayPause()
            true
        }
        Key.DirectionLeft, Key.MediaRewind, Key.J -> {
            actions.onSeekRelative(-stepMs)
            true
        }
        Key.DirectionRight, Key.MediaFastForward, Key.L -> {
            actions.onSeekRelative(stepMs)
            true
        }
        Key.DirectionUp -> {
            actions.onStepVolume(1)
            true
        }
        Key.DirectionDown -> {
            actions.onStepVolume(-1)
            true
        }
        Key.M, Key.VolumeMute -> {
            actions.onToggleMute()
            true
        }
        Key.F -> {
            actions.onToggleResizeMode()
            true
        }
        Key.C -> {
            actions.onToggleSubtitles()
            true
        }
        Key.N, Key.MediaNext -> {
            actions.onSkipNext()
            true
        }
        Key.P, Key.MediaPrevious -> {
            actions.onSkipPrevious()
            true
        }
        Key.MoveHome -> {
            actions.onSeekToStart()
            true
        }
        Key.Zero, Key.NumPad0 -> { actions.onSeekToFraction(0.0f); true }
        Key.One, Key.NumPad1 -> { actions.onSeekToFraction(0.1f); true }
        Key.Two, Key.NumPad2 -> { actions.onSeekToFraction(0.2f); true }
        Key.Three, Key.NumPad3 -> { actions.onSeekToFraction(0.3f); true }
        Key.Four, Key.NumPad4 -> { actions.onSeekToFraction(0.4f); true }
        Key.Five, Key.NumPad5 -> { actions.onSeekToFraction(0.5f); true }
        Key.Six, Key.NumPad6 -> { actions.onSeekToFraction(0.6f); true }
        Key.Seven, Key.NumPad7 -> { actions.onSeekToFraction(0.7f); true }
        Key.Eight, Key.NumPad8 -> { actions.onSeekToFraction(0.8f); true }
        Key.Nine, Key.NumPad9 -> { actions.onSeekToFraction(0.9f); true }
        else -> false
    }
}
