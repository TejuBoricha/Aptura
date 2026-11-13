# Agent Workflow

Lightweight engineering workflow for Aptura. Hard product and verification rules live in [AGENTS.md](../AGENTS.md).

## 1. Pick up a task

- Work from an explicit request in your session or an issue labelled `agent-ready`.
- Claim `agent-ready` issues with label `claimed:<tool>` and comment `claim: <tool> (<model>)`.
- Check open PRs (`gh pr list`) before editing any file in `tools/agents/hot-files.txt`.

## 2. Branch, commits, and PRs

- Branch name: `agent/<tool>/<issue>-<slug>`. Never commit directly to `main`.
- End every AI-assisted commit with `Assisted-by: <Tool> (<model>)` (and `Refs: #<issue>` when linked to an issue). Human-written commits end with `Human-authored: yes`.
- Open a PR using [the PR template](../.github/pull_request_template.md). Keep `## Handoff` current while a PR is draft or in progress; set `Next: none` once ready.
- Before pushing after changing build tasks, CI workflows, permissions, or state docs, run `rg` across `docs/` and `.agents/` to keep cross-references consistent in one pass.

## 3. Automatic checks (`tools/agents/pr_checks.py`)

CI (`verify`) and `.github/workflows/agent-checks.yml` automatically block PRs that:

- contain commits without `Assisted-by:` / `Human-authored: yes` attribution (Dependabot/bot commits are exempt);
- modify a file in `tools/agents/hot-files.txt` that an older open PR already touches;
- duplicate an older open PR working on the same issue.

Test locally at any time with:

```powershell
python -m unittest discover -s tools/agents
```

## 4. Verification & merge

- Run `.\tools\gate.ps1` before every push.
- Prove failure paths as well as happy paths on checks, invariants, and error handling (`"Prove it fails when it should"`).
- Security, privacy invariants, and Room migrations are gated by automated checks (`MergedManifestInvariantsTest`, `DependencyInvariantsTest`, `PlatformInvariantsTest`, `verifyReleaseApk`, `verifyReleaseBundle`, `ApturaDatabaseSchemaValidationTest`, `check_no_secret_files.py`); run a second-model review only if explicitly requested by the owner.
- Once `verify` and `connected` pass on the current head, rebase-merge with `--match-head-commit` and delete the branch.
