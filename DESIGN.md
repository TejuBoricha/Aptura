# Aptura — Design System & Visual Specification (`DESIGN.md`)

> **Package:** `org.aptura.player`  
> **Source of Truth:** [spec/06_UX_SCREEN_AND_STATE_SPECIFICATION.md](spec/06_UX_SCREEN_AND_STATE_SPECIFICATION.md)  
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
- **Short-window rule (a phone held sideways):** height decides as well as width. A current phone sideways is 891 x 411 dp and a small one 740 x 360 dp: wide enough for the tablet layout, a third of a tablet's height. The collection index needs about 450 dp before its first folder row, so it is used only at **600 dp of height or more**; below that the **88 dp rail** is used at every width, with the folder bar in the content (Browse folders, chips, Add folder). Below **480 dp** the rail drops its text labels (its icons keep their names for TalkBack), as it does at large text, so four buttons and the header fit in 360 dp. Spacing on the empty and permission screens tightens below 480 dp so the main button is in view. Any sheet whose content can be taller than 360 dp scrolls (the View sheet, the details sheet, dialog bodies). Below the same 480 dp (`SHORT_WINDOW_HEIGHT`, one constant in `ui/components/WindowSize.kt`) the **Continue watching** cards are **168 dp** wide instead of 248 dp, so the library under the shelf stays in view, and **Settings has no large title**: the compact bar (back button and title) is always there, the list starts under it, and that title is the page's heading for TalkBack. Upright phones and tablets are unchanged.
- **Large-text fallback:** At `fontScale >= 1.5`, force the 88dp icon rail at every width. The rail hides labels and keeps the icons' `contentDescription` as the accessible names. This is a layout decision, not a truncation: folder names must never be squeezed into unreadable fragments.
- **Sidebar motion:** Expand/collapse animates over 220ms with size, fade and a small horizontal slide. The compact header is one **68 x 48dp** chip that combines the aperture mark and the chevron; do not separate the mark and the arrow into a stack. Every sidebar control keeps a 48dp target.
- **Library selection bar:** Cap the selection action bar at **680dp** and center it under the search row at any width; do not let it stretch edge to edge on a tablet. It shows the count, Select all/Clear, Move, Share and the overflow; Back clears selection before leaving the library. Failed deletes stay selected.
- **Folder swipe:** A horizontal drag of **48dp** on the videos moves to the next/previous folder the moment the finger has gone that far (not when it lifts), and a quicker flick (500dp/s or more) counts on letting go, whatever its length; one move per touch (see 4.2). Disable the gesture while a selection is active.
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

### 4.2 Library Layout and Folder Navigation

The library is shown the way the person chooses, and the folder they are in is never in doubt.

- **View options** (the sliders button beside search) is one sheet with four choices, all kept in DataStore (never Room, so no schema change): **Layout** (Grid or List), **Size** (Compact, Comfortable, Large), **Details** (Title only, Standard, Detailed) and **Sort**. Choices apply at once, survive restarts, and read the same on every screen size. **Layout** is two picture cards, a small drawn grid and a small drawn list, so the difference is shown and not only named; **Size** and **Details** are tonal switches; the chosen sort row is tinted. All of them use the folder chips' language (a tint of the accent, a hairline of it, accent-coloured text) because filling the chosen option with the full accent made three stacked switches loud and flat (`ChoiceCard` and `TonalSegmentedPicker`, `ui/components/ApturaChoiceControls.kt`; every option is a radio button and at least 48dp tall, `ChoiceControlsTest`). The welcome guide's Grid/List switch is the same control. The measurements live in one object, `LibraryMetrics`, so the grid, the list and the guide's picture of them agree.
- **Grid** is an adaptive grid with a minimum cell width per size, not a fixed column count: Compact 100dp, Comfortable 132dp, Large 190dp on a phone (so 3, 2 and 1 columns on a 360dp phone), wider on tablets. **List** is rows with a 96, 132 or 172dp picture; one column on a phone, as many as fit on a tablet. Details: Standard is `size · folder`; Detailed is one fact per line (quality and size, folder, when added), so a narrow card never cuts a date in half.
- **Folder bar** (phone and medium widths): `Browse folders` (48dp, left) · scrolling chips · `Add folder` (48dp, accent, right). Both buttons are pinned outside the scrolling chips, so "Add folder" is always visible and never sits at the end of a long row. The chips scroll to keep the selected one centred, whichever way it was selected, and fade at an edge that continues.
- **Folder browser sheet:** every folder at once as rows or cover cards (a frame from the folder's newest video), with video count and runtime, and "Add folder" at the top. It is the fast route when there are many folders.
- **Swipe between folders:** a 48dp horizontal drag on the videos, or a quick flick, moves one folder along (All videos is the first stop). The new folder slides in and fades in over **170ms** (the old one slides out and fades over 100ms) with an easing that starts fast; the default easing starts slowly, which was most of why the slide felt like it was waiting. `FolderSwipeFeelTest` keeps the timing and the gesture. The chip bar, the sidebar row and a short **position pill** ("Movies, 3 of 6", bottom centre, a polite live region) follow it, with a light haptic tick. At the first or last folder it stays put (no wrap-around) and the pill still names the folder, so the end reads as the end. The pill sits at the bottom so it never covers a title.
- **Sidebar list (≥700dp):** the selected folder's row is scrolled into view however it was selected. The list's top and bottom edges are the panel's own colour for 12dp and then fade, so a row sliding under the library summary dissolves instead of being cut through its text.

- **Search hint:** the hint steps down to the longest wording that fits whole: "Search videos" (inside a folder, "Search <folder>"), then "Search", then nothing, because the magnifier beside it says it. It is measured with the real font size and never shows a cut word ("Search vi…" was the 360 dp phone). `LibrarySearchHintTest`.

- **Folder identity:** a folder is identified by where it is, not by its name. The library's folder key is the folder's path on its storage volume (`/storage/emulated/0/DCIM/Camera`, `/storage/1A2B-3C4D/DCIM/Camera`), taken from `RELATIVE_PATH` and `VOLUME_NAME` on Android 10 and newer (the documented, non-deprecated columns) and from the file path before that; the bucket name is only the last resort. This is how Photos, Files and the common gallery apps group folders, and it keeps two folders called Camera (the phone's and an SD card's) or two Downloads apart, each with its own videos, count and cover. People read the folder's own name; a name that two folders share gets what separates it, first the folder it is in and then the storage ("Camera (DCIM, Internal)" and "Camera (DCIM, SD card)"), and a name that is not shared is left alone (`folderLabels`, `MediaFolderKey`). Search matches the name you see, not the storage path. No Room schema change: the folder key is a text value that the startup scan rewrites on every video, keeping progress and hidden state (`FileReconciliationEngine`). `MediaFolderKeyTest`, `MediaStoreScannerFoldersTest`, `FolderLabelsTest`.

### 4.2b First Scan

On a fresh install the library is empty until the first scan ends (the scan saves its results in one transaction, so nothing appears gradually). The screen must say so plainly and must not flicker on the way:

- While the library is empty **and a scan is queued or running** the state is *Looking for videos…*, never "No videos yet". "No videos yet" (with Scan device and Add a folder) appears only once the library has stayed empty for 600 ms with no scan under way, because the database answers "nothing" a few milliseconds before WorkManager reports the scan. Without storage access there is no scan to wait for, so the permission screen appears at once.
- The loading skeleton fades in only after 300 ms, so a load that is over sooner (every normal start) shows nothing instead of a flash of grey blocks. It says "Looking for videos…" and "The first scan can take a little while."
- Guarded by `MediaLibraryViewModelTest` (the first-scan group) and `MediaLibraryScreenUiTest`, each shown to fail when its code is removed.

### 4.2c Video access

- **Settings > Library & storage > Video access** is the first row of the panel. It says whether Aptura may list the videos on this device ("Allowed. Aptura can list the videos on this device." or "Not allowed. Aptura only sees videos you open or add.") with one button: **Allow** asks the system again, **Manage** opens the system's page for the app, where access can be changed or taken back. Until now access could only be answered in the first-run dialog.
- **The answer is the system's, read again.** The app reads access every time it comes back to the front (`rememberVideoAccess`) and trusts the system's state, not the reply the dialog sent: on the second phone the report came from, access was granted and the library stayed empty, and on the next start the app asked again. A grant, from the dialog or from the system's settings, now reaches the library at once and scans immediately and again five seconds later (`MediaScanScheduler.scheduleScansAfterAccessGranted`; the second look is not a visible scan, so no spinner shows while it waits). When the system no longer shows its dialog (refused twice), asking opens the app's settings page with a one-line toast instead of doing nothing (`replyToVideoAccessRequest`). No new permission is requested and `READ_MEDIA_VIDEO` stays the only one (DEC-011).

### 4.3 Screen Transitions

- **One palette per screen.** The palette is a `CompositionLocal`, never a global: the always-dark player wraps itself in `ApturaTheme(DARK)` while a Light-theme library may still be on screen beneath it during a transition, and each draws its own colours. A global palette flipped a frame after the route changed was the cause of the back-navigation flicker. One composable (`ApturaWindowChrome`) owns the status-bar icons and window background.
- **The player is never animated.** Its picture is a `SurfaceView` behind the window, visible through a hole; sliding or fading the player leaves the hole misplaced or shows the page beneath through it. Navigation 3 always stacks the newer screen on top, so the screen beneath cannot be revealed by fading either. Instead a flat cover is drawn **above** the picture inside the player route: opening cuts to black and lifts over 160ms; closing rises to the page colour over 120ms and the player leaves when it has covered everything. Every other route slides, as below.
- **Screens slide; nothing is faded.** Navigation 3's own defaults are a **700ms cross-fade** for going forward and back, and for the Back gesture a page that shrinks to 70% without ever fading and then disappears when the gesture ends. For most of that cross-fade two whole screens are half see-through on top of each other (the Settings title drawn through the library, the library's cards through the Settings page), and the shrunken page is still fully opaque on the frame it vanishes: both read as flicker, on every back navigation. Going forward the new page slides in from the right over the old one, which moves a fifth of its width to the left underneath, over **300ms** with an emphasized-decelerate easing; going back is the reverse. Both pages are opaque the whole time (a route that paints no background of its own gets one: the details page did not). The Back gesture drags the page along with the finger (it slides and shrinks to 92%, linearly, so it moves exactly as far as the finger), and letting go carries the same movement on to the edge: the gesture and the Back button use the same properties, so the page never waits and vanishes. `ScreenTransitionTest` counts blended pixels between two flat-colour screens in every frame and watches the page after the finger lets go.
- **Stable insets.** Screens that sit under the player's immersive bars use insets that do not change when the bars hide, so nothing jumps while a screen leaves.
- **Cold start.** The colour behind the first frame, which the system's launch screen also uses, follows the phone's day or night mode (`values/themes.xml` light, `values-night/colors.xml` dark; each equals the palette's base surface, and `LaunchBackgroundTest` keeps them so). The first frame is held until the saved theme has been composed, for at most 700 ms (`FirstFrameGate`), so no frame is drawn in a theme the person did not choose: before, someone who set Dark on a Light phone saw dark, then a light frame, then dark. `FirstFrameGateTest`.
- **Thumbnails return in the first frame.** A size-bounded in-memory cache of decoded frames means a grid that comes back from the player paints its pictures immediately; it is trimmed on memory pressure.

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

### 5.3b Audio-only Layout

In audio-only mode the headphone panel (artwork, title, reason, **Show video**) and the centred transport buttons must never share space. The panel has its own region: the upper half of the screen, below the top bar (72dp) and 44dp clear of the button cluster, the same whether the controls are showing or not so it never jumps. When that region is shorter than 250dp (a phone held sideways, a small screen) the panel is one row: a 56dp artwork beside the title and a 36dp **Show video** button. If even that does not fit it scrolls rather than overlap. `AudioOnlyLayoutTest` checks this at portrait, small portrait, large text, sideways and small sideways.

### 5.3c Stats Panel and the Controls

The stats panel (top right, up to 380dp wide) and the centred transport buttons must never overlap, and the panel must never cover the seek bar or the bottom row. In a landscape window too narrow to hold both side by side (every phone held sideways, and small tablets under about 1150dp) the buttons move to the left while the panel is open, and the panel is never wider than the room to their right (24dp margin, 12dp gap, 240dp at the least). It stops 120dp above the bottom edge in a landscape window and above the buttons in a portrait one, and it scrolls inside for the rows that do not fit: on the smallest window it shows the first rows. A tablet at 1280 x 800 has room for both and is unchanged. `StatsOverlayLayoutTest` checks every pressable control and the seek bar against the panel at seven window shapes.

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

### 5.5b A video that does not start

A video that is asked for must play, whatever state the player is in. The owner's Samsung S24 sat in "buffering" at 0:00 with no error for every video after the first, so no rule about *why* is enough: the player is **replaced**, because a wedged player (a codec that never answers, an audio output that never opens) cannot be repaired from outside, while a new one starts clean, like the first video after the app opens.

**Prevention first.** Leaving the player (Back, gesture or arrow) lets the video decoder go *before* the picture's surface is destroyed (`stopForDismissal`, called from the single back policy): a paused player keeps its decoder running and bound to the surface, and on some phones detaching a surface from a running decoder freezes the player for good (Media3 issue 1772 names Samsung). The playlist and position are kept; the saved position comes from the screen's state, not the player.

**Then the safety net.** While the player is meant to be playing and is not (not an error, not the end, not a person's pause, not the system holding the audio), a timer runs, **4 seconds** at a time. A slow disk is not a frozen player: while data is still arriving and there is not yet enough loaded to start (about 2.5 s ahead), the wait is extended up to three times; once enough is loaded and it still does not start, or nothing more arrives, the player is called frozen:

1. **First wait ends:** the video starts again, where it was, on a **brand-new player** with the same queue (`Media3PlaybackController.startOnFreshPlayer`). Track choices, speed, volume, repeat, shuffle and the audio and subtitle delays are carried over; the media session and the picture follow the new player (`currentPlayer`); the old one is released before the new one asks for a codec.
2. **Second wait ends:** the same on another new player with the **software decoder**, and with **only the video that was asked for** (the queue around it is dropped): better to play it than to fail on its neighbours.
3. **Third wait ends:** only then does the player say **"Playback Did Not Start"** (12 s in all), with Retry (which starts on a new player again) and Open in another app.

A player that reports `ERROR_CODE_TIMEOUT` (a surface or codec that never answered) is replaced at once. A start that needed a new player makes the controller **cautious for the rest of the session**: every later start begins on a new player, so the stall does not wait to happen again on every video. Playlist operations that add items *before* the playing one are avoided in the recovery path: with a media session attached, Media3 1.5.1 crashed the process on them in the connected tests.

Every step is written to the diagnostic report (`play.session.start` for each recovery with the player's steps, buffering and thread states; `play.error` when it gives up), so a report from a phone still says what happened. `Media3PlaybackControllerStallTest` covers each rung, the carried-over settings, the order of the hand-over, what happens to the queue and the final message. `PlaybackSmokeTest` (connected) plays H.264, HEVC, a 558-item queue and a second video after the first on the CI emulator, wedges a real player with a source that never delivers a byte and requires the video to play on the replacement, and requires the picture to follow a replaced player.

### 5.5d Scrolling a big library stays smooth

The owner's library has 558 videos and fast scrolling felt laggy. Every cell that scrolled into view started reading or making its picture at once, with no limit and no pause for cells that were only flashing past: a hard fling started 212 pieces of picture work and up to 30 ran together, taking the processor from the screen. Now (`VideoThumbnail.kt`):

- **Nothing starts during a fling.** The grid measures how fast it moves (`rememberFlingGate`: more than 12 cells per second is a fling) and the cells wait; 150 ms after it slows, the cells on screen get their pictures, 60 ms later. A cell is composed for as long as a few rows either side of the screen are, far longer than it is seen, so a fixed delay alone did not help (it still started 106).
- **At most three at once** (`ThumbnailWork`), at background thread priority, so the thread that draws the screen is served first. Work that is still waiting is dropped, never started, when its cell scrolls away.
- Pictures already in memory still appear in the first frame; the pictures of a fast scroll fill in a moment after it stops.

`ThumbnailScrollLoadTest` measures both: a hard fling starts at most 10 pieces of work (212 before), and at most 3 run together (30 before). Not measured on a phone.

### 5.5c Bottom sheets stay put

Every bottom sheet is `ApturaModalBottomSheet` (`ui/components/ApturaSheet.kt`), never Material's `ModalBottomSheet` directly. Material gives the sheet whatever speed a fling has left at the end of a list, and the sheet settles at the top with it; a spring that starts at its resting place with speed away from it swings past and back, so a flicked list lifted the whole sheet about 85 dp and dropped it again ("the screen dances"). The wrapper swallows the **upward** leftover speed only. Flicking down from the top of a list still closes the sheet, by distance or by speed alone. `ApturaSheetTest` measures the heading's top frame by frame after a flick and fails if a source file opens a plain `ModalBottomSheet`.

### 5.6 Player Sheets and Settings Controls

Every player sheet (Audio, Subtitles, Playback tools, Speed, Queue) is built from one set of controls in `ui/player/PlayerSheetChrome.kt` and `ui/player/PlayerControls.kt`. A tab must not bring its own pill, slider or stepper.

- **Chrome:** `PlayerSheetHeader` (icon tile, title, one-line status subtitle that turns accent when something is active, close button) and `PlayerSheetTabs`. One header and one tab style for every sheet.
- **Sections:** `SheetSection` is a neutral uppercase label above a `SurfaceElevated1` panel with a 16dp radius and a hairline `BorderSubtle` border. An optional trailing action (Reset, Clear, +5 min) sits on the label row, not inside the panel.
- **On/off is always a real `Switch`** (`SheetSwitchRow`), and the whole row toggles it. Never use ON/OFF text pills.
- **One-of-N:** a short fixed set (sleep durations, aspect ratio, subtitle size) uses the Settings `SegmentedPicker` (a library sheet and the guide use `TonalSegmentedPicker`, and a choice that is better seen than read, like Grid or List, uses `ChoiceCard`). A list with descriptions (audio tracks, decoder) uses `SheetOptionRow`, where a check marks the choice, not colour alone.
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

### 6.5 Welcome Guide (`ui/onboarding/`)

Five pages, each saying one thing: **Welcome** (what stays private), **Your library**, **In the player**, **Tools**, **Access**. It is shown on first launch and any time from Settings > Welcome guide.

- **The pictures are the app's own parts.** The library page lays out the real `LibraryHeader`, `FolderStrip` and `VideoGridCard`/`VideoListRow` at a phone's design width and scales them to fit (`GuideScreenFrame`); the player page uses the player's own `VerticalLevelGaugeHud`, `DoubleTapSeekRippleOverlay` and `HorizontalSwipeSeekHud`; the tools page carries the real `SpeedRail`. The guide therefore cannot show a screen the app does not have. Touches stop at the library picture (nothing opens a keyboard or a menu); its Grid/List buttons are live.
- **No stand-ins.** Before the first scan there are no videos of the person's own, so the guide draws six small film frames (`GuideStill`: sky, sun, three ridges) instead of a grey tile. When the guide is replayed from Settings and the library holds at least four playable videos, the library page shows **their own** videos, frames and folders. It falls back to the samples while the library is filtered, so a picture never contradicts its folder bar.
- **Accuracy is tested, not promised.** Every action the guide names in the ⋮ menu comes from one list (`GuideMenuActions`) that a test checks against the real menu; the 12 tools, the gestures and the Settings labels were each checked against the code, and the names follow the UI's own capitalisation ("Continue watching", "Welcome guide", "Extra folders").
- **Motion, with a reason.** Parts of a page fade and rise in as it is swiped to the centre (`reveal`, read in the draw phase, so a swipe recomposes nothing); an accent glow behind the pages drifts with the swipe, so the guide reads as one surface; the welcome rings spread outward; the player picture plays brightness, volume, double-tap and scrub in turn, only while its page is in view. With Android's animations off (animator scale 0) every picture rests on one frame that says the same thing, and a short window (a landscape phone) drops the decorative header so the headline comes first.
- **Built ahead, then kept.** Each page is a screen's worth of real composables (50 to 130 ms to build). The pager used to build a page on the frame a swipe began and discard it when the page was left, so every Next and Back, and every swipe, began with a hitch: measured with a scripted lap, one frame of 40 to 128 ms among frames of 2 to 5 ms in every page turn. Once the first page has settled (1.2 s), the others are built behind it one at a time (every 350 ms) and kept, and the worst frame of a page turn is 13 ms. The welcome rings, which spread forever, move only while their page is the one in front, so a kept page does not keep redrawing out of sight (`rememberGuidePageActive`). `GuideBuildAheadTest`.
- **Orientation and progress:** a step count ("2 / 5") beside **Skip** at the top, capsules at the bottom (announced once as "Page n of 5"), **Back** and **Continue** in a fixed footer. Back on the system gesture steps to the previous page. The pages' top and bottom edges fade to transparent (not to a flat band), so the glow behind is never cut.

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
11. **Always look at a new screen, sheet or dialog on a phone held sideways** (891 x 411 and 740 x 360 dp) as well as upright: nothing overlaps, every control is reachable, and anything taller than the window scrolls. `LandscapePhoneGalleryTest` renders the library, its sheets and dialogs, and Settings at both sizes.
