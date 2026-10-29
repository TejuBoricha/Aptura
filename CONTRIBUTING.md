# Contributing to Aptura

Thanks for taking the time to improve a local-first player. This project has a small set of hard constraints and a quality gate that is expected to stay green.

## Before you start

Read these three first:

1. [DESIGN.md](DESIGN.md) - visual system, player controls, component contracts.
2. [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - layers and invariants.
3. [docs/TESTING.md](docs/TESTING.md) - what evidence a change needs.

If your change adds a permission, a network library, a telemetry SDK or a software video decoder, stop: it conflicts with the product. Open an issue to discuss it first.

## Set up

```bash
git clone https://github.com/TejuBoricha/Aptura.git
cd Aptura
./gradlew assembleDebug
./tools/gate.ps1
```

Requirements and signing are covered in [docs/BUILDING.md](docs/BUILDING.md).

## Make a change

- Keep the change focused. One behavior per commit is easier to review than a mixed refactor.
- Follow the surrounding style: 4-space indentation, 120-column soft limit, LF line endings, trailing commas where the file already uses them.
- Put domain contracts under `domain/` and keep Android imports out of that package.
- Do not add a library if the platform or an existing dependency already does the job.
- Do not weaken an invariant test to make a change pass. Change the design or discuss the invariant.
- Do not add a control that is not wired. A button that shows a toast is not a feature.

## Add tests

The minimum evidence depends on the change:

| Change | Evidence |
|---|---|
| Logic, parsing, math | JVM unit test, including the failure path |
| Room query or schema | Real SQLite test; migration test if the schema changed |
| Scan, move, rename, missing file | Scanner or reconciliation test with the edge case |
| ViewModel behavior | Coroutine test with a fake repository; cancellation covered |
| Player control | ViewModel/Compose test plus the sheet render |
| Manifest or dependency | Invariant test under `core/invariants` |
| Screen or sheet | Compose test; regenerate the gallery image if it is part of the review evidence |

Tests run on the JVM by default. Use a fake at the platform seam, not a mocked implementation detail.

## Run the gate

```powershell
./tools/gate.ps1
```

Every commit should pass it. The gate compiles test sources, runs the JVM suite fail-fast, runs lint, then builds the R8 release and checks the shipped code for network and telemetry packages.

If the gate is too slow on your machine, run the same steps individually while iterating, but run the full gate before you push.

## Design changes

If you change a screen, a sheet or a shared component:

- Prefer the `ApturaDesignKit` components over a new one-off.
- Keep the player and its sheets dark in both app themes.
- Preserve 48dp touch targets and WCAG AA contrast.
- Update the gallery render under `review/audit-2026-10-28/evidence/` only when the change is part of that review, or add a new render where the evidence index expects it.
- Never present a Robolectric render as a device screenshot.

## Commits

Use an imperative subject that says what the commit does:

```
Add subtitle timing control to the player sheet
Fix trash-first hierarchy in the delete dialog
Document the design kit and player controls
```

Avoid `fixes`, `updates`, `wip` and `misc`. A body is welcome when the reason is not obvious.

## Pull requests

A good PR:

- explains the problem and the chosen behavior, not only the diff;
- lists the tests added or updated;
- calls out anything not verified on a device;
- keeps unrelated formatting and refactors out;
- updates the docs when a contract, a screen or a workflow changed.

Small PRs get reviewed faster. If a change needs a migration, a new dependency or a new permission-adjacent behavior, open an issue first.

## AI coding agents

Several AI agents contribute to this repository in parallel. They follow [AGENTS.md](AGENTS.md) and the procedure in [docs/AGENT_WORKFLOW.md](docs/AGENT_WORKFLOW.md): they take work only from the owner (a direct request, or an issue labelled `agent-ready`), use one branch and one PR per task, keep a Handoff block in the PR, and end each commit with an `Assisted-by: <Tool> (<model>)` trailer.

**Every commit in a pull request needs an attribution trailer, or CI fails.** If you wrote the commit yourself, end its message with `Human-authored: yes`; if an AI assistant helped, use `Assisted-by: <Tool> (<model>)`. CI also blocks a pull request that changes a file another, older open pull request is already changing in [tools/agents/hot-files.txt](tools/agents/hot-files.txt), or that works on the same issue. See "Automatic checks" in [docs/AGENT_WORKFLOW.md](docs/AGENT_WORKFLOW.md). A commit that only carries an AI tool's own `Co-Authored-By:` line passes, with a warning to add `Assisted-by:`.

The agents' progress, and anything that needs the owner, is on the [agent status page](https://github.com/TejuBoricha/Aptura/issues?q=is%3Aissue+is%3Aopen+label%3Aagent-status).

## Reporting bugs

Use the bug report form. Include:

- Android version and device/emulator;
- the exact steps;
- what happened and what you expected;
- the exported diagnostic report, if you are comfortable sharing it (it is redacted, but review it first).

Do not paste full file paths or personal file names into a public issue.

## Security

Do not open a public issue for a vulnerability. Follow [SECURITY.md](SECURITY.md).

## License

By contributing, you agree that your contribution is licensed under the Apache License 2.0, the same license as the project.
