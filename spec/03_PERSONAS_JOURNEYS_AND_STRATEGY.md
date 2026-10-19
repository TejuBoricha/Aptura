# 03 — Personas, Jobs to Be Done, Journeys, Positioning, and KPIs

**Version:** 0.1 (provisional — to be refined after Phase 1 research completes)
**Date:** 2026-10-18
**Status:** Draft. Personas and pain hypotheses were **explicit hypotheses** `[I]`; the **W1 evidence update is applied (2026-10-19, see §4.1)**; W2–W4 pending. IDs are frozen; content may be revised in v0.2.
**Consumes:** 01 (scope), 00 §3 (assumptions).
**Feeds:** 04 (requirements traceability: `PER/JRN → FR`), 06 (UX), 13/14 (KPI instrumentation).

---

## 1. Personas (hypotheses H1–H5)

Prioritization key: **P1 = primary design target**, P2 = important, P3 = considered but not targeted in R1.

### PER-1 — "The everyday local-library viewer" (P1)
- **Sketch `[I]`:** Adult, Android mid-range phone (4–6 GB RAM), plays downloaded/recorded/sent video files; often in markets with high local-media usage (India, Brazil, SE Asia, MENA — pending W4 confirmation).
- **Goals:** Open a file and have it simply play; subtitles readable without fiddling; keep the phone cool and battery alive during a commute; find last-watched position; no interruptions.
- **Context:** Files from messaging apps, download folders, SD card; variable network (offline common); 1–500 loose files; tends to open files from the file manager/chat ("open with") rather than browsing a library.
- **Frustrations `[H]`:** Ads/nag screens in incumbent players; files that "sometimes just fail"; subtitle files that don't auto-load or show garbage characters; player forgetting resume position; UI full of things they never use.
- **Constraints:** Low patience for setup; doesn't read settings; storage constraints (16–128 GB); data costs.
- **Technical confidence:** Low–medium. Wants sensible defaults; will not tune decoders.
- **Accessibility needs:** None assumed; benefits from caption readability defaults and large-text support.
- **Reason to switch `[H]`:** "This one just plays my files and doesn't annoy me."
- **Evidence basis:** `[I]` pending W1 (review mining: ad complaints, subtitle failures, resume bugs).

### PER-2 — "The power user / cinephile" (P1 for quality features)
- **Sketch `[I]`:** Maintains a curated library of rips/recordings; HEVC 10-bit, high bitrate, embedded subtitles (SRT/ASS/PGS); expects frame-rate and audio-sync correctness; may later want SMB/NAS (explicitly deferred, communicated honestly).
- **Goals:** Perfect A/V sync; accurate seeking; audio delay and subtitle sync controls; track selection; playback stats (resolution/codec/bitrate/dropped frames); predictable behavior across odd files; watching on a tablet or phone in bed.
- **Context:** 1k–50k files; external storage or SD; experiments with files no one else plays.
- **Frustrations `[H]`:** Client-side failures on "weird" files; ASS typesetting mangled or unsupported; wrong audio track default; no diagnostics to understand failures; incumbent UIs cluttered with discovery/ads; battery/thermal throttling unnoticed.
- **Constraints:** Will notice and report correctness issues; compares against desktop players (mpv/VLC) and VLC on the same file.
- **Technical confidence:** High; will use advanced settings and read changelogs; values honest error explanations.
- **Accessibility needs:** N/A assumed.
- **Reason to switch:** Superior long-tail reliability + subtitle fidelity + diagnostics honesty `[H]`.
- **Evidence basis:** `[I]` pending W1 (forum/GitHub issue signals).

### PER-3 — "The privacy-conscious user" (P2; brand-critical)
- **Sketch `[I]`:** Rejects ad-driven and telemetry-heavy apps; checks permissions on install; prefers FOSS ethos but accepts a well-behaved proprietary app.
- **Goals:** Play local files; zero tracking; no accounts; clear permission justification; ability to verify claims (no network by default).
- **Frustrations `[H]`:** Incumbent players requesting broad permissions/devices; ads funded by data; "data safety" labels that are vague; unexplained network calls.
- **Constraints:** Small addressable segment if isolated; but upstream influence (forums, reviews) disproportionate.
- **Technical confidence:** High; will test with network monitors.
- **Reason to switch:** Verifiable privacy by construction (DEC-008) `[H]`.
- **Evidence basis:** `[I]` pending W4 (segment size signals) and W1 (privacy complaint history).

### PER-4 — "The casual / low-confidence user" (P2)
- **Sketch `[I]`:** Uses phone for family videos, WhatsApp files, screen recordings; low comfort with files/settings; often older or younger users (shared family device).
- **Goals:** Play a video someone sent; not lose it; find it again; no jargon.
- **Frustrations `[H]`:** Storage permission walls; "file not found" confusion; complex libraries; accidental deletions.
- **Constraints:** May deny permissions or not understand them; slow device; one-handed use.
- **Reason to switch:** Simplicity + trust (no traps, no upsells) `[H]`.
- **Evidence basis:** `[I]` pending W1/W4.

### PER-5 — "The accessibility-dependent viewer" (P2; conformance obligation per doc 05/06)
- **Sketch `[I]`:** Low-vision user relying on large text/screen reader; or hard-of-hearing user relying on captions; or user with motor constraints needing large targets and minimal precision gestures.
- **Goals:** Operate the player fully via TalkBack/VoiceOver/screen controls; read captions with adjustable size/contrast; avoid accidental activation.
- **Frustrations `[H]`:** Custom video players with poor focus order; gesture-only controls; tiny targets; captions that ignore font-size settings.
- **Constraints:** Accessibility conformance requirements; testing with real users/screen readers mandatory (doc 12).
- **Reason to switch:** Being one of the few players that does accessibility properly `[H]`.
- **Evidence basis:** `[I]`; conformance also driven by legal/advisory guidance (W4).

*Evidence gap to close in Phase 1: validate/invalidate PER-1/PER-2 as primary design targets; quantify PER-3 segment; map PER-4 to "open with" usage data found in research.*

---

## 2. Jobs to be done

| ID | Job statement (when… I want to… so I can…) | Primary personas |
|---|---|---|
| JTBD-01 | When I have a video file on my phone, I want it to play correctly on the first attempt, so I can watch without troubleshooting | PER-1, PER-2, PER-4 |
| JTBD-02 | When I get interrupted mid-video, I want to resume exactly where I stopped, so I don't lose my place | All |
| JTBD-03 | When subtitles exist (embedded or beside the file), I want them found, readable, and adjustable, so I can follow dialogue in any language | PER-1, PER-2 |
| JTBD-04 | When a file fails or behaves oddly, I want to understand why and what to try, so I'm not left guessing | PER-2, PER-1 |
| JTBD-05 | When my library grows large, I want to find, sort, and start videos quickly, so browsing doesn't feel like work | PER-2, PER-1 |
| JTBD-06 | When I watch in awkward situations (one hand, dark room, locked screen), I want simple controls that don't misfire, so watching stays comfortable | PER-1, PER-4, PER-5 |
| JTBD-07 | When I play demanding files, I want sync, performance, and battery behavior to stay respectful, so my device doesn't suffer | PER-2, PER-1 |
| JTBD-08 | When I install a player, I want to trust it with my files and device, so I don't feel exploited | PER-3, PER-4 |
| JTBD-09 | When I use the app over time, I want my preferences, positions, and organization to persist through updates, so the app feels like mine | All |
| JTBD-10 | When audio has multiple tracks or wrong sync, I want quick, correct control, so I can fix it in seconds | PER-2 |
| JTBD-11 | When someone sends me a video, I want the fastest safe path from "open with" to playback, so the moment isn't lost | PER-1, PER-4 |
| JTBD-12 | When I rely on assistive tech, I want the full experience operable, so I'm not a second-class user | PER-5 |

---

## 3. End-to-end journeys (v0.1; each maps to candidate requirement areas)

Each journey lists: goal → steps → failure/edge branches → implied requirement areas (to be given FR IDs in doc 04) → success signal.

**JRN-01 — First launch to first play (activation)**
1. Install → first launch → privacy-forward onboarding (no login) → permission education ("why we need access to your videos") → grant or skip → library bootstrap begins with progress UI → user opens a file (library or "open with") → first frame.
2. Branches: permission denied (JRN-06); zero files found (empty state with "browse folders"/"open a file" guidance); huge library bootstrap running in background with clear progress and cancellation.
3. Implied: FR-LIB-01… (onboarding, permission education, bootstrap), FR-PLAY-01 (first frame), NFR-PERF (TTFF), SCR-001/010/020.
4. Success: user reaches first frame without reading help; activation KPI recorded.

**JRN-02 — Routine play with resume**
1. Open app → recently played/first in list → resume badge visible → play → (auto-resume position) → adjust volume/brightness gestures → pause → exit → position saved.
2. Branches: file moved/deleted (staleness handling); resume prompt for nearly-finished items; multiple sessions in parallel (PiP).
3. Implied: FR-LIB resume/history; FR-CTRL gestures; FR-PLAY state persistence; SCR-030/040.
4. Success: resume accuracy within tolerance (doc 05 NFR).

**JRN-03 — Subtitle rescue (common frustration path)**
1. Play file → subtitles not shown/incorrect → open subtitle panel → detect embedded tracks + sidecar candidates → select track or filesystem sidecar → encoding garbage → switch encoding → adjust size/position/sync → persist per-video.
2. Branches: malformed subtitle file (must skip gracefully, not crash — FC list); ASS typesetting best-effort disclosure; RTL/Indic scripts.
3. Implied: FR-SUB-01…; SCR-060; doc 07 subtitle matrix.
4. Success: subtitle task completion KPI; zero crashes from subtitle parsers (TST adversarial).

**JRN-04 — Large-library setup and maintenance**
1. First run on a 10k-file library → choose scan locations → scan with progress + throughput estimate → browse/sort/filter/search responsively during and after scan → later: "new files found" incremental rescan → moved files reconciled via stable-ish identifiers.
2. Branches: interrupted scan (process death), SD card removal, permission revocation mid-use, corrupt file metadata (skip+log locally).
3. Implied: FR-LIB scanning/rescan/refresh; doc 09 data model; NFR-PERF scan targets.
4. Success: scan completes within budget with UI responsive (NFR).

**JRN-05 — Failure recovery ("won't play")**
1. Open unsupported/corrupt/damaged file → clear error surface (what happened: container/codec/DRM/corrupt; what we tried: hw→sw fallback) → actionable options (try software decode if not already, open details, report with redacted diagnostics) → user continues to another file without app friction.
2. Branches: decoder crash mid-playback (auto-fallback once, then honest failure); audio-only salvage when video decode impossible (`MAY`).
3. Implied: FR-PLAY error taxonomy; FR-DIAG; doc 10/14 redaction rules; FC register.
4. Success: fatal-error sessions have actionable next step; low "silent failure" reports.

**JRN-06 — Permission refusal and recovery**
1. User skips permission → app remains usable in limited mode (open-with playback works; no library) → settings screen explains what is unavailable and how to grant later (deep link to OS settings) → user grants later → library bootstraps without reinstall.
2. Branches: "limited access" (Android 14+ partial photo access analog for video), revoked while running (detect, degrade gracefully, no crash), TV? (N/A).
3. Implied: FR-LIB permission flows; SCR-011; NFR-A11Y (understanding).
4. Success: no dead-ends; recovery without data loss.

**JRN-07 — Interruption and lifecycle resilience**
1. Playing → incoming call/notification/audio focus loss → pause dutifully → resume on focus return (configurable) → rotate device/orientation change without glitch → background (PiP or pause per setting) → process death (OS kill) → relaunch → restore last position and session context seamlessly.
2. Branches: Bluetooth route change mid-play; headset unplug (pause); thermal throttle (graceful quality note if needed); battery saver.
3. Implied: FR-PLAY lifecycle; FR-AUD focus; FR-CTRL PiP; NFR-REL restoration exactness.
4. Success: state restoration correctness NFR; zero duplicate audio sessions.

**JRN-08 — Returning after an update / migration**
1. App updates (possibly schema migration) → launch → library/progress/settings intact → "what's new" in plain language → any reformatting (e.g., thumbnail cache rebuild) proceeds in background without blocking.
2. Branches: interrupted migration → safe retry; downgrade not supported (documented); corrupted DB → rebuild with user-visible notice and no false data.
3. Implied: FR-SET update notes; doc 09 migration; FC register (interrupted migration).
4. Success: migration success rate (local diagnostic); zero user-data loss reports.

---

## 4. Positioning and differentiation thesis

**Positioning statement (draft):** *For people who watch their own video files on their phone, [The Player] is the player that just works — reliable playback, first-class subtitles, and a calm interface, with nothing monetized against your attention and nothing leaving your device by default.*

**Differentiation hypotheses** (each: problem → segment → evidence status → incumbent gap → our advantage → feasibility → dependencies → privacy/a11y/licensing → validation method → falsifier). All DH-* are hypotheses to validate per §5.

| ID | Hypothesis | Segment | Evidence (v0.1) | Incumbent gap `[H]` | Advantage | Feasibility | Dependencies & cost | Privacy / a11y / licensing | Validation | Falsifier |
|---|---|---|---|---|---|---|---|---|---|---|
| DH-1 | Boring reliability + honest failures on the messy file long tail is a switch-worthy differentiator | PER-1, PER-2 | `[I]` pending W1 | Occasional opaque failures; users can't diagnose | Curated decoder strategy + fallback + error taxonomy + diagnostics | Medium: engine limits (RQ-01/RQ-03); needs compat matrix discipline | FFmpeg ext toolchain (RQ-02); test corpus | LGPL-clean build (DEC-009); a11y of error surfaces | Spike corpus benchmark; review mining of "won't play" complaints | Users report failures at similar rates; incumbents' failure UX adequate |
| DH-2 | Subtitle craftsmanship (discovery, encoding, styling, sync) | PER-1, PER-2 | `[I]` | Subtitle steps are fiddly; styling fidelity varies | Best-in-class subtitle UX + best-feasible ASS rendering | ASS fidelity uncertain (RQ-06) | libass-class rendering integration | Font licensing (W3); RTL/complex scripts (a11y/i18n) | Usability tests; compat corpus | Subtitle complaints are marginal in evidence |
| DH-3 | Verifiable privacy (no ads/trackers/accounts; network-silent by default) | PER-3, PER-4 | `[I]` pending W4 | Ad-driven incumbents; trust erosion | Zero-tracker build; no default network; honest permissions | High | None major | Core to brand; store data-safety claims must be exact | Static analysis + runtime capture proof; segment sizing | Segment too small to matter for retention |
| DH-4 | Speed and stability on low-end devices and large libraries | PER-1, PER-2 | `[I]` | Sluggish scanning/browsing reported for large libraries | Measured NFR budgets; indexing architecture (doc 09) | Medium | Engineering discipline (perf budgets in CI) | Battery/a11y overlap | Benchmark protocol (doc 13) on device tiers | Low-end performance parity with incumbents |
| DH-5 | Calm, uncluttered, reachable UI with configurable gestures | PER-1, PER-4 | `[I]` | Feature-cluttered, ad-infested UIs | Minimal-default UI + progressive disclosure | High (execution) | Design tokens/components (doc 06) | Touch targets/a11y built-in | Usability tests; accidental-activation metrics | Users prefer incumbent control layouts |
| DH-6 | State correctness as a feature (resume, restore, orientation, process death) | All | `[I]` | Reported resume/rotation glitches in incumbents | Deterministic state persistence + tests | Medium | Data model rigor (doc 09); test automation | No privacy impact | Chaos-testing results (doc 12) | Incumbents already robust here |
| DH-7 | Local-first data ownership (no cloud lock-in; import/export later) | PER-3, PER-2 | `[I]` | Cloud-coupled ecosystems (Plex-class) | Local library truth; portable formats later | High | Export format design (R2) | Privacy-positive | Beta cohort survey | Users don't care about ownership |

*Note: feature parity basics (play/pause, tracks, PiP, speed) are table stakes (doc 02 §baseline list) — not differentiators.*

### 4.1 Evidence update — W1 competitor research (accessed 2026-10-18; doc 02 v0.1)

Hypothesis status changes (the original table is retained above as the v0.1 record):

- **DH-1 (reliability): evidence upgraded.** Field-wide grind is visible (Nova sync/seek changelog tail; VLC crash-at-point reports; MX resume/login bugs) `[V1/R]`. Remains a core bet; frequency magnitudes still unmeasured.
- **DH-2 (subtitles): strong evidence; ranked #1 opportunity.** Fidelity demand proven (mpv/libass value), UX gap proven (Media3 players' missing styling/sync UI; zoom-hides-subs bug) `[R/V1]`.
- **DH-3 (privacy): strong contrast evidence** (MX history + privacy evaluations) `[V1/V2]`; segment size pending W4 → §5 validation.
- **DH-4 (scan/perf at scale): strong evidence; ranked #3.** Incumbent weakness concentrated here (VLC/KMPlayer/Nova signals) `[R/V1]`.
- **DH-5 (calm UI): evidence weak — demoted to execution quality, not a headline differentiator.** UI-clutter complaints were not strongly evidenced in the sampled sources.
- **DH-6 (state correctness): supported as part of the reliability grind;** keep (Nova/VLC/MX resume + regression signals).
- **DH-7 (local-first ownership): still `[I]/[U]`;** validate via survey before any external use.
- **NEW DH-8 (HDR/DV honest handling + best-effort correctness): added.** Evidence: mpv hwdec HDR breakage / no DV; Kodi v22 DV+PGS fix; Just DV P7→HDR fallback `[V1]`. Feasibility Low-Med (platform-bound); scope as an honesty/diagnostics differentiator, never a blanket claim.
- **Boundary input:** SMB + sidecar subtitles = strongest single specific gap, but out of R1 (DEC-001) → R2 gate Q1 (doc 02 §9).
- **Benchmark to respect in doc 07:** MX's vendor-documented subtitle format breadth (SRT, SSA/ASS full styling, SMI, SUB, IDX/VobSub, MPL2, TMPlayer, Teletext, PJS, WebVTT, DVD/DVB) is the incumbent baseline for the subtitle matrix.

---

## 5. Validation plan for personas & hypotheses (before/while building)

| Method | What it validates | Privacy notes |
|---|---|---|
| Review/forum mining (already in W1) | Pain frequency/severity DH-1/2/3/4 | Public data only |
| Structured interviews (8–15 target users across PER-1/2/3, incl. ≥2 assistive-tech users) | Personas, JTBD, switch reasons | Consent, no recording defaults, compensation stated |
| Landing-page smoke test (R2 candidate) | Positioning resonance for DH-3 | Minimal analytics, aggregate only, disclosed |
| Closed beta cohort + opt-in surveys | Satisfaction, subtitle task completion, betrayal checks | No media content collected; survey-based |
| Benchmark corpus runs during spikes | DH-1/4 feasibility | Synthetic/licensed test media only (doc 12 fixtures) |
| Runtime network/static analysis of our build | DH-3 proof | We prove our own claims; publish methods |

Falsification triggers are recorded per DH above; any DH refuted twice moves to doc 02's "claims to avoid" and doc 16 backlog. KPIs below are the quantitative half of validation.

---

## 6. KPI framework

Legend: **Source** = Play Console / Local-only diagnostics / Opt-in survey / Bench protocol. **Target** = `T` provisional (with rationale) or `MF` measure-first (no target until baseline exists). All KPIs are segmented by: device tier (low/mid/high), Android version, storage class, codec class, region (coarse), and app version — where the source supports it.

### 6.1 Acquisition
| ID | KPI | Definition / formula | Source | Privacy implications | Cadence | Target |
|---|---|---|---|---|---|---|
| KPI-ACQ-01 | Store listing conversion | Installs ÷ store visits (Play Console view) | Play Console | Aggregate platform data only | Monthly | MF |
| KPI-ACQ-02 | Install mix by region | Installs per region ÷ total | Play Console | Aggregate only | Monthly | MF |

### 6.2 Activation
| ID | KPI | Definition / formula | Source | Privacy | Cadence | Target |
|---|---|---|---|---|---|---|
| KPI-ACT-01 | First-session success | Users reaching first frame of any file ÷ new installs in cohort | Local-only aggregate diagnostic (opt-in) + user survey fallback | Counted locally; reported only if diagnostics consented (DEC-008) | Monthly cohort | T: ≥80% `[E]` (rationale: permission friction + empty-library cases account for most losses; validate) |
| KPI-ACT-02 | Time to first frame (activation context) | Wall-time install→first frame for activated users | Local diagnostic | Local-only by default | Monthly | MF (protocol in doc 13) |

### 6.3 Playback reliability
| ID | KPI | Definition / formula | Source | Privacy | Cadence | Target |
|---|---|---|---|---|---|---|
| KPI-REL-01 | Playback session success | Sessions completing without fatal playback error ÷ sessions started (session = one file play intent) | Local diagnostic, opt-in upload | No filenames/paths; codec/container class only (doc 14 schema) | Monthly | T: ≥99% `[E]` pending baseline; validate against corpus evidence |
| KPI-REL-02 | Fatal playback error rate | Fatal errors ÷ sessions started, by error class | Same | Same | Monthly | T: ≤1% `[E]` |
| KPI-REL-03 | Software-fallback rate | Sessions using sw decode ÷ sessions (higher = compat win, battery cost) | Same | Same | Monthly | MF (watch vs battery KPI) |
| KPI-REL-04 | Seek failure rate | Seeks not reaching target time ±tolerance ÷ seeks | Same | Same | Monthly | T: ≤0.5% `[E]` |
| KPI-REL-05 | State-restoration success | Sessions resuming at persisted position ÷ sessions where applicable | Local test + chaos suite (doc 12), not telemetry | No user data | Per release | T: 100% on chaos suite |
| KPI-REL-06 | Crash-free & ANR-free sessions | Platform vitals | Play Console | Platform-mandated | Continuous | Beat Play "bad behavior" thresholds with margin (doc 05) |

### 6.4 Engagement / retention
| ID | KPI | Definition | Source | Cadence | Target |
|---|---|---|---|---|---|
| KPI-RET-01 | D7 / D30 retention | Platform cohort retention | Play Console | Monthly | MF |
| KPI-RET-02 | Weekly playback sessions/active user | Sessions ÷ WAU (local aggregate, consent) | Local diagnostic | Monthly | MF |

### 6.5 Satisfaction
| ID | KPI | Definition | Source | Cadence | Target |
|---|---|---|---|---|---|
| KPI-SAT-01 | Store rating trend | Rating average + delta | Play Console | Monthly | MF (watch subtitle/one-star themes) |
| KPI-SAT-02 | Subtitle task completion | Users completing JRN-03 without abandoning (task-based study + telemetry-free instrumented beta) | Usability lab + beta survey | Per study | T: ≥90% in lab `[E]` |
| KPI-SAT-03 | Switch-intent survey | "Would you recommend over your previous player?" | Opt-in survey in beta | Per beta wave | MF |

### 6.6 Performance / resources
| ID | KPI | Definition | Source | Cadence | Target |
|---|---|---|---|---|---|
| KPI-PERF-01 | Cold start p50/p90 | Time to interactive library (definitions in doc 13) | Bench protocol | Per release | Targets in doc 05/13 `[E]` |
| KPI-PERF-02 | TTFF by codec tier | Play→first frame, per codec/container tier | Bench protocol | Per release | doc 05 `[E]` |
| KPI-PERF-03 | Scan throughput | Files indexed/min by storage class | Bench protocol | Per release | doc 05 `[E]` |
| KPI-PERF-04 | Battery per hour by tier | %/h 1080p30 hw, 1080p HEVC 10-bit, 4K where applicable | Bench protocol | Per release | doc 05 `[E]` |
| KPI-PERF-05 | Peak memory p95 | RSS during playback/scan | Bench protocol | Per release | doc 05 `[E]` |

### 6.7 Support burden
| ID | KPI | Definition | Source | Cadence | Target |
|---|---|---|---|---|---|
| KPI-SUP-01 | "Won't play" reports per 1k sessions | Support+review keyword coding | Support log + store reviews (manual coding) | Monthly | T: below incumbent baseline once measured `[E]` |
| KPI-SUP-02 | Subtitle-related contacts per 1k MAU | Same method | Monthly | MF |

### 6.8 Business sustainability (only if monetization proceeds)
| ID | KPI | Definition | Source | Cadence | Target |
|---|---|---|---|---|---|
| KPI-BIZ-01 | Unlock conversion | Purchasers ÷ eligible installs | Play Console/Billing | Quarterly | MF |
| KPI-BIZ-02 | Refund/chargeback rate | Refunds ÷ purchases | Store reports | Quarterly | Keep below store thresholds |

**Instrumentation rule (links doc 14):** every telemetry-based KPI above must exist in an opt-in, content-free schema; where consent is absent, the KPI degrades to survey/benchmark sources. No KPI may require filenames, paths, subtitles, frames, or viewing history.

---

## 7. Open questions

| # | Question | Resolves in |
|---|---|---|
| Q1 | Are PER-1/PER-2 really the primary segments (vs PER-4 dominance)? | W4 + user interviews → doc 02 v0.2 |
| Q2 | Is DH-3 a retention generator or a niche trust signal? | W4 + landing test |
| Q3 | Which DH-* survive evidence scrutiny? | Phase 1 synthesis |
| Q4 | KPI-REL-01 baseline/target realism | doc 13 (measure-first protocol) |
| Q5 | Assistive-tech user recruitment path for JRN validation | doc 12 (accessibility testing plan) |

## 8. Sources
Persona/JTBD/journey content: hypotheses `[I]` from master prompt principles + competitive hypotheses pending `research/competitors.md`, `research/market_compliance.md`. No external claims asserted yet; citations land with doc 02 v0.2.
