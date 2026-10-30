package org.aptura.player

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import org.aptura.player.data.scanner.MediaScanScheduler
import org.aptura.player.data.transfer.MediaTransferScheduler
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder

/**
 * Aptura Application class.
 * Configures application-wide singletons and hardware-accelerated Coil VideoFrameDecoder
 * for video thumbnail rendering (ADR-001, spec/06).
 */
open class ApturaApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    /** On-demand WorkManager initialisation: scans can be scheduled from `onCreate` regardless of provider ordering. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        // Persistent local diagnostics + crash handler first, so a crash in anything below is captured.
        org.aptura.player.core.diagnostics.ApturaDiagnostics.initialize(this)
        try {
            val filter = org.aptura.player.core.platform.StorageVolumeReceiver.createIntentFilter()
            // Not exported: only the system (which sends these broadcasts) and this app may reach the receiver.
            ContextCompat.registerReceiver(
                this,
                org.aptura.player.core.platform.StorageVolumeReceiver(),
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Throwable) {}

        // Index new videos without the user having to press Refresh (audit P0-09).
        MediaScanScheduler.scheduleOnProcessStart(this)
        // A transfer that WorkManager preempted while no library screen existed resumes from its journal.
        runCatching { MediaTransferScheduler.resumePending(this) }
        observeMediaStoreChanges()
    }

    /** Re-scans (debounced) whenever Android's video index changes, e.g. a download or recording finishes. */
    private fun observeMediaStoreChanges() {
        val handler = Handler(Looper.getMainLooper())
        val rescan = Runnable { MediaScanScheduler.scheduleImmediateScan(this) }
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            /* notifyForDescendants = */ true,
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    handler.removeCallbacks(rescan)
                    handler.postDelayed(rescan, MEDIASTORE_RESCAN_DEBOUNCE_MS)
                }
            }
        )
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        org.aptura.player.core.media.ThumbnailBitmapCache.trim(level)
    }

    private companion object {
        const val MEDIASTORE_RESCAN_DEBOUNCE_MS = 4_000L
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
