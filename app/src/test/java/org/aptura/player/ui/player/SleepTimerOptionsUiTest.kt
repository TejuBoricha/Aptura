package org.aptura.player.ui.player

import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The sleep timer's visible choices and what each state of the timer shows. The custom field's bounds and the dialog's
 * wiring are covered by `SleepTimerOptionsTest`; a Compose test that opens the dialog and types cannot settle under
 * Robolectric because the focused text field's cursor keeps animating, so that interaction is left to the device pass.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1700dp-xhdpi")
class SleepTimerOptionsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val minute = 60_000L

    private class Taps {
        val started = mutableListOf<Int>()
        val extended = mutableListOf<Int>()
        var cancelled = 0
        var atEnd = 0
    }

    private fun showTab(state: PlayerUiState = PlayerUiState(), taps: Taps = Taps()) {
        composeRule.setContent {
            ApturaTheme {
                PlaybackToolsTab(
                    uiState = state,
                    onAudioOnlyChanged = {},
                    onSpeedSelected = {},
                    onStartSleepTimer = { taps.started += it },
                    onCancelSleepTimer = { taps.cancelled++ },
                    onExtendSleepTimer = { taps.extended += it },
                    onSleepAtEndOfVideo = { taps.atEnd++ },
                    onSetAbPointA = {},
                    onSetAbPointB = {},
                    onClearAbRepeat = {},
                    onStepFrame = {},
                    onAddBookmark = {},
                    onSeekTo = {},
                    onDeleteBookmark = {},
                    onUpdateBookmarkNote = { _, _ -> },
                    onToggleStatsOverlay = {},
                    onResetVideoPreferences = {}
                )
            }
        }
    }

    private fun running(totalMinutes: Int, leftMinutes: Int) = PlayerUiState(
        sleepTimerEndsAtElapsedRealtime = SystemClock.elapsedRealtime() + leftMinutes * minute,
        sleepTimerTotalMs = totalMinutes * minute
    )

    @Test
    fun everyPresetCustomAndEndOfVideo_areOffered() {
        showTab()
        SLEEP_TIMER_PRESET_MINUTES.forEach { composeRule.onNodeWithText("$it min").assertIsDisplayed() }
        composeRule.onNodeWithText("Custom").assertIsDisplayed()
        composeRule.onNodeWithText("End of video").assertIsDisplayed()
    }

    @Test
    fun withNoTimer_itSaysOff_offersNoActions_andMarksNothing() {
        showTab()
        composeRule.onNodeWithText("Off").assertIsDisplayed()
        composeRule.onNodeWithText("+5 min").assertDoesNotExist()
        composeRule.onNodeWithText("Turn off").assertDoesNotExist()
        SLEEP_TIMER_PRESET_MINUTES.forEach { composeRule.onNodeWithText("$it min").assertIsNotSelected() }
    }

    @Test
    fun tappingATile_startsThatManyMinutes() {
        val taps = Taps()
        showTab(taps = taps)
        composeRule.onNodeWithText("45 min").performClick()
        assertEquals(listOf(45), taps.started)
    }

    @Test
    fun aRunningTimer_showsTheTimeLeft_theClockTimeItStops_andMarksTheTimeChosen() {
        showTab(running(totalMinutes = 30, leftMinutes = 20))
        composeRule.onNodeWithText("Stops in", substring = true).assertTextContains("Stops in 19:5", substring = true)
        composeRule.onNode(hasText("Pauses at", substring = true)).assertIsDisplayed()
        composeRule.onNodeWithText("30 min").assertIsSelected()
        composeRule.onNodeWithText("45 min").assertIsNotSelected()
    }

    @Test
    fun theChosenTime_isStillMarked_afterTheSheetIsOpenedAgain() {
        // Nothing is remembered by the sheet: it is built from the timer alone, as when it is reopened.
        showTab(running(totalMinutes = 15, leftMinutes = 9))
        composeRule.onNodeWithText("15 min").assertIsSelected()
    }

    @Test
    fun aTimerExtendedPastAPreset_marksCustom_withItsLength() {
        showTab(running(totalMinutes = 35, leftMinutes = 33))
        SLEEP_TIMER_PRESET_MINUTES.forEach { composeRule.onNodeWithText("$it min").assertIsNotSelected() }
        composeRule.onNode(hasText("Custom", substring = true)).assertIsSelected()
        composeRule.onNode(hasText("35 min", substring = true)).assertIsDisplayed()
    }

    @Test
    fun aRunningTimer_canBeExtendedByFive_andTurnedOff() {
        val taps = Taps()
        showTab(running(totalMinutes = 30, leftMinutes = 20), taps)
        composeRule.onNodeWithText("+5 min").performClick()
        composeRule.onNodeWithText("Turn off").performClick()
        assertEquals(listOf(5), taps.extended)
        assertEquals(1, taps.cancelled)
    }

    @Test
    fun endOfVideo_isMarked_andCanBeTurnedOff() {
        val taps = Taps()
        showTab(PlayerUiState(sleepAtEndOfVideo = true), taps)
        composeRule.onNodeWithText("Stops when this video ends").assertIsDisplayed()
        composeRule.onNodeWithText("End of video").assertIsSelected()
        composeRule.onNodeWithText("Turn off").performClick()
        assertEquals(1, taps.cancelled)
    }

    @Test
    fun afterItFires_itOffersFiveMinutes_orToDismiss() {
        val taps = Taps()
        showTab(PlayerUiState(sleepTimerExpired = true), taps)
        composeRule.onNodeWithText("Sleep timer ended").assertIsDisplayed()
        composeRule.onNodeWithText("+5 min").performClick()
        composeRule.onNodeWithText("Dismiss").performClick()
        assertEquals(listOf(5), taps.extended)
        assertEquals(1, taps.cancelled)
        assertTrue("nothing was started by accident", taps.started.isEmpty())
    }

    @Test
    fun onAPhone_theSixTimesAreTwoRowsOfThree() {
        showTab()
        val top = { text: String -> composeRule.onNodeWithText(text).getUnclippedBoundsInRoot().top }
        assertEquals("10, 15 and 20 share the first row", top("10 min"), top("20 min"))
        assertEquals(top("10 min"), top("15 min"))
        assertEquals("30, 45 and 60 share the second row", top("30 min"), top("60 min"))
        assertTrue("the second row is below the first", top("30 min") > top("10 min"))
    }

    @Config(qualifiers = "w700dp-h1700dp-xhdpi")
    @Test
    fun onAWideWindow_theSixTimesShareOneRow() {
        showTab()
        val top = { text: String -> composeRule.onNodeWithText(text).getUnclippedBoundsInRoot().top }
        SLEEP_TIMER_PRESET_MINUTES.drop(1).forEach { assertEquals("$it min is on the first row", top("10 min"), top("$it min")) }
    }

    @Test
    fun endOfVideoTile_startsTheEndOfVideoTimer() {
        val taps = Taps()
        showTab(taps = taps)
        composeRule.onNodeWithText("End of video").performClick()
        assertEquals(1, taps.atEnd)
        assertTrue("a time was started by accident", taps.started.isEmpty())
    }
}
