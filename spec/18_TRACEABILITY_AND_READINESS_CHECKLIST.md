# 18 — Traceability, Gap Audit, and Readiness Verdict

**Version:** 0.3 (draft — final review artifact; v0.3 adds **Gate C post-audit delta 2** for doc 04 v0.7 (Parts 24–28, D-10…D-15) and confirms the verdict; v0.2 added the Gate C post-audit delta review for doc 04 v0.3)
**Date:** 2026-10-19
**Status:** Final deliverable of the specification engagement. **The verdict at the end of this document is the package's readiness verdict.** This review was performed against the master prompt's §21 (failure-mode review), §22 (deliverables), §23 (quality gates), and §24 (execution order), on all documents at their recorded versions (01–17 v0.1, 02/04 v0.2 at audit time; doc 04 is now v0.3 — see the Gate C post-audit delta in §3). No app code exists; no benchmark was run; no device was tested — those claims would be false and are not made.
**Consumes:** every document 00–17.
**Feeds:** the build phase (kickoff checklist §9), owner decisions (00 §5).

> ⚠ **Naming note:** master prompt §23 defines specification-quality "Gates A/B/C". Doc 12 defines *release-pipeline* Gates A/B/C. They are different things; doc 12 §9 now says so too. In this document, "Spec Gate" always means §23.

---

## 1. Traceability (§22 #22)

### 1.1 Requirement → acceptance → test

**Chain:** user problem (doc 02 §5 / doc 03 JTBD) → FR record with user need + priority + Given/When/Then acceptance criteria + `Tests:` line (doc 04) → TST suite with levels and device classes (doc 12) → gate that consumes it (doc 12 §9 / doc 13 baselines).

Every FR record in doc 04 carries an acceptance criterion and a `Tests:` reference (verified by sweep: 80+ `Tests:` lines across LIB/PLAY/CTRL/SUB/AUD/PLAT/SET/DIAG domains). Case-level IDs allocate in the build phase inside the doc 12 suite ranges; the authoritative case matrix then lives with the build repo and returns to this document's §1 table at the next revision.

| FR domain | Suites (doc 12 §4) | Verification levels | Consuming gate |
|---|---|---|---|
| FR-LIB (23) | TST-ONB, PRM, SCAN, META, THB, LIBV, FILT, SRCH, DETL, HIST, RSUM, FAV, PLST, RCN, EMP, OPEN, SDCARD, STORE, DUP | Unit, integration, instrumented, fault injection | Release Gate A + MB-03/04/05 (doc 13) |
| FR-PLAY (20) | TST-PLAY, CMPT, DEC, ERR, SEEK, SYNC, STALL, VFR, CORR, BIT, ORI, GEOM, RATE, HDR, BIT?, REST, DCRASH, LEAK, BG, DEG, 4K | Instrumented, harness (MB-07/08/10), manual on qualifying devices | Release Gates A–C |
| FR-CTRL (17) | TST-CTRL, GEST, PREF, ERGO, EXT, QUEUE | Instrumented + manual (OEM variance) | Release Gate B |
| FR-SUB (10) | TST-SUB (31+ cases) | Unit, instrumented, manual fidelity protocol | Release Gate B |
| FR-AUD (8) | TST-AUD (23+ cases) | Instrumented + manual route matrix | Release Gate B |
| FR-PLAT (6) | TST-PLAT, BG, REST | Instrumented + manual | Release Gate A |
| FR-SET / FR-DIAG | TST-SET, DIAG, PRIV-NET | Unit, instrumented, manual | Release Gates A/B |
| FR-NET / FR-PERS | Boundary declarations (doc 04 §6); no R1 feature tests | — (TST-PRIV-NET proofs the *absence* of network behavior) | Release Gate A |

### 1.2 NFR → verification

| NFR domain (doc 05) | Verification | Home |
|---|---|---|
| NFR-PERF-001…012 | MB-01…12 protocol, per-class baselines | Doc 13 |
| NFR-REL-* | Soak/stability (MB-12), TST-REST/DCRASH, Play vitals thresholds | 12/13 |
| NFR-RES-* | MB-09…11 (battery/memory), TST-THB/DEG | 13/12 |
| NFR-COMP-* | TST-CMPT tier validation; device matrix §6 | 12/07 |
| NFR-A11Y-* | TST-A11Y scripted journeys + token audit | 12/06 |
| NFR-SEC-* | Doc 10 threat verifications; supply-chain checks (CI) | 10/12 |
| NFR-PRIV-* | TST-PRIV-NET-01…04 proofs; no-INTERNET assertion | 10/12/14 |
| NFR-LOC-* | RTL/font-scale/script corpus sweeps | 06/12 |
| NFR-OPS-* | Release gates + maintenance model audits | 12/15 |

### 1.3 KPI → evidence source (honest measurement reality)

KPIs (doc 03 §7) are measured only from what our architecture makes observable: Play Console aggregates (ratings, retention, vitals), local diagnostics the user can export (share-sheet, doc 14), and lab studies. There is no dwell-stream or funnel analytics; targets stay provisional and are validated against the beta cohort first.

### 1.4 Example end-to-end chains (spot-check)

1. **Subtitle pain** (doc 02 §5, W1 evidence) → DH-3 → FR-SUB-004/006/010 → AC in doc 04 → TST-SUB suites → Release Gate B manual protocol → KPI-SAT-02.
2. **Scan friction** (doc 02 §5) → FR-LIB-004/006 → TST-SCAN-01…06 + MB-04 → release Gate A perf measurement → KPI-ACT first-session success.
3. **Permission trust** (W2/W4 policy research) → DEC-011/FR-PRM-001 → TST-PRM-01…09 + TST-PRIV-NET-04 → Release Gate A privacy proofs.

No chain terminates in an unowned requirement; no test suite exists without a requirement family.

---

## 2. Deliverable checklist (§22 #1–24) — final audit

| # | Required deliverable | Home | Audit result |
|---|---|---|---|
| 1 | Executive summary, vision, scope, exclusions | 01 (+00) | ✅ Present; boundary types A–E analyzed (01 §4), exclusions explicit |
| 2 | Research methodology, sources, competitor analysis | 02 + `research/` | ✅ Deliverables 1–8 of §5 all present (matrix, profiles, SWOT, pain synthesis §5, baseline-vs-differentiator, opportunity ranking, claims-to-avoid, unanswered questions §9) |
| 3 | Personas, JTBD, journeys | 03 | ✅ 5 personas, JTBD, JRN-01…08 incl. failure/refusal/upgrade journeys |
| 4 | Positioning, differentiation, validation plan | 03 (+02 §8) | ✅ DH hypotheses with falsification |
| 5 | PRD + FR catalogue | 04 | ✅ All domains; full record format incl. Given/When/Then AC |
| 6 | NFRs with measurable targets | 05 | ✅ 64 NFRs; provisional `[T]` labeled |
| 7 | Screens, navigation, states, UX | 06 | ✅ SCR-001…018, state catalogue, design system, a11y, wireframes |
| 8 | Compatibility matrix | 07 | ✅ A/B/C/D/X tiers + promotion pipeline + claims discipline |
| 9 | Technology evaluation, stack | 08 | ✅ Scored evaluation; ADR-001…013 |
| 10 | Architecture, modules, diagrams, ADRs | 08+09 | ✅ Text diagrams (overview + pipelines + state machine); formal sequence diagrams deferred to build docs (GAP-C4, minor) |
| 11 | Data model, persistence, backup, deletion, migration | 09 | ✅ Entities, identity/reconciliation, backup=false, migration |
| 12 | Threat model + privacy inventory | 10 | ✅ THR-001…015; inventory; permission matrix |
| 13 | Licensing register | 11 | ✅ Components + patents + LGPL process + SBOM plan; legal backlog L1–L8 |
| 14 | Monetization & commercial viability | 15A | ✅ Options A–E; hypotheses only (per instructions) |
| 15 | Backend decision or rationale | 09 §11 | ✅ Documented no-backend rationale |
| 16 | QA strategy, matrix, acceptance criteria | 12 | ✅ Suite map, fixtures, device matrix, severity, gates |
| 17 | Perf/battery/thermal benchmark protocol | 13 | ✅ MB-01…12, M1–M10, statistics, claim policy |
| 18 | Diagnostics, telemetry, support, feedback | 14 | ✅ Local-first design; EV registry; report bundle; blind spots stated |
| 19 | Store compliance, release, rollout, maintenance | 15B | ✅ Prereqs S1–S10, pipeline, playbooks, maintenance, EOL (§7.1) |
| 20 | Roadmap, dependencies, roles, estimates | 16 | ✅ Trains, B0–B7 `[E]` ranges, DEP register, roles |
| 21 | Risk/assumption/dependency/decision registers | 17 | ✅ Consolidated canonical registers |
| 22 | Requirements-to-tests traceability | 18 §1 (+04/12) | ✅ Domain-level matrix + completeness statement |
| 23 | DoR/DoD, release gates | 17 §6 + 12 §9 | ✅ Feature and release DoR/DoD; release gates defined |
| 24 | Final gap audit, open questions, residual risks | 18 | ✅ This document |

**Caveats recorded, not hidden:** concrete device list, case-level TST matrix, formal sequence diagrams, and reproducible-build runbooks are deliberately build-phase artifacts (they require physical hardware/code that this phase's constraints forbid creating). They are registered below (§5/§6), not omitted.

---

## 3. Specification-quality gates (§23)

### Spec Gate A — product/user completeness → **PASS**
- Personas/journeys: 5 personas, JRN-01…08 incl. first-run, failure recovery, permission refusal, upgrade return. ✔
- Scope boundaries explicit: DEC-001 (A now/B later/C–E out), OS/platform fences (DEC-002/005), permission model (DEC-011). ✔
- Features/screens/settings/permissions/states/user-facing errors covered: FR catalogue + SCR inventory incl. error/empty/limited states + error-code taxonomy. ✔
- Requirements connect to real user problems: §1.4 chains; W1 evidence base. ✔
- *Notes:* OD-1/2/3/5 answers would convert working defaults into owner-confirmed scope (not a completeness defect; 00 §5 defaults apply).

### Spec Gate B — technical/operational completeness → **PASS (spike-conditional items labeled)**
- Media engine and format claims realistic: tier system with X-blocked-legal cells; no universal claims; fallback honesty paths. ✔
- Platform differences documented: Android/iOS capability tables (01 §7, 02 §2.4, 07); iOS explicitly deferred with gate criteria. ✔
- Architecture/data/threading/lifecycle/resources/security/licensing/testing/release/maintenance/recovery addressed: 08/09/10/11/12/13/15 (+EOL §7.1). ✔
- Targets have definitions + verification: NFR↔MB/TST mapping (§1.2); provisional labels honest. ✔
- *Conditions:* engine/media specifics firm up at B0 spikes (RQ-01/03/05/06/13/14); bundled-audio path pending L1/L2. These were designed as spike/legal gates from the start, not discovered here.

### Spec Gate C — adversarial gap & consistency review → **PASS with recorded findings**

| # | Finding | Disposition |
|---|---|---|
| C-1 | Doc 03 KPI wording ("reported only if consented") read against DEC-012 (no INTERNET): cannot mean background upload | **Resolved** — doc 14 defines the only egress: user-initiated share-sheet export; KPI measurement adapted (§1.3) |
| C-2 | "Gate A/B/C" name collision (spec quality vs release pipeline) | **Resolved** — doc 12 §9 clarification added; doc 18 uses "Spec Gate" |
| C-3 | Doc 15 lacked end-of-life/unpublishing coverage (§18 requirement) | **Resolved** — doc 15 §7.1 added |
| C-4 | Sequence diagrams for critical journeys not formalized | **Accepted (minor)** — text wireflows (06 §9) + pipelines (09 §6/§7) carry the design; formalized first thing in build docs |
| C-5 | Concrete device list absent | **By design** — doc 12 §6 states coverage requirements; DEP-005 procures; list is a build-phase artifact |
| C-6 | Reproducible-build runbook thin | **Accepted (minor)** — doc 15 notes; CI defined in 12 §8; full runbook is build-phase |
| C-7 | Play Billing behavior without INTERNET permission unproven | **Registered** — RQ-08 spike; monetization out of R1 anyway |
| C-8 | Duplicate/conflicting requirements sweep | **Clean** — FR-NET/FR-PERS boundaries declared; no contradictions found in a domain-by-domain pass |
| C-9 | Vague-language sweep ("fast/smooth/secure") | **Clean** — NFRs carry measurement definitions; remaining vagueness lives only in non-normative prose |
| C-10 | Untestable requirements sweep | **Clean** — every FR has objective AC; claims carry tier-validation conditions |

*Method note:* this was a structured self-audit across all documents, requirements, and registers (consistency re-read, cross-reference check, claims sweep, dependency check). It is not a substitute for external adversarial review; the beta phase and legal review remain the external adversarial layers.

### Spec Gate C — post-audit delta (doc 04 v0.3 / doc 02A v0.2)

The audit above was performed against doc 04 **v0.2**. A subsequent evidence pass (W5 competitor case studies, doc 02A) changed doc 04 to **v0.3**. The delta was re-reviewed against the same Gate C criteria rather than assuming the original PASS still applies:

| # | Delta | Gate C question | Result |
|---|---|---|---|
| D-1 | **New FR-CTRL-017** (queue/play modes/end-of-item) | Was the gap real, or is this scope creep? | **Real gap** — `ENDED` appeared in the state machine (FR-PLAY-001, doc 09 §7.1) with no requirement defining what follows, and "next item" was referenced by FR-CTRL-002 without a queue model. Requirement is MUST/P1, not gold-plating |
| D-2 | FR-PLAY-003 fallback boundedness | Does this contradict "at most once per failure class"? | **No** — restates the same bound with the notice/persistence obligations attached; no new retry path created |
| D-3 | FR-LIB-015 tri-state resume | Does "Never" break Continue-watching? | **Resolved in the requirement** — position still recorded for the shelf unless history is separately opted out; no contradiction with FR-LIB-014 |
| D-4 | FR-LIB-011 article sort | Silent behaviour change? | **No** — off by default, stated explicitly; no migration of existing user state (there is no shipped state yet) |
| D-5 | FR-SUB-001 extra subtitle folder | Does this weaken DEC-011 (no broad storage)? | **No** — one user-designated folder via SAF, not a tree walk; consistent with the permission model |
| D-6 | FR-PLAY-011 Auto aspect | Any "magic" claim risk? | **Bounded** — MUST fall back to Fit when detection is inconclusive; no promise of automatic perfection |
| D-7 | Doc 09 `ENDED → next` transition | Is the transition testable without a decoder? | **Yes** — queue orchestration sits outside the engine adapter (§3 module map), so the matrix is unit-testable |
| D-8 | Doc 05 auto-advance clause | Is the target fabricated? | **Provisional `[T]`, unmeasured**, same status as every other NFR-PERF target; flagged as needing spike evidence |
| D-9 | Doc 07 §13 competitor-claims warning | Does it introduce unsourced competitor claims? | **No** — it explicitly refuses to treat vendor lists as evidence and names doc 02A §13's non-establishable items |

**Gate C remains PASS.** No delta introduced a contradiction, an untestable requirement, or an unsourced claim. **Verdict (§7) unchanged: READY FOR TECHNICAL SPIKES** — but note that doc 04 is now one revision past the audited baseline, so the build phase should treat v0.3 as authoritative.

**Risk created by the delta — since resolved.** FR-CTRL-017's auto-advance default was initially set by preference (stop-at-end) with thin evidence. It has since been **re-decided against documented industry practice** in six shipping products (Netflix, YouTube, Plex, Emby, Infuse, MX Player) plus one instructive counter-example (Swiftfin, whose OFF default generated support load). The default is now **ON when a next item exists**, with a 10 s cancellable countdown and a 2 h inactivity guard — see `research/autoplay_defaults.md` and the revised FR-CTRL-017. Q12 is **closed**; the residual risk is now only about tuning the two borrowed values (RSK-015), not about the direction of the decision.

### Spec Gate C — post-audit delta 2 (doc 04 **v0.7**, after Parts 24–28)

Parts 24–28 changed doc 04 four more times (v0.4 → v0.7), and docs 05/06/16/17 with it. The delta was re-reviewed against the same Gate C criteria rather than assuming the earlier PASS still holds:

| # | Delta | Gate C question | Result |
|---|---|---|---|
| D-10 | FR-CTRL-017 end-of-item default reversed (Part 24) | Was the reversal evidence-led, or preference again? | **Evidence-led** — six comparable products default auto-advance ON with a cancellable countdown, and the one counter-example reports support load for OFF. Independent of the earlier reasoning for the opposite choice, which is named in 17 §1.3 |
| D-11 | FR-PLAY-003 decoder labelling changed (Part 26) | Does the new copy make a capability claim? | **No** — "technical name + when-to-use description" describes *selection*, not supported formats; doc 07 §13 forbidden-phrasing rules still bind |
| D-12 | Interface constants changed: seek ±10 s, auto-hide 5 s, speed ladder, sleep-timer ladder (Part 27) | Is any of this a silent behaviour change? | **No** — each constant is recorded against its source (Media3 / YouTube / competitor practice) in `research/interface_defaults.md`, and the one internal contradiction found (FR-PLAY-005 vs FR-CTRL-002, two requirements disagreeing about the same control) was resolved rather than papered over |
| D-13 | FR-PLAT-001 now mandates `VideoPlaybackPictureInPicture` and the PiP platform contract (Part 28) | Does mandating one API over-constrain the build, or add an unsourced claim? | **Neither** — it is the platform vendor's own purpose-built implementation for video PiP, and every constraint listed is quoted from the Android SDK reference; the requirement adds no promise about behaviour on any OEM skin |
| D-14 | Layout restated by breakpoint instead of device (Part 28) | Scope expansion, or correction? | **Correction** — the requirement set already asserted adapted layouts; only the trigger was wrong. "Tablet" is not a breakpoint and does not survive rotation, folding or split-screen. No new requirement, no new screen |
| D-15 | FR-LIB-011 gains default sort + density; FR-CTRL-002 gains the 3 s previous-item boundary; FR-PLAY-005 names `SeekParameters` (Part 28) | Do any of these contradict an existing requirement or NFR? | **No** — all three fill absences. The sort default is recorded *together with* the platform constant that contradicts it, which is the opposite of an unsourced claim |

**Gate C remains PASS** across delta 2. No contradiction, no untestable requirement, no fabricated target. **Verdict (§7) unchanged: READY FOR TECHNICAL SPIKES**, with doc 04 **v0.7** authoritative.

---

## 4. Failure-mode review (§21)

Method: FMEA-style pass (detection → user impact → feedback → recovery → residual risk) over the §21 list plus architecture-derived scenarios. Full mechanisms live in the referenced docs; here is the audit summary.

| # | Scenario | Detection & feedback | Recovery | Key tests | Residual risk |
|---|---|---|---|---|---|
| 1 | Permissions denied/limited/revoked mid-session | FR-PRM states; banner (SCR-012) | Degraded mode; auto-rescan on return | TST-PRM | OEM revocation timing variance |
| 2 | File disappears/changes while indexed/open | Missing state (never silent vanish) | Relink guidance; identity reconciliation | TST-RCN/SDCARD | Heuristic false-positive relinks (user-confirmable) |
| 3 | Storage removed / out of space | Scan cancel states; thumbnail pause banner | Resume on return; hash-verified intact | TST-SDCARD/THB | Cheap-reader hardware quirks |
| 4 | Decoder crash / unsupported media | Error taxonomy + fallback notice | hw→sw election; tier-honest outcome | TST-DEC/ERR/CMPT | Vendor codec bugs (case-by-case; tiers absorb) |
| 5 | Memory exhaustion / process kill / thermal | Stats + resume markers | ≤last-persisted-point resume; never silent-zero | TST-REST/DCRASH, MB-10 | OS-kill timing (bounded loss by design) |
| 6 | Missing/malformed metadata | Null-safe fields surfaced honestly | Scan never blocks; edit-free display rules | TST-META/FUZZ | Exotic vendor tags |
| 7 | Timestamp/duration/sync anomalies | Salvage counters (never silent); sync stat | CORR degrade paths | TST-SYNC/VFR/CORR | Pathological files → best-effort tier |
| 8 | Seek stall / unrecoverable position | Error state with actions | Position fallback + report path | TST-STALL/SEEK/ERR | Specific vendor decoder seek bugs |
| 9 | Subtitle (un)supported / parse cost | Cap-and-degrade; encoding fix UI | Override + re-encode path; per-video memory | TST-SUB + corpus | ASS styling ceiling (RQ-06 decides tier) |
| 10 | Network source failure | **Out of R1 scope by decision** | N/A — no network stack exists (DEC-012) | PRIV-NET proofs absence | None in R1; R2 requires new review |
| 11 | Route/focus/orientation/lifecycle change | System-consistent states; TST-BG | Auto-pause/resume rules | TST-BG/AUD/REST | OEM gesture-zone variance |
| 12 | Large library / dupes / corrupt index | Progress truth; rebuild offer | 09 §5.4 rebuild; duplicate opt-in review | TST-SCAN/DUP; MB-04 | 50k+ remains stress tier |
| 13 | Interrupted update/migration | Migration progress surface | Idempotent migrations; resume | TST-SET | Multi-version jump matrix at release gates |
| 14 | Third-party SDK down/discontinued | N/A (minimal deps; no exported SDK) | Media3→libVLC path pre-scoped; exit strategies in license register | ADR program | Media3 strategy shift (fallback exists) |
| 15 | Device lacks presumed feature | Capability election + honest error | Tier-appropriate degradation | TST-CMPT/DEC | Rare OEM capability misreporting |
| 16 | Unintentional destructive action | Confirmations name exact scope | Read-only guarantee (hash-verified); index-only deletes | TST-STORE | None for user media (by construction) |
| 17 | Privacy control disabled / deletion request | Settings + deletion UX | Instant local delete; nothing remote exists | TST-PRIV-NET-04 | None remote |
| 18 | Optional cloud/AI failure affects local playback | N/A — none in R1 (DEC-007) | Local playback independent by architecture | — | Future-feature governance (14 §9 preconditions) |

No scenario is claimed "solved perfectly"; residual risks are the honest remainder, each owned by the tier system, the error taxonomy, or a registered risk (doc 17 §3).

---

## 5. Residual risks and unresolved items (post-audit)

**Blocking nothing at the current stage (spike start OK):** none. The audit found no contradiction or omission that invalidates the approach or prevents starting B0.

**Unresolved items, ranked by what they block:**

| Blocks | Items |
|---|---|
| Implementation commitments | B0 spike outcomes (RQ-01/03/05/06/08/13/14); provisional `[T]` targets become measured or adjusted at release Gate B |
| Bundled-audio extension only | Legal L1 (LGPL relinking), L2 (AC-3/E-AC-3) — fallback defined (doc 11 §3.5) |
| Calendar/estimation | OD-4 (team/budget/time); ASM-005/006 remain unvalidated |
| Owner-confirmed scope | OD-1, OD-2, OD-3, OD-5, OD-6 (working defaults active meanwhile) |
| Launch operations | S2 developer verification, S8 closed-testing clock, DEP-005 device procurement, DEP-006 hosting/support surfaces |
| Market hypothesis | ASM-011 (trust-over-features) — validated by beta signals before monetization decisions |

---

## 6. Open questions

Canonical index: doc 17 §5 (all Q-sets by document). The five that matter for the next milestones: OD-4, L1, RQ-01 outcome, OD-3, DEP-005/006. Nothing in the question backlog prevents B0 kickoff.

---

## 7. Readiness verdict (§23)

> # ✅ **READY FOR TECHNICAL SPIKES**

**Rationale.** The package is internally consistent (Spec Gate C pass with findings resolved or registered), complete against the §22 checklist, and honest about its provisional content. The designed next step per the master prompt (§19: "technical feasibility spikes after specification approval") is the spike program, which is fully specified in doc 08 (RQ-01…15 with hypotheses, thresholds, conditions, and the decisions each unblocks). Estimation may start **in parallel** on doc 16's ranges; implementation planning should wait for spike results and the owner conclusions of OD-1…OD-4.

**What this verdict does NOT mean:** it is not READY FOR IMPLEMENTATION PLANNING (spike outcomes and legal L1/L2 are still open), and it is not a claim that production will be easy — it is a claim that a competent team can now *start correctly* and will not have to invent major requirements.

**Permitted in parallel:** device procurement (DEP-005), store prerequisites S1/S2/S8 (long-lead), legal briefs L1/L2/L5/L6, design translation of doc 06, beta-recruitment groundwork, and owner decision-making on OD-1…6.

---

## 8. Package coverage statement (§23 closing discipline)

This package does **not** claim to cover literally every conceivable scenario. It claims: all master-prompt §22 deliverables present; all §21 failure families addressed with pointers and residual risk; all consequential claims evidence-labeled; every requirement testable; every uncertainty either spiked, gated, or registered. The remaining uncertainty is listed above — which is the point of a specification that is candid about being version 0.1.

---

## 9. Build-kickoff checklist (hand-off)

1. Owner: answer OD-1…6 (anything left unanswered keeps its default, which becomes expensive to reverse later).
2. Eng: stand up B0 spikes exactly per doc 08 definitions; time-box; record results as evidence rows.
3. Owner/ops: start S1 (Play Console/signing), S2 (verification), S8 (closed-testing recruitment clock), DEP-005 device purchases.
4. Owner/counsel: open L1 (LGPL relinking brief), L2 (codec confirmations).
5. Design: translate doc 06 into visuals (no structural changes without the doc 17 change template).
6. PM: convert doc 16 ranges to calendar once OD-4 is answered; Ratify `[T]` targets into a benchmark plan schedule (doc 13 §5).
7. Everyone: read in the order 00 → 01 → 17 → 04 → 05 → 08 → 09; obey the change process (17 §7); never let a dependency enter the graph without passing doc 11.
