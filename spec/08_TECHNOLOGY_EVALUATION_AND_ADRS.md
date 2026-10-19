# 08 — Technology Evaluation and Architecture Decision Records

**Version:** 0.7 (draft; all choices are provisional hypotheses gated by the spike program in §6; v0.7 **corrects §6.2's RQ-12 note** — the Part 32 "no owning requirement" claim was wrong: FR-PLAY-012 owns refresh-rate alignment, and the check that produced the claim was case-sensitive (Part 34); v0.6 applies the Part 32 audit fixes — RQ-01's measurement set corrected (NFR-PERF-006 was search latency and has no bearing on codec conformance), plus §6.2 notes on the RQ-03 procurement rule, RQ-12's refresh half, RQ-10's bundle reference and corpus sizing; **no hypothesis or pass/fail threshold changed**; v0.5 adds the **procedure layer** — `08A_SPIKE_RUNBOOKS.md`: operating rules S1–S10, the B0.0 instrumentation prerequisite, the scheduling map, per-spike runbook cards RQ-01…16, procurement implications and the result template; v0.4 adds **RQ-16** — verify on retail builds that the **media-session notification exemption** and **gesture exclusion rects** behave as documented, the two platform assumptions the specification now depends on — bumping the set to RQ-01…16; v0.3 adds **RQ-15** — adaptive layout across breakpoint changes (rotation, foldable, split-screen), the empirical half of the now-breakpoint-based layout spec; v0.2 adds §6.1 prior-art notes from doc 02A)
**Date:** 2026-10-19
**Status:** Draft. No technology has been tested, benchmarked, or integrated by us. Scores below are **reasoned comparisons** labeled `[I/E]`, built on `research/platform.md` and `research/licensing.md` evidence; the spike program (§6) decides for real in the build phase. **No spike has been run.**
**Consumes:** 01 (scope/platform), 05 (NFRs), 07 (compatibility tiers), `research/platform.md`, `research/licensing.md`.
**Feeds:** 09 (architecture), 11 (license register), 12/13 (spike measurement design), 16 (roadmap sizing), 18 (traceability).

---

## 1. Evaluation method

**Criteria and weights** (fail-any rules marked ⚠):

| Criterion | Weight | Why |
|---|---|---|
| ⚠ Licensing fit for a store build (LGPL-clean, no GPL linkage; royalty posture) | Critical | DEC-009; `research/licensing.md`: naive FFmpeg/mpv paths carry GPL or royalty problems |
| ⚠ Compatibility reach vs doc 07 tiers | Critical | The product promise lives here (DH-1/DH-2) |
| ⚠ Subtitle fidelity path (ASS/SSA styling; RQ-06) | Critical | Ranked #1 opportunity (doc 02 §8) needs a credible fidelity ceiling |
| Ecosystem/maintenance (releases, docs, community, security responses) | High | One-maintainer risks are visible in the field (mpvKt archived, W1) |
| Integration effort & team fit (Kotlin/Android-native, learning curve) | High | ASM-005 small team |
| Performance/battery control (hw-first, fallback plumbing, PiP/FGS behaviors) | High | NFR-PERF/RES |
| App size / build complexity | Medium | NFR-PERF-009; AAB limits |
| Long-term exit cost (switching engines later) | Medium | Fallback strategy must stay real |
| TV/future-platform path (R2+) | Low (R1) | Out of R1 scope, but shouldn't be a dead end |

Scoring vocabulary: ● strong · ◐ adequate · ○ weak — no numeric fiction.

---

## 2. Candidate stack evaluation

### 2.1 Media engine

| Criterion | **Media3/ExoPlayer** | libmpv (JNI, mpv-android-class) | libVLC (JNI) | Platform MediaPlayer |
|---|---|---|---|---|
| Licensing fit ⚠ | ● Apache-2.0 `[V1]` | ○ mpv is GPL `[V1]` — store-build contamination risk (DEC-009) | ◐ libVLC core LGPL-2.1 possible; app integration nontrivial `[V1]` | ● system |
| Compatibility reach ⚠ | ◐ device-decoder-bound; AVI/others partial (doc 07) | ● widest render/decode toolchain | ● broad (VLC-class) | ○ minimal |
| Subtitle fidelity path ⚠ | ◐ native renderers; libass integration would be a build task (RQ-06) | ● libass-class fidelity (mpv) but thin UX in existing apps `[R]` | ◐ capable, heavier | ○ weak |
| Ecosystem | ● Google-maintained, frequent releases `[V1]` | ◐ small maintainer set; one-maintainer risk seen in field | ● VideoLAN org | ● platform |
| Integration effort | ● native AndroidX; docs; samples | ○ JNI/NDK + custom render pipeline; team cost high `[E]` | ○ JNI + libVLC app patterns; medium-high `[E]` | ● trivial |
| Perf/battery control | ● platform-integrated (PiP, FGS, tunneling, media session) `[V1]` | ◐ full control, more to build | ◐ full control, more to build | ○ limited |
| Size/build | ● small core | ◐ large native libs | ◐ large native libs | ● none |
| Exit cost later | ● Media3 → libVLC-class is a rewrite of the playback layer only | ◐ | ● | — |

**Conclusion (provisional):** keep **DEC-003**: Media3 core, with the **self-built LGPL-2.1 FFmpeg audio extension** (audio-only, per W3) and platform software decoders as the video fallback; **libVLC as the sanctioned fallback engine** if RQ-01/RQ-06 fail hard; **mpv-class is rejected for the store build** on license grounds (would be reconsidered only with a dynamic-linking legal opinion — recorded as a legal question, not a plan). `[I]` — spike RQ-01 decides.

### 2.2 Application stack (Android)

| Area | Choice (provisional) | Alternatives considered | Notes |
|---|---|---|---|
| Language | Kotlin | Java (legacy tooling) | Coroutines/Flow for async; `[I]` |
| UI toolkit | **Jetpack Compose** + Material 3 | XML Views; hybrid | Compose now default for new apps `[V1]`; **D1/API-26 performance must be spiked** (RQ-13 below) — escape hatch: custom `SurfaceView`/`PlayerView` interop for the video surface itself (standard with Media3) |
| Video surface | Media3 `PlayerView`/Compose interop; `SurfaceView` default, `TextureView` fallback | Custom GL | Panel/zoom behaviors per FR-PLAY-011 |
| Persistence | **Room/SQLite** (library DB, history, prefs cache) + **DataStore** (settings/flags) | ObjectBox, Realm, raw SQLite | Room = Jetpack, well-understood `[I]`; schema migrations per NFR-OPS-004 |
| DI | **Hilt** | Koin, manual | Ecosystem alignment; keep module graph small `[I]` |
| Images/thumbnails UI | **Coil** | Glide | Compose-first, small `[I]` |
| Video thumbnails | **Media3 Thumbnailer** primary (device-decoder aware), `MediaMetadataRetriever` fallback | Custom codec pipeline | Battery-aware scheduling per FR-LIB-009/NFR-RES |
| Background work | **WorkManager** (+ JobScheduler semantics) | Custom service scheduling | Constraint-based (charging/idle) per NFR-RES-007 |
| Concurrency | Coroutines + Flow; strict dispatcher rules in doc 09 | RxJava | NFR-REL-002 (ANR) protection |
| Crash/telemetry | **None third-party in R1**; Play vitals + local logs + user-initiated report flow | Firebase Crashlytics, Sentry | DEC-008 privacy posture; ADR-007 |
| Build | Gradle Kotlin DSL + version catalog; R8; per-ABI splits; baseline profiles | — | NFR-PERF-009, NFR-OPS-001; pins rechecked at build start |
| Testing | JUnit/Robolectric unit; instrumentation for device-coupled flows; **Macrobenchmark** for doc 13 protocols; screenshot tests | — | Doc 12 defines suites; no performance claims until doc 13 |
| Cross-platform frameworks | **Rejected for R1** — Flutter/RN interop with a native media pipeline adds a layer without product value `[I]` | — | Revisit never for player surface; maybe for non-player surfaces only if team changes |

---

## 3. Architecture Decision Records (ADR) — provisional until spikes pass

Each ADR: **Context → Decision → Alternatives → Consequences → Revisit trigger.** Status: *proposed (spike-gated)* unless noted.

**ADR-001 — Media3 as the playback core.**
Context: engine choice (DEC-003). Decision: androidx.media3 with platform decode first; platform software decoders as video fallback; self-built LGPL-2.1 FFmpeg extension for audio decode; libVLC as fallback candidate; mpv-class rejected for store build. Consequences: engine capabilities bounded by device decoders (doc 07 matrix governs messaging); we own the fallback UX; exit cost contained to the playback layer. **Revisit:** RQ-01 or RQ-06 fail (libVLC path opens).

**ADR-002 — Kotlin + Jetpack Compose, with a spiked D1 performance gate.**
Context: NFR-PERF (start, scroll, memory) on API 26 D1 devices. Decision: Compose + Material 3; player surface via Media3 interop views. Consequences: modern DX; **risk item RQ-13** (Compose on D1) with baseline profiles + macrobenchmark gates. **Revisit:** if D1 targets are unreachable after optimization, hybrid Views for the heavy list screens.

**ADR-003 — Subtitle rendering path: Media3-native now, libass-gated by RQ-06.**
Context: subtitle fidelity is ranked #1 opportunity; Media3 parses SRT/SSA/ASS/VTT/TTML but ASS fidelity is unverified `[V1]`; libass (ISC) is the reference `[V1/W3]`. Decision: ship Media3-native rendering + our UX layer (styling controls, sync, encoding, per-video memory); run RQ-06 early; if fidelity PASS on target corpus → stay native; if FAIL and libass integration cost is acceptable → libass-in-pipeline; else document the ceiling honestly. Consequences: UX layer is ours regardless; fidelity ceiling decision deferred to evidence, not vibe. **Revisit:** RQ-06.

**ADR-004 — Room for library data, DataStore for settings.**
Context: 10k+ item libraries, migrations, testability (NFR-PERF-005/006, NFR-OPS-004). Decision: Room + Flow; DataStore for simple prefs. **Revisit:** measured scan/search regressions (RQ-11/13).

**ADR-005 — Hilt DI.**
Context: small team, test seams for media/time; keep depth low. Decision: Hilt modules per layer; no service locator patterns. **Revisit:** build-time cost becomes real (possible KSP-only manual DI alternative noted).

**ADR-006 — Coil for images.**
Decision: Coil (thumbnails UI-side); generation stays in our pipeline. **Revisit:** tv/future.

**ADR-007 — No third-party telemetry/crash SDK in R1.**
Context: DEC-008, privacy positioning; Play vitals exist. Decision: rely on Play vitals + local diagnostics + FR-DIAG-004 report flow; any future addition requires doc 14 schema + store label updates. **Revisit:** support burden evidence (doc 14 triggers).

**ADR-008 — minSdk 26 (Android 8.0), target API current per Play deadlines.**
Context: doc 01 §7/W2 coverage data (29+ ≈91%, 28+ ≈94% `[V2]`); PiP native at 26; Media3-friendly. Decision: minSdk 26; API-behavior matrix in NFR-COMP-001. **Revisit:** QA cost or adoption data before R1 lock.

**ADR-009 — ABI strategy: arm64-v8a required; armeabi-v7a decided at build spike.**
Context: NFR-PERF-009 size vs D1 fleets. Decision: splits; decide 32-bit after size/perf measurement (RQ-14). **Revisit:** RQ-14.

**ADR-010 — Self-built LGPL-2.1 FFmpeg audio extension; no bundled video/AAC software decode.**
Context: W3 (FFmpegKit retired; ext is audio-only; royalty posture). Decision: pin FFmpeg version at build start; maintain source offer + relinkable objects process ⚖️; decoder list limited to license-cleared codecs (AC-3/E-AC-3 add-back gated). **Revisit:** doc 11 legal outcomes; codec needs from RQ-01.

**ADR-011 — R1 contains no network stack at all.**
Context: FR-NET-001 boundary; privacy proofs (NFR-PRIV-002). Decision: no HTTP/SMB dependencies shipped; store/Play infrastructure excluded from the definition; proof tests per doc 10. **Revisit:** R2 gate only.

**ADR-012 — Packaged feature flags; no remote config.**
Context: no network (ADR-011) + rollout containment (NFR-OPS-002). Decision: flags compiled per build (safe defaults); containment = staged rollout + halt + hotfix. **Revisit:** R2+.

**ADR-013 — WorkManager for batch work.**
Context: scan/thumbnail scheduling (FR-LIB-004/009). Decision: constrained jobs, cancelable, battery-aware. **Revisit:** scheduler limitations found in RQ-05.

---

## 4. Explicitly rejected options (with reasons)

| Option | Verdict | Reason |
|---|---|---|
| FFmpegKit (mobile FFmpeg packages) | **Rejected** | Retired Jan 2025; repo archived Jul 2026 `[V1/W3]` |
| Full FFmpeg with `--enable-gpl`/`nonfree` | **Rejected** | GPL/nonfree obligations vs DEC-009 |
| Bundled **video** software decode (any builder) | **Rejected for R1** | Royalty ambiguity (HEVC pools; AV1 litigation context) `[W3/⚖️]` |
| mpv as engine | **Rejected for store build** | GPL `[V1]`; dynamic-linking workaround = legal opinion required, not planned |
| Standalone ExoPlayer (pre-Media3) | **Rejected** | Superseded by Media3 `[V1]` |
| Platform MediaPlayer / VideoView as main engine | **Rejected** | Docs 04/07 requirements unreachable (tracks, sync control, subtitles, stats) |
| WebView/HTML5 player | **Rejected** | Format coverage, hw path, FGS/PiP integration all worse |
| Third-party ad/analytics SDKs | **Rejected** | DEC-008 + product identity |
| Remote config / A-B service in R1 | **Rejected** | ADR-012 (no network in R1) |
| Cloud sync/accounts | **Rejected for R1** | Scope (DEC-001/004); backend cost |

---

## 5. Risk register deltas from this evaluation

- **RSK-08-01 (new):** Compose-on-D1 performance risk (mitigation: RQ-13, hybrid fallback).
- **RSK-08-02 (new):** FFmpeg audio extension legal process must clear before shipping audio decode beyond platform codecs (mitigation: ADR-010 gates; FR-AUD-007 split).
- **RSK-08-03 (new):** Engine fallback (libVLC) is a real plan only if evaluated promptly after a spike failure — mitigation: time-boxed RQ-01 with pre-agreed thresholds.
- **RSK-08-04:** ASS fidelity ceiling could undercut the #1 differentiator (mitigation: RQ-06 + honest positioning fallback in doc 02 §8).

*(Full registers live in doc 17; these are pre-registrations.)*

---

## 6. Spike program (definitions; nothing executed in the spec phase)

**Governance:** each spike is timeboxed, has the hypothesis/pass-fail below, produces an evidence artifact (results table + fixtures) filed under doc 13/18, and its decision is recorded in the ADR + doc 17. **A spike is not allowed to become a product milestone silently.**

**How a spike is run:** `08A_SPIKE_RUNBOOKS.md` is the procedure layer for this section — operating rules S1–S10 (fixtures pinned before the run, timebox expiry means *inconclusive*, independent re-adjudication, conditions recorded with every figure), the **B0.0 prerequisite stage** (the fixture corpus and measurement harnesses this table assumes but never lists), the scheduling map by binding prerequisite, a runbook card per spike, the lab/procurement implications, and the result-artifact template. **Division of authority:** this document is authoritative for *what* each spike decides and *what counts as pass or fail*; 08A is authoritative for *how a run is conducted, recorded and adjudicated*. Where 08A sharpened a criterion to make it runnable, the amendment belongs here.

| ID | Question (from 00 §3.4) | Hypothesis | Method (build phase) | Pass/Fail thresholds (provisional) | Unblocks |
|---|---|---|---|---|---|
| RQ-01 | Media3(+audio ext) conformance on the personal-library corpus | PASS expected on doc 07 A-tier cells; degradation only in B/C | 30-file fixture corpus per doc 12 across D1–D3; classify per FR-PLAY-004; measure NFR-PERF-003/004 | PASS: all A cells play; ≥99% sessions reach first frame; seeks within NFR targets on D2. FAIL: systematic A-cell failure → engine fallback review | ADR-001, doc 07 promotions |
| RQ-02 | Royalty-safe shipping config + toolchain without FFmpegKit | Self-built LGPL-2.1 FFmpeg audio ext is buildable/maintainable; size within budget | Reproducible build + SBOM; decoder list vs doc 11; size/perf measures | PASS: license-cleared list shipping; size ≤ NFR-PERF-009 revised target; builds reproducible | ADR-010, NFR-PERF-009 re-base, doc 11 sign-off items |
| RQ-03 | HEVC 10-bit/AV1 coverage on D1–D3 + behavior when only sw decode exists | Coverage concentrated on newer devices; D1 fails Main10 gracefully | Device matrix probes with fixtures; battery/thermal envelope for sw paths | PASS: graceful taxonomy on all; ≥consistent hw on D2/D3 classes; documented fleet matrix | Doc 07 profile tiers, FR-PLAY-014 messaging |
| RQ-04 | Dolby handling (AC-3/E-AC-3/DTS/TrueHD/DV) lawfully+technically | Passthrough-first works where routes exist; bundled audio legal only per doc 11 | Route matrix (speaker/BT/HDMI-cast out of scope?), legal review meetings ⚖️ | PASS: policy table frozen with legal sign-off; honest UX for all outcomes | ADR-010 list, FR-AUD-007, doc 07 §7/§9 |
| RQ-05 | Scoped-storage/SAF performance at 10k+ items on slow SD | Targets NFR-PERF-005 achievable with batched scan design | Synthetic corpora + harness; measure scan/thumb at scale | PASS: NFR-PERF-005 targets on D2, within +100% on SD | Doc 09 scan architecture, FR-LIB-004 confidence |
| RQ-06 | ASS/SSA styling fidelity ceiling | Media3-native under-shoots libass on styled scripts; libass integration is feasible | Render comparison on styled fixtures; integration cost estimate | PASS: native within agreed quality bar → ship native. Else: libass integration plan with cost/date range | ADR-003, DH-2 positioning, doc 07 §8 |
| RQ-07 | State restoration correctness across lifecycle edge matrix | Media3 + our session model restores ≥NFR-REL-005 | Automated kill/rotate/PiP/permission matrix | PASS: NFR-REL-005 target met | FR-PLAY-015 confidence, doc 09 model |
| RQ-08 | One-time unlock without backend (Play Billing offline behavior) | Works with documented caveats; regional pricing viable | Billing test track + refund/cancel matrix; policy read | PASS: unlock/restore robust in test matrix; no backend needed | Doc 15/16 monetization plan |
| RQ-09 | iOS viability gate (post-R1) | AVPlayer-only ceiling is too low for the promise; custom decode cost high | Re-derive doc 07 for iOS; cost model; review-risk review | PASS to proceed: value > cost with a privacy/simplicity angle | DEC-002 revisit |
| RQ-10 | Privacy-preserving diagnostics that still resolve real failures | Sufficient signal from local traces + redacted bundles | Failure-class experiment: can we diagnose top-5 classes from FR-DIAG-002 bundles? | PASS: ≥4/5 classes diagnosable without media content | Doc 14 viability |
| RQ-11 | DB/thumbnail architecture at scale | Room + Media3 Thumbnailer meets NFR-PERF-005/006/010 | Bench harness at 50k items | PASS: targets met on D2 | ADR-004 confidence, doc 09 |
| RQ-12 | HDR/VFR/refresh-rate behaviors "for free" vs to-build | Platform gives tone-mapping + VFR tolerance; refresh alignment is feature-flag territory | Fixture matrix on HDR-declaring devices; measure correctness qualitatively | PASS: no color corruption (FR-PLAY-013); refresh switch kill-switch validated | Doc 07 §9 tiers |
| RQ-13 | Compose performance on D1/API 26 | Targets achievable with baseline profiles + disciplined composables; else hybrid | Macrobenchmark suite on D1 devices | PASS: NFR-PERF-001/007 D1 targets | ADR-002 |
| RQ-14 | 32-bit ABI demand vs size cost | arm64-only acceptable for target fleet | Crash/device telemetry (store dashboards) + size analysis | PASS either way with documented tradeoff | ADR-009, NFR-COMP-002 |
| RQ-15 | Adaptive layout across breakpoint changes — rotation, foldable fold/unfold, split-screen, free-form windows | M3's breakpoint model + Android's canonical-layout state rules hold in practice; the 840dp+ two-pane layout loses no state and duplicates no pane | Instrumented UI tests across window-size transitions (compact↔medium↔expanded, mid-playback rotation, fold/unfold, split-screen resize), plus PiP entry/exit from a two-pane screen | PASS: layout follows the documented component/pane per class with no state loss and no lost selection; PiP enters correctly from every class. FAIL: any class loses state or renders a broken pane → fix before the pane layout ships (this is the empirical half of the doc 16 de-scope item 3) | Doc 06 §3, FR-CTRL-013, U1 residual risk |
| RQ-16 | Platform integration assumptions hold on retail builds — the media-session notification exemption, and gesture exclusion rects at the screen edge | The documented `POST_NOTIFICATIONS` exemption and the documented 200dp exclusion-rect behaviour both work as specified on retail builds and on at least one OEM skin; the scrubber is usable at the screen edge | On a device where `POST_NOTIFICATIONS` has never been granted: confirm the playback notification appears and lock-screen controls work. With system bars visible *and* stickily hidden: confirm exclusion rects are honoured at the left/right edge and that the scrubber thumb can be dragged inward from the edge | PASS: notification + controls present without the permission on all tested builds; scrubber reachable at the edge. FAIL: notification absent → **re-opens the permission decision** (FR-PLAT-002, NFR-PRIV-004) and weakens the DH-3 minimalism claim; exclusion ignored → widen edge insets and record the deviation | FR-PLAT-002, FR-CTRL-013, NFR-PRIV-004, DH-3 |

### 6.1 Prior art the spike owner should read first (from doc 02A)

These are **documented competitor behaviours**, not measurements — they tell the spike what to look for and what a credible answer looks like. None of them substitutes for our own fixtures.

- **RQ-01 / RQ-03 (engine + HEVC coverage):** Just Player publishes an explicit **hardware → software → unsupported** fallback table and a per-file decoder indicator; Next Player documents a tri-state decoder preference (auto / hardware / software) with a confirmation step before degrading a user's explicit choice (doc 02A §5, §8). Both imply the failure surface is **per-file, not per-device** — design the fixture corpus to prove the *class* of failure, and expect the taxonomy (FR-PLAY-004) to need more than "hw/sw".
- **RQ-06 (ASS fidelity):** our own positioning claims sit on this spike (ADR-003). Before accepting a quality bar, compare against what VLC and mpv-android actually render for styled ASS (positioning, fonts, karaoke, \t transforms) — a "close enough" bar set without looking at the reference implementation will understate the gap (doc 02A §3.4, §11.2).
- **RQ-12 (HDR/DV/refresh):** Infuse 8 exposes a **Dolby Vision profile-8 compatibility toggle** and a cache-mode control in its settings, which is direct evidence that DV handling is device-class-dependent and users are expected to intervene (doc 02A §9). Treat "works for free" as a hypothesis to falsify, and make sure DH-8's honest-messaging path has a UI home (SCR-010h).
- **Scoped storage (RQ-05):** Nova documents that enabling scoped storage **loses ASS subtitles, NFO metadata, and torrent support** (doc 02A §6). That is a real, shipped consequence of the storage model DEC-011 chose — the spike should measure whether our SAF path avoids it, not assume it does.

### 6.2 Notes added by the Part 32 audit (no hypothesis or threshold changed)

- **RQ-01's measurement set corrected.** The method cell cited NFR-PERF-003/004/**006**; 006 is *search latency*, which has no bearing on codec conformance. The set is now 003 (first-frame) / 004 (seek). Nothing else changed.
- **RQ-03's D1 procurement rule.** D1's HEVC Main10 capability is **unverified** for the class. The hypothesis above ("D1 fails Main10 gracefully") must be tested with a **class-representative** device (doc 05 §1) — never one selected for a decode deficiency, which would manufacture the expected result (`08B` §3.1, rule 2). If Main10 works across the class, the hypothesis is falsified — that is a result.
- **RQ-12's refresh half — corrected (Part 34).** Part 32's §6.2 claimed no FR owns a refresh-rate switch. **That was wrong:** doc 04's **FR-PLAY-012** owns refresh-rate alignment (SHOULD · R1 · P2; the AC's kill-switch disables switching app-wide; `TST-RATE-01…04`). The erroneous claim came from a case-sensitive search that missed a capitalised heading, and it is withdrawn. What survives, narrower: FR-PLAY-012 does not state the kill-switch's *form*, and with no network in R1 (DEC-012) it can only be a shipped safe-list and/or a local control — the spike should end with that explicit decision (08A RQ-12).
- **RQ-10's bundles** are specified in **doc 14 §4** and exported through FR-DIAG-002/003 — cite the bundle spec, not the requirement ID.
- **Corpus sizing:** doc 12 §5 governs fixture composition (≥3 files per validated A/B-tier cell, generated and checksummed); the "30-file corpus" in the RQ-01 method is a v0 sizing that the plan supersedes.

## 7. Open questions

| # | Question | Owner |
|---|---|---|
| T1 | Exact version pins (Media3/FFmpeg/Compose/AGP) drifted by build start — pin + recheck viable | build phase |
| T2 | libass integration path details (JNI surface, font handling with OFL set) — only if RQ-06 needs it | 09/11 |
| T3 | Tunneling-mode playback implications for PiP/stats overlay on Media3 | 09 |
| T4 | Whether Chromecast/HDMI-out testing belongs to doc 12 matrix (out-of-scope vs cheap win) | 12 |
| T5 | Gradle/CI environment reproducibility spec (exact toolchain pins) | 15/16 |
| T6 | **RQ-06 has no pass criterion until the ASS "agreed quality bar" is written down** — an undefined bar is not a threshold (08A RB-5). Must exist *before* the comparison runs | 07/06 |
