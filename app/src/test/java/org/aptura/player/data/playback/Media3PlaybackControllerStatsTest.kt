@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.Timeline
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.aptura.player.domain.model.DomainMediaItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit FUNC-10: the drop percentage in the stats overlay comes from the player's own counters. The count of rendered
 * frames was declared but never filled in, so the share was always 0% or 100%.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Media3PlaybackControllerStatsTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var player: ExoPlayer
    private lateinit var controller: Media3PlaybackController

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        player = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()
        controller = Media3PlaybackController(
            player = player,
            scope = TestScope(dispatcher),
            mainDispatcher = dispatcher,
            enablePositionTicker = false
        )
    }

    @After
    fun tearDown() {
        player.release()
        Dispatchers.resetMain()
    }

    private val eventTime = AnalyticsListener.EventTime(
        0L, Timeline.EMPTY, 0, null, 0L, Timeline.EMPTY, 0, null, 0L, 0L
    )

    private fun counters(rendered: Int) = DecoderCounters().apply { renderedOutputBufferCount = rendered }

    @Test
    fun oneDroppedFrameAfterAThousandRendered_isATenthOfAPercent_notOneHundred() {
        val live = counters(rendered = 999)
        controller.analyticsListener.onVideoEnabled(eventTime, live)

        controller.analyticsListener.onDroppedVideoFrames(eventTime, 1, 100L)

        assertEquals(0.1f, controller.playbackStats.value.dropPercentage, 0.01f)
        assertEquals(1, controller.playbackStats.value.droppedFrames)
    }

    @Test
    fun theShareFollowsTheFramesAsTheyAreRendered() {
        val live = counters(rendered = 50)
        controller.analyticsListener.onVideoEnabled(eventTime, live)
        controller.analyticsListener.onDroppedVideoFrames(eventTime, 5, 100L)
        assertEquals(5f * 100f / 55f, controller.playbackStats.value.dropPercentage, 0.05f)

        live.renderedOutputBufferCount = 950 // playback goes on; the same five drops are now a smaller share
        controller.updatePlaybackStats()

        assertEquals(0.5f, controller.playbackStats.value.dropPercentage, 0.05f)
    }

    @Test
    fun aDropBeforeAnyFrameHasBeenCounted_showsNoRate() {
        controller.analyticsListener.onDroppedVideoFrames(eventTime, 1, 100L)

        assertEquals(0f, controller.playbackStats.value.dropPercentage, 0.001f)
        assertEquals("the count itself is still shown", 1, controller.playbackStats.value.droppedFrames)
    }
    @Test
    fun aNewVideoStartsItsCountsAgain() = runTest(dispatcher) {
        val live = counters(rendered = 1_000)
        controller.analyticsListener.onVideoEnabled(eventTime, live)
        controller.analyticsListener.onDroppedVideoFrames(eventTime, 1, 100L)

        // The next video: the renderer stays enabled and its counter goes on from where it was.
        controller.playQueue(
            listOf(DomainMediaItem(uri = "file:///definitely/missing/next.mp4", title = "Next", durationMs = 60_000L, sizeBytes = 1L, mimeType = "video/mp4")),
            0
        )
        live.renderedOutputBufferCount = 1_030 // 30 frames of the new video
        controller.analyticsListener.onDroppedVideoFrames(eventTime, 3, 100L)

        val stats = controller.playbackStats.value
        assertEquals("the earlier video's drop is not carried over", 3, stats.droppedFrames)
        assertEquals("3 of its 33 frames, not of the 1,033 since the renderer was enabled", 3f * 100f / 33f, stats.dropPercentage, 0.05f)
    }

    @Test
    fun aDecoderChangeInTheMiddleOfAVideo_keepsTheFramesAlreadyCounted() {
        val first = counters(rendered = 500)
        controller.analyticsListener.onVideoEnabled(eventTime, first)
        controller.analyticsListener.onVideoDisabled(eventTime, first) // the decoder is switched

        val second = counters(rendered = 100)
        controller.analyticsListener.onVideoEnabled(eventTime, second)
        controller.analyticsListener.onDroppedVideoFrames(eventTime, 6, 100L)

        // 6 dropped of 600 rendered plus the 6: 6 / 606, about 0.99%. Without the 500 it would read 5.7%.
        assertEquals(6f * 100f / 606f, controller.playbackStats.value.dropPercentage, 0.05f)
    }
}
