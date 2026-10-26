package org.aptura.player.ui.onboarding

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.visual.captureScreen
import org.aptura.player.ui.visual.toPx
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The guide footer used to be drawn over the pager, so long pages could never scroll their last row clear of it
 * (audit 2026-10-24, F-05). The fix moved the footer outside the pager. These guards check both halves of that claim:
 * the page's last row stops above the footer, and the footer paints an opaque strip all the way to the screen edge.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xhdpi")
class GuideFooterPixelTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        mode: ThemeMode = ThemeMode.LIGHT,
        fontScale: Float = 1f,
        hasStorageAccess: Boolean = false
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ApturaTheme(themeMode = mode) {
                    OnboardingScreen(
                        hasStorageAccess = hasStorageAccess,
                        onRequestPermissionClick = {},
                        onContinueWithoutPermissionClick = {}
                    )
                }
            }
        }
    }

    private fun goToTheLibraryPage() {
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.waitForIdle()
    }

    private fun scrollToTheLastLibraryRow() {
        // The pager itself scrolls horizontally; only the page inside it scrolls vertically. Matching just
        // hasScrollAction() found both roots and failed the test with two nodes.
        val verticalPage = hasScrollAction() and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        composeRule.onNode(verticalPage).performScrollToNode(hasText("More places"))
        composeRule.waitForIdle()
    }

    @Test
    fun onTheLibraryPage_theLastRowStopsAboveTheFooter_andTheFooterIsOpaque() {
        show()
        goToTheLibraryPage()
        scrollToTheLastLibraryRow()

        val screen = composeRule.captureScreen()
        val footer = composeRule.onNodeWithTag("guide_footer").getUnclippedBoundsInRoot()
        val lastRow = composeRule.onNodeWithText("More places").getUnclippedBoundsInRoot()

        assertTrue(
            "the last page row (${lastRow.bottom}) must end above the fixed footer (${footer.top})",
            lastRow.bottom <= footer.top
        )

        val left = composeRule.toPx(footer.left + 1.dp)
        val right = left + 1
        val top = composeRule.toPx(footer.top + 2.dp)
        val bottom = composeRule.toPx(footer.bottom - 2.dp)
        screen.assertRegionIs(
            ApturaColors.SurfaceBase,
            left,
            top,
            right,
            bottom,
            "guide footer strip in light mode"
        )
    }

    @Test
    fun inDarkMode_theFooterStripAndPrimaryActionKeepTheirTokens() {
        show(mode = ThemeMode.DARK, hasStorageAccess = true)
        composeRule.onNodeWithText("Skip").performClick()
        composeRule.waitForIdle()

        val screen = composeRule.captureScreen()
        val footer = composeRule.onNodeWithTag("guide_footer").getUnclippedBoundsInRoot()
        val action = composeRule.onNodeWithText("Start watching").getUnclippedBoundsInRoot()

        val left = composeRule.toPx(footer.left + 1.dp)
        val right = left + 1
        val top = composeRule.toPx(footer.top + 2.dp)
        val bottom = composeRule.toPx(footer.bottom - 2.dp)
        screen.assertRegionIs(ApturaColors.SurfaceBase, left, top, right, bottom, "dark guide footer strip")

        // Eight dp inside the circle-shaped 52 dp button is clear of the centred label and inside the accent fill.
        val buttonX = composeRule.toPx(action.left + 8.dp)
        val buttonY = composeRule.toPx((action.top + action.bottom) / 2)
        screen.assertRegionIs(
            ApturaColors.AccentPrimary,
            buttonX,
            buttonY,
            buttonX + 1,
            buttonY + 1,
            "guide primary action in dark mode"
        )

        assertTrue("action must sit inside the footer", action.top >= footer.top && action.bottom <= footer.bottom)
    }

    @Test
    fun at200PercentFont_theFooterStillFitsTheActionWithoutOverlap() {
        show(fontScale = 2f, hasStorageAccess = true)
        composeRule.onNodeWithText("Skip").performClick()
        composeRule.waitForIdle()

        val footer = composeRule.onNodeWithTag("guide_footer").getUnclippedBoundsInRoot()
        val action = composeRule.onNodeWithText("Start watching").getUnclippedBoundsInRoot()

        assertTrue("action top ${action.top} must stay below the footer top ${footer.top}", action.top >= footer.top)
        assertTrue("action bottom ${action.bottom} must stay above the screen edge ${footer.bottom}", action.bottom <= footer.bottom)
        assertTrue("the action must not be pushed off screen", action.bottom <= footer.bottom)
    }
}
