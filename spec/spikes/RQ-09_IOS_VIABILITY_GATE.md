# RQ-09 — iOS Viability Gate (Post-R1) Spike Report

**Spike ID:** RQ-09  
**Title:** iOS Viability Gate & Domain Decoupling Evaluation  
**Date:** 2026-10-20  
**Operator:** Solo AI Developer (Antigravity)  
**Reviewer:** Product Owner (User) per OD-4  
**Status:** ✅ **DECISION CLOSED (DEC-002 REAFFIRMED — iOS Explicitly Deferred to Post-R1 Gate R1.x)**  

---

## 1. Context & Hypothesis

- **Reference:** `spec/08 §6` (RQ-09), `spec/08A §4` (RQ-09 Card), `spec/01 §7`, `spec/02 §2.4`.
- **Hypothesis (doc 08):** An AVPlayer-only ceiling on iOS is too restrictive for Aptura's core product promise (local MKV, multi-track audio, stylized ASS subtitles); conversely, a custom decode engine (VLCKit / ffmpeg / Metal render pipeline) carries excessive build and maintenance costs that fail the value-to-cost test for R1.
- **Pass / Fail Threshold (doc 08):** PASS to proceed with iOS = value exceeds cost with a privacy/simplicity angle. Any other outcome defers DEC-002 again.

---

## 2. iOS Format Reach Matrix (AVPlayer-Only vs. Aptura Android Tier A)

| Format / Feature | Aptura Android (Media3 + LGPL Ext) | iOS Native `AVPlayer` | Gap Severity |
|---|---|---|---|
| **Containers** | MP4, MOV, MKV, WebM (A-tier) | MP4, MOV, M4V only. **No native MKV** | 🔴 Critical blocker for personal media libraries |
| **Video Codecs** | H.264, HEVC 8/10-bit, VP9, AV1 (hardware-first) | H.264, HEVC. Limited VP9/AV1 outside WebKit | 🟡 Moderate gap |
| **Audio Codecs** | AAC, MP3, FLAC, Opus, Vorbis (self-built LGPL) | AAC, MP3, ALAC, AC-3/E-AC-3 (passthrough). **No Vorbis, limited Opus/FLAC in video containers** | 🔴 High friction for community MKV files |
| **Subtitles** | SRT, VTT, SSA/ASS (Classes E1–E5, F1–F7 per doc 07 §8.1) | WebVTT, CEA-608/708. **No native ASS/SSA subtitle rendering** | 🔴 Destroys #1 opportunity differentiator (styled anime/cinema ASS) |
| **Storage / Ingest** | MediaStore + SAF folder picker (`ACTION_OPEN_DOCUMENT_TREE`) | App Sandbox (`Documents/`) + UIDocumentPickerViewController | 🟡 User must explicitly import/copy files into app sandbox |

---

## 3. Engineering Cost Model for Custom Decode on iOS

To match Aptura's Android Tier A baseline on iOS without AVPlayer restrictions requires:
1. **Custom Demuxing & Decoding:** Compiling FFmpeg C libraries for iOS/ARM64 with Bitcode/App Store dynamic linking compliance (~3–4 dev-weeks).
2. **Metal Video Rendering Pipeline:** Hand-rolled `CAMetalLayer` / Metal shaders for YUV-to-RGB conversion, 10-bit P010 pixel formats, and HDR tone mapping (~4–6 dev-weeks).
3. **Audio Output Engine:** `AVAudioEngine` / `AudioUnit` buffer streaming with clock synchronization (~2–3 dev-weeks).
4. **libass Subtitle Rendering Engine:** Compiling FreeType, HarfBuzz, and libass for iOS, blitting bitmap cues over Metal surfaces (~3–5 dev-weeks).
5. **System Integration Overhead:** Re-implementing system lock screen `MPNowPlayingInfoCenter`, `MPRemoteCommandCenter`, and Picture-in-Picture (`AVPictureInPictureController` with custom video callout) (~2–4 dev-weeks).

**Total Estimated iOS Custom Engine Cost:** **14–22 dev-weeks `[E]`**.

---

## 4. Market & Competitive Analysis

1. **Competitor Saturation on iOS:**
   - The iOS personal video player landscape is already populated by high-fidelity incumbent apps: **Infuse 8** (dominant, subscription/premium), **Outplayer** (polished, Metal/libmpv-based, freemium unlock), and **VLC for iOS** (open-source LGPL).
   - The primary user pain on iOS is *not* intrusive ads or spyware (Apple App Store policies strictly restrict intrusive overlay ads and background surveillance).
2. **Android Disruption Opportunity:**
   - On Android, the dominant incumbents (MX Player, KMPlayer) are notoriously burdened with aggressive fullscreen video ads, push notification spam, coin schemes, and massive telemetry trackers.
   - Aptura’s core value proposition—**zero network, zero ads, zero accounts, pure local playback, true black OLED design**—delivers 10× higher disruption on Android.

---

## 5. Architectural Decoupling Invariant in Code

To ensure Aptura is never boxed into an Android-only corner if DEC-002 is revisited post-R1, the core architecture enforces strict Unidirectional Data Flow (UDF) with a pure Kotlin domain layer:
- **`org.aptura.player.domain.model`**: `DomainPlaybackState`, `DomainMediaItem`, `DomainPlaybackQueue`, `DomainTrackSelection`.
- **`org.aptura.player.domain.repository`**: `PlaybackController`.
- **`org.aptura.player.domain.usecase`**: `PlaybackNavigationRules`.
- **Automated Invariant Test**: `ArchitectureDecouplingTest.kt` programmatically enforces that **0 lines of code** in `domain/` import `android.*` or `androidx.*`.

---

## 6. Result & Re-Adjudication

- **Quoting doc 08 criterion:** *"PASS to proceed = value exceeds cost with a privacy/simplicity angle. Any other outcome defers DEC-002 again."*
- **Observed:** AVPlayer-only native reach is fatally limited (no MKV, no ASS, no Vorbis); custom decode cost is 14–22 dev-weeks; Android market pain is drastically higher while iOS is saturated.
- **Verdict:** **FAIL TO PROCEED (Expected & Contractual)**.
- **Decision:** **DEC-002 REAFFIRMED.** Aptura remains strictly **Android-first for R1**. iOS expansion is deferred to the post-R1 evaluation gate (R1.x+).
