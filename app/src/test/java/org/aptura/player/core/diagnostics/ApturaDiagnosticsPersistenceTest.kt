package org.aptura.player.core.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * The real [ApturaDiagnostics] against real files, simulating a process restart by starting a second "process" on the
 * same directory with an empty memory. Audit P1-18: after a crash the export said "No errors logged in recent sessions".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApturaDiagnosticsPersistenceTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var originalHandler: Thread.UncaughtExceptionHandler

    private class RecordingDelegate : Thread.UncaughtExceptionHandler {
        var received: Throwable? = null
        override fun uncaughtException(t: Thread, e: Throwable) { received = e }
    }

    @Before
    fun setUp() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler() ?: Thread.UncaughtExceptionHandler { _, _ -> }
        ApturaDiagnostics.ringBuffer.clear()
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        ApturaDiagnostics.ringBuffer.clear()
    }

    private fun dir() = File(folder.root, "diagnostics")

    /** Starts a "process": an empty memory, a fresh store on the shared directory, writes executed immediately. */
    private fun startProcess(previousExit: String? = null, crashHandler: Boolean = false) {
        ApturaDiagnostics.ringBuffer.clear()
        ApturaDiagnostics.initialize(
            context,
            diagnosticStore = DiagnosticStore(dir()),
            executor = { it.run() },
            installCrashHandler = crashHandler,
            previousExit = previousExit
        )
    }

    @Test
    fun events_surviveARestart_andEachRunHasItsOwnSessionId() {
        startProcess()
        val firstSession = ApturaDiagnostics.currentSessionId
        ApturaDiagnostics.logEvent(EventCode.EV_011, payload = mapOf("source" to "player_vm", "error" to "IOException"))

        startProcess(previousExit = "crash")

        val events = ApturaDiagnostics.ringBuffer.getAllEvents()
        assertTrue(
            "the first run's error is still there",
            events.any { it.code == EventCode.EV_011 && it.payload["source"] == "player_vm" && it.sessionId == firstSession }
        )
        assertNotEquals("a new run is a new session", firstSession, ApturaDiagnostics.currentSessionId)
        val start = events.last { it.code == EventCode.EV_001 }
        assertEquals("crash", start.payload["previousExit"])
        assertEquals(ApturaDiagnostics.currentSessionId, start.sessionId)
    }

    @Test
    fun aCrash_isInTheNextRunsExport_insteadOfNoErrorsLogged() {
        val delegate = RecordingDelegate()
        Thread.setDefaultUncaughtExceptionHandler(delegate)
        startProcess(crashHandler = true)

        // The real thing a dying thread does: call the process's default handler.
        val handler = Thread.getDefaultUncaughtExceptionHandler()!!
        handler.uncaughtException(Thread.currentThread(), IllegalStateException("opened /storage/emulated/0/Movies/private.mp4"))
        assertNotNull("Android's own handler must still run", delegate.received)

        startProcess(previousExit = "crash") // the next launch
        val report = ApturaDiagnostics.generateDiagnosticMarkdown(context)

        assertFalse("the old behaviour", report.contains("No errors logged in recent sessions"))
        assertTrue(report.contains("exception: java.lang.IllegalStateException"))
        assertTrue(report.contains("Crash Log (1 recorded"))
        assertFalse("never the message", report.contains("private.mp4"))
    }

    @Test
    fun theReportStatesTheRealBuild_notAHardCodedOne() {
        startProcess()

        val report = ApturaDiagnostics.generateDiagnosticMarkdown(context)

        assertFalse(report.contains("aptura-part63"))
        val installed = context.packageManager.getPackageInfo(context.packageName, 0)
        assertTrue("version comes from the package: ${installed.versionName}", report.contains("${installed.versionName} ("))
        assertTrue(report.contains("**Build Type:** debug") || report.contains("**Build Type:** release"))
    }

    @Test
    fun theCrashPrompt_appearsOnceAfterACrash_andStopsOnceDismissed() {
        startProcess(crashHandler = true)
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), RuntimeException("x"))

        startProcess()
        val pending = ApturaDiagnostics.pendingCrashNotice()
        assertNotNull(pending)

        ApturaDiagnostics.acknowledgeCrash(pending!!.name)

        assertNull(ApturaDiagnostics.pendingCrashNotice())
        assertTrue("the report itself is kept", DiagnosticStore(dir()).loadCrashes().isNotEmpty())
    }

    @Test
    fun deletingDiagnosticData_clearsMemoryAndDisk() {
        startProcess(crashHandler = true)
        ApturaDiagnostics.logEvent(EventCode.EV_011, payload = mapOf("source" to "x"))
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), RuntimeException("x"))

        ApturaDiagnostics.clearAll()

        assertEquals(0, ApturaDiagnostics.ringBuffer.size)
        assertTrue(DiagnosticStore(dir()).loadEvents().isEmpty())
        assertTrue(DiagnosticStore(dir()).loadCrashes().isEmpty())
    }

    @Test
    fun initialisingTwice_doesNotStackCrashHandlers() {
        val delegate = RecordingDelegate()
        Thread.setDefaultUncaughtExceptionHandler(delegate)

        startProcess(crashHandler = true)
        startProcess(crashHandler = true)

        val handler = Thread.getDefaultUncaughtExceptionHandler() as CrashHandler
        assertSame("the original handler stays the only delegate", delegate, handler.delegate)
    }
}
