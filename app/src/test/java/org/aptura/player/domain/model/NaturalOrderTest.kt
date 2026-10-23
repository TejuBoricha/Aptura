package org.aptura.player.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plain lower-casing sorted `Show.S01E10` before `Show.S01E2`, so the "next episode" was often the wrong file. */
class NaturalOrderTest {

    private fun sorted(vararg titles: String) = titles.toList().sortedWith(NaturalOrder)

    @Test
    fun numbersCompareByValue_notDigitByDigit() {
        assertEquals(
            listOf("Show S01E1", "Show S01E2", "Show S01E10", "Show S01E11"),
            sorted("Show S01E10", "Show S01E2", "Show S01E11", "Show S01E1")
        )
    }

    @Test
    fun seasonsAndEpisodesOrderTogether() {
        assertEquals(
            listOf("S1E9", "S1E10", "S2E1", "S10E1"),
            sorted("S10E1", "S2E1", "S1E10", "S1E9")
        )
    }

    @Test
    fun caseAndAccentsDoNotSplitTheList() {
        assertEquals(
            listOf("apple 2", "Banana 1", "cherry 3"),
            sorted("cherry 3", "Banana 1", "apple 2")
        )
        assertEquals("accents sort with their base letter", listOf("Éclair", "Zebra"), sorted("Zebra", "Éclair"))
        assertTrue("still a total order, never \"equal\"", NaturalOrder.compare("Éclair", "eclair") != 0)
    }

    @Test
    fun numbersComeBeforeWords_likeAFileManager() {
        assertEquals(listOf("2 Fast", "Alpha"), sorted("Alpha", "2 Fast"))
    }

    @Test
    fun zeroPaddedAndPlainNumbersAreAdjacent_andOrderIsDeterministic() {
        val expected = listOf("Part 1", "Part 02", "Part 2", "Part 003")

        assertEquals(expected, sorted("Part 02", "Part 2", "Part 1", "Part 003"))
        assertEquals("another input order gives the same result", expected, sorted("Part 003", "Part 1", "Part 2", "Part 02"))
    }

    @Test
    fun hugeNumbersDoNotOverflow() {
        assertEquals(
            listOf("Take 99999999999999999999", "Take 100000000000000000000"),
            sorted("Take 100000000000000000000", "Take 99999999999999999999")
        )
    }

    @Test
    fun aPrefixSortsBeforeItsExtension() {
        assertEquals(listOf("Movie", "Movie 2", "Movie Extras"), sorted("Movie Extras", "Movie 2", "Movie"))
    }
}
