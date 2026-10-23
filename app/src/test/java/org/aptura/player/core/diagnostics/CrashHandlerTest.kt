package org.aptura.player.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * An uncaught exception must leave a crash report on disk (class names and stack — never the message, which routinely
 * contains file names) and still reach Android's own crash handling.
 */
class CrashHandlerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val app = DiagnosticBundleExporter.AppContext("1.2.3 (45)", "release", "abc12345")
    private val device = DiagnosticBundleExporter.DeviceContext("Google", "Pixel 8", 35, "arm64-v8a", "en-US")

    private class RecordingDelegate : Thread.UncaughtExceptionHandler {
        var received: Throwable? = null
        override fun uncaughtException(t: Thread, e: Throwable) { received = e }
    }

    private fun handler(
        store: DiagnosticStore,
        ring: DiagnosticRingBuffer = DiagnosticRingBuffer(),
        delegate: Thread.UncaughtExceptionHandler? = null
    ) = CrashHandler(store, ring, sessionId = { "s-test" }, app = { app }, device = { device }, delegate = delegate, clock = { 5_000L })

    private fun newStore(dir: File = File(folder.root, "d")) = DiagnosticStore(dir, clock = { 5_000L })

    private fun crash(): Throwable =
        IllegalStateException("could not open /storage/emulated/0/Movies/private_clip.mp4 for jane@example.com")

    @Test
    fun aCrashLeavesAReport_withClassAndStack_butNoMessage() {
        val store = newStore()

        handler(store).uncaughtException(Thread.currentThread(), crash())

        val text = store.loadCrashes().single().text
        assertTrue(text.contains("exception: java.lang.IllegalStateException"))
        assertTrue("the stack is there", text.contains("CrashHandlerTest"))
        assertTrue(text.contains("session: s-test"))
        assertTrue(text.contains("app: 1.2.3 (45) release abc12345"))
        assertFalse("the message must never be written", text.contains("private_clip"))
        assertFalse(text.contains("jane@example.com"))
    }

    @Test
    fun theCrashAlsoAppearsInTheEventLog_forThisAndTheNextSession() {
        val store = newStore()
        val ring = DiagnosticRingBuffer()

        handler(store, ring).uncaughtException(Thread.currentThread(), crash())

        val inMemory = ring.getAllEvents().single()
        assertEquals(EventCode.EV_018, inMemory.code)
        assertEquals("crash", inMemory.payload["event"])
        assertEquals("java.lang.IllegalStateException", inMemory.payload["exception"])
        assertEquals("the next process sees it too", "crash", store.loadEvents().single().payload["event"])
    }

    @Test
    fun androidsOwnHandlerStillRuns() {
        val delegate = RecordingDelegate()
        val error = crash()

        handler(newStore(), delegate = delegate).uncaughtException(Thread.currentThread(), error)

        assertEquals(error, delegate.received)
    }

    @Test
    fun aStorageFailure_cannotStopTheCrashFromReachingTheDelegate_orThrow() {
        val blocker = folder.newFile("blocked") // storage that cannot be written
        val delegate = RecordingDelegate()
        val error = crash()

        handler(newStore(blocker), delegate = delegate).uncaughtException(Thread.currentThread(), error)

        assertEquals(error, delegate.received)
    }

    @Test
    fun aCauseChainIsReportedWithoutMessages_andALongStackIsTruncated() {
        val root = RuntimeException("secret root message")
        val top = IllegalArgumentException("secret top message", root)

        val text = CrashReportFormatter.format(1L, "s", app, device, "main", top)

        assertTrue(text.contains("exception: java.lang.IllegalArgumentException"))
        assertTrue(text.contains("caused by: java.lang.RuntimeException"))
        assertFalse(text.contains("secret"))
        val deep = Throwable().also { it.stackTrace = Array(100) { i -> StackTraceElement("a.B", "m$i", "B.kt", i) } }
        assertTrue(CrashReportFormatter.format(1L, "s", app, device, "main", deep).contains("... 60 more"))
    }
}
