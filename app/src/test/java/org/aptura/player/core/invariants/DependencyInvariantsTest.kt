package org.aptura.player.core.invariants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Audit P1-20: "no telemetry SDKs" was a substring search of app/build.gradle.kts — blind to transitive libraries and
 * false-positive on any line containing the word "adjust". These tests read the RESOLVED release runtime graph.
 * The fixtures prove each rule fires on a real violation.
 */
class DependencyInvariantsTest {

    private val libraries by lazy { ResolvedLibrary.parseReport(InvariantFiles.releaseDependencies.readText()) }

    // ---- the real graph --------------------------------------------------------------------------------------------

    @Test
    fun theReleaseGraph_isActuallyRead() {
        // Guards the guard: an empty or unrelated report would make every check below pass for nothing.
        assertTrue("only ${libraries.size} libraries parsed", libraries.size > 100)
        listOf("androidx.media3:media3-exoplayer:", "androidx.room:room-runtime:", "io.coil-kt.coil3:coil-core").forEach { prefix ->
            assertTrue("$prefix missing from the report", libraries.any { it.coordinate.startsWith(prefix) })
        }
    }

    @Test
    fun theReleaseClasspath_hasNoTelemetryCrashReportingOrAdSdk_evenTransitively() {
        val problems = DependencyInvariants.violations(libraries).filter { it.startsWith("telemetry") }

        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun theReleaseClasspath_hasNoHttpClient_atAll() {
        val problems = DependencyInvariants.violations(libraries)

        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun noApprovedException_isStale_soItCannotQuietlyExcuseSomethingElseLater() {
        val stale = DependencyInvariants.staleExceptions(libraries)

        assertTrue(stale.joinToString("\n"), stale.isEmpty())
    }

    @Test
    fun noBuildScriptAppliesATelemetryPlugin() {
        val files = listOf(File("build.gradle.kts"), File("../build.gradle.kts"), File("../settings.gradle.kts"), File("../gradle/libs.versions.toml"))
        assertTrue("build files not found from ${File(".").absoluteFile}", File("build.gradle.kts").exists())

        val problems = DependencyInvariants.telemetryPluginsIn(files)

        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    // ---- fixtures: each rule must fire on a real violation ---------------------------------------------------------

    private fun graph(vararg lines: String) = ResolvedLibrary.parseReport(lines.joinToString("\n"))

    @Test
    fun fixture_aTransitiveFirebaseLibrary_isCaught() {
        val problems = DependencyInvariants.violations(
            graph("com.google.firebase:firebase-analytics:22.0.0 <- com.example:nice-ui:1.0", "com.example:nice-ui:1.0 <- project :app")
        )

        assertEquals(1, problems.size)
        assertTrue(problems.single(), problems.single().contains("com.google.firebase:firebase-analytics") && problems.single().contains("nice-ui"))
    }

    @Test
    fun fixture_attributionAndCrashSdks_areCaught() {
        val sdks = listOf(
            "com.adjust.sdk:adjust-android:5.0.0", "io.sentry:sentry-android:7.0.0", "com.mixpanel.android:mixpanel-android:8.0.0",
            "com.datadoghq:dd-sdk-android-rum:2.0.0", "com.google.android.gms:play-services-measurement:21.0.0"
        )

        sdks.forEach { sdk ->
            assertEquals(sdk, 1, DependencyInvariants.violations(graph("$sdk <- project :app")).size)
        }
    }

    @Test
    fun fixture_theWordAdjust_isNotAFalsePositive() {
        val problems = DependencyInvariants.violations(
            graph(
                "androidx.compose.foundation:foundation-layout:1.10.0 <- project :app",
                "com.example:adjustable-sliders:1.0 <- project :app",
                "com.adjustment.tools:ruler:1.0 <- project :app"
            )
        )

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun fixture_okHttp_isAViolationWhoeverRequiresIt() {
        // Coil 2 pulled OkHttp in and R8 shipped it; with no approved exception that must fail the build.
        val problems = DependencyInvariants.violations(graph("com.squareup.okhttp3:okhttp:4.12.0 <- io.coil-kt:coil-base:2.7.0"))

        assertEquals(1, problems.size)
        assertTrue(problems.single(), problems.single().contains("HTTP client") && problems.single().contains("coil-base"))
    }

    private val excusedOkHttp = DependencyInvariants.Exception(
        coordinatePrefix = "com.squareup.okhttp3:okhttp:",
        onlyRequiredBy = setOf("io.coil-kt:coil-base:"),
        reason = "fixture"
    )

    @Test
    fun fixture_anApprovedException_excusesOnlyItsOwnPath() {
        assertEquals(
            emptyList<String>(),
            DependencyInvariants.violations(graph("com.squareup.okhttp3:okhttp:4.12.0 <- io.coil-kt:coil-base:2.7.0"), listOf(excusedOkHttp))
        )
        assertEquals(
            "a second path to the same library is a new HTTP client",
            1,
            DependencyInvariants.violations(
                graph("com.squareup.okhttp3:okhttp:4.12.0 <- io.coil-kt:coil-base:2.7.0, com.example:uploader:1.0"),
                listOf(excusedOkHttp)
            ).size
        )
    }

    @Test
    fun fixture_otherHttpStacks_areCaught() {
        val stacks = listOf(
            "com.squareup.retrofit2:retrofit:2.9.0", "io.ktor:ktor-client-core:2.3.0", "com.android.volley:volley:1.2.1",
            "org.apache.httpcomponents:httpclient:4.5.14", "com.squareup.okhttp3:logging-interceptor:4.12.0"
        )

        stacks.forEach { stack ->
            assertEquals(stack, 1, DependencyInvariants.violations(graph("$stack <- project :app")).size)
        }
    }

    @Test
    fun fixture_aStaleException_isReported() {
        val exceptions = listOf(excusedOkHttp)

        assertTrue(DependencyInvariants.staleExceptions(graph("androidx.core:core-ktx:1.18.0 <- project :app"), exceptions).isNotEmpty())
        assertFalse(
            DependencyInvariants.staleExceptions(graph("com.squareup.okhttp3:okhttp:4.12.0 <- io.coil-kt:coil-base:2.7.0"), exceptions).isNotEmpty()
        )
    }

    @Test
    fun fixture_aTelemetryPlugin_inABuildFile_isCaught() {
        val file = File.createTempFile("build", ".gradle.kts").apply {
            deleteOnExit()
            writeText("plugins { id(\"com.google.firebase.crashlytics\") }")
        }

        val problems = DependencyInvariants.telemetryPluginsIn(listOf(file))

        assertEquals(1, problems.size)
    }
}
