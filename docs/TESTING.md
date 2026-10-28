# Testing

Aptura has no network and no backend, so almost every behavior can be tested on the JVM. The test suite is built around that: pure logic first, Robolectric for Android and Compose behavior, a small instrumented suite for what only a real device can prove.

## Current state

| Check | Result |
|---|---|
| JVM tests | 679 tests across 100 suites |
| Executed in the gate | 601 (the 78 opt-in gallery renders are skipped) |
| Failures / errors | 0 / 0 |
| Lint | 0 issues |
| Release invariants | Pass |
| Instrumented suite | API 35 tablet emulator, run in CI |
| Agent coordination checks | `python -m unittest discover -s tools/agents`, run by the gate when Python 3 is installed and always in CI |

Counts come from the latest full `tools/gate.ps1` run; update this table when they move.

## Run it

```powershell
# Everything the local gate runs
./tools/gate.ps1
```

Or individually:

```bash
./gradlew compileDebugUnitTestKotlin
./gradlew testDebugUnitTest --fail-fast
./gradlew lintDebug
./gradlew assembleRelease
python -m unittest discover -s tools/agents
```

The unit-test task has a 300-second whole-task timeout. That is a hang guard, not a performance target; a cold CI runner exceeded the old 120 seconds at ~680 tests. If the suite legitimately grows past it, raise the guard in `app/build.gradle.kts` instead of deleting it.

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

### 5. Gallery renders

`ui/visual/*GalleryTest` renders real composables to PNG through Robolectric native graphics. These are review evidence, not regression assertions, and are skipped unless `APTURA_GALLERY_DIR` is set.

See [BUILDING.md](BUILDING.md#design-gallery) for commands and limits.

## Invariants

Some properties are too important to leave to a code review:

- **Manifest:** no `INTERNET`, no `MANAGE_EXTERNAL_STORAGE`, no `POST_NOTIFICATIONS`, scoped media permissions only.
- **Dependencies:** the resolved release graph contains no network stack or telemetry SDK (`DependencyInvariantsTest`).
- **Shipped code:** `verifyReleaseApk` reads the R8 mapping and the dex, because the classpath is not what ships.
- **Domain purity:** no Android imports under `domain/`.
- **Schema:** committed Room schemas and migration tests.
- **Licensing:** the LGPL audio extension is checked separately; no software video decoder is bundled.

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

These are tracked in [review/audit-2026-10-28/UI_UX_REVIEW.md](../review/audit-2026-10-28/UI_UX_REVIEW.md) and the audit reports under `review/`. [DEVICE_CHECKLIST.md](DEVICE_CHECKLIST.md) turns them into the pass to run before the 1.0 tag.
