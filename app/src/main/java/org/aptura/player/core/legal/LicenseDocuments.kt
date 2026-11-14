package org.aptura.player.core.legal

import android.content.Context

/** One licence text the app shows. [asset] is a file in `assets/open-source-licenses/` (see `collectLicenseAssets` in app/build.gradle.kts). */
data class LicenseDocument(val id: String, val title: String, val summary: String, val asset: String)

/**
 * The texts behind Settings > Open-source licences.
 *
 * They are packaged from the same sources the build uses (the repository's NOTICE and LICENSE, and the licence texts that
 * travel inside the pinned FFmpeg bundle), so what the app shows cannot drift from what it links. LGPL-2.1 section 6 asks for
 * a notice with each copy that the library is used and a copy of the licence; this is where both live inside the app.
 */
object LicenseDocuments {
    const val ASSET_DIRECTORY = "open-source-licenses"

    /** Shown above the list. The first sentence is the notice FFmpeg's compliance checklist asks for, in its own words. */
    const val FFMPEG_NOTICE =
        "This software uses libraries from the FFmpeg project (https://ffmpeg.org) under the GNU Lesser General Public " +
            "License, version 2.1 (LGPLv2.1). FFmpeg is a trademark of Fabrice Bellard, originator of the FFmpeg project. " +
            "The libraries are separate files inside this app; the exact source they were built from, the build records and " +
            "the licence text are published at the link below."

    /** Where the FFmpeg source, build records and licence texts are published (the release the build links). */
    const val FFMPEG_SOURCE_URL = "https://github.com/TejuBoricha/Aptura/releases/tag/ffmpeg-audio-n9.0.2-r2"

    val all: List<LicenseDocument> = listOf(
        LicenseDocument(
            id = "notice",
            title = "Notices",
            summary = "The open-source components in Aptura, who wrote them and under which licence",
            asset = "NOTICE.txt"
        ),
        LicenseDocument(
            id = "aptura",
            title = "Aptura",
            summary = "Apache License 2.0",
            asset = "LICENSE-Aptura-Apache-2.0.txt"
        ),
        LicenseDocument(
            id = "ffmpeg-lgpl",
            title = "FFmpeg",
            summary = "GNU Lesser General Public License, version 2.1",
            asset = "FFmpeg-COPYING.LGPLv2.1.txt"
        ),
        LicenseDocument(
            id = "ffmpeg-overview",
            title = "FFmpeg licensing overview",
            summary = "How FFmpeg's licences apply, from the FFmpeg project",
            asset = "FFmpeg-LICENSE.md"
        ),
        LicenseDocument(
            id = "androidx-media",
            title = "AndroidX Media",
            summary = "Apache License 2.0",
            asset = "AndroidX-Media-LICENSE-Apache-2.0.txt"
        ),
        LicenseDocument(
            id = "protobuf",
            title = "Protocol Buffers",
            summary = "BSD 3-Clause License, used by Jetpack DataStore",
            asset = "Protobuf-LICENSE-BSD-3-Clause.txt"
        )
    )

    /** The text of [document], read from the app's assets. Throws if the asset is missing: a build without its licences is broken. */
    fun read(context: Context, document: LicenseDocument): String =
        context.assets.open("$ASSET_DIRECTORY/${document.asset}").bufferedReader(Charsets.UTF_8).use { it.readText() }
}

/**
 * Turns a licence text into paragraphs that read well on a narrow screen.
 *
 * The texts are hard-wrapped at about 78 columns, which wraps raggedly at phone width. A line break is kept where it means
 * something (the next line is indented, or the line before it is short, as a heading or the end of a sentence is) and dropped
 * where it only wraps a sentence. Only whitespace changes: [words] of the input and of the output are identical, and a test
 * checks that for every shipped document.
 */
object LicenseText {
    private const val SHORT_LINE = 45

    fun paragraphs(text: String): List<String> =
        text.replace("\r\n", "\n").split(Regex("\n[ \t]*\n+")).map { reflow(it) }.filter { it.isNotBlank() }

    private fun reflow(paragraph: String): String {
        val lines = paragraph.trimEnd().split('\n')
        val out = StringBuilder()
        lines.forEachIndexed { index, line ->
            if (index == 0) {
                out.append(line.trimEnd())
                return@forEachIndexed
            }
            val previous = lines[index - 1]
            val keepBreak = line.firstOrNull()?.isWhitespace() == true || previous.trim().length < SHORT_LINE
            out.append(if (keepBreak) "\n" else " ").append(if (keepBreak) line.trimEnd() else line.trim())
        }
        return out.toString().trimStart('\n')
    }

    /** The words of [text], in order. Used to prove that reflowing changed whitespace and nothing else. */
    fun words(text: String): List<String> = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
}
