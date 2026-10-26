package org.aptura.player.data.transfer

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Schedules the one media transfer at a time as resumable WorkManager work.
 *
 * A workstation notification is deliberately absent: it would be a non-media notification, which would break the
 * media-session exemption from `POST_NOTIFICATIONS` (FR-PLAT-002). The in-app dialog is the progress and cancel
 * surface; WorkManager still keeps the job alive across rotation, navigation and process death.
 */
object MediaTransferScheduler {

    const val UNIQUE_WORK = "aptura_media_transfer"
    private const val TAG_PREFIX = "aptura_transfer_job_"

    sealed interface StartResult {
        data object Started : StartResult
        data object AlreadyRunning : StartResult
        data object Unfinished : StartResult
    }

    suspend fun start(context: Context, job: MediaTransferJob): StartResult {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val active = workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK).first()
            .any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
        if (active) return StartResult.AlreadyRunning
        // A journal that is not this job belongs to a transfer the user has not seen finish yet: usually a move
        // whose copies are complete and whose originals still need Android's deletion consent. Overwriting it would
        // strand those copies. The screen surfaces the existing job instead.
        val existing = MediaTransferJournal.read(context)
        if (existing != null && existing.id != job.id) return StartResult.Unfinished

        MediaTransferJournal.write(context, job)
        enqueue(context, job, ExistingWorkPolicy.REPLACE)
        return StartResult.Started
    }

    /**
     * Called when the UI discovers that a non-cancelled job has no active work (for example, WorkManager stopped a
     * run without a replacement). `KEEP` avoids creating a second chain if a retry is already queued.
     */
    fun resume(context: Context, jobId: String) {
        val job = MediaTransferJournal.read(context)?.takeIf { it.id == jobId } ?: return
        if (job.cancelled || job.copyFinished) return
        enqueue(context, job, ExistingWorkPolicy.KEEP)
    }

    fun cancel(context: Context, jobId: String) {
        var shouldCancelWork = false
        val updated = MediaTransferJournal.update(context) { current ->
            if (current.id == jobId && !current.copyFinished) {
                shouldCancelWork = true
                current.copy(cancelled = true)
            } else {
                current
            }
        } ?: return
        if (shouldCancelWork && updated.id == jobId && updated.cancelled) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK)
        }
    }

    fun observe(context: Context, jobId: String): Flow<List<WorkInfo>> =
        WorkManager.getInstance(context.applicationContext).getWorkInfosByTagFlow(tagFor(jobId))

    /** Prefer a live run, then a queued one, then the last terminal record for an honest failure message. */
    fun pickActive(infos: List<WorkInfo>): WorkInfo? =
        infos.lastOrNull { it.state == WorkInfo.State.RUNNING }
            ?: infos.lastOrNull { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
            ?: infos.lastOrNull()

    fun isActive(info: WorkInfo?): Boolean = when (info?.state) {
        WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> true
        else -> false
    }

    fun isTerminal(state: WorkInfo.State): Boolean = when (state) {
        WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> true
        else -> false
    }

    fun tagFor(jobId: String): String = TAG_PREFIX + jobId

    private fun enqueue(context: Context, job: MediaTransferJob, policy: ExistingWorkPolicy) {
        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<MediaTransferWorker>()
            .setInputData(workDataOf(MediaTransferWorker.KEY_JOB_ID to job.id))
            .addTag(UNIQUE_WORK)
            .addTag(tagFor(job.id))
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_WORK, policy, request)
    }
}
