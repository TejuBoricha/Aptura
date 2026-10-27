package org.aptura.player.ui.library

import org.aptura.player.data.transfer.MediaTransferJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A move has two independent outcomes: the copy and Android's deletion decision. The dialog must never hide a
 * successful copy just because deletion was denied, or the user will retry the move and duplicate every file.
 */
class MediaTransferResultMessagesTest {

    private fun uri(index: Int) = "content://source/$index"

    private fun job(total: Int, copied: Int, failed: Int = 0) = MediaTransferJob(
        id = "job",
        sources = (1..total).map(::uri),
        destination = "content://tree/root",
        isMove = true,
        fromClipboard = false,
        copiedUris = (1..copied).map(::uri),
        failedUris = ((copied + 1)..(copied + failed)).map(::uri)
    )

    private fun titles(vararg indexes: Int) = indexes.associate { uri(it) to "Video $it" }

    @Test
    fun everyOriginalDeleted_reportsACompleteMove() {
        val result = moveResultMessage(
            job = job(total = 2, copied = 2),
            deletedUris = listOf(uri(1), uri(2)),
            notDeletedUris = emptyList(),
            titlesByUri = titles(1, 2)
        )

        assertEquals("Move complete", result.title)
        assertTrue(result.message.contains("2 of 2 videos moved"))
        assertTrue(result.message.contains("originals were removed"))
    }

    @Test
    fun deletionDenied_saysTheCopiesExistInsteadOfOnlyMoveNotCompleted() {
        val result = moveResultMessage(
            job = job(total = 2, copied = 2),
            deletedUris = emptyList(),
            notDeletedUris = listOf(uri(1), uri(2)),
            titlesByUri = titles(1, 2)
        )

        assertEquals("Copied, originals kept", result.title)
        assertTrue(result.message.contains("2 of 2 videos copied to the selected folder"))
        assertTrue("the user must be told the copies exist", result.message.contains("both places"))
        assertTrue(result.message.contains("Video 1"))
        assertTrue(result.message.contains("Delete the originals from the library after checking the copies"))
        assertFalse("a denied deletion must not be described as if nothing happened", result.message.contains("Move not completed"))
    }

    @Test
    fun partialDeletion_reportsCopiedDeletedAndKeptSeparately() {
        val result = moveResultMessage(
            job = job(total = 3, copied = 3),
            deletedUris = listOf(uri(1)),
            notDeletedUris = listOf(uri(2), uri(3)),
            titlesByUri = titles(1, 2, 3)
        )

        assertEquals("Move partially complete", result.title)
        assertTrue(result.message.contains("3 of 3 videos copied"))
        assertTrue(result.message.contains("1 original deleted"))
        assertTrue(result.message.contains("2 originals"))
        assertTrue(result.message.contains("Video 2") && result.message.contains("Video 3"))
    }

    @Test
    fun failedCopies_areNamedAsStillInTheLibrary() {
        val result = moveResultMessage(
            job = job(total = 3, copied = 2, failed = 1),
            deletedUris = listOf(uri(1), uri(2)),
            notDeletedUris = emptyList(),
            titlesByUri = titles(1, 2, 3)
        )

        assertEquals("Move partially complete", result.title)
        assertTrue(result.message.contains("2 of 3 videos copied"))
        assertTrue(result.message.contains("Not copied (originals kept)"))
        assertTrue(result.message.contains("Video 3"))
    }

    @Test
    fun longFailureLists_areBounded() {
        val uris = (1..5).map(::uri)
        val names = describeVideoUris(uris, (1..5).associate { uri(it) to "Video $it" })

        assertEquals("Video 1, Video 2, Video 3, and 2 more", names)
    }
}
