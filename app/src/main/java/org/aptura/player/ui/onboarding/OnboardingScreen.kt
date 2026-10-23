package org.aptura.player.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

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
 */
@Composable
fun OnboardingScreen(
    onRequestPermissionClick: () -> Unit,
    onContinueWithoutPermissionClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasStorageAccess: Boolean = false,
    onFinishClick: () -> Unit = {},
    onReachedLastPage: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val page = pagerState.currentPage

    LaunchedEffect(page) {
        if (page == LAST_PAGE) onReachedLastPage()
    }
    // Back steps to the previous page instead of leaving the guide.
    BackHandler(enabled = page > 0) {
        coroutineScope.launch { pagerState.animateScrollToPage(page - 1) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApturaColors.SurfaceBase)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            GuidePage(
                // Pages dim slightly as they slide away, so the swipe reads as one motion.
                distanceFromCenter = { abs((pagerState.currentPage - index) + pagerState.currentPageOffsetFraction) }
            ) {
                when (index) {
                    0 -> {
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
                            body = "Videos appear on their own and stay up to date."
                        )
                        LibraryMock()
                        FeatureList(
                            listOf(
                                "Search, filter and sort" to "Search by title, filter by folder, and order the grid your way.",
                                "Continue Watching" to "Videos you stopped partway sit at the top, with the time left.",
                                "The ⋮ menu on any video" to "Play next, add to queue, play audio only, share, open in another player, rename, hide or delete.",
                                "More places" to "Add a folder or an SD card, or open a single file."
                            )
                        )
                    }
                    2 -> {
                        PageHeading(
                            label = "In the player",
                            title = "Control playback with your thumbs",
                            body = "The screen is split in two. No buttons needed."
                        )
                        GestureMap()
                        FeatureList(ExtraGestures)
                    }
                    3 -> {
                        PageHeading(
                            label = "Tools",
                            title = "Tools for the details",
                            body = "Tap the screen while a video plays to show the controls and menus. These are in there."
                        )
                        ToolGrid()
                    }
                    else -> {
                        PageHeading(
                            label = if (hasStorageAccess) "Ready" else "Access",
                            title = if (hasStorageAccess) "You're all set" else "Access Your Library",
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

        // The same fade at the top, so scrolled content never runs under the status bar or the Skip button.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 80.dp)
                .background(
                    Brush.verticalGradient(
                        0f to ApturaColors.SurfaceBase,
                        0.65f to ApturaColors.SurfaceBase,
                        1f to Color.Transparent
                    )
                )
        )

        // Skip. Declared AFTER the pager: later siblings draw and receive touches on top. When it was declared before
        // the full-screen pager, the pager swallowed every tap and Skip never worked (audit P1-03).
        AnimatedVisibility(
            visible = page < LAST_PAGE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 16.dp)
        ) {
            TextButton(
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(LAST_PAGE) } }
            ) {
                Text(text = "Skip", style = ApturaTypography.labelLarge, color = ApturaColors.TextMediumEmphasis)
            }
        }

        // A pages content scrolls, and it must never show through the controls: fade it out behind them.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.30f to ApturaColors.SurfaceBase,
                        1f to ApturaColors.SurfaceBase
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageIndicator(current = page)
            Spacer(Modifier.height(20.dp))

            val primaryColors = ButtonDefaults.buttonColors(
                containerColor = ApturaColors.AccentPrimary,
                contentColor = ApturaColors.TextOnAccent
            )
            val buttonModifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .height(52.dp)

            when {
                page < LAST_PAGE -> {
                    Button(
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(page + 1) } },
                        modifier = buttonModifier,
                        shape = CircleShape,
                        colors = primaryColors
                    ) {
                        Text(text = "Continue", style = ApturaTypography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                hasStorageAccess -> {
                    Button(onClick = onFinishClick, modifier = buttonModifier, shape = CircleShape, colors = primaryColors) {
                        Text(text = "Start watching", style = ApturaTypography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(onClick = onRequestPermissionClick, modifier = buttonModifier, shape = CircleShape, colors = primaryColors) {
                        Text(text = "Grant Media Access", style = ApturaTypography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // One fixed-height slot, so the layout never jumps between pages.
            Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                when {
                    page == LAST_PAGE && !hasStorageAccess -> TextButton(onClick = onContinueWithoutPermissionClick) {
                        Text(
                            text = "Continue without access (Limited Mode)",
                            style = ApturaTypography.bodyMedium,
                            color = ApturaColors.TextMediumEmphasis
                        )
                    }
                    page > 0 -> TextButton(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(page - 1) } }) {
                        Text(text = "Back", style = ApturaTypography.bodyMedium, color = ApturaColors.TextMediumEmphasis)
                    }
                }
            }
        }
    }
}

/** One scrolling page: content is capped to a readable width and leaves room for Skip above and the controls below. */
@Composable
private fun GuidePage(
    distanceFromCenter: () -> Float,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - distanceFromCenter().coerceIn(0f, 1f) * 0.6f },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 224.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            content = content
        )
    }
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
                if (selected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
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
