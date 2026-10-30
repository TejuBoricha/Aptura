package org.aptura.player.ui.library

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The search field's hint on a narrow phone. The field shares its row with three buttons, and "Search videos" used to
 * be cut to "Search vi…", which reads as a fault. The hint now says "Search" when the longer wording does not fit.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class LibrarySearchHintTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(scopeName: String? = null, fontScale: Float = 1f) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ApturaTheme {
                    LibraryHeader(
                        query = "",
                        scopeName = scopeName,
                        isRefreshing = false,
                        showRefreshAndSettings = true,
                        pagePadding = 16.dp,
                        onQueryChange = {},
                        onSearchDone = {},
                        onViewOptions = {},
                        onRefresh = {},
                        onSettingsClick = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** What the hint says, and whether its text had to be cut to fit. */
    private fun hint(): Pair<String, Boolean> {
        val node = composeRule.onNode(hasText("Search", substring = true), useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        val read = node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results) ?: false
        assertTrue("the hint's text layout could not be read", read && results.isNotEmpty())
        val layout = results.single()
        // hasVisualOverflow is not used: it can read true for a line that fits. A one-line hint is cut when its line is
        // ellipsized, or when it had to wrap.
        val cut = layout.lineCount > 1 || layout.isLineEllipsized(layout.lineCount - 1)
        return layout.layoutInput.text.text to cut
    }

    private fun assertNeverCut() {
        val (text, cut) = hint()
        assertFalse("the hint \"$text\" is cut off", cut)
    }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test
    fun aCommonPhone_showsTheFullHint() {
        show()
        assertEquals("Search videos" to false, hint())
    }

    @Config(qualifiers = "w360dp-h780dp-xhdpi")
    @Test
    fun aNarrowPhone_neverShowsACutWord() {
        show()
        assertNeverCut()
    }

    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    @Test
    fun aVeryNarrowPhone_showsNoHintRatherThanACutWord() {
        show()
        // Not even "Search" fits beside the three buttons here: the magnifier says it.
        val hints = composeRule.onAllNodes(hasText("Search", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("expected no hint text, found $hints", hints.isEmpty())
    }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test
    fun aLargeSystemFont_saysJustSearchRatherThanCuttingTheWord() {
        show(fontScale = 1.6f)
        assertEquals("Search" to false, hint())
    }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test
    fun aShortFolderName_keepsItsScope() {
        show(scopeName = "Movies")
        assertEquals("Search Movies" to false, hint())
    }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test
    fun aLongFolderName_fallsBackToSearchInsteadOfCuttingTheName() {
        show(scopeName = "Screen recordings")
        assertEquals("Search" to false, hint())
    }

    @Config(qualifiers = "w600dp-h900dp-xhdpi")
    @Test
    fun aWideWindow_keepsTheFullHint() {
        show(scopeName = "Screen recordings")
        assertEquals("Search Screen recordings" to false, hint())
    }
}
