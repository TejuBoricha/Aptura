package org.aptura.player.core.invariants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * NOTICE is the list of who wrote what the app is built from and under which licence; the Open-source licences screen shows it.
 * A list written by hand goes stale the day a dependency is added, and until now it had: Guava, Okio, JetBrains' Compose and
 * AndroidX builds, Accompanist and JSpecify were on the release classpath and missing from it, and DataStore's repackaged
 * Protocol Buffers (BSD-3-Clause, which asks to be reproduced with a binary) was unmentioned.
 *
 * Like the privacy policy against the merged manifest ([PrivacyPolicyPermissionsTest]), NOTICE is held to the RESOLVED release
 * runtime graph: every library group must be named in a "Maven groups:" line, and every name in such a line must still exist.
 */
class NoticeCoverageTest {

    private fun noticeText(): String {
        val file = listOf("NOTICE", "../NOTICE").map(::File).firstOrNull { it.isFile }
        assertTrue("NOTICE was not found from ${File(".").absolutePath}", file != null)
        return file!!.readText(Charsets.UTF_8)
    }

    private val graph by lazy {
        ResolvedLibrary.parseReport(InvariantFiles.releaseDependencies.readText()).filterNot { it.coordinate.startsWith("project ") }
    }

    private fun listed(notice: String, label: String): List<String> =
        Regex("^\\s*$label:\\s*(.+)$", RegexOption.MULTILINE).findAll(notice)
            .flatMap { it.groupValues[1].split(',') }.map { it.trim() }.filter { it.isNotEmpty() }.toList()

    private fun covers(group: String, prefix: String) = group == prefix || group.startsWith("$prefix.")

    /** Everything wrong between NOTICE and the libraries the release build resolves. */
    private fun problems(notice: String, libraries: List<ResolvedLibrary>): List<String> = buildList {
        val groups = listed(notice, "Maven groups")
        val artifacts = listed(notice, "Maven artifacts")

        libraries.map { it.group }.toSortedSet().filter { group -> groups.none { covers(group, it) } }.forEach {
            add("the release classpath has group '$it' but NOTICE names no 'Maven groups:' entry for it")
        }
        groups.filter { prefix -> libraries.none { covers(it.group, prefix) } }.forEach {
            add("NOTICE lists group '$it' but no library of it is on the release classpath")
        }
        artifacts.filter { artifact -> libraries.none { "${it.group}:${it.name}" == artifact } }.forEach {
            add("NOTICE lists artifact '$it' but it is not on the release classpath")
        }
        // Every entry that claims libraries must say who wrote them and under which licence.
        notice.split(Regex("\n\\s*\n")).filter { it.contains("Maven groups:") || it.contains("Maven artifacts:") }.forEach { entry ->
            val name = entry.lines().first().take(60)
            if (!entry.contains("Copyright")) add("NOTICE entry '$name' names no copyright holder")
            if (!entry.contains("licen", ignoreCase = true)) add("NOTICE entry '$name' names no licence")
        }
    }

    // ---- the real thing --------------------------------------------------------------------------------------------

    @Test
    fun notice_coversEveryLibraryGroupOfTheReleaseClasspath_andNamesNothingThatIsGone() {
        val found = problems(noticeText(), graph)

        assertTrue("NOTICE disagrees with the resolved release classpath:\n" + found.joinToString("\n") { " - $it" }, found.isEmpty())
    }

    @Test
    fun theParser_findsTheEntriesAndTheGraph() {
        // Guards the guard: a reworded NOTICE that parsed to nothing, or an empty graph, would make the test above agree with anything.
        val notice = noticeText()

        assertTrue("only ${graph.size} libraries parsed", graph.size > 100)
        assertTrue("'androidx' not found in ${listed(notice, "Maven groups")}", "androidx" in listed(notice, "Maven groups"))
        assertTrue(
            listed(notice, "Maven artifacts").toString(),
            "androidx.datastore:datastore-preferences-external-protobuf" in listed(notice, "Maven artifacts")
        )
    }

    @Test
    fun theProtobufNotice_hasItsLicenceTextInTheApp() {
        // The BSD-3-Clause text must be reproduced with the binary: it is NOTICE's job to point at it and the screen's to show it.
        val text = File(InvariantFiles.licenseAssets, "open-source-licenses/Protobuf-LICENSE-BSD-3-Clause.txt")
        assertTrue("${text.path} is missing", text.isFile)
        assertTrue(text.readText().contains("Redistributions in binary form must reproduce the above"))
    }

    // ---- fixtures: each rule must fire on the thing that was wrong -------------------------------------------------

    private fun library(coordinate: String) = ResolvedLibrary(coordinate, setOf("project :app"))

    private val goodNotice = """
        Aptura

        Guava
          Copyright The Guava Authors
          Licensed under the Apache License, Version 2.0.
          Maven groups: com.google.guava

        Protocol Buffers
          Copyright 2008 Google Inc.
          Licensed under the BSD 3-Clause License.
          Maven artifacts: androidx.datastore:datastore-preferences-external-protobuf
    """.trimIndent()

    private val goodGraph = listOf(
        library("com.google.guava:guava:33.7.2-android"),
        library("com.google.guava:failureaccess:1.0.3"),
        library("androidx.datastore:datastore-preferences-external-protobuf:1.1.2")
    ) + library("androidx.datastore:datastore-preferences:1.1.2")

    @Test
    fun fixture_aCoveredGraph_passes() {
        // androidx.datastore is not a listed GROUP in this fixture, so add it: the artifact line alone does not cover the group.
        val notice = goodNotice.replace("Aptura", "Aptura\n\nAndroidX\n  Copyright The Android Open Source Project\n  Licensed under the Apache License, Version 2.0.\n  Maven groups: androidx")

        assertEquals(emptyList<String>(), problems(notice, goodGraph))
    }

    @Test
    fun fixture_aNewDependencyGroup_isCaught() {
        val found = problems(goodNotice, goodGraph + library("com.squareup.okio:okio:3.9.0"))

        assertTrue(found.toString(), found.any { it.contains("'com.squareup.okio'") })
    }

    @Test
    fun fixture_aGroupPrefix_matchesOnABoundaryOnly() {
        // 'org.jetbrains' covers 'org.jetbrains.kotlin' but 'com.google.guava' must not cover 'com.google.guavafake'.
        val found = problems(goodNotice, goodGraph + library("com.google.guavafake:x:1"))

        assertTrue(found.toString(), found.any { it.contains("'com.google.guavafake'") })
    }

    @Test
    fun fixture_aStaleEntry_isCaught() {
        val found = problems(goodNotice.replace("com.google.guava", "com.google.guava\n  Maven groups: com.removed.sdk"), goodGraph)

        assertTrue(found.toString(), found.any { it.contains("'com.removed.sdk'") })
    }

    @Test
    fun fixture_aVanishedArtifact_isCaught() {
        val found = problems(goodNotice, goodGraph.filterNot { it.name.endsWith("-external-protobuf") })

        assertTrue(found.toString(), found.any { it.contains("datastore-preferences-external-protobuf") && it.contains("not on the release classpath") })
    }

    @Test
    fun fixture_anEntryWithoutAHolderOrLicence_isCaught() {
        val found = problems("Guava\n  Maven groups: com.google.guava", listOf(library("com.google.guava:guava:1")))

        assertTrue(found.toString(), found.any { it.contains("no copyright holder") })
        assertTrue(found.toString(), found.any { it.contains("no licence") })
    }
}
