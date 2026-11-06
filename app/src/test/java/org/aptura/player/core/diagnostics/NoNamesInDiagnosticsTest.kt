package org.aptura.player.core.diagnostics

import org.aptura.player.data.database.MediaItemEntity
import org.aptura.player.data.scanner.FileReconciliationEngine
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Audit PRIV-01, the first line of defence: nothing in the app logs a title, a file name or an exception message into
 * the diagnostic report. Redaction is only the second line.
 */
class NoNamesInDiagnosticsTest {

    @Before
    fun clean() = ApturaDiagnostics.ringBuffer.clear()

    @After
    fun tidy() = ApturaDiagnostics.ringBuffer.clear()

    private fun entity(id: Long, uri: String, title: String) = MediaItemEntity(
        id = id, mediaStoreId = id, uri = uri, title = title, displayName = "$title.mp4",
        durationMs = 60_000L, sizeBytes = 5_000_000L, mimeType = "video/mp4"
    )

    @Test
    fun aDetectedMoveDoesNotLogEitherTitle() {
        val before = entity(1, "content://media/external/video/media/1", "Family Holiday 2025")
        val after = entity(2, "content://media/external/video/media/2", "Secret Project Renamed")

        FileReconciliationEngine.reconcile(scannedItems = listOf(after), existingItems = listOf(before))

        val logged = ApturaDiagnostics.ringBuffer.getAllEvents().joinToString("\n") { it.payload.toString() }
        assertTrue("the move should have been logged", logged.contains("file_moved_or_renamed"))
        assertFalse(logged.contains("Family"))
        assertFalse(logged.contains("Holiday"))
        assertFalse(logged.contains("Secret"))
        assertFalse(logged.contains("Renamed"))
    }

    @Test
    fun navigationLogsExceptionClassesNotMessages() {
        // The messages of SecurityException and IOException can embed the URI or path that failed. A source check is
        // enough here: every diagnostic payload in Navigation.kt must use the class name.
        val source = listOf("app/src/main/java/org/aptura/player/Navigation.kt", "../app/src/main/java/org/aptura/player/Navigation.kt")
            .map(::File).first { it.exists() }.readText()
        assertFalse(
            "a diagnostic payload in Navigation.kt records e.message",
            Regex("""logEvent\([^)]*e\.message""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(source)
        )
        assertFalse(source.contains("\"error\" to (e.message"))
    }
}
