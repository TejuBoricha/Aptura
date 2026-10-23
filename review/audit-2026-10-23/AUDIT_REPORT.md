# Aptura — Uncompromising Full-Stack Audit

**Snapshot:** `main` @ `588cc3a` (2026-10-23) · **Package:** `org.aptura.player` · **Stack:** Kotlin 2.3 / Compose / Media3 1.5.1 / Room / WorkManager
**Auditor stance:** senior product designer + lead engineer + QA architect + security reviewer + hostile end user.
**Nothing in the repo was modified** except this folder (`review/audit-2026-10-23/`). Nothing is committed.

Evidence tags used throughout:

| Tag | Meaning |
|---|---|
| **[V]** | **Verified at runtime** (emulator, DB inspection, `dumpsys`, release build). Reproduction steps in Appendix A. |
| **[C]** | Confirmed by reading the code at the cited `file:line`; not executed. |
| **[I]** | Inference that needs a device/test to confirm. Called out as such. |

---

## 0. Verdict

> **Aptura has an impressive surface and an unreliable core.** The sheets are polished, the privacy posture is genuinely good, and 206 tests are green. But the *main* journeys — open a video, watch it, leave it, come back to it, queue the next one — are broken in ways the test suite cannot see, and roughly a dozen prominent controls are UI-only stubs that do nothing. One dialog actively lies to the user ("Delete Permanently" does not delete anything).

The failure pattern is consistent with a process that **optimised for visible output and green checkmarks** (66 "Parts", 206 tests, "visual verification" screenshots) instead of **verifying behaviour end to end**. The repo's own "verification" artefact for subtitles (`review/captures/subtitle_attached.png`) shows *"Subtitles Off"* behind a playback-error dialog.

### Scorecard (subjective, calibrated against shipping Android media players)

| Dimension | Grade | One line |
|---|---|---|
| Functional correctness | **D** | 10 P0 defects in core flows; ~12 controls that do nothing |
| Product / UX flow | **C-** | Good ideas, but HUD never hides, Back keeps playing, resume ignored |
| Visual design | **B-** | Cohesive dark/cyan identity; unreadable selected chips, crowded top bar, inset overlaps |
| Architecture | **C-** | Global singletons, Activity-scoped "god" ViewModel (75-field state), 2,701-line composable |
| Code quality | **C** | Readable but 71 swallowed exceptions, dead code, fake data fallbacks |
| Performance | **C** | 4 Hz full-screen recomposition, no thumbnail cache, whole-table re-queries (device numbers unverified) |
| Security / privacy | **B** | No INTERNET, no backup, no telemetry — but invariants are weaker than claimed (OkHttp ships, exported receiver) |
| Accessibility | **D+** | Scrubber has no semantics, 40 dp targets, 60 hard-coded labels, gray-on-video icons |
| Responsiveness | **B-** | Rail/grid adapt; 360 dp top bar collapses the title to 2 characters |
| Test / QA | **D** | Green but blind: no test for the bugs below; invariants test scans text, not the merged manifest |
| Docs / process honesty | **D** | `MEMORY.md` and UI copy overstate (see §5) |

---

## 1. Method, coverage, and limits

**Build gate (run by me):** `testDebugUnitTest` → 206 tests, 0 failures (task was cache-hit, results dated 2026-10-22 23:58, inputs unchanged). `lintDebug` → **0 errors, 66 warnings, 1 hint** (the repo's notes claim "0 warnings").

**Static review (read in full or in the relevant parts):** `PlayerViewModel`, `Media3PlaybackController`, `PlayerManager`, `ApturaPlaybackService`, `ApturaMediaCodecSelector`, `ApturaTextRenderer`, `AudioEnhancementController`, `Navigation`, `MainActivity`, `ApturaApplication`, `PlayerScreen` (≈85 %), `MediaLibraryViewModel`/`UiState`/card composable, `MediaRepositoryImpl`, DAO/Entity/Database, `MediaScanWorker`/`Scheduler`/`MediaStoreScanner`/`SafTreeScanner`/`FileReconciliationEngine`, `SidecarSubtitleDetector`, `ApturaSettingsRepository`, diagnostics, `StorageVolumeReceiver`, theme, manifest, Gradle files, ProGuard, `PlatformInvariantsTest`.
**Skimmed only:** `SubtitleSheet` (1,355 lines), `AudioEnhancementSheet`, `MediaDetailsSheet`, `PlaybackQueueSheet`, `StatsOverlay`, `UpNextOverlay`, `CompatibilityExplainerSheet`, most of the test suite.

**Runtime:** Pixel 8 / API 35 emulator (1080×2400 @ 420 dpi) with a purpose-built corpus (4-min 720p clip, 3 episodes with `.srt` sidecars, AC3-only and DTS-only files). Also: landscape, font scale 1.3 and 1.8, 360×640 dp, 800×1280 dp, night-mode/uiMode recreation, permission revoke/regrant, `ACTION_VIEW` launch, PiP, SQLite inspection via `run-as`, `dumpsys media_session / activity / window / gfxinfo`, and an **R8-minified release build** (signed with the debug key, installed, smoke-tested: scan → play → audio/picture/queue sheets, no crash).

**Not verifiable here (see §7):** real-device codecs/HDR/thermal/battery, TalkBack, Bluetooth A/V sync, SD-card/SAF at scale, process-death restore, Android 8–12 behaviour, 10k-item libraries. Performance numbers are from a software-GPU emulator and are **indicative only**.

---

## 2. P0 — Critical (stop-ship)

Definition: breaks a core promise, loses/destroys data, or deceives the user.

### P0-01 · The playback HUD never auto-hides [V][C]
- **Where:** `ui/player/PlayerViewModel.kt:186-194` calls `scheduleAutoHide()` on **every** `Ready` emission; the controller emits every 250 ms while playing (`Media3PlaybackController.kt:98-114`). `scheduleAutoHide()` (`:1209`) cancels and restarts the 5-second job → it can never fire.
- **Observed:** HUD still fully visible at playback time 0:15 after ~14 s of no input (`evidence/s07_player_t16.png`). Spec FR-CTRL-001 says 5 s.
- **Impact:** The one thing a video player must do — get out of the way — doesn't. Controls (and the status/nav bars, which are tied to `isControlsVisible`) permanently cover the picture.
- **Why the suite is green anyway:** `PlayerViewModelTest.controlsAutoHide_whenPlaying_hidesAfter5Seconds` (`:553`) exists and passes — because `FakePlaybackController` emits **one static `Ready` state**, never the real 250 ms stream. The test encodes the author's assumption, not the product's behaviour (see §6.1).
- **Fix:** Schedule hide only on *interaction events* and on `isPlaying` false→true / sheet-closed transitions, never from the position tick. Alternative: `LaunchedEffect(isControlsVisible, isPlaying, interactionTick) { delay(5000); hide() }`.
- **Acceptance:** (1) Unit test with a virtual clock: emit 30 `Ready` states 250 ms apart → `isControlsVisible == false` at t ≥ 5 s ± 0.5 s. (2) Any tap/drag/seek resets the timer. (3) HUD stays while scrubbing, sheet open, error, locked-hint, or paused. (4) Instrumented test on emulator confirms.

### P0-02 · System Back leaves the video playing under the library [V][C]
- **Where:** No `BackHandler` anywhere (`grep BackHandler` → 0). `NavDisplay(onBack = { backStack.removeLastOrNull() })` (`Navigation.kt:177`) pops without `onPlayerDismissed()`; only the top-bar arrow calls it (`:505-508`).
- **Observed:** After gesture/button Back, `media_session` stayed `PLAYING`, position advancing 31 s → 40 s while the library was on screen. Position is not checkpointed either.
- **Impact:** Audio keeps playing from a screen showing a grid of thumbnails, with no notification to stop it (P0-03). Most users navigate with the system gesture, not the arrow.
- **Fix:** `BackHandler`/`NavDisplay` back → `onPlayerDismissed()` (pause + persist) — or tie to the player entry's `DisposableEffect.onDispose`.
- **Acceptance:** Back from player (button, gesture, predictive-back) ⇒ player paused, position persisted within 1 s, library visible; instrumented test asserts `player.isPlaying == false`.

### P0-03 · "Background playback" is not real: no foreground service, no notification, no metadata [V]
- **Observed:** `dumpsys activity services`: `startForegroundCount=0`; `dumpsys notification`: nothing for the app while playing in background; `media_session` metadata `description=null, null, null`.
- **Cause (observed outcome [V]; mechanism [I]):** `ApturaPlaybackService` builds a `MediaSession` but nothing ever connects a `MediaController`/binds the service, so Media3 never posts the media notification; `MediaItem.fromUri(item.uri)` (`Media3PlaybackController.kt:438`) carries no `MediaMetadata` (title/artwork). The queue also lives outside the player timeline (single `setMediaItem`), so notification/headset **next/previous are disabled**.
- **Impact:** No lock-screen/notification/Bluetooth controls; on Android 12+ a background app with no foreground service is killable at will; Settings' "Background Audio Playback" toggle promises behaviour that cannot exist. The manifest declares `FOREGROUND_SERVICE_MEDIA_PLAYBACK` but never uses it.
- **Fix:** Put the queue in the ExoPlayer timeline (`setMediaItems`), supply `MediaMetadata` (title, duration, thumbnail), connect a `MediaController`/use `MediaSessionService` correctly so the notification appears, honour the setting.
- **Acceptance:** Press Home during playback with background audio ON → media notification with title + play/pause/next/prev; headset next works; `startForegroundCount ≥ 1`; with the toggle OFF playback pauses on leave.

### P0-04 · Resume position and per-video settings are not applied when starting from the library [V][C]
- **Where:** `Navigation.kt:208-221` builds `DomainMediaItem(uri, title, durationMs, sizeBytes, "video/*")` from the UI card and **drops** `playbackPositionMs`, `isCompleted`, `resizeMode`, `playbackSpeed`, `decoderStrategy`, `audioDelayMs`, `subtitleDelayMs`. `PlayerViewModel.playQueue` then sees position 0 and defaults. (`rawItem` with the full data is available on the card; the Details "Play" path uses it correctly.)
- **Observed:** Library card showed ~20 % progress for `long_720p`; tapping it started at 0:03/0:00 (`evidence/s09_lib_after_back_with_bg_play.png` shows the bar). Three separate re-opens, same result.
- **Impact:** "Resume where you left off" — a headline feature, persisted and displayed — silently doesn't work on the primary path. Per-video speed/aspect/decoder never restore either.
- **Fix:** Pass `card.rawItem` (domain item) into `playQueue`. Add a test that goes through the *real* click path.
- **Acceptance:** Play 30 s, Back, reopen from grid ⇒ resumes at ≈ 28 s (pre-roll 2 s); speed/aspect/delays restore.

### P0-05 · Every library scan wipes per-video preferences (and renames) [V][C]
- **Where:** `FileReconciliationEngine.kt:65-73` and `:97-105` copy only `playbackPositionMs`, `isCompleted`, `lastPlayedTimestamp` from the existing row; the DAO uses `OnConflictStrategy.REPLACE`. Speed, resize, decoder, audio/subtitle delay (and the user's **title rename**) revert to defaults on each scan.
- **Observed (SQLite):** `ac3_only.playbackSpeed` was `1.5` → tapped *Refresh* → `1.0`. Position survived, preference did not.
- **Impact:** Silent data loss of user choices; compounds P0-09 (scans happen on manual refresh and on volume events). Bookmarks are keyed by `mediaUri`, so SAF renames/moves also orphan them.
- **Fix:** Merge *all* user-owned columns in reconcile (or `@Update` only scanner-owned columns instead of REPLACE).
- **Acceptance:** Property-based test: for every `MediaItemEntity` field marked user-owned, reconcile(existing, scanned) preserves it; DB test: set speed 1.5 → run worker → still 1.5; rename survives.

### P0-06 · "Delete Permanently" does not delete; "Hide" is not persistent [V][C]
- **Where:** `Navigation.kt:364-374` → `libraryViewModel.removeVideoFromLibrary(uri)` (DAO `DELETE` of the *index row*). Dialog copy (`MediaDetailsSheet.kt:794-815`): *"Permanently delete … from your device's physical storage? … Android will prompt you to confirm the deletion."*
- **Observed:** Confirmed the dialog → no Android prompt; `ls /sdcard/Movies/AuditMisc` → file still there (59 MB); after *Refresh* the item **reappeared** (`evidence/v07_delete_dialog.png`).
- **Impact:** Deceptive destructive-action copy (users believe space was freed / content gone) and a "Hide" that un-hides itself at the next scan.
- **Fix:** Either implement `MediaStore.createDeleteRequest()` (API 30+) / SAF delete, or remove the Delete button. Persist hidden URIs in a `hidden` table consulted by the scanner.
- **Acceptance:** Delete ⇒ system consent dialog ⇒ file removed from disk and index; Hide ⇒ stays hidden across scans until "Show hidden" is used.

### P0-07 · Queue modes are broken: Repeat All = Repeat One, Shuffle does nothing [V][C]
- **Where:** `Media3PlaybackController.setPlayMode` (`:590-598`) sets `player.repeatMode = REPEAT_MODE_ALL` / `shuffleModeEnabled`, but every item is played with `setMediaItem` (single-item timeline), so ExoPlayer loops the same item forever and shuffle has no effect. `skipToNext` treats `SHUFFLE` as sequential (`:467`).
- **Observed:** 3 episodes, Repeat All, seek to end → the same episode restarted (0:05 within 5 s); never advanced.
- **Impact:** Two of four play modes are wrong. A "Repeat All" playlist loops one file.
- **Fix:** Model the queue inside the player (`setMediaItems`, native shuffle/repeat) — solves P0-03 as well.
- **Acceptance:** Queue [A,B,C] + Repeat All: A→B→C→A; Shuffle visits each exactly once per cycle (seeded, testable).

### P0-08 · Controls that do nothing (UI-only features presented as real) [C]
| Control | Reality | Evidence |
|---|---|---|
| **Lip-sync / audio delay** (stepper, "Buds +150 ms" presets, shown in Stats) | Stored, persisted, displayed — **never applied to audio** | `Media3PlaybackController.kt:690-693`; grep: no consumer outside state/DB |
| **Contrast / Saturation** sliders | Never applied (no `ColorFilter`/`ColorMatrix` anywhere); only night-mode amber overlay exists | `PlayerScreen.kt:403-413`, sliders at `:2540-2600` |
| **"Audio Only"** button (Details) | Identical to *Play* | `Navigation.kt:292-295` |
| **Double-Tap Seek Step** setting (5/10/15/30 s) | Ignored; hard-coded `10_000L` | `PlayerScreen.kt:498,506,579,587,1128,1166` |
| **Up Next Countdown** setting | Ignored; hard-coded `10 downTo 1` | `PlayerViewModel.kt:387` |
| **Background Audio** setting | Never read | grep `backgroundAudioEnabled` → Settings screen only |
| **Decoder "Hardware"** vs "Auto" | Branches are identical | `ApturaMediaCodecSelector.kt:39-48` |
| **Frame stepper** | Fixed 42 ms regardless of fps; default (closest-sync) seek, doesn't pause | `PlayerViewModel.kt:1182-1191` |
- **Impact:** Each is a trust break; lip-sync presets for Bluetooth are the most harmful (users will "fix" sync and nothing changes). Four of seven playback settings are placebo.
- **Fix:** Implement (audio delay needs a custom `AudioProcessor`/offset; contrast/saturation needs a GL effect via Media3 `Effects` or a `ColorMatrixColorFilter` on a `TextureView`) **or remove the control**. Ship nothing that isn't wired.
- **Acceptance:** Per control, an automated test proving the effect (e.g. audio delay +200 ms shifts audio PTS by 200 ms in a captured sink; seek step setting 30 s → double-tap seeks 30 s).

### P0-09 · The library is never refreshed automatically [V][C]
- **Where:** `MediaScanScheduler.schedulePeriodicScan` has **zero callers**; `scheduleImmediateScan` runs only on permission grant, folder add, manual Refresh/Rescan, volume broadcasts.
- **Observed:** New videos pushed to `/sdcard/Movies` did not appear until I tapped Refresh; the release build, granted via `pm grant`, showed *"No videos found"* until *Scan Device*.
- **Impact:** A camera/download/screen-recorder user opens Aptura and doesn't see yesterday's video. Refresh gives no feedback either: `onRefresh` flips `isRefreshing` true→false around a call that only *enqueues* work (`MediaLibraryViewModel.kt:181-190`).
- **Fix:** Scan on cold start (cheap `MediaStore` diff), register a `ContentObserver` on `MediaStore.Video`, schedule the periodic worker, surface worker state.
- **Acceptance:** Record/copy a video → appears within 5 s of opening the app; spinner reflects real `WorkInfo` state.

### P0-10 · Sidecar-subtitle auto-pairing is wrong, inert, and racy [V][C]
- **Wrong match:** `SidecarSubtitleDetector.kt:135` derives `baseName = name.substringBeforeLast(".")` from the **MediaStore title without extension**, so `Show.S01E01` → `Show`, and the prefix rule (`:205-207`) attaches **every** `Show.*.srt`. Observed on E01: sheet lists S01E01, S01E02 **and** S01E03 subtitles (`evidence/v30_subtitle_sheet.png`); toast says *"Auto-paired: S01e01"* (language label is the junk string after the prefix). Any title with dots (`Spider.Man.2002`) mis-matches.
- **Inert:** Header says *"Active: Off"* and "Subtitles Off" stays selected — nothing is enabled by the "auto-pair".
- **Not rendering (unresolved) [V]:** after I manually selected the S01E01 track the subtitle icon lit up and the sheet said Active, but **no cue text was visible** at 0:13–0:14 (cue window 10–20 s). Not root-caused; needs investigation of `ApturaTextRenderer` (`Renderer by delegate`) + `PlayerView.subtitleView`.
- **Racy:** `Navigation.kt:439-474` runs `detectSidecars` in a `LaunchedEffect` on the **main thread** (file listing + validation) and then `setMediaItem` again (`Media3PlaybackController.kt:710-739`). Once (BBB trailer + `.en/.es.srt`) playback stayed in `BUFFERING` indefinitely with no error and no timeout UI (`evidence/v43_now.png`); the second attempt worked.
- **Only first video:** the effect is keyed on `playerKey.videoUri` (the first item), so Up Next episodes never get detection [C].
- **Side-effect:** `setSeekParameters(EXACT)` (`:734`) is set globally and never reset → slower seeking forever after.
- **Fix:** Match on the *file name*, not the title; require exact or `name + "." + lang` patterns; run off-main with `Dispatchers.IO`; attach via the item's `SubtitleConfiguration` *before* `prepare()`; enable the default track; trigger per queue item; add a "buffering too long" recovery.
- **Acceptance:** `Show.S01E02.mkv` + `Show.S01E01/02/03.srt` ⇒ only E02 pairs, is enabled, cues render at the right time; Up Next E03 pairs E03; 20 consecutive launches, 0 hangs.

---

## 3. P1 — Major weaknesses

| ID | Finding | Evidence | Impact / Recommendation / Acceptance |
|---|---|---|---|
| **P1-01** | **Selected chips are unreadable** (≈1.8:1 contrast) | [V] `evidence/v01_settings_top.png`, `v15…png`; [C] `theme/Type.kt:15-55` hard-codes `color =` into every `TextStyle`, which **overrides** M3 `selectedLabelColor` | Selected value of every Settings option is the least legible thing on screen. Remove `color` from the Typography styles (colour belongs to the container), add a contrast unit/screenshot test. *AC:* selected-chip label ≥ 4.5:1 in all chip groups. |
| **P1-02** | **Activity recreation duplicates the player on the back stack** (`ACTION_VIEW` launches) | [V] night-mode toggle ×2 ⇒ 3 Back presses to reach the library (4 to leave the app). [C] `Navigation.kt:153-158` re-runs on recreation; `configChanges` lacks `uiMode|fontScale|density|locale|layoutDirection`; no `launchMode`/`onNewIntent` | Back-trap + restart of playback after dark-mode/font change. *AC:* config change keeps the same stack; second "Open with" while running reuses the instance. |
| **P1-03** | **Onboarding re-appears on every cold start without permission; "Skip" is dead** | [V] `onboarding_completed=true` in DataStore, permission revoked ⇒ onboarding shown; two taps on *Skip* did nothing. [C] `startKey` uses initial `ApturaSettings()` (`Navigation.kt:90-94`); Skip is drawn **before** the full-screen pager in the same `Box` (`OnboardingScreen.kt:67-92`) so the pager swallows the touch | "Continue without access" users are nagged forever; first-run CTA broken. *AC:* start destination resolved after DataStore loads; Skip works. |
| **P1-04** | **PiP is unreliable** | [V] Home during playback did **not** enter PiP; after one manual PiP press, Home did. Log: *"Skip client enterPictureInPictureMode request while pausing, auto-enter-pip is enabled"*. [C] `setAutoEnterEnabled` only inside the manual path (`MainActivity.kt:82-84`); lint `PictureInPictureIssue`; no feature check | Expected PiP on Home fails on first use. Set PiP params proactively (`setPictureInPictureParams` with auto-enter + `sourceRectHint`) whenever playing; guard `FEATURE_PICTURE_IN_PICTURE`. |
| **P1-05** | **Brightness gesture leaks into the whole app** and mis-seeds | [V] after leaving the player the **library window** still had `screenBrightnessOverride=0.78` (system setting 0.40). [C] `PlayerScreen.kt:542-549` never restores; assumes 0.5 when unset | Dims/brightens other screens permanently. Read `Settings.System.SCREEN_BRIGHTNESS`, restore `BRIGHTNESS_OVERRIDE_NONE` in `onDispose`. |
| **P1-06** | **Decoder choices leak** | [C] In-player decoder change/"Switch to Software" also writes the **global** default (`Navigation.kt:577-580`); `playQueue` re-applies each item's stored value (0) over the global default | One-off troubleshooting permanently changes all future playback; the Settings default is ignored for library playback. Separate "this video" from "default". |
| **P1-07** | **Sleep timer UI lies** | [V] start 15 m → change speed → sheet shows *Off* while the job still runs. [C] `PlayerViewModel.kt:291` overwrites `sleepTimerRemainingMs` from a controller that never sets it; the remaining value never ticks (`:1065-1074`) | Show a real countdown from one source of truth; fade-out before pausing. |
| **P1-08** | **State leaks between videos** | [C] Activity-scoped ViewModel: A-B loop points (`PlayerViewModel.kt:182-185`), picture adjustments, bookmarks job, `isEnded` survive `skipToNext/jumpToQueueItem` | A loop set in episode 1 re-seeks episode 2. Reset per-item state on item change; add test. |
| **P1-09** | **The app states things it doesn't know** | [V] Details shows `/storage/emulated/0/AuditMisc/dts_only` (real: `/storage/emulated/0/Movies/AuditMisc/dts_only.mkv`) — path is *fabricated* (`MediaLibraryUiState.kt:140-147`) and has a Copy button. [C] fallbacks `"Stereo (System Decoded)"`, `"No embedded subtitles"` (`MediaDetailsSheet.kt:347,360`); Stats default `"video/mp4"`, `48000 Hz`, `"Stereo (2 ch)"` (`Media3PlaybackController.kt:350-358,381,389`); "Video Bitrate" is overall bitrate; Settings footer *"v1.0.0 (Build 63)"* (`SettingsScreen.kt:670`) vs `versionCode=1` and credits **"LGPL FFmpeg"** that isn't in the build (`:676`) | A diagnostics UI that invents values is worse than none. Show "Unknown", read `PackageInfo`, resolve real path via `MediaStore.RELATIVE_PATH`. |
| **P1-10** | **Player top bar collapses on 360 dp phones** | [V] title rendered as **"lo…"**; at 411 dp "Show.S01…" hides the episode number (`evidence/s_small_player.png`). [C] 5 controls + chips in one `Row` (`PlayerScreen.kt:935-1086`), orientation/PiP/aspect are `40.dp` (< 48 dp rule in DESIGN.md §4) | Title is the primary orientation cue. Move secondary actions into an overflow/sheet; give the title ≥ 40 % width; 48 dp targets. |
| **P1-11** | **Accessibility is weak** | [C] Scrubber is a `Canvas` with no `semantics`/`progressBarRangeInfo`/`setProgress` (TalkBack can't scrub) `PlayerScreen.kt:1235-1281`; scrubber height 36 dp; icons tinted `TextMediumEmphasis` (#9BA3AF) sit on arbitrary video (invisible on bright frames, `evidence/v12_landscape.png`); `contentDescription = "Play Mode: NORMAL"` exposes an enum; no live-region for toasts; single-tap delayed by double-tap detection. [V] a11y tree otherwise has labels | Real TalkBack run needed (§7). *AC:* scrubber adjustable via `a11y` actions; all targets ≥ 48 dp; icons ≥ 3:1 over worst-case frames (use scrim chips). |
| **P1-12** | **No localisation** | [C] `strings.xml` has 1 string; `stringResource` uses = **0**; 60 hard-coded `contentDescription`s, 15 hard-coded Toasts; `Locale.US` formatting; Toast "Will play next: …" | OD-5 promises "global from day one". Extract strings, add pseudo-locale test, RTL pass (manifest has `supportsRtl=true` but layouts are untested). |
| **P1-13** | **Thumbnail pipeline** | [V] Settings *Thumbnail Cache: 0 B* with a "Clear" button; [C] Coil `VideoFrameDecoder` frames aren't disk-cached, fixed `videoFrameMillis(2000)`, `MediaMetadataRetriever` per tile (`MediaLibraryScreen.kt:813-826`) | Scrolling a big library re-extracts frames repeatedly; the button is a placebo. Add a persistent thumbnail store (generate once, key by `mediaStoreId+modified`). |
| **P1-14** | **Library data/UX model** | [V] default sort = newest first ⇒ episodes list **E03, E02, E01** and queue "Next" is disabled on E01. [C] `title.lowercase()` sort isn't natural (`E10` < `E2`); folder identity = `BUCKET_DISPLAY_NAME` (two same-named folders would merge [I]); queue = *filtered grid*; `DURATION > 0` hides un-probed files (`MediaStoreScanner.kt:48-50`); `getAllVideos()` re-emits the whole table at every 5 s position checkpoint (`MediaLibraryViewModel.kt:79-125`); only 2 sort modes; no Continue-Watching, no folder navigation, no multi-select | Natural sort, "Continue watching" rail, folder hierarchy view, sort by modified/size/duration/last-played, narrow the Room query. |
| **P1-15** | **Position persistence is fragile** | [C] `onCleared()` calls `persistCurrentPosition()` which launches in the already-cancelled `viewModelScope` (`PlayerViewModel.kt:1265-1276`) — very likely a no-op [I: confirm with a 10-line JVM test]; system Back doesn't checkpoint (P0-02); `lastPersistedPositionMs` not reset between items | Up to 5 s (or the whole session on Back) lost. Persist on `ON_STOP` and item change with `NonCancellable`. |
| **P1-16** | **Audio-effects engine** | [C] Effects not persisted (EQ resets each launch); a new `AudioEnhancementController` + `Equalizer/BassBoost/Virtualizer/LoudnessEnhancer` per Activity recreation and **never released** (`Navigation.kt:78-85`) ⇒ eventual `AudioEffect` exhaustion; loudness +12 dB with no limiter (`:168`); slider changes are silent no-ops unless the master toggle is on; one failed band aborts the rest (`reapplyState` single try/catch); "Flat" displayed while bands show +3 dB (`evidence`: `r03`) | Release in `onDispose`, persist, cap/limit, reflect hardware state honestly. |
| **P1-17** | **Performance hot spots** | [V-indicative] `gfxinfo` while playing: 47 frames/12 s (≈4 Hz), 40 % janky, p50 18 ms / p90 44 ms (software emulator). [C] whole `PlayerUiState` (75 fields) recomposes every 250 ms; `AndroidView.update` re-creates `CaptionStyleCompat` each tick (`PlayerScreen.kt:347-396`); `BoxWithConstraints` at the root; single 1,700-line composable | Split state (position as `State<Long>`/lambda providers), `derivedStateOf`, hoist subtitle style, decompose `PlayerScreen`, add Macrobenchmark + Baseline Profile. |
| **P1-18** | **Diagnostics can't diagnose** | [C] Ring buffer is in-memory only (`DiagnosticRingBuffer`), no `UncaughtExceptionHandler`, hard-coded `appVersion="1.0.0", buildType="debug", buildHash="aptura-part63"` (`ApturaDiagnostics.kt:31-37`) | After a crash the export says "No errors logged in recent sessions". Persist to a bounded file, install a crash handler, read real build info. |
| **P1-19** | **Release engineering gaps** | [C] No CI (`.github` absent); `checkAarMetadata` tasks force-disabled (`build.gradle.kts` end); Room `fallbackToDestructiveMigration(dropAllTables = true)` + `exportSchema = false` ⇒ one forgotten migration wipes history/bookmarks; blanket `-dontwarn androidx.media3.**`; no Baseline Profile; unsigned release; lint 66 warnings (19 outdated deps) | Add CI (gate + lint + R8 smoke), export schemas + migration tests, remove blanket dontwarn, pin & update dependencies. **[V] R8 release build installs and runs (3.3 MB/ABI).** |
| **P1-20** | **"Invariants" are weaker than advertised** | [V] debug APK dex contains `okhttp3.OkHttpClient`, `okio`, `java.net.HttpURLConnection` (via Coil 2.7) although `invariants.md` says *"no HTTP clients may exist"*; merged manifest adds `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`. [C] `PlatformInvariantsTest` greps the **source** manifest and `build.gradle.kts` text only (version-catalog deps invisible; banned keyword `"adjust"` would false-positive) | INTERNET absence is the only real barrier. Assert on the **merged** manifest + `dependencies` output; consider Coil 3 (network module optional). |
| **P1-21** | **Privacy messaging vs. web subtitle search** | [C] `WebSearchDialog` sends the (editable) filename-derived query to Subdl/OpenSubtitles/Google via the browser; onboarding says "Your personal videos never leave your device" and Settings says "100% Offline"; the dialog doesn't disclose that titles go to third parties; Subdl is pre-selected as "Recommended" | Add an explicit disclosure line + per-session consent; reconsider "Recommended" third-party default. |
| **P1-22** | **Audio-codec coverage is device-roulette** | [C] No FFmpeg/extension decoder in dependencies (`libs.versions.toml`), yet spec/Settings footer imply it. AC3/E-AC3/DTS/TrueHD depend on OEM `MediaCodec` decoders; the app's explicit software-fallback flow is video-only (`Media3PlaybackController.kt:140-191`) and there is no non-`MediaCodec` audio decoder to fall back to [I: verify on real devices] | "Plays everything" is the category promise; silent video for DTS is the top complaint in this category. Decide: integrate LGPL FFmpeg audio extension (as the spec says) or state the limits in-app. |
| **P1-23** | **`PlayerScreen` / ViewModel architecture** | [C] `PlayerScreen.kt` 2,701 lines with ~100 parameters; `PlayerViewModel` 1,277 lines/3 constructor overloads + `clearForTesting()` in prod; "pure Kotlin" claim contradicted (imports `data.*`, `java.io.File`); `PlayerManager` global singleton; repositories built inside `@Composable` with `remember` (`Navigation.kt:68-72`) | Introduce DI (Hilt/Koin or manual graph), split into `PlayerState` machine + feature slices (queue, subtitles, audio, picture), sealed `PlayerScreenState` instead of ~25 booleans. |

---

## 4. P2 — Minor imperfections (still worth fixing)

**Library & details**
- **P2-01** Card layout is not baseline-aligned: single-line titles leave a large gap above the metadata, two-line titles pack tightly; the `⋮` floats mid-block (`evidence/v26…`). Use a fixed 2-line title slot or align metadata to the bottom.
- **P2-02** `⋮ "More actions"` opens the full-screen *Details* — not a menu. Rename or open a real overflow menu (Play next / Add to queue / Hide / Share).
- **P2-03** Details stacks **9 actions** (Play, Audio Only, Play Next, Add to Queue, Rename, Open With, Share, Hide, Delete) with equal weight; destructive ones sit beside benign ones. Group/overflow them.
- **P2-04** Title inconsistency: grid shows `dts only`, details shows `dts_only`, player shows `Show.S01…`; `cleanTitle` strips only `_` and a fixed extension list.
- **P2-05** Progress bar: `LinearProgressIndicator` with an 80 %-black track + M3 stop-indicator dot looks like a rendering glitch on thumbnails (`evidence/v14…`). Use a flat 3 dp bar without the stop dot.
- **P2-06** Search placeholder wraps to two lines at 360 dp / large fonts; the `+ Folder` chip is clipped with no scroll affordance; no app title/branding on phones.
- **P2-07** Tablet (800 dp): `NavigationRail` mixes destinations and actions ("Open", "Refresh") and the brand block "Aptura" is styled like a selected destination (`evidence/t_library.png`).

**Player**
- **P2-08** Long-press-2× release also toggles the HUD (tap fires on release) [V]; `detectTapGestures` with `onDoubleTap` delays every single tap by ~300 ms.
- **P2-09** Transport buttons use 50 %-black circles with **video-coloured** halos — on saturated frames the icons lose contrast (`evidence/v12…`). Use a consistent scrim.
- **P2-10** Orientation button on ≥ 600 dp does nothing but show a toast (`PlayerScreen.kt:970-972`) — hide it. PiP button isn't hidden on devices without PiP.
- **P2-11** Overlay stacking uses magic offsets (`116.dp`, `160.dp`) to dodge each other; compat chip, orientation toast and auto-pair toast can collide.
- **P2-12** Error card copy hard-codes "10-bit" for any `UNSUPPORTED_PROFILE` and shows the raw enum (`UNSUPPORTED_PROFILE`) as a chip; "Diagnostics" copies to clipboard silently.
- **P2-13** Sheets don't pad for the gesture bar: *Subtitle* sheet's "Import Subtitle File" and *Advanced* sheet's last header sit under the nav handle (`evidence/v30…`, `r04…`).
- **P2-14** Playback speed is configurable in **three** places (HUD chip sheet, Advanced tab, Settings default) with different option sets (0.25-2.0 vs 0.5-2.0 vs 0.75-2.0).
- **P2-15** Subtitle sheet: every external row has a red ✕ that calls `removeExternalSubtitles()` — it removes **all** external subtitles, not that row (`SubtitleSheet.kt:545`). Language label is a raw string (`S01e01`).
- **P2-16** "Advanced Settings" is a kitchen sink (speed, sleep timer, A-B, frame step, bookmarks, decoder, aspect, night mode, picture) — poor information architecture; bookmarks have no notes/rename UI.
- **P2-17** No predictive back (`android:enableOnBackInvokedCallback` missing; log warning). No system-gesture exclusion rects although DESIGN.md §4 requires them for the bottom scrubber (`grep systemGestureExclusion` → 0).

**Design system / docs drift**
- **P2-18** Typography defines 5 of 15 M3 styles; code uses `labelMedium/Large`, `bodySmall`, `headlineSmall` which fall back to defaults ⇒ inconsistent sizes. DESIGN.md names Inter; `FontFamily.Default` is used. `ApturaTheme(darkTheme)` param is ignored.
- **P2-19** DESIGN.md vs code: "true `#000000`" vs `SurfaceBase #0D0E11`; scrubber 4→8 dp expand and floating timecode chip (§5.1) not implemented (fixed 6 dp); thumb 16/20 dp spec vs 20/24 dp drawn; touch targets 48 dp violated (P1-10).

**Engineering hygiene**
- **P2-20** 71 `catch (_: Throwable|Exception)` blocks swallow errors (18 files), including the whole scan worker (`MediaScanWorker.kt:94-96` → `Result.failure()` with no log). Route through `ApturaErrorHandler`.
- **P2-21** Dead/unused code: `searchVideos`, `getAllVideosSortedByTitle`, `getVideosByFolder`, `schedulePeriodicScan`, `release()`, `data_extraction_rules.xml`/`backup_rules.xml` (lint: unused), `ApturaTheme.darkTheme`.
- **P2-22** `StorageVolumeReceiver` is declared in the manifest **`exported="true"` without a permission** *and* registered at runtime without an export flag (`ApturaApplication.kt:19`, lint `UnspecifiedRegisterReceiverFlag`): any app can spoof `MEDIA_EJECT` to pause playback / trigger scans; the receiver stores the raw path in the ring buffer (redacted only at export).
- **P2-23** SAF scan runs `MediaMetadataRetriever` on **every** file on **every** scan (no differential skip) and has no stop/progress handling (`SafTreeScanner.kt:118-136`); synthetic IDs are a 31-bit hash with a unique index + REPLACE → collisions silently overwrite rows (`:190-194`).
- **P2-24** `AndroidManifest`: no `launchMode`, `onNewIntent` unhandled; `ACTION_VIEW` accepts `file://`.
- **P2-25** Lint: 66 warnings (19 `GradleDependency`, 18 `UseKtx`, 10 `ModifierParameter`, `ConfigurationScreenWidthHeight`, `SourceLockedOrientationActivity`, `SelectedPhotoAccess`). Repo notes say 0.
- **P2-26** `.agents/MEMORY.md` is stale (says 97 tests/Part 67; HEAD has 206 tests and later commits); several "verified" claims (e.g. sleep timer "live countdown") are false.

---

## 5. Where the documentation overstates (trust debt)

| Claim | Reality |
|---|---|
| "Sleep Timer with live countdown & cancel" (`MEMORY.md` Part 62) | Counter never ticks; UI resets to Off (P1-07) |
| "Lip-Sync Audio Delay Stepper" | No effect on audio (P0-08) |
| "Differential Ingest … periodic" | Periodic scan never scheduled (P0-09) |
| "Delete from Device … Android will prompt you" | Index-row delete only (P0-06) |
| "Auto-paired" subtitle toast | Wrong files, not enabled (P0-10) |
| "Lint clean, 0 warnings" | 66 warnings |
| "No HTTP clients may exist" | OkHttp ships in the APK (P1-20) |
| "LGPL FFmpeg" (Settings footer) | Not integrated (P1-22) |
| "Visual verification gate" | Screenshots capture states, not behaviours (`subtitle_attached.png` = "Subtitles Off") |
| "100 % green" | Green because tests run against fakes that don't reproduce the failing paths (e.g. the auto-hide test) |

---

## 6. Systemic root causes (fix these or the bugs come back)

1. **Tests verify the implementation's own assumptions.** The auto-hide test passes against a fake that never ticks (P0-01); sidecar tests use temp-dir `File` access (works in JVM, not under scoped storage); invariants test greps text; Robolectric UI tests assert node presence, not behaviour. *Fix:* a small, ruthless **instrumented E2E suite** of the 12 core journeys (see §8) that runs in CI on an emulator, plus screenshot tests for contrast.
2. **UI-first development.** State fields and screens were added before engines (`audioDelayMs`, `contrast`, `backgroundAudioEnabled`). *Fix:* a "no control without an effect test" rule and a feature-flag list.
3. **Implicit state machine in booleans + a global singleton player.** `PlayerUiState` has ~25 independent flags (`isAudioSheetVisible`, `isQueueSheetVisible`, …) and Activity-scoped ViewModel state leaks (P1-08). *Fix:* sealed UI states, one `ModalState`, per-item reset.
4. **Design tokens bypassed.** `color` baked into `TextStyle` (P1-01); sizes 40 dp vs 48 dp rule; magic offsets. *Fix:* lint/screenshot tests for token usage.
5. **Platform-contract assumptions** (scoped storage, MediaSession notification, PiP auto-enter, activity recreation) were never exercised on a device. *Fix:* device-matrix smoke tests.
6. **Docs written as marketing.** Replace status prose with generated facts (test count, lint count, feature-flag table).

---

## 7. What I could not verify — test plan

| Area | How to verify |
|---|---|
| TalkBack / Switch Access | Real device; scrub, sheets, queue reorder, error dialog |
| Codecs: AC3, E-AC3, DTS, TrueHD, 10-bit HEVC, HDR10/HLG, Dolby Vision | 3 devices (Pixel, Samsung, low-end MediaTek) with the fixtures in `fixtures/a_matrix` |
| ASS fidelity (spec classes E/F/X) | ExoPlayer's SSA parser is not libass — run `fixtures/adv_sub/ass_fidelity` and compare |
| Subtitle delay correctness | Selected-track rendering (P0-10 unresolved) then ± delay with a clapper test clip |
| Bluetooth A/V sync | Needs P0-08 implemented first |
| SD card / USB / SAF at scale | 5k-file tree; measure scan time, ANR, battery |
| 10k-item library | `fixtures/scale` generator; scroll jank, DB re-emission, memory |
| Process death / low-memory restore | `adb shell am kill`, "Don't keep activities" |
| Android 8–12, Android 14 partial media access | Emulators 26/29/31/34 |
| Real performance | Macrobenchmark on a mid-range phone; Baseline Profile |
| Long-run leaks | 2 h soak, LeakCanary, `AudioEffect` count |
| Predictive back / foldables / ChromeOS keyboard | Emulator + hardware keys (space, arrows, J/L) |

---

## 8. Recommended sequence

**Stop-ship (≈ 1 week):** P0-01, 02, 04, 05, 06, 09 (all small, high-trust); P0-08 *remove or hide* every inert control until implemented; P1-01 (contrast), P1-03.
**Then (≈ 2–3 weeks):** P0-03 + P0-07 together (queue in the player timeline + media notification + metadata); P0-10 (rewrite sidecar matching + investigate rendering/buffering); P1-02, 04, 05, 06, 07, 08, 09.
**Then:** accessibility + localisation pass (P1-10/11/12); thumbnail store + library model (P1-13/14); effects/engine honesty (P0-08 implementations: audio delay `AudioProcessor`, picture via Media3 `Effects`); diagnostics (P1-18); CI + migrations (P1-19).
**Then:** decomposition/perf (P1-17/23), FFmpeg-audio decision (P1-22).

**New quality gate (add to `tools/gate.ps1` and CI):**
1. Emulator E2E for: HUD auto-hide; Back-pauses; resume from grid; scan keeps prefs; delete/hide; Repeat-All order; sidecar exact-match; process-death resume; config-change stack; PiP on Home.
2. Contrast/target-size screenshot test for chips and top bar.
3. `lint` baseline file with **0 new** warnings.
4. Merged-manifest + `dependencies` assertions (no INTERNET, no http client artefacts unless allow-listed).
5. R8 release smoke (install + 60-second scripted play).

---

## 9. What is genuinely good (keep it)

- Privacy fundamentals: no INTERNET, `allowBackup=false`, no telemetry SDK, redaction engine, bounded diagnostics.
- R8 release works and is tiny (3.3 MB/ABI) — no crash across scan/play/sheets in my smoke.
- Cohesive visual identity (OLED dark + electric cyan), tabular numerals, polished Subtitles/Audio sheets, thoughtful decoder-fallback + error taxonomy design, Up-Next overlay.
- Settings holds up at 1.8× font scale; tablet rail + 3-column grid work; lazy grid has stable `key` + `contentType`.
- Robust subtitle charset/zip/validation utilities and a large adversarial fixture corpus — a real asset once the E2E layer exists.

---

## Appendix A — Reproduction recipes (all on the Pixel 8 / API 35 emulator)

Corpus (ffmpeg): 4-min `long_720p.mp4` (testsrc2 + 440 Hz tone); `Show.S01E01..03.mkv` (45 s each) with matching `.srt`; `ac3_only.mkv`, `dts_only.mkv`. Pushed to `/sdcard/Movies/AuditMisc` and `/sdcard/Movies/AuditSeries` (removed afterwards).

| Finding | Steps |
|---|---|
| P0-01 | Play `long_720p`, don't touch; screenshot at +16 s — HUD present |
| P0-02/03 | While playing press Back → `dumpsys media_session` shows PLAYING, position increasing; `dumpsys activity services org.aptura.player` → `startForegroundCount=0`; `dumpsys notification` → nothing |
| P0-04 | Play 30 s, Back, reopen from grid → starts at 0; DB `playbackPositionMs` > 0 |
| P0-05 | Set 1.5× on a video; `run-as org.aptura.player cat databases/aptura_media.db` before/after tapping Refresh → `playbackSpeed` 1.5 → 1.0 |
| P0-06 | Details → Delete → "Delete Permanently"; `ls /sdcard/Movies/AuditMisc` → file present; Refresh → item returns |
| P0-07 | Series queue, tap Play-mode until `REPEAT_ALL`, tap scrubber at ~97 % → same episode restarts |
| P0-09 | Push a new mp4, reopen app → not listed until Refresh; fresh install + `pm grant` → "No videos found" |
| P0-10 | Open `Show.S01E01` from grid → toast "Auto-paired: S01e01"; Subtitles sheet lists 3 episodes, "Active: Off" |
| P1-01 | Settings → selected chips |
| P1-02 | `am start -a VIEW -d content://media/external/video/media/<id> -t video/x-matroska`; `cmd uimode night yes`/`no`; press Back 4× |
| P1-03 | `pm revoke … READ_MEDIA_VIDEO` + cold start → onboarding; tap *Skip* → no effect |
| P1-04 | Play, Home → no PiP; tap PiP button once; return, Home → PiP |
| P1-05 | Swipe vertically on left half; Back; `dumpsys window` → `sbrt=…` on the library window |
| P1-07 | Advanced → Sleep 15 m, change speed, reopen → "Off" |
| P1-10 | `wm size 540x960; wm density 240` → title "lo…" |
| P1-17 | `dumpsys gfxinfo org.aptura.player reset`, play 12 s, dump |
| P1-20 | `unzip -p app-universal-debug.apk classes15.dex | grep okhttp3/OkHttpClient`; `dumpsys package` permissions |

Evidence images are in `evidence/`. Environment was restored (font scale, display size/density, debug build reinstalled, test media removed). A signed copy of the release APK used for the R8 smoke test remains only in the session scratchpad.

## Appendix B — Quick wins (≤ ½ day each)
1. Stop calling `scheduleAutoHide()` from the position tick (P0-01).
2. `BackHandler { onPlayerDismissed(); pop() }` (P0-02).
3. Use `card.rawItem` in `onVideoClick` (P0-04).
4. Copy all user-owned columns in `FileReconciliationEngine` (P0-05).
5. Hide Delete/Audio-Only/Lip-sync/Contrast/Saturation/seek-step/countdown/background controls until wired (P0-06/08).
6. Remove `color =` from `ApturaTypography` styles (P1-01).
7. Move `Skip` below the pager in `OnboardingScreen` (P1-03).
8. Scan on cold start + register the periodic worker (P0-09).
9. Restore window brightness in `onDispose` (P1-05).
10. Fix `baseName` in `SidecarSubtitleDetector` to use the real file name (P0-10).

---

## Appendix C — Remediation log (same day, uncommitted working tree)

Scope: the ten Appendix B quick wins, then — on the instruction "fix and make it work rather than remove or hide it" — the P0-08 stub controls were **implemented**, not hidden. Evidence key for this appendix: **Unit** = automated test that fails when the behaviour is broken (red→green checked for the ones marked ↺), **Device** = re-verified on the Pixel 8 / API 35 emulator after the change.

| Finding | What changed | Unit | Device |
|---|---|---|---|
| P0-01 HUD never hides | Auto-hide re-armed only when playback *starts*, not on every 250 ms tick | ✔ ↺ (ticking fake) | ✔ HUD gone ≈5 s into playback |
| P0-02 Back keeps playing | Back pauses, saves position, pops (`handleBack`) | ✔ | ✔ session `PAUSED` after Back |
| P0-03 No foreground service / notification | `addSession()` in `ApturaPlaybackService`; queue + title metadata in the player | ✔ | ✔ `isForeground=true`, `foregroundId=1001`, `category=transport`, 3 actions, session title "Verify C", next/prev actions enabled |
| P0-04 Resume ignored from grid | Library's own items go to the queue; **plus** a second bug only a device showed: an early seek was clamped to the not-yet-known duration (= 0:00) | ✔ ↺ (real ExoPlayer) | ✔ saved 0:40 → reopened at 0:41 |
| P0-05 Scan wipes prefs / renames | Reconciliation copies every user-owned column; Room v5 migration (`customTitle`, `isHidden`) | ✔ real-SQL tests | ✔ positions survive restart, reinstall and cold-start scans |
| P0-06 Delete doesn't delete; Hide not persistent | `MediaDeleter` (file / SAF / MediaStore + consent), persistent hide with "Hidden videos" restore row, honest dialog copy | ✔ ↺ (6 of 9 tests go red when deletion is faked) | ✔ consent dialog → file gone from `/sdcard/Movies/Verify` → tile removed |
| P0-07 Repeat All = Repeat One, Shuffle no-op | Queue is the player's playlist; shuffle order starts at the playing item so one pass visits everything | ✔ ↺ (real ExoPlayer) | ✔ Repeat All A→C (wrap)→B |
| P0-08 stub controls | Audio delay (`AudioDelayProcessor` in the sink), contrast/saturation/night mode (colour matrix on `TextureView`), seek step, Up Next countdown, background-audio setting, Audio Only (video track disabled), real frame stepping (fps-derived, exact seek, no drift), hardware-only decoder strategy | ✔ each | ✘ not re-checked by eye on device (see "Not verified") |
| P0-09 No auto scan | Cold-start scan (KEEP policy), periodic worker registered, MediaStore `ContentObserver` (4 s debounce), real `WorkInfo`-driven spinner; WorkManager on-demand init (manifest initializer removed) | ✔ real WorkManager | ✔ files pushed while the app was closed appear on launch; a file pushed while open appears without Refresh |
| P0-10 Sidecar subtitles | Exact-name matching on the real file name; resolved off-main **before** `prepare()` and baked into the `MediaItem`; neighbours prefetched; default sidecar enabled and announced | ✔ | ✔ "Auto-paired: English", cue 1 at 0:03, cue 2 at 0:43 rendered (the "no cue text" item in P0-10 is resolved) |
| P1-01 baked colours in typography | Removed | ✔ contrast test | – |
| P1-03 Onboarding | Skip jumps to the permission page; completion persisted; not re-shown | ✔ | ✔ completed in limited mode, relaunch → library state, no onboarding |
| P1-05 Brightness leak | Restored on dispose | ✔ | – |
| (new) PiP only after manual use | Auto-enter PiP params set up front | ✔ | ✔ first Home press → `mode=pinned`, playback continues |

### Found while verifying (not in the original audit)
1. **Seek clamped to unknown duration** (fixed, above) — the resume fix was green in unit tests and still broken on a device; only the emulator exposed it.
2. **Missing files in the play queue** (fixed): the grid's queue included deleted-on-disk entries, so *Next* landed on a "Storage Inaccessible" error screen. `buildPlayQueueRequest` now leaves them out (clicking a Missing tile still opens it, to show the message).
3. **Play-mode button announced a raw enum** ("Play Mode: REPEAT_ALL"); now "Play mode: repeat all".
4. **Test-suite hygiene**: every Robolectric test used to boot the real `ApturaApplication`, which now starts WorkManager; leaked threads failed *unrelated* tests at random. Tests now default to a bare `PlainTestApplication`; only the cold-start test opts in.

### Not fixed / not verified (be careful before relying on these)
- **Storage path in Details is wrong** (new): it is guessed from the folder *name* — `/storage/emulated/0/Verify/Verify_D` for a file that is really `/storage/emulated/0/Movies/Verify/Verify_D.mp4` — and *Copy Path* copies the wrong string. Needs the real `RELATIVE_PATH` + display name (schema change).
- **Playback that fails mid-queue** (file removed after the queue was built) still stops on the error screen instead of skipping on.
- Not re-checked on the device by eye: audio-delay audibility, contrast/saturation/night-mode appearance, seek-step & countdown settings, Audio Only, sleep timer, brightness restore, VIEW-intent + night-mode back stack, hardware-only decoder choice, frame-step on real footage. They have unit/real-player tests but no device evidence.
- Untouched P1/P2 items: decoder per-video vs global precedence, scrubber semantics/48 dp targets, localisation, thumbnail cache, diagnostics persistence, natural sort/folder identity, manifest-invariant test, instrumented E2E suite.
- Nothing is committed. `tools/gate.ps1`: compile ✔, unit tests ✔ (279 run, 0 failed, 0 skipped — up from 206), lint ✔ with **0 errors** (2 errors the changes introduced — a missing API-29 guard and the WorkManager initializer — were fixed); the existing ~70 lint warnings are unchanged in kind.

---

## Appendix D — Second remediation round (device pass + trust fixes)

Same evidence key as Appendix C. Everything below is uncommitted until the commit that follows this round.

### Device pass (emulator, API 35) over the items Appendix C listed as "not verified"
| Item | Result |
|---|---|
| Seek-step setting | ✔ buttons become "Rewind/Forward 30s" and seek 30 s. The double-tap *gesture* itself could not be driven reliably through `adb` (two `input tap` processes are slower than the double-tap window), so it is covered by the Compose test only |
| Up Next countdown setting | ✔ set to 5 s, the countdown starts at 5 |
| Audio Only | ✔ panel shown, session keeps playing. **Found a real bug:** *Show video* could never be pressed — the full-screen gesture layer sat above the panel and swallowed every touch. Fixed (panel moved above the gesture layer); ↺ red/green with a real touch-injection Compose test; ✔ on device afterwards |
| Sleep timer | ✔ "Cancel (14:58 left)" and it keeps counting after a speed change (was reset to "Off") |
| Brightness restore | ✔ override `NaN` → `0.68` while swiping → `NaN` again after leaving the player |
| VIEW intent + night-mode ×2 | ✔ one Back reaches the library, the second leaves the app (was 3 + 1) |
| Contrast / saturation / night-mode *appearance* | ✘ still not checked by eye on a device |
| Audio-delay audibility, hardware-only decoder, frame-step on real footage | ✘ not checked on a device |

One unexplained episode: right after the emulator was cold-booted, the very first playback (started as Audio Only) showed a black picture after *Show video*, and stayed black for later videos in that process. Three later attempts, including the identical sequence on a fresh process, rendered correctly. I could not reproduce it and cannot say whether it is an app bug or a cold-emulator codec problem; flagging it rather than hiding it.

### Fixed this round
| Finding | Change | Unit | Device |
|---|---|---|---|
| P1-09 fabricated values | Details reads the real path from MediaStore (`RELATIVE_PATH` + file name; volume-relative for SD cards; "Unknown" instead of a guess; Copy disabled when unknown). Audio/subtitle fallbacks say "None found/embedded" only after the container was really read, otherwise "Unknown/Reading…". Overall bitrate is labelled as such. Stats no longer default to "Stereo / 48 kHz / video/mp4". Settings footer shows the installed package's version, and the "LGPL FFmpeg" credit (not in the build) is gone. Metadata inspection moved off the main thread | ✔ real-provider tests, real `PackageInfo` | ✔ `/storage/emulated/0/Movies/Verify/Verify_C.mp4`, "Overall Bitrate", "None embedded", "v1.0.0 (build 1)" |
| P2-22 storage receiver | Manifest `exported="false"`; runtime registration `RECEIVER_NOT_EXPORTED`; any action outside the five storage broadcasts is ignored before it is logged. (Correction to the original finding: those actions are protected broadcasts, so the practical exposure was log/diagnostic noise from explicit intents, not spoofed ejects.) | ✔ ↺ (merged-manifest check, spoof test) | ✔ registered in `dumpsys package` |
| P1-21 privacy messaging | Search dialog states that Aptura stays offline but the browser sends the text to the chosen site, and that file names can hold personal details; "(Recommended)" removed from the third-party default | ✔ | – |
| Mid-queue failure | A file that cannot be opened (not found / no permission / IO) is skipped with a "Skipped “X”: file unavailable" notice; bounded to queue-size − 1 so an all-broken queue (even in Repeat All) cannot loop; a single broken video still shows its error | ✔ ↺ real ExoPlayer erroring on real missing files | ✘ not triggered on the device (ExoPlayer had already opened the next file before I removed it, and a rescan then dropped it from the queue — which also demonstrates the earlier "missing files are not queued" fix) |
| P1-19 (part) | `exportSchema = true` with `schemas/…/5.json` committed; migration list exposed and a chain test fails if a schema bump has no migration; destructive fallback now only for the ancient v1 and for downgrades (a forgotten migration fails loudly instead of wiping history); `.github/workflows/ci.yml` runs compile, tests, lint and an R8 release build | ✔ ↺ | – (CI has not run: the repo has no remote) |
| (new) stale label | Frame stepper no longer says "~42 ms" | – | – |

### Still open
P1-06 decoder precedence · P1-10 top bar on 360 dp · P1-11 scrubber a11y · P1-12 localisation · P1-13 thumbnail cache · P1-14 library model · P1-15 remainder (`onCleared` persistence) · P1-16 audio-effects release/persistence · P1-17 performance · P1-18 diagnostics persistence/crash handler · P1-20 merged-manifest/dependency invariants · P1-22 audio codec coverage · P1-23 architecture · most P2 items · the unexplained black-picture episode above.

---

## Appendix E — Data-safety round (P1-15, P1-16, P1-06)

Evidence key as before. Gate: 321 unit tests, 0 failures, lint 0 errors.

### P1-15 · Position persistence
| Defect (reproduced by a test that failed first) | Fix | Device |
|---|---|---|
| The last save of a session was launched from `onCleared()`, but Android cancels `viewModelScope` *before* it calls `onCleared()` — so the final stretch (up to the checkpoint interval) was dropped. Reproduced with a real `ViewModelStore.clear()`: saved 42 000 ms instead of 44 000 ms | Saves run in a scope that outlives the ViewModel | – |
| Pausing from anywhere except the app's own buttons (notification, headset, sleep timer) saved nothing: 30 000 saved, 32 500 actual | Any playing → paused transition saves immediately | ✔ media-key pause at 11 599 ms, process killed, **11 599 ms** in the database |
| Moving to the next video lost the last 3 s of the one being left | The old video's position is saved before the state switches; the checkpoint baseline resets per video | – |
| Nothing saved on `ON_STOP` | `persistProgress()` on `ON_STOP` | – |

### P1-16 · Audio effects
- **Leak (the audit's main point):** a new `AudioEnhancementController` was built per Activity recreation and never released. The ViewModel now owns it, binds it to the player's *current* audio session (it follows `onAudioSessionIdChanged` instead of binding once) and releases it when cleared. ✔ device: leaving the app removes the 4 effect instances (`dumpsys media.audio_flinger`).
- **Persisted:** enabled / preset / per-band levels / bass / virtualizer / loudness are stored in DataStore (debounced; the last tweak is also written on clear) and restored before the first sound. ✔ device: Rock preset survives a process kill and is applied to the new session.
- **"Flat" over +3 dB bands:** the controller's own state is now authoritative; hardware starting values never override it, and the preset label is derived from the actual levels. ↺
- **One refused band aborted the rest:** each effect and each band is applied in isolation. ↺
- **Silent sliders / presets with the master switch off:** touching any control now turns the master on. ✔ device: choosing Rock with the switch off → "DSP Effects Active", bands +4/+2/−1/+2/+4 dB.
- **Loudness top end** lowered from +12 dB to +6 dB (the platform enhancer applies gain without knowing the content's headroom). Judgement call, not measured.
- Test seam: the AudioFX classes sit behind a small `AudioEffectsBackend`; controller tests run against a recording stand-in (the audio HAL cannot run in a unit test). The real effects were exercised on the emulator only.

### P1-06 · Decoder precedence
- **Model:** a video's decoder is now either *its own choice* or *"follow the Settings default"*. Previously both were the same stored `0`, so a library video forced Auto over a global Hardware default. Room schema v6 adds `decoderOverridden`; migration 5→6 keeps any non-zero stored value as a choice and turns every 0 into "follow the default". ✔ **migrated in place on the emulator** (data kept, `user_version` 6); ↺ real-SQL migration test from v4 through all migrations.
- **Leaks removed:** choosing a decoder in the player no longer overwrites the global default; a Settings change no longer overwrites the current video's saved choice; the automatic software fallback is no longer frozen into the video's saved preferences by the next unrelated change (e.g. aspect ratio).
- **UI copy:** Settings "Default Decoder" says that videos changed in the player keep their own choice; the player sheet header reads "Default: …" or "This video: …".
- ✔ device: default Hardware → a video with no choice shows "Default: Hardware"; choosing Software for one video shows "This video: Software" and the Settings default stays Hardware.

### Still open
P1-10 · P1-11 · P1-12 · P1-13 · P1-14 · P1-17 · P1-18 · P1-20 · P1-22 · P1-23, most P2 items, and the unexplained black-picture episode (Appendix D). Not checked on a device: audible effect of the equalizer (only that the effects exist, are enabled and carry our levels).

---

## Appendix F — Library and accessibility round (P1-14 part, P1-10, P1-11)

Gate: 354 unit tests, 0 failures, lint 0 errors.

### Library
| Finding | Change | Unit | Device |
|---|---|---|---|
| Titles sorted by `lowercase()`, so `E10` came before `E2` | `NaturalOrder`: digit runs compare by value, text ignores case/accents, always a total order (no reshuffling between equal refreshes). Every sort falls back to it, so items that tie (a season downloaded in one second) stay readable | ✔ 7 cases incl. 20-digit numbers | ✔ |
| Default newest-first listed episodes E03, E02, E01 and left "Next" disabled on E01 | **Automatic sort:** newest-first on the home view, *episode order inside a folder*; a sort the user picks is kept everywhere. The player queue is the visible list, so Next follows episode order | ✔ ↺ (fails on the old code) | ✔ folder "Verify" opens A, B, C |
| Only 2 sort modes | Recently Added · **Recently Played** · Title (A–Z, natural) · **Longest** · **Largest** (needs the new `lastPlayedTimestamp` on the domain item) | ✔ | ✔ all five in the menu |
| No "Continue watching" | A shelf above the grid on the unfiltered home view: started (≥ 10 s), not finished (< 95 %, not marked completed), still on disk, most recently played first, max 10; "N min left"; tapping resumes with the library's own item (so the saved position travels); hidden while searching or inside a folder | ✔ view-model + real Compose click | ✔ appears after watching, "Resume Verify C, 2 min left" |

### Player
| Finding | Change | Unit | Device |
|---|---|---|---|
| **P1-11** seek bar is an unlabelled `Canvas` (TalkBack cannot use it) | Semantics: "Seek bar", state "0:30 of 2:00", `ProgressBarRangeInfo`, `SetProgress` (clamped to the video), custom actions "Rewind/Forward N seconds" (the seek-step setting). Height 36 → 48 dp | ✔ ↺ | ✔ node is exposed 126 px (48 dp) tall |
| **P1-10** top bar: five controls + chips squeezed the title ("lo…") | Below 560 dp wide: ← title (2 lines) · queue chip · ⋮ menu (speed, aspect, orientation, PiP; each row 48 dp). A speed other than 1.0× gets its own chip line under the title so it is not forgotten. At tablet width everything stays inline | ✔ ↺ (title ≥ 40 % of width at 360 dp, nothing cut off) | ✔ title 214 dp of 411; menu rows 48 dp apart; "Speed 1.5x" line |
| 40 dp controls | Orientation / PiP / aspect (were 40), speed and queue chips, transport skip/seek buttons (were 36–46 on narrow windows), library card ⋮ (was 40) are now ≥ 48 dp; verified from the semantics node's real touch bounds | ✔ ↺ | – |

Not done / not verified
- **TalkBack itself was not run** (no screen-reader session on the emulator): the semantics are asserted on the tree TalkBack reads, and the node is present on the device, but how it *sounds* is unverified. The two time labels beside the bar are still read separately ("0:30", the bar, "1:30"), which is a little noisy.
- The folder identity is still the bucket's *display name* (two folders with the same name merge) and the SAF scan still re-reads every file (P1-14/P2-23 remainder); no folder hierarchy view, no multi-select.
- Other 40 dp targets may remain in sheets and Settings (only the player and the library home were audited this round).


---

## Appendix G — Diagnostics persistence and invariants round (P1-18, P1-20)

Gate: 410 unit tests, 0 failures, lint 0 errors, `assembleRelease` (which now also runs `verifyReleaseApk`) passes.

### P1-18 · Diagnostics that can diagnose
| Finding | Change | Unit | Device |
|---|---|---|---|
| Events lived only in memory, so a report written after a crash said "No errors logged" about the session that crashed | `DiagnosticStore`: bounded on-device log `files/diagnostics/events.jsonl` (≤ 2 MB, last 5 sessions, 30 days, payloads redacted **before** they are written, a write cut off by a crash costs only that line). Restored into the ring buffer at start | ✔ 11 against real files (round trip, no raw path/e-mail/IP on disk, size cap keeps newest, retention, 5 sessions, torn write, unwritable store) | ✔ second launch: `restoredEvents: 2`, `previousExit: crash` |
| No crash capture at all | `CrashHandler` writes a plain-text report (class names and frames only — **never the exception message**, which routinely contains file names) and an `EV_018` event before delegating to Android's own handler; keeps the last 3; cannot throw or recurse | ✔ 5 (no message, cause chain, truncation, delegate still runs, storage failure harmless) | ✔ `adb shell am crash` produced `crash-<ms>.txt` |
| Export hard-coded build info ("aptura-part63") and a constant session id | Version, version code, build type and the git revision (a generated string resource) are read from the installed package; one rotating session id per process; Android 11+ `ApplicationExitInfo` adds *why* the last process ended (crash / ANR / native / low memory) | ✔ | ✔ report header `1.0.0 (1) debug dd35cc5b` |
| Export had no crash section | New "Crash Log" section; error history includes crashes | ✔ crash appears in the next run's export instead of "No errors logged" | – |
| No post-crash prompt, no way to delete diagnostics (spec/14 §2) | Quiet card on the library after a crash ("Share details" opens the user's own share sheet with the redacted report; "Not now" stops asking, report is kept). Settings → **Delete Diagnostic Data** with a confirmation | ✔ 6 Compose tests on the real `ApturaDiagnostics` and real files (delete test is red when the handler is a no-op) | ✔ card shown once, gone after "Not now" + relaunch; Delete leaves `files/diagnostics` empty |

### P1-20 · Invariants that check what ships
| Finding | Change | Unit | Build |
|---|---|---|---|
| Manifest tests read `src/main/AndroidManifest.xml`, which shows 4 of the 8 permissions the shipped app requests | `MergedManifestInvariantsTest` parses the **merged release manifest**: permissions must equal an allow-list with a reason each (the library-added ones are `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED` and AndroidX's signature permission); exactly 4 components are exported (MainActivity and three library components each gated by `BIND_JOB_SERVICE`/`DUMP`); backup off, not debuggable, no cleartext, no `<queries>` packages | ✔ 12 (4 on the real manifest, 8 fixtures that prove each rule fires) | ✔ red when INTERNET is added (3 tests fail) |
| "No telemetry SDK" was a substring search of `build.gradle.kts` — blind to transitive libraries, and any line containing "adjust" would have failed it | `DependencyInvariantsTest` reads the **resolved release runtime graph** (210 libraries, written by a new `writeReleaseRuntimeDependencies` task): no telemetry, crash-reporting or ad SDK and no HTTP client stack, matched on group/artifact boundaries (`adjust` no longer false-positives); exceptions must be reasoned, bounded to a parent library and not stale; telemetry Gradle plugins are refused | ✔ 12 (5 on the real graph, 7 fixtures) | ✔ red when the exception list is broken |
| Nothing checked what R8 actually ships | `verifyReleaseApk` (finalizes `assembleRelease`, so CI and the gate run it): R8's mapping is read for kept classes in forbidden packages — R8 renames classes (`okhttp3.internal.http.HttpHeaders` is `xb.e`), so searching the dex for names finds almost nothing — plus a dex text search as a second net; it refuses to pass on an empty mapping or an APK without dex | – | ✔ **red on the first run, green after the fix below** |

**Correction.** Earlier in this work I took it for granted that R8 strips the OkHttp that Coil 2.7 brings in, and an interim note said the release dex had none. That was wrong: the new check found **119 retained `okhttp3` classes in the release APK** (`OkHttpClient`, `RealCall`, `RealConnection`, `ConnectionPool` …) plus Coil's `HttpUriFetcher`. They could not connect (no `INTERNET` permission), but "no HTTP clients may exist" was false for the shipped app.

**Fix, not an excuse.** Coil is now 3.3.0 with **no `coil-network-*` artifact** (`io.coil-kt.coil3:coil-compose` and `coil-video`). Touch points: `ApturaApplication` (`SingletonImageLoader.Factory`), the thumbnail-cache clear in `Navigation`, and the two `AsyncImage` call sites. The release graph now has no OkHttp at all, `approvedExceptions` is empty, and `verifyReleaseApk` scans 5,643 kept classes clean. ✔ device: grid and Continue-watching thumbnails render exactly as before; "Clear" tapped without error.

### Not done / not verified
- The crash card appears on the library's **Success** state only; with an empty or still-loading library it waits.
- Native crashes and ANRs leave no stack (only the next launch's `previousExit` reason); the Java handler cannot see them.
- The report's build hash is `HEAD` when the app was built; uncommitted changes are not reflected in it.
- Media3's own `HttpURLConnection`-based data source still ships (part of ExoPlayer, inert without `INTERNET`); it is not asserted either way.
- Three library components stay exported on purpose (WorkManager's job service, WorkManager's and the profile installer's diagnostics receivers); the test pins their permission gates but does not question the libraries.
- Thumbnails were compared by eye, not pixel-diffed against Coil 2; the thumbnail disk cache holds ~0 B for local video, so "Clear" mostly empties `cacheDir`.
- The delete flow was confirmed on the device against another session's redesigned Settings screen (the emulator was shared); the unit test covers the screen present in this commit.
- `gate.ps1` is about a minute longer: it now builds the release APK.

### Still open
P1-12 · P1-13 · P1-14 (remainder) · P1-17 · P1-22 · P1-23, most P2 items, and the unexplained black-picture episode (Appendix D).


---

## Appendix H — Settings screen and welcome guide round (user-reported; follows P1-01, P1-03, P1-21)

Gate at `a47ae25`: 417 unit tests, lint 0 errors, `assembleRelease` with `verifyReleaseApk` passes. After the correction below (one sentence, one fact, one assertion) the unit suite was re-run: **417 tests, 0 failures**; lint and the release build were not re-run for that change.

### Settings screen (`b7fdf6a`)
The request was "plain, dull, awful, redesign it". The first version was over-decorated and the user called it AI slop; this appendix records both because the second pass is the one worth keeping.

| What | Change | Why it stays |
|---|---|---|
| Structure | A large title that collapses into a compact bar; a library readout (videos, total time, footprint); flat panels of related settings with hairline edges | Each part carries information or groups settings |
| Option groups | A **selector switch** replaces the chips: recessed track, cyan thumb that slides on a spring. Every option is a real radio button for TalkBack (announces selected / not selected), 48 dp tall, with haptics. A stored value that is not among the options selects nothing instead of crashing | Functional improvement, not decoration |
| Colour | Cyan only for "selected / interactive", red only for actions that destroy something (Delete Diagnostic Data, Clear, remove folder), green only for the single offline statement | Colour tells you something |
| Also | Settings → **Delete Diagnostic Data** (from Appendix G) lives here; the cache-size walk moved off the main thread; "remove folder" is a 48 dp target | |

**What the slop pass removed** (each was decoration with no job): a lens-dial header that rotated as you scrolled, glow and halo effects, a marketing tagline, the offline guarantee stated four times (status pill, panel title, subtitle, three check chips), "01 / 02 / 03" section numbers, a staggered entrance animation on every open, and a cyan icon tile on every row. It contradicted `DESIGN.md`'s own rule that every element is deliberate; the dial was the clearest case, because it could not do anything except duplicate the selector.

| Evidence | |
|---|---|
| Unit | `SettingsChipContrastTest` (6): the selected label is ≥ 4.5:1 on the real thumb and the unselected label on the real track, selected/unselected are announced as radio buttons, tapping an option reports it, an out-of-range stored value selects nothing |
| Device | Tapped 1.5x, left Settings, re-entered: still 1.5x (real DataStore); the title bar scrim hides content scrolled under it |

### Welcome guide (`a47ae25`, corrected in the next commit)
Two reports: "it doesn't show when we open the app after installation" and "it is missing many aspects / not updated for the app".

| Finding | Cause | Change |
|---|---|---|
| Never shown on first launch when storage access was already granted | `startDestinationFor` was "guide only if access is **not** granted **and** not completed", and "completed" was only recorded inside the permission flow, so a reinstall or any device that pre-grants access skipped it forever and only Settings could reach it (spec/06 says first run) | The first launch always shows it; it depends only on whether it was **seen**. Reaching the last page counts as seen (closing the app there does not bring it back) |
| Three pages of icons and adjectives; nothing about gestures, subtitles, sound, A-B repeat, bookmarks, sleep timer, queue, PiP, night mode | Written before most of the player existed | Five scannable, skippable pages: privacy facts; the library (a miniature of the real layout, Continue Watching, the ⋮ menu); a **map of the player's touch zones** plus hold for 2×, swipe to seek, two-finger tap, Lock; a grid of **12 tools**, each checked against the code; access |
| The last page asked for permission even when it was already granted | – | Without access: why, what it can do, alternatives, **Grant** or **Limited Mode**. With access: "You're all set", how to reopen the guide, **Start watching** |
| Replay stacked a second library | `finish` always pushed `MediaLibrary` | Replay returns to Settings; first run goes on to the library |

Also: Back steps to the previous page; the page indicator is spoken as "Page n of 5"; each illustration is one sentence for TalkBack (its fragments are hidden); scrolled content fades behind the controls and the status bar; Settings row renamed **Welcome Guide**; `spec/06` updated (it said "3 pages max").

**Correction found while writing this appendix.** Page 1 originally said "nothing can be uploaded, tracked or sold". That is the same unqualified offline claim P1-21 (Appendix D) removed from the subtitle search: **Search Subtitles on Web hands an editable title to the user's browser**, which sends it to a third-party site. Page 1 now says the *app itself* cannot upload, track or sell anything, gains a fifth fact ("Subtitle search is yours to start … the video itself never leaves your device"), and the Subtitles tile mentions the browser search. A test pins the fact.

| Evidence | |
|---|---|
| Unit | `StartDestinationGateTest` (3): decided only after settings load; first launch → guide; depends only on "seen", not on access. `OnboardingScreenUiTest` (9): Skip and Continue with real pointer events, Back, the indicator, every page's content, the tools list (unique, no blanks, even count), the last page with access (Start watching only) and without (Grant / Limited Mode only), the seen signal fires exactly once |
| Device | **Access pre-granted** (`pm clear` + `pm grant`, the broken case): guide shows, five pages, "Start watching". **No access**: Skip → "Access Your Library", Grant and Limited Mode. **Grant**: system dialog → "Allow all" → library with thumbnails. **Replay** from Settings → Skip → Start watching → back on Settings |

### Not done / not verified
- **TalkBack itself was not run** on either screen; the semantics are asserted on the tree it reads.
- **Layouts beyond a phone were not looked at** (tablet, foldable, landscape) for Settings or the guide; both cap their width (600 / 560 dp) but I did not see the result.
- The guide's **content comes from a code survey**: every listed tool and gesture exists, but I did not open each one in the player, and "tap the screen to show the controls and menus" is deliberately generic about where each tool lives.
- **Existing installs** whose "seen" flag was never set will see the guide once.
- **First launch from "Open with"** (a video opened from a file manager before the guide was ever seen): the guide sits under the player, so Back from the player lands on it. Untested; probably acceptable.
- The countdown option "Immediate" became "Instant" so five options fit a 360 dp row.
- The launcher icon and splash are still the default green Android robot. Not touched.

### Still open
The player's floating **speed pill** (it reads as a stray element under the title bar; suggested fix: a "1.5x" suffix on the title line that still opens the speed sheet) awaits a decision. Otherwise unchanged from Appendix G: P1-12 · P1-13 · P1-14 (remainder) · P1-17 · P1-22 · P1-23, most P2 items, and the unexplained black-picture episode.
