package org.aptura.player.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.Dispatchers

/**
 * Runs the details sheet's lookups and the thumbnail decode on the calling thread, for a test that composes them.
 *
 * In a Compose test the code after a hop to another thread resumes on that thread, and the test harness applies the
 * next frame there. When that frame changes a visible text it fails with "Only the original thread that created a view
 * hierarchy can touch its views" (seen once on CI in `DetailsPlayLabelTest`). Production keeps the IO pool; the
 * work done here is the same, only the thread differs.
 */
@Composable
internal fun OnTheCallingThread(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalThumbnailDecodeContext provides Dispatchers.Main.immediate,
        LocalDetailsLookupContext provides Dispatchers.Main.immediate,
        content = content
    )
}
