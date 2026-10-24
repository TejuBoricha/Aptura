package org.aptura.player.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/*
 * The guide's illustrations. They show how Aptura works rather than decorate: a miniature of the real library layout,
 * a map of the player's touch zones, and a grid of the tools that really exist. Illustrations are described once for
 * TalkBack and their parts are hidden from it, so a screen reader hears one clear sentence instead of fragments.
 */

private val Hairline: Color get() = ApturaColors.BorderSubtle

@Composable
internal fun GuidePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ApturaColors.SurfaceElevated1)
            .border(1.dp, Hairline, shape),
        content = content
    )
}

// ---- page 1: privacy --------------------------------------------------------------------------------------------

/** The offline promise as facts a person can check, not adjectives. */
@Composable
internal fun PrivacyFacts() {
    GuidePanel {
        val facts = listOf(
            "No internet permission" to "Android would block Aptura from going online even if it tried.",
            "No accounts, ads or analytics" to "There is nothing to sign in to and nothing that reports on you.",
            "Your files are only read" to "Nothing is renamed, moved or deleted without your explicit confirmation.",
            "Diagnostics stay on this device" to "Error logs are kept locally, and leave only if you choose to share them.",
            "Subtitle search is yours to start" to "It opens your browser with a title you can edit. The video itself never leaves your device."
        )
        facts.forEachIndexed { index, (title, detail) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ApturaColors.StatusSuccess,
                    modifier = Modifier.padding(top = 2.dp).size(20.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, style = ApturaTypography.titleMedium, color = ApturaColors.TextHighEmphasis)
                    Text(detail, style = ApturaTypography.bodyMedium, color = ApturaColors.TextMediumEmphasis)
                }
            }
            if (index < facts.lastIndex) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Hairline))
            }
        }
    }
}

// ---- page 2: library --------------------------------------------------------------------------------------------

/** A miniature of the real library screen: search, folder filters, and a grid with a resumable video. */
@Composable
internal fun LibraryMock(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ApturaColors.SurfaceBase)
            .border(1.dp, Hairline, shape)
            .padding(12.dp)
            .clearAndSetSemantics {
                contentDescription = "A picture of the library: a search bar, folder filters, and a grid of videos where one shows how far you watched."
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ApturaColors.SurfaceElevated1)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = ApturaColors.TextMediumEmphasis, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Search videos", style = ApturaTypography.bodyMedium, color = ApturaColors.TextLowEmphasis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MockChip("All Videos", selected = true)
            MockChip("Camera", selected = false)
            MockChip("Movies", selected = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MockThumb(Color(0xFF1C3A44), Color(0xFF0C1A20), "Beach day", progress = 0.62f, modifier = Modifier.weight(1f))
            MockThumb(Color(0xFF2B2E40), Color(0xFF12131C), "Lecture 04", progress = null, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MockChip(text: String, selected: Boolean) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        text = text,
        style = ApturaTypography.labelSmall,
        color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) ApturaColors.AccentPrimary.copy(alpha = 0.12f) else ApturaColors.SurfaceElevated1)
            .border(1.dp, if (selected) ApturaColors.AccentPrimary.copy(alpha = 0.4f) else Hairline, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun MockThumb(top: Color, bottom: Color, title: String, progress: Float?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(top, bottom)))
        ) {
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(ApturaColors.AccentPrimary)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(title, style = ApturaTypography.bodyMedium, color = ApturaColors.TextHighEmphasis)
    }
}

/** A short list of what the library can do, each with a plain-language outcome. */
@Composable
internal fun FeatureList(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEach { (title, detail) ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ApturaColors.AccentPrimary)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, style = ApturaTypography.titleMedium, color = ApturaColors.TextHighEmphasis)
                    Text(detail, style = ApturaTypography.bodyMedium, color = ApturaColors.TextMediumEmphasis)
                }
            }
        }
    }
}

// ---- page 3: gestures -------------------------------------------------------------------------------------------

/** The player as a landscape screen split into the two touch zones, with what each zone does. */
@Composable
internal fun GestureMap(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 8.2f)
            .clip(shape)
            .background(ApturaColors.SurfacePureBlack)
            .border(1.dp, Hairline, shape)
            .drawBehind {
                // The seam between the two halves.
                drawLine(
                    color = ApturaColors.BorderSubtle,
                    start = Offset(size.width / 2, 14.dp.toPx()),
                    end = Offset(size.width / 2, size.height - 38.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
            .clearAndSetSemantics {
                contentDescription = "The player screen has two halves. Swipe up or down on the left half to change brightness and on the right half to change volume. " +
                    "Double-tap the left to rewind and the right to fast-forward."
            }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(bottom = 34.dp)) {
            ZoneLabel(
                icon = Icons.Default.BrightnessMedium,
                name = "Brightness",
                doubleTapIcon = Icons.Default.FastRewind,
                doubleTapLabel = "double-tap",
                modifier = Modifier.weight(1f)
            )
            ZoneLabel(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                name = "Volume",
                doubleTapIcon = Icons.Default.FastForward,
                doubleTapLabel = "double-tap",
                modifier = Modifier.weight(1f)
            )
        }
        // The scrubber along the bottom: a swipe anywhere moves along it.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ApturaColors.AccentPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(ApturaColors.BorderSubtle)
            ) {
                Box(Modifier.fillMaxWidth(0.42f).fillMaxHeight().background(ApturaColors.AccentPrimary))
            }
            Spacer(Modifier.width(8.dp))
            Text("swipe to seek", style = ApturaTypography.labelSmall, color = ApturaColors.TextMediumEmphasis)
        }
    }
}

@Composable
private fun ZoneLabel(
    icon: ImageVector,
    name: String,
    doubleTapIcon: ImageVector,
    doubleTapLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ApturaColors.TextHighEmphasis, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.SwapVert, contentDescription = null, tint = ApturaColors.AccentPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(name, style = ApturaTypography.titleMedium, color = ApturaColors.TextHighEmphasis)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(doubleTapIcon, contentDescription = null, tint = ApturaColors.TextMediumEmphasis, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(doubleTapLabel, style = ApturaTypography.labelSmall, color = ApturaColors.TextMediumEmphasis)
        }
    }
}

/** The gestures that are not obvious from the map. */
internal val ExtraGestures = listOf(
    "Hold anywhere" to "Plays at 2× while you keep your finger down, then returns to your speed.",
    "Swipe left or right" to "Scrubs through the video; lift to jump there.",
    "Tap with two fingers" to "Plays or pauses.",
    "Lock button" to "Ignores every touch until you unlock, handy in a pocket or for little hands."
)

// ---- page 4: tools ----------------------------------------------------------------------------------------------

internal data class GuideTool(val icon: ImageVector, val title: String, val detail: String)

/** Everything here exists in the player today; if a tool is removed or added, this list is the one place to update. */
internal val GuideTools = listOf(
    GuideTool(Icons.Default.Subtitles, "Subtitles", "Import .srt, .vtt, .ass or a .zip, or search for one in your browser. Nudge timing, tap to sync, restyle."),
    GuideTool(Icons.Default.GraphicEq, "Sound studio", "Equalizer, bass boost, surround and dialogue boost, plus a lip-sync delay for earbuds."),
    GuideTool(Icons.Default.Repeat, "A-B repeat", "Loop any passage between two points you set."),
    GuideTool(Icons.Default.Bookmark, "Bookmarks", "Save a moment and jump back to it later."),
    GuideTool(Icons.Default.Bedtime, "Sleep timer", "Stops playback after the time you pick."),
    GuideTool(Icons.Default.Tune, "Picture", "Brightness, contrast and saturation, plus an eye-comfort night mode."),
    GuideTool(Icons.Default.SkipNext, "Frame step", "Move one frame at a time to catch the exact shot."),
    GuideTool(Icons.Default.Speed, "Speed & decoder", "0.25× to 2×, and hardware or software decoding per video."),
    GuideTool(Icons.AutoMirrored.Filled.QueueMusic, "Queue", "Play next, add to queue, and an Up Next countdown."),
    GuideTool(Icons.Default.PictureInPictureAlt, "Window & background", "Float the video, or keep just the audio going with the screen off."),
    GuideTool(Icons.Default.QueryStats, "Playback stats", "Live decoder, resolution, frame rate, bitrate and dropped frames."),
    GuideTool(Icons.Default.Lock, "Lock", "Freeze the touch controls until you unlock them.")
)

@Composable
internal fun ToolGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideTools.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { tool -> ToolTile(tool, Modifier.weight(1f).fillMaxHeight()) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ToolTile(tool: GuideTool, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(ApturaColors.SurfaceElevated1)
            .border(1.dp, Hairline, shape)
            .padding(14.dp)
    ) {
        Icon(tool.icon, contentDescription = null, tint = ApturaColors.AccentPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(10.dp))
        Text(tool.title, style = ApturaTypography.titleMedium, color = ApturaColors.TextHighEmphasis)
        Spacer(Modifier.height(2.dp))
        Text(tool.detail, style = ApturaTypography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 17.sp), color = ApturaColors.TextMediumEmphasis)
    }
}

// ---- page 5: access ---------------------------------------------------------------------------------------------

/** What access is for, and what happens without it. */
@Composable
internal fun AccessExplainer(hasAccess: Boolean) {
    GuidePanel {
        val rows = if (hasAccess) {
            listOf(
                "Your library is ready" to "Aptura already has permission to read your videos. New files appear after a scan.",
                "Change this any time" to "Revoke access in Android settings, or add extra folders and SD cards in Aptura's Settings.",
                "See this guide again" to "Open Settings from the library and choose Welcome Guide."
            )
        } else {
            listOf(
                "Why it asks" to "Android requires permission before an app can list the videos on your device.",
                "What it can do" to "Read your videos so they can be listed and played. It never changes a file without asking.",
                "Prefer not to?" to "Open single videos from your file manager, or pick a folder yourself, with no broad access."
            )
        }
        rows.forEachIndexed { index, (title, detail) ->
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(title, style = ApturaTypography.titleMedium, color = ApturaColors.TextHighEmphasis)
                Text(detail, style = ApturaTypography.bodyMedium, color = ApturaColors.TextMediumEmphasis)
            }
            if (index < rows.lastIndex) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Hairline))
            }
        }
    }
}

/** A heading block shared by the pages: a small label, then the title. */
@Composable
internal fun PageHeading(label: String, title: String, body: String) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = ApturaTypography.labelSmall.copy(letterSpacing = 1.8.sp),
            color = ApturaColors.AccentPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = ApturaTypography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
            color = ApturaColors.TextHighEmphasis,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            style = ApturaTypography.bodyLarge,
            color = ApturaColors.TextMediumEmphasis,
            textAlign = TextAlign.Start
        )
    }
}

