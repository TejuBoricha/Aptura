# Vendored from AndroidX Media

These five Java files are copied **unmodified** from the AndroidX Media project (Apache License 2.0; each file keeps its
licence header). They are the Java half of Media3's FFmpeg audio extension. The native half is the FFmpeg bundle that
`app/build.gradle.kts` fetches (`gradle/ffmpeg-bundle.properties`).

- Upstream: https://github.com/androidx/media
- Upstream tag: `1.5.1`
- Upstream folder: `libraries/decoder_ffmpeg/src/main/java/androidx/media3/decoder/ffmpeg/`

| File | SHA-256 |
|---|---|
| `FfmpegAudioDecoder.java` | `3e29133f941d3f94af40496d05fb18b96754e0066c23a95951baee276524e1e5` |
| `FfmpegAudioRenderer.java` | `340a614e99c0725a38a6c978539fe36fe28a99414da53534a9914e6242a55234` |
| `FfmpegDecoderException.java` | `9e73968d0802b2d7f8add911ec4dc5a6e621777e997eca74332e173c5463ad7a` |
| `FfmpegLibrary.java` | `d7d00601c1864f742c3e0d73f7e9b77d9946f2a9dbe5a6e8e255272bc54d2356` |
| `package-info.java` | `1d75d02151fcec7a1c0036ee4a216e885ae91e1f85e8c9ddfbd72b4def6709be` |

`VendoredMedia3Test` fails when a file differs from this table, when a file is added or removed, or when the `media3`
version in `gradle/libs.versions.toml` is no longer the tag above. Because the files are verbatim, no "modified" notice
(Apache-2.0 section 4(b)) is needed; keep them verbatim.

## Why vendored

The prebuilt `org.jellyfin.media3:media3-ffmpeg-decoder` AAR linked a GPL-labelled, statically linked FFmpeg with decoders
the project does not bundle (AAC, AC-3, E-AC-3, DTS, TrueHD, MLP). The self-built extension (LEG-02, DEC-014, DEC-020,
DEC-024) needs the same Java classes without that binary, and Maven Central has no Media3 FFmpeg module that is not prebuilt.

## Not vendored, on purpose

`ExperimentalFfmpegVideoRenderer.java`: a software video decoder, which Aptura does not bundle (ADR-010, DEC-009). Its
only mention in these files is a Javadoc link in `FfmpegLibrary.java`.

## To move to another Media3 version

1. Fetch the five files from the matching tag and update the tag and the checksums above.
2. Re-check `ffmpeg_jni.cc` at that tag: `.github/workflows/ffmpeg-audio.yml` (`JNI_URL` and `JNI_SHA256`) builds the native
   glue from it, so a changed file means a new bundle revision. Publish it, then update `gradle/ffmpeg-bundle.properties`.
3. Check `app/proguard-rules.pro`: the FFmpeg keep rules must still match what the JNI looks up by name.
