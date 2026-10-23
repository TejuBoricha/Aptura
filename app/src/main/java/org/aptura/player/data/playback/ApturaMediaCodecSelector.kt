@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Custom MediaCodecSelector adhering to spec/04 FR-PLAY-003 and spec/07 §4.
 *
 * Configurable decoder strategies:
 * - 0 (Auto): Hardware decoders preferred first; software decoders positioned as secondary
 *   fallbacks when decoder fallback is enabled.
 * - 1 (Hardware): Hardware-accelerated video decoders only; no software video fallback.
 *   For audio MIME types, preserves platform audio decoders if no hardware
 *   audio MediaCodec is reported so Opus/Vorbis/AAC audio never breaks in Hardware mode.
 * - 2 (Software): Prioritizes platform software decoders (e.g. c2.android.hevc.decoder)
 *   to avoid hardware codec crashes or unsupported profile failures (e.g. 10-bit HEVC).
 *
 * Also appends base-layer and alias fallback decoders for [MimeTypes.VIDEO_DOLBY_VISION],
 * DivX/Xvid (`video/divx`, `video/xvid`), MPEG-1/2 (`video/mpeg`, `video/mpeg2`),
 * VC-1/WMV (`video/x-ms-wmv`), and VP8/VP9/AV1 aliases.
 */
class ApturaMediaCodecSelector(
    private val baseSelector: MediaCodecSelector = MediaCodecSelector.DEFAULT,
    private val getStrategy: () -> Int = { 0 }
) : MediaCodecSelector {

    constructor(getStrategy: () -> Int) : this(MediaCodecSelector.DEFAULT, getStrategy)

    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean
    ): List<MediaCodecInfo> {
        val primaryList = baseSelector.getDecoderInfos(
            mimeType,
            requiresSecureDecoder,
            requiresTunnelingDecoder
        )

        val fallbackMimes = fallbackMimeTypesFor(mimeType)
        val defaultList = if (fallbackMimes.isNotEmpty()) {
            val fallbacks = fallbackMimes.flatMap { fallbackMime ->
                baseSelector.getDecoderInfos(
                    fallbackMime,
                    requiresSecureDecoder,
                    requiresTunnelingDecoder
                )
            }
            (primaryList + fallbacks).distinctBy { it.name }
        } else {
            primaryList
        }

        if (defaultList.isEmpty()) return emptyList()

        return when (getStrategy()) {
            2 -> { // Software preferred (strict software decode to prevent hardware crash re-entry)
                val software = defaultList.filter { it.softwareOnly || !it.hardwareAccelerated }
                if (software.isNotEmpty()) software else defaultList
            }
            1 -> { // Hardware ONLY for video: never silently falls back to a software video decoder.
                // However, Android's built-in audio decoders (c2.android.opus.decoder, c2.android.vorbis.decoder, etc.)
                // are software MediaCodecs, so audio/* must fall back to defaultList if no HW audio decoder exists.
                val hardware = defaultList.filter { it.hardwareAccelerated && !it.softwareOnly }
                if (hardware.isNotEmpty() || !mimeType.startsWith("audio/")) hardware else defaultList
            }
            else -> { // Auto: hardware first, followed by software codecs as fallback
                val hardware = defaultList.filter { it.hardwareAccelerated && !it.softwareOnly }
                val software = defaultList.filter { !it.hardwareAccelerated || it.softwareOnly }
                if (hardware.isNotEmpty()) hardware + software else defaultList
            }
        }
    }

    private fun fallbackMimeTypesFor(mimeType: String): List<String> = when (mimeType.lowercase()) {
        MimeTypes.VIDEO_DOLBY_VISION -> listOf(MimeTypes.VIDEO_H265, MimeTypes.VIDEO_H264)
        "video/divx", "video/xvid", "video/x-msvideo" -> listOf(MimeTypes.VIDEO_MP4V)
        MimeTypes.VIDEO_MPEG -> listOf(MimeTypes.VIDEO_MPEG2)
        MimeTypes.VIDEO_MPEG2 -> listOf(MimeTypes.VIDEO_MPEG)
        "video/x-ms-wmv" -> listOf(MimeTypes.VIDEO_VC1)
        "video/vp8" -> listOf(MimeTypes.VIDEO_VP8)
        "video/vp9" -> listOf(MimeTypes.VIDEO_VP9)
        "video/av1" -> listOf(MimeTypes.VIDEO_AV1)
        else -> emptyList()
    }
}
