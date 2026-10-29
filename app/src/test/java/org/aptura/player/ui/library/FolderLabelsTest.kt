package org.aptura.player.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

/** Two folders must never read the same: with identical chips there is no telling which one is selected. */
class FolderLabelsTest {

    @Test
    fun aFolderIsNamedByItsLastSegment() {
        assertEquals(mapOf("Movies" to "Movies", "DCIM/Camera" to "Camera"), folderLabels(listOf("Movies", "DCIM/Camera")))
    }

    @Test
    fun twoFoldersWithTheSameLastSegment_arePrefixedWithTheirParent() {
        val labels = folderLabels(listOf("Severance/Season 1", "Andor/Season 1", "Andor/Season 2"))

        assertEquals("Season 1 (Severance)", labels.getValue("Severance/Season 1"))
        assertEquals("Season 1 (Andor)", labels.getValue("Andor/Season 1"))
        assertEquals("a folder whose name is unique is left alone", "Season 2", labels.getValue("Andor/Season 2"))
        assertEquals("no two folders share a label", 3, labels.values.toSet().size)
    }

    @Test
    fun aTopLevelDuplicate_fallsBackToInternalStorage() {
        val labels = folderLabels(listOf("Camera", "SD/Camera"))

        assertEquals("Camera (Internal Storage)", labels.getValue("Camera"))
        assertEquals("Camera (SD)", labels.getValue("SD/Camera"))
    }

    @Test
    fun aTrailingSlashDoesNotChangeTheName() {
        assertEquals("Movies", folderLabels(listOf("storage/Movies/")).getValue("storage/Movies/"))
    }

    // ---- keys from the device's media library are absolute paths on a storage volume -------------------------------------

    private val phoneCamera = "/storage/emulated/0/DCIM/Camera"
    private val cardCamera = "/storage/1A2B-3C4D/DCIM/Camera"

    @Test
    fun theSameNameOnThePhoneAndOnAnSdCard_isToldApartByTheStorage() {
        val labels = folderLabels(listOf(phoneCamera, cardCamera))

        assertEquals("Camera (DCIM, Internal)", labels.getValue(phoneCamera))
        assertEquals("Camera (DCIM, SD card)", labels.getValue(cardCamera))
    }

    @Test
    fun aTopLevelFolderOnEachStorage_isToldApartByTheStorageToo() {
        val labels = folderLabels(listOf("/storage/emulated/0/Download", "/storage/1A2B-3C4D/Download"))

        assertEquals("Download (Internal)", labels.getValue("/storage/emulated/0/Download"))
        assertEquals("Download (SD card)", labels.getValue("/storage/1A2B-3C4D/Download"))
    }

    @Test
    fun aNameThatIsNotShared_isJustTheName() {
        val labels = folderLabels(listOf(phoneCamera, "/storage/emulated/0/Movies", "/storage/1A2B-3C4D/Music videos"))

        assertEquals("Camera", labels.getValue(phoneCamera))
        assertEquals("Movies", labels.getValue("/storage/emulated/0/Movies"))
        assertEquals("Music videos", labels.getValue("/storage/1A2B-3C4D/Music videos"))
    }

    @Test
    fun theTopOfAStorage_isNamedAfterTheStorage_notAfterAPathSegment() {
        assertEquals("Internal Storage", friendlyFolderName("/storage/emulated/0"))
        assertEquals("SD card", friendlyFolderName("/storage/1A2B-3C4D"))
        assertEquals("Camera", friendlyFolderName("/storage/emulated/0/DCIM/Camera"))
    }

    @Test
    fun noTwoFoldersEverShareALabel() {
        val folders = listOf(
            phoneCamera, cardCamera, "/storage/5E6F-7A8B/DCIM/Camera", "/storage/emulated/0/Movies/Camera",
            "/storage/emulated/0/Download", "/storage/1A2B-3C4D/Download", "Season 1", "Show/Season 1", "Other/Season 1"
        )
        val labels = folderLabels(folders)
        assertEquals("every label is different: $labels", folders.size, labels.values.toSet().size)
    }
}
