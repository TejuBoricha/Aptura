package org.aptura.player.core.diagnostics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * App-level coroutine safety net (Q-02, spec/04 FR-PLAY-003 error taxonomy).
 *
 * An uncaught exception in any `viewModelScope.launch` would otherwise crash the process
 * (a Play-vitals liability). This handler converts it into a privacy-preserving local
 * diagnostics event (spec/14): only the source label and exception *class name* are recorded —
 * never the message, because messages routinely embed URIs / file names.
 */
object ApturaErrorHandler {

    /**
     * Records a failure into the on-device ring buffer. Safe to call from any thread.
     * Cancellation is control flow, not a failure, and is ignored.
     */
    fun report(
        source: String,
        throwable: Throwable,
        code: EventCode = EventCode.EV_011,
        sink: (EventCode, Map<String, String>) -> Unit = { c, p -> ApturaDiagnostics.logEvent(c, payload = p) }
    ) {
        if (throwable is CancellationException) return
        sink(
            code,
            mapOf(
                "source" to source,
                "error" to throwable.javaClass.simpleName
            )
        )
    }

    /** Builds a [CoroutineExceptionHandler] that routes failures to [report] instead of crashing. */
    fun create(
        source: String,
        code: EventCode = EventCode.EV_011,
        sink: (EventCode, Map<String, String>) -> Unit = { c, p -> ApturaDiagnostics.logEvent(c, payload = p) }
    ): CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        report(source, throwable, code, sink)
    }
}
