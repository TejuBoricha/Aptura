package org.aptura.player.domain.model

/*
 * How the library is laid out. These are the user's choices, kept in settings; what each one looks like is decided by
 * the library screen. Pure Kotlin, like everything in domain/.
 */

/** Videos as a grid of picture cards, or as rows with a small picture beside the text. */
enum class LibraryLayout { GRID, LIST }

/** One step of size for the pictures: how many fit across, and how much space they get. */
enum class LibraryDensity { COMPACT, COMFORTABLE, LARGE }

/** How much each video says about itself. */
enum class LibraryDetail {
    /** Just the title. */
    TITLE_ONLY,

    /** The title, the size and the folder. */
    STANDARD,

    /** Adds the picture quality and when the video was added. */
    DETAILED
}

/** The folder browser: rows with a cover picture, or a grid of cover cards. */
enum class FolderLayout { LIST, GRID }
