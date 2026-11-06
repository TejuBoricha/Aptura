package org.aptura.player.core.media

/**
 * Which storage volume a folder path is on, and whether a video stays on its volume when it is moved there
 * (audit DATA-02).
 *
 * A folder the library offers as a destination is named by its path, built from `MediaStore`'s `VOLUME_NAME` and
 * `RELATIVE_PATH` (see `MediaFolderKey`): `/storage/emulated/0/Movies` is on the phone's own storage and
 * `/storage/1A2B-3C4D/Movies` is on an SD card. Moving a video in place only rewrites its `RELATIVE_PATH`, which never
 * changes the volume. So a move to a folder on another volume has to copy the file and delete the original.
 */
internal object StorageVolumes {

    /** `MediaStore`'s name for the phone's own storage. */
    const val PRIMARY = "external_primary"

    private const val PRIMARY_ROOT = "/storage/emulated/0"

    /**
     * The volume [folderPath] is on: [PRIMARY] for the phone, otherwise the SD card's id in lower case, which is how
     * `MediaStore` spells `VOLUME_NAME` (the path spells it in upper case). Null when the path names no volume, such as
     * a relative folder (`Movies/`): that means "wherever the video already is".
     */
    fun ofFolderPath(folderPath: String): String? {
        val clean = folderPath.trim()
        return when {
            clean == PRIMARY_ROOT || clean.startsWith("$PRIMARY_ROOT/") -> PRIMARY
            clean.startsWith("/storage/") -> clean.split('/').filter { it.isNotBlank() }.getOrNull(1)?.lowercase()
            else -> null
        }
    }

    /**
     * True when a video on [sourceVolume] is still on its volume after being moved to [destinationPath].
     *
     * An unknown source volume (null or blank) is read as the phone's own storage, which is what `MediaFolderKey` does
     * with a missing volume. So an unknown video is moved in place only to a folder on the phone.
     */
    fun sameVolume(sourceVolume: String?, destinationPath: String): Boolean {
        val destination = ofFolderPath(destinationPath) ?: return true
        val source = sourceVolume?.takeIf { it.isNotBlank() }?.lowercase() ?: PRIMARY
        return source == destination
    }
}
