package org.aptura.player.core.invariants

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Automated Platform Invariants Gate (spec/10, spec/12, spec/18, DEC-011, DEC-012).
 * Formally enforces legal, privacy, and architecture invariants by inspecting the actual
 * AndroidManifest.xml and app/build.gradle.kts files on every test run.
 * If any commit, library, or configuration violates these rules, the build FAILS immediately.
 */
class PlatformInvariantsTest {

    private val manifestFile = File("src/main/AndroidManifest.xml")
    private val buildGradleFile = File("build.gradle.kts")

    private fun getDeclaredPermissions(xmlContent: String): List<String> {
        // Strip XML comments first
        val noComments = xmlContent.replace(Regex("""<!--[\s\S]*?-->"""), "")
        val permissionRegex = Regex("""<uses-permission\s+[^>]*android:name="([^"]+)"[^>]*/>""")
        return permissionRegex.findAll(noComments).map { it.groupValues[1] }.toList()
    }

    @Test
    fun manifest_containsZeroInternetPermission() {
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val permissions = getDeclaredPermissions(manifestFile.readText())

        assertFalse(
            "CRITICAL INVARIANT VIOLATION (DEC-012): AndroidManifest.xml must NEVER declare android.permission.INTERNET",
            permissions.any { it.contains("INTERNET", ignoreCase = true) }
        )
    }

    @Test
    fun manifest_containsZeroManageExternalStoragePermission() {
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val permissions = getDeclaredPermissions(manifestFile.readText())

        assertFalse(
            "CRITICAL INVARIANT VIOLATION (DEC-011): AndroidManifest.xml must NEVER declare MANAGE_EXTERNAL_STORAGE",
            permissions.any { it.contains("MANAGE_EXTERNAL_STORAGE", ignoreCase = true) }
        )
    }

    @Test
    fun manifest_containsZeroPostNotificationsPermission() {
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val permissions = getDeclaredPermissions(manifestFile.readText())

        assertFalse(
            "CRITICAL INVARIANT VIOLATION (FR-PLAT-002, W10): Playback notifications are platform-exempt. Do NOT request POST_NOTIFICATIONS.",
            permissions.any { it.contains("POST_NOTIFICATIONS", ignoreCase = true) }
        )
    }

    @Test
    fun manifest_disablesAllowBackup() {
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()
        assertTrue(
            "CRITICAL INVARIANT VIOLATION (S-01): AndroidManifest.xml must set android:allowBackup=\"false\" to prevent cloud leakage of local library data",
            content.contains("android:allowBackup=\"false\"")
        )
    }

    @Test
    fun manifest_containsZeroAudioPermission() {
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val permissions = getDeclaredPermissions(manifestFile.readText())
        assertFalse(
            "CRITICAL INVARIANT VIOLATION (S-03): Aptura is strictly a video player; do NOT request READ_MEDIA_AUDIO",
            permissions.any { it.contains("READ_MEDIA_AUDIO", ignoreCase = true) }
        )
    }

    @Test
    fun manifest_declaresMediaPlaybackForegroundService() {
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()

        assertTrue(
            "ApturaPlaybackService must be declared with foregroundServiceType=\"mediaPlayback\"",
            content.contains("android:name=\".data.playback.ApturaPlaybackService\"") &&
                content.contains("android:foregroundServiceType=\"mediaPlayback\"")
        )
    }

    // The "no telemetry SDK" check used to be a substring search of this build file. It could not see transitive
    // libraries and flagged any line containing the word "adjust". It now lives in DependencyInvariantsTest, which reads
    // the resolved release runtime graph; the merged-manifest rules are in MergedManifestInvariantsTest.

    @Test
    fun buildGradle_disablesLanguageDynamicSplit() {
        assertTrue("app/build.gradle.kts must exist", buildGradleFile.exists())
        val content = buildGradleFile.readText()

        assertTrue(
            "CRITICAL INVARIANT VIOLATION (RQ-14, DEC-012): bundle.language.enableSplit MUST be false for offline-only operation",
            content.contains("enableSplit = false")
        )
    }

    @Test
    fun manifest_declaresResizableActivity() {
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()
        assertTrue(
            "CRITICAL INVARIANT VIOLATION (R-A11): AndroidManifest.xml must declare android:resizeableActivity=\"true\"",
            content.contains("android:resizeableActivity=\"true\"")
        )
    }

    @Test
    fun manifest_declaresDisplayCutoutShortEdges() {
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()
        assertTrue(
            "CRITICAL INVARIANT VIOLATION (R-B14): AndroidManifest.xml must declare android:windowLayoutInDisplayCutoutMode=\"shortEdges\"",
            content.contains("android:windowLayoutInDisplayCutoutMode=\"shortEdges\"")
        )
    }

    @Test
    fun manifest_declaresZeroFixedScreenOrientation() {
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()
        assertFalse(
            "CRITICAL INVARIANT VIOLATION (R-B02): MainActivity must never declare android:screenOrientation; it must remain resizable and continuum-fluid",
            content.contains("android:screenOrientation")
        )
    }
}
