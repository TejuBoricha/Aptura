package org.aptura.player.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayTitleTest {

    @Test
    fun cleanDisplayTitle_stripsCommonVideoExtensions() {
        assertEquals("dts only", cleanDisplayTitle("dts_only.mkv"))
        assertEquals("sample", cleanDisplayTitle("sample.mp4"))
        assertEquals("sample", cleanDisplayTitle("sample.webm"))
        assertEquals("sample", cleanDisplayTitle("sample.avi"))
        assertEquals("sample", cleanDisplayTitle("sample.mov"))
        assertEquals("sample", cleanDisplayTitle("sample.ts"))
        assertEquals("sample", cleanDisplayTitle("sample.m4v"))
        assertEquals("sample", cleanDisplayTitle("sample.3gp"))
        assertEquals("sample", cleanDisplayTitle("sample.flv"))
        assertEquals("sample", cleanDisplayTitle("sample.wmv"))
        assertEquals("sample", cleanDisplayTitle("sample.m2ts"))
    }

    @Test
    fun cleanDisplayTitle_convertsUnderscoresToSpaces() {
        assertEquals("my vacation video", cleanDisplayTitle("my_vacation_video.mp4"))
        assertEquals("audio 5.1 surround", cleanDisplayTitle("audio_5.1_surround.mkv"))
    }

    @Test
    fun cleanDisplayTitle_convertsDotsInDotSeparatedTitles() {
        assertEquals("Show S01E01", cleanDisplayTitle("Show.S01E01.mkv"))
        assertEquals("Breaking Bad S01E03 1080p", cleanDisplayTitle("Breaking.Bad.S01E03.1080p.mkv"))
        assertEquals("Movie Name 2024", cleanDisplayTitle("Movie.Name.2024.mp4"))
    }

    @Test
    fun cleanDisplayTitle_preservesVersionAndDecimalNumbers() {
        assertEquals("Aptura Player v1.2", cleanDisplayTitle("Aptura Player v1.2.mp4"))
        assertEquals("Surround 5.1 test", cleanDisplayTitle("Surround 5.1 test.mkv"))
    }

    @Test
    fun cleanDisplayTitle_handlesBlankAndEdgeCases() {
        assertEquals("Untitled", cleanDisplayTitle(""))
        assertEquals("Untitled", cleanDisplayTitle("   "))
        assertEquals("Video", cleanDisplayTitle("Video"))
        assertEquals("Multiple Spaces", cleanDisplayTitle("Multiple    _   Spaces.mkv"))
    }
}
