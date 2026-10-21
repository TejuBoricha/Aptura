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
            assertEquals(10, initial.autoAdvanceCountdownSeconds)
            assertEquals(18, initial.subtitleFontSizeSp)
            assertEquals("#FFFFFF", initial.subtitleTextColorHex)
            assertFalse(initial.subtitleBackgroundEnabled)
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

            cancelAndIgnoreRemainingEvents()
        }
    }
}
