# Code Gap Brief — Aptura current state (2026-10-21, post commit `ee726cc`)

## 0. Product context (for researchers reading this)

- **Product:** `org.aptura.player` — "Aptura", a local-first Android video player. R1 strategy: **no network at all** (zero `INTERNET` permission is a deliberate, verifiable privacy differentiator — DEC-012), no ads, no telemetry, no bundled video/AAC decode, library-first shape (Shape B: player-as-library).
- **Stack:** Jetpack Compose, Media3/ExoPlayer 1.5.1, Room, WorkManager, Coil (+ custom `VideoFrameDecoder`), Navigation 3. 37 Kotlin files, exactly **2 screens**.
- **Companion review:** `review/Aptura-UI-UX-Consolidated-Review.md` (findings F-01…F-19, gesture parity matrix, edge-to-edge/adaptive/perf audits). Commit `ee726cc` reports F-01…F-18 addressed; spot-checks confirm several landed (insets hide/show in player, friendly folder-name derivation, `MainScreen.kt` deleted). Full re-verification pending.

## 1. Implemented surface (what users actually get today)

- **Library screen:** adaptive grid (`GridCells.Adaptive(160.dp)`), in-memory search (`title` OR `folder` contains — the DAO's indexed `searchVideos` is unused), 2 sort orders (`DATE_ADDED_DESC`, `TITLE_ASC`), folder filter chips, WorkManager-triggered full rescan on refresh, empty/loading states. All copy hardcoded English (`strings.xml` contains exactly 1 string: `app_name`).
- **Player screen (~950 lines):** HUD (back/title/aspect pill/speed badge), transport cluster (replay/play/forward 10s + haptics), double-tap ±10s, left-half vertical drag = window brightness, right-half = `AudioManager` volume, controls lock, 5s HUD auto-hide with system-bar hide/show, error banner with Go Back/Retry, play-mode chip (cosmetic), speed sheet (8 fixed presets 0.25×–2.0×), audio-track sheet, subtitle-track sheet.
- **Persistence:** Room `media_items` — position/completion/`lastPlayedTimestamp` only. Resume: >3s & not completed → restore with 2s pre-roll; 5s checkpoints; silent (no "resuming" affordance).
- **Trust boundary (verified):** zero `INTERNET`, no `MANAGE_EXTERNAL_STORAGE`, no `POST_NOTIFICATIONS`, no telemetry SDK, no bundled video/AAC decode.

## 2. Functional-but-dead wiring (code exists, effect is nil) — top integration debt

1. **Track selection is a UI stub:** `selectAudioTrack`/`selectSubtitleTrack` only mutate StateFlows; **never call `player.trackSelectionParameters`** → sheets always show "Default Audio Track"; choosing a track does nothing.
2. **MediaSessionService is dead wiring:** `ApturaPlaybackService` (manifest-registered `MediaSessionService`) is never started/bound; UI plays on a separate app-scoped `PlayerManager` ExoPlayer → no media notification, no lockscreen/Bluetooth/headset controls. Two independent ExoPlayer instances exist.
3. **Every playback is a 1-item queue:** `onVideoClick` drops the passed list (`Navigation.kt:47–49`) → `playQueue`/`skipToNext`/`skipToPrevious` unreachable from UI; shuffle with a 1-item queue is meaningless.
4. **PlayMode chip cosmetic:** cycles UI state only; never applies repeat/shuffle to the player (since queue is 1 item).
5. `setSubtitleDelay`/`setAudioDelay` exist without any UI; `sleepTimerRemainingMs` placeholder never used.
6. Dead/unused: `DAO.searchVideos`, `getAllVideosSortedByTitle`, `schedulePeriodicScan` (defined, never called), DataStore dependency (declared, zero usage), `DiagnosticBundleExporter` (built for SCR-015, unwired), `isCompleted` computed but never rendered.

## 3. Absent user-facing features (verified absent)

Settings root (SCR-013/014) — nothing persists except playback position; **per-video settings memory absent** (no per-item speed/aspect/decoder/subtitle columns). **Per-video three-dot menu / any file operation (delete/rename/share/move/properties) — none** (spec posture: R1 non-destructive; rename/move/delete = MAY·R2). Multi-select. Folder browser (SCR-004/005). Playlists (SCR-006/007). Watch-history UI (timestamp stored, no query/sort/screen). Favorites (FR-LIB-016). Media-details view (SCR-009, FR-LIB-013). Onboarding/permission education (SCR-001, FR-LIB-001/002/003). Scan-progress surface (SCR-011) + limited-mode banner (SCR-012). Advanced sheet (SCR-010e: decoder override, stats overlay, A-B repeat, sleep timer). Up-next/ended surface + auto-advance countdown + 2h guard (SCR-010f/g). Compatibility notice (SCR-010h). Diagnostics/help UI (SCR-015/016). Destructive-action confirmations (SCR-018). "Open with"/ACTION_VIEW/share intents (FR-LIB-020). OTG/USB/secondary volumes. Localization. Light/dynamic theme. Widgets/app shortcuts/deep links. Pull-to-refresh.

## 4. Playback engine gaps vs the spec's own codec-honesty matrix (doc 07)

- **FFmpeg audio extension not wired:** build script + LGPL symbol-verification test exist (RQ-02 ✅) but no `media3-exoplayer-ffmpeg` gradle dep, no `jniLibs` `.so`, no renderer registration → today's playback is platform decoders only. ⚖️ **Flag:** the script enables `aac` decode — contradicts ADR-010/doc-07 §10 (no bundled AAC via LA question); must be resolved before any ship.
- No HW/SW/auto decoder override (FR-PLAY-003), no fallback ladder or "why it fell back" labeling, no compatibility-tier messaging (FR-PLAY-002), no HDR/DV policy (FR-PLAY-013), no refresh-rate alignment (kill-switch, FR-PLAY-012), no corrupt-file best-effort guarantees (FR-PLAY-009), no `SeekParameters` keyframe tolerance (FR-PLAY-005), no buffer tuning, no chapters (MKV), no bookmarks, no A-B loop, no frame-step, no screenshot, no URL/stream playback (held by design), no DRM (held), no wake-lock audit, no `configChanges` (rotation recreates Activity; player survives via app singleton).

## 5. Spec-internal planned backlog (documented intent, absent in code) — the honest "TODO" list

1. Onboarding + permission education + limited mode (SCR-001/012)
2. Media-details view incl. delete-from-library (SCR-009)
3. Settings root + storage/diagnostics/help/about (SCR-013–016; all FR-SET)
4. Subtitle machinery: sidecar discovery/ranking/extra folder, offset + calibration aid, styling, encoding/mojibake fix, per-video memory (FR-SUB-001…009)
5. Track-selection application to the real Player (FR-AUD-001)
6. Queue/Up-next/ended + auto-advance countdown & 2h guard (FR-CTRL-017)
7. Advanced sheet: stats overlay, A-B repeat, sleep timer, decoder override (SCR-010e)
8. Compatibility-mode notice (SCR-010h; blocked on FR-PLAY-003 fallback)
9. PiP via AndroidX VideoPlaybackPictureInPicture (FR-PLAT-001) + background-continuation policy (FR-PLAY-018/FR-AUD-006)
10. MediaSession service ↔ active player unification (FR-PLAT-002/003)
11. Error taxonomy + redacted diagnostics UI (FR-PLAY-004, FR-DIAG-001–004)
12. Scan-phase progress surface (SCR-011)
13. Open-with/share intent filters (FR-LIB-020)
14. FFmpeg LGPL audio ext production + wiring (after legal aac resolution)
15. targetSdk 35 → 36 recheck before launch (spec S7)
16. `READ_MEDIA_AUDIO` beyond spec's video-only posture — justify or drop

## 6. Explicit non-goals (R1, owner-confirmed) — any recommendation crossing these needs an ADR

iOS (closed via RQ-09) · Android TV/car/wearables/desktop/web · **all network playback** (SMB/NAS/NFS/HTTP(S)/HLS/DASH/RTSP/live, casting/AirPlay) · DRM/protected content · accounts/cloud sync · **AI of any kind (DEC-007, R3 only)** · online subtitle download · equalizer/audio effects · video editing/transcoding · torrents/download managers · ads at launch · F-Droid/GPL channel.
**Never-claims:** "plays everything" / full DV / 8K / best subtitles / instant scans / casting just-works / battery-friendly.

## 7. White space — absent from ALL specs (neither planned nor discussed)

USB-OTG/flash-drive playback · home-screen widgets · Android Auto · kids/parental controls · screenshot gesture · light-theme/theme engine · Wi-Fi transfer (device-to-device) · lyrics/album-art views.

## 8. Planned differentiators (protect and amplify — do not dilute)

1. **Subtitle system quality** ("fidelity where feasible + full UX; no competitor occupies it" — rank 1)
2. **Verifiable trust/privacy** ("not absence of ads — verifiable absence of data collection"; zero-INTERNET proof)
3. **Library scan correctness + speed at scale** on scoped storage
4. **Playback reliability discipline** (long-session A/V sync, seek, resume, update-regression prevention)
5. **HDR/DV + decoder honesty** ("why did it fall back?" labels + per-item memory; Next Player proves the design, nobody markets it)
6. **Library-first product shape**; calm/reachable/customizable UI (execution quality, not headline)
7. Adopted-practice behaviors: tri-state resume, auto-advance with cancellable countdown + 2h guard, symmetric ±10s, 42-char caption envelope + CaptioningManager delegation, YouTube speed/sleep ladders, single-pane via breakpoints, 5s auto-hide.

## 9. Top risks shaping feature scope (spec 17)

RSK-001 engine conformance (RQ-01 open) · RSK-003 "won't play my file" expectation damage · RSK-009 scope creep (firewall = de-scope ladder) · RSK-008 subtitle edge cases eroding the differentiator · RSK-002/L1 LGPL verdict → ship without audio ext · RSK-010 zero-telemetry blind spots · RSK-013 SD/low-end storage variance.
**Never-cut list:** playback honesty paths, scan reliability, accessibility floor, privacy proofs, error taxonomy.
