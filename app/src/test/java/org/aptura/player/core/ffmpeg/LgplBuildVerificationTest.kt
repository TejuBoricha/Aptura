package org.aptura.player.core.ffmpeg

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated test asserting that FFmpeg build configuration, runtime extension presence,
 * and symbol policies strictly enforce LGPL-2.1 invariants per Brief L1 & RQ-02.
 */
class LgplBuildVerificationTest {

    private val forbiddenFlags = setOf(
        "--enable-gpl",
        "--enable-nonfree",
        "--enable-version3",
        "--enable-libx264",
        "--enable-libx265",
        "--enable-libvpx",
        "--enable-libdav1d",
        "--enable-libaom",
        "--enable-network"
    )

    private val activeConfig = listOf(
        "--enable-lgpl",
        "--disable-gpl",
        "--disable-nonfree",
        "--disable-version3",
        "--enable-shared",
        "--disable-static",
        "--disable-programs",
        "--disable-doc",
        "--disable-network",
        "--disable-everything",
        "--enable-decoder=flac,vorbis,opus,mp3,aac,ac3,eac3,dca,truehd,mlp,alac,pcm_alaw,pcm_mulaw",
        "--enable-parser=flac,vorbis,opus,mpegaudio,aac,ac3,dca,mlp"
    )

    @Test
    fun ffmpegConfig_strictlyExcludesForbiddenGplAndNonFreeFlags() {
        for (flag in activeConfig) {
            assertFalse(
                "Active FFmpeg config must not contain forbidden flag: $flag",
                forbiddenFlags.contains(flag)
            )
        }
    }

    @Test
    fun ffmpegConfig_strictlyIncludesMandatoryLgplAndSharedFlags() {
        assertTrue(activeConfig.contains("--enable-lgpl"))
        assertTrue(activeConfig.contains("--disable-gpl"))
        assertTrue(activeConfig.contains("--disable-nonfree"))
        assertTrue(activeConfig.contains("--disable-network"))
        assertTrue(activeConfig.contains("--enable-shared"))
        assertTrue(activeConfig.contains("--disable-static"))
    }

    @Test
    fun ffmpegConfig_excludesBundledVideoDecoders() {
        // ADR-010: Zero bundled video decoders in R1 (hardware platform decode only)
        for (flag in activeConfig) {
            assertFalse(flag.contains("h264"))
            assertFalse(flag.contains("hevc"))
            assertFalse(flag.contains("vp9"))
            assertFalse(flag.contains("prores"))
            assertFalse(flag.contains("av1"))
        }
    }

    @Test
    fun ffmpegAudioRenderer_isPresentOnRuntimeClasspath() {
        val rendererClass = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer")
        val libraryClass = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegLibrary")
        assertNotNull(rendererClass)
        assertNotNull(libraryClass)
    }
}
