"""Tests for release_smoke.py. The decisions are pure functions; the whole run is driven with a fake adb so every way it
should fail is exercised, not just the happy path."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

import release_smoke as smoke

RELEASE_FLAGS = "    flags=[ HAS_CODE ALLOW_CLEAR_USER_DATA ]\n"
DEBUG_FLAGS = "    flags=[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]\n"
CRASH_LOG = (
    "E/AndroidRuntime( 4187): FATAL EXCEPTION: main\n"
    "E/AndroidRuntime( 4187): Process: org.aptura.player, PID: 4187\n"
    "E/AndroidRuntime( 4187): java.lang.NoClassDefFoundError: Failed resolution of: Lx/y;\n"
)
OTHER_APP_CRASH_LOG = (
    "E/AndroidRuntime( 900): FATAL EXCEPTION: main\n"
    "E/AndroidRuntime( 900): Process: com.example.other, PID: 900\n"
    "E/AndroidRuntime( 900): java.lang.IllegalStateException: not ours\n"
)
CLEAN_MONKEY = "// Monkey finished\nEvents injected: 400\n## Network stats: elapsed time=1ms\n"


HEALTHY_DEVICE = {
    "shell getprop sys.boot_completed": "1\n",
    "uninstall": "Success\n",
    "install -r": "Performing Streamed Install\nSuccess\n",
    "shell dumpsys package": RELEASE_FLAGS,
    "shell pidof": "3514\n",
    "shell monkey -p org.aptura.player --throttle": CLEAN_MONKEY,
    "shell content query": "Row: 23 _id=1000000503, _display_name=smoke_clip.mp4\n",
    "shell dumpsys media_session": "Media button session is org.aptura.player/androidx.media3.session.id./7\n",
    "logcat -d": "I/ActivityManager( 598): Start proc 3514:org.aptura.player\n",
}


class FakeAdb:
    """Scripted adb. Responses are matched by command prefix, overrides first. A response is a string, or a
    callable(call_index) -> str where call_index counts earlier calls that matched the same prefix."""

    def __init__(self, overrides: dict | None = None):
        self.responses = list((overrides or {}).items()) + list(HEALTHY_DEVICE.items())
        self.calls: list[tuple[str, ...]] = []
        self.counts: dict[str, int] = {}

    def __call__(self, *args: str, timeout: int = 180) -> str:
        self.calls.append(args)
        line = " ".join(args)
        for prefix, response in self.responses:
            if line.startswith(prefix):
                index = self.counts.get(prefix, 0)
                self.counts[prefix] = index + 1
                return response(index) if callable(response) else str(response)
        return ""

    def ran(self, prefix: str) -> bool:
        return any(" ".join(call).startswith(prefix) for call in self.calls)


def fake(overrides: dict | None = None) -> FakeAdb:
    return FakeAdb(overrides)


class PureDecisionsTest(unittest.TestCase):
    def test_package_flags(self):
        self.assertEqual({"HAS_CODE", "ALLOW_CLEAR_USER_DATA"}, smoke.package_flags(RELEASE_FLAGS))
        self.assertIn("DEBUGGABLE", smoke.package_flags(DEBUG_FLAGS))
        self.assertIn("DEBUGGABLE", smoke.package_flags("  pkgFlags=[ DEBUGGABLE HAS_CODE ]"))
        self.assertIsNone(smoke.package_flags("nothing useful here"))
        self.assertIsNone(smoke.package_flags(""))

    def test_crash_findings_sees_only_this_apps_crashes(self):
        self.assertEqual(1, len(smoke.crash_findings(CRASH_LOG)))
        self.assertIn("NoClassDefFoundError", smoke.crash_findings(CRASH_LOG)[0])
        self.assertEqual([], smoke.crash_findings(OTHER_APP_CRASH_LOG))
        self.assertEqual([], smoke.crash_findings("I/ActivityManager( 598): Start proc\n"))
        self.assertEqual([], smoke.crash_findings(""))

    def test_crash_findings_sees_anr_and_native_crash(self):
        anr = smoke.crash_findings("E/ActivityManager( 598): ANR in org.aptura.player (org.aptura.player/.MainActivity)")
        self.assertEqual(1, len(anr))
        self.assertIn("ANR", anr[0])
        native = smoke.crash_findings(
            "F/libc ( 3514): Fatal signal 11 (SIGSEGV), code 1, fault addr 0x0 in tid 3514 (org.aptura.player)")
        self.assertEqual(1, len(native))
        self.assertEqual([], smoke.crash_findings("ANR in org.aptura.playerX something"))  # different package name

    def test_monkey_findings(self):
        self.assertEqual([], smoke.monkey_findings(CLEAN_MONKEY))
        self.assertTrue(smoke.monkey_findings("// CRASH: org.aptura.player (pid 1)\nEvents injected: 400\n"))
        self.assertTrue(smoke.monkey_findings("// NOT RESPONDING: org.aptura.player\nEvents injected: 400\n"))
        self.assertTrue(smoke.monkey_findings("Events injected: 120\n"))      # cut short
        self.assertTrue(smoke.monkey_findings(""))                            # nothing at all is not a pass
        self.assertTrue(smoke.monkey_findings("Monkey aborted due to error.\n"))

    def test_media_id(self):
        out = "Row: 0 _id=7, _display_name=other.mp4\nRow: 1 _id=1000000503, _display_name=smoke_clip.mp4\n"
        self.assertEqual("1000000503", smoke.media_id(out, "smoke_clip.mp4"))
        self.assertIsNone(smoke.media_id(out, "smoke_hevc.mp4"))
        self.assertIsNone(smoke.media_id("", "smoke_clip.mp4"))


class RunTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.clips = Path(self.tmp.name)
        for name in smoke.CLIPS:
            (self.clips / name).write_bytes(b"x")
        self.apk = Path(self.tmp.name) / "app-release.apk"
        self.apk.write_bytes(b"x")

    def run_with(self, adb: FakeAdb, **kwargs) -> smoke.Result:
        return smoke.run_smoke(adb, self.apk, self.clips, sleep=lambda _s: None, **kwargs)

    def test_a_healthy_run_passes(self):
        result = self.run_with(fake())
        self.assertEqual((smoke.OK, []), (result.code, result.failures))
        self.assertTrue(any("media session of the app is active" in n for n in result.notes))

    def test_a_failed_install_stops_before_anything_is_run(self):
        adb = fake({"install -r": "Failure [INSTALL_FAILED_INVALID_APK]\n"})
        result = self.run_with(adb)
        self.assertEqual(smoke.SETUP_PROBLEM, result.code)
        self.assertFalse(adb.ran("shell monkey"))

    def test_a_debuggable_apk_is_refused(self):
        result = self.run_with(fake({"shell dumpsys package": DEBUG_FLAGS}))
        self.assertEqual(smoke.SETUP_PROBLEM, result.code)
        self.assertIn("debuggable", result.failures[0])

    def test_unreadable_flags_are_refused_not_assumed_to_be_release(self):
        self.assertEqual(smoke.SETUP_PROBLEM, self.run_with(fake({"shell dumpsys package": ""})).code)

    def test_a_device_that_never_boots_is_a_setup_problem(self):
        result = self.run_with(fake({"shell getprop sys.boot_completed": "\n"}), boot_attempts=2)
        self.assertEqual(smoke.SETUP_PROBLEM, result.code)

    def test_missing_sample_clips_are_a_setup_problem(self):
        (self.clips / smoke.CLIPS[0]).unlink()
        self.assertEqual(smoke.SETUP_PROBLEM, self.run_with(fake()).code)

    def test_an_app_that_is_dead_after_cold_start_fails(self):
        result = self.run_with(fake({"shell pidof": ""}))
        self.assertEqual(smoke.APP_PROBLEM, result.code)
        self.assertTrue(any("cold start" in f for f in result.failures))

    def test_an_app_that_dies_during_the_ui_exerciser_fails(self):
        # alive after cold start (call 0), dead from then on
        result = self.run_with(fake({"shell pidof": lambda i: "3514\n" if i == 0 else ""}))
        self.assertEqual(smoke.APP_PROBLEM, result.code)
        self.assertTrue(any("after the UI exerciser" in f for f in result.failures))

    def test_an_app_that_dies_on_playback_fails(self):
        result = self.run_with(fake({"shell pidof": lambda i: "3514\n" if i < 2 else ""}))
        self.assertEqual(smoke.APP_PROBLEM, result.code)
        self.assertTrue(any("playback" in f for f in result.failures))

    def test_a_crash_in_logcat_fails_even_if_the_process_looks_alive(self):
        result = self.run_with(fake({"logcat -d": CRASH_LOG}))
        self.assertEqual(smoke.APP_PROBLEM, result.code)
        self.assertTrue(any("fatal exception" in f for f in result.failures))

    def test_another_apps_crash_does_not_fail_the_run(self):
        self.assertEqual(smoke.OK, self.run_with(fake({"logcat -d": OTHER_APP_CRASH_LOG})).code)

    def test_a_monkey_crash_fails(self):
        out = "// CRASH: org.aptura.player (pid 3514)\nEvents injected: 400\n"
        result = self.run_with(fake({"shell monkey -p org.aptura.player --throttle": out}))
        self.assertEqual(smoke.APP_PROBLEM, result.code)

    def test_an_unindexed_clip_is_only_a_note_but_a_crash_would_still_fail(self):
        result = self.run_with(fake({"shell content query": ""}))
        self.assertEqual(smoke.OK, result.code)
        self.assertTrue(any("not indexed" in n for n in result.notes))
        self.assertEqual(smoke.APP_PROBLEM,
                         self.run_with(fake({"shell content query": "", "logcat -d": CRASH_LOG})).code)


class CliTest(unittest.TestCase):
    def test_a_missing_apk_is_a_setup_problem(self):
        self.assertEqual(smoke.SETUP_PROBLEM, smoke.main(["definitely-not-here.apk"]))

    def test_a_missing_adb_binary_is_a_setup_problem(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "a.apk"
            apk.write_bytes(b"x")
            self.assertEqual(smoke.SETUP_PROBLEM, smoke.main([str(apk), "--adb", "no-such-adb-binary-xyz"]))


if __name__ == "__main__":
    unittest.main()
