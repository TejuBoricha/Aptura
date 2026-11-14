@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.core.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import androidx.media3.decoder.ffmpeg.FfmpegLibrary

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
    val audio: List<CodecSupportEntry>,
    /** libavcodec's identification string (for example `Lavc63.1.102`) when the bundled audio extension loaded, or null when it did not. */
    val ffmpegVersion: String? = null
) {
    /**
     * Whether the bundled audio extension is really there. A release build can lose it without any crash (the native
     * library is found by name), so the report says so instead of leaving a support conversation to guess.
     */
    fun ffmpegStatusLine(): String =
        if (ffmpegVersion != null) "FFmpeg audio extension: loaded (libavcodec ${ffmpegVersion.removePrefix("Lavc")})" else "FFmpeg audio extension: not loaded"

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
        appendLine(ffmpegStatusLine())
        appendLine(FFMPEG_NOTE)
    }

    companion object {
        /**
         * What the bundled extension decodes (spec/11B D2, DEC-020): only the audio formats that are free to ship and that
         * Android does not guarantee on every device. Everything else is the device's own decoder, and the wording names
         * formats factually without claiming a capability or a licence (spec/11B section 6).
         */
        const val FFMPEG_NOTE =
            "Bundled FFmpeg extension: audio only, and only ALAC and G.711 A-law and mu-law. Every other format, " +
                "including AAC, AC-3, E-AC-3, DTS, TrueHD, FLAC and MP3, is decoded by this device itself. " +
                "No software video decoders are bundled; video codecs the device cannot decode here can be " +
                "opened in an external player."
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
            audio = audio,
            ffmpegVersion = ffmpegVersion()
        )
    }.getOrElse {
        CodecSupportReport(
            deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL} - Android ${Build.VERSION.SDK_INT}",
            video = emptyList(),
            audio = emptyList(),
            ffmpegVersion = ffmpegVersion()
        )
    }

    /** FFmpeg's version when its native libraries loaded in this process, otherwise null. Never throws. */
    private fun ffmpegVersion(): String? = runCatching {
        if (FfmpegLibrary.isAvailable()) FfmpegLibrary.getVersion() ?: "unknown version" else null
    }.getOrNull()

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
