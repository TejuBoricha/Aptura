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
    val output = runShell(command)
    // `pm grant` prints nothing on success. Anything else is an error that must not be ignored.
    check(output.isBlank()) { "`$command` failed: ${output.trim()}" }
}

/**
 * Runs [command] as the shell user and returns what it printed to stdout, after it has finished. Keep the command plain:
 * on API 26 it was split on spaces and run without a shell, so quotes, redirects and `;` did not work, and the command's
 * error output did not come back. A command that must have worked needs its effect checked, not only its output.
 */
internal fun runShell(command: String): String {
    // Reading to the end of the stream is what waits for the shell command to finish.
    return ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    ).use { it.readBytes().decodeToString() }
}
