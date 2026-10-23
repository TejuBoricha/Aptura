package org.aptura.player.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aptura.player.data.database.MediaItemEntity
import java.util.ArrayDeque
import kotlin.math.abs

/**
 * Storage Access Framework (SAF) Tree Scanner (FR-LIB-004, FR-LIB-005, RQ-05).
 * Efficiently traverses user-granted document directory trees (via ACTION_OPEN_DOCUMENT_TREE),
 * respects .nomedia directories, and extracts video metadata into MediaItemEntity instances.
 *
 * Invariant: Never requests MANAGE_EXTERNAL_STORAGE (DEC-011).
 */
class SafTreeScanner(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    companion object {
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "webm", "mov", "avi", "ts", "m2ts", "mts", "flv", "3gp", "3g2",
            "m4v", "wmv", "ogv", "mpg", "mpeg", "vob", "divx", "f4v", "asf", "m2v", "mp2v", "mpv", "rmvb"
        )

        private val DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
    }

    /**
     * Scans a persisted SAF directory tree recursively using a memory-safe BFS queue.
     * Skips any folder containing a `.nomedia` sentinel file, and skips [MediaMetadataRetriever]
     * probing for unchanged files already present in [existingByUri] (audit P2-23).
     */
    suspend fun scanTree(
        treeUri: Uri,
        existingByUri: Map<String, MediaItemEntity> = emptyMap()
    ): List<MediaItemEntity> = withContext(ioDispatcher) {
        val discoveredItems = mutableListOf<MediaItemEntity>()

        val rootDocId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: Throwable) {
            return@withContext emptyList()
        }

        val rootFolderName = resolveTreeDisplayName(treeUri, rootDocId)

        // Queue: Pair of (DocumentId, Relative Folder Path)
        val directoryQueue = ArrayDeque<Pair<String, String>>()
        directoryQueue.add(rootDocId to rootFolderName)

        while (directoryQueue.isNotEmpty()) {
            val (parentDocId, currentFolderPath) = directoryQueue.removeFirst()

            val childrenUri = try {
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            } catch (_: Throwable) {
                continue
            }

            // Phase 1: Query child documents in current folder
            val childEntries = mutableListOf<SafEntry>()
            var hasNoMedia = false

            try {
                contentResolver.query(childrenUri, DOCUMENT_PROJECTION, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val docId = if (idCol >= 0) cursor.getString(idCol) else continue
                        val displayName = if (nameCol >= 0 && !cursor.isNull(nameCol)) cursor.getString(nameCol) else ""
                        val mimeType = if (mimeCol >= 0 && !cursor.isNull(mimeCol)) cursor.getString(mimeCol) else ""
                        val size = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
                        val lastMod = if (modCol >= 0 && !cursor.isNull(modCol)) cursor.getLong(modCol) else 0L

                        if (displayName.equals(".nomedia", ignoreCase = true)) {
                            hasNoMedia = true
                            break
                        }

                        childEntries.add(SafEntry(docId, displayName, mimeType, size, lastMod))
                    }
                }
            } catch (_: Throwable) {
                continue
            }

            // FR-LIB-005: If .nomedia is present, ignore this entire folder and all subfolders
            if (hasNoMedia) {
                continue
            }

            // Phase 2: Process child documents
            for (entry in childEntries) {
                if (entry.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val subFolderPath = "$currentFolderPath/${entry.displayName}"
                    directoryQueue.add(entry.docId to subFolderPath)
                } else if (isVideoFile(entry.displayName, entry.mimeType)) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, entry.docId)
                    val docUriStr = docUri.toString()
                    val modifiedSec = entry.lastModifiedMs / 1000L
                    val existing = existingByUri[docUriStr]
                    val syntheticId = existing?.mediaStoreId ?: computeSyntheticId(docUriStr)

                    var durationMs = existing?.durationMs ?: 0L
                    var width = existing?.width ?: 0
                    var height = existing?.height ?: 0
                    val isUnchanged = existing != null &&
                        existing.sizeBytes == entry.sizeBytes &&
                        existing.dateModifiedSec == modifiedSec &&
                        existing.durationMs > 0L

                    // Extract duration & dimensions via MediaMetadataRetriever only when new or modified (audit P2-23)
                    if (!isUnchanged && entry.sizeBytes > 1024L) {
                        try {
                            contentResolver.openFileDescriptor(docUri, "r")?.use { pfd ->
                                val retriever = MediaMetadataRetriever()
                                try {
                                    retriever.setDataSource(pfd.fileDescriptor)
                                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                    durationMs = durStr?.toLongOrNull() ?: 0L
                                    val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                                    width = wStr?.toIntOrNull() ?: 0
                                    val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                                    height = hStr?.toIntOrNull() ?: 0
                                } finally {
                                    retriever.release()
                                }
                            }
                        } catch (_: Throwable) {
                            // Non-fatal metadata read error; item will still be indexed with size and name
                        }
                    }

                    val title = if (entry.displayName.contains('.')) {
                        entry.displayName.substringBeforeLast('.')
                    } else entry.displayName

                    discoveredItems.add(
                        MediaItemEntity(
                            mediaStoreId = syntheticId,
                            uri = docUriStr,
                            title = title.ifBlank { entry.displayName },
                            displayName = entry.displayName,
                            durationMs = durationMs,
                            sizeBytes = entry.sizeBytes,
                            mimeType = entry.mimeType.ifBlank { "video/*" },
                            width = width,
                            height = height,
                            frameRate = existing?.frameRate ?: 0f,
                            dateAddedSec = existing?.dateAddedSec ?: modifiedSec,
                            dateModifiedSec = modifiedSec,
                            folderPath = currentFolderPath,
                            playbackPositionMs = 0L,
                            isCompleted = false,
                            lastPlayedTimestamp = 0L,
                            isMissing = false
                        )
                    )
                }
            }
        }

        discoveredItems
    }

    private fun isVideoFile(displayName: String, mimeType: String): Boolean {
        if (mimeType.startsWith("video/")) return true
        val ext = displayName.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXTENSIONS
    }

    /**
     * Computes a deterministic 63-bit negative ID from SHA-256 for SAF documents so it never collides
     * with positive MediaStore IDs or across large SAF libraries (audit P2-23).
     */
    internal fun computeSyntheticId(uriString: String): Long {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(uriString.toByteArray(Charsets.UTF_8))
        var raw = 0L
        for (i in 0 until 8) {
            raw = (raw shl 8) or (digest[i].toLong() and 0xFFL)
        }
        val positive63 = (raw and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L)
        return -positive63
    }

    private fun resolveTreeDisplayName(treeUri: Uri, rootDocId: String): String {
        return try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            var name: String? = null
            contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                }
            }
            name?.takeIf { it.isNotBlank() } ?: rootDocId.substringAfterLast(':').substringAfterLast('/')
        } catch (_: Throwable) {
            rootDocId.substringAfterLast(':').substringAfterLast('/').ifBlank { "Custom Folder" }
        }
    }

    private data class SafEntry(
        val docId: String,
        val displayName: String,
        val mimeType: String,
        val sizeBytes: Long,
        val lastModifiedMs: Long
    )
}
