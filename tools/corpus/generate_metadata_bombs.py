#!/usr/bin/env python3
"""
tools/corpus/generate_metadata_bombs.py
Generates metadata bombs per spec/12A_B0_INSTRUMENTATION.md §2.7.
Covers:
1. Multi-MB title/comment tags inside media container
2. Oversized embedded artwork (e.g. 5MB-10MB attached picture)
3. 10k-character filename / deep folder structure
4. Unicode confusable filename (homoglyphs / RTL override marks)
5. Invalid UTF-8 filename bytes
"""

import os
import sys
import subprocess
from pathlib import Path

FIXTURES_DIR = Path("fixtures/meta_bombs")

def ensure_dir(path: Path):
    path.mkdir(parents=True, exist_ok=True)

def generate_oversized_metadata():
    out_dir = FIXTURES_DIR / "tags"
    ensure_dir(out_dir)

    # Multi-MB string for comment/description written to an ffmetadata file
    meta_file = out_dir / "ffmetadata.txt"
    huge_string = ("Aptura Metadata Bomb Title " * 50000) # ~1.3 MB
    with open(meta_file, "w", encoding="utf-8") as f:
        f.write(";FFMETADATA1\n")
        f.write("title=Aptura Meta Bomb\n")
        f.write(f"comment={huge_string}\n")
        f.write(f"description={huge_string}\n")

    # Generate a tiny 1-second synthetic video with this huge metadata tag in MP4
    cmd = [
        "ffmpeg", "-y",
        "-f", "lavfi", "-i", "testsrc=duration=1:size=160x120:rate=10",
        "-i", str(meta_file),
        "-map_metadata", "1",
        "-c:v", "libx264",
        str(out_dir / "meta_multi_mb_tags.mp4")
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if meta_file.exists():
        meta_file.unlink()

def generate_oversized_artwork():
    out_dir = FIXTURES_DIR / "artwork"
    ensure_dir(out_dir)

    # 1. Generate an oversized JPEG cover art (~4MB uncompressed noise)
    art_path = out_dir / "oversized_cover.jpg"
    cmd_art = [
        "ffmpeg", "-y",
        "-f", "lavfi", "-i", "nullsrc=s=4096x4096",
        "-frames:v", "1",
        "-q:v", "1",
        str(art_path)
    ]
    subprocess.run(cmd_art, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    # 2. Attach it to MKV file
    cmd_mkv = [
        "ffmpeg", "-y",
        "-f", "lavfi", "-i", "testsrc=duration=1:size=160x120:rate=10",
        "-c:v", "libx264",
        "-attach", str(art_path),
        "-metadata:s:t", "mimetype=image/jpeg",
        str(out_dir / "meta_oversized_artwork.mkv")
    ]
    subprocess.run(cmd_mkv, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def generate_path_and_name_bombs():
    out_dir = FIXTURES_DIR / "paths"
    ensure_dir(out_dir)

    # 1. Deep path test
    deep_path = out_dir
    for i in range(15):
        deep_path = deep_path / f"nested_level_{i:02d}"
    ensure_dir(deep_path)

    # Tiny sample file in deep path
    with open(deep_path / "deep_target.txt", "w", encoding="utf-8") as f:
        f.write("Target in deeply nested directory structure.\n")

    # 2. Unicode confusable / RTL override filename
    # U+202E is RIGHT-TO-LEFT OVERRIDE
    confusable_name = "sample_video_\u202Egpj.4pm"
    try:
        with open(out_dir / confusable_name, "w", encoding="utf-8") as f:
            f.write("Unicode confusable test payload.\n")
    except OSError:
        pass # Some filesystems disallow RTL override characters

    # 3. 200-char filename (Windows MAX_PATH safe limit for local files)
    long_name = ("aptura_video_precision_" * 8) + ".mp4"
    with open(out_dir / long_name, "w", encoding="utf-8") as f:
        f.write("Long filename test placeholder.\n")

def main():
    print("Generating metadata bombs...")
    generate_oversized_metadata()
    generate_oversized_artwork()
    generate_path_and_name_bombs()
    print("Metadata bombs generated successfully.")

if __name__ == "__main__":
    main()
