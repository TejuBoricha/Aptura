package org.aptura.player.data.scanner

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Orchestrator for scheduling MediaScanWorker tasks via WorkManager.
 * Enforces battery-not-low constraints to avoid draining user battery (NFR-RES-007).
 */
object MediaScanScheduler {

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
