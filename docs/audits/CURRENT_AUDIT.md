# Aptura: End-to-End Project Audit

Date: 2026-11-05. Audited commit: `b4de60b` on `main`. Author: Claude Opus 5.5, for the owner, Tejas Gogara.

> **STATUS: ACTIVE. This is the only current audit of Aptura.** All earlier audits were removed from the repository because they are superseded. If another document or note disagrees with this file about what is open, this file wins, except that `.agents/STATE.md` wins on project facts.

## How to work from this audit

- **Order:** work the roadmap in phases, starting with Phase 0. Finding IDs such as `STAB-01` are stable, so use them in branch names, PR titles and commits.
- **Who starts work:** this is the backlog, not a work order. Take work only as `AGENTS.md` rule 1 says: from the owner directly, or from an issue labelled `agent-ready`.
- **Check first:** this is a snapshot of commit `b4de60b`. Before fixing a finding, confirm it still holds in the current code. Labels Traced and Suspected mean the path was not run.
- **Record progress:** when a PR fixes a finding, add a row to the table below in the same PR. A finding is closed only by a test or device evidence named in that row. Do not edit the findings themselves, so the baseline stays comparable.
- **New audits:** there is only ever one audit file. A new audit replaces this one, and open items carry over explicitly.

## Open parts

What is still open after the work recorded in the Progress table, in one place. Each line points at the row that explains it. Findings with no row in the Progress table have not been started, and are not listed here.

| Finding | What is still open | What unblocks it |
| --- | --- | --- |
| All "Fixed in code, not closed" rows | Nothing was tried on a device. `docs/DEVICE_CHECKLIST.md` has the unticked rows. | A person with a phone |
| SHIP-02 | Re-running TalkBack, Bluetooth, the screen-off sleep timer and task removal on a phone, with a dated capture linked from each ticked row | A phone |
| STAB-01 | Verified on an emulator only | A real phone |
| FUNC-05 | The Up Next countdown still lives in the screen. The view model still makes its own progress writes and follows the audio session itself; both go once a device confirms the process-scoped versions. | A device test, then removing the duplicates |
| FUNC-08 | The Continue watching shelf stops at 95% (`FINISHED_FRACTION`), the player at the last 10 seconds. Rows saved before the change are handled when resumed, not rewritten. | The owner: what counts as "watched" |
| FUNC-07 | "One atomic restart action" was not done, on purpose | Only if `PlayerScreen`'s callbacks change anyway |
| DATA-05 | The single work name; the longer lock measured with a large library. (A truly concurrent write test is verified in `ScanMergeDuringChangesTest`). | Code and a measurement |
| PROD-01 | The quiet thank-you the audit suggests | The owner |
| DOC-01 | `.agents/STATE.md` line 54 says a signed 1.0 tag waits for "a signing key"; only the owner can confirm that. It is a serialized hot file. | The owner |
| SEC-01 | Move any existing upload key out of the repository folder; a key that was ever pushed must be replaced | The owner |
| LEG-01 | Replace the decoder (ADR-010's route) or get the publisher's written terms and counsel's sign-off, before any store release | The owner |
| DEV-03 | Whether to remove the debate and status code for good | The owner |
| A11Y-04 | Seek bar and sheet controls measured in unit tests against solid backgrounds, not over a bright video frame behind the controls' scrim; not verified on a physical phone. | A person with a phone |
| A11Y-03 | Seen on a phone, in daylight, and with the font at 200%. Low-emphasis text on `SurfaceElevated3` is 4.3:1: one place that drew it there (the compatibility sheet's detail line) now uses the medium level, but the other ~50 uses were not each traced to the surface behind them. | A person with a phone; a pass over the screen gallery |
| SEC-04 | Removing `ACCESS_NETWORK_STATE` was not tried. WorkManager adds it for its network constraints and Aptura sets none, so it could be removed from the merged manifest; that changes a permission and the allow-list in `ManifestInvariants`, which the owner should decide. | The owner |
| SEC-02 | The limits (1000 entries, 64 MB unpacked) are judgement, not measured on real subtitle packs. (Picker execution off the main thread is verified by 6 automated unit tests in `SubtitleImportCoordinatorTest`). | A device |
| I18N-02 | Seen in Arabic or Hebrew on a phone (or with Developer options > Force RTL layout direction). The whole player is left to right now, so its top bar, sheets and menus are too, not only the seek bar and transport the audit named; they hold English text, so real right-to-left versions wait for the translation work (I18N-01). The library and settings screens were not checked in right-to-left. | A person with a phone |
| SEC-07 | `BROWSABLE` was not dropped from the Open with filter: the audit only said to consider it, and removing it changes the manifest and the intent-category allow-list in `ManifestInvariants`, which the owner should decide. A refused link is ignored without a message. The check is tested as a function; the activity's use of it is not, and an explicit intent was not sent to a real phone (device checklist). | The owner for `BROWSABLE`; a phone |
| PERF-05 | Battery drain and thermal reduction measured on a device during extended background audio playback | A device |
| UX-04 | Truncated media error presentation and external file title resolution on physical device | A phone |
| DATA-11 | Mixed-encoding subtitle packs verification on physical devices across diverse languages | A device |
| UX-02 | External single-open video launch and subsequent library rescan verification on a physical device | A phone |
| PERF-04 | Large-library thumbnail generation and LRU disk eviction on a physical device | A phone |
| I18N-03 | Locale-specific number, date, and language formatting verification across non-English device locales | A phone |
| UX-03 | Snackbar undo for clear queue, reset progress, and unhiding videos verified on a physical device | A phone |
| UX-06 | Touch target sizes and layout across diverse physical phone screen aspect ratios | A phone |
| UX-07 | Copy consistency and screen reader pronunciation across Android locales on a physical device | A phone |
| SEC-08 | Backup exclusion and D2D migration isolation on physical Android 12+ device | A phone |
| SEC-12 | R8 release log stripping verified on physical device logcat | A phone |
| UX-08 | Physical tablet and Android 16 letterboxing verification on real large screen hardware | A tablet |
| UX-05 | Player tools content description aligned with sheet title on physical screen reader | A phone |
| SEC-09 | Explicit offline DataSource factory verified on physical network intent attempt | A phone |
| A11Y-07 | 200% font scale verified on physical Android device across all player sheets | A phone |
| A11Y-06 | Not tried with TalkBack or Switch Access on a phone: whether the two actions appear in the TalkBack actions menu on the video area, and whether the slider is comfortable to set by tap. Volume has no control of its own in the player; the phone's volume keys cover it. | A person with a phone |
| DATA-09 | Watch history clear action verified on physical device with real media library | A phone |
| SEC-11 | Screen security FLAG_SECURE recents masking and overlay protection verified on physical phone | A phone |
| A11Y-05 | System caption preferences verification on physical device across diverse accessibility caption styles | A phone |
| PROD-G12 | Subtitle character set encoding overrides on physical device across legacy non-UTF8 media files | A phone |
| A11Y-02 | Hardware keyboard and D-pad remote pass on physical Android tablet / Chromebook | A tablet or Chromebook |
| PROD-02 | Human visual review of refreshed Play Store phone/tablet screenshots before store submission | The owner |
| QA-05 | Kover coverage artifact publishing in CI workflows (`.github/`) and physical device debug StrictMode/LeakCanary soak | Different-vendor `.github/` review; debug build on device |
| QA-07 | Nightly workflow (`.github/`) to generate the absent video fixtures from `tools/corpus/` and run them through `PlaybackSmokeTest` | Different-vendor `.github/` review |

When a PR closes an open part, delete its line here in the same PR, and add a line for any part it leaves open.

## Progress

| Finding | Status | PR | Evidence |
| --- | --- | --- | --- |
| DEV-03 | Done in a minimal form. The 15-minute status schedule is removed, so the bot no longer announces overnight silence. A process freeze note is in `.agents/STATE.md`. Kept: `pr_checks.py`, the PR template, attribution trailers and independent review for security, privacy and Room changes. Debates are paused, not deleted. Open: the owner decides later whether to remove the debate and status code for good. | This PR | The workflow no longer has a `schedule:` trigger; `python -m unittest discover -s tools/agents` passes; the wording in `AGENTS.md`, `STATE.md`, `AGENT_WORKFLOW.md` and `stalls.py` matches |
| DEV-04 | Mostly done: evidence removed from `main`. The older history stays in the git pack. | This consolidation | Removed files listed under "Earlier audits" at the end |
| SEC-01 | Done in code, one owner step left. `.gitignore` covers `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `*.pem`, `*.p8`, `*.key`, SSH keys and `.env` files. CI now fails any push or pull request that tracks such a file (`tools/security/check_no_secret_files.py`, which refuses to pass if it cannot list files). The keystore guide now says to keep the key outside the repository. Open: the owner moves any existing upload key out of the repository folder. The check sees the current tree only, so a key that was ever pushed must be replaced. | [#54](https://github.com/TejuBoricha/Aptura/pull/54), merged; [#56](https://github.com/TejuBoricha/Aptura/pull/56) | 9 tests pass, including a force-added `.jks` that makes the check exit 1 and name the file; the check exits 0 on the 492 files tracked today; `.env.example` stays allowed |
| LEG-01 | Decision recorded as DEC-014, legal sign-off still open. `NOTICE` no longer says the decoder is LGPL: its publisher labels it GPL-3.0. Checked on the shipped library: FFmpeg 6.0, linked statically into `libffmpegJNI.so`, with AC-3, E-AC-3, DTS, TrueHD, MLP, ALAC, AAC-LATM, A-law and mu-law decoders, which is outside ADR-010's approved set. Open: replace it with the self-built route in ADR-010, or get the publisher's written terms and counsel's sign-off, before any store release. | This PR | `strings` on `libffmpegJNI.so` for `ff_*_decoder` and `Lavc60.3.100`; the library's POM and upstream LICENSE; `tools/ffmpeg/verify_lgpl_symbols.py` approved list |
| STAB-01 | Fixed in code, verified on an emulator, still open for a real phone. `ApturaPlaybackService.onDestroy` now releases only the media session, so the player outlives the service. `MainActivity.onStart` starts the service again through `PlayerManager.ensureServiceRunning`. Swiping the task away with nothing playing still releases the player. | [#55](https://github.com/TejuBoricha/Aptura/pull/55) | `PlaybackServiceLifecycleTest` (4 cases, real service and real ExoPlayer under Robolectric), red without the fix and green with it in CI. The audit repro on a Pixel 8 emulator (API 35): a 7-minute clip, paused, 100 s in the background, the system stopped the service, on return it restarted, Play worked, and a second video played with no dead-thread errors; the old build failed the same test. Closes only after the same check passes on a physical phone. |
| DOC-01 | Partly done. `spec/00_INDEX_AND_STATUS.md` now opens with a banner saying it is a 2026-10-20 snapshot from before any code, points to `.agents/STATE.md` and this audit, and labels its status, verdict and "documentation only" constraint as historical. `docs/TESTING.md` no longer keeps test counts: it says where CI prints them. Still open: `.agents/STATE.md` line 54 says a signed 1.0 tag waits for "a signing key", while the finding says the owner's `local.properties` already has a release keystore section. That is a fact only the owner can confirm, and `.agents/STATE.md` is a serialized hot file, so it is left alone. | This PR | Documentation only, so no test applies. The links added to `spec/00` resolve, and the CI step names in `docs/TESTING.md` match `.github/workflows/ci.yml` |
| FUNC-03 | Fixed in code, verified on emulator, not closed for physical phone. `PlayerViewModel.openRouteVideo` loads the player route's video only when nothing is loaded, so a route that comes back after the system recreated the activity no longer restarts video 1 as a one-item queue. It still loads the key when the ViewModel is empty, the process-killed case restored from the saved back stack. `Navigation.kt` calls it in place of its own comparison. All six places that push a `PlayerKey` already load a video first. An automated instrumented test flipping `uiMode` and recreating the activity on queue item 2 is in `PlaybackSmokeTest` (`uiModeRecreationOnQueueItemTwo_preservesQueueAndCurrentItem`). Still open: nothing was checked on a physical phone. | This PR | `PlayerViewModelTest` (three tests) and `PlaybackSmokeTest.uiModeRecreationOnQueueItemTwo_preservesQueueAndCurrentItem` (instrumented test on real player with MediaStore video clips). In `PlaybackSmokeTest`, an activity recreation while playing item 2 confirms that the queue stays intact with all 3 items and remains on item 2. CI runs it on the API 35 tablet emulator |
| DATA-12 | Fixed in code, not closed. `MediaItemDao.deleteByUriPrefix` now compares the removed folder's URI as plain text and requires a `/` right after it, which is how the SAF scan builds a video's URI from its tree. It used a `LIKE` pattern, where `%` and `_` are wildcards and every tree URI carries a `%`, with no boundary, so removing `Movies` also deleted `Movies2` and look-alike folders, with their progress and bookmarks. A query change only: no schema change and no migration. Still open: nothing was checked on a device or emulator. | This PR | `ApturaDatabaseRealSqlTest`, two new real-SQL tests. On the old query both failed, with every row deleted (`expected:<[the other folders' videos]> but was:<[]>`). With the fix the class passes (7 tests, 0 failures) and `MediaRepositoryTest` passes (9 tests), locally on the Android 14 Robolectric jar. CI runs them on the default SDK |
| DATA-02 | Fixed in code, not closed. `MediaMover.canMoveAllInPlace` now needs every video to be on the destination's storage volume, so a move to an SD card folder (or back) goes through the copy path. An in-place move only rewrites `RELATIVE_PATH`, which never changes the volume, and `relativePathFor` drops the volume segment, so the app reported "Moved" while the file stayed on phone storage. The new `StorageVolumes` says which volume a folder path is on; the source volume comes from `VOLUME_NAME`, one query per 500 videos. `moveInPlace` also refuses a cross-volume move without writing anything. Still open: nothing was tried with a real or emulated SD card, so that the copy path lands the file there is not shown. | This PR | `MediaMoverTest` and `StorageVolumesTest`. On the old mover five tests failed, among them `moveInPlace_toAnotherVolume_failsAndChangesNothing` with "reported Moved". With the fix `MediaMoverTest` passes (20 tests) and `StorageVolumesTest` passes (7), locally on the Android 14 Robolectric jar. `lintDebug`: 0 errors, nothing in the changed files. CI runs the tests on the default SDK |
| DATA-06 | Fixed in code, not closed. `MediaFileTransfer` now shows a copy complete before it counts, in both copy paths: the bytes written must equal the size the source reported, and the size the destination reports, when each reports one. A mismatch fails the file, removes the partial destination and keeps the original; on the MediaStore path the check runs before the video is published. The destination's descriptor is also synced before it closes, best effort, because a provider may hand out a pipe. It used to count the copy as done when the source stream ended and the destination stream closed, and a move then deleted the original. Still open: no real USB, MTP, SD-card or cloud provider was tried, so a provider that reports a wrong or stale size may now have a good copy reported as failed (the original is kept). | This PR | `MediaFileTransferTest`, four new fault-injecting tests. On the old code the three that inject a fault failed with `expected:<0> but was:<1>` (the copy was counted as done); the fourth, a provider reporting the full size, passed on both. With the fix the class passes (13 tests) and so do the transfer worker, journal and scheduler tests, locally on the Android 14 Robolectric jar. CI runs them on the default SDK |
| DATA-01 | Fixed in code, not closed. A move by copy-then-delete no longer drops the video's index row and bookmarks when the original is removed. `MediaLibraryScreen` gives the move's "remove the originals" step its own delete flow, which calls `onVideoMovedAway`: the row is marked missing, with its progress, settings and bookmarks kept, and the next scan pairs the copy with it (`FileReconciliationEngine` already matches by size and length, matches rows already marked missing, and never deletes an unmatched one). A real delete still removes the row. No DAO or schema change. Changed behaviour: if the copy goes to a folder the library does not scan, the original stays as a missing video, with its progress, until the person removes it; before, it vanished. Still open: nothing was tried on a device. (Screen callback choice verified by automated tests in `MediaLibraryScreenUiTest`: `crossVolumeMoveCompletion_invokesOnVideoMovedAway_notOnVideoDeleted` and `permanentDeletion_invokesOnVideoDeleted_notOnVideoMovedAway`). | This PR | Real-SQL tests in `ApturaDatabaseRealSqlTest`: marked missing, the scan carries progress and bookmarks to the copy; removed first (the old behaviour), the copy starts unwatched and the bookmarks are gone; marked missing and nothing matches, the row stays with its data. View-model tests: moved away keeps the row, a real delete still drops it. `ApturaDatabaseRealSqlTest` (10), `MediaRepositoryTest` (9) and `MediaLibraryViewModelTest` (32) pass locally on the Android 14 Robolectric jar. Automated UI tests in `MediaLibraryScreenUiTest` assert the callback selection. CI runs them on the default SDK |
| DATA-05 | Fixed in code, not closed. `MediaScanWorker.persistScan` (the merge step of the scan, extracted so it can be tested) now reads the rows again inside the transaction that writes them and reconciles against those, instead of against the snapshot taken when the scan began. A scan spends minutes reading storage and then wrote whole rows back from that old snapshot, so a video finished, renamed or hidden during the scan could revert, a deleted row could return, and a row the person had just changed could be purged as having no user data. A row that was in the snapshot and is gone now was removed meanwhile, so the scan's stale finding of its file does not bring it back (the next scan re-adds the file if it is still there). The purge is decided from the current rows. Not done: the single work name the audit also suggests. Overlapping scans are now safe because each merges against current rows in one transaction, and changing WorkManager's names would change when scans run. Still open: nothing was tried on a device, and the longer lock was not measured with a large library. (A truly concurrent write test is in `ScanMergeDuringChangesTest.concurrentDatabaseWrites_duringScanMerge_areSerializedSafelyWithoutCorruption`). | This PR | `ScanMergeDuringChangesTest`, eight tests. Six tested user modifications between snapshot and merge, one tested new files, and `concurrentDatabaseWrites_duringScanMerge_areSerializedSafelyWithoutCorruption` tests simultaneous background coroutine database writes during `persistScan`. With the fix all eight pass, and so do the 18 existing scan tests (`AccessGrantedScanTest`, `ColdStartScanTest`, `MediaScanLogicTest`, `MissingRowPurgeTest`, `UnprobedMediaStoreRowsTest`) and `ApturaDatabaseRealSqlTest` (10), locally on the Android 14 Robolectric jar. CI runs them on the default SDK |
| DATA-08 | Fixed in code, not closed. A batch move no longer runs twice. After Android's one permission dialog for a batch, `MediaMoveFlow`'s result callback cleared `awaitingBatchConsent` at once but only filtered `moveQueue` after the whole loop had finished. The `LaunchedEffect` that walks the queue is keyed on both, so it restarted straight away, saw "nothing awaited" and a still-full queue, and began moving the same videos one by one next to the batch loop. Now `awaitingBatchConsent` stays set until the batch has finished and the queue is updated, and a `finally` clears it so a failed batch cannot leave the flow saying a move is still running. The same flag keeps a second move request out ("already in progress") for as long as the batch runs. The platform mover sits behind a small interface (`InPlaceMoveEngine`, a pure refactor) so the real composable can be tested; the public `rememberMediaMoveFlow` is unchanged. Still open: not tried on a device, with the real permission dialog or a real MediaStore (the tests' mover always returns Moved), so what a second move of an already moved file does on a phone is not known; the single-video consent path (one dialog per video) has no test; the `finally` is reasoned, not separately tested. | This PR | `MediaMoveFlowTest` (4), the first direct tests of this flow (`QA-04` named it untested). On the unchanged logic the race test failed: `expected [#1, #2, #3] but was [#1, #1, #2, #2, #3, #3]`, every video reached the mover twice. Three guards pass on both: one video is moved once and reported once; declining the permission moves nothing and reports every video failed; a second request while a batch runs is refused and its video never touched. The harness only reproduces the bug when the fake dialog is answered after the screen has been redrawn, as a person's tap is; answering inside `launch` hid it. With the fix all 4 pass on three runs of three, and so do the 256 tests of the `ui.library` and `core.media` packages, locally |
| FUNC-04 | Fixed in code, not closed. The audio delay is now applied once, in real time, and survives a speed change. Media3's sink flushes its processors when the speed changes, with no seek (read in the 1.5.1 bytecode: `setPlaybackParameters` drains the sink, then `AudioProcessorChain.applyPlaybackParameters` and `setupAudioProcessors` flush the pipeline), and `AudioDelayProcessor` took that flush for a seek and applied the whole delay again. It also sat before the speed change, so the silence was stretched with the audio. `DelayAfterSpeedChain` (new, in `AudioDelayProcessor.kt`) puts the delay after Media3's standard processors, and tells it through `keepOffsetAcrossNextFlush` that the flush straight after `applyPlaybackParameters` is a new speed, which keeps the offset for that one flush; a seek has no such call and still starts the offset again. `buildAudioSink` is the one place the sink is wired, used by `PlayerManager` and by the tests. Changed behaviour: a delay now means the same time at every speed, so at 2x it is twice what it was. Still open: not tried on a device and no clap test with Bluetooth earbuds; the Robolectric `AudioTrack` never plays, so what the media clock reports while the silence plays is reasoned from Media3's `getMediaDuration`, not measured; `setEnableAudioTrackPlaybackParams(true)` (not used) would stretch the delay again. | This PR | `AudioDelayThroughSinkTest` (7) plays a constant signal through the real `DefaultAudioSink` and counts the silence that reaches the `AudioTrack`. On the unchanged wiring three failed, each by the number worked out beforehand: +300 ms with two speed changes gave 36000 frames (750 ms) instead of 14400 (14400 + 7200 + 14400); the same delay at 2x gave 7200 (150 ms); -100 ms across two speed changes dropped 12000 frames (250 ms) instead of 4800. The guards passed on both: one delay with no speed change, a seek, and a seek after speed changes each give the full delay. With the fix all 7 pass, the 12 `AudioDelayProcessorTest` tests (4 new) and the 85 tests of the playback package pass, locally on the Android 14 Robolectric jar. Broken on purpose: without the `applyPlaybackParameters` call 3 tests fail (900 ms); with a mark that never clears 3 fail (a seek loses its delay). CI runs them on the default SDK |
| FUNC-05 | Fixed in code, not closed: all four losses it recorded are fixed (the queue stopped after one video, progress, the sound effects, and the notification tap), each by moving the work to the object whose lifetime matches it, in place of the single `PlaybackSessionCoordinator` the audit proposed. After the task is swiped out of Recents while a video plays, the foreground service keeps playing but the screen's view model is gone. `PlayerViewModel.onCleared` now tells the player to stop parking at the end of each video (`setPauseAtEndOfItems(false)`): the player had been told to park so the screen's Up Next countdown could run, and with no screen it stopped at the end of the first video and never advanced. The sleep timer's own "end of this video" stop is a separate flag and stays in force (`Media3PlaybackController.applyPauseAtEnd`), and the next screen sets the parking flag again from the settings. Back from the player is not affected: it pauses (`onPlayerDismissed`), and the view model lives as long as the activity, so this only concerns the swipe-away. Progress is now saved with no screen: `PlaybackProgressRecorder`, owned by `PlayerManager` beside the player and cancelled when the player is released, reads only the player's state and queue and saves a checkpoint every 5 s of playback, where playback stops when it pauses, the video that was left under its own name when another becomes current, and a finished video as complete at its full length (leaving a finished video does not undo that). A failed write does not stop it. The view model still saves too; the two write the same values, so the overlap is harmless, and removing the view model's own writes waits until the recorder has been tried on a device. The sound effects now stay on after the swipe-away: `PlayerManager` creates the `AudioEnhancementController` with the player, `AudioEffectsSessionFollower` moves it to the player's audio session whenever that changes, and it is released with the player. `PlayerViewModel` borrows it (`ownsAudioEnhancement = false`, passed by `Navigation`): when cleared it still saves the last settings, but it no longer releases effects it did not create, while a view model that made its own controller still releases it. The view model also still follows the audio session itself; attaching a session that is already held is ignored, so the overlap is harmless, and it can go the same way as the progress writes after a device check. Tapping the notification now shows the player. The notification's intent carries its own action (`MainActivity.ACTION_SHOW_PLAYER`), `MainActivity` turns it into a one-shot request on first creation and on `onNewIntent`, and `Navigation` pushes `PlayerKey` for the video loaded in the player (`playerKeyToShow`), but not when nothing is loaded and not when the player is already the screen. The route only looks at the video: it never loads or restarts one. Back from it goes to the library and pauses, as from any player. Tracing this found a defect on the same path, which also hit anyone reopening the app from the launcher icon after a swipe-away while a video played: a new screen treated the video it found playing like a video change. It seeked the player to the position saved when the queue was built (a video at 50 minutes went back to 48 seconds), pushed that old copy's speed and delays into the sound, and a route that ran before the view model had read the player could replace a three-video queue with its one key. A view model now adopts the first video it did not load itself: it reads the saved row, shows it, and sends the player nothing. Still open: the Up Next countdown lives in the screen (with no screen the next video starts at once), and the view model still makes its own progress writes and follows the audio session itself; both go once a device confirms the process-scoped versions. Not tried on a device. | This PR | `PlayerViewModelTest.whenTheScreenIsGoneForGood_thePlayerStopsParkingAtTheEndOfEachVideo`: on the unchanged code it failed (`expected:<false> but was:<true>`, the player stayed parked); with the fix it passes. `Media3PlaybackControllerQueueTest.theSleepTimersStop_staysInForce_whenParkingForTheCountdownIsSwitchedOff` passes before and after, as the standing guarantee that the sleep timer still stops. The 274 tests of the `ui.player` and `data.playback` packages pass locally. `PlaybackProgressRecorderTest` (9), run with no screen and no view model. Written against a do-nothing stub first, six of the first eight failed because nothing was saved, and the two "saves nothing" guards passed; with the recorder all 9 pass on two runs. Broken on purpose, one rule at a time: leaving a finished video saved it again as unfinished, 1 test fails; the video being left never saved, 1 fails; a failed save allowed to stop the recorder, 1 fails. The 288 tests of the `data.playback` and `ui.player` packages pass, locally. Effects: `AudioEffectsSessionFollowerTest` (4), written against a do-nothing stub first: 3 failed because no effects were attached and the guard that nothing attaches for an invalid session passed; with the follower all 4 pass on two runs. `PlayerViewModelTest.effectsLentByThePlayersOwner_surviveTheScreen_andTheirSettingsAreStillSaved`: on the unchanged code it could not be written, because the `ownsAudioEnhancement` parameter did not exist (compile error), so there was no failing run to show; broken on purpose by releasing borrowed effects again, it is the only one of 107 view-model tests that fails, and the existing `soundEffects_followThePlayersAudioSession_andAreReleasedWithTheViewModel` (a view model that owns its effects) stays green. The full unit suite passes locally: 1,116 tests, 0 failures, 165 skipped (the screen-gallery tests, which run only with `APTURA_GALLERY_DIR` set); lint 0 errors, 63 warnings, none in the changed files. Not tried on a device: that the equalizer, bass boost and virtualizer really stay audible after a swipe-away is the first row to tick in `docs/DEVICE_CHECKLIST.md`. Notification and joining a live player: on the unchanged code three `PlayerViewModelTest` tests failed for the reasons above (`expected null, but was 48000`; the sheet showed a delay of 0 where 300 was saved; the queue went from 3 videos to 1), and with the change they pass on two runs. Broken on purpose, one change at a time: pushing settings and seeking into a joined player, 1 fails; showing the queue's old copy instead of the saved row, 1 fails; the route replacing a live player, 1 fails; treating a joining screen as any video change, 2 fail. `ShowPlayerRequestTest` (6) against do-nothing stubs: the 3 tests of real behaviour failed and the 3 "opens nothing" guards passed; with the code all 6 pass on two runs, and each of three broken-on-purpose versions fails its own test. The full unit suite passes locally: 1,125 tests, 0 failures, 165 skipped (the screen-gallery tests); lint 0 errors, 63 warnings, none in the changed files. Not tested: the wiring itself (the service using the new intent, the activity reading it, the navigation effect) is covered only by compilation, because it needs a running activity and player; nothing was run on a device |
| DATA-07 | Closed in code and verified by unit & UI tests; not tried on a device with a really damaged file. The settings DataStore has a `ReplaceFileCorruptionHandler` (`settingsCorruptionHandler`): an unreadable file is replaced by an empty preferences instance tagged with `SETTINGS_RESET_BY_CORRUPTION = true`. The app starts from default settings without throwing, surfaces a quiet, non-blocking notice banner on `MediaLibraryScreen` informing the user that settings were restored to defaults while custom folders were preserved, and auto-recovers persisted SAF folder tree URIs from Android's `context.contentResolver.persistedUriPermissions`. Dismissing the notice invokes `acknowledgeSettingsReset()`, clearing the corruption reset flag and permanently writing back the recovered folder list. | #68, this PR | `SettingsCorruptionTest`: damaged file initiates reset flag and starts from defaults; healthy file has no reset flag; save survives restart. `ApturaSettingsRepositoryTest`: `wasResetByCorruption` surfaces in `settingsFlow`, auto-recovering SAF tree permissions; `acknowledgeSettingsReset` clears flag and persists recovered folder set. `DiagnosticsUiTest`: "Settings restored" banner renders with action button and dismisses cleanly. |
| SHIP-02 | Partly done: the unproven ticks are reverted, the device re-runs the finding asks for are still open. `docs/DEVICE_CHECKLIST.md` now ticks a row only when a dated device run backs it: ticked rows went from 19 of 22 to 16 of 27 (11 open). TalkBack on the slider, sheet tabs, HDR (a partial pass on `c2.exynos.hevc.decoder`, tone mapping not judged) and process death with audio (failed on 2026-10-29, fix not retested) are unticked with the real result written next to them; the thermal row keeps its tick with its caveats (USB power, a 30 fps test pattern, wrong `ap_c` and `skin_c` columns, and adb cannot measure audio/video desync). New unticked rows for `STAB-01`, `FUNC-01`, `FUNC-02` and `A11Y-01`; a row for `FUNC-04` was added afterwards. `README.md` (two places), `docs/RELEASE_KIT.md` (two lines) and `CHANGELOG.md` (one line) no longer say "physically verified", "Verified 4K 60fps and HDR10 hardware decoding" or "survives background playback". Still open: re-running TalkBack, Bluetooth, the screen-off sleep timer and task removal on a phone, and a dated capture linked from each ticked row (the audit's own check). | #67 | Every status was checked against `docs/device-testing/2026-10-29-galaxy-s24-findings.md` line by line, and the numbers quoted in README and CHANGELOG (30 minutes, 2.5 GB, 42%, 0 throttles, 0 dropped frames, USB power) come from the same file. `python tools/security/check_no_secret_files.py` passes. Documentation only, so no test applies |
| PROD-01 | Fixed in code, not closed. The Gold accent is open to everyone, like the other five. Removed: the lock on Gold (its icon, the "Gold 🔒" label and the toast), the "I Supported Aptura" honour toggle, the "Champagne Gold Active" badge and the "⭐ Supporter" footer star. "Star on GitHub" and "Share app" are plain links again; starring no longer flips a flag and switches the accent. The share text says "no internet permission" instead of "zero permissions" (the app requests seven permissions, see SHIP-01). `ApturaSettings.isSupporter` and `updateSupporter` stay, unread, so nothing breaks for an installed copy that stored the flag. Not done: the quiet thank-you the audit suggests; `DESIGN.md` section 5, which still describes the locked Gold (a protected file, so its own PR); the store screenshot `05_supporter_gold_unlocked.png` (PROD-02); and the policy read before submission, which is a human step. Not tried on a device. | #66 | `SettingsAppearanceTest`: `goldIsOpenToEveryone_withNoLockAndNoSupporterToggle` (choosing Gold reports Gold, and no lock, toggle or badge exists) and `anOldSupporterFlag_doesNotAddABadgeOrLabel`. Against `main`'s code before the change they failed: `expected:<GOLD> but was:<null>` (the tap was swallowed by the lock) and a leftover label. With the change all 7 tests of the class pass, locally and in CI on the merged head |
| FUNC-06 | Fixed in code, not closed. The Up Next countdown no longer outlives what started it. It ran in the screen's view model, and only that view model's own methods cancelled it, so a Bluetooth headset's Next, the notification or another app, which go straight to the player, left it running: Next went to video 2, then the countdown expired and skipped on to video 3. `PlayerViewModel` now cancels a running countdown when the controller reports any state other than Ended, and when the current video changes. The controller reports Ended for as long as the player stays parked at the end of an item (`pausedAtItemEnd`, read in `Media3PlaybackController.updatePlaybackState`), so a legitimate countdown is not cancelled by its own parked state; it ends at the next item change or when playback starts. Still open: not tried with a real headset or the notification on a device; the "Ended while parked" contract is read from the controller code and exercised through a fake controller, not through a real ExoPlayer; and `FUNC-05` is untouched (the countdown still lives in the screen's view model, so once the task is removed nothing runs it). | This PR | `PlayerViewModelTest` gained five tests. On the unchanged code three of the first four failed, each for the bug: the video the person chose is skipped (expected video 2, was video 3), the Up Next card stays on screen after an external Next (expected none, was 10 seconds), and a restart of playback leaves the countdown running (expected none, was 10). The fourth, an uninterrupted countdown still advances exactly once, passed on both. The fifth (the queue moves on while the state still reads Ended) was written afterwards to show the queue rule is not dead code. Broken on purpose, one rule at a time: without the state rule `playbackStartingAgain_duringTheCountdown_cancelsIt` fails; without the queue rule `theQueueMovingOn_duringTheCountdown_cancelsIt` fails. With the fix `PlayerViewModelTest` (105) passes, and so do the 277 tests of the `ui.player` and `data.playback` packages, locally |
| FUNC-08 | Fixed in code, not closed. Two faults from the audit's entry. (1) Next then Previous returned to the position of the queue's old copy, which dates from when the queue was built. `PlayerViewModel.observePlaybackQueue` now reads the saved row on every video change (it already did for a screen joining a live player), so Previous goes back to where the video was left. (2) The near-end rule. The new pure `ResumeRules` (`domain/usecase`) says a position in the last 10 seconds of a video, or the last twentieth of a clip shorter than 200 seconds, is finished, and gives where a saved position starts (none: from the beginning, for a watched video, a position of 3 seconds or less, or a finished one). It is used at save: `MediaRepositoryImpl.savePlaybackPosition` stores a near-end position as watched whichever writer sent it, however a last paused tick and the ended save happened to be ordered. It is used at resume: in the player, and in the details sheet's button, which said Resume from one second in and for a video a second from its end while the player started both from the beginning. Rows saved before this change are handled when they are resumed, not rewritten. Left as it was: the library's Continue watching shelf has its own 95% rule (`FINISHED_FRACTION`), so a video at 96% leaves the shelf but still resumes; joining the two decides what counts as watched, which is a product call. `PlaybackStateJournal`, which the audit says has no production callers, is untouched. Not tried on a device. | This PR | Seven tests failed on the unchanged behaviour, each for the audit's reason: the video resumed three seconds from its end (`expected null, but was 10137000`), a position 200 ms from the end was stored as not finished, Previous after Next sought to 48000 where 1198000 was left (the queue's old copy), and four of `ResumeRulesTest` against a stub that kept today's behaviour. With the change they pass on two runs, and the guards (a mid-video position stays unfinished, a flag the caller set is kept, an unknown length trusts the caller, a position of 3 seconds or less is not resumed) pass before and after. Broken on purpose, one change at a time, each file restored byte for byte: a short clip given the full ten-second window, 2 fail; the boundary off by one, 2 fail; the repository ignoring the rule, 1 fails; Next and Previous back on the old copy, 1 fails; resume back on the old condition, 1 fails. `DetailsPlayLabelTest` (4, new): on the old label 2 failed (Resume shown for a position of 2 seconds and for one a second from the end) and 2 guards passed; it passes on two runs, and the old rule put back fails the same 2. The full unit suite passes locally: 1,143 tests, 0 failures, 165 skipped (the screen-gallery tests); lint 0 errors, 63 warnings, none in the changed files. One earlier full run failed `MediaLibraryViewModelTest.searchDebounce_echoesRawQueryImmediately_andFiltersAfterPause` (it expected the query `Avatar` and got `Av`); that class passed 12 of 12 runs alone and the full suite passed on the next run, so it looks like a load-sensitive race in a test this change does not touch, not fixed here. Not tried on a device |
| FUNC-07 | Fixed in code, not closed. Retry no longer rebuilds the queue. `PlayerViewModel.retryPlayback` called `loadAndPlay`, which makes a queue of the one video and applies its saved settings again, so a video that failed as number 3 of 10 left a queue of 1. It now tells the player to try again (`PlaybackController.retryPlayback`, which keeps the playlist and the position), and starts the current video again only when the player has nothing loaded, for example after it was recreated. The software-decoder button reverting on the first tap, which the audit only suspected, was the same cause: the button chooses software and then retries; the retry applied the saved settings, and the software choice is saved only a moment later, so the decoder went back to its saved value. With Retry going through the player that step is gone. Not done: the audit's "one atomic restart action" as a single function. The button still makes two calls, in the right order and no longer interfering; one call would change the screen's callbacks, which 18 call sites pass. Not tried on a device. | This PR | `PlayerViewModelTest`, on the unchanged code: `retryOnItemThreeOfTen_keepsTheQueue_andRetriesThroughThePlayer` failed with `expected:<10> but was:<1>`, and `trySoftwareDecoding_onTheFirstTap_staysOnSoftware` failed with `expected:<2> but was:<0>` (the player ended on the saved decoder, which is the revert). A guard, `retry_whenThePlayerHasNothingLoaded_startsTheCurrentVideoAgain`, passed before and after. With the change all 3 pass on two runs. Broken on purpose: the exact original Retry body put back, the first two fail; Retry always taking the "rebuild" branch fails only the queue test, because the extra step hides the decoder race, so that variant is weaker than the original and is not counted as proof of the second test; Retry doing nothing when the player has nothing loaded, the guard fails. Each file restored byte for byte. The full unit suite passes locally: 1,146 tests, 0 failures, 165 skipped (the screen-gallery tests); lint 0 errors, 63 warnings, none in the changed file. Not tried on a device: the decoder failures this button exists for need a real video the phone cannot decode in hardware |
| FUNC-09 | Done in code, not closed for physical phone. The start watchdog is fixed in code, and the lost surface is fixed. The 8 s start watchdog in `Media3PlaybackController` armed whenever the player was meant to be playing and was not, so a video that had played and then buffered for 8 s (a slow network share, a long seek) was switched to the software decoder as though it had failed to start. It now guards only the start: `currentItemStarted` is set when the video plays and cleared whenever the player goes idle (every decoder change, retry and fallback passes through idle), when a new video begins without playback carrying over, and when a queue is loaded, and the transition re-checks the watch. A video that plays and then buffers is left to the player; a video that never starts, including the next one after an advance, a restart after a decoder change and the same video loaded again, is still caught. The video surface is preserved on runtime check error (does not call `clearVideoSurface()`), so Retry resumes video playback without a black picture. Still open: nothing was tried on a physical phone. | This PR | `Media3PlaybackControllerStallTest` (14 tests, including `runtimeCheckError_doesNotClearVideoSurface_andRetryPreservesSurface`). On the unchanged controller `aVideoThatBuffersAfterItHasStarted_isNotStuck_andIsNeverSwitchedToSoftware` failed with `a long buffer is not a failed start expected:<0> but was:<1>`. On the surface check before the fix, `runtimeCheckError_doesNotClearVideoSurface_andRetryPreservesSurface` failed with `expected:<0> but was:<1>` because `clearVideoSurface` was called; with the fix it passes and retry prepares and plays cleanly. The full unit suite passes locally. CI runs them on the default SDK |
| FUNC-10 | Fixed in code, not closed. All four parts are fixed. (1) Stats: the drop percentage was always 0% or 100%. `renderedVideoFrames` was declared, reset and used in the percentage, but nothing ever filled it in, so every dropped frame was a share of itself, and the stats overlay turns the line into a warning above 5%. The controller now reads the enabled video renderer's `DecoderCounters`, keeps the frames of a renderer switched off by a decoder change, starts again for a new video, and shows a rate only from 30 frames on (fewer say nothing: 3 drops in 23 frames would read 13% and warn for nothing); the count of dropped frames is shown regardless. (2) Hardware only: Hardware was treated like Auto. A decoder failure, or a video that did not start within 8 seconds, switched to the software decoder behind the person's back. Only Auto does that now. With Hardware chosen the failure is reported, the "did not start" message says the software decoder was not tried and points to Decoder in the player tools, and the decoder-failure screen's own "Try software decoding" button is the explicit way to switch. (3) Play next under shuffle: ExoPlayer puts a new video at a random place in the shuffle order, so "Play next" played a random video. `placeInShuffleOrderRightAfterCurrent` keeps the order as it stands and moves the new video to right after the one playing, so videos already played in the pass stay behind it and the pass still visits every video once. (4) Unsupported audio tracks: every audio track was listed as if it could play, one with no decoder on the device could be chosen (leaving the video silent), and when the audio failed the fallback took "the first track that is not selected", which could be one with no decoder. Each audio track now carries `isSupported` from Media3's own check (no decoder at all; a track that only exceeds the device's capabilities still plays and still counts as supported). `selectAudioTrack` refuses an unsupported track, the audio-failure fallback skips them, and the audio sheet lists them dimmed with "Not supported on this device" and takes no tap (`SheetOptionRow` has a new `enabled` parameter, true by default, so the other sheets are unchanged). Not tried on a device. | This PR | Written against stubs of the old behaviour first. Stats: 5 of 8 failed (`expected:<0.1> but was:<100.0>` through the real controller; `3 of 23` read `13.04`), with 3 guards passing. Hardware only: 3 failed (`expected:<0> but was:<1>` for both the decoder failure and the stalled start; the message still said the player "was waited on"), with the Auto guard passing. Shuffle: 2 failed (round 0 already played a random video next), with the not-shuffling guard passing. All pass with the changes, on two to three runs. Broken on purpose, one part at a time, files restored byte for byte: frame counting off, a rate shown from the first frame, a new video not starting its counts again, a decoder change forgetting the frames counted (each fails its own test); a decoder failure or a stalled start falling back with Hardware chosen, the controller not passing hardware-only on, the message ignoring it (each fails its own test); Play next left to ExoPlayer, put at the front of the order, or everything reshuffled so played videos return (each fails). One of my mutants was too weak at first and the message test caught it only after I added an assertion on the suggested action. Part 4: on the stub field, 4 controller tests failed (the fallback chose the unsupported track, `expected:<2> but was:<1>`; nothing was marked; selecting it was allowed; and the all-unsupported case moved the choice), then the view-model test (`expected:<[true, false]> but was:<[true, true]>`) and the sheet test (no reason text); the supported-selection guard passed. Broken on purpose, each caught: tracks not marked, an unsupported track selectable, the fallback allowed to pick one, the view model dropping the flag, the row taking taps while disabled, the sheet not disabling or not giving the reason. Pointer clicks do not land on these rows under Robolectric (the sheet's scroll handling; two ordinary supported rows did not register one either), so my first sheet test passed or failed for the wrong reason; it now asserts the disabled state and the reason text, and a semantic click on a supported row, which the existing tests in that file also use. The full unit suite passes locally: 1,175 tests on the part 4 branch (1,168 before it), 0 failures, 165 skipped (the screen-gallery tests); lint 0 errors, 63 warnings, none in the changed files. Not tried on a device: the real renderer counters, a real hardware decoder failure and a real shuffled queue were not used; the tests drive `DecoderCounters`, a stand-in player, and a real ExoPlayer under Robolectric |
| DATA-10 | Fixed in code, not closed. A rename dropped the real extension when the new title had a dot in it. `ApturaMediaFileManager` took any suffix of two to five letters or digits after the last dot for an extension, so "Mr.Robot" on a `.mkv` was sent to the provider as `Mr.Robot` and the file lost its type (the same for "Dr.Who" or "Episode.One"). The rule is now one pure function, `resolveFileName`: a typed suffix counts as an extension only when it is the file's own (any case) or another known video extension the person chose on purpose; anything else is part of the title and the real extension is added. A name that ends in another file type, such as "notes.txt", is no longer kept as if it were video. Not tried on a device. | This PR | `ResolveFileNameTest` (11, new) and one added case in `ApturaMediaFileManagerTest`. Against a stub that reproduced the old rule, the first 10 table tests gave 3 failures (`expected:<Mr.Robot[.mkv]> but was:<Mr.Robot[]>`, the same for "Dr.Who", and "notes.txt" kept as video) and 7 passes (a name with no dot, the file's own extension in any case, a deliberate choice of another video extension, a year, an episode code, a space after the dot, the file's type used when its name has no extension, and nothing known); the 11th test (an uncommon extension the file already has) and the real-rename case were added afterwards. All pass with the fix on two runs. Broken on purpose, each file restored byte for byte: any typed suffix counting as an extension again, 4 fail; a deliberate other video extension no longer kept, 2 fail; the file's own uncommon extension no longer recognised, 1 fails. The full unit suite passes locally on the rebased branch: 1,187 tests, 0 failures, 165 skipped (the screen-gallery tests); lint 0 errors, 63 warnings, none in the changed file. Not tried on a device: the rename went through a fake provider and a local file, not a real MediaStore rename, and the list of video extensions is mine, not from a standard |
| DATA-14 | Fixed in code, not closed. Library search compared the raw title, but the card shows `cleanDisplayTitle(title)` (dots and underscores turned into spaces), so "Show S01E01" missed `Show.S01E01` and "cafe" missed "Café". `matchesSearch` (`domain/model/SearchText.kt`, pure Kotlin) now folds the title or folder name and the query the same way: lower case, accents removed, and dot, underscore and hyphen read as spaces. Whatever matched before still matches (it is tried first), and a query that folds to nothing (only punctuation) matches only what contains it as typed, never everything. `MediaLibraryViewModel` uses it for both the title and the folder name. Not done: the audit also notes that `MediaItemDao.searchVideos` is dead code; it is untouched, and it would have the same raw-title problem if anything used it. Not tried on a device. | This PR | `SearchTextTest` (8) and two cases in `MediaLibraryViewModelTest` that run the real library filtering. Against a stub that kept the old raw-title match: 7 failed (`expected:<[Café Society]> but was:<[]>`, `expected:<[Show.S01E01]> but was:<[]>`, and five of the pure tests) and 3 guards passed (what matched before still matches; the spelling still has to match; a query that folds to nothing does not match everything). All pass with the change on two runs. Broken on purpose, one part at a time, files restored byte for byte: accents not removed, 1 fails; separators not read as spaces, 5 fail; no shortcut for what matched before, 1 fails; an empty folded query matching everything, 1 fails; the candidate not lower-cased, 5 fail; the library going back to the raw title, 2 fail. The full unit suite passes locally on the rebased branch: 1,197 tests, 0 failures, 165 skipped (the screen-gallery tests); lint 0 errors, 63 warnings, none in the changed files. Not tried on a device or with a large library: each search now folds every title that the plain match did not already accept, which adds work per query on the compute dispatcher (PERF-03 is about scale) and was not measured |
| QA-04 | Fixed in code, not closed. Pure formatters, delete flow, playback service, and media inspector on corrupt/empty files are tested. `MediaMetadataInspector` releases native retriever handles safely in `finally` (preventing native leaks when `setDataSource` throws), and sets `inspected = true` only when valid tracks are extracted. 6 unit tests in `MediaMetadataInspectorTest` assert graceful handling of corrupt bytes, zero-byte files, nonexistent URIs, fake text files, retriever runtime exceptions, and track extraction on valid media using Robolectric shadows. `MediaDeleteFlow` has 7 direct tests in `MediaDeleteFlowTest` covering direct delete without consent, single item consent granted/declined, batch delete consent granted/declined, concurrent in-progress delete refusal, and empty queue no-op. `ApturaPlaybackService` has 5 direct tests in `ApturaPlaybackServiceTest`. Not tried on a physical device. | This PR | `MediaMetadataFormattersTest` (12), `MediaDeleteFlowTest` (7), `ApturaPlaybackServiceTest` (5), and `MediaMetadataInspectorTest` (6). The full unit suite passes locally. |
| SEC-07 | Fixed in code, not closed. The manifest's Open with filter lists `content` and `file`, but a filter only matches implicit intents: another app can address `MainActivity` directly and put anything in `data`, and `launchUriFrom` passed it to the player under the app's own identity (an `https` link, or a `file` path into the app's own databases and settings). `MainActivity.acceptedViewUri` now accepts a `content` URI, or a `file` URI that does not resolve into the app's private storage (`..` and symbolic links followed, so neither climbs back in; a sibling folder with the same name prefix is still allowed), and refuses everything else, a path it cannot resolve, and a `file` URI with no path. The scheme is matched without regard to case, as Android matches it. | This PR | New `ViewIntentUriTest` (10 tests). Five fail on the stub of the old behaviour (an `https` link, a file in the private folder, a `..` climb, a symbolic link, an empty path); the other five pass before and after on purpose (a content URI and a shared-storage file still open, case, the sibling prefix, and a non-view intent). Four deliberate breaks (other schemes allowed, no path resolution, prefix match without the separator, case-sensitive scheme) each turned the matching tests red, and the file was restored byte for byte. **Not verified:** an explicit intent on a real phone, and `BROWSABLE` (Open parts). |
| I18N-02 | Fixed in code, not closed. In a right-to-left language the player half mirrored. Measured in Robolectric under a right-to-left layout: the transport row ran Next, Forward, Play, Rewind, Previous from left to right (Previous at x=270, Next at x=50), the elapsed time was on the right and the total on the left, the brightness gauge appeared on the right half, and a double tap on the **left** half fast-forwarded, because the gesture zones are a plain `Row`. `PlayerScreen` now lays its stage out left to right whatever the system direction is, with a `CompositionLocalProvider(LocalLayoutDirection provides Ltr)` around it. A video's position runs left to right in every language and the gestures are named for the side of the glass. Text still reads right to left by its own content (Compose picks the paragraph direction from the text). The wrapper covers the whole player, not only the transport and seek bar the audit named, because the stats panel docking, the HUD sides and the gesture zones all depend on the same direction. | This PR | New `PlayerRtlLayoutTest` (5 tests). On the old code four failed with the numbers above; the left-to-right control passed. A break that forces right to left instead turned all five red, including the control, so the measurements do move with the direction. The 226 player and navigation tests pass on the fix. **Not verified:** a phone set to Arabic or Hebrew, and any other screen in right-to-left (Open parts). |
| SEC-02 | Fixed in code, not closed. The zip import capped each subtitle at 20 MB and nothing else. Now the whole archive is bounded: every entry counts toward 1000 and every byte unpacked toward 64 MB, **including entries that are skipped**, which `ZipInputStream` unpacks anyway to reach the next one (before, a zip of 120 MB of non-subtitle files plus a subtitle imported fine, spending the CPU for all of it). Each import gets its own folder under `subtitles_extracted`, and only the chosen subtitle is kept: before, every subtitle in the zip stayed on disk (up to 20 MB each, with no total limit) and a second zip with an `English.srt` overwrote the first one's file while the player could be reading it. A refused import leaves nothing behind, not even a folder; folders untouched for 7 days are removed on the next import. The cache screen (`ThumbnailRepository`) now counts and clears those folders; its plain `delete()` would have failed on them. The file picker validated the subtitle on the main thread, so a large zip froze the player; it now runs on the IO dispatcher decoupled via `SubtitleImportCoordinator`. Furthermore, `SubtitleCharsetNormalizer` now writes normalized copies to dedicated per-import directories (`norm_${UUID.randomUUID()}/<filename>`) and prunes stale normalizations older than 7 days, so two subtitle imports with the same name never collide or overwrite each other. | This PR | Six of the ten extractor tests failed on the old code. `SubtitleCharsetNormalizerTest`: asserts two file imports with the same name and two byte imports with the same display name write to separate directories and preserve their distinct content; asserts `pruneStaleImports` removes directories older than 7 days and preserves fresh ones. `SubtitleImportCoordinatorTest` (6 tests) asserts that picker validation executes strictly off the Android UI main thread (`Thread.currentThread() != Looper.getMainLooper().thread`), verifies target URI selection, error toast dispatch, and haptic feedback. All 20 tests pass. **Not verified:** the limits on real subtitle packs on a device (Open parts). |
| A11Y-03 | Fixed in code, not closed. Low-emphasis text now passes AA on the surfaces it sits on, in both modes. The dark token went from `#606978` to `#8A93A1` (3.48:1 on the base and 2.84:1 on sheets before; 6.2:1 and 5.1:1 now), the light token from `#6B7280` to `#646B78` (4.47:1 on the base before; 5.0:1 now, 4.7:1 on the darkest light surface). Medium emphasis is above 4.5:1 on every surface and the three levels keep a visible step (low is at least 0.5:1 below medium on the base). `DESIGN.md` carried ratios measured on white alone, and two others were wrong (`TextHighEmphasis` 16.2 was 17.5, `TextOnAccent` 17.5 was 12.4); the tables now give the worst surface. The compatibility sheet drew its detail line in low emphasis on `SurfaceElevated3`, the one surface low emphasis does not reach 4.5:1 on (4.3:1), so it uses medium emphasis. | This PR | New `ApturaContrastTest` measures the real palettes on every surface (WCAG luminance, 4.5:1 for text and 3:1 for icons). Run against the old values it failed with the audit's own numbers (3.48, 2.84, 4.47 and the light 4.20 on the darkest surface). Three deliberate breaks (dark low at `#7A8494`, low equal to medium, dark track at `#4A515E`) each turned exactly the matching test red, and the file was restored byte for byte. **Not verified:** seen on a phone; which of the ~50 low-emphasis uses sit on `SurfaceElevated3` besides the one fixed (the tokens are right but placement was not traced). Open parts lists both. |
| A11Y-04 | Fixed in code for all tracks and sliders, not closed. The dark `ControlTrack` token was raised to `#687282` (3.23:1 on `SurfaceElevated2` sheets, 3.65:1 on cards, 4.32:1 on black and base) so it passes the 3:1 WCAG control boundary requirement on every surface it sits on. All other tracks in the app (`ApturaSlider` in player sheets, equalizer faders in `AudioEnhancementSheet`, switch off track in `PlayerControls`, countdown progress in `UpNextOverlay`, and transfer progress in `MediaLibraryScreen`) now use `ControlTrack` instead of `SurfaceElevated3`. | This PR | `ApturaContrastTest.darkControlTrack_isVisibleOnTheSurfacesItSitsOn` asserts >= 3.0:1 across black, base, cards, and sheets; failed on `#5E6674` with 2.72:1 on `elevated 2 (sheets)` and passes on `#687282`. `PlayerScreenUiTest` and `AudioEnhancementSheetUiTest` pass. **Not verified:** the bar over a bright video frame, and verification on a physical phone (Open parts). |
| SEC-04 | Done: the text was already fixed in `e08cc09` (with SHIP-01), and this adds the guard that keeps it fixed. `docs/PRIVACY.md` had listed four permissions and said `ACCESS_NETWORK_STATE` was not requested, while the release APK holds eight. Now `docs/PRIVACY.md` lists all of them with why, and `PrivacyPolicyPermissionsTest` compares it with the merged release manifest: every permission the app holds must be in the table (or, for the signature permission, named under it), none may be listed as not requested, and no table row may be for a permission the app no longer holds. A permission a library adds already failed `MergedManifestInvariantsTest` until approved; now approving it also needs the policy text. Also, `docs/PRIVACY.md` and the store metadata folder are declared inputs of the unit-test task in `app/build.gradle.kts`: without that Gradle called the test up to date after an edit to those files, so a local run stayed green on changed text (`StoreListingClaimsTest` had the same gap). | This PR | Run against the policy text from before the fix (`git show e08cc09^`) the test fails; two other breaks (the `WAKE_LOCK` row removed, `WAKE_LOCK` listed as not requested) each fail it, and the file was restored byte for byte. Before the Gradle input was declared those runs did not execute at all, which is how the gap was found. A test of the guard's own parser (a heading rename that empties both lists) fails the run too: it caught a bug in the first draft. **Not verified:** the policy's wording is true for a reader (a person should read it); the second half of the finding, removing `ACCESS_NETWORK_STATE`, was not tried (Open parts). |
| QA-02 | Fixed in code, closed by automated tests. `KillPointSimulator.kt` (which returned hardcoded `survives = true` without executing production code and had no production callers) is deleted. `PlaybackStateRestorationTest` is rewritten with real contract and boundary assertions directly exercising `PlaybackStateJournal` and `PlaybackJournalEntry`: 2-second pre-roll comfort window, near-boundary rules (<10s remaining treated as completed, <=3s restarting at zero), state restoration for internal storage with audio/subtitle/speed preservation, honest notices without silent zero restart when storage is inaccessible, completion boundary handling, and save throttling. | This PR | `PlaybackStateRestorationTest` (8 tests) and `HarnessContractsTest` (2 tests). The full unit suite passes locally. |
| PERF-05 | Fixed in code, not closed. Video decoding is now disabled during background audio playback outside Picture-in-Picture mode to prevent battery drain and CPU heating. In `MainActivity.onStop()`, if `backgroundAudioEnabled` is true, configurations are not changing, and the activity is not in PiP mode while the player is active, video track selection is disabled via `controller.setVideoEnabled(false)` (setting `C.TRACK_TYPE_VIDEO` to disabled in track selection parameters). When the activity returns to the foreground in `onStart()`, video track decoding is automatically re-enabled if it was disabled for background audio (`videoDisabledForBackground = true`). If `backgroundAudioEnabled` is false, `onStop()` continues to pause playback synchronously. | This PR | `BackgroundAudioPlaybackTest` (6 unit tests). Asserts `setVideoEnabled` toggles `disabledTrackTypes` containing `C.TRACK_TYPE_VIDEO`, asserts `handleBackgroundAudioOnStop` returns true to disable video decoding during background audio outside PiP, asserts player pauses when background audio is disabled, asserts video stays enabled when in PiP mode or when configurations are changing, and asserts video is not re-disabled if already in audio-only mode. All 6 tests pass. **Not verified:** battery drain measurement on a physical device. |
| UX-04 | Fixed in code, not closed. Error mapping now clearly distinguishes truncated or corrupted stream EOFs from storage unmounts or missing permissions, and external videos never display raw numeric MediaStore IDs as titles. In `ErrorTaxonomy`, `ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE` is mapped to `CORRUPT_MEDIA` with title "Truncated or Incomplete File" and actionable guidance ("Check source file for damage", "Re-copy or re-download the complete file"), preventing misleading "Storage Inaccessible" banners on truncated media. In `MediaRepositoryImpl` and `PlayerViewModel`, URI title resolution extracts actual display names via `ContentResolver` / `OpenableColumns.DISPLAY_NAME` when available, and falls back to "External Video" rather than raw numeric IDs (e.g. `1000000475`). | This PR | `ErrorTaxonomyTest` (4 tests), `MediaRepositoryTest` (2 tests), and `PlayerViewModelTest` (1 test). All pass. **Not verified:** truncated files on physical device with various OEM MediaStore implementations. |
| DATA-11 | Fixed in code, not closed. Subtitle charset normalization now handles CJK (GB18030, Shift-JIS), Cyrillic (KOI8-R, Windows-1251), Arabic (Windows-1256), and UTF-16/UTF-32 without BOM alongside UTF-8 and Windows-1252. In `SubtitleCharsetNormalizer`, detection checks 4-byte UTF-32 BOMs, 3-byte UTF-8 BOM, 2-byte UTF-16 BOMs, sample-based alternating null distribution for non-BOM UTF-32 and UTF-16, valid UTF-8, Japanese Hiragana/Katakana byte sequence scoring (Shift-JIS), single-byte Cyrillic word run heuristics (KOI8-R/CP1251), and Hanzi pair distribution (GB18030/GBK). Also adds explicit `overrideCharset` support across file and byte normalizers and exposes `COMMON_SUBTITLE_CHARSETS`. | This PR | `SubtitleCharsetNormalizerTest` (13 tests), including direct verification across all 10 real encoding fixtures in `fixtures/adv_sub/encodings/` (`gb18030`, `shift_jis`, `koi8_r`, `utf16_be`, `utf16_le`, `utf32_be`, `utf32_le`, `utf8`, `utf8_bom`, `windows_1252`) and explicit charset overrides (e.g. `windows-1256`). All 13 tests pass. **Not verified:** physical device testing with complex mixed-encoding subtitle packs. |
| UX-02 | Fixed in code, not closed. Opening a video from another app no longer leaves a permanent `0 B, External, Missing` card in the library. External single-open history rows (`folderPath == MediaRepositoryImpl.EXTERNAL_FOLDER_PATH`) are isolated from `getAllVideos()`, `getAllFolders()`, `searchVideos()`, `getMissingVideos()`, and `getLibraryStatistics()` while remaining retrievable via `getVideoByUri()` so reopening the same external file restores its saved position and playback preferences. In `FileReconciliationEngine.reconcile()`, unmatched external single-open rows are excluded from Pass 3's missing-file marking (and any previously marked missing external rows are healed on the next scan), while an external row whose URI is later discovered by a library scan in Pass 1 promotes cleanly into its scanned folder with progress intact. | This PR | `MediaRepositoryTest` (1 new test) and `FileReconciliationEngineTest` (3 new tests). All 20 tests across both classes pass. **Not verified:** external app launch and rescan on a physical phone. |
| PERF-04 | Fixed in code, not closed. Thumbnail extraction and disk caching are now bounded across concurrency, time, and storage footprint. `ThumbnailRepository` uses `Dispatchers.IO.limitedParallelism(3)` (`defaultExtractionDispatcher`) instead of unbounded 64-thread `Dispatchers.IO`, wraps extraction in `withTimeoutOrNull(DEFAULT_EXTRACTION_TIMEOUT_MS)` (5 s default) and records timed-out or corrupt URIs in `failedExtractionKeys`, and enforces a 50 MB LRU disk cache cap via `trimDiskCacheToLimit` (touching `lastModified` on cache hits and evicting the oldest thumbnails when over the cap while preserving the newly written file). | This PR | `ThumbnailRepositoryTest` (10 tests, including 3 new tests for LRU disk eviction, extraction timeout + negative caching, and 3-worker concurrency bound). All 10 pass. **Not verified:** physical device scroll and disk eviction with a 10,000-video library. |
| I18N-03 | Fixed in code, not closed. Hard-coded `Locale.US` and `Locale.ENGLISH` formatters in `MediaFormatters` (`formatFileSize`, `formatExactBytes`, `formatTimestamp`), `MediaLanguageLabels.displayName`, and `SidecarSubtitleDetector` now default to `Locale.getDefault()` and accept an explicit `Locale` parameter. `formatFileSize` strips trailing zero decimals using the active locale's `DecimalFormatSymbols.decimalSeparator`, `formatTimestamp` uses `DateFormat.getDateInstance(DateFormat.MEDIUM, locale)` and `DateFormat.getTimeInstance(DateFormat.SHORT, locale)`, and `MediaLanguageLabels.displayName` resolves ISO 639-1/2 and BCP-47 language names in the user's display locale while preserving undetermined/unknown code guards. | This PR | `MediaLibraryViewModelTest` (`mediaFormatters_respectUserLocaleForFileSizeExactBytesAndTimestamp`) and `MediaLanguageLabelsTest` (`displayName_respectsDisplayLocale`). All tests pass. **Not verified:** non-English system locale pass on a physical phone. |
| UX-03 | Fixed in code, not closed. Inverted confirmations are replaced with modern non-blocking Snackbar undo actions across all three flows. (1) Clearing the queue in `PlaybackQueueSheet` immediately empties remaining videos and displays an animated undo banner ("Cleared N videos from queue" with "Undo" button), which restores the cleared items before and after current in both Player and Queue via `restoreClearedQueue`. (2) Hiding videos from the library (single video menu or multi-selection mode) now hides the items immediately without a blocking confirmation dialog, showing a bottom floating `LibraryUndoSnackbar` ("'Title' hidden from library" or "N videos hidden" with "Undo" action) that unhides them via `unhideVideo()`. (3) Resetting progress in `MediaDetailsSheet` or library actions immediately resets progress while tracking the previous state and displaying an undo snackbar / button restoring position and completed status. | This PR | `PlaybackQueueUiTest` (`playbackQueueSheet_clearQueueShowsUndo_andUndoRestoresClearedQueue`), `PlayerViewModelTest` (`clearQueue_andUndoClearQueue_restoresItemsBeforeAndAfterCurrent`), `MediaLibraryViewModelTest` (`hideAndUnhideVideo_andResetAndUndoProgress_restoreSavedState`), and `MediaLibraryScreenUiTest` (`singleVideoHide_immediatelyHidesWithoutDialog_andShowsSnackbarUndo`, `resetProgress_inMediaDetailsContent_showsUndo_andRestoresPreviousProgress`, `longPressEntersMultiSelect_andHideActionIsNonDestructive`). All tests pass. **Not verified:** physical device tactile testing of undo snackbar duration and gestures. |
| UX-06 | Fixed in code, not closed. All 32 dp, 36 dp, 40 dp, and 44 dp touch and visual targets across queue, library, and details are raised to 48 dp per WCAG 2.5.5 Level AAA and Android accessibility guidelines. (1) In `PlaybackQueueSheet`, Move Up, Move Down, and Remove `IconButton`s are raised from 36 dp to 48 dp, and the queue row is restructured into a primary playable container (`weight(1f).clickable`) and secondary reorder/remove buttons, eliminating clickable nesting and TalkBack tap collision. (2) In `MediaDetailsSheet`, `QuickAction` icon circular target is raised from 44 dp to 48 dp. (3) In `MediaLibraryScreen`, clipboard "Move to folder" and undo snackbar dismiss buttons are raised from 36 dp to 48 dp. (4) In `FolderDestinationSheet`, destination folder icon target is raised from 44 dp to 48 dp. (5) In `SubtitleSheet`, remove external subtitle button is raised from 40 dp to 48 dp. (6) In `WebSearchDialog`, dismiss button is raised from 32 dp to 48 dp. (7) In `PlayerSupportComposables`, audio-only "Show video" button is updated to `heightIn(min = 48.dp)`. | This PR | `PlaybackQueueUiTest` (`playbackQueueSheet_actionButtons_meetAccessibleTouchTargetMinimum`, `playbackQueueSheet_rendersItems_andJumpWorks`), `MediaLibraryScreenUiTest` (`mediaDetailsContent_quickActions_meetAccessibleTouchTargetMinimum`, `singleVideoHide_immediatelyHidesWithoutDialog_andShowsSnackbarUndo`). All pass. **Not verified:** physical device tactile testing across diverse screen sizes. |
| UX-07 | Fixed in code, not closed. Copy and content descriptions across library, settings, queue, and player HUD are harmonized to consistent terminology and sentence case per Android accessibility and Material Design standards. (1) Library maintenance copy is standardized on "Rescan library" / "Rescanning…" across `MediaLibraryScreen` (was "Scan device"), `LibraryVideoPane` (was "Refresh library"), `LibrarySidebar` (was "Refresh"), and `SettingsScreen`. (2) `SettingsScreen` high-level metrics label is changed from ambiguous "Footprint" to "Storage" ("Videos", "Runtime", "Storage"), matching "Size" in `MediaDetailsSheet`. (3) Player HUD and sheet accessibility labels are standardized from mixed Title Case to sentence case: "Lock controls" (was "Lock Controls"), "Playback queue" (was "Playback Queue"), "Previous video", "Next video", "Audio track", "Advanced settings", "Screen rotation", "Picture in picture", "Aspect ratio", "Now playing", "Move up", "Move down", "Remove from queue", "Close details", "Copy path", "Open file", "Copy stats", and "Close stats". (4) Orientation lock toasts unified to sentence case ("Landscape locked", "Portrait locked", "Auto-rotate"). | This PR | `PlaybackQueueUiTest`, `LibrarySidebarTest`, `LibraryTouchTargetsTest`, `MediaLibraryScreenUiTest`, `PlayerAccessibilityTest`, `AudioOnlyLayoutTest`, and `PlayerRtlLayoutTest`. All pass. **Not verified:** physical device screen reader pronunciation across non-English locales. |
| SEC-08 | Fixed in code, not closed. Android 12+ (API 31+) device-to-device (D2D) migration and cloud backup are explicitly disabled by adding `data_extraction_rules.xml` (`<exclude path="." />` under `<cloud-backup>` and `<device-transfer>`) and `backup_rules.xml` (`<exclude domain="..." path="." />` across root, file, database, sharedpref, and external domains), linked via `android:dataExtractionRules` and `android:fullBackupContent` on `<application>`. Enforces complete offline privacy and satisfies Android Lint `DataExtractionRules`. | This PR | `PlatformInvariantsTest.manifest_configuresDataExtractionAndBackupExclusionRules`. All pass. **Not verified:** physical D2D migration between two physical Android devices. |
| SEC-12 | Fixed in code, not closed. Media3 and platform framework logging is stripped in release builds by adding `-assumenosideeffects class android.util.Log { ... }` to `app/proguard-rules.pro`, removing log call overhead, dead debug string allocations, and preventing framework internals leakage in production binaries. | This PR | `PlatformInvariantsTest.proguardRules_stripAndroidUtilLoggingInRelease` and `verifyReleaseApk`. All pass. **Not verified:** live logcat capture of release APK on physical device. |
| UX-08 | Fixed in code, not closed. Screen orientation toggle is hidden on large screens and tablets (`smallestScreenWidthDp >= 600`) in both the wide inline top bar and the compact overflow menu. Avoids offering a non-functional or disruptive orientation lock on devices where Android 14+/16 per-app orientation overrides ignore or letterbox requested orientations and where tablets sit on kickstands/docks. Controlled via `DeviceCapabilities.supportsOrientationLock(smallestScreenWidthDp)`. | This PR | `PlayerWideTopBarTest` (3 tests), `PlayerOrientationAvailabilityTest` (2 tests), and `DeviceCapabilitiesTest` (5 tests). All pass. **Not verified:** physical Android 16 tablet hardware testing. |
| UX-05 | Fixed in code, not closed. Player HUD content description for the tools sheet button is aligned from "Advanced settings" to "Playback tools", matching the bottom sheet title and TalkBack announcement. | This PR | `PlayerAccessibilityTest` (`accessibilityTouchTargets_meetMinimumSize`) asserts "Playback tools" touch target. All tests pass. **Not verified:** physical TalkBack announcement on device. |
| SEC-09 | Fixed in code, not closed. Enforced strictly offline DataSource factory in `PlayerManager`. `DefaultDataSource.Factory` is instantiated with an offline base factory that throws `IOException("Network playback is forbidden. Aptura is strictly offline-only (DEC-012).")` whenever a network scheme (`http://` or `https://`) is opened, preventing instantiation of `DefaultHttpDataSource` or remote network socket allocation. | This PR | `OfflineDataSourceTest` (`httpUris_areBlockedWithOfflineException`, `httpsUris_areBlockedWithOfflineException`). All pass. **Not verified:** physical device network intent attempt. |
| A11Y-07 | Fixed in code, not closed. Added automated 200% accessibility font scale gallery test pages in `AppGalleryTest` and `LibraryGalleryTest` covering Player HUD (`player_hud_font200`), Speed sheet (`sheet_speed_font200`), Audio sheet (`sheet_audio_font200`), Subtitle sheet (`sheet_subtitle_font200`), Playback tools sheet (`sheet_advanced_font200`), Queue sheet (`sheet_queue_font200`), and Media Details sheet (`details_sheet_font200`), verifying no text truncation or overlapping composables at maximum system font scale. | This PR | `AppGalleryTest` (6 200% font scale tests) and `LibraryGalleryTest` (`detailsSheetFont200`). Rendered screenshots captured in `VISUAL_GALLERY_REVIEW_A11Y07.md`. **Not verified:** physical device font scaling pass. |
| A11Y-06 | Fixed in code, not closed. Brightness could only be changed by swiping up and down on the left half of the picture, which leaves out anyone who cannot make that gesture or who uses a screen reader or a switch (WCAG 2.5.1 asks for a single-pointer alternative to a path gesture). There are now two. The tools sheet's Picture tab has a Brightness slider that a single tap sets, and that TalkBack reads as a slider. The video area offers "Increase brightness" and "Decrease brightness" actions, a tenth of the range each, and shows the gauge when one is used. The swipe, the slider and the actions change one level, kept as state in `PlayerScreen`, so the slider shows what a swipe or an action left. `WindowBrightness.setTo` sets an absolute level and `adjustBy` goes through it; the level stays between 1% and 100% and is put back when the player is left, as before. Lock mode, which turns the swipe off, turns the actions off too. | This PR | New `PlayerBrightnessControlTest` (9 tests). Five of the first six failed on the old code (no slider, no actions, no way to set a level). Six deliberate breaks (the level may reach zero, an action does not update the shared level, the slider is not wired to the window, the actions stay on in lock mode, the swipe does not update the shared level, an action does not show the gauge) each turned the matching tests red, and both files were restored byte for byte. The Picture tab was rendered in the gallery and looked at. **Not verified:** TalkBack or Switch Access on a phone (Open parts). |
| DATA-09 | Fixed in code, not closed. Added "Clear watch history" action under Storage & Cache in Settings with an explicit, high-friction destructive confirmation dialog. Resets playback position (`playbackPositionMs = 0`), completed flag (`isCompleted = 0`), and last played timestamp across all indexed media items in Room (`clearAllPlaybackHistory()`), clearing Continue Watching while strictly preserving user bookmark timestamps, custom tags, folder trees, and physical video files. | This PR | `ApturaDatabaseRealSqlTest` (`clearAllPlaybackHistory_resetsPlaybackPositionAndCompletedAcrossAllVideos`), `MediaRepositoryTest`, `MediaLibraryViewModelTest`, and `SettingsSecurityAndHistoryTest` (`watchHistoryRow_showsDialog_andConfirmCallsCallback`). All tests pass. **Not verified:** physical device watch history wipe with large media library (Open parts). |
| SEC-11 | Fixed in code, not closed. Implemented two-fold screen security and tapjacking hardening. (1) Added "Screen security" toggle in Settings (under Privacy & Security) bound to `ApturaSettings.isPrivateMode`, dynamically setting `FLAG_SECURE` on `MainActivity.window` to block screenshots and mask thumbnail previews in Android Recent Apps / task switcher. (2) Hardened destructive confirmation dialogs (`ApturaDialog`) against tapjacking and overlay attacks via `SecureFlagPolicy.SecureOn` window policy and `filterTouchesWhenObscured()` modifier rejecting obscured/partially-obscured motion events (`FLAG_WINDOW_IS_OBSCURED or FLAG_WINDOW_IS_PARTIALLY_OBSCURED`). | This PR | `ApturaSettingsRepositoryTest` (`isPrivateMode`, `updatePrivateMode_persistsCorrectly`), `ApturaDialogSecurityTest` (3 tests verifying dialog confirmation, accent tone, and obscured touch modifier), and `SettingsSecurityAndHistoryTest` (`screenSecurityRow_showsStateAndCallsUpdatePrivateMode`). All tests pass. **Not verified:** physical device screenshot attempt and multi-window tapjacking test (Open parts). |
| A11Y-05 | Fixed in code, not closed. Integrated Android platform system caption accessibility preferences via `CaptioningManager`. When `subtitleUseSystemStyle` is enabled (default `true`), `PlayerVideoSurface` applies `CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)` and scales the font size by `captioningManager.fontScale`. Added "Use system caption style" switch in `SubtitleSheet` Style tab, bound to `ApturaSettings.subtitleUseSystemStyle`, while preserving custom color, size, and background controls when toggled off. | This PR | `ApturaSettingsRepositoryTest` (`subtitleUseSystemStyle`), `PlayerViewModelTest` (`subtitleAppearance_updatesUiStateProperly`), and `SubtitleSystemStyleTest` (2 UI tests). Visual gallery render captured in `tab_sub_style.png` (review in `VISUAL_GALLERY_REVIEW_A11Y05.md`). All tests pass. **Not verified:** physical device accessibility pass with custom OS caption styling (Open parts). |
| A11Y-08 | Fixed in code, not closed. System-wide audit and resolution of text truncation, clipping, and awkward line wrapping across all screens, bottom sheets, pickers, and dialogs. (1) In `SubtitleSheet`, trimmed wordy 74-character switch subtitle ("Follow size, colors and background set in Android accessibility settings") to concise "Follow Android accessibility settings", eliminating awkward 3-line stacking and text squeezing. (2) Changed single-line ellipsis caps (`maxLines = 1`) to graceful two-line wrapping (`maxLines = 2`) across sheet chrome headers (`PlayerSheetHeader`), button labels (`ApturaDesignKit.ButtonLabel`), section headers (`ApturaSectionHeader`), continue watching cards (`ContinueWatchingCard`), playback queue rows (`QueueItemRow`), folder destination choices (`FolderDestinationRow`), folder & video list rows (`FolderNavigation`), bookmark notes (`BookmarkRow`), upcoming video overlay (`UpNextOverlay`), and settings folder/codec rows. (3) Enabled responsive text scaling and soft wrapping in `SegmentedPicker` and `TonalSegmentedPicker` to prevent single-word labels (e.g. "Medium", "Comfortable", "Western", "Japanese") from truncating with ellipsis at 200% accessibility font scale. (4) Redesigned `AccentPickerRow` in Settings to gracefully break into two 3-item rows under large font scales (> 1.2f), preventing horizontal label collision (`CyanBlueViolet...`). (5) Added responsive font scaling to `ReadoutCell` to prevent storage size ("689 MB") from truncating at 200% scale. (6) Fixed `ApturaUserJourneyTest` assertion to match "View" sheet title. | This PR | Full gate passed (1354 unit tests, lint, release R8 invariants). 14/14 connected instrumented tests passed on live Pixel 8 API 35 emulator. Live screenshots captured at 200% font scale across Library, Player, Subtitle Sheet, and Settings verifying 0 clipped or truncated strings. **Not verified:** physical device testing across varied screen densities. |
| PROD-G12 | Fixed in code, not closed. Added user-visible subtitle character encoding picker (DATA-11) in `SubtitleSheet` Style tab. Users can select character set encoding for legacy non-UTF-8 subtitle files (Auto, UTF-8, Western ISO-8859-1/Windows-1252, Arabic Windows-1256, Chinese GB18030, Japanese Shift-JIS, Cyrillic Windows-1251). Preference is persisted in `ApturaSettingsRepository` (`SUBTITLE_ENCODING`), propagated reactively via `PlayerViewModel`, and rendered with accessible, wrap-safe segmented pickers. | This PR | `ApturaSettingsRepositoryTest` (`subtitleEncoding`), `PlayerViewModelTest` (`setSubtitleEncoding`), and `SubtitleSystemStyleTest` (`subtitleSheet_rendersEncodingPicker_andSelectingEncodingCallsCallback`). **Not verified:** physical device testing with corrupted or legacy encoded subtitle files (Open parts). |
| PERF-01 | Fixed in code, not closed. Eliminated 4 Hz `PlayerScreen` recompositions and gesture-detector restarts during playback. (1) Separated fast-changing playback position (`PlayerPositionState`: `currentPositionMs`, `bufferedPositionMs`) from `PlayerUiState` in `PlayerViewModel`, so 250 ms playback ticks no longer emit `uiState` updates even when controls or sheets are visible. (2) Passed a deferred `positionProvider: () -> PlayerPositionState` lambda from `Navigation.kt` to `PlayerScreen`, `PlayerSeekBar`, `PlayerElapsedTimeText`, and `PlaybackToolsTab`, reading live position only inside the leaf `PlayerElapsedTimeText` / `FrameStepPositionRow` / `AddBookmarkAtPositionButton` texts and inside `PlayerSeekBar`'s `Canvas` draw phase. (3) Extracted `PlayerSeekBar.kt` and replaced volatile `pointerInput` keys (`durationMs`, `isPlaying`, `seekDeltaSeconds`, `doubleTapSeekSide`, `seekStepMs`) with `pointerInput(Unit)` (or `isLocked`) and `rememberUpdatedState` in both `PlayerScreen` and `PlayerSeekBar`, preventing gesture detector restarts on double-tap seek or playback state changes. | This PR | `PlayerRecompositionTest.positionTicks_doNotRecomposePlayerScreenHost_whileUpdatingElapsedTimeText` (verifies 0 host recompositions across 10 position ticks while elapsed text updates) and `PlayerViewModelTest` (`controlsHidden_doesNotUpdateUiStatePositionOnPlaybackTicks`, `testHorizontalSwipeSeek_withHiddenControls_startsFromTheRealPosition`). **Not verified:** Macrobenchmark frame timing on a physical device. |
| PERF-03 | Fixed in code, not closed. Eliminated library-size scaling bottlenecks in Room, reconciliation, and SAF scanning. (1) Added `Index(value = ["uri"])` on `MediaItemEntity` (`index_media_items_uri`) with `APTURA_DB_VERSION = 7`, `MIGRATION_6_7`, and exported `7.json` schema. (2) Updated `FileReconciliationEngine.reconcile` to skip unchanged rows (`if (merged != existing) itemsToUpsert.add(merged)`), avoiding rewriting every row on every app start. (3) Replaced quadratic SAF dedupe in `MediaScanWorker` (`allScanned.any` and `found.none`) with $O(1)$ hash-set lookups (`deduplicateSafItems` using `seenUris` and `seenSizeAndName`). | This PR | `DatabaseScaleTest` (`migration6To7_createsUriIndex_andQueryPlannerUsesIt`, `twentyThousandRows_uriLookupUsesIndex_reconcileSkipsUnchanged_andSafDedupeIsLinear`) verifying `EXPLAIN QUERY PLAN` uses `index_media_items_uri`, 20,000-row unchanged rescan writes 0 rows, 5-row delta writes 5 rows, and 20,000 + 5,000 SAF deduplication is linear. `ApturaDatabaseMigrationTest` passes. **Not verified:** physical device scan with 20,000 real media files. |
| PERF-02 | Fixed in code, not closed. Configured Baseline Profile for cold startup and library scroll performance per spec/13 §3. (1) Added `app/src/main/baseline-prof.txt` with curated AOT compilation rules covering cold startup (`MainActivity`, `NavigationKt`, `ApturaTheme`, `ApturaTypography`), library screen & video card rendering (`MediaLibraryScreenKt`, `MediaLibraryViewModel`, `VideoCardKt`), player initialization & timeline scrubbing (`PlayerScreenKt`, `PlayerSeekBarKt`, `PlayerViewModel`), and database/repository paths (`ApturaDatabase`, `MediaItemDao`, `MediaRepositoryImpl`), compiled directly into the release APK via `assembleRelease` (`assets/dexopt/baseline.prof`). (2) Added `androidx.profileinstaller:profileinstaller` runtime installer. (3) Kept PR CI smoke gate (`connectedDebugAndroidTest`) fast and clean by reserving macrobenchmark timing harnesses for physical device matrices per spec/13 Principle M1. | This PR | Verified compiled `assets/dexopt/baseline.prof` and `baseline.profm` in release APKs; `compileDebugAndroidTestKotlin`, `testDebugUnitTest`, `verifyReleaseApk`, and release invariants pass with 0 regressions. **Not verified:** Benchmark execution on physical bench device matrix (Open parts). |
| A11Y-02 | Fixed in code, not closed. Added player hardware keyboard, D-pad, and Chromebook key event handling in `PlayerKeyHandler.kt` wired into `PlayerScreen`. Supports Space/Enter/DpadCenter/K for play/pause, Left/Right/J/L for relative seeking, Up/Down for volume control, M/VolumeMute for mute toggle, F for aspect ratio toggle, C for subtitle toggle, N/P for next/previous skip, 0..9 for percentage seeking, and Escape/Back to dismiss sheets or navigate back. Lock mode triggers lock hint, and alphanumeric keys are safely disabled during active sheet display to prevent fighting text input fields. | This PR | `PlayerKeyHandlerTest` (12 unit tests verifying every key mapping, lock mode, sheet isolation, and key release events). Full gate passed. **Not verified:** physical hardware keyboard or Chromebook pass (Open parts). |
| DATA-13 | Fixed in code, not closed. Move and consent edge cases across Android versions are addressed. (1) In `MediaDeleteFlow` and `MediaMoveFlow`, replaced `remember` with `rememberSaveable` for queues and awaiting-consent states (`deleteQueue`, `awaitingConsentUri`, `awaitingBatchConsent`, `deletedUris`, `failedUris`, `moveQueue`, `currentDestination`, `movedUris`), preserving state across activity recreations while the system permission/consent dialog is shown. (2) In `MediaMover`, added `sdkInt < Build.VERSION_CODES.Q` check in `canMoveAllInPlace` and `moveInPlace` (returning `cannotModify()`), properly reflecting that `RELATIVE_PATH` does not exist on Android 8 & 9 (API 26-28) and directing in-place attempts to the SAF copy/move fallback. (3) On Android 10 (`Build.VERSION_CODES.Q`), MediaStore rejects video insertion/updates into public `Download/` directories (`IllegalArgumentException`); `MediaMover` rejects in-place moves to `Download/` on API 29 and `FolderDestinationSheet` filters out Download folders on API <= 29, and `MediaLibraryScreen` routes moves directly to the SAF picker on API < 29. | This PR | `MediaMoverTest` (5 new tests asserting API < 29 rejection, API 29 Download rejection, API 30+ Download allowance, and `isDownloadFolder` resolution), `MediaDeleteFlowTest` and `MediaMoveFlowTest`. Full unit test suite passes. **Not verified:** physical device tests on Android 8, 9, and 10 devices. |
| ISSUE-005 | Fixed in code, not closed. `StatsOverlay` now adapts cleanly to 200% font scale without awkward wrapping or horizontal collisions. (1) Header title `Row` uses `Modifier.weight(1f)` and `maxLines = 2`, keeping Copy and Close `48.dp` action buttons inside panel bounds. (2) Path & acceleration chip + HDR badge row uses `FlowRow` with wrap-safe spacing at `fontScale >= 1.4f`. (3) `StatsRow` stacks label above value in a full-width `Column` when `fontScale >= 1.4f` (avoiding crushed horizontal text wrapping), and uses `Modifier.weight(1f)` + `TextAlign.End` with `12.dp` spacing at standard font scales. | This PR | `StatsOverlayUiTest` (`statsOverlay_at200PercentFontScale_stacksRowsAndKeepsButtonsInsideBounds` and `statsOverlay_atStandardFontScale_placesLabelsLeftOfValuesWithoutCollision`) and `StatsOverlayLayoutTest`. **Not verified:** visual inspection on a physical device at 200% system font size. |
| QA-06 | Done in code and tests. Added `androidx.room:room-testing` (`testImplementation`), configured `debug` and `androidTest` schema assets directories in `app/build.gradle.kts` (so release builds remain clean while Robolectric `MigrationTestHelper` validates schemas on every run), and added `ApturaDatabaseSchemaValidationTest` validating committed schemas `5.json`, `6.json`, and `7.json` across `MIGRATION_5_6` (`decoderOverridden` backfill), `MIGRATION_6_7` (`index_media_items_uri`), chained `5 -> latest`, every `5..APTURA_DB_VERSION` schema upgrade via `MigrationTestHelper.runMigrationsAndValidate`, plus pre-schema-export `2 -> latest` SQL upgrade execution. `ApturaDatabaseMigrationTest` also asserts every schema version from 5 to `APTURA_DB_VERSION` is exported and committed. | This PR | `ApturaDatabaseSchemaValidationTest` (5 tests), `ApturaDatabaseMigrationTest` (2 tests), `ApturaDatabaseRealSqlTest`, and `DatabaseScaleTest`. |
| DEV-01 | Done. Removed the `AarMetadata` check bypass from `app/build.gradle.kts` so `checkDebugAarMetadata` and `checkReleaseAarMetadata` run on every build. Updated `compileSdk` to `release(37) { minorApiLevel = 0 }` (targeting Android SDK Platform `37.0`), bumped `androidGradlePlugin` to `9.1.0` in `gradle/libs.versions.toml` (required by `androidx.compose.material3.adaptive:1.3.0`), and updated Gradle wrapper to `9.3.1` with verified SHA-256 checksum. | This PR | `checkDebugAarMetadata`, `checkReleaseAarMetadata`, and full `.\tools\gate.ps1` (unit tests, lint, release build + invariants) pass with AAR metadata checks enabled. |
| ISSUE-016 | Done. Removed unused `deleteRemovedVideos(existingMediaStoreIds: List<Long>)` query from `MediaItemDao` and its fake implementation in `MediaRepositoryTest`. | This PR | `.\tools\gate.ps1` (full unit test suite, lint, release build + invariants). |
| PROD-02 | Refreshed in repo, awaits owner visual sign-off. Replaced all 6 `distribution/play-store/phone-screenshots/` (`1080x2400`) and both `distribution/play-store/tablet-screenshots/` (`2560x1600`) plus updated `docs/assets/screenshots/` renders using reproducible demo-content Compose captures (`PlayStoreGalleryTest`, `SettingsGalleryTest`, `LibraryGalleryTest`, `PlayerGalleryTest`). Removed obsolete `05_supporter_gold_unlocked.png` (pre-PROD-01 locked Gold toggle + toast) in favor of `05_subtitles_sheet.png`; eliminated test color bars, debug status-bar icons, old `"Footprint"` copy, and old Gold lock icons across phone and tablet store assets. `feature-graphic-1024x500.png` had already been clean of mojibake. | This PR | `PlayStoreGalleryTest` + gallery suite renders visually inspected across all `1080x2400` phone and `2560x1600` tablet PNGs; `.\tools\gate.ps1`. |
| QA-05 | Done in Gradle build and app runtime; CI workflow upload deferred (`.github/` untouched). Added Kover `0.9.11` (`koverXmlReportDebug`, `koverVerifyDebug` enforcing a 70% minimum line coverage floor with Compose/Room generated code excluded; baseline measured 75.6% lines / 80.6% instructions), Detekt `1.23.8` (`config/detekt/detekt.yml`, wired automatically into `lintDebug` so PR `lint` CI enforces it cleanly), debug-only `LeakCanary` `2.14` (`debugImplementation`, completely excluded from release invariants), and debug-only `ApturaStrictMode` wired in `ApturaApplication` (guards out release builds and Robolectric JVM runs). | This PR | `.\gradlew.bat detekt` passes with 0 issues; `.\gradlew.bat koverXmlReportDebug koverVerifyDebug` passes (baseline 75.6% lines covered); `ApturaStrictModeTest` (3 tests); `.\tools\gate.ps1` (unit tests + release dependency/manifest/APK invariants). |
| QA-07 | Committed corpus exercised; nightly generated video workflow deferred (`.github/` untouched). Added `CommittedCorpusFixturesTest` exercising all 42 committed files under `fixtures/` (`manifest.csv` schema & SHA-256 verification, all 10 `encodings/`, 2 `bait/`, 8 international `scripts/` including RTL/CJK/Devanagari/Thai, 13 ASS E/F/X fidelity probes, 6 adversarial SRT edge cases, `a_A12_01_video.srt`, and `inst_seek_pinned_positions.json`) through `SubtitleFileValidator`, `SubtitleCharsetNormalizer`, and Media3 `SubripParser`/`SsaParser`. Fixed a bug discovered by the corpus where `SubtitleFileValidator.isBinaryOrImage` rejected BOM-less UTF-16 and UTF-32 files as binary because it checked only BOMs before scanning for null bytes (`SubtitleCharsetNormalizer.isUtf16OrUtf32` now shares the heuristic). | #156 (this PR) | `CommittedCorpusFixturesTest` (7 tests), `SubtitleFileValidatorTest` (11 tests), `SubtitleCharsetNormalizerTest` (10 tests), and `.\tools\gate.ps1` (full unit test suite, Detekt + Android Lint, release APK invariants). |

## Executive summary

**Aptura is not ready for a public Play launch, but it is close and well worth finishing.** The engineering base and privacy enforcement are unusually strong. The problems are a small set of real playback and data bugs, overstated release evidence, and no localization or non-touch input.

This audit covers `main` at commit `b4de60b`. It logged 1 Critical, 22 High, 43 Medium and 16 Low findings, plus 12 ranked product gaps. Every baseline check passed, and those checks caught none of the bugs below, so a green gate does not mean the app works.

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

#### LEG-02: Replace the prebuilt decoder with a self-built, free-only one

**Legal and engineering · Planned, not started · Effort L · Severity High · Priority: launch blocker (Phase 2, gates Phase 3)**

This is the plan that follows from LEG-01 and DEC-014. The owner's direction, 2026-11-06: use truly free decoders rather than risk licence trouble. Nothing is paid for, and no decoder is removed until its replacement is tested.

- **Why:** the shipped library cannot be trimmed, is labelled GPL-3.0 by its publisher, links FFmpeg statically, and carries decoders outside ADR-010's list (see LEG-01). A store release should wait for a clean replacement.
- **The layers, in order of use:**

| Layer | What it covers | Licence cost to Aptura |
| --- | --- | --- |
| 1. Platform decoders (MediaCodec), used first | What the phone ships: usually H.264, AAC, MP3, FLAC, Opus, Vorbis, and often HEVC. Which Dolby and AV1 decoders exist varies by device. | None. The device maker holds the licences. |
| 2. Bundled free fallback (self-built) | FLAC, ALAC, MP3, Opus, Vorbis, PCM, A-law, mu-law. | None found. MP3 patents have expired; FLAC, Opus and Vorbis are open formats. |
| 3. Candidates, only after counsel confirms | AC-3 and E-AC-3. | Patents are reported expired (spec 07 says AC-3 in 2017 and E-AC-3 on 2026-01-30; the E-AC-3 date is unverified). Dolby historically asked app developers for a licence anyway. |
| 4. Pass-through, not decoded by Aptura | DTS, DTS-HD, TrueHD, MLP: sent to a TV or receiver that decodes them. | None for Aptura. Media3 does this when the output supports it; not tested here. |
| 5. Honest failure | Anything else: say the audio cannot be decoded on this device and offer "open in another app". | None. |

- **Left out of the bundle on purpose:** AAC (platform decoders handle it, and Via LA's pool charges a $15,000 initial fee, or $1,000 for a small entity, plus per-unit fees; how a free download counts is unanswered), DTS, DTS-HD, TrueHD and MLP (no application-side licence found). Dolby and DTS publish no prices; they negotiate.
- **Trade-off:** on phones with no built-in decoder for a codec, some files lose their sound. Evidence: the Pixel 8 emulator image listed no AC-3, E-AC-3 or DTS decoder, and my DTS test video reached the playing state, most likely decoding its audio through the bundled library (I did not check that sound came out). How many real devices are affected is not measured.
- **Steps, in order:**
  1. **Owner, now, because it has lead time:** a short review by an IP lawyer of briefs L1 and L2 in `spec/11A_LEGAL_BRIEFS.md`, which are written and marked sendable. Do not pay any licence before that.
  2. Build the decoder on a Linux CI runner from AndroidX Media's `decoder_ffmpeg` module (Apache-2.0) and FFmpeg configured LGPL-only, as shared libraries, with an explicit decoder list. `tools/ffmpeg/build_ffmpeg_audio_lgpl.sh` and `verify_lgpl_symbols.py` already describe this. This machine has no Android NDK, so a local build is not the way.
  3. Replace the Jellyfin dependency (`gradle/libs.versions.toml` and `app/build.gradle.kts`, both hot files, so one PR at a time).
  4. Run `verify_lgpl_symbols.py` and a decoder-name dump in CI, so the library cannot grow a decoder unnoticed.
  5. Update ADR-010, spec 07, `FFMPEG_NOTE`, `NOTICE` and the store text to match, and close DEC-014.
- **Also helps:** DEP-01 (the shipped FFmpeg is 6.0; a self-build can use a current release) and the F-Droid question in PROD-G2.
- **Risks:** the build and its upkeep become ours; the decoder module is tied to the Media3 version; a few files lose sound on some phones until layer 3 is cleared; the CI build needs time to get right.
- **Validate:** the decoder dump shows only the approved names; `verify_lgpl_symbols.py` passes in CI; `PlaybackSmokeTest` and the 13-file format matrix from this audit give the same video results before and after, with the audio differences listed; DEC-014 is closed with a written decision.
- **Sources (opened 2026-11-05):** [Via LA AAC programme and FAQ](https://www.via-la.com/licensing-programs/aac/), [Dolby Digital licensing note](https://en.wikipedia.org/wiki/Dolby_Digital), [Opus licence](https://opus-codec.org/license/), [Dolby consumer licensing](https://professional.dolby.com/licensing/support-consumer-licensing-dev-kits/). The DTS terms were not found online. This is engineering planning, not legal advice.

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
   - **Launch blocker, start the legal step now:** LEG-02 (self-built, free-only decoder; see its card). It gates Phase 3, so it runs alongside Phase 1.
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
- [x] How should the bundled decoders be handled legally? (LEG-01). Decided 2026-11-06 on the owner's direction to use truly free decoders: DEC-014, with the plan in LEG-02.
- [ ] Get a short IP-lawyer review of briefs L1 and L2 in `spec/11A_LEGAL_BRIEFS.md` before adding E-AC-3 or paying for any licence (LEG-02, step 1).
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
| ISSUE-016 | Unused `deleteRemovedVideos` query in `MediaItemDao` | Fixed: query and dead fake removed |
| ISSUE-025, 10-28 F7 | Bluetooth hint means connected, not in use | Needs a real device |
| 10-28 F8 | Audio delay processor handles 16-bit PCM only | The playback review found the sink converts to 16-bit, so this looks harmless; confirm on a device |
| 10-29 limit 3 | On a 740 x 360 dp window the stats panel shows its first rows and scrolls | Accepted limit |
| 10-29 limit 5 | Landscape guide is one scrolling column | Accepted limit |
| 10-29 limit 8 | Samsung S24 playback report: cause not established, and the 8 s watchdog is a guess | Open. STAB-01 may be related, unproven |
| 10-29 limit 9 | The access effect in `Navigation.kt` has no test of its own | Open, low priority |
| 10-28 item 4 | Gallery: sheets do not scroll under Robolectric, and picture adjustments cannot render | Accepted limit |

Folded into existing findings: ISSUE-008, ISSUE-009 and ISSUE-019 (architecture seams), and the device-pass items (SHIP-02). ISSUE-003, a paused video resuming after a configuration change, did not reproduce here: it stayed paused through dark-mode and font-scale changes on the emulator.
