#!/usr/bin/env python3
"""
tools/corpus/generate_scale_library.py
Generates scale test libraries per spec/12A_B0_INSTRUMENTATION.md §2.8.
Rule: Clones tiny master media with UNIQUE metadata on each item so MediaStore/Room
indexing and extraction are realistic and avoid synthetic cache hits.
"""

import os
import sys
import argparse
import subprocess
from pathlib import Path

OUT_DIR = Path("fixtures/scale")

def ensure_dir(path: Path):
    path.mkdir(parents=True, exist_ok=True)

def generate_master_seed(seed_path: Path):
    """Generates a tiny (1 frame / 0.1s) valid MP4 file to serve as the cloning master."""
    cmd = [
        "ffmpeg", "-y",
        "-f", "lavfi", "-i", "testsrc=duration=0.5:size=160x120:rate=10",
        "-c:v", "libx264", "-pix_fmt", "yuv420p",
        str(seed_path)
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def generate_scale_set(count: int, set_name: str):
    target_dir = OUT_DIR / set_name
    ensure_dir(target_dir)

    seed_file = OUT_DIR / "_master_seed.mp4"
    if not seed_file.exists():
        generate_master_seed(seed_file)

    with open(seed_file, "rb") as f:
        master_bytes = f.read()

    print(f"Generating {count} unique scale library files in {target_dir}...")
    total_bytes = 0
    for i in range(1, count + 1):
        # We append a unique custom trailing atom or comment identifier so hashes and file metadata differ
        unique_meta = f"\n[ApturaItem:{set_name}:{i:06d}:title=Sample_Video_{i:06d}]".encode("utf-8")
        item_data = master_bytes + unique_meta
        item_path = target_dir / f"video_{i:06d}.mp4"
        with open(item_path, "wb") as f:
            f.write(item_data)
        total_bytes += len(item_data)

    print(f"Generated {count} files. Total disk footprint: {total_bytes / (1024*1024):.2f} MB")
    return total_bytes

def main():
    parser = argparse.ArgumentParser(description="Aptura Scale Library Generator")
    parser.add_argument("--count", type=int, default=1000, help="Number of items to generate (default 1000 for 1k test)")
    parser.add_argument("--name", type=str, default="scale_1k", help="Name of scale set directory")
    args = parser.parse_args()

    ensure_dir(OUT_DIR)
    generate_scale_set(args.count, args.name)

if __name__ == "__main__":
    main()
