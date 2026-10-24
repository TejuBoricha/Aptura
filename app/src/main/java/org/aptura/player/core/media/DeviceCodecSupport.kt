package org.aptura.player.core.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build

/** One MIME type the platform can decode, with the best decoder the device exposes for it. */
data class CodecSupportEntry(
    val mime: String,
    val label: String,
    val decoderName: String,
    val hardware: Boolean
)

/**
 * Read-only snapshot of this device's MediaCodec capabilities.
 *
 * Aptura deliberately ships **no software video decoders** (ADR-010: patent-pool safe, LGPL-clean build). Video is
 * therefore always platform-decoded; this report is the honest answer to "will my phone play this?", and the data a
 * support report needs. It never touches the network and contains no file paths.
 */
data class CodecSupportReport(
    val deviceLabel: String,
    val video: List<CodecSupportEntry>,
    val audio: List<CodecSupportEntry>
) {
    fun toPlainText(): String = buildString {
        appendLine("Aptura device codec report")
        appendLine(deviceLabel)
        appendLine()
        appendLine("Video decoders (platform MediaCodec)")
        if (video.isEmpty()) {
            appendLine("  none reported")
        } else {
            video.forEach { entry ->
                appendLine("  ${entry.label} - ${if (entry.hardware) "hardware" else "software"} - ${entry.decoderName}")
            }
        }
        appendLine()
        appendLine("Audio decoders (platform MediaCodec)")
        if (audio.isEmpty()) {
            appendLine("  none reported")
        } else {
            audio.forEach { entry ->
                appendLine("  ${entry.label} - ${if (entry.hardware) "hardware" else "software"} - ${entry.decoderName}")
            }
        }
        appendLine()
        appendLine(FFMPEG_NOTE)
    }

    companion object {
        const val FFMPEG_NOTE =
            "Bundled FFmpeg extension: audio only (AAC, AC-3, E-AC-3, DTS, TrueHD, MLP, FLAC, ALAC, MP3, " +
                "A-law, mu-law, AAC-LATM). No software video decoders are bundled; video codecs the device " +
                "cannot decode here can be opened in an external player."
    }
}

object DeviceCodecSupport {

    private val videoOrder = listOf(
        "video/avc",
        "video/hevc",
        "video/x-vnd.on2.vp9",
        "video/x-vnd.on2.vp8",
        "video/av01",
        "video/mp4v-es",
        "video/mpeg2",
        "video/3gpp",
        "video/dolby-vision"
    )

    private val labels = mapOf(
        "video/avc" to "H.264 / AVC",
        "video/hevc" to "H.265 / HEVC",
        "video/x-vnd.on2.vp8" to "VP8",
        "video/x-vnd.on2.vp9" to "VP9",
        "video/av01" to "AV1",
        "video/mp4v-es" to "MPEG-4 Part 2",
        "video/mpeg2" to "MPEG-2",
        "video/3gpp" to "H.263",
        "video/dolby-vision" to "Dolby Vision",
        "video/x-ms-wmv" to "WMV / VC-1",
        "audio/mp4a-latm" to "AAC",
        "audio/ac3" to "AC-3",
        "audio/eac3" to "E-AC-3",
        "audio/vnd.dts" to "DTS",
        "audio/vnd.dts.hd" to "DTS-HD",
        "audio/true-hd" to "TrueHD",
        "audio/flac" to "FLAC",
        "audio/alac" to "ALAC",
        "audio/mpeg" to "MP3",
        "audio/opus" to "Opus",
        "audio/vorbis" to "Vorbis",
        "audio/raw" to "PCM",
        "audio/pcm" to "PCM"
    )

    /** Human label for a MediaCodec MIME type, falling back to the subtype so unknown codecs stay readable. */
    internal fun labelFor(mime: String): String =
        labels[mime] ?: mime.substringAfter('/').replace('-', ' ').replaceFirstChar { it.uppercase() }

    fun report(): CodecSupportReport = runCatching {
        val infos = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            .filter { !it.isEncoder }
            .filterNot { Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && it.isAlias }

        val video = decodeEntries(infos, "video/").sortedWith(
            compareBy(
                { entry -> videoOrder.indexOf(entry.mime).let { if (it < 0) Int.MAX_VALUE else it } },
                { it.label }
            )
        )
        val audio = decodeEntries(infos, "audio/").sortedBy { it.label }

        CodecSupportReport(
            deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL} - Android ${Build.VERSION.SDK_INT}",
            video = video,
            audio = audio
        )
    }.getOrElse {
        CodecSupportReport(
            deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL} - Android ${Build.VERSION.SDK_INT}",
            video = emptyList(),
            audio = emptyList()
        )
    }

    private fun decodeEntries(infos: List<MediaCodecInfo>, prefix: String): List<CodecSupportEntry> {
        val byMime = linkedMapOf<String, CodecSupportEntry>()
        infos.forEach { info ->
            info.supportedTypes.filter { it.startsWith(prefix) }.forEach { mime ->
                val candidate = CodecSupportEntry(
                    mime = mime,
                    label = labelFor(mime),
                    decoderName = info.name,
                    hardware = isHardware(info)
                )
                val existing = byMime[mime]
                // Prefer a hardware decoder when the device exposes both kinds for the same MIME.
                if (existing == null || (!existing.hardware && candidate.hardware)) {
                    byMime[mime] = candidate
                }
            }
        }
        return byMime.values.toList()
    }

    private fun isHardware(info: MediaCodecInfo): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            info.isHardwareAccelerated
        } else {
            // Pre-Q has no capability flag; Google's software codecs are the only reliable naming convention.
            !info.name.startsWith("OMX.google.") && !info.name.startsWith("c2.android.")
        }
}
