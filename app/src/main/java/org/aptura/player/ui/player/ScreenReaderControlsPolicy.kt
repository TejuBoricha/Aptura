package org.aptura.player.ui.player

import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Tells the player how the system's accessibility settings apply to its controls (audit A11Y-01).
 *
 * While touch exploration (TalkBack) is on the controls must not hide on their own, because a hidden control cannot be
 * focused or announced. The system's "time to take action" setting is passed on too, so someone who asked for more time
 * gets it. Both are read when the player opens and again whenever touch exploration is switched on or off.
 */
@Composable
internal fun ScreenReaderControlsPolicy(onPolicyChanged: (touchExplorationOn: Boolean, recommendedAutoHideMs: Long) -> Unit) {
    val context = LocalContext.current
    val latest = rememberUpdatedState(onPolicyChanged)
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        fun publish() {
            val touchExploration = manager?.isTouchExplorationEnabled == true
            val recommended = if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                manager.getRecommendedTimeoutMillis(DEFAULT_AUTO_HIDE_MS, AccessibilityManager.FLAG_CONTENT_CONTROLS).toLong()
            } else {
                DEFAULT_AUTO_HIDE_MS.toLong()
            }
            latest.value(touchExploration, recommended)
        }
        publish()
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { publish() }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
}

private const val DEFAULT_AUTO_HIDE_MS = 5_000
