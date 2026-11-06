# Aptura: End-to-End Project Audit

Date: 2026-11-05. Audited commit: `b4de60b` on `main`. Author: Claude Opus 5.5, for the owner, Tejas Gogara.

> **STATUS: ACTIVE. This is the only current audit of Aptura.** All earlier audits were removed from the repository because they are superseded. If another document or note disagrees with this file about what is open, this file wins, except that `.agents/STATE.md` wins on project facts.

## How to work from this audit

- **Order:** work the roadmap in phases, starting with Phase 0. Finding IDs such as `STAB-01` are stable, so use them in branch names, PR titles and commits.
- **Who starts work:** this is the backlog, not a work order. Take work only as `AGENTS.md` rule 1 says: from the owner directly, or from an issue labelled `agent-ready`.
- **Check first:** this is a snapshot of commit `b4de60b`. Before fixing a finding, confirm it still holds in the current code. Labels Traced and Suspected mean the path was not run.
- **Record progress:** when a PR fixes a finding, add a row to the table below in the same PR. A finding is closed only by a test or device evidence named in that row. Do not edit the findings themselves, so the baseline stays comparable.
- **New audits:** there is only ever one audit file. A new audit replaces this one, and open items carry over explicitly.

## Progress

| Finding | Status | PR | Evidence |
| --- | --- | --- | --- |
| DEV-03 | Done in a minimal form. The 15-minute status schedule is removed, so the bot no longer announces overnight silence. A process freeze note is in `.agents/STATE.md`. Kept: `pr_checks.py`, the PR template, attribution trailers and independent review for security, privacy and Room changes. Debates are paused, not deleted. Open: the owner decides later whether to remove the debate and status code for good. | This PR | The workflow no longer has a `schedule:` trigger; `python -m unittest discover -s tools/agents` passes; the wording in `AGENTS.md`, `STATE.md`, `AGENT_WORKFLOW.md` and `stalls.py` matches |
| DEV-04 | Mostly done: evidence removed from `main`. The older history stays in the git pack. | This consolidation | Removed files listed under "Earlier audits" at the end |
| SEC-01 | Done in code, one owner step left. `.gitignore` covers `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `*.pem`, `*.p8`, `*.key`, SSH keys and `.env` files. CI now fails any push or pull request that tracks such a file (`tools/security/check_no_secret_files.py`, which refuses to pass if it cannot list files). The keystore guide now says to keep the key outside the repository. Open: the owner moves any existing upload key out of the repository folder. The check sees the current tree only, so a key that was ever pushed must be replaced. | [#54](https://github.com/TejuBoricha/Aptura/pull/54), merged; [#56](https://github.com/TejuBoricha/Aptura/pull/56) | 9 tests pass, including a force-added `.jks` that makes the check exit 1 and name the file; the check exits 0 on the 492 files tracked today; `.env.example` stays allowed |

## Executive summary

**Aptura is not ready for a public Play launch, but it is close and well worth finishing.** The engineering base and privacy enforcement are unusually strong. The problems are a small set of real playback and data bugs, overstated release evidence, and no localization or non-touch input.

This audit covers `main` at commit `b4de60b`. It logged 1 Critical, 21 High, 43 Medium and 16 Low findings, plus 12 ranked product gaps. Every baseline check passed, and those checks caught none of the bugs below, so a green gate does not mean the app works.

**The five findings that matter most:**

1. **The player dies after a short pause in the background (STAB-01, reproduced).** Pause, press Home, wait 90 seconds and return. Play does nothing, and new videos fail with a false "Software Decoder" banner until the process is killed. At 30 seconds it works.
2. **Swipe-seek jumps backward (FUNC-01, reproduced).** After 42 seconds of playback, a small forward swipe landed at 7.1 seconds, in two runs out of two.
3. **Importing a subtitle silently does nothing (FUNC-02, reproduced).** The picker returns, the sheet still says "Showing: Off", no track appears and no error shows.
4. **Moving a video by copy-then-delete erases its progress and bookmarks (DATA-01, traced in code).** Scans can also overwrite recent changes and clear hidden flags (DATA-04, DATA-05).
5. **The release story is not true yet (SHIP-01 to SHIP-04).** The store listing says "zero permissions" while the APK requests seven, and it says speeds reach 3.0x while the code stops at 2.0x. The device checklist ticks TalkBack as PASS on the strength of a JVM test. A v1.0.0 "Official Production Release" is already public.

**Scorecard.** These grades are my judgement against a polished production app, not measurements.

| Dimension | Grade | Why |
| --- | --- | --- |
| Privacy enforcement | A- | Manifest and dependency allow-lists run in CI. Diagnostic redaction leaks names (PRIV-01). |
| Security and CI | B+ | No Critical issue, least-privilege workflows, and no secrets found in history. FFmpeg is old and there are no dependency alerts. |
| Architecture | B | Clean layers and a pure domain, but three very large files carry too much. |
| Stress stability | B+ | 10,097 monkey events caused no Aptura crash or ANR. |
| Lifecycle reliability | D | STAB-01, plus state owned by the wrong layer (FUNC-05). |
| Functional correctness | C | Common flows work, but several tools fail silently. |
| Data integrity | C+ | The transfer journal is careful. Identity and scan merging are not. |
| Performance | B | 3,037 videos cost 67 MB and the APK is 4.8 MB, but nothing is profiled on real hardware. |
| Visual design | B+ | Restrained and consistent; the store assets let it down. |
| Accessibility | D+ | Good semantics when controls show; nothing is reachable when they hide; no keyboard. |
| Internationalization | F | One string resource and about 972 hard-coded literals. |
| Testing | B- | 997 tests with real engines, but thin on lifecycle and the shipped variant. |
| Release readiness | D | False listing text, overstated evidence and no release workflow. |

**Recommendation:** freeze features and process work. Work through Phases 0 to 2 of the roadmap below, redo the device pass honestly, then launch. I estimate 4 to 6 focused weeks excluding localization, and that estimate is untested.

## Architecture and technical maturity

**The structure is sound and needs refactoring at three seams, not a rewrite.** Aptura is a single-module Android app in Kotlin with Compose, Media3, Room, WorkManager and DataStore. It has 136 production source files with 32,478 lines, and 149 test files with 23,294 lines.

**Architecture diagram, as text.** Finding: the layers are clean, but playback state straddles the service and the ViewModel.

- **UI (Compose, Navigation 3):** `Navigation.kt` 1,033 lines, `MediaLibraryScreen` 1,881, `PlayerScreen` 2,265, `SettingsScreen` 1,048. Adaptive grid, rail and sidebar. Fixed dark player. No string resources or key handling.
  - Calls down to ViewModels.
- **ViewModels:** `PlayerViewModel`, 1,579 lines, saves progress and owns item preferences, Up Next and audio effects.
  - Calls down to Domain.
  - **Failing boundary:** holds a handle to the player owned by the service layer below.
- **Playback service and PlayerManager (the failing boundary):** owns one player for the process. `onDestroy` releases it while the ViewModel keeps the handle.
- **Domain (pure Kotlin):** models, repository interfaces and use cases. No Android imports, enforced by tests.
- **Data (implements Domain):** Room schema 6, DataStore, scan and transfer workers, `Media3PlaybackController` 1,333 lines.
- **Core (used by Domain-facing code):** file transfer, thumbnails, subtitle validation, diagnostics and redaction, platform helpers.
- **Android platform (below Data and Core):** MediaStore, SAF, WorkManager, Media3 1.5.1, FFmpeg 6.0 audio decoders, MediaCodec.

The red boundary explains most of the serious playback bugs. The service can destroy the player the ViewModel still holds (STAB-01). The ViewModel can die while playback continues without it (FUNC-05).

**Maturity by area:**

| Area | Level | Evidence |
| --- | --- | --- |
| Layering and invariants | Strong | Pure `domain/`, and tests read the merged release manifest and resolved dependencies. |
| Failure handling | Good | Typed error taxonomy, decoder fallback, and all 6 unplayable test files showed an error card rather than a crash. |
| Lifecycle ownership | Weak | Process-scoped player and ViewModel-scoped behaviour are not reconciled. |
| Data model | Moderate | Migrations 2 to 6 chained, but file identity across moves is not modelled and `uri` has no index. |
| Testability | Moderate | Good fakes, but fakes cannot catch ExoPlayer behaviour (FUNC-02, FUNC-04). |
| Release engineering | Weak | No release workflow, and `versionCode = 1` is hard-coded. |

**Recommended seams, lowest risk first.** A pure `PlaybackFailurePolicy` moves error and watchdog decisions out of the controller. A process-scoped `PlaybackSessionCoordinator` takes progress, item preferences, Up Next and effects from the ViewModel. A `QueueSubtitleManager` isolates subtitle rebuilds. Finally, split `PlayerScreen` into gesture, top bar, transport and error parts, with a `PlayerActions` holder replacing its 80+ parameters.

## Audit coverage checklist

**The audit read the production code broadly and ran the release build on one emulator, but it did not touch real hardware, a screen reader or a removable volume.** Six specialist reviews covered the code, and I ran the live tests myself.

**How to read the evidence labels used in this report:**

- **Reproduced:** I saw it happen on the emulator or in a tool's output.
- **Traced:** a reviewer followed the code path, but it was not run. I re-read the key lines for STAB-01, FUNC-01, DATA-01, DATA-02, DATA-04, PRIV-01 and SHIP-01.
- **Observed:** seen in a screenshot, document or GitHub record.
- **Suspected:** a plausible risk that needs a device or more work.
- **Subjective:** a design opinion.

| Area | Status | What was done | Left open |
| --- | --- | --- | --- |
| Repo, docs, specs | Inspected | State, decisions and release kit, and sampled specs | Not every spec read line by line |
| Playback and player | Inspected, tested | Engine, service, ViewModel; 13-file format matrix | Audio sync not measured by ear or instrument |
| Library, scanner, Room | Inspected, tested | Schemas, scan paths, 3,037-video library | Details sheet and folder navigation only partly read |
| File transfer | Inspected | Move, copy, rename, delete paths | Not run: needs SD card and kill tests |
| Security and CI | Inspected | Manifest, intents, workflows, git history, ruleset | FFmpeg CVE reachability; released APK signatures |
| Privacy | Inspected, tested | Merged permissions dumped, redaction regex, policy text | Device-to-device transfer behaviour |
| UI and visual | Partial | Code, stored captures, about 20 live screens | Settings and onboarding not fully walked live |
| Accessibility | Partial | Accessibility tree, contrast maths, keyboard events | **TalkBack never run** |
| Internationalization | Tested | String counts; Arabic locale on the player | Gesture direction under RTL inconclusive |
| Performance | Partial | Cold start, memory, frame timing on an emulator | **No real-device or thermal numbers** |
| Tests and CI | Inspected, run | Unit, lint, release and 147 tool tests; last 100 CI runs | Connected suite read from CI, not run locally |
| Dependencies | Inspected | Versions against Maven; 16 KB alignment checked | No CVE scanner run across the full graph |
| Competitors | Researched | 6 players, from pages opened on 2026-11-05 | Some matrix cells from memory, marked there |
| Backend, API, auth, servers | Not applicable | The app has no backend, accounts or network permission | None |

## Baseline evidence

**Every automated gate passes at `b4de60b`, and no product code was changed by this audit.** The build ran from a clean worktree in 5 minutes 34 seconds. Test media and captures stayed in a temporary folder.

| Check | Result | Note |
| --- | --- | --- |
| Unit and Robolectric | 997 tests, 0 failures, 165 skipped | All skips are gallery tests that need an environment variable |
| Android lint | 0 errors, 68 warnings, 1 hint | Includes `SelectedPhotoAccess` and `DataExtractionRules`, both relevant below |
| Release build and `verifyReleaseApk` | Pass | 5,708 kept classes and 4 dex files checked against 21 forbidden packages |
| Agent tool tests | 147 pass in 0.8 s |  |
| 16 KB page alignment | Pass | `zipalign -c -P 16` passes on the arm64 APK, including `libffmpegJNI.so` |
| Release APK size | arm64 4.85 MB, universal 9.38 MB |  |
| CI on `main`, 29 Oct to 4 Nov | 26 of 26 green | Median 7.6 minutes, one likely flaky test since stabilised |
| Requested permissions | 7 | `READ_MEDIA_VIDEO`, `READ_EXTERNAL_STORAGE` (up to API 32), 2 foreground service, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`. No `INTERNET`. |

**Live test results.** Pixel 8 emulator, API 35, software rendering, R8 release build signed with the local debug key. Its timings do not represent a real phone.

| Test | Result |
| --- | --- |
| Plays: H.264/AAC, AV1/Opus, VP9/Vorbis, Xvid/MP3 in AVI, H.264/DTS, H.264 with ASS track, odd Unicode file name | All reached playing state |
| Fails with an error card: random bytes, zero bytes, truncated MP4, WMV2, MPEG-2 PS, 10-bit HEVC | No crash. The emulator has no MPEG-2 or suitable 10-bit HEVC decoder, so these say little about real phones. |
| Automatic sidecar `.srt` | Works: "Auto-paired", and shown in the track list |
| Monkey stress, 3 seeds | 10,097 events, no Aptura crash or ANR. One run stopped on a launcher crash, not Aptura. |
| Cold start, 5 runs on first install | 1,140 to 1,687 ms |
| Cold start with 3,037 videos, 3 runs | 613 to 779 ms |
| Memory with 3,037 videos | 53 MB PSS, 67 MB after fast scrolling |
| Fast scrolling with 3,037 videos | 12% janky, 31 ms median frame, under software rendering |
| Dark mode and font-scale change while paused | Stayed paused at the same position |

The repo's earlier audit files and `docs/TESTING.md` report 682 to 694 tests with 78 skipped. Those numbers are stale, and I did not rely on them.

## Findings register

**Fix STAB-01 first: it is the only Critical finding, it was reproduced, and a user can hit it by simply pausing and leaving the app.** Critical and High findings have full cards, while Medium and Low findings are in tables. Paths are relative to `app/src/main/java/org/aptura/player/`.

Severity is how bad the problem is. The Phase in each card is when to do it, so a severe bug and an easy win are not ranked alike. Effort: S is under a day, M is 1 to 3 days, L is more than 3 days.

### Critical

#### STAB-01: Player dies after about 90 seconds paused in the background

**Reliability · Reproduced · Effort M · Phase 0**

- **Evidence:** the service was gone and Play was dead after 90 s and 150 s in the background. After 30 s, playback resumed. Logcat showed `Handler ... sending message to a Handler on a dead thread` and `Ignoring messages sent after release`.
- **Consequence:** opening another video in the same process fails to start. It shows a false "Playing in compatibility mode (Software Decoder)" banner and a seek bar reading 0:56 of 0:30. Only killing the process recovers.
- **Root cause:** `ApturaPlaybackService.onDestroy` calls `PlayerManager.release()`, which frees the player and nulls the instance. The ViewModel and `Navigation.kt` still hold the old controller, and nothing rebinds. Android stops the idle service after playback leaves the foreground.
- **Impact:** every user who pauses and returns later. The reviewer suspects this also explains the unexplained stall reported on the Samsung S24, which is unproven.
- **Fix:** keep the player for the process and release only the session in `onDestroy`. Recreate the service and session on `ON_START` and in `playQueue`. Release the player only on an explicit exit.
- **Risks:** a process-long player holds codec resources, so it must drop them when stopped. Touches sleep timer and PiP paths.
- **Validate:** instrumented test: pause, background, force the service to stop, return, then Play advances the position and a second video starts. Repeat on a real phone.

### High: playback and player

#### FUNC-01: Swipe-seek starts from a stale position

**Functionality · Reproduced · Effort S · Phase 0**

- **Evidence:** on two fresh 90-second files, after 42 s of hidden-controls playback, a 60-pixel forward swipe landed at 7,096 ms and 7,026 ms.
- **Root cause:** `ui/player/PlayerViewModel.kt:860` seeds the swipe from `effectivePositionMs`. Lines 327 to 333 stop updating that value while controls are hidden, for performance.
- **Impact:** the main seek gesture throws people back, by as much as the whole film. A bad first impression and a likely one-star review.
- **Fix:** seed from `lastKnownPositionMs`, as `seekRelative` already does.
- **Risks:** low.
- **Validate:** a ViewModel regression test with hidden controls, then repeat the swipe on a device.

#### FUNC-02: Importing a subtitle silently does nothing; queue sidecars never load

**Functionality · Import reproduced, root cause traced in bytecode · Effort S to M · Phase 0**

- **Evidence:** I imported a valid `.srt` from Downloads during playback. The sheet stayed at "Showing: Off" with no new track and no error. A sidecar found on first load works.
- **Root cause:** `data/playback/Media3PlaybackController.kt:1145-1171` uses `replaceMediaItems` with only subtitle changes. ExoPlayer treats that as updating the item in place, so no text source is created. The same path handles next-item sidecars, and items reached without prefetch are never resolved (lines 742-763).
- **Impact:** a headline player tool fails silently, including for anyone downloading subtitles through the web search flow.
- **Fix:** force a rebuild with a `customCacheKey` derived from the subtitle configs, or remove and re-add the item. Restore position and shuffle order. Prefetch on queue edits.
- **Risks:** a rebuild can cause a short rebuffer. Needs care with shuffle.
- **Validate:** a real-ExoPlayer test: import, assert a selectable text track, and confirm the text renders.

#### FUNC-03: Activity recreation after auto-advance restarts the first video and drops the queue

**Functionality · Traced · Effort S for the guard · Phase 1**

- **Evidence:** `Navigation.kt:858-861` reloads `PlayerKey.videoUri`, which never updates on transitions. The manifest's `configChanges` omits `uiMode`, `density`, `fontScale` and keyboard changes, so these recreate the activity. My dark-mode test used a single-item queue and passed, so it does not rule this out.
- **Impact:** on item 4, a scheduled dark-mode flip returns the user to item 1 as a one-item queue.
- **Fix:** run the effect only when no queue is active, and replace the back-stack key on transitions.
- **Risks:** low.
- **Validate:** instrumented test that flips `uiMode` on queue item 2 and asserts the item and queue length.

#### FUNC-04: Audio delay accumulates on each speed change

**Functionality · Traced in bytecode, not measured · Effort M · Phase 1**

- **Evidence:** a speed change flushes the audio pipeline, and `data/playback/AudioDelayProcessor.kt:60-62` resets its count and applies the delay again. The processor also runs before speed change, so real delay is the set delay divided by speed.
- **Impact:** with +300 ms set, a 2x hold (two speed changes) leaves about 900 ms of offset until the next seek. This breaks the app's own Bluetooth guidance.
- **Fix:** tell seek flushes from parameter flushes, or apply the offset at the sink or renderer clock.
- **Risks:** medium, since audio pipeline timing is subtle.
- **Validate:** a pipeline test through speed changes, plus a clap test on a device.

#### FUNC-05: Playback outlives the ViewModel that owns essential behaviour

**Architecture and reliability · Traced · Effort M to L · Phase 1**

- **Evidence:** after swiping the task away while playing, `onCleared` releases EQ and loudness (`ui/player/PlayerViewModel.kt:1576`), positions stop saving and pause-at-end stays set with no owner (`Media3PlaybackController.kt:241`, `770`).
- **Impact:** background audio loses progress, effects and queue advance. Tapping the notification opens the library with no path back to the player.
- **Fix:** add the process-scoped `PlaybackSessionCoordinator`. As an interim step, clear pause-at-end in `onCleared`.
- **Risks:** medium; a central refactor. Do it right after STAB-01 so both changes share a design.
- **Validate:** task-removal test on a device that asserts saved position and queue advance.

### High: data integrity

#### DATA-01: A copy-then-delete move erases progress, settings and bookmarks

**Data integrity · Traced · Effort M · Phase 1**

- **Evidence:** after the copy, `deleteAll` triggers `onVideoDeleted`, which runs `removeVideoFromLibrary` (`data/repository/MediaRepositoryImpl.kt:92-95`). It deletes the row and bookmarks before the rescan can match the new file. See also `ui/library/MediaLibraryScreen.kt:1496` and `Navigation.kt:513`.
- **Impact:** a half-watched film moved to a SAF folder reappears unwatched with its bookmarks gone, and they cannot be recovered.
- **Fix:** record the destination URI in the transfer journal and remap the row and bookmarks, or mark the row missing until reconciliation.
- **Risks:** medium; touches Room, a hot file.
- **Validate:** instrumented move test that asserts position and bookmarks at the new URI.

#### DATA-02: In-place move ignores the destination volume

**Data integrity · Logic traced, outcome needs a device · Effort S · Phase 1**

- **Evidence:** `core/media/MediaMover.kt:49-58` checks only "MediaStore source, non-SAF destination", despite documenting same-volume behaviour. `relativePathFor` drops the volume (`core/media/MediaFileTransfer.kt:332-345`).
- **Impact:** moving a video to an SD-card folder can report "Moved to Movies" while the file stays on phone storage, so no space is freed.
- **Fix:** compare the source `VOLUME_NAME` with the destination, and route cross-volume moves through the copy path.
- **Risks:** low.
- **Validate:** unit test for the decision, plus a device run with a real or emulated SD card.

#### DATA-03: A file that takes over 10 minutes to copy never finishes

**Reliability · Logic traced, stop timing needs a device · Effort L · Phase 2**

- **Evidence:** `data/transfer/MediaTransferScheduler.kt:106-118` has no foreground or expedited request. On stop, `core/media/MediaFileTransfer.kt:146` and `167` delete the partial file, and the journal tracks whole files only.
- **Impact:** a 20 GB MKV to a slow card restarts from zero repeatedly, wasting battery and flash wear. Originals stay safe.
- **Fix:** keep and resume partial files after checking length, or use user-initiated jobs on API 34+.
- **Risks:** medium. A foreground service conflicts with the no-notification-permission rule, so that option needs an owner decision.
- **Validate:** a throttled test provider that stops the worker mid-file and asserts resume.

#### DATA-04: Scans purge rows that still hold user state

**Data integrity and privacy · Traced · Effort S · Phase 0**

- **Evidence:** `data/scanner/MediaScanWorker.kt:159-177` checks only position, completed and custom title. It ignores `isHidden`, `lastPlayedTimestamp`, speed, decoder, delays and bookmarks. A null cursor returns an empty list (`data/scanner/MediaStoreScanner.kt:67-75`), which looks like "no videos".
- **Impact:** hide a private video on an SD card, eject, rescan and reinsert, and it comes back visible. A transient empty result could clear hidden flags library-wide.
- **Fix:** treat a null cursor as a failed scan. Skip the purge on an empty scan, an unmounted volume or limited access. Extend the retention test to every user-owned column.
- **Risks:** low; missing rows may linger a little longer.
- **Validate:** Robolectric tests for each column and each failure signal.

#### DATA-05: A running scan overwrites changes made during it

**Data integrity · Traced · Effort M · Phase 1**

- **Evidence:** user columns come from a snapshot taken at scan start, then a whole-row REPLACE writes them back (`data/scanner/MediaScanWorker.kt:111`, `153`, `180-185`). Four unique work names let scans overlap.
- **Impact:** during a long SAF scan, finishing a video or renaming one can revert. A just-deleted row can return.
- **Fix:** update only scanner-owned columns, merge inside the transaction, and use a single work name.
- **Risks:** medium; a hot file, needing independent review if the schema changes.
- **Validate:** concurrency test that writes during a paused scan and asserts the write survives.

### High: privacy, accessibility and language

#### PRIV-01: Diagnostic report leaks file names and paths, contrary to its promise

**Privacy · Regex confirmed by reading, leak traced · Effort M · Phase 0**

- **Evidence:** the pattern in `core/diagnostics/RedactionEngine.kt:12-14` stops at whitespace and commas. `/storage/emulated/0/Movies/My Holiday/secret clip.mp4` leaves `Holiday/secret clip.mp4` readable. `content://` URIs, `/mnt/media_rw/` and `/data/media/0/` are not covered. Raw titles enter the log at `data/scanner/FileReconciliationEngine.kt:97-103`.
- **Impact:** a user emails a report believing it is clean, as `docs/PRIVACY.md` promises, and it contains sensitive titles. For a privacy-first product this is a direct trust breach. Settings also goes straight to the chooser, with no preview.
- **Fix:** log an allow-list of fields, with no free text, titles or exception messages. Add a preview before sharing.
- **Risks:** low; reports carry less detail.
- **Validate:** adversarial test paths with spaces, commas, Unicode, SD cards and encoded content URIs.

#### A11Y-01: Nothing is reachable by a screen reader when player controls hide

**Accessibility · Tree reproduced, TalkBack not run · Effort S · Phase 0**

- **Evidence:** during hidden-controls playback, the accessibility tree held no labelled nodes. The tap zones are raw `pointerInput` (`ui/player/PlayerScreen.kt:492`, `571`) and the 5-second hide is fixed (`ui/player/PlayerViewModel.kt:123`).
- **Impact:** a blind user probably cannot pause or leave once controls hide. Release notes say "Full TalkBack".
- **Fix:** no auto-hide during touch exploration, `calculateRecommendedTimeoutMillis`, and a labelled clickable surface.
- **Risks:** low.
- **Validate:** a real TalkBack pass through play, pause, seek and exit.

#### A11Y-02: No keyboard, D-pad or Chromebook control

**Accessibility and compatibility · Reproduced · Effort M · Phase 2**

- **Evidence:** during playback, Space and D-pad plus Enter had no effect on the player. Media keys did work. Reviewers found no key handlers in the package.
- **Impact:** tablets with keyboards, Chromebooks, switch access and remote-style input are poorly served.
- **Fix:** a player key map (space, arrows, f, m) with visible focus.
- **Risks:** low; must not fight text fields.
- **Validate:** instrumented key-event tests and a hardware keyboard pass.

#### I18N-01: The app cannot be translated

**Internationalization · Observed in code · Effort L · Phase 2**

- **Evidence:** `res/values/strings.xml` holds one string. There are 0 `stringResource` calls and about 972 user-text literals (a heuristic count), 0 plurals, 0 locale folders and no `locales_config`.
- **Root cause:** never set up from the start, and no lint rule prevents it. The spec's Persona 1 targets India, Brazil, South-East Asia and the Middle East and North Africa.
- **Impact:** English only worldwide, with 20+ hand-written plural branches. This is the largest gap against every serious competitor.
- **Fix:** extract in passes, enable a lint gate, add plurals and a locale config, then seed 5 to 8 languages through a community platform that requires no app network access.
- **Risks:** a large diff across hot UI files. Do it after the player split to reduce conflicts.
- **Validate:** a test that fails on hard-coded UI text, plus a pseudolocale gallery run.

### High: release, dependencies and legal

#### SHIP-01: Store listing makes false claims

**Release and policy · Observed in files · Effort S · Phase 0**

- **Evidence:** `distribution/play-store/metadata/short_description.txt` says "zero permissions", but the release APK requests 7. The full description says 0.25x to 3.0x in 0.05 steps, but `core/playback/PlaybackSpeedOptions.kt:11` has 8 values ending at 2.0x. It lists accent names the code does not have. Release notes say "Full TalkBack".
- **Impact:** a misleading-metadata rejection and angry reviews. False claims are especially damaging for a product sold on honesty.
- **Fix:** rewrite from the code, and add a test comparing listed claims with constants.
- **Risks:** none.
- **Validate:** a reviewer checks each claim against code.

#### SHIP-02: The device checklist overstates its evidence

**Process integrity · Observed in documents · Effort M · Phase 0**

- **Evidence:** `docs/DEVICE_CHECKLIST.md` has 19 of 22 rows ticked. TalkBack rows say PASS from a JVM test, while `docs/device-testing/2026-10-29-galaxy-s24-findings.md` says not tested. HDR is PASS with a different decoder than the findings' PARTIAL. Process death is ticked as fixed by a PR with no retest found. The thermal run was on USB power.
- **Root cause:** agents converted test results and merged fixes into hardware passes. The original S24 findings are honest; the summary is not.
- **Impact:** the 1.0 gate cannot be trusted, and STAB-01 shows what that gate missed.
- **Fix:** revert unproven ticks and require device, build and capture for each row. Re-run TalkBack, Bluetooth, background audio and task removal.
- **Risks:** none; it only delays false confidence.
- **Validate:** each ticked row links to a dated capture.

#### SHIP-03: An "Official Production Release" is public while the gates are open

**Release · Observed on GitHub · Effort S · Phase 0**

- **Evidence:** GitHub release v1.0.0, published 2026-10-30, is marked final and carries an AAB and 4 APKs. `.agents/STATE.md` says the signed tag waits for a key and the checklist, and the README says pre-release. The tag is 26 commits behind `main`, and `versionCode = 1` is hard-coded, so any update needs a bump first.
- **Impact:** anyone downloading it gets STAB-01 and the other bugs. If it was signed with a different key from the eventual Play upload key, sideload users cannot update in place. I did not check its signature.
- **Fix:** mark it pre-release now. Verify and publish the certificate fingerprint. Derive `versionCode` from the tag or CI.
- **Risks:** low.
- **Validate:** `apksigner verify --print-certs` on the assets; a build with a higher `versionCode` installs over the old one.

#### SHIP-04: Play declarations are missing from the release kit

**Release and policy · Absence confirmed by reviewer's search, policy applicability suspected · Effort M · Phase 3**

- **Evidence:** `docs/RELEASE_KIT.md` and `distribution/` have no foreground-service declaration or demo video, no `READ_MEDIA_VIDEO` declaration, and no plan for the closed-testing rule on new personal accounts. Content rating is pre-filled as no online interaction, although the app hands off to a browser.
- **Impact:** review rejection or weeks of delay.
- **Fix:** a console runbook, with the rating taken from the actual questionnaire. Confirm current policy on the day of submission.
- **Risks:** none.
- **Validate:** a dry-run internal track upload passes pre-launch checks.

#### QA-01: CI never exercises what actually ships

**Testing · Observed in CI config · Effort S to M · Phase 1**

- **Evidence:** `connected` runs 13 debug-variant tests on one API 35 tablet image. The R8 release is built but never launched. There is no API 26, 29 or 36 run. `bundleRelease` is not in CI, and `verifyReleaseApk` attaches only to `assembleRelease`, so **the AAB uploaded to Play skips the privacy check**.
- **Impact:** R8 and old-Android failures reach users. The core privacy promise is unchecked on the real artifact.
- **Fix:** finalize `bundleRelease` with the check, and run a nightly release-variant connected job across an API 26, 29, 36 matrix.
- **Risks:** more CI minutes; run the matrix nightly rather than on each PR.
- **Validate:** deliberately add a forbidden package and confirm the bundle build fails.

#### DEP-01: Media stack and native decoders are years of fixes behind, with no update path

**Dependencies and security · Versions observed, CVE exposure suspected · Effort L · Phase 2**

- **Evidence:** Media3 is 1.5.1 against 1.11.1 stable. `libffmpegJNI.so` reports `Lavc60.3.100`, the FFmpeg 6.0 line. Dependabot and Renovate are absent, and Dependabot security updates are off. Other gaps are listed in the technical debt section.
- **Impact:** hostile media reaches old C parsers. Files from outside the app are the realistic attack surface. There is no network to exfiltrate over, which limits the impact. No specific CVE was proven reachable.
- **Fix:** move Media3 and the Jellyfin decoder together to 1.9.x, or self-build as ADR-010 says. Enable alerts and Renovate.
- **Risks:** medium to high; many behaviour changes. Do it behind the new real-ExoPlayer tests and a full format matrix.
- **Validate:** the format matrix and `PlaybackSmokeTest` on two real devices.

#### LEG-01: Shipped decoders contradict the project's own licensing decisions

**Legal and compliance · Contradiction observed, legal outcome unknown · Effort S to decide · Phase 0**

- **Evidence:** ADR-010 describes a self-built FFmpeg with no bundled AAC, and spec 07 marks bundled DTS and TrueHD as never. `spec/17` gates AC-3 and E-AC-3 on counsel. The app ships a prebuilt Jellyfin library whose decoders include these, and the code's own note lists AAC, DTS and TrueHD. The Jellyfin repo displays GPL-3.0, and nobody checked the artifact's licence terms against the Apache-2.0 app.
- **Impact:** possible patent or licence exposure on a public release. This also blocks a clean F-Droid story.
- **Fix:** dump the enabled decoders and read the artifact's POM. Then decide with counsel if needed, and update the registers and `NOTICE`. I am not a lawyer, so treat this as a question to resolve rather than a conclusion.
- **Risks:** removing these decoders would remove a real differentiator.
- **Validate:** a written decision in the DEC register, matched by a decoder dump in CI.

### Medium: product, playback and data

| ID | Finding and evidence | Root cause and impact | Fix | Effort, risk | Validate |
| --- | --- | --- | --- | --- | --- |
| UX-01 | **Limited access is unsupported.** Reproduced: choosing "Allow limited access" shows 2 videos with no explanation. Within 70 s in the background the grant lapsed, and the app showed "Limited access" while its chips still counted 2. | `READ_MEDIA_VISUAL_USER_SELECTED` is not declared, so Android gives a temporary grant. Users on Android 14+ are re-prompted and confused, and DATA-04 can then purge rows. | Declare it, detect partial mode, offer "select more", skip purge. | M, medium | Run the flow on API 34+ across restarts |
| UX-02 | **Opening a file from another app leaves a permanent card.** Reproduced: a card titled `1000000475`, "0 B, External, Missing" sat at the top of the library. | External opens create rows that scans mark missing and never purge (`data/repository/MediaRepositoryImpl.kt:69-86`). Looks broken. | Separate history flag, resolve the display name, exclude from missing logic. | S, low | Open externally, rescan, assert no card |
| UX-03 | **No undo anywhere.** Clear queue and reset progress are immediate, while the reversible Hide gets a dialog. | Confirmations are inverted (`ui/player/PlaybackQueueSheet.kt:245`, `ui/library/MediaLibraryScreen.kt:1194`, `:1353`). | Snackbar undo for all three. | S, low | UI tests |
| UX-04 | **Error labels can mislead.** Reproduced: a truncated MP4 says "Storage Inaccessible", and a zero-byte file's title is its numeric ID. | Error mapping is coarse for source errors. People are sent to check the wrong thing. | Distinguish parse failure from I/O failure. | S, low | Taxonomy tests with these files |
| I18N-02 | **RTL player partly mirrors.** Reproduced in Arabic: Rewind moved right, Forward left, and time labels swapped. Gesture sides are suspected to flip. No RTL test exists. | No left-to-right provider on media controls. Arabic and Hebrew users likely see this today. | Force LTR for seek and transport, and add RTL gallery tests. | S, low | Arabic device pass |
| I18N-03 | **US formats are hard-coded.** `ui/library/MediaLibraryUiState.kt:119`, `:164`, `:170`; language names forced to English. | Dates and sizes ignore the user's locale. | Locale-aware formatters. | S, low | Unit tests per locale |
| A11Y-03 | **Low-emphasis text fails AA.** Computed: `#606978` is 3.48:1 on base and 2.84:1 on sheets, used 53 times. Light-theme `#6B7280` is 4.47:1. | DESIGN.md's ratios were computed on white only. | Lighten the dark token toward `#8A93A1`, darken the light token, and test the ratios. | S, low | Automated contrast test on real surfaces |
| A11Y-04 | **Seek track is nearly invisible.** `#2B2F38` on black is 1.57:1, against the 3:1 minimum for controls. | Token too subtle (`ui/player/PlayerScreen.kt:1461`). | Lighten it. | S, low | Contrast test |
| A11Y-05 | **System caption preferences are ignored.** No `CaptioningManager` use (`ui/player/PlayerVideoSurface.kt:102-109`). | Users with system settings must redo them. | "Use system style" as the default. | S, low | Change system captions and observe |
| A11Y-06 | **Brightness is gesture-only.** `ui/player/PlayerScreen.kt:541`. | No alternative for people who cannot perform the gesture. | Custom action or slider in tools. | S, low | TalkBack action test |
| FUNC-06 | **Up Next countdown survives external commands.** Bluetooth "Next" goes to B, then the timer skips to C (`ui/player/PlayerViewModel.kt:638-661`). Traced. | The job is cancelled only by ViewModel methods. | Cancel on any non-Ended state or URI change. | S, low | Controller test |
| FUNC-07 | **Retry and decoder recovery misbehave.** Retry always makes a one-item queue (`ui/player/PlayerViewModel.kt:1113`, `:790`). The software-decoder button often reverts on first tap (`:1256`, suspected race). Traced. | Recovery rebuilds rather than retries. | Retry through the controller; one atomic restart action. | S, low | Error-path tests on item 3 of 10 |
| FUNC-08 | **Resume data goes stale; no near-end rule.** Next then Previous restores the old position (traced). Reproduced: reopening a finished video lands on 0:30 of 0:30 with a replay icon. | Preferences come from the queue-build snapshot, and `PlaybackStateJournal` has no production callers. | Re-read the row on each transition, and apply one near-end rule at save and resume. | S to M, low | Pure rule tests and a device replay |
| FUNC-09 | **Recovery edge cases.** The surface is not re-attached after a runtime check error (`data/playback/Media3PlaybackController.kt:365`, traced). The 8 s start watchdog also fires on mid-playback buffering, and can force software decoding (`:619-663`, suspected). | Watchdog has no first-frame condition. | Keep the surface; fall back only before the first frame. | S, low | Slow-source test |
| DATA-06 | **Originals are deleted without verifying the copy.** No byte comparison or fsync (`core/media/MediaFileTransfer.kt:145-163`, `:292-314`). Traced. | Success means only that the stream closed. A flaky USB provider could lose the only good copy. | Compare sizes, sync, treat a mismatch as failure. | S, low | Fault-injecting provider test |
| DATA-07 | **DataStore has no corruption handler.** `data/settings/ApturaSettingsRepository.kt:30`. Traced. | A corrupted file silently resets settings and SAF folders, and writes keep failing. | `ReplaceFileCorruptionHandler`. | S, low | Corrupt the file in a test |
| DATA-08 | **Batch in-place move can run twice.** `ui/library/MediaMoveFlow.kt:71-92`, `:130-158`. Traced. | The consent flag clears while the queue stays full, so the effect restarts. | In-flight guard and a set of URIs. | S, low | Compose test |
| DATA-09 | **No way to clear history, and no backup.** `clearAll()` is unused, and `allowBackup=false` with no export. Some delete paths orphan bookmarks. Traced. | Privacy users cannot wipe history, and a new phone loses all progress. | "Clear watch history", and SAF export and import. | M, low | Round-trip test |
| DATA-10 | **Rename can drop the extension.** "Mr.Robot" on a `.mkv` (`core/media/ApturaMediaFileManager.kt:114`, `:141-148`). Traced. | Any short suffix counts as an extension. The SAF file may then vanish from the library. | Keep the extension unless the new one is a known video type. | S, low | Table test |
| DATA-11 | **Subtitle charset handling is Western only.** BOM, UTF-8, else windows-1252 (`core/media/SubtitleCharsetNormalizer.kt:79-101`). Traced. | CP1251, GBK, Shift-JIS and CP1256 files show mojibake. This hurts the target regions directly. | Encoding picker plus detection heuristics. | M, low | Fixtures already exist in `fixtures/adv_sub/encodings` |
| DATA-12 | **Folder removal deletes too broadly.** Removing `...%3AMovies` also matches `...%3AMovies2` (`data/database/MediaItemDao.kt:97-98`). Traced. | Unescaped prefix match without a boundary. Unrelated progress is lost. | Bound the prefix. | S, low | DAO test |
| DATA-13 | **Move and consent edge cases.** Consent state uses `remember` (`ui/library/MediaDeleteFlow.kt:55-60`). `Download/` may be an invalid video destination. Android 8 and 9 lack `RELATIVE_PATH` (suspected). | Recreation during a system dialog leaves stale rows, and old Android gets technical errors. | `rememberSaveable`, filter destinations, route API under 29 to the picker. | S, low | API 26 and 29 emulator runs |

### Medium: security, performance and engineering

| ID | Finding and evidence | Root cause and impact | Fix | Effort, risk | Validate |
| --- | --- | --- | --- | --- | --- |
| SEC-01 | **Keystore guide says the key is git-ignored, and it is not.** `git check-ignore upload-keystore.jks` returns 1. The guide tells people to keep it in the repo root. No keystore is in history. | `.gitignore` has only `local.properties`. One `git add -A` by any agent could publish the upload key to a public repo. | Ignore `*.jks`, `*.keystore`, `*.p12`, `*.pem`, move the key outside the repo, add a CI reject. | S, none | Try adding a dummy `.jks` |
| SEC-02 | **Zip extraction caps each entry but not the total.** 20 MB per entry, with no count or total limit. Recent `.zip` files matching a title are checked without a tap (`core/media/SubtitleZipExtractor.kt:60-104`). Zip Slip is blocked. | A compressible zip can fill storage or cause an ANR. No code execution. | Limit entries and total bytes, use a per-import folder, run off the main thread. | S to M, low | Zip-bomb fixtures |
| SEC-03 | **Agent controls are self-asserted.** The ruleset needs 0 approvals with no CODEOWNERS. All agents post as the owner, so an `Override:` is plain text. | Independent review of `.github/`, invariants and Room is policy only. A mistaken or prompt-injected agent could weaken a check and merge on green. | Separate bot identity and required code-owner review on those paths. | M, some friction | Attempt a merge without review on a test branch |
| SEC-04 | **Privacy policy misstates permissions.** Reproduced with `aapt2`: `ACCESS_NETWORK_STATE`, `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED` are present. The policy says the first is not requested and omits the others. | Policy was written from the source manifest, not the merged one. This is the Play policy URL. | Correct it, and try removing `ACCESS_NETWORK_STATE` if tests allow. | S, none | Compare with the `aapt2` dump in a test |
| SEC-05 | **No dependency integrity or alerts.** No `verification-metadata.xml`, CodeQL or SBOM. Dependabot security updates are off. | A tampered or vulnerable library goes unnoticed in an app whose promise is trust. | Turn on alerts now; add verification metadata and a release SBOM. | S to M, low | Alert test, tampered-checksum build fails |
| PERF-01 | **Player recomposes 4 times a second in one huge function.** 250 ms position tick reaches `PlayerScreen`, about 2,060 lines. `pointerInput` keys also restart gesture detection (`ui/player/PlayerScreen.kt:492`, `:571`). Cost unmeasured. | Could cost frames and battery on low-end phones. | Separate position flow read in the draw phase; `rememberUpdatedState`; split the composable. | M, medium | Compose metrics before and after |
| PERF-02 | **No baseline profile or macrobenchmark.** The spec's startup and jank targets go unmeasured. | No regression signal on real hardware, and Compose apps start slower without a profile. | Add both, starting with cold start and library scroll. | M, low | Benchmark in CI on a device or nightly |
| PERF-03 | **Database work grows with the library.** `uri` is unindexed though every position save filters on it. Every scan rewrites every row at each start. SAF dedupe is quadratic. | At 3,037 videos it was fine. 10,000+ is unmeasured and may lag. | Index `uri`, write only changed rows, hash-map dedupe. | M, medium; hot files | Scale test at 20,000 rows |
| PERF-04 | **Thumbnail generation is unbounded.** No LRU cache cap, up to 64 threads, no timeout on corrupt files (`core/media/ThumbnailRepository.kt:195-203`). Hang is suspected. | Disk growth and stalled workers on bad files. | `limitedParallelism(3)`, LRU trim, timeout. | M, low | Corrupt-file corpus |
| PERF-05 | **Background audio appears to keep decoding video.** Nothing disables the video track on stop (`MainActivity.kt:107-112`). Suspected. | Battery drain in the mode people use for long sessions. | Disable the track on stop outside PiP and audio-only. | S, medium | Battery measurement on a device |
| QA-02 | **Kill-point tests are tautologies.** `core/playback/KillPointSimulator.kt` returns hard-coded `survives = true` with no production callers. `PlaybackStateRestorationTest:117-123` asserts those constants. | 12 restoration proofs prove nothing, which is how STAB-01-class bugs survive. | Delete it and write a real kill test. | S, low | The new test fails on seeded breakage |
| QA-03 | **Gallery tests assert nothing.** 165 of 997 tests skip by default and only write PNGs. | No visual regression signal. | Roborazzi goldens on key screens. | M, low | Intentional UI change fails |
| QA-04 | **Critical classes are thinly tested.** The service has only a manifest reference, and `MediaMetadataInspector` (329 lines of hostile-file parsing) has none. `MediaDeleteFlow` and `MediaMoveFlow` have no direct tests. | Tests with fakes cannot catch ExoPlayer or lifecycle bugs. | Small real-ExoPlayer suite; service lifecycle tests; inspector tests with bad files. | M, low | Reintroduce FUNC-02 and see it fail |
| QA-05 | **No coverage or static analysis.** No Kover, detekt, ktlint, LeakCanary or StrictMode. Lint gates errors only. | Test quality and leaks are invisible. | Kover on PRs, then detekt; debug-only StrictMode and LeakCanary. | M, low | Reports in CI |
| QA-06 | **Migration testing is partial.** Only 4-to-latest executes SQL. Only schemas 5 and 6 are committed, with no `MigrationTestHelper`. Downgrade wipes data. | A bad migration could lose user history. | Add `room-testing` from now on. | S to M, low | Upgrade test on each schema |
| QA-07 | **Corpus is disconnected.** `fixtures/manifest.csv` lists about 1,086 media files absent from the checkout, and nothing reads `fixtures/`. | The media corpus gives no ongoing protection. | Nightly generated small matrix through `PlaybackSmokeTest`. | M, low | Nightly report |
| DEV-01 | **AAR metadata check disabled for all libraries.** Bottom of `app/build.gradle.kts`, to allow adaptive 1.3.0 needing compileSdk 37. | Future incompatibilities pass silently. | Move to compileSdk 37 and remove the block. | S, low | Build with the check on |
| DEV-02 | **No release workflow.** No signed AAB job, automated tag or staged rollout. Released binaries are built outside CI, without checksums. | Releases cannot be reproduced or traced. | Tag-triggered workflow using protected secrets. | M, low | Dry-run to the internal track |
| DEV-03 | **Process is oversized for one owner.** `tools/agents` is 3,038 lines, plus 557 lines of workflow docs. 11 of 46 PRs were process work, and 63 of the last 100 Actions runs are the status bot. 16 merged branches remain. | Attention goes to coordination while real bugs ship. Bus factor is 1. | Freeze it and drop debates and the schedule. Keep `pr_checks.py` and security review. | M, low | Fewer process PRs next month |
| PROD-01 | **Gold accent is unlocked by a star click or honour toggle.** `ui/settings/SettingsScreen.kt:719-727`, `:762`. A store screenshot advertises it. Policy risk suspected. | Looks like an incentivised action and feels off in a free app. | Make Gold free; keep a quiet thank-you. | S, none | Policy read before submission |
| PROD-02 | **Store assets look unprofessional.** Feature graphic shows `â€¢` mojibake, and open PR #52 may fix it. Screenshots show test bars, debug icons and a toast over a card. | The listing undersells a well-designed app. | Reshoot with demo content, as `docs/assets/screenshots` already does. | S, none | Human review |

### Low

| ID | Finding and evidence | Fix | Effort |
| --- | --- | --- | --- |
| SEC-06 | Release has no checksums or attestation | Publish SHA-256 and the certificate fingerprint | S |
| SEC-07 | `MainActivity.kt:229` passes any VIEW data onward; `file://` reaches the player with app identity. No exfiltration route found | Allow only `content` and `file`, reject paths in `dataDir`, consider dropping `BROWSABLE` | S |
| SEC-08 | `allowBackup=false` may not cover device-to-device transfer (suspected). Lint also flags `DataExtractionRules` | Add rules excluding all domains | S |
| SEC-09 | `verifyReleaseApk` skips native libraries and `java.net`. `DefaultHttpDataSource` ships inside the APK, so "zero network stack" is overstated. It cannot work without `INTERNET` | File and content `DataSource.Factory` only; word claims as "no network permission" | S to M |
| SEC-10 | Actions are pinned by tag rather than SHA. Their jobs hold no secrets | Pin to SHAs | S |
| SEC-11 | No `FLAG_SECURE`, recents protection or overlay protection | Optional private mode; hide overlays on destructive dialogs | S |
| SEC-12 | Media3's own logging is not stripped in release (suspected). App code has no `Log` calls | `-assumenosideeffects` for `Log` | S |
| UX-05 | Player has many sheets, and "Advanced Settings" opens "Playback tools" (partly subjective) | Align names; regroup tools | M |
| UX-06 | 36 dp and 44 dp visual targets in queue, library and details | Raise to 48 dp | S |
| UX-07 | Mixed terms and case: "Scan device", "Rescan library", "Footprint", "Lock Controls" against "Seek bar". Small polish items in library and details | One copy pass | S |
| UX-08 | Orientation button may do nothing on Android 16 tablets (suspected): `ui/player/PlayerScreen.kt:1069-1073` | Hide on large screens | S |
| A11Y-07 | Only onboarding checks reduced motion. Player and sheets lack 200% font gallery pages. In my quick look at 200%, the subtitle sheet held up | Add those pages | S |
| DATA-14 | Search matches raw titles, not displayed ones: "Show S01E01" misses `Show.S01E01`, "cafe" misses "Café" (`ui/library/MediaLibraryViewModel.kt:159-162`). A DAO search is dead code | Search normalized titles and display names | S |
| FUNC-10 | Stats show 100% drops after one dropped frame. "Hardware only" still falls back. "Play next" under shuffle is random. Unsupported audio tracks can be chosen | Fix each; S each | S |
| DEV-04 | `review/` tracks 267 files and 43 MB; four `AUDIT_*.md` sit at root; 86 MiB pack | Archive evidence outside the repo | S |
| DOC-01 | `spec/00` still says documentation only (last changed 2026-10-20). `docs/TESTING.md` counts are stale. STATE says no signing key while `local.properties` in the main checkout has a release keystore section | Point `spec/00` at STATE; generate counts from CI | S |

## Security, privacy and reliability assessment

**No Critical security issue exists, and the no-network claim holds at the permission level, but privacy promises are worded more strongly than the code delivers.** An app with no `INTERNET` permission rules out remote attacks and easy exfiltration. The realistic threats are hostile local files, leaks through user-shared reports, and repository compromise.

**Solid, confirmed by review:**

- No `INTERNET` permission, confirmed in the merged release APK by my own dump. Tests fail if a dependency adds it.
- No `pull_request_target`, no script injection, least-privilege workflow tokens and no secrets in CI.
- Secret scanning and push protection are on. History searches found no keystores or tokens.
- `main` requires `verify` and `connected`, with linear history and no bypass actors.
- App code has no sockets, WebView or exported provider. Zip Slip is blocked.

**Weak:** PRIV-01 (redaction), SEC-01 (key handling), SEC-03 (self-asserted agent controls), SEC-04 (policy text), DEP-01 (old FFmpeg), and QA-01 (the AAB skips the privacy check).

**Reliability.** Stress tolerance is good, with no crashes in 10,097 random events and clean handling of every unplayable file. Lifecycle reliability is the weak point, because state ownership is split (STAB-01, FUNC-03, FUNC-05). Data handling is careful about originals and about the transfer journal, but not about library metadata (DATA-01, DATA-04, DATA-05). No crash reporting exists by design, so field failures stay invisible unless users send a report. That raises the value of on-device testing and makes PRIV-01 more important.

## Performance and optimization assessment

**Nothing I measured is alarming, but no number here came from a real phone, so performance is unproven rather than good.** The emulator used software rendering, which inflates frame times and says little about thermals or battery.

The library held up at 3,037 videos, with cold start under 800 ms and 67 MB PSS after fast scrolling. The APK is small at 4.85 MB for arm64.

**Opportunities, in order of value:**

1. Add a baseline profile and macrobenchmarks, so every other change can be measured (PERF-02).
2. Stop decoding video during background audio, if measurement confirms it (PERF-05).
3. Split the player's position updates from its large composable (PERF-01).
4. Prepare for very large libraries: index `uri` and stop rewriting every row (PERF-03).
5. Bound thumbnail work and disk use (PERF-04).

Avoid optimizing further before item 1 exists. The rest is informed guesswork until then.

## UI/UX, accessibility and compatibility assessment

**The app looks good and common tasks are short, but it is effectively English-only and touch-only, and its accessibility work stops where the controls hide.** The dark visual language is restrained and consistent, and it reads as premium outside the store assets.

Common tasks take few taps: open 1, resume 1, change subtitle 3, change speed 3 to 4, delete 3. Empty, limited-access and error states are real and useful, and the error card offers "Open in another app". Onboarding is honest, with a working Skip.

**Accessibility against WCAG 2.1 AA, partial review only:**

| Criterion | Status | Evidence |
| --- | --- | --- |
| 1.4.3 Text contrast | Fails in places | Low-emphasis dark text 2.84:1 to 3.48:1 (A11Y-03) |
| 1.4.11 Control contrast | Fails | Seek track 1.57:1 (A11Y-04) |
| 2.1.1 Keyboard | Fails | Space and D-pad do nothing in the player (A11Y-02) |
| 2.2.1 Timing | Likely fails | Fixed 5-second hide (A11Y-01) |
| 4.1.2 Name, role, value | Mostly passes when visible | Labelled controls and seek bar actions; nothing exposed when hidden |
| 1.4.4 Resize text | Partly passes | Holds up at 200% in the screens I checked |

**Compatibility:**

| Target | State |
| --- | --- |
| Phones, tablets, landscape | Adaptive layouts exist and are gallery-tested. Only one phone-size emulator was run here. |
| Android 8 to 10 | Not run. Move paths are at risk (DATA-13). |
| Android 14+ limited access | Reproduced as unsupported (UX-01) |
| Android 16 large screens | Orientation button may not work (UX-08) |
| Chromebook, keyboards | Not supported (A11Y-02) |
| Android TV | Not declared: no leanback launcher, and `touchscreen` is not optional |
| RTL languages | Partly mirrored player (I18N-02) |
| 16 KB memory pages | Passes |

## Testing and QA maturity

**The team writes many tests with real engines, but they cluster around logic that was already easy to test, and the riskiest lifecycle paths lack them.** The proof is direct: 832 executed tests passed, while STAB-01, FUNC-01 and FUNC-02 shipped.

| Level | State | Gap |
| --- | --- | --- |
| Unit and Robolectric | Strong, real SQLite and some real ExoPlayer | Lifecycle, subtitle rebuilds, audio pipeline |
| Instrumented | 13 tests on one debug tablet image, with real codec playback | Release variant, old APIs, service stop |
| Visual | PNGs for humans only | No golden comparison (QA-03) |
| On-device | One honest S24 session | Summary overstates it (SHIP-02); TalkBack not done |
| Non-functional | None automated | Performance, leaks, coverage (PERF-02, QA-05) |
| Static | Lint errors only | 68 warnings allowed, no detekt |

**Strengths worth keeping:** the invariant tests that refuse a false pass, `PlaybackSmokeTest` with H.264, HEVC and a 558-item queue, pixel-level transition tests, and a stable CI at 26 of 26 on `main`.

**The most valuable additions** are one real-ExoPlayer lifecycle suite and a nightly release-variant run. Each reproduced bug above should get a regression test that fails first, as `AGENTS.md` already requires.

## Missing features and competitive benchmarking

**Aptura can credibly be the most trustworthy local video player, but it trails on language, subtitles and old formats, and the niche is a reputation project rather than a business.** Comparable open-source players sit at 100K to 1M+ installs, while VLC and MX Player hold the mass market. Competitor pages were opened on 2026-11-05 by the research reviewer. Reach and revenue views are estimates.

| App | Installs, rating | Money | Play data label | Recurring complaints |
| --- | --- | --- | --- | --- |
| [MX Player](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad) | 1B+, 4.2 | Ads, purchases | Collects data; All Files Access | Ads, EAC3 not supported, subtitle drift |
| [VLC](https://play.google.com/store/apps/details?id=org.videolan.vlc) | 500M+, 4.0 | Free | None collected | Crashes, memory |
| [Just Player](https://play.google.com/store/apps/details?id=com.brouken.player) | 1M+, 4.0 | Donations | None collected | No loop or frame step; lost progress |
| [Nova](https://play.google.com/store/apps/details?id=org.courville.nova) | 1M+, 4.2 | Free | None collected | No PGS, network reliability |
| [mpv-android](https://play.google.com/store/apps/details?id=is.xyz.mpv) | 500K+, 4.2 | Free | None collected | Subtitle styling UI, resume, file browser |
| [Next Player](https://play.google.com/store/apps/details?id=dev.anilbeesetti.nextplayer) | 100K+, 4.4 | Donations | None collected | Crashes, no audio sync |

**Feature comparison.** Aptura cells come from the code. An "m" marks a cell from memory, and a dash means not established.

| Capability | Aptura | VLC | MX | mpv | Just | Next | Nova |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Bundled software video decode | No, platform only | Yes | Yes | Yes | No | Yes | Yes |
| AC3, DTS, TrueHD audio | Yes | Yes m | Partial | Yes m | Yes | Yes | Yes m |
| Full ASS styling | No | Yes m | Yes | Yes | No | No | — |
| Languages | **1** | Many m | Many m | Some | Community | Community | Community |
| LAN or SMB | No, by design | Yes | Yes m | URL | Via provider | Yes | Yes |
| Android TV | No | Yes | Yes | Yes | Yes | Yes | Yes |
| Audio sync tools | Yes | Yes m | Yes m | Yes | Bluetooth auto | No | Yes |
| A-B repeat, frame step | Yes | Yes m | Partly m | Partly m | No | — | — |
| Trash-first file management | Yes | Delete only | Yes m | No | Delete only | — | — |
| Seek preview thumbnails | No | — | Yes m | — | No | — | — |
| F-Droid channel | No (DEC-013) | Yes | No m | Yes | Yes | Yes | Yes |

**Differentiated:** a CI-enforced lack of `INTERNET`, which none of the opened apps does; careful file management; EAC3 and DTS out of the box, against MX's top complaint; and more tool depth than other Media3 players. "No data collected" is not unique, since most of these apps declare it too. So the proof must be visible to users, not just stated.

**Will disappoint:** every non-English user, anime and fansub viewers (ASS), owners of old libraries, NAS and TV households, and F-Droid users.

**Challenging the two big decisions:**

- **Keep "no bundled software video decoder" for 1.0, and open a narrow debate later.** The reviewer's unmeasured estimate of hard failures is 2% or less for phone and chat libraries, 3 to 8% for mid-range mixed downloads, and 15 to 40% for anime or pre-2012 archives. A cheap mitigation comes first (PROD-G3). Run the corpus on a 2019 entry-level phone to test these numbers. A later exception could cover dav1d on API 26 to 30 and older codecs, if counsel confirms patents have expired.
- **Keep "no `INTERNET`".** It is the identity of the product. SMB may already work through a SAF documents provider such as [CIFS Documents Provider](https://github.com/wa2c/cifs-documents-provider), which is untested here and cheap to confirm.

**Ranked gaps:**

| ID | Gap | User impact | Fits invariants | Effort |
| --- | --- | --- | --- | --- |
| PROD-G1 | Localization (I18N-01) | High | Yes | L |
| PROD-G2 | F-Droid or IzzyOnDroid channel, with a self-built FFmpeg | High for target users | Needs a DEC change | M |
| PROD-G3 | "May not play here" badge before opening; keep the "open in another player" path | High | Yes | S to M |
| PROD-G4 | libass rendering for ASS | High for anime | Yes, not a video decoder | L |
| PROD-G5 | Test PGS and VobSub end to end; fix or disclose | Medium to high | Yes | S |
| PROD-G6 | Narrow software decode exception | High for a subset | Conflicts, owner decides | L |
| PROD-G7 | Test and document SMB through a provider | Medium | Yes | S |
| PROD-G8 | Chapters, persisted playlists, skip silence (no chapter or playlist code found, though spec 01 scopes both) | Medium | Yes | S to M each |
| PROD-G9 | Seek-bar frame preview | Medium | Yes | M |
| PROD-G10 | Android TV | Medium | Yes | L |
| PROD-G11 | Locked private folder (hide exists, without a lock) | Low to medium | Yes | M |
| PROD-G12 | User-visible subtitle encoding picker (DATA-11) | Medium in target regions | Yes | M |

**Three differentiators that fit without bloat:**

- **Privacy proof screen:** live permission list, build hash, reproducible builds and SBOM, so users can check the claim.
- **Library playability report:** "412 of 430 files play on this phone, and here is why the rest will not." None of the opened apps showed this. The 412 and 430 are illustrative.
- **Study mode:** A-B, frame step, tap-to-sync and dual subtitles for language learners and coaches.

**Other sources opened:** [Media3 supported formats](https://developer.android.com/media/media3/exoplayer/supported-formats), [Android supported formats](https://developer.android.com/media/platform/supported-formats), [Next Player repo](https://github.com/anilbeesetti/nextplayer), [Just Player repo](https://github.com/moneytoo/Player), [VLC for Android](https://www.videolan.org/vlc/download-android.html), [mpv-android releases](https://github.com/mpv-android/mpv-android/releases).

## Technical debt, maintainability and missing practices

**The debt is manageable and concentrated: three oversized files, an aging media stack, and documentation that no longer tells the truth.** No rewrite is justified.

| Library | In use | Latest stable, 2026-11-05 |
| --- | --- | --- |
| Media3 | 1.5.1 | 1.11.1 |
| Jellyfin FFmpeg decoder | 1.5.0+1 | 1.9.0+1 (still tracks the FFmpeg 6.0 branch) |
| Room | 2.7.2 | 2.8.5 |
| WorkManager | 2.10.0 | 2.12.0 |
| DataStore | 1.1.2 | 1.3.0 |
| Coil | 3.3.0 | 3.6.3 |
| Kotlin | 2.3.20 | 2.4.21 |
| AGP | 9.0.1 | 9.5.0 |
| Compose BOM | 2026.03.01 | 2026.09.00 |
| Navigation 3 | 1.0.1 | 1.2.1 |
| Robolectric | 4.14.1 | 4.17 |

**Long-term concerns:**

- **Upgrade ceiling.** Jellyfin decoder versions track Media3, so about 1.9 is the practical limit unless the project self-builds. Plan that now.
- **Conflict magnets.** `PlayerScreen`, `MediaLibraryScreen`, and `PlayerViewModel` draw most changes, and earlier audits deferred splitting them to avoid merge conflicts. Doing it when one agent holds the area is cheaper than continuing to defer.
- **Bus factor of 1.** Owner decisions and all credentials sit with one person, and agents cite owner approval as a standing rule.
- **Documents as liabilities.** Stale claims now mislead the agents that rely on them (SHIP-02, DOC-01). Facts should be generated where possible.
- **Fixed version code.** No path to shipping an update until SHIP-03 is resolved.

**Missing practices worth adopting, in order:** dependency update bot and alerts; release workflow with checksums; coverage on PRs; baseline profile and macrobenchmark; Room migration test helper; visual goldens; detekt; debug-only StrictMode and LeakCanary; SBOM and dependency verification; reproducible builds, which suit the privacy pitch.

**Deliberately not recommended:** multi-module conversion, a dependency injection framework, a rewrite of the player in another engine, or adding analytics. None of them solves a demonstrated problem, and the last would break the product's identity.

## Prioritized improvement roadmap

**Do Phase 0 first: it fixes the worst user harm and every false public claim, and most of its items are small.** I estimate about 4 to 6 focused weeks to launch, excluding translation. Severity and timing differ on purpose: A11Y-02 is High but waits until Phase 2, while SEC-01 is Medium and takes minutes, so it goes first.

**Roadmap diagram, as text.** Finding: launch only after the honest device gate passes. Phases run in this order and are not to scale. Week ranges are estimates.

1. **Phase 0: Stop the harm and the false claims. About week 1.**
   - Bugs: STAB-01, FUNC-01, FUNC-02, DATA-04
   - Trust: PRIV-01, A11Y-01, SEC-01, SEC-04, PROD-01
   - Truth: SHIP-01, SHIP-02, SHIP-03 (mark v1.0.0 pre-release), DOC-01
   - Decide: LEG-01, DEV-03 (freeze process work)
   - Each reproduced bug gets a regression test that fails first.
2. **Gate A:** top 3 bugs gone, listing is true.
3. **Phase 1: Make the core trustworthy. About weeks 2 to 3.**
   - Lifecycle: FUNC-03, FUNC-04, FUNC-05, FUNC-06, FUNC-07, FUNC-08
   - Data: DATA-01, DATA-02, DATA-05, DATA-06, DATA-07, DATA-08, DATA-12, UX-01, UX-02
   - Tests: QA-01 (bundle privacy check first), QA-02, QA-04, QA-06
   - Quick accessibility: A11Y-03, A11Y-04, A11Y-05, A11Y-06, I18N-02, UX-03
4. **Phase 2: Harden and prepare release. About weeks 4 to 6.**
   - Stack: DEP-01, DEV-01, SEC-05, PERF-02, then PERF-01, PERF-03, PERF-05
   - Pipeline: DEV-02, SEC-06, QA-03, QA-05, QA-07
   - Reach: A11Y-02, DATA-03, DATA-11, PROD-02, PROD-G3, PROD-G5
   - Parallel track, not a launch blocker: I18N-01, I18N-03
5. **Gate B:** honest device pass with dated captures, on a low-end and a flagship phone, covering TalkBack, Bluetooth and task removal.
6. **Phase 3: Launch.** SHIP-04, internal then closed track, staged rollout, public release notes, new tag and version code. Watch user reports and diagnostic emails, since there is no telemetry by design.
7. **Phase 4: Differentiate. After launch, evidence-led.**
   - Language rollout, F-Droid (PROD-G2), libass (PROD-G4), playability report, privacy proof screen
   - Then chapters and playlists, seek preview, SMB docs, Android TV, private folder
   - Remaining Medium and Low items fit alongside the nearest related work.

Two things should be sequenced together: STAB-01 and FUNC-05 share one design, and I18N-01 should follow the `PlayerScreen` split to reduce conflicts.

**Fastest wins, each under a day:** FUNC-01, DATA-04, SEC-01, SEC-04, SHIP-01, SHIP-03, A11Y-01, A11Y-03, A11Y-04, DATA-07, PROD-01 and the bundle privacy check in QA-01.

## Unresolved questions, limitations and unverified areas

**This audit does not prove the app is safe to release: no real phone, TalkBack session, SD card or Bluetooth device was used.** It does prove several problems exist. Findings marked Traced are code-reading conclusions that could still be wrong in an edge I did not see.

**Limitations:**

| Not done | Why | What is needed |
| --- | --- | --- |
| Real-device playback, HDR, thermal, battery | Only an emulator was available | Two phones, one low-end and one flagship, on battery |
| TalkBack pass | The emulator run did not include one | Any device with TalkBack, 1 hour |
| Bluetooth latency and routing | No hardware | Bluetooth headphones and a clap test |
| SD card and USB moves (DATA-02, DATA-03) | No removable volume was set up | Device or emulated SD card |
| Process kill during a transfer | Not attempted | Scripted kill test |
| Android 8 to 10 and 16+ | One API 35 image used | Emulators or devices at these levels |
| Audio delay measurement (FUNC-04) | Not measurable on the emulator | Instrumented pipeline test |
| Queue recreation (FUNC-03) | My test used a single item | Queue test with `uiMode` flip |
| Connected test suite locally | Taken from CI results | None if CI is trusted |
| Full CVE scan | No scanner run across the graph; FFmpeg reachability not analysed | OSV scan and a decoder dump |
| Released APK signature | Not inspected | `apksigner verify --print-certs` |
| Full reading of every file | Scope and time | Details sheet, folder navigation and some specs got partial review |

**Evidence caveats:**

- 6 of 13 matrix files errored partly because the emulator lacks those decoders. Real results will differ by device.
- Competitor rows marked "m" and failure-share percentages are not measured data.
- About 972 literals is a heuristic count.
- RTL gesture direction stayed inconclusive. My scripted double-tap did not register as a double tap.
- One STAB-01 note: the reviewer linked it to the S24 stall, but I did not prove that link.

**Decisions only the owner can make:**

- [ ] Should v1.0.0 be marked pre-release now? (SHIP-03, recommended yes)
- [ ] How should the bundled decoders be handled legally? (LEG-01)
- [ ] Should transfers use a foreground service or resumable partial files? (DATA-03)
- [ ] Should Aptura stay Play-only, or also ship on F-Droid? (PROD-G2)
- [x] Should the multi-agent process be frozen? (DEV-03, recommended yes). Decided 2026-11-06 on the owner's instruction in their session to take the recommended option; see the DEV-03 row.
- [ ] Which languages come first? (I18N-01)

The earlier root `AUDIT_*.md` files report issues as Verified and phases as 100% complete. I did not treat them as evidence. The STAB-01 release call dates from commit \`9305123\` on 2026-10-23, itself an audit fix, so it predates the 2026-10-30 audit. Their "production readiness" conclusion should be withdrawn.

## Earlier audits: what was removed and what carries over

**The older audit files were removed from `main`, and they stay available in git.** To read one, use `git show b4de60b:<path>`, for example `git show b4de60b:AUDIT_TRACKER.md`.

Removed: `AUDIT_TRACKER.md`, `AUDIT_PROGRESS.md`, `AUDIT_CHANGELOG.md`, `AUDIT_TEST_RESULTS.md` and all of `review/` (267 files: audits of 2026-10-23, 10-24, 10-28 and 10-29, two dossiers and 216 captures). `docs/device-testing/` stays, since it holds the only real-device evidence.

I reconciled the deferred items in `AUDIT_TRACKER.md` and the open-item lists of the 2026-10-28 and 2026-10-29 reviews. I searched the rest (2026-10-23, 10-24 and the two dossiers) only for keywords and did not reconcile them item by item.

**Items not covered by a finding above, carried over unverified:**

| Old ID | Item | Status at this audit |
| --- | --- | --- |
| ISSUE-005 | Stats overlay wraps awkwardly at 200% font scale | Not rechecked |
| ISSUE-012, 10-28 item 3 | No "apply this audio sync to every video". Per-video storage cannot tell 0 from unset, so it needs a storage decision | Owner decision, also listed in `.agents/STATE.md` |
| ISSUE-016 | Unused `deleteRemovedVideos` query in `MediaItemDao` | Not rechecked; Room is a hot file |
| ISSUE-025, 10-28 F7 | Bluetooth hint means connected, not in use | Needs a real device |
| 10-28 F8 | Audio delay processor handles 16-bit PCM only | The playback review found the sink converts to 16-bit, so this looks harmless; confirm on a device |
| 10-29 limit 3 | On a 740 x 360 dp window the stats panel shows its first rows and scrolls | Accepted limit |
| 10-29 limit 5 | Landscape guide is one scrolling column | Accepted limit |
| 10-29 limit 8 | Samsung S24 playback report: cause not established, and the 8 s watchdog is a guess | Open. STAB-01 may be related, unproven |
| 10-29 limit 9 | The access effect in `Navigation.kt` has no test of its own | Open, low priority |
| 10-28 item 4 | Gallery: sheets do not scroll under Robolectric, and picture adjustments cannot render | Accepted limit |

Folded into existing findings: ISSUE-008, ISSUE-009 and ISSUE-019 (architecture seams), and the device-pass items (SHIP-02). ISSUE-003, a paused video resuming after a configuration change, did not reproduce here: it stayed paused through dark-mode and font-scale changes on the emulator.
