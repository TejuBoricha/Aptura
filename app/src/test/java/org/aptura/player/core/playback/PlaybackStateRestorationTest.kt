package org.aptura.player.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RQ-07: Process-Death and Playback State Restoration Matrix.
 * Source of truth: spec/08A RQ-07, spec/09 §4, and spec/12A §3.3 (TST-REST)
 */
class PlaybackStateRestorationTest {

    private lateinit var journal: PlaybackStateJournal

    @Before
    fun setUp() {
        journal = PlaybackStateJournal(saveIntervalMs = 5000L)
    }

    @Test
    fun calculateResumePosition_applies2SecondPreRollComfortWindow() {
        val entry = PlaybackJournalEntry(
            mediaId = "test-video-1",
            mediaUri = "content://media/1",
            positionMs = 45_000L,
            durationMs = 120_000L
        )

        // 45s position -> 43s resume position (2s pre-roll)
        assertEquals(43_000L, entry.calculateResumePositionMs())
        assertTrue(entry.isResumeCandidate())
    }

    @Test
    fun calculateResumePosition_boundariesHandledCorrectly() {
        // Within first 3 seconds -> restart from 0
        val nearStart = PlaybackJournalEntry(
            mediaId = "start-test",
            mediaUri = "content://media/2",
            positionMs = 2_500L,
            durationMs = 60_000L
        )
        assertEquals(0L, nearStart.calculateResumePositionMs())
        assertFalse(nearStart.isResumeCandidate())

        // Near end (<10s remaining) -> restart from 0
        val nearEnd = PlaybackJournalEntry(
            mediaId = "end-test",
            mediaUri = "content://media/3",
            positionMs = 55_000L,
            durationMs = 60_000L // 5s remaining
        )
        assertEquals(0L, nearEnd.calculateResumePositionMs())
        assertFalse(nearEnd.isResumeCandidate())
    }

    @Test
    fun restoreSession_honestNoticeWhenSourceUnavailable_neverSilentlyFromZero() {
        journal.updatePosition(
            mediaId = "media-sd-01",
            mediaUri = "content://sdcard/media/5",
            positionMs = 60_000L,
            durationMs = 180_000L,
            immediate = true
        )

        // Source unavailable (e.g. SD card pulled)
        val result = journal.restoreSession("media-sd-01", sourceAvailable = false)
        assertTrue(result is PlaybackStateJournal.RestoreResult.RestartWithNotice)
        val notice = result as PlaybackStateJournal.RestoreResult.RestartWithNotice
        assertTrue(notice.reason.contains("inaccessible"))
    }

    @Test
    fun rq07_spike_all12KillPoints_satisfyContractualExpectedRestoreStates() {
        // Prepare journal states for the playback killpoints
        journal.updatePosition(
            mediaId = "media-internal-01",
            mediaUri = "content://media/internal/10",
            positionMs = 30_000L,
            durationMs = 120_000L,
            audioTrackId = "audio_en_1",
            subtitleTrackId = "sub_en_1",
            immediate = true
        )

        journal.updatePosition(
            mediaId = "media-sd-01",
            mediaUri = "content://media/sd/20",
            positionMs = 75_000L,
            durationMs = 300_000L,
            immediate = true
        )

        journal.updatePosition(
            mediaId = "media-saf-01",
            mediaUri = "content://com.android.externalstorage.documents/tree/30",
            positionMs = 90_000L,
            durationMs = 300_000L,
            immediate = true
        )

        // Completed item for Killpoint 12
        journal.updatePosition(
            mediaId = "media-completed-01",
            mediaUri = "content://media/internal/40",
            positionMs = 120_000L,
            durationMs = 120_000L,
            immediate = true
        )
        journal.markCompleted("media-completed-01")

        // Evaluate all 12 kill points per spec/12A §3.3
        val records = (1..12).map { kp ->
            KillPointSimulator.evaluateKillPoint(kp, journal)
        }

        assertEquals("Must evaluate all 12 killpoints", 12, records.size)

        for (record in records) {
            assertTrue("Killpoint ${record.killpointId} must survive", record.survives)
            assertTrue("Killpoint ${record.killpointId} must maintain journal consistency", record.journalConsistent)
            assertTrue("Killpoint ${record.killpointId} must have valid non-empty restore state", record.restoredState.isNotBlank())
        }

        // Verify Killpoint 12 specifically does NOT falsely resume
        val kp12 = records.first { it.killpointId == 12 }
        assertEquals("NOTHING_TO_RESTORE_NO_FALSE_RESUME", kp12.restoredState)
        assertFalse(kp12.honestNoticeShown)

        // Verify Killpoint 5 with unavailable source presents honest notice
        val kp5Unavailable = KillPointSimulator.evaluateKillPoint(5, journal, sourceAvailable = false)
        assertTrue(kp5Unavailable.survives)
        assertTrue(kp5Unavailable.honestNoticeShown)
        assertEquals("HONEST_NOTICE_SOURCE_UNAVAILABLE", kp5Unavailable.restoredState)
    }
}
