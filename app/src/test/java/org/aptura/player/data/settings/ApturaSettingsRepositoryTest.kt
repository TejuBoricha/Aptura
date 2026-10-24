package org.aptura.player.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ThemeMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApturaSettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: ApturaSettingsRepository

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        repository = ApturaSettingsRepository(context)
        repository.resetForTesting()
    }

    @Test
    fun defaultSettings_haveExpectedValues() = runTest {
        repository.settingsFlow.test {
            val initial = awaitItem()
            assertEquals(1.0f, initial.defaultPlaybackSpeed)
            assertEquals(0, initial.defaultResizeMode)
            assertEquals(10, initial.seekDeltaSeconds)
            assertTrue(initial.backgroundAudioEnabled)
            assertFalse(initial.onboardingCompleted)
            assertEquals(0, initial.lastSeenGuideVersion)
            assertEquals(ThemeMode.SYSTEM, initial.themeMode)
            assertEquals(AccentColor.CYAN, initial.accentColor)
            assertEquals(10, initial.autoAdvanceCountdownSeconds)
            assertEquals(18, initial.subtitleFontSizeSp)
            assertEquals("#FFFFFF", initial.subtitleTextColorHex)
            assertFalse(initial.subtitleBackgroundEnabled)
            assertEquals(0, initial.defaultDecoderStrategy)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateSettings_persistsAndEmitsUpdatedValues() = runTest {
        repository.settingsFlow.test {
            // Initial defaults
            val initial = awaitItem()
            assertEquals(1.0f, initial.defaultPlaybackSpeed)

            // Update playback speed
            repository.updateDefaultPlaybackSpeed(1.5f)
            assertEquals(1.5f, awaitItem().defaultPlaybackSpeed)

            // Update resize mode
            repository.updateDefaultResizeMode(2)
            assertEquals(2, awaitItem().defaultResizeMode)

            // Update seek delta
            repository.updateSeekDeltaSeconds(15)
            assertEquals(15, awaitItem().seekDeltaSeconds)

            // Update background audio
            repository.updateBackgroundAudioEnabled(false)
            assertFalse(awaitItem().backgroundAudioEnabled)

            // Update onboarding completed
            repository.setOnboardingCompleted(true)
            assertTrue(awaitItem().onboardingCompleted)

            // Record the guide version and appearance
            repository.setLastSeenGuideVersion(7)
            assertEquals(7, awaitItem().lastSeenGuideVersion)

            repository.updateThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)

            repository.updateAccentColor(AccentColor.VIOLET)
            assertEquals(AccentColor.VIOLET, awaitItem().accentColor)

            // Update auto-advance countdown
            repository.updateAutoAdvanceCountdown(5)
            assertEquals(5, awaitItem().autoAdvanceCountdownSeconds)

            // Update subtitle preferences
            repository.updateSubtitleFontSize(22)
            assertEquals(22, awaitItem().subtitleFontSizeSp)

            repository.updateSubtitleTextColor("#FFEB3B")
            assertEquals("#FFEB3B", awaitItem().subtitleTextColorHex)

            repository.updateSubtitleBackground(true)
            assertTrue(awaitItem().subtitleBackgroundEnabled)

            // Update decoder strategy
            repository.updateDefaultDecoderStrategy(1)
            assertEquals(1, awaitItem().defaultDecoderStrategy)

            // Add and remove SAF folders
            val sampleFolder = "content://com.android.externalstorage.documents/tree/primary%3AMovies"
            repository.addSafFolder(sampleFolder)
            val withFolder = awaitItem()
            assertTrue(withFolder.persistedSafFolders.contains(sampleFolder))

            repository.removeSafFolder(sampleFolder)
            val withoutFolder = awaitItem()
            assertFalse(withoutFolder.persistedSafFolders.contains(sampleFolder))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun audioEffects_surviveARestart() = runTest {
        val saved = org.aptura.player.domain.model.AudioEffectsSettings(
            enabled = true,
            preset = org.aptura.player.domain.model.EqualizerPreset.CUSTOM,
            bandLevelsMb = listOf(100, -200, 300, 0, 50),
            bassBoostPercent = 40,
            virtualizerPercent = 25,
            loudnessBoostPercent = 60
        )
        repository.updateAudioEffects(saved)

        // a brand-new repository instance reads the same file, like a new process would
        val reloaded = ApturaSettingsRepository(context).load()

        assertEquals(saved, reloaded)
    }

    @Test
    fun audioEffects_defaultToOff() = runTest {
        val loaded = repository.load()

        assertFalse(loaded.enabled)
        assertTrue(loaded.bandLevelsMb.isEmpty())
        assertEquals(0, loaded.bassBoostPercent)
    }
}
