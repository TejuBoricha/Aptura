package org.aptura.player.ui.library

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext

/**
 * The details sheet reads the file system and MediaStore through [LocalDetailsLookupContext], so a test can keep that
 * work on the calling thread. A test that composes the sheet without this once failed on CI because a lookup came back
 * on an IO worker thread and the Compose test harness applied a frame there (`DetailsPlayLabelTest`). The race itself
 * is too rare to hit on demand, so these tests pin the cause: both lookups go through the given context, and with the
 * test helper they never leave the calling thread.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DetailsLookupContextTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Records the thread of every dispatch and runs the work there, so it never hops. */
    private class RecordingContext : CoroutineDispatcher() {
        val threads = CopyOnWriteArrayList<String>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            threads += Thread.currentThread().name
            block.run()
        }
    }

    private val item = DomainMediaItem(
        uri = "content://media/1",
        title = "Movie",
        durationMs = 7_200_000L,
        sizeBytes = 500_000_000L,
        mimeType = "video/mp4"
    )

    @Test
    fun bothLookups_runThroughTheContextTheSheetIsGiven_onTheCallingThread() {
        val lookups = RecordingContext()
        val testThread = Thread.currentThread().name

        composeRule.setContent {
            CompositionLocalProvider(
                LocalThumbnailDecodeContext provides Dispatchers.Main.immediate,
                LocalDetailsLookupContext provides lookups
            ) {
                ApturaTheme { MediaDetailsContent(item = item, onClose = {}, onPlayClick = {}) }
            }
        }
        composeRule.waitForIdle()

        assertEquals("the path lookup and the metadata lookup each dispatch once", 2, lookups.threads.size)
        assertEquals(
            "neither lookup left the thread that composed the sheet",
            List(2) { testThread },
            lookups.threads.toList()
        )
    }

    @Test
    fun inProduction_theLookupsRunOnTheIoPool() {
        lateinit var seen: CoroutineContext

        composeRule.setContent { seen = LocalDetailsLookupContext.current }
        composeRule.waitForIdle()

        assertSame("the default must keep file reads off the main thread", Dispatchers.IO, seen)
    }
}
