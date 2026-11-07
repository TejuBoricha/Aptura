package org.aptura.player.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit FUNC-08: the details sheet says "Resume" only when the player really resumes. It said it for any position of a
 * second or more, and for a video saved a second from its end, while the player started those from the beginning.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DetailsPlayLabelTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val twoHours = 7_200_000L

    private fun video(positionMs: Long, isCompleted: Boolean = false) = DomainMediaItem(
        uri = "content://media/1",
        title = "Movie",
        durationMs = twoHours,
        sizeBytes = 500_000_000L,
        mimeType = "video/mp4",
        playbackPositionMs = positionMs,
        isCompleted = isCompleted
    )

    private fun show(item: DomainMediaItem) {
        composeRule.setContent {
            ApturaTheme { MediaDetailsContent(item = item, onClose = {}, onPlayClick = {}) }
        }
    }

    @Test
    fun aVideoSavedMidway_offersResume() {
        show(video(positionMs = 1_200_000L))
        composeRule.onNodeWithText("Resume").assertIsDisplayed()
    }

    @Test
    fun aVideoSavedInItsFirstSeconds_offersPlay_becausePlaybackStartsFromTheBeginning() {
        show(video(positionMs = 2_000L))
        composeRule.onNodeWithText("Play").assertIsDisplayed()
    }

    @Test
    fun aVideoSavedJustBeforeItsEnd_offersPlay_becausePlaybackStartsFromTheBeginning() {
        show(video(positionMs = twoHours - 1_000L))
        composeRule.onNodeWithText("Play").assertIsDisplayed()
    }

    @Test
    fun aWatchedVideo_offersPlay() {
        show(video(positionMs = 1_200_000L, isCompleted = true))
        composeRule.onNodeWithText("Play").assertIsDisplayed()
    }
}
