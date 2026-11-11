@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.app.Application
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.aptura.player.MainActivity
import org.junit.After
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
class BackgroundAudioPlaybackTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        PlayerManager.release()
    }

    @After
    fun tearDown() {
        PlayerManager.release()
    }

    @Test
    fun setVideoEnabled_togglesVideoTrackSelectionDisabling() = runTest {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer

        // Initially video is enabled
        assertFalse(player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO))

        // Disable video track (background audio mode / audio-only)
        controller.setVideoEnabled(false)
        assertTrue(
            "Video track must be disabled to prevent background decoding (PERF-05)",
            player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
        )

        // Re-enable video track on foreground return
        controller.setVideoEnabled(true)
        assertFalse(
            "Video track must be re-enabled on return",
            player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
        )
    }

    @Test
    fun handleBackgroundAudioOnStop_whenBackgroundAudioEnabledAndPlaying_returnsTrueToDisableVideo() {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer
        player.playWhenReady = true

        val activity = MainActivity()

        val shouldDisable = activity.handleBackgroundAudioOnStop(
            player = player,
            backgroundAudioEnabled = true,
            isChangingConfigurations = false,
            isInPictureInPictureMode = false
        )

        assertTrue("Must return true to disable video decoding during background audio (PERF-05)", shouldDisable)
    }

    @Test
    fun handleBackgroundAudioOnStop_whenBackgroundAudioDisabled_pausesPlayerAndReturnsFalse() {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer
        player.playWhenReady = true

        val activity = MainActivity()

        val shouldDisable = activity.handleBackgroundAudioOnStop(
            player = player,
            backgroundAudioEnabled = false,
            isChangingConfigurations = false,
            isInPictureInPictureMode = false
        )

        assertFalse(shouldDisable)
        assertFalse("Player must be paused when background audio is disabled", player.playWhenReady)
    }

    @Test
    fun handleBackgroundAudioOnStop_whenInPictureInPictureMode_returnsFalseToKeepVideoPlaying() {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer
        player.playWhenReady = true

        val activity = MainActivity()

        val shouldDisable = activity.handleBackgroundAudioOnStop(
            player = player,
            backgroundAudioEnabled = true,
            isChangingConfigurations = false,
            isInPictureInPictureMode = true
        )

        assertFalse("Must keep video rendering while in picture-in-picture mode", shouldDisable)
    }

    @Test
    fun handleBackgroundAudioOnStop_whenChangingConfigurations_returnsFalse() {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer
        player.playWhenReady = true

        val activity = MainActivity()

        val shouldDisable = activity.handleBackgroundAudioOnStop(
            player = player,
            backgroundAudioEnabled = true,
            isChangingConfigurations = true,
            isInPictureInPictureMode = false
        )

        assertFalse("Must not interrupt video track when configuration is changing (e.g. rotation)", shouldDisable)
    }

    @Test
    fun handleBackgroundAudioOnStop_whenAlreadyAudioOnly_returnsFalse() = runTest {
        val controller = PlayerManager.getOrCreate(app)
        val player = controller.player as ExoPlayer
        player.playWhenReady = true

        // User already in audio-only mode
        controller.setVideoEnabled(false)

        val activity = MainActivity()

        val shouldDisable = activity.handleBackgroundAudioOnStop(
            player = player,
            backgroundAudioEnabled = true,
            isChangingConfigurations = false,
            isInPictureInPictureMode = false
        )

        assertFalse("Must not re-disable or flag video if already disabled", shouldDisable)
    }
}
