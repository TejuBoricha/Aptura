package org.aptura.player.core.diagnostics

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApturaStrictModeTest {

    @Test
    fun installIfDebug_enablesPoliciesOnlyWhenDebuggableAndNotRobolectric() {
        var installedCount = 0
        val enabled = ApturaStrictMode.installIfDebug(
            applicationFlags = ApplicationInfo.FLAG_DEBUGGABLE,
            fingerprint = "google/husky/husky:15/AP3A.241005.015/userdebug",
            installer = { installedCount++ }
        )

        assertTrue(enabled)
        assertTrue(installedCount == 1)
    }

    @Test
    fun installIfDebug_doesNotEnableInReleaseBuild() {
        var installedCount = 0
        val enabled = ApturaStrictMode.installIfDebug(
            applicationFlags = 0,
            fingerprint = "google/husky/husky:15/AP3A.241005.015/user",
            installer = { installedCount++ }
        )

        assertFalse(enabled)
        assertTrue(installedCount == 0)
    }

    @Test
    fun installIfDebug_skipsWhenRunningUnderRobolectric() {
        var installedCount = 0
        val enabled = ApturaStrictMode.installIfDebug(
            applicationFlags = ApplicationInfo.FLAG_DEBUGGABLE,
            fingerprint = "robolectric",
            installer = { installedCount++ }
        )

        assertFalse(enabled)
        assertTrue(installedCount == 0)
    }
}
