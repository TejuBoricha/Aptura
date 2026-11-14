#!/usr/bin/env python3
"""Decide whether a change can alter what the shipped, R8-minified release build does.

tools/gate.ps1 asks this before it runs the slow release builds (assembleRelease and bundleRelease, which are
finalized by verifyReleaseApk and verifyReleaseBundle). CI runs those two builds on every pull request, and merging
needs CI, so locally they are an early warning. A change to Kotlin sources, tests or docs cannot change the
dependency set, the shrinker configuration or the manifest, so the local release builds are skipped for it. A change
to any file below can, so they run.

Reads changed repository paths from stdin, one per line (forward or back slashes).
Exit code 0: release builds needed, reasons on stdout.
Exit code 1: release builds can be skipped.
Exit code 2: could not decide (nothing to look at, or a read error). The caller must treat this as "needed".

The list is deliberately short and anchored at the repository root. If a file that defines the release build is
renamed, test_release_scope.py fails instead of the file silently becoming skippable.
"""

from __future__ import annotations

import re
import sys
from typing import Iterable

NEEDED = 0
SKIPPABLE = 1
UNDECIDABLE = 2

RULES: tuple[tuple[re.Pattern[str], str], ...] = tuple(
    (re.compile(pattern), reason)
    for pattern, reason in (
        (r"^(build|settings)\.gradle(\.kts)?$", "root Gradle script (defines the verifyRelease* invariants)"),
        (r"^app/build\.gradle(\.kts)?$", "app build script (R8, signing, dependencies)"),
        (r"^gradle/", "version catalog or Gradle wrapper"),
        (r"^(gradle\.properties|gradlew|gradlew\.bat)$", "Gradle settings or launcher"),
        (r"^(buildSrc|build-logic)/", "build logic"),
        (r"^app/(.*/)?[^/]*\.pro$", "R8/ProGuard rules"),
        (r"^app/(.*/)?proguard[^/]*$", "R8/ProGuard rules"),
        (r"^app/src/[^/]+/AndroidManifest\.xml$", "manifest (permissions are a privacy invariant)"),
        (r"^app/src/[^/]+/res/(xml|raw)/", "xml/raw resources (backup rules, resource shrinker keep file)"),
        (r"^app/src/[^/]+/(jniLibs|cpp|assets)/", "native libraries or assets packaged in the APK"),
        (r"^app/src/main/baseline-prof\.txt$", "baseline profile packaged in the APK"),
    )
)


def normalize(path: str) -> str:
    """Repository-relative path with forward slashes and no leading './'."""
    cleaned = path.strip().replace("\\", "/")
    return cleaned.removeprefix("./")


def classify(paths: Iterable[str]) -> tuple[int, list[str]]:
    """Return (exit code, human-readable reasons). Reasons are only given when the release builds are needed."""
    cleaned = [p for p in (normalize(raw) for raw in paths) if p]
    if not cleaned:
        return UNDECIDABLE, ["no changed paths were given, so the change cannot be judged"]
    reasons: list[str] = []
    for path in cleaned:
        for pattern, reason in RULES:
            if pattern.search(path):
                reasons.append(f"{path}  ({reason})")
                break
    return (NEEDED, reasons) if reasons else (SKIPPABLE, [])


def main(raw: bytes | None = None) -> int:
    try:
        data = sys.stdin.buffer.read() if raw is None else raw
        # Strict UTF-8, independent of the Windows code page. "utf-8-sig" also drops a byte-order mark: PowerShell can
        # prepend one, and it would make the first path miss its rule and wrongly look skippable.
        code, reasons = classify(data.decode("utf-8-sig").splitlines())
    except Exception as error:  # noqa: BLE001 - any failure must fall back to "run the builds"
        print(f"could not read the changed paths: {error}")
        return UNDECIDABLE
    for line in reasons:
        print(line)
    return code


if __name__ == "__main__":
    sys.exit(main())
