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

- **Going to a screen and back no longer cross-fades.** Navigation 3's defaults are a 700 ms cross-fade (both screens half see-through at once) and, for the Back gesture, a page that shrinks and then vanishes while still opaque. Screens now slide over each other, solid the whole time, in 300 ms; the Back gesture drags the page with the finger and letting go carries it on to the edge.
- **The folder swipe is quicker and answers sooner, with the same slide and fade.** The new folder is complete after 144 ms instead of 240 ms. The swipe counts as soon as the finger has travelled 48 dp (it waited for the finger to lift after 64 dp), and a short quick flick works too.
- **The View options sheet looks calmer and says more.** Layout is two picture cards (a small drawn grid and list), Size and Details are tinted switches in the same language as the folder chips instead of three stacked, fully filled bars, and the chosen sort is tinted. The welcome guide's Grid/List switch matches.
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

- **A video that did not start left a black, paused player with no message** (the owner's phone: every video after the first sat in "buffering" at 0:00). A player that is asked to play and does not start within 4 seconds is now **replaced by a brand-new one** that starts the video where it was, with all settings carried over; if that does not start either, another new player with the software decoder is tried; only then does the player say "Playback Did Not Start", with Retry that starts on a new player. A player that times out is replaced at once, and a slow load that is still bringing in data is given up to three extra waits instead of being replaced. The cause that fits the phone best is also prevented: leaving the player now lets the video decoder go before the picture's surface is destroyed, instead of only pausing it with the decoder still running (on some phones detaching a surface from a running decoder freezes the player for good). After one such recovery every later start in that session begins on a new player, so the stall does not have to happen again on every video. The last attempt plays only the video that was asked for, without its queue. New connected tests wedge a real player with a source that never delivers a byte and require the video to play on the replacement, and play a second video after the first, which nothing in the gate did before.
- **Fast scrolling through a big library felt laggy** (the owner's 558 videos). Every cell that scrolled into view started reading or making its picture immediately, without limit and without pausing for cells that were only flashing past: a hard fling started 212 pieces of picture work and up to 30 ran together. Nothing is started while the grid is being flung now; the cells on screen get their pictures when it slows down, at most three at a time and at background priority. A hard fling starts at most 10 pieces of work (212 before) and 3 run together (30 before).
- **A stopped player was treated as a stalled one.** The start watch also counted an idle player that still had play-when-ready on (after a stop from outside), and would have started it again; it now waits only for a player that is buffering or ready.
- **The Folders sheet (and every other bottom sheet) "danced" when it was flicked upward** (the owner's phone, three rounds). A sheet settles with the speed the finger had, and a spring that starts at the top with speed away from it swings past and back: the whole sheet rose 30 to 85 dp and dropped again. The first fix covered only a long list flicked to its end; the second took upward finger movement away from the sheet, which made Android cancel taps and scrolls inside it (the owner: "touch not responsive, scrolling sometimes does not react"). Now nothing the finger does on a sheet's content reaches the sheet: lists scroll and buttons tap freely and the sheet does not move. A sheet closes by its handle strip (swipe down or tap), a tap outside it, or Back. `ApturaSheetTest` measures the heading frame by frame, checks that a tap that drifts upward still taps and that repeated flicks still scroll, and fails if any sheet is opened without the wrapper.
- **When playback stalls, the diagnostic report now says where it stopped.** The stall record carries the player's last steps with times (requested, state changes, loads started and finished or failed, tracks found, video and audio renderers switched on, audio moving, surface size, first frame), how much was buffered and whether it was loading, and the playback threads' states, so a report from a phone shows the cause instead of "buffering".
- **Back navigation flickered on every screen** (the owner's phone). The cause was Navigation 3's default 700 ms cross-fade and, for the Back gesture, a shrunken page that was still opaque when it vanished. See Changed.
- **The welcome guide hitched at every page turn.** Each page was built on the frame the swipe began and thrown away when left. The pages are now built one at a time behind the first one and kept (the slowest frame of a page turn went from 40-128 ms to 13 ms in a measured lap), and the welcome rings move only on the page in front.
- **Access to videos granted on a phone, and the library stayed empty until the app was closed and opened again, when it asked again.** The app now reads the system's answer every time it comes to the front, a grant scans at once and again five seconds later, and Settings > Library & storage has a **Video access** row to look at, give or take back access (and a settings shortcut when the system no longer shows its dialog).
- **The flicker when going back from the player** in the Light theme. The app kept one global colour palette and switched it to dark a frame after the player opened or closed, so the library briefly turned dark on the way in and the player's controls briefly turned white on the way out. Each screen now carries its own palette, the player is never animated (its picture cannot follow a slide or fade) and a cover above the picture does the opening and closing, system bars and insets no longer change under a leaving screen, and returning thumbnails paint in the first frame. Reproduced and verified frame by frame under Robolectric; not yet checked on a device.
- **After a first install the library flickered through three screens while the first scan ran:** a flash of grey loading blocks for a few milliseconds, then "No videos yet" with a Scan device button, then the videos. It now shows one calm "Looking for videos…" screen until the scan ends. "No videos yet" appears only if the library really is empty after the scan, and the loading blocks appear only when loading lasts longer than 300 ms.
- **Audio-only mode drew the transport buttons over the headphone artwork and the video's title** (a portrait screenshot from the owner's phone). The panel now has its own space between the top bar and the buttons, becomes one row when that space is short, and scrolls rather than overlap.
- **On a phone held sideways the library used the tablet sidebar,** which needs about 450 dp before its first folder row, so there was no folder list and no way to add a folder; and the narrow rail did not fit 360 dp, cutting off its Settings button. Below 600 dp of height the library now uses the rail with the folder bar, and below 480 dp the rail drops its text labels.
- **The search hint was cut to "Search vi…" on a 360 dp phone,** because the field shares its row with three buttons. It now says "Search videos" when that fits, "Search" when it does not, and nothing on a very narrow phone, never a cut word.
- **The stats panel covered the transport buttons and the seek bar** on a phone held sideways (and touched the play button upright), and swallowed taps meant for them. While it is open the buttons move to the left, the panel stops above the seek bar (or the buttons, upright) and scrolls inside.
- **On a phone held sideways Settings spent a third of the screen on a large title,** and the Continue watching cards were 140 dp tall pictures that pushed the library out of view. Settings now shows only its compact bar on a short screen, and the cards are smaller there.
- **Cold start:** the colour behind the first frame was always dark, so a phone set to Light opened on dark and turned light; and someone who set the app to Dark on a Light phone saw a light frame while the saved theme was being read. The launch colour now follows the phone's mode and the first frame waits (at most 0.7 s) for the saved theme.
- **Folders with the same name were one folder.** The library grouped by the folder's name, so DCIM/Camera on the phone and DCIM/Camera on an SD card, or two Download folders, were mixed together with one count and one cover. Folders are now identified by where they are (volume and path, the way Photos and Files do it) and a name that two folders share is shown with what separates them ("Camera (DCIM, SD card)"). Searching now matches the folder name you see rather than its storage path. Nothing is lost: the next scan moves each video to its folder and keeps its progress and hidden state.
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
