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
`gh pr view <n> --json files`). If one already changes the file, wait for it, or agree the order with a `To:` message on the older PR. The
block clears by itself: once the older PR merges, the newer one must rebase to be up to date with `main`, and that
push re-runs the check.

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
alone but break together. A rebase that needed no conflict resolution keeps the review's approval; one that resolved
conflicts in reviewed code needs the reviewer, or another model, to re-check the resolution. If a merged change turns out wrong, `git revert` its commits; `Refs: #N` finds them.

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
- **A name is its first word**, so `From: OpenCode (DeepSeek V4)` and `To: Claude Code` mean opencode and claude.
  The `To:` line holds names only: write `To: claude` and the request on the next line, not `To: claude, please
  review`, which would address an agent called "please".
  Agents use one stable name each: `claude`, `opencode`, `codex`, `gemini`, `cursor`, `copilot`.
- **Answer on the PR, not only with a push.** A wait ends when the addressee replies to the sender (or to `all`). A
  push alone does not answer anything; say what the push did.
- **`python3` means Python 3.9 or newer.** On Windows, use `py -3` in its place.
- **Waiting for a reply:** run `python3 tools/agents/inbox.py --pr <n> --me <your agent>`. It uses `GITHUB_TOKEN`, or
  the GitHub CLI's login if you have run `gh auth login`; without a token it checks only every 4 minutes, to stay
  inside GitHub's limit of 60 requests an hour. `<n>` can be a pull request
  or a debate issue. It reads from your own latest message there, so a reply that arrived before you started waiting
  is not lost. It checks once a minute, prints new messages addressed to you (or to `all`, which includes the stall
  check's nudges) and exits 0. It exits 3 if the PR is merged or closed and 2 after the `--timeout` (default four
  hours), so a local agent can wait for hours without anyone relaying.
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

- **If an agent goes silent,** a `To:` message addressed to it that gets no reply from it within **30 minutes** lets
  another agent take over. An agent that is running answers within minutes, so 30 minutes of silence means it is away.
  Long work is not silence: acknowledge it with a line starting `Working:` ("Working: reviewing, done by 15:00 UTC"),
  which restarts the 30 minutes without ending the wait. A pause note makes the wait overdue at once, even one posted
  before the message. The stall check announces an overdue wait on the PR within about 15 minutes (see "Nothing
  waits without a deadline").
- **Never wait idle.** While one step waits on someone, work on your next task. Only the step that needs the answer
  pauses, never the agent.
- **Taking over** starts with a comment, before any push:

  ```markdown
  To: all
  Taking over #4 from opencode: no reply to the 14:05 message for 30 minutes. Continuing from the Handoff's "Next".
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
such a step, it addresses the message to an agent that has the permission (`To: claude`), not to `all`, so the stall
check holds it to the 30-minute deadline. If nobody can do it, the owner is asked once, as for any setting or secret.
Better still, give every agent's token the permissions in the owner setup checklist below, so no step depends on one
agent. A step that only one agent can do is written down here, so the others know whom to ask.

### When every agent is stopped

The work waits on GitHub: the branch, the Handoff and the `Status:` line. Nothing is lost that was pushed, and the
owner has nothing to do. The first agent to come back reads the PR and continues. A third tool (any agent that reads
this file) can pick it up the same way. The status issue keeps showing the stall, and it is the only signal while
every agent is away. Only an agent running in GitHub Actions, which needs a paid model key (see "Talking to each
other"), would remove this pause.

## Nothing waits without a deadline

Every wait between agents has a deadline and a fallback, so no flow depends on one particular agent coming back. This
is the general rule; the table lists the waits known so far.

| Waiting for | Deadline | Then |
|---|---|---|
| A reply to a `To:` message | 30 min, restarted by a `Working:` note; at once after `Pausing` | Another agent may take over (rule 9) |
| An independent review | 30 min for the reviewer to answer or post `Working:`; at once when it is known to be away | The fallback reviewer (below), then `Withdrawn: <reviewer>` |
| Critiques in a debate | 2 h after the proposal | Fallback critics, the same way |
| The proposer's answer to critiques | 2 h after the last critique | Another agent may take the debate over |
| A whole debate | 24 h, and two rounds | The owner gets a one-screen summary and picks |
| A step only some agents can do | 30 min after asking | The owner, once (a setting or a permission) |
| CI | the job's own timeout | An infrastructure failure gets one re-run by an agent with the permission |
| A claimed task to become a pull request | 2 h after the `claim:` comment | Another agent may take the task over |
| Any sign of life on an open PR | 4 h without a message or commit | Its driver posts where it stands, or another agent takes over |
| Anything above, once announced | 1 day with nobody acting | Listed under "Needs you": perhaps no agent is running |
| An owner decision | none: the owner is not on a clock | It stays under "Needs you" in the status issue; agents work on something else meanwhile, and never take a product decision by default |

**When you create a new kind of wait,** say in the same message what happens if nobody answers, and by when. A wait
with no stated fallback falls back to the 30-minute rule.

**The words the stall check reads.** They count only at the start of one of the comment's own lines, never inside
quoted (`>`) text or code blocks. Bold labels and list bullets are fine (`**To:** opencode`, `- Working: …`). To see
how a message will be read before posting it, pipe it into `python3 tools/agents/stalls.py --explain`; a message
whose `To:` line is not first, or that lacks `From:`, is reported as unreadable rather than silently ignored.

| Line | Meaning |
|---|---|
| `Working: <what>, done by <HH:MM> UTC` | I am on it. Holds the wait until that time plus 30 minutes, up to 24 hours ahead; without a time in UTC (`2:30pm`, `CET`), for 2 hours. Counts however it is addressed, and never reopens a question already answered. An agent may renew it; the status issue shows the wait meanwhile. |
| `Pausing: <why>, back around <time>` | I am stopping. Anyone may take over now. |
| `No reply needed` (anywhere in the message's own text, not in a quote or code) | This message is not a wait. Put it on merge notes, status notes, and on an approving review, whose author merges rather than answers. Any other addressed message is a wait, so a forgotten one is caught rather than missed. |
| `Withdrawn: <agent>` | The sender no longer waits on that agent, for example after using a fallback reviewer. |
| `Taking over #<n> from <agent>: <why>` | Ends the waits on that agent, when another agent posts it. |
| `Hold: <reason>` / `Hold lifted: <reason>` | The owner's "wait" and "go", recorded by the agent that heard it. While a hold stands, no deadline on that PR or debate runs out. A hold older than a day is listed for the owner. |
| `Override: <step> on #<n>` with `Follow-up: <what>` | The owner told an agent to skip a step. Listed in the status issue until `Follow-up done:` (see "When the owner overrides the process"). |

`To: owner` is never a wait: the owner answers in chat, on no clock. A question for the owner goes in the agent's
chat and, if it blocks a decision, under "Needs you" through a `Decision:` that needs approval.

**The stall check.** The **Agent status** workflow (`.github/workflows/agent-status.yml`) runs
`tools/agents/stalls.py` every 15 minutes, with no AI model and no secret. It finds the waits above that passed their
deadline on open pull requests and debates. On each, it posts one `To: all` comment, signed `From: agent-status`,
that names the stall and the fallback. The comment wakes the agents: local agents waiting in `inbox.py`, and cloud
agents subscribed to the PR. It also keeps the issue labelled `agent-status` current. That issue opens with **Needs
you**: the decisions waiting for the owner and the holds older than a day. It is the owner's one page for what is
moving and what is stuck. Editing that page notifies nobody, so when something new needs the owner the check also
posts one comment there, which GitHub emails to the owner; agent-only stalls never do. It also lists every `Owner
said` line recorded in the last week, so the owner can confirm they are their words. To see the same report
locally: `python3 tools/agents/stalls.py`.

Each run reads comments through two repository-wide listings: about 40 requests for 30 open and 40 recently closed
pull requests, well inside the 1,000 requests an hour GitHub gives the workflow token (shared with CI).

GitHub runs scheduled workflows on a best-effort basis. A run can start late or be dropped under load, and schedules
pause after 60 days without repository activity. The status issue shows the time of its last run, so a stale page is
visible; run the workflow from the Actions tab to restart it.

**Only the owner's account counts.** Every agent posts through the owner's GitHub account. On this public
repository anyone else can comment, so `inbox.py` and the stall check ignore every other author. The one exception
is `github-actions[bot]`, and only for the stall check's own nudges, which carry its marker. A stranger can't address
an agent, fake a `Hold:`, or close a wait. Debates opened by anyone but the owner are not work. Titles shown in the
status issue are escaped, so a title can't mention people or inject links.

## When the owner overrides the process

The owner may tell any agent to skip any step: "skip the review and merge it now, it's urgent", "don't wait for
OpenCode", "decide without a debate". The owner's word wins, and these rules make an override visible, scoped and
followed up, so the other agents know it was intended and nothing skipped is forgotten.

1. **Record it before acting,** on the PR or issue it concerns, quoting the owner exactly:

   ```markdown
   To: all
   Owner said (in my session, 2026-10-29 10:15 UTC): "skip the review and merge it now, it's urgent"
   Override: independent review on #12
   Reason: urgent fix (owner's priority)
   Scope: this PR only
   Follow-up: post-merge review by opencode
   Withdrawn: opencode
   No reply needed.
   From: claude
   ```

   - **The owner's words are quoted, not paraphrased,** in quotation marks on the `Owner said` line. Nobody can
     stretch an override beyond what was said. Every agent posts through the owner's account, so the quote is what
     makes an override checkable: one without it is listed as **unverified** and does not unlock anything.
   - **`Withdrawn:`** names any agent the skipped step was waiting on, so that wait stops counting.
   - **`Override:` names the step skipped:** a review, a deadline, a debate's critiques, a hold.
   - **`Scope:` is one PR, issue or task.** An override never becomes a standing rule unless the owner says "from
     now on"; a standing rule is then written into this file in its own PR.
   - **`Follow-up:` says what replaces the skipped step later.** A skipped review becomes a post-merge review, and
     skipped critiques become critiques of the decision. If nothing replaces it, say "none, by the owner's
     decision". An override without a `Follow-up:` line is listed as having none stated.
   - **Only an override of the critiques** (its `Override:` line names the critiques, the debate or the decision)
     lets a debate decide before two critiques; an override of something else does not.
2. **The other agents accept it.** They do not undo, block or re-argue it. If they see a concrete risk, they post it
   once, with evidence, on the same PR, and carry on. An override is not a question to the owner.
3. **The follow-up is done** by whoever it names, who then posts `Follow-up done: <result>` on the same PR or issue
   (it closes the overrides recorded there before it). Findings from a post-merge review become a fix PR first in
   that agent's queue. Until then the status issue lists the override under "Overrides in effect", also after the PR
   merges. The stall check gives an item with a pending follow-up the `follow-up` label, keeps reading labelled items
   however long ago they closed, and removes the label once every follow-up on the item is done. A single
   `Follow-up done:` note closes all the overrides recorded before it on that item.
4. **What an override cannot do.**
   - The checks GitHub enforces (the ruleset's required checks, branch protection, and so the hot-file order and
     the commit trailers that `verify` checks) can't be skipped by an agent. Only the owner can do that, in GitHub's
     settings. To let a newer PR go first, the owner can tell an agent to close or park the older one instead.
   - The product invariants (no network, no telemetry, scoped storage, no bundled video decoder) can still be
     changed by the owner, but that is a product decision. It is recorded in the DEC register in the same PR, and
     the invariant tests are changed openly rather than skipped.
   - A stranger's comment cannot record one: only comments by the owner's account are read.

## Reviews when the reviewer is away

The independent reviewer is a role, not a particular agent. Take the first of these that is possible:

1. The agent that was asked, if it answers within 30 minutes (a review, or `Working:`).
2. Any other running agent whose model differs from the author's.
3. A review the author obtains from a different model:
   - a review subagent on another model;
   - a one-off session in another tool (Gemini CLI, Codex, OpenCode with another provider);
   - GitHub's Copilot code review on the PR.
4. If none of these is possible, the PR waits and the status issue shows it. The owner is never asked to review code.

Then post `Withdrawn: <the reviewer you first asked>` so the original request stops counting as a wait.

A review the author obtains (step 3) can be skewed by how it was asked, so it must be checkable:

- Give the reviewer only the diff, the PR description, `AGENTS.md` and
  [`.agents/rules/qa-and-code-review.md`](../.agents/rules/qa-and-code-review.md). Leave out the author's reasoning,
  so it judges the work rather than the story.
- Post on the PR, as one comment:
  - the exact instructions you gave it;
  - its model;
  - the commit it reviewed;
  - its full, unedited verdict.
- Fix its must-fix findings, then have a different model from yours re-check them on the new commit. That can be the
  same reviewer again.

A model from a **different vendor** is preferred. Two models from one family tend to share blind spots, and in
debate research, same-family agents copied each other's answers most of the time. For privacy invariants, Room
migrations and file deletion, the reviewer must be from a different vendor and run in its own session or tool, not
as the author's subagent. If none is available, those PRs wait. When the original reviewer comes back, it may still
review after the merge; its must-fix findings become a follow-up PR, first in its queue.

## Debates

A debate is how the agents decide something that has more than one reasonable answer and is expensive to get wrong.
Examples are an architecture or data-model choice, a change to this workflow, or anything touching hot files. Routine
fixes don't need one, and neither does a question that a test or a measurement can settle: run that instead.

The rules come from what goes wrong when language models debate:
- they drift toward the majority answer;
- long threads destabilize correct reasoning;
- accuracy often drops as rounds go on;
- majority votes discard correct minority answers.

See "Why Do Multi-Agent LLM Systems Fail?" (arXiv 2503.13657), "The Cost of Consensus" (arXiv 2605.00914) and
"Understanding Failure Modes in Multi-Agent Debate" (arXiv 2509.05396). So debates here are short, blind, mixed-model
and decided by evidence, never by vote.

1. **Open it.** Use an issue made with the **Debate** form and labelled `debate`. The owner opens it, or an agent opens
   it on the owner's request in its chat and says so in the issue.
2. **Proposal** (at most 500 words). One agent posts a comment with a line starting `Proposal:`. It covers:
   - the problem;
   - two or three options;
   - a recommendation, and the risks;
   - **what evidence would change its mind**.

   Its evidence is concrete: `file:line`, test output, measurements, or linked sources. Details go in links, not in
   the comment. A claim without evidence counts as an opinion.
3. **Critiques** (at most 350 words and three must-fix points each). At least **two** agents running models other
   than the proposer's each post a comment with a line starting `Critique:` that names the critic's model. At least
   one should be from a different vendor.
   - **Write your critique before reading the other critiques.** Independence is the point.
   - End with a line `Verdict: agree`, `Verdict: agree with changes` or `Verdict: object`. A critique without a clear
     verdict counts as an objection.
   - A critique that agrees still names at least one risk or missing scenario, so agreement is never a rubber stamp.
   - One model fills one critic slot, and the proposer cannot critique its own proposal.
   - Critiques are due 2 hours after the proposal; after that, use the fallback ladder above.
4. **Rebuttal.** The proposer answers once, in a comment with a line starting `Rebuttal:`, within 2 hours of the last
   critique.
   - For each must-fix point, it accepts the change or gives the evidence against it.
   - It states the revised plan.
   - It ends with a three-line summary of where the debate stands, so nobody (including the owner) has to read the
     whole thread.
5. **Decision.** Only the proposer, or an agent that took the debate over, posts a line starting `Decision:`, and only
   after two critiques (unless the owner overrode that step). It contains:
   - the plan;
   - the task list;
   - what was rejected, and why;
   - `Owner approval needed: yes/no`.

   Objections are weighed by their evidence, not counted. A single must-fix objection backed by evidence blocks the
   decision, and two agreements don't override it. An objection without evidence is recorded and does not block.
   Approval is needed when the plan changes what the user sees or does, or takes more than about a day of agent work.
6. **Limits.** A debate has two rounds of critique and rebuttal at most, and 24 hours in total. If an evidenced
   objection still stands after that, the proposer sends the owner a one-screen summary: the options, the strongest
   argument for each, and a recommendation. The owner picks, and that is final. The stall check flags a debate that
   passes its time box or its rounds, and proposals or critiques over their word limits.
7. **Approval.** The owner answers in any agent's chat. That agent posts it on the issue on a line starting
   `Owner said`, with the owner's words in quotation marks. The answer is the quote's first word: `approve`, `yes`
   or `go ahead` approves, while `reject` or `no` sends the decision back to the proposer. Anything else ("not
   yet") is shown to the owner as unclear rather than guessed. Until then the decision is listed under "Needs you"
   in the status issue.
8. **Build.** Each task in the decision becomes an issue or a checklist item. Agents claim and merge them under the
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
   `main`: `agent-ready` (owner-only signal), `task`, `debate`, `agent-status`, `follow-up` (set by the stall check),
   and `claimed:<tool>` for each tool.
   To add a tool, edit that file.
3. **Secret protection:** Settings → Code security → enable secret scanning and push protection.
4. **Agent tokens:** give each tool its own fine-grained token, scoped to this repository, with contents, pull
   request and issue write access, and **Actions read and write**, so every agent can re-run a failed CI job instead
   of waiting for the one that can. Grant no admin rights, so no agent can change the ruleset.
5. **Your status page:** the open issue labelled
   [`agent-status`](https://github.com/TejuBoricha/Aptura/issues?q=is%3Aissue+is%3Aopen+label%3Aagent-status).
   Read its "Needs you" line; nothing else on it needs you. GitHub also emails you each stall nudge on a pull
   request. To keep those out of your inbox, filter mail from `notifications@github.com` that contains
   `agent-status` and `Overdue:` into a folder, and keep mail about the `Agent status` issue.
6. **Optional, recommended later: a separate account for the agents.** Today every agent posts as you, so only the
   "Recorded as your words" list on the status page shows whether an `Owner said` line is really yours. A second,
   free GitHub account added as a collaborator for the agents would make your own comments the only ones that can
   carry your decisions. It needs your decision and setup; until then, that list is the check.

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
