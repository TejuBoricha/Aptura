package org.aptura.player.core.media

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coordinates external subtitle file validation and importation.
 * Guarantees that hostile file parsing, archive decompression, and charset normalization
 * execute strictly off the main thread on an IO dispatcher (complying with audit SEC-02).
 */
interface SubtitleImportCoordinator {
    suspend fun importSubtitle(
        context: Context,
        uri: Uri,
        onExternalSubtitleSelected: (Uri, String, String) -> Unit,
        showToast: (String, Int) -> Unit,
        performHaptic: (HapticFeedbackType) -> Unit
    ): SubtitleValidationResult
}

class DefaultSubtitleImportCoordinator(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val validator: (Context, Uri) -> SubtitleValidationResult = { ctx, uri ->
        SubtitleFileValidator.validate(ctx, uri)
    }
) : SubtitleImportCoordinator {

    override suspend fun importSubtitle(
        context: Context,
        uri: Uri,
        onExternalSubtitleSelected: (Uri, String, String) -> Unit,
        showToast: (String, Int) -> Unit,
        performHaptic: (HapticFeedbackType) -> Unit
    ): SubtitleValidationResult {
        val validation = withContext(ioDispatcher) {
            validator(context, uri)
        }
        when (validation) {
            is SubtitleValidationResult.Valid -> {
                val targetUri = validation.validatedUri ?: uri
                onExternalSubtitleSelected(targetUri, validation.mimeType, validation.displayName)
                performHaptic(HapticFeedbackType.TextHandleMove)
                showToast("Loaded subtitle: ${validation.displayName}", android.widget.Toast.LENGTH_SHORT)
            }
            is SubtitleValidationResult.Invalid -> {
                performHaptic(HapticFeedbackType.LongPress)
                showToast(validation.reason, android.widget.Toast.LENGTH_LONG)
            }
        }
        return validation
    }
}

val LocalSubtitleImportCoordinator = staticCompositionLocalOf<SubtitleImportCoordinator> {
    DefaultSubtitleImportCoordinator()
}
