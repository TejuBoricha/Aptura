# Android Video Player Competitive Landscape — Research Notes

Research notes for the next-generation Android player spec. Compiled 2026-10-18 from public web sources only.
Nothing here was tested hands-on; no playback, battery, crash-rate, or performance benchmark is inferred from user reviews.

**Evidence labels used:** `[V1]` verified primary (official store/docs/repo/license) · `[V2]` verified secondary (established press/data sites) · `[R]` reported user experience (reviews/forums) · `[I]` inference · `[E]` estimate · `[U]` unverified/unknown.
Conflicts and gaps are flagged explicitly. Store pages were reachable on 2026-10-18 unless noted.

---

## 1. VLC for Android

### 1.1 Versions, cadence, distribution, scale
- Current stable: **3.7.1** (F-Droid added 2026-05-10; official mirror `download.videolan.org/pub/videolan/vlc-android/last/` holds 3.7.1 APKs, files dated 2026-07-14). A **3.7.2-Beta-1** exists (get.videolan.org/testing, 2026-07-10); **no 3.7.2 stable was found** as of access date. [V1] https://f-droid.org/en/packages/org.videolan.vlc/ · https://download.videolan.org/pub/videolan/vlc-android/last/ · https://get.videolan.org/testing/android/3.7.2-Beta-1/
- Prior releases: 3.7.0 on **2026-02-25**, 3.6.0 on **2025-01-13** (VideoLAN news page). Major cadence ≈ annual, with point releases/betas in between; nightly build dirs were still being produced 2026-10-14. [V1] https://www.videolan.org/news.html · https://artifacts.videolan.org/vlc-android/
- Play Store (org.videolan.vlc, publisher "Videolabs"): **4.0★, 2.01M reviews, 500M+ installs (store bucket)**, listed "Updated on May 11, 2026". [V1] https://play.google.com/store/apps/details?id=org.videolan.vlc
- Also on F-Droid (own build), direct APKs, Chromebooks, Android TV. Desktop VLC 3.0.24 (2026-09-22, 130+ security fixes, FFmpeg bump) shows upstream security work continues, but it is a *desktop* release — do not quote it as an Android fix. [V1] https://www.videolan.org/news.html

### 1.2 Documented format/container support
- Store/docs list: "All formats … MKV, MP4, AVI, MOV, Ogg, FLAC, TS, M2TS, WV and AAC"; subtitles embedded and external including **ASS and DVD**; Teletext and Closed Captions. [V1] https://play.google.com/store/apps/details?id=org.videolan.vlc · https://www.videolan.org/vlc/download-android.html
- libVLC README (the engine VLC Android ships): SMB/FTP/SFTP/NFS filesystem browsing, UPnP/DLNA servers, HDR incl. tonemapping to SDR, SPDIF/HDMI passthrough for DD+/TrueHD/DTS-HD, casting to Chromecast/UPnP renderers, hardware decode "up to 8K". [V1] https://github.com/videolan/vlc-android/
- Official wiki feature matrix (documented through app version 3.1 — **older than current app; treat as directional, possibly stale**): UPnP discovery/streaming and FTP since 1.6; Android TV since 1.6; PiP partial in 2.0 then Yes (2.5+); **Chromecast Yes from 3.0**. [V1] https://wiki.videolan.org/Documentation:Android

### 1.3 Network features and reported reliability
- **SMB playback failures on Android** are a recurring class: NAS shares not playing while local files play (r/VLC, Mar 2026 thread; suggested cause: SMB protocol-version mismatch). [R] https://www.reddit.com/r/VLC/comments/1ro2si1/vlc_on_android_not_playing_video_on_smb/
- **External subtitles over SMB are not picked up** (same-folder .srt ignored). Reported multiple times 2025–2026 (r/VLC Feb 2026; r/AndroidTV Jul 2025 where a reply calls it a known limitation). [R] https://www.reddit.com/r/VLC/comments/1quqitx/android_vlc_app_cannot_see_or_use_the_srt_file/ · https://www.reddit.com/r/AndroidTV/comments/1lqs1fq/vlc_subtitles_cant_reach_the_smb_network/
- HLS with external/segmented VTT: subtitles absent while streaming, same file works locally (r/VLC, 2025). [R] https://www.reddit.com/r/VLC/comments/1jos2c9/no_subtitle_on_hls_streaming_vlc_363android/
- Online subtitle download (OpenSubtitles-backed) had an outage in late 2024 acknowledged by a VLC dev due to upstream API changes; a 2026 thread reports a fix landed on beta. [R] https://www.reddit.com/r/VLC/comments/1gq74gt/subtitle_issue_with_vlc_android/ · https://www.reddit.com/r/VLC/comments/1sx11tj/how_can_i_fix_this/
- Streaming instability vs other platforms: Android "freezes, cannot connect, reconnects" on RTMP where iOS/Windows worked (r/VLC, Apr 2025). [R] https://www.reddit.com/r/VLC/comments/1jxx8sz/android_version_of_vlc_is_unreliable/
- UPnP: an open VideoLAN issue documents DLNA servers not found by the Android app on ChromeOS (#27774); a search of recent sources found **no 2025–26 UPnP reliability metric for normal Android devices** [U]. [V1] https://code.videolan.org/videolan/vlc/-/issues/27774
- Casting (Chromecast/UPnP renderers): officially supported [V1, see 1.2]; the 2018 launch note called it beta and explained transcode/CPU/battery cost for unsupported codecs — **dated; recent reliability evidence not found** [U]. https://geoffreymetais.github.io/features/vlc-30/

### 1.4 Subtitles, PiP, TV, permissions, monetization, privacy
- Subtitle features: embedded/external (ASS/DVD), multi-track selection, font customization, audio/subtitle delay; CC and Teletext. [V1] https://wiki.videolan.org/Documentation:Android · https://www.videolan.org/vlc/download-android.html
- **PiP: yes** (native PiP; store also documents an "draw over other apps" permission for the custom PiP widget). Android TV: yes (startup-receiver and mic permissions are TV-only per store text). [V1] https://play.google.com/store/apps/details?id=org.videolan.vlc
- Permissions requested (store listing): storage read/write, full network access, prevent sleep, change audio settings, modify system settings (ringtone), view network connections, draw over other apps, vibrate, run at startup (TV), microphone (TV voice search). [V1] same Play URL.
- Monetization: free; store text says "no ads, no in-app-purchases, no spying" (developer claim, but the listing carries no "Contains ads" tag and no IAP products). Funding is donation/volunteer based. [V1] same Play URL.
- Privacy: Play Data safety declares **no data collected, no data shared** (developer declaration); F-Droid build available for those who prefer it. Absence of evidence of VLC Android ad/tracker SDKs in this pass. [V1] same Play URL.

### 1.5 Recurring user complaints (evidence-sampled)
- Crash/stop at the same point in some videos (Play review, Sep 2026). [R] https://play.google.com/store/apps/details?id=org.videolan.vlc
- Zooming a video makes subtitles disappear; customization too limited (Play review, Jun 2025). [R] same URL.
- Certain TS/MPG files failing on current versions where an older release (3.5.1) worked (r/VLC, Jul 2026). [R] https://www.reddit.com/r/VLC/comments/1uyecsn/subtitles_and_subtrack_in_android/
- Media-library scan problems: library never finishing on a Pixel 8 Pro (permissions + all-files granted), manual browsing works (r/VLC, Dec 2025); forum "VLC does not find all media" (root cause in that case: `.nomedia` files); GitLab issues #2728 "does not load all videos"; SMB favorite indexing slow (#2399). [R] https://www.reddit.com/r/VLC/comments/1pmksfe/vlc_android_not_scanning/ · https://forum.videolan.org/viewtopic.php?f=35&t=160264 · https://code.videolan.org/videolan/vlc-android/-/issues/2728 · https://code.videolan.org/videolan/vlc-android/-/issues/2399
- **UI-clutter complaints: not strongly evidenced in the sources sampled for 2025–26** — leave as [U] rather than assert.

---

## 2. MX Player (global app + Amazon MX Player India)

### 2.1 Status of the offline player (late 2026)
- The Android app is **still shipped and still updated**: Play listing (package `com.mxtech.videoplayer.ad`, developer **Amazon Mobile LLC**, title "MX Player: Video Player & OTT") shows "Updated on Oct 15, 2026"; APKMirror logs version **3.3.1 (2026-10-12)**, 3.3.0 (Oct 5), 3.2.13 (Oct 7), 3.2.5/3.2.4 (Sep 26) — i.e., a fast 3.x train. [V1] https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad · https://www.apkmirror.com/apk/amazon-mobile-llc/mx-player/
- Scale: **1B+ installs (store bucket)**, **14.3M reviews**, **~4.2–4.24★**, "Contains ads", "In-app purchases"; min Android 7.0, target API 36, ~49 permissions, Android TV supported. [V1/V2] same links + https://www.appbrain.com/app/mx-player/com.mxtech.videoplayer.ad
- A second Amazon app, "MX Player Online: OTT & Videos" (`com.mxtech.videoplayer.online`), coexists. [V1] https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad (similar-apps list)

### 2.2 India rebrand / Amazon streaming unification
- Amazon acquired MX Player assets in 2024 and merged its OTT with miniTV into "Amazon MX Player". OTT service for users **outside India was discontinued 2024-08-12**. [V1] https://support.mxplayer.in/support/solutions/folders/43000595307 · [V2] https://www.aftvnews.com/amazon-takes-over-mx-player-app-takes-down-pro-version-and-limits-mx-player-tv-streaming-service-to-india/
- **2026 unification with Prime Video began 2026-05-07** (About Amazon): app refreshed as "MX Player by Prime Video"; iOS app, TV app, web discontinued/redirected to Prime Video; **the Android app remains available** with "Videos" tab, Fatafat, and the Local Video Player; free content stays ad-supported; Prime membership not required for the local player or free catalogue. [V1] https://www.aboutamazon.in/news/company-news/prime-video-mx-player-india-largest-streaming-service · https://support.mxplayer.in/support/solutions/articles/43000788232-will-amazon-mx-player-be-discontinued-where-can-i-watch-amazon-mx-player-content-do-i-need-to-insta · [V2] https://deadline.com/2026/05/prime-video-merge-mx-player-india-1236884030/
- Account migration: third-party (Google/Facebook/Apple/Truecaller) logins deprecated by **2026-07-01**; Amazon-account sign-in required; previously downloaded OTT content not restorable for those users. [V1] https://support.mxplayer.in/support/solutions/folders/43000601956
- Global availability: the *free* app remained on Play "in all regions" per Oct-2024 press; a Japanese comparison site viewed the JP store listing (4.2★, ~14.2M reviews, 1B+ installs) on 2026-08-04, consistent with continued global store presence. **MX Player Pro** (paid, local-only) was pulled from Play in 2024, its store page 404s in JP as of 2026-08-04, while a legacy MX support page still tells non-India users to buy Pro — **conflict; treat Pro as unavailable unless the user's own store shows it**. [V2] https://www.androidpolice.com/mx-player-pro-taken-down-in-us-now-owned-by-amazon/ · https://howtogeek.com/amazon-mx-player-loses-pro-service-outside-india/ · https://saas-hikaku.com/tools/mx-player/ · [V1-declared-but-stale] https://support.mxplayer.in/support/solutions/articles/43000608796-how-can-i-get-rid-of-advertisements-

### 2.3 Feature set (vendor-documented)
- HW+ decoder, multi-core decoding (vendor claim "70% better on multi-core" is marketing — not independently verified [U]), pinch zoom/pan, subtitle gestures, kids lock, privacy folder, video cutter, HD music player, equalizer/bass boost, night mode, **local network SMBv2**, floating window + PiP with custom controls. [V1] https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad
- Subtitle formats: SRT, SSA/ASS (full styling), SMI, SUB, IDX/VobSub, MPL2, TMPlayer, Teletext, PJS, WebVTT, DVD/DVB tracks. [V1] same URL. (This breadth is a genuine differentiator vs Media3-only players.)
- Permissions (store text): All-files access (MANAGE_EXTERNAL_STORAGE) for media/subtitle discovery; optional camera/location/nearby-devices for MX Share file transfer; modify system settings (enable Wi-Fi for P2P); notifications; draw-over-other-apps for PiP; phone (India autofill). [V1] same URL; policy rationale: https://support.mxplayer.in/support/solutions/articles/43000652175

### 2.4 Monetization & ads sentiment
- Ads + IAP in the free app; India plans: **MX Local** (ad-*lite* local playback) and MX Gold (ad-lite local+online) — **Gold/Silver discontinued 2026-04-30, auto-renewal off, benefits ending Jun 8/15, 2026, prorated refunds**. [V1] https://support.mxplayer.in/support/solutions/folders/43000601568 · https://support.mxplayer.in/support/solutions/articles/43000608796-how-can-i-get-rid-of-advertisements-
- Prime Ad-Free add-on does **not** remove ads from free MX content (MX support clarification; secondary analysis agrees). [V2] https://www.itechguides.com/how-to-remove-ads-from-mx-player-official-options-for-android-tv-and-prime-video/
- Recent Play reviews complain about ad density (e.g., "~7 ad breaks in a 40-min episode", repeated TV ads, Sep 2026) and Amazon login persistence bugs (Sep 2026). These are individual reports, not measured averages. [R] https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad

### 2.5 Privacy/security history (sourced)
- 2020: **CVE-2020-5764** path traversal → remote code execution via the MX Share file-transfer hotspot; patched in v1.24.5. [V2] https://medium.com/tenable-techblog/android-mx-player-path-traversal-to-code-execution-9134b623eb34 · https://www.techradar.com/news/vulnerabilities-found-in-indian-video-streaming-app-mx-player
- MX privacy policy documents collection of device/network identifiers (including IMEI, MAC, ad ID), log data, and Google-API-based **installed-application collection** ("store it to https://mxase.clmbtech.com"), plus Google Analytics/Facebook ad SDKs. [V1] https://sites.google.com/site/mxvpen/about/privacy-policy
- Common Sense Privacy evaluation rated MX Player **"Warning"** (3rd-party marketing, personalized ads, data profiles). [V2] https://privacy.commonsense.org/evaluation/MX-Player
- Current Play Data safety label: "may collect: Location, Personal info and 4 others"; "No data shared with third parties"; encrypted in transit; deletion requests supported. [V1] store URL above. Note the tension between "no sharing" and the policy's third-party SDK/marketing language [I].
- Legacy Advanced Protection-era scrutiny (MX Player Pro data questions on XDA, 2021) remains a recurring community theme. [R] https://xdaforums.com/t/mx-player-pro-some-questions-not-in-faqs.4292039/

---

## 3. Just Player (moneytoo/Player)

- Engine: **AndroidX Media3 (formerly ExoPlayer) + ExoPlayer's FFmpeg extension** (audio formats enabled: AC3/EAC3/DTS/DTS-HD/TrueHD etc.). [V1] https://github.com/moneytoo/Player/
- Repo: ~2.6k stars, license **The Unlicense**, active issues page; distribution **Google Play + F-Droid + GitHub APKs** (F-Droid builds signed by F-Droid). Play: ~**1M+ installs** (bucket), ~1.2M cumulative per AppBrain; ratings conflict between sources: **4.0★/3.42K reviews** (Play widget snapshot) vs **3.88/2,561 ratings** (AppBrain) — cite as a range. [V1] https://f-droid.org/en/packages/com.brouken.player/ · [V2] https://www.appbrain.com/app/just-video-player/com.brouken.player
- Update cadence: tracks Media3 releases closely — GitHub v0.217 (2026-09-11, Media3 1.11.1), v0.216 (2026-08-08); Play shows 0.218 on 2026-09-26; F-Droid v0.217 added 2026-09-15. Cadence ≈ monthly. [V1] https://github.com/moneytoo/Player/releases
- Formats (README): containers MP4/MOV/WebM/MKV/Ogg/MPEG-TS/PS/FLV + AVI (🚧 in progress); video H.263/H.264 (Main on 6+)/HEVC/MPEG-4 SP/VP8/VP9/AV1; streaming DASH/HLS/SmoothStreaming/RTSP; subtitles SRT/SSA/ASS (limited styling)/TTML/VTT/DVB. HDR10+ and Dolby Vision on supported hardware; **Dolby Vision profile 7 (UHD-BD) played as HDR HEVC fallback**; tunneled playback for TV; AC-4 when device decoder exists. [V1] same README.
- Explicit non-features (author statements): **no media library/playlists** ("will probably never have … rich media library UI"; "never truly compete with feature rich VLC"); AVI partial; WMV/Theora unsupported upstream; 10-bit AVC unsupported on many devices; audio-only playback not officially supported. [V1] README + https://xdaforums.com/t/app-5-0-just-video-player-no-bluetooth-lag-exoplayer-ffmpeg-audio-codecs.4189183/
- Why users like it [R]: clean single-screen UI, Bluetooth A/V sync, rare audio codecs, PiP without minimizing other apps, DV/HDR on capable phones (AppBrain review snippets Feb–Jun 2026). Complaints [R]: no library view, no subtitle customization/sync UI, only crop/fit aspects, Google-TV episode flow issues (reviews 2026). [R] https://www.appbrain.com/app/just-video-player/com.brouken.player
- Monetization: **none** — free, Unlicense, no ads/IAP; donation links only (F-Droid page: BTC/LTC/PayPal). [V1] F-Droid URL above.

---

## 4. mpv-based Android players

### 4.1 mpv-android (the reference libmpv app)
- Engine: **libmpv** (mpv core; FFmpeg + libass + libplacebo in the build dependency list). Latest build **2026-09-17**; 2026 cadence: Sep 17, Aug 11, Apr 25, Mar 22 (+ Dec 2025, Aug/Jul/Apr 2025) — roughly every 1–2 months, more when upstream breaks. [V1] https://github.com/mpv-android/mpv-android/releases · https://f-droid.org/packages/is.xyz.mpv/
- Distribution: F-Droid (incl. Android TV), GitHub APKs, and **Google Play** (500K+ installs, 4.42K reviews, updated Sep 17, 2026). Release notes add a caveat: the new `allstorage` APK uses MANAGE_EXTERNAL_STORAGE and **"will stay separate and not be shipped to Google Play"**, and **Android TV is available only via F-Droid or manual APK**. [V1] https://play.google.com/store/apps/details?id=is.xyz.mpv · release notes above.
- Feature strengths [V1]: hardware + software decode, full **libass styled subtitles** (the fidelity leader in this comparison), secondary/dual subtitles, advanced render options (interpolation, debanding, scalers), background playback, PiP, keyboard input, Open URL streaming. Repo ~3k stars. [V1] https://github.com/mpv-android/mpv-android/releases (README/F-Droid listing)
- Android limitations (maintainer-sourced): **no Dolby Vision support** (feature request #717 closed as duplicate; DV Profile 5 renders green/purple without a software-decode + `gpu-next` workaround); maintainer (2025): "**Even normal HDR does not seem to work correctly with hwdec**" and there is no hwdec + DV tonemapping combination; open hwdec color-correctness bug #1074; no HDR passthrough — mpv tone-maps. [V1] https://github.com/mpv-android/mpv-android/issues/1081 · https://github.com/mpv-android/mpv-android/issues/1074 · https://github.com/mpv-android/mpv-android/issues/717 · https://github.com/mpv-android/mpv-android/issues/1022
- User sentiment [R]: love for subtitle rendering; complaints about no resume in some file-picking flows, no subtitle font/shadow settings, file browser showing non-media folders (Play reviews 2023–2026). [R] https://play.google.com/store/apps/details?id=is.xyz.mpv

### 4.2 mpvKt (nicer UI over libmpv)
- Jetpack-Compose front-end "based on mpv-android" with nicer player UI, history, sleep timer, speed presets, smoother PiP; Apache-2.0; distributed GitHub/F-Droid/IzzyOnDroid/Play per README. [V1] https://github.com/abdallahmehiz/mpvKt
- **Project health: repository archived by owner on 2026-03-30** ("read-only"); last release **v0.1.6** (GitHub shows "14 Sep" without year in the page render; IzzyOnDroid snapshots date the last app update 2024-09-14 — **date uncertainty**). F-Droid/IzzyOnDroid flag the archived source ("no updates anymore"). Treat as frozen; forks may continue. [V1] https://github.com/abdallahmehiz/mpvKt · [V2] https://apt.izzysoft.de/fdroid/index/apk/live.mehiz.mpvkt
- Implication [I]: no currently-major, actively-maintained "mpv with a nice UI" app; demand is served by mpv-android itself and by dual-engine ideas (e.g., an experiment repo "Just Player Pro" bundling Media3+mpv, GPLv3, created 2026-09-12, 13 stars — too new/small to treat as a competitor). [V1] https://github.com/Zain-Imam/just-player-pro

---

## 5. Other meaningful players

### 5.1 Nova Video Player (FOSS)
- Engine: native core "avos" (fork of Archos Video Player CE) using **FFmpeg + Android MediaCodec** for demux/decode; Apache-2.0; media library and scraper are separate modules. (Note: Just Player's older comparison table lists Nova as "MediaPlayer — ExoPlayer in nova v7"; current repo README says FFmpeg/MediaCodec — **source conflict, current repo wins**.) [V1] https://github.com/nova-video-player/aos-avos · https://github.com/nova-video-player/aos-AVP
- Activity: **very high** — GitHub v6.5.5 (2026-10-09) on the new 6.5 branch; Play publication **6.4.72-20260924.2228** (Sep 24/25, 2026) tracks a 6.4.7x stabilization line (changelog: "in sync of soon to come Google Play publications") [I on branch policy, V1 on dates]. [V1] https://github.com/nova-video-player/aos-AVP/releases
- Scale/rating: Play **1M+ installs bucket** (AppBrain: 4.7M cumulative, ~82K installs/30 days); rating 4.1★/13.5K reviews on Play vs 4.02/8,126 on AppBrain (minor conflict). [V1/V2] https://play.google.com/store/apps/details?id=org.courville.nova · https://www.appbrain.com/app/nova-video-player/org.courville.nova
- Distribution: Play, F-Droid, IzzyOnDroid, Amazon Appstore, GitHub; **donations only, no ads**. [V1] https://nova-video-player.github.io/ (README badges) · AppBrain "No ads".
- Feature set: SMB/UPnP/FTPS/SFTP/WebDAV sources, TMDb artwork/metadata scraping, NFO (Kodi/Jellyfin) support, subtitle download, multi-audio/subs, AC3/DTS passthrough, Android TV leanback UI, Trakt sync, multi-device network resume. [V1] Play listing above.
- Pain evidence: changelogs show a **long tail of A/V-sync, seek, SMB and passthrough fixes** through 2026 (e.g., v6.4.38 "anchor video timing to the audio position"; v6.4.30 SMB jcifs-ng work; v6.4.42 "fix SMB preventing playback"), and Aug 2026 Play reviews report post-update playback/spin/crash regressions on some TVs. [V1/R] changelog + reviews links above.

### 5.2 Kodi (media-center reference — scope differs)
- Not a drop-in file player: full media center (library, scrapers, add-ons, PVR), GPLv2+, HTPC/10-foot UI orientation; treat as an adjacent reference for library/TV UX, not a direct competitor to a file-first player. [V2] https://www.appbrain.com/app/kodi/org.xbmc.kodi
- Versions: stable **21.3 "Omega"** mirror build dated 2025-12-13; **v22 "Piers" RC1 on 2026-10-10** (Beta 2 2026-09-05). Play listing lags (AppBrain last saw 21.2, Jan 2025 — Play flakiness; confirm per device). [V1] https://mirrors.kodi.tv/releases/android/arm64-v8a/ · https://kodi.tv/article/kodi-22-piers-beta-2/
- Relevant to us: v22 beta notes fix **Dolby Vision playback with PGS subtitles on Android** and improve HDR/tonemapping — evidence that DV+subtitle-layer interactions are a real Android pain class. [V1] https://kodi.tv/article/kodi-22-piers-beta-2/
- Scale: Play ~73M installs cumulative, 3.55★/263K ratings (AppBrain, data updated 2026-10-15); user reviews on Play include startup black-screen complaints on recent Samsung/Pixel devices (2026). [V2/R] AppBrain URL above.

### 5.3 KMPlayer (com.kmplayer, PANDORA.TV)
- Scale: Play **10M+ installs**, 385K reviews, IAP (KMPlayer VIP), updated 2026-10-15; store data safety: no data collected/shared declared. [V1] https://play.google.com/store/apps/details?id=com.kmplayer
- Versioning: apk20 logs **36.10.140 (2026-10-14)**; official blog posts 36.x updates Apr 2026; Uptodown lists "46.04.235 (2026-04-24)" — **numbering conflict across trackers, likely scraping artifact**. [V1/V2]
- Features: Chromecast, network (FTP/UPnP/SMB/WebDAV), cloud (Dropbox/OneDrive), section repeat, color controls, subtitle styling, VIP: torrent streaming, video cut, GIF, MP3 extraction, themes. [V1] Play listing above.
- Pain evidence [R]: library discovery with very large folder trees ("hours … app closes itself and restarts looking"), crashes during discovery, torrent/magnet failures (Play reviews Jul–Aug 2026). Vendor replies are canned support replies — note before citing. [R] same Play URL.

### 5.4 "PlayerX" and others without real user bases
- Multiple unrelated apps share the name "PlayerX". The most-listed has **500+ downloads** (ads) [V1] https://play.google.com/store/apps/details?id=com.plyer.playerx; another (missuteam) appears dormant since 2025 with ~600K cumulative APK-site downloads [V2] https://www.uptodown.com/android (playerx). **Do not treat "PlayerX" as a competitor.**
- Worth one line each on the radar: **Next Player** (FOSS, Media3, 100K+ installs, 4.3★) [V2 AppBrain alternatives]; **Vimu Media Player for TV** (paid TV player, 100K+) [V2]; **Fermata Media Player** (multi-engine, niche) [U]. Excluded from the table for lack of current verified scale.

---

## 6. Synthesis A — baseline/commodity vs plausible differentiators

| Capability | Commodity baseline (≥3 of 6 majors ship it) | Evidence | Plausible differentiator if materially better |
|---|---|---|---|
| MKV/MP4/H.264/H.265/VP9/AV1 playback | All (VLC, MX, Just, mpv, Nova, KMPlayer) | [V1] vendor listings §1–5 | Demonstrably fewer "can't play" cases on edge files (VC-1, 10-bit AVC, TS/MPG) |
| SRT/ASS subtitle rendering, track select | All | [V1] §1–5 | Full libass fidelity (only mpv-class apps) + subtitle UI polish; mpv is the bar |
| Gestures, speed control, PiP, audio-track select | All | [V1] §1–5 | Reliability of resume/seek under long sessions (weak across the field) |
| SMB/network share playback | VLC, MX, Nova, KMPlayer (mpv via URL/script; Just: none) | [V1] listings | **SMB + same-folder sidecar subtitles that actually work** — repeatedly broken on VLC (see §1.3) |
| Library view + metadata scraping | VLC, Nova, Kodi, KMPlayer, MX | [V1] | Scan speed/accuracy on Android 11+ scoped storage is a known weak spot (VLC, KMPlayer complaints) |
| Android TV / Leanback app | VLC, MX, Just, mpv (F-Droid only), Nova, KMPlayer | [V1] | TV-first episode flow (Just Player TV complaints) |
| Casting (Chromecast/UPnP/DLNA) | VLC, KMPlayer (Chromecast); Nova (UPnP) | [V1] | Reliable first-party casting without Play Services is rare; VLC's is self-built |
| Codec breadth incl. DTS/TrueHD/AC-4 | Just (FFmpeg ext.), VLC, mpv, Nova | [V1] | Passthrough correctness (DTS:X/Atmos sync is an active bug area) |
| No ads / no trackers | VLC, Just, mpv, Nova | [V1] | Verifiable data-safety posture is a marketing differentiator vs MX |
| HDR/DV playback | "On supported hardware" (Just, MX, VLC) — accuracy varies by engine | [V1] | Correct DV P7 fallback + HDR tonemapping that doesn't tint or drop frames (mpv fails here per maintainer) |
| Music/audio extra modes | VLC, MX, KMPlayer | [V1] | Out of scope for a video-first product; don't compete here |

Key reading: only **network-share reliability, subtitle-system quality, HDR/DV correctness, and trust/privacy** are differentiators with evidence of incumbent weakness. Everything else is table stakes.

## 7. Synthesis B — pain-point ranking (frequency · severity · evidence strength)

| # | Pain point | Frequency (field-wide) | Severity | Evidence strength | Notes/sources |
|---|---|---|---|---|---|
| 1 | SMB/NAS playback failures & SMB subtitle blind spots | Medium-high | High | **Strong** ([R] multi-forum + [V1] open issues) | VLC §1.3; Nova SMB fix logs §5.1; generic SMB-protocol mismatch advice |
| 2 | Library scanning/metadata: slow, resets, misses files, crashes on huge trees | High | Medium-high | **Strong** ([R]+[V1] issues/reviews) | VLC §1.5; KMPlayer §5.3; Nova post-update regressions §5.1 |
| 3 | HDR/Dolby Vision: tinting, no passthrough, hwdec color bugs, DV+PGS conflicts | Medium (concentrated in enthusiast audience) | High on affected devices | Medium-strong ([V1] maintainer statements, release notes) | mpv §4.1; Kodi v22 fix §5.2; Just DV P7 fallback §3 |
| 4 | Ads/monetization intrusiveness (esp. MX) | High in MX base | Medium-high (churn driver) | **Strong** ([R] many + [V1] plan changes) | MX §2.4 |
| 5 | Subtitle customization/behavior gaps (zoom hides subs, no styling in ExoPlayer players, sync UI) | High | Medium | Strong ([R]) | VLC §1.5; Just §3; mpv §4.1 |
| 6 | Stability regressions after updates; resume/position bugs | Medium-high | High (uninstall trigger) | Medium-strong ([R] + [V1] changelogs show repeated regressions) | Nova §5.1; VLC crash review §1.5; MX login bug §2.4 |
| 7 | Streaming protocol edge cases (HLS subs, RTMP reconnects, TLS delays) | Medium | Medium | Medium ([R]; some [V1] fixes) | VLC §1.3; mpv release-note TLS fix §4.1 |
| 8 | A/V sync work (Bluetooth, passthrough, speed changes, long uptime) | Medium | Medium-high | Strong ([V1] changelogs + [R]) | Just's founding rationale §3; Nova changelogs §5.1 |
| 9 | Privacy distrust & historical vulnerabilities (MX) | Medium | High for privacy-sensitive users | Strong ([V2] CVE/evaluation + [V1] policy) | MX §2.5 |
| 10 | TV/remote UX flows (episode-next, folder memory, D-pad) | Medium | Medium | Medium ([R]) | Just Player TV reviews §3; Nova TV fixes §5.1 |

Caveat: frequencies are **qualitative counts of the sampled 2025–26 sources**, not measured market incidence [E]. No crash-rate or battery claims are made anywhere.

## 8. Synthesis C — competitive claims to AVOID until independently validated

1. "Plays everything / all formats" — every incumbent says this; upstream ExoPlayer lacks AVI (partial in Just), WMV, Theora, and 10-bit AVC coverage varies by device [V1 §3]; VLC docs themselves define format support at the engine level, not per-file guarantees.
2. "Full Dolby Vision support" — DV support is profile- and hardware-dependent even in the best apps (DV P7 = HDR fallback in Just; not supported in mpv; Kodi just fixed DV+PGS interactions) [V1 §3, §4.1, §5.2].
3. "No battery drain / longest battery life" — no credible source found; don't infer from reviews [U].
4. "Zero crashes / most stable" — incumbents' own changelogs and store reviews show regressions after updates [V1/R §5.1]; only claim after internal soak tests.
5. "Best subtitle rendering" — libass/mpv-class rendering is the reference; VLC and MX also have bulk-format support; must be benchmarked per-script (complex ASS, typesetting) before claiming.
6. "Chromecast/UPnP casting that just works" — VLC's own launch note called its Chromecast support beta with transcode/battery caveats [V2, dated]; no independent reliability data found [U].
7. "8K playback" — appears as KMPlayer/MX marketing; treat as device-dependent [V1 marketing].
8. "Library scanning is instant" — the opposite is an evidenced field weakness [R §1.5, §5.3]; only claim measured scan times on named device classes.
9. "Zero data collection" — VLC/Just/Nova can say this with store declarations [V1]; a new product should earn it with build transparency (F-Droid/reproducible builds) before using it as a slogan.
10. "#1 video player" style rank claims — Play rankings fluctuate by country/category and buckets are coarse [V1]; avoid.

---

## 9. Open questions

1. VLC Android 4.x/rewrite status: nightlies continue but no public roadmap evidence found; is a UI/engine overhaul imminent? [U]
2. What share of MX Player's 1B+ install base still uses the **local** player vs the OTT tabs? No public split found. [U]
3. Is MX Player Pro genuinely re-purchasable anywhere outside India in late 2026? Official support page (stale?) says yes; store checks say no. [conflict]
4. Which exact codecs/containers fail most in the field (VC-1, 10-bit AVC, TS/MPG, DV P7, AV1 10-bit)? Only anecdotal evidence found; needs a structured test matrix. [U]
5. True reliability of VLC casting (Chromecast/UPnP renderers) on 2025–26 devices: no recent data found. [U]
6. Just Player rating discrepancy (Play 4.0 vs AppBrain 3.88) — which vintage is current? Minor, but fix before quoting.
7. mpv-android Play vs F-Droid functional deltas (allstorage vs scoped-storage build; TV availability) — verify on real devices.
8. Nova's Play (6.4.7x) vs GitHub (6.5.x) feature/bugfix gap — how long until Play catches up?
9. KMPlayer's true active-install base vs the 10M bucket; its library-discovery failures deserve a fresh review sample in 2026 Q4.
10. Audio-only and music features: do users actually want them in a video-first app, or is that MX/VLC clutter? Needs user research, not competitor copying. [U]

---

## Sources (accessed 2026-10-18)

Google Play store pages:
- VLC for Android — https://play.google.com/store/apps/details?id=org.videolan.vlc
- MX Player — https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad
- Just (Video) Player — https://play.google.com/store/apps/details?id=com.brouken.player
- mpv-android — https://play.google.com/store/apps/details?id=is.xyz.mpv
- NOVA Video Player — https://play.google.com/store/apps/details?id=org.courville.nova
- KMPlayer — https://play.google.com/store/apps/details?id=com.kmplayer
- PlayerX — https://play.google.com/store/apps/details?id=com.plyer.playerx

Official/vendor docs, repos, releases:
- VideoLAN news — https://www.videolan.org/news.html; VLC Android download page — https://www.videolan.org/vlc/download-android.html
- VLC F-Droid — https://f-droid.org/en/packages/org.videolan.vlc/; VLC Android mirrors/last — https://download.videolan.org/pub/videolan/vlc-android/last/; betas — https://get.videolan.org/testing/android/3.7.2-Beta-1/; artifacts — https://artifacts.videolan.org/vlc-android/
- VLC wiki Android features — https://wiki.videolan.org/Documentation:Android; vlc-android repo — https://github.com/videolan/vlc-android/
- VideoLAN GitLab issues — https://code.videolan.org/videolan/vlc-android/-/issues/2728 · /-issues/2399 · vlc/-/issues/27774; forums — https://forum.videolan.org/viewtopic.php?f=35&t=160264
- VLC 3.0 Android dev blog (Chromecast beta) — https://geoffreymetais.github.io/features/vlc-30/
- MX Player support: unification — https://support.mxplayer.in/support/solutions/articles/43000788232-… · membership plans — https://support.mxplayer.in/support/solutions/folders/43000601568 · ads — https://support.mxplayer.in/support/solutions/articles/43000608796-… · outside-India OTT notice — https://support.mxplayer.in/support/solutions/folders/43000595307 · permissions FAQ — https://support.mxplayer.in/support/solutions/articles/43000652175-…
- MX Player privacy policy — https://sites.google.com/site/mxvpen/about/privacy-policy; Pro EU policy — https://sites.google.com/site/mxvpen/about/mx-pro-eu-privacy-policy
- APKMirror MX Player — https://www.apkmirror.com/apk/amazon-mobile-llc/mx-player/
- Just Player repo/releases — https://github.com/moneytoo/Player/ · /releases; F-Droid — https://f-droid.org/en/packages/com.brouken.player/
- mpv-android releases — https://github.com/mpv-android/mpv-android/releases; issues — #717, #1022, #1074, #1081 (same repo); F-Droid — https://f-droid.org/packages/is.xyz.mpv/
- mpvKt — https://github.com/abdallahmehiz/mpvKt (archived 2026-03-30); release v0.1.6 — /releases/tag/v0.1.6; IzzyOnDroid — https://apt.izzysoft.de/fdroid/index/apk/live.mehiz.mpvkt
- Nova — https://github.com/nova-video-player/aos-AVP · releases; avos core — https://github.com/nova-video-player/aos-avos; FAQ — https://home.courville.org/nova_video_player-faq/
- Kodi — https://kodi.tv/article/kodi-22-piers-beta-2/ · https://mirrors.kodi.tv/releases/android/arm64-v8a/
- KMPlayer blog — https://kmplayerofficial.blogspot.com/2026/

Press and data sites:
- Deadline (Prime–MX merger) — https://deadline.com/2026/05/prime-video-merge-mx-player-india-1236884030/
- About Amazon India (unification) — https://www.aboutamazon.in/news/company-news/prime-video-mx-player-india-largest-streaming-service
- AFTVnews — https://www.aftvnews.com/amazon-takes-over-mx-player-app-takes-down-pro-version-and-limits-mx-player-tv-streaming-service-to-india/; HowToGeek — https://www.howtogeek.com/amazon-mx-player-loses-pro-service-outside-india/; Android Police — https://www.androidpolice.com/mx-player-pro-taken-down-in-us-now-owned-by-amazon/
- Tenable blog (CVE-2020-5764) — https://medium.com/tenable-techblog/android-mx-player-path-traversal-to-code-execution-9134b623eb34; TechRadar — https://www.techradar.com/news/vulnerabilities-found-in-indian-video-streaming-app-mx-player; Common Sense Privacy — https://privacy.commonsense.org/evaluation/MX-Player
- AppBrain — Just Player — https://www.appbrain.com/app/just-video-player/com.brouken.player; Nova — https://www.appbrain.com/app/nova-video-player/org.courville.nova; MX — https://www.appbrain.com/app/mx-player/com.mxtech.videoplayer.ad; Kodi — https://www.appbrain.com/app/kodi/org.xbmc.kodi
- AppstoreSpy (VLC stats) — https://appstorespy.com/android-google-play/org.videolan.vlc-trends-revenue-statistics-downloads-ratings
- iTechGuides (MX ad options 2026) — https://www.itechguides.com/how-to-remove-ads-from-mx-player-official-options-for-android-tv-and-prime-video/

User-experience threads (Reddit/XDA):
- r/VLC SMB failure — https://www.reddit.com/r/VLC/comments/1ro2si1/vlc_on_android_not_playing_video_on_smb/
- r/VLC SMB subtitle blind spot — https://www.reddit.com/r/VLC/comments/1quqitx/android_vlc_app_cannot_see_or_use_the_srt_file/; r/AndroidTV — https://www.reddit.com/r/AndroidTV/comments/1lqs1fq/vlc_subtitles_cant_reach_the_smb_network/
- r/VLC HLS subtitles — https://www.reddit.com/r/VLC/comments/1jos2c9/no_subtitle_on_hls_streaming_vlc_363android/; subtitle service issue — https://www.reddit.com/r/VLC/comments/1gq74gt/subtitle_issue_with_vlc_android/; TS/MPG regression — https://www.reddit.com/r/VLC/comments/1uyecsn/subtitles_and_subtrack_in_android/; RTMP instability — https://www.reddit.com/r/VLC/comments/1jxx8sz/android_version_of_vlc_is_unreliable/; scan issue — https://www.reddit.com/r/VLC/comments/1pmksfe/vlc_android_not_scanning/
- XDA Just Player thread — https://xdaforums.com/t/app-5-0-just-video-player-no-bluetooth-lag-exoplayer-ffmpeg-audio-codecs.4189183/; XDA MX Pro removal — https://xdaforums.com/t/mx-player-pro-no-longer-on-google-play.4692396/; XDA MX Pro data questions — https://xdaforums.com/t/mx-player-pro-some-questions-not-in-faqs.4292039/
- r/mpv HDR on Android — https://www.reddit.com/r/mpv/comments/1fmbplk/hdr_on_android/
- saas-hikaku MX store check (JP, 2026-08-04) — https://saas-hikaku.com/tools/mx-player/
