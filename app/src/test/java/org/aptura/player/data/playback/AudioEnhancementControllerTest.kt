package org.aptura.player.data.playback

import org.aptura.player.domain.model.AudioEffectsSettings
import org.aptura.player.domain.model.EqualizerPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour of the sound-effects controller against a recording stand-in for AudioFX (the only thing that cannot run
 * in a unit test is the audio HAL itself). Audit P1-16: effects were never released, never saved, "Flat" was shown over
 * +3 dB bands, one refused band aborted the rest, and sliders did nothing while the master switch was off.
 */
class AudioEnhancementControllerTest {

    @Test
    fun attachingTwiceToTheSameSession_createsEffectsOnce_andANewSessionReleasesTheOldOnes() {
        val backend = FakeBackend()
        val controller = AudioEnhancementController(backend)

        controller.attachToSession(7)
        controller.attachToSession(7)
        assertEquals("no duplicate effects for the same session", 1, backend.equalizers.size)

        controller.attachToSession(9)

        assertEquals(2, backend.equalizers.size)
        assertTrue("the old session's effects must be released", backend.equalizers[0].released)
        assertTrue(backend.bassBoosts[0].released && backend.virtualizers[0].released && backend.loudnessEffects[0].released)
        assertFalse(backend.equalizers[1].released)
    }

    @Test
    fun releaseEffects_releasesEverything() {
        val backend = FakeBackend()
        val controller = AudioEnhancementController(backend)
        controller.attachToSession(7)

        controller.releaseEffects()

        assertTrue(backend.equalizers.single().released)
        assertTrue(backend.bassBoosts.single().released)
        assertTrue(backend.virtualizers.single().released)
        assertTrue(backend.loudnessEffects.single().released)
    }

    @Test
    fun theLabelNeverContradictsTheFaders_hardwareStartingAtPlus3dB_isBroughtToFlat() {
        val backend = FakeBackend() // hardware says every band is at +300 mB when we attach
        val controller = AudioEnhancementController(backend)

        controller.attachToSession(7)

        val state = controller.state.value
        assertEquals(EqualizerPreset.FLAT, state.preset)
        assertTrue("faders must show flat", state.bands.all { it.levelMilliBels == 0 })
        assertTrue("and the hardware must actually be flat", backend.equalizers.single().levels.all { it == 0 })
    }

    @Test
    fun oneBandTheDeviceRefuses_doesNotStopTheOthers() {
        val backend = FakeBackend(equalizerFactory = { FakeEqualizer(refusesBand = 1) })
        val controller = AudioEnhancementController(backend)
        controller.attachToSession(7)

        controller.setPreset(EqualizerPreset.ROCK) // 400, 200, -100, 200, 400

        val levels = backend.equalizers.single().levels
        assertEquals(400, levels[0])
        assertEquals("band 1 was refused and keeps whatever the hardware had", 300, levels[1])
        assertEquals(listOf(-100, 200, 400), levels.slice(2..4))
    }

    @Test
    fun savedSettings_areAppliedTheMomentTheEqualizerAppears() {
        val backend = FakeBackend()
        val controller = AudioEnhancementController(backend)
        controller.restore(
            AudioEffectsSettings(
                enabled = true,
                preset = EqualizerPreset.CUSTOM,
                bandLevelsMb = listOf(100, 200, 300, -100, 0),
                bassBoostPercent = 40,
                virtualizerPercent = 25,
                loudnessBoostPercent = 50
            )
        )

        controller.attachToSession(7)

        assertEquals(listOf(100, 200, 300, -100, 0), backend.equalizers.single().levels)
        assertTrue(backend.equalizers.single().on)
        assertEquals(400, backend.bassBoosts.single().permille)
        assertTrue(backend.bassBoosts.single().on)
        assertEquals(250, backend.virtualizers.single().permille)
        assertEquals(300, backend.loudnessEffects.single().gainMb)
        assertEquals(EqualizerPreset.CUSTOM, controller.state.value.preset)
    }

    @Test
    fun savedSettingsArriveAfterTheEffectsWereAttached_areStillApplied() {
        val backend = FakeBackend()
        val controller = AudioEnhancementController(backend)
        controller.attachToSession(7) // the session was ready before the settings finished loading

        controller.restore(AudioEffectsSettings(enabled = true, preset = EqualizerPreset.POP, bassBoostPercent = 60))

        assertEquals(listOf(-100, 200, 400, 200, -100), backend.equalizers.single().levels)
        assertEquals(600, backend.bassBoosts.single().permille)
        assertEquals(EqualizerPreset.POP, controller.state.value.preset)
    }

    @Test
    fun aSliderMovedWhileTheMasterSwitchIsOff_turnsItOn_insteadOfDoingNothing() {
        val backend = FakeBackend()
        val controller = AudioEnhancementController(backend)
        controller.attachToSession(7)
        assertFalse(controller.state.value.enabled)

        controller.setBassBoost(30)

        assertTrue(controller.state.value.enabled)
        assertTrue("the effect must really be on", backend.bassBoosts.single().on)
        assertEquals(300, backend.bassBoosts.single().permille)
    }

    @Test
    fun movingABand_switchesToCustom_andAFaderMatchingAPresetNamesIt() {
        val backend = FakeBackend()
        val controller = AudioEnhancementController(backend)
        controller.attachToSession(7)

        controller.setBandLevel(0, 250)
        assertEquals(EqualizerPreset.CUSTOM, controller.state.value.preset)

        controller.setPreset(EqualizerPreset.JAZZ)
        controller.setBandLevel(0, 200) // same as Jazz's band 0
        assertEquals(EqualizerPreset.JAZZ, controller.state.value.preset)
    }

    @Test
    fun loudnessIsCappedAtSixDecibels() {
        val backend = FakeBackend()
        val controller = AudioEnhancementController(backend)
        controller.attachToSession(7)

        controller.setLoudnessBoost(100)

        assertEquals("100 % must not exceed +6 dB", 600, backend.loudnessEffects.single().gainMb)
    }

    @Test
    fun aDeviceWithoutAnEqualizer_stillGetsTheOtherEffects_andANormalLookingBandList() {
        val backend = FakeBackend(hasEqualizer = false)
        val controller = AudioEnhancementController(backend)

        controller.attachToSession(7)
        controller.setVirtualizer(50)

        assertEquals(5, controller.state.value.bands.size)
        assertEquals(500, backend.virtualizers.single().permille)
    }

    @Test
    fun snapshot_roundTripsThroughRestore() {
        val first = AudioEnhancementController(FakeBackend())
        first.attachToSession(7)
        first.setPreset(EqualizerPreset.ROCK)
        first.setBassBoost(20)
        first.setLoudnessBoost(35)

        val second = AudioEnhancementController(FakeBackend())
        second.restore(first.snapshot())
        second.attachToSession(7)

        assertEquals(first.state.value, second.state.value)
        assertNotNull(second.state.value.bands.firstOrNull())
    }

    @Test
    fun matchPreset_namesACurveOnlyWhenItIsExactlyThatCurve() {
        assertEquals(EqualizerPreset.ROCK, AudioEnhancementController.matchPreset(listOf(400, 200, -100, 200, 400)))
        assertEquals(EqualizerPreset.FLAT, AudioEnhancementController.matchPreset(listOf(0, 0, 0, 0, 0)))
        assertEquals(EqualizerPreset.CUSTOM, AudioEnhancementController.matchPreset(listOf(400, 200, -100, 200, 401)))
    }
}
