package org.aptura.player.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleQuerySanitizerTest {

    @Test
    fun sanitize_complexMovieWithAllSceneTags_returnsCleanTitleAndYear() {
        val raw = "Oppenheimer.2023.IMAX.2160p.UHD.HDR.TrueHD.7.1.Atmos.x265-FLUX.mkv"
        val result = SubtitleQuerySanitizer.sanitize(raw)

        assertEquals("Oppenheimer 2023", result.searchQuery)
        assertEquals("Oppenheimer", result.displayTitle)
        assertEquals("2023", result.year)
        assertFalse(result.isTvShow)
    }

    @Test
    fun sanitize_tvShowWithSeasonEpisode_returnsShowAndEpisodeMarker() {
        val raw = "Breaking.Bad.S01E03.Long.Long.Time.1080p.WEB-DL.DDP5.1.H.264-FLUX.mkv"
        val result = SubtitleQuerySanitizer.sanitize(raw)

        assertEquals("Breaking Bad S01E03", result.searchQuery)
        assertEquals("Breaking Bad", result.displayTitle)
        assertEquals("S01E03", result.episodeMarker)
        assertTrue(result.isTvShow)
    }

    @Test
    fun sanitize_tvShowWithNumberXFormat_returnsNormalizedEpisode() {
        val raw = "Lost.1x04.Walkabout.720p.HDTV.x264.mkv"
        val result = SubtitleQuerySanitizer.sanitize(raw)

        assertEquals("Lost 1X04", result.searchQuery)
        assertEquals("Lost", result.displayTitle)
        assertEquals("1X04", result.episodeMarker)
        assertTrue(result.isTvShow)
    }

    @Test
    fun sanitize_ytsBracketsFormat_stripsBracketsAndKeepsTitleYear() {
        val raw = "[YTS.MX] Dune Part Two (2024) [1080p] [WEBRip] [5.1] [YIFY].mp4"
        val result = SubtitleQuerySanitizer.sanitize(raw)

        assertEquals("Dune Part Two 2024", result.searchQuery)
        assertEquals("Dune Part Two", result.displayTitle)
        assertEquals("2024", result.year)
        assertFalse(result.isTvShow)
    }

    @Test
    fun sanitize_movieWithHyphenInTitle_preservesHyphen() {
        val raw = "Spider-Man.No.Way.Home.2021.REPACK.1080p.BluRay.x264-SPARKS.mkv"
        val result = SubtitleQuerySanitizer.sanitize(raw)

        assertEquals("Spider-Man No Way Home 2021", result.searchQuery)
        assertEquals("2021", result.year)
    }

    @Test
    fun sanitize_simpleTitleWithoutTags_preservesTitle() {
        val raw = "Family Vacation.mp4"
        val result = SubtitleQuerySanitizer.sanitize(raw)

        assertEquals("Family Vacation", result.searchQuery)
        assertEquals("Family Vacation", result.displayTitle)
    }

    @Test
    fun buildSubdlUrl_encodesQueryProperly() {
        val url = SubtitleQuerySanitizer.buildSubdlUrl("Dune Part Two 2024")
        assertEquals("https://subdl.com/search/Dune+Part+Two+2024", url)
    }

    @Test
    fun buildOpenSubtitlesUrl_encodesQueryProperly() {
        val url = SubtitleQuerySanitizer.buildOpenSubtitlesUrl("Inception 2010")
        assertEquals("https://www.opensubtitles.org/search/sublanguageid-all/moviename-Inception+2010", url)
    }

    @Test
    fun buildGoogleSearchUrl_appendsSubtitlesKeywords() {
        val url = SubtitleQuerySanitizer.buildGoogleSearchUrl("Oppenheimer 2023")
        assertEquals("https://www.google.com/search?q=Oppenheimer+2023+subtitles+srt", url)
    }
}
