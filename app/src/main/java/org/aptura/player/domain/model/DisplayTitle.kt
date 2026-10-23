package org.aptura.player.domain.model

private val VIDEO_EXTENSION = Regex(
    """\.(mp4|mkv|webm|avi|mov|ts|m4v|3gp|flv|wmv|asf|m2ts|mts|vob|ogv|divx|f4v)$""",
    RegexOption.IGNORE_CASE
)

/** Human-readable title for a raw file/media title: no video extension, underscores and dot separators become spaces (P2-04). */
fun cleanDisplayTitle(rawTitle: String): String {
    if (rawTitle.isBlank()) return "Untitled"
    val withoutExt = rawTitle.replace(VIDEO_EXTENSION, "").trim()
    var cleaned = withoutExt.replace('_', ' ')
    if (cleaned.contains('.') && !cleaned.contains(' ')) {
        // Dot-separated scene titles with no existing whitespace (e.g. "Show.S01E01")
        cleaned = cleaned.replace('.', ' ')
    } else if (cleaned.contains('.')) {
        // Titles with dots between word boundaries, preserving version/decimal numbers like 5.1 or v1.2
        cleaned = cleaned.replace(Regex("""(?<=\w)\.(?=[A-Za-z])"""), " ")
    }
    return cleaned.replace(Regex("""\s+"""), " ").trim()
}
