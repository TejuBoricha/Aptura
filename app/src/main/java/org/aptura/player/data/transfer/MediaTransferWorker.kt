package org.aptura.player.data.transfer

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aptura.player.core.media.MediaFileTransfer
import org.aptura.player.core.media.MediaTransferProgress

/**
 * Copies a batch into a SAF tree, resuming from [MediaTransferJournal] after WorkManager restarts the process or
 * preempts a long run. It intentionally does not post a notification: R1 posts no non-media notifications, because
 * that would break the media-session `POST_NOTIFICATIONS` exemption (FR-PLAT-002). Progress and Cancel live in the
 * library's transfer dialog; the journal keeps the job safe while that dialog is gone.
 */
class MediaTransferWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val initial = withContext(Dispatchers.IO) { MediaTransferJournal.read(applicationContext) }
            ?.takeIf { it.id == jobId }
            ?: return Result.failure()
        if (initial.cancelled) return Result.failure()
        if (initial.copyFinished) return Result.success()

        return try {
            removePartialFromPreviousRun(initial)
            val remaining = initial.remainingUris
            if (remaining.isEmpty()) {
                complete(jobId) { it.copy(copyFinished = true) }
                return Result.success()
            }

            // Ask the destination before writing a byte. A provider that reports no quota is allowed to try.
            val space = withContext(Dispatchers.IO) {
                MediaFileTransfer.measureSpace(
                    applicationContext,
                    remaining,
                    Uri.parse(initial.destination)
                )
            }
            if (!space.hasEnoughSpace) {
                MediaTransferJournal.update(applicationContext) { current ->
                    if (current.id != jobId) current else current.copy(
                        copyFinished = true,
                        failure = MediaTransferFailure.NOT_ENOUGH_SPACE,
                        requiredBytes = space.requiredBytes,
                        availableBytes = space.availableBytes,
                        inProgressSource = null,
                        inProgressDestination = null
                    )
                }
                return Result.failure()
            }

            val result = MediaFileTransfer.copyIntoTree(
                context = applicationContext,
                sourceUris = initial.sources,
                treeUri = Uri.parse(initial.destination),
                skipCopied = initial.copiedUris.toSet(),
                onDocumentCreated = { source, destination ->
                    update(jobId) {
                        it.copy(
                            inProgressSource = source,
                            inProgressDestination = destination.toString()
                        )
                    }
                },
                onSourceCopied = { source, _ ->
                    update(jobId) {
                        it.copy(
                            copiedUris = (it.copiedUris + source).distinct(),
                            inProgressSource = null,
                            inProgressDestination = null
                        )
                    }
                },
                onProgress = { progress -> setProgress(progress.toWorkData()) }
            )

            MediaTransferJournal.update(applicationContext) { current ->
                if (current.id != jobId) current else current.copy(
                    copiedUris = result.copiedUris,
                    failedUris = result.failedUris,
                    copyFinished = true,
                    inProgressSource = null,
                    inProgressDestination = null,
                    failure = null
                )
            }
            Result.success()
        } catch (cancel: CancellationException) {
            // copyIntoTree has already deleted its partial destination. Leave the completed files and the journal
            // intact so the next run can resume instead of starting over.
            MediaTransferJournal.update(applicationContext) { current ->
                if (current.id != jobId) current else current.copy(
                    inProgressSource = null,
                    inProgressDestination = null
                )
            }
            throw cancel
        } catch (error: Exception) {
            MediaTransferJournal.update(applicationContext) { current ->
                if (current.id != jobId) current else current.copy(
                    copyFinished = true,
                    failure = if (current.failure == MediaTransferFailure.NOT_ENOUGH_SPACE) {
                        current.failure
                    } else {
                        MediaTransferFailure.PROVIDER
                    },
                    inProgressSource = null,
                    inProgressDestination = null
                )
            }
            Result.failure(workDataOf(KEY_ERROR to (error.message ?: "Transfer failed")))
        }
    }

    private suspend fun removePartialFromPreviousRun(job: MediaTransferJob) {
        val partial = job.inProgressDestination ?: return
        withContext(Dispatchers.IO) {
            MediaFileTransfer.deletePartial(applicationContext, Uri.parse(partial))
        }
        update(job.id) {
            it.copy(inProgressSource = null, inProgressDestination = null)
        }
    }

    private suspend fun update(jobId: String, transform: (MediaTransferJob) -> MediaTransferJob) {
        MediaTransferJournal.update(applicationContext) { current ->
            if (current.id == jobId) transform(current) else current
        }
    }

    private suspend fun complete(jobId: String, transform: (MediaTransferJob) -> MediaTransferJob) {
        update(jobId, transform)
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_ERROR = "error"
        const val KEY_COMPLETED_FILES = "completed_files"
        const val KEY_TOTAL_FILES = "total_files"
        const val KEY_COPIED_BYTES = "copied_bytes"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_CURRENT_NAME = "current_name"

        fun progressFrom(data: Data): MediaTransferProgress? {
            val total = data.getInt(KEY_TOTAL_FILES, 0)
            if (total <= 0) return null
            return MediaTransferProgress(
                completedFiles = data.getInt(KEY_COMPLETED_FILES, 0),
                totalFiles = total,
                copiedBytes = data.getLong(KEY_COPIED_BYTES, 0L),
                totalBytes = data.getLong(KEY_TOTAL_BYTES, 0L),
                currentName = data.getString(KEY_CURRENT_NAME).orEmpty()
            )
        }
    }
}

internal fun MediaTransferProgress.toWorkData(): Data = workDataOf(
    MediaTransferWorker.KEY_COMPLETED_FILES to completedFiles,
    MediaTransferWorker.KEY_TOTAL_FILES to totalFiles,
    MediaTransferWorker.KEY_COPIED_BYTES to copiedBytes,
    MediaTransferWorker.KEY_TOTAL_BYTES to totalBytes,
    MediaTransferWorker.KEY_CURRENT_NAME to currentName
)
