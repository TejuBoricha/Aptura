# 13 — Performance and Benchmarking Protocol

**Version:** 0.1 (draft)
**Date:** 2026-10-19
**Status:** Draft. **No benchmark has been run; no performance number in this package is measured.** All targets are the provisional `[T]` values from doc 05, to be converted to measured baselines (or formally adjusted once) at Gate B (docs 12/18). This document owns the *methodology*; doc 12 owns harness implementation and suite scheduling.
**Consumes:** 05 (NFR targets), 07 (tiers — what is worth measuring per claim), 09 (architecture, to know what can regress), 12 (suites TST-SYNC/TST-LIBV/TST-DEG…, device matrix).
**Feeds:** 15 (store claims discipline), 18 (measured-evidence registry), build-phase CI baselines.

---

## 1. Purpose and claim philosophy

1. **No number ships before it is earned.** Every performance statement in UI, docs, or store copy (speed, battery, smoothness, startup, "4K plays fine on…") must point to a bench report produced under this protocol and recorded in doc 18.
2. **Conditions are part of the number.** A result is meaningless without device class, OS version, fixture, thermal state, and method. Every figure is quoted with its conditions or not at all.
3. **Classes, not slogans.** Results are reported per device class (D1/D2/D3, doc 05) — never as universal claims (DEC-010, doc 07 §13).
4. **Regression defense, not marketing theater.** The primary consumer of these numbers is the engineering process (baseline + drift gates), not the store listing.
5. **No competitor benchmarks.** Comparative claims against named products are out of scope unless a controlled, pre-registered, reproducible test exists and counsel/product approves — default answer: don't (doc 02 §claims-to-avoid).

---

## 2. Measurement principles

| # | Principle | Consequence |
|---|---|---|
| M1 | Physical devices only for any number that could be quoted | Emulators allowed for functional tests, never for timing/power/thermal |
| M2 | Standardized device state before every run | Airplane mode on (R1 has no network anyway, this removes radio variance); battery ≥50% or powered *for non-battery tests*; brightness fixed & documented; all background apps killed; device cooled to ≤35 °C surface `[T]` |
| M3 | Battery tests are unplugged, screen-on, from a calibrated 100% or 80% start | Documented start level; no charging during run; battery-saver OFF unless that is the scenario |
| M4 | Warm-up runs are discarded | Macrobenchmark convention; explicitly documented per test |
| M5 | Statistical rigor | N≥5 runs per cell `[T]`; report median **and** p95; run-to-run coefficient of variation logged; a cell is "measured" only when variance <10% `[T]` or the instability is itself reported |
| M6 | Pre-registered outlier rule | Outliers dropped only by a stated mechanical rule (e.g., >3× median), never by human "feel" |
| M7 | Fixtures pinned | Benchmark fixtures = checksum-pinned subset of the doc 12 corpus; a fixture change invalidates baselines |
| M8 | Thermal context always logged | Surface/battery temp at start and end; throttling states noted (Android thermal status) |
| M9 | Fresh install vs. steady-state reported separately | Cold-start on first run differs from warm library; both are first-class metrics |
| M10 | Reports are versioned artifacts | Stored in build-era repo with device serial (anonymized), firmware, build hash |

---

## 3. Metric catalogue and per-metric method

| ID | Metric | Method | Conditions / instrumentation | Claims use |
|---|---|---|---|---|
| MB-01 | **Cold startup** | Macrobenchmark `StartupTimingMetric`, process killed between runs | Fresh install state + steady state, both reported | Possible claim (with class) |
| MB-02 | **Warm start / resume** | Same metric with warm process | After library load, after playback background | Internal |
| MB-03 | **Library load & scroll** | Macrobenchmark `FrameTimingMetric` on lists/grids at 1k/10k/50k fixture sizes | Debug-off release build; R8 on | Internal gate |
| MB-04 | **Scan throughput** | Harness: timed scan over synthetic libraries (1k/10k/50k), split into enumerate/extract/thumbnail stages | SD-card variant mandatory for slow-storage realism; screen-on; battery-saver scenario separate | Internal + possible honesty note ("library of N scanned in ~X on D2")—only if variance holds |
| MB-05 | **Scan power cost** | Battery histogram of a full scan over a pinned corpus | MB-03 device set; reported as %/1k files `[T]` | Internal |
| MB-06 | **Thumbnail pipeline** | Time-to-thumbnail for N items + total energy; tests cache hit/miss | Cold cache vs warm cache | Internal |
| MB-07 | **Seek latency** | TST-SEEK harness: time from input to first correct frame at N pinned positions, per container/codec tier | Keyframe vs non-keyframe split; hardware-decoded only for claim-bearing cells | Internal + possible claim for A-tier cells |
| MB-08 | **AV sync** | TST-SYNC harness (screen/audio capture rig, doc 12): drift trajectory + p95 offset over 30-min `[T]` playbacks | Per device class; per container/codec cell | Internal + possible claim (bounded wording) |
| MB-09 | **Playback power drain** | Screen-on, unplugged, pinned files (1080p H.264 + 4K HEVC where the class supports it), 60-min run, per-hour % drain | Silent playback (no audio route variance) + one loudspeaker pass; brightness 50% | Internal; used for honest battery wording only if repeated ≥3 devices/class |
| MB-10 | **Thermal / sustained performance** | 90-min `[T]` continuous playback + 30-min scrub-heavy stress; FPS drops and throttling states sampled | Warm ambient documented; D1 mandatory, D2/D3 as available | Feeds doc 07 tier honesty (e.g., 4K caveats) |
| MB-11 | **Memory** | Peak RSS + codec-instance count during playback sessions and 50k-library browse | Process-death adjacency checks | Internal gate |
| MB-12 | **Stability soak** | 4-h playback cycling mixed corpus; crash/ANR/wakelock counters | Nightly/weekly harness (doc 12 §8) | Release gate input |

*(MB IDs are internal references for reports and doc 18 evidence entries; they are not user-facing.)*

---

## 4. Environment and device regimen

- **Device set:** the doc 12 §6 matrix defines coverage; benchmarks officially run on the *bench subset* — at least one device per class D1/D2/D3, two per class where budget allows `[T]` — with serials anonymized in reports.
- **Storage matrix:** MB-04/MB-06 always include one portable/adoptable SD device; internal-only numbers are labeled as such.
- **Thermal preconditioning:** all devices cooled to documented start state (M2/M8); no benchmarks while charging unless the metric is explicitly a charging scenario.
- **Standard fixture set:** small (10 files), medium (1k), large (10k), XL (50k) libraries from generated corpus; codec sample set pinned per doc 07 A/B tiers.
- **Screen state:** locked brightness/stay-awake; rotation fixed; DND on; notifications cleared.

---

## 5. Statistical protocol and regression gates

| Item | Rule |
|---|---|
| Runs per cell | N≥5 `[T]`; report median + p95 + variance |
| Variance acceptance | CV <10% `[T]`; otherwise the cell is marked unstable and excluded from claims until fixed |
| Baseline storage | Per device × metric × fixture-size baseline files, versioned with firmware/build hash |
| Regression gate | Weekly macrobenchmark: >10% `[T]` median degradation vs baseline (same device/fixture/firmware) opens a P2 investigation; >25% or any MB-12 stability regression blocks RC |
| Noise floor | Established per device before first release (idle jitter, thermal drift) and re-checked after major OS updates |
| OS-update policy | Baseline invalidated on device firmware/OS update; re-baseline before comparing across the boundary |

---

## 6. From measurement to claim (claim policy)

1. A claim is eligible only after: MB cell measured per this protocol **and** doc 07 tier for the underlying capability is A/B (validated) **and** wording passes the doc 07 §13 audit.
2. Eligible claims follow the forms: per-class and bounded ("on D2-class devices"), never "always", "instant", "zero", "best", "all formats" (doc 06 §7 banned patterns).
3. Battery claims require ≥3 devices per class `[T]`, ≥3 repeats, and wording that names the test conditions (e.g., "~X%/h screen-on playback of 1080p H.264, silent, 50% brightness, device-class D2") — or we don't make the claim.
4. Thermal/4K caveats become *documented expectations* in Help (SCR-016) when measurements show class-dependent behavior.
5. Store listings and screenshots never contain a performance number that is not traceable to a report ID in doc 18.

---

## 7. Reporting artifacts

| Artifact | Content | Location / consumers |
|---|---|---|
| Bench report (internal) | Device, firmware, build hash, fixture checksums, per-metric table with N, median, p95, CV, thermal notes | Build-era repo; summarized in release notes for team; evidence row in doc 18 |
| Baseline pack | Machine-readable baselines per device | CI (weekly comparator) |
| Public note (optional) | Human-readable summary per class | Help/About page (SCR-016) if we choose to publish |
| Gate B evidence | The set of reports satisfying doc 05 NFRs, with waivers | Doc 18 readiness checklist |

---

## 8. Cross-references

- Targets: NFR-PERF-001…012, NFR-RES-*, NFR-REL-* (doc 05, all `[T]` until Gate B).
- Suites/harnesses: TST-SYNC, TST-SEEK, TST-LIBV, TST-DEG, soak in doc 12 §7–8.
- Tier promotions: doc 07 pipeline consumes MB-07/MB-08/MB-10 results for honesty tiers.
- Legal/claims: doc 07 §13, doc 11 (no claim depends on Forbidden components), doc 15 (store copy), doc 18 (evidence registry).

---

## 9. Open questions

| # | Question | Owner |
|---|---|---|
| Q13-1 | Exact ambient temperature and cooling procedure for the bench room/lab | 13 |
| Q13-2 | Battery test start level (100% vs 80%) and stopping rule | 13 |
| Q13-3 | Whether MB-04 scan target should be cited in-app as an honesty note at all, or remain internal | 13/06 |
| Q13-4 | Which class gets the first claim-bearing run set (recommend D2 mainstream) | 13/15 |
| Q13-5 | Screen-capture rig acquisition vs rental for MB-08 | 12/16 |
| Q13-6 | Public benchmark note: publish at all, or keep internal until R1.x | 15 |
