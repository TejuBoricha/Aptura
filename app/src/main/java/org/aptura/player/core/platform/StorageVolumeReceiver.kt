package org.aptura.player.core.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.core.diagnostics.EventCode
import org.aptura.player.data.playback.PlayerManager
import org.aptura.player.data.scanner.MediaScanScheduler

/**
 * BroadcastReceiver for hardware storage volume hot-plug events (FR-LIB-021).
 * Listens for external SD cards and USB drives being mounted, unmounted, or removed.
 *
 * When media is removed during active playback:
 * - Safely pauses playback to prevent decoder deadlocks or ANRs.
 * - Triggers differential library reconciliation to mark items as missing rather than deleting.
 *
 * When media is mounted:
 * - Immediately schedules differential scan to ingest new files and restore missing records.
 */
class StorageVolumeReceiver : BroadcastReceiver() {

    companion object {
        /** The only broadcasts this receiver acts on. Anything else is ignored before it is even logged. */
        private val STORAGE_ACTIONS = setOf(
            Intent.ACTION_MEDIA_MOUNTED,
            Intent.ACTION_MEDIA_UNMOUNTED,
            Intent.ACTION_MEDIA_EJECT,
            Intent.ACTION_MEDIA_REMOVED,
            Intent.ACTION_MEDIA_BAD_REMOVAL
        )

        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                STORAGE_ACTIONS.forEach { addAction(it) }
                addDataScheme("file")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in STORAGE_ACTIONS) return
        val path = intent.data?.path ?: ""

        ApturaDiagnostics.logEvent(
            code = EventCode.EV_006,
            payload = mapOf(
                "storage_event" to action,
                "path" to org.aptura.player.core.diagnostics.RedactionEngine.redactText(path)
            )
        )

        when (action) {
            Intent.ACTION_MEDIA_MOUNTED -> {
                // Removable volume mounted: trigger immediate scan
                try {
                    MediaScanScheduler.scheduleImmediateScan(context.applicationContext)
                } catch (_: Throwable) {}
            }
            Intent.ACTION_MEDIA_UNMOUNTED,
            Intent.ACTION_MEDIA_EJECT,
            Intent.ACTION_MEDIA_REMOVED,
            Intent.ACTION_MEDIA_BAD_REMOVAL -> {
                // Storage removed: pause if playing media from this volume
                try {
                    val controller = PlayerManager.activeController
                    controller?.let {
                        val activeItem = it.player.currentMediaItem
                        val activeUri = activeItem?.localConfiguration?.uri?.toString() ?: ""
                        if (path.isNotBlank() && activeUri.contains(path)) {
                            CoroutineScope(Dispatchers.Main).launch {
                                it.pause()
                            }
                        }
                    }
                } catch (_: Throwable) {}

                // Trigger library reconciliation to mark unmounted files as missing without deleting
                try {
                    MediaScanScheduler.scheduleImmediateScan(context.applicationContext)
                } catch (_: Throwable) {}
            }
        }
    }
}
