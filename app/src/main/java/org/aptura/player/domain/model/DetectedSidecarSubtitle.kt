package org.aptura.player.domain.model

/**
 * Pure Kotlin domain model representing a discovered sidecar subtitle file.
 * Invariant: ZERO Android framework or Media3 dependencies in domain/ (DEC-002, ADR-001).
 */
data class DetectedSidecarSubtitle(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val languageCode: String?,
    val languageLabel: String,
    val isDefault: Boolean = false,
    /** True when the subtitle file name equals the video name (no language/role tags). */
    val isExactMatch: Boolean = false
)
