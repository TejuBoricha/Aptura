@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import org.junit.Assert.assertEquals
import org.junit.Test

class ApturaMediaCodecSelectorTest {

    private fun codecInfo(
        name: String,
        mimeType: String,
        hardwareAccelerated: Boolean,
        softwareOnly: Boolean = !hardwareAccelerated
    ): MediaCodecInfo = MediaCodecInfo.newInstance(
        /* name = */ name,
        /* mimeType = */ mimeType,
        /* codecMimeType = */ mimeType,
        /* capabilities = */ null,
        /* hardwareAccelerated = */ hardwareAccelerated,
        /* softwareOnly = */ softwareOnly,
        /* vendor = */ hardwareAccelerated,
        /* forceDisableAdaptive = */ false,
        /* forceSecure = */ false
    )

    private val hwHevc = codecInfo("c2.qti.hevc.decoder", MimeTypes.VIDEO_H265, hardwareAccelerated = true)
    private val swHevc = codecInfo("c2.android.hevc.decoder", MimeTypes.VIDEO_H265, hardwareAccelerated = false)
    private val hwDv = codecInfo("c2.qti.dv.decoder", MimeTypes.VIDEO_DOLBY_VISION, hardwareAccelerated = true)

    @Test
    fun getDecoderInfos_autoStrategy_ordersHardwareBeforeSoftwareFallback() {
        val base = MediaCodecSelector { mimeType, _, _ ->
            if (mimeType == MimeTypes.VIDEO_H265) listOf(swHevc, hwHevc) else emptyList()
        }
        val selector = ApturaMediaCodecSelector(base) { 0 }

        val result = selector.getDecoderInfos(MimeTypes.VIDEO_H265, false, false)

        assertEquals(listOf("c2.qti.hevc.decoder", "c2.android.hevc.decoder"), result.map { it.name })
    }

    @Test
    fun getDecoderInfos_hardwareStrategy_excludesSoftwareDecoders() {
        val base = MediaCodecSelector { mimeType, _, _ ->
            if (mimeType == MimeTypes.VIDEO_H265) listOf(hwHevc, swHevc) else emptyList()
        }
        val selector = ApturaMediaCodecSelector(base) { 1 }

        val result = selector.getDecoderInfos(MimeTypes.VIDEO_H265, false, false)

        assertEquals(listOf("c2.qti.hevc.decoder"), result.map { it.name })
    }

    @Test
    fun getDecoderInfos_softwareStrategy_prioritizesSoftwareDecoders() {
        val base = MediaCodecSelector { mimeType, _, _ ->
            if (mimeType == MimeTypes.VIDEO_H265) listOf(hwHevc, swHevc) else emptyList()
        }
        val selector = ApturaMediaCodecSelector(base) { 2 }

        val result = selector.getDecoderInfos(MimeTypes.VIDEO_H265, false, false)

        assertEquals(listOf("c2.android.hevc.decoder"), result.map { it.name })
    }

    @Test
    fun getDecoderInfos_dolbyVision_appendsHevcBaseLayerFallbacksAcrossStrategies() {
        val base = MediaCodecSelector { mimeType, _, _ ->
            when (mimeType) {
                MimeTypes.VIDEO_DOLBY_VISION -> listOf(hwDv)
                MimeTypes.VIDEO_H265 -> listOf(hwHevc, swHevc)
                else -> emptyList()
            }
        }
        var strategy = 0
        val selector = ApturaMediaCodecSelector(base) { strategy }

        // Auto (0): hardware DV -> hardware HEVC -> software HEVC
        assertEquals(
            listOf("c2.qti.dv.decoder", "c2.qti.hevc.decoder", "c2.android.hevc.decoder"),
            selector.getDecoderInfos(MimeTypes.VIDEO_DOLBY_VISION, false, false).map { it.name }
        )

        // Software (2): falls back to software HEVC base-layer decoder when no software DV decoder exists
        strategy = 2
        assertEquals(
            listOf("c2.android.hevc.decoder"),
            selector.getDecoderInfos(MimeTypes.VIDEO_DOLBY_VISION, false, false).map { it.name }
        )
    }
}
