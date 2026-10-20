@file:Suppress("DEPRECATION")

package org.aptura.player.data.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.aptura.player.domain.model.AudioEnhancementState
import org.aptura.player.domain.model.EqualizerBand
import org.aptura.player.domain.model.EqualizerPreset

/**
 * Controller managing hardware-accelerated audio effects via Android AudioFX framework.
 * Binds Equalizer, BassBoost, Virtualizer, and LoudnessEnhancer to the active ExoPlayer audioSessionId.
 * Gracefully degrades on emulators and OEMs with limited AudioFX HAL support (no crashes).
 */
class AudioEnhancementController {

    private val _state = MutableStateFlow(AudioEnhancementState())
    val state: StateFlow<AudioEnhancementState> = _state.asStateFlow()

    private var activeSessionId: Int = 0

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    @Synchronized
    fun attachToSession(sessionId: Int) {
        if (sessionId <= 0 || sessionId == activeSessionId) return
        releaseEffects()
        activeSessionId = sessionId

        try {
            equalizer = Equalizer(0, sessionId).apply {
                val numBands = numberOfBands.toInt()
                val minLevel = bandLevelRange[0].toInt()
                val maxLevel = bandLevelRange[1].toInt()
                val bandList = mutableListOf<EqualizerBand>()
                for (i in 0 until numBands) {
                    val centerFreqHz = getCenterFreq(i.toShort()) / 1000
                    val currentLevel = getBandLevel(i.toShort()).toInt()
                    bandList.add(
                        EqualizerBand(
                            bandIndex = i,
                            centerFreqHz = centerFreqHz,
                            levelMilliBels = currentLevel,
                            minMilliBels = minLevel,
                            maxMilliBels = maxLevel
                        )
                    )
                }
                _state.value = _state.value.copy(bands = bandList)
            }
        } catch (_: Throwable) {
            equalizer = null
            // Fallback: provide default standard 5-band configuration if hardware equalizer is unavailable
            if (_state.value.bands.isEmpty()) {
                val fallbackBands = listOf(
                    EqualizerBand(0, 60, 0),
                    EqualizerBand(1, 230, 0),
                    EqualizerBand(2, 910, 0),
                    EqualizerBand(3, 3600, 0),
                    EqualizerBand(4, 14000, 0)
                )
                _state.value = _state.value.copy(bands = fallbackBands)
            }
        }

        try {
            bassBoost = BassBoost(0, sessionId)
        } catch (_: Throwable) {
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(0, sessionId)
        } catch (_: Throwable) {
            virtualizer = null
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId)
        } catch (_: Throwable) {
            loudnessEnhancer = null
        }

        reapplyState()
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled)
        reapplyState()
    }

    @Synchronized
    fun setPreset(preset: EqualizerPreset) {
        _state.value = _state.value.copy(preset = preset)
        val bands = _state.value.bands.toMutableList()
        val numBands = bands.size

        if (numBands > 0) {
            val levels = calculatePresetLevels(preset, numBands)
            for (i in 0 until numBands) {
                bands[i] = bands[i].copy(levelMilliBels = levels.getOrElse(i) { 0 })
            }
            _state.value = _state.value.copy(bands = bands)
        }
        reapplyState()
    }

    @Synchronized
    fun setBandLevel(bandIndex: Int, levelMilliBels: Int) {
        val bands = _state.value.bands.toMutableList()
        if (bandIndex in bands.indices) {
            bands[bandIndex] = bands[bandIndex].copy(
                levelMilliBels = levelMilliBels.coerceIn(bands[bandIndex].minMilliBels, bands[bandIndex].maxMilliBels)
            )
            _state.value = _state.value.copy(bands = bands, preset = EqualizerPreset.CUSTOM)
            try {
                equalizer?.takeIf { _state.value.enabled }?.setBandLevel(bandIndex.toShort(), levelMilliBels.toShort())
            } catch (_: Throwable) {}
        }
    }

    @Synchronized
    fun setBassBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _state.value = _state.value.copy(bassBoostPercent = clamped)
        try {
            bassBoost?.let {
                it.enabled = _state.value.enabled && clamped > 0
                if (it.strengthSupported) {
                    it.setStrength((clamped * 10).toShort())
                }
            }
        } catch (_: Throwable) {}
    }

    @Synchronized
    fun setVirtualizer(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _state.value = _state.value.copy(virtualizerPercent = clamped)
        try {
            virtualizer?.let {
                it.enabled = _state.value.enabled && clamped > 0
                if (it.strengthSupported) {
                    it.setStrength((clamped * 10).toShort())
                }
            }
        } catch (_: Throwable) {}
    }

    @Synchronized
    fun setLoudnessBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _state.value = _state.value.copy(loudnessBoostPercent = clamped)
        try {
            loudnessEnhancer?.let {
                it.enabled = _state.value.enabled && clamped > 0
                // Target gain up to 1200 mB (~12 dB boost)
                it.setTargetGain((clamped * 12))
            }
        } catch (_: Throwable) {}
    }

    private fun reapplyState() {
        val current = _state.value
        try {
            equalizer?.let { eq ->
                eq.enabled = current.enabled
                if (current.enabled) {
                    current.bands.forEach { band ->
                        eq.setBandLevel(band.bandIndex.toShort(), band.levelMilliBels.toShort())
                    }
                }
            }
        } catch (_: Throwable) {}

        try {
            bassBoost?.let { bb ->
                bb.enabled = current.enabled && current.bassBoostPercent > 0
                if (bb.strengthSupported) {
                    bb.setStrength((current.bassBoostPercent * 10).toShort())
                }
            }
        } catch (_: Throwable) {}

        try {
            virtualizer?.let { virt ->
                virt.enabled = current.enabled && current.virtualizerPercent > 0
                if (virt.strengthSupported) {
                    virt.setStrength((current.virtualizerPercent * 10).toShort())
                }
            }
        } catch (_: Throwable) {}

        try {
            loudnessEnhancer?.let { le ->
                le.enabled = current.enabled && current.loudnessBoostPercent > 0
                le.setTargetGain(current.loudnessBoostPercent * 12)
            }
        } catch (_: Throwable) {}
    }

    private fun calculatePresetLevels(preset: EqualizerPreset, numBands: Int): List<Int> {
        return when (preset) {
            EqualizerPreset.FLAT -> List(numBands) { 0 }
            EqualizerPreset.BASS_BOOST -> when (numBands) {
                5 -> listOf(600, 400, 100, 0, 0)
                else -> List(numBands) { if (it < numBands / 2) 500 else 0 }
            }
            EqualizerPreset.VOCAL_BOOST -> when (numBands) {
                5 -> listOf(-200, 100, 500, 400, 100)
                else -> List(numBands) { if (it in (numBands / 3)..(numBands * 2 / 3)) 400 else 0 }
            }
            EqualizerPreset.ROCK -> when (numBands) {
                5 -> listOf(400, 200, -100, 200, 400)
                else -> List(numBands) { if (it == 0 || it == numBands - 1) 400 else 100 }
            }
            EqualizerPreset.POP -> when (numBands) {
                5 -> listOf(-100, 200, 400, 200, -100)
                else -> List(numBands) { if (it == numBands / 2) 400 else 100 }
            }
            EqualizerPreset.CLASSICAL -> when (numBands) {
                5 -> listOf(300, 200, -100, 200, 300)
                else -> List(numBands) { if (it == 0 || it == numBands - 1) 300 else 0 }
            }
            EqualizerPreset.JAZZ -> when (numBands) {
                5 -> listOf(200, 100, 200, 300, 200)
                else -> List(numBands) { 200 }
            }
            EqualizerPreset.CUSTOM -> _state.value.bands.map { it.levelMilliBels }
        }
    }

    @Synchronized
    fun releaseEffects() {
        try { equalizer?.release() } catch (_: Throwable) {}
        try { bassBoost?.release() } catch (_: Throwable) {}
        try { virtualizer?.release() } catch (_: Throwable) {}
        try { loudnessEnhancer?.release() } catch (_: Throwable) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        activeSessionId = 0
    }
}
