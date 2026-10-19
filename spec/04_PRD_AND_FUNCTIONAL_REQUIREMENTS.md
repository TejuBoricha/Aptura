# 04 — PRD and Functional Requirements Catalogue

**Version:** 0.5 (Part E: **Q13 closed against industry practice** — FR-PLAY-003 decoder controls now specify the settled labelling pattern (technical name + when-to-use description, `Auto` as default, a global setting *and* a per-item override) and explicitly forbid bare HW/SW tokens as the only label; sourced in `research/decoder_label_practice.md`. Part D retained: Q12 auto-advance default ON with a 10 s cancellable countdown, 2 h inactivity guard, stop-only when no queue)
**Date:** 2026-10-19
**Status:** Draft. All requirements follow the convention below; priority/release tags are frozen per DEC-001…010 in doc 00 §3.3. Acceptance criteria are written to be testable; test IDs are placeholders finalized in doc 12.
**Consumes:** 01 (scope), 03 (personas/journeys/KPIs), doc 00 registers.
**Feeds:** 05 (NFRs), 06 (screens; every requirement must map to UI), 12 (tests), 18 (traceability).

---

## 0. Conventions

### 0.1 Requirement record template
Every requirement uses this compact record (all fields mandated by master prompt §7):

- **ID — Title**, plus `MUST/SHOULD/MAY`, release (`R1/R1.x/R2/R3`), priority (`P0` launch-critical, `P1` important-at-launch, `P2` next, `P3` opportunistic).
- **Statement** — normative behavior.
- **Need/rationale** — link to persona/JTBD/journey/differentiator.
- **Preconditions / permissions / configuration.**
- **Workflow + alternate workflows**; **states & persistence**.
- **Error & recovery behavior**; **offline/interruption/restart behavior**.
- **Platform differences.**
- **Dependencies, risks, constraints.**
- **Security/privacy/accessibility/performance implications.**
- **Acceptance criteria** (Given/When/Then, objective).
- **Tests** (placeholder TST IDs) & **confidence** (`High/Med/Low`, with reason).

### 0.2 Domain map (ID → area → part)

| Domain | Area | In doc |
|---|---|---|
| FR-LIB-* | Library, discovery, file access (§7.1) | Part A ✅ |
| FR-PLAY-* | Playback engine, compatibility behavior (§7.2) | Part A ✅ |
| FR-CTRL-* | Controls & player interaction (§7.3) | Part B ✅ |
| FR-SUB-* | Subtitle system (§7.4) | Part B ✅ |
| FR-AUD-* | Audio (§7.5) | Part B ✅ |
| FR-PLAT-* | Platform integration (open-with, PiP, media session) | Part B ✅ |
| FR-SET-* | Settings, help, recovery (§7.8) | Part B ✅ |
| FR-DIAG-* | Diagnostics & error reporting surfaces (§17 hook) | Part B ✅ |
| FR-NET-* | Network/streaming (§7.6) | Boundary only (R2 candidates) |
| FR-PERS-* | Personalization/intelligence (§7.7) | Boundary only (R3, evidence-gated) |

### 0.3 Global requirement rules
1. **No silent capability claims:** any user-facing capability statement must match doc 07's compatibility categories at ship time (DEC-010).
2. **Graceful degradation first:** every FR with a failure mode references the failure-case (FC-*) it must survive (registers in doc 17; full FC catalogue in doc 18 audit). 
3. **Data minimization:** no FR may introduce telemetry without an approved EV-* event in doc 14.
4. **Accessibility applies to every FR:** all interactive requirements inherit NFR-A11Y-* (doc 05); per-FR notes only call out specifics.
5. **Local-first default:** no FR may require network for its core path in R1.

---

## 1. FR-LIB — Library, discovery, and file access

#### FR-LIB-001 — First-run experience without account
**MUST · R1 · P0**
**Statement:** On first launch, the app MUST present a concise value/`privacy` introduction, require **no** account/sign-in/network, and route the user to file-access setup in ≤2 taps.
**Need/rationale:** JRN-01 activation; PER-3 trust; DEC-004.
**Preconditions:** None. **Alternate:** If launched via "open with", the app MUST skip onboarding and play the file directly while surfacing library setup as a dismissible suggestion.
**States/persistence:** `onboarding_completed` flag; re-shown sections accessible from Settings/Help.
**Errors/recovery:** Kill during onboarding MUST NOT corrupt state; restarts at last step.
**Platform:** Android only in R1.
**Deps/risks:** Onboarding flow decided in doc 06 (SCR-001).
**Privacy/a11y/perf:** Zero network calls during onboarding; full screen-reader compatibility; instant first paint.
**AC:** Given a fresh install with airplane mode ON, When the user completes onboarding and denies permissions, Then the app remains navigable in limited mode with an explanation and no crash or dead end.
**Tests:** TST-ONB-01…03. **Confidence:** High.

#### FR-LIB-002 — Permission education and request flow
**MUST · R1 · P0**
**Statement:** Before the OS permission dialog, the app MUST explain in plain language what is requested and why (browse/play your videos), request only the minimum scope (e.g., `READ_MEDIA_VIDEO` on Android 13+; legacy `READ_EXTERNAL_STORAGE` pre-13), and honor any chosen alternative access path (folder-scoped SAF trees) without nagging.
**Need/rationale:** Principle 3; JRN-06; PER-4.
**Workflow:** rationale screen → OS dialog → granted (bootstrap JRN-01) / denied (limited mode FR-LIB-003). **Alternate:** SAF folder selection offered when user declines broad access where technically supported (see doc 07/09 for the exact policy per OS level — pending W2).
**Error/recovery:** "Don't ask again" detected; deep-link to app settings with per-permission guidance.
**Platform:** Android 13+ onward differs from ≤12; Android 14 "selected photos/videos" partial-grant behavior MUST be tested (FC-002).
**Deps/risks:** Play policy compliance for file access (doc 10, DEP register).
**AC:** Given a fresh install, When permissions are denied once, Then the app shows limited mode and a Settings deep-link, and never shows an unskippable permission wall.
**Tests:** TST-PRM-01…05. **Confidence:** High (API surface stable), Med on partial-grant UX details (device-behavior variance).

#### FR-LIB-003 — Limited mode (denied/revoked access)
**MUST · R1 · P0**
**Statement:** With no or partial video access, the app MUST remain fully functional for single-file playback (open-with, picker), show what library features are unavailable and why, and auto-recover (rescan) when access is later granted or revoked **while the app runs**.
**Need/rationale:** JRN-06; FC-001/FC-002.
**States/persistence:** Limited-mode banner state; library hidden but data retained.
**Error/recovery:** Mid-session revocation MUST degrade gracefully (reading already-open files continues where the OS allows; new reads fail with specific error).
**AC:** Given limited mode, When the user grants permission via Settings and returns, Then the library bootstraps within one foreground session without reinstall and without losing favorites/progress.
**Tests:** TST-PRM-06…09. **Confidence:** High.

#### FR-LIB-004 — Library bootstrap scan
**MUST · R1 · P0**
**Statement:** After access is granted, the app MUST discover accessible video files, persist library records incrementally, and present progress with cancellation; already-found items MUST be playable while scanning continues.
**Need/rationale:** JRN-01/JRN-04; DH-4; KPI-ACT-01.
**Preconditions:** Access granted (any mode). **Workflow:** queued scan → MediaStore enumeration + optional SAF tree walk → metadata extraction (FR-LIB-008) → thumbnails deferred (FR-LIB-009) → completion notice.
**States/persistence:** Scan checkpoint persisted; process death resumes incrementally, never restarting from zero silently.
**Errors/recovery:** Unreadable entries skipped with local log; corrupt MediaStore rows tolerated; scan cancel ≠ data loss.
**Platform:** Android scanning APIs differ ≤12 vs 13+ (permission scopes); SAF trees can be slow (RQ-05).
**Deps/risks:** RQ-05 perf; doc 09 indexing architecture; NFR-PERF scan targets.
**Privacy/a11y/perf:** No filenames leave device; progress UI must be screen-reader updated at sensible intervals (not each file); scan MUST NOT block UI thread or main playback. The progress surface MUST state **what phase is running** (discovering / reading metadata / thumbnails / cleaning up) and **how many items remain**, not a bare indeterminate spinner (prior art: Nova's scan-phase badge, doc 02A §7.2).
**AC:** Given a 10,000-file test corpus on a mid-tier device, When bootstrap runs, Then the UI remains responsive (60 fps browse) and the scan completes within the doc 05 budget without ANR.
**Tests:** TST-SCAN-01…06. **Confidence:** Med (perf targets pending bench; architecture settled in doc 09).

#### FR-LIB-005 — Scan locations & exclusions
**MUST · R1 · P1**
**Statement:** Users MUST be able to include/exclude specific folders (including hidden/private folders) from the library, with the exclusion list respected by all scans and search.
**Need/rationale:** §7.1; PER-2 organization; PER-3 privacy.
**States/persistence:** Exclusion list persisted; applying changes triggers targeted rescan.
**Errors/recovery:** Invalid/deleted exclusion paths pruned silently (local log).
**AC:** Given an excluded folder containing 500 videos, When the library scans, Then none appear in browse/search/history and re-including restores them without duplication.
**Tests:** TST-SCAN-07…08. **Confidence:** High.

#### FR-LIB-006 — Incremental rescan and new-item detection
**MUST · R1 · P1**
**Statement:** On app foreground (throttled), storage events where available, or manual refresh, the app MUST detect added/removed/changed items incrementally without a full rebuild, and MUST surface a lightweight "N new videos" affordance.
**Need/rationale:** JRN-04 routine; PER-1 expectation.
**Error/recovery:** Interrupted rescan resumes; failures fall back to full scan only with user-visible notice (FR-LIB-007).
**Platform:** Background/foreground restrictions vary by OS version (doc 09).
**AC:** Given 10k indexed items and 3 new files, When the user foregrounds the app, Then new items appear within the doc 05 budget and no full-scan indicator is shown.
**Tests:** TST-SCAN-09…11. **Confidence:** Med (platform event availability varies).

#### FR-LIB-007 — Full rescan and index rebuild
**MUST · R1 · P1**
**Statement:** The app MUST offer a full rescan that safely rebuilds library records and thumbnails when the user suspects staleness or after index corruption, preserving favorites/progress where item identity can be confirmed (FR-LIB-019), and warning when identity is ambiguous.
**Need/rationale:** §7.1 recovery; FC (corrupt DB).
**AC:** Given a deliberately corrupted library database, When the app launches, Then it detects corruption, offers rebuild, and recovers to a scanned state without crash or phantom entries.
**Tests:** TST-SCAN-12…14. **Confidence:** Med (identity strategy in doc 09 pending).

#### FR-LIB-008 — Metadata extraction
**MUST · R1 · P0**
**Statement:** For every indexed item, the app MUST extract and persist: duration (tolerant of missing/absurd values), resolution, orientation, container/stream identifiers needed by the compat matrix, size, date added/modified, and a best-effort display title from filename + embedded title when cheaply available.
**Need/rationale:** Browsing, sorting, compat reporting, doc 07.
**Errors/recovery:** Malformed metadata MUST NOT crash or block indexing; per-field nullability rules in doc 09.
**Perf:** Extraction budget per file defined in doc 05; parallelized with limits; never on main thread.
**AC:** Given a corpus including a file with no duration and one with garbage metadata, When scanned, Then both are indexed with null-safe fields, display sensibly, and neither blocks the scan.
**Tests:** TST-META-01…05. **Confidence:** High.

#### FR-LIB-009 — Thumbnails and artwork
**MUST · R1 · P1**
**Statement:** The app MUST generate legitimate video-frame thumbnails (or embedded artwork where appropriate) with a configurable cache size, LRU eviction, low-priority background generation, and correctness under rotation metadata.
**Need/rationale:** §7.1; DH-4 (scan/battery respect).
**Errors/recovery:** Failure yields a graceful placeholder; cache corruption self-heals; clearing cache never touches user media.
**Privacy/perf:** Thumbnails stored app-private; generation MUST respect battery/thermal state (defer on low battery/battery saver; doc 05).
**AC:** Given 5,000 videos on SD card with battery saver ON, When the library loads, Then no thumbnail generation starts until conditions allow, placeholders show, and enabling charging triggers generation at the doc 05 throughput budget.
**Tests:** TST-THB-01…06. **Confidence:** High (mechanism), Med (power thresholds).

#### FR-LIB-010 — Browse views
**MUST · R1 · P0**
**Statement:** The library MUST offer: All videos, Folders, Recently added, Continue watching, Favorites, and Playlists, each with stable sort order and position memory.
**Need/rationale:** JRN-02/JRN-04; PER-1 simplicity.
**States:** Empty states per view (FR-LIB-020). Grid/list toggle SHOULD exist.
**AC:** Given a library of 10k items, When switching views, Then each renders within the doc 05 budget and preserves scroll position per view.
**Tests:** TST-LIBV-01…04. **Confidence:** Med (perf budget pending).

#### FR-LIB-011 — Sorting, grouping, and filtering
**SHOULD · R1 · P1**
**Statement:** Users SHOULD be able to sort by name/date added/date modified/duration/size/folder, group by folder, and filter (watched/unwatched, resolution class, container class) with state remembered per view. Name sorting SHOULD ignore leading grammatical articles ("The", "A", "An", and localized equivalents) behind a setting that is **off by default**, so "The Matrix" sorts under M without surprising users who expect literal order.
**AC:** Given mixed content, When the user filters to "HEVC", Then only HEVC items (per stored codec class) appear, and the filter persists across restarts. Given the article-ignoring setting on, Then "The Matrix" appears under M and "A Quiet Place" under Q; with it off, both sort literally.
**Tests:** TST-FILT-01…03. **Confidence:** High.

#### FR-LIB-012 — Search
**MUST · R1 · P0**
**Statement:** Search MUST match substrings of title/folder quickly on 10k+ item libraries (incremental results as you type), tolerate case and diacritics (SHOULD), and offer recent searches.
**Perf:** Query latency budget in doc 05; search MUST remain usable during an active scan.
**AC:** Given 50k indexed items, When the user types a 4-character substring, Then first results appear within the doc 05 budget with no UI jank.
**Tests:** TST-SRCH-01…05. **Confidence:** Med (depends on doc 09 index design).

#### FR-LIB-013 — Media details view
**MUST · R1 · P1**
**Statement:** A details view MUST show filename, path (copyable), folder, size, dates, duration, resolution/fps where known, container/video/audio codec identifiers, subtitle track inventory (cheap probe), and actions (play, resume, favorite, add to playlist, delete-from-library vs delete-file distinction).
**Need/rationale:** PER-2 diagnostics; DH-1 transparency; feeds FR-DIAG.
**Security/privacy:** File deletion UX (if offered; MAY R1) MUST distinguish app-library removal from file deletion with explicit confirmation and no silent file ops (§7.1 / master prompt data-loss protection).
**AC:** Given any indexed file, When opened in details, Then shown identifiers match stored metadata, and any destructive action requires an explicit confirmation naming the exact item.
**Tests:** TST-DETL-01…04. **Confidence:** High.

#### FR-LIB-014 — Continue watching and history
**MUST · R1 · P0**
**Statement:** The app MUST maintain a local "Continue watching" list and a history view, with per-item resume positions (FR-LIB-015), and MUST offer "remove from history" per item and globally.
**Privacy:** History is local-only by default; no export/telemetry (DEC-008).
**AC:** Given items played to >5% and <95%, When opening Continue watching, Then those items appear ordered by recency with accurate remaining-time indicators.
**Tests:** TST-HIST-01…03. **Confidence:** High.

#### FR-LIB-015 — Resume semantics
**MUST · R1 · P0**
**Statement:** The app MUST persist resume position per item with defined semantics: save cadence (no more than doc 05 interval or on lifecycle events), completion threshold (≥95% → start from beginning next time, with "restart" affordance), near-start threshold (≤5% → restart silently), and tolerance handling for seek-back/-forward. The user-facing **resume policy MUST be tri-state — Always / Never / Ask — not a boolean** (prior art: doc 02A §3.3), with Always as the default; "Never" MUST still record position for Continue-watching display unless the user also opts out of history.
**Need/rationale:** JTBD-02; JRN-02; KPI-REL-05.
**Errors/recovery:** Process death within a session MUST NOT lose more than the cadence window; corrupt progress rows are ignored safely.
**AC:** Given playback killed by process death at 41:00, When reopening the item, Then resume offers within ±2 s of the last persisted point and never resumes at 0 silently. Given policy = Ask, Then reopening prompts once with the saved timestamp; Given policy = Never, Then playback starts at 0 without a prompt.
**Tests:** TST-RSUM-01…06. **Confidence:** High.

#### FR-LIB-016 — Favorites
**SHOULD · R1 · P1**
**Statement:** Users SHOULD be able to mark/unmark favorites from lists and the player, with a dedicated view and filter integration.
**AC:** Given a favorite item whose file is deleted, When the library refreshes, Then the favorite entry shows as unavailable (not silently vanish) with a "remove" option.
**Tests:** TST-FAV-01…02. **Confidence:** High.

#### FR-LIB-017 — Playlists (basic)
**SHOULD · R1 · P2**
**Statement:** Users SHOULD be able to create named playlists, add/remove/reorder items, and play a playlist with automatic next-item advance and consistent resume behavior.
**Errors/recovery:** Missing items are skipped with a notice at play time, not removed silently.
**AC:** Given a 50-item playlist where 2 files were moved, When starting playlist playback, Then playback continues across all available items and reports skipped ones at the end.
**Tests:** TST-PLST-01…04. **Confidence:** Med (interaction with queue/player state is the risk).

#### FR-LIB-018 — Moved/renamed/deleted file reconciliation
**MUST · R1 · P0**
**Statement:** The app MUST detect files that disappeared or changed and update states without data loss: resume/favorites survive re-index where identity can be reasonably confirmed (strategy in doc 09: media-store ID, size+duration+name heuristics, SAF document ID); otherwise entries become "missing" with explicit UI rather than silent removal.
**Need/rationale:** §7.1/§11; FC (file disappears); user trust (DH-7).
**AC:** Given a file moved between folders, When rescanning, Then its progress/favorite are retained if identity confirms, and otherwise the item is marked missing with a manual "re-link" option (MAY R2 UI; R1 shows missing state).
**Tests:** TST-RCN-01…05. **Confidence:** Med (identity heuristics need design; doc 09).

#### FR-LIB-019 — Empty states and guidance
**MUST · R1 · P1**
**Statement:** Every list/view MUST define empty, loading, error, and "nothing matches filters" states with actionable next steps (no dead ends).
**Need/rationale:** JRN-01/JRN-06; PER-4.
**AC:** Given a fresh install with zero videos, When the library opens, Then the empty state explains how to add content (picker/open-with) and remains fully screen-reader navigable.
**Tests:** TST-EMP-01…03. **Confidence:** High.

#### FR-LIB-020 — "Open with" / share-sheet playback
**MUST · R1 · P0**
**Statement:** The app MUST register for video `VIEW`/share intents (MIME type `video/*` and common specific types), start playback directly without requiring library access, and afterward offer (non-blocking) to index the containing folder where feasible.
**Need/rationale:** JTBD-11; JRN-01; PER-4; likely a major usage path.
**Error/recovery:** Unsupported URI schemes or permission-lacking URIs produce the FR-PLAY-004 error taxonomy treatment; content:// from other apps handled read-only.
**Security:** Treat incoming URIs/EXIF as untrusted input (doc 10, THR register).
**AC:** Given a video shared from a file manager to a fresh install with no permissions, When the user selects the app, Then playback starts (if decode is possible) with no library setup, and no permission nag appears before first frame.
**Tests:** TST-OPEN-01…05. **Confidence:** High.

#### FR-LIB-021 — Removable storage
**MUST · R1 · P1**
**Statement:** The app MUST browse/play/index from removable storage where the OS grants access, detect removal mid-use, and recover when reinserted (rescan targeted; no duplicate entries).
**Errors/recovery:** Removal during playback: defined UX (pause + explain; FC list) — never crash.
**AC:** Given library items on SD card, When the card is removed and re-inserted, Then items return searchable with progress intact and no duplicated records.
**Tests:** TST-SDCARD-01…04. **Confidence:** Med (device/OS variance).

#### FR-LIB-022 — Storage and cache management
**SHOULD · R1 · P2**
**Statement:** Settings SHOULD expose cache size, thumbnail cache clearing, DB rebuild, and library data reset — all without touching user media files (hard separation per §11).
**AC:** Given any of these actions, When executed, Then user media files are provably untouched (test verifies file hashes) and the app reaches a consistent state.
**Tests:** TST-STORE-01…03. **Confidence:** High.

#### FR-LIB-023 — Duplicate awareness
**MAY · R2 · P3**
**Statement:** The app MAY detect likely duplicates (identical size+partial hash) and offer informational grouping — never auto-delete.
**AC:** Given two identical files, When duplicates are computed (opt-in), Then they are grouped for review with zero automatic file operations.
**Tests:** TST-DUP-01…02. **Confidence:** Med.

*File operations (rename/move/delete of user media) are **MAY · R2** only where platform access allows, each behind explicit confirmation + undo-where-feasible; R1 limits destructive capability to "remove from library" (non-destructive).*

---

## 2. FR-PLAY — Playback engine and media compatibility behavior

#### FR-PLAY-001 — Playback contract
**MUST · R1 · P0**
**Statement:** Given a local media URI, the player MUST start playback with hardware decode when the platform supports the streams, expose first-frame state within doc 05 budgets, and terminate cleanly releasing all resources (no orphan decoders/audio sessions).
**Need/rationale:** JTBD-01; KPI-REL-01; engine hypothesis DEC-003.
**States:** `IDLE → PREPARING → BUFFERING? → PLAYING → (PAUSED) → ENDED/ERROR` state machine (full diagram in doc 09); state observable to UI.
**AC:** Given 30 representative files (doc 12 corpus), When each is played for 60 s with one seek, Then first frame and stability meet doc 05 budgets on the reference device set.
**Tests:** TST-PLAY-001…0NN. **Confidence:** Med (engine spike RQ-01 pending).

#### FR-PLAY-002 — Compatibility policy tiers
**MUST · R1 · P0**
**Statement:** The product MUST maintain and enforce the doc 07 matrix: **required/validated**, **expected (unvalidated)**, **best-effort**, **unsupported**, **blocked** — and user-facing messaging MUST NOT contradict it (DEC-010).
**Need/rationale:** Principle 5; DH-1.
**AC:** Given any file from the "unsupported" tier, When opened, Then the error taxonomy explains the class honestly (e.g., "protected/DRM content is not supported") without implying a temporary glitch.
**Tests:** TST-CMPT-01…04. **Confidence:** High (policy), matrix content pending doc 07.

#### FR-PLAY-003 — Decode strategy: hw-first, controlled fallback
**MUST · R1 · P0**
**Statement:** Decode MUST prefer hardware paths; on init/decode failure or unsupported streams, the player MUST attempt the sanctioned fallback **at most once per failure class** with bounded cost: for **video**, fallback = platform software decoders only (W3: bundled software *video* decode is out of R1 — Media3's FFmpeg extension is audio-only, and HEVC software-decode royalty status is ambiguous ⚖️); for **audio**, fallback = passthrough → bundled LGPL-clean FFmpeg audio decode where doc 11 clears it (see FR-AUD-007). A manual per-playback override MUST exist in the player's advanced menu, and a **global default decoder setting** MUST exist in Settings — the per-item override does not replace a global default (prior art: VLC ships a *Hardware Acceleration* setting with `Automatic` as the default; MX keeps a default decoder in Settings *and* a separate per-item "Play…" chooser). **Label language (Q13 — closed 2026-10-19):** the control MUST pair a technical name with a plain-language when-to-use description — `Auto` ("picks the best decoder automatically" — the default), `Hardware` ("faster and easier on the battery; keep this if playback is smooth"), `Software` ("most compatible; may stutter or drain battery on large 4K files"). **Bare `HW`/`HW+`/`SW` tokens MUST NOT be the only label:** the cost of that pattern is documented by the vendor that chose it — MX Player maintains a support-article tree explaining its own abbreviations, third parties publish explainers, and users still ask what the letters mean (`research/decoder_label_practice.md`).
**Need/rationale:** JTBD-01/JTBD-07; DH-1; battery budget NFR.
**Errors/recovery:** Fallback failure → FR-PLAY-004 taxonomy; fallback success → unobtrusive, dismissible **"playing in compatibility mode"** notice with a "why?" sheet naming the failed class and what was used instead. Fallback MUST be bounded: **at most one automatic attempt per failure class per item**, no silent retry loops; a manual override in the advanced sheet (SCR-010e) MAY try another path once. The user's explicit choice MUST persist per item (FR-CTRL-012) and MUST NOT be overridden automatically on reopen.
**Perf/battery:** Fallback sessions logged locally (class only) to audit battery impact; doc 05 sets sustained software-decode envelope.
**AC:** Given a HEVC 10-bit file on a device whose hw decoder rejects the profile, When played, Then software fallback engages within the doc 05 time budget, A/V sync holds, and the user sees a dismissible notice explaining the fallback. Given a file that fails every path, Then exactly one fallback attempt occurs per class, the error taxonomy is shown, and no retry loop starts.
**Tests:** TST-DEC-01…08. **Confidence:** Med (depends on platform software-decoder behavior on target SoCs — RQ-03; plus doc 11 licensing sign-off for audio decode). Prior art for the fallback policy: doc 02A §9.3.

#### FR-PLAY-004 — Error taxonomy and honest failure UI
**MUST · R1 · P0**
**Statement:** Playback failures MUST map to a stable taxonomy with user-searchable error codes: `UNSUPPORTED_CONTAINER`, `UNSUPPORTED_CODEC`, `UNSUPPORTED_PROFILE` (e.g., 10-bit on limited hw), `DRM_PROTECTED`, `CORRUPT_MEDIA`, `IO_STORAGE_UNAVAILABLE`, `PERMISSION_LOST`, `DECODER_FAILURE`, `RESOURCE_EXHAUSTED`, `UNKNOWN`. Each MUST show: what happened, what was tried, 1–3 concrete next actions, and a copy-diagnostics affordance (redacted; FR-DIAG).
**Need/rationale:** JRN-05; DH-1; support burden KPI.
**AC:** Given each taxonomy case with a fixture file, When played, Then the correct code-specific UI appears, is screen-reader complete, and offers at least one action that does not dead-end (e.g., "try software decoding", "copy diagnostics", "open details").
**Tests:** TST-ERR-01…10. **Confidence:** High.

#### FR-PLAY-005 — Seeking
**MUST · R1 · P0**
**Statement:** Seeking MUST support relative steps (configurable ±5/10/30 s defaults), scrub-to-position with a preview where cheap, and MUST render the frame at the target within doc 05 budget; seeks during hw limitations (e.g., HEVC slow seek) MUST remap to nearest keyframe with UI honesty, then refine when accurate seeking is possible.
**Need/rationale:** JTBD-01; KPI-REL-04.
**Errors/recovery:** Unseekable file (weird timestamps) → scrub disabled with explanation (FR-PLAY-019).
**AC:** Given 10 file classes, When performing 20 seeks each, Then ≥ doc 05 target accuracy within budget and no session interruption.
**Tests:** TST-SEEK-01…06. **Confidence:** Med.

#### FR-PLAY-006 — A/V sync maintenance and repair
**MUST · R1 · P0**
**Statement:** The player MUST maintain A/V sync through start, seeks, speed changes, route changes, and long sessions; drift beyond tolerance MUST auto-correct, and a manual audio offset control (FR-AUD-003) MUST remain available.
**AC:** Given a 45-minute test file with 30 seeks, When measured at checkpoints, Then sync remains within doc 05 tolerance without user intervention.
**Tests:** TST-SYNC-01…06. **Confidence:** Med (measurement method in doc 13).

#### FR-PLAY-007 — Buffering/stall recovery for local storage
**MUST · R1 · P1**
**Statement:** Slow storage reads MUST present a buffering state, retry with backoff, and recover without losing audio continuity where possible; repeated stalls MUST surface the storage-health error class rather than hanging forever.
**AC:** Given throttled/slow SD reads (test harness), When playing, Then the session survives within doc 05 stall-recovery budget or fails into `IO_STORAGE_UNAVAILABLE` with actions.
**Tests:** TST-STALL-01…04. **Confidence:** Med (needs harness design, doc 12).

#### FR-PLAY-008 — VFR and unusual timing tolerance
**SHOULD · R1 · P1**
**Statement:** The player SHOULD play variable-frame-rate and unusual-timestamp files with stable sync and no accumulating drift; files with broken timestamps (negative/shuffled) MUST degrade to a defined mode (FR-PLAY-019) rather than loop or crash.
**AC:** Given VFR fixtures (screen recordings, phone captures) and a timestamp-broken fixture, When played 10 minutes each, Then sync/perf hold and no crash/hang occurs.
**Tests:** TST-VFR-01…04. **Confidence:** Med.

#### FR-PLAY-009 — Corrupt/truncated media handling
**MUST · R1 · P1**
**Statement:** The player MUST attempt best-effort playback of partially damaged files (play until damage point), clearly mark damaged media, and MUST NEVER crash the app or loop infinitely on parser errors; salvage modes (e.g., mkv damaged index) MAY be added per doc 07 feasibility.
**AC:** Given truncated and header-mangled fixtures, When played, Then the app reaches either playable state with damage marking or taxonomy error; zero crashes in the adversarial suite.
**Tests:** TST-CORR-01…06. **Confidence:** High (posture), Med (salvage quality).

#### FR-PLAY-010 — Orientation, rotation, and system UI
**MUST · R1 · P0**
**Statement:** Playback MUST honor rotation metadata, sensor orientation per user mode (auto/portrait/landscape/locked), hide system bars in fullscreen per platform idiom, and switch without frame-loss glitches or state resets.
**AC:** Given a rotated-90° file on a rotating device, When rotating during playback, Then presentation matches metadata + sensor policy, position/state unchanged, no blank-frame flash beyond doc 05 budget.
**Tests:** TST-ORI-01…05. **Confidence:** High.

#### FR-PLAY-011 — Aspect, zoom, and geometry
**MUST · R1 · P1**
**Statement:** The player MUST offer fit/fill/crop/stretch + source-aspect modes, pinch-zoom/pan with double-tap-to-fit reset, and MUST persist per-video geometry preference separately from global default. An **Auto** mode SHOULD detect and trim hard-coded black borders where cheaply detectable, and MUST fall back to Fit (with no visual glitch) when detection is inconclusive.
**AC:** Given a 4:3 file in default fit mode, When switching to crop and reopening later, Then the per-video choice persists and other files remain unaffected.
**Tests:** TST-GEOM-01…04. **Confidence:** High.

#### FR-PLAY-012 — Refresh-rate alignment
**SHOULD · R1 · P2**
**Statement:** Where the platform exposes it, the player SHOULD align display refresh to video cadence-aware modes, and MUST NOT cause visible judder regressions on devices where switching is problematic (feature-flagged safe-list approach; doc 07/08).
**Risks:** Device variance is high; treat as enhancement with kill-switch.
**AC:** Given 24/25/30/60 fps fixtures on a switch-capable device, When played, Then cadence is smooth per doc 13 methodology and the kill-switch disables switching app-wide.
**Tests:** TST-RATE-01…04. **Confidence:** Low-Med (device variance research pending W2).

#### FR-PLAY-013 — HDR content policy
**MUST · R1 · P0 (policy)**
**Statement:** HDR handling MUST follow doc 07's per-class policy: present natively where validated; otherwise tone-map or fall back per platform capability with no color corruption and no false HDR badge; user-visible indication of what is happening MUST be available in playback stats (FR-DIAG).
**AC:** Given HDR10 and DV fixtures on devices with and without support, When played, Then each device path yields the documented outcome and stats report the actual path taken.
**Tests:** TST-HDR-01…08. **Confidence:** Low (policy), matrix pending W2/RQ-03/RQ-12.

#### FR-PLAY-014 — High-bit-depth policy (10-bit)
**SHOULD · R1 · P1**
**Statement:** 10-bit content SHOULD play via hw where validated; software fallback MUST disclose performance implications; if neither path is viable, taxonomy error applies — never silent magenta/garbage output.
**AC:** Given HEVC 10-bit on a device without hw 10-bit, When played, Then either correct software playback with notice or `UNSUPPORTED_PROFILE` error — never corrupt visuals.
**Tests:** TST-BIT-01…04. **Confidence:** Med.

#### FR-PLAY-015 — State restoration across lifecycle
**MUST · R1 · P0**
**Statement:** Playback state (position, speed, tracks, geometry, subtitle config) MUST survive: orientation change, PiP transitions, backgrounding (policy per FR-PLAY-018), permission changes, and process death (resume semantics FR-LIB-015).
**AC:** Given a running session, When the OS kills the process, Then relaunch restores item + position + user prefs and reports no false completion.
**Tests:** TST-REST-01…08. **Confidence:** High (design), Med (OS edge behavior).

#### FR-PLAY-016 — Decoder crash containment
**MUST · R1 · P0**
**Statement:** A platform decoder crash (native layer) MUST be contained: the app relaunches playback with the alternate decode path once automatically where safe, and otherwise fails into taxonomy with the session's diagnostics preserved (local).
**AC:** Given forced decoder-crash fixtures/harness, When the crash occurs, Then the app does not itself die, performs at most one auto-fallback, and records a local diagnostic event (EV-*) without media content.
**Tests:** TST-DCRASH-01…05. **Confidence:** Med (platform variance).

#### FR-PLAY-017 — Resource lifecycle and leak prevention
**MUST · R1 · P0**
**Statement:** Stopping playback MUST release decoders, surfaces, audio session, wake locks, and any scanning work tied to the session; repeated open/close cycles MUST show no unbounded memory/handle growth.
**AC:** Given 200 open/close cycles across codec classes, When profiled via doc 13 protocol, Then resource counts return to baseline within defined bounds.
**Tests:** TST-LEAK-01…04. **Confidence:** High.

#### FR-PLAY-018 — Background and PiP behavior
**MUST · R1 · P1**
**Statement:** Video playback MUST follow: exiting to PiP continues (where OS supports); full backgrounding without PiP pauses by default (configurable to "continue audio-only" for audio-capable files); media session/lock-screen controls MUST stay correct (FR-PLAT domain, Part B).
**AC:** Given each background path, When exercised, Then behavior matches settings, audio focus is correct, and no ghost playback continues unnoticed.
**Tests:** TST-BG-01…06. **Confidence:** High (policy), platform variance Med.

#### FR-PLAY-019 — Unusual-timestamp/degraded mode
**SHOULD · R1 · P2**
**Statement:** For files where duration/timestamps are unusable, the player SHOULD enter a degraded mode: playback works, progress UI shows indeterminate state, seeking disabled with explanation, resume reduced to time-based fallback.
**AC:** Given timestamp-broken fixtures, When played, Then degraded mode engages per definition and returns to normal mode on well-formed files.
**Tests:** TST-DEG-01…03. **Confidence:** Med.

#### FR-PLAY-020 — 4K/8K and high-resolution policy
**MUST · R1 · P0 (policy)**
**Statement:** High-resolution playback MUST be supported only where device/platform validation confirms it; messaging MUST NOT promise 4K/8K universally; resource guards (memory/thermal) MUST degrade gracefully (notice, not crash).
**AC:** Given 4K fixtures across tiers, When played on capable devices, Then outcomes meet doc 05; on incapable, taxonomy/notice per policy — zero OOM crashes in adversarial suite.
**Tests:** TST-4K-01…05. **Confidence:** Med.

---

## 3. Part B — Controls, Subtitles, Audio, Platform, Settings, Diagnostics (+ boundaries)

### 3.0 Scope note
Network playback and personalization/intelligence are specified only as **boundary requirements** (FR-NET, FR-PERS): nothing network- or AI-related ships in R1. Part B requirement records carry the same fields as Part A (statement, rationale, states, errors, platform, deps, privacy/a11y/perf, AC, tests, confidence); where a field adds nothing beyond Part A's conventions set, it is marked "per §0 conventions".

### 3.1 FR-CTRL — Controls and player interaction

#### FR-CTRL-001 — Core transport controls
**MUST · R1 · P0**
**Statement:** Tapping the video (or configured equivalent) MUST toggle the control overlay; the overlay MUST provide play/pause, ±seek steps, a position scrubber with time labels, and next/previous when a playlist/queue exists; controls MUST auto-hide per configured timeout and reappear on interaction.
**Need/rationale:** JTBD-01/06; JRN-02. **States/persistence:** overlay visibility per session; timeout default 3 s (configurable 2–10 s or "never while locked").
**Errors/recovery:** Controls remain operable in error states (retry/exit). **Platform:** PiP uses system controls; hardware keys via MediaSession (FR-PLAT-002).
**A11y/perf:** All controls exposed with roles/labels; targets ≥48 dp; overlay must not drop video frames (NFR-PERF).
**AC:** Given playback, When tapping, Then controls appear within 100 ms and auto-hide after the configured timeout; near-edge incidental taps don't hide them.
**Tests:** TST-CTRL-01…05. **Confidence:** High.

#### FR-CTRL-002 — Seek steps and skip
**MUST · R1 · P0**
**Statement:** Configurable forward/back seek steps (defaults 10 s/30 s, per-direction), previous/next item, hold-to-repeat with acceleration (SHOULD); each action gives visible position feedback (time + delta).
**AC:** Given configured 10/30, When skipping, Then position changes by exactly the step (within NFR-SEEK tolerance) with feedback; rapid repeats cannot desync the UI.
**Tests:** TST-CTRL-06…08. **Confidence:** High.

#### FR-CTRL-003 — Scrubbing and preview
**SHOULD · R1 · P1**
**Statement:** Dragging the scrubber MUST show live position; frame/thumbnail preview while scrubbing SHOULD appear when cheaply possible (cached thumbnails); when unavailable, time-only display is used — degraded honestly, never faked.
**AC:** Given cached thumbs, When scrubbing, Then preview updates within 250 ms per position change on the reference device set; without cache, no jank.
**Tests:** TST-CTRL-09…10. **Confidence:** Med (preview cost varies by codec/device).

#### FR-CTRL-004 — Playback speed
**MUST · R1 · P1**
**Statement:** Presets (0.25×–3×) plus custom entry (0.1×–4.0×); pitch-corrected audio where the platform supports it (disclose when not); speed resets to default on new items unless "remember" is set.
**AC:** Given 2.0× with pitch correction available, Then speech intelligibility per listening protocol (doc 13) and position accuracy hold within tolerance.
**Tests:** TST-CTRL-11…12. **Confidence:** Med (platform pitch behavior varies).

#### FR-CTRL-005 — Gesture system
**MUST · R1 · P0**
**Statement:** Defaults = single-tap toggle controls; double-tap left/right = seek ∓/± step (configurable/disable); vertical swipe left = brightness (true system brightness where possible, else overlay dimming **and the setting says so**); vertical swipe right = volume (media stream only); horizontal swipe = seek with live delta; pinch = zoom; edge/system gestures never stolen; every gesture individually disableable; min-distance/time thresholds prevent accidental activation.
**Need/rationale:** JTBD-06; PER-1/4/5; DH-5 (execution quality).
**Honesty constraint:** never pretend to change something we cannot (brightness on some paths).
**AC:** Given the doc-12 gesture suite (incl. rapid, edge, and simultaneous touches), Then actions match spec with <2% false positives and no conflict with system gestures on reference devices.
**Tests:** TST-GEST-01…08. **Confidence:** Med (device/OEM gesture variance).

#### FR-CTRL-006 — Screen lock / accidental-touch prevention
**MUST · R1 · P1**
**Statement:** Lock mode blocks all playback input except an explicit, discoverable unlock affordance; lock state is visible; first tap on locked screen shows an unlock hint instead of acting.
**AC:** Given pocket-simulation input (doc 12), Then no playback action triggers until unlock.
**Tests:** TST-CTRL-13…14. **Confidence:** High.

#### FR-CTRL-007 — Fullscreen, system bars, safe areas
**MUST · R1 · P1**
**Statement:** Immersive playback per platform idiom; system bars hidden with tap-to-reveal; cutout/rounded-corner/gesture safe areas respected in all orientations.
**AC:** Given a cutout device in landscape, Then controls/subtitles never intersect unsafe areas; system back gesture remains functional.
**Tests:** TST-CTRL-15…16. **Confidence:** High.

#### FR-CTRL-008 — Orientation modes
**MUST · R1 · P0**
**Statement:** Modes auto/portrait/landscape/locked (+reverse); persisted per user preference; rotation follows FR-PLAY-010 without state loss; "auto" is default.
**AC:** Given landscape-locked, When rotating to portrait, Then playback continues in landscape with no state change or glitch.
**Tests:** TST-CTRL-17…18. **Confidence:** High.

#### FR-CTRL-009 — Sleep timer
**MUST · R1 · P2**
**Statement:** Off / end-of-current-item / N minutes (with fade-out SHOULD); countdown visible on demand; timers survive background audio continuation; manual stop clears the timer.
**AC:** Given a 30-min timer, When elapsed, Then stop/pause per setting within ±5 s with fade if enabled.
**Tests:** TST-CTRL-19. **Confidence:** High.

#### FR-CTRL-010 — A-B repeat
**SHOULD · R1 · P2**
**Statement:** Set/clear A and B points with visible markers; loop region playback; defined conflict policy for seeks outside the region (default: stay in loop).
**AC:** Given A-B set, When B reached, Then loop repeats within ±100 ms; clearing restores normal playback.
**Tests:** TST-CTRL-20. **Confidence:** Med (timestamp edge cases).

#### FR-CTRL-011 — Chapters and bookmarks
**SHOULD (chapters) / MAY (bookmarks) · R1/R2 · P2**
**Statement:** Chapter list when the container provides it, with accurate jumps; bookmarks with timestamp (+ optional local note) — local-only in R1, no sharing/sync.
**AC:** Given a chaptered MKV, Then list matches container data and jumps land within tolerance.
**Tests:** TST-CTRL-21…22. **Confidence:** Med (container variance).

#### FR-CTRL-012 — Preference persistence model (per-video vs global)
**MUST · R1 · P1**
**Statement:** A single documented matrix defines which preferences persist per-video (geometry/zoom, subtitle track+style+offset+encoding, audio track+delay, speed if enabled, resume) vs globally (defaults, gesture settings); "reset this video" restores global defaults for that item only.
**AC:** Given overrides on two videos, Then each restores its own overrides after restart and a reset affects only the targeted video.
**Tests:** TST-PREF-01…03. **Confidence:** High.

#### FR-CTRL-013 — One-handed reachability
**SHOULD · R1 · P1**
**Statement:** Essential controls reachable one-handed on tall phones (bottom-weighted layout option); tablets get adapted (centered/larger) layouts; no interactive control inside reserved system-gesture zones.
**AC:** Given a ≥6.5" phone, Then play/pause/seek/back reachable per doc-06 ergonomics checklist without chorded gestures.
**Tests:** TST-ERGO-01. **Confidence:** High (design-dependent).

#### FR-CTRL-014 — External controls: headsets, media keys, keyboard, D-pad
**MUST · R1 · P1**
**Statement:** Media buttons (play/pause/next) via MediaSession incl. double/triple-click headset dispatch where the platform sends it; keyboard shortcuts where keyboards exist; D-pad focus operable on supported form factors; every external action has an on-screen/accessibility equivalent.
**AC:** Given BT headset, When rapid play/pause presses, Then state matches platform dispatch without desync or double-toggle.
**Tests:** TST-EXT-01…03. **Confidence:** Med (OEM dispatch variance).

#### FR-CTRL-015 — Haptics and feedback overlays
**SHOULD · R1 · P2**
**Statement:** Haptic ticks for scrub boundaries/double-tap (respect system haptics + reduce-motion); brightness/volume/seek feedback overlays readable at maximum font scale.
**AC:** Given haptics disabled system-wide, Then none fire; overlays remain legible at 200% font scale.
**Tests:** TST-CTRL-23. **Confidence:** High.

#### FR-CTRL-016 — Control customization
**SHOULD · R1 · P2**
**Statement:** Customizable gesture mapping, step presets, overlay timeout, optional button visibility — with reset; customization MUST NOT be able to strand a user (core actions always reachable via buttons + accessibility actions).
**AC:** Given a heavily customized mapping, Then a settings reset (and OS accessibility actions) can still operate all core functions.
**Tests:** TST-CTRL-24. **Confidence:** High.

#### FR-CTRL-017 — Playback queue, play modes, and end-of-item behaviour
**MUST · R1 · P1**
**Statement:** Behaviour at `ENDED` MUST be defined and user-controlled, never implicit. Play modes MUST include **Single**, **Folder** (the item's folder in current sort order), **Queue/Playlist**, **Repeat one**, and **Repeat all**; **shuffle** SHOULD be available for folder/queue contexts. The active mode MUST be visible in the player overlay and MUST persist across sessions.

**Default (evidence-based, doc `research/autoplay_defaults.md`):**
1. **If a next item exists** (the user started from a folder, playlist, or series context), the default MUST be **auto-advance ON** with a **cancellable countdown — 10 s on mobile**, configurable to Immediate / 5 / 10 / 15 / 30 / 60 s. **Never an instant cut.**
2. **If no next item exists** (single file opened from a file manager or share sheet), playback MUST simply stop at `ENDED` with a replay affordance. There is nothing to advance to; no policy is applied.
3. **Inactivity guard (MUST):** auto-advance MUST be suppressed when there has been **no user interaction for more than 2 hours**, and the Up next item MUST still be shown and pre-selected so one tap plays it. This prevents unattended all-night playback — the principal hazard of defaulting ON.
4. Auto-advance MUST NOT fire when there is no foreground playback session, and MUST be suppressed while lock mode is engaged unless the user explicitly enabled it.

**Need/rationale:** JTBD-02 (finish a folder/series without returning to the library); KPI-REL-05. The default is **not a preference**: every comparable product defaults auto-advance ON when a next item exists — Netflix, YouTube (adults), Plex, Emby, Infuse ("Continuous Playback"), and MX Player ("Playalone") — and the one comparable that defaulted OFF (Swiftfin) reports that "the default off setting is the source of half the 'can you make it play the next one' requests" `[V2]` (research/autoplay_defaults.md §2.8). The reversibility argument for defaulting OFF is answered by the countdown, not by stopping: a countdown is cancellable, so a user who wants to stop is not trapped, while a user who wants to continue is not forced to touch the device.

**States/persistence:** `ENDED` is distinct from `PAUSED`; completion is recorded per FR-LIB-015 thresholds. Mode persists globally; a queue is session-scoped unless it originates from a playlist (FR-LIB-017). The countdown is visible, cancellable, and pausable by any interaction.

**Errors/recovery:** Missing/unplayable next item → skip with a non-blocking notice and continue; empty queue → stop cleanly with no error. If the next item fails to prepare, stay on the `ENDED` surface with the error surfaced per FR-PLAY-004 — never a blank screen.

**Post-playback actions (MUST):** the `ENDED` surface MUST keep **replay**, **next**, and **delete-from-library** reachable. Auto-advance MUST NOT be the only path forward, and adding it MUST NOT remove the ability to act on the item that just finished (prior art: Infuse's auto-advance regression broke delete-after-watch, research/autoplay_defaults.md §2.4).

**Platform:** Auto-advance interacts with background/PiP policy (FR-PLAY-018) and must not start playback from background.

**Known limitation:** YouTube defaults autoplay OFF for users aged 13–17. We have no account and no age signal (DEC-004/DEC-012), so we cannot replicate age-gating; this is recorded rather than ignored.

**AC:** Given a 10-item folder with default settings, When item 3 ends, Then a **10 s cancellable countdown** appears and item 4 starts when it elapses, and item 3 is marked complete. Given the user cancels the countdown, Then playback holds on the `ENDED` surface and does not advance. Given a single file opened via share sheet, When it ends, Then playback stops with replay and no countdown. Given no interaction for >2 h, When an item ends, Then no auto-advance occurs and Up next is shown pre-selected. Given Repeat one, Then the item restarts without a countdown. Given auto-advance disabled in settings, Then playback stops at `ENDED` with Up next available.

**Tests:** TST-CTRL-25…27, TST-QUEUE-*. **Confidence:** High (behaviour is well-evidenced across six products); Med (2 h guard and 10 s countdown values are adopted from Plex defaults and are not validated for our audience — treat as configurable defaults, not constants).

### 3.2 FR-SUB — Subtitle system

#### FR-SUB-001 — Track discovery and matching
**MUST · R1 · P0**
**Statement:** Discover embedded tracks and sidecar files (same directory; `basename.*`, `basename.lang.*`, `subs/` folders), rank candidates (declared language preference > exact basename > encoding heuristic), and resolve ambiguity with a chooser that labels candidates (language, format, encoding where detectable); a user's manual choice is remembered and never silently overridden. For libraries where sidecars live outside the media folder, the user MUST be able to designate **one additional subtitle folder** to search (prior art: MX Player's subtitle folder, doc 02A §4.4) — this is the practical mitigation for scoped-storage libraries where sidecars are not co-located.
**Need/rationale:** JTBD-03; JRN-03; doc 02 §6 #5 (subtitle UX is a top-ranked evidence-backed pain).
**AC:** Given 3 embedded + 2 sidecar subs (mixed languages, one mis-encoded), Then all appear with labels, default matches rules, and the previous manual choice is honored on reopen.
**Tests:** TST-SUB-01…05. **Confidence:** Med (matching heuristics need corpus testing).

#### FR-SUB-002 — Format support policy
**MUST · R1 · P0**
**Statement:** Supported subtitle formats MUST match doc 07's tiers (baseline: SRT, SSA/ASS, WebVTT, TTML — Media3-supported MIME types [V1 platform.md §1]; SMI/SUB/IDX-VobSub/PJS/MPL2/Teletext = best-effort per matrix; image-based tracks (PGS/VobSub) per doc 07 engine-capability tier); unsupported formats produce honest messaging, never silent garbage.
**AC:** Given one file per tier, Then rendering matches tier definition and unsupported formats surface a clear notice while the chooser remains available.
**Tests:** TST-SUB-06…10. **Confidence:** Med (ASS/image-tier behavior depends on engine spike RQ-06).

#### FR-SUB-003 — Styling controls
**MUST · R1 · P1**
**Statement:** User controls for font family (bundled = OFL fonts only, e.g., Noto, per doc 11; plus system default), size (relative), color, outline width/color, shadow, background box/opacity, vertical position, margins; live preview; global defaults + per-video override (FR-CTRL-012); where styled subtitles (ASS) specify their own look, the user's explicit override wins after a one-time notice explaining fidelity limits.
**AC:** Given each control, Then rendering updates within 200 ms without playback interruption and persists per policy.
**Tests:** TST-SUB-11…13. **Confidence:** Med (ASS styling fidelity — spike).

#### FR-SUB-004 — Sync adjustment
**MUST · R1 · P0**
**Statement:** Offset control with coarse (50/100/500 ms) and fine (10 ms via hold) steps, live apply, reset to zero; active offset is visible in the subtitle panel; persisted per video. The panel SHOULD offer a **calibration aid** for users who cannot estimate an offset in milliseconds — a two-tap "that sound / that picture" flow (prior art: VLC's SOUND HEARD / SOUND SPOTTED widget, doc 02A §3.4) that computes the delta and previews it before applying.
**AC:** Given +500 ms, Then timing shifts by exactly 500 ms (±10 ms) and survives restart for that video only. Given the calibration aid, Then the computed offset is within ±50 ms of the true offset for a known fixture and is applied only after explicit confirmation.
**Tests:** TST-SUB-14…15. **Confidence:** High (manual offset); Med (calibration accuracy depends on user timing).

#### FR-SUB-005 — Encodings, RTL, complex scripts
**MUST · R1 · P0**
**Statement:** Auto-detect common encodings (UTF-8/16 + legacy codepages; CJK best-effort) with manual override in the chooser (mojibake fix flow); correct BiDi/RTL rendering and complex-script shaping via the platform text stack; missing glyphs fall back to system fonts before showing tofu.
**AC:** Given the encoding/RTL/complex-script fixture table (doc 12), Then rendered text is correct (or the app explains a genuine font-coverage limit), with zero crashes.
**Tests:** TST-SUB-16…20. **Confidence:** Med (codepage detection heuristics).

#### FR-SUB-006 — Scaling and placement
**MUST · R1 · P1**
**Statement:** Subtitle size respects user setting with sane device-size scaling; safe-area defaults avoid notches/gesture bars; portrait/landscape transitions preserve placement; system font scale does not double-apply (explicit policy).
**AC:** Given max font scale + 200% subtitle size in both orientations, Then no clipping and playback perf holds.
**Tests:** TST-SUB-21…22. **Confidence:** High.

#### FR-SUB-007 — Malformed and extreme subtitle handling
**MUST · R1 · P0**
**Statement:** Malformed files (invalid/overlapping/zero-length cues, 100k+ cues, recursive tags, bad encodings) MUST degrade gracefully (skip bad cues, bounded parse work) and MUST NOT crash, hang, or leak; parser resource caps are defined in doc 09.
**AC:** Given the adversarial subtitle corpus (doc 12), Then zero crashes/ANRs; failures surface as notices; load-time caps respected.
**Tests:** TST-SUB-23…27. **Confidence:** High (posture), Med (adversarial corpus quality).

#### FR-SUB-008 — Caption readability and accessibility
**MUST · R1 · P1**
**Statement:** Legibility-first defaults (background box ON for plain-text formats), optional high-contrast preset, respect for platform caption preferences where feasible (integration policy doc 06), and screen-reader semantics that do not spam subtitle updates.
**AC:** Given default settings, Then subtitle contrast meets WCAG AA against bright backgrounds per doc 13 method; TalkBack receives a single stable caption element, not per-cue spam.
**Tests:** TST-SUB-28…29. **Confidence:** High.

#### FR-SUB-009 — Per-video subtitle memory
**MUST · R1 · P1**
**Statement:** Chosen track, offset, style/encoding overrides persist per video (identity rules per FR-LIB-018); an "Auto" option restores rule-based selection.
**AC:** Given a manual choice on video A, Then after restart A restores it and video B is unaffected.
**Tests:** TST-SUB-30…31. **Confidence:** High.

#### FR-SUB-010 — Online subtitle services (boundary)
**MUST NOT ship in R1 · R2/R3 candidate**
**Statement:** No third-party subtitle download/translation/generation until: provider terms reviewed (doc 11), consent + privacy design approved (doc 10/14), no filename/content transmission by default, and core offline behavior remains unaffected. The R1 build MUST contain no such capability (verifiable by network capture + code audit).
**AC (boundary):** Given R1 build + test harness, Then no network subtitle call is possible.
**Tests:** TST-PRIV-NET-01 (doc 10). **Confidence:** High.

### 3.3 FR-AUD — Audio

#### FR-AUD-001 — Track selection
**MUST · R1 · P0**
**Statement:** List audio tracks with language/title/channels/codec labels when available; default selection policy = language preference > non-commentary > container-default flag (documented); choice persists per video.
**AC:** Given multi-track MKV, Then labels are accurate, default matches policy, switching preserves position and sync.
**Tests:** TST-AUD-01…03. **Confidence:** High.

#### FR-AUD-002 — Multichannel and downmix policy
**MUST · R1 · P1**
**Statement:** Output follows platform routing/downmix; the app MUST NOT claim controllable fidelity it doesn't have; >2ch passthrough only where device+route support it (doc 07 tiers); stereo path always works.
**AC:** Given 5.1 content with stereo-only output, Then audible downmix with no pitch/sync artifacts; stats overlay reports the actual path.
**Tests:** TST-AUD-04…05. **Confidence:** Med (route/device variance).

#### FR-AUD-003 — Audio delay adjustment
**MUST · R1 · P1**
**Statement:** Offset control (10 ms steps, hold for coarse; bounded ±5 s), live apply, per-video persist, reset.
**AC:** Given +200 ms on a sync fixture, Then lip-sync improvement measurable per doc 13 method; persists per video only.
**Tests:** TST-AUD-06…07. **Confidence:** High.

#### FR-AUD-004 — Audio focus and interruptions
**MUST · R1 · P0**
**Statement:** Correct audio-focus participation (pause on loss unless transient/per policy; configurable resume-after-interruption); calls/notifications/alarms per platform norms; Android 15+/17 hardening honored (focus only when top-app or compliant FGS; background-audio rules per FR-PLAT-003).
**AC:** Given an incoming call mid-playback, Then audio pauses cleanly, resumes per setting, and no dual-audio overlap occurs with the other app.
**Tests:** TST-AUD-08…12. **Confidence:** Med (OS-version behavior matrix).

#### FR-AUD-005 — Route changes
**MUST · R1 · P0**
**Statement:** Headset unplug → pause (default; configurable); BT connect/disconnect transitions per policy with at most a brief, bounded gap; volume carried safely across routes (no blast at speaker on unplug).
**AC:** Given BT disconnect during playback, Then behavior matches setting; no unexpected speaker output; position/sync intact.
**Tests:** TST-AUD-13…15. **Confidence:** Med (device variance).

#### FR-AUD-006 — Background/audio-continuation policy
**SHOULD · R1 · P1**
**Statement:** Default = backgrounding pauses video; optional "continue as audio" for audio-capable content; when enabled it runs with compliant FGS + MediaSession; no boot-time auto-resume (Android 15+ constraint).
**AC:** Given "continue as audio" OFF, Then backgrounding pauses and no FGS mismatch occurs; ON → audio continues with correct notification/session.
**Tests:** TST-AUD-16…17. **Confidence:** Med (Android 17 hardening nuance — platform.md open Q5).

#### FR-AUD-007 — Unsupported audio codec policy
**MUST · R1 · P0**
**Statement:** For audio the platform lacks (AC-3/E-AC-3/DTS/DTS-HD/TrueHD etc.): attempt (a) passthrough where device+route support, then (b) bundled **audio** decode where doc 11 legally clears it (current W3 status: self-built LGPL FFmpeg audio extension; **AC-3/E-AC-3 = candidate add-back pending legal recheck; DTS = passthrough-only without license; TrueHD/Dolby-branded = passthrough-only**), else (c) error class + offer "video without audio" with explicit notice.
**Need/rationale:** JTBD-01/10; doc 02 §6 #8; W3 working rule §6.5 (passthrough avoids app-level codec licensing; bundled decode pulls exposure into our binary).
**AC:** Given TrueHD with no decoder/passthrough, Then app offers video-without-audio + explanation (never a frozen UI or silent failure); given E-AC-3 after add-back approval, decode works within battery budget.
**Tests:** TST-AUD-18…22. **Confidence:** Med (licensing-dependent tiers).

#### FR-AUD-008 — Volume and safety caveats
**MUST · R1 · P0 (caveat)**
**Statement:** Volume gestures/controls affect the media stream only; the app MUST NOT bypass system volume safeguards; no absolute-volume circumvention; a plain-language note explains device limits.
**AC:** Given max system volume, Then no control path exceeds it; UI reflects the true stream volume.
**Tests:** TST-AUD-23. **Confidence:** High.

### 3.4 FR-PLAT — Platform integration

#### FR-PLAT-001 — Picture-in-Picture
**MUST · R1 · P1**
**Statement:** PiP on Android 8+ (API 26 floor): auto-enter on API 31+ (`setAutoEnterEnabled`), explicit path 26–30; window controls work; exit returns state intact; PiP not offered in fatal error states.
**AC:** Given home gesture during playback on reference devices, Then PiP enters smoothly with functional controls on 31+, works on 26–30, and state restores on return.
**Tests:** TST-PLAT-01…04. **Confidence:** High.

#### FR-PLAT-002 — Media session and notification
**MUST · R1 · P1**
**Statement:** Media3 MediaSessionService provides metadata/artwork/transport/seek; Bluetooth/Auto/watch callers handled; media notification behaves correctly (exempt from POST_NOTIFICATIONS per platform, but honor user expectations); no stale/ghost sessions after stop or process death.
**AC:** Given playback, Then lock-screen/BT controls operate correctly; after stop, no orphaned session/notification remains (automated check).
**Tests:** TST-PLAT-05…07. **Confidence:** High.

#### FR-PLAT-003 — Foreground service compliance
**MUST · R1 · P0**
**Statement:** Background/audio continuation uses FGS with `mediaPlayback` type + permission; start/stop lifecycle airtight (no `MissingForegroundServiceTypeException`); MUST NOT attempt mediaPlayback FGS from BOOT_COMPLETED (Android 15+ restriction) — no boot auto-resume feature in R1.
**AC:** Given background-audio session, Then FGS active exactly during playback and stops cleanly; boot path triggers no exception/crash.
**Tests:** TST-PLAT-08…10. **Confidence:** High.

#### FR-PLAT-004 — External handoff
**SHOULD · R1 · P2**
**Statement:** "Open with another app" from details/player; optional "share" via platform share sheet (no upload by us); failures return to our player with no state loss.
**AC:** Given handoff, Then target receives correct URI + flags; cancel/failure restores player state.
**Tests:** TST-PLAT-11. **Confidence:** High.

#### FR-PLAT-005 — Screen keep-awake
**MUST · R1 · P1**
**Statement:** Keep screen awake only while video is actively playing/visible; release immediately on pause/exit/error/PiP-minimized; no lingering wake holds after process death (tested).
**AC:** Given playback then pause, Then screen sleeps per system settings; battery protocol (doc 13) shows no post-session wake locks.
**Tests:** TST-PLAT-12…13. **Confidence:** High.

#### FR-PLAT-006 — Store-policy-resilient file access (launch gate)
**MUST · R1 · P0**
**Statement:** The core library experience uses MediaStore + (READ_MEDIA_VIDEO with a Play Console declaration) per platform.md; the app MUST NOT depend on MANAGE_EXTERNAL_STORAGE (Play lists media-file access as an invalid use [V1 platform.md §3]); a fully functional **degraded mode** (Photo Picker + SAF subfolder grants + open-with) MUST exist and be tested for the case where the declaration is denied or the user refuses; capability differences are communicated honestly in-app.
**Need/rationale:** launch feasibility depends on this (RISK register doc 17); JTBD-01/05; JRN-01/06.
**AC:** Given (a) full-access path and (b) simulated denial path, Then both pass the library journey suite (full vs degraded) with no dead ends and honest communication.
**Tests:** TST-PRM-10…14. **Confidence:** Med (policy outcome unknown — launch gate).

### 3.5 FR-SET — Settings, help, recovery

#### FR-SET-001 — Settings information architecture (+ search)
**MUST · R1 · P1**
**Statement:** Settings grouped as: Playback, Controls & Gestures, Subtitles, Audio, Library & Scanning, Privacy, Storage, Diagnostics, About; no duplicated/contradictory settings (governance rule: settings map to FRs); settings search SHOULD exist (name + synonyms).
**AC:** Given any setting, Then it has exactly one home, a plain-language description, and (where it maps to an FR) consistency with that FR's behavior.
**Tests:** TST-SET-01…02. **Confidence:** High.

#### FR-SET-002 — Defaults and reset
**MUST · R1 · P1**
**Statement:** Per-section reset + full reset with explicit two-step confirmation; scoped clearly (settings vs library data vs caches); reset NEVER touches user media files (hash-verified in tests).
**AC:** Given full reset, Then app returns to first-run state; media-file hashes unchanged.
**Tests:** TST-SET-03…04. **Confidence:** High.

#### FR-SET-003 — Import/export settings
**MAY · R2 · P3**
**Statement:** Versioned, human-readable export; import validates and previews changes; unknown keys ignored with a report; no media/library data included (settings only).
**AC:** Given an export from v1 on a fresh install, Then settings import cleanly with a change summary.
**Tests:** TST-SET-05. **Confidence:** Med.

#### FR-SET-004 — Privacy controls
**MUST · R1 · P0**
**Statement:** Diagnostics consent toggle (default OFF per DEC-008) with plain-language "what we would/wouldn't collect"; local-data management (clear history/caches); privacy policy link; "no account" statement; deletion actions delete effectively (verified).
**AC:** Given diagnostics OFF (default), Then network capture shows no telemetry; toggling ON/OFF is reversible and clearly described.
**Tests:** TST-SET-06…08, TST-PRIV-NET-02. **Confidence:** High.

#### FR-SET-005 — Update notes and migration messaging
**SHOULD · R1 · P2**
**Statement:** Plain-language "what's new" after updates; explicit notice + progress when a migration/rebuild runs (JRN-08); skippable; accessible.
**AC:** Given an update with a schema change, Then the user sees a clear notice, migration proceeds without blocking playback, and completion is silent.
**Tests:** TST-SET-09. **Confidence:** High.

#### FR-SET-006 — Permission and capability recovery
**MUST · R1 · P0**
**Statement:** Per-permission status surfaces with OS-settings deep links; capability warnings in honest, non-alarming language (e.g., HDR display limitation); revoked-mid-session messaging with recovery actions; no nagging loops (respect "don't ask again").
**AC:** Given revoked storage access mid-use, Then a single clear message + recovery path appears; no repeated dialogs.
**Tests:** TST-PRM-15…16. **Confidence:** High.

#### FR-SET-007 — Help and troubleshooting
**MUST · R1 · P1**
**Statement:** Offline help covering top tasks + error-code lookup (ties FR-DIAG-002); "how to report a problem" guidance; searchable SHOULD.
**AC:** Given a known error code, Then help search finds the corresponding entry offline.
**Tests:** TST-SET-10. **Confidence:** High.

#### FR-SET-008 — About and licenses
**MUST · R1 · P1**
**Statement:** Version/build info; open-source notices + license texts per doc 11 (Media3/Apache, FFmpeg LGPL + source pointer, libass ISC, OFL fonts); privacy policy; SBOM-generation plan reference.
**AC:** Given the About screen, Then all bundled components from the doc 11 register appear with correct license text; FFmpeg source pointer resolves at release.
**Tests:** TST-SET-11. **Confidence:** High (execution), dependencies on doc 11 sign-off.

### 3.6 FR-DIAG — Diagnostics and reporting

#### FR-DIAG-001 — Playback stats overlay
**MUST · R1 · P1**
**Statement:** Toggleable overlay showing decoder path (hw/platform-sw/fallback), container + video/audio codec ids, resolution/fps, bitrate when known, dropped frames, current A/V offsets, HDR path taken; text selectable.
**AC:** Given playback of each doc-07 tier sample, Then overlay reports the true path/ids (spot-verified against fixtures) with negligible perf cost.
**Tests:** TST-DIAG-01…03. **Confidence:** High.

#### FR-DIAG-002 — Error codes and redacted diagnostics copy
**MUST · R1 · P0**
**Statement:** User-visible, searchable error codes from FR-PLAY-004's taxonomy; "copy diagnostics" produces a versioned, redacted text bundle (app version, device class, codec classes, trace) that EXCLUDES filenames/paths by default; opt-in inclusion of a specific filename requires explicit user action.
**AC:** Given a triggered error, Then copied bundle contains no paths/filenames by default (verified by test fixture) and the code appears in Help.
**Tests:** TST-DIAG-04…06. **Confidence:** High.

#### FR-DIAG-003 — Local logging
**MUST · R1 · P0**
**Statement:** Local ring-buffer logs with retention limits (default ≤7 days / bounded size), no media content, redaction rules applied before display/export; user can view + clear; exports flow only through FR-DIAG-002.
**AC:** Given retention exceeded, Then oldest entries purge automatically; logs never contain subtitle text or file contents (fixture-verified).
**Tests:** TST-DIAG-07…08. **Confidence:** High.

#### FR-DIAG-004 — Report-a-problem workflow
**SHOULD · R1 · P1**
**Statement:** Share-sheet flow with the redacted bundle + optional user description; explains exactly what will be sent before sending; nothing uploads automatically.
**AC:** Given a report flow, Then user sees a preview of the payload and can cancel; no background upload occurs.
**Tests:** TST-DIAG-09. **Confidence:** High.

#### FR-DIAG-005 — Optional quality telemetry
**MAY · R2 · P3**
**Statement:** OFF by default; opt-in per DEC-008; schema limited to approved EV-* events (doc 14) with NO filenames/paths/subtitles/frames/history; revocable; deletion path documented; Play Data safety/Apple labels must exactly match.
**AC:** Given consent OFF, Then no event leaves the device; given ON, Then captured payloads match the doc-14 schema byte-for-byte (fixture test).
**Tests:** TST-DIAG-10…11, TST-PRIV-NET-03. **Confidence:** High (design), pending doc 14.

### 3.7 FR-NET — Network and streaming boundary (no R1 implementation)

#### FR-NET-001 — Boundary requirement
**MUST (as a boundary) · R1 · P0**
**Statement:** R1 MUST NOT ship any network playback surface (no SMB/HTTP/HLS/DASH/RTSP/URL entry hidden or visible). If R2 adopts network playback, scope = SMB2/3 + direct HTTP(S) single-file only, gated by: doc 10 threat-model update, credential-storage design, TLS policy, buffering/retry UX spike, metered-network controls, and licensing review. Rationale for discipline: strongest single evidenced market gap (doc 02 §6 #1, VLC SMB failures) vs the maintenance/security cost of a second playback domain.
**AC (boundary):** Given R1 build, Then network capture shows zero non-store network activity (doc 10 proof test); no network UI exists.
**Tests:** TST-PRIV-NET-04. **Confidence:** High.

#### FR-NET-002 — Direct URL playback
**MAY · R2 · P3**
**Statement:** If adopted: user-initiated direct URL entry only; no auto-discovery, no manifest parsing beyond simple progressive files; same error taxonomy; meter-warning policy.
**AC (placeholder):** defined fully at R2 gate.
**Confidence:** n/a (boundary).

### 3.8 FR-PERS — Personalization/intelligence boundary

#### FR-PERS-001 — AI boundary
**MUST (as a boundary) · R1–R2 · P0**
**Statement:** No AI/intelligent features ship in R1 (DEC-007); R3 candidates (scene indexing, subtitle generation/translation, summaries) each require: on-device-first processing, explicit opt-in, no silent uploads of filenames/subtitles/frames/media, model licensing review (doc 11), battery/thermal budget (doc 05), failure fallback leaving core playback untouched.
**AC (boundary):** Given R1 build, Then no ML-inference user feature exists; any later feature ships with the documented gates.
**Tests:** n/a (governance check). **Confidence:** High.

#### FR-PERS-002 — Non-AI personalization
**SHOULD · R1 · P2**
**Statement:** Ordering in Continue watching/recent lists is deterministic, locally computed, documented, and user-clearable; no behavioral profiling; no recommendations engine in scope.
**AC:** Given history, Then ordering follows the documented rule and "clear history" removes all derived state.
**Tests:** TST-PERS-01. **Confidence:** High.

### 3.9 Part B traceability snapshot

| Requirement | Personas/JTBD | Journeys | KPI | Tests |
|---|---|---|---|---|
| FR-CTRL-005/006 | PER-1/4/5; JTBD-06 | JRN-02 | KPI-SAT-02, KPI-SUP-02 | TST-GEST-* |
| FR-CTRL-017 | PER-1/2; JTBD-02 | JRN-01/02 | KPI-REL-05 | TST-CTRL-25…27 |
| FR-SUB-001…005/009 | PER-1/2; JTBD-03 | JRN-03 | KPI-SAT-02, KPI-SUP-02 | TST-SUB-* |
| FR-AUD-004/005/007 | All; JTBD-10 | JRN-07 | KPI-REL-06 | TST-AUD-* |
| FR-PLAT-001…003 | All | JRN-07 | KPI-REL-06 | TST-PLAT-* |
| FR-PLAT-006 | PER-1/4; JTBD-01 | JRN-01/06 | KPI-ACT-01 | TST-PRM-* |
| FR-SET-004/006 | PER-3/4; JTBD-08 | JRN-06 | KPI-REL-*, KPI-SAT-* | TST-SET-*, TST-PRM-* |
| FR-DIAG-002/003/004 | PER-2; JTBD-04 | JRN-05 | KPI-SUP-01 | TST-DIAG-* |
| FR-NET-001 / FR-PERS-001 | (boundary) | — | (proofs) TST-PRIV-NET-* | doc 10/18 |

## 4. Traceability snapshot (Parts A+B; full matrix in doc 18)

| Requirement | Personas/JTBD | Journeys | KPI | Tests (doc 12) |
|---|---|---|---|---|
| FR-LIB-002/003 | PER-4, PER-1; JTBD-08 | JRN-01/06 | KPI-ACT-01 | TST-PRM-* |
| FR-LIB-004/006 | PER-1/2; JTBD-05 | JRN-04 | KPI-PERF-03 | TST-SCAN-* |
| FR-LIB-015 | All; JTBD-02 | JRN-02/07 | KPI-REL-05 | TST-RSUM-* |
| FR-PLAY-003/004 | PER-1/2; JTBD-01/04 | JRN-05 | KPI-REL-01/02 | TST-DEC-*, TST-ERR-* |
| FR-PLAY-015/016 | All; JTBD-09 | JRN-07 | KPI-REL-05/06 | TST-REST-* |
| FR-LIB-020 | PER-1/4; JTBD-11 | JRN-01 | KPI-ACT-01 | TST-OPEN-* |
| FR-CTRL-017 | PER-1/2; JTBD-02 | JRN-01/02 | KPI-REL-05 | TST-QUEUE-* |

## 5. Open questions

| # | Question | Owner doc |
|---|---|---|
| Q1 | Final scan/enumerate wiring (MediaStore-first model set by W2) | 09 |
| Q2 | FFmpeg **audio**-extension build config (LGPL-2.1 self-build; decoder list) + legal sign-offs ⚖️ | 11 (RQ-02) |
| Q3 | Identity heuristics for moved files | 09 |
| Q4 | HDR/10-bit per-device class policy finalization | 07 (RQ-03/RQ-12) |
| Q5 | Refresh-rate switch safe-list strategy viability | 08 (RQ-12) |
| Q6 | Gesture false-positive target feasibility on OEM-skinned devices | 13 |
| Q7 | ASS styling policy (user override vs embedded styling) + fidelity ceiling | 07 (RQ-06) |
| Q8 | PGS/image-subtitle support tier (engine-dependent) | 07 |
| Q9 | "Continue as audio" demand (default-off; validate) | 03/05 |
| Q10 | READ_MEDIA_VIDEO declaration outcome (launch gate) + degraded-mode acceptance | 15/17 |
| Q11 | AC-3/E-AC-3 decode add-back after legal recheck ⚖️ | 11 |
| Q12 | **CLOSED 2026-10-19** — auto-advance default settled by evidence: **ON when a next item exists**, 10 s cancellable countdown, 2 h inactivity guard, stop-only when no queue. See `research/autoplay_defaults.md` and FR-CTRL-017 | 02A / research W6 |
| Q13 | **CLOSED 2026-10-19** — the framing was a false dichotomy. Practice is **technical name + when-to-use description**, `Auto` as default, a global setting *and* a per-item override; bare `HW/SW` tokens are forbidden as the only label. See `research/decoder_label_practice.md`, FR-PLAY-003, doc 06 SCR-010e | 02A CS-4 / research W7 |

## 6. Sources

- **Platform behavior (Parts A+B):** `research/platform.md` (accessed 2026-10-18) — Media3 supported formats [A1]; FFmpeg extension & licensing [A26/A27]; Play policies: photo/video permissions [A19], all-files access [A18], data safety [A22], vitals [A20], target API [A17]; FGS/PiP/audio-focus/notification behavior [A11–A16]; CDD decode mandates [A4/A5]; iOS decision facts [I1/I2/I3/I8/I9]. Full URLs in the research file.
- **Licensing constraints (FR-PLAY-003, FR-AUD-007, FR-SUB-003, FR-SET-008):** `research/licensing.md` (accessed 2026-10-18) — LGPL-2.1 build flags ⚖️, FFmpegKit retirement, audio-only Media3 extension, AC-3/E-AC-3 patent status, DTS/TrueHD passthrough rule, OFL fonts, SBOM plan.
- **Competitive context:** `research/competitors.md` via doc 02 (W1).
- Items marked ⚖️ require qualified legal review before any shipping decision (doc 11 register).
