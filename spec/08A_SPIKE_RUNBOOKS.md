# 08A — Spike Runbooks (RQ-01…RQ-16)

**Version:** 0.1 (draft — **nothing in this document has been executed**)
**Date:** 2026-10-19
**Status:** Draft, docs-only. **No spike has been run, no measurement exists, and no result is claimed.** This document is *procedure*: it converts doc 08 §6's spike definitions (hypothesis, method, pass/fail) into runnable instructions, and it is the artifact the build phase executes from. Doc 08 remains **authoritative for what each spike decides and what counts as pass or fail**; 08A is authoritative for **how a run is conducted, recorded and adjudicated**.
**Companion status:** numbered `08A` deliberately, following the `02A` precedent — the master prompt's deliverable set is 00–18 and this document does not alter it (doc 18 §22 audit remains 24/24).
**Consumes:** 08 (§6 spike definitions, RQ-01…16; ADRs each unblocks), 12 (§5 fixture corpora, §6 device matrix, §7 harnesses and special programs, §3 test levels), 13 (M1–M10 measurement principles, MB-* metric catalogue, §5 statistical protocol), 05 (NFR-* targets, all `[T]`), 11 (L1/L2 licensing items), 16 (B0 phase and DEP register), 17 (§1.3 rule; §5 question stack).
**Feeds:** 16 (B0 sizing reality-check, §6 of this document), DEP-005 procurement scope, doc 18 (spike evidence registry), doc 08 (each ADR lock).

---

## 1. Operating rules

These apply to every spike. A run that ignores them produces an anecdote, not evidence.

| # | Rule | Why |
|---|---|---|
| S1 | **Fixtures are declared and checksum-pinned before the run starts.** The fixture list is written down first, then executed. | Prevents the most common spike failure: quietly dropping the files that misbehave and reporting the rest |
| S2 | **Timebox is fixed at the start. Overrunning a timebox makes the spike *inconclusive* — never "extended".** A second timebox requires a new, explicitly justified run. | Stops a spike becoming a silent product milestone (doc 08 §6 governance) |
| S3 | **Pass/fail is decided by doc 08's written rule, not by impression.** If the rule turns out to be ambiguous, that ambiguity is a *finding* and doc 08 is amended before the run is adjudicated. | A judgement call made after seeing the data is not a threshold |
| S4 | **Raw captures are retained.** A result without its raw evidence (timings, logs, captures, checksums) does not exist. | Enables re-adjudication and the independent review below |
| S5 | **A negative result is a first-class deliverable.** Failing spikes are recorded with the same care as passing ones, and are typically worth more: they are the cheapest point at which a plan can change. | RQ-06 failing changes the subtitle tier; discovering this at B4 costs far more |
| S6 | **Independent re-adjudication.** Someone who did not run the spike reads the raw evidence against doc 08's rule and signs the result. This is the cheap adversarial layer (doc 12 §1 "adversarial by default"). | Owner-reviewer self-approval is how a "pass" gets written onto an uncomfortable result |
| S7 | **No number leaves a spike as a public claim.** Spike output feeds ADRs, tiers and internal targets. Claim eligibility is doc 13 §6 and requires the doc 07 tier to be validated. | Prevents a spike number becoming store copy |
| S8 | **Measurement conditions are recorded with every figure** — device class, OS build, firmware, thermal state, build hash (doc 13 M2/M8/M10). | A number without conditions is not a number (doc 13 §1.2) |
| S9 | **Spikes run on the doc 12 §6 matrix.** Emulators are permitted for logic (RQ-07, RQ-15 instrumentation) and are **never** authority for codec, timing, power or thermal results (doc 12 §6, doc 13 M1). | The classic error this runbook set exists to prevent |
| S10 | **Every spike closes with a decision, not a summary.** "Locked", "amended", "inconclusive → re-run with X", or "abandoned because Y". A spike that ends without one has not finished. | The point of B0 is to replace hypothesis with evidence and then *act* |

---

## 2. The prerequisite stage B0.0 — the one the spike list does not contain

**Finding.** Doc 08 §6 defines sixteen spikes and no instrumentation stage. But almost every spike's method reads "…corpus", "…harness", or "…matrix" — and neither the fixture corpus (doc 12 §5) nor the measurement harnesses (doc 12 §7) exist yet. They are prerequisites of the spike program, not outputs of it.

B0 therefore has an **unlisted stage zero**, which should be planned and named:

| B0.0 item | What it is | Owned by | Needed by |
|---|---|---|---|
| **Fixture corpus v1** | The doc 12 §5 media matrix — generated, checksummed, license-noted (own recordings + CC0/CC-BY only), covering doc 07's A/B-tier cells at ≥3 files per validated cell `[T]` | 12 | RQ-01, RQ-03, RQ-04, RQ-05, RQ-06, RQ-12, RQ-13 |
| **Adversarial corpora v1** | Truncated/zero-byte/absurd-duration/backwards-timestamp/storm files; malformed and mixed-encoding subtitle sets (doc 12 §5) | 12 | RQ-01, RQ-06, RQ-10 |
| **Scan-at-scale corpora** | Synthetic libraries at 1k / 10k / 50k items `[T]` (doc 12 §7.2) | 12 | RQ-05, RQ-11 |
| **Seek & first-frame harness** | Input→correct-frame timing at pinned positions, keyframe/non-keyframe split (doc 13 MB-07) | 12 (impl) / 13 (method) | RQ-01, RQ-03 |
| **AV-sync harness** | Screen/audio capture rig per doc 12 §7.1 (acquisition is open question doc 13 Q13-5) | 12/16 | RQ-01 (bounded), doc 13 MB-08 |
| **State-restoration matrix** | The ≥12 kill points of doc 12 §7.3 with defined expected restore states | 12 | RQ-07 |
| **Device lab** | DEP-005 — see §5 | owner | 11 of 16 spikes |

**Consequence for planning.** B0 cannot start with RQ-01. It starts with B0.0, and B0.0 is not free: the corpus and the seek harness are themselves multi-week engineering artifacts `[E]`. Any B0 schedule that begins at "run RQ-01" on day one is wrong.

---

## 3. Scheduling map — what can start when

Spikes are grouped by their *binding prerequisite*, because that determines what can run concurrently and what the critical path actually is.

| Group | Binding prerequisite | Spikes | Can start |
|---|---|---|---|
| **A — analysis only** | None (no device, no harness) | RQ-09; the paper half of RQ-14 | **Immediately**, alongside B0.0 |
| **B — build machine** | Toolchain, no device | RQ-02 | Immediately |
| **C — corpus + harness** | B0.0 | RQ-01, RQ-03, RQ-05, RQ-06, RQ-10, RQ-11, RQ-12, RQ-13 | After B0.0 (RQ-10 can start on the adversarial corpora alone) |
| **D — device lab** | DEP-005 (one device per class D1–D3, plus an SD device, an HDR/DV device, a ≥90 Hz device) | RQ-04, RQ-07, RQ-15, RQ-16 (+ RQ-03, RQ-12, RQ-13 which also need specific device capabilities) | After B0.0 **and** lab arrival |
| **E — console/account** | Play Console internal test track, closed-testing enrolment | RQ-08 | Can be prepared early; elapsed time is dominated by store review, not engineering |
| **F — counsel** | Legal brief L1/L2 (doc 11) | RQ-04 (Dolby), RQ-02 (license sign-off) | Engaged in parallel; **elapsed time is counsel's, not ours** |

**Critical path.** Groups D and E are the only ones whose *elapsed* time is outside our control. DEP-005 is therefore on the B0 critical path, and the store track for RQ-08 should be opened long before the engineering work is finished.

---

## 4. Spike cards

Each card follows the same shape: what it unblocks, its binding prerequisite, its timebox `[E]`, hypothesis and pass/fail (condensed from doc 08, which remains authoritative), fixtures, numbered procedure, capture list, and what an inconclusive result means. **Timeboxes are `[E]` estimates in dev-weeks for one engineer working alone on that spike**, excluding elapsed-to-counsel and elapsed-to-store.

---

### RQ-01 — Media3 (+ audio extension) conformance on the personal-library corpus
**Unblocks:** ADR-001 engine lock, doc 07 tier promotions · **Prereq:** Group C (corpus, seek harness) · **Timebox:** 2–3 wk `[E]`
**Hypothesis (doc 08):** PASS expected on doc 07 A-tier cells; degradation only in B/C.
**Fixtures:** doc 07 A/B/C-tier media matrix, ≥3 files per validated cell `[T]`; the adversarial set for the graceful-degradation half.
**Procedure:**
1. Freeze and checksum the fixture list; declare it in the run record before playback (S1).
2. Per device class D1/D2/D3, play every fixture through the candidate stack; classify the observed outcome against doc 07's tier definitions — **not** against expectation.
3. For every non-A outcome, record the exact failure mode and whether our error taxonomy (TST-ERR) reaches the right state, or a spinner.
4. Measure first-frame latency (MB-01-adjacent, NFR-PERF-003) and seek latency (MB-07, NFR-PERF-004) on A-tier cells only, N≥5 runs, median + p95 + CV (doc 13 §5).
5. Re-adjudicate per S6 and close with a decision (S10).
**Capture:** per-cell outcome table; failure-mode list; timing distributions with conditions; the emitted error state per failure; harness raw output.
**Pass / fail (doc 08):** PASS = all A cells play, ≥99% of sessions reach first frame, seeks within NFR targets on D2. FAIL = systematic A-cell failure → engine fallback review.
**If inconclusive:** the usual cause is corpus gaps, not engine ambiguity. Re-run after extending the corpus for the specific cell that was under-sampled — do not widen the pass criterion.

---

### RQ-02 — Royalty-safe shipping config and toolchain without FFmpegKit
**Unblocks:** ADR-010, NFR-PERF-009 re-base, doc 11 sign-off items · **Prereq:** Group B + counsel (L1) · **Timebox:** 1–2 wk `[E]`
**Hypothesis:** a self-built LGPL-2.1 FFmpeg **audio** extension is buildable and maintainable, and lands within the size budget.
**Procedure:**
1. Establish a reproducible build of the audio extension against the pinned FFmpeg version; record the exact configure flags and the resulting decoder list.
2. Diff the built decoder list against doc 11's royalty-cleared list. **Any decoder present but not cleared is a failure regardless of size.**
3. Produce an SBOM for the artifact; confirm it matches doc 11's component register with no Forbidden component (doc 11 deny-list).
4. Measure the size contribution against NFR-PERF-009 in both ABI-split configurations (feeds RQ-14).
5. Confirm the LGPL §6 obligations are *satisfiable* — this is a **feasibility** check, not legal advice; counsel owns L1.
**Capture:** flags, decoder list diff, SBOM, size table, reproducibility evidence (two clean builds, comparable outputs).
**Pass / fail (doc 08):** PASS = license-cleared decoder list shipping, size within NFR-PERF-009 revised target, builds reproducible. FAIL = a non-cleared decoder is unavoidable, or size cannot be met.
**If inconclusive:** treat as FAIL for planning purposes and exercise doc 11 §3.5 — ship without the bundled extension. The design deliberately survives that outcome; it is not a schedule emergency.

---

### RQ-03 — HEVC 10-bit / AV1 coverage on D1–D3, and behaviour with software decode only
**Unblocks:** doc 07 profile tiers, FR-PLAY-014 messaging · **Prereq:** Group D (needs HDR/10-bit-capable devices) · **Timebox:** 1.5–2 wk `[E]`
**Hypothesis:** coverage concentrates on newer devices; D1 fails Main10 gracefully.
**Procedure:**
1. Probe codec capabilities per device (documented capability query), then play the 10-bit/AV1 fixtures — capability claims and observed playback are recorded separately, because they disagree.
2. For every software-decode path, record battery and thermal behaviour during a timed run (MB-09/MB-10), N≥5.
3. Verify that when only software decode exists, the user is told — the honest-degradation path (FR-PLAY-014) — rather than silently given a pixelated result.
4. Build the fleet matrix table: device class × codec profile × decoder used × outcome.
**Capture:** capability vs observed matrix; power/thermal series; screenshots of any degradation notice; per-device notes.
**Pass / fail (doc 08):** PASS = graceful taxonomy on all devices, consistent hardware on D2/D3 classes, documented fleet matrix. FAIL = systematic silent degradation, or a D1 class where the app becomes unusable rather than honest.
**If inconclusive:** the likely cause is sampling too few devices per class. Note that this spike's *output shape* (a fleet matrix) is deliberately chosen so partial results are still useful.

---

### RQ-04 — Dolby handling (AC-3/E-AC-3/DTS/TrueHD/DV): lawful and technical
**Unblocks:** ADR-010 list, FR-AUD-007, doc 07 §7/§9 · **Prereq:** Group F (counsel) + Group D · **Timebox:** 1–2 wk `[E]` engineering, plus counsel elapsed
**Hypothesis:** passthrough-first works where routes exist; bundled audio is legal only per doc 11.
**Procedure:**
1. Build the route matrix: device speaker, wired, Bluetooth, (cast/HDMI path noted as out of R1 scope where applicable). For each route × each codec, record what the platform will decode, what it will pass through, and what it will silently refuse.
2. **Set the legal posture from counsel's L2 answer first (doc 11)** — the engineering matrix must be filtered through the legal outcome, not reported alongside it as if independent.
3. For every combination that cannot work, verify the UX says so honestly (FR-AUD-007) instead of offering a control that does nothing.
**Capture:** route × codec matrix; counsel's written position; the UI state for each non-working combination.
**Pass / fail (doc 08):** PASS = policy table frozen **with legal sign-off**, honest UX for all outcomes. FAIL = an unclassifiable combination, or an offer that implies capability we do not have.
**If inconclusive:** counsel elapsed time is the usual cause. Do not guess the legal position to unblock engineering; instead freeze the *honest-polymorphism* half (every path has a truthful state) and finish the matrix when counsel answers.

---

### RQ-05 — Scoped-storage / SAF performance at 10k+ items on slow SD
**Unblocks:** doc 09 scan architecture, FR-LIB-004 confidence · **Prereq:** Group C + an SD device · **Timebox:** 1.5–2 wk `[E]`
**Hypothesis:** NFR-PERF-005 is achievable with the batched scan design.
**Procedure:**
1. Run the scan over synthetic libraries at 1k/10k/50k `[T]` (MB-04), with enumerate/extract/thumbnail stages timed separately.
2. Repeat the whole set on internal storage **and** portable/adoptable SD, reporting the two separately (an internal-only number must be labelled as such — doc 13 §4).
3. Interleave removal/revocation scenarios (TST-SDCARD) and confirm the inbox prompts degrade correctly instead of hanging.
4. Measure scan power cost as %/1k files (MB-05) on the pinned corpus.
**Capture:** stage-split timings per library size per storage class; variance; degradation-banner states; power series.
**Pass / fail (doc 08):** PASS = NFR-PERF-005 targets on D2, within +100% on SD. FAIL = targets missed on D2 internal.
**If inconclusive:** if variance exceeds CV 10% `[T]`, the *instability is itself the reported finding* (doc 13 M5) and the cell is excluded from claims until fixed.

---

### RQ-06 — ASS/SSA styling fidelity ceiling
**Unblocks:** ADR-003, DH-2 positioning, doc 07 §8 · **Prereq:** Group C (subtitle corpus) · **Timebox:** 1–1.5 wk `[E]`
**Hypothesis:** Media3-native undershoots libass on styled scripts; libass integration is feasible.
**Procedure:**
1. Assemble the styled-script fixture set across the ASS style tiers, plus the malformed/oversized adversarial subtitle set (doc 12 §5).
2. Render the same cues through the Media3-native path and capture the output; do the same with a libass reference render.
3. Present both to a human fidelity protocol (doc 12 §3 "manual charters"): the question asked of the reviewer is **"is the native result within the agreed quality bar?"**, not "which is prettier".
4. Separately confirm the **degradation path is honest**: unsupported styling must visibly fall back, not silently drop.
5. Estimate libass integration cost **if and only if** the native path misses the bar.
**Capture:** paired renders; the reviewer's written bar-judgement; degradation evidence; cost estimate if applicable.
**Pass / fail (doc 08):** PASS = native within the agreed quality bar → ship native. Else: libass integration plan with a cost/date range.
**If inconclusive:** "the bar" is the deliverable if it is not yet written down — an undefined bar is not a pass criterion. Write the bar, then re-render.

---

### RQ-07 — State-restoration correctness across the lifecycle edge matrix
**Unblocks:** FR-PLAY-015 confidence, doc 09 model · **Prereq:** Group D · **Timebox:** 1 wk `[E]`
**Hypothesis:** Media3 plus our session model restores at least to NFR-REL-005.
**Procedure:**
1. Execute the doc 12 §7.3 matrix — ≥12 kill points (mid-scan, mid-thumbnail, mid-playback by content source, mid-migration, mid-playlist-edit) — each with its **expected restore state written down before the run**.
2. Compare observed against expected per kill point. **"Resumed from zero without telling the user" is a failure even when nothing crashes** (doc 12 §7.3).
3. Cross the matrix with the interruption set (doc 12 §7.4): calls, alarms, assistant, PiP gestures, split-screen, screen off/on, route changes.
**Capture:** kill-point × expected × observed table; logs; a note per any state that was silently lost.
**Pass / fail (doc 08):** PASS = NFR-REL-005 target met. FAIL = any silent loss of position/queue/track state.
**If inconclusive:** emulator runs may resolve *logic* ambiguity (S9) but may not be used to declare the matrix passed.

---

### RQ-08 — One-time unlock without a backend (Play Billing offline behaviour)
**Unblocks:** doc 15/16 monetization plan · **Prereq:** Group E (internal test track) · **Timebox:** 0.5–1 wk `[E]` engineering, plus store elapsed
**Hypothesis:** works with documented caveats; regional pricing viable.
**Procedure:**
1. Open the internal test track **early** — elapsed time here is dominated by enrolment and review, not code.
2. Exercise the purchase → kill → relaunch → restore path, including offline relaunch and cache-clear relaunch.
3. Walk the refund/cancel/chargeback matrix and record the resulting entitlement state each time.
4. Confirm the exact wording shown to a user whose entitlement cannot be verified — it must not imply a purchase failed when verification simply could not run.
**Capture:** state table per scenario; screenshots of entitlement messaging; store-side configuration notes.
**Pass / fail (doc 08):** PASS = unlock/restore robust across the matrix with no backend. FAIL = any scenario where a paying user loses access silently.
**If inconclusive:** treat entitlement messaging as unshippable until resolved — a monetization plan that cannot state the user's entitlement truthfully is worse than no monetization (doc 12 §2 "truthfulness" priority 2).

---

### RQ-09 — iOS viability gate (post-R1)
**Unblocks:** DEC-002 revisit · **Prereq:** Group A — **none** · **Timebox:** 0.5 wk `[E]`
**Hypothesis:** an AVPlayer-only ceiling is too low for the promise; custom decode cost is high.
**Procedure:**
1. Re-derive doc 07's tier matrix for iOS against AVPlayer-only capabilities.
2. Cost the delta required to close the gap (the custom-decode route) in the same units doc 16 uses.
3. Compare value against cost with a privacy/simplicity angle, and write the recommendation with its counter-argument.
**Capture:** the iOS tier matrix, the cost model, the recommendation and its strongest objection.
**Pass / fail (doc 08):** PASS to proceed = value exceeds cost with a privacy/simplicity angle. Any other outcome defers DEC-002 again.
**If inconclusive:** default to deferring — R1 is Android-only (DEC-002), so inconclusive costs nothing today.

---

### RQ-10 — Privacy-preserving diagnostics that still resolve real failures
**Unblocks:** doc 14 viability · **Prereq:** Group C (adversarial corpora; no device strictly required) · **Timebox:** 0.5–1 wk `[E]`
**Hypothesis:** local traces plus redacted bundles carry enough signal to resolve the top failure classes.
**Procedure:**
1. Take the doc 12 adversarial corpus and the RQ-01 failure-mode list as the sample of "real failures".
2. For each failure class, ask the actual question the team will need to answer — *"can we tell what happened from this bundle alone?"* — and record yes/no with the specific evidence used.
3. Redaction check: confirm the bundle excludes media content, filenames as personal data, and identifiers (doc 14, doc 10).
4. Report the count of diagnosable classes and the ones that are not diagnosable, with what *would* be needed.
**Capture:** per-class diagnosability table; bundle samples; redaction diff.
**Pass / fail (doc 08):** PASS = ≥4/5 classes diagnosable without media content.
**If inconclusive:** an undiagnosable class is a doc 14 finding, not a spike failure — record the missing signal explicitly.

---

### RQ-11 — DB and thumbnail architecture at scale
**Unblocks:** ADR-004 confidence, doc 09 · **Prereq:** Group C (scan-at-scale corpora) · **Timebox:** 1–1.5 wk `[E]`
**Hypothesis:** Room plus the Media3 thumbnailer meets NFR-PERF-005/006/010.
**Procedure:**
1. Bench the harness at 50k items: cold library load, paging scroll (MB-03), search latency (NFR-PERF-006), thumbnail time-to-first and steady-state with cold and warm cache (MB-06).
2. Check the thumbnail disk cap and the low-battery pause behaviour under fault injection (TST-THB).
3. Confirm cache invalidation on file change, and that a moved file does not produce a stale thumbnail.
**Capture:** latency distributions per stage; cap enforcement; invalidation results; memory during 50k browse (MB-11).
**Pass / fail (doc 08):** PASS = targets met on D2.
**If inconclusive:** separate the DB from the thumbnailer — they have independent architectures and combining them hides which one missed.

---

### RQ-12 — HDR / VFR / refresh-rate behaviours: free versus to-build
**Unblocks:** doc 07 §9 tiers · **Prereq:** Group D (HDR-declaring, ≥90 Hz devices) · **Timebox:** 1 wk `[E]`
**Hypothesis:** the platform gives tone-mapping and VFR tolerance for free; refresh alignment is feature-flag territory.
**Procedure:**
1. On HDR-declaring devices, play HDR fixtures and check for colour corruption and tone-mapping (FR-PLAY-013) — qualitatively, and against the declared-versus-actual attributes.
2. Play VFR fixtures and check cadence/A-V behaviour.
3. Enable the refresh-rate switch candidate and determine whether the kill-switch is required in practice; record the conditions under which it misbehaves.
4. Confirm DH-8's honest path renders correctly when the device *declares* HDR but the content cannot be handled well — there must be a real UI home (SCR-010h), not just a log line.
**Capture:** HDR/VFR observations per device; refresh-switch behaviour and its failure conditions; the honesty-path UI state.
**Pass / fail (doc 08):** PASS = no colour corruption (FR-PLAY-013) and a validated refresh-switch kill-switch.
**If inconclusive:** default to shipping the kill-switch **on** (i.e. feature off) — the conservative reading of "works for free" being unproven (doc 08 §6.1 note on RQ-12).

---

### RQ-13 — Compose performance on D1 / API 26
**Unblocks:** ADR-002 · **Prereq:** Group D (a real D1 device; emulators are not authority — M1) · **Timebox:** 1–1.5 wk `[E]`
**Hypothesis:** targets are reachable with baseline profiles and disciplined composables; otherwise hybrid Views.
**Procedure:**
1. Run the macrobenchmark suite on the D1 device: startup (MB-01/MB-02), library scroll and grid (MB-03), against NFR-PERF-001/007.
2. Apply baseline profiles and re-run; report before/after with the same conditions.
3. If targets are still missed, evaluate the documented escape hatch — custom `SurfaceView`/`PlayerView` interop and hybrid Views for the heavy list screens (doc 08 ADR-002 consequence) — and report the delta.
**Capture:** before/after distributions; profile artifacts; the decision point if the escape hatch is needed.
**Pass / fail (doc 08):** PASS = NFR-PERF-001/007 D1 targets.
**If inconclusive:** an unstable D1 measurement is a hardware/thermal finding (S8) — check the noise floor (doc 13 §5) before concluding anything about Compose.

---

### RQ-14 — 32-bit ABI demand versus size cost
**Unblocks:** ADR-009, NFR-COMP-002 · **Prereq:** Group A for the analysis half; store telemetry for the demand half · **Timebox:** 0.5 wk `[E]`
**Hypothesis:** arm64-only is acceptable for the target fleet.
**Procedure:**
1. Measure the size delta of split versus universal builds (pairs with RQ-02's size work).
2. Establish whether store dashboards can answer the 32-bit demand question at all pre-launch, and if they cannot, say so — do not substitute a guess.
3. Record the decision and its revisit trigger (post-launch telemetry) rather than resolving it prematurely.
**Pass / fail (doc 08):** PASS either way **with a documented tradeoff**.
**Note:** this is the one spike whose outcome may legitimately be "decided later, with the trigger written down". That is a pass, not a deferral failure.

---

### RQ-15 — Adaptive layout across breakpoint changes (rotation, foldable, split-screen)
**Unblocks:** doc 06 §3 confidence, FR-CTRL-013, U1 residual risk · **Prereq:** Group D · **Timebox:** 0.5 wk `[E]`
**Hypothesis:** M3's breakpoint model and Android's documented list-detail state rules hold in practice; the 840dp+ two-pane layout loses no state and duplicates no pane.
**Procedure:**
1. Instrument transitions compact ↔ medium ↔ expanded across rotation, fold/unfold, split-screen resize and free-form windows.
2. Assert per class: the documented navigation component appears, the documented pane count appears, selection is preserved, and **no duplicated pane** is rendered.
3. Enter and exit PiP from a two-pane screen.
**Capture:** transition recordings; state assertions; any class that loses state or renders a broken pane.
**Pass / fail (doc 08):** PASS = layout follows the documented component/pane per class with no state loss and no lost selection; PiP enters correctly from every class. FAIL = any class loses state or renders a broken pane → fix before the pane layout ships.
**If inconclusive:** this is the empirical half of doc 16's de-scope item 3; an inconclusive result exercises that de-scope rather than blocking R1.

---

### RQ-16 — Platform integration assumptions on retail builds
**Unblocks:** FR-PLAT-002, FR-CTRL-013, NFR-PRIV-004, DH-3 · **Prereq:** Group D · **Timebox:** 0.5 wk `[E]`
**Hypothesis:** the documented `POST_NOTIFICATIONS` exemption for media-session notifications and the documented 200dp gesture-exclusion behaviour both work as specified on retail builds and on at least one OEM skin.
**Procedure:**
1. On a device where `POST_NOTIFICATIONS` has **never** been granted, start playback and confirm the media notification appears and lock-screen transport controls work. Record the permission state before and after.
2. Confirm the manifest declares no `POST_NOTIFICATIONS` and that no code path posts a non-media notification (the invariant that preserves the exemption — FR-PLAT-002).
3. With system bars visible **and** stickily hidden, verify exclusion rects are honoured at the left/right edge and that the scrubber thumb can be dragged inward from the edge.
4. Confirm the `PlaybackState`-derived control set is what the user actually sees on Android 13+.
**Capture:** permission-state log; notification and lock-screen captures; edge-gesture recordings in both bar states; observed control set.
**Pass / fail (doc 08):** PASS = notification and controls present without the permission on all tested builds; scrubber reachable at the edge. FAIL = **notification absent → re-opens the permission decision** (FR-PLAT-002, NFR-PRIV-004) and weakens the DH-3 minimalism claim; **exclusion ignored → widen edge insets** and record the deviation.
**If inconclusive:** the FAIL branch is deliberately explicit. If the exemption proves unreliable on an OEM skin, the correct response is a scope decision (accept a possibly-missing notification, or request the permission and lose the no-prompt property) — **not** an engineering workaround.

---

## 5. Lab and procurement implications (feeds DEP-005)

| Requirement | Drives | Notes |
|---|---|---|
| ≥1 device per class **D1, D2, D3** | RQ-01, RQ-03, RQ-05, RQ-11, RQ-13, RQ-15, RQ-16 | D1 is the binding constraint — it is the only class that can fail RQ-13 and the only one that tests graceful degradation |
| Portable **and** adoptable SD | RQ-05, RQ-11, TST-SDCARD | Slow-storage realism cannot be emulated |
| HDR-declaring device; one DV-declaring device | RQ-03, RQ-12, TST-HDR | Used **only** for honest-path tests, per doc 12 §6 |
| ≥90 Hz display device | RQ-12 | Refresh-alignment work is unmeasurable without it |
| ≥1 OEM-skinned device (Samsung-class and Xiaomi-class minimum) | RQ-16, RQ-15 | Gesture and notification variance is an OEM property, not an OS property |
| OS-band coverage across 26 → 36 | doc 12 §6 | At least one device per band |

**Two scheduling corrections this runbook set surfaces.**

1. **DEP-005 is on the B0 critical path, not a parallel long lead.** Doc 16 lists device procurement among "off-path but launch-relevant long leads" (doc 16 §"Off-path", line 107), while its own B0 row requires "device lab minimum (≥1 device/class D1–D3)". Both are true of *different paths*: procurement does not gate store submission (the launch path), but it **does** gate 11 of the 16 spikes (the B0 path). It should be ordered on day one of B0, not treated as something that can drift to B6.

2. **B0's 4–8 dev-week range only holds with concurrency.** Summed sequentially, the timeboxes in §4 total roughly **18 dev-weeks `[E]`** — before B0.0's corpus and harness work. The doc 16 range therefore implicitly assumes either several engineers running spikes concurrently, or substantial reuse from B0.0. **If one engineer owns B0, the range is not achievable and doc 16's estimate should be re-based rather than quietly missed.** This is flagged rather than asserted: the timeboxes here are `[E]` estimates and are themselves uncalibrated until the first three spikes close (at which point the remaining estimates should be re-derived from actuals).

---

## 6. Result artifact template

Every spike files one record. Ten fields; any missing field makes the result inadmissible (§1 S4).

| Field | Content |
|---|---|
| Spike ID and title | `RQ-nn — …` |
| Date, operator, reviewer | Operator and the **independent** re-adjudicator (S6) |
| Decision closed with | Locked / amended / inconclusive → re-run with X / abandoned because Y (S10) |
| Conditions | Device class, model, OS build, firmware, thermal start/end, build hash (S8) |
| Fixture manifest | Checksummed list, declared **before** the run (S1) |
| Raw evidence | Timings, logs, captures, screenshots, matrices — attached, not summarised away |
| Result against doc 08's rule | The rule quoted, then the observed outcome, then pass/fail (S3) |
| Negative findings | Recorded with equal prominence (S5) |
| ADR / doc impact | Which ADR locks, which tier moves, which NFR target is re-based, which doc is amended |
| Follow-ups | Named owner and next action, including any re-run |

---

## 7. Boundaries — what this document is not

- **Not executed.** Nothing here has been run. No device has been obtained; no corpus exists; no result is claimed.
- **Not code.** Procedures name the documented tools and harnesses they depend on; the harnesses themselves are doc 12's implementation work and are out of scope for this specification phase.
- **Not a re-definition of pass/fail.** Doc 08 §6 remains authoritative. Where this document appeared to sharpen a criterion, it did so only to make it runnable, and the amendment belongs in doc 08.
- **Not a schedule.** Timeboxes are single-engineer `[E]` estimates and are uncalibrated until the first three spikes close. §5's concurrency finding is a flag for doc 16, not a revised plan.
- **Not evidence about devices.** Every capability statement here is a *hypothesis to test*. This document must not be quoted as showing that anything works on any device class.

---

## 8. Open questions raised by this runbook set

| # | Question | Consequence if unanswered | Owner |
|---|---|---|---|
| RB-1 | Is B0.0 (corpus + harness) inside doc 16's 4–8 dev-week B0 range, or additive to it? | The most likely source of a missed B0 estimate | 16 |
| RB-2 | How many engineers will run B0 concurrently? | Determines whether the 4–8 wk range is achievable at all (§5) | owner (OD-4) |
| RB-3 | Is DEP-005 ordered on day one of B0, given it gates 11 spikes? | Every device-dependent spike stalls | owner (OD-4) / 16 |
| RB-4 | Who is the independent re-adjudicator (S6), and is that role staffed? | Results lose their adversarial layer | owner / 12 |
| RB-5 | Is the ASS "agreed quality bar" (RQ-06) written down before the spike runs? | RQ-06 has no pass criterion until it is | 07 / 06 |
| RB-6 | Screen/audio capture rig: acquire or rent (doc 13 Q13-5)? | RQ-01's sync half and MB-08 are blocked | 12/16 |
