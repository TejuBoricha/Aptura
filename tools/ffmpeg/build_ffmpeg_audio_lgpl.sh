#!/usr/bin/env bash
# ==============================================================================
# Aptura LGPL audio-only FFmpeg build, one ABI.
#
#   build_ffmpeg_audio_lgpl.sh <abi> <ffmpeg-tag> <ffmpeg-commit> <output-dir>
#
#   abi            arm64-v8a | armeabi-v7a | x86_64
#   ffmpeg-tag     an upstream FFmpeg release tag, for example n7.1.5
#   ffmpeg-commit  the full commit that tag must resolve to; the build stops if it does not, so a moved tag cannot slip in
#   output-dir     <output-dir>/src (FFmpeg), /build (config.h, config_components.h), /stage/lib (the shared libraries)
#
# Needs the Android NDK (ANDROID_NDK_HOME or ANDROID_NDK_ROOT), git, make, and nasm for x86_64. Runs on Linux x86_64 only.
#
# What it builds, and why (spec/11B_LEGAL_RESEARCH_FINDINGS.md, DEC-014, DEC-020, ADR-010):
#   - upstream FFmpeg, unmodified, from the official mirror, LGPL-2.1+ (no --enable-gpl, --enable-version3, --enable-nonfree);
#   - shared libraries, so the user can swap them (LGPL-2.1 section 6) and FFmpeg's own compliance checklist is followed;
#     `--target-os=android` makes FFmpeg name them libavcodec.so and so on, which an APK can carry;
#   - only the decoders in tools/ffmpeg/approved_decoders.txt, no demuxers (--disable-avformat), no network, nothing else;
#   - libswresample, because AndroidX Media's JNI resamples through it;
#   - 16 KB page alignment, which Google Play requires of 64-bit native libraries;
#   - no stripping here, so tools/ffmpeg/verify_lgpl_symbols.py can read the decoder names. Android's packaging strips later.
#
# The result must pass:
#   python3 tools/ffmpeg/verify_lgpl_symbols.py --build-dir <output-dir>/build --libs <output-dir>/stage/lib
# ==============================================================================

set -euo pipefail

if [ "$#" -ne 4 ]; then
    echo "usage: $0 <abi> <ffmpeg-tag> <ffmpeg-commit> <output-dir>" >&2
    exit 2
fi
ABI="$1"
TAG="$2"
COMMIT="$3"
OUT="$4"

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
if [ -z "$NDK" ] || [ ! -d "$NDK" ]; then
    echo "ERROR: set ANDROID_NDK_HOME (or ANDROID_NDK_ROOT) to an installed NDK." >&2
    exit 1
fi

API=26                     # the app's minSdk
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
case "$ABI" in
    arm64-v8a)   TRIPLE="aarch64-linux-android";    ARCH="aarch64"; CPU="armv8-a"; ABI_CFLAGS="" ;;
    armeabi-v7a) TRIPLE="armv7a-linux-androideabi"; ARCH="arm";     CPU="armv7-a"; ABI_CFLAGS="-march=armv7-a -mfloat-abi=softfp" ;;
    x86_64)      TRIPLE="x86_64-linux-android";     ARCH="x86_64";  CPU="";        ABI_CFLAGS="" ;;
    *) echo "ERROR: unsupported ABI '$ABI' (arm64-v8a, armeabi-v7a, x86_64)." >&2; exit 2 ;;
esac
CC="$TOOLCHAIN/${TRIPLE}${API}-clang"
if [ ! -x "$CC" ]; then
    echo "ERROR: compiler not found: $CC" >&2
    exit 1
fi

# The approved decoder list is the one source of truth, shared with the verifier.
DECODERS="$(grep -v -E '^[[:space:]]*(#|$)' "$HERE/approved_decoders.txt" | paste -sd, -)"
if [ -z "$DECODERS" ]; then
    echo "ERROR: tools/ffmpeg/approved_decoders.txt lists no decoders." >&2
    exit 1
fi

SRC="$OUT/src"
BUILD="$OUT/build"
STAGE="$OUT/stage"
rm -rf "$OUT"
mkdir -p "$BUILD" "$STAGE"

echo "=== FFmpeg $TAG ($COMMIT) for $ABI, decoders: $DECODERS ==="
git clone --quiet --depth 1 --branch "$TAG" https://github.com/FFmpeg/FFmpeg.git "$SRC"
ACTUAL="$(git -C "$SRC" rev-parse HEAD)"
if [ "$ACTUAL" != "$COMMIT" ]; then
    echo "ERROR: tag $TAG resolves to $ACTUAL, expected $COMMIT. Refusing to build." >&2
    exit 1
fi
# LGPL checklist item 5: the changes against upstream. There are none, and this proves it.
git -C "$SRC" diff --quiet || { echo "ERROR: the FFmpeg tree is not pristine." >&2; exit 1; }

FLAGS=(
    --prefix="$STAGE"
    --target-os=android
    --arch="$ARCH"
    --enable-cross-compile
    --cc="$CC"
    --nm="$TOOLCHAIN/llvm-nm"
    --ar="$TOOLCHAIN/llvm-ar"
    --ranlib="$TOOLCHAIN/llvm-ranlib"
    --strip="$TOOLCHAIN/llvm-strip"
    --extra-cflags="$ABI_CFLAGS"
    --extra-ldflags="-Wl,-z,max-page-size=16384"
    --disable-gpl
    --disable-nonfree
    --disable-version3
    --enable-shared
    --disable-static
    --disable-stripping
    --disable-programs
    --disable-doc
    --disable-avdevice
    --disable-avformat
    --disable-avfilter
    --disable-swscale
    --disable-postproc
    --disable-network
    --disable-autodetect
    --disable-everything
    --enable-swresample
    --enable-decoder="$DECODERS"
)
if [ -n "$CPU" ]; then
    FLAGS+=(--cpu="$CPU")
fi

cd "$BUILD"
"$SRC/configure" "${FLAGS[@]}"
make -j"$(nproc)"
make install

# What the libraries are built from, kept beside them (LGPL checklist items 3, 4 and 6).
{
    echo "FFmpeg tag:    $TAG"
    echo "FFmpeg commit: $COMMIT"
    echo "Source:        https://github.com/FFmpeg/FFmpeg/tree/$COMMIT"
    echo "ABI:           $ABI (Android API $API)"
    echo "Decoders:      $DECODERS"
    echo "Configure:     ${FLAGS[*]}"
} > "$STAGE/BUILD-INFO.txt"

echo "=== built: $(ls "$STAGE/lib"/*.so | tr '\n' ' ') ==="
