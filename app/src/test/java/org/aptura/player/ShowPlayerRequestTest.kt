package org.aptura.player

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit FUNC-05: tapping the playback notification after the task was swiped away opened the library, with no way back
 * to the video that was playing. The notification now asks for the player, and the navigation opens it for the video
 * that is loaded, once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShowPlayerRequestTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    // --- The notification's intent ---

    @Test
    fun theNotificationsIntent_asksForThePlayer_inTheMainActivity() {
        val intent = MainActivity.showPlayerIntent(context)

        assertEquals(MainActivity.ACTION_SHOW_PLAYER, intent.action)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
    }

    @Test
    fun onlyTheNotificationsAction_isARequestToShowThePlayer() {
        assertTrue(MainActivity.isShowPlayerRequest(MainActivity.showPlayerIntent(context)))

        // Everything else that opens the app must keep opening it the way it does today.
        assertFalse("the launcher icon", MainActivity.isShowPlayerRequest(Intent(Intent.ACTION_MAIN)))
        assertFalse("Open with", MainActivity.isShowPlayerRequest(Intent(Intent.ACTION_VIEW)))
        assertFalse("an intent with no action", MainActivity.isShowPlayerRequest(Intent()))
        assertFalse("no intent at all", MainActivity.isShowPlayerRequest(null))
    }

    // --- Which route the tap opens ---

    @Test
    fun aRequest_withAVideoLoaded_opensThePlayerForThatVideo() {
        val key = playerKeyToShow(requested = true, currentVideoUri = "content://media/7", topOfStack = MediaLibrary)

        assertEquals(PlayerKey("content://media/7"), key)
    }

    @Test
    fun noRequest_opensNothing() {
        assertNull(playerKeyToShow(requested = false, currentVideoUri = "content://media/7", topOfStack = MediaLibrary))
    }

    @Test
    fun aRequest_whenNothingIsLoaded_staysWhereItIs() {
        assertNull(playerKeyToShow(requested = true, currentVideoUri = null, topOfStack = MediaLibrary))
        assertNull(playerKeyToShow(requested = true, currentVideoUri = "", topOfStack = MediaLibrary))
    }

    @Test
    fun aRequest_whenThePlayerIsAlreadyOnScreen_doesNotStackAnotherOne() {
        val onScreen = PlayerKey("content://media/7")

        assertNull(playerKeyToShow(requested = true, currentVideoUri = "content://media/7", topOfStack = onScreen))
    }
}
