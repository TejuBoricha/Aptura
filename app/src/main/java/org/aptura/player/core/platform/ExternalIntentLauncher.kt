package org.aptura.player.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Safely starts an external activity intent, guarding against [ActivityNotFoundException]
 * or security exceptions when no suitable app or browser is installed (audit APP-5).
 */
fun Context.safeStartActivity(
    intent: Intent,
    chooserTitle: String? = null,
    errorMessage: String = "No app available to handle this request."
): Boolean {
    val targetIntent = if (chooserTitle != null) {
        Intent.createChooser(intent, chooserTitle)
    } else {
        intent
    }
    return try {
        startActivity(targetIntent)
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        false
    } catch (_: Exception) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        false
    }
}
