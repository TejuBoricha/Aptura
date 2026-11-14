"""Tests for verify_lgpl_symbols.py.

The old verifier audited a list inside itself and could not fail on what ships. These tests make sure the new one fails when
it should: every way a build or a library can break the LGPL audio-only policy, and every way it can be handed nothing.
Fixtures use the formats FFmpeg's `configure` really writes (`config.h`: FFMPEG_CONFIGURATION, FFMPEG_LICENSE, CONFIG_
switches; `config_components.h`: one CONFIG_<NAME>_<KIND> switch per component), checked against the n6.1.1 script.
"""

import struct
import tempfile
import unittest
from pathlib import Path

import verify_lgpl_symbols as v

APPROVED = {"alac", "pcm_alaw", "pcm_mulaw"}
GOOD_CONFIGURE = (
    "--enable-shared --disable-static --disable-gpl --disable-nonfree --disable-version3 --disable-programs "
    "--disable-doc --disable-network --disable-autodetect --disable-everything "
    "--enable-decoder=alac,pcm_alaw,pcm_mulaw"
)


def write_build_dir(
    folder: Path,
    decoders=APPROVED,
    extra_components: str = "",
    configure: str = GOOD_CONFIGURE,
    licence: str = "LGPL version 2.1 or later",
    switches: str = "#define CONFIG_GPL 0\n#define CONFIG_NONFREE 0\n#define CONFIG_VERSION3 0\n#define CONFIG_NETWORK 0\n",
    components: str | None = None,
) -> Path:
    config_h = f'#define FFMPEG_CONFIGURATION "{configure}"\n#define FFMPEG_LICENSE "{licence}"\n{switches}'
    if components is None:
        components = "".join(f"#define CONFIG_{d.upper()}_DECODER 1\n" for d in sorted(decoders))
        components += "#define CONFIG_H264_DECODER 0\n#define CONFIG_AAC_ENCODER 0\n" + extra_components
    (folder / "config.h").write_text(config_h, encoding="utf-8")
    (folder / "config_components.h").write_text(components, encoding="utf-8")
    return folder


class BuildRecordTests(unittest.TestCase):
    def audit(self, **kwargs):
        with tempfile.TemporaryDirectory() as folder:
            return v.audit_build_dir(write_build_dir(Path(folder), **kwargs), APPROVED)

    def test_a_clean_build_passes(self):
        self.assertEqual(self.audit(), [])

    def test_an_unapproved_decoder_is_named(self):
        problems = self.audit(extra_components="#define CONFIG_AC3_DECODER 1\n#define CONFIG_DCA_DECODER 1\n")
        self.assertIn("decoder built but not approved: ac3", problems)
        self.assertIn("decoder built but not approved: dca", problems)

    def test_a_video_decoder_is_a_violation(self):
        self.assertIn("decoder built but not approved: h264", self.audit(extra_components="#define CONFIG_H264_DECODER 1\n"))

    def test_an_approved_decoder_missing_from_the_build_is_reported(self):
        problems = self.audit(decoders=APPROVED - {"alac"})
        self.assertIn("approved decoder missing from the build: alac", problems)

    def test_every_forbidden_component_kind_is_caught(self):
        for kind in v.MUST_BE_EMPTY:
            with self.subTest(kind=kind):
                problems = self.audit(extra_components=f"#define CONFIG_FOO_{kind.upper()} 1\n")
                self.assertIn(f"{kind} built but none is allowed: foo", problems)

    def test_an_unapproved_parser_is_caught_and_an_approved_one_is_not(self):
        self.assertIn("parser built but not approved: h264", self.audit(extra_components="#define CONFIG_H264_PARSER 1\n"))
        self.assertEqual(self.audit(extra_components="#define CONFIG_FLAC_PARSER 1\n"), [])

    def test_gpl_nonfree_version3_and_network_switches_are_caught(self):
        for name in ("GPL", "NONFREE", "VERSION3", "NETWORK"):
            with self.subTest(switch=name):
                switches = "".join(f"#define CONFIG_{n} {1 if n == name else 0}\n" for n in ("GPL", "NONFREE", "VERSION3", "NETWORK"))
                self.assertIn(f"CONFIG_{name} is 1, expected 0", self.audit(switches=switches))

    def test_a_missing_switch_is_not_read_as_off(self):
        problems = self.audit(switches="#define CONFIG_GPL 0\n#define CONFIG_NONFREE 0\n#define CONFIG_VERSION3 0\n")
        self.assertIn("CONFIG_NETWORK is missing, expected 0", problems)

    def test_a_gpl_or_nonfree_licence_is_caught(self):
        for licence in ("GPL version 2 or later", "LGPL version 3 or later", "nonfree and unredistributable"):
            with self.subTest(licence=licence):
                self.assertTrue(any("FFMPEG_LICENSE" in p for p in self.audit(licence=licence)))

    def test_configure_arguments_are_audited_as_recorded(self):
        self.assertIn("forbidden configure flag: --enable-gpl", self.audit(configure=GOOD_CONFIGURE + " --enable-gpl"))
        self.assertIn("missing mandatory configure flag: --disable-everything", self.audit(configure=GOOD_CONFIGURE.replace("--disable-everything", "")))
        self.assertIn("configure enables a decoder that is not approved: ac3", self.audit(configure=GOOD_CONFIGURE + " --enable-decoder=ac3"))

    def test_a_record_that_is_not_ffmpegs_fails_loudly(self):
        with tempfile.TemporaryDirectory() as folder:
            write_build_dir(Path(folder), switches="")
            with self.assertRaises(v.InputError):
                v.audit_build_dir(Path(folder), APPROVED)

    def test_an_empty_component_list_fails_loudly_instead_of_passing(self):
        with tempfile.TemporaryDirectory() as folder:
            write_build_dir(Path(folder), components="/* nothing */\n")
            with self.assertRaises(v.InputError):
                v.audit_build_dir(Path(folder), APPROVED)

    def test_a_missing_directory_fails_loudly(self):
        with self.assertRaises(v.InputError):
            v.audit_build_dir(Path("/no/such/ffmpeg/build"), APPROVED)


class LibraryTests(unittest.TestCase):
    def audit(self, files: dict[str, bytes]):
        with tempfile.TemporaryDirectory() as folder:
            paths = []
            for name, data in files.items():
                path = Path(folder, name)
                path.write_bytes(data)
                paths.append(path)
            return v.audit_libraries(paths, APPROVED)

    def test_approved_decoders_only_passes(self):
        self.assertEqual(self.audit({"libavcodec.so": b"\0ff_alac_decoder\0ff_pcm_alaw_decoder\0"}), [])

    def test_the_decoder_list_of_the_library_that_ships_today_is_rejected(self):
        # What `libffmpegJNI.so` of the prebuilt Jellyfin 1.5.0+1 decoder contains, dumped on 2026-11-14 (LEG-01, LEG-02).
        shipped = "aac aac_latm ac3 alac dca eac3 flac mlp mp3 pcm_alaw pcm_mulaw truehd".split()
        data = b"\0".join(f"ff_{name}_decoder".encode() for name in shipped)
        problems = self.audit({"libffmpegJNI.so": data})
        rejected = sorted(p.rsplit(": ", 1)[1] for p in problems if "not approved" in p)
        self.assertEqual(rejected, ["aac", "aac_latm", "ac3", "dca", "eac3", "flac", "mlp", "mp3", "truehd"])

    def test_a_gpl_licence_string_is_caught(self):
        self.assertTrue(self.audit({"libavutil.so": b"\0GPL version 2 or later\0"}))
        self.assertTrue(self.audit({"libavutil.so": b"\0nonfree and unredistributable\0"}))

    def test_the_lgpl_string_is_not_mistaken_for_gpl(self):
        self.assertEqual(self.audit({"libavutil.so": b"\0LGPL version 2.1 or later\0"}), [])

    def test_a_stripped_libavcodec_is_a_failure_not_a_pass(self):
        problems = self.audit({"libavcodec.so.60": b"\0nothing readable here\0"})
        self.assertTrue(any("nothing can be proven" in p for p in problems))

    def test_other_libraries_need_no_decoder_names(self):
        self.assertEqual(self.audit({"libffmpegJNI.so": b"\0jni glue\0"}), [])

    def test_a_name_that_merely_contains_an_approved_one_is_not_approved(self):
        self.assertTrue(self.audit({"libavcodec.so": b"\0ff_alacx_decoder\0"}))

    def test_directories_are_searched_and_missing_paths_fail(self):
        with tempfile.TemporaryDirectory() as folder:
            sub = Path(folder, "arm64-v8a")
            sub.mkdir()
            (sub / "libavcodec.so").write_bytes(b"ff_alac_decoder")
            self.assertEqual([p.name for p in v.find_libraries([Path(folder)])], ["libavcodec.so"])
            with self.assertRaises(v.InputError):
                v.find_libraries([Path(folder, "missing")])
        with tempfile.TemporaryDirectory() as empty, self.assertRaises(v.InputError):
            v.find_libraries([Path(empty)])


def make_elf(is64: bool = True, align: int = 0x4000, soname: str | None = "libavcodec.so", needed: tuple[str, ...] = ("libavutil.so",)) -> bytes:
    """A minimal little-endian ELF shared object: one PT_LOAD, one PT_DYNAMIC, a string table and a dynamic section."""
    strtab = b"\0"
    offsets: dict[str, int] = {}
    for name in [*needed, *([soname] if soname else [])]:
        offsets[name] = len(strtab)
        strtab += name.encode() + b"\0"
    ehdr_size, phdr_size, entry = (64, 56, 16) if is64 else (52, 32, 8)
    strtab_off = ehdr_size + 2 * phdr_size
    dyn_off = (strtab_off + len(strtab) + 7) // 8 * 8
    entries = [(1, offsets[n]) for n in needed] + ([(14, offsets[soname])] if soname else []) + [(5, strtab_off), (0, 0)]
    dyn_size = len(entries) * entry
    total = dyn_off + dyn_size
    efmt, pack = ("<qQ", struct.pack) if is64 else ("<iI", struct.pack)
    if is64:
        header = b"\x7fELF" + bytes([2, 1, 1]) + bytes(9) + struct.pack("<HHIQQQIHHHHHH", 3, 183, 1, 0, ehdr_size, 0, 0, ehdr_size, phdr_size, 2, 0, 0, 0)
        load = struct.pack("<IIQQQQQQ", 1, 5, 0, 0, 0, total, total, align)
        dynamic = struct.pack("<IIQQQQQQ", 2, 6, dyn_off, dyn_off, dyn_off, dyn_size, dyn_size, 8)
    else:
        header = b"\x7fELF" + bytes([1, 1, 1]) + bytes(9) + struct.pack("<HHIIIIIHHHHHH", 3, 40, 1, 0, ehdr_size, 0, 0, ehdr_size, phdr_size, 2, 0, 0, 0)
        load = struct.pack("<IIIIIIII", 1, 0, 0, 0, total, total, 5, align)
        dynamic = struct.pack("<IIIIIIII", 2, dyn_off, dyn_off, dyn_off, dyn_size, dyn_size, 6, 4)
    body = header + load + dynamic + strtab
    body += bytes(dyn_off - len(body))
    for tag, value in entries:
        body += pack(efmt, tag, value)
    return body


class ElfRuleTests(unittest.TestCase):
    """The rules a shipped native library must meet: 16 KB pages on 64-bit, and names an APK can carry."""

    def test_the_reader_finds_what_the_builder_put_in(self):
        info = v.elf_info(make_elf(soname="libavcodec.so", needed=("libavutil.so", "libswresample.so")))
        self.assertEqual((info["is64"], info["aligns"], info["soname"], info["needed"]), (True, [0x4000], "libavcodec.so", ["libavutil.so", "libswresample.so"]))

    def test_a_good_64_bit_library_has_no_findings(self):
        self.assertEqual(v.audit_elf("arm64-v8a/libavcodec.so", make_elf()), [])

    def test_a_4k_aligned_64_bit_library_is_rejected_for_play(self):
        problems = v.audit_elf("arm64-v8a/libavcodec.so", make_elf(align=0x1000))
        self.assertTrue(any("16 KB" in p for p in problems), problems)

    def test_a_4k_aligned_32_bit_library_is_not_rejected(self):
        self.assertEqual(v.audit_elf("armeabi-v7a/libavcodec.so", make_elf(is64=False, align=0x1000)), [])

    def test_a_16k_aligned_32_bit_library_is_read_correctly(self):
        info = v.elf_info(make_elf(is64=False, align=0x4000))
        self.assertEqual((info["is64"], info["aligns"], info["soname"]), (False, [0x4000], "libavcodec.so"))

    def test_a_versioned_soname_is_rejected(self):
        problems = v.audit_elf("x86_64/libavcodec.so", make_elf(soname="libavcodec.so.61"))
        self.assertTrue(any("SONAME" in p for p in problems), problems)

    def test_a_versioned_dependency_is_rejected(self):
        problems = v.audit_elf("x86_64/libavcodec.so", make_elf(needed=("libavutil.so.59",)))
        self.assertTrue(any("depends on 'libavutil.so.59'" in p for p in problems), problems)

    def test_something_that_is_not_an_elf_is_left_to_the_other_checks(self):
        self.assertEqual(v.audit_elf("x/libavcodec.so", b"ff_alac_decoder"), [])
        self.assertIsNone(v.elf_info(b"\x7fELF"))

    def test_a_truncated_elf_does_not_crash_the_audit(self):
        self.assertEqual(v.audit_elf("x/lib.so", make_elf()[:90]), [])

    def test_the_elf_rules_run_inside_the_library_audit(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder, "libavcodec.so")
            path.write_bytes(make_elf(align=0x1000) + b"ff_alac_decoder")
            problems = v.audit_libraries([path], APPROVED)
            self.assertTrue(any("16 KB" in p for p in problems), problems)


class CommandLineTests(unittest.TestCase):
    def test_nothing_to_check_is_exit_two_not_a_pass(self):
        self.assertEqual(v.main([]), 2)

    def test_a_clean_build_is_exit_zero_and_a_dirty_one_is_exit_one(self):
        with tempfile.TemporaryDirectory() as folder, tempfile.TemporaryDirectory() as approved_dir:
            approved = Path(approved_dir, "approved.txt")
            approved.write_text("# comment\n" + "\n".join(sorted(APPROVED)) + "\n", encoding="utf-8")
            build = write_build_dir(Path(folder))
            self.assertEqual(v.main(["--build-dir", str(build), "--approved", str(approved)]), 0)
            write_build_dir(Path(folder), extra_components="#define CONFIG_AC3_DECODER 1\n")
            self.assertEqual(v.main(["--build-dir", str(build), "--approved", str(approved)]), 1)

    def test_an_unreadable_input_is_exit_two(self):
        self.assertEqual(v.main(["--build-dir", "/no/such/dir"]), 2)

    def test_an_empty_approved_list_is_exit_two(self):
        with tempfile.TemporaryDirectory() as folder:
            approved = Path(folder, "approved.txt")
            approved.write_text("# nothing approved\n", encoding="utf-8")
            self.assertEqual(v.main(["--libs", str(approved), "--approved", str(approved)]), 2)


class PolicyFileTests(unittest.TestCase):
    def test_the_approved_list_is_what_the_platform_does_not_guarantee_and_is_free(self):
        self.assertEqual(v.load_approved(v.DEFAULT_APPROVED), APPROVED)

    def test_nothing_with_a_licence_question_is_approved(self):
        for name in ("aac", "aac_latm", "ac3", "eac3", "dca", "truehd", "mlp", "h264", "hevc", "av1", "vp9"):
            self.assertNotIn(name, v.load_approved(v.DEFAULT_APPROVED), name)

    def test_nothing_the_android_cdd_already_guarantees_is_bundled(self):
        # CDD 5.1.3 requires FLAC, MP3, Vorbis and Opus decoding on every compatible device (spec/11B, D2).
        for name in ("flac", "mp3", "mp3float", "vorbis", "opus"):
            self.assertNotIn(name, v.load_approved(v.DEFAULT_APPROVED), name)


if __name__ == "__main__":
    unittest.main()
