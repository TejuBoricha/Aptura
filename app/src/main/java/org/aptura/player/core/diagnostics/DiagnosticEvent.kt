package org.aptura.player.core.diagnostics

import kotlinx.serialization.Serializable

/**
 * Diagnostic event codes EV-001 through EV-020
 * Source of truth: spec/14_ANALYTICS_AND_SUPPORT.md §3
 */
enum class EventCode(val eventName: String, val category: EventCategory) {
    EV_001("app.start", EventCategory.APP),
    EV_002("app.migration", EventCategory.APP),
    EV_003("lib.scan.start", EventCategory.LIB),
    EV_004("lib.scan.progress", EventCategory.LIB),
    EV_005("lib.scan.end", EventCategory.LIB),
    EV_006("lib.scan.file_error", EventCategory.LIB),
    EV_007("lib.identity.resolve", EventCategory.LIB),
    EV_008("lib.thumb_fail", EventCategory.LIB),
    EV_009("play.session.start", EventCategory.PLAY),
    EV_010("play.decode.elect", EventCategory.PLAY),
    EV_011("play.error", EventCategory.PLAY),
    EV_012("play.session.end", EventCategory.PLAY),
    EV_013("play.seek", EventCategory.PLAY),
    EV_014("play.sync_shift", EventCategory.PLAY),
    EV_015("sub.load", EventCategory.SUB),
    EV_016("sub.font_fallback", EventCategory.SUB),
    EV_017("aud.track.elect", EventCategory.AUD),
    EV_018("plat.lifecycle", EventCategory.PLAT),
    EV_019("set.migration", EventCategory.SET),
    EV_020("diag.export", EventCategory.DIAG)
}

enum class EventCategory {
    APP,
    LIB,
    PLAY,
    SUB,
    AUD,
    PLAT,
    SET,
    DIAG
}

@Serializable
data class DiagnosticEvent(
    val code: EventCode,
    val sessionId: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val payload: Map<String, String> = emptyMap()
)
