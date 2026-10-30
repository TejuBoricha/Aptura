package org.aptura.player.core.platform

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The first frame waits for the saved theme, so a person who chose Dark on a phone set to Light does not see a light
 * frame first, and it never waits for long: a slow settings file delays the first frame, it cannot block it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirstFrameGateTest {

    private var now = 1_000L
    private var clockReads = 0

    private fun gate(timeoutMs: Long = 700L) = FirstFrameGate(timeoutMs) {
        clockReads++
        now
    }

    @Test
    fun theFrameWaitsUntilTheSavedThemeIsApplied() {
        val gate = gate()
        assertFalse("nothing has been applied yet", gate.mayDraw())
        gate.themeApplied()
        assertTrue(gate.mayDraw())
    }

    @Test
    fun aSlowSettingsFileDelaysTheFrameButNeverBlocksIt() {
        val gate = gate(timeoutMs = 700L)
        now += 699L
        assertFalse("still inside the timeout", gate.mayDraw())
        now += 1L
        assertTrue("the timeout has passed and the theme never arrived", gate.mayDraw())
    }

    @Test
    fun theThemeArrivingAfterTheTimeoutChangesNothing() {
        val gate = gate(timeoutMs = 700L)
        now += 5_000L
        gate.themeApplied()
        assertTrue(gate.mayDraw())
    }

    private fun aView() = View(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun theFirstDrawIsCancelledWhileTheGateIsClosed() {
        val view = aView()
        val gate = gate()
        view.holdFirstFrameUntil(gate)
        // dispatchOnPreDraw returns true when a listener cancelled the draw (the system then asks again next frame).
        assertTrue("the draw should be cancelled", view.viewTreeObserver.dispatchOnPreDraw())
        assertTrue("and cancelled again on the next frame", view.viewTreeObserver.dispatchOnPreDraw())
    }

    @Test
    fun onceTheThemeIsApplied_theDrawGoesAhead() {
        val view = aView()
        val gate = gate()
        view.holdFirstFrameUntil(gate)
        assertTrue(view.viewTreeObserver.dispatchOnPreDraw())
        gate.themeApplied()
        assertFalse("the draw should go ahead", view.viewTreeObserver.dispatchOnPreDraw())
    }

    @Test
    fun aGateThatTimedOutLetsTheDrawGoAhead_andTheListenerIsGoneAfterwards() {
        val view = aView()
        val gate = gate(timeoutMs = 700L)
        view.holdFirstFrameUntil(gate)
        assertTrue(view.viewTreeObserver.dispatchOnPreDraw())
        now += 700L
        assertFalse(view.viewTreeObserver.dispatchOnPreDraw())

        // Later frames are not asked about at all: the listener removed itself.
        val readsAfterOpening = clockReads
        repeat(3) { assertFalse(view.viewTreeObserver.dispatchOnPreDraw()) }
        assertEquals(readsAfterOpening, clockReads)
    }
}
