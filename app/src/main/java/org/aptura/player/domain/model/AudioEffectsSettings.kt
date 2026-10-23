package org.aptura.player.domain.model

/** What the user chose for the sound effects, in the shape that is saved and restored between launches. */
data class AudioEffectsSettings(
    val enabled: Boolean = false,
    val preset: EqualizerPreset = EqualizerPreset.FLAT,
    /** Per-band equalizer levels in milli-bels, index = band. Empty until the user has changed something. */
    val bandLevelsMb: List<Int> = emptyList(),
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val loudnessBoostPercent: Int = 0
)
