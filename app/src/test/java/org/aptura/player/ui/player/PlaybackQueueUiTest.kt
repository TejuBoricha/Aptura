package org.aptura.player.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for PlaybackQueueSheet (SCR-010f) and UpNextOverlay (SCR-010g).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaybackQueueUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleItems = listOf(
        DomainMediaItem(uri = "content://media/1", title = "Episode 1: Pilot", durationMs = 120_000L, sizeBytes = 1000L, mimeType = "video/mp4"),
        DomainMediaItem(uri = "content://media/2", title = "Episode 2: The Return", durationMs = 150_000L, sizeBytes = 1000L, mimeType = "video/mp4"),
        DomainMediaItem(uri = "content://media/3", title = "Episode 3: The Finale", durationMs = 180_000L, sizeBytes = 1000L, mimeType = "video/mp4")
    )

    @Test
    fun playbackQueueSheet_rendersItems_andJumpWorks() {
        var jumpedIndex = -1

        composeTestRule.setContent {
            ApturaTheme {
                PlaybackQueueContent(
                    queueItems = sampleItems,
                    currentIndex = 0,
                    playMode = PlayMode.NORMAL,
                    onJumpToItem = { jumpedIndex = it },
                    onMoveItem = { _, _ -> },
                    onRemoveItem = {},
                    onClearQueue = {},
                    onTogglePlayMode = {}
                )
            }
        }

        // Verify title & active indicator
        composeTestRule.onNodeWithText("Queue").assertIsDisplayed()
        composeTestRule.onNodeWithText("NOW").assertIsDisplayed()

        // Verify items displayed
        composeTestRule.onNodeWithText("Episode 1: Pilot").assertIsDisplayed()
        composeTestRule.onNodeWithText("Episode 2: The Return").assertIsDisplayed()

        // Click on second item
        composeTestRule.onNodeWithText("Episode 2: The Return").performClick()
        assertEquals(1, jumpedIndex)
    }

    @Test
    fun playbackQueueSheet_reorderAndClearWork() {
        var movedFrom = -1
        var movedTo = -1
        var cleared = false

        composeTestRule.setContent {
            ApturaTheme {
                PlaybackQueueContent(
                    queueItems = sampleItems,
                    currentIndex = 1,
                    playMode = PlayMode.NORMAL,
                    onJumpToItem = {},
                    onMoveItem = { from, to ->
                        movedFrom = from
                        movedTo = to
                    },
                    onRemoveItem = {},
                    onClearQueue = { cleared = true },
                    onTogglePlayMode = {}
                )
            }
        }

        // Move item up (Episode 2 is index 1, can move up)
        val moveUpButtons = composeTestRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Move Up"))
        moveUpButtons[0].performClick()
        assertEquals(1, movedFrom)
        assertEquals(0, movedTo)

        // Clear queue
        composeTestRule.onNodeWithContentDescription("Clear queue").performClick()
        assertTrue(cleared)
    }

    @Test
    fun playbackQueueSheet_playModeToggleWorks() {
        var playModeToggled = false

        composeTestRule.setContent {
            ApturaTheme {
                PlaybackQueueContent(
                    queueItems = sampleItems,
                    currentIndex = 0,
                    playMode = PlayMode.NORMAL,
                    onJumpToItem = {},
                    onMoveItem = { _, _ -> },
                    onRemoveItem = {},
                    onClearQueue = {},
                    onTogglePlayMode = { playModeToggled = true }
                )
            }
        }

        // Toggle play mode chip ("Play All" for NORMAL mode)
        composeTestRule.onNodeWithText("Play all").performClick()
        assertTrue(playModeToggled)
    }

    @Test
    fun upNextOverlay_rendersCountdown_andActionsWork() {
        var playNowClicked = false
        var replayClicked = false
        var cancelClicked = false

        composeTestRule.setContent {
            ApturaTheme {
                UpNextOverlay(
                    countdownSeconds = 8,
                    nextTitle = "Episode 2: The Return",
                    nextDurationMs = 150_000L,
                    onPlayNow = { playNowClicked = true },
                    onReplay = { replayClicked = true },
                    onCancel = { cancelClicked = true }
                )
            }
        }

        // Verify countdown seconds and Up Next label
        composeTestRule.onNodeWithText("8").assertIsDisplayed()
        composeTestRule.onNodeWithText("UP NEXT").assertIsDisplayed()
        composeTestRule.onNodeWithText("Episode 2: The Return").assertIsDisplayed()

        // Test Replay button
        composeTestRule.onNodeWithText("Replay").performClick()
        assertTrue(replayClicked)

        // Test Play Now button
        composeTestRule.onNodeWithText("Play now").performClick()
        assertTrue(playNowClicked)

        // Test Cancel Auto-Advance button
        composeTestRule.onNodeWithText("Stop countdown").performClick()
        assertTrue(cancelClicked)
    }
}
