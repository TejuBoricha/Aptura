# UI/UX round, 2026-10-29: outcome record

The owner's list had six points. This file records points 1 to 5: what was found, the root cause, what changed, how it
was tested, what is not verified and what remains. Point 6 (feedback from the owner's screenshots) has **not started**;
the one defect a screenshot has already shown (the sidebar, below) is fixed and recorded here. Work is on PR #18.

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
display). **Not fixed:** a cold start where the pinned theme differs from the system theme can show one frame of the
system theme before settings load; fixing it needs a synchronous settings read at start-up.

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

## Verification

- Full unit suite: 776 tests, 0 failures, 104 skipped (the galleries, which run only when `APTURA_GALLERY_DIR` is set).
- Guide gallery renders reviewed at: phone, small phone (320 dp), 200% font, landscape, tablet, light and dark, and the
  replay with a library. The library layouts are rendered by `LibraryLayoutGalleryTest` at its listed sizes.
- `lintDebug`: 0 errors, 91 warnings, none in the guide files (a first run flagged two uses of `Configuration.screenHeightDp` there; they now use the window-size API). `assembleRelease` with `verifyReleaseApk`
  (which fails if the APK gains the INTERNET permission or a bundled software decoder): passes. Both ran online, because
  lint's instrumented-test model cannot resolve offline. CI runs them again on the PR.

## Known limitations and remaining issues

1. Not verified on a physical device or at 60/90/120 Hz (on hold at the owner's request).
2. Cold-start theme flash when the pinned theme differs from the system theme (above).
3. **At 360 dp wide the search field's hint truncates** ("Search vi…") because the refresh and settings buttons share the
   row. It is the real library header, seen when the guide's picture was first laid out at 360 dp. Not changed: a copy or
   layout decision for the owner (shorter hint, or move Refresh into the View sheet).
4. Folders with the same name in different places merge into one bucket (`MediaStore` groups by bucket name); older
   behaviour, not changed.
5. In landscape the guide is still one column that scrolls; a two-column layout (picture beside the text) would suit it
   better and was left out as decoration without a measured need.
6. The replay shows the person's own video titles and frames on the library page. It is on-device only and nothing is
   stored or sent, but a screenshot of the guide would show them.
7. Point 6 is waiting for the owner's screenshots.
