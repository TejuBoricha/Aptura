package org.aptura.player.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.library.ApturaMark

/*
 * The guide's pages. Each page says one thing: a headline, a picture of how it works that is built from the app's own
 * parts (see GuideDemos.kt), and a short list. Pictures are described once for TalkBack and their parts are hidden from
 * it, so a screen reader hears one clear sentence instead of fragments.
 */

private val Hairline: Color @Composable get() = ApturaColors.BorderSubtle

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

/** A heading block shared by the pages: a small label, then the title, then one sentence. Each part arrives in turn. */
@Composable
internal fun PageHeading(label: String, title: String, body: String) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = ApturaTypography.labelSmall.copy(letterSpacing = 1.8.sp),
            color = ApturaColors.AccentPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.reveal(1)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = ApturaTypography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
            color = ApturaColors.TextHighEmphasis,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier.reveal(2)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            style = ApturaTypography.bodyLarge,
            color = ApturaColors.TextMediumEmphasis,
            textAlign = TextAlign.Start,
            modifier = Modifier.reveal(3)
        )
    }
}

/** A short list of what a page's picture cannot show, each with a plain-language outcome. */
@Composable
internal fun FeatureList(items: List<Pair<String, String>>, firstReveal: Int = 5) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEachIndexed { index, (title, detail) ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.reveal(firstReveal + index)) {
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

// ---- page 1: welcome and privacy ----------------------------------------------------------------------------------

/**
 * The Aptura mark inside rings that spread outward and fade, with a lock on it: the page's one idea (this stays here)
 * before a word is read. The rings move only when the guide's motion is on.
 */
@Composable
internal fun WelcomeHero(modifier: Modifier = Modifier) {
    val accent = ApturaColors.AccentPrimary
    val phase: State<Float> = if (LocalGuideMotion.current) {
        rememberInfiniteTransition(label = "welcome-rings").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
            label = "ring-phase"
        )
    } else {
        remember { mutableFloatStateOf(0.3f) }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inner = 34.dp.toPx()
            val outer = 66.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.24f), Color.Transparent), center = center, radius = outer),
                radius = outer,
                center = center
            )
            for (ring in 0 until 3) {
                val p = (phase.value + ring / 3f) % 1f
                drawCircle(
                    color = accent.copy(alpha = (1f - p) * 0.34f),
                    radius = lerp(inner, outer, p),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
        Box {
            ApturaMark(size = 64.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ApturaColors.SurfaceBase)
                    .border(1.5.dp, accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            }
        }
    }
}

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
                modifier = Modifier
                    .fillMaxWidth()
                    .reveal(4 + index)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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

// ---- page 2: the library ------------------------------------------------------------------------------------------

/**
 * The actions the ⋮ menu on a video offers, spelled as the menu spells them. A test opens the real menu and checks that
 * each one is there, so the guide cannot name an action the menu does not have.
 */
internal val GuideMenuActions = listOf(
    "Play next", "Add to queue", "Select", "Move to folder", "Share", "Hide from library", "Delete video"
)

/** What the library does beyond what its picture shows. Every line describes a control the library really has. */
internal val LibraryFeatures = listOf(
    "Search, filter and sort" to "Search by title, filter by folder, and order the list your way.",
    "Grid or list, your size" to "The sliders button beside search sets grid or list, picture size and which details show. Aptura remembers your choice.",
    "Swipe between folders" to "Swipe the videos left or right to move to the next folder. The folders button on the folder bar lists them all.",
    "Continue watching" to "Videos you stopped partway sit at the top of All videos, with the time left.",
    "The ⋮ menu on any video" to GuideMenuActions.joinToString(" · ") + ". Video details adds Rename and Open with.",
    "More places" to "Add a folder or an SD card, or open a single file."
)

// ---- page 3: the player -------------------------------------------------------------------------------------------

/** The gestures that are not obvious from the picture. */
internal val ExtraGestures = listOf(
    "Hold anywhere" to "Plays at 2× while you keep your finger down, then returns to your speed.",
    "Swipe left or right" to "Scrubs through the video; lift to jump there.",
    "Tap with two fingers" to "Plays or pauses.",
    "Lock button" to "Ignores every touch until you unlock, handy in a pocket or for little hands."
)

// ---- page 4: tools ------------------------------------------------------------------------------------------------

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
        GuideTools.chunked(2).forEachIndexed { row, pair ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).reveal(6 + row),
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

// ---- page 5: access -----------------------------------------------------------------------------------------------

/** A ring around the library icon, with a tick once access is granted. */
@Composable
internal fun AccessHero(hasAccess: Boolean, modifier: Modifier = Modifier) {
    val accent = ApturaColors.AccentPrimary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = accent, modifier = Modifier.size(38.dp))
        }
        if (hasAccess) {
            Box(
                modifier = Modifier
                    .offset(x = 34.dp, y = 30.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(ApturaColors.StatusSuccess),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** What access is for, and what happens without it. */
@Composable
internal fun AccessExplainer(hasAccess: Boolean) {
    GuidePanel {
        val rows = if (hasAccess) {
            listOf(
                "Your library is ready" to "Aptura already has permission to read your videos. New files appear after a scan.",
                "Change this any time" to "Revoke access in Android settings, or add Extra folders and SD cards in Aptura's Settings.",
                "See this guide again" to "Open Settings from the library and choose Welcome guide."
            )
        } else {
            listOf(
                "Why it asks" to "Android requires permission before an app can list the videos on your device.",
                "What it can do" to "Read your videos so they can be listed and played. It never changes a file without asking.",
                "Prefer not to?" to "Open single videos from your file manager, or pick a folder yourself, with no broad access."
            )
        }
        rows.forEachIndexed { index, (title, detail) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .reveal(4 + index)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(title, style = ApturaTypography.titleMedium, color = ApturaColors.TextHighEmphasis)
                Text(detail, style = ApturaTypography.bodyMedium, color = ApturaColors.TextMediumEmphasis)
            }
            if (index < rows.lastIndex) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Hairline))
            }
        }
    }
}
