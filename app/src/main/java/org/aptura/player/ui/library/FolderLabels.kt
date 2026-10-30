package org.aptura.player.ui.library

/**
 * The names a person sees for folders.
 *
 * A folder is shown by its own name ("Movies"). Two folders can end the same way: every show has a "Season 1", and the
 * phone and its SD card both have a "DCIM/Camera". Identical chips and rows would leave no way to tell which is selected,
 * so a name that is shared gets what separates it: first the folder it is in ("Season 1 (Severance)"), then, if that is
 * the same too, the storage it is on ("Camera (DCIM, SD card)"). A name that is not shared is left alone.
 */
internal fun folderLabels(folders: Collection<String>): Map<String, String> {
    val names = folders.associateWith(::friendlyFolderName)
    val sharedNames = names.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    val withParent = names.mapValues { (folder, name) ->
        if (name in sharedNames) "$name (${parentFolderName(folder)})" else name
    }
    val sharedWithParent = withParent.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    val withStorage = withParent.mapValues { (folder, label) ->
        if (label in sharedWithParent) {
            "${names.getValue(folder)} (${parentFolderName(folder)}, ${folderPlace(folder).storageSuffix ?: "Internal Storage"})"
        } else {
            label
        }
    }
    // Two keys are never equal, so the full path always separates what is still the same (two SD cards, two chosen trees).
    val stillShared = withStorage.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    return withStorage.mapValues { (folder, label) ->
        if (label in stillShared) "${names.getValue(folder)} (${folder.trim('/')})" else label
    }
}

/** The folder one level up, or the storage the folder is on when it is at the top. */
internal fun parentFolderName(folder: String): String {
    val place = folderPlace(folder)
    return if (place.segments.size >= 2) place.segments[place.segments.size - 2] else place.storageSuffix ?: "Internal Storage"
}

private const val INTERNAL_STORAGE = "Internal Storage"
private const val SD_CARD = "SD card"

/** A folder key split into the storage it is on (when the key says) and the folders below that storage. */
internal class FolderPlace(val storage: String?, val segments: List<String>) {
    /** The storage's name as the end of a label, where room is short: "Camera (DCIM, Internal)" still fits a sidebar row. */
    val storageSuffix: String? get() = if (storage == INTERNAL_STORAGE) "Internal" else storage
}

/**
 * Keys from the device's media library are absolute paths on a storage volume (`/storage/emulated/0/DCIM/Camera`,
 * `/storage/1A2B-3C4D/DCIM/Camera`); keys from a folder the person added are a name and its subfolders. Only the first
 * kind names a storage.
 */
internal fun folderPlace(folder: String): FolderPlace {
    val segments = folder.split('/').filter { it.isNotBlank() }
    if (folder.startsWith("/storage/") && segments.size >= 2) {
        return if (segments[1] == "emulated" && segments.size >= 3) {
            FolderPlace(INTERNAL_STORAGE, segments.drop(3))
        } else {
            FolderPlace(SD_CARD, segments.drop(2))
        }
    }
    return FolderPlace(null, segments)
}
