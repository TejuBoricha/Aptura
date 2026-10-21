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

    @Test
    fun buildGradle_containsZeroThirdPartyTelemetrySdk() {
        assertTrue("app/build.gradle.kts must exist", buildGradleFile.exists())
        val content = buildGradleFile.readText().lowercase()

        val bannedTelemetryKeywords = listOf(
            "firebase",
            "crashlytics",
            "sentry",
            "mixpanel",
            "datadog",
            "newrelic",
            "appsflyer",
            "adjust"
        )

        for (banned in bannedTelemetryKeywords) {
            assertFalse(
                "CRITICAL INVARIANT VIOLATION (ADR-007): Banned telemetry SDK detected in build.gradle.kts: $banned",
                content.contains(banned)
            )
        }
    }

    @Test
    fun buildGradle_disablesLanguageDynamicSplit() {
        assertTrue("app/build.gradle.kts must exist", buildGradleFile.exists())
        val content = buildGradleFile.readText()

        assertTrue(
            "CRITICAL INVARIANT VIOLATION (RQ-14, DEC-012): bundle.language.enableSplit MUST be false for offline-only operation",
            content.contains("enableSplit = false")
        )
    }
}
