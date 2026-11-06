package org.aptura.player.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Audit DATA-02: which volume a folder path names, and when a move stays on the video's volume. */
class StorageVolumesTest {

    @Test
    fun theHomeOfThePhone_isThePrimaryVolume() {
        assertEquals(StorageVolumes.PRIMARY, StorageVolumes.ofFolderPath("/storage/emulated/0"))
        assertEquals(StorageVolumes.PRIMARY, StorageVolumes.ofFolderPath("/storage/emulated/0/Movies"))
        assertEquals(StorageVolumes.PRIMARY, StorageVolumes.ofFolderPath("  /storage/emulated/0/DCIM/Camera  "))
    }

    @Test
    fun anSdCardFolder_isNamedByItsIdInLowerCase() {
        assertEquals("1a2b-3c4d", StorageVolumes.ofFolderPath("/storage/1A2B-3C4D/Movies"))
        assertEquals("1a2b-3c4d", StorageVolumes.ofFolderPath("/storage/1A2B-3C4D"))
    }

    @Test
    fun aPathWithNoVolumeInIt_namesNone() {
        assertNull(StorageVolumes.ofFolderPath("Movies/"))
        assertNull(StorageVolumes.ofFolderPath("Download/Sub Folder"))
        assertNull(StorageVolumes.ofFolderPath(""))
    }

    @Test
    fun aMoveStaysOnItsVolume_onlyWhenTheVolumesMatch() {
        assertTrue(StorageVolumes.sameVolume("external_primary", "/storage/emulated/0/Movies"))
        assertFalse(StorageVolumes.sameVolume("external_primary", "/storage/1A2B-3C4D/Movies"))
        assertTrue(StorageVolumes.sameVolume("1a2b-3c4d", "/storage/1A2B-3C4D/Download"))
        assertFalse(StorageVolumes.sameVolume("1a2b-3c4d", "/storage/emulated/0/Movies"))
        assertFalse(StorageVolumes.sameVolume("1a2b-3c4d", "/storage/9Z9Z-9Z9Z/Movies"))
    }

    @Test
    fun aVolumeIsComparedWithoutRegardToCase() {
        assertTrue(StorageVolumes.sameVolume("1A2B-3C4D", "/storage/1a2b-3c4d/Movies"))
        assertTrue(StorageVolumes.sameVolume("EXTERNAL_PRIMARY", "/storage/emulated/0/Movies"))
    }

    @Test
    fun aFolderWithNoVolume_isWhereverTheVideoAlreadyIs() {
        assertTrue(StorageVolumes.sameVolume("1a2b-3c4d", "Movies/"))
        assertTrue(StorageVolumes.sameVolume("external_primary", "Movies/"))
        assertTrue(StorageVolumes.sameVolume(null, "Movies/"))
    }

    @Test
    fun anUnknownSourceVolume_isThePhonesOwnStorage() {
        assertTrue(StorageVolumes.sameVolume(null, "/storage/emulated/0/Movies"))
        assertTrue(StorageVolumes.sameVolume("", "/storage/emulated/0/Movies"))
        assertTrue(StorageVolumes.sameVolume("   ", "/storage/emulated/0/Movies"))
        assertFalse(StorageVolumes.sameVolume(null, "/storage/1A2B-3C4D/Movies"))
    }
}
