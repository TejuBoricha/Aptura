package org.aptura.player

import android.Manifest
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Grants the app the permission that gates the library on the API level the test runs on.
 *
 * `UiAutomation.grantRuntimePermission` only exists from API 28, so on the API 26 nightly leg it threw
 * NoSuchMethodError before any test ran. A shell `pm grant` works on every level the tests run on.
 * READ_MEDIA_VIDEO is only known to the system from API 33; below that the manifest asks for
 * READ_EXTERNAL_STORAGE (`maxSdkVersion="32"`).
 */
internal fun grantMediaAccess() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val command = "pm grant ${instrumentation.targetContext.packageName} $permission"
    // Reading to the end of the stream is what waits for the shell command to finish.
    val output = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
        .use { it.readBytes().decodeToString() }
    // `pm grant` prints nothing on success. Anything else is an error that must not be ignored.
    check(output.isBlank()) { "`$command` failed: ${output.trim()}" }
}
