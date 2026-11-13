@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.core.media

import android.content.Context
import android.net.Uri
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.ssa.SsaParser
import androidx.media3.extractor.text.subrip.SubripParser
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Exercises all committed corpus fixtures under `fixtures/` (manifest, subtitle encodings,
 * multi-script RTL/CJK/Indic samples, ASS fidelity classes, adversarial SRT edge cases,
 * bait charset files, sidecar subtitle, and pinned seek targets) through Aptura's production
 * validators/normalizers and Media3 subtitle parsers (QA-07).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CommittedCorpusFixturesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun fixtureFile(relativePath: String): File {
        val normalized = relativePath.replace('\\', File.separatorChar).replace('/', File.separatorChar)
        val direct = File("fixtures", normalized)
        if (direct.exists()) return direct
        val parent = File("..", "fixtures${File.separator}$normalized")
        if (parent.exists()) return parent
        error("Fixture not found for $relativePath from user.dir=${System.getProperty("user.dir")}")
    }

    private fun parseSubtitles(parser: SubtitleParser, bytes: ByteArray): List<CuesWithTiming> {
        val out = mutableListOf<CuesWithTiming>()
        parser.parse(bytes, SubtitleParser.OutputOptions.allCues()) { cues ->
            out.add(cues)
        }
        return out
    }

    private fun validateAndReadNormalizedBytes(file: File): Pair<SubtitleValidationResult.Valid, ByteArray> {
        val result = SubtitleFileValidator.validate(context, Uri.fromFile(file))
        assertTrue(
            "Expected ${file.name} to validate cleanly but got $result",
            result is SubtitleValidationResult.Valid
        )
        val valid = result as SubtitleValidationResult.Valid
        val uri = valid.validatedUri ?: Uri.fromFile(file)
        val bytes = when (uri.scheme) {
            "file", null -> File(requireNotNull(uri.path)).readBytes()
            else -> context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        }
        return valid to bytes
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun splitCsvLine(line: String): List<String> {
        val cols = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    cols.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        cols.add(current.toString())
        return cols
    }

    @Test
    fun manifestCsv_hasValidSchemaAndMatchesAllCommittedFixtureDigests() {
        val manifest = fixtureFile("manifest.csv")
        val lines = manifest.readLines(StandardCharsets.UTF_8).filter { it.isNotBlank() }
        assertTrue("Expected >1000 manifest rows", lines.size > 1000)

        assertEquals(
            "id,cell,properties,duration_s,size_bytes,sha256,source_type,source_ref,license_note,gen_script_ref,pinned,notes",
            lines.first().trim()
        )

        val shaByPath = mutableMapOf<String, Pair<Long, String>>()
        val hexRegex = Regex("^[0-9a-f]{64}$")

        for (line in lines.drop(1)) {
            val cols = splitCsvLine(line)
            assertEquals("Manifest column count mismatch on row: $line", 12, cols.size)
            assertTrue("Blank id on row: $line", cols[0].isNotBlank())
            assertTrue("Blank cell on row: $line", cols[1].isNotBlank())
            val sizeBytes = cols[4].toLongOrNull()
            assertNotNull("Invalid size_bytes '${cols[4]}' on row ${cols[0]}", sizeBytes)
            assertTrue("Size must be >= 0 for ${cols[0]}", sizeBytes!! >= 0L)
            assertTrue("Invalid sha256 '${cols[5]}' on row ${cols[0]}", hexRegex.matches(cols[5]))
            assertTrue("Missing license_note on row ${cols[0]}", cols[8].isNotBlank())

            val pathToken = cols[2].substringAfter("path=", "").trim()
            if (pathToken.isNotEmpty()) {
                val unixPath = pathToken.replace('\\', '/')
                shaByPath[unixPath] = sizeBytes to cols[5]
            }
        }

        val committedFixtures = listOf(
            "a_matrix/a_A12_01_video.srt",
            "inst/inst_seek_pinned_positions.json",
            "adv_sub/adversarial/adv_sub_10k_char_line.srt",
            "adv_sub/adversarial/adv_sub_cue_beyond_duration.srt",
            "adv_sub/adversarial/adv_sub_malformed_tags.srt",
            "adv_sub/adversarial/adv_sub_negative_and_zero_timing.srt",
            "adv_sub/adversarial/adv_sub_overlapping_cues.srt",
            "adv_sub/adversarial/adv_sub_zero_length_cue.srt",
            "adv_sub/bait/adv_sub_bait_sjis_nobom.srt",
            "adv_sub/bait/adv_sub_bait_win1252_as_latin1.srt",
            "adv_sub/encodings/adv_sub_enc_gb18030.srt",
            "adv_sub/encodings/adv_sub_enc_koi8_r.srt",
            "adv_sub/encodings/adv_sub_enc_shift_jis.srt",
            "adv_sub/encodings/adv_sub_enc_utf16_be.srt",
            "adv_sub/encodings/adv_sub_enc_utf16_le.srt",
            "adv_sub/encodings/adv_sub_enc_utf32_be.srt",
            "adv_sub/encodings/adv_sub_enc_utf32_le.srt",
            "adv_sub/encodings/adv_sub_enc_utf8.srt",
            "adv_sub/encodings/adv_sub_enc_utf8_bom.srt",
            "adv_sub/encodings/adv_sub_enc_windows_1252.srt",
            "adv_sub/scripts/adv_sub_script_arabic_rtl.srt",
            "adv_sub/scripts/adv_sub_script_cjk_chinese.srt",
            "adv_sub/scripts/adv_sub_script_cjk_japanese.srt",
            "adv_sub/scripts/adv_sub_script_cjk_korean.srt",
            "adv_sub/scripts/adv_sub_script_devanagari.srt",
            "adv_sub/scripts/adv_sub_script_hebrew_rtl.srt",
            "adv_sub/scripts/adv_sub_script_latin.srt",
            "adv_sub/scripts/adv_sub_script_thai.srt",
            "adv_sub/ass_fidelity/adv_sub_ass_E1_timing_integrity.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_E2_line_breaks.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_E3_basic_inline_styling.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_E4_positioning.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_E5_simultaneous_cues.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_F1_motion_fade.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_F2_karaoke.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_F3_rotation.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_F4_blur_edges.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_F5_transform_anim.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_F6_alpha_colors.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_F7_wrap_tolerance.ass",
            "adv_sub/ass_fidelity/adv_sub_ass_X1_vector_drawing.ass"
        )

        for (rel in committedFixtures) {
            val entry = shaByPath[rel]
            assertNotNull("Committed fixture missing from manifest.csv: $rel", entry)
            val file = fixtureFile(rel)
            assertTrue("Committed fixture missing on disk: $rel", file.exists())
            val rawBytes = file.readBytes()
            val candidateHashes = buildSet {
                add(sha256Hex(rawBytes))
                if (rawBytes.size.toLong() != entry!!.first) {
                    // manifest.csv was compiled on Windows (CRLF for UTF-8 text files) while Git stores LF in the index.
                    val text = String(rawBytes, StandardCharsets.UTF_8).replace("\r\n", "\n")
                    add(sha256Hex(text.toByteArray(StandardCharsets.UTF_8)))
                    add(sha256Hex(text.replace("\n", "\r\n").toByteArray(StandardCharsets.UTF_8)))
                }
            }
            assertTrue(
                "SHA-256 mismatch for committed fixture $rel: expected ${entry!!.second}, got $candidateHashes",
                entry.second in candidateHashes
            )
        }
    }

    @Test
    fun encodingsCorpus_allTenFilesValidateNormalizeAndParseInMedia3() {
        val expectedPhrases = mapOf(
            "adv_sub/encodings/adv_sub_enc_utf8.srt" to "Café, Noël, façade",
            "adv_sub/encodings/adv_sub_enc_utf8_bom.srt" to "UTF-8 with BOM: Aptura Video Player test",
            "adv_sub/encodings/adv_sub_enc_utf16_le.srt" to "UTF-16 Little Endian",
            "adv_sub/encodings/adv_sub_enc_utf16_be.srt" to "UTF-16 Big Endian",
            "adv_sub/encodings/adv_sub_enc_utf32_le.srt" to "UTF-32 Little Endian",
            "adv_sub/encodings/adv_sub_enc_utf32_be.srt" to "UTF-32 Big Endian",
            "adv_sub/encodings/adv_sub_enc_windows_1252.srt" to "Coût: €100",
            "adv_sub/encodings/adv_sub_enc_shift_jis.srt" to "Shift-JIS 日本語字幕テスト",
            "adv_sub/encodings/adv_sub_enc_gb18030.srt" to "GB18030 中文字幕测试",
            "adv_sub/encodings/adv_sub_enc_koi8_r.srt" to "KOI8-R Русский субтитр"
        )

        for ((rel, phrase) in expectedPhrases) {
            val file = fixtureFile(rel)
            val (valid, bytes) = validateAndReadNormalizedBytes(file)
            assertEquals("application/x-subrip", valid.mimeType)

            val utf8Text = String(bytes, StandardCharsets.UTF_8)
            assertFalse("BOM leaked in $rel", utf8Text.startsWith("\uFEFF"))
            assertFalse("Replacement char leaked in $rel", utf8Text.contains('\uFFFD'))

            val cues = parseSubtitles(SubripParser(), bytes)
            assertTrue("Expected parsed cues for $rel", cues.isNotEmpty())
            val joined = cues.flatMap { it.cues }.mapNotNull { it.text?.toString() }.joinToString("\n")
            assertTrue("Missing expected text '$phrase' in parsed cues for $rel (got: $joined)", joined.contains(phrase))
        }
    }

    @Test
    fun baitCorpus_wrongCharsetFixturesRecoverViaAutoDetectionAndExplicitOverride() {
        val cacheDir = tempFolder.newFolder("bait_cache")

        val win1252Bait = fixtureFile("adv_sub/bait/adv_sub_bait_win1252_as_latin1.srt")
        val (_, winBytes) = validateAndReadNormalizedBytes(win1252Bait)
        val winCues = parseSubtitles(SubripParser(), winBytes).flatMap { it.cues }
            .mapNotNull { it.text?.toString() }.joinToString("\n")
        assertTrue("Expected cp1252 bait text decoded cleanly, got: $winCues", winCues.contains("Hôtellerie & Café spécialités — coût €45"))

        val explicitWin = SubtitleCharsetNormalizer.normalizeToUtf8(
            win1252Bait,
            cacheDir,
            overrideCharset = Charset.forName("windows-1252")
        ).readText(StandardCharsets.UTF_8)
        assertTrue(explicitWin.contains("coût €45"))

        val sjisBait = fixtureFile("adv_sub/bait/adv_sub_bait_sjis_nobom.srt")
        val (_, sjisBytes) = validateAndReadNormalizedBytes(sjisBait)
        val sjisCues = parseSubtitles(SubripParser(), sjisBytes).flatMap { it.cues }
            .mapNotNull { it.text?.toString() }.joinToString("\n")
        assertTrue("Expected Shift-JIS bait text decoded cleanly, got: $sjisCues", sjisCues.contains("ベイト字幕: 文字化け復旧テスト (FR-SUB-005)"))

        val explicitSjis = SubtitleCharsetNormalizer.normalizeToUtf8(
            sjisBait,
            cacheDir,
            overrideCharset = Charset.forName("Shift_JIS")
        ).readText(StandardCharsets.UTF_8)
        assertTrue(explicitSjis.contains("文字化け復旧テスト"))
    }

    @Test
    fun scriptsCorpus_allEightScriptsValidateAndParseCleanly() {
        val expectedByScript = mapOf(
            "adv_sub/scripts/adv_sub_script_latin.srt" to "The quick brown fox",
            "adv_sub/scripts/adv_sub_script_arabic_rtl.srt" to "مرحبا بكم في مشغل الفيديو ابتورا",
            "adv_sub/scripts/adv_sub_script_hebrew_rtl.srt" to "שלום וברוכים הבאים לנגן הווידאו אפטורה",
            "adv_sub/scripts/adv_sub_script_devanagari.srt" to "आपतुरा वीडियो प्लेयर में आपका स्वागत है",
            "adv_sub/scripts/adv_sub_script_thai.srt" to "ยินดีต้อนรับสู่ Aptura เครื่องเล่นวิดีโอ",
            "adv_sub/scripts/adv_sub_script_cjk_chinese.srt" to "沉浸式无损画质视听享受",
            "adv_sub/scripts/adv_sub_script_cjk_japanese.srt" to "映画館級の色彩美と音響体験",
            "adv_sub/scripts/adv_sub_script_cjk_korean.srt" to "최고 수준의 로컬 미디어 재생기"
        )

        for ((rel, snippet) in expectedByScript) {
            val file = fixtureFile(rel)
            val (valid, bytes) = validateAndReadNormalizedBytes(file)
            assertEquals("application/x-subrip", valid.mimeType)
            val batches = parseSubtitles(SubripParser(), bytes)
            assertEquals("Expected 1 cue timing window for $rel", 1, batches.size)
            assertEquals(1_000_000L, batches.first().startTimeUs)
            assertEquals(4_000_000L, batches.first().durationUs)
            val text = batches.first().cues.joinToString("\n") { it.text?.toString().orEmpty() }
            assertTrue("Expected script text '$snippet' in $rel (got '$text')", text.contains(snippet))
        }
    }

    @Test
    fun assFidelityCorpus_allThirteenProbesValidateAndParseThroughSsaParser() {
        val probes = mapOf(
            "adv_sub/ass_fidelity/adv_sub_ass_E1_timing_integrity.ass" to listOf("E1: Parse integrity test"),
            "adv_sub/ass_fidelity/adv_sub_ass_E2_line_breaks.ass" to listOf("E2: First line", "Second line"),
            "adv_sub/ass_fidelity/adv_sub_ass_E3_basic_inline_styling.ass" to listOf("E3:", "Bold", "Italic", "Cyan"),
            "adv_sub/ass_fidelity/adv_sub_ass_E4_positioning.ass" to listOf("E4: Top-left positioned cue"),
            "adv_sub/ass_fidelity/adv_sub_ass_E5_simultaneous_cues.ass" to listOf("E5: Bottom subtitle cue", "E5: Top subtitle cue"),
            "adv_sub/ass_fidelity/adv_sub_ass_F1_motion_fade.ass" to listOf("F1: Moving cue"),
            "adv_sub/ass_fidelity/adv_sub_ass_F2_karaoke.ass" to listOf("Aptura Precision Player"),
            "adv_sub/ass_fidelity/adv_sub_ass_F3_rotation.ass" to listOf("F3: Rotated text"),
            "adv_sub/ass_fidelity/adv_sub_ass_F4_blur_edges.ass" to listOf("F4: Heavy blur"),
            "adv_sub/ass_fidelity/adv_sub_ass_F5_transform_anim.ass" to listOf("F5: Transform scale animation"),
            "adv_sub/ass_fidelity/adv_sub_ass_F6_alpha_colors.ass" to listOf("F6: Secondary alpha"),
            "adv_sub/ass_fidelity/adv_sub_ass_F7_wrap_tolerance.ass" to listOf("F7: Long unbroken cue"),
            "adv_sub/ass_fidelity/adv_sub_ass_X1_vector_drawing.ass" to listOf("X1: Vector box drawing test")
        )

        for ((rel, expectedSnippets) in probes) {
            val file = fixtureFile(rel)
            val (valid, bytes) = validateAndReadNormalizedBytes(file)
            assertEquals("text/x-ssa", valid.mimeType)
            assertEquals("SubStation Alpha", valid.formatDescription)

            val batches = parseSubtitles(SsaParser(), bytes)
            assertTrue("Expected parsed SSA cues for $rel", batches.isNotEmpty())
            val text = batches.flatMap { it.cues }.mapNotNull { it.text?.toString() }.joinToString("\n")
            for (snippet in expectedSnippets) {
                assertTrue("Expected '$snippet' in parsed SSA output for $rel (got '$text')", text.contains(snippet))
            }
        }
    }

    @Test
    fun adversarialAndSidecarSubtitles_validateAndParseWithoutCrashing() {
        val parser = SubripParser()

        // 1. 10k char line
        run {
            val file = fixtureFile("adv_sub/adversarial/adv_sub_10k_char_line.srt")
            val (_, bytes) = validateAndReadNormalizedBytes(file)
            val cues = parseSubtitles(parser, bytes).flatMap { it.cues }
            assertEquals(1, cues.size)
            assertTrue("Expected >=10,000 characters in parsed cue", cues.first().text!!.length >= 10_000)
        }

        // 2. Overlapping cues
        run {
            val file = fixtureFile("adv_sub/adversarial/adv_sub_overlapping_cues.srt")
            val (_, bytes) = validateAndReadNormalizedBytes(file)
            val text = parseSubtitles(parser, bytes).flatMap { it.cues }
                .mapNotNull { it.text?.toString() }.joinToString("\n")
            assertTrue(text.contains("Overlapping cue 1"))
            assertTrue(text.contains("Overlapping cue 2"))
            assertTrue(text.contains("Overlapping cue 3"))
        }

        // 3. Zero-length cue followed by normal cue
        run {
            val file = fixtureFile("adv_sub/adversarial/adv_sub_zero_length_cue.srt")
            val (_, bytes) = validateAndReadNormalizedBytes(file)
            val text = parseSubtitles(parser, bytes).flatMap { it.cues }
                .mapNotNull { it.text?.toString() }.joinToString("\n")
            assertTrue(text.contains("Normal cue following zero duration"))
        }

        // 4. Malformed HTML tags
        run {
            val file = fixtureFile("adv_sub/adversarial/adv_sub_malformed_tags.srt")
            val (_, bytes) = validateAndReadNormalizedBytes(file)
            val batches = parseSubtitles(parser, bytes)
            assertEquals(1, batches.size)
            val text = batches.first().cues.first().text?.toString().orEmpty()
            assertTrue("Expected surviving text after malformed tag sanitization, got '$text'", text.contains("Unclosed and malformed tags"))
        }

        // 5. Negative and zero timing
        run {
            val file = fixtureFile("adv_sub/adversarial/adv_sub_negative_and_zero_timing.srt")
            val (_, bytes) = validateAndReadNormalizedBytes(file)
            val batches = parseSubtitles(parser, bytes)
            assertTrue(batches.any { it.startTimeUs == 0L })
            val text = batches.flatMap { it.cues }.mapNotNull { it.text?.toString() }.joinToString("\n")
            assertTrue(text.contains("Cue at exact t=0"))
        }

        // 6. Cue beyond typical media duration (24 hours)
        run {
            val file = fixtureFile("adv_sub/adversarial/adv_sub_cue_beyond_duration.srt")
            val (_, bytes) = validateAndReadNormalizedBytes(file)
            val batches = parseSubtitles(parser, bytes)
            assertEquals(1, batches.size)
            assertEquals(86_400_000_000L, batches.first().startTimeUs)
            assertEquals(5_000_000L, batches.first().durationUs)
        }

        // 7. Sidecar subtitle in a_matrix
        run {
            val file = fixtureFile("fixtures/a_matrix/a_A12_01_video.srt".removePrefix("fixtures/"))
            val (valid, bytes) = validateAndReadNormalizedBytes(file)
            assertEquals("application/x-subrip", valid.mimeType)
            val batches = parseSubtitles(parser, bytes)
            assertEquals(1, batches.size)
            assertEquals("A12 Sidecar Subtitle Cue", batches.first().cues.first().text?.toString()?.trim())
        }
    }

    @Test
    fun pinnedSeekPositionsJson_containsValidAscendingTargetsAcrossAllZones() {
        val jsonFile = fixtureFile("inst/inst_seek_pinned_positions.json")
        val root = JSONObject(jsonFile.readText(StandardCharsets.UTF_8))
        val targets = root.getJSONArray("targets")
        assertEquals(8, targets.length())

        val zonesSeen = mutableSetOf<String>()
        val targetTypesSeen = mutableSetOf<String>()
        var previousUs = -1L

        for (i in 0 until targets.length()) {
            val item = targets.getJSONObject(i)
            val posUs = item.getLong("position_us")
            val posS = item.getDouble("position_s")
            val targetType = item.getString("target_type")
            val zone = item.getString("zone")

            assertTrue("Seek positions must strictly ascend", posUs > previousUs)
            assertEquals("Microsecond conversion mismatch at index $i", (posS * 1_000_000.0).toLong(), posUs)
            previousUs = posUs
            zonesSeen.add(zone)
            targetTypesSeen.add(targetType)
        }

        assertEquals(setOf("near-start", "mid", "near-end"), zonesSeen)
        assertEquals(setOf("keyframe", "non-keyframe"), targetTypesSeen)
    }
}
