package org.aptura.player.core.diagnostics

import java.util.regex.Pattern

/**
 * Privacy-preserving redaction engine.
 * Source of truth: spec/14_ANALYTICS_AND_SUPPORT.md §1 & §4 (Principles D4 & D5)
 * Strips personal identifiers, user directories, file paths, and emails from diagnostic outputs.
 */
object RedactionEngine {

    private val STORAGE_PATH_PATTERN = Pattern.compile(
        """(?:/storage/emulated/\d+/|/sdcard/|/data/user/\d+/|/data/data/)[^\s,\]}"]+""",
        Pattern.CASE_INSENSITIVE
    )

    private val EMAIL_PATTERN = Pattern.compile(
        """[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+"""
    )

    private val IP_V4_PATTERN = Pattern.compile(
        """\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b"""
    )

    /**
     * Redacts a raw text string, replacing filesystem paths and user identifiers
     * while preserving container extensions needed for failure diagnosis.
     */
    fun redactText(input: String, includeFileName: Boolean = false): String {
        if (input.isBlank()) return input

        var result = input

        // 1. Scrub storage paths
        val matcher = STORAGE_PATH_PATTERN.matcher(result)
        val sb = StringBuffer()
        while (matcher.find()) {
            val fullPath = matcher.group()
            val ext = fullPath.substringAfterLast('.', "")
            val replacement = if (includeFileName) {
                val fileName = fullPath.substringAfterLast('/')
                "[PATH_REDACTED]/$fileName"
            } else if (ext.isNotEmpty() && ext.length in 2..5) {
                "[FILE_REDACTED].$ext"
            } else {
                "[PATH_REDACTED]"
            }
            matcher.appendReplacement(sb, MatcherQuote(replacement))
        }
        matcher.appendTail(sb)
        result = sb.toString()

        // 2. Scrub email addresses
        result = EMAIL_PATTERN.matcher(result).replaceAll("[EMAIL_REDACTED]")

        // 3. Scrub IP addresses
        result = IP_V4_PATTERN.matcher(result).replaceAll("[IP_REDACTED]")

        return result
    }

    /**
     * Redacts a map of diagnostic payload fields.
     */
    fun redactPayload(payload: Map<String, String>, includeFileName: Boolean = false): Map<String, String> {
        return payload.mapValues { (key, value) ->
            if (key.equals("path", ignoreCase = true) || key.equals("uri", ignoreCase = true) || key.equals("fileName", ignoreCase = true)) {
                redactText(value, includeFileName)
            } else {
                redactText(value, false)
            }
        }
    }

    private fun MatcherQuote(s: String): String {
        return s.replace("\\", "\\\\").replace("$", "\\$")
    }
}
