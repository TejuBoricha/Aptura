# 06 — UX, Screen, and State Specification

**Version:** 0.4 (draft; v0.4 aligns the auto-hide default to Media3's 5 s and the wireframe seek steps to the symmetric ±10 s default; v0.3 sharpened SCR-010e decoder copy to the industry-settled labelling pattern — Auto/Hardware/Software each with a when-to-use description; v0.2 added SCR-010f/g/h and the Ended/Compatibility player states)
**Date:** 2026-10-19
**Status:** Draft. Text wireframes and tables only — **no visual mockups exist in this spec phase**; a designer will translate this into visuals during the build phase without changing structure. Every screen maps to FR requirements (doc 04) and inherits NFR-A11Y (doc 05). Claims and copy obey doc 07 §13 (no forbidden phrasing).
**Consumes:** 03 (journeys), 04 (FRs), 05 (NFRs), 07 (claims/messaging), 09 (architecture states).
**Feeds:** 12 (UI test matrix), 15 (store screenshots), 18 (traceability).

---

## 1. Design principles (tied to product principles)

1. **Calm and reachable:** controls where thumbs live; nothing important in top corners on tall phones; no decorative clutter (DH-5 as execution quality — doc 02 §8).
2. **Honest surfaces:** every state shows what is true — scanning vs complete, fallback decode vs hardware, missing file vs gone, unsupported profile vs bug (FR-PLAY-004, doc 07 tiers).
3. **No dead ends:** every error/empty state offers 1–3 concrete actions (FR-LIB-019).
4. **Player first:** tap-to-play is never more than 2 taps from any list; the player itself is a calm, uncluttered surface.
5. **Accessible by default:** TalkBack, 200% font scale, contrast, reduce-motion, non-touch (NFR-A11Y).
6. **No dark patterns:** no nags, no fake urgency; destructive actions are explicit and neutral-toned.

---

## 2. Information architecture

```
Onboarding (first run only)
Library
├── Home (Continue watching • Recently added • All videos shortcut)
├── Videos (browse all; sort/filter)
├── Folders (tree → folder detail)
├── Playlists (list → detail)
└── Search (+ recent searches)
Media details (bottom sheet from any list/player)
Player (fullscreen; modal over everything)
├── Controls overlay
├── Speed sheet
├── Subtitle panel (track, style, sync, encoding)
├── Audio panel (track, delay)
├── Advanced sheet (decode override, stats overlay toggle, A-B, sleep timer)
└── Error state
Settings
├── Playback
├── Controls & Gestures
├── Subtitles
├── Audio
├── Library & Scanning
├── Privacy
├── Storage & Cache
├── Diagnostics
└── Help & About (error-code lookup, licenses, privacy page, version)
```

Navigation model: single-activity; bottom navigation with **Library / Folders / Playlists / Search** (Settings via top-level affordance); player always fullscreen modal; details as bottom sheet. Tablet: two-pane (list ↔ detail) where stated; effects none differently.

---

## 3. Screen inventory (SCR IDs)

| ID | Screen | Purpose | Entry points | Key elements | FR anchors |
|---|---|---|---|---|---|
| SCR-001 | Onboarding & permissions | Value intro, privacy statement, file-access setup | First run; Settings replay | 3 concise pages max; permission rationale inline; "Continue without access" path | FR-LIB-001/002 |
| SCR-002 | Library home | Resume-fast surface | App open (default tab) | Continue-watching shelf, recently added, all-videos entry | FR-LIB-010/014 |
| SCR-003 | Videos browser | Full library | Nav tab; empty-state CTA | Grid/list toggle, sort/filter sheet, item tiles | FR-LIB-010/011/012 |
| SCR-004 | Folders browser | Orientation by folder | Nav tab | Folder rows with counts, exclusions affordance | FR-LIB-005 |
| SCR-005 | Folder detail | Contents of one folder | Folders; deep links | Same tile system; breadcrumb-ish header | FR-LIB-010 |
| SCR-006 | Playlists list | Manage lists | Nav tab | Create playlist; count + duration | FR-LIB-017 |
| SCR-007 | Playlist detail | Play/manage playlist | Playlists | Reorder (drag), remove, play-all, missing-item notices | FR-LIB-017/018 |
| SCR-008 | Search | Find fast | Nav tab; player menu | Query field, incremental results, recent searches | FR-LIB-012 |
| SCR-009 | Media details | Truth about one item | Long-press tile; player menu | Metadata table, actions (play/resume, favorite, playlist, handoff), missing-state variant | FR-LIB-013/016 |
| SCR-010 | Player | Watch | Any playback trigger | Surface + controls overlay (SCR-010a–e); error variant (SCR-010x) | FR-PLAY-*, FR-CTRL-* |
| SCR-010a | Controls overlay | Transport/context | Tap in player | Play/pause, ±seek, scrubber+times, speed, lock, subtitle/audio buttons, more menu | FR-CTRL-001…008 |
| SCR-010b | Speed sheet | Rate control | Player controls | Presets + custom; pitch note where applicable | FR-CTRL-004 |
| SCR-010c | Subtitle panel | Everything subtitles | Player; Settings shortcut | Track list w/ labels, style controls, sync offset, encoding override, per-video reset | FR-SUB-001…009 |
| SCR-010d | Audio panel | Track/delay | Player | Track list, delay stepper, passthrough/effected labels (factual) | FR-AUD-001/003 |
| SCR-010e | Advanced sheet | Power tools | Player ⋮ | Decode override — **Auto / Hardware / Software, each with a when-to-use description** (never bare HW/SW tokens); stats overlay toggle, A-B repeat, sleep timer | FR-PLAY-003, FR-CTRL-009/010, FR-DIAG-001 |
| SCR-010f | Play-mode sheet | What happens next | Player overlay (mode chip); auto-advance prompt | Single/Folder/Queue/Repeat one/Repeat all, shuffle; auto-advance toggle + delay; visible in overlay | FR-CTRL-017 |
| SCR-010g | Up next / ended surface | End-of-item clarity | `ENDED` with a queue | Next item card, play-next, back-to-library, delete-from-library, **10 s cancellable countdown ring**; no countdown when no queue or after 2 h inactivity (Up next pre-selected) | FR-CTRL-017, FR-LIB-015 |
| SCR-010h | Compatibility-mode notice | Honest degradation | Fallback engaged | Dismissible chip "Playing in compatibility mode" + "Why?" sheet naming failed class and chosen path | FR-PLAY-003, FR-PLAY-004 |
| SCR-010x | Playback error state | Honest failure | Any playback failure | What happened / tried / actions; error code; copy diagnostics | FR-PLAY-004, FR-DIAG-002 |
| SCR-011 | Scan progress surface | Truthful scanning | First run; manual refresh | Progress summary, per-stage hint, cancel, "play while scanning" | FR-LIB-004/006 |
| SCR-012 | Limited mode banner surface | Access-denied clarity | Library when restricted | Banner + explainer sheet + Settings deep link | FR-LIB-003, FR-PLAT-006 |
| SCR-013 | Settings root + sections | Single home per setting | Nav; player shortcuts | Section list per IA; search-in-settings | FR-SET-001 |
| SCR-014 | Storage & cache | Housekeeping | Settings | Cache size, clear thumbs, rebuild index, reset scopes (media-safe copy) | FR-LIB-022, FR-SET-002 |
| SCR-015 | Diagnostics | Truth & reports | Settings; error state | Stats-overlay default, log controls, report flow, redaction preview | FR-DIAG-002…004 |
| SCR-016 | Help & About | Education & transparency | Settings | Top tasks, error-code lookup table, licenses/SBOM view, privacy page, version | FR-SET-007/008 |
| SCR-017 | Update notes / migration | Calm updates | After update | Plain-language changes; migration progress if any | FR-SET-005 |
| SCR-018 | Delete/reset confirmations | Prevent data loss | Destructive actions | Two-step confirm; scope named explicitly ("library index", never "files") | FR-SET-002, FR-LIB-022 |

*(SCR-018 is a pattern component with defined copy, listed here for auditability.)*

---

## 4. State catalogue

Every list screen must define these states (implementation via doc 09 observables):

| State | Behaviour | Notes |
|---|---|---|
| Loading (first) | Skeletons within 100 ms; never a blank flash | NFR-PERF-001 |
| Partial (scanning) | Results stream in; "Scanning… N found" chip; play-while-scanning allowed | FR-LIB-004, SCR-011 |
| Empty | Explains how content gets here (picker/open-with/scan) + action | FR-LIB-019 |
| Filtered-empty | "Nothing matches — clear filters" | FR-LIB-011 |
| Limited (no access) | Banner + explainer; open-with still works | FR-LIB-003/012 |
| Error (list) | DB/scan failure message + retry/rebuild affordance | FR-LIB-007 |
| Missing items | Tiles show missing state; never silently vanish | FR-LIB-018 |
| Player variants | PREPARING/BUFFERING/PLAYING/PAUSED/ENDED/ERROR + fallback notice + PiP | FR-PLAY-001, SCR-010x |
| Ended (with queue) | Stops on last frame; Up next card + **10 s cancellable countdown ring** (configurable); replay/next/delete all reachable; no countdown after 2 h inactivity (Up next pre-selected); never starts from background | FR-CTRL-017, SCR-010g |
| Compatibility mode | Persistent dismissible chip while a fallback decode path is active; "Why?" sheet on demand | FR-PLAY-003, SCR-010h |

Cross-cutting variants every screen must handle: RTL mirroring; 200% font scale; dark/light/system; landscape where allowed; tablet pane layout; TalkBack traversal.

---

## 5. Design system (tokens & components)

- **Grid:** 4 dp base; spacing scale 4/8/12/16/24/32; corner radius 12 dp cards, 8 dp chips; elevation via tonal surfaces (M3) not shadows (calm).
- **Color roles:** M3 role tokens; brand accent configurable at build; semantic: error red reserved for destructive/error; "scanning/limited/fallback" notices use neutral/tonal surfaces with icon + text (never color-only — NFR-A11Y-002). Dynamic color (Material You) MAY; brand palette fallback always present.
- **Type:** system-relative scale honoring font override up to 200%; player overlay labels separate from subtitle typography (subtitle engine sizing per FR-SUB-006, not bound to system font scale).
- **Motion:** ≤150 ms micro-transitions; auto-hide **5 s** default (Media3 `DEFAULT_SHOW_TIMEOUT_MS`; player-configurable per FR-CTRL-001); reduce-motion honored (no non-essential motion).
- **Iconography:** consistent outline set (Apache-2.0 sets only, doc 11); state icons: missing (dashed file), scanning (pulse), fallback-decode notice (chip in stats + one-time toast).
- **Tile spec:** 16:9 thumbnail (rotation-correct), duration chip bottom-right, resume progress line, badges: watched, missing, in-playlist count; **no HDR/"8K"/"HQ" badges** — capability badges only where doc 07 says validation exists (honesty rule).
- **Sheets:** bottom sheets for player panels (thumb-reachable); destructive confirmations centered dialogs.
- **Banners:** limited-mode, battery-saver thumbnails pause, SD removed — all dismissible with "why" link to Help.

---

## 6. Player UX specification

**Anatomy (SCR-010a), portrait & landscape:**
```
┌─────────────────────────────────────────────┐
│ ‹ Back            Title (truncated)         │  top bar (fade with overlay)
│                                             │
│              [surface]                      │
│                                             │
│        ⏪10s   ⏯   ⏩10s                    │  center transport cluster
│                                             │
│  00:12:04 ━━━━━●━━━━━━━━━ 01:32:10          │  scrubber (time labels both ends)
│  [Lock] [Subs] [Audio] [Speed] [⋮]          │  bottom action row
└─────────────────────────────────────────────┘
```
- Overlay auto-hides (configurable); single tap toggles; near-edge taps never hide (FR-CTRL-001).
- Lock mode: all controls hidden; first tap shows a one-line unlock hint (FR-CTRL-006).
- Subtitle placement: safe-area defaults (FR-SUB-006); user position/margins applied; never under system gesture zone.
- Brightness/volume/seek feedback overlays: large numerals, high contrast, top-third placement away from subtitles.
- Stats overlay (from SCR-010e): compact two-column readout — decode path, codecs, resolution/fps, bitrate (if known), dropped frames, A/V offset, HDR path (FR-DIAG-001).
- Error state (SCR-010x): pattern = **What happened** (plain) / **What we tried** (one line, only if fallback attempted) / **What you can do** (1–3 buttons) / code + copy-diagnostics. No blame-y phrasing, no "try again" loops that obviously fail; taxonomy text table lives in SCR-016.
- PiP: system controls; return restores overlay state; no custom PiP UI (FR-PLAT-001).
- End of item (SCR-010g): freeze on last frame (no auto-loop), show Up next card when a queue exists. **With a queue, the default is a 10 s cancellable countdown ring** (Immediate / 5 / 10 / 15 / 30 / 60 s configurable) — not an instant cut and not a dead stop; without a queue (single file from a file manager), stop with replay only. Replay, next, and delete-from-library must all be reachable on this surface — auto-advance must never be the only way forward. After **2 h without user interaction**, suppress the countdown but keep Up next pre-selected so one tap continues. Mode chip (Single/Folder/Repeat) always visible in the overlay so "why did it stop / why did it continue" is answerable without opening a menu (FR-CTRL-017).
- Compatibility notice (SCR-010h): a neutral chip, not an error colour — falling back is not a failure. "Why?" opens a short factual sheet (what failed, what is being used, what would help). Never modal, never blocks playback.

**Gesture map** (defaults; each disableable — FR-CTRL-005): single-tap = controls; double-tap left/right = ∓/± seek step; vertical swipe left = brightness (with honest system-vs-overlay labeling in settings); vertical swipe right = volume; horizontal swipe = seek preview; pinch = zoom; two-finger tap = play/pause (MAY); long-press = speed-up while held (MAY, off by default). Edge zones reserved for system gestures; conflict rules documented in help.

---

## 7. Copy & tone

- **Voice:** direct, calm, specific. Second person. Sentence case.
- **Banned patterns** (doc 07 §13): "plays everything", "best", "zero crashes", "instant", "supports all formats", any battery claim, "8K", "full Dolby Vision".
- **Error copy template:** `What happened → optional cause → specific actions → code`. Example: "This file uses a 10-bit profile this device doesn't decode. We tried software decoding. [Play video without sound? no —] [Open details] [Copy diagnostics] · code PLAY-UNSUPPORTED-PROFILE." (final strings authored per taxonomy case in build phase).
- **Honesty rules:** fallback decode notice says *why* and *what it costs* (adds battery) once per session; scanning progress never fakes percentage beyond real work; missing files say "missing", not "deleted".
- **Privacy statements:** mirrored from doc 10 §4 only (no new promises); "no account, no ads, no tracking" allowed as factual statements tied to release verification.

---

## 8. Accessibility specification (per screen class)

| Concern | Approach |
|---|---|
| TalkBack | Rows announce: title, duration, progress, state badges; grids announce position ("3 of 42"); player controls announce state changes, not timers (no spam); subtitle cues are *not* read aloud (visual-only by design; audio track is the auditory channel) |
| Focus order | Player: back → transport → scrubber → actions (landscape left-right); sheets trap focus until dismissed |
| Live regions | Buffering/error transitions announced once; scan progress announced at coarse intervals only |
| Font scale | All screens usable at 200%; player overlay labels scale; subtitle size independent |
| Contrast/motion | Token-enforced AA; reduce-motion removes fades beyond 150 ms |
| Timeouts | No time-out traps; auto-hide adjustable; lock hint persists until action |
| Non-touch | Space/arrows/media keys mirror all core controls (FR-CTRL-014) |

---

## 9. Key flows (text wireflows)

1. **First run (JRN-01):** SCR-001 (3 pages) → permission rationale → OS dialog → [granted] SCR-011 scan starts → SCR-002 with streaming results; [denied] SCR-012 limited mode + picker/open-with paths.
2. **Play from library (JRN-02):** SCR-002 → tap tile → SCR-010 PREPARING → PLAYING (overlay auto-hides) → pause → resume banner on return via SCR-002 shelf.
3. **Subtitle rescue (JRN-03):** SCR-010c → wrong-charset? encoding override → live fix; offset stepper; style tweak with preview; per-video memory saved.
4. **Big library (JRN-04):** SCR-011 progress; user browses/plays during scan; "N new" chip on completion → filtered view.
5. **Failure (JRN-05):** SCR-010x pattern → "Copy diagnostics" → optionally Help lookup from code → alternatives (software decode override from SCR-010e where applicable).
6. **Permission revoked (JRN-06):** banner (SCR-012) + Settings deep link; auto-rescan on return without reinstall; history/favorites intact.
7. **Interruption/resume (JRN-07):** call → auto-pause → resume per setting; process death → relaunch restores position (never silently from 0).
8. **Update (JRN-08):** SCR-017 notes → migration chip if rebuilding → done silently; no data-loss scare copy.

---

## 10. Text wireframes (selected)

**Library home (SCR-002):**
```
┌────────────────────────────┐
│  Library            ⟳  🔍  │
│  Continue watching         │
│  [tile][tile][tile] →      │
│  Recently added            │
│  [tile][tile][tile] →      │
│  All videos (1 245)  →     │
│ ┌────────────────────────┐ │
│ │ Scanning… 312 found    │ │   ← chip appears only while real work runs
│ └────────────────────────┘ │
│ ⌂ Library ▸ Folders ▸ Lists ▸ Search │
└────────────────────────────┘
```

**Playback error (SCR-010x):**
```
┌────────────────────────────┐
│  ⚠ This file didn't play   │
│  Reason: 10-bit HEVC isn't │
│  decodable on this device. │
│  We tried software decode. │
│  [Try software decode again]│
│  [Open details] [Copy info]│
│  Code: PLAY-UNSUPPORTED-   │
│  PROFILE · Help            │
└────────────────────────────┘
```
*(Copy above is illustrative of structure and tone; final strings per taxonomy case.)*

---

## 11. Open questions

| # | Question | Owner |
|---|---|---|
| U1 | Bottom-nav vs drawer at tablet widths (two-pane feasibility test) | build phase design |
| U2 | Dynamic color (Material You) default on/off | brand decision (OD-6) |
| U3 | Player overlay micro-layout on small-landscape (≤5") | design |
| U4 | Whether speed-hold (long-press) ships in R1 or R2 | FR-CTRL-016 review |
| U5 | Exact empty-state illustrations (assets cost vs calm text-first default) | design |
| U6 | In-settings search depth (names only vs synonyms table maintenance) | FR-SET-001 impl |
