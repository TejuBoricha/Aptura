package org.aptura.player.core.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * The FFmpeg audio extension's Java classes are vendored from AndroidX Media (app/src/main/java/androidx/media3/decoder/ffmpeg).
 * They are the half of the extension that is source code in this repository, so three things must stay true or the
 * extension quietly stops being what the project says it is:
 *
 *  - the files are byte-for-byte the upstream ones (the licence notice then needs no "modified" statement, and a hand edit is
 *    visible),
 *  - no other file is added, in particular not the software video renderer upstream ships beside them (ADR-010),
 *  - the Media3 version the app builds against is the tag they were copied from (the JNI glue and these classes are one unit).
 *
 * The record is VENDORED.md, next to the files.
 */
class VendoredMedia3Test {

    private fun repoFile(path: String): File {
        val file = listOf(path, "../$path").map(::File).firstOrNull { it.exists() }
        assertTrue("$path was not found from ${File(".").absolutePath}", file != null)
        return file!!
    }

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    /** What VENDORED.md says: the upstream tag and the checksum of each file. */
    private class Record(val tag: String?, val checksums: Map<String, String>)

    private fun parseRecord(text: String) = Record(
        tag = Regex("""Upstream tag: `([^`]+)`""").find(text)?.groupValues?.get(1),
        checksums = Regex("""^\| `([A-Za-z0-9-]+\.java)` \| `([0-9a-f]{64})` \|""", RegexOption.MULTILINE)
            .findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
    )

    /** Everything wrong between the record, the files on disk, the Media3 version in the catalog and the workflow. */
    private fun problems(record: Record, actual: Map<String, String>, media3Version: String?, workflow: String): List<String> = buildList {
        if (record.tag == null) add("VENDORED.md names no upstream tag")
        if (record.checksums.isEmpty()) add("VENDORED.md lists no files")
        (actual.keys - record.checksums.keys).sorted().forEach { add("$it is in the folder but not in VENDORED.md; vendoring a file is a decision") }
        (record.checksums.keys - actual.keys).sorted().forEach { add("$it is in VENDORED.md but not in the folder") }
        record.checksums.forEach { (name, expected) ->
            val found = actual[name]
            if (found != null && found != expected) add("$name differs from the upstream file recorded in VENDORED.md (is $found, expected $expected)")
        }
        if (record.tag != null && media3Version != record.tag) {
            add("media3 in gradle/libs.versions.toml is '$media3Version' but the vendored classes are from '${record.tag}'; re-vendor them (see VENDORED.md)")
        }
        if (record.tag != null && !workflow.contains("/androidx/media/${record.tag}/")) {
            add("the FFmpeg bundle workflow does not build the JNI glue from the same tag '${record.tag}'")
        }
    }

    // ---- the real thing --------------------------------------------------------------------------------------------

    private val folder get() = repoFile("app/src/main/java/androidx/media3/decoder/ffmpeg")

    private fun actualChecksums() = folder.listFiles().orEmpty().filter { it.name.endsWith(".java") }.associate { it.name to sha256(it) }

    private fun media3Version() = Regex("""^media3 = "([^"]+)"""", RegexOption.MULTILINE)
        .find(repoFile("gradle/libs.versions.toml").readText())?.groupValues?.get(1)

    @Test
    fun theVendoredClasses_areTheUpstreamFilesTheRecordNames() {
        val record = parseRecord(File(folder, "VENDORED.md").readText())

        val found = problems(record, actualChecksums(), media3Version(), repoFile(".github/workflows/ffmpeg-audio.yml").readText())

        assertTrue("The vendored FFmpeg classes do not match VENDORED.md:\n" + found.joinToString("\n") { " - $it" }, found.isEmpty())
    }

    @Test
    fun theRecord_isReadFromTheFile_notEmpty() {
        // Guards the guard: a reworded table that parsed to nothing would make the test above compare empty with empty.
        val record = parseRecord(File(folder, "VENDORED.md").readText())

        assertEquals("1.5.1", record.tag)
        assertEquals(5, record.checksums.size)
        assertTrue("the software video renderer must not be vendored", "ExperimentalFfmpegVideoRenderer.java" !in record.checksums)
    }

    // ---- fixtures: each rule must fire on the thing that was wrong ---------------------------------------------------

    private val tag = "1.5.1"
    private val good = Record(tag, mapOf("A.java" to "a".repeat(64), "B.java" to "b".repeat(64)))
    private val goodFiles = mapOf("A.java" to "a".repeat(64), "B.java" to "b".repeat(64))
    private val goodWorkflow = "JNI_URL: https://raw.githubusercontent.com/androidx/media/$tag/libraries/decoder_ffmpeg/x.cc"

    @Test
    fun fixture_aMatchingRecord_passes() {
        assertEquals(emptyList<String>(), problems(good, goodFiles, tag, goodWorkflow))
    }

    @Test
    fun fixture_aHandEditedFile_isCaught() {
        val edited = goodFiles + ("A.java" to "c".repeat(64))

        assertTrue(problems(good, edited, tag, goodWorkflow).single().contains("A.java differs"))
    }

    @Test
    fun fixture_anAddedFile_isCaught_forExampleTheVideoRenderer() {
        val added = goodFiles + ("ExperimentalFfmpegVideoRenderer.java" to "d".repeat(64))

        assertTrue(problems(good, added, tag, goodWorkflow).single().contains("ExperimentalFfmpegVideoRenderer.java is in the folder"))
    }

    @Test
    fun fixture_aMissingFile_isCaught() {
        assertTrue(problems(good, goodFiles - "B.java", tag, goodWorkflow).single().contains("B.java is in VENDORED.md but not in the folder"))
    }

    @Test
    fun fixture_aMedia3BumpWithoutRevendoring_isCaught() {
        val found = problems(good, goodFiles, "1.5.2", goodWorkflow)

        assertTrue(found.toString(), found.single().contains("re-vendor"))
    }

    @Test
    fun fixture_aWorkflowBuildingTheJniFromAnotherTag_isCaught() {
        val found = problems(good, goodFiles, tag, goodWorkflow.replace("media/$tag/", "media/1.6.0/"))

        assertTrue(found.toString(), found.single().contains("same tag"))
    }

    @Test
    fun fixture_anEmptyRecord_isCaught() {
        val found = problems(parseRecord("nothing useful here"), emptyMap(), tag, goodWorkflow)

        assertTrue(found.toString(), found.any { it.contains("names no upstream tag") } && found.any { it.contains("lists no files") })
    }
}
