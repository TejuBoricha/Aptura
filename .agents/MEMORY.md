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

## 5. Next Immediate Milestones (Phase B1 Advanced Player Panels & Media Details)
- **Phase B1 Step 6 (SCR-009 & SCR-010e):**
  - **Media Details Sheet (SCR-009):** Metadata viewer showing container format, video codec, audio channels, bitrate, resolution, frame rate, file path, size, date added.
  - **Advanced Player Sheet & Stats (SCR-010e):** Decoder override selector (Auto/HW/SW) with informative descriptions, audio delay calibration slider (±5s in 50ms increments per `FR-AUD-004`), subtitle delay calibration slider (±10s in 50ms increments per `FR-SUB-003`), and real-time playback diagnostics stats overlay (`EV-001…020`).
- **4-Pillar QA Gate:** Automated unit tests, `tools/gate.ps1` execution, and 3-Point Pre-Mortem verification before commit.




