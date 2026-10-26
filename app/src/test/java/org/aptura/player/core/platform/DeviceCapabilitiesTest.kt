package org.aptura.player.core.platform

import android.content.pm.PackageManager
import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * PiP is advertised as optional in the manifest and hidden in the player when the device cannot float a window
 * (F-07). These tests pin the runtime half of that contract, including devices that report the feature but run an
 * API too old to use it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceCapabilitiesTest {

    private val packageManager: PackageManager
        get() = RuntimeEnvironment.getApplication().packageManager

    @Test
    fun aDeviceThatReportsPip_isSupported() {
        shadowOf(packageManager).setSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE, true)

        assertTrue(DeviceCapabilities.supportsPictureInPicture(packageManager))
    }

    @Test
    fun aDeviceWithoutTheFeature_hidesPip() {
        shadowOf(packageManager).setSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE, false)

        assertFalse(DeviceCapabilities.supportsPictureInPicture(packageManager))
    }

    @Test
    fun belowApi26_pipIsUnavailable_evenWhenTheFeatureIsReported() {
        shadowOf(packageManager).setSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE, true)

        assertFalse(
            DeviceCapabilities.supportsPictureInPicture(packageManager, sdkInt = Build.VERSION_CODES.N)
        )
    }
}
