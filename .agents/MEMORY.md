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
- **Test Baseline:** 38 unit tests passing across 11 suites (100% green). Android Lint static analysis clean (0 errors, 0 security warnings).
- **Active Skills & Tooling:** `android-cli` (with official Android docs KB), `jetpack-compose-performance`, `adaptive`, `edge-to-edge`, `testing-setup`, QA & Code Review Gate, Compose Taste rules, and frozen Invariants.

---

## 5. Next Immediate Milestones (Phase B1 Jetpack Compose Media Grid & Player HUD)
- Implement Jetpack Compose Media Grid with Coil video thumbnail decoding, folder filtering, and instant search.
- Implement Player HUD screen with edge-to-edge gestures and tabular figures.




