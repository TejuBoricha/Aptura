package org.aptura.player.core.legal

import org.aptura.player.core.invariants.InvariantFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

/**
 * LGPL-2.1 section 6 asks for a notice with each copy that the library is used, and for a copy of the licence. These tests hold
 * the Open-source licences screen to the texts the build packages: every document it lists exists and says what it claims to,
 * nothing is packaged that the screen cannot show, and reflowing a text for a narrow screen changes whitespace only.
 */
class LicenseDocumentsTest {

    private val assetDirectory get() = File(InvariantFiles.licenseAssets, LicenseDocuments.ASSET_DIRECTORY)

    private fun text(document: LicenseDocument) = File(assetDirectory, document.asset).readText(Charsets.UTF_8)

    private fun document(id: String) = LicenseDocuments.all.single { it.id == id }

    // ---- the real thing --------------------------------------------------------------------------------------------

    @Test
    fun everyListedDocument_exists_andIsNotEmpty() {
        for (document in LicenseDocuments.all) {
            val file = File(assetDirectory, document.asset)
            assertTrue("${document.title}: ${file.path} is missing from the packaged licence assets", file.isFile)
            assertTrue("${document.title}: ${file.path} is nearly empty", file.length() > 500)
        }
    }

    @Test
    fun theListedDocuments_areTheWholePackagedSet() {
        // A licence text that is packaged but never shown does not reach the person; one that is listed but not packaged crashes.
        val packaged = assetDirectory.listFiles().orEmpty().map { it.name }.toSet()

        assertEquals(LicenseDocuments.all.map { it.asset }.toSet(), packaged)
        assertEquals("document ids must be unique", LicenseDocuments.all.size, LicenseDocuments.all.map { it.id }.toSet().size)
    }

    @Test
    fun eachDocument_saysWhatItClaimsToBe() {
        val lgpl = text(document("ffmpeg-lgpl"))
        assertTrue(lgpl.contains("GNU LESSER GENERAL PUBLIC LICENSE"))
        assertTrue(lgpl.contains("Version 2.1"))
        // The section the project relies on: distribution under terms that allow modification and relinking.
        assertTrue(lgpl.lowercase().contains("work that uses the library"))

        for (id in listOf("aptura", "androidx-media")) {
            val apache = text(document(id))
            assertTrue("$id is not the Apache License", apache.contains("Apache License") && apache.contains("Version 2.0"))
        }

        val bsd = text(document("protobuf"))
        assertTrue(bsd.contains("Copyright 2008 Google Inc."))
        assertTrue(bsd.contains("Redistributions in binary form must reproduce the above"))

        val notice = text(document("notice"))
        assertTrue(notice.contains("FFmpeg"))
        assertTrue(notice.contains("GNU Lesser General Public License"))
        assertFalse("NOTICE still describes the replaced prebuilt decoder", notice.contains("Jellyfin"))
    }

    @Test
    fun theFfmpegNotice_saysFfmpegIsUsedUnderTheLgpl_andLinksToTheReleaseTheBuildLinks() {
        val notice = LicenseDocuments.FFMPEG_NOTICE
        assertTrue(notice.contains("FFmpeg"))
        assertTrue(notice.contains("GNU Lesser General Public License"))
        assertTrue(notice.contains("LGPLv2.1"))

        val pin = Properties().apply { listOf("gradle/ffmpeg-bundle.properties", "../gradle/ffmpeg-bundle.properties").map(::File).first { it.isFile }.inputStream().use(::load) }
        val tagPage = LicenseDocuments.FFMPEG_SOURCE_URL
        assertTrue(tagPage.contains("/releases/tag/"))
        assertTrue(
            "the licence screen links to $tagPage but the build links ${pin.getProperty("url")}",
            pin.getProperty("url").startsWith(tagPage.replace("/releases/tag/", "/releases/download/") + "/")
        )
    }

    @Test
    fun reflowingEveryShippedDocument_changesWhitespaceOnly() {
        for (document in LicenseDocuments.all) {
            val original = text(document)
            val paragraphs = LicenseText.paragraphs(original)

            assertTrue("${document.title} produced no paragraphs", paragraphs.isNotEmpty())
            assertEquals(
                "${document.title}: reflowing changed the words of the licence text",
                LicenseText.words(original),
                LicenseText.words(paragraphs.joinToString("\n\n"))
            )
        }
    }

    // ---- fixtures: the reflow rules --------------------------------------------------------------------------------

    @Test
    fun fixture_aHardWrappedSentence_isJoined() {
        val wrapped = "This License Agreement applies to any software library or other program which\ncontains a notice placed by the copyright holder saying it may be distributed."

        assertEquals(
            listOf("This License Agreement applies to any software library or other program which contains a notice placed by the copyright holder saying it may be distributed."),
            LicenseText.paragraphs(wrapped)
        )
    }

    @Test
    fun fixture_indentedLines_andShortLines_keepTheirBreaks() {
        val entry = "Coil 3 (image loading)\n  Copyright Coil Contributors\n  Licensed under the Apache License, Version 2.0."

        assertEquals(listOf(entry), LicenseText.paragraphs(entry))
    }

    @Test
    fun fixture_blankLinesSplitParagraphs_andWindowsLineEndingsAreHandled() {
        assertEquals(listOf("one", "two"), LicenseText.paragraphs("one\r\n\r\n\r\ntwo\r\n"))
        assertEquals(emptyList<String>(), LicenseText.paragraphs("  \n\n \n"))
    }
}
