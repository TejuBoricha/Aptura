# 12 — QA Strategy, Test Plan, and Acceptance Criteria

**Version:** 0.3 (draft; v0.3 cross-references `08B` §3/§3.5 as the procurement plan that **closes toward** this document's §6 matrix — the B0 lab is a subset of it, not a separate fleet (Part 32, D-23); v0.2 adds the TST-QUEUE end-of-item suite)
**Date:** 2026-10-19
**Status:** Draft. **Nothing in this document has been executed.** No test results exist; no device or benchmark claims are made anywhere in this package. All numeric budgets marked `[T]` are provisional QA targets to be ratified at Gate A (doc 18). This document defines *what the suites cited across docs 04/05/10 actually are* and how quality is proven before release.
**Consumes:** 04 (FRs + TST refs), 05 (NFRs + verification methods), 07 (tier matrix — testing gates every claim), 09 (states/architecture), 10 (privacy proofs, threat register), 11 (license CI gate).
**Feeds:** 13 (benchmark methodology), 15 (release ops), 18 (requirement↔test traceability; authoritative case matrix).

---

## 1. QA posture

1. **Honest failure is a pass; silent wrong behavior is a fail.** For a trust product, a mishandled file that *clearly reports* its state beats a file that "mostly" plays while lying (auto-fallback without notice, skipped corrupt frames without a counter, progress bar that pretends).
2. **Claims are test artifacts.** Every capability sentence in UI or store copy must trace to a doc 07 tier that a test validates. Copy audit is a release gate (VIS suite, §4).
3. **Data integrity is inviolable.** The app never mutates, moves, or deletes user media files (doc 09). Tests assert read-only behavior at the filesystem level, not just by review.
4. **Adversarial by default.** Local-file players eat untrusted bytes (doc 10 THR-001). A crash on a malformed file is a defect, not a "bad file".
5. **No vanity coverage.** Coverage targets apply to critical-path logic (core/indexing, session state machine, subtitle pipeline), not package totals.

---

## 2. Quality priorities (ranked; drives severity, not just test order)

| P | Area | Rationale |
|---|---|---|
| 1 | Reliability & data integrity | Crash/ANR/history loss destroys the core promise (KPI-REL) |
| 2 | Truthfulness | States, stats, errors, and copy must match reality (doc 07 §13) |
| 3 | Playback correctness | AV sync, seek accuracy, lifecycle transitions, resume fidelity |
| 4 | Performance within NFR budgets | Provisional `[T]` numbers in doc 05, measured at Gate B |
| 5 | Accessibility & i18n | NFR-A11Y as a launch requirement, not a backlog item |
| 6 | Visual polish | Calm execution quality (DH-5), lowest severity class |

---

## 3. Test levels and tooling

| Level | What it covers | Tooling approach | Cadence | Budget `[T]` |
|---|---|---|---|---|
| Unit (JVM) | Pure logic: identity/reconciliation, subtitle parse + charset detection, time math, session state machine, settings migrations, error-code mapping | JUnit-family + Kotlin test | Every change | <5 min |
| Integration | Room migrations, DataStore, WorkManager, MediaStore/SAF abstractions with fakes | Robolectric per-change; real-device variants nightly | Change/nightly | Robolectric <15 min |
| Instrumented UI | Journeys JRN-01…08, screens SCR-*, permission dialogs, PiP, share/open-with intent flows | Compose UI test + androidx.test + UIAutomator for system surfaces | Nightly + RC | nightly <2 h `[T]` |
| System/performance | Startup, scroll, scan, jank, soak | Macrobenchmark; protocol owned by doc 13 | Weekly + RC | weekly |
| Fault injection | Storage removal/revocation, low space, clock changes, battery saver, process-kill matrix, thermal soak | Harness scripts + device lab; emulator for logic only | Weekly + RC | per suite |
| Adversarial/fuzz | Subtitle parsers, URI/intent handling, metadata ingestion, our native audio ext boundary | JVM fuzzer (Jazzer-class) + native fuzzer (libFuzzer-class) where applicable | Nightly (time-boxed) + RC | 30–60 min/run `[T]` |
| Manual charters | Gesture ergonomics, subtitle fidelity eyeball protocol, TalkBack sweeps, copy/claims audit | Charter-driven exploratory | RC (per release) | 1–2 days total `[T]` |
| Privacy proofs | No-network verification, deletion correctness, redaction fidelity | Manifest assertion + runtime monitor + hash-diff fixtures | RC (part of Gate A/B) | — |

---

## 4. Suite map (resolves the TST-* references in docs 04/05/10)

Case IDs allocate inside these suites exactly as already cited (e.g. `TST-SCAN-01…06`); the case-level matrix — every TST ID ↔ FR/NFR ↔ device class ↔ gate — is maintained in **doc 18** (authoritative). Suites added by this doc are marked ➕.

**Library & onboarding**

| Suite | Scope | Levels |
|---|---|---|
| TST-ONB | First-run flow, permission rationale copy, skip path | Instrumented, manual |
| TST-PRM | Permission matrix: grant/deny/partial/revoke per Android version; degraded-mode banner; auto-rescan on return | Instrumented + manual |
| TST-SCAN | Scan correctness, incremental results, cancellation, resumability after process death, MediaStore/SAF event handling | Integration + instrumented |
| TST-META | Metadata extraction fields, rotation, duration correctness, HDR/DV attribute detection feeding doc 07 tiers | Unit + integration |
| TST-THB | Thumbnail generation quality, disk cap, low-battery pause, cache invalidation on file change | Integration + fault injection |
| TST-LIBV | Library grid/list rendering budgets, paging, sort | Macrobenchmark + instrumented |
| TST-FILT / TST-SRCH | Filter semantics, search index behavior, recent searches | Unit + instrumented |
| TST-DETL / TST-HIST / TST-RSUM | Details truth, history write semantics (stop-time accuracy), resume banner | Unit + instrumented |
| TST-FAV / TST-PLST / TST-RCN | Favorites, playlist reorder/queue interplay, folder/SAF reconnect matching | Instrumented + fault injection |
| TST-EMP / TST-OPEN / TST-DUP / TST-STORE | Empty states, external open/share intents, duplicate handling policy, storage-classed sources | Instrumented + manual |
| TST-SDCARD | SD/adoptable-storage variance, removal mid-scan/mid-playback | Fault injection + manual |

**Playback**

| Suite | Scope | Levels |
|---|---|---|
| TST-PLAY | PLAY-001…0NN core conformance: play/stop/repeat, per doc 07 A/B-tier cells on D1–D3 | Instrumented + manual on real devices |
| TST-CMPT | Tier-matrix validation harness: each claimed cell gets ≥1 batch-p fixture run; unsupported cells must produce the *right* error, not a spinner | Instrumented (drives doc 07 promotion pipeline) |
| TST-DEC | Decode-path election: hw → sw fallback order, honest fallback notice, override, error taxonomy | Instrumented + manual |
| TST-ERR | Error taxonomy: every code reachable and mapped to SCR-010x pattern with working actions | Unit (mapping) + instrumented |
| TST-SEEK / TST-SYNC / TST-STALL / TST-VFR | Seek accuracy (± budget `[T]`), AV sync drift bounds, buffering/underrun behavior, VFR correctness | Harness (doc 13 methods) |
| TST-CORR / TST-BIT | Corruption salvage behavior + counters (never silent), bitrate/timestamp anomalies | Fault injection |
| TST-ORI / TST-GEOM / TST-RATE | Rotation metadata, aspect modes/crop, playback-speed pitch behavior | Instrumented + manual |
| TST-HDR / TST-4K | HDR/DV honest-handling paths (declared vs actual, tonemap pass-through only where doc 07 validates), 4K heat/decoder limits | Manual on qualifying devices |
| TST-REST / TST-DCRASH | Resume correctness incl. 12-point process-kill matrix; restore is *never silently from zero* | Fault injection + unit |
| TST-LEAK / TST-BG / TST-DEG | Codec/session leak sweep, foreground/background policy compliance, degraded-mode transitions | Instrumented + manual |

**Controls & settings**

| Suite | Scope | Levels |
|---|---|---|
| TST-CTRL / TST-GEST | Every FR-CTRL control incl. scrub preview, lock mode, gesture zones vs system gesture conflicts | Instrumented + manual (real devices, OEM variance) |
| TST-QUEUE ➕ | End-of-item matrix: Single/Folder/Queue/Repeat one/Repeat all × auto-advance on/off × queue present/absent × missing next item × lock mode × background | Instrumented + manual |
| TST-PREF / TST-ERGO | Preference persistence semantics; ergonomics charter (thumb reach, one-hand) | Unit + manual |
| TST-EXT | External intent/assistant/media-key dispatch variance | Manual |
| TST-SET / TST-DIAG | Settings truth (no orphan toggles), diagnostics copy, redaction preview fidelity | Instrumented + manual |
| TST-PRIV-NET | **(proofs)** no-INTERNET manifest assertion (01/02), runtime egress monitor (03), deletion-of-user-data verification incl. history/favorites/playlists removal (04) | Instrumented + scripted |
| TST-A11Y ➕ | Scripted TalkBack journeys per screen class, contrast token audit, 200% font sweep, reduce-motion, non-touch path | Instrumented + manual |
| TST-VIS ➕ | Visibility/claims audit: string sweep vs doc 07 §13 banned patterns; store listing; debug strings leak check | Manual (checklist, Gate B) |
| TST-FUZZ ➕ | Regression housing for fuzzer findings (every finding → minimized fixture + case) | Automated |

**Media domains**

| Suite | Scope | Levels |
|---|---|---|
| TST-SUB | 31+ cases already scoped in doc 04: matching heuristics, ASS tiers, charset detection, styling controls, adversarial subtitle corpus (§5) | Unit + instrumented + manual fidelity protocol |
| TST-AUD | 23+ cases: track election, delay, route changes, passthrough-vs-effected honest labeling, licensing-gated tiers | Instrumented + manual |
| TST-PLAT | PiP, lifecycle, multi-window, system integration (FR-PLAT) | Instrumented + manual |

---

## 5. Fixture & corpus plan

| Corpus | Content | Rules |
|---|---|---|
| Media matrix | Containers × video × audio × subtitle permutations matching doc 07; **≥3 files per validated A/B-tier cell** `[T]` | Generated by checked-in FFmpeg scripts; sources = our own recordings + CC0/CC-BY media only; **no third-party copyrighted content ever**; each fixture carries a license note |
| Subtitle corpus | Encodings (UTF-8/16/32 ±BOM, Win-125x family, Shift-JIS, GB-class, others); RTL (Arabic/Hebrew); complex scripts (Devanagari, Thai); ASS style tiers | Includes wrong-charset bait files to prove re-encoding UI (TST-SUB) |
| Adversarial media | Truncated (header/mid-packet), zero-byte, absurd durations, backwards timestamps, packet storms, max track counts | Every fixed adversarial bug becomes a permanent fixture (regression law, §10) |
| Adversarial subtitles | 10^5-cue files, overlapping/zero-length cues, malformed tags, BOM storms, mixed encodings | Cap-and-degrade behavior asserted; never OOM/hang |
| Metadata bombs | Multi-MB tag values, huge embedded artwork, long/confusable filenames | Parsing bounded; UI never drops into jank |
| Corpus governance | Versioned + checksummed; stored in build-phase repo/lfs with license manifest; growth driven by bug regressions | No personal media in CI, ever (doc 10) |

---

## 6. Device & environment matrix

**Classes** per doc 05 (D1 low, D2 mainstream, D3 high). Coverage requirements, not device claims:

| Dimension | Coverage requirement `[T]` |
|---|---|
| OS bands | 26/27 (floor), 28–29, 30–32, 33, 34–35, 36 (target-API deadline) — at least one device per band |
| Storage | Internal; portable + adoptable SD; removal scenarios (TST-SDCARD) |
| Display | 60 Hz + ≥90 Hz; cutout/notch; HDR-declaring display; one DV-declaring device — used only for TST-HDR honest-path tests |
| OEM slant | AOSP-like + Samsung-class + Xiaomi-class minimum (gesture/decoder variance) |
| ABI/Build | arm64 primary (RQ-14 finalizes splits); debug + release (R8) variants both tested — R8 regressions are a classic escape |
| Locales | en + RTL + CJK + Devanagari + es/pt/ru (foundation set from doc 05 LOC) |
| Emulator roles | API grid for logic/instrumented only; **never the authority for playback/codec claims** (no codec claims from emulators) |
| Farm | Commercial device farm MAY supplement breadth; still not codec authority |

The concrete device list + purchase budget is locked in the build phase (doc 16 footing); `TST-CMPT` runs happen on the §6 matrix, not on borrowed devices. **Procurement is specified in `08B` §3 as one fleet that grows into this matrix:** B0 buys a subset (the 11 device-dependent spikes), and later purchases must close uncovered matrix items rather than start a second fleet (Part 32, D-23).

---

## 7. Specialized test programs

1. **AV-sync & seek harness** (implementation owned here; *methodology* owned by doc 13): timestamped impulse fixtures + frame-tagged video; measured via screen/audio capture rig; targets from NFR-PERF. Runs in TST-SYNC/TST-SEEK.
2. **Scan-at-scale:** synthetic libraries of 1k / 10k / 50k items `[T]`; asserts incremental streaming, UI budgets (TST-LIBV), kill-resume, and no phantom-dataset regressions.
3. **Process-death matrix (TST-REST):** ≥12 kill points (mid-scan, mid-thumbnail, mid-playback variant × content source, mid-migration, mid-playlist edit) each with a defined expected restore state; "resume from zero without telling the user" is a failure.
4. **Interruption matrix:** calls, alarms, assistant, PiP gestures, split-screen, screen off/on, router/BT changes — cross of TST-BG/AUD/REST.
5. **Fault injection:** storage revocation mid-operation, read-only media, low space (thumbnail pause banner), clock skew, battery saver, thermal soak (protocol via doc 13).
6. **Accessibility:** scripted TalkBack journeys per screen class; token contrast audit; 200% font sweep; reduce-motion; keyboard/remote path for all core controls (TST-A11Y).
7. **Copy & claims audit (TST-VIS):** string sweep vs doc 07 §13 and doc 06 §7; store listing & screenshots; debug strings/leaks check in R8 builds; license notices presence (doc 11).

---

## 8. CI/CD pipeline & automation policy

| Stage | Contents | Trigger |
|---|---|---|
| Per change | Static (lint, detekt-class) + **license deny-list gate (doc 11 §4)** + unit + Robolectric | Every push/PR |
| Nightly | Instrumented suites on device lab; fuzzing (time-boxed); dependency CVE + license drift scan; Media3/Compose channel watch | Nightly |
| Weekly | Macrobenchmarks + soak (4 h playback × 2 devices `[T]`); flake review | Weekly |
| RC | Full suite + manual charters + macro + upgrade matrix + Play pre-launch report (augments, never replaces, device passes) | Release candidate |

Policy: max 2 CI retries; auto-quarantine after 2 consecutive failures (issue required); **no silent skips — a skipped test is a defect**; device lab pinned to release firmware with change control.

---

## 9. Acceptance criteria (release gates)

*(These are **release gates for the app** (Gate A/B/C for the release pipeline). They are distinct from the master prompt §23 spec-quality gates of the same letters, which review the specification package itself and are exercised in doc 18 — noted here to prevent conflation.)*

**Gate A — feature complete**
- All in-scope suites green; **zero open S1/S2**; S3 open only with owner-signed waivers.
- Adversarial corpora: **zero** crashes/ANRs/hangs; every rejection produces a mapped error state (TST-ERR).
- Privacy proofs pass: no-INTERNET assertion + runtime egress monitor + deletion correctness (TST-PRIV-NET-01…04).
- Read-only guarantee verified (file hashes unchanged across full user-journey runs).
- Upgrade matrix passes: fresh → current; each released version → current (migrations TST-SET).

**Gate B — release candidate**
- NFR budgets (doc 05) measured per doc 13 protocol: met, or waived once with owner sign-off recorded in doc 18 — **no renegotiation after Gate B**.
- Accessibility audit complete (TST-A11Y full pass).
- Claims/visibility audit pass (TST-VIS); every textual capability claim maps to a validated doc 07 tier.
- Store artifacts cross-checked: SBOM + notices (doc 11), data-safety form vs doc 10 inventory, screenshots vs actual UI.

**Gate C — staged rollout**
- On ≥5% cohort `[T]`: Play vitals within NFR-REL targets; halt criteria: crash or ANR rate >2× ceiling `[T]`, or any credible data-loss report — halt, triage, resume only after a fix or a documented low-impact finding.
- Rollback/forward plan documented (doc 15).

---

## 10. Defect management

| Sev | Definition | Examples |
|---|---|---|
| S1 | Data loss / privacy breach / false claim that misleads materially | History silently dropped; file write without user intent; "supported" copy for unvalidated tier |
| S2 | Core journey broken, crash/ANR reachable, silent wrong playback behavior | Play fails on A-tier content; resume from zero silently; fallback without notice |
| S3 | Degraded UX with correct behavior | Wrong thumbnail placeholder; misaligned overlay in rare geometry |
| S4 | Cosmetic | Copy typo; icon weight inconsistency |

Regression law: **every fixed bug yields a permanent fixture + case** (TST-FUZZ/TST-CORR house the adversarial ones). S1/S2 regressions block release unconditionally. Triage is lightweight (twice-weekly cadence during build phase); severity is re-derived, never negotiated ("it's S3 because deadline").

---

## 11. Quality metrics `[T]` (tracked, not vanity)

| Metric | Target |
|---|---|
| Critical-path branch coverage (index core, session machine, subtitle pipeline, error mapping) | ≥70% |
| Flake rate | <1% of runs; quarantine turnaround ≤1 week |
| S1/S2 escapes from Gate B → wide rollout | 0 (aspirational; any escape triggers RCA) |
| MTTR for S1/S2 during rollout watch | ≤72 h |
| Suite runtime budgets (§3) | met on CI reference machine |

---

## 12. Roles (small-team reality)

No dedicated QA org is assumed (doc 16 sizing). Components: one **quality lead role** (an engineer with accountabilities, not a new headcount), everyone-owns-testing norm, device-lab custody, weekly 30-min quality review (flakes, escapes, coverage of new code), optional device-farm supplemental pass. Support signal routing (doc 14) feeds recurring defects back into TST suites.

---

## 13. Open questions

| # | Question | Owner |
|---|---|---|
| Q12-1 | Fixture license archive format (attribution manifest storage in repo vs release assets) | 12/11 |
| Q12-2 | Device purchase budget + which DV/HDR test device (doc 16 footing) | 16 |
| Q12-3 | Fuzzing depth vs schedule trade (time-box both parsers or prioritize subtitle only) | 12 |
| Q12-4 | Whether Play pre-launch report coverage can absorb any manual device pass (working assumption: no; it supplements) | 12/15 |
| Q12-5 | AV-sync rig precision target vs achievable camera-based capture | 13/12 |
| Q12-6 | Are ≥90 Hz devices required for trustworthy jank reads (or do 60 Hz + macrobenchmark suffice) | 13 |
