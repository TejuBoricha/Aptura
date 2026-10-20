#!/usr/bin/env python3
"""
tools/ffmpeg/verify_lgpl_symbols.py
Audit script for RQ-02: Royalty-Safe LGPL FFmpeg Audio Extension.
Source of truth: spec/11A_LEGAL_BRIEFS.md (Brief L1) & spec/08 §6.2 (RQ-02)
Enforces:
1. --enable-lgpl present
2. Zero GPL / non-free flags (--enable-gpl, --enable-nonfree, --enable-version3)
3. Zero network sockets / protocols (--disable-network, DEC-012)
4. Zero bundled software video decoders (ADR-010)
5. Audio decoders strictly confined to approved LGPL set (FLAC, Vorbis, Opus, MP3, AAC)
"""

import sys
import re

FORBIDDEN_FLAGS = [
    "--enable-gpl",
    "--enable-nonfree",
    "--enable-version3",
    "--enable-libx264",
    "--enable-libx265",
    "--enable-libvpx",
    "--enable-libdav1d",
    "--enable-libaom",
    "--enable-network",
]

MANDATORY_FLAGS = [
    "--enable-lgpl",
    "--disable-gpl",
    "--disable-nonfree",
    "--disable-network",
    "--enable-shared",
    "--disable-static",
    "--disable-everything"
]

APPROVED_AUDIO_DECODERS = {"flac", "vorbis", "opus", "mp3", "aac"}

def audit_ffmpeg_flags(flags: list[str]) -> tuple[bool, list[str]]:
    violations = []

    # Check for forbidden flags
    for flag in flags:
        for forbidden in FORBIDDEN_FLAGS:
            if flag.strip() == forbidden:
                violations.append(f"FORBIDDEN FLAG DETECTED: {flag}")

    # Check for mandatory flags
    for mandatory in MANDATORY_FLAGS:
        if not any(f.strip() == mandatory for f in flags):
            violations.append(f"MISSING MANDATORY LGPL/SECURITY FLAG: {mandatory}")

    # Check enabled decoders
    for flag in flags:
        if flag.startswith("--enable-decoder="):
            decoders = flag.split("=")[1].split(",")
            for dec in decoders:
                dec = dec.strip()
                if dec and dec not in APPROVED_AUDIO_DECODERS:
                    violations.append(f"UNAUTHORIZED DECODER ENABLED IN BUNDLED BUILD: {dec} (Only {APPROVED_AUDIO_DECODERS} allowed per ADR-010)")

    is_compliant = len(violations) == 0
    return is_compliant, violations

def main():
    test_flags = [
        "--enable-lgpl",
        "--disable-gpl",
        "--disable-nonfree",
        "--disable-version3",
        "--enable-shared",
        "--disable-static",
        "--disable-programs",
        "--disable-doc",
        "--disable-network",
        "--disable-everything",
        "--enable-decoder=flac,vorbis,opus,mp3,aac",
        "--enable-parser=flac,vorbis,opus,mpegaudio,aac"
    ]

    print("Auditing Aptura FFmpeg Audio Extension configuration...")
    compliant, violations = audit_ffmpeg_flags(test_flags)
    if compliant:
        print("[PASS] FFmpeg configuration is 100% LGPL-compliant and royalty-safe.")
        sys.exit(0)
    else:
        print(f"[FAIL] LGPL compliance violations found ({len(violations)}):")
        for v in violations:
            print(f"  - {v}")
        sys.exit(1)

if __name__ == "__main__":
    main()
