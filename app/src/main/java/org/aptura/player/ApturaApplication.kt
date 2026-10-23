package org.aptura.player

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import org.aptura.player.data.scanner.MediaScanScheduler
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

/**
 * Aptura Application class.
 * Configures application-wide singletons and hardware-accelerated Coil VideoFrameDecoder
 * for video thumbnail rendering (ADR-001, spec/06).
 */
open class ApturaApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    /** On-demand WorkManager initialisation: scans can be scheduled from `onCreate` regardless of provider ordering. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
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

    private companion object {
        const val MEDIASTORE_RESCAN_DEBOUNCE_MS = 4_000L
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
