package org.aptura.player.ui.player

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertTrue

/**
 * The area that actually reacts to a touch (the semantics node's touch bounds, which include the padding Compose adds
 * to reach the minimum interactive size), in dp. Material's guideline is 48 x 48 dp.
 */
internal fun SemanticsNodeInteraction.assertTouchTarget(
    density: Density,
    minWidthDp: Float = 48f,
    minHeightDp: Float = 48f
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode()
    val bounds = node.touchBoundsInRoot
    val widthDp = bounds.width / density.density
    val heightDp = bounds.height / density.density
    assertTrue(
        "touch target of ${node.config} is ${widthDp}x${heightDp} dp; needs at least ${minWidthDp}x${minHeightDp} dp",
        widthDp >= minWidthDp - 0.5f && heightDp >= minHeightDp - 0.5f
    )
    return this
}
