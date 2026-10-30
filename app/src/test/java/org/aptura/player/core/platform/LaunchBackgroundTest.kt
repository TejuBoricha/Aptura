package org.aptura.player.core.platform

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import org.aptura.player.R
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaPalettes
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The colour behind the first frame (and under the system's launch screen) used to be the dark base colour whatever the
 * phone's mode, so a phone set to Light opened on a dark screen that then turned light. It follows the phone's mode now,
 * and these tests keep it the same colour as the app's own base surface in that mode.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LaunchBackgroundTest {

    private fun windowBackgroundColor(): Int {
        val themed = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_Aptura)
        val attrs = themed.theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        try {
            return (attrs.getDrawable(0) as ColorDrawable).color
        } finally {
            attrs.recycle()
        }
    }

    @Config(qualifiers = "notnight")
    @Test
    fun aPhoneSetToLight_opensOnTheLightBaseColour() {
        assertEquals(ApturaPalettes.light(AccentColor.CYAN).surfaceBase.toArgb(), windowBackgroundColor())
    }

    @Config(qualifiers = "night")
    @Test
    fun aPhoneSetToDark_opensOnTheDarkBaseColour() {
        assertEquals(ApturaPalettes.dark(AccentColor.CYAN).surfaceBase.toArgb(), windowBackgroundColor())
    }

    @Config(qualifiers = "night")
    @Test
    fun theTestReallyRunsInNightMode() {
        val mode = ApplicationProvider.getApplicationContext<Context>().resources.configuration.uiMode
        assertEquals(Configuration.UI_MODE_NIGHT_YES, mode and Configuration.UI_MODE_NIGHT_MASK)
    }
}
