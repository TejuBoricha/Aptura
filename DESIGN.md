# Aptura — Design System & Visual Specification (`DESIGN.md`)

> **Package:** `org.aptura.player`  
> **Source of Truth:** [spec/06_UX_SCREEN_AND_STATE_SPECIFICATION.md](file:///c:/Users/tejas/Desktop/Mobile%20App/spec/06_UX_SCREEN_AND_STATE_SPECIFICATION.md)  
> **Target Framework:** Android Jetpack Compose (Material 3 baseline with bespoke media extensions)  
> **Purpose:** Authoritative UI/UX contract for AI-assisted code generation. Every screen, composable, and theme definition MUST conform to the tokens and rules in this document.

---

## 1. Aesthetic Vision & Philosophy

Aptura is a **precision optical instrument for personal media**. 
- **Distraction-Free Immersion:** In playback mode, content is the hero. The interface retreats into invisibility until summoned. The player is **always dark**, even when the app is in light mode.
- **Cinematic Dark-First, Not Dark-Only:** The dark palette remains the design baseline, engineered for OLED/AMOLED displays with true `#000000` black in the player. The app follows the system theme by default and can be pinned to Light or Dark.
- **Anti-Slop Guarantee:** Zero generic cookie-cutter templates, zero childish saturated gradients, zero rounded bubbly cards with no purpose. Every element is deliberate, refined, and tactile.

---

## 2. Color Tokens & Semantic Roles

Appearance is a user setting (`ThemeMode` = `System` / `Light` / `Dark`, plus one of five accents: Cyan, Blue, Violet, Amber, Rose). `ApturaTheme` swaps one complete palette at a time, so a token always means the same thing. The dark palette below is unchanged from the original specification; the light palette is its daylight counterpart. The player and its menus always resolve to the dark palette.

### 2.1 Surfaces & Backgrounds (Dark Mode Baseline)
| Token | Hex Value | Semantic Role / Usage |
|---|---|---|
| `SurfacePureBlack` | `#000000` | Fullscreen video background, letterbox fill, player scrim base |
| `SurfaceBase` | `#0D0E11` | Primary library and folder view background |
| `SurfaceElevated1` | `#16181D` | Standard media cards, list item surfaces, top app bar |
| `SurfaceElevated2` | `#20232A` | Dialogs, bottom sheets, search bar container |
| `SurfaceElevated3` | `#2B2F38` | Hover/pressed states, focused list items, active chips |
| `SurfaceRecessed` | `#00000080` | Recessed tracks: selector switches, folder chips |
| `SurfaceScrim` | `#000000CC` | 80% opacity overlay behind HUD controls during playback |
| `SurfaceGlass` | `#16181DB3` | 70% opacity blurred backdrop for quick-setting flyouts |

### 2.1b Light Palette (same token names)
| Token | Hex Value | Notes |
|---|---|---|
| `SurfacePureBlack` | `#FFFFFF` | The brightest surface in light mode (thumbnails, selected-chip labels) |
| `SurfaceBase` | `#F5F6F8` | Primary background |
| `SurfaceElevated1` | `#FFFFFF` | Cards and panels |
| `SurfaceElevated2` | `#FCFCFD` | Dialogs and sheets |
| `SurfaceElevated3` | `#EDEFF3` | Pressed/active states |
| `SurfaceRecessed` | `#0F172A0F` | 6% ink recessed track |
| `SurfaceScrim` | `#0A0C10CC` | Scrims stay dark over imagery |
| `SurfaceGlass` | `#FFFFFFB3` | Light glass |
| `BorderSubtle` | `#0F172A1F` | 12% ink hairline |
| `TextHighEmphasis` | `#111418` | 16:1 on the light base |
| `TextMediumEmphasis` | `#4B5563` | 7:1 on white |
| `TextLowEmphasis` | `#6B7280` | 4.8:1 on white |
| `TextOnAccent` | `#FFFFFF` | Light accents are dark enough for white labels |
| `TextOnScrim` | `#F0F4F8` | Fixed light text over dark scrims, in either mode |
| `StatusSuccess` | `#087A42` | Darkened for 4.5:1 on white |
| `StatusWarning` | `#9A6B00` | Darkened for 4.5:1 on white |
| `StatusError` | `#C62828` | Darkened for 4.5:1 on white |
| `StatusInfo` | `#0B6FA4` | Darkened for 4.5:1 on white |

### 2.2 Brand & Functional Accents
Each accent has a dark and a light primary/secondary variant; the active pair is part of the palette.

| Accent | Dark primary | Light primary | Role |
|---|---|---|---|
| Cyan (default) | `#00E5FF` | `#00707F` | **Aptura Electric Cyan** — active states, scrubber fill, selection |
| Blue | `#6EA8FF` | `#1D4ED8` | Cool secondary identity |
| Violet | `#B388FF` | `#6D28D9` | Expressive secondary identity |
| Amber | `#FFC857` | `#8A5300` | Warm secondary identity |
| Rose | `#FF6E9C` | `#B3124B` | Warm secondary identity |

`AccentSubtle` is the active primary at 15% alpha (dark) or 12% alpha (light). `BorderSubtle` is `#FFFFFF26` in dark and `#0F172A1F` in light.

### 2.3 Semantic State Colors
| Token | Hex Value (dark) | Role |
|---|---|---|
| `StatusSuccess` | `#00E676` | Verified format badges, completed scans, healthy states |
| `StatusWarning` | `#FFD600` | Unsupported audio channel fallback notice, high thermal warning |
| `StatusError` | `#FF5252` | Corrupted media files, decoder failure fallback states |
| `StatusInfo` | `#40C4FF` | Format discovery tags, informational badges |

### 2.4 Typography Color Scale
| Token | Hex Value (dark) | Contrast Ratio on `SurfaceBase` | Role |
|---|---|---|---|
| `TextHighEmphasis` | `#F0F4F8` | 16.2:1 (Exceeds WCAG AAA) | Video titles, primary labels, current timecode |
| `TextMediumEmphasis`| `#9BA3AF` | 7.8:1 (WCAG AAA) | Video duration, file resolution, folder item counts |
| `TextLowEmphasis` | `#606978` | 4.6:1 (WCAG AA) | Inactive icons, subtitle track codecs, disabled states |
| `TextOnAccent` | `#001217` | 17.5:1 on Cyan | Text inside primary cyan badges or action buttons |
| `TextOnScrim` | `#F0F4F8` | - | Text/icons on a dark scrim over thumbnails |

### 2.5 Brand mark
The Aptura mark is a cyan aperture disc whose triangular opening is the play symbol. Three curved iris seams cut through the disc at 120-degree symmetry. The silhouette stays a clean circle at launcher sizes; the seams never become separated blades.

| Property | Value |
|---|---|
| Disc gradient | `#6FF3FF` to `#00A8C4`, top-left to bottom-right |
| Iris seams | `#151820`, rounded ends |
| Optional plate | Rounded square, `#171C25` to `#0D1016`, `#2A3441` hairline |
| Clear space | 25% of the mark diameter on every side |
| Minimum size | 16 px without the plate; 48 px when the plate is shown |

Canonical assets live in `docs/assets/` (`logo.png` with plate, `logo-mark.png` transparent). The launcher foreground in `res/drawable/ic_launcher_foreground.xml` uses the same geometry. Do not redraw the mark as three separated wedges, and do not add a tick outside the disc.

---

## 3. Typography Scale (Jetpack Compose `Typography`)

Font Family: the platform default (**Roboto** on Android), with tabular figures (`tnum`) for timecodes and counters. No font file is bundled, so the app follows the system font and the user's font scale.

```kotlin
// Jetpack Compose TextStyle Definitions
val ApturaTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        color = ApturaColors.TextHighEmphasis
    ),
    headlineMedium = TextStyle( // Library Section Headers
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        color = ApturaColors.TextHighEmphasis
    ),
    titleMedium = TextStyle( // Video Item Titles in Grid/List
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        color = ApturaColors.TextHighEmphasis
    ),
    bodyMedium = TextStyle( // Metadata (Resolution, Codec, Size)
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.2.sp,
        color = ApturaColors.TextMediumEmphasis
    ),
    labelSmall = TextStyle( // Badges, Pills, Scrub Timecode
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum", // Tabular figures for jitter-free timers
        color = ApturaColors.TextMediumEmphasis
    )
)
```

---

## 4. Spacing, Layout & Touch Targets

- **Base Spacing Grid:** Multiples of `4.dp` / `8.dp` (`4.dp`, `8.dp`, `12.dp`, `16.dp`, `24.dp`, `32.dp`, `48.dp`).
- **Minimum Touch Target:** `48.dp x 48.dp` for every interactive control (buttons, icons, chips, slider thumbs).
- **Responsive Width Breakpoints:**
  - **Compact (<600dp, Phones portrait):** Single-column media list or 2-column card grid. No persistent sidebar; folder chips and the bottom navigation stay in the content column.
  - **Medium (600–699dp, Foldables / Small Tablets):** **88dp compact rail** with Videos, Open, Refresh and Settings, plus the film count. Expand is not offered because 248dp would leave too little grid; folder chips remain in the content top edge.
  - **Expanded (≥700dp, Tablets / Desktops):** **248dp collection index** with the library pulse (films, runtime, footprint), All videos, folder rows with counts, Add folder, Open a file, Rescan, Settings and the offline promise. It collapses to the 88dp rail on demand; the choice is remembered for the session.
- **Large-text fallback:** At `fontScale >= 1.5`, force the 88dp icon rail at every width. The rail hides labels and keeps the icons' `contentDescription` as the accessible names. This is a layout decision, not a truncation: folder names must never be squeezed into unreadable fragments.
- **Sidebar motion:** Expand/collapse animates over 220ms with size, fade and a small horizontal slide. The compact header is one **68 x 48dp** chip that combines the aperture mark and the chevron; do not separate the mark and the arrow into a stack. Every sidebar control keeps a 48dp target.
- **Library selection bar:** Cap the selection action bar at **680dp** and center it under the search row at any width; do not let it stretch edge to edge on a tablet. It shows the count, Select all/Clear, Move, Share and the overflow; Back clears selection before leaving the library. Failed deletes stay selected.
- **Folder swipe:** A horizontal drag of at least **64dp** on the video grid moves to the next/previous folder. Disable the gesture while a selection is active.
- **System Gesture Zones:**
  - Honor `WindowInsets.systemGestures`.
  - Scrub bar thumb excludes bottom edge via `setSystemGestureExclusionRects` during immersive video playback.

### 4.1 Library File Action Contract

- **Selection:** Provide an explicit **Select** entry in every card overflow as well as long-press. Selection is a mode: Back clears it before navigating; a failed delete stays selected for retry; a successful delete clears the affected items.
- **Batch bar:** Count, Select all/Clear, Move, Share and overflow. Rename appears only for a single item. The bar is capped at 680dp and centered under the search row.
- **Rename:** Rename the file, not only the library row. Preserve the real extension when the user enters a title without one; prefer the current file's extension, then the MIME type. A dot is only an extension when the suffix looks like one. `DocumentsContract.renameDocument` allocates a **new** URI: persist it on the row and remap the bookmarks in the same operation, so the saved position and per-video settings stay attached and the next scan does not split the item into "missing" plus "new". A title-only update must not overwrite `displayName`.
- **Move:** Copy into the chosen SAF folder first and keep the source. Delete the originals only after the copy succeeds and Android grants deletion. On copy failure, retain and name the originals; on partial deletion, report both counts.
- **Transfer progress:** A transfer is a named job, not a spinner. Show the current file, real byte progress (fall back to file count when sizes are hidden), a percentage and a **Cancel** action. Block tap-outside dismissal. Cancellation stops between chunks, removes only the partial destination, keeps every original and keeps clipboard entries for retry.
- **Free space:** Ask the destination's provider root for `COLUMN_AVAILABLE_BYTES` and match it to the chosen tree by root id. If the provider reports no quota, attempt the copy and report the real write failure; if it reports a shortfall, refuse before writing and name both required and available sizes.
- **Durable transfer:** Persist the transfer journal before the first byte, record each completed source synchronously, and resume the same WorkManager job after rotation, navigation or process death. Resume an unfinished journal at process start and when the library observes a preempted run. Remove a recorded partial destination before retrying; never recreate a completed source. Post no transfer notification: R1 keeps the media-session notification exemption, so progress and Cancel stay in the in-app dialog.
- **Move results:** Report copying and deletion as separate outcomes. A denied deletion is **Copied, originals kept**, never "Move not completed": say the videos now exist in both places, name them (bounded to three plus a count), and tell the user to remove the originals only after checking the copies. Reserve **Move complete** for every source copied and every original deleted; a partial deletion states both counts.
- **Delete / trash:** Offer **Move to trash** only for MediaStore URIs on API 30+ where the system trash exists. Otherwise offer permanent deletion with explicit "cannot be undone" copy. The dialog names a single video or states the batch count.
- **Missing files:** A missing card exposes only **Video Details** and **Remove from library**; every action that needs bytes is hidden, not left to fail. The removal dialog states that nothing on disk is deleted, that saved position and bookmarks are removed, and that a returning file is added as new. Never auto-purge a missing row that a disconnected drive may bring back.
- **Clipboard:** Copy/Cut stores URIs. Paste consumes entries that transferred successfully and keeps failures for retry. Clear clipboard is always available.
- **Folders:** A 64dp horizontal swipe moves between folders; disable it while selecting.

---

## 5. Player HUD & Interaction States

### 5.1 Playback Scrub Bar (Timeline)
- **Track Height:** `4.dp` default, expands smoothly to `8.dp` on touch/drag.
- **Thumb Size:** `16.dp` diameter circle with `AccentPrimary` fill, scaling to `20.dp` while actively dragging.
- **Colors:** 
  - Played track: `AccentPrimary` (`#00E5FF`).
  - Buffered track: `AccentSubtle` (`#00E5FF26`).
  - Remaining track: `SurfaceElevated3` (`#2B2F38`).
- **Timecode Display:** Floating chip above thumb showing current position (`00:14:22 / 01:45:00`) in tabular figures.

### 5.2 HUD Visibility & Transitions
- **Auto-Hide Timeout:** **5 seconds** (`5000ms`, aligned with Media3 `DEFAULT_SHOW_TIMEOUT_MS`).
- **Enter Animation:** `fadeIn(animationSpec = tween(150))`.
- **Exit Animation:** `fadeOut(animationSpec = tween(250))`.
- **Double-Tap Seek Overlay:**
  - Geometry: **132 x 68dp** rounded card (18dp radius), not a full-height edge scrim.
  - Left double-tap: 42dp accent disc with a backward arrow, accumulated `-10s`, label **Back**.
  - Right double-tap: 42dp accent disc with a forward arrow, accumulated `+10s`, label **Ahead**.
  - Multi-tap accumulation: Subsequent taps accumulate `20s`, `30s`, `40s`; the overlay stays compact so the picture remains visible.
  - Force LTR arrow direction so a mirrored layout cannot reverse "forward"; expose the direction and accumulated delta semantically.

### 5.3 Swipe Gestures (Full-screen Video)
- **Left Vertical Swipe:** Brightness adjustment (0% to 100% overlay pill).
- **Right Vertical Swipe:** Media volume adjustment (0% to 100% overlay pill; hardware volume sync).
- **Horizontal Swipe:** High-precision scrub with fine-grained time delta indicator (`+0:15`, `-1:02`).

### 5.4 Playback Speed Rail (Settings and Player)

- **One source of truth:** `PlaybackSpeedOptions.all` = `0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0`. Settings, the player speed chip sheet and the advanced sheet import it; never retype the list.
- **One control:** a single horizontal rail, not a grid of cards. A live value bubble rides the finger; ticks rise from slower to faster; the ends name the range; the needle springs to the nearest detent on release.
- **Interaction:** tap anywhere to select the nearest detent; drag for continuous selection; haptic feedback when crossing a detent.
- **Accessibility:** the whole rail is one slider: `contentDescription` names the range, `stateDescription` names the value, `progressBarRangeInfo` exposes the steps, and `setProgress` changes it with TalkBack.
- **Large text:** the bubble is measured from the current font, not fixed; it grows instead of clipping at 200% type.
- **Player sheets:** apply `WindowInsets.navigationBars` and `imePadding()` before `verticalScroll`, so the last row is never behind the system bar or keyboard. Player sheets use the dark palette even when the app is Light.

### 5.5 Device Capability Contract

- **R1 form factors:** phones and tablets only (DEC-005). No TV, Automotive, Wear or desktop launcher entries; the merged-manifest invariants reject Leanback/Auto categories and their feature prefixes.
- **Picture-in-picture is optional:** declare `android.software.picture_in_picture` with `required="false"` and `android:supportsPictureInPicture="true"` on the player activity. One without the other is a bug: the first without the second advertises a capability nothing implements, the second without the first hides the app from devices that cannot float a window. The action is hidden whenever `DeviceCapabilities.supportsPictureInPicture` is false.
- **No hardware claims:** camera, telephony, location, microphone, Bluetooth and NFC feature declarations are forbidden, even as optional. A feature declaration is a store promise, not a comment.
- **Runtime checks live in `DeviceCapabilities`:** a screen must not call `hasSystemFeature` for a product-level capability on its own, or two surfaces can disagree about the same device.
- **Storage eras are explicit:** system trash is offered only for MediaStore URIs on API 30+, never on API 29, and never for a batch that mixes MediaStore with another provider.

### 5.6 Player Sheets and Settings Controls

Every player sheet (Audio, Subtitles, Playback tools, Speed, Queue) is built from one set of controls in `ui/player/PlayerSheetChrome.kt` and `ui/player/PlayerControls.kt`. A tab must not bring its own pill, slider or stepper.

- **Chrome:** `PlayerSheetHeader` (icon tile, title, one-line status subtitle that turns accent when something is active, close button) and `PlayerSheetTabs`. One header and one tab style for every sheet.
- **Sections:** `SheetSection` is a neutral uppercase label above a `SurfaceElevated1` panel with a 16dp radius and a hairline `BorderSubtle` border. An optional trailing action (Reset, Clear, +5 min) sits on the label row, not inside the panel.
- **On/off is always a real `Switch`** (`SheetSwitchRow`), and the whole row toggles it. Never use ON/OFF text pills.
- **One-of-N:** a short fixed set (sleep durations, aspect ratio, subtitle size) uses the Settings `SegmentedPicker`. A list with descriptions (audio tracks, decoder) uses `SheetOptionRow`, where a check marks the choice, not colour alone.
- **Sliders:** use `ApturaSlider` (4dp track, 20dp thumb). The fill grows from an `origin`, so "Normal" in the middle reads as zero, with a tick at the origin. It exposes `setProgress` and a spoken value. Picture values read as relative percentages ("Normal", "+10%", "−20%") and snap to whole percents.
- **Timing offsets** (audio sync, subtitle timing) use `TimingOffsetControl`:
  - a large readout with a signed value ("+150 ms", "−250 ms", using U+2212);
  - one sentence saying what the value does;
  - Reset when the value is non-zero;
  - 48dp −/+ step buttons that **repeat while held** (400ms delay, then every 90ms; a tap is exactly one step);
  - a slider snapped to the step;
  - both directions named in words under the slider.
- **Sign convention:** a positive audio offset delays the sound. Bluetooth headphones already play sound late, so their fix is **negative** ("Sound earlier"). When Bluetooth output is connected and sync is at 0, the Audio sync section offers a one-tap "Try −200 ms" starting point. Never ship device presets that push the offset the wrong way.
- **Sleep timer** (FR-CTRL-009):
  - The choices are Off / 15 / 30 / 45 / 60 min, plus an "At the end of this video" switch.
  - "At the end of this video" pauses when the item ends, even under Repeat one, and does not start Up Next. It fires once.
  - The sound fades out over the last 10 seconds using the player's own volume, never the system volume. Full volume returns after the pause and after cancel or extend.
  - A running timer offers "+5 min", which adds to the time left instead of restarting the countdown.
  - While a timer is set, the HUD shows "Sleep in m:ss" or "Sleep at end" under the title.
- **Frame by frame** shows the position to the millisecond (`1:02:03.417`).
- **Reset this video** names what it resets: speed, aspect ratio, decoder, and audio and subtitle sync.

---

## 6. Component Style Guide (Jetpack Compose)

### 6.1 Media Card (Video Thumbnail)
```kotlin
// Visual Attributes:
// Shape: RoundedCornerShape(10.dp)
// Elevation: 0.dp (flat surface, uses SurfaceElevated1 border/fill)
// Aspect Ratio: 16:9 thumbnail preview
// Badges: Bottom-right duration pill (SurfacePureBlack at 85% alpha, 4.dp padding, 4.dp rounded corners)
// Progress Indicator: 3.dp accent line across thumbnail bottom for partially-watched videos
```

### 6.2 Format & Codec Pills
```kotlin
// Shape: RoundedCornerShape(4.dp)
// Padding: horizontal 6.dp, vertical 2.dp
// Border: 1.dp solid SurfaceElevated3
// Background: SurfaceElevated2
// Text: labelSmall, uppercase, fontFeatureSettings = "tnum"
// Examples: "4K", "10-BIT", "HEVC", "ASS", "5.1"
```

### 6.3 Shared Design Kit (`ui/components/ApturaDesignKit.kt`)

Dialogs, buttons and menus come from the kit, so every screen has the same shapes, order and tone.

- **Buttons:** `ApturaButton`, `ApturaOutlinedButton` and `ApturaTextButton`, each with an `ApturaTone` (Accent, Neutral or Destructive). Destructive actions are never Accent.
- **Dialogs:** `ApturaDialog` has a fixed button order: the dismiss action on the left, the confirming action on the right, and an optional `DialogAlternative`. `DialogAlternative` carries an `ApturaTone`, so a destructive alternative is outlined red rather than inheriting the primary tone. `iconTone` defaults to `confirmTone` but can be set independently: a delete dialog whose primary action is recoverable still shows a destructive red icon. Dialogs that ask for a choice use `DialogOptionRow`.
- **Destructive hierarchy:** make the recoverable action primary. "Move to trash" is the filled button; "Delete permanently" is the red outlined alternative. When no recoverable path exists, permanent deletion is the only action and keeps the destructive tone.
- **Menus:** `ApturaMenuItem` (icon plus label, destructive in `StatusError`) and `ApturaMenuCaption`.
- **Structure:** `SectionTitle` for headings, `NoticeBanner` for non-blocking notices with actions, and `SkeletonBlock` for loading placeholders.
- **Copy:** use sentence case everywhere ("All videos", "Reset to defaults"), plain language over jargon, and say the consequence of an action, not only its name.

### 6.4 Design Review Gallery

`app/src/test/.../ui/visual/` renders real screens, sheets and dialogs with representative data to PNG through Robolectric native graphics. It is a review tool, not a regression test, and runs only when `APTURA_GALLERY_DIR` is set:

```
APTURA_GALLERY_DIR=/tmp/gallery ./gradlew :app:testDebugUnitTest --tests '*GalleryTest*'
```

- Run the gallery classes one at a time if the suite timeout trips.
- Picture adjustments stay at their defaults in gallery states, because a non-default adjustment switches the video to a `TextureView`, which cannot be drawn to a software canvas.
- Sheets show only what fits on screen and do not scroll under Robolectric. For long tabs, render the tab content directly; `tPlaybackContent` is an example.

---

## 7. Explicit Design Guardrails (DOs & DON'Ts)

### ❌ NEVER DO:
1. **Never use saturated purple or pink gradients** as card or background fills.
2. **Never hardcode white or black backgrounds.** Use the active `Surface*` tokens. In Light mode `SurfacePureBlack` is white by design (thumbnails, selected-chip labels); in the player it is true black. A literal `Color.White` or `Color.Black` outside the player root is a bug.
3. **Never render text directly over video** without an alpha-controlled scrim (`SurfaceScrim`).
4. **Never create touch targets smaller than 48dp** on mobile screens. The compact sidebar chip is 68 x 48dp, not 34 x 48dp.
5. **Never drop frame rates** for animations — use Compose `Animatable` or `animateFloatAsState` with hardware-accelerated transforms (`graphicsLayer`).
6. **Never use generic standard Android dialogs** with default light themes; always theme dialogs with `SurfaceElevated2` and `ApturaTypography`.
7. **Never ship a control that is not wired.** No fake success, no placeholder toast, no "Picture in Picture" entry on a device without the feature, no speed value that only one surface can set.
8. **Never show light player chrome when the app is Light.** The player and every sheet it opens use the dark palette, including the speed, subtitle, audio, queue and advanced sheets.
9. **Never build a one-off control inside a player sheet.** Use the player controls in §5.6; a new need extends them.
10. **Never label a timing offset with a bare number or a device name.** Say which way the sound or text moves.

### ✅ ALWAYS DO:
1. **Always use tabular digits (`tnum`)** for timecodes, durations, bitrates, and counters to prevent UI jitter.
2. **Always respect edge-to-edge window insets** (`WindowInsets.safeDrawing`, `displayCutout`, `statusBars`, `navigationBars`). Apply insets before `verticalScroll`, not after.
3. **Always provide haptic feedback** (`LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)`) when crossing seek markers or scrubbing.
4. **Always ensure WCAG 2.2 AA (minimum 4.5:1) contrast** for all informative labels and icons.
5. **Always test 200% font, 600/699dp, 700dp and the scrolled top bar.** A layout that only works at the reviewer's window is not finished.
6. **Always copy first, then delete.** A move is a successful copy followed by a consented deletion; a failed copy leaves the original untouched and names it.
7. **Always preserve the real file extension on rename.** The visible title may omit it; the file on disk may not.
8. **Always persist the URI a provider returns from a rename.** Move the row, saved position and bookmarks with it; a stable MediaStore URI is the exception, not the rule.
9. **Always make a long transfer observable, cancellable and durable.** Real byte progress, a named file, a working Cancel, no tap-outside dismissal, no originals deleted mid-copy, and a journaled WorkManager resume path.
10. **Always show the consequence before removing a missing item.** No disk file is deleted; progress and bookmarks are; the row returns as a new video if the file comes back.
