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
| Full local gate | `./tools/gate.ps1` (Windows) |

`tools/gate.ps1` compiles the test sources, runs the JVM suite fail-fast, runs lint, then builds the R8 release. The release build is finalized by `verifyReleaseApk`, which fails if a network stack or telemetry SDK survived R8 or appears in the dex.

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

Never commit a keystore or its passwords. `local.properties`, `*.jks` and build output are ignored.

## Testing the release build

Instrumented tests target the debug build by default. To run the E2E suite against the R8 release build:

```bash
./gradlew connectedReleaseAndroidTest -Paptura.testBuildType=release
```

This is a local/manual check. CI runs the debug instrumentation on an API 35 tablet emulator.

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
