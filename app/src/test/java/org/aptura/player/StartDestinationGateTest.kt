package org.aptura.player

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.flow.MutableSharedFlow
import org.aptura.player.data.settings.ApturaSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for audit P1-03: the start destination used to be decided on the first frame from the DEFAULT
 * settings (onboardingCompleted = false), before DataStore had delivered the real value, so users who had
 * already completed onboarding without granting access saw it again on every cold start.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StartDestinationGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noAccess_butOnboardingAlreadyCompleted_startsInLibrary_afterSettingsLoad() {
        val settings = MutableSharedFlow<ApturaSettings>(replay = 1)
        val composedWith = mutableListOf<String>()
        composeRule.setContent {
            StartDestinationGate(settingsFlow = settings, hasStorageAccess = false) { _, startKey ->
                composedWith += startKey.toString()
                Text("START=$startKey")
            }
        }

        // Settings have not arrived yet: nothing may be decided (and nothing wrongly composed).
        assertTrue("content composed before settings loaded: $composedWith", composedWith.isEmpty())

        settings.tryEmit(ApturaSettings(onboardingCompleted = true))
        composeRule.waitForIdle()

        composeRule.onNodeWithText("START=MediaLibrary").assertIsDisplayed()
        assertEquals(listOf("MediaLibrary"), composedWith.distinct())
    }

    @Test
    fun noAccess_andOnboardingNotCompleted_startsInOnboarding() {
        val settings = MutableSharedFlow<ApturaSettings>(replay = 1)
        settings.tryEmit(ApturaSettings(onboardingCompleted = false))
        composeRule.setContent {
            StartDestinationGate(settings, hasStorageAccess = false) { _, startKey -> Text("START=$startKey") }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("START=Onboarding").assertIsDisplayed()
    }

    @Test
    fun withStorageAccess_alwaysStartsInLibrary() {
        assertEquals(MediaLibrary, startDestinationFor(hasStorageAccess = true, onboardingCompleted = false))
        assertEquals(MediaLibrary, startDestinationFor(hasStorageAccess = true, onboardingCompleted = true))
        assertEquals(MediaLibrary, startDestinationFor(hasStorageAccess = false, onboardingCompleted = true))
        assertEquals(Onboarding, startDestinationFor(hasStorageAccess = false, onboardingCompleted = false))
    }
}
