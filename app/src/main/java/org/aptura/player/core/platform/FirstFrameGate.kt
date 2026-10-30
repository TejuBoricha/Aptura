package org.aptura.player.core.platform

import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver

/**
 * Decides when a window may draw its first frame. The app's theme (light, dark or follow the phone) is saved in a file
 * that takes a moment to read, so until it is read the first frame can only be drawn in the phone's own mode. Someone
 * who chose Dark on a phone set to Light saw a light frame, then a dark one, on every start.
 *
 * The window is held on its launch background (see `aptura_surface_base`) until [themeApplied] says the saved theme is
 * on screen, but never for longer than [timeoutMs], so a slow or failing read can delay the first frame and never
 * block it.
 */
internal class FirstFrameGate(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val uptimeMs: () -> Long = SystemClock::uptimeMillis
) {
    private val startedAt = uptimeMs()
    private var themeIsApplied = false

    /** The saved theme has been composed, so the next frame is the right one to show. */
    fun themeApplied() {
        themeIsApplied = true
    }

    fun mayDraw(): Boolean = themeIsApplied || uptimeMs() - startedAt >= timeoutMs

    companion object {
        /** Long enough for the settings file on a slow phone; short enough that a failure is not mistaken for a hang. */
        const val DEFAULT_TIMEOUT_MS = 700L
    }
}

/**
 * Holds this view's first draw until [gate] allows it. Returning false from a pre-draw listener cancels that frame and
 * the system asks again on the next one, the way the platform's own splash screen waits; once the gate has opened the
 * listener removes itself, so later frames are untouched.
 */
internal fun View.holdFirstFrameUntil(gate: FirstFrameGate) {
    viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (!gate.mayDraw()) return false
            // The observer this was added to can have been replaced when the view was attached; ask the live one.
            viewTreeObserver.removeOnPreDrawListener(this)
            return true
        }
    })
}
