# Platform media capabilities & constraints — research notes

Scope: Android (primary build target) and iOS (decision-relevant facts only). Platform docs only, no application code.
Date: all sources accessed **2026-10-18**. Version facts are as of late 2026 unless noted.
Evidence key: **[V1]** primary official (platform/vendor docs, CDD, policy pages) · **[V2]** verified secondary · **[R]** reported user · **[I]** inference · **[E]** estimate · **[U]** unknown/unverified. Source refs (A#/I#) map to "Sources" at the end.

---

## 1. Android — Media3/ExoPlayer (library layer)

- **Latest stable: Media3 1.11.1 (2026-09-10)**; previous stable 1.11.0 (2026-08-05). At access date no RC/beta/alpha channel listed. Media3 is the current-generation successor to standalone ExoPlayer; actively developed by Google (release notes updated Sept 2026, docs pages updated mid/late 2026). [V1 A2, A1]
- ExoPlayer **plays what the device decoder supports**; "the supported sample formats depend on the underlying platform rather than on ExoPlayer". Individual devices may support more than the platform baseline. [V1 A1, A3]
- **Software decoder extensions** (must be manually built + bundled): AV1, VP9, FLAC, Opus, FFmpeg, MIDI, IAMF, MPEG-H. None ship via Google Maven as drop-in artifacts. [V1 A1]
- Containers (progressive, native extractors): MP4/M4A/FMP4, WebM, Matroska (MKV), MP3, Ogg (Vorbis/Opus/FLAC), WAV, MPEG-TS, MPEG-PS, FLV (not seekable), ADTS, FLAC, AMR (CBR-seeking flags needed for MP3/ADTS/AMR). [V1 A1]
- Audio via **FFmpeg extension** (decoder name → codec): Vorbis, Opus, FLAC, ALAC, PCM µ/A-law, MP1/2/3, AMR-NB/WB, AAC, **AC-3 (ac3), E-AC-3 (eac3), DTS/DTS-HD (dca), TrueHD (mlp/truehd)**. Enabled per-build; codecs not enabled are not available. [V1 A1]
- **AC-3/E-AC-3/DTS/TrueHD are not in the Android platform's guaranteed format tables** → on stock platform decode they are device-dependent; Media3 covers them only through the (bundled) FFmpeg extension or device passthrough. [V1 A1, A3]
- **FFmpeg extension licensing/ops**: module code is Apache-2.0 but FFmpeg itself is "licensed separately"; built locally from source (recommended FFmpeg `release/6.0`, NDK r26b; not supported to build on Windows — Linux/macOS instructions given, "should be possible" via PowerShell). [V1 A26]
- FFmpeg license: **LGPL-2.1-or-later by default; GPL-2+ if GPL-covered parts are enabled**; not available under proprietary terms; compliance checklist = compile without `--enable-gpl/--enable-nonfree`, prefer dynamic linking, distribute FFmpeg source. [V1 A27]
- Google Play's Device & Network Abuse policy forbids **downloading executable code (dex/JAR/.so) at runtime** → FFmpeg/native decoder `.so` files must be shipped inside the app bundle, never fetched. [V1 A21]
- **Decoder fallback**: `DefaultRenderersFactory.setEnableDecoderFallback(...)` = "fallback to lower-priority decoders if decoder initialization fails" (default value not stated in the fetched Javadoc; commonly off — [I]); extension renderers with `EXTENSION_RENDERER_MODE_ON` are used when `MediaCodecAudioRenderer` doesn't support the format, `_PREFER` gives them priority. [V1 A28, A26, A33]
- HDR selection nuance: ExoPlayer's default `MediaCodec` selector picks a decoder that supports the required profile even if it sits lower in the codec list → can end up on a **software decoder** for high-profile content. [V1 A1]
- Subtitles (standalone, side-loaded): **WebVTT, TTML/SMPTE-TT, SubRip (SRT), SubStationAlpha (SSA/ASS)** are supported by MIME type (`TEXT_VTT`, `APPLICATION_TTML`, `APPLICATION_SUBRIP`, `TEXT_SSA`). CEA-608/708 supported when embedded (HLS/DASH statements). [V1 A1]
- ASS styling/library fidelity: docs promise format support, **not libass-grade ASS typesetting**; no official statement on advanced ASS styling → treat as [U] and validate in a spike. [I on doc scope, A1]
- HLS/DASH (context, out of scope but relevant): HLS containers MPEG-TS/FMP4(CMAF)/ADTS/MP3, subtitles CEA-608/708/WebVTT, LL-HLS supported, Widevine/AES-128; DASH fMP4/WebM/Matroska demuxed only (no MPEG-TS), TTML/WebVTT/CEA, Widevine/PlayReady(TV)/ClearKey. [V1 A1]

## 2. Android — platform decode mandates (CDD/CTS)

- Android 17 CDD (current) handheld **MUST** support, available to third-party apps: **decode** H.264 AVC, H.265 HEVC, MPEG-4 SP, VP8, VP9, **AV1**; **encode** H.264, VP8, **AV1**. Identical list in the Android 16 CDD. [V1 A4, A5]
- Per-codec history (platform table, "available on handsets/tablets from version X+"): H.264 BP 3.0+, H.264 MP 6.0+, **HEVC decode 5.0+**, VP8 2.3.3+, **VP9 decode 4.4+**, **AV1 decode 10+ and "encoder and decoder are mandatory beginning with Android 14"**; APV encoder+decoder mandatory from Android 16. [V1 A3]
- CDD detail (Android 17 CDD): H.264 → Main Profile L3.1 + Baseline; HEVC → Main Profile L3 main tier + SD profiles MUST, HD profiles MUST *if a hardware decoder exists*; "MUST support at least one of H.265 or VP9 decoding of 720/1080/UHD profiles" when display height ≥ resolution; AV1 → if AV1 is made available it **MUST support Main profile incl. 8-bit and 10-bit**; hardware AV1 decoder must cover 720p/1080p table profiles (SD/720p/1080p/UHD rows, 30 fps). TV adds MPEG-2 + UHD Main10 for UHD-capable TVs. TV also mandates AV1 decode (Android 17 CDD). [V1 A4, A5]
- **10-bit/HDR — device-dependent except where the device *claims* capability**: AV1 Main 10-bit is mandatory *when AV1 is supported*; VP9 12-bit is OPTIONAL; if a device declares a VP9 HDR profile it MUST accept/propagate HDR metadata (static + HDR10+) and display correctly; Dolby Vision applies **only if** the device declares `HDR_TYPE_DOLBY_VISION` (then it MUST ship a DV extractor + display DV correctly + base-layer track-ID rule). Handheld HEVC Main10 is not universally mandated by the sections reviewed → assume device-dependent. [V1 A4, A5]
- Platform doc (developer.android.com): "OEMs that add HDR support **must support HDR10 and HLG10**" (movies / UGC respectively); HDR10+/Dolby Vision are optional extras. Playback codec profiles in HDR are HEVC 10-bit. [V1 A3]
- If a handheld claims an **HDR display** (`Configuration.isScreenHdr()`), CDD requires advertising BT.2020-PQ EGL/VK extensions. [V1 A5]
- Media-performance-class (MPC) requirements in CDD (devices declaring a class): e.g., 6 concurrent 8-bit HW decoder sessions (3×1080p30 + 3×4K30) and 3 concurrent 10-bit HDR sessions at 4K30 (≤1 encoder), ≤1 dropped frame/sec; AV1 sessions only need 1080p even where 4K is otherwise required. [V1 A5]
- Practical reading: **H.264/HEVC/VP9/AV1 8-bit playback is a reasonable floor on any 2026 third-party-API surface; 10-bit/HDR10/HLG and Dolby Vision must be probed at runtime** (`MediaCodecList`/`HdrCapabilities`/`Display.HdrCapabilities`) rather than assumed. [I]

## 3. Android — storage, permissions, file access

- **Scoped storage** is default for apps targeting API 29+; app-only access to the app-specific dir + specific media the app created. [V1 A6]
- **MediaStore** is the optimized system index for media on external volumes; query `MediaStore.Video.Media` etc. via ContentResolver; `getContentUri(MediaStore.VOLUME_EXTERNAL)` covers external volumes (API 29+). Files stay after uninstall. [V1 A7]
- Permission model: API ≤28 → `READ_EXTERNAL_STORAGE` (+`WRITE_EXTERNAL_STORAGE` to modify, `maxSdkVersion=29` now recommended); **API 33+ → `READ_MEDIA_VIDEO` / `READ_MEDIA_IMAGES` / `READ_MEDIA_AUDIO`** (granular, runtime). Both media perms requested together → single system dialog. [V1 A7]
- **Play policy on `READ_MEDIA_VIDEO` (Photo & Video Permissions policy)**: apps targeting 33+ may request it **only when system pickers (Android Photo Picker) are not sufficient for core functionality**; must submit a declaration in Play Console; "custom pickers are not automatically qualified"; full compliance mandatory since 2025-05-28; non-compliant apps subject to removal. Android docs themselves carry a "Caution: restricted to specific use cases" note pointing at this policy. [V1 A19, A7]
- **Android Photo Picker** alternative: no storage permission needed; grants persist until device restart or app stop; use `takePersistableUriPermission()` for longer (cap: **5,000 grants**, oldest dropped after). [V1 A9]
- **Direct file paths** are allowed from API 30+ for media (File API / native `fopen`) *with* the appropriate media permission, "to help apps work with third-party media libraries". [V1 A7]
- **Direct-path performance**: sequential reads ≈ MediaStore; **random reads/writes can be up to 2× slower via direct paths → docs recommend the MediaStore API** (relevant for seeking/large MKV scanning). [V1 A7]
- **SAF (ACTION_OPEN_DOCUMENT / _TREE / _CREATE_DOCUMENT)**: the escape hatch for non-media and arbitrary user-picked files; API 30+ **cannot grant the root of internal storage, the root of (reliable) SD-card volumes, or the Download directory** via `OPEN_DOCUMENT_TREE`; iterating very large directories "might reduce performance". [V1 A8]
- MediaStore.Downloads items **not created by your app require SAF** to access. [V1 A7]
- **MANAGE_EXTERNAL_STORAGE ("All files access")** — high-risk permission, Play-restricted: permitted categories are effectively **file managers, backup/restore, anti-virus, document management**; the policy lists "**Media Files access**" as a **common invalid use** and points to MediaStore/SAF; requires the Permissions Declaration Form + Play approval; apps failing this "may be removed from Google Play". **A local video player does not qualify on media-access grounds** (file-manager phrasing is the only class that plausibly fits; [I] until declared). [V1 A18]
- Removable SD cards: MediaStore exposes media across external volumes (VOLUME_EXTERNAL), but SAF can't grant card roots on API 30+, and direct-path behavior on secondary volumes is not documented in the sources reviewed → [U]. Plan MediaStore + SAF as the supported paths. [V1 A7, A8; I]
- Android 16 (API 36): `MediaStore#getVersion()` is now unique per app (anti-fingerprinting) — minor but affects any version-string caching. [V1 A34]

## 4. Android — media app platform behavior

- **Picture-in-Picture**: manifest `android:supportsPictureInPicture="true"` + `configChanges` handling; API 31+ use `setAutoEnterEnabled(true)` for automatic, smoother PiP (no `onUserLeaveHint` gymnastics); ≤30 must call `enterPictureInPictureMode()`. PiP is the documented *alternative* to a mediaPlayback foreground service when the user is watching video. [V1 A10, A11]
- **Foreground service (Android 14+, target API 34+)**: must declare a type + matching permission; media playback → `android:foregroundServiceType="mediaPlayback"` **and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`**; missing type ⇒ `MissingForegroundServiceTypeException` at `startForeground()`. Documented purpose: "Continue audio or video playback from the background." Runtime prerequisites: none. [V1 A11]
- **Android 15 restriction**: `BOOT_COMPLETED` receivers **cannot start `mediaPlayback` FGS** (throws `ForegroundServiceStartNotAllowedException`) — relevant for "resume playback on boot" features. [V1 A12]
- **Android 17 (target API 37) — "background audio hardening"**: background audio interactions (playback, focus requests, volume APIs) require a running FGS that meets one of: FGS with while-in-use capabilities, or exact-alarm permission + `USAGE_ALARM` audio. [V1 A13]
- **Audio focus**: Android 12+ = system-managed (system forces fade-out on `AUDIOFOCUS_GAIN` from another media app; players stay muted until focus is re-requested; automatic ducking since API 26). **Android 15+ target: cannot request focus unless top app or running a FGS — otherwise `AUDIOFOCUS_REQUEST_FAILED`.** ExoPlayer can manage all of this via `setAudioAttributes(attrs, handleAudioFocus=true)`. [V1 A14]
- **Notifications**: media-session notifications are **exempt from the Android 13+ `POST_NOTIFICATIONS` runtime permission**; a Media3 `MediaSessionService` auto-publishes a MediaStyle `MediaNotification` with controls (works with Bluetooth/Auto callers). [V1 A16, A15]
- Casting (context only): Media3 ships a `CastPlayer`/`media3-cast` integration for Google Cast; app also needs the Cast SDK per its docs. Not in current scope. [V1 A31]
- Android 17 also introduces a **local-network permission** regime (target 37): LAN device communication (e.g., casting receivers, local servers) requires either system device pickers or the new runtime permission. [V1 A13]

## 5. Android — Google Play rules & quality gates

- **Target API level (current)**: from **2026-08-31, new apps and updates must target Android 16 (API 36)**; Wear OS/Automotive ≥35; Android TV/XR ≥34. Existing apps must target ≥35 to stay available to new users on devices running newer OS than their target. Extensions available to **2026-11-01**. [V1 A17]
- **App bundle**: since Aug 2021 new apps must ship as Android App Bundles on Play; >200 MB needs Play Feature/Asset Delivery. [V1 A23]
- **Data safety form**: mandatory for all apps; must declare data collection/sharing including **third-party libraries/SDKs**, shown on the store listing. A fully local player can truthfully scope this narrowly, but the form is still required. [V1 A22]
- **Android vitals bad-behavior thresholds (core vitals)**: user-perceived **crash ≥1.09%** of daily users (overall) / **≥8%** for a single device model; user-perceived **ANR ≥0.47%** overall / **≥8%** per model; Play "generally considers the last 28 days", may act sooner on spikes; consequences: reduced discoverability and, sometimes, a store-listing warning. [V1 A20]
- **Device and Network Abuse**: no self-update outside Play, no downloading executable code (dex/JAR/.so) at runtime, no interference with other apps/devices/networks; apps must comply with the **Core App Quality** guidelines (incl. stability — no crashes/ANRs). [V1 A21, A24]
- **"Broken functionality" policy note**: the historical Spam & Minimum Functionality help page (`answer/9898846`) **returned 404 on 2026-10-18**; treat broken-functionality expectations as enforced now via Core App Quality tests + vitals thresholds + pre-launch report tools. [observed/A32; V1 A24, A20]
- **Accessibility**: Android provides guidance + tooling — "Develop Accessible Apps" guidance (labels, focus order), **Accessibility Scanner** for automated checks, Material guidelines, plus automated testing docs. Brief, but expected for a mainstream player (labels on controls, captions UX). [V1 A30]
- Vitals-adjacent note: Play surfaces per-form-factor quality and can steer users away from high-crash titles; media apps are crash-prone around codec inits — invest in decoder-failure handling. [V1 A20; I]

## 6. Android — version adoption & low-end device constraints (late 2026)

Cumulative active-device share (API level ≥ X), aggregated from Google Play-based data via apilevels.com [V2 A25; not an official Google publication]:

| Android version | API | Devices on this API or higher |
|---|---|---|
| 17 | 37 | ~0% (0% listed; QPR beta tracked) |
| 16 | 36 | ~22% |
| 15 | 35 | ~41% |
| 14 | 34 | ~55% |
| 13 | 33 | ~69% |
| 12 | 31/32 | ~79% |
| 11 | 30 | ~87% |
| 10 | 29 | ~91% |
| 9 | 28 | ~94% |
| ≤8.1 | ≤27 | ~95–100% |

- Reading: **minSdk 24–26 covers ~96%+ of devices; the API 33+ permission model (READ_MEDIA_VIDEO) applies to roughly two-thirds of devices; roughly 9% of devices are API ≤29** (legacy permission path still matters). [V2 + I]
- Method caveat: Google's own distribution dashboard was retired; apilevels is a widely used community aggregation — treat as [V2] and re-check before locking minSdk. [I]
- Low-end floors (CDD): handhelds must have **≥4 GB non-volatile space for `/data`**; must report `isLowRamDevice()==true` below 1 GB RAM. Some 32-bit-only devices have minimum memory floors (416–592 MB range) — those are edge/legacy tiers. [V1 A5]
- Performance classes: CDD defines **Media Performance Class 31/33/34/35** (35 from Android 15); devices not declaring any MPC are effectively low-end for media purposes [I]; query via `androidx.core:core-performance`. Use as a feature-gate signal (e.g., HDR, 4K decode assumptions). [V1 A29; I]
- Typical 2026 entry hardware (3–4 GB RAM, entry-tier SoC): no single official source found in this pass → **[E]**; recommend gating by MPC/`MediaCodecList` probes plus real-device testing rather than spec sheets. [I]

## 7. iOS — AVFoundation/AVPlayer formats (not building now; decision facts)

- **Containers/file types** (`AVFileType` enum, exhaustive list captured): `mov, mp4, m4v, m4a, mp3, qta, wav, aiff, aifc, caf, ac3, eac3, au, amr, mobile3GPP, mobile3GPP2, avci, appleiTT, scc, ahap` + image types (heic/heif/jpg/tif/dcm/dng). **No `mkv`, `avi`, `webm`, `flv`, `ogg`, `ts`, `flac` constants** → MKV/AVI/etc. are not first-class AVFoundation file types. [V1 I2]
- **Device codec support (iPhone 16 Pro tech specs, official)**: video playback "supported formats include **HEVC, H.264, AV1, and ProRes**; HDR with **Dolby Vision, HDR10+/HDR10, and HLG**"; audio playback includes "**AAC, MP3, Apple Lossless, FLAC, Dolby Digital (AC-3), Dolby Digital Plus (E-AC-3), Dolby Atmos**". [V1 I3]
- **AV1**: decode is chip-dependent — documented as a *device* capability in tech specs (iPhone 16 Pro class) rather than as a general AVFoundation format; exact chip floor (A17 Pro vs older) was not re-verified here → **[U]**, verify per model before claiming AV1 parity. [V1 I3; U]
- **ProRes**: listed as a playback format on that device class (ProRes decode support is device-dependent; older chips lack it) → [V1 device-level; chip matrix U]. [V1 I3]
- **HLS-centric behavior**: Apple's authoring spec requires HLS video to be **H.264, HEVC, Dolby Vision or AV1**; HDR HEVC must be HDR10/HLG/DV; HEVC ≤ **Main10 L5.1 High tier**; **Dolby Vision in HLS must be Profile 5 (single-layer 10-bit HEVC)**; subtitles must be **WebVTT or IMSC1 (text profile)** with CEA-608/708 carried in video segments. [V1 I1]
- **Subtitles/subtitle formats**: native legible-media handling centers on embedded tracks + HLS (WebVTT, IMSC1/TTML subset, CEA-608/708); `appleiTT` and `scc` are recognized file types. **SRT and ASS/SSA are not documented native playback formats** (no file type, not in HLS spec) → a third-party player must render them itself. [V1 I1, I2; I]
- **Dolby Vision nuance**: DV is device-capability + HLS-profile-gated (profile 5 required for HLS); for **local files**, DV profile handling (e.g., profile 8.4 MP4 from iPhone captures) is not spelled out in the sources reviewed → **[U]**, likely falls back to base-layer/HLG behavior on some paths. [I1/I3; U]
- FLAC: appears in device playback formats, but not in `AVFileType` — whether plain `AVPlayer` plays `.flac` files directly is **[U]**; ALAC is the guaranteed lossless route (`.caf`/`.m4a`). [V1 I3, I2]

## 8. iOS — file access, networking, background/PiP

- **Files / document access**: `UIDocumentPickerViewController` provides access to documents/destinations **outside the app sandbox** (Files/`UIDocumentPicker`); URLs are security-scoped (`startAccessingSecurityScopedResource()` pattern). [V1 I5]
- **Photo library without permission**: `PHPickerViewController` (`PhotosPicker`) — "**apps don't need to request photo library permission** when using either class"; it is out-of-process. An "Add Photos Only" level exists for saving. [V1 I4]
- **Local network**: since iOS 14, local-network access requires `NSLocalNetworkUsageDescription` (and Bonjour service declarations for discovery) — relevant for DLNA/casting/local servers. [V1 I6]
- **Background audio**: requires the `UIBackgroundModes` key with value `audio` (plus an appropriately configured audio session, category playback). [V1 I7]
- **PiP**: `AVPictureInPictureController` for custom players; **"Only begin PiP playback in response to user interaction and never programmatically. The App Store review team rejects apps that fail to follow this requirement."** [V1 I8]

## 9. iOS — App Store review & privacy (decision facts)

- **4.2 Minimum Functionality**: app must be useful/"app-like" beyond a repackaged site; "simply a song or movie" belongs in the iTunes Store → a player must add real playback/library value. [V1 I9]
- **4.1 Copycats**: no minor-variation clones of popular apps. [V1 I9]
- **2.5.1 Public APIs only, must run on the shipping OS; phase out deprecated APIs.** **2.5.2**: apps self-contained in bundles; may not download/install/execute code that changes features — **bundled codecs (e.g., FFmpeg inside the app) are the consistent approach**; runtime code download is prohibited (same spirit as Android). [V1 I9; I]
- **No decoder-specific prohibition found** in the guidelines; the constraints that hit players are public-API, self-containment, and content rights. [observed I9]
- **3.1.1 Payments**: unlocking features/functionality (e.g., "Pro" unlock) **must use In-App Purchase**; own license-key/QR unlocking mechanisms are prohibited. [V1 I9]
- **5.1.1 + App Privacy**: privacy policy required in metadata and in-app; **App Store Connect "app privacy details" are required to submit**, covering third-party partners' data practices. [V1 I9, I10]
- Upcoming-requirements page (build SDK minimums) was not verified in this pass → **[U]** for the exact Xcode/SDK floor in late 2026.

## 10. iOS — version adoption (late 2026)

StatCounter, mobile iOS version share worldwide, **September 2026 snapshot** [V2 I11]: iOS 26.6 ≈ **56.6%**, iOS 26.5 ≈ 9.3%, iOS 26.3 ≈ 1.9%, **iOS 27 ≈ 2.5%**, plus an 18.x bucket (10.2% + 1.8%) that StatCounter flags as **partly a Safari anti-fingerprinting misreport of 26.x versions** (patched 2026-01-19). Working conclusion: **iOS 26.x ≈ two-thirds-plus of active iPhones; iOS 27 rolling out since Sept 2026**. [V2 + I]
- Apple's developer "App Store" support page (developer.apple.com/support/app-store/) **no longer exposes a static iOS-usage table via fetch** (JS-rendered; no percentages retrievable 2026-10-18) → use StatCounter-style public stats or App Store Connect analytics. [observed I12]

## 11. Cross-platform implications (product decisions)

- Subtitles are the biggest iOS gap: Android/Media3 natively covers SRT/SSA/ASS/WebVTT/TTML; iOS natively covers WebVTT/IMSC1/iTT/CEA. A feature-parity player implies an in-app subtitle engine (rendering + sync) on both platforms. [I]
- Audio format gaps (AC-3/E-AC-3/DTS/TrueHD) are solved the same way on both: bundled decoders (Android: Media3 FFmpeg extension; iOS: third-party). Shipping them means LGPL/GPL compliance work from day one. [I; A26/A27]
- Storage access strategy diverges: Android = MediaStore + photo picker + SAF (with a Play declaration if READ_MEDIA_VIDEO is used); iOS = Files picker + PHPicker (permission-free). [I]
- Background playback requires concrete platform plumbing: Android FGS `mediaPlayback` + MediaSession + audio focus (tightening in 15/17); iOS background-audio mode + user-initiated PiP. [I]
- HDR/DV are best treated as runtime-detected bonus paths, not assumed baselines (Android: device/claim-based; iOS: chip + HLS-profile-based). [I]

---

## Open questions

1. Read/write performance of MediaStore vs direct paths for **seeking in large MKV files** — need our own measurements (docs only say "up to 2× slower" for random I/O via direct paths). [A7 validated experimentally]
2. Will Google Play approve a `READ_MEDIA_VIDEO` declaration for a *player* whose core UX is a library grid (vs. picker-only)? Policy says gallery-class apps qualify; a player is adjacent — need a policy-precedent check or Console declaration draft. [A19]
3. Does Media3's SSA/ASS support render typical fansub secondary features (signs/timing/karaoke)? Official docs don't say; needs a spike. [A1]
4. `DefaultRenderersFactory` decoder-fallback default value (enabled or disabled) — confirm against source/version. [A28]
5. Android 17 background-audio hardening: does normal foreground-interactive playback + MediaSessionService satisfy "while-in-use capabilities" FGS without extra user-action plumbing? [A13]
6. AV1 decode floor on iOS: exactly which iPhone/iPad chips decode AV1 (A17 Pro and later?), and does AVPlayer use it. [I3; U]
7. Dolby Vision playback of **local profile 8.x MP4** on iOS (iPad/iPhone exports) — does AVPlayer preserve DV or fall back to HLG/SDR? [U]
8. iOS build-SDK minimum ("upcoming requirements") for submissions in late 2026. [U]
9. Which low-end SoC/RAM tiers to define as the support floor — no authoritative public 2026 dataset found; consider using MPC class + Play device catalog. [A29; E]
10. Casting scope: Cast (Android) and AirPlay (iOS) both imply extra permissions/UX (Android 17 LAN permission; iOS local-network) — decide if cast is in v1. [A13, I6]

---

## Sources (accessed 2026-10-18)

**Android / Google (official [V1] unless noted)**
- A1. Media3 ExoPlayer supported formats — https://developer.android.com/media/media3/exoplayer/supported-formats (page last updated 2026-07-21)
- A2. Media3 release notes (1.11.1, 2026-09-10) — https://developer.android.com/jetpack/androidx/releases/media3
- A3. Android platform supported media formats — https://developer.android.com/media/platform/supported-formats (updated 2026-10-07)
- A4. Android 17 CDD — https://source.android.com/docs/compatibility/17/android-17-cdd
- A5. Android 16 CDD — https://source.android.com/docs/compatibility/16/android-16-cdd
- A6. Data & file storage overview (scoped storage) — https://developer.android.com/training/data-storage
- A7. Access media files from shared storage — https://developer.android.com/training/data-storage/shared/media
- A8. Access documents with SAF — https://developer.android.com/training/data-storage/shared/documents-files
- A9. Android photo picker — https://developer.android.com/training/data-storage/shared/photopicker
- A10. Picture-in-Picture — https://developer.android.com/develop/ui/views/picture-in-picture
- A11. FGS types required (Android 14; mediaPlayback) — https://developer.android.com/about/versions/14/changes/fgs-types-required
- A12. Android 15 behavior changes (BOOT_COMPLETED FGS) — https://developer.android.com/about/versions/15/behavior-changes-15
- A13. Android 17 behavior changes (background audio hardening; LAN permission) — https://developer.android.com/about/versions/17/behavior-changes-17
- A14. Manage audio focus — https://developer.android.com/media/optimize/audio-focus
- A15. Background playback with MediaSessionService — https://developer.android.com/media/media3/session/background-playback
- A16. Notification runtime permission (media-session exemption) — https://developer.android.com/develop/ui/views/notifications/notification-permission
- A17. Play target API level requirements — https://support.google.com/googleplay/android-developer/answer/11926878
- A18. Play All-files-access (MANAGE_EXTERNAL_STORAGE) policy — https://support.google.com/googleplay/android-developer/answer/10467955
- A19. Play Photo and Video Permissions policy — https://support.google.com/googleplay/android-developer/answer/14115180
- A20. Play Android vitals (bad-behavior thresholds) — https://support.google.com/googleplay/android-developer/answer/9844486
- A21. Play Device and Network Abuse — https://support.google.com/googleplay/android-developer/answer/9888379
- A22. Play Data safety form — https://support.google.com/googleplay/android-developer/answer/10787469
- A23. About Android App Bundles (requirement) — https://developer.android.com/guide/app-bundle
- A24. Core App Quality guidelines — https://developer.android.com/docs/quality-guidelines/core-app-quality
- A25. API levels & cumulative usage — https://apilevels.com/ — **[V2]** community aggregation (not official; dashboard retired)
- A26. Media3 FFmpeg decoder README — https://github.com/androidx/media/blob/release/libraries/decoder_ffmpeg/README.md
- A27. FFmpeg license & legal — https://ffmpeg.org/legal.html
- A28. DefaultRenderersFactory (setEnableDecoderFallback) — https://developer.android.com/reference/androidx/media3/exoplayer/DefaultRenderersFactory
- A29. Performance class (MPC) — https://developer.android.com/topic/performance/performance-class
- A30. Accessibility on Android (guidance + scanner) — https://developer.android.com/guide/topics/ui/accessibility
- A31. Media3 CastPlayer — https://developer.android.com/media/media3/cast
- A32. Play "Spam and Minimum Functionality" page — https://support.google.com/googleplay/android-developer/answer/9898846 — **404 on 2026-10-18** (recorded here)
- A33. Media3 troubleshooting (decoder libs) — https://developer.android.com/media/media3/exoplayer/troubleshooting
- A34. Android 16 behavior changes (MediaStore version lockdown) — https://developer.android.com/about/versions/16/behavior-changes-16

**Apple [V1] unless noted**
- I1. HLS Authoring Specification for Apple Devices — https://developer.apple.com/documentation/http-live-streaming/hls-authoring-specification-for-apple-devices
- I2. AVFileType — https://developer.apple.com/documentation/avfoundation/avfiletype
- I3. iPhone 16 Pro Tech Specs — https://support.apple.com/en-us/121031
- I4. Selecting Photos and Videos in iOS (PHPicker; no permission) — https://developer.apple.com/documentation/photokit/selecting-photos-and-videos-in-ios
- I5. UIDocumentPickerViewController — https://developer.apple.com/documentation/uikit/uidocumentpickerviewcontroller
- I6. NSLocalNetworkUsageDescription — https://developer.apple.com/documentation/bundleresources/information-property-list/nslocalnetworkusagedescription
- I7. UIBackgroundModes — https://developer.apple.com/documentation/bundleresources/information-property-list/uibackgroundmodes
- I8. Adopting Picture in Picture in a custom player — https://developer.apple.com/documentation/avkit/adopting-picture-in-picture-in-a-custom-player
- I9. App Store Review Guidelines — https://developer.apple.com/app-store/review/guidelines/
- I10. App privacy details — https://developer.apple.com/app-store/app-privacy-details/
- I11. StatCounter iOS version share (Sept 2026) — https://gs.statcounter.com/ios-version-market-share/mobile/worldwide — **[V2]**
- I12. Apple developer App Store support page (no static usage table retrieved 2026-10-18) — https://developer.apple.com/support/app-store/ — [observed]

Notes on unreachable/limited sources: the Play "Spam and Minimum Functionality" policy URL 404s (A32); Apple's app-store support page is JS-rendered and yielded no percentages (I12); apilevels.com and StatCounter are secondary sources and labeled [V2] above.
