package org.aptura.player.core.diagnostics

import kotlinx.serialization.json.Json
import java.io.File

/**
 * On-device persistence for diagnostics (spec/14 §2). The in-memory ring buffer alone vanished with the process, so a
 * crash report said "No errors logged in recent sessions" about the very session that crashed.
 *
 * Layout, all inside one app-private directory:
 * - `events.jsonl` — one redacted [DiagnosticEvent] per line, appended as events happen.
 * - `crashes/crash-<epochMs>.txt` — one plain-text crash report per crash (class names and stack only, never messages).
 *
 * Bounds (spec/14 §2): the last [maxSessions] sessions, at most [maxBytes], nothing older than [retentionMs]; the last
 * [maxCrashes] crashes. Writes are append-only and tolerate being cut off mid-line: the reader skips what it cannot parse.
 *
 * Nothing here touches the network and nothing leaves the device unless the user exports it. Payloads are redacted
 * BEFORE they are written, so the file never holds a raw path, e-mail address or IP address.
 */
class DiagnosticStore(
    private val directory: File,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val maxSessions: Int = DEFAULT_MAX_SESSIONS,
    private val retentionMs: Long = DEFAULT_RETENTION_MS,
    private val maxCrashes: Int = DEFAULT_MAX_CRASHES
) {
    private val eventsFile = File(directory, EVENTS_FILE)
    private val crashDirectory = File(directory, CRASH_DIR)
    private val lock = Any()
    private val json = Json { ignoreUnknownKeys = true }

    /** Appends one event. Never throws: diagnostics must not be able to break the app they describe. */
    fun append(event: DiagnosticEvent): Boolean = synchronized(lock) {
        try {
            directory.mkdirs()
            eventsFile.appendText(encode(event) + "\n", Charsets.UTF_8)
            if (eventsFile.length() > maxBytes) compactLocked()
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** Everything still within the bounds, oldest first. Unreadable lines (a write cut off by a crash) are skipped. */
    fun loadEvents(): List<DiagnosticEvent> = synchronized(lock) { withinBounds(readAllLocked(), clock()) }

    /** Writes one crash report and drops all but the newest [maxCrashes]. @return the file name, or null if it failed. */
    fun writeCrash(timestampMs: Long, text: String): String? = synchronized(lock) {
        try {
            crashDirectory.mkdirs()
            val name = "crash-$timestampMs.txt"
            File(crashDirectory, name).writeText(text, Charsets.UTF_8)
            crashFilesLocked().drop(maxCrashes).forEach {
                it.delete()
                File(crashDirectory, it.name + ".ack").delete()
            }
            name
        } catch (_: Throwable) {
            null
        }
    }

    /** Crash reports within the retention window, newest first. */
    fun loadCrashes(): List<StoredCrash> = synchronized(lock) {
        val oldest = clock() - retentionMs
        crashFilesLocked().mapNotNull { file ->
            val timestamp = file.name.removePrefix("crash-").removeSuffix(".txt").toLongOrNull() ?: return@mapNotNull null
            if (timestamp < oldest) {
                null
            } else {
                runCatching { StoredCrash(file.name, timestamp, file.readText(Charsets.UTF_8), isAcknowledged(file)) }.getOrNull()
            }
        }
    }

    /** The user has seen (or dismissed) this crash; it stays in the log but stops prompting. */
    fun acknowledgeCrash(name: String): Boolean = synchronized(lock) {
        try {
            File(crashDirectory, "$name.ack").writeText("1")
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** "Delete diagnostic data": removes everything, immediately, with no hidden copies (spec/14 §2). */
    fun clear(): Boolean = synchronized(lock) {
        try {
            eventsFile.delete()
            crashDirectory.deleteRecursively()
            true
        } catch (_: Throwable) {
            false
        }
    }

    // ---- internals ----------------------------------------------------------------------------------------------

    private fun encode(event: DiagnosticEvent): String {
        val safe = event.copy(
            sessionId = event.sessionId.take(MAX_FIELD_CHARS),
            payload = RedactionEngine.redactPayload(event.payload, includeFileName = false)
                .entries.associate { (key, value) -> key.take(MAX_FIELD_CHARS) to value.take(MAX_VALUE_CHARS) }
        )
        // JSON puts the whole event on one line, so a payload value can never forge a second event.
        return json.encodeToString(DiagnosticEvent.serializer(), safe)
    }

    private fun readAllLocked(): List<DiagnosticEvent> {
        if (!eventsFile.exists()) return emptyList()
        return try {
            eventsFile.readLines(Charsets.UTF_8).mapNotNull { line ->
                if (line.isBlank()) null else runCatching { json.decodeFromString(DiagnosticEvent.serializer(), line) }.getOrNull()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /** Retention: nothing older than [retentionMs], and only the newest [maxSessions] sessions. */
    private fun withinBounds(events: List<DiagnosticEvent>, now: Long): List<DiagnosticEvent> {
        val recent = events.filter { it.timestampMs >= now - retentionMs }
        val sessions = recent.map { it.sessionId }.distinct() // order of first appearance = oldest first
        val keep = sessions.takeLast(maxSessions).toSet()
        return recent.filter { it.sessionId in keep }
    }

    /** Rewrites the file with the newest events that fit in 75 % of the size cap (so it does not compact on every append). */
    private fun compactLocked() {
        val kept = withinBounds(readAllLocked(), clock()).map { encode(it) + "\n" }
        var budget = (maxBytes * 3) / 4
        val newest = ArrayDeque<String>()
        for (line in kept.asReversed()) {
            val size = line.toByteArray(Charsets.UTF_8).size
            if (size > budget) break
            budget -= size
            newest.addFirst(line)
        }
        val temporary = File(directory, "$EVENTS_FILE.tmp")
        temporary.writeText(newest.joinToString(""), Charsets.UTF_8)
        if (!temporary.renameTo(eventsFile)) {
            // renameTo can fail across some file systems; fall back to copy semantics rather than losing the log.
            eventsFile.writeText(temporary.readText(Charsets.UTF_8), Charsets.UTF_8)
            temporary.delete()
        }
    }

    private fun crashFilesLocked(): List<File> =
        crashDirectory.listFiles { file -> file.name.startsWith("crash-") && file.name.endsWith(".txt") }
            .orEmpty()
            .sortedByDescending { it.name.removePrefix("crash-").removeSuffix(".txt").toLongOrNull() ?: 0L }

    private fun isAcknowledged(crash: File) = File(crashDirectory, crash.name + ".ack").exists()

    data class StoredCrash(val name: String, val timestampMs: Long, val text: String, val acknowledged: Boolean)

    companion object {
        const val EVENTS_FILE = "events.jsonl"
        const val CRASH_DIR = "crashes"
        const val DEFAULT_MAX_BYTES = 2L * 1024 * 1024
        const val DEFAULT_MAX_SESSIONS = 5
        const val DEFAULT_RETENTION_MS = 30L * 24 * 60 * 60 * 1000
        const val DEFAULT_MAX_CRASHES = 3
        private const val MAX_FIELD_CHARS = 64
        private const val MAX_VALUE_CHARS = 1024
    }
}
