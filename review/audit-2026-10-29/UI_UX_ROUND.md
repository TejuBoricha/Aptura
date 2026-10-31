# UI/UX round, 2026-10-29: outcome record

The owner's list had six points. This file records points 1 to 5: what was found, the root cause, what changed, how it
was tested, what is not verified and what remains. Point 6 (feedback from the owner's screenshots) has **not started**;
the one defect a screenshot has already shown (the sidebar, below) is fixed and recorded here. Work is merged in PR #29 (`5c87aed`).

How to read the evidence: everything below was measured under Robolectric with native graphics (frame stepping, pixel
capture, semantics), not on a phone. Hardware testing is on hold at the owner's request and stays listed as
"Hardware verification" in `.agents/STATE.md`.

## Point 5. Back navigation flickers

**Reproduced first, on `main` (391c0a8).** A frame-stepped capture of opening and closing the player in the Light theme,
using the old wiring (one global palette, flipped by a side effect, default slide transitions). `corner` is a pixel of
the page background, `swatch` is a pixel of the screen's own surface element, one row per 16 ms frame:

| Old wiring | frame | corner | swatch | what is on screen |
|---|---|---|---|---|
| open | 0-1 | F5F6F8 | FFFFFF | the light library, as it should be |
| open | 2 | **0D0E11** | **16181D** | the library has turned **dark** while it slides out (palette flipped a frame after the push) |
| close | 2-8 | 000000 | **FFFFFF** | the player's own surface has turned **white** while it leaves |
| close | 9-23 | 010101 ... A8A9AA | FFFFFF | still climbing through grey at frame 23 (about 380 ms after the pop began) |

Dark and system-dark users never saw it, because the colours it flipped between were the same. That is why it looked
intermittent.

**Root cause.** `ApturaColors` was a process-wide singleton that `apply()` rewrote one frame after the route changed.
For that frame (and for the whole slide) every screen on stage painted with whichever palette was current. Compounding
it: the player's picture is a `SurfaceView` behind the window, so sliding or fading the player leaves its hole misplaced
or shows the page beneath through it; Navigation 3 always stacks the newer screen on top (it overrides `zIndex` with the
back-stack depth), so the screen beneath cannot be revealed by fading either; the player hid and showed system bars, which
moved insets under a leaving screen; and a returning grid decoded its thumbnails again, so it painted empty for a frame.

**Fix (commit 10d6fe4).** Palette is a `CompositionLocal`; the player wraps itself in its own dark theme; one composable
owns the status-bar icons and window background; the player is never animated and a cover drawn above its picture opens
(black, lifting over 160 ms) and closes (rising to the page colour over 120 ms) it; stable insets; a size-bounded
in-memory thumbnail cache, trimmed on memory pressure.

| New wiring | frame | corner | swatch | what is on screen |
|---|---|---|---|---|
| open | 0-1 | F5F6F8 | FFFFFF | the light library |
| open | 2-3 | 000000 | 000000 | a cut to black; the library never turned dark |
| open | 4-12 | 000000 | 020303 ... 16181D | the player's controls rise out of the dark |
| close | 0-2 | 000000 | 16181D | the player, unchanged |
| close | 3-10 | 0D0D0D ... F5F6F8 | 222428 ... F5F6F8 | one smooth ramp from the player to the page colour (about 130 ms) |
| close | 11-23 | F5F6F8 | FFFFFF | the library, in its own palette, settled |

Palette reads during the transitions: the library read only the light palette, the player only the dark one, and neither
was ever recomposed with the other's. The grid keeps its scroll position (item 37, offset 12 in the test) across a trip to
the player.

**Tests:** `PlayerRouteTransitionTest` (open, close, scroll restoration), `ApturaThemeTest`, `ThumbnailBitmapCacheTest`,
`VideoThumbnailTest`, the palette-read probes in the settings and guide pixel tests.

**Not verified:** a physical device, and 60/90/120 Hz displays (every animation here is time-based, so it follows the
display). The cold start where the pinned theme differs from the system theme, left open here, is fixed below ("The items left open").

## Point 1. Folder navigation, visibility and organisation

- **Findings.** While swiping, nothing said which folder you were in once its chip scrolled away; "Add folder" sat at
  the end of a long chip row; the sidebar did not scroll the selected row into view; spacing and hierarchy of the header
  were uneven.
- **Changes.** A folder bar with Browse folders (left) and Add folder (right) pinned outside the scrolling chips; chips
  centre on the selected folder however it was selected; a folder browser sheet (rows or cover cards, with count and
  runtime); a position pill ("Movies, 3 of 6", bottom centre, live region) and a light haptic on each folder swipe, with
  no wrap-around at the ends; the sidebar scrolls the selected row into view.
- **Found after the owner's screenshot (sidebar, tablet).** A row that scrolled under the "N videos" card was cut
  through its text ("Tutorials 37"). The list edges are now the panel's colour for 12 dp and then fade
  (`LibrarySidebarScrollEdgeTest`; removing the solid stretch makes it fail).
- **Tests:** `FolderNavigationUiTest`, `FolderLabelsTest`, `FolderSwipeTest`, `MediaLibraryViewModelTest`,
  `LibrarySidebarScrollEdgeTest`. Two defects found by these tests while building: a stale callback in the swipe and a
  null folder ("All videos") being treated as "no target".

## Point 2. Customisable folder and video layouts

- Grid or List, three sizes, three levels of detail and the sort order, in one **View options** sheet; stored in
  DataStore (not Room: no schema change, no hot file), applied at once, restored on start. Adaptive grid by minimum cell
  width so the same choice gives two columns on a phone and five on a tablet; a Large size is one big card on a phone on
  purpose. Detailed shows one fact per line (an earlier version cut a date in half).
- **Tests:** `LibraryViewSheetTest`, `LibraryMetricsTest`, `VideoMetadataTest`, `ApturaSettingsRepositoryTest` (round trip,
  and unknown stored values fall back to the defaults), `LibraryLayoutGalleryTest` (render at phone, small phone, medium,
  tablet, landscape).

## Points 3 and 4. Welcome guide and replay guide

- **Findings (replay).** Thumbnails were flat gradient tiles; the library picture was hand-drawn and did not match the
  app (an "All Videos" chip with different capitalisation, no folder bar buttons); the ⋮ menu line promised "play audio
  only", "open in another player" and "rename", none of which that menu offers; "Continue Watching" and "Welcome Guide"
  used capitalisation the app does not. Checked against the code, all twelve tools, all gestures and the Settings labels
  are real; the menu line and two labels were wrong.
- **Redesign.** Five pages, each saying one thing. Pictures are the app's own parts: the library page lays out the real
  `LibraryHeader`, `FolderStrip` and `VideoGridCard`/`VideoListRow` at a phone's width and scales them (with a Grid/List
  switch to try); the player page uses the player's own brightness, volume, double-tap and scrub indicators and plays
  through them while it is in view; the tools page carries the real speed control. A glow behind the pages drifts with the
  swipe, parts of each page arrive in turn as it is swiped to the centre (read in the draw phase, no recomposition),
  there is a step count beside Skip. No flat tiles: before the first scan the guide draws six film frames; replayed from
  Settings with at least four playable videos, it shows the person's own videos, frames and folders.
- **Orientation and size.** Tablet: the phone picture keeps its width and sits centred. Landscape phone: a one-row
  footer (dots, Back, Continue) so the page keeps two thirds of the height, and the decorative header is dropped on short
  windows. 200% font and a 320 dp phone: nothing is cut off (renders below).
- **Motion off.** With Android's animation scale at 0 every picture rests on one frame that says the same thing.
- **Tests (new):** `GuideDemosTest` (real parts present; a drawn film frame on every card in grid and in list; touches
  stop at the picture; Grid/List switch; the player demo plays each gesture in turn while its page is in view, and stops
  when it is not; the gauges follow the finger), `GuideAccuracyTest` (opens the real ⋮ menu and checks every action the
  guide names; film frames are never flat and are deterministic), `GuideLibrarySourceTest` (the replay uses their library
  and falls back to samples when it is small or filtered), `OnboardingLandscapeTest`; `OnboardingScreenUiTest` and
  `GuideFooterPixelTest` updated. Mutation checks, each run and each failing for the reason intended: thumbnails replaced
  by the library's blank placeholder; the touch shield removed; the demo ignoring whether its page is in view; the menu
  line naming an action the menu lacks; the compact footer removed.

## Added after the owner's questions: a phone held sideways, and the audio-only overlap

The owner asked whether the whole app works flawlessly sideways on a phone (not a tablet) and sent a screenshot of the
audio-only player with its buttons drawn over the artwork. Until then only the guide, the player and some library
renders had been looked at sideways. Every screen was then rendered at 891 x 411 dp (a current phone) and 740 x 360 dp
(a small one), `LandscapePhoneGalleryTest`.

| Finding | Cause | Fix | Guard |
|---|---|---|---|
| Audio-only: transport buttons over the headphone artwork and the title (owner's screenshot, portrait; also sideways) | the panel and the button cluster were both centred on the screen | the panel has its own region between the top bar and the cluster; one row when that region is under 250 dp; scrolls rather than overlap | `AudioOnlyLayoutTest` failed at all five window shapes before the change |
| Library sideways: no folder list, no way to add a folder | 891 dp wide gets the tablet sidebar, whose fixed parts need about 450 dp before the first folder row | the index needs 600 dp of height; below that the rail with the folder bar is used | `LibrarySidewaysTest` (fails if the height rule is removed) |
| Rail at 360 dp tall: Settings cut off | four labelled items and the header are taller than 360 dp | labels dropped below 480 dp (icons keep their names) | same (fails if the labels stay) |
| View options sheet could not reach "Sort by" on a short screen | my point 2 sheet had no scrolling | scrolls | same (fails if scrolling is removed) |
| Permission and empty screens: main button at the very edge | tablet spacing | tighter spacing below 480 dp | same (fails if the spacing stays) |

Two things about how the fixes were checked. First, the permission-screen guard did not fail when its fix was removed at
first: the fix measured the window inside a scrolling box, which is given unlimited height, so it could never have taken
effect. The mutation check showed it; the measurement now sits outside the scroll and the guard fails without the fix.
Second, the renders also showed that the player's bottom sheets open as a strip on a 360 dp-tall window under
Robolectric; the sheets scroll and use Material's own sizing, and under Robolectric sheets are known not to lay out as
they do on a device, so this is **not verified** and belongs with the device checks.

### First install: the scan indicator that flashed (owner's report)

The owner said that on a first install the scan symbol appears for a few milliseconds and feels off. Traced in code: the
library's state starts as *Loading* (a grey skeleton) and the first database answer, an empty list, turned it into *No
videos yet* (with Scan device and Add a folder) at once, although the first scan was only then being queued; the scan
saves everything in one transaction, so the videos arrive all at once at the end. That is three screens in quick
succession: skeleton for milliseconds, an empty screen, then the library. The state is now *Looking for videos…* for as
long as the library is empty and a scan is queued or running; an empty library must hold for 600 ms with no scan before
it is called empty; the skeleton appears only after 300 ms. `MediaLibraryViewModelTest` (first-scan group) and
`MediaLibraryScreenUiTest` guard each part; four mutations (no Loading while scanning, no hold, a held-back permission
screen, an instant skeleton) each failed the intended test. Which symbol the owner meant is inferred from the code, not
seen on a device: if it is a different one, a screenshot will say.

**Seen sideways and first left open** (fixed below): the stats panel covers the right half of the controls when both are
showing; Settings' large title takes a third of the height until scrolled; the Continue watching cards are large on a
short screen, so the grid starts below the fold.

## The items left open, fixed afterwards (the owner asked for them)

Every one has a render from the code before and after at the same size and state, and a test that fails when the fix is
removed (each mutation was run and caught).

| Item | Cause | Fix | Tests |
|---|---|---|---|
| Search hint cut to "Search vi…" at 360 dp | The field shares a row with three 48 dp buttons, leaving about 80 dp for the hint, and the hint was one fixed string | The hint steps down to the longest wording that fits (full, "Search", nothing), measured with the real font | `LibrarySearchHintTest` (7) |
| Stats panel over the transport buttons, the seek bar and the bottom row | The panel hangs from the top right at up to 380 dp; the buttons are centred; the panel had no height limit and consumed taps | Landscape windows too narrow for both: the buttons move left and the panel is width- and height-limited; portrait: the panel stops above the buttons. It scrolls inside | `StatsOverlayLayoutTest` (7 window shapes; every pressable control and the seek bar are checked against the panel) |
| Settings' large title (a third of 360 dp) | A 64 dp gap and 36 sp title above the first setting, for a collapse effect a short screen has no room for | Below 480 dp there is no large title; the compact bar is always shown and is the heading | `SettingsShortWindowTest` |
| Continue watching cards large on a short screen | 248 dp wide cards are 140 dp tall pictures | 168 dp cards below 480 dp | `LibrarySidewaysTest` (sideways, small sideways, upright, tablet) |
| Same-named folders merged | `MediaStoreScanner` stored the bucket name (the folder's own name) as the folder key | The key is the folder's path on its volume (`RELATIVE_PATH` + `VOLUME_NAME` on Android 10+, the file path before); labels add the parent, then the storage, only on a collision; search matches the shown name. No schema change: the startup scan rewrites the value and keeps user data | `MediaFolderKeyTest`, `MediaStoreScannerFoldersTest` (through the real scanner and a fake provider), `FolderLabelsTest`, `MediaLibraryViewModelTest` |
| View options toggles looked heavy (the owner's report) | Three identical stacked bars with the chosen option filled with the full accent, and a Grid/List choice that was only words | Picture cards for Layout, tonal switches for Size and Details, tinted sort row; the guide's switch matches | `ChoiceControlsTest` (radio semantics, 48 dp targets), the existing sheet, guide and touch-target tests |
| Cold-start theme flash | The launch colour was always dark whatever the phone's mode; and the first frame was drawn in the system theme before the saved theme was read | The launch colour follows day/night; the first frame is held until the saved theme is composed, 700 ms at most | `FirstFrameGateTest` (6), `LaunchBackgroundTest` (3) |

Renders at the same sizes before and after are byte-identical where the screen has room (the 411 dp hint, an upright
Settings, an upright shelf, the stats panel on a 1280 x 800 tablet), which is the evidence that nothing else moved. While
testing the sideways stats panel the upright one was found to touch the play button too; it is fixed and tested.

**Not verified:** the cold-start change on a real phone, and the system's own launch screen (it draws an app icon over the
launch colour, which the renders do not include). The first-frame hold is tested as logic and as a view listener, and the
theme is tested at the resource level; the activity wiring itself is not under test.

**Decision on same-named folders (the owner asked for it to be taken on industry practice):** the library now identifies a folder by its volume and path, the way Photos, Files and the common gallery apps do (their `BUCKET_ID` is a hash of the directory), using the documented `RELATIVE_PATH` and `VOLUME_NAME` columns and the file path only before Android 10. Because the folder key is already a text column that the UI treats as a path (a folder the person added is one), this needed no Room schema change and no migration: the startup scan runs on every launch and rewrites the key on each video while keeping progress, per-video choices and hidden state. Not verified on a device with an SD card; the Android 10+ path is tested through the real scanner with a fake provider, and the older path through the key builder and the projection, because the Android 9 simulator is not available offline.

## After the owner's phone test (a Samsung S24 and a second phone)

The owner installed the debug build (52e0ec2) on a Samsung S24 and on another phone and reported five things. This is
the first time the app ran on a device in this round.

### No video started on the S24 (blocker)

**What was seen.** The player opens, shows the title and duration, the queue chip reads 558, the controls are up, and it
sits paused at 0:00 with a black picture. No error. Every video tried, picked at random from the gallery.

**What is established.** Nothing in my branch changed playback code (the controller, the view model, the codec selector
and the player manager are untouched; the player route's wrapper and the player screen's layout were). The CI `connected`
job runs the app on an API 35 x86_64 emulator with a software decoder: `PlaybackSmokeTest` puts a real 4 s H.264 + AAC
clip in MediaStore and requires the player to be playing it with the position moving. It passed for the controller and,
after the test itself was fixed (it first tapped a folder chip called "AppturaSmoke" instead of the video), for a tap on
the card in the library. So the pipeline from the library to a moving picture works on a clean device. The cause is
therefore specific to the S24, to its 558-video library, or to a codec or audio path the emulator does not have.

**What is not established: the cause.** Candidates, none confirmed: a hardware decoder (the S24's Exynos codecs) that
accepts the stream and never produces a frame; the audio sink or audio focus never letting the player start; a very
large queue. The owner can settle it with Settings > Share diagnostic report after trying one video.

**How it got past the gate.** No test in the repository ever played a video: the unit tests run on a JVM where there is
no codec, and the connected tests only opened the app and looked at the screen (launch, rotation, back). Device testing
was on hold at the owner's request, so the first time anyone watched a video play was the owner's.

**What changed.** (1) `PlaybackSmokeTest` (connected, CI): an H.264 clip, an HEVC clip, a 558-item queue started at item
301, and a tap on a card in the library; a failure names the player's state, play-when-ready, suppression, position and
error and includes the screen and the log. (2) A start watchdog in the controller: playback that has been asked for and
has not begun after 8 s tries the software decoder once, then shows "Playback Did Not Start" with Retry (DESIGN.md
§5.5b); nothing explained by an error, the end of the video, a person's pause or the system holding the audio back is
touched. (3) The player's state at a stall, and when playback was requested and when it started, are written to the
diagnostic report. Tests: `Media3PlaybackControllerStallTest` (6 tests; 8 mutations, each caught).

**Not verified:** on the S24. If it is a decoder stall, the software fallback should start it; if it is something else,
the message and the report will say what.

### Back navigation flickers (the Settings back gesture and "most back navigation")

**Reproduced first** with the app's real transition code, one picture per 16 ms (`BackShots`, before = `b7e3d57`):
going back from Settings was a 700 ms cross-fade in which the Settings title is drawn through the library and the
library's cards through the Settings page; and after a half-way Back gesture the shrunken page hung in place, fully
opaque, for about 190 ms and then vanished in a single frame.

**Root cause.** The commit before this one assumed "every other route keeps Navigation's default slide". In Navigation 3
1.0.1 the defaults are `fadeIn/fadeOut(tween(700))` for going forward and back, and for the gesture
`fadeIn + scaleOut(0.7)` with no fade-out (read from the library's own bytecode). Two opaque screens cross-fading are
half see-through for most of the move, and a page that ends a gesture opaque must disappear all at once.

**Fix.** Screens slide over each other, solid, in 300 ms (the page underneath moves a fifth of its width); the gesture
drags the page with the finger (slide and shrink to 92%) and the Back button uses the same properties, so letting go
carries the page on to the edge. The details route painted no background of its own, so it now does.
`ScreenTransitionTest`: no blended pixel between two flat screens in any frame of going forward, going back or the
gesture; the page keeps moving after the finger lets go. 5 mutations, each caught.

**Not verified:** on a phone, and at 60/90/120 Hz. The previous round's player-route fix (a cover above the video) is
untouched, and `PlayerRouteTransitionTest` still passes.

### Folder swipe is slow

Two causes: the swipe only counted after the finger had gone 64 dp **and lifted**, so a short flick did nothing and a
long swipe started moving once the gesture was over; and the slide used the default easing, which starts slowly. Now the
folder changes the moment the finger has gone 48 dp, a quick flick (500 dp/s) counts on letting go, and the same slide and
fades take 170 ms with an easing that starts fast. Measured on the real library screen: the new folder is half there
after 80 ms (112 before) and complete after 144 ms (240 before). `FolderSwipeFeelTest` (6 tests, 5 mutations caught).
Not verified: the feel on a finger.

### The welcome guide is laggy going back and forth

**Measured** with a scripted Next, Next, Next, Back, Back, Back lap (twice; frame times on the build machine's JVM, so
relative): every page turn had one frame of 14 to 128 ms (most 40 to 95) among frames of 2 to 5 ms. **Root cause:** each
page is a screen's worth of real composables (the library header, folder bar and cards; the player scene), the pager built
a page on the frame a swipe began and discarded it when the page was left, so every Next, Back and swipe began with a
build. **Fix:** once the first page has settled, the other pages are built behind it one at a time and kept; the welcome
rings move only while their page is in front. After: the worst frame of any turn is 13 ms. The cost moved to one pause
while the first page is being read (about 0.5 s on the measuring machine, cold). `GuideBuildAheadTest` (4 tests, 4
mutations caught). A debug build, which is what the owner installed, exaggerates any such hitch.

### Videos did not appear after access was granted; the app asked again

**What is established.** Access was read once when the app opened, and otherwise only from the reply to the app's own
dialog; the scan after a grant ran once. Access given or taken anywhere else (or a reply that never arrived) was never
seen until the app was closed and opened again, and Settings had no row for it at all.
**Not established:** why the first scan after the grant found nothing on that phone (the index still catching up is
the likeliest).
**Fix:** `rememberVideoAccess` reads the system's answer every time the app comes to the front and the permission result
handler trusts it over the reply; a grant starts a scan at once and another 5 s later (not shown as a visible scan);
Settings > Library & storage > **Video access** says "Allowed" or "Not allowed" with Allow (asks the system) or Manage
(the system's page for the app), and when the system no longer shows its dialog, asking opens that page. No new
permission. Tests: `VideoAccessTest`, `AccessGrantedScanTest`, `SettingsVideoAccessTest` (10 tests, 7 mutations caught).
The wiring in `Navigation.kt` (the effect that reacts to a change of access) is exercised by compiling, not by a test.
**Not verified:** on that phone.

## Verification

- Full unit suite: 933 tests, 0 failures, 162 skipped (the galleries, which run only when `APTURA_GALLERY_DIR` is set).
- Guide gallery renders reviewed at: phone, small phone (320 dp), 200% font, landscape, tablet, light and dark, and the
  replay with a library. The library layouts are rendered by `LibraryLayoutGalleryTest` at its listed sizes.
- `lintDebug`: 0 errors, 91 warnings (the same count as before the sideways, first-scan and later work), none in the guide files (a first run flagged two uses of `Configuration.screenHeightDp` there; they now use the window-size API). `assembleRelease` with `verifyReleaseApk`
  (which fails if the APK gains the INTERNET permission or a bundled software decoder): passes. Both ran online, because
  lint's instrumented-test model cannot resolve offline. CI runs them again on the PR.

## Known limitations and remaining issues

1. Not verified on a physical device or at 60/90/120 Hz (on hold at the owner's request).
2. The cold-start change is not checked on a real phone (above).
3. On the smallest sideways window (740 x 360 dp) the stats panel shows its first rows and scrolls for the rest, because
   the room above the seek bar is about 175 dp.
4. Same-named folders: fixed (above). Until the first scan after the update finishes, a video can still sit under its old folder name for a moment.
5. In landscape the guide is still one column that scrolls; a two-column layout (picture beside the text) would suit it
   better and was left out as decoration without a measured need.
6. The replay shows the person's own video titles and frames on the library page. It is on-device only and nothing is
   stored or sent, but a screenshot of the guide would show them.
7. Point 6 is waiting for the owner's screenshots.
8. Playback on the Samsung S24 is not verified: the cause of the report is not established (above). The start watchdog
   waits 8 s, which is a guess about what is too long; if a real video needs longer it will be tried with the software decoder.
9. The access effect in `Navigation.kt` is not covered by a test of its own.
