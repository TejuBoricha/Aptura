package org.aptura.player.ui.settings

import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performScrollToNode
import org.aptura.player.core.legal.LicenseDocument
import org.aptura.player.core.legal.LicenseDocuments
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * Settings > Open-source licences. The notice that FFmpeg is used (LGPL-2.1 section 6) must be where nobody has to look for it,
 * every licence text must be reachable, and a text must really be read from the packaged assets and shown (not just a row that
 * says it would be).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LicensesScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        onBack: () -> Unit = {},
        readText: (Context, LicenseDocument) -> String = LicenseDocuments::read
    ) {
        composeRule.setContent { ApturaTheme { LicensesScreen(onBackClick = onBack, readText = readText) } }
        composeRule.waitForIdle()
    }

    /**
     * An opened row is brought as high as the list allows: right under the back button, whose row's top edge sits one content
     * padding (8 dp) below the top bar (the window here is 1 px per dp and has no status bar inset), or, when what is left
     * below it is too short to fill the screen (a short text in the last row), with the list scrolled to its very end.
     */
    private fun assertRowIsAsHighAsTheListAllows(id: String) {
        val top = composeRule.onNodeWithTag(licenseDocumentTag(id)).fetchSemanticsNode().boundsInRoot.top
        val atTheTop = (SETTINGS_TOP_BAR_HEIGHT_DP + 8) * composeRule.density.density
        val range = composeRule.onNode(hasScrollAction()).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        val atTheEnd = range.value() >= range.maxValue() - 1f
        assertTrue(
            "the '$id' row should be at the top ($atTheTop px) or the list at its end, but the row's top edge is at $top px " +
                "and the list is scrolled ${range.value()} of ${range.maxValue()}",
            abs(top - atTheTop) <= 2f || atTheEnd
        )
    }

    private fun scrollTo(tag: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
    }

    /** The text is read on a background thread; Robolectric's main looper runs what it posts back only when the test idles it. */
    private fun waitForText(text: String) {
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return
            Thread.sleep(20)
        }
        throw AssertionError(
            "'$text' was not shown within 15 seconds. On screen:\n" + composeRule.onRoot(useUnmergedTree = true).printToString(maxDepth = 12)
        )
    }

    @Test
    fun theFfmpegNotice_isAtTheTop_withoutATap_andLinksToTheSource() {
        show()

        composeRule.onNodeWithText("This software uses libraries from the FFmpeg project", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("GNU Lesser General Public License, version 2.1", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(LICENSES_SOURCE_TAG).assertIsDisplayed()
    }

    @Test
    fun everyLicenceText_hasARow_withItsTitle() {
        show()

        for (document in LicenseDocuments.all) {
            scrollTo(licenseDocumentTag(document.id))
            composeRule.onNodeWithTag(licenseDocumentTag(document.id)).assertExists()
        }
    }

    @Test
    fun theTitleIsTheHeading_andBackGoesBack() {
        var backs = 0
        show(onBack = { backs++ })

        val title = composeRule.onNodeWithText("Open-source licences", useUnmergedTree = true)
        assertTrue(SemanticsProperties.Heading in title.fetchSemanticsNode().config)
        composeRule.onNodeWithTag(LICENSES_BACK_TAG).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun openingTheLgplRow_showsTheRealLicenceText_andCollapsesAgain() {
        show()
        scrollTo(licenseDocumentTag("ffmpeg-lgpl"))
        composeRule.onNodeWithTag(licenseDocumentTag("ffmpeg-lgpl")).performClick()

        waitForText("GNU LESSER GENERAL PUBLIC LICENSE")
        // The state is spoken, not only drawn: exactly one row says Expanded.
        composeRule.onAllNodes(expectState("Expanded"), useUnmergedTree = true).assertCountEquals(1)

        scrollTo(licenseDocumentTag("ffmpeg-lgpl"))
        composeRule.onNodeWithTag(licenseDocumentTag("ffmpeg-lgpl")).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(expectState("Expanded"), useUnmergedTree = true).assertCountEquals(0)
        assertTrue(
            "the text should be gone after collapsing",
            composeRule.onAllNodesWithText("GNU LESSER GENERAL PUBLIC LICENSE", substring = true).fetchSemanticsNodes().isEmpty()
        )
    }

    private fun expectState(value: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    /** DESIGN.md: a new screen is looked at on a small phone held sideways (740 x 360 dp) as well as upright. */
    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test
    fun onASmallSidewaysPhone_theBackButtonAndTheNoticeAreInView_andATextStillOpens() {
        show()

        composeRule.onNodeWithTag(LICENSES_BACK_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("This software uses libraries from the FFmpeg project", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(LICENSES_SOURCE_TAG).assertIsDisplayed()

        scrollTo(licenseDocumentTag("ffmpeg-lgpl"))
        composeRule.onNodeWithTag(licenseDocumentTag("ffmpeg-lgpl")).performClick()
        waitForText("GNU LESSER GENERAL PUBLIC LICENSE")
    }

    @Test
    fun opening_aDifferentRow_closesTheFirst() {
        show()
        scrollTo(licenseDocumentTag("aptura"))
        composeRule.onNodeWithTag(licenseDocumentTag("aptura")).performClick()
        // Not just "Apache License": the row's own subtitle says that before anything is read.
        waitForText("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION")

        scrollTo(licenseDocumentTag("protobuf"))
        composeRule.onNodeWithTag(licenseDocumentTag("protobuf")).performClick()
        waitForText("Redistributions in binary form must reproduce the above")
        assertRowIsAsHighAsTheListAllows("protobuf")

        assertTrue(
            "the Apache text should have closed when the Protocol Buffers row opened",
            composeRule.onAllNodesWithText("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION", substring = true)
                .fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * Regression: the row was scrolled to the top while its text was still being read, when only a "Reading…" line sat below it,
     * so the list could not scroll far enough; once the text arrived nothing moved it, and the first lines opened below the
     * fold. It passed when the read was fast and failed on a slow CI runner. A deliberately slow read must give the same result.
     */
    @Test
    fun aSlowRead_stillBringsTheOpenedRowToTheTop_withItsTextInView() {
        show(readText = { context, document ->
            Thread.sleep(600)
            LicenseDocuments.read(context, document)
        })
        scrollTo(licenseDocumentTag("aptura"))
        composeRule.onNodeWithTag(licenseDocumentTag("aptura")).performClick()
        waitForText("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION")
        assertRowIsAsHighAsTheListAllows("aptura")

        scrollTo(licenseDocumentTag("protobuf"))
        composeRule.onNodeWithTag(licenseDocumentTag("protobuf")).performClick()
        waitForText("Redistributions in binary form must reproduce the above")
        assertRowIsAsHighAsTheListAllows("protobuf")
    }

    @Test
    fun reopeningAnAlreadyReadText_bringsItsRowToTheTopAgain() {
        show()
        scrollTo(licenseDocumentTag("aptura"))
        composeRule.onNodeWithTag(licenseDocumentTag("aptura")).performClick()
        waitForText("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION")
        scrollTo(licenseDocumentTag("protobuf"))
        composeRule.onNodeWithTag(licenseDocumentTag("protobuf")).performClick()
        waitForText("Redistributions in binary form must reproduce the above")

        // Apache's text was read before, so this time it is shown at once and nothing is read.
        scrollTo(licenseDocumentTag("aptura"))
        composeRule.onNodeWithTag(licenseDocumentTag("aptura")).performClick()
        waitForText("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION")
        composeRule.waitForIdle()
        assertRowIsAsHighAsTheListAllows("aptura")
    }
}
