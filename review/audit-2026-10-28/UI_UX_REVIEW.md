# Aptura - UI/UX Review and Polish Pass

**Status:** merged to `main` as `77176eb`..`392616a` (five commits)
**Gate on merged `main`:** 666 tests across 97 suites - 589 executed, 77 opt-in gallery tests skipped; 0 failures, 0 errors. Lint 0. Release invariants clean. `assembleDebug` succeeds.
**Evidence:** `review/audit-2026-10-28/evidence/`. These are Robolectric renders of real screens, not device screenshots.

> **Scope.** This pass went through every screen, sheet, dialog, menu and state in the app, with no restriction on what could be criticised, earlier work included. It then went past looks into how the player settings behave.

---

## 1. Method and limits

- **How screens were checked.** An opt-in screen gallery (`ui/visual/*GalleryTest`, see DESIGN.md §6.4) renders real composables to PNG with representative data:
  - library at phone and tablet width, Light and Dark, empty, loading, error, limited-access, selection and search states;
  - Settings and onboarding;
  - details sheet, every dialog and menu;
  - player HUD, every player sheet and every tab.
- **How problems were found.** Before and after renders were compared side by side, and the critique used the design-critique checklist: hierarchy, consistency, copy, touch targets and state coverage.
- **Evidence stays current.** The before/after pairs in `01`, `02` and `04` still describe the current screens. Where a later change made the old "before" state irrelevant (`03`, `05`-`08`), the index uses after-only renders, so no image claims a state the code no longer has.
- **What was not possible.** No emulator could run here because the container has no KVM, so nothing was checked on a device. TalkBack, real touch feel, animation timing and true Bluetooth latency are **unverified**.

## 2. What changed

### 2.1 Shared design kit (`77176eb`, `7148892`)
- **New shared components.** `ApturaDesignKit` (dialog, buttons with tones, menu rows, section titles, notice banners, skeletons) and `PlayerSheetChrome` (sheet header, tabs, section label). Every screen now uses them instead of its own variants.
- **Library.**
  - Flat cards without dead space, a titled grid, and an aligned search row.
  - The selection bar is one row with an exit.
  - Loading uses skeletons.
  - Empty and loading states keep an app bar, so Settings is never a dead end.
  - Two grid columns down to 320dp width.
- **Details sheet.** The video and Play come first, then quick actions, then grouped specs.
- **Settings and onboarding.** Sentence case and plain-language copy throughout.
- **Player.**
  - One header and tab style across sheets.
  - Thumbnails in the queue and Up Next.
  - The error card scrolls.
  - A running sleep timer or A-B loop shows under the title.

### 2.2 Player sheets rebuilt on one set of controls (`95231ca`)
- **Wrong-way audio sync preset fixed.** A positive offset delays the sound, and Bluetooth headphones are already late, so the "Buds (+150ms)" preset made sync **worse**. Device presets were removed. The control now says in words which way the sound moves.
- **Shared player controls.** Section panel, switch row, origin-aware slider, timing control, option row and chip; see DESIGN.md §5.6. Every tab was rebuilt with them:
  - **Audio:** EQ faders are drawn at their real height and show units, and each effect has a single slider.
  - **Subtitles:** the same timing control as audio, and the style options use selectors.
  - **Playback tools / Picture:** real switches instead of ON/OFF pills, and contrast and colour shown as relative percentages.

### 2.3 Player settings behaviour (`08a41fb`)
Implements the outstanding parts of FR-CTRL-009 and improves the sync controls:

| Change | Why |
|---|---|
| Sleep timer fades the sound out over the last 10 s, then restores volume | A sleeper should not be woken by the audio stopping dead. The fade uses the player's own volume, not the system volume. |
| "+5 min" on a running timer | The extend affordance in FR-CTRL-009. Picking a duration again would restart the countdown, not extend it. |
| "At the end of this video" | Required by FR-CTRL-009 ("end-of-current-item"). It works even under Repeat one, does not start Up Next, and fires once. |
| Press and hold on sync -/+ | Moving from 0 to 1 s took twenty taps. A tap is still exactly one step. |
| "Try -200 ms" when Bluetooth audio is connected | A one-tap starting point in the right direction. It is worded as a suggestion because latency varies by device. |
| Frame step shows `h:mm:ss.mmm` | Whole seconds cannot tell frames apart. |
| Reset names what it resets | "Reset this video's settings" did not say what it would change. |

New controller API: `PlaybackController.setStopAfterCurrentItem` and `PlaybackController.setVolume`, implemented in Media3.

### 2.4 Delete is recoverable by default (final pass)
- **Hierarchy flipped.** "Move to trash" is now the filled primary action. "Delete permanently" is an outlined red alternative, and Cancel stays last. When trash is unavailable, permanent deletion is still the only action and keeps its destructive styling.
- **Icon tone is independent.** `ApturaDialog` gained `iconTone`, so the delete icon stays red even when the primary action is the non-destructive one (`iconTone` defaults to `confirmTone`).
- **Copy states the consequence.** The message says the item can be restored from Android's trash, and that permanent deletion cannot be undone. Bulk deletes use the same hierarchy and pluralised copy.

### 2.5 Launcher mark
- A cyan aperture disc with a play-triangle opening and three curved iris seams replaces the old placeholder foreground. The geometry is 120-degree symmetric, keeps a circular silhouette at 48 px, and shares its colours with the in-app accent. `docs/assets/logo.png` is the same mark on the project plate.

## 3. Verification

- **Full gate.** `tools/gate.ps1` on merged `main`: 666 tests / 97 suites, 0 failures, 0 errors, 77 skipped (the gallery tests are opt-in). Lint reports 0 issues, release invariants pass, and `assembleDebug` succeeds.
- **ViewModel tests:**
  - the fade, including volume returning to full after the pause;
  - cancelling and extending mid-fade;
  - stop at end of video without starting Up Next, and that it fires once;
  - a timed countdown replacing end-of-video.
- **UI tests:**
  - a tap moves exactly one step;
  - a one-second hold repeats, and releasing adds no extra step;
  - audio sync moves in the right direction.
- **Updated tests.** Existing UI tests were updated for the new copy and controls.

## 4. Open items

1. **Device pass.** Check on a phone and a tablet:
   - TalkBack on the new slider and the hold-to-repeat buttons;
   - the fade on real hardware;
   - whether the Bluetooth suggestion is detected on A2DP and LE Audio devices;
   - the new launcher mark at 48 px and in themed-icon mode.
2. **Sleep timer gaps against FR-CTRL-009.** Closed on `agent/opencode/sleep-timer-and-review-fixes` (PR #4): the
   full 10/15/20/30/45/60 ladder, a 1-720 minute custom duration, and a `+5 min` offer that resumes playback after
   the timer has fired.
3. **Global audio sync.** There is no "apply this audio sync to every video" option. Per-video storage cannot tell "0" apart from "unset", so this needs a schema decision first.
4. **Gallery limits.** Sheets do not scroll under Robolectric, and non-default picture adjustments cannot be rendered (TextureView). See DESIGN.md §6.4.

## 5. Evidence index

| File | Shows |
|---|---|
| `01` | Library, phone: before/after - spacing, cards, search row, Continue watching |
| `02` | Library, tablet: before/after - rail, grid density, header |
| `03` | Details in Dark and Light, plus the trash-first delete dialog |
| `04` | Empty state and overflow menu: before/after |
| `05` | Audio, subtitle and speed sheets after the rebuild |
| `06` | Audio tabs: tracks, equalizer, effects |
| `07` | Subtitle tabs: tracks, timing, style |
| `08` | Playback and Picture tabs |
| `09` | Sleep timer with "+5 min", fade note and end-of-video switch |
| `10` | Audio sync with Bluetooth connected |
| `11` | Frame step with millisecond position, bookmarks |
