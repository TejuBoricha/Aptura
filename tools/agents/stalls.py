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
follow-up until a `Follow-up done:` line; an item with a pending follow-up gets the `follow-up` label, which keeps it
read after it closes, at any age, and is removed when the follow-up is done. A `Hold:` line (the owner said wait) pauses
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
from inbox import (Message, _login, every, force_utf8_output, github_token, malformed, message_lines, recipients,
                   sender, trusted, trusted_authors)

BOT = "agent-status"
STATUS_LABEL = "agent-status"
DEBATE_LABEL = "debate"
FOLLOW_UP_LABEL = "follow-up"
MARKER = "<!-- stall:{key} -->"
PROPOSAL_WORDS = 500
CRITIQUE_WORDS = 350
CLOSED_DAYS = 14
READ_DAYS = 90           # one repo-wide read of comments covers items opened in this window; older ones are read alone
WORKING_CAP = timedelta(hours=8)
IDLE = timedelta(hours=4)
CLAIM_WITHOUT_PR = timedelta(hours=2)
ESCALATE = timedelta(days=1)
BODY_LIMIT = 60_000      # GitHub rejects issue bodies over 65,536 characters


def own_lines(body: str) -> list[str]:
    """The lines a comment says itself (inbox.message_lines): no quotes, no code blocks, list and bold marks undone."""
    return message_lines(body)


def has_line(body: str, word: str) -> bool:
    """True when one of the comment's own lines starts with `word:`, e.g. `Proposal:` or `Pausing:`."""
    pattern = re.compile(rf"^\s*{word}\s*:", re.IGNORECASE)
    return any(pattern.match(line) for line in own_lines(body))


def no_reply_needed(body: str) -> bool:
    """`No reply needed` at the start of a line or as its own sentence ("Merged. No reply needed."), not inside one
    ("this rule is not a no reply needed case"), which would silence a real wait."""
    return any(re.search(r"(^|[.!?:]\s+)no reply needed\b", line.strip(), re.IGNORECASE) for line in own_lines(body))


NEEDS = "<!-- needs:{key} -->"


def needs(key: str, text: str) -> str:
    """A "Needs you" entry. The key, hidden in the page, stays the same while the visible text (an age) changes, so
    the owner is told about each entry once, not at every run."""
    return f"{text} {NEEDS.format(key=key)}"


def is_wait(body: str) -> bool:
    """Whether an addressed message waits for an answer. Acknowledgements (`Working:`) and pause notes answer someone
    and never make the other side wait, and `No reply needed` opts out."""
    return not (no_reply_needed(body) or has_line(body, "working") or has_line(body, "pausing"))


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
    is_open: bool = True
    labels: set[str] = field(default_factory=set)
    kind: str = ""                      # "PR", "Debate" or "Task"; derived when empty
    claimed_at: datetime | None = None  # a claimed task issue: when it was claimed
    linked: bool = False                # a claimed task issue: an open pull request mentions it
    idle_since: datetime | None = None  # an open pull request: last trusted message or commit

    def __post_init__(self):
        self.kind = self.kind or ("Debate" if self.is_debate else "PR")

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
    label: set[int] = field(default_factory=set)    # items to give the follow-up label
    unlabel: set[int] = field(default_factory=set)  # items whose follow-ups are all done
    owner_words: list[str] = field(default_factory=list)


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
    """When a `Working:` note runs out: its `by HH:MM UTC` time plus the window, or 2 hours after it was posted when it
    names no time in UTC. Other formats (2:30pm, CET) are ignored rather than misread. A time more than WORKING_CAP
    away, which is also how a time already past reads ("by 09:00" posted at 10:01), falls back to 2 hours: a note
    must not hold a wait for a day after its agent stopped."""
    for line in own_lines(body):
        match = re.match(r"^\s*working\s*:.*\bby\s+(\d{1,2}):(\d{2})\s*(?:utc|z)\b", line, re.IGNORECASE)
        if match and int(match.group(1)) < 24 and int(match.group(2)) < 60:
            due = posted.replace(hour=int(match.group(1)), minute=int(match.group(2)), second=0, microsecond=0)
            if due <= posted:
                due += timedelta(days=1)
            if due - posted <= WORKING_CAP:
                return due + window
    return posted + timedelta(hours=2)


def unanswered(item: Item, now: datetime, window: timedelta) -> list[Stall]:
    """The oldest overdue `To:` message per addressee (see the module docstring for the rules)."""
    stalls, seen = [], set()
    ordered = sorted(item.messages, key=lambda m: m.created_at)
    for m in ordered:
        author = sender(m.body)
        if author in (BOT, None) or not is_wait(m.body):
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
            after_m = [x for x in from_agent if x.created_at > m.created_at]
            # A reply is addressed to the sender or to all. A `Working:` note counts however it is addressed, but only
            # holds the wait; it never reopens one that a real reply already answered.
            notes = [x for x in after_m if has_line(x.body, "working")]
            replies = [x for x in after_m if x not in notes and not has_line(x.body, "pausing")
                       and (author in recipients(x.body) or "all" in recipients(x.body))]
            if replies:
                continue  # answered; a later pause concerns later waits only
            paused = bool(from_agent) and has_line(from_agent[-1].body, "pausing")
            working = not paused and bool(notes)
            due = working_until(notes[-1].body, notes[-1].created_at, window) if working else m.created_at + window
            if not paused and now < due:
                continue
            seen.add(agent)
            anchor = notes[-1].created_at if working else m.created_at
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
    stated = next((l for l in lines if re.match(r"^\s*verdict\s*:", l, re.IGNORECASE)), None)
    if stated is not None:  # the `Verdict:` line starts with the verdict itself
        text = stated.split(":", 1)[1].strip().lower()
        if re.match(r"agree with changes\b", text):
            return "changes"
        return "agree" if re.match(r"agreed?\b", text) else "object"
    text = next((l for l in lines if re.match(r"^\s*critique\s*:", l, re.IGNORECASE)), "").lower()
    if re.search(r"\b(object|objects|objection|disagree|disagrees)\b|(?:\bnot|n't)\s+agree\b", text):
        return "object"
    if re.search(r"\bagree with changes\b", text):
        return "changes"
    return "agree" if re.search(r"\bagreed?\b", text) else "object"


def owner_answer(m: Message) -> str | None:
    """The owner's answer to a decision, from a line starting `Owner said` that quotes it: `approve` when the quote
    starts with approve, approved, yes or go ahead; `reject` when it starts with reject, rejected or no; `unclear`
    otherwise, which is shown to the owner instead of guessed. An override is not an answer to a decision."""
    lines = own_lines(m.body)
    if any(re.match(r"^\s*override\s*:", l, re.IGNORECASE) for l in lines):
        return None
    for line in lines:
        if not re.match(r"^\s*owner said\b", line, re.IGNORECASE):
            continue
        quote = re.search(r"[\"“](.+?)[\"”]", line)
        text = (quote.group(1) if quote else line.split(":", 1)[-1]).strip().lower()
        if re.match(r"(approve|approved|yes|go ahead)\b", text):
            return "approve"
        if re.match(r"(reject|rejected|no)\b", text):
            return "reject"
        return "unclear"
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
    takers = {sender(m.body) for m in after if taken_over_from(m.body)}
    deciders = {proposer} | (takers if proposer in takeovers else set())
    critiques = [m for m in after if has_line(m.body, "critique") and sender(m.body) not in (proposer, None)]
    decisions = [m for m in after if has_line(m.body, "decision") and sender(m.body) in deciders]
    if decisions:
        decision = decisions[-1]
        overridden = any(m.created_at <= decision.created_at and is_verified_override(m)
                         and re.search(r"\b(critiques?|debate|decision)\b", override_step(m) or "", re.IGNORECASE)
                         for m in ordered)
        critics_before = {sender(c.body) for c in critiques if c.created_at < decision.created_at}
        if len(critics_before) < 2 and not overridden and decision.created_at - proposal.created_at < timebox:
            stall("skipped-critiques", f"a `Decision:` ({decision.url}) was posted with {len(critics_before)} of 2 "
                                       f"critiques. It does not count until two other models have critiqued the "
                                       f"proposal, or the owner overrides the step (`Override:`).")
            return found
        if re.search(r"owner approval needed\s*:\s*yes", " ".join(own_lines(decision.body)), re.IGNORECASE):
            answers = [a for m in ordered if m.created_at > decision.created_at and (a := owner_answer(m))]
            if not answers:
                found.needs_owner.append(needs(f"approve#{decision.url}", f"[#{item.number}]({item.url}) decision "
                                                               f"waiting for your approval: {decision.url}"))
            elif answers[-1] == "unclear":
                found.needs_owner.append(needs(f"unclear#{decision.url}", f"[#{item.number}]({item.url}) your answer "
                                                               f"to the decision ({decision.url}) was recorded but is "
                                                               f"unclear: say approve or reject."))
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
                                                f"`Decision:` for {_age(now - last.created_at)}. Another agent may "
                                                f"take "
                                                f"the debate over after posting `Taking over ... from {proposer}`.")
    return found


def override_step(m: Message) -> str | None:
    return next((re.sub(r"^\s*override\s*:\s*", "", l, flags=re.IGNORECASE) for l in own_lines(m.body)
                 if re.match(r"^\s*override\s*:", l, re.IGNORECASE)), None)


def is_verified_override(m: Message) -> bool:
    """An override counts only with the owner's words quoted on an `Owner said` line in the same comment. Every
    agent posts through the owner's account, so the quote is what makes an override checkable."""
    return override_step(m) is not None and any(
        re.match(r"^\s*owner said\b.*[\"“].+[\"”]", l, re.IGNORECASE) for l in own_lines(m.body))


def overrides(item: Item) -> tuple[list[str], bool]:
    """Overrides recorded on the item, open or recently closed, and whether their follow-up is done. A
    `Follow-up done:` line closes the overrides posted before it on the same item."""
    notes, pending = [], False
    for m in sorted(item.messages, key=lambda m: m.created_at):
        step = override_step(m)
        if step is None:
            continue
        lines = own_lines(m.body)
        follow = next((re.sub(r"^\s*follow-up\s*:\s*", "", l, flags=re.IGNORECASE) for l in lines
                       if re.match(r"^\s*follow-up\s*:", l, re.IGNORECASE)), None)
        done = any(x.created_at > m.created_at and has_line(x.body, "follow-up done") for x in item.messages)
        pending = pending or (bool(follow) and not done)
        if done:
            continue  # finished: nothing left to show
        state = "done" if done else ("**pending**" if follow else "**no follow-up stated**")
        verified = "" if is_verified_override(m) else " **unverified: no quoted owner words**"
        notes.append(f"[#{item.number}]({item.url}) skipped {safe(step)} ({m.url}){verified}; follow-up: "
                     f"{safe(follow or 'none')}, {state}")
    return notes, pending


def owner_words(item: Item, now: datetime) -> list[str]:
    """Every `Owner said` line of the last week. Every agent posts through the owner's account, so the owner seeing
    what was recorded in their name is the check that nobody wrote words they never said."""
    notes = []
    for m in item.messages:
        if now - m.created_at > timedelta(days=7):
            continue
        for line in own_lines(m.body):
            if re.match(r"^\s*owner said\b", line, re.IGNORECASE):
                notes.append(f"[#{item.number}]({item.url}) {m.created_at:%m-%d %H:%M} by {sender(m.body)}: "
                             f"{safe(line.strip())[:200]} ({m.url})")
    return notes


def silent(item: Item, now: datetime) -> list[Stall]:
    """Stalls with no unanswered message: a task claimed without a pull request, or a pull request gone quiet."""
    if item.kind == "Task":
        if item.claimed_at and not item.linked and now - item.claimed_at >= CLAIM_WITHOUT_PR:
            return [Stall(item, f"{item.url}#claim#{item.claimed_at:%Y%m%dT%H%M}",
                          f"task #{item.number} was claimed {_age(now - item.claimed_at)} ago and no open pull request "
                          f"mentions it. The claimer opens a draft PR, or another agent takes the task over after "
                          f"posting `Taking over #{item.number} from <agent>` and swapping the `claimed:` label.")]
        return []
    if item.kind == "PR" and item.idle_since and now - item.idle_since >= IDLE:
        return [Stall(item, f"{item.url}#idle#{item.idle_since:%Y%m%dT%H%M}",
                      f"#{item.number} has had no message or commit for {_age(now - item.idle_since)}. Its driver "
                      f"posts where it stands (or `Pausing:`); if nobody does, another agent may take it over.")]
    return []


def find(items: list[Item], now: datetime, window: timedelta, step: timedelta, timebox: timedelta) -> Findings:
    found = Findings()
    for item in items:
        found.owner_words += owner_words(item, now)
        notes, pending = overrides(item)
        found.overrides += notes
        # The label keeps a pending follow-up in view however long it takes: labelled items are read even when
        # closed, at any age, and the label goes when every follow-up on the item is done.
        if pending and FOLLOW_UP_LABEL not in item.labels:
            found.label.add(item.number)
        if not pending and FOLLOW_UP_LABEL in item.labels:
            found.unlabel.add(item.number)
        if not item.is_open:
            continue  # a recently closed item is read only for its overrides' follow-ups
        hold = current_hold(item.messages)
        if hold:
            if now - hold.since > timedelta(days=1):
                found.needs_owner.append(needs(f"hold#{hold.url}", f"[#{item.number}]({item.url}) has been on hold "
                                                                   f"for {_age(now - hold.since)} ({hold.url}): lift it "
                                                                   f"or close the item."))
            continue
        mine = unanswered(item, now, window)
        for m in item.messages:
            reason = malformed(m.body) if sender(m.body) != BOT and _login_of(m) != "github-actions[bot]" else None
            author = sender(m.body)
            # Once its sender has posted again, an old unreadable message is history, not a stall. Without a
            # signature there is no sender to wait for: it is history once anyone has posted after it and it has
            # been flagged for an hour.
            later = [x for x in item.messages if x.created_at > m.created_at and sender(x.body) != BOT
                     and _login_of(x) != "github-actions[bot]"]
            if reason and (any(sender(x.body) == author for x in later) if author
                           else later and now - m.created_at >= timedelta(hours=1)):
                reason = None
            if reason:
                mine.append(Stall(item, f"{m.url}#malformed",
                                  f"the message {m.url} cannot be read: {reason}. Repost it with `To: <agent>` as "
                                  f"the first line and `From: <agent>` as the last line, so its wait and its answer "
                                  f"count."))
        if item.is_debate:
            sub = debate(item, now, step, timebox)
            mine += sub.stalls
            found.needs_owner += sub.needs_owner
        if not mine:
            mine = silent(item, now)
        for stall in mine:
            if stall.key.endswith("#malformed"):
                continue  # an unreadable message is the agents' to fix; it never needs the owner
            # A stall nobody resolved within a day needs the owner: perhaps no agent is running, or none can do it.
            first = next((m for m in item.messages if MARKER.format(key=stall.key) in (m.body or "")), None)
            if first and now - first.created_at >= ESCALATE:
                found.needs_owner.append(needs(f"escalate#{stall.key}", f"[#{item.number}]({item.url}) stalled for "
                                                                         f"{_age(now - first.created_at)} with no agent "
                                                                         f"acting on it ({first.url}): start an agent "
                                                                         f"on it, or close it."))
        found.stalls += mine
    return found


def _login_of(m: Message) -> str:
    return (m.author or "").lower()


def already_announced(item: Item, stall: Stall) -> bool:
    return any(MARKER.format(key=stall.key) in (m.body or "") for m in item.messages)


def nudge_body(stalls: Stall | list[Stall]) -> str:
    """One comment for all of an item's new stalls, so a busy item gets one notification, not one per stall."""
    stalls = stalls if isinstance(stalls, list) else [stalls]
    lines = [f"Overdue: {stalls[0].text}"] if len(stalls) == 1 else ["Overdue:", *[f"- {s.text}" for s in stalls]]
    markers = "\n".join(MARKER.format(key=s.key) for s in stalls)
    return "To: all\n\n" + "\n".join(lines) + f"\n\n{markers}\nFrom: {BOT}"


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
    if found.owner_words:
        lines += ["**Recorded as your words this week** (check they are yours; agents post through your account):",
                  *[f"- {o}" for o in found.owner_words], ""]
    if not any(i.is_open for i in items):
        lines.append("No open pull requests, debates or claimed tasks.")
    for item in (i for i in items if i.is_open):
        mine = [s for s in found.stalls if s.item is item]
        state = "on hold" if current_hold(item.messages) else ("**stalled**" if mine else "moving")
        lines.append(f"- {item.kind} [#{item.number}]({item.url}) {safe(item.title)}: {state}")
        lines += [f"  - {s.text}" for s in mine]
    return "\n".join(lines) + "\n"


# ---- GitHub I/O -------------------------------------------------------------------------------------------------

def _send(method: str, url: str, token: str, payload: dict | None):
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(url, method=method, data=data, headers={
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "aptura-stalls",
        "Authorization": f"Bearer {token}",
    })
    with urllib.request.urlopen(request, timeout=30) as response:
        raw = response.read()
        return json.loads(raw) if raw else None


def _message(kind: str, entry: dict, time_key: str = "created_at") -> Message:
    return Message(kind, pr_checks._parse_time(entry[time_key]), entry["html_url"], entry["body"] or "", _login(entry))


def _comments(base: str, number: int, token: str | None, is_pr: bool, authors: set[str]) -> list[Message]:
    """Everything said on one item: for items older than the repo-wide read, and closed follow-up items."""
    found = [_message("comment", c) for c in every(f"{base}/issues/{number}/comments", token)]
    if is_pr:
        found += [_message("review", r, "submitted_at") for r in every(f"{base}/pulls/{number}/reviews", token)
                  if r.get("submitted_at")]
        found += [_message("review comment", c) for c in every(f"{base}/pulls/{number}/comments", token)]
    # A stranger's comment on this public repository must not create a wait, fake a `Hold:` or resolve one.
    return trusted(found, authors)


def recently_closed(url: str, token: str | None, since: datetime):
    """Closed entries updated after `since`, newest first, stopping at the first older one."""
    for page in range(1, 11):
        batch = pr_checks._get(f"{url}&sort=updated&direction=desc&per_page=100&page={page}", token)
        for entry in batch:
            if pr_checks._parse_time(entry["updated_at"]) < since:
                return
            yield entry
        if len(batch) < 100:
            return


def _number(url: str) -> int:
    return int(url.rstrip("/").rsplit("/", 1)[1])


def fetch(repo: str, token: str | None, extra_trusted: list[str] | None = None,
          now: datetime | None = None) -> list[Item]:
    """Everything the checks read, in a fixed number of listing calls plus one review call per open pull request.

    Comments come from two repository-wide listings (issue comments and review comments) covering every item opened
    in the last READ_DAYS days, instead of three calls per item: every run must fit, with room to spare, in the
    1,000 requests an hour GitHub gives the workflow token, shared with CI.
    """
    now = now or datetime.now(timezone.utc)
    base = f"{pr_checks.API}/repos/{repo}"
    authors = trusted_authors(repo, extra_trusted)
    cutoff = now - timedelta(days=CLOSED_DAYS)

    # Agents open pull requests through the owner's account; anyone else's (a stranger's, Dependabot's) is not
    # agent work and gets no nudges.
    open_prs = [pr for pr in every(f"{base}/pulls?state=open", token) if _login(pr).lower() in authors]
    open_issues = [i for i in every(f"{base}/issues?state=open", token) if "pull_request" not in i]
    # Anyone can open an issue with the Debate form; only the owner's (and so the agents') debates are work.
    debates = [i for i in open_issues if DEBATE_LABEL in _labels(i) and _login(i).lower() in authors]
    tasks = [i for i in open_issues if any(label.startswith("claimed:") for label in _labels(i))
             and DEBATE_LABEL not in _labels(i)]
    # An override's follow-up usually comes after the merge ("skip the review, review afterwards"). Closed pull
    # requests and debates are read for their overrides only: those closed in the last CLOSED_DAYS days, to find new
    # overrides, and those labelled follow-up at any age, until their follow-up is done.
    closed: dict[int, dict] = {}
    for entry in recently_closed(f"{base}/pulls?state=closed", token, cutoff):
        closed[entry["number"]] = {**entry, "pull_request": {}}
    for entry in recently_closed(f"{base}/issues?state=closed&labels={DEBATE_LABEL}", token, cutoff):
        closed.setdefault(entry["number"], entry)
    for entry in every(f"{base}/issues?state=closed&labels={FOLLOW_UP_LABEL}", token):
        closed.setdefault(entry["number"], entry)

    entries = [*open_prs, *debates, *tasks]
    since = max(min((pr_checks._parse_time(e["created_at"]) for e in entries), default=cutoff),
                now - timedelta(days=READ_DAYS))
    since = min(since, cutoff)
    stamp = f"{since:%Y-%m-%dT%H:%M:%SZ}"
    said: dict[int, list[Message]] = {}
    for c in every(f"{base}/issues/comments?since={stamp}&sort=created&direction=asc", token):
        said.setdefault(_number(c["issue_url"]), []).append(_message("comment", c))
    for c in every(f"{base}/pulls/comments?since={stamp}&sort=created&direction=asc", token):
        said.setdefault(_number(c["pull_request_url"]), []).append(_message("review comment", c))

    def messages(entry: dict, is_pr: bool, is_open: bool) -> list[Message]:
        if pr_checks._parse_time(entry["created_at"]) < since:  # older than the repo-wide read: read it alone
            return _comments(base, entry["number"], token, is_pr, authors)
        found = list(said.get(entry["number"], []))
        # Reviews have no repository-wide listing. Overrides are comments, so closed items skip them.
        if is_pr and is_open:
            found += [_message("review", r, "submitted_at") for r in every(f"{base}/pulls/{entry['number']}/reviews",
                                                                           token) if r.get("submitted_at")]
        return trusted(found, authors)

    items: list[Item] = []
    for number, entry in sorted(closed.items()):
        # Reviews are read for a closed item only while its follow-up is pending, for overrides recorded in a review.
        found = messages(entry, "pull_request" in entry, FOLLOW_UP_LABEL in _labels(entry))
        if any(override_step(m) for m in found):
            items.append(Item(number, entry["title"], entry["html_url"], False, found, is_open=False,
                              labels=_labels(entry)))
    # A task is linked when an open pull request, or one closed recently (merged), mentions it.
    pr_text = " ".join(f"{pr.get('title') or ''} {pr.get('body') or ''}" for pr in
                       [*open_prs, *(e for e in closed.values() if "pull_request" in e)])
    for pr in open_prs:
        found = messages(pr, True, True)
        item = Item(pr["number"], pr["title"], pr["html_url"], False, found, labels=_labels(pr))
        # Any trusted comment is activity, signed or not (the owner writes plain comments); the bot's own are not.
        last = max((m.created_at for m in found if sender(m.body) != BOT and _login_of(m) != "github-actions[bot]"),
                   default=pr_checks._parse_time(pr["created_at"]))
        if now - last >= IDLE:  # only then is the head commit worth a request
            commit = pr_checks._get(f"{base}/commits/{pr['head']['sha']}", token)
            last = max(last, pr_checks._parse_time(commit["commit"]["committer"]["date"]))
        item.idle_since = last
        items.append(item)
    for issue in debates:
        items.append(Item(issue["number"], issue["title"], issue["html_url"], True, messages(issue, False, True),
                          labels=_labels(issue)))
    for issue in tasks:
        found = messages(issue, False, True)
        claims = [m.created_at for m in found if any(re.match(r"^\s*claim\s*:", l, re.IGNORECASE)
                                                      for l in own_lines(m.body))]
        items.append(Item(issue["number"], issue["title"], issue["html_url"], False, found, labels=_labels(issue),
                          kind="Task", claimed_at=max(claims, default=pr_checks._parse_time(issue["updated_at"])),
                          linked=re.search(rf"#{issue['number']}\b", pr_text) is not None))
    return items


def _labels(entry: dict) -> set[str]:
    return {label["name"] for label in entry.get("labels") or []}


def publish(repo: str, token: str, items: list[Item], found: Findings, text: str) -> tuple[int, list[str]]:
    """Post one nudge per item with new stalls, set the follow-up labels, and keep the report in the open issue
    labelled agent-status. A write that fails is reported and the rest still happen, so one locked conversation or
    a label removed by hand cannot freeze the whole status page."""
    base = f"{pr_checks.API}/repos/{repo}"
    failures: list[str] = []

    def write(method: str, url: str, payload: dict | None) -> bool:
        for attempt in range(pr_checks.RETRIES):
            try:
                _send(method, url, token, payload)
                return True
            except (urllib.error.URLError, TimeoutError) as error:
                if pr_checks.is_outage(error) and attempt < pr_checks.RETRIES - 1:
                    pr_checks.time.sleep(5 * (attempt + 1))
                    continue
                if isinstance(error, urllib.error.HTTPError) and method == "DELETE" and error.code == 404:
                    return True  # the label was already gone
                failures.append(f"{method} {url}: {error}")
                return False
        return False

    posted = 0
    by_item: dict[int, list[Stall]] = {}
    for stall in found.stalls:
        if not already_announced(stall.item, stall):
            by_item.setdefault(stall.item.number, []).append(stall)
    for number, new in sorted(by_item.items()):
        posted += write("POST", f"{base}/issues/{number}/comments", {"body": nudge_body(new)})
    for number in sorted(found.label):
        write("POST", f"{base}/issues/{number}/labels", {"labels": [FOLLOW_UP_LABEL]})
    for number in sorted(found.unlabel):
        write("DELETE", f"{base}/issues/{number}/labels/{FOLLOW_UP_LABEL}", None)
    status = list(every(f"{base}/issues?state=open&labels={STATUS_LABEL}", token))
    footer = ("\n_Updated by the Agent status workflow (`tools/agents/stalls.py`) about every 15 minutes; GitHub may "
              "delay or skip a scheduled run. If the time above is hours old, run the workflow from the Actions tab. "
              "Stalled items also get a `To: all` comment, which wakes the agents waiting on them._\n")
    body = text if len(text) + len(footer) <= BODY_LIMIT else text[:BODY_LIMIT - len(footer) - 40] + "\n\n(cut short)\n"
    if status:
        write("PATCH", f"{base}/issues/{status[0]['number']}", {"body": body + footer})
        # Editing the page notifies nobody. A new "Needs you" entry gets one comment, which GitHub sends the owner;
        # agent-only stalls never do, so the owner's notifications are only the things only the owner can settle.
        old = status[0].get("body") or ""
        new = [n for n in found.needs_owner
               if (key := re.search(r"<!-- needs:.+? -->", n)) is None or key.group(0) not in old]
        if new:
            write("POST", f"{base}/issues/{status[0]['number']}/comments",
                  {"body": "The agents need you for:\n\n" + "\n".join(f"- {n}" for n in new)})
    else:
        write("POST", f"{base}/issues", {"title": "Agent status", "body": body + footer, "labels": [STATUS_LABEL]})
    return posted, failures


def _age(delta: timedelta) -> str:
    minutes = int(delta.total_seconds() // 60)
    return f"{minutes // 60} h {minutes % 60:02d} min"


def explain(body: str) -> int:
    """Show how the checks read a message, so an agent can check its format before posting."""
    markers = sorted({word for word in ("working", "pausing", "withdrawn", "hold lifted", "hold", "override",
                                        "follow-up done", "follow-up", "proposal", "critique", "verdict", "rebuttal",
                                        "decision", "claim") if has_line(body, word)}
                     | ({"taking over"} if taken_over_from(body) else set())
                     | ({"owner said"} if any(re.match(r"^\s*owner said\b", l, re.IGNORECASE) for l in own_lines(body))
                        else set()))
    print(f"to:       {', '.join(sorted(recipients(body))) or '(nobody: not a message)'}")
    print(f"from:     {sender(body) or '(missing)'}")
    print(f"markers:  {', '.join(markers) or '(none)'}")
    print(f"is a wait: {bool(recipients(body) - {'all', 'owner'}) and is_wait(body)}")
    problem = malformed(body)
    print(f"problem:  {problem or 'none'}")
    return 1 if problem else 0


def main() -> int:
    force_utf8_output()
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", "TejuBoricha/Aptura"))
    parser.add_argument("--minutes", type=float, default=30, help="deadline for answering a To: message (default 30)")
    parser.add_argument("--debate-hours", type=float, default=2, help="deadline for each debate step (default 2)")
    parser.add_argument("--timebox-hours", type=float, default=24, help="time box for a whole debate (default 24)")
    parser.add_argument("--trust", nargs="*", default=[], help="extra GitHub logins whose comments count")
    parser.add_argument("--nudge", action="store_true", help="post the nudges and update the status issue")
    parser.add_argument("--explain", action="store_true",
                        help="read one message from standard input and show how the checks read it")
    args = parser.parse_args()
    if args.explain:
        return explain(sys.stdin.read())
    token = github_token()
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
            posted, failures = publish(args.repo, token, items, found, text)
            print(f"Posted {posted} new nudge(s).")
            for failure in failures:
                print(f"::error::Agent status could not write: {failure}")
            if failures:
                return 1
    except (urllib.error.URLError, TimeoutError) as error:
        if not pr_checks.is_outage(error):
            print(f"::error::Agent status could not use the GitHub API ({error}). Check the workflow's permissions.")
            return 1
        print(f"::warning::Agent status skipped: GitHub API unavailable ({error}). The next scheduled run retries.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
