package org.aptura.player.core.platform

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Verifies Android 14+ partial visual access (UX-01), Android 13 scoped access, and legacy API <= 32 access.
 * Crucially verifies [PermissionHelper.hasFullStoragePermission] stays false under [MediaAccessLevel.LIMITED]
 * so `MissingRowPurge` never prunes unselected library entries.
 */
@RunWith(RobolectricTestRunner::class)
class PermissionHelperTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun api34_distinguishesNoneLimitedAndFullAccess() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(app)

        assertArrayEquals(
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ),
            PermissionHelper.getRequiredPermissions()
        )
        assertEquals(MediaAccessLevel.NONE, PermissionHelper.getMediaAccessLevel(app))
        assertFalse(PermissionHelper.hasStoragePermission(app))
        assertFalse(PermissionHelper.hasFullStoragePermission(app))

        shadowApp.grantPermissions(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        assertEquals(MediaAccessLevel.LIMITED, PermissionHelper.getMediaAccessLevel(app))
        assertTrue("Selected videos must be scannable via MediaStore", PermissionHelper.hasStoragePermission(app))
        assertFalse("Partial access must NOT enable full-library missing-row purge", PermissionHelper.hasFullStoragePermission(app))

        shadowApp.grantPermissions(Manifest.permission.READ_MEDIA_VIDEO)
        assertEquals(MediaAccessLevel.FULL, PermissionHelper.getMediaAccessLevel(app))
        assertTrue(PermissionHelper.hasStoragePermission(app))
        assertTrue(PermissionHelper.hasFullStoragePermission(app))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun api33_requestsOnlyReadMediaVideo() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
            PermissionHelper.getRequiredPermissions()
        )
        assertEquals(MediaAccessLevel.NONE, PermissionHelper.getMediaAccessLevel(app))

        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_VIDEO)
        assertEquals(MediaAccessLevel.FULL, PermissionHelper.getMediaAccessLevel(app))
        assertTrue(PermissionHelper.hasFullStoragePermission(app))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun api30_requestsOnlyReadExternalStorage() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            PermissionHelper.getRequiredPermissions()
        )
        assertEquals(MediaAccessLevel.NONE, PermissionHelper.getMediaAccessLevel(app))

        shadowOf(app).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertEquals(MediaAccessLevel.FULL, PermissionHelper.getMediaAccessLevel(app))
        assertTrue(PermissionHelper.hasFullStoragePermission(app))
    }
}
