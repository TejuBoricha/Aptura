package org.aptura.player.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for process-death state restoration and playback journal contracts (QA-02, spec/09 §4, spec/12A §3.3).
 * Verifies real session restoration, 2-second comfort window, near-boundary rules, and honest notices
 * without tautological simulator mocks.
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

        // 45s position -> 43s resume position (2s pre-roll comfort window)
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
    fun restoreSession_internalStorageMidPlayback_resumesWithComfortWindowAndPreservesTracks() {
        journal.updatePosition(
            mediaId = "media-internal-01",
            mediaUri = "content://media/internal/10",
            positionMs = 30_000L,
            durationMs = 120_000L,
            audioTrackId = "audio_en_1",
            subtitleTrackId = "sub_en_1",
            playbackSpeed = 1.25f,
            immediate = true
        )

        val result = journal.restoreSession("media-internal-01", sourceAvailable = true)
        assertTrue("Internal playback must resume", result is PlaybackStateJournal.RestoreResult.Resumed)
        val resumed = result as PlaybackStateJournal.RestoreResult.Resumed

        assertEquals(28_000L, resumed.resumePositionMs) // 30s - 2s comfort window
        assertEquals("audio_en_1", resumed.entry.audioTrackId)
        assertEquals("sub_en_1", resumed.entry.subtitleTrackId)
        assertEquals(1.25f, resumed.entry.playbackSpeed, 0.001f)
    }

    @Test
    fun restoreSession_removableStorageUnavailable_presentsHonestNoticeWithoutSilentZeroRestart() {
        journal.updatePosition(
            mediaId = "media-sd-01",
            mediaUri = "content://sdcard/media/5",
            positionMs = 60_000L,
            durationMs = 180_000L,
            immediate = true
        )

        // Storage unmounted or permission revoked
        val result = journal.restoreSession("media-sd-01", sourceAvailable = false)
        assertTrue(
            "Unavailable source must present honest notice, never silently restarting from zero",
            result is PlaybackStateJournal.RestoreResult.RestartWithNotice
        )
        val notice = result as PlaybackStateJournal.RestoreResult.RestartWithNotice
        assertTrue(notice.reason.contains("inaccessible"))
        assertEquals(60_000L, notice.entry.positionMs)
    }

    @Test
    fun restoreSession_completedMedia_returnsCompletedOrAtStartWithoutFalseResume() {
        journal.updatePosition(
            mediaId = "media-completed-01",
            mediaUri = "content://media/internal/40",
            positionMs = 120_000L,
            durationMs = 120_000L,
            immediate = true
        )
        journal.markCompleted("media-completed-01")

        val result = journal.restoreSession("media-completed-01", sourceAvailable = true)
        assertTrue(
            "Completed media must not trigger false resumption",
            result is PlaybackStateJournal.RestoreResult.CompletedOrAtStart
        )
        val completed = result as PlaybackStateJournal.RestoreResult.CompletedOrAtStart
        assertTrue(completed.entry.completed)
        assertEquals(0L, completed.entry.calculateResumePositionMs())
    }

    @Test
    fun restoreSession_nearEndWithinLast10Seconds_treatedAsCompletedWithoutFalseResume() {
        journal.updatePosition(
            mediaId = "media-near-end-01",
            mediaUri = "content://media/internal/50",
            positionMs = 115_000L,
            durationMs = 120_000L, // 5s remaining (<10s)
            immediate = true
        )

        val result = journal.restoreSession("media-near-end-01", sourceAvailable = true)
        assertTrue(
            "Media in last 10s must be treated as completed",
            result is PlaybackStateJournal.RestoreResult.CompletedOrAtStart
        )
        val completed = result as PlaybackStateJournal.RestoreResult.CompletedOrAtStart
        assertTrue(completed.entry.completed)
        assertEquals(0L, completed.entry.calculateResumePositionMs())
    }

    @Test
    fun restoreSession_nonexistentMedia_returnsNoState() {
        val result = journal.restoreSession("nonexistent-id", sourceAvailable = true)
        assertEquals(PlaybackStateJournal.RestoreResult.NoState, result)
    }

    @Test
    fun updatePosition_throttlesPeriodicSavesWithinSaveInterval_unlessImmediate() {
        journal.updatePosition(
            mediaId = "throttle-test",
            mediaUri = "content://media/throttle/1",
            positionMs = 10_000L,
            durationMs = 60_000L,
            immediate = true
        )

        // Non-immediate call within 5s is dropped
        journal.updatePosition(
            mediaId = "throttle-test",
            mediaUri = "content://media/throttle/1",
            positionMs = 12_000L,
            durationMs = 60_000L,
            immediate = false
        )
        assertEquals(10_000L, journal.getActiveEntry()?.positionMs)

        // Immediate call updates right away
        journal.updatePosition(
            mediaId = "throttle-test",
            mediaUri = "content://media/throttle/1",
            positionMs = 14_000L,
            durationMs = 60_000L,
            immediate = true
        )
        assertEquals(14_000L, journal.getActiveEntry()?.positionMs)
    }
}
