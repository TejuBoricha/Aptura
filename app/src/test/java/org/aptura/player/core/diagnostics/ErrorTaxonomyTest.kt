@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.core.diagnostics

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorTaxonomyTest {

    @Test
    fun mapException_hevc10BitDecodingFailed_mapsToUnsupportedProfile() {
        val rawMessage = "MediaCodecVideoRenderer error, index=0, format=Format(1, null, null, video/hevc, hvc1.4.10.L120.9E.08, -1, und, [1920, 1080, -1.0, null], [-1, -1]), format_supported=YES"
        val exception = PlaybackException(rawMessage, null, PlaybackException.ERROR_CODE_DECODING_FAILED)

        val taxonomy = ErrorTaxonomy.mapException(exception)

        assertEquals("UNSUPPORTED_PROFILE", taxonomy.code)
        assertEquals("Decoder Profile Incompatible", taxonomy.title)
        assertTrue(taxonomy.canSwitchToSoftware)
        assertTrue(taxonomy.isRecoverable)
        assertTrue(taxonomy.whatHappened.contains("10-bit HEVC profile"))
        assertFalse("Must never leak raw Format string", taxonomy.whatHappened.contains("format=Format"))
        assertFalse("Must never leak raw renderer name", taxonomy.whatHappened.contains("MediaCodecVideoRenderer"))
    }

    @Test
    fun mapException_decoderInitFailure_mapsToDecoderFailureAndNotRecoverable() {
        val exception = PlaybackException("Decoder initialization failed", null, PlaybackException.ERROR_CODE_DECODER_INIT_FAILED)
        val taxonomy = ErrorTaxonomy.mapException(exception)

        assertEquals("DECODER_FAILURE", taxonomy.code)
        assertTrue(taxonomy.canSwitchToSoftware)
        assertFalse(taxonomy.isRecoverable)
    }

    @Test
    fun mapException_decodingFailed_isRecoverable() {
        val exception = PlaybackException("Decoding frame failed", null, PlaybackException.ERROR_CODE_DECODING_FAILED)
        val taxonomy = ErrorTaxonomy.mapException(exception)

        assertEquals("DECODER_FAILURE", taxonomy.code)
        assertTrue(taxonomy.canSwitchToSoftware)
        assertTrue(taxonomy.isRecoverable)
    }

    @Test
    fun mapException_unsupportedContainer_mapsToUnsupportedContainer() {
        val exception = PlaybackException("No extractor found", null, PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)
        val taxonomy = ErrorTaxonomy.mapException(exception)

        assertEquals("UNSUPPORTED_CONTAINER", taxonomy.code)
        assertFalse(taxonomy.canSwitchToSoftware)
        assertFalse(taxonomy.isRecoverable)
    }

    @Test
    fun mapException_corruptMedia_mapsToCorruptMedia() {
        val exception = PlaybackException("Corrupt data", null, PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)
        val taxonomy = ErrorTaxonomy.mapException(exception)

        assertEquals("CORRUPT_MEDIA", taxonomy.code)
        assertFalse(taxonomy.isRecoverable)
    }

    @Test
    fun mapException_permissionLost_mapsToPermissionLost() {
        val exception = PlaybackException("Permission denied", null, PlaybackException.ERROR_CODE_IO_NO_PERMISSION)
        val taxonomy = ErrorTaxonomy.mapException(exception)

        assertEquals("PERMISSION_LOST", taxonomy.code)
        assertFalse(taxonomy.isRecoverable)
    }

    @Test
    fun mapException_drmProtected_mapsToDrmProtected() {
        val exception = PlaybackException("DRM unsupported", null, PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED)
        val taxonomy = ErrorTaxonomy.mapException(exception)

        assertEquals("DRM_PROTECTED", taxonomy.code)
        assertFalse(taxonomy.isRecoverable)
    }
}
