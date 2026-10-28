# Working with several AI agents

Aptura is developed by one owner and several AI coding agents, often at the same time and from different tools.
This guide is the procedure behind the rules in [`AGENTS.md`](../AGENTS.md).

Git and GitHub are the coordination layer, because they are the one thing every tool shares:

- **Shared context:** `AGENTS.md` and `.agents/STATE.md`.
- **Tasks:** GitHub Issues.
- **Progress:** branches, plus the pull request's `## Handoff` block.
- **Acceptance:** the protected merge into `main`.

## Lifecycle of a task

```
issue (owner adds agent-ready)
  -> agent claims it (label claimed:<tool> + claim comment)
  -> branch agent/<tool>/<issue>-<slug>, own worktree or sandbox
  -> draft PR after the first meaningful commit, Handoff block filled in
  -> commits pushed at each green checkpoint
  -> PR ready: gate green, trailers present, review recorded if required
  -> owner merges (rebase), STATE.md corrected in the same PR if needed
```

### 1. Create the task (owner)

There are two ways to give an agent work:

- **Directly, in the agent's own session.** This is quickest for work one agent will finish. The agent makes the
  work visible by opening a draft PR early.
- **As an issue.** Use this for anything another agent might need to pick up, or when several agents work at once.

For an issue, use the **Agent task** form. It asks for the goal, acceptance criteria, and the files or areas the
task is expected to touch. Add the `agent-ready` label when it is ready for an agent. Agents ignore issues without it,
so a stranger's issue on this public repo cannot steer them.

### 2. Claim it (agent)

For a direct request, skip to step 3 and open the draft PR early. For an issue:

1. Check that the issue has `agent-ready` and no `claimed:*` label, and is not blocked by another open issue.
2. Add `claimed:<tool>` (for example `claimed:codex`) and comment `claim: <tool> (<model>)`.
3. If two claims race, the earlier comment wins. The other agent removes its label and stops.

### 3. Isolate the work

Each agent gets its own branch and its own working copy.

```bash
git fetch origin
git worktree add ../aptura-42 -b agent/codex/42-global-audio-sync origin/main
```

On Windows run the two commands separately. Cloud agents get their own sandbox automatically. Run no more than two
local Gradle builds at once.

### 4. Check for overlap before editing hot files

Hot files are listed in `AGENTS.md` and `.agents/STATE.md`. They merge cleanly in Git but break when two branches
change them independently, for example two branches that each add Room migration 6→7. Before touching one, check the
open PRs:

```bash
gh pr list --state open --json number,headRefName --jq '.[].number' |
  xargs -I{} gh pr diff {} --name-only
```

If another PR already touches the file, comment on your issue and wait, or ask the owner to sequence the work.

### 5. Leave a handoff, continuously

Open a **draft PR** as soon as there is a meaningful commit, and keep the `## Handoff` block in its description
current at every checkpoint:

```markdown
## Handoff
_Updated 2026-10-28 14:10 by Claude Code (claude-opus-5-5)_
- **Goal:** #41 sleep timer 10/20 min presets and custom duration
- **Done:** presets 10/20 (a1b2c3d), tests for both
- **Next:** 1. custom duration dialog 2. gallery render
- **Decisions:** custom duration is capped at 12 h (owner's call in the issue thread)
- **Verified:** `testDebugUnitTest` green at a1b2c3d; lint not run
- **Gotchas:** SegmentedPicker truncates past 5 options at 360dp
```

Push often. A session can end at any moment through a usage limit, a crash or a closed laptop. Pushed commits and
the handoff survive it; uncommitted changes do not.

### 6. Taking over someone else's task

Use the same prompt in any tool:

> Continue PR #N for issue #M. Read `AGENTS.md`, `.agents/STATE.md`, the issue, the PR's Handoff block and
> `git log origin/main..HEAD` on its branch. Do not redo completed steps. Add a Handoff line saying you took over,
> then continue from "Next".

Check out the existing branch rather than starting a new one. Swap the `claimed:*` label to your tool.

### 7. Commit trailers

```
Fix sleep timer drift by measuring elapsed time

Assisted-by: OpenCode (deepseek-v4.1-flash)
Refs: #41
```

`Assisted-by` names the tool and model that helped; the owner stays the accountable author, as in the Linux kernel's
policy. A tool may add its own `Co-Authored-By` or session trailer as well. Commits written by hand need no trailer.

### 8. Review

| Change | Required review |
|---|---|
| Docs, copy, isolated UI tweak | The gate only |
| Playback, file operations, Room schema or migrations, privacy invariants, or about 300+ changed lines | The gate plus one review by a **different model** than the author, recorded in the PR |
| Anything | Never merge on a review alone while a test fails |

Reviews go in the PR or in `review/` for large audits, such as `review/audit-2026-10-28/CODE_REVIEW.md`. Which
reviewer finds real defects is tracked over time: keep a reviewer whose findings you usually accept, and drop one
that mostly produces noise.

### 9. Merge (owner)

The owner merges with **rebase** once CI is green and the branch is up to date with `main`. If the branch is behind,
the agent rebases and CI runs again on the combined code. That is the step that catches two changes which each pass
alone but break together. If a merged change turns out wrong, `git revert` its commits; `Refs: #N` finds them.

## Owner setup checklist

These settings live in GitHub, not in the repository, so only the owner can apply them.

1. **Ruleset for `main`:** Settings → Rules → Rulesets → New branch ruleset. Target: default branch. Enable:
   - Restrict deletions; Block force pushes; Require linear history.
   - Require a pull request before merging (0 required approvals, because you cannot approve your own PR);
     Require conversation resolution.
   - Require status checks to pass: `verify` and `connected`, with **Require branches to be up to date before
     merging** turned on.
2. **Labels:** `agent-ready` (owner-only signal), plus `claimed:claude`, `claimed:codex`, `claimed:opencode`,
   `claimed:antigravity`, and one per other tool you use.
3. **Secret protection:** Settings → Code security → enable secret scanning and push protection.
4. **Agent tokens:** give each tool its own fine-grained token, scoped to this repository, with contents and pull
   request write access. Grant no admin rights, so no agent can change the ruleset.

## What this deliberately does not do

- **No custom orchestrator, dashboard or shared vector memory.** GitHub's issue, PR and check views are the dashboard.
- **No agent-to-agent messaging.** Agents communicate through issues, PRs and commits, which are durable and visible
  to the owner.
- **No real-time file locks.** Overlap is detected at the PR level. Revisit this only if agents keep editing the same
  files at the same time.
- **No merge queue.** It is not available on personal-account repositories; up-to-date branches serve the same
  purpose at this volume.

Planned next steps, once this has been used for a few weeks: CI checks for trailers and PR overlap, and a script that
claims an issue and creates the worktree in one step.
