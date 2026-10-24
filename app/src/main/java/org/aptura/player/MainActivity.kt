package org.aptura.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.VideoSize
import kotlinx.coroutines.launch
import org.aptura.player.data.playback.PlayerManager
import org.aptura.player.data.settings.ApturaSettingsRepository
import org.aptura.player.ui.player.PipSupport

class MainActivity : ComponentActivity() {

    private val isPipMode = mutableStateOf(false)

    /** The `ACTION_VIEW` URI that has not been handed to the UI yet. Consumed exactly once. */
    private var pendingLaunchUri by mutableStateOf<String?>(null)

    /** Mirrors the "Background Audio Playback" setting so [onStop] can honour it synchronously. */
    private var backgroundAudioEnabled = true

    /** True only while the active navigation destination is [PlayerKey]. */
    private var isPlayerRouteActive = false

    private var observedPlayer: Player? = null
    private val pipStateListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updatePictureInPictureParams()
        override fun onVideoSizeChanged(videoSize: VideoSize) = updatePictureInPictureParams()
        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) =
            updatePictureInPictureParams()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Only the *first* creation consumes the launching intent. After a configuration change the system
        // re-delivers the same intent; handling it again stacked a duplicate player entry (audit P1-02).
        val alreadyConsumed = savedInstanceState?.getBoolean(STATE_LAUNCH_CONSUMED, false) == true
        if (!alreadyConsumed) {
            pendingLaunchUri = launchUriFrom(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Edge-to-edge with transparent bars. Their icon colour follows the in-app theme, which can differ from the
        // system's own mode, so ApturaTheme sets it on every theme change instead of pinning it here (F-26).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MainNavigation(
                initialLaunchUri = pendingLaunchUri,
                onLaunchUriConsumed = { pendingLaunchUri = null },
                isPipMode = isPipMode.value,
                onEnterPip = ::enterPictureInPicture,
                onPlayerRouteActiveChanged = { active ->
                    isPlayerRouteActive = active
                    updatePictureInPictureParams()
                }
            )
        }

        observePlayerForPictureInPicture()

        lifecycleScope.launch {
            ApturaSettingsRepository(this@MainActivity).settingsFlow.collect { settings ->
                backgroundAudioEnabled = settings.backgroundAudioEnabled
            }
        }
    }

    /**
     * "Background Audio Playback" was a toggle nothing read. When it is OFF, leaving the app (not merely rotating
     * or going into picture-in-picture) pauses playback.
     */
    override fun onStop() {
        super.onStop()
        if (!backgroundAudioEnabled && !isChangingConfigurations && !isInPictureInPictureMode) {
            PlayerManager.activeController?.player?.pause()
        }
    }

    /** A second "Open with" while the app is already running reuses this instance (launchMode=singleTask). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchUriFrom(intent)?.let { pendingLaunchUri = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_LAUNCH_CONSUMED, true)
    }

    override fun onDestroy() {
        observedPlayer?.removeListener(pipStateListener)
        observedPlayer = null
        super.onDestroy()
    }

    /**
     * API 26-30 have no auto-enter: enter PiP when the user leaves while playing.
     * API 31+ enters automatically from [updatePictureInPictureParams] (so it works on the *first* Home press).
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && isPlayingVideo() && !isPipMode.value) {
            enterPictureInPicture()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipMode.value = isInPictureInPictureMode
    }

    private fun supportsPictureInPicture(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    /** True only when the user is on the Player screen and actively playing video (not in Audio-Only mode). */
    private fun isPlayingVideo(): Boolean {
        if (!isPlayerRouteActive) return false
        val player = PlayerManager.activeController?.player ?: return false
        if (!player.isPlaying) return false
        val isVideoDisabled = try {
            player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
        } catch (_: Throwable) {
            false
        }
        return !isVideoDisabled
    }

    private fun observePlayerForPictureInPicture() {
        if (!supportsPictureInPicture()) return
        val player = PlayerManager.getOrCreate(this).player
        observedPlayer = player
        player.addListener(pipStateListener)
        updatePictureInPictureParams()
    }

    private fun buildPictureInPictureParams(autoEnter: Boolean): PictureInPictureParams {
        val size = PlayerManager.activeController?.player?.videoSize
        val (num, den) = PipSupport.aspectRatioFor(size?.width ?: 0, size?.height ?: 0)
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(num, den))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
        }
        return builder.build()
    }

    /** Keeps the system's PiP parameters in sync so Home enters PiP immediately while a video plays. */
    private fun updatePictureInPictureParams() {
        if (!supportsPictureInPicture()) return
        try {
            setPictureInPictureParams(buildPictureInPictureParams(autoEnter = isPlayingVideo()))
        } catch (_: IllegalStateException) {
            // Activity is finishing or PiP is disabled for this window; nothing to update.
        }
    }

    fun enterPictureInPicture() {
        if (!supportsPictureInPicture()) return
        try {
            enterPictureInPictureMode(buildPictureInPictureParams(autoEnter = isPlayingVideo()))
        } catch (_: IllegalStateException) {
            // PiP disabled by the user/device policy.
        }
    }

    private fun launchUriFrom(intent: Intent?): String? =
        if (intent?.action == Intent.ACTION_VIEW) intent.data?.toString() else null

    private companion object {
        const val STATE_LAUNCH_CONSUMED = "aptura.launch_consumed"
    }
}
