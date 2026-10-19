# 02A — Competitor Case Studies (Deep Dive)

**Version:** 0.3 (v0.3 resolves **CS-4** against documented practice — decoder labelling settled as technical name + when-to-use description; see `research/decoder_label_practice.md`) · **Date:** 2026-10-19 (sources accessed 2026-10-19 unless noted)
**Status:** Companion to `02_MARKET_AND_COMPETITOR_RESEARCH.md`. Doc 02 remains the synthesis and scoring layer; this document is the **anatomy layer** — what each app provides, which options it exposes, how the playback stack works, and how the UI/UX is structured.
**Adopted into the spec:** §12's implications were folded into doc 04 (FR-CTRL-017 new; FR-PLAY-003, FR-LIB-015, FR-LIB-011, FR-SUB-001, FR-SUB-004, FR-PLAY-011 sharpened; Q12 and Q13 both opened here and both **subsequently closed by research** — W6 `autoplay_defaults.md`, W7 `decoder_label_practice.md`), doc 06 (SCR-010f/g/h + two player states), doc 12 (TST-QUEUE) and doc 18 (FR-CTRL count 17). See doc 04 §5, doc 17 §1.3, and doc 00 Parts 22/24/26 for the change record.
**Consumes:** `research/competitors.md` (W1), plus primary docs fetched for this pass (official docs, repos, support pages, store listings).
**Feeds:** 04 (FR refinement), 06 (UX patterns), 07 (compat claims), 08 (decoder/engine design), 15 (monetization contrast).

**Evidence labels:** `[V1]` verified primary (official docs/repo/store/licence) · `[V2]` reliable secondary · `[R]` reported user experience · `[I]` inference · `[E]` estimate · `[U]` unknown/unverified.

**Hard limit — read this first.** Nothing in this document was produced by installing or operating any competitor app. There are **no tap-count measurements, no screenshots we took, no performance numbers, and no usability test results**. Everything is reconstructed from vendor documentation, source code and design notes, store listings, and user reports. Where a UI detail is not documented, it is marked `[U]` rather than guessed. Section §13 lists exactly what this method cannot establish.

---

## 1. Method and per-app source depth

The apps do not have equal evidence availability, and pretending otherwise would be dishonest. Depth tiers:

| Tier | Meaning | Apps |
|---|---|---|
| **D1 — documented internals** | Official user docs + source code / design notes; settings and behaviour can be described precisely | VLC, Just Player, mpv-android, Nova, Next Player, Infuse |
| **D2 — documented surface** | Vendor feature/support pages describe features and some settings; no source | MX Player, KMPlayer, nPlayer |
| **D3 — listing-level** | Store listing + release notes only; internals `[U]` | Kodi (adjacent), mpvKt (archived) |

**What "how it works" means here.** For D1 apps we can name the engine, the decode path, the subtitle renderer and the fallback logic. For D2 apps we can only describe the *vendor's stated* mechanism (e.g. MX's "HW+ own media framework") — we cannot verify it. Those statements are labelled as vendor claims, not facts about implementation quality.

---

## 2. Cross-app summary — the ten findings that matter

1. **Every serious player converges on the same gesture grammar** (vertical left = brightness, vertical right = volume, horizontal = seek, double-tap edges = ±10 s, pinch = zoom). Differentiation is not the gesture set; it is **whether the gestures can be disabled, remapped, and explained**. `[V1 for VLC/Just/mpv/Nova; V1 vendor for MX/KMPlayer]`
2. **Subtitle depth splits into two camps that never meet.** Fidelity camp: mpv/VLC (libass-class rendering, ASS styling) with thin subtitle *management*. Breadth camp: MX (12+ formats, gestures, sync panel, encoding, font folder) with a heavy UI. **Nobody ships fidelity + a first-class management panel + honest fallbacks in one app.** This is the single most defensible in-scope gap. `[V1/R]`
3. **Settings architecture is where apps lose users.** Just Player hides its entire advanced settings tree behind a **long-press on the gear** `[V1]`; Nova exposes an enormous surface (passthrough modes, protocol clients, scraper rules) `[V1]`; MX scatters settings across Decoder / Player / Subtitle / List / Display `[V2]`. There is no accepted convention for "advanced" in this category.
4. **Decoder fallback is a real design problem with a published solution.** Next Player documents a tri-state decoder model (HW / SW+ / FFMPEG) with a **bounded per-track fallback queue and explicit confirmation before degrading** `[V1]`. This is directly reusable as a design reference for our FR-PLAY-003/004.
5. **The library is the product for half the market, and the file chooser is the product for the other half.** Nova/Infuse build libraries with scraping, collections and smart rows; Just Player explicitly refuses a library and hands off to Nova `[V1]`. Our R1 choice (library-first, DEC-011) puts us in the harder camp and must be resourced accordingly.
6. **Scoped storage is the structural fault line.** Nova documents losing ASS/NFO/torrent visibility after dropping `MANAGE_EXTERNAL_STORAGE` and says Google refused its request while competitors hold the permission `[V1]`. MX still declares All-Files Access `[V1]`. This validates DEC-011 (never request it) *and* predicts exactly which files our degraded mode must handle.
7. **Network playback is table stakes for the paid/FOSS tier and a monetization surface for the mass tier.** VLC/Nova/Infuse/nPlayer/KMPlayer all ship SMB; MX ships SMBv2 plus its own OTT streaming. Our R1 exclusion (DEC-001) is a deliberate, documented gap — not an oversight.
8. **"No ads" is a real positioning wedge but a weak moat.** VLC, Just, mpv, Nova, Next Player, KMPlayer (claimed), Outplayer, Infuse (free tier) all ship ad-free. The differentiator is not the absence of ads; it is **verifiable absence of data collection** — only Outplayer and VLC make that claim credibly at the store level `[V1]`.
9. **HDR/Dolby Vision is universally caveated, including by leaders.** Nova documents hardware licensing, HDMI colour-space failures and a "disable Dolby Vision" escape hatch `[V1]`; mpv has no DV and broken HDR under hwdec (maintainer-reported, W1); Just maps DV P7 to HDR HEVC `[V1]`; Infuse exposes a DV Profile-8 on/off switch `[V1]`. This is the strongest evidence that our DH-8 honesty stance is correct and that a "DV supported" badge would be a lie.
10. **Two viable product shapes exist, and they are mutually exclusive at R1.** Shape A: *player-as-tool* (Just, mpv, Outplayer) — fast, minimal, file-first, no library. Shape B: *player-as-library* (Nova, Infuse, VLC, MX) — discovery, metadata, collections. Our spec chose B. The case studies show B's cost is concentrated in scan correctness, metadata edge cases and settings surface — exactly the areas docs 04/05 already target.

---

## 3. VLC for Android — the reference incumbent

### 3.1 Identity and model
Full libVLC port; free, no ads, no IAP, donations; 500M+ installs; Play + F-Droid + Amazon + Huawei. Engine: libVLC (LGPL core, GPL app). Cadence ≈ annual majors with betas (3.7.0 Feb 2026; 3.7.1 May 2026; 3.7.2 beta Jul 2026). `[V1]`

### 3.2 What it provides
| Area | Documented capability |
|---|---|
| Video | Broad container/codec support via libVLC; hardware and software decoding; multi-core; aspect-ratio adjustment; deinterlacing; 360° video; secondary display |
| Audio | Full music player; 10-band equalizer (rewritten in 3.7.0, auto-saving, disable-default-values option); audio boost to 200%; filters; background playback |
| Subtitles | Embedded + external; ASS and DVD subtitles; Teletext; closed captions; multi-track selection; font customization; audio/subtitle delay; download from OpenSubtitles with manual search; subtitle history |
| Library | Audio + video media library with search and history; folder browsing; grouping of similarly-named videos |
| Network | SMB, SFTP, FTP, UPnP, Plex discovery, HLS/DASH, RTSP; network stream URLs; **remote access** (browser-based control with OTP, added in 3.7.x) |
| TV / cast | Android TV, Chromecast, Android Auto, Chromebooks, PiP, media session |
| Settings | Export/import of preferences (3.7.0: now includes equalizer values; old backups incompatible) |

### 3.3 Settings catalogue (documented)
VLC organizes preferences into **UI, Video, Audio, Subtitles, Advanced, Parental control, Android Auto, Casting, Remote Access, Widgets** `[V2 — source-derived wiki]`.

**Video preferences documented individually** `[V1]`:
| Setting | Behaviour |
|---|---|
| Save audio delay | Persists per-video delay adjustments |
| Save brightness level | Keeps brightness across videos when enabled; resets otherwise |
| Save playback speed | Keeps speed across videos when enabled |
| Fast seek | Faster scrubbing, less accurate |
| Video transition | Shows title/controls when the next playlist item starts |
| Use custom PiP popup | Resizable PiP window; required for pre-Android-8 PiP |
| Resume played videos | **Always / Never / Ask confirmation** |
| Audio boost | Up to 200% device volume |
| Volume gesture | Enable/disable right-half vertical swipe |
| Brightness gesture | Enable/disable left-half vertical swipe |
| Double tap to seek | ±10 s at screen edges; **repeated taps accumulate** (3 taps = 20 s, 4 = 30 s) |
| Seek buttons | Show ±10 s buttons in the overlay |
| Prefer clone (secondary display) | Clone instead of using the phone as a remote |

**Advanced** exposes network caching, hardware acceleration, OpenGL settings, frame skipping, debug logs, media database operations, and custom libVLC options `[V2]`. This is a genuinely expert-grade surface — and a support burden.

### 3.4 Player UI anatomy (documented)
Overlay elements: title · battery/time · play/pause · aspect ratio · audio tracks · subtitle tracks · video menu (DVD ISO only) · lock · elapsed time · seek bar · total/remaining time `[V1]`.

**More menu:** equalizer · sleep timer · playback speed · jump to time · play as audio · pop-up player · repeat mode · A-B repeat · save playlist · add subtitle `[V1/V2]`.

**Audio/subtitle window:** one combined pane (audio pane omitted when the file has no audio tracks). Contains track selection, disable, **audio delay** with ±50 ms buttons and a calibration widget using **SOUND HEARD / SOUND SPOTTED** taps, and subtitle selection/download/history `[V1]`.

### 3.5 Gestures
Left half vertical = brightness · right half vertical = volume · horizontal = seek · edge double-tap = ±10 s (accumulating) · pinch = zoom. Zoom behaviour interacts badly with subtitles in user reports (subtitles do not follow the zoomed frame) `[R]`.

### 3.6 How it works
libVLC C engine via JNI; VLC's own demux/decode/render pipeline; MediaCodec for hardware decode with software fallback; OpenGL ES video output; libass for ASS rendering; preferences are translated into libVLC command-line-style options (`--freetype-rel-fontsize` etc.) `[V2 — source-derived]`.

### 3.7 Strengths / weaknesses
**Strengths:** unmatched breadth; genuinely free; zero declared data collection; TV/desktop-class features (remote access, equalizer, secondary display); settings portability.
**Weaknesses (evidenced):** SMB reliability and SMB sidecar subtitles; library scan completeness/speed; dated information architecture; expert settings exposed without guardrails; subtitle zoom interaction. `[R + V1 issues, W1]`

### 3.8 What we take / what we leave
- **Take:** resume policy with three states (Always/Never/Ask) — better than a boolean; accumulating edge double-tap; combined audio+subtitle pane; explicit "save per-video adjustment" toggles; settings export as a trust feature.
- **Leave:** the 10-category settings tree with a raw libVLC options escape hatch; two different "more" menus (player vs. app) with overlapping items.

---

## 4. MX Player (Amazon MX Player) — the mass-market case

### 4.1 Identity and model
Now branded **Amazon MX Player**, developer Amazon Mobile LLC. 1B+ installs, 14.3M reviews, "Contains ads", in-app purchases, updated 2026-10-15. The app is a **hybrid**: local player + OTT streaming (200,000+ hours, Amazon content, MX Fatafat vertical micro-drama). Gold/Silver subscriptions discontinued 2026-04-30 (per W1). `[V1]`

### 4.2 What it provides
| Area | Documented capability |
|---|---|
| Video | Broad format support; **three decoders: HW, HW+, SW**; multi-core decoding; 8K/4K/HD claims (vendor); night mode; video cutter; privacy folder |
| Audio | Equalizer + bass booster; music player; background play (long-press play) |
| Subtitles | 12+ formats incl. SSA/ASS full styling, SAMI ruby, VobSub, Teletext, PJS, VTT; online subtitle search; multi-subtitle; **AI subtitle translation** (external SRT only) |
| Gestures | Volume, brightness, zoom/pan, fast-forward/rewind, speed, **subtitle-based seeking**, resize/reposition subtitles |
| Multitasking | Floating window, PiP with custom controls |
| Network | SMBv2 local network; OTT streaming |
| Extras | Kids lock, themes, Wi-Fi transfer, MX Share |

### 4.3 Decoder model (the interesting part)
MX documents three decode paths `[V1 vendor]`:
- **HW** — stock Android media framework (MediaCodec).
- **HW+** — MX's *own* media framework; vendor claims it plays more files; required for >100% volume and background playback on some versions; disabled by default; hidden on unsupported devices.
- **SW** — CPU decode, with "speedup tricks" merged into SW since 1.7.

Per-path settings: default decoder, **network** decoder (separate SW/HW+ choices), and **Calibrate playback position** for devices with HW timing bugs (documented for AMLogic). Fast-seek toggle exists because some HW decoders only seek to keyframes. `[V1]`

**Why this matters:** MX's three-way model is the market's most explicit admission that (a) hardware decode is not uniformly correct, (b) the app needs a user-visible escape hatch, and (c) A/V timing can be device-wrong and needs calibration. Our FR-PLAY-003/004 should expose the same *concept* with less jargon.

### 4.4 Settings surfaces (reconstructed — full tree `[U]`)
| Group | Documented settings |
|---|---|
| Decoder | S/W, H/W, H/W+, network variants, calibrate playback position |
| Player | Fast seeking; touch controls (double-tap on/off, lock mode, kids lock); alternative screen lock; background play |
| Subtitles | Subtitle folder; encoding; text appearance (font, size/scale, colour, border, shadow, fade); font folder; default font override; H/W subtitle acceleration; sync ±0.1 s |
| List | "New" tag period |
| Display | Aspect ratio, zoom, subtitle scale, on-screen clock/battery |
| Audio | Equalizer, bass boost |

### 4.5 Player UI anatomy
Landscape overlay with top-right closed-caption button, bottom transport, and a **menu button that opens a vertical list** (Play / Display / Subtitle / Audio / Tools). Subtitle panel is a dedicated screen with **+/− sync at 0.1 s**, size, position and style. Long-press on transport buttons reveals secondary actions (zoom, lock target, shuffle/loop). Kids lock unlocks by tapping four corners clockwise. `[V1/V2]`

### 4.6 Gesture map
Left half vertical = brightness · right half vertical = volume · horizontal = seek · two-finger vertical = speed · pinch = zoom/pan · double-tap = play/pause (optional, off by default since 1.7.16) · subtitle gestures: horizontal scroll = move between subtitle cues (seeks video), vertical = move subtitle block, pinch = subtitle size. `[V1]`

### 4.7 How it works
Closed source. Vendor describes HW+ as its own media framework layered over platform decode, with per-device capability probing (reads `build.prop` on some devices). No public architecture detail. `[V1 vendor claim; internals U]`

### 4.8 Permissions and privacy posture
Declares **All files access** (`MANAGE_EXTERNAL_STORAGE`), microphone (voice search), Bluetooth (AV-sync calibration), camera + location (MX Share). Data safety declares collection of **location, personal info and four other categories**, encrypted in transit, deletion requests supported, "no data shared with third parties" (self-declared). History: installed-app collection, CVE-2020-5764 (patched 1.24.5), Common Sense "Warning" (W1). `[V1/V2]`

### 4.9 Strengths / weaknesses
**Strengths:** deepest subtitle *format* coverage; the most complete gesture vocabulary; decoder escape hatches; kids lock and privacy folder are genuinely useful local features; enormous distribution.
**Weaknesses:** ad density and OTT fusion (reviews cite 7 ad breaks in a 40-minute episode); account/login coupling for downloads with session loss; UI heaviness; permission breadth; privacy history. `[R/V1]`

### 4.10 What we take / what we leave
- **Take:** subtitle sync at 0.1 s granularity; subtitle-folder concept; decoder tri-state with per-network override; kids lock as a *local* feature (no account); long-press secondary actions (used sparingly).
- **Leave:** All-Files Access; ads/OTT in the player surface; account requirement for any local function; gesture overload (subtitle-scroll-to-seek conflicts with horizontal seek).

---

## 5. Just Player — the Media3 reference implementation

### 5.1 Identity and model
Unlicense; no monetization (donations); Media3 **1.11.1**; Android 6+ and Android TV; Play, F-Droid, Accrescent, Obtainium, Galaxy Store, AppGallery, Amazon, Aptoide. Deliberately **no media library**. `[V1]`

### 5.2 What it provides
14 shipped features: track selection · speed · horizontal swipe + double-tap seek · vertical brightness/volume · pinch zoom (Android 7+) · PiP (resizable 11+) · resize fit/crop · volume boost · auto frame-rate matching (TV/boxes) · post-playback actions (delete / next) · touch lock (long tap) · file-chooser app shortcut · third-party equalizer support · media session/audio focus · pause on headphone disconnect. `[V1]`

**Formats:** audio incl. AC-3/E-AC-3/DTS/DTS-HD/TrueHD/IAMF/MPEG-H via FFmpeg extension; video H.263/H.264/HEVC/MPEG-4 SP/VP8/VP9/AV1; containers MP4/MOV/WebM/MKV/Ogg/TS/PS/FLV, **AVI in progress**; streaming DASH/HLS/SS/RTSP; subtitles SRT/SSA-ASS (**limited styling**)/TTML/VTT/DVB. Explicitly cannot do WMV/Theora, and most devices cannot do 10-bit AVC. `[V1]`

### 5.3 Settings catalogue (complete)
Hidden behind **long-press on the gear** (or App info → settings) `[V1]`:
| Setting | Options / behaviour |
|---|---|
| Default audio tracks | Specific language · prefer device language · media file default |
| Default subtitle tracks | Specific language · device · file default · none |
| File access mode | SAF · MediaStore · legacy file access |
| Decoder priority | Prefer device or app decoders |
| Auto frame-rate matching | Seamless refresh switching on Android 11+ compatible displays |
| Tunneled playback | Improves 4K/HDR on TV |
| Dolby Vision P7 | Play UHD-Blu-ray DV7 as HDR HEVC |
| Auto PiP | Enter PiP on Home when playing |
| Skip silence | — |
| Repeat toggle | — |
| Subtitle embedded styles | Apply / do not apply styles defined in the file |

### 5.4 Player UI anatomy
Single screen. Bottom bar: file open, subtitle, resize, play/pause, seek. Long-press file-open sets a **root video folder** so sidecar subtitles auto-load; long-press subtitle opens **system Caption preferences**; long-press title opens MediaInfo (if installed). Settings reachable only by long-pressing the gear. `[V1]`

### 5.5 How it works
Media3 ExoPlayer + `extension-ffmpeg` (audio formats enabled); platform MediaCodec for video; `SubtitleView` for text rendering — which is why ASS styling is "limited" (upstream ExoPlayer issue #8435) and why styling is delegated to `CaptioningManager`/system preferences. DV7→HDR is a track-selection mapping, not DV decoding. `[V1 + source commit]`

### 5.6 Strengths / weaknesses
**Strengths:** extremely legible codebase and behaviour; honest format documentation; excellent TV/PiP/refresh handling; no ads/tracking; proves the Media3 + FFmpeg-audio stack is viable for codec-hungry users.
**Weaknesses:** no library; subtitle styling/sync absent; settings discoverability is poor (long-press is undocumented in-app); no URL entry; AVI incomplete. `[V1/R]`

### 5.7 What we take / what we leave
- **Take:** the **entire decoder/refresh/PiP/skip-silence option set** as a baseline checklist; system-Caption integration as an *accessibility* fallback; root-folder sidecar subtitle model; honest "cannot play" documentation.
- **Leave:** hiding settings behind a long-press; delegating subtitle style entirely to the OS (our DH-2 requires in-app control).

---

## 6. mpv-android — the fidelity reference

### 6.1 Identity and model
libmpv; LGPL-core; Play (scoped-storage build) + F-Droid (incl. TV/all-storage builds); ~500K installs; ≈1–2 month cadence. Not a reusable AAR — the README explicitly says to use the code as inspiration. `[V1]`

### 6.2 What it provides
Hardware + software decoding · gesture seeking/volume/brightness · **libass styled subtitles** · **secondary/dual subtitles** · advanced rendering (scalers, debanding, interpolation) · Open URL · background playback · PiP · keyboard input. `[V1]`

### 6.3 Settings / options
The app exposes mpv's own option space rather than a curated settings tree — advanced rendering options are the point. No font/shadow preference UI; styling comes from libass and the script. `[V1/R]`

### 6.4 How it works
libmpv + FFmpeg demux/decode; `vo=gpu` with libplacebo for rendering; `hwdec=mediacodec` for hardware decode; libass for subtitle rasterisation. Maintainer-reported limits: HDR broken when using hwdec; no Dolby Vision. `[V1/R, W1]`

### 6.5 Strengths / weaknesses
**Strengths:** best-in-class ASS fidelity; dual subtitles; rendering control; tiny surface area.
**Weaknesses:** thin file/library UX; no resume in some flows; advanced options are opaque; HDR/DV limits. `[R]`

### 6.6 What we take / what we leave
- **Take:** dual subtitles as a **MAY/R2 candidate** (it is a real enthusiast need and no Media3 app does it well); the principle that rendering quality is a feature, not a setting.
- **Leave:** exposing raw engine options; using mpv as the engine (GPL/LGPL integration cost — DEC-003 stands).

---

## 7. Nova Video Player — the library-first FOSS case

### 7.1 Identity and model
Fork of Archos Video Player; Apache-2.0; FFmpeg + MediaCodec; Play/Amazon/F-Droid/IzzyOnDroid/GitHub; donation-funded; very high cadence (v6.4.51 Aug 2026, v6.5.5 Oct 2026). `[V1]`

### 7.2 What it provides — the widest local-feature surface in the set
| Area | Documented capability |
|---|---|
| Library | Local + network indexing; TMDB scraping; NFO (Kodi spec, tmdbid only); local artwork naming conventions; `.nomedia` exclusion; TV badge showing scan phase (`L/N` storage, `S/D` scan/delete, `I` identify) |
| Playback | Play modes: Single · Folder · Repeat single · Repeat folder · **Binge watching** (cross-folder episode order); speed 0.05–2.00×; A-B repeat; aspect modes; projector mode |
| Audio | Three passthrough modes + force override; audio boost; PCM guidance |
| Subtitles | SRT/SUB/VobSub/PGS; **SSA rudimentary**; up to **128 subtitle tracks / 32 audio tracks**; OpenSubtitles download (credentials, quota) |
| Network | SMB1–3 (jcifs-ng default, smbj alt), UPnP, FTP/FTPS, SFTP, WebDAV/S; per-protocol caveats |
| Resume | Local + `.video.mkv.archos.resume.xml` sidecar on writable shares; Trakt scrobbling (last 100) |
| TV | Leanback rows incl. separate Anime row; row visibility/sort; D-pad and keyboard shortcuts |
| Intro skip | Crowd-sourced timestamps from theintrodb.org / introdb.app; recap skipped only during binge |

### 7.3 Settings catalogue (documented highlights)
- **Audio:** passthrough mode (Nova encapsulation / System encapsulation / Universal AC3 recode), Force passthrough, software decoding toggle, audio speed mode (AudioTrack vs FFmpeg atempo).
- **UI:** hide watched, smart home rows, ignore initial articles (20+ languages), recreate sort titles, TV row configuration.
- **Playback:** enable playback speed, enable projector mode, resume/private mode.
- **Network:** disable SMB UDP discovery (Wi-Fi driver workaround), protocol selection.
- **Subtitles:** OpenSubtitles account, subtitle folder behaviour.
- **Advanced:** rescrape all, recreate sort titles, debugging.

### 7.4 Player UI anatomy
Phone/tablet: playback menu tile for play mode, speed, aspect, subtitle/audio, intro-skip toggle. TV: D-pad Up/Menu opens the same menu. OSD includes a lock state; when locked, gestures show unlock instructions instead of acting. `[V1]`

### 7.5 Gesture map — the most elaborate in the set
Documented zone model: three columns (25% / 50% / 25%) × two rows (top 75% / bottom 25%) `[V1]`:
| Zone | Single tap | Double tap | Scroll |
|---|---|---|---|
| Left top (75%) | OSD toggle | −10 s | Volume |
| Left bottom (25%) | OSD toggle | Speed −0.05× | — |
| Centre (middle third, full height) | **Pause + show OSD** (if playing) / **Resume + hide OSD** (if paused) | — | — |
| Right top (75%) | OSD toggle | +10 s | Brightness |
| Right bottom (25%) | OSD toggle | Speed +0.05× | — |

Plus keyboard: Space/P/Enter play · arrows seek · 0–9 jump to 0–90% · F aspect · O overlays · A/# audio · S/J subtitle · Z/X subtitle delay · H/G speed · Menu/D-pad Up TV menu. `[V1]`

**Assessment:** this is powerful and *extremely* undiscoverable. Six distinct double-tap behaviours in four zones plus a state-dependent centre tap is a memory tax. It is the clearest negative lesson in the set.

### 7.6 How it works
Archos `avos` core: FFmpeg demux/decode, MediaCodec hardware decode, own player service; library backed by a local DB populated by MediaStore for local storage and protocol clients for network; scraping via TMDB; resume sidecars written next to media on writable shares. `[V1/V2]`

### 7.7 The scoped-storage case study (important for us)
Nova documents that after Google restricted `MANAGE_EXTERNAL_STORAGE` at API 31 it switched to MediaStore, and consequently **cannot see ASS, NFO or torrent files** on local/USB storage; it moved NFO/artwork into its app-specific folder; it states its permission request was refused while VLC, MX, Kodi and others hold the permission. `[V1]`

This is the most useful single document in the competitor set for our FR-PLAT-006 degraded mode: it tells us exactly which file classes break under MediaStore and what users lose.

### 7.8 Strengths / weaknesses
**Strengths:** deepest library feature set in FOSS; TV-first thinking; honest hardware caveats; sidecar resume; protocol breadth.
**Weaknesses:** settings density; gesture complexity; SSA rudimentary; scan/regression tail in changelog (seek-back false positives, audio silent after seek, ANR lock inversion, pitch drift, buffering) — evidence that this class of app is a permanent quality grind, not a one-time build. `[V1/R]`

### 7.9 What we take / what we leave
- **Take:** play modes (especially Folder and Binge); resume sidecar *concept* for R2 network; scan-phase feedback badge (great trust pattern); "ignore initial articles" sorting; aspect Auto-crop; projector mode (R2/TV); explicit hardware caveats in help.
- **Leave:** the six-zone gesture matrix; exposing three passthrough modes to normal users (we should auto-select and offer one override); protocol-client choices in settings.

---

## 8. KMPlayer — the feature-bloat case

### 8.1 Identity and model
PandoraTV; free, ad-free claim, VIP IAP; Android + iOS; 10M+ installs; Android 5.0+. `[V1]`

### 8.2 What it provides
4K/UHD playback · auto-scan of device/SD/cloud · external media over FTP/FTPS/SFTP/UPnP/SMB/WebDAV/NFS · cloud (Dropbox, OneDrive) · URL streams · Chromecast + iTunes · subtitle styling/position/size + download · speed 0.25–4.0× · section repeat · flip/rotate · playlists · equalizer · sleep timer · bookmarks · quick buttons · Wi-Fi transfer · themes. Premium: magnet/torrent streaming, GIF creation, crop, MP3 extraction. `[V1]`

### 8.3 Settings / UI
Vendor documents features, not a settings tree; the player exposes a "quick button" for toggling options and a large tools menu. Full settings structure `[U]`. `[V1/U]`

### 8.4 Quality signals
The vendor's own format list contains obvious errors (the "video formats" row begins "VI, WAV, AAC…"), which is a useful warning about vendor-published capability lists generally — including our own future store copy. User reports cite slow/crashing discovery on very large folder trees. `[V1/R]`

### 8.5 What we take / what we leave
- **Take:** almost nothing structurally; the bookmark and sleep-timer features are worth noting as low-cost utilities.
- **Leave:** feature accumulation as strategy (torrent client, GIF maker, MP3 converter inside a player); vendor format lists that mix audio into video.

---

## 9. Next Player — the closest architectural peer

### 9.1 Identity and model
GPL-3.0; Kotlin/Compose; Media3 + FFmpeg via `nextlib`; Material 3; network storage SMB/FTP/SFTP/WebDAV. Small but technically transparent. `[V1]`

### 9.2 What it provides
Media3 playback with **runtime decoder switching**; network storage; Compose UI; per-item decoder memory; decoder fallback with confirmation dialogs. `[V1]`

### 9.3 The decoder-switching design (published, directly relevant)
From the project's design note `[V1]`:
- One `ExoPlayer`; a `DecoderManager` attached to a custom `RenderersFactory`.
- Video and audio choices are **independent** and stored **per MediaItem**; new items default to `AUTO`; returning to an item restores its choices.
- UI offers **HW**, **SW+** (software MediaCodec), **SW** (FFMPEG); automatic selection stays internal and the UI shows the initialised category or “–”.
- State is published through MediaSession extras so the dialog updates without a playback-state event.
- Fallback is **owned by the app**, per track, with a bounded queue and duplicate-failure suppression:

| Failed selection | Next attempts |
|---|---|
| `AUTO` | Silently try `FFMPEG` |
| Explicit `HARDWARE` | Confirm → `SOFTWARE` → `FFMPEG` |
| Explicit `SOFTWARE` | Confirm → `FFMPEG` |
| Explicit `FFMPEG` | Confirm → `AUTO` |
| Exhausted | Player error dialog |

- Only present-but-unsupported tracks or decoder errors trigger recovery; missing tracks do not. Position, playlist and `playWhenReady` survive a switch.
- Their own device note records a **known FFmpeg video colour defect** in emulator testing (green/wrong colours with SwiftShader and host GPU) while MediaCodec output was correct — an honest reminder that "FFmpeg video fallback" is not free.

### 9.4 What we take / what we leave
- **Take:** the entire fallback policy table as the reference design for FR-PLAY-003/004; per-item decoder memory; confirmation-before-degrade; state via session extras.
- **Leave:** exposing three decoder names to users as primary UI (we should present *Auto / Compatibility / Software* with a technical detail disclosure); shipping an FFmpeg video path unless RQ-01/03 prove it (DEC-003 says audio-only FFmpeg — this case study supports that caution).

---

## 10. iOS set — the paid-tier bar

Included because the iOS gate (RQ-09) may activate later; **not** R1 scope. `[V1]`

### 10.1 Infuse 8 (Firecore)
Free + subscription ($1.99/mo · $16.99/yr · $99.99 lifetime per W1; recheck). Apple-only (iPhone/iPad/Apple TV/Mac/Vision). 8.5.7 Oct 2026.

**Feature surface:** library with TMDB metadata/artwork, collections, Smart Groups, extras, alphabet scrollbars, Direct Mode; playback of MKV/MP4/ISO/VIDEO_TS/BDMV without transcoding; DV Profile 5 & 8, HDR10/10+/HLG; Atmos (E-AC3) passthrough; SMB/NFS/FTP/SFTP/WebDAV/UPnP + Plex/Emby/Jellyfin + many cloud drives; Trakt/iCloud sync; skip intros/credits via community DBs; AI upscaling.

**Settings catalogue (documented, complete):** General (filenames, file management, sort, posters, appearance, icon, auto-download subs, cellular sync, Up Next rows, watched indicators, picks, hide spoilers, crash-data sharing, auto-select next) · Sync (Trakt, iCloud) · Metadata & Artwork (fetching, embedded/local artwork, local NFO, logos, external ratings, cache) · Collections & Groups (movie/TV grouping, TMDB collections, custom, details view, smart groups, version selection) · Playback (subtitles default, gesture control, continuous playback, resume behaviour, playback controls chapters-vs-time, forward/back skip seconds, background playback, landscape lock, **streaming cache: Auto/Limited/Memory-only/Legacy**, speed, **DV Profile 8 on/off**, skip intros/credits: On/Auto-delay/Auto-instant/Off, live previews) · Audio (5.1 HDMI, 5.1 AirPlay, volume boost 300%, downmix normalized/original, LFE in stereo) · Parental controls (restrictions, biometrics, passcode, app lock, search lock, Files-app disable, rating allowances, PIN frequency).

**Lesson:** the paid tier sells *control over playback edge cases* (cache mode, DV fallback, skip behaviour, downmix), not format counts. That is a product-shape insight for our future paid tier — if any.

### 10.2 nPlayer (Newin)
One-time $4.99 unlock (W1). Non-encoding playback; vendor claims official DTS/DTS-HD and Dolby AC3/E-AC3 support; embedded browser; Chromecast/UPnP; cloud sync; WebDAV/FTP/SFTP/HTTP/SMB/UPnP streaming and download; image processing (flip, brightness/chroma); aspect presets (1:1, 3:2, 4:3, 5:3, 16:9, 1.85:1…); HDMI bitstream passthrough; playlists; folder lock; subtitles SMI/SRT/SSA/IDX/SUB/LRC, multi-track, SSA/ASS styling + resident fonts, sync, font/colour/shadow/outline; speed 0.5–4.0×. `[V1]`

### 10.3 Outplayer
mpv-based; free with one-time "Advanced" unlock; **no ads, no playback restrictions**; App Store privacy label **"Data Not Collected"**. HDR, PiP, spatial audio, dual subtitles (with secondary position/delay), chapters/playlists, Files app + SMB + external drives, 4K HDR, pinch zoom, centre-tap play/pause, hold-and-drag scrub, screenshot gesture, URL entry, volume boost. Monthly cadence through 2026. `[V1]`

**Lesson:** a one-time unlock on top of a free, fully functional player is a viable, privacy-compatible model — and Outplayer's "Data Not Collected" label is the closest iOS analogue to our DEC-012 stance.

---

## 11. Cross-app pattern comparison

### 11.1 Gesture grammar
| Pattern | VLC | MX | Just | mpv | Nova | KMPlayer | Next |
|---|---|---|---|---|---|---|---|
| Vertical L = brightness | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Vertical R = volume | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Horizontal = seek | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Double-tap edges ±10 s | ✅ (accum.) | ✅ (opt.) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Pinch zoom | ✅ | ✅ | ✅ | ✅ | `[U]` | ✅ | `[U]` |
| Two-finger speed | — | ✅ | — | — | — | — | — |
| Subtitle-cue scroll | — | ✅ | — | — | — | — | — |
| Zone-based multi-action | — | — | — | — | ✅ (6 zones) | — | — |
| Gesture disable switches | ✅ | ✅ | partial | `[U]` | partial | `[U]` | `[U]` |
| Lock mode | ✅ | ✅ (kids) | ✅ | `[U]` | ✅ | `[U]` | `[U]` |

**Conclusion:** the grammar is settled; the open design problem is **conflict management and discoverability**, not invention.

### 11.2 Subtitle capability
| Capability | VLC | MX | Just | mpv | Nova | KMPlayer | Next |
|---|---|---|---|---|---|---|---|
| ASS/SSA full styling | ✅ libass-class | ✅ (claimed full) | ⚠️ limited | ✅ libass | ⚠️ rudimentary | ✅ claimed | ⚠️ Media3 |
| Bitmap subs (PGS/VobSub) | ✅ | ✅ | ❌ | ✅ | ✅ PGS/VobSub | ✅ | `[U]` |
| Dual subtitles | ❌ | ✅ multi | ❌ | ✅ | ❌ | ✅ multi | ❌ |
| Sync adjustment | ✅ ±50 ms + calibration | ✅ ±0.1 s | ❌ | via script | ✅ keys | ✅ | ❌ |
| Encoding override | ✅ | ✅ | ❌ | via script | `[U]` | `[U]` | `[U]` |
| Font/colour/size UI | ✅ | ✅ | ❌ (system) | ❌ | `[U]` | ✅ | `[U]` |
| Online search/download | ✅ OpenSubtitles | ✅ | ❌ | ❌ | ✅ OpenSubtitles | ✅ | ❌ |
| Sidecar auto-load | ✅ | ✅ folder | ✅ root folder | ✅ | ✅ | ✅ | ✅ |

**Conclusion:** the "fidelity + management" quadrant is empty. MX has management without reference fidelity; mpv has fidelity without management.

### 11.3 Library and settings
| Dimension | VLC | MX | Just | Nova | Infuse |
|---|---|---|---|---|---|
| Library | ✅ | ✅ + OTT | ❌ by design | ✅ deep | ✅ deepest |
| Metadata scraping | limited | OTT metadata | ❌ | TMDB + NFO | TMDB + NFO |
| Collections/smart groups | ❌ | ❌ | ❌ | partial | ✅ |
| Settings depth | very high | high | low (hidden) | very high | very high |
| Settings discoverability | medium | medium | **poor** | medium | high |
| TV UI | ✅ | ✅ | ✅ | ✅ leanback | ✅ tvOS |
| Network | broad | SMBv2 | via providers | broadest FOSS | broadest |

### 11.4 Monetization / privacy
| App | Model | Ads | Account required | Declared data collection |
|---|---|---|---|---|
| VLC | Free/donation | No | No | None (declared) |
| MX | Ads + IAP + OTT | Yes | For downloads/OTT | Location, personal info +4 |
| Just | Donation | No | No | None |
| mpv | Donation | No | No | None |
| Nova | Donation | No | No | None (FOSS) |
| KMPlayer | IAP (VIP) | No (claimed) | No | None (declared) |
| Next | FOSS | No | No | None |
| Infuse | Subscription | No | No | Crash data opt-in |
| nPlayer | One-time | No | No | `[U]` |
| Outplayer | Free + one-time | No | No | **Data Not Collected** |

---

## 12. Design implications for our spec

These are the concrete changes/additions this study justifies. Each is tied to an existing requirement so it can be accepted or rejected in the change process (doc 17 §7).

### 12.1 Adopt (strengthens existing requirements)
**Status legend:** ✅ landed in this revision · ◐ already covered before this pass · ○ deferred with reason · ✗ not adopted.

| # | Pattern | Source evidence | Our hook | Status |
|---|---|---|---|---|
| A1 | **Resume policy = Always / Never / Ask**, not a boolean | VLC documented | FR-LIB-015 (resume) | ✅ tri-state added |
| A2 | **Accumulating edge double-tap** (3 taps = 20 s…) | VLC documented | FR-CTRL-005 | ○ deferred — marginal value vs. gesture complexity; revisit with Q6 data |
| A3 | **Subtitle sync at 0.1 s with ± buttons**, plus a calibration aid | MX documented | FR-SUB-004 (sync) | ✅ calibration aid added; fine steps already existed |
| A4 | **Subtitle folder / root-folder** for sidecar discovery | MX + Just documented | FR-SUB-001 (external subs) | ✅ one extra subtitle folder added |
| A5 | **Decoder tri-state with confirmation before degrading** and per-item memory | Next Player documented | FR-PLAY-003/004 | ◐ boundedness + per-item memory landed; **confirm-before-degrade deferred** pending Q13 |
| A6 | **Scan-phase feedback** (what is being scanned, how many) | Nova TV badge | FR-LIB-004, KPI-ACT-01 | ✅ phase + remaining count added |
| A7 | **Play modes: Single / Folder / Repeat / Binge** | Nova documented | FR-CTRL-017 | ✅ Single/Folder/Queue/Repeat; **cross-folder binge deferred** to R1.x |
| A8 | **Ignore leading articles in sort** | Nova documented | FR-LIB-011 (sort) | ✅ added, off by default |
| A9 | **Aspect Auto-crop** in addition to Fit/Crop/Stretch | Nova documented | FR-PLAY-011 (aspect) | ✅ added with Fit fallback |
| A10 | **System Caption preferences respected** as an accessibility fallback, with in-app override | Just documented | FR-SUB-008 | ◐ already specified |
| A11 | **Settings export/import** as a trust/portability feature | VLC 3.7.0 | FR-SET-003 | ◐ already specified |
| A12 | **Post-playback action** (delete / next / stay) | Just documented | FR-PLAY-016 / FR-CTRL-017 | ◐ next/stay landed; **delete-after-play not adopted** (data-loss risk) |
| A13 | **Skip silence** as an optional audio feature | Just documented | FR-AUD (optional) | ○ deferred to R1.x — needs its own speech-detection spike |
| A14 | **Touch lock via long-press** with a one-line unlock hint | Just/Nova/VLC | FR-CTRL-006 | ◐ already specified |

### 12.2 Reject (with reasons)
| # | Pattern | Why we reject it |
|---|---|---|
| R1 | Settings hidden behind long-press | Discoverability failure; support cost; contradicts our "calm, reachable" DH-5 |
| R2 | Six-zone gesture matrix with state-dependent centre tap | Memory tax and conflict risk; our gesture map should stay ≤5 actions and be remappable |
| R3 | All-Files Access permission | DEC-011 — never; Nova's case shows the cost but also the correct alternative (MediaStore + SAF + degraded mode) |
| R4 | Ads/OTT fused into the local player | DEC-004/DEC-006; also the clearest churn driver in MX reviews |
| R5 | Account/login for local functions | DEC-012/no-backend; MX review evidence shows session loss breaks downloads |
| R6 | Exposing raw engine options (libVLC/mpv flags) | Support burden; violates claims discipline; hide behind diagnostics only |
| R7 | Vendor format lists that mix categories or overclaim | KMPlayer's list is internally wrong; doc 07 tiers already forbid this |
| R8 | Three user-facing passthrough modes | Auto-select + one "force passthrough" override is enough (Nova's own FAQ shows the confusion) |

### 12.3 Gaps we can own (confirm or adjust doc 02 §8 ranking)
1. **Subtitle system: fidelity + management + honesty.** Still #1. The case studies show no competitor occupies it. Concretely: ASS styling where RQ-06 allows, bitmap subs, dual-subtitle as R2, sync panel, encoding override, sidecar discovery, per-video style memory.
2. **Library at scale on scoped storage.** Still #3 but *raise its cost estimate*: Nova's changelog and MX/KMPlayer reports show this is a permanent engineering tail, not a feature.
3. **Trust/privacy.** Still #2. The differentiator must be *verifiable* (no INTERNET permission, no SDKs, reproducible build) — Outplayer and VLC prove the claim is credible at store level.
4. **Decoder honesty.** New entrant to the list: a user-visible "why did it fall back?" explanation with per-item memory. Next Player proves the design; nobody markets it.
5. **Do not claim:** "all formats", "DV support", "8K", "best subtitle rendering", "instant scan". Unchanged from doc 02 §7 — this study adds four more vendor examples of overclaiming.

### 12.4 What this changes in doc 06 (UX)
**Status: ✅ implemented in this revision** — SCR-010f (play-mode sheet), SCR-010g (Up next / ended), SCR-010h (compatibility notice), plus the Ended and Compatibility states in the state catalogue.
- Add a **decoder fallback notice** state to SCR-010 variants (currently PREPARING/BUFFERING/PLAYING/PAUSED/ENDED/ERROR + fallback notice — the notice exists, but the case studies justify a *persistent, dismissible* "playing in compatibility mode" chip with a "why?" sheet).
- Add a **scan progress surface** for first-run and large libraries (Nova's badge is TV-only; we should do it on phone too, in the library header).
- Confirm the **subtitle panel** scope: track list, style, sync, encoding, position — MX's panel is the benchmark; ours must add per-video reset and a preview.
- Confirm **gesture count** stays at the documented five plus optional two-finger play/pause; do not add zone-based speed gestures.

### 12.5 What this changes in doc 07/08
**Status: ✅ applied (Part 23)** — doc 07 §13 now carries the "competitor format lists are not evidence" rules (container ≠ codec; the decoder path matters more than the format name), and doc 08 §6.1 carries the prior-art notes for RQ-01/03/05/06/12.
- Doc 07: add a footnote that MX's and KMPlayer's published format lists are vendor claims and contain internal errors — reinforces the "documented ≠ validated" rule.
- Doc 08: cite Next Player's fallback table as prior art in RQ-01/RQ-03; cite Just Player's Media3 1.11.1 + FFmpeg-audio stack as existence proof for DEC-003; cite mpv's HDR/hwdec failure and Nova's DV caveats in RQ-12/DH-8.

---

## 13. What this study does NOT establish

1. **No usability findings.** We did not measure task success, time-on-task, error rates, or learnability. Statements like "Nova's gesture map is a memory tax" are **expert judgement `[I]`**, not test data.
2. **No visual critique.** We did not inspect screenshots at pixel level, measure contrast, or evaluate layout across screen sizes. UI anatomy here is structural, not visual.
3. **No performance or stability data.** No startup times, seek latencies, battery drain, crash rates, or scan throughput. Review complaints are signals `[R]`, not measurements.
4. **No codec validation.** "Supports X" means the vendor or README says so. We did not play a single file.
5. **No pricing verification.** Prices are from W1/store snapshots and must be rechecked before any external use.
6. **Incomplete settings trees** for MX and KMPlayer. Their full in-app preference lists are `[U]`; only documented surfaces are described.
7. **No iOS hands-on.** Infuse/nPlayer/Outplayer are described from documentation only.
8. **Time sensitivity.** Store listings, versions and prices change; everything here is a 2026-10-19 snapshot.

---

## 14. Open questions this study creates

| # | Question | Why it matters | Method |
|---|---|---|---|
| CS-1 | Does MX's "HW+" actually play files that Media3 cannot, or is it a decoder-wrapping claim? | Would change RQ-01's comparison baseline | Device corpus test (spike) |
| CS-2 | Is MX's subtitle *management* (sync/encoding/font folder) actually usable at speed, or is it buried? | Determines how much of the panel we must redesign vs. copy | Moderated usability test on MX |
| CS-3 | How often does ASS styling materially differ between libass and Media3? | Sets RQ-06's pass/fail threshold and whether "fidelity" is a real claim | Scripted subtitle corpus (RQ-06) |
| CS-4 | Do users understand "HW/SW/Auto" decoder labels, or do they need outcome language ("Best quality / Most compatible")? | Directly shapes FR-PLAY-003 UI | **Resolved 2026-10-19 — the dichotomy was false.** Every vendor pairs a technical name with a when-to-use description, and the one shipping bare tokens carries a documented support cost (`research/decoder_label_practice.md`). FR-PLAY-003 + SCR-010e now specify that pattern. The residual test is narrowed to the *wording quality of our own descriptions*, not to the technical-vs-outcome choice |
| CS-5 | Is dual-subtitle demand large enough for R2, or is it a loud minority? | Prioritization | Review mining + survey |
| CS-6 | Does a scan-progress surface reduce abandonment on 10k+ libraries? | Justifies the UX addition | Prototype test with large library |
| CS-7 | Would a "why did it fall back?" explanation reduce support contacts? | Justifies diagnostics work (doc 14) | Support-deflection estimate; later A/B |

---

## 15. Sources (accessed 2026-10-19)

**VLC:** `videolan.org/vlc/download-android.html` (features, 3.7.0) · `wiki.videolan.org/Documentation:Android` (feature matrix, UI anatomy) · `docs.videolan.me/vlc-user/android/3.X/en/video/video_player.html` (audio/subtitle delay, download) · `docs.videolan.me/vlc-android-user/more/settings/extra_video.html` (video settings) · `dev.to/npomepuy/vlc-for-android-370-1h6p` (3.7.0 changelog) · `deepwiki.com/videolan/vlc-android/6-settings-and-preferences` (settings categories; source-derived `[V2]`) · `github.com/videolan/vlc-android/blob/master/NEWS`.
**MX Player:** `play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad` (listing, permissions, data safety, reviews) · `mxplayer.net/faq.html` (decoders, settings paths, subtitle panel) · `mx.j2inter.com/features` (feature list, gestures) · `support.mxplayer.in` subtitle folder.
**Just Player:** `github.com/moneytoo/Player` README (features, formats, advanced settings, FAQ) · commit `39da84d3` (CaptionManager/embedded-style behaviour).
**mpv-android:** `github.com/mpv-android/mpv-android` README.
**Nova:** `github.com/nova-video-player/aos-AVP` FAQ (passthrough, network protocols, subtitles, resume, play modes, intro skip, sorting, aspect/projector, speed, gestures, shortcuts, API31) · `CHANGELOG.md` (v6.4.51 fixes).
**Next Player:** `github.com/anilbeesetti/nextplayer` · `feature/player/DECODER_SWITCHING.md` (decoder model, fallback table, device note).
**mpvKt:** `github.com/abdallahmehiz/mpvKt` (archived 2026-03-30; feature list).
**KMPlayer:** `kmplayer.com/mobile` (features, formats, premium).
**Infuse:** `firecore.com/infuse` (technical specs) · `support.firecore.com/hc/en-us/articles/360015608854-Settings-Overview` (full settings catalogue) · `firecore.com/releases` (8.5.x) · `firecore.com/blog/infuse-8-now-available`.
**nPlayer:** `nplayer.com` (features, formats, codecs, UI claims).
**Outplayer:** App Store listing `id1449923287` (features, privacy label, version history) · `github.com/OutplayerApp`.

Cross-reference: `research/competitors.md` (W1, 2026-10-18) for scale, monetization and privacy history; `research/market_compliance.md` (W4) for pricing/benchmarks.
