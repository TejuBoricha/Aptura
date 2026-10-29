#!/usr/bin/env python3
"""Find work that is waiting past its deadline, and say so where the agents will see it.

Every wait between agents has a deadline (docs/AGENT_WORKFLOW.md, "Nothing waits without a deadline"). This script
finds the ones that have passed it, so a stall is announced within the hour instead of when the owner notices:

- a `To:` message on an open pull request or `debate` issue that its addressee has not answered, by a reply or by a
  commit naming its tool in `Assisted-by:`, within --hours (default 2), or that the addressee answered with `Pausing`.
  Every addressed message is a wait unless it says "No reply needed", so a forgotten wait is reported, not missed. A
  `Working:` note from the addressee restarts the deadline without ending the wait;
- a `debate` issue whose `Proposal:` has fewer than two critiques after --hours, or whose critiques have had no
  `Rebuttal:` or `Decision:` for --hours.

A `Hold:` line (the owner said wait) pauses the checks on that item until a later `Hold lifted` line.

    python3 tools/agents/stalls.py                 # print the report
    python3 tools/agents/stalls.py --nudge         # also post one `To: all` comment per stall (needs a write token)

The hourly `Agent status` workflow runs it with --nudge and keeps the report in the issue labelled `agent-status`.
A nudge is addressed to all, so local agents waiting in inbox.py and cloud agents subscribed to the pull request wake
up on it. Exit code: 0 (stalls are reported, not failures), 1 when GitHub refuses the request (a missing permission
should not turn the watch off unnoticed), 0 with a warning during a GitHub outage. Standard library only.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

import pr_checks
from inbox import Message, _login, force_utf8_output, recipients, sender, trusted, trusted_authors

BOT = "agent-status"
STATUS_LABEL = "agent-status"
DEBATE_LABEL = "debate"
MARKER = "<!-- stall:{key} -->"
ASSISTED_BY = re.compile(r"^assisted-by:\s*(.+)$", re.IGNORECASE | re.MULTILINE)
NO_REPLY = re.compile(r"\bno reply needed\b", re.IGNORECASE)  # "Merged #4. No reply needed." is not a wait
TAKING_OVER = re.compile(r"^\s*taking over\b", re.IGNORECASE | re.MULTILINE)  # "Taking over #4 from opencode: ..."
HOLD = re.compile(r"^\s*hold(\s+lifted)?\s*:", re.IGNORECASE | re.MULTILINE)


def has_line(body: str, word: str) -> bool:
    """True when a line of `body` starts with `word:`, e.g. `Proposal:` or `Pausing:`."""
    return re.search(rf"^\s*{word}\s*:", body or "", re.IGNORECASE | re.MULTILINE) is not None


@dataclass
class Commit:
    at: datetime
    message: str

    def by(self, agent: str) -> bool:
        return any(agent in line.lower() for line in ASSISTED_BY.findall(self.message))


@dataclass
class Item:
    """An open pull request or debate issue, with everything said and pushed on it."""
    number: int
    title: str
    url: str
    is_debate: bool
    messages: list[Message]
    commits: list[Commit]


@dataclass
class Stall:
    item: Item
    key: str      # identifies this stall, so it is announced once
    text: str


def on_hold(messages: list[Message]) -> bool:
    """The owner's word wins: a `Hold:` line holds the item until a later `Hold lifted` line."""
    held = False
    for m in sorted(messages, key=lambda m: m.created_at):
        for match in HOLD.finditer(m.body or ""):
            held = match.group(1) is None
    return held


def unanswered(item: Item, now: datetime, window: timedelta) -> list[Stall]:
    """The oldest unanswered `To:` message per addressee, once its deadline has passed or the addressee paused."""
    stalls, seen = [], set()
    ordered = sorted(item.messages, key=lambda m: m.created_at)
    for m in ordered:
        author = sender(m.body)
        if author == BOT or NO_REPLY.search(m.body or ""):
            continue
        for agent in sorted(recipients(m.body) - {"all", author}):
            if agent in seen:
                continue
            later = [x for x in ordered if x.created_at > m.created_at and sender(x.body) == agent]
            pushed = any(c.at > m.created_at and c.by(agent) for c in item.commits)
            taken_over = any(x.created_at > m.created_at and TAKING_OVER.search(x.body or "") for x in ordered)
            # Only the addressee's latest word counts. `Pausing` makes the wait overdue at once; `Working:` (an
            # acknowledgement, "reviewing, done by 15:00") restarts the deadline but does not end the wait, so an
            # agent that says "on it" and then stops is still caught. Anything else from it is an answer.
            last = later[-1] if later else None
            paused = last is not None and has_line(last.body, "pausing")
            working = last is not None and has_line(last.body, "working")
            if (last and not paused and not working) or pushed or taken_over:
                continue
            age = now - (last.created_at if working else m.created_at)
            if not paused and age < window:
                continue
            seen.add(agent)
            if paused:
                why = f"{agent} posted `Pausing`"
            elif working:
                why = f"{agent}'s `Working:` note is {_hours(age)} old with no answer or push since"
            else:
                why = f"no reply or push from {agent} for {_hours(age)}"
            stalls.append(Stall(item, f"{m.url}#{agent}",
                                f"{author or 'someone'}'s message to {agent} ({m.url}) is waiting: {why}. Another "
                                f"agent may take over after posting `Taking over` (AGENTS.md rule 9); for a review, "
                                f"use the fallback reviewer (docs/AGENT_WORKFLOW.md)."))
    return stalls


def debate_stalls(item: Item, now: datetime, window: timedelta) -> list[Stall]:
    """A debate stalls when critiques or the proposer's answer to them are overdue."""
    ordered = sorted(item.messages, key=lambda m: m.created_at)
    if any(has_line(m.body, "decision") for m in ordered):
        return []
    proposals = [m for m in ordered if has_line(m.body, "proposal")]
    if not proposals:
        return []
    proposal = proposals[-1]
    proposer = sender(proposal.body)
    critiques = [m for m in ordered if m.created_at > proposal.created_at and has_line(m.body, "critique")]
    critics = {sender(m.body) for m in critiques} - {proposer, None}
    if len(critics) < 2:
        if now - proposal.created_at < window:
            return []
        return [Stall(item, f"{proposal.url}#critiques",
                      f"the proposal ({proposal.url}) has {len(critics)} of 2 critiques after "
                      f"{_hours(now - proposal.created_at)}. Any agent running a model other than {proposer or 'the proposer'}"
                      f" may critique it; failing that, the proposer gets a critique from another model "
                      f"(docs/AGENT_WORKFLOW.md, \"Debates\").")]
    last = critiques[-1]
    answered = any(m.created_at > last.created_at and has_line(m.body, "rebuttal") for m in ordered)
    if answered or now - last.created_at < window:
        return []
    return [Stall(item, f"{last.url}#rebuttal",
                  f"the critiques are in, but {proposer or 'the proposer'} has not posted `Rebuttal:` or `Decision:` "
                  f"for {_hours(now - last.created_at)}. Another agent may take the debate over after posting "
                  f"`Taking over`.")]


def find_stalls(items: list[Item], now: datetime, window: timedelta) -> list[Stall]:
    stalls = []
    for item in items:
        if on_hold(item.messages):
            continue
        stalls += unanswered(item, now, window)
        if item.is_debate:
            stalls += debate_stalls(item, now, window)
    return stalls


def already_announced(item: Item, stall: Stall) -> bool:
    return any(MARKER.format(key=stall.key) in (m.body or "") for m in item.messages)


def nudge_body(stall: Stall) -> str:
    return f"To: all\n\nOverdue: {stall.text}\n\n{MARKER.format(key=stall.key)}\nFrom: {BOT}"


def report(items: list[Item], stalls: list[Stall], now: datetime) -> str:
    lines = [f"### Agent status ({now:%Y-%m-%d %H:%M} UTC)", ""]
    if not items:
        lines.append("No open pull requests or debates.")
    for item in items:
        mine = [s for s in stalls if s.item is item]
        kind = "Debate" if item.is_debate else "PR"
        state = "held by the owner" if on_hold(item.messages) else ("**stalled**" if mine else "moving")
        lines.append(f"- {kind} [#{item.number}]({item.url}) {item.title}: {state}")
        lines += [f"  - {s.text}" for s in mine]
    return "\n".join(lines) + "\n"


# ---- GitHub I/O -------------------------------------------------------------------------------------------------

def _send(method: str, url: str, token: str, payload: dict):
    request = urllib.request.Request(url, method=method, data=json.dumps(payload).encode("utf-8"), headers={
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "aptura-stalls",
        "Authorization": f"Bearer {token}",
    })
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def _comments(base: str, number: int, token: str | None, is_pr: bool, authors: set[str]) -> list[Message]:
    found = [Message("comment", pr_checks._parse_time(c["created_at"]), c["html_url"], c["body"] or "", _login(c))
             for c in pr_checks._pages(f"{base}/issues/{number}/comments", token, limit=5)]
    if is_pr:
        found += [Message("review", pr_checks._parse_time(r["submitted_at"]), r["html_url"], r["body"] or "",
                          _login(r))
                  for r in pr_checks._pages(f"{base}/pulls/{number}/reviews", token, limit=5) if r.get("submitted_at")]
        found += [Message("review comment", pr_checks._parse_time(c["created_at"]), c["html_url"], c["body"] or "",
                          _login(c))
                  for c in pr_checks._pages(f"{base}/pulls/{number}/comments", token, limit=5)]
    # A stranger's comment on this public repository must not create a wait, fake a `Hold:` or resolve one.
    return trusted(found, authors)


def fetch(repo: str, token: str | None) -> list[Item]:
    base = f"{pr_checks.API}/repos/{repo}"
    authors = trusted_authors(repo)
    items = []
    for pr in pr_checks._pages(f"{base}/pulls?state=open", token, limit=3):
        commits = [Commit(pr_checks._parse_time(c["commit"]["committer"]["date"]), c["commit"]["message"])
                   for c in pr_checks._pages(f"{base}/pulls/{pr['number']}/commits", token, limit=3)]
        items.append(Item(pr["number"], pr["title"], pr["html_url"], False,
                          _comments(base, pr["number"], token, True, authors), commits))
    for issue in pr_checks._pages(f"{base}/issues?state=open&labels={DEBATE_LABEL}", token, limit=3):
        # Anyone can open an issue with the Debate form; only the owner's (and so the agents') debates are work.
        if "pull_request" not in issue and _login(issue).lower() in authors:
            items.append(Item(issue["number"], issue["title"], issue["html_url"], True,
                              _comments(base, issue["number"], token, False, authors), []))
    return items


def publish(repo: str, token: str, items: list[Item], stalls: list[Stall], text: str) -> int:
    """Post one nudge per new stall, and keep the report in the open issue labelled agent-status."""
    base = f"{pr_checks.API}/repos/{repo}"
    posted = 0
    for stall in stalls:
        if not already_announced(stall.item, stall):
            _send("POST", f"{base}/issues/{stall.item.number}/comments", token, {"body": nudge_body(stall)})
            posted += 1
    status = list(pr_checks._pages(f"{base}/issues?state=open&labels={STATUS_LABEL}", token, limit=1))
    body = (text + "\n_Updated every hour by the Agent status workflow (`tools/agents/stalls.py`). Stalled items also "
                   "get a `To: all` comment, which wakes the agents waiting on them._\n")
    if status:
        _send("PATCH", f"{base}/issues/{status[0]['number']}", token, {"body": body})
    else:
        _send("POST", f"{base}/issues", token, {"title": "Agent status", "body": body, "labels": [STATUS_LABEL]})
    return posted


def _hours(delta: timedelta) -> str:
    minutes = int(delta.total_seconds() // 60)
    return f"{minutes // 60} h {minutes % 60:02d} min"


def main() -> int:
    force_utf8_output()
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", "TejuBoricha/Aptura"))
    parser.add_argument("--hours", type=float, default=2.0, help="the deadline for a reply (default 2)")
    parser.add_argument("--nudge", action="store_true", help="post the nudges and update the status issue")
    args = parser.parse_args()
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if args.nudge and not token:
        print("::error::--nudge needs GITHUB_TOKEN or GH_TOKEN with issues and pull-requests write access.")
        return 2
    now = datetime.now(timezone.utc)
    try:
        items = fetch(args.repo, token)
        stalls = find_stalls(items, now, timedelta(hours=args.hours))
        text = report(items, stalls, now)
        print(text)
        if summary := os.environ.get("GITHUB_STEP_SUMMARY"):
            with open(summary, "a", encoding="utf-8") as handle:
                handle.write(text)
        if args.nudge:
            print(f"Posted {publish(args.repo, token, items, stalls, text)} new nudge(s).")
    except (urllib.error.URLError, TimeoutError) as error:
        if not pr_checks.is_outage(error):
            print(f"::error::Agent status could not use the GitHub API ({error}). Check the workflow's permissions.")
            return 1
        print(f"::warning::Agent status skipped: GitHub API unavailable ({error}). The next hourly run retries.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
