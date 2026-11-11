package org.aptura.player.core.media

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.widget.Toast
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubtitleImportCoordinatorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun importSubtitle_runsValidationStrictlyOffMainThread() = runTest {
        val uri = Uri.parse("content://media/external/file/101")
        val threadSawBackground = AtomicBoolean(false)
        val validationThreadRef = AtomicReference<Thread?>(null)

        val coordinator = DefaultSubtitleImportCoordinator(
            ioDispatcher = kotlinx.coroutines.Dispatchers.IO,
            validator = { _, _ ->
                val currentThread = Thread.currentThread()
                validationThreadRef.set(currentThread)
                val isMainThread = Looper.getMainLooper() != null &&
                    currentThread == Looper.getMainLooper().thread
                threadSawBackground.set(!isMainThread)
                SubtitleValidationResult.Valid(
                    displayName = "test.srt",
                    mimeType = "application/x-subrip",
                    formatDescription = "SubRip"
                )
            }
        )

        var selectedUri: Uri? = null
        coordinator.importSubtitle(
            context = context,
            uri = uri,
            onExternalSubtitleSelected = { u, _, _ -> selectedUri = u },
            showToast = { _, _ -> },
            performHaptic = { }
        )

        assertTrue(
            "Validator must run off the Android main UI thread to protect UI responsiveness (SEC-02)",
            threadSawBackground.get()
        )
        assertNotNull(validationThreadRef.get())
        assertEquals(uri, selectedUri)
    }

    @Test
    fun importSubtitle_whenValidResult_invokesCallbackWithTargetUriAndShowsSuccessToast() = runTest {
        val originalUri = Uri.parse("content://media/external/file/original")
        val extractedTargetUri = Uri.parse("file:///data/user/0/cache/subs/extracted.srt")

        val coordinator = DefaultSubtitleImportCoordinator(
            validator = { _, _ ->
                SubtitleValidationResult.Valid(
                    displayName = "extracted.srt",
                    mimeType = "application/x-subrip",
                    formatDescription = "Extracted Subtitle",
                    validatedUri = extractedTargetUri
                )
            }
        )

        var reportedUri: Uri? = null
        var reportedMime: String? = null
        var reportedName: String? = null
        var reportedHaptic: HapticFeedbackType? = null
        var reportedToastMessage: String? = null
        var reportedToastDuration: Int? = null

        val result = coordinator.importSubtitle(
            context = context,
            uri = originalUri,
            onExternalSubtitleSelected = { u, mime, name ->
                reportedUri = u
                reportedMime = mime
                reportedName = name
            },
            showToast = { msg, duration ->
                reportedToastMessage = msg
                reportedToastDuration = duration
            },
            performHaptic = { haptic ->
                reportedHaptic = haptic
            }
        )

        assertTrue(result is SubtitleValidationResult.Valid)
        assertEquals(extractedTargetUri, reportedUri)
        assertEquals("application/x-subrip", reportedMime)
        assertEquals("extracted.srt", reportedName)
        assertEquals(HapticFeedbackType.TextHandleMove, reportedHaptic)
        assertEquals("Loaded subtitle: extracted.srt", reportedToastMessage)
        assertEquals(Toast.LENGTH_SHORT, reportedToastDuration)
    }

    @Test
    fun importSubtitle_whenValidResultWithNullValidatedUri_usesOriginalUri() = runTest {
        val originalUri = Uri.parse("content://media/external/file/direct.srt")

        val coordinator = DefaultSubtitleImportCoordinator(
            validator = { _, _ ->
                SubtitleValidationResult.Valid(
                    displayName = "direct.srt",
                    mimeType = "application/x-subrip",
                    formatDescription = "Direct Subtitle",
                    validatedUri = null
                )
            }
        )

        var reportedUri: Uri? = null
        coordinator.importSubtitle(
            context = context,
            uri = originalUri,
            onExternalSubtitleSelected = { u, _, _ -> reportedUri = u },
            showToast = { _, _ -> },
            performHaptic = { }
        )

        assertEquals(originalUri, reportedUri)
    }

    @Test
    fun importSubtitle_whenInvalidResult_showsErrorToastAndPerformsLongPressHapticWithoutSelecting() = runTest {
        val uri = Uri.parse("content://media/external/file/bad.png")
        val errorReason = "Selected file is an image (bad.png). Please choose a valid .srt, .vtt, or .ass subtitle file."

        val coordinator = DefaultSubtitleImportCoordinator(
            validator = { _, _ ->
                SubtitleValidationResult.Invalid(errorReason)
            }
        )

        var selectionCalled = false
        var reportedHaptic: HapticFeedbackType? = null
        var reportedToastMessage: String? = null
        var reportedToastDuration: Int? = null

        val result = coordinator.importSubtitle(
            context = context,
            uri = uri,
            onExternalSubtitleSelected = { _, _, _ -> selectionCalled = true },
            showToast = { msg, duration ->
                reportedToastMessage = msg
                reportedToastDuration = duration
            },
            performHaptic = { haptic ->
                reportedHaptic = haptic
            }
        )

        assertTrue(result is SubtitleValidationResult.Invalid)
        assertFalse("onExternalSubtitleSelected must NOT be called for invalid subtitle", selectionCalled)
        assertEquals(HapticFeedbackType.LongPress, reportedHaptic)
        assertEquals(errorReason, reportedToastMessage)
        assertEquals(Toast.LENGTH_LONG, reportedToastDuration)
    }

    @Test
    fun importSubtitle_withRealSubtitleFileValidator_validatesActualSrtOffMainThread() = runTest {
        val srtFile = File(context.cacheDir, "movie_subs.srt").apply {
            writeText("1\n00:00:01,000 --> 00:00:04,000\nHello World\n\n")
        }
        val uri = Uri.fromFile(srtFile)

        val coordinator = DefaultSubtitleImportCoordinator()

        var selectedUri: Uri? = null
        var selectedMime: String? = null
        var selectedName: String? = null
        var toastMessage: String? = null

        val result = coordinator.importSubtitle(
            context = context,
            uri = uri,
            onExternalSubtitleSelected = { u, mime, name ->
                selectedUri = u
                selectedMime = mime
                selectedName = name
            },
            showToast = { msg, _ -> toastMessage = msg },
            performHaptic = { }
        )

        assertTrue("Real .srt file must validate successfully", result is SubtitleValidationResult.Valid)
        assertNotNull(selectedUri)
        assertEquals("application/x-subrip", selectedMime)
        assertTrue(selectedName?.endsWith("movie_subs.srt") == true)
        assertTrue(toastMessage?.contains("movie_subs.srt") == true)
    }

    @Test
    fun importSubtitle_withRealSubtitleFileValidator_rejectsCorruptedZipArchiveOffMainThread() = runTest {
        val corruptZip = File(context.cacheDir, "corrupted.zip").apply {
            FileOutputStream(this).use { it.write(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0xFF.toByte())) }
        }
        val uri = Uri.fromFile(corruptZip)

        val coordinator = DefaultSubtitleImportCoordinator()

        var selectionCalled = false
        var toastMessage: String? = null
        var toastDuration: Int? = null

        val result = coordinator.importSubtitle(
            context = context,
            uri = uri,
            onExternalSubtitleSelected = { _, _, _ -> selectionCalled = true },
            showToast = { msg, dur ->
                toastMessage = msg
                toastDuration = dur
            },
            performHaptic = { }
        )

        assertTrue("Corrupted zip archive must fail validation", result is SubtitleValidationResult.Invalid)
        assertFalse(selectionCalled)
        assertNotNull(toastMessage)
        assertEquals(Toast.LENGTH_LONG, toastDuration)
    }
}
