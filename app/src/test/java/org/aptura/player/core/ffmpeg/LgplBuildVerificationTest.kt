package org.aptura.player.core.ffmpeg

import org.aptura.player.core.invariants.InvariantFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The build records of one ABI of the FFmpeg bundle, parsed from the files the bundle ships next to the libraries
 * (`config.h`, `config_components.h`, `BUILD-INFO.txt`).
 *
 * An earlier version of this test asserted on a list of configure flags typed into the test itself, so it passed whatever
 * the app really linked (and the list still named AAC, AC-3 and DTS after the policy dropped them). These records are what
 * FFmpeg wrote when it was built, and the app build refuses a bundle whose checksum is not the pinned one, so the records
 * and the libraries cannot disagree.
 */
internal class FfmpegBuildRecord(val abi: String, config: String, components: String, info: String) {
    val license: String? = Regex("""#define FFMPEG_LICENSE "([^"]*)"""").find(config)?.groupValues?.get(1)
    val configuration: String = Regex("""#define FFMPEG_CONFIGURATION "(.*)"""").find(config)?.groupValues?.get(1).orEmpty()
    val commit: String? = Regex("""FFmpeg commit:\s*([0-9a-f]{40})""").find(info)?.groupValues?.get(1)
    private val configValues = Regex("""#define (CONFIG_[A-Z0-9_]+) (\d)""").findAll(config).associate { it.groupValues[1] to it.groupValues[2] }
    private val enabledComponents = Regex("""#define CONFIG_([A-Z0-9_]+) 1""").findAll(components).map { it.groupValues[1] }.toList()

    /** FFmpeg decoder names (`pcm_alaw`), from the lines `#define CONFIG_PCM_ALAW_DECODER 1`. */
    val decoders: Set<String> = enabledComponents.filter { it.endsWith("_DECODER") }.map { it.removeSuffix("_DECODER").lowercase() }.toSet()

    /** Everything else that was switched on besides decoders, such as encoders, muxers, protocols or hardware acceleration. */
    val otherComponents: List<String> = enabledComponents.filterNot { it.endsWith("_DECODER") || it.endsWith("_PARSER") }

    fun violations(approvedDecoders: Set<String>): List<String> = buildList {
        if (license != "LGPL version 2.1 or later") add("$abi: FFmpeg recorded its licence as '$license'")
        for (flag in listOf("CONFIG_GPL", "CONFIG_NONFREE", "CONFIG_VERSION3")) {
            if (configValues[flag] != "0") add("$abi: $flag is ${configValues[flag]}, expected 0")
        }
        // `--enable-lib...` is any external library (x264, x265, fdk-aac, ...): the one found is named in full.
        val forbidden = listOf(
            Regex("--enable-gpl\\b"), Regex("--enable-nonfree\\b"), Regex("--enable-version3\\b"),
            Regex("--enable-lib[A-Za-z0-9_-]*"), Regex("--enable-network\\b")
        )
        for (flag in forbidden) {
            flag.find(configuration)?.let { add("$abi: the configure line contains ${it.value}") }
        }
        for (required in listOf("--disable-gpl", "--disable-nonfree", "--enable-shared", "--disable-static", "--disable-network")) {
            if (!configuration.contains(required)) add("$abi: the configure line lacks $required")
        }
        if (configValues["CONFIG_SHARED"] != "1" || configValues["CONFIG_STATIC"] != "0") add("$abi: not built as shared libraries")
        (decoders - approvedDecoders).sorted().forEach { add("$abi: decoder '$it' is built but not in approved_decoders.txt") }
        (approvedDecoders - decoders).sorted().forEach { add("$abi: approved decoder '$it' is missing from the build") }
        otherComponents.forEach { add("$abi: $it is switched on; the extension is for decoding only") }
        if (commit == null) add("$abi: BUILD-INFO.txt names no 40-digit source commit")
    }
}

/** Automated check that the FFmpeg the app links is the LGPL, shared-library, approved-decoder build (ADR-010, DEC-014, DEC-020). */
class LgplBuildVerificationTest {

    private val shippedAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")

    private fun approvedDecoders(): Set<String> {
        val file = listOf("tools/ffmpeg/approved_decoders.txt", "../tools/ffmpeg/approved_decoders.txt").map(::File).firstOrNull { it.isFile }
        assertTrue("tools/ffmpeg/approved_decoders.txt was not found from ${File(".").absolutePath}", file != null)
        return file!!.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()
    }

    private fun records(): List<FfmpegBuildRecord> = shippedAbis.map { abi ->
        val dir = File(InvariantFiles.ffmpegBuildRecords, abi)
        FfmpegBuildRecord(
            abi = abi,
            config = File(dir, "config.h").readText(),
            components = File(dir, "config_components.h").readText(),
            info = File(dir, "BUILD-INFO.txt").readText()
        )
    }

    // ---- the real thing --------------------------------------------------------------------------------------------

    @Test
    fun theLinkedFfmpeg_isLgplSharedAndDecodesOnlyTheApprovedFormats_onEveryShippedAbi() {
        val problems = records().flatMap { it.violations(approvedDecoders()) }

        assertTrue("The FFmpeg bundle breaks the LGPL / approved-decoder policy:\n" + problems.joinToString("\n") { " - $it" }, problems.isEmpty())
    }

    @Test
    fun theApprovedList_isTheSmallOneThePolicyDecided() {
        // Guards the guard: an emptied or widened approved list would make the test above agree with anything.
        // spec/11B D2 and DEC-020. Widening it (the AC-3 / E-AC-3 add-back, DEC-021) is a decision, so this line changes with it.
        assertEquals(setOf("alac", "pcm_alaw", "pcm_mulaw"), approvedDecoders())
        assertEquals(setOf("alac", "pcm_alaw", "pcm_mulaw"), records().first().decoders)
    }

    @Test
    fun everyAbi_wasBuiltFromTheSameSourceCommit_andNoticeNamesIt() {
        val commits = records().map { it.commit }.toSet()
        assertEquals("the ABIs were built from different FFmpeg commits: $commits", 1, commits.size)

        val notice = File(InvariantFiles.licenseAssets, "open-source-licenses/NOTICE.txt").readText()
        assertTrue("NOTICE does not name the FFmpeg commit ${commits.single()} that is linked", notice.contains(commits.single()!!))
    }

    @Test
    fun ffmpegAudioRenderer_isPresentOnRuntimeClasspath() {
        // The Java side is vendored (androidx/media3/decoder/ffmpeg); the native side comes from the bundle.
        Class.forName("androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer")
        Class.forName("androidx.media3.decoder.ffmpeg.FfmpegLibrary")
    }

    // ---- fixtures: each rule must fire on the record that was wrong -------------------------------------------------

    private val goodConfig = """
        #define FFMPEG_CONFIGURATION "--disable-gpl --disable-nonfree --disable-version3 --enable-shared --disable-static --disable-network --enable-decoder='alac'"
        #define FFMPEG_LICENSE "LGPL version 2.1 or later"
        #define CONFIG_SHARED 1
        #define CONFIG_STATIC 0
        #define CONFIG_GPL 0
        #define CONFIG_NONFREE 0
        #define CONFIG_VERSION3 0
    """.trimIndent()
    private val goodComponents = "#define CONFIG_ALAC_DECODER 1\n#define CONFIG_AAC_DECODER 0\n#define CONFIG_AAC_PARSER 0\n"
    private val goodInfo = "FFmpeg commit: 946fcce07b6dcd0331c8cc609192aeff5e1924f8\n"

    private fun record(config: String = goodConfig, components: String = goodComponents, info: String = goodInfo) =
        FfmpegBuildRecord("fixture", config, components, info)

    @Test
    fun fixture_aCorrectRecord_passes() {
        assertEquals(emptyList<String>(), record().violations(setOf("alac")))
    }

    @Test
    fun fixture_aGplRecord_isCaught() {
        val gpl = goodConfig
            .replace("--disable-gpl", "--enable-gpl")
            .replace("LGPL version 2.1 or later", "GPL version 2 or later")
            .replace("CONFIG_GPL 0", "CONFIG_GPL 1")
        val problems = record(config = gpl).violations(setOf("alac"))

        assertTrue(problems.toString(), problems.any { it.contains("licence") })
        assertTrue(problems.toString(), problems.any { it.contains("CONFIG_GPL") })
        assertTrue(problems.toString(), problems.any { it.contains("--enable-gpl") })
        assertTrue(problems.toString(), problems.any { it.contains("lacks --disable-gpl") })
    }

    @Test
    fun fixture_nonfreeVersion3AndExternalLibraries_areCaught() {
        for (flag in listOf("--enable-nonfree", "--enable-version3", "--enable-libx264", "--enable-network")) {
            val problems = record(config = goodConfig.replace("--disable-static", "--disable-static $flag")).violations(setOf("alac"))
            assertTrue("$flag was not caught: $problems", problems.any { it.contains(flag) })
        }
    }

    @Test
    fun fixture_aStaticBuild_isCaught() {
        val problems = record(config = goodConfig.replace("CONFIG_SHARED 1", "CONFIG_SHARED 0").replace("CONFIG_STATIC 0", "CONFIG_STATIC 1"))
            .violations(setOf("alac"))

        assertTrue(problems.toString(), problems.any { it.contains("not built as shared libraries") })
    }

    @Test
    fun fixture_aDecoderOutsideTheApprovedList_isCaught() {
        // The shape of the problem with the prebuilt decoder this replaced: it decoded AAC, AC-3, DTS and TrueHD.
        val problems = record(components = goodComponents + "#define CONFIG_AC3_DECODER 1\n#define CONFIG_DCA_DECODER 1\n")
            .violations(setOf("alac"))

        assertEquals(2, problems.count { it.contains("not in approved_decoders.txt") })
        assertTrue(problems.toString(), problems.any { it.contains("'ac3'") })
        assertTrue(problems.toString(), problems.any { it.contains("'dca'") })
    }

    @Test
    fun fixture_anApprovedDecoderThatIsNotBuilt_isCaught() {
        val problems = record().violations(setOf("alac", "pcm_alaw"))

        assertTrue(problems.toString(), problems.any { it.contains("'pcm_alaw'") && it.contains("missing") })
    }

    @Test
    fun fixture_anEncoderOrProtocol_isCaught() {
        val problems = record(components = goodComponents + "#define CONFIG_AAC_ENCODER 1\n#define CONFIG_HTTP_PROTOCOL 1\n")
            .violations(setOf("alac"))

        assertTrue(problems.toString(), problems.any { it.contains("AAC_ENCODER") })
        assertTrue(problems.toString(), problems.any { it.contains("HTTP_PROTOCOL") })
    }

    @Test
    fun fixture_aRecordWithoutACommit_isCaught() {
        assertTrue(record(info = "FFmpeg tag: n9.0.2\n").violations(setOf("alac")).any { it.contains("commit") })
    }
}
