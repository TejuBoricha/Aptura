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

    @Test
    fun theShippedApp_advertisesPictureInPictureAsOptional() {
        val pip = shipped.features.single { it.name == ManifestInvariants.PIP_FEATURE }

        assertTrue("PiP must stay optional so devices without it can install Aptura", !pip.required)
        assertTrue(
            "MainActivity must enable PiP at runtime",
            shipped.components.any { it.name == "org.aptura.player.MainActivity" && it.supportsPictureInPicture }
        )
    }

    @Test
    fun theShippedApp_advertisesOnlyPhoneAndTabletIntentCategories() {
        assertEquals(ManifestInvariants.approvedIntentCategories, shipped.intentCategories.toSet())
    }

    @Test
    fun theShippedManifest_doesNotShipTheDebugDocumentsProvider() {
        val authorities = shipped.components.flatMap { it.authorities }

        assertTrue(
            "The debug-only DocumentsProvider authority leaked into the release manifest: $authorities",
            "org.aptura.player.test.documents" !in authorities
        )
        assertTrue(
            "The debug-only provider class leaked into the release manifest",
            shipped.components.none { it.name.endsWith("RenamingDocumentsProvider") }
        )
    }

    // ---- fixtures: each rule must fire on a real violation ---------------------------------------------------------

    @Test
    fun fixture_providerAuthoritiesAreParsedWithTheirSemicolonSeparator() {
        val parsed = manifest(
            components = """
                <provider
                    android:name="com.example.DebugProvider"
                    android:authorities="com.example.one; com.example.two"
                    android:exported="false" />
            """.trimIndent()
        )

        assertEquals(
            listOf("com.example.one", "com.example.two"),
            parsed.components.single { it.name == "com.example.DebugProvider" }.authorities
        )
    }

    private fun manifest(
        permissions: String = "",
        features: String = """<uses-feature android:name="android.software.picture_in_picture" android:required="false" />""",
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
            $features
            <application $application>
                <activity
                    android:name="org.aptura.player.MainActivity"
                    android:exported="true"
                    android:supportsPictureInPicture="true" />
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

    @Test
    fun fixture_aRequiredPictureInPictureFeature_isCaught() {
        val problems = ManifestInvariants.violations(
            manifest(features = """<uses-feature android:name="android.software.picture_in_picture" android:required="true" />""")
        )

        assertTrue(problems.toString(), problems.any { it.contains("required=\"false\"") })
    }

    @Test
    fun fixture_aCameraFeature_isForbiddenEvenWhenOptional() {
        val problems = ManifestInvariants.violations(
            manifest(features = """<uses-feature android:name="android.hardware.camera.any" android:required="false" />""")
        )

        assertTrue(problems.toString(), problems.any { it.contains("forbidden feature android.hardware.camera.any") })
    }

    @Test
    fun fixture_aLeanbackLauncherCategory_isCaught() {
        val problems = ManifestInvariants.violations(
            manifest(
                components = """
                    <activity android:name="com.example.Tv">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
                        </intent-filter>
                    </activity>
                """.trimIndent()
            )
        )

        assertTrue(problems.toString(), problems.any { it.contains("android.intent.category.LEANBACK_LAUNCHER") })
    }

    @Test
    fun fixture_anActivityThatSupportsPipWithoutTheOptionalFeature_isCaught() {
        val problems = ManifestInvariants.violations(manifest(features = ""))

        assertTrue(problems.toString(), problems.any { it.contains("does not declare android.software.picture_in_picture") })
    }

    @Test
    fun fixture_thePipFeatureWithoutAnySupportingActivity_isCaught() {
        val xml = """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="org.aptura.player">
                <uses-feature android:name="android.software.picture_in_picture" android:required="false" />
                <application android:allowBackup="false">
                    <activity android:name="org.aptura.player.MainActivity" android:exported="true" />
                </application>
            </manifest>
        """.trimIndent()

        val problems = ManifestInvariants.violations(MergedManifest.parse(xml))

        assertTrue(problems.toString(), problems.any { it.contains("no activity sets android:supportsPictureInPicture") })
    }
}
