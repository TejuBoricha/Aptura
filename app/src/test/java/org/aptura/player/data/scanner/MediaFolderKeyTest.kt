package org.aptura.player.data.scanner

import android.os.Build
import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A folder is identified by where it is, not by what it is called: two folders named Camera (the phone's and an SD
 * card's) used to be one folder in the library, with their videos mixed together.
 */
class MediaFolderKeyTest {

    @Test
    fun twoFoldersWithTheSameName_onDifferentStorage_getDifferentKeys() {
        val phone = MediaFolderKey.of("external_primary", "DCIM/Camera/", null, "Camera")
        val sdCard = MediaFolderKey.of("1a2b-3c4d", "DCIM/Camera/", null, "Camera")

        assertEquals("/storage/emulated/0/DCIM/Camera", phone)
        assertEquals("/storage/1A2B-3C4D/DCIM/Camera", sdCard)
        assertNotEquals(phone, sdCard)
    }

    @Test
    fun twoFoldersWithTheSameName_inDifferentPlaces_getDifferentKeys() {
        val one = MediaFolderKey.of("external_primary", "Movies/Download/", null, "Download")
        val other = MediaFolderKey.of("external_primary", "Download/", null, "Download")
        assertNotEquals(one, other)
    }

    @Test
    fun theSameFolder_alwaysGetsTheSameKey_whateverTheSlashes() {
        val keys = setOf(
            MediaFolderKey.of("external_primary", "DCIM/Camera/", null, "Camera"),
            MediaFolderKey.of("external_primary", "DCIM/Camera", null, "Camera"),
            MediaFolderKey.of(null, "/DCIM/Camera/", null, "Camera"),
            MediaFolderKey.of(null, null, "/storage/emulated/0/DCIM/Camera/VID_1.mp4", "Camera"),
            MediaFolderKey.of(null, null, "/sdcard/DCIM/Camera/VID_1.mp4", "Camera"),
            MediaFolderKey.of(null, null, "/storage/self/primary/DCIM/Camera/VID_1.mp4", "Camera")
        )
        assertEquals("every spelling of the same folder is one key: $keys", 1, keys.size)
    }

    @Test
    fun aVideoAtTheTopOfAStorage_isInThatStoragesRoot() {
        assertEquals("/storage/emulated/0", MediaFolderKey.of("external_primary", "", null, "0"))
        assertEquals("/storage/1A2B-3C4D", MediaFolderKey.of("1a2b-3c4d", "", null, null))
    }

    @Test
    fun beforeAndroid10_theFileSPathIsUsed() {
        assertEquals("/storage/1A2B-3C4D/Movies", MediaFolderKey.of(null, null, "/storage/1A2B-3C4D/Movies/a.mp4", "Movies"))
    }

    @Test
    fun whenThePlatformSaysNothingAboutWhereItIs_theBucketNameIsTheFallback() {
        assertEquals("Camera", MediaFolderKey.of(null, null, null, "Camera"))
        assertEquals("a blank bucket name is no name", "Internal Storage", MediaFolderKey.of(null, null, "", " "))
        assertEquals("Internal Storage", MediaFolderKey.of(null, null, null, null))
    }

    @Test
    fun aFilePathThatIsNotAbsolute_isNotTrusted() {
        assertNull(MediaFolderKey.fromFilePath("Camera/VID_1.mp4"))
        assertNull(MediaFolderKey.fromFilePath("VID_1.mp4"))
        assertNull(MediaFolderKey.fromFilePath(null))
    }

    @Test
    fun theScannerAsksForTheLocationColumns_thatTheAndroidVersionHas() {
        val modern = MediaStoreScanner.projection(Build.VERSION_CODES.Q).toList()
        assertTrue(MediaStore.MediaColumns.RELATIVE_PATH in modern)
        assertTrue(MediaStore.MediaColumns.VOLUME_NAME in modern)
        @Suppress("DEPRECATION")
        assertFalse("the deprecated file path is not requested where the documented columns exist", MediaStore.Video.Media.DATA in modern)

        val old = MediaStoreScanner.projection(Build.VERSION_CODES.P).toList()
        @Suppress("DEPRECATION")
        assertTrue(MediaStore.Video.Media.DATA in old)
        assertFalse(MediaStore.MediaColumns.RELATIVE_PATH in old)
        assertTrue("the bucket name stays as the last resort", MediaStore.Video.Media.BUCKET_DISPLAY_NAME in old)
    }
}
