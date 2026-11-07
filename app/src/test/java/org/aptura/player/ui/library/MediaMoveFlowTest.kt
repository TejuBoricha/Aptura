package org.aptura.player.ui.library

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Looper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import org.aptura.player.core.media.MoveOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Audit DATA-08: a batch move asks Android once for permission to change all the files, then moves them. The real
 * composable runs here with a fake mover and a fake permission dialog, so what the person would see is checked:
 * every video is moved once, and the result is reported once, after the last one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaMoveFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** A mover whose platform call takes real time, so work that overlaps would show. */
    private class FakeMover(
        private val sender: IntentSender?,
        private val millisPerMove: Long = 60
    ) : InPlaceMoveEngine {
        val calls = CopyOnWriteArrayList<String>()

        override fun moveInPlace(uriString: String, destinationPath: String): MoveOutcome {
            calls += uriString
            Thread.sleep(millisPerMove)
            return MoveOutcome.Moved
        }

        override fun requestBatchWriteConsent(uris: List<String>): IntentSender? = sender
    }

    /**
     * The system's permission dialog. It stays on screen until the test answers it, as it does until the person taps:
     * by then the screen has been redrawn with the move under way. Answering at once, inside `launch`, would hide the
     * very state the bug lives in.
     */
    private class FakeConsentDialog : ActivityResultRegistry() {
        private var shownFor: Int? = null

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            shownFor = requestCode
        }

        val isShowing: Boolean get() = shownFor != null

        fun tap(resultCode: Int) {
            val code = checkNotNull(shownFor) { "no permission dialog is showing" }
            shownFor = null
            dispatchResult(code, resultCode, Intent())
        }
    }

    private fun anIntentSender(): IntentSender {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE).intentSender
    }

    private fun uris(vararg n: Int) = n.map { "content://media/external/video/media/$it" }

    private fun show(
        mover: InPlaceMoveEngine,
        dialog: FakeConsentDialog,
        onMoved: (String) -> Unit = {}
    ): MediaMoveFlow {
        lateinit var flow: MediaMoveFlow
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = dialog
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                flow = rememberMediaMoveFlow(onMoved, mover)
            }
        }
        composeRule.waitForIdle()
        return flow
    }

    /**
     * Lets the screen run until the first report arrives, then a while longer, so work that should not be happening has
     * time to show. Robolectric's main looper is paused: what the dialog and the mover's threads post back to the main
     * thread runs only when the test idles it, so this loop does that itself instead of leaning on `waitUntil`.
     */
    private fun waitForReport(results: List<MoveBatchResult>) {
        fun settle() {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
        }
        val deadline = System.currentTimeMillis() + 15_000
        while (results.isEmpty() && System.currentTimeMillis() < deadline) {
            settle()
            Thread.sleep(20)
        }
        assertTrue("a result was reported within 15 seconds", results.isNotEmpty())
        repeat(15) {
            Thread.sleep(100)
            settle()
        }
    }

    @Test
    fun aBatchMove_movesEachVideoOnce_andReportsOnceAfterTheLastOne() {
        val batch = uris(1, 2, 3)
        val mover = FakeMover(anIntentSender())
        val movedCallbacks = CopyOnWriteArrayList<String>()
        val dialog = FakeConsentDialog()
        val flow = show(mover, dialog) { movedCallbacks += it }
        val results = CopyOnWriteArrayList<MoveBatchResult>()

        composeRule.runOnUiThread { flow.moveAll(batch, "Movies/Trips") { results += it } }
        composeRule.waitForIdle()
        assertTrue("Android asked once for permission to change all three", dialog.isShowing)
        composeRule.runOnUiThread { dialog.tap(Activity.RESULT_OK) }
        waitForReport(results)

        assertEquals("each video is moved once, not again by a second run over the queue", batch, mover.calls.sorted())
        assertEquals("the result is reported once", 1, results.size)
        assertEquals("all three are reported moved, each once", batch, results.single().movedUris.sorted())
        assertTrue("none failed", results.single().failedUris.isEmpty())
        assertEquals("the library is told about each move once", batch, movedCallbacks.sorted())
    }

    // --- Guards: the paths that were right must stay right ---

    @Test
    fun aSingleVideoMove_stillMovesOnce_andReportsOnce() {
        val batch = uris(1)
        val mover = FakeMover(anIntentSender())
        val dialog = FakeConsentDialog()
        val flow = show(mover, dialog)
        val results = CopyOnWriteArrayList<MoveBatchResult>()

        composeRule.runOnUiThread { flow.moveAll(batch, "Movies/Trips") { results += it } }
        waitForReport(results)

        assertTrue("one video needs no batch dialog", !dialog.isShowing)
        assertEquals(batch, mover.calls.toList())
        assertEquals(1, results.size)
        assertEquals(batch, results.single().movedUris)
    }

    @Test
    fun declining_theBatchPermission_movesNothing_andReportsEveryVideoFailed() {
        val batch = uris(1, 2, 3)
        val mover = FakeMover(anIntentSender())
        val dialog = FakeConsentDialog()
        val flow = show(mover, dialog)
        val results = CopyOnWriteArrayList<MoveBatchResult>()

        composeRule.runOnUiThread { flow.moveAll(batch, "Movies/Trips") { results += it } }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { dialog.tap(Activity.RESULT_CANCELED) }
        waitForReport(results)

        assertTrue("nothing was touched", mover.calls.isEmpty())
        assertEquals(1, results.size)
        assertTrue(results.single().movedUris.isEmpty())
        assertEquals(batch, results.single().failedUris.sorted())
    }

    @Test
    fun aSecondMove_whileABatchIsRunning_isRefused_andTheBatchIsUnaffected() {
        val batch = uris(1, 2, 3)
        val mover = FakeMover(anIntentSender(), millisPerMove = 200)
        val dialog = FakeConsentDialog()
        val flow = show(mover, dialog)
        val first = CopyOnWriteArrayList<MoveBatchResult>()
        val second = CopyOnWriteArrayList<MoveBatchResult>()

        composeRule.runOnUiThread { flow.moveAll(batch, "Movies/Trips") { first += it } }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { dialog.tap(Activity.RESULT_OK) }
        // The batch is now moving (200 ms per video). A second request arrives while it runs.
        composeRule.runOnUiThread { flow.moveAll(uris(9), "Movies/Other") { second += it } }
        waitForReport(first)

        assertEquals("the second move was refused at once", 1, second.size)
        assertTrue(second.single().movedUris.isEmpty())
        assertEquals(uris(9), second.single().failedUris)
        assertTrue("the refused video was never touched", "content://media/external/video/media/9" !in mover.calls)
    }
}
