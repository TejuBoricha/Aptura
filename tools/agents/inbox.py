#!/usr/bin/env python3
"""Wait for a message from another agent on a pull request, so the owner does not have to relay it.

Agents talk on the pull request itself (docs/AGENT_WORKFLOW.md, "Talking to each other"). A message is any PR
comment, review or review comment whose first line is `To: <agent>` (several names separated by commas, or `all`),
signed at the end with `From: <agent>`. Everything posts through the owner's GitHub account, so these two lines,
not the GitHub author, say who is talking to whom. Only comments by the owner's account (and the Agent status bot)
count: on a public repository anyone can comment, and a stranger must not be able to address an agent.

After asking another agent something, run this and continue when it returns:

    python3 tools/agents/inbox.py --pr 4 --me opencode

It checks every --interval seconds and prints each new message addressed to you (exit 0). It also returns when the
pull request is merged or closed (exit 3), and gives up after --timeout seconds (exit 2). Cloud agents that receive
GitHub events directly (Claude Code on the web) do not need it. Output is UTF-8 with lossy substitution, so a console
whose code page cannot express a character (Windows cp1252) prints a replacement instead of crashing.
Standard library only.
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import time
from dataclasses import dataclass
from datetime import datetime, timezone

import pr_checks

TO_LINE = re.compile(r"^\s*to:\s*(.+?)\s*$", re.IGNORECASE)
FROM_LINE = re.compile(r"^\s*from:\s*(.+?)\s*$", re.IGNORECASE)
FENCE = re.compile(r"^\s*(`{3,}|~{3,})")


def message_lines(body: str) -> list[str]:
    """The lines a comment says itself, as the markers are read from them.

    Quoted lines (`> ...`) and fenced code blocks repeat or illustrate other text, so a `Hold:` or `From:` inside them
    never counts. Common formatting is undone, so `**To:** opencode` and `- Working: ...` read as `To: opencode` and
    `Working: ...`.
    """
    raw = (body or "").splitlines()
    lines, fence = [], None  # the open fence: its character and length, as CommonMark closes only on a match
    for line in raw:
        match = FENCE.match(line)
        if match:
            mark = match.group(1)
            if fence is None and mark[0] not in line.strip()[len(mark):]:  # ```x``` on one line is inline code
                fence = (mark[0], len(mark))
                continue
            if fence and mark[0] == fence[0] and len(mark) >= fence[1] and not line.strip()[len(mark):].strip():
                fence = None
                continue
        if fence or line.lstrip().startswith(">"):
            continue
        line = re.sub(r"^\s*[-*+]\s+", "", line)  # a list bullet
        # Bold around a leading label only (`**To:** x`, `**To: x**`), so content such as `__main__` stays intact.
        line = re.sub(r"^(\s*)(\*\*|__)(.+?)\2", r"\1\3", line, count=1)
        lines.append(line)
    if fence is not None:
        # A fence that never closes would swallow the signature: read the message as if it had no code block.
        return [re.sub(r"^(\s*)(\*\*|__)(.+?)\2", r"\1\3", re.sub(r"^\s*[-*+]\s+", "", line), count=1)
                for line in raw if not FENCE.match(line) and not line.lstrip().startswith(">")]
    return lines


def github_token() -> str | None:
    """GITHUB_TOKEN or GH_TOKEN, else the GitHub CLI's login (`gh auth token`), which most local agents already have.
    Without a token GitHub allows 60 requests an hour, which a waiting agent would use up in minutes."""
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        return token
    try:
        result = subprocess.run(["gh", "auth", "token"], capture_output=True, text=True, timeout=10)
    except (OSError, subprocess.SubprocessError):
        return None
    if result.returncode != 0:
        return None
    return result.stdout.strip() or None


def force_utf8_output() -> None:
    """Write UTF-8 with lossy substitution, so printing never kills the wait.

    A Windows console defaults to a code page such as cp1252; printing a message body that the page cannot express
    (a `->` arrow in a real PR comment) raised UnicodeEncodeError with exit 1, which a waiting agent reads as "the
    script is broken" instead of "a message is waiting". Streams that cannot reconfigure (test capture harnesses,
    replaced pipes) keep their current behaviour.
    """
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except (AttributeError, ValueError):
            pass


@dataclass
class Message:
    kind: str
    created_at: datetime
    url: str
    body: str
    author: str = ""  # the GitHub login that posted it


# Every agent posts through the owner's account, and the Agent status workflow as github-actions[bot]. Anyone else
# can comment on a public repository, so their comments are never messages: a stranger must not be able to address an
# agent, fake a `Hold:` or a `Taking over`, or answer for one.
STATUS_BOT = "github-actions[bot]"
STALL_MARKER = "<!-- stall:"  # every nudge stalls.py posts carries one


def trusted_authors(repo: str, extra: list[str] | None = None) -> set[str]:
    """The owner (every agent posts through the owner's account) plus any logins named explicitly."""
    return {repo.split("/")[0].lower(), *(name.lower() for name in extra or [])}


def is_trusted(m: Message, authors: set[str]) -> bool:
    if m.author.lower() == STATUS_BOT:
        # github-actions[bot] is shared by every workflow in the repository, and another workflow could echo text a
        # stranger controls. Only the stall check's own nudges, which carry its marker and signature, count.
        return STALL_MARKER in (m.body or "") and sender(m.body) == "agent-status"
    return m.author.lower() in authors


def trusted(messages: list[Message], authors: set[str]) -> list[Message]:
    return [m for m in messages if is_trusted(m, authors)]


def every(url: str, token: str | None, max_pages: int = 100):
    """All pages of a listing. Listings are oldest first, so stopping early would drop the newest messages, the ones
    that matter most; past `max_pages` (10,000 entries) fail loudly instead."""
    for page in range(1, max_pages + 1):
        sep = "&" if "?" in url else "?"
        batch = pr_checks._get(f"{url}{sep}per_page=100&page={page}", token)
        yield from batch
        if len(batch) < 100:
            return
    raise RuntimeError(f"more than {max_pages * 100} entries at {url}; refusing to read a truncated thread")


def agent_name(text: str) -> str | None:
    """The agent's name is the first word: `OpenCode (DeepSeek V4)` and `Claude Code` are opencode and claude."""
    match = re.match(r"\s*([\w-]+)", text or "")
    return match.group(1).lower() if match else None


def recipients(body: str) -> set[str]:
    """Names on the first non-empty line `To: a, b` (or `a and b`); empty when the message is not addressed."""
    for line in message_lines(body):
        if line.strip():
            match = TO_LINE.match(line)
            if not match:
                return set()
            parts = re.split(r",|\band\b", match.group(1), flags=re.IGNORECASE)
            return {name for part in parts if (name := agent_name(part))}
    return set()


def malformed(body: str) -> str | None:
    """Why a comment that looks like a message cannot be read as one, or None. A message nobody can parse would
    otherwise be silently ignored: no wait, no answer, no nudge."""
    lines = [line for line in message_lines(body) if line.strip()]
    has_to = any(TO_LINE.match(line) for line in lines)
    has_from = any(FROM_LINE.match(line) for line in lines)
    # A note signed `From:` with no `To:` (a pause, a status) is fine: it addresses nobody, so nobody waits on it.
    if has_to and not TO_LINE.match(lines[0]):
        return "its `To:` line is not the first line"
    if has_to and not has_from:
        return "it has a `To:` line but no `From:` line"
    return None


def sender(body: str) -> str | None:
    matches = [m.group(1) for line in message_lines(body) if (m := FROM_LINE.match(line))]
    return agent_name(matches[-1]) if matches else None


def for_me(messages: list[Message], me: str, since: datetime) -> list[Message]:
    """New messages addressed to `me` (or to all), excluding the ones `me` sent."""
    me = agent_name(me) or ""
    picked = [m for m in messages
              if m.created_at > since and (me in recipients(m.body) or "all" in recipients(m.body))
              and sender(m.body) != me]
    return sorted(picked, key=lambda m: m.created_at)


def fetch_messages(repo: str, number: int, token: str | None, is_pr: bool = True) -> list[Message]:
    """Comments on a pull request or issue; a pull request also has reviews and review comments."""
    base = f"{pr_checks.API}/repos/{repo}"
    found: list[Message] = []
    for c in every(f"{base}/issues/{number}/comments", token):
        found.append(Message("comment", pr_checks._parse_time(c["created_at"]), c["html_url"], c["body"] or "",
                             _login(c)))
    if not is_pr:
        return found
    for r in every(f"{base}/pulls/{number}/reviews", token):
        if r.get("submitted_at"):
            found.append(Message("review", pr_checks._parse_time(r["submitted_at"]), r["html_url"], r["body"] or "",
                                 _login(r)))
    for c in every(f"{base}/pulls/{number}/comments", token):
        found.append(Message("review comment", pr_checks._parse_time(c["created_at"]), c["html_url"], c["body"] or "",
                             _login(c)))
    return found


def _login(entry: dict) -> str:
    return (entry.get("user") or {}).get("login") or ""


def main() -> int:
    force_utf8_output()
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", "TejuBoricha/Aptura"))
    parser.add_argument("--pr", type=int, required=True, help="the pull request or debate issue number")
    parser.add_argument("--me", required=True, help="your agent name, as others write it after To:")
    parser.add_argument("--since", help="ISO time to read from, e.g. 2026-10-28T15:00:00Z (default: your own latest "
                                         "message there, so a reply that came before you started waiting is not lost)")
    parser.add_argument("--interval", type=int, default=60)
    parser.add_argument("--timeout", type=int, default=4 * 3600)
    parser.add_argument("--trust", nargs="*", default=[],
                        help="extra GitHub logins whose comments count as messages (default: the owner and the bot)")
    args = parser.parse_args()
    authors = trusted_authors(args.repo, args.trust)
    token = github_token()
    if not token:
        # 60 requests an hour without a token, and each check costs four: slow down rather than run out.
        args.interval = max(args.interval, 240)
        print("No GitHub token (set GITHUB_TOKEN, or log in with `gh auth login`): checking every 4 minutes to stay "
              "within GitHub's limit of 60 requests an hour.", flush=True)
    since = pr_checks._parse_time(args.since) if args.since else None
    started = datetime.now(timezone.utc).replace(microsecond=0)
    deadline = time.monotonic() + args.timeout
    print(f"Waiting for messages to '{args.me}' on #{args.pr} ...", flush=True)

    while True:
        try:
            # The issues endpoint answers for pull requests and debate issues alike.
            state = pr_checks._get(f"{pr_checks.API}/repos/{args.repo}/issues/{args.pr}", token)
            messages = trusted(fetch_messages(args.repo, args.pr, token, "pull_request" in state), authors)
            if since is None:
                # From the last question this agent asked (a message to someone other than all), so a reply that
                # arrived before the wait started is not lost, even if a status note was posted after it.
                me = agent_name(args.me)
                asked = [m.created_at for m in messages if sender(m.body) == me and recipients(m.body) - {"all"}]
                since = max(asked, default=started)
                print(f"Reading messages after {since:%Y-%m-%dT%H:%M:%SZ}.", flush=True)
            new = for_me(messages, args.me, since)
        except Exception as error:  # A network blip should not end a long wait; report it and keep going.
            print(f"(could not check GitHub: {error}; trying again)", flush=True)
            new, state = [], {}
        if new:
            for m in new:
                print(f"\n--- {m.kind} {m.created_at:%Y-%m-%d %H:%M}Z from {sender(m.body) or 'unknown'} ---\n"
                      f"{m.url}\n\n{m.body}\n", flush=True)
            return 0
        if state.get("state") == "closed":
            merged = state.get("merged") or (state.get("pull_request") or {}).get("merged_at")
            print(f"#{args.pr} was {'merged' if merged else 'closed'}; nothing more to wait for.")
            return 3
        if time.monotonic() >= deadline:
            print(f"No message after {args.timeout} s. Check the pull request, or ask the owner.")
            return 2
        time.sleep(args.interval)


if __name__ == "__main__":
    sys.exit(main())
