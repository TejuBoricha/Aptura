package org.aptura.player.data.scanner

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Stands in for the scan itself, which would open the real database on threads that outlive the test. */
private class QuietWorkerFactory : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
        object : Worker(appContext, workerParameters) {
            override fun doWork(): Result = Result.success()
        }
}

/**
 * Access was granted and the library stayed empty until the app was closed and opened again: the first scan after a
 * grant can see nothing on some phones. A grant now scans at once and again a few seconds later, and the second one
 * must not make the library show a refresh spinner while it waits.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AccessGrantedScanTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).setWorkerFactory(QuietWorkerFactory()).build()
        )
    }

    @After
    fun tearDown() = WorkManagerTestInitHelper.closeWorkDatabase()

    @Test
    fun aGrant_scansAtOnce_andAgainLater() {
        MediaScanScheduler.scheduleScansAfterAccessGranted(context)

        val manager = WorkManager.getInstance(context)
        val now = manager.getWorkInfosForUniqueWork(MediaScanWorker.TAG_ONE_TIME_SCAN).get()
        assertEquals("a scan for now", 1, now.size)

        val later = manager.getWorkInfosForUniqueWork(MediaScanScheduler.UNIQUE_AFTER_ACCESS_SCAN).get()
        assertEquals("and one for later", 1, later.size)
        assertEquals("the later one is still waiting for its delay", WorkInfo.State.ENQUEUED, later.single().state)
    }

    @Test
    fun theWaitingScan_isNotAVisibleScan() {
        MediaScanScheduler.scheduleScansAfterAccessGranted(context)

        val later = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(MediaScanScheduler.UNIQUE_AFTER_ACCESS_SCAN).get().single()
        assertFalse(
            "the library's refresh spinner follows this tag; five quiet seconds must not look like scanning",
            MediaScanWorker.TAG_ONE_TIME_SCAN in later.tags
        )
        assertTrue(later.state == WorkInfo.State.ENQUEUED)
    }

    @Test
    fun grantingTwice_doesNotPileUpScans() {
        MediaScanScheduler.scheduleScansAfterAccessGranted(context)
        MediaScanScheduler.scheduleScansAfterAccessGranted(context)

        val later = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(MediaScanScheduler.UNIQUE_AFTER_ACCESS_SCAN).get()
        assertEquals(1, later.count { it.state == WorkInfo.State.ENQUEUED })
    }
}
