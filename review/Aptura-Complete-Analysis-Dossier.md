# Aptura — Complete Analysis Dossier

**Single combined document** · merged 2026-10-21

This dossier consolidates every analysis produced for Aptura into one file. Nothing was rewritten during the merge — only cross-references were updated so they resolve internally.

| Part | Content | Sections & IDs | Layer |
|---|---|---|---|
| A | Original Inspection Gallery (as submitted, annotated) | A.1–A.5 | Evidence as submitted |
| B | Consolidated UI/UX Review (v1) | §1–9 · findings F-01…F-12 | Visual/UX defects + doc critique |
| C | Deep-Dive (skills · subagents · web validation) | §10–17 · findings F-13…F-19, gesture parity, insets/adaptive/perf audits | Code-verified defect layer |
| D | Competitive Feature Gap Analysis & Roadmap ("Honest Player") | §1–12 + Appendices A–C · gaps G-01…G-29, sources [1]–[106] | Product/feature layer + roadmap |

**Reading conventions:** findings `F-nn` are defined in Parts B–C; section numbers are per-part; `file:line` anchors reflect repo state post-`ee726cc`; `[n]` citations resolve to Part D Appendix A; recommendations crossing an R1 non-goal carry `⚠ Requires ADR`; Part D's Now/Next tiers are the authoritative sequencing for Part C's §17 action plan.

**External artifacts (referenced, unchanged):** research notes `research_notes/Aptura media player competitive roadmap/` (4 files, incl. `code_gap_brief.md`); spec package `spec/`; capture set `review/captures/`.

---

# Part A–C — UI Inspection Gallery & Consolidated Review

**Date:** 2026-10-20
**Scope:** Visual inspection of four emulator captures (Pixel 8, Android 15) — Media Library (empty + populated), Video Player (portrait + landscape) — cross-referenced against `MediaLibraryScreen.kt`, `AndroidManifest.xml`, and `spec/06_UX_SCREEN_AND_STATE_SPECIFICATION.md`.
**Source:** The submitted inspection gallery (`ui_visual_inspection_gallery.md`) is integrated below as **Part A** with its images relocated into `review/captures/` and relative links; **Parts B–C** are the reviewer's audit of that gallery and the code behind it. Part A claims are preserved verbatim except for two mechanical fixes: absolute image paths → relative links, LaTeX math (`$\pm 10\text{s}$`) → plain `±10s`.
**Method:** Static inspection of stills + code reading. No runtime interaction, no instrumentation.
**Deep-dive method (Part C):** dedicated skills (`edge-to-edge`, `adaptive`, `jetpack-compose-performance`) + two parallel code-audit subagents (player UI; library & data pipeline) + web-validated competitor baseline, with independent line-level verification of every high-severity claim before it was written into this file.

**Evidence key:** · ✅ Verified on capture · ⚠️ Inferred (plausible cause) · ❓ Needs runtime verification

---

# Part A — Original Inspection Gallery (as submitted, annotated)

*The gallery below reproduces the submitted inspection doc faithfully — claims preserved verbatim, with a reviewer verdict added under each section. Where a claim is wrong, the verdict points to the finding that supersedes it (findings index in §2; details in Parts B–C).*

## A.1 · Media Library — Empty State (First Launch)
*When no videos have been scanned or stored yet, Aptura displays a helpful, uncluttered onboarding state.*

![Media Library Empty State](captures/screenshot_library_empty.png)

- **Surface Luminance:** OLED-black base (`#0A0E17`) with elevated icon circular disc (`#121824`).
- **Typography:** Bold headline with medium-emphasis guidance text.
- **Action Button:** Cyan accent pill button (`#00E5FF`) with 48dp touch target and refresh icon.

**Reviewer verdict:** Layout verified. The color value is wrong twice — code and `DESIGN.md` define `SurfaceBase = #0D0E11`; `#0A0E17` exists nowhere in the codebase (§12.3), and `#0D0E11` is a blue-tinted near-black, not "OLED-black." The copy contradicts itself ("ready" + "automatically" vs a manual refresh CTA), and the SCR-001 permission flow the spec requires is absent codebase-wide (F-07, §12.2).

## A.2 · Media Library — Populated Video Grid
*Real database items rendered in an adaptive 2-column grid with horizontal folder filtering and search.*

![Media Library Populated Grid](captures/screenshot_library_live.png)

- **Search Bar:** Integrated leading search icon with responsive "Search 4 videos..." placeholder.
- **Top Bar Actions:** Sort order menu toggle and manual scan trigger.
- **Filter Chips:** Horizontal chip strip ("All Videos", "/storage/emulated/0/Camera", "Movies", "Gaming"). Selected chips highlight with cyan accent borders.
- **Video Cards:**
  - High-contrast card surfaces (`SurfaceElevated1`).
  - Tabular figure duration badges (`tnum`) in the lower right corner (e.g. `1:15`, `6:12`, `18:45`, `2:30`).
  - File size and folder name labels with ellipsis truncation.
  - Watch progress indicator line along the bottom edge of cards showing resume position.

**Reviewer verdict:** Chip strip, bottom-right badge placement, `tnum`, and bottom-edge progress all verified against code (§12.3; grid is genuinely `GridCells.Adaptive(160.dp)`). But "real database items rendered" is invalid as evidence: all four posters render the app's own empty-state screenshot, because the scanner ingests unfiltered MediaStore rows (F-01/F-02, confirmed §12.1). Chips leak raw filesystem paths (F-06); a friendly bucket name is already captured at scan time and simply unused.

## A.3 · Video Player — Portrait HUD & Scrubber
*Full gesture playback viewport with transport HUD, scrub bar, and quick toggles.*

![Player Screen Portrait HUD](captures/screenshot_player_live.png)

- **Top Bar:** Back navigation arrow, video title with edge-to-edge status scrim, aspect ratio pill (`Fit`), and playback speed badge (`1.0x`).
- **Center Transport HUD:**
  - Prominent circular play/replay action button.
  - ±10s relative seek touch targets on left and right with haptic feedback hooks.
- **Bottom Scrubber:**
  - Progress bar with cyan elapsed fill and buffered track indicator.
  - Tabular timestamps (`0:00 / 0:00`) that never jitter during scrubbing.
  - Left button: Touch Lock toggle (locks screen to prevent accidental touches).
  - Right buttons: Subtitle track sheet toggle, audio track sheet toggle, aspect ratio toggle.

**Reviewer verdict:** Control inventory matches code — sheets, lock, and the ±10s targets with haptics are real (§11.1). But the capture is an idle-like state (`0:00 / 0:00`), so "elapsed fill", "never jitter", and "buffered track indicator" are asserted, not observed (F-08); `bufferedFraction` is computed but never rendered (§11.3). The top bar actually collides with the status bar (F-03, verified `PlayerScreen.kt:406–414`) — the "edge-to-edge status scrim" is a bug wearing a feature's name until §13's fix lands. Playhead renders deformed (F-04).

## A.4 · Video Player — Cinematic Landscape
*Automatically adapts when rotating the phone to landscape mode, maximizing video surface area.*

![Player Screen Landscape HUD](captures/screenshot_player_landscape.png)

- **Wide Aspect Scrims:** Ergonomic top and bottom tapered gradients ensure 100% legibility over any video frame without cluttering the screen center.
- **Full-Width Scrubber:** Extended timeline scrubber for accurate frame-level scrubbing.

**Reviewer verdict:** Contradicted by its own capture — the video surface is a portrait-ratio poster letterboxed to ~20% of screen width, with duplicated clocks and dual gesture pills (F-05). No orientation logic exists in the codebase at all; the same portrait HUD `Column` serves both orientations (F-18, §14). "100% legibility over any video frame" is untestable from a still and should be retired as a claim.

## A.5 · UI Feedback Checklist for Review (from original doc)
1. **Color & Contrast:** Is the deep dark `#0A0E17` with cyan `#00E5FF` accent visually pleasing, or would you prefer a different accent color (e.g. amber, purple, blue)? → *Answered in Part B §7; keep cyan, with measured rationale.*
2. **Card Density:** Is the 2-column card grid clean, or would you prefer a single-column list view option? → *Answered in Part B §7; keep 2-column, defer list-view. The grid is already adaptive (§12.3).*
3. **Player HUD:** Do the center ±10s buttons and bottom timeline feel right, or should any controls be rearranged? → *Answered in Part B §7; parity matrix in Part C §11.2.*

---

# Part B — Consolidated Review (v1, captures-only pass)

## 1. Executive summary

The visual language is coherent and close to a shippable dark theme. The **product problem is not styling — it is that the captures do not show a working media player.** Every thumbnail and both player surfaces render the app's own empty-state screenshot instead of video frames, the player HUD collides with system status icons, and there is no immersion, buffering, or error state anywhere. Several claims in the source inspection doc are aspirational rather than observed.

| Area | Score (1–5) | One-line verdict |
|---|---|---|
| Visual language / theme | 4.0 | Coherent, high-contrast dark system; token-based ✅ |
| Library IA & browsing | 2.0 | Unusable for browsing while posters are polluted; raw paths leak |
| Player experience | 2.0 | HUD/status-bar collision, no immersion, no playback shown |
| State robustness | 1.0 | Loading/error/permission states not captured at all |
| Accessibility | 3.0 | Accent contrast excellent (≈12.5:1); rest untested |
| Source doc quality | 2.0 | Good format; claims outrun evidence; no defect log |

> **Part C calibration (after agent audits):** the gesture floor is higher than the captures suggested — single-tap HUD toggle, double-tap ±10s with haptics, and brightness/volume vertical-swipe zones with animated heads-up pills already exist (`PlayerScreen.kt:163–246`). The gap vs MX Player is *depth* (2×-hold, pinch-to-aspect, seek-preview), not basics.

**Bottom line:** Fix the poster pipeline and HUD insets before any aesthetic iteration. The theme is ready; the experience is not.

---

## 2. Findings index

| ID | Severity | Area | Title |
|---|---|---|---|
| F-01 | **P0** | Library | Posters render the app's empty-state screenshot, not video frames |
| F-02 | P1 | Library | False affordances baked into posters (clickable-looking Refresh pill, fake system chrome) |
| F-03 | P1 | Player | HUD collides with system status bar; playback not immersive |
| F-04 | P1 | Player | Playhead renders as a deformed clipped pill; no visible buffered/elapsed track |
| F-05 | P1 | Landscape | Video surface uses ~20% of screen; duplicated clock + dual gesture pills |
| F-06 | P1 | Library | Raw filesystem paths in chips and card metadata; chips clip without scroll affordance |
| F-07 | P1 | Onboarding | Empty-state copy contradicts itself; SCR-001 flow absent — verified in code (§12.2) |
| F-08 | P1 | Player | Capture shows no playback: `0:00 / 0:00`, replay icon on unstarted video |
| F-09 | P2 | Library | Demo data realism: near-full progress on "unwatched" videos; `.0` size formatting |
| F-10 | P2 | Library | "Search 4 videos…" count-in-placeholder |
| F-11 | P2 | Player | Replay-vs-play icon semantics; no loading/buffering affordance |
| F-12 | P2 | A11y | Chip selection relies on hue + border only |
| F-13 | P1 | Player | Resume-from-killpoint restores position silently — no "Resuming from…" affordance (`PlayerViewModel.kt:188–193`) ✅ |
| F-14 | P2 | Player | Repeat/play-mode icon is rendered non-clickable — false affordance (`PlayerScreen.kt:647–662`) ✅ |
| F-15 | P1 | Onboarding | SCR-001 onboarding + runtime permission request absent codebase-wide — verified, not merely uncaptured ✅ |
| F-16 | P2 | Library | `isRefreshing` not observed by the state combine — spinner can go stale/dropped (`MediaLibraryViewModel.kt:36–42, 79`) ✅ |
| F-17 | P2 | Hygiene | Dead `AnimatedVisibility` imports, unreachable MainScreen template screen, duplicated crossfade config, scanner comment/code mismatch ✅ |
| F-18 | P1 | Adaptive | No orientation or adaptive logic exists in the player — one HUD `Column` serves both orientations; zero `WindowInsets` / `LocalConfiguration` / `BoxWithConstraints` usage ✅ |
| F-19 | P2 | Library | No `Error` state in `MediaLibraryUiState` (Loading/Empty/Success only) — scan/DB failures render as silently empty ✅ |

Severity: **P0** = broken core experience · **P1** = visible defect / spec violation · **P2** = polish.

---

## 3. Detailed findings

### F-01 · P0 · Posters are the app's own empty-state screenshot ✅

**Evidence:** [captures/screenshot_library_live.png](captures/screenshot_library_live.png) — all four cards ("Camera REC Family Vacation", "Oppenheimer Trinity Test Scene", "Cyberpunk 2077 Night City 4K", "Interstellar IMAX Trailer 4K") show the identical miniaturized empty-state screen, complete with its embedded fake status bar ("10:51") and "Refresh Videos" pill. The same composition fills both player surfaces ([player portrait](captures/screenshot_player_live.png), [player landscape](captures/screenshot_player_landscape.png)).

**Impact:** The library is unusable for its primary job (visual browsing) and the inspection doc presents this state as "real database items rendered." Every downstream visual claim built on these captures inherits the same defect.

**Root cause analysis (⚠️ in v1 — CONFIRMED in §12.1):** The code is correct in intent — `MediaLibraryScreen.kt` builds `AsyncImage` requests with `videoFrameMillis(2000L)` to extract a frame from `card.uri`. Therefore the input data is wrong: the demo corpus URIs resolve to a screenshot PNG (or the scan ingested images as videos). Check: (a) scan-time MIME filtering on `video/*`, (b) how the demo/seed library generator set the poster, (c) whether the emulator MediaStore holds PNGs mislabeled as video.

**Fix:** Purge the demo corpus; re-verify with `tools/corpus/` fixtures (which are real, codec-diverse media files); add a scan-time guard that skips non-video MIME types with a logged warning.

---

### F-02 · P1 · False affordances inside the media area ✅

The poster contains a "Refresh Videos" button, a status bar, and a gesture-nav pill. Users will tap the pill inside a card thumbnail and nothing will happen; worse, the real UI's progress line is drawn across that fake button, making it look like a broken overlay.

**Fix (after F-01):** Posters must contain only decoded pixels of the video. Never serialize UI chrome into poster assets.

---

### F-03 · P1 · Player HUD collides with system status bar ✅

**Evidence:** [player portrait](captures/screenshot_player_live.png) — the back arrow overlaps the "10:56" clock; the network indicator (⚠ triangle) sits inside the title's first character zone; battery and Wi‑Fi icons visibly overlap the "1.0x" speed badge. The status bar and gesture-nav pill are fully visible during "playback."

**Impact:** Borderline duplicate-clock rendering and direct icon collision; also violates immersion expectations for any video player.

**Cross-check ❓:** `spec/06` screen-state requirements and standard video-player behavior (e.g., `WindowInsetsController` immersive hide) should be asserted here. The captures show neither a status-bar-aware top inset nor immersive mode.

**Fix:** Enter immersive mode on playback; pad HUD by `WindowInsets.statusBars` when HUD is visible; re-capture.

---

### F-04 · P1 · Deformed playhead; scrubber claims unsubstantiated ✅

**Evidence:** Portrait and landscape scrubbers render a tall, clipped cyan pill protruding above and below the track at position zero — no distinct elapsed fill is visible, and the claimed "buffered track indicator" is indistinguishable in both captures (uniform gray track).

**Root cause ⚠️:** Custom thumb/track composition (clipping or mis-sized thumb) rather than a `Slider`-managed geometry; the doc's "timestamps that never jitter" and "elapsed fill" claims are temporal and cannot be evidenced from a static capture.

**Fix:** Audit the scrubber composable's thumb sizing/clipping; re-capture mid-playback with buffered range > 0 and elapsed > 0 (see Appendix C).

---

### F-05 · P1 · Landscape wastes ~80% of the screen ✅

**Evidence:** [landscape](captures/screenshot_player_landscape.png) — the video area is a portrait-ratio poster centered at roughly a fifth of screen width, with black voids either side, while the HUD stretches edge-to-edge over the voids. The poster's own baked-in fake status clock ("10:51") and gesture pill duplicate the real ones — two clocks, two nav pills.

**Impact:** Directly contradicts the doc's claim "maximizing video surface area." Landscape is the mode where a video player earns its keep; this capture shows the least usable orientation.

**Fix:** After F-01 (real 16:9 posters), re-verify landscape. Decide whether "Fit" letterboxing should ever produce portrait-width video in landscape (for portrait-ratio media, consider blurred-cover paddings or justified crop toggle). Enter immersive mode here too.

---

### F-06 · P1 · Raw filesystem paths in the UI ✅

**Evidence:** Filter chips read `/storage/emulated/0/Camera` and the third chip clips mid-path (`/storage/emulated…`) with no scroll-affordance indicator; card metadata lines truncate to `/storage/emul…`.

**Impact:** Leaks kernel paths to end users, hurts scannability, and crops useful info out of cards.

**Fix:** Display friendly folder names ("Camera", "Movies"); keep the raw path in tooltips/Detekt-style detail only. Reserve card metadata line for the most useful attributes (resolution badge e.g. "4K", date added) over file size — see F-09. Add a scroll fade on the chip strip.

---

### F-07 · P1 · Empty-state copy contradictions + missing permission flow ✅ / ❓

**Evidence:** [empty state](captures/screenshot_library_empty.png) — headline "Your library is ready" on an **empty** library; body says videos "will appear here automatically," while the only CTA is a manual "Refresh Videos" button. No permission rationale, no "Continue without access" path visible on first run.

**Cross-check:** `spec/06` SCR-001 requires an onboarding & permissions screen (max 3 pages, inline permission rationale, decline path), and `AndroidManifest.xml` declares `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO`. The capture shows none of that flow.

**❓ Verify first:** whether the capture predates or postdates onboarding. If this is genuinely first-run, it is a spec violation, not a copy tweak.

**Fix (regardless):** Rename headline ("No videos yet" / "Add your library"); rename CTA to a scan action ("Scan device") with scan iconography; reconcile "automatic" claim with the manual affordance; land SCR-001 wiring.

---

### F-08 · P1 · No playback is actually shown ✅

Timestamps read `0:00 / 0:00` in both orientations, no elapsed fill, replay arrow instead of play triangle (player in IDLE/ended-like state). **Consequence: the entire "scrubber behavior" section of the doc is asserted, not observed.** Re-capture mid-playback states before trusting any player claim.

---

### F-09 · P2 · Demo data realism ✅

All four cards show near-full progress indicators despite the doc framing them as freshly scanned; the progress line overlapping the poster's fake button reinforces F-02. Sizes render as "180.0 MB" (trailing `.0` is noise). Consider resolution badge (`4K`) more than byte size for a video library.

---

### F-10 · P2 · "Search 4 videos…" ✅

A volatile count inside a placeholder invites layout shift and odd semantics; static "Search videos" is the norm, with the count shown in a header or empty-results copy.

---

### F-11 · P2 · Player state semantics ✅ / ⚠️

Replay arrow on a never-started video (play is expected for `STATE_IDLE`-with-poster); no buffering spinner anywhere. Both may be state-dependent rather than bugs — but for a media player the inspection doc should enumerate all visible states and this captures none of the transitions.

---

### F-12 · P2 · Chip selection salience ✅

Selected chip = cyan border + cyan text; unselected = plain border. Vietnamese-hue-level differentiation may starve color-blind/low-vision users: add a check-state indicator (check icon / filled treatment) so selection never rests on color alone.

---

## 4. Cross-cutting gaps for a media player (not captured anywhere)

The repo is unusually well instrumented for failure modes — `fixtures/` carries an adversarial set (missing `moov`, multi-track overloads), a full codec matrix (HEVC 10-bit, Theora, WMV, E-AC‑3…), and seek-pinned instrumented files (`inst/`). Yet **no runtime states were captured**, and the Part C code audit splits the missing list in two. *Exist in code but absent from every capture:* buffering spinner (`PlayerScreen.kt:353–362`) and playback-error banner with retry (`671–727`). *Do not exist anywhere in the codebase:* PiP, floating player, resume-from-killpoint UI (silent restore only), seek-preview thumbnail, buffered-track rendering, onboarding/permission flow.

**Recommendation:** Treat the capture gallery as a **state matrix**, not four flattering stills. Appendix C lists the minimum matrix.

---

## 5. Accessibility review

| Check | Status | Detail |
|---|---|---|
| Accent contrast `#00E5FF` on `#0A0E17` | ✅ Pass **AAA** | ≈ 12.5:1 (WCAG relative luminance method) |
| Inverse (dark text on cyan CTA) | ✅ Pass **AAA** | ≈ 12.5:1 — same token pair, symmetric result |
| Primary text on background | ✅ Pass | White ≈ 19:1 |
| Secondary copy | ⚠️ Likely pass | Looks ≈ 7:1 if token ≈ `#98A2B3`; certify against actual tokens |
| Chip selection by color alone | ⚠️ F-12 | Add non-color selection indicator |
| Touch targets | ⚠️ Plausible pass | 48dp claimed; bottom icon row spacing looks adequate, but certify with layout inspector |
| Icon-only buttons content descriptions | ❓ Unknown | Not testable from stills; TalkBack sweep required |
| Font scale 200% / large display | ❓ Untested | Especially card grid + player bottom cluster |
| RTL | ❓ Untested | Symmetric HUD (±10s) makes this a real risk |

---

## 6. Review of the source inspection doc itself

1. **Broken links (why this file was rewritten):** absolute paths into an IDE-internal session directory (`~/.gemini/antigravity-ide/brain/…`) work only on this machine for one session. Captures are now versioned under `review/captures/`; alt text should describe the image, not repeat the caption.
2. **LaTeX notation** (`$\pm 10\text{s}$`) renders only in math-enabled viewers — use `±10s`.
3. **Missing repro metadata:** build/version, API level (35), density, font scale, capture timestamps (10:51/10:56 visible and uncited).
4. **No defect log, no issues found:** at least four real defects were visible (F‑01/02/03/05) while the doc reported zero. An inspection that only confirms is worthless; observations/claims/defects/questions must be separated.
5. **Unverifiable temporal claims** ("never jitter", "100% legibility over any video frame", "automatically adapts"): a still cannot evidence behavior. Tag every bullet *verified* vs *asserted*.
6. **Terminology:** `#0A0E17` is not "OLED-black" (that is `#000000`); "adaptive 2-column grid" was never shown across widths; "haptic feedback hooks" is unfalsifiable.
7. **One false negative corrected:** the doc's "duration badges in the lower right corner" and "progress line at card bottom edge" claims **do match** the code (`BottomEnd`, bottom-edge indicator) — the *visual* problem is the polluted poster beneath them, not the layout.

---

## 7. Answers to the gallery doc's three open questions

1. **Color & contrast:** Keep the cyan accent. Rationale: ≈12.5:1 on the near-black base (AAA), tech/precision connotation fitting a player, and the dark-on-cyan CTA is symmetric. Constraint: reserve cyan for primary action/focus/progress — no large cyan fills (halation on OLED). Do not switch to amber/purple on aesthetics alone without a measurable rationale.
2. **Card density:** Keep 2‑column at phone widths; add a list view toggle later for large libraries (the `generate_scale_library.py` corpus targets scale, and 2‑column scanning degrades past ~hundreds of items). Defer — list view is a feature, not a redesign.
3. **Player HUD:** Keep the ±10s lateral placement (industry convention, muscle memory). Priorities are: (a) inset/immersive fix (F‑03), (b) real playback captures, (c) add long‑press 2× and seek-preview before rearranging anything.

---

## 8. Prioritized action plan

> *(v1 plan — superseded by the consolidated §17 plan at the end of Part C.)*

| Priority | Action | Trace |
|---|---|---|
| **P0** | Fix poster pipeline: real frame extraction verified; purge polluted demo data; scan-time MIME guard | F‑01, F‑02 · `MediaLibraryScreen.kt` L365–413 |
| **P1** | Immersive playback + status-bar insets for HUD | F‑03, F‑05 | 
| **P1** | Scrubber geometry audit (playhead clipping, buffered track) | F‑04 |
| **P1** | Friendly folder names in chips and cards; chip strip scroll affordance | F‑06 |
| **P1** | Empty-state copy + CTA reconciliation; verify/wire SCR‑001 permission flow | F‑07 |
| **P1** | Re-capture the state matrix (Appendix C) and re-issue the inspection doc with verified/asserted tags | F‑08 |
| **P2** | Demo data realism, size formatting, search placeholder, chip selection indicator, metadata (resolution > size) | F‑09, F‑10, F‑12 |
| **P2** | A11y cert pass: TalkBack sweep, contentDescriptions, 200% font scale, RTL, target audit | §5 |

---

## 9. Appendices

### A. Capture inventory

| File | Screen | Time | Notes |
|---|---|---|---|
| `captures/screenshot_library_empty.png` | Library, empty state | 10:51 | Valid capture; copy/permission findings apply |
| `captures/screenshot_library_live.png` | Library, 4-item grid | 10:56 | Posters polluted (F‑01); badge/progress layout itself matches code |
| `captures/screenshot_player_live.png` | Player, portrait | 10:56 | `0:00/0:00` idle-like state; HUD/status overlap |
| `captures/screenshot_player_landscape.png` | Player, landscape | 10:56 | Letterboxed portrait poster; duplicated chrome |

### B. Code & spec cross-references

- `app/.../ui/library/MediaLibraryScreen.kt` L349–413 — `VideoGridCard`: 16:9 poster box, `AsyncImage` `videoFrameMillis(2000L)`, badge `BottomEnd`, 3dp bottom progress indicator gated on `progressFraction > 0`.
- `app/src/main/AndroidManifest.xml` L5–6 — `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` declared (permission flow capture missing).
- `spec/06_UX_SCREEN_AND_STATE_SPECIFICATION.md` — SCR‑001 Onboarding & permissions (§ screen matrix), SCR‑014 storage housekeeping.
- `app/.../core/playback/KillPointSimulator.kt`, `core/harness/HarnessContracts.kt` — resume/thumbnail/seek instrumentation exists; none visually captured.
- `fixtures/` (a_matrix · b_set · cd_samples · adv_media · inst) — full state-matrix test beds awaiting UX captures.

### C. Minimum capture matrix for the next inspection round

1. Playing state, mid-video (elapsed > 0, buffered > 0), portrait + landscape, HUD shown **and** auto-hidden.
2. Scrubbing in progress with seek-preview thumbnail visible.
3. Permission-denied / "Continue without access" library state; onboarding pages (SCR‑001).
4. Corrupt-codec error card + player error surface (use `fixtures/b_set` / `adv_media`).
5. Audio-only file presentation (e.g., `b_B09_02_audio_only.flac`).
6. Chip strip with >5 folders to evidence scroll affordance.
7. Font scale 200%, TalkBack traversal order, RTL pseudolocales.
8. Tablet/two-pane library (to substantiate "adaptive grid").
9. Subtitle and audio track sheets with the instrumented multi-track fixtures (`inst_`).

---

# Part C — Deep-Dive (skills · subagents · web validation)

## 10. Method and source registry

| Source | Applied to |
|---|---|
| `edge-to-edge` skill (official Android playbook) | §13 — inset/immersive compliance audit |
| `adaptive` skill (official Android playbook) | §14 — orientation/tablet audit |
| `jetpack-compose-performance` skill | §15 — perf-as-UX audit |
| Explore subagent A — player UI audit | §11 (files, lines, absence lists) |
| Explore subagent B — library & data pipeline audit | §12 (scanner root cause, SCR-001, theme truth) |
| Web validation | §16 — MX Player Pro v1.93.4 (Aug 2025) listing, Accessible Android gesture review, Beebom "Best Android video players 2026" |
| Independent verification (this reviewer) | `PlayerScreen.kt:395–509`, `MediaStoreScanner.kt:28–97`, `build.gradle.kts:16`, `AndroidManifest.xml:24` — read directly before writing claims |

Every new claim below carries a file:line anchor. ✅ = verified directly. ⚠️ = inferred. ❓ = needs runtime/benchmark check.

---

## 11. Player deep dive (subagent audit, self-verified)

### 11.1 Verified structural facts

- Player UI is **one 906-line composable** (`PlayerScreen.kt`) plus state/VM. No separate landscape variant, no `HUD` decomposition — every control lives in one `Box` tree.
- Aspect handling is delegated to ExoPlayer's `AspectRatioFrameLayout.RESIZE_MODE_*` (`PlayerScreen.kt:151–159`); the enum has 5 modes (`Fit/Fill/Zoom/16:9/4:3`, `PlayerUiState.kt:9–14`) — good parity for crop modes.
- Sheets exist and are real UI: Speed (0.25×–2.0×, L729–776), Audio track (778–832), Subtitles (834–904) — all `ModalBottomSheet` with check-mark selection.
- Haptics: five sites, all `HapticFeedbackType.TextHandleMove` (L175, 218, 497, 514, 537, 585). Single gesture vocabulary; no `LongPress`, no scrub tick.

### 11.2 Gesture parity vs MX Player (matrix)

| Gesture | MX Player Pro | Aptura today | Gap |
|---|---|---|---|
| Tap → HUD toggle | ✅ | ✅ (`:172,215`) | — |
| Double-tap ±10s + haptic | ✅ | ✅ (`:173–178, 216–221`) | — |
| Vertical swipe left/right → brightness/volume + heads-up pill | ✅ | ✅ (`:181–204, 224–244`, `AnimatedVisibility` pills `:250–254, 283–287`) | Pill is text-only — no progress bar inside |
| Long-press → 2× while held | ✅ | ❌ — zero `onLongPress` in codebase | **Add** |
| Pinch → zoom/pan (custom aspect) | ✅ | ❌ — zero `detectTransformGestures` | **Add** (map to existing 5-mode enum or free zoom) |
| Horizontal drag on surface → scrub | ✅ | ❌ — slider-only scrub | Add |
| Two-finger vertical → speed | ✅ | ❌ | Defer |
| Subtitle-based seeking / subtitle reposition gestures | ✅ | ❌ | Defer (spec-owned) |

**Verdict:** the "gesture foundation exists" correction is the single most important calibration of this review — v1 assumptions about missing basics were wrong; the gap is depth and polish.

### 11.3 State coverage (present vs absent)

Present ✅: buffering spinner (L353–362, gated `isBuffering && !isPlaying`), error banner with Go Back/Retry (L671–727), decoder-init-failure recoverability branch (`Media3PlaybackController.kt:76`), lock mode with full gesture gating (`key(uiState.isLocked)` pointerInput).

Absent ❌ (codebase-wide greps = 0 matches): PiP (`supportsPictureInPicture`, `enterPictureInPictureMode`), floating/popup player, immersive mode (`WindowInsetsController.hide`), any `WindowInsets` padding, seek-preview thumbnail (thumbnails exist only inside `KillPointSimulator.kt`), buffered-track render (`bufferedFraction` computed in `PlayerUiState.kt:77–80`, never referenced by the UI), resume-from-killpoint UI (silent 2s pre-roll restore at `PlayerViewModel.kt:188–193`).

**New findings logged:** F-13 (silent resume), F-14 (play-mode icon display-only — dead control), F-18 (no orientation logic at all: same HUD `Column` in both orientations; zero `LocalConfiguration`/`BoxWithConstraints`/`screenOrientation` matches).

### 11.4 Motion inventory

Whole-player motion = four `AnimatedVisibility` fades/scales (L250–254, 283–287, 316–319, 395–399). Zero `animate*AsState`, zero `Crossfade`, zero `tween/spring`, zero `MotionLayout`. Consequences visible in captures: play/pause icon swaps hard, buffering spinner pops, aspect changes are instant. This is the mechanical root of the "boring" feel — motion was never a styling problem, it is an implementation gap.

---

## 12. Library & data deep dive (subagent audit, self-verified)

### 12.1 F-01 root cause — CONFIRMED ✅

`MediaStoreScanner.kt:49–55`:

```kotlin
49:  val cursor: Cursor? = contentResolver.query(
50:      uri, PROJECTION,   // MediaStore.Video.Media.EXTERNAL_CONTENT_URI
51:      null, null, sortOrder)   // selection = NULL — no MIME filter
```

The only guard is `if (size > 1024L)` (L86). The method's own KDoc (L41) promises a duration filter that **does not exist**. So any row in the video volume — mislabeled PNGs, junk fragments — becomes a "video" card, and `VideoFrameDecoder` then fails or renders garbage posters (the polluted captures).

**Provenance refinement:** no app-side seeding exists (greps for seed/demo/generators = 0 in `app/src/main`; the four capture titles appear only in the review doc and Robolectric fixtures). The polluted quartet was emulator-side ad-hoc media from the capture session. **So F-01 = polluted capture corpus × unprotected scanner.** The systemic fix is the scanner guard, which protects every future device:

```kotlin
val selection = "${MediaStore.Video.Media.DURATION} > 0 " +
    "AND ${MediaStore.Video.Media.SIZE} > 1024 " +
    "AND ${MediaStore.Video.Media.MIME_TYPE} LIKE 'video/%'"
```

Also reconcile: the projection already captures `BUCKET_DISPLAY_NAME` (L35) and the scanner stores it as `folder` (L81) — a friendly name is *already available at scan time*, yet chips/cards render the raw `folderPath` column (`MediaItemDao.kt:31–32`). The F-06 fix may be a column-selection change, not a feature.

### 12.2 SCR-001 onboarding — verified absent ✅

- Permissions declared (`AndroidManifest.xml:5–6`) but **never requested**: zero `rememberLauncherForActivityResult` / `requestPermission` matches in `app/src/main`.
- Navigation root is `MediaLibrary` directly (`Navigation.kt:25, 43–59`) — no onboarding destination; `strings.xml` has no onboarding strings.
- Spec journey (`spec/06` L181): `SCR-001 → OS dialog → SCR-011 scan → SCR-002 / denied → SCR-012 limited mode` — none of it exists; `MediaScanWorker` never checks grant state (`MediaScanWorker.kt:26–76`).
- UX consequence: without permission the scanner returns empty and the UI shows the **positive** "Your library is ready" — indistinguishable from an actually empty device. This is the concrete definition of "not UX friendly" for first-run.
- (F-07 upgraded from ❓-not-evidenced to ✅-absent; F-19 = no `Error` state branch in `MediaLibraryUiState`.)

### 12.3 Corrections and refinements vs v1

- **"Adaptive 2-column grid" claim — partially vindicated:** the grid is `GridCells.Adaptive(minSize = 160.dp)` (`MediaLibraryScreen.kt:328`) — genuinely adaptive at cell level. What's missing is form-factor *verification* (no `WindowSizeClass`, no screenshot tests — see §14) and stable keys: items are keyed `"${index}_${item.uri}"` (L338), which is position-fragile on sort/search changes.
- **Demo-data theory corrected:** v1's "demo corpus generator" framing was wrong — there is no in-app seeder (§12.1).
- **Theme truth:** actual `SurfaceBase` = `#0D0E11` (`Color.kt:12`, matches `DESIGN.md:25`); `#0A0E17` exists nowhere in code — the original gallery doc's "OLED-black `#0A0E17`" was doubly wrong (wrong value *and* wrong name). Contrast re-check on the real token: cyan `#00E5FF` vs `#0D0E11` ≈ 12.3:1 — still AAA.
- **Typography:** every style is `FontFamily.Default` (Roboto) — zero brand typography; `labelSmall` does carry the `tnum` feature (confirmed `Type.kt:46–54`).
- **Theme is hard-locked dark** (`ApturaTheme` ignores its `darkTheme` parameter, `Theme.kt:44`) — acceptable for a v1 media app, but blocks future dynamic-color theming if not revisited.

### 12.4 Additional findings

- **F-16:** the library combine reads `_isRefreshing` once (L36–42, 79); mutating it (L101–107) does not re-trigger the combine — the refresh spinner can silently never appear. Logic bug, not a styling issue.
- **F-17 hygiene:** dead `AnimatedVisibility/fadeIn/fadeOut` imports (L3–5), unreachable `MainScreen.kt` "Hello Android" template screen, duplicated crossfade config (global loader `ApturaApplication.kt:20` + per-request L377), scanner comment/code mismatch.
- **Grid empty-state text:** "Scanning local videos..." (L549) is fine; the 2s fake spinner on Refresh (L252–257) is a stop-gap that should be replaced by real scan-state feedback once F-16 lands.

---

## 13. Edge-to-edge compliance audit (skill-driven)

Prerequisites: targetSdk 35 ✅ (`build.gradle.kts:16`); `enableEdgeToEdge()` ✅ (`MainActivity.kt:17`); `adjustResize` ✅ (`AndroidManifest.xml:24`). The floor is set correctly — and then **nothing else happens**:

| Skill checklist item | Status |
|---|---|
| `enableEdgeToEdge()` called | ✅ |
| `adjustResize` in manifest | ✅ |
| Insets applied anywhere (`windowInsetsPadding` / `statusBarsPadding` / `safeDrawing` / `WindowInsets` API) | ❌ **zero matches in `app/src/main`** |
| Status-bar-safe HUD / scrim with inset-aware content | ❌ — top bar uses raw `padding(16,12)` (`PlayerScreen.kt:406–414`), hence the F-03 collision |
| Bottom content above nav bar | ❌ — bottom cluster raw `padding(16,8)` (L552–560) |
| System-bar hide/show for immersive playback | ❌ |
| `isNavigationBarContrastEnforced = false` (SDK 29+) | ❓ verify |
| Icon appearance flags | ✅ correct for dark theme (`Theme.kt:51–53`) |

**Prescribed fix pattern (per skill, minimal diff):**

```kotlin
// Top HUD — scrim must still paint behind the status bar:
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(Brush.verticalGradient(listOf(ApturaColors.SurfaceScrim, Color.Transparent)))
        .windowInsetsPadding(WindowInsets.statusBars)   // padding AFTER background → full-bleed scrim
        .padding(horizontal = 16.dp, vertical = 12.dp)
)

// Bottom cluster: same with WindowInsets.navigationBars (or imePadding-adjacent per skill rules).
// Player: on play (and on HUD-hide), WindowInsetsControllerCompat.hide(statusBars() | navigationBars());
//         on tap-reveal, show(). One inset method only — do not double-pad via Scaffold + modifiers.
```

The gallery doc's "edge-to-edge status scrim" claim becomes *true* exactly when this lands.

---

## 14. Adaptive & orientation audit (skill-driven)

Skill prerequisites: Compose-only ✅; **Navigation 3 ✅** (uses `rememberNavBackStack`/`NavDisplay`).

- **Step 1 (verify current UI per form factor) — missing.** No `@Preview` matrix for foldable/tablet/desktop; no screenshot tests of form factors. Per skill, add a `FormFactorPreviews` annotation across library/player screens first.
- **Step 2/3 (adaptive nav, multi-pane) — correctly deferred.** Player is full-bleed media: the skill itself says avoid list-detail when the detail is media that benefits from full screen. Phone-first is right; a two-pane library is a later SCR decision, not a defect.
- **Step 4 (adaptive columns) — already done** via `GridCells.Adaptive(160.dp)` — but validate at 800dp+ boundaries.
- **Player orientation — the actual gap (F-18):** no orientation detection at all. Landscape is the same portrait `Column`; capture F-05's letterboxed portrait poster is `RESIZE_MODE_FIT` applied to a polluted portrait asset, *plus* no design answer for portrait-ratio media in landscape. Decide: blurred-cover padding or justified-crop toggle for portrait media, and a landscape HUD variant (controls to sides, taller scrubber below) once posters are real.

---

## 15. Compose performance audit (perf-as-UX)

On a 1,086-fixture scale corpus, scroll jank *is* the "boring/cheap" feeling. Skill-rule scan of the grid path:

| Rule (skill category) | Status | Location | Action |
|---|---|---|---|
| Mandatory primitive `key` in lazy layouts | ⚠️ Weak | `MediaLibraryScreen.kt:338` `"${index}_${item.uri}"` | Key by `item.uri` alone; index-keying breaks identity on sort/search |
| Mandatory `contentType` in lazy layouts | ❌ | grid `itemsIndexed` (L336–344) | Add `contentType` so slots reuse while scrolling |
| `CompositionLocal` reads inside lazy items | ⚠️ | `LocalContext.current` inside `VideoGridCard` (L372) | Resolve once at screen level, pass down |
| Poster decode sizing (4K frames → 160dp cell) | ❓ | Coil `VideoFrameDecoder`, `videoFrameMillis(2000L)` (L376) | Verify decoder honors target size; benchmark with `spec/13` harness on the scale corpus; add Baseline Profiles (skill prereq — likely absent) |
| Duplicate work | ✅ minor | crossfade configured twice (global loader + per-request) | Drop one (F-17) |
| Phase-deferral / derived state | ✅ | `effectivePositionMs` is a plain getter (`PlayerUiState.kt:69–70`) | Consider throttling timecode emission to ~10Hz instead of per-position-update |
| Side effects in items | ✅ none found | — | — |

**Grade: no perf bomb, two mandatory-rule violations (key/contentType), one benchmark question.** Fix the two rules before any "polish" work; they're one-line changes.

---

## 16. Competitive baseline (web-validated) — and how to exceed it

MX Player Pro (v1.93.4, Aug 2025) remains the gesture/knowledge benchmark: double-tap ±10s, swipe volume/brightness, long-press 2×, zoom/pan, **customizable floating window + PiP**, subtitle gestures (seek-by-subtitle, repositioning), online subtitle search, Kids Lock, HW+ decoding, equalizer, SMB/NAS. The 2026 market (Beebom): VLC (compatibility), MX (gestures/performance), **Next Player ("Material You design, modern clean")**, Nova (scraper/TV), Just (HDR10+/AFR), MPV (purists).

**Positioning read:** the *modern-polish* niche is already owned (Next Player). MX owns power-user depth but is dated, ad-bloated in free tier, and bloat-carried since the Amazon acquisition. **Aptura's winning slot = MX's control depth + Next/Infuse-tier polish + a smart library neither has.** Concretely:

| Move | Why it exceeds |
|---|---|
| Cinematic scrub: floating frame preview + big tabular timecode while dragging | Highest-recall "wow"; your killpoint/harness engineering already builds half of it |
| Resume UX that speaks (F-13): "Resuming from 4:12" + auto-hide, killpoint-powered | MX resumes silently too; spoken resume reads *considerate* |
| Dynamic poster palette (Material You-style tinting from video frames) | MX/VLC can't; instantly makes the library feel alive (also fixes "boring") |
| Floating popup player + PiP (modernized: rounded, chat-friendly) | MX's signature stickiness; currently 0% implemented |
| Gesture depth completion: long-press 2×, pinch-to-aspect (map to the existing 5-mode enum), surface-drag scrub | Closes the parity matrix from §11.2 |
| Motion system: springs on HUD, icon transitions, `animateItem()` on sort | Removes the "settings page" feel; cheapest perceived-quality lever |

Scope guard from your own specs: Kids Lock / equalizer / SMB / music-player are *feature-creep traps* until `spec/03` personas demand them — parity on gestures, not on catalog breadth, is the goal.

---

## 17. Consolidated action plan (supersedes §8)

**Phase 0 — correctness (days)**
1. Scanner guard: selection + duration + MIME filter; fix KDoc (F-01, F-17) · `MediaStoreScanner.kt`
2. Stable grid keys (`item.uri`) + `contentType` (§15) · `MediaLibraryScreen.kt`
3. `isRefreshing` emission fix (F-16); replace 2s fake spinner with real scan state · `MediaLibraryViewModel`
4. Remove dead code: imports, `MainScreen`, duplicate crossfade (F-17)

**Phase 1 — correctness with visible payoff (week 1–2)**
5. Insets + immersive per §13 pattern (F-03, F-18) → re-capture: HUD no longer collides
6. Real posters from `tools/corpus` fixtures; re-capture gallery (F-01/F-02) → library suddenly looks like a product
7. SCR-001: 3 onboarding pages + runtime request + denied/limited state + worker gating + `Error` UI state (F-15, F-19) + empty-state copy fix (F-07)
8. Friendly folder names from the already-captured bucket name (F-06)

**Phase 2 — parity & feel (weeks 3–4)**
9. Long-press 2×, pinch-to-aspect, surface-drag scrub, buffered secondary track render (§11.2, F-04)
10. Resume toast + auto-hide polish (F-13), play-mode control made live or removed (F-14)
11. Motion pass: springs for HUD, icon transitions, `animateItem()` (§11.4)
12. A11y cert: TalkBack sweep, 200% font scale, chip selection non-color indicator (F-12)

**Phase 3 — signature layer (roadmap, needs ADRs vs `spec/08`)**
13. Cinematic scrub preview; dynamic poster palette; floating popup + PiP; Continue-Watching hero rail; list-view toggle for scale libraries

**Definition of "exceeding MX":** Phase 0–1 = *not embarrassing*, Phase 2 = *competitive*, Phase 3 = *distinctive*. Each phase is independently shippable.


---

# Part D — Competitive Feature Gap Analysis & Roadmap: "Transforming Aptura Into the Honest Player"

> **Scope:** product/function layer — features, gaps, use cases, prioritization, roadmap. Visual defects are cited via `F-nn` from Parts B–C above. The sequencing below is authoritative for everything Part C §17 deferred to the product level.

**Aptura's problem is not feature poverty — it is unfinished wiring plus an absent management layer, both squarely inside its own deliberately-chosen R1 boundary.** The player UI calmly shows controls that do nothing: track-selection sheets choose tracks that are never applied, a manifest-declared `MediaSessionService` is never connected so there is no media notification or lockscreen controls, and every tap on a video converts a multi-item list into a 1-item queue that makes skip-to-next, shuffle and play-modes inert. Behind that sits a larger hole: half of the spec's own R1 MUSTs — onboarding/permission flow, per-video actions, media details, settings persistence — have no code at all. The market is simultaneously opening: Amazon has stripped MX Player's ad-free Pro tier and folded the free tier into an ad-funded OTT funnel ([56](https://www.reddit.com/r/Android/comments/1gf1psl/amazon_takes_over_mx_player_app_takes_down_pro/), [1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [43](https://ppc.land/amazon-kills-mx-player-brand-folding-it-into-prime-video-for-india-ad-reach/)), the competitive cohort that matches Aptura's privacy values (Next Player, Just Player, VLC) all ship material feature sets Aptura has not begun, and users systematically punish exactly the behaviors (ads, data overreach, permission bloat) that R1 was designed to avoid. The strategic answer is a three-step ladder: **fix the broken promises first (P0) → complete the 14-item 2026 table-stakes list (P1) → own the "honest player" position with whitespace features no researched competitor ships (P2)** — with every feature that crosses an R1 non-goal (network playback, TV, online subtitles, equalizer, monetization) explicitly fenced behind ⚠ ADR flags in a Later tier rather than the main roadmap. Appendix B dispositions every researched feature; Section 10 sequences the work in dev-weeks with acceptance anchors in Aptura's own spec.

**Citation key:** `[n]` refers to the numbered source register in Appendix A (all URLs deduplicated from the three market-research notes). `file:line` anchors refer to the repo state audited on 2026-10-21 (post commit `ee726cc`). **⚠ Requires ADR** marks recommendations crossing an owner-confirmed R1 non-goal (code_gap_brief §6); these are routed to the Later tier only. Companion-review references use the `F-nn` findings IDs defined in Parts B–C of this dossier.

---

## 1. Document control

| Field | Value |
|---|---|
| **Date** | 2026-10-21 |
| **Product** | `org.aptura.player` ("Aptura") — local-first Android video player; Compose + Media3/ExoPlayer 1.5.1, Room, WorkManager, Coil (+ custom `VideoFrameDecoder`), Navigation 3; 37 Kotlin files, 2 screens |
| **Scope of this report** | **Product/function layer only**: features, functional gaps, use cases, prioritization, roadmap. Visual/UI defects live in Parts A–C and are referenced (not duplicated) via `F-01…F-19` |
| **Parts A–C** | Above in this dossier (§1–17) — F-nn findings, gesture parity matrix vs MX Player (Part C §11.2), edge-to-edge (§13), adaptive (§14), perf (§15) audits, phased action plan §17 |
| **Method** | 3 code-audit agents (player; library & data pipeline; platform/engine) · 3 market researchers (competitor feature matrix; user expectations/pain points; UX patterns & platform opportunities) · deep-research synthesis over four notes in `research_notes/Aptura media player competitive roadmap/` |
| **Confidence — code claims** | High. All anchors below were verified by the code-audit agents or directly by the reviewer in Parts A–C; every claim keeps its `file:line` anchor |
| **Confidence — market claims** | Medium. Store listings and Reddit/Play review text are volatile; 👍 counts reflect the Oct 2026 fetch (user_expectations note, Q2 gaps). Items the researchers could not verify are marked *(unverified)* inline |
| **Confidence — demand signals** | Medium-low. GitHub thumbs-up counts are a proxy, not survey data; no quantitative retention/market-share data surfaced in any note |

Known evidence blind spots carried through from the notes: KMPlayer Play-listing detail and Mi Video's full feature list were not fetched; VLC's current online-subtitle status could not be confirmed (wiki fetch 404) *(unverified)*; MX/VLC sleep-timer and screenshot presence in listings found no evidence either way; no Accessible Android reviews exist for VLC/Next/Just — their TalkBack quality is unknown; the embedded-photo-picker blog URL in the notes is truncated *(unverified)* [91](https://android-developers.googleblog.com/2026/01/).

---

## 2. Executive summary

**The verdict, in three sentences.** Aptura today is a polished visual shell wrapped around a player that systematically breaks promises its own UI makes — four P0 "dead wiring" defects (track selection that never calls `player.trackSelectionParameters`, a never-connected `MediaSessionService`, a 1-item queue that kills skip/shuffle/play-modes, and subtitle/audio-delay + sleep-timer state with no UI) mean that what users *can see* mostly does not work, and what users *cannot see* (onboarding, per-video actions, details, settings root, the entire subtitle machinery) is simply absent code. Because these gaps are mirrored almost one-for-one by the spec's own planned-but-unbuilt backlog (16 items) and by the 2026 table-stakes list (14 items, of which 12 are in-boundary), the fastest path to competitiveness is not new invention but sequencing: wire what exists, then build the spec's own backlog as the table-stakes lane. The winning end-state is a defensible niche nobody occupies today — a **privacy-verifiable, management-complete local library player with decoder honesty and a real subtitle flagship** — in a market whose leader (MX Player) just retreated from the paid-ad-free segment entirely.

### Top 10 gaps

| # | Gap | Why it matters | Tier |
|---|---|---|---|
| 1 | **Track selection is a UI stub** — `selectAudioTrack`/`selectSubtitleTrack` mutate StateFlows but never call `player.trackSelectionParameters`; sheets always show "Default Audio Track" | A media player whose track menus do nothing fails its first test; users read it as "broken" not "missing" (R1 table-stakes item 3) | **P0** |
| 2 | **`MediaSessionService` declared but never connected** — two independent ExoPlayers; no media notification, no lockscreen/Bluetooth/headset controls | Background/controls are the single highest-demand feature in the two most comparable FOSS trackers (19👍, shipped in Just; ×3 duplicates shipped in Next) [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc), [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc) | **P0** |
| 3 | **Every playback is a 1-item queue** (`Navigation.kt:47–49`) — `playQueue`/`skipToNext`/`skipToPrevious` unreachable; play-mode chip cosmetic | Kills auto-advance, Up-next, shuffle, repeat — Spec FR-CTRL-017/SCR-010f/g are designed and unimplemented | **P0** |
| 4 | **Subtitle/audio delay + sleep timer are state placeholders with no UI** (`setSubtitleDelay`/`setAudioDelay`; `sleepTimerRemainingMs`) | Advanced-sheet items (SCR-010e) are spec-owned and cheap; sync control is the entry point of Aptura's flagship lane | **P0** |
| 5 | **No onboarding/permission flow; denied access renders as a positive empty state** ("Your library is ready", zero permission requests codebase-wide) | First-run is the highest-churn moment; without grant the app silently shows "ready" — F-07/F-15 ✅; SCR-001 unimplemented | **P1** |
| 6 | **No per-video menu or file-level actions; FR-LIB-020 (MUST·R1, "open with"/share handoff) entirely unimplemented** | "Delete/rename/share/details" is table-stakes item 11 [9](https://github.com/anilbeesetti/nextplayer), [2](https://play.google.com/store/apps/details?id=org.videolan.vlc); its absence makes the library a dead-end viewer | **P1** |
| 7 | **No settings root and nothing persists but position** — no decoder override, no per-video memory, no background-play policy, no aspect defaults | Settings is where "honest player" claims must live (decoder labels, compat notices — FR-PLAY-002/003, SCR-010h) | **P1** |
| 8 | **No subtitle machinery beyond a stub sheet** — sidecar discovery/ranking, offset/calibration, styling, encoding/mojibake handling (FR-SUB-001…009), libass fidelity lane gated by RQ-06 | Subtitles are Aptura's chosen #1 differentiator while ASS/SSA rendering remains broken in mainstream Android stacks (Jellyfin, Wholphin) [62](https://www.reddit.com/r/jellyfin/comments/v7v053/the_android_app_integrated_players_ass_subtitle/), [63](https://github.com/damontecres/Wholphin/issues/1049) | **P1** |
| 9 | **Decoder honesty absent** — no HW/SW/auto override, no fallback explanation, no "why it fell back" labels (FR-PLAY-002/003) | Next Player proves the design works [10](https://github.com/anilbeesetti/nextplayer/blob/main/feature/player/DECODER_SWITCHING.md); honesty labels are the marquee of the "honest player" strategy | **P1** |
| 10 | **No PiP and no background-audio policy** (FR-PLAT-001, FR-PLAY-018/FR-AUD-006) while the platform hardens FGS rules | PiP shipped in Next (7👍) and is a delighter across archetypes [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc), [28](https://firecore.com/releases); skipping it concedes a sticky surface | **P2** |

### Strategy in one paragraph

Aptura should not chase feature-for-feature parity with the ad-funded super-apps — that race runs toward network playback, downloader bloat and AI tools, all of which are R1 non-goals and strategically wrong for a privacy-first product. Its ladder is: **(1) fix broken promises** — the four P0 wiring defects convert visible UI from lies into function and are days-to-weeks of work; **(2) complete table stakes** — the spec's own 16-item backlog is, item for item, the 2026 table-stakes list minus the [OUT] distribution surfaces (TV, casting, network sources), so building it is both honesty and competitiveness; **(3) own the honest-player position** — verifiable zero-INTERNET proof as marketing artifact, decoder/fallback honesty labels, whitespace features no researched player ships (folder counts, hidden-folder exclusion lists, duplicate detection, manual+heuristic intro/outro skip), and a subtitle flagship (management UX + sync wizard + honest encoding handling now; libass fidelity behind the RQ-06 gate). In a market where the free leader is an ads-and-funnel OTT brand, the paid-ad-free shelf is empty, and the FOSS cohort defers library management to other apps, "the player that respects you *and* manages your files" is an unoccupied quadrant — and every one of its hard problems is already specified in Aptura's own docs.

## 3. Where Aptura stands today

The code audit resolves into three layers: a modest **implemented surface** (the real basics exist and look good in captures), a **dead-wiring layer** of code that exists but was never integrated (the most dangerous band, because users see working-looking controls), and a wide band of **absent features** that are either deliberate R1 posture or spec-planned-but-unbuilt. Parts A–C add the visual layer: the theme is coherent and AAA-contrast, but captures showed posters polluted by an unguarded scanner (F-01 ✅) and HUD/status-bar collision (F-03 ✅) — `MediaStoreScanner.kt:49–55` runs an unfiltered MediaStore query (no MIME or duration guard) whose KDoc promises a duration filter that does not exist (`MediaStoreScanner.kt:41`).

### 3.1 Implemented surface (what users actually get)

- **Library:** adaptive grid `GridCells.Adaptive(160.dp)` (`MediaLibraryScreen.kt:328`), in-memory `title OR folder contains` search while the DAO's indexed `searchVideos` sits unused, two sort orders (DATE_ADDED_DESC, TITLE_ASC), folder filter chips, full-rescan-on-refresh via WorkManager, empty/loading states. All copy hardcoded English — `strings.xml` contains exactly one string (`app_name`).
- **Player (~950-line composable):** HUD (back/title/aspect pill/speed badge), transport cluster (replay/play/±10s + haptics), double-tap ±10s, left-half vertical drag = brightness, right-half = `AudioManager` volume, controls lock, 5s HUD auto-hide with system-bar hide/show (`PlayerScreen.kt:163–246, 250–287`), buffering spinner (`:353–362`), error banner with Go Back/Retry (`:671–727`), play-mode chip (`:647–662` — purely cosmetic), speed sheet with 8 fixed presets (`:729–776`), audio-track sheet (`:778–832`), subtitle-track sheet (`:834–904`). Aspect handled via ExoPlayer `RESIZE_MODE_*` with 5 modes (`PlayerScreen.kt:151–159`, `PlayerUiState.kt:9–14`).
- **Persistence:** Room `media_items` — position/completion/`lastPlayedTimestamp` only; resume >3s & not completed restores with 2s pre-roll (5s checkpoints) but **silently** (`PlayerViewModel.kt:188–193`, F-13 ✅).
- **Verified trust boundary:** zero `INTERNET`, no `MANAGE_EXTERNAL_STORAGE`, no `POST_NOTIFICATIONS`, no telemetry SDK, no bundled video/AAC decode. This boundary is real, verifiable, and the single strongest asset in the research.

### 3.2 Dead-wiring debt (code exists, effect is nil) — the "UI shows features that do nothing" cluster

| # | Dead wiring | Anchor / evidence | User-visible symptom |
|---|---|---|---|
| D-1 | **Track selection never applied to the player** | `selectAudioTrack`/`selectSubtitleTrack` mutate StateFlows; **zero** `trackSelectionParameters` calls | Sheets always show "Default Audio Track"; choosing a track does nothing |
| D-2 | **`ApturaPlaybackService` (manifest-registered `MediaSessionService`) never started/bound** | UI plays on separate app-scoped `PlayerManager` ExoPlayer; two independent ExoPlayer instances | No media notification, no lockscreen/Bluetooth/headset controls; background death |
| D-3 | **Every playback is a 1-item queue** | `onVideoClick` drops the passed list (`Navigation.kt:47–49`) | `playQueue`/`skipToNext`/`skipToPrevious` unreachable; shuffle meaningless; play-modes inert |
| D-4 | **Play-mode chip cosmetic** | `PlayerScreen.kt:647–662` — cycles UI state only (F-14 ✅) | Repeat/shuffle icons lie |
| D-5 | **Delay + sleep timer are ornamental state** | `setSubtitleDelay`/`setAudioDelay` exist with no UI; `sleepTimerRemainingMs` never used | Spec SCR-010e caps stay unshipped |
| D-6 | **Built-but-unwired machinery** | `DAO.searchVideos`, `getAllVideosSortedByTitle`, `schedulePeriodicScan` (defined, never called), DataStore (declared, zero usage), `DiagnosticBundleExporter` (built for SCR-015, unwired), `isCompleted` computed but never rendered | Indexed search, help/diagnostics UI, watch-state badges all one integration step away from existing |

### 3.3 Absent features — deliberate posture vs. self-imposed debt

Postures that are **deliberate** (honor them; do not silently re-open): no network playback of any kind, no TV/car/wearables, no DRM, no cloud/accounts, no AI (DEC-007, R3 only), no online subtitle download, no equalizer/audio effects, no video editing, no torrents, no ads/monetization at launch, F-Droid/GPL channel not pursued at R1. Absences that are **self-imposed debt** (the app should ship them and its own spec says so — code_gap_brief §5 lists 16 items):

- Settings root (SCR-013/014) + storage/diagnostics/help/about (SCR-015/016) — nothing persists except position.
- **Per-video three-dot menu / any file operation (delete/rename/share/move/properties) — none**; spec posture: R1 non-destructive, file rename/move/delete = MAY·R2 ⚠; but *delete-from-library*, *details*, *share handoff*, *open-with/ACTION_VIEW* are achievable now, and **FR-LIB-020 (open-with/share; MUST·R1) is entirely unimplemented**.
- Onboarding/permission education + limited mode (SCR-001/SCR-012; F-15 ✅ absent codebase-wide — `MediaScanWorker.kt:26–76` never checks grant state).
- Media-details view incl. delete-from-library (SCR-009, FR-LIB-013). Multi-select. Folder browser (SCR-004/005). Playlists (SCR-006/007). Watch-history UI (timestamp stored, never queried). Favorites (FR-LIB-016).
- Scan-progress surface (SCR-011) + limited-mode banner (SCR-012). Advanced sheet (SCR-010e: decoder override, stats overlay, A-B repeat, sleep timer). Compatibility notice (SCR-010h). Up-next/ended + auto-advance countdown + 2h guard (SCR-010f/g, FR-CTRL-017). Destructive-action confirmations (SCR-018). Diagnostics/help UI (SCR-015/016).
- Full subtitle machinery (FR-SUB-001…009): sidecar discovery/ranking/extra folder, offset + calibration aid, styling, encoding/mojibake fix, per-video memory.
- PiP via AndroidX VideoPlaybackPictureInPicture (FR-PLAT-001); MediaSession ↔ active player unification (FR-PLAT-002/003); error taxonomy + redacted diagnostics (FR-PLAY-004, FR-DIAG-001–004); FFmpeg LGPL audio extension production + wiring (after legal resolution of the `aac` contradiction — see §11); open-with/share intent filters (FR-LIB-020); targetSdk 35→36 recheck (spec S7); `READ_MEDIA_AUDIO` beyond the spec's video-only posture (justify or drop).
- Engine-side gaps vs the spec's own codec-honesty matrix (doc 07): no HW/SW/auto decoder override (FR-PLAY-003), no fallback ladder or "why it fell back" labels, no refresh-rate alignment (FR-PLAY-012), no HDR/DV policy (FR-PLAY-013), no corrupt-file best-effort guarantees (FR-PLAY-009), no `SeekParameters` keyframe tolerance (FR-PLAY-005), no buffer tuning, no chapters (MKV), no bookmarks, no A-B loop, no frame-step, no screenshot, no wake-lock audit, no `configChanges` handling (rotation recreates the Activity; the player survives only via the app singleton).

**Spec-internal white space (absent from all Aptura specs and absent from every researched competitor — see §4.4):** USB-OTG/flash-drive playback, home-screen widgets, kids/parental controls, screenshot gesture, light/theme engine, Wi-Fi device-to-device transfer (⚠ network), lyrics/album-art views.

## 4. Competitive landscape

The category has split into three archetypes, and the players closest to Aptura's values are the ones exposing its functional debts most clearly. Archetype (a) is the **ad-funded super-app**: MX/Amazon, PLAYit, KMPlayer, Mi Video — players that bolt OTT streaming, downloaders, AI tools and game halls onto the player and monetize the funnel ([1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [3](https://play.google.com/store/apps/details?id=com.playit.videoplayer), [33](https://www.playit.app), [30](https://www.kmplayer.com/home), [7](https://play.google.com/store/apps/details?id=com.miui.videoplayer&hl=en_US)). Archetype (b) is the **clean FOSS cohort**: VLC, Next Player, Just Player, Nova, mpv-android/mpvKt — differentiation through trust, engine depth and honesty-of-engineering; inside this cohort sits a second split: "player with a real managed library" (VLC, Nova) vs "thin player that delegates library duty to another app" (Just Player explicitly says it will *never* try to compete with feature-rich VLC and points library users at Nova [13](https://github.com/moneytoo/Player)). Archetype (c) is the **OEM stock player, retreating**: Samsung removed its native video player from Galaxy S25 US ([45](https://us.community.samsung.com/t5/Galaxy-S25/Disappointed-the-Samsung-video-player-app-was-removed/td-p/3251786)) and folded video editing into Gallery ([46](https://tech.yahoo.com/phones/articles/hidden-galaxy-s25-feature-makes-164505954.html)); Mi Video is a downloader/streamer [7](https://play.google.com/store/apps/details?id=com.miui.videoplayer&hl=en_US)).

| Archetype | Members | Size / model | Signature strengths | Signature weaknesses |
|---|---|---|---|---|
| Ad-funded super-app | MX Player (1B+ installs, Amazon, AVOD + MX Fatafat OTT [1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [41](https://www.hollywoodreporter.com/business/business-news/apos-amazon-streaming-india-prime-video-and-mx-player-1236300136/), [42](https://www.thearcweb.com/article/amazon-merges-mx-player-minitv-for-adbased-streaming-C4eTpcoe4iCfcz7f)); PLAYit (500M+, VIP/coins [3](https://play.google.com/store/apps/details?id=com.playit.videoplayer)); KMPlayer (magnet streaming, GIF tools [30](https://www.kmplayer.com/home), [31](https://kmplayer.en.uptodown.com/android)); Mi Video (player + downloader [7](https://play.google.com/store/apps/details?id=com.miui.videoplayer&hl=en_US)) | Ads + IAP; data collection at scale (PLAYit: 23 permissions, 14 data types, 71/100 privacy [49](https://gameappchecker.com/app/com.playit.videoplayer)) | Codec breadth, floating window, exotic extras | Ad saturation (~7 breaks/40 min reported [1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [52](https://apps.apple.com/lk/app/mx-player-all-in-one-player/id1594318832?see-all=reviews&platform=iphone)); trust deficit; bloat drift |
| Clean FOSS | VLC (500M+, libVLC, no data collected [2](https://play.google.com/store/apps/details?id=org.videolan.vlc)); **Next Player** (Media3+FFmpeg ext, GPL-3 [9](https://github.com/anilbeesetti/nextplayer)); **Just Player** (Media3+FFmpeg ext, donations [13](https://github.com/moneytoo/Player)); Nova (1M+, scraper/TV [4](https://play.google.com/store/apps/details?id=org.courville.nova), [15](https://github.com/nova-video-player/aos-AVP)); mpv-android / mpvKt (engine purists [20](https://github.com/mpv-android/mpv-android), [21](https://github.com/abdallahmehiz/mpvKt)) | Free/donations; "no ads / no tracking / no excessive permissions" as headline [9](https://github.com/anilbeesetti/nextplayer) | Engine depth, trust, Material 3 modernity (Next) | UX drift (VLC "prehistoric" [34](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care)); library duty outsourced (Just [13](https://github.com/moneytoo/Player)); small teams starving (mpv) [24](https://github.com/mpv-player/mpv/discussions/14257) |
| OEM stock | Samsung (removed), Xiaomi Mi Video | Gallery-integrated | Preinstalled reach | Retreating ([45](https://us.community.samsung.com/t5/Galaxy-S25/Disappointed-the-Samsung-video-player-app-was-removed/td-p/3251786)); downloader/aggressive-ad posture (Xiaomi) |
| (Absent quadrant) | **"Privacy-clean player with a real managed library"** — Infuse would occupy it on polish but **has no Android version at all** ([27](https://firecore.com/infuse), [51](https://www.cinaura.tv/infuse-for-android-tv); a lookalike app squats the name on Play [8](https://play.google.com/store/apps/details?id=com.infuse.infuse&hl=en_US)) | — | — | **Thinly occupied — Aptura's implied slot** [13](https://github.com/moneytoo/Player) |

### 4.1 Table stakes for a 2026 local video player (14 items; evidence per feature in the notes)

| # | Capability | Evidenced in ≥4 of | In Aptura's R1 boundary? | Aptura status |
|---|---|---|---|---|
| 1 | All-format/HW-accelerated playback incl. 4K | MX [1], VLC "all codecs included" [2], Nova [4], PLAYit [3], KM [5](https://play.google.com/store/apps/details?id=com.kmplayer&hl=en_US), Just [13], mpv [20] | ✅ [IN] | Partial — platform decoders only; FFmpeg audio ext unwired (G-17); "plays everything" is a **never-claim** by strategy |
| 2 | Folder-based browsing/library | VLC [2], Next (tree/folder/file) [9], Nova [4], PLAYit [3], MX [1] | ✅ | ❌ Folder filter chips only (G-05) |
| 3 | Audio + subtitle track selection | VLC [2], Next [9], Just [13], Nova [4], mpv [20] | ✅ | ❌ **Dead wiring** (G-11) |
| 4 | External subtitle file loading | MX (11 formats) [1], Next [9], Just [13], Nova [4] | ✅ | ❌ Not implemented beyond embedded tracks (G-18) |
| 5 | HW↔SW decoder override | MX [1], Next [10], Just [13], Nova [4], PLAYit [3], mpv [20] | ✅ | ❌ (G-15) |
| 6 | Gestures: brightness/volume/seek/pinch-zoom | MX [1], VLC [2], Next [9], Just [13], PLAYit [3], mpv [20] | ✅ | 🟡 Basics done (`PlayerScreen.kt:163–246`); depth missing (long-press 2×, pinch-pan, surface-drag scrub — companion §11.2) |
| 7 | Playback speed control | MX [1], Next [9], Just [13], PLAYit [3], KM [5], mpv [20] | ✅ | 🟡 8 presets, no pitch note, no per-video memory |
| 8 | Aspect/resize control (fit/crop/zoom) | VLC [2], MX [1], Just [13], Next [9] | ✅ | 🟡 5-mode enum exists; no pinch-pan |
| 9 | PiP or floating window | MX [1], VLC [2], Next [9], Just [13], PLAYit [3], mpv [20] | ✅ | ❌ (G-20) |
| 10 | SD-card/external-storage browsing | MX [1], VLC [2], Nova [4], PLAYit [3], Next/Just via SAF [9][13] | ✅ | ❌ (G-09) |
| 11 | Delete/share/manage files from within the app | VLC (multi-file delete re-added Oct 2026) [2], Just (post-playback delete) [13], PLAYit [3], MX [1] | ✅ (library-level; destructive file ops = R2 ⚠) | ❌ **FR-LIB-020 unimplemented** (G-02) |
| 12 | Background playback (video→audio or background video) | MX [1], VLC [2], Next [9], PLAYit [3], mpv [20] (Just declines [13]) | ✅ | ❌ Dead wiring (G-12) |
| 13 | Android TV / leanback build | VLC [2], Next [9], Nova [4], Just [13], MX (India TV) [1], mpv (sideload) [20] | ❌ **[OUT]** — R1 non-goal | N/A (demand hedge: 13👍 [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |
| 14 | Network stream/source support (SMB/FTP/WebDAV/UPnP/URL) | VLC [2], Next [9], Nova [4], MX [1], KM [31], mpv (URL) [20], PLAYit [3] | ❌ **[OUT]** — R1 non-goal | N/A (14👍 URL-streaming request in Just [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |

Items 1–12 are the floor a 2026 local-first player must hit; Next Player's README reads as a checklist of exactly these, and the community rates it accordingly — "basically a lite open source version of MX Player Pro" [64](https://www.reddit.com/r/androidapps/comments/17resjv/the_best_free_android_video_player_at_the_moment). Equalizer and online-subtitle download are *common but not universal* and both sit **[OUT]** of R1 — VLC's Oct 2026 changelog full of equalizer fixes shows it is a maintenance liability, not a moat [2](https://play.google.com/store/apps/details?id=org.videolan.vlc).

### 4.2 Rare differentiators (1–2 apps each) and what is dated

| Differentiator | Who has it | Boundary read | Aptura relevance |
|---|---|---|---|
| Intro/outro auto-skip | **Nova only** [16](https://github.com/nova-video-player/aos-AVP/releases) | [IN] | Rare; a v1-safe variant (manual segments + frame-difference heuristic, no AI) is true whitespace (§9) |
| Seamless refresh-rate matching + tunneled playback + DV profile 7 | **Just only** [13](https://github.com/moneytoo/Player) | [IN] | FR-PLAY-012 kill-switch already spec'd; differentiator-grade |
| mpv-class rendering (scalers, debanding, interpolation; dual subtitles) | mpv-android [20](https://github.com/mpv-android/mpv-android) | [IN] but engine-war bait | **No mpv-class swap needed** (DEC-003); cherry-pick *dual subtitles* into the libass lane |
| NFO/poster scraping, Trakt, multi-device network resume | Nova [4](https://play.google.com/store/apps/details?id=org.courville.nova), [16](https://github.com/nova-video-player/aos-AVP/releases) | Scraping/Trakt [OUT]; local NFO/seasons [IN] | Later, low priority (RSK-009 scope firewall) |
| 3D SBS/TB; AC3/DTS passthrough (3 modes) | Nova [4](https://play.google.com/store/apps/details?id=org.courville.nova), [19](https://home.courville.org/nova_video_player-faq/index.html) | [IN] (TV-oriented) | Defer |
| Skip silence; pause on headphone disconnect; touch lock; post-playback actions | Just [13](https://github.com/moneytoo/Player) | [IN] | Adopt; amplitude-heuristic skip-silence is R1-safe [75](https://www.reddit.com/r/androidapps/comments/11r125z/any_offline_video_player_with_skip_silence_option/), [76](https://github.com/vantezzen/skip-silence) |
| Private mode suppressing history | Nova [4](https://play.google.com/store/apps/details?id=org.courville.nova) | [IN] | Candidate P3 (privacy-brand affinity) |
| Volume boost | Just ("volume boost") [13](https://github.com/moneytoo/Player); Nova ("Audio Boost") [4](https://play.google.com/store/apps/details?id=org.courville.nova); mpv users openly complain it is missing [24](https://github.com/mpv-player/mpv/discussions/14257) | [IN] | Candidate in the audio settings family |
| Remote web UI (OTP), teletext/CC | VLC [2](https://play.google.com/store/apps/details?id=org.videolan.vlc) | Remote web = [OUT] (network) | Reject for R1 |
| Device-to-device transfer; game hall; AI effects; MP4→MP3 | PLAYit [3](https://play.google.com/store/apps/details?id=com.playit.videoplayer) | [OUT] | Reject |
| Torrent/magnet streaming; URL/YouTube download; cutter/MP3 converter; MX Share (camera+location) | KM [30](https://www.kmplayer.com/home), [32](https://kmplayerofficial.blogspot.com/2025/) / MX [1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | [OUT] | Reject (torrents also explicit non-goal) |
| **Dated/declining:** one-time paid Pro tiers being withdrawn; downloader-era bloat; OEM retreat; legacy format-list marketing | MX Pro off Play ([55](https://xdaforums.com/t/mx-player-pro-no-longer-on-google-play.4692396), [56](https://www.reddit.com/r/Android/comments/1gf1psl/amazon_takes_over_mx_player_app_takes_down_pro/)); PLAYit coins/VIP [3](https://play.google.com/store/apps/details?id=com.playit.videoplayer); Samsung ([45](https://us.community.samsung.com/t5/Galaxy-S25/Disappointed-the-Samsung-video-player-app-was-removed/td-p/3251786)); Beebom still ranking MoboPlayer-era apps [36](https://beebom.com/best-android-video-player-apps/) | — | Market-timing signal: 2026 roundups converge on VLC/MX/Nova/Next [37](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm), [38](https://ottmega.com/blog/best-android-media-player-2026), [39](https://www.youtube.com/watch?v=rBEhABqzQi0) and treat "no ads, no tracking, no excessive permissions" as a headline feature itself [9](https://github.com/anilbeesetti/nextplayer), [12](https://github.com/moneytoo/Player) |

### 4.3 Criticisms of the leaders (the attack surface)

MX Player: ~7 ad breaks per 40-minute episode totaling 20+ minutes (Sept 2026 review, 69 helpful votes), Amazon-login churn, mic + Bluetooth + MX Share camera/location permission requests, data-safety declaring Location + Personal info + 4 types ([1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [44](https://www.forbes.com/sites/swetakaushal/2025/02/12/how-amazon-mx-player-plans-to-use-ai-for-avod-growth/)). PLAYit: "bombarded with ads… recommend looking for an alternative" (11,423 helpful votes), pay-to-de-ads VIP escalation, "Data can't be deleted" ([3](https://play.google.com/store/apps/details?id=com.playit.videoplayer), [49](https://gameappchecker.com/app/com.playit.videoplayer), [50](https://playit.en.softonic.com/android)). VLC: "starting to look prehistoric… a total contrast to the Material You philosophy" [34](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care); "one of the worst settings dialogs of any mainstream app" [58](https://www.reddit.com/r/VLC/comments/1x6h1y1/new_ui_redesign_what_do_you_think); library rescanning its "one fatal flaw… sometimes up to 2 minutes" ([2](https://play.google.com/store/apps/details?id=org.videolan.vlc) reviews); slow-motion crashes [60](https://www.trustpilot.com/review/www.vlc-media-player.org); Android 16 background-playback cut-outs [61](https://www.reddit.com/r/VLC/comments/1utfwkc/finally_given_up_on_this_app_and_uninstalled_it); invasive all-files permission criticized by GrapheneOS [47](https://discuss.grapheneos.org/d/2714-is-vlc-player-good-for-security-and-privacy). Nova: TV experience lags tablet [68](https://www.reddit.com/r/AndroidTV/comments/1kb9qil/nova_video_player_is_the_best_for_android_tv/); SMB performance and Dolby Vision decoding called blocking ([17](https://github.com/nova-video-player/aos-AVP/issues/1651)); files missed after volume remount [18](https://github.com/nova-video-player/aos-AVP/issues/1454). mpv-android: "dire need of developers", missing volume boost and external-subtitle ergonomics [24](https://github.com/mpv-player/mpv/discussions/14257), no A-B repeat UI [25](https://github.com/mpv-player/mpv/issues/7331).

### 4.4 Whitespace inside the R1 boundary — absent from *every* researched player

The matrix pass found no source for any reviewed player documenting **folder counts in the library IA**, **hidden-folder exclusion lists**, or **duplicate detection** ([9](https://github.com/anilbeesetti/nextplayer), [2](https://play.google.com/store/apps/details?id=org.videolan.vlc), [1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [4](https://play.google.com/store/apps/details?id=org.courville.nova), [13](https://github.com/moneytoo/Player), [3](https://play.google.com/store/apps/details?id=com.playit.videoplayer), [20](https://github.com/mpv-android/mpv-android), [7](https://play.google.com/store/apps/details?id=com.miui.videoplayer&hl=en_US)) — these are genuine whitespace *inside* the boundary. Also unoccupied within R1: USB-OTG/flash-drive playback and home-screen widgets appear in no Aptura spec and in none of the researched players' marketed feature sets (§3.3). On the *user-value* side, several of the known delighters are largely absent from the ad-funded leaders (skip silence [25](https://github.com/mpv-player/mpv/issues/7331), sub-sync wizards, watch-state labels, decoder honesty) — see §5.4.

## 5. What users actually want

### 5.1 Complaints that define the opening

Each category leader owns a signature complaint, and all three are the mirror image of Aptura's R1 posture. **MX Player = ad saturation plus loss of the paid ad-free tier**: iOS users call it "absolutely unusable due to the insane amount of ads" ([52](https://apps.apple.com/lk/app/mx-player-all-in-one-player/id1594318832?see-all=reviews&platform=iphone), [53](https://www.mouthshut.com/product-reviews/mx-player-reviews-925903202-page-6)), full-screen ads have shaped sentiment since ~2017 [54](https://www.reddit.com/r/androidapps/comments/5pm0ap/discussion_whats_upwent_wrong_with_mx_player), the company maintains an "escalate inappropriate ads" support flow [57](https://support.mxplayer.in/support/solutions/articles/43000660733-how-to-report-escalate-inappropriate-or-explicit-ads-), and MX Player Pro was pulled from Play in Oct 2024 — buyers are pushed to sideload from mx.j2inter.com ([55](https://xdaforums.com/t/mx-player-pro-no-longer-on-google-play.4692396), [56](https://www.reddit.com/r/Android/comments/1gf1psl/amazon_takes_over_mx_player_app_takes_down_pro/)). On TV the brand confusion is total: "Do not confuse it with MX Player TV… a streaming service for films and shows in India, not a player for your own files" [37](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm). **PLAYit = permission/data overreach**: 23 permissions, 14 collected data types, 71/100 privacy score "use with caution" [49](https://gameappchecker.com/app/com.playit.videoplayer), BLUETOOTH_SCAN/WRITE_SETTINGS among sensitive flags in a Softonic scan [50](https://playit.en.softonic.com/android). **VLC = UX drift and library scan pain**: "prehistoric" [34](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care), "worst settings dialogs" [58](https://www.reddit.com/r/VLC/comments/1x6h1y1/new_ui_redesign_what_do_you_think), "clunky and unintuitive… biggest problem is the performance" [59](https://www.reddit.com/r/fossdroid/comments/1exnfvw/whats_a_good_video_player_option_vlc_isnt_that), and the famous rescan complaint — switching video→music→video re-scans "sometimes up to 2 minutes," its "one fatal flaw" ([2](https://play.google.com/store/apps/details?id=org.videolan.vlc) reviews).

The migration path is documented: users exit MX for VLC/Next/mpvKt, with the trigger being **ads first, bloat second** [66](https://www.reddit.com/r/androidapps/comments/1lbmx1q/best_media_player_best_alternative_of_mx_player) — and the pitch language for alternatives is "clean, simple UI with zero ads." Android Police recommends players by single killer need (Just for Bluetooth latency [67](https://xdaforums.com/t/app-5-0-just-video-player-no-bluetooth-lag-exoplayer-ffmpeg-audio-codecs.4189183), Nova for TV-show library sorting, mpvKt for a cleaner mpv [35](https://www.androidpolice.com/i-love-these-obscure-video-players-on-android)) — proof that a single sharp axis is enough to win a segment.

### 5.2 Demand hierarchy from GitHub reactions (counts as of Oct 2026 fetch)

The two most comparable FOSS trackers give a demand hierarchy — note that duplicate requests (background audio ×3 in Next Player) and wontfix'd high-reaction issues are the strongest signals a new entrant can inherit:

| Feature | Evidence & 👍 | Status | Boundary |
|---|---|---|---|
| **Background audio playback + lockscreen/session controls** | Just #44 **19👍** (shipped Oct 2024); Next #251/#359/#681 (8/8/5👍, all shipped Dec 2024) [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc), [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc) | Shipped in both peers | ✅ [IN] — G-12 |
| **Dismissable/reduced UI while paused** | Just #195 **16👍** — **refused as wontfix** | Unmet demand in peer | ✅ [IN] — adoptable polish |
| **URL/stream playback** | Just #645 **14👍** | Refused (offline-only app) | ❌ [OUT] — network non-goal ⚠ |
| **Android TV build** | Next #322 **13👍** + #808 8👍 | Unshipped | ❌ [OUT] — R1 non-goal; roadmap hedge §11 |
| **Long-press → 2× speed while held** | Just #568 **9👍** (shipped) | Shipped | ✅ [IN] — gesture depth (companion §11.2) |
| **Local audio files as sources** | Just #55 **9👍** — wontfix | Unmet | 🟡 Video-first product; reject for R1 |
| **Skip silence** | Next #517 **8👍** (shipped Jul 2024) | Shipped | ✅ [IN] — amplitude heuristic [76](https://github.com/vantezzen/skip-silence) |
| **PiP** | Next #250 **7👍** (shipped Feb 2024) | Shipped | ✅ [IN] — G-20 |
| **Subtitle polish** (outline thickness 6👍, custom fonts/placement 5👍, vertical position, disable-subs-permanently 5👍, sync 4👍 shipped) | Both trackers [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc), [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc) | Partial | ✅ [IN] — flagship lane G-18 |
| **Right-second-tier utilities** | video-info display 5👍, panorama zoom 5👍, buffer config 5👍, HDR indicator 5👍, resume button 4👍, already-seen indicator 4👍, "NEW" label 4👍, chapters, sleep timer, advanced zoom 4👍 [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc), [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc) | Mixed | ✅ [IN] mostly — feed §7–§10 |

### 5.3 Table stakes — users treat them as given and only speak when broken

Gestures, resume, format coverage, thumbnails and track handling surface in reviews only when they fail: Android Police calls VLC's scheme "nearly perfect" [34](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care); MX Player Pro markets the identical set [69](https://accessibleandroid.com/app/mx-player-pro/); format coverage is *why* players exist — VLC's software decode "opens files your stock player refuses to touch" [34](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care), and MX Player Custom Codec 3.1.0 remains the #1 hottest download on free-codecs, evidence that DTS/AC3/HEVC gaps are endemic and users hunt codec packs [37](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm). External-subtitle handling is so basic that Just Player documents a TV workaround (open video in a file manager, return, then open the subtitle file via return-safety behavior) [13](https://github.com/moneytoo/Player). Resume appears as an explicit request only when missing (Next #687, 4👍 [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)). **Library-scan speed is a table stake VLC fails publicly — a fast, quiet scan is a cheap win** [2](https://play.google.com/store/apps/details?id=org.videolan.vlc).

### 5.4 Delighters (the word-of-mouth tier)

Community-singled delighters, all in-boundary or cheaply adjacent: **PiP/floating window** (MX Pro markets it as a headline [69](https://accessibleandroid.com/app/mx-player-pro/); Next shipped #250), **skip silence** (Next #517 shipped; extension-based equivalents spread by word of mouth [75](https://www.reddit.com/r/androidapps/comments/11r125z/any_offline_video_player_with_skip_silence_option/), [76](https://github.com/vantezzen/skip-silence)), **long-press 2×** with haptics (shipped), **background audio + lockscreen controls** (highest-reaction in both repos), **subtitle sync wizard** (Just shipped audio/subtitle sync #6; Next still tracks delay as #693 [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)), **sleep timer** (#1243), chapters, **watch-state labels** ("already watched"/"NEW" #822/#1141), **HDR badge** (#506). Delighters cluster in two families — *"effortless viewing"* (skip silence, 2× hold, resume, background) and *"polish that shows"* (PiP, HDR badge, watch-state labels, subtitle sync) — both aligned with Media3's strengths and a Material 3 dark theme, i.e., aligned with capabilities Aptura already has.

### 5.5 Permissions, first-run and platform friction (Android 13–16)

Android 13 replaced READ_EXTERNAL_STORAGE with the granular media permissions [93](https://developer.datalogic.com/mobile-computers/news/a13_new_permissions_for_file_access), [94](https://stackoverflow.com/questions/72948052/android-13-read-external-storage-permission-still-usable); partial media access exists as `READ_MEDIA_VISUAL_USER_SELECTED` and PLAYit declares it — meaning users routinely hit "select which photos/videos to allow" [50](https://playit.en.softonic.com/android), [89](https://developer.android.com/about/versions/14/changes/partial-photo-video-access). Google's stated direction is *"Permissionless is the future of storage"* — the photo picker requires no media permission, but its backport **excludes Android Go and non-GMS devices**, i.e., exactly the low-end emerging-market segment where the friction returns [92](https://medium.com/androiddevelopers/permissionless-is-the-future-of-storage-on-android-3fbceeb3d70a). Poorly implemented scoped storage causes "slow media queries and UI freezes," and `MANAGE_EXTERNAL_STORAGE` is effectively unavailable to players — Google is "strict about approvals" [48](https://itnext.io/navigating-the-challenges-of-scoped-storage-lessons-for-modern-android-apps-b5fd8318a02c). Android 16 **blocks programmatic orientation switching**; Just Player's README tells users to fix it manually via system Settings (aspect ratio → "App default") — a first-run papercut users must solve themselves [13](https://github.com/moneytoo/Player), [87](https://developer.android.com/about/versions/16/behavior-changes-16). Play policy is simultaneously pushing the photo picker over broad media permissions [90](https://support.google.com/googleplay/android-developer/answer/16935362?hl=en).

**Read-through for Aptura:** a zero-INTERNET + READ_MEDIA_VIDEO-only posture is provably unusual (PLAYit requests 23 permissions) and lands on the right side of Google's stated direction — but the *first-run education* SCR-001 demands is missing (F-15 ✅), so the narrowest-permission app in the category currently has the worst first-run explanation of it.

### 5.6 Accessibility — a low bar that must be beaten *deliberately*

Accessible Android's MX Player Pro review (v1.74.7, Android 14) finds "overall accessibility is good, although quite a few unlabeled buttons, especially on the player screen," plus TalkBack/Jieshuo workarounds users must learn (two-finger swipes for seek/volume/brightness, passthrough gestures for speed, "element monitoring" to auto-read subtitles) [69](https://accessibleandroid.com/app/mx-player-pro/). No major local player is publicly praised as fully accessible [70](https://accessibleandroid.com/app); TalkBack's Magic Tap toggles playback system-wide [71](https://afb.org/aw/22/5/17556). Concrete wins available to Aptura: labeled player controls, speed control reachable without gesture passthrough, subtitle auto-read support, non-color chip selection (companion F-12). The bar is low, beating it is cheap, and it is a trust-brand behavior.

## 6. Gap analysis

Each gap carries: **Gap ID · description · evidence (code line or [n] URL) · spec linkage (FR/SCR) · priority tier · effort** (S = ≤1 dev-week, M = 2–3 dev-weeks, L = 4–6 dev-weeks; units align with the spec's estimate model in doc 16). Tiers: **P0** dead wiring/broken promises · **P1** table stakes + spec-mandated · **P2** differentiators · **P3** later tier (⚠ Requires ADR where an R1 non-goal is crossed).

### 6.A Library & file management

#### G-01 · No onboarding, permission education, or denied-access state — first run misleads · P1 · S
Without `READ_MEDIA_VIDEO` the scanner simply returns empty and the library shows the *positive* copy "Your library is ready" — indistinguishable from an actually empty device; zero permission requests exist codebase-wide, and `MediaScanWorker.kt:26–76` never checks grant state. **Evidence:** F-07/F-15 ✅ (companion §12.2); `AndroidManifest.xml:5–6` declares permissions never requested; `Navigation.kt:25,43–59` has no onboarding destination. **Spec linkage:** SCR-001 (onboarding, ≤3 pages, inline rationale, decline path), SCR-012 (limited mode), FR-LIB-001/002/003. This is the highest-churn moment in the app lifecycle and the single worst "underwhelming" experience today.

#### G-02 · No per-video actions; FR-LIB-020 (MUST·R1) unimplemented · P1 · M
The library is a dead-end viewer: there is no context menu anywhere, no delete-from-library, no details, no share, no "open with". FR-LIB-020 — ACTION_VIEW/share-sheet playback with intent filters — is a spec MUST·R1 with zero implementation. Community expectation is table-stakes item 11 (VLC re-added multi-file delete in Oct 2026 [2](https://play.google.com/store/apps/details?id=org.videolan.vlc); Just ships post-playback delete [13](https://github.com/moneytoo/Player); PLAYit markets "manage & share" [3](https://play.google.com/store/apps/details?id=com.playit.videoplayer)). Spec posture honored: destructive **file** delete/rename/move = MAY·R2 ⚠; **library-level** remove, details, share-handoff, open-with are achievable now. **Evidence:** code_gap_brief §3; talk-track in §6.B for the concrete menu. **Spec linkage:** FR-LIB-020, SCR-009, SCR-018 (confirmations).

#### G-03 · No media-details view · P1 · S
Resolution, size, path, duration, codecs, date, position — all stored or trivially obtained, none exposed. Users equate "player with a details screen" with a serious tool; Just Player's video-info request carried 5👍 [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc). **Evidence:** code_gap_brief §3 (SCR-009 absent); `isCompleted` computed but never rendered (`PlayerUiState.kt:80`, code_gap_brief §2.6). **Spec linkage:** SCR-009, FR-LIB-013.

#### G-04 · No folder browser and no multi-select · P1 · M
Power users expect tree/folder/file browsing (Next Player's model [9](https://github.com/anilbeesetti/nextplayer)) and multi-select actions; folder filter chips alone do not survive a 10k-item library. **Evidence:** SCR-004/005 unimplemented (code_gap_brief §3). **Spec linkage:** SCR-004/005; FR-LIB-* family.

#### G-05 · No scan progress surface or limited-mode banner · P1 · S
Full rescans run invisibly on refresh; a denied/partial state shows the same empty grid as success. VLC's public rescan failure shows scan-speed *communication* is user-visible table stakes, not plumbing ([2](https://play.google.com/store/apps/details?id=org.videolan.vlc)). **Evidence:** SCR-011/012 absent (code_gap_brief §3). **Spec linkage:** SCR-011, SCR-012.

#### G-06 · No settings root; nothing persists but playback position · P1 · M–L
No global defaults (speed, aspect, decoder policy, background-play), no storage housekeeping UI, no help/about; DataStore declared with zero usage (code_gap_brief §2.6). Every "honest player" artifact (decoder labels, compat notices, resume policy) has to live here. **Evidence:** SCR-013/014 absent; no per-video settings columns in `media_items`. **Spec linkage:** SCR-013/014, FR-SET-*.

#### G-07 · No library intelligence: history, favorites, playlists, watch-state · P2 · M
Timestamps/`isCompleted` are stored but never surfaced (code_gap_brief §2.6); no Continue/Recently-added rails, no "watched" badges (Next demanded them: #687, #822 "NEW" — both requested repeatedly [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)). **Spec linkage:** SCR-006/007, FR-LIB-016.

#### G-08 · Whitespace trio absent everywhere: folder counts, hidden-folder exclusion list, duplicate detection · P2 · M–L
No researched player documents any of these ([9](https://github.com/anilbeesetti/nextplayer), [2](https://play.google.com/store/apps/details?id=org.videolan.vlc), [1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [4](https://play.google.com/store/apps/details?id=org.courville.nova), [3](https://play.google.com/store/apps/details?id=com.playit.videoplayer), [13](https://github.com/moneytoo/Player)) — competitor-function AND user-request whitespace inside the boundary. Concretely: per-folder video counts in chips/cards (IA aid), a per-folder "hide from library" exclusion list (Race/Cam folders, demo junk, private material), and on-demand duplicate detection (same duration+resolution+size heuristics). **Spec linkage:** none — propose new FRs (§9/§10).

#### G-09 · No OTG/USB drives or secondary volumes · P2 · M
Foundational for offline habits and SD-card fleets; peers cover SD via SAF ([9](https://github.com/anilbeesetti/nextplayer), [13](https://github.com/moneytoo/Player)) and lists promote it ([1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [2](https://play.google.com/store/apps/details?id=org.videolan.vlc), [4](https://play.google.com/store/apps/details?id=org.courville.nova), [3](https://play.google.com/store/apps/details?id=com.playit.videoplayer)). Honest-ordering note: the R1 spec's engine posture okays SAF/volume reuse; needs a scan-arch decision (G-05 interplay). **Spec linkage:** white space list (code_gap_brief §7); new FR proposal.

#### G-10 · Search & IA are superficial · P2 · S–M
In-memory `contains` filter while the DAO's indexed `searchVideos` is unused (code_gap_brief §2.6); no history/recents search, no token splitting; grid keys position-fragile (`MediaLibraryScreen.kt:338`, §15 perf audit). Cheap wins with visible payoff at scale. **Spec linkage:** FR-LIB search family; SCR-003 sorting.

### 6.B Player controls & settings

#### G-11 · **P0** — Track selection is a UI stub · P0 · S
`selectAudioTrack`/`selectSubtitleTrack` mutate StateFlows and never call `player.trackSelectionParameters` → sheets always show "Default Audio Track"; choosing does nothing. **Evidence:** code_gap_brief §2.1; sheets at `PlayerScreen.kt:778–832/834–904`. **Spec linkage:** FR-AUD-001 (spec explicitly planned: "Track-selection application to the real Player"). This is the top user-facing lie in the product today.

#### G-12 · **P0** — MediaSessionService never connected · P0 · M
`ApturaPlaybackService` is manifest-registered but never started/bound; the UI plays on a separate app-scoped ExoPlayer — two independent players, no media notification, no lockscreen/Bluetooth/headset controls, no background survival. **Evidence:** code_gap_brief §2.2. **Spec linkage:** FR-PLAT-002/003 (unification), FR-PLAY-018/FR-AUD-006 (background-continuation policy). Market weight: the single highest-reacted request in both peer trackers [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc), [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc).

#### G-13 · **P0** — 1-item queue kills up-next/shuffle/play-modes · P0 · M
`onVideoClick` drops the passed list (`Navigation.kt:47–49`) → 1-item queue → `playQueue`/`skipToNext`/`skipToPrevious` unreachable; shuffle meaningless; play-mode chip cosmetic (D-3/D-4). The spec already designed the fix: queue orchestration, `ENDED` as a decision point, 10s cancellable auto-advance countdown, 2h inactivity guard, foreground-only advance. **Evidence:** code_gap_brief §2.3–2.4; spec FR-CTRL-017, SCR-010f/g. 

#### G-14 · **P0** — Delay and sleep-timer placeholders with no UI · P0 · S
`setSubtitleDelay`/`setAudioDelay` exist; `sleepTimerRemainingMs` is a placeholder (code_gap_brief §2.5). SCR-010e already specifies the delivery vehicle (advanced sheet: decoder override, stats overlay, A-B repeat, sleep timer). **Spec linkage:** SCR-010e; FR-AUD (delay family); FR-CTRL-016 (sleep timer in the adopted YouTube ladders).

#### G-15 · No decoder override or fallback honesty · P1 · M
No HW/SW/auto override (FR-PLAY-003), no fallback ladder, no "why it fell back" compatibility messaging (SCR-010h), no per-item decoder memory. Next Player proves the UX (runtime switching documented [10](https://github.com/anilbeesetti/nextplayer/blob/main/feature/player/DECODER_SWITCHING.md)); MX/Nova/Just all ship the setting ([1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [4](https://play.google.com/store/apps/details?id=org.courville.nova), [13](https://github.com/moneytoo/Player)). Labels must be honest about what "Software" means *today* — the platform decoder, not bundled FFmpeg (that relationship is ADR-010-gated, §11). **Spec linkage:** FR-PLAY-002/003/004, SCR-010e (override control), SCR-010h (compat notice).

#### G-16 · No dedicated audio & video settings IA · P1 · M
The player has no organized place for: working audio-track selection, audio delay, night mode (dynamic volume — Nova/MX ship it [1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [4](https://play.google.com/store/apps/details?id=org.courville.nova)), volume boost (Just/Nova [13](https://github.com/moneytoo/Player), [4](https://play.google.com/store/apps/details?id=org.courville.nova)), headphone-disconnect pause and routing notes (Just [13](https://github.com/moneytoo/Player)), background-play policy, aspect/zoom with pinch-pan, rotation modes, refresh-rate matching switch (FR-PLAY-012), brightness handling and per-video memory. **Spec linkage:** FR-AUD-001…008 · FR-PLAY-003/012 · FR-CTRL-008/011/012/016 · SCR-010e.

#### G-17 · FFmpeg audio extension unwired; LGPL/aac contradiction must be resolved · P1 · M (gate: legal)
The FFmpeg audio extension is not wired at all: `media3-exoplayer-ffmpeg` gradle dep missing, no `jniLibs` `.so`, no renderer registration → platform decoders only, while the build script + LGPL symbol-verification test exist (RQ-02 ✅). ⚖️ The script enables `aac` decode, contradicting ADR-010/doc-07 §10 (no bundled AAC via LA question). **Evidence:** code_gap_brief §4. **Spec linkage:** ADR-010, RSK-002/L1, FR-AUD-007. Users hunting codec packs is endemic [37](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm) — a legal, in-strategy path to DTS/AC3 decode matters, but only after counsel clears aac.

#### G-18 · Subtitle machinery absent — the flagship lane is empty · P1 · L
Everything upstream of "embedded track selection" is missing: sidecar discovery/ranking/naming conventions/extra-folder scan, per-video selection memory, offset + calibration, styling, encoding detection + mojibake fix. ASS/SSA rendering remains broken or half-broken in mainstream Android stacks in 2026 (Jellyfin: "just completely broken"; Wholphin Mar 2026: "completely invisible") [62](https://www.reddit.com/r/jellyfin/comments/v7v053/the_android_app_integrated_players_ass_subtitle/), [63](https://github.com/damontecres/Wholphin/issues/1049); ExoPlayer's own limitation is documented [26](https://github.com/google/ExoPlayer/issues/8435); mpv/libass is the fidelity path (dual subs, styled rendering [20](https://github.com/mpv-android/mpv-android), [23](https://mpv.io/manual/master/)), **gated by RQ-06** with ADR-003 (Media3-native now, libass-gated). Meanwhile R1's no-online-subs boundary is *defensible*: Next Player still lists online subtitles as only-planned [9](https://github.com/anilbeesetti/nextplayer) — Aptura's subtitle flagship lane is therefore deliberately **sidecar management + sync wizard + styling + honest encoding handling**, not online search. **Spec linkage:** FR-SUB-001…009; RQ-06/ADR-003; RSK-008.

#### G-19 · Advanced playback tools absent (scrub preview, chapters, A-B, frame-step, screenshot, stats overlay) · P2 · M
No in-HUD seek-preview thumbnails (Media3 1.9 `FrameExtractor` makes it R1-safe without playback [81](https://android-developers.googleblog.com/2025/12/media3-190-whats-new.html), [82](https://developer.android.com/media/media3/inspector); Bitmovin documents the paid-player standard [104](https://developer.bitmovin.com/playback/docs/thumbnail-preview-support)); no chapter markers/MKV chapters; no A-B repeat/screenshot/frame-step; stats overlay (SCR-010e/FR-DIAG-001) stays unbuilt despite `DiagnosticBundleExporter` existing unwired (code_gap_brief §2.6). **Spec linkage:** SCR-010e, FR-DIAG-001, FR-PLAY-005/011.

#### G-20 · No PiP · P2 · M
AndroidX VideoPlaybackPictureInPicture path specified (FR-PLAT-001) and unimplemented; PiP is a cross-archetype delighter [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc), [69](https://accessibleandroid.com/app/mx-player-pro/), [28](https://firecore.com/releases). G-12 (session) is its prerequisite — PiP without a live session duplicates the two-player trap.

##### Concrete design proposals requested by the product owner

**(1) Per-video three-dot menu (library card), contents & ordering — most-frequent first, destructive last (SCR-018 confirmations on all destructive items):**

| Position | Item | Status/tier | Notes |
|---|---|---|---|
| 1 | **Play / Resume from 4:12** | Now | Resume call-out makes tri-state policy visible (F-13 fix family) |
| 2 | **Add to Up-next** (after R-03, else "Queue all here") | Now (P1, w/ queue) | Trains the queue model early |
| 3 | **Details** | Now (P1) | SCR-009 view: resolution, size, path, codecs, date, position |
| 4 | **Watched / Mark as watched / Unseen** | Now (P1, cheap) | Feeds watch-state badges (G-07) |
| 5 | **Share** | Now (P1) | FR-LIB-020 share handoff via chooser; no app-side network |
| 6 | **Open with… / play via external app** | Now (P1) | FR-LIB-020 ACTION_VIEW; also cover "open in browser-less world" honestly (no URL play by R1) |
| 7 | **Remove from library** | Now (P1) | Destructive *to DB only* — file untouched; SCR-018 confirmation copy; undo snackbar |
| 8 | Rename · Move · Delete file | R2 ⚠ (ADR) | R1 stays non-destructive; delete file = R2 with SAF + per-SCR-018 confirmation |
| 9 | Multi-select shelf (select all, play all, remove selected, share selected) | Now for library-level; share-files = R2 ⚠ | Matches VLC's re-added multi-delete pattern [2](https://play.google.com/store/apps/details?id=org.videolan.vlc) |

**(2) Player overflow menu (three-dot), per the UX note's evidence-backed ordering [103](https://designsystem.line.me/LDSG/components/contents/video-player-en/):** Audio · Subtitles · Aspect/Zoom · Playback speed · Video settings · Advanced · Chapters · (Lock). Bottom-sheet surfaces beat full-screen dialogs; keep stats overlay as the differentiator (G-19).

**(3) Dedicated audio & video settings screens/sheets (two entries, one IA):**

- **Audio sheet** — track list that *actually applies* (G-11 fix), per-track labels, **Audio delay** stepper (±50ms preset ladder, YouTube-style), **Night mode** (dynamic volume, toggle; honest "compression" label), **Volume boost** (caps 200% with a "may clip" honesty note; Just/Nova precedent [13](https://github.com/moneytoo/Player), [4](https://play.google.com/store/apps/details?id=org.courville.nova)), **Headphones**: pause-on-disconnect (Just precedent) + routing note ("Bluetooth latency is device-side; sync delay is the fix"), **Background play policy**: Always / While playing only / Off (FR-PLAY-018/FR-AUD-006), **Sleep timer** (YouTube-style ladder 10/20/30/60/90, "watch until end" option).
- **Video sheet (Video)** — **Decoder**: Auto (recommended) / Hardware (best battery) / Software — labeled honestly as *platform software decoding* today; on fallback show the SCR-010h "playing in compatibility mode — here's why" notice (G-15); **Aspect/Zoom**: 5 existing modes + pinch-pan free-zoom with per-video memory; **Rotation**: Auto / Landscape / Portrait — with the Android 16 reality documented: programmatic orientation switching is blocked, so implement via surface rotation and the documented per-app aspect-ratio workaround pattern (Just's approach [13](https://github.com/moneytoo/Player), [87](https://developer.android.com/about/versions/16/behavior-changes-16)); **Frame-rate matching**: kill-switch toggle (FR-PLAY-012), default off; **Brightness**: keep window-attr wheel (already shipped) **+ per-video remembered brightness** as the beyond-window-attr item; **Night/AMOLED chrome**: subtitle styling link (flagship lane) rather than a theme engine day one.

**Advanced sheet (SCR-010e, one destination):** decoder override (also here) · stats overlay (decode path, codecs, resolution/fps, dropped frames, A/V offset) · A-B repeat · sleep timer · subtitle delay · audio delay. This closes G-14 + G-15 + part of G-19 with one surface.

### 6.C Trust, privacy & accessibility — Aptura's moat, currently unmarketed

#### G-21 · The privacy proof exists in the manifest but has no product surface · P1 · S
Zero `INTERNET` is verified (code_gap_brief §1) but nowhere shown in-app or on the store. The field's working pattern is to make the claim first-class: F-Droid surfaces "No Internet permission" as a listing attribute [96](https://apt.izzysoft.de/fdroid/index.php/list/page/110?limit=25;repo=main), [97](https://dbeley.github.io/fdroid-insights/) and Musicolet built a following on exactly this language — "No Ads. No internet permission, Completely offline" [99](https://www.apkmirror.com/apk/krosbits/musicolet-music-player-free-no-ads/musicolet-music-player-free-no-ads-4-2-2-release/musicolet-music-player-free-no-ads-4-2-2-build183-android-apk-download/); privacy directories spotlight the same assertion [98](https://checkthis.fyi/); Next Player's F-Droid main-repo presence is the trust anchor FOSS peers hold [12](https://f-droid.org/en/packages/dev.anilbeesetti.nextplayer/). Concretely: an **About/Privacy screen (SCR-016)** with a manifest-diff-style claim ("this APK has zero INTERNET permission — check it"), plus store-listing copy and a privacy-policy page mirroring F-Droid labeling. **Spec linkage:** SCR-015/016, FR-SET-*, DEC-012. *(F-Droid listing itself remains an R1 non-goal — the *attribute language* is reusable without joining the channel.)*

#### G-22 · Accessibility unproven; the bar is low and beatable on purpose · P1 · S–M
MX Player Pro's player screen has multiple unlabeled buttons and TalkBack-only workarounds; nobody in the category is publicly praised as fully accessible [69](https://accessibleandroid.com/app/mx-player-pro/), [70](https://accessibleandroid.com/app). Aptura's own floor is unverified: chip selection is color-only (F-12), content descriptions unknown (Part C §5), RTL/font-scale untested. **Spec linkage:** FR-NF accessibility floor (never-cut list: "accessibility floor"); Part C §17 item 12. Deliberate wins: fully labeled controls, speed reachable without gesture passthrough, subtitle auto-read support, non-color selection indicators.

#### G-23 · No error taxonomy or redacted on-device diagnostics surface · P1 · M
`DiagnosticBundleExporter` was built for SCR-015 and never wired; there is no help UI, no stats overlay, no "why did it fall back" messaging, no structured error codes anywhere user-visible (code_gap_brief §2.6, §3). VLC's crash complaints and "currently playing screen missing for years" show what unowned failure UX does to a brand [47](https://discuss.grapheneos.org/d/2714-is-vlc-player-good-for-security-and-privacy), [2](https://play.google.com/store/apps/details?id=org.videolan.vlc). Zero telemetry (RSK-010) demands an **on-device diagnostics UI as the replacement**, not silence. **Spec linkage:** FR-PLAY-004, FR-DIAG-001–004, SCR-015/016.

### 6.D Platform & modernization

#### G-24 · Media3 1.9.0 primitives unadopted (app is on 1.5.1) · P2 · M
Dec 2025's 1.9.0 release hands local players exactly the R1-safe primitives: **`FrameExtractor`/`MetadataRetriever` for out-of-playback thumbnails and metadata** (scrub-preview filmstrip without touching the live player [81](https://android-developers.googleblog.com/2025/12/media3-190-whats-new.html), [82](https://developer.android.com/media/media3/inspector)), **`media3-ui-compose-material3` styled control components** (composable seek bar/button building blocks for M3 HUDs [83](https://developer.android.com/media/media3/ui/compose), [85](https://proandroiddev.com/from-androidview-to-playersurface-modernizing-exoplayer-with-media3s-compose-ui-74e40ce81f94)), **`setMediaButtonPreferences`** for lock-screen controls, **PreloadManager** disk cache, `selectTextByDefault` CC toggle, `StuckPlayerException` handling, and the **dav1d AV1 decoder** [84](https://github.com/androidx/media/releases). Upgrade before (or as part of) building the session/queue/HUD layers so G-12/G-13/G-19 are built on supported APIs. **Spec linkage:** stack modernization implied by FR-PLAT-00x; pairs with targetSdk 36 (G-25).

#### G-25 · targetSdk 35 → 36 + Android 16 behavior deltas unverified · P1 · S (recheck) 
Android 16 (API 36) brings predictive back, **foreground-service limits with declared types**, foldable/adaptive updates and the orientation-switching block [86](https://developer.android.com/about/versions/16/summary), [87](https://developer.android.com/about/versions/16/behavior-changes-16), [88](https://meisteritsystems.com/news/android-16-migration-guide-developer-checklist-api-36/). Direct impacts: **FR-CTRL-008/rotation** (orientation block — see G-16 workaround design), **FR-PLAY-018/FR-AUD-006 background play** (FGS type declaration becomes mandatory; Media3 1.9 adds wake-lock default-on [84](https://github.com/androidx/media/releases)), and **partial photo access / photo-picker policy pressure** on the library scanner posture [89](https://developer.android.com/about/versions/14/changes/partial-photo-video-access), [90](https://support.google.com/googleplay/android-developer/answer/16935362?hl=en). **Spec linkage:** spec S7 item 15 (targetSdk 36 recheck); RSK-013 device variance.

#### G-26 · No localization; English hardcoded · P2 · M–L
`strings.xml` contains exactly one string; all copy hardcoded (code_gap_brief §1). Regional evidence (§5.5–5.6) shows emerging-market sensitivity (SD-card fleets, low-end Go devices, local-language subtitle files) [92](https://medium.com/androiddevelopers/permissionless-is-the-future-of-storage-on-android-3fbceeb3d70a); RTL behavior of the symmetric HUD (±10s) is a real risk flagged untested (companion §5). **Spec linkage:** FR-NF localization-lite; companion accessibility audit.

#### G-27 · Widgets, app shortcuts, deep links absent · P3 · S–M
None implemented (code_gap_brief §3) and white space in all specs (§7) — a P3 concurrency item rather than a must (notification/session controls require G-12 first). **Spec linkage:** new FR proposal; deprioritize behind RSK-009 firewalls.

### 6.E Store-readiness & distribution

#### G-28 · Store package incomplete: data-safety, policy alignment, listing copy, brand protection · P1 · S
The manifest is the asset: zero INTERNET, no MANAGE_EXTERNAL_STORAGE, READ_MEDIA_VIDEO-only — directly aligned with Play's restricted-permission/photo-picker direction [90](https://support.google.com/googleplay/android-developer/answer/16935362?hl=en) and Google's permissionless-storage thesis [92](https://medium.com/androiddevelopers/permissionless-is-the-future-of-storage-on-android-3fbceeb3d70a). Missing artifacts: accurate **Data safety** form (no collection, no sharing), privacy policy page, listing copy with the zero-INTERNET claim + subtitle-flagship positioning [96](https://apt.izzysoft.de/fdroid/index.php/list/page/110?limit=25;repo=main), [99](https://www.apkmirror.com/apk/krosbits/musicolet-music-player-free-no-ads/musicolet-music-player-free-no-ads-4-2-2-release/musicolet-music-player-free-no-ads-4-2-2-build183-android-apk-download/), and **brand hygiene** — the fake-"Infuse" episode shows lookalike squatting is a live risk in this category [8](https://play.google.com/store/apps/details?id=com.infuse.infuse&hl=en_US). **Spec linkage:** SCR-015/016 artifacts; DEC-012; spec S (distribution lane).

#### G-29 · Monetization undecided (and the paid-ad-free shelf is momentarily empty) · P3 ⚠ Requires ADR · M
R1 = no monetization at launch (non-goal; ads stay banned permanently). The reference disappears at R2: **Infuse's per-feature IAP** ("Added free in-app purchases for Dolby and DTS features" [28](https://firecore.com/releases)) is the only researched per-feature unlock pattern in media players; RevenueCat 2026 shows monetization strength varies by market, ca. $32 median realized LTV per payer in NA after Year 1 [106](https://www.revenuecat.com/state-of-subscription-apps/). Market timing note: MX Player Pro's withdrawal consolidates ads-removal into subscription/VIP models ([55](https://xdaforums.com/t/mx-player-pro-no-longer-on-google-play.4692396), [3](https://play.google.com/store/apps/details?id=com.playit.videoplayer)) — the one-time-paid lane is untested at scale, and category precedent (Just's donations, VLC free) leans free [13](https://github.com/moneytoo/Player), [2](https://play.google.com/store/apps/details?id=org.videolan.vlc). Option B (R1.x, per-feature Pro unlocking depth — libass fidelity pack, styling editor, dual-sub display, stats overlay — never gating basics like subtitle sync/support) is viable but needs its own decision and, critically, a working, honest product first (§9). **Spec linkage:** DEC/monetization non-goal; doc 15 pending owner.

## 7. Prioritized recommendations

Scoring per item: **UV** = user value (1–5) · **FB** = feasibility in-boundary (1–5) · **IC** = impact on competitiveness (1–5) · **Σ** = sum (max 15). Tiers define *when*; Σ orders *within* tiers. Effort in dev-weeks (S ≈ ≤1, M ≈ 2–3, L ≈ 4–6; matches the spec estimate units of doc 16). **⚠** = crosses an R1 non-goal → Later/R2–R3 tier with an ADR required before any build.

### P0 — Fix the broken promises (dead wiring)

| Rec | Delivers | Gap | UV | FB | IC | Σ | Effort |
|---|---|---|---|---|---|---|---|
| **R-01** | Wire `selectAudioTrack`/`selectSubtitleTrack` to `player.trackSelectionParameters` (FR-AUD-001); per-video selection memory column | G-11 | 5 | 5 | 5 | **15** | 1 |
| **R-02** | Connect `ApturaPlaybackService` ↔ live player; media notification, lockscreen/BT/headset controls, background survival; retire the second ExoPlayer (FR-PLAT-002/003) | G-12 | 5 | 4 | 5 | **14** | 2–3 |
| **R-03** | Real queue from library context; Up-next/ended sheets; auto-advance with cancellable 10s countdown + 2h guard (FR-CTRL-017, SCR-010f/g); play-mode chip made real | G-13 | 5 | 4 | 5 | **14** | 2–3 |
| **R-04** | Advanced sheet v1: subtitle/audio delay UI + sleep timer (SCR-010e cap subset) | G-14 | 4 | 5 | 4 | **13** | 1–2 |

### P1 — Table stakes + spec-mandated (the "your own spec already demands this" tier)

| Rec | Delivers | Gap | UV | FB | IC | Σ | Effort |
|---|---|---|---|---|---|---|---|
| **R-05** | Onboarding + permission education + limited-mode state + honest empty-state copy (SCR-001/012); `MediaScanWorker` grant gating; `Error` UI state (F-19) | G-01 | 5 | 4 | 5 | **14** | 2 |
| **R-06** | Per-video three-dot menu v1 (details/share/open-with/remove-from-library, ordered per §6.B) + ACTION_VIEW/share intent filters (FR-LIB-020, SCR-009, SCR-018) | G-02, G-03 | 5 | 4 | 5 | **14** | 2–3 |
| **R-07** | Settings root + persistence: global defaults (speed/aspect/decoder policy/background/bg audio policy) + per-video memory columns (DataStore finally used) (SCR-013/014) | G-06 | 5 | 4 | 4 | **13** | 3 |
| **R-08** | Subtitle machinery v1: sidecar discovery/ranking/extra-folder, offset, encoding/mojibake fix, per-video memory (FR-SUB-001…009) — the flagship lane's floor | G-18 | 5 | 4 | 5 | **14** | 4–6 |
| **R-09** | Decoder override HW/SW/auto with honest labels + fallback ladder + "playing in compatibility mode" notice (FR-PLAY-002/003, SCR-010e/h) | G-15 | 5 | 4 | 4 | **13** | 2–3 |
| **R-10** | Media-details view (SCR-009/FR-LIB-013) | G-03 | 4 | 5 | 4 | **13** | 1 |
| **R-11** | Folder browser (SCR-004/005) + multi-select with batch play/queue/remove/share | G-04 | 4 | 4 | 4 | **12** | 2–3 |
| **R-12** | Scan progress surface + limited-mode banner (SCR-011/012) | G-05 | 4 | 4 | 4 | **12** | 1 |
| **R-13** | Diagnostics UI + error taxonomy, redacted, on-device (FR-PLAY-004, FR-DIAG-001–004; wire `DiagnosticBundleExporter`) | G-23 | 3 | 4 | 3 | **10** | 2 |
| **R-14** | About/Privacy in-app proof screen + Data-safety form, policy page, listing copy (G-21+G-28 package) | G-21, G-28 | 4 | 5 | 4 | **13** | 1 |
| **R-15** | Accessibility floor: content labels, TalkBack sweep, 200% fonts, non-color chip indicator (F-12; companion §5) | G-22 | 4 | 5 | 3 | **12** | 1–2 |
| **R-16** | FFmpeg LGPL audio-extension wiring decision + build (post-counsel) — 3–4 wk engineering IF unblocked | G-17 | 5 | 3 | 4 | **12** | 2–3 |
| **R-17** | Phase-0 correctness bundle (companion §17): scanner MIME/duration guard (F-01), grid key/contentType (§15), `isRefreshing` fix (F-16), dead-code removal (F-17) | G-10 | 4 | 5 | 4 | **13** | 1 |

### P2 — Differentiators (the "own the position" tier)

| Rec | Delivers | Gap | UV | FB | IC | Σ | Effort |
|---|---|---|---|---|---|---|---|
| **R-18** | PiP via AndroidX VideoPlaybackPictureInPicture (FR-PLAT-001) | G-20 | 4 | 4 | 4 | **12** | 2 |
| **R-19** | Scrub thumbnail preview via Media3 `FrameExtractor` (R1-safe; perf-toggleable) [81](https://android-developers.googleblog.com/2025/12/media3-190-whats-new.html), [104](https://developer.bitmovin.com/playback/docs/thumbnail-preview-support) | G-19 | 4 | 4 | 4 | **12** | 2–3 |
| **R-20** | Gesture depth: long-press 2× (9👍 precedent), pinch-pan into the 5-mode enum, surface-drag scrub (companion §11.2) | G-16 | 4 | 4 | 4 | **12** | 2 |
| **R-21** | **Whitespace trio**: folder counts in IA, hidden-folder exclusion list, on-demand duplicate detection (§4.4) | G-08 | 4 | 4 | 4 | **12** | 3–4 |
| **R-22** | Manual intro/outro segments + amplitude-based skip-silence (non-AI; Nova runs segment skipping on chapters/catalog metadata [16](https://github.com/nova-video-player/aos-AVP/releases)) | G-08 | 4 | 3 | 4 | **11** | 2–3 |
| **R-23** | Rails & badges: Continue / Recently-added / watch-history screen + watched/"NEW" labels + alphabet scrollbar (Next demands #687/#822/#1141 [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc); Infuse's IA model [28](https://firecore.com/releases)) | G-07 | 4 | 4 | 4 | **12** | 2–3 |
| **R-24** | Refresh-rate matching kill-switch (+ evaluated tunneled-playback later train; Just's differentiator [13](https://github.com/moneytoo/Player)) | G-16 | 3 | 3 | 4 | **10** | 2 |
| **R-25** | Subtitle sync wizard + styling preview + honor-original-styling toggle (fixes the Plex/Infuse complaints [73](https://www.reddit.com/r/PleX/comments/1gcfe6x/can_anyone_explain_the_subtitle_offset_once_and/), [72](https://www.reddit.com/r/infusevideoplayer/comments/1x4ym5c/infuse_857_now_available/)) | G-18 | 5 | 3 | 4 | **12** | 2–3 |
| **R-26** | Platform train: Media3 1.9 + targetSdk 36 adoption (compose-material3 controls, `setMediaButtonPreferences`, PreloadManager, dav1d) | G-24, G-25 | 3 | 4 | 3 | **10** | 2 |
| **R-27** | Localization-lite: string extraction, first non-English locales, RTL pass | G-26 | 3 | 3 | 3 | **9** | 3 |
| **R-28** | OTG/USB/secondary volumes via SAF/StorageManager | G-09 | 4 | 3 | 4 | **11** | 2–3 |
| **R-29** | Advanced sheet completion: stats overlay, A-B repeat, frame-step, screenshot (FR-DIAG-001 surface; mpv has literally no A-B UI [25](https://github.com/mpv-player/mpv/issues/7331)) | G-19 | 3 | 4 | 3 | **10** | 2 |

### P3 — Later tier (⚠ Requires ADR where non-goals are crossed) / R2–R3

| Rec | Delivers | Crossing | UV | FB | IC | Σ | Effort |
|---|---|---|---|---|---|---|---|
| **R-30** ⚠ | Network playback family (SMB/FTP/WebDAV/UPnP/HTTP/HLS/DASH) | Network non-goal | 4 | 1 | 4 | **9** | 6+ |
| **R-31** ⚠ | Casting / AirPlay | Network non-goal | 4 | 1 | 3 | **8** | 6+ |
| **R-32** ⚠ | Android TV / leanback build | TV non-goal (13👍 demand hedge [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) | 4 | 2 | 4 | **10** | 6+ |
| **R-33** ⚠ | Online subtitle search with user-handoff UX ("bring your own file" first) | Online-subs non-goal ([9](https://github.com/anilbeesetti/nextplayer) shows peers also defer) | 3 | 2 | 3 | **8** | 3–4 |
| **R-34** ⚠ | Destructive file ops: delete/rename/move files (SAF + SCR-018) | R1 non-destructive posture | 4 | 3 | 4 | **11** | 2–3 |
| **R-35** | Equalizer / audio FX | Explicit R1 non-goal; VLC's maintenance burden [2](https://play.google.com/store/apps/details?id=org.videolan.vlc) → **Reject-leaning** (Appendix B) | 3 | 1 | 2 | **6** | 6+ |
| **R-36** ⚠ | Monetization: per-feature "Aptura Pro" (Infuse-style [28](https://firecore.com/releases)) or donations | No-monetization-at-launch non-goal | 2 | 2 | 3 | **7** | 3–4 |
| **R-37** ⚠ | AI anything (scene detection, auto skip, upscaling, tagging) | DEC-007 AI non-goal → R3 | 3 | 1 | 3 | **7** | 6+ |
| **R-38** ⚠ | Trakt/cloud sync, multi-device positions | Accounts/cloud non-goal | 3 | 1 | 3 | **7** | 6+ |
| **R-39** | Widgets/app shortcuts/deep links | In-boundary, unplanned | 3 | 4 | 2 | **9** | 1–2 |

**Reading guidance.** R-01–R-04 are non-negotiable first (they convert lies into features; days of coding that rescue the entire perceived quality of the player). R-05–R-08 are the competitiveness floor and happen to be the app's own spec backlog — no new invention required. R-16, R-30–R-38 are fenced: build only after the named ADR/law-recheck, and never before P1 is shippable. Items deliberately **not** in this table (kids lock, EQ, thumb-scraper chat, engine swap) are dispositioned in Appendix B so nothing researched is silently dropped.

## 8. Overlooked use cases & scenarios

A player is judged by its behavior in the unglamorous moments. This matrix is the checklist the app must eventually pass; each row cites the gap(s) that currently prevent a good answer, and the fixtures/research that evidence it.

| Scenario | What must happen | Today | Gap(s) | Evidence |
|---|---|---|---|---|
| Corrupt/missing file discovered post-scan | Structured error card (taxonomy), removal from grid, no crash, rescan cleans up | ❌ Unfiltered scanner ingests junk (`MediaStoreScanner.kt:49–55`); no error library state (F-19) | G-01, G-05, G-15, R-17 | `fixtures/b_set/` missing-moov corpus exists; FR-PLAY-004/009 |
| SD card physically removed mid-play | Graceful stop, honest message, no crash on next resume, volume disappearance handled by rescan | ❌ No volume-loss handling anywhere | G-05, G-09 | Nova's remount bug shows peers fail publicly [18](https://github.com/nova-video-player/aos-AVP/issues/1454) |
| App killed then restored | Universal AOSP expectation: tri-state resume, evidently announced ("Resuming from 4:12" + auto-hide) | 🟡 Position restored *silently* (`PlayerViewModel.kt:188–193`, F-13) | R-05/R-07 polish fold-in | Companion F-13 |
| Headphones/BT unplugged mid-play | Auto-pause (route-change), no stuck-state confusion, routing-dependent audio delay note | ❌ No routing logic | G-12, G-16 | Just ships pause-on-disconnect [13](https://github.com/moneytoo/Player); Bluetooth latency as a killer need [67](https://xdaforums.com/t/app-5-0-just-video-player-no-bluetooth-lag-exoplayer-ffmpeg-audio-codecs.4189183) |
| Notification actions while locked | Media notification with correct controls after unplug/lock | ❌ No session = no notification at all | G-12 | 19👍 demand [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc) |
| Huge libraries (10k+) | Indexed search used, paging, stable keys, alphabet scrollbar, fast quiet scan | 🟡 In-memory filter only; DAO `searchVideos` unused; position-fragile grid keys (`MediaLibraryScreen.kt:338`) | G-10, R-17, G-23 | VLC's public 2-minute rescan pain [2](https://play.google.com/store/apps/details?id=org.videolan.vlc) |
| Filenames full of metadata noise (release tags, episode walls) | Display-name cleanup (title vs filename), optional series/grouping heuristic (non-AI), folder counts as orientation | ❌ Raw titles; raw paths in chips (F-06) | G-07, G-08 | Nova groups by seasons via scraping [4](https://play.google.com/store/apps/details?id=org.courville.nova) — out of R1; heuristics are the in-boundary variant |
| Duplicate files (same episode on both storages) | On-demand duplicate detection with keep/choose UI (no silent deletes) | ❌ Nothing | G-08 | Whitespace: no researched player ships it (§4.4) |
| Partial grant (READ_MEDIA_VISUAL_USER_SELECTED) | Limited-mode banner, re-permission path, honest count of what's visible | ❌ Denied == empty-ready | G-01, G-05 | PLAYit declares partial-access permission [50](https://playit.en.softonic.com/android); Play policy direction [90](https://support.google.com/googleplay/android-developer/answer/16935362?hl=en) |
| RTL locales & 200% font scale | Symmetric HUD survives mirroring; cards reflow; truncation logic holds | ❓ Untested (companion §5) | G-26, R-15 | RTL flagged a real risk in the audit |
| Android Go / low RAM | No photo-picker dependency (backport excludes Go [92](https://medium.com/androiddevelopers/permissionless-is-the-future-of-storage-on-android-3fbceeb3d70a)); efficient scan; low-memory decode paths | ❓ Untested | G-09, G-24 | Emerging-market segment evidence [92](https://medium.com/androiddevelopers/permissionless-is-the-future-of-storage-on-android-3fbceeb3d70a) |
| Portrait video opened in landscape / rotation under Android 16 | Surface rotation or documented per-app workaround (orientation block [87](https://developer.android.com/about/versions/16/behavior-changes-16)); portrait media gets blurred-cover padding decision | ❌ No orientation logic at all (F-18) | G-16, R-20/R-26 | Companion §14; Just's README workaround [13](https://github.com/moneytoo/Player) |
| Audio-only file encountered post-scan (`b_B09_02_audio_only.flac`) | Honest presentation ("audio item"), no black screen confusion | ❓ No dedicated state | G-23 | Companion capture matrix item 5 |
| File with no subs and no sidecar | Honest "no subtitles found — bring your own file" affordance (Chromeless, zero-INTERNET world) | 🟡 Sheet lists embedded tracks only | G-18 | Flagship lane floor |
| Sleep timer crossing item end | Timer holds across items, cancels cleanly, 2h-inguard compatibility | 🟡 Placeholder only (`sleepTimerRemainingMs`) | G-14, G-13 | SCR-010e |
| Item ends with a queue | Freeze on last frame (no auto-loop), Up-next card, cancellable countdown, foreground-only | ❌ No queue, no `ENDED` design | G-13 | FR-CTRL-017 already specifies it |
| Battery kill mid auto-advance | Countdown cancelled safely; no phantom progress on next item | ❌ No advance at all | G-13, R-07 | FR-CTRL-017 guard rules |

## 9. Differentiation strategy — how to own "the honest player"

Aptura's end-state brand is **"the honest player"**: a product whose claims are verifiable in the APK, whose failures explain themselves, and whose library actually manages files. Four pillars, each built from in-boundary work already researched here:

**Pillar 1 — Subtitle flagship (management UX now, fidelity behind the gate).** Aptura's chosen #1 differentiator is exposed where competitors are demonstrably weak: ASS/SSA is still broken in the mainstream stacks (Jellyfin, Wholphin [62](https://www.reddit.com/r/jellyfin/comments/v7v053/the_android_app_integrated_players_ass_subtitle/), [63](https://github.com/damontecres/Wholphin/issues/1049), [26](https://github.com/google/ExoPlayer/issues/8435)), Infuse keeps polishing styling edge cases alone [28](https://firecore.com/releases), and the honor-styling-vs-override complaint is live user pain ([72](https://www.reddit.com/r/infusevideoplayer/comments/1x4ym5c/infuse_857_now_available/)). The lane is explicitly *not* online search (Next Player hasn't shipped it either [9](https://github.com/anilbeesetti/nextplayer) — R1's boundary is defensible, not suicidal): it is sidecar discovery/ranking/memory, a **sync wizard** with direction-disambiguation and reference-frame juxtaposition (the Plex offset confusion is exactly what to fix [73](https://www.reddit.com/r/PleX/comments/1gcfe6x/can_anyone_explain_the_subtitle_offset_once_and/), [74](https://github.com/jellyfin/jellyfin-web/discussions/4364)), styling preview, honor-original toggle, honest encoding/mojibake handling — and **libass fidelity as the gated end-state** (RQ-06: if the native Media3 result misses the SSA/ASS quality bar, an integration plan with cost/date leads to R2; spinning up libass earlier is RSK-008 exposure).

**Pillar 2 — Verifiable privacy as marketing artifact.** Evidence-first: make the zero-INTERNET claim legible the way F-Droid surfaces "No Internet permission" [96](https://apt.izzysoft.de/fdroid/index.php/list/page/110?limit=25;repo=main) and Musicolet's listing does [99](https://www.apkmirror.com/apk/krosbits/musicolet-music-player-free-no-ads/musicolet-music-player-free-no-ads-4-2-2-release/musicolet-music-player-free-no-ads-4-2-2-build183-android-apk-download/) — in-app About (manifest-diff claim), store listing, README; add the "smallest possible access" talking point that Play's own policy direction rewards [90](https://support.google.com/googleplay/android-developer/answer/16935362?hl=en), and publish an on-device diagnostics surface (G-23) as the anti-telemetry statement. This aligns with the 2026 rounding where privacy-clean positioning is a headline feature itself [9](https://github.com/anilbeesetti/nextplayer), [34](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care).

**Pillar 3 — Decoder/hardware honesty.** Nobody markets fallback explanations; Next Player proves the mechanic's worth ([10](https://github.com/anilbeesetti/nextplayer/blob/main/feature/player/DECODER_SWITCHING.md)) and users hunt codecs because no one tells them what happened ([37](https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm)). Executive-behavior package: honest HW/SW labels, per-item decoder memory, "playing in compatibility mode — here's why" notices (SCR-010h), stats overlay (SCR-010e/FR-DIAG-001), HDR/DV badges (Next's HDR-tag request #1260 [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) and a rate-matching kill-switch (FR-PLAY-012). Honesty labels cost days and define the brand.

**Pillar 4 — Occupied whitespace.** Folder counts, hidden-folder exclusion lists, and duplicate detection are absent from every researched player (§4.4) and simultaneously absent from Aptura's specs — build them as *the* structured-library differentiators, alongside **manual intro/outro segments + amplitude-based skip silence** (Nova's auto-skip is rare [16](https://github.com/nova-video-player/aos-AVP/releases); the amplitude path is deterministic, no AI [76](https://github.com/vantezzen/skip-silence); *frame-difference heuristics are TBD and must be validated before any claim — label the feature "experimental" until they pass*), watch-state labels, and volume boost (widely demanded in-context [24](https://github.com/mpv-player/mpv/discussions/14257)).

**What NOT to build (protect the strategy from feature-for-feature's-sake):** equalizer/audio FX (explicit non-goal; VLC's equalizer-fix churn shows the liability [2](https://play.google.com/store/apps/details?id=org.videolan.vlc)); torrents/magnet/downloaders [30](https://www.kmplayer.com/home), [32](https://kmplayerofficial.blogspot.com/2025/); the streaming funnel (MX's decline is the cautionary case [41](https://www.hollywoodreporter.com/business/business-news/apos-amazon-streaming-india-prime-video-and-mx-player-1236300136/), [43](https://ppc.land/amazon-kills-mx-player-brand-folding-it-into-prime-video-for-india-ad-reach/)); an mpv-class engine swap (there is no engine-war win in swapping; cherry-pick *dual subtitles* instead [20](https://github.com/mpv-android/mpv-android)); scraping/Trakt/AI (R3 ⚠). Also: no F-Droid channel work during R1 (posture; the *attribute language* is reusable), and no Kids Lock until the `spec/03` personas demand it (companion §16 guard). The full disposition table is Appendix B.

## 10. Roadmap

Sequencing principle: **nothing differentiates until nothing lies.** Effort is dev-weeks (doc-16 units); acceptance anchors name the FR/SCR that closes. The three phases map 1:1 onto Part C's "Definition of 'exceeding MX': Phase 0–1 = *not embarrassing*, Phase 2 = *competitive*, Phase 3 = *distinctive*" — adopted here as the joint phase checklist with the visual layer.

### Now (0–4 weeks) — "not embarrassing"

| # | Item (Rec → Gap) | Effort | Acceptance anchor |
|---|---|---|---|
| 1 | Correctness bundle: scanner MIME/duration guard, friendly folder names, grid keys/contentType, `isRefreshing`, dead-code purge, error UI state (R-17, folds F-01/06/16/17/19) | 1 | Companion §17 Phase 0–1 checklist; F-01–F-06 closed on re-capture |
| 2 | **R-01** track application + per-video selection memory | 1 | FR-AUD-001: non-default track sticks after app restart |
| 3 | **R-02** MediaSessionService ↔ live player; notification/lockscreen/BT (FR-PLAT-002/003) | 2–3 | Controls functional from lock screen; one ExoPlayer instance remains |
| 4 | **R-03** queue + Up-next/ended + cancellable auto-advance + 2h guard (FR-CTRL-017/SCR-010f/g); play-mode real | 2–3 | FR-CTRL-017 end-of-item matrix green; play-modes reflect real state |
| 5 | **R-04** advanced sheet v1: subtitle/audio delay + sleep timer (SCR-010e cap) | 1–2 | Delay audibly effective; timer stops at edge items per ladder |
| 6 | **R-05** onboarding + permission education + limited mode (SCR-001/012; F-15) | 2 | Denied state ≠ empty state; worker refuses without grant |
| 7 | **R-10 + R-12** details view + scan-progress/limited banner | 2 | SCR-009/SCR-011/SCR-012 closed |
| 8 | **R-14** About/Privacy proof screen + Data-safety draft + listing claims | 1 | G-21/G-28 artifacts review-ready |

≈ 12–17 dev-weeks across the window; parallelizable (player lane vs library lane vs platform lane). With R-01→R-05 done, the app stops promising things it does not do.

### Next (4–12 weeks) — "competitive"

| # | Item | Effort | Acceptance anchor |
|---|---|---|---|
| 9 | **R-07** settings root + per-video/global persistence (full DataStore adoption) | 3 | FR-SET family green; defaults survive restarts |
| 10 | **R-08** subtitle machinery v1 (FR-SUB-001…009): sidecar discovery/ranking/extra folder, offset, encoding fix, per-video memory | 4–6 | FR-SUB suite; mojibake appears correctly on the encoding fixtures |
| 11 | **R-09** decoder override + fallback ladder + compat notice (SCR-010e/h; FR-PLAY-002/003) | 2–3 | Fallback of an `adv_media` fixture explains itself; per-item memory holds |
| 12 | **R-06** three-dot menu v1 + intent filters (FR-LIB-020, SCR-018 confirmations) | 2–3 | ACTION_VIEW/share validated against test files; remove-from-library confirms + undo |
| 13 | **R-11 + R-12(scan refresh)** folder browser + multi-select; scan progress bakes into it | 2–3 | SCR-004/005 closed |
| 14 | **R-13** diagnostics UI + error taxonomy wired to exporter | 2 | SCR-015/016 open from the help/about surface; FR-DIAG-001–004 |
| 15 | **R-15 + R-26** a11y floor + platform train (Media3 1.9, targetSdk 36) | 3–4 | TalkBack sweep zero unlabeled; 36 checklist deltas dispositioned |
| 16 | **R-16** FFmpeg audio ext build+wiring **counsel-gated** | 2–3 | ADR-010 contract honored; audio-ext fixtures play or decision recorded not to |

### Later (12+ weeks) — "distinctive" (⚠ where non-goals are crossed — ADR before any build)

| # | Item | Effort | Anchor |
|---|---|---|---|
| 17 | **R-18/R-19/R-20** PiP + scrub preview (`FrameExtractor`) + gesture depth (long-press 2×, pinch-pan, surface-drag) | 5–7 | FR-PLAT-001; scrub preview toggle; §11.2 matrix fully ✅ |
| 18 | **R-21 + R-22** whitespace trio (folder counts, hidden-folder exclusion, dup detection) + manual intro/outro segments + skip-silence (experimental label until frame-difference heuristics validated) | 5–7 | New FRs accepted; no-AI constraint documented (DEC-007-compatible) |
| 19 | **R-23 + R-25** rails/badges + subtitle sync wizard + styling preview + honor-original toggle | 4–6 | Flagship pillars 1–4 of §9 materially landed |
| 20 | **R-24/R-28/R-29** refresh-rate kill-switch, OTG/USB volumes, stats overlay/A-B/frame-step/screenshot completion | 5–6 | FR-PLAY-012 toggle; SAF volumes list; SCR-010e full matrix |
| 21 | ⚠ **ADR lane** (R-30/31/32/33/34): network playback, casting, TV build, online subs w/ handoff, destructive file ops | 6+ each | Each requires a filed ADR + owner sign-off; never enter while P1 open |
| 22 | ⚠ **R2/R3 candidates** (R-35→R-39): equalizer (Reject-leaning), monetization option B, AI, cloud sync, widgets/shortcuts | — | Owner decision only; see Appendix B/C |

**Definition of competitive (phase alignment):** Phase 0–1 = the P0 wiring fixed and Phase-0 correctness landed → *not embarrassing*; Phase 2 = Now+Next complete → *competitive* (12/12 in-boundary table stakes alive, spec R1 MUSTs closed); Phase 3 = Later tiers → *distinctive* (honest-player pillars shipped, whitespace owned). Each phase is independently shippable, per Part C.

## 11. Risks & dependencies

| # | Risk / dependency | Detail | Handling |
|---|---|---|---|
| 1 | **LGPL/`aac` contradiction (⚖️ hard gate)** | The FFmpeg build script enables `aac` decode while ADR-010/doc-07 §10 forbids bundled AAC; the extension is fully built-but-unwired (RQ-02 ✅) [code_gap_brief §4] | Counsel verdict **before** R-16; fallback = ship without audio ext (RSK-002/L1) and label codec honesty accordingly |
| 2 | **RQ-01 engine conformance spike open** | Playback-reliability discipline (long-session A/V sync, seek, resume) is a differentiator the engine isn't proven on yet [RSK-001] | Keep spike-ranked; never cut "playback honesty paths" (never-cut list) |
| 3 | **targetSdk 35 → 36** | FGS limits vs FR-PLAY-018/FR-AUD-006 background policy; orientation block vs FR-CTRL-008 (rotation modes); predictive back [86](https://developer.android.com/about/versions/16/summary), [87](https://developer.android.com/about/versions/16/behavior-changes-16), [88](https://meisteritsystems.com/news/android-16-migration-guide-developer-checklist-api-36/) | Adopt inside R-26 train; rotation implemented as surface rotation; FGS type declared when session lands |
| 4 | **Android 16 orientation block** | FR-CTRL-008 (rotation modes) cannot use `requestedOrientation` on 16 devices | Just Player documents the per-app aspect-ratio workaround pattern; adopt + document honestly [13](https://github.com/moneytoo/Player) |
| 5 | **FGS limits vs background play** | Media3 1.9 wake-lock default-on and session-aware FGS types change the contract [84](https://github.com/androidx/media/releases) | Build background survival on R-02 only after the type declaration; no notification-less background video |
| 6 | **Zero-telemetry blind spots (RSK-010)** | No crash reports, no funnel data — shipping blind | On-device diagnostics UI (R-13) as the anti-telemetry statement; DiagnosticsBundle fields defined redacted-by-default |
| 7 | **Market window is real but not infinite** | MX Pro withdrawal opened the paid-ad-free shelf [55](https://xdaforums.com/t/mx-player-pro-no-longer-on-google-play.4692396); Next Player's star rate and VLC's UX drift show movable cohorts [64](https://www.reddit.com/r/androidapps/comments/17resjv/the_best_free_android_video_player_at_the_moment) [34](https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care) | P0+P1 speed matters more than P2 polish; monetization decision only after product honesty (G-29) |
| 8 | **Subtitle edge cases erode the flagship (RSK-008)** | ASS edge cases are proven rabbit holes [28](https://firecore.com/releases) | RQ-06 gate discipline; honest fallback labeling from day one |
| 9 | **Libraries at scale on adversarial storage (RSK-013)** | SD/low-end variance; scan correctness claimed as instant (a never-claim) | R-17/R-12 first; benchmarks on `spec/13` corpora before promise language |
| 10 | **Scope creep (RSK-009)** | 106 researched sources tempt parity hunting | Appendix B dispositions everything; de-scope ladder is the firewall |

## 12. Next steps — the next two weeks, then the ship train

Twelve concrete actions for the next two weeks (owner-role placeholders for the team to fill; every item names its file targets and Definition of Done):

| # | Action | Owner (role) | File targets | Definition of Done |
|---|---|---|---|---|
| 1 | Apply track selection to the real Player (`trackSelectionParameters`) + per-video memory column | *Player-eng lead* | `PlayerViewModel`, `PlayerManager`, `MediaItemDao`, sheets at `PlayerScreen.kt:778–904` | Non-default audio/sub track audibly/visibly sticks; survives app restart; "Default Audio Track" only when true |
| 2 | Queue orchestration: stop dropping the list in `onVideoClick`; build Up-next/ended + cancellable advance + 2h guard | *Player-eng lead* | `Navigation.kt:47–49`, new `PlaybackQueue` module, `PlayerScreen.kt:647–662` | Multi-item play = N-item queue; skip/prev/shuffle/repeat functional; `ENDED` matrix green |
| 3 | Connect `ApturaPlaybackService` ↔ live `PlayerManager`; single ExoPlayer; media notification | *Platform/Android eng* | `ApturaPlaybackService`, `PlayerManager`, manifest | Lockscreen/BT/headset controls work; removal or unbind kills playback cleanly |
| 4 | Advanced sheet v1 — subtitle/audio delay + sleep timer UI on existing state | *Player-eng lead* | `PlayerScreen.kt` sheets, `PlayerViewModel` delay/sleep fields | Delay applied live; sleep ladder stops playback per spec ladder |
| 5 | Onboarding + permission flow + limited mode + honest empty state (SCR-001/012) | *Library/UX eng* | `Navigation.kt`, new onboarding destination, `MediaScanWorker.kt:26–76`, `strings.xml` | Denied access shows limited-mode banner + re-request path; no positive copy on failure |
| 6 | Three-dot menu v1 + intent filters (details/share/open-with/remove-from-library; SCR-018 copy) | *Library-eng + UX* | `MediaLibraryScreen.kt` card, details screen, manifest intent filters, shared `DetailsSheet` | One menu per card; ACTION_VIEW/share validated; confirmations per SCR-018 |
| 7 | Scanner guard: MIME `video/%` + duration + size filter; friendly bucket names from already-captured BUCKET_DISPLAY_NAME; KDoc reconciliation | *Library-eng* | `MediaStoreScanner.kt:41–97`, `MediaItemDao.kt:31–32` | Corrupted capture corpus no longer ingestible; chips show friendly names |
| 8 | Grid correctness: stable `item.uri` keys + `contentType`; `isRefreshing` combine fix; dead-code purge (F-16/F-17) | *Library-eng* | `MediaLibraryScreen.kt:338`, `MediaLibraryViewModel.kt:36–42,79`, `MainScreen.kt` (delete), imports | Scroll identity stable under sort/search; refresh spinner always truthful |
| 9 | Details view + watch-state surfacing (`isCompleted` renders; "Resuming from 4:12" toast) | *Library/UX eng* | New `DetailsScreen`, `PlayerViewModel.kt:188–193`, `PlayerUiState.kt:69–80` | SCR-009 fields complete; resume is announced and auto-hides |
| 10 | Settings-root scaffold with real persistence (globals now; per-video columns next): decoder policy, speed default, aspect default, background policy | *Platform/Android eng* | New `SettingsScreen(s)`, DataStore wiring, `PlayerManager` | Chosen defaults survive restart; DataStore no longer dead weight |
| 11 | Platform train prep: Media3 1.9 + targetSdk 36 compile upgrade, delta checklist, `FrameExtractor` POC for scrub preview | *Platform/Android eng* | `build.gradle.kts:16`, version catalog, POC spike | Upgrade compiles; delta list dispositioned; POC extracts frames from a `tools/corpus` fixture without playback |
| 12 | Store package: Data-safety answers, privacy-policy page, listing copy with zero-INTERNET claim + About/Privacy screen | *Product + legal-reviewer* | Store listing assets, `SCR-016` About screen, welcomed Policy doc | Artifacts approve-ready in review; claims match manifest exactly |

## Appendix A — Source register

All URLs from `competitor_feature_matrix.md`, `user_expectations_pain_points.md`, and `ux_patterns_opportunities.md` (deduplicated; trailing-slash variants merged). 👍 reaction counts reflect the Oct 2026 fetch. Items the researchers flagged as unresolved/not-fetched are marked *(unverified)*.

**Player listings & repositories**

1. MX Player (free) Play listing — https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad
2. VLC for Android Play listing (incl. user-review citations) — https://play.google.com/store/apps/details?id=org.videolan.vlc
3. PLAYit Play listing (incl. dev reply + reviews) — https://play.google.com/store/apps/details?id=com.playit.videoplayer
4. Nova Video Player Play listing — https://play.google.com/store/apps/details?id=org.courville.nova
5. KMPlayer Play listing — https://play.google.com/store/apps/details?id=com.kmplayer&hl=en_US
6. mpv-android Play listing — https://play.google.com/store/apps/details?id=is.xyz.mpv&hl=en_US
7. Mi Video Play listing — https://play.google.com/store/apps/details?id=com.miui.videoplayer&hl=en_US
8. Fake "Infuse" Play listing (brand squatting) — https://play.google.com/store/apps/details?id=com.infuse.infuse&hl=en_US
9. Next Player (GitHub) — https://github.com/anilbeesetti/nextplayer
10. Next Player · DECODER_SWITCHING.md — https://github.com/anilbeesetti/nextplayer/blob/main/feature/player/DECODER_SWITCHING.md
11. Next Player issues sorted by 👍 — https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc
12. Next Player on F-Droid — https://f-droid.org/en/packages/dev.anilbeesetti.nextplayer/
13. Just Player (GitHub README/FAQ) — https://github.com/moneytoo/Player
14. Just Player issues sorted by 👍 — https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc
15. Nova (GitHub) — https://github.com/nova-video-player/aos-AVP
16. Nova GitHub releases (intro/outro skip, Trakt, Lists) — https://github.com/nova-video-player/aos-AVP/releases
17. Nova issue #1651 (DV/SMB) — https://github.com/nova-video-player/aos-AVP/issues/1651
18. Nova issue #1454 (remount misses) — https://github.com/nova-video-player/aos-AVP/issues/1454
19. Nova FAQ (passthrough modes) — https://home.courville.org/nova_video_player-faq/index.html
20. mpv-android (GitHub) — https://github.com/mpv-android/mpv-android
21. mpvKt (GitHub) — https://github.com/abdallahmehiz/mpvKt
22. mpvKt on IzzyOnDroid — https://apt.izzysoft.de/fdroid/index/apk/live.mehiz.mpvkt
23. mpv manual (libass) — https://mpv.io/manual/master/
24. mpv discussion #14257 (dev shortage/volume boost/subs) — https://github.com/mpv-player/mpv/discussions/14257
25. mpv issue #7331 (no A-B UI) — https://github.com/mpv-player/mpv/issues/7331
26. ExoPlayer issue #8435 (SSA/ASS limit) — https://github.com/google/ExoPlayer/issues/8435
27. Infuse product page — https://firecore.com/infuse
28. Infuse release notes (all 8.x features) — https://firecore.com/releases
29. Firecore community, "Infuse for Android" — https://community.firecore.com/t/infuse-for-android/11048
30. KMPlayer.com (Premium/Magnet) — https://www.kmplayer.com/home
31. KMPlayer on Uptodown (Chromecast/URL) — https://kmplayer.en.uptodown.com/android
32. KMPlayer official blog 2025 — https://kmplayerofficial.blogspot.com/2025/
33. playit.app (India positioning) — https://www.playit.app
34. Android Police, "VLC is ugly and I don't care" — https://www.androidpolice.com/vlc-is-ugly-and-i-dont-care
35. Android Police, obscure players — https://www.androidpolice.com/i-love-these-obscure-video-players-on-android
36. Beebom, best Android video players — https://beebom.com/best-android-video-player-apps/
37. free-codecs, Android TV player guide (codec-pack demand) — https://www.free-codecs.com/guides/the-best-video-player-for-android-tv.htm
38. ottmega, best Android media player 2026 — https://ottmega.com/blog/best-android-media-player-2026
39. 2026 video-roundup (YouTube) — https://www.youtube.com/watch?v=rBEhABqzQi0
40. vdocipher, top video players roundup — https://www.vdocipher.com/blog/top-video-player-for-android
41. Hollywood Reporter, Amazon's MX Player acquisition — https://www.hollywoodreporter.com/business/business-news/apos-amazon-streaming-india-prime-video-and-mx-player-1236300136/
42. The Arc, MX + MiniTV AVOD merge — https://www.thearcweb.com/article/amazon-merges-mx-player-minitv-for-adbased-streaming-C4eTpcoe4iCfcz7f
43. ppc.land: brand folded into Prime Video India — https://ppc.land/amazon-kills-mx-player-brand-folding-it-into-prime-video-for-india-ad-reach/
44. Forbes, Amazon's AI-for-AVOD plans — https://www.forbes.com/sites/swetakaushal/2025/02/12/how-amazon-mx-player-plans-to-use-ai-for-avod-growth/
45. Samsung Community, S25 native player removed — https://us.community.samsung.com/t5/Galaxy-S25/Disappointed-the-Samsung-video-player-app-was-removed/td-p/3251786
46. Yahoo Tech, Galaxy S25 video features in Gallery — https://tech.yahoo.com/phones/articles/hidden-galaxy-s25-feature-makes-164505954.html
47. GrapheneOS forum, VLC security/privacy critique — https://discuss.grapheneos.org/d/2714-is-vlc-player-good-for-security-and-privacy
48. itnext.io, scoped storage lessons — https://itnext.io/navigating-the-challenges-of-scoped-storage-lessons-for-modern-android-apps-b5fd8318a02c
49. GameAppChecker, PLAYit audit (23 perms/14 types) — https://gameappchecker.com/app/com.playit.videoplayer
50. Softonic PLAYit scan (sensitive permissions) — https://playit.en.softonic.com/android
51. cinaura.tv, Infuse Android TV status — https://www.cinaura.tv/infuse-for-android-tv

**User evidence (reviews, forums, trackers)**

52. App Store MX Player reviews — https://apps.apple.com/lk/app/mx-player-all-in-one-player/id1594318832?see-all=reviews&platform=iphone
53. MouthShut MX Player reviews — https://www.mouthshut.com/product-reviews/mx-player-reviews-925903202-page-6
54. Reddit r/androidapps, "what went wrong with MX (2017 ads)" — https://www.reddit.com/r/androidapps/comments/5pm0ap/discussion_whats_upwent_wrong_with_mx_player
55. XDA, MX Player Pro off Play — https://xdaforums.com/t/mx-player-pro-no-longer-on-google-play.4692396
56. Reddit r/Android, Amazon takes over MX Player — https://www.reddit.com/r/Android/comments/1gf1psl/amazon_takes_over_mx_player_app_takes_down_pro/
57. MX support, escalate-inappropriate-ads flow — https://support.mxplayer.in/support/solutions/articles/43000660733-how-to-report-escalate-inappropriate-or-explicit-ads-
58. Reddit r/VLC redesign thread — https://www.reddit.com/r/VLC/comments/1x6h1y1/new_ui_redesign_what_do_you_think
59. Reddit r/fossdroid VLC critique — https://www.reddit.com/r/fossdroid/comments/1exnfvw/whats_a_good_video_player_option_vlc_isnt_that
60. Trustpilot, VLC media player reviews — https://www.trustpilot.com/review/www.vlc-media-player.org
61. Reddit r/VLC, background audio cut-outs (Android 16) — https://www.reddit.com/r/VLC/comments/1utfwkc/finally_given_up_on_this_app_and_uninstalled_it
62. Reddit r/jellyfin, ASS subtitles broken — https://www.reddit.com/r/jellyfin/comments/v7v053/the_android_app_integrated_players_ass_subtitle/
63. Wholphin issue #1049 (ASS invisible) — https://github.com/damontecres/Wholphin/issues/1049
64. Reddit 17resjv, "best free Android video player" thread — https://www.reddit.com/r/androidapps/comments/17resjv/the_best_free_android_video_player_at_the_moment
65. Reddit 1933xok, 2024 player recommendations — https://www.reddit.com/r/androidapps/comments/1933xok/video_player_for_android_in_2024
66. Reddit 1lbmx1q, MX-alternatives migration thread — https://www.reddit.com/r/androidapps/comments/1lbmx1q/best_media_player_best_alternative_of_mx_player
67. XDA, Just Player Bluetooth lag differentiation — https://xdaforums.com/t/app-5-0-just-video-player-no-bluetooth-lag-exoplayer-ffmpeg-audio-codecs.4189183
68. Reddit r/AndroidTV, Nova TV critique — https://www.reddit.com/r/AndroidTV/comments/1kb9qil/nova_video_player_is_the_best_for_android_tv/
69. Accessible Android, MX Player Pro review — https://accessibleandroid.com/app/mx-player-pro/
70. Accessible Android app directory — https://accessibleandroid.com/app
71. AFB AccessWorld, TalkBack Magic Tap — https://afb.org/aw/22/5/17556
72. Reddit r/infusevideoplayer, 8.5.7 ASS-override complaint — https://www.reddit.com/r/infusevideoplayer/comments/1x4ym5c/infuse_857_now_available/
73. Reddit r/PleX, subtitle-offset confusion — https://www.reddit.com/r/PleX/comments/1gcfe6x/can_anyone_explain_the_subtitle_offset_once_and/
74. Jellyfin-web discussion #4364 (sync workflow) — https://github.com/jellyfin/jellyfin-web/discussions/4364
75. Reddit r/androidapps, offline skip-silence request — https://www.reddit.com/r/androidapps/comments/11r125z/any_offline_video_player_with_skip_silence_option/
76. skip-silence (GitHub, amplitude heuristic) — https://github.com/vantezzen/skip-silence
77. auto-skip extension (GitHub) — https://github.com/Meowsbot/auto-skip
78. REX-Player issue #78 (chapter gestures) — https://github.com/mpvRex/REX-Player/issues/78
79. Morphe #2244 (YouTube double-tap chapter) — https://github.com/MorpheApp/morphe-patches/issues/2244
80. Jellyfin-android issue #155 (next-chapter button) — https://github.com/jellyfin/jellyfin-android/issues/155

**Platform, trust and monetization**

81. Media3 1.9.0 release post (Dec 2025) — https://android-developers.googleblog.com/2025/12/media3-190-whats-new.html
82. Media3 inspector docs (`MetadataRetriever`/`FrameExtractor`) — https://developer.android.com/media/media3/inspector
83. Media3 Compose UI guide — https://developer.android.com/media/media3/ui/compose
84. androidx/media releases (notification/session APIs, PreloadManager, dav1d) — https://github.com/androidx/media/releases
85. ProAndroidDev, modernizing ExoPlayer with Compose — https://proandroiddev.com/from-androidview-to-playersurface-modernizing-exoplayer-with-media3s-compose-ui-74e40ce81f94
86. Android 16 summary — https://developer.android.com/about/versions/16/summary
87. Android 16 behavior changes — https://developer.android.com/about/versions/16/behavior-changes-16
88. Android 16 migration checklist (third-party) — https://meisteritsystems.com/news/android-16-migration-guide-developer-checklist-api-36/
89. Partial photo/video access — https://developer.android.com/about/versions/14/changes/partial-photo-video-access
90. Play restricted-permissions policy — https://support.google.com/googleplay/android-developer/answer/16935362?hl=en
91. Embedded photo-picker post — https://android-developers.googleblog.com/2026/01/ *(unverified — URL truncated in notes)*
92. "Permissionless is the future of storage" (Android Developers Medium) — https://medium.com/androiddevelopers/permissionless-is-the-future-of-storage-on-android-3fbceeb3d70a
93. Datalogic, Android 13 media permissions — https://developer.datalogic.com/mobile-computers/news/a13_new_permissions_for_file_access
94. StackOverflow, Android 13 media permission behavior — https://stackoverflow.com/questions/72948052/android-13-read-external-storage-permission-still-usable
95. AOSP scoped-storage rationale — https://source.android.com/docs/core/storage/scoped
96. IzzyOnDroid listings carrying "No internet permission" attribute — https://apt.izzysoft.de/fdroid/index.php/list/page/110?limit=25;repo=main
97. F-Droid Insights index — https://dbeley.github.io/fdroid-insights/
98. checkthis.fyi privacy directory — https://checkthis.fyi/
99. Musicolet listing ("No Ads. No internet permission, Completely offline") — https://www.apkmirror.com/apk/krosbits/musicolet-music-player-free-no-ads/musicolet-music-player-free-no-ads-4-2-2-release/musicolet-music-player-free-no-ads-4-2-2-build183-android-apk-download/
100. Google M3E announcement — https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/
101. Material 3 Expressive blog — https://m3.material.io/blog/building-with-m3-expressive
102. Compose M3 docs — https://developer.android.com/develop/ui/compose/designsystems/material3
103. LINE design system, video player spec — https://designsystem.line.me/LDSG/components/contents/video-player-en/
104. Bitmovin, thumbnail-preview support — https://developer.bitmovin.com/playback/docs/thumbnail-preview-support
105. Vidzflow, player controls UX best practices — https://www.vidzflow.com/blog/mastering-video-player-controls-ux-best-practices
106. RevenueCat, State of Subscription Apps 2026 — https://www.revenuecat.com/state-of-subscription-apps/

## Appendix B — Feature decision ledger

Every feature surfaced by the three research notes is dispositioned here: **Adopt-now** (in the active roadmap tiers P0/P1), **Adopt-later** (P2/P3, with ⚠ Requires ADR where an R1 non-goal is crossed), or **Reject** (with the reason, so nothing researched dies in silence). Boundary flags: ✅ = inside R1 boundary; ❌ = outside ([OUT]).

| Feature | Category | Disposition | Tier / Reason |
|---|---|---|---|
| Audio/subtitle track selection actually applied | Player core | **Adopt-now** | P0 (FR-AUD-001); dead wiring |
| Media notification/lockscreen controls/background survival | Player core | **Adopt-now** | P0 (19👍 precedent [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |
| Real queue + Up-next + auto-advance + play modes | Player core | **Adopt-now** | P0 (FR-CTRL-017) |
| Subtitle/audio delay + sleep timer | Player core | **Adopt-now** | P0/P1 (SCR-010e) |
| External subtitle sidecar loading/management/mojibake fix | Subtitles | **Adopt-now** | P1 (FR-SUB-001…009); flagship floor |
| HW↔SW decoder override + fallback honesty labels | Player core | **Adopt-now** | P1 (FR-PLAY-002/003); brand pillar 3 |
| Onboarding/permission education/limited mode | Library | **Adopt-now** | P1 (SCR-001/012) |
| Open-with/ACTION_VIEW/share handoff | Library | **Adopt-now** | P1 (FR-LIB-020, MUST·R1) |
| Delete-from-library + details view | Library | **Adopt-now** | P1 (SCR-009/018) |
| Settings root + per-video memory | Library/settings | **Adopt-now** | P1 |
| PiP | Platform | **Adopt-now** | P2 (7👍 [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |
| Scrub thumbnail preview (FrameExtractor) | Player HUD | **Adopt-now** | P2; R1-safe deterministic primitive [81](https://android-developers.googleblog.com/2025/12/media3-190-whats-new.html) |
| Gesture depth: long-press 2×, pinch-pan, surface-drag scrub | Player HUD | **Adopt-now** | P2 (9👍 precedent [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |
| Folder browser + multi-select | Library | **Adopt-now** | P1 (SCR-004/005; VLC re-added multi-delete [2](https://play.google.com/store/apps/details?id=org.videolan.vlc)) |
| Stats-overlay/for-nerds HUD | Player/diagnostics | **Adopt-now** | P1→P2 (SCR-010e/FR-DIAG-001; Infuse parity [28](https://firecore.com/releases)) |
| Chapters (MKV) navigation incl. on-bar markers | Player core | **Adopt-later** | P2 (Now: parse/display; Next: on-bar + gestures #78/#155 [78](https://github.com/mpvRex/REX-Player/issues/78), [80](https://github.com/jellyfin/jellyfin-android/issues/155)) |
| Skip silence (amplitude heuristic) | Delighter | **Adopt-later** | P2 (8👍 shipped precedent [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc), [76](https://github.com/vantezzen/skip-silence)) |
| Intro/outro skip — manual segments | Delighter | **Adopt-later** | P2; Nova's segment skip is rare [16](https://github.com/nova-video-player/aos-AVP/releases) |
| Intro/outro skip — automated frame-difference heuristics | Delighter | **Adopt-later** | P2 *experimental*, label first; validity TBD (research gap in notes) |
| AI scene detection/anything | Delighter/tools | **Adopt-later ⚠** | R3 (DEC-007 AI non-goal) |
| Watch-state badges/"NEW" labels/Continue rail/history UI | Library IA | **Adopt-now** | P2 (Infuse IA [28](https://firecore.com/releases); Next #687/#822/#1141 [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |
| Subtitle sync wizard + styling preview + honor-original toggle | Subtitles | **Adopt-now** | P2 (flagship pillar; fixes Plex/Infuse pain [73](https://www.reddit.com/r/PleX/comments/1gcfe6x/can_anyone_explain_the_subtitle_offset_once_and/), [72](https://www.reddit.com/r/infusevideoplayer/comments/1x4ym5c/infuse_857_now_available/)) |
| libass rendering path (styled ASS fidelity) | Subtitles | **Adopt-later ⚠** | R2 gated by RQ-06/ADR-003 — integration plan only if native bar fails |
| Dual/secondary subtitles | Subtitles | **Adopt-later** | R2, in the libass lane (mpv-only today [20](https://github.com/mpv-android/mpv-android)) |
| Per-feature-economy "Aptura Pro" unlock | Monetization | **Adopt-later ⚠** | R1.x option B (Infuse pattern [28](https://firecore.com/releases)); never gate basics |
| Donations channel | Monetization | **Adopt-later** | Low-cost, precedent Just/VLC [13](https://github.com/moneytoo/Player), [2](https://play.google.com/store/apps/details?id=org.videolan.vlc) |
| Refresh-rate matching + frame adaptation | Engine/platform | **Adopt-later** | P2 kill-switch (FR-PLAY-012); Just-only trench [13](https://github.com/moneytoo/Player) |
| Tunneled playback for AFR | Engine/platform | **Adopt-later** | P3; device-dependent (Just precedent [13](https://github.com/moneytoo/Player)) |
| OTG/USB/flash-drive playback via SAF | Library | **Adopt-later** | P2–P3 (white space §7; peers do SAF [9](https://github.com/anilbeesetti/nextplayer), [13](https://github.com/moneytoo/Player)) |
| Hidden-folder exclusion list, folder counts, duplicate detection | Library | **Adopt-later** | P2 whitespace (§4.4) — no researched player ships them |
| Private folder (PIN) | Library | **Adopt-later** | P3; PLAYit/Nova precedent [3](https://play.google.com/store/apps/details?id=com.playit.videoplayer), [4](https://play.google.com/store/apps/details?id=org.courville.nova) |
| Private playback (suppress history) | Library | **Adopt-later** | P3 (Nova model [4](https://play.google.com/store/apps/details?id=org.courville.nova)) |
| Volume boost (≤200% + clip honesty) | Audio | **Adopt-later** | P2–P3 (Just/Nova precedent; mpv demand [24](https://github.com/mpv-player/mpv/discussions/14257)) |
| Night mode (dynamic volume compression) | Audio | **Adopt-later** | P2 (MX/Nova precedent [1](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad), [4](https://play.google.com/store/apps/details?id=org.courville.nova)) |
| Pause on headphone disconnect + routing notes | Audio policy | **Adopt-now** | P1 audio family (Just precedent [13](https://github.com/moneytoo/Player)) |
| Post-playback actions sheet | Player ended-state | **Adopt-later** | P3 (folded into FR-CTRL-017 ENDED design; Just precedent) |
| Keep screen on while paused | Micro-policy | **Adopt-later** | P3 micro (Next #587 class [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |
| Buffer tuning UI | Advanced | **Adopt-later** | P3 (Next #154 class [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |
| HDR/DV badges + policy | Engine honesty | **Adopt-now** | P1–P2 (FR-PLAY-013; Next HDR tags #1260 [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |
| Ambient glow in black bars | Delight | **Adopt-later** | P3 niche (Infuse 8.3.3 [28](https://firecore.com/releases)) |
| VVC/AV1 (dav1d) codec sweep | Engine | **Adopt-now** | P2 via Media3 1.9 upgrade [81](https://android-developers.googleblog.com/2025/12/media3-190-whats-new.html) |
| Danmaku-style overlays | Niche | **Reject** | Niche + dependency cost; revisit only with persona demand |
| Widgets/app shortcuts/deep links | Surface | **Adopt-later** | P3 (after session/notification stack) |
| Home-screen widget | Surface | **Adopt-later** | P3; in white space but low UV before notification layer |
| Light/dynamic theme engine | Surface | **Adopt-later** | P3 (dark-locked is fine for v1 [companion §12.3]) |
| Localization (multi-language + RTL) | Platform | **Adopt-now** | P2 (hardcoded English unacceptable at scale; regional evidence §5.6) |
| Android TV / leanback build | Distribution | **Adopt-later ⚠** | R2 (TV non-goal; 13👍 demand hedge [11](https://github.com/anilbeesetti/nextplayer/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |
| Network sources: SMB/FTP/SFTP/WebDAV/UPnP/NFS | Distribution | **Adopt-later ⚠** | R2+ ADR (VLC/Nova/Next all ship it [2](https://play.google.com/store/apps/details?id=org.videolan.vlc), [4](https://play.google.com/store/apps/details?id=org.courville.nova), [9](https://github.com/anilbeesetti/nextplayer) — but network is the core non-goal) |
| HTTP/HLS/DASH/RTSP stream playback | Distribution | **Adopt-later ⚠** | R2+ ADR (URL-streaming demand 14👍 [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)) |
| Casting (Chromecast/AirPlay) | Distribution | **Adopt-later ⚠** | R3 ADR |
| Online subtitle download (OpenSubtitles-style) | Subtitles | **Adopt-later ⚠** | R2 ADR w/ explicit user-handoff UX; peers also defer [9](https://github.com/anilbeesetti/nextplayer) |
| Open URL entry (mpv's "Open URL only" pattern) | Distribution | **Adopt-later ⚠** | R2; only with network ADR [20](https://github.com/mpv-android/mpv-android) |
| Destructive file ops (delete/rename/move files) | Library | **Adopt-later ⚠** | R2 ADR (R1 non-destructive posture; SAF + SCR-018) |
| Multi-file destructive ops | Library | **Adopt-later ⚠** | R2 (follows file-op gate) |
| Wi-Fi device-to-device transfer | Distribution | **Adopt-later ⚠** | R3 (network non-goal) |
| Remote-access web UI (VLC-style OTP) | Distribution | **Reject** | [OUT] network + server-side attack surface in a zero-INTERNET brand |
| Trakt/NFO-scraping/online posters | Library | **Adopt-later ⚠** | R3 ADR (network); *local NFO only* variant = P3 in-boundary if personas demand |
| Torrent/magnet streaming; YT download; MP3 extraction | Distribution/tools | **Reject** | Explicit non-goals; legal/brand anti-gradient [30](https://www.kmplayer.com/home) |
| Equalizer/bass-boost/audio-FX suite | Audio | **Reject** | Explicit R1 non-goal; maintenance liability, not a moat [2](https://play.google.com/store/apps/details?id=org.videolan.vlc) |
| Video cutter/trim/transcode/editor | Tools | **Reject** | Explicit non-goal (video editing); feature-for-feature creep |
| Game hall / AI suite / "super-app" attachment | Tools | **Reject** | Strategic anti-gradient; the entire §4 archetypes evidence |
| MX-Share-style extras requesting camera/location | Tools | **Reject** | Permission anti-pattern; contradicts DEC-012 |
| Ads in any form | Monetization | **Reject** | Permanent non-posture (DEC-012; the market's biggest complaint axis) |
| "Plays everything" claim language | Marketing | **Reject** | Never-claim; codec-honesty positioning replaces it |
| mpv/libmpv engine swap | Engine | **Reject** | DEC-003 conformance; cherry-pick features (dual subs/styling) into Media3+libass lane instead [23](https://mpv.io/manual/master/) |
| Music-player add-on / local audio as sources | Scope | **Reject** | Video-only posture (also Just's wontfix #55 precedent [14](https://github.com/moneytoo/Player/issues?q=is%3Aissue+sort%3Areactions-%2B1-desc)); ties to READ_MEDIA_AUDIO justify-or-drop |
| Kids lock / parental controls | Scope | **Adopt-later** | No ADR needed (in-boundary) but persona-gated (companion §16 guard) |
| 3D SBS/TB playback | Niche | **Adopt-later** | P3 niche (Nova precedent [4](https://play.google.com/store/apps/details?id=org.courville.nova)) |
| AC3/DTS passthrough modes | Audio/TV | **Adopt-later ⚠** | Engine work + TV-orientation; after audio-ext lane |
| F-Droid/GPL channel join | Distribution | **Adopt-later ⚠** | R1 posture keeps it off; *attribute language* reused now [96](https://apt.izzysoft.de/fdroid/index.php/list/page/110?limit=25;repo=main) |
| Photo-picker-based ingestion | Library | **Adopt-later** | Deliberate option for privacy-max posture; media-library scan remains primary [92](https://medium.com/androiddevelopers/permissionless-is-the-future-of-storage-on-android-3fbceeb3d70a) |
| Table-stakes items 13–14 (TV build, network sources) | — | **[OUT of boundary]** | Remain table stakes for the market as a whole (§4.1) while sitting outside Aptura's R1 boundary |

## Appendix C — Open questions for the product owner

Decision-ready; each names the decision and its default if left unanswered:

1. **LGPL/`aac` (blocks R-16):** put the precise question to counsel — may an LGPL-2.1 FFmpeg audio extension ship with AAC decode enabled, or must AAC stay excluded (hard-fail)? *Default if unanswered by launch: ship without the audio extension, keep honest codec-state labels (per ADR-010).*
2. **Destructive file ops (G-02/R2):** confirm R1 ships library-level remove only, with file delete/rename/move entering R2 under an ADR and SCR-018 confirmations. Also: is remove-from-library undoable (snackbar) or final? *Default: yes to the posture; undoable remove.*
3. **Monetization posture (G-29):** choose the R1.x lane — per-feature Aptura Pro (option B, Infuse-style), donations, or no monetization — before storefront copy freezes. Ads remain permanently banned either way. *Default: defer; revisit only after P1 ships.*
4. **`READ_MEDIA_AUDIO` (spec backlog item 16):** justify or drop. If dropped, remove the declaration and adjust any audio-related copy; if kept, define what audio means in a video-first library. *Default: drop for R1 (video-only posture).*
5. **Subtitle fidelity endgame (G-18/RQ-06/ADR-003):** if RQ-06 returns FAIL against the native Media3 bar, approve a costed libass integration plan (R2 spend) and the R1 fallback messaging now, so the flagship lane never wobbles mid-train. *Default: rule as spec'd — native now, libass-gated.*
6. **Skip-intro approach (G-08/R-22):** approve the non-AI path (manual segments now; frame-difference heuristic labeled experimental) as the R1-bounded answer, with true automation deferred to R3 under DEC-007. *Default: yes; label experimental until heuristics validate.*
7. **targetSdk timing (G-25):** adopt targetSdk 36 + Media3 1.9 in the Next train (recommended: the platform train precedes session/HUD rebuilds), or freeze targetSdk 35 for the launch train and recheck only. *Default: adopt in Next (R-26), recheck FGS/orientation deltas explicitly.*
8. **Diagnostics stance (G-23/RSK-010):** approve the on-device diagnostics UI as the formal replacement for telemetry — what questions must it answer post-crash, and what redaction level is acceptable in user-initiated export? *Default: redacted-by-default export, never automatic.*

**Closing note.** Two takeaways sharpen the plan. First, Aptura's biggest sin this cycle is not missing features but *lying states* — every P0 fix converts an existing component into an honest one, which is why the cheapest week of work is also the most valuable. Second, the category is sorting itself into bloat and honesty camps precisely as Aptura's owners chose their boundary; the research shows the honest camp wins only when it is *finished* — Just Player and VLC both lose users on unfinished UX, not missing ideals. Ship the promises the spec already made, then spend the same discipline on the four pillars of the honest player, and the roadmap above is little more than the app's own documents, finally sequenced.


