@file:androidx.annotation.OptIn(
    androidx.media3.common.util.UnstableApi::class
)
@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package org.aptura.player.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.library.MediaFormatters

/**
 * Jetpack Compose Player Screen & Video Surface HUD (SCR-010).
 * Implements Media3 PlayerView interop, 5s auto-hide HUD, edge-to-edge gestures,
 * and high-contrast tactile media controls conforming to taste-compose.md.
 */
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    player: Player?,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    onScrubStart: (Long) -> Unit,
    onScrubMove: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    onControlsVisibilityToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onResizeModeToggle: () -> Unit,
    onSpeedSheetVisible: (Boolean) -> Unit,
    onAudioSheetVisible: (Boolean) -> Unit,
    onSubtitleSheetVisible: (Boolean) -> Unit,
    onAudioTrackSelected: (Int?) -> Unit,
    onSubtitleTrackSelected: (Int?) -> Unit,
    onVolumePercentChanged: (Int) -> Unit,
    onBrightnessPercentChanged: (Int) -> Unit,
    onRetryPlayback: () -> Unit,
    onPlayModeToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val activity = context as? Activity
    DisposableEffect(activity, uiState.isControlsVisible) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (uiState.isControlsVisible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            window?.let {
                WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApturaColors.SurfacePureBlack)
    ) {
        // 1. Video Surface (Media3 PlayerView)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.player = player
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = when (uiState.resizeMode) {
                    AspectRatioMode.FIT, AspectRatioMode.FIXED_16_9, AspectRatioMode.FIXED_4_3 ->
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        )

        // 2. Gesture Detector Layer (Left Half: Brightness & Rewind, Right Half: Volume & Fast-forward)
        Row(modifier = Modifier.fillMaxSize()) {
            // Left half
            var leftDragAccumulator by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(uiState.isLocked) {
                        detectTapGestures(
                            onTap = { onControlsVisibilityToggle() },
                            onDoubleTap = {
                                if (!uiState.isLocked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSeekRelative(-10_000L)
                                }
                            }
                        )
                    }
                    .pointerInput(uiState.isLocked) {
                        if (!uiState.isLocked) {
                            detectVerticalDragGestures(
                                onDragStart = { leftDragAccumulator = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    leftDragAccumulator -= dragAmount
                                    val activity = context as? Activity
                                    val window = activity?.window
                                    if (window != null) {
                                        val currentBrightness = window.attributes.screenBrightness.let {
                                            if (it < 0f) 0.5f else it
                                        }
                                        val newBrightness = (currentBrightness + (leftDragAccumulator / 2000f))
                                            .coerceIn(0.01f, 1.0f)
                                        val lp = window.attributes
                                        lp.screenBrightness = newBrightness
                                        window.attributes = lp
                                        onBrightnessPercentChanged((newBrightness * 100).toInt())
                                    }
                                }
                            )
                        }
                    }
            )

            // Right half
            var rightDragAccumulator by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(uiState.isLocked) {
                        detectTapGestures(
                            onTap = { onControlsVisibilityToggle() },
                            onDoubleTap = {
                                if (!uiState.isLocked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSeekRelative(10_000L)
                                }
                            }
                        )
                    }
                    .pointerInput(uiState.isLocked) {
                        if (!uiState.isLocked) {
                            detectVerticalDragGestures(
                                onDragStart = { rightDragAccumulator = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    rightDragAccumulator -= dragAmount
                                    if (audioManager != null && maxVolume > 0) {
                                        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val step = if (rightDragAccumulator > 30f) 1 else if (rightDragAccumulator < -30f) -1 else 0
                                        if (step != 0) {
                                            val newVol = (currentVol + step).coerceIn(0, maxVolume)
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                            rightDragAccumulator = 0f
                                            onVolumePercentChanged((newVol * 100 / maxVolume))
                                        }
                                    }
                                }
                            )
                        }
                    }
            )
        }

        // 3. Transient Feedback Overlays (Volume, Brightness, Double-Tap Seek)
        // Center Volume Indicator
        AnimatedVisibility(
            visible = uiState.volumePercent != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            uiState.volumePercent?.let { vol ->
                Surface(
                    color = ApturaColors.SurfaceElevated2.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            tint = ApturaColors.AccentPrimary
                        )
                        Text(
                            text = "$vol%",
                            style = ApturaTypography.titleMedium,
                            color = ApturaColors.TextHighEmphasis
                        )
                    }
                }
            }
        }

        // Center Brightness Indicator
        AnimatedVisibility(
            visible = uiState.brightnessPercent != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            uiState.brightnessPercent?.let { bri ->
                Surface(
                    color = ApturaColors.SurfaceElevated2.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrightnessMedium,
                            contentDescription = "Brightness",
                            tint = ApturaColors.AccentPrimary
                        )
                        Text(
                            text = "$bri%",
                            style = ApturaTypography.titleMedium,
                            color = ApturaColors.TextHighEmphasis
                        )
                    }
                }
            }
        }

        // Double-Tap Seek Delta Pulse Indicator
        AnimatedVisibility(
            visible = uiState.seekDeltaSeconds != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(
                if ((uiState.seekDeltaSeconds ?: 0) >= 0) Alignment.CenterEnd else Alignment.CenterStart
            )
        ) {
            uiState.seekDeltaSeconds?.let { delta ->
                val isForward = delta >= 0
                Surface(
                    color = ApturaColors.SurfaceElevated2.copy(alpha = 0.85f),
                    shape = CircleShape,
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isForward) Icons.Filled.Forward10 else Icons.Filled.Replay10,
                            contentDescription = null,
                            tint = ApturaColors.AccentPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isForward) "+${delta}s" else "${delta}s",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.TextHighEmphasis
                        )
                    }
                }
            }
        }

        // Center Buffering Spinner
        if (uiState.isBuffering && !uiState.isPlaying) {
            CircularProgressIndicator(
                color = ApturaColors.AccentPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
            )
        }

        // 4. Lock Mode Indicator (When locked)
        if (uiState.isLocked) {
            Surface(
                color = ApturaColors.SurfaceElevated2.copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
                    .clickable { onLockToggle() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Locked",
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Locked • Tap to unlock",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextHighEmphasis
                    )
                }
            }
        }

        // 5. Main Controls Overlay (Top Bar, Transport, Bottom Bar)
        AnimatedVisibility(
            visible = uiState.isControlsVisible && !uiState.isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar with gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ApturaColors.SurfaceScrim, Color.Transparent)
                            )
                        )
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ApturaColors.TextHighEmphasis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = uiState.videoTitle,
                            style = ApturaTypography.titleMedium,
                            color = ApturaColors.TextHighEmphasis,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Aspect ratio badge button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ApturaColors.SurfaceElevated2,
                            modifier = Modifier
                                .clickable { onResizeModeToggle() }
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = uiState.resizeMode.label,
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.AccentPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Speed selector button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ApturaColors.SurfaceElevated2,
                            modifier = Modifier
                                .clickable { onSpeedSheetVisible(true) }
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Speed,
                                    contentDescription = "Speed",
                                    tint = ApturaColors.TextMediumEmphasis,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${uiState.playbackSpeed}x",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.TextHighEmphasis
                                )
                            }
                        }
                    }
                }

                // Center Transport Cluster (⏪10s • ⏯ • ⏩10s)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSeekRelative(-10_000L)
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = ApturaColors.TextHighEmphasis,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPlayPauseClick()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(ApturaColors.AccentPrimary, CircleShape)
                    ) {
                        Icon(
                            imageVector = when {
                                uiState.isEnded -> Icons.Filled.Replay
                                uiState.isPlaying -> Icons.Filled.Pause
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                            tint = ApturaColors.TextOnAccent,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSeekRelative(10_000L)
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward 10s",
                            tint = ApturaColors.TextHighEmphasis,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Bar with gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, ApturaColors.SurfaceScrim)
                            )
                        )
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Scrubber timeline row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = MediaFormatters.formatDuration(uiState.effectivePositionMs),
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextHighEmphasis
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // Secondary buffered track indicator behind the slider
                                if (uiState.bufferedFraction > 0f) {
                                    LinearProgressIndicator(
                                        progress = { uiState.bufferedFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = ApturaColors.SurfaceElevated3,
                                        trackColor = ApturaColors.SurfaceElevated1
                                    )
                                }

                                Slider(
                                    value = uiState.progressFraction,
                                    onValueChange = { fraction ->
                                        val targetMs = (fraction * uiState.durationMs).toLong()
                                        onScrubStart(targetMs)
                                        onScrubMove(targetMs)
                                    },
                                    onValueChangeFinished = {
                                        onScrubEnd()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = ApturaColors.AccentPrimary,
                                        activeTrackColor = ApturaColors.AccentPrimary,
                                        inactiveTrackColor = if (uiState.bufferedFraction > 0f) Color.Transparent else ApturaColors.SurfaceElevated2
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = MediaFormatters.formatDuration(uiState.durationMs),
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextMediumEmphasis
                            )
                        }

                        // Bottom Actions Row (Lock, Subs, Audio, Repeat)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onLockToggle,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LockOpen,
                                    contentDescription = "Lock Controls",
                                    tint = ApturaColors.TextMediumEmphasis
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { onSubtitleSheetVisible(true) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Subtitles,
                                        contentDescription = "Subtitles",
                                        tint = if (uiState.selectedSubtitleTrackIndex != null)
                                            ApturaColors.AccentPrimary
                                        else
                                            ApturaColors.TextMediumEmphasis
                                    )
                                }

                                IconButton(
                                    onClick = { onAudioSheetVisible(true) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Audiotrack,
                                        contentDescription = "Audio Track",
                                        tint = ApturaColors.TextMediumEmphasis
                                    )
                                }

                                IconButton(
                                    onClick = onPlayModeToggle,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = when (uiState.playMode) {
                                            PlayMode.REPEAT_ONE -> Icons.Filled.RepeatOne
                                            PlayMode.REPEAT_ALL -> Icons.Filled.Repeat
                                            PlayMode.SHUFFLE -> Icons.Filled.Shuffle
                                            PlayMode.NORMAL -> Icons.Filled.Repeat
                                        },
                                        contentDescription = "Play Mode: ${uiState.playMode.name}",
                                        tint = if (uiState.playMode != PlayMode.NORMAL)
                                            ApturaColors.AccentPrimary
                                        else
                                            ApturaColors.TextLowEmphasis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Playback Error State Banner (SCR-010x)
        if (uiState.errorMessage != null) {
            Surface(
                color = ApturaColors.SurfaceElevated2,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApturaColors.StatusError),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Playback Error",
                        style = ApturaTypography.titleMedium,
                        color = ApturaColors.StatusError
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        style = ApturaTypography.bodyMedium,
                        color = ApturaColors.TextHighEmphasis
                    )
                    uiState.errorCode?.let { code ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Code: $code",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.TextLowEmphasis
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onBackClick,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ApturaColors.TextHighEmphasis
                            )
                        ) {
                            Text("Go Back")
                        }
                        if (uiState.isRecoverableError) {
                            Button(
                                onClick = onRetryPlayback,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ApturaColors.AccentPrimary,
                                    contentColor = ApturaColors.TextOnAccent
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }

        // 7. Speed Selection Sheet (SCR-010b)
        if (uiState.isSpeedSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { onSpeedSheetVisible(false) },
                sheetState = rememberModalBottomSheetState(),
                containerColor = ApturaColors.SurfaceElevated2
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Playback Speed",
                        style = ApturaTypography.titleMedium,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                    LazyColumn {
                        items(speedOptions) { speed ->
                            val isSelected = uiState.playbackSpeed == speed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSpeedSelected(speed) }
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (speed == 1.0f) "Normal (1.0x)" else "${speed}x",
                                    style = ApturaTypography.bodyMedium,
                                    color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = ApturaColors.AccentPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Audio Track Selection Sheet (SCR-010d)
        if (uiState.isAudioSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { onAudioSheetVisible(false) },
                sheetState = rememberModalBottomSheetState(),
                containerColor = ApturaColors.SurfaceElevated2
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Audio Tracks",
                        style = ApturaTypography.titleMedium,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    if (uiState.audioTracks.isEmpty()) {
                        Text(
                            text = "Default Audio Track",
                            style = ApturaTypography.bodyMedium,
                            color = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                        )
                    } else {
                        LazyColumn {
                            items(uiState.audioTracks) { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAudioTrackSelected(track.index) }
                                        .padding(horizontal = 24.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = track.label,
                                        style = ApturaTypography.bodyMedium,
                                        color = if (track.isSelected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                                    )
                                    if (track.isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = ApturaColors.AccentPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 9. Subtitle Track Selection Sheet (SCR-010c)
        if (uiState.isSubtitleSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { onSubtitleSheetVisible(false) },
                sheetState = rememberModalBottomSheetState(),
                containerColor = ApturaColors.SurfaceElevated2
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Subtitles",
                        style = ApturaTypography.titleMedium,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubtitleTrackSelected(null) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Off",
                            style = ApturaTypography.bodyMedium,
                            color = if (uiState.selectedSubtitleTrackIndex == null)
                                ApturaColors.AccentPrimary
                            else
                                ApturaColors.TextHighEmphasis
                        )
                        if (uiState.selectedSubtitleTrackIndex == null) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = ApturaColors.AccentPrimary
                            )
                        }
                    }
                    LazyColumn {
                        items(uiState.subtitleTracks) { track ->
                            val isSelected = uiState.selectedSubtitleTrackIndex == track.index
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSubtitleTrackSelected(track.index) }
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.label,
                                    style = ApturaTypography.bodyMedium,
                                    color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = ApturaColors.AccentPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
