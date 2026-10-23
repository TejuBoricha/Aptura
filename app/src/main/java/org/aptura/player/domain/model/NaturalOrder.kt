package org.aptura.player.domain.model

import java.text.Collator

/**
 * "Natural" text ordering: runs of digits compare by their number, everything else compares like a person would read
 * it (case and accents ignored). `Episode 2` comes before `Episode 10`, and `S01E09` before `S01E10` — plain
 * lower-casing put `E10` before `E2`.
 *
 * It is a total order: two titles that differ only by case, accents or zero-padding still sort deterministically,
 * so a list never reshuffles between two otherwise identical refreshes.
 */
object NaturalOrder : Comparator<String> {

    private val collator: Collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val aIsDigit = a[i].isAsciiDigit()
            val bIsDigit = b[j].isAsciiDigit()
            when {
                aIsDigit && bIsDigit -> {
                    val endA = runEnd(a, i, digits = true)
                    val endB = runEnd(b, j, digits = true)
                    val numberA = a.substring(i, endA).trimStart('0')
                    val numberB = b.substring(j, endB).trimStart('0')
                    val byMagnitude = numberA.length.compareTo(numberB.length)
                    if (byMagnitude != 0) return byMagnitude
                    val byDigits = numberA.compareTo(numberB)
                    if (byDigits != 0) return byDigits
                    i = endA
                    j = endB
                }
                // Numbers sort before words ("1 Show" < "Show"), like a file manager does.
                aIsDigit -> return -1
                bIsDigit -> return 1
                else -> {
                    val endA = runEnd(a, i, digits = false)
                    val endB = runEnd(b, j, digits = false)
                    val byText = synchronized(collator) { collator.compare(a.substring(i, endA), b.substring(j, endB)) }
                    if (byText != 0) return byText
                    i = endA
                    j = endB
                }
            }
        }
        val byRemaining = (a.length - i).compareTo(b.length - j)
        if (byRemaining != 0) return byRemaining
        return a.compareTo(b)
    }

    private fun Char.isAsciiDigit() = this in '0'..'9'

    private fun runEnd(text: String, start: Int, digits: Boolean): Int {
        var end = start
        while (end < text.length && text[end].isAsciiDigit() == digits) end++
        return end
    }
}
