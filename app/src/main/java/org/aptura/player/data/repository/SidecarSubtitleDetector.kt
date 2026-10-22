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
     * Normalizes a filename or title by stripping punctuation and delimiters for fuzzy matching.
     */
    fun normalizeTitle(name: String): String =
        name.lowercase()
            .replace(Regex("[._\\-\\s]+"), " ")
            .trim()

    /**
     * Resolves the underlying File for a given URI (handling file://, MediaStore content://, and SAF).
     */
    fun resolveFile(context: Context, uri: Uri): File? {
        if (uri.scheme == "file") {
            val p = uri.path
            if (!p.isNullOrBlank()) {
                val f = File(p)
                if (f.exists()) return f
            }
        } else if (uri.scheme == "content") {
            val directPath = uri.path
            if (!directPath.isNullOrBlank()) {
                val f = File(directPath)
                if (f.exists()) return f
            }

            try {
                val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val dataIdx = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DATA)
                    if (dataIdx != -1 && cursor.moveToFirst()) {
                        val path = cursor.getString(dataIdx)
                        if (!path.isNullOrBlank()) {
                            val f = File(path)
                            if (f.exists()) return f
                        }
                    }
                }
            } catch (_: Throwable) {}

            try {
                if (android.provider.DocumentsContract.isDocumentUri(context, uri)) {
                    val docId = android.provider.DocumentsContract.getDocumentId(uri)
                    if (docId.startsWith("primary:")) {
                        val relPath = docId.substringAfter("primary:")
                        val extDir = Environment.getExternalStorageDirectory()
                        val candidate = File(extDir, relPath)
                        if (candidate.exists()) return candidate
                    }
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        return cursor.getString(nameIdx)
                    }
                }
            } catch (_: Throwable) {}
        }
        return uri.lastPathSegment
    }

    /**
     * Detects sidecar subtitles for a given video file.
     */
    fun detectSidecars(
        context: Context,
        videoUri: Uri,
        videoDisplayName: String? = null
    ): List<DetectedSidecarSubtitle> {
        val results = mutableListOf<DetectedSidecarSubtitle>()

        val file = resolveFile(context, videoUri)
        val resolvedName = videoDisplayName?.takeIf { it.isNotBlank() }
            ?: file?.name
            ?: resolveDisplayName(context, videoUri)
            ?: ""

        val baseName = resolvedName.substringBeforeLast(".")
        if (baseName.isBlank()) return emptyList()

        val candidateDirs = mutableListOf<File>()
        val parentDir = file?.parentFile
        if (parentDir != null && parentDir.isDirectory) {
            candidateDirs.add(parentDir)
            val subDirs = listOf(
                File(parentDir, "subs"),
                File(parentDir, "subtitles"),
                File(parentDir, "Subs"),
                File(parentDir, "Subtitles")
            ).filter { it.isDirectory }
            candidateDirs.addAll(subDirs)
        }

        // Check common shared directories if parent is unknown or to search downloads
        val commonDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ).filter { it.isDirectory }

        for (cd in commonDirs) {
            if (candidateDirs.none { try { it.canonicalPath == cd.canonicalPath } catch (_: Throwable) { it.path == cd.path } }) {
                candidateDirs.add(cd)
                val subDirs = listOf(File(cd, "subs"), File(cd, "subtitles")).filter { it.isDirectory }
                candidateDirs.addAll(subDirs)
            }
        }

        val distinctDirs = candidateDirs.distinctBy {
            try { it.canonicalPath } catch (_: Throwable) { it.absolutePath }
        }

        for (dir in distinctDirs) {
            results.addAll(scanDirectoryForBaseName(context, dir, baseName))
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
        val normBase = normalizeTitle(baseName)

        for (f in files) {
            if (!f.isFile) continue
            val ext = f.extension.lowercase()
            if (!SUPPORTED_EXTENSIONS.contains(ext)) continue

            val nameWithoutExt = f.nameWithoutExtension
            val lowerName = nameWithoutExt.lowercase()
            val normName = normalizeTitle(nameWithoutExt)

            val isExact = lowerName == lowerBase || normName == normBase
            val hasPrefix = lowerName.startsWith(lowerBase) || normName.startsWith(normBase)

            if (isExact || hasPrefix) {
                val validation = SubtitleFileValidator.validate(context, Uri.fromFile(f))
                if (validation is SubtitleValidationResult.Valid) {
                    val langPart = if (!isExact) {
                        if (lowerName.startsWith(lowerBase)) {
                            lowerName.removePrefix(lowerBase).trim('.', '_', '-', ' ')
                        } else {
                            normName.removePrefix(normBase).trim('.', '_', '-', ' ')
                        }
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
