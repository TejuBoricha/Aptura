# 16 — Roadmap, Dependencies, Roles, and Estimates

**Version:** 0.2 (draft; v0.2 restates de-scope item 3 by **window width** — "the 840dp+ two-pane layout" — instead of by device, consistent with doc 06 §3's breakpoint model, and adds **RQ-15** (adaptive layout) to the B0 phase within its existing range. v0.1: roadmap, release trains, phases, DEP register, roles, de-scope ladder)
**Date:** 2026-10-19
**Status:** Draft. **Planning ranges only — no calendar dates, no commitments.** Timeline and budget are unknown (ASM-006, OD-4 pending), so this document is phase-relative by design. Every size is an `[E]` estimate in dev-weeks with stated assumptions; calibration happens after the spike program (doc 08, RQ-01…15) replaces hypothesis with evidence.
**Consumes:** 01 (scope fences), 08 (spikes/ADRs), 11 (legal gates), 12/13 (hardening/bench programs), 15 (launch ops).
**Feeds:** 17 (dependency register consolidation), 18 (readiness).

---

## 1. Release trains

| Train | Goal | Scope fence | Entry condition | Exit criterion |
|---|---|---|---|---|
| **R1** | Trustworthy local library + player on Android phones/tablets | Docs 04 FR catalogue (L/P/C/S/A/PLAT/SET/DIAG domains); no network features | This spec package accepted; OD-1/OD-2 answered | Doc 12 Gate C passed at 100% rollout |
| **R1.x** | Stabilize + ship gated increments | Anything with a resolved gate: audio add-backs (L2), libass, iOS decision, monetization B (OD-3), F-Droid decision | R1 stable for ≥1 release cycle; gates resolved | Each increment independently gated (doc 12 scope) |
| **R2** | Network candidate + platform expansion | SMB/NAS/HTTP (DEC-001 candidate), Android TV exploration | DEC-001 revisited with R1 usage/failure evidence; security review for network surfaces | Separate spec addendum (this document set explicitly defers it) |
| **R3+** | Advanced experiments | AI-adjacent features (DEC-007 constraints: on-device-first, opt-in, never silent upload) | R2 learnings; explicit owner decision | — |

Nothing from R2/R3 is designed here; their existence is registered so scope discipline stays visible.

---

## 2. Build-phase plan (relative; sizes `[E]` dev-weeks)

Assumptions: 2–4 engineers (ASM-005), part-time design, spec package as input, no legacy code. Sizes are ranges (opt–likely–pess) reflecting the cone of uncertainty at spec time.

| Phase | Contents | Key inputs | Size `[E]` |
|---|---|---|---|
| **B0 — Spikes** | RQ-01 (engine conformance), RQ-03 (HEVC/AV1 10-bit reality), RQ-05 (scan at scale), RQ-06 (ASS feasibility), RQ-07 (process-death), RQ-08 (billing offline), RQ-13 (Compose on D1), RQ-14 (ABI/splits), **RQ-15 (adaptive layout across breakpoint changes — added Part 28; absorbed within this range because it needs instrumentation, not additional device classes)**; legal L1/L8 opened with counsel; long-lead store items S1/S2/S8 started | Doc 08 spike definitions; device lab minimum (≥1 device/class D1–D3) | 4–8 |
| **B1 — Foundations** | Repo/CI (incl. license deny-list gate), signing setup, module skeleton per doc 09, local crash plumbing, design tokens from doc 06, store accounts & verification | B0 ADR locks | 3–5 |
| **B2 — Library & scan** | FR-LIB catalogue: MediaStore/SAF ingestion, scan pipeline, identity/reconciliation, thumbnails, browser UI (SCR-002…009, 011, 012) | B1; doc 09 data model | 6–10 |
| **B3 — Player core** | FR-PLAY/FR-CTRL: Media3 integration, decode election + honest fallback, gestures, PiP, session machine, audio track election | B0 engine lock (ADR-001); licensing clearance for any bundled decode | 8–12 |
| **B4 — Subtitles & audio depth** | FR-SUB (matching, encodings, styling, tier per RQ-06 outcome), FR-AUD depth (delay, routing) | RQ-06; L1/L2 for any bundled decode | 4–7 |
| **B5 — Settings, diagnostics, polish** | FR-SET/FR-DIAG, SCR-013…017, design-system completion, accessibility pass (NFR-A11Y), copy audit prep | B2–B4 states exist | 3–5 |
| **B6 — Hardening & beta** | Full Gate A program (doc 12), device matrix runs, benchmark protocol (doc 13) from `[T]` → measured, closed testing (S8), store assets (S3–S6) | B5 feature freeze | 4–6 |
| **B7 — Launch ops** | Staged rollout (doc 15 §5), vitals watch, hotfix readiness, support live | Gate B passed; Gate C thresholds armed | 2–3 |

**R1 total `[E]`: ≈34–56 dev-weeks** (not elapsed time — divide by effective parallel engineer count; design/legal/support are parallel tracks, not counted). Calibration rules: re-estimate at every phase exit; B0 outcomes are the biggest uncertainty killer; ranges never silently become promises (doc 17 DoD).

**Parallel tracks (not in phase sizes):** legal L1–L8 (doc 11), design translation of doc 06, device-lab procurement (long lead — start in B0), community/support standing setup, privacy-policy hosting.

---

## 3. Dependency register (initial DEP entries; canonical in doc 17)

| ID | Dependency | Blocks | Owner | Mitigation |
|---|---|---|---|---|
| DEP-001 | RQ-01/02/03 spike outcomes → engine ADR-001 lock | B3 start | eng | Spikes first by design; libVLC fallback path pre-scoped (doc 08) |
| DEP-002 | Legal L1: LGPL §6 relinking verdict | Bundled FFmpeg audio ext inclusion | owner + counsel | Fallback = ship without ext (doc 11 §3.5); no launch dependency |
| DEP-003 | Legal L2: AC-3/E-AC-3 confirmations | FR-AUD-007 add-back (R1.x) | counsel | Default posture already excludes bundled decode |
| DEP-004 | OD-4 (team/budget/time) | Calendar conversion of all sizes; device-lab scope | owner | Ranges stand alone meanwhile |
| DEP-005 | Device lab procurement (≥1/class + SD + HDR device) | B6 benchmarks; TST-CMPT runs | owner | Order in B0; farm as supplement not substitute |
| DEP-006 | Privacy-policy hosting + support channel (Q14-1/2) | Store listing S4/S6 | owner | Static page, no-analytics provider (OQ15-3) |
| DEP-007 | Developer verification (S2) completion | Distribution availability | owner | Start early; regional rollout documented (W4) |
| DEP-008 | Closed-testing recruitment (12 testers/14 days if required) | Production launch | owner | Beta cohort doubles as signal source (doc 14 §7) |
| DEP-009 | RQ-06 ASS verdict | Subtitle tier commitment (libass vs. styled-subset) | eng | Tier system degrades gracefully (doc 07) |
| DEP-010 | RQ-08 billing-offline verdict | R1.x monetization option B | eng | Option A (free) is the standing default |
| DEP-011 | Part-time designer engagement | B1 tokens / B5 polish / store assets | owner | Doc 06 text spec is buildable without for early phases |
| DEP-012 | Patent/court developments (AV1 pool, HEVC terms) — watch item | Nothing directly; informs R2 | owner | Quarterly recheck (doc 11 §2); no R1 exposure change expected |

---

## 4. Roles (small-team model; no dedicated QA org assumed)

| Role | Coverage | Notes |
|---|---|---|
| Owner/product | OD-1…6, store identity, brand, budget, legal engagement | The only decision bottleneck by design |
| Eng lead + engineers (2–4) | B1–B7; quality-lead accountability held by one engineer (doc 12 §12) | Everyone tests; no hand-off culture |
| Design (part-time) | Doc 06 → visuals, store assets, review of motion/contrast | Can start at B1 on tokens |
| Legal (external, ad hoc) | L1–L8, store policy questions, trademark/name (OD-6) | Scheduled touchpoints at B0 (L1 opened early) and pre-launch |
| Support (fractional at launch) | Doc 14 channels; 72h target during rollout watch `[T]` | Doubles as the fix-forward feedback loop |

---

## 5. Estimation method and honesty rules

1. **Relative sizing** (dev-weeks, ranges) because calendar depends on OD-4; converting to dates requires owner input, then re-basing.
2. **Cone discipline:** re-estimate at every phase exit; B0 is expected to *narrow* ranges, not widen scope.
3. **No silent promises:** ranges quoted anywhere external must carry "estimate" framing; doc 15 changelog tone rules apply.
4. **De-scope ladder (only with owner):** if R1 must shrink, documented cut order: (1) advanced playlist depth, (2) non-core settings surface breadth, (3) the 840dp+ two-pane layout (compact single-pane remains fully functional), (4) ASS/libass tier → styled-subset, (5) playlist persistence. **Never cut:** core playback honesty paths, scan reliability, accessibility floor, privacy proofs, error taxonomy. (Cuts are recorded as scope changes per doc 17 change rules.)
5. **Scope creep firewall:** the de-scope ladder runs downward only; new R1 wishes go to the R1.x candidate table (§6), not into the train.

---

## 6. R1.x / R2 candidate stack (priority order, gated)

| Order | Candidate | Gate/condition | Value | Size `[E]` |
|---|---|---|---|---|
| 1 | Quality patch train | Continuous | Trust maintenance | — |
| 2 | Audio decode add-backs (AC-3/E-AC-3) | L2 resolved positively | Compatibility depth | 1–2 |
| 3 | GUIDED troubleshooting flows (doc 14 §5.4) | R1 support themes justify | Support deflection | 1–2 |
| 4 | libass integration (if not already in R1) | RQ-06 positive + budget | Power-user subtitle fidelity | 2–4 |
| 5 | iOS gate execution | RQ-09 criteria fully defined in doc 08 | New market | 6–10 + platform costs |
| 6 | Monetization option B | OD-3 + RQ-08 + KPI evidence | Sustainability | 2–4 |
| 7 | F-Droid / FOSS channel decision | DEC-009 revisit; own-license question OQ-1 | Ethics-community reach; second build burden | 2–4 |
| 8 | Android TV exploration | Input-model design + TV class testing | Reach | 6–12 |
| 9 | SMB/NAS/HTTP (R2 proper) | DEC-001 revisit + security review | Biggest scope jump; new threat surface | 10–20 |
| 10 | R3 experiments | DEC-007 constraints | Unknown | — |

---

## 7. Critical path (single line through B0→B7)

**Spikes (B0) → ADR-001 engine lock → Player core (B3) → Subtitles depth (B4) → Hardening/beta (B6) → Launch ops (B7).**

Off-path but launch-relevant long leads: store verification (S2), closed-testing clock (S8), device procurement (DEP-005), privacy/support surfaces (DEP-006). Legal L1/L2 is *not* on the R1 critical path — the design deliberately survives its failure (doc 11 §3.5).

---

## 8. Risk-adjusted notes

- **RQ-01 FAIL** → ADR-001 fallback (libVLC route) re-opens engine work: add `[E]` 6–10 dev-weeks and a licensing rework pass; the honest tier matrix (doc 07) absorbs capability differences either way.
- **L1/L2 negative** → remove items 2/libass from R1.x stack; R1 unchanged (no launch dependency).
- **OD-4 delay** → planning continues in dev-weeks; no calendar promises get made in the meantime.
- Buffers are inside the ranges (≈20–30% `[E]` contingency implicit); no separate hidden buffer exists — stated so nobody double-counts.

---

## 9. Open questions

| # | Question | Owner |
|---|---|---|
| Q16-1 | OD-4 answer (team/budget/time) — the single unlock for calendar conversion | Owner |
| Q16-2 | Designer sourcing (DEP-011) and whether store assets need an external agency | Owner |
| Q16-3 | Support staffing model at launch (Q14-5) | Owner |
| Q16-4 | Whether the open-testing stage (doc 15 §5) is worth the extra clock vs. closed→staged | 15/16 |
| Q16-5 | Device-lab interim strategy before DEP-005 lands (borrow vs. farm-first) | 16/12 |
