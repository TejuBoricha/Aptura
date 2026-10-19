# 00 — Index and Status
**Project:** Next-Generation Mobile Video Player — Product & Technical Specification
**Working title:** TBD (deliberately not chosen during specification phase)
**Package version:** 0.1 — Parts 1–26 delivered (package complete; verdict in doc 18; continuation guide: `HANDOFF.md`; W5 deep case studies in `02A`; version-controlled since Part 25; industry-practice-first decision rule in 17 §1.3)
**Last updated:** 2026-10-19
**Overall status:** All phases complete (0–6). Final audit issued — **verdict: READY FOR TECHNICAL SPIKES** (doc 18 §7). Owner decisions OD-1…OD-6 pending; defaults active.
**Hard constraints (honored):** Documentation only. No application source code, no repository scaffolding, no dependency installation, no prototype. No feature is claimed to work; no benchmark is claimed to have been measured; no device is claimed to have been tested. Claims are labeled by evidence class in every document.
**Resuming work?** Start with `HANDOFF.md` — the done/pending inventory and continuation rules for a new session.
---

## 1. How to use this package
| Doc | File | Purpose | Status | Ver |
|---|---|---|---|---|
| 00 | 00_INDEX_AND_STATUS.md | Navigation, completion ledger, required first response, ID conventions, change rules | Draft | 0.1 |
| 01 | 01_PRODUCT_VISION_AND_SCOPE.md | Vision, product-boundary decision, in/out/future scope, platform targets | Draft (W1–W4 evidence applied) | 0.1 |
| 02 | 02_MARKET_AND_COMPETITOR_RESEARCH.md | Research methodology, competitor matrix, profiles, SWOT, pain-point synthesis, opportunity ranking | Draft v0.2 (W1+W4 integrated: iOS set, monetization anchors) | 0.2 |
| 02A | 02A_COMPETITOR_CASE_STUDIES.md | **Deep competitor case studies (W5):** per-app feature/option catalogues, settings trees, player UI anatomy, gesture maps, decode/subtitle internals, design implications | Draft v0.3 (10 apps; findings folded into doc 04 v0.5; CS-4 resolved via W7) | 0.3 |
| 03 | 03_PERSONAS_JOURNEYS_AND_STRATEGY.md | Personas, JTBD, journeys, positioning, differentiation hypotheses, KPI framework | Draft (W1 evidence applied; W4 pending) | 0.1 |
| 04 | 04_PRD_AND_FUNCTIONAL_REQUIREMENTS.md | Full functional requirement catalogue (FR-*) with acceptance criteria | Draft v0.5 (Parts A–E: all FR domains; W5 findings; Q12 closed via W6; Q13 closed via W7) | 0.5 |
| 05 | 05_NON_FUNCTIONAL_REQUIREMENTS.md | Measurable NFRs (NFR-*): performance, reliability, resources, compatibility, accessibility | Draft v0.2 (64 NFRs; targets provisional; auto-advance clause) | 0.2 |
| 06 | 06_UX_SCREEN_AND_STATE_SPECIFICATION.md | Screen/state inventory, IA, navigation, design system, accessibility, wireframe descriptions | Draft v0.3 (SCR-001…018 + SCR-010f/g/h, state catalogue, player UX, decoder copy) | 0.3 |
| 07 | 07_MEDIA_COMPATIBILITY.md | Container/codec/audio/subtitle/HDR matrix with verification categories | Draft v0.2 (A–X tiers; competitor-claims discipline) | 0.2 |
| 08 | 08_TECHNOLOGY_EVALUATION_AND_ADRS.md | Stack and media-engine evaluation, scoring, ADRs, spike definitions | Draft v0.2 (ADRs 001–013; spike program; §6.1 prior art) | 0.2 |
| 09 | 09_ARCHITECTURE_AND_DATA.md | System architecture, modules, threading, data model, backup/migration | Draft v0.2 (modules, data model, session/scan design, queue orchestration, no-backend rationale) | 0.2 |
| 10 | 10_SECURITY_PRIVACY_AND_COMPLIANCE.md | Threat model, privacy data inventory, compliance review items | Draft v0.1 (THR register, data inventory, compliance register) | 0.1 |
| 11 | 11_LICENSING_AND_DEPENDENCIES.md | License/patent register per component, SBOM and attribution plan | Draft v0.1 (component + patent registers, LGPL process, SBOM plan) | 0.1 |
| 12 | 12_QA_AND_TEST_STRATEGY.md | Risk-based test program, device/media matrix, fixtures, exit gates | Draft v0.2 (TST suite map incl. TST-QUEUE, fixture corpus, device matrix, Gates A–C) | 0.2 |
| 13 | 13_PERFORMANCE_BENCHMARKING.md | Measurement definitions, benchmark protocol, provisional targets | Draft v0.1 (MB-01…12 methods, statistics, claim policy) | 0.1 |
| 14 | 14_ANALYTICS_AND_SUPPORT.md | Diagnostics, telemetry (privacy-first), support workflow, feedback | Draft v0.1 (local diagnostics, EV registry, report bundle, support flow) | 0.1 |
| 15 | 15_MONETIZATION_AND_OPERATIONS.md | Monetization hypotheses, store/release/ops, distribution plan | Draft v0.1 (options A–E, launch prereqs, rollout pipeline, maintenance) | 0.1 |
| 16 | 16_ROADMAP_AND_ESTIMATES.md | Phased roadmap, MoSCoW/RICE prioritization, roles, sizing ranges | Draft v0.1 (trains, B0–B7 sizes, DEP register, de-scope ladder) | 0.1 |
| 17 | 17_RISKS_ASSUMPTIONS_AND_DECISIONS.md | Canonical registers: DEC/ASM/RSK/DEP, DoR/DoD, change template | Draft v0.3 (registers consolidated; **§1.3 industry-practice-first rule + decision-set classification**; change process; question index) | 0.3 |
| 18 | 18_TRACEABILITY_AND_READINESS_CHECKLIST.md | Requirements→architecture→test traceability, quality gates A/B/C, readiness verdict | Draft v0.2 — **final deliverable** (traceability, §22 audit, §23 Spec Gates A–C + post-audit delta, §21 failure review, verdict) | 0.2 |
| — | research/*.md | Raw, sourced research notes by workstream (W1–W7) | W1–W7 ✅ complete; synthesis incorporated into docs 02/07/11/15 and 04 (W6 → FR-CTRL-017; W7 → FR-PLAY-003) | 0.1 |
| — | HANDOFF.md | Continuation guide for a new session: done / pending inventory, resume rules, next-action checklist, file map | Current (2026-10-19) | 0.1 |
Reading order for a new team member or AI implementer: **00 → 01 → 17 → 04 → 05 → 08 → 09**. Every downstream document must preserve the IDs defined here and in 17.
---

## 2. Completion ledger
| Phase (master prompt §24) | Deliverables | Status | Part |
|---|---|---|---|
| Phase 0 — Required first response | §3 of this document | ✅ Complete | 1 |
| Phase 1 — Research and evidence collection | `research/` notes; doc 02; feeds 01/07/08/11/15 | ✅ W1–W4 complete; synthesized into docs 02/07/11/15 | 1–5 |
| Phase 2 — Product boundary, user needs, differentiation | Doc 01 (validate), doc 03 | ✅ 01, 03 drafted | 1–2 |
| Phase 3 — Functional and non-functional requirements | Docs 04, 05, 07 | ✅ 04 v0.2, 05 v0.1, 07 v0.1 drafted | 7 |
| Phase 4 — UX, states, architecture, compatibility, data, privacy, licensing | Docs 06, 08, 09, 10, 11 | ✅ 06, 08–11 drafted (v0.1) | 12 |
| Phase 5 — QA, benchmarking, release ops, monetization, roadmap | Docs 12, 13, 14, 15, 16 | ✅ 12–16 drafted (v0.1) | 17 |
| Phase 6 — Traceability, adversarial review, readiness verdict | Docs 17, 18; register consolidation | ✅ 17–18 drafted; audit + readiness verdict issued | 19 |

The ledger is updated after every part. No required deliverable will be silently omitted; anything deferred is listed here as Not started.
---

## 3. Required first response (master prompt §24)

### 3.1 Research plan and source strategy

**Source hierarchy (primary first):**
1. **Primary/official:** Google for Developers (Media3/ExoPlayer docs, storage & permissions, CDD/CTS, Play policy center), Apple Developer (AVFoundation/AVPlayer format docs, App Store Review Guidelines, privacy details), project documentation and license texts (VideoLAN, FFmpeg legal page, mpv, Media3 GitHub, libass), standards bodies (ISO/IEC for containers where accessible via summaries, W3C, IETF), patent pools (Access Advance, Via LA), regulator/statute texts (GDPR, CCPA/CPRA, India DPDP Act, EU EAA).
2. **Store listings:** Google Play and Apple App Store product pages (install ranges, ratings, update dates, data-safety/privacy labels). Treated as marketing + metadata, not performance proof.
3. **Reputable secondary:** established tech analysis (Ars Technica, Android Authority, XDA), maintainer release notes, GitHub issues/discussions for real-world defect signals.
4. **User-generated signals:** app reviews, Reddit, forums — used only as *signals* of pain points, labeled `[R]`, never as proof of defects or benchmarks.

**Evidence labels used throughout the package:**
- `[V1]` verified from a primary source, `[V2]` verified from a reliable secondary source, `[R]` reported user experience, `[I]` our inference, `[E]` our estimate, `[U]` unknown/unverified.
- Every material claim carries a URL and access date (2026-10-18 unless otherwise stated). Claims that may change (versions, policies, prices) carry a "recheck before release" flag.

**Workstreams (running in parallel; raw notes in `research/`):**
| WS | Topic | Output | Status |
|---|---|---|---|
| W1 | Competitor deep-dive: VLC, MX Player, Just Player, mpv-based players, other Android players | `research/competitors.md` → doc 02 | ✅ complete 2026-10-18 (244 lines) |
| W2 | Platform capabilities and policies: Android (Media3, codecs, storage, Play policies), iOS (AVFoundation, review guidelines) | `research/platform.md` → docs 07, 08, 10 | ✅ complete 2026-10-19 (208 lines) |
| W3 | Licensing and patents: FFmpeg/libVLC/Media3/mpv licenses; HEVC/AVC/AAC/AV1/Dolby patent exposure; FFmpegKit retirement implications | `research/licensing.md` → doc 11 | ✅ complete 2026-10-19 (228 lines) |
| W4 | Market, monetization, compliance: install bases, regional patterns, competitor pricing, Play/Apple billing rules, privacy law basics | `research/market_compliance.md` → docs 02, 15, 10 | ✅ complete 2026-10-19 (263 lines) |

**Explicit non-actions this phase:** no app code, no scaffolding, no dependency installation, no benchmarks claimed, no device testing claimed. Competitor "compatibility" will be recorded as *documented/claimed/reported*, never as "tested by us".

### 3.2 Initial assumptions and unknowns
All are provisional until validated in Phase 1–2; canonical register is doc 17.
| ID | Assumption | Validation method | Status |
|---|---|---|---|
| ASM-001 | Primary users are individuals playing **personal local video files** (downloaded, recorded, or copied), not streamers of paid services | Competitor review mining (W1); personas (03) | Provisional |
| ASM-002 | **Android-first** release; iOS as a later, evidence-gated decision (built-in player is stronger on iOS; custom-engine cost is higher) | Platform capability + market research (W2, W4); DEC-002 | Provisional |
| ASM-003 | Product is **local-first, no account, no mandatory backend**; optional network playback is deferred beyond MVP | Scope decision DEC-001; backend necessity analysis in 09 | Provisional |
| ASM-004 | MVP monetization is **free, no ads**; later paid unlock hypotheses only (nothing implemented in spec phase) | Monetization analysis (W4, doc 15) | Provisional |
| ASM-005 | Small senior team (≈2–4 engineers + part-time design/QA); affects MVP scope and estimates | Owner confirmation (OD-4) | Unvalidated |
| ASM-006 | Timeline and budget are unknown; roadmap gives phase-relative sizing ranges only | Owner input; doc 16 | Unvalidated |
| ASM-007 | Default shipping configuration uses **platform decoders + royalty-free software decoders only**; anything with unclear royalty exposure is excluded or explicitly flagged | Licensing register (doc 11, W3) | Provisional |
| ASM-008 | Incumbent pain is concentrated in: intrusive ads/privacy trust, playback reliability edge cases, subtitle fidelity/UX, large-library/scan friction, low-end performance, and bloat/clutter | Pain-point synthesis (doc 02, W1) | Provisional |
| ASM-009 | Launch regions prioritize markets where local-playback apps are heavily used (e.g., India, Brazil, SE Asia, MENA, plus US/EU); localization starts with English + a small set | Market research (W4); owner confirmation (OD-5) | Provisional |
| ASM-010 | No DRM/protected-playback support in scope (documented boundary; graceful, honest failure UI) | Scope decision DEC-006 | Provisional |

**Key unknowns:** Android vs iOS first-release economics; true size of the "privacy-respecting, no-ads, reliable player" willingness-to-pay segment; AV1/HEVC 10-bit decode coverage on the low-end Android devices we must support; realistic one-time-unlock vs donation conversion; regional policy nuances at launch.

### 3.3 Most consequential product-boundary decisions (recommendations — owner approval requested)
Master prompt §4 requires an explicit boundary choice among product types A–E. **Recommendation: type A now, limited type B later — explicitly not C/D/E.**
| ID | Decision (recommended) | Rationale (summary; full rationale in doc 01/02) | Owner approval |
|---|---|---|---|
| DEC-001 | **R1 scope = A (local-file player) + selector/library polish. B (SMB/NAS/direct HTTP(S) streams) = R2 candidate. C/D/E = out of scope.** | Local playback is the addressable, evidence-strong pain area; streaming-client features are a different product with permanent maintenance/legal cost | OD-2 |
| DEC-002 | **Android-first for R1; iOS evaluated at an R1.x gate** against explicit criteria (cost, differentiation, codec constraints) | Android media-player market is larger, incumbents are more disliked, and a custom engine is feasible; iOS holds strong paid incumbents and stricter playback limits | OD-1 |
| DEC-003 | **Playback engine hypothesis: Media3/ExoPlayer core + self-built LGPL-2.1 FFmpeg extension for *audio* decode; libmpv/libVLC as fallback candidates only if the spike FAILs; no bundled software *video* decode in R1** | Media3 is Apache-2.0, actively maintained, integrates with Android platform; avoids GPL contamination of naive FFmpeg builds. W3 refinement: Media3's FFmpeg extension is **audio-only** and not on Maven; video fallback stays on platform (hw/platform-sw) decoders because bundled video software decode carries unresolved royalty exposure ⚖️; decision finalized by tech eval (doc 08) + spike definitions (not executed this phase) | — |
| DEC-004 | **Free, local-first, no account, no ads at launch; monetization hypotheses validated later** | Trust differentiation; avoids billing/compliance complexity in MVP | OD-3 |
| DEC-005 | **Targets: Android phones + tablets from R1; Android TV, foldable-specific UX, car, wearables excluded from R1; minimum OS version recommended in doc 01 (pending W2 evidence)** | Device tiers define test matrix and performance budget; TVs are a different input model | — |
| DEC-006 | **No DRM/protected-content playback; unsupported cases fail with honest explanation** | Keeps scope, licensing, and security surface small | — |
| DEC-007 | **No AI features in R1; any AI feature in R3+ must be on-device-first, opt-in, and never silently upload media** | Master prompt §7.7; privacy positioning | — |
| DEC-008 | **Telemetry: none by default beyond what the store requires; optional, explicit-consent crash diagnostics; local-only diagnostics by default** | Privacy product principle; doc 10/14 detail | — |
| DEC-009 | **All-GPL component linkage avoided in the store build (LGPL-clean build)**; FOSS/GPL distribution channel (e.g., F-Droid) deferred and would be a separate build decision | Prevents incompatible obligations; keeps commercial options open | — |
| DEC-010 | **Project will not promise universal format/device support anywhere (store copy, docs, UI)** | Master prompt §2.5; compatibility matrix defines the honest contract | — |

### 3.4 Highest-risk technical questions (to investigate; spikes defined in docs 08/16 — not executed this phase)

| ID | Question | Why it is high-risk |
|---|---|---|
| RQ-01 | Can a Media3(+FFmpeg ext) stack match libVLC/mpv on the mainstream personal-library corpus (MKV, H.264/HEVC 8/10-bit, AAC/AC3 pass-through, VFR, odd timestamps) with hw-first + sw fallback and no unacceptable failures? | Determines engine choice and the whole differentiator promise |
| RQ-02 | What is the royalty-safe shipping configuration (FFmpeg LGPL build, decoder set, container/app-size cost) and does FFmpegKit's retirement force a self-built binary toolchain? | Licensing exposure + build effort |
| RQ-03 | HEVC 10-bit and AV1 hardware decode coverage on target low-end/mid Android devices; behavior when only software decode exists (battery/thermal budget) | Compatibility claims and performance NFRs depend on this |
| RQ-04 | Dolby (AC-3/E-AC-3/Dolby Vision) handling: platform pass-through vs. unsupported vs. licensed decode — what is lawfully and technically feasible? | Common in downloaded content; licensing minefield |
| RQ-05 | Scoped-storage/SAF performance for 10k+ file libraries on slow SD cards: scan latency, incremental rescan, thumbnails without battery damage | Core library UX; NFR targets |
| RQ-06 | ASS/SSA subtitle fidelity (libass-class rendering) inside a Media3 pipeline: feasible within budget? | Power-user differentiator; affects engine plumbing |
| RQ-07 | Process-death/state-restoration correctness across playback, scanning, and permission changes on Android 12–16 behavior changes | Reliability NFRs; user-visible quality |
| RQ-08 | One-time unlock monetization *without* a backend/account: Play Billing offline behavior, restore flows, regional pricing | Monetization hypothesis viability |
| RQ-09 | iOS viability gate: what fraction of target experience is achievable with AVPlayer only, and the true cost of bundled custom decode (licensing + size + review risk)? | DEC-002 depends on this |
| RQ-10 | Privacy-preserving diagnostics: what can be collected lawfully/consentfully that still resolves real playback failures (samples? decoder trace?) without media content | Doc 14 viability; trust promise |
| RQ-11 | Large-library DB + thumbnail architecture (Room/SQLite vs alternatives; MediaMetadataRetriever vs codec-based thumbs) under low-page-cache constraints | Performance at scale |
| RQ-12 | Variable frame rate, HDR tone-mapping fallback on SDR displays, refresh-rate switching — what does the platform give for free vs. what must we build? | Playback quality promise |
Each spike gets hypotheses, pass/fail thresholds, test conditions, and a decision that follows (docs 08 §spikes, 16 §Phase 2).

### 3.5 Deliverable checklist and proposed document structure
All 24 required deliverables (master prompt §22) map to the 19 documents above plus the live registers:
| # | Required deliverable | Lives in |
|---|---|---|
| 1 | Executive summary, vision, scope, exclusions | 01 (+00 summary) |
| 2 | Research methodology, sources, competitor analysis | 02 |
| 3 | Personas, JTBD, journeys | 03 |
| 4 | Positioning, differentiation hypotheses, validation plan | 03 |
| 5 | PRD + functional requirements catalogue | 04 |
| 6 | NFRs with measurable targets | 05 |
| 7 | Screen inventory, navigation, state catalogue, UX spec | 06 |
| 8 | Media compatibility matrix | 07 |
| 9 | Technology evaluation, recommended stack | 08 |
| 10 | Architecture, modules, diagrams, ADRs | 08 (ADRs) + 09 |
| 11 | Data model, persistence, backup, migration | 09 |
| 12 | Threat model + privacy data inventory | 10 |
| 13 | License/codec/patent register | 11 |
| 14 | Monetization & commercial viability | 15 |
| 15 | Backend decision (or documented no-backend rationale) | 09 (+15) |
| 16 | QA strategy, test plan, matrix, acceptance criteria | 12 |
| 17 | Performance/battery/thermal benchmark protocol | 13 |
| 18 | Diagnostics, telemetry, support, feedback plan | 14 |
| 19 | Store compliance, release, rollout, maintenance | 15 |
| 20 | Roadmap, dependencies, roles, estimates | 16 |
| 21 | Risk/assumption/dependency/decision registers | 17 (initial entries in 00 §3) |
| 22 | Requirements-to-tests traceability matrix | 18 |
| 23 | Definition of Ready/Done, release gates | 17 (+18) |
| 24 | Final gap audit, open questions, residual risks | 18 |

---
## 4. ID conventions (frozen)

| Prefix | Meaning | Example |
|---|---|---|
| FR-`<DOMAIN>`-`###` | Functional requirement. Domains: LIB, PLAY, CTRL, SUB, AUD, NET, PERS, SET, DIAG, PLAT | FR-PLAY-001 |
| NFR-`<DOMAIN>`-`###` | Non-functional requirement. Domains: PERF, REL, RES, COMP, A11Y, SEC, PRIV, LOC, OPS | NFR-PERF-001 |
| SCR-`###` | Screen/state | SCR-020 |
| PER-`#` / JRN-`##` | Persona / user journey | PER-1 / JRN-03 |
| KPI-`<AREA>`-`##` | KPI (ACQ, ACT, REL, RET, SAT, PERF, SUP, BIZ) | KPI-REL-01 |
| DEC-`###`, ASM-`###`, RSK-`###`, DEP-`###` | Decision / assumption / risk / dependency | DEC-001 |
| ADR-`###` | Architecture decision record | ADR-002 |
| RQ-`##` | High-risk question (spikes) | RQ-01 |
| TST-`###` | Test case | TST-042 |
| FC-`###` / THR-`###` | Failure case / threat | FC-001 / THR-003 |
| EV-`###` | Telemetry/diagnostic event | EV-001 |
| R1 / R1.x / R2 / R3 | Release train (MVP / patch / network+platform expansion / advanced experiments) | R1 |

---
## 5. Owner decisions requested (materially change scope, cost, or risk)

| ID | Question | Options | Default if no answer |
|---|---|---|---|
| OD-1 | Platform order: Android-first vs. both-platforms-from-day-one | Android-first (recommended) / both / iOS-first | DEC-002 recommendation |
| OD-2 | R1 boundary: local-only vs. include network playback (SMB/HTTP) in R1 | Local-only (recommended) / include network | DEC-001 recommendation |
| OD-3 | Monetization intent at launch: free/no-ads vs. paid unlock at launch | Free (recommended) / paid unlock / undecided→hypotheses only | DEC-004 recommendation |
| OD-4 | Team, budget, and time constraints for estimation calibration | (free-form) | Ranges labeled `[E]` in doc 16 |
| OD-5 | Launch regions and languages | (free-form multi-select) | ASM-009 provisional set |
| OD-6 | Product/brand name | (free-form) | "working title TBD" kept |

These are the only questions worth interrupting for; everything else proceeds with documented assumptions.
---

## 6. Change rules (summary; full process in doc 17)
- **Industry practice first — standing rule (17 §1.3).** Before designing a behaviour, default, interaction, or mechanism from first principles, establish what shipping products in this category do and why. If practice is settled and our constraints do not conflict, **adopt it**. Deviations are recorded with (a) the practice, (b) why it does not suit us, (c) who accepted it — see the classification table in 17 §1.3.
- Every consequential change triggers a change-impact assessment: requirements touched, UX, data migration, security/privacy, platform restrictions, performance, tests, licensing, release risk, docs.
- Requirement IDs, once published, are never silently repurposed; deprecate with a note instead.
- Statuses in this ledger are updated after each part; "complete" means all of the doc's mandated sections exist with sources.
---

## 7. Part log

| Part | Date | Contents |
|---|---|---|
| 1 | 2026-10-18 | §3 required first response; docs 00 + 01 (draft); launched research workstreams W1–W4 |
| 2 | 2026-10-18 | Doc 03 draft (personas PER-1…5, JTBD-01…12, journeys JRN-01…08, differentiation hypotheses DH-1…7, KPI framework); research W1–W4 still running |
| 3 | 2026-10-19 | Doc 04 Part A (conventions + FR-LIB-001…023, FR-PLAY-001…020 with full records); research W1–W4 still running |
| 4 | 2026-10-19 | Doc 02 v0.1 (W1 competitor research synthesized: matrix, profiles, SWOT, pain points, opportunity ranking, claims to avoid); evidence updates to docs 01 (§3.1) and 03 (§4.1, +DH-8); W2–W4 still running |
| 5 | 2026-10-19 | Doc 04 v0.2 completed — Part B (FR-CTRL-001…016, FR-SUB-001…010, FR-AUD-001…008, FR-PLAT-001…006, FR-SET-001…008, FR-DIAG-001…005, FR-NET/FR-PERS boundaries) + W3/W4 refinements (audio-only FFmpeg ext, no bundled video sw-decode, launch-ops constraints); all W1–W4 research complete |
| 6 | 2026-10-19 | Doc 02 v0.2 (iOS snapshot §2.4, monetization anchors §8.1, W4 market synthesis); doc 05 v0.1 (64 NFRs across PERF/REL/RES/COMP/A11Y/SEC/PRIV/LOC/OPS; device classes D1–D3; all targets provisional pending doc 13) |
| 7 | 2026-10-19 | Doc 07 v0.1 (compatibility matrix: A Commit / B Expected / C Best-effort / D Unsupported / X Blocked-legal tiers across containers, video, audio, subtitles, HDR; promotion pipeline via 12/13/18; claims discipline for doc 15) |
| 8 | 2026-10-19 | Doc 08 v0.1 (stack evaluation: Media3 core + LGPL audio ext confirmed provisionally; ADR-001…013 incl. no-telemetry-SDK, minSdk 26, no-network R1, packaged flags; spike program RQ-01…14 with pass/fail definitions) |
| 9 | 2026-10-19 | Doc 09 v0.1 (architecture: module map + dependency rules, threading model, library data model with derived-vs-user data split, identity/reconciliation strategy, scan pipeline, playback session state machine, backup decisions, documented no-backend rationale) |
| 10 | 2026-10-19 | Doc 10 v0.1 (security & privacy: THR-001…015 threat register, authoritative data inventory + permission matrix, compliance register for Play + GDPR/CCPA/DPDP/LGPD/COPPA/EAA scope notes, incident response without a server) |
| 11 | 2026-10-19 | Doc 11 v0.1 (license register: component verdicts incl. LGPL FFmpeg audio ext ⚖️; forbiddens: GPL/mpv, FFmpegKit, ProRes, runtime downloads; codec patent register with platform-vs-bundled exposure logic; LGPL §6 process; SBOM plan; legal backlog L1–L8) |
| 12 | 2026-10-19 | Doc 06 v0.1 (UX: IA, SCR-001…018 screen inventory, state catalogue, design system tokens, player UX + gesture map, copy/honesty rules, accessibility spec, 8 wireflows, text wireframes); **Phase 4 complete** |
| 13 | 2026-10-19 | Doc 12 v0.1 (QA: suite map resolving all TST-* refs, fixture corpus plan, device matrix, specialized programs, CI policy, Gates A–C, severity model, metrics) |
| 14 | 2026-10-19 | Doc 13 v0.1 (benchmarking: MB-01…12 metric methods per class, M1–M10 principles, statistical protocol + regression gates, claim policy tying measured evidence to store wording) |
| 15 | 2026-10-19 | Doc 14 v0.1 (diagnostics/support: no-telemetry reality + blind-spot compensation, local diagnostics design, EV-001…020 registry, report-bundle spec with redaction preview, support workflow, preconditions for ever adding upload) |
| 16 | 2026-10-19 | Doc 15 v0.1 (monetization hypotheses A–E with gates; store ops prereqs S1–S10 incl. developer verification + closed-testing; staged rollout + halt/rollback playbooks; maintenance model; flat-cost posture) |
| 17 | 2026-10-19 | Doc 16 v0.1 (roadmap: release trains R1–R3, build phases B0–B7 with [E] dev-week ranges, DEP register, roles, de-scope ladder, gated R1.x/R2 stack); **Phase 5 complete** |
| 18 | 2026-10-19 | Doc 17 v0.1 (registers consolidated: DEC-001…013, ASM-001…013, RSK-001…014, DEP-001…012, ADR pointer table; DoR/DoD for feature and release; change-impact template + versioning discipline) |
| 19 | 2026-10-19 | Doc 18 v0.1 — **final deliverable** (traceability matrices, §22 checklist audit, Spec Gates A–C review with findings C-1…C-10, §21 failure-mode review, residual risks; **verdict: READY FOR TECHNICAL SPIKES**); consistency fixes landed in docs 12/15; **package complete** |
| 20 | 2026-10-19 | Session handoff: `HANDOFF.md` added (done/pending inventory incl. OD/L/RSK/spike/store/artifact lists, resume rules, next-action checklist, file map, housekeeping notes); 00 ledger amended with handoff pointer; **known pending revision recorded: doc 03 W4 evidence pass → v0.2** |
| 21 | 2026-10-19 | **W5 deep competitor case studies** — `02A_COMPETITOR_CASE_STUDIES.md` v0.1: 10 apps (VLC, MX/Amazon MX, Just Player, mpv-android, Nova, KMPlayer, Next Player, mpvKt, Infuse, nPlayer, Outplayer), per-app feature + settings catalogues, player UI anatomy, gesture maps, decode/subtitle internals, cross-app pattern tables, 14 adopt / 8 reject design implications, explicit non-establishable limits, 7 new open questions (CS-1…CS-7). Doc 02 cross-referenced. Ledger + file table updated. |
| 22 | 2026-10-19 | **W5 findings folded into the requirements** (doc 04 → v0.3, doc 06, doc 12, doc 18): **new FR-CTRL-017** (play modes + defined end-of-item behaviour — previously an actual gap: nothing specified what happens at `ENDED`); FR-PLAY-003 fallback boundedness + persistent compatibility notice; FR-LIB-015 tri-state resume (Always/Never/Ask); FR-LIB-011 article-ignoring sort (off by default); FR-LIB-004 scan-phase feedback; FR-SUB-001 extra subtitle folder; FR-SUB-004 calibration aid; FR-PLAY-011 Auto aspect. UX: SCR-010f/g/h + Ended/Compatibility states. QA: TST-QUEUE suite. Doc 18 FR-CTRL count 16→17. New questions Q12 (auto-advance default), Q13 (decoder label language). |
| 23 | 2026-10-19 | **Propagation pass — closing the loop so FR-CTRL-017 is not orphaned.** Doc 09 v0.2: `ENDED` documented as a decision point (not terminal), queue/play-mode orchestration assigned to the `playback` module outside the engine adapter, auto-advance guarded to foreground, completion written before next prepare, A5 updated (folder/queue modes re-derivable → no persistence needed). Doc 05 v0.2: NFR-PERF-003 auto-advance clause (reuses live session; 1.5× budget on config change). Doc 18 v0.2: Gate C **post-audit delta review** (D-1…D-9) against doc 04 v0.3, verdict unchanged; FR-CTRL suite list gains QUEUE. Doc 17 v0.2: **RSK-015** (thin evidence for end-of-item default), question index extended to Q13 + CS-1…CS-7. Doc 07 v0.2 §13: competitor format lists explicitly refused as evidence (container ≠ codec; decoder path ≠ format name). Doc 08 v0.2 §6.1: prior-art notes for RQ-01/03/05/06/12. |
| 24 | 2026-10-19 | **Q12 re-decided against industry practice — the earlier stop-at-end default was wrong.** New workstream **W6** `research/autoplay_defaults.md` (8 products documented, 24 sourced URLs): every comparable defaults auto-advance **ON** when a next item exists — Netflix, YouTube (adults; OFF for 13–17), Plex, Emby, Infuse ("Continuous Playback"), MX Player ("Playalone") — and the one comparable that defaulted OFF (Swiftfin) reports the OFF default caused "half the 'can you make it play the next one' requests". FR-CTRL-017 rewritten: default **ON with a 10 s cancellable countdown** (Plex's documented mobile default; configurable Immediate/5/10/15/30/60), **stop-only when no queue exists**, **2 h inactivity guard** (Plex's Passout Protection default), and post-playback actions (replay/next/delete) must remain reachable (Infuse regression). Deliberately rejected Plex's "<5 min" suppression rule (our users watch short clips). Doc 06 SCR-010g + Ended state updated; doc 09 orchestrator gains the countdown/inactivity guard; doc 17 RSK-015 rescoped to tuning only; doc 18 delta note updated; Q12 **closed**. |
| 25 | 2026-10-19 | **Package integrity: version control established, and the editing incident recorded.** `git init` (branch `main`) + initial commit `d7671ad` — 28 files, 6,152 insertions (the 21 documents in `spec/`, the 5 research notes, `.gitignore`, `.gitattributes`). `.gitattributes` normalizes Markdown to LF in-repo; `.gitignore` excludes recovery/scratch artifacts and future build output. **Incident (Part 24):** an errant `Set-Content -NoNewline` collapsed this file from 222 lines to 1 by stripping every line break; content and UTF-8 encoding survived intact but the structure did not, and there was no backup, snapshot or history to restore from. Newlines were reconstructed from the surviving Markdown markers and verified against the original (222 lines, 24 part-log rows, 10 table separators, all Unicode preserved), then confirmed **byte-identical to the committed blob** by hash comparison across all 26 files. Constraint note: versioning the **documents** is not application "repository scaffolding" — no app code, project files, build config or dependencies were created. |
| 26 | 2026-10-19 | **Industry practice adopted as a standing decision rule; Q13 and CS-4 closed.** New **17 §1.3**: before designing anything from first principles, establish what shipping products do; adopt it when practice is settled and our constraints do not conflict; record deviations with the practice, the reason and who accepted it; design from scratch only when practice is **absent, contradictory, or demonstrably worse**. Added a **classification of the current decision set** — every *how* decision is aligned or openly justified; the deviations are **scope sequencing** (Android TV, distribution channels, destructive file ops), not behaviour. New workstream **W7** `research/decoder_label_practice.md` (5 products + an SDK vendor, 13 sourced URLs) **closes Q13 and CS-4**: the technical-vs-outcome framing was a false dichotomy — the settled pattern is a technical name **plus a when-to-use description**, `Auto` as default, a global setting *and* a per-item override. Bare `HW/HW+/SW` is forbidden as the only label, because MX's own support-article tree, third-party explainers and forum threads asking what its abbreviations mean are documented evidence of that pattern's cost. FR-PLAY-003 (label language + a global default setting) and doc 06 SCR-010e updated; version bumps 04→v0.5, 06→v0.3, 02A→v0.3, 17→v0.3. |
