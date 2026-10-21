# Aptura — Persistent Agent Memory (`MEMORY.md`)

> **Location:** `.agents/MEMORY.md`  
> **Purpose:** Persistent memory across conversation compaction turns. Summarizes current phase, locked decisions, verified facts, and critical operational rules.

---

## 1. Project Identity & Pinned Constants
- **Product Name:** Aptura Video Player
- **Package ID:** `org.aptura.player`
- **Current Phase:** B0.0 Instrumentation & Scaffolding Entry (Spec Phase 0–6 Complete; Part 42 Committed)
- **Git Branch:** `main`

---

## 2. All 6 Confirmed Owner Decisions (100% Locked)
1. **OD-1 (Platform):** Android-first release. iOS evidence-gated at R1.x (`DEC-002`).
2. **OD-2 (Scope Boundary):** Local-only personal media player. Zero network, no SMB/NAS in R1 (`DEC-001`).
3. **OD-3 (Monetization):** Free, zero ads, no accounts at launch (`DEC-004`).
4. **OD-4 (Team & Execution):** Solo AI Developer (Antigravity) + Product Owner (User) model ($0 payroll, sequential build execution, Q16-7 = 1, RB-2 = 1).
5. **OD-5 (Market Scope):** Global targeting from Day 1 (English baseline UI, multi-script subtitle support for Arabic RTL, CJK, Devanagari, Cyrillic; global statute compliance under Brief L5).
6. **OD-6 (Brand):** Primary brand name **Aptura** (`org.aptura.player`) with clearance shortlist: 1. Aptura, 2. Caelis, 3. Pellucis (0 collisions in Google Play video players).

---

## 3. Top Architectural & Platform Invariants
- **ZERO `android.permission.INTERNET`:** Never add this permission to `AndroidManifest.xml` (`DEC-012`).
- **ZERO Telemetry SDKs:** No Firebase, Sentry, Mixpanel, Crashlytics (`ADR-007`). Local diagnostics only (`spec/14`).
- **Storage:** Scoped storage only. Never `MANAGE_EXTERNAL_STORAGE` (`DEC-011`).
- **Licensing:** LGPL-2.1 dynamic linking only for FFmpeg audio extension (`DEC-009`, Brief L1). No bundled video decoders in R1 (`ADR-010`).
- **UI Architecture:** Jetpack Compose + Material 3, adhering strictly to [DESIGN.md](file:///c:/Users/tejas/Desktop/Mobile%20App/DESIGN.md) and [.agents/rules/taste-compose.md](file:///c:/Users/tejas/Desktop/Mobile%20App/.agents/rules/taste-compose.md).
- **Tooling Rule:** In Windows PowerShell, NEVER chain commands with `&&` (causes syntax errors). Use `;` or separate commands.

---

## 4. Current Progress & Executed Milestones
- **B0.0 Instrumentation (Part 45):** 1,086 test fixtures generated, `fixtures/manifest.csv` pinned, test harness contracts implemented.
- **Spike RQ-10 (Part 46):** Privacy-Preserving Diagnostics (PASSED). 5/5 failure classes diagnosable without PII.
- **Spike RQ-07 (Part 47):** Process-Death & State Restoration (PASSED). 12/12 kill points restored contractually.
- **Spike RQ-02 (Part 48):** Royalty-Safe LGPL FFmpeg Audio Extension Config (PASSED).
- **Spike RQ-14 (Part 49):** ABI & Language Bundle Strategy (PASSED). Universal APK 16.1 MB, per-ABI 12.5 MB, language split disabled for zero-network offline safety.
- **Spike RQ-09 (Part 50):** iOS Viability Gate (CLOSED / DEC-002 REAFFIRMED). Pure Kotlin domain contracts established with automated zero-Android-import assertion (`ArchitectureDecouplingTest.kt`).
- **Phase B1 Kickoff (Part 51):** `ApturaPlaybackService` (Media3 foreground service with audio focus and platform notification exemption), Room database (`MediaItemEntity`, `MediaItemDao`, `ApturaDatabase`), and `MediaRepositoryImpl` implemented.
- **Phase B1 Media Ingest (Part 52):** `MediaStoreScanner` (sub-second cold scan projection), `MediaScanWorker` (`WorkManager` differential ingest with progress preservation), and `MediaScanScheduler` implemented.
- **Phase B1 Playback Controller Bridge (Part 53):** `Media3PlaybackController` bridging the live `MediaSession` to domain `PlaybackController` (StateFlow mappings, 3s seek-to-previous threshold, speed clamping, and repeat/shuffle).
- **QA Review & Hardening (Part 54):** Eliminated N+1 SQLite queries in `MediaScanWorker` via single bulk fetch and chunked 500-item deletes. Fixed `Media3PlaybackController` error state clobbering bug; added `playQueue` and repeat-mode wrap-around navigation; expanded test suite with 5 new tests.
- **Invariants Gate & Anti-Confirmation Mandate (Part 55):** Codified 3-Point Pre-Mortem Falsification Protocol. Implemented `PlatformInvariantsTest.kt` asserting zero INTERNET, zero MANAGE_EXTERNAL_STORAGE, zero POST_NOTIFICATIONS, zero telemetry SDKs, and offline bundle invariants.
- **Test Baseline:** 57 unit tests passing across 13 suites (100% green). Android Lint static analysis clean (0 errors, 0 security warnings).
- **Crash-Proof Scans & Edge-to-Edge Cleanup (Part 56):** Hardened `MediaStoreScanner` to safely handle column lookups on API 26-28 and OEM variations. Stripped deprecated window bar color overrides in `Theme.kt` for Android 15 edge-to-edge.
- **Media Library Grid UI (Part 57):** Implemented Coil hardware video frame decoder via `ApturaApplication`, `@Immutable` `MediaLibraryUiState`, reactive `MediaLibraryViewModel` combining 5 flows with instant search and folder chip filtering, and adaptive Compose `MediaLibraryScreen` (16:9 thumbnails, `tnum` tabular timecodes, watch progress bars, empty/loading states per `taste-compose.md`). Implemented 8 new unit tests in `MediaLibraryViewModelTest`.
- **Player Screen & Video Surface HUD (Part 58):** Implemented `PlayerScreen.kt` (Media3 PlayerView interop, dual-half gesture layers for volume/brightness, top/bottom scrims, auto-hide HUD), `PlayerViewModel.kt` (event-driven 5s checkpointing, comfort pre-roll restoration, multi-tap seek accumulation, speed/aspect ladders), `PlayerManager.kt` thread-safe singleton, and 14 unit tests in `PlayerViewModelTest.kt`. Permanently hardened quality gates: configured 30s Gradle test timeout in `build.gradle.kts`, codified zero-polling and Media3 AndroidX OptIn rules in `coding-standards.md`, and created atomic validation script `tools/gate.ps1`. 71 unit tests passing across 14 suites (100% green); lint clean.
- **Automated Compose UI Testing on JVM (Part 59):** Integrated Robolectric 4.14.1 and Compose testing APIs (`createComposeRule()`) into JVM testing pipeline. Created `PlayerScreenUiTest.kt` (5 tests) and `MediaLibraryScreenUiTest.kt` (4 tests) asserting real Compose semantic hierarchies, buttons, scrims, filter chips, and click callbacks. All 80 unit & UI tests passing across 16 suites (100% green); lint clean.
- **Active Skills & Tooling:** `android-cli`, `jetpack-compose-performance`, `adaptive`, `edge-to-edge`, `testing-setup`, QA & Code Review Gate, Compose Taste rules, and frozen Invariants.

---

## 5. Next Immediate Milestones & Continuous Evolution Roadmap
- **Advanced Audio & Video Customization Suite (Part 60):** Implemented 5-band Equalizer, Presets, Bass Boost, Virtualizer, Loudness Enhancer, Subtitle Delay (-10s to +10s), Audio Sync Delay (-5s to +5s), Sleep Timer, Frame Stepper, A-B Repeat Looper, Picture Adjustments (Night Mode / Eye Comfort, Contrast, Saturation) with 94 unit/UI tests passing.
- **Player Screen Alignment & Scrubber Modernization (Part 61):** 
  - Fixed horizontal and vertical misalignment of Center Transport Cluster (`⏮ ⏪10 ⏯ ⏩10 ⏭`) by switching HUD controls overlay from `Column(SpaceBetween)` (which pinned pill to `Alignment.Start` and shifted it upwards) to `Box(fillMaxSize())` with `.align(Alignment.Center)`. Mathematically dead-centers the controls (540px X, 1200px Y on 1080x2400) across all devices and orientations.
  - Replaced rigid bordered pill surface with modern floating circular controls (72dp primary Play/Pause, 48/52dp secondary controls with translucent scrims).
- **Pixel-Perfect Concentric Scrubber & Commercial Information Architecture (Part 62):**
  - **Scrubber Concentric Alignment (0.0px Error):** Diagnosed 2.67px vertical offset and black void gap in Material 3's `Slider`. Replaced with custom Compose `Canvas` video scrubber where both track and thumb calculate from unified midline `cy = size.height / 2f`. Verified on Pixel 8 framebuffer: Thumb Y = 2142.0, Track Y = 2142.0 (0.0px offset). Edge clamping prevents clipping at 0% and 100%.
  - **Commercial Information Architecture (IA):** Deconstructed 2,500px developer debug dumping ground into clean, context-driven sheets:
    1. **`AudioSheet`:** Audio Track list + Lip-Sync Audio Delay Stepper (with Bluetooth presets +150ms/+250ms/-100ms) + Equalizer presets (Flat, Bass Boost, Vocal, Rock) & 5-band EQ + Bass Boost, Virtualizer, Loudness Boost.
    2. **`SubtitleSheet`:** Subtitle Track list + Subtitle Delay Stepper with 1-tap "Reset (0ms)" and step size chips. Removed duplicate audio delays.
    3. **`PlaybackSettingsSheet` (Segmented Tab Bar):**
       - **Tab 0 (Playback Tools):** Speed pills (0.5x–2.0x), Sleep Timer (15/30/45/60m with live countdown & cancel), A-B Repeat Loop, Frame-by-Frame Stepper (~42ms).
       - **Tab 1 (Picture & Display):** Aspect Ratio chips (FIT, FILL, ZOOM, 16:9, 4:3), Eye Comfort Night Mode toggle & warmth slider, Picture Calibration (Contrast & Saturation with "Reset to Natural").
  - **Visual Verification Gate:** Live emulator screencaps captured and visually validated: `player_hud_new.png`, `sheet_audio.png`, `sheet_subtitles.png`, `sheet_settings_tab0.png`, `sheet_settings_tab1.png`. All 94 unit/UI tests green, lint clean via `tools/gate.ps1`.
- **Commercial UX, Privacy Onboarding & Transparency Architecture (Part 63):**
  - **First-Run Onboarding (`SCR-001`):** 3-page pager highlighting 100% offline privacy, zero network sockets, precision engine, and scoped storage access with "Grant Media Access" and "Continue without access (Limited Mode)" CTAs.
  - **Limited Storage Mode & Database Safeguard (`SCR-012`, `FR-LIB-001..003`):** High-contrast warning banner when storage permission is denied; wired SAF document picker (`ACTION_OPEN_DOCUMENT_TREE`) and permission re-request. Guarded `MediaScanWorker` with `PermissionHelper.hasStoragePermission` to permanently prevent accidental `clearAll()` database wipes on revoked permission.
  - **Media Details Sheet & Non-Destructive Removal (`SCR-009`, `SCR-018`):** Granular metadata sheet (full path, duration, file size, MIME format, watch status) and destructive-action confirmation dialog to remove items from Aptura index without mutating physical storage (`DEC-001`).
  - **Commercial Settings & Transparency Root (`SCR-013..016`):** DataStore-backed asynchronous preferences (`ApturaSettingsRepository`), live library footprint statistics (`LibraryStatistics`), offline privacy manifesto, redacted local diagnostic markdown export (`ApturaDiagnostics`), and third-party license SBOM.
  - **Up Next / Auto-Advance Countdown (`SCR-010g`, `FR-CTRL-017`):** 10-second circular progress ring with tabular timecodes (`tnum`), cancellable and immediate play controls on playback completion.
  - **System "Open With" Handoff (`FR-LIB-020`):** Registered `ACTION_VIEW` intent filter for `video/*` in `AndroidManifest.xml`, routing external launches directly to `PlayerScreen`.
  - **Verification Gate:** Added 6 new unit tests (`ApturaSettingsRepositoryTest`, `PlayerViewModelTest`, `MediaLibraryViewModelTest`). `tools/gate.ps1` 100% green (Compilation: 4.27s, Fail-Fast Unit Tests: 4.69s, Lint: 4.05s, 0 errors, 0 warnings).
- **Mandatory Quality Gate:** Every commit and review must pass `tools/gate.ps1` (100% tests green, 0 lint errors) and adhere to the visual inspection and contextual IA invariants.





