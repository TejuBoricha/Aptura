package org.aptura.player.core.diagnostics

import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Thread-safe bounded ring buffer for diagnostic events.
 * Source of truth: spec/14_ANALYTICS_AND_SUPPORT.md §2 (Bounds: last sessions or 2 MB)
 */
class DiagnosticRingBuffer(
    val maxEventCount: Int = 500,
    val maxEstimatedBytes: Long = 2 * 1024 * 1024 // 2 MB bound
) {
    private val buffer = ConcurrentLinkedDeque<DiagnosticEvent>()
    @Volatile private var estimatedBytes: Long = 0

    fun record(event: DiagnosticEvent) {
        val eventSize = estimateEventBytes(event)
        
        while (buffer.size >= maxEventCount || (estimatedBytes + eventSize > maxEstimatedBytes && !buffer.isEmpty())) {
            val removed = buffer.pollFirst() ?: break
            estimatedBytes = maxOf(0L, estimatedBytes - estimateEventBytes(removed))
        }

        buffer.addLast(event)
        estimatedBytes += eventSize
    }

    fun getAllEvents(): List<DiagnosticEvent> {
        return buffer.toList()
    }

    fun getRedactedEvents(includeFileName: Boolean = false): List<DiagnosticEvent> {
        return buffer.map { event ->
            event.copy(payload = RedactionEngine.redactPayload(event.payload, includeFileName))
        }
    }

    fun clear() {
        buffer.clear()
        estimatedBytes = 0
    }

    val size: Int
        get() = buffer.size

    val totalBytes: Long
        get() = estimatedBytes

    private fun estimateEventBytes(event: DiagnosticEvent): Long {
        // Base overhead ~ 64 bytes + payload string lengths
        var bytes = 64L + event.code.name.length + event.sessionId.length
        for ((k, v) in event.payload) {
            bytes += k.length + v.length + 8
        }
        return bytes
    }
}
