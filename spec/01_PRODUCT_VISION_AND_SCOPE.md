# 01 — Product Vision and Scope

**Version:** 0.1 (draft — pending Phase 1 research validation)
**Date:** 2026-10-18
**Status:** Draft. Sections marked ⏳ are explicitly pending research (W1–W4) or owner decisions (OD-*) and will be revised; nothing here is a measured fact.

---

## 1. Vision statement (draft)

> A mobile video player that plays your personal video files with boring reliability, excellent subtitle handling, fast library browsing even on large or slow-storage libraries, and zero exploitation of your attention or data — on the phone you already own, without an account.

The bar we hold ourselves to is not "more features than VLC/MX Player." It is: **if a file plays, it plays correctly; if it cannot play, we say so honestly and explain why; and the app never lies about your data.**

## 2. Product principles (adopted from master prompt §2, with two clarifications)

1. Playback quality and reliability before feature count.
2. Core player useful with no account, no network, no cloud.
3. Permissions only where there is a clear, explained benefit.
4. No intrusive ads, no surprise data collection, no manipulative upsells.
5. Honest capability communication — never promise universal format/device support.
6. Prefer native platform behavior where it wins; share code only where sensible.
7. Design recovery paths, not just happy paths.
8. Performance, battery, security, accessibility, maintainability are requirements from day one.
9. Differentiate with validated customer problems, not novelty.
10. Candor about costs, platform limits, licensing, trade-offs.

**Clarification A (scope discipline):** a feature being *requested* by some users does not make it in-scope; it enters the roadmap only with evidence and a costed feasibility note.
**Clarification B (AI):** no AI feature ships in R1; any later feature must be optional, on-device-first, explain its limits, and must never silently upload filenames, subtitles, frames, or media.

## 3. Problem space (provisional; validated in doc 02)

Provisional problem statements to be confirmed/refuted by Phase 1 research, each with a falsifier:

| # | Provisional problem | Falsifier (would refute) |
|---|---|---|
| P1 | Incumbent player apps are monetized in ways users actively dislike (ads, nagging upgrades, subscription creep), eroding trust | Research shows ad-supported players dominate without meaningful complaint volume |
| P2 | Playback "long tail" failures (odd MKV/HEVC 10-bit/VFR files, decoder fallback gaps, A/V sync on seeks) persist in incumbents | Structured evidence shows mainstream incumbents fail these rarely |
| P3 | Subtitle handling (ASS/embedded styling, encodings, sync, readability) is a frequent frustration and a top support driver | Review mining shows subtitles are a low-frequency complaint |
| P4 | Library UX at scale (10k+ files, slow SD cards, folders/sorting/search) is slow or cluttered in incumbents | Tester/stats show libraries are typically small and performance adequate |
| P5 | Low-end devices get worse experiences (thermal, battery, memory pressure) than they should | Evidence shows e.g. Media3-based players run fine on low-end devices with no mitigation needed |
| P6 | Privacy-first positioning (no trackers, no ads, local-only by default) is a real, addressable segment, not a niche | Market data shows privacy plays no role in player choice |

### 3.1 Evidence update — W1 competitor research (accessed 2026-10-18; synthesized in doc 02 v0.1)

- **P1 (monetization distrust): SUPPORTED, concentrated in the MX segment** — recent Play-review reports of ad density plus MX's plan discontinuations/prorated refunds [R/V1, doc 02 §2.4]; the ad-free cluster (VLC/Just/Nova/mpv-android) keeps the contrast real.
- **P2 (long-tail playback failures): PARTIALLY SUPPORTED** — specific documented pockets: TS/MPG regression vs older version [R], crash-at-same-point reports [R], Nova's continuous A/V-sync/seek/SMB fix tail [V1 changelogs]. Field-wide frequency remains unmeasured [U].
- **P3 (subtitle friction): SUPPORTED** — zoom-hides-subtitles bug [R], no styling/sync UI in Media3-based players [R], mpv lacking font/shadow prefs [R]; MX's vendor-documented format breadth sets the bar [V1].
- **P4 (library at scale): SUPPORTED (strong)** — VLC scan incompleteness + open tracker issues [R/V1], KMPlayer huge-tree discovery crashes/restarts [R], Nova scan regressions [V1/R].
- **P5 (low-end device experience): NOT YET SUPPORTED** — no admissible battery/thermal/crash-rate evidence found; retain as internal quality discipline, not a market claim.
- **P6 (privacy segment): PARTIALLY SUPPORTED** — sharp contrast evidence (MX: installed-app collection in policy, CVE-2020-5764, Common Sense "Warning") [V1/V2]; commercial segment size still unquantified (W4 + validation plan in doc 03 §5).
- **NEW P7 (HDR/Dolby Vision correctness): SUPPORTED as enthusiast-visible pain** — mpv maintainer-stated HDR breakage under hwdec and no DV; Kodi v22 DV+PGS fix; Just Player's DV P7→HDR fallback [V1]. In-scope response is honesty + best-effort correctness (DH-8), never blanket claims.
- **Boundary note:** the strongest *specific* evidenced gap — SMB playback + sidecar subtitles done right — lies **outside R1 scope** (DEC-001). Recorded as R2 gate question Q1 (doc 02 §9); revisit only via owner decision.

## 4. Product-boundary analysis (master prompt §4 types A–E)

| Type | Description | Addressable pain | Cost & risk | Verdict |
|---|---|---|---|---|
| A | Primarily local-file player (+ library management) | High: files arrive from downloads, camera, chat apps, SD cards | Lowest: no servers, minimal legal surface | **R1 core** |
| B | A + network shares/direct streams (SMB/NAS, HTTP(S)) | Real but more niche; strong in prosumer/NAS households | Moderate: protocol security, credential storage, buffering UX | **R2 candidate** (bounded: SMB2/3 + direct URLs only) |
| C | A + broad streaming-protocol client (HLS/DASH/RTSP/live) | Served by VLC; less differentiated; ongoing protocol maintenance | High: adaptive streaming, DRM encourters, endless edge cases | **Out of scope** (revisit only with evidence) |
| D | Full media-library/server ecosystem (Plex/Jellyfin-class) | Different product with backend+apps; large ongoing ops | Very high: servers, accounts, sync, privacy obligations | **Out of scope** |
| E | Content service/streaming platform | Not a player problem; licensing, content, CDN, moderation costs | Extreme | **Out of scope** (explicitly per master prompt) |

**Recommendation (DEC-001):** Ship **A** as R1; keep **B** as a *bounded* R2 candidate behind explicit gates. Do not blend B/C/D/E into the MVP. (Owner approval requested — OD-2.)

## 5. Recommended initial scope (R1) — draft pending validation

### 5.1 In scope (R1)

- **Library & file access:** scan user-selected folders/videos via Android scoped storage (MediaStore + SAF as appropriate, exact mechanism pending W2); incremental rescan; thumbnails with sensible caching; sorting/filtering/search; recently played; resume positions; favorites; basic playlists; hidden-folder/exclusion config; handling of moved/renamed/deleted files.
- **Playback:** Media3/ExoPlayer-based engine (DEC-003) with hardware-first decode and an LGPL-clean FFmpeg software-decode extension for the supported corpus; common containers (MP4/M4V, MKV, WebM at minimum — final list in doc 07); H.264, HEVC, MPEG-4 Part 2 (best-effort), VP9, AV1 (subject to device); 8/10-bit where platform permits; VFR tolerance; rotation metadata; aspect/zoom/crop; seeking with fast recovery; A/V sync maintenance and manual audio offset.
- **Controls:** play/pause/seek/skip (configurable steps), speed presets (0.25×–3×, pitch-corrected where available), gesture set (single/double tap, vertical brightness/volume, horizontal seek, pinch zoom, lock mode), screen lock, orientation modes, sleep timer, A-B repeat (MAY), chapter list when present; reachable one-handed layout.
- **Subtitles:** embedded + external files; SRT (primary), ASS/SSA (best-effort styling fidelity — spike RQ-06), WebVTT; encoding detection with manual override; styling controls (size, color, outline, background, position, margins); timing offset; per-video preference memory.
- **Audio:** track selection; audio delay adjustment; audio focus/session handling; route change handling; passthrough only where platform supports; no equalizer in R1 (revisit later).
- **Integration:** "open with" / share-sheet entry points; PiP where platform supports; no casting in R1.
- **Settings/help/diagnostics:** settings IA (doc 06), local diagnostics screen, privacy controls, permission-recovery flows, honest error surfaces with recovery actions.
- **Accessibility:** screen-reader-complete player and library (doc 06), dynamic type, contrast, reduced motion, 48dp targets, focus order.
- **Performance/reliability:** measurable NFRs (doc 05) including cold start, time-to-first-frame, scan throughput, battery budget, crash/ANR targets, state restoration.

### 5.2 Out of scope (R1) — explicit exclusions

- iOS client (decision gate R1.x per DEC-002).
- Android TV, car, wearables, desktop, web.
- Network playback of any kind: SMB/NAS/NFS, HTTP(S) direct, HLS/DASH/RTSP/live, casting/AirPlay/Chromecast.
- DRM/protected content playback (DEC-006).
- Accounts, cloud sync, backup services, social features.
- AI features of any kind (DEC-007).
- Online subtitle download/translation.
- Equalizer/audio effects; video editing/transcoding; torrents; download managers.
- Ads and any monetization mechanics at launch (DEC-004).
- F-Droid/GPL distribution channel (DEC-009).

### 5.3 Future scope (candidates, evidence-gated)

| Candidate | Train | Gate |
|---|---|---|
| SMB2/3 + direct HTTP(S) file playback | R2 | Security design accepted (doc 10); spike on buffering UX; user evidence |
| iOS client | R1.x gate decision | RQ-09 answers: achievable experience %, licensing, cost |
| Android TV | R2 | Input-model UX design; device market evidence |
| Casting (Google Cast) | R2 | Platform SDK review + privacy review |
| Subtitle download (lawful sources only) | R2/R3 | Provider terms review; no default third-party dependency |
| On-device subtitle generation/translation | R3 | Feasibility + privacy + model licensing + battery budget |
| Equalizer/effects | R3 | Platform feasibility; no battery regressions |
| Settings import/export, device-to-device migration | R2 | Data model stability (doc 09) |

## 6. Users (preview — full personas in doc 03)

Primary provisional groups: (1) **Everyday local-library users** on mid/low-end Android phones, often in dense media markets; (2) **Power/cinephile users** with large libraries, HEVC 10-bit and subtitle-heavy content; (3) **Privacy-conscious users** seeking no-tracker, no-ads tools; (4) **Casual viewers** opening files from chat/downloads/SD cards via "open with". Full JTBD and journeys in doc 03 (pending research).

## 7. Platform and device targets (draft pending W2)

| Item | Draft recommendation | Basis / to confirm |
|---|---|---|
| First platform | Android | **W2 evidence strengthens DEC-002:** iOS `AVFileType` lacks MKV/AVI/WebM/FLAC; native subtitle support is WebVTT/IMSC1/iTT/CEA only (no SRT/ASS); Dolby Vision in HLS restricted to Profile 5; PiP must be user-initiated or review rejects the app; unlocks must use IAP [V1 Apple docs → platform.md §7–9]. W4 to size the market. |
| Minimum OS (Android) | **Android 8.0 (API 26)** draft: API 28+ ≈94%, 29+ ≈91%, 33+ ≈69% of active devices [V2 apilevels.com, accessed 2026-10-18; recheck before locking]; API 26 = PiP floor + broad coverage; final decision in doc 08 with QA-cost review | W2 §6 |
| File access model | MediaStore (primary) + Android Photo Picker + SAF; **must not depend on `MANAGE_EXTERNAL_STORAGE`** (Play lists media-file access as invalid use for it); `READ_MEDIA_VIDEO` requires a Play Console declaration → a degraded library mode is mandatory if denied/refused (FR-PLAT-006) | W2 §3 [V1 Play policies A18/A19] |
| Form factors R1 | Phones + tablets (portrait & landscape); foldables must not break (no specific foldable UX commitment) | W2 ergonomics + market |
| Chipsets/ABIs | arm64-v8a primary; armeabi-v7a status decided by size/perf trade-off in doc 08 | W2 + build analysis |
| Decode strategy | Hardware-first; fallback = **platform** software decoders (video) and a self-built **LGPL-2.1 FFmpeg audio** extension where doc 11 clears it; **no bundled software video / AAC / HEVC decode in R1** (W3: Media3's FFmpeg extension is audio-only; bundled video-decode royalty exposure unresolved ⚖️); per-device capability detection + honest failure UX | W3 + DEC-003; doc 07 tiers |
| Network requirements | None for core function (offline-first) | Principle 2 |

## 8. Distribution, business and operational constraints (assumptions)

- Distribution: Google Play primary (AAB); regions per OD-5/ASM-009.
- Business model: free, no ads at launch; later hypotheses only (doc 15). Costs are dominated by developer time; no backend planned (doc 09 documents the no-backend decision).
- Operations: staged rollout, crash/ANR vitals monitoring (privacy-respecting), quarterly dependency review (docs 14/15).
- Launch-ops constraints (W4): new personal Play developer accounts require ≥12 testers × 14 days of closed testing before production access; Android developer verification rollout affects non-Play distribution (BR/ID/SG/TH from 2026-09-30, global 2027); Play Billing v8+ is required if any monetization ships (deadline 2026-08-31, extension 2026-11-01) [V1 → W4 notes].
- Team assumption ASM-005 (small senior team) drives scope realism; doc 16 sizes phases in ranges, not dates.

## 9. Success definition (preview; KPIs in doc 03 §KPI)

Provisional headline outcomes (targets set only where justified; else "measure-first"):
1. **Reliability:** ≥99.5% of initiated playback sessions reach first frame and complete without error `[E, to validate]` — definition and measurement in docs 05/13/14.
2. **Trust:** zero trackers in the shipping build; zero network by default beyond store-required services; verifiable via static analysis + runtime capture (doc 10/14).
3. **Quality:** subtitle-related support contacts and "won't play" reports materially below incumbent complaint baselines `[methodology in 02/14]`.

## 10. Open questions carried forward

| # | Question | Resolved by |
|---|---|---|
| Q1 | Are P1–P6 supported by evidence? | Phase 1 (doc 02) |
| Q2 | Minimum Android version and low-end device baseline | W2 + doc 07 |
| Q3 | Subtitle fidelity (ASS) feasibility inside Media3 | RQ-06 spike (later phase) |
| Q4 | One-time-unlock monetization viability without backend | W4 + doc 15 |
| Q5 | iOS gate criteria thresholds | RQ-09 + doc 16 |
| Q6 | OD-1…OD-6 owner decisions | Owner |

## 11. Sources

To be populated from `research/` notes in Part 2 (docs 02/07/08/11/15). No external claims are asserted in this draft beyond the master prompt itself; draft statements are assumptions `[E]/[I]` until sourced.
