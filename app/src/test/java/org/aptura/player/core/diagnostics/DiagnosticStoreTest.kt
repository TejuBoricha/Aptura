package org.aptura.player.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The diagnostics used to live only in memory, so a report written after a crash said "No errors logged in recent
 * sessions" about the very session that crashed. These tests use real files in a real directory.
 */
class DiagnosticStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var now = 1_000_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun store(
        maxBytes: Long = DiagnosticStore.DEFAULT_MAX_BYTES,
        maxSessions: Int = DiagnosticStore.DEFAULT_MAX_SESSIONS,
        maxCrashes: Int = DiagnosticStore.DEFAULT_MAX_CRASHES,
        dir: File = File(folder.root, "diagnostics")
    ) = DiagnosticStore(dir, clock = { now }, maxBytes = maxBytes, maxSessions = maxSessions, maxCrashes = maxCrashes)

    private fun event(
        n: Int,
        session: String = "s1",
        payload: Map<String, String> = mapOf("n" to n.toString()),
        at: Long = now
    ) = DiagnosticEvent(EventCode.EV_011, session, at, payload)

    @Test
    fun eventsSurviveANewStoreInstance_likeANewProcess() {
        store().apply { append(event(1)); append(event(2)) }

        val reloaded = store().loadEvents()

        assertEquals(listOf("1", "2"), reloaded.map { it.payload["n"] })
    }

    @Test
    fun paths_emails_andIpAddresses_neverReachTheFile() {
        val s = store()
        s.append(
            event(
                1,
                payload = mapOf(
                    "path" to "/storage/emulated/0/Movies/secret_holiday.mp4",
                    "note" to "mail me at jane.doe@example.com from 192.168.1.20"
                )
            )
        )

        val raw = File(folder.root, "diagnostics/${DiagnosticStore.EVENTS_FILE}").readText()

        assertFalse(raw.contains("secret_holiday"))
        assertFalse(raw.contains("jane.doe"))
        assertFalse(raw.contains("192.168.1.20"))
        assertTrue("the structural part survives", raw.contains("mp4"))
    }

    @Test
    fun aPayloadCannotForgeAnotherEvent() {
        val s = store()
        s.append(event(1, payload = mapOf("note" to "x\"}\n{\"code\":\"EV_011\",\"sessionId\":\"evil\",\"payload\":{}}")))

        val loaded = s.loadEvents()

        assertEquals("exactly the one event that was written", 1, loaded.size)
        assertEquals("s1", loaded.single().sessionId)
    }

    @Test
    fun theFileStaysWithinItsSizeCap_keepingTheNewestEvents() {
        val s = store(maxBytes = 20_000)
        repeat(2_000) { s.append(event(it)) }

        val file = File(folder.root, "diagnostics/${DiagnosticStore.EVENTS_FILE}")
        val loaded = s.loadEvents()

        assertTrue("file is ${file.length()} bytes", file.length() <= 20_000)
        assertEquals("the newest event is kept", "1999", loaded.last().payload["n"])
        assertTrue("the oldest are dropped", loaded.first().payload["n"]!!.toInt() > 0)
        assertTrue(loaded.size > 50)
    }

    @Test
    fun eventsOlderThanThirtyDays_areDropped() {
        val s = store()
        s.append(event(1, at = now - 31 * day))
        s.append(event(2, at = now - 29 * day))

        assertEquals(listOf("2"), s.loadEvents().map { it.payload["n"] })
    }

    @Test
    fun onlyTheLastFiveSessionsAreKept() {
        val s = store()
        (1..7).forEach { session -> s.append(event(session, session = "s$session", at = now + session)) }

        assertEquals(listOf("s3", "s4", "s5", "s6", "s7"), s.loadEvents().map { it.sessionId })
    }

    @Test
    fun aWriteCutOffByACrash_doesNotLoseTheEventsBeforeIt() {
        val s = store()
        s.append(event(1))
        s.append(event(2))
        File(folder.root, "diagnostics/${DiagnosticStore.EVENTS_FILE}").appendText("{\"code\":\"EV_011\",\"sessionId\":\"s1\",\"timest")

        assertEquals(listOf("1", "2"), s.loadEvents().map { it.payload["n"] })
    }

    @Test
    fun onlyTheLastThreeCrashesAreKept_newestFirst() {
        val s = store()
        (1..5).forEach { s.writeCrash(now + it, "crash $it") }

        val crashes = s.loadCrashes()

        assertEquals(listOf("crash 5", "crash 4", "crash 3"), crashes.map { it.text })
    }

    @Test
    fun anAcknowledgedCrashStaysInTheLog_butStopsPrompting() {
        val s = store()
        val name = s.writeCrash(now, "boom")!!
        assertFalse(s.loadCrashes().single().acknowledged)

        s.acknowledgeCrash(name)

        val crash = s.loadCrashes().single()
        assertTrue(crash.acknowledged)
        assertEquals("boom", crash.text)
    }

    @Test
    fun deletingDiagnosticData_removesEverything() {
        val s = store()
        s.append(event(1))
        s.writeCrash(now, "boom")

        s.clear()

        assertTrue(s.loadEvents().isEmpty())
        assertTrue(s.loadCrashes().isEmpty())
        assertFalse(File(folder.root, "diagnostics/${DiagnosticStore.EVENTS_FILE}").exists())
    }

    @Test
    fun aStoreThatCannotWrite_failsQuietly_insteadOfBreakingTheApp() {
        val blocker = folder.newFile("not-a-directory") // a FILE where the directory should be
        val s = store(dir = blocker)

        assertFalse(s.append(event(1)))
        assertNull(s.writeCrash(now, "boom"))
        assertTrue(s.loadEvents().isEmpty())
    }
}
