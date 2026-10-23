package org.aptura.player.data.playback

/** Recording stand-ins for AudioFX (the audio HAL cannot run in a unit test); shared by the controller and ViewModel tests. */
internal class FakeEqualizer(
    override val bandCount: Int = 5,
    /** What the hardware claims its bands are at when we attach (some devices start at +3 dB). */
    startLevels: List<Int> = List(bandCount) { 300 },
    private val refusesBand: Int? = null
) : EqualizerEffect {
    override val minLevelMb = -1500
    override val maxLevelMb = 1500
    val levels = startLevels.toMutableList()
    var on = false
    var released = false
    override fun centerFreqHz(band: Int) = 60 * (band + 1)
    override fun bandLevelMb(band: Int) = levels[band]
    override fun setBandLevelMb(band: Int, levelMb: Int) {
        if (band == refusesBand) throw IllegalStateException("HAL refused band $band")
        levels[band] = levelMb
    }
    override fun setEnabled(enabled: Boolean) { on = enabled }
    override fun release() { released = true }
}

internal class FakeStrength : StrengthEffect {
    override val strengthSupported = true
    var permille = -1
    var on = false
    var released = false
    override fun setStrength(permille: Int) { this.permille = permille }
    override fun setEnabled(enabled: Boolean) { on = enabled }
    override fun release() { released = true }
}

internal class FakeLoudness : LoudnessEffect {
    var gainMb = -1
    var on = false
    var released = false
    override fun setTargetGainMb(milliBels: Int) { gainMb = milliBels }
    override fun setEnabled(enabled: Boolean) { on = enabled }
    override fun release() { released = true }
}

internal class FakeBackend(
    private val hasEqualizer: Boolean = true,
    private val equalizerFactory: () -> FakeEqualizer = { FakeEqualizer() }
) : AudioEffectsBackend {
    val equalizers = mutableListOf<FakeEqualizer>()
    val bassBoosts = mutableListOf<FakeStrength>()
    val virtualizers = mutableListOf<FakeStrength>()
    val loudnessEffects = mutableListOf<FakeLoudness>()
    val sessions = mutableListOf<Int>()

    override fun equalizer(sessionId: Int): EqualizerEffect? {
        sessions += sessionId
        return if (hasEqualizer) equalizerFactory().also { equalizers += it } else null
    }
    override fun bassBoost(sessionId: Int): StrengthEffect = FakeStrength().also { bassBoosts += it }
    override fun virtualizer(sessionId: Int): StrengthEffect = FakeStrength().also { virtualizers += it }
    override fun loudness(sessionId: Int): LoudnessEffect = FakeLoudness().also { loudnessEffects += it }
}
