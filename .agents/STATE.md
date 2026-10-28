# Aptura - Current State

> The current truth about the project, kept short so every agent can load it. **Correct lines in place** when your
> change makes them false; do not append contradicting notes. History lives in [`MEMORY.md`](MEMORY.md); decisions in
> [`spec/17`](../spec/17_RISKS_ASSUMPTIONS_AND_DECISIONS.md) (DEC) and [`spec/08`](../spec/08_TECHNOLOGY_EVALUATION_AND_ADRS.md) (ADR).

**Last verified against:** `main` @ `329dec5` plus PR #4 on `agent/opencode/sleep-timer-and-review-fixes` (2026-10-28)

## Project

- **Product:** Aptura, an offline-first Android video player. Package `org.aptura.player`. Public repo
  `TejuBoricha/Aptura` (personal account), Apache-2.0.
- **Phase:** 1.0 pre-release polish. The 1.0 scope is feature-complete; hardware verification is the remaining gate.
- **Team:** one owner (Tejas) and several AI agents working in parallel: Claude Code (cloud), OpenCode (various models,
  including DeepSeek), Antigravity. Coordination rules: [`AGENTS.md`](../AGENTS.md),
  [`docs/AGENT_WORKFLOW.md`](../docs/AGENT_WORKFLOW.md). This replaces the "solo AI developer" model in OD-4.
- **Owner's machine:** Windows, PowerShell. Use `;` not `&&` in commands meant for it.

## Quality gate

- `tools/gate.ps1` and CI (`.github/workflows/ci.yml`: `verify` = agent coordination checks, unit tests, lint, R8
  release + `verifyReleaseApk`; `connected` = instrumented tests on an API 35 tablet emulator).
- The gate also runs the checker's Python tests (`python -m unittest discover -s tools/agents`) when Python 3 is
  installed; CI always runs them.
- Last full result: 679 tests / 100 suites, 0 failures, 0 errors, 78 opt-in gallery tests skipped; lint 0; release
  invariants clean (branch `agent/opencode/sleep-timer-and-review-fixes`, rebased on `main` @ `329dec5`).
- `gradle/actions/setup-gradle` is pinned to **v5 on purpose** (v6 pulls a proprietary build-cache component).

## Open work (highest first)

1. **PR #4 review:** the 2026-10-28 review findings F1-F6, F9 and F10 and the FR-CTRL-009 gaps are fixed on
   `agent/opencode/sleep-timer-and-review-fixes`. Playback changes need one independent review by a different model
   before merge.
2. **Hardware verification:** physical-device playback, TalkBack, Bluetooth latency and detection, HDR, thermal,
   process-kill during a SAF copy. Run [DEVICE_CHECKLIST.md](../docs/DEVICE_CHECKLIST.md) before tagging 1.0.
3. **Release:** signed 1.0 tag when a signing key exists; social-preview image upload.
4. **Undecided:** one audio sync value for every video needs a storage decision first (per-video storage cannot tell 0
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
- **Delete:** "Move to trash" is the primary action where the platform supports it; "Delete permanently" is a red
  outlined alternative.
- **Merge queue is unavailable** (it needs an organization-owned repo). `main` has a ruleset (active since
  2026-10-28): PR required, `verify` and `connected` must pass on an up-to-date branch, linear history, no force-push
  or deletion. `verify` also runs the agent coordination checks (`tools/agents/pr_checks.py`).

## Hot files (one open PR at a time)

Enforced by CI from [`tools/agents/hot-files.txt`](../tools/agents/hot-files.txt): Room schema and database code,
`app/build.gradle.kts`, `gradle/libs.versions.toml`, `DESIGN.md`, `.github/`, `AGENTS.md`, this file and `tools/agents/`.
`CHANGELOG.md` is not on the list: Git already flags clashing edits to it.
