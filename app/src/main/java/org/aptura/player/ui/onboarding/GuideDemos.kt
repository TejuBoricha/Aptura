package org.aptura.player.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import org.aptura.player.core.playback.PlaybackSpeedOptions
import org.aptura.player.domain.model.LibraryLayout
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.SpeedRail
import org.aptura.player.ui.library.FolderStrip
import org.aptura.player.ui.library.LibraryHeader
import org.aptura.player.ui.library.VideoCardCallbacks
import org.aptura.player.ui.library.VideoGridCard
import org.aptura.player.ui.library.VideoListRow
import org.aptura.player.ui.library.folderLabels
import org.aptura.player.ui.player.DoubleTapSeekRippleOverlay
import org.aptura.player.ui.player.HorizontalSwipeSeekHud
import org.aptura.player.ui.player.SeekTapSide
import org.aptura.player.ui.player.VerticalLevelGaugeHud
import org.aptura.player.ui.settings.SegmentedPicker

/*
 * The guide's demonstrations. Each one is built from the app's own composables, so the guide cannot describe a screen
 * the app does not have: the library page shows the real header, folder bar and cards; the player page shows the real
 * brightness, volume and seek indicators; the tools page carries the real speed control.
 */

internal const val GUIDE_LIBRARY_DEMO_TAG = "guide_library_demo"
internal const val GUIDE_PLAYER_DEMO_TAG = "guide_player_demo"

// ---- the library -----------------------------------------------------------------------------------------------

private val DemoCallbacks = VideoCardCallbacks(onClick = {}, onMore = {})

/**
 * A picture of the library screen, with the person's choice of layout to try. It is a picture: the real search field,
 * folder bar and cards, laid out as on a phone, but touches stop at the frame so nothing in it opens a keyboard or a
 * video. The layout buttons below it are live.
 */
@Composable
internal fun LibraryDemo(modifier: Modifier = Modifier, library: GuideLibrary = SampleLibrary) {
    var layout by remember { mutableStateOf(LibraryLayout.GRID) }
    val motion = LocalGuideMotion.current
    val labels = remember(library) { folderLabels(library.folders) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SegmentedPicker(
            options = listOf("Grid", "List"),
            selectedIndex = if (layout == LibraryLayout.GRID) 0 else 1,
            onSelect = { layout = if (it == 0) LibraryLayout.GRID else LibraryLayout.LIST },
            label = "Try a layout",
            modifier = Modifier.widthIn(max = 400.dp).align(Alignment.CenterHorizontally)
        )

        Box(
            modifier = Modifier
                .testTag(GUIDE_LIBRARY_DEMO_TAG)
                .clearAndSetSemantics {
                    contentDescription = "A picture of the library: a search bar, a folder bar, and your videos as " +
                        (if (layout == LibraryLayout.GRID) "a grid" else "a list") +
                        ", one of them with a line showing how far you watched."
                }
        ) {
            GuideScreenFrame(designWidth = 400.dp) {
                Box {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        LibraryHeader(
                            query = "",
                            scopeName = null,
                            isRefreshing = false,
                            showRefreshAndSettings = true,
                            pagePadding = 16.dp,
                            onQueryChange = {},
                            onSearchDone = {},
                            onViewOptions = {},
                            onRefresh = {},
                            onSettingsClick = {}
                        )
                        FolderStrip(
                            folders = library.folders,
                            labels = labels,
                            counts = library.folderCounts,
                            totalCount = library.totalCount,
                            selectedFolder = null,
                            onFolderSelected = {},
                            onBrowseFolders = {},
                            onAddFolder = {}
                        )
                        AnimatedContent(
                            targetState = layout,
                            transitionSpec = { layoutSwitch(motion) },
                            label = "guide-library-layout"
                        ) { shown ->
                            when (shown) {
                                LibraryLayout.GRID -> DemoGrid(library.videos)
                                LibraryLayout.LIST -> DemoList(library.videos)
                            }
                        }
                    }
                    // Touches stop here; drags pass through, so the page still turns when it is swiped across the picture.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(Unit) { detectTapGestures { } }
                    )
                }
            }
        }

    }
}

private fun layoutSwitch(motion: Boolean): ContentTransform {
    val enter = if (motion) 220 else 0
    val exit = if (motion) 120 else 0
    return ContentTransform(
        targetContentEnter = fadeIn(tween(enter, delayMillis = exit)),
        initialContentExit = fadeOut(tween(exit)),
        sizeTransform = SizeTransform(clip = true) { _, _ -> tween(enter) }
    )
}

@Composable
private fun DemoGrid(videos: List<GuideSample>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        videos.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { sample ->
                    VideoGridCard(
                        card = sample.card,
                        callbacks = DemoCallbacks,
                        modifier = Modifier.weight(1f),
                        thumbnail = sample.still?.let { seed -> { GuideStill(seed = seed) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoList(videos: List<GuideSample>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        videos.forEach { sample ->
            VideoListRow(
                card = sample.card,
                callbacks = DemoCallbacks,
                thumbnail = sample.still?.let { seed -> { GuideStill(seed = seed) } }
            )
        }
    }
}

// ---- the player ------------------------------------------------------------------------------------------------

/** What the player demonstration is showing, in the order it plays them. */
internal enum class PlayerDemoStep(val caption: String) {
    BRIGHTNESS("Slide up or down on the left half to change brightness"),
    VOLUME("Slide up or down on the right half to change volume"),
    DOUBLE_TAP("Double-tap either side to jump 10 seconds"),
    SCRUB("Swipe left or right to scrub through the video")
}

private const val STATIC_CAPTION = "Left half: brightness. Right half: volume. Double-tap to jump, swipe to scrub."
private const val STEP_MS = 2100
private const val STEP_GAP_MS = 450

/**
 * The player, with its two touch zones, and a finger showing what each gesture does. The brightness, volume and seek
 * indicators are the player's own. It plays through four gestures while its page is in view and rests on one frame when
 * motion is off.
 */
@Composable
internal fun PlayerDemo(modifier: Modifier = Modifier) {
    val motion = LocalGuideMotion.current
    val pageProgress = LocalGuidePageProgress.current
    var step by remember { mutableStateOf(PlayerDemoStep.BRIGHTNESS) }
    val t = remember { Animatable(0.75f) }
    var inView by remember { mutableStateOf(false) }

    LaunchedEffect(motion) {
        if (motion) snapshotFlow { pageProgress() >= 0.95f }.collect { inView = it }
    }
    LaunchedEffect(motion, inView) {
        if (!motion || !inView) return@LaunchedEffect
        while (true) {
            for (next in PlayerDemoStep.entries) {
                step = next
                t.snapTo(0f)
                // The gesture takes STEP_MS; the picture then holds on its result for STEP_GAP_MS before the next.
                t.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = STEP_MS + STEP_GAP_MS
                        0f at 0 using FastOutSlowInEasing
                        1f at STEP_MS
                    }
                )
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .testTag(GUIDE_PLAYER_DEMO_TAG)
                .clearAndSetSemantics {
                    contentDescription = "The player screen has two halves. Swipe up or down on the left half to change brightness " +
                        "and on the right half to change volume. Double-tap the left to rewind and the right to fast-forward. " +
                        "Swipe left or right anywhere to scrub."
                }
        ) {
            GuideDarkScene {
                GuideScreenFrame(designWidth = 480.dp, background = Color.Black) {
                    PlayerScene(step = step, progress = { t.value }, resting = !motion)
                }
            }
        }

        AnimatedContent(
            targetState = if (motion) step.caption else STATIC_CAPTION,
            transitionSpec = { fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) },
            label = "guide-player-caption",
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }
        ) { caption ->
            Text(
                text = caption,
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().height(40.dp)
            )
        }
    }
}

@Composable
private fun PlayerScene(step: PlayerDemoStep, progress: () -> Float, resting: Boolean) {
    val seam = ApturaColors.BorderSubtle
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .drawBehind {
                drawLine(
                    color = seam,
                    start = Offset(size.width / 2, 20.dp.toPx()),
                    end = Offset(size.width / 2, size.height - 52.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

        GuideStill(seed = 0)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.34f)))

        Row(modifier = Modifier.fillMaxSize().padding(top = 22.dp, bottom = 48.dp)) {
            ZoneLabel(
                icon = Icons.Default.BrightnessMedium,
                name = "Brightness",
                dimmed = !resting && step == PlayerDemoStep.BRIGHTNESS,
                modifier = Modifier.weight(1f)
            )
            ZoneLabel(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                name = "Volume",
                dimmed = !resting && step == PlayerDemoStep.VOLUME,
                modifier = Modifier.weight(1f)
            )
        }

        // The scrubber along the bottom: a swipe anywhere moves along it.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ApturaColors.AccentPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f))
            ) {
                val scrubbed = if (!resting && step == PlayerDemoStep.SCRUB) 0.30f + 0.12f * progress() else 0.30f
                Box(Modifier.fillMaxWidth(scrubbed).fillMaxHeight().background(ApturaColors.AccentPrimary))
            }
            Spacer(Modifier.width(10.dp))
            Text("swipe to scrub", style = ApturaTypography.labelSmall, color = Color.White.copy(alpha = 0.8f))
        }

        // What the current gesture does, drawn by the player's own indicators.
        val t = progress()
        if (resting) {
            // One still frame: both indicators at once, since there is no finger to say which half does what.
            VerticalLevelGaugeHud(
                valuePercent = 69,
                icon = Icons.Default.BrightnessMedium,
                label = "Brightness",
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp)
            )
            VerticalLevelGaugeHud(
                valuePercent = 55,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = "Volume",
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp)
            )
            return@BoxWithConstraints
        }
        when (step) {
            PlayerDemoStep.BRIGHTNESS -> {
                VerticalLevelGaugeHud(
                    valuePercent = lerp(35f, 80f, t).roundToInt(),
                    icon = Icons.Default.BrightnessMedium,
                    label = "Brightness",
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp)
                )
                Finger(Modifier.offset { IntOffset((widthPx * 0.27f).roundToInt(), (heightPx * lerp(0.84f, 0.46f, t)).roundToInt()) })
            }
            PlayerDemoStep.VOLUME -> {
                VerticalLevelGaugeHud(
                    valuePercent = lerp(40f, 75f, t).roundToInt(),
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Volume",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp)
                )
                Finger(Modifier.offset { IntOffset((widthPx * 0.70f).roundToInt(), (heightPx * lerp(0.84f, 0.46f, t)).roundToInt()) })
            }
            PlayerDemoStep.DOUBLE_TAP -> {
                val resultAlpha = ((t - 0.42f) / 0.14f).coerceIn(0f, 1f)
                DoubleTapSeekRippleOverlay(
                    seekDeltaSeconds = 10,
                    side = SeekTapSide.RIGHT,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 60.dp)
                        .graphicsLayer { alpha = resultAlpha }
                )
                TapRipple(at = 0.12f, now = t, x = 0.74f, y = 0.5f, widthPx = widthPx, heightPx = heightPx)
                TapRipple(at = 0.30f, now = t, x = 0.74f, y = 0.5f, widthPx = widthPx, heightPx = heightPx)
            }
            PlayerDemoStep.SCRUB -> {
                val delta = (t * 45_000f).toLong()
                HorizontalSwipeSeekHud(
                    targetMs = 125_000L + delta,
                    durationMs = 5_400_000L,
                    deltaMs = delta,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 26.dp)
                )
                Finger(Modifier.offset { IntOffset((widthPx * lerp(0.38f, 0.66f, t)).roundToInt(), (heightPx * 0.60f).roundToInt()) })
            }
        }
    }
}

@Composable
private fun ZoneLabel(icon: ImageVector, name: String, dimmed: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().graphicsLayer { alpha = if (dimmed) 0.22f else 1f },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.SwapVert, contentDescription = null, tint = ApturaColors.AccentPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(name, style = ApturaTypography.titleMedium, color = Color.White)
    }
}

/** A fingertip on the glass. */
@Composable
private fun Finger(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.24f))
            .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape)
    )
}

/** A tap: a ring that grows from the fingertip and fades. Draws nothing before [at] or after it has faded. */
@Composable
private fun TapRipple(at: Float, now: Float, x: Float, y: Float, widthPx: Float, heightPx: Float) {
    val p = ((now - at) / 0.22f)
    if (p <= 0f || p >= 1f) return
    val size = lerp(30f, 88f, p)
    val sizeDp = with(LocalDensity.current) { size.toDp() }
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (widthPx * x - size.dp.roundToPx() / 2f).roundToInt(),
                    (heightPx * y - size.dp.roundToPx() / 2f).roundToInt()
                )
            }
            .size(sizeDp)
            .graphicsLayer { alpha = (1f - p) * 0.9f }
            .border(2.dp, Color.White, CircleShape)
    )
}

// ---- the tools ------------------------------------------------------------------------------------------------

/** The speed control from the player's Playback tab, working. */
@Composable
internal fun SpeedTryIt(modifier: Modifier = Modifier) {
    var speed by remember { mutableFloatStateOf(1f) }
    GuidePanel(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text("Try it: playback speed", style = ApturaTypography.titleMedium, color = ApturaColors.TextHighEmphasis)
            Text(
                text = "Drag along the rail. The same control is in the player's Playback tab.",
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis
            )
            Spacer(Modifier.height(8.dp))
            SpeedRail(
                speeds = PlaybackSpeedOptions.all,
                selectedSpeed = speed,
                onSelect = { speed = it },
                label = "Playback speed"
            )
        }
    }
}
