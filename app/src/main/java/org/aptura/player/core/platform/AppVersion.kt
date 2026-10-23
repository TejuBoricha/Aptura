package org.aptura.player.core.platform

import android.content.Context
import android.os.Build

/** What the app says about its own version — read from the installed package, never typed in by hand. */
object AppVersion {
    fun label(context: Context): String {
        val info = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Throwable) {
            return "Aptura Video Player"
        }
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        return "Aptura Video Player v${info.versionName ?: "?"} (build $code)"
    }
}
