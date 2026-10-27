package org.aptura.player.data.transfer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.Worker
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Scheduling is a one-transfer-at-a-time contract. The important failure here is not a crash: it is overwriting an
 * unfinished journal, which can strand a completed move before its originals are deleted, or double-cancel a newer
 * transfer. These tests use an in-memory WorkManager with no-op work.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaTransferSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var workManager: WorkManager

    private class NoOpWorkerFactory : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker = object : Worker(appContext, workerParameters) {
            override fun doWork(): Result = Result.success()
        }
    }

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(NoOpWorkerFactory())
                .build()
        )
        workManager = WorkManager.getInstance(context)
        MediaTransferJournal.clear(context)
    }

    @After
    fun tearDown() {
        MediaTransferJournal.clear(context)
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    private fun job(id: String) = MediaTransferJob(
        id = id,
        sources = listOf("content://source/one"),
        destination = "content://tree/root",
        isMove = true,
        fromClipboard = false
    )

    @Test
    fun start_doesNotReplaceAnUnfinishedTransfer() = runBlocking {
        val unfinished = job("older")
        MediaTransferJournal.write(context, unfinished)

        val result = MediaTransferScheduler.start(context, job("newer"))

        assertEquals(MediaTransferScheduler.StartResult.Unfinished, result)
        assertEquals("the unfinished journal still owns the result and the move consent", unfinished, MediaTransferJournal.read(context))
        assertTrue(
            workManager.getWorkInfosByTag(MediaTransferScheduler.tagFor("newer")).get().isEmpty()
        )
    }

    @Test
    fun start_writesTheJournalBeforeEnqueuingTheUniqueWork() = runBlocking {
        val fresh = job("fresh")

        val result = MediaTransferScheduler.start(context, fresh)

        assertEquals(MediaTransferScheduler.StartResult.Started, result)
        assertEquals(fresh, MediaTransferJournal.read(context))
        assertTrue(workManager.getWorkInfosForUniqueWork(MediaTransferScheduler.UNIQUE_WORK).get().isNotEmpty())
    }

    @Test
    fun start_refusesWhileARequestIsAlreadyQueued() = runBlocking {
        val inFlight = OneTimeWorkRequestBuilder<MediaTransferWorker>()
            .setInitialDelay(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniqueWork(MediaTransferScheduler.UNIQUE_WORK, ExistingWorkPolicy.REPLACE, inFlight)
            .result.get()

        val result = MediaTransferScheduler.start(context, job("new"))

        assertEquals(MediaTransferScheduler.StartResult.AlreadyRunning, result)
        assertNull("a refused start must not write its journal", MediaTransferJournal.read(context))
    }

    @Test
    fun cancel_onlyAffectsTheJobItNames() {
        MediaTransferJournal.write(context, job("job-1"))

        MediaTransferScheduler.cancel(context, "job-2")

        assertFalse("cancelling another id must not mark this job", MediaTransferJournal.read(context)!!.cancelled)

        MediaTransferScheduler.cancel(context, "job-1")

        assertNotNull(MediaTransferJournal.read(context))
        assertTrue(MediaTransferJournal.read(context)!!.cancelled)
    }

    @Test
    fun resumePending_restoresAnUnfinishedJobAfterProcessStart() {
        MediaTransferJournal.write(context, job("pending"))

        MediaTransferScheduler.resumePending(context)

        assertTrue(
            "a journaled copy that no screen saw must be restored at process start",
            workManager.getWorkInfosByTag(MediaTransferScheduler.tagFor("pending")).get().isNotEmpty()
        )
    }

    @Test
    fun resumePending_leavesCancelledAndFinishedJobsAlone() {
        MediaTransferJournal.write(context, job("cancelled").copy(cancelled = true))
        MediaTransferScheduler.resumePending(context)
        assertTrue(workManager.getWorkInfosByTag(MediaTransferScheduler.tagFor("cancelled")).get().isEmpty())

        MediaTransferJournal.write(context, job("finished").copy(copyFinished = true))
        MediaTransferScheduler.resumePending(context)
        assertTrue(workManager.getWorkInfosByTag(MediaTransferScheduler.tagFor("finished")).get().isEmpty())
    }
}
