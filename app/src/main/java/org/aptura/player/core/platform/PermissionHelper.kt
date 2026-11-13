package org.aptura.player.core.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Granularity of library video access granted by Android (UX-01). */
enum class MediaAccessLevel {
    /** Full library access (`READ_MEDIA_VIDEO` on API 33+, or `READ_EXTERNAL_STORAGE` on API 26–32). */
    FULL,

    /** Android 14+ partial/selected video access (`READ_MEDIA_VISUAL_USER_SELECTED` only). */
    LIMITED,

    /** No MediaStore video access; only user-opened files or explicitly added SAF folders are readable. */
    NONE
}

/**
 * Storage and media access permission helper (DEC-011, FR-LIB-001..003, UX-01).
 * Adapts to scoped storage across Android API tiers without requesting MANAGE_EXTERNAL_STORAGE.
 */
object PermissionHelper {

    fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /** True only when full device-wide MediaStore video listing permission is granted. */
    fun hasFullStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getMediaAccessLevel(context: Context): MediaAccessLevel {
        if (hasFullStoragePermission(context)) {
            return MediaAccessLevel.FULL
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return MediaAccessLevel.LIMITED
        }
        return MediaAccessLevel.NONE
    }

    /** True when either full library access or Android 14+ selected-video access is active. */
    fun hasStoragePermission(context: Context): Boolean =
        getMediaAccessLevel(context) != MediaAccessLevel.NONE
}
