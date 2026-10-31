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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aptura.player.core.media.MediaMover
import org.aptura.player.core.media.MoveOutcome

/** Starts user-consented in-place media moving; [onMoved] fires after the file is relocated. */
data class MoveBatchResult(
    val movedUris: List<String>,
    val failedUris: List<String>,
    val destinationFolder: String
) {
    val movedCount: Int get() = movedUris.size
    val failedCount: Int get() = failedUris.size
}

class MediaMoveFlow internal constructor(
    private val start: (List<String>, String, ((MoveBatchResult) -> Unit)?) -> Unit
) {
    fun moveAll(uris: List<String>, destinationFolder: String, onFinished: ((MoveBatchResult) -> Unit)? = null) =
        start(uris, destinationFolder, onFinished)
}

/**
 * Manages the in-place move flow for MediaStore items using Scoped Storage metadata updates.
 *
 * For single or batch items, this prompts Android's native write consent ("Allow Aptura to modify [N] video(s)?")
 * via [MediaStore.createWriteRequest] (API 30+) instead of the confusing and alarming "delete" prompt.
 */
@Composable
fun rememberMediaMoveFlow(onMoved: (uri: String) -> Unit): MediaMoveFlow {
    val context = LocalContext.current
    val mover = remember(context) { MediaMover(context) }
    val coroutineScope = rememberCoroutineScope()
    val currentOnMoved by rememberUpdatedState(onMoved)

    var moveQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentDestination by remember { mutableStateOf("") }
    var awaitingConsentUri by remember { mutableStateOf<String?>(null) }
    var awaitingBatchConsent by remember { mutableStateOf<List<String>>(emptyList()) }
    var completionCallback by remember { mutableStateOf<((MoveBatchResult) -> Unit)?>(null) }
    var movedUris by remember { mutableStateOf(emptyList<String>()) }
    var failedUris by remember { mutableStateOf(emptyList<String>()) }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val batch = awaitingBatchConsent
        val destination = currentDestination
        if (batch.isNotEmpty()) {
            awaitingBatchConsent = emptyList()
            if (result.resultCode == Activity.RESULT_OK) {
                coroutineScope.launch {
                    val newlyMoved = mutableListOf<String>()
                    val newlyFailed = mutableListOf<String>()
                    withContext(Dispatchers.IO) {
                        for (uri in batch) {
                            when (mover.moveInPlace(uri, destination)) {
                                MoveOutcome.Moved -> {
                                    newlyMoved += uri
                                    withContext(Dispatchers.Main) { currentOnMoved(uri) }
                                }
                                is MoveOutcome.Failed, is MoveOutcome.NeedsConsent -> {
                                    newlyFailed += uri
                                }
                            }
                        }
                    }
                    movedUris = movedUris + newlyMoved
                    failedUris = failedUris + newlyFailed
                    moveQueue = moveQueue.filterNot(batch.toSet()::contains)
                }
            } else {
                failedUris = failedUris + batch
                Toast.makeText(context, "Move cancelled. No files were modified.", Toast.LENGTH_SHORT).show()
                moveQueue = moveQueue.filterNot(batch.toSet()::contains)
            }
            return@rememberLauncherForActivityResult
        }

        val uri = awaitingConsentUri
        if (uri == null) return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            failedUris = failedUris + uri
            Toast.makeText(context, "Move cancelled. The file was not modified.", Toast.LENGTH_SHORT).show()
            awaitingConsentUri = null
            moveQueue = moveQueue.dropWhile { it != uri }.drop(1)
        } else {
            coroutineScope.launch {
                when (val retry = withContext(Dispatchers.IO) { mover.moveInPlace(uri, destination) }) {
                    MoveOutcome.Moved -> {
                        movedUris = movedUris + uri
                        currentOnMoved(uri)
                    }
                    is MoveOutcome.Failed -> {
                        failedUris = failedUris + uri
                        Toast.makeText(context, retry.message, Toast.LENGTH_LONG).show()
                    }
                    is MoveOutcome.NeedsConsent -> {
                        failedUris = failedUris + uri
                        Toast.makeText(context, "Android did not allow modifying this video.", Toast.LENGTH_LONG).show()
                    }
                }
                awaitingConsentUri = null
                moveQueue = moveQueue.dropWhile { it != uri }.drop(1)
            }
        }
    }

    LaunchedEffect(moveQueue, awaitingConsentUri, awaitingBatchConsent) {
        if (awaitingConsentUri != null || awaitingBatchConsent.isNotEmpty()) return@LaunchedEffect
        if (moveQueue.isEmpty()) {
            val callback = completionCallback
            if (callback != null) {
                completionCallback = null
                callback(MoveBatchResult(movedUris, failedUris, currentDestination))
            }
            return@LaunchedEffect
        }
        val uri = moveQueue.first()
        val destination = currentDestination
        when (val outcome = withContext(Dispatchers.IO) { mover.moveInPlace(uri, destination) }) {
            MoveOutcome.Moved -> {
                movedUris = movedUris + uri
                currentOnMoved(uri)
                moveQueue = moveQueue.drop(1)
            }
            is MoveOutcome.NeedsConsent -> {
                awaitingConsentUri = uri
                consentLauncher.launch(IntentSenderRequest.Builder(outcome.intentSender).build())
            }
            is MoveOutcome.Failed -> {
                failedUris = failedUris + uri
                Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                moveQueue = moveQueue.drop(1)
            }
        }
    }

    return remember(mover, consentLauncher) {
        MediaMoveFlow { uris, destinationFolder, onFinished ->
            if (moveQueue.isNotEmpty() || awaitingConsentUri != null || awaitingBatchConsent.isNotEmpty()) {
                Toast.makeText(context, "A move operation is already in progress.", Toast.LENGTH_SHORT).show()
                onFinished?.invoke(MoveBatchResult(emptyList(), uris.distinct(), destinationFolder))
            } else {
                val uniqueUris = uris.distinct()
                if (uniqueUris.isEmpty()) {
                    onFinished?.invoke(MoveBatchResult(emptyList(), emptyList(), destinationFolder))
                    return@MediaMoveFlow
                }
                movedUris = emptyList()
                failedUris = emptyList()
                currentDestination = destinationFolder
                completionCallback = onFinished

                if (uniqueUris.size > 1 &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    uniqueUris.all { it.toUri().authority == MediaStore.AUTHORITY }
                ) {
                    val sender = mover.requestBatchWriteConsent(uniqueUris)
                    if (sender != null) {
                        awaitingBatchConsent = uniqueUris
                        moveQueue = uniqueUris
                        consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
                        return@MediaMoveFlow
                    }
                }
                moveQueue = uniqueUris
            }
        }
    }
}
