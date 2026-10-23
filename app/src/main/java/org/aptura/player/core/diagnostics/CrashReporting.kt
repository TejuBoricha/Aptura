package org.aptura.player.core.diagnostics

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plain-text crash report (spec/14 §2: "stored as plain text + stack").
 *
 * Deliberately contains NO exception messages: they routinely embed file names, URIs and search text, and the spec
 * treats paths and names as sensitive. What is left — exception class names, thread, and the stack (class, method, file,
 * line of code) — identifies the defect without identifying the user's media.
 */
object CrashReportFormatter {
    private const val MAX_FRAMES = 40
    private const val MAX_CAUSES = 4

    fun format(
        timestampMs: Long,
        sessionId: String,
        app: DiagnosticBundleExporter.AppContext,
        device: DiagnosticBundleExporter.DeviceContext,
        threadName: String,
        throwable: Throwable
    ): String = buildString {
        appendLine("Aptura crash report")
        appendLine("time: $timestampMs (epoch ms)")
        appendLine("session: $sessionId")
        appendLine("app: ${app.appVersion} ${app.buildType} ${app.buildHash}")
        appendLine("device: ${device.manufacturer} ${device.model} | API ${device.apiLevel} | ${device.abi} | ${device.locale}")
        appendLine("thread: $threadName")
        appendLine()

        val seen = HashSet<Throwable>()
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && seen.add(current) && depth < MAX_CAUSES) {
            appendLine(if (depth == 0) "exception: ${current.javaClass.name}" else "caused by: ${current.javaClass.name}")
            val frames = current.stackTrace
            frames.take(MAX_FRAMES).forEach { appendLine("    at $it") }
            if (frames.size > MAX_FRAMES) appendLine("    ... ${frames.size - MAX_FRAMES} more")
            current = current.cause
            depth++
        }
    }

    /** One line for the event log: where it happened, without any message. */
    fun topFrame(throwable: Throwable): String = throwable.stackTrace.firstOrNull()?.toString().orEmpty()
}

/**
 * Installed as the process's default uncaught-exception handler. It writes the crash report and a log event
 * SYNCHRONOUSLY (the process is about to die, so a background queue would never run), then hands the exception on to the
 * handler that was there before so Android's own crash handling — the dialog, the process kill, Play vitals — still happens.
 *
 * It can never make a crash worse: any failure while recording is swallowed.
 */
class CrashHandler(
    private val store: DiagnosticStore,
    private val ringBuffer: DiagnosticRingBuffer,
    private val sessionId: () -> String,
    private val app: () -> DiagnosticBundleExporter.AppContext,
    private val device: () -> DiagnosticBundleExporter.DeviceContext,
    val delegate: Thread.UncaughtExceptionHandler?,
    private val clock: () -> Long = System::currentTimeMillis
) : Thread.UncaughtExceptionHandler {

    private val recording = AtomicBoolean(false)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // A crash while recording a crash must not recurse.
        if (recording.compareAndSet(false, true)) {
            try {
                record(thread, throwable)
            } catch (_: Throwable) {
                // nothing sensible left to do
            } finally {
                recording.set(false)
            }
        }
        delegate?.uncaughtException(thread, throwable)
    }

    private fun record(thread: Thread, throwable: Throwable) {
        val now = clock()
        val session = sessionId()
        store.writeCrash(
            now,
            CrashReportFormatter.format(now, session, app(), device(), thread.name, throwable)
        )
        val event = DiagnosticEvent(
            code = EventCode.EV_018,
            sessionId = session,
            timestampMs = now,
            payload = mapOf(
                "event" to "crash",
                "exception" to throwable.javaClass.name,
                "thread" to thread.name,
                "topFrame" to CrashReportFormatter.topFrame(throwable)
            )
        )
        ringBuffer.record(event)
        store.append(event)
    }
}
