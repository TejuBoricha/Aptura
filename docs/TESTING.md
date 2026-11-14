# Testing

Aptura has no network and no backend, so almost every behavior can be tested on the JVM. The test suite is built around that: pure logic first, Robolectric for Android and Compose behavior, a small instrumented suite for what only a real device can prove.

## Current state

The numbers change with every pull request, so they are not kept in this file. Read them from CI: open the latest run of the **CI** workflow, then the job named in the table.

| Check | Where to read the result |
|---|---|
| JVM tests & coverage | The **Unit tests and Kover coverage verification (min 70% lines)** step of the `unit-tests` job (fanned into `verify`); uploads `test-reports` and `kover-report` (`reportDebug.xml`). |
| Lint & Detekt | The **Lint** step of the `lint` job (fanned into `verify`); runs Android Lint (`lintDebug`) and `detekt`. |
| Release invariants | The **Release APK build and shipped-code invariants** (`verifyReleaseApk`) and **Release App Bundle (.aab) build and shipped-code invariants** (`verifyReleaseBundle`) steps of the `release` job (fanned into `verify`); uploads `release-invariant-reports`. |
| Instrumented suite | The `connected` job: an API 35 tablet emulator |
| Agent coordination checks | `python -m unittest discover -s tools/agents`, run by `tools/gate.ps1` when Python 3 is installed and by the **Agent coordination checks** step in CI `unit-tests`. |
| Gate scoping | `python -m unittest discover -s tools/gate` tests `release_scope.py`, which lets the local gate skip the R8 release builds for changes that cannot affect them. Run by `tools/gate.ps1` and by CI `unit-tests`. |
| Supply-chain & workflow pins | `python -m unittest discover -s tools/security` verifies secret file exclusions (`check_no_secret_files.py`) and 40-char commit SHA pinning on all `.github/workflows/*.yml` files (`test_workflow_pins.py`), and tests the advisory checker `osv_check.py` against a fake OSV service, including every way it must fail (`test_osv_check.py`). |
| Release APK smoke | `python -m unittest discover -s tools/smoke` tests `release_smoke.py` against a fake adb (no device needed); run by `tools/gate.ps1` and by CI `unit-tests`. The script itself runs nightly as the `release-smoke` job (see below) and by hand, see `docs/BUILDING.md`. |
| FFmpeg audio build | `.github/workflows/ffmpeg-audio.yml` (on demand, and on any pull request touching `tools/ffmpeg/**`): builds the self-built LGPL FFmpeg audio libraries (7.1.5, 8.0.3, 9.0.2) for arm64-v8a, armeabi-v7a and x86_64, runs each through `tools/ffmpeg/verify_lgpl_symbols.py` (licence recorded by FFmpeg itself, decoders actually built, 16 KB alignment, loadable library names), and links AndroidX Media's JNI against each version's libraries (its dependencies must be system libraries or siblings). A `package` job then assembles the pinned version (DEC-023) into one bundle with the exact FFmpeg source, build records and licence texts, re-verifies it, and uploads it; dispatched from `main` with `publish`, it attests the provenance and publishes it as a release (DEC-024). The app build downloads that release, refuses it unless its SHA-256 equals the pin in `gradle/ffmpeg-bundle.properties`, and links it (DEC-025). |
| Nightly corpus & API matrix | `.github/workflows/nightly.yml` (daily + `workflow_dispatch`): generates the `tools/corpus/` synthetic suite via `ffmpeg`, verifies `fixtures/manifest.csv`, runs `CommittedCorpusFixturesTest`, and runs every instrumented test (`PlaybackSmokeTest`, `FfmpegAudioPlaybackTest`, `ApturaAppE2eTest`, `ApturaUserJourneyTest`, `DocumentsProviderRenameTest`) on an API 26 (minSdk) and an API 36 emulator. The `release-smoke` job in the same workflow builds the signed R8 release APK (throwaway key), installs it on an API 35 tablet emulator and runs `tools/smoke/release_smoke.py`: cold start, a fixed-seed UI exerciser, clip playback, and a scan for a crash, ANR or native crash. The `dependency-advisories` job writes the release build's resolved runtime dependency list and runs `tools/security/osv_check.py` on it: every library that ships is checked against the OSV vulnerability database, and the job fails on any advisory that applies (exit 1) or when OSV cannot be reached (exit 3), so a Dependabot blind spot such as Guava through Media3 is found. A reviewed, accepted advisory goes in `tools/security/osv_allowlist.txt` with an expiry date. |

`tools/gate.ps1` runs the same checks on your machine. CI runs the Release APK/AAB R8 builds on every pull request; locally the gate runs them only when the branch changes a release-affecting file (decided by `tools/gate/release_scope.py`, which runs them whenever it cannot tell). Run `.\tools\gate.ps1` before pushing.

## Run it

```powershell
# The pre-push gate. Runs the R8 APK/AAB builds only for release-affecting changes and prints why or why not.
./tools/gate.ps1

# Always run the R8 APK/AAB builds (for example before cutting a release)
./tools/gate.ps1 -Full

# Rapid iteration (unit tests + lint + python guards, never the R8 APK/AAB)
./tools/gate.ps1 -Quick
```

Or individually:

```bash
./gradlew testDebugUnitTest --fail-fast
./gradlew lintDebug
./gradlew detekt
./gradlew koverXmlReportDebug koverVerifyDebug
./gradlew assembleRelease
./gradlew bundleRelease
python -m unittest discover -s tools/agents
python -m unittest discover -s tools/security
```

The unit-test task uses parallel forks (up to 3 locally, 2 in CI) with a 300-second whole-task timeout and skips `APTURA_GALLERY_DIR`-gated `*GalleryTest` suites unless `APTURA_GALLERY_DIR` is set. If the suite legitimately grows past the 300 s hang guard, raise the guard in `app/build.gradle.kts` instead of deleting it.

## Test layers

### 1. Pure JVM tests

Fast tests for models, formatting, sorting, math, validators, reconciliation, redaction and state machines. No Android runtime.

Examples: `NaturalOrderTest`, `SpeedRailMathTest`, `FileReconciliationEngineTest`, `SubtitleFileValidatorTest`, `DiagnosticRedactionTest`.

### 2. Robolectric tests

Robolectric supplies an Android runtime on the JVM. This is where most repository, database, scanner and Compose behavior lives.

- **Real Room/SQLite** where SQL matters: `ApturaDatabaseRealSqlTest`, `ApturaDatabaseMigrationTest`. DAO fakes are not used for SQL behavior.
- **Real WorkManager** test driver for cold-start scan behavior: `ColdStartScanTest`.
- **Real ExoPlayer** for the playback bridge where possible: `Media3PlaybackControllerTest`, `Media3PlaybackControllerQueueTest`.
- **Compose semantics and pixels**: screen tests, touch-target tests and focused pixel tests such as `PlayerSheetThemePixelTest`.

### 3. Fakes at the seams

Small fakes keep tests deterministic:

- `FakeAudioEffects` (`FakeEqualizer`, `FakeStrength`, `FakeLoudness`) for the audio-effects controller.
- `FakeMediaItemDao` and `FakeBookmarkDao` for repository behavior that is not about SQL.
- `FakeMediaStore` content provider for path resolution.
- `PlainTestApplication` for tests that must not start the production application graph.

A fake belongs at a domain or platform boundary, not in the middle of the logic under test.

### 4. Instrumented tests

Only two suites need a real Android process:

- `ApturaAppE2eTest` - launches the activity and exercises the end-to-end flow.
- `DocumentsProviderRenameTest` - needs a real `DocumentsProvider` for SAF rename semantics.

CI runs them on an API 35 `pixel_tablet` emulator. Locally:

```bash
./gradlew connectedDebugAndroidTest
```

### 5. Visual regression & gallery renders

Unconditional Robolectric native-graphics pixel and layout regression tests (`ui/visual/KeyScreenVisualRegressionTest`: `PhonePortraitVisualRegressionTest`, `PhoneLandscapeVisualRegressionTest`, `TabletVisualRegressionTest`) plus targeted pixel tests (`SettingsTopBarPixelTest`, `PlayerSheetThemePixelTest`, `SelectionBarPixelTest`, `GuideFooterPixelTest`) run on every `testDebugUnitTest` build and assert surface theme fills, non-blank rendered content, and key node bounds across phone portrait, landscape, and tablet sizes without relying on PNG golden comparisons across host OS font rasterizers.

`ui/visual/*GalleryTest` renders the full human-review PNG gallery through Robolectric native graphics and is skipped unless `APTURA_GALLERY_DIR` is set.

See [BUILDING.md](BUILDING.md#design-gallery) for commands and limits.

## Invariants

Some properties are too important to leave to a code review:

- **Manifest:** no `INTERNET`, no `MANAGE_EXTERNAL_STORAGE`, no `POST_NOTIFICATIONS`, scoped media permissions only.
- **Dependencies:** the resolved release graph contains no network stack or telemetry SDK (`DependencyInvariantsTest`).
- **Shipped code:** `verifyReleaseApk` and `verifyReleaseBundle` read the R8 mapping and every shipped dex file inside the release APKs and `.aab` bundle, because the classpath is not what ships.
- **Domain purity:** no Android imports under `domain/`.
- **Schema:** committed Room schemas and migration tests.
- **Licensing:** no software video decoder is bundled. `tools/ffmpeg/verify_lgpl_symbols.py` audits an FFmpeg build record (`config.h`) and built libraries against the approved decoder list `tools/ffmpeg/approved_decoders.txt`; its tests (`python -m unittest discover -s tools/ffmpeg`, run by the gate and CI) prove it fails on a GPL or nonfree licence, an unapproved decoder, a stripped `libavcodec` and on no input at all. On the prebuilt decoder this replaced it failed (9 unapproved decoders in each ABI: aac, aac_latm, ac3, dca, eac3, flac, mlp, mp3, truehd). The app now links the self-built bundle (LEG-02, DEC-025) and the same policy is checked in four more places: `LgplBuildVerificationTest` parses the bundle's own build records (`config.h`, `config_components.h`, `BUILD-INFO.txt`, unpacked by `fetchFfmpegBundle`) for every shipped ABI and fails on a non-LGPL licence, GPL, nonfree or version3, an external library, a static build, a decoder that is not in `approved_decoders.txt` (or one missing), any encoder, muxer or protocol, and ABIs built from different commits (with fixtures that prove each rule fires); `VendoredMedia3Test` holds the five vendored Media3 classes to the checksums in `VENDORED.md`; `verifyReleaseApk` / `verifyReleaseBundle` read R8's mapping and fail when a class or method name that the shipped `libffmpegJNI.so` exports (and `growOutputBuffer`, and the renderer constructor) was renamed or removed, since that loss is silent; `FfmpegAudioPlaybackTest` (instrumented, in `connected` and the nightly on API 26 and 36) loads the libraries on a device, checks which MIME types they decode (ALAC, AC-3, A-law and mu-law yes; AAC, E-AC-3, DTS, TrueHD, MP3, FLAC, Opus, Vorbis, AMR and H.264/HEVC no), and plays ALAC, AC-3, A-law and mu-law files through a player whose only renderer is FFmpeg's, requiring the end of the file, rendered buffers and the FFmpeg decoder's own name. `NoticeCoverageTest` holds NOTICE to the resolved release classpath, and `LicenseDocumentsTest` holds the Open-source licences screen to the packaged texts.

If you add a dependency, run the gate. If it is a network or telemetry library, the build will fail for a reason.

## What to test when you change something

| Change | Minimum evidence |
|---|---|
| Pure logic | A JVM test for the new branch, including the failure case |
| Room query or migration | Real SQLite test; add a migration if the schema changed |
| Scan or reconciliation | A scanner/reconciliation test with a moved, renamed or missing file |
| ViewModel behavior | A coroutine test with a fake repository, including cancellation |
| Player control | A ViewModel or Compose test proving the control is wired, plus the sheet render |
| Screen or sheet layout | A Compose semantics test; update the gallery render and `DESIGN.md` if the system changed |
| Permission or manifest | An invariant test in `core/invariants` |
| File operation | A test for failure, cancellation and partial progress, not only success |

## Evidence rules

- Screenshots in the repository are Robolectric renders of real composables. They are labelled as such; never present them as device captures.
- When a change makes an earlier "before" state irrelevant, replace the composite with after-only evidence rather than keeping a stale image.
- Do not claim a device, TalkBack, Bluetooth, HDR or thermal result that was not measured on hardware. Those remain open in the audit.

## Known gaps

The suite is strong on logic and UI structure, but it cannot prove:

- real codec behavior on a specific GPU/SoC;
- Bluetooth A2DP/LE latency;
- TalkBack reading order and spoken feedback;
- thermal throttling during long 4K playback;
- HDR tone mapping on a real panel;
- process death during an actual SAF copy (the journal and worker are tested with fakes);
- PiP behavior on an OEM that customizes the window manager.

These are tracked in [the current audit](audits/CURRENT_AUDIT.md). [DEVICE_CHECKLIST.md](DEVICE_CHECKLIST.md) turns them into the pass to run before the 1.0 tag.
