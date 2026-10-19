# 09 — Architecture, Data, and Backend Decision

**Version:** 0.2 (draft; design-level, no code; v0.2 adds queue/play-mode ownership and the `ENDED → next` transition)
**Date:** 2026-10-19
**Status:** Draft. This document specifies the intended architecture, module boundaries, data model, threading rules, and the **no-backend decision** (master prompt §22 #10/11/15). It contains no application code — only structures a build team can implement directly. All performance-relevant claims are design intentions validated later by RQ-spikes and doc 13.
**Consumes:** 05 (NFRs), 07 (compat tiers), 08 (ADRs), `research/platform.md`.
**Feeds:** 10 (threat model surface), 11 (dependency register), 12 (test seams), 14 (diagnostics), 18 (traceability).

---

## 1. Architectural principles

1. **Offline-first, network-free R1.** No network stack ships (ADR-011). The strongest privacy proof is the absence of the `INTERNET` permission — review at release gate (doc 10).
2. **Single engine, thin adapters.** Media3 is wrapped by one adapter surface so the engine-fallback plan (ADR-001 → libVLC) stays real without rewriting features.
3. **Derived vs user data separation.** Library records are derived (rebuildable); favorites/playlists/progress/prefs/overrides are **user data** (never lost to a rebuild — FR-LIB-007/018).
4. **Main thread is sacred.** No IO, parsing, or DB on main; ANR target ≤0.10% (NFR-REL-002).
5. **Everything observable.** Playback session, scan, and diagnostics expose observable state for UI and tests.
6. **No silent machinery.** Every background behavior (scan, thumbs, logs) is visible, bounded, cancellable, and battery-aware (NFR-RES-007).

---

## 2. System overview (text diagram)

```
┌────────────────────────────────────────────────────────────┐
│ UI layer (Compose; Material 3)                             │
│   Screens per doc 06; player surface via Media3 interop    │
└───────────────▲──────────────────────────────┬─────────────┘
                │ state (Flow)                 │ actions
┌───────────────┴──────────────────────────────▼─────────────┐
│ Orchestration: ViewModels / controllers                    │
│   library, player, settings, dialogs, pickers              │
└───────────────▲──────────────────────────────┬─────────────┘
                │ domain API                   │ intents
┌───────────────┴──────────────────────────────▼─────────────┐
│ Domain services                                            │
│  LibraryService   ScanService    PlaybackSession           │
│  SubtitleService  AudioService   SettingsService           │
│  DiagnosticsService (cross-cutting leaf)                   │
└──────▲────────────▲─────────────▲────────────▲─────────────┘
       │            │             │            │
┌──────┴─────┐ ┌────┴─────┐ ┌─────┴─────┐ ┌────┴───────────┐
│ Platform   │ │ Engine   │ │ Storage   │ │ System         │
│ data access│ │ adapter  │ │ & cache   │ │ integration    │
│ MediaStore │ │ (Media3) │ │ Room      │ │ FGS/MediaSession│
│ SAF/picker │ │ + ext    │ │ DataStore │ │ PiP, notifications│
└────────────┘ └──────────┘ │ thumbs    │ └────────────────┘
                            │ log files │
                            └───────────┘
```

**Dependency rules (enforced by module boundaries + lint):** UI → orchestration → domain → platform/storage. No upward or sibling-feature dependencies; `DiagnosticsService` is a leaf interface injectable everywhere; playback domain **must not** depend on UI or library-listing internals (only item handles).

---

## 3. Module map (responsibilities ↔ FR domains)

| Module | Owns | FR domains |
|---|---|---|
| `app` shell | Navigation host, DI wiring, theme, first-run gate | — |
| `library` | MediaStore/SAF enumeration, metadata extraction, folders, search index, reconciliation | FR-LIB-* |
| `scan` | Scan pipeline stages, checkpointing, scheduling (WorkManager), storage-event handling | FR-LIB-004…007, 021 |
| `playback` | Engine adapter, session state machine, decode-path policy, error assembly, custom controls state, **queue + play-mode orchestration** (resolves `ENDED` → next per FR-CTRL-017; owns mode persistence and the auto-advance guard) | FR-PLAY-*, FR-CTRL-* |
| `subtitles` | Discovery/matching, encoding detection, styling model, sync, per-video memory | FR-SUB-* |
| `audio` | Track model, passthrough policy, delay, focus/route glue | FR-AUD-* |
| `settings` | Schema, defaults, search metadata, reset scopes | FR-SET-* |
| `diagnostics` | Stats overlay model, error-code registry, redacted bundles, log buffer | FR-DIAG-* |
| `platform-integration` | PiP, MediaSession/notification, FGS lifecycle, wake policy, share/handoff | FR-PLAT-* |
| `design-system` | Tokens, components, accessibility primitives | doc 06 |
| `testing-kit` | Fakes (clock, storage, engine), fixtures loaders, idling | doc 12 |

**Engine adapter surface (one interface, two impls eventual):** prepare/play/pause/seek/set-surface/set-speed/track-enumeration-select/error-events/stats-snapshot. Fallback impl would be libVLC behind the same surface (ADR-001 revisit trigger).

---

## 4. Threading and concurrency model

| Work | Dispatcher / runner | Rules |
|---|---|---|
| UI rendering, state collection | Main | No IO; heavy lists only via lazy/precomputed data |
| DB queries (browse/search) | IO pool (Room default) | Paginated; FTS query ≤ budgets (NFR-PERF-006) |
| Metadata extraction | Default pool, bounded parallelism (≈min(4, cores−1)) `[E]` | Per-file timeout; cancellation-safe; never main |
| Thumbnail generation | Dedicated limited pool (2) + WorkManager for bulk | Battery/thermal gates (FR-LIB-009) |
| Scan orchestration | WorkManager unique work + foreground process while app visible | Checkpoint per batch; user-cancellable |
| Playback session | Media3 application looper (owns player) + our session actor on a single-threaded context | All player calls through the actor; no concurrent player mutation |
| Logging | Lock-free ring, flush batching | Redaction before write (doc 10) |

**Rules:** structured concurrency (scopes tied to UI/session lifecycles); every long task cancellable and idempotent; no lock held across suspends; `Dispatchers` injectable for tests (doc 12 seams).

---

## 5. Library data model

### 5.1 Core entities (Room; names indicative)

**`media_item`** — one playable file
`id PK · volume_id · media_store_id (nullable, unique-when-present) · saf_document_id (nullable) · uri · display_name · title (nullable) · folder_id FK · mime_class · size_bytes · duration_ms (nullable) · width/height · rotation · video_codec_class · audio_summary_json · subtitle_summary_json · date_added · date_modified · identity_hash · state (AVAILABLE/MISSING/GONE) · first_seen_at · last_seen_at`

**`folder`** — tree + exclusions
`id PK · volume_id · uri/path · parent_id · name · is_excluded (bool) · last_scanned_at`

**`progress`** — resume (1:1)
`media_item_id PK/FK · position_ms · duration_snapshot_ms · updated_at · completed (bool ≥95% rule)`

**`play_event`** — history feed
`id PK · media_item_id FK · occurred_at · kind (START/STOP/COMPLETE) · position_ms`

**`favorite`** — `media_item_id PK/FK · created_at`

**`playlist`, `playlist_item`** — `playlist(id, name, created_at, updated_at)`; `playlist_item(playlist_id, media_item_id, position, added_at)`

**Overrides (user data, per FR-CTRL-012):**
`subtitle_override(media_item_id, track_ref, encoding, offset_ms, style_json, updated_at)`
`audio_override(media_item_id, track_ref, delay_ms)`
`geometry_override(media_item_id, mode, zoom, pan_x, pan_y)`

**`scan_state`** — `id · scope · checkpoint_json · last_scan_at · last_result`

**Derived-vs-user classification:** `media_item`, `folder`, `scan_state` = derived (rebuildable); `progress`, `play_event`, `favorite`, `playlist*`, overrides = **user data** (protected through rebuilds; FR-LIB-007).

### 5.2 Identity and reconciliation (FR-LIB-018)

| Confidence | Key | Used for |
|---|---|---|
| Highest | MediaStore ID stable on same volume | Routine rescans; rename/move within volume |
| High | SAF document ID | SAF-sourced trees |
| Medium | `identity_hash` = size + duration bucket + basename-stem hash | Cross-volume moves, eject/insert cycles |
| Low | basename only | Ambiguity → item marked MISSING, progress preserved, **no silent merge** (collision UX = R2 relink screen; R1 shows "missing" honestly) |

Promotion/demotion of matches requires a user-visible rule table in doc 06 (details view); heuristic tuning = RQ-05 artifact.

### 5.3 Search index
FTS table (`media_item_fts`: title, display_name, folder name path) updated transactionally with item writes; query layer prefers FTS, falls back to LIKE for 1–2 char prefixes. Diacritics: normalized column `title_folded [E]`. Latency targets: NFR-PERF-006. **Re-baseline in RQ-11.**

### 5.4 Size, integrity, repair
- Estimate ≈0.5–2 KB/item incl. indexes `[E]` (re-baseline RQ-11); 50k items ≈ 25–100 MB.
- WAL mode; batched transactions (≈100 items) during scans.
- Integrity check on open-failure; corruption → FR-LIB-007 rebuild flow preserving user data via identity rules.

---

## 6. Scan pipeline (design)

```
trigger ─▶ S1 enumerate ─▶ S2 diff ─▶ S3 metadata ─▶ S4 persist ─▶ S5 thumbnails (deferred) ─▶ S6 done
 (first run, resume, manual, storage event, SAF change)   (bounded parallel)    (incremental txns)   (gated queue)
```

- **Triggers:** first run (FR-LIB-002/004), app foreground (throttled, FR-LIB-006), user refresh, `MediaStore` change signals where reliable, SD insert/remove (FR-LIB-021). No background scanning outside user-visible sessions in R1 (WorkManager continuation with constraints allowed for long scans).
- **Checkpointing:** `scan_state.checkpoint` per pipeline stage; process death resumes; cancel is safe; a cancelled scan never mutates user data.
- **Throttling:** metadata extraction concurrency caps; thumbnails deferred to charging/idle (FR-LIB-009); all work yields to active playback (NFR-RES-007).
- **Storage model:** MediaStore primary (per W2 policy — no `MANAGE_EXTERNAL_STORAGE`); SAF trees secondary; Photo Picker + open-with fallback mode (FR-PLAT-006 degraded mode shares this pipeline minus enumeration).
- **Deletion detection:** items absent in enumeration move to MISSING, then GONE after grace period `[E]` (policy in doc 06 copy; data retained per reconciliation rules).

---

## 7. Playback session model

### 7.1 State machine (engine-adapter observable)

```
IDLE ─prepare─▶ PREPARING ─ok─▶ READY/PAUSED ─play─▶ PLAYING ─┐
  ▲                │                                     │  │ seek/route/rotation: internal transitions
  │                └─fail─▶ ERROR(code, layer, actions) ◀─┘  │
  └─stop─────────── ENDED ◀──natural end── PLAYING ◀─────────┘
```
- **Single active session** in R1; playlist queues live in the orchestration layer over the same session.
- **`ENDED` is a decision point, not a terminal state** (FR-CTRL-017). The queue orchestrator — not the engine adapter — owns what happens next, so end-of-item policy stays testable without a decoder:
  `ENDED` → read play mode (Single / Folder / Queue / Repeat one / Repeat all) → resolve next item → `PREPARING` for the next item, or stay `ENDED` and surface Up next (SCR-010g). With a next item, the **default path is the countdown** (10 s, cancellable) — the orchestrator starts the timer, and `PREPARING` for the next item begins when it elapses.
  **Inactivity guard:** the orchestrator tracks last-user-interaction time; past **2 h**, it skips the countdown and holds `ENDED` with Up next pre-selected (FR-CTRL-017). This is a guard in the orchestrator, not a UI behaviour, so it holds even if the screen is not redrawn.
  Auto-advance requires a **foreground** session and is suppressed in background/lock (FR-PLAY-018, FR-CTRL-017); the countdown is cancellable and cancelling holds `ENDED` without tearing down the session.
  Advance MUST reuse the session actor (no teardown/rebuild) so the transition meets the auto-advance budget rather than paying cold-start cost.
- **Persistence triggers (resume):** start, pause, seek settle, 5 s cadence, stop/end, lifecycle events (FR-LIB-015, NFR-PERF-011). Item completion is written at `ENDED` **before** the next item is prepared, so a crash mid-transition cannot lose the completion of the item that just finished.
- **Lifecycle:** rotation/PiP/background/permission-change/SD-removal handled by the session actor; process death → snapshot (item handle + position + tracks + geometry) in SavedState + DB.

### 7.2 Decode path policy (engine adapter)
`hw decoder → platform software decoder (video) → passthrough (audio) → bundled FFmpeg audio decode (doc 11-cleared list) → taxonomy error` — at most one automatic escalation per failure class (FR-PLAY-003); manual override per session; every escalation logged as a class-only event for stats overlay (FR-DIAG-001).

### 7.3 Error assembly
One `PlaybackError { code, layer(ENGINE/PLATFORM/IO/PERMISSION/CONTENT), tried[], recoverable, diagRef }` built at the adapter boundary; `code` ∈ the doc 07 / FR-PLAY-004 registry; UI copy lookup happens in doc 06 layer; redaction rules in doc 10 before any log/report.

### 7.4 Platform integration points
- **FGS/MediaSession:** session actor drives start/stop of `mediaPlayback` FGS only in playing states (FR-PLAT-003); notification from MediaSession (FR-PLAT-002).
- **PiP:** surface/state bridge (FR-PLAT-001); auto-enter on 31+.
- **Wake:** screen-on flag scoped to visible playback (FR-PLAT-005).

---

## 8. Subtitle and audio pipelines (integration view)

**Subtitles:** discovery (embedded enumeration via engine + sidecar matcher per naming rules) → candidate ranking (language prefs > basename > heuristic) → user chooser on ambiguity → parse (engine path; encoding detector ours) → render (ADR-003 path) with overrides from `subtitle_override` → sync offset applied at render layer (FR-SUB-004) → per-video memory write-back.
Malformed-file defenses: parser caps + watchdog (FR-SUB-007); failures degrade to notices, never block playback.

**Audio:** enumerate tracks (labels) → default per policy (FR-AUD-001) → route/passthrough decision (device+route capabilities) → optional bundled decode per doc 11 list → delay applied → focus/route/interruption controllers (FR-AUD-004/005) → per-video memory for track/delay.

---

## 9. Settings, diagnostics, and storage of state

- **DataStore (settings):** versioned schema; sections mirror doc 04 FR-SET-001; unknown/legacy keys ignored with report (forward compat).
- **Diagnostics:** log files (ring, ≤10 MB/7 days) + error-code registry + redacted bundle builder (FR-DIAG-002/003); stats overlay reads engine snapshots + session counters.
- **Caches:** thumbnail files in app cache with LRU index table (`thumb_entry(media_item_id, variant, file, size_bytes, last_used_at)`); cap default 150 MB; clearing cache never touches user data (FR-SET-002/FR-LIB-022).

---

## 10. Backup, migration, and portability

| Topic | Decision (R1) | Rationale |
|---|---|---|
| Android auto-backup / device transfer | **Disabled** (`allowBackup=false`) — nothing in cloud/D2D | Local-first promise (NFR-PRIV-001); avoids stale library restores referencing files that don't exist on the new device; revisit post-R1 with user-data-only export |
| DB migrations | Room migrations, versioned, forward-only, tested on upgrade matrix | NFR-OPS-004; rollback = app-level reset + rescan (derived data safe; user data loss only via explicit reset) |
| Settings migrations | Keyed, additive; defaults for new keys | FR-SET-002 |
| User-data export/import | **MAY · R2** (FR-SET-003) — settings-only, no media paths | Privacy + scope |
| Reinstall behavior | Fresh scan; no residual data | Consistent with no-account/no-cloud design |

---

## 11. Backend decision: none (documented rationale, master prompt §22 #15)

**Decision:** R1 ships with **no backend, no accounts, no server components** (DEC-004/008; ADR-011).

| A backend would enable | Why it's out of R1 |
|---|---|
| Crash/usage telemetry | DEC-008 (opt-in only, later phase); Play vitals cover ops basics; RQ-10 tests diagnosability without it |
| Subtitle download/services | FR-SUB-010 boundary (legal + privacy review first) |
| Cloud sync of progress/prefs | Different product value prop; account machinery; R3 candidate at earliest |
| Remote config/kill switches | ADR-012; containment via staged rollout + hotfix |
| A/B experiments | No traffic to experiment on; premature for R1 |
| Server-side library parsing | Local capability is the product; cost/Privacy |

**Consequences we accept:** no remote metrics beyond store consoles; feature decisions ship via app updates; support flows rely on user-initiated redacted reports (FR-DIAG-004). **Trigger to revisit:** doc 14 support-burden evidence or an R2 network feature (FR-NET-001 gate).

---

## 12. Performance-critical paths (design commitments)

| Path | Design commitment | Guarded by |
|---|---|---|
| Cold start | Lazy singletons; library render before full scan; no blocking DI | NFR-PERF-001 |
| Browse scroll | Paging + precomputed rows; thumbnails progressive; no main-thread decoding | NFR-PERF-007 |
| Search | FTS + debounce + cancellation of stale queries | NFR-PERF-006 |
| Scan | Batched writes, bounded parallelism, checkpointing, battery gates | NFR-PERF-005, NFR-RES-007 |
| Playback entry | Engine prepared before surface swap on nav; no scan/DB work on the tap path | NFR-PERF-003 |
| Rotation | Session survives surface recreation; player not restarted | NFR-PERF-012 |

---

## 13. Test seams (what doc 12 will exploit)

Clock injection; storage abstraction (fake MediaStore/SAF); engine adapter fake for session-state tests; dispatcher injection; deterministic scan fixtures; corruption injectors (DB, thumbnails, subtitles); lifecycle drivers (process-death simulation).

## 14. Open questions

| # | Question | Owner |
|---|---|---|
| A1 | `identity_hash` parameters tuned on real corpora (RQ-05) — collision/recall tradeoff | 12/13 |
| A2 | FTS4 vs FTS5-with-triggers tradeoffs on Room; folded vs not | RQ-11 |
| A3 | MISSING→GONE grace period length + copy (doc 06) | 06 |
| A4 | Auto-backup revisit after R1 (user-data-only export design) | 15/16 |
| A5 | Whether play_queue persistence is needed for R1 (currently in-memory only). FR-CTRL-017 keeps this viable: **folder/queue modes are re-derivable at play time** from the library + current sort, so only playlist-originated queues have a durable source (`playlist_item`); a session queue need not survive process death | RQ-07 |
| A6 | SAF provider (DocumentsUI/third-party) performance variance — expand RQ-05 coverage | 12 |
