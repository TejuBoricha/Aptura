# 16 — Roadmap, Dependencies, Roles, and Estimates

**Version:** 0.8 (draft; v0.8 **adopts the Part 38 working scenario** — §2.1's 2-engineer case as the planning baseline (no hard date; modest ceiling) — answering **Q16-6** (B0.0 additive), **Q16-7** (concurrency 2) and **Q16-8** (rung definitions confirmed; the ladder is exercisable); v0.7 **sizes the de-scope ladder (§5.1)** — rung-by-rung scope, `[E]` savings, decision points and cumulative value, plus the finding that items 1/2/5 were slogans rather than scopes and that items 1+5 are one decision (new **Q16-8**); §2.1(C) updated from "sizing available on request" to the sized result; v0.6 adds **§2.1 OD-4 decision support** — headcount scenarios derived from §2's own ranges, the externally-clocked floor no headcount compresses, and what a hard date would change; v0.5 records that the device-dependency count is **derived** (11 of 16 — 08A §3) rather than asserted, and that the B0 lab is a **subset** of the doc 12 §6 matrix fleet, procured as one fleet that grows (Part 32, D-23/D-26); v0.4 points DEP-005 at the procurement specification and the owner kickoff pack in `08B`; v0.3 adds **B0.0 — instrumentation** as the named prerequisite stage of the spike program and corrects DEP-005's path classification (it gates most of B0, so it is on the *B0* critical path even though it is off the *launch* path) — both surfaced by `08A_SPIKE_RUNBOOKS.md` §2/§5; v0.2 restates de-scope item 3 by **window width** — "the 840dp+ two-pane layout" — instead of by device, consistent with doc 06 §3's breakpoint model, and adds **RQ-15** (adaptive layout) and **RQ-16** (notification exemption + gesture exclusion) to the B0 phase within its existing range. v0.1: roadmap, release trains, phases, DEP register, roles, de-scope ladder)
**Date:** 2026-10-20
**Status:** Draft. **Planning ranges only — no calendar dates, no commitments.** Timeline and budget are unknown (ASM-006, OD-4 pending), so this document is phase-relative by design. Every size is an `[E]` estimate in dev-weeks with stated assumptions; calibration happens after the spike program (doc 08 definitions + `08A_SPIKE_RUNBOOKS.md` procedures, RQ-01…16) replaces hypothesis with evidence.
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
| **B0 — Spikes** | RQ-01 (engine conformance), RQ-03 (HEVC/AV1 10-bit reality), RQ-05 (scan at scale), RQ-06 (ASS feasibility), RQ-07 (process-death), RQ-08 (billing offline), RQ-13 (Compose on D1), RQ-14 (ABI/splits), **RQ-15 (adaptive layout across breakpoint changes — added Part 28; absorbed within this range because it needs instrumentation, not additional device classes)**, **RQ-16 (media-session notification exemption + gesture exclusion on retail builds — added Part 29; absorbed, as it runs on the lab devices already in scope)**; legal L1/L8 opened with counsel; long-lead store items S1/S2/S8 started | Doc 08 spike definitions + 08A runbooks; **B0.0 instrumentation first** (fixture corpus + adversarial/scale corpora + seek and state-restoration harnesses — a prerequisite the spike list did not contain; 08A §2); device lab minimum (≥1 device/class D1–D3) **ordered on day one** (see B0 sizing flag) | 4–8 (⚠ flag — **resolved Part 38**: B0.0 additive; 2 engineers) |
| **B1 — Foundations** | Repo/CI (incl. license deny-list gate), signing setup, module skeleton per doc 09, local crash plumbing, design tokens from doc 06, store accounts & verification | B0 ADR locks | 3–5 |
| **B2 — Library & scan** | FR-LIB catalogue: MediaStore/SAF ingestion, scan pipeline, identity/reconciliation, thumbnails, browser UI (SCR-002…009, 011, 012) | B1; doc 09 data model | 6–10 |
| **B3 — Player core** | FR-PLAY/FR-CTRL: Media3 integration, decode election + honest fallback, gestures, PiP, session machine, audio track election | B0 engine lock (ADR-001); licensing clearance for any bundled decode | 8–12 |
| **B4 — Subtitles & audio depth** | FR-SUB (matching, encodings, styling, tier per RQ-06 outcome), FR-AUD depth (delay, routing) | RQ-06; L1/L2 for any bundled decode | 4–7 |
| **B5 — Settings, diagnostics, polish** | FR-SET/FR-DIAG, SCR-013…017, design-system completion, accessibility pass (NFR-A11Y), copy audit prep | B2–B4 states exist | 3–5 |
| **B6 — Hardening & beta** | Full Gate A program (doc 12), device matrix runs, benchmark protocol (doc 13) from `[T]` → measured, closed testing (S8), store assets (S3–S6) | B5 feature freeze | 4–6 |
| **B7 — Launch ops** | Staged rollout (doc 15 §5), vitals watch, hotfix readiness, support live | Gate B passed; Gate C thresholds armed | 2–3 |

**R1 total `[E]`: ≈34–56 dev-weeks** (not elapsed time — divide by effective parallel engineer count; design/legal/support are parallel tracks, not counted). Calibration rules: re-estimate at every phase exit; B0 outcomes are the biggest uncertainty killer; ranges never silently become promises (doc 17 DoD).

**B0 sizing flag (surfaced by 08A §5 — must be resolved before B0 is scheduled as a plan; both items answered below — Part 38).** This range was set before the spikes existed as procedures. Two corrections follow from writing them out:

1. **B0 has an unlisted stage zero.** Nearly every spike's method depends on the fixture corpus (doc 12 §5) and the measurement harnesses (doc 12 §7). Neither exists yet and neither is a spike output, so **B0.0 — instrumentation** precedes all spike runs (08A §2). Whether B0.0 sits *inside* the 4–8 window or is additive to it is not stated anywhere; until answered, the range is ambiguous by the size of the corpus-plus-harness work `[E]` (Q16-6). **Answered (Part 38, delegated): B0.0 is *additive* — the 4–8 range covers spike execution; B0.0's own size is fixed at build start against `12A` §4's done criteria.**
2. **The range assumes concurrency.** Summed sequentially, 08A §4's per-spike timeboxes total ≈**18 dev-weeks `[E]`**, before B0.0. The 4–8 range is therefore only achievable with several engineers running spikes in parallel (and with B0.0 amortised). **With a single engineer on B0 the range is not achievable and should be re-based rather than quietly missed** (Q16-7, owner input OD-4). The timeboxes themselves are `[E]` and uncalibrated until the first three spikes close, at which point the remainder should be re-derived from actuals. **Answered (Part 38, delegated): two engineers run B0 (Q16-7); the re-derivation rule stands.**

**Parallel tracks (not in phase sizes):** legal L1–L8 (doc 11), design translation of doc 06, device-lab procurement (long lead — start in B0), community/support standing setup, privacy-policy hosting.

### 2.1 OD-4 decision support — what each answer changes (Part 33)

Doc 16's sizes are in dev-weeks *because* headcount is unknown (ASM-005, OD-4). This section pre-computes what the owner's answer does, so OD-4 can be answered in one line without a follow-up round. **Everything below is derived from §2's own ranges; it is `[E]`, and the point is the shape, not the digits.**

**Adopted scenario (Part 38, owner-delegated).** The **2-engineer row below is the working planning case** — ≈5–9 months `[E]` from B0 start to launch, with overlap chosen deliberately where the phase chain allows (B1‖B0, B5‖B4). **No hard external date** is set, so the de-scope ladder is **exercisable but not exercised**, and its rung definitions are confirmed (Q16-8). Spend posture: **modest** — the §3.1-minimum device lab (`08B` §3), grown by the buy-one→probe→extend rule, plus batch-A counsel. The scenario exists to make the plan schedulable; **it stands in for facts only the owner holds (actual headcount, budget) and is replaced wholesale when those are known** (17 §7).

**A. A dev-week total is not a calendar.**

| Scenario | Assumption | Indicative calendar `[E]` | What it implies |
|---|---|---|---|
| **1 engineer** | full-time; design not counted | **≈8–13 months** | 34–56 dev-weeks at 1×, plus B0.0. **B0 alone (≈18 dev-weeks sequential, 08A §5) cannot be compressed** — with one engineer the B0 range in §2 is not reachable and should be re-based (Q16-7) |
| **2 engineers** | the ASM-005 floor | **≈5–9 months** | The phase chain is largely sequential (B2→B3 needs the data model; B4 needs B3; B6 needs a feature freeze), so two engineers give **≈1.5×, not 2×** — the gain comes from choosing overlap deliberately (B1‖B0, B5‖B4), not from headcount |
| **3–4 engineers** | the ASM-005 ceiling | **≈4–7 months** | ≈1.8–2.2×; review/coordination load grows, and the tail (B6/B7) is clock-bound — this is where the returns flatten |
| **>4 engineers** | — | **no material gain** | 34–56 dev-weeks does not decompose further without inventing parallel work; past this point, more people mostly add coordination cost |

**B. Part of every calendar is externally clocked, and no headcount compresses it.**

- **Closed testing: 12 testers / 14 days** (doc 15 §4 S8, assumed required) — a hard ~2-week block before production.
- **Developer verification + regional rollout** (doc 15 §4 S2) — gates *distribution*, not quality, but it sits in the calendar.
- **Device-lab lead time** (DEP-005) — external. If it lands late, group C/D spikes slide **regardless of how many engineers exist**.
- **Counsel (L1/L2)** — *not* on the critical path (doc 11 §3.5 fallbacks exist), so it does not lengthen the plan; it only decides whether the audio extension ships in R1 or R1.x.

**Consequence:** the last ~1–2 months of any scenario is externally clocked. **A hard date shorter than that is not a staffing problem, and hiring cannot fix it.**

**C. If there is a hard date.** In order of preference:
1. **Staff to the scenario** — cheapest, provided the window is at least the 3–4-engineer case.
2. **Exercise the de-scope ladder (§5)** in its documented order — **now sized in §5.1**: cumulative recovery ≈**5.5–14.5 dev-weeks** (typically 6–12), i.e. **≈1–2 calendar months** at 2 engineers; rungs 1+5 are one decision, and rung 4 is conditional on RQ-06 (it may be worth zero). **If the window is still short after the ladder, the ladder is not the instrument** — the documented fallback is item 3 below; **rung definitions are confirmed (Part 38)** and the ladder is exercisable.
3. **Move the date** — the honest default when neither closes the gap.

**What never happens:** cutting §5's *never cut* list (playback honesty paths, scan reliability, accessibility floor, privacy proofs, error taxonomy) to protect a date. If the date cannot be met with them, the date is wrong — not the requirements.

**D. If the answer is "no date yet".** Nothing changes; that is a legitimate answer, the plan stays in dev-weeks, and no commitment is implied. One thing does not wait for it: **the device order and the store clock run anyway** (DEP-005, S2/S8) — ordering the lab does not depend on the calendar decision, which is why §2.1 ends here rather than gating procurement on it.

---

## 3. Dependency register (initial DEP entries; canonical in doc 17)

| ID | Dependency | Blocks | Owner | Mitigation |
|---|---|---|---|---|
| DEP-001 | RQ-01/02/03 spike outcomes → engine ADR-001 lock | B3 start | eng | Spikes first by design; libVLC fallback path pre-scoped (doc 08) |
| DEP-002 | Legal L1: LGPL §6 relinking verdict | Bundled FFmpeg audio ext inclusion | owner + counsel | Fallback = ship without ext (doc 11 §3.5); no launch dependency |
| DEP-003 | Legal L2: AC-3/E-AC-3 confirmations | FR-AUD-007 add-back (R1.x) | counsel | Default posture already excludes bundled decode |
| DEP-004 | OD-4 (team/budget/time) | Calendar conversion of all sizes; device-lab scope | owner | Ranges stand alone meanwhile |
| DEP-005 | Device lab procurement (≥1/class **D1–D3** + SD-bearing + HDR-declaring + ≥90 Hz + OEM-skinned devices; scope in 08A §5, **capability spec + selection criteria + intake protocol in `08B` §3**) | **The device-dependent spikes — a derived 11 of 16** (08A §3 shows the count: only RQ-02/09/10/14 need no device, and RQ-08 needs no fleet capability) **plus** B6 benchmarks and TST-CMPT runs | owner | **Order on day one of B0.** It is on the ***B0* critical path** (spike elapsed time) even though it is off the *launch* path (§7); farm as supplement not substitute; interim borrow strategy = Q16-5 |
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
4. **De-scope ladder (only with owner):** if R1 must shrink, documented cut order: (1) advanced playlist depth, (2) non-core settings surface breadth, (3) the 840dp+ two-pane layout (compact single-pane remains fully functional), (4) ASS/libass tier → styled-subset, (5) playlist persistence. **Never cut:** core playback honesty paths, scan reliability, accessibility floor, privacy proofs, error taxonomy. (Cuts are recorded as scope changes per doc 17 change rules.) **The rungs are sized in §5.1** — which found that items 1, 2 and 5 were slogans rather than scopes, and proposes the definitions the owner must confirm before either can be exercised (Q16-8).
5. **Scope creep firewall:** the de-scope ladder runs downward only; new R1 wishes go to the R1.x candidate table (§6), not into the train.

### 5.1 The sized ladder (Part 36) — what each rung really removes, and what it is worth

**Why this exists.** §2.1(C) says a hard window shorter than ~4 months requires the ladder to be sized first; only rungs 3 and 4 had any adjacent sizing anywhere in the package. This section derives each rung's saving from §2's own phase ranges and the one existing adjacent figure (§6 row 4, libass ≈2–4). It is `[E]` — the shape matters, not the digits — and its first product is a **finding**: rungs 1, 2 and 5 were written as slogans, not scopes (rung 2's most obvious candidate, settings import/export, is **R2-only**, so it cannot be what "settings breadth" means). The definitions below are **confirmed (Part 38 — Q16-8)**: the ladder is exercisable, though not exercised (no hard date).

**Rung definitions and sizes** (scope = what stops being built; every cut keeps the never-cut list of §5 item 4 intact):

| Rung | Scope removed (working definition — to confirm) | Saving `[E]` | Latest useful decision point | Felt by | Re-add path |
|---|---|---|---|---|---|
| **1. Advanced playlist depth** | Named-playlist management (FR-LIB-017: create/add/remove/reorder UI, missing-item reconciliation at play time) **plus the queue depth beyond the MUSTs**: queue reordering/removal, shuffle, save-queue-as-playlist. **Kept:** FR-CTRL-017's modes (Single/Folder/Queue/Repeat), the auto-advance countdown and its guard — those are MUSTs | **2.5–5** *(together with rung 5 — see below)* | **Before B2's browser UI** (playlist screens are B2 work) | Collectors/power users who curate; binge-journey users | New §6 row needed (the candidate stack currently assumes playlists ship in R1) |
| **2. Non-core settings breadth** | The P2 configuration tail: FR-CTRL-016 (full gesture remapping, step presets, overlay-timeout customization beyond defaults), FR-SET-005 (what's-new/migration messaging → silent migration + one help entry), settings search and IA niceties; FR-LIB-022's extras (DB-rebuild UI) fold into mandated reset/clear actions. **Kept:** every FR-mandated toggle, privacy controls, reset, permission recovery, help, About/licenses | **1.5–2.5** | **Before B5 entry** (settings build) | Power users who customize; almost no one else | New §6 row |
| **3. 840dp+ two-pane layout** | Expanded-width list-detail two-pane (doc 06 §3) and Android's pane state-preservation rules; **compact/medium single-pane + rail navigation remain fully functional**. Also shrinks RQ-15's instrumented scope to single-pane transitions | **1.5–3** | **Before B2's browser UI** (late cuts pay for built panes) | Tablet / landscape / foldable users | New §6 row |
| **4. ASS/libass tier → styled-subset** | **Conditional on RQ-06** (DEP-009): if the spike says Media3-native rendering is sufficient, this rung has **nothing to remove (saving ≈0)** — record and skip. If libass-in-pipeline was chosen, cutting it saves the integration and ships the native styled-subset with the one-time honest ceiling notice (doc 07 §8) | **0 — or 2–4** | **At RQ-06 close (B0), before B4** | Subtitle-fidelity power users | §6 row 4 already exists |
| **5. Playlist persistence** | The durability half: playlist/queue state surviving relaunch, rescan reconciliation (FR-LIB-017 × FR-LIB-018 interplay), data-model tables | *included in rung 1's figure* | **Before B2's persistence work** | Same as rung 1 | With rung 1 |

**Findings the sizing produced (beyond the numbers):**

1. **Rungs 1 and 5 are one decision, not two.** If playlists are cut, there is nothing for persistence to persist; if playlists stay, persistence is a dial, not a rung. The size above treats them as a block (2.5–5). Exercising "5 without 1" is incoherent and should not appear as an option.
2. **Rung 4 is conditional, and may evaporate.** Its saving is zero when RQ-06 confirms native rendering — a legitimate spike outcome that *removes* a rung rather than exercising it.
3. **Decision points are phase-entry, not launch-time.** Taking a cut after its phase has shipped recovers nothing; the ladder is a planning device, so each rung's row names the latest useful moment.
4. **Every exercise carries fixed change-record work:** re-marking FRs to R1.x, retiring test suites (TST-PLST-, TST-SET/CTRL subsets), trimming doc 06's IA (the Playlists destination), doc 09's data model, doc 12's case lists and doc 18's traceability rows ≈ **0.3–0.5 dev-week per block**. Take rungs in blocks, not dribbles.

**Cumulative value.** Rungs 1+5, 2 and 3: **≈5.5–10.5 dev-weeks**; plus rung 4's 0 or 2–4 → **≈5.5–14.5 total, call it 6–12 in the typical case.** At the 2-engineer effective rate (§2.1A, ≈1.5×) that is **≈1–2 calendar months** off any scenario's calendar — less at 3–4 engineers, because the savings are work, not clock.

**What that means for a hard date.** The ladder recovers roughly one to two months; it does not convert a 3-month window into a 5-month project. **If the gap after full exercise is larger than that, the documented next step is moving the date (§2.1C item 3)** — not cutting into the never-cut list, which would trade the product's trust identity for time. Cuts are recorded as scope changes per doc 17 change rules; the firewall (§5 item 5) still applies: the ladder runs downward only.

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

Off the launch path but still long-lead and time-sensitive: store verification (S2), closed-testing clock (S8), privacy/support surfaces (DEP-006). **DEP-005 (device lab) is a special case and must not be read as parallel-to-B0:** it is off the *launch* path, but it is on the ***B0* critical path** — the device-dependent spikes (a derived 11 of 16; 08A §3) cannot run without it (08A §3/§5) — so it is ordered on day one of B0, not at B6. Legal L1/L2 is *not* on the R1 critical path — the design deliberately survives its failure (doc 11 §3.5).

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
| Q16-1 | OD-4 answer (team/budget/time) — the single unlock for calendar conversion. **§2.1 pre-computes what each answer changes** | Owner |
| Q16-2 | Designer sourcing (DEP-011) and whether store assets need an external agency | Owner |
| Q16-3 | Support staffing model at launch (Q14-5) | Owner |
| Q16-4 | Whether the open-testing stage (doc 15 §5) is worth the extra clock vs. closed→staged | 15/16 |
| Q16-5 | Device-lab interim strategy before DEP-005 lands (borrow vs. farm-first) | 16/12 |
| Q16-6 | Is **B0.0** (fixture corpus + measurement harnesses) inside the 4–8 dev-week B0 range, or additive to it? — **answered (Part 38, delegated): additive**; B0.0's size is fixed at build start (08A RB-1) | 16/12 |
| Q16-7 | How many engineers run B0 concurrently? — **answered (Part 38, delegated): 2** (the adopted working scenario; re-derive at the first three spike closes — 08A RB-2) | Owner |
| Q16-8 | **Confirm the rung definitions used to size the de-scope ladder (§5.1)** — **confirmed as proposed (Part 38, delegated)**: 1+5 are one decision; rung 4 stays conditional on RQ-06 | Owner |
