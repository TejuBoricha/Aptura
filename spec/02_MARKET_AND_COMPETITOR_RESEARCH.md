# 02 — Market and Competitor Research

**Version:** 0.2 (W1 competitor research + W4 market/monetization/compliance integrated; W2/W3 research feeds docs 07/11 and is referenced, not duplicated, here)
**Date:** 2026-10-19 (research accessed 2026-10-18)
**Status:** Draft. All findings are from public sources; **nothing was hands-on tested**. No benchmark, battery, or crash-rate claim is made or inferred from reviews. Raw notes: `research/competitors.md` (244 lines, fully sourced).
**Consumes:** master prompt §5; `research/competitors.md`.
**Feeds:** 03 (personas/DH evidence), 06 (UX gaps), 07 (compat), 15 (monetization), 16 (prioritization), 17 (registers).

> **Companion:** `02A_COMPETITOR_CASE_STUDIES.md` (v0.1) is the depth layer for this document — per-app feature and settings catalogues, player UI anatomy, gesture maps, decoder/subtitle internals, and the adopt/reject design implications. This document stays the synthesis and scoring layer; 02A holds the anatomy. Read 02 for "what it means", 02A for "how it works".

---

## 1. Methodology and source strategy

**Question set answered here:** competitor positioning, audiences, scale, update health; capability claims (containers/codecs/subtitles/network/HDR); monetization and monetization sentiment; privacy posture and public incidents; recurring user complaints; unoccupied opportunities.

**Method:** desk research via web search + page fetches on 2026-10-18; store pages (Google Play, F-Droid), project repos/releases/issues (GitHub/GitLab), vendor docs/support pages, established press, privacy evaluations, and user threads (Reddit/XDA) as pain-point signals.

**Evidence labels:** `[V1]` primary verified (store/repo/docs/license), `[V2]` reliable secondary, `[R]` reported user experience (signal only), `[I]` inference, `[E]` estimate, `[U]` unknown/unverified.

**Limits and honesty statement:** store "installs" are coarse buckets; ratings vary slightly across trackers (noted where conflicting). Review quotes are individual reports, not defect proof. Documented format support ≠ validated per-device playback. We did not test any competitor app. Frequencies in §6 are qualitative counts of sampled 2025–26 sources `[E]`.

---

## 2. Competitor set and matrix

Included: VLC, MX Player, Just Player, mpv-android, mpvKt (historical), Nova, KMPlayer. Adjacent reference: Kodi (media center, different product type — per §4 boundary analysis). Radar only: Next Player (FOSS, Media3, 100K+), Vimu (TV, paid), Fermata (niche). "PlayerX" excluded (no real user base). iOS set: summarized in §2.4 (W4), decision-input only.

### 2.1 Status and positioning

| Product | Engine / license | Scale (Play, 2026-10-18) | Update cadence | Monetization | Privacy posture |
|---|---|---|---|---|---|
| **VLC for Android** | libVLC (LGPL core / GPL app) [V1] | 500M+ installs; 4.0★, 2.01M reviews | ≈annual majors (3.7.0 Feb 2026; 3.7.1 May 2026; 3.7.2 beta) | Free; donations; no ads/IAP; store page claims "no spying" | Play data-safety: no data collected/shared (declared) [V1]; F-Droid build exists |
| **MX Player** | In-house + platform (HW+ decoder) [V1] | 1B+ installs; ~4.2★, 14.3M reviews; "Contains ads", IAP | Rapid 3.x train (v3.3.1, 2026-10-12; Play update 2026-10-15) | Ads + IAP; MX Gold/Silver discontinued 2026-04-30 (prorated refunds); free content stays ad-supported | Documented collection incl. device IDs + installed-app collection; Common Sense "Warning"; CVE-2020-5764 (patched 1.24.5) [V1/V2] |
| **Just Player** | **AndroidX Media3 + FFmpeg ext.**; Unlicense | ~1M+ installs; 4.0★/~3.4K vs 3.88/2.6K (source conflict — cite as range) | ≈monthly; tracks Media3 (v0.217 Sep 2026; 0.218 on Play) | None (donations only) | No ads/trackers; Unlicense; F-Droid available |
| **mpv-android** | libmpv (FFmpeg+libass+libplacebo); LGPL-core | 500K+ installs; 4.42K reviews | ≈1–2 months (latest 2026-09-17) | None | F-Droid + Play (scoped-storage build; `allstorage` build explicitly **not** shipped to Play) |
| **mpvKt** (historical) | libmpv; Apache-2.0 | (small) | **Archived 2026-03-30**; last release v0.1.6 | None | FOSS; frozen — forks may continue |
| **Nova** | FFmpeg + MediaCodec (avos core); Apache-2.0 | 1M+ installs; 4.1★/13.5K | Very high (v6.5.5 2026-10-09; Play 6.4.72 Sep 2026) | Donations only | FOSS; no ads |
| **KMPlayer** | In-house multi-engine | 10M+ installs; 385K reviews | Active (v36.10.x Oct 2026) | IAP (VIP); data-safety: none collected (declared) | Declared minimal; vendor replies on reviews are canned `[I]` |
| *Kodi (adjacent)* | Own (FFmpeg-based); GPLv2+ | ~73M cumulative (AppBrain) | v21.3 stable; **v22 "Piers" RC1 2026-10-10** | Free | FOSS |

### 2.2 Capability highlights (differentiating cells bolded)

| Product | Formats (notable) | Subtitles | Network / streaming | TV / casting | Library / UX notes |
|---|---|---|---|---|---|
| VLC | MKV/MP4/AVI/TS/M2TS/…; libVLC README: SMB/FTP/SFTP/NFS, UPnP, HDR+tonemap, DD+/TrueHD/DTS passthrough, "up to 8K", Chromecast | Embedded+external incl. **ASS**, DVD, Teletext, CC; font customization; delays; **zoom hides subs; limited customization `[R]`** | SMB/SFTP/UPnP/RTSP/HLS…; **SMB failures + SMB sidecar-subtitle blind spot well-evidenced `[R]`**; HLS external-VTT gaps `[R]` | Android TV yes; Chromecast (beta heritage; recent reliability unverified `[U]`) | Library exists; **scan incompleteness/slowness well-evidenced `[R]`** |
| MX Player | Broad; subtitle format breadth **(SRT/SSA-ASS full styling/SMI/SUB/IDX-VobSub/MPL2/TMPlayer/Teletext/PJS/VTT/DVD-DVB)**; HW+ decoder (vendor claims unverified) | **Best-documented breadth**; subtitle gestures | SMBv2 local network; **OTT streaming integrated (Amazon/Prime)** | TV supported; floating window + PiP custom controls | Kids lock, privacy folder, equalizer, video cutter; heavy UI |
| Just Player | Media3+FFmpeg: MP4/MOV/WebM/MKV/Ogg/TS/PS/FLV (+AVI 🚧); H.263/H.264/HEVC/MPEG-4/VP8/VP9/AV1; **AC3/EAC3/DTS/DTS-HD/TrueHD via FFmpeg ext.**; HDR10+/DV on supported hw with **DV P7→HDR fallback** | SRT/SSA/ASS (limited styling)/TTML/VTT/DVB — **no styling/sync UI `[R]`** | DASH/HLS/RTSP/SS (streaming focus) | TV: tunneled playback; TV episode-flow complaints `[R]` | **No media library by design**; single-screen simplicity loved `[R]` |
| mpv-android | Full mpv breadth; libass rendered styling "fidelity leader" **`[V1]`-supported** | **libass = reference quality**; secondary subs; no font/shadow prefs `[R]` | Open URL; scripting | TV via F-Droid/manual only; PiP, background | File-browser UX thin; no resume in some flows `[R]`; **hwdec HDR broken (maintainer), no DV support** |
| Nova | FFmpeg/MediaCodec; broad; AC3/DTS passthrough | Multi-audio/subs; subtitle download | SMB/UPnP/FTPS/SFTP/WebDAV; Trakt; TMDb scraping; NFO; network resume | TV leanback UI | **Changelog = long A/V-sync/seek/SMB fix tail; post-update regressions `[R]`** |
| KMPlayer | Broad + cloud + torrent-VIP | Styling; section repeat | FTP/UPnP/SMB/WebDAV/Dropbox/OneDrive; Chromecast | Casting | **Huge-folder discovery failures/crashes `[R]`; slow scans** |

### 2.3 Why this matters to our scope (boundary implications)

1. **The single strongest evidenced specific gap is SMB playback + SMB sidecar subtitles (VLC, repeatedly).** Our R1 boundary (DEC-001) defers network playback. Consequence: our R1 differentiators must not depend on it; R2 gate should treat "SMB done right" as a **high-priority candidate** (owner-informed; see Q1).
2. **Subtitle-system quality is the highest in-scope evidence-backed lever:** mpv proves fidelity demand but lacks UX; Media3 players lack styling/sync UI; MX has breadth but heavy UI. A "fidelity + usability" subtitle system is unoccupied in-scope.
3. **Library scanning robustness/perf on scoped storage is an evidenced weak spot across VLC/KMPlayer/Nova** — in scope for R1 and aligned with DH-4.
4. **Privacy/trust has a concrete incumbent contrast (MX)** — in scope (DH-3), with W4 to size the segment.
5. **HDR/DV correctness is evidenced but platform-constrained** (mpv hwdec HDR broken, no DV; Just does DV P7→HDR fallback; Kodi v22 just fixed DV+PGS). In-scope treatment = honesty + best-effort correctness, never a blanket claim (new DH-8).
6. **"Nice UI over mpv" is vacant** (mpvKt archived 2026-03-30) — but adopting libmpv-class rendering carries licensing/product cost (W3) and integration risk (RQ-06); keep as fallback candidate only (DEC-003).

### 2.4 iOS competitor snapshot (W4; decision input only — iOS remains post-R1 per DEC-002)

| Product | Model/price `[V1, prices recheck]` | Rating signal | Notes |
|---|---|---|---|
| Infuse | Free + subscription ($1.99/mo · $16.99/yr · $99.99 lifetime) | 4.7★ (~28K ratings) | The paid-tier bar-setter; format/protocol coverage specifics are `[U]` this pass → re-verify at the iOS gate (RQ-09) |
| nPlayer | $4.99 one-time unlock | — | One-time-unlock pricing precedent (outside Play billing) |
| VLC iOS | Free | 3.7★ | Same trust brand as Android but visibly lower satisfaction than paid players |
| Outplayer | Free + one-time unlock | — | Store privacy label: "Data Not Collected" — shows a privacy-forward niche exists on iOS |

**Implications:** (1) paying iOS users expect Infuse-class format/protocol breadth — expensive for us given AVFileType gaps (no MKV/AVI/WebM; native subtitles VTT/IMSC1/iTT/CEA only — W2); (2) our iOS angle, if pursued, would be privacy + simplicity + honest handling, addressing the VLC-iOS satisfaction gap rather than Infuse's feature set; (3) reinforces Android-first (DEC-002). iOS format-complaint themes are `[U]` this pass (RQ-09 recheck).

---

## 3. Short profiles

**VLC (VideoLAN).** The scale+trust incumbent: 500M+ installs, free/no-ads/no-IAP, declared zero data collection, broadest self-described format/network coverage. Weakness concentrates in *specific reliability pockets*: SMB playback and SMB sidecar subtitles, HLS external subtitles, library scan incompleteness (multiple 2025–26 reports + open issues), occasional zoom/subtitle UI gaps, and a dated-feeling UX. Engine is libVLC with annual major cadence; no public evidence of an imminent rewrite `[U]`. [V1/R: §1 of raw notes]

**MX Player (Amazon).** Mass-market giant (1B+ installs) now fused with Amazon's India strategy; global app still ships rapidly (3.3.x, Oct 2026) while iOS/TV/web were discontinued/redirected to Prime Video (2026-05+). Local player remains; monetization history now includes discontinued Gold/Silver tiers and ad-density complaints; privacy history (installed-app collection, CVE-2020-5764, Common Sense "Warning") gives a durable trust contrast. Subtitle format breadth is class-leading (vendor-documented). [V1/V2/R]

**Just Player.** The "clean Media3 reference": Unlicense, no monetization, monthly Media3-tracking releases, FFmpeg-ext audio codecs, DV P7→HDR fallback. Explicitly **no media library** (author: "never truly compete with feature rich VLC"). Loved for simplicity and codec reach; criticized for no library view and minimal subtitle customization. Proves a Media3+FFmpeg-ext stack can satisfy codec-hungry users — a direct feasibility signal for DEC-003. [V1]

**mpv-android.** Subtitle-fidelity leader via libass; advanced render options; roughly bimonthly builds; scoped-storage vs `allstorage` build split (former on Play). Maintainer-confirmed limits: HDR broken with hwdec, no Dolby Vision; thin file-picking/resume UX. Proves demand for rendering quality and exposes how hard HDR/DV is on Android even for a beloved engine. [V1/R]

**Nova.** FOSS library-centric player with very active maintenance; broad network sources, scrapers, TV UI; changelog shows constant A/V-sync/seek/SMB bug-tail — evidence that *these exact quality areas* are the grind, not one-off defects. Donation-funded, Play + F-Droid. [V1/R]

**KMPlayer.** Mass-market (10M+) multi-feature player with cloud/torrent extras and VIP IAP; recurring reports of slow/crashing discovery on huge folder trees — reinforces the large-library pain and the "feature-bloat tax" narrative. [V1/R]

**Kodi (adjacent, not a target).** Media-center scope; v22 RC fixes DV+PGS playback on Android — corroborates HDR/DV+subtitle-layer interactions as a real platform pain class. [V1]

---

## 4. Strategic SWOT (for our product at R1)

| | Helpful | Harmful |
|---|---|---|
| **Internal** | **S:** quality-first positioning is unclaimed; measured NFRs + privacy posture + subtitle focus + scan/perf discipline; small scope = fast iteration; Media3 foundation proven by Just Player; **W:** late entrant; no network/TV/casting in R1 while incumbents have them; Media3 gaps (AVI/WMV/Theora/10-bit AVC variance); no iOS; small team (ASM-005) | |
| **External** | **O:** MX trust erosion + ad fatigue (active churn driver); VLC's SMB/scan/subtitle pockets; "mpv-quality UX" vacancy (mpvKt dead); Media3 users underserved on library+subtitle UX (Just Player gap); scoped-storage pain for incumbents on Android 11+ | **T:** VLC is free+trusted+massive (price floor is $0); MX ubiquity/distribution; platform policy shifts (file access, FGS); codec/licensing cost surprises (W3); native players improving; ad-funded rivals outspending on store presence |

---

## 5. Baseline vs plausible differentiators

**Commodity baseline (table stakes; ≥3 of 6 majors ship):** MKV/MP4/H.264/HEVC/VP9/AV1 playback; SRT/ASS rendering with track selection; gestures/speed/PiP/audio-track select; network-shares for most; library view for most; Android TV for most; casting for some; no-ads for FOSS cluster; HDR/DV "on supported hardware" claims.

**Evidence-backed differentiator candidates (only these clear the bar):**
1. Subtitle-system quality (fidelity + UX + reliability) — incumbents strong in breadth (MX) or fidelity (mpv) but weak in UX/sync/styling glue; `[R + V1]`.
2. Verifiable privacy/trust (no ads/trackers/accounts; honest permissions) — concrete contrast exists (MX); `[V1/V2 + R]`.
3. Library scan correctness + speed at scale on scoped storage; `[R + V1 issues]`.
4. Playback reliability focus incl. A/V sync, seek, resume across long sessions & regressions — evidenced as a perennial field grind (Nova changelog tail, VLC crashes, MX login/resume bugs); `[V1/R]`.
5. HDR/DV honesty + best-effort correctness (no false badges; documented fallbacks); `[V1 maintainer statements]`.
6. (R2 candidate) SMB + sidecar subtitles done right — strongest single gap but out of R1 scope per DEC-001; `[R multi-forum + V1 issues]`.

**Do NOT treat as differentiators:** format-count marketing, casting claims, "8K", battery claims, rank claims — see §7.

---

## 6. Pain-point synthesis (ranked)

Frequencies are qualitative counts of sampled 2025–26 sources `[E]` — not market incidence.

| # | Pain | Freq | Severity | Evidence | Lead sources (raw notes §) |
|---|---|---|---|---|---|
| 1 | SMB/NAS failures; SMB sidecar subs ignored | Med-High | High | **Strong** | VLC §1.3; Nova §5.1 |
| 2 | Library scan: slow/resets/misses/crashes on big trees | High | Med-High | **Strong** | VLC §1.5; KMPlayer §5.3; Nova §5.1 |
| 3 | HDR/DV: tinting, hwdec color bugs, DV+PGS conflicts, no passthrough | Medium (enthusiast-concentrated) | High where affected | Med-Strong | mpv §4.1; Kodi §5.2; Just §3 |
| 4 | Ad/monetization intrusiveness (esp. MX) | High in MX base | Med-High (churn) | **Strong** | MX §2.4 |
| 5 | Subtitle UX gaps (zoom hides subs, styling/sync missing, encoding misses) | High | Medium | Strong | VLC §1.5; Just §3; mpv §4.1 |
| 6 | Update regressions; resume/position bugs | Med-High | High (uninstall) | Med-Strong | Nova §5.1; VLC §1.5; MX §2.4 |
| 7 | Streaming protocol edge cases (HLS subs, RTMP, TLS) | Medium | Medium | Medium | VLC §1.3 |
| 8 | A/V sync work (Bluetooth, passthrough, speed, long sessions) | Medium | Med-High | Strong | Just §3 rationale; Nova changelogs |
| 9 | Privacy distrust & past vulnerabilities (MX) | Medium | High for segment | Strong | MX §2.5 |
| 10 | TV/remote UX flows (episode-next, D-pad) | Medium | Medium | Medium | Just §3; Nova §5.1 |

**Confidence note:** #1/#2/#4/#5/#9 are the best-evidenced; #3/#6/#8 are strongly indicated by maintainer/engine evidence but device-concentrated; #7/#10 are context outside R1 scope.

---

## 7. Competitive claims to avoid until independently validated

1. "Plays everything / all formats" (upstream Media3 lacks AVI-complete/WMV/Theora; 10-bit AVC variance).
2. "Full Dolby Vision support" (profile/hardware-dependent even for leaders; DV P7 = HDR fallback in Just; absent in mpv).
3. Any battery-life claim (no credible comparable data; reviews not admissible).
4. "Zero crashes / most stable" (field regressions documented across incumbents).
5. "Best subtitle rendering" (libass/mpv is the reference until we benchmark per-script).
6. "Casting that just works" (unverified since VLC's beta-era caveats).
7. "8K playback" (marketing-grade; device-dependent).
8. "Instant library scanning" (field weakness with our measured targets unproven).
9. "Zero data collection" as a slogan before verification artifacts exist (build transparency first).
10. "#1 video player" ranking-style claims (volatile, coarse).

---

## 8. Opportunity ranking (user value × evidence × feasibility × strategic fit)

| Rank | Opportunity | Value | Evidence | Feasibility (R1, our constraints) | Strategic fit |
|---|---|---|---|---|---|
| 1 | Subtitle system quality (fidelity where feasible + full UX: discovery, encoding, sync, styling, readability) | High | Strong | Med (RQ-06 decides fidelity ceiling; UX fully ours) | Core DH-2; in scope |
| 2 | Verifiable trust/privacy (no ads/trackers/accounts; permission minimalism; honest store posture) | High (segment) | Strong (contrast) | High | Core DH-3 |
| 3 | Library scan correctness + speed at scale (scoped storage, SD cards, 10k+ files) | High | Strong | Med (architecture-defined; NFR-driven) | Core DH-4 |
| 4 | Reliability discipline: long-session A/V sync, seek, resume, update-regression prevention | High | Med-Strong | Med (QA investment) | Core DH-1/DH-6 |
| 5 | HDR/DV honesty + best-effort correctness (incl. DV P7→HDR fallback honesty) | Med | Med-Strong | Low-Med (platform-bound; must stay honest) | DH-8 |
| 6 | Calm, reachable, customizable UI | Med | Weak (clutter complaints not strongly evidenced) | High | DH-5 — **demote to execution quality, not headline differentiator** |
| 7 | SMB + sidecar subtitles (R2 candidate; strongest single specific gap) | High | Strong | — (out of R1) | R2 gate Q1 |
| 8 | TV/casting UX | Med | Weak-Med | — (out of R1) | R2 candidates |

### 8.1 Monetization anchors (W4 input to docs 15/16)

- **Subscription benchmarks** (RevenueCat 2025, cited in W4 notes): download→trial median 6.2% (p90 20.3%); trial→paid in media/entertainment 43.8%; D35→paid freemium 2.18% vs hard paywall 12.11%; D60 revenue per install $0.31; ~30% annual churn in month 1.
- **Competitor price anchors:** Infuse $1.99/mo · $16.99/yr · $99.99 lifetime; nPlayer $4.99 one-time; Outplayer free + one-time unlock. For a no-backend, no-account app, a one-time unlock is the lowest-complexity candidate (spike RQ-08).
- **Rough revenue model:** `[E]` $0.03–0.20 gross per install depending on unlock rate and price (assumptions in W4 notes; nothing measured).
- **Ad-tolerance evidence remains thin** `[R only]` — never present "users hate ads" as a quantified fact (ties to §7 claim discipline).

---

## 9. Unanswered market questions and how to validate

| # | Question | Method |
|---|---|---|
| Q1 | Should "SMB done right" pull network playback into R1/R1.5, given it's the strongest evidenced gap? | Owner decision + spike cost estimate (doc 16); evidence strong, cost/UX risk real |
| Q2 | What split of MX's 1B+ uses local player vs OTT? | No public data; proxy via review-keyword mining (W1 extension) |
| Q3 | Which codecs/containers fail most in the field (VC-1, 10-bit AVC, TS/MPG, DV P7, AV1 10-bit)? | Structured compat corpus testing in spikes (doc 12 design) |
| Q4 | VLC 4.x/rewrite timing (could reset the landscape) | Track nightlies/repo quarterly |
| Q5 | Privacy segment size / willingness-to-pay (W4: no credible market-size figure found `[U]`; ad-tolerance evidence thin) | Zero-data landing test + review mining (doc 03 §5; doc 15) |
| Q6 | Just Player rating discrepancy / mpv Play-vs-FDroid deltas / Nova Play lag — minor, fix before quoting | Recheck store pages at store-copy time |
| Q7 | Do users want audio-only/music modes in a video player, or is that clutter? | User research, not copying (doc 03 methods) |

---

## 10. Sources

Full labelled source list with URLs: `research/competitors.md` §Sources (accessed 2026-10-18). Key primaries: Google Play listings (VLC, MX, Just, mpv, Nova, KMPlayer); VideoLAN news/wiki/download; MX support+privacy pages; Just Player GitHub README/releases; mpv-android releases/issues; Nova releases; Kodi v22 notes; Common Sense privacy evaluation; Tenable CVE writeup; AppBrain; user threads (Reddit/XDA) as `[R]` signals.

Market/monetization/compliance facts (§2.4, §8.1, §9): `research/market_compliance.md` (accessed 2026-10-18) — store pages and Apple/Google policy pages, RevenueCat 2025 benchmarks, regional pricing notes; `[U]` gaps listed in that file's Open questions.
