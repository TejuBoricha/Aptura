package org.aptura.player.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Architectural Decoupling Invariant Test (RQ-09 & ADR-001).
 * Verifies that the domain layer (org.aptura.player.domain) is completely pure Kotlin
 * and contains ZERO dependencies on Android framework or AndroidX APIs.
 * This guarantees clean architecture and future cross-platform/iOS portability.
 */
class ArchitectureDecouplingTest {

    @Test
    fun domainLayer_containsZeroAndroidImports() {
        // Resolve project domain directory relative to module root
        val domainDir = File("src/main/java/org/aptura/player/domain")
        assertTrue("Domain directory must exist at ${domainDir.absolutePath}", domainDir.exists())

        val kotlinFiles = domainDir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("Domain directory must contain Kotlin source files", kotlinFiles.isNotEmpty())

        val forbiddenImportPrefixes = listOf(
            "import android.",
            "import androidx."
        )

        val violations = mutableListOf<String>()

        for (file in kotlinFiles) {
            val lines = file.readLines()
            for ((index, line) in lines.withIndex()) {
                val trimmed = line.trim()
                for (prefix in forbiddenImportPrefixes) {
                    if (trimmed.startsWith(prefix)) {
                        violations.add("${file.name}:${index + 1} -> $trimmed")
                    }
                }
            }
        }

        assertTrue(
            "Domain layer MUST be pure Kotlin with zero Android/AndroidX dependencies.\nViolations found:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
