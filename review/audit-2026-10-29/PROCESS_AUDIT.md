# Agent coordination process: end-to-end audit, 2026-10-29

**Scope.** This audit covers the multi-agent coordination process, not the Aptura app:
- the rules: `AGENTS.md`, `docs/AGENT_WORKFLOW.md` and `.agents/STATE.md`;
- the scripts: `tools/agents/` (`pr_checks.py`, `inbox.py`, `stalls.py`);
- the workflows: `ci.yml`, `agent-checks.yml`, `agent-status.yml` and `labels.yml`;
- the issue and PR templates;
- how the owner and each agent use all of it.

It was audited at three levels: technical implementation, solution architecture, and real-world use.

**Method.**
1. **Independent audit.** A separate model (Claude Sonnet, given only the repository and the owner's goals, not the
   author's reasoning) read every file. It ran the tests, wrote repros, applied 11 mutations to the safety-critical
   lines, and checked live repository data: PR comments, the ruleset, labels and workflow runs.
2. **Repro of every claim.** The author reproduced each claim before acting on it. All six repros of the key claims
   behaved exactly as reported.
3. **Live verification.** The Agent status workflow was run on `main` with the real workflow token. It created the
   status page ([#11](https://github.com/TejuBoricha/Aptura/issues/11)), and the Labels workflow created `debate`,
   `agent-status` and `follow-up`.
4. **Proof for every fix.** Each fix has a regression test, and each safety rule was mutation-tested: weakening or
   removing it makes a test fail.

**Result.** The design was sound, but four gaps would have hurt in real use:
- **The API budget:** at realistic scale the status check alone would use most of the hourly allowance.
- **The parser:** blind to common formatting, but fooled by examples in code blocks.
- **Stranger blocking:** strangers could block agent work.
- **Silent stalls:** stalls with no pending message went unnoticed.

All four are fixed, along with the medium and low findings below, except the items in "Owner decisions and accepted
limits".

| Severity | Found | Fixed in this PR | Owner decision / accepted limit |
|---|---|---|---|
| High | 7 | 6 | 1 (A1, partly mitigated) |
| Medium | 6 | 6 | 0 |
| Low | 2 (10 sub-items) | 6 sub-items | 4 sub-items |

## Findings

Severity: **H** high, **M** medium, **L** low. Level: tech, arch (architecture) or UX (owner and agents as users).

### T1 · tech · H · The API budget runs out at realistic scale

- **Impact:** The status check read 3 lists per item. With 30 open and 40 recently closed PRs, that is 215 requests
  a run, or 860 an hour, against the workflow token's 1,000 an hour, which CI shares. Once the budget is exhausted,
  `pr_checks.py` treats the rate limit as an outage. That makes the coordination checks pass silently exactly when
  activity is highest.
- **Root cause:** every run reread every item; there were no repository-wide reads.
- **Fix:** comments now come from two repository-wide listings (issue comments, and review comments since the
  oldest open item). Closed items skip reviews. The head commit is read only for a PR that has been quiet for
  4 hours.
- **Acceptance:** a fake API with 30 open and 40 closed PRs costs ≤ 40 requests a run, with the same findings.
  Measured: **37 a run, 148 an hour** (`test_the_api_budget_fits_at_scale`).
- **Status:** fixed.

### T2 · tech · H · `inbox.py` uses up the unauthenticated limit in 15 minutes

- **Impact:** each poll costs 4 requests, so 240 an hour, against GitHub's 60 an hour without a token. A local
  agent without `GITHUB_TOKEN` then printed "could not check" for hours while it believed it was waiting.
- **Root cause:** the script read only environment variables, and the docs said no token was needed.
- **Fix:**
  - fall back to `gh auth token` (most local agents are logged in);
  - with no token at all, check every 4 minutes and say why;
  - document it.
- **Acceptance:** `TokenTest`: the CLI fallback is used; without a token every sleep is 240 s.
- **Status:** fixed.

### T3 · tech · M-H · The parser reads the wrong things and misses the right ones

- **Impact:**
  - `Hold:`, `Override:` and `From:` inside code blocks counted. A docs example could freeze a PR or fake its
    sender.
  - `**To:** opencode`, `- Working: …` and `To: a and b` (common LLM formatting) were silently not messages.
  - Nothing told anyone a message was unreadable.
- **Root cause:** only `>` quotes were excluded; labels were matched literally; an unparsable message was dropped
  silently.
- **Fix:**
  - `message_lines()` skips fenced code, and undoes list bullets and bold;
  - `and` separates recipients;
  - a message whose `To:` is not first, or that lacks `From:` (or `To:`), is reported once as unreadable;
  - `stalls.py --explain` shows how a message will be read.
- **Acceptance:** `AuditParsingTest`: fenced markers ignored, formatting understood, unreadable messages reported.
- **Status:** fixed. One sub-item was resolved the other way: `No reply needed` stays valid anywhere in a message's
  own text, because agents write it mid-sentence, and the docs now say so.

### T4 · tech · M · Wait-state edge cases

- **Impact:**
  - A `Pausing:` posted after an answer reopened the answered question.
  - A `Working: … by 09:00 UTC` posted at 10:01 held the wait for 23 hours.
  - Any `Working:` note could hold a wait for 24.5 hours after its agent died.
- **Root cause:** the pause was checked before the reply; past times rolled to the next day; the cap was 24 hours.
- **Fix:**
  - a real reply answers for good, and a pause note is never an answer;
  - a time more than 8 hours away (including a time already past) falls back to 2 hours.
- **Acceptance:** `test_a_pause_after_an_answer_does_not_reopen_it`,
  `test_a_working_time_already_past_or_too_far_falls_back_to_two_hours`.
- **Status:** fixed. The cap is 8 hours rather than the auditor's 4, so overnight local work fits; agents can renew
  a note.

### T5 · tech · M · One failed write froze the status page

- **Impact:** a locked conversation or a label removed by hand made the first write fail. Every later nudge, every
  label change and the status update were then skipped on every run. Bodies over 65,536 characters would also
  fail.
- **Root cause:** writes had no per-write error handling and no size guard.
- **Fix:**
  - each write is retried on outages, and a failure is logged without stopping the rest;
  - the run exits 1 when anything failed;
  - the body is cut to 60,000 characters;
  - a `DELETE` of an already-gone label counts as done.
- **Acceptance:** `test_a_failed_write_does_not_stop_the_others`, `test_the_status_body_stays_under_githubs_limit`.
- **Status:** fixed.

### T6 · tech · L · Small correctness and portability items

- **Fixed:**
  - "agreed" was read as an objection;
  - Windows uses `py -3`, not `python3`;
  - the minimum Python version (3.9) was unstated.
- **Accepted limits:** the two items under "Owner decisions and accepted limits" (critic slots per name, and an
  owner-to-organisation move).
- **Acceptance:** `test_agreed_is_agreement`; docs state the Python version and the Windows command.
- **Status:** fixed / accepted.

### A1 · arch · H · One identity: owner approvals are forgeable

- **Impact:** every agent posts as the owner. A confused or prompt-injected agent can write `Owner said: "approve"`,
  an `Override:` or `Hold lifted:`, and the checks accept it.
- **Root cause:** a single GitHub account for the owner and every agent.
- **Fix (partial, in this PR):** a detective control. The status page lists every `Owner said` line of the last
  week (who recorded it, and where) under "Recorded as your words", so the owner can spot words they never said.
  Overrides were already listed, and unquoted ones marked unverified.
- **Full fix (owner decision):** a separate, free GitHub account for the agents, added as a collaborator. Then only
  the owner's own comments can carry the owner's decisions, and the checks can verify it. Documented in the owner
  setup checklist (item 6).
- **Acceptance (partial):** `test_words_recorded_in_the_owners_name_are_listed_for_the_owner`.
- **Status:** mitigated; the rest awaits the owner.

### A2 · arch · H · A stranger can block agent work

- **Impact:** on this public repository, one stranger's older PR touching a hot file or citing `Fixes #41` blocked
  agents' PRs. Their activity kept it from ever going stale.
- **Root cause:** `pr_checks.py` compared against all open PRs, regardless of author.
- **Fix:**
  - only PRs opened by the owner's account (every agent) or a bot block; others warn;
  - the stall check ignores PRs not opened by the owner's account.
- **Acceptance:** `test_a_strangers_older_pull_request_never_blocks`; the fetch test ignores a stranger's and a
  Dependabot PR.
- **Status:** fixed.

### A3 · arch · H · "No stalls" covered only waits with a pending message

- **Impact:** a task claimed by an agent that then stopped kept its `claimed:` label forever, and the rules tell
  others to pick another task. A driver that stopped mid-task without a message left a PR marked "moving".
- **Root cause:** the stall check looked only at `To:` messages and debates.
- **Fix:**
  - a claim with no PR mentioning it after 2 hours is a stall;
  - a PR with no message or commit for 4 hours is a stall;
  - any announced stall left for a day goes under "Needs you".
- **Acceptance:** `test_a_claimed_task_without_a_pull_request_is_a_stall_after_two_hours`,
  `test_a_quiet_pull_request_is_a_stall_after_four_hours_counting_commits`,
  `test_a_stall_nobody_resolves_for_a_day_goes_to_the_owner`.
- **Status:** fixed. A blocked PR does not need a re-run trigger: `main` requires branches to be up to date, so the
  blocked PR must rebase after the blocker merges, and that push re-runs the check (now in the docs).

### A4 · arch · M · Contradictions and self-judging rules

- **Impact:**
  - The docs offered an override of "hot-file order", which CI makes impossible.
  - The docs told agents to "ask the owner to sequence the work", against "owner only for decisions".
  - A PR that edited `pr_checks.py` was judged by its own edited copy, so it could switch its own checks off.
  - The volatile lines in `STATE.md` (test counts, the verified SHA) went false on almost every PR. Since
    `STATE.md` is a hot file, that serialized most PRs.
  - Whether a rebase needs a fresh review was undefined.
- **Root cause:** the rules grew in several passes; CI ran the checker from the PR's checkout.
- **Fix:**
  - the impossible override is removed, and how to let a newer PR go first is explained;
  - sequencing goes through a `To:` message;
  - CI and Agent checks run `pr_checks.py` and `hot-files.txt` from the base branch;
  - `STATE.md` keeps only lasting facts;
  - a rebase without conflict resolution keeps the approval.
- **Acceptance:** the base-branch copy was run against a real PR (all clear, exit 0); the docs list no step that
  cannot be performed.
- **Status:** fixed.

### A5 · arch · L · Smaller architecture notes

- **Fixed:**
  - Gemini had no claim label;
  - the docs' agent names and the labels could drift apart (now a test);
  - the deadlines in the docs could drift from the code (now a test).
- **Accepted limits:** the two A5 items under "Owner decisions and accepted limits" (scheduled runs are best effort,
  and actions are pinned by tag).
- **Status:** fixed / accepted.

### U1 · UX · H · The rules are heavy and easy to mis-format

- **Impact:** about 8,700 words to read, and about 22 markers. Smaller models will skip or mis-format markers.
- **Fix:**
  - a six-line "Everyday messages" sheet in `AGENTS.md`, with debates, holds and overrides kept for when they are
    needed;
  - `--explain` to check a message before posting;
  - unreadable messages are now reported (T3).
- **Acceptance:** `test_the_cheat_sheet_says_what_the_parser_reads` parses the sheet with the real parser.
- **Status:** fixed.

### U2 · UX · H · The owner had to go and look

- **Impact:** editing the status page notifies nobody, so a decision only the owner can make could wait unseen. Bot
  nudges on PRs, meanwhile, do email the owner.
- **Fix:**
  - a new "Needs you" entry posts one comment on the status issue, which GitHub emails;
  - agent-only stalls never do;
  - the status page is linked from `STATE.md`, `CONTRIBUTING.md` and the owner checklist;
  - a mail-filter tip keeps nudge emails out of the inbox.
- **Acceptance:** `test_a_new_needs_you_entry_notifies_the_owner_once`.
- **Status:** fixed.

### U3 · UX · M · Noise

- **Impact:** several stalls on one item meant several comments, and several notifications.
- **Fix:** one nudge comment per item per run, listing every new stall.
- **Acceptance:** `test_several_new_stalls_on_one_item_make_one_comment`.
- **Status:** fixed.

### U4 · UX · M · Onboarding and recovery

- **Impact:**
  - A tool that adds only its own `Co-Authored-By:` failed CI, and the fix (reword and force-push) is beyond a
    non-technical owner.
  - Gemini CLI had no pointer to `AGENTS.md`.
- **Fix:**
  - a `Co-Authored-By:` line naming an AI tool passes with a warning (a person as co-author still does not count);
  - the error message says how to reword;
  - `GEMINI.md` imports `AGENTS.md`.
- **Acceptance:** `test_a_tools_co_authored_by_line_passes_with_a_warning`; the existing test that a human
  co-author alone fails still passes.
- **Status:** fixed.

## Owner decisions and accepted limits

- **A1, a separate agent account:** recommended. It is the only way to make the owner's decisions unforgeable, and
  it needs the owner to create the account and grant access. Until then, the "Recorded as your words" list is the
  check.
- **Critic slots are counted per agent name, not per model:** nothing in a comment proves which model wrote it. The
  rule "one model fills one critic slot" stays written, not checked.
- **An owner-to-organisation move would make every comment untrusted:** trust is the owner's login. Moving the
  repository to an organisation needs `--trust <login>` in the workflow; this is noted, and no move is planned.
- **Scheduled runs are best effort:** GitHub may delay or drop them, and pauses them after 60 days without activity.
  The status page shows its last run time; an external scheduler would be the next step if that proves unreliable.
- **Actions are pinned by tag, not commit SHA:** a low risk here, because no workflow handles secrets. Revisit if
  secrets are added.
- **Unverified:** whether a `github-actions[bot]` nudge wakes a subscribed cloud agent. This PR deliberately leaves
  a request to OpenCode (offline) unanswered, so the next run posts a real nudge, and the author records whether it
  arrived.

## What is solid and should not change

- The trust rule (the owner's account, plus the stall check's own marked nudges) and the escaping of titles.
- No PR or comment text reaches a shell in any workflow.
- Permission errors fail loudly; real outages are skipped with a warning; truncated threads refuse to be read.
- Nudges are de-duplicated by marker, runs are serialized, and the hot-file list has a drift test.
- The `main` ruleset (required checks on an up-to-date branch, linear history), and merging with the checked head
  SHA.
- The deadline-and-fallback model, `Working:` / `Withdrawn:` / `Taking over`, and the evidence-based debate limits.
- Independent review: every review round on this process found real defects the author had missed. It stays
  mandatory.
