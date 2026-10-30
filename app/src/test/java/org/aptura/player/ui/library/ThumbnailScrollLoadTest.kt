package org.aptura.player.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.aptura.player.core.media.ThumbnailBitmapCache
import org.aptura.player.theme.ApturaTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * "If I scroll fast through the videos it feels laggy" (558 videos, Samsung S24).
 *
 * Every cell that scrolled into view started reading or making its picture at once, with no limit on how many ran at the
 * same time and no pause for cells that were only flashing past, so a hard fling started over two hundred of them (and up
 * to thirty ran together) and took the processor away from the screen. Now nothing starts while the grid is being flung,
 * the cells on screen when it slows down get their pictures then, and at most three run together however many are asked
 * for. Measured before the change: 212 pieces of work during the fling, 30 at once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ThumbnailScrollLoadTest {

    @get:Rule
    val composeRule = createComposeRule()

    @After
    fun clear() = ThumbnailBitmapCache.clear()

    /** Counts every piece of picture work that is started, and runs it where it stands. */
    private class CountingDispatcher : CoroutineDispatcher() {
        var started = 0
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            started++
            block.run()
        }
    }

    private fun flingThroughTheLibrary(): Pair<Int, Int> {
        val counting = CountingDispatcher()
        lateinit var state: LazyGridState
        composeRule.setContent {
            CompositionLocalProvider(LocalThumbnailDecodeContext provides counting) {
                ApturaTheme {
                    state = rememberLazyGridState()
                    CompositionLocalProvider(LocalThumbnailsPaused provides rememberFlingGate(state)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = state,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = List(558) { "content://video/$it" }, key = { it }) { uri ->
                            VideoThumbnail(uri, 100L, null, Modifier.height(120.dp))
                        }
                    }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1_000)
        val atRest = counting.started

        // A hard flick: about eight rows every tenth of a second, for a second, one frame at a time.
        composeRule.mainClock.autoAdvance = false
        val rowPx = with(composeRule.density) { 128.dp.toPx() }
        repeat(60) {
            composeRule.runOnUiThread { state.dispatchRawDelta(rowPx) }
            composeRule.mainClock.advanceTimeBy(16)
        }
        val duringTheFling = counting.started - atRest
        // Then the finger stops.
        repeat(40) { composeRule.mainClock.advanceTimeBy(16) }
        val afterItStops = counting.started - atRest - duringTheFling
        return duringTheFling to afterItStops
    }

    @Test
    fun aHardFlingThroughHundredsOfVideos_startsNoPictureWorkForCellsThatFlashPast() {
        val (duringTheFling, afterItStops) = flingThroughTheLibrary()
        println("PICTURE WORK STARTED: during the fling = $duringTheFling, after it stops = $afterItStops")

        // The first frame or two of a fling start a little work before the speed is known; before the change it was 212.
        assertTrue("$duringTheFling pieces of picture work started while the cells were only flashing past", duringTheFling <= 10)
        assertTrue("the cells on screen when it stops still get their pictures ($afterItStops)", afterItStops in 1..40)
    }

    @Test
    fun atMostThreePicturesAreMadeAtOnce_howeverManyAreAskedFor() {
        fun peakWith(dispatcher: CoroutineDispatcher): Int = runBlocking {
            val running = AtomicInteger()
            val peak = AtomicInteger()
            List(60) {
                async {
                    withContext(dispatcher) {
                        val now = running.incrementAndGet()
                        peak.updateAndGet { maxOf(it, now) }
                        Thread.sleep(15) // reading and decoding a picture blocks its thread
                        running.decrementAndGet()
                    }
                }
            }.awaitAll()
            peak.get()
        }

        val unlimited = peakWith(Dispatchers.IO)
        val limited = peakWith(ThumbnailWork.dispatcher)
        println("PICTURES AT ONCE: before (plain IO) = $unlimited, now = $limited")

        assertTrue("the limit does not hold: $limited at once", limited <= ThumbnailWork.PARALLEL)
    }
}
