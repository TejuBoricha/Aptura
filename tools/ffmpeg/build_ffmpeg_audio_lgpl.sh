#!/usr/bin/env bash
# ==============================================================================
# Aptura LGPL audio-only FFmpeg build.  STATUS: NOT IMPLEMENTED (LEG-02, step 2).
#
# This script fails on purpose. An earlier version only defined a list of flags and printed "Ready for compilation": it
# never downloaded, configured or built anything, and its flag list could not have worked. Checked on 2026-11-14 against
# FFmpeg n6.1.1's `configure` and AndroidX Media 1.5.1's own build_ffmpeg.sh:
#   - `--enable-lgpl` is not an FFmpeg option; `configure` aborts on an unknown option. LGPL 2.1 is what it computes when
#     GPL, version3 and nonfree are all off. The licence is audited from FFMPEG_LICENSE in config.h instead.
#   - `--disable-swresample` would break Media3's JNI, which resamples through libswresample (Media3 builds with
#     `--enable-swresample`).
#   - Media3 builds static archives (`--enable-static --disable-shared`) and links them into one libffmpegJNI.so, which
#     is how the prebuilt Jellyfin decoder ended up with FFmpeg linked statically. ADR-010 wants shared libraries so a
#     user can relink (LGPL-2.1 section 6), so the module's CMakeLists has to link libavcodec/libavutil/libswresample as
#     shared libraries, or the relinkable objects have to be offered.
#   - Media3 passes `--disable-avformat`: the extension decodes frames and needs no demuxers, muxers or protocols.
#
# The decoders it may enable are tools/ffmpeg/approved_decoders.txt, one source of truth shared with
# verify_lgpl_symbols.py. Whatever this script eventually builds must pass:
#   python3 tools/ffmpeg/verify_lgpl_symbols.py --build-dir <ffmpeg build dir> --libs <built .so files>
#
# INVARIANTS (DEC-009, DEC-012, ADR-010): never --enable-gpl, --enable-version3 or --enable-nonfree; no video decoders; no
# network; shared libraries; only the decoders in approved_decoders.txt.
# ==============================================================================

set -euo pipefail

echo "build_ffmpeg_audio_lgpl.sh is not implemented yet (LEG-02 step 2). See the header of this file." >&2
exit 3
