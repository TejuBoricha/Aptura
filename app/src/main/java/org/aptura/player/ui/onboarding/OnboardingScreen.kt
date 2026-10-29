package org.aptura.player.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.launch
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.windowSizeDp
import org.aptura.player.ui.library.MediaLibraryUiState

private const val PAGE_COUNT = 5
private const val LAST_PAGE = PAGE_COUNT - 1

/**
 * The welcome guide (SCR-001, FR-LIB-001..003): what Aptura keeps private, how the library works, the player's touch
 * controls, the tools, and finally access to your videos.
 *
 * It is shown on first launch whether or not storage access is already granted, and from Settings at any time. The last
 * page adapts: without access it asks for it (or offers Limited Mode); with access it simply lets you start.
 *
 * [onReachedLastPage] fires once the final page is shown, so the guide counts as seen even if the app is closed there.
 *
 * [animateDemos] lets the pictures move on their own (the welcome rings, the player gestures). It follows Android's
 * animation setting by default; with it off every picture rests on one frame that says the same thing.
 *
 * [libraryState] is the person's library. When it holds enough videos, the library page shows them (their own frames
 * and folders) instead of the samples, which is what the replay from Settings does.
 */
@Composable
fun OnboardingScreen(
    onRequestPermissionClick: () -> Unit,
    onContinueWithoutPermissionClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasStorageAccess: Boolean = false,
    onFinishClick: () -> Unit = {},
    onReachedLastPage: () -> Unit = {},
    animateDemos: Boolean = rememberGuideMotionEnabled(),
    libraryState: MediaLibraryUiState.Success? = null
) {
    val library = remember(libraryState) { libraryState?.let(::guideLibraryFrom) } ?: SampleLibrary
    CompositionLocalProvider(LocalGuideMotion provides animateDemos) {
        GuideContent(
            hasStorageAccess = hasStorageAccess,
            onRequestPermissionClick = onRequestPermissionClick,
            onContinueWithoutPermissionClick = onContinueWithoutPermissionClick,
            onFinishClick = onFinishClick,
            onReachedLastPage = onReachedLastPage,
            animateDemos = animateDemos,
            library = library,
            modifier = modifier
        )
    }
}

@Composable
private fun GuideContent(
    hasStorageAccess: Boolean,
    onRequestPermissionClick: () -> Unit,
    onContinueWithoutPermissionClick: () -> Unit,
    onFinishClick: () -> Unit,
    onReachedLastPage: () -> Unit,
    animateDemos: Boolean,
    library: GuideLibrary,
    modifier: Modifier
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val shortWindow = isShortWindow()
    // The first page builds itself up when the guide opens; later pages do it as they are swiped in.
    val intro = remember { Animatable(if (animateDemos) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateDemos) intro.animateTo(1f, tween(durationMillis = 900, delayMillis = 100))
    }
    val coroutineScope = rememberCoroutineScope()
    val page = pagerState.currentPage

    LaunchedEffect(page) {
        if (page == LAST_PAGE) onReachedLastPage()
    }
    // Back steps to the previous page instead of leaving the guide.
    BackHandler(enabled = page > 0) {
        coroutineScope.launch { pagerState.animateScrollToPage(page - 1) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ApturaColors.SurfaceBase)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            GuideBackdrop(
                position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                modifier = Modifier.fillMaxSize()
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .fadeEdges(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp, bottom = GUIDE_BOTTOM_FADE_HEIGHT),
                key = { it }
            ) { index ->
                GuidePage(
                    // A page's parts arrive as it is swiped to the centre, and leave as it is swiped away. The first
                    // page also waits for the guide's own opening.
                    progress = {
                        val centred = 1f - abs((pagerState.currentPage - index) + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                        if (index == 0) min(centred, intro.value) else centred
                    }
                ) {
                    when (index) {
                        0 -> {
                            if (!shortWindow) WelcomeHero(Modifier.reveal(0))
                            PageHeading(
                                label = "Welcome to Aptura",
                                title = "Everything stays on your device",
                                body = "Aptura plays the videos already on your phone. It has no internet permission at all, so the app itself cannot upload, track or sell anything."
                            )
                            PrivacyFacts()
                        }
                        1 -> {
                            PageHeading(
                                label = "Your library",
                                title = "Find it, then pick up where you left off",
                                body = "Videos appear on their own and stay up to date. Try the layouts below."
                            )
                            LibraryDemo(Modifier.reveal(4), library = library)
                            FeatureList(LibraryFeatures)
                        }
                        2 -> {
                            PageHeading(
                                label = "In the player",
                                title = "Control playback with your thumbs",
                                body = "The screen is split in two. No buttons needed."
                            )
                            PlayerDemo(Modifier.reveal(4))
                            FeatureList(ExtraGestures)
                        }
                        3 -> {
                            PageHeading(
                                label = "Tools",
                                title = "Tools for the details",
                                body = "Tap the screen while a video plays to show the controls and menus. These are in there."
                            )
                            SpeedTryIt(Modifier.reveal(4))
                            ToolGrid()
                        }
                        else -> {
                            if (!shortWindow) AccessHero(hasAccess = hasStorageAccess, modifier = Modifier.reveal(0))
                            PageHeading(
                                label = if (hasStorageAccess) "Ready" else "Access",
                                title = if (hasStorageAccess) "You're all set" else "Let Aptura see your videos",
                                body = if (hasStorageAccess) {
                                    "You can start watching now."
                                } else {
                                    "To list the videos on your device, Aptura needs permission to read them."
                                }
                            )
                            AccessExplainer(hasAccess = hasStorageAccess)
                        }
                    }
                }
            }

            // Where you are, and Skip. Declared AFTER the pager: later siblings draw and receive touches on top. When
            // Skip was declared before the full-screen pager, the pager swallowed every tap and Skip never worked
            // (audit P1-03). Fully qualified: the imported name resolves to the ColumnScope extension of the footer.
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 24.dp, end = 16.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The capsules below already say "Page n of N" to a screen reader; this is the same fact for the eye.
                Text(
                    text = "${page + 1} / $PAGE_COUNT",
                    style = ApturaTypography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = ApturaColors.TextMediumEmphasis,
                    modifier = Modifier.clearAndSetSemantics { }
                )
                Spacer(Modifier.weight(1f))
                androidx.compose.animation.AnimatedVisibility(
                    visible = page < LAST_PAGE,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextButton(
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(LAST_PAGE) } }
                    ) {
                        Text(text = "Skip", style = ApturaTypography.labelLarge, color = ApturaColors.TextMediumEmphasis)
                    }
                }
            }
        }

        val onNext: () -> Unit = { coroutineScope.launch { pagerState.animateScrollToPage(page + 1) } }
        val onBack: () -> Unit = { coroutineScope.launch { pagerState.animateScrollToPage(page - 1) } }
        // A landscape phone has barely 400 dp of height: stacked dots, button and Back would take half of it and hide the
        // page. There the three share one row.
        val window = windowSizeDp()
        val compactFooter = window.height < 520.dp && window.width > window.height

        if (compactFooter) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guide_footer")
                    .background(ApturaColors.SurfaceBase)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    GuideSecondaryAction(page, hasStorageAccess, onBack, onContinueWithoutPermissionClick)
                }
                PageIndicator(current = page)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    GuidePrimaryButton(
                        page = page,
                        hasStorageAccess = hasStorageAccess,
                        onNext = onNext,
                        onFinish = onFinishClick,
                        onRequestPermission = onRequestPermissionClick,
                        modifier = Modifier.widthIn(min = 200.dp).height(48.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guide_footer")
                    .background(ApturaColors.SurfaceBase)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(current = page)
                Spacer(Modifier.height(20.dp))
                GuidePrimaryButton(
                    page = page,
                    hasStorageAccess = hasStorageAccess,
                    onNext = onNext,
                    onFinish = onFinishClick,
                    onRequestPermission = onRequestPermissionClick,
                    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().height(52.dp)
                )
                // One fixed-height slot, so the layout never jumps between pages.
                Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                    GuideSecondaryAction(page, hasStorageAccess, onBack, onContinueWithoutPermissionClick)
                }
            }
        }
    }
}

/** The one thing to do on this page: go on, start watching, or allow access. */
@Composable
private fun GuidePrimaryButton(
    page: Int,
    hasStorageAccess: Boolean,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ButtonDefaults.buttonColors(
        containerColor = ApturaColors.AccentPrimary,
        contentColor = ApturaColors.TextOnAccent
    )
    when {
        page < LAST_PAGE -> Button(onClick = onNext, modifier = modifier, shape = CircleShape, colors = colors) {
            Text(text = "Continue", style = ApturaTypography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        hasStorageAccess -> Button(onClick = onFinish, modifier = modifier, shape = CircleShape, colors = colors) {
            Text(text = "Start watching", style = ApturaTypography.labelLarge, fontWeight = FontWeight.Bold)
        }
        else -> Button(onClick = onRequestPermission, modifier = modifier, shape = CircleShape, colors = colors) {
            Text(text = "Allow access", style = ApturaTypography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** The quieter second choice: limited access on the last page without access, otherwise Back (never on the first page). */
@Composable
private fun GuideSecondaryAction(
    page: Int,
    hasStorageAccess: Boolean,
    onBack: () -> Unit,
    onContinueWithoutPermission: () -> Unit
) {
    when {
        page == LAST_PAGE && !hasStorageAccess -> TextButton(onClick = onContinueWithoutPermission) {
            Text(
                text = "Continue with limited access",
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis
            )
        }
        page > 0 -> TextButton(onClick = onBack) {
            Text(text = "Back", style = ApturaTypography.bodyMedium, color = ApturaColors.TextMediumEmphasis)
        }
    }
}

internal val GUIDE_BOTTOM_FADE_HEIGHT = 20.dp

/**
 * Bottom inset for a page's content. It must exceed the edge fade, otherwise the last row can never be scrolled
 * completely out of the scrim (audit 2026-10-24: page 2's "More places" and page 4's tool grid were stuck half-faded).
 */
internal val GUIDE_CONTENT_BOTTOM_PADDING = GUIDE_BOTTOM_FADE_HEIGHT + 16.dp

/** One scrolling page: content is capped to a readable width and leaves room for the top bar and the fixed footer. */
@Composable
private fun GuidePage(
    progress: () -> Float,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    GuidePageScope(progress) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = GUIDE_CONTENT_BOTTOM_PADDING),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                content = content
            )
        }
    }
}

/**
 * Fades the top and bottom edges of a scrolling page to transparent, so content that scrolls under the top bar (or
 * towards the footer) dissolves into whatever is behind it, here the backdrop's glow, instead of being covered by a
 * flat band of the page colour that would show as a seam against it.
 */
private fun Modifier.fadeEdges(top: Dp, bottom: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val topPx = top.toPx()
        val bottomPx = bottom.toPx()
        drawRect(
            brush = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = topPx),
            size = Size(size.width, topPx),
            blendMode = BlendMode.DstIn
        )
        drawRect(
            brush = Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, startY = size.height - bottomPx, endY = size.height),
            topLeft = Offset(0f, size.height - bottomPx),
            size = Size(size.width, bottomPx),
            blendMode = BlendMode.DstIn
        )
    }

/**
 * A soft glow of the accent colour behind the pages. It drifts sideways as the pages are swiped, so the whole guide
 * feels like one surface moving under the finger instead of five separate screens. Drawn behind everything, and read in
 * the draw phase, so a swipe costs no recomposition.
 */
@Composable
private fun GuideBackdrop(position: () -> Float, modifier: Modifier = Modifier) {
    val accent = ApturaColors.AccentPrimary
    val strength = if (ApturaColors.IsDark) 0.20f else 0.11f
    Box(
        modifier = modifier.drawBehind {
            val centre = Offset(size.width * (1.05f - 0.30f * position()), size.height * 0.08f)
            val radius = size.width * 0.95f
            drawCircle(
                brush = Brush.radialGradient(listOf(accent.copy(alpha = strength), Color.Transparent), center = centre, radius = radius),
                radius = radius,
                center = centre
            )
        }
    )
}

/** Capsules, the current one wider. Spoken once as "Page n of N" rather than as five unnamed shapes. */
@Composable
private fun PageIndicator(current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Page ${current + 1} of $PAGE_COUNT"
        }
    ) {
        repeat(PAGE_COUNT) { index ->
            val selected = index == current
            val width = animateDpAsState(if (selected) 24.dp else 8.dp, label = "indicator-width")
            val color = animateColorAsState(
                if (selected) ApturaColors.AccentPrimary else ApturaColors.TextLowEmphasis.copy(alpha = 0.35f),
                label = "indicator-color"
            )
            Box(
                modifier = Modifier
                    .width(width.value)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color.value)
                    .clearAndSetSemantics { }
            )
        }
    }
}
