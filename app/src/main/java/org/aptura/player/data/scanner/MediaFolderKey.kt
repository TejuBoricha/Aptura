package org.aptura.player.data.scanner

/**
 * Which folder a video is in, as a key that tells two folders apart.
 *
 * `MediaStore`'s bucket name is only the folder's own name. `DCIM/Camera` on the phone and `DCIM/Camera` on an SD card,
 * or two apps' `Download` folders, therefore came out as one folder called "Camera" or "Download" with their videos mixed
 * together, and a count, a cover and a swipe that were wrong. Photos, Files and the common gallery apps identify a folder
 * by where it is (this is what `BUCKET_ID` hashes) and show its name, adding the parent only where two names collide.
 * This does the same: the key is the folder's path on its storage volume, and the names people read are made from it
 * (`folderLabels`).
 *
 * The documented way to get the location is `RELATIVE_PATH` with `VOLUME_NAME` (Android 10 and newer, the only API that
 * is not deprecated); before Android 10 it is the file path. When neither is available the bucket name is used, which
 * is what every folder was before.
 */
object MediaFolderKey {

    private const val PRIMARY_VOLUME = "external_primary"
    private const val PRIMARY_ROOT = "/storage/emulated/0"

    /** Other names for the phone's own storage, as older file paths spell it. */
    private val primaryAliases = listOf("/storage/self/primary", "/storage/emulated/legacy", "/mnt/sdcard", "/sdcard")

    fun of(volumeName: String?, relativePath: String?, filePath: String?, bucketName: String?): String =
        fromRelativePath(volumeName, relativePath)
            ?: fromFilePath(filePath)
            ?: bucketName?.takeIf { it.isNotBlank() }
            ?: "Internal Storage"

    /** `RELATIVE_PATH` ("DCIM/Camera/") on the volume `VOLUME_NAME` ("external_primary", or an SD card's id). */
    internal fun fromRelativePath(volumeName: String?, relativePath: String?): String? {
        // Null means the platform did not say (an older row, or the column is not there); "" means the volume's root.
        relativePath ?: return null
        val root = if (volumeName.isNullOrBlank() || volumeName == PRIMARY_VOLUME) PRIMARY_ROOT else "/storage/${volumeName.uppercase()}"
        val relative = relativePath.trim('/')
        return if (relative.isEmpty()) root else "$root/$relative"
    }

    /** The folder part of a file's absolute path (before Android 10, where `RELATIVE_PATH` does not exist). */
    internal fun fromFilePath(filePath: String?): String? {
        if (filePath.isNullOrBlank() || !filePath.startsWith("/")) return null
        val folder = filePath.substringBeforeLast('/', missingDelimiterValue = "")
        if (folder.isBlank()) return null
        val alias = primaryAliases.firstOrNull { folder == it || folder.startsWith("$it/") }
        return if (alias != null) PRIMARY_ROOT + folder.removePrefix(alias) else folder
    }
}
