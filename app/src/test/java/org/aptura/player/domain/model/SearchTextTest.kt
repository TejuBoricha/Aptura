package org.aptura.player.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit DATA-14: search matched the raw title, not the one on the card. "Show S01E01" missed `Show.S01E01`, and "cafe"
 * missed "Café".
 */
class SearchTextTest {

    // --- The bugs ---

    @Test
    fun aQueryWithSpaces_findsATitleWithDotsOrUnderscores() {
        assertTrue(matchesSearch("Show.S01E01", "Show S01E01"))
        assertTrue(matchesSearch("Show_S01E01", "show s01e01"))
        assertTrue(matchesSearch("Mr.Robot.S02E03", "robot s02"))
    }

    @Test
    fun aQueryWithDots_findsATitleWithSpaces() {
        assertTrue(matchesSearch("Show S01E01", "Show.S01E01"))
    }

    @Test
    fun accentsDoNotGetInTheWay_inEitherDirection() {
        assertTrue(matchesSearch("Café Society", "cafe"))
        assertTrue(matchesSearch("Café Society", "CAFÉ"))
        assertTrue(matchesSearch("Cafe Society", "café"))
        assertTrue(matchesSearch("Amélie", "amelie"))
        assertTrue(matchesSearch("Pokémon: The Movie", "pokemon"))
    }

    @Test
    fun aHyphenAndASpaceAreTheSame() {
        assertTrue(matchesSearch("Spider-Man", "spider man"))
        assertTrue(matchesSearch("Spider Man", "spider-man"))
    }

    // --- What must keep working ---

    @Test
    fun whatMatchedBeforeStillMatches() {
        assertTrue(matchesSearch("Inception", "incep"))
        assertTrue(matchesSearch("Inception", "INCEPTION"))
        assertTrue(matchesSearch("Movies", "ovie"))
        assertTrue("a query that is only punctuation still matches a title that has it", matchesSearch("Vol.2", "."))
    }

    @Test
    fun theSpellingStillHasToMatch() {
        assertFalse(matchesSearch("Inception", "Avatar"))
        assertFalse(matchesSearch("Café Society", "cafes"))
        assertFalse(matchesSearch("Show.S01E01", "Show S02"))
    }

    @Test
    fun extraSpacesInTheQueryOrTitleDoNotMatter() {
        assertTrue(matchesSearch("Show  S01E01", "show s01e01"))
        assertTrue(matchesSearch("Show.S01E01", "  show   s01e01  "))
    }

    @Test
    fun aQueryThatFoldsToNothing_doesNotMatchEverything() {
        assertFalse(matchesSearch("Inception", "..."))
        assertFalse(matchesSearch("Inception", " _ "))
    }
}
