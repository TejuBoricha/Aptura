package org.aptura.player.ui.library

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.aptura.player.core.media.DeleteOutcome
import org.aptura.player.core.media.MediaDeleter

/** Starts a real, user-consented deletion of a video file; [onDeleted] fires only once the file is gone. */
class MediaDeleteFlow internal constructor(private val start: (String) -> Unit) {
    fun delete(uri: String) = start(uri)
}

/**
 * Remembers the whole delete journey: direct delete when Android allows it, the system consent dialog
 * when it doesn't, and an honest message when neither is possible. Used by every delete entry point so the
 * app can never report a deletion that didn't happen (audit P0-06).
 */
@Composable
fun rememberMediaDeleteFlow(onDeleted: (uri: String) -> Unit): MediaDeleteFlow {
    val context = LocalContext.current
    val deleter = remember(context) { MediaDeleter(context) }
    val currentOnDeleted by rememberUpdatedState(onDeleted)
    var pendingUri by remember { mutableStateOf<String?>(null) }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val uri = pendingUri
        pendingUri = null
        if (uri == null) return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            Toast.makeText(context, "Deletion cancelled. The file was kept.", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Android 10: the consent only grants access; the app must repeat the delete itself.
            when (val retry = deleter.delete(uri)) {
                DeleteOutcome.Deleted -> currentOnDeleted(uri)
                is DeleteOutcome.Failed -> Toast.makeText(context, retry.message, Toast.LENGTH_LONG).show()
                is DeleteOutcome.NeedsConsent ->
                    Toast.makeText(context, "Android did not allow the deletion.", Toast.LENGTH_LONG).show()
            }
        } else {
            currentOnDeleted(uri)
        }
    }

    return remember(deleter, consentLauncher) {
        MediaDeleteFlow { uri ->
            when (val outcome = deleter.delete(uri)) {
                DeleteOutcome.Deleted -> currentOnDeleted(uri)
                is DeleteOutcome.NeedsConsent -> {
                    pendingUri = uri
                    consentLauncher.launch(IntentSenderRequest.Builder(outcome.intentSender).build())
                }
                is DeleteOutcome.Failed -> Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
