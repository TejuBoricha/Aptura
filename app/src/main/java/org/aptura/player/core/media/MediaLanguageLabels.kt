package org.aptura.player.core.media

import java.util.Locale

/**
 * Turns the language codes stored in media metadata into names a person can read.
 *
 * Containers use a mixture of ISO 639-1 ("en"), ISO 639-2 ("eng") and BCP-47 ("pt-BR") codes, and they use
 * "und" when the language is unknown. The UI must never show raw codes: "English" is useful, "eng" is noise, and
 * "und" is worse than saying nothing.
 */
object MediaLanguageLabels {

    /** Returns a display name such as "English", or null when the track has no meaningful language. */
    fun displayName(code: String?): String? {
        val normalized = code?.trim().orEmpty()
        if (normalized.isEmpty() || normalized.equals("und", ignoreCase = true)) return null

        val name = try {
            Locale.forLanguageTag(normalized).getDisplayLanguage(Locale.ENGLISH).trim()
        } catch (_: Throwable) {
            ""
        }

        return when {
            name.isEmpty() -> null
            name.equals("undetermined", ignoreCase = true) -> null
            // An unknown-but-well-formed code comes back unchanged; show it as a code rather than dropping it.
            name.equals(normalized, ignoreCase = true) -> normalized.uppercase(Locale.ROOT)
            else -> name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
        }
    }
}
