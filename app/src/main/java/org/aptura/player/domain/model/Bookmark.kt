package org.aptura.player.domain.model

/**
 * Pure Kotlin domain model representing a saved playback bookmark (FR-CTRL-011).
 * Invariant: ZERO Android framework dependencies.
 */
data class Bookmark(
    val id: Long = 0L,
    val mediaUri: String,
    val timestampMs: Long,
    val note: String = "",
    val createdAtSec: Long = 0L
)
