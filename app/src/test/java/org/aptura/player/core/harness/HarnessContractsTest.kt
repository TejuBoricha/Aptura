package org.aptura.player.core.harness

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying serialization and field contracts of spec/12A §3 test harnesses.
 */
class HarnessContractsTest {

    private val json = Json { prettyPrint = true }

    @Test
    fun seekBenchmarkRecord_serialization_retainsAllContractFields() {
        val record = SeekBenchmarkRecord(
            fixtureId = "a_A01_01_plain",
            deviceRecordId = "LAB-DEV-D2-PIXEL6",
            buildId = "aptura-b0-001",
            positionRequestedUs = 15_000_000L,
            positionLandedUs = 15_004_000L,
            landedOnKeyframe = false,
            seekLatencyMs = 84L,
            firstFrameLatencyMs = 92L,
            decodePath = SeekBenchmarkRecord.DecodePath.HW,
            droppedFramesDuringLanding = 0,
            renderNote = "nominal landing",
            thermalState = "nominal"
        )

        val encoded = json.encodeToString(SeekBenchmarkRecord.serializer(), record)
        assertTrue(encoded.contains("\"fixtureId\": \"a_A01_01_plain\""))
        assertTrue(encoded.contains("\"decodePath\": \"HW\""))

        val decoded = json.decodeFromString(SeekBenchmarkRecord.serializer(), encoded)
        assertEquals(record, decoded)
    }

    @Test
    fun avSyncBenchmarkRecord_serialization_retainsAllContractFields() {
        val record = AvSyncBenchmarkRecord(
            fixtureId = "inst_avsync_impulse_10s",
            deviceRecordId = "LAB-DEV-D1-MOTO-G",
            buildId = "aptura-b0-001",
            driftOffsetMs = 4.2,
            p95OffsetMs = 8.5,
            maxAbsOffsetMs = 14.1,
            sampleCadenceMs = 1000L,
            windowPhase = AvSyncBenchmarkRecord.WindowPhase.MID
        )

        val encoded = json.encodeToString(AvSyncBenchmarkRecord.serializer(), record)
        val decoded = json.decodeFromString(AvSyncBenchmarkRecord.serializer(), encoded)
        assertEquals(record, decoded)
    }

    @Test
    fun stateRestorationRecord_validatesKillpointRanges() {
        val record = StateRestorationRecord(
            killpointId = 5, // Mid-playback kill
            triggerMethod = "SIGKILL",
            operationTimeMs = 120_000L,
            relaunchTimeMs = 450L,
            survives = true,
            restoredState = "PLAYBACK_RESUMED_FROM_SAVED_POSITION",
            honestNoticeShown = false,
            journalConsistent = true
        )

        assertTrue(record.killpointId in 1..12)
        assertTrue(record.survives)
    }

    @Test
    fun scanAtScaleRecord_validatesPerformanceBudgets() {
        val record = ScanAtScaleRecord(
            stage = ScanAtScaleRecord.ScanStage.FULL_PIPELINE,
            enumerateMs = 120L,
            extractMs = 340L,
            thumbnailMs = 850L,
            itemsPerStage = 1000,
            uiFrameBudgetP95Ms = 11.2,
            cancelLatencyMs = 45L,
            phantomDatasetCheck = true,
            incrementalStreamingCheck = true
        )

        assertEquals(ScanAtScaleRecord.ScanStage.FULL_PIPELINE, record.stage)
        assertTrue(record.phantomDatasetCheck)
        assertTrue(record.incrementalStreamingCheck)
    }
}
