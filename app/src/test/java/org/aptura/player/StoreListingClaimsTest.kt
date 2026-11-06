package org.aptura.player

import org.aptura.player.core.playback.PlaybackSpeedOptions
import org.aptura.player.theme.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Audit SHIP-01: the store text said "zero permissions", speeds up to 3.0x in 0.05x steps, accent names the app does not
 * have, and "HDR10" and "TalkBack" support nobody had verified. Text written without checking the code is how an app
 * gets a misleading-metadata rejection and bad reviews. These tests check the numbers and names the listing states
 * against the code, and keep out the claims that are not verified.
 */
class StoreListingClaimsTest {

    private fun metadata(name: String): String {
        val file = listOf("distribution/play-store/metadata/$name", "../distribution/play-store/metadata/$name")
            .map(::File).first { it.exists() }
        return file.readText(Charsets.UTF_8)
    }

    private val full get() = metadata("full_description.txt")
    private val short get() = metadata("short_description.txt").trim()
    private val notes get() = metadata("release_notes_v1.0.0.txt")

    @Test
    fun playStoreLengthLimitsHold() {
        assertTrue("short description is ${short.length} characters, the limit is 80", short.length <= 80)
        assertTrue("full description is ${full.length} characters, the limit is 4000", full.length <= 4000)
        assertTrue("release notes are ${notes.length} characters, the limit is 500", notes.length <= 500)
    }

    @Test
    fun theSpeedRangeStatedIsTheOneInTheApp() {
        val min = PlaybackSpeedOptions.all.min()
        val max = PlaybackSpeedOptions.all.max()
        assertTrue("states ${min}x to ${max}x", full.contains("from ${min}x to ${max}x"))
        assertFalse(full.contains("3.0x"))
        assertFalse(full.contains("0.05x"))
    }

    @Test
    fun everyAccentColourNamedExistsAndTheCountMatches() {
        val labels = AccentColor.values().map { it.label }
        for (label in labels) assertTrue("the listing should name the accent '$label'", full.contains(label))
        assertTrue("the listing should say there are ${labels.size.toWords()} accents", full.lowercase().contains("${labels.size.toWords()} accent colours"))
        for (invented in listOf("Emerald", "Aptura Blue", "Champagne")) assertFalse("'$invented' is not an accent in the app", full.contains(invented))
    }

    @Test
    fun theSleepTimerRangeStatedIsTheOneInTheApp() {
        assertTrue(full.contains("from 1 to 720 minutes"))
        assertFalse(full.contains("5 to 720"))
    }

    @Test
    fun noPermissionIsClaimedAwayAndNoUnverifiedClaimIsMade() {
        val everything = (full + short + notes).lowercase()
        assertFalse("the app requests permissions (read video, foreground service, and more)", everything.contains("zero permissions"))
        assertFalse("not verified on a device: HDR10 playback", everything.contains("hdr10"))
        assertFalse("not verified with a screen reader on a device: TalkBack", everything.contains("talkback"))
        assertFalse("'zero network stack' is stronger than the manifest proves; the permission is what is guaranteed", everything.contains("zero network stack"))
        assertTrue(everything.contains("internet permission"))
    }

    private fun Int.toWords() = when (this) {
        4 -> "four"; 5 -> "five"; 6 -> "six"; 7 -> "seven"; 8 -> "eight"; else -> toString()
    }
}
