package org.aptura.player.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** One swipe moves one folder; at either end it stays put instead of teleporting to the other end. */
class FolderSwipeTest {

    private val folders = listOf("Movies", "Series", "Camera")

    @Test
    fun theOrderStartsWithAllVideos() {
        assertEquals(listOf(null, "Movies", "Series", "Camera"), folderOrder(folders))
    }

    @Test
    fun swipingForward_movesToTheNextFolder() {
        assertEquals(1 to "Movies", neighbouringFolder(folders, null, +1))
        assertEquals(3 to "Camera", neighbouringFolder(folders, "Series", +1))
    }

    @Test
    fun swipingBack_movesToThePreviousFolder_andFromTheFirstFolderToAllVideos() {
        assertEquals(1 to "Movies", neighbouringFolder(folders, "Series", -1))
        assertEquals(0 to null, neighbouringFolder(folders, "Movies", -1))
    }

    @Test
    fun atEitherEnd_thereIsNowhereToGo() {
        assertNull("no wrap from the last folder to All videos", neighbouringFolder(folders, "Camera", +1))
        assertNull("no wrap from All videos to the last folder", neighbouringFolder(folders, null, -1))
    }

    @Test
    fun aFolderThatNoLongerExists_isTreatedAsAllVideos() {
        assertEquals(1 to "Movies", neighbouringFolder(folders, "Deleted", +1))
    }

    @Test
    fun withNoFolders_thereIsNothingToSwipeTo() {
        assertNull(neighbouringFolder(emptyList(), null, +1))
        assertNull(neighbouringFolder(emptyList(), null, -1))
    }
}
