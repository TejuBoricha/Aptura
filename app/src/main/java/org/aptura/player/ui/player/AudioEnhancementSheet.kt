package org.aptura.player.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.AudioEnhancementState
import org.aptura.player.domain.model.EqualizerBand
import org.aptura.player.domain.model.EqualizerPreset
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/**
 * Studio-Grade Audio & Sound Enhancements Modal Bottom Sheet (SCR-010d, FR-AUD-001..004).
 * Replaces linear wall of unstyled controls with a structured 3-tab architecture:
 * 1. Tracks & Lip-Sync: Humanized audio tracks and tactile audio delay stepper.
 * 2. Studio Equalizer: Master bypass, presets carousel, and hardware mixer vertical fader rack.
 * 3. Sound FX: Bass boost, surround sound virtualizer, and dialogue loudness booster cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEnhancementSheet(
    audioTracks: List<TrackInfo>,
    selectedAudioTrackIndex: Int?,
    audioDelayMs: Long,
    delayStepMs: Long,
    audioEnhancement: AudioEnhancementState,
    initialTab: Int = 0,
    onDismissRequest: () -> Unit,
    onAudioTrackSelected: (Int?) -> Unit,
    onAudioDelayChanged: (Long) -> Unit,
    onEqualizerEnabledChanged: (Boolean) -> Unit,
    onEqualizerPresetSelected: (EqualizerPreset) -> Unit,
    onEqualizerBandChanged: (Int, Int) -> Unit,
    onBassBoostChanged: (Int) -> Unit,
    onVirtualizerChanged: (Int) -> Unit,
    onLoudnessBoostChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ApturaColors.SurfaceElevated2,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ApturaColors.AccentSubtle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = ApturaColors.AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Audio & Sound Studio",
                            style = ApturaTypography.titleMedium,
                            color = ApturaColors.TextHighEmphasis
                        )
                        Text(
                            text = if (audioEnhancement.enabled) "DSP Effects Active" else "Native Playback",
                            style = ApturaTypography.labelSmall,
                            color = if (audioEnhancement.enabled) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Top Tab Row: [ Tracks & Sync | Equalizer | Effects ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ApturaColors.SurfaceElevated3, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "Tracks & Sync" to Icons.Default.Sync,
                    "Equalizer" to Icons.Default.GraphicEq,
                    "Effects" to Icons.Default.SurroundSound
                ).forEachIndexed { index, (label, icon) ->
                    val isSelected = selectedTab == index
                    val animBg by animateColorAsState(
                        targetValue = if (isSelected) ApturaColors.SurfaceElevated1 else Color.Transparent,
                        label = "tabBg"
                    )
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = index
                        },
                        shape = RoundedCornerShape(9.dp),
                        color = animBg,
                        border = if (isSelected) BorderStroke(1.dp, ApturaColors.BorderSubtle) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = ApturaTypography.labelSmall,
                                color = if (isSelected) ApturaColors.TextHighEmphasis else ApturaColors.TextMediumEmphasis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Tab Content
            when (selectedTab) {
                0 -> AudioTracksAndSyncTab(
                    audioTracks = audioTracks,
                    selectedAudioTrackIndex = selectedAudioTrackIndex,
                    audioDelayMs = audioDelayMs,
                    delayStepMs = delayStepMs,
                    onAudioTrackSelected = onAudioTrackSelected,
                    onAudioDelayChanged = onAudioDelayChanged
                )
                1 -> EqualizerTab(
                    audioEnhancement = audioEnhancement,
                    onEqualizerEnabledChanged = onEqualizerEnabledChanged,
                    onEqualizerPresetSelected = onEqualizerPresetSelected,
                    onEqualizerBandChanged = onEqualizerBandChanged
                )
                2 -> SoundEffectsTab(
                    audioEnhancement = audioEnhancement,
                    onBassBoostChanged = onBassBoostChanged,
                    onVirtualizerChanged = onVirtualizerChanged,
                    onLoudnessBoostChanged = onLoudnessBoostChanged
                )
            }
        }
    }
}

// ============================================================================
// TAB 1: AUDIO TRACKS & LIP-SYNC DELAY
// ============================================================================

@Composable
private fun AudioTracksAndSyncTab(
    audioTracks: List<TrackInfo>,
    selectedAudioTrackIndex: Int?,
    audioDelayMs: Long,
    delayStepMs: Long,
    onAudioTrackSelected: (Int?) -> Unit,
    onAudioDelayChanged: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION 1: AUDIO TRACK SELECTION
        Text(
            text = "AUDIO TRACK",
            style = ApturaTypography.labelSmall,
            color = ApturaColors.AccentPrimary
        )

        if (audioTracks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ApturaColors.SurfaceElevated3,
                border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Default Audio Stream",
                            style = ApturaTypography.bodyMedium,
                            color = ApturaColors.TextHighEmphasis
                        )
                        Text(
                            text = "Auto-detected system output",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.TextMediumEmphasis
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                audioTracks.forEach { track ->
                    val isSelected = track.isSelected
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ApturaColors.AccentPrimary.copy(alpha = 0.12f) else ApturaColors.SurfaceElevated3,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAudioTrackSelected(track.index)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) ApturaColors.AccentPrimary else Color.Transparent,
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis
                                    ),
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    if (isSelected) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(ApturaColors.SurfacePureBlack, CircleShape)
                                            )
                                        }
                                    }
                                }
                                Column {
                                    val cleanLabel = formatTrackLabel(track)
                                    Text(
                                        text = cleanLabel,
                                        style = ApturaTypography.bodyMedium,
                                        color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (track.language != null && !track.language.equals("und", true)) {
                                            "Language: ${track.language.uppercase()}"
                                        } else {
                                            "Embedded Audio Stream"
                                        },
                                        style = ApturaTypography.labelSmall,
                                        color = ApturaColors.TextMediumEmphasis
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = ApturaColors.AccentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SECTION 2: AUDIO SYNC (LIP-SYNC DELAY)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ApturaColors.SurfaceElevated1,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LIP-SYNC CALIBRATION",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.AccentPrimary
                        )
                        Text(
                            text = "Fine-tune audio/video alignment",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.TextMediumEmphasis
                        )
                    }
                    if (audioDelayMs != 0L) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ApturaColors.SurfaceElevated3,
                            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAudioDelayChanged(0L)
                            }
                        ) {
                            Text(
                                text = "Reset (0ms)",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.AccentPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Tactile Stepper Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ApturaColors.SurfaceElevated3,
                        border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAudioDelayChanged(audioDelayMs - delayStepMs)
                        }
                    ) {
                        Text(
                            text = "-${delayStepMs}ms",
                            style = ApturaTypography.titleSmall,
                            color = ApturaColors.TextHighEmphasis,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }

                    Text(
                        text = "${if (audioDelayMs > 0) "+" else ""}${audioDelayMs} ms",
                        style = ApturaTypography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                        color = if (audioDelayMs != 0L) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ApturaColors.SurfaceElevated3,
                        border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAudioDelayChanged(audioDelayMs + delayStepMs)
                        }
                    ) {
                        Text(
                            text = "+${delayStepMs}ms",
                            style = ApturaTypography.titleSmall,
                            color = ApturaColors.TextHighEmphasis,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }
                }

                // Device Presets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Buds (+150ms)" to 150L,
                        "Speaker (+250ms)" to 250L,
                        "Dialogue (-100ms)" to -100L
                    ).forEach { (label, presetVal) ->
                        val isPresetActive = audioDelayMs == presetVal
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPresetActive) ApturaColors.AccentPrimary.copy(alpha = 0.2f) else ApturaColors.SurfaceElevated3,
                            border = BorderStroke(
                                1.dp,
                                if (isPresetActive) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAudioDelayChanged(presetVal)
                                }
                        ) {
                            Text(
                                text = label,
                                style = ApturaTypography.labelSmall,
                                color = if (isPresetActive) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: STUDIO GRAPHIC EQUALIZER (HARDWARE MIXER RACK)
// ============================================================================

@Composable
private fun EqualizerTab(
    audioEnhancement: AudioEnhancementState,
    onEqualizerEnabledChanged: (Boolean) -> Unit,
    onEqualizerPresetSelected: (EqualizerPreset) -> Unit,
    onEqualizerBandChanged: (Int, Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Bypass Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ApturaColors.SurfaceElevated1,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EQUALIZER ENGINE",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.AccentPrimary
                    )
                    Text(
                        text = if (audioEnhancement.enabled) "Hardware DSP processing active" else "Audio bypass mode (neutral)",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextMediumEmphasis
                    )
                }
                Switch(
                    checked = audioEnhancement.enabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEqualizerEnabledChanged(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ApturaColors.SurfacePureBlack,
                        checkedTrackColor = ApturaColors.AccentPrimary,
                        uncheckedThumbColor = ApturaColors.TextMediumEmphasis,
                        uncheckedTrackColor = ApturaColors.SurfaceElevated3
                    )
                )
            }
        }

        // Presets Carousel
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PRESETS",
                style = ApturaTypography.labelSmall,
                color = ApturaColors.TextMediumEmphasis
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqualizerPreset.entries.forEach { preset ->
                    val isSelected = audioEnhancement.preset == preset && audioEnhancement.enabled
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated3,
                        border = if (isSelected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (!audioEnhancement.enabled) {
                                onEqualizerEnabledChanged(true)
                            }
                            onEqualizerPresetSelected(preset)
                        }
                    ) {
                        Text(
                            text = preset.displayName,
                            style = ApturaTypography.labelSmall,
                            color = if (isSelected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Studio Rack: Vertical Faders for 5-Band Equalizer
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ApturaColors.SurfaceElevated1,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5-BAND FREQUENCY RACK",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.AccentPrimary
                    )
                    Text(
                        text = if (audioEnhancement.enabled) "±10 dB Range" else "Bypassed",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextMediumEmphasis
                    )
                }

                if (audioEnhancement.bands.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AudioFX Equalizer not available on this device",
                            style = ApturaTypography.bodyMedium,
                            color = ApturaColors.TextMediumEmphasis
                        )
                    }
                } else {
                    // 5 Vertical Faders in a row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        audioEnhancement.bands.forEach { band ->
                            VerticalMixerFader(
                                band = band,
                                enabled = audioEnhancement.enabled,
                                onLevelChanged = { newMilliBels ->
                                    onEqualizerBandChanged(band.bandIndex, newMilliBels)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bespoke Hardware-Style Vertical Fader for the Studio Graphic Equalizer.
 * Provides a vertical fader track with center 0dB indicator tick, metallic mixer cap,
 * and tabular readout for zero-jitter visual polish.
 */
@Composable
private fun VerticalMixerFader(
    band: EqualizerBand,
    enabled: Boolean,
    onLevelChanged: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val minLevel = band.minMilliBels.toFloat()
    val maxLevel = band.maxMilliBels.toFloat()
    val range = (maxLevel - minLevel).coerceAtLeast(1f)

    val freqLabel = when {
        band.centerFreqHz >= 1000 -> "${band.centerFreqHz / 1000}k"
        else -> "${band.centerFreqHz}"
    }

    val dBValue = band.levelMilliBels / 100
    val dBText = if (dBValue > 0) "+$dBValue" else "$dBValue"

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Gain Readout at top
        Text(
            text = "${dBText}dB",
            style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = when {
                !enabled -> ApturaColors.TextLowEmphasis
                dBValue > 0 -> ApturaColors.AccentPrimary
                dBValue < 0 -> ApturaColors.TextMediumEmphasis
                else -> ApturaColors.TextHighEmphasis
            }
        )

        // Vertical Draggable Fader Area
        Box(
            modifier = Modifier
                .weight(1f)
                .width(44.dp)
                .pointerInput(enabled, minLevel, maxLevel) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val heightPx = size.height.toFloat()
                        if (heightPx > 0) {
                            // Up is negative Y, meaning positive gain increment
                            val deltaNormalized = -dragAmount / heightPx
                            val deltaMilliBels = (deltaNormalized * range).toInt()
                            val newMilliBels = (band.levelMilliBels + deltaMilliBels)
                                .coerceIn(band.minMilliBels, band.maxMilliBels)

                            // Haptic tick if crossing 0 dB center
                            if ((band.levelMilliBels < 0 && newMilliBels >= 0) ||
                                (band.levelMilliBels > 0 && newMilliBels <= 0)
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onLevelChanged(newMilliBels)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Track Groove (Background bar)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ApturaColors.SurfaceElevated3)
            )

            // Center 0 dB Tick Mark
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(2.dp)
                    .background(ApturaColors.BorderSubtle)
            )

            // Normalized position of fader cap: 0.0 (bottom, min) to 1.0 (top, max)
            val normalized = ((band.levelMilliBels - minLevel) / range).coerceIn(0f, 1f)

            // Hardware Mixer Fader Cap
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = (normalized * 95).dp) // Responsive vertical offset
                        .size(width = 32.dp, height = 18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (enabled) ApturaColors.SurfaceElevated3 else ApturaColors.SurfaceElevated1
                        )
                        .border(
                            1.dp,
                            if (enabled) ApturaColors.AccentPrimary.copy(alpha = 0.6f) else ApturaColors.BorderSubtle,
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Center grip illuminated notch
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(
                                if (enabled) ApturaColors.AccentPrimary else ApturaColors.TextLowEmphasis,
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }

        // Center Frequency Label at bottom
        Text(
            text = freqLabel,
            style = ApturaTypography.labelSmall,
            color = if (enabled) ApturaColors.TextMediumEmphasis else ApturaColors.TextLowEmphasis
        )
    }
}

// ============================================================================
// TAB 3: SOUND FX & DSP ENHANCEMENTS
// ============================================================================

@Composable
private fun SoundEffectsTab(
    audioEnhancement: AudioEnhancementState,
    onBassBoostChanged: (Int) -> Unit,
    onVirtualizerChanged: (Int) -> Unit,
    onLoudnessBoostChanged: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Bass Boost Card
        SoundEffectCard(
            title = "BASS BOOST",
            subtitle = "Deep acoustic low-end resonance for headphones & subwoofers",
            valuePercent = audioEnhancement.bassBoostPercent,
            icon = Icons.Default.Hearing,
            presets = listOf("Off" to 0, "Mild" to 30, "Punch" to 60, "Max" to 100),
            onValueChanged = onBassBoostChanged
        )

        // 2. Surround Sound Virtualizer Card
        SoundEffectCard(
            title = "SURROUND VIRTUALIZER",
            subtitle = "Expands stereo soundstage into a multi-dimensional sound field",
            valuePercent = audioEnhancement.virtualizerPercent,
            icon = Icons.Default.SurroundSound,
            presets = listOf("Off" to 0, "Spatial" to 35, "Wide" to 70, "Max" to 100),
            onValueChanged = onVirtualizerChanged
        )

        // 3. Loudness & Dialogue Booster Card
        SoundEffectCard(
            title = "DIALOGUE & LOUDNESS BOOST",
            subtitle = "Clarifies quiet whispers and balances explosive dynamic volume",
            valuePercent = audioEnhancement.loudnessBoostPercent,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            presets = listOf("Off" to 0, "Clear" to 30, "Loud" to 65, "Max" to 100),
            onValueChanged = onLoudnessBoostChanged
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundEffectCard(
    title: String,
    subtitle: String,
    valuePercent: Int,
    icon: ImageVector,
    presets: List<Pair<String, Int>>,
    onValueChanged: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ApturaColors.SurfaceElevated1,
        border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.AccentPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (valuePercent > 0) ApturaColors.AccentPrimary.copy(alpha = 0.15f) else ApturaColors.SurfaceElevated3
                ) {
                    Text(
                        text = if (valuePercent > 0) "$valuePercent%" else "OFF",
                        style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = if (valuePercent > 0) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = subtitle,
                style = ApturaTypography.labelSmall,
                color = ApturaColors.TextMediumEmphasis
            )

            // Sleek Custom Slider with Circular Thumb and Clean Rounded Track
            Slider(
                value = valuePercent.toFloat(),
                onValueChange = {
                    onValueChanged(it.toInt())
                },
                valueRange = 0f..100f,
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(ApturaColors.AccentPrimary, CircleShape)
                            .border(2.dp, ApturaColors.SurfacePureBlack, CircleShape)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 0.dp,
                        colors = SliderDefaults.colors(
                            activeTrackColor = ApturaColors.AccentPrimary,
                            inactiveTrackColor = ApturaColors.SurfaceElevated3
                        ),
                        modifier = Modifier.height(6.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Quick Preset Steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { (label, stepVal) ->
                    val isSelected = valuePercent == stepVal
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) ApturaColors.AccentPrimary.copy(alpha = 0.2f) else ApturaColors.SurfaceElevated3,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onValueChanged(stepVal)
                            }
                    ) {
                        Text(
                            text = label,
                            style = ApturaTypography.labelSmall,
                            color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Humanizes raw technical track labels (e.g. "und", "UND") into friendly user-facing labels.
 */
private fun formatTrackLabel(track: TrackInfo): String {
    val raw = track.label.trim()
    if (raw.isBlank() || raw.equals("und", ignoreCase = true) || raw.equals("null", ignoreCase = true)) {
        return "Audio Track ${track.index + 1} (Stereo)"
    }
    return raw
}
