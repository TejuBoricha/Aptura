package org.aptura.player.data.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A video that never starts has to say where it stopped (the owner's report from a Samsung S24 said only "buffering").
 * The trace keeps the player's steps in order with their timing, bounded; the thread summary names where the playback
 * threads were, by class and method only.
 */
class PlaybackTraceTest {

    private var now = 0L
    private fun trace(capacity: Int = 48) = PlaybackTrace(capacity) { now }

    @Test
    fun stepsAreKeptInOrder_withTheirTime() {
        val t = trace()
        t.note("requested")
        now += 120_000_000L
        t.note("load-start")
        now += 30_000_000L
        t.note("load-done")
        assertEquals("+0ms requested | +120ms load-start | +150ms load-done", t.summary())
    }

    @Test
    fun aNewRequestStartsAgain() {
        val t = trace()
        t.note("old")
        now += 5_000_000_000L
        t.restart()
        t.note("new")
        assertEquals("+0ms new", t.summary())
    }

    @Test
    fun itIsBounded_andKeepsTheNewestSteps() {
        val t = trace(capacity = 3)
        repeat(10) { t.note("step$it") }
        val summary = t.summary()
        assertFalse(summary.contains("step6"))
        assertTrue(summary.contains("step7") && summary.contains("step8") && summary.contains("step9"))
    }

    @Test
    fun aLongSummary_isCutFromTheFront_soTheLatestStepsSurvive() {
        val t = trace(capacity = 200)
        repeat(200) { t.note("a-fairly-long-step-name-$it") }
        val summary = t.summary(maxChars = 300)
        assertTrue(summary.length <= 300)
        assertTrue(summary.endsWith("a-fairly-long-step-name-199"))
        assertTrue(summary.startsWith("…"))
    }

    // ---- where the threads were ------------------------------------------------------------------------------------

    private fun frames(vararg frames: Pair<String, String>) =
        frames.map { (cls, method) -> StackTraceElement(cls, method, "X.java", 1) }.toTypedArray()

    @Test
    fun onlyThePlaybackThreadsAreDescribed_byClassAndMethodName() {
        val loader = Thread(null, {}, "ExoPlayer:Loader:ProgressiveMediaPeriod")
        val unrelated = Thread(null, {}, "OkHttp Dispatcher")
        val summary = PlaybackThreads.summarise(
            mapOf(
                loader to frames("java.io.FileInputStream" to "read", "androidx.media3.datasource.ContentDataSource" to "read"),
                unrelated to frames("x.Y" to "z")
            )
        )
        assertTrue(summary, summary.contains("ExoPlayer:Loader:ProgressiveMediaPeriod [NEW]: FileInputStream.read < ContentDataSource.read"))
        assertFalse(summary.contains("OkHttp"))
    }

    @Test
    fun aThreadIsShownByItsFirstFramesOnly() {
        val playback = Thread(null, {}, "ExoPlayer:Playback")
        val many = frames(*Array(20) { "pkg.Class$it" to "method$it" })
        val summary = PlaybackThreads.summarise(mapOf(playback to many), framesPerThread = 3)
        assertTrue(summary.contains("Class0.method0 < Class1.method1 < Class2.method2"))
        assertFalse(summary.contains("Class3"))
    }

    @Test
    fun withNoPlaybackThreads_itSaysSo() {
        assertEquals("(no playback threads)", PlaybackThreads.summarise(mapOf(Thread(null, {}, "main") to frames())))
    }

    @Test
    fun theSummaryIsBounded() {
        val threads = (1..6).associate { Thread(null, {}, "ExoPlayer:Thread$it") to frames(*Array(7) { i -> "a.LongClassName$i" to "aLongMethodName$i" }) }
        assertTrue(PlaybackThreads.summarise(threads, maxChars = 200).length <= 200)
    }
}
