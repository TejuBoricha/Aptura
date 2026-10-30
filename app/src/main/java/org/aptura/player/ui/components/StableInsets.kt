package org.aptura.player.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.runtime.Composable

/*
 * Window insets that keep their size while the bars are hidden.
 *
 * The player hides the system bars and shows them again as it closes. A screen laid out with the ordinary
 * (visibility-aware) insets measures zero for the first frames of the way back and then jumps down by the height of the
 * status bar when the bars return. The bars' sizes never change, only their visibility, so screens that can be revealed
 * by closing the player pad for these instead.
 */

val StableSystemBars: WindowInsets
    @OptIn(ExperimentalLayoutApi::class)
    @Composable get() = WindowInsets.systemBarsIgnoringVisibility

val StableStatusBars: WindowInsets
    @OptIn(ExperimentalLayoutApi::class)
    @Composable get() = WindowInsets.statusBarsIgnoringVisibility

val StableNavigationBars: WindowInsets
    @OptIn(ExperimentalLayoutApi::class)
    @Composable get() = WindowInsets.navigationBarsIgnoringVisibility
