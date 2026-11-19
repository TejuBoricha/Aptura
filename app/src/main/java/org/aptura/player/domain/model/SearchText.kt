package org.aptura.player.domain.model

import java.text.Normalizer
import java.util.Locale

private val COMBINING_MARKS = Regex("""\p{Mn}+""")
private val SEPARATORS = Regex("""[._\-]+""")
private val SPACES = Regex("""\s+""")

private fun replaceSpecialLatinLetters(text: String): String {
    var result = text
    if (result.contains('ß')) result = result.replace("ß", "ss")
    if (result.contains('æ')) result = result.replace("æ", "ae")
    if (result.contains('œ')) result = result.replace("œ", "oe")
    if (result.contains('ø')) result = result.replace('ø', 'o')
    if (result.contains('ł')) result = result.replace('ł', 'l')
    if (result.contains('đ')) result = result.replace('đ', 'd')
    return result
}

/**
 * [text] folded for searching: lower case, accents removed, special latin letters mapped,
 * and the separators dot, underscore and hyphen read as spaces. "Show.S01E01", "Show S01E01" and "show_s01e01"
 * fold to the same key, as do "Café" and "cafe".
 */
internal fun searchKey(text: String): String {
    val folded = replaceSpecialLatinLetters(text.lowercase(Locale.ROOT))
    return Normalizer.normalize(folded, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .replace(SEPARATORS, " ")
        .replace(SPACES, " ")
        .trim()
}

/**
 * Whether [candidate] (a title or a folder name) matches the [query] the person typed (audit DATA-14).
 *
 * Supports optional precomputed [queryKey] and [queryTokens] to prevent O(N) re-normalization across large libraries.
 */
fun matchesSearch(
    candidate: String,
    query: String,
    queryKey: String = searchKey(query),
    queryTokens: List<String> = if (queryKey.isNotEmpty()) queryKey.split(' ').filter { it.isNotEmpty() } else emptyList()
): Boolean {
    if (candidate.contains(query, ignoreCase = true)) return true
    if (queryKey.isEmpty()) return false
    val candidateKey = searchKey(candidate)
    if (candidateKey.contains(queryKey)) return true
    return queryTokens.size > 1 && queryTokens.all { candidateKey.contains(it) }
}
