package org.aptura.player.core.media

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield

data class MediaTransferResult(
    val copiedUris: List<String>,
    val failedUris: List<String>
) {
    val copied: Int get() = copiedUris.size
}

/** Live figures for the transfer dialog. [fraction] prefers bytes, then falls back to file count. */
data class MediaTransferProgress(
    val completedFiles: Int,
    val totalFiles: Int,
    val copiedBytes: Long,
    val totalBytes: Long,
    val currentName: String
) {
    val fraction: Float
        get() = when {
            totalBytes > 0L -> (copiedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
            totalFiles > 0 -> (completedFiles.toFloat() / totalFiles).coerceIn(0f, 1f)
            else -> 0f
        }
}

/** What the destination says it can take. A null [availableBytes] means the provider did not report a quota. */
data class MediaTransferSpace(
    val requiredBytes: Long,
    val availableBytes: Long?
) {
    val hasEnoughSpace: Boolean get() = availableBytes == null || requiredBytes <= availableBytes
}

/**
 * Copies media into a folder explicitly chosen through Android's Storage Access Framework.
 * The destination provider owns naming and storage permission decisions.
 *
 * The copy is cooperative: it reports byte progress and checks for cancellation between chunks, and a cancelled
 * copy removes the partial destination instead of leaving a truncated video behind. The source is never touched.
 */
object MediaFileTransfer {

    private const val COPY_BUFFER_BYTES = 64 * 1024
    private const val PROGRESS_INTERVAL_MS = 200L

    suspend fun copyIntoTree(
        context: Context,
        sourceUris: List<String>,
        treeUri: Uri,
        skipCopied: Set<String> = emptySet(),
        onDocumentCreated: suspend (sourceUri: String, destinationUri: Uri) -> Unit = { _, _ -> },
        onSourceCopied: suspend (sourceUri: String, destinationUri: Uri) -> Unit = { _, _ -> },
        onProgress: suspend (MediaTransferProgress) -> Unit = {}
    ): MediaTransferResult {
        val resolver = context.contentResolver
        val destinationId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: IllegalArgumentException) {
            return MediaTransferResult(copiedUris = emptyList(), failedUris = sourceUris)
        }
        val destination = DocumentsContract.buildDocumentUriUsingTree(treeUri, destinationId)
        val sources = sourceUris.distinct()
        val sizes = sources.associateWith { sourceSize(resolver, it.toUri()) }
        val totalBytes = sizes.values.sum()
        // A resumed job already has these at the destination. They count as copied so the caller can report and
        // (for a move) delete exactly the files that now exist, but they are never written a second time.
        val copied = sources.filter { it in skipCopied }.toMutableList()
        val failed = mutableListOf<String>()
        var copiedBytes = copied.sumOf { sizes[it] ?: 0L }
        var completed = copied.size
        var lastReportAt = 0L

        suspend fun notify(block: suspend () -> Unit) {
            try {
                block()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // Persistence/progress reporting is best-effort; it must never turn a completed copy into a failure.
            }
        }

        suspend fun report(currentName: String, force: Boolean = false) {
            val now = SystemClock.elapsedRealtime()
            if (!force && now - lastReportAt < PROGRESS_INTERVAL_MS) return
            lastReportAt = now
            notify {
                onProgress(
                    MediaTransferProgress(
                        completedFiles = completed,
                        totalFiles = sources.size,
                        copiedBytes = copiedBytes,
                        totalBytes = totalBytes,
                        currentName = currentName
                    )
                )
            }
        }

        report(currentName = "", force = true)
        for (sourceString in sources.filterNot { it in skipCopied }) {
            coroutineContext.ensureActive()
            val source = sourceString.toUri()
            var created: Uri? = null
            try {
                val sourceDocumentId = try {
                    DocumentsContract.getDocumentId(source)
                } catch (_: IllegalArgumentException) {
                    null
                }
                if (sourceDocumentId != null &&
                    (sourceDocumentId == destinationId ||
                        sourceDocumentId.startsWith("$destinationId/") ||
                        sourceDocumentId.startsWith("$destinationId:"))
                ) {
                    throw IOException("Choose a different destination folder.")
                }
                val name = displayName(resolver, source)
                val mimeType = resolver.getType(source) ?: mimeTypeFromName(name)
                val createdUri = DocumentsContract.createDocument(resolver, destination, mimeType, name)
                    ?: throw IOException("The selected folder refused to create a file.")
                created = createdUri
                // Journal the destination before writing, so a process death can remove a truncated document.
                notify { onDocumentCreated(sourceString, createdUri) }
                val input = resolver.openInputStream(source)
                    ?: throw IOException("The source video could not be opened.")
                input.use { sourceStream ->
                    val output = resolver.openOutputStream(createdUri, "w")
                        ?: throw IOException("The selected folder could not be written.")
                    output.use { targetStream ->
                        val buffer = ByteArray(COPY_BUFFER_BYTES)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = sourceStream.read(buffer)
                            if (read < 0) break
                            targetStream.write(buffer, 0, read)
                            copiedBytes += read
                            report(name)
                            // Keeps cancellation responsive even when the whole copy runs on one dispatcher.
                            yield()
                        }
                    }
                }
                copied += sourceString
                completed++
                notify { onSourceCopied(sourceString, createdUri) }
                report(name, force = true)
            } catch (cancel: CancellationException) {
                created?.let { deleteQuietly(resolver, it) }
                throw cancel
            } catch (_: Exception) {
                created?.let { deleteQuietly(resolver, it) }
                failed += sourceString
                completed++
                report(source.lastPathSegment.orEmpty(), force = true)
            }
        }
        return MediaTransferResult(copied, failed)
    }

    /** Removes a document that a previous process created but did not finish writing. */
    fun deletePartial(context: Context, uri: Uri) {
        deleteQuietly(context.contentResolver, uri)
    }

    /** Sum of the readable source sizes and the destination's advertised free space, if it reports one. */
    fun measureSpace(context: Context, sourceUris: List<String>, treeUri: Uri): MediaTransferSpace {
        val resolver = context.contentResolver
        val required = sourceUris.distinct().sumOf { sourceSize(resolver, it.toUri()) }
        if (!DocumentsContract.isTreeUri(treeUri)) {
            return MediaTransferSpace(required, availableBytes = 0L)
        }
        return MediaTransferSpace(required, availableBytes(resolver, treeUri))
    }

    /**
     * Free space is a property of the provider's root, not of the selected document. Roots are matched to the
     * tree by document-id prefix ("primary:Movies" belongs to root "primary"). Providers that do not expose a
     * quota, or whose root cannot be matched, return null: the copy is attempted and a write failure is reported
     * honestly, rather than blocking a transfer on a guess.
     */
    private fun availableBytes(resolver: ContentResolver, treeUri: Uri): Long? {
        val authority = treeUri.authority ?: return null
        val treeDocumentId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val roots: List<Pair<String, Long>> = try {
            resolver.query(
                DocumentsContract.buildRootsUri(authority),
                arrayOf(
                    DocumentsContract.Root.COLUMN_ROOT_ID,
                    DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Root.COLUMN_ROOT_ID)
                val availableIndex =
                    cursor.getColumnIndex(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES)
                val result = mutableListOf<Pair<String, Long>>()
                if (idIndex >= 0 && availableIndex >= 0) {
                    while (cursor.moveToNext()) {
                        val rootId = cursor.getString(idIndex)
                        if (rootId != null && !cursor.isNull(availableIndex)) {
                            result += rootId to cursor.getLong(availableIndex)
                        }
                    }
                }
                result
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        if (roots.isEmpty()) return null

        val match = roots.firstOrNull { (rootId, _) -> treeDocumentId == rootId }
            ?: roots.firstOrNull { (rootId, _) ->
                treeDocumentId.startsWith("$rootId:") || treeDocumentId.startsWith("$rootId/")
            }
        return match?.second?.coerceAtLeast(0L)
    }

    private fun deleteQuietly(resolver: ContentResolver, uri: Uri) {
        try {
            DocumentsContract.deleteDocument(resolver, uri)
        } catch (_: Exception) {
            // The copy failed; the provider owns any partial document it refuses to remove.
        }
    }

    private fun sourceSize(resolver: ContentResolver, uri: Uri): Long {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it).length() }?.coerceAtLeast(0L) ?: 0L
        }
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                    cursor.getLong(index).coerceAtLeast(0L)
                } else {
                    0L
                }
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String {
        val cursor = try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        } catch (_: Exception) {
            null
        }
        cursor?.use {
            val column = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && it.moveToFirst()) {
                it.getString(column)?.takeIf(String::isNotBlank)?.let { name -> return name }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: throw IOException("The source video has no usable file name.")
    }

    private fun mimeTypeFromName(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "3gp" -> "video/3gpp"
            else -> "video/*"
        }
    }
}
