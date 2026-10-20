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

## 4. Immediate Roadmap (B0.0 Instrumentation)
- Stage 1: Android project scaffolding (`applicationId = org.aptura.player`).
- Stage 2: Media corpus generation scripts & manifest CSVs per [spec/12A §2](file:///c:/Users/tejas/Desktop/Mobile%20App/spec/12A_B0_INSTRUMENTATION.md#L45-L120).
- Stage 3: Test harness contracts for seek latency, AV-sync, and process-death restoration per [spec/12A §3](file:///c:/Users/tejas/Desktop/Mobile%20App/spec/12A_B0_INSTRUMENTATION.md#L125-L185).
