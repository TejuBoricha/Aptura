@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.ui.player

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import org.aptura.player.R
import org.aptura.player.domain.model.PictureAdjustmentState

/**
 * Isolated Video Surface Composable (SCR-010).
 * Hoisted outside the 4 Hz playback position tick so it never recomposes during normal playback.
 * Automatically swaps between SurfaceView (zero copy, lowest power) and TextureView (color filtering).
 */
@Composable
fun PlayerVideoSurface(
    player: Player?,
    resizeMode: AspectRatioMode,
    pictureAdjustment: PictureAdjustmentState,
    isAudioOnly: Boolean,
    subtitleTextColorHex: String,
    subtitleBackgroundEnabled: Boolean,
    subtitleFontSizeSp: Int,
    isControlsVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isAudioOnly) return

    val useColorFilterSurface = pictureAdjustment.isModified
    key(useColorFilterSurface) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { ctx ->
                val layout = if (useColorFilterSurface) {
                    R.layout.aptura_player_view_texture
                } else {
                    R.layout.aptura_player_view_surface
                }
                (LayoutInflater.from(ctx).inflate(layout, null, false) as PlayerView).apply {
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.player = player
                }
            },
            update = { playerView ->
                if (playerView.player != player) {
                    playerView.player = player
                }
                val contentFrame = playerView.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
                when (resizeMode) {
                    AspectRatioMode.FIT -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        val videoSize = player?.videoSize
                        if (videoSize != null && videoSize.width > 0 && videoSize.height > 0) {
                            contentFrame?.setAspectRatio(videoSize.width.toFloat() / videoSize.height.toFloat())
                        }
                    }
                    AspectRatioMode.FIXED_16_9 -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        contentFrame?.setAspectRatio(16f / 9f)
                    }
                    AspectRatioMode.FIXED_4_3 -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        contentFrame?.setAspectRatio(4f / 3f)
                    }
                    AspectRatioMode.FILL -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                    AspectRatioMode.ZOOM -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                }

                val styleKey = "$subtitleTextColorHex|$subtitleBackgroundEnabled|$subtitleFontSizeSp|$isControlsVisible"
                if (playerView.tag != styleKey) {
                    playerView.tag = styleKey
                    val fgColor = try {
                        Color.parseColor(subtitleTextColorHex)
                    } catch (_: Throwable) {
                        Color.WHITE
                    }
                    val bgColor = if (subtitleBackgroundEnabled) {
                        Color.argb(160, 0, 0, 0)
                    } else {
                        Color.TRANSPARENT
                    }
                    val edgeType = if (subtitleBackgroundEnabled) {
                        CaptionStyleCompat.EDGE_TYPE_NONE
                    } else {
                        CaptionStyleCompat.EDGE_TYPE_OUTLINE
                    }
                    playerView.subtitleView?.apply {
                        setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleFontSizeSp.toFloat())
                        setStyle(CaptionStyleCompat(fgColor, bgColor, Color.TRANSPARENT, edgeType, Color.BLACK, null))
                        setBottomPaddingFraction(if (isControlsVisible) 0.16f else 0.09f)
                    }
                }

                PictureFilter.apply(playerView.videoSurfaceView as? TextureView, pictureAdjustment)
            },
            onRelease = { playerView ->
                playerView.player = null
            }
        )
    }
}
