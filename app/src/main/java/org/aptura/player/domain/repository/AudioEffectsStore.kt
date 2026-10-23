package org.aptura.player.domain.repository

import org.aptura.player.domain.model.AudioEffectsSettings

/** Where the sound-effect choices are kept between launches. */
interface AudioEffectsStore {
    suspend fun load(): AudioEffectsSettings
    suspend fun save(settings: AudioEffectsSettings)
}
