package org.aptura.player.ui.player

import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import org.aptura.player.ui.components.ApturaModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.aptura.player.ui.components.ApturaTextButton
import org.aptura.player.ui.components.ApturaTone
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

    ApturaModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ApturaColors.SurfaceElevated2,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            PlayerSheetHeader(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Audio",
                subtitle = if (audioEnhancement.enabled) "Sound effects on" else "Original sound",
                subtitleIsActive = audioEnhancement.enabled,
                onClose = onDismissRequest
            )

            Spacer(modifier = Modifier.height(14.dp))

            PlayerSheetTabs(
                tabs = listOf(
                    "Tracks" to Icons.Default.Sync,
                    "Equalizer" to Icons.Default.GraphicEq,
                    "Effects" to Icons.Default.SurroundSound
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        SheetSection("Audio track") {
            if (audioTracks.isEmpty()) {
                SheetRowText("Default track", "This video has one audio track.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    audioTracks.forEach { track ->
                        SheetOptionRow(
                            title = formatTrackLabel(track),
                            subtitle = if (track.isExternal) "Added file" else null,
                            selected = track.isSelected || track.index == selectedAudioTrackIndex,
                            onClick = { onAudioTrackSelected(track.index) }
                        )
                    }
                }
            }
        }

        // A positive offset delays the sound (see AudioDelayProcessor). Wireless headphones already make sound late,
        // so the fix for them is "Earlier". The old one-tap "Buds (+150ms)" preset made them worse, so device presets
        // are gone: the words now say which way to move.
        SheetSection("Audio sync") {
            TimingOffsetControl(
                offsetMs = audioDelayMs,
                onOffsetChange = onAudioDelayChanged,
                rangeMs = AUDIO_SYNC_RANGE_MS,
                stepMs = delayStepMs.coerceAtLeast(10L),
                earlierLabel = "Sound earlier",
                laterLabel = "Sound later",
                label = "Audio sync",
                describe = { ms ->
                    when {
                        ms == 0L -> "Sound and picture are in sync"
                        ms > 0L -> "Sound plays ${formatOffset(ms).drop(1)} after the picture"
                        else -> "Sound plays ${formatOffset(ms).drop(1)} before the picture"
                    }
                }
            )
            // Wireless audio is the usual cause of late sound, so when it is the output, offer the usual fix in one tap.
            // It is a starting point, not a preset: latency varies by headphone, and Android compensates for some.
            if (rememberBluetoothOutputConnected() && audioDelayMs == 0L) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ApturaColors.SurfaceElevated2)
                        .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
                ) {
                    Text(
                        text = "Bluetooth is connected, and wireless sound often runs late. A good place to start:",
                        style = ApturaTypography.bodySmall,
                        color = ApturaColors.TextMediumEmphasis,
                        modifier = Modifier.weight(1f)
                    )
                    ApturaTextButton(
                        text = "Try ${formatOffset(-BLUETOOTH_SUGGESTED_OFFSET_MS)}",
                        onClick = { onAudioDelayChanged(-BLUETOOTH_SUGGESTED_OFFSET_MS) },
                        tone = ApturaTone.Accent
                    )
                }
            } else {
                Text(
                    text = "Hearing voices after lips move, for example on Bluetooth headphones? Move toward Sound earlier.",
                    style = ApturaTypography.bodySmall,
                    color = ApturaColors.TextLowEmphasis
                )
            }
        }
    }
}

/** Two-sided audio offset range shown in the player (the processor itself accepts up to ten seconds). */
private const val AUDIO_SYNC_RANGE_MS = 1_000L

/** Typical uncompensated Bluetooth lag. */
private const val BLUETOOTH_SUGGESTED_OFFSET_MS = 200L

/** Whether sound is going to a Bluetooth device right now; follows headphones connecting and disconnecting. */
@Composable
private fun rememberBluetoothOutputConnected(): Boolean {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return@DisposableEffect onDispose {}
        fun refresh() {
            connected = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in BLUETOOTH_OUTPUT_TYPES }
        }
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh()
        }
        refresh()
        audio.registerAudioDeviceCallback(callback, null)
        onDispose { audio.unregisterAudioDeviceCallback(callback) }
    }
    return connected
}

@android.annotation.SuppressLint("InlinedApi") // Plain ints; older releases just never report the BLE types.
private val BLUETOOTH_OUTPUT_TYPES = setOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER
)

@Composable
private fun EqualizerTab(
    audioEnhancement: AudioEnhancementState,
    onEqualizerEnabledChanged: (Boolean) -> Unit,
    onEqualizerPresetSelected: (EqualizerPreset) -> Unit,
    onEqualizerBandChanged: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        SheetSection("Tone") {
            SheetSwitchRow(
                title = "Equalizer",
                subtitle = if (audioEnhancement.enabled) "Shaping the sound with the bands below" else "Off: you hear the original mix",
                checked = audioEnhancement.enabled,
                onCheckedChange = onEqualizerEnabledChanged
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqualizerPreset.entries.filter { it != EqualizerPreset.CUSTOM || audioEnhancement.preset == it }.forEach { preset ->
                    SheetChip(
                        label = preset.displayName,
                        selected = audioEnhancement.enabled && audioEnhancement.preset == preset,
                        onClick = {
                            if (!audioEnhancement.enabled) onEqualizerEnabledChanged(true)
                            onEqualizerPresetSelected(preset)
                        }
                    )
                }
            }
        }

        SheetSection("Bands") {
            if (audioEnhancement.bands.isEmpty()) {
                SheetRowText("Not available", "This device doesn't offer an equalizer to apps.")
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    audioEnhancement.bands.forEach { band ->
                        VerticalMixerFader(
                            band = band,
                            enabled = audioEnhancement.enabled,
                            onLevelChanged = { newMilliBels -> onEqualizerBandChanged(band.bandIndex, newMilliBels) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * One equalizer band: a vertical slider with its 0 dB line, filled from 0 dB to the level, the gain above and the
 * frequency (with its unit) below. The cap used to move by a fixed 95 dp whatever the fader's height, so +3 dB and
 * −1 dB sat at almost the same place; it is now drawn from the measured height. Tap or drag anywhere on it.
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
    val currentLevel by rememberUpdatedState(band.levelMilliBels)
    val currentOnChange by rememberUpdatedState(onLevelChanged)

    val freqLabel = if (band.centerFreqHz >= 1000) {
        val k = band.centerFreqHz / 1000f
        "${if (k % 1f == 0f) k.toInt().toString() else "%.1f".format(k)} kHz"
    } else {
        "${band.centerFreqHz} Hz"
    }
    val dB = band.levelMilliBels / 100f
    val dBText = when {
        dB > 0f -> "+${"%.0f".format(dB)} dB"
        dB < 0f -> "−${"%.0f".format(-dB)} dB"
        else -> "0 dB"
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dBText,
            style = ApturaTypography.labelMedium,
            color = when {
                !enabled -> ApturaColors.TextLowEmphasis
                band.levelMilliBels != 0 -> ApturaColors.AccentPrimary
                else -> ApturaColors.TextMediumEmphasis
            }
        )
        Spacer(Modifier.height(8.dp))
        val accent = ApturaColors.AccentPrimary
        val trackColor = ApturaColors.SurfaceElevated3
        val zeroColor = ApturaColors.TextLowEmphasis
        val ring = ApturaColors.SurfaceElevated1
        Canvas(
            modifier = Modifier
                .weight(1f)
                .width(48.dp)
                .semantics {
                    contentDescription = "$freqLabel band"
                    stateDescription = dBText
                    progressBarRangeInfo = ProgressBarRangeInfo(band.levelMilliBels.toFloat(), minLevel..maxLevel)
                    if (enabled) {
                        setProgress { target ->
                            currentOnChange(target.toInt().coerceIn(band.minMilliBels, band.maxMilliBels))
                            true
                        }
                    }
                }
                .pointerInput(enabled, minLevel, maxLevel) {
                    if (!enabled) return@pointerInput
                    val pad = 10.dp.toPx()
                    fun levelAt(y: Float): Int {
                        val fraction = 1f - ((y - pad) / (size.height - 2 * pad)).coerceIn(0f, 1f)
                        // Snap to whole decibels; a hair either side of zero lands on zero.
                        return ((minLevel + fraction * range) / 100f).let { kotlin.math.round(it) * 100f }.toInt()
                            .coerceIn(band.minMilliBels, band.maxMilliBels)
                    }
                    detectTapGestures { currentOnChange(levelAt(it.y)) }
                }
                .pointerInput(enabled, minLevel, maxLevel) {
                    if (!enabled) return@pointerInput
                    val pad = 10.dp.toPx()
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val fraction = 1f - ((change.position.y - pad) / (size.height - 2 * pad)).coerceIn(0f, 1f)
                        val next = (kotlin.math.round((minLevel + fraction * range) / 100f) * 100f).toInt()
                            .coerceIn(band.minMilliBels, band.maxMilliBels)
                        if ((currentLevel < 0 && next >= 0) || (currentLevel > 0 && next <= 0)) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        if (next != currentLevel) currentOnChange(next)
                    }
                }
        ) {
            val pad = 10.dp.toPx()
            val cx = size.width / 2f
            val usable = size.height - 2 * pad
            fun yOf(level: Float) = pad + (1f - ((level - minLevel) / range).coerceIn(0f, 1f)) * usable
            val w = 4.dp.toPx()
            drawRoundRect(trackColor, topLeft = Offset(cx - w / 2, pad), size = Size(w, usable), cornerRadius = CornerRadius(w / 2))
            val zeroY = yOf(0f)
            drawRect(zeroColor, topLeft = Offset(cx - 9.dp.toPx(), zeroY - 1.dp.toPx()), size = Size(18.dp.toPx(), 2.dp.toPx()))
            val y = yOf(band.levelMilliBels.toFloat())
            if (enabled) {
                drawRoundRect(accent, topLeft = Offset(cx - w / 2, minOf(y, zeroY)), size = Size(w, kotlin.math.abs(zeroY - y)), cornerRadius = CornerRadius(w / 2))
            }
            drawCircle(ring, radius = 12.dp.toPx(), center = Offset(cx, y))
            drawCircle(if (enabled) accent else zeroColor, radius = 10.dp.toPx(), center = Offset(cx, y))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = freqLabel,
            style = ApturaTypography.labelSmall,
            color = if (enabled) ApturaColors.TextMediumEmphasis else ApturaColors.TextLowEmphasis
        )
    }
}

@Composable
private fun SoundEffectsTab(
    audioEnhancement: AudioEnhancementState,
    onBassBoostChanged: (Int) -> Unit,
    onVirtualizerChanged: (Int) -> Unit,
    onLoudnessBoostChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // One slider per effect. Each card used to carry a slider *and* four preset buttons that set the same value.
        SheetSection("Effects") {
            SoundEffectRow(
                title = "Bass boost",
                subtitle = "More low end, best on headphones",
                valuePercent = audioEnhancement.bassBoostPercent,
                onValueChanged = onBassBoostChanged
            )
            SoundEffectRow(
                title = "Surround",
                subtitle = "Widens stereo so it feels less inside your head",
                valuePercent = audioEnhancement.virtualizerPercent,
                onValueChanged = onVirtualizerChanged
            )
            SoundEffectRow(
                title = "Dialogue boost",
                subtitle = "Lifts quiet voices and evens out loud scenes",
                valuePercent = audioEnhancement.loudnessBoostPercent,
                onValueChanged = onLoudnessBoostChanged
            )
        }
    }
}

@Composable
private fun SoundEffectRow(
    title: String,
    subtitle: String,
    valuePercent: Int,
    onValueChanged: (Int) -> Unit
) {
    SheetSliderRow(
        title = title,
        subtitle = subtitle,
        valueText = if (valuePercent > 0) "$valuePercent%" else "Off",
        value = valuePercent.toFloat(),
        onValueChange = { onValueChanged((it / 5f).roundToInt() * 5) },
        valueRange = 0f..100f
    )
}

/**
 * Humanizes raw technical track labels (e.g. "und", "UND") into friendly user-facing labels.
 */
private fun formatTrackLabel(track: TrackInfo): String {
    val raw = track.label.trim()
    if (raw.isBlank() || raw.equals("und", ignoreCase = true) || raw.equals("null", ignoreCase = true)) {
        return "Track ${track.index + 1}"
    }
    return raw
}
