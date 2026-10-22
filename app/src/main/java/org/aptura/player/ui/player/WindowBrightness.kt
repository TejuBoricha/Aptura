package org.aptura.player.ui.player

import android.content.Context
import android.provider.Settings
import android.view.Window
import android.view.WindowManager

/**
 * Per-window brightness override used by the swipe-to-adjust gesture.
 *
 *  - When no override is active it starts from the device's real brightness instead of assuming 50 %,
 *    so the first swipe doesn't jump (audit P1-05).
 *  - [restore] puts the window back exactly as it was found, so leaving the player no longer leaves the
 *    library (and every other screen) at the dimmed/boosted level.
 */
class WindowBrightness(
    private val window: Window?,
    private val systemBrightness: () -> Float
) {
    private val original: Float =
        window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

    /** Current effective brightness in 0.01..1.0. */
    fun current(): Float {
        val override = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        return (if (override < 0f) systemBrightness() else override).coerceIn(MIN, 1f)
    }

    /** Applies [delta] (fraction of the full range) and returns the new brightness, or null without a window. */
    fun adjustBy(delta: Float): Float? {
        val target = window ?: return null
        val next = (current() + delta).coerceIn(MIN, 1f)
        val params = target.attributes
        params.screenBrightness = next
        target.attributes = params
        return next
    }

    fun restore() {
        val target = window ?: return
        val params = target.attributes
        params.screenBrightness = original
        target.attributes = params
    }

    companion object {
        const val MIN = 0.01f

        /** The user's system brightness setting as 0..1 (falls back to mid-range if unreadable). */
        fun systemBrightnessFraction(context: Context): Float {
            val raw = try {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            } catch (_: Settings.SettingNotFoundException) {
                128
            }
            return (raw / 255f).coerceIn(MIN, 1f)
        }
    }
}
