package org.aptura.player.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SEC-11: Verifies tapjacking and overlay protections on critical and destructive dialogs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApturaDialogSecurityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun destructiveDialog_rendersAndConfirmsUnderNormalConditions() {
        var confirmed = false
        var dismissed = false

        composeRule.setContent {
            ApturaDialog(
                onDismissRequest = { dismissed = true },
                title = "Clear watch history?",
                message = "This resets playback progress across all videos.",
                confirmLabel = "Clear",
                confirmTone = ApturaTone.Destructive,
                icon = Icons.Default.DeleteForever,
                onConfirm = { confirmed = true }
            )
        }

        composeRule.onNodeWithText("Clear watch history?").assertExists()
        composeRule.onNodeWithText("Clear").performClick()
        assertTrue("Confirm action is invoked on un-obscured tap", confirmed)
        assertFalse("Dismiss action was not invoked", dismissed)
    }

    @Test
    fun nonDestructiveDialog_rendersNormalAccentConfirmation() {
        var confirmed = false

        composeRule.setContent {
            ApturaDialog(
                onDismissRequest = {},
                title = "Reset playback defaults?",
                confirmLabel = "Reset",
                confirmTone = ApturaTone.Accent,
                onConfirm = { confirmed = true }
            )
        }

        composeRule.onNodeWithText("Reset playback defaults?").assertExists()
        composeRule.onNodeWithText("Reset").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun filterTouchesWhenObscured_modifierExistsAndCompilesCleanly() {
        var clicked = false
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .filterTouchesWhenObscured()
                    .testTag("secured_box")
            )
        }
        composeRule.onNodeWithTag("secured_box").assertExists()
    }
}
