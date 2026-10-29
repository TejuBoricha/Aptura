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
}
