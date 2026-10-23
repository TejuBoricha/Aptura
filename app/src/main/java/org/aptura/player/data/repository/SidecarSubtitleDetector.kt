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
    val isDefault: Boolean = false,
    /** True when the subtitle file name equals the video name (no language/role tags). */
    val isExactMatch: Boolean = false
)

/**
 * Probes the local storage context for sidecar subtitle files (.srt, .vtt, .ass, .ssa, .sub, .ttml, .dfxp)
 * sharing the same base name, language tags, or located in child /subs/ directories.
 * Also scans the Downloads directory for recently downloaded subtitles matching the active title.
 */
object SidecarSubtitleDetector {

    private val SUPPORTED_EXTENSIONS = setOf("srt", "vtt", "ass", "ssa", "sub", "ttml", "dfxp")

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

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "mov", "avi", "ts", "m2ts", "mts", "m4v", "3gp", "3g2",
        "flv", "wmv", "ogv", "mpg", "mpeg", "vob", "divx", "f4v", "asf", "m2v", "mp2v", "mpv", "rmvb"
    )

    /** Words that may follow the video name in a subtitle file name without changing which video it belongs to. */
    private val SUBTITLE_TAGS = setOf(
        "forced", "sdh", "cc", "default", "full", "signs", "songs", "commentary", "closed", "captions"
    )

    private val LANGUAGE_NAMES = LANGUAGE_MAP.values.map { it.lowercase() }.toSet()
    private val LANGUAGE_TAG = Regex("^[a-z]{2,3}(-[a-z0-9]{2,8})?$")

    /**
     * Removes the file extension only when it really is a video extension. The previous implementation chopped
     * everything after the last dot, so the extension-less MediaStore title `Show.S01E02` became `Show` and every
     * `Show.*.srt` was attached (audit P0-10).
     */
    internal fun stripVideoExtension(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return if (ext in VIDEO_EXTENSIONS) name.substringBeforeLast('.') else name
    }

    /** How a subtitle file name relates to a video name. */
    internal sealed interface SidecarMatch {
        data object Exact : SidecarMatch
        data class Tagged(val tokens: List<String>) : SidecarMatch
    }

    /**
     * Strict sidecar rule: the subtitle name must equal the video name, or be the video name followed by a
     * separator and only language/role tags (`.en`, `.pt-BR`, `.forced`, `.en.sdh`). `Show.S01E01` therefore never
     * matches `Show.S01E02.srt` or `Show.S01E011.srt`.
     */
    internal fun matchSidecarName(videoBase: String, subtitleBase: String): SidecarMatch? {
        fun attempt(video: String, sub: String): SidecarMatch? {
            if (video.isBlank()) return null
            if (sub == video) return SidecarMatch.Exact
            if (!sub.startsWith(video)) return null
            val remainder = sub.substring(video.length)
            if (remainder.isEmpty() || remainder[0] !in "._- ") return null
            val tokens = remainder.split('.', '_', ' ').filter { it.isNotBlank() }.map { it.trim('-') }.filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return null
            val allTags = tokens.all { it in SUBTITLE_TAGS || it in LANGUAGE_NAMES || LANGUAGE_TAG.matches(it) }
            return if (allTags) SidecarMatch.Tagged(tokens) else null
        }
        val v = videoBase.lowercase()
        val sb = subtitleBase.lowercase()
        return attempt(v, sb) ?: attempt(normalizeTitle(videoBase), normalizeTitle(subtitleBase))
    }

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
        // Prefer the REAL file name. `videoDisplayName` is only a last resort: callers historically passed the
        // cleaned/extension-less title, which cannot be matched against subtitle file names reliably.
        val resolvedName = file?.name
            ?: resolveDisplayName(context, videoUri)?.takeIf { it.isNotBlank() }
            ?: videoDisplayName?.takeIf { it.isNotBlank() }
            ?: ""

        val baseName = stripVideoExtension(resolvedName)
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

        val unique = results.distinctBy {
            try {
                val p = it.uri.path
                if (p != null) File(p).canonicalPath else it.uri.toString().lowercase()
            } catch (_: Throwable) {
                it.uri.toString().lowercase()
            }
        }
        return markSingleDefault(unique)
    }

    /**
     * Exactly one track may auto-select: the exact-name match, otherwise the English one. Everything else stays
     * available in the sheet but isn't switched on behind the user's back.
     */
    private fun markSingleDefault(found: List<DetectedSidecarSubtitle>): List<DetectedSidecarSubtitle> {
        val chosen = found.firstOrNull { it.isExactMatch }
            ?: found.firstOrNull { it.languageCode == "en" || it.languageCode == "eng" }
        return found.map { it.copy(isDefault = it === chosen) }
    }

    private fun scanDirectoryForBaseName(
        context: Context,
        dir: File,
        baseName: String
    ): List<DetectedSidecarSubtitle> {
        val candidates = mutableListOf<DetectedSidecarSubtitle>()
        val files = dir.listFiles() ?: return emptyList()

        val matched = files
            .filter { it.isFile && SUPPORTED_EXTENSIONS.contains(it.extension.lowercase()) }
            .mapNotNull { f -> matchSidecarName(baseName, f.nameWithoutExtension)?.let { f to it } }
            // Plain names first (no role tags), then alphabetical, so the default pick is stable and un-forced.
            .sortedWith(compareBy({ (it.second as? SidecarMatch.Tagged)?.tokens?.size ?: 0 }, { it.first.name.lowercase() }))

        for ((f, match) in matched) {
            val validation = SubtitleFileValidator.validate(context, Uri.fromFile(f))
            if (validation !is SubtitleValidationResult.Valid) continue

            val tokens = (match as? SidecarMatch.Tagged)?.tokens.orEmpty()
            val langToken = tokens.firstOrNull { it in LANGUAGE_MAP.keys || it in LANGUAGE_NAMES }
                ?: tokens.firstOrNull { it !in SUBTITLE_TAGS && LANGUAGE_TAG.matches(it) }
            val langCode = langToken?.let { token ->
                LANGUAGE_MAP.entries.firstOrNull { (code, name) -> code == token || name.equals(token, true) }?.key
                    ?: token.substringBefore('-') // pt-BR -> pt
            }
            val roles = tokens.filter { it in SUBTITLE_TAGS && it != "default" }
            val label = buildLabel(langCode, roles)

            candidates.add(
                DetectedSidecarSubtitle(
                    uri = validation.validatedUri ?: Uri.fromFile(f),
                    displayName = f.name,
                    mimeType = validation.mimeType,
                    languageCode = langCode,
                    languageLabel = label,
                    isDefault = false,
                    isExactMatch = match is SidecarMatch.Exact
                )
            )
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

    private fun buildLabel(code: String?, roles: List<String>): String {
        val language = code?.let { c ->
            LANGUAGE_MAP[c.lowercase()]
                ?: Locale.forLanguageTag(c).getDisplayLanguage(Locale.ENGLISH).takeIf { it.isNotBlank() }
                ?: c.uppercase(Locale.ROOT)
        }
        val roleText = roles.joinToString(" · ") { r ->
            when (r) {
                "sdh", "cc", "closed", "captions" -> "SDH"
                else -> r.replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
        }.let { text -> text.split(" · ").distinct().joinToString(" · ") }
        return listOfNotNull(language, roleText.takeIf { it.isNotBlank() })
            .joinToString(" · ")
            .ifBlank { "External (Sidecar)" }
    }
}
