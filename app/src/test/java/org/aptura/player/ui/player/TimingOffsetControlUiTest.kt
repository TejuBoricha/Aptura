package org.aptura.player.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimingOffsetControlUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var offset by mutableLongStateOf(0L)

    private fun setUp() {
        composeRule.setContent {
            ApturaTheme {
                TimingOffsetControl(
                    offsetMs = offset,
                    onOffsetChange = { offset = it },
                    rangeMs = 5_000L,
                    stepMs = 50L,
                    earlierLabel = "Text earlier",
                    laterLabel = "Text later",
                    label = "Subtitle timing",
                    describe = { "" }
                )
            }
        }
    }

    @Test
    fun tap_movesExactlyOneStep() {
        setUp()
        composeRule.onNodeWithContentDescription("+50 ms, Text later").performClick()
        composeRule.waitForIdle()
        assertEquals(50L, offset)
    }

    @Test
    fun hold_keepsStepping_andReleaseAddsNoExtraStep() {
        setUp()
        composeRule.mainClock.autoAdvance = false
        val plus = composeRule.onNodeWithContentDescription("+50 ms, Text later")
        plus.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_000L)
        plus.performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(100L)
        // 400 ms before repeating, then about every 90 ms: roughly seven steps in a one-second hold.
        assertTrue("held for a second, offset is $offset", offset in 250L..450L)
        val afterRelease = offset
        composeRule.mainClock.advanceTimeBy(500L)
        assertEquals("stops on release", afterRelease, offset)
    }
}
