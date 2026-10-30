package org.aptura.player.core.platform

import android.Manifest
import android.app.Application
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Access to the videos, as the system says it is now. Reported on a phone where access was granted and the library
 * stayed empty, and where the app then asked for it again on the next start: the answer was read once, when the app
 * opened, and from the reply to the app's own dialog, so access changed anywhere else was not seen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoAccessTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun grant() = shadowOf(ApplicationProvider.getApplicationContext<Application>())
        .grantPermissions(Manifest.permission.READ_MEDIA_VIDEO)

    private fun show() {
        composeRule.setContent {
            val access by rememberVideoAccess()
            Text(if (access) "access: allowed" else "access: not allowed")
        }
        composeRule.waitForIdle()
    }

    private fun comeBackToTheFront() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    @Test
    fun withoutAccess_itSaysNotAllowed() {
        show()
        composeRule.onNodeWithText("access: not allowed").assertExists()
    }

    @Test
    fun accessGivenInTheSystemSettings_isSeenWhenTheAppComesBack() {
        show()
        composeRule.onNodeWithText("access: not allowed").assertExists()

        grant()
        comeBackToTheFront()

        composeRule.onNodeWithText("access: allowed").assertExists()
    }

    @Test
    fun accessTakenAwayWhileTheAppWasAway_isSeenToo() {
        grant()
        show()
        composeRule.onNodeWithText("access: allowed").assertExists()

        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .denyPermissions(Manifest.permission.READ_MEDIA_VIDEO)
        comeBackToTheFront()

        composeRule.onNodeWithText("access: not allowed").assertExists()
    }

    @Test
    fun aGrantIsAnswered_aRefusalCanBeAskedAgain_aFinalRefusalGoesToTheSettingsPage() {
        assertEquals(AccessReply.GRANTED, replyToVideoAccessRequest(granted = true, canAskAgain = false))
        assertEquals(AccessReply.GRANTED, replyToVideoAccessRequest(granted = true, canAskAgain = true))
        assertEquals(AccessReply.DENIED, replyToVideoAccessRequest(granted = false, canAskAgain = true))
        assertEquals(AccessReply.OPEN_SETTINGS, replyToVideoAccessRequest(granted = false, canAskAgain = false))
    }

    @Test
    fun theSettingsPageIsTheAppsOwn() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val intent = appDetailsSettingsIntent(context)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
    }
}
