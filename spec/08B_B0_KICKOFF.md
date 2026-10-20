# 08B — B0 Kickoff Pack (Owner Decisions, Device Procurement, Week One)

**Version:** 0.5 (draft — nothing ordered, nothing executed); v0.5 records the **owner directive on OD-5 (Part 39)** — targeted globally from Day 1 with worldwide Google Play distribution, English baseline UI at launch, full multi-script subtitle rendering (Arabic RTL, CJK, Devanagari, Cyrillic), and progressive UI translation rollouts; v0.4 recorded the Part 38 delegated decisions; v0.3 made the device order executable (§3.2.1 purchase worksheet, §3.4.1 intake probe specification); v0.2 applied Part 32 audit fixes.
**Date:** 2026-10-20
**Status:** Draft, docs-only. This is the **action layer** for the spike program: it converts the pending owner decisions (doc 00 §5), the device-lab dependency (DEP-005) and the first week of B0 into things a person can actually do on a Monday morning. It decides nothing itself.
**Companion status:** numbered `08B` alongside `08A`, following the `02A` precedent — the master prompt's deliverable set (00–18) is unchanged and doc 18's §22 audit remains 24/24.
**Consumes:** 00 §5 (OD table + defaults), 08 §6 + 08A (§spike definitions and runbooks — the source of every device requirement here), 12 §5/§6 (corpora and device matrix), 13 (M1/M8 — why farms are useless for our questions), 16 §2/§3/§7 (B0, DEP register, critical path), 11 §5 (L1/L2), 15 §4 (S1/S2/S8).
**Feeds:** the owner (OD answers), DEP-005 purchase order, doc 00 §5 (answers recorded), doc 16 (calendar conversion once OD-4 lands).

---

## 1. What this document is, and is not

| It is | It is not |
|---|---|
| A **decision brief** for each pending OD, written so the answer takes one line | A recommendation to proceed with the build — that gate is the owner's, not this document's |
| A **capability specification** for the device lab, not a shopping list of unverified models | A purchase order, a budget, or a claim about any device's real behaviour |
| A **week-one sequence** derived from the B0.0 finding (08A §2) | A schedule. Calendar conversion applies doc 16 §2.1's 2-engineer working case (Part 38) |
| A place to record answers that already have defaults | A re-opening of decisions already made and defaulted |

**The honest situation (updated, Part 38):** the specification is complete and audited; the spike program is written and runnable; **the decisions are recorded (delegated — §5) but no hardware exists yet.** One external dependency still blocks B0: **DEP-005** — the lab gates the 11 device-dependent spikes (the count is *derived*, not asserted: 08A §3). **OD-4's questions are answered by delegation** (2 engineers full-time; no hard date — Q16-7 = 2). Everything else in B0 can be prepared now.

---

## 2. Owner decision briefs (OD-1…OD-6)

**Status (Part 38): every OD below now carries a delegated decision — see the filled answer sheet in §5.** The briefs remain the rationale for each.

Each brief states the question, why it is being asked *now* rather than later, the realistic options with their consequences, our recommendation, what the answer unblocks, and the cost of leaving it unanswered. **Defaults are in force meanwhile** (doc 00 §5) — an unanswered OD is not a blocked OD unless its row says so.

---

### OD-1 — Platform order: Android-first, both, or iOS-first
**Default in force:** Android-first; iOS only at an R1.x gate (DEC-002).

**Why now:** this is not a marketing question, it is an *engine* question. On iOS, playback goes through AVPlayer, so the entire differentiator — decode election, per-file honesty, the A/B/C tier matrix — is capped by what AVPlayer exposes. Closing that gap means bundling a custom decoder: a different cost class entirely (licensing, size, review risk) and the subject of RQ-09.

| Option | Consequence | Verdict |
|---|---|---|
| **Android-first; iOS decided at an R1.x gate** | Single-platform build; RQ-09 runs in parallel anyway (0.5 wk `[E]`, no device needed) and its answer arrives before it costs anything | **Recommended** |
| Both from day one | `+6–10 dev-weeks` `[E]` platform costs (doc 16 §6 item 5) *and* RQ-09 must be answered **before** build start, not after; device lab doubles; the differentiator is spread thinner on both platforms | Not recommended at this team size |
| iOS-first | Contradicts the evidence: iOS has a strong built-in player and entrenched paid incumbents (doc 02 §2.4) | Not recommended |

**Unblocks:** B1 module skeleton (single- vs multi-platform), DEP-005 device scope, RQ-09 scheduling.
**Cost of delay:** low — the default is the recommended answer, and RQ-09 can be executed at any time without a device.

---

### OD-2 — R1 boundary: local-only, or include network playback (SMB/NAS/HTTP)
**Default in force:** local-only; network is an R2 candidate (DEC-001).

**Why now:** this is the largest scope decision in the package — doc 16 sizes SMB/NAS/HTTP at `10–20 dev-weeks` `[E]`, **comparable to the player core and subtitle depth combined (B3+B4 = 12–19 `[E]`), not to R1 as a whole (34–56)**. It also has *non-size* consequences that are easy to miss:

- **It breaks a standing constraint.** Network playback needs `INTERNET`, which R1 currently does not request at all (DEC-012). The privacy posture, the data-safety form, and the "nothing leaves your device" claim are all built on its absence.
- **It adds a permanent security surface** (doc 10 §6 threat register) and a support burden (credentials, share discovery, permission failures on NAS boxes).

| Option | Consequence | Verdict |
|---|---|---|
| **Local-only for R1** | Shipable scope; privacy claim stays unconditional; network stays a gated R2 candidate with its own security review | **Recommended** |
| Include network in R1 | Roughly doubles R1; adds `INTERNET`, a threat surface, and a class of support issues with no device-side fix | Not recommended for R1 |
| Defer the decision | Acceptable — but the *module skeleton* choice (B1) should then reserve a boundary, and doc 09's no-network rationale is written to hold either way | Acceptable |

**Unblocks:** B1 module boundaries, the store listing's core claim, doc 10's threat model scope.

---

### OD-3 — Monetization at launch
**Default in force:** free, no ads (DEC-004).

**Why now:** it decides whether **RQ-08** (Play Billing without a backend) is an R1 dependency or an R1.x candidate (DEP-010). Billing at launch means store product configuration, refund/cancel/chargeback handling, and entitlement messaging all become launch-blocking — and RQ-08's elapsed time is *store review*, not engineering.

| Option | Consequence | Verdict |
|---|---|---|
| **Free at launch; paid unlock hypotheses validated later** | R1 has no billing dependency, no refund support load; RQ-08 runs without a deadline; the owner can still monetize in R1.x with evidence | **Recommended** |
| Paid unlock at launch | RQ-08 becomes a launch gate; adds payment-related support and compliance surface; must ship *before* any evidence of willingness to pay | Higher risk at this stage |
| Ads | Conflates the product with the exact experience the research identifies as the incumbents' most-hated trait (doc 02 pain-point synthesis) | **Contradicts the positioning** |

**Unblocks:** RQ-08 timing, Play Console product setup, store copy, doc 15's option table.

---

### OD-4 — Team, budget, and time
**Default in force:** ranges in `[E]` dev-weeks only (doc 16).

**Why now — and why this is the one to answer first:** every size in the package is in dev-weeks precisely because headcount is unknown. Without it there is no calendar, no way to know whether the de-scope ladder (doc 16 §5) needs to be exercised, and no answer to **Q16-7**, the question that decides whether B0's 4–8 week range is reachable at all (08A §5: spike timeboxes alone sum to ≈18 dev-weeks sequentially).

The four things actually needed — a rough answer beats a careful one:

1. **Engineers**: how many, and are they full-time on this? (A range is fine: "1–2", "3".)
2. **Design**: is there a designer at part-time hours (DEP-011)?
3. **Window**: is there a target date, and is it a hard external commitment or an internal preference?
4. **Spend ceiling**: the two cash items in the plan are the **device lab** (DEP-005) and **legal counsel** (L1/L2). A ceiling of "some" is enough to decide whether the lab is 3 devices or 6.

**What this unblocks:** calendar conversion of every range; B0 concurrency; lab size; whether the launch window is plausible at all.
**Cost of delay:** the plan continues in dev-weeks, but **B0 cannot be scheduled**, and the device order and legal engagement — both with external lead times — stay unsent.

---

### OD-5 — Launch regions and languages
**Decision (Owner Directive, Part 39):** **Targeted globally from Day 1** — worldwide Google Play store distribution with English UI baseline at launch, complete international subtitle script rendering (Latin extended, Arabic RTL, CJK, Devanagari, Cyrillic), and progressive UI translation additions (R1.x+).

**Why now:** it is not just translation. Regions determine **S2** developer-verification rollout (doc 15 §4), the statute set that legal must consider (L5), content rating, and — the one that touches engineering — the **OFL font subsets** needed for subtitle rendering (OQ-5).

**Global posture (confirmed):** Because the app is local-only with no backend, no accounts, and no `INTERNET` permission (DEC-012, DEC-004), worldwide distribution carries minimal backend compliance overhead. L5 scopes to global statutes (GDPR, CCPA, DPDP, LGPD, EAA) concurrently; UI starts in English with ICU-clean externalized strings (NFR-LOC-001) for gradual localization.

**Unblocks:** S2 sequencing, font subsetting (OQ-5), L5 scope, store listing work (S3).

---

### OD-6 — Product/brand name
**Default in force:** working title TBD.

**Why now:** trademark clearance (L6) is a long-lead with a real chance of failure, and the `applicationId` should be pinned once — **changing it later means a new store listing**, losing install base, reviews and ranking.

**Practical ask:** give **two or three** candidate names so L6 can clear them in parallel and at least one survives.

**Unblocks:** L6, S1 (store account identity), `applicationId` pinning.

---

## 3. DEP-005 — device-lab procurement specification

**Principle: buy the *edges* of the supported envelope, not the middle.** Mid-range phones are the easiest devices to borrow; the low end and the outliers are the ones that fail, and they are the reason RQ-13 and RQ-03 exist at all.

**Two layering rules that make §3.1 readable (Part 32, D-21/D-27):**

1. **Doc 05 §1 defines the device classes. This section adds procurement requirements on top — it does not redefine them.** Where a row below states a capability (decode support, refresh rate, HDR), that is a requirement *we impose when buying*, not part of the class definition. Where it repeats doc 05's hardware-era figures, those figures are doc 05's, quoted.
2. **Never buy a device for a decode deficiency.** RQ-03's hypothesis is that the D1 class fails HEVC 10-bit gracefully; selecting a device *because* it cannot decode Main10 would manufacture that result. Class-representative devices only — the spike records what the class actually does.

### 3.1 Minimum fleet (capability requirements, derived from 08A §5)

| # | Role | Capability requirement | Why this device, and not another | Spikes served |
|---|---|---|---|---|
| 1 | **D1 — the floor** (class per doc 05 §1: ~2019–2021 mid-range SoC, **3–4 GB RAM**, eMMC or slow UFS, 60 Hz 720–1080p) | Class-representative hardware, **plus**: no capability assumptions — the device's decode support is *probed at intake*, never assumed and never selected for a deficiency (D-27) | This device *is* RQ-13's entire question (does Compose hold on the floor?) and RQ-03's honesty path | RQ-01, RQ-03, **RQ-13**, RQ-15 |
| 2 | **D2 — the mainstream** | API 30–33 class; HEVC Main10 hw decode; 1080p; ≥6 GB | Most users live here; all D2-targeted NFR pass/fail is judged here | RQ-01, RQ-03, RQ-05, RQ-11, RQ-15 |
| 3 | **D3 — the frontier** | API 34–36 class; AV1 hw decode; HDR-declaring | Establishes what the *good* case looks like and whether D1's failures are device class or app design | RQ-01, RQ-03, RQ-12, RQ-15 |
| 4 | **SD-bearing** | microSD slot, supporting **portable** (and ideally adoptable) use | Slow-storage realism cannot be emulated; **verify the slot exists before buying — many flagship-class devices no longer have one** | RQ-05, RQ-11 |
| 5 | **High-refresh** | ≥90 Hz display (may coincide with #3) | Refresh-alignment behaviour is unmeasurable otherwise | RQ-12 |
| 6 | **OEM-skinned ×2** | At least two manufacturer skins (e.g. Samsung-class and Xiaomi-class, may coincide with #2/#3) | Gesture, notification and PiP variance is a **skin** property, not an OS property — RQ-16's whole point | **RQ-16**, RQ-15, RQ-01 |
| 7 | **OS-band coverage** | At least one device in each of doc 12 §6's **six** bands: 26/27 · 28–29 · 30–32 · 33 · 34–35 · 36 | doc 12 §6 matrix; behaviour changes cluster at band boundaries. **Class and OS band are separate axes** — a D1-class device may run a newer OS | all |

**Minimum realistic purchase for B0:** 3–4 devices if roles coincide (D1 + D2 + D3/HDR/high-refresh + one different-skin device **with an SD slot**), 6 if they do not. The D1 device is non-negotiable and is usually the cheapest item on the list.

**But the B0 lab is not the whole fleet — plan one fleet that grows (Part 32, D-23).** Doc 12 §6's matrix requires, for release-time `TST-CMPT` work: **six OS bands**, internal + portable + adoptable SD, 60 Hz *and* ≥90 Hz, cutout/notch, HDR-declaring and one DV-declaring device, **three** OEM slants (AOSP-like, Samsung-class, Xiaomi-class), and a locale set including RTL, CJK and Devanagari. A 3–4 device B0 purchase cannot satisfy that matrix, and buying twice pays lead time twice. See §3.5.

### 3.2 Selection criteria — and why no models are named here

We deliberately do **not** name specific models. Three reasons, and they are the same reasons doc 07 refuses to treat store listings as evidence:

1. **Unverified `[U]`.** We have not probed any device; naming a model would assert a decode capability we have not measured, in the document whose entire purpose is to avoid that.
2. **Region and time variance.** Availability differs by market and models turn over in months; the fleet must be selected at purchase time, not at spec time.
3. **Capability, not brand, is the requirement — and never a *deficiency*.** What the spikes need is a device that *represents a class* (doc 05 §1), not a particular product; and, per rule 2 above, never one chosen *for* a capability it lacks. (v0.2 used "a device without HEVC Main10 hardware decode" as this example — the exact circularity D-27 removed; corrected in v0.3.)

**Buyer's procedure:** take §3.1's capability rows → shortlist locally-available models whose **published** specs *suggest* they satisfy them → **buy one, probe it (§3.4.1), then extend to a set** only if the probe confirms the row it was bought for. Published specs shortlist; the probe decides.

#### 3.2.1 Purchase worksheet (one per candidate, filled at purchase time)

| Field | Candidate A | Candidate B | Candidate C |
|---|---|---|---|
| Model / region / availability | | | |
| Intended role (§3.1 row #) | | | |
| Class per doc 05 §1 — and why this candidate is class-representative | | | |
| OS band (doc 12 §6's six) | | | |
| microSD slot — portable or adoptable state? | | | |
| Display refresh capability (≥90 Hz?) | | | |
| OEM skin (AOSP-like / Samsung-class / Xiaomi-class) | | | |
| Price at purchase — **filled by the buyer; not researched here** `[U]` | | | |
| Closes an uncovered doc 12 §6 matrix item? (§3.5 rule) | | | |
| **Intake-probe verdict (§3.4.1) — pass / conditional / reject** | | | |

**Decision rule:** buy the smallest set that covers §3.1's rows, preferring candidates that also close uncovered matrix items (§3.5). A candidate that cannot be probed before purchase is bought **as one unit**, probed, and only then bought in quantity.

### 3.3 What we deliberately do not buy yet

| Item | Why not yet | Trigger to buy |
|---|---|---|
| **Dolby Vision-declaring device** | RQ-04's legal answer (L2/counsel) may make DV a passthrough-only path, in which case the device buys nothing | RQ-04 needs it *and* counsel confirms a DV surface we can act on |
| **Foldable** | Expensive, and RQ-15's breakpoint transitions can be exercised on emulators for the *logic* half (§1 S9); the physical half is a borrow/rent case | RQ-15 shows a physical fold is required to reproduce a failure |
| **Android TV / cast device** | Out of R1 scope (DEC-005); T4 answers whether cast testing belongs in the matrix at all | T4 answered "yes, cheap win" |
| **Second unit of the busiest class** | Only needed to de-serialize concurrent spike runs | Q16-7 answered "≥2 engineers on B0" |

### 3.4 Device-farm policy (say it once, apply it always)

Farms are a **supplement, never a substitute** (doc 16 §3). Concretely:

- **Farms may generate signals, never answers.** A farm may tell us *where to look* — "this file misbehaves on this class" is a **hypothesis generator**. Doc 12 §6 is explicit that a farm is "still not codec authority", so **a farm result can never close RQ-03**, promote a doc 07 tier, or appear in a claim.
- **Farms may not answer** anything about **timing, power, thermal or gesture/notification behaviour** — M1 requires physical, thermal-state-recorded devices for those, and virtual devices have different clocks, thermals and input paths (doc 13 M1, doc 12 §6).
- **Every result must name how it was obtained.** A farm result presented as a lab result is exactly the failure mode §1 S8 exists to prevent.

**Intake protocol (per device, before it counts as lab equipment):** record model, build number, firmware, region variant, display refresh capability, decode capabilities as *probed* (not as published), storage topology (internal/SD, portable/adoptable), and thermal behaviour under a fixed load. Freeze this as the device's lab-matrix record; every spike result cites it rather than restating it (08A §1 S8).

#### 3.4.1 The intake probe — what to record, and which documented surface answers it

The probe is the instrument that makes rule 2 enforceable: **published specs may shortlist a candidate; only the device's own report admits it.** Every value comes from a documented platform surface — this table is the *record shape*, not a frozen API contract, and the implementer verifies exact names and version gates against the SDK reference when building the probe in B0.0.

| Record | Documented surface to read | Needed by |
|---|---|---|
| OS/API level, build fingerprint, security patch level | `Build.VERSION`, `Build.FINGERPRINT` | doc 12 §6 band coverage; every result cites it (08A S8) |
| **Decode support per codec / profile / level, hw-accelerated or not** | `MediaCodecList` + `MediaCodecInfo.CodecCapabilities` | **RQ-01, RQ-03** — the value that replaces the spec sheet |
| Display refresh modes; panel HDR capability | `Display` supported modes / `Display.Mode`; panel HDR capability surface | RQ-12 (≥90 Hz), RQ-03 (HDR honesty paths) |
| Storage topology: internal / removable, portable or adoptable | `StorageManager` volume list, `StorageVolume` | RQ-05, RQ-11 |
| Audio outputs and routes available | `AudioManager` device list | RQ-04 route matrix |
| Thermal behaviour under a fixed timed load; temperature before/after | platform thermal-status surface (version-gated) + doc 13 M2/M8 protocol | RQ-03; MB-09/10; M8's thermal-context rule |
| RAM class; supported ABIs | `ActivityManager` memory info, `Build.SUPPORTED_ABIS` | class confirmation (doc 05 §1); RQ-14 |
| PiP action capacity; gesture-exclusion behaviour at screen edges | `PictureInPictureParams` surface; edge-drag test with and without exclusion rects | RQ-16 |

**Three rules:** (1) record **what the device reports**, never what its marketing page claims; (2) record **unavailability** too — a version-gated surface that returns nothing is logged "not available on this build", never left blank; (3) the probe output **is** the device's lab-matrix record (S8) — no device enters the lab matrix without one.

**Budget:** **no prices were researched for this document** `[U]` — cost is region- and timing-dependent, and inflating a number here would be exactly the kind of invented constant the standing rule forbids (17 §1.3). The cost *drivers* are: the D1 device is the cheapest and the most load-bearing; HDR/high-refresh and any foldable are the expensive outliers; SD-capable devices are cheap but must be verified, not assumed.

### 3.5 Fleet growth: the B0 subset → the doc 12 §6 matrix

**The mistake this section exists to prevent (Part 32, D-23):** procuring a small B0 lab and later discovering that release testing needs a different, larger set — and paying a second lead time at the worst possible moment.

| Stage | Coverage required | Indicative devices | When |
|---|---|---|---|
| **B0** | The 11 device-dependent spikes: one D1 (class-representative), one D2, one D3/HDR/high-refresh, one different-skin device **with SD** | 3–4 | Day one of B0 |
| **B0 top-up** | doc 12 §6's six OS bands not already covered; a second OEM slant; adoptable SD | +1–3 | **Order with the B0 set if the ceiling allows** — ordering later costs a second lead time |
| **B6 matrix** | Full doc 12 §6 coverage: bands, storage modes, display set incl. a DV-declaring device, three OEM slants, locale set | +2–4 | Before `TST-CMPT` runs |
| **Borrow or rent — never buy** | Foldable (unless RQ-15 needs one), DV-declaring device (unless RQ-04 needs it), cast/TV hardware (T4 pending), farm access for breadth | 0 | On demand |

**Selection rule:** when two candidates both satisfy a B0 requirement, prefer the one that also closes an uncovered item of doc 12 §6's matrix. It costs nothing at purchase time and saves a procurement round later. One fleet that grows — not two purchases.

---

## 4. Week-one sequence

Derived from the B0.0 finding (08A §2). The order matters: **device order and legal engagement have external lead times and should start before any engineering work on the corpus.**

| When | Action | Owner | Notes |
|---|---|---|---|
| **Day 1** | Answer **OD-4** (even roughly) | owner | Decides lab size and B0 concurrency (Q16-7) — the single highest-value answer; **doc 16 §2.1 pre-computes what each possible answer changes** |
| Day 1 | Answer **OD-2** and **OD-3** | owner | Both have safe defaults; answering removes them from the open list |
| Day 1 | **Order the device lab** (§3, worksheet §3.2.1) | owner | The device-dependent spikes — a derived 11 of 16 (08A §3) — are blocked without it; external lead time |
| Day 1 | **Open L1 and L2 with counsel** (briefs: `11A_LEGAL_BRIEFS.md`) | owner | L1 is the highest-priority legal item (doc 11 §5); both have safe fallbacks, so this is not on R1's critical path |
| Day 1–2 | Open Play Console (doc 15 S1), start developer verification (S2), open the **test track early** for RQ-08 | owner | Store elapsed time dominates RQ-08, not engineering |
| Day 2–3 | Begin **B0.0**: fixture corpus v1 + adversarial and scale corpora (doc 12 §5) | eng | Prerequisite of most spikes; do not wait for hardware to start this |
| Day 2–3 | Implement the **intake probe** and lab-matrix record (§3.4.1 — specified) | eng | Needed the day the first device arrives |
| Week 1, in parallel | **RQ-09** (iOS viability — no device needed) and **RQ-02** (toolchain — build machine only); paper half of **RQ-14** | eng | The three spikes that are not blocked by hardware or corpus |
| Week 1 | Specify the seek/first-frame and state-restoration harnesses (doc 12 §7) | eng | Second half of B0.0 |
| **Do not** | Start RQ-01 (or any group-C/D spike) | — | Running a spike without its pinned corpus converts evidence into anecdote (08A S1) |

---

## 5. Owner answer sheet — filled (Part 38, 2026-10-20; owner-delegated)

**These decisions were taken under the owner's instruction to proceed on the process's best judgment.** They are recorded in the ledger (doc 00 §5) and mirrored into doc 17's registers; each is replaceable through the change process (17 §7). **OD-4's scenario stands in for facts only the owner holds** (actual headcount, budget) — replace it wholesale when those arrive.

| OD | Question | Decision | Notes |
|---|---|---|---|
| OD-1 | Platform order | **Android-first** (DEC-002 confirmed) | iOS stays an evidence-gated R1.x decision (RQ-09) |
| OD-2 | R1 boundary: local-only vs network | **Local-only** (DEC-001 confirmed) | SMB/NAS/HTTP remain an R2 candidate |
| OD-3 | Monetization at launch | **Free / no ads / no account** (DEC-004 confirmed) | Monetization option B remains an R1.x candidate |
| OD-4 | Team, design, window, spend ceiling | **2 engineers full-time + part-time design; no hard external date (internal aim only); modest ceiling** | Lab: §3.1 minimum fleet grown by §3.5 (buy one → probe → extend); counsel: batch A. Calendar: doc 16 §2.1's 2-engineer row (≈5–9 months). Q16-7 = 2 engineers on B0; **the §3.3 "second unit of the busiest class" trigger is now met** — buy one duplicate when the probe confirms its class, budget allowing |
| OD-5 | First market set / launch regions | **Targeted globally from Day 1** (owner directive — Part 39) | Worldwide availability on Google Play; English baseline UI at launch; full multi-script subtitle support (Arabic RTL, CJK, Devanagari, Cyrillic) from Day 1; progressive UI translation additions. L5 statute scope covers global regimes (GDPR, CCPA, DPDP, LGPD, EAA) concurrently |
| OD-6 | Candidate names (2–3) | **Deferred by decision** — naming happens at launch-prep | The collision risk is the one error that forces a rebrand; nothing blocks on it except L6, which starts once a shortlist exists |

**Remaining physical acts (cannot be done from the spec desk):** order the lab (§3), **send batch A to counsel (cover note: `11A` §0.1)**, open the Play Console track (S1/S2). **If a hard date ever appears**, the de-scope ladder is now exercisable (doc 16 §5.1; rung definitions confirmed — Q16-8).

---

## 6. Boundaries

- **Nothing here is executed or ordered.** No device has been bought and no counsel engaged. The decisions taken so far are the **Part 38 delegated set** (doc 00 §5) — delegated, not owner-validated, and replaceable via 17 §7.
- **No claim that the B0 device subset satisfies the release matrix.** The B0 lab is a *subset* of doc 12 §6's coverage; §3.5 is the plan to close the gap, and until it is closed, `TST-CMPT` runs on the doc 12 §6 matrix only (doc 12 §6).
- **No prices, no models, no benchmarks.** §3.1 specifies *capabilities to verify*, not products; §3.4's farm policy exists so that a cheap fleet result can never be presented as a lab result.
- **No claim that answering the ODs unblocks launch.** They unblock **B0 scheduling**. The build/launch decision remains the owner's separate gate.
- **Legal briefs are inputs to counsel, not legal advice** — `11A_LEGAL_BRIEFS.md` states our reading so it can be corrected, which is the point of sending it.
