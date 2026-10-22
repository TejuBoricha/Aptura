package org.aptura.player

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for audit P0-02: system Back from the player used to pop the screen but leave the video
 * playing (only the top-bar arrow paused it). These tests drive the *real* back dispatcher through a real
 * NavDisplay using the same [handleBack] policy MainNavigation installs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationBackTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun systemBack_fromPlayer_dismissesPlayerExactlyOnce_andShowsLibrary() {
        var dismissed = 0
        composeRule.setContent {
            val backStack = rememberNavBackStack(MediaLibrary, PlayerKey("content://media/1"))
            NavDisplay(
                backStack = backStack,
                onBack = { handleBack(backStack) { dismissed++ } },
                entryProvider = entryProvider {
                    entry<MediaLibrary> { Text("LIBRARY_SCREEN") }
                    entry<PlayerKey> { Text("PLAYER_SCREEN") }
                }
            )
        }
        composeRule.onNodeWithText("PLAYER_SCREEN").assertIsDisplayed()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals("player must be paused/persisted on system Back", 1, dismissed)
        composeRule.onNodeWithText("LIBRARY_SCREEN").assertIsDisplayed()
    }

    @Test
    fun handleBack_fromNonPlayerScreen_doesNotTouchPlayback() {
        var dismissed = 0
        val stack = mutableListOf<NavKey>(MediaLibrary, Settings)
        handleBack(stack) { dismissed++ }
        assertEquals(0, dismissed)
        assertEquals(listOf<NavKey>(MediaLibrary), stack)
    }

    @Test
    fun handleBack_stackedPlayerEntries_dismissesPerPop() {
        var dismissed = 0
        val stack = mutableListOf<NavKey>(MediaLibrary, PlayerKey("a"), PlayerKey("a"))
        handleBack(stack) { dismissed++ }
        handleBack(stack) { dismissed++ }
        assertEquals(2, dismissed)
        assertTrue(stack.single() is MediaLibrary)
    }

    @Test
    fun handleBack_onEmptyStack_isSafe() {
        var dismissed = 0
        handleBack(mutableListOf<NavKey>()) { dismissed++ }
        assertEquals(0, dismissed)
    }
}
