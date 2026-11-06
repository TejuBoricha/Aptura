package org.aptura.player.core.diagnostics

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Privacy-preserving redaction engine.
 * Source of truth: spec/14_ANALYTICS_AND_SUPPORT.md §1 & §4 (Principles D4 & D5)
 * Strips personal identifiers, user directories, file paths, and emails from diagnostic outputs.
 *
 * This is the second line of defence. The first is not to log names at all: diagnostic payloads carry codes, counts and
 * exception class names, never titles, file names or exception messages (audit PRIV-01). The rules below exist because
 * somebody will eventually log a path anyway, so they err on the side of removing too much.
 */
object RedactionEngine {

    /**
     * `content://` and `file://` URIs, to the end of the line. A URI that has not been percent-encoded can contain
     * spaces (`file:///storage/emulated/0/Movies/My Holiday/clip.mp4`), and a pattern that stopped at the first one left
     * the rest of the name behind (found in the independent review of the first version of this change).
     */
    private val URI_PATTERN = Pattern.compile("""(?:content|file)://[^\r\n]*""", Pattern.CASE_INSENSITIVE)

    /**
     * A storage root and everything after it up to the end of the line. File names contain spaces and commas, so a
     * pattern that stops at the first one leaves the rest of the name in the report. Anything after the root on the same
     * line is treated as part of the path; text that follows a path on that line is removed with it.
     *
     * The roots: `/storage/...` (every volume and `self/primary`), `/sdcard` and `/sdcard0`, and everything under `/mnt/`
     * (`/mnt/media_rw`, `/mnt/user/0/emulated/0`, `/mnt/runtime/default/...`, `/mnt/expand/<uuid>`) and `/data/`
     * (`/data/user`, `/data/media`, `/data/local/tmp`). A root only counts when the character before it is not part of
     * a word, so `https://example.com/data/x` is not touched.
     */
    private val STORAGE_PATH_PATTERN = Pattern.compile(
        """(?<![A-Za-z0-9._-])(?:/storage(?:/(?:emulated/\d+|[^/\s]+))?|/sdcard\d*|/mnt|/data)(?![^/\s])(?:/[^\r\n]*)?""",
        Pattern.CASE_INSENSITIVE
    )

    /** A path that was percent-encoded before it was logged: `%2Fstorage%2Femulated%2F0%2FMovies%2F...`. */
    private val ENCODED_PATH_PATTERN = Pattern.compile("""(?:%2F)(?:storage|sdcard\d*|mnt|data)(?:%2F|\b)[^\s]*""", Pattern.CASE_INSENSITIVE)

    /** Storage Access Framework document ids: `primary:Movies/Private/a.mp4`, `1A2B-3C4D:Movies/a.mp4`, `raw:/storage/...`. */
    private val DOCUMENT_ID_PATTERN = Pattern.compile(
        """(?<![A-Za-z0-9])(?:primary|home|[0-9a-f]{4}-[0-9a-f]{4}):[^\s\r\n][^\r\n]*|(?<![A-Za-z0-9])raw:/[^\r\n]*""",
        Pattern.CASE_INSENSITIVE
    )

    private val EMAIL_PATTERN = Pattern.compile(
        """[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+"""
    )

    private val IP_V4_PATTERN = Pattern.compile(
        """\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b"""
    )

    /**
     * The extensions worth keeping, because they say what kind of file failed. Anything else after the last dot is more
     * likely part of a name (`Trip.Priya`, `a.mp4 owner priya.kumar`) than an extension, so it is removed with the rest.
     */
    private val KNOWN_EXTENSIONS = setOf(
        "mp4", "m4v", "mkv", "webm", "avi", "mov", "wmv", "flv", "mpg", "mpeg", "ts", "m2ts", "mts", "3gp", "ogv", "rm", "rmvb",
        "mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "wma", "ac3", "dts",
        "srt", "vtt", "ass", "ssa", "sub", "ttml", "dfxp", "zip"
    )

    /** Payload keys whose value is a name the person chose or a file has. Always removed. */
    private val NAME_KEYS = setOf("title", "oldtitle", "newtitle", "displayname", "name", "label", "foldername", "folder", "customtitle")

    /**
     * Diagnostic values are short. A very long one is cut before the patterns run: the e-mail pattern is quadratic on a long
     * run of letters, and nothing useful for diagnosis is in the tail of a 20 KB value.
     */
    private const val MAX_INPUT_CHARS = 4_096

    /**
     * Redacts a raw text string, replacing filesystem paths and user identifiers
     * while preserving container extensions needed for failure diagnosis.
     */
    fun redactText(input: String, includeFileName: Boolean = false): String {
        if (input.isBlank()) return input

        var result = if (input.length > MAX_INPUT_CHARS) input.substring(0, MAX_INPUT_CHARS) + "[TRUNCATED]" else input

        // 1. Scrub URIs (before paths, so file:///storage/... is one match)
        result = replaceAll(URI_PATTERN, result) { "[URI_REDACTED]" }

        // 2. Scrub storage paths, percent-encoded paths and document ids
        result = replaceAll(STORAGE_PATH_PATTERN, result) { fullPath ->
            val ext = fullPath.substringAfterLast('.', "").lowercase()
            if (includeFileName) {
                "[PATH_REDACTED]/${fullPath.substringAfterLast('/')}"
            } else if (ext in KNOWN_EXTENSIONS) {
                "[FILE_REDACTED].$ext"
            } else {
                "[PATH_REDACTED]"
            }
        }
        result = replaceAll(ENCODED_PATH_PATTERN, result) { "[PATH_REDACTED]" }
        result = replaceAll(DOCUMENT_ID_PATTERN, result) { "[DOC_ID_REDACTED]" }

        // 3. Scrub email addresses
        result = EMAIL_PATTERN.matcher(result).replaceAll("[EMAIL_REDACTED]")

        // 4. Scrub IP addresses
        result = IP_V4_PATTERN.matcher(result).replaceAll("[IP_REDACTED]")

        return result
    }

    /**
     * Redacts a map of diagnostic payload fields.
     */
    fun redactPayload(payload: Map<String, String>, includeFileName: Boolean = false): Map<String, String> {
        return payload.mapValues { (key, value) ->
            val k = key.lowercase()
            when {
                k in NAME_KEYS -> if (value.isBlank()) value else "[NAME_REDACTED]"
                k == "filename" -> if (includeFileName) redactText(value, true) else redactFileName(value)
                k == "path" || k == "uri" -> redactText(value, includeFileName)
                else -> redactText(value, false)
            }
        }
    }

    /** A bare file name with no path: keep only a known extension, which is what helps diagnose a container problem. */
    private fun redactFileName(value: String): String {
        if (value.isBlank()) return value
        val redactedPaths = redactText(value, false)
        if (redactedPaths != value) return redactedPaths
        val ext = value.substringAfterLast('.', "").lowercase()
        return if (ext in KNOWN_EXTENSIONS) "[FILE_REDACTED].$ext" else "[FILE_REDACTED]"
    }

    private fun replaceAll(pattern: Pattern, input: String, replacement: (String) -> String): String {
        val matcher = pattern.matcher(input)
        val sb = StringBuffer()
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement(matcher.group())))
        }
        matcher.appendTail(sb)
        return sb.toString()
    }
}
