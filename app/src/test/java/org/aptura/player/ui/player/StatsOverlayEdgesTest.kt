package org.aptura.player.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.PlaybackStatsInfo
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What the stats panel does at its edges: text that is never cut in half or broken a letter before the end, and a
 * header that is always there. Native graphics, so text is measured with the real font rather than a stand-in; the
 * claims here are about how text fits.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
class StatsOverlayEdgesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val stats = PlaybackStatsInfo(
        decoderPath = "Software Fallback",
        isHardwareAccelerated = false,
        videoDecoderName = "c2.qti.hevc.decoder.secure",
        videoCodec = "hvc1.2.4.L153.B0",
        videoResolution = "3840x2160",
        frameRate = 23.976f,
        videoBitrateKbps = 24_500,
        droppedFrames = 412,
        dropPercentage = 6.4f,
        audioDecoderName = "c2.android.eac3.decoder",
        audioCodec = "ec-3",
        audioChannels = "5.1 Surround (6 ch)",
        audioSampleRateHz = 48_000,
        audioBitrateKbps = 640,
        audioDelayMs = -200L,
        subtitleDelayMs = 1_250L,
        hdrFormat = "Dolby Vision"
    )

    /** The panel alone in the window, or in a box that leaves it only [width] by [height]. */
    private fun show(fontScale: Float = 1f, height: Dp? = null, width: Dp? = null) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ApturaTheme {
                    var room: Modifier = Modifier
                    if (height != null) room = room.height(height)
                    if (width != null) room = room.width(width)
                    Box(room) { StatsOverlay(stats = stats, onCloseClick = {}) }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** A node whose text is [value] once any break opportunities are taken out: what the device reported. */
    private fun hasValue(value: String) = SemanticsMatcher("text is $value") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.any { it.text.replace("​", "") == value } == true
    }

    private fun lineTexts(value: String): List<String> {
        val node = composeRule.onNode(hasValue(value)).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        val layout = results.single()
        return (0 until layout.lineCount).map { layout.layoutInput.text.text.substring(layout.getLineStart(it), layout.getLineEnd(it)) }
    }

    @Test
    fun theTitleIsTheNameOfTheSwitchThatOpensIt() {
        show()
        composeRule.onNodeWithText("Playback stats").assertIsDisplayed()
    }

    @Test
    fun aLabelAndValueThatFit_shareALine_withTheValueFlushRight() {
        show()
        val label = composeRule.onNodeWithText("Video Bitrate").getUnclippedBoundsInRoot()
        val value = composeRule.onNodeWithText("24.5 Mbps").getUnclippedBoundsInRoot()
        val panel = composeRule.onNodeWithTag(STATS_TAG).getUnclippedBoundsInRoot()
        assertTrue("value ($value) beside label ($label)", value.left >= label.right + 8.dp)
        assertTrue("value is on the label's line", value.top < label.bottom)
        // Flush with the readout's right padding (16 dp), not floating in the middle.
        assertEquals((panel.right - 16.dp).value, value.right.value, 2f)
    }

    @Test
    fun aValueThatDoesNotFitBesideItsLabel_goesUnderIt_notIntoAFewCharactersOfWidth() {
        // 280 dp is the narrowest the panel is ever drawn (a phone held sideways with the controls moved aside).
        show(width = 280.dp)
        val label = composeRule.onNodeWithText("Audio Channels").getUnclippedBoundsInRoot()
        val value = composeRule.onNodeWithText("5.1 Surround (6 ch) · 48000 Hz").getUnclippedBoundsInRoot()
        assertTrue("value ($value) must sit under label ($label)", value.top >= label.bottom)
        assertEquals("a stacked value starts where its label starts", label.left.value, value.left.value, 0.5f)
    }

    @Test
    fun atTheLargestText_rowsStack_headerHoldsTogether_andNothingLeavesThePanel() {
        show(fontScale = 2f)
        val panel = composeRule.onNodeWithTag(STATS_TAG).getUnclippedBoundsInRoot()
        val title = composeRule.onNodeWithText("Playback stats").getUnclippedBoundsInRoot()
        val copy = composeRule.onNodeWithContentDescription("Copy stats").getUnclippedBoundsInRoot()
        val close = composeRule.onNodeWithContentDescription("Close stats").getUnclippedBoundsInRoot()
        assertTrue("title ($title) must not collide with copy button ($copy)", title.right <= copy.left)
        assertTrue("close button ($close) must fit inside panel ($panel)", close.right <= panel.right)

        val chip = composeRule.onNodeWithText("Compatibility Fallback").getUnclippedBoundsInRoot()
        val hdr = composeRule.onNodeWithText("Dolby Vision").getUnclippedBoundsInRoot()
        assertTrue("HDR badge ($hdr) must not collide with the chip ($chip)", hdr.top >= chip.bottom || hdr.left >= chip.right)

        val label = composeRule.onNodeWithText("Resolution / FPS").performScrollTo().getUnclippedBoundsInRoot()
        val value = composeRule.onNodeWithText("3840x2160 @ 24.0 fps").performScrollTo().getUnclippedBoundsInRoot()
        assertTrue("value ($value) must be stacked below label ($label) at 200% text", value.top >= label.bottom)
        assertTrue("value ($value) must stay within panel ($panel)", value.right <= panel.right)
    }

    @Test
    fun aLongDecoderName_atTheLargestText_breaksAtADot_neverLeavingALoneLetter() {
        show(fontScale = 2f)
        val lines = lineTexts("c2.qti.hevc.decoder.secure")
        assertTrue("expected the name to need more than one line at 200% text, got $lines", lines.size > 1)
        // Every line but the last ends at a break opportunity (after a dot), so nothing is split inside a word.
        lines.dropLast(1).forEach { assertTrue("a line ends in the middle of a word: '$it' in $lines", it.endsWith("​")) }
        val widowed = lines.last().replace("​", "")
        assertTrue("the last line is a lone fragment: '$widowed' in $lines", widowed.length > 2)
    }

    @Test
    fun theNameIsWhatTheDeviceReported_whenItFitsOnALine() {
        show()
        // Whole and exact: a selection copies this text, so no invisible break characters while none are needed.
        composeRule.onNodeWithText("c2.qti.hevc.decoder.secure").assertIsDisplayed()
    }

    @Test
    fun breakOpportunities_goAfterDotsAndSlashes_andRemovingThemGivesBackTheOriginal() {
        listOf("c2.qti.hevc.decoder.secure", "hvc1.2.4.L153.B0", "video/hevc", "OMX_google_h264", "ec-3").forEach { value ->
            val broken = withBreakOpportunities(value)
            assertEquals(value, broken.replace("​", ""))
        }
        assertEquals("c2.​qti.​hevc", withBreakOpportunities("c2.qti.hevc"))
        // A trailing separator has nothing after it to break before.
        assertEquals("end.", withBreakOpportunities("end."))
    }

    @Test
    fun theHeader_staysReachable_whenOnlyAFewRowsFit() {
        show(height = 150.dp)
        composeRule.onNodeWithText("Playback stats").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Copy stats").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close stats").assertIsDisplayed().performClick()
    }

    @Test
    fun everyRowCanBeScrolledTo_inAShortPanel() {
        show(height = 150.dp)
        composeRule.onNodeWithText("Subtitle Offset").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("+1250 ms").assertIsDisplayed()
        // The header did not scroll away with the rows.
        composeRule.onNodeWithContentDescription("Close stats").assertIsDisplayed()
    }

    @Test
    fun noTextStartsInTheCorner() {
        show()
        val panel = composeRule.onNodeWithTag(STATS_TAG).getUnclippedBoundsInRoot()
        val title = composeRule.onNodeWithText("Playback stats").getUnclippedBoundsInRoot()
        val label = composeRule.onNodeWithText("Decode Path").getUnclippedBoundsInRoot()
        // The corner radius is 16 dp; text that starts at least that far in is clear of the curve.
        assertTrue("the icon and title start at ${title.left - panel.left}", title.left - panel.left >= 16.dp)
        assertTrue("rows start at ${label.left - panel.left}", label.left - panel.left >= 16.dp)
    }
}
