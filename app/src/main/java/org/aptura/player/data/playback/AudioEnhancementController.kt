package org.aptura.player.data.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.aptura.player.domain.model.AudioEffectsSettings
import org.aptura.player.domain.model.AudioEnhancementState
import org.aptura.player.domain.model.EqualizerBand
import org.aptura.player.domain.model.EqualizerPreset

/**
 * Owns the equalizer, bass boost, virtualizer and loudness effects bound to the player's audio session.
 *
 * The controller's [state] is the single source of truth: whatever the hardware reports at attach time never
 * overrides it (it used to, which showed "Flat" over bands that were really at +3 dB). Each effect, and each
 * equalizer band, is applied on its own, so one the device refuses cannot stop the rest. Effects are released when
 * the session changes or [releaseEffects] is called — the owner must call it, they are a limited system resource.
 * Gracefully degrades on emulators and OEMs with limited AudioFX support (no crashes).
 */
class AudioEnhancementController(
    private val backend: AudioEffectsBackend = AndroidAudioEffectsBackend
) {

    private val _state = MutableStateFlow(AudioEnhancementState())
    val state: StateFlow<AudioEnhancementState> = _state.asStateFlow()

    private var activeSessionId: Int = 0

    private var equalizer: EqualizerEffect? = null
    private var bassBoost: StrengthEffect? = null
    private var virtualizer: StrengthEffect? = null
    private var loudness: LoudnessEffect? = null

    /** Levels the user saved before any equalizer was attached (the band count is only known once one is). */
    private var savedBandLevels: List<Int> = emptyList()

    /** Loads what was saved last time. Call before the first [attachToSession] so the first sound already has it. */
    @Synchronized
    fun restore(saved: AudioEffectsSettings) {
        savedBandLevels = saved.bandLevelsMb
        _state.value = _state.value.copy(
            enabled = saved.enabled,
            preset = saved.preset,
            bassBoostPercent = saved.bassBoostPercent.coerceIn(0, 100),
            virtualizerPercent = saved.virtualizerPercent.coerceIn(0, 100),
            loudnessBoostPercent = saved.loudnessBoostPercent.coerceIn(0, 100),
            bands = if (equalizer != null) emptyList() else _state.value.bands
        )
        // Already attached (settings arrived late): rebuild the bands from the restored values and apply them.
        if (equalizer != null) {
            rebuildBands(equalizer!!)
        }
        apply()
    }

    /** The current choices in saveable form. */
    fun snapshot(): AudioEffectsSettings {
        val s = _state.value
        return AudioEffectsSettings(
            enabled = s.enabled,
            preset = s.preset,
            bandLevelsMb = s.bands.map { it.levelMilliBels }.ifEmpty { savedBandLevels },
            bassBoostPercent = s.bassBoostPercent,
            virtualizerPercent = s.virtualizerPercent,
            loudnessBoostPercent = s.loudnessBoostPercent
        )
    }

    @Synchronized
    fun attachToSession(sessionId: Int) {
        if (sessionId <= 0) return
        if (sessionId == activeSessionId && hasAnyEffect()) return
        releaseEffects()
        activeSessionId = sessionId

        equalizer = backend.equalizer(sessionId)
        bassBoost = backend.bassBoost(sessionId)
        virtualizer = backend.virtualizer(sessionId)
        loudness = backend.loudness(sessionId)

        val eq = equalizer
        if (eq != null) {
            rebuildBands(eq)
        } else if (_state.value.bands.isEmpty()) {
            // No hardware equalizer: still offer the standard five bands so the UI has something coherent to show.
            _state.value = _state.value.copy(bands = FALLBACK_BANDS)
        }
        apply()
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled)
        apply()
    }

    @Synchronized
    fun setPreset(preset: EqualizerPreset) {
        val current = _state.value
        val bands = current.bands
        val updated = if (preset != EqualizerPreset.CUSTOM && bands.isNotEmpty()) {
            val levels = presetLevels(preset, bands.size)
            bands.mapIndexed { i, band -> band.copy(levelMilliBels = levels.getOrElse(i) { 0 }.coerceIn(band.minMilliBels, band.maxMilliBels)) }
        } else {
            bands
        }
        // Choosing a sound is a request to hear it: with the master switch off it would otherwise do nothing.
        _state.value = current.copy(preset = preset, bands = updated, enabled = true)
        apply()
    }

    @Synchronized
    fun setBandLevel(bandIndex: Int, levelMilliBels: Int) {
        val bands = _state.value.bands.toMutableList()
        if (bandIndex !in bands.indices) return
        val band = bands[bandIndex]
        bands[bandIndex] = band.copy(levelMilliBels = levelMilliBels.coerceIn(band.minMilliBels, band.maxMilliBels))
        _state.value = _state.value.copy(
            bands = bands,
            preset = matchPreset(bands.map { it.levelMilliBels }),
            enabled = true
        )
        apply()
    }

    @Synchronized
    fun setBassBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _state.value = _state.value.copy(bassBoostPercent = clamped, enabled = _state.value.enabled || clamped > 0)
        apply()
    }

    @Synchronized
    fun setVirtualizer(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _state.value = _state.value.copy(virtualizerPercent = clamped, enabled = _state.value.enabled || clamped > 0)
        apply()
    }

    @Synchronized
    fun setLoudnessBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _state.value = _state.value.copy(loudnessBoostPercent = clamped, enabled = _state.value.enabled || clamped > 0)
        apply()
    }

    @Synchronized
    fun releaseEffects() {
        try { equalizer?.release() } catch (_: Throwable) {}
        try { bassBoost?.release() } catch (_: Throwable) {}
        try { virtualizer?.release() } catch (_: Throwable) {}
        try { loudness?.release() } catch (_: Throwable) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
        activeSessionId = 0
    }

    private fun hasAnyEffect() = equalizer != null || bassBoost != null || virtualizer != null || loudness != null

    /** Bands as the hardware describes them, but with OUR levels: from the preset, else saved/previous, else flat. */
    private fun rebuildBands(eq: EqualizerEffect) {
        val count = eq.bandCount
        val current = _state.value
        val levels: List<Int> = when {
            current.preset != EqualizerPreset.CUSTOM -> presetLevels(current.preset, count)
            current.bands.size == count -> current.bands.map { it.levelMilliBels }
            savedBandLevels.size == count -> savedBandLevels
            else -> List(count) { 0 }
        }
        val bands = (0 until count).map { i ->
            EqualizerBand(
                bandIndex = i,
                centerFreqHz = safe(0) { eq.centerFreqHz(i) },
                levelMilliBels = levels.getOrElse(i) { 0 }.coerceIn(eq.minLevelMb, eq.maxLevelMb),
                minMilliBels = eq.minLevelMb,
                maxMilliBels = eq.maxLevelMb
            )
        }
        _state.value = current.copy(bands = bands, preset = matchPreset(bands.map { it.levelMilliBels }))
    }

    /** Pushes [state] to whichever effects exist. Each effect, and each band, is isolated from the others' failures. */
    private fun apply() {
        val s = _state.value

        equalizer?.let { eq ->
            safe(Unit) { eq.setEnabled(s.enabled) }
            s.bands.forEach { band ->
                safe(Unit) { eq.setBandLevelMb(band.bandIndex, if (s.enabled) band.levelMilliBels else 0) }
            }
        }
        bassBoost?.let { effect ->
            safe(Unit) {
                if (effect.strengthSupported) effect.setStrength(s.bassBoostPercent * 10)
                effect.setEnabled(s.enabled && s.bassBoostPercent > 0)
            }
        }
        virtualizer?.let { effect ->
            safe(Unit) {
                if (effect.strengthSupported) effect.setStrength(s.virtualizerPercent * 10)
                effect.setEnabled(s.enabled && s.virtualizerPercent > 0)
            }
        }
        loudness?.let { effect ->
            safe(Unit) {
                effect.setTargetGainMb(s.loudnessBoostPercent * LOUDNESS_MB_PER_PERCENT)
                effect.setEnabled(s.enabled && s.loudnessBoostPercent > 0)
            }
        }
    }

    private inline fun <T> safe(fallback: T, block: () -> T): T = try { block() } catch (_: Throwable) { fallback }

    companion object {
        /**
         * 100 % = +6 dB. The platform's LoudnessEnhancer applies the gain without us knowing the content's headroom,
         * so the old +12 dB top end could clip hard on already-loud videos.
         */
        const val LOUDNESS_MB_PER_PERCENT = 6

        private val FALLBACK_BANDS = listOf(
            EqualizerBand(0, 60, 0),
            EqualizerBand(1, 230, 0),
            EqualizerBand(2, 910, 0),
            EqualizerBand(3, 3600, 0),
            EqualizerBand(4, 14000, 0)
        )

        /** The preset (other than Custom) whose curve these levels are, so the label never contradicts the faders. */
        fun matchPreset(levels: List<Int>): EqualizerPreset =
            EqualizerPreset.entries
                .filter { it != EqualizerPreset.CUSTOM }
                .firstOrNull { presetLevels(it, levels.size) == levels }
                ?: EqualizerPreset.CUSTOM

        fun presetLevels(preset: EqualizerPreset, numBands: Int): List<Int> = when (preset) {
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
            EqualizerPreset.CUSTOM -> List(numBands) { 0 }
        }
    }
}
