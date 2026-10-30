package org.aptura.player.core.media

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

/**
 * Finds where a video really lives on storage, from what Android itself says about it.
 *
 * The Details screen used to *guess* a path from the folder's display name, which was wrong for any nested folder
 * and for the file extension (and the Copy button copied the wrong string). A guess is worse than "Unknown".
 */
object MediaPathResolver {

    /**
     * @return the absolute path (primary storage), a volume-relative path for other volumes, or null when the
     * location cannot be determined (e.g. a Storage Access Framework document).
     */
    fun resolve(
        context: Context,
        uriString: String,
        primaryStorageRoot: () -> String = { Environment.getExternalStorageDirectory().absolutePath }
    ): String? {
        val uri = try { uriString.toUri() } catch (_: Throwable) { return null }
        return when {
            uri.scheme == "file" -> uri.path?.takeIf { it.isNotBlank() }
            uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY -> fromMediaStore(context, uri, primaryStorageRoot)
            else -> null
        }
    }

    private fun fromMediaStore(context: Context, uri: Uri, primaryStorageRoot: () -> String): String? {
        val hasRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        @Suppress("DEPRECATION")
        val projection = if (hasRelativePath) {
            arrayOf(
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.VOLUME_NAME,
                MediaStore.MediaColumns.DATA
            )
        } else {
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA)
        }

        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                fun string(column: String): String? =
                    cursor.getColumnIndex(column).takeIf { it >= 0 }?.let { cursor.getString(it) }?.takeIf { it.isNotBlank() }

                val displayName = string(MediaStore.MediaColumns.DISPLAY_NAME)
                val relativePath = if (hasRelativePath) string(MediaStore.MediaColumns.RELATIVE_PATH) else null
                val volume = if (hasRelativePath) string(MediaStore.MediaColumns.VOLUME_NAME) else null
                @Suppress("DEPRECATION")
                val dataPath = string(MediaStore.MediaColumns.DATA)

                when {
                    relativePath != null && displayName != null && (volume == null || volume == MediaStore.VOLUME_EXTERNAL_PRIMARY) ->
                        primaryStorageRoot().trimEnd('/') + "/" + relativePath.trim('/') + "/" + displayName
                    dataPath != null -> dataPath
                    relativePath != null && displayName != null -> relativePath.trim('/') + "/" + displayName
                    else -> null
                }
            }
        } catch (_: Throwable) {
            null
        }
    }
}
