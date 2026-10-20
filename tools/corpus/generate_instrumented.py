#!/usr/bin/env python3
"""
tools/corpus/generate_instrumented.py
Generates instrumented seek & AV-sync fixtures per spec/12A_B0_INSTRUMENTATION.md §2.9.
1. Frame-tagged video with burned-in frame counter and millisecond timecodes.
2. Pinned seek position index (>= 8 pinned positions covering keyframe & non-keyframe).
3. Impulse A/V sync fixtures (visual flash + audio click on 1.0s cadence).
"""

import json
import subprocess
from pathlib import Path

OUT_DIR = Path("fixtures/inst")

def ensure_dir(path: Path):
    path.mkdir(parents=True, exist_ok=True)

def generate_instrumented_seek():
    ensure_dir(OUT_DIR)

    font_path = "C\\:/Windows/Fonts/arial.ttf"
    vf_text = (
        f"drawtext=fontfile='{font_path}':"
        "text='FRAME %{n} | TC %{pts\\:hms}':"
        "fontsize=32:fontcolor=white:box=1:boxcolor=black@0.6:"
        "x=(w-tw)/2:y=h-th-30"
    )

    # 1. Standard GOP H.264 (30s duration, 24fps, keyframe every 24 frames = 1.0s)
    out_h264 = OUT_DIR / "inst_seek_tagged_h264_30s.mp4"
    cmd_h264 = [
        "ffmpeg", "-y",
        "-f", "lavfi", "-i", "testsrc=duration=30:size=640x360:rate=24",
        "-f", "lavfi", "-i", "sine=duration=30:frequency=440",
        "-vf", vf_text,
        "-c:v", "libx264", "-g", "24", "-keyint_min", "24", "-sc_threshold", "0",
        "-c:a", "aac",
        str(out_h264)
    ]
    subprocess.run(cmd_h264, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    # 2. Long GOP HEVC (30s duration, 24fps, keyframe every 96 frames = 4.0s)
    out_hevc = OUT_DIR / "inst_seek_tagged_hevc_long_gop_30s.mkv"
    cmd_hevc = [
        "ffmpeg", "-y",
        "-f", "lavfi", "-i", "testsrc=duration=30:size=640x360:rate=24",
        "-f", "lavfi", "-i", "sine=duration=30:frequency=440",
        "-vf", vf_text,
        "-c:v", "libx265", "-x265-params", "keyint=96:min-keyint=96:no-scenecut=1",
        "-c:a", "aac",
        str(out_hevc)
    ]
    subprocess.run(cmd_hevc, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    # 3. Pinned seek targets (>= 8 positions for the 30s files)
    # Testing MB-07's keyframe vs non-keyframe split across near-start, mid, near-end
    pinned_positions = {
        "description": "Pinned seek targets for TST-SEEK and MB-07 latency benchmarks per spec/12A §2.9",
        "targets": [
            {"position_us": 500000, "position_s": 0.5, "target_type": "non-keyframe", "zone": "near-start"},
            {"position_us": 1000000, "position_s": 1.0, "target_type": "keyframe", "zone": "near-start"},
            {"position_us": 2500000, "position_s": 2.5, "target_type": "non-keyframe", "zone": "near-start"},
            {"position_us": 12000000, "position_s": 12.0, "target_type": "keyframe", "zone": "mid"},
            {"position_us": 14750000, "position_s": 14.75, "target_type": "non-keyframe", "zone": "mid"},
            {"position_us": 15000000, "position_s": 15.0, "target_type": "non-keyframe", "zone": "mid"},
            {"position_us": 27000000, "position_s": 27.0, "target_type": "keyframe", "zone": "near-end"},
            {"position_us": 29200000, "position_s": 29.2, "target_type": "non-keyframe", "zone": "near-end"},
        ]
    }
    with open(OUT_DIR / "inst_seek_pinned_positions.json", "w", encoding="utf-8") as f:
        json.dump(pinned_positions, f, indent=2)

def generate_instrumented_sync():
    ensure_dir(OUT_DIR)

    # Visual flash: black background with a white box flash at each second mark
    # Audio click: short beep burst at each second mark
    out_sync = OUT_DIR / "inst_avsync_impulse_10s.mp4"
    
    # 10s video with flash on every integer second (duration 1 frame = 1/25 s)
    vf = "color=c=black:s=640x360:d=10,drawbox=x=0:y=0:w=640:h=360:color=white:t=fill:enable='lt(mod(t,1.0),0.04)'"
    # Audio click: 1000Hz tone gated to 40ms bursts every second
    af = "sine=frequency=1000:duration=10,volume=enable='lt(mod(t,1.0),0.04)':volume=1.0:eval=frame"

    cmd_sync = [
        "ffmpeg", "-y",
        "-f", "lavfi", "-i", vf,
        "-f", "lavfi", "-i", af,
        "-c:v", "libx264", "-pix_fmt", "yuv420p",
        "-c:a", "aac",
        str(out_sync)
    ]
    subprocess.run(cmd_sync, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def main():
    print("Generating instrumented seek & sync fixtures...")
    generate_instrumented_seek()
    generate_instrumented_sync()
    print("Instrumented fixtures generated successfully.")

if __name__ == "__main__":
    main()
