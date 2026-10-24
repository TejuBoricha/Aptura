package org.aptura.player.core.playback

/**
 * The speeds the player offers and the speeds a user may choose as their default.
 *
 * These must be one list. When Settings offered a smaller range than the player, 0.5x and 1.75x could be used
 * for a single video but never set as a default (audit 2026-10-24).
 */
object PlaybackSpeedOptions {

    val all: List<Float> = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    /** The way every surface writes a speed: "1.0x", "1.25x". */
    fun label(speed: Float): String = "${speed}x"
}
