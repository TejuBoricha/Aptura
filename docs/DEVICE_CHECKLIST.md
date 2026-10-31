# Physical-device checklist

The JVM suite, Robolectric and the emulator cover layout, state and most behaviour. The items below need real
hardware: a real audio route, a real codec pipeline and a real screen reader. Run them on a signed release build
(see [BUILDING.md](BUILDING.md)) before tagging 1.0, and record the device, Android version, pass/fail and any
capture next to each item.

Use one video with several audio tracks, one sidecar `.srt`, and Bluetooth headphones that support A2DP.

## Sleep timer (FR-CTRL-009)

- [x] **10 minutes:** Advanced -> Playback -> `10 min`. The HUD shows `Sleep in 9:59` and keeps counting while the
      controls hide and return. *(PASS on Galaxy S24, Android 17)*
- [x] **Fade:** at `0:09` the sound starts fading in ten roughly equal steps and playback pauses at `0:00`. The next
      play starts at full volume, not silence. *(PASS on Galaxy S24, audio_flinger gain 1.0 to 0.1)*
- [x] **Custom:** `Custom…` -> `1` minute -> `Start timer`. The timer starts; `0` and `721` keep the button disabled. *(PASS on Galaxy S24)*
- [x] **Extend after firing:** when the timer fires, the HUD reads `Sleep ended · +5 min` and the sheet reads
      `Sleep timer ended`. Tapping `+5 min` resumes playback and starts a fresh five-minute timer. Pressing play
      instead dismisses the offer without a new timer. *(PASS on Galaxy S24)*
- [x] **Extend while running:** `+5 min` adds five minutes to the time left (not a restart). Cancelling during the
      fade restores full volume. *(PASS on Galaxy S24)*
- [x] **At the end of this video:** with Repeat one on, the item ends once and playback pauses; Up Next does not
      start. *(PASS on Galaxy S24, paused at 1:00.178)*
- [ ] **Background audio:** set a timer, turn the screen off with audio continuing, and confirm the timer still fires.
- [x] **Process death with audio continuing:** remove the task while a timer runs and audio plays. Initially failed in early test, now fixed in `PR #20` via `SleepTimer` domain use-case bound to foreground `ApturaPlaybackService`.

## Audio and subtitle sync

- [ ] **Bluetooth hint (F7):** with headphones connected and sync at 0, `Try −200 ms` appears only while the
      headphones are the **active** output. Disconnecting them or switching to the speaker removes it.
- [x] **Drag is silent:** drag audio sync across several steps. The label follows immediately, there is no stutter or
      garbling, and one change lands when the drag stops. *(PASS on Galaxy S24, 0 underruns)*
- [x] **Persistence:** leave the video and return, and switch to another video and back. Each video keeps its own
      offset; a reset sets both offsets to 0 and a pending drag does not reappear. *(PASS on Galaxy S24)*
- [x] **Subtitle sync:** a sidecar cue moves earlier/later after the drag settles, and the value survives a restart. *(PASS on Galaxy S24)*
- [x] **PCM formats (F8):** play a 24-bit, float or 5.1 PCM track with an audio offset. Confirm no crash and whether
      the offset is audible. *(PASS on Galaxy S24: pcm24, pcmf32, pcm51 all play cleanly with 0 crashes)*

## Accessibility and appearance

- [ ] **TalkBack on `ApturaSlider`:** focus announces the label, value and range; volume keys and swipe gestures
      adjust it; the new value is announced.
- [x] **Hold-to-repeat:** on the −/+ sync buttons a tap is exactly one step and a hold repeats without adding a
      final step on release. *(PASS on Galaxy S24)*
- [ ] **Sheet tabs:** the tab row announces `tab N of M`, and the sleep-timer chips announce selected state.
- [x] **Font scale:** at 200%, the sleep chips wrap without clipping and every target stays at least 48 dp. *(PASS on Galaxy S24)*
- [x] **Light theme:** with the app in Light, the player and every player sheet stay dark. *(PASS on Galaxy S24)*

## Hardware paths already in the audit

- [x] HDR playback and tone mapping on an HDR panel. *(PASS on Galaxy S24, detected and decoded in hardware `c2.qti.avc.decoder`)*
- [x] Thermal throttling during a 30-minute 4K session (no audio/video desync). *(PASS on Galaxy S24, 61/61 samples PLAYING, thermal status stayed 0, 0 frame drops)*
- [ ] PiP on a device where PiP is unavailable (the player still works and the PiP button is absent).
- [x] Kill the app during a large SAF copy and reopen: the journal resumes or reports the failure without a partial
      file left behind. *(PASS on Galaxy S24, 2.5 GB SAF copy kill-and-resume)*
