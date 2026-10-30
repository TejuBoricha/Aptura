# Aptura Centralized Audit Tracker (Single Source of Truth)

> Master inventory of all findings, issue IDs, priorities, affected files, root causes, proposed solutions, verification criteria, and lifecycle statuses across all audit phases.
> Maintained strictly per the Phased Audit, Refactoring & Improvement Workflow.

## Issue Status Lifecycle Taxonomy
Every finding progresses strictly through the following states:
`Pending` → `Investigating` → `In Progress` → `Fixed` → `Verification Pending` → `Verified`
*(Exceptions: `Deferred` with documented justification, or `Blocked` with documented explanation)*

---

## Priority Classification
- **P0 — Critical:** Security vulnerabilities, privacy invariant breaks, data loss, application-breaking crashes/deadlocks.
- **P1 — High:** Major functional defects, serious performance/memory bottlenecks, lifecycle state loss.
- **P2 — Medium:** Maintainability issues, architectural bottlenecks, inefficient implementations, UI glitching under edge cases.
- **P3 — Low:** Minor code cleanup, naming inconsistencies, non-essential optimizations, lint/dependency hygiene.

---

## Master Inventory of Identified Issues

| ID | Phase | Priority | Category | Summary | Affected Files | Status | Verification Evidence |
|---|---|---|---|---|---|---|---|
| **ISSUE-001** | Phase 1 | P1 | Lifecycle / Playback | Swiping app out of Recents keeps audio playing but kills sleep timer | `PlayerViewModel.kt`, `Media3PlaybackController.kt` | Investigating | Regression test in `PlayerViewModelTest` |
| **ISSUE-002** | Phase 1 | P2 | Platform / UI | PiP tap is silent no-op when PiP permission/app-op is disabled | `PlayerScreen.kt`, `MainActivity.kt` | Pending | TBD |
| **ISSUE-003** | Phase 1 | P2 | Lifecycle / Configuration | Paused video resumes playing after configuration change (font scale / display size) | `PlayerViewModel.kt`, `MainActivity.kt` | Pending | TBD |
| **ISSUE-004** | Phase 1 | P2 | Storage / Playback | A/V offsets set on video opened via VIEW intent lost if not indexed in Room library | `PlayerViewModel.kt`, `MediaRepositoryImpl.kt` | Pending | TBD |
| **ISSUE-005** | Phase 1 | P2 | UI / Accessibility | Stats overlay text wraps awkwardly at 200% accessibility font scale | `StatsOverlay.kt` | Pending | TBD |
| **ISSUE-006** | Phase 1 | P3 | Android Lint | 3 `ApplySharedPref` warnings using blocking `commit()` in `MediaTransferJournal` | `MediaTransferJournal.kt` | Pending | TBD |
| **ISSUE-007** | Phase 1 | P3 | Android Lint / Platform | Android 12+ PiP transition recommendation: `setAutoEnterEnabled(true)` and `setSourceRectHint` missing | `AndroidManifest.xml`, `MainActivity.kt` | Pending | TBD |
| **ISSUE-008** | Phase 1 | P2 | Architecture / Maintainability | Massive God-composables (`PlayerScreen.kt` 2,172 lines; `MediaLibraryScreen.kt` 2,137 lines) | `PlayerScreen.kt`, `MediaLibraryScreen.kt` | Pending | TBD |
| **ISSUE-009** | Phase 1 | P2 | Architecture / Maintainability | `PlayerViewModel.kt` is 1,582 lines managing multiple disjoint feature states | `PlayerViewModel.kt` | Pending | TBD |
| **ISSUE-010** | Phase 1 | P3 | Code Hygiene | 36 `UseKtx` Android Lint warnings across core and data layers | Various | Pending | TBD |
| **ISSUE-011** | Phase 1 | P2 | Stability / Playback | Playback start stall / black screen report on hardware (watchdog fallback in PR #18) | `Media3PlaybackController.kt` | Investigating | CI connected playback tests |
| **ISSUE-012** | Phase 1 | P2 | Storage / Domain | Global audio sync offset storage ambiguity (per-video vs global setting) | `ApturaSettingsRepository.kt`, `PlayerViewModel.kt` | Pending | TBD |
| **ISSUE-013** | Phase 2 | P2 | Playback / Audio | `AudioDelayProcessor` only supports 16-bit PCM; float/hi-res PCM silently bypasses lip-sync delay | `AudioDelayProcessor.kt` | Pending | TBD |
| **ISSUE-014** | Phase 2 | P2 | Error Handling | Blind `catch (_: Throwable)` blocks swallow system errors and avoid diagnostic tracing | `Navigation.kt`, `ApturaApplication.kt` | Pending | TBD |
| **ISSUE-015** | Phase 2 | P3 | UI / UX | Silent no-op on `Open With` and `Share` if no handler application is installed | `Navigation.kt` | Pending | TBD |
| **ISSUE-016** | Phase 2 | P3 | Architecture / Dead Code | Unused `deleteRemovedVideos` query in `MediaItemDao` | `MediaItemDao.kt` | Pending | TBD |
| **ISSUE-017** | Phase 2 | P3 | Android Lint / Resources | `res/mipmap-anydpi-v26` triggers `ObsoleteSdkInt` lint warning (minSdk is 26) | `res/mipmap-anydpi-v26` | Pending | TBD |
| **ISSUE-018** | Phase 2 | P3 | Code Hygiene | 36 `UseKtx` lint warnings (`Uri.parse()` instead of `toUri()`) | Various | Pending | TBD |
| **ISSUE-019** | Phase 2 | P2 | Architecture / Maintainability | Decompose `PlayerScreen.kt` and `MediaLibraryScreen.kt` into modular sub-composables | `PlayerScreen.kt`, `MediaLibraryScreen.kt` | Pending | TBD |
| **ISSUE-020** | Phase 3 | P1 | Security / Privacy | `RedactionEngine` misses removable SD card storage paths (`/storage/<uuid>/`) | `RedactionEngine.kt` | Verified | `DiagnosticRedactionTest.redactionEngine_stripsRemovableSdCardPaths` PASSED |
| **ISSUE-021** | Phase 3 | P2 | Security / DoS | `SubtitleZipExtractor` lacks byte ceiling during extraction (Zip Bomb risk) | `SubtitleZipExtractor.kt` | Verified | `SubtitleZipExtractorTest.extractSubtitleFromStream_exceedsMaxBytes_abortsWithFailure` PASSED |
| **ISSUE-022** | Phase 4 | P2 | Performance / Compose | 4 Hz position ticker triggers full `PlayerScreen` recomposition when controls hidden | `PlayerScreen.kt`, `Media3PlaybackController.kt` | Pending | TBD |
| **ISSUE-023** | Phase 4 | P2 | Performance / Database | Room table invalidation re-queries entire library on 5s playback checkpoints | `MediaLibraryViewModel.kt`, `MediaItemDao.kt` | Pending | TBD |
| **ISSUE-024** | Phase 5 | P2 | Navigation / Lifecycle | In-app Back arrow from external `ACTION_VIEW` launch drops user into Welcome / Onboarding flow | `Navigation.kt` | Pending | TBD |
| **ISSUE-025** | Phase 5 | P2 | Audio / Platform | Bluetooth auto-sync hint (`Try −200 ms`) reflects connected devices rather than active audio output route | `AudioEnhancementSheet.kt` | Pending | TBD |
| **ISSUE-026** | Phase 5 | P3 | Accessibility / UI | Sleep timer presets lack selected accessibility state for TalkBack screen reader | `SleepTimerSheet.kt` | Pending | TBD |

---

## Detailed Issue Tracking Records

### ISSUE-001: Sleep timer lost on app task removal while playback continues
- **Phase:** Phase 1 / Phase 5
- **Priority:** P1 (High)
- **Category:** Playback / Process Lifecycle
- **Affected Files:** `app/src/main/java/org/aptura/player/ui/player/PlayerViewModel.kt`, `app/src/main/java/org/aptura/player/data/playback/Media3PlaybackController.kt`, `app/src/main/java/org/aptura/player/domain/usecase/SleepTimer.kt`
- **Root Cause:** Sleep timer coroutine job and deadline tracking was hosted within `PlayerViewModel.viewModelScope`. When swiping the app away from Recents, the activity is destroyed and `PlayerViewModel.onCleared()` cancels `viewModelScope`. However, `ApturaPlaybackService` continues running as a foreground service, causing audio to play all night without ever fading or pausing.
- **Proposed Solution:** Move `SleepTimer` into the pure Kotlin `domain/usecase` layer. Bind its lifecycle to `PlaybackController` / `Media3PlaybackController` inside `PlayerManager` (held by `ApturaPlaybackService`), and have `PlayerViewModel` delegate to it and observe its state.
- **Regression Risks:** Disarming of `stopAfterCurrentItem` when a video ends must be self-contained in the service layer if the UI is dead.
- **Acceptance Criteria:** Clearing the ViewModel or destroying the Activity while a sleep timer is active does not cancel the timer; audio continues fading at deadline - 10s and pauses at deadline.
- **Verification Plan:** Unit tests with mock clock simulating ViewModel clearing during active countdown.

### ISSUE-002: PiP affordance silent no-op when Picture-in-Picture app-op is disabled
- **Phase:** Phase 1 / Phase 5
- **Priority:** P2 (Medium)
- **Category:** UI / Platform Integration
- **Affected Files:** `app/src/main/java/org/aptura/player/ui/player/PlayerScreen.kt`, `app/src/main/java/org/aptura/player/MainActivity.kt`
- **Root Cause:** When Picture-in-Picture permission/app-op is revoked in system settings on a device that supports PiP, clicking the PiP button triggers `enterPictureInPictureMode()` which returns false or fails silently without providing user feedback or disabling the option.
- **Proposed Solution:** Check `AppOpsManager` or `activity.isInPictureInPictureMode` / capability before rendering or show an explanatory snackbar/notice directing the user to system settings.
- **Acceptance Criteria:** Tapping PiP when denied displays an informative message or the control is visibly disabled with an explanation.

### ISSUE-003: Video resumes playing after configuration change (font scale)
- **Phase:** Phase 1 / Phase 5
- **Priority:** P2 (Medium)
- **Category:** Lifecycle / Configuration
- **Affected Files:** `app/src/main/java/org/aptura/player/ui/player/PlayerViewModel.kt`, `app/src/main/java/org/aptura/player/MainActivity.kt`
- **Root Cause:** Activity recreation on `font_scale` change triggers re-initialization of playback or auto-resumes playback state unexpectedly.
- **Proposed Solution:** Ensure paused state is strictly preserved during activity configuration changes without auto-play triggering.
- **Acceptance Criteria:** If video was paused prior to configuration change, it remains strictly paused after recreation.

### ISSUE-006: Synchronous `commit()` used on SharedPreferences in `MediaTransferJournal`
- **Phase:** Phase 1 / Phase 4
- **Priority:** P3 (Low)
- **Category:** Android Lint / Disk I/O
- **Affected Files:** `app/src/main/java/org/aptura/player/data/transfer/MediaTransferJournal.kt:82, 89, 105`
- **Root Cause:** `commit()` synchronously writes to disk on the calling thread, triggering Android Lint `ApplySharedPref` warnings.
- **Proposed Solution:** Assess whether synchronous durability is strictly needed for the crash journal; if asynchronous persistence is safe or already guarded by I/O dispatcher, use `apply()` or document exception.
- **Acceptance Criteria:** Lint warning resolved or documented justification in `lint-baseline.xml`.

### ISSUE-007: Android 12+ PiP transition recommendation missing
- **Phase:** Phase 1 / Phase 5
- **Priority:** P3 (Low)
- **Category:** Platform / Lint
- **Affected Files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/org/aptura/player/MainActivity.kt`
- **Root Cause:** Target SDK 36 requires `setAutoEnterEnabled(true)` and `setSourceRectHint` on Android 12+ (API 31+) for smooth PiP entry transitions.
- **Proposed Solution:** Provide source rect hint corresponding to video surface dimensions and enable auto-enter on API 31+.
- **Acceptance Criteria:** Smooth transition into PiP without jarring cut or lint warning.

### ISSUE-008: Monolithic composables exceeding 2,000 lines
- **Phase:** Phase 1 / Phase 2
- **Priority:** P2 (Medium)
- **Category:** Architecture / Maintainability
- **Affected Files:** `app/src/main/java/org/aptura/player/ui/player/PlayerScreen.kt` (2,172 lines), `app/src/main/java/org/aptura/player/ui/library/MediaLibraryScreen.kt` (2,137 lines)
- **Root Cause:** Excessive accumulation of inline dialogs, sheets, gestures, and event handlers in a single Composable file.
- **Proposed Solution:** Modularize into discrete composables (e.g. `PlayerTopBar`, `PlayerCenterControls`, `PlayerBottomScrubber`, `LibraryTopBar`, `LibraryGridContent`, etc.) following UDF and `@Stable` state parameters.
- **Acceptance Criteria:** Maintainability improved, recomposition scopes minimized, zero visual or behavioral regressions.

### ISSUE-013: AudioDelayProcessor silently bypasses lip-sync offset on non-16-bit PCM
- **Phase:** Phase 2
- **Priority:** P2 (Medium)
- **Category:** Playback / Audio
- **Affected Files:** `app/src/main/java/org/aptura/player/data/playback/AudioDelayProcessor.kt:41`
- **Root Cause:** `AudioDelayProcessor.onConfigure` checks `if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) return AudioFormat.NOT_SET`. If high-resolution audio (24-bit PCM, 32-bit float PCM) or float output is enabled in the audio sink, the delay processor deactivates and the user's audio offset is silently ignored.
- **Proposed Solution:** Support `C.ENCODING_PCM_FLOAT` and `C.ENCODING_PCM_24BIT` or downsample/convert in processor, or document and expose format compatibility state so the user knows if audio sync is active.
- **Acceptance Criteria:** Audio delay is applied for float PCM, or UI indicates when delay is inactive for the current audio format.

### ISSUE-014: Blind `catch (_: Throwable)` blocks swallow system errors and avoid diagnostic tracing
- **Phase:** Phase 2
- **Priority:** P2 (Medium)
- **Category:** Error Handling
- **Affected Files:** `app/src/main/java/org/aptura/player/Navigation.kt:274, 290, 699, 709, 751`
- **Root Cause:** Catching `Throwable` indiscriminately catches JVM virtual machine errors (such as `OutOfMemoryError`, `StackOverflowError`, `ThreadDeath`) and suppresses `SecurityException` during URI permission management without logging to `ApturaDiagnostics`.
- **Proposed Solution:** Narrow exception types to `Exception` or specific checked types (`SecurityException`, `IllegalArgumentException`), and log suppressed failures to `ApturaDiagnostics.logEvent`.
- **Acceptance Criteria:** System errors are never caught; suppressed operational exceptions are logged to local diagnostics.

### ISSUE-015: Silent failure on external intent launch if no app handles intent
- **Phase:** Phase 2
- **Priority:** P3 (Low)
- **Category:** UI / UX
- **Affected Files:** `app/src/main/java/org/aptura/player/Navigation.kt:524, 534`
- **Root Cause:** In `Navigation.kt`, `onOpenWithClick` and `onShareClick` wrap `context.startActivity` in `try { ... } catch (_: Exception) {}` with an empty body. If no app handles the intent or the intent chooser fails, nothing happens on screen.
- **Proposed Solution:** Show an informative `Toast` or snackbar (matching `MediaLibraryScreen.kt:1948`) informing the user that no app is available to open or share the video.
- **Acceptance Criteria:** Actionable feedback shown on screen instead of a silent no-op.

### ISSUE-016: Unused dead query `deleteRemovedVideos` in `MediaItemDao`
- **Phase:** Phase 2
- **Priority:** P3 (Low)
- **Category:** Architecture / Dead Code
- **Affected Files:** `app/src/main/java/org/aptura/player/data/database/MediaItemDao.kt:113`
- **Root Cause:** `deleteRemovedVideos(existingMediaStoreIds: List<Long>)` was left behind when `FileReconciliationEngine` was introduced to batch-delete obsolete items safely without triggering SQLite parameter limits.
- **Proposed Solution:** Remove the unused method when the next database/schema PR is opened, or keep tracked until a database maintenance pass.
- **Acceptance Criteria:** Dead code removed without breaking callers or tests.

### ISSUE-017: Obsolete `-v26` resource qualifier on `res/mipmap-anydpi-v26`
- **Phase:** Phase 2
- **Priority:** P3 (Low)
- **Category:** Android Lint / Resources
- **Affected Files:** `app/src/main/res/mipmap-anydpi-v26`
- **Root Cause:** `minSdkVersion` is 26, so `-v26` qualifier is completely redundant and triggers `ObsoleteSdkInt`.
- **Proposed Solution:** Rename/merge `mipmap-anydpi-v26` to `mipmap-anydpi`.
- **Acceptance Criteria:** `ObsoleteSdkInt` warning eliminated from `lintDebug`.

### ISSUE-020: RedactionEngine misses removable SD card storage paths (`/storage/<uuid>/`)
- **Phase:** Phase 3
- **Priority:** P1 (High)
- **Category:** Security / Privacy
- **Affected Files:** `app/src/main/java/org/aptura/player/core/diagnostics/RedactionEngine.kt:13`, `app/src/test/java/org/aptura/player/core/diagnostics/DiagnosticRedactionTest.kt`
- **Root Cause:** `STORAGE_PATH_PATTERN` matches only `/storage/emulated/\d+/`, `/sdcard/`, `/data/user/\d+/`, and `/data/data/`. On physical Android devices with MicroSD cards, paths follow `/storage/XXXX-XXXX/` (e.g. `/storage/4CE2-1407/Movies/video.mp4`). These paths are NOT redacted by the pattern and can leak file names and directory paths into exported diagnostic logs.
- **Proposed Solution:** Generalize `STORAGE_PATH_PATTERN` to match `/storage/(?:emulated/\d+|[^/\s,\]}"]+)/` in addition to `/sdcard/` and `/data/`.
- **Acceptance Criteria:** Diagnostic export redaction test proves that `/storage/ABCD-1234/Movies/PersonalVideo.mp4` is redacted to `[PATH_REDACTED]` or `[FILE_REDACTED].mp4`.
- **Status:** Verified
- **Resolution & Evidence:** Implemented in `RedactionEngine.kt`. Verified via `DiagnosticRedactionTest.redactionEngine_stripsRemovableSdCardPaths` and full gate.

### ISSUE-021: SubtitleZipExtractor lacks byte extraction ceiling (Zip Bomb vulnerability)
- **Phase:** Phase 3
- **Priority:** P2 (Medium)
- **Category:** Security / Denial of Service
- **Affected Files:** `app/src/main/java/org/aptura/player/core/media/SubtitleZipExtractor.kt:73`, `app/src/test/java/org/aptura/player/core/media/SubtitleZipExtractorTest.kt`
- **Root Cause:** Subtitle ZIP extraction uses unbounded `zis.copyTo(fos)`. A specially crafted zip bomb archive can decompress gigabytes of data into the application cache directory, leading to device storage exhaustion and app crash.
- **Proposed Solution:** Implement bounded stream copying (e.g. capping total extracted bytes at 20 MB, matching `SubtitleFileValidator.MAX_SUBTITLE_BYTES`). Fail cleanly with `ZipExtractionResult.Failure` if extraction exceeds ceiling.
- **Acceptance Criteria:** Uncompressing an entry exceeding 20 MB terminates immediately without filling disk.
- **Status:** Verified
- **Resolution & Evidence:** Implemented bounded stream copy with `MAX_SUBTITLE_BYTES = 20 MB` in `SubtitleZipExtractor.kt`. Verified via `SubtitleZipExtractorTest.extractSubtitleFromStream_exceedsMaxBytes_abortsWithFailure` and full gate.

### ISSUE-022: 4 Hz position ticker triggers full `PlayerScreen` recomposition when controls hidden
- **Phase:** Phase 4
- **Priority:** P2 (Medium)
- **Category:** Performance / Compose
- **Affected Files:** `app/src/main/java/org/aptura/player/ui/player/PlayerScreen.kt:203`, `app/src/main/java/org/aptura/player/data/playback/Media3PlaybackController.kt:153`
- **Root Cause:** `PlayerScreen` directly collects `PlayerUiState`. Every 250 ms, `Media3PlaybackController` updates `currentPositionMs`, triggering recomposition of the outer 2,172-line `PlayerScreen` composable even when `isControlsVisible == false` and the user is simply watching video.
- **Proposed Solution:** Isolate `currentPositionMs` updates into the bottom scrubber/timeline composable scope using lambdas or derived state holders so the outer video container and hidden control scrims skip recomposition.
- **Acceptance Criteria:** Recomposition counts for `PlayerVideoSurface` and outer container remain 0 during continuous playback with controls hidden.

### ISSUE-023: Room table invalidation re-queries entire library on 5s playback checkpoints
- **Phase:** Phase 4
- **Priority:** P2 (Medium)
- **Category:** Performance / Database
- **Affected Files:** `app/src/main/java/org/aptura/player/ui/library/MediaLibraryViewModel.kt:100`, `app/src/main/java/org/aptura/player/data/database/MediaItemDao.kt:22`
- **Root Cause:** Room table invalidation tracks writes at table granularity. Every 5 seconds during playback, `PlayerViewModel` writes `mediaItemDao.updatePlaybackPosition(...)`. This invalidates `getAllVideosSortedByDate()`, causing `MediaLibraryViewModel` to re-fetch all rows from SQLite, re-sort, and re-allocate UI card models even when library contents have not changed.
- **Proposed Solution:** Add `distinctUntilChanged` on key entity fields or separate playback position tracking from static media metadata, or decouple checkpoint updates so the library flow only updates when titles, completion, or membership change.
- **Acceptance Criteria:** Inactive library view does not re-allocate all card UI models every 5s during active video playback.

### ISSUE-024: In-app Back arrow from external ACTION_VIEW launch drops user into Welcome / Onboarding flow
- **Phase:** Phase 5
- **Priority:** P2 (Medium)
- **Category:** Navigation / Lifecycle
- **Affected Files:** `app/src/main/java/org/aptura/player/Navigation.kt:313-317`
- **Root Cause:** When Aptura is launched via an external `ACTION_VIEW` intent on a clean install (or before onboarding was completed), `backStack` is initialized with `[Onboarding]`. The launch effect pushes `PlayerKey` so the stack is `[Onboarding, PlayerKey]`. Pressing Back or the top-bar arrow pops `PlayerKey`, dropping the user into the `Onboarding` welcome screen instead of finishing the activity. Even if onboarding was already completed, `backStack` has `[MediaLibrary, PlayerKey]`, dropping the user into `MediaLibrary` instead of finishing the activity to return to the calling app.
- **Proposed Solution:** In `handleBack`, if the activity was launched with an external `ACTION_VIEW` intent, finish the activity when popping the player instead of dropping the user into the library or onboarding.
- **Acceptance Criteria:** Opening a video from an external file manager and pressing Back cleanly exits Aptura and returns to the calling app.

### ISSUE-025: Bluetooth auto-sync hint (`Try −200 ms`) reflects connected devices rather than active audio output route
- **Phase:** Phase 5
- **Priority:** P2 (Medium)
- **Category:** Audio / Platform
- **Affected Files:** `app/src/main/java/org/aptura/player/ui/player/AudioEnhancementSheet.kt:143`
- **Root Cause:** `AudioEnhancementSheet.rememberBluetoothOutputConnected()` queries `audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in BLUETOOTH_OUTPUT_TYPES }`. This returns true if any Bluetooth headset is bonded or connected, even if the active media output route is currently routed to the built-in speaker.
- **Proposed Solution:** Inspect active routing via `AudioTrack.getRoutedDevice()` or verify device state against active communication / media routing.
- **Acceptance Criteria:** `Try -200 ms` only appears when audio is actively routed to Bluetooth output.

### ISSUE-026: Sleep timer presets lack selected accessibility state for TalkBack screen reader
- **Phase:** Phase 5
- **Priority:** P3 (Low)
- **Category:** Accessibility / UI
- **Affected Files:** `app/src/main/java/org/aptura/player/ui/player/SleepTimerSheet.kt`
- **Root Cause:** When a sleep timer is actively counting down, no chip is marked selected in the UI (`selected = false` for all chips because `activeTimerMinutes` is 0 or countdown is in progress). TalkBack announces all chips as unselected, so visually impaired users cannot hear which preset was chosen.
- **Proposed Solution:** Ensure the matching preset chip corresponding to the active timer duration announces selected or has an active indicator for screen readers.
- **Acceptance Criteria:** Screen readers announce which sleep timer preset is currently active.



