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
  -> an agent merges (rebase) once CI is green and the required review approved, STATE.md corrected if needed
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

Hot files are listed in [`tools/agents/hot-files.txt`](../tools/agents/hot-files.txt) and named in `AGENTS.md`.
They merge cleanly in Git but break when two branches change them independently, for example two branches that each
add Room migration 6→7. Before touching one, look at the open PRs (`gh pr list --state open`, then
`gh pr view <n> --json files`). If one already changes the file, wait for it, or ask the owner to sequence the work.

CI enforces the same rule, so a mistake here is caught rather than merged (see "Automatic checks" below).

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
policy. A tool may add its own `Co-Authored-By` or session trailer as well. A commit written by hand ends with
`Human-authored: yes` instead. `Refs` is only needed when there is an issue. Name the model when the tool allows it;
CI checks that the trailer is present and not empty, not its exact format.

### 8. Review

| Change | Required review |
|---|---|
| Docs, copy, isolated UI tweak | The gate only |
| Playback, file operations, Room schema or migrations, privacy invariants, or about 300+ changed lines | The gate plus one review by a **different model** than the author, recorded in the PR |
| Anything | Never merge on a review alone while a test fails |

If the reviewer you asked is away, use the fallback ladder in "Reviews when the reviewer is away" below rather than
waiting for it. Reviews go in the PR or in `review/` for large audits, such as `review/audit-2026-10-28/CODE_REVIEW.md`. Which
reviewer finds real defects is tracked over time: keep a reviewer whose findings you usually accept, and drop one
that mostly produces noise.

### 9. Merge

The agents merge without the owner, with **rebase**, once the conditions in `AGENTS.md` hold: CI green on an
up-to-date branch, the required independent review approved with its must-fix findings re-checked, and no message
left unanswered. Usually the reviewer merges after confirming the fixes; the author may merge once the reviewer has
approved. The merging agent posts `To: all` with what went in. The owner is asked only for product decisions,
settings or secrets, irreversible actions outside the repository, or a disagreement two rounds did not settle. If the branch is behind,
the agent rebases and CI runs again on the combined code. That is the step that catches two changes which each pass
alone but break together. If a merged change turns out wrong, `git revert` its commits; `Refs: #N` finds them.

## Talking to each other

Agents talk on the pull request, so the owner does not have to copy messages between tools. Everything posts through
the owner's GitHub account, so two plain lines say who is talking to whom:

```markdown
To: opencode
PR #4 review is posted: two small fixes requested, details below.
...
From: claude
```

- **First line** `To:` names one or more agents, comma-separated, or `all`.
- **Last line** `From:` names the sender. Do not use `@name`: it notifies real GitHub users with those names.
- **Waiting for a reply:** run `python3 tools/agents/inbox.py --pr <n> --me <your agent>`. It checks once a minute,
  prints new messages addressed to you and exits 0. It exits 3 if the PR is merged or closed and 2 after the
  `--timeout` (default four hours), so a local agent can wait for hours without anyone relaying.
- **Cloud agents** that already receive GitHub events (Claude Code on the web, subscribed to the PR) wake up on the
  comment by themselves and do not need `inbox.py`.

What still needs the owner: starting a session when no agent is running, and decisions. A message on the PR waits
until the addressed agent is running and checks. A fully automatic setup, where a comment such as `/oc` or `@claude`
starts an agent in GitHub Actions, exists for both tools. It needs a paid model key stored as a repository secret, and
a token other than `GITHUB_TOKEN` for one bot's comment to trigger the other, because GitHub deliberately does not
start workflows from `GITHUB_TOKEN` events. Consider it only if this lighter setup still leaves too much relaying.

## When an agent stops

Agents stop without warning: a usage limit, a crash, a restarted cloud session, a closed laptop. The rules below keep
the work moving without the owner relaying anything, and keep two agents from colliding when one comes back. They
apply to any tool and model.

### The principles

1. **The owner's word wins, at any time.** "Take over" or "go ahead" means act now, without waiting for a timer.
   "Wait", "hold" or "stop" means no agent pushes, takes over or merges on that PR until the owner lifts it. No timer
   or rule here overrides the owner.
2. **The owner's instructions go on the PR.** The owner usually talks to one agent in its own session, and the other
   agents cannot see that chat. An agent told something about a PR posts it there before acting on it:

   ```markdown
   To: all
   Owner said (in my session, 2026-10-29 09:10 UTC): take over #4 now; OpenCode is out of limit until tonight.
   From: claude
   ```

   If two recorded instructions conflict, the newer one wins. If that still leaves the next step unclear, ask the
   owner; that is a decision, not message-carrying.
3. **One agent drives a PR at a time.** The first line of the PR description says who and what it waits on, and the
   driver keeps it current:

   ```markdown
   Status: driven by opencode — waiting for claude's review (asked 14:05 UTC)
   ```

   Only the driver pushes to the branch. Only the driver or the approving reviewer merges.
4. **Re-read before every action.** After any pause (a usage limit, a restart, a long `inbox.py` wait), read the PR's
   comments, head commit and CI again before pushing, merging or posting. Never act on a plan made before the pause.
   Push with `--force-with-lease`, which refuses to overwrite commits you have not seen.
5. **Push after every finished step**, not only at green checkpoints. A small commit on your own branch is cheap;
   unpushed work is the only work a stop can lose. Each commit still needs its trailers.

### Stopping, and taking over

- **If you know you are about to stop** (a usage-limit warning, the end of a session), push, update the Handoff, and
  post a pause note. Another agent may take over at once:

  ```markdown
  To: all
  Pausing: usage limit, back around 18:00 UTC. Pushed a1b2c3d. Next: the custom-duration dialog (Handoff is current).
  From: opencode
  ```

- **If an agent goes silent,** a `To:` message addressed to it that gets no reply and no push from it for **2 hours**
  lets another agent take over. CI running is not silence. An agent that needs longer acknowledges with a line starting
  `Working:` ("Working: reviewing, done by 15:00 UTC"), which restarts the 2 hours but does not end the wait. Use
  `inbox.py --timeout 7200` so a local agent's wait ends when this rule starts to apply. The hourly stall check
  announces the overdue wait on the PR (see "Nothing waits without a deadline").
- **Taking over** starts with a comment, before any push:

  ```markdown
  To: all
  Taking over #4 from opencode: no reply to the 14:05 message for 2 hours. Continuing from the Handoff's "Next".
  From: claude
  ```

  Then follow "Taking over someone else's task" above and update the `Status:` line. If two agents post `Taking over`,
  the earlier comment wins; the other posts that it is backing off and does not push.
- **Coming back** after a stop, read the PR first. If someone took over, do not push to the branch; post that you are
  back and offer to review. Changes you never pushed are offered as a suggestion on the PR or a follow-up commit agreed
  with the driver, not force-pushed over theirs.
- **A takeover does not skip review.** Code an agent pushes while taking over needs a review by a different model, like
  any other commit. A reviewer who takes over cannot approve its own new commits; the returning author, or a third
  model, reviews them.

### Steps only some agents can do

Agents do not all have the same permissions. On this repository, for example, re-running a failed CI job needs Actions
write access, which the cloud Claude session has and OpenCode's local token does not. When the driver is blocked on
such a step, it posts `To: all` naming the step, and any agent that has the permission does it. If none does, the
owner is asked, as for any setting or secret. A step that only one agent can do should be written down here, so the
others know whom to ask.

### When every agent is stopped

The work waits on GitHub: the branch, the Handoff and the `Status:` line. Nothing is lost that was pushed, and the
owner has nothing to do. The first agent to come back reads the PR and continues. A third tool (any agent that reads
this file) can pick it up the same way. Only an agent running in GitHub Actions, which needs a paid model key (see
"Talking to each other"), would remove this pause.

## Nothing waits without a deadline

Every wait between agents has a deadline and a fallback, so no flow depends on one particular agent coming back. This
is the general rule; the table lists the waits known so far.

| Waiting for | Deadline | Then |
|---|---|---|
| A reply to a `To:` message | 2 h, restarted by a `Working:` note; at once after `Pausing` | Another agent may take over (rule 9) |
| An independent review | 2 h after the request | The fallback reviewer (below) |
| Critiques in a debate | 2 h after the proposal | Fallback critics, the same way |
| The proposer's answer to critiques | 2 h after the last critique | Another agent may take the debate over |
| A step only some agents can do | 2 h after asking | The owner, once (a setting or a permission) |
| CI | the job's own timeout | An infrastructure failure gets one re-run by an agent with the permission |
| An owner decision | none: the owner is not on a clock | The question stays at the top of the status issue; agents work on something else meanwhile, and never take a product decision by default |

**When you create a new kind of wait,** say in the same message what happens if nobody answers, and by when. A wait
with no stated fallback falls back to the 2-hour rule.

**A message that needs no answer** (a merge note, an approval with nothing left to do) says `No reply needed`, so it
does not count as a wait.

**The owner's "wait" or "stop"** is recorded as a line `Hold: <reason>` in the agent's `Owner said` comment, and
lifted with `Hold lifted: <reason>`. While a hold stands, no deadline on that PR or debate runs out.

**The hourly stall check.** The **Agent status** workflow (`.github/workflows/agent-status.yml`) runs
`tools/agents/stalls.py` every hour, with no AI model and no secret. It finds the waits above that passed their
deadline on open pull requests and debates. On each, it posts one `To: all` comment, signed `From: agent-status`,
that names the stall and the fallback. The comment wakes the agents: local agents waiting in `inbox.py`, and cloud
agents subscribed to the PR. It also keeps a one-page summary in the issue labelled `agent-status`, which is the
owner's view of what is moving and what is stuck. To see the same report locally:
`python3 tools/agents/stalls.py`.

**Only the owner's account counts.** Every agent posts through the owner's GitHub account. On this public
repository anyone else can comment, so `inbox.py` and the stall check ignore every other author except
`github-actions[bot]`, the stall check's own account. A stranger can't address an agent, fake a `Hold:`, or close a
wait. Debates opened by anyone but the owner are not work.

## Reviews when the reviewer is away

The independent reviewer is a role, not a particular agent. Take the first of these that is possible:

1. The agent that was asked, if it answers within 2 hours (or posts `Working:`).
2. Any other running agent whose model differs from the author's.
3. A review the author obtains itself from a different model, with the model recorded on the PR. Any of these will
   do:
   - a review subagent on another model;
   - a one-off session in another tool (Gemini CLI, Codex, OpenCode with another provider);
   - GitHub's Copilot code review on the PR.

   Give that reviewer only the diff, the PR description, `AGENTS.md` and
   [`.agents/rules/qa-and-code-review.md`](../.agents/rules/qa-and-code-review.md), not the author's reasoning, so it
   judges the work rather than the story. Post its full verdict on the PR, so anyone can check it.
4. If none of these is possible, the PR waits and the status issue shows it. The owner is never asked to review code.

A model from a **different vendor** is preferred, because two models from one family tend to share blind spots. For
privacy invariants, Room migrations and file deletion, a same-vendor fallback is not enough: wait for a different
vendor through steps 1–3. When the original reviewer comes back, it may still review after the merge. Its must-fix
findings become a follow-up PR, which goes first in its queue.

## Debates

A debate is how the agents decide something that has more than one reasonable answer and is expensive to get wrong.
Examples are an architecture or data-model choice, a change to this workflow, or anything touching hot files. Routine
fixes don't need one.

1. **Open it.** Use an issue made with the **Debate** form and labelled `debate`. The owner opens it, or an agent opens
   it on the owner's request in its chat and says so in the issue.
2. **Proposal.** One agent posts a comment with a line starting `Proposal:`. It covers the problem, two or three
   options, a recommendation, and the risks. Its evidence is concrete: `file:line`, test output, measurements, or
   linked sources. A claim without evidence counts as an opinion.
3. **Critiques.** At least **two** agents running models other than the proposer's each post a comment with a line
   starting `Critique:`. Prefer at least one from a different vendor. Write your critique before reading the other
   critiques, so the second is independent of the first. Each critique ends with a verdict: agree, agree with
   changes, or object. A critique that agrees still names at least one risk or missing scenario, so agreement is
   never a rubber stamp. One model fills one critic slot, and the proposer cannot critique its own proposal. If
   critics are missing after 2 hours, use the fallback ladder above.
4. **Rebuttal.** The proposer answers once, in a comment with a line starting `Rebuttal:`. For each must-fix point,
   it accepts the change or gives the evidence against it, then states the revised plan.
5. **Decision.** If no critic still objects, the proposer posts a line starting `Decision:` with:
   - the plan;
   - the task list;
   - what was rejected, and why;
   - `Owner approval needed: yes/no`.

   Approval is needed when the plan changes what the user sees or does, or takes more than about a day of agent work.
   If an objection survives the rebuttal, there is one more round of critique and rebuttal. If it still stands, the
   owner gets a one-screen summary of the options with a recommendation, and picks.
6. **Approval.** The owner answers in any agent's chat ("approve", or a choice). That agent posts it on the issue as
   `Owner said …`.
7. **Build.** Each task in the decision becomes an issue or a checklist item. Agents claim and merge them under the
   usual rules. The proposer records a durable decision in the DEC register
   ([`spec/17`](../spec/17_RISKS_ASSUMPTIONS_AND_DECISIONS.md)) in the first task's PR.

## Automatic checks

`tools/agents/pr_checks.py` runs first in the required `verify` CI job on every pull request. It takes a few seconds.

| Check | Result |
|---|---|
| A commit has neither `Assisted-by:` nor `Human-authored: yes` (bot accounts such as Dependabot are exempt) | **Blocks**, naming the commits |
| An **older** open PR already changes the same hot file | **Blocks** the newer PR until the older one merges or closes, then the newer one rebases and pushes |
| An **older** open PR references the same issue (`Refs`, `Closes`, `Fixes`, or `Issue: #N` in the title, body or commits) | **Blocks**: one task, one PR |
| The older PR has been idle for 14 days | Only warns, so an abandoned draft cannot block forever |
| Ordinary files overlap with another open PR | Warns; whichever merges second rebases and re-runs CI |
| Half or more of the PR's files overlap another PR | Warns that the two may be the same task |
| A draft PR has no `## Handoff` block | Warns |

"Older" means the lower PR number, so two PRs never block each other: the first one goes in first. Only PRs into the
same base branch are compared, and a renamed file counts under its old path too. The result shows in the job summary.
If a blocking PR closes without merging, push again or re-run the job to clear the check.

Editing a PR's title or description, or marking a draft ready, re-runs only this script (the **Agent checks**
workflow) rather than the whole build. That re-run is **not a required check**: a red X there is a visible warning,
and it becomes blocking at the next push, when `verify` runs the same script. To make edits block immediately, add
`agent-checks` to the ruleset's required status checks.

If GitHub is down or rate-limiting (5xx, 429, or a 403 with an exhausted counter or a `Retry-After`) after three
tries, the check is skipped with a warning instead of blocking every open PR; the tests and build in the same job
still gate the code. A permission or token error is not retried or skipped: it fails at once, so a misconfiguration
cannot silently switch the checks off. GitHub lists at most 250 commits per pull
request, so commits beyond that are not checked.

To check a PR before pushing more work: `python3 tools/agents/pr_checks.py --repo TejuBoricha/Aptura --pr <n>`. It
needs no token for this public repository. Its tests run with `python3 -m unittest discover -s tools/agents`.

## Owner setup checklist

These settings live in GitHub, not in the repository, so only the owner can apply them.

1. **Ruleset for `main`:** Settings → Rules → Rulesets → New branch ruleset. Target: default branch. Enable:
   - Restrict deletions; Block force pushes; Require linear history.
   - Require a pull request before merging (0 required approvals, because you cannot approve your own PR);
     Require conversation resolution.
   - Require status checks to pass: `verify` and `connected`, with **Require branches to be up to date before
     merging** turned on.
2. **Labels:** created automatically by the **Labels** workflow (`.github/workflows/labels.yml`) when it reaches
   `main`: `agent-ready` (owner-only signal), `task`, `debate`, `agent-status`, and `claimed:<tool>` for each tool.
   To add a tool, edit that file.
3. **Secret protection:** Settings → Code security → enable secret scanning and push protection.
4. **Agent tokens:** give each tool its own fine-grained token, scoped to this repository, with contents, pull
   request and issue write access, and **Actions read and write**, so every agent can re-run a failed CI job instead
   of waiting for the one that can. Grant no admin rights, so no agent can change the ruleset.

## What this deliberately does not do

- **No custom orchestrator, dashboard or shared vector memory.** GitHub's issue, PR and check views are the dashboard.
- **No agent-to-agent messaging.** Agents communicate through issues, PRs and commits, which are durable and visible
  to the owner.
- **No real-time file locks.** Overlap is detected at the PR level when an agent pushes. Revisit this only if agents
  keep editing the same files at the same time before either pushes.
- **No merge queue.** It is not available on personal-account repositories; up-to-date branches serve the same
  purpose at this volume.

Possible next steps, only if usage shows the need: an AI review that runs automatically on risky pull requests (GitHub
Agentic Workflows can do this, but it needs a paid model key), and a script that claims an issue and creates the
worktree in one step.
