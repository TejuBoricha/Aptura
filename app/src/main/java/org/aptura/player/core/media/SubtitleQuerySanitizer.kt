package org.aptura.player.core.media

import java.net.URLEncoder

/**
 * Domain data model representing a sanitized movie or TV show title.
 */
data class CleanedMediaInfo(
    val searchQuery: String,
    val displayTitle: String,
    val year: String?,
    val episodeMarker: String?,
    val isTvShow: Boolean
)

/**
 * Sanitizes messy raw filenames (from scene releases, torrents, or camera rips)
 * into high-precision search queries suitable for subtitle search engines (Subdl, OpenSubtitles).
 *
 * Example:
 *  "Oppenheimer.2023.IMAX.2160p.UHD.HDR.TrueHD.7.1.Atmos.x265-FLUX.mkv"
 *   -> CleanedMediaInfo(searchQuery = "Oppenheimer 2023", displayTitle = "Oppenheimer", year = "2023", ...)
 *
 *  "Breaking.Bad.S01E03.Long.Long.Time.1080p.mkv"
 *   -> CleanedMediaInfo(searchQuery = "Breaking Bad S01E03", displayTitle = "Breaking Bad", episodeMarker = "S01E03", isTvShow = true)
 */
object SubtitleQuerySanitizer {

    private val SCENE_TAGS = listOf(
        // Resolutions & Standards
        "2160p", "1080p", "1080i", "720p", "576p", "480p", "360p", "4k", "uhd", "fhd", "hd", "sd",
        // Video Codecs & Formats
        "hevc", "x265", "x264", "h265", "h264", "av1", "xvid", "divx", "10bit", "8bit", "hdr10\\+", "hdr10", "hdr",
        "dolby", "vision", "dv", "imax",
        // Sources & Release Types
        "bluray", "blu-ray", "bdrip", "brrip", "web-dl", "webdl", "webrip", "web", "hdrip", "dvdrip", "dvd", "remux",
        "screener", "scr", "cam", "camrip", "ts", "telesync", "tc", "r5", "vdr",
        // Audio Codecs & Channels
        "truehd", "atmos", "dts-hd", "dtshd", "dts", "ddp5\\.1", "dd5\\.1", "ddp", "ac3", "aac", "flac", "eac3", "mp3",
        "7\\.1", "5\\.1", "2\\.0", "dual", "multi", "audio",
        // Release Bloat & Suffixes
        "extended", "unrated", "remastered", "repack", "proper", "internal", "complete", "sub", "esubs", "subs", "vostfr",
        "ita", "fra", "ger", "spa", "rus", "hindi", "kor", "jap", "nl"
    )

    private val SCENE_REGEX = Regex("(?i)\\b(${SCENE_TAGS.joinToString("|")})\\b")
    private val BRACKET_CONTENT_REGEX = Regex("\\[.*?\\]|\\(.*?\\)|\\{.*?\\}")
    private val RELEASE_GROUP_REGEX = Regex("-[A-Za-z0-9_]+$")
    private val TV_EPISODE_REGEX = Regex("(?i)\\b(S\\d{1,2}E\\d{1,2}|\\d{1,2}x\\d{1,2})\\b")
    private val YEAR_REGEX = Regex("\\b(19\\d{2}|20\\d{2})\\b")
    private val SEPARATORS_REGEX = Regex("[._+]|\\s+-\\s+|\\s+")

    fun sanitize(rawFilename: String): CleanedMediaInfo {
        if (rawFilename.isBlank()) {
            return CleanedMediaInfo("", "", null, null, false)
        }

        // 1. Strip file extension if present (e.g. .mkv, .mp4)
        val withoutExt = if (rawFilename.contains(".")) {
            rawFilename.substringBeforeLast(".")
        } else {
            rawFilename
        }

        // 2. Extract TV Show pattern before bracket stripping (e.g. S01E03 or 1x03)
        val tvMatch = TV_EPISODE_REGEX.find(withoutExt)
        val episodeMarker = tvMatch?.value?.uppercase()

        // 3. Remove text inside brackets [RARBG], (2020), {YTS}
        // Exception: keep year if inside parentheses
        var working = withoutExt
        val yearInBrackets = YEAR_REGEX.find(working)?.value
        working = working.replace(BRACKET_CONTENT_REGEX, " ")

        // 4. Strip scene tags and trailing release groups (-FLUX, -EVO)
        working = working.replace(SCENE_REGEX, " ")
        working = working.replace(RELEASE_GROUP_REGEX, "")

        // 5. Replace dots, underscores, dashes with clean whitespace
        working = working.replace(SEPARATORS_REGEX, " ").trim()

        // 6. Find year (either from stripped text or original)
        val year = YEAR_REGEX.find(working)?.value ?: yearInBrackets

        val cleanTitle: String
        val searchQuery: String

        if (episodeMarker != null) {
            // TV Show: Extract text prior to the episode marker
            val beforeEpisode = withoutExt.substringBefore(tvMatch.value)
                .replace(BRACKET_CONTENT_REGEX, " ")
                .replace(SCENE_REGEX, " ")
                .replace(SEPARATORS_REGEX, " ")
                .trim()

            cleanTitle = beforeEpisode.ifBlank { "TV Show" }
            searchQuery = "$cleanTitle $episodeMarker"
        } else {
            // Movie: If year is found, title is everything before or including the year
            if (year != null && working.contains(year)) {
                val titlePart = working.substringBefore(year)
                    .replace(SEPARATORS_REGEX, " ")
                    .trim()
                cleanTitle = titlePart.ifBlank { working }
                searchQuery = "$cleanTitle $year".trim()
            } else {
                cleanTitle = working
                searchQuery = if (year != null) "$cleanTitle $year".trim() else cleanTitle
            }
        }

        val normalizedQuery = searchQuery.replace(Regex("\\s+"), " ").trim()
        val normalizedTitle = cleanTitle.replace(Regex("\\s+"), " ").trim()

        return CleanedMediaInfo(
            searchQuery = normalizedQuery,
            displayTitle = normalizedTitle,
            year = year,
            episodeMarker = episodeMarker,
            isTvShow = episodeMarker != null
        )
    }

    /**
     * Builds URL for Subdl subtitle search.
     */
    fun buildSubdlUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return "https://subdl.com/search/$encoded"
    }

    /**
     * Builds URL for OpenSubtitles web search.
     */
    fun buildOpenSubtitlesUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return "https://www.opensubtitles.org/search/sublanguageid-all/moviename-$encoded"
    }

    /**
     * Builds universal Google subtitle query URL.
     */
    fun buildGoogleSearchUrl(query: String): String {
        val encoded = URLEncoder.encode("$query subtitles srt", "UTF-8")
        return "https://www.google.com/search?q=$encoded"
    }

    /**
     * Helper to directly extract the cleaned search query string.
     */
    fun sanitizeToSearchQuery(rawFilename: String): String = sanitize(rawFilename).searchQuery
}
