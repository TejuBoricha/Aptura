# HANDOFF — Progress, Pending Work, and How to Resume

**As of:** 2026-10-19 · **Package:** `spec/` v0.1 (docs 00–18 + `research/`) · **Previous session:** specification engagement, Parts 1–19
**This file:** the "start here" for any new session (human or AI). It says what is done, what is not, and what to do next. It duplicates nothing critical — the authoritative ledger stays `00_INDEX_AND_STATUS.md`; the authoritative registers stay `17_…`; the authoritative verdict stays `18_… §7`.

---

## 0. TL;DR

1. **The specification package is COMPLETE.** All 19 documents (00–18) plus four sourced research files exist; phases 0–6 of the master prompt are done; the final audit issued the verdict **READY FOR TECHNICAL SPIKES** (doc 18 §7).
2. **Nothing in the package has been executed.** No app code, no tests run, no benchmark measured, no device tested — by the engagement's hard constraints. Do not ever describe the content as "verified working".
3. **The next session's job is NOT more spec writing.** It is: owner decisions (OD), the B0 technical spikes, the legal briefs (L), store prerequisites (S) — or revision work only if the owner requests changes. See §3.
4. **Before doing anything:** read `00` (ledger/IDs), then `18` (verdict + handoff checklist), then this file, then the task-relevant document. Do not redo completed docs.

---

## 1. What is DONE (do not redo)

| # | Item | Status |
|---|---|---|
| 00–18 | All 19 specification documents (vision, market, personas, FRs, NFRs, UX, compatibility, tech/ADRs, architecture/data, security/privacy, licensing, QA, benchmarking, diagnostics, ops, roadmap, registers, traceability) | ✅ Drafted (02A v0.2, 04 v0.4, 05/06/07/08/09/12/17/18 v0.2, rest v0.1) |
| research/ | W1 competitors · W2 platform · W3 licensing · W4 market/compliance — sourced, dated, evidence-labeled | ✅ Complete (accessed 2026-10-18, synthesized 2026-10-19) |
| 00 | Ledger, ID conventions, part log (Parts 1–23), OD table, first-response §3 | ✅ Closed for this phase |
| 18 | Traceability matrices, §22 deliverable audit (24/24), §23 Spec Gates A/B/C review (findings C-1…C-10) + Gate C post-audit delta (D-1…D-9), §21 failure-mode review | ✅ Verdict issued |
| 17 | Canonical registers: DEC-001…013, ASM-001…013, RSK-001…015 (+RSK-08-01…04 in doc 08), DEP-001…012, DoR/DoD, change process | ✅ |
| Constraints honored | No code, no scaffolding, no deps, no prototype, no execution claims | ✅ |

**Version map:** doc **04 = v0.4**; docs **02A, 05, 06, 07, 08, 09, 12, 17, 18 = v0.2**; doc **02 = v0.2**; everything else = v0.1.

---

## 2. What is NOT done (the pick-up inventory)

### 2A. Owner decisions — waiting on the owner (defaults active meanwhile; 00 §5)

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

### 2C. Technical spikes (B0) — the designed next step (definitions + thresholds: doc 08)

RQ-01 Media3(+FFmpeg audio ext) vs. incumbent-engine conformance · RQ-02 royalty-safe FFmpeg build config · RQ-03 HEVC-10/AV1 hardware-decode coverage on target classes · RQ-04 Dolby handling feasibility · RQ-05 SAF/MediaStore performance at 10k+ libraries · RQ-06 ASS/libass fidelity inside Media3 · RQ-07 process-death/state restoration · RQ-08 Play Billing offline without backend · RQ-09 iOS viability gate · RQ-10 privacy-preserving diagnostics viability · RQ-11 DB + thumbnail architecture at scale · RQ-12 VFR/HDR/refresh-rate behaviors · RQ-13 Compose performance on D1 class · RQ-14 ABI/split strategy.
**Note:** spikes execute in a *build* environment; this was a docs-only engagement. Spike *runbooks* can be written as documents if requested.

### 2D. Build-phase artifacts deliberately deferred (registered, not omissions — doc 18 §2)

Concrete device list + purchases (DEP-005) · case-level TST matrix (doc 18 §1.1) · formal sequence diagrams (GAP C-4) · build-reproducibility runbook (GAP C-6) · test-fixture corpus implementation (doc 12 §5) · visual design produced from doc 06 · beta cohort recruitment (S8).

### 2E. Store / ops prerequisites (checklist: doc 15 §4)

S1 Play Console + signing · S2 developer verification (regional rollout per W4; start early) · S3 listing + claims audit · S4 privacy policy URL + data-safety form · S5 content rating · S6 support contact live · S7 target-API 36 compliance · S8 closed testing 12 testers/14 days (assume required) · S9 SBOM wiring · S10 LGPL source-offer hosting (**conditional** — only if L1 clears and the audio ext ships).

### 2F. Known minor revision work (only if owner wants it; not blocking anything)

| Item | Scope | Why minor |
|---|---|---|
| Doc 03 → v0.2 | Apply W4 evidence: persona segment signals, DH-3 validation, Q1/Q2 updates (its status line still says "W2–W4 pending") | W4 market content already lives in docs 02 v0.2/15/10; doc 03 is internally consistent without it |
| Doc 02A → v0.3 | Fold CS-1…CS-7 answers (MX HW+ reality, ASS fidelity delta, decoder-label comprehension, dual-subtitle demand) | Requires device testing / user testing — spike or research work, not desk research. **Its §12.1 adoption status is now current as of v0.2** |
| Doc 08 → v0.3 | Fold spike outcomes when they exist | Requires executed spikes |
| Q13 → doc 04 | Settle decoder-label language with a comprehension test | Blocks final advanced-sheet copy. **Q12 is closed** (research/autoplay_defaults.md) |
| All docs | Version bumps + ledger rows on any future change | Process is ready (17 §7) |

### 2G. Full open-questions index
Per-document Q-sets: doc 04 Q1–Q13 (Q12 closed) · 05 NQ1–6 · 06 U1–6 · 07 C1–6 · 08 T1–5 · 09 A1–6 · 10 S1–6 · 11 OQ-1…5 · 12 Q12-1…6 · 13 Q13-1…6 · 14 Q14-1…5 · 15 OQ15-1…5 · 16 Q16-1…5 · 02A CS-1…CS-7 (priority stack: doc 17 §5).

---

## 3. Suggested next actions (pick-up checklist, in order)

1. **Ask the owner for OD-1…OD-6** (start with OD-4). Record answers in `00 §5` and mirror into `17 §1` statuses. Anything unanswered keeps its default (stated in 00 §5).
2. **If spikes are greenlit:** write spike runbooks from doc 08's definitions (docs-only work, allowed), start device procurement (DEP-005), then execute spikes in a build environment.
3. **Open legal briefs L1/L2** with counsel (doc 11 §5 describes exactly what to ask; L1's counterfactual fallback is doc 11 §3.5).
4. **Long-lead store items:** S1, S2, S8 (doc 15 §4).
5. **Optional revisions** (§2F) if the owner requests them — use the change template (17 §7).
6. **After ANY new work:** update `00` (doc row + phase if applicable + part log — next part is **Part 24**) and bump the changed document's version.

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
| `02A_COMPETITOR_CASE_STUDIES.md` (v0.2) | Deep case studies: feature + settings catalogues, UI anatomy, gestures, decode/subtitle internals, adoption status | "How does app X actually do it?" |
| `03_PERSONAS_JOURNEYS_AND_STRATEGY.md` | Personas, JTBD, journeys, KPIs | User-value questions |
| `04_PRD_AND_FUNCTIONAL_REQUIREMENTS.md` (v0.4) | All FRs w/ acceptance criteria (incl. FR-CTRL-017 queue/end-of-item) | Feature questions |
| `05_NON_FUNCTIONAL_REQUIREMENTS.md` | NFR targets (provisional `[T]`) | Quality-target questions |
| `06_UX_SCREEN_AND_STATE_SPECIFICATION.md` | SCR-001…018, states, design system, wireflows | UI questions / design work |
| `07_MEDIA_COMPATIBILITY.md` | A–X tier matrix, claims discipline | Format/claims questions |
| `08_TECHNOLOGY_EVALUATION_AND_ADRS.md` | Stack evaluation, ADR-001…013, RQ-01…14 spike defs | Before any tech work |
| `09_ARCHITECTURE_AND_DATA.md` | Modules, threading, data model, no-backend rationale | Before any build work |
| `10_SECURITY_PRIVACY_AND_COMPLIANCE.md` | THR-001…015, privacy inventory, compliance register | Privacy/security/store-form questions |
| `11_LICENSING_AND_DEPENDENCIES.md` | Component/patent registers, LGPL process, L1–L8 | Before adding ANY dependency |
| `12_QA_AND_TEST_STRATEGY.md` | TST suites, fixtures, device matrix, Release Gates | Any test planning |
| `13_PERFORMANCE_BENCHMARKING.md` | MB-01…12, claim policy | Any performance claim/question |
| `14_ANALYTICS_AND_SUPPORT.md` | EV registry, report bundles, support workflow | Diagnostics/support work |
| `15_MONETIZATION_AND_OPERATIONS.md` | Monetization A–E, S1–S10, rollout, maintenance, EOL | Store/monetization/ops work |
| `16_ROADMAP_AND_ESTIMATES.md` | Trains, phases B0–B7 `[E]`, DEP register, roles | Planning/sizing questions |
| `17_RISKS_ASSUMPTIONS_AND_DECISIONS.md` | Canonical registers + DoR/DoD + change process | Any change; status lookups |
| `research/*.md` | W1–W4 raw sourced evidence | When a claim needs its source |

---

## 6. Housekeeping notes

- **Version control:** the workspace **is** a git repository (initialized 2026-10-19, branch `main`). All 26 markdown files are committed and byte-identical to the working tree (verified by blob-hash comparison). Commit `d7671ad` is the initial import of the package. **Commit before starting any revision.** Note the constraint nuance: this repository versions the *documents*; it is not application repository scaffolding — no app code, project files, or dependencies exist (see Part 25 in doc 00 §7).
- **Dates:** research accessed 2026-10-18; synthesis and all doc v0.1 drafting 2026-10-19; W5 case studies and the propagation pass 2026-10-19. Recheck-flagged facts age — see "recheck before release" items before any launch-related decision.
- **Part counter:** last logged = **Part 25**; next continuation = **Part 26**. Log it.
