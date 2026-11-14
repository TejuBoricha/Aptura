#!/usr/bin/env python3
"""Audit the bundled FFmpeg audio extension against ADR-010, DEC-009, DEC-014 and LEG-02.

An earlier version of this file audited a list of flags written inside itself and always printed PASS, so it checked
nothing about what ships. This one reads what was actually built, and refuses to pass when it is given nothing to read.

Two kinds of evidence, usable together:

  --build-dir DIR   the FFmpeg build directory after `configure` (its `config.h` and `config_components.h`). This is the
                    authoritative record: the licence FFmpeg decided on, the exact configure arguments, and every
                    decoder, encoder, muxer, demuxer, protocol, filter and hardware accelerator that was switched on.
  --libs PATH...    built shared libraries (files, or directories searched for `*.so`). A defence in depth that reads the
                    binary itself: decoder names (`ff_<name>_decoder`) and the licence string. It cannot see through a
                    stripped library, so a `libavcodec*` with no decoder names is a failure, not a pass.

The approved decoder list is tools/ffmpeg/approved_decoders.txt (also read by the build recipe).

Exit codes: 0 compliant, 1 violations, 2 nothing was given to check or an input could not be read.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

DEFAULT_APPROVED = Path(__file__).resolve().with_name("approved_decoders.txt")

# Parsers the approved decoders may bring along. Anything else switched on is a violation.
APPROVED_PARSERS = {"flac", "vorbis", "opus", "mpegaudio"}

FORBIDDEN_FLAGS = {
    "--enable-gpl",
    "--enable-nonfree",
    "--enable-version3",
    "--enable-network",
    "--enable-libx264",
    "--enable-libx265",
    "--enable-libvpx",
    "--enable-libdav1d",
    "--enable-libaom",
}

# FFmpeg has no `--enable-lgpl`: LGPL 2.1 is what `configure` computes when GPL, version3 and nonfree are all off, and an
# unknown option aborts it (checked against the n6.1.1 configure script). The licence itself is audited from FFMPEG_LICENSE.
MANDATORY_FLAGS = {
    "--disable-gpl",
    "--disable-nonfree",
    "--disable-version3",
    "--disable-network",
    "--enable-shared",
    "--disable-static",
    "--disable-everything",
}

# Component kinds that must be entirely off: the extension decodes audio and does nothing else.
MUST_BE_EMPTY = ("encoder", "muxer", "demuxer", "protocol", "filter", "hwaccel", "indev", "outdev")

CONFIG_FLAG = re.compile(r"^#define\s+(CONFIG_[A-Z0-9_]+)\s+([01])\s*$", re.MULTILINE)
CONFIG_STRING = re.compile(r'^#define\s+(FFMPEG_CONFIGURATION|FFMPEG_LICENSE)\s+"(.*)"\s*$', re.MULTILINE)
COMPONENT = re.compile(r"^CONFIG_([A-Z0-9_]+)_(DECODER|ENCODER|MUXER|DEMUXER|PROTOCOL|FILTER|HWACCEL|PARSER|INDEV|OUTDEV)$")
DECODER_SYMBOL = re.compile(rb"ff_([a-z0-9_]+)_decoder\b")
GPL_LICENCE = re.compile(rb"(?<![L])GPL version [23]|nonfree and unredistributable")


class InputError(Exception):
    """Something to check could not be read; distinct from a violation."""


def load_approved(path: Path) -> set[str]:
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as error:
        raise InputError(f"cannot read the approved decoder list {path}: {error}") from error
    names = {line.strip() for line in text.splitlines() if line.strip() and not line.lstrip().startswith("#")}
    if not names:
        raise InputError(f"{path} lists no decoders; an empty list would approve nothing and hide a mistake")
    return names


def audit_flags(flags: list[str], approved: set[str]) -> list[str]:
    """Audit the configure arguments themselves (as FFMPEG_CONFIGURATION recorded them)."""
    violations: list[str] = []
    seen = {flag.strip() for flag in flags}
    for flag in sorted(seen & FORBIDDEN_FLAGS):
        violations.append(f"forbidden configure flag: {flag}")
    for flag in sorted(MANDATORY_FLAGS - seen):
        violations.append(f"missing mandatory configure flag: {flag}")
    for flag in flags:
        if flag.startswith("--enable-decoder="):
            for name in (n.strip() for n in flag.split("=", 1)[1].split(",")):
                if name and name not in approved:
                    violations.append(f"configure enables a decoder that is not approved: {name}")
    return violations


def audit_build_dir(build_dir: Path, approved: set[str]) -> list[str]:
    """Audit `config.h` and `config_components.h` as written by FFmpeg's configure."""
    try:
        config_h = (build_dir / "config.h").read_text(encoding="utf-8", errors="replace")
        components_h = (build_dir / "config_components.h").read_text(encoding="utf-8", errors="replace")
    except OSError as error:
        raise InputError(f"cannot read the FFmpeg build record in {build_dir}: {error}") from error

    violations: list[str] = []
    switches = dict(CONFIG_FLAG.findall(config_h))
    strings = dict(CONFIG_STRING.findall(config_h))
    if not switches:
        raise InputError(f"{build_dir / 'config.h'} has no CONFIG_ switches; this is not an FFmpeg configure record")

    for switch in ("CONFIG_GPL", "CONFIG_NONFREE", "CONFIG_VERSION3", "CONFIG_NETWORK"):
        if switches.get(switch) != "0":
            violations.append(f"{switch} is {switches.get(switch, 'missing')}, expected 0")
    licence = strings.get("FFMPEG_LICENSE")
    if licence is None:
        violations.append("config.h does not record FFMPEG_LICENSE")
    elif not licence.startswith("LGPL version 2.1"):
        violations.append(f"FFMPEG_LICENSE is {licence!r}, expected an LGPL 2.1 licence")

    configuration = strings.get("FFMPEG_CONFIGURATION")
    if configuration is None:
        violations.append("config.h does not record FFMPEG_CONFIGURATION")
    else:
        violations += audit_flags(configuration.split(), approved)

    enabled: dict[str, set[str]] = {}
    for name, value in CONFIG_FLAG.findall(components_h):
        match = COMPONENT.match(name)
        if match and value == "1":
            enabled.setdefault(match.group(2).lower(), set()).add(match.group(1).lower())
    if not any(COMPONENT.match(name) for name, _ in CONFIG_FLAG.findall(components_h)):
        raise InputError(f"{build_dir / 'config_components.h'} lists no components; cannot tell what was built")

    for decoder in sorted(enabled.get("decoder", set()) - approved):
        violations.append(f"decoder built but not approved: {decoder}")
    for decoder in sorted(approved - enabled.get("decoder", set())):
        violations.append(f"approved decoder missing from the build: {decoder}")
    for parser in sorted(enabled.get("parser", set()) - APPROVED_PARSERS):
        violations.append(f"parser built but not approved: {parser}")
    for kind in MUST_BE_EMPTY:
        for name in sorted(enabled.get(kind, set())):
            violations.append(f"{kind} built but none is allowed: {name}")
    return violations


def find_libraries(paths: list[Path]) -> list[Path]:
    found: list[Path] = []
    for path in paths:
        if path.is_dir():
            found += sorted(path.rglob("*.so"))
        elif path.is_file():
            found.append(path)
        else:
            raise InputError(f"no such library or directory: {path}")
    if not found:
        raise InputError("no shared libraries (*.so) were found to inspect")
    return found


def audit_libraries(libraries: list[Path], approved: set[str]) -> list[str]:
    """Read the binaries: decoder names and the licence string. Stripped libavcodec is a failure, not a pass."""
    violations: list[str] = []
    for library in libraries:
        try:
            data = library.read_bytes()
        except OSError as error:
            raise InputError(f"cannot read {library}: {error}") from error
        label = f"{library.parent.name}/{library.name}"  # the parent is the ABI folder in a jniLibs or lib tree
        decoders = {m.group(1).decode() for m in DECODER_SYMBOL.finditer(data)}
        for decoder in sorted(decoders - approved):
            violations.append(f"{label}: contains a decoder that is not approved: {decoder}")
        if GPL_LICENCE.search(data):
            violations.append(f"{label}: carries a GPL or nonfree licence string")
        if library.name.startswith("libavcodec") and not decoders:
            violations.append(
                f"{label}: no decoder names found, so nothing can be proven (stripped? audit the build dir instead)"
            )
    return violations


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--build-dir", type=Path, action="append", default=[], help="FFmpeg build directory (config.h)")
    parser.add_argument("--libs", type=Path, nargs="+", default=[], help="built .so files or directories of them")
    parser.add_argument("--approved", type=Path, default=DEFAULT_APPROVED, help="approved decoder list")
    args = parser.parse_args(argv)

    if not args.build_dir and not args.libs:
        print("nothing to check: give --build-dir and/or --libs (a check with no input must not pass)")
        return 2
    try:
        approved = load_approved(args.approved)
        violations: list[str] = []
        for build_dir in args.build_dir:
            violations += [f"{build_dir}: {v}" for v in audit_build_dir(build_dir, approved)]
        if args.libs:
            violations += audit_libraries(find_libraries(args.libs), approved)
    except InputError as error:
        print(f"cannot check: {error}")
        return 2

    if violations:
        print(f"[FAIL] {len(violations)} violation(s) of the LGPL audio-only policy:")
        for violation in violations:
            print(f"  - {violation}")
        return 1
    print(f"[PASS] {len(args.build_dir)} build record(s) and {len(args.libs)} library path(s) match the policy")
    return 0


if __name__ == "__main__":
    sys.exit(main())
