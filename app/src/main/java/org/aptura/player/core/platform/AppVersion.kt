package org.aptura.player.core.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build

/** What the app says about its own version — read from the installed package, never typed in by hand. */
object AppVersion {
    fun label(context: Context): String {
        val info = packageInfo(context) ?: return "Aptura Video Player"
        return "Aptura Video Player v${info.versionName ?: "?"} (build ${versionCodeOf(info)})"
    }

    /**
     * Monotonic build number, used to decide whether the welcome guide should be shown again after an update.
     * Returns 0 when the package cannot be read, which callers treat as "unknown" and never as an update.
     */
    fun versionCode(context: Context): Int {
        val info = packageInfo(context) ?: return 0
        return versionCodeOf(info)
    }

    private fun packageInfo(context: Context): PackageInfo? = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (_: Throwable) {
        null
    }

    @Suppress("DEPRECATION")
    private fun versionCodeOf(info: PackageInfo): Int {
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        return code.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }
}
