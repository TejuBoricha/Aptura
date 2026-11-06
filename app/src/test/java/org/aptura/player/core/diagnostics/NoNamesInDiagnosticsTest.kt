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

    /**
     * Review finding: the first version only looked at Navigation.kt and only for `e.message`. Every diagnostic call in
     * the app is checked here, for every way an exception's text can end up in a payload.
     */
    @Test
    fun noDiagnosticCallRecordsExceptionTextAnywhereInTheApp() {
        val root = listOf("app/src/main/java", "../app/src/main/java").map(::File).first { it.exists() }
        val forbidden = listOf(
            Regex("""\.message\b"""), Regex("""localizedMessage"""), Regex("""\$\{?(?:e|t|error|throwable|ex)\}?""""),
            Regex("""\b(?:e|t|error|throwable|ex)\.toString\(\)""")
        )
        val offenders = mutableListOf<String>()
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            var from = 0
            while (true) {
                val at = text.indexOf("logEvent(", from)
                if (at < 0) break
                from = at + 1
                val call = text.substring(at, minOf(text.length, at + 700))
                // The call ends at the first blank line or the next function; 700 characters covers every payload here.
                val inCall = call.substringBefore("\n\n")
                if (forbidden.any { it.containsMatchIn(inCall) }) offenders += "${file.name}: ${inCall.lines().take(6).joinToString(" ").take(160)}"
            }
        }
        assertTrue("diagnostic calls that record exception text: $offenders", offenders.isEmpty())
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
