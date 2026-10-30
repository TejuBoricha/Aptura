package org.aptura.player.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.aptura.player.domain.model.FolderLayout
import org.aptura.player.domain.model.LibraryDensity
import org.aptura.player.domain.model.LibraryDetail
import org.aptura.player.domain.model.LibraryLayout
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
            assertFalse(initial.isSupporter)
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

            repository.updateSupporter(true)
            assertTrue(awaitItem().isSupporter)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun resetPlaybackDefaults_restoresFactoryProfile() = runTest {
        repository.updateDefaultPlaybackSpeed(1.75f)
        repository.updateDefaultResizeMode(3)
        repository.updateSeekDeltaSeconds(30)
        repository.updateBackgroundAudioEnabled(false)
        repository.updateAutoAdvanceCountdown(0)
        repository.updateDefaultDecoderStrategy(2)

        repository.resetPlaybackDefaults()

        val reset = repository.settingsFlow.first()
        assertEquals(1.0f, reset.defaultPlaybackSpeed)
        assertEquals(0, reset.defaultResizeMode)
        assertEquals(10, reset.seekDeltaSeconds)
        assertTrue(reset.backgroundAudioEnabled)
        assertEquals(10, reset.autoAdvanceCountdownSeconds)
        assertEquals(0, reset.defaultDecoderStrategy)
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

    @Test
    fun libraryLayout_defaultsToAComfortableGridWithStandardDetails() = runTest {
        val initial = repository.settingsFlow.first()

        assertEquals(LibraryLayout.GRID, initial.libraryLayout)
        assertEquals(LibraryDensity.COMFORTABLE, initial.libraryDensity)
        assertEquals(LibraryDetail.STANDARD, initial.libraryDetail)
        assertEquals(FolderLayout.LIST, initial.folderLayout)
        assertEquals("the library chooses its own sort until the user picks one", null, initial.librarySortName)
    }

    @Test
    fun libraryLayout_survivesANewProcess() = runTest {
        repository.updateLibraryLayout(LibraryLayout.LIST)
        repository.updateLibraryDensity(LibraryDensity.LARGE)
        repository.updateLibraryDetail(LibraryDetail.DETAILED)
        repository.updateFolderLayout(FolderLayout.GRID)
        repository.updateLibrarySortName("TITLE_ASC")

        // a brand-new repository instance reads the same file, like a new process would
        val reloaded = ApturaSettingsRepository(context).settingsFlow.first()

        assertEquals(LibraryLayout.LIST, reloaded.libraryLayout)
        assertEquals(LibraryDensity.LARGE, reloaded.libraryDensity)
        assertEquals(LibraryDetail.DETAILED, reloaded.libraryDetail)
        assertEquals(FolderLayout.GRID, reloaded.folderLayout)
        assertEquals("TITLE_ASC", reloaded.librarySortName)
    }

    @Test
    fun clearingTheSort_returnsToTheLibrarysOwnChoice() = runTest {
        repository.updateLibrarySortName("SIZE_DESC")
        repository.updateLibrarySortName(null)

        assertEquals(null, repository.settingsFlow.first().librarySortName)
    }

    @Test
    fun anUnknownStoredLayout_fallsBackToTheDefault_insteadOfCrashing() = runTest {
        // A name from a newer build, or a damaged file, must not take the library down.
        repository.writeRawForTesting("library_layout", "MOSAIC")

        assertEquals(LibraryLayout.GRID, repository.settingsFlow.first().libraryLayout)
    }
}
