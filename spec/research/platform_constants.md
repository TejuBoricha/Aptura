# W9 — Platform-documented constants: layout breakpoints, PiP contract, Media3 defaults

**Accessed:** 2026-10-19 · **Method:** desk research only. No device or user testing.
**Purpose:** W8 audited the constants borrowed from *product* practice. This pass audits the layer beneath it — the **platform's own documented contract**. Platform defaults are the strictest form of "known solution" available: they are versioned, maintained by the vendor, and shipped in code we already depend on. Where W8 asked "what do shipping players do?", W9 asks "what has the platform and the library already decided for us?"

This pass also **tests a claim made at the end of Part 27** — that the remaining unaudited areas needed device testing rather than desk research. That claim was wrong on four counts, all recorded below.

---

## 1. Adaptive layout — our navigation was device-based, not breakpoint-based

Material 3 documents five breakpoints and, critically, **which navigation component belongs to each** `[V1]`:

| Breakpoint | Width | Common devices | Panes | Navigation |
|---|---|---|---|---|
| Compact | **< 600dp** | Phone portrait | 1 | Navigation **bar**, or modal expanded rail |
| Medium | **600–839dp** | Tablet portrait, foldable unfolded | 1 (recommended) or 2 | Navigation **bar** (two-pane) or **rail** |
| Expanded | **840–1199dp** | Phone landscape, tablet landscape | 1 or 2 (2 recommended) | Modal or standard expanded **rail** |
| Large | **1200–1599dp** | Desktop | 1 or 2 | Rail |
| Extra-large | **1600dp+** | Ultra-wide | 1–3 | Rail |

Canonical **list-detail** layout `[V1]`: compact = **1 pane**; medium = 1 (recommended) or 2; **expanded = 2 panes**. Fixed-and-flexible panes: the fixed pane is **360dp by default**. Split-pane: two 50% panes. Android's supporting-pane guidance: medium (w600dp) = 50/50; expanded (w840dp) = **supporting pane 30%, main 70%** `[V1]`.

Margins and gutters `[V1]`: compact **16dp / 8dp**; medium and expanded **24dp / 16dp**; large and extra-large **24dp / 24dp**.

Android also documents the **state-preservation behaviour** when the breakpoint changes at runtime `[V1]` — narrowing from expanded keeps the *detail* pane visible and hides the list; widening to expanded shows list and detail together and marks the selected item; widening with only the list visible shows list plus a **placeholder** detail pane. This matters directly because our target set includes foldables, split-screen, free-form windows and phone landscape.

**Finding.** Doc 06 §3 defined navigation as *"bottom navigation with Library / Folders / Playlists / Search … Tablet: two-pane (list ↔ detail) where stated"*. Two defects:
1. **Device-based, not width-based.** "Tablet" is not a breakpoint. A phone in landscape is expanded (840dp+); a tablet in split-screen is compact. M3 states this explicitly: design for breakpoints *because* devices change class by orientation, folding and multi-window.
2. **No rail.** A bottom bar at 840dp is a documented deviation — the guide puts a rail at medium and expanded.

**Change:** doc 06 now expresses navigation and pane behaviour in breakpoints with the documented component per class, and records the state-preservation rules.

---

## 2. Picture-in-Picture — a documented contract we had reduced to two API names

`PictureInPictureParams.Builder` documents hard constraints `[V1]`:

| Setting | Documented contract |
|---|---|
| `setAspectRatio` | Aspect ratio (width/height) **must be between 2.39:1 and 1:2.39 inclusive**; does not change on rotation |
| `setActions` | **Truncated** to `Activity.getMaxNumPictureInPictureActions()` |
| `setSourceRectHint` | Window-coordinate bounds of the area that stays visible; for best effect it **should also match the aspect ratio**; on **Android 12+ reused to improve the exit transition** |
| `setSeamlessResizeEnabled` | *"This should normally be the case for video content"*; default **true** |
| `setAutoEnterEnabled` | API 31+ |
| `setExpandedAspectRatio` | API 33+; must **not** fall within 2.39:1–1:2.39; requires `FEATURE_EXPANDED_PICTURE_IN_PICTURE` |
| `setCloseAction` | API 33+ |

**The purpose-built implementation.** AndroidX Core ships `androidx.core.pip.**VideoPlaybackPictureInPicture**`, documented as *"Picture-in-Picture implementation optimized for video playback applications. Enables seamless resize and allows tracking a View to automatically update the source rectangle hint for smooth animations"* via `ViewBoundsTracker` `[V1]`. Sibling `BasicPictureInPicture` is described as the general case, with *"Seamless resize is disabled and no sourceRectHint is used."*

**Finding.** FR-PLAT-004 reduced PiP to `setAutoEnterEnabled` plus an explicit path for 26–30. It did not carry the aspect-ratio clamp (a 2.39:1 anamorphic film sits exactly at the platform limit), the action-count cap, the source-rect/exit-transition behaviour, or the existence of a video-specific implementation in AndroidX. **We had specified the API call and not the contract.**

**Change:** FR-PLAT-004 now carries the platform contract and names `VideoPlaybackPictureInPicture` as the sanctioned implementation, so the build does not hand-roll `PictureInPictureParams`.

---

## 3. Media3's own defaults — and a correction to W8

Documented constants `[V1]`:

| Constant | Value |
|---|---|
| `PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS` | **5000** (already adopted in W8) |
| `C.DEFAULT_SEEK_BACK_INCREMENT_MS` | **5000** |
| `C.DEFAULT_SEEK_FORWARD_INCREMENT_MS` | **15000** |
| `C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS` | **3000** |
| `PlayerControlView.DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS` | 200 |
| `PlayerControlView.DEFAULT_REPEAT_TOGGLE_MODES` | `REPEAT_TOGGLE_MODE_NONE` |
| `show_rewind_button` / `show_fastforward_button` | default **true** |
| `show_subtitle_button` | default **false** |

### 3.1 Correction: asymmetry is not anomalous

W8 concluded that our old asymmetric 10 s/30 s seek default "is the Hulu pattern" and treated asymmetry as the anomaly. **Media3's own defaults are asymmetric: 5 s back, 15 s forward.** The honest statement is narrower:

- The **specific pairing 10/30 (a 1:3 ratio)** is Hulu's, and Hulu exists to clear ad-break recaps.
- **Asymmetry as such** is not anomalous — it is our own library's default, tuned for the "skip back a line / skip forward a recap" rhythm of long-form content.
- The **product-level consensus** among the apps our users actually learned the gesture in (VLC, YouTube, Netflix, Disney+) is nevertheless **symmetric ±10 s**.

The decision to keep symmetric ±10 s stands, but the *reason* is not "asymmetry is wrong" — it is the rule refinement in §4 below.

### 3.2 A rule we were missing entirely

`DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS = 3000`: *"A default maximum position for which a seek to previous will seek to the previous window."* In plain terms — **"previous" restarts the current item if you are past 3 s, and jumps to the previous item if you are within 3 s.** This is the near-universal media-player behaviour, it is Media3's documented default, and our FR-CTRL-002 simply said "previous/next item" with no such rule. Adopted.

---

## 4. Rule refinement — two classes of constant

W8 established "inherit the default of the stack you already chose." W9 shows that instruction is **incomplete**, because we inherited Media3's 5 s auto-hide but did *not* inherit its 5 s/15 s seek increments. That is not inconsistency once the two classes are separated:

| Class | What it is | Where the answer lies | Our cases |
|---|---|---|---|
| **A — invisible constants** | Values the user never forms an expectation about: timeouts, update intervals, tolerances, thresholds | **Inherit from the stack you chose.** The library has tested it against real devices; you have not | auto-hide 5000 ms; time-bar update 200 ms; repeat default `NONE` |
| **B — learned interactions** | Values the user has a **motor habit** for from daily use: seek increments, gesture step sizes, control placement | **Follow product-level consensus**, even when your library defaults otherwise — the habit was learned in YouTube/VLC, not in ExoPlayer | symmetric ±10 s seek; the 3 s "previous restarts item" boundary (both the consensus *and* the library agree here) |

Add this test to the method: *would a user notice, and would they already have an expectation?* If no → inherit (Class A). If yes → follow the consensus (Class B), and cite the consensus.

---

## 5. Seek parameters — the mechanism was documented and we described prose instead

`SeekParameters` `[V1]`: *"the predefined `EXACT`, `CLOSEST_SYNC`, `PREVIOUS_SYNC` and `NEXT_SYNC` parameters are suitable for most use cases. **Seeking to sync points is typically faster but less accurate than exact seeking.**"* An instance carries `toleranceBeforeUs` and `toleranceAfterUs`, so "seek to x, accepting a keyframe within ±N ms" is expressible directly. `DEFAULT` is the builder default; `ExoPlayer.Builder.setSeekParameters` / `ExoPlayer.setSeekParameters` apply it; `MediaController` exposes `seekBack()`/`seekForward()` using the increments.

**Finding.** FR-PLAY-005 described remapping as a bespoke policy ("remap to nearest keyframe with UI honesty, then refine"). Media3 exposes it as a first-class parameter. **Change:** FR-PLAY-005 now names the mechanism (tolerance-based `SeekParameters`) rather than describing behaviour to be reinvented.

---

## 6. Q14 — settled from the platform, and the platform contradicts itself

Q14 asked for the default library sort order and view density. Desk research found vendors contradictory. The platform is a better witness — and it turns out **the platform's two APIs disagree too** `[V1]`:

| API | Default |
|---|---|
| `MediaStore.Video.Media.DEFAULT_SORT_ORDER` (legacy constant) | **`"title"`** |
| `MediaStore.Video.DEFAULT_SORT_ORDER` (legacy constant) | `"_display_name"` |
| `MediaStore.MediaColumns.DATE_ADDED` under `QUERY_ARG_MEDIA_STANDARD_SORT_ORDER` (modern) | **`DATE_ADDED` descending** — "the sorting is based on this column in descending order" |

**Reading.** The legacy constant says *name*; the modern standard-sort-order argument says *most-recently-added-first*. We query MediaStore through the modern path (DEC-011), and most-recent-first is the dominant gallery convention. **Decision for Q14: default sort = most recently added first, with name ascending available and remember-per-view.** Recorded with the contradicting legacy constant stated, so a reviewer is not surprised by it rather than quietly resolved.

**View density** also resolves from Material's canonical **feed** layout: compact 1 column of cards, medium 2, expanded 3, large 4 — which coincides with how phone video apps actually look (a single column of full-width video cards).

**Q14 is therefore closed as a decision, not left open as a design question.**

---

## 7. Limits

- **No device testing.** Breakpoint behaviour on foldables, PiP behaviour on OEM skins, and the feel of a rail vs a bar are not verified here. This pass establishes *what the platform documents*, not that our implementation will match it.
- **M3 renamed the concept.** Material now says "breakpoints" where it previously said "window size classes"; the dp values are unchanged (600/840/1200/1600) and `WindowSizeClass` remains the Compose API. Terminology drift, not a value change.
- **PiP constraint details are API-level**, and OEM implementations vary — the aspect-ratio clamp and the action cap are real but the *result* of exceeding them is device-dependent.
- **`VideoPlaybackPictureInPicture` was not opened at source level.** Its description and `ViewBoundsTracker` reference are from the API reference; whether it is stable or experimental was not confirmed.
- **Media3 constants are a snapshot** of the `release` branch as accessed 2026-10-19; they are `C` constants and could change with a major version.

---

## 8. Sources (accessed 2026-10-19)

**Official vendor documentation / source `[V1]`**
- Material 3 — Breakpoints overview (five classes; panes, navigation, communication, action per breakpoint): https://m3.material.io/m3/pages/breakpoints
- Material 3 — Breakpoints: Medium (600–839dp; rail; two-pane 50/50; ergonomic regions): https://m3.material.io/foundations/layout/breakpoints/medium
- Material 3 — Breakpoints: Expanded (840–1199dp; fixed pane 360dp; split-pane; dynamic transition triggers): https://m3.material.io/foundations/layout/breakpoints/expanded
- Material 3 — Canonical layouts: list-detail (1 pane compact; 1 or 2 medium; 2 panes expanded+): https://m3.material.io/foundations/layout/canonical-examples/list-detail
- Material 3 — Layout overview: https://m3.material.io/foundations/layout/layout-overview
- Android Developers — Canonical layouts (supporting pane at w600dp 50/50, w840dp 30/70; list-detail state preservation across breakpoint changes): https://developer.android.com/develop/ui/views/layout/canonical-layouts
- AndroidX — `WindowSizeClass` lower bounds (`WIDTH_DP_MEDIUM_LOWER_BOUND`, `Compact` 0dp, `Medium` 600dp, `Expanded` 840dp, `Large` 1200dp, `ExtraLarge` 1600dp): https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/material3/material3-adaptive-navigation-suite/src/commonMain/kotlin/androidx/compose/material3/adaptive/navigationsuite/WindowSizeClassHelper.kt
- Android — `PictureInPictureParams.Builder` (aspect ratio 2.39:1 / 1:2.39; action truncation; source rect hint + Android 12 exit transition; seamless resize default true; auto-enter API 31+; expanded aspect ratio API 33+; close action): https://developer.android.com/reference/android/app/PictureInPictureParams.Builder
- Android platform source — `PictureInPictureParams.java` (constraint comments in source): https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/PictureInPictureParams.java
- AndroidX Core — `VideoPlaybackPictureInPicture` (optimised for video playback; enables seamless resize and `ViewBoundsTracker`): https://developer.android.com/reference/kotlin/androidx/core/pip/VideoPlaybackPictureInPicture
- AndroidX Core — `BasicPictureInPicture` ("Seamless resize is disabled and no sourceRectHint is used"): https://developer.android.com/reference/kotlin/androidx/core/pip/BasicPictureInPicture
- Media3 — `C` constants (`DEFAULT_SEEK_BACK_INCREMENT_MS = 5000`, `DEFAULT_SEEK_FORWARD_INCREMENT_MS = 15000`, `DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS = 3000`): https://developer.android.com/reference/androidx/media3/common/C
- Media3 — `C.java` source: https://github.com/androidx/media/blob/release/libraries/common/src/main/java/androidx/media3/common/C.java
- Media3 — `SeekParameters` (`EXACT`/`CLOSEST_SYNC`/`PREVIOUS_SYNC`/`NEXT_SYNC`, `toleranceBeforeUs`/`toleranceAfterUs`): https://developer.android.com/reference/androidx/media3/exoplayer/SeekParameters
- Media3 — `PlayerControlView` (show timeout 5000; rewind/fast-forward/subtitle button defaults; `DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS`): https://developer.android.com/reference/androidx/media3/ui/PlayerControlView
- Media3 — `ExoPlayer.Builder` (`setSeekParameters` default `SeekParameters.DEFAULT`; `seekBackIncrementMs` default): https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer.Builder
- Android — `MediaStore.Video.Media.DEFAULT_SORT_ORDER` = `"title"`: https://developer.android.com/reference/android/provider/MediaStore.Video.Media
- Android — `MediaStore.Video.DEFAULT_SORT_ORDER` = `"_display_name"`: https://developer.android.com/reference/android/provider/MediaStore.Video
- Android — `MediaStore.MediaColumns.DATE_ADDED` (standard sort order = descending): https://developer.android.com/reference/android/provider/MediaStore.MediaColumns
