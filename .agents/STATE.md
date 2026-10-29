# Aptura - Current State

> The current truth about the project, kept short so every agent can load it. **Correct lines in place** when your
> change makes them false; do not append contradicting notes. History lives in [`MEMORY.md`](MEMORY.md); decisions in
> [`spec/17`](../spec/17_RISKS_ASSUMPTIONS_AND_DECISIONS.md) (DEC) and [`spec/08`](../spec/08_TECHNOLOGY_EVALUATION_AND_ADRS.md) (ADR).

Only facts that stay true between pull requests belong here. Test counts and the last verified commit live in each
PR's Verification section and in CI, not here, so most pull requests never need to touch this hot file.

## Project

- **Product:** Aptura, an offline-first Android video player. Package `org.aptura.player`. Public repo
  `TejuBoricha/Aptura` (personal account), Apache-2.0.
- **Phase:** 1.0 pre-release polish. The 1.0 scope is feature-complete; hardware verification is the remaining gate.
- **Team:** one owner (Tejas) and several AI agents working in parallel: Claude Code (cloud), OpenCode (various models,
  including DeepSeek), Antigravity. Coordination rules: [`AGENTS.md`](../AGENTS.md),
  [`docs/AGENT_WORKFLOW.md`](../docs/AGENT_WORKFLOW.md). This replaces the "solo AI developer" model in OD-4.
- **Owner's machine:** Windows, PowerShell. Use `;` not `&&` in commands meant for it. Python is `py -3` there.
- **Coordination status page:** the open issue labelled
  [`agent-status`](https://github.com/TejuBoricha/Aptura/issues?q=is%3Aissue+is%3Aopen+label%3Aagent-status), kept
  current by the Agent status workflow whenever the owner's account comments or opens, labels or closes an issue,
  and on a 15-minute schedule that GitHub runs best effort. A run that sees a plain `To:` deadline within 35 minutes
  waits for it, so a quiet stretch is still announced; a longer `Working:` note is seen by the next run after it
  expires. It opens with what needs the owner.

## Quality gate

- `tools/gate.ps1` and CI (`.github/workflows/ci.yml`: `verify` = agent coordination checks, unit tests, lint, R8
  release + `verifyReleaseApk`; `connected` = instrumented tests on an API 35 tablet emulator).
- The gate also runs the checker's Python tests (`python -m unittest discover -s tools/agents`) when Python 3 is
  installed; CI always runs them.
- `gradle/actions/setup-gradle` is pinned to **v5 on purpose** (v6 pulls a proprietary build-cache component).

## Open work (highest first)

1. **Hardware verification:** physical-device playback, TalkBack, Bluetooth latency and detection, HDR, thermal,
   process-kill during a SAF copy (and a running sleep timer, FR-CTRL-009). Run
   [DEVICE_CHECKLIST.md](../docs/DEVICE_CHECKLIST.md) before tagging 1.0.
2. **Release:** signed 1.0 tag when a signing key exists.
3. **Undecided:** one audio sync value for every video needs a storage decision first (per-video storage cannot tell 0
   from "never set").

## Facts that older notes get wrong

- **Sleep timer:** one monotonic deadline (`sleepTimerEndsAtElapsedRealtime`, injected clock) is authoritative; the UI
  derives the countdown only while it is visible and never polls the ViewModel. Expiry pauses and offers `+5 min`,
  which resumes playback with a fresh five minutes.
- **Audio sync sign:** a positive offset **delays** the sound. Bluetooth audio is already late, so its fix is
  negative ("Sound earlier"). The old "+150 ms / +250 ms Bluetooth presets" were wrong and were removed in `95231ca`.
  The player offers "Try −200 ms" only when Bluetooth output is connected and sync is at 0.
- **Player sheets** are built from the shared controls in `ui/player/PlayerControls.kt` and
  `PlayerSheetChrome.kt` (DESIGN.md §5.6), not from per-sheet pills and steppers.
- **Theme and navigation:** the palette is a `CompositionLocal` (`LocalApturaPalette`), never a global; the always-dark
  player wraps itself in `ApturaTheme(DARK)`. The player route is never animated (its video is a `SurfaceView`): a cover
  above the picture opens and closes it (`NavigationTransitions.kt`, DESIGN.md §4.3). A global palette flipped a frame
  late was the cause of the back-navigation flicker; do not bring one back.
- **Library view choices** (layout, size, details, sort, folder browser layout) live in DataStore, not Room. The welcome
  guide's pictures are built from the library's and player's own composables, and tests check its claims against the
  real menus (DESIGN.md §6.5): change a menu action and `GuideAccuracyTest` says so.
- **Short windows:** layouts depend on height as well as width. The collection index needs 600 dp of height, the rail
  drops its labels below 480 dp, and a phone held sideways (891 x 411 and 740 x 360 dp) gets the rail with the folder bar
  (DESIGN.md §4). `LandscapePhoneGalleryTest` renders the screens at both sizes; do not measure a size inside a
  scrolling box (it is given unlimited height and never reads as short). Below 480 dp (`SHORT_WINDOW_HEIGHT`) Settings
  has no large title and the Continue watching cards shrink; the stats panel and the transport buttons are kept apart
  at every window shape (§5.3c).
- **Cold start:** the launch colour follows day/night and the first frame waits for the saved theme (`FirstFrameGate`,
  §4.3). Not checked on a real phone.
- **Delete:** "Move to trash" is the primary action where the platform supports it; "Delete permanently" is a red
  outlined alternative.
- **Merge queue is unavailable** (it needs an organization-owned repo). `main` has a ruleset (active since
  2026-10-28): PR required, `verify` and `connected` must pass on an up-to-date branch, linear history, no force-push
  or deletion. `verify` also runs the agent coordination checks (`tools/agents/pr_checks.py`).

## Hot files (one open PR at a time)

Enforced by CI from [`tools/agents/hot-files.txt`](../tools/agents/hot-files.txt): Room schema and database code,
`app/build.gradle.kts`, `gradle/libs.versions.toml`, `DESIGN.md`, `.github/`, `AGENTS.md`, this file and `tools/agents/`.
`CHANGELOG.md` is not on the list: Git already flags clashing edits to it.
