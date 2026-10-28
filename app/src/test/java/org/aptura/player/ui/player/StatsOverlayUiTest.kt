package org.aptura.player.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.aptura.player.domain.model.PlaybackStatsInfo
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for StatsOverlay (FR-DIAG-001, SCR-010e)
 * and CompatibilityExplainerContent (FR-PLAY-003, SCR-010h).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatsOverlayUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun statsOverlay_rendersLiveTelemetry_andHandlesClose() {
        var closed = false
        val sampleStats = PlaybackStatsInfo(
            decoderPath = "Auto (Hardware)",
            isHardwareAccelerated = true,
            videoDecoderName = "c2.goldfish.h264.decoder",
            videoCodec = "video/avc",
            videoResolution = "1920x1080",
            frameRate = 60.0f,
            videoBitrateKbps = 4500,
            droppedFrames = 5,
            dropPercentage = 0.2f,
            audioDecoderName = "c2.android.aac.decoder",
            audioCodec = "audio/mp4a-latm",
            audioChannels = "Stereo (2 ch)",
            audioSampleRateHz = 48000,
            audioBitrateKbps = 192,
            audioDelayMs = 50L,
            subtitleDelayMs = -100L,
            hdrFormat = "SDR"
        )

        composeTestRule.setContent {
            ApturaTheme {
                StatsOverlay(
                    stats = sampleStats,
                    onCloseClick = { closed = true }
                )
            }
        }

        // Header and badges
        composeTestRule.onNodeWithText("Diagnostics & Stats").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hardware Accelerated").assertIsDisplayed()

        // Telemetry rows (using assertExists for scrollable content)
        composeTestRule.onNodeWithText("Auto (Hardware)").assertExists()
        composeTestRule.onNodeWithText("c2.goldfish.h264.decoder").assertExists()
        composeTestRule.onNodeWithText("1920x1080 @ 60.0 fps").assertExists()
        composeTestRule.onNodeWithText("4.5 Mbps").assertExists()
        composeTestRule.onNodeWithText("5 (0.2%)").assertExists()
        composeTestRule.onNodeWithText("+50 ms").assertExists()
        composeTestRule.onNodeWithText("-100 ms").assertExists()

        // Close action
        composeTestRule.onNodeWithContentDescription("Close Stats").performClick()
        assertTrue(closed)
    }

    @Test
    fun compatibilityExplainerSheet_rendersFacts_andAllowsOverride() {
        var selectedStrategy = -1
        var dismissed = false

        composeTestRule.setContent {
            ApturaTheme {
                CompatibilityExplainerContent(
                    failedCodecName = "c2.goldfish.hevc.decoder",
                    failureReason = "Profile Main10 not supported by hardware codec",
                    activeDecoderPath = "c2.android.hevc.decoder",
                    currentStrategy = 0,
                    onStrategySelected = { selectedStrategy = it },
                    onDismissRequest = { dismissed = true }
                )
            }
        }

        // Header & Questions
        composeTestRule.onNodeWithText("Software decoding").assertExists()
        composeTestRule.onNodeWithText("Why is this video using software decoding?").assertExists()

        // Factual points
        composeTestRule.onNodeWithText("What happened").assertExists()
        composeTestRule.onNodeWithText("What is being used instead").assertExists()
        composeTestRule.onNodeWithText("What it means for you").assertExists()

        // Decoder Strategy options with required descriptions
        composeTestRule.onNodeWithText("Hardware").assertExists()
        composeTestRule.onNodeWithText("Software").assertExists()

        // Click Software override (with scroll)
        composeTestRule.onNodeWithText("Software").performScrollTo().performClick()
        assertEquals(2, selectedStrategy)

        // Dismiss action (with scroll)
        composeTestRule.onNodeWithText("Got it").performScrollTo().performClick()
        assertTrue(dismissed)
    }
}
