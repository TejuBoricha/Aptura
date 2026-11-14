#!/usr/bin/env python3
"""Fail when a library in the shipped release build has a published security advisory.

The release build's resolved runtime dependency list (`writeReleaseRuntimeDependencies`, one `group:artifact:version <- ...`
line per library) is checked against the OSV database (https://osv.dev), which aggregates the GitHub Advisory Database
and others. Only public library coordinates leave the machine.

Why this exists: Dependabot only sees what the build files declare. A library that arrives through another one (Guava,
through Media3, which is held at 1.5.x on purpose) never gets a security update, so the advisory has to be looked for.

Exit codes, so a finding and an outage are never confused:
  0  no advisory applies to the shipped versions
  1  at least one advisory applies (named on stdout)
  2  bad input: unreadable or empty report, malformed allowlist (a check that has nothing to check must not pass)
  3  OSV could not be reached or answered nonsense (nothing was proven either way)

An advisory the project has looked at and accepted goes in the allowlist with an expiry date, so it comes back for review:

    GHSA-xxxx-xxxx-xxxx  2026-12-31  why it does not apply here
"""

from __future__ import annotations

import argparse
import datetime
import json
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Iterable, Protocol

CLEAN, FOUND, BAD_INPUT, UNREACHABLE = 0, 1, 2, 3

OSV_BATCH_URL = "https://api.osv.dev/v1/querybatch"
OSV_VULN_URL = "https://api.osv.dev/v1/vulns/"
BATCH_SIZE = 500

DEPENDENCY_LINE = re.compile(r"^([\w.\-]+):([\w.\-]+):(\S+) <-")
ALLOWLIST_LINE = re.compile(r"^(\S+)\s+(\d{4}-\d{2}-\d{2})\s+(\S.*)$")

Coordinate = tuple[str, str, str]


class Transport(Protocol):
    def post_json(self, url: str, body: dict) -> dict: ...

    def get_json(self, url: str) -> dict: ...


class HttpTransport:
    """The real thing: a few retries with a growing pause, because a scheduled job should not fail on one dropped packet."""

    def __init__(self, attempts: int = 3, timeout: float = 60.0) -> None:
        self.attempts = attempts
        self.timeout = timeout
        self.headers = {"User-Agent": "aptura-dependency-advisory-check/1.0", "Content-Type": "application/json"}

    def _open(self, request: urllib.request.Request) -> dict:
        last: Exception | None = None
        for attempt in range(self.attempts):
            try:
                with urllib.request.urlopen(request, timeout=self.timeout) as response:
                    return json.load(response)
            except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
                last = error
                time.sleep(2 * (attempt + 1))
        raise OSError(f"OSV did not answer after {self.attempts} attempts: {last}")

    def post_json(self, url: str, body: dict) -> dict:
        return self._open(urllib.request.Request(url, json.dumps(body).encode(), self.headers))

    def get_json(self, url: str) -> dict:
        return self._open(urllib.request.Request(url, headers=self.headers))


def parse_report(text: str) -> list[Coordinate]:
    """Every `group:artifact:version <- ...` line; the header comment and blank lines are skipped."""
    found: list[Coordinate] = []
    for line in text.splitlines():
        match = DEPENDENCY_LINE.match(line)
        if match:
            found.append((match.group(1), match.group(2), match.group(3)))
    return found


def parse_allowlist(text: str, today: datetime.date) -> tuple[set[str], list[str]]:
    """Returns (ids still accepted, ids whose acceptance has run out). A malformed line is an error, never skipped."""
    active: set[str] = set()
    expired: list[str] = []
    for number, raw in enumerate(text.splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        match = ALLOWLIST_LINE.match(line)
        if not match:
            raise ValueError(f"allowlist line {number} is not `ID  YYYY-MM-DD  reason`: {raw!r}")
        advisory_id, expires, _reason = match.groups()
        try:
            expiry = datetime.date.fromisoformat(expires)
        except ValueError as error:
            raise ValueError(f"allowlist line {number} has an invalid date: {expires!r}") from error
        if expiry >= today:
            active.add(advisory_id)
        else:
            expired.append(advisory_id)
    return active, expired


def advisories_for(coordinates: list[Coordinate], transport: Transport) -> dict[Coordinate, list[str]]:
    """Advisory ids per coordinate, from OSV's batch endpoint. Anything but one answer per question is an error."""
    hits: dict[Coordinate, list[str]] = {}
    for start in range(0, len(coordinates), BATCH_SIZE):
        chunk = coordinates[start:start + BATCH_SIZE]
        body = {"queries": [{"package": {"name": f"{g}:{a}", "ecosystem": "Maven"}, "version": v} for g, a, v in chunk]}
        results = transport.post_json(OSV_BATCH_URL, body).get("results")
        if not isinstance(results, list) or len(results) != len(chunk):
            got = len(results) if isinstance(results, list) else "no"
            raise OSError(f"OSV returned {got} results for {len(chunk)} questions")
        for coordinate, result in zip(chunk, results):
            ids = [entry["id"] for entry in (result or {}).get("vulns", []) if "id" in entry]
            if ids:
                hits[coordinate] = ids
    return hits


def check(
    coordinates: list[Coordinate],
    transport: Transport,
    allowed: set[str],
    expired: Iterable[str],
) -> tuple[int, list[str]]:
    expired_ids = set(expired)
    lines: list[str] = []
    for (group, artifact, version), ids in sorted(advisories_for(coordinates, transport).items()):
        for advisory_id in ids:
            if advisory_id in allowed:
                continue
            details = transport.get_json(OSV_VULN_URL + advisory_id)
            if details.get("withdrawn"):
                continue
            summary = str(details.get("summary", "")).strip() or "no summary"
            note = " (the allowlist entry for it has expired)" if advisory_id in expired_ids else ""
            lines.append(f"{group}:{artifact}:{version}  {advisory_id}  {summary}{note}")
    return (FOUND if lines else CLEAN), lines


def main(argv: list[str] | None = None, transport: Transport | None = None, today: datetime.date | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("report", type=Path, help="release-runtime-dependencies.txt from writeReleaseRuntimeDependencies")
    parser.add_argument("--allowlist", type=Path, default=Path(__file__).with_name("osv_allowlist.txt"))
    args = parser.parse_args(argv)

    try:
        coordinates = parse_report(args.report.read_text(encoding="utf-8"))
    except OSError as error:
        print(f"cannot read the dependency report: {error}")
        return BAD_INPUT
    if not coordinates:
        print(f"{args.report} lists no libraries; refusing to report a clean result for nothing")
        return BAD_INPUT

    try:
        allowlist_text = args.allowlist.read_text(encoding="utf-8") if args.allowlist.exists() else ""
        allowed, expired = parse_allowlist(allowlist_text, today or datetime.date.today())
    except (OSError, ValueError) as error:
        print(f"bad allowlist {args.allowlist}: {error}")
        return BAD_INPUT

    try:
        code, lines = check(coordinates, transport or HttpTransport(), allowed, expired)
    except OSError as error:
        print(f"could not check against OSV, so nothing is proven either way: {error}")
        return UNREACHABLE

    if code == FOUND:
        print(f"{len(lines)} known advisory(ies) apply to the release build ({len(coordinates)} libraries checked):")
        for line in lines:
            print(f"  {line}")
        print("Raise the version (a constraint in app/build.gradle.kts works for a transitive library) or, after "
              "judging it not reachable, add it to tools/security/osv_allowlist.txt with an expiry date.")
    else:
        print(f"no known advisory applies to the {len(coordinates)} libraries in the release build")
    return code


if __name__ == "__main__":
    sys.exit(main())
