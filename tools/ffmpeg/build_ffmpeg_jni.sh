#!/usr/bin/env bash
# ==============================================================================
# Links AndroidX Media's FFmpeg JNI glue against the shared FFmpeg libraries, for one ABI.
#
#   build_ffmpeg_jni.sh <abi> <stage-dir> <path-to-ffmpeg_jni.cc>
#
#   stage-dir   the `stage` directory build_ffmpeg_audio_lgpl.sh wrote (include/ and lib/); libffmpegJNI.so goes into stage/lib
#   ffmpeg_jni.cc   AndroidX Media's JNI source (Apache-2.0). CI downloads it from the 1.5.1 tag and checks its SHA-256 first.
#
# This is what Media3's own CMakeLists.txt does (target ffmpegJNI, linking swresample, avcodec, avutil, log and android, with
# 16 KB pages, and -Bsymbolic on arm64), except that FFmpeg is linked as shared libraries, so the user can swap them
# (LGPL-2.1 section 6, ADR-010). Doing it here, in CI, keeps the NDK out of the app's own Gradle build.
# ==============================================================================

set -euo pipefail

if [ "$#" -ne 3 ]; then
    echo "usage: $0 <abi> <stage-dir> <path-to-ffmpeg_jni.cc>" >&2
    exit 2
fi
ABI="$1"
STAGE="$2"
JNI_SOURCE="$3"

NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
if [ -z "$NDK" ] || [ ! -d "$NDK" ]; then
    echo "ERROR: set ANDROID_NDK_HOME (or ANDROID_NDK_ROOT) to an installed NDK." >&2
    exit 1
fi
API=26
case "$ABI" in
    arm64-v8a)   TRIPLE="aarch64-linux-android";    EXTRA_LDFLAGS="-Wl,-Bsymbolic" ;;
    armeabi-v7a) TRIPLE="armv7a-linux-androideabi"; EXTRA_LDFLAGS="" ;;
    x86_64)      TRIPLE="x86_64-linux-android";     EXTRA_LDFLAGS="" ;;
    *) echo "ERROR: unsupported ABI '$ABI' (arm64-v8a, armeabi-v7a, x86_64)." >&2; exit 2 ;;
esac
CXX="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/${TRIPLE}${API}-clang++"
for need in "$CXX" "$JNI_SOURCE" "$STAGE/include/libavcodec/avcodec.h" "$STAGE/lib/libavcodec.so" "$STAGE/lib/libswresample.so" "$STAGE/lib/libavutil.so"; do
    if [ ! -e "$need" ]; then
        echo "ERROR: missing $need" >&2
        exit 1
    fi
done

# shellcheck disable=SC2086  # EXTRA_LDFLAGS is deliberately a word list (or empty)
"$CXX" -shared -fPIC -std=c++11 -O2 \
    -I "$STAGE/include" \
    "$JNI_SOURCE" \
    -L "$STAGE/lib" -lswresample -lavcodec -lavutil -llog -landroid \
    -Wl,-soname,libffmpegJNI.so \
    -Wl,-z,max-page-size=16384 \
    -Wl,--no-undefined \
    $EXTRA_LDFLAGS \
    -o "$STAGE/lib/libffmpegJNI.so"

echo "=== linked: $STAGE/lib/libffmpegJNI.so ($ABI) ==="
