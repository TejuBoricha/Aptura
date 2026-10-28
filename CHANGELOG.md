# Changelog

All notable changes to Aptura are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

- Public project documentation: architecture, build, testing and privacy guides.
- Apache-2.0 license, third-party notice, contribution guide, security policy and code of conduct.
- GitHub issue forms and a pull-request template.
- New launcher mark: a cyan aperture disc with a play-triangle opening and three curved iris seams.

### Changed

- Delete dialogs now make **Move to trash** the primary action and **Delete permanently** a red, explicitly secondary choice. When the platform cannot trash a file, permanent deletion remains the only, destructive action.
- `ApturaDialog` accepts an independent `iconTone`; `DialogAlternative` accepts a tone, so a non-destructive primary action can still show a red warning icon.
- README rewritten around the product, privacy guarantees and verification evidence.
- UI/UX review updated with the merged commit range, the full gate result and the current evidence index.

### Fixed

- Stale player-sheet and delete-dialog evidence replaced with renders from the current code, including after-only evidence where the old "before" state no longer exists.

## 1.0.0 - 2026-10-28

Pre-release baseline. Not distributed through an app store.

### Added

- Offline-first local video library backed by MediaStore and SAF folder access.
- Incremental WorkManager scanning, reconciliation of moved/renamed files, and preservation of user progress.
- Adaptive Compose library with search, category filters, sorting, multi-select, rename, share, move, hide and delete flows.
- Media3 player with hardware decoding, decoder strategy, PiP, gestures, audio-only mode, orientation lock and playback statistics.
- Audio track selection, five-band equalizer and presets, bass boost, virtualizer, dialogue boost and audio sync.
- Subtitle tracks, sidecar import, timing, tap-to-sync, styling and a browser hand-off for online search.
- Playback speed, sleep timer with fade, "+5 min" extension, end-of-video stop, A-B repeat, frame stepping and bookmarks.
- Picture controls: aspect ratio, night mode, contrast, colour and decoder selection.
- Shared design kit and one player-sheet chrome across every sheet and tab.
- Dark and Light app themes; the player and its sheets remain dark in both.
- Local diagnostics with redaction, export and delete.
- JVM, Robolectric and instrumented test suites, a 97-suite gate, and release-build checks for network and telemetry code.
- GitHub Actions CI: JVM verification and API 35 tablet instrumentation.

### Security

- No `INTERNET` permission, no telemetry, no ads and no accounts.
- Scoped storage only; no `MANAGE_EXTERNAL_STORAGE`.
- Android backup disabled.
- Release builds fail if a network stack or telemetry SDK survives R8.
