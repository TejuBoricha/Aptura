package org.aptura.player.core.media

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import java.io.FileNotFoundException

/**
 * A debug-only [DocumentsProvider] used by the instrumented rename test. It models the one provider behavior the
 * unit-test fake could not: `renameDocument` allocates a **new document ID** and returns it, exactly as a real SAF
 * provider may do. That is why callers must persist the returned URI instead of the one they passed in.
 *
 * The provider is metadata-only; it never opens file bytes. It is declared in the debug manifest and is absent from
 * release APKs.
 */
class RenamingDocumentsProvider : DocumentsProvider() {

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor = cursor(
        projection,
        mapOf(
            Root.COLUMN_ROOT_ID to ROOT_ID,
            Root.COLUMN_DOCUMENT_ID to ROOT_DOCUMENT_ID,
            Root.COLUMN_TITLE to "Aptura Test Documents",
            Root.COLUMN_FLAGS to Root.FLAG_LOCAL_ONLY,
            Root.COLUMN_MIME_TYPES to "video/mp4",
            Root.COLUMN_AVAILABLE_BYTES to ONE_GIB
        )
    )

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor = when (documentId) {
        ROOT_DOCUMENT_ID -> cursor(
            projection,
            mapOf(
                Document.COLUMN_DOCUMENT_ID to ROOT_DOCUMENT_ID,
                Document.COLUMN_DISPLAY_NAME to "Aptura Test Documents",
                Document.COLUMN_MIME_TYPE to Document.MIME_TYPE_DIR,
                Document.COLUMN_FLAGS to 0
            )
        )
        currentDocumentId -> cursor(projection, documentValues())
        // The old ID stops resolving after a rename, like a provider that allocates a fresh ID.
        else -> emptyCursor(projection)
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor = if (parentDocumentId == ROOT_DOCUMENT_ID) {
        cursor(projection, documentValues())
    } else {
        emptyCursor(projection)
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor = throw FileNotFoundException("RenamingDocumentsProvider is metadata-only in tests")

    override fun renameDocument(documentId: String, displayName: String): String {
        check(documentId == currentDocumentId) { "unknown document $documentId" }
        currentDisplayName = displayName
        currentDocumentId = "renamed-${++revision}"
        return currentDocumentId
    }

    private fun documentValues(): Map<String, Any?> = mapOf(
        Document.COLUMN_DOCUMENT_ID to currentDocumentId,
        Document.COLUMN_DISPLAY_NAME to currentDisplayName,
        Document.COLUMN_MIME_TYPE to MIME_TYPE,
        Document.COLUMN_SIZE to FILE_SIZE,
        Document.COLUMN_LAST_MODIFIED to 1_700_000_000_000L,
        Document.COLUMN_FLAGS to 0
    )

    private fun cursor(projection: Array<out String>?, values: Map<String, Any?>): Cursor {
        val columns = projection?.takeIf { it.isNotEmpty() } ?: values.keys.toTypedArray()
        val cursor = MatrixCursor(columns)
        val row = cursor.newRow()
        columns.forEach { column -> row.add(values[column]) }
        return cursor
    }

    private fun emptyCursor(projection: Array<out String>?): Cursor =
        MatrixCursor(projection ?: arrayOf(Document.COLUMN_DOCUMENT_ID))

    companion object {
        const val AUTHORITY = "org.aptura.player.test.documents"

        private const val ROOT_ID = "root"
        private const val ROOT_DOCUMENT_ID = "root"
        private const val INITIAL_DOCUMENT_ID = "doc-1"
        private const val MIME_TYPE = "video/mp4"
        private const val FILE_SIZE = 1_048_576L
        private const val ONE_GIB = 1L shl 30

        @Volatile
        private var revision = 0

        @Volatile
        private var currentDocumentId = INITIAL_DOCUMENT_ID

        @Volatile
        private var currentDisplayName = "Holiday.mp4"

        fun reset() {
            revision = 0
            currentDocumentId = INITIAL_DOCUMENT_ID
            currentDisplayName = "Holiday.mp4"
        }

        fun documentUri(): Uri = DocumentsContract.buildDocumentUri(AUTHORITY, currentDocumentId)

        val documentId: String get() = currentDocumentId
        val displayName: String get() = currentDisplayName
    }
}
