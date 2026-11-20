package org.aptura.player.core.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles video frame capture and persistence to device storage (FEAT-13).
 * Operates offline-only using standard Scoped Storage MediaStore collections (DEC-011, DEC-012).
 */
object FrameCaptureCoordinator {

    suspend fun captureAndSaveFrame(
        context: Context,
        videoUri: Uri,
        positionMs: Long,
        videoTitle: String? = null,
        rotationDegrees: Int = 0,
        isMirrored: Boolean = false
    ): Uri? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val timeUs = (positionMs * 1000L).coerceAtLeast(0L)
            val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } else {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            } ?: return@withContext null

            val finalBitmap = transformBitmap(rawBitmap, rotationDegrees, isMirrored)
            saveBitmapToGallery(context, finalBitmap, videoTitle ?: "frame")
        } catch (_: Throwable) {
            null
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }

    fun transformBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int,
        isMirrored: Boolean
    ): Bitmap {
        if (rotationDegrees == 0 && !isMirrored) return bitmap
        val matrix = Matrix().apply {
            if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
            if (isMirrored) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        baseTitle: String
    ): Uri? {
        val sanitized = baseTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(30)
        val displayName = "Aptura_${sanitized}_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Aptura")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = context.contentResolver.insert(collection, values) ?: return null
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)) {
                    return null
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            return uri
        } catch (_: Throwable) {
            try { context.contentResolver.delete(uri, null, null) } catch (_: Throwable) {}
            return null
        }
    }
}
