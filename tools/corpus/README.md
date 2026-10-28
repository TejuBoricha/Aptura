# Aptura Test Corpus & Generator Toolchain

> **Source of Truth:** [spec/12A_B0_INSTRUMENTATION.md](../../spec/12A_B0_INSTRUMENTATION.md)  
> **Status:** B0.0-a Corpus Complete  
> **Index of Record:** [`fixtures/manifest.csv`](../../fixtures/manifest.csv)

---

## 1. Structure & Tools

This directory contains generator scripts for the Aptura B0.0 test fixture corpus:

1. **`generate_media_matrix.py`**
   - Synthesizes core A-cell combinations (MP4/MKV/MOV/WebM × H.264/HEVC/VP9 × AAC/MP3/FLAC/Opus/Vorbis/AC-3/E-AC-3).
   - Synthesizes B-set and C/D-sample video files.
   - Computes SHA-256 hashes, durations, and sizes.
   - Compiles and outputs `fixtures/manifest.csv`.

2. **`generate_subtitles.py`**
   - Synthesizes encodings: UTF-8, UTF-8-BOM, UTF-16 LE/BE, UTF-32 LE/BE, Windows-1252, Shift-JIS, GB18030, KOI8-R.
   - Multi-script probes: Latin, RTL (Arabic, Hebrew), Devanagari, Thai, CJK.
   - ASS quality bar probe fixtures: Class E (E1–E5), Class F (F1–F7), and Class X per `spec/07 §8.1`.
   - FR-SUB-005 bait files (misdeclared charsets).
   - Adversarial subtitles (10k-char line, overlapping cues, zero-length, malformed tags, cue beyond duration).

3. **`generate_metadata_bombs.py`**
   - Multi-MB container tags (`comment`, `description`).
   - Oversized embedded artwork (4MB+ cover art).
   - Deep folder paths and Unicode confusable / RTL filenames.

4. **`generate_scale_library.py`**
   - 1k / 10k / 50k item generators.
   - Generates unique metadata per item to prevent synthetic cache hits.

5. **`generate_instrumented.py`**
   - Burned-in frame counter and timecode overlays (`drawtext`).
   - Pinned seek targets covering keyframes and non-keyframes across near-start, mid, and near-end zones (`inst_seek_pinned_positions.json`).
   - Periodic impulse visual flash + audio click sync pairs for `TST-SYNC`.

---

## 2. Re-generating the Corpus

Run in the project root:
```bash
python tools/corpus/generate_subtitles.py
python tools/corpus/generate_metadata_bombs.py
python tools/corpus/generate_adversarial_media.py
python tools/corpus/generate_scale_library.py --count 1000 --name scale_1k
python tools/corpus/generate_instrumented.py
python tools/corpus/generate_media_matrix.py
```
This updates all fixtures and rebuilds `fixtures/manifest.csv`.
