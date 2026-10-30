# Handoff: on-device testing on the Galaxy S24 (claude to opencode)

Written 2026-10-29 around 13:00 local, when claude's usage limit was ending. Another agent can continue from this
file, the findings file next to it, and the scripts in `tools/device/`.

## The request (from the owner)

"Complete whatever can be tested on device, left nothing." Later additions from the owner:
- Personal videos on the phone may be used for testing, **except delete and other important actions** (no delete, hide,
  rename, move or share on anything that is not a test file; never use the library's ⋮ menu on a personal video).
- Note down findings, and also improvements noticed along the way.
- Put everything in the hands of opencode.

## Where things are

| What | Where |
|---|---|
| Results, evidence, defects, improvement ideas | [`2026-10-29-galaxy-s24-findings.md`](2026-10-29-galaxy-s24-findings.md) (the source of truth; update it, do not duplicate it) |
| adb helpers (UI dump and tap, screenshots, flows, thermal sampler, media generator) | [`tools/device/`](../../tools/device) |
| Thermal run output (not in the repo) | `C:\Users\tejas\AppData\Local\Temp\claude\c--Users-tejas-Desktop-Mobile-App\cc52b14f-40b8-45d1-95cf-1713cd165073\scratchpad\thermal-run.csv` and `thermal_stats_NN.png` next to it |
| 2.5 GB file for the copy test (not in the repo) | same folder, `media2\bigtest.mp4` |
| Original (path-specific) copies of the scripts | same folder: `ui.ps1`, `snap.ps1`, `flow.ps1`, `thermal-run.ps1`, `gen-media.ps1` |

The scratchpad folder is temporary. Copy anything you still need (the CSV and the stats images first) into
`$env:APTURA_DEVICE_WORK` or into the repo soon.

Script settings: `ADB` (path to adb.exe, default `adb`), `FFMPEG` (default `ffmpeg`), `APTURA_DEVICE_WORK` (scratch
folder, default `%TEMP%\aptura-device-test`). On this PC: adb is
`C:\Users\tejas\AndroidDev\sdk\platform-tools\adb.exe` and ffmpeg is the WinGet `Gyan.FFmpeg` install. Use
`. tools\device\flow.ps1` to load all the helpers.

## Test setup

- Phone: Galaxy S24 (SM-S921B), Android 17, USB debugging on, authorised. Display density is overridden to 420 dpi
  (physical 480), so **48 dp = 126 px**.
- App: release build (`assembleRelease`) of this repo, signed with the local "Aptura Smoke Key". **Never use
  `installDebug` on this phone**: the debug key differs and Android refuses the update, and uninstalling wipes data.
  Update with `adb install -r app\build\outputs\apk\release\app-arm64-v8a-release.apk`.
- The app is installed fresh: the welcome flow is done and "Allow all" photos and videos access is granted. Offsets are
  only saved for videos that have a library row, so test persistence from the library, not from an external VIEW intent.
- Test media is in `/sdcard/Movies/AptureTest/` on the phone: `sleep-test.mp4` (+ `.srt`), `other-test.mp4`,
  `pcm24.mkv`, `pcmf32.mkv`, `pcm51.mkv`, `hdr10.mp4`, `uhd-sdr.mp4`. Regenerate with `tools/device/gen-media.ps1`.

## Status

Done (details and evidence in the findings file): sleep timer 10 min, fade, custom, extend after firing, extend while
running, cancel during fade, play dismisses the offer, end of video with Repeat one, background audio (screen on),
process death; drag, hold-to-repeat, persistence, subtitle sync, PCM formats; font scale 200%, Light theme, sheet
selected-state semantics (partial); HDR detection and decode; PiP approximation.

**Headline defect:** after the app task is removed from recents, the sleep timer is lost but playback continues
(the foreground `ApturaPlaybackService` keeps running). A 3-minute timer was still not firing 35 s past its deadline.

In flight when claude stopped:
1. **30-minute 4K thermal run**: a detached PowerShell sampler (pid 15228) started 12:55:06 and ends about 13:25:06.
   It logs every 30 s to `thermal-run.csv` (state, real position, drift against the wall clock, thermal status,
   AP and skin temperature, battery, underruns, CPU frequency) and saves a stats crop (dropped frames) every 5 min.
   The phone is playing `uhd-sdr` on repeat in the player. **Do not touch the phone until the CSV ends with a `DONE`
   line.** If the run was interrupted, restart it: open `uhd-sdr` with Repeat one, press play, run
   `tools\device\thermal-run.ps1 -Minutes 30`. Then summarise: max and min of `ap_c`, `skin_c`, `battery_c`,
   `cpu_big_khz`, the thermal status values, `underruns`, the `drift_ms` range, and read the dropped-frames number in the
   stats crops. Caveat: audio/video desync cannot be measured over adb, only playback-clock drift against wall time.
   **The `ap_c` and `skin_c` columns of that CSV are stale** (cached values); use `thermal-live.csv` for temperatures and
   read the new section "Thermal run data notes" in the findings file. The phone was also charging over USB.

## What is left, in this order

1. **Thermal results**: summarise as above into the findings file (the table row says IN PROGRESS).
2. **Kill the app during a large SAF copy** (`docs/DEVICE_CHECKLIST.md` line 59). The feature is the library's
   **Move to folder** (`MediaTransferWorker`, journal in `MediaTransferJournal`, resumed by
   `MediaTransferScheduler.resumePending` in `ApturaApplication.onCreate`). Plan: push `bigtest.mp4` to
   `/sdcard/Movies/AptureTest/`, `mkdir /sdcard/Movies/AptureTestDest`, find `bigtest` with the library search, open
   the ⋮ menu **of that tile only** ("More actions for bigtest"), choose Move to folder, pick `AptureTestDest` in the
   system picker, then poll the destination size with `adb shell stat -c %s` and `adb shell am force-stop
   org.aptura.player` at about 40%. Reopen the app and record: does it resume, or report a failure; is a partial file
   left in the destination; is the source still there. Only ever operate on the test file.
3. **TalkBack on `ApturaSlider` and the "tab N of M" announcement** (checklist lines 46 and 50). A UI dump does not
   show them. Both TalkBack builds are installed (`com.google.android.marvin.talkback`,
   `com.samsung.android.accessibility.talkback`). One other accessibility service is already enabled on the phone
   (screen mirroring); **read `settings get secure enabled_accessibility_services` first, keep that entry, and restore
   the exact original string afterwards**. Turn on TalkBack's developer option "Display speech output" so the spoken
   text can be read from a screenshot. With TalkBack on, a single `input tap` only focuses; a double tap activates.
   If this cannot be done safely, mark the items NOT TESTED rather than guess.
4. **Screen-off background audio** (checklist line 24). The phone has a lock credential and locks as soon as the screen
   turns off; adb cannot unlock it. Do this last and only with the owner present: start a 1-minute timer, play, press
   power, wait 70 s, ask the owner to unlock, and check the audio stopped at about 1:00 (the gain log in
   `flow.ps1: Watch-Playback` works with the screen off).
5. **Bluetooth hint (F7)**: needs the owner's A2DP headphones. Not testable by an agent.
6. **Clean up the phone** (see below), then **record the results in `docs/DEVICE_CHECKLIST.md`** (device, Android
   version, pass/fail next to each item, as that file asks) on a branch that does not touch the hot files, and let the
   owner decide which findings become issues.

## Phone state to restore at the end

- PiP app-op for Aptura: already put back to `default` (it was denied for two tests).
- Font scale: restored to `1.0`. App theme: restored to `System`.
- Player "Play mode" is on **repeat this video**: tap the play-mode button in the player controls until the label reads
  "Play mode: play in order".
- Playback stats overlay is switched on for the current player session (it resets when the app restarts).
- Files to delete **after** the work above: `Movies/AptureTest/*`, `Movies/AptureTestDest/` (only these).
- The media permission and the finished welcome flow can stay.

## Gotchas that cost time (so you do not repeat them)

- `uiautomator dump` returns nothing useful **while a video plays and the controls are hidden**. Pause first
  (`input keyevent 127`), then dump. The controls auto-hide after about 3 s, and a tap on the video toggles them.
- Media keys go to the most recent media session. `dumpsys media_session` lists other apps' sessions first, so read
  Aptura's block (`Get-AptState`), not the first `state=PlaybackState` line.
- The session `position` can be stale for a long time on some items (the 4K clip stayed at 3 ms). Real position =
  `position + (nowELAPSED - updated) * speed`, with `nowELAPSED` from `dumpsys alarm` (`Get-RealPos` in
  `thermal-run.ps1`). Do not trust the raw field for drift.
- Pressing Home while a video plays makes a Picture-in-Picture window, which has no recents card. Deny the PiP app-op
  (`cmd appops set org.aptura.player PICTURE_IN_PICTURE deny`) to get a normal background, and restore it to `default`.
- Removing the task from recents: the card sits at the middle of the screen, `input swipe 540 1100 540 150 160`.
  Check `dumpsys activity recents` before and after so only Aptura's task goes.
- The Back key sent over adb did not close the Playback tools sheet. Use the sheet's drag handle (`Close-Sheet`).
- `Tap-Ui` matches by regex on the label. **Never use loose patterns such as `^More`**: it opened a personal video's ⋮
  menu once (nothing was selected). Use exact labels, and open test videos with `Open-ByName` (library search).
- Taps on chips next to each other are close: a mis-tap changed a 10-minute timer to 20 minutes once.
- `am start ... --grant-read-uri-permission -a VIEW -d content://media/external/video/media/<id>` opens a clip without
  the library, but offsets set that way are not saved (see the findings file).
- Hold-to-repeat timing is 400 ms delay then a step every 90 ms; a 450 ms hold must give exactly one step.
- Fade check without ears: sample the track gain from `dumpsys media.audio_flinger` (columns `G dB`, vol) once a
  second near the deadline.

## Repo state

This branch only adds docs and scripts under `docs/device-testing/` and `tools/device/`. It touches no hot file and no
app code. `tools/gate.ps1` was **not** run locally (documentation and helper scripts only, and the owner's limit was
ending); CI will run. PR #15 (opencode's) and the working tree of the main checkout were not touched. When claude
started, that checkout was on `agent/opencode/14-silence-detection`; it is now on `test/stall-live-0700`.

## State at the 2026-10-30 hold (opencode)

The owner said **"leave that testing on hold"** on 2026-10-30 (in opencode's session). No deadline on #17 runs out
while that stands, and no agent should start device work on this phone until the owner lifts it. The branch is pushed.

Done since this handoff was written (evidence in the findings file):

- 30-minute 4K thermal run: **PASS** (`f586bbc`). Live HAL data from `thermal-live.csv`; the `ap_c`/`skin_c` columns
  of `thermal-run.csv` are invalid.
- Kill during the 2.5 GB SAF move: **PASS** (`cc60d77`). The resumed run deleted the partial and recopied it, then
  the move completed after Android's delete consent.

In flight when the hold started:

- **TalkBack is still enabled on the phone.** Google TalkBack
  (`com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService`) was appended to
  `enabled_accessibility_services` for the two accessibility checks. The USB connection dropped before the checks ran
  and before the setting was restored, and the phone is not on adb.
- Restore over adb once the phone is back (the original string saved before the change was exactly the first one;
  `accessibility_enabled` was 1 and `touch_exploration_enabled` was 0):

  ```powershell
  adb shell settings put secure enabled_accessibility_services "com.microsoft.appmanager/com.microsoft.mmx.screenmirroringsrc.accessibility.ScreenMirroringAccessibilityService"
  adb shell settings put secure touch_exploration_enabled 0
  adb shell settings put secure accessibility_enabled 1
  ```

- Without adb, on the phone: double-tap the red **Turn off TalkBack** card, or **Settings > Accessibility > TalkBack >
  Off**. With TalkBack on, one tap focuses and a double tap activates.
- Not run: the TalkBack slider and `tab N of M` checks, screen-off background audio (needs the owner), the Bluetooth
  F7 hint (needs headphones), the phone cleanup, and the `docs/DEVICE_CHECKLIST.md` record.

To finish after the hold: restore the setting, run the two TalkBack items, do the screen-off test with the owner,
record everything in the findings file and the checklist, then clean up `Movies/AptureTest/`, `Movies/AptureTestDest/`,
`/sdcard/ui.xml`, play mode (back to play in order) and the stats overlay.

