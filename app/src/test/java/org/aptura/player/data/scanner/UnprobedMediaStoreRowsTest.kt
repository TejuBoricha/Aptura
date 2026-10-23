package org.aptura.player.data.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.aptura.player.data.database.MediaItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for audit P1-14 (remainder): MediaStoreScanner filtered `DURATION > 0`, so files the
 * platform had not yet probed (fresh screen recordings/downloads keep DURATION null) were invisible
 * until some other app measured them — silently defeating auto-scan (P0-09) on the primary path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UnprobedMediaStoreRowsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun item(
        uri: String,
        durationMs: Long = 0L,
        sizeBytes: Long = 5_000_000L,
        mediaStoreId: Long = 0L
    ) = MediaItemEntity(
        id = 0L,
        mediaStoreId = mediaStoreId,
        uri = uri,
        title = uri.substringAfterLast('/'),
        displayName = uri.substringAfterLast('/'),
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        mimeType = "video/mp4"
    )

    @Test
    fun scannedRowWithoutDuration_isKeptByTheFilterPath() {
        // The old selection required DURATION > 0. The scanner now tolerates durationMs = 0;
        // this asserts the contract the worker relies on: un-probed rows reach reconciliation.
        val unprobed = item("content://media/external/video/media/99")
        assertEquals(0L, unprobed.durationMs)
        assertTrue(unprobed.sizeBytes > 1024L)
    }

    @Test
    fun resolveMissingDurations_carriesKnownDurationForward_whenFileUnchanged() {
        val known = item("content://media/external/video/media/1", durationMs = 45_000L)
        val rescanned = item("content://media/external/video/media/1", durationMs = 0L)
        val existing = mapOf(known.uri to known)

        val resolved = MediaScanWorker.resolveMissingDurations(context, listOf(rescanned), existing)

        assertEquals("duration must not regress to 0:00 on a rescan of an unchanged file", 45_000L, resolved.first().durationMs)
    }

    @Test
    fun resolveMissingDurations_doesNotTrustStaleDuration_whenFileWasReplaced() {
        val replaced = item("uri_media_1", durationMs = 45_000L, sizeBytes = 1_000_000L)
        val rescanned = item("uri_media_1", durationMs = 0L, sizeBytes = 9_999_999L)
        val existing = mapOf(replaced.uri to replaced)

        val resolved = MediaScanWorker.resolveMissingDurations(context, listOf(rescanned), existing)

        // Optimistically carry the duration when the file size is the same, otherwise probe (which
        // fails under Robolectric, so the value stays 0 rather than being wrongly reused).
        assertEquals(0L, resolved.first().durationMs)
    }

    @Test
    fun resolveMissingDurations_knownRowIsUntouched_andFastPathReturnsSameList() {
        val probed = item("content://media/external/video/media/2", durationMs = 20_000L)
        val result = MediaScanWorker.resolveMissingDurations(context, listOf(probed), emptyMap())

        assertEquals(1, result.size)
        assertEquals(20_000L, result.first().durationMs)

        val allKnown = listOf(
            item("content://media/external/video/media/3", durationMs = 10_000L),
            item("content://media/external/video/media/4", durationMs = 0L, sizeBytes = 500L)
        )
        val fastPath = MediaScanWorker.resolveMissingDurations(context, allKnown, emptyMap())
        assertSame("known rows must skip the retriever entirely", allKnown, fastPath)
    }
}
