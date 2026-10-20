package org.aptura.player.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying RQ-10 privacy-preserving diagnostics and redaction rules.
 * Source of truth: spec/08A RQ-10 & spec/14_ANALYTICS_AND_SUPPORT.md
 */
class DiagnosticRedactionTest {

    private lateinit var ringBuffer: DiagnosticRingBuffer

    @Before
    fun setUp() {
        ringBuffer = DiagnosticRingBuffer(maxEventCount = 100)
    }

    @Test
    fun redactionEngine_stripsAbsolutePathsWhilePreservingExtension() {
        val rawPath = "/storage/emulated/0/DCIM/Family/birthday_party_2026.mp4"
        val redactedDefault = RedactionEngine.redactText(rawPath, includeFileName = false)

        assertEquals("[FILE_REDACTED].mp4", redactedDefault)
        assertFalse(redactedDefault.contains("birthday_party_2026"))
        assertFalse(redactedDefault.contains("/storage/emulated/0"))

        val redactedOptIn = RedactionEngine.redactText(rawPath, includeFileName = true)
        assertEquals("[PATH_REDACTED]/birthday_party_2026.mp4", redactedOptIn)
        assertFalse(redactedOptIn.contains("/storage/emulated/0"))
    }

    @Test
    fun redactionEngine_stripsEmailsAndIps() {
        val rawLog = "Contact user test.author+tag@gmail.com on server 192.168.1.55"
        val redacted = RedactionEngine.redactText(rawLog)

        assertTrue(redacted.contains("[EMAIL_REDACTED]"))
        assertTrue(redacted.contains("[IP_REDACTED]"))
        assertFalse(redacted.contains("test.author+tag@gmail.com"))
        assertFalse(redacted.contains("192.168.1.55"))
    }

    @Test
    fun ringBuffer_enforcesCapacityBoundaries() {
        val smallBuffer = DiagnosticRingBuffer(maxEventCount = 3)
        smallBuffer.record(DiagnosticEvent(EventCode.EV_001, "sess-1", payload = mapOf("seq" to "1")))
        smallBuffer.record(DiagnosticEvent(EventCode.EV_009, "sess-1", payload = mapOf("seq" to "2")))
        smallBuffer.record(DiagnosticEvent(EventCode.EV_011, "sess-1", payload = mapOf("seq" to "3")))
        assertEquals(3, smallBuffer.size)

        // Adding 4th event must evict the 1st
        smallBuffer.record(DiagnosticEvent(EventCode.EV_012, "sess-1", payload = mapOf("seq" to "4")))
        assertEquals(3, smallBuffer.size)

        val events = smallBuffer.getAllEvents()
        assertEquals("2", events[0].payload["seq"])
        assertEquals("3", events[1].payload["seq"])
        assertEquals("4", events[2].payload["seq"])
    }

    /**
     * RQ-10 Spike Adjudication Assertion:
     * Validates that the top-5 failure classes are 100% diagnosable from
     * redacted events alone without media bytes.
     * Pass criterion: >= 4/5 classes diagnosable (doc 08 §6.2).
     */
    @Test
    fun rq10_spike_top5FailureClasses_areDiagnosableWithoutMediaContent() {
        val testSessionId = "session-rq10-test"

        // 1. Class 1: Decoder initialization failure (e.g. HEVC 10-bit unsupported on D1 hardware)
        val eventClass1 = DiagnosticEvent(
            code = EventCode.EV_010,
            sessionId = testSessionId,
            payload = mapOf(
                "requestedPath" to "HW",
                "electedPath" to "NONE",
                "codec" to "video/hevc-10",
                "resolution" to "3840x2160",
                "notice" to "MediaCodecRenderer: Decoder init failed for OMX.qcom.video.decoder.hevc"
            )
        )

        // 2. Class 2: Unsupported codec / audio passthrough missing (e.g. DTS track)
        val eventClass2 = DiagnosticEvent(
            code = EventCode.EV_011,
            sessionId = testSessionId,
            payload = mapOf(
                "errorCode" to "ERR_UNSUPPORTED_CODEC",
                "mimeType" to "audio/vnd.dts",
                "stage" to "audio_init",
                "devicePassthroughAvailable" to "false"
            )
        )

        // 3. Class 3: Corrupted container (truncated fMP4 or missing moov atom)
        val eventClass3 = DiagnosticEvent(
            code = EventCode.EV_011,
            sessionId = testSessionId,
            payload = mapOf(
                "errorCode" to "ERR_PARSER_CORRUPT",
                "container" to "MP4",
                "parserMessage" to "Atom moov not found before EOF",
                "path" to "/storage/emulated/0/Download/broken_clip.mp4"
            )
        )

        // 4. Class 4: Permission / Storage Access revoked
        val eventClass4 = DiagnosticEvent(
            code = EventCode.EV_006,
            sessionId = testSessionId,
            payload = mapOf(
                "stage" to "extract",
                "errorClass" to "SecurityException",
                "message" to "Permission denial: reading content://media/external/video/media/123",
                "path" to "/storage/emulated/0/Movies/sample.mkv"
            )
        )

        // 5. Class 5: I/O Timeout / Corrupt sector on external SD card
        val eventClass5 = DiagnosticEvent(
            code = EventCode.EV_011,
            sessionId = testSessionId,
            payload = mapOf(
                "errorCode" to "ERR_IO_READ_TIMEOUT",
                "stage" to "decode",
                "storageType" to "removable_sdcard",
                "bytesRead" to "2048000"
            )
        )

        val testEvents = listOf(eventClass1, eventClass2, eventClass3, eventClass4, eventClass5)
        for (evt in testEvents) {
            ringBuffer.record(evt)
        }

        // Export bundle
        val deviceContext = DiagnosticBundleExporter.DeviceContext(
            manufacturer = "Google",
            model = "Pixel 6",
            apiLevel = 34,
            abi = "arm64-v8a",
            locale = "en-US"
        )
        val bundle = DiagnosticBundleExporter.createBundle(ringBuffer, deviceContext)
        val markdown = DiagnosticBundleExporter.exportToMarkdown(bundle)

        // Verify zero unredacted storage paths remain in export
        assertFalse("Bundle must not expose raw user storage path", markdown.contains("/storage/emulated/0"))
        assertFalse("Bundle must not contain raw filename without opt-in", markdown.contains("broken_clip.mp4"))

        // Verify all 5 failure classes are identifiable from the diagnostic fields alone
        var diagnosableCount = 0

        // Check Class 1
        if (markdown.contains("video/hevc-10") && markdown.contains("Decoder init failed")) {
            diagnosableCount++
        }
        // Check Class 2
        if (markdown.contains("audio/vnd.dts") && markdown.contains("ERR_UNSUPPORTED_CODEC")) {
            diagnosableCount++
        }
        // Check Class 3
        if (markdown.contains("Atom moov not found") && markdown.contains("ERR_PARSER_CORRUPT")) {
            diagnosableCount++
        }
        // Check Class 4
        if (markdown.contains("SecurityException") && markdown.contains("Permission denial")) {
            diagnosableCount++
        }
        // Check Class 5
        if (markdown.contains("ERR_IO_READ_TIMEOUT") && markdown.contains("removable_sdcard")) {
            diagnosableCount++
        }

        // Assert RQ-10 Pass threshold: >= 4 of 5 (doc 08 §6.2)
        assertEquals("All 5 failure classes must be diagnosable", 5, diagnosableCount)
        assertTrue("RQ-10 pass threshold (>= 4/5) met", diagnosableCount >= 4)
    }
}
