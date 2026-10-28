# Physical-device checklist

The JVM suite, Robolectric and the emulator cover layout, state and most behaviour. The items below need real
hardware: a real audio route, a real codec pipeline and a real screen reader. Run them on a signed release build
(see [BUILDING.md](BUILDING.md)) before tagging 1.0, and record the device, Android version, pass/fail and any
capture next to each item.

Use one video with several audio tracks, one sidecar `.srt`, and Bluetooth headphones that support A2DP.

## Sleep timer (FR-CTRL-009)

- [ ] **10 minutes:** Advanced -> Playback -> `10 min`. The HUD shows `Sleep in 9:59` and keeps counting while the
      controls hide and return.
- [ ] **Fade:** at `0:09` the sound starts fading in ten roughly equal steps and playback pauses at `0:00`. The next
      play starts at full volume, not silence.
- [ ] **Custom:** `Custom…` -> `1` minute -> `Start timer`. The timer starts; `0` and `721` keep the button disabled.
- [ ] **Extend after firing:** when the timer fires, the HUD reads `Sleep ended · +5 min` and the sheet reads
      `Sleep timer ended`. Tapping `+5 min` resumes playback and starts a fresh five-minute timer. Pressing play
      instead dismisses the offer without a new timer.
- [ ] **Extend while running:** `+5 min` adds five minutes to the time left (not a restart). Cancelling during the
      fade restores full volume.
- [ ] **At the end of this video:** with Repeat one on, the item ends once and playback pauses; Up Next does not
      start.
- [ ] **Background audio:** set a timer, turn the screen off with audio continuing, and confirm the timer still fires.

## Audio and subtitle sync

- [ ] **Bluetooth hint (F7):** with headphones connected and sync at 0, `Try −200 ms` appears only while the
      headphones are the **active** output. Disconnecting them or switching to the speaker removes it. (The current
      implementation reacts to the device being connected, not to the active route; record what actually happens.)
- [ ] **Drag is silent:** drag audio sync across several steps. The label follows immediately, there is no stutter or
      garbling, and one change lands when the drag stops.
- [ ] **Persistence:** leave the video and return, and switch to another video and back. Each video keeps its own
      offset; a reset sets both offsets to 0 and a pending drag does not reappear.
- [ ] **Subtitle sync:** a sidecar cue moves earlier/later after the drag settles, and the value survives a restart.
- [ ] **PCM formats (F8):** play a 24-bit, float or 5.1 PCM track with an audio offset. Confirm no crash and whether
      the offset is audible. The processor currently handles 16-bit PCM only; record the stream format shown by the
      playback stats when it does nothing.

## Accessibility and appearance

- [ ] **TalkBack on `ApturaSlider`:** focus announces the label, value and range; volume keys and swipe gestures
      adjust it; the new value is announced.
- [ ] **Hold-to-repeat:** on the −/+ sync buttons a tap is exactly one step and a hold repeats without adding a
      final step on release.
- [ ] **Sheet tabs:** the tab row announces `tab N of M`, and the sleep-timer chips announce selected state.
- [ ] **Font scale:** at 200%, the sleep chips wrap without clipping and every target stays at least 48 dp.
- [ ] **Light theme:** with the app in Light, the player and every player sheet stay dark.

## Hardware paths already in the audit

- [ ] HDR playback and tone mapping on an HDR panel.
- [ ] Thermal throttling during a 30-minute 4K session (no audio/video desync).
- [ ] PiP on a device where PiP is unavailable (the player still works and the PiP button is absent).
- [ ] Kill the app during a large SAF copy and reopen: the journal resumes or reports the failure without a partial
      file left behind.
