# 07 — Media Compatibility Matrix

**Version:** 0.2 (draft; tier assignments are targets, not validated results; v0.2 adds §13 competitor-claims discipline)
**Date:** 2026-10-19
**Status:** Draft. **No cell in this matrix has been tested by us.** Every entry is a *target tier* to be validated by the doc 12 fixture corpus and doc 13 methodology in the build phase; promotion/demotion rules in §3 keep it honest.
**Consumes:** 04 (FR-PLAY-002 error taxonomy, FR-SUB-002, FR-AUD-007), 05 (NFR-COMP-004), `research/platform.md` (CDD mandates, Media3 format notes), `research/licensing.md` (decode/licensing verdicts), 02 §7 (claims to avoid).
**Feeds:** 06 (fidelity-aware UI states), 08/09 (engine config), 12 (fixture corpus design), 15 (store copy), 17/18 (registers, traceability).

---

## 1. Purpose and scope

This matrix is the **single source of truth for what the product may claim and how failures are messaged** (DEC-010). It covers: containers, video codecs, audio codecs, subtitle formats, HDR/color, and the display/behavioral cases that affect perceived quality. iOS support is out of scope here (deferred per DEC-002; re-verified at the iOS gate, RQ-09) — one boundary note in §11.

Two facts shape everything below:

1. **On Android, decode support is primarily the device's, not the app's** (`research/platform.md`: ExoPlayer/Media3 plays what the device decoder supports; CDD mandates set the floor). Our tiers therefore describe *likely device populations plus our verification commitment*, not app-internal magic.
2. **Bundled software decode is a licensing minefield** (`research/licensing.md`, W3): we ship no bundled video software decoders in R1 (no HEVC/AV1/AVC software decode), no ProRes on Android, and for audio we ship only what the doc 11 register clears (candidate: AC-3/E-AC-3 after legal recheck ⚖️; FFmpeg audio extension is LGPL-2.1 self-built ⚖️). This bounds the matrix's realistic reach.

---

## 2. Tier definitions (levels)

| Level | Name | Meaning | Product behavior | Release impact |
|---|---|---|---|---|
| **A** | **Commit** | We will validate on the D1–D3 set and treat failures as defects | Supported; documented in help; may appear in user-facing copy **after** validation evidence exists | Blocks R1 exit for its cells until resolved or demoted |
| **B** | **Expected** | Platform-mandated or high-likelihood; likely fine, not release-blocking | Playback attempted normally; failures handled per FR-PLAY-004 taxonomy; not promised in store copy | Monitored; demote if the doc 12 corpus shows systematic failure |
| **C** | **Best-effort** | Works only where the device/engine path exists; quality varies | Attempt; degrade honestly (notices, fallbacks); no fidelity promise | None |
| **D** | **Unsupported** | Deliberate no-support (technical/scope) | Clear taxonomy error with honest explanation (e.g., "protected content is not supported") | None |
| **X** | **Blocked (legal/policy)** | Licensing/policy prevents shipping this decode path in any build config | Never enabled anywhere; no marketing mention; revisit only via doc 11 legal sign-off | None |

Additional rules: a cell may be **split** (e.g., pass-through vs bundled decode); splits are explicit. Devices declaring more than the floor may exceed the matrix — the matrix describes what we commit to, not device ceilings.

---

## 3. Method: evidence per cell, promotion & demotion

**Evidence classes used in the tables:** `CDD` = platform requirement, Android Compatibility Definition (primary `[V1]`, via platform.md refs); `MD3` = Media3/ExoPlayer documented behavior `[V1]`; `LIC` = licensing verdict from doc 11/W3 `[V1/⚖️]`; `INC` = competitor field signal `[V1/R]` (context only — **never evidence of our support**); `EST` = our engineering estimate `[I/E]`.

**Promotion (B/C → A):** only after (1) doc 12 corpus coverage, (2) doc 13-measured results within NFR budgets, (3) doc 18 gate checklist. **Demotion (A → B/C/D):** any systematic failure in validation, with change-impact assessment (doc 17). **Cells never rise by marketing** — copy follows the matrix, not the reverse (DEC-010, doc 02 §7).

---

## 4. Platform baseline (what is true before we write code)

| Fact | Evidence | Consequence for us |
|---|---|---|
| Handheld devices MUST decode H.264/AVC, HEVC, VP9, and (since Android 14) AV1 — AV1 Main incl. 10-bit | CDD `[V1]` | The **codec floor** of any modern Android phone is strong; our gap analysis focuses on containers, profiles, audio formats, subtitles, and quality |
| HDR10/HLG required only on HDR-declaring devices; Dolby Vision strictly conditional on device declaration | CDD `[V1]` | HDR support is a **device-class feature**, not an app feature; honesty layer is our job |
| Media3 decodes "what the device supports"; software extensions exist (audio FFmpeg ext; AV1/VP9/FLAC/Opus/FFmpeg) but are not all on Maven | MD3 `[V1]` | Video fallback = **platform software decoders** only; our shipped audio extension is self-built (LIC) |
| Bundled AAC decode triggers Via LA licensing questions; HEVC/AV1 software decode royalty ambiguous/contested | LIC `[V1/⚖️]` | Prefer platform for AAC; no bundled video software decode (DEC-003 refinement) |
| AC-3 patents expired (2017); last US E-AC-3 patent reported expired 2026-01-30 | LIC `[V1/R ⚖️]` | AC-3/E-AC-3 bundled decode = **candidate add-back** pending legal recheck |
| DTS/DTS-HD/TrueHD: no bundled-decode license found; passthrough avoids app-side licensing | LIC `[V1/⚖️]` | Pass-through-only policy (FR-AUD-007) |

---

## 5. Containers (target tiers)

| Container | Extensions | Target | Notes |
|---|---|---|---|
| MP4 / M4V / MOV (ISO BMFF) | .mp4 .m4v .mov | **A** | Baseline container; fragmented MP4 read support via Media3; fast-start check for seek perf |
| Matroska / WebM | .mkv .webm | **A** | The core personal-library container; WebM = muxed subset; EBML oddities covered by adversarial corpus |
| MPEG-TS / M2TS | .ts .m2ts | **B** | Commit split: H.264/HEVC streams → B; MPEG-2/interlaced variants → C (deinterlacing device-dependent) |
| MPEG-PS | .mpg .mpeg .vob(read) | **C** | Common in old captures; stream detection best-effort; DVD-VOB playback structure unsupported (D) |
| AVI | .avi | **C** | Media3 marks AVI incomplete (MD3); DivX/Xvid-class files tolerated best-effort, no quality promise |
| FLV | .flv | **C** | H.264+AAC FLV best-effort; VP6 → D |
| Ogg | .ogv .ogg | **C** | Theora video → C/D; Ogg audio (Vorbis/Opus) → B |
| 3GP / 3G2 | .3gp .3g2 | **B** | Phone-capture files; AMR audio path per §7 |
| Raw/other containers (WTV, RM/RMVB, MXF, M2P collections…) | — | **D** | Honest taxonomy errors; no silent attempts that hang |
| Audio-only files in video containers (MP3/FLAC/M4A/OGG) | — | **B** | Playable with player UI (artwork optional); policy in FR-PLAY-019 area |

Container-level features: chapters — MKV **B**, MP4 **C**; embedded cover art **B**; multiple video tracks **C** (switch policy doc 09); subtitle/audio track enumeration **A** (FR-SUB-001, FR-AUD-001).

---

## 6. Video codecs (target tiers)

| Codec | Target | Split / notes |
|---|---|---|
| H.264/AVC 8-bit (Baseline/Main/High) | **A** | The workhorse; profiles above High (Hi10P) excluded from commitment |
| H.264 High 10 (10-bit AVC) | **C** | Device-dependent; if no path: honest `UNSUPPORTED_PROFILE` (FR-PLAY-004); never silent corruption |
| HEVC/H.265 8-bit Main | **A** | CDD floor; the second workhorse for MKV libraries |
| HEVC Main10 | **B** on devices declaring 10-bit/HDR support; **C** otherwise | Fallback attempt (platform sw where present) with disclosure; no bundled sw HEVC (X, LIC) |
| VP9 (8-bit) | **A** | CDD floor; WebM ecosystem |
| VP9 Profile 2 (10-bit) | **B** on HDR-declaring devices; C otherwise | Same honesty path as HEVC Main10 |
| AV1 (8/10-bit Main) | **B** on Android 14+ / devices declaring AV1; **C** on older | Mandate covers new devices; older fleet varies; no bundled AV1 software decode commitment (LIC size/licensing) |
| MPEG-4 Part 2 (DivX/Xvid ASP) | **C** | Media3 software path where present; legacy-file audience; no quality promise |
| MPEG-2 | **C** | TS/PS legacy; interlaced content often disappointing — messaging must not overapologize, just explain |
| VC-1 / WMV3 | **C/D** | Hardware-dependent, frequently absent on phones; taxonomy error text must not imply a bug |
| H.263 | **C** | 3GP legacy |
| Theora | **C/D** | Rare; Ogg legacy |
| ProRes | **X (Android)** | LIC verdict: never bundle FFmpeg ProRes; iOS platform path only at iOS gate |
| Dolby Vision (video layer) | **C** | Device/profile-dependent: P5/P8 on declaring devices; P7 → documented base-layer/tone-map fallback behavior; **no "full DV" copy ever** (doc 02 §7 #2) |
| Multi-angle / MVC / SBS-3D, frame-packed 3D | **D** | Honest unsupported messaging |

---

## 7. Audio codecs (target tiers)

| Codec | Target | Split / notes |
|---|---|---|
| AAC-LC / HE-AAC | **A** | CDD floor; prefer platform (LIC) |
| MP3 | **A** | CDD floor |
| Opus | **A** | CDD floor (platform); used by WebM/OGG |
| Vorbis | **A** | CDD floor |
| FLAC (in MKV/OGG/FLAC files) | **A** (platform path since API 21-class devices; verify per fleet) | Lossless audience expectation |
| PCM / WAV | **B** | Simple containers; AV sync handling |
| AMR-NB/WB | **B** | 3GP/pocket-capture files |
| AC-3 (Dolby Digital) | **B/C device passthrough** + **C candidate bundled decode ⚖️** | Patents expired; add-back pending doc 11 sign-off (FR-AUD-007) |
| E-AC-3 (Dolby Digital Plus) | **B/C passthrough** + **C candidate bundled decode ⚖️** | Last US patent reported expired 2026-01-30 `[R]`; legal recheck gate |
| DTS / DTS-HD | **C passthrough only**; **X bundled decode** | No license found for app-side decode (LIC); message honestly when neither path exists |
| TrueHD | **C passthrough only**; **X bundled decode** | Dolby-branded; passthrough-only policy |
| ALAC | **C** | Via FFmpeg audio extension only if build enables it; rare on Android libraries |
| ATRAC, DSD, MQA, etc. | **D** | Honest unsupported |
| Channel handling | — | Output follows platform routing/downmix (FR-AUD-002); **no controllable-fidelity claims**; passthrough only where device+route supports it |

---

## 8. Subtitle formats (target tiers)

| Format | Target | Notes |
|---|---|---|
| SubRip (SRT) | **A** | Baseline; encoding handling per FR-SUB-005 |
| WebVTT | **A** | Baseline streaming-era format; also used in MP4 (wvtt) |
| TTML/IMSC | **A** | Baseline (MD3 parses) |
| SSA/ASS | **A display** / **B styling fidelity** | Rendering commit; styling fidelity (libass-class) is the RQ-06 spike; user-override policy per FR-SUB-003; if fidelity ceiling is lower than libass, say so once, plainly |
| CEA-608/708 (embedded) | **B** | Platform/engine path; test corpus includes US broadcast captures |
| PGS (image) | **C** | Engine capability spike decides; if absent → honest notice + no fake rendering. (Kodi v22's DV+PGS fix shows how sensitive this class is — doc 02 §6 #3) |
| VobSub (IDX/SUB image) | **C** | Legacy rips; image OCR out of scope (D) |
| SMI (SAMI) | **C** | Legacy; parsing best-effort |
| MPL2 / PJS / TMPlayer / AQTitle | **C** | Simple legacy timing formats; low-cost parsers |
| Teletext (TS) | **C/D** | Rare on mobile use cases; no fidelity promise |
| DVD/DVB subtitle streams | **C/D** | Structural support depends on engine paths |
| Styled-subtitle policy | — | User override wins over embedded styling after a one-time notice (FR-SUB-003); **never claim "best subtitle rendering"** (doc 02 §7 #5) |
| Sidecar matching | — | `basename.*` / `subs/` discovery per FR-SUB-001; naming-convention coverage tested in doc 12 |

---

## 9. HDR, color, and display behavior (target tiers)

| Case | Target | Notes |
|---|---|---|
| HDR10 / HLG10 on devices declaring support | **B** | Present natively where the device path works; stats overlay reports actual path (FR-DIAG-001) |
| HDR content on SDR displays / non-declaring devices | **C** | Tone-mapping quality is platform-dependent `[U]`; we commit to *honest indication + no false HDR badge*, not to a quality level (FR-PLAY-013) |
| HDR10+ dynamic metadata | **C** | Device-dependent; passthrough/tone-map outcomes documented in stats |
| Dolby Vision | **C** | See §6; profile-dependent; P7 fallback documented |
| Wide-color (P3) rendering | **B** | Platform compositor handles; no claims beyond device capability |
| Refresh-rate alignment | **C** | Feature-flagged safe-list approach (FR-PLAY-012); kill-switch; **never claim "buttery" anything** before doc 13 |
| VFR (variable frame rate) files | **B** | Tolerance path per FR-PLAY-008; screen-recording/phone-capture corpus |
| Rotation metadata | **A** | FR-PLAY-010; correctness is table stakes |
| Interlaced content | **C** | Deinterlacing device-dependent; messaging explains quality variance honestly |

---

## 10. Engineering escape-hatches inventory (what we deliberately do NOT ship)

| Option | Status | Why |
|---|---|---|
| Bundled FFmpeg **video** decode (any codec) | **X** in R1 | Royalty ambiguity (HEVC pools, AV1 litigation context) + size/toolchain cost (LIC/W3) |
| Bundled HEVC/AVC software decoders | **X** | Same; platform software decoders remain allowed |
| Bundled AAC decoder | **not shipped**; prefer platform | Via LA app-side license question ⚖️ (LIC) |
| Bundled AC-3/E-AC-3 decoders | **candidate (C)** | Patent-status window; add-back only after doc 11 legal sign-off ⚖️ |
| libmpv/libVLC engines | fallback candidates only | DEC-003; license/product cost; only if engine spike FAILs |
| Runtime download of codecs/artifacts | **X** (also Play policy) | Security + policy |
| Online subtitle fetching | **X** in R1 | FR-SUB-010 boundary |

---

## 11. iOS boundary note

Per DEC-002, iOS is post-R1. When the gate (RQ-09) runs, this matrix must be re-derived for AVFoundation realities (AVFileType gaps: no MKV/AVI/WebM; native subtitles WebVTT/IMSC1/iTT/CEA only — `research/platform.md` §8), likely yielding a different tier map. **No cell in this document may be reused as an iOS claim.**

---

## 12. Verification plan and promotion pipeline

1. **Doc 12** builds the fixture corpus: per-cell sample files (including adversarial), device matrix (D1–D3 + SD + HDR-declaring + DV-declaring devices), and automation harness hooks.
2. **Doc 13** defines measurement: success/failure classification, quality proxies (dropped frames, sync drift, first-frame, seek), and reporting format.
3. **Doc 18** carries the promotion checklist: a cell moves to **A** only with recorded evidence; regression demotes it and triggers change-impact.
4. **Error-text library** (doc 06) is generated *from this matrix*: every D/X cell gets authored, honest copy (FR-PLAY-004 taxonomy mapping).

## 13. User-facing claims discipline (feeds doc 15 store copy)

**Allowed (example patterns):** "Plays MKV/MP4 with H.264/H.265 on supported devices." · "Subtitles: SRT, SSA/ASS, WebVTT, TTML, with adjustable styling and sync." · "No ads, no trackers, no account."
**Forbidden until measured and approved:** "plays everything", "full Dolby Vision/Atmos support", "8K", "zero crashes", "best subtitles", "instant scans", "no data collection" as a slogan, "battery-friendly" (doc 02 §7 list applies verbatim).

**Competitor format lists are not evidence.** Vendor "supported formats" pages routinely list containers and codecs the app only *demuxes*, or that play only on specific SoCs (doc 02A §13 records where this could not be established). Two concrete traps:
- **Container ≠ codec.** "MKV support" in a store listing says nothing about 10-bit HEVC, PGS, or TrueHD inside it. Our claims must name the **cell**, never the container alone.
- **Decoder path matters more than format name.** The same HEVC file plays on one device and fails on another with identical app version — because the hw decoder differs. Any claim that reads as device-independent is a false claim.

Rule: a format may appear in user-facing copy only when the corresponding doc 07 cell is at tier **A (Commit)** and the claim names the condition ("on supported devices" is acceptable only where the doc 07 cell is A for the class being named).

## 14. Open questions

| # | Question | Owner |
|---|---|---|
| C1 | RQ-06 outcome: ASS styling fidelity ceiling (Media3 path vs libass integration) | 08/11 |
| C2 | PGS/image-subtitle support in the chosen engine path (device/engine-dependent) | 08/12 |
| C3 | AC-3/E-AC-3 add-back legality (⚖️) and real battery cost of bundled audio decode | 11/13 |
| C4 | MPEG-2/interlaced expectation management for cord-cutter audiences (copy testing) | 06/15 |
| C5 | AV1 software-decode temptation on old devices — firmly budget-gated (`C` only) | 08 |
| C6 | Whether Dolby Vision P7 fallback behavior matches Just-Player-class handling on target devices | 12/13 |
