# Code review - merged UI/player work

**Range:** `77176eb..392616a` (five commits, 4,747 insertions / 4,326 deletions across 60 files)
**Reviewed:** 2026-10-28
**Tree:** `504981d` (nothing has changed in `app/` since `392616a` except the trash-first dialog and the launcher mark)
**Gate at review time:** 666 tests / 97 suites, 0 failures, 0 errors, 77 opt-in gallery renders skipped; lint 0; `assembleRelease` + `verifyReleaseApk` clean
**Reviewer:** DeepSeek V4.1 Flash (OpenCode)

> This is a read-only review. It did not run the app: there is no device or emulator here. Every runtime claim below
> comes from reading the code and the existing tests, and the items that genuinely need hardware are marked as such.

---

## Verdict

**Approve with changes.** The design-kit consolidation is a real improvement and the sync sign convention is now
correct. Nothing here blocks a debug build. Three items should be fixed before a 1.0 tag:

1. the sleep timer counts by subtracting the requested step instead of measuring elapsed time (drifts, and breaks the
   repo's own zero-polling rule);
2. audio sync persists to Room on every slider frame;
3. audio sync inserts or drops PCM on every slider frame, so dragging the control is audible.

Items marked **Verify on device** cannot be settled by reading code; they belong in the manual pass.

---

## Method

Read in full: `ApturaDesignKit.kt`, `PlayerControls.kt`, `PlayerSheetChrome.kt`, `PlayerToolsTabs.kt`,
`AudioEnhancementSheet.kt`, `AudioDelayProcessor.kt`, `Theme.kt`, `Color.kt`, the player sections of
`PlayerViewModel.kt`, `TimingOffsetControlUiTest.kt`, and the two sleep-timer tests.

Sampled: the library/settings diffs and the gallery tests. I did not line-by-line review all 4,700 changed lines, and
I did not review the evidence images.

---

## Findings

### F1 - The sleep timer drifts (Should fix)

`PlayerViewModel.kt:1260-1269`

```kotlin
while (remaining > 0L) {
    val step = minOf(SLEEP_TIMER_TICK_MS, remaining)
    delay(step)
    remaining -= step
    ...
}
```

`delay` guarantees *at least* the requested time. The countdown assumes it was exact, so every late wake-up is
silently lost and the error accumulates: 3,600 iterations for an hour, each adding scheduling jitter, plus batching
when the screen is off. A 30-minute timer can fire tens of seconds late - on a feature whose entire purpose is to stop
at a time.

The Robolectric tests cannot catch this: `advanceTimeBy` advances virtual time exactly, so the loop is exact by
construction.

**Fix:** keep a monotonic deadline and derive the remaining time from it.

```kotlin
val deadline = SystemClock.elapsedRealtime() + durationMs
while (true) {
    val left = deadline - SystemClock.elapsedRealtime()
    if (left <= 0) break
    delay(minOf(SLEEP_TIMER_TICK_MS, left))
    ...
}
```

Better still for F2: one `delay(durationMs)` for the pause, a `sleepTimerEndsAtElapsedRealtime` field in the UI state,
and a 1 Hz ticker only while the sheet is open. The pause is then exact and the UI owns the ticking.

### F2 - The sleep timer violates the repo's zero-polling rule (Should fix)

`.agents/rules/coding-standards.md:20` says, in capitals, that ViewModels must never use `while (...) delay(...)` and
that state tracking must be event-driven. `PlayerViewModel.kt:1260` is exactly that pattern. This is the rule the
project cites when it explains why the checkpointing is event-driven, so leaving an exception in the newest code
undermines it.

(The loop is bounded, so it does not hang `advanceUntilIdle`; it would simply simulate the whole countdown. The
"deadlock" concern in the earlier plan is overstated. The drift and the rule are the real issues.)

The same shape exists in the pre-existing Up Next countdown (`PlayerViewModel.kt:581-592`). That one is five seconds
and barely matters, but if the rule is absolute, fix both or write down the exception.

### F3 - Audio sync writes the database on every frame of a drag (Should fix)

`PlayerViewModel.kt:1058-1064` calls `persistPlaybackPreferences()` on every `setAudioDelay`, and
`persistPlaybackPreferences()` (`:1218-1234`) launches another coroutine that upserts the whole row.

The slider reports every snapped step (`PlayerControls.kt:328-340`, 50 ms steps by default). Dragging across the
range is dozens of Room writes, and holding a step button is ~11 writes per second. Last-write-wins keeps the value
correct, so this is not data loss - it is unnecessary IO and allocation on the player's hot path.

**Fix:** update `_uiState` immediately, debounce the persist (300-500 ms) or persist on drag end / button release.
`TimingOffsetControl` needs an `onValueChangeFinished` hook for that.

### F4 - Audio sync is audible while you drag it (Should fix, then verify on device)

`AudioDelayProcessor.kt:54-70` implements the offset by inserting silence (`deltaFrames > 0`) or dropping samples
(`deltaFrames < 0`) the moment the target changes. That is correct for an absolute offset, but it means every
intermediate value during a drag is applied as a real audio edit:

- dragging toward "later" inserts that much silence, in steps - the sound stutters;
- dragging toward "earlier" drops PCM mid-word - clipped, garbled speech;
- dragging up, down and up again re-inserts silence each time.

**Fix:** preview while dragging, apply on release; or ramp `delayMs` toward the target over ~150-250 ms so the
processor never jumps by a full step. Either way the control still ends at the requested offset.

**Verify on device** with Bluetooth headphones: the whole feature is untested outside fakes.

### F5 - "Fades out over the last 10 seconds" is 9 seconds (Nit)

`PlayerViewModel.kt:1266`. The tick that lands on 10,000 ms subtracts first, so the first fade step is at 9,000 ms
(volume 0.9). The copy in `PlayerToolsTabs.kt:108` promises ten. Fixing F1 makes this exact automatically.

### F6 - `SectionTitle` halves its title when `trailing` is set (Latent bug)

`ApturaDesignKit.kt:410` gives the title `weight(1f, fill = false)` and `:422` adds `Spacer(Modifier.weight(1f))` for
the trailing slot. Two equal weights split the free width 50/50, so a long title is measured against half the row and
ellipsizes early while the other half sits empty.

No current caller passes both a long title and `trailing`, so nothing is broken today - but it is a shared component,
and the next caller will hit it. Replace the spacer with a fixed gap (or give the title the only weight).

### F7 - The Bluetooth hint means "connected", not "in use" (Low)

`AudioEnhancementSheet.kt:287-304` documents itself as "whether sound is going to a Bluetooth device right now" but
checks `getDevices(GET_DEVICES_OUTPUTS)` for any Bluetooth type. A paired headset that is connected but not the active
route still triggers the suggestion. Either rename the state and soften the copy, or compare against the active output
device.

### F8 - The processor only handles 16-bit PCM (Verify on device)

`AudioDelayProcessor.kt:40-45` returns `AudioFormat.NOT_SET` for anything that is not `ENCODING_PCM_16BIT`, which
passes audio through untouched. That is a safe failure, but it is a silent one: if the audio sink negotiates float
output, the sync control moves, persists, and does nothing. Confirm the negotiated encoding on a real device before
calling the feature done.

### F9 - Decimal separator follows the device locale inside English copy (Nit)

`PlayerControls.kt:275` formats with `"%.2f".format(...)`, which uses the default locale. A French-locale device shows
`+1,2 s` in an otherwise English UI. Use `Locale.ROOT` for a developer-facing offset, or pass the locale explicitly.

### F10 - The tab row is not a selectable group (Low, accessibility)

`PlayerSheetChrome.kt:127-148` sets `role = Tab` and `selected` per tab, but the row has no `selectableGroup()`.
TalkBack will not announce "tab 2 of 4" or the group's label. One modifier.

---

## What is right, and should not be "fixed" later

- **Sync sign convention.** `+` inserts silence = sound later; the UI says "later". The Bluetooth suggestion uses
  `-200 ms` = sound earlier, which is the correct compensation for A2DP lag. The "backwards audio sync" bug from
  `95231ca` is genuinely fixed.
- **`ApturaSlider` semantics.** `progressBarRangeInfo`, `setProgress`, `contentDescription` and `stateDescription`
  are all present (`PlayerControls.kt:213-221`). It is a real slider for TalkBack, not a painted canvas.
- **`SegmentedPicker`.** `selectableGroup` + `selectable(role = RadioButton)` + 54 dp targets
  (`SettingsDesign.kt:288-311`). The `-1` "no current choice" state is documented and handled by hiding the thumb.
- **Trash-first delete.** Main action recoverable, permanent delete as the red outlined alternative, and the copy
  states the consequence. This is the correct hierarchy.
- **Player stays dark.** `themeModeForRoute` (`Theme.kt:20-21`) is pure and unit-tested; the route, not the app
  setting, decides.
- **Bluetooth callback lifecycle.** `AudioDeviceCallback` is registered and unregistered in `DisposableEffect`
  (`AudioEnhancementSheet.kt:290-302`); no leak.

## Architectural note (not a finding)

`ApturaColors` is a process-global `mutableStateOf` palette written during composition (`Color.kt:48`,
`Theme.kt:45-48`). It is documented and works, but only one palette can be active per process: if two `ApturaTheme`s
compose at once - a navigation transition, multi-window, a preview - the last one applied wins for everything. Watch
for it when transition durations change. If it ever bites, `staticCompositionLocalOf` is the migration, and it is a
mechanical one.

## Coverage gaps

- No test asserts that the pause fires at the deadline under *real* elapsed time; virtual time hides F1 by design.
  (Addressed: `remainingAt` now exists as the pure `sleepTimerRemainingMs(deadline, now)`, see the addendum.)
- No test covers `extendSleepTimer` when the timer is off (it returns silently) or the picker's deselected state.
- `TimingOffsetControlUiTest` covers tap and hold-to-repeat well, but nothing asserts the slider's accessibility
  semantics (`setProgress`), which is the part the device pass is meant to exercise.

---

## Addendum - fixes landed in `3844c44` and the FR-CTRL-009 follow-up

F1-F6, F9 and F10 are fixed in code; F7 and F8 stay open pending real hardware. FR-CTRL-009 (the documented preset
ladder, a custom duration, and an offer after the timer fires) is complete in the same branch.

- **F1 (drift) + F2 (polling):** the ViewModel now keeps one absolute deadline,
  `PlayerUiState.sleepTimerEndsAtElapsedRealtime`, sampled from an injected monotonic `clock`
  (`SystemClock.elapsedRealtime()` by default). `runSleepCountdown` is gone. The fade re-anchors each of its ten
  one-second steps to the same deadline, so a late wake-up is caught up instead of added. The countdown itself is
  derived in the UI by `rememberSleepTimerRemainingMs`, which ticks only while a screen displays it; playback state
  no longer changes every second. The pure `sleepTimerRemainingMs(deadline, now)` closes the first coverage gap
  above (`SleepTimerRemainingTest`, 3 cases).
- **F3 (a write per frame) + F4 (audible drag):** `setAudioDelay` / `setSubtitleDelay` update the label immediately,
  then wait `SYNC_APPLY_DEBOUNCE_MS` (200 ms) of stillness before telling the controller and persisting. A pending
  edit is cancelled in `applyItemPreferences` (video change) and `resetVideoPreferences`, so it cannot land on the
  next video or reappear after a reset. One test drives twenty 50 ms steps and asserts zero writes during the drag
  and exactly one write at the end.
  - The track-selection collector no longer copies `audioDelayMs` / `subtitleDelayMs` back from the controller.
    While the edit is pending the controller still holds the old value, so an unrelated emission would have
    reverted the label mid-drag. The ViewModel owns those two values; everything else in the collector is unchanged.
- **F5:** the fade is exactly 10 s - ten equal steps, the first at 9 s remaining (`SLEEP_FADE_STEPS`,
  `SLEEP_FADE_STEP_MS`), pinned by `sleepTimer_fadeStartsExactlyTenSecondsBeforeTheDeadline`.
- **F6:** `SectionTitle` gives the title the free width (`fill = trailing == null`), so a long title is measured
  against the whole row instead of half of it.
- **F9:** `formatOffset` uses `Locale.ROOT`, so the decimal separator stays a dot in English copy.
- **F10:** the sheet tab row is a `selectableGroup()`.
- **F7 (Bluetooth hint) and F8 (16-bit PCM):** untouched; both need a real device and are part of the physical
  checklist in `docs/DEVICE_CHECKLIST.md`.
- **FR-CTRL-009 (sleep timer spec):** the sheet now offers the documented 10/15/20/30/45/60 ladder and a 1-720 minute
  custom duration (wrapped chips, 48 dp targets), and expiry pauses with a `+5 min` offer in both the HUD and the
  sheet. Accepting the offer resumes playback and starts a fresh five minutes; resuming manually dismisses it.
  `SleepTimerOptionsTest` pins the ladder and the custom bounds, `SleepTimerOptionsUiTest` pins the visible chips.
- **CI hang guard:** the unit-test task timeout moved from 120 s to 300 s after a cold GitHub runner exceeded the old
  value at ~680 tests. It remains a guard, not a disabled check (`app/build.gradle.kts`).
- **Gate after the fixes:** 679 tests / 100 suites, 0 failures, 0 errors, 78 opt-in gallery renders skipped; lint 0;
  `assembleRelease` + `verifyReleaseApk` clean.
