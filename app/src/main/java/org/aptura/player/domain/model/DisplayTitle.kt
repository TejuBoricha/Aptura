package org.aptura.player.domain.model

private val VIDEO_EXTENSION = Regex("""\.(mp4|mkv|webm|avi|mov|ts|m4v|3gp)$""", RegexOption.IGNORE_CASE)

/** Human-readable title for a raw file/media title: no video extension, underscores become spaces. */
fun cleanDisplayTitle(rawTitle: String): String {
    if (rawTitle.isBlank()) return "Untitled"
    return rawTitle.replace(VIDEO_EXTENSION, "").replace('_', ' ').trim()
}
