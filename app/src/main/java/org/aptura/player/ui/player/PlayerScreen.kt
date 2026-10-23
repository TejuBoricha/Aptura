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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.core.media.SubtitleFileValidator
import org.aptura.player.core.media.SubtitleValidationResult
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import org.aptura.player.R
import android.view.TextureView
import android.view.LayoutInflater
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
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
    onSeekRelative: (Long) -> Unit = {},
    onSeekRelativeWithSide: (Long, SeekTapSide?) -> Unit = { delta, _ -> onSeekRelative(delta) },
    onHorizontalSwipeSeekStart: () -> Unit = {},
    onHorizontalSwipeSeek: (Long) -> Unit = {},
    onHorizontalSwipeSeekCommit: () -> Unit = {},
    onHorizontalSwipeSeekCancel: () -> Unit = {},
    onTriggerLockHint: () -> Unit = {},
    onTwoFingerTap: () -> Unit = {},
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
    onExternalSubtitleSelected: (android.net.Uri, String, String) -> Unit = { _, _, _ -> },
    onRemoveExternalSubtitle: () -> Unit = {},
    onSubtitleFontSizeChanged: (Int) -> Unit = {},
    onSubtitleTextColorChanged: (String) -> Unit = {},
    onSubtitleBackgroundChanged: (Boolean) -> Unit = {},
    onAddBookmark: () -> Unit = {},
    onDeleteBookmark: (Long) -> Unit = {},
    onDecoderStrategyChanged: (Int) -> Unit = {},
    onDismissCompatibilityNotice: () -> Unit = {},
    onSearchWebClick: () -> Unit = {},
    onLaunchWebSearch: (query: String, provider: Int) -> Unit = { _, _ -> },
    onDismissWebSearchDialog: () -> Unit = {},
    onAttachRecentSubtitle: (org.aptura.player.data.repository.DetectedSidecarSubtitle) -> Unit = {},
    onDismissRecentSubtitle: () -> Unit = {},
    onDismissAutoPairToast: () -> Unit = {},
    onQueueSheetVisible: (Boolean) -> Unit = {},
    onJumpToQueueItem: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onRemoveQueueItem: (Int) -> Unit = {},
    onClearQueue: () -> Unit = {},
    onReplayCurrentItem: () -> Unit = {},
    onToggleStatsOverlay: () -> Unit = {},
    onWhyCompatibilityClick: () -> Unit = {},
    onDismissWhyCompatibilitySheet: () -> Unit = {},
    onResetVideoPreferences: () -> Unit = {},
    onAudioOnlyChanged: (Boolean) -> Unit = {},
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
            when (val validation = SubtitleFileValidator.validate(context, uri)) {
                is SubtitleValidationResult.Valid -> {
                    val targetUri = validation.validatedUri ?: uri
                    onExternalSubtitleSelected(targetUri, validation.mimeType, validation.displayName)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    Toast.makeText(context, "Loaded subtitle: ${validation.displayName}", Toast.LENGTH_SHORT).show()
                }
                is SubtitleValidationResult.Invalid -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, validation.reason, Toast.LENGTH_LONG).show()
                }
            }
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

    // Keep screen awake while actively playing media (P-16, spec/10:92)
    DisposableEffect(activity, uiState.isPlaying) {
        val window = activity?.window
        if (uiState.isPlaying) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Swipe-to-adjust brightness: seeded from the real system brightness and always restored on exit.
    val windowBrightness = remember(activity) {
        WindowBrightness(activity?.window) { WindowBrightness.systemBrightnessFraction(context) }
    }
    DisposableEffect(windowBrightness) {
        onDispose { windowBrightness.restore() }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
    }

    androidx.compose.runtime.DisposableEffect(player) {
        onDispose {
            player?.clearVideoSurface()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ApturaColors.SurfacePureBlack)
    ) {
        val screenWidth = maxWidth
        // 1. Video Surface (Media3 PlayerView).
        // SurfaceView by default (HDR-capable, lowest power). While picture adjustments are active the view is swapped
        // for a TextureView, the only surface that can be colour-filtered per frame (audit P0-08).
        // Audio-only mode removes the video view entirely: no surface, no rendering.
        val useColorFilterSurface = uiState.pictureAdjustment.isModified
        if (!uiState.isAudioOnly) key(useColorFilterSurface) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
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
                    when (uiState.resizeMode) {
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

                    // Subtitle style: only touch the SubtitleView when something actually changed (this block runs on
                    // every position tick; re-creating the style 4x/s was pure waste).
                    val styleKey = "${uiState.subtitleTextColorHex}|${uiState.subtitleBackgroundEnabled}|${uiState.subtitleFontSizeSp}"
                    if (playerView.tag != styleKey) {
                        playerView.tag = styleKey
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
                            setBottomPaddingFraction(0.09f)
                        }
                    }

                    PictureFilter.apply(playerView.videoSurfaceView as? TextureView, uiState.pictureAdjustment)
                },
                onRelease = { playerView ->
                    playerView.player = null
                }
            )
        }

        // 2. Gesture Detector Layer
        var swipeDragAccumulator by remember { mutableFloatStateOf(0f) }
        val swipeSensitivityMsPerPixel = remember(uiState.durationMs) {
            (uiState.durationMs.toFloat() / 1500f).coerceIn(50f, 500f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.isLocked) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var twoFingerDetected = false
                        val startTime = System.currentTimeMillis()
                        var movedPastSlop = false
                        val touchSlop = viewConfiguration.touchSlop
                        val startPos = down.position

                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.size >= 2) {
                                twoFingerDetected = true
                            }
                            for (change in event.changes) {
                                if ((change.position - startPos).getDistance() > touchSlop * 2) {
                                    movedPastSlop = true
                                }
                            }
                            val allUp = event.changes.all { !it.pressed }
                            if (allUp) {
                                val elapsed = System.currentTimeMillis() - startTime
                                if (twoFingerDetected && !movedPastSlop && elapsed < 500L && !uiState.isLocked) {
                                    event.changes.forEach { it.consume() }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTwoFingerTap()
                                }
                                break
                            }
                        }
                    }
                }
                .pointerInput(uiState.isLocked, uiState.durationMs) {
                    if (!uiState.isLocked && uiState.durationMs > 0L) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                swipeDragAccumulator = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onHorizontalSwipeSeekStart()
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                swipeDragAccumulator += dragAmount
                                val deltaMs = (swipeDragAccumulator * swipeSensitivityMsPerPixel).toLong()
                                onHorizontalSwipeSeek(deltaMs)
                            },
                            onDragEnd = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onHorizontalSwipeSeekCommit()
                            },
                            onDragCancel = {
                                onHorizontalSwipeSeekCancel()
                            }
                        )
                    }
                }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left half: Brightness & Rewind
                var leftDragAccumulator by remember { mutableFloatStateOf(0f) }
                var leftBoxHeightPx by remember { mutableFloatStateOf(1000f) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onSizeChanged { leftBoxHeightPx = it.height.toFloat().coerceAtLeast(200f) }
                        .pointerInput(uiState.isLocked, uiState.isPlaying, uiState.seekDeltaSeconds, uiState.doubleTapSeekSide, uiState.seekStepMs) {
                            detectTapGestures(
                                onTap = {
                                    if (uiState.isLocked) {
                                        onTriggerLockHint()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (uiState.seekDeltaSeconds != null && uiState.doubleTapSeekSide == SeekTapSide.LEFT) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSeekRelativeWithSide(-uiState.seekStepMs, SeekTapSide.LEFT)
                                    } else {
                                        onControlsVisibilityToggle()
                                    }
                                },
                                onDoubleTap = {
                                    if (!uiState.isLocked) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSeekRelativeWithSide(-uiState.seekStepMs, SeekTapSide.LEFT)
                                    } else {
                                        onTriggerLockHint()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                        val oldPercent = (windowBrightness.current() * 100).toInt()
                                        val newBrightness = windowBrightness.adjustBy(-dragAmount / leftBoxHeightPx)
                                        if (newBrightness != null) {
                                            val newPercent = (newBrightness * 100).toInt()
                                            if (newPercent / 10 != oldPercent / 10 || newPercent == 100 || newPercent == 1) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            onBrightnessPercentChanged(newPercent)
                                        }
                                    }
                                )
                            }
                        }
                )

                // Right half: Volume & Fast-forward
                var rightDragAccumulator by remember { mutableFloatStateOf(0f) }
                var rightBoxHeightPx by remember { mutableFloatStateOf(1000f) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onSizeChanged { rightBoxHeightPx = it.height.toFloat().coerceAtLeast(200f) }
                        .pointerInput(uiState.isLocked, uiState.isPlaying, uiState.seekDeltaSeconds, uiState.doubleTapSeekSide, uiState.seekStepMs) {
                            detectTapGestures(
                                onTap = {
                                    if (uiState.isLocked) {
                                        onTriggerLockHint()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (uiState.seekDeltaSeconds != null && uiState.doubleTapSeekSide == SeekTapSide.RIGHT) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSeekRelativeWithSide(uiState.seekStepMs, SeekTapSide.RIGHT)
                                    } else {
                                        onControlsVisibilityToggle()
                                    }
                                },
                                onDoubleTap = {
                                    if (!uiState.isLocked) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSeekRelativeWithSide(uiState.seekStepMs, SeekTapSide.RIGHT)
                                    } else {
                                        onTriggerLockHint()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                            val stepThreshold = (rightBoxHeightPx / maxVolume.toFloat()).coerceIn(20f, 60f)
                                            if (rightDragAccumulator > stepThreshold) {
                                                val newVol = (currentVol + 1).coerceIn(0, maxVolume)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                rightDragAccumulator = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onVolumePercentChanged((newVol * 100 / maxVolume))
                                            } else if (rightDragAccumulator < -stepThreshold) {
                                                val newVol = (currentVol - 1).coerceIn(0, maxVolume)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                rightDragAccumulator = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onVolumePercentChanged((newVol * 100 / maxVolume))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                )
            }
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

        // Momentary Orientation Feedback Toast (SCR-010e) - TopCenter chip avoids central transport control collisions
        AnimatedVisibility(
            visible = orientationToastMessage != null && !isPipMode,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    top = when {
                        uiState.isCompatibilityMode && uiState.compatibilityNotice != null -> 116.dp
                        uiState.isControlsVisible -> 68.dp
                        else -> 24.dp
                    }
                )
        ) {
            Surface(
                color = ApturaColors.SurfaceElevated2.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ScreenRotation,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = orientationToastMessage.orEmpty(),
                        style = ApturaTypography.labelLarge,
                        color = ApturaColors.TextHighEmphasis
                    )
                }
            }
        }

        // Auto-Paired Subtitle Feedback Toast
        LaunchedEffect(uiState.autoPairedSubtitleToast) {
            if (uiState.autoPairedSubtitleToast != null) {
                kotlinx.coroutines.delay(4000L)
                onDismissAutoPairToast()
            }
        }

        AnimatedVisibility(
            visible = uiState.autoPairedSubtitleToast != null && !isPipMode,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    top = when {
                        uiState.isCompatibilityMode && uiState.compatibilityNotice != null -> 160.dp
                        orientationToastMessage != null -> 116.dp
                        uiState.isControlsVisible -> 68.dp
                        else -> 24.dp
                    }
                )
        ) {
            Surface(
                color = ApturaColors.SurfaceElevated2.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.5f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = uiState.autoPairedSubtitleToast.orEmpty(),
                        style = ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = ApturaColors.TextHighEmphasis
                    )
                    IconButton(
                        onClick = onDismissAutoPairToast,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss auto-pair notification",
                            tint = ApturaColors.TextLowEmphasis,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2b. Audio-only panel (the video view is not composed in this mode). It must sit ABOVE the gesture layer:
        // the full-screen gesture detector otherwise swallows the touch and "Show video" can never be pressed.
        if (uiState.isAudioOnly && !isPipMode) {
            AudioOnlyPanel(
                title = uiState.videoTitle,
                onShowVideo = { onAudioOnlyChanged(false) },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 3. Transient Feedback Overlays (Volume, Brightness, Double-Tap Seek, Horizontal Swipe Seek, Lock Hint)
        // Right-side Volume Indicator (FR-CTRL-005, FR-CTRL-015)
        AnimatedVisibility(
            visible = uiState.volumePercent != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
        ) {
            uiState.volumePercent?.let { vol ->
                VerticalLevelGaugeHud(
                    valuePercent = vol,
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Volume"
                )
            }
        }

        // Left-side Brightness Indicator (FR-CTRL-005, FR-CTRL-015)
        AnimatedVisibility(
            visible = uiState.brightnessPercent != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp)
        ) {
            uiState.brightnessPercent?.let { bri ->
                VerticalLevelGaugeHud(
                    valuePercent = bri,
                    icon = Icons.Filled.BrightnessMedium,
                    label = "Brightness"
                )
            }
        }

        // Double-Tap Seek Curved Ripple Overlay (FR-CTRL-002)
        AnimatedVisibility(
            visible = uiState.seekDeltaSeconds != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(
                if (uiState.doubleTapSeekSide == SeekTapSide.LEFT || (uiState.seekDeltaSeconds ?: 0) < 0) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            )
        ) {
            val delta = uiState.seekDeltaSeconds ?: 0
            val side = uiState.doubleTapSeekSide ?: if (delta < 0) SeekTapSide.LEFT else SeekTapSide.RIGHT
            DoubleTapSeekRippleOverlay(
                seekDeltaSeconds = delta,
                side = side
            )
        }

        // Horizontal Swipe Seek HUD (FR-CTRL-005, FR-CTRL-003)
        AnimatedVisibility(
            visible = uiState.swipeSeekTargetMs != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val targetMs = uiState.swipeSeekTargetMs ?: 0L
            val deltaMs = uiState.swipeSeekDeltaMs ?: 0L
            HorizontalSwipeSeekHud(
                targetMs = targetMs,
                durationMs = uiState.durationMs,
                deltaMs = deltaMs
            )
        }

        // Lock Mode Touch Guard Hint (FR-CTRL-006)
        AnimatedVisibility(
            visible = uiState.isLockHintVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
        ) {
            LockModeGuardHint()
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
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
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
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val isExpandedWindow = configuration.screenWidthDp >= 600

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isExpandedWindow) {
                                    orientationToastMessage = "Orientation managed by system on large screens"
                                } else {
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
                                tint = if (currentOrientationMode != 0 && !isExpandedWindow) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis
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

                        // Aspect ratio button
                        IconButton(
                            onClick = onResizeModeToggle,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = if (uiState.resizeMode != AspectRatioMode.FIT) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Speed selector button
                        Surface(
                            onClick = { onSpeedSheetVisible(true) },
                            shape = RoundedCornerShape(16.dp),
                            color = ApturaColors.SurfaceScrim.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3.copy(alpha = 0.6f)),
                            modifier = Modifier.padding(horizontal = 2.dp)
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

                        // Queue button (SCR-010f, FR-CTRL-001) - prominent chip when queue has multiple items
                        if (uiState.queueItems.size > 1) {
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onQueueSheetVisible(true)
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = ApturaColors.AccentSubtle,
                                border = BorderStroke(1.dp, ApturaColors.AccentPrimary),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = "Playback Queue",
                                        tint = ApturaColors.AccentPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${uiState.queueItems.size}",
                                        style = ApturaTypography.labelSmall,
                                        color = ApturaColors.AccentPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Transport Cluster (⏮ Prev • ⏪10s • ⏯ • ⏩10s • ⏭ Next)
                // Window-aware adaptive cluster sizing (R-B09, R-A10):
                // Protects gesture seek zones from being swallowed on narrow split windows (< 340dp).
                val isNarrowWindow = screenWidth < 340.dp
                val isCompactCluster = screenWidth < 480.dp
                val clusterSpacing = if (isNarrowWindow) 6.dp else if (isCompactCluster) 10.dp else 16.dp
                val playSize = if (isNarrowWindow) 52.dp else if (isCompactCluster) 62.dp else 72.dp
                val playIconSize = if (isNarrowWindow) 28.dp else if (isCompactCluster) 34.dp else 40.dp
                val seekSize = if (isNarrowWindow) 40.dp else if (isCompactCluster) 46.dp else 52.dp
                val seekIconSize = if (isNarrowWindow) 22.dp else if (isCompactCluster) 26.dp else 30.dp
                val skipSize = if (isNarrowWindow) 36.dp else if (isCompactCluster) 42.dp else 48.dp
                val skipIconSize = if (isNarrowWindow) 20.dp else if (isCompactCluster) 24.dp else 28.dp

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(clusterSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipPrevious()
                        },
                        enabled = uiState.hasPrevious,
                        modifier = Modifier
                            .size(skipSize)
                            .background(ApturaColors.SurfaceScrim.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous Video",
                            tint = if (uiState.hasPrevious) ApturaColors.TextHighEmphasis else ApturaColors.TextLowEmphasis.copy(alpha = 0.38f),
                            modifier = Modifier.size(skipIconSize)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSeekRelative(-uiState.seekStepMs)
                        },
                        modifier = Modifier
                            .size(seekSize)
                            .background(ApturaColors.SurfaceScrim.copy(alpha = 0.5f), CircleShape)
                    ) {
                        SeekStepIcon(
                            forward = false,
                            seconds = (uiState.seekStepMs / 1000L).toInt(),
                            iconSize = seekIconSize
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPlayPauseClick()
                        },
                        modifier = Modifier
                            .size(playSize)
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
                            modifier = Modifier.size(playIconSize)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSeekRelative(uiState.seekStepMs)
                        },
                        modifier = Modifier
                            .size(seekSize)
                            .background(ApturaColors.SurfaceScrim.copy(alpha = 0.5f), CircleShape)
                    ) {
                        SeekStepIcon(
                            forward = true,
                            seconds = (uiState.seekStepMs / 1000L).toInt(),
                            iconSize = seekIconSize
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipNext()
                        },
                        enabled = uiState.hasNext,
                        modifier = Modifier
                            .size(skipSize)
                            .background(ApturaColors.SurfaceScrim.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Video",
                            tint = if (uiState.hasNext) ApturaColors.TextHighEmphasis else ApturaColors.TextLowEmphasis.copy(alpha = 0.38f),
                            modifier = Modifier.size(skipIconSize)
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
                                maxLines = 1,
                                softWrap = false,
                                color = ApturaColors.TextHighEmphasis
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Custom Pixel-Perfect Video Scrubber (Concentric cy = size.height / 2f)
                            var scrubberWidthPx by remember { mutableFloatStateOf(0f) }
                            val trackHeight = 6.dp
                            val thumbRadius = if (uiState.isScrubbing) 12.dp else 10.dp

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
                                maxLines = 1,
                                softWrap = false,
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
                                        contentDescription = "Play mode: " + when (uiState.playMode) {
                                            PlayMode.NORMAL -> "play in order"
                                            PlayMode.REPEAT_ONE -> "repeat this video"
                                            PlayMode.REPEAT_ALL -> "repeat all"
                                            PlayMode.SHUFFLE -> "shuffle"
                                        },
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

        // Compatibility Mode Notice Chip (SCR-010h)
        if (uiState.isCompatibilityMode && uiState.compatibilityNotice != null && !isPipMode) {
            Surface(
                color = ApturaColors.SurfaceElevated1.copy(alpha = 0.94f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 64.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.compatibilityNotice.orEmpty(),
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextHighEmphasis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // "Why?" Explainer button per SCR-010h and FR-PLAY-003
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ApturaColors.AccentPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.clickable { onWhyCompatibilityClick() }
                    ) {
                        Text(
                            text = "Why?",
                            style = ApturaTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ApturaColors.AccentPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDismissCompatibilityNotice,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = ApturaColors.TextLowEmphasis,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 5b. Playback Stats Studio HUD Overlay (SCR-010e, FR-DIAG-001)
        if (uiState.isStatsOverlayVisible && !isPipMode) {
            StatsOverlay(
                stats = uiState.playbackStats,
                onCloseClick = onToggleStatsOverlay,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 64.dp, end = 16.dp)
            )
        }

        // 6. Playback Error State Banner (SCR-010x)
        if (uiState.errorMessage != null) {
            Surface(
                color = ApturaColors.SurfaceElevated2,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ApturaColors.StatusError.copy(alpha = 0.8f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 420.dp)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(ApturaColors.StatusError.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = ApturaColors.StatusError,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Playback Error",
                        style = ApturaTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ApturaColors.TextHighEmphasis
                    )
                    uiState.errorTaxonomyCode?.let { code ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = ApturaColors.SurfaceElevated1,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, ApturaColors.StatusWarning.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = code,
                                style = ApturaTypography.labelSmall.copy(
                                    fontFeatureSettings = "tnum",
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = ApturaColors.StatusWarning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    val isSoftwareAlreadyAttempted = uiState.isCompatibilityMode || uiState.decoderStrategy == 2
                    val failureExplanation = if (isSoftwareAlreadyAttempted && uiState.errorTaxonomyCode == "UNSUPPORTED_PROFILE") {
                        "Neither hardware nor platform software decoders could play this 10-bit video format on this device. Open with an external player."
                    } else {
                        uiState.errorMessage.orEmpty()
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = failureExplanation,
                        style = ApturaTypography.bodyMedium,
                        color = ApturaColors.TextMediumEmphasis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uiState.canSwitchToSoftware && !isSoftwareAlreadyAttempted) {
                            Button(
                                onClick = {
                                    onDecoderStrategyChanged(2)
                                    onRetryPlayback()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ApturaColors.AccentPrimary,
                                    contentColor = ApturaColors.TextOnAccent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Switch to Software Decoder", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (isSoftwareAlreadyAttempted && uiState.videoUri.isNotBlank()) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(uiState.videoUri), "video/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open Video With"))
                                    } catch (_: Throwable) {
                                        Toast.makeText(context, "No external video player found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ApturaColors.AccentPrimary,
                                    contentColor = ApturaColors.TextOnAccent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open in External Player", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (uiState.isRecoverableError) {
                            OutlinedButton(
                                onClick = onRetryPlayback,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ApturaColors.TextHighEmphasis
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (!isSoftwareAlreadyAttempted && uiState.videoUri.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(uiState.videoUri), "video/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open Video With"))
                                    } catch (_: Throwable) {
                                        Toast.makeText(context, "No external video player found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ApturaColors.TextHighEmphasis
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open in External Player")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val diagText = ApturaDiagnostics.generateDiagnosticMarkdown(
                                        context = context,
                                        settingsSnapshot = mapOf(
                                            "decoderStrategy" to uiState.decoderStrategy.toString(),
                                            "isCompatibilityMode" to uiState.isCompatibilityMode.toString(),
                                            "errorCode" to (uiState.errorCode ?: "none"),
                                            "videoUri" to uiState.videoUri.take(12)
                                        )
                                    )
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Aptura Diagnostics", diagText)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ApturaColors.TextMediumEmphasis
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Diagnostics", style = ApturaTypography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = onBackClick,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ApturaColors.TextMediumEmphasis
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Go Back", style = ApturaTypography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // 7. Up Next Auto-Advance Countdown Overlay (SCR-010g)
        if (uiState.isEnded && uiState.autoAdvanceCountdownSeconds != null) {
            UpNextOverlay(
                countdownSeconds = uiState.autoAdvanceCountdownSeconds,
                nextTitle = uiState.nextItemTitle ?: "Next Video",
                nextDurationMs = uiState.nextItemDurationMs,
                onPlayNow = onPlayNextImmediately,
                onReplay = onReplayCurrentItem,
                onCancel = onCancelAutoAdvance
            )
        }

        // 7b. Playback Queue Sheet (SCR-010f, FR-CTRL-001, FR-CTRL-017)
        if (uiState.isQueueSheetVisible) {
            PlaybackQueueSheet(
                queueItems = uiState.queueItems,
                currentIndex = uiState.currentQueueIndex,
                playMode = uiState.playMode,
                onDismiss = { onQueueSheetVisible(false) },
                onJumpToItem = onJumpToQueueItem,
                onMoveItem = onMoveQueueItem,
                onRemoveItem = onRemoveQueueItem,
                onClearQueue = onClearQueue,
                onTogglePlayMode = onPlayModeToggle
            )
        }

        // 7c. Speed Selection Sheet (SCR-010b)
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

        // 8. Audio & Sound Enhancements Sheet (SCR-010d, FR-AUD-001..004)
        if (uiState.isAudioSheetVisible) {
            AudioEnhancementSheet(
                audioTracks = uiState.audioTracks,
                selectedAudioTrackIndex = uiState.selectedAudioTrackIndex,
                audioDelayMs = uiState.audioDelayMs,
                delayStepMs = uiState.delayStepMs,
                audioEnhancement = uiState.audioEnhancement,
                onDismissRequest = { onAudioSheetVisible(false) },
                onAudioTrackSelected = onAudioTrackSelected,
                onAudioDelayChanged = onAudioDelayChanged,
                onEqualizerEnabledChanged = onEqualizerEnabledChanged,
                onEqualizerPresetSelected = onEqualizerPresetSelected,
                onEqualizerBandChanged = onEqualizerBandChanged,
                onBassBoostChanged = onBassBoostChanged,
                onVirtualizerChanged = onVirtualizerChanged,
                onLoudnessBoostChanged = onLoudnessBoostChanged
            )
        }

        // 9. Subtitle Track & Sync Sheet (SCR-010c)
        if (uiState.isSubtitleSheetVisible) {
            SubtitleSheet(
                uiState = uiState,
                onDismissRequest = { onSubtitleSheetVisible(false) },
                onSubtitleTrackSelected = onSubtitleTrackSelected,
                onSubtitleDelayChanged = onSubtitleDelayChanged,
                onImportSubtitleClick = {
                    subtitlePickerLauncher.launch(
                        arrayOf(
                            "application/x-subrip",
                            "text/vtt",
                            "text/x-ssa",
                            "text/plain",
                            "application/zip"
                        )
                    )
                },
                onRemoveExternalSubtitle = onRemoveExternalSubtitle,
                onSubtitleFontSizeChanged = onSubtitleFontSizeChanged,
                onSubtitleTextColorChanged = onSubtitleTextColorChanged,
                onSubtitleBackgroundChanged = onSubtitleBackgroundChanged,
                onSearchWebClick = onSearchWebClick,
                onAttachRecentSubtitle = onAttachRecentSubtitle,
                onDismissRecentSubtitle = onDismissRecentSubtitle
            )
        }

        // 9b. Subtitle Web Search Assistant Dialog
        if (uiState.isWebSearchDialogVisible) {
            WebSearchDialog(
                initialQuery = uiState.cleanSearchQuery.ifBlank { uiState.videoTitle },
                onDismissRequest = onDismissWebSearchDialog,
                onLaunchSearch = onLaunchWebSearch
            )
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
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 24.dp)
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

                        // 0. Audio-only mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AUDIO ONLY",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.AccentPrimary
                                )
                                Text(
                                    text = "Turn the video off and keep listening",
                                    style = ApturaTypography.bodyMedium,
                                    color = ApturaColors.TextMediumEmphasis
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = uiState.isAudioOnly,
                                onCheckedChange = onAudioOnlyChanged,
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedTrackColor = ApturaColors.AccentPrimary,
                                    checkedThumbColor = ApturaColors.SurfacePureBlack
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 1. Playback Speed Selector (2 balanced rows, zero truncation)
                        Text(
                            text = "PLAYBACK SPEED",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.AccentPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0.5f, 0.75f, 1.0f).forEach { speed ->
                                    val isSelected = uiState.playbackSpeed == speed
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                        border = if (isSelected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSpeedSelected(speed) }
                                    ) {
                                        Text(
                                            text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                            style = ApturaTypography.labelSmall,
                                            color = if (isSelected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1.25f, 1.5f, 2.0f).forEach { speed ->
                                    val isSelected = uiState.playbackSpeed == speed
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                        border = if (isSelected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSpeedSelected(speed) }
                                    ) {
                                        Text(
                                            text = "${speed}x",
                                            style = ApturaTypography.labelSmall,
                                            color = if (isSelected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
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
                                    text = "Cancel (${MediaFormatters.formatDuration(uiState.sleepTimerRemainingMs)} left)",
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
                            val isOff = uiState.sleepTimerRemainingMs == null
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isOff) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                border = if (isOff) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onCancelSleepTimer() }
                            ) {
                                Text(
                                    text = "Off",
                                    style = ApturaTypography.labelSmall,
                                    color = if (isOff) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
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

                        // 4. Frame-by-Frame Stepper (Unwrapped buttons with full tactile width)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "FRAME-BY-FRAME STEPPER",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.AccentPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ApturaColors.SurfaceElevated3,
                                    border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onStepFrame(false) }
                                ) {
                                    Text(
                                        text = "◀ -1 Frame",
                                        style = ApturaTypography.labelSmall,
                                        color = ApturaColors.TextHighEmphasis,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ApturaColors.SurfaceElevated3,
                                    border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onStepFrame(true) }
                                ) {
                                    Text(
                                        text = "+1 Frame ▶",
                                        style = ApturaTypography.labelSmall,
                                        color = ApturaColors.TextHighEmphasis,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(vertical = 10.dp)
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // 6. Playback Stats Studio Toggle (SCR-010e, FR-DIAG-001)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PLAYBACK STATS OVERLAY",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.AccentPrimary
                                )
                                Text(
                                    text = "Live decode path, resolution, FPS, bitrate & frame drops",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.TextLowEmphasis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (uiState.isStatsOverlayVisible) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                                modifier = Modifier.clickable { onToggleStatsOverlay() }
                            ) {
                                Text(
                                    text = if (uiState.isStatsOverlayVisible) "ON" else "OFF",
                                    style = ApturaTypography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isStatsOverlayVisible) ApturaColors.SurfacePureBlack else ApturaColors.TextMediumEmphasis,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 7. Reset Video Preferences (FR-CTRL-012)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ApturaColors.SurfaceElevated3,
                            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onResetVideoPreferences()
                                    android.widget.Toast.makeText(context, "Video preferences reset to defaults", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Replay,
                                    contentDescription = null,
                                    tint = ApturaColors.AccentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reset This Video's Settings",
                                    style = ApturaTypography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ApturaColors.AccentPrimary
                                )
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(0, "Auto", "Picks the best decoder automatically (recommended)"),
                                Triple(1, "Hardware", "Faster and easier on the battery; keep this if playback is smooth"),
                                Triple(2, "Software", "Most compatible; may stutter or drain battery on large 4K files")
                            ).forEach { (strategy, title, description) ->
                                val isSelected = uiState.decoderStrategy == strategy
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) ApturaColors.AccentPrimary.copy(alpha = 0.12f) else ApturaColors.SurfaceElevated3,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onDecoderStrategyChanged(strategy) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                style = ApturaTypography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = description,
                                                style = ApturaTypography.labelSmall,
                                                color = ApturaColors.TextMediumEmphasis
                                            )
                                        }
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = ApturaColors.AccentPrimary,
                                                modifier = Modifier.size(18.dp)
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

        // 11. Compatibility Explainer Sheet (SCR-010h, FR-PLAY-003)
        if (uiState.isWhyCompatibilitySheetVisible) {
            CompatibilityExplainerSheet(
                failedCodecName = uiState.failedCodecName,
                failureReason = uiState.failureReason,
                activeDecoderPath = uiState.playbackStats.decoderPath,
                currentStrategy = uiState.decoderStrategy,
                onStrategySelected = onDecoderStrategyChanged,
                onDismissRequest = { onDismissWhyCompatibilitySheet() }
            )
        }
    }
}

