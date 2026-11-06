package org.aptura.player.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit PRIV-01: the diagnostic report promised that file names were removed, and the old pattern stopped at the first
 * space or comma, so `/storage/emulated/0/Movies/My Holiday/secret clip.mp4` came out as `[R] Holiday/secret clip.mp4`.
 * Content URIs, `/mnt/media_rw/` and `/data/media/0/` were not covered at all. The tests below use the awkward names
 * real people have.
 */
class RedactionAdversarialTest {

    private fun assertNoneOf(redacted: String, vararg leaks: String) {
        for (leak in leaks) assertFalse("'$leak' leaked into: $redacted", redacted.contains(leak, ignoreCase = true))
    }

    @Test
    fun aPathWithSpacesLeavesNothingBehind() {
        val out = RedactionEngine.redactText("/storage/emulated/0/Movies/My Holiday/secret clip.mp4")
        assertNoneOf(out, "Holiday", "secret", "clip", "Movies", "/storage")
        assertEquals("[FILE_REDACTED].mp4", out)
    }

    @Test
    fun aPathWithACommaLeavesNothingBehind() {
        val out = RedactionEngine.redactText("/storage/ABCD-1234/Priya, wedding.mkv")
        assertNoneOf(out, "Priya", "wedding", "ABCD-1234")
    }

    @Test
    fun aPathInsideASentenceIsRemovedWithTheRestOfTheLine() {
        val out = RedactionEngine.redactText("Failed to open /storage/emulated/0/My Videos/a b.mp4 because access was denied")
        assertNoneOf(out, "My Videos", "a b")
        assertTrue(out.startsWith("Failed to open "))
    }

    @Test
    fun unicodeAndPunctuationInNamesAreRemoved() {
        val out = RedactionEngine.redactText("/storage/emulated/0/Movies/日本語 ü [1080p] (2026).mkv")
        assertNoneOf(out, "日本語", "ü", "1080p", "2026")
    }

    @Test
    fun contentUrisAreRemovedWhetherEncodedOrNot() {
        val encoded = "content://com.android.externalstorage.documents/document/primary%3AMovies%2FPrivate%2Fa.mp4"
        val plain = "content://com.android.externalstorage.documents/document/primary:Movies/Private/a.mp4"
        for (raw in listOf(encoded, plain)) {
            val out = RedactionEngine.redactText("Permission denial: reading $raw")
            assertNoneOf(out, "Private", "Movies", "a.mp4", "primary")
            assertTrue(out.contains("Permission denial"))
        }
    }

    @Test
    fun fileUrisAreRemoved() {
        val out = RedactionEngine.redactText("file:///storage/emulated/0/DCIM/Family/kid.mp4 failed")
        assertNoneOf(out, "DCIM", "Family", "kid")
    }

    @Test
    fun otherStorageRootsAreCovered() {
        val roots = listOf(
            "/mnt/media_rw/ABCD-1234/Videos/x.mp4",
            "/data/media/0/Movies/x.mp4",
            "/sdcard/Movies/x.mp4",
            "/mnt/sdcard/Movies/x.mp4",
            "/data/user/0/org.aptura.player/files/x.mp4",
            "/STORAGE/EMULATED/0/Movies/X.MP4"
        )
        for (raw in roots) {
            val out = RedactionEngine.redactText(raw)
            assertNoneOf(out, "Videos", "Movies", "ABCD-1234", "org.aptura", "/data", "/mnt", "/sdcard", "/storage")
        }
    }

    @Test
    fun textWithoutAPathIsLeftAlone() {
        assertEquals("Decoder init failed", RedactionEngine.redactText("Decoder init failed"))
        assertEquals("aspect 16/9 at 1920/1080", RedactionEngine.redactText("aspect 16/9 at 1920/1080"))
        assertEquals("see https://example.com/data/x", RedactionEngine.redactText("see https://example.com/data/x"))
        assertEquals("ERR_PARSER_CORRUPT stage=decode", RedactionEngine.redactText("ERR_PARSER_CORRUPT stage=decode"))
    }

    /** Review finding 1: a URI with an unencoded space used to leave the rest of the name behind. */
    @Test
    fun urisWithSpacesLeaveNothingBehind() {
        val uris = listOf(
            "file:///storage/emulated/0/Movies/My Holiday/secret clip.mp4",
            "content://com.android.externalstorage.documents/document/primary:Movies/My Holiday/secret clip.mp4",
            "content://com.android.providers.downloads.documents/document/raw:/storage/emulated/0/Download/Priya wedding.mp4",
            "Unable to open content://media/external/file/My Holiday.mp4"
        )
        for (raw in uris) {
            val out = RedactionEngine.redactText(raw)
            assertNoneOf(out, "Holiday", "secret", "Priya", "wedding", "clip", "Movies")
        }
    }

    /** Review finding 2: roots Android really emits. */
    @Test
    fun theRootsAndroidActuallyEmitsAreCovered() {
        val roots = listOf(
            "/mnt/user/0/emulated/0/Movies/Secret Holiday.mp4",
            "/sdcard0/Movies/Secret Holiday.mp4",
            "/mnt/runtime/default/emulated/0/Secret.mp4",
            "/mnt/expand/0a1b2c3d/media/0/Secret.mp4",
            "/data/local/tmp/Secret Holiday.mp4",
            "/storage/self/primary/Movies/Secret Holiday.mp4"
        )
        for (raw in roots) {
            val out = RedactionEngine.redactText("I/O error on $raw")
            assertNoneOf(out, "Secret", "Holiday", "/mnt", "/sdcard", "/data", "/storage")
            assertTrue(out.startsWith("I/O error on "))
        }
    }

    @Test
    fun anExtensionIsKeptOnlyWhenItIsAKnownOne() {
        // The old code took everything after the last dot of the whole rest of the line.
        val trailing = RedactionEngine.redactText("/storage/emulated/0/a.mp4 owner priya.kumar")
        assertNoneOf(trailing, "priya", "kumar", "owner")
        val nameWithDot = RedactionEngine.redactText("/storage/emulated/0/Movies/Trip.Priya")
        assertNoneOf(nameWithDot, "Priya", "Trip")
        assertEquals("[FILE_REDACTED].mkv", RedactionEngine.redactText("/storage/emulated/0/Movies/Show S01E02.MKV"))
        assertEquals(
            "[FILE_REDACTED]",
            RedactionEngine.redactPayload(mapOf("fileName" to "Priya.Kumar"), includeFileName = false)["fileName"]
        )
    }

    @Test
    fun percentEncodedPathsAndDocumentIdsAreRemoved() {
        val encoded = RedactionEngine.redactText("opening %2Fstorage%2Femulated%2F0%2FMovies%2FSecret%20Holiday.mp4")
        assertNoneOf(encoded, "Secret", "Holiday", "Movies")
        for (id in listOf("primary:Movies/Private/Secret Holiday.mp4", "1A2B-3C4D:Movies/Secret Holiday.mp4", "raw:/storage/emulated/0/x y.mp4")) {
            assertNoneOf(RedactionEngine.redactText("document $id"), "Secret", "Private", "Movies", "storage")
        }
    }

    @Test
    fun aHugeValueIsHandledQuickly() {
        val start = System.nanoTime()
        val out = RedactionEngine.redactText("a".repeat(50_000))
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("took $ms ms", ms < 1_000)
        assertTrue(out.length < 5_000)
    }

    @Test
    fun theOptInKeepsOnlyTheFileNameNeverTheDirectories() {
        val out = RedactionEngine.redactText("/storage/emulated/0/Movies/My Holiday/clip.mp4", includeFileName = true)
        assertNoneOf(out, "Holiday", "/storage", "Movies")
        assertTrue(out.endsWith("clip.mp4"))
    }

    @Test
    fun namePayloadKeysAreAlwaysRemoved() {
        val out = RedactionEngine.redactPayload(
            mapOf(
                "oldTitle" to "Family Holiday 2025",
                "newTitle" to "Secret Project",
                "title" to "Anything",
                "displayName" to "x.mp4",
                "sizeBytes" to "123456"
            ),
            includeFileName = true
        )
        assertNoneOf(out.values.joinToString(" "), "Family", "Secret", "Anything", "x.mp4")
        assertEquals("123456", out["sizeBytes"])
    }

    @Test
    fun aBareFileNameKeepsOnlyItsExtensionUnlessTheUserOptsIn() {
        val closed = RedactionEngine.redactPayload(mapOf("fileName" to "birthday party.mkv"), includeFileName = false)
        assertEquals("[FILE_REDACTED].mkv", closed["fileName"])
        val noExt = RedactionEngine.redactPayload(mapOf("fileName" to "birthday"), includeFileName = false)
        assertEquals("[FILE_REDACTED]", noExt["fileName"])
        val open = RedactionEngine.redactPayload(mapOf("fileName" to "birthday party.mkv"), includeFileName = true)
        assertEquals("birthday party.mkv", open["fileName"])
    }
}
