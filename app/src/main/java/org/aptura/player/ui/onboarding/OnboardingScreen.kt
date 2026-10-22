package org.aptura.player.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/**
 * First-run Onboarding & Permissions Flow (SCR-001, FR-LIB-001..003).
 * Explains Aptura's 100% offline trust boundary, precision engine, and requests media permissions.
 */
@Composable
fun OnboardingScreen(
    onRequestPermissionClick: () -> Unit,
    onContinueWithoutPermissionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApturaColors.SurfaceBase)
    ) {
        // Top Skip Button (shown on first 2 pages)
        AnimatedVisibility(
            visible = pagerState.currentPage < 2,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 20.dp)
        ) {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(2)
                    }
                }
            ) {
                Text(
                    text = "Skip",
                    style = ApturaTypography.labelLarge,
                    color = ApturaColors.TextMediumEmphasis
                )
            }
        }

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> OnboardingPageContent(
                    icon = Icons.Default.Shield,
                    badgeText = "ZERO NETWORK · 100% OFFLINE",
                    title = "Absolute Privacy",
                    description = "Aptura operates completely offline with zero network permissions. No telemetry, no remote analytics, no accounts. Your personal videos never leave your device."
                )
                1 -> OnboardingPageContent(
                    icon = Icons.Default.Tune,
                    badgeText = "STUDIO GRADE · MEDIA3 ENGINE",
                    title = "Precision Playback",
                    description = "Engineered with a 5-band equalizer, fine lip-sync delay, custom concentric scrubber, subtitle offset calibration, and hardware video decoding."
                )
                2 -> OnboardingPageContent(
                    icon = Icons.Default.FolderShared,
                    badgeText = "ON-DEVICE STORAGE · PRIVACY FIRST",
                    title = "Access Your Library",
                    description = "To scan and organize your video collection, Aptura requests standard media read access. We never alter, rename, or delete your files without your explicit confirmation."
                )
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pager Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) ApturaColors.AccentPrimary
                                else ApturaColors.SurfaceElevated2
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            if (pagerState.currentPage < 2) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApturaColors.SurfaceElevated2,
                        contentColor = ApturaColors.TextHighEmphasis
                    )
                ) {
                    Text(
                        text = "Continue",
                        style = ApturaTypography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Button(
                    onClick = onRequestPermissionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApturaColors.AccentPrimary,
                        contentColor = ApturaColors.SurfacePureBlack
                    )
                ) {
                    Text(
                        text = "Grant Media Access",
                        style = ApturaTypography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onContinueWithoutPermissionClick,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = "Continue without access (Limited Mode)",
                        style = ApturaTypography.bodyMedium,
                        color = ApturaColors.TextLowEmphasis
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    icon: ImageVector,
    badgeText: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .verticalScroll(scrollState)
                .padding(top = 32.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon circular badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(ApturaColors.SurfaceElevated1),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ApturaColors.AccentPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Badge pill
            Box(
                modifier = Modifier
                    .background(ApturaColors.SurfaceElevated2, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = badgeText,
                    style = ApturaTypography.labelSmall,
                    color = ApturaColors.AccentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = title,
                style = ApturaTypography.headlineLarge,
                color = ApturaColors.TextHighEmphasis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = description,
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis,
                textAlign = TextAlign.Center,
                lineHeight = androidx.compose.ui.unit.TextUnit(22f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
    }
}
