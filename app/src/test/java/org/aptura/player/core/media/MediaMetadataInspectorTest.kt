package org.aptura.player.core.media

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaExtractor
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaMetadataInspectorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        ShadowMediaExtractor.reset()
    }

    @Test
    fun inspect_corruptGarbageBytesFile_returnsUninspectedGracefullyWithoutThrowing() {
        val corruptFile = File(context.cacheDir, "corrupt_video.mp4").apply {
            FileOutputStream(this).use { fos ->
                fos.write(byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88.toByte()))
            }
        }
        val uri = Uri.fromFile(corruptFile)

        val metadata = MediaMetadataInspector.inspect(context, uri)

        assertFalse("Corrupt file must not be reported as inspected", metadata.inspected)
        assertNull(metadata.videoCodec)
        assertNull(metadata.videoBitrate)
        assertEquals(0L, metadata.durationMs)
        assertTrue("Audio tracks must be empty for corrupt file", metadata.audioTracks.isEmpty())
        assertTrue("Subtitle tracks must be empty for corrupt file", metadata.subtitleTracks.isEmpty())
        assertFalse(metadata.isHdr)
    }

    @Test
    fun inspect_emptyZeroByteFile_returnsUninspectedGracefullyWithoutThrowing() {
        val emptyFile = File(context.cacheDir, "empty_video.mp4").apply {
            createNewFile()
        }
        val uri = Uri.fromFile(emptyFile)

        val metadata = MediaMetadataInspector.inspect(context, uri)

        assertFalse("Empty file must not be reported as inspected", metadata.inspected)
        assertNull(metadata.videoCodec)
        assertEquals(0L, metadata.durationMs)
        assertTrue(metadata.audioTracks.isEmpty())
        assertTrue(metadata.subtitleTracks.isEmpty())
    }

    @Test
    fun inspect_nonexistentUri_returnsUninspectedGracefullyWithoutThrowing() {
        val nonexistentUri = Uri.parse("content://org.aptura.nonexistent.provider/missing.mp4")

        val metadata = MediaMetadataInspector.inspect(context, nonexistentUri)

        assertFalse("Nonexistent URI must not be reported as inspected", metadata.inspected)
        assertNull(metadata.videoCodec)
        assertEquals(0L, metadata.durationMs)
        assertTrue(metadata.audioTracks.isEmpty())
        assertTrue(metadata.subtitleTracks.isEmpty())
    }

    @Test
    fun inspect_plainTextDisguisedAsVideo_returnsUninspectedGracefullyWithoutThrowing() {
        val fakeVideo = File(context.cacheDir, "fake_movie.mkv").apply {
            writeText("This is plain text and not an actual Matroska or MP4 container.")
        }
        val uri = Uri.fromFile(fakeVideo)

        val metadata = MediaMetadataInspector.inspect(context, uri)

        assertFalse("Non-media text file must not be reported as inspected", metadata.inspected)
        assertNull(metadata.videoCodec)
        assertEquals(0L, metadata.durationMs)
        assertTrue(metadata.audioTracks.isEmpty())
        assertTrue(metadata.subtitleTracks.isEmpty())
    }

    @Test
    fun inspect_simulatedRetrieverException_doesNotCrashAndReleasesResources() {
        val file = File(context.cacheDir, "throwing_file.mp4").apply {
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        }
        val uri = Uri.fromFile(file)

        ShadowMediaMetadataRetriever.addException(
            org.robolectric.shadows.util.DataSource.toDataSource(context, uri),
            RuntimeException("Native MediaMetadataRetriever failed to open container")
        )

        val metadata = MediaMetadataInspector.inspect(context, uri)

        assertFalse("Failure during extraction must return uninspected", metadata.inspected)
        assertNull(metadata.videoCodec)
        assertEquals(0L, metadata.durationMs)
        assertTrue(metadata.audioTracks.isEmpty())
        assertTrue(metadata.subtitleTracks.isEmpty())
    }

    @Test
    fun inspect_validMediaWithTracks_reportsInspectedTrueAndExtractsCodecs() {
        val file = File(context.cacheDir, "valid_video.mp4").apply {
            writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x20))
        }
        val uri = Uri.fromFile(file)
        val dataSource = org.robolectric.shadows.util.DataSource.toDataSource(context, uri)

        val videoFormat = android.media.MediaFormat.createVideoFormat("video/avc", 1920, 1080).apply {
            setInteger(android.media.MediaFormat.KEY_BIT_RATE, 5_000_000)
            setInteger(android.media.MediaFormat.KEY_COLOR_TRANSFER, android.media.MediaFormat.COLOR_TRANSFER_ST2084)
            setInteger(android.media.MediaFormat.KEY_COLOR_STANDARD, android.media.MediaFormat.COLOR_STANDARD_BT2020)
        }
        val audioFormat = android.media.MediaFormat.createAudioFormat("audio/mp4a-latm", 48000, 2).apply {
            setInteger(android.media.MediaFormat.KEY_BIT_RATE, 256_000)
            setString(android.media.MediaFormat.KEY_LANGUAGE, "eng")
        }

        org.robolectric.shadows.ShadowMediaExtractor.addTrack(dataSource, videoFormat, ByteArray(10))
        org.robolectric.shadows.ShadowMediaExtractor.addTrack(dataSource, audioFormat, ByteArray(10))

        val metadata = MediaMetadataInspector.inspect(context, uri)

        assertTrue("Valid media container with tracks must report inspected = true", metadata.inspected)
        assertEquals("H.264 / AVC", metadata.videoCodec)
        assertEquals(5_000_000L, metadata.videoBitrate)
        assertTrue("HDR flag must be detected from color transfer", metadata.isHdr)
        assertEquals("HDR10 / PQ (ST 2084)", metadata.colorTransfer)
        assertEquals("BT.2020 (Wide Gamut)", metadata.colorStandard)
        assertEquals(1, metadata.audioTracks.size)
        assertEquals("AAC", metadata.audioTracks[0].codec)
        assertEquals("eng", metadata.audioTracks[0].language)
    }
}
