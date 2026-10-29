#!/usr/bin/env python3
"""Find work that is waiting past its deadline, and say so where the agents will see it.

Every wait between agents has a deadline (docs/AGENT_WORKFLOW.md, "Nothing waits without a deadline"). This script
finds the ones that passed it on open pull requests and `debate` issues, so a stall is announced within minutes
instead of when the owner notices. The rules, in the order they apply to a `To:` message:

- `No reply needed` on a line of its own (outside quoted `>` lines) means the message is not a wait.
- The sender withdraws it with a line `Withdrawn: <agent>`, for example after using a fallback reviewer.
- Another agent's line `Taking over ... from <agent>` resolves the waits on that agent.
- The addressee's latest message decides: `Pausing:` makes the wait overdue at once (even if the pause came first);
  a `Working:` line holds the wait until its `done by HH:MM` time (UTC) plus --minutes, or for 2 hours when it names
  no time; any other reply to the sender or to all answers it. A push alone is not an answer: say so on the PR.
- Otherwise the wait is overdue --minutes (default 30) after the message. `To: owner` is never a wait: the owner is
  not on a clock. Names are the first word, so `From: OpenCode (DeepSeek V4)` is opencode.

A debate stalls when its critiques are overdue, the proposer's answer to them is overdue, it ran past its time box,
it went past two rounds, or a proposal or critique is over its word limit. A `Decision:` posted before two critiques
does not count unless the owner overrode that step. Owner overrides (`Override:` lines) are listed with their
follow-up until a `Follow-up done:` line. A `Hold:` line (the owner said wait) pauses
an item until a later `Hold lifted:` line; holds older than a day are listed for the owner.

    python3 tools/agents/stalls.py                 # print the report
    python3 tools/agents/stalls.py --nudge         # also post one `To: all` comment per stall (needs a write token)

The **Agent status** workflow runs it with --nudge every 15 minutes and keeps the report in the issue labelled
`agent-status`. A nudge is addressed to all, so local agents waiting in inbox.py and cloud agents subscribed to the
pull request wake up on it. Only comments by the owner's account count (inbox.trusted). Exit code: 0 (stalls are
reported, not failures), 1 when GitHub refuses the request (a missing permission must not turn the watch off
unnoticed), 0 with a warning during a GitHub outage. Standard library only.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone

import pr_checks
from inbox import Message, _login, every, force_utf8_output, recipients, sender, trusted, trusted_authors

BOT = "agent-status"
STATUS_LABEL = "agent-status"
DEBATE_LABEL = "debate"
MARKER = "<!-- stall:{key} -->"
PROPOSAL_WORDS = 500
CRITIQUE_WORDS = 350


def own_lines(body: str) -> list[str]:
    """The lines a comment says itself: quoted lines (`> ...`) repeat someone else and never count as markers."""
    return [line for line in (body or "").splitlines() if not line.lstrip().startswith(">")]


def has_line(body: str, word: str) -> bool:
    """True when one of the comment's own lines starts with `word:`, e.g. `Proposal:` or `Pausing:`."""
    pattern = re.compile(rf"^\s*{word}\s*:", re.IGNORECASE)
    return any(pattern.match(line) for line in own_lines(body))


def no_reply_needed(body: str) -> bool:
    return any(re.search(r"\bno reply needed\b", line, re.IGNORECASE) for line in own_lines(body))


def withdrawn(body: str) -> set[str]:
    """Agents named on a `Withdrawn: a, b` line."""
    names: set[str] = set()
    for line in own_lines(body):
        match = re.match(r"^\s*withdrawn\s*:\s*(.+)$", line, re.IGNORECASE)
        if match:
            names |= {n.lower() for n in re.findall(r"[\w-]+", match.group(1))}
    return names


def taken_over_from(body: str) -> set[str]:
    """Agents named in `Taking over #4 from opencode: ...`."""
    names = set()
    for line in own_lines(body):
        match = re.match(r"^\s*taking over\b.*?\bfrom\s+([\w-]+)", line, re.IGNORECASE)
        if match:
            names.add(match.group(1).lower())
    return names


def words(body: str) -> int:
    return len(" ".join(own_lines(body)).split())


@dataclass
class Item:
    """An open pull request or debate issue, with everything trusted said on it."""
    number: int
    title: str
    url: str
    is_debate: bool
    messages: list[Message]


@dataclass
class Stall:
    item: Item
    key: str      # identifies this stall, so it is announced once
    text: str


@dataclass
class Hold:
    since: datetime
    url: str


@dataclass
class Findings:
    stalls: list[Stall] = field(default_factory=list)
    needs_owner: list[str] = field(default_factory=list)
    overrides: list[str] = field(default_factory=list)


def current_hold(messages: list[Message]) -> Hold | None:
    """The owner's word wins: a `Hold:` line holds the item until a later `Hold lifted:` line."""
    hold = None
    for m in sorted(messages, key=lambda m: m.created_at):
        for line in own_lines(m.body):
            if re.match(r"^\s*hold\s+lifted\s*:", line, re.IGNORECASE):
                hold = None
            elif re.match(r"^\s*hold\s*:", line, re.IGNORECASE):
                hold = Hold(m.created_at, m.url)
    return hold


def working_until(body: str, posted: datetime, window: timedelta) -> datetime:
    """When a `Working:` note runs out: its `done by HH:MM` time (UTC, at most 12 hours away) plus the window, or
    2 hours after it was posted when it names no time."""
    for line in own_lines(body):
        match = re.match(r"^\s*working\s*:.*?\bby\s+(\d{1,2}):(\d{2})", line, re.IGNORECASE)
        if match and int(match.group(1)) < 24 and int(match.group(2)) < 60:
            due = posted.replace(hour=int(match.group(1)), minute=int(match.group(2)), second=0, microsecond=0)
            if due <= posted:
                due += timedelta(days=1)
            if due - posted <= timedelta(hours=12):
                return due + window
    return posted + timedelta(hours=2)


def unanswered(item: Item, now: datetime, window: timedelta) -> list[Stall]:
    """The oldest overdue `To:` message per addressee (see the module docstring for the rules)."""
    stalls, seen = [], set()
    ordered = sorted(item.messages, key=lambda m: m.created_at)
    for m in ordered:
        author = sender(m.body)
        # An acknowledgement (`Working:`) or a pause note answers someone; it never makes the other side wait.
        if author in (BOT, None) or no_reply_needed(m.body) or has_line(m.body, "working") or has_line(m.body, "pausing"):
            continue
        # The owner is not on a clock (an owner decision has no deadline), so `To: owner` is never a wait.
        for agent in sorted(recipients(m.body) - {"all", "owner", author}):
            if agent in seen:
                continue
            later = [x for x in ordered if x.created_at > m.created_at]
            if any(sender(x.body) == author and agent in withdrawn(x.body) for x in later):
                continue
            if any(sender(x.body) != agent and agent in taken_over_from(x.body) for x in later):
                continue
            from_agent = [x for x in ordered if sender(x.body) == agent]
            # A reply is addressed to the sender or to all; a `Working:` note counts however it is addressed.
            replies = [x for x in from_agent if x.created_at > m.created_at
                       and (author in recipients(x.body) or "all" in recipients(x.body) or has_line(x.body, "working"))]
            paused = bool(from_agent) and has_line(from_agent[-1].body, "pausing")
            working = not paused and bool(replies) and has_line(replies[-1].body, "working")
            if replies and not paused and not working:
                continue
            due = working_until(replies[-1].body, replies[-1].created_at, window) if working else m.created_at + window
            if not paused and now < due:
                continue
            seen.add(agent)
            anchor = replies[-1].created_at if working else m.created_at
            if paused:
                why = f"{agent} posted `Pausing` ({from_agent[-1].url})"
            elif working:
                why = f"{agent}'s `Working:` note ran out {_age(now - due)} ago with no answer since"
            else:
                why = f"no reply from {agent} for {_age(now - m.created_at)}"
            stalls.append(Stall(item, f"{m.url}#{agent}#{anchor:%Y%m%dT%H%M}",
                                f"{author}'s message to {agent} ({m.url}) is waiting: {why}. Another agent may take "
                                f"over after posting `Taking over ... from {agent}` (AGENTS.md rule 9); for a review, "
                                f"use the fallback reviewer and post `Withdrawn: {agent}` (docs/AGENT_WORKFLOW.md)."))
    return stalls


def verdict(body: str) -> str:
    """A critique's verdict: `agree`, `changes` or `object`, read from a `Verdict:` line, else from the `Critique:`
    line. An unclear verdict counts as an objection, so nothing is waved through by accident."""
    lines = own_lines(body)
    text = next((l for l in lines if re.match(r"^\s*verdict\s*:", l, re.IGNORECASE)),
                next((l for l in lines if re.match(r"^\s*critique\s*:", l, re.IGNORECASE)), "")).lower()
    if "object" in text:
        return "object"
    if "with changes" in text:
        return "changes"
    return "agree" if "agree" in text else "object"


def owner_answer(m: Message) -> str | None:
    """`approve` or `reject`, from a line starting `Owner said` recorded by the agent that heard the owner."""
    for line in own_lines(m.body):
        if re.match(r"^\s*owner said\b", line, re.IGNORECASE):
            low = line.lower()
            if re.search(r"\b(reject|rejected|don't|do not|not approved)\b", low):
                return "reject"
            if re.search(r"\b(approve|approved|go ahead|yes|agreed|pick|picks|choose|chose)\b", low):
                return "approve"
    return None


def debate(item: Item, now: datetime, step: timedelta, timebox: timedelta) -> Findings:
    """Overdue steps, broken limits, skipped critiques and owner approvals in a debate."""
    found = Findings()
    ordered = sorted(item.messages, key=lambda m: m.created_at)
    proposals = [m for m in ordered if has_line(m.body, "proposal")]
    if not proposals:
        return found
    proposal = proposals[-1]
    proposer = sender(proposal.body)
    after = [m for m in ordered if m.created_at > proposal.created_at]

    def stall(key: str, text: str):
        found.stalls.append(Stall(item, f"{proposal.url}#{key}", text))

    takeovers = {name for m in after for name in taken_over_from(m.body)}
    deciders = {proposer} | ({sender(m.body) for m in after if taken_over_from(m.body)} if proposer in takeovers else set())
    critiques = [m for m in after if has_line(m.body, "critique") and sender(m.body) not in (proposer, None)]
    decisions = [m for m in after if has_line(m.body, "decision") and sender(m.body) in deciders]
    overridden = any(has_line(m.body, "override") for m in ordered)
    if decisions:
        decision = decisions[-1]
        critics_before = {sender(c.body) for c in critiques if c.created_at < decision.created_at}
        if len(critics_before) < 2 and not overridden and decision.created_at - proposal.created_at < timebox:
            stall("skipped-critiques", f"a `Decision:` ({decision.url}) was posted with {len(critics_before)} of 2 "
                                       f"critiques. It does not count until two other models have critiqued the "
                                       f"proposal, or the owner overrides the step (`Override:`).")
            return found
        if re.search(r"owner approval needed\s*:\s*yes", " ".join(own_lines(decision.body)), re.IGNORECASE):
            answers = [a for m in ordered if m.created_at > decision.created_at and (a := owner_answer(m))]
            if not answers:
                found.needs_owner.append(f"[#{item.number}]({item.url}) decision waiting for your approval: "
                                         f"{decision.url}")
            elif answers[-1] == "reject":
                stall("rejected", f"the owner rejected the decision ({decision.url}): {proposer} revises the "
                                  f"proposal or closes the debate.")
        return found

    if words(proposal.body) > PROPOSAL_WORDS:
        stall("long-proposal", f"the proposal ({proposal.url}) is {words(proposal.body)} words, over the "
                               f"{PROPOSAL_WORDS}-word limit: shorten it and link the details.")
    for c in critiques:
        if words(c.body) > CRITIQUE_WORDS:
            found.stalls.append(Stall(item, f"{c.url}#long", f"the critique {c.url} is {words(c.body)} words, over "
                                      f"the {CRITIQUE_WORDS}-word limit: keep at most three must-fix points."))
    rebuttals = [m for m in after if has_line(m.body, "rebuttal") and sender(m.body) in deciders]
    if now - proposal.created_at > timebox:
        stall("timebox", f"the debate has run {_age(now - proposal.created_at)}, past its time box of "
                         f"{_age(timebox)}: {proposer} posts `Decision:`, or sends the owner a one-screen summary.")
    elif len(rebuttals) >= 2 and any(c.created_at > rebuttals[1].created_at and verdict(c.body) != "agree"
                                     for c in critiques):
        stall("rounds", f"two rounds are done and objections remain: {proposer} sends the owner a one-screen summary "
                        f"of the options with a recommendation; the owner picks.")
    critics = {sender(c.body) for c in critiques}
    if len(critics) < 2:
        if now - proposal.created_at >= step:
            stall("critiques", f"the proposal ({proposal.url}) has {len(critics)} of 2 critiques after "
                               f"{_age(now - proposal.created_at)}. Any agent running a model other than {proposer}'s "
                               f"may critique it; failing that, use the fallback ladder (docs/AGENT_WORKFLOW.md).")
    elif critiques:
        last = critiques[-1]
        answered = any(r.created_at > last.created_at for r in rebuttals)
        if not answered and now - last.created_at >= step:
            stall(f"rebuttal-{len(rebuttals)}", f"the critiques are in, but {proposer} has not posted `Rebuttal:` or "
                                                f"`Decision:` for {_age(now - last.created_at)}. Another agent may take "
                                                f"the debate over after posting `Taking over ... from {proposer}`.")
    return found


def overrides(item: Item) -> list[str]:
    """Owner overrides recorded on the item (`Override:`), and whether their follow-up is done (`Follow-up done:`)."""
    notes = []
    for m in sorted(item.messages, key=lambda m: m.created_at):
        lines = own_lines(m.body)
        for line in lines:
            match = re.match(r"^\s*override\s*:\s*(.+)$", line, re.IGNORECASE)
            if not match:
                continue
            follow = next((re.sub(r"^\s*follow-up\s*:\s*", "", l, flags=re.IGNORECASE) for l in lines
                           if re.match(r"^\s*follow-up\s*:", l, re.IGNORECASE)), "none stated")
            done = any(x.created_at > m.created_at and has_line(x.body, "follow-up done") for x in item.messages)
            notes.append(f"[#{item.number}]({item.url}) skipped {safe(match.group(1))} ({m.url}); follow-up: "
                         f"{safe(follow)}, {'done' if done else '**pending**'}")
    return notes


def find(items: list[Item], now: datetime, window: timedelta, step: timedelta, timebox: timedelta) -> Findings:
    found = Findings()
    for item in items:
        found.overrides += overrides(item)
        hold = current_hold(item.messages)
        if hold:
            if now - hold.since > timedelta(days=1):
                found.needs_owner.append(f"[#{item.number}]({item.url}) has been on hold for {_age(now - hold.since)} "
                                         f"({hold.url}): lift it or close the item.")
            continue
        found.stalls += unanswered(item, now, window)
        if item.is_debate:
            sub = debate(item, now, step, timebox)
            found.stalls += sub.stalls
            found.needs_owner += sub.needs_owner
    return found


def already_announced(item: Item, stall: Stall) -> bool:
    return any(MARKER.format(key=stall.key) in (m.body or "") for m in item.messages)


def nudge_body(stall: Stall) -> str:
    return f"To: all\n\nOverdue: {stall.text}\n\n{MARKER.format(key=stall.key)}\nFrom: {BOT}"


def safe(text: str) -> str:
    """A title anyone can write, made inert: no mentions that notify people, no links, no formatting."""
    text = re.sub(r"[\\`*_\[\]()<>#|!]", lambda m: "\\" + m.group(0), text)
    return text.replace("@", "@\u200b").replace("://", ":/\u200b/")


def report(items: list[Item], found: Findings, now: datetime) -> str:
    lines = [f"### Agent status ({now:%Y-%m-%d %H:%M} UTC)", "", "**Needs you:** " +
             ("nothing." if not found.needs_owner else ""), *[f"- {n}" for n in found.needs_owner], ""]
    if found.overrides:
        lines += ["**Overrides in effect** (steps skipped on the owner's instruction):",
                  *[f"- {o}" for o in found.overrides], ""]
    if not items:
        lines.append("No open pull requests or debates.")
    for item in items:
        mine = [s for s in found.stalls if s.item is item]
        kind = "Debate" if item.is_debate else "PR"
        state = "on hold" if current_hold(item.messages) else ("**stalled**" if mine else "moving")
        lines.append(f"- {kind} [#{item.number}]({item.url}) {safe(item.title)}: {state}")
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
             for c in every(f"{base}/issues/{number}/comments", token)]
    if is_pr:
        found += [Message("review", pr_checks._parse_time(r["submitted_at"]), r["html_url"], r["body"] or "",
                          _login(r))
                  for r in every(f"{base}/pulls/{number}/reviews", token) if r.get("submitted_at")]
        found += [Message("review comment", pr_checks._parse_time(c["created_at"]), c["html_url"], c["body"] or "",
                          _login(c))
                  for c in every(f"{base}/pulls/{number}/comments", token)]
    # A stranger's comment on this public repository must not create a wait, fake a `Hold:` or resolve one.
    return trusted(found, authors)


def fetch(repo: str, token: str | None, extra_trusted: list[str] | None = None) -> list[Item]:
    base = f"{pr_checks.API}/repos/{repo}"
    authors = trusted_authors(repo, extra_trusted)
    items = []
    for pr in every(f"{base}/pulls?state=open", token):
        items.append(Item(pr["number"], pr["title"], pr["html_url"], False,
                          _comments(base, pr["number"], token, True, authors)))
    for issue in every(f"{base}/issues?state=open&labels={DEBATE_LABEL}", token):
        # Anyone can open an issue with the Debate form; only the owner's (and so the agents') debates are work.
        if "pull_request" not in issue and _login(issue).lower() in authors:
            items.append(Item(issue["number"], issue["title"], issue["html_url"], True,
                              _comments(base, issue["number"], token, False, authors)))
    return items


def publish(repo: str, token: str, items: list[Item], found: Findings, text: str) -> int:
    """Post one nudge per new stall, and keep the report in the open issue labelled agent-status."""
    base = f"{pr_checks.API}/repos/{repo}"
    posted = 0
    for stall in found.stalls:
        if not already_announced(stall.item, stall):
            _send("POST", f"{base}/issues/{stall.item.number}/comments", token, {"body": nudge_body(stall)})
            posted += 1
    status = list(every(f"{base}/issues?state=open&labels={STATUS_LABEL}", token))
    body = (text + "\n_Updated by the Agent status workflow (`tools/agents/stalls.py`) about every 15 minutes; GitHub "
                   "may delay or skip a scheduled run. If the time above is hours old, run the workflow from the "
                   "Actions tab. Stalled items also get a `To: all` comment, which wakes the agents waiting on them._\n")
    if status:
        _send("PATCH", f"{base}/issues/{status[0]['number']}", token, {"body": body})
    else:
        _send("POST", f"{base}/issues", token, {"title": "Agent status", "body": body, "labels": [STATUS_LABEL]})
    return posted


def _age(delta: timedelta) -> str:
    minutes = int(delta.total_seconds() // 60)
    return f"{minutes // 60} h {minutes % 60:02d} min"


def main() -> int:
    force_utf8_output()
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", "TejuBoricha/Aptura"))
    parser.add_argument("--minutes", type=float, default=30, help="deadline for answering a To: message (default 30)")
    parser.add_argument("--debate-hours", type=float, default=2, help="deadline for each debate step (default 2)")
    parser.add_argument("--timebox-hours", type=float, default=24, help="time box for a whole debate (default 24)")
    parser.add_argument("--trust", nargs="*", default=[], help="extra GitHub logins whose comments count")
    parser.add_argument("--nudge", action="store_true", help="post the nudges and update the status issue")
    args = parser.parse_args()
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if args.nudge and not token:
        print("::error::--nudge needs GITHUB_TOKEN or GH_TOKEN with issues and pull-requests write access.")
        return 2
    now = datetime.now(timezone.utc)
    try:
        items = fetch(args.repo, token, args.trust)
        found = find(items, now, timedelta(minutes=args.minutes), timedelta(hours=args.debate_hours),
                     timedelta(hours=args.timebox_hours))
        text = report(items, found, now)
        print(text)
        if summary := os.environ.get("GITHUB_STEP_SUMMARY"):
            with open(summary, "a", encoding="utf-8") as handle:
                handle.write(text)
        if args.nudge:
            print(f"Posted {publish(args.repo, token, items, found, text)} new nudge(s).")
    except (urllib.error.URLError, TimeoutError) as error:
        if not pr_checks.is_outage(error):
            print(f"::error::Agent status could not use the GitHub API ({error}). Check the workflow's permissions.")
            return 1
        print(f"::warning::Agent status skipped: GitHub API unavailable ({error}). The next scheduled run retries.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
