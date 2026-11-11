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
import org.aptura.player.core.media.DeleteOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Direct unit tests for [MediaDeleteFlow], covering audit QA-04.
 * Exercises single and batch deletions, system consent dialog interactions,
 * failure recovery, and concurrency guards.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaDeleteFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeDeleteEngine(
        private val batchSender: IntentSender? = null,
        private val singleSender: IntentSender? = null,
        private val shouldFail: Boolean = false
    ) : MediaDeleteEngine {
        val deletedCalls = CopyOnWriteArrayList<String>()

        override fun delete(uri: String): DeleteOutcome {
            if (shouldFail) return DeleteOutcome.Failed("Could not delete $uri")
            if (singleSender != null) return DeleteOutcome.NeedsConsent(singleSender)
            deletedCalls += uri
            return DeleteOutcome.Deleted
        }

        override fun requestBatchDeleteConsent(uris: List<String>): IntentSender? = batchSender
    }

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
        engine: MediaDeleteEngine,
        dialog: FakeConsentDialog,
        onDeleted: (String) -> Unit = {}
    ): MediaDeleteFlow {
        lateinit var flow: MediaDeleteFlow
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = dialog
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                flow = rememberMediaDeleteFlow(onDeleted, engine)
            }
        }
        composeRule.waitForIdle()
        return flow
    }

    private fun waitForDialog(dialog: FakeConsentDialog) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!dialog.isShowing && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
            Thread.sleep(20)
        }
        assertTrue("system consent dialog shown", dialog.isShowing)
    }

    private fun waitForReport(results: List<DeleteBatchResult>) {
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
        repeat(5) {
            Thread.sleep(50)
            settle()
        }
    }

    @Test
    fun directDelete_withoutConsent_deletesAndNotifies() {
        val target = uris(1)
        val engine = FakeDeleteEngine()
        val deletedCallbacks = CopyOnWriteArrayList<String>()
        val dialog = FakeConsentDialog()
        val flow = show(engine, dialog) { deletedCallbacks += it }
        val results = CopyOnWriteArrayList<DeleteBatchResult>()

        composeRule.runOnUiThread { flow.deleteAll(target) { results += it } }
        waitForReport(results)

        assertFalse("no dialog needed", dialog.isShowing)
        assertEquals(target, engine.deletedCalls.toList())
        assertEquals(target, deletedCallbacks.toList())
        assertEquals(1, results.size)
        assertEquals(target, results.single().deletedUris)
        assertTrue(results.single().failedUris.isEmpty())
    }

    @Test
    fun singleDelete_needsConsent_whenApproved_deletesAndNotifies() {
        val target = uris(1)
        val engine = FakeDeleteEngine(singleSender = anIntentSender())
        val deletedCallbacks = CopyOnWriteArrayList<String>()
        val dialog = FakeConsentDialog()
        val flow = show(engine, dialog) { deletedCallbacks += it }
        val results = CopyOnWriteArrayList<DeleteBatchResult>()

        composeRule.runOnUiThread { flow.deleteAll(target) { results += it } }
        waitForDialog(dialog)

        composeRule.runOnUiThread { dialog.tap(Activity.RESULT_OK) }
        waitForReport(results)

        assertEquals(target, deletedCallbacks.toList())
        assertEquals(1, results.size)
        assertEquals(target, results.single().deletedUris)
        assertTrue(results.single().failedUris.isEmpty())
    }

    @Test
    fun singleDelete_needsConsent_whenDeclined_keepsFileAndReportsFailure() {
        val target = uris(1)
        val engine = FakeDeleteEngine(singleSender = anIntentSender())
        val deletedCallbacks = CopyOnWriteArrayList<String>()
        val dialog = FakeConsentDialog()
        val flow = show(engine, dialog) { deletedCallbacks += it }
        val results = CopyOnWriteArrayList<DeleteBatchResult>()

        composeRule.runOnUiThread { flow.deleteAll(target) { results += it } }
        waitForDialog(dialog)

        composeRule.runOnUiThread { dialog.tap(Activity.RESULT_CANCELED) }
        waitForReport(results)

        assertTrue("nothing notified as deleted", deletedCallbacks.isEmpty())
        assertEquals(1, results.size)
        assertTrue("nothing deleted", results.single().deletedUris.isEmpty())
        assertEquals(target, results.single().failedUris)
    }

    @Test
    fun batchDelete_asksConsentOnce_whenApproved_deletesAll() {
        val batch = uris(1, 2, 3)
        val engine = FakeDeleteEngine(batchSender = anIntentSender())
        val deletedCallbacks = CopyOnWriteArrayList<String>()
        val dialog = FakeConsentDialog()
        val flow = show(engine, dialog) { deletedCallbacks += it }
        val results = CopyOnWriteArrayList<DeleteBatchResult>()

        composeRule.runOnUiThread { flow.deleteAll(batch) { results += it } }
        waitForDialog(dialog)

        composeRule.runOnUiThread { dialog.tap(Activity.RESULT_OK) }
        waitForReport(results)

        assertEquals(batch, deletedCallbacks.toList())
        assertEquals(1, results.size)
        assertEquals(batch, results.single().deletedUris)
        assertTrue(results.single().failedUris.isEmpty())
    }

    @Test
    fun batchDelete_whenDeclined_keepsAllFiles() {
        val batch = uris(1, 2, 3)
        val engine = FakeDeleteEngine(batchSender = anIntentSender())
        val deletedCallbacks = CopyOnWriteArrayList<String>()
        val dialog = FakeConsentDialog()
        val flow = show(engine, dialog) { deletedCallbacks += it }
        val results = CopyOnWriteArrayList<DeleteBatchResult>()

        composeRule.runOnUiThread { flow.deleteAll(batch) { results += it } }
        waitForDialog(dialog)

        composeRule.runOnUiThread { dialog.tap(Activity.RESULT_CANCELED) }
        waitForReport(results)

        assertTrue(deletedCallbacks.isEmpty())
        assertEquals(1, results.size)
        assertTrue(results.single().deletedUris.isEmpty())
        assertEquals(batch, results.single().failedUris)
    }

    @Test
    fun deleteWhileAnotherInProgress_isRejectedImmediately() {
        val firstBatch = uris(1)
        val secondBatch = uris(2)
        val engine = FakeDeleteEngine(singleSender = anIntentSender())
        val dialog = FakeConsentDialog()
        val flow = show(engine, dialog)
        val secondResults = CopyOnWriteArrayList<DeleteBatchResult>()

        composeRule.runOnUiThread { flow.deleteAll(firstBatch) }
        waitForDialog(dialog)

        composeRule.runOnUiThread {
            flow.deleteAll(secondBatch) { secondResults += it }
        }
        composeRule.waitForIdle()

        assertEquals("second request fails fast with rejected result", 1, secondResults.size)
        assertTrue(secondResults.single().deletedUris.isEmpty())
        assertEquals(secondBatch, secondResults.single().failedUris)
    }

    @Test
    fun emptyDeleteList_reportsImmediatelyWithNoOp() {
        val engine = FakeDeleteEngine()
        val dialog = FakeConsentDialog()
        val flow = show(engine, dialog)
        val results = CopyOnWriteArrayList<DeleteBatchResult>()

        composeRule.runOnUiThread { flow.deleteAll(emptyList()) { results += it } }
        composeRule.waitForIdle()

        assertFalse(dialog.isShowing)
        assertEquals(1, results.size)
        assertTrue(results.single().deletedUris.isEmpty())
        assertTrue(results.single().failedUris.isEmpty())
    }
}
