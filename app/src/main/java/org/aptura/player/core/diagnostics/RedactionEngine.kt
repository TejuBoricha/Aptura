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

    /** `content://` and `file://` URIs, which can carry a whole path, encoded or not. Ends at whitespace. */
    private val URI_PATTERN = Pattern.compile("""(?:content|file)://\S+""", Pattern.CASE_INSENSITIVE)

    /**
     * A storage root and everything after it up to the end of the line. File names contain spaces and commas, so a
     * pattern that stops at the first one leaves the rest of the name in the report (audit PRIV-01: a path with spaces
     * came out as `[R] Holiday/secret clip.mp4`). Anything after the root on the same line is therefore treated as part of
     * the path; text that follows a path on that line is removed with it.
     */
    private val STORAGE_PATH_PATTERN = Pattern.compile(
        """(?:/storage/(?:emulated/\d+|[^/\s]+)|/sdcard|/mnt/sdcard|/mnt/media_rw/[^/\s]+|/data/user/\d+|/data/data|/data/media/\d+)""" +
            """(?![^/\s])(?:/[^\r\n]*)?""",
        Pattern.CASE_INSENSITIVE
    )

    private val EMAIL_PATTERN = Pattern.compile(
        """[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+"""
    )

    private val IP_V4_PATTERN = Pattern.compile(
        """\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b"""
    )

    private val EXTENSION_PATTERN = Pattern.compile("""[A-Za-z0-9]{2,5}""")

    /** Payload keys whose value is a name the person chose or a file has. Always removed. */
    private val NAME_KEYS = setOf("title", "oldtitle", "newtitle", "displayname", "name", "label", "foldername", "folder", "customtitle")

    /**
     * Redacts a raw text string, replacing filesystem paths and user identifiers
     * while preserving container extensions needed for failure diagnosis.
     */
    fun redactText(input: String, includeFileName: Boolean = false): String {
        if (input.isBlank()) return input

        // 1. Scrub URIs (before paths, so file:///storage/... is one match)
        var result = replaceAll(URI_PATTERN, input) { "[URI_REDACTED]" }

        // 2. Scrub storage paths
        result = replaceAll(STORAGE_PATH_PATTERN, result) { fullPath ->
            val ext = fullPath.substringAfterLast('.', "")
            if (includeFileName) {
                val fileName = fullPath.substringAfterLast('/')
                "[PATH_REDACTED]/$fileName"
            } else if (EXTENSION_PATTERN.matcher(ext).matches()) {
                "[FILE_REDACTED].$ext"
            } else {
                "[PATH_REDACTED]"
            }
        }

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

    /** A bare file name with no path: keep only a short extension, which is what helps diagnose a container problem. */
    private fun redactFileName(value: String): String {
        if (value.isBlank()) return value
        val redactedPaths = redactText(value, false)
        if (redactedPaths != value) return redactedPaths
        val ext = value.substringAfterLast('.', "")
        return if (EXTENSION_PATTERN.matcher(ext).matches()) "[FILE_REDACTED].$ext" else "[FILE_REDACTED]"
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
