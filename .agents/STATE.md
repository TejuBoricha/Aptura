# Aptura - Current State

> The current truth about the project, kept short so every agent can load it. **Correct lines in place** when your
> change makes them false; do not append contradicting notes. History lives in [`MEMORY.md`](MEMORY.md); decisions in
> [`spec/17`](../spec/17_RISKS_ASSUMPTIONS_AND_DECISIONS.md) (DEC) and [`spec/08`](../spec/08_TECHNOLOGY_EVALUATION_AND_ADRS.md) (ADR).

**Last verified against:** `main` @ `330cef7` (2026-10-28)

## Project

- **Product:** Aptura, an offline-first Android video player. Package `org.aptura.player`. Public repo
  `TejuBoricha/Aptura` (personal account), Apache-2.0.
- **Phase:** 1.0 pre-release polish. The 1.0 scope is feature-complete; hardware verification is the remaining gate.
- **Team:** one owner (Tejas) and several AI agents working in parallel: Claude Code (cloud), OpenCode (various models,
  including DeepSeek), Antigravity. Coordination rules: [`AGENTS.md`](../AGENTS.md),
  [`docs/AGENT_WORKFLOW.md`](../docs/AGENT_WORKFLOW.md). This replaces the "solo AI developer" model in OD-4.
- **Owner's machine:** Windows, PowerShell. Use `;` not `&&` in commands meant for it.

## Quality gate

- `tools/gate.ps1` and CI (`.github/workflows/ci.yml`: `verify` = unit tests, lint, R8 release + `verifyReleaseApk`;
  `connected` = instrumented tests on an API 35 tablet emulator).
- Last full result: 666 tests / 97 suites, 0 failures, 77 opt-in gallery tests skipped; lint 0; release invariants
  clean.
- `gradle/actions/setup-gradle` is pinned to **v5 on purpose** (v6 pulls a proprietary build-cache component).

## Open work (highest first)

1. **Code review findings** on the merged player work ([`CODE_REVIEW.md`](../review/audit-2026-10-28/CODE_REVIEW.md)),
   to fix before a 1.0 tag:
   - F1/F2: the sleep timer subtracts requested steps instead of measuring elapsed time (drifts) and is a polling loop,
     which `.agents/rules/coding-standards.md` forbids.
   - F3: audio sync writes Room on every slider frame.
   - F4: audio sync inserts or drops PCM on every slider frame, so dragging is audible (then verify on device).
   - F5-F10: nits and low items listed in the review.
2. **Sleep timer spec gaps (FR-CTRL-009):** no 10 and 20 min presets, no custom duration, no extend offer after the
   timer has fired.
3. **Hardware verification:** physical-device playback, TalkBack, Bluetooth latency and detection, HDR, thermal.
4. **Release:** signed 1.0 tag when a signing key exists; social-preview image upload.
5. **Undecided:** one audio sync value for every video needs a storage decision first (per-video storage cannot tell 0
   from "never set").

## Facts that older notes get wrong

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
