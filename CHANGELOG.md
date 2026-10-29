# Changelog

All notable changes to Aptura are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

- Public project documentation: architecture, build, testing and privacy guides.
- Apache-2.0 license, third-party notice, contribution guide, security policy and code of conduct.
- GitHub issue forms and a pull-request template.
- Agent coordination checks in CI (`tools/agents/pr_checks.py`): every commit must name the AI that helped or say a person wrote it, only one open pull request may change a hot file such as the Room schema, and one task gets one pull request. Permission errors fail loudly; a GitHub outage only warns.
- `AGENTS.md`, `.agents/STATE.md` and `docs/AGENT_WORKFLOW.md`: one set of instructions, current state and handoff procedure shared by every AI coding agent, with an agent-task issue form and a Handoff section in the PR template.
- Agent coordination without waiting on any one agent: every wait has a deadline and a fallback, the independent reviewer is a role filled by any other model when the one asked is away, and an **Agent status** workflow (`tools/agents/stalls.py`, every 15 minutes, no AI model, no secret) announces overdue waits on the pull request and keeps a summary issue that opens with what needs the owner. A **Debate** issue form and procedure let agents propose, critique blind and decide by evidence, with word limits, two rounds and a 24-hour time box. Agent messages and stall checks now ignore comments from anyone but the owner's account. When the owner tells an agent to skip a step, an `Override:` record (the owner's words, reason, scope and follow-up) tells the other agents it was intended, and the status issue lists it until the follow-up is done.
- New launcher mark: a cyan aperture disc with a play-triangle opening and three curved iris seams.
- Library layouts you can choose: Grid or List, three picture sizes, three levels of detail (Title only, Standard, Detailed) and the sort order, in one **View options** sheet that remembers your choice. The grid fits as many columns as the screen has room for.
- A **folder bar** with a Browse folders button and an Add folder button that stay in place while the folder chips scroll, and a **folder browser** that shows every folder at once as rows or cover cards with their video count and runtime.
- Swiping between folders now shows where you landed ("Movies, 3 of 6"), keeps the folder bar and sidebar on the current folder, and ticks with a light haptic.
- The welcome guide is redesigned. Its library page is built from the real header, folder bar and video cards with a Grid/List switch you can try; its player page plays through the real brightness, volume, double-tap and scrub indicators; its tools page has the real speed control. Pages build themselves in as you swipe, a glow drifts behind them, and a step count sits beside Skip. When the guide is replayed from Settings it shows your own videos and folders.

### Changed

- An independent review by a second model is now required only for security, privacy and database changes. Playback, file operations and large changes merge on the gate and green CI alone.
- Agent coordination, after an end-to-end audit: the status check reads comments repository-wide (37 requests a run for 30 open and 40 closed pull requests, down from 215), flags claimed tasks without a pull request after 2 hours and quiet pull requests after 4, escalates any stall left a day to the owner, emails the owner only for new "Needs you" items, reports unreadable messages, and keeps working when one write fails. `inbox.py` falls back to the GitHub CLI's login and slows down without a token. `AGENTS.md` gains an everyday-messages cheat sheet, and drift tests keep the docs and the code in step.
- The Agent status check now catches silence: a run started by a comment that sees a plain `To:` deadline within 35 minutes waits for it and checks again, so a wait nobody answers is announced about half an hour after it was posted even when every agent then goes quiet. A run the 5-minute throttle would skip waits out the gap instead; scheduled and manual runs are never throttled.

- The sleep timer now keeps one absolute deadline instead of subtracting ticks (no drift), derives its countdown only while a screen shows it, and offers the full 10/15/20/30/45/60 minute ladder plus a custom duration. After it fires, `+5 min` resumes playback with a fresh five minutes.
- A/V sync previews instantly but waits for the drag to stop before editing the audio stream or writing the database; a pending edit is cancelled when the video changes or the preferences are reset.
- Delete dialogs now make **Move to trash** the primary action and **Delete permanently** a red, explicitly secondary choice. When the platform cannot trash a file, permanent deletion remains the only, destructive action.
- `ApturaDialog` accepts an independent `iconTone`; `DialogAlternative` accepts a tone, so a non-destructive primary action can still show a red warning icon.
- README rewritten around the product, privacy guarantees and verification evidence.
- UI/UX review updated with the merged commit range, the full gate result and the current evidence index.

### Fixed

- **The flicker when going back from the player** in the Light theme. The app kept one global colour palette and switched it to dark a frame after the player opened or closed, so the library briefly turned dark on the way in and the player's controls briefly turned white on the way out. Each screen now carries its own palette, the player is never animated (its picture cannot follow a slide or fade) and a cover above the picture does the opening and closing, system bars and insets no longer change under a leaving screen, and returning thumbnails paint in the first frame. Reproduced and verified frame by frame under Robolectric; not yet checked on a device.
- **After a first install the library flickered through three screens while the first scan ran:** a flash of grey loading blocks for a few milliseconds, then "No videos yet" with a Scan device button, then the videos. It now shows one calm "Looking for videos…" screen until the scan ends. "No videos yet" appears only if the library really is empty after the scan, and the loading blocks appear only when loading lasts longer than 300 ms.
- **Audio-only mode drew the transport buttons over the headphone artwork and the video's title** (a portrait screenshot from the owner's phone). The panel now has its own space between the top bar and the buttons, becomes one row when that space is short, and scrolls rather than overlap.
- **On a phone held sideways the library used the tablet sidebar,** which needs about 450 dp before its first folder row, so there was no folder list and no way to add a folder; and the narrow rail did not fit 360 dp, cutting off its Settings button. Below 600 dp of height the library now uses the rail with the folder bar, and below 480 dp the rail drops its text labels.
- The **View options sheet** did not scroll, so on a short screen the sort order below the fold could not be reached. On the permission and empty screens the main button sat at the very edge of a sideways phone; the spacing is now tighter there.
- The tablet sidebar cut a folder row through its text where it scrolled under the library summary card, and a folder reached by swiping could be off screen in the sidebar. Rows now dissolve at the list edges and the selected row scrolls into view.
- The welcome guide described a ⋮ menu that did not exist ("play audio only", "open in another player", "rename"), used capitalisation the app does not use ("Continue Watching", "Welcome Guide") and showed flat grey tiles as thumbnails.
- The sleep timer's fade is exactly 10 seconds in ten equal steps; `SectionTitle` no longer halves a long title when a trailing action is present (the Library count stays next to its title, and a trailing action sits at the row's end); decimal separators in sync offsets no longer follow the device locale; the player sheet tabs form a selectable group for TalkBack; a sync drag that ends just before the player closes is still applied and saved.
- `inbox.py` no longer dies mid-wait on a message the console cannot encode: output is forced to UTF-8 with lossy substitution, so a Windows cp1252 console prints a replacement character instead of exiting 1 (which a waiting agent would read as "the script is broken", not "a message is waiting").
- Stale player-sheet and delete-dialog evidence replaced with renders from the current code, including after-only evidence where the old "before" state no longer exists.

### Security

- Agent coordination: a stranger's pull request can no longer block agent work, comments are read only from the owner's account (plus the status check's own marked nudges), markers inside code blocks no longer count, and pull requests are judged by the base branch's copy of the coordination rules. See `review/audit-2026-10-29/PROCESS_AUDIT.md`.

## 1.0.0 - 2026-10-28

Pre-release baseline. Not distributed through an app store.

### Added

- Offline-first local video library backed by MediaStore and SAF folder access.
- Incremental WorkManager scanning, reconciliation of moved/renamed files, and preservation of user progress.
- Adaptive Compose library with search, category filters, sorting, multi-select, rename, share, move, hide and delete flows.
- Media3 player with hardware decoding, decoder strategy, PiP, gestures, audio-only mode, orientation lock and playback statistics.
- Audio track selection, five-band equalizer and presets, bass boost, virtualizer, dialogue boost and audio sync.
- Subtitle tracks, sidecar import, timing, tap-to-sync, styling and a browser hand-off for online search.
- Playback speed, sleep timer with fade, "+5 min" extension, end-of-video stop, A-B repeat, frame stepping and bookmarks.
- Picture controls: aspect ratio, night mode, contrast, colour and decoder selection.
- Shared design kit and one player-sheet chrome across every sheet and tab.
- Dark and Light app themes; the player and its sheets remain dark in both.
- Local diagnostics with redaction, export and delete.
- JVM, Robolectric and instrumented test suites, a 97-suite gate, and release-build checks for network and telemetry code.
- GitHub Actions CI: JVM verification and API 35 tablet instrumentation.

### Security

- No `INTERNET` permission, no telemetry, no ads and no accounts.
- Scoped storage only; no `MANAGE_EXTERNAL_STORAGE`.
- Android backup disabled.
- Release builds fail if a network stack or telemetry SDK survives R8.
