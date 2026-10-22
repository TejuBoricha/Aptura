package org.aptura.player.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import org.aptura.player.core.media.SubtitleFileValidator
import org.aptura.player.core.media.SubtitleValidationResult
import java.io.File
import java.util.Locale

/**
 * Data representation of a discovered sidecar subtitle file.
 */
data class DetectedSidecarSubtitle(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val languageCode: String?,
    val languageLabel: String,
    val isDefault: Boolean = false
)

/**
 * Probes the local storage context for sidecar subtitle files (.srt, .vtt, .ass, .ssa, .sub)
 * sharing the same base name, language tags, or located in child /subs/ directories.
 * Also scans the Downloads directory for recently downloaded subtitles matching the active title.
 */
object SidecarSubtitleDetector {

    private val SUPPORTED_EXTENSIONS = setOf("srt", "vtt", "ass", "ssa", "sub")

    private val LANGUAGE_MAP = mapOf(
        "en" to "English", "eng" to "English",
        "es" to "Spanish", "spa" to "Spanish",
        "fr" to "French", "fra" to "French", "fre" to "French",
        "de" to "German", "deu" to "German", "ger" to "German",
        "hi" to "Hindi", "hin" to "Hindi",
        "it" to "Italian", "ita" to "Italian",
        "ja" to "Japanese", "jpn" to "Japanese",
        "ko" to "Korean", "kor" to "Korean",
        "pt" to "Portuguese", "por" to "Portuguese",
        "ru" to "Russian", "rus" to "Russian",
        "zh" to "Chinese", "zho" to "Chinese", "chi" to "Chinese",
        "ar" to "Arabic", "ara" to "Arabic",
        "tr" to "Turkish", "tur" to "Turkish",
        "nl" to "Dutch", "nld" to "Dutch",
        "pl" to "Polish", "pol" to "Polish",
        "sv" to "Swedish", "swe" to "Swedish"
    )

    /**
     * Detects sidecar subtitles for a given video file.
     */
    fun detectSidecars(
        context: Context,
        videoUri: Uri,
        videoDisplayName: String? = null
    ): List<DetectedSidecarSubtitle> {
        val results = mutableListOf<DetectedSidecarSubtitle>()

        // 1. Resolve parent directory if direct file path is accessible
        val file = if (videoUri.scheme == "file") {
            File(videoUri.path ?: "")
        } else {
            // Check if path is encoded in URI or DATA
            val path = videoUri.path
            if (path != null && File(path).exists()) File(path) else null
        }

        val parentDir = file?.parentFile
        val baseName = (videoDisplayName ?: file?.name ?: "").substringBeforeLast(".")

        if (parentDir != null && parentDir.isDirectory && baseName.isNotBlank()) {
            results.addAll(scanDirectoryForBaseName(context, parentDir, baseName))

            // Also check subdirectories: subs/, subtitles/, Subs/, Subtitles/
            val subDirs = listOf(
                File(parentDir, "subs"),
                File(parentDir, "subtitles"),
                File(parentDir, "Subs"),
                File(parentDir, "Subtitles")
            ).filter { it.isDirectory }
             .distinctBy { try { it.canonicalPath } catch (_: Throwable) { it.absolutePath } }

            for (subDir in subDirs) {
                results.addAll(scanDirectoryForBaseName(context, subDir, baseName))
            }
        }

        return results.distinctBy {
            try {
                val p = it.uri.path
                if (p != null) File(p).canonicalPath else it.uri.toString().lowercase()
            } catch (_: Throwable) {
                it.uri.toString().lowercase()
            }
        }
    }

    private fun scanDirectoryForBaseName(
        context: Context,
        dir: File,
        baseName: String
    ): List<DetectedSidecarSubtitle> {
        val candidates = mutableListOf<DetectedSidecarSubtitle>()
        val files = dir.listFiles() ?: return emptyList()

        val lowerBase = baseName.lowercase()

        for (f in files) {
            if (!f.isFile) continue
            val ext = f.extension.lowercase()
            if (!SUPPORTED_EXTENSIONS.contains(ext)) continue

            val nameWithoutExt = f.nameWithoutExtension
            val lowerName = nameWithoutExt.lowercase()

            // Match condition 1: Exact match "movie.srt"
            val isExact = lowerName == lowerBase
            // Match condition 2: Language suffix "movie.en.srt", "movie.eng.srt", "movie_es.srt"
            val hasPrefix = lowerName.startsWith(lowerBase)

            if (isExact || hasPrefix) {
                val validation = SubtitleFileValidator.validate(context, Uri.fromFile(f))
                if (validation is SubtitleValidationResult.Valid) {
                    val langPart = if (!isExact) {
                        lowerName.removePrefix(lowerBase)
                            .trim('.', '_', '-', ' ')
                    } else null

                    val langCode = langPart?.takeIf { it.isNotBlank() }
                    val label = resolveLanguageLabel(langCode, isExact)

                    candidates.add(
                        DetectedSidecarSubtitle(
                            uri = validation.validatedUri ?: Uri.fromFile(f),
                            displayName = f.name,
                            mimeType = validation.mimeType,
                            languageCode = langCode,
                            languageLabel = label,
                            isDefault = isExact || langCode == "en" || langCode == "eng"
                        )
                    )
                }
            }
        }

        return candidates
    }

    /**
     * Scans the system Downloads folder for recent subtitle files (.srt, .vtt, .zip)
     * modified within the last 30 minutes matching any keywords of the movie query.
     */
    fun findRecentDownloads(
        context: Context,
        searchQuery: String
    ): List<DetectedSidecarSubtitle> {
        if (searchQuery.isBlank()) return emptyList()

        val downloadDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        )

        val queryKeywords = searchQuery.lowercase()
            .split(Regex("\\s+"))
            .filter { it.length > 2 }

        val thresholdTime = System.currentTimeMillis() - (30 * 60 * 1000L) // 30 minutes
        val matches = mutableListOf<DetectedSidecarSubtitle>()

        for (dir in downloadDirs) {
            if (!dir.exists() || !dir.isDirectory) continue
            val files = dir.listFiles() ?: continue

            for (f in files) {
                if (!f.isFile) continue
                if (f.lastModified() < thresholdTime) continue

                val ext = f.extension.lowercase()
                val isSubtitleExt = SUPPORTED_EXTENSIONS.contains(ext) || ext == "zip"
                if (!isSubtitleExt) continue

                val lowerName = f.name.lowercase()
                val keywordMatch = queryKeywords.any { kw -> lowerName.contains(kw) }

                if (keywordMatch) {
                    val validation = SubtitleFileValidator.validate(context, Uri.fromFile(f))
                    if (validation is SubtitleValidationResult.Valid) {
                        matches.add(
                            DetectedSidecarSubtitle(
                                uri = validation.validatedUri ?: Uri.fromFile(f),
                                displayName = validation.displayName,
                                mimeType = validation.mimeType,
                                languageCode = null,
                                languageLabel = "Downloaded Subtitle",
                                isDefault = true
                            )
                        )
                    }
                }
            }
        }

        return matches.distinctBy { it.displayName }
    }

    private fun resolveLanguageLabel(code: String?, isExact: Boolean): String {
        if (code == null || isExact) return "External (Sidecar)"
        val cleanCode = code.lowercase()
        return LANGUAGE_MAP[cleanCode] ?: cleanCode.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }
}
