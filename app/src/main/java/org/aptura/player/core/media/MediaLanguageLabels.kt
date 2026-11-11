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

    private val iso6392ToIso6391: Map<String, String> by lazy {
        buildMap {
            for (iso2 in Locale.getISOLanguages()) {
                runCatching {
                    val iso3 = Locale.forLanguageTag(iso2).isO3Language
                    if (iso3.isNotEmpty()) {
                        put(iso3.lowercase(Locale.ROOT), iso2)
                    }
                }
            }
            // Bibliographic ISO 639-2/B aliases commonly emitted by Matroska/MP4 muxers
            put("fre", "fr")
            put("ger", "de")
            put("chi", "zh")
            put("dut", "nl")
            put("gre", "el")
            put("cze", "cs")
            put("rum", "ro")
            put("per", "fa")
            put("alb", "sq")
            put("arm", "hy")
            put("baq", "eu")
            put("bur", "my")
            put("geo", "ka")
            put("ice", "is")
            put("mac", "mk")
            put("mao", "mi")
            put("may", "ms")
            put("slo", "sk")
            put("tib", "bo")
            put("wel", "cy")
        }
    }

    /**
     * Returns a display name such as "English" (or localized equivalent in [displayLocale], audit I18N-03),
     * or null when the track has no meaningful language.
     */
    fun displayName(code: String?, displayLocale: Locale = Locale.getDefault()): String? {
        val normalized = code?.trim().orEmpty()
        if (normalized.isEmpty() || normalized.equals("und", ignoreCase = true)) return null

        val primarySubtag = normalized.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)
        val canonicalTag = iso6392ToIso6391[primarySubtag]?.let { iso2 ->
            val rest = normalized.substring(primarySubtag.length)
            iso2 + rest
        } ?: normalized

        val targetLocale = Locale.forLanguageTag(canonicalTag)
        val englishName = try {
            targetLocale.getDisplayLanguage(Locale.ENGLISH).trim()
        } catch (_: Throwable) {
            ""
        }
        val name = try {
            targetLocale.getDisplayLanguage(displayLocale).trim()
        } catch (_: Throwable) {
            ""
        }

        return when {
            name.isEmpty() || englishName.isEmpty() -> null
            englishName.equals("undetermined", ignoreCase = true) || name.equals("undetermined", ignoreCase = true) -> null
            // An unknown-but-well-formed code comes back unchanged; show it as a code rather than dropping it.
            englishName.equals(normalized, ignoreCase = true) ||
                englishName.equals(canonicalTag, ignoreCase = true) ||
                name.equals(normalized, ignoreCase = true) ||
                name.equals(canonicalTag, ignoreCase = true) -> normalized.uppercase(Locale.ROOT)
            else -> name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(displayLocale) else it.toString() }
        }
    }
}
