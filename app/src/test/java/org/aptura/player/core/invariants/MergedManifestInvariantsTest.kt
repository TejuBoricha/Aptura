package org.aptura.player.core.invariants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit P1-20: the invariants were checked against `src/main/AndroidManifest.xml`, which is not what ships. These tests
 * parse the MERGED release manifest — ours plus every library's — and hold it to an explicit allow-list.
 * The fixtures at the bottom prove each rule fires on a real violation, so a green run means something.
 */
class MergedManifestInvariantsTest {

    // ---- the real thing ------------------------------------------------------------------------------------------

    private val shipped by lazy { MergedManifest.parse(InvariantFiles.releaseManifest) }

    @Test
    fun theShippedManifest_meetsEveryInvariant() {
        val problems = ManifestInvariants.violations(shipped)

        assertTrue("The merged release manifest violates the invariants:\n" + problems.joinToString("\n") { " - $it" }, problems.isEmpty())
    }

    @Test
    fun theShippedApp_requestsNoInternetPermission() {
        assertTrue(
            "INTERNET appears in the merged manifest (a library may have added it): ${shipped.permissions.keys}",
            "android.permission.INTERNET" !in shipped.permissions
        )
    }

    @Test
    fun theParserSeesWhatLibrariesAdded_notJustOurOwnManifest() {
        // Guards the guard: if the merged manifest were swapped for the source one, this would stop being true.
        assertTrue("android.permission.WAKE_LOCK" in shipped.permissions)
        assertTrue(shipped.components.any { it.name == "androidx.work.impl.background.systemjob.SystemJobService" })
        assertEquals("org.aptura.player", shipped.packageName)
    }

    @Test
    fun onlyTheApprovedComponents_areReachableByOtherApps() {
        val exported = shipped.components.filter { it.exported }.map { it.name }.toSet()

        assertEquals(ManifestInvariants.approvedExportedComponents.keys, exported)
    }

    // ---- fixtures: each rule must fire on a real violation ---------------------------------------------------------

    private fun manifest(
        permissions: String = "",
        application: String = "android:allowBackup=\"false\"",
        components: String = ""
    ) = MergedManifest.parse(
        """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="org.aptura.player">
            <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
            <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
            <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
            <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
            $permissions
            <application $application>
                <activity android:name="org.aptura.player.MainActivity" android:exported="true" />
                $components
            </application>
        </manifest>
        """.trimIndent()
    )

    @Test
    fun fixture_aCleanManifestPasses() {
        assertEquals(emptyList<String>(), ManifestInvariants.violations(manifest()))
    }

    @Test
    fun fixture_internetIsCaught() {
        val problems = ManifestInvariants.violations(manifest(permissions = """<uses-permission android:name="android.permission.INTERNET" />"""))

        assertTrue(problems.toString(), problems.any { it.contains("android.permission.INTERNET") })
    }

    @Test
    fun fixture_aPermissionAddedByALibrary_isCaughtUntilReviewed() {
        val problems = ManifestInvariants.violations(
            manifest(permissions = """<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />""")
        )

        assertTrue(problems.toString(), problems.any { it.contains("unapproved permission android.permission.ACCESS_FINE_LOCATION") })
    }

    @Test
    fun fixture_anUncappedReadExternalStorage_isCaught() {
        val xml = """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="org.aptura.player">
                <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
                <application android:allowBackup="false" />
            </manifest>
        """.trimIndent()

        val problems = ManifestInvariants.violations(MergedManifest.parse(xml))

        assertTrue(problems.toString(), problems.any { it.contains("READ_EXTERNAL_STORAGE must be capped") })
    }

    @Test
    fun fixture_anExportedReceiverWithoutAPermission_isCaught() {
        val problems = ManifestInvariants.violations(
            manifest(components = """<receiver android:name="com.example.Open" android:exported="true" />""")
        )

        assertTrue(problems.toString(), problems.any { it.contains("unapproved exported component com.example.Open") })
    }

    @Test
    fun fixture_aComponentWithAnIntentFilterIsExportedEvenWithoutTheAttribute() {
        val problems = ManifestInvariants.violations(
            manifest(
                components = """
                    <service android:name="com.example.Implicit">
                        <intent-filter><action android:name="com.example.ACTION" /></intent-filter>
                    </service>
                """.trimIndent()
            )
        )

        assertTrue(problems.toString(), problems.any { it.contains("com.example.Implicit") })
    }

    @Test
    fun fixture_anApprovedExportedComponentLosingItsPermissionGate_isCaught() {
        val problems = ManifestInvariants.violations(
            manifest(components = """<receiver android:name="androidx.work.impl.diagnostics.DiagnosticsReceiver" android:exported="true" />""")
        )

        assertTrue(problems.toString(), problems.any { it.contains("must be protected by android.permission.DUMP") })
    }

    @Test
    fun fixture_backupDebuggableAndCleartext_areCaught() {
        val problems = ManifestInvariants.violations(
            manifest(application = "android:allowBackup=\"true\" android:debuggable=\"true\" android:usesCleartextTraffic=\"true\"")
        )

        assertTrue(problems.toString(), problems.any { it.contains("allowBackup") })
        assertTrue(problems.toString(), problems.any { it.contains("debuggable") })
        assertTrue(problems.toString(), problems.any { it.contains("usesCleartextTraffic") })
    }
}
