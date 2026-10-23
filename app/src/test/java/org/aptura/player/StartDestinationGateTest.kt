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
 * Two regressions about when the welcome guide appears.
 *
 * Audit P1-03: the start destination used to be decided on the first frame from the DEFAULT settings
 * (onboardingCompleted = false), before DataStore had delivered the real value, so users who had already seen the
 * guide saw it again on every cold start.
 *
 * First run: the guide used to be skipped whenever storage access was already granted (a reinstall, a device or install
 * command that pre-grants it), and "seen" was only recorded inside the permission flow, so those users never saw it and
 * could only find it in Settings. The first launch now always shows it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StartDestinationGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingAlreadyCompleted_startsInLibrary_afterSettingsLoad() {
        val settings = MutableSharedFlow<ApturaSettings>(replay = 1)
        val composedWith = mutableListOf<String>()
        composeRule.setContent {
            StartDestinationGate(settingsFlow = settings) { _, startKey ->
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
    fun firstLaunch_startsInTheGuide() {
        val settings = MutableSharedFlow<ApturaSettings>(replay = 1)
        settings.tryEmit(ApturaSettings(onboardingCompleted = false))
        composeRule.setContent {
            StartDestinationGate(settings) { _, startKey -> Text("START=$startKey") }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("START=Onboarding").assertIsDisplayed()
    }

    @Test
    fun theGuideDependsOnlyOnWhetherItWasSeen_notOnStorageAccess() {
        assertEquals(Onboarding, startDestinationFor(onboardingCompleted = false))
        assertEquals(MediaLibrary, startDestinationFor(onboardingCompleted = true))
    }
}
