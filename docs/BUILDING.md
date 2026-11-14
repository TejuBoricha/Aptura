# Building Aptura

## Requirements

| Tool | Version |
|---|---|
| JDK | 17 |
| Android SDK | 36 (compileSdk and targetSdk) |
| Gradle | 9.1.0 via the wrapper |
| Android Studio | Current stable with AGP 9 support, or the command line |

`minSdk` is 26 (Android 8.0). The build needs no network permission in the app; Gradle itself downloads dependencies on the first run.

## First build

```bash
git clone https://github.com/TejuBoricha/Aptura.git
cd Aptura
./gradlew assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### The FFmpeg audio libraries

The app's audio extension (ALAC and G.711, LGPL-2.1) is not built by Gradle and needs no NDK. The `fetchFfmpegBundle` task downloads the pinned bundle from this repository's `ffmpeg-audio-<version>-r<n>` GitHub release, **refuses it unless its SHA-256 equals the one in `gradle/ffmpeg-bundle.properties`**, and unpacks the libraries (packaged by AGP), the licence texts (shown in the app) and the build records (read by the unit tests). The first build therefore needs network access to `github.com`; the zip is then kept in `~/.gradle/aptura/ffmpeg-bundles/<sha256>.zip` and re-checked on every run, so later builds work offline. A checksum failure means the download is not the bundle the repository pins: do not edit the checksum to make it pass.

To move to a new bundle, publish it with `.github/workflows/ffmpeg-audio.yml` (`workflow_dispatch`, which also needs the vendored Media3 classes to match: see `app/src/main/java/androidx/media3/decoder/ffmpeg/VENDORED.md`), then change `url` and `sha256` together in `gradle/ffmpeg-bundle.properties`.

If `local.properties` is missing, Android Studio creates it with `sdk.dir`. From the command line, set `ANDROID_HOME` or create `local.properties`:

```properties
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
```

## Common tasks

| Task | Command |
|---|---|
| Debug APK | `./gradlew assembleDebug` |
| R8 release build (unsigned by default) | `./gradlew assembleRelease` |
| JVM unit tests | `./gradlew testDebugUnitTest` |
| Instrumented tests (device/emulator) | `./gradlew connectedDebugAndroidTest` |
| Lint | `./gradlew lintDebug` |
| Local gate | `./tools/gate.ps1` (Windows; `-Full` always runs the R8 builds, `-Quick` never does) |

`tools/gate.ps1` compiles the test sources, runs the JVM suite fail-fast and runs lint (including Detekt). It then builds both the R8 release APKs and the release App Bundle (`.aab`) when the branch changes a release-affecting file (build scripts, `gradle/`, ProGuard rules, manifests, `res/xml`, `res/raw`, packaged native libraries), or when it cannot tell. Those builds are finalized by `verifyReleaseApk` and `verifyReleaseBundle`, which fail if a network stack or telemetry SDK survived R8 or appears in any shipped dex. CI runs them on every pull request regardless.

## Release signing

Signing is opt-in and secret-free in the repository. Without values, `assembleRelease` still builds and verifies an unsigned APK.

Provide all four values either in `local.properties`:

```properties
aptura.signing.storeFile=keystore/release.jks
aptura.signing.storePassword=...
aptura.signing.keyAlias=aptura
aptura.signing.keyPassword=...
```

or as environment variables:

| Variable | Value |
|---|---|
| `APTURA_SIGNING_STORE_FILE` | Path to the keystore, relative to the repository root or absolute |
| `APTURA_SIGNING_STORE_PASSWORD` | Keystore password |
| `APTURA_SIGNING_KEY_ALIAS` | Key alias |
| `APTURA_SIGNING_KEY_PASSWORD` | Key password |

Never commit a keystore or its passwords. `local.properties`, key and certificate files (`*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `*.pem`, `*.p8`, `*.key`), `.env` files and build output are ignored. Ignoring is not enough on its own, because `git add -f` bypasses it, so CI also fails any pull request that tracks one of those files (`tools/security/check_no_secret_files.py`). If a key is ever pushed, treat it as compromised and replace it.

## Testing the release build

Instrumented tests (`connectedDebugAndroidTest`) run against the debug build, in CI on an API 35 tablet emulator. They cannot run against the R8 release build: the test runner expects classes R8 removed from the minified app (`NoClassDefFoundError: androidx.tracing.Trace` in `AndroidJUnitRunner.onCreate`), and the keep rules that would fix that change what R8 produces, so the thing tested would no longer be the thing that ships.

To test the shipped build, install the signed release APK on a running emulator and drive it from outside:

```bash
./gradlew assembleRelease   # needs signing values (see above): an unsigned APK cannot be installed
python3 tools/smoke/release_smoke.py app/build/outputs/apk/release/app-x86_64-release.apk   # Windows: py -3
```

The script refuses an APK that is debuggable, cold-starts the app, runs a fixed-seed UI exerciser, opens a sample clip for playback and fails on a fatal exception, an ANR or a native crash of the app. Exit code 0 means nothing wrong was found, 1 means the app misbehaved, 2 means the check could not run (no device, install failed, wrong APK). CI runs it every night as the `release-smoke` job in `.github/workflows/nightly.yml`, with a throwaway signing key made inside the job.

## Design gallery

The gallery renders real Compose screens to PNG with Robolectric. It is opt-in so normal test runs stay fast and deterministic.

PowerShell:

```powershell
$env:APTURA_GALLERY_DIR = "C:\Users\you\AppData\Local\Temp\aptura-gallery"
.\gradlew.bat :app:testDebugUnitTest --tests "org.aptura.player.ui.visual.LibraryGalleryTest"
```

Bash:

```bash
export APTURA_GALLERY_DIR=/tmp/aptura-gallery
./gradlew :app:testDebugUnitTest --tests "org.aptura.player.ui.visual.LibraryGalleryTest"
```

Run each gallery class separately if the 300-second task timeout trips:

- `LibraryGalleryTest`
- `PlayerGalleryTest`
- `SettingsGalleryTest`
- `OnboardingGalleryTest`

The PNGs are written flat into `APTURA_GALLERY_DIR`. Gallery tests are skipped when the variable is unset, which is why the gate reports them as skipped.

Limits: Robolectric does not scroll sheets, and a non-default picture adjustment switches the video surface to a `TextureView`, which cannot be drawn to a software canvas. Render tab content directly for long sheets; see `tPlaybackContent` in `AppGalleryTest`.

## CI

`.github/workflows/ci.yml` has two jobs:

1. **verify** - JVM unit tests, lint, R8 release build, shipped-code invariants; uploads test and lint reports.
2. **connected** - instrumented tests on an API 35 `pixel_tablet` emulator with KVM; uploads the Android test report.

Both run on pushes to `main` and on pull requests. No repository secrets are required.

## Troubleshooting

- **`SDK location not found`** - set `ANDROID_HOME` or add `sdk.dir` to `local.properties`.
- **Gradle daemon or configuration-cache oddities** - `./gradlew --stop`, then rerun. The build uses the configuration cache; a stale entry is invalidated automatically on input changes.
- **Gallery task times out** - run one gallery class at a time, as above.
- **Instrumented tests cannot find a device** - `adb devices`; the emulator must be API 35 for the same results as CI.
- **Release build differs from CI** - CI builds unsigned; signing values change only the signature, not the R8 output. Check the `verifyReleaseApk` report under `app/build/reports/` if the invariant fails.
