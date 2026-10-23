@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.aptura.player.domain.model.DomainExternalSubtitle
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.domain.repository.SidecarSubtitleResolver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The queue lives inside a REAL [ExoPlayer] playlist (audit P0-07: Repeat All used to loop one file, Shuffle did
 * nothing, and next/previous were dead in the notification). These tests drive the production controller against the
 * production player class — the only thing missing is media to decode, which queue navigation does not need.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Media3PlaybackControllerQueueTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var player: ExoPlayer

    private fun item(n: Int) = DomainMediaItem(
        uri = "content://media/$n",
        title = "Track $n",
        durationMs = 60_000L * n,
        sizeBytes = 1_048_576L * n,
        mimeType = "video/mp4"
    )

    private val items = (1..4).map(::item)

    /** Files that do not exist: the real ExoPlayer fails to open them with a real IO_FILE_NOT_FOUND error. */
    private val missingFiles = (1..3).map { item(it).copy(uri = "file:///definitely/missing/video$it.mp4") }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        player = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()
    }

    @After
    fun tearDown() {
        player.release()
        Dispatchers.resetMain()
    }

    private fun controller(resolver: SidecarSubtitleResolver? = null) = Media3PlaybackController(
        player = player,
        scope = TestScope(testDispatcher),
        mainDispatcher = testDispatcher,
        enablePositionTicker = false,
        sidecarResolver = resolver,
        ioDispatcher = testDispatcher
    )

    /** Lets ExoPlayer's queued listener callbacks reach the controller, like the main looper does in production. */
    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    private fun playerUris() = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }

    @Test
    fun playQueue_loadsTheWholeQueueIntoThePlayer_andStartsTheChosenItem() = runTest(testDispatcher) {
        val controller = controller()

        controller.playQueue(items.take(3), startIndex = 1)
        settle()

        assertEquals(items.take(3).map { it.uri }, playerUris())
        assertEquals(1, player.currentMediaItemIndex)
        assertTrue("playback must be requested", player.playWhenReady)

        val queue = controller.playbackQueue.value
        assertEquals(1, queue.currentIndex)
        assertEquals(items[1], queue.currentItem)
        assertEquals(true, queue.nextAvailable)
        assertEquals(true, queue.previousAvailable)
    }

    @Test
    fun seekRightAfterLoadingTheQueue_isNotClampedToZero_soResumeWorks() = runTest(testDispatcher) {
        val controller = controller()
        controller.playQueue(items.take(2), startIndex = 1)
        // The player has only just been given the media: it does not know the duration yet.
        assertEquals(C.TIME_UNSET, player.duration)

        controller.seekTo(25_000L) // what "resume where you left off" does

        assertEquals("resume was lost: seek was clamped to the unknown duration", 25_000L, player.currentPosition)
        assertEquals(1, player.currentMediaItemIndex)
    }

    /**
     * ExoPlayer schedules its work with delayed messages on its own thread. Robolectric's clock only moves when told
     * to, so move it a little on every turn, drain the main looper, and give the real threads a moment to run.
     */
    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        val mainLooper = shadowOf(Looper.getMainLooper())
        while (System.currentTimeMillis() < deadline) {
            mainLooper.idleFor(java.time.Duration.ofMillis(50))
            if (condition()) return true
            Thread.sleep(10)
        }
        mainLooper.idle()
        return condition()
    }

    @Test
    fun anItemThatCannotBeOpened_isSkipped_andTheRestOfTheQueueIsNotStranded() = runTest(testDispatcher) {
        val controller = controller()
        val skipped = mutableListOf<String>()
        val collector = launch(testDispatcher) { controller.skippedItems.collect { skipped += it } }
        runCurrent()

        controller.playQueue(missingFiles, startIndex = 0)
        waitUntil { runCurrent(); skipped.size >= 2 } // the collector only runs when the test scheduler is advanced
        runCurrent()

        assertEquals("Track 1 and Track 2 are skipped; Track 3 has nowhere further to go", listOf("Track 1", "Track 2"), skipped)
        assertEquals(2, player.currentMediaItemIndex)
        collector.cancel()
    }

    @Test
    fun aBrokenQueueInRepeatAll_doesNotSkipForever() = runTest(testDispatcher) {
        val controller = controller()
        val skipped = mutableListOf<String>()
        val collector = launch(testDispatcher) { controller.skippedItems.collect { skipped += it } }
        runCurrent()

        controller.setPlayMode(PlayMode.REPEAT_ALL)
        controller.playQueue(missingFiles, startIndex = 0)
        waitUntil { runCurrent(); skipped.size >= 2 } // the collector only runs when the test scheduler is advanced
        Thread.sleep(400) // give a runaway loop the chance to show itself
        settle()
        runCurrent()

        assertEquals("at most queue-size minus one skips, then the error is shown", 2, skipped.size)
        collector.cancel()
    }

    @Test
    fun aSingleBrokenVideo_isNotSkipped_theUserSeesTheError() = runTest(testDispatcher) {
        val controller = controller()
        val skipped = mutableListOf<String>()
        val collector = launch(testDispatcher) { controller.skippedItems.collect { skipped += it } }
        runCurrent()

        controller.playQueue(missingFiles.take(1), startIndex = 0)
        waitUntil { controller.playbackState.value is org.aptura.player.domain.model.DomainPlaybackState.Error }

        assertEquals(emptyList<String>(), skipped)
        assertTrue(controller.playbackState.value is org.aptura.player.domain.model.DomainPlaybackState.Error)
        collector.cancel()
    }

    @Test
    fun skipToNext_movesThePlayerToTheNextItem_andStopsAtTheEndOfTheQueue() = runTest(testDispatcher) {
        val controller = controller()
        controller.playQueue(items.take(2), startIndex = 0)
        settle()

        controller.skipToNext()
        settle()

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(items[1], controller.playbackQueue.value.currentItem)
        assertEquals("last item of a normal queue has no next", false, controller.playbackQueue.value.nextAvailable)

        controller.skipToNext() // must be a no-op, not a crash or a wrap-around
        settle()
        assertEquals(1, player.currentMediaItemIndex)
    }

    @Test
    fun skipToPrevious_goesToThePreviousItem_whenLessThanThreeSecondsIn() = runTest(testDispatcher) {
        val controller = controller()
        controller.playQueue(items.take(2), startIndex = 1)
        settle()

        controller.skipToPrevious()
        settle()

        assertEquals(0, player.currentMediaItemIndex)
        assertEquals(items[0], controller.playbackQueue.value.currentItem)
    }

    @Test
    fun repeatAll_wrapsToTheFirstItem_insteadOfLoopingTheCurrentOne() = runTest(testDispatcher) {
        val controller = controller()
        controller.playQueue(items.take(3), startIndex = 2) // the LAST item
        controller.setPlayMode(PlayMode.REPEAT_ALL)
        settle()

        val queue = controller.playbackQueue.value
        assertEquals(Player_REPEAT_ALL, player.repeatMode)
        assertEquals("Repeat All must offer a next item after the last one", true, queue.nextAvailable)
        assertEquals("…and it must be the FIRST item, not the same one again", 0, queue.nextIndex)

        controller.skipToNext()
        settle()
        assertEquals(0, player.currentMediaItemIndex)
    }

    @Test
    fun shuffle_visitsEveryItemExactlyOnce() = runTest(testDispatcher) {
        val controller = controller()
        controller.playQueue(items, startIndex = 0)
        controller.setPlayMode(PlayMode.SHUFFLE)
        settle()
        assertTrue(player.shuffleModeEnabled)

        val visited = mutableListOf(player.currentMediaItemIndex)
        repeat(items.size - 1) {
            controller.skipToNext()
            visited += player.currentMediaItemIndex
        }

        assertEquals("every item once, none repeated: $visited", items.indices.toSet(), visited.toSet())
        assertEquals(items.size, visited.size)
        assertFalse("after the whole shuffled queue there is nothing further", player.hasNextMediaItem())
    }

    @Test
    fun queueEdits_areAppliedToThePlayersPlaylist() = runTest(testDispatcher) {
        val controller = controller()
        controller.playQueue(items.take(3), startIndex = 0)
        settle()

        controller.addToQueue(items[3])
        assertEquals(items.map { it.uri }, playerUris())

        controller.removeFromQueue(3)
        controller.playNext(items[3]) // plays right after the current item
        settle()
        assertEquals(listOf(items[0], items[3], items[1], items[2]).map { it.uri }, playerUris())
        assertEquals(
            "the controller's queue is the player's playlist",
            playerUris(),
            controller.playbackQueue.value.items.map { it.uri }
        )

        controller.moveQueueItem(fromIndex = 3, toIndex = 1)
        settle()
        assertEquals(listOf(items[0], items[2], items[3], items[1]).map { it.uri }, playerUris())

        controller.clearQueue() // keeps what is playing
        settle()
        assertEquals(listOf(items[0].uri), playerUris())
        assertEquals(1, controller.playbackQueue.value.items.size)
    }

    @Test
    fun pauseAtEndOfItems_isSwitchedOffForRepeatOne_andOnOtherwise() = runTest(testDispatcher) {
        val controller = controller()
        controller.playQueue(items.take(2), startIndex = 0)

        controller.setPauseAtEndOfItems(true)
        assertTrue("Up Next countdown needs the player to park at the end", player.pauseAtEndOfMediaItems)

        controller.setPlayMode(PlayMode.REPEAT_ONE)
        assertFalse("repeat-one must loop, never park", player.pauseAtEndOfMediaItems)

        controller.setPlayMode(PlayMode.NORMAL)
        assertTrue(player.pauseAtEndOfMediaItems)
    }

    @Test
    fun audioOnly_disablesTheVideoTrackType_andShowVideoRestoresIt() = runTest(testDispatcher) {
        val controller = controller()

        controller.setVideoEnabled(false)
        assertTrue(C.TRACK_TYPE_VIDEO in player.trackSelectionParameters.disabledTrackTypes)

        controller.setVideoEnabled(true)
        assertFalse(C.TRACK_TYPE_VIDEO in player.trackSelectionParameters.disabledTrackTypes)
    }

    @Test
    fun mediaItems_carryTheTitle_soTheNotificationAndLockScreenCanShowIt() = runTest(testDispatcher) {
        val controller = controller()
        controller.playQueue(
            listOf(item(1).copy(title = "My_Holiday_Clip.mp4")),
            startIndex = 0
        )

        val metadata = player.getMediaItemAt(0).mediaMetadata
        assertEquals("My Holiday Clip", metadata.title.toString())
        assertEquals("My Holiday Clip", metadata.displayTitle.toString())
    }

    @Test
    fun sidecarSubtitle_isInTheMediaItemBeforePlaybackStarts_andAnnounced() = runTest(testDispatcher) {
        val sidecar = DomainExternalSubtitle(
            uri = "content://subs/track1.en.srt",
            label = "English (track1.en.srt)",
            language = "en",
            isDefault = true
        )
        val controller = controller(object : SidecarSubtitleResolver {
            override suspend fun resolve(mediaUri: String) =
                if (mediaUri == items[0].uri) listOf(sidecar) else emptyList()
        })
        val announced = mutableListOf<String>()
        val collector = launch(testDispatcher) { controller.autoPairedSubtitles.collect { announced += it } }
        runCurrent()

        controller.playQueue(items.take(2), startIndex = 0)
        runCurrent()

        // The very first MediaItem handed to the player already contains the subtitle: playback never starts
        // without it and then restarts when the lookup finishes (audit P0-10).
        val subs = player.getMediaItemAt(0).localConfiguration?.subtitleConfigurations.orEmpty()
        assertEquals(1, subs.size)
        assertEquals("English (track1.en.srt)", subs[0].label.toString())
        assertEquals(setOf("English"), announced.toSet())
        collector.cancel()
    }

    @Test
    fun neighbouringItems_getTheirSidecarsResolvedInTheBackground_withoutRestartingPlayback() = runTest(testDispatcher) {
        val nextSidecar = DomainExternalSubtitle(
            uri = "content://subs/track2.srt",
            label = "Subtitles (track2.srt)",
            isDefault = true
        )
        val controller = controller(object : SidecarSubtitleResolver {
            override suspend fun resolve(mediaUri: String) =
                if (mediaUri == items[1].uri) listOf(nextSidecar) else emptyList()
        })

        controller.playQueue(items.take(3), startIndex = 0)
        runCurrent() // runs the background prefetch for the next item
        settle()

        assertEquals("prefetch must not move the player", 0, player.currentMediaItemIndex)
        val next = player.getMediaItemAt(1)
        assertEquals(items[1].uri, next.mediaId)
        assertNotNull(next.localConfiguration?.subtitleConfigurations?.singleOrNull())
    }

    private companion object {
        const val Player_REPEAT_ALL = androidx.media3.common.Player.REPEAT_MODE_ALL
    }
}
