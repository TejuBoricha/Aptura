package org.aptura.player.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Audit DATA-10: renaming "Mr.Robot" on a `.mkv` produced a file named `Mr.Robot`. A dot followed by two to five letters
 * was taken for an extension, so the real one was never added and Android could no longer tell the file is a video.
 */
class ResolveFileNameTest {

    private fun resolve(requested: String, current: String? = "Holiday.mp4", mime: String? = "video/mp4") =
        ApturaMediaFileManager.resolveFileName(requested, current, mime)

    // --- The bug: a title with a dot in it ---

    @Test
    fun aTitleWithADotAndFiveLetters_isNotTakenForAnExtension() {
        assertEquals("Mr.Robot.mkv", resolve("Mr.Robot", current = "Mr Robot.mkv", mime = "video/x-matroska"))
    }

    @Test
    fun otherTitlesThatLookLikeAnExtension_keepTheRealOne() {
        assertEquals("Dr.Who.mp4", resolve("Dr.Who"))
        assertEquals("Episode.One.mp4", resolve("Episode.One"))
        assertEquals("St.Elmos.mp4", resolve("St.Elmos"))
        assertEquals("Vol.2B.mp4", resolve("Vol.2B"))
    }

    @Test
    fun aNameThatEndsInAnotherFileType_isNotKeptAsIfItWereVideo() {
        assertEquals("notes.txt.mp4", resolve("notes.txt"))
    }

    // --- What must keep working ---

    @Test
    fun aNameWithNoDot_getsTheRealExtension() {
        assertEquals("Summer.mp4", resolve("Summer"))
        assertEquals("Summer.mkv", resolve("Summer", current = "Old.mkv", mime = null))
    }

    @Test
    fun theFilesOwnExtension_isKept_whateverTheCase() {
        assertEquals("Summer.mp4", resolve("Summer.mp4"))
        assertEquals("Summer.MP4", resolve("Summer.MP4"))
    }

    @Test
    fun aDeliberateChoiceOfAnotherVideoExtension_isKept() {
        assertEquals("Summer.mkv", resolve("Summer.mkv"))
        assertEquals("Summer.avi", resolve("Summer.avi"))
    }

    @Test
    fun aYearOrAnEpisodeCodeInTheTitle_isNotAnExtension() {
        assertEquals("Summer.2024.mp4", resolve("Summer.2024"))
        assertEquals("Show.S01E01.mp4", resolve("Show.S01E01"))
    }

    @Test
    fun aTitleWithASpaceAfterTheDot_isNotAnExtension() {
        assertEquals("Dr. Strangelove.mp4", resolve("Dr. Strangelove"))
    }

    @Test
    fun whenTheCurrentNameHasNoExtension_theTypeOfTheFileIsUsed() {
        assertEquals("Summer.mkv", resolve("Summer", current = null, mime = "video/x-matroska"))
        assertEquals("Summer.webm", resolve("Summer", current = "clip", mime = "video/webm"))
    }

    @Test
    fun whenNothingIsKnownAboutTheExtension_theNameIsUsedAsTyped() {
        assertEquals("Summer", resolve("Summer", current = null, mime = null))
    }
    @Test
    fun aFileWithAnUncommonExtension_keepsItWhenTypedAndWhenOmitted() {
        // m2v is not in the list of video extensions, so it is the file's own that makes it an extension.
        assertEquals("Summer.m2v", resolve("Summer.m2v", current = "Clip.m2v", mime = null))
        assertEquals("Summer.m2v", resolve("Summer", current = "Clip.m2v", mime = null))
    }
}
