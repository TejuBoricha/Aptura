# Agent Workflow

Lightweight coordination rules for AI coding agents working on Aptura. The hard rules live in [AGENTS.md](../AGENTS.md).

## 1. Pick up a task

- Work from an explicit request in your session or an issue labelled `agent-ready`.
- Claim `agent-ready` issues with label `claimed:<tool>` and comment `claim: <tool> (<model>)`.
- Check open PRs (`gh pr list`) before editing any file in `tools/agents/hot-files.txt`.

## 2. Branch, commits, and PRs

- Branch name: `agent/<tool>/<issue>-<slug>`. Never commit directly to `main`.
- End every AI-assisted commit with `Assisted-by: <Tool> (<model>)` (and `Refs: #<issue>` when linked to an issue). Human-written commits end with `Human-authored: yes`.
- Open a PR using [the PR template](../.github/pull_request_template.md). Keep `## Handoff` current while a PR is draft or in progress so another session can resume from the PR alone; set `Next: none` once ready.
- Before pushing or requesting review after modifying build tasks, CI workflows, permissions, or state docs, run `rg` across `docs/` and `.agents/` to catch stale cross-references in a single pass.

## 3. Automatic checks (`tools/agents/pr_checks.py`)

CI (`verify`) and `.github/workflows/agent-checks.yml` automatically block PRs that:

- contain commits without `Assisted-by:` / `Human-authored: yes` attribution (Dependabot/bot commits are exempt);
- modify a file in `tools/agents/hot-files.txt` that an older open PR already touches;
- duplicate an older open PR working on the same issue.

Test locally at any time with:

```powershell
python -m unittest discover -s tools/agents
```

## 4. Independent review & merge

- Run `.\tools\gate.ps1` before every push.
- Obtain one independent review by a **different model** than the author only when changing:
  1. security (`.github/`, tokens/permissions, `tools/agents/`, `tools/security/`),
  2. privacy invariants (`core/invariants/`, `AndroidManifest.xml`, `docs/PRIVACY.md`, APK/AAB checks), or
  3. Room schemas or migrations (`app/schemas/`, `data/database/`).
- Pass the reviewer only `git diff`, the PR description, `AGENTS.md`, and `.agents/rules/qa-and-code-review.md` (omit author reasoning), then post the exact prompt, model name, commit SHA, and unedited verdict on the PR.
- Once `verify` and `connected` pass on the current head (and independent review approves if required), rebase-merge with `--match-head-commit` and delete the branch.
