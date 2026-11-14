"""Tests for release_scope.py. They also prove the failure paths: when the change cannot be judged, the answer is
"run the release builds", never "skip them"."""

from __future__ import annotations

import subprocess
import sys
import unittest
from pathlib import Path

import release_scope as scope

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
SCRIPT = HERE / "release_scope.py"

# Files that define what the release build is. If one is renamed or moved, this test must fail loudly instead of the
# file silently falling out of the list and becoming skippable.
RELEASE_DEFINING_FILES = (
    "build.gradle.kts",
    "settings.gradle.kts",
    "app/build.gradle.kts",
    "app/proguard-rules.pro",
    "gradle/libs.versions.toml",
    "gradle/wrapper/gradle-wrapper.properties",
    "gradle.properties",
    "app/src/main/AndroidManifest.xml",
    "app/src/debug/AndroidManifest.xml",
)


class ClassifyTest(unittest.TestCase):
    def test_release_defining_files_exist_and_need_the_builds(self):
        for name in RELEASE_DEFINING_FILES:
            with self.subTest(name=name):
                self.assertTrue((ROOT / name).is_file(), f"{name} moved: update RULES in release_scope.py")
                code, reasons = scope.classify([name])
                self.assertEqual(scope.NEEDED, code)
                self.assertEqual(1, len(reasons))

    def test_more_release_affecting_paths(self):
        for path in (
            "app/src/main/res/xml/backup_rules.xml",
            "app/src/main/res/raw/keep.xml",
            "app/src/main/jniLibs/arm64-v8a/libx.so",
            "app/src/main/baseline-prof.txt",
            "app/src/main/java/androidx/media3/decoder/ffmpeg/FfmpegLibrary.java",
            "gradle/ffmpeg-bundle.properties",
            "gradle/wrapper/gradle-wrapper.jar",
            "gradlew.bat",
            "app/consumer-rules.pro",
            "buildSrc/src/main/kotlin/Foo.kt",
        ):
            with self.subTest(path=path):
                self.assertEqual(scope.NEEDED, scope.classify([path])[0])

    def test_source_test_and_doc_changes_can_skip(self):
        for path in (
            "app/src/main/java/org/aptura/player/PlayerScreen.kt",
            "app/src/test/java/org/aptura/player/FooTest.kt",
            "app/src/androidTest/java/org/aptura/player/BarTest.kt",
            "app/src/main/res/values/strings.xml",
            "docs/BUILDING.md",
            "README.md",
            ".agents/STATE.md",
            ".github/workflows/ci.yml",
            "tools/gate.ps1",
        ):
            with self.subTest(path=path):
                self.assertEqual((scope.SKIPPABLE, []), scope.classify([path]))

    def test_one_release_file_among_many_others_is_enough(self):
        code, reasons = scope.classify(
            ["docs/BUILDING.md", "app/src/main/java/A.kt", "gradle/libs.versions.toml", "README.md"]
        )
        self.assertEqual(scope.NEEDED, code)
        self.assertEqual(["gradle/libs.versions.toml  (version catalog or Gradle wrapper)"], reasons)

    def test_rules_are_anchored_at_the_repository_root(self):
        # A lookalike name deeper in the tree must not trigger the builds, and must not be mistaken for the real file.
        for path in (
            "docs/app/build.gradle.kts",
            "app/src/main/java/org/aptura/gradle/Foo.kt",
            "tools/proguard-rules.pro",
            "app/src/main/java/org/aptura/res/xml/Foo.kt",
        ):
            with self.subTest(path=path):
                self.assertEqual(scope.SKIPPABLE, scope.classify([path])[0])

    def test_windows_separators_and_dot_slash_are_normalized(self):
        self.assertEqual(scope.NEEDED, scope.classify(["app\\build.gradle.kts"])[0])
        self.assertEqual(scope.NEEDED, scope.classify(["./gradle/libs.versions.toml"])[0])
        # ".github" must keep its leading dot: only "./" is stripped, not every leading dot.
        self.assertEqual("app/x", scope.normalize("./app/x"))
        self.assertEqual(".github/ci.yml", scope.normalize(".github/ci.yml"))

    def test_nothing_to_judge_is_undecidable_not_skippable(self):
        for paths in ([], [""], ["   ", "\t"]):
            with self.subTest(paths=paths):
                self.assertEqual(scope.UNDECIDABLE, scope.classify(paths)[0])


class CliTest(unittest.TestCase):
    def run_cli(self, stdin: bytes) -> subprocess.CompletedProcess:
        return subprocess.run([sys.executable, "-I", str(SCRIPT)], input=stdin, capture_output=True, check=False)

    def test_exit_codes(self):
        needed = self.run_cli(b"docs/a.md\ngradle/libs.versions.toml\n")
        self.assertEqual(0, needed.returncode)
        self.assertIn(b"gradle/libs.versions.toml", needed.stdout)
        self.assertEqual(1, self.run_cli(b"app/src/main/java/A.kt\n").returncode)

    def test_empty_stdin_is_undecidable(self):
        self.assertEqual(2, self.run_cli(b"").returncode)

    def test_unreadable_stdin_is_undecidable_not_skippable(self):
        # Invalid UTF-8 cannot be decoded; the caller must still be told to run the builds.
        result = self.run_cli(b"\xff\xfe\x00gradle\xff")
        self.assertEqual(2, result.returncode)

    def test_byte_order_mark_does_not_hide_the_first_path(self):
        # PowerShell can prepend a BOM. Without handling it, "app/build.gradle.kts" would not match and look skippable.
        self.assertEqual(0, self.run_cli(b"\xef\xbb\xbfapp/build.gradle.kts\n").returncode)


if __name__ == "__main__":
    unittest.main()
