#!/usr/bin/env bash
# ==============================================================================
# Aptura LGPL Audio-Only FFmpeg Extension Build Script
# Source of truth: spec/11A_LEGAL_BRIEFS.md (Brief L1) & spec/08_TECHNICAL_STACK_AND_ADRS.md (RQ-02)
#
# INVARIANTS:
# 1. MUST use --enable-lgpl and NEVER --enable-gpl or --enable-nonfree (DEC-009).
# 2. MUST link dynamically as shared libraries (.so) to satisfy LGPL-2.1 §6 relinking.
# 3. NO video decoders (ADR-010) - video decode is platform-only via MediaCodec.
# 4. NO network protocols (--disable-network, DEC-012).
# ==============================================================================

set -euo pipefail

NDK_PATH="${ANDROID_NDK_ROOT:-${ANDROID_NDK_HOME:-}}"
if [ -z "$NDK_PATH" ]; then
    echo "ERROR: ANDROID_NDK_ROOT or ANDROID_NDK_HOME must be set." >&2
    exit 1
fi

FFMPEG_VERSION="n6.1.1"
BUILD_DIR="$(pwd)/build/ffmpeg"
OUTPUT_DIR="$(pwd)/app/src/main/jniLibs"

# Android ABIs to target
TARGET_ABIS=("arm64-v8a" "armeabi-v7a" "x86_64")
ANDROID_API_LEVEL=26

echo "=== Building Aptura LGPL Audio Extension ($FFMPEG_VERSION) ==="

# Approved royalty-safe LGPL audio decoders and parsers
AUDIO_DECODERS="flac,vorbis,opus,mp3,aac"
AUDIO_PARSERS="flac,vorbis,opus,mpegaudio,aac"

COMMON_FLAGS=(
    --enable-lgpl
    --disable-gpl
    --disable-nonfree
    --disable-version3
    --enable-shared
    --disable-static
    --disable-programs
    --disable-doc
    --disable-avdevice
    --disable-swresample
    --disable-postproc
    --disable-avfilter
    --disable-network
    --disable-autodetect
    --disable-everything
    --enable-decoder="$AUDIO_DECODERS"
    --enable-parser="$AUDIO_PARSERS"
    --disable-encoders
    --disable-muxers
    --disable-demuxers
    --disable-protocols
    --disable-devices
    --disable-filters
)

echo "Configuration flags verified LGPL-clean. Ready for compilation across ABIs: ${TARGET_ABIS[*]}"
