#!/usr/bin/env python3
"""Smoke-test the shipped release APK on a running emulator or device.

Why this exists: CI builds the R8-minified release (the `release` job) but never starts it, so a shrinker problem that
only shows at run time (a class R8 removed that is reached by reflection, a serializer, a service) would pass every
check. Instrumented tests against the release build do not fix that: they need extra keep rules for the test runner,
which changes what R8 produces, so the thing tested is no longer the thing that ships. This script installs the real,
signed release APK and only drives it from outside.

What it does, in order:
  1. waits for the device to finish booting and installs the APK;
  2. refuses an APK that is debuggable (that would test the wrong artifact);
  3. grants media access and adds the two sample clips from app/src/androidTest/assets;
  4. cold-starts the app and checks it is alive;
  5. runs the UI exerciser (`monkey`, fixed seed) and checks it is still alive;
  6. opens a clip for playback and checks it is still alive;
  7. scans logcat for a fatal exception, an ANR or a native crash of the app.

Exit code 0: nothing wrong found. 1: the app misbehaved (crash, ANR, died). 2: the check could not run (no device,
install failed, wrong APK, missing clips). CI treats both 1 and 2 as a failure.

Usage: python3 tools/smoke/release_smoke.py APK [--adb adb] [--serial SERIAL] [--logcat-out FILE]
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable

PACKAGE = "org.aptura.player"
MONKEY_EVENTS = 400
MONKEY_SEED = 20261114
CLIPS = ("smoke_clip.mp4", "smoke_hevc.mp4")
MEDIA_PERMISSIONS = (
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.READ_MEDIA_IMAGES",
)
ROOT = Path(__file__).resolve().parents[2]

OK, APP_PROBLEM, SETUP_PROBLEM = 0, 1, 2


# --- pure decisions (unit-tested) -------------------------------------------------------------------------------


def package_flags(dumpsys_package: str) -> set[str] | None:
    """Flags of the installed package, e.g. {"HAS_CODE", "DEBUGGABLE"}. None when no flags line can be found."""
    found: set[str] | None = None
    for match in re.finditer(r"\b(?:pkgFlags|flags)=\[([^\]]*)\]", dumpsys_package):
        found = (found or set()) | set(match.group(1).split())
    return found


def crash_findings(logcat: str, package: str = PACKAGE) -> list[str]:
    """Fatal exceptions, ANRs and native crashes of `package` in a logcat dump. Other apps' crashes are ignored."""
    lines = logcat.splitlines()
    findings: list[str] = []
    for index, line in enumerate(lines):
        if "FATAL EXCEPTION" in line:
            window = lines[index + 1 : index + 4]
            if any(f"Process: {package}" in following for following in window):
                exception = next((w for w in window if "Process:" not in w), "")
                findings.append(f"fatal exception: {_tail(exception) or _tail(line)}")
        elif re.search(rf"\bANR in {re.escape(package)}\b", line):
            findings.append(f"ANR: {_tail(line)}")
        elif "Fatal signal" in line and re.search(rf"\b{re.escape(package)}\b", line):
            findings.append(f"native crash: {_tail(line)}")
    return findings


def monkey_findings(output: str, expected_events: int = MONKEY_EVENTS) -> list[str]:
    """Problems reported by the UI exerciser. An empty or cut-short run is a problem, not a pass."""
    findings = [f"monkey: {_tail(line)}" for line in output.splitlines()
                if line.startswith("// CRASH") or "NOT RESPONDING" in line or "Monkey aborted" in line]
    injected = re.search(r"Events injected:\s*(\d+)", output)
    if not injected:
        findings.append("monkey: no 'Events injected' line, the run did not finish")
    elif int(injected.group(1)) < expected_events:
        findings.append(f"monkey: only {injected.group(1)} of {expected_events} events were injected")
    return findings


def media_id(content_query: str, display_name: str) -> str | None:
    """MediaStore _id of the row with this display name, from `content query --projection _id:_display_name`."""
    match = re.search(rf"_id=(\d+),\s*_display_name={re.escape(display_name)}\b", content_query)
    return match.group(1) if match else None


def _tail(text: str, limit: int = 220) -> str:
    return text.strip()[:limit]


# --- the run (adb and sleep are injected so the whole flow is testable without a device) ------------------------

Adb = Callable[..., str]


@dataclass
class Result:
    code: int = OK
    failures: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)
    logcat: str = ""


def _alive(adb: Adb, package: str) -> bool:
    return adb("shell", "pidof", package).strip().split()[:1] != []


def run_smoke(
    adb: Adb,
    apk: Path,
    clips_dir: Path,
    sleep: Callable[[float], None] = time.sleep,
    package: str = PACKAGE,
    boot_attempts: int = 60,
) -> Result:
    result = Result()

    def setup_problem(message: str) -> Result:
        result.code = SETUP_PROBLEM
        result.failures.append(message)
        return result

    for _ in range(boot_attempts):
        if adb("shell", "getprop", "sys.boot_completed").strip() == "1":
            break
        sleep(5)
    else:
        return setup_problem("the device did not finish booting")

    missing = [name for name in CLIPS if not (clips_dir / name).is_file()]
    if missing:
        return setup_problem(f"sample clips not found in {clips_dir}: {', '.join(missing)}")

    adb("uninstall", package)
    if "Success" not in adb("install", "-r", str(apk)):
        return setup_problem(f"could not install {apk}")

    flags = package_flags(adb("shell", "dumpsys", "package", package))
    if flags is None:
        return setup_problem("could not read the installed package's flags, so it cannot be shown to be a release build")
    if "DEBUGGABLE" in flags:
        return setup_problem("the installed APK is debuggable: this is not the release build that ships")

    for permission in MEDIA_PERMISSIONS:
        adb("shell", "pm", "grant", package, permission)  # not every API level declares all three
    adb("shell", "mkdir", "-p", "/sdcard/Movies")
    for name in CLIPS:
        adb("push", str(clips_dir / name), f"/sdcard/Movies/{name}")
        adb("shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE",
            "-d", f"file:///sdcard/Movies/{name}")
    sleep(4)

    adb("logcat", "-c")

    adb("shell", "monkey", "-p", package, "-c", "android.intent.category.LAUNCHER", "1")
    sleep(12)
    if not _alive(adb, package):
        result.failures.append("the app is not running 12 s after a cold start")

    monkey_out = adb("shell", "monkey", "-p", package, "--throttle", "200", "--ignore-security-exceptions",
                     "-s", str(MONKEY_SEED), str(MONKEY_EVENTS), timeout=600)
    result.failures += monkey_findings(monkey_out)
    if not _alive(adb, package):
        result.failures.append("the app is not running after the UI exerciser")

    clip_id = media_id(adb("shell", "content", "query", "--uri", "content://media/external/video/media",
                           "--projection", "_id:_display_name"), CLIPS[0])
    if clip_id is None:
        result.notes.append(f"{CLIPS[0]} was not indexed by MediaStore; the playback step was not exercised")
    else:
        adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d",
            f"content://media/external/video/media/{clip_id}", "-t", "video/mp4", package)
        sleep(10)
        if not _alive(adb, package):
            result.failures.append("the app is not running after opening a clip for playback")
        session = adb("shell", "dumpsys", "media_session")
        result.notes.append("a media session of the app is active after opening the clip"
                            if package in session else "no media session of the app was found after opening the clip")

    result.logcat = adb("logcat", "-d", "-v", "brief")
    result.failures += crash_findings(result.logcat, package)
    result.code = APP_PROBLEM if result.failures else OK
    return result


def make_adb(adb_path: str, serial: str | None) -> Adb:
    base = [adb_path] + (["-s", serial] if serial else [])

    def run(*args: str, timeout: int = 180) -> str:
        completed = subprocess.run(base + list(args), capture_output=True, text=True, errors="replace",
                                   timeout=timeout, check=False)
        return (completed.stdout or "") + (completed.stderr or "")

    return run


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("apk", type=Path, help="the signed release APK for the device's ABI")
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", default=None)
    parser.add_argument("--clips", type=Path, default=ROOT / "app" / "src" / "androidTest" / "assets")
    parser.add_argument("--logcat-out", type=Path, default=None, help="where to save the logcat of the run")
    args = parser.parse_args(argv)

    if not args.apk.is_file():
        print(f"APK not found: {args.apk}")
        return SETUP_PROBLEM
    try:
        result = run_smoke(make_adb(args.adb, args.serial), args.apk, args.clips)
    except (OSError, subprocess.SubprocessError) as error:
        print(f"could not run adb: {error}")
        return SETUP_PROBLEM

    if args.logcat_out and result.logcat:
        args.logcat_out.write_text(result.logcat, encoding="utf-8")
    for note in result.notes:
        print(f"note: {note}")
    if result.failures:
        print("RELEASE SMOKE FAILED:" if result.code == APP_PROBLEM else "RELEASE SMOKE COULD NOT RUN:")
        for failure in result.failures:
            print(f"  - {failure}")
    else:
        print("release smoke passed: cold start, UI exerciser and playback ran without a crash, ANR or native crash")
    return result.code


if __name__ == "__main__":
    sys.exit(main())
