@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Custom MediaCodecSelector adhering to spec/04 FR-PLAY-003 and spec/07 §4.
 *
 * Configurable decoder strategies:
 * - 0 (Auto): Hardware decoders preferred first; software decoders positioned as secondary
 *   fallbacks when decoder fallback is enabled.
 * - 1 (Hardware): Prioritizes hardware-accelerated decoders.
 * - 2 (Software): Prioritizes platform software decoders (e.g. c2.android.hevc.decoder)
 *   to avoid hardware codec crashes or unsupported profile failures (e.g. 10-bit HEVC).
 */
class ApturaMediaCodecSelector(
    private val getStrategy: () -> Int = { 0 }
) : MediaCodecSelector {

    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean
    ): List<MediaCodecInfo> {
        val defaultList = MediaCodecSelector.DEFAULT.getDecoderInfos(
            mimeType,
            requiresSecureDecoder,
            requiresTunnelingDecoder
        )
        if (defaultList.isEmpty()) return emptyList()

        return when (getStrategy()) {
            2 -> { // Software preferred (strict software decode to prevent hardware crash re-entry)
                val software = defaultList.filter { it.softwareOnly || !it.hardwareAccelerated }
                if (software.isNotEmpty()) software else defaultList
            }
            1 -> { // Hardware preferred
                val hardware = defaultList.filter { it.hardwareAccelerated && !it.softwareOnly }
                val software = defaultList.filter { !it.hardwareAccelerated || it.softwareOnly }
                if (hardware.isNotEmpty()) hardware + software else defaultList
            }
            else -> { // Auto: hardware first, followed by software codecs as fallback
                val hardware = defaultList.filter { it.hardwareAccelerated && !it.softwareOnly }
                val software = defaultList.filter { !it.hardwareAccelerated || it.softwareOnly }
                if (hardware.isNotEmpty()) hardware + software else defaultList
            }
        }
    }
}
