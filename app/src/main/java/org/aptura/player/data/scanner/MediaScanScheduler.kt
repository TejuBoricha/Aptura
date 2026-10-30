package org.aptura.player.data.scanner

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * Orchestrator for scheduling MediaScanWorker tasks via WorkManager.
 * Enforces battery-not-low constraints to avoid draining user battery (NFR-RES-007).
 */
object MediaScanScheduler {

    /** Unique-work name for the scan that runs when the process starts (audit P0-09). */
    const val UNIQUE_STARTUP_SCAN = "aptura_media_scan_startup"

    /**
     * Everything that must happen when the app process starts: a differential scan so new videos show up
     * without the user pressing Refresh, and the periodic background scan (which previously was never scheduled).
     */
    fun scheduleOnProcessStart(context: Context) {
        val startupScan = OneTimeWorkRequestBuilder<MediaScanWorker>()
            .addTag(MediaScanWorker.TAG_ONE_TIME_SCAN)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_STARTUP_SCAN,
            ExistingWorkPolicy.KEEP,
            startupScan
        )
        schedulePeriodicScan(context)
    }

    /** True while a one-time scan is queued or running; drives the Refresh spinner (audit P0-09). */
    fun isScanning(context: Context): Flow<Boolean> =
        WorkManager.getInstance(context)
            .getWorkInfosByTagFlow(MediaScanWorker.TAG_ONE_TIME_SCAN)
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }

    /**
     * Enqueues an immediate one-time scan of local media (e.g. on app launch or pull-to-refresh).
     */
    fun scheduleImmediateScan(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<MediaScanWorker>()
            .addTag(MediaScanWorker.TAG_ONE_TIME_SCAN)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            MediaScanWorker.TAG_ONE_TIME_SCAN,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /** Unique-work name for the scan that follows a fresh grant of access. */
    const val UNIQUE_AFTER_ACCESS_SCAN = "aptura_media_scan_after_access"

    /** A second look this long after access was granted; see [scheduleScansAfterAccessGranted]. */
    const val AFTER_ACCESS_DELAY_SECONDS = 5L

    /**
     * Access to the videos was just granted. Scan now, and once more a few seconds later: on some phones the first scan
     * after a grant sees nothing (the system's media index is still catching up, or still answering as if access were
     * not there), and the person was left with an empty library until the app was closed and opened again. The second
     * scan is a cheap comparison when the first one found everything. It is not tagged as a visible scan, so the
     * library does not show a refresh spinner for the five seconds it waits.
     */
    fun scheduleScansAfterAccessGranted(context: Context) {
        scheduleImmediateScan(context)
        val followUp = OneTimeWorkRequestBuilder<MediaScanWorker>()
            .setInitialDelay(AFTER_ACCESS_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_AFTER_ACCESS_SCAN,
            ExistingWorkPolicy.REPLACE,
            followUp
        )
    }

    /**
     * Schedules a periodic background scan (every 6 hours) with battery-not-low constraint.
     */
    fun schedulePeriodicScan(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<MediaScanWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(MediaScanWorker.TAG_PERIODIC_SCAN)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MediaScanWorker.TAG_PERIODIC_SCAN,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }
}
