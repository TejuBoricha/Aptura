# HANDOFF — Progress, Pending Work, and How to Resume

**As of:** 2026-10-19 · **Package:** `spec/` v0.1 (docs 00–18 + `02A` case studies + `08A` spike runbooks + `research/`) · **Previous session:** specification engagement, Parts 1–30
**This file:** the "start here" for any new session (human or AI). It says what is done, what is not, and what to do next. It duplicates nothing critical — the authoritative ledger stays `00_INDEX_AND_STATUS.md`; the authoritative registers stay `17_…`; the authoritative verdict stays `18_… §7`.

---

## 0. TL;DR

1. **The specification package is COMPLETE.** All 19 documents (00–18) plus four sourced research files exist; phases 0–6 of the master prompt are done; the final audit issued the verdict **READY FOR TECHNICAL SPIKES** (doc 18 §7).
2. **Nothing in the package has been executed.** No app code, no tests run, no benchmark measured, no device tested — by the engagement's hard constraints. Do not ever describe the content as "verified working".
3. **The next session's job is NOT more spec writing.** It is: owner decisions (OD), the B0 technical spikes (runnable procedures in `08A`; week-one kickoff in `08B`), the legal briefs (L — sendable briefs for L1/L2 in `11A`), store prerequisites (S) — or revision work only if the owner requests changes. See §3.
4. **Before doing anything:** read `00` (ledger/IDs), then `18` (verdict + handoff checklist), then this file, then the task-relevant document. Do not redo completed docs.

---

## 1. What is DONE (do not redo)

| # | Item | Status |
|---|---|---|
| 00–18 | All 19 specification documents (vision, market, personas, FRs, NFRs, UX, compatibility, tech/ADRs, architecture/data, security/privacy, licensing, QA, benchmarking, diagnostics, ops, roadmap, registers, traceability) | ✅ Drafted — current versions in the version map below |
| research/ | W1 competitors · W2 platform · W3 licensing · W4 market/compliance · W5 case studies (02A) · W6 autoplay defaults · W7 decoder labels · W8 interface constants · W9 platform constants · **W10 permissions, gestures, captions** — sourced, dated, evidence-labeled | ✅ Complete (W1–W4 accessed 2026-10-18; W5–W10 2026-10-19) |
| 00 | Ledger, ID conventions, part log (Parts 1–30), OD table, first-response §3 | ✅ Closed for this phase |
| 18 | Traceability matrices, §22 deliverable audit (24/24), §23 Spec Gates A/B/C review (findings C-1…C-10) + Gate C post-audit delta (D-1…D-9), §21 failure-mode review | ✅ Verdict issued |
| 17 | Canonical registers: DEC-001…013, ASM-001…013, RSK-001…015 (+RSK-08-01…04 in doc 08), DEP-001…012, DoR/DoD, change process | ✅ |
| Constraints honored | No code, no scaffolding, no deps, no prototype, no execution claims | ✅ |
| 08A | **Spike runbooks** (companion to doc 08): operating rules S1–S10, the **B0.0 instrumentation prerequisite**, scheduling map by binding prerequisite, a runbook card for every spike RQ-01…16, lab/procurement scope, result-artifact template, RB-1…RB-6 | ✅ Written (docs-only) — **nothing executed** |
| 08B | **B0 kickoff pack** (action layer): OD-1…OD-6 decision briefs, DEP-005 procurement specification (capability rows, selection criteria, farm policy, intake protocol), week-one sequence, fill-in answer sheet | ✅ Written — **no device ordered, no counsel engaged, nothing decided** |
| 11A | **Counsel briefs L1/L2** in sendable form: facts as shipped, our readings for correction, numbered questions with what each answer changes, fallbacks, assets to hand over | ✅ Written — **not legal advice; not yet reviewed by a lawyer** |

**Version map:** doc **04 = v0.8**; doc **17 = v0.6**; docs **06, 08 = v0.5**; docs **05, 16, 18 = v0.4**; doc **02A = v0.3**; docs **02, 07, 09, 11, 12 = v0.2**; everything else = v0.1 (the new companions **08A, 08B, 11A = v0.1**).

---

## 2. What is NOT done (the pick-up inventory)

### 2A. Owner decisions — waiting on the owner (defaults active meanwhile; 00 §5 · **briefs + fill-in answer sheet: `08B` §2/§5**)

| ID | Question | Default in force if unanswered |
|---|---|---|
| OD-1 | Android-first vs. both platforms | Android-first (DEC-002) |
| OD-2 | R1 = local-only vs. include network playback | Local-only (DEC-001) |
| OD-3 | Monetization intent at launch | Free/no-ads (DEC-004) |
| OD-4 | **Team, budget, time** (blocks calendar conversion) | Ranges `[E]` only (doc 16) |
| OD-5 | Launch regions & languages | ASM-009 provisional set |
| OD-6 | Product/brand name (blocks trademark work L6) | Working title TBD |

### 2B. Legal items ⚖️ — counsel required (canonical list: doc 11 §5; mirror doc 10 §6.3)

L1 LGPL §6 relinking (highest priority; blocks only the optional bundled audio extension — fallback exists) · L2 AC-3/E-AC-3 confirmations · L3 DTS/TrueHD wording · L4 Dolby brand usage in UI copy · L5 statute applicability (GDPR/CCPA/DPDP/LGPD/COPPA/EAA) · L6 Name/trademark clearance · L7 Own-app license/distribution model (F-Droid implications) · L8 Store-copy wording audit (per release).
**Sendable briefs for L1 and L2: `11A_LEGAL_BRIEFS.md`** (facts as shipped, readings stated for correction, numbered questions with what each answer changes). L3–L8 are not yet briefed.

### 2C. Technical spikes (B0) — the designed next step (definitions + thresholds: doc 08)

RQ-01 Media3(+FFmpeg audio ext) vs. incumbent-engine conformance · RQ-02 royalty-safe FFmpeg build config · RQ-03 HEVC-10/AV1 hardware-decode coverage on target classes · RQ-04 Dolby handling feasibility · RQ-05 SAF/MediaStore performance at 10k+ libraries · RQ-06 ASS/libass fidelity inside Media3 · RQ-07 process-death/state restoration · RQ-08 Play Billing offline without backend · RQ-09 iOS viability gate · RQ-10 privacy-preserving diagnostics viability · RQ-11 DB + thumbnail architecture at scale · RQ-12 VFR/HDR/refresh-rate behaviors · RQ-13 Compose performance on D1 class · RQ-14 ABI/split strategy · **RQ-15 adaptive layout across breakpoint changes** (rotation/foldable/split-screen; added Part 28) · **RQ-16 platform integration assumptions on retail builds** (media-session notification exemption + gesture exclusion rects; added Part 29).
**Note:** spikes execute in a *build* environment; this was a docs-only engagement. **Runbooks are now written: `08A_SPIKE_RUNBOOKS.md`** — operating rules, per-spike procedures, result template. Writing them out also surfaced two things doc 16 did not state: **B0 has an unlisted prerequisite stage — B0.0, instrumentation (fixture corpus + harnesses)** — and **DEP-005 sits on the B0 critical path** (11 of 16 spikes need the lab), so it is ordered on day one — doc 16 v0.3 §2/§7, questions Q16-6/Q16-7.

### 2D. Build-phase artifacts deliberately deferred (registered, not omissions — doc 18 §2)

Concrete device list + purchases (DEP-005 — now scoped in **08A §5**) · case-level TST matrix (doc 18 §1.1) · formal sequence diagrams (GAP C-4) · build-reproducibility runbook (GAP C-6) · test-fixture corpus implementation (doc 12 §5 — now a named **B0.0** prerequisite, 08A §2) · visual design produced from doc 06 · beta cohort recruitment (S8).

### 2E. Store / ops prerequisites (checklist: doc 15 §4)

S1 Play Console + signing · S2 developer verification (regional rollout per W4; start early) · S3 listing + claims audit · S4 privacy policy URL + data-safety form · S5 content rating · S6 support contact live · S7 target-API 36 compliance · S8 closed testing 12 testers/14 days (assume required) · S9 SBOM wiring · S10 LGPL source-offer hosting (**conditional** — only if L1 clears and the audio ext ships).

### 2F. Known minor revision work (only if owner wants it; not blocking anything)

| Item | Scope | Why minor |
|---|---|---|
| Doc 03 → v0.2 | Apply W4 evidence: persona segment signals, DH-3 validation, Q1/Q2 updates (its status line still says "W2–W4 pending") | W4 market content already lives in docs 02 v0.2/15/10; doc 03 is internally consistent without it |
| Doc 02A → v0.3 | ✅ **done 2026-10-19** — v0.3 resolves **CS-4** (decoder labelling) from documented practice. Remaining **CS-1/2/3/5/6/7** need device or user testing | Those six are spike / user-test work, not desk research — v0.3 does not wait on them |
| Doc 08 → v0.6 | Fold spike outcomes when they exist | Requires executed spikes. (v0.5 is now taken by Part 30's pointer to `08A` as the procedure layer) |
| Q13 → doc 04 | ✅ **CLOSED 2026-10-19** — settled from documented practice rather than a test: technical name + when-to-use description, `Auto` default, global setting *and* per-item override; bare HW/SW tokens forbidden (`research/decoder_label_practice.md`) | No longer blocking — advanced-sheet copy can be finalised. **Q12 also closed** (`research/autoplay_defaults.md`) |
| Q14 → doc 04/06 | ✅ **CLOSED 2026-10-19 (Part 28)** — the platform settles it by *contradicting itself*: legacy `MediaStore.Video.Media.DEFAULT_SORT_ORDER` = `"title"`, modern `QUERY_ARG_MEDIA_STANDARD_SORT_ORDER` = `DATE_ADDED` descending. Default = **most-recently-added first** (the modern path we query) with the contradicting constant recorded; density from M3's canonical feed layout (1/2/3/4 columns) (`research/platform_constants.md` §6) | No longer blocking the library screens. FR-LIB-011 now states both defaults |
| All docs | Version bumps + ledger rows on any future change | Process is ready (17 §7) |

### 2G. Full open-questions index
Per-document Q-sets: doc 04 Q1–Q14 (**Q12, Q13, Q14 all closed**) · 05 NQ1–6 · 06 U1–6 (**U1 closed**) · 07 C1–6 · 08 T1–6 (**T6 new**: RQ-06 has no pass criterion until the ASS quality bar is written down) · 09 A1–6 · 10 S1–6 · 11 OQ-1…5 · 12 Q12-1…6 · 13 Q13-1…6 · 14 Q14-1…5 · 15 OQ15-1…5 · 16 Q16-1…7 (**Q16-6/Q16-7 new**: is B0.0 inside the B0 range; how many engineers run B0 concurrently) · 02A **CS-4 resolved**, CS-1/2/3/5/6/7 open (priority stack: doc 17 §5) · **08A RB-1…RB-6 open** (B0.0 in-range vs additive · B0 concurrency · DEP-005 day-one order · independent re-adjudicator staffing · ASS quality bar before RQ-06 · capture rig acquire-vs-rent).

---

## 3. Suggested next actions (pick-up checklist, in order)

1. **Ask the owner for OD-1…OD-6** (start with OD-4 — it now also decides Q16-7, the B0 concurrency question that determines whether doc 16's 4–8 dev-week B0 range is reachable at all). Record answers in `00 §5` and mirror into `17 §1` statuses. Anything unanswered keeps its default (stated in 00 §5).
2. **Order the device lab (DEP-005) on day one.** It gates 11 of the 16 spikes, and its elapsed time is outside our control. Device scope: 08A §5.
3. **Build B0.0 instrumentation before any spike runs** — fixture corpus and adversarial/scale corpora (doc 12 §5), seek/first-frame and state-restoration harnesses (doc 12 §7). This is the work doc 16's B0 range does not visibly contain (Q16-6).
4. ~~Write spike runbooks~~ ✅ **done, Part 30** — `08A_SPIKE_RUNBOOKS.md`. Next: execute the spikes in a build environment.
5. **Open legal briefs L1/L2** with counsel (doc 11 §5 describes exactly what to ask; L1's counterfactual fallback is doc 11 §3.5).
6. **Long-lead store items:** S1, S2, S8 (doc 15 §4). Open the Play test track early for **RQ-08** — its elapsed time is store review, not engineering.
7. **Optional revisions** (§2F) if the owner requests them — use the change template (17 §7).
8. **After ANY new work:** update `00` (doc row + phase if applicable + part log — next part is **Part 31**) and bump the changed document's version.

---

## 4. Rules that must survive this handoff (non-negotiable)

1. **Docs-only until the owner explicitly opens a build/implementation phase.** The "no code" constraint belongs to this engagement; do not create app code as a continuation of it without a new owner instruction.
2. **Evidence discipline:** new external claims get `[V1]/[V2]/[R]/[I]/[E]/[U]` labels with URL + access date; volatile facts get "recheck before release".
3. **IDs are frozen and never repurposed** (FR/NFR/SCR/TST/EV/DEC/ASM/RSK/DEP/ADR/RQ/OD/L/S). Deprecate with a note instead.
4. **The constraint stack** every future decision must respect: no `INTERNET` in R1 (DEC-012) · no third-party telemetry/crash SDK (ADR-007) · no GPL linkage in the store build (DEC-009) · never `MANAGE_EXTERNAL_STORAGE` (DEC-011) · no bundled video decode / no bundled AAC (ADR-010) · no universal-support or unmeasured performance claims (DEC-010, doc 07 §13, doc 13 §6).
5. **Never silently omit or shorten** a mandated section; new material goes into a new part and the ledger/part log records it.
6. **Gates naming:** "Spec Gates A/B/C" = master-prompt §23 package-quality gates (doc 18); "Release Gates A/B/C" = app release pipeline (doc 12 §9). Don't conflate.

---

## 5. File map (what to open when)

| File | What it is | Open when |
|---|---|---|
| `00_INDEX_AND_STATUS.md` | Master ledger, IDs, ODs, part log | Always first |
| `18_TRACEABILITY_AND_READINESS_CHECKLIST.md` | Verdict, audits, failure review, kickoff checklist | Always second |
| `01_PRODUCT_VISION_AND_SCOPE.md` | Vision, boundary types A–E, scope fences | Scope questions |
| `02_MARKET_AND_COMPETITOR_RESEARCH.md` (v0.2) | Competitor matrix/profiles/SWOT, monetization anchors | Positioning questions |
| `02A_COMPETITOR_CASE_STUDIES.md` (v0.3) | Deep case studies: feature + settings catalogues, UI anatomy, gestures, decode/subtitle internals, adoption status | "How does app X actually do it?" |
| `03_PERSONAS_JOURNEYS_AND_STRATEGY.md` | Personas, JTBD, journeys, KPIs | User-value questions |
| `04_PRD_AND_FUNCTIONAL_REQUIREMENTS.md` (v0.8) | All FRs w/ acceptance criteria (incl. FR-CTRL-017 queue/end-of-item, FR-PLAT-001 PiP contract, FR-PLAT-002 notification/session, FR-LIB-011 defaults, FR-SUB-008 caption envelope) | Feature questions |
| `05_NON_FUNCTIONAL_REQUIREMENTS.md` | NFR targets (provisional `[T]`) | Quality-target questions |
| `06_UX_SCREEN_AND_STATE_SPECIFICATION.md` | SCR-001…018, states, design system, wireflows | UI questions / design work |
| `07_MEDIA_COMPATIBILITY.md` | A–X tier matrix, claims discipline | Format/claims questions |
| `08_TECHNOLOGY_EVALUATION_AND_ADRS.md` (v0.5) | Stack evaluation, ADR-001…013, RQ-01…16 spike defs (+ the authority split with 08A) | Before any tech work |
| `08A_SPIKE_RUNBOOKS.md` (v0.1) | **Spike runbooks:** S1–S10 operating rules, B0.0 instrumentation, scheduling map, per-spike cards, result template, RB-1…RB-6 | Immediately before **any** B0 work — including procurement and corpus building |
| `08B_B0_KICKOFF.md` (v0.1) | **B0 kickoff pack:** OD decision briefs, DEP-005 device-lab specification, week-one sequence, owner answer sheet | When asking the owner for decisions, or buying the device lab |
| `09_ARCHITECTURE_AND_DATA.md` | Modules, threading, data model, no-backend rationale | Before any build work |
| `10_SECURITY_PRIVACY_AND_COMPLIANCE.md` | THR-001…015, privacy inventory, compliance register | Privacy/security/store-form questions |
| `11_LICENSING_AND_DEPENDENCIES.md` (v0.2) | Component/patent registers, LGPL process, L1–L8 | Before adding ANY dependency |
| `11A_LEGAL_BRIEFS.md` (v0.1) | **Counsel briefs L1/L2** — sendable as-is: facts, readings for correction, numbered questions, fallbacks | When engaging counsel for L1/L2 |
| `12_QA_AND_TEST_STRATEGY.md` | TST suites, fixtures, device matrix, Release Gates | Any test planning |
| `13_PERFORMANCE_BENCHMARKING.md` | MB-01…12, claim policy | Any performance claim/question |
| `14_ANALYTICS_AND_SUPPORT.md` | EV registry, report bundles, support workflow | Diagnostics/support work |
| `15_MONETIZATION_AND_OPERATIONS.md` | Monetization A–E, S1–S10, rollout, maintenance, EOL | Store/monetization/ops work |
| `16_ROADMAP_AND_ESTIMATES.md` (v0.4) | Trains, phases B0–B7 `[E]`, B0 sizing flag, DEP register, roles | Planning/sizing questions |
| `17_RISKS_ASSUMPTIONS_AND_DECISIONS.md` | Canonical registers + DoR/DoD + change process | Any change; status lookups |
| `research/*.md` | W1–W4 raw sourced evidence | When a claim needs its source |

---

## 6. Housekeeping notes

- **Industry practice first — standing rule.** Before designing a behaviour, default, interaction, mechanism **or constant** from first principles, establish what shipping products do and why; adopt it when practice is settled and our constraints do not conflict; record deviations with the practice, the reason and who accepted them. Full rule, method and the classification of the current decision set: **17 §1.3**. Three rounds of this engagement have now found spec values that were invented rather than looked up — see Parts 24, 26 and 27 in doc 00 §7.
- **Version control:** the workspace **is** a git repository (initialized 2026-10-19, branch `main`). All **33** markdown files (24 documents + 9 research notes) plus `.gitignore`/`.gitattributes` are tracked and committed. Every part ends with a commit; **commit before starting any revision** so a revert is always one command away. Note the constraint nuance: this repository versions the *documents*; it is not application repository scaffolding — no app code, project files, or dependencies exist (see Part 25 in doc 00 §7).
- **Dates:** research accessed 2026-10-18; synthesis and all doc v0.1 drafting 2026-10-19; W5 case studies, the propagation pass, W6 (auto-advance), W7 (decoder labelling), W8 (interface constants), W9 (platform constants) and W10 (permissions, gesture exclusion, caption standards) 2026-10-19. Recheck-flagged facts age — see "recheck before release" items before any launch-related decision.
- **Part counter:** last logged = **Part 31**; next continuation = **Part 32**. Log it.
- **B0 is not "just run the spikes".** Three prerequisites must exist first, and two of them are absent from doc 08's spike table: the **device lab** (DEP-005 — order on day one; 11 of 16 spikes cannot run without it), the **B0.0 instrumentation** (fixture corpus + harnesses, doc 12 §5/§7), and the **ASS quality bar** that is RQ-06's pass criterion (08A RB-5, doc 08 T6). Authority: `08A_SPIKE_RUNBOOKS.md`; sizing consequences: doc 16 v0.3 §2 (B0 sizing flag) with Q16-6/Q16-7.
