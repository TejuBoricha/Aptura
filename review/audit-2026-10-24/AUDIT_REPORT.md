# Aptura - Follow-up Audit and Remediation

**Snapshot:** `main` @ `90ce6b9` (2026-10-26)  
**Baseline:** `review/audit-2026-10-23/AUDIT_REPORT.md` @ `588cc3a`  
**Delta for this round:** 56 files, +5,430 / -799 across `14f26d1^..90ce6b9`  
**Quality gate after the final code change:** **523 tests / 82 suites, 0 failures, 0 skipped**; lint 0 errors; `assembleRelease` + `verifyReleaseApk` clean  
**Runtime evidence:** `review/audit-2026-10-24/evidence/` (8 curated captures + UI XML)

> This round was not a second hunt for the P0 playback defects fixed in the previous audit. It targeted the layer underneath those fixes: **features that were absent, controls that looked wired but were not, and UI that only worked at the reviewer's window size, font scale and theme.** The report records what was found, what changed, what is still open, why the earlier passes missed this class of defect, and what now prevents it from coming back.

---

## 0. Verdict

The previous audit made Aptura **correct**. This round makes it **complete enough to behave like a finished product** on tablets, in Light mode, at large text sizes, and when the user tries to manage files rather than only play them.

The most important outcome is not any single screen. It is that the app now has **one source of truth for each cross-surface decision**:

- one eight-speed list and one speed control shared by Settings and both player surfaces;
- one appearance model shared by the library, Settings, the guide and system bars, with the player deliberately outside it;
- one tablet index that owns collection navigation instead of duplicating it in chips;
- one file-action pipeline that copies first, deletes only after consent, and reports retained originals;
- one language-label mapper so raw container codes stop leaking into the UI.

No known P0 defect remains in the reviewed scope. The remaining risk is concentrated in **physical-device verification** and **edge cases whose failure mode is an unavailable URI or a long-running copy**, not in the main journeys.

| Dimension | After this round | One line |
|---|---|---|
| Feature completeness | **B+** | The visible features now have working actions behind them. |
| UX consistency | **B+** | Speed, theme, file actions and language labels no longer disagree across screens. |
| Responsive / adaptive | **A-** | The library is a real tablet index, not a stretched phone; large text is a first-class state. |
| Accessibility | **B** | Touch targets and slider semantics are tested; TalkBack on hardware is still unverified. |
| Data / file safety | **B+** | Copy-before-delete and honest batch results; SAF URI remapping remains open. |
| Test / QA | **B** | Strong Robolectric coverage and live checks; no release-variant instrumented E2E. |
| Evidence honesty | **B** | Library/Settings captures are clean; the player capture limitation is documented rather than hidden. |

---

## 1. Method and limits

### 1.1 What was reviewed

- Every commit in `14f26d1^..90ce6b9`, with the delivered behavior checked against the code, not the commit message.
- The library at phone width, tablet width (2560 x 1600, 240 dpi), Light and Dark, 100% and 200% font scale.
- Settings, the welcome guide, the player controls, the speed sheet, the subtitle sheet, the selection bar and the compact sidebar.
- Real MediaStore rename through Android's content resolver, then a query of `_display_name` and `_data` to prove the file on disk changed, not only the database row.
- The final gate output and the test-result XML, not a cached summary.

### 1.2 Runtime matrix

| State | How it was exercised |
|---|---|
| Tablet, expanded library | 2560 x 1600 @ 240 dpi; 248 dp collection index |
| Tablet, compact library | 88 dp rail; 68 x 48 dp expand chip; live expand/collapse |
| Large text | 200% font in Robolectric and on the emulator; rail falls back to icon-only |
| Light theme | Settings and library captured; player chrome checked separately |
| Dark theme | Library, Settings, player speed sheet |
| Selection | Long-press and explicit **Select**; batch bar; Back clears selection |
| Rename | Live MediaStore rename with extension preservation, then restore |
| Guide | First run, version bump path, page 5 footer, replay from Settings |

### 1.3 What this environment cannot prove

- Real-device codec behavior, HDR, thermal throttling, battery cost, Bluetooth A/V sync and TalkBack.
- PiP behavior on a device that does **not** support PiP; the emulator supports it, so the hidden-control state is covered by tests, not a screenshot.
- Release-variant instrumented E2E under AGP 9.0.1; release verification remains a manual smoke test.
- Large SAF copies at multi-gigabyte scale, removable SD-card removal during a transfer, and process death during a copy.
- A clean screenshot of the player's `SurfaceView` layer. The emulator tiles that layer under both SwiftShader and the host GPU; see 3.4.

---

## 2. Findings and fixes

Severity here is product severity, not the previous report's P0 scale: **P1** = a capability users reasonably expect is missing or unsafe; **P2** = a visible, repeatable UX defect; **P3** = polish or consistency.

### F-01 - The app had no appearance system; Light mode did not exist [P1]

**Found.** The library was dark-only. There was no way to follow the system theme, pin Light or Dark, or choose an accent. Adding Light naively would also have washed out the player and its menus, which must stay cinematic.

**Why it mattered.** A media player that ignores the system theme feels foreign on a phone set to Light, and the player is exactly where a bright theme would be most damaging.

**Fix (`0e932d2`).**
- `ThemeMode` = `SYSTEM` (default) / `LIGHT` / `DARK`; `AccentColor` = Cyan, Blue, Violet, Amber, Rose.
- Each accent has separate dark and light primary/secondary values; a cyan that glows on near-black is not readable on white.
- `ApturaPalette` is one complete token set. `ApturaColors` is state-backed, so existing call sites subscribe to a palette swap without signature churn.
- `ApturaTheme` applies the palette in `remember(palette)` to avoid a one-frame flash on cold start or the player transition.
- `MainNavigation` forces `ThemeMode.DARK` while the player route is active; the player root uses literal `Color.Black` so the letterbox is black on the first frame.
- System status/navigation bar icon appearance follows the effective palette, not the system theme.
- Settings copy is explicit: **"System follows your device. Playback and its menus always stay dark."**

**Proof.** `ApturaPalettesTest` (5), `ApturaThemeTest` (3), `SettingsAppearanceTest` (3), plus `ApturaSettingsRepositoryTest` (5) and `StartDestinationGateTest` (6). Evidence 03 is Light Settings; evidence 08 is the final Light library; evidence 06 is the dark player speed sheet.

---

### F-02 - The welcome guide only appeared on first install, never after an update [P2]

**Found.** `onboardingCompleted` was a one-way boolean. Existing users never saw the guide again after a release that added features, and there was no record of which guide version they had seen.

**Why it mattered.** The guide is the only explanation of gestures, file actions and the offline promise. A user who updates from an older build never gets the new pages.

**Fix (`0e932d2`).**
- `lastSeenGuideVersion` stores the `versionCode` whose guide was completed.
- The guide shows when `!onboardingCompleted` **or** the stored version differs from the installed version.
- A stored `0` is treated as a pre-versioning install and is backfilled silently, so the first launch after the update does not replay the guide for every existing user.
- The version is written on permission grant, guide finish/skip, and reaching the last page. The guide remains replayable from Settings.

**Proof.** `StartDestinationGateTest`: first launch, completed launch, update replay, same-version suppression, legacy backfill. Evidence 07 shows page 5 with the fixed footer.

---

### F-03 - The tablet library was a stretched phone, and the collapsed rail had a detached chevron [P1]

**Found.** The wide layout did not have a collection index with library totals, folder navigation and maintenance actions. At large text, folder names were squeezed or truncated. The first collapsed-sidebar design put the aperture mark and the expand chevron on separate lines, which is the exact defect the user reported with a zoomed crop: the chevron looked detached from the brand mark.

**Why it mattered.** On a tablet, the sidebar is the primary navigation. If it cannot collapse, it steals width from the grid forever; if it collapses badly, it looks like a rendering bug.

**Fix (`5e47b29`, `90ce6b9`, plus polish in `90c0b61`).**
- **248 dp** expanded collection index at **>= 700 dp** when `fontScale < 1.5`: library pulse (films, runtime, footprint), All videos, folders with counts, Add folder, Open a file, Rescan, Settings, and the offline promise.
- **88 dp** compact rail at **600-699 dp**, or at any width when `fontScale >= 1.5`. Labels disappear above 150% type; icon content descriptions carry the names for TalkBack.
- Animated expand/collapse (`AnimatedContent`, 220 ms, size + fade + slide); the choice is remembered for the session.
- The compact header is now **one 68 x 48 dp chip** that combines the aperture mark and the chevron. The chip is the expand control; there is no second floating arrow.
- All controls retain 48 dp targets; the rail respects top/bottom window insets.

**Proof.** `LibrarySidebarTest` (2) checks the wide index, the collapse/expand round trip, the exact chip bounds (68 x 48 dp), and the 200% font fallback. Live `uiautomator` bounds for **Expand sidebar** were `[15,78][117,150]` = 102 x 72 px at 1.5x density = **68 x 48 dp**. Evidence 01 is the collapsed rail; evidence 02 is the 248 dp index.

---

### F-04 - The compact Settings bar faded to transparent inside its own height [P2]

**Found.** The bar used a vertical gradient that reached `Color.Transparent` at its bottom. When Settings scrolled, rows showed through the back button and title. The bar also did not cover the full 56 dp control row.

**Why it mattered.** It reads as a rendering glitch on the most trust-sensitive screen, and it makes the back affordance harder to see.

**Fix (`ffa5525`).**
- `SettingsTopBarBackground` is a `SolidColor(ApturaColors.SurfaceBase)` with alpha 1.
- `SETTINGS_TOP_BAR_HEIGHT_DP = 56`; the bar is exactly the control row plus the status-bar inset, with a hairline at the lower edge.
- The bar has a test tag and a regression test that scrolls the list, then checks the bar is opaque and reaches below the back button.

**Proof.** `SettingsTopBarTest` (2). Evidence 03 shows the Light Settings header.

---

### F-05 - The guide's fixed footer covered the last rows of long pages [P2]

**Found.** The pager filled the screen and the footer was drawn over it. A 280 dp bottom scrim washed out content, and the page's 224 dp bottom padding did not reliably let the last row scroll clear. On page 2, "More places" stayed half-faded; on page 4, the bottom of the tool grid did too.

**Why it mattered.** The guide's last bullet on a page is often the one that explains the feature the page exists for. A half-faded row looks broken and hides information.

**Fix (`ffa5525`, `90ce6b9`).**
- The page area is now a `weight(1f)` box above a footer that sits outside the pager and owns the navigation-bar inset.
- The bottom scrim is **20 dp** (`GUIDE_BOTTOM_FADE_HEIGHT`), not 280 dp; content bottom padding is 36 dp, so the last row can scroll completely clear.
- The pager uses a stable page key so page state does not collide during the transition.

**Proof.** `OnboardingScreenUiTest` (10), including `theContent_canScrollClearOfTheBottomFade`. Evidence 07 shows page 5 with **Start watching** and **Back** fully visible.

---

### F-06 - Settings and the player disagreed about speed, and the player used a two-row grid [P2]

**Found.** Settings exposed five speeds; the player exposed eight in a two-row grid of cards. `0.5x` and `1.75x` could be used for one video but could never be set as the default. The grid consumed two rows and made a single number feel like a spreadsheet.

**Why it mattered.** The same setting had two different vocabularies and two different ranges. That is a trust break even when both controls "work".

**Fix (`cec17b1`).**
- `PlaybackSpeedOptions.all` is the single eight-speed list.
- `SpeedRail` is one control, not a grid: a live value bubble rides the finger, the ticks rise from slower to faster, and the ends name the range. The needle springs to the nearest detent on release.
- The whole rail is one accessible slider (`progressBarRangeInfo`, `setProgress`, `stateDescription`), so TalkBack can change it with the standard action.
- The bubble grows with the font instead of clipping at 200% type; haptics fire when crossing a detent.
- Settings and both player surfaces (the speed chip sheet and the advanced sheet) use the same list and the same rail.

**Proof.** `PlaybackSpeedOptionsTest` (2), `SpeedRailMathTest` (5), `SettingsSpeedOptionsTest` (6), `PlayerSpeedRailTest` (2), `SettingsChipContrastTest` (4). The tests cover every speed, half speed, tapping the far right for 2.0x, an unknown stored speed snapping to the nearest value, and the rail remaining reachable after scrolling.

---

### F-07 - PiP was offered on devices that cannot use it [P2]

**Found.** The inline player bar and the overflow menu always showed **Picture in Picture**, even when the system feature was absent. Tapping it failed silently or did nothing.

**Why it mattered.** A dead control is worse than no control, especially in the player where every tap is deliberate.

**Fix (`cec17b1`).**
- `pipSupported` is passed into `PlayerScreen`.
- The inline control and the overflow item are both omitted when the feature is absent; every other action stays in place.
- The caller derives the value from `PackageManager.hasSystemFeature(FEATURE_PICTURE_IN_PICTURE)`.

**Proof.** `PlayerPipAvailabilityTest` (1) on a phone without PiP; `PlayerWideTopBarTest` (2) on an 800 dp window confirms the remaining actions stay inline with full touch targets.

---

### F-08 - There was no way to reset playback defaults [P2]

**Found.** Defaults were scattered across speed, aspect ratio, seek step, background audio, auto-advance countdown and decoder strategy. A user who experimented had to find and undo each one by hand.

**Why it mattered.** "Reset" is a standard recovery path. Without it, a bad combination of defaults is effectively permanent unless the user remembers every screen.

**Fix (`90c0b61`).**
- `ApturaSettingsRepository.resetPlaybackDefaults()` restores speed 1.0x, aspect mode 0, seek 10 s, background audio on, auto-advance 10 s and decoder Auto.
- Settings exposes **Reset Playback Defaults** in the Playback section, with a confirmation dialog that names what will change and a toast on completion.
- The action resets **defaults**, not per-video overrides; the dialog copy says so.

**Proof.** `ApturaSettingsRepositoryTest` (5); the dialog and row are in the Settings screen captured in evidence 03.

---

### F-09 - Library file actions were mostly index operations; selection, rename, move and paste were incomplete [P1]

**Found.** The library could long-press into a selection, but there was no explicit **Select** entry, no batch action bar that stayed centered on a tablet, no real multi-item move, and no clipboard workflow. Rename updated the library title in at least one path without guaranteeing the file name on disk. Move was not a real SAF copy-then-delete pipeline.

**Why it mattered.** A library that can play files but cannot safely rename, move or delete them is a viewer, not a library. The failure mode is data loss: a title-only rename or a delete-without-copy looks successful while the bytes are unchanged or gone.

**Fix (`90ce6b9`).**
- **Explicit selection:** every card overflow has **Select**. Long-press still works. The action bar shows the count, **Select all / Clear**, Move, Share, and a per-selection overflow (Rename for one item, Copy, Hide, Delete). Back clears selection before leaving the library.
- **Centered reading width:** the action bar sits in a `widthIn(max = 680.dp)` container, centered under the search row. A UI test pins the 680 dp cap and center at 1200 dp.
- **Rename:** `ApturaMediaFileManager.renameMediaFile` updates MediaStore `DISPLAY_NAME` and `TITLE`, or calls `DocumentsContract.renameDocument` for SAF, or renames the file directly. Callers pass only the visible title; the manager preserves the real extension from the current file name or MIME type. `"Dr. Strangelove"` is not mistaken for an extension; a trailing dot does not create `..mp4`.
- **Move:** `MediaFileTransfer.copyIntoTree` copies bytes into the chosen SAF tree and keeps the source. Only after the copy succeeds does the app ask Android to delete the originals. If the copy fails, originals remain and the result dialog names the retained files. If deletion is partial, the dialog reports both counts.
- **Clipboard:** Copy/Cut stores URIs; Paste copies or moves into the chosen folder. Successfully transferred entries leave the clipboard; failed entries stay available for retry.
- **Failure state:** failed deletes stay selected so the user can retry; successful deletes clear the selection.

**Proof.** `MediaLibraryScreenUiTest` (16) includes the explicit Select entry, the 680 dp centered bar, single and bulk delete copy, bulk move copy, and clipboard controls. `MediaFileTransferTest` (1) proves bytes are copied and the source is retained. `ApturaMediaFileManagerTest` (4) covers extension preservation, an explicit extension, a year in the title, and a trailing dot. Live rename proof: `AuditClip` -> `AuditClipRenamed`, with MediaStore `_display_name=AuditClipRenamed.mp4` and `_data=.../AuditClipRenamed.mp4`; the fixture was restored to `AuditClip.mp4` afterwards.

---

### F-10 - Delete and trash choices were unclear and batch results could hide retained files [P1]

**Found.** The destructive dialog did not distinguish Android's restorable trash from permanent deletion. It did not scale to a batch, and it could not tell the user which files were retained when only part of a batch succeeded.

**Why it mattered.** "Delete" is the highest-risk action in the app. Ambiguous copy plus a partial failure is how users lose files they believed were copied.

**Fix (`90ce6b9`, building on the previous audit's delete work).**
- The dialog now has a batch title (`Delete N videos from device?`), names a single video, and offers **Move to trash** only when every URI is a MediaStore URI on API 30+.
- Non-MediaStore items and API 29 get permanent deletion only, with explicit "cannot be undone" copy.
- `MediaDeleteFlow` accounts for every URI in a batch and returns `DeleteBatchResult(deletedUris, failedUris)`.
- A move result distinguishes complete, partial and not completed; it names the retained source files rather than claiming success.
- `MediaStorageActionsTest` (2) pins the trash eligibility rule: MediaStore + API 30+ yes; SAF document no; API 29 no.

**Proof.** `MediaLibraryScreenUiTest` single and bulk delete dialogs, `MediaStorageActionsTest`, and the live final library state (no selection, two fixtures).

---

### F-11 - Folder browsing had no swipe and could fight selection mode [P3]

**Found.** Moving between folders required tapping chips. A horizontal swipe in the grid did nothing.

**Why it mattered.** On a tablet, swiping the grid is the natural way to move between collections; it also makes the library feel like a media browser rather than a form.

**Fix (`90ce6b9`).**
- A horizontal drag of at least **64 dp** advances to the next or previous folder with an animated transition.
- The gesture is disabled while a selection is active, so a swipe cannot silently change the folder under the selection bar.
- Tests cover left, both directions, and the selection-mode no-op.

**Proof.** `MediaLibraryScreenUiTest` swipe tests (3).

---

### F-12 - The subtitle `EXTERNAL` badge rendered one letter per line [P2]

**Found.** The badge sat in a title row after an unbounded file name with no remaining width. It wrapped to `E X T E R N A L` on separate lines, which looked like a broken layout and made the track row unreadable.

**Why it mattered.** External subtitles are a core feature for this audience. The badge is the only signal that the track is a sidecar file rather than an embedded track.

**Fix (`14f26d1`).**
- Each track is one card: language/role as the title, file name on the metadata line, badge on its own constrained line with `maxLines = 1` and `softWrap = false`.
- The selected card uses an accent border, tint and check mark; the header's active label ellipsizes instead of pushing the row apart.

**Proof.** Commit `14f26d1`; manually rechecked on the signed release build. There is no dedicated Compose test for this sheet yet; it remains a test gap, not a verification claim.

---

### F-13 - Raw language codes (`und`, `eng`, `ja`) leaked into the UI [P3]

**Found.** Video Details rendered strings such as `Audio Stream (und)`. Container metadata mixes ISO 639-1, ISO 639-2 and BCP-47, and uses `und` for unknown. The UI treated them as display text.

**Why it mattered.** `und` is worse than saying nothing; `eng` is noise. It makes a polished player look like a debug build.

**Fix (`ffa5525`).**
- `MediaLanguageLabels.displayName` returns a readable name for `en`, `eng`, `pt-BR`, `EN`, ` eng `.
- `und`, blank and whitespace return `null`, so the caller omits the language instead of printing it.
- A well-formed but unknown code is shown as an uppercase code rather than dropped.
- Video Details and the subtitle sheet use the mapper.

**Proof.** `MediaLanguageLabelsTest` (6).

---

### F-14 - "Software" decoder copy implied FFmpeg video decoding; there was no device capability report [P2]

**Found.** The decoder picker said "Software" without explaining that it means Android's platform software decoder, not the bundled FFmpeg extension. Users could reasonably conclude the app bundles a software video decoder, which it does not. There was also no way to see which codecs the device actually advertises.

**Why it mattered.** This is a truthfulness issue in a privacy-first app, and it is a support issue: a failed video looks like an app bug when the device simply lacks a decoder.

**Fix (`c0b250b`).**
- `DeviceCodecSupport` enumerates `MediaCodecList.ALL_CODECS` (decoders only, aliases skipped), prefers the hardware decoder when both exist, and maps MIME types to human labels.
- Settings -> Diagnostics -> **Device Codec Support** opens a copyable report with video/audio entries, HW/SW badges, decoder names, and the explicit disclosure that the FFmpeg extension is audio-only and bundles no video decoder.
- Copy everywhere now says "Software" = **Android platform software**, not FFmpeg; "Hardware only" says it fails rather than falling back.
- This preserves ADR-010 / DEC-009: no bundled software video decoder, no GPL dependency.

**Proof.** `DeviceCodecSupportTest` (3) pins labels, report safety and the policy note. Commit `c0b250b`.

---

### F-15 - The double-tap seek HUD covered a large part of the picture [P3]

**Found.** The overlay was a 136 x 210 dp curved edge scrim with a 38 dp icon, an accumulated time and a two-word label. It was visually heavy for a gesture that is supposed to be a glance.

**Why it mattered.** Double-tap seeking is frequent; a large overlay blocks the frame the user is trying to inspect.

**Fix (`90ce6b9`).**
- The HUD is now **132 x 68 dp**: a 42 dp accent disc with a direction arrow, the accumulated delta in tabular figures, and a single "Ahead"/"Back" label.
- The surface uses an 18 dp corner radius, an 8 dp shadow, and a 0.88-alpha scrim; the arrow direction is forced LTR so a mirrored layout cannot reverse "forward".
- A `double_tap_seek_hud` test tag and semantics descriptions make the state testable.

**Proof.** `GestureHudUiTest` (6); the compact geometry is in `GestureHudOverlays.kt`.

---

### F-16 - Player sheet content could sit under the navigation bar [P2]

**Found.** The advanced sheet applied `verticalScroll` before `windowInsetsPadding`. The scrollable content therefore measured against the full height and the last row could sit behind the navigation bar.

**Why it mattered.** The bottom rows are exactly where the speed rail and advanced controls live; an inset bug makes them hard to reach.

**Fix (`90c0b61`).**
- `windowInsetsPadding(WindowInsets.navigationBars)` and `imePadding()` are applied **before** `verticalScroll`, so the scroll viewport itself is inset.
- The same ordering is used for the sheet's content column; bottom padding remains 24 dp.

**Proof.** Code ordering in `PlayerScreen.kt`; no dedicated inset test. This should be covered by the future sheet golden test in section 7.1.

---

### F-17 - Continue Watching looked like a secondary afterthought [P3]

**Found.** Cards were 200 dp wide, used a 12 dp corner radius and a flat 3 dp progress line. There was no explicit "Resume" affordance; the play disc was the only cue.

**Why it mattered.** Continue Watching is the first row on the library and the shortest path back into a video. It should look like a primary action, not a thumbnail strip.

**Fix (`90c0b61`).**
- Cards are 220 dp, 16 dp radius, with a hairline border.
- A **Resume** pill sits top-right; the play disc is 42 dp; the progress indicator is inset 8 dp so it reads as part of the card rather than a browser loading bar.
- The row has 2 dp horizontal content padding so the first card's border is not clipped.
- The TalkBack label remains one stop per card: "Resume Title, N min left".

**Proof.** Evidence 02 shows the row; `MediaLibraryScreenUiTest` renders the library at 1200 dp with the same card model.

---

## 3. Verification

### 3.1 Gate

After the final code change (`90ce6b9`):

```
523 tests / 82 suites / 0 failures / 0 skipped
lintDebug: 0 errors
assembleRelease + verifyReleaseApk: clean
```

The test counts were read from `app/build/test-results/testDebugUnitTest/*.xml`, not from a cached console line. The gate was run after the final code change; only documentation and curated evidence changed afterwards.

### 3.2 Live checks

| Check | Result |
|---|---|
| Rename preserves the extension | `AuditClip` -> `AuditClipRenamed`; MediaStore `_display_name=AuditClipRenamed.mp4`; `_data=.../AuditClipRenamed.mp4`; fixture restored |
| Compact rail chip | **Expand sidebar** bounds `[15,78][117,150]` = 68 x 48 dp; expand/collapse round trip works |
| Selection bar | 680 dp cap; centered under search; explicit **Select**; Back clears; failed items remain selected |
| Light theme | Settings and library captured in Light; font scale 1.0; two fixture videos |
| Guide footer | Page 5 shows **Start watching** and **Back** without overlap |
| Final device state | Light theme, font scale 1.0, expanded sidebar, no selection, two fixture videos |

### 3.3 Evidence index

All paths are relative to this folder.

| File | What it shows |
|---|---|
| `evidence/01_collapsed_sidebar_light.png` + `.xml` | 88 dp rail, 68 x 48 dp chip, folder chips returned to the top edge |
| `evidence/02_expanded_sidebar_light.png` + `.xml` | 248 dp collection index: pulse, folders with counts, tools, offline promise |
| `evidence/03_settings_light.png` + `.xml` | Light Settings: theme, accent, speed rail, reset row, codec section |
| `evidence/04_selection_bar_light.png` + `.xml` | Centered selection bar; `04_selection_bar_crop.png` is the close-up |
| `evidence/05_player_dark_under_light_theme.png` | Player chrome while the app is Light; see 3.4 for the `SurfaceView` limitation |
| `evidence/06_player_speed_sheet_dark.png` + `.xml` | Dark speed sheet with all eight speeds |
| `evidence/07_guide_footer_light.png` + `.xml` | Guide page 5; fixed footer; last rows clear of the fade |
| `evidence/08_library_final_light.png` | Final Light library, expanded sidebar, two fixtures, no selection |

### 3.4 Evidence limitations

- **Player `SurfaceView` tiling.** The emulator composites the video layer as repeated tiles under SwiftShader and under the host GPU. The tiling is in the capture, not in the decoded file: pixel checks of flat regions are single-color, and the same fixture decodes cleanly outside the player. Evidence 05 is therefore used only for the dark player chrome and the dark letterbox, never to judge video output. The dark-menu claim is additionally supported by evidence 06 and by the forced-dark route code.
- **Emulator render ghosting.** One capture pass showed faint retained frames after repeated screencaps. A reboot cleared it; the curated evidence was recaptured. Flat regions in the final PNGs were checked numerically (one unique color) before use.
- **Robolectric vs. hardware.** The responsive tests run at fixed qualifiers (for example `w900dp-h1100dp-xhdpi` and `w1200dp-h900dp-xhdpi`). They prove layout decisions, not real GPU performance.
- **No physical device.** Codec/HDR/thermal/TalkBack conclusions are limited to what the emulator and the code can prove.

---

## 4. What remains pending

### 4.1 Physical-device verification [highest residual risk]

**Why it is open.** The emulator cannot prove hardware codec behavior, HDR tone mapping, thermal throttling, battery cost, Bluetooth A/V sync or TalkBack navigation.

**Next step.** Run the same evidence protocol on at least one mid-range and one flagship phone: H.264/HEVC/AV1/VP9, HDR10, 4K/60, 10-bit, AC3/EAC3/DTS/TrueHD, Bluetooth earbuds with a lip-sync clip, TalkBack through library -> player -> sheets, and a 30-minute thermal soak.

### 4.2 SAF rename can orphan the database URI

**Why it is open.** `DocumentsContract.renameDocument` returns a **new** URI, but `ApturaMediaFileManager.renameMediaFile` returns only `Boolean`; the caller updates the library title but keeps the old URI. The next scan can see the old URI as missing and the renamed document as a new item. The live MediaStore path is unaffected because the URI stays stable.

**Next step.** Return the renamed URI (or a small result type), update the repository row and saved progress in one transaction, and add a test with a real `DocumentsProvider` that changes the document ID. Until then, document-provider renames should be treated as "re-scan required" in the copy or an explicit caveat.

### 4.3 Large copy/move has no progress or cancellation

**Why it is open.** `MediaFileTransfer` copies in a loop with no progress callback and no cooperative cancellation. A multi-gigabyte move can look frozen; the UI only sees the final result.

**Next step.** Move the transfer into a foreground/WorkManager job with progress, a cancel action, free-space preflight, and a cleanup policy for partial destinations. Keep copy-before-delete.

### 4.4 Missing items are badged but not removable per item

**Why it is open.** A file deleted outside Aptura can remain as a **Missing** card when it carries saved progress. Settings has a bulk **Remove all** action, and the card is badged, but there is no per-card "Remove from library" action, and a scan does not auto-purge progress-bearing rows. This was observed during evidence cleanup with a screen recording deleted outside the app (`ev2d`); it was hidden for the final capture.

**Next step.** Add a per-card remove action and a "keep progress if the file returns" explanation. Do not silently delete progress.

### 4.5 Release-variant instrumented E2E is still shelved

**Why it is open.** AGP 9.0.1 does not create `connectedReleaseAndroidTest` from `testBuildType` alone; the opt-in `androidComponents.beforeVariants` work is documented but not enabled. Release verification is currently a manual smoke test.

**Next step.** Enable the device-test variant in a dedicated CI job, or accept debug-only instrumentation and keep the manual release smoke checklist.

### 4.6 PiP-unsupported UI is test-only

**Why it is open.** The emulator supports PiP, so the hidden-control state cannot be photographed. `PlayerPipAvailabilityTest` and `PlayerWideTopBarTest` cover it.

**Next step.** Verify on a device or emulator image without `FEATURE_PICTURE_IN_PICTURE`; capture the top bar and overflow.

### 4.7 Player capture remains unreliable on this emulator

**Why it is open.** The `SurfaceView` tiling affects both renderers. This is a capture limitation, not an app defect, but it means the player's video output cannot be used as visual evidence here.

**Next step.** Capture on hardware, or use a `TextureView`-based debug build for screenshots if pixel evidence of the video layer is required. Do not change the production surface path solely for screenshots.

---

## 5. Why the earlier reviews missed this

The previous audit was strong on correctness and weak on **completeness under non-default conditions**. The misses were not random; they came from five repeatable habits.

### 5.1 Correctness bias

The earlier report asked, "Does this code do what it claims?" It did not ask, "Is the capability the user expects actually present?" A missing theme system, a missing explicit Select action or a missing reset row cannot fail a test because there is no test to fail. The review treated the absence of code as out of scope.

### 5.2 Default-device bias

Most inspection happened on a phone at default font scale, in Dark mode. The tablet sidebar, the 200% font fallback, the Light palette and the 680 dp selection bar were not part of the reviewer's visual field. The sidebar defect the user reported was visible only when the compact header was zoomed: at full-frame scale, a 34 dp aperture mark and a 48 dp chevron looked like two intentional elements.

### 5.3 Green tests encoded assumptions

The tests were implementation-shaped:

- the speed test checked the five values the Settings picker happened to expose, not whether the player and Settings agreed;
- the guide test checked first-run Skip, not the update path;
- the library test long-pressed into selection, so the absence of an explicit **Select** entry was invisible;
- the rename test checked a database title, not `_display_name` and `_data`;
- the subtitle test used a short, well-formed language code, not `und` or a long external file name;
- the PiP test assumed the feature existed because the emulator had it.

A green suite proved the author's assumptions, not the product's state space.

### 5.4 File actions were reviewed as UI, not as byte movement

The old flow could update an index row and look successful. Nothing asserted that a SAF copy wrote bytes, that the source survived a failed copy, that a rename changed the file on disk, or that a batch reported retained files. The risk was invisible because the visible layer was complete.

### 5.5 Visual evidence was trusted at full-frame scale

Screenshots were inspected for overall composition, not for 1-2 px alignment, 200% text overflow, a badge wrapping one letter per line, or a bar's alpha at its own bottom edge. The emulator's render ghosting made some captures unreliable, which encouraged "it looks fine" judgments instead of pixel checks. The user's cropped image was the forcing function for the sidebar chip: zooming in made a defect obvious that the full frame hid.

### 5.6 The "feature exists" narrative

The project's own notes counted parts and screens. A screen with a control was treated as a shipped feature. This round's standard is stricter: **a control is shipped only when its action is wired, its failure mode is honest, and its state is tested at the sizes and themes where it will be used.**

---

## 6. Prevention now in place

### 6.1 One source of truth per cross-surface decision

- `PlaybackSpeedOptions.all` feeds Settings and both player surfaces.
- `MediaLanguageLabels` is the only language-code mapper.
- `ApturaPalette` is the only token set; the player route is the only forced-dark override.
- `MediaFileTransfer` is the only SAF copy path; `MediaDeleteFlow` is the only destructive path.
- `ApturaMediaFileManager` is the only rename path.

When two screens need the same list, they import it; they do not retype it.

### 6.2 Regression tests for the exact defects

The gate now includes, among others:

| Suite | Tests | Pins |
|---|---:|---|
| `MediaLibraryScreenUiTest` | 16 | Select entry, selection/Back, batch delete/move copy, 680 dp centered bar, swipe and selection no-op |
| `OnboardingScreenUiTest` | 10 | Skip, paging, content, footer clearance, last-page behavior, guide-seen once |
| `SettingsSpeedOptionsTest` | 6 | Every speed, half speed, far-right 2.0x, stored default, unknown snap, scroll reachability |
| `MediaLanguageLabelsTest` | 6 | ISO/BCP-47 names, `und` suppression, unknown code, casing |
| `GestureHudUiTest` | 6 | Compact double-tap HUD, both directions, volume/brightness, lock hint |
| `ApturaPalettesTest` | 5 | Dark tokens unchanged, light contrast, five accents, status colors |
| `SpeedRailMathTest` | 5 | Even fractions, index round trip, snapping, single speed, stored detent |
| `StartDestinationGateTest` | 6 | First run, update replay, same version, legacy 0 |
| `ApturaSettingsRepositoryTest` | 5 | Appearance/guide defaults and persistence |
| `ApturaMediaFileManagerTest` | 4 | Extension preservation and dot edge cases |
| `SettingsChipContrastTest` | 4 | Selected/unselected contrast, radio semantics, no baked-in colors |
| `SettingsAppearanceTest` | 3 | Theme choice, accent choice, selected accent announcement |
| `ApturaThemeTest` | 3 | Palette application, accent swap, reset |
| `DeviceCodecSupportTest` | 3 | Labels, safe report, audio-only policy |
| `SettingsTopBarTest` | 2 | Opaque bar, 56 dp coverage |
| `LibrarySidebarTest` | 2 | Wide index, 68 x 48 chip, 200% fallback |
| `MediaStorageActionsTest` | 2 | Trash only for MediaStore + API 30+ |
| `PlayerSpeedRailTest` | 2 | Same ramp in the advanced sheet; chosen speed reaches the player |
| `PlayerWideTopBarTest` | 2 | Wide bar actions stay inline; PiP omitted when absent |
| `PlaybackSpeedOptionsTest` | 2 | Shared list and labels |
| `MediaFileTransferTest` | 1 | Bytes copied, source retained |
| `PlayerPipAvailabilityTest` | 1 | No PiP control without the feature |
| `LibraryTouchTargetsTest` | 1 | 48 dp targets |

That is **97 tests across the 23 suites most relevant to this round**; the full gate is **523 / 82**.

### 6.3 Layout tests at the sizes that break layouts

- `w900dp-h1100dp-xhdpi` for the expanded sidebar and 200% font fallback.
- `w1200dp-h900dp-xhdpi` for the 680 dp centered selection bar.
- `w800dp-h600dp-xhdpi` for the wide player top bar.
- Phone qualifiers for the PiP-absent overflow.
- 200% font in both Robolectric and the emulator.

A layout that only works at the reviewer's window is no longer accepted.

### 6.4 Destructive-action contract

1. Copy first, verify the copy result, then request deletion.
2. If copy fails, the source remains and the dialog names it.
3. If deletion is partial, report both counts; never claim a clean move.
4. Offer system trash only where it can work (MediaStore, API 30+).
5. Keep failed items selected for retry.
6. Preserve the file extension on rename; do not trust the visible title.

### 6.5 Evidence protocol

- Capture with `screenrecord`, pull the first frame, and keep the `uiautomator` XML beside it.
- Verify flat regions numerically before using a screenshot.
- Never use a `SurfaceView` capture to judge video output; state the limitation in the report.
- Record the build, window size, density, theme and font scale with the evidence.
- Do not call an image "clean" because the preview looks clean; check the file.

### 6.6 Documentation

- `DESIGN.md` now records the 248/88 dp sidebar, the 600/700 dp breakpoints, the 150% font fallback, the 68 x 48 chip, the 680 dp action bar, the shared speed rail, the player-dark rule, and the file-action contract.
- `.agents/MEMORY.md` Part 74 records the commits, decisions, tests, evidence and remaining work.

---

## 7. Future prevention steps

These are not one-time fixes; they are the next layer of process.

1. **Golden/screenshot tests** for the library and Settings at phone/tablet, Light/Dark, 100/200% font, including the scrolled top bar, the selection bar and the guide footer. This is the only reliable way to catch "transparent bar" and "badge one letter per line" without a human zooming in.
2. **A real SAF provider test** that renames a document, changes its document ID, and proves the library row and saved progress follow the new URI. This closes 4.2.
3. **A file-transfer job with progress and cancellation**, plus a free-space preflight and partial-destination cleanup. This closes 4.3.
4. **A capability matrix in CI**: no PiP, no camera, no telephony, TV/Auto, API 29/30/35. `hasSystemFeature` branches need a test for the absent case, not only the happy path.
5. **Release-variant instrumented E2E** on a device farm, or an explicit signed release smoke checklist in CI. The current manual release smoke is honest but not scalable.
6. **A physical-device lab pass** for codecs, HDR, thermal, Bluetooth and TalkBack. The emulator is a layout and logic tool, not a media-certification tool.
7. **A design-review checklist** applied to every new screen: empty, loading, error, disabled, 200% font, narrow width, wide width, Light, Dark, scrolled, keyboard open, navigation bar inset, and a destructive action with a partial failure.
8. **An audit cadence** that samples the product at the edges, not only the center: the user's cropped sidebar image found in one minute what a full-frame review missed for hours.

---

## 8. What is genuinely good and should be kept

- **The privacy posture is real.** No INTERNET permission, no telemetry, no accounts, no `MANAGE_EXTERNAL_STORAGE`; the settings screen proves it in plain language.
- **The player stays dark.** The route-level override and the literal black root are the right architecture; the menus inherit the dark palette.
- **The single Media3 engine with platform decoders and an audio-only FFmpeg extension** is the correct store-build trade-off. The rejection of NextLib, libVLC and a custom video FFmpeg build is documented and upheld.
- **The copy-before-delete move flow** is the right data-safety default, and the batch result accounting is honest.
- **The test suite is now behavioral in the places that matter**: selection/Back, 680 dp centering, chip bounds, extension preservation, trash eligibility, speed reachability, guide versioning.
- **The design language remains coherent.** The new tablet index, compact rail, speed ramp and selection bar use the same tokens and typography rather than introducing a second visual system.

---

## Appendix A - Commit map

| Commit | Subject | What it delivered |
|---|---|---|
| `14f26d1` | Redesign subtitle track list as readable cards | Card layout, constrained `EXTERNAL` badge, selected state |
| `c0b250b` | Add device codec report and honest decoder labels | Device capability report, platform-software wording, audio-only FFmpeg disclosure |
| `0e932d2` | Add theme modes and guide versioning | System/Light/Dark, five accents, player-dark override, guide version gate |
| `ffa5525` | Fix settings bar coverage, track language labels, and guide footer | Opaque 56 dp bar, readable language names, fixed guide footer and fade |
| `cec17b1` | Add a shared speed ramp and hide PiP where unsupported | `PlaybackSpeedOptions.all`, `SpeedRail`, PiP capability gating |
| `5e47b29` | Add a collapsible tablet library index | 248/88 dp sidebar, 600/700 dp breakpoints, collection index |
| `90c0b61` | Polish library and playback settings | Continue Watching, reset defaults, sheet insets, sidebar controls |
| `90ce6b9` | Complete library file actions and compact sidebar polish | Explicit Select, batch bar, rename, SAF move, trash, clipboard, swipe, compact HUD, 68 x 48 chip |

**Diff:** 56 files, +5,430 / -799 across `14f26d1^..90ce6b9`.

---

## Appendix B - Evidence capture notes

- Device: `Aptura_Tablet_API_35`, 2560 x 1600, density 240, font scale 1.0, Light theme for captures 01-04 and 07-08; player chrome in 05-06.
- Final state: Light theme, expanded sidebar, no selection, two fixture videos (`AuditClip.mp4`, `Episode01.mkv`).
- Capture method: `adb shell screenrecord --time-limit 2`, pull, `ffmpeg -frames:v 1`, then `uiautomator dump` for the XML companion.
- Pixel checks: flat light regions resolve to a single RGB value (`242,244,244` in the Settings header sample); player letterbox/control samples are dark (`10,11,14` and `15,12,38`).
- The screen-recording files were removed from the device after extraction so they would not be indexed as library videos. A stale `ev2d` row from an earlier recording was hidden for the final capture; see 4.4.

---

## Appendix C - Reproduction recipes for the main fixes

### C.1 Theme and player-dark

1. Settings -> App Theme -> Light.
2. Confirm the library and Settings are light.
3. Open a video; confirm the player chrome remains dark.
4. Open the speed sheet; confirm it remains dark.
5. Return to Settings -> App Theme -> System; confirm the app follows the device.

### C.2 Sidebar chip and breakpoints

1. Open the library at >= 700 dp with font scale 1.0.
2. Confirm the 248 dp index with `COLLECTIONS`, counts, tools and the offline promise.
3. Tap **Collapse sidebar**; confirm the 88 dp rail and the single 68 x 48 dp chip.
4. Measure **Expand sidebar** bounds; expect 68 x 48 dp.
5. Tap the chip; confirm the index returns.
6. Set font scale to 2.0; confirm the rail is icon-only at any width and the content descriptions remain.

### C.3 Selection and file actions

1. Open a card's overflow -> **Select**.
2. Confirm the centered action bar with count, Select all/Clear, Move, Share and the overflow.
3. Press Back; confirm selection clears before navigation.
4. Select two items -> Move -> choose a SAF folder; confirm the copy dialog's copy-then-delete explanation.
5. Force a copy failure (read-only destination); confirm the originals remain and the result names them.
6. Rename a video without typing an extension; confirm the file on disk keeps `.mp4`.

### C.4 Guide after an update

1. Complete the guide at version 1.
2. Install a build with a higher `versionCode`.
3. Confirm the guide appears once; Skip lands on the library.
4. Relaunch; confirm the guide does not appear again.
5. Replay it from Settings; confirm it is still available.

### C.5 Speed rail

1. Settings -> Playback Speed; drag from 0.25x to 2.0x; confirm the bubble follows and the value snaps.
2. Tap the far right; confirm 2.0x.
3. Open the player -> Speed; confirm the same rail and the same eight values.
4. Open the advanced sheet; confirm the same rail again.
5. Set 200% font; confirm the bubble grows and no value is clipped.
