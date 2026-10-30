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
 * Whether Aptura may list the videos on this device, kept true to what the system says *now*.
 *
 * The answer used to be read once when the app opened and then only from the reply to Aptura's own dialog. Access given
 * or taken away anywhere else (the system's app settings, a dialog whose reply never arrived) was not noticed until the
 * app was closed and opened again: the library stayed empty after access was granted, and asked for it again after it
 * had been. The value is read again every time the app comes back to the front.
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

/** What to do with the reply to the system's "allow access to your videos" dialog. */
enum class AccessReply {
    /** Access is allowed now. */
    GRANTED,

    /** Refused this time; the system will ask again if the person asks again. */
    DENIED,

    /** Refused for good: the system no longer shows the dialog, so the only way left is its settings page. */
    OPEN_SETTINGS
}

/** [canAskAgain] is `shouldShowRequestPermissionRationale` read after the reply: false once the system stops asking. */
fun replyToVideoAccessRequest(granted: Boolean, canAskAgain: Boolean): AccessReply = when {
    granted -> AccessReply.GRANTED
    canAskAgain -> AccessReply.DENIED
    else -> AccessReply.OPEN_SETTINGS
}

/** The system's page for this app, where its permissions can be changed. */
fun appDetailsSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
