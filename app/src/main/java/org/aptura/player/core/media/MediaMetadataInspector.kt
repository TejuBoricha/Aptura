@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.core.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.MetadataRetriever
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Granular media metadata inspection using Android framework MediaExtractor and MediaMetadataRetriever.
 * Strictly local, offline, and lightweight (extracts track and container header data without decoding).
 * Complies with spec/04, spec/14, and ADR-007 (0 third-party telemetry, 0 network).
 */
data class MediaExtendedMetadata(
    val videoCodec: String? = null,
    val videoProfile: String? = null,
    val videoBitrate: Long? = null,
    val colorStandard: String? = null,
    val colorTransfer: String? = null,
    val isHdr: Boolean = false,
    val audioTracks: List<AudioTrackMetadata> = emptyList(),
    val subtitleTracks: List<SubtitleTrackMetadata> = emptyList(),
    val durationMs: Long = 0L,
    val bitRateTotal: Long? = null,
    /** True only when the container was actually opened and its tracks listed (so "no tracks" is a fact, not a guess). */
    val inspected: Boolean = false
)

data class AudioTrackMetadata(
    val index: Int,
    val codec: String,
    val channelCount: Int,
    val sampleRate: Int,
    val bitrate: Long? = null,
    val language: String? = null,
    val name: String? = null
)

data class SubtitleTrackMetadata(
    val index: Int,
    val format: String,
    val language: String? = null,
    val name: String? = null
)

object MediaMetadataInspector {

    fun inspect(context: Context, uri: Uri): MediaExtendedMetadata {
        val audioTracks = mutableListOf<AudioTrackMetadata>()
        val subtitleTracks = mutableListOf<SubtitleTrackMetadata>()
        var videoCodec: String? = null
        var videoProfile: String? = null
        var videoBitrate: Long? = null
        var isHdr = false
        var colorStandardStr: String? = null
        var colorTransferStr: String? = null
        var totalBitrate: Long? = null
        var durationMs = 0L
        var inspected = false

        // 1. MediaMetadataRetriever pass for overall container stats
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val brStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (!brStr.isNullOrBlank()) {
                totalBitrate = brStr.toLongOrNull()
            }
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (!durStr.isNullOrBlank()) {
                durationMs = durStr.toLongOrNull() ?: 0L
            }
            retriever.release()
        } catch (_: Throwable) {
            // best-effort
        }

        // 2. MediaExtractor pass for granular track inspection
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            inspected = true
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()

                if (mime.startsWith("video/")) {
                    if (videoCodec == null) {
                        videoCodec = formatMimeToCodecName(mime)
                        if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            videoBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE).toLong()
                        }
                        if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                            val transfer = format.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                            isHdr = transfer == MediaFormat.COLOR_TRANSFER_ST2084 ||
                                    transfer == MediaFormat.COLOR_TRANSFER_HLG
                            colorTransferStr = when (transfer) {
                                MediaFormat.COLOR_TRANSFER_ST2084 -> "HDR10 / PQ (ST 2084)"
                                MediaFormat.COLOR_TRANSFER_HLG -> "HLG (ARIB STD-B67)"
                                MediaFormat.COLOR_TRANSFER_SDR_VIDEO -> "SDR (BT.709)"
                                MediaFormat.COLOR_TRANSFER_LINEAR -> "Linear"
                                else -> null
                            }
                        }
                        if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) {
                            val standard = format.getInteger(MediaFormat.KEY_COLOR_STANDARD)
                            colorStandardStr = when (standard) {
                                MediaFormat.COLOR_STANDARD_BT2020 -> "BT.2020 (Wide Gamut)"
                                MediaFormat.COLOR_STANDARD_BT709 -> "BT.709 (HD/sRGB)"
                                MediaFormat.COLOR_STANDARD_BT601_NTSC -> "BT.601 NTSC"
                                MediaFormat.COLOR_STANDARD_BT601_PAL -> "BT.601 PAL"
                                else -> null
                            }
                        }
                    }
                } else if (mime.startsWith("audio/")) {
                    val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    } else 2
                    val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    } else 0
                    val br = if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        format.getInteger(MediaFormat.KEY_BIT_RATE).toLong()
                    } else null
                    val lang = if (format.containsKey(MediaFormat.KEY_LANGUAGE)) {
                        format.getString(MediaFormat.KEY_LANGUAGE)
                    } else null

                    audioTracks.add(
                        AudioTrackMetadata(
                            index = i,
                            codec = formatMimeToAudioCodecName(mime),
                            channelCount = channels,
                            sampleRate = sampleRate,
                            bitrate = br,
                            language = lang,
                            name = "Audio Track ${audioTracks.size + 1}"
                        )
                    )
                } else if (mime.startsWith("text/") || mime.startsWith("application/")) {
                    val lang = if (format.containsKey(MediaFormat.KEY_LANGUAGE)) {
                        format.getString(MediaFormat.KEY_LANGUAGE)
                    } else null
                    subtitleTracks.add(
                        SubtitleTrackMetadata(
                            index = i,
                            format = formatMimeToSubtitleName(mime),
                            language = lang,
                            name = "Subtitle ${subtitleTracks.size + 1}"
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            // best-effort
        } finally {
            try { extractor.release() } catch (_: Throwable) {}
        }

        // 3. Media3 MetadataRetriever pass when Android MediaExtractor misses MKV/TS tracks (e.g., AC-3, E-AC-3, DTS, embedded SRT/ASS/PGS)
        if (videoCodec == null || audioTracks.isEmpty() || subtitleTracks.isEmpty()) {
            val needAudio = audioTracks.isEmpty()
            val needSubtitles = subtitleTracks.isEmpty()
            try {
                val trackGroups = MetadataRetriever.retrieveMetadata(context, MediaItem.fromUri(uri))
                    .get(2, TimeUnit.SECONDS)
                inspected = true
                for (i in 0 until trackGroups.length) {
                    val group = trackGroups.get(i)
                    if (group.length <= 0) continue
                    val format = group.getFormat(0)
                    val mime = format.sampleMimeType.orEmpty()
                    val codeMime = format.codecs?.takeIf { it.isNotBlank() }

                    if (mime.startsWith("video/") && videoCodec == null) {
                        videoCodec = formatMimeToCodecName(mime)
                        if (format.bitrate > 0) {
                            videoBitrate = format.bitrate.toLong()
                        }
                        format.colorInfo?.let { color ->
                            isHdr = color.colorTransfer == C.COLOR_TRANSFER_ST2084 ||
                                color.colorTransfer == C.COLOR_TRANSFER_HLG
                            colorTransferStr = when (color.colorTransfer) {
                                C.COLOR_TRANSFER_ST2084 -> "HDR10 / PQ (ST 2084)"
                                C.COLOR_TRANSFER_HLG -> "HLG (ARIB STD-B67)"
                                C.COLOR_TRANSFER_SDR -> "SDR (BT.709)"
                                else -> colorTransferStr
                            }
                            colorStandardStr = when (color.colorSpace) {
                                C.COLOR_SPACE_BT2020 -> "BT.2020 (Wide Gamut)"
                                C.COLOR_SPACE_BT709 -> "BT.709 (HD/sRGB)"
                                C.COLOR_SPACE_BT601 -> "BT.601"
                                else -> colorStandardStr
                            }
                        }
                    } else if (mime.startsWith("audio/") && needAudio) {
                        audioTracks.add(
                            AudioTrackMetadata(
                                index = i,
                                codec = formatMimeToAudioCodecName(mime),
                                channelCount = format.channelCount.takeIf { it > 0 } ?: 2,
                                sampleRate = format.sampleRate.takeIf { it > 0 } ?: 0,
                                bitrate = format.bitrate.takeIf { it > 0 }?.toLong(),
                                language = format.language,
                                name = format.label?.takeIf { it.isNotBlank() } ?: "Audio Track ${audioTracks.size + 1}"
                            )
                        )
                    } else if (
                        (mime.startsWith("text/") || mime.startsWith("application/") || mime.startsWith("image/")) &&
                        needSubtitles &&
                        !mime.equals("application/x-media3-cues", ignoreCase = true)
                    ) {
                        val effectiveSubMime = if (mime == "application/x-media3-cues" && codeMime != null) codeMime else mime
                        subtitleTracks.add(
                            SubtitleTrackMetadata(
                                index = i,
                                format = formatMimeToSubtitleName(effectiveSubMime),
                                language = format.language,
                                name = format.label?.takeIf { it.isNotBlank() } ?: "Subtitle ${subtitleTracks.size + 1}"
                            )
                        )
                    } else if (mime.equals("application/x-media3-cues", ignoreCase = true) && needSubtitles) {
                        subtitleTracks.add(
                            SubtitleTrackMetadata(
                                index = i,
                                format = formatMimeToSubtitleName(codeMime ?: "SubRip (SRT)"),
                                language = format.language,
                                name = format.label?.takeIf { it.isNotBlank() } ?: "Subtitle ${subtitleTracks.size + 1}"
                            )
                        )
                    }
                }
            } catch (_: Throwable) {
                // best-effort fallback
            }
        }

        return MediaExtendedMetadata(
            videoCodec = videoCodec,
            videoProfile = videoProfile,
            videoBitrate = videoBitrate,
            colorStandard = colorStandardStr,
            colorTransfer = colorTransferStr,
            isHdr = isHdr,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            durationMs = durationMs,
            bitRateTotal = totalBitrate,
            inspected = inspected
        )
    }

    fun formatMimeToCodecName(mime: String): String = when (mime.lowercase(Locale.US)) {
        "video/avc", "video/mp4v-es" -> "H.264 / AVC"
        "video/hevc" -> "H.265 / HEVC"
        "video/dolby-vision" -> "Dolby Vision"
        "video/x-vnd.on2.vp9" -> "Google VP9"
        "video/x-vnd.on2.vp8" -> "Google VP8"
        "video/av01" -> "AOMedia AV1"
        "video/mpeg2", "video/mpeg" -> "MPEG-2"
        "video/wvc1", "video/vc1" -> "VC-1 / WMV"
        "video/3gpp" -> "H.263"
        "video/mp4" -> "MPEG-4 Part 2"
        else -> mime.removePrefix("video/").uppercase(Locale.US)
    }

    fun formatMimeToAudioCodecName(mime: String): String = when (mime.lowercase(Locale.US)) {
        "audio/mp4a-latm", "audio/aac" -> "AAC"
        "audio/ac3" -> "Dolby Digital (AC-3)"
        "audio/eac3", "audio/eac3-joc" -> "Dolby Digital Plus (E-AC-3)"
        "audio/vnd.dts", "audio/dts" -> "DTS"
        "audio/vnd.dts.hd" -> "DTS-HD"
        "audio/true-hd" -> "Dolby TrueHD"
        "audio/opus" -> "Opus"
        "audio/flac" -> "FLAC (Lossless)"
        "audio/alac" -> "Apple Lossless (ALAC)"
        "audio/mpeg", "audio/mp3" -> "MP3"
        "audio/mpeg-l2" -> "MP2"
        "audio/vorbis" -> "Vorbis"
        "audio/raw" -> "PCM"
        else -> mime.removePrefix("audio/").uppercase(Locale.US)
    }

    fun formatMimeToSubtitleName(mime: String): String = when (mime.lowercase(Locale.US)) {
        "application/x-subrip", "text/x-subrip" -> "SubRip (SRT)"
        "text/vtt" -> "WebVTT"
        "application/x-matroska", "text/x-ssa", "text/x-ass" -> "SubStation Alpha (ASS)"
        "application/ttml+xml" -> "Timed Text (TTML)"
        "application/pgs" -> "Blu-ray PGS"
        "application/vobsub" -> "DVD VobSub"
        "application/dvbsubs" -> "DVB Subtitles"
        else -> mime.substringAfterLast('/').uppercase(Locale.US)
    }

    fun formatBitrate(bps: Long): String {
        if (bps <= 0L) return "Unknown"
        val kbps = bps / 1000.0
        val mbps = kbps / 1000.0
        return when {
            mbps >= 1.0 -> String.format(Locale.US, "%.1f Mbps", mbps)
            kbps >= 1.0 -> "${Math.round(kbps)} kbps"
            else -> "$bps bps"
        }
    }

    fun formatChannels(channelCount: Int): String = when (channelCount) {
        1 -> "Mono (1.0 ch)"
        2 -> "Stereo (2.0 ch)"
        6 -> "5.1 Surround (6 ch)"
        8 -> "7.1 Surround (8 ch)"
        else -> "$channelCount channels"
    }

    fun formatSampleRate(sampleRateHz: Int): String {
        if (sampleRateHz <= 0) return "Unknown"
        val khz = sampleRateHz / 1000.0
        return String.format(Locale.US, "%.1f kHz", khz)
    }
}
