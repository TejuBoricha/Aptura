# Physical-device checklist

The JVM suite, Robolectric and the emulator cover layout, state and most behaviour. The items below need real
hardware: a real audio route, a real codec pipeline and a real screen reader. Run them on a signed release build
(see [BUILDING.md](BUILDING.md)) before tagging 1.0, and record the device, Android version, pass/fail and any
capture next to each item.

**A row is ticked only when a dated run on a device backs it.** A JVM test, a merged fix or a code reading is not a device
pass: write it as a note, and leave the box empty. The evidence for every ticked row is in
[`device-testing/2026-10-29-galaxy-s24-findings.md`](device-testing/2026-10-29-galaxy-s24-findings.md), which also
records what was not judged. That run used adb only: nothing was judged by ear, and no Bluetooth headphones, TalkBack
speech or HDR panel judgement was involved unless a row says so.

Use one video with several audio tracks, one sidecar `.srt`, and Bluetooth headphones that support A2DP.

## Sleep timer (FR-CTRL-009)

- [x] **10 minutes:** Advanced -> Playback -> `10 min`. The HUD shows `Sleep in 9:59` and keeps counting while the
      controls hide and return. *(PASS on Galaxy S24, Android 17, 2026-10-29)*
- [x] **Fade:** at `0:09` the sound starts fading in ten roughly equal steps and playback pauses at `0:00`. The next
      play starts at full volume, not silence. *(PASS on Galaxy S24, audio_flinger gain 1.0 to 0.1; not judged by ear)*
- [x] **Custom:** `Custom…` -> `1` minute -> `Start timer`. The timer starts; `0` and `721` keep the button disabled. *(PASS on Galaxy S24)*
- [x] **Extend after firing:** when the timer fires, the sheet reads `Sleep timer ended`. Tapping `+5 min` resumes
      playback and starts a fresh five-minute timer. Pressing play instead dismisses the offer without a new timer.
      *(PASS on Galaxy S24; the HUD wording `Sleep ended · +5 min` was not seen)*
- [x] **Extend while running:** `+5 min` adds five minutes to the time left (not a restart). Cancelling during the
      fade restores full volume. *(PASS on Galaxy S24)*
- [x] **At the end of this video:** with Repeat one on, the item ends once and playback pauses; Up Next does not
      start. *(PASS on Galaxy S24, paused at 1:00.178)*
- [x] **Background audio, screen on:** set a timer, press Home, and confirm the timer still fires. *(PASS on Galaxy S24)*
- [ ] **Background audio, screen off:** set a timer, turn the screen off with audio continuing, and confirm the timer
      still fires. *(not run)*
- [ ] **Process death with audio continuing:** remove the task while a timer runs and audio plays. *(FAILED on the
      Galaxy S24 on 2026-10-29: the audio kept playing after the task was removed and the timer was lost. A fix landed
      in PR #20 and the service-lifetime fix `STAB-01` followed, but **neither has been re-run on a device**.)*
- [ ] **Player survives the system stopping its service (`STAB-01`):** play a video, pause it, press Home, wait about 90
      seconds, return. Play advances, and a second video starts. *(Passed on a Pixel 8 emulator on 2026-11-06; not run on
      a physical phone.)*

## Audio and subtitle sync

- [ ] **Bluetooth hint (F7):** with headphones connected and sync at 0, `Try −200 ms` appears only while the
      headphones are the **active** output. Disconnecting them or switching to the speaker removes it. *(not testable:
      no Bluetooth headphones were available)*
- [x] **Drag is silent:** drag audio sync across several steps. The label follows immediately, there is no stutter or
      garbling, and one change lands when the drag stops. *(PASS on Galaxy S24, 0 underruns; not judged by ear)*
- [x] **Persistence:** leave the video and return, and switch to another video and back. Each video keeps its own
      offset. *(PASS on Galaxy S24. The "pending drag does not reappear after reset" part was not testable by hand.)*
- [x] **Subtitle sync:** a sidecar cue moves earlier/later after the drag settles, and the value survives a restart. *(PASS on Galaxy S24)*
- [x] **PCM formats (F8), no crash:** play a 24-bit, float or 5.1 PCM track with an audio offset. *(PASS on Galaxy S24:
      pcm24, pcmf32, pcm51 all play with 0 crashes and 0 underruns. Whether the offset is audible was not testable.)*
- [ ] **Subtitle import (`FUNC-02`):** import an `.srt` during playback. The track appears, is selected, and its text is
      drawn. *(Passed on a Pixel 8 emulator on 2026-11-06; not run on a physical phone.)*
- [ ] **Swipe seek with the controls hidden (`FUNC-01`):** play for a minute with the controls hidden, then swipe a small
      distance forward. Playback moves forward from where it was. *(Reproduced on a Pixel 8 emulator before the fix;
      not run on a physical phone after it.)*
- [ ] **Audio delay at any speed (`FUNC-04`):** with Bluetooth earbuds and the sync set to +300 ms, play a clap or beep
      video at 1x and at 2x: the offset you hear is the same at both speeds. Hold to speed up and let go (two speed
      changes): the offset does not grow. Seek: it is applied again from the new position. *(Not run on a phone. The fix
      is tested on the JVM through Media3's real audio sink, which is the wiring the player uses; that sink never plays
      there, so what the media clock reports while the silence plays was not measured.)*
- [ ] **Swipe the app away while a video plays (`FUNC-05`):** queue three short videos, turn on the equalizer with some
      bass boost, play the first, then swipe Aptura out of Recents. The sound goes on, and three things hold: the next
      video starts by itself when the first ends (playback does not stop after one video); the bass boost and
      equalizer are still audible the whole time; and after you open Aptura again each video you passed resumes where
      you left it, the one that finished showing as watched. Then, with the video still playing, tap the notification:
      the player opens on that same video, which carries on from where it was (no jump back, no restart), and Back
      leaves to the library. Do the same from the launcher icon: the library opens and the video is not moved. *(Not
      run on a phone. All of it is tested on the JVM with fakes for the player and the sound effects; whether Android
      keeps the effects on the audio session once the screen is gone, and the notification's tap itself, were not
      measured.)*
- [ ] **Resume and the end of a video (`FUNC-08`):** with a queue of two videos, watch the first for a few minutes, press
      Next, then Previous: it goes back to where you left it (two seconds before), not to where it was when the queue
      was built. Then let a short video play to the end, or drag to its last seconds and leave, and open it again: it
      starts from the beginning, shows as watched, and its button in the details sheet says Play, not Resume. A video
      left half-way shows Resume and continues from there. *(Not run on a phone. All of it is tested on the JVM with a
      fake player and fake repository; the real database write at the end of a video was tested only with a fake
      DAO.)*

## Accessibility and appearance

- [ ] **TalkBack on `ApturaSlider`:** focus announces the label, value and range; volume keys and swipe gestures
      adjust it; the new value is announced. *(NOT TESTED with TalkBack on a device. `PlayerSheetChromeAccessibilityTest`
      checks the semantics on the JVM, which is not the same.)*
- [x] **Hold-to-repeat:** on the −/+ sync buttons a tap is exactly one step and a hold repeats without adding a
      final step on release. *(PASS on Galaxy S24)*
- [ ] **Sheet tabs:** the tab row announces `tab N of M`, and the sleep-timer chips announce selected state.
      *(PARTIAL: selected state was seen in the accessibility tree on the Galaxy S24. `tab N of M` needs TalkBack and
      is only checked on the JVM.)*
- [ ] **Player controls with TalkBack on (`A11Y-01`):** play, let the controls hide, swipe to the `Video` item, double-tap
      it: the controls return and stay while TalkBack is on. Pause, seek and leave the player using only TalkBack.
      *(not run: the controls used to be unreachable once hidden; fixed in code, never tried with TalkBack)*
- [x] **Font scale:** at 200%, the sleep chips wrap without clipping and every target stays at least 48 dp. *(PASS on Galaxy S24)*
- [x] **Light theme:** with the app in Light, the player and every player sheet stay dark. *(PASS on Galaxy S24)*

## Hardware paths already in the audit

- [ ] HDR playback and tone mapping on an HDR panel. *(PARTIAL on Galaxy S24: an HDR10 test clip played with 0 dropped
      frames and the stats overlay read "HDR10 (PQ)" and "Hardware Accelerated" on `c2.exynos.hevc.decoder`. Tone mapping
      and colour were not judged.)*
- [x] Thermal throttling during a 30-minute 4K session (no audio/video desync). *(PASS on Galaxy S24: 61/61 samples
      PLAYING, thermal status stayed 0, 0 dropped frames. **Caveats:** the phone was on USB power, so this is not an
      on-battery test; the clip was a 30 fps test pattern; the `ap_c` and `skin_c` columns of the CSV are wrong, and
      the live temperatures are in `thermal-live.csv`; and adb cannot measure audio/video desync.)*
- [ ] PiP on a device where PiP is unavailable (the player still works and the PiP button is absent). *(cannot be fully
      tested on a phone that supports PiP; with the app-op denied, the menu entry did nothing and showed no message)*
- [x] Kill the app during a large SAF copy and reopen: the journal resumes or reports the failure without a partial
      file left behind. *(PASS on Galaxy S24, 2.5 GB SAF move killed at 42%; the partial file is deleted and the file is
      copied again from zero, which can take long on slow storage.)*
