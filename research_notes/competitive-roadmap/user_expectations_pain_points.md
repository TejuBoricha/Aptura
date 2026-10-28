# Android Local/Offline Video Players: User Expectations, Pain Points & Delighters (2024–2026)

## Q1 — Complaints about top players (MX, VLC, Playit, KMPlayer, others)

### Takeaway
The three category leaders each own a signature complaint: MX Player = ad saturation plus the loss of its paid ad-free tier, VLC = dated UX and slow library scans, PLAYit = permission/data overreach. Each cluster maps to a positioning gap Aptura can occupy.

### Cited Findings
- **MX Player / ads:** iOS users say the app "has become absolutely unusable due to the insane amount of ads" — [App Store reviews](https://apps.apple.com/lk/app/mx-player-all-in-one-player/id1594318832?see-all=reviews&platform=iphone); MouthShut reviewers: "too many ads, recently the ads are too much disgusting" — [MouthShut](https://www.mouthshut.com/product-reviews/mx-player-reviews-925903202-page-6). MX has run full-screen ads alongside ToS/privacy-policy changes since ~2017 and sentiment never recovered — [r/androidapps](https://www.reddit.com/r/androidapps/comments/5pm0ap/discussion_whats_upwent_wrong_with_mx_player).
- **MX Player / paid tier:** MX Player Pro was removed from Google Play (Oct 2024 thread); users are pushed to sideload from mx.j2inter.com — [XDA](https://xdaforums.com/t/mx-player-pro-no-longer-on-google-play.4692396). The Play listing now shows "More by Amazon Mobile LLC," confirming Amazon ownership — [Google Play](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad&hl=en_US). MX officially has an "escalate inappropriate ads" support flow — i.e., explicit-ad complaints are frequent enough to need one — [MX support](https://support.mxplayer.in/support/solutions/articles/43000660733-how-to-report-escalate-inappropriate-or-explicit-ads-).
- **MX Player / identity drift:** On TV, "Do not confuse it with MX Player TV in the Play Store. That app is a streaming service for films and shows in India, not a player for your own files" — [free-codecs guide](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm). vdocipher's 2026 roundup calls MX's trade-off a "content/ads/subscription angle [that] can feel heavy" — [vdocipher](https://www.vdocipher.com/blog/top-video-player-for-android).
- **VLC / UX:** Android Police: VLC "is starting to look prehistoric," a "total contrast to the Material You philosophy," with weak visual contrast (yellow-on-dark icons hard to locate); explicitly compares it unfavorably to Next Player "built on Material 3" — [Android Police](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care). r/VLC redesign thread (160 votes, 42 comments): "VLC has one of the worst settings dialogs of any mainstream app" — [r/VLC](https://www.reddit.com/r/VLC/comments/1x6h1y1/new_ui_redesign_what_do_you_think). r/fossdroid: "interface is clunky and unintuitive… the biggest problem is the performance" — [r/fossdroid](https://www.reddit.com/r/fossdroid/comments/1exnfvw/whats_a_good_video_player_option_vlc_isnt_that).
- **VLC / library performance:** Play Store review calls slow rescanning its "one fatal flaw… switching from video to music then video again makes the library getting scanned… sometimes it takes up to 2 minutes" — [Google Play listing](https://play.google.com/store/apps/details?id=org.videolan.vlc&hl=en_US). Trustpilot (Oct 2025): slow-motion "freezes the frame and sometimes crashes the app" — [Trustpilot](https://www.trustpilot.com/review/www.vlc-media-player.org). r/VLC (2025/2026, Android 16): background playback "cuts out and stops playing" — [r/VLC](https://www.reddit.com/r/VLC/comments/1utfwkc/finally_given_up_on_this_app_and_uninstalled_it).
- **PLAYit / privacy:** third-party analysis found 23 permissions and 14 collected data types, privacy score 71/100 "use with caution" — [GameAppChecker](https://gameappchecker.com/app/com.playit.videoplayer); Softonic's scan flags sensitive permissions incl. READ_MEDIA_IMAGES/AUDIO/VIDEO, READ_MEDIA_VISUAL_USER_SELECTED, BLUETOOTH_SCAN, WRITE_SETTINGS, with ~26% privacy / 37% collected / 37% shared permission distribution — [Softonic report](https://playit.en.softonic.com/android).
- **KMPlayer / others:** KMPlayer (with MX) described as "good phone players that work on a TV with compromises" — [free-codecs](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm); BSPlayer's known trade-offs are "Ads complaints; fewer modern extras" — [vdocipher](https://www.vdocipher.com/blog/top-video-player-for-android).

### Inferences
- Category migration path is MX-free-tier → VLC/Next Player/mpvKt; the trigger is ads first, bloat second — [r/androidapps "Best media player (best alternative of MX player)"](https://www.reddit.com/r/androidapps/comments/1lbmx1q/best_media_player_best_alternative_of_mx_player) where alternatives are pitched as "clean, simple UI with zero ads."
- Ads, privacy, and modern Material 3 design are the three exploitable complaint axes; codec reliability and gestures are not (everyone has them).

### Gaps
- Bulk Play Store review text could not be scraped directly (listings render reviews via JS); evidence relies on mirrored review sites, App Store reviews, Trustpilot, and Reddit.
- No dedicated 2024–2026 KMPlayer review excerpts retrieved; only aggregate comparisons.

## Q2 — Switch triggers & most-requested features (GitHub evidence, thumbs-up counts)

### Takeaway
GitHub reaction counts show the demand hierarchy: background audio, Android TV support, PiP/floating window, skip-silence, and long-press 2x speed lead; subtitle polish (sync, outline, fonts, position, two-subtitle) forms a strong second tier.

### Cited Findings
- **Next Player** ([issues sorted by 👍](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc), 786 issues): Android TV support #322 (13👍) & #808 (8👍); background audio requested 3× (#251 8👍, #359 8👍, #681 5👍 — all shipped Dec 2024); skip silence #517 (8👍, shipped Jul 2024); PiP #250 (7👍); subtitle outline thickness #409 (6👍); custom subtitle fonts/placement #645 (5👍); resume-last-playback button #687 (4👍); advanced zoom #704 (4👍); already-seen indicator #822 (4👍); chapters #363/#1654; sleep timer #1243; nested folders #924; HDR tags #1260; "NEW" label on thumbnails #1141.
- **Just Player** ([issues sorted by 👍](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc), 621 issues): background playback #44 (19👍, shipped Oct 2024); dismissable UI while paused #195 (16👍, refused as wontfix); streaming-URL input for TV users #645 (14👍); long-press 2x speed #568 (9👍, shipped); local audio files #55 (9👍, wontfix); video lock #28 (6👍, shipped); HDR indicator #506 (5👍); pan+zoom #101 (5👍); video-info display #177 (5👍); buffer config #154 (5👍); disable-subs-permanently #274 (5👍); subtitle/audio sync #6 (4👍, shipped); default subs off #207 (4👍); subtitle vertical position #301; chapters #318; speed preset expansion #307; remove big play-button overlay #290.
- **Switch triggers from forums:** Bluetooth audio sync/lag is called out as Just Player's differentiator ("no Bluetooth lag / ExoPlayer / FFmpeg audio codecs" XDA title) — [XDA](https://xdaforums.com/t/app-5-0-just-video-player-no-bluetooth-lag-exoplayer-ffmpeg-audio-codecs.4189183); Android Police recommends players by single killer need: Just Player for "Bluetooth quality and latency," Nova for big-screen TV-show library sorting, mpvKt for "a cleaner UI" over mpv-android — [Android Police](https://www.androidpolice.com/i-love-these-obscure-video-players-on-android).
- **Community consensus recommendation:** "The best free android video player at the moment is Next Player. It's basically a lite open source version of MX Player Pro" — [r/androidapps](https://www.reddit.com/r/androidapps/comments/17resjv/the_best_free_android_video_player_at_the_moment); 2024-era thread recommendations center on VLC + MX Pro + Nova — [r/androidapps](https://www.reddit.com/r/androidapps/comments/1933xok/video_player_for_android_in_2024).

### Inferences
- Duplicate requests for the same feature (background audio ×3 in Next Player) are a stronger signal than raw counts.
- Wontfix'd high-reaction issues (#195 16👍, #55 9👍) are exactly the gaps a new full-feature player can fill.
- "Skip silence" reaching 8👍 and shipping within a year shows appetite for smart, low-effort viewing (also validates skip-intro-style ideas).

### Gaps
- vlc-android's issue tracker was not separately fetched; VLC requests likely visible via r/VLC threads instead.
- Exact per-issue 👍 counts change over time; numbers reflect the Oct 2026 fetch.

## Q3 — Table stakes (assumed, rarely requested)

### Takeaway
Gestures, resume, format coverage, thumbnails/folder browsing, and track/external-subtitle handling are treated as given; reviews only surface them when they break.

### Cited Findings
- Android Police describes VLC's gesture scheme (left=brightness, right=volume, horizontal seek) as "nearly perfect" — i.e., the baseline all players must ship — [Android Police](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care); MX Player Pro markets the identical gesture set — [Accessible Android](https://accessibleandroid.com/app/mx-player-pro/).
- Format coverage is the reason players exist: VLC uses software decode to "open files your stock player refuses to touch" — [Android Police](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care); "MX Player Custom Codec 3.1.0" is the #1 hottest download on free-codecs — evidence that DTS/AC3/HEVC gaps are endemic and users actively hunt codec packs — [free-codecs](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm).
- External-subtitle handling is so basic that Just Player documents a workaround for TV (open video in file manager, return, open subtitle file) — [moneytoo/Player README](https://github.com/moneytoo/Player).
- Resume/killpoint persistence appears as explicit feature requests only when missing (Next Player #687) — [nextplayer issues](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc).

### Inferences
- Aptura must treat the Next-Player-class baseline (gestures, resume, PiP, background audio, pitch-corrected speeds, network streams) as entry criteria, not differentiators — see also Next Player shipped features in Q2.
- Library-scan speed is a table stake that VLC fails publicly; a fast, quiet scan is a cheap win — [Google Play VLC review](https://play.google.com/store/apps/details?id=org.videolan.vlc&hl=en_US).

### Gaps
- Thumbnail-generation failure complaints were not directly captured in fetched threads (usually implied by library-UX requests like #1141/#822).

## Q4 — Permissions, first-run, scoped storage (Android 13–15)

### Takeaway
Users on Android 13–15 face a more complex permission matrix; players that fail to handle READ_MEDIA_VIDEO, partial photo access, and orientation quirks generate public friction, while zero-permission designs sidestep all of it.

### Cited Findings
- Android 13 replaces READ_EXTERNAL_STORAGE with READ_MEDIA_IMAGES/VIDEO/AUDIO; WRITE_EXTERNAL_STORAGE "has no effect — always denied" — [Datalogic dev portal](https://developer.datalogic.com/mobile-computers/news/a13_new_permissions_for_file_access), [StackOverflow](https://stackoverflow.com/questions/72948052/android-13-read-external-storage-permission-still-usable).
- Partial-media access exists as READ_MEDIA_VISUAL_USER_SELECTED; PLAYit declares it, meaning users routinely hit the "select which photos/videos to allow" flow — [Softonic PLAYit report](https://playit.en.softonic.com/android).
- Google's direction is "Permissionless is the future of storage": photo picker requires no media permission, but the backport excludes "Android Go and non-GMS devices" — precisely the low-end emerging-market segment — [Android Developers Medium](https://medium.com/androiddevelopers/permissionless-is-the-future-of-storage-on-android-3fbceeb3d70a).
- Android 16 blocks programmatic orientation switching; Just Player's README tells users to fix it manually via system Settings ("Aspect ratio… switch from Full screen to App default") — a first-run papercut users must solve themselves — [moneytoo/Player README](https://github.com/moneytoo/Player).
- Poorly implemented scoped storage causes "slow media queries and UI freezes"; MANAGE_EXTERNAL_STORAGE exists for file managers but "Google is strict about approvals" — players legitimately can't claim it — [itnext.io](https://itnext.io/navigating-the-challenges-of-scoped-storage-lessons-for-modern-android-apps-b5fd8318a02c).
- Scoped storage rationale includes "protecting user information (such as photo metadata)" — [AOSP docs](https://source.android.com/docs/core/storage/scoped).

### Inferences
- Aptura's zero-INTERNET + READ_MEDIA_VIDEO-only posture is provably unusual: leading competitors (PLAYit: 23 permissions) request multiples more.
- A permissionless first-run (photo-picker or MediaStore-only scan, no MANAGE_EXTERNAL_STORAGE) aligns with Google's stated direction and removes the most common Android 13–15 friction.

### Gaps
- Did not find quantified survey data on first-run abandonment; evidence is qualitative (issue threads, README workarounds, permission scans).

## Q5 — Accessibility expectations

### Takeaway
Blind users actively review players; MX Player Pro passes but with unlabeled player-screen buttons and gesture workarounds, showing the bar is low and beatable.

### Cited Findings
- Accessible Android's MX Player Pro review (tested v1.74.7 on Android 14): "overall accessibility is good, although there are quite a few unlabeled buttons, especially on the player screen. However, the latest version of TalkBack detects the icons correctly" — [Accessible Android](https://accessibleandroid.com/app/mx-player-pro/).
- Same review documents TalkBack/Jieshuo workarounds users must learn: two-finger swipes for seek/volume/brightness, TalkBack passthrough gesture for two-finger speed-swipe ("with the screen reader enabled, it isn't possible to adjust the speed" under Jieshuo), double-tap-and-hold for 2x speed, and Jieshuo "element monitoring" to auto-read subtitles — [Accessible Android](https://accessibleandroid.com/app/mx-player-pro/).
- Accessible Android maintains an app directory + bug tracker and explicitly advises developers (developer resources page exists) — [Accessible Android directory](https://accessibleandroid.com/app).
- TalkBack's two-finger double-tap ("Magic Tap") toggles media playback system-wide — [AFB Accessworld](https://afb.org/aw/22/5/17556).

### Inferences
- Player-screen content descriptions, speed control usable without gesture passthrough, and subtitle auto-read are concrete accessibility wins; no major local player is publicly praised as fully accessible, so this is an open differentiator.

### Gaps
- No Accessible Android reviews found for VLC, Next Player, or Just Player in the fetched search (only MX Player Pro surfaced); their TalkBack behavior is unverified.

## Q6 — Regional / usage variance

### Takeaway
Emerging-market usage (India-first apps, SD cards, low-storage Go devices, offline-first habits) is where heavyweight players most often fail or overreach.

### Cited Findings
- PLAYit's India positioning ("your best online video mate… in India") bundles auto SD-card identification, online search/download, floating & background play, and MP4→MP3 conversion — [playit.app](https://www.playit.app), [YouTube review](https://www.youtube.com/watch?v=pyl3Ce25Zaw); this bundling is exactly what generates its 23-permission footprint — [GameAppChecker](https://gameappchecker.com/app/com.playit.videoplayer).
- MX Player's India streaming pivot confuses device-owner users on TV ("streaming service for films and shows in India, not a player for your own files") — [free-codecs](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm); MX Pro buyers also hit geo-gates: "regionally restricted features such as online videos may not be available" — [Accessible Android](https://accessibleandroid.com/app/mx-player-pro/).
- The photo-picker backport excludes Android Go and non-GMS devices — the permission friction returns exactly on low-end emerging-market hardware — [Android Developers Medium](https://medium.com/androiddevelopers/permissionless-is-the-future-of-storage-on-android-3fbceeb3d70a).
- Codec-pack demand (MX custom codec as top download) implies regional device fleets lacking DTS/AC3/HEVC hardware decode — [free-codecs](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm).
- Storage hygiene is a stated scoped-storage failure mode: poor implementations cause freezes and slow media queries on constrained devices — [itnext.io](https://itnext.io/navigating-the-challenges-of-scoped-storage-lessons-for-modern-android-apps-b5fd8318a02c).

### Inferences
- SD-card browse support, small-APK/low-RAM efficiency, subtitle files in local languages, and no forced streaming funnel are the regionally sensitive requirements.

### Gaps
- No direct 2024–2026 user threads on SD-card SAF (SAF/highGB SD failure) were captured in this pass; SD-card claims here rest on permission scans and feature lists.

## Q7 — Delighters (word-of-mouth generators)

### Takeaway
Shipping-quality delighters per community signal: PiP/floating window, skip-silence, long-press 2x, background audio with lockscreen controls, subtitle sync wizard, sleep timer, TV support, and "already watched" indicators.

### Cited Findings
- **Floating window + PiP:** MX Player Pro advertises "a fully customizable floating window and Picture-in-Picture (PIP) mode" — [Accessible Android](https://accessibleandroid.com/app/mx-player-pro/); Next Player's PiP request (#250, 7👍) shipped in Feb 2024 — [nextplayer issues](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc).
- **Skip silence:** Next Player #517 "Fast forward during silence" (8👍) shipped Jul 2024 — [nextplayer #517](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc).
- **Long-press 2x speed:** Just Player #568 (9👍) shipped — [moneytoo/Player issues](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc).
- **Background audio + lockscreen controls:** highest-reaction requests in both repos (#44 19👍 Just Player; ×3 duplicates in Next Player) — [moneytoo/Player #44](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc).
- **Subtitle sync:** Just Player shipped subtitle/audio synchronization (#6, 4👍) — [moneytoo/Player](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc); Next Player still tracks audio/subtitle delay as #693 — [nextplayer #693](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc).
- **Smaller delighters with visible demand:** sleep timer (#1243), chapters (#363/#1654/#318), A-B repeat & playlists (#757), loop (#572), keep-screen-on-while-paused (#587), disable delete button (#660), HDR indicator (#506), resume-last-playback button (#687), already-seen/"NEW" labels (#822/#1141) — [both trackers](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc).
- **Gestures as delight:** VLC's gesture scheme earns press praise ("nearly perfect") and MX mirrors it in marketing — [Android Police](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care), [Accessible Android](https://accessibleandroid.com/app/mx-player-pro/).

### Inferences
- Delighters cluster around "effortless viewing" (skip silence, 2x hold, resume, background) and "polish that shows" (PiP, HDR badge, watch-state labels, subtitle sync) — both categories align with Media3's strengths and a Material 3 dark theme.
- Android TV was the single highest-reaction unmet request in Next Player; worth flagging to the client as a roadmap hedge even if v1 is phone-only.

### Gaps
- Frame-precise scrub preview and skip-intro specifically: no direct GitHub/Reddit thread surfaced in fetched results; nearest proxies are chapter navigation and skip-silence requests. Treat as unvalidated-by-evidence until a follow-up pass.
