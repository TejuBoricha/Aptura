package org.aptura.player.ui.player

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.aptura.player.domain.model.AbRepeatState
import org.aptura.player.domain.model.Chapter
import org.aptura.player.theme.ApturaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerSeekBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playerSeekBar_whenScrubbingWithoutPreview_rendersTimecodeChipOnly() {
        composeTestRule.setContent {
            ApturaTheme {
                PlayerSeekBar(
                    positionProvider = { PlayerPositionState(15_000L, 30_000L) },
                    durationMs = 60_000L,
                    isScrubbing = true,
                    scrubPositionMs = 25_000L,
                    seekStepMs = 10_000L,
                    bookmarks = emptyList(),
                    abRepeat = AbRepeatState(),
                    onScrubStart = {},
                    onScrubMove = {},
                    onScrubEnd = {},
                    onSeekRelative = {},
                    previewBitmap = null
                )
            }
        }

        composeTestRule.onNodeWithTag("ScrubTimecodeChip").assertExists()
        composeTestRule.onNodeWithTag("ScrubPreviewCard").assertDoesNotExist()
        composeTestRule.onNodeWithTag("ScrubPreviewThumbnail").assertDoesNotExist()
        composeTestRule.onNodeWithText("0:25").assertExists()
    }

    @Test
    fun playerSeekBar_whenScrubbingWithPreview_rendersPreviewCardAndThumbnail() {
        val bitmap = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888).asImageBitmap()

        composeTestRule.setContent {
            ApturaTheme {
                PlayerSeekBar(
                    positionProvider = { PlayerPositionState(15_000L, 30_000L) },
                    durationMs = 120_000L,
                    isScrubbing = true,
                    scrubPositionMs = 45_000L,
                    seekStepMs = 10_000L,
                    bookmarks = emptyList(),
                    abRepeat = AbRepeatState(),
                    onScrubStart = {},
                    onScrubMove = {},
                    onScrubEnd = {},
                    onSeekRelative = {},
                    previewBitmap = bitmap
                )
            }
        }

        composeTestRule.onNodeWithTag("ScrubPreviewCard").assertExists()
        composeTestRule.onNodeWithTag("ScrubPreviewThumbnail").assertExists()
        composeTestRule.onNodeWithTag("ScrubTimecodeChip").assertDoesNotExist()
        composeTestRule.onNodeWithText("0:45").assertExists()
    }

    @Test
    fun playerSeekBar_whenNotScrubbing_rendersNoPreviewOverlay() {
        composeTestRule.setContent {
            ApturaTheme {
                PlayerSeekBar(
                    positionProvider = { PlayerPositionState(15_000L, 30_000L) },
                    durationMs = 120_000L,
                    isScrubbing = false,
                    scrubPositionMs = 0L,
                    seekStepMs = 10_000L,
                    bookmarks = emptyList(),
                    abRepeat = AbRepeatState(),
                    onScrubStart = {},
                    onScrubMove = {},
                    onScrubEnd = {},
                    onSeekRelative = {},
                    previewBitmap = null
                )
            }
        }

        composeTestRule.onNodeWithTag("ScrubPreviewCard").assertDoesNotExist()
        composeTestRule.onNodeWithTag("ScrubTimecodeChip").assertDoesNotExist()
    }

    @Test
    fun playerSeekBar_whenScrubbingWithChapters_displaysChapterTitleInChip() {
        val testChapters = listOf(
            Chapter("ch1", "Introduction", 0L, 20_000L),
            Chapter("ch2", "The Climax", 20_000L, 60_000L)
        )

        composeTestRule.setContent {
            ApturaTheme {
                PlayerSeekBar(
                    positionProvider = { PlayerPositionState(5_000L, 30_000L) },
                    durationMs = 60_000L,
                    isScrubbing = true,
                    scrubPositionMs = 25_000L,
                    seekStepMs = 10_000L,
                    bookmarks = emptyList(),
                    abRepeat = AbRepeatState(),
                    chapters = testChapters,
                    onScrubStart = {},
                    onScrubMove = {},
                    onScrubEnd = {},
                    onSeekRelative = {},
                    previewBitmap = null
                )
            }
        }

        composeTestRule.onNodeWithTag("ScrubTimecodeChip").assertExists()
        composeTestRule.onNodeWithTag("ScrubChapterTitle").assertExists()
        composeTestRule.onNodeWithText("The Climax").assertExists()
        composeTestRule.onNodeWithText("0:25").assertExists()
    }

    @Test
    fun playerSeekBar_whenScrubbingWithChaptersAndPreview_displaysChapterTitleInPreviewCard() {
        val bitmap = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888).asImageBitmap()
        val testChapters = listOf(
            Chapter("ch1", "Introduction", 0L, 20_000L),
            Chapter("ch2", "The Climax", 20_000L, 60_000L)
        )

        composeTestRule.setContent {
            ApturaTheme {
                PlayerSeekBar(
                    positionProvider = { PlayerPositionState(5_000L, 30_000L) },
                    durationMs = 60_000L,
                    isScrubbing = true,
                    scrubPositionMs = 25_000L,
                    seekStepMs = 10_000L,
                    bookmarks = emptyList(),
                    abRepeat = AbRepeatState(),
                    chapters = testChapters,
                    onScrubStart = {},
                    onScrubMove = {},
                    onScrubEnd = {},
                    onSeekRelative = {},
                    previewBitmap = bitmap
                )
            }
        }

        composeTestRule.onNodeWithTag("ScrubPreviewCard").assertExists()
        composeTestRule.onNodeWithTag("ScrubPreviewChapterTitle").assertExists()
        composeTestRule.onNodeWithText("The Climax").assertExists()
        composeTestRule.onNodeWithText("0:25").assertExists()
    }
}
