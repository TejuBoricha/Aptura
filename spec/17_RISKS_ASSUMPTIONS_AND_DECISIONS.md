# 17 — Consolidated Registers, DoR/DoD, and Change Process

**Version:** 0.2 (draft; v0.2 adds RSK-015 and the doc 02A question pointers)
**Date:** 2026-10-19
**Status:** Draft — **this document is the canonical home** for the DEC / ASM / RSK / DEP registers and for the Definition of Ready/Done. Where earlier documents hold originals (00 §3, 08, 16 §3), this register supersedes for lookup while preserving the original context. IDs are frozen; nothing is silently renumbered (change rules §7).
**Consumes:** every prior document.
**Feeds:** 18 (readiness verdict consumes risk/decision status).

---

## 1. Decision register (canonical)

### 1.1 Product & boundary decisions (DEC)

Status legend: **Active** = working default in force (owner silent → 00 §5 default applied); **Pending OD** = an owner answer could still change it before build phase; decisions once exercised in code become expensive to reverse (noted).

| ID | Decision | Rationale (one-line) | Status |
|---|---|---|---|
| DEC-001 | R1 = local-file player (type A); SMB/NAS/HTTP = R2 candidate; C/D/E out of scope | Evidence-strong pain area; bounded ops/legal surface | Active (OD-2 pending) |
| DEC-002 | Android-first; iOS at an evidence-gated R1.x decision (RQ-09) | Market/likelihood + custom-engine feasibility | Active (OD-1 pending) |
| DEC-003 | Engine hypothesis: Media3 core + self-built LGPL-2.1 FFmpeg **audio** extension; no bundled video decode; libVLC fallback only if spikes fail | Licensing-clean + platform-native; finalized by B0 spikes | Active (spike-pending) |
| DEC-004 | Free, no ads, no account at launch; monetization hypotheses only | Trust positioning; billing deferred | Active (OD-3 pending) |
| DEC-005 | Android phones + tablets from R1; TV/foldables/car/wearables out of R1 | Input-model and test-matrix discipline | Active |
| DEC-006 | No DRM/protected playback; honest failure UI | Keeps scope/licensing/security small | Active |
| DEC-007 | No AI features in R1; any R3+ AI feature must be on-device-first, opt-in, never silently upload | Privacy boundary | Active |
| DEC-008 | Telemetry: none by default; local-only diagnostics; any upload would be explicit user action | Privacy product principle | Active |
| DEC-009 | LGPL-clean store build; GPL/FOSS channel (F-Droid) deferred as separate build decision | Prevents incompatible obligations | Active |
| DEC-010 | No universal format/device support promised anywhere | Master prompt §2.5; tier system is the contract | Active |
| DEC-011 | File access via MediaStore + Photo Picker + SAF; `MANAGE_EXTERNAL_STORAGE` never requested | Play policy + user trust; degrades honestly (FR-PLAT-006) | Active (declaration gate doc 10) |
| DEC-012 | R1 ships **without** the `INTERNET` permission (strengthens DEC-008) | Strongest possible privacy proof; no network features in R1 scope | Active (see ADR-011) |
| DEC-013 | R1 distribution is store-only (Play); no direct-APK sideload channel | Update integrity/security; noted as a risk if store friction occurs (RSK-005) | Active |

### 1.2 Architecture decisions (ADR) — canonical in doc 08 §ADR

Pointer table (details, alternatives, consequences in doc 08; several are *provisional* until B0 spikes):

| ADR | One-line | Posture |
|---|---|---|
| ADR-001 | Media3 as playback core | Provisional until RQ-01 |
| ADR-002 | Kotlin + Compose with D1 spike gate | Provisional until RQ-13 |
| ADR-003 | Subtitle path Media3-native; libass gated by RQ-06 | Provisional until RQ-06 |
| ADR-004 | Room (library) + DataStore (settings) | Settled |
| ADR-005 | Hilt DI | Settled |
| ADR-006 | Coil images | Settled |
| ADR-007 | No third-party telemetry/crash SDK in R1 | Settled (product-level) |
| ADR-008 | minSdk 26; target API per Play deadlines (`recheck`) | Settled draft |
| ADR-009 | arm64 required; v7 decision at build spike (RQ-14) | Provisional until RQ-14 |
| ADR-010 | Self-built LGPL FFmpeg **audio** ext; no bundled video/AAC decode | Gated by L1 (legal) |
| ADR-011 | No network stack at all in R1 | Settled (mirrors DEC-012) |
| ADR-012 | Packaged flags; no remote config | Settled |
| ADR-013 | WorkManager for batch work | Settled |

---

## 2. Assumption register (canonical)

| ID | Assumption | Validation method | Status |
|---|---|---|---|
| ASM-001 | Primary users play personal local files (downloaded/recorded/copied) | W1 evidence; personas | Supported by research (provisional) |
| ASM-002 | Android-first economics favor R1 Android | W2/W4 evidence; DEC-002 | Provisional |
| ASM-003 | Local-first, no account, no mandatory backend | Doc 09 no-backend rationale | Validated by design (no server exists) |
| ASM-004 | Free/no-ads launch | DEC-004; W4 monetization analysis | Active (OD-3 pending) |
| ASM-005 | Small senior team (2–4 eng + part-time design/QA) | OD-4 | **Unvalidated** |
| ASM-006 | Timeline/budget unknown; phase-relative sizing only | OD-4; doc 16 ranges | **Unvalidated** |
| ASM-007 | Shipping config uses platform/royalty-free decoders; unclear royalty → excluded/flagged | Doc 11 register | Active |
| ASM-008 | Incumbent pain clusters: ads/trust, reliability edges, subtitles, scan friction, low-end perf, bloat | W1 pain synthesis (doc 02 §5) | Supported |
| ASM-009 | Launch regions: India, Brazil, SE Asia, MENA + US/EU; EN + small locale set | W4; OD-5 | Provisional |
| ASM-010 | No DRM support; honest failure | DEC-006 | Active |
| ASM-011 | Target segment prioritizes trust/privacy/reliability over feature breadth | Positioning depends on it (docs 02/03); beta signals | **Core hypothesis to watch** |
| ASM-012 | Bundled audio decode is an enhancement; product survives its removal | Doc 11 §3.5 fallback design | Validated-by-design |
| ASM-013 | Typical large libraries ≈ ≤10k items; 50k is a stress case `[E]` | Doc 13 XL benchmark tier; support signals | Provisional |

---

## 3. Risk register (canonical, program-level)

Spike-scoped pre-registrations **RSK-08-01…04** remain authoritative in doc 08 §risks (Compose-on-D1, FFmpeg-legal gate, fallback readiness, ASS ceiling). This register adds program-level risks; L = likelihood, I = impact (L/M/H; `U` = unknown).

| ID | Risk | L×I | Mitigation | Owner | Status |
|---|---|---|---|---|---|
| RSK-001 | Engine conformance fails on mainstream corpus (RQ-01) | M×H | Time-boxed spike w/ pre-agreed thresholds; libVLC fallback pre-scoped; tier matrix absorbs gaps | eng | Open (B0) |
| RSK-002 | LGPL §6 relinking verdict negative (L1) | U×L | Doc 11 §3.5 fallback: ship without bundled audio decode; no launch dependency | owner+counsel | Open (legal) |
| RSK-003 | Expectation mismatch: "won't play my file" (HDR/DV/DTS-heavy content) → review damage despite honest tiers | M×H | Error taxonomy precision; Help pages; onboarding copy; claims discipline | product | Open (standing) |
| RSK-004 | D1 devices below UX bar on Compose/player | M×M | RQ-13 spike + hybrid fallback; de-scope ladder items | eng | Open (B0) |
| RSK-005 | Store friction: verification program, closed-testing clock, policy shifts, store-only distribution (DEC-013) | M×M | Start S1/S2/S8 early; "recheck before release" flags; appeal pack = compliance docs | owner | Open (ops) |
| RSK-006 | Name/trademark conflict (OD-6) | M×M | L6 legal clearance before listing; rename cost budgeted in spirit | owner | Open (legal) |
| RSK-007 | Incumbent copycat of positioning | M×L | Execution quality + documented trust proofs as the moat (doc 02 §8) | product | Accepted |
| RSK-008 | Subtitle edges (mojibake, ASS fidelity) erode the differentiator (see RSK-08-04) | M×M | RQ-06; corpus-driven testing; honest tiers; guided fixes | eng | Open |
| RSK-009 | Scope creep inflates R1 | M×H | One-way de-scope ladder (16 §5); R1.x gating; firewall rule | owner | Open (standing) |
| RSK-010 | Zero-telemetry blind spots delay defect discovery | M×M | Beta cohort, Play vitals, review mining, report bundles (14 §7) | quality | Accepted by design |
| RSK-011 | Annual OS behavior changes break storage/background/gesture flows | M×M | Maintenance model (15 §7); beta-season reviews; threat-model updates | eng | Standing |
| RSK-012 | Patent landscape shifts (HEVC terms, AV1 litigation) change cost calculus | L×M | Platform-decode-only posture insulates R1; quarterly rechecks (doc 11 §2) | owner | Accepted |
| RSK-013 | Storage variance (SD/portable) undermines scan expectations on low-end | M×M | MB-04 slow-storage profile; honest notes; degraded messaging | eng | Open |
| RSK-014 | Key-person concentration in small team | M×H | Docs-as-contract (this package); modular architecture; pairing on critical paths | owner | Open (standing) |
| RSK-015 | **Auto-advance default (FR-CTRL-017) is now evidence-based but the specific values are borrowed** — 10 s countdown and 2 h inactivity guard are adopted from Plex's documented defaults, not validated for our audience. A wrong countdown is mildly annoying; a missing inactivity guard would let the app play all night | M×M | Default ON only when a next item exists; countdown always cancellable; 2 h guard mandatory (not a setting default that can be silently dropped); both values configurable and covered by TST-QUEUE; re-check with beta cohort | product | Open (tuning only — **decision itself closed** 2026-10-19, research/autoplay_defaults.md) |

---

## 4. Dependency register (canonical)

Restated from doc 16 §3 (origin); canonical here for lookup.

| ID | Dependency | Blocks | Owner |
|---|---|---|---|
| DEP-001 | RQ-01/02/03 outcomes → ADR-001 lock | B3 start | eng |
| DEP-002 | Legal L1 (LGPL relinking) | Bundled audio ext | owner+counsel |
| DEP-003 | Legal L2 (AC-3/E-AC-3) | FR-AUD-007 add-back (R1.x) | counsel |
| DEP-004 | OD-4 (team/budget/time) | Calendar conversion of all sizes | owner |
| DEP-005 | Device-lab procurement | B6 benchmarks; TST-CMPT | owner |
| DEP-006 | Privacy-policy hosting + support channel | Store prereqs S4/S6 | owner |
| DEP-007 | Developer verification completion | Distribution | owner |
| DEP-008 | Closed-testing recruitment (12/14-day rule if applicable) | Production launch | owner |
| DEP-009 | RQ-06 ASS verdict | Subtitle tier commitment | eng |
| DEP-010 | RQ-08 billing-offline verdict | R1.x monetization option B | eng |
| DEP-011 | Designer engagement | B1 tokens/B5 polish/store assets | owner |
| DEP-012 | Patent/court watch (informational) | R2 planning only | owner |

---

## 5. Open-questions index

Detail lives in each source document; this index is for navigation. **Owner decisions gate the rest** — the top of the stack:

| Priority | Open item | Home |
|---|---|---|
| 1 | **OD-4** team/budget/time (unlocks calendar + device-lab scope) | 00 §5 |
| 2 | **L1** LGPL relinking verdict (gates only the optional audio ext) | 11 §5 |
| 3 | **B0 spike outcomes** (RQ-01/03/05/06/08/13/14) | 08 |
| 4 | OD-3 monetization intent; OD-5 regions; OD-6 name; OD-1/OD-2 confirmations | 00 §5 |
| 5 | Device budget (Q12-2), support staffing (Q14-5/Q16-3), hosting (OQ15-3/Q14-2) | 12/14/15/16 |

Full sets: doc 04 Q1–Q13 · doc 05 NQ1–NQ6 · doc 06 U1–U6 · doc 07 C1–C6 · doc 08 T1–T5 · doc 09 A1–A6 · doc 10 S1–S6 · doc 11 OQ-1…5 · doc 12 Q12-1…6 · doc 13 Q13-1…6 · doc 14 Q14-1…5 · doc 15 OQ15-1…5 · doc 16 Q16-1…5 · doc 02A CS-1…CS-7.

**Behavioural questions raised by W5 that are *not* owner decisions** (they need evidence, not preference): **Q13** decoder label language (doc 04 §5) and **CS-1…CS-7** (doc 02A §14). **Q12 is closed** — the auto-advance default was settled against six shipping products (`research/autoplay_defaults.md`, doc 04 FR-CTRL-017). Q13 must be settled before the advanced-sheet copy is finalised.

---

## 6. Definition of Ready / Definition of Done

### 6.1 DoR — a feature may enter a build phase only with

1. FR(s) with acceptance criteria (doc 04); 2. UX states specified (doc 06); 3. tier honesty classification (doc 07) where capability-bearing; 4. architectural placement (doc 09); 5. privacy impact assessed against doc 10 inventory (no new data flow without doc 10 update first); 6. license impact clean or profiled (doc 11); 7. tests identified (doc 12); 8. benchmark method if claim-bearing (doc 13); 9. strings externalized-ready; 10. no unresolved dependency from §4 blocking it.

### 6.2 DoD — a feature is done only when

1. Implemented to spec incl. **all states** (empty/loading/error/limited per doc 06 §4); 2. tests green (unit + integration + UI where applicable) per doc 12; 3. accessibility checked (TalkBack label, font scale, contrast); 4. performance within budget or explicit waiver recorded; 5. no new dependency without license review entry (doc 11); 6. IDs preserved (no renumbering); 7. changelog line + help/annotation updated where user-visible; 8. evidence row created in doc 18; 9. registers updated if any DEC/ASM/RSK/DEP touched.

### 6.3 DoD — a release (R1) is done only when

Gate A + Gate B + staged Gate C (doc 12 §9); store prerequisites S1–S10 (doc 15 §4); claims audit (TST-VIS); benchmarks measured per doc 13; legal items resolved **or** their fallbacks executed (doc 11 §3.5); this register updated and ledger closed (doc 00).

---

## 7. Change process

**Trigger:** any change to scope, requirements, IDs, gates, or register entries.

**Change-impact template (mandatory fields):**

| Field | Content |
|---|---|
| Change | What changes (before → after) |
| Reason | Evidence or owner directive |
| IDs | FR/NFR/SCR/TST/ADR/DEC… touched; confirm none renumbered |
| UX | Screens/states affected (doc 06) |
| Data | Migration implications (doc 09) |
| Security/privacy | Doc 10 inventory/consent impact; if yes → doc 10 first then implement |
| Licensing | Doc 11 rows affected; forbidens database consulted |
| Tests | Suites added/amended (doc 12); fixtures needed |
| Claims | Copy/store impact (doc 07 §13 audit) |
| Risk | RSK updates; new RSK if applicable |
| Approvals | Owner (scope/DEC), eng lead (ADR/arch), quality lead (gates) |

**Versioning discipline:** document versions bump on material change; the 00 ledger's part log records each part of the engagement; **IDs are never reused** — deprecate with a note. "Recheck" flags (versions/policies/rates) must be re-verified at each release gate; failures to recheck block the gate, not the documentation.

**Register maintenance cadence:** updated at every release gate; full audit quarterly (per doc 15 §7); any silent scope change is a process defect (RSK-009).

---

## 8. Open questions (this doc)

None new. The register's own completeness is audited in doc 18 (gap audit), which also issues the final readiness verdict.
