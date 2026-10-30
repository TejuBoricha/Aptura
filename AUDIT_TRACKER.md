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
