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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.aptura.player.domain.model.EqualizerPreset
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.pm.ActivityInfo
import android.util.TypedValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.ui.CaptionStyleCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Bedtime
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
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
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onAdvancedSheetVisible: (Boolean) -> Unit = {},
    onSubtitleDelayChanged: (Long) -> Unit = {},
    onAudioDelayChanged: (Long) -> Unit = {},
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onDelayStepChanged: (Long) -> Unit = {},
    onEqualizerEnabledChanged: (Boolean) -> Unit = {},
    onEqualizerPresetSelected: (EqualizerPreset) -> Unit = {},
    onEqualizerBandChanged: (bandIndex: Int, levelMilliBels: Int) -> Unit = { _, _ -> },
    onBassBoostChanged: (Int) -> Unit = {},
    onVirtualizerChanged: (Int) -> Unit = {},
    onLoudnessBoostChanged: (Int) -> Unit = {},
    onContrastChanged: (Float) -> Unit = {},
    onSaturationChanged: (Float) -> Unit = {},
    onNightModeToggle: (Boolean) -> Unit = {},
    onNightModeWarmthChanged: (Float) -> Unit = {},
    onResetPictureAdjustment: () -> Unit = {},
    onSetAbPointA: () -> Unit = {},
    onSetAbPointB: () -> Unit = {},
    onClearAbRepeat: () -> Unit = {},
    onStepFrame: (Boolean) -> Unit = {},
    onCancelAutoAdvance: () -> Unit = {},
    onPlayNextImmediately: () -> Unit = {},
    isPipMode: Boolean = false,
    onEnterPip: () -> Unit = {},
    onExternalSubtitleSelected: (android.net.Uri) -> Unit = {},
    onSubtitleFontSizeChanged: (Int) -> Unit = {},
    onSubtitleTextColorChanged: (String) -> Unit = {},
    onSubtitleBackgroundChanged: (Boolean) -> Unit = {},
    onAddBookmark: () -> Unit = {},
    onDeleteBookmark: (Long) -> Unit = {},
    onDecoderStrategyChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var is2xSpeedActive by remember { mutableStateOf(false) }
    var currentOrientationMode by remember { mutableIntStateOf(0) } // 0: Auto, 1: Landscape, 2: Portrait
    var orientationToastMessage by remember { mutableStateOf<String?>(null) }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onExternalSubtitleSelected(uri)
        }
    }

    val activity = context as? Activity
    DisposableEffect(activity, uiState.isControlsVisible, isPipMode) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (uiState.isControlsVisible && !isPipMode) {
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

    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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

                val fgColor = try {
                    android.graphics.Color.parseColor(uiState.subtitleTextColorHex)
                } catch (_: Throwable) {
                    android.graphics.Color.WHITE
                }
                val bgColor = if (uiState.subtitleBackgroundEnabled) {
                    android.graphics.Color.argb(160, 0, 0, 0)
                } else {
                    android.graphics.Color.TRANSPARENT
                }
                val edgeType = if (uiState.subtitleBackgroundEnabled) {
                    CaptionStyleCompat.EDGE_TYPE_NONE
                } else {
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE
                }
                playerView.subtitleView?.apply {
                    setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, uiState.subtitleFontSizeSp.toFloat())
                    setStyle(CaptionStyleCompat(fgColor, bgColor, android.graphics.Color.TRANSPARENT, edgeType, android.graphics.Color.BLACK, null))
                }
            }
        )

        // 1b. Eye-Comfort Night Mode Filter (Amber warmth attenuation)
        if (uiState.pictureAdjustment.isNightModeEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color(0xFFFF9800).copy(
                            alpha = (0.12f + 0.18f * uiState.pictureAdjustment.nightModeWarmth)
                        )
                    )
            )
        }

        // 2. Gesture Detector Layer (Left Half: Brightness & Rewind, Right Half: Volume & Fast-forward)
        Row(modifier = Modifier.fillMaxSize()) {
            // Left half
            var leftDragAccumulator by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(uiState.isLocked, uiState.isPlaying) {
                        detectTapGestures(
                            onTap = { onControlsVisibilityToggle() },
                            onDoubleTap = {
                                if (!uiState.isLocked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSeekRelative(-10_000L)
                                }
                            },
                            onPress = {
                                var longPressActive = false
                                val job = coroutineScope.launch {
                                    kotlinx.coroutines.delay(400L)
                                    if (!uiState.isLocked && uiState.isPlaying) {
                                        longPressActive = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        is2xSpeedActive = true
                                        onSpeedSelected(2.0f)
                                    }
                                }
                                tryAwaitRelease()
                                job.cancel()
                                if (longPressActive) {
                                    is2xSpeedActive = false
                                    onSpeedSelected(uiState.playbackSpeed)
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
                    .pointerInput(uiState.isLocked, uiState.isPlaying) {
                        detectTapGestures(
                            onTap = { onControlsVisibilityToggle() },
                            onDoubleTap = {
                                if (!uiState.isLocked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSeekRelative(10_000L)
                                }
                            },
                            onPress = {
                                var longPressActive = false
                                val job = coroutineScope.launch {
                                    kotlinx.coroutines.delay(400L)
                                    if (!uiState.isLocked && uiState.isPlaying) {
                                        longPressActive = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        is2xSpeedActive = true
                                        onSpeedSelected(2.0f)
                                    }
                                }
                                tryAwaitRelease()
                                job.cancel()
                                if (longPressActive) {
                                    is2xSpeedActive = false
                                    onSpeedSelected(uiState.playbackSpeed)
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

        // Floating 2.0x Fast-Forward Indicator (SCR-010f)
        AnimatedVisibility(
            visible = is2xSpeedActive && !isPipMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            Surface(
                color = ApturaColors.SurfaceElevated2.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ApturaColors.AccentPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FastForward,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "2.0x Fast-Forward",
                        style = ApturaTypography.labelMedium,
                        color = ApturaColors.AccentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Momentary Orientation Feedback Toast
        AnimatedVisibility(
            visible = orientationToastMessage != null && !isPipMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
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
                        imageVector = Icons.Filled.ScreenRotation,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary
                    )
                    Text(
                        text = orientationToastMessage.orEmpty(),
                        style = ApturaTypography.titleMedium,
                        color = ApturaColors.TextHighEmphasis
                    )
                }
            }
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
            visible = uiState.isControlsVisible && !uiState.isLocked && !isPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar with gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
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

                        // Orientation toggle button (SCR-010e)
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOrientationMode = (currentOrientationMode + 1) % 3
                                when (currentOrientationMode) {
                                    1 -> {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        orientationToastMessage = "Landscape Locked"
                                    }
                                    2 -> {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                        orientationToastMessage = "Portrait Locked"
                                    }
                                    else -> {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                        orientationToastMessage = "Auto-Rotate"
                                    }
                                }
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1800L)
                                    orientationToastMessage = null
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ScreenRotation,
                                contentDescription = "Screen Orientation",
                                tint = if (currentOrientationMode != 0) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis
                            )
                        }

                        // PiP button (SCR-010e, FR-CTRL-014)
                        IconButton(
                            onClick = onEnterPip,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureInPictureAlt,
                                contentDescription = "Picture in Picture",
                                tint = ApturaColors.TextHighEmphasis
                            )
                        }

                        // Aspect ratio badge button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ApturaColors.SurfaceScrim.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable { onResizeModeToggle() }
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = ApturaColors.AccentPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = uiState.resizeMode.label,
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.AccentPrimary
                                )
                            }
                        }

                        // Speed selector button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ApturaColors.SurfaceScrim.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable { onSpeedSheetVisible(true) }
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Speed,
                                    contentDescription = "Speed",
                                    tint = ApturaColors.TextMediumEmphasis,
                                    modifier = Modifier.size(14.dp)
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

                // Center Transport Cluster (⏮ Prev • ⏪10s • ⏯ • ⏩10s • ⏭ Next)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipPrevious()
                        },
                        enabled = uiState.hasPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .background(ApturaColors.SurfaceScrim.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous Video",
                            tint = if (uiState.hasPrevious) ApturaColors.TextHighEmphasis else ApturaColors.TextLowEmphasis.copy(alpha = 0.38f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSeekRelative(-10_000L)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(ApturaColors.SurfaceScrim.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = ApturaColors.TextHighEmphasis,
                            modifier = Modifier.size(30.dp)
                        )
                    }

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

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSeekRelative(10_000L)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(ApturaColors.SurfaceScrim.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward 10s",
                            tint = ApturaColors.TextHighEmphasis,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipNext()
                        },
                        enabled = uiState.hasNext,
                        modifier = Modifier
                            .size(48.dp)
                            .background(ApturaColors.SurfaceScrim.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Video",
                            tint = if (uiState.hasNext) ApturaColors.TextHighEmphasis else ApturaColors.TextLowEmphasis.copy(alpha = 0.38f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Bar with gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
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
                                style = ApturaTypography.labelSmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                color = ApturaColors.TextHighEmphasis
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Custom Pixel-Perfect Video Scrubber (Concentric cy = size.height / 2f)
                            var scrubberWidthPx by remember { mutableFloatStateOf(0f) }
                            val trackHeight = 4.dp
                            val thumbRadius = if (uiState.isScrubbing) 8.dp else 6.dp

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .onSizeChanged { scrubberWidthPx = it.width.toFloat() }
                                    .pointerInput(uiState.durationMs) {
                                        detectTapGestures(
                                            onPress = { offset ->
                                                if (scrubberWidthPx > 0f && uiState.durationMs > 0) {
                                                    val fraction = (offset.x / scrubberWidthPx).coerceIn(0f, 1f)
                                                    val targetMs = (fraction * uiState.durationMs).toLong()
                                                    onScrubStart(targetMs)
                                                    onScrubMove(targetMs)
                                                    tryAwaitRelease()
                                                    onScrubEnd()
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        )
                                    }
                                    .pointerInput(uiState.durationMs) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { offset ->
                                                if (scrubberWidthPx > 0f && uiState.durationMs > 0) {
                                                    val fraction = (offset.x / scrubberWidthPx).coerceIn(0f, 1f)
                                                    val targetMs = (fraction * uiState.durationMs).toLong()
                                                    onScrubStart(targetMs)
                                                    onScrubMove(targetMs)
                                                }
                                            },
                                            onDragEnd = {
                                                onScrubEnd()
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            },
                                            onDragCancel = {
                                                onScrubEnd()
                                            },
                                            onHorizontalDrag = { change, _ ->
                                                change.consume()
                                                if (scrubberWidthPx > 0f && uiState.durationMs > 0) {
                                                    val fraction = (change.position.x / scrubberWidthPx).coerceIn(0f, 1f)
                                                    val targetMs = (fraction * uiState.durationMs).toLong()
                                                    onScrubMove(targetMs)
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    val cy = size.height / 2f
                                    val trackH = trackHeight.toPx()
                                    val thumbR = thumbRadius.toPx()
                                    val totalW = size.width

                                    // 1. Background Inactive Track
                                    drawRoundRect(
                                        color = ApturaColors.SurfaceElevated3,
                                        topLeft = Offset(0f, cy - trackH / 2f),
                                        size = Size(totalW, trackH),
                                        cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                                    )

                                    // 2. Buffered Track
                                    if (uiState.bufferedFraction > 0f) {
                                        val buffW = totalW * uiState.bufferedFraction.coerceIn(0f, 1f)
                                        drawRoundRect(
                                            color = ApturaColors.TextMediumEmphasis.copy(alpha = 0.35f),
                                            topLeft = Offset(0f, cy - trackH / 2f),
                                            size = Size(buffW, trackH),
                                            cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                                        )
                                    }

                                    // 3. Active Played Track
                                    val activeW = totalW * uiState.progressFraction.coerceIn(0f, 1f)
                                    if (activeW > 0f) {
                                        drawRoundRect(
                                            color = ApturaColors.AccentPrimary,
                                            topLeft = Offset(0f, cy - trackH / 2f),
                                            size = Size(activeW, trackH),
                                            cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                                        )
                                    }

                                    // 3b. Bookmark Pips (FR-CTRL-011)
                                    if (uiState.durationMs > 0 && uiState.bookmarks.isNotEmpty()) {
                                        val pipW = 3.dp.toPx()
                                        val pipH = (trackH * 1.8f).coerceAtLeast(8.dp.toPx())
                                        uiState.bookmarks.forEach { bookmark ->
                                            val pipFraction = (bookmark.timestampMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f)
                                            val pipX = totalW * pipFraction
                                            drawRoundRect(
                                                color = Color.White.copy(alpha = 0.9f),
                                                topLeft = Offset(pipX - pipW / 2f, cy - pipH / 2f),
                                                size = Size(pipW, pipH),
                                                cornerRadius = CornerRadius(pipW / 2f, pipW / 2f)
                                            )
                                        }
                                    }

                                    // 4. Thumb Circle (Concentric cy = size.height / 2f)
                                    val clampedThumbX = if (totalW > thumbR * 2) {
                                        activeW.coerceIn(thumbR, totalW - thumbR)
                                    } else {
                                        totalW / 2f
                                    }
                                    drawCircle(
                                        color = ApturaColors.AccentPrimary,
                                        radius = thumbR,
                                        center = Offset(clampedThumbX, cy)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = MediaFormatters.formatDuration(uiState.durationMs),
                                style = ApturaTypography.labelSmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
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

                                IconButton(
                                    onClick = { onAdvancedSheetVisible(true) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = "Advanced Settings",
                                        tint = if (uiState.subtitleDelayMs != 0L || uiState.audioDelayMs != 0L || uiState.sleepTimerRemainingMs != null)
                                            ApturaColors.AccentPrimary
                                        else
                                            ApturaColors.TextMediumEmphasis
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

        // 7. Up Next Auto-Advance Countdown Overlay (SCR-010g)
        if (uiState.isEnded && uiState.autoAdvanceCountdownSeconds != null) {
            UpNextCountdownOverlay(
                nextTitle = uiState.nextItemTitle ?: "Next Video",
                countdownSeconds = uiState.autoAdvanceCountdownSeconds,
                onPlayNext = onPlayNextImmediately,
                onCancel = onCancelAutoAdvance,
                modifier = Modifier.align(Alignment.Center)
            )
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

        // 8. Audio & Sound Enhancements Sheet (SCR-010d)
        if (uiState.isAudioSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { onAudioSheetVisible(false) },
                sheetState = rememberModalBottomSheetState(),
                containerColor = ApturaColors.SurfaceElevated2
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 36.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Audio & Sound Enhancements",
                        style = ApturaTypography.titleMedium,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // SECTION 1: AUDIO TRACKS
                    Text(
                        text = "AUDIO TRACK",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.AccentPrimary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    if (uiState.audioTracks.isEmpty()) {
                        Text(
                            text = "Default Audio Track",
                            style = ApturaTypography.bodyMedium,
                            color = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        uiState.audioTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAudioTrackSelected(track.index) }
                                    .padding(vertical = 12.dp),
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: AUDIO SYNC (LIP-SYNC DELAY)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUDIO SYNC (LIP-SYNC DELAY)",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.AccentPrimary
                        )
                        if (uiState.audioDelayMs != 0L) {
                            Text(
                                text = "Reset (0ms)",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.AccentPrimary,
                                modifier = Modifier
                                    .clickable { onAudioDelayChanged(0L) }
                                    .padding(start = 8.dp)
                            )
                        }
                    }

                    // Tactile Stepper Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ApturaColors.SurfaceElevated3,
                            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                            modifier = Modifier.clickable {
                                onAudioDelayChanged(uiState.audioDelayMs - uiState.delayStepMs)
                            }
                        ) {
                            Text(
                                text = "-${uiState.delayStepMs}ms",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextHighEmphasis,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }

                        Text(
                            text = "${if (uiState.audioDelayMs > 0) "+" else ""}${uiState.audioDelayMs} ms",
                            style = ApturaTypography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            color = if (uiState.audioDelayMs != 0L) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ApturaColors.SurfaceElevated3,
                            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                            modifier = Modifier.clickable {
                                onAudioDelayChanged(uiState.audioDelayMs + uiState.delayStepMs)
                            }
                        ) {
                            Text(
                                text = "+${uiState.delayStepMs}ms",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextHighEmphasis,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Quick Bluetooth & Sync Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.audioDelayMs == 150L) ApturaColors.AccentPrimary.copy(alpha = 0.2f) else ApturaColors.SurfaceElevated3,
                            border = BorderStroke(1.dp, if (uiState.audioDelayMs == 150L) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAudioDelayChanged(150L) }
                        ) {
                            Text(
                                text = "Buds (+150ms)",
                                style = ApturaTypography.labelSmall,
                                color = if (uiState.audioDelayMs == 150L) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.audioDelayMs == 250L) ApturaColors.AccentPrimary.copy(alpha = 0.2f) else ApturaColors.SurfaceElevated3,
                            border = BorderStroke(1.dp, if (uiState.audioDelayMs == 250L) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAudioDelayChanged(250L) }
                        ) {
                            Text(
                                text = "Speaker (+250ms)",
                                style = ApturaTypography.labelSmall,
                                color = if (uiState.audioDelayMs == 250L) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.audioDelayMs == -100L) ApturaColors.AccentPrimary.copy(alpha = 0.2f) else ApturaColors.SurfaceElevated3,
                            border = BorderStroke(1.dp, if (uiState.audioDelayMs == -100L) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAudioDelayChanged(-100L) }
                        ) {
                            Text(
                                text = "Dialogue (-100ms)",
                                style = ApturaTypography.labelSmall,
                                color = if (uiState.audioDelayMs == -100L) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // SECTION 3: SOUND ENHANCEMENTS & EQUALIZER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SOUND ENHANCEMENTS & EQUALIZER",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.AccentPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.audioEnhancement.enabled) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                            modifier = Modifier.clickable {
                                onEqualizerEnabledChanged(!uiState.audioEnhancement.enabled)
                            }
                        ) {
                            Text(
                                text = if (uiState.audioEnhancement.enabled) "ACTIVE" else "OFF",
                                style = ApturaTypography.labelSmall,
                                color = if (uiState.audioEnhancement.enabled) ApturaColors.SurfacePureBlack else ApturaColors.TextMediumEmphasis,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Equalizer Presets Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EqualizerPreset.entries.forEach { preset ->
                            val isSelected = uiState.audioEnhancement.preset == preset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                border = if (isSelected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                modifier = Modifier.clickable { onEqualizerPresetSelected(preset) }
                            ) {
                                Text(
                                    text = preset.displayName,
                                    style = ApturaTypography.labelSmall,
                                    color = if (isSelected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Multi-band equalizer sliders
                    if (uiState.audioEnhancement.bands.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        uiState.audioEnhancement.bands.forEach { band ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val freqText = if (band.centerFreqHz >= 1000) "${band.centerFreqHz / 1000} kHz" else "${band.centerFreqHz} Hz"
                                Text(
                                    text = freqText,
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.TextHighEmphasis,
                                    modifier = Modifier.width(60.dp)
                                )
                                Slider(
                                    value = band.levelMilliBels.toFloat(),
                                    onValueChange = { onEqualizerBandChanged(band.bandIndex, it.toInt()) },
                                    valueRange = band.minMilliBels.toFloat()..band.maxMilliBels.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = ApturaColors.AccentPrimary,
                                        activeTrackColor = ApturaColors.AccentPrimary,
                                        inactiveTrackColor = ApturaColors.SurfaceElevated3
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${band.levelMilliBels / 100} dB",
                                    style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = ApturaColors.TextMediumEmphasis,
                                    modifier = Modifier.width(50.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    // Bass Boost Slider
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bass Boost",
                            style = ApturaTypography.labelMedium,
                            color = ApturaColors.TextMediumEmphasis
                        )
                        Text(
                            text = "${uiState.audioEnhancement.bassBoostPercent}%",
                            style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = ApturaColors.AccentPrimary
                        )
                    }
                    Slider(
                        value = uiState.audioEnhancement.bassBoostPercent.toFloat(),
                        onValueChange = { onBassBoostChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = ApturaColors.AccentPrimary,
                            activeTrackColor = ApturaColors.AccentPrimary,
                            inactiveTrackColor = ApturaColors.SurfaceElevated3
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Virtualizer (Surround Sound) Slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Surround Sound Virtualizer",
                            style = ApturaTypography.labelMedium,
                            color = ApturaColors.TextMediumEmphasis
                        )
                        Text(
                            text = "${uiState.audioEnhancement.virtualizerPercent}%",
                            style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = ApturaColors.AccentPrimary
                        )
                    }
                    Slider(
                        value = uiState.audioEnhancement.virtualizerPercent.toFloat(),
                        onValueChange = { onVirtualizerChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = ApturaColors.AccentPrimary,
                            activeTrackColor = ApturaColors.AccentPrimary,
                            inactiveTrackColor = ApturaColors.SurfaceElevated3
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Loudness Enhancer (Dialogue Booster) Slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Loudness & Dialogue Boost",
                            style = ApturaTypography.labelMedium,
                            color = ApturaColors.TextMediumEmphasis
                        )
                        Text(
                            text = "${uiState.audioEnhancement.loudnessBoostPercent}%",
                            style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = ApturaColors.AccentPrimary
                        )
                    }
                    Slider(
                        value = uiState.audioEnhancement.loudnessBoostPercent.toFloat(),
                        onValueChange = { onLoudnessBoostChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = ApturaColors.AccentPrimary,
                            activeTrackColor = ApturaColors.AccentPrimary,
                            inactiveTrackColor = ApturaColors.SurfaceElevated3
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 9. Subtitle Track & Sync Sheet (SCR-010c)
        if (uiState.isSubtitleSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { onSubtitleSheetVisible(false) },
                sheetState = rememberModalBottomSheetState(),
                containerColor = ApturaColors.SurfaceElevated2
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 36.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Subtitles & Synchronization",
                        style = ApturaTypography.titleMedium,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // SECTION 1: SUBTITLE TRACKS
                    Text(
                        text = "SUBTITLE TRACKS",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.AccentPrimary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    // Off Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubtitleTrackSelected(null) }
                            .padding(vertical = 12.dp),
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

                    // Available Tracks
                    uiState.subtitleTracks.forEach { track ->
                        val isSelected = uiState.selectedSubtitleTrackIndex == track.index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSubtitleTrackSelected(track.index) }
                                .padding(vertical = 12.dp),
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

                    // Load External Subtitle (SAF)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                subtitlePickerLauncher.launch(
                                    arrayOf(
                                        "text/*",
                                        "application/x-subrip",
                                        "text/vtt",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "Load External Subtitle",
                            tint = ApturaColors.AccentPrimary
                        )
                        Column {
                            Text(
                                text = "Load External Subtitle…",
                                style = ApturaTypography.bodyMedium,
                                color = ApturaColors.AccentPrimary
                            )
                            Text(
                                text = "Select external .srt, .vtt, or .ass file",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextLowEmphasis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: SUBTITLE DELAY (SYNC)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SUBTITLE DELAY (SYNC)",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.AccentPrimary
                        )
                        if (uiState.subtitleDelayMs != 0L) {
                            Text(
                                text = "Reset (0ms)",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.AccentPrimary,
                                modifier = Modifier
                                    .clickable { onSubtitleDelayChanged(0L) }
                                    .padding(start = 8.dp)
                            )
                        }
                    }

                    // Tactile Stepper Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ApturaColors.SurfaceElevated3,
                            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                            modifier = Modifier.clickable {
                                onSubtitleDelayChanged(uiState.subtitleDelayMs - uiState.delayStepMs)
                            }
                        ) {
                            Text(
                                text = "-${uiState.delayStepMs}ms",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextHighEmphasis,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }

                        Text(
                            text = "${if (uiState.subtitleDelayMs > 0) "+" else ""}${uiState.subtitleDelayMs} ms",
                            style = ApturaTypography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            color = if (uiState.subtitleDelayMs != 0L) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ApturaColors.SurfaceElevated3,
                            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                            modifier = Modifier.clickable {
                                onSubtitleDelayChanged(uiState.subtitleDelayMs + uiState.delayStepMs)
                            }
                        ) {
                            Text(
                                text = "+${uiState.delayStepMs}ms",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextHighEmphasis,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Step Size Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step Size",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.TextMediumEmphasis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(10L, 50L, 100L, 250L).forEach { step ->
                                val selected = uiState.delayStepMs == step
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                    border = if (selected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                    modifier = Modifier.clickable { onDelayStepChanged(step) }
                                ) {
                                    Text(
                                        text = "${step}ms",
                                        style = ApturaTypography.labelSmall,
                                        color = if (selected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 3: SUBTITLE APPEARANCE & TYPOGRAPHY
                    Text(
                        text = "SUBTITLE APPEARANCE",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.AccentPrimary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    // Font Size Options
                    Text(
                        text = "Text Size",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextMediumEmphasis,
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(14, 18, 22, 26).forEach { size ->
                            val selected = uiState.subtitleFontSizeSp == size
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                border = if (selected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSubtitleFontSizeChanged(size) }
                            ) {
                                Text(
                                    text = "${size}sp",
                                    style = ApturaTypography.labelSmall,
                                    color = if (selected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Text Color Options
                    Text(
                        text = "Color",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextMediumEmphasis,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val colorChoices = listOf(
                            "White" to "#FFFFFF",
                            "Yellow" to "#FFEB3B",
                            "Cyan" to "#00E5FF",
                            "Green" to "#00E676"
                        )
                        colorChoices.forEach { (label, hex) ->
                            val selected = uiState.subtitleTextColorHex.equals(hex, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                border = if (selected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSubtitleTextColorChanged(hex) }
                            ) {
                                Text(
                                    text = label,
                                    style = ApturaTypography.labelSmall,
                                    color = if (selected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Background Box vs Outline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Background Box",
                                style = ApturaTypography.bodyMedium,
                                color = ApturaColors.TextHighEmphasis
                            )
                            Text(
                                text = if (uiState.subtitleBackgroundEnabled) "Semi-transparent black box" else "Contrasting outline shadow",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextLowEmphasis
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = uiState.subtitleBackgroundEnabled,
                            onCheckedChange = { onSubtitleBackgroundChanged(it) },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = ApturaColors.SurfacePureBlack,
                                checkedTrackColor = ApturaColors.AccentPrimary,
                                uncheckedThumbColor = ApturaColors.TextLowEmphasis,
                                uncheckedTrackColor = ApturaColors.SurfaceElevated3
                            )
                        )
                    }
                }
            }
        }

        // 10. Playback & Display Settings Sheet (SCR-010e)
        if (uiState.isAdvancedSheetVisible) {
            var selectedTab by remember { mutableIntStateOf(0) }

            ModalBottomSheet(
                onDismissRequest = { onAdvancedSheetVisible(false) },
                sheetState = rememberModalBottomSheetState(),
                containerColor = ApturaColors.SurfaceElevated2
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 36.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Playback & Display Settings",
                        style = ApturaTypography.titleMedium,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Segmented Tab Row: [ Playback Tools | Picture & Display ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ApturaColors.SurfaceElevated3, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 0) ApturaColors.AccentPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 0 }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Playback Tools",
                                    style = ApturaTypography.labelMedium,
                                    color = if (selectedTab == 0) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 1) ApturaColors.AccentPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 1 }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AspectRatio,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Picture & Display",
                                    style = ApturaTypography.labelMedium,
                                    color = if (selectedTab == 1) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // TAB 0: PLAYBACK TOOLS

                        // 1. Playback Speed Selector
                        Text(
                            text = "PLAYBACK SPEED",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.AccentPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                val isSelected = uiState.playbackSpeed == speed
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                    border = if (isSelected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                    modifier = Modifier.clickable { onSpeedSelected(speed) }
                                ) {
                                    Text(
                                        text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                        style = ApturaTypography.labelSmall,
                                        color = if (isSelected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 2. Sleep Timer Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bedtime,
                                    contentDescription = null,
                                    tint = ApturaColors.AccentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "SLEEP TIMER",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.AccentPrimary
                                )
                            }
                            if (uiState.sleepTimerRemainingMs != null) {
                                Text(
                                    text = "Cancel (${(uiState.sleepTimerRemainingMs / 60000L).coerceAtLeast(1)}m remaining)",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.StatusError,
                                    modifier = Modifier.clickable { onCancelSleepTimer() }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 30, 45, 60).forEach { minutes ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ApturaColors.SurfaceElevated3,
                                    border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onStartSleepTimer(minutes) }
                                ) {
                                    Text(
                                        text = "${minutes}m",
                                        style = ApturaTypography.labelSmall,
                                        color = ApturaColors.TextHighEmphasis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. A-B Repeat Loop
                        Text(
                            text = "A-B REPEAT LOOP",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.AccentPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val ptA = uiState.abRepeat.pointA
                            val ptB = uiState.abRepeat.pointB

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (ptA != null) ApturaColors.AccentPrimary.copy(alpha = 0.2f) else ApturaColors.SurfaceElevated3,
                                border = BorderStroke(1.dp, if (ptA != null) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetAbPointA() }
                            ) {
                                Text(
                                    text = if (ptA != null) "Point A: ${MediaFormatters.formatDuration(ptA)}" else "Set Point A",
                                    style = ApturaTypography.labelSmall,
                                    color = if (ptA != null) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (ptB != null) ApturaColors.AccentPrimary.copy(alpha = 0.2f) else ApturaColors.SurfaceElevated3,
                                border = BorderStroke(1.dp, if (ptB != null) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetAbPointB() }
                            ) {
                                Text(
                                    text = if (ptB != null) "Point B: ${MediaFormatters.formatDuration(ptB)}" else "Set Point B",
                                    style = ApturaTypography.labelSmall,
                                    color = if (ptB != null) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }

                            if (ptA != null || ptB != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ApturaColors.StatusError.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, ApturaColors.StatusError),
                                    modifier = Modifier.clickable { onClearAbRepeat() }
                                ) {
                                    Text(
                                        text = "Clear",
                                        style = ApturaTypography.labelSmall,
                                        color = ApturaColors.StatusError,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4. Frame-by-Frame Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FRAME-BY-FRAME STEPPER (~42ms)",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.AccentPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ApturaColors.SurfaceElevated3,
                                    border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                    modifier = Modifier.clickable { onStepFrame(false) }
                                ) {
                                    Text(
                                        text = "◀ -1 Frame",
                                        style = ApturaTypography.labelSmall,
                                        color = ApturaColors.TextHighEmphasis,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ApturaColors.SurfaceElevated3,
                                    border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                    modifier = Modifier.clickable { onStepFrame(true) }
                                ) {
                                    Text(
                                        text = "+1 Frame ▶",
                                        style = ApturaTypography.labelSmall,
                                        color = ApturaColors.TextHighEmphasis,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 5. Timeline Bookmarks & Chapters (FR-CTRL-011)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = ApturaColors.AccentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "BOOKMARKS & CHAPTERS",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.AccentPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ApturaColors.AccentPrimary,
                                modifier = Modifier.clickable { onAddBookmark() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.BookmarkAdd,
                                        contentDescription = null,
                                        tint = ApturaColors.SurfacePureBlack,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Bookmark @ ${MediaFormatters.formatDuration(uiState.currentPositionMs)}",
                                        style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = ApturaColors.SurfacePureBlack,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (uiState.bookmarks.isEmpty()) {
                            Text(
                                text = "No bookmarks saved for this video yet. Tap above to add one at current timecode.",
                                style = ApturaTypography.bodySmall,
                                color = ApturaColors.TextLowEmphasis,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                uiState.bookmarks.forEach { bookmark ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ApturaColors.SurfaceElevated3)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    onScrubStart(bookmark.timestampMs)
                                                    onScrubEnd()
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = ApturaColors.SurfaceElevated1
                                            ) {
                                                Text(
                                                    text = MediaFormatters.formatDuration(bookmark.timestampMs),
                                                    style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                    color = ApturaColors.AccentPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = bookmark.note.ifBlank { "Bookmark at ${MediaFormatters.formatDuration(bookmark.timestampMs)}" },
                                                style = ApturaTypography.labelSmall,
                                                color = ApturaColors.TextHighEmphasis,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteBookmark(bookmark.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete Bookmark",
                                                tint = ApturaColors.StatusError.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    } else {
                        // TAB 1: PICTURE & DISPLAY

                        // 1. Aspect Ratio Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ASPECT RATIO",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.AccentPrimary
                            )
                            Text(
                                text = "Current: ${uiState.resizeMode.label}",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextMediumEmphasis
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AspectRatioMode.entries.forEach { mode ->
                                val isSelected = uiState.resizeMode == mode
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                    border = if (isSelected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!isSelected) {
                                                onResizeModeToggle()
                                            }
                                        }
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = ApturaTypography.labelSmall,
                                        color = if (isSelected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 2. Eye Comfort Night Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "EYE COMFORT NIGHT MODE",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.AccentPrimary
                                )
                                Text(
                                    text = "Warm amber blue-light attenuation",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.TextLowEmphasis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (uiState.pictureAdjustment.isNightModeEnabled) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                modifier = Modifier.clickable {
                                    onNightModeToggle(!uiState.pictureAdjustment.isNightModeEnabled)
                                }
                            ) {
                                Text(
                                    text = if (uiState.pictureAdjustment.isNightModeEnabled) "ON" else "OFF",
                                    style = ApturaTypography.labelSmall,
                                    color = if (uiState.pictureAdjustment.isNightModeEnabled) ApturaColors.SurfacePureBlack else ApturaColors.TextMediumEmphasis,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (uiState.pictureAdjustment.isNightModeEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Warmth Level",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.TextMediumEmphasis
                                )
                                Text(
                                    text = "${(uiState.pictureAdjustment.nightModeWarmth * 100).toInt()}%",
                                    style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = ApturaColors.AccentPrimary
                                )
                            }
                            Slider(
                                value = uiState.pictureAdjustment.nightModeWarmth,
                                onValueChange = onNightModeWarmthChanged,
                                valueRange = 0.1f..0.8f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ApturaColors.AccentPrimary,
                                    activeTrackColor = ApturaColors.AccentPrimary,
                                    inactiveTrackColor = ApturaColors.SurfaceElevated3
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. Picture Calibration (Contrast & Saturation)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PICTURE CALIBRATION",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.AccentPrimary
                            )
                            if (uiState.pictureAdjustment.contrast != 1.0f ||
                                uiState.pictureAdjustment.saturation != 1.0f ||
                                uiState.pictureAdjustment.isNightModeEnabled
                            ) {
                                Text(
                                    text = "Reset to Natural",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.AccentPrimary,
                                    modifier = Modifier.clickable { onResetPictureAdjustment() }
                                )
                            }
                        }

                        // Contrast Slider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contrast",
                                style = ApturaTypography.labelMedium,
                                color = ApturaColors.TextMediumEmphasis
                            )
                            Text(
                                text = "%.2fx".format(uiState.pictureAdjustment.contrast),
                                style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = ApturaColors.TextHighEmphasis
                            )
                        }
                        Slider(
                            value = uiState.pictureAdjustment.contrast,
                            onValueChange = onContrastChanged,
                            valueRange = 0.5f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = ApturaColors.AccentPrimary,
                                activeTrackColor = ApturaColors.AccentPrimary,
                                inactiveTrackColor = ApturaColors.SurfaceElevated3
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Saturation Slider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Saturation",
                                style = ApturaTypography.labelMedium,
                                color = ApturaColors.TextMediumEmphasis
                            )
                            Text(
                                text = "%.2fx".format(uiState.pictureAdjustment.saturation),
                                style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = ApturaColors.TextHighEmphasis
                            )
                        }
                        Slider(
                            value = uiState.pictureAdjustment.saturation,
                            onValueChange = onSaturationChanged,
                            valueRange = 0.0f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = ApturaColors.AccentPrimary,
                                activeTrackColor = ApturaColors.AccentPrimary,
                                inactiveTrackColor = ApturaColors.SurfaceElevated3
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4. Decoder Engine Strategy (FR-PLAY-003, Q13)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DECODER ENGINE STRATEGY",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.AccentPrimary
                            )
                            val currentDecoderLabel = when (uiState.decoderStrategy) {
                                1 -> "Hardware"
                                2 -> "Software"
                                else -> "Auto"
                            }
                            Text(
                                text = "Current: $currentDecoderLabel",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextMediumEmphasis
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0 to "Auto",
                                1 to "Hardware",
                                2 to "Software"
                            ).forEach { (strategy, label) ->
                                val isSelected = uiState.decoderStrategy == strategy
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                    border = if (isSelected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onDecoderStrategyChanged(strategy) }
                                ) {
                                    Text(
                                        text = label,
                                        style = ApturaTypography.labelSmall,
                                        color = if (isSelected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
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

/**
 * Up Next Auto-Advance Countdown Overlay (SCR-010g, FR-CTRL-017).
 * Displays a 10s countdown ring with play-now and cancellable controls.
 */
@Composable
private fun UpNextCountdownOverlay(
    nextTitle: String,
    countdownSeconds: Int,
    onPlayNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(24.dp)
            .widthIn(max = 380.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ApturaColors.SurfaceElevated1.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "UP NEXT",
                style = ApturaTypography.labelSmall,
                color = ApturaColors.AccentPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = nextTitle,
                style = ApturaTypography.headlineSmall,
                color = ApturaColors.TextHighEmphasis,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Countdown circular ring
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { countdownSeconds / 10f },
                    modifier = Modifier.fillMaxSize(),
                    color = ApturaColors.AccentPrimary,
                    trackColor = ApturaColors.SurfaceElevated2,
                    strokeWidth = 4.dp
                )
                Text(
                    text = "$countdownSeconds",
                    style = ApturaTypography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                    color = ApturaColors.TextHighEmphasis,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = ApturaColors.TextMediumEmphasis)
                }

                Button(
                    onClick = onPlayNext,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApturaColors.AccentPrimary,
                        contentColor = ApturaColors.SurfacePureBlack
                    )
                ) {
                    Text("Play Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

