package org.aptura.player.data.transfer

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.runBlocking
import org.aptura.player.core.media.MediaTransferProgress
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Drives [MediaTransferWorker] directly, the way WorkManager does after restoring a request. The journal is the
 * only state carried into the new process, so these tests assert what a resumed run is allowed to touch: remove the
 * recorded partial document, skip every completed source, and never delete an original.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaTransferWorkerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var destinationFile: File
    private lateinit var provider: WorkerDestinationProvider

    class WorkerDestinationProvider : ContentProvider() {
        lateinit var outputFile: File
        lateinit var createdUri: Uri
        var availableBytes: Long? = null
        var createCount = 0
        val deletedUris = mutableListOf<Uri>()

        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            if (uri == DocumentsContract.buildRootsUri(AUTHORITY)) {
                return MatrixCursor(
                    arrayOf(
                        DocumentsContract.Root.COLUMN_ROOT_ID,
                        DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
                    )
                ).apply {
                    addRow(arrayOf<Any?>("root", availableBytes))
                }
            }
            return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply {
                addRow(arrayOf(uri.lastPathSegment.orEmpty()))
            }
        }

        override fun getType(uri: Uri) = "video/mp4"
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor = ParcelFileDescriptor.open(
            outputFile,
            ParcelFileDescriptor.MODE_WRITE_ONLY or
                ParcelFileDescriptor.MODE_CREATE or
                ParcelFileDescriptor.MODE_TRUNCATE
        )

        override fun call(method: String, arg: String?, extras: Bundle?): Bundle? = when (method) {
            "android:createDocument" -> {
                createCount++
                Bundle().apply { putParcelable("uri", createdUri) }
            }

            "android:deleteDocument" -> {
                extras?.getParcelable("uri", Uri::class.java)?.let { deletedUris += it }
                outputFile.delete()
                Bundle.EMPTY
            }

            else -> super.call(method, arg, extras)
        }

        companion object {
            const val AUTHORITY = "org.aptura.test.transfer"
        }
    }

    @Before
    fun setUp() {
        destinationFile = temporaryFolder.newFile("destination.mp4").also { it.writeBytes(byteArrayOf()) }
        provider = Robolectric.setupContentProvider(
            WorkerDestinationProvider::class.java,
            WorkerDestinationProvider.AUTHORITY
        )
        provider.outputFile = destinationFile
        provider.createdUri = Uri.parse("content://${WorkerDestinationProvider.AUTHORITY}/document/destination.mp4")
        MediaTransferJournal.clear(context)
    }

    @After
    fun tearDown() {
        MediaTransferJournal.clear(context)
    }

    private fun job(
        sources: List<String>,
        copied: List<String> = emptyList(),
        partial: String? = null,
        cancelled: Boolean = false,
        copyFinished: Boolean = false
    ) = MediaTransferJob(
        id = "job-1",
        sources = sources,
        destination = "content://${WorkerDestinationProvider.AUTHORITY}/tree/root",
        isMove = true,
        fromClipboard = false,
        copiedUris = copied,
        inProgressSource = sources.firstOrNull(),
        inProgressDestination = partial,
        copyFinished = copyFinished,
        cancelled = cancelled
    )

    private suspend fun run(job: MediaTransferJob): ListenableWorker.Result {
        MediaTransferJournal.write(context, job)
        return TestListenableWorkerBuilder<MediaTransferWorker>(context)
            .setInputData(workDataOf(MediaTransferWorker.KEY_JOB_ID to job.id))
            .build()
            .doWork()
    }

    @Test
    fun resume_removesThePartialDocument_andCopiesOnlyRemainingSources() = runBlocking {
        val first = temporaryFolder.newFile("first.mp4").also { it.writeBytes(byteArrayOf(1, 2, 3)) }
        val second = temporaryFolder.newFile("second.mp4").also { it.writeBytes(byteArrayOf(4, 5, 6, 7)) }
        val firstUri = Uri.fromFile(first).toString()
        val secondUri = Uri.fromFile(second).toString()
        val partial = "content://${WorkerDestinationProvider.AUTHORITY}/document/partial.mp4"
        val original = job(sources = listOf(firstUri, secondUri), copied = listOf(firstUri), partial = partial)

        val result = run(original)

        assertTrue("a completed transfer must report success", result is ListenableWorker.Result.Success)
        assertEquals("the half-written document from the previous process must be removed", listOf(Uri.parse(partial)), provider.deletedUris)
        assertArrayEquals("only the remaining source may be written", second.readBytes(), destinationFile.readBytes())
        assertEquals("the completed source must not be created a second time", 1, provider.createCount)
        assertTrue("the original is never touched by a move", first.exists() && second.exists())

        val journal = MediaTransferJournal.read(context)!!
        assertEquals(listOf(firstUri, secondUri), journal.copiedUris)
        assertTrue(journal.failedUris.isEmpty())
        assertTrue(journal.copyFinished)
        assertNull(journal.inProgressSource)
        assertNull(journal.inProgressDestination)
    }

    @Test
    fun resume_afterTheCopyPhase_doesNotWriteAgain() = runBlocking {
        val first = temporaryFolder.newFile("done.mp4").also { it.writeBytes(byteArrayOf(9, 9)) }
        val original = job(sources = listOf(Uri.fromFile(first).toString()), copyFinished = true)

        val result = run(original)

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(0, provider.createCount)
        assertTrue(provider.deletedUris.isEmpty())
        assertTrue(MediaTransferJournal.read(context)!!.copyFinished)
    }

    @Test
    fun noSpaceAtTheDestination_failsBeforeCreatingADocumentOrDeletingAnything() = runBlocking {
        val source = temporaryFolder.newFile("too-big.mp4").also { it.writeBytes(ByteArray(10)) }
        provider.availableBytes = 3L
        val original = job(sources = listOf(Uri.fromFile(source).toString()))

        val result = run(original)

        assertTrue(result is ListenableWorker.Result.Failure)
        assertEquals(0, provider.createCount)
        assertTrue(provider.deletedUris.isEmpty())
        assertTrue("the source must be intact", source.readBytes().size == 10)

        val journal = MediaTransferJournal.read(context)!!
        assertEquals(MediaTransferFailure.NOT_ENOUGH_SPACE, journal.failure)
        assertTrue(journal.copyFinished)
        assertEquals(10L, journal.requiredBytes)
        assertEquals(3L, journal.availableBytes)
    }

    @Test
    fun cancelledJournal_stopsWithoutTouchingTheDestinationOrASource() = runBlocking {
        val source = temporaryFolder.newFile("cancelled.mp4").also { it.writeBytes(byteArrayOf(1)) }
        val original = job(sources = listOf(Uri.fromFile(source).toString()), cancelled = true)

        val result = run(original)

        assertTrue(result is ListenableWorker.Result.Failure)
        assertEquals(0, provider.createCount)
        assertTrue(provider.deletedUris.isEmpty())
        assertTrue(MediaTransferJournal.read(context)!!.cancelled)
    }

    @Test
    fun progress_survivesTheWorkDataRoundTrip() {
        val progress = MediaTransferProgress(
            completedFiles = 2,
            totalFiles = 5,
            copiedBytes = 1_024L,
            totalBytes = 4_096L,
            currentName = "clip.mp4"
        )

        assertEquals(progress, MediaTransferWorker.progressFrom(progress.toWorkData()))
        assertNull(MediaTransferWorker.progressFrom(workDataOf()))
    }
}
