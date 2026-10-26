package org.aptura.player.data.transfer

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Why a transfer stopped before the copy phase completed. Per-file failures live in [MediaTransferJob.failedUris]. */
@Serializable
enum class MediaTransferFailure {
    NOT_ENOUGH_SPACE,
    PROVIDER,
    UNKNOWN
}

/**
 * A transfer journal, persisted before the first byte is written. It is the single source of truth for a transfer
 * that outlives the library screen: WorkManager can restart the worker after process death, and the next run resumes
 * from [copiedUris] instead of writing the same videos again.
 *
 * [id] is the transfer's identity and stays stable across WorkManager retries; it is deliberately not the WorkRequest
 * UUID, because a resumed run is a new request.
 */
@Serializable
data class MediaTransferJob(
    val id: String,
    val sources: List<String>,
    val destination: String,
    val isMove: Boolean,
    val fromClipboard: Boolean,
    val copiedUris: List<String> = emptyList(),
    val failedUris: List<String> = emptyList(),
    val inProgressSource: String? = null,
    val inProgressDestination: String? = null,
    val copyFinished: Boolean = false,
    val cancelled: Boolean = false,
    val failure: MediaTransferFailure? = null,
    val requiredBytes: Long? = null,
    val availableBytes: Long? = null
) {
    val totalFiles: Int get() = sources.distinct().size

    /** Sources that still need a destination document. */
    val remainingUris: List<String> get() = sources.distinct() - copiedUris.toSet()
}

/**
 * Durable, process-wide storage for the one active transfer. WorkManager already persists the job; this journal
 * persists what WorkManager cannot: which individual files are already complete, which destination document was
 * being written when the process died, and whether the user cancelled.
 *
 * SharedPreferences is used with `commit()` because the worker writes from a background thread and a lost write
 * would turn a safe resume into a duplicate copy. There is only one small JSON value.
 */
object MediaTransferJournal {

    private const val PREFS_NAME = "aptura_media_transfer_journal"
    private const val KEY_JOB = "job"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val lock = Any()

    fun read(context: Context): MediaTransferJob? = synchronized(lock) {
        readLocked(context)
    }

    fun write(context: Context, job: MediaTransferJob) = synchronized(lock) {
        writeLocked(context, job)
    }

    fun update(context: Context, transform: (MediaTransferJob) -> MediaTransferJob): MediaTransferJob? =
        synchronized(lock) {
            val current = readLocked(context) ?: return null
            val next = transform(current)
            writeLocked(context, next)
            next
        }

    fun clear(context: Context) = synchronized(lock) {
        prefs(context).edit().remove(KEY_JOB).commit()
        Unit
    }

    /** Clears only if the journal still belongs to [jobId], so a late result cannot delete a newer transfer. */
    fun clearIf(context: Context, jobId: String) = synchronized(lock) {
        if (readLocked(context)?.id == jobId) {
            prefs(context).edit().remove(KEY_JOB).commit()
        }
        Unit
    }

    private fun readLocked(context: Context): MediaTransferJob? {
        val raw = prefs(context).getString(KEY_JOB, null) ?: return null
        return try {
            json.decodeFromString<MediaTransferJob>(raw)
        } catch (_: Exception) {
            // A corrupt journal must not crash the library. The next transfer overwrites it.
            null
        }
    }

    private fun writeLocked(context: Context, job: MediaTransferJob) {
        prefs(context).edit().putString(KEY_JOB, json.encodeToString(job)).commit()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
