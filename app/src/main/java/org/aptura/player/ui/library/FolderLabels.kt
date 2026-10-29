package org.aptura.player.ui.library

/**
 * The names a person sees for folders.
 *
 * A folder path is shown by its last segment ("Movies"). Two folders can end the same way (every show has a "Season 1"),
 * and then the last segment alone would give two identical chips and two identical rows, with no way to tell which is
 * selected. For those the parent is added: "Season 1 (Severance)".
 */
internal fun folderLabels(folders: Collection<String>): Map<String, String> {
    val plain = folders.associateWith(::friendlyFolderName)
    val ambiguous = plain.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    return plain.mapValues { (folder, name) ->
        if (name in ambiguous) "$name (${parentFolderName(folder)})" else name
    }
}

/** The folder one level up, or "Internal Storage" when the folder is at the top. */
internal fun parentFolderName(folder: String): String {
    val segments = folder.trimEnd('/').split('/').filter { it.isNotBlank() }
    return if (segments.size >= 2) segments[segments.size - 2] else "Internal Storage"
}
