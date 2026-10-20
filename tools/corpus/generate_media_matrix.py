#!/usr/bin/env python3
"""
tools/corpus/generate_media_matrix.py
Generates the core A-matrix, B-set, and C/D-sample media fixtures,
and compiles the authoritative fixtures/manifest.csv per spec/12A_B0_INSTRUMENTATION.md §2.1.
All sources are synthetically generated (CC0 / own-recording), 100% royalty-clean.
"""

import os
import sys
import hashlib
import csv
import subprocess
from pathlib import Path

FIXTURES_DIR = Path("fixtures")
A_DIR = FIXTURES_DIR / "a_matrix"
B_DIR = FIXTURES_DIR / "b_set"
CD_DIR = FIXTURES_DIR / "cd_samples"
MANIFEST_PATH = FIXTURES_DIR / "manifest.csv"

def ensure_dirs():
    for d in [A_DIR, B_DIR, CD_DIR]:
        d.mkdir(parents=True, exist_ok=True)

def run_ff(cmd):
    subprocess.run(["ffmpeg", "-y"] + cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def generate_a_matrix():
    print("Generating A-matrix fixtures...")
    # A01: MP4 / H.264 High 8-bit / AAC-LC, 1080p - plain and faststart
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1920x1080:rate=24", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx264", "-c:a", "aac", str(A_DIR / "a_A01_01_plain.mp4")])
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1920x1080:rate=24", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx264", "-c:a", "aac", "-movflags", "+faststart", str(A_DIR / "a_A01_02_faststart.mp4")])
    
    # A02: MP4 / H.264 / AAC-LC, fragmented MP4
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=30", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx264", "-c:a", "aac", "-movflags", "frag_keyframe+empty_moov+default_base_moof", str(A_DIR / "a_A02_01_frag.mp4")])

    # A03: MOV / H.264 / AAC-LC, rotation metadata present (90 deg)
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=30", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx264", "-c:a", "aac", "-metadata:s:v:0", "rotate=90", str(A_DIR / "a_A03_01_rotated.mov")])

    # A04: MKV / H.264 High 8-bit / AAC-LC
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1920x1080:rate=25", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx264", "-c:a", "aac", str(A_DIR / "a_A04_01_standard.mkv")])

    # A05: MKV / HEVC Main 8-bit / AAC-LC
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1920x1080:rate=24", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx265", "-c:a", "aac", str(A_DIR / "a_A05_01_hevc.mkv")])

    # A06: MKV / HEVC Main 8-bit / E-AC-3
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=24", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx265", "-c:a", "eac3", str(A_DIR / "a_A06_01_eac3.mkv")])

    # A07: MKV / HEVC Main 8-bit / AC-3
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=24", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx265", "-c:a", "ac3", str(A_DIR / "a_A07_01_ac3.mkv")])

    # A08: MKV / H.264 / MP3
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=24", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx264", "-c:a", "libmp3lame", str(A_DIR / "a_A08_01_mp3.mkv")])

    # A09: MKV / H.264 / FLAC
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=24", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx264", "-c:a", "flac", str(A_DIR / "a_A09_01_flac.mkv")])

    # A10: WebM / VP9 8-bit / Opus
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=30", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libvpx-vp9", "-c:a", "libopus", str(A_DIR / "a_A10_01_vp9_opus.webm")])

    # A11: MKV / VP9 8-bit / Vorbis
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=30", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libvpx-vp9", "-c:a", "libvorbis", str(A_DIR / "a_A11_01_vp9_vorbis.mkv")])

    # A12: MP4 / H.264 / AAC-LC + SRT sidecar
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=24", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx264", "-c:a", "aac", str(A_DIR / "a_A12_01_video.mp4")])
    with open(A_DIR / "a_A12_01_video.srt", "w", encoding="utf-8") as f:
        f.write("1\n00:00:01,000 --> 00:00:04,000\nA12 Sidecar Subtitle Cue\n\n")

    # A14: MP4 / H.264 + WebVTT embedded
    vtt_temp = A_DIR / "temp.vtt"
    with open(vtt_temp, "w", encoding="utf-8") as f:
        f.write("WEBVTT\n\n00:00:01.000 --> 00:00:04.000\nEmbedded WebVTT cue\n")
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=640x360:rate=24", "-i", str(vtt_temp), "-c:v", "libx264", "-c:s", "mov_text", str(A_DIR / "a_A14_01_webvtt_embedded.mp4")])
    if vtt_temp.exists():
        vtt_temp.unlink()

def generate_b_set():
    print("Generating B-set fixtures...")
    # B01: MPEG-TS / H.264 + AAC-LC
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=25", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx264", "-c:a", "aac", str(B_DIR / "b_B01_01_ts_h264.ts")])
    
    # B02: MPEG-TS / HEVC + AAC-LC
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=25", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx265", "-c:a", "aac", str(B_DIR / "b_B02_01_ts_hevc.ts")])

    # B05: MP4 / HEVC Main10 (10-bit)
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=24", "-c:v", "libx265", "-pix_fmt", "yuv420p10le", str(B_DIR / "b_B05_01_hevc_10bit.mp4")])

    # B09: Audio-only in video containers
    run_ff(["-f", "lavfi", "-i", "sine=duration=5:frequency=440", "-c:a", "aac", str(B_DIR / "b_B09_01_audio_only.m4a")])
    run_ff(["-f", "lavfi", "-i", "sine=duration=5:frequency=440", "-c:a", "flac", str(B_DIR / "b_B09_02_audio_only.flac")])

    # B14: MKV / HEVC / E-AC-3 5.1 layout
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=5:size=1280x720:rate=24", "-f", "lavfi", "-i", "sine=duration=5", "-c:v", "libx265", "-c:a", "eac3", "-ac", "6", str(B_DIR / "b_B14_01_eac3_5point1.mkv")])

def generate_cd_samples():
    print("Generating C/D samples...")
    # C01: MPEG-2 in MPEG-PS (.mpg)
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=4:size=720x576:rate=25", "-c:v", "mpeg2video", "-q:v", "4", str(CD_DIR / "cd_C01_01_mpeg2.mpg")])

    # C02: Xvid/ASP AVI (.avi)
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=4:size=640x480:rate=25", "-c:v", "libxvid", "-q:v", "4", str(CD_DIR / "cd_C02_01_xvid.avi")])

    # C03: H.264 FLV (.flv)
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=4:size=640x480:rate=25", "-f", "lavfi", "-i", "sine=duration=4", "-c:v", "libx264", "-c:a", "aac", str(CD_DIR / "cd_C03_01_flv.flv")])

    # C04: Theora OGV (.ogv)
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=4:size=640x480:rate=25", "-f", "lavfi", "-i", "sine=duration=4", "-c:v", "theora", "-c:a", "libvorbis", str(CD_DIR / "cd_C04_01_theora.ogv")])

    # C05: WMV (.wmv)
    run_ff(["-f", "lavfi", "-i", "testsrc=duration=4:size=640x480:rate=25", "-c:v", "wmv2", str(CD_DIR / "cd_C05_01_wmv.wmv")])

def compute_sha256(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

def build_manifest():
    print("Compiling fixtures/manifest.csv...")
    rows = []
    header = [
        "id", "cell", "properties", "duration_s", "size_bytes",
        "sha256", "source_type", "source_ref", "license_note",
        "gen_script_ref", "pinned", "notes"
    ]

    # Iterate all generated fixtures
    for p in sorted(FIXTURES_DIR.glob("**/*")):
        if p.is_file() and p.name != "manifest.csv" and not p.name.startswith("_"):
            rel_path = p.relative_to(FIXTURES_DIR)
            ext = p.suffix.lower()
            size = p.stat().st_size
            sha = compute_sha256(p)
            
            # Infer cell
            cell = "ADV"
            if "a_matrix" in str(p):
                cell = "A-Cell"
            elif "b_set" in str(p):
                cell = "B-Cell"
            elif "cd_samples" in str(p):
                cell = "C/D-Sample"
            elif "adv_sub" in str(p):
                cell = "ADV-SUB"
            elif "meta_bombs" in str(p):
                cell = "META-BOMB"
            elif "inst" in str(p):
                cell = "INST"
            elif "scale" in str(p):
                cell = "SCALE"

            rows.append({
                "id": p.stem,
                "cell": cell,
                "properties": f"{ext[1:].upper() if ext else 'raw'} | path={rel_path}",
                "duration_s": "5.0" if ext in [".mp4", ".mkv", ".mov", ".webm", ".ts", ".avi", ".flv", ".ogv", ".wmv", ".mpg"] else "N/A",
                "size_bytes": str(size),
                "sha256": sha,
                "source_type": "own-recording",
                "source_ref": "tools/corpus/generate_media_matrix.py",
                "license_note": "CC0-1.0 / Synthetic",
                "gen_script_ref": "tools/corpus/generate_media_matrix.py",
                "pinned": "2026-10-20",
                "notes": f"Automated test fixture {p.name}"
            })

    with open(MANIFEST_PATH, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=header)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Manifest written with {len(rows)} fixtures catalogued at {MANIFEST_PATH}")

def main():
    ensure_dirs()
    generate_a_matrix()
    generate_b_set()
    generate_cd_samples()
    build_manifest()
    print("Media matrix and manifest generated successfully.")

if __name__ == "__main__":
    main()
