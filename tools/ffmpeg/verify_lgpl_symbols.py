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
import shlex
import struct
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
        # FFmpeg records the arguments shell-quoted, for example --enable-decoder='alac,pcm_alaw' (seen in a real CI build),
        # inside a C string literal whose own quotes and backslashes are escaped. Undo both before looking at the flags.
        try:
            arguments = shlex.split(configuration.replace('\\"', '"').replace("\\\\", "\\"))
        except ValueError as error:
            violations.append(f"FFMPEG_CONFIGURATION cannot be parsed ({error}), so the flags cannot be audited")
        else:
            violations += audit_flags(arguments, approved)

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


PAGE_16K = 16384
VERSIONED_SO = re.compile(r"\.so\.\d")


def elf_info(data: bytes) -> dict | None:
    """Just enough of an ELF reader for the Play and APK rules: LOAD alignment, SONAME and NEEDED. None if not little-endian ELF."""
    if len(data) < 64 or data[:4] != b"\x7fELF" or data[5] != 1:
        return None
    is64 = data[4] == 2
    try:
        if is64:
            phoff, = struct.unpack_from("<Q", data, 0x20)
            phentsize, phnum = struct.unpack_from("<HH", data, 0x36)
        else:
            phoff, = struct.unpack_from("<I", data, 0x1C)
            phentsize, phnum = struct.unpack_from("<HH", data, 0x2A)
        loads: list[tuple[int, int, int, int]] = []  # vaddr, offset, filesz, align
        dynamic: tuple[int, int] | None = None
        for i in range(phnum):
            base = phoff + i * phentsize
            ptype, = struct.unpack_from("<I", data, base)
            if is64:
                offset, vaddr, _paddr, filesz, _memsz, align = struct.unpack_from("<QQQQQQ", data, base + 8)
            else:
                offset, vaddr, _paddr, filesz, _memsz, _flags, align = struct.unpack_from("<IIIIIII", data, base + 4)
            if ptype == 1:
                loads.append((vaddr, offset, filesz, align))
            elif ptype == 2:
                dynamic = (offset, filesz)
        needed_offsets: list[int] = []
        soname_offset: int | None = None
        strtab_vaddr: int | None = None
        if dynamic:
            entry = 16 if is64 else 8
            fmt = "<qQ" if is64 else "<iI"
            for pos in range(dynamic[0], dynamic[0] + dynamic[1], entry):
                tag, value = struct.unpack_from(fmt, data, pos)
                if tag == 0:
                    break
                if tag == 1:
                    needed_offsets.append(value)
                elif tag == 14:
                    soname_offset = value
                elif tag == 5:
                    strtab_vaddr = value

        def string_at(string_offset: int) -> str | None:
            if strtab_vaddr is None:
                return None
            for vaddr, offset, filesz, _align in loads:
                if vaddr <= strtab_vaddr < vaddr + filesz:
                    start = offset + (strtab_vaddr - vaddr) + string_offset
                    end = data.index(b"\0", start)
                    return data[start:end].decode("ascii", "replace")
            return None

        return {
            "is64": is64,
            "aligns": [load[3] for load in loads],
            "soname": string_at(soname_offset) if soname_offset is not None else None,
            "needed": [name for name in (string_at(o) for o in needed_offsets) if name],
        }
    except (struct.error, ValueError):
        return None


def audit_elf(label: str, data: bytes) -> list[str]:
    """Rules a shipped native library must meet: 16 KB pages on 64-bit (Play), and names Android can load from an APK."""
    info = elf_info(data)
    if info is None:
        return []
    violations: list[str] = []
    if info["is64"] and any(align < PAGE_16K for align in info["aligns"]):
        violations.append(f"{label}: a LOAD segment is aligned to less than 16 KB (Play requires 16 KB pages for 64-bit)")
    soname = info["soname"]
    if soname and not soname.endswith(".so"):
        violations.append(f"{label}: SONAME {soname!r} does not end in .so, so an APK cannot carry it")
    for name in info["needed"]:
        if VERSIONED_SO.search(name):
            violations.append(f"{label}: depends on {name!r}, a versioned name an APK cannot carry")
    return violations


DECODER_OBJECT = re.compile(rb"^ff_([a-z0-9_]+)_decoder$")


def elf_decoder_objects(data: bytes) -> set[str] | None:
    """Decoder names from the ELF symbol table, counting only data objects.

    A real decoder is a `const FFCodec ff_<name>_decoder` object. FFmpeg 8 also has a helper function called `ff_codec_is_decoder`,
    which a plain text search reads as a decoder named "codec_is"; its symbol type (function, not object) tells them apart.
    Returns None when there is no symbol table to read (not an ELF, or stripped), so the caller can fall back to a text search.
    """
    if len(data) < 64 or data[:4] != b"\x7fELF" or data[5] != 1:
        return None
    is64 = data[4] == 2
    try:
        if is64:
            shoff, = struct.unpack_from("<Q", data, 0x28)
            shentsize, shnum = struct.unpack_from("<HH", data, 0x3A)
        else:
            shoff, = struct.unpack_from("<I", data, 0x20)
            shentsize, shnum = struct.unpack_from("<HH", data, 0x2E)
        if not shoff or not shnum:
            return None
        sections: list[tuple[int, int, int, int]] = []  # type, file offset, size, linked section
        for i in range(shnum):
            base = shoff + i * shentsize
            if is64:
                _name, kind, _flags, _addr, offset, size, link, _info, _align, _entsize = struct.unpack_from("<IIQQQQIIQQ", data, base)
            else:
                _name, kind, _flags, _addr, offset, size, link, _info, _align, _entsize = struct.unpack_from("<IIIIIIIIII", data, base)
            sections.append((kind, offset, size, link))
        names: set[str] = set()
        found_table = False
        for kind, offset, size, link in sections:
            if kind not in (2, 11):  # SHT_SYMTAB, SHT_DYNSYM
                continue
            found_table = True
            strings_at = sections[link][1]
            step = 24 if is64 else 16
            for position in range(offset, offset + size, step):
                if is64:
                    st_name, st_info, _other, _shndx, _value, _size = struct.unpack_from("<IBBHQQ", data, position)
                else:
                    st_name, _value, _size, st_info, _other, _shndx = struct.unpack_from("<IIIBBH", data, position)
                if not st_name or (st_info & 0xF) != 1:  # STT_OBJECT only
                    continue
                start = strings_at + st_name
                match = DECODER_OBJECT.match(data[start:data.index(b"\0", start)])
                if match:
                    names.add(match.group(1).decode())
        return names if found_table else None
    except (struct.error, ValueError, IndexError):
        return None


def audit_libraries(libraries: list[Path], approved: set[str]) -> list[str]:
    """Read the binaries: decoder names, the licence string and the ELF rules. Stripped libavcodec is a failure, not a pass."""
    violations: list[str] = []
    for library in libraries:
        try:
            data = library.read_bytes()
        except OSError as error:
            raise InputError(f"cannot read {library}: {error}") from error
        label = f"{library.parent.name}/{library.name}"  # the parent is the ABI folder in a jniLibs or lib tree
        in_symbol_table = elf_decoder_objects(data)
        decoders = in_symbol_table if in_symbol_table is not None else {m.group(1).decode() for m in DECODER_SYMBOL.finditer(data)}
        for decoder in sorted(decoders - approved):
            violations.append(f"{label}: contains a decoder that is not approved: {decoder}")
        if GPL_LICENCE.search(data):
            violations.append(f"{label}: carries a GPL or nonfree licence string")
        violations += audit_elf(label, data)
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
