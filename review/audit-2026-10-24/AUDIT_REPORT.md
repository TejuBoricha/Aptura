# Aptura - Follow-up Audit and Remediation

**Snapshot:** `main` @ `8ad9ebb` (2026-10-26)
**Baseline:** `review/audit-2026-10-23/AUDIT_REPORT.md` @ `588cc3a`  
**Delta for the first round:** 56 files, +5,430 / -799 across `14f26d1^..90ce6b9`  
**Delta for the data-integrity round:** 18 files, +1,389 / -402 across `48f6b43..65e54be`  
**Delta for the visual/capability round:** includes the reusable pixel helper, four coarse pixel guards, the route-theme resolver and the manifest feature/category matrix  
**Quality gate after the final code change:** **582 tests / 92 suites, 0 failures, 0 skipped**; lint 0 errors; `assembleRelease` + `verifyReleaseApk` clean
**Runtime evidence:** `review/audit-2026-10-24/evidence/` (9 curated captures + UI XML); new visual guards are Robolectric pixel checks, not device screenshots

> This round was not a second hunt for the P0 playback defects fixed in the previous audit. It targeted the layer underneath those fixes: **features that were absent, controls that looked wired but were not, and UI that only worked at the reviewer's window size, font scale and theme.** The report records what was found, what changed, what is still open, why the earlier passes missed this class of defect, and what now prevents it from coming back.

---

## 0. Verdict

The previous audit made Aptura **correct**. This round makes it **complete enough to behave like a finished product** on tablets, in Light mode, at large text sizes, and when the user tries to manage files rather than only play them.

The most important outcome is not any single screen. It is that the app now has **one source of truth for each cross-surface decision**:

- one eight-speed list and one speed control shared by Settings and both player surfaces;
- one appearance model shared by the library, Settings, the guide and system bars, with the player deliberately outside it;
- one tablet index that owns collection navigation instead of duplicating it in chips;
- one file-action pipeline that copies first, deletes only after consent, and reports retained originals;
- one rename contract that returns the file's new URI and moves the row, its saved position and its bookmarks with it;
- one transfer job that reports real byte progress, checks space before writing, and cancels without touching the originals;
- one language-label mapper so raw container codes stop leaking into the UI;
- one route-theme resolver so no screen can accidentally bring the Light palette into playback;
- one manifest capability matrix so the store listing and the runtime feature checks describe the same device set;
- one synchronous pixel-capture helper whose guards test flat surfaces, not fonts or golden files.

No known P0 defect remains in the reviewed scope. The three data-integrity caveats from the first pass are closed: SAF rename no longer orphans the row, large transfers have progress/cancellation/free-space preflight plus process-durable resumption, and missing items have an explicit per-item removal. The remaining risk is concentrated in **physical-device verification** and **live large-scale SAF/process-death validation**, not in the main journeys.

| Dimension | After this round | One line |
|---|---|---|
| Feature completeness | **B+** | The visible features now have working actions behind them. |
| UX consistency | **B+** | Speed, theme, file actions and language labels no longer disagree across screens. |
| Responsive / adaptive | **A-** | The library is a real tablet index, not a stretched phone; large text is a first-class state. |
| Accessibility | **B** | Touch targets and slider semantics are tested; TalkBack on hardware is still unverified. |
| Data / file safety | **A-** | Copy-before-delete, URI remap for SAF rename, process-durable cancellation/resumption and explicit missing-item removal. |
| Test / QA | **B+** | Strong Robolectric coverage, coarse pixel guards and a manifest capability matrix; debug-build instrumented E2E runs in CI. Release-variant instrumentation was attempted and rejected because R8 strips test-only stdlib/coroutines members, so release verification is the artifact invariants plus a signed manual smoke pass. |
| Evidence honesty | **B** | Library/Settings captures are clean; the player capture limitation is documented rather than hidden. |

---

## 1. Method and limits

### 1.1 What was reviewed

- Every commit in `14f26d1^..90ce6b9`, with the delivered behavior checked against the code, not the commit message.
- The library at phone width, tablet width (2560 x 1600, 240 dpi), Light and Dark, 100% and 200% font scale.
- Settings, the welcome guide, the player controls, the speed sheet, the subtitle sheet, the selection bar and the compact sidebar.
- The pixels beneath four surfaces that had failed structurally before: the scrolled Settings bar, the selection bar, the guide footer and the player's quick speed sheet under a Light app theme.
- The merged release manifest's `<uses-feature>` declarations and component intent categories, because those decide which devices the store considers compatible.
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
- Release instrumentation on the R8-minified app: attempted on 2026-10-27 and rejected because the test APK links against test-only Kotlin/coroutines symbols that the app pass removes. Debug-build instrumented E2E runs in CI; release verification remains `assembleRelease` + `verifyReleaseApk` + a signed manual smoke pass.
- Large SAF copies at multi-gigabyte scale, removable SD-card removal during a transfer, and process-death recovery on physical devices.
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
- Settings and both player surfaces (the speed chip sheet and the advanced sheet) use the same list and the same rail. The quick sheet later drifted back to a vertical list; F-21 records the re-fix and the guard that now prevents a third version.

**Proof.** `PlaybackSpeedOptionsTest` (2), `SpeedRailMathTest` (5), `SettingsSpeedOptionsTest` (6), `PlayerSpeedRailTest` (4, now covering both player sheets), `SettingsChipContrastTest` (4). The tests cover every speed, half speed, tapping the far right for 2.0x, an unknown stored speed snapping to the nearest value, and the rail remaining reachable after scrolling.

---

### F-07 - PiP was offered on devices that cannot use it [P2]

**Found.** The inline player bar and the overflow menu always showed **Picture in Picture**, even when the system feature was absent. Tapping it failed silently or did nothing.

**Why it mattered.** A dead control is worse than no control, especially in the player where every tap is deliberate.

**Fix (`cec17b1`).**
- `pipSupported` is passed into `PlayerScreen`.
- The inline control and the overflow item are both omitted when the feature is absent; every other action stays in place.
- `DeviceCapabilities.supportsPictureInPicture` is the single runtime check (API 26+ **and** the system feature), used by `MainActivity`.
- The manifest now declares `android.software.picture_in_picture` with `required="false"`, so a device without PiP can install Aptura instead of being filtered out by the store. The manifest invariants require both halves: the optional feature and `android:supportsPictureInPicture="true"` on the activity.

**Proof.** `PlayerPipAvailabilityTest` (1) on a phone without PiP; `PlayerWideTopBarTest` (2) on an 800 dp window confirms the remaining actions stay inline with full touch targets; `DeviceCapabilitiesTest` (3) covers feature present, feature absent and an API below 26; `MergedManifestInvariantsTest` (21) pins the optional declaration, rejects a required one, rejects camera/telephony/location/microphone/Bluetooth/NFC/TV/Auto features or categories, and parses provider authorities before asserting the debug provider is absent.

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

**Proof.** `MediaLibraryScreenUiTest` (16) includes the explicit Select entry, the 680 dp centered bar, single and bulk delete copy, bulk move copy, and clipboard controls. `MediaFileTransferTest` (6) proves bytes are copied, the source is retained, progress reaches 100%, space shortfalls are reported, cancellation cleans up, and resumed sources are not created again. `ApturaMediaFileManagerTest` (4) covers extension preservation, an explicit extension, a year in the title, and a trailing dot. Live rename proof: `AuditClip` -> `AuditClipRenamed`, with MediaStore `_display_name=AuditClipRenamed.mp4` and `_data=.../AuditClipRenamed.mp4`; the fixture was restored to `AuditClip.mp4` afterwards.

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

### F-18 - A SAF rename orphaned the library row, its progress and its bookmarks [P1]

**Found (carried from section 4.2 of the first pass).** `DocumentsContract.renameDocument` returns a **new** document URI, but `ApturaMediaFileManager.renameMediaFile` returned only `Boolean`. The caller updated the visible title and kept the old URI. The next scan could then see the old row as missing and the renamed document as a brand-new video, splitting saved progress and bookmarks.

**Fix.**
- `renameMediaFile` now returns `MediaRenameResult`: `Renamed(uri, displayName)`, `PermissionRequired(intentSender)` or `Failed`. The callback is gone, so callers cannot accidentally treat "needs consent" as success.
- Both library rename surfaces (card overflow and details sheet, including the details route in `Navigation.kt`) pass the provider-allocated URI back through `MediaLibraryViewModel.renameVideo(uri, title, newUri, displayName)`.
- `MediaItemDao.renameAndRemapUri` moves `media_items.uri`, `displayName`, `title` and `customTitle` in one UPDATE, so the row id, playback position and per-video preferences stay attached to the same record.
- `MediaRepositoryImpl` also remaps `bookmarks.mediaUri` when the URI changes. A title-only update still uses `updateCustomTitle`, so it cannot overwrite the real file name or its extension.
- The extension rule is unchanged: prefer the current file's extension, then the MIME type; `Dr. Strangelove` is a title, not a suffix.

**Proof.** `ApturaMediaFileManagerTest` (5) drives the four extension cases through `DocumentsContract.renameDocument` against a registered `ContentProvider`, asserting that the provider receives the requested name and that its **new** document URI is returned; `renamingALocalFileReturnsTheNewUri` covers the file path. `MediaRepositoryTest` (9) includes `renameVideo_withANewSafUri_movesTheRowAndBookmarks_andKeepsProgress`, which seeds 42 s of progress, renames `Clip.mp4` to `Holiday.mp4` under a new SAF URI, and asserts the old URI is gone, the progress survives and the bookmark follows. The real provider path is now covered outside the unit gate: `DocumentsProviderRenameTest` (2, instrumented on the API 35 emulator) renames through Android's real `DocumentsContract` against a debug-only `DocumentsProvider` whose `renameDocument` allocates `renamed-1`. It asserts that the returned URI is the new document, that the old document ID stops resolving, and that an extension-bearing title reaches the provider unchanged. The provider is protected by `MANAGE_DOCUMENTS`, is declared only in the debug manifest and is absent from the release merged manifest. The instrumented test runs under `connectedDebugAndroidTest`, not under `tools/gate.ps1`.

---

### F-19 - A large copy or move looked frozen and could not be stopped [P1]

**Found (carried from section 4.3 of the first pass).** `MediaFileTransfer.copyIntoTree` copied in a loop with no progress callback and no cooperative cancellation. A multi-gigabyte move had no visible state between "start" and the final dialog.

**Fix.**
- `copyIntoTree` is now `suspend` and copies in 64 KiB chunks. It reports `MediaTransferProgress(completedFiles, totalFiles, copiedBytes, totalBytes, currentName)` at most every 200 ms, with forced first, per-file and final updates; `fraction` uses bytes, then file count when a provider hides sizes.
- Cancellation is cooperative (`ensureActive` plus a `yield` per chunk). A cancelled copy deletes only the in-progress destination document; completed copies stay, every original stays, and clipboard entries are retained for retry. Copy-before-delete is unchanged: deletion starts only after the whole copy phase completes.
- `measureSpace` preflights the destination before writing. Free space comes from the provider's **root** (`Root.COLUMN_AVAILABLE_BYTES`), matched to the tree document id by exact or `rootId:`/`rootId/` prefix. A provider that reports no quota returns `null` and the copy is attempted, with a real write failure reported instead of a false refusal. A malformed tree returns zero available bytes.
- The library now shows a **TransferProgressDialog**: verb + count, current file, byte progress, real percentage, and a **Cancel** button that becomes "Cancelling…". It cannot be dismissed by tapping outside. A shortfall produces a **Not enough space** dialog naming both required and available sizes and confirming nothing was copied.
- A cancellation result names how many files were copied, states that the unfinished file was removed, and states that no originals were deleted (or that originals are unchanged for a copy).
- `MediaTransferWorker` and `MediaTransferJournal` now move that same copy contract outside the composition. WorkManager restores one unique job after process death; the journal commits the in-progress destination before writing and each completed source after writing, so a restored run removes only the partial document and skips completed sources. The selected SAF tree retains read/write permission for the worker, and the journal stays until move deletion consent completes.
- No non-media notification is posted. R1 keeps the media-session `POST_NOTIFICATIONS` exemption (FR-PLAT-002), so a transfer notification would either need an undeclared permission or break that exemption. The in-app dialog remains the progress and Cancel surface; process start resumes an unfinished journal with `KEEP`, and the library rehydrates the same journal when it opens.

**Proof.** `MediaFileTransferTest` (6) covers bytes copied with the source retained, byte progress reaching 100%, a shortfall before any write, a provider with no quota, a mid-copy cancellation that deletes the partial destination and leaves the source byte-identical, and resumed-source skipping. The durable path adds `MediaTransferJournalTest` (7: field round trip, id-scoped clear, corrupt JSON, remaining-source calculation), `MediaTransferWorkerTest` (5: partial removal and completed-source skipping in one resumed run, post-copy idempotence, preflight shortfall, cancelled journal, WorkData progress round trip) and `MediaTransferSchedulerTest` (6: unfinished-journal refusal, journal-before-enqueue, queued-work refusal, id-scoped cancel, process-start resume, finished/cancelled no-resume). The full gate is green; a connected-device kill during a multi-gigabyte provider transfer remains physical-device work.

---

### F-20 - Missing videos were badged but not removable per item [P2]

**Found (carried from section 4.4 of the first pass).** A file deleted outside Aptura could remain as a **Missing** card when it carried saved progress. Settings had a bulk **Remove all**, and the card was badged, but there was no per-item action. Offering Play, Rename, Share or Delete on a file that no longer exists produces failures with no useful outcome.

**Fix.**
- A missing card's overflow now contains **Video Details** and one destructive action, **Remove from library**. Play Next, Add to Queue, Select, Move, Share, Hide and Delete are not offered.
- The details sheet replaces the entire action area with a `MissingVideoNotice` that explains the file may have moved, been deleted or be on a disconnected drive, and offers **Remove from library**.
- `RemoveMissingVideoDialog` is explicit: nothing on disk will be deleted because the file is already unavailable; the saved position and bookmarks **will** be removed; if the file returns it is added as a new video.
- `MediaLibraryViewModel.removeMissingVideo` deletes the index row (and its progress/bookmarks) through `removeVideoFromLibrary`; it never touches storage. The Settings bulk action remains for cleanup.
- This is deliberately not automatic: a temporarily disconnected drive must not silently erase progress.

**Proof.** `MediaLibraryScreenUiTest` (17) includes `missingCardOffersOnlyRemove_andConfirmsBeforeDeletingTheRow`, which asserts the absent actions, the confirmation copy and the callback URI. `MediaLibraryViewModelTest` (21) includes `removeMissingVideo_deletesTheIndexRow`.

---

### F-21 - The quick speed sheet reverted to an eight-row list, and the forced-dark rule had no pixel proof [P2]

**Found.** F-06 replaced the two-row grid in the advanced sheet and the five-chip row in Settings, but the sheet opened by the player's speed chip still rendered a `LazyColumn` of eight rows. The two surfaces therefore looked like different products again, and the quick sheet grew taller than a phone in landscape.

The rule that keeps playback dark also lived inline in `MainNavigation`. Structural tests could prove `pipSupported`, labels and semantics, but not that the Light palette never reaches the player's **separate sheet windows**: a sheet is its own Compose root, and a missing visual assertion would only be noticed by a human opening it on a Light phone.

**Why it mattered.** The speed chip is the shortest path to changing speed during playback, so its sheet is the one users see most. A regression in the route override is worse: every player menu would turn near-white while the video frame stayed black, which is the exact defect F-01 set out to prevent.

**Fix (this round).**
- The quick sheet now renders the same `SpeedRail` as Settings and the advanced sheet; the eight-row `LazyColumn`, its `items` import and its bespoke selected-row layout are gone. Selecting a speed still closes the sheet through the existing view-model contract.
- `themeModeForRoute(isPlayerRouteActive, appThemeMode)` is a pure function in `Theme.kt`, used by `MainNavigation`. It is the only place that decides the player is dark, and it is unit-tested for every app mode.
- A new `ComposePixels` helper captures a Compose window synchronously (`ViewRootForTest.view.draw(Canvas(bitmap))`) under Robolectric `NATIVE` graphics. `captureToImage()` remains forbidden: it waits for a frame callback Robolectric never delivers and times out.
- Four coarse pixel guards now assert flat regions only, never glyphs or golden files: the scrolled Settings bar, the selection bar, the guide footer, and the quick speed sheet under a Light app theme.

**Proof.** `ThemeRouteTest` (2) pins the resolver for all three app modes and the non-player route. `PlayerSheetThemePixelTest` (1) renders the sheet through the resolver while the app is Light and asserts the fixed **dark** `SurfaceElevated2` palette; flipping the resolver to Light was verified to fail with light pixels (`0.988, 0.988, 0.992`) instead of the dark token, so the guard is not comparing the pixels to themselves. `PlayerSpeedRailTest` (4) covers both sheets, including that the quick sheet no longer contains a "Normal (1.0x)" row. `SettingsTopBarPixelTest` (2), `SelectionBarPixelTest` (2) and `GuideFooterPixelTest` (3) pin the other three surfaces in Light and Dark, including 200% font.

---

### F-22 - A denied move deletion hid the copies that already existed [P2]

**Found (while exercising the durable move on the API 35 tablet).** The copy phase succeeded, Android showed its delete-consent dialog, and the reviewer tapped **Deny**. The result dialog said **Move not completed**, **0 of 1 videos moved**, and **Originals still in the library**. The video had in fact been copied to the selected folder; only the deletion was denied. Nothing in the dialog said a copy existed.

**Why it mattered.** A user told that a move did not happen will reasonably retry it. The retry creates a second copy in the destination while the original is still in the library, so one intended move becomes two or three files. The partial-deletion case had the same gap: it reported the deletion count but not which copies remained in both places.

**Why earlier reviews missed it.** The transfer tests asserted bytes, progress, cancellation, free space and the deletion counts. None asserted the **split outcome** of a move: deleted originals versus copies that survive a denial. The manual dialog check only confirmed that a result appeared, not that its sentence matched the disk.

**Fix (this round).**
- `moveResultMessage` is now a pure builder that takes the copied job, the deleted URIs, the retained originals and the titles. It reports the copy count independently of the deletion count.
- A fully denied deletion produces **Copied, originals kept**, states that the videos now exist in both places, names them, and tells the user to delete the originals only after checking the copies. A partial result names the deleted count, the kept originals and any sources that were never copied. **Move complete** is reserved for every source copied and every original deleted.
- The list of names stays bounded at three plus a count, so a large batch cannot turn the dialog into a wall of text.

**Proof.** `MediaTransferResultMessagesTest` (5) pins complete, denied, partial, uncopied and bounded-name outcomes. The denied case explicitly asserts that the message contains **both places** and does not contain **Move not completed**. The before-fix dialog is captured in `evidence/09_move_denied_before_fix.png`: copy complete, deletion denied, yet the text says **0 of 1 videos moved**.

---

## 3. Verification

### 3.1 Gate

After the visual/capability round:

```
582 tests / 92 suites / 0 failures / 0 skipped
lintDebug: 0 errors
assembleRelease + verifyReleaseApk: clean
```

The test counts were read from `app/build/test-results/testDebugUnitTest/*.xml`, not from a cached console line. The data-integrity round ended at **531 / 82**; the visual/capability round added **25 tests**; the durable-transfer implementation added one resumed-copy case (included in the 557). This proof round adds **25**: 18 journal, worker and scheduler cases for the process-durable path, a release-manifest guard that parses provider authorities so the debug-only provider cannot ship, and 5 move-result cases that separate copying from deletion.

The pixel tests are deliberately narrow. They capture the window synchronously and assert a flat fill region of one to a few pixels, away from text, rounded corners and hairlines. They catch a surface that vanishes, turns translucent or switches palette without freezing a screenshot that every font update would break.

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
| `evidence/09_move_denied_before_fix.png` | F-22 before the fix: Android denied deletion, the copy existed, but the dialog said **Move not completed** and **0 of 1 videos moved** |

### 3.4 Evidence limitations

- **Player `SurfaceView` tiling.** The emulator composites the video layer as repeated tiles under SwiftShader and under the host GPU. The tiling is in the capture, not in the decoded file: pixel checks of flat regions are single-color, and the same fixture decodes cleanly outside the player. Evidence 05 is therefore used only for the dark player chrome and the dark letterbox, never to judge video output. The dark-menu claim is additionally supported by evidence 06 and by the forced-dark route code.
- **Emulator render ghosting.** One capture pass showed faint retained frames after repeated screencaps. A reboot cleared it; the curated evidence was recaptured. Flat regions in the final PNGs were checked numerically (one unique color) before use.
- **Robolectric vs. hardware.** The responsive tests run at fixed qualifiers (for example `w900dp-h1100dp-xhdpi` and `w1200dp-h900dp-xhdpi`). They prove layout decisions, not real GPU performance.
- **Pixel guards are not screenshots.** They draw the Compose view into a bitmap under Robolectric and assert flat fill regions, so they prove opacity and palette, not anti-aliasing, shadows or the `SurfaceView` video layer. A deliberate font or spacing change does not fail them; a translucent bar, a missing surface or a light palette in the player does.
- **No physical device.** Codec/HDR/thermal/TalkBack conclusions are limited to what the emulator and the code can prove.

---

## 4. What remains pending

### 4.1 Physical-device verification [highest residual risk]

**Why it is open.** The emulator cannot prove hardware codec behavior, HDR tone mapping, thermal throttling, battery cost, Bluetooth A/V sync or TalkBack navigation.

**Next step.** Run the same evidence protocol on at least one mid-range and one flagship phone: H.264/HEVC/AV1/VP9, HDR10, 4K/60, 10-bit, AC3/EAC3/DTS/TrueHD, Bluetooth earbuds with a lip-sync clip, TalkBack through library -> player -> sheets, and a 30-minute thermal soak.

### 4.2 Process-durable transfers [closed by `8ad9ebb`]

**Finding.** Progress, cancellation and preflight previously lived in the library composition (`rememberCoroutineScope` plus `remember` state). Leaving the screen or process death cancelled the copy instead of resuming it.

**Fix (`8ad9ebb`, proof added after).** `MediaTransferWorker` runs the existing `MediaFileTransfer` byte loop through a unique WorkManager request. `MediaTransferJournal` uses synchronous SharedPreferences commits to record the stable job, completed source URIs and the in-progress destination document; a restored worker removes a truncated document and resumes only the remaining sources. The library observes WorkManager progress, rehydrates after process death, retains the journal through move-deletion consent, and persists read/write SAF grants when the destination is selected. Cancellation is journaled before WorkManager cancellation, and a copy-completion race cannot turn a late Cancel into source deletion. `ApturaApplication` also calls `resumePending` at process start, so a run WorkManager preempted while no library screen existed is restored without waiting for the screen.

**Why no notification.** WorkManager can post a progress notification through a foreground service, but that is a non-media notification. R1 intentionally has no `POST_NOTIFICATIONS` permission: the media-session exemption covers playback, and adding a transfer notification would either break FR-PLAT-002 or leave the notification undeliverable on Android 13+. The in-app transfer dialog is the progress and Cancel surface; the journal, not a notification, is the durable state.

**Proof.** `MediaTransferJournalTest` (7), `MediaTransferWorkerTest` (5) and `MediaTransferSchedulerTest` (6) drive the journal, the restored worker and the scheduling guard directly; `MediaFileTransferTest` (6) covers the byte loop, progress, preflight and cancellation cleanup. The full gate passes. A connected-device kill during a multi-gigabyte SAF copy, including a provider that refuses partial-document deletion, remains a physical validation item.

### 4.3 Release-variant instrumented E2E is rejected

**What was tried (2026-10-27).** Setting `testBuildType` to `release` did create `connectedReleaseAndroidTest`; the test APK compiled, minified and installed, and the runner got past `AndroidJUnitRunner.onCreate`. The exact command was `gradlew -Paptura.testBuildType=release :app:connectedReleaseAndroidTest` on the API 35 tablet emulator. It then failed in sequence:

1. `NoClassDefFoundError: kotlin/LazyKt` from `TestDirCalculator`.
2. `NoSuchFieldError` on `kotlin/time/Duration$Companion` after keeping `kotlin.LazyKt`.
3. `NoClassDefFoundError: kotlinx/coroutines/JobKt` from Compose's `IdlingResourceRegistry` after keeping `kotlin.time.**`.
4. `NoSuchMethodError: Job.complete()` after keeping `kotlinx.coroutines.JobKt`.

**Cause.** The app's R8 pass removes or rewrites stdlib and coroutine members that only the test runner uses. The test APK is linked against the app's mapping, so every test-only keep uncovers the next dangling symbol. Keeping the whole stdlib/coroutines surface, or disabling app shrinking, would stop the test from exercising the shipped artifact. Test-APK minification cannot fix it because Kotlin and coroutines classes are provided by the app classloader, not bundled in the test APK.

**Decision.** Do not chase R8 test scaffolding. Debug-build instrumented E2E remains the behavioral check; release verification stays `assembleRelease` + `verifyReleaseApk` + lint plus the signed manual smoke. The attempted build and ProGuard changes were reverted; the debug suite was re-run at 5/5 and the gate at 582/92.

**Next step.** If release instrumentation becomes mandatory, the honest options are a dedicated non-minified `releaseTest` build type (which does not test the shipped artifact) or a device-farm smoke run of the signed release APK. Neither is worth adding today.

### 4.4 PiP-unsupported UI is test-only

**Why it is open.** The emulator supports PiP, so the hidden-control state cannot be photographed. The logic is now centralized in `DeviceCapabilities`, declared optional in the manifest and covered by `PlayerPipAvailabilityTest`, `PlayerWideTopBarTest` and `DeviceCapabilitiesTest`, but no device screenshot exists.

**Next step.** Verify on a device or emulator image without `FEATURE_PICTURE_IN_PICTURE`; capture the top bar and overflow.

### 4.5 Player capture remains unreliable on this emulator

**Why it is open.** The `SurfaceView` tiling affects both renderers. This is a capture limitation, not an app defect, but it means the player's video output cannot be used as visual evidence here.

**Next step.** Capture on hardware, or use a `TextureView`-based debug build for screenshots if pixel evidence of the video layer is required. Do not change the production surface path solely for screenshots.

### 4.6 The instrumented proof runs in CI, not in the local commit gate

**Why it stays separate.** `tools/gate.ps1` remains JVM-only for fast local feedback. `DocumentsProviderRenameTest` needs a real `DocumentsProvider` and `ApturaAppE2eTest` needs a real activity, so `.github/workflows/ci.yml` now runs `:app:connectedDebugAndroidTest` on an API 35 Pixel Tablet image in its own job. The local command is unchanged. The debug-only provider is excluded from release and the merged-manifest guard fails if its authority or class reaches the release manifest.

**Remaining risk.** GitHub emulator jobs can be slow or flaky. The separate job means an emulator outage cannot hide a unit, lint or release regression, but a red connected job still has to be investigated rather than ignored.

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
| `MediaLibraryScreenUiTest` | 17 | Select entry, selection/Back, batch delete/move copy, 680 dp centered bar, swipe, missing-card removal |
| `OnboardingScreenUiTest` | 10 | Skip, paging, content, footer clearance, last-page behavior, guide-seen once |
| `SettingsSpeedOptionsTest` | 6 | Every speed, half speed, far-right 2.0x, stored default, unknown snap, scroll reachability |
| `MediaLanguageLabelsTest` | 6 | ISO/BCP-47 names, `und` suppression, unknown code, casing |
| `GestureHudUiTest` | 6 | Compact double-tap HUD, both directions, volume/brightness, lock hint |
| `ApturaPalettesTest` | 5 | Dark tokens unchanged, light contrast, five accents, status colors |
| `SpeedRailMathTest` | 5 | Even fractions, index round trip, snapping, single speed, stored detent |
| `StartDestinationGateTest` | 6 | First run, update replay, same version, legacy 0 |
| `ApturaSettingsRepositoryTest` | 5 | Appearance/guide defaults and persistence |
| `ApturaMediaFileManagerTest` | 5 | Extension preservation, dot edge cases, renamed-file URI |
| `MediaRepositoryTest` | 9 | URI remap moves the row and bookmarks while progress survives |
| `SettingsChipContrastTest` | 4 | Selected/unselected contrast, radio semantics, no baked-in colors |
| `SettingsAppearanceTest` | 3 | Theme choice, accent choice, selected accent announcement |
| `ApturaThemeTest` | 3 | Palette application, accent swap, reset |
| `DeviceCodecSupportTest` | 3 | Labels, safe report, audio-only policy |
| `SettingsTopBarTest` | 2 | Opaque bar, 56 dp coverage |
| `SettingsTopBarPixelTest` | 2 | Scrolled bar is an opaque `SurfaceBase` strip in Light and Dark |
| `SelectionBarPixelTest` | 2 | Selection bar is an opaque `SurfaceElevated1` surface in Light and Dark |
| `GuideFooterPixelTest` | 3 | Footer strip opacity in Light/Dark, last row clears the footer, 200% fit |
| `PlayerSheetThemePixelTest` | 1 | Quick speed sheet stays on the dark palette under a Light app theme |
| `ThemeRouteTest` | 2 | Player route is dark for every app mode; other routes keep the chosen mode |
| `DeviceCapabilitiesTest` | 3 | PiP present, absent, and below API 26 |
| `MergedManifestInvariantsTest` | 21 | Permissions, exported components, features/categories, optional PiP, provider-authority parsing, debug-provider authority absent |
| `LibrarySidebarTest` | 2 | Wide index, 68 x 48 chip, 200% fallback |
| `MediaStorageActionsTest` | 5 | Trash only for MediaStore on API 29/30/35; mixed-provider batches refused |
| `PlayerSpeedRailTest` | 4 | Same ramp in both player sheets; chosen speed reaches the player |
| `PlayerWideTopBarTest` | 2 | Wide bar actions stay inline; PiP omitted when absent |
| `PlaybackSpeedOptionsTest` | 2 | Shared list and labels |
| `MediaFileTransferTest` | 6 | Bytes copied, progress, space shortfall, no-quota fallback, cancellation cleanup, resumed-source skipping |
| `MediaTransferJournalTest` | 7 | Field round trip, remaining sources, corrupt JSON, id-scoped clear |
| `MediaTransferWorkerTest` | 5 | Partial removal, resume skip, post-copy idempotence, shortfall, cancellation, progress data |
| `MediaTransferSchedulerTest` | 6 | Unfinished-journal refusal, journal-before-enqueue, queued refusal, id-scoped cancel, process-start resume |
| `MediaTransferResultMessagesTest` | 5 | Complete, denied, partial, uncopied, bounded-name move outcomes |
| `PlayerPipAvailabilityTest` | 1 | No PiP control without the feature |
| `LibraryTouchTargetsTest` | 1 | 48 dp targets |

That is **175 tests across the 35 suites most relevant to this round**; the full gate is **582 / 92**. Outside the unit gate, `DocumentsProviderRenameTest` (2) runs on a connected API 35 emulator against the debug-only provider; instrumented tests are not part of either count.

### 6.3 Layout tests at the sizes that break layouts

- `w900dp-h1100dp-xhdpi` for the expanded sidebar and 200% font fallback.
- `w1200dp-h900dp-xhdpi` for the 680 dp centered selection bar.
- `w800dp-h600dp-xhdpi` for the wide player top bar.
- Phone qualifiers for the PiP-absent overflow.
- `w400dp-h800dp-xhdpi` with `GraphicsMode.NATIVE` for the coarse pixel guards, including the player's separate sheet window.
- 200% font in both Robolectric and the emulator.

A layout that only works at the reviewer's window is no longer accepted.

### 6.4 Destructive-action contract

1. Copy first, verify the copy result, then request deletion.
2. If copy fails, the source remains and the dialog names it.
3. If deletion is partial, report both counts; never claim a clean move.
4. Offer system trash only where it can work (MediaStore, API 30+).
5. Keep failed items selected for retry.
6. Persist the provider-allocated URI after a SAF rename, move the row and bookmarks to it, and keep the saved position on the same row.
7. Preserve the file extension on rename; do not trust the visible title.
8. Preflight destination space when the provider reports a quota; if it does not, attempt the copy and report the write failure honestly.
9. Cancellation stops between chunks, removes only the partial destination, keeps every original, and keeps clipboard entries for retry.
10. A missing file gets one honest action, Remove from library, with explicit progress/bookmark consequences. Do not auto-purge a row that a disconnected drive may bring back.
11. A transfer journal is written before the first byte, completed sources are recorded synchronously, and WorkManager resumes the same job after process death without duplicating completed copies.

### 6.5 Evidence protocol

- Capture with `screenrecord`, pull the first frame, and keep the `uiautomator` XML beside it.
- Verify flat regions numerically before using a screenshot.
- Never use a `SurfaceView` capture to judge video output; state the limitation in the report.
- Record the build, window size, density, theme and font scale with the evidence.
- Do not call an image "clean" because the preview looks clean; check the file.

### 6.6 Documentation

- `DESIGN.md` now records the 248/88 dp sidebar, the 600/700 dp breakpoints, the 150% font fallback, the 68 x 48 chip, the 680 dp action bar, the shared speed rail, the player-dark rule, and the file-action contract, including URI remapping, rename-before-scan ordering, transfer progress/cancel/resumption and the missing-item rule.
- `.agents/MEMORY.md` Part 74 records the first audit round; Part 75 records the data-integrity round; Part 76 records the visual/capability round; Part 77 records the real `DocumentsProvider` rename proof; Part 78 records the process-durable transfer and the move-result contract.

### 6.7 Device capability matrix

The merged release manifest is now parsed for `<uses-feature>` and for the intent categories declared by every component, not only for permissions and components:

- `android.software.picture_in_picture` must be present with `required="false"`; a required declaration fails because it would filter out devices that cannot float a window.
- The activity must also declare `android:supportsPictureInPicture="true"`. One without the other fails: the flag alone hides the store contract, and the feature alone advertises a capability no activity implements.
- Any camera, telephony, location, microphone, Bluetooth or NFC feature prefix fails, even when it is optional. R1 is a local video player; a feature declaration is a capability claim.
- TV, Automotive, Wear and Leanback declarations fail; R1 targets phones and tablets only (DEC-005).
- Only `DEFAULT`, `LAUNCHER` and `BROWSABLE` intent categories are approved, so a library cannot quietly add a TV launcher or car entry point.
- `DeviceCapabilities.supportsPictureInPicture` is the single runtime check used by the activity and tested for present, absent and sub-API-26 devices.
- Storage-era behavior is pinned at API 29 (no system trash), API 30 (trash available) and API 35 (trash stays MediaStore-only; mixed-provider batches are refused).

### 6.8 Connected-device CI

`.github/workflows/ci.yml` now has a separate `connected` job on an API 35 Pixel Tablet image. It runs all five instrumented tests: the two `DocumentsProviderRenameTest` cases against Android's real `DocumentsContract`, and the three `ApturaAppE2eTest` journeys (launch, rotation, system Back). The JVM gate stays fast and local; the connected job is the protected check for behavior that Robolectric cannot model. Its reports are uploaded even when it fails. The job deliberately targets the debug build: the 2026-10-27 release-variant attempt is recorded in 4.3 and rejected, because a minified test APK cannot reliably link to an R8-optimized app.

---

## 7. Future prevention steps

These are not one-time fixes; they are the next layer of process.

1. **Coarse pixel guards are now in place** for the scrolled Settings bar, the selection bar, the guide footer and the player's quick sheet under a Light app theme. They deliberately assert flat regions, not glyphs or golden files. The remaining step is a hardware-captured screenshot matrix for typography, illustrations and the player's video layer, where a golden image is the only honest evidence.
2. **The real `DocumentsProvider` path is now proven.** `DocumentsProviderRenameTest` (2, instrumented, API 35) renames through Android's `DocumentsContract` against a debug-only provider that allocates a new document ID; it asserts the new URI is returned, the old ID stops resolving and the extension rule still holds. `MediaRepositoryTest` proves the row, saved progress and bookmarks follow that URI. The connected job in `.github/workflows/ci.yml` now runs this test and the three app journey tests on an API 35 tablet image, so the proof is no longer local-only.
3. **Exercise the durable transfer on physical storage.** The WorkManager/journal path is now covered at the JVM level by the journal, worker and scheduler suites, including resumed partial cleanup and process-start scheduling. The remaining step is a connected process-kill test during a large SAF copy, including a provider that refuses partial-document deletion and a grant that cannot be persisted.
4. **The capability matrix is now enforced in every gate**: the merged manifest's features and categories are allow-listed, PiP is optional in both the store declaration and the activity flag, `DeviceCapabilities` has present/absent/sub-API tests, and storage-era behavior is pinned at API 29/30/35. The remaining step is to run the same matrix on a device farm, including a real PiP-absent image.
5. **Release-variant instrumentation is not a goal.** The 2026-10-27 attempt showed R8 removing test-only Kotlin/coroutines members; every keep rule uncovered the next dangling symbol, and broad keeps would stop the test from exercising the shipped artifact. Keep the debug E2E job and add an explicit signed-release smoke checklist in CI if release coverage must grow.
6. **A physical-device lab pass** for codecs, HDR, thermal, Bluetooth and TalkBack. The emulator is a layout and logic tool, not a media-certification tool.
7. **A design-review checklist** applied to every new screen: empty, loading, error, disabled, 200% font, narrow width, wide width, Light, Dark, scrolled, keyboard open, navigation bar inset, and a destructive action with a partial failure.
8. **An audit cadence** that samples the product at the edges, not only the center: the user's cropped sidebar image found in one minute what a full-frame review missed for hours.

---

## 8. What is genuinely good and should be kept

- **The privacy posture is real.** No INTERNET permission, no telemetry, no accounts, no `MANAGE_EXTERNAL_STORAGE`; the settings screen proves it in plain language.
- **The player stays dark.** The route-level override and the literal black root are the right architecture; the menus inherit the dark palette. The rule is now a tested pure resolver, and the quick sheet's pixels prove a Light app theme cannot leak into playback.
- **The visual guards are honest.** They draw the real Compose window, assert a flat fill rather than a reference image, and fail on the defects semantics cannot see. They will not break on a font update, and they do not pretend to test the video surface.
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
| `65e54be` | Make SAF rename, missing-item removal, and transfers safe | Provider-allocated rename URI with row/bookmark remap, per-item missing-file removal, byte progress/cancel/free-space transfer pipeline |
| `8ad9ebb` | Make media transfers process durable | WorkManager transfer worker, synchronous journal, resumable completed-source tracking, persisted destination grants, race-safe cancellation |

**Diff:** 56 files, +5,430 / -799 across `14f26d1^..90ce6b9`; 18 files, +1,389 / -402 across `48f6b43..65e54be`; 6 files, +739 / -154 in `8ad9ebb`.

---

## Appendix B - Evidence capture notes

- Device: `Aptura_Tablet_API_35`, 2560 x 1600, density 240, font scale 1.0, Light theme for captures 01-04 and 07-08; player chrome in 05-06.
- Final state: Light theme, expanded sidebar, no selection, two fixture videos (`AuditClip.mp4`, `Episode01.mkv`).
- Capture method: `adb shell screenrecord --time-limit 2`, pull, `ffmpeg -frames:v 1`, then `uiautomator dump` for the XML companion.
- Pixel checks: flat light regions resolve to a single RGB value (`242,244,244` in the Settings header sample); player letterbox/control samples are dark (`10,11,14` and `15,12,38`).
- The screen-recording files were removed from the device after extraction so they would not be indexed as library videos. A stale `ev2d` row from an earlier recording was hidden for the final capture; that gap is now closed by the per-card **Remove from library** action (F-20).

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

### C.6 Transfer progress, cancellation and free space

1. Select two large videos -> Move -> choose a SAF folder.
2. Confirm the progress dialog names the current file, shows a percentage and byte counts, and offers **Cancel**.
3. Tap **Cancel** mid-copy; confirm **Transfer cancelled**, the unfinished destination file is gone, every original is still in the library, and the clipboard still holds the items.
4. Choose a destination with less free space than the selected total; confirm **Not enough space** names both figures and that nothing was copied.
5. Complete a move; confirm originals are deleted only after the copy phase and the result names any retained originals.

### C.7 Missing video removal

1. Delete a library file outside Aptura (for example `adb shell rm`), then Rescan.
2. Confirm the card shows **Missing**.
3. Open its overflow; confirm only **Video Details** and **Remove from library** are offered.
4. Open the details sheet; confirm the `File unavailable` notice replaces Play, Queue, Rename, Share, Open, Hide and Delete.
5. Confirm **Remove** states that the file is already gone, that progress and bookmarks are removed, and that a returning file is added as new. Confirm the row disappears.

### C.8 SAF rename keeps the row

1. Add a folder through Android's document picker.
2. Rename a video inside it and note its saved position and a bookmark.
3. Confirm the card keeps that position and bookmark, and that a Rescan does not create a duplicate card for the renamed file.
4. The MediaStore path keeps the same URI; the SAF path must use the provider-allocated URI returned by `renameDocument`.
