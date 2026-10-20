package org.aptura.player.core.harness

import kotlinx.serialization.Serializable

/**
 * Aptura Test Harness Contract Data Models
 * Source of truth: spec/12A_B0_INSTRUMENTATION.md §3 & spec/13_BENCHMARKING_AND_TELEMETRY_METHODS.md
 */

/**
 * 3.1 Seek & first-frame harness record (TST-SEEK · MB-07 · NFR-PERF-003/004)
 */
@Serializable
data class SeekBenchmarkRecord(
    val fixtureId: String,
    val deviceRecordId: String,
    val buildId: String,
    val positionRequestedUs: Long,
    val positionLandedUs: Long,
    val landedOnKeyframe: Boolean,
    val seekLatencyMs: Long,
    val firstFrameLatencyMs: Long,
    val decodePath: DecodePath,
    val droppedFramesDuringLanding: Int,
    val renderNote: String = "",
    val thermalState: String = "nominal",
    val timestampMs: Long = System.currentTimeMillis()
) {
    enum class DecodePath {
        HW,
        SW_PLATFORM,
        MIXED
    }
}

/**
 * 3.2 AV-sync harness record (TST-SYNC · MB-08)
 */
@Serializable
data class AvSyncBenchmarkRecord(
    val fixtureId: String,
    val deviceRecordId: String,
    val buildId: String,
    val driftOffsetMs: Double,
    val p95OffsetMs: Double,
    val maxAbsOffsetMs: Double,
    val sampleCadenceMs: Long,
    val windowPhase: WindowPhase,
    val timestampMs: Long = System.currentTimeMillis()
) {
    enum class WindowPhase {
        START,
        MID,
        END
    }
}

/**
 * 3.3 State-restoration matrix record (TST-REST · RQ-07)
 */
@Serializable
data class StateRestorationRecord(
    val killpointId: Int, // 1 to 12 per spec/12A §3.3 table
    val triggerMethod: String, // e.g. "SIGKILL", "low_memory_kill", "activity_destroy"
    val operationTimeMs: Long,
    val relaunchTimeMs: Long,
    val survives: Boolean,
    val restoredState: String,
    val honestNoticeShown: Boolean,
    val journalConsistent: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * 3.4 Scan-at-scale harness record (TST-LIBV · MB-03/04 · NFR-PERF-001/005/007)
 */
@Serializable
data class ScanAtScaleRecord(
    val stage: ScanStage,
    val enumerateMs: Long,
    val extractMs: Long,
    val thumbnailMs: Long,
    val itemsPerStage: Int,
    val uiFrameBudgetP95Ms: Double,
    val cancelLatencyMs: Long,
    val phantomDatasetCheck: Boolean,
    val incrementalStreamingCheck: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
) {
    enum class ScanStage {
        ENUMERATE,
        EXTRACT,
        THUMBNAIL,
        FULL_PIPELINE
    }
}
