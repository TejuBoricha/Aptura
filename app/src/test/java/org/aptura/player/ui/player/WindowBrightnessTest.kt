package org.aptura.player.ui.player

import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for audit P1-05: the brightness swipe set a window-level override that was never removed, so the
 * dimmed/boosted level leaked onto the library and every other screen, and the first swipe assumed 50 %.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WindowBrightnessTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun windowBrightness() = composeRule.activity.window.attributes.screenBrightness

    @Test
    fun firstAdjustment_startsFromTheRealSystemBrightness_notFiftyPercent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 51) // 20 %
        val brightness = WindowBrightness(composeRule.activity.window) {
            WindowBrightness.systemBrightnessFraction(context)
        }

        val next = brightness.adjustBy(0.10f)

        assertEquals(0.30f, next!!, 0.01f) // 20 % + 10 %, not 50 % + 10 %
    }

    @Test
    fun restore_putsTheWindowBackToNoOverride() {
        val brightness = WindowBrightness(composeRule.activity.window) { 0.4f }
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE, windowBrightness(), 0f)

        brightness.adjustBy(0.3f)
        assertNotEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE, windowBrightness())

        brightness.restore()
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE, windowBrightness(), 0f)
    }

    @Test
    fun swipingInRealPlayerScreen_overridesBrightness_andLeavingThePlayerRestoresIt() {
        var showPlayer by mutableStateOf(true)
        composeRule.setContent {
            ApturaTheme {
                if (showPlayer) {
                    PlayerScreen(
                        uiState = PlayerUiState(
                            videoTitle = "Clip",
                            durationMs = 60_000L,
                            isPlaying = true,
                            isControlsVisible = false // keep the HUD out of the gesture layer's way
                        ),
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
                        onBrightnessPercentChanged = {},
                        onRetryPlayback = {}
                    )
                }
            }
        }
        assertEquals(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE, windowBrightness(), 0f)

        // Vertical swipe on the LEFT half of the screen = brightness.
        composeRule.onRoot().performTouchInput {
            swipe(
                start = Offset(width * 0.25f, height * 0.8f),
                end = Offset(width * 0.25f, height * 0.2f),
                durationMillis = 400
            )
        }
        composeRule.waitForIdle()
        assertTrue(
            "brightness swipe should set a window override, was ${windowBrightness()}",
            windowBrightness() >= 0f
        )

        // Leave the player (as navigating back to the library does).
        showPlayer = false
        composeRule.waitForIdle()

        assertEquals(
            "leaving the player must remove the override",
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
            windowBrightness(),
            0f
        )
    }
}
