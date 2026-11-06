package org.aptura.player.data.scanner

/** MediaStore returned no cursor for the video query. The scan failed; it must not be read as an empty library. */
class MediaStoreUnavailableException : RuntimeException("MediaStore returned no cursor for the video query")
