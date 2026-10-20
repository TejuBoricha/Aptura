# 05 — Non-Functional Requirements (NFRs)

**Version:** 0.5 (draft; every target is provisional and unmeasured; v0.5 updates NFR-LOC-003 to reflect global launch targeting per owner decision OD-5 in Part 39: English baseline UI at launch with worldwide store availability, multi-script subtitle support, and progressive UI localization; v0.4 removed the notification permission from NFR-PRIV-004; v0.3 completed Play vitals thresholds; v0.2 added FR-CTRL-017 auto-advance clause)
**Date:** 2026-10-20
**Status:** Draft. **No figure in this document is a measured result.** Every threshold is a provisional target (`[T]`), with rationale and verification method, to be validated or revised by doc 13 (benchmark protocol) and doc 12 (tests). Thresholds without direct evidence are additionally labeled `[E]`.
**Consumes:** 01 (scope/platform), 04 (FRs), `research/platform.md`, `research/licensing.md`, `research/competitors.md` (via 02).
**Feeds:** 06 (UI budgets), 09 (architecture constraints), 12 (test cases), 13 (measurement protocol), 18 (traceability).

---

## 0. How to read NFR records

Each record: **ID — Title** (MUST/SHOULD, release) · **Statement** (normative) · **Target** `[T]` · **Conditions** (device class, corpus, storage, power state) · **Rationale/evidence** · **Verification** (which protocol/test will measure it) · **Notes**.

**Rules:**
1. Targets are **engineering commitments, not marketing claims.** Public statements may only quote measured results from doc 13 — never these targets (DEC-010, doc 02 §7).
2. Device classes D1/D2/D3 (§1); where a class is unspecified, the target applies to **D2**.
3. Where the ideal measurement is impossible, the NFR names the closest **proxy** and says so.
4. Changing a published target requires doc 13 validation + change-impact assessment (doc 17 §change rules).

---

## 1. Device reference classes and test contexts

| Class | Representative profile `[E]` | Notes |
|---|---|---|
| **D1 — entry/low-end** | ~2019–2021 mid-range SoC class, 3–4 GB RAM, eMMC or slow UFS, 60 Hz 720–1080p | The class our PER-1 audience carries; targets relaxed where physics demands |
| **D2 — mainstream** | ~2021–2023 mid/upper SoC class, 6–8 GB RAM, UFS 2.x, 60–90 Hz 1080p | **Default baseline for all targets** |
| **D3 — high-end** | recent flagship class, ≥8 GB RAM, UFS 3+, 90–120 Hz | Stricter latency expectations; where 4K HDR is even feasible |

- Exact device list, firmware states, and purchase/binning rules: doc 12 §device matrix (final authority; classes here are for target-setting only).
- **Storage contexts:** internal (baseline); SD/removable (Class A1/V30-class card) with relaxed targets (stated per NFR).
- **Power/thermal contexts:** plugged + screen-off for batch work (scanning, thumbnails); battery-saver and thermally-throttled states tested for graceful degradation.
- **Corpus:** the doc 12 fixture corpus (codec tiers per doc 07, subtitle formats, adversarial files).
- **Play vitals context** — the documented bad-behaviour thresholds at which Google reduces app visibility `[V1]` (re-verified 2026-10-19; `research/interface_defaults.md` §6): user-perceived crash rate **1.09%** overall / 8% per phone model; user-perceived ANR **0.47%** / 8%; excessive partial wake locks **5%**; excessive battery usage 1%; bitmap memory **200 MB** (foreground / user-perceived / background), 400 MB cached; DEX optimisation ≥**25%** mandatory for apps with >10 MB DEX code — satisfied by the R8 + shrinking build in doc 08, so R8 is a store requirement rather than a preference. Android vitals flags "emerging issues" after 7 days and allows 21 days to address them. Several NFR-REL and NFR-PERF targets below are deliberately **stricter** than these flags (notably NFR-PERF-008: 250–500 MB against a 2.00 GB foreground flag on a 4–6 GB device).

---

## 2. NFR-PERF — Performance and responsiveness

#### NFR-PERF-001 — Cold start to usable library
**MUST · R1** · **Target `[T]`:** ≤1.5 s D2 / ≤2.5 s D1 (library-visible-and-scrollable; a fully-indexed state is not required to be interactive).
**Rationale:** first-run activation (JRN-01/02); incumbents' scan slowness is a known pain (doc 02 §6 #2). **Verification:** doc 13 startup protocol (N runs, p50/p90, cold-app-definition stated); doc 12 smoke set.

#### NFR-PERF-002 — Warm start / resume from background
**MUST · R1** · **Target `[T]`:** ≤400 ms D2 to interactive.
**Verification:** doc 13 protocol; automation per doc 12.

#### NFR-PERF-003 — First-frame latency
**MUST · R1** · **Target `[T]`:** ≤1.2 s (1080p H.264 8-bit, hw path) / ≤2.0 s (HEVC 10-bit, hw-capable) D2, from confirmed tap; ≤2.5 s D1; **SD card +50%**. Buffering-to-first-frame for slow storage is governed by NFR-PERF-004/007-adjacent states, not by pretending it played.
**Auto-advance clause (FR-CTRL-017):** the same budget applies to the **next item's first frame measured from `ENDED`**, because the transition reuses the live session rather than paying cold-start cost; if the next item needs a different engine configuration (codec/audio path change), the budget is 1.5× and the countdown must cover the gap rather than showing a frozen last frame with no feedback.
**Verification:** doc 12 fixture suite + doc 13 protocol (percentiles over ≥30 runs/file-class).

#### NFR-PERF-004 — Seek responsiveness
**MUST · R1** · **Target `[T]`:** scrub feedback ≤100 ms; frame-at-target ≤600 ms hw path (codec-dependent; keyframe-remap cases documented honestly per FR-PLAY-005); accuracy defined in doc 13.
**Verification:** doc 12 seek suite; distribution report per codec class.

#### NFR-PERF-005 — Library bootstrap and rescan
**MUST · R1** · **Target `[T]`:** first scan of 10,000 items ≤90 s D2 (background, UI stays ≥55 fps), ≤180 s D1; no-change incremental rescan ≤2 s; single-changed-folder rescan ≤5 s. **SD card:** scan targets +100%, thumbnails deferred (FR-LIB-009).
**Verification:** synthetic-corpus harness (doc 12 §perf fixtures); doc 13 protocol.

#### NFR-PERF-006 — Search latency
**MUST · R1** · **Target `[T]`:** incremental first results ≤150 ms per keystroke at 50k indexed items D2; ≤400 ms D1; no dropped keystrokes.
**Verification:** doc 12 search benchmark fixture; p90 reported.

#### NFR-PERF-007 — Browse scrolling
**MUST · R1** · **Target `[T]`:** sustained ≥55 fps (D2) while scrolling 10k-item grids with thumbnails loading; no single frame >100 ms (jank definition in doc 13); D1 ≥45 fps.
**Verification:** frame-timing capture protocol (doc 13); automated scroll suite (doc 12).

#### NFR-PERF-008 — Memory ceilings
**MUST · R1** · **Targets `[T]`:** library browsing ≤250 MB; 1080p playback ≤350 MB; 4K hw playback ≤500 MB (D2). **Leak gate:** no monotonic growth >10% over a 2-hour session (FR-PLAY-017).
**Verification:** doc 13 memory protocol; leak scenarios in doc 12.

#### NFR-PERF-009 — Install size
**SHOULD · R1** · **Target `[T]`:** base AAB download (arm64) ≤25 MB; ≤35 MB with FFmpeg audio extension + fonts `[E]` — **to be re-based after the doc 08 build spike (RQ-02)**; per-ABI splits mandatory.
**Rationale:** incumbents' bloat complaints; low-end storage pressure. **Verification:** build-toolchain spike measurement, then CI budget (doc 13 §size).

#### NFR-PERF-010 — Thumbnail pipeline
**MUST · R1** · **Targets `[T]`:** visible-tile render ≤50 ms; background generation ≥8 thumbs/s D2 while charging+idle, throttled/cancelled on battery saver (FR-LIB-009). No thumbnail work may violate NFR-PERF-007.
**Verification:** doc 12 thumbnail harness.

#### NFR-PERF-011 — Resume fidelity
**MUST · R1** · **Target `[T]`:** ≤2 s deviation from last true position after process death; save cadence ≤5 s or lifecycle event (FR-LIB-015).
**Verification:** kill/restore matrix (doc 12).

#### NFR-PERF-012 — Rotation and resize continuity
**MUST · R1** · **Target `[T]`:** no visible blank-frame >250 ms; decoder re-init (where platform forces it) ≤500 ms; position/state intact (FR-PLAY-010/015).
**Verification:** rotation suite (doc 12).

---

## 3. NFR-REL — Reliability

#### NFR-REL-001 — Crash-free sessions
**MUST · R1** · **Target `[T]`:** ≥99.7% crash-free sessions (internal gate; stricter than Play's 1.09% user-perceived flag `[V1]`). No public claim until doc 13 data exists.
**Verification:** Play vitals + opt-in diagnostics (DEC-008); internal pre-release soak.

#### NFR-REL-002 — ANR rate
**MUST · R1** · **Target `[T]`:** ≤0.10% (stricter than Play's 0.47% flag).
**Verification:** same as REL-001; strict main-thread rules in doc 09.

#### NFR-REL-003 — Playback success rate
**MUST · R1** · **Target `[T]`:** ≥99.5% of user-initiated local playbacks reach first frame; ≥99% complete without a fatal error on **validated** tiers (doc 07); failures on expected/best-effort tiers are handled per taxonomy, not counted as regressions.
**Verification:** in-app counters (local only; aggregate only with consent) + doc 12/13 suites.

#### NFR-REL-004 — Failure containment
**MUST · R1** · **Target `[T]`:** 100% of adversarial-corpus failures map to the FR-PLAY-004 taxonomy with actionable UI; zero unhandled exception classes across the doc 12 adversarial suite.
**Verification:** adversarial suite (doc 12); crash-class audit.

#### NFR-REL-005 — State restoration
**MUST · R1** · **Target `[T]`:** ≥99.9% successful restores across the lifecycle matrix (rotation, background, PiP, process death, permission change, SD removal).
**Verification:** doc 12 lifecycle matrix (deterministic automation).

#### NFR-REL-006 — Long-session stability
**MUST · R1** · **Target `[T]`:** 4-hour continuous playback: A/V drift within doc 13 tolerance; memory growth ≤10%; no decoder restart storms.
**Verification:** doc 13 soak protocol (lab device set).

#### NFR-REL-007 — Update and migration safety
**MUST · R1** · **Target `[T]`:** zero crash-loop classes in upgrade tests across the supported migration matrix; migration failure → safe mode with guidance (FR-LIB-007), never data loss of user preferences without notice.
**Verification:** upgrade matrix (doc 12); staged rollout gates (doc 15).

---

## 4. NFR-RES — Resources and efficiency

#### NFR-RES-001 — Battery during playback
**SHOULD · R1** · **Target `[T]`:** ≤6%/h for 1080p hw-decoded playback at ~35% brightness D2 `[E]` — **estimate to be replaced by doc 13 measurement; no public battery claims until then** (doc 02 §7 claim #3). Software-decode paths: publish only after measurement; expect materially higher drain (documented, not apologized for).
**Verification:** doc 13 battery protocol (screen-on, fixed brightness, airplane-on, audio route fixed).

#### NFR-RES-002 — Thermal behavior
**SHOULD · R1** · **Target `[T]`:** ≤1% dropped frames attributable to thermal events across a 30-min 1080p session D2/D3 `[E]`; graceful quality notices if a device throttles (honest UI, no silent stutter excuse).
**Verification:** doc 13 thermal protocol.

#### NFR-RES-003 — Cache and log caps
**MUST · R1** · **Targets `[T]`:** thumbnail cache default ≤150 MB (user-adjustable, LRU); logs ≤10 MB / ≤7 days (FR-DIAG-003); library DB ≈O(2 KB) per item `[E]`.
**Verification:** storage audit (doc 12).

#### NFR-RES-004 — Network silence
**MUST · R1** · **Target `[T]`:** 0 bytes of non-store network traffic in R1 (store infra artifacts like Play services excluded; app-originated = zero). Privacy proof per doc 10.
**Verification:** network capture suite (TST-PRIV-NET-*); code-path audit.

#### NFR-RES-005 — Power artifacts precision
**MUST · R1** · **Target `[T]`:** wake locks/FGS active only in playing/buffering states (FR-PLAT-003/005); zero wake-lock leaks after process death; idle wakeups below doc 13 threshold `[E]`.
**Verification:** automation + doc 13 idle-drain protocol.

#### NFR-RES-006 — Storage write discipline
**MUST · R1** · **Target `[T]`:** user media touched read-only (hash-verified, FR-SET-002 tests); app writes batched/rate-limited; no write-amplification patterns on removable storage.
**Verification:** doc 12 storage-fault suite + hash checks.

#### NFR-RES-007 — Background work discipline
**MUST · R1** · **Statement:** all batch work (scan, thumbnails, cleanup) runs via constrained schedulers (charging/idle where applicable), is cancelable, and never degrades active playback (FR-LIB-004/009).
**Verification:** scheduler audit + doc 13 interference protocol.

---

## 5. NFR-COMP — Compatibility and portability

#### NFR-COMP-001 — OS range
**MUST · R1** · **Statement:** Android 8.0 (API 26) through current (target-API 36 compliance `[V1]`, doc 01 §7); a documented behavior matrix per API band (26–28, 29–30, 31–32, 33, 34–35, 36+): permissions, storage, FGS, PiP, notifications.
**Verification:** doc 12 OS-band matrix; store pre-launch report.

#### NFR-COMP-002 — ABI coverage
**MUST · R1** · **Statement:** arm64-v8a required; armeabi-v7a per doc 08 size/telemetry decision `[T]`; no production x86.
**Verification:** build config gate.

#### NFR-COMP-003 — Screen and input coverage
**MUST · R1** · **Statement:** 4.7"–13" displays, portrait and landscape; foldables/cutouts must not break any core journey (no special foldable features promised); gesture-nav safe areas respected.
**Verification:** doc 12 layout matrix (emulator + device classes).

#### NFR-COMP-004 — Media compatibility governance
**MUST · R1** · **Statement:** user-facing capability never exceeds doc 07's validated tiers (DEC-010); each release re-checks the matrix against engine changes.
**Verification:** release gate checklist (doc 18).

#### NFR-COMP-005 — Removable storage
**MUST · R1** · **Statement:** full library journeys on SD-class storage with the relaxed performance targets stated above; ejection/reinsertion handled per FR-LIB-021.
**Verification:** doc 12 storage matrix.

#### NFR-COMP-006 — Locale and script readiness
**MUST · R1** · **Statement:** all UI strings externalized; RTL mirroring complete; subtitle script coverage per FR-SUB-005 (NFR-LOC-* detail).
**Verification:** pseudo-locale run + RTL slot in doc 12.

#### NFR-COMP-007 — Theming
**MUST · R1** · **Statement:** light/dark/system themes; contrast maintained in both (NFR-A11Y-002); OLED-friendly pure-dark option MAY.
**Verification:** visual audit per doc 06 spec.

#### NFR-COMP-008 — Capability gating without stale lists
**MUST · R1** · **Statement:** feature gating uses runtime capability detection/device-class signals where available (FR-PLAY-003); hardcoded device lists are allowed only in **test infrastructure**, never as silent product logic `[T]`.
**Verification:** architecture review (doc 09) + code audit.

---

## 6. NFR-A11Y — Accessibility

#### NFR-A11Y-001 — Screen-reader operability
**MUST · R1** · **Statement:** all core journeys (onboard, browse, play, tracks, settings, error recovery) operable with TalkBack; labels/roles/states complete; no focus traps; live regions used sparingly (FR-SUB-008).
**Verification:** TalkBack journey suite (doc 12); accessibility scanner.

#### NFR-A11Y-002 — Touch targets and contrast
**MUST · R1** · **Statement:** interactive targets ≥48×48 dp; UI text/controls meet WCAG 2.2 AA contrast (≥4.5:1 normal text); color never the sole carrier of meaning (state, errors, watched markers).
**Verification:** audit in doc 12 A11Y track; doc 06 design tokens.

#### NFR-A11Y-003 — Font scaling
**MUST · R1** · **Statement:** 200% font scale usable across settings/details/lists/subtitle UI without loss of critical function; if a screen must reflow differently, doc 06 defines how.
**Verification:** doc 12 font-scale matrix.

#### NFR-A11Y-004 — Motion and haptics respect
**MUST · R1** · **Statement:** reduce-motion honored (no decorative motion); haptics only when enabled system-wide AND in-app; no essential information conveyed only via motion.
**Verification:** settings-matrix tests.

#### NFR-A11Y-005 — Non-touch operation
**MUST · R1** · **Statement:** keyboard/D-pad/media keys cover all core playback actions (FR-CTRL-014); focus order logical in all dialogs.
**Verification:** input-device suite (doc 12).

#### NFR-A11Y-006 — Captions and subtitles accessibility
**MUST · R1** · **Statement:** subtitle defaults accessible (FR-SUB-008); platform caption preferences respected where feasible; no caption-only critical information.
**Verification:** caption audit.

#### NFR-A11Y-007 — Timeout flexibility
**MUST · R1** · **Statement:** no time-out trap: control auto-hide adjustable (FR-CTRL-016); error/dialog surfaces stable and dismissible deliberately, never on a hidden timer.
**Verification:** interaction audit.

#### NFR-A11Y-008 — Orientation flexibility
**SHOULD · R1** · **Statement:** core journeys complete in both orientations; lock modes do not strand a user in an unusable configuration.
**Verification:** doc 12 layout matrix.

---

## 7. NFR-SEC — Security

#### NFR-SEC-001 — Untrusted-input posture
**MUST · R1** · **Statement:** media files, subtitle files, intents/URIs, and filenames are treated as untrusted; parsers run with resource caps (FR-SUB-007); fuzzable surfaces enumerated in doc 10 THR register; no parser may block the UI thread unboundedly.
**Verification:** adversarial suite (doc 12) + doc 10 review.

#### NFR-SEC-002 — Attack-surface minimization
**MUST · R1** · **Statement:** only required components exported; no dynamic code loading; no runtime artifact downloads; FGS usage Play-compliant (FR-PLAT-003); deep links/VIEW intents validated before use.
**Verification:** manifest audit + doc 10 checklist.

#### NFR-SEC-003 — Secrets and signing
**MUST · R1** · **Statement:** no secrets in repo/build artifacts; Play App Signing; signing keys offline; CI uses least-privilege credentials.
**Verification:** repo scan + release checklist (doc 15).

#### NFR-SEC-004 — Dependency hygiene
**MUST · R1** · **Statement:** pinned dependency versions; quarterly CVE/license review (doc 11); SBOM generated per release (CISA 2026 v2.1 fields, CycloneDX/SPDX — W3).
**Verification:** CI artifact check.

#### NFR-SEC-005 — Local data protection
**MUST · R1** · **Statement:** library metadata/history/thumbnails in app-private storage; no cross-app exposure; optional app-lock (biometric) MAY for R2 `[T]`; no cloud sync in R1.
**Verification:** storage audit (doc 10).

---

## 8. NFR-PRIV — Privacy

#### NFR-PRIV-001 — Local-first data inventory
**MUST · R1** · **Statement:** a complete local data inventory (doc 10 §inventory) covers every stored item (library records, history, positions, prefs, thumbnails, logs) with purpose and retention; nothing leaves the device by default; no account exists.
**Verification:** doc 10 inventory review + network proofs.

#### NFR-PRIV-002 — Network silence (proof-level)
**MUST · R1** · **Statement:** as NFR-RES-004; additionally the Play Data-safety form and (future) Apple labels must exactly match the inventory — no aspirational claims (doc 15 gate).
**Verification:** doc 10/14 proofs + store-form review.

#### NFR-PRIV-003 — Consent and revocation
**MUST · R1 (design)** · **Statement:** any future diagnostics/telemetry requires plain-language opt-in (DEC-008), is revocable, and deletion actually deletes; revocation stops collection immediately.
**Verification:** design review + fixture tests when implemented.

#### NFR-PRIV-004 — Permission minimalism
**MUST · R1** · **Statement:** request only: media read (per OS band) and the **foreground-service** permission(s) required for background audio; **no notification permission is requested or declared** — media-session notifications are exempt from `POST_NOTIFICATIONS` per Android's documented exemption list, and that exemption is preserved by the R1 rule that no non-media notification is ever posted (FR-PLAT-002). Each request carries an in-context explanation (FR-LIB-002); no location/contacts/identifiers beyond what stores require.
**Verification:** manifest audit + onboarding tests, **including a negative check that no `POST_NOTIFICATIONS` declaration exists**.

#### NFR-PRIV-005 — Children and family policy
**MUST · R1** · **Statement:** product is not directed at children; no accounts/collection; store family-policy compliance reviewed in doc 10; content rating setup truthful (doc 15).
**Verification:** store-forms review + doc 10.

#### NFR-PRIV-006 — User-controlled deletion
**MUST · R1** · **Statement:** users can clear history/caches/derived data and wipe app data effectively (FR-SET-004); deletion verified to remove thumbnails, DB, and logs.
**Verification:** deletion audit (doc 12 fixture test).

---

## 9. NFR-LOC — Internationalization and localization

#### NFR-LOC-001 — Externalized, ICU-correct strings
**MUST · R1** · **Statement:** no hardcoded UI strings; plurals/genders via ICU; no sentence concatenation; translator context notes maintained.
**Verification:** lint gate + pseudo-locale run.

#### NFR-LOC-002 — RTL and BiDi
**MUST · R1** · **Statement:** complete RTL layout mirroring (Arabic/Hebrew/Urdu); BiDi correctness in lists, details, and subtitle rendering (FR-SUB-005).
**Verification:** RTL slot in doc 12; native-reviewer pass at launch locale expansion.

#### NFR-LOC-003 — Launch locales and global targeting
**SHOULD · R1** · **Statement:** Targeted globally from Day 1 (owner decision OD-5, Part 39) with worldwide Google Play distribution. App ships with an English UI baseline at launch; UI strings externalized (NFR-LOC-001) for progressive localization rollouts (R1.x+); complete international subtitle rendering support across global scripts (Latin extended, Arabic RTL, CJK, Devanagari, Cyrillic) supported from Day 1.
**Verification:** localization manifest and Play Store console distribution configuration per release (doc 15).

#### NFR-LOC-004 — Locale-aware formatting
**MUST · R1** · **Statement:** dates, sizes, durations, times, and numbers formatted per system locale; 12/24-hour preference honored.
**Verification:** format matrix test.

#### NFR-LOC-005 — Content-review safety
**MUST · R1** · **Statement:** strings/screenshots culture-tolerant; no strings that read as UI without translation; screenshot strings avoid cut-off-prone compounds.
**Verification:** screenshot review per release (doc 15).

---

## 10. NFR-OPS — Operability and maintainability

#### NFR-OPS-001 — Build and release integrity
**MUST (process)** · **Statement:** reproducible CI builds; lint + unit + emulator-smoke gates; signed AAB per Play rules `[V1]`; target-API compliance maintained ahead of deadlines `[V1]`; versioned SBOM artifact.
**Verification:** CI configuration review (doc 15).

#### NFR-OPS-002 — Rollout and containment
**MUST (process)** · **Statement:** staged rollout with explicit halt criteria tied to NFR-REL gates and the doc 17 risk register; in R1 there is **no remote config server** — containment = rollout halt + hotfix + packaged feature flags `[E]`.
**Verification:** release runbook (doc 15).

#### NFR-OPS-003 — Monitoring with consent only
**MUST (process)** · **Statement:** health monitoring = store vitals + consented diagnostics (DEC-008); no third-party trackers; alert thresholds internal (map to NFR-REL-001/002).
**Verification:** doc 14 plan review.

#### NFR-OPS-004 — Migration discipline
**MUST (process)** · **Statement:** schema/format migrations versioned, tested on upgrade matrices (NFR-REL-007); forward-fix strategy documented; no silent data loss classes.
**Verification:** upgrade suites (doc 12).

#### NFR-OPS-005 — Supportability
**SHOULD (process)** · **Statement:** offline error-code lookup (FR-SET-007) + redacted report flow (FR-DIAG-004); taxonomy codes used consistently in triage labels `[E]`; response-time commitments deferred to doc 15.
**Verification:** support dry-run per release candidate.

#### NFR-OPS-006 — Maintainer documentation
**SHOULD (process)** · **Statement:** module docs + ADRs updated as part of definition-of-done (doc 17); architecture drift reviewed quarterly.
**Verification:** DoD checklist usage.

---

## 11. NFR ↔ KPI ↔ verification map (summary)

| NFR group | Primary KPIs | Verification owner |
|---|---|---|
| PERF | KPI-PERF-01…06 | doc 13 protocol + doc 12 suites |
| REL | KPI-REL-01…06 | doc 12 + release gates |
| RES | KPI-PERF-07 (battery/thermal), PRIV proofs | doc 13 + doc 10 |
| COMP | KPI-REL-02 (compat failures) | doc 12 matrix |
| A11Y | KPI-SAT-0x (accessibility items) | doc 12 A11Y track |
| SEC/PRIV | KPI trust proofs | doc 10 proofs |
| LOC | launch-locale readiness | doc 15 release checklist |
| OPS | process gates | doc 15/18 |

*(KPI IDs above are the groups defined in doc 03 §6; doc 18 carries the full per-requirement traceability.)*

## 12. Open questions

| # | Question | Owner |
|---|---|---|
| NQ1 | Battery target needs real calibration — no admissible competitor data exists (W1/W4); doc 13 must define the ambient protocol precisely | 13 |
| NQ2 | AAB size with FFmpeg audio extension + fonts — re-base target after build spike (RQ-02) | 08/13 |
| NQ3 | D1 feasibility for 10k-library scans + thumbnails (may need relaxed tier or feature limits) | 13/09 |
| NQ4 | 200% font scale on 4.7" screens may force alternate layouts — doc 06 must resolve | 06 |
| NQ5 | Play vitals rate stability at small install bases — interpretation rules for early launches | 15 |
| NQ6 | FGS/audio-focus behavior differences on Android 15–17 (hardening trajectory) — keep matrix current | 09/10 |
