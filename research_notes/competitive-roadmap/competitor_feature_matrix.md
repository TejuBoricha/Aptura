# Competitive Feature Matrix: Android Local/Offline-First Video Players (2025–2026)

Legend: n/s = "not surfaced" in the cited source (absence of evidence, not proof of absence). R1 boundary = Aptura's deliberate exclusions: no network/INTERNET permission, no casting, no TV, no AI, no equalizer, no online subtitle download. **[IN]** = inside R1 boundary (must-have candidate); **[OUT]** = outside boundary (do not treat as must-have).

## Key Question 1: Per-app feature matrix — library, player, subtitles, ecosystem, settings, monetization

### Takeaway
The category has split into three archetypes: (a) ad-funded "super-apps" (Amazon MX Player, PLAYit, KMPlayer, Mi Video) that bolt OTT streaming, downloaders, AI tools and game halls onto the player; (b) clean FOSS players (VLC, Next Player, Just Player, Nova, mpv-android/mpvKt) whose differentiation is trust + engine depth; and (c) OEM stock players, which are retreating (Samsung removed its native player app; Xiaomi's is a downloader/streamer). Every claim below is tied to a source URL.

### Cited Findings

**Overview / scale / monetization**

| App | Dev · Installs · Updated | Engine | Monetization | Platform notes |
|---|---|---|---|---|
| MX Player (free) | Amazon Mobile LLC · 1B+ · Oct 15 2026 | Proprietary, "70% decoding boost" | Ads + IAP; OTT (MX Fatafat micro-dramas, 200k+ hrs) | Now an AVOD OTT brand ("Amazon MX Player") — [Play listing](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) |
| MX Player Pro | n/a | — | One-time paid tier **removed from Play** when Amazon took over; TV streaming limited to India — [r/Android](https://www.reddit.com/r/Android/comments/1gf1psl/amazon_takes_over_mx_player_app_takes_down_pro/) | Acquisition ~$100M — [THR](https://www.hollywoodreporter.com/business/business-news/apos-amazon-streaming-india-prime-video-and-mx-player-1236300136/); brand folded into Prime Video India ad reach — [ppc.land](https://ppc.land/amazon-kills-mx-player-brand-folding-it-into-prime-video-for-india-ad-reach/) |
| VLC for Android | Videolabs · 500M+ · Oct 14 2026 | libVLC | Free, no ads/IAP/"no spying"; declares **no data collected** — [Play listing](https://play.google.com/store/apps/details?id=org.videolan.vlc) | Phone, tablet, Android TV (recs + voice search) |
| Next Player | anilbeesetti · 4.6k★ GPL-3.0 | Media3/ExoPlayer + FFmpeg ext | Free, no ads, "no excessive permissions" — [GitHub](https://github.com/anilbeesetti/nextplayer) | Play, F-Droid, IzzyOnDroid; Android TV build; runtime decoder switching — [DECODER_SWITCHING.md](https://github.com/anilbeesetti/nextplayer/blob/main/feature/player/DECODER_SWITCHING.md) |
| Nova Video Player | Courville Software · 1M+ · Sep 25 2026 | MediaPlayer + ffmpeg 9.0.1 | Free, FOSS, no ads; declares no data collected — [Play listing](https://play.google.com/store/apps/details?id=org.courville.nova) | Phone/tablet + dedicated leanback Android TV UI; repo: [aos-AVP](https://github.com/nova-video-player/aos-AVP) |
| Just (Video) Player | moneytoo · 2.7k★ Unlicense | Media3/ExoPlayer + FFmpeg ext | Free + donations (PayPal/BTC/LTC); no ads/tracking — [GitHub](https://github.com/moneytoo/Player) | Android 6+, Android TV; on Play, F-Droid, Accrescent, **Galaxy Store, Mi App Mall, Amazon** |
| KMPlayer | Pandora/KMPlayer | Proprietary | Ads + Premium ("Magnet client" torrent streaming, GIF toast) — [kmplayer.com](https://www.kmplayer.com/home) | Android/iOS; Chromecast + URL play — [Uptodown](https://kmplayer.en.uptodown.com/android); URL/YouTube download — [official blog 2025](https://kmplayerofficial.blogspot.com/2025/) |
| PLAYit | PLAYIT Technology PTE (Singapore) · 500M+ · Oct 12 2026 | Proprietary, SW decoding emphasis | Ads + IAP (VIP/coins to remove ads — [dev reply](https://play.google.com/store/apps/details?id=com.playit.videoplayer)) | Player + downloader + editor + **game hall** — [Play listing](https://play.google.com/store/apps/details?id=com.playit.videoplayer) |
| mpv-android | mpv-android org · 3.6k★ MIT | libmpv | Free, FOSS — [GitHub](https://github.com/mpv-android/mpv-android) | TV only via F-Droid/sideload; minimal UI, no media library |
| mpvKt | abdallahmehiz · libmpv | libmpv | Free, FOSS; "nicer UI over mpv-android" — [GitHub](https://github.com/abdallahmehiz/mpvKt) | On Play (live.mehiz.mpvkt) + IzzyOnDroid — [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/live.mehiz.mpvkt) |
| Infuse (Firecore) | Firecore | — | Apple freemium | **No Android/Android TV version exists** (iPhone/iPad/Apple TV/Mac/Vision only) — [firecore.com/infuse](https://firecore.com/infuse), confirmed 2026 — [cinaura](https://www.cinaura.tv/infuse-for-android-tv); the Play Store "com.infuse.infuse" app is an unrelated impostor — [Play](https://play.google.com/store/apps/details?id=com.infuse.infuse&hl=en_US) |
| OEM: Samsung / Xiaomi | Samsung / Xiaomi | Gallery-integrated | Ad-supported (Xiaomi) | Samsung **removed its native video player app** (S25 US) — [Samsung community](https://us.community.samsung.com/t5/Galaxy-S25/Disappointed-the-Samsung-video-player-app-was-removed/td-p/3251786); video editing (speed/crop/rotate, Instant slow-mo) lives in Gallery — [Yahoo Tech](https://tech.yahoo.com/phones/articles/hidden-galaxy-s25-feature-makes-164505954.html); Mi Video = "local video player **and downloader**… play, download, stream, manage" — [Play listing](https://play.google.com/store/apps/details?id=com.miui.videoplayer&hl=en_US) |

**Library features**

| Feature | MX | VLC | Next | Nova | Just | KM | PLAYit | mpv |
|---|---|---|---|---|---|---|---|---|
| Folder browsing view | scan all storage incl. non-media-scanner files [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | media library + "browse folders directly" [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | tree/folder/file view modes [GitHub](https://github.com/anilbeesetti/nextplayer) | folder browsing + movies by name/genre/year/duration/rating + TV by seasons [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | none — system file chooser (Videos / full browser modes) [GitHub](https://github.com/moneytoo/Player) | n/s | auto-detect + sort/organize + storage meter [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | none |
| Multi-select delete/rename/share | delete/share implied; rename n/s | multi-file deletion re-added Oct 2026 (had been dropped) [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | n/s | n/s | post-playback delete [GitHub](https://github.com/moneytoo/Player) | n/s | manage & share [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | n/s |
| Hidden-folder exclusion | n/s | n/s | n/s | n/s | n/s | n/s | n/s | n/s |
| Private/PIN folder | n/s in current listing | n/s | — | private mode = suppress history (not PIN) [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | — | n/s | "Protect Videos in a Private Folder" [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | — |
| Playlists | music player [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | audio player DB | — | TV "Lists" pages (add to list) — [releases](https://github.com/nova-video-player/aos-AVP/releases) | — | n/s | audio playlists [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | n/s |
| History / continue-watching | n/s (OTT-driven) | n/s | n/s | recently played + resume + multi-device network resume [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | n/s | n/s | n/s | n/s |
| Favorites | n/s | n/s | — | — | — | n/s | n/s | — |
| Duplicate detection | — none of the 9 sources mention it — | — | — | — | — | — | — | — |
| SD card / USB-OTG | all-files incl. external storage [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | SD cards [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | via SAF [GitHub](https://github.com/anilbeesetti/nextplayer) | external USB storage [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | via SAF [GitHub](https://github.com/moneytoo/Player) | n/s | SD cards [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | n/s |
| Network SMB/FTP/cloud | SMBv2 [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | network shares/drives/streams/DVD ISO [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | SMB/FTP/SFTP/WebDAV [GitHub](https://github.com/anilbeesetti/nextplayer) | FTP/SFTP/WebDAV/SMB/UPnP + scheduled rescan [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | none built-in — recommends third-party document providers; integrates Nova as library [GitHub](https://github.com/moneytoo/Player) | Chromecast/URL [Uptodown](https://kmplayer.en.uptodown.com/android) | n/s (cloud in privacy note) | Open URL only [GitHub](https://github.com/mpv-android/mpv-android) |
| Online metadata scraping | n/a (own OTT catalog) | n/s | planned online subs only [GitHub](https://github.com/anilbeesetti/nextplayer) | auto poster/backdrop/description scraping + NFO [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | — | — | — | — |

**Player features**

| Feature | MX | VLC | Next | Nova | Just | PLAYit | mpv |
|---|---|---|---|---|---|---|---|
| Decoder HW/SW override | multi-core boost; force-SW troubleshooting note [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | n/s | SW decoders for h264/HEVC + runtime switching [GitHub](https://github.com/anilbeesetti/nextplayer) | "force software decoding in preferences" [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | decoder priority setting [GitHub](https://github.com/moneytoo/Player) | "advanced software decoding" [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | HW + SW [GitHub](https://github.com/mpv-android/mpv-android) |
| Gestures | volume/brightness/zoom/pan/FF-RW/speed/subtitle-seek/subtitle-resize [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | volume, brightness, seek gestures [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | vertical swipe brightness/volume, horizontal seek, zoom [GitHub](https://github.com/anilbeesetti/nextplayer) | n/s | swipe/double-tap seek, pinch zoom [GitHub](https://github.com/moneytoo/Player) | speed/brightness/volume gestures [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | gestures [GitHub](https://github.com/mpv-android/mpv-android) |
| Aspect/zoom presets | zoom & pan gestures [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | aspect-ratio adjustments [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | zoom gesture [GitHub](https://github.com/anilbeesetti/nextplayer) | n/s | Resize fit/crop + zoom mode (TV: keys) [GitHub](https://github.com/moneytoo/Player) | n/s | n/s |
| Speed (custom/pitch) | speed gesture [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | n/s | speed control [GitHub](https://github.com/anilbeesetti/nextplayer) | n/s | speed control [GitHub](https://github.com/moneytoo/Player) | gesture speed [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | mpv speed [GitHub](https://github.com/mpv-android/mpv-android) |
| EQ / bass / night mode | **Equalizer/Bass Booster + Night Mode** [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | equalizer + filters (EQ fixes in Oct 2026 changelog) [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | — | Audio Boost + Night Mode (dynamic volume) [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | deliberately none; supports 3rd-party equalizers (Wavelet) [GitHub](https://github.com/moneytoo/Player) | n/s | missing (community complaint) — [mpv discussion](https://github.com/mpv-player/mpv/discussions/14257) |
| Frame-rate / refresh sync | n/s | n/s | n/s | n/s | **auto frame-rate matching** + seamless refresh-rate switching A11+ + tunneled playback for 4K/HDR TV [GitHub](https://github.com/moneytoo/Player) | n/s | n/s |
| HDR | 8K/4K/HD claim [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | n/s | n/s | DV decoding criticized as weak — [issue #1651](https://github.com/nova-video-player/aos-AVP/issues/1651) | HDR10+ + Dolby Vision (incl. profile 7 as HDR HEVC) [GitHub](https://github.com/moneytoo/Player) | 4K claim [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | n/s |
| PiP / floating window | fully customizable floating window + PiP [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | "draw over other apps" popup widget [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | PiP [GitHub](https://github.com/anilbeesetti/nextplayer) | n/s | PiP A8+, resizable A11+, auto-PiP [GitHub](https://github.com/moneytoo/Player) | floating + background play [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | PiP + background [GitHub](https://github.com/mpv-android/mpv-android) |
| Background audio-only play | music player [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | full audio player [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | background playback [GitHub](https://github.com/anilbeesetti/nextplayer) | n/s | **not supported** ("audio only playback isn't officially supported") [GitHub](https://github.com/moneytoo/Player) | background play [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | background playback [GitHub](https://github.com/mpv-android/mpv-android) |
| Sleep timer / A-B loop / screenshot / bookmarks | n/s in listing | n/s | n/s | n/s | n/s | n/s | no A-B repeat UI — [mpv issue #7331](https://github.com/mpv-player/mpv/issues/7331) |
| Rotation modes | n/s | auto-rotation [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | n/s | n/s | Android 16 blocks programmatic orientation switching; per-app aspect-ratio workaround documented [GitHub](https://github.com/moneytoo/Player) | n/s | n/s |
| Extras | video cutter/trim, MP3 converter, MX Share (camera+location), mic voice search, Bluetooth AV-sync latency [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | remote-access web UI (OTP), teletext/CC [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | — | 3D SBS/TB playback, AC3/DTS passthrough (3 modes — [FAQ](https://home.courville.org/nova_video_player-faq/index.html)), intro/outro auto-skip — [releases](https://github.com/nova-video-player/aos-AVP/releases), Trakt sync, provisional end-time OSD | touch lock, pause on headphone disconnect, skip silence, repeat, post-playback actions [GitHub](https://github.com/moneytoo/Player) | video-to-audio convert, AI effects, game hall, device-to-device transfer [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | dual subtitles, libass styling, scalers/debanding/interpolation, keyboard input [GitHub](https://github.com/mpv-android/mpv-android) |

**Subtitles & ecosystem**

| App | External subs | Online subs | Styling/delay | Ecosystem |
|---|---|---|---|---|
| MX | 11 formats: SRT, SSA/ASS, VobSub, TXT, WebVTT, SAMI, MicroDVD, SubViewer2, DVD/DVB, MPL2, Teletext [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | n/s in listing | gesture resize/reposition of subs [Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | floating/PiP; OTT |
| VLC | multi-track subs, teletext, CC [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | n/s in listing | subtitle size options added [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) | widget, TV recommendations + voice search, remote-access web player with OTP [Play](https://play.google.com/store/apps/details?id=org.videolan.vlc) |
| Next | external subs via SAF [GitHub](https://github.com/anilbeesetti/nextplayer) | **only planned, not shipped** [GitHub](https://github.com/anilbeesetti/nextplayer) | subtitle delay [GitHub](https://github.com/anilbeesetti/nextplayer); SSA/ASS styling limited (ExoPlayer limitation) [issue](https://github.com/google/ExoPlayer/issues/8435) | Android TV build; Material 3; search [GitHub](https://github.com/anilbeesetti/nextplayer) |
| Nova | SRT/SUB/ASS/SMI [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | **integrated subtitle download** [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | manual subtitle sync + manual A/V sync [Play](https://play.google.com/store/apps/details?id=org.courville.nova) | leanback UI, Trakt, NFO, SMB/UPnP, multi-device resume [Play](https://play.google.com/store/apps/details?id=org.courville.nova) |
| Just | long-press file open via SAF; TV workaround via file managers; same-name auto-detect over HTTP [GitHub](https://github.com/moneytoo/Player) | — | styling delegated to system Caption preferences (long-tap sub button) [GitHub](https://github.com/moneytoo/Player) | Android TV; app shortcut (A7.1+); handles streaming links via intent/share; no URL-entry UI [GitHub](https://github.com/moneytoo/Player) |
| PLAYit | n/s | online subtitle search (EN/HI/Tamil…) [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | n/s | floating/background; game hall; transfer [Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer) |
| KM | subtitle files [Play](https://play.google.com/store/apps/details?id=com.kmplayer&hl=en_US) | n/s | bookmarks + quick button + speed [vdocipher 2026](https://www.vdocipher.com/blog/top-video-player-for-android/) | Chromecast, URL play [Uptodown](https://kmplayer.en.uptodown.com/android), magnet torrent [kmplayer.com](https://www.kmplayer.com/home) |
| mpv-android | libass styled subs [GitHub](https://github.com/mpv-android/mpv-android) | — | dual/secondary subtitles; advanced render settings [GitHub](https://github.com/mpv-android/mpv-android) | keyboard input; TV via F-Droid only [GitHub](https://github.com/mpv-android/mpv-android) |

### Inferences
- No source for any reviewed player documents folder **counts**, hidden-folder **exclusion lists**, or **duplicate detection** — these appear to be genuine whitespace inside the R1 boundary.
- The FOSS cohort (Next, Just, mpv) proves a player can ship everything Table-stakes with zero ads; Just Player's FAQ explicitly says it will "never truly compete with feature rich VLC" and defers library duty to Nova — meaning a **privacy-clean player with a real managed library** (Aptura's implied position) is thinly occupied ([GitHub](https://github.com/moneytoo/Player)).
- Next Player still lists online-subtitle download as *planned* ([GitHub](https://github.com/anilbeesetti/nextplayer)) — so R1's no-online-subs exclusion is currently shared only with Just/mpv, not with VLC/Nova/MX/PLAYit; it is a defensible, not suicidal, boundary.

### Gaps
- Exact per-video three-dot menu contents (rename/move/private-folder/multi-select flows) and sleep-timer/screenshot/A-B-loop presence in MX/VLC/KM/PLAYit: not verifiable from listings; Accessible Android app reviews did not surface in search results.
- KMPlayer Play listing detail and Mi Video full feature list not fetched; VLC online-subtitle download current status (wiki.videolan.org fetch returned 404) unverified.
- Infuse pricing tiers and any Firecore Android roadmap statement beyond community speculation unverified.

## Key Question 2: Table stakes for a 2026 Android local video player

### Takeaway
14 capabilities are now near-universal across the reviewed cohort; all fall **inside** Aptura's R1 boundary except TV/casting/network sources — meaning R1 excludes the *distribution surfaces* of table stakes, not the core playback+library set.

### Cited Findings — table stakes (feature → evidenced in ≥4 players)
1. All-format/HW-accelerated playback incl. 4K — MX, VLC ("all codecs included"), Nova, PLAYit, KM, Just, mpv (URLs in matrix above).
2. Folder-based browsing/library — VLC, Next (tree/folder/file), Nova, PLAYit, MX (scan all storage).
3. Audio + subtitle track selection — VLC (multi-track), Next, Just, Nova, mpv.
4. External subtitle file loading — MX (11 formats), Next, Just, Nova.
5. HW↔SW decoder override — MX, Next, Just, Nova, PLAYit, mpv.
6. Gestures: swipe brightness/volume, double-tap seek, pinch zoom — MX, VLC, Next, Just, PLAYit, mpv.
7. Playback speed control — MX, Next, Just, PLAYit, KM, mpv.
8. Aspect/resize control (fit/crop/zoom) — VLC, MX, Just, Next.
9. PiP or floating window — MX, VLC, Next, Just, PLAYit, mpv.
10. SD-card/external-storage browsing — MX, VLC, Nova, PLAYit, Next/Just (SAF).
11. Delete/share/manage files from within the app — VLC (multi-file delete), Just (post-playback delete), PLAYit (manage/share), MX (share).
12. Background playback (video or audio) — MX, VLC, Next, PLAYit, mpv (only Just declines).
13. Android TV / leanback build — VLC, Next, Nova, Just, MX (India TV), mpv (sideload) — **[OUT] for Aptura R1**.
14. Network stream/source support (SMB/FTP/WebDAV/UPnP/URL) — VLC, Next, Nova, MX, KM, mpv, PLAYit-cloud — **[OUT] for Aptura R1**.

### Inferences
- Items 1–12 constitute the floor a 2026 local-first player must hit; Next Player's README reads as a checklist of exactly these 12 — a useful acceptance benchmark ([GitHub](https://github.com/anilbeesetti/nextplayer)).
- Equalizer (MX, VLC, KM, PLAYit) and online subtitles (Nova, PLAYit, KM) are *common but not universal*, and both are **[OUT]** by R1 — competitors' equalizer churn (VLC's Oct-2026 "equalizer fixes") shows it's a maintenance liability, not a moat.

### Gaps
- Sleep timer and screenshot: universally rumored, zero listing evidence found in this pass.

## Key Question 3: Rare differentiators vs dated/declining features

### Takeaway
True differentiators cluster in engine depth (Just/mpv) and library intelligence (Nova); the ad-funded cohort is racing *away* from the core player toward OTT/downloader/AI bloat — leaving the "great pure player" quadrant under-occupied.

### Cited Findings — rare differentiators (1–2 apps)
| Differentiator | Who | Boundary |
|---|---|---|
| Intro/outro auto-skip | Nova only ([releases](https://github.com/nova-video-player/aos-AVP/releases)) | [IN] |
| Seamless frame-rate/refresh-rate matching + tunneled playback + DV profile 7 | Just only ([GitHub](https://github.com/moneytoo/Player)) | [IN] |
| Advanced mpv rendering: scalers, debanding, interpolation; dual subtitles | mpv-android only ([GitHub](https://github.com/mpv-android/mpv-android)) | [IN] |
| NFO metadata + online poster/backdrop scraping; TV-show/season browsing; Trakt sync; multi-device network resume | Nova only ([Play](https://play.google.com/store/apps/details?id=org.courville.nova)) | scraping/Trakt [OUT]; local NFO+seasons [IN] |
| 3D SBS/TB playback; AC3/DTS HDMI/S-PDIF passthrough (3 modes) | Nova ([Play](https://play.google.com/store/apps/details?id=org.courville.nova), [FAQ](https://home.courville.org/nova_video_player-faq/index.html)) | [IN] (TV-oriented) |
| Skip silence; pause on headphone disconnect; touch lock; post-playback actions | Just ([GitHub](https://github.com/moneytoo/Player)) | [IN] |
| Private mode suppressing playback-history recording | Nova ([Play](https://play.google.com/store/apps/details?id=org.courville.nova)) | [IN] |
| Volume boost | Just ("volume boost"), Nova ("Audio Boost") ([GitHub](https://github.com/moneytoo/Player), [Play](https://play.google.com/store/apps/details?id=org.courville.nova)); mpv users complain it's missing ([discussion #14257](https://github.com/mpv-player/mpv/discussions/14257)) | [IN] |
| Remote-access web UI (browser control with OTP) | VLC ([Play](https://play.google.com/store/apps/details?id=org.videolan.vlc)) | [OUT] |
| Device-to-device transfer/share; game hall; AI video effects | PLAYit ([Play](https://play.google.com/store/apps/details?id=com.playit.videoplayer)) | [OUT] |
| Torrent (magnet) streaming; GIF-from-video; URL/YouTube download | KMPlayer ([kmplayer.com](https://www.kmplayer.com/home), [blog](https://kmplayerofficial.blogspot.com/2025/)) | [OUT] |
| Video cutter/trim; MP3 converter; MX Share; mic voice search | MX ([Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad)) | [OUT] |
| Brand-squatting risk: fake "Infuse" app on Play Store | ([Play](https://play.google.com/store/apps/details?id=com.infuse.infuse&hl=en_US)) | warning |

### Cited Findings — dated/declining
- **Paid one-time Pro tiers are being withdrawn**: MX Player Pro pulled from Play at Amazon takeover ([r/Android](https://www.reddit.com/r/Android/comments/1gf1psl/amazon_takes_over_mx_player_app_takes_down_pro/)) — the "ads-removal IAP" model is consolidating into subscription/VIP (PLAYit's coins/VIP — [dev reply](https://play.google.com/store/apps/details?id=com.playit.videoplayer)).
- **"All-in-one downloader/streamer" era**: PLAYit game hall + AI effects + downloader, KM magnet/URL-download, Mi Video "player and downloader… stream" ([listings](https://play.google.com/store/apps/details?id=com.miui.videoplayer&hl=en_US)) — bloat direction, and all [OUT] of R1.
- **OEM stock players retreating**: Samsung removed its native player; video features folded into Gallery ([Samsung community](https://us.community.samsung.com/t5/Galaxy-S25/Disappointed-the-Samsung-video-player-app-was-removed/td-p/3251786)) — less OEM-UX competition at the top.
- **Legacy-name churn**: Beebom's classic list still ranks MoboPlayer/BSPlayer/RockPlayer-era apps ([Beebom](https://beebom.com/best-android-video-player-apps/)); 2026 roundups converge on VLC, MX, Nova, Next Player ([ottmega](https://ottmega.com/blog/best-android-media-player-2026), [free-codecs](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm)) and rank Nova (TV/NAS) and Next Player (all-rounder) on top ([YouTube roundup 2026](https://www.youtube.com/watch?v=rBEhABqzQi0)).
- **Legacy format lists linger**: MX still advertises MicroDVD/Teletext/VobSub subtitle formats ([Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad)); Just Player documents unfixable gaps (AVI 🚧, WMV, Theora, 10-bit AVC) ([GitHub](https://github.com/moneytoo/Player)).

### Inferences
- Differentiators that matter inside R1 are *engine/UX polish* items (frame-rate matching, intro-skip, volume boost, private mode) — cheap to defend, absent from the ad-funded leaders.
- Watch the 2026 rounding: reviewers now treat "no ads, no tracking, no excessive permissions" as a headline feature itself (Next/Just/Nova/VLC positioning), i.e., privacy is the differentiator of the era.

### Gaps
- No 2025–2026 source quantifies feature presence rates across >10 players (e.g., "X% ship sleep timer") — the table-stakes list above is sourced per-feature, not statistically.

## Key Question 4: Known criticisms of category leaders

### Takeaway
The dominant apps' weaknesses are structural: Amazon turned MX Player into an ad-heavy OTT funnel, PLAYit pairs aggressive ads with heavy data collection, VLC suffers UX drift and crash complaints, and Nova's TV experience lags its tablet experience — all attackable by a clean local-first player.

### Cited Findings
- **MX Player / Amazon**: users report ~7 ad breaks per 40-minute episode totaling 20+ minutes of ads (Sept 2026 review, 69 helpful votes) plus repeated Amazon-login churn — [Play listing](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad); Amazon acquired MX Player from Times Group (~$100M) — [THR](https://www.hollywoodreporter.com/business/business-news/apos-amazon-streaming-india-prime-video-and-mx-player-1236300136/); merged it with MiniTV for AVOD — [The Arc](https://www.thearcweb.com/article/amazon-merges-mx-player-minitv-for-adbased-streaming-C4eTpcoe4iCfcz7f); folded the brand into Prime Video India ad reach — [ppc.land](https://ppc.land/amazon-kills-mx-player-brand-folding-it-into-prime-video-for-india-ad-reach/); took down the Pro version and limited TV streaming to India — [r/Android](https://www.reddit.com/r/Android/comments/1gf1psl/amazon_takes_over_mx_player_app_takes_down_pro/); requests mic (voice search), Bluetooth (AV-sync latency), and MX Share camera+location; data-safety declares collection of Location + Personal info + 4 types — [Play listing](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad); Amazon plans AI for AVOD growth — [Forbes](https://www.forbes.com/sites/swetakaushal/2025/02/12/how-amazon-mx-player-plans-to-use-ai-for-avod-growth/).
- **PLAYit**: "bombarded with ads… recommend looking for an alternative" (Aug 2024 review, 11,423 helpful votes); developer's reply steers users to subscribe/use in-app coins for VIP — [Play listing](https://play.google.com/store/apps/details?id=com.playit.videoplayer); third-party audit: 23 permissions, 14 data types collected, privacy score 71/100 "use with caution" — [GameAppChecker](https://gameappchecker.com/app/com.playit.videoplayer); Play data-safety: collects Photos/videos + Audio + 4 others and **"Data can't be deleted"** — [Play listing](https://play.google.com/store/apps/details?id=com.playit.videoplayer).
- **VLC**: crash complaints on specific files — [Play review](https://play.google.com/store/apps/details?id=org.videolan.vlc); subtitles disappear when zooming and styling "too limited" (755 helpful votes; developer acknowledged) — same listing; audio "currently playing" screen missing until 2025 (7,805 helpful votes; Videolabs replied March 2025 admitting gap) — same listing; criticized for requesting highly invasive all-files storage permission — [GrapheneOS forum](https://discuss.grapheneos.org/d/2714-is-vlc-player-good-for-security-and-privacy); Reddit verdict: "Just player has smoother playback. Vlc and Nova player is a broken tool for watching video on your cell phone" — [r/androidapps](https://www.reddit.com/r/androidapps/comments/1lbmx1q/best_media_player_best_alternative_of_mx_player/).
- **Nova**: Android TV version "not so good compared to tablet version" — [r/AndroidTV](https://www.reddit.com/r/AndroidTV/comments/1kb9qil/nova_video_player_is_the_best_for_android_tv/); SMB performance and Dolby Vision decoding called blocking issues — [issue #1651](https://github.com/nova-video-player/aos-AVP/issues/1651); files sometimes not detected after remount — [issue #1454](https://github.com/nova-video-player/aos-AVP/issues/1454); SMBv2-default change broke playback until user disabled it (developer confirmed) — [Play review thread](https://play.google.com/store/apps/details?id=org.courville.nova).
- **mpv-android**: "dire need of developers… still misses some basic features like volume boost up to 200% and external [subtitle handling]" — [mpv discussion #14257](https://github.com/mpv-player/mpv/discussions/14257); no A-B repeat UI — [issue #7331](https://github.com/mpv-player/mpv/issues/7331).
- **Infuse**: no Android/Android TV version as of 2026 — [cinaura](https://www.cinaura.tv/infuse-for-android-tv), [firecore.com/infuse](https://firecore.com/infuse); community pressure for a port exists ([Firecore forum](https://community.firecore.com/t/infuse-for-android/11048)); a lookalike "Infuse" app trades on the name in Play — [Play](https://play.google.com/store/apps/details?id=com.infuse.infuse&hl=en_US).
- **OEM**: Galaxy S25 US ships without any native Samsung video player app, disappointing users — [Samsung community](https://us.community.samsung.com/t5/Galaxy-S25/Disappointed-the-Samsung-video-player-app-was-removed/td-p/3251786).

### Inferences
- The criticism pattern (ads → data collection → bloat) is the mirror image of Aptura's R1: a player that documents *fewer permissions than VLC* (VLC is the privacy leader among majors and still requires all-files access) and *zero ads* occupies the exact vacuum MX/PLAYit created for themselves.
- MX's Pro-takedown and PLAYit's pay-to-de-ads tactic suggest the ad-free niche via one-time purchase is now untested at scale — a monetization lane open to a local-only player, though category precedent (Just Player's donations, VLC's free model) is donation/free.

### Gaps
- No Accessible Android review pages surfaced in searches despite being suggested sources — screen-reader accessibility quality (TalkBack labels, TV focus) of MX/VLC/Next remains unverified; direct fetch of accessibleandroid.com recommended.
- kmplayer.com "Premium Features" page implies an ad-free tier but current Play-side pricing/status unverified.
- No quantitative 2025–2026 market-share or retention data for local players surfaced in this pass.
