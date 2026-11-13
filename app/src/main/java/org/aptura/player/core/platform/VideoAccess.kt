package org.aptura.player.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Current video-library access level, refreshed every time the screen returns to the front.
 * Re-reads both full (`READ_MEDIA_VIDEO`) and Android 14+ partial (`READ_MEDIA_VISUAL_USER_SELECTED`) grants.
 */
@Composable
fun rememberVideoAccessLevel(): MutableState<MediaAccessLevel> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val access = remember { mutableStateOf(PermissionHelper.getMediaAccessLevel(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                access.value = PermissionHelper.getMediaAccessLevel(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return access
}

/**
 * Whether Aptura has any MediaStore video access right now (full or Android 14+ selected videos), refreshed on resume.
 */
@Composable
fun rememberVideoAccess(): MutableState<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val access = remember { mutableStateOf(PermissionHelper.hasStoragePermission(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) access.value = PermissionHelper.hasStoragePermission(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return access
}

/** Intent opening Aptura's own page in Android's app settings, where permissions are managed. */
fun appDetailsSettingsIntent(context: Context): Intent = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", context.packageName, null)
)

/** What the permission flow should do right after Android answers a permission request. */
enum class AccessReply { GRANTED, DENIED, OPEN_SETTINGS }

/**
 * Decides the next step after Android's permission prompt returns.
 * On Android 14+, both full access and selected-video (`LIMITED`) grants yield [AccessReply.GRANTED].
 */
fun replyToVideoAccessRequest(granted: Boolean, canAskAgain: Boolean): AccessReply = when {
    granted -> AccessReply.GRANTED
    canAskAgain -> AccessReply.DENIED
    else -> AccessReply.OPEN_SETTINGS
}
