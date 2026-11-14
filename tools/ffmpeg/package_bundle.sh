#!/usr/bin/env bash
# ==============================================================================
# Assembles the FFmpeg audio bundle the app downloads: the libraries for every ABI, the exact FFmpeg source they were built from,
# the build records, the licence texts and a README, with a checksum list, in one zip.
#
#   package_bundle.sh <ffmpeg-tag> <ffmpeg-commit> <revision> <artifacts-dir> <output-dir>
#
#   artifacts-dir   holds one folder per ABI, named ffmpeg-audio-<tag>-<abi>, as the build job uploaded them
#   output-dir      receives aptura-ffmpeg-audio-<tag>-r<revision>.zip and the files that go beside it in a release
#
# Why the source and the records travel with the binaries: LGPL-2.1 section 6 and FFmpeg's own compliance checklist (items 3 to 8)
# ask for the corresponding source, the way it was built, and the licence texts, from the same place as the binaries.
# ==============================================================================

set -euo pipefail

if [ "$#" -ne 5 ]; then
    echo "usage: $0 <ffmpeg-tag> <ffmpeg-commit> <revision> <artifacts-dir> <output-dir>" >&2
    exit 2
fi
TAG="$1"
COMMIT="$2"
REVISION="$3"
ARTIFACTS="$4"
OUT="$5"

NAME="aptura-ffmpeg-audio-$TAG-r$REVISION"
BUNDLE="$OUT/$NAME"
rm -rf "$OUT"
mkdir -p "$BUNDLE/jniLibs" "$BUNDLE/build-records" "$BUNDLE/licenses" "$BUNDLE/source"

for ABI in arm64-v8a armeabi-v7a x86_64; do
    FROM="$ARTIFACTS/ffmpeg-audio-$TAG-$ABI"
    for need in libavcodec.so libavutil.so libswresample.so libffmpegJNI.so BUILD-INFO.txt config.h config_components.h; do
        if [ ! -f "$FROM/$need" ]; then
            echo "ERROR: $FROM/$need is missing." >&2
            exit 1
        fi
    done
    mkdir -p "$BUNDLE/jniLibs/$ABI" "$BUNDLE/build-records/$ABI"
    cp "$FROM"/lib*.so "$BUNDLE/jniLibs/$ABI/"
    cp "$FROM/BUILD-INFO.txt" "$FROM/config.h" "$FROM/config_components.h" "$BUNDLE/build-records/$ABI/"
done

# The exact FFmpeg source (LGPL checklist items 3, 4, 7): the pinned commit, exported as it is.
SRC="$OUT/ffmpeg-src"
git clone --quiet --depth 1 --branch "$TAG" https://github.com/FFmpeg/FFmpeg.git "$SRC"
if [ "$(git -C "$SRC" rev-parse HEAD)" != "$COMMIT" ]; then
    echo "ERROR: $TAG no longer resolves to $COMMIT." >&2
    exit 1
fi
git -C "$SRC" archive --format=tar --prefix="ffmpeg-$TAG/" HEAD | xz -9 > "$OUT/ffmpeg-$TAG-source.tar.xz"
cp "$OUT/ffmpeg-$TAG-source.tar.xz" "$BUNDLE/source/"
cp "$SRC/COPYING.LGPLv2.1" "$BUNDLE/licenses/FFmpeg-COPYING.LGPLv2.1.txt"
cp "$SRC/LICENSE.md" "$BUNDLE/licenses/FFmpeg-LICENSE.md"

# AndroidX Media's JNI glue is Apache-2.0; its source and licence come along too.
curl -fsSL -o "$BUNDLE/source/ffmpeg_jni.cc" "https://raw.githubusercontent.com/androidx/media/1.5.1/libraries/decoder_ffmpeg/src/main/jni/ffmpeg_jni.cc"
curl -fsSL -o "$BUNDLE/licenses/AndroidX-Media-LICENSE-Apache-2.0.txt" "https://raw.githubusercontent.com/androidx/media/1.5.1/LICENSE"

# The decoders, as the build itself recorded them (the same line for every ABI), so this text can never disagree with the libraries.
DECODERS="$(sed -n 's/^Decoders: *//p' "$BUNDLE/build-records/arm64-v8a/BUILD-INFO.txt" | head -n 1)"
if [ -z "$DECODERS" ]; then
    echo "ERROR: build-records/arm64-v8a/BUILD-INFO.txt has no Decoders line." >&2
    exit 1
fi

cat > "$BUNDLE/README.txt" <<EOF
Aptura FFmpeg audio libraries, $TAG, revision $REVISION
========================================================

What this is
  Shared libraries (libavcodec.so, libavutil.so, libswresample.so) built from unmodified upstream FFmpeg $TAG, commit
  $COMMIT, plus libffmpegJNI.so, AndroidX Media 1.5.1's JNI glue (Apache-2.0) linked against them. They decode only the audio
  formats listed in the build records (FFmpeg decoders: $DECODERS). Layout: jniLibs/<abi>/ for arm64-v8a, armeabi-v7a, x86_64.

Licences
  FFmpeg is licensed under the GNU Lesser General Public License version 2.1 or later (licenses/FFmpeg-COPYING.LGPLv2.1.txt). It was
  configured without --enable-gpl, --enable-version3 and --enable-nonfree; each build-records/<abi>/config.h records
  FFMPEG_LICENSE and FFMPEG_CONFIGURATION. The JNI glue is Apache-2.0 (licenses/AndroidX-Media-LICENSE-Apache-2.0.txt).
  This software uses libraries from the FFmpeg project (https://ffmpeg.org) under the LGPLv2.1.

Source and how it was built
  source/ffmpeg-$TAG-source.tar.xz is the exact FFmpeg source of those libraries. There are no changes to it.
  The configure line is in build-records/<abi>/BUILD-INFO.txt. The build scripts (tools/ffmpeg/ in https://github.com/TejuBoricha/Aptura)
  and the workflow .github/workflows/ffmpeg-audio.yml reproduce the build; source/ffmpeg_jni.cc is the glue source.

Replacing the libraries (LGPL-2.1 section 6)
  Aptura's complete source is public (Apache-2.0). To use a modified FFmpeg, rebuild it with tools/ffmpeg/build_ffmpeg_audio_lgpl.sh,
  relink the glue with tools/ffmpeg/build_ffmpeg_jni.sh, put the libraries where the app's build expects them and rebuild the app.
  Reverse engineering for debugging such modifications is permitted.

Integrity
  SHA256SUMS.txt lists the checksum of every file in this bundle.
EOF

(cd "$BUNDLE" && find . -type f ! -name SHA256SUMS.txt -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS.txt)
(cd "$OUT" && zip -q -X -r "$NAME.zip" "$NAME")
cp "$BUNDLE/README.txt" "$OUT/README.txt"
cp "$BUNDLE/SHA256SUMS.txt" "$OUT/SHA256SUMS.txt"
(cd "$OUT" && sha256sum "$NAME.zip" "ffmpeg-$TAG-source.tar.xz" > "$NAME.zip.sha256")

echo "=== $OUT ==="
ls -la "$OUT" | grep -v -E " (ffmpeg-src|$NAME)$"
cat "$OUT/$NAME.zip.sha256"
