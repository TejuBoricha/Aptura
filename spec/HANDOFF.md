# HANDOFF — Progress, Pending Work, and How to Resume

**As of:** 2026-10-20 · **Package:** `spec/` v0.1 (docs 00–18 + `02A` case studies + `08A` spike runbooks + `08B` kickoff pack + `11A` counsel briefs + `12A` B0.0 build order + `research/`) · **Previous session:** specification engagement, Parts 1–38
**This file:** the "start here" for any new session (human or AI). It says what is done, what is not, and what to do next. It duplicates nothing critical — the authoritative ledger stays `00_INDEX_AND_STATUS.md`; the authoritative registers stay `17_…`; the authoritative verdict stays `18_… §7`.

---

## 0. TL;DR

1. **The specification package is COMPLETE.** All 19 documents (00–18) plus ten sourced research files exist; phases 0–6 of the master prompt are done; the final audit issued the verdict **READY FOR TECHNICAL SPIKES** (doc 18 §7).
2. **Nothing in the package has been executed.** No app code, no tests run, no benchmark measured, no device tested — by the engagement's hard constraints. Do not ever describe the content as "verified working".
3. **The next session's job is NOT more spec writing.** The owner decisions are **recorded** (Part 38 — delegated, replaceable: `00 §5`); what remains is **physical acts and build work**: order the device lab, send the counsel briefs (`11A` — cover note in §0.1), execute the B0 spikes (runnable procedures in `08A`; kickoff in `08B`), start store prerequisites (S) — or revision work only if the owner requests changes. See §3.
4. **Before doing anything:** read `00` (ledger/IDs), then `18` (verdict + handoff checklist), then this file, then the task-relevant document. Do not redo completed docs.

---

## 1. What is DONE (do not redo)

| # | Item | Status |
|---|---|---|
| 00–18 | All 19 specification documents (vision, market, personas, FRs, NFRs, UX, compatibility, tech/ADRs, architecture/data, security/privacy, licensing, QA, benchmarking, diagnostics, ops, roadmap, registers, traceability) | ✅ Drafted — current versions in the version map below |
| research/ | W1 competitors · W2 platform · W3 licensing · W4 market/compliance · W5 case studies (02A) · W6 autoplay defaults · W7 decoder labels · W8 interface constants · W9 platform constants · **W10 permissions, gestures, captions** · **W11 ASS/SSA fidelity sources** — sourced, dated, evidence-labeled | ✅ Complete (W1–W4 accessed 2026-10-18; W5–W11 2026-10-19) |
| 00 | Ledger, ID conventions, part log (Parts 1–38), OD table (decided by delegation — Part 38), first-response §3 | ✅ Closed for this phase |
| 18 | Traceability matrices, §22 deliverable audit (24/24), §23 Spec Gates A/B/C review (findings C-1…C-10) + **four** Gate C post-audit deltas (D-1…D-32; delta 4's D-31 withdrawn in Part 34 after it was found to be itself wrong), §21 failure-mode review | ✅ Verdict issued, re-confirmed after delta 4 + correction |
| 17 | Canonical registers: DEC-001…013, ASM-001…013, RSK-001…015 (+RSK-08-01…04 in doc 08), DEP-001…012, DoR/DoD, change process | ✅ |
| Constraints honored | No code, no scaffolding, no deps, no prototype, no execution claims | ✅ |
| 08A | **Spike runbooks** (companion to doc 08): operating rules S1–S10, the **B0.0 instrumentation prerequisite**, scheduling map by binding prerequisite, a runbook card for every spike RQ-01…16, lab/procurement scope, result-artifact template, RB-1…RB-7 | ✅ Written (docs-only) — **nothing executed**; **v0.5 resolves RB-1…RB-4 and RB-6 (Part 38)**; **v0.4 resolved RB-5: the RQ-06 bar is written (doc 07 §8.1)** |
| 08B | **B0 kickoff pack** (action layer): OD-1…OD-6 decision briefs, DEP-005 procurement specification (capability rows, purchase worksheet, intake probe, fleet growth, farm policy), week-one sequence, answer sheet | ✅ Written — **no device ordered, no counsel engaged**; **answer sheet filled with the Part 38 delegated decisions (§5)** |
| 11A | **Counsel briefs L1–L8** in sendable form: facts as shipped, our readings for correction, numbered questions with what each answer changes, postures/fallbacks, assets to hand over — batch A (L1/L2), batch B (L3–L8), plus a **paste-ready cover note (§0.1)** | ✅ Written — **not legal advice; not yet reviewed by a lawyer** |
| 12A | **B0.0 build order** (companion to doc 12): fixture manifests (A/B/C-D/adversarial/subtitle/metadata/scale + instrumented seek/sync), harness contracts (seek/first-frame, AV-sync, state-restoration, scan-at-scale), B0.0 done criteria — the corpus half needs **no devices and no owner answers** | ✅ Written (docs-only) — **nothing built: no fixture, no script, no harness exists**; v0.2 maps the subtitle corpus to the bar (doc 07 §8.1) |
| 07 §8.1 | **SSA/ASS fidelity quality bar** — RQ-06's pass criterion: plain-text floor, Classes E/F/X, adjudication + freezing rules; W11 sources (`research/ass_fidelity_reference.md`) | ✅ Written (docs-only) — resolves doc 08 T6 and 08A RB-5 |
| OD set | **Owner decisions recorded by delegation (Part 38)** — OD-1/2/3 confirm defaults; OD-4 → working scenario (2 engineers full-time, part-time design, no hard date, modest ceiling; answers Q16-6/Q16-7, confirms Q16-8); OD-5 → wave-1 set (India/Brazil/US/Indonesia, ≤5 locales); OD-6 → deferred by decision (L6 starts at launch-prep) | ✅ Recorded `00 §5`, `17 §2`; filled sheet `08B §5`; replaceable via 17 §7 |

**Version map:** docs **04, 08, 16 = v0.8**; docs **17, 18 = v0.7**; docs **06, 08A, 12 = v0.5**; docs **05, 08B = v0.4**; docs **02A, 07, 11, 11A = v0.3**; docs **02, 09, 12A = v0.2**; everything else = v0.1 (docs 00/01/03/10/13/14/15).

---

## 2. What is NOT done (the pick-up inventory)

### 2A. Owner decisions — **recorded by delegation (Part 38); replaceable via 17 §7** (`00 §5` · filled sheet: `08B` §5)

| ID | Question | Decision (Part 38, 2026-10-20) |
|---|---|---|
| OD-1 | Android-first vs. both platforms | **Android-first** (DEC-002 confirmed; iOS gated on RQ-09 at R1.x) |
| OD-2 | R1 = local-only vs. include network playback | **Local-only** (DEC-001 confirmed; network = R2 candidate) |
| OD-3 | Monetization intent at launch | **Free / no ads / no account** (DEC-004 confirmed) |
| OD-4 | **Team, budget, time** | **Working scenario: 2 engineers full-time + part-time design; no hard external date; modest ceiling.** Q16-7 = 2; Q16-8 confirmed. Replace with real figures when known |
| OD-5 | Launch regions & languages | **Wave 1: India (en-IN, hi-IN) · Brazil (pt-BR) · US (en-US) · Indonesia (id-ID)**; MENA/EU wave 2 |
| OD-6 | Product/brand name (blocks L6) | **Deferred by decision** — naming at launch-prep; L6 starts on a shortlist |

**Physical acts still outstanding (not decisions):** order the lab (DEP-005), send `11A` to counsel, open the Play Console track.

### 2B. Legal items ⚖️ — counsel required (canonical list: doc 11 §5; mirror doc 10 §6.3)

L1 LGPL §6 relinking (highest priority; blocks only the optional bundled audio extension — fallback exists) · L2 AC-3/E-AC-3 confirmations · L3 DTS/TrueHD wording · L4 Dolby brand usage in UI copy · L5 statute applicability (GDPR/CCPA/DPDP/LGPD/COPPA/EAA) · L6 Name/trademark clearance · L7 Own-app license/distribution model (F-Droid implications) · L8 Store-copy wording audit (per release).
**Sendable briefs for all of L1–L8: `11A_LEGAL_BRIEFS.md` v0.3** (facts as shipped, readings stated for correction, numbered questions with what each answer changes). Two batches: A = L1/L2 (capability gates, send now — **cover note ready: `11A` §0.1**); B = L3–L8 (copy/statute/naming — **L5's scope is now the wave-1 set (Part 38); L6 starts at launch-prep**).

### 2C. Technical spikes (B0) — the designed next step (definitions + thresholds: doc 08)

RQ-01 Media3(+FFmpeg audio ext) vs. incumbent-engine conformance · RQ-02 royalty-safe FFmpeg build config · RQ-03 HEVC-10/AV1 hardware-decode coverage on target classes · RQ-04 Dolby handling feasibility · RQ-05 SAF/MediaStore performance at 10k+ libraries · RQ-06 ASS/libass fidelity inside Media3 · RQ-07 process-death/state restoration · RQ-08 Play Billing offline without backend · RQ-09 iOS viability gate · RQ-10 privacy-preserving diagnostics viability · RQ-11 DB + thumbnail architecture at scale · RQ-12 VFR/HDR/refresh-rate behaviors · RQ-13 Compose performance on D1 class · RQ-14 ABI/split strategy · **RQ-15 adaptive layout across breakpoint changes** (rotation/foldable/split-screen; added Part 28) · **RQ-16 platform integration assumptions on retail builds** (media-session notification exemption + gesture exclusion rects; added Part 29).
**Note:** spikes execute in a *build* environment; this was a docs-only engagement. **Runbooks are now written: `08A_SPIKE_RUNBOOKS.md`** — operating rules, per-spike procedures, result template — and **the B0.0 build order is written: `12A_B0_INSTRUMENTATION.md`** (corpus manifests, harness contracts, done criteria). Writing the runbooks out also surfaced two things doc 16 did not state: **B0 has an unlisted prerequisite stage — B0.0, instrumentation (fixture corpus + harnesses)** — and **DEP-005 sits on the B0 critical path** (the device-dependent spikes — a derived 11 of 16; 08A §3 — need the lab), so it is ordered on day one — doc 16 v0.4 §2/§7; **Q16-6/Q16-7 are now answered (Part 38: B0.0 additive; concurrency 2)**.

### 2D. Build-phase artifacts deliberately deferred (registered, not omissions — doc 18 §2)

Concrete device list + purchases (DEP-005 — now scoped in **08A §5**, procurement spec in **08B §3**) · case-level TST matrix (doc 18 §1.1) · formal sequence diagrams (GAP C-4) · build-reproducibility runbook (GAP C-6) · test-fixture corpus implementation (doc 12 §5 — a named **B0.0** prerequisite, 08A §2; **build order now written in `12A` §2**) · visual design produced from doc 06 · beta cohort recruitment (S8).

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
Per-document Q-sets: doc 04 Q1–Q14 (**Q12, Q13, Q14 all closed**) · 05 NQ1–6 · 06 U1–6 (**U1 closed**) · 07 C1–6 · 08 T1–6 (**T6 resolved, Part 37**: the bar exists — doc 07 §8.1) · 09 A1–6 · 10 S1–6 · 11 OQ-1…5 · 12 Q12-1…6 · 13 Q13-1…6 · 14 Q14-1…5 · 15 OQ15-1…5 · 16 Q16-1…8 (**Q16-6/Q16-7 answered, Q16-8 confirmed — Part 38**) · 02A **CS-4 resolved**, CS-1/2/3/5/6/7 open (priority stack: doc 17 §5) · **08A RB-1…RB-7** (**RB-1…RB-4 and RB-6 resolved — Part 38**: B0.0 additive · B0 concurrency = 2 · DEP-005 ordered on day one · independent re-adjudicator = the non-running engineer · capture rig borrowed/rented first; **RB-5 resolved (Part 37): the bar is written — doc 07 §8.1**; **RB-7 open** — second procurement round decided at B6 gap analysis).

---

## 3. Suggested next actions (pick-up checklist, in order)

1. ~~Ask the owner for OD-1…OD-6~~ ✅ **recorded Part 38 (delegated — `00 §5`, filled sheet `08B` §5)**. Replace any decision that misstates reality via the change process (17 §7) — most consequential: OD-4's headcount/budget scenario. **Next physical act: order the lab (item 2).**
2. **Order the device lab (DEP-005) on day one.** It gates 11 of the 16 spikes, and its elapsed time is outside our control. Device scope: 08A §5.
3. **Build B0.0 instrumentation before any spike runs** — fixture corpus and adversarial/scale corpora (doc 12 §5), seek/first-frame and state-restoration harnesses (doc 12 §7). This is the work doc 16's B0 range does not visibly contain (Q16-6), and it is the only B0 work that needs **no devices and no owner answers**. Build order: **`12A_B0_INSTRUMENTATION.md`** §2 (corpora), §3 (harness contracts), §4 (done criteria).
4. ~~Write spike runbooks~~ ✅ **done, Part 30** — `08A_SPIKE_RUNBOOKS.md`. Next: execute the spikes in a build environment.
5. **Send legal briefs L1/L2 to counsel** — briefs: `11A` §2–§3 with the cover note (`11A` §0.1); L1's counterfactual fallback is doc 11 §3.5.
6. **Long-lead store items:** S1, S2, S8 (doc 15 §4). Open the Play test track early for **RQ-08** — its elapsed time is store review, not engineering.
7. **Optional revisions** (§2F) if the owner requests them — use the change template (17 §7).
8. **After ANY new work:** update `00` (doc row + phase if applicable + part log — next part is **Part 39**) and bump the changed document's version.

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
| `18_TRACEABILITY_AND_READINESS_CHECKLIST.md` (v0.7) | Verdict, audits, four Gate C post-audit deltas, failure review, kickoff checklist (Part 38 status propagated; verdict unchanged) | Always second |
| `01_PRODUCT_VISION_AND_SCOPE.md` | Vision, boundary types A–E, scope fences | Scope questions |
| `02_MARKET_AND_COMPETITOR_RESEARCH.md` (v0.2) | Competitor matrix/profiles/SWOT, monetization anchors | Positioning questions |
| `02A_COMPETITOR_CASE_STUDIES.md` (v0.3) | Deep case studies: feature + settings catalogues, UI anatomy, gestures, decode/subtitle internals, adoption status | "How does app X actually do it?" |
| `03_PERSONAS_JOURNEYS_AND_STRATEGY.md` | Personas, JTBD, journeys, KPIs | User-value questions |
| `04_PRD_AND_FUNCTIONAL_REQUIREMENTS.md` (v0.8) | All FRs w/ acceptance criteria (incl. FR-CTRL-017 queue/end-of-item, FR-PLAT-001 PiP contract, FR-PLAT-002 notification/session, FR-LIB-011 defaults, FR-SUB-008 caption envelope) | Feature questions |
| `05_NON_FUNCTIONAL_REQUIREMENTS.md` | NFR targets (provisional `[T]`) | Quality-target questions |
| `06_UX_SCREEN_AND_STATE_SPECIFICATION.md` | SCR-001…018, states, design system, wireflows | UI questions / design work |
| `07_MEDIA_COMPATIBILITY.md` (v0.3) | A–X tier matrix, claims discipline, **§8.1 ASS fidelity bar (RQ-06's pass criterion)** | Format/claims questions; RQ-06 prep |
| `08_TECHNOLOGY_EVALUATION_AND_ADRS.md` (v0.8) | Stack evaluation, ADR-001…013, RQ-01…16 spike defs (+ the authority split with 08A; §6.1 prior art + RQ-06 bar calibration; §6.2 audit notes incl. the Part 34 D-31 correction; T6 resolved via doc 07 §8.1) | Before any tech work |
| `08A_SPIKE_RUNBOOKS.md` (v0.5) | **Spike runbooks:** S1–S10 operating rules, B0.0 instrumentation (build order: `12A`), scheduling map with derived device requirement, per-spike cards (RQ-06 points at the bar), result template, RB-1…RB-7 (RB-1…RB-4/RB-6 resolved — Part 38; RB-5 Part 37; RB-7 open) | Immediately before **any** B0 work — including procurement and corpus building |
| `08B_B0_KICKOFF.md` (v0.4) | **B0 kickoff pack:** OD decision briefs, DEP-005 device-lab specification (worksheet §3.2.1, intake probe §3.4.1, fleet growth §3.5), week-one sequence, **filled answer sheet (§5)** | When buying the device lab, or re-reading the Part 38 decisions |
| `09_ARCHITECTURE_AND_DATA.md` | Modules, threading, data model, no-backend rationale | Before any build work |
| `10_SECURITY_PRIVACY_AND_COMPLIANCE.md` | THR-001…015, privacy inventory, compliance register | Privacy/security/store-form questions |
| `11_LICENSING_AND_DEPENDENCIES.md` (v0.3) | Component/patent registers, LGPL process, L1–L8 (all briefed) | Before adding ANY dependency |
| `11A_LEGAL_BRIEFS.md` (v0.3) | **Counsel briefs L1–L8** — sendable: facts, readings for correction, numbered questions, postures; batch A (L1/L2) with **cover note (§0.1)** now, batch B (L3–L8) when convenient | When engaging counsel |
| `12_QA_AND_TEST_STRATEGY.md` (v0.5) | TST suites, fixtures, device matrix, Release Gates | Any test planning |
| `12A_B0_INSTRUMENTATION.md` (v0.2) | **B0.0 build order:** fixture manifests (A/B/C-D/adversarial/subtitle/metadata/scale, instrumented seek/sync), harness contracts with output artifacts, B0.0 done criteria | Immediately, when starting B0.0 — the corpus half needs no devices |
| `13_PERFORMANCE_BENCHMARKING.md` | MB-01…12, claim policy | Any performance claim/question |
| `14_ANALYTICS_AND_SUPPORT.md` | EV registry, report bundles, support workflow | Diagnostics/support work |
| `15_MONETIZATION_AND_OPERATIONS.md` | Monetization A–E, S1–S10, rollout, maintenance, EOL | Store/monetization/ops work |
| `16_ROADMAP_AND_ESTIMATES.md` (v0.8) | Trains, phases B0–B7 `[E]`, **§2.1 OD-4 decision support (2-engineer working case adopted — Part 38)**, **§5.1 sized de-scope ladder, exercisable** (rung scopes, `[E]` savings, decision points, cumulative value; Q16-8 confirmed), B0 sizing flag (resolved), DEP register, roles | Planning/sizing questions and any de-scope discussion |
| `17_RISKS_ASSUMPTIONS_AND_DECISIONS.md` | Canonical registers + DoR/DoD + change process | Any change; status lookups |
| `research/*.md` | W1–W11 raw sourced evidence | When a claim needs its source |

---

## 6. Housekeeping notes

- **Industry practice first — standing rule.** Before designing a behaviour, default, interaction, mechanism **or constant** from first principles, establish what shipping products do and why; adopt it when practice is settled and our constraints do not conflict; record deviations with the practice, the reason and who accepted them. Full rule, method and the classification of the current decision set: **17 §1.3**. Three rounds of this engagement have now found spec values that were invented rather than looked up — see Parts 24, 26 and 27 in doc 00 §7.
- **Version control:** the workspace **is** a git repository (initialized 2026-10-19, branch `main`). All **35** markdown files (24 documents + 10 research notes + this guide) plus `.gitignore`/`.gitattributes` (37 tracked files) are tracked and committed. Every part ends with a commit; **commit before starting any revision** so a revert is always one command away. Note the constraint nuance: this repository versions the *documents*; it is not application repository scaffolding — no app code, project files, or dependencies exist (see Part 25 in doc 00 §7).
- **Dates:** research accessed 2026-10-18; synthesis and all doc v0.1 drafting 2026-10-19; W5 case studies, the propagation pass, W6 (auto-advance), W7 (decoder labelling), W8 (interface constants), W9 (platform constants) and W10 (permissions, gesture exclusion, caption standards), plus W11 (ASS/SSA fidelity sources — recheck-flagged) 2026-10-19; the Part 38 decisions were recorded 2026-10-20. Recheck-flagged facts age — see "recheck before release" items before any launch-related decision.
- **Part counter:** last logged = **Part 38**; next continuation = **Part 39**. Log it.
- **B0 is not "just run the spikes".** Three prerequisites must exist first, and two of them are absent from doc 08's spike table: the **device lab** (DEP-005 — order on day one; the device-dependent spikes — a derived 11 of 16, 08A §3 — cannot run without it), the **B0.0 instrumentation** (fixture corpus + harnesses, doc 12 §5/§7; **build order in `12A`**), and the **ASS quality bar** that is RQ-06's pass criterion — **now written: doc 07 §8.1 (Part 37)**. Authority: `08A_SPIKE_RUNBOOKS.md`; sizing consequences: doc 16 v0.7 §2 (B0 sizing flag; §2.1 OD-4 scenarios; §5.1 sized de-scope ladder) with Q16-6/Q16-7/Q16-8. **Two rules this engagement learned the hard way, both from Part 32:** never buy a device *for* a decode deficiency (it manufactures the spike's expected result — D-27), and the B0 lab is a **subset** of doc 12 §6's matrix fleet, procured as one fleet that grows (`08B` §3.5 — D-23). **And one rule from Part 34:** a *negative* claim ("no requirement exists") requires a case-insensitive search plus synonyms — D-31 was written on one case-sensitive search and was wrong.
