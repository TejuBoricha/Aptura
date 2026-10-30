# Aptura Audit Changelog

> Permanent log of code refactoring, bug fixes, architecture improvements, and design decisions made throughout the audit.
> Formatted in reverse chronological order with traceability to Audit Tracker Issue IDs.

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
