package org.aptura.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

/**
 * Aptura Application class.
 * Configures application-wide singletons and hardware-accelerated Coil VideoFrameDecoder
 * for video thumbnail rendering (ADR-001, spec/06).
 */
class ApturaApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        try {
            val filter = org.aptura.player.core.platform.StorageVolumeReceiver.createIntentFilter()
            registerReceiver(org.aptura.player.core.platform.StorageVolumeReceiver(), filter)
        } catch (_: Throwable) {}
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
