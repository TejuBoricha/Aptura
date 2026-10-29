# Aptura on-device test findings (running log)

Started 2026-10-29. Written while testing, updated as items finish.

## Test setup

- Phone: Samsung Galaxy S24 (SM-S921B), Android 17 (API 37), serial (serial omitted). Display density override 420 dpi
  (physical 480), so 1 dp = 2.625 px and 48 dp = 126 px.
- App: `org.aptura.player` 1.0.0 (versionCode 1), **release** build from this checkout (`assembleRelease`, signed with the
  local "Aptura Smoke Key"; `verifyReleaseApk` passed). The checkout is on `agent/opencode/14-silence-detection`, whose
  changes are agent tooling only, so the app code is the same as `main`.
- Method: adb. Screenshots, `uiautomator dump` (only works while the video is paused), `dumpsys media_session` /
  `media.audio_flinger` (track gain, format, underruns). **Nothing was judged by ear**, and no Bluetooth headphones,
  TalkBack speech or HDR panel judgement is involved unless stated.
- Test media made with ffmpeg and stored on the phone in `Movies/AptureTest/`.

## Results by checklist item

### Sleep timer (FR-CTRL-009)

| Item | Result | Evidence |
|---|---|---|
| 10 minutes | PASS | Sheet "Stops in 9:58" + "The sound fades out over the last 10 seconds". HUD "Sleep in m:ss" with moon icon. Countdown matched wall clock exactly (17:47 at 11:50:14 for a 12:08:01 deadline) across control hide/show, a video restart and a rotation to portrait. |
| Fade | PASS | Track gain from audio_flinger: 1.0, 0.9, 0.8 ... 0.1 (0 to -20 dB), one step per ~1 s starting ~9.1 s before the end; pause at 0:00 and gain reset to 1.0. |
| Next play at full volume | PASS | After `+5 min` the track gain was 0 dB. |
| Custom | PASS | Hint "1 to 720 minutes". `0` and `721` leave **Start timer** disabled; `1` enables it. |
| Extend after firing | PASS (HUD wording not seen) | Sheet reads "Sleep timer ended / Playback is paused. Add five more minutes, or pick a new time." `+5 min` resumed playback with a fresh "Stops in 4:58". The HUD text `Sleep ended · +5 min` was **not** captured. |
| Pressing play dismisses the offer, no new timer | PASS | Used the play media key (not the on-screen button). No "Sleep in" HUD afterwards. |
| Extend while running | PASS | About 4:48 left became 9:48. |
| Cancel during fade restores volume | PASS | Tapped Off at gain 0.7; gain was 1.0 again 0.2 s later and playback continued. |
| At the end of this video + Repeat one | PASS | 60 s clip, Repeat one ("repeat this video") + "At the end of this video" on, seeked to 57 s, played. It reached 1:00.178, paused there, and stayed paused for 20 s: no loop, no move to the next video. |
| Background audio, app in the background (screen ON) | PASS | 1-minute timer, pressed Home (PiP denied, see below), audio kept playing, fade started ~8 s before the end, paused at 60 s, gain reset to 1.0. Note: with PiP allowed, Home puts the player into a Picture-in-Picture window, so my first attempt was really a PiP run. |
| Background audio, screen OFF | NOT YET RUN (will lock the phone) | |
| Process death with a timer | **FAIL (as the checklist predicted)** | 3-minute timer, playing, Home, swiped Aptura out of recents at 12:41:11. Process stayed alive; `ApturaPlaybackService` kept running as a media-playback foreground service and audio kept playing at full volume. The 12:43:47 deadline passed with no fade and no pause; audio was still playing at 12:44:22 (35 s late). The timer is lost with the activity while playback continues indefinitely. |

### Audio and subtitle sync

| Item | Result | Evidence |
|---|---|---|
| Drag is silent | PASS (not judged by ear) | Raw-touch drag while playing: label followed the finger immediately (-100, +100, +400, +650 ms) and stayed at +650 after release. audio_flinger underruns stayed 0; playback never paused. The code applies the value after a 200 ms debounce. |
| Persistence | PASS (via the library) | Offsets are per video: sleep-test +250 ms, other-test +150 ms stayed separate across leave/return and switching. Both audio and subtitle offsets survived a force-stop and relaunch. "Reset to defaults" (Playback tools, For troubleshooting) set audio and subtitle offsets back to 0. |
| "A pending drag does not reappear" after reset | NOT TESTABLE | Reset to defaults is in a different sheet from the sync sliders, so it cannot be tapped inside the 200 ms window. Code reading: reset cancels both pending jobs. |
| Subtitle sync | PASS | The sidecar `sleep-test.srt` was picked up automatically ("Showing: sleep-test.srt", EXTERNAL). With +1.95 s no cue at 6.2 s and "CUE 1" at 8.3 s; with -1.95 s "CUE 2 at 0:10" visible at 9.16 s. Value survived a restart. |
| PCM formats (F8) | PASS for "no crash"; offset audibility NOT TESTABLE | pcm24 (mono 24-bit), pcmf32 (mono float), pcm51 (5.1, 16-bit): all play, 0 crashes, 0 underruns, stats show "A/V Sync Offset +200 ms". Stats show codec `raw`, channels and 48000 Hz only. audio_flinger received **16-bit PCM** (format 0x1) for all three. |
| Bluetooth hint (F7) | NOT TESTABLE | No Bluetooth headphones available. |

### Accessibility and appearance

| Item | Result | Evidence |
|---|---|---|
| Hold-to-repeat | PASS | One tap = exactly one 50 ms step. Three 450 ms holds = exactly one step each (so release adds none). A 1 s hold stepped 6 times and stopped; nothing 1.5 s later. |
| Font scale 200% | PASS | Sleep chips wrap onto 4 rows with no clipping. Each chip's tappable area is 126 px = 48 dp; smallest tappable control in the sheet 126x127 px. Font scale restored to 1.0 afterwards. |
| Sheet tabs / chips announce selected state | PARTIAL | Accessibility tree: "Tracks" tab `selected=true`; "Off" chip `checked=true` when no timer; track row `checked=true`. "tab N of M" is not visible in a UI dump (needs TalkBack). |
| TalkBack on ApturaSlider | NOT TESTED YET | Slider exposes as SeekBar with labels "Audio sync" / "Subtitle timing"; step buttons labelled "-50 ms, Sound earlier". |
| Light theme | PASS | Theme set to Light (library turned light, brightness 171/255). Player controls (8), Playback tools (35), Picture tab (38), Audio sheet (37) with Equalizer (33) and Effects (34) tabs, Subtitles sheet (41) with Style (36) and Timing (37) tabs, and the Custom sleep timer dialog (31) all stayed dark. Not confirmed: the top-right ⋮ menu (my tap may not have opened it). Theme restored to System. |

### Hardware paths

| Item | Result | Evidence |
|---|---|---|
| HDR playback | PARTIAL PASS | HDR10 test clip (HEVC Main10, BT.2020/PQ, mastering metadata, 1080p) played with 0 crashes, 0 dropped frames. Stats overlay: **"HDR10 (PQ)"**, "Hardware Accelerated", `c2.exynos.hevc.decoder`, `hvc1.2.4.L120.90`. NOT judged: tone-mapping quality / panel HDR brightness (needs eyes), and the compositor dump did not show a clear BT.2020/PQ layer dataspace, so I cannot say the panel switched to HDR mode. |
| 30-minute 4K thermal run | PASS (no throttling; no A/V desync measurable) | 3840x2160 HEVC looped for 30.2 min (12:55:06 to 13:25:19) with the phone on USB power. 0 dropped frames (0.0%) in all seven stats crops, 0 audio underruns in all 61 samples, thermal status 0 (None) throughout. Live HAL temperatures (13:11:33 to 13:27:12): AP 39.5 to 42.2, CP/PA 38.4 to 40.4, skin 37.4 to 38.1, battery 36.2 to 36.8, USB 34.8 to 35.1 degrees C. Playback clock drift 0 to -401 ms over 30 min (about -13 ms/min, loop restarts); A/V desync itself is not measurable over adb. |
| PiP unavailable | CANNOT BE TESTED FULLY | The S24 supports PiP. With the PiP app-op denied for Aptura: Home no longer made a PiP window (normal background), and tapping "Picture in picture" in More options did nothing (no crash, no message). The menu entry is still shown, so "PiP button absent" is not met by this approximation. Note: with PiP allowed, pressing Home during playback auto-enters PiP. |
| Kill the app during a large SAF copy | NOT YET RUN | Built a 2.5 GB valid MP4 (`bigtest.mp4`) to move with the library's "Move to folder" action. |

## Things worth a look (possible defects or oddities)

1. **Playback stats "Audio Decoder" is stale for raw PCM**: it showed `c2.android.aac.decoder` while playing PCM files
   (left over from the previous AAC video). Also no bit depth or sample format is shown, only "raw" + channels + rate.
2. **No chip is marked selected while a sleep timer is running** (only "Off" is checked when no timer). The countdown
   text carries the state, but a screen reader user hears no selected preset. May be by design.
3. **Offsets set on a video opened from outside the app are lost** when the library has no row for it. Seen on first run
   before granting media access: I set +650 ms on a clip opened by a VIEW intent; after leaving and reopening it was 0.
   After granting access and opening from the library the same flow persisted correctly.
4. **After a configuration change (font scale), a paused video came back playing** (position ~3 s). Seen once.
5. Stats overlay text wraps mid-word at 200% font scale ("c2.exynos.h264.de / coder"). Cosmetic.
6. The Back key sent over adb did not close the Playback tools sheet. Unconfirmed; may be an adb artifact.

7. **(High) Sleep timer is lost when the app task is removed, but playback keeps going** (see Process death above).
   A user who sets a timer and swipes the app away keeps hearing audio all night. The timer deadline should live in
   `ApturaPlaybackService`, which already outlives the activity.
8. Tapping **Picture in picture** when PiP is not allowed is a silent no-op (menu entry stays, nothing happens, no
   message). Either hide or disable the entry, or say why nothing happened. (Tested by denying the PiP app-op; this phone
   supports PiP, so the real "feature absent" case could not be tested.)
9. After a clip ends, the "play in order" queue moved on to the next library item on its own (queue chip showed 557 =
   the whole library). Opening one video from the library queues everything. Worth confirming that is intended.
10. On a first run, the in-app Back arrow from a video opened by another app (VIEW intent) dropped me into the welcome
    flow, not back to the calling app. Possibly expected; worth a check for the "Open with" path.

## Improvement ideas (noticed while testing; not defects)

Product and UX
- **Stats overlay**: show the real audio format (encoding, bit depth, sample rate, channel layout, and what is sent to the
  AudioTrack), and clear the decoder row for raw PCM instead of keeping the previous video's. For the PCM checks the
  checklist asks testers to "record the stream format shown by the playback stats", and today it only shows "raw".
- **Stats overlay at large font sizes**: rows wrap mid-word ("c2.exynos.h264.de / coder"). Allow the value column to
  ellipsize or stack under the label.
- **Sleep chips**: show which preset is running (or a "Custom 1 min" chip) while a timer counts down. Today no chip is
  checked once a timer starts, so neither sighted nor screen-reader users can tell which choice is active.
- **Hold-to-repeat** on the sync buttons has no acceleration: moving the subtitle offset by about 2 s took a 4 s hold.
  Speeding up after a few repeats (or a bigger step on long holds) would help.
- **Persist offsets for videos opened from outside the library**, or say they are not being saved. Today the sync UI
  accepts the change and silently drops it.
- **Config changes**: changing the font size mid-playback recreated the screen and a paused video came back playing.
  Keep the paused state across recreation.
- A "PiP not available" message (see item 8) and an option to turn off auto-PiP on Home, if there is none (I did not
  look in Settings).

Testability (would make on-device and instrumented tests cheaper)
- **Expose test tags as resource ids** (`Modifier.semantics { testTagsAsResourceId = true }` at the root). Every Compose
  node showed an empty `resource-id` in UI dumps, so tests and tools have to find controls by visible text or position.
- The accessibility tree has no "tab N of M" or slider range information I could read from a dump; if that is provided
  through semantics, a small instrumented test asserting it would replace the manual TalkBack check.
- Keep a short `docs/DEVICE_CHECKLIST.md` preface: use a release build signed with the smoke key, complete the welcome
  flow and grant "Allow all" **before** the persistence check, and note that the phone's display-size setting changes
  dp-to-pixel maths (here 420 dpi override, 1 dp = 2.625 px).
- If useful, the adb helper scripts I wrote could be kept under `tools/` (UI dump + tap helpers, track-gain sampler).

## Changes I made on the phone (to undo at the end)

- Installed the release build; completed the first-run welcome flow; granted "Allow all" photos and videos access.
- Test files in `Movies/AptureTest/` (sleep-test.mp4/.srt, other-test.mp4, pcm24/pcmf32/pcm51.mkv, hdr10.mp4, uhd-sdr.mp4).
- Playback stats overlay switched on in the player (turn off). Saved per-video offsets on the test files.
- Font scale: changed to 2.0 and restored to 1.0.

## Thermal run data notes (read before using the CSV)

- Run: 12:55:06 to about 13:25:06 local, `uhd-sdr` (3840x2160, 30 fps, HEVC, hardware decode) looping with Repeat one,
  stats overlay on, screen on. **The phone was plugged into USB and charging the whole time** (`USB powered: true`,
  status 2, battery 94% to 100%; it hit full at 13:16:40 and reported status 5 after that). That adds heat and means
  this is not an on-battery thermal test.
- **`ap_c` and `skin_c` in `thermal-run.csv` are wrong.** The sampler read the "Cached temperatures" section of
  `dumpsys thermalservice` (constant 47.5 and 37.8 in every row). The repo copy of `tools/device/thermal-run.ps1` is
  fixed to read "Current temperatures from HAL"; the copy that produced the CSV was not.
- Valid columns: `state`, `pos_ms`, `expected_pos_ms`, `drift_ms`, `thermal_status`, `battery_c`, `battery_pct`,
  `underruns`, `cpu_big_khz` (one core, one sample, so sparse).
- A second logger, `thermal-live.csv` (same temporary folder as `thermal-run.csv`), records the live HAL temperatures
  every 30 s from 13:11:33 to about 13:27: AP, CP, PA, SKIN, BAT, USB and the thermal status. First rows: AP 39.6 to
  39.9, SKIN 37.4 to 37.5, BAT 36.2 to 36.3 degrees C, status 0. Idle reading before the run: AP 38.9 degrees C.
  Skin throttling thresholds on this phone (from `dumpsys thermalservice`): 38, 40, 42, 45, 47, 60, 90 degrees C.
  Take one more live reading right after the run ends, while the phone is still hot.
- `drift_ms` (real position against the wall clock) went from -50 ms at 30 s to about -250 ms after 15 min, roughly
  -13 ms per minute. The clip loops every 60 s, so loop-restart gaps are the likely cause. It is not an audio/video
  desync measurement (adb cannot measure that).
- The media session `position` shown by `dumpsys media_session` is a base value; clients add
  `(now - updated) * speed`. A value that looks frozen (3 ms for 90 s on the 4K clip) is normal, not a defect.

### Final result (opencode)

- The run completed cleanly: 61/61 samples `PLAYING`, `DONE 13:25:19`; the live logger wrote `DONE` at 13:27.
- Temperatures never reached the first skin threshold for long: skin touched 38.1 degrees C twice (13:15:34 and
  13:25:06), AP peaked at 42.2 at 13:15:34, and the thermal status stayed 0 the whole time. No throttle event, no
  frame drops, no underruns.
- The post-run reading at 13:27:12, while the phone was still hot, was AP 39.7, CP 38.6, PA 38.7, skin 37.6,
  battery 36.5, USB 35.0, status 0, battery 100% (status 5, full).
- Battery temperature rose from 34.1 to 36.8 degrees C over the run; that is mostly charging plus decoding, so it
  cannot be split between the two with this data.
- Limitation: the screen was on at a fixed brightness on USB power, and the clip is a 30 fps test pattern rather than
  a high-bitrate movie. A hotter, on-battery, higher-bitrate run could still throttle; this run did not.

## Observations added after the first version of this file

1. The play-mode setting persists across restarts (Repeat one was still on after the app was killed and relaunched);
   the Playback stats switch does not (it resets on restart).
2. HDR render: the yellow bar of the test pattern looked orange in the screenshot. This is a BT.2020 clip with
   SDR-level content, so a colour conversion is plausible; not judged.
3. After the task-removal test, the audio was paused by something I did not do at about 12:44:3x (the owner may have).
   No crash.
4. One loose tap pattern (`^More`) opened the ⋮ menu of a personal video in the library. I closed it with Back without
   choosing anything; the library was unchanged and no dialog appeared. Taps are by exact label from then on.
5. Another app has its own media session on this phone. My play key was routed to Aptura (the media-button session at
   that moment), and the other session's state was PAUSED afterwards. One earlier read showed it as CONNECTING right
   after my key, so I cannot fully rule out that the key briefly touched it.
6. While Aptura plays with the task removed, the status bar shows a media indicator and Android's recents screen shows
   "1 active app"; the playback service notification is a media notification (no `POST_NOTIFICATIONS` request seen).

## Exact list of phone settings and files I changed

Written with `settings put` or `cmd appops set`: only two things, both restored and checked.
- `system font_scale`: 1.0 to 2.0 and back to 1.0.
- App-op `PICTURE_IN_PICTURE` for `org.aptura.player`: default to deny and back to default.

Not written by me, and read back unchanged against what I saw at the start: `stay_on_while_plugged_in` 0,
`screen_off_timeout` 600000, `screen_brightness` 55 (mode 0), `accelerometer_rotation` 1, `user_rotation` 0, animation
scales 1.0, display density override 420, `enabled_accessibility_services` (one screen-mirroring entry),
`touch_exploration_enabled` 0. Developer options and USB debugging were already on (the owner turned USB debugging on).
The phone rotated to portrait on its own once (auto-rotate is on).

Other changes: the app was freshly installed with `adb install` (and `installDebug` was tried once and refused, which
changed nothing); the welcome flow was completed and "Allow all" media access granted; app settings changed and
restored (theme System, play mode still on Repeat one); per-video offsets were saved on the test files only; files were
pushed to `Movies/AptureTest/`; `/sdcard/ui.xml` (16 KB, a UI-dump temp file) is still on the phone; `logcat -c` was run
many times (the log buffers are cleared); the app process was force-stopped and its task removed from recents several
times; media-scanner broadcasts were sent for the test files.

## TalkBack (not attempted)

I did not try to enable TalkBack, so I have no blocker to report. Facts gathered read-only: both builds are installed
(`com.google.android.marvin.talkback`, service `com.google.android.marvin.talkback.TalkBackService`, and
`com.samsung.android.accessibility.talkback`, whose service name was not captured); the only enabled accessibility
service is the screen-mirroring one; there is a lock credential and a 5 s lock-after-timeout. Safe way in and out, not
yet tried: save the exact `enabled_accessibility_services` string first; append one TalkBack component (never both
builds) with `settings put secure enabled_accessibility_services "<saved>:<component>"`; to leave, write the saved
string back over adb (this works even when TalkBack has taken over the touch screen).
