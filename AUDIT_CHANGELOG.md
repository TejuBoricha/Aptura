# Aptura Audit Changelog

> Permanent log of code refactoring, bug fixes, architecture improvements, and design decisions made throughout the audit.
> Formatted in reverse chronological order with traceability to Audit Tracker Issue IDs.

## [Phase 7 Batch 4 — Performance & Resource Lifecycle Optimizations] — 2026-10-30

### Fixed & Verified Issues
- **ISSUE-022 (P2): Compose Recomposition Decoupling in PlayerScreen**
  - *Files Changed:* `app/src/main/java/org/aptura/player/ui/player/PlayerViewModel.kt`, `app/src/test/java/org/aptura/player/ui/player/PlayerViewModelTest.kt`
  - *Remediation:* Maintained internal `lastKnownPositionMs` in `PlayerViewModel` and gated 4 Hz position updates to `uiState.currentPositionMs` when controls are hidden (`isControlsVisible == false`). Full `PlayerScreen` recomposition drops to 0 during full-screen playback. When user taps to reveal HUD, position immediately synchronizes from `lastKnownPositionMs`. Added unit test `positionTicks_whenControlsHidden_suppressUiStateUpdates_untilControlsRevealed`.
- **ISSUE-023 (P2): Room Invalidation Dampening in MediaLibraryViewModel**
  - *Files Changed:* `app/src/main/java/org/aptura/player/ui/library/MediaLibraryViewModel.kt`, `app/src/test/java/org/aptura/player/ui/library/MediaLibraryViewModelTest.kt`
  - *Remediation:* Applied `distinctUntilChanged` on `mediaRepository.getAllVideos()`. Structural metadata and progress deltas are evaluated: sub-1% progress deltas from 5-second playback checkpoints are dampened, preventing full re-query, re-sorting, and re-allocation of `VideoCardUiModel` objects for the entire library. Added unit test `smallProgressCheckpointUpdates_doNotTriggerLibraryReEmission`.
- **ISSUE-013 (P2): Diagnostic Tracing for Audio Delay Format Bypass**
  - *Files Changed:* `app/src/main/java/org/aptura/player/data/playback/AudioDelayProcessor.kt`, `app/src/test/java/org/aptura/player/data/playback/AudioDelayProcessorTest.kt`
  - *Remediation:* Logged `EV_014` (`play.sync_shift`) to `ApturaDiagnostics` when audio encoding bypasses delay processor (e.g. non-16-bit PCM). Verified through regression unit test `nonPcm16Input_leavesTheProcessorInactive_soPlaybackIsNeverBroken`.

### Verification Evidence
- Full gate passed cleanly via `tools/gate.ps1`:
  - 694 unit tests passing (0 failures).
  - Android Lint passed with 0 errors.
  - Release APK minified with R8, zero forbidden packages.
  - 147 Python tests passed.

## [Phase 7 Batch 3 — Functional Integrity & Lifecycle Resiliency] — 2026-10-30

### Fixed & Verified Issues
- **ISSUE-001 (P1): Sleep Timer Survives App Task Removal**
  - *Files Changed:* `app/src/main/java/org/aptura/player/domain/usecase/SleepTimer.kt`, `app/src/main/java/org/aptura/player/domain/repository/PlaybackController.kt`, `app/src/main/java/org/aptura/player/data/playback/Media3PlaybackController.kt`, `app/src/main/java/org/aptura/player/ui/player/PlayerViewModel.kt`, `app/src/test/java/org/aptura/player/domain/usecase/SleepTimerTest.kt`, `app/src/test/java/org/aptura/player/ui/player/PlayerViewModelTest.kt`
  - *Remediation:* Extracted `SleepTimer` into pure Kotlin `domain/usecase` bound to `PlaybackController` (held by `ApturaPlaybackService`). `PlayerViewModel` delegates commands and observes state. Unit test suite and ViewModel clear test prove timer survives activity destruction.
- **ISSUE-002 (P2): User Feedback on Revoked PiP Permission**
  - *Files Changed:* `app/src/main/java/org/aptura/player/MainActivity.kt`
  - *Remediation:* Added explicit `AppOpsManager` verification and user Toast feedback ("Picture-in-Picture is disabled in Android settings") when PiP permission is denied or entry fails.
- **ISSUE-004 (P2): Persistence of A/V Offsets on External Videos**
  - *Files Changed:* `app/src/main/java/org/aptura/player/data/repository/MediaRepositoryImpl.kt`, `app/src/main/java/org/aptura/player/data/scanner/SafTreeScanner.kt`
  - *Remediation:* In `MediaRepositoryImpl.savePlaybackPreferences` and `savePlaybackPosition`, if the video row is not in Room (external `ACTION_VIEW` or SAF file), upsert a minimal `MediaItemEntity` using deterministic synthetic ID (`SafTreeScanner.computeSyntheticId`). A/V offsets, playback speed, and progress now persist across app restarts for external videos.
- **ISSUE-024 (P2): Clean Exit on Back from External Launch Intent**
  - *Files Changed:* `app/src/main/java/org/aptura/player/Navigation.kt`
  - *Remediation:* Tracked `isExternalLaunch` via `rememberSaveable`. In `dismissPlayerAndResetOrientation`, finish the activity if opened via an external `ACTION_VIEW` intent rather than trapping the user in internal onboarding/library screens.
- **ISSUE-026 (P3): Sleep Timer Preset Accessibility State for TalkBack**
  - *Files Changed:* `app/src/main/java/org/aptura/player/ui/player/PlayerToolsTabs.kt`
  - *Remediation:* Added `activePresetMinutes` state tracking so TalkBack announces `selected = true` on the active preset chip during sleep timer countdown.

### Verification Evidence
- Full gate passed cleanly via `tools/gate.ps1`:
  - 691 unit tests passing (0 failures).
  - Android Lint passed with 0 errors.
  - Release APK minified with R8, zero forbidden packages.
  - 147 Python tests passed.

## [Phase 7 Batch 2 — Exception Hygiene & Lint Cleanliness] — 2026-10-30

### Fixed & Verified Issues
- **ISSUE-014 (P2): Exception Narrowing & Diagnostic Tracing in Navigation**
  - *Files Changed:* `app/src/main/java/org/aptura/player/Navigation.kt`
  - *Remediation:* Replaced blind `catch (_: Throwable)` blocks with `catch (e: Exception)` and routed suppressed failures to `ApturaDiagnostics.logEvent` with semantic event codes (`EV_006`, `EV_008`, `EV_015`).
- **ISSUE-015 (P3): User Feedback for External Intent Launch Failures**
  - *Files Changed:* `app/src/main/java/org/aptura/player/Navigation.kt`, `app/src/main/java/org/aptura/player/ui/library/MediaLibraryScreen.kt`
  - *Remediation:* Added descriptive Toast messages ("No app available to open this video" / "No app available to share this video") when external `ACTION_VIEW` or `ACTION_SEND` intents fail or have no handling applications.
- **ISSUE-006 (P3): ApplySharedPref Lint Warnings in MediaTransferJournal**
  - *Files Changed:* `app/src/main/java/org/aptura/player/data/transfer/MediaTransferJournal.kt`
  - *Remediation:* Added `@SuppressLint("ApplySharedPref")` with architectural documentation explaining intentional synchronous durability for crash recovery (ADR-008, DEC-011).
- **ISSUE-017 (P3): Redundant -v26 Mipmap Resource Qualifier**
  - *Files Changed:* Renamed `app/src/main/res/mipmap-anydpi-v26` to `app/src/main/res/mipmap-anydpi`
  - *Remediation:* Removed obsolete API qualifier on minSdk 26 build, eliminating `ObsoleteSdkInt` lint warning.
- **ISSUE-010 / ISSUE-018 (P3): Android KTX toUri() Migration**
  - *Files Changed:* `Navigation.kt`, `PlayerScreen.kt`, `MediaDetailsSheet.kt`, `MediaLibraryScreen.kt`, `MediaDeleter.kt`, `MediaFileTransfer.kt`, `ThumbnailRepository.kt`, `MediaTransferWorker.kt`, `MediaScanWorker.kt`, `MediaTransferResultMessages.kt`, `PlayerManager.kt`, `AndroidSidecarSubtitleResolver.kt`, `Media3PlaybackController.kt`.
  - *Remediation:* Migrated `Uri.parse()` calls to `toUri()` Kotlin extension method, reducing total Android Lint warnings from 91 down to 58.

### Verification Evidence
- Full gate passed cleanly via `tools/gate.ps1`:
  - 684 unit tests passing (0 failures).
  - Android Lint passed with 0 errors and warning count dropped by 33 (91 -> 58).
  - Release APK minified with R8, zero forbidden packages.
  - 147 Python tests passed.

## [Phase 7 Batch 1 — Security & Privacy Invariants] — 2026-10-30

### Fixed & Verified Issues
- **ISSUE-020 (P1): Removable SD Card Path Leakage in RedactionEngine**
  - *Files Changed:* `app/src/main/java/org/aptura/player/core/diagnostics/RedactionEngine.kt`, `app/src/test/java/org/aptura/player/core/diagnostics/DiagnosticRedactionTest.kt`
  - *Root Cause:* `STORAGE_PATH_PATTERN` only matched `/storage/emulated/\d+/`, leaking physical SD card paths (`/storage/XXXX-XXXX/`) into diagnostic exports.
  - *Remediation:* Generalized pattern to match `/storage/(?:emulated/\d+|[^/\s,\]}"]+)/`. Added unit test verifying `/storage/4CE2-1407/` is redacted to `[FILE_REDACTED].mp4` and `[PATH_REDACTED]`.
- **ISSUE-021 (P2): Subtitle Zip Bomb Disk-Exhaustion DoS Guard**
  - *Files Changed:* `app/src/main/java/org/aptura/player/core/media/SubtitleZipExtractor.kt`, `app/src/test/java/org/aptura/player/core/media/SubtitleZipExtractorTest.kt`
  - *Root Cause:* Unbounded `zis.copyTo(fos)` permitted zip bombs to decompress gigabytes into application cache.
  - *Remediation:* Replaced unbounded copy with a bounded stream copy enforcing `MAX_SUBTITLE_BYTES = 20 MB`. If uncompressed data exceeds 20 MB, the partial file is purged and `ZipExtractionResult.Failure` is returned immediately. Added test verifying oversized stream abortion.

### Verification Evidence
- Full gate passed cleanly via `tools/gate.ps1`:
  - 684 unit tests passing (0 failures).
  - Android Lint passed with 0 errors.
  - Release APK minified with R8, zero forbidden packages.
  - 147 Python tests passed.

## [Phase 1 Baseline] — 2026-10-30

### Initial Baseline Established
- **Git Branch:** `agent/antigravity/app-audit-remediation` initialized from `origin/main` at commit `391c0a8`.
- **Audit System Initialized:** Created master tracking suite:
  - `AUDIT_TRACKER.md` (Issue master inventory & lifecycle)
  - `AUDIT_PROGRESS.md` (Phase milestones & blocker tracking)
  - `AUDIT_CHANGELOG.md` (Code modification history)
  - `AUDIT_TEST_RESULTS.md` (Test execution metrics & regression proof)
- **Baseline Quality Metrics Captured:**
  - 682 unit & Robolectric tests passing (0 failures, 78 skipped gallery tests).
  - Android Lint: 0 errors, 91 warnings, 1 hint.
  - Release build invariants: zero network stack, zero telemetry SDKs verified via R8 mapping.
  - Agent coordination checks: 147 Python tests passing.
- **Architectural Observations:**
  - Strict domain decoupling verified (`domain/ArchitectureDecouplingTest` passing with 0 Android framework imports).
  - Modularization opportunities identified in God-composables (`PlayerScreen.kt` 2,172 lines, `MediaLibraryScreen.kt` 2,137 lines).
