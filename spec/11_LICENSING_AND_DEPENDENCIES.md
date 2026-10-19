# 11 — Licensing, Patents, and Dependency Register

**Version:** 0.2 (draft; v0.2 adds the sendable counsel briefs pointer in §5 — see `11A_LEGAL_BRIEFS.md`)
**Date:** 2026-10-19
**Status:** Draft. Built on `research/licensing.md` (W3, accessed 2026-10-18). **Nothing in this register has received legal sign-off.** Items marked ⚖️ require qualified legal review before any shipping decision; rate/date-sensitive facts are marked "recheck". This register is the **only** authority for "may we ship this component/codec" questions — product copy defers to doc 07 §13.
**Consumes:** 07 (compat tiers), 08 (ADRs), 09 (architecture), W3 research.
**Feeds:** 10 (§6.3 legal list), 15 (release gates), 18 (readiness checklist), build phase (CI license gates).

---

## 0. How to read this register

- **Verdict vocabulary:** **Allowed** (ship; obligations tracked) · **Conditional ⚖️** (ship only after legal sign-off or condition met) · **Forbidden** (never in the store build) · **Fallback-only** (only if ADR-001 revisit triggers, still gated) · **Test-only** (not distributed).
- **Linkage vocabulary:** *bundled* (in AAB) · *linked* (same, runtime) · *platform* (OS-provided) · *build-only*.
- Every row names the obligation artifact (license text, source offer, attribution) and its owner process (CI, release checklist).

---

## 1. Component register

### 1.1 Application & platform stack (expected Allowed)

| Component | License | Linkage | Use in R1 | Obligations | Verdict |
|---|---|---|---|---|---|
| Kotlin stdlib, coroutines | Apache-2.0 | bundled | Language/runtime | Notices | **Allowed** |
| AndroidX: Media3, Compose, Room, DataStore, WorkManager, Lifecycle | Apache-2.0 | bundled | Core app + engine | Notices | **Allowed** |
| Hilt / Dagger | Apache-2.0 | bundled (KSP/kapt) | DI | Notices | **Allowed** |
| Coil | Apache-2.0 | bundled | Images | Notices | **Allowed** |
| SQLite (via Room) | Public domain | platform/bundled | DB engine | None | **Allowed** |
| Platform codecs (MediaCodec/C2) | system | platform | Primary decode | None for app (OEM/patent responsibility with device) | **Allowed** |
| Material icons (Apache-2.0 set only) | Apache-2.0 | bundled | UI | Notices | **Allowed** |
| OFL fonts (Noto family subset per doc 06) | OFL-1.1 | bundled | Subtitle/UI fonts | License text + reserved-name respect; no re-licensing | **Allowed** |
| JUnit, Robolectric, Macrobenchmark (test) | Apache-2.0 / EPL | test-only | Tests | None in app artifact | **Test-only** |

### 1.2 Media libraries (the consequential rows)

| Component | License | Linkage | Use in R1 | Key obligations / risks | Verdict |
|---|---|---|---|---|---|
| **Self-built FFmpeg (LGPL-2.1-or-later, configured without `--enable-gpl` / `--enable-nonfree`)** | LGPL-2.1 | bundled `.so` (**audio decode subset only**) | Audio fallback decode (license-cleared codec list) | ⚖️ **LGPL §6 relinking mechanics on Play-distributed APK/AAB** — mitigation set: full corresponding source offer + `changes.diff` + relinkable objects; license text in notices; no static-linking trap | **Conditional ⚖️** |
| **FFmpegKit (any mobile FFmpeg packaging)** | LGPL/GPL mix (project) | — | — | Retired Jan 2025; repo archived Jul 2026 `[V1]` | **Forbidden** |
| **Media3 FFmpeg extension (upstream)** | Apache-2.0 (extension code) + FFmpeg inside (LGPL/GPL) | — | — | **Audio-only** upstream; not on Google Maven; we self-build instead | **Not used** (we build our own, row above) |
| **libass** | ISC | bundled (JNI) | Only if RQ-06 elects libass for ASS rendering (ADR-003) | Attribution; font handling stays OFL-only | **Conditional** (spike-gated) |
| **libVLC / VLC core** | LGPL-2.1 core (VLC *app* is GPL) | bundled (JNI) | **Fallback engine only** if ADR-001 revisit triggers | App-side integration obligations; heavy; keep out unless needed | **Fallback-only ⚖️** |
| **mpv** | GPLv2+ (with LGPL-core nuance depending on build) | — | — | GPL contamination for store build (DEC-009) | **Forbidden (store build)** |
| **ffmpeg `nonfree` builds, ProRes on Android** | — | — | — | W3 verdict: never bundle ProRes on Android | **Forbidden** |
| **Runtime-downloaded codec/artifacts** | — | — | — | Security + Play policy; ADR-011 | **Forbidden** |

**Own-app licensing:** the application's own source license/distribution model is **not yet decided** (proprietary vs open; F-Droid-variant implications per DEC-009). Recorded as open question OQ-1; does not block the spec.

---

## 2. Patent & codec-exposure register

**Core strategic fact:** with platform decoders, patent licensing sits with device makers (CDD-mandated codecs are their compliance problem). **Bundling our own decode creates app-level exposure.** Hence: no bundled video decode anywhere in R1; bundled audio decode only for the rows marked *cleared*.

| Technology | Pool/licensor posture (as researched) | App-impact if platform-decoded | App-impact if *we* bundle decode | Verdict for us |
|---|---|---|---|---|
| H.264/AVC | Via LA/MPEG LA-type pools; OEMs license | None | Unneeded (CDD floor) | Platform only |
| HEVC/H.265 | Pools active: VCL Advance rates cited **$0.30 R1 / $0.20 R2 per unit after first 100k** `[V1, recheck]`; **HEVC Advance "HEVC Software" category ambiguous** (rate tables image-only `[U]`) | None | Ambiguous + material cost risk | **No bundled HEVC decode** (FR-PLAY-003; doc 07 X-tier) |
| AAC | Via LA licenses "end-user decoder products"; **Google does not cover third-party app bundling**; app-side license question ⚖️ | None | License obligation likely for the *app* | Prefer platform; **no bundled AAC** in R1 (ADR-010) |
| AC-3 | Patents expired 2017 `[V1]` | Device-dependent | Low risk | **Candidate bundled add-back ⚖️** (FR-AUD-007) |
| E-AC-3 | Last US patent reported expired 2026-01-30 `[R, recheck]` | Device-dependent | Low risk if expiry confirmed | **Candidate bundled add-back ⚖️** |
| DTS / DTS-HD | No app-side license path found in research `[U]` | Passthrough only | Exposure unclear | **Passthrough only; bundled decode Forbidden** |
| TrueHD / Dolby-branded audio, Dolby Vision, Atmos | Dolby licensing required for decode/branding | Passthrough/declared devices | Requires Dolby license | **Passthrough only**; no brand claims in UI beyond factual device paths (doc 07 §13) |
| AV1 | "Royalty-free" contested: Sisvel pool cited (€0.32 / €0.11 per device, components excluded) `[V1]`; Dolby v. Snap (Mar 2026, first platform-facing suit) `[R]` | None (CDC mandate; OEM side) | Unneeded + contested ground | **Platform only; no bundled AV1 decode commitment** |
| VP9 | Royalty-free posture | None | Unneeded | Platform only |
| Opus/Vorbis/FLAC | Royalty-free | None | Unneeded (platform) | Platform (+ license-clear ext if ever needed) |

**Recheck cadence:** patent rows re-verified at every release gate (doc 15 checklist) and before enabling any *Conditional* decode row.

---

## 3. LGPL compliance process (for the FFmpeg audio extension)

The build phase must maintain, per release:

1. **Source offer**: exact FFmpeg source + build scripts + `changes.diff` published alongside releases (hosting TBD — OQ-3), referenced in-app (FR-SET-008).
2. **License text**: LGPL-2.1 + notices bundled in the About screen.
3. **Relinkability package** ⚖️: satisfy §6's "work that uses the library" mechanics in the Play-distributed artifact (dynamic linking + relinkable objects or written offer per counsel). **This is the single highest-priority legal item** (doc 10 §6.3).
4. **Configuration lock**: build fails CI if `--enable-gpl`, `--enable-nonfree`, or non-cleared decoders appear (license CI gate).
5. **No-audit shortcut**: if counsel rejects the relinking mechanics, fallback = ship **without** bundled audio decode (device codecs only) and degrade FR-AUD-007 accordingly — product survives; differentiator unaffected (audio was a compatibility add-on, not a headline claim).

---

## 4. SBOM and attribution plan

| Item | Plan |
|---|---|
| SBOM format | CycloneDX (preferred) or SPDX; CISA 2026 v2.1 minimum fields (17) `[V1/W3]` |
| Generation | CI artifact per release build; stored with release records (no network in product required) |
| Notices | In-app "About → Open source" from a generated registry (license text per component; FR-SET-008) |
| License policy checks | CI deny-list: GPL/GPL-type, `nonfree`, unknown-license dependencies; new dependency review template (purpose, license, maintenance, size) |
| Quarterly review | Dependency + CVE + license drift review (NFR-SEC-004); patent rows recheck (doc 15 gates) |
| F-Droid / GPL channel | Separate build decision deferred (DEC-009); would not alter the store build's license-clean posture |

---

## 5. Legal-review backlog (⚖️ — consolidated; mirrored in doc 10 §6.3)

| # | Item | Priority | Blocks |
|---|---|---|---|
| L1 | LGPL §6 relinking mechanics on Play (source offer + relinkable objects) | **Highest** | Bundled FFmpeg audio extension (ADR-010) |
| L2 | AC-3/E-AC-3 bundled decode (patent expiry confirmations; pool silence) | High | FR-AUD-007 add-back |
| L3 | DTS/TrueHD passthrough wording + bundled-decode prohibition rationale in copy | Medium | doc 07 §7/§13 copy |
| L4 | Dolby brand usage rules in UI text ("Dolby", "Dolby Vision", "Atmos" references) | Medium | doc 06 error/copy strings |
| L5 | Statute applicability notes (GDPR/DPDP/LGPD/EAA/COPPA per doc 10 §6.2) | Medium | Privacy page, store forms |
| L6 | Trademark/name clearance + copycat response (OD-6) | Medium | Store listing, doc 15 |
| L7 | Own-app license/distribution model incl. F-Droid implications | Low (post-spec) | OQ-1 |
| L8 | Store-copy wording audit vs doc 07 §13 (claims discipline) | Gate-each-release | doc 15 checklist |

**Briefs ready to send:** `11A_LEGAL_BRIEFS.md` contains **L1 and L2 in sendable form** — the facts as they will ship, our readings stated explicitly so counsel can correct them, numbered questions each with the decision its answer changes, the compliance package we propose to implement, the assets we can hand over, and the designed fallbacks. L3–L8 are not yet briefed (they are copy, statute and naming matters and are handled with those documents).

## 6. Open questions

| # | Question | Owner |
|---|---|---|
| OQ-1 | Own-app license/distribution model (proprietary vs FOSS; F-Droid variant) | Owner/15 |
| OQ-2 | SBOM tooling choice + storage location (CI-native vs external) | 15 |
| OQ-3 | Hosting for LGPL source offers and relinkables (repo vs release assets vs site) | 15/⚖️ |
| OQ-4 | Whether any **device bucket** of D1 ships usable DTS/TrueHD passthrough worth documenting to users | 12/13 |
| OQ-5 | Font subsetting rules vs OFL reserved names on feature expansion | 06/11 |
