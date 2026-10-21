package org.aptura.player.core.diagnostics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Q-02: uncaught coroutine failures must be recorded as redacted local diagnostics events
 * instead of crashing the process.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ApturaErrorHandlerTest {

    @Test
    fun report_recordsSourceAndExceptionClassOnly_neverTheMessage() {
        val recorded = mutableListOf<Pair<EventCode, Map<String, String>>>()

        ApturaErrorHandler.report(
            source = "player_vm",
            throwable = IllegalStateException("content://media/external/video/42 kids_birthday.mp4"),
            sink = { code, payload -> recorded.add(code to payload) }
        )

        assertEquals(1, recorded.size)
        assertEquals(EventCode.EV_011, recorded[0].first)
        assertEquals("player_vm", recorded[0].second["source"])
        assertEquals("IllegalStateException", recorded[0].second["error"])
        assertFalse(
            "Exception messages can embed URIs / file names and must never be logged",
            recorded[0].second.values.any { it.contains("content://") || it.contains("kids_birthday") }
        )
    }

    @Test
    fun report_ignoresCancellation() {
        var called = false
        ApturaErrorHandler.report("library_vm", CancellationException("scope closed"), sink = { _, _ -> called = true })
        assertFalse(called)
    }

    @Test
    fun create_handlerSwallowsUncaughtLaunchFailure_andRecordsIt() = runTest {
        val recorded = mutableListOf<Map<String, String>>()
        val handler = ApturaErrorHandler.create("library_vm", EventCode.EV_006) { code, payload ->
            assertEquals(EventCode.EV_006, code)
            recorded.add(payload)
        }
        // Mirrors viewModelScope: SupervisorJob + handler.
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler) + handler)

        scope.launch { throw IllegalArgumentException("boom") }
        advanceUntilIdle()

        assertEquals(1, recorded.size)
        assertEquals("IllegalArgumentException", recorded[0]["error"])
        assertTrue(scope.coroutineContext[Job]!!.isActive)
    }
}
