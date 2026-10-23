@file:Suppress("DEPRECATION")

package org.aptura.player.data.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer

/**
 * The thin edge between [AudioEnhancementController] and Android's AudioFX framework. The controller holds the logic
 * (what is on, at which level, what survives a new audio session); this layer only creates and drives the effects, so
 * that logic can be tested without an audio HAL. Every `create` returns null when the device cannot provide the effect.
 */
interface AudioEffectsBackend {
    fun equalizer(sessionId: Int): EqualizerEffect?
    fun bassBoost(sessionId: Int): StrengthEffect?
    fun virtualizer(sessionId: Int): StrengthEffect?
    fun loudness(sessionId: Int): LoudnessEffect?
}

interface EqualizerEffect {
    val bandCount: Int
    val minLevelMb: Int
    val maxLevelMb: Int
    fun centerFreqHz(band: Int): Int
    fun bandLevelMb(band: Int): Int
    fun setBandLevelMb(band: Int, levelMb: Int)
    fun setEnabled(enabled: Boolean)
    fun release()
}

interface StrengthEffect {
    val strengthSupported: Boolean

    /** 0..1000 */
    fun setStrength(permille: Int)
    fun setEnabled(enabled: Boolean)
    fun release()
}

interface LoudnessEffect {
    fun setTargetGainMb(milliBels: Int)
    fun setEnabled(enabled: Boolean)
    fun release()
}

/** The real thing. Constructors throw on devices/emulators without the effect; that becomes "not available". */
object AndroidAudioEffectsBackend : AudioEffectsBackend {

    override fun equalizer(sessionId: Int): EqualizerEffect? = try {
        val eq = Equalizer(0, sessionId)
        object : EqualizerEffect {
            override val bandCount = eq.numberOfBands.toInt()
            override val minLevelMb = eq.bandLevelRange[0].toInt()
            override val maxLevelMb = eq.bandLevelRange[1].toInt()
            override fun centerFreqHz(band: Int) = eq.getCenterFreq(band.toShort()) / 1000
            override fun bandLevelMb(band: Int) = eq.getBandLevel(band.toShort()).toInt()
            override fun setBandLevelMb(band: Int, levelMb: Int) = eq.setBandLevel(band.toShort(), levelMb.toShort())
            override fun setEnabled(enabled: Boolean) { eq.enabled = enabled }
            override fun release() = eq.release()
        }
    } catch (_: Throwable) {
        null
    }

    override fun bassBoost(sessionId: Int): StrengthEffect? = try {
        val effect = BassBoost(0, sessionId)
        object : StrengthEffect {
            override val strengthSupported = effect.strengthSupported
            override fun setStrength(permille: Int) = effect.setStrength(permille.toShort())
            override fun setEnabled(enabled: Boolean) { effect.enabled = enabled }
            override fun release() = effect.release()
        }
    } catch (_: Throwable) {
        null
    }

    override fun virtualizer(sessionId: Int): StrengthEffect? = try {
        val effect = Virtualizer(0, sessionId)
        object : StrengthEffect {
            override val strengthSupported = effect.strengthSupported
            override fun setStrength(permille: Int) = effect.setStrength(permille.toShort())
            override fun setEnabled(enabled: Boolean) { effect.enabled = enabled }
            override fun release() = effect.release()
        }
    } catch (_: Throwable) {
        null
    }

    override fun loudness(sessionId: Int): LoudnessEffect? = try {
        val effect = LoudnessEnhancer(sessionId)
        object : LoudnessEffect {
            override fun setTargetGainMb(milliBels: Int) = effect.setTargetGain(milliBels)
            override fun setEnabled(enabled: Boolean) { effect.enabled = enabled }
            override fun release() = effect.release()
        }
    } catch (_: Throwable) {
        null
    }
}
