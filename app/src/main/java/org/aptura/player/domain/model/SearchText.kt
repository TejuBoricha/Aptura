package org.aptura.player.domain.model

import java.text.Normalizer
import java.util.Locale

private val COMBINING_MARKS = Regex("""\p{Mn}+""")
private val SEPARATORS = Regex("""[._\-]+""")
private val SPACES = Regex("""\s+""")

/**
 * [text] folded for searching: lower case, accents removed, and the separators dot, underscore and hyphen read as
 * spaces. "Show.S01E01", "Show S01E01" and "show_s01e01" fold to the same key, as do "Café" and "cafe".
 */
internal fun searchKey(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .lowercase(Locale.ROOT)
        .replace(COMBINING_MARKS, "")
        .replace(SEPARATORS, " ")
        .replace(SPACES, " ")
        .trim()

/**
 * Whether [candidate] (a title or a folder name) matches the [query] the person typed (audit DATA-14).
 *
 * Search used to compare the raw title, while the card shows the title with dots and underscores turned into spaces, so
 * "Show S01E01" missed `Show.S01E01` and "cafe" missed "Café". Both sides are now folded the same way. Whatever matched
 * before still matches, and a query that folds to nothing (only punctuation) matches only what contains it as typed.
 */
fun matchesSearch(candidate: String, query: String): Boolean {
    if (candidate.contains(query, ignoreCase = true)) return true
    val key = searchKey(query)
    return key.isNotEmpty() && searchKey(candidate).contains(key)
}
