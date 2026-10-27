package org.aptura.player.data.transfer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The journal is what makes a transfer survive process death: WorkManager can restore the request, but only the
 * journal says which sources are already at the destination and which destination document was half-written.
 * These tests pin the persistence contract itself, including the id-scoped clear that prevents a late result from
 * deleting a newer transfer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaTransferJournalTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearJournal() {
        MediaTransferJournal.clear(context)
    }

    @After
    fun clearAfter() {
        MediaTransferJournal.clear(context)
    }

    private fun job(
        id: String = "job-1",
        sources: List<String> = listOf("content://source/one", "content://source/two"),
        copied: List<String> = emptyList()
    ) = MediaTransferJob(
        id = id,
        sources = sources,
        destination = "content://tree/root",
        isMove = true,
        fromClipboard = true,
        copiedUris = copied,
        failedUris = listOf("content://source/three"),
        inProgressSource = "content://source/two",
        inProgressDestination = "content://tree/root/document/partial",
        copyFinished = false,
        cancelled = false,
        failure = MediaTransferFailure.PROVIDER,
        requiredBytes = 900L,
        availableBytes = 100L
    )

    @Test
    fun writeAndRead_preservesEveryField() {
        val original = job()
        MediaTransferJournal.write(context, original)

        assertEquals(original, MediaTransferJournal.read(context))
    }

    @Test
    fun update_changesOnlyTheStoredJob_andReturnsTheNewValue() {
        MediaTransferJournal.write(context, job(copied = listOf("content://source/one")))

        val updated = MediaTransferJournal.update(context) {
            it.copy(copyFinished = true, inProgressDestination = null)
        }

        assertTrue(updated!!.copyFinished)
        assertNull(updated.inProgressDestination)
        assertEquals(updated, MediaTransferJournal.read(context))
    }

    @Test
    fun remainingUris_areDistinctSourcesThatAreNotCopiedYet() {
        val original = job(
            sources = listOf("a", "b", "a", "c"),
            copied = listOf("a")
        )

        assertEquals(3, original.totalFiles)
        assertEquals(listOf("b", "c"), original.remainingUris)
    }

    @Test
    fun clearIf_leavesANewerJobAlone() {
        val original = job(id = "job-1")
        MediaTransferJournal.write(context, original)

        MediaTransferJournal.clearIf(context, "job-2")

        assertEquals(original, MediaTransferJournal.read(context))
    }

    @Test
    fun clearIf_removesTheMatchingJob() {
        MediaTransferJournal.write(context, job(id = "job-1"))

        MediaTransferJournal.clearIf(context, "job-1")

        assertNull(MediaTransferJournal.read(context))
    }

    @Test
    fun corruptJournal_readsAsAbsentInsteadOfCrashing() {
        context.getSharedPreferences("aptura_media_transfer_journal", Context.MODE_PRIVATE)
            .edit()
            .putString("job", "{not json")
            .commit()

        assertNull(MediaTransferJournal.read(context))

        val replacement = job(id = "job-3")
        MediaTransferJournal.write(context, replacement)
        assertEquals(replacement, MediaTransferJournal.read(context))
    }

    @Test
    fun updateWithoutAJournal_isANoOp() {
        assertNull(MediaTransferJournal.update(context) { it.copy(copyFinished = true) })
        assertNull(MediaTransferJournal.read(context))
    }
}
