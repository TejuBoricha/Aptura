package org.aptura.player.ui.player

import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit A11Y-06: brightness could only be changed by swiping up and down on the left half of the picture. Someone who
 * cannot make that gesture, or who uses a screen reader or a switch, had no way to change it (WCAG 2.5.1 asks for a
 * single-tap alternative to any path gesture). There are now two: a Brightness slider in the tools sheet's Picture tab,
 * which a tap sets, and two actions on the video area for a screen reader or a switch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerBrightnessControlTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val brightnessShown = mutableListOf<Int>()

    private fun window() = composeRule.activity.window.attributes.screenBrightness

    private fun seedSystemBrightness(of255: Int) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, of255)
    }

    private var screenState by mutableStateOf(PlayerUiState())

    private fun render(uiState: PlayerUiState) {
        screenState = uiState
        composeRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = screenState,
                    player = null,
                    onBackClick = {},
                    onPlayPauseClick = {},
                    onScrubStart = {},
                    onScrubMove = {},
                    onScrubEnd = {},
                    onControlsVisibilityToggle = {},
                    onLockToggle = {},
                    onSpeedSelected = {},
                    onResizeModeToggle = {},
                    onSpeedSheetVisible = {},
                    onAudioSheetVisible = {},
                    onSubtitleSheetVisible = {},
                    onAudioTrackSelected = {},
                    onSubtitleTrackSelected = {},
                    onVolumePercentChanged = {},
                    onBrightnessPercentChanged = { brightnessShown += it },
                    onRetryPlayback = {}
                )
            }
        }
    }

    private fun playing() = PlayerUiState(videoTitle = "Clip", durationMs = 60_000L, isPlaying = true, isControlsVisible = false)

    private fun videoAction(label: String): () -> Boolean {
        val actions = composeRule.onNodeWithContentDescription("Video").fetchSemanticsNode().config
            .getOrNull(SemanticsActions.CustomActions).orEmpty()
        val action = actions.firstOrNull { it.label == label }
        assertNotNull("the video area should offer \"$label\"; it offers ${actions.map { it.label }}", action)
        return action!!.action
    }

    // ---- the helper ------------------------------------------------------------------------------------------------

    @Test
    fun setTo_setsTheWindowOverride_withinTheRange() {
        val brightness = WindowBrightness(composeRule.activity.window) { 0.4f }

        assertEquals(0.25f, brightness.setTo(0.25f)!!, 0.001f)
        assertEquals(0.25f, window(), 0.001f)
        assertEquals("above the range stops at full", 1f, brightness.setTo(2f)!!, 0.001f)
        assertEquals("the screen is never set fully dark", WindowBrightness.MIN, brightness.setTo(0f)!!, 0.0001f)
        assertEquals(WindowBrightness.MIN, window(), 0.0001f)

        brightness.restore()
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE, window(), 0f)
    }

    @Test
    fun setTo_withoutAWindow_doesNothing() {
        assertNull(WindowBrightness(null) { 0.4f }.setTo(0.5f))
    }

    // ---- actions for a screen reader or a switch ---------------------------------------------------------------------

    @Test
    fun theVideoArea_offersActionsThatRaiseAndLowerBrightness() {
        seedSystemBrightness(51) // 20 %
        render(playing())

        assertTrue(videoAction("Increase brightness")())
        composeRule.waitForIdle()
        assertEquals("one step up from 20 %", 0.30f, window(), 0.01f)
        assertEquals("the gauge shows the new level", listOf(30), brightnessShown)

        assertTrue(videoAction("Decrease brightness")())
        assertTrue(videoAction("Decrease brightness")())
        composeRule.waitForIdle()
        assertEquals(0.10f, window(), 0.01f)
    }

    @Test
    fun theBrightnessActions_stopAtTheEnds() {
        seedSystemBrightness(255)
        render(playing())

        assertTrue(videoAction("Increase brightness")())
        composeRule.waitForIdle()
        assertEquals(1f, window(), 0.001f)

        repeat(12) { videoAction("Decrease brightness")() }
        composeRule.waitForIdle()
        assertEquals("never fully dark", WindowBrightness.MIN, window(), 0.0001f)
    }

    // ---- the slider in the tools sheet -----------------------------------------------------------------------------

    private val brightnessSlider = hasContentDescription("Brightness") and SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)

    private fun openPictureTab() {
        composeRule.onNode(hasText("Picture") and androidx.compose.ui.test.isSelectable())
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
    }

    @Test
    fun theToolsSheet_hasABrightnessSlider_thatSetsTheWindow() {
        seedSystemBrightness(51) // 20 %
        render(playing().copy(activeSheet = PlayerSheet.Advanced))
        openPictureTab()

        val slider = composeRule.onNode(brightnessSlider)
        assertEquals("it starts at the level the screen is at", "20%", slider.fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription))

        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(0.6f) }
        composeRule.waitForIdle()

        assertEquals(0.6f, window(), 0.001f)
        assertEquals("60%", composeRule.onNode(brightnessSlider).fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription))
    }

    @Test
    fun theSlider_cannotTurnTheScreenFullyDark() {
        render(playing().copy(activeSheet = PlayerSheet.Advanced))
        openPictureTab()

        composeRule.onNode(brightnessSlider).performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        composeRule.waitForIdle()

        assertEquals(WindowBrightness.MIN, window(), 0.0001f)
    }

    @Test
    fun theSlider_followsAChangeMadeAnotherWay() {
        seedSystemBrightness(51) // 20 %
        render(playing().copy(activeSheet = PlayerSheet.Advanced))
        openPictureTab()

        assertTrue(videoAction("Increase brightness")())
        composeRule.waitForIdle()

        assertEquals("30%", composeRule.onNode(brightnessSlider).fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription))
    }

    @Test
    fun lockMode_offersNoBrightnessActions_asItTurnsTheSwipeOff() {
        render(playing().copy(isLocked = true))

        val actions = composeRule.onNodeWithContentDescription("Video").fetchSemanticsNode().config
            .getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertEquals(emptyList<String>(), actions.map { it.label })
    }

    @Test
    fun theSlider_showsTheLevelASwipeLeft() {
        seedSystemBrightness(51) // 20 %
        render(playing())

        // The swipe on the left half of the picture, as before.
        composeRule.onRoot().performTouchInput {
            swipe(start = Offset(width * 0.25f, height * 0.8f), end = Offset(width * 0.25f, height * 0.2f), durationMillis = 400)
        }
        composeRule.waitForIdle()
        val afterSwipe = window()
        assertTrue("the swipe should have raised brightness above 20 %, it is $afterSwipe", afterSwipe > 0.25f)

        screenState = screenState.copy(activeSheet = PlayerSheet.Advanced)
        composeRule.waitForIdle()
        openPictureTab()

        assertEquals(
            "${Math.round(afterSwipe * 100)}%",
            composeRule.onNode(brightnessSlider).fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        )
    }
}
