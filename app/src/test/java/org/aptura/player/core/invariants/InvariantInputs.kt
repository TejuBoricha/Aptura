package org.aptura.player.core.invariants

import org.junit.Assert.assertTrue
import java.io.File

/**
 * Locates the release-variant files the invariant tests examine. Gradle passes their exact paths as system properties
 * (see `InvariantInputs` in app/build.gradle.kts) and makes the unit-test task depend on the tasks that produce them.
 * Run from an IDE without those properties, the conventional build locations are tried; a missing file FAILS the test —
 * a guard that quietly skips when its input is absent is no guard.
 */
object InvariantFiles {

    private fun resolve(property: String, fallback: String, producedBy: String): File {
        val file = System.getProperty(property)?.let(::File) ?: File(fallback)
        assertTrue(
            "Invariant input ${file.path} is missing. Run `./gradlew testDebugUnitTest` (it runs $producedBy first) instead of " +
                "executing this test class on its own.",
            file.isFile
        )
        return file
    }

    val releaseManifest: File
        get() = resolve(
            "aptura.invariants.releaseManifest",
            "build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml",
            "processReleaseMainManifest"
        )

    val releaseDependencies: File
        get() = resolve(
            "aptura.invariants.releaseDependencies",
            "build/invariants/release-runtime-dependencies.txt",
            "writeReleaseRuntimeDependencies"
        )
}
