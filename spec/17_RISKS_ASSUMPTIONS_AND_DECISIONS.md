# 17 — Consolidated Registers, DoR/DoD, and Change Process

**Version:** 0.5 (draft; v0.5 adds a **fourth error class — platform contracts under-read** — plus method steps **7 (read the platform contract, not just the API name)** and **8 (classify the constant: Class A invisible → inherit from the stack we chose; Class B learned interaction → follow product consensus)**; closes **Q14**; adds classification rows for PiP, seek remapping, the previous-item boundary, layout breakpoints, and default sort/density. v0.4 added step 6 — **audit the specification's constants, not only its behaviours** — and recorded invented constants as the third error class the rule exists to prevent; v0.3 added the rule and the decision-set classification; v0.2 added RSK-015 and the doc 02A question pointers)
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

### 1.3 Decision rule: industry practice first

**Rule (standing — binds every future decision).** Before a behaviour, default, interaction, or mechanism is designed from first principles, establish what shipping products in this category do and why. If practice is settled and our constraints do not conflict, **adopt it**. That is the default path, not a compromise, and it does not need further justification. If we deviate, the deviation is recorded with (a) the practice, (b) why it does not suit us, (c) who accepted it. Designing from scratch is legitimate only when practice is **absent**, **contradictory**, or **demonstrably worse** — and the entry must say which of the three applies.

**Why this rule exists.** It was adopted after the opposite approach produced three avoidable classes of error in one engagement:

1. **Auto-advance (Part 24).** The end-of-item default was set to *stop* by a reversibility argument. Research found every comparable product defaults auto-advance **ON** with a cancellable countdown, and that the one comparable which defaulted OFF reports the OFF default generates support load. The reasoning was not wrong in spirit — it applied the reversibility instinct to the wrong lever. See `research/autoplay_defaults.md`.
2. **Decoder labels (Part 26).** Q13 framed the question as technical *or* outcome language. Practice shows it is neither: every vendor pairs a technical term with a when-to-use sentence, and the one vendor shipping bare tokens carries a documented support cost. See `research/decoder_label_practice.md`.
3. **Invented constants (Part 27).** The rule had been applied to behaviour and mechanism, but not to *numbers* — which is where it is easiest to skip, because a plausible value does not feel like an invention. An audit of every constant found four invented ones (asymmetric 10 s/30 s seek steps, which turn out to be **Hulu's** ad-break pattern rather than the category default; a 3 s auto-hide timeout that overrode the documented default of the library we had already chosen; an undefined speed ladder; an unnamed sleep-timer ladder) plus one requirement that contradicted another about the same control. See `research/interface_defaults.md`.
4. **Platform contracts under-read (Part 28).** The rule had been applied to competitors and to our own library's *defaults*, but not to **the constraints the platform documents around the APIs we call**. PiP was specified as `setAutoEnterEnabled` plus an explicit path, without the documented 2.39:1–1:2.39 aspect clamp, the platform action truncation, or the source-rect/exit-transition behaviour. Layout was specified by *device* ("tablet: two-pane") where the framework documents *breakpoints by window width* — and "tablet" is not a breakpoint. Media3's documented rule that "previous" restarts the current item when position > 3 s was missing from the requirement entirely. **Reading the API name is not reading the contract.** See `research/platform_constants.md`.

**Method — how to establish "the practice" (desk-only, cheap):**
1. The vendor's **own help centre / official docs** `[V1]` — settings trees, option semantics, and, most importantly, **defaults**.
2. **Source, changelogs, prior art** `[V2]`.
3. **Reviews and forums** `[R]` — evidence of pain, never proof of behaviour.
4. Read the **option tree and its defaults**, not the marketing page.
5. **Count the vendor's own support burden.** If a vendor maintains support articles explaining its own labels, or third parties publish explainers, that is evidence *against* the pattern — not merely neutral.
6. **Audit our own constants, not just our behaviours.** The rule is easiest to skip on *numbers* — timeouts, step sizes, preset ladders, thresholds — because a plausible-looking value does not feel like an invention. Run a pass over every value in the specification (W8, Part 27: four were invented; one internally contradicted another requirement). The cheapest possible alignment is a number inherited from a stack we already chose: Media3's own `DEFAULT_SHOW_TIMEOUT_MS` is a better authority for our auto-hide timeout than any competitor's behaviour.
7. **Read the platform contract, not just the API name.** Every platform API we depend on documents constraints around its use — limits, clamps, truncation, defaults, and the *recommended* implementation. Audit those the same way as competitor defaults. Where the platform ships a **purpose-built implementation for our exact use case** (e.g. `androidx.core.pip.VideoPlaybackPictureInPicture` for video PiP), that is the sanctioned answer and hand-rolling becomes a deviation requiring justification. (W9, Part 28.)
8. **Classify the constant before inheriting or overriding it.** Two classes behave differently:
   - **Class A — invisible constants.** Timeouts, update intervals, tolerances, thresholds the user never forms an expectation about. **Inherit from the stack we already chose**; it was tested against real devices and we were not.
   - **Class B — learned interactions.** Seek increments, gesture step sizes, control placement — things the user has a **motor habit** for, learned in YouTube/VLC/Netflix rather than in our library. **Follow the product-level consensus**, even when our library defaults otherwise, and cite the consensus.
   Ask: *would a user notice this number, and would they already have an expectation about it?* This resolves what would otherwise look like inconsistency — we inherit Media3's 5 s auto-hide but deliberately do **not** inherit its 5 s/15 s seek increments.

**Deviation protocol.** A deviation is not a defect, but it must be classified and visible. Use the table below; a deviation of material cost becomes an owner decision rather than a silent choice.

**Scope note — "best suited" ≠ "most common".** Where our constraints genuinely differ (licensing, Play policy, privacy posture, no-account architecture), copying practice would be wrong. The rule requires that the *conflict be named*, not that practice always win.

#### Classification of the current decision set

Reviewed 2026-10-19 against docs 02/02A and the W1–W8 research notes. "Deviating" rows are deliberate and are rechecked at each release gate.

| Decision | What shipping products do | Our choice | Classification |
|---|---|---|---|
| DEC-001 R1 = local-file player | VLC, Just, mpv-android, Nova are all local-first | same | **Aligned** |
| DEC-003 Media3 + self-built FFmpeg **audio** ext | Just Player ships exactly this stack (Media3 + FFmpeg audio extension) `[V1]` | same | **Aligned** (existence proof) |
| DEC-004 free / no ads | VLC, Just, Nova: free, no ads, donation-funded in this niche | same | **Aligned** |
| DEC-005 phones + tablets; TV out of R1 | VLC, mpv-android, Nova and Just all ship Android TV/box builds | deferred to R1.x | **Deviating — sequenced** (input model + test-matrix discipline; not refused) |
| DEC-008 no telemetry | VLC and Just position on "no ads, tracking or permissions" | stronger (no upload path at all) | **Aligned — exceeds** |
| DEC-011 MediaStore + SAF; never `MANAGE_EXTERNAL_STORAGE` | mpv-android ships a separate `allstorage` APK using it and keeps it off Play; Nova's all-files history is the cautionary case | never request it | **Deviating — justified** (Play policy + documented incumbent cost) |
| DEC-012 no `INTERNET` permission in R1 | VLC/Just include it for network playback | omitted entirely | **Deviating — deliberate differentiator** (verifiable trust proof) |
| DEC-013 store-only at R1; no sideload channel | This niche ships multi-channel (Play + F-Droid + GitHub + IzzyOnDroid + others) | Play only at R1 | **Deviating — sequenced** (DEC-009 defers the FOSS build; revisit at R1.x, see RSK-005) |
| FR-CTRL-017 end-of-item default | Auto-advance **ON** with cancellable countdown, scoped to a queue | adopted (Part 24) | **Aligned** (was deviating) |
| FR-LIB-015 resume semantics | Tri-state documented by Just Player; VLC ships an equivalent "Resume played videos" three-option setting `[V1]` | tri-state, Always default | **Aligned** |
| FR-PLAY-003 decoder labelling | Technical term **+** when-to-use description; `Auto` default; global setting + per-item override | adopted (Part 26) | **Aligned** (was open) |
| FR-PLAY-003 bounded fallback + restore | VLC does exactly this — one automatic fallback, previous setting restored | same | **Aligned** |
| File delete of user media | Just Player documents *delete file* as a post-playback action `[V1]` | R2 only; R1 is non-destructive | **Deviating — justified** (destructive capability deferred; R1 keeps "remove from library"; the ENDED surface keeps the action reachable per FR-CTRL-017) |
| FR-LIB-011 article-ignoring sort | Nova documents it | adopted, off by default | **Aligned** |
| FR-CTRL-001 auto-hide timeout | Media3 documents **5 s** (`DEFAULT_SHOW_TIMEOUT_MS`); other players do not publish theirs | now 5 s | **Aligned** (was deviating — Part 27) |
| FR-CTRL-002 seek steps | Symmetric **±10 s** default (VLC, YouTube, Netflix, Disney+); preset ladder 5/10/15/20/30/60 s; multi-tap accumulation | adopted (Part 27) | **Aligned** (was deviating). **Class B — learned interaction.** Note: Media3's *own* defaults are asymmetric (5 s back / 15 s forward), so asymmetry is not itself anomalous — what made our old 10/30 pairing Hulu's was the **1:3 ratio** (Part 28) |
| FR-CTRL-002 previous-item boundary | Media3 documents `DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS = 3000`: "previous" restarts the current item past 3 s, jumps to the previous item within 3 s — near-universal media behaviour | adopted (Part 28) | **Aligned** (was missing entirely) |
| FR-PLAY-005 seek remapping | Media3 exposes `SeekParameters` with `toleranceBeforeUs`/`toleranceAfterUs`; documents the trade-off ("sync points faster but less accurate") | adopted as the mechanism; bespoke remap prose dropped | **Aligned** (was bespoke) |
| FR-PLAT-001 PiP | Platform ships a video-purpose-built implementation (`androidx.core.pip.VideoPlaybackPictureInPicture`); documents aspect clamp 2.39:1–1:2.39, action truncation, source-rect reuse on Android 12+ | adopted; hand-rolled params rejected | **Aligned — platform-sanctioned** |
| Layout & navigation component | M3 documents 600/840/1200/1600 with a navigation component per class; Android documents list-detail pane counts and state-preservation rules | adopted (Part 28) | **Aligned** (was device-based: "Tablet: two-pane") |
| FR-LIB-011 default sort | Platform **contradicts itself**: legacy `MediaStore.Video.Media.DEFAULT_SORT_ORDER` = `"title"`; modern `QUERY_ARG_MEDIA_STANDARD_SORT_ORDER` = `DATE_ADDED` **descending** | most-recently-added first — the modern path we actually query — with the contradicting constant recorded | **Deviating — justified** (Q14 closed, Part 28) |
| FR-LIB-011 default view density | M3 canonical **feed** layout: 1 column of cards at compact, 2 at medium, 3 at expanded, 4 at large | adopted | **Aligned** |
| FR-CTRL-004 playback speed | YouTube documents the 0.25–2× ladder; Netflix caps at 1.5× and resets per title | ladder adopted; our custom range runs to 4× | **Aligned — deliberately wider** (no creator-relations constraint applies to a local-file player; YouTube's own blog records user requests for 3–4×) |
| FR-CTRL-009 sleep timer | YouTube: 10/15/20/30/45/60 min + end-of-item, with an **extend prompt** on expiry | adopted (Part 27) | **Aligned** (the ladder was missing) |
| ~~Default library sort order / view density~~ | Was **contradictory** across vendors — and the platform's own two APIs disagree with each other as well | **Resolved (Part 28):** most-recently-added first; M3 canonical feed column counts by breakpoint | **Closed** — see the two FR-LIB-011 rows above |

**Reading of the table.** Every *how* decision, every audited constant, and every platform contract we depend on is aligned, openly justified, or explicitly named as a deviation. The deviations are **scope sequencing** (TV, distribution, destructive file ops) or a **documented platform self-contradiction we resolved deliberately** (default sort order), rather than behaviour — so no deviation requires an owner decision at this time; each is sequenced in docs 15/16 or recorded above. After Part 28 **no item in the decision set is recorded as unsettled.** That is not a claim that the specification is complete — it is the narrower claim that the **desk-research class of uncertainty is exhausted**: what remains (gesture swipe tuning, scrubber-preview behaviour, grid-density feel, resume thresholds, PiP across OEM skins, foldable breakpoint behaviour) is **empirical**, and only device and beta work can settle it (doc 17 §5; doc 18 Spec Gates).

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

Full sets: doc 04 Q1–Q14 (Q12, Q13, Q14 closed) · doc 05 NQ1–NQ6 · doc 06 U1–U6 · doc 07 C1–C6 · doc 08 T1–T5 · doc 09 A1–A6 · doc 10 S1–S6 · doc 11 OQ-1…5 · doc 12 Q12-1…6 · doc 13 Q13-1…6 · doc 14 Q14-1…5 · doc 15 OQ15-1…5 · doc 16 Q16-1…5 · doc 02A CS-1…CS-7.

**Behavioural questions raised by W5 that are *not* owner decisions** (they need evidence, not preference): **CS-1, CS-2, CS-3, CS-5, CS-6, CS-7** (doc 02A §14). **Q12 and Q13 are both closed** — auto-advance was settled against six shipping products (`research/autoplay_defaults.md`, FR-CTRL-017), and decoder labelling against five (`research/decoder_label_practice.md`, FR-PLAY-003, which also resolves CS-4). Neither required an owner decision, because in both cases practice was settled and our constraints did not conflict (§1.3). **Q14 is also closed (Part 28)** — not by a consensus that does not exist, but by following the modern MediaStore path we actually query plus Material's canonical feed layout, with the contradicting legacy constant recorded rather than hidden. What remains open (CS-1/2/3/5/6/7, U2–U6, and the empirical items in doc 18) needs **device, beta, or brand input** — not more desk research.

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
