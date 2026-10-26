package org.aptura.player.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Playback is a dark room regardless of the app's appearance choice (F-01, F-21). The rule used to live inline in
 * the navigation tree, where only the whole app could exercise it; it is now a pure function with its own contract.
 */
class ThemeRouteTest {

    @Test
    fun thePlayerRoute_isAlwaysDark_evenWhenTheAppIsPinnedToLight() {
        ThemeMode.entries.forEach { appMode ->
            assertEquals(
                "player must stay dark while the app is $appMode",
                ThemeMode.DARK,
                themeModeForRoute(isPlayerRouteActive = true, appThemeMode = appMode)
            )
        }
    }

    @Test
    fun everyOtherRoute_keepsTheModeTheUserChose() {
        assertEquals(ThemeMode.LIGHT, themeModeForRoute(false, ThemeMode.LIGHT))
        assertEquals(ThemeMode.DARK, themeModeForRoute(false, ThemeMode.DARK))
        assertEquals(ThemeMode.SYSTEM, themeModeForRoute(false, ThemeMode.SYSTEM))
    }
}
