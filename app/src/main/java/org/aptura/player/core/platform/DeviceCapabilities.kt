package org.aptura.player.core.platform

import android.content.pm.PackageManager
import android.os.Build

/**
 * Device features the UI adapts to instead of assuming. Kept in one place so a control and its menu cannot disagree:
 * the PiP action, the player menu and the manifest declaration all describe the same capability (F-07).
 */
object DeviceCapabilities {

    /**
     * PiP exists from API 26, but some devices (TVs, low-end tablets, managed profiles) never report the feature.
     * Offering the action there produces a button that can only fail, so the UI hides it.
     */
    fun supportsPictureInPicture(
        packageManager: PackageManager,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Boolean = sdkInt >= Build.VERSION_CODES.O &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}
