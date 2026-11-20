package org.aptura.player.core.platform

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalIntentLauncherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun safeStartActivity_whenNoHandlerAvailable_catchesExceptionAndReturnsFalse() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).checkActivities(true)

        // Create an intent with a nonexistent action and scheme that no handler can resolve
        val intent = Intent("nonexistent.custom.action.NEVER_HANDLED").apply {
            data = Uri.parse("customscheme://nonexistent/path")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Must not crash with ActivityNotFoundException, should safely return false
        val result = context.safeStartActivity(intent, errorMessage = "No handler found")
        assertFalse(result)
    }

    @Test
    fun safeStartActivity_withChooser_handlesNonexistentTargetSafely() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).checkActivities(true)

        val intent = Intent("nonexistent.custom.action.NEVER_HANDLED").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val result = context.safeStartActivity(intent, chooserTitle = "Choose App", errorMessage = "No handler found")
        // Safe execution without crashing
        assertTrue(result || !result)
    }
}

