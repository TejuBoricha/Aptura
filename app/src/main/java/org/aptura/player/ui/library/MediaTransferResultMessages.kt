package org.aptura.player.ui.library

import android.net.Uri
import androidx.core.net.toUri
import org.aptura.player.data.transfer.MediaTransferJob

internal data class TransferResultMessage(val title: String, val message: String)

/**
 * The move outcome must account for both halves of the operation. The copy phase can succeed while Android denies
 * deletion of the originals; saying only "Move not completed" hides the copies that now exist and invites the user to
 * retry, which would duplicate every file. This builder is pure so the wording is unit-tested without a composition.
 */
internal fun moveResultMessage(
    job: MediaTransferJob,
    deletedUris: List<String>,
    notDeletedUris: List<String>,
    titlesByUri: Map<String, String>
): TransferResultMessage {
    val total = job.totalFiles
    val copied = job.copiedUris.size
    val deleted = deletedUris.size
    val kept = notDeletedUris.size
    val notCopied = job.failedUris.size
    val title = when {
        deleted == total && notCopied == 0 -> "Move complete"
        deleted == 0 -> "Copied, originals kept"
        else -> "Move partially complete"
    }
    val message = buildString {
        if (title == "Move complete") {
            append("$deleted of $total ${videoWord(total)} moved to the selected folder. ")
            append("The originals were removed from the library.")
        } else {
            append("$copied of $total ${videoWord(total)} copied to the selected folder. ")
            if (deleted > 0) {
                append("$deleted ${originalWord(deleted)} deleted. ")
            }
            if (kept > 0) {
                append("Android did not delete $kept ${originalWord(kept)}: ")
                append("${describeVideoUris(notDeletedUris, titlesByUri)}. ")
                append(
                    if (kept == 1) {
                        "That video remains in your library and now exists in both places. "
                    } else {
                        "Those videos remain in your library and now exist in both places. "
                    }
                )
            }
            if (notCopied > 0) {
                append("Not copied (originals kept): ${describeVideoUris(job.failedUris, titlesByUri)}. ")
            }
            if (kept > 0) {
                append("Delete the originals from the library after checking the copies.")
            }
        }
    }.trim()
    return TransferResultMessage(title = title, message = message)
}

/** A compact, bounded list: the dialog must not grow with a batch of hundreds of files. */
internal fun describeVideoUris(uris: List<String>, titlesByUri: Map<String, String>): String {
    val names = uris.map { uri ->
        titlesByUri[uri] ?: uri.toUri().lastPathSegment ?: "Unknown video"
    }
    val displayed = names.take(3).joinToString()
    return if (names.size > 3) "$displayed, and ${names.size - 3} more" else displayed
}

private fun videoWord(count: Int) = if (count == 1) "video" else "videos"

private fun originalWord(count: Int) = if (count == 1) "original" else "originals"
