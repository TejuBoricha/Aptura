package org.aptura.player.core.platform

import android.content.Context
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Settings used to print a hand-typed "v1.0.0 (Build 63)" while the app's real versionCode was 1. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppVersionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun label_reportsWhatTheInstalledPackageSays() {
        val installed = PackageInfo().apply {
            packageName = context.packageName
            versionName = "2.5.1"
            longVersionCode = 77L
        }
        shadowOf(context.packageManager).installPackage(installed)

        val label = AppVersion.label(context)

        assertEquals("Aptura Video Player v2.5.1 (build 77)", label)
        assertFalse(label.contains("Build 63"))
    }
}
