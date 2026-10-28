package org.aptura.player.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.domain.model.AudioEnhancementState
import org.aptura.player.domain.model.EqualizerBand
import org.aptura.player.domain.model.EqualizerPreset
import org.aptura.player.theme.ApturaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI test suite for AudioEnhancementSheet (SCR-010d, FR-AUD-001..004).
 * Verifies segmented tab navigation, humanized track labels, lip-sync delay stepper,
 * equalizer vertical fader rack, and sound effect preset chips.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioEnhancementSheetUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleTracks = listOf(
        TrackInfo(id = "1", index = 0, label = "Track 1 (Stereo)", language = "und", isSelected = true)
    )

    private val sampleBands = listOf(
        EqualizerBand(bandIndex = 0, centerFreqHz = 60, levelMilliBels = 300, minMilliBels = -1000, maxMilliBels = 1000),
        EqualizerBand(bandIndex = 1, centerFreqHz = 230, levelMilliBels = 0, minMilliBels = -1000, maxMilliBels = 1000),
        EqualizerBand(bandIndex = 2, centerFreqHz = 910, levelMilliBels = 0, minMilliBels = -1000, maxMilliBels = 1000),
        EqualizerBand(bandIndex = 3, centerFreqHz = 3000, levelMilliBels = 0, minMilliBels = -1000, maxMilliBels = 1000),
        EqualizerBand(bandIndex = 4, centerFreqHz = 14000, levelMilliBels = 300, minMilliBels = -1000, maxMilliBels = 1000)
    )

    private val sampleAudioEnhancement = AudioEnhancementState(
        enabled = true,
        preset = EqualizerPreset.FLAT,
        bands = sampleBands,
        bassBoostPercent = 60,
        virtualizerPercent = 0,
        loudnessBoostPercent = 0
    )

    @Test
    fun audioEnhancementSheet_rendersHeaderAndTabs() {
        composeTestRule.setContent {
            ApturaTheme {
                AudioEnhancementSheet(
                    audioTracks = sampleTracks,
                    selectedAudioTrackIndex = 0,
                    audioDelayMs = 0L,
                    delayStepMs = 50L,
                    audioEnhancement = sampleAudioEnhancement,
                    onDismissRequest = {},
                    onAudioTrackSelected = {},
                    onAudioDelayChanged = {},
                    onEqualizerEnabledChanged = {},
                    onEqualizerPresetSelected = {},
                    onEqualizerBandChanged = { _, _ -> },
                    onBassBoostChanged = {},
                    onVirtualizerChanged = {},
                    onLoudnessBoostChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Audio").assertExists()
        composeTestRule.onNodeWithText("Tracks").assertExists()
        composeTestRule.onNodeWithText("Equalizer").assertExists()
        composeTestRule.onNodeWithText("Effects").assertExists()
    }

    @Test
    fun audioTracksAndSyncTab_rendersCleanTrackInfoAndStepper() {
        composeTestRule.setContent {
            ApturaTheme {
                AudioEnhancementSheet(
                    audioTracks = sampleTracks,
                    selectedAudioTrackIndex = 0,
                    audioDelayMs = 150L,
                    delayStepMs = 50L,
                    audioEnhancement = sampleAudioEnhancement,
                    onDismissRequest = {},
                    onAudioTrackSelected = {},
                    onAudioDelayChanged = {},
                    onEqualizerEnabledChanged = {},
                    onEqualizerPresetSelected = {},
                    onEqualizerBandChanged = { _, _ -> },
                    onBassBoostChanged = {},
                    onVirtualizerChanged = {},
                    onLoudnessBoostChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Track 1 (Stereo)").assertExists()
        composeTestRule.onNodeWithText("AUDIO SYNC").assertExists()
        composeTestRule.onNodeWithText("+150 ms").assertExists()
        composeTestRule.onNodeWithText("-50ms").assertExists()
        composeTestRule.onNodeWithText("+50ms").assertExists()
        composeTestRule.onNodeWithText("Buds (+150ms)").assertExists()
    }

    @Test
    fun equalizerTab_switchesAndRendersFaderRack() {
        composeTestRule.setContent {
            ApturaTheme {
                AudioEnhancementSheet(
                    audioTracks = sampleTracks,
                    selectedAudioTrackIndex = 0,
                    audioDelayMs = 0L,
                    delayStepMs = 50L,
                    audioEnhancement = sampleAudioEnhancement,
                    initialTab = 1,
                    onDismissRequest = {},
                    onAudioTrackSelected = {},
                    onAudioDelayChanged = {},
                    onEqualizerEnabledChanged = {},
                    onEqualizerPresetSelected = {},
                    onEqualizerBandChanged = { _, _ -> },
                    onBassBoostChanged = {},
                    onVirtualizerChanged = {},
                    onLoudnessBoostChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithText("EQUALIZER").assertExists()
        composeTestRule.onNodeWithText("BANDS").assertExists()
        composeTestRule.onNodeWithText("Flat").assertExists()
        composeTestRule.onNodeWithText("Bass Boost").assertExists()
        composeTestRule.onNodeWithText("60").assertExists()
        composeTestRule.onNodeWithText("14k").assertExists()
    }

    @Test
    fun soundEffectsTab_switchesAndRendersCardsAndPills() {
        composeTestRule.setContent {
            ApturaTheme {
                AudioEnhancementSheet(
                    audioTracks = sampleTracks,
                    selectedAudioTrackIndex = 0,
                    audioDelayMs = 0L,
                    delayStepMs = 50L,
                    audioEnhancement = sampleAudioEnhancement,
                    initialTab = 2,
                    onDismissRequest = {},
                    onAudioTrackSelected = {},
                    onAudioDelayChanged = {},
                    onEqualizerEnabledChanged = {},
                    onEqualizerPresetSelected = {},
                    onEqualizerBandChanged = { _, _ -> },
                    onBassBoostChanged = {},
                    onVirtualizerChanged = {},
                    onLoudnessBoostChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithText("BASS BOOST").assertExists()
        composeTestRule.onNodeWithText("60%").assertExists()
        composeTestRule.onNodeWithText("SURROUND VIRTUALIZER").assertExists()
        composeTestRule.onNodeWithText("DIALOGUE & LOUDNESS BOOST").assertExists()
        composeTestRule.onNodeWithText("Punch").assertExists()
    }
}
