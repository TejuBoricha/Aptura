package org.aptura.player

import android.content.ContentValues
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Puts a video from the test APK's assets into MediaStore and returns its content URI, on every API level the nightly
 * matrix runs. Returns what [unseedVideo] needs to take it out again.
 *
 * From API 29 an app inserts its own rows (scoped storage). Before that, MediaStore refuses an insert from an app that
 * does not hold WRITE_EXTERNAL_STORAGE, and this app never declares it: scoped storage only is a product invariant, so
 * the permission must not be added to make a test pass. The shell user may write to shared storage, so below API 29 the
 * file is copied into Movies/Checks by the shell and indexed by the media scanner, which is what happens to a video a
 * person copies onto a phone over USB.
 */
internal fun seedVideo(displayName: String, asset: String): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        insertAsApp(displayName, asset)
    } else {
        copyWithShellAndScan(displayName, asset)
    }
}

/** Removes a video made by [seedVideo]. Never throws: a clip left behind must not hide the result of the test. */
internal fun unseedVideo(uri: Uri) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            InstrumentationRegistry.getInstrumentation().targetContext.contentResolver.delete(uri, null, null)
        } else {
            // The app may not delete rows it did not insert before API 29; the shell may. MediaProvider removes the file.
            runShell("content delete --uri $uri")
        }
    }
}

private fun insertAsApp(displayName: String, asset: String): Uri {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val resolver = instrumentation.targetContext.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$FOLDER")
        put(MediaStore.Video.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("MediaStore refused to create $displayName")
    resolver.openOutputStream(uri)!!.use { out ->
        instrumentation.context.assets.open(asset).use { it.copyTo(out) }
    }
    resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
    return uri
}

@Suppress("DEPRECATION") // getExternalStoragePublicDirectory is deprecated from API 29; this path only runs below it.
private fun copyWithShellAndScan(displayName: String, asset: String): Uri {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val target = instrumentation.targetContext

    // The shell cannot read the app's private files, but it can read the app's own folder on shared storage, which the
    // app may write without any permission.
    val staging = File(checkNotNull(target.getExternalFilesDir(null)) { "No shared storage on this device" }, displayName)
    instrumentation.context.assets.open(asset).use { input -> staging.outputStream().use { input.copyTo(it) } }

    val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), FOLDER)
    val destination = File(folder, displayName)
    try {
        // On API 26 UiAutomation split the command on spaces and ran it without a shell (quoted paths were taken
        // literally and nothing was copied), and the error output did not come back either. So: no quotes, and the
        // result is checked below instead of read from the output. Neither path holds a space.
        runShell("mkdir -p ${folder.path}")
        runShell("cp ${staging.path} ${destination.path}")
        check(destination.length() == staging.length() && staging.length() > 0) {
            "The shell did not copy $displayName to ${destination.path} (found ${destination.length()} of ${staging.length()} bytes)"
        }
    } finally {
        staging.delete()
    }
    return scan(target, destination)
}

private fun scan(context: android.content.Context, file: File): Uri {
    val result = AtomicReference<Uri?>()
    val done = CountDownLatch(1)
    MediaScannerConnection.scanFile(context, arrayOf(file.path), arrayOf("video/mp4")) { _, uri ->
        result.set(uri)
        done.countDown()
    }
    check(done.await(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        "The media scanner did not finish indexing ${file.path} within $SCAN_TIMEOUT_SECONDS s"
    }
    return result.get() ?: error("The media scanner did not index ${file.path}")
}

private const val FOLDER = "Checks"
private const val SCAN_TIMEOUT_SECONDS = 20L
