# Research Notes — Licensing, Patents, and Distribution Constraints (W3)

**Project:** Next-Generation Mobile Video Player — raw research notes
**Feeds:** doc 11 (`11_LICENSING_AND_DEPENDENCIES.md`), plus 07 (compatibility), 08 (tech evaluation), 15 (distribution)
**Status:** Phase 1 research — evidence collected, consolidation pending
**Last updated:** 2026-10-18 · **All sources accessed 2026-10-18** unless noted
**As of:** late 2026
**Scope:** license, patent, and distribution constraints for a documentation-only mobile video player (Android-first; iOS noted where relevant). No code exists or ships.
**Labels:** `[V1]` primary source · `[V2]` reliable secondary · `[R]` reported/discussion evidence · `[I]` inference · `[E]` estimate · `[U]` unknown/unverified. ⚖️ marks items **requiring qualified legal review** before any shipping decision. Rates/dates/versions marked "recheck" can change.

---

## 1. FFmpeg — license terms, build flags, linking, FFmpegKit status

### 1.1 Core license and build flags
- FFmpeg's own code is **LGPL v2.1-or-later** in the default build; the project's legal page documents the obligations and the recommended compliance route (ffmpeg.org/legal.html `[V1]`).
- `--enable-gpl` makes the whole binary **GPL v2+**, because GPL-only libraries and GPL-only source files get compiled in; such a binary cannot ship inside a proprietary app (ffmpeg.org/legal.html; github.com/FFmpeg/FFmpeg/blob/master/LICENSE.md `[V1]`).
- GPL-only libraries include **x264, x265, xvid, xavs, avisynth, frei0r, libcdio, libdavs2, librubberband, libvidstab** (LICENSE.md `[V1]`). A playback-only product needs none of these encoders; decoding does not require `--enable-gpl` (LICENSE.md `[V1]`).
- `--enable-nonfree` (e.g., **libfdk_aac**, **OpenSSL**) makes the binary **unredistributable** — those licenses are incompatible with (L)GPL (ffmpeg.org/legal.html; LICENSE.md `[V1]`).
- Apache-2.0 components (mbedTLS, RK MPI, OpenCORE, VisualOn, libastcenc) are incompatible with LGPLv2.1/GPLv2 → require `--enable-version3`; `smbclient` additionally needs `--enable-gpl` plus `--enable-version3` (LICENSE.md `[V1]`).
- GPL-only internal files (not libraries): some x86 asm (flac_dsp_gpl.asm, idct_mmx.c, vf_removegrain.asm), many libavfilter filters (eq, delogo, cropdetect, blackframe, …), and libpostproc, which is excluded from default builds (LICENSE.md `[V1]`). A player can avoid all of these.
- All video/audio decoders relevant to playback (H.264, HEVC, AV1, VP9, AAC, MP3, Opus, Vorbis, FLAC, AC-3, E-AC-3, DTS, TrueHD…) are LGPL-side code; only *encoders* like x264/x265 are GPL (LICENSE.md `[V1]`).
- Trademark: "FFmpeg" is Fabrice Bellard's registered trademark; do not imply project endorsement (ffmpeg.org/legal.html `[V1]`).
- Patent reality: FFmpeg grants **no patent license**; the legal page warns patents on codecs may require separate licenses and that pools have historically been vigilant (ffmpeg.org/legal.html `[V1]`).

### 1.2 LGPL compliance mechanics (the 18-point checklist)
- The FFmpeg legal page publishes an LGPL compliance checklist. Distilled requirements: build **without** `--enable-gpl` and **without** `--enable-nonfree`; **link dynamically** against shared libraries; ship the **exact corresponding source** of the FFmpeg version used, plus a diff of any changes (e.g., `changes.diff`) and the exact `configure` line; host that source on the same webserver that serves the binary; name FFmpeg + LGPL in an "about box" and in the EULA; do not add reverse-engineering restrictions beyond the LGPL's own terms; do not rename `avcodec`/`avformat` libraries to obscure names (ffmpeg.org/legal.html `[V1]`).
- LGPL-2.1 §6 relinking: if the library is **statically linked**, the distributor must supply object code enabling the user to relink with a modified library; using a **shared library mechanism** (library replaced at runtime) is the intended compliance path (GNU LGPL-2.1 §6, as summarized by ffmpeg.org/legal.html `[V1]`).
- Mobile twist: on Android/iOS all native code ships **inside the signed package** (APK `.so` / IPA framework). End users cannot swap a library the way they can on desktop, so the "suitable shared library mechanism" argument is weaker. This is a known gray area for FFmpeg-on-mobile; the conservative reading (written offer + relinkable object files + full source) is the safe posture **⚖️ [I]**.
- LGPL-3.0 adds GPLv3-style "Installation Information" (anti-tivoization) duties for "User Products"; satisfying those on locked app stores is awkward — one reason to prefer **LGPL-2.1 FFmpeg builds** over LGPL-3.0 artifacts **⚖️ [I]**.
- Google Play itself does not audit OSS license compliance; the obligations bind the distributor (us) directly `[I]`.

### 1.3 FFmpegKit retirement and alternatives
- FFmpegKit (Arthenica) was **retired 2025-01-06**; native binaries removed from Maven Central/CocoaPods/npm on **2025-04-01**; the GitHub repo was **archived 2026-07-02** (github.com/arthenica/ffmpeg-kit `[V1]`; itpathsolutions.com/ffmpegkit-shutdown-what-to-do-next `[R]`).
- Stated reasons: effort of keeping pace with upstream FFmpeg + **legal uncertainty after the MPEG LA→Via LA transition** raised codec-patent exposure questions for distributed apps (itpathsolutions.com `[R]`).
- FFmpegKit default license: **LGPL-3.0**; `-gpl` variants GPL-3.0; build scripts always used `--enable-version3`, never nonfree (github.com/arthenica/ffmpeg-kit `[V1/R]`).
- Continuation: **FFmpegKitNext** — source-only (no Maven/CocoaPods publishing); Nix/MSYS2 builds; FFmpeg 6.1+; release v9.0.0 with FFmpeg 9.0.1 dated 2026-08-24; LGPL-3.0 default (github.com/arthenica/ffmpeg-kit-next `[V1]`).
- Community drop-in forks on Maven Central exist (e.g., `dev.ffmpegkit-maintained:ffmpeg-kit-full`, LGPL-3.0, third-party publisher) — unvetted supply-chain and legal risk **⚖️ [R]**.
- Migration guidance lives in ffmpeg-kit issue #1099: self-host artifacts or fork; no single project carries the full mantle (github.com/arthenica/ffmpeg-kit/issues/1099 `[V1]`; itpathsolutions.com `[R]`).
- [I] Consequence: any FFmpeg integration is **built and maintained by us** (NDK/Xcode builds pinned to a known configure line) — which is also the only way to control the §1.2 checklist and the version3/anti-tivoization question.

## 2. Media3 / ExoPlayer (Apache-2.0) and its FFmpeg extension
- Media3 (AndroidX Media, including ExoPlayer) is **Apache-2.0** (github.com/androidx/media, LICENSE `[V1]`). No copyleft; obligations are notice/license-text retention only `[V1]`.
- Media3's FFmpeg extension (`libraries/decoder_ffmpeg`) provides **`FfmpegAudioRenderer` — audio decode only**, no video (github.com/androidx/media, decoder_ffmpeg/README.md `[V1]`).
- The extension is **not published on Google Maven**; integrators clone Media3 and build FFmpeg themselves via `build_ffmpeg.sh` with an `ENABLED_DECODERS` list (NDK r26b documented at time of research) (decoder_ffmpeg/README.md `[V1]`, recheck versions at adoption).
- Documented decoder support includes the **AAC, AC-3, E-AC-3, DTS/DTS-HD (dca), TrueHD, ALAC, MP3, AMR** families — precisely the formats Android's platform decoder table often lacks (decoder_ffmpeg/README.md `[V1]`).
- The module README carries an explicit **license note**: bundled external libraries are licensed separately and compliance is the integrator's responsibility (decoder_ffmpeg/README.md `[V1]`) — i.e., §1.2 duties re-attach to us.
- Other Media3 extension modules (also self-built, not on Google Maven): AV1 (libgav1), VP9, FLAC, Opus, MIDI, IAMF, MPEG-H (github.com/androidx/media `[V1]`; recheck module names at adoption) — relevant only where platform decode is missing.
- Android platform media stack (Media3's default): audio decode of **AAC-LC/HE-AAC/xHE-AAC, FLAC, MP3, Opus (5.0+), Vorbis, PCM**; video **H.264, HEVC Main Profile L3 (5.0+), VP8/VP9 (4.4+), AV1 (decode 10+, mandatory 14+)**; the platform audio table lists **no AC-3 / E-AC-3 / DTS decode** (developer.android.com/media/platform/supported-formats `[V1]`).
- Dolby Vision on Android is not an AOSP feature; CDD tracks DV profiles (e.g., 8.4) as OEM-dependent (source.android.com CDD `[V2/R]`). iOS gets DV through system frameworks `[V2 recheck]`.
- [I] Architecture implication: Media3 = Apache-2.0 shell + platform codecs; the FFmpeg extension is the "escape hatch" for missing audio formats, and the entire licensing stack of §1 re-attaches to that escape hatch. Do not bundle software video decode initially (see §7).

## 3. libVLC vs the VLC application (VideoLAN)
- **libVLC is LGPL-2.1** ("released under the LGPL2.1 license") (videolan.org/vlc/libvlc.html `[V1]`).
- The **VLC application** (desktop/mobile) is **GPLv2** (COPYING in the source tree; videolan.org/legal.html `[V1]`). VLC app code cannot be reused in a proprietary app; libVLC embedding triggers LGPL obligations equivalent in kind to §1.2 (dynamic linking, source offer) `[V1]`.
- Trademarks: "VLC", "VideoLAN" and the cone logo are trademarks; they may not be used to promote non-OSI software or to imply endorsement (videolan.org/legal.html `[V1]`). Do not name the app "VLC-something" **⚖️**.
- VideoLAN's published position is that French/EU law does not recognize software patents — this is **VideoLAN's jurisdictional interpretation, not a waiver of patent exposure in the US or elsewhere** (videolan.org/legal.html `[V1]`) **⚖️**.
- iOS caveat: static linking plus App Store distribution makes LGPL relinking duties hard in the same way as Android; VideoLAN's iOS app is distributed with published source as a GPL application `[R/U — recheck iOS licensing at adoption]` **⚖️**.
- [I] Practical comparison vs Media3 + FFmpeg extension: libVLC = one dependency, hardware decode, broad format coverage, but a larger, harder-to-audit native stack; Media3 = cleaner architecture but we build FFmpeg ourselves. Codec-patent exposure is essentially the same, since both embed FFmpeg-derived decoders `[I]`.

## 4. mpv / libmpv, libass, and bundled fonts
- mpv's default build is **GPLv2+ for the whole program** (github.com/mpv-player/mpv, Copyright file `[V1]`).
- Building with `-Dgpl=false` yields **LGPLv2.1+**, but disables GPL-only parts: X11 VO, OSS audio, vdpau, jack, DVD/CDDA/DVB, CACA, legacy d3d VO, and more (mpv Copyright `[V1]`).
- The Copyright file warns explicitly: **linked libraries can change the final license** — LGPL mpv linked against a `--enable-gpl` FFmpeg becomes GPL overall (mpv Copyright `[V1]`). Any mpv use forces the same FFmpeg discipline as §1.
- The LGPL mode is intended for **libmpv embedding**, not the CLI (mpv Copyright `[V1]`).
- libmpv on mobile: no official Android/iOS distribution from the project; third-party apps exist (e.g., mpv-android, GPL) `[R]`; embedding on mobile is significant integration work plus the LGPL-on-mobile question of §1.2 **⚖️ [I]**.
- **libass is ISC-licensed** (permissive; attribution only); latest release 0.17.5 dated 2026-06-24 (github.com/libass/libass `[V1]`). libass renders ASS/SSA subtitles and ships **no fonts** of its own `[V1]`.
- Fonts: libass takes fonts from the platform or from assets you bundle. Bundling fonts adds **font licenses** on top: SIL OFL fonts are redistributable with notices; OFL forbids selling the fonts by themselves and reserved font names must not be used for modified versions (openfontlicense.org; scripts.sil.org/OFL `[V2/V1]`). Proprietary fonts cannot be redistributed in an app without a license **⚖️ [V2/I]**.
- [I] Recommendation inputs: bundle **only OFL fonts** (e.g., the Noto family) or none (rely on system fonts); record each font file + license in the register (see §7, §8).

## 5. Codec patent exposure (separate from open-source licenses)

### 5.1 HEVC (H.265)
- Two device-facing pools dominate: **Access Advance HEVC Advance** and the former **Via LA HEVC/VVC program**, acquired by Access Advance effective **2025-12-15** and now administered by **Video Codec Licensing LLC (VCL Advance)** (via-la.com/licensing-programs/hevc-vvc/; accessadvance.com/2025/12/15/access-advance-and-via-licensing-alliance-announce-hevc-vvc-program-acquisition/ `[V1]`).
- VCL Advance published rates: first **100,000 units $0.00**, then **$0.30/unit (R1)** or **$0.20/unit (R2)**; enterprise annual cap **$30,000,000**; royalties for sales from 2013-05-01 forward; coverage note says the portfolio license **"extends to devices implementing the technology"** (via-la.com/licensing-programs/hevc-vvc/ `[V1]`). Attachment 1 patent list updated 2026-10-01 `[V1]`.
- Access Advance HEVC Advance licenses HEVC decoders/encoders "whether made available in **devices, software, or through Cloud Based Services**"; there are royalty categories for **HEVC Software sold separately from a device** and per-authorized-user terms for cloud; caps exclude "certain HEVC Software"; a duplicate-royalty policy exists across pools (accessadvance.com/licensing-programs/hevc-advance/, /topic-what-do-we-license/ `[V1]`).
- The detailed HEVC Advance royalty-rate tables are published as **images — numbers not machine-extractable**, hence `[U]` until an exemplar license agreement is requested (accessadvance.com/topic-where-and-when-is-a-royalty-due/ `[V1]` for the topic page; rates `[U]`).
- Access Advance's **Video Distribution Patent Pool (VDP)** covers HEVC/VVC/VP9/AV1 **streaming services**, sized by MAU/subscribers/revenue — relevant if we ever run a streaming service, not for a local player (accessadvance.com/licensing-programs/vdp-pool/, /vdp-pool-license-scope `[V1]`).
- [I] Planning risk: whether a **pure software HEVC decoder inside an app** falls into an "HEVC Software" royalty category is ambiguous; device pools have historically billed chip/device makers, but the Software category exists explicitly **⚖️**.

### 5.2 AVC/H.264
- Via LA AVC pool (codec manufacture): first **100,000 units $0.00**, then **$0.20/unit** to 5,000,000, then **$0.10/unit**; enterprise cap **$9.75M/yr** (2017 onward); Section 2 streaming licenses are tiered by subscribers/MAU from **$100K (small/nascent)** to **$4.5M (Tier 1, 100M+)** (via-la.com/licensing-programs/avc/ `[V1]`).
- Many H.264 patents have expired; the last major US patent commonly cited (US 7,826,532) reportedly expires around **November 2027** `[R]`. The pool still operates and maintains its patent list (via-la.com AVC program page `[V1]`).
- [I] A local playback app is not a "codec manufacturer" selling units, and Section 2 targets streaming services; practical exposure sits with platform codecs (carried by OEMs). Bundling H.264 software decode would need advice **⚖️**.

### 5.3 AAC
- Via LA AAC: **$0.98/unit** for the first 500K, declining to **$0.10/unit** above 75M; a multichannel product counts as **1.5 units**; one-time initial fee **$15,000** (**$1,000** for small entities: ≤15 employees and <$1M revenue); **no fees on distributed bitstreams**; term 5 years, renewable; PC-software pricing only for products approved by Via LA (via-la.com/licensing-programs/aac/ `[V1]`).
- FAQ: **Google does not pay AAC fees on behalf of Android OEMs**; manufacturers/products must obtain their own license; the FAQ quotes Android CDD §5's disclaimer that codecs may require third-party patent licenses (via-la.com/licensing-programs/aac/ `[V1]`).
- The same FAQ frames who needs a license: "manufacturers or developers of **end-user encoder and/or decoder products**"; components/implementations are not covered — the party incorporating them must license (via-la.com/licensing-programs/aac/ `[V1]`).
- [I] So: an app that **bundles its own AAC decoder** (e.g., via FFmpeg) plausibly needs its own AAC program license; relying on **platform AAC decode** keeps app-side royalties out of scope (the platform vendor carries them) **⚖️**.

### 5.4 MP3
- Fraunhofer/Technicolor's MP3 licensing program **terminated 2017-04-23**; core patents expired (last US 6,009,399 expired 2017-04-16) (Fraunhofer IIS mp3 page `[V2]`; industry coverage `[R]`).
- [I] Industry practice treats MP3 decoding as royalty-free; theoretical implementation-specific patents have not surfaced in public disputes `[I]`.

### 5.5 AV1 (and VP9)
- AOMedia offers royalty-free licensing commitments (W3C mode) and the AOM Patent License 1.0 to implementers (aomedia.org/about/legal/ `[V1]`).
- However, **Sisvel runs an AV1 patent pool**: public list of **1,941 patents from 21 licensors**; consumer display devices **€0.32 standard / €0.24 compliant** per unit; non-display **€0.11 / €0.08**; committed-volume tiers from €0.168/unit; the license **"does not cover components or subassemblies"** (sisvel.com AV1 programme page `[V1]`). Sisvel claims roughly **50% of the AV1 finished-product market** is licensed (sisvel.com insights `[V1]`).
- **Dolby v. Snap** (filed 2026-03-24; US District Court of Delaware + Rio de Janeiro): the **first AV1 patent assertion against a streaming platform**, alongside HEVC claims; Dolby **made no royalty-free/FRAND commitment for AV1** — "Labeling a codec 'royalty-free' does not eliminate underlying patent rights" (accessadvance.com/2026/03/24/… `[V1]`; arstechnica.com 2026-03 `[V2]`). Further suits: Brazil (June 2026) and UPC HEVC actions (July 2026) (ipfray.com `[V2]`; iam-media.com `[V2]`).
- Google's public position: aware of the Sisvel pools; no plans to change VP9/AV1 use; Sisvel's licensors did not contribute to VP9 development (webmproject.org/about/faq `[V1]`).
- VP9: covered by Google's perpetual royalty-free WebM grant (terminates on patent litigation) (webmproject.org/license/additional/ `[V1]`), yet Sisvel's separate VP9 pool charges **€0.24/€0.18 (display)** and **€0.08/€0.06 (non-display)** per unit (sisvel.com VP9 programme page `[V1]`).
- No publicly reported royalty demand against a **software-only, non-device** AV1 distributor was found as of 2026-10-18 `[U]` — absence of evidence, not clearance **⚖️**.

### 5.6 Opus, FLAC, Vorbis, Matroska/WebM, ProRes
- **Opus**: royalty-free; BSD-3 reference implementation; royalty-free patent grants from Xiph/Broadcom/Microsoft with defense-termination clauses; IETF IPR disclosures exist (Qualcomm, Huawei, France Telecom, Ericsson) but Xiph's external counsel (Dergosits & Noah) advised that Opus can be implemented without licensing those (opus-codec.org/license/ `[V1]`).
- **FLAC**: "no licensing fees or royalties"; "no known patent" covers the FLAC/Ogg FLAC formats or their encoding/decoding methods; libFLAC/libFLAC++ are New BSD (Xiph); command-line tools are GPL (xiph.org/flac/license.html `[V1]`).
- **Vorbis**: Xiph states "fully open, non-proprietary, patent-and-royalty-free"; libvorbis is BSD (xiph.org/vorbis/ `[V1]`). No lawyer-signed formal clearance statement was located `[U]`; risk treated as low `[I]`.
- **Matroska**: "Anyone can use it or modify it … without paying any license or patents"; commercial use of the Matroska name/logo requires permission (matroska.org/legal.html `[V1]`). **WebM**: Google perpetual royalty-free patent grant, terminating on litigation (webmproject.org/license/additional/ `[V1]`).
- **ProRes**: Apple's "Apple ProRes and ProRes RAW Authorized Products" article states that **unauthorised codec implementations, "such as the FFmpeg and derivative implementations"**, are not authorized and may produce errors; licensing/certification is via ProRes@apple.com (support.apple.com/en-us/118584 `[V1]`). A ProRes RAW for Windows software license agreement exists, showing Apple licenses implementations selectively (apple.com/legal/sla/docs/ProResRaw.pdf `[V1]`).
- [I] Do not ship FFmpeg-based ProRes decode on Android; on iOS, ProRes playback via AVFoundation/platform codecs is the only safe route **⚖️**.
- Context: the MPEG-4 Visual (Part 2) patent family is commonly reported as expired since 2026-07-19 — it matters only for legacy containers/codecs `[R]`.

## 6. Dolby, DTS, and passthrough vs bundled decode

### 6.1 Dolby licensing mechanics
- Dolby's product licensing process: application → license agreement + initial fee → product testing/approval → listing as a licensee; ongoing quarterly royalty reporting, audit rights, and termination if no sales occur in a year (professional.dolby.com/licensing/ `[V1]`; handbook.dolby.com `[V1]`). This applies to any product implementing Dolby technology, including software.

### 6.2 AC-3 / E-AC-3 (Dolby Digital / Dolby Digital Plus)
- **AC-3**: last US patents expired **March 2017** (Dolby's own SEC filing language "2008–2017"; ETSI/AVSForum references) `[R/V2]`.
- **E-AC-3**: last US patent **US 7,516,064 expired 2026-01-30**; Phoronix reported the family of E-AC-3 patents "might now be expired" and is explicit it is *not* a lawyer (phoronix.com/news/Dolby-Digital-Plus-E-AC3-2026 `[R]`). Pre-expiry, a vendor blog (MainConcept) stated E-AC-3 patents were active "until early 2026" and warned the royalty situation was unclear and responsibility sits with the implementer (blog.mainconcept.com `[R]`). Fedora's legal review of enabling decode was pending (discussion.fedoraproject.org `[R]`).
- [I] Post-expiry, US-side patent risk for AC-3/E-AC-3 **software decode** is materially lower; non-US patents, Dolby trademarks ("Dolby Digital" naming/logos), and unknown family members keep this in recheck status **⚖️**.

### 6.3 Dolby Vision and Atmos
- **Dolby Vision (DV)**: proprietary end-to-end system; implementing DV requires a Dolby license + certification. Reported figures: content-side "Trim" license ~$1,000 one-time / $2,500/yr, per-device royalties reported under $3 (en.wikipedia.org/wiki/Dolby_Vision `[V2]`); Dolby licensing page for process `[V1]`.
- Platform reality: Apple platforms provide DV capture/playback via system frameworks on supported hardware `[V2 recheck]`; Android treats DV as OEM-specific (CDD `[V2/R]`).
- [I] Our app should render **HDR10/HLG** itself and depend on **platform-provided DV passthrough** where the device supports it; never bundle a DV pipeline **⚖️**.
- **Dolby Atmos**: decoding requires a Dolby-licensed decoder (consumer route on PC/Xbox is a paid Dolby software app) `[R]`; mainstream open-source players have historically omitted licensed Atmos decode. The standard legal path is **bitstream passthrough** of E-AC-3/TrueHD to a licensed sink (AVR/soundbar) or platform decode where the OS holds the license `[I]`.

### 6.4 DTS (Xperi)
- DTS (now Xperi) licenses decoders per device/implementation; no permissive/open license was located; the MDA content-mixing license is separate and described as fee-free `[U/R]`.
- [I] Players commonly **omit DTS software decode** and pass through. Bundling software DTS decode = **do not without counsel** `[I]` **⚖️**.

### 6.5 Passthrough vs bundled decode — working rule
- Passthrough (app forwards compressed audio to the output path / AVR without decoding) avoids app-level codec patent licensing; **bundled decode** pulls the codec's exposure into our binary `[I]`.
- Any bundled **software audio decode** beyond the patent-free/expired set (Opus, Vorbis, FLAC, MP3-now, AC-3/E-AC-3 post-expiry recheck) requires legal sign-off **⚖️**; video decode should stay on platform codecs (see §7).

## 7. Component × license × obligations × patent risk × recommendation

| Component | License | Key obligations | Patent/royalty risk | Recommendation |
|---|---|---|---|---|
| Media3/ExoPlayer core | Apache-2.0 (github.com/androidx/media `[V1]`) | Notices/LICENSE retention | Low (demux/container only) | **Adopt** as primary engine |
| FFmpeg build (LGPL, dynamic) | LGPL-2.1+ (ffmpeg.org/legal.html `[V1]`) | §1.2 checklist; source offer; no `--enable-gpl`/`--enable-nonfree`; version3 caution | Per-codec (below) | **Adopt** with compliance process **⚖️** (APK `.so`/relink question) |
| libVLC | LGPL-2.1 (videolan.org `[V1]`) | Same as FFmpeg + trademark care | Same exposure as FFmpeg `[I]` | **Hold** as alternative engine; iOS LGPL friction `[R]` |
| mpv/libmpv | GPLv2+ default; LGPL2.1+ with `-Dgpl=false` (mpv Copyright `[V1]`) | LGPL mode still infected by a GPL FFmpeg `[V1]` | As FFmpeg | **Avoid** for v1 unless LGPL build + LGPL FFmpeg justified |
| libass | ISC (github.com/libass `[V1]`) | Attribution only | None known | **Adopt** for ASS/SSA subtitles |
| Bundled fonts | OFL / others (openfontlicense.org `[V2]`) | OFL notices; no reserved-name reuse on modification | None | Ship **only OFL** fonts (e.g., Noto) or none |
| Platform decoders (MediaCodec / AVFoundation) | OS component (developer.android.com `[V1]`) | None app-side | OEM/platform carries codec licensing `[I]` | **Prefer** for AVC/HEVC/AV1/VP9/AAC + Dolby/DTS passthrough |
| Bundled HEVC software decode | LGPL via FFmpeg (LICENSE.md `[V1]`) | FFmpeg obligations | Pool ambiguity for "HEVC Software" (accessadvance.com `[V1]`) **⚖️** | **Do not bundle** for v1 |
| Bundled AV1 software decode (dav1d / libgav1) | Permissive: dav1d BSD-2-Clause; libgav1 Apache-2.0 `[V1 recheck]` | Notices | Sisvel AV1 pool exists; no known software-only demands `[U]` | **Not needed** (platform AV1 decode ≥ Android 10); optional later `[U]` |
| Bundled AAC software decode | Platform (preferred) vs FFmpeg LGPL | If FFmpeg: §1.2 | Via LA AAC license plausibly due app-side (via-la.com/aac/ `[V1]`) **⚖️** | **Prefer platform AAC**; legal advice before bundling |
| Bundled AC-3 / E-AC-3 decode | FFmpeg decoder (LGPL) | §1.2 | US patents expired 2017 / 2026 `[R]`; non-US uncertainty | **Candidate add-back** after legal recheck **⚖️** |
| ProRes decode (FFmpeg) | Not Apple-authorized (support.apple.com/118584 `[V1]`) | n/a | Apple disapproval; no license | **Never ship** on Android; iOS platform only |

## 8. Notices, attribution, SBOM, and store practice
- **Android/Google Play**: developers are responsible for displaying open-source notices; Google publishes `oss-licenses-plugin` + `play-services-oss-licenses` (`OssLicensesMenuActivity`) to render them (developers.google.com/android/guides/opensource `[V1]`). Note the plugin renders dependency metadata — it is **not** a substitute for manual notices for vendored native libraries (FFmpeg, fonts) `[I]`.
- Play review does **not** verify OSS license compliance; the obligations are contractual on the developer `[I]`.
- **Apple**: the Developer Program License Agreement requires third-party terms/notices compliance and puts IP claims on the developer (not Apple); App Store EULA minimum terms reinforce third-party license compliance (developer.apple.com DPLA download; app-store/review/guidelines/ `[V2]`).
- **SBOM**: CISA + 17 international partner agencies published **"2026 Minimum Elements for a Software Bill of Materials" v2.1 on 2026-07-29**, which **updates and replaces** the NTIA 2021 baseline: **17 data fields** (up from 7), adding **Component Hash Value + Algorithm, Component License, SBOM Author Signature, SBOM Tool Name, SBOM Generation Context**; scope explicitly includes open-source, AI and SaaS software (cisa.gov news 2026-07-29; cisa.gov resource page; ic3.gov/CSA/2026/260729.pdf `[V1]`).
- SBOM formats to use: **CycloneDX** and **SPDX**, with published field mappings (runsafesecurity.com mapping article `[V2]`).
- Minimum plan for this product (spec level): (a) generate a CycloneDX SBOM per release from Gradle + native build inputs; (b) maintain an in-app notices/licenses screen (Media3/Apache texts + FFmpeg LGPL text + full source pointer); (c) publish the FFmpeg exact-source archive + configure line + `changes.diff` at a stable URL **before** each release; (d) log every native library and font (license, source URL, version) in the register that becomes doc 11 `[I]`.
- Keep old source archives reachable across version churn (pin FFmpeg commits; archive per release) — the LGPL offer must cover each distributed version (ffmpeg.org/legal.html `[V1]` rationale) `[I]`.

## Open questions / legal review items
Every item below flagged **⚖️** requires qualified legal review before it can be relied upon for a shipping decision; none of this document is legal advice.

- **⚖️ LGPL on mobile**: does shipping an unmodified FFmpeg `.so` inside a signed APK satisfy LGPL-2.1 §6 relinking? Proposed mitigation: written offer + full source + relinkable object files + in-app notice.
- **⚖️ LGPL-3.0 artifacts**: if FFmpegKitNext (forced `--enable-version3`) is used, do "Installation Information" duties apply on locked app stores? Prefer LGPL-2.1 self-builds.
- **⚖️ App-side AAC license**: confirm whether bundling any AAC decoder (even in an audio-only FFmpeg build) triggers a Via LA AAC license for the app itself.
- **⚖️ HEVC software decode**: request the HEVC Advance "HEVC Software" terms and the VCL Advance exemplar agreement; determine whether app-bundled software HEVC decode is royalty-bearing. (Published rate tables are images → request documents.)
- **⚖️ AC-3/E-AC-3 post-expiry**: obtain a legal opinion on enabling decode (US + key markets) after the 2026-01-30 expiry of US 7,516,064; check non-US family members.
- **⚖️ AV1**: monitor Sisvel pool posture and the Dolby v. Snap litigation; decide later whether any bundled AV1 software decode is justified.
- **⚖️ Dolby branding**: usage rules for "Dolby", "Dolby Digital", "Dolby Atmos" names/logos when only passing bitstreams through.
- **⚖️ DTS**: confirm no open/standard license path for software decode; keep passthrough-only unless licensed.
- **⚖️ ProRes**: confirm Apple's position for platform-only decode; never authorize an FFmpeg ProRes path.
- **⚖️ Fonts**: finalize the bundled font set and verify each license (OFL reserved-name and notice rules).
- **⚖️ libVLC on iOS**: static-linking + LGPL question if libVLC becomes the engine.
- **Monitor (no action yet)**: H.264 last major US patent ~Nov 2027 `[R]`; HEVC pool software categories; VP9 pool quiet; AAC/MP3/AOM statement recency.
- **Process**: assign an owner for the release-time legal checklist (SBOM + notices + source offer) and a recheck cadence (every release; license-sensitive component changes trigger immediate review).
- **[U] items to close**: HEVC Advance rate tables (image-only); Xperi/DTS public terms; Fedora legal review outcome; any Sisvel/AV1 enforcement against software-only distributors (none found).

## Sources (accessed 2026-10-18)

**FFmpeg / FFmpegKit**
- ffmpeg.org/legal.html `[V1]` — LGPL/GPL behavior, 18-point compliance checklist, trademark, patent warnings.
- github.com/FFmpeg/FFmpeg/blob/master/LICENSE.md `[V1]` — GPL-only libraries/files; version3/nonfree classification.
- github.com/arthenica/ffmpeg-kit (+ issues/1099) `[V1]` — retirement notice, archive date, licensing defaults, fork guidance.
- github.com/arthenica/ffmpeg-kit-next `[V1]` — source-only continuation; FFmpeg 9.0.1; LGPL-3.0.
- itpathsolutions.com/ffmpegkit-shutdown-what-to-do-next `[R]` — retirement timeline/reasons (secondary).

**Media3 / Android platform**
- github.com/androidx/media (LICENSE; libraries/decoder_ffmpeg/README.md) `[V1]` — Apache-2.0; audio-only FFmpeg renderer; build requirements; license note.
- developer.android.com/media/platform/supported-formats `[V1]` — platform codec matrix.
- source.android.com/docs/compatibility/cdd `[V2/R]` — Dolby Vision profile treatment.
- developers.google.com/android/guides/opensource `[V1]` — OSS notice requirements and tooling.

**VideoLAN / mpv / libass / fonts**
- videolan.org/vlc/libvlc.html and videolan.org/legal.html `[V1]` — libVLC LGPL-2.1; GPL app; trademarks; patent stance.
- github.com/mpv-player/mpv/blob/master/Copyright `[V1]` — GPL default; `-Dgpl=false`; linked-library warning; file lists.
- github.com/libass/libass (+ releases) `[V1]` — ISC license; 0.17.5 (2026-06-24).
- openfontlicense.org; scripts.sil.org/OFL `[V2/V1]` — OFL terms (notice, reserved names).
- www.vorbis.com unreachable 2026-10-18 (transport error) → used xiph.org/vorbis/ `[V1]`; matroska.org/license/index.html returned 404 → used matroska.org/legal.html `[V1]`.

**Patent pools / litigation**
- via-la.com/licensing-programs/hevc-vvc/ `[V1]` — VCL Advance rates, caps, device scope; Oct 2026 patent list.
- accessadvance.com/2025/12/15/access-advance-and-via-licensing-alliance-announce-hevc-vvc-program-acquisition/ `[V1]`.
- accessadvance.com/licensing-programs/hevc-advance/ + /topic-what-do-we-license/ + /topic-where-and-when-is-a-royalty-due/ `[V1]` — software category; duplicate royalty policy; image-only rate tables `[U]`.
- accessadvance.com/licensing-programs/vdp-pool/ and /vdp-pool-license-scope `[V1]` — streaming-service pool scope.
- via-la.com/licensing-programs/avc/ `[V1]` — AVC manufacture and streaming fees.
- via-la.com/licensing-programs/aac/ `[V1]` — AAC fees, initial fee, Android OEM FAQ, end-user product definition.
- sisvel.com AV1 + VP9 programme pages `[V1]` — rates, portfolio sizes, component exclusion; "half the AV1 market" insight `[V1]`.
- accessadvance.com/2026/03/24/access-advance-licensor-sues-snap-inc-for-av1-and-hevc-patent-infringement `[V1]`; arstechnica.com 2026-03 `[V2]`; ipfray.com 2026 `[V2]`; iam-media.com 2026 `[V2]` — Dolby v. Snap; UPC escalation.
- aomedia.org/about/legal/ `[V1]`; webmproject.org/license/additional/ `[V1]`; webmproject.org/about/faq `[V1]`.
- opus-codec.org/license/ `[V1]`; xiph.org/vorbis/ `[V1]`; xiph.org/flac/license.html `[V1]`; matroska.org/legal.html `[V1]`.
- support.apple.com/en-us/118584 `[V1]`; apple.com/legal/sla/docs/ProResRaw.pdf `[V1]` — ProRes authorization.

**Dolby / DTS / audio codec patents**
- professional.dolby.com/licensing/ `[V1]`; handbook.dolby.com `[V1]` — licensing process, reporting, audits.
- phoronix.com/news/Dolby-Digital-Plus-E-AC3-2026 `[R]` — US 7,516,064 expiry (2026-01-30); non-lawyer caveat.
- blog.mainconcept.com/dolby-digital-audio-in-ffmpeg `[R]` — vendor statement on E-AC-3 patents/royalties (pre-expiry).
- discussion.fedoraproject.org thread 180329 `[R]` — Fedora legal review pending.
- avsforum.com thread 2789001 `[R]` — AC-3 last patent expiry 2017 (SEC/ETSI references).
- en.wikipedia.org/wiki/Dolby_Vision `[V2]` — reported DV license cost figures.
- xperi.com `[U]` — DTS licensing terms not publicly detailed; no permissive grant located.

**Notices / SBOM / stores**
- cisa.gov news 2026-07-29 + cisa.gov/resources-tools/resources/2026-minimum-elements-software-bill-materials-sbom; ic3.gov/CSA/2026/260729.pdf `[V1]` — SBOM v2.1, 17 fields, scope.
- runsafesecurity.com SBOM mapping article `[V2]` — CycloneDX/SPDX field mapping.
- developer.apple.com DPLA download + app-store/review/guidelines/ `[V2]` — third-party notices; developer responsibility.
