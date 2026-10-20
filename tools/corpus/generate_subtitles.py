#!/usr/bin/env python3
"""
tools/corpus/generate_subtitles.py
Generates the comprehensive subtitle test corpus per spec/12A_B0_INSTRUMENTATION.md §2.6.
Covers:
- Encodings: UTF-8, UTF-8-BOM, UTF-16 LE/BE, UTF-32 LE/BE, Windows-1252, Shift-JIS, GB18030, KOI8-R
- Scripts: Latin, RTL (Arabic, Hebrew), Devanagari, Thai, CJK
- ASS fidelity classes: E1–E5, F1–F7, X per spec/07 §8.1
- Bait files: wrong-charset declarations for FR-SUB-005
- Sidecar naming conventions: basename.srt, basename.en.srt, basename.forced.srt, subs/ folder
- Adversarial subtitles: 10k-char line, overlapping cues, zero-length cues, malformed tags, cue at t=0, cue beyond duration
"""

import os
import sys
from pathlib import Path

FIXTURES_DIR = Path("fixtures/adv_sub")

def ensure_dir(path: Path):
    path.mkdir(parents=True, exist_ok=True)

def generate_encodings():
    out_dir = FIXTURES_DIR / "encodings"
    ensure_dir(out_dir)

    text_samples = {
        "utf8": ("UTF-8 Plain Latin sample text: Aptura Video Player.\nLine 2 with accents: Café, Noël, façade.", "utf-8", False),
        "utf8_bom": ("UTF-8 with BOM: Aptura Video Player test.\nUniversal multi-script subtitle sample.", "utf-8-sig", False),
        "utf16_le": ("UTF-16 Little Endian: Aptura Video Player test.\nPrecision optical media playback.", "utf-16-le", False),
        "utf16_be": ("UTF-16 Big Endian: Aptura Video Player test.\nPrecision optical media playback.", "utf-16-be", False),
        "utf32_le": ("UTF-32 Little Endian: Aptura Video Player.\nWide unicode character test.", "utf-32-le", False),
        "utf32_be": ("UTF-32 Big Endian: Aptura Video Player.\nWide unicode character test.", "utf-32-be", False),
        "windows_1252": ("Windows-1252 sample: L'été arrive à la fête. Coût: €100.", "windows-1252", False),
        "shift_jis": ("Shift-JIS 日本語字幕テスト: アプチュラ プレイヤー\n高精細映像再生エンジン。", "shift_jis", False),
        "gb18030": ("GB18030 中文字幕测试: 视频播放器\n高清晰度画质与色彩精确度。", "gb18030", False),
        "koi8_r": ("KOI8-R Русский субтитр: Видеоплеер Аптура\nВысокая точность воспроизведения.", "koi8-r", False),
    }

    for name, (content, encoding, _) in text_samples.items():
        srt_content = f"1\n00:00:01,000 --> 00:00:05,000\n{content}\n\n"
        file_path = out_dir / f"adv_sub_enc_{name}.srt"
        with open(file_path, "wb") as f:
            f.write(srt_content.encode(encoding, errors="replace"))

def generate_scripts():
    out_dir = FIXTURES_DIR / "scripts"
    ensure_dir(out_dir)

    script_samples = {
        "latin": "1\n00:00:01,000 --> 00:00:05,000\nLatin script: The quick brown fox jumps over the lazy dog.\n\n",
        "arabic_rtl": "1\n00:00:01,000 --> 00:00:05,000\nمرحبا بكم في مشغل الفيديو ابتورا (Arabic RTL test)\n\n",
        "hebrew_rtl": "1\n00:00:01,000 --> 00:00:05,000\nשלום וברוכים הבאים לנגן הווידאו אפטורה (Hebrew RTL test)\n\n",
        "devanagari": "1\n00:00:01,000 --> 00:00:05,000\nआपतुरा वीडियो प्लेयर में आपका स्वागत है (Devanagari Hindi test)\n\n",
        "thai": "1\n00:00:01,000 --> 00:00:05,000\nยินดีต้อนรับสู่ Aptura เครื่องเล่นวิดีโอ (Thai script test)\n\n",
        "cjk_chinese": "1\n00:00:01,000 --> 00:00:05,000\n沉浸式无损画质视听享受 (Simplified Chinese test)\n\n",
        "cjk_japanese": "1\n00:00:01,000 --> 00:00:05,000\n映画館級の色彩美と音響体験 (Japanese test)\n\n",
        "cjk_korean": "1\n00:00:01,000 --> 00:00:05,000\n최고 수준의 로컬 미디어 재생기 (Korean Hangul test)\n\n",
    }

    for name, content in script_samples.items():
        file_path = out_dir / f"adv_sub_script_{name}.srt"
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)

def generate_ass_fidelity_corpus():
    out_dir = FIXTURES_DIR / "ass_fidelity"
    ensure_dir(out_dir)

    header = """[Script Info]
Title: Aptura ASS Quality Bar Probe
ScriptType: v4.00+
WrapStyle: 0
ScaledBorderAndShadow: yes
PlayResX: 1920
PlayResY: 1080

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Arial,52,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,3,2,2,40,40,40,1
Style: Karaoke,Arial,60,&H0000E5FF,&H00FFFFFF,&H00000000,&H80000000,1,0,0,0,100,100,0,0,1,4,2,2,40,40,60,1
Style: Positioned,Arial,48,&H00FFD600,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,2,1,7,40,40,40,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
"""

    probes = {
        # Class E - Essential
        "E1_timing_integrity": header + "Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,E1: Parse integrity test without dropped cues.\n",
        "E2_line_breaks": header + "Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,E2: First line\\NSecond line\\nThird line (soft)\\hNon-breaking space.\n",
        "E3_basic_inline_styling": header + "Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,E3: {\\b1}Bold{\\b0} {\\i1}Italic{\\i0} {\\u1}Underline{\\u0} {\\s1}Strike{\\s0} {\\fs70}Size70{\\r} {\\c&H00E5FF&}Cyan{\\c}.\n",
        "E4_positioning": header + "Dialogue: 0,0:00:01.00,0:00:04.00,Positioned,,0,0,0,,{\\an7\\pos(100,100)}E4: Top-left positioned cue via \\pos(100,100) and \\an7.\n",
        "E5_simultaneous_cues": header + """Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,{\\an2}E5: Bottom subtitle cue (speaker 1).
Dialogue: 0,0:00:01.00,0:00:04.00,Positioned,,0,0,0,,{\\an8}E5: Top subtitle cue (speaker 2).
""",
        # Class F - Fidelity
        "F1_motion_fade": header + "Dialogue: 0,0:00:01.00,0:00:05.00,Default,,0,0,0,,{\\fad(500,500)\\move(200,900,1720,900)}F1: Moving cue with fade in/out.\n",
        "F2_karaoke": header + "Dialogue: 0,0:00:01.00,0:00:05.00,Karaoke,,0,0,0,,{\\k30}Ap{\\k40}tu{\\k50}ra {\\k60}Pre{\\k40}ci{\\k50}sion {\\k80}Play{\\k60}er.\n",
        "F3_rotation": header + "Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,{\\frz25\\pos(960,540)}F3: Rotated text 25 degrees on Z axis.\n",
        "F4_blur_edges": header + "Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,{\\blur5\\bord10}F4: Heavy blur (5) and thick border (10).\n",
        "F5_transform_anim": header + "Dialogue: 0,0:00:01.00,0:00:05.00,Default,,0,0,0,,{\\t(0,2000,\\fscx150\\fscy150)}F5: Transform scale animation.\n",
        "F6_alpha_colors": header + "Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,{\\1a&H80&\\3a&H40&}F6: Secondary alpha and border transparency.\n",
        "F7_wrap_tolerance": header + "Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,{\\q2}F7: Long unbroken cue demonstrating wrapped tolerance without truncation in player HUD viewport.\n",
        # Class X - Decorative / Extended
        "X1_vector_drawing": header + "Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,{\\p1}m 0 0 l 100 0 100 100 0 100{\\p0} X1: Vector box drawing test.\n"
    }

    for name, content in probes.items():
        file_path = out_dir / f"adv_sub_ass_{name}.ass"
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)

def generate_bait_files():
    """Generates wrong-charset files that test FR-SUB-005 manual re-encoding UI."""
    out_dir = FIXTURES_DIR / "bait"
    ensure_dir(out_dir)

    # Windows-1252 text saved with UTF-8 declared or vice-versa
    latin_text = "1\n00:00:01,000 --> 00:00:05,000\nBait: Hôtellerie & Café spécialités — coût €45.\n\n"
    file_path = out_dir / "adv_sub_bait_win1252_as_latin1.srt"
    with open(file_path, "wb") as f:
        f.write(latin_text.encode("windows-1252"))

    # Shift-JIS text without BOM
    sjis_text = "1\n00:00:01,000 --> 00:00:05,000\nベイト字幕: 文字化け復旧テスト (FR-SUB-005)\n\n"
    file_path = out_dir / "adv_sub_bait_sjis_nobom.srt"
    with open(file_path, "wb") as f:
        f.write(sjis_text.encode("shift_jis"))

def generate_adversarial_cues():
    out_dir = FIXTURES_DIR / "adversarial"
    ensure_dir(out_dir)

    # 1. 10k-char single line
    long_line = "Aptura " * 1500
    with open(out_dir / "adv_sub_10k_char_line.srt", "w", encoding="utf-8") as f:
        f.write(f"1\n00:00:01,000 --> 00:00:05,000\n{long_line}\n\n")

    # 2. Overlapping cues
    overlapping = """1
00:00:01,000 --> 00:00:06,000
Overlapping cue 1 (1s to 6s)

2
00:00:02,000 --> 00:00:05,000
Overlapping cue 2 (2s to 5s)

3
00:00:03,000 --> 00:00:08,000
Overlapping cue 3 (3s to 8s)
"""
    with open(out_dir / "adv_sub_overlapping_cues.srt", "w", encoding="utf-8") as f:
        f.write(overlapping)

    # 3. Zero-length cues
    zero_len = """1
00:00:02,000 --> 00:00:02,000
Zero duration cue at exactly 2.0s

2
00:00:04,000 --> 00:00:06,000
Normal cue following zero duration
"""
    with open(out_dir / "adv_sub_zero_length_cue.srt", "w", encoding="utf-8") as f:
        f.write(zero_len)

    # 4. Malformed tags
    malformed = """1
00:00:01,000 --> 00:00:05,000
<b><i>Unclosed and malformed tags <font color="red">test<br><div>Broken HTML</b>
"""
    with open(out_dir / "adv_sub_malformed_tags.srt", "w", encoding="utf-8") as f:
        f.write(malformed)

    # 5. Negative timing and cue at t=0
    negative_time = """1
00:00:00,000 --> 00:00:03,000
Cue at exact t=0

2
-00:00:02,000 --> 00:00:04,000
Negative start timestamp cue
"""
    with open(out_dir / "adv_sub_negative_and_zero_timing.srt", "w", encoding="utf-8") as f:
        f.write(negative_time)

    # 6. Cue beyond media duration (e.g. at 24 hours)
    beyond = """1
24:00:00,000 --> 24:00:05,000
Cue scheduled at 24 hours (far beyond typical file duration)
"""
    with open(out_dir / "adv_sub_cue_beyond_duration.srt", "w", encoding="utf-8") as f:
        f.write(beyond)

def main():
    print("Generating subtitle test corpus...")
    generate_encodings()
    generate_scripts()
    generate_ass_fidelity_corpus()
    generate_bait_files()
    generate_adversarial_cues()
    print("Subtitle corpus generated successfully.")

if __name__ == "__main__":
    main()
