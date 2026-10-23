@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.core.diagnostics

import androidx.media3.common.PlaybackException

/**
 * Stable, privacy-preserving Error Taxonomy per spec/04 FR-PLAY-003 and FR-PLAY-004.
 *
 * Maps low-level Media3/ExoPlayer exceptions and codec dumps into user-understandable,
 * searchable taxonomy codes and honest explanations. Never displays raw class dumps,
 * stack traces, or internal format descriptor strings to end users.
 */
data class TaxonomyError(
    val code: String,
    val title: String,
    val whatHappened: String,
    val whatWasTried: String,
    val suggestedActions: List<String>,
    val canSwitchToSoftware: Boolean = false,
    val isRecoverable: Boolean = true
)

object ErrorTaxonomy {

    fun isAudioError(exception: PlaybackException): Boolean {
        val errorCode = exception.errorCode
        if (
            errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED
        ) {
            return true
        }
        val combinedMessage = "${exception.message.orEmpty()} ${exception.cause?.message.orEmpty()}"
        val isDecoderErrorCode =
            errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ||
                errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
        return isDecoderErrorCode && (
            combinedMessage.contains("MediaCodecAudioRenderer", ignoreCase = true) ||
                combinedMessage.contains("AudioTrack", ignoreCase = true) ||
                combinedMessage.contains("audio/", ignoreCase = true)
            )
    }

    fun mapException(
        exception: PlaybackException,
        isSoftwareAlreadyAttempted: Boolean = false
    ): TaxonomyError {
        val errorCode = exception.errorCode
        val rawMessage = exception.message.orEmpty()

        if (isAudioError(exception)) {
            return TaxonomyError(
                code = "AUDIO_INIT_FAILED",
                title = "Audio Track Incompatible",
                whatHappened = "This video's audio stream uses a format or surround codec (such as DTS, AC-3, or TrueHD) not supported by your device's platform audio decoders.",
                whatWasTried = "Platform MediaCodec audio decoders and audio output paths were evaluated.",
                suggestedActions = listOf(
                    "Switch to another audio track if available",
                    "Open video in an external player app",
                    "Copy diagnostic report"
                ),
                canSwitchToSoftware = false,
                isRecoverable = true
            )
        }

        val isUnsupportedProfileOrExceedsCapabilities = errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ||
            rawMessage.contains("hevc", ignoreCase = true) ||
            rawMessage.contains("hvc1", ignoreCase = true) ||
            rawMessage.contains("10-bit", ignoreCase = true) ||
            rawMessage.contains("L120", ignoreCase = true) ||
            rawMessage.contains("Main10", ignoreCase = true) ||
            rawMessage.contains("Hi10P", ignoreCase = true) ||
            rawMessage.contains("Hi444P", ignoreCase = true) ||
            rawMessage.contains("avc1.F", ignoreCase = true) ||
            rawMessage.contains("NO_EXCEEDS_CAPABILITIES", ignoreCase = true)

        return when {
            errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> {
                if (isUnsupportedProfileOrExceedsCapabilities) {
                    TaxonomyError(
                        code = "UNSUPPORTED_PROFILE",
                        title = "Decoder Profile Incompatible",
                        whatHappened = if (isSoftwareAlreadyAttempted) {
                            "Neither hardware nor platform software decoders on this device can process this video stream (10-bit HEVC profile or format exceeds device capabilities)."
                        } else {
                            "Your device hardware decoder could not process this video stream (10-bit HEVC profile or format exceeds decoder capabilities)."
                        },
                        whatWasTried = if (isSoftwareAlreadyAttempted) {
                            "Hardware decode and platform software fallback were both attempted and failed."
                        } else {
                            "Hardware decode and automated fallback paths were attempted."
                        },
                        suggestedActions = if (isSoftwareAlreadyAttempted) {
                            listOf(
                                "Open video in an external player app",
                                "Copy diagnostic report"
                            )
                        } else {
                            listOf(
                                "Try switching to Software Decoder",
                                "Open video in an external player app",
                                "Copy diagnostic report"
                            )
                        },
                        canSwitchToSoftware = !isSoftwareAlreadyAttempted,
                        isRecoverable = true
                    )
                } else {
                    TaxonomyError(
                        code = "DECODER_FAILURE",
                        title = "Video Decoder Failed",
                        whatHappened = if (isSoftwareAlreadyAttempted) {
                            "Both hardware and platform software decoders encountered an unrecoverable rendering or composition error on this device."
                        } else {
                            "The device video decoder encountered an unrecoverable rendering or composition error."
                        },
                        whatWasTried = if (isSoftwareAlreadyAttempted) {
                            "Platform hardware and software codec initializations were both attempted."
                        } else {
                            "Platform codec initialization was attempted."
                        },
                        suggestedActions = if (isSoftwareAlreadyAttempted) {
                            listOf(
                                "Open video in an external player app",
                                "Retry playback",
                                "Copy diagnostic report"
                            )
                        } else {
                            listOf(
                                "Try switching to Software Decoder",
                                "Open video in an external player app",
                                "Retry playback"
                            )
                        },
                        canSwitchToSoftware = !isSoftwareAlreadyAttempted,
                        isRecoverable = errorCode != PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                    )
                }
            }

            errorCode == PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> {
                TaxonomyError(
                    code = "RENDERER_RUNTIME_ERROR",
                    title = "Display Surface Lost",
                    whatHappened = "The video rendering surface was detached or reset by the system during playback.",
                    whatWasTried = "Surface composition was re-evaluated.",
                    suggestedActions = listOf(
                        "Resume playback",
                        "Open video in an external player app"
                    ),
                    canSwitchToSoftware = false,
                    isRecoverable = true
                )
            }

            errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
            errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> {
                TaxonomyError(
                    code = "UNSUPPORTED_CONTAINER",
                    title = "Unsupported Container",
                    whatHappened = "The file format or container structure is not recognized or unsupported.",
                    whatWasTried = "ISO BMFF and Matroska extractors were evaluated.",
                    suggestedActions = listOf(
                        "Verify file integrity",
                        "Open video in an external player app"
                    ),
                    canSwitchToSoftware = false,
                    isRecoverable = false
                )
            }

            errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
            errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> {
                TaxonomyError(
                    code = "CORRUPT_MEDIA",
                    title = "Corrupted Media File",
                    whatHappened = "The media file contains invalid or corrupted stream packets.",
                    whatWasTried = "Media parsers attempted best-effort stream salvage.",
                    suggestedActions = listOf(
                        "Check source file for damage",
                        "Re-copy or re-download the file"
                    ),
                    canSwitchToSoftware = false,
                    isRecoverable = false
                )
            }

            errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
            errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION ||
            errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> {
                val isPermission = errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION
                TaxonomyError(
                    code = if (isPermission) "PERMISSION_LOST" else "IO_STORAGE_UNAVAILABLE",
                    title = if (isPermission) "Storage Permission Missing" else "Storage Inaccessible",
                    whatHappened = if (isPermission) {
                        "Aptura does not have permission to read this file."
                    } else {
                        "The media file could not be read. The storage may have unmounted or the file was moved."
                    },
                    whatWasTried = "Direct storage read via platform ContentResolver was attempted.",
                    suggestedActions = listOf(
                        "Grant storage permission in App Info",
                        "Rescan library or re-import the file"
                    ),
                    canSwitchToSoftware = false,
                    isRecoverable = false
                )
            }

            errorCode == PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED ||
            errorCode == PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> {
                TaxonomyError(
                    code = "DRM_PROTECTED",
                    title = "Protected Content",
                    whatHappened = "This media is protected with Digital Rights Management (DRM). Offline playback is not supported.",
                    whatWasTried = "DRM scheme interrogation.",
                    suggestedActions = listOf(
                        "Use the provider app that issued this content"
                    ),
                    canSwitchToSoftware = false,
                    isRecoverable = false
                )
            }

            else -> {
                TaxonomyError(
                    code = "UNKNOWN",
                    title = "Playback Error",
                    whatHappened = "An unexpected error occurred during playback (${exception.errorCodeName}).",
                    whatWasTried = "Standard Media3 playback pipeline was executed.",
                    suggestedActions = listOf(
                        "Retry playback",
                        "Open video in an external player app",
                        "Copy diagnostic report"
                    ),
                    canSwitchToSoftware = !isSoftwareAlreadyAttempted,
                    isRecoverable = true
                )
            }
        }
    }

    /**
     * Formats internal taxonomy codes into clean, human-readable user labels (P2-12).
     */
    fun formatTaxonomyLabel(code: String): String = when (code) {
        "UNSUPPORTED_PROFILE" -> "Unsupported Codec Profile"
        "DECODER_FAILURE" -> "Video Decoder Failure"
        "RENDERER_RUNTIME_ERROR" -> "Display Surface Error"
        "UNSUPPORTED_CONTAINER" -> "Unsupported Container"
        "CORRUPT_MEDIA" -> "Corrupted Media File"
        "PERMISSION_LOST" -> "Storage Permission Missing"
        "IO_STORAGE_UNAVAILABLE" -> "Storage Inaccessible"
        "DRM_PROTECTED" -> "DRM Protected Content"
        "AUDIO_INIT_FAILED" -> "Audio Track Incompatible"
        else -> code.replace('_', ' ').lowercase().split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
