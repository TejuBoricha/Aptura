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

### Changed

- Agent coordination, after an end-to-end audit: the status check reads comments repository-wide (37 requests a run for 30 open and 40 closed pull requests, down from 215), flags claimed tasks without a pull request after 2 hours and quiet pull requests after 4, escalates any stall left a day to the owner, emails the owner only for new "Needs you" items, reports unreadable messages, and keeps working when one write fails. `inbox.py` falls back to the GitHub CLI's login and slows down without a token. `AGENTS.md` gains an everyday-messages cheat sheet, and drift tests keep the docs and the code in step.

- The sleep timer now keeps one absolute deadline instead of subtracting ticks (no drift), derives its countdown only while a screen shows it, and offers the full 10/15/20/30/45/60 minute ladder plus a custom duration. After it fires, `+5 min` resumes playback with a fresh five minutes.
- A/V sync previews instantly but waits for the drag to stop before editing the audio stream or writing the database; a pending edit is cancelled when the video changes or the preferences are reset.
- Delete dialogs now make **Move to trash** the primary action and **Delete permanently** a red, explicitly secondary choice. When the platform cannot trash a file, permanent deletion remains the only, destructive action.
- `ApturaDialog` accepts an independent `iconTone`; `DialogAlternative` accepts a tone, so a non-destructive primary action can still show a red warning icon.
- README rewritten around the product, privacy guarantees and verification evidence.
- UI/UX review updated with the merged commit range, the full gate result and the current evidence index.

### Fixed

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
