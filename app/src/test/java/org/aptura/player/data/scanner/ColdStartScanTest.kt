package org.aptura.player.data.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.aptura.player.ApturaApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Stands in for [MediaScanWorker] so the scan does not actually run (it would open the real database on background
 * threads that outlive the test). What the app ENQUEUES at process start is the behaviour under test; what the worker
 * does once it runs is covered by the scanner tests.
 */
private class DoesNotScanWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker = object : Worker(appContext, workerParameters) {
        override fun doWork(): Result = Result.success()
    }
}

/**
 * The REAL [ApturaApplication] — its real `onCreate()` — with WorkManager swapped for the documented test
 * initialisation (in-memory database, work runs on the calling thread) so nothing outlives the test.
 */
class ColdStartApplication : ApturaApplication() {
    override fun onCreate() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            this,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(DoesNotScanWorkerFactory())
                .build()
        )
        super.onCreate()
    }
}

/**
 * Regression for audit P0-09: nothing ever scanned on app start (and the periodic worker had zero callers), so new
 * videos only appeared after the user pressed Refresh. Robolectric creates the application and runs its `onCreate()`
 * before each test, so these assertions observe what process start actually enqueues in WorkManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = ColdStartApplication::class)
class ColdStartScanTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun processStart_enqueuesAStartupScan() {
        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(MediaScanScheduler.UNIQUE_STARTUP_SCAN).get()
        assertFalse("no startup scan was scheduled when the process started", infos.isEmpty())
    }

    @Test
    fun processStart_registersThePeriodicScan() {
        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(MediaScanWorker.TAG_PERIODIC_SCAN).get()
        assertFalse("periodic scan was never registered", infos.isEmpty())
        assertTrue(infos.none { it.state == WorkInfo.State.CANCELLED })
    }

    @Test
    fun startupScan_doesNotReplaceAScanThatIsStillInFlight() {
        // A scan from before (queued, or running when the process was killed) must survive the next process start.
        val workManager = WorkManager.getInstance(context)
        val inFlight = OneTimeWorkRequestBuilder<MediaScanWorker>().setInitialDelay(1, TimeUnit.DAYS).build()
        workManager.enqueueUniqueWork(MediaScanScheduler.UNIQUE_STARTUP_SCAN, ExistingWorkPolicy.REPLACE, inFlight)
            .result.get()

        MediaScanScheduler.scheduleOnProcessStart(context)

        val ids = workManager.getWorkInfosForUniqueWork(MediaScanScheduler.UNIQUE_STARTUP_SCAN).get().map { it.id }
        assertEquals("a second process start replaced the scan that was already queued", listOf(inFlight.id), ids)
    }
}
