# Architecture

Aptura is a single-activity Android app. The UI is Jetpack Compose; playback is Media3; the library index is Room. The point of the layering is not ceremony: it keeps Android and network-capable APIs out of the domain, and it lets most behavior be tested on the JVM.

## 1. Layers

```
┌──────────────────────────────────────────────────────────────┐
│ UI (Compose)                                                 │
│ library · details · player · settings · onboarding · kit     │
└───────────────────────────┬──────────────────────────────────┘
                            │ state + events
┌───────────────────────────▼──────────────────────────────────┐
│ ViewModels                                                    │
│ MediaLibraryViewModel · PlayerViewModel                       │
└───────────────────────────┬──────────────────────────────────┘
                            │ domain interfaces, Flow
┌───────────────────────────▼──────────────────────────────────┐
│ Domain                                                        │
│ models · MediaRepository · PlaybackController · use cases     │
│ No Android imports.                                           │
└───────────────────────────┬──────────────────────────────────┘
                            │ implementations
┌───────────────────────────▼──────────────────────────────────┐
│ Data / core                                                   │
│ Room · MediaStore · SAF · DataStore · Media3 · WorkManager    │
│ diagnostics · file operations · permission helpers            │
└──────────────────────────────────────────────────────────────┘
```

`domain/ArchitectureDecouplingTest` fails the build if a domain file imports Android.

## 2. Packages

| Package | Responsibility |
|---|---|
| `ui/library` | Grid/list library, search, sorting, selection, details sheet, delete and transfer flows. |
| `ui/player` | Player surface, HUD, gestures, sheets, queue, Up Next, PiP, stats. |
| `ui/settings` | Appearance, playback defaults, library maintenance, diagnostics, about. |
| `ui/onboarding` | First-run privacy and storage-access guide. |
| `ui/components` | `ApturaDesignKit`: dialogs, buttons, menu rows, notices, skeletons. |
| `theme` | Colour, type, shape, accent and route-specific theme policy. |
| `data/database` | Room entities, DAOs, migrations (schema version 6). |
| `data/scanner` | MediaStore and SAF scanners, reconciliation, WorkManager scheduling. |
| `data/playback` | Media3 service, player bridge, audio effects, subtitle renderer. |
| `data/settings` | DataStore-backed `ApturaSettings`. |
| `data/transfer` | Journaled copy/move worker for long file operations. |
| `domain` | Pure Kotlin models, repository contracts, playback use cases. |
| `core` | Permissions, file operations, metadata, diagnostics, platform capabilities. |

## 3. Startup

`ApturaApplication.onCreate`:

1. Initialises `ApturaDiagnostics` (local ring buffer + file store + crash handler).
2. Registers the non-exported storage-volume receiver.
3. Schedules an on-process-start media scan and resumes a preempted file transfer from its journal.
4. Observes `MediaStore.Video` and schedules a debounced rescan when Android's index changes.

`MainActivity` is edge-to-edge, single-task, and handles `ACTION_VIEW` video intents. `Navigation.kt` owns the back policy: leaving the player by any route pauses playback and persists position.

## 4. Library index

Room stores two tables:

- `media_items` - URI, MediaStore id, title/display name, duration, size, MIME, dimensions, frame rate, dates, folder, playback position, completion, last played, missing flag, resize mode, speed, decoder override, audio/subtitle delays, custom title, hidden flag.
- `bookmarks` - media URI, timestamp, note, creation time.

The scanner owns the file facts; the user owns progress, titles, hidden state, decoder choice and delays. `MediaItemEntity.withUserDataFrom` carries every user column across a rescan, so a `REPLACE` cannot silently reset them.

The scan pipeline is:

1. Scan `MediaStore` when storage permission is granted.
2. Scan every persisted SAF tree, deduplicating against MediaStore rows.
3. Run `FileReconciliationEngine` to match moved/renamed files and produce upserts, URI remaps and deletions.
4. Remap bookmarks for renamed URIs.
5. Preserve genuinely missing external items that still carry user state; delete stale internal rows that carry none.
6. Delete obsolete rows in batches of 500.

A transient empty MediaStore result never wipes the library. A permission denial is not treated as "all files deleted".

## 5. Playback

`ApturaPlaybackService` is a Media3 `MediaSessionService` with `mediaPlayback` foreground type. `PlayerManager` holds the active controller. `Media3PlaybackController` implements the domain `PlaybackController` and maps Media3 state into `DomainPlaybackState`.

- The player UI observes domain state, not `ExoPlayer` directly.
- Position checkpoints are written on events and on a ticker; the journal restores position after process death.
- Audio sync and subtitle delay are applied through processors/renderers, not by changing the file.
- Audio effects go through `AudioEffectsBackend`, so JVM tests use `FakeBackend`.
- Decoder choice is per app (Settings) and optionally per video (player Picture tab). The codec selector never falls back to a bundled software video decoder.
- PiP is offered only when the device reports the feature; `MainActivity` updates the PiP parameters from playback and video size.

## 6. Settings

`ApturaSettingsRepository` wraps a Preferences DataStore (`aptura_settings`). It exposes one `Flow<ApturaSettings>` and suspend setters. Values cover playback defaults, theme/accent, background audio, guide version, subtitle style, decoder strategy, persisted SAF trees, and audio effects.

## 7. Diagnostics

`ApturaDiagnostics` writes bounded events to an in-memory ring buffer and a bounded on-device file. A crash handler writes a report before the process dies. `RedactionEngine` removes storage paths, file names (unless explicitly included), email addresses and IPv4 addresses. Reports are generated locally and leave the device only when the user exports or shares them.

There is no upload path. The app has no `INTERNET` permission.

## 8. Invariants

These are enforced, not aspirational:

| Invariant | Enforcement |
|---|---|
| No `INTERNET` permission | `PlatformInvariantsTest`, merged-manifest test |
| No telemetry SDK | Dependency-graph test; R8 mapping + dex check in `assembleRelease` |
| No network stack in shipped code | `verifyReleaseApk` reads the R8 mapping and dex |
| No `MANAGE_EXTERNAL_STORAGE` | Manifest invariant test |
| No bundled software video decoder | Dependency/no-decoder checks and ADR-010 |
| Domain has no Android imports | `ArchitectureDecouplingTest` |
| Schema changes are migrations | Committed Room schemas + migration tests |

## 9. Threading

- Room and file work run on `Dispatchers.IO`.
- Search filtering and derived state use `Dispatchers.Default` where the work is non-trivial.
- UI state is exposed as `StateFlow` and collected with lifecycle awareness.
- WorkManager owns long scans and transfers so they survive the UI process.

## 10. Adding a feature

1. Put the contract and models in `domain`.
2. Implement Android specifics in `data` or `core`.
3. Keep the ViewModel free of Android framework types where possible.
4. Add a fake at the same seam and test the behavior on the JVM.
5. If it changes a screen, render it in the gallery and update `DESIGN.md` only when the system changes.
6. Run `tools/gate.ps1` before the commit.
