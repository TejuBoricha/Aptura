package org.aptura.player.ui.library

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import org.aptura.player.core.media.DeleteOutcome
import org.aptura.player.core.media.MediaDeleter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Starts user-consented deletion; [onDeleted] fires only after Android confirms the operation. */
data class DeleteBatchResult(
    val deletedUris: List<String>,
    val failedUris: List<String>
) {
    val deletedCount: Int get() = deletedUris.size
    val failedCount: Int get() = failedUris.size
}

class MediaDeleteFlow internal constructor(
    private val start: (List<String>, ((DeleteBatchResult) -> Unit)?) -> Unit
) {
    fun delete(uri: String) = start(listOf(uri), null)

    fun deleteAll(uris: List<String>, onFinished: ((DeleteBatchResult) -> Unit)? = null) = start(uris, onFinished)
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
    val coroutineScope = rememberCoroutineScope()
    val currentOnDeleted by rememberUpdatedState(onDeleted)
    var deleteQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var awaitingConsentUri by remember { mutableStateOf<String?>(null) }
    var awaitingBatchConsent by remember { mutableStateOf<List<String>>(emptyList()) }
    var completionCallback by remember { mutableStateOf<((DeleteBatchResult) -> Unit)?>(null) }
    var deletedUris by remember { mutableStateOf(emptyList<String>()) }
    var failedUris by remember { mutableStateOf(emptyList<String>()) }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val batch = awaitingBatchConsent
        if (batch.isNotEmpty()) {
            awaitingBatchConsent = emptyList()
            if (result.resultCode == Activity.RESULT_OK) {
                deletedUris = deletedUris + batch
                batch.forEach(currentOnDeleted)
            } else {
                failedUris = failedUris + batch
                Toast.makeText(context, "Deletion cancelled. The files were kept.", Toast.LENGTH_SHORT).show()
            }
            deleteQueue = deleteQueue.filterNot(batch.toSet()::contains)
            return@rememberLauncherForActivityResult
        }
        val uri = awaitingConsentUri
        if (uri == null) return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            failedUris = failedUris + uri
            Toast.makeText(context, "Deletion cancelled. The file was kept.", Toast.LENGTH_SHORT).show()
            awaitingConsentUri = null
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Android 10: the consent only grants access; the app must repeat the delete itself.
            coroutineScope.launch {
                when (val retry = withContext(Dispatchers.IO) { deleter.delete(uri) }) {
                    DeleteOutcome.Deleted -> {
                        deletedUris = deletedUris + uri
                        currentOnDeleted(uri)
                    }
                    is DeleteOutcome.Failed -> {
                        failedUris = failedUris + uri
                        Toast.makeText(context, retry.message, Toast.LENGTH_LONG).show()
                    }
                    is DeleteOutcome.NeedsConsent -> {
                        failedUris = failedUris + uri
                        Toast.makeText(context, "Android did not allow the deletion.", Toast.LENGTH_LONG).show()
                    }
                }
                awaitingConsentUri = null
                deleteQueue = deleteQueue.dropWhile { it != uri }.drop(1)
            }
            return@rememberLauncherForActivityResult
        } else {
            deletedUris = deletedUris + uri
            currentOnDeleted(uri)
            awaitingConsentUri = null
        }
        deleteQueue = deleteQueue.dropWhile { it != uri }.drop(1)
    }

    LaunchedEffect(deleteQueue, awaitingConsentUri, awaitingBatchConsent) {
        if (awaitingConsentUri != null || awaitingBatchConsent.isNotEmpty()) return@LaunchedEffect
        if (deleteQueue.isEmpty()) {
            completionCallback?.invoke(DeleteBatchResult(deletedUris, failedUris))
            completionCallback = null
            return@LaunchedEffect
        }
        val uri = deleteQueue.first()
        when (val outcome = withContext(Dispatchers.IO) { deleter.delete(uri) }) {
            DeleteOutcome.Deleted -> {
                deletedUris = deletedUris + uri
                currentOnDeleted(uri)
                deleteQueue = deleteQueue.drop(1)
            }
            is DeleteOutcome.NeedsConsent -> {
                awaitingConsentUri = uri
                consentLauncher.launch(IntentSenderRequest.Builder(outcome.intentSender).build())
            }
            is DeleteOutcome.Failed -> {
                failedUris = failedUris + uri
                Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                deleteQueue = deleteQueue.drop(1)
            }
        }
    }

    return remember(deleter, consentLauncher) {
        MediaDeleteFlow { uris, onFinished ->
            if (deleteQueue.isNotEmpty() || awaitingConsentUri != null || awaitingBatchConsent.isNotEmpty()) {
                Toast.makeText(context, "A deletion is already in progress.", Toast.LENGTH_SHORT).show()
                onFinished?.invoke(DeleteBatchResult(deletedUris = emptyList(), failedUris = uris.distinct()))
            } else {
                val uniqueUris = uris.distinct()
                if (uniqueUris.isEmpty()) {
                    onFinished?.invoke(DeleteBatchResult(emptyList(), emptyList()))
                    return@MediaDeleteFlow
                }
                deletedUris = emptyList()
                failedUris = emptyList()
                completionCallback = onFinished
                if (uniqueUris.size > 1 &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    uniqueUris.all { it.toUri().authority == MediaStore.AUTHORITY }
                ) {
                    try {
                        awaitingBatchConsent = uniqueUris
                        deleteQueue = uniqueUris
                        val sender = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            uniqueUris.map(Uri::parse)
                        ).intentSender
                        consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    } catch (_: Exception) {
                        awaitingBatchConsent = emptyList()
                        deleteQueue = emptyList()
                        failedUris = uniqueUris
                        completionCallback?.invoke(DeleteBatchResult(emptyList(), failedUris))
                        completionCallback = null
                        Toast.makeText(context, "Could not request Android's delete confirmation.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    deleteQueue = uniqueUris
                }
            }
        }
    }
}

/** Android's system trash request; only MediaStore items can use this action. */
class MediaTrashFlow internal constructor(private val start: (List<String>) -> Boolean) {
    fun trash(uris: List<String>): Boolean = start(uris)
}

internal fun canMoveToSystemTrash(uris: List<String>): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        uris.isNotEmpty() &&
        uris.all { it.toUri().authority == MediaStore.AUTHORITY }

@Composable
fun rememberMediaTrashFlow(onTrashed: (List<String>) -> Unit): MediaTrashFlow {
    val context = LocalContext.current
    val currentOnTrashed by rememberUpdatedState(onTrashed)
    var trashUris by remember { mutableStateOf<List<String>>(emptyList()) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(
                context,
                "Moved to Android trash. If restored, unhide it from Settings.",
                Toast.LENGTH_LONG
            ).show()
            currentOnTrashed(trashUris)
        } else {
            Toast.makeText(context, "Items were not moved to trash.", Toast.LENGTH_SHORT).show()
        }
        trashUris = emptyList()
    }

    return remember(launcher) {
        MediaTrashFlow { uris ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !canMoveToSystemTrash(uris)) {
                false
            } else {
                try {
                    val parsedUris = uris.map(String::toUri)
                    trashUris = uris.distinct()
                    val sender = MediaStore.createTrashRequest(context.contentResolver, parsedUris, true).intentSender
                    launcher.launch(IntentSenderRequest.Builder(sender).build())
                    true
                } catch (_: Exception) {
                    trashUris = emptyList()
                    Toast.makeText(context, "Could not request Android's trash confirmation.", Toast.LENGTH_LONG).show()
                    false
                }
            }
        }
    }
}
