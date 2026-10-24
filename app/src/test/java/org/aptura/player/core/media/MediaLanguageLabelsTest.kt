package org.aptura.player.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Language codes come straight from the container. The UI must show names, and must stay silent for "und"
 * (audit 2026-10-24: Video Details rendered "Audio Stream (und)").
 */
class MediaLanguageLabelsTest {

    @Test
    fun unknownOrMissingLanguage_hasNoLabel() {
        assertNull(MediaLanguageLabels.displayName(null))
        assertNull(MediaLanguageLabels.displayName(""))
        assertNull(MediaLanguageLabels.displayName("   "))
        assertNull(MediaLanguageLabels.displayName("und"))
        assertNull(MediaLanguageLabels.displayName("UND"))
    }

    @Test
    fun twoLetterCodes_becomeReadableNames() {
        assertEquals("English", MediaLanguageLabels.displayName("en"))
        assertEquals("Japanese", MediaLanguageLabels.displayName("ja"))
        assertEquals("French", MediaLanguageLabels.displayName("fr"))
    }

    @Test
    fun threeLetterIso639Codes_becomeReadableNames() {
        assertEquals("English", MediaLanguageLabels.displayName("eng"))
        assertEquals("Japanese", MediaLanguageLabels.displayName("jpn"))
    }

    @Test
    fun regionalTags_keepTheirLanguageName() {
        assertEquals("Portuguese", MediaLanguageLabels.displayName("pt-BR"))
        assertEquals("English", MediaLanguageLabels.displayName("en-GB"))
    }

    @Test
    fun codesAndCasing_areNormalized() {
        assertEquals("English", MediaLanguageLabels.displayName("EN"))
        assertEquals("English", MediaLanguageLabels.displayName(" eng "))
    }

    @Test
    fun unknownButWellFormedCode_isShownAsACode_notDropped() {
        assertEquals("QQQ", MediaLanguageLabels.displayName("qqq"))
    }
}
