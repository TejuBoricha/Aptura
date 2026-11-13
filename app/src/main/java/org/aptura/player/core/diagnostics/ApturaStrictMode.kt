package org.aptura.player.core.diagnostics

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.StrictMode

/**
 * Configures Android StrictMode in debuggable builds outside Robolectric JVM tests (QA-05).
 * Release builds (where [ApplicationInfo.FLAG_DEBUGGABLE] is clear) never install StrictMode policies.
 */
internal object ApturaStrictMode {

    fun isDebuggable(applicationFlags: Int): Boolean =
        (applicationFlags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun isRobolectricRuntime(fingerprint: String = Build.FINGERPRINT): Boolean =
        fingerprint.equals("robolectric", ignoreCase = true)

    fun installIfDebug(
        applicationFlags: Int,
        fingerprint: String = Build.FINGERPRINT,
        installer: () -> Unit = ::installDefaultPolicies
    ): Boolean {
        if (!isDebuggable(applicationFlags) || isRobolectricRuntime(fingerprint)) {
            return false
        }
        installer()
        return true
    }

    fun installDefaultPolicies() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .penaltyLog()
                .build()
        )
    }
}
