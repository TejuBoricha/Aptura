package org.aptura.player.ui.visual

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue

/**
 * Pixel inspection for Compose screens under Robolectric's native graphics.
 *
 * `captureToImage()` waits for a frame callback that Robolectric never delivers, so it times out
 * (robolectric#8071). Drawing the host view into a bitmap synchronously captures exactly the pixels Compose
 * composed. The tests that use this deliberately assert *coarse flat regions* (an opaque bar, a selection surface,
 * a footer strip), never text glyphs: the result is deterministic across fonts while still catching the class of
 * defect semantics cannot see, such as a bar that fades to transparent under the rows it is supposed to cover.
 */
class CapturedScreen internal constructor(private val pixels: PixelMap) {

    val width: Int get() = pixels.width
    val height: Int get() = pixels.height

    fun colorAt(x: Int, y: Int): Color = pixels[x, y]

    /**
     * Asserts that every pixel in [left, right) x [top, bottom) is exactly [expected].
     * The message names the first mismatch so a failure points at the offending region instead of at a boolean.
     */
    fun assertRegionIs(
        expected: Color,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        label: String
    ) {
        require(left in 0..right && right <= width) { "region x [$left, $right) is outside 0..$width" }
        require(top in 0..bottom && bottom <= height) { "region y [$top, $bottom) is outside 0..$height" }

        var mismatches = 0
        var first: Pair<Int, Int>? = null
        for (y in top until bottom) {
            for (x in left until right) {
                if (pixels[x, y] != expected) {
                    mismatches++
                    if (first == null) first = x to y
                }
            }
        }
        val total = (right - left) * (bottom - top)
        val firstColor = first?.let { (x, y) -> pixels[x, y] }
        assertTrue(
            "$label: $mismatches of $total pixels are not $expected (first at $first was $firstColor)",
            mismatches == 0
        )
    }
}

fun ComposeContentTestRule.captureScreen(): CapturedScreen = onRoot().captureRoot()

/**
 * Captures the window that owns [this] node. A sheet or dialog is a second Compose root, so capturing the screen's
 * root would miss it; capturing the node's own root keeps the sheet's pixels in scope.
 */
fun SemanticsNodeInteraction.captureRoot(): CapturedScreen {
    val root = fetchSemanticsNode().root ?: error("the node has no Compose view root")
    require(root is ViewRootForTest) { "not a Compose view root: ${root::class.java.name}" }
    val view = root.view
    require(view.width > 0 && view.height > 0) { "the Compose root has not been laid out yet" }

    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    return CapturedScreen(bitmap.asImageBitmap().toPixelMap())
}

fun ComposeContentTestRule.toPx(dp: Dp): Int = with(density) { dp.roundToPx() }

fun ComposeContentTestRule.px(dp: Float): Int = toPx(dp.dp)
