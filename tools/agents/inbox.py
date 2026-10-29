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
import sys
import time
from dataclasses import dataclass
from datetime import datetime, timezone

import pr_checks

TO_LINE = re.compile(r"^\s*to:\s*(.+?)\s*$", re.IGNORECASE)
FROM_LINE = re.compile(r"^\s*from:\s*(.+?)\s*$", re.IGNORECASE | re.MULTILINE)


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


def recipients(body: str) -> set[str]:
    """Names on the first non-empty line `To: a, b`; empty when the message is not addressed."""
    for line in (body or "").splitlines():
        if line.strip():
            match = TO_LINE.match(line)
            return {n.strip().lower() for n in match.group(1).split(",") if n.strip()} if match else set()
    return set()


def sender(body: str) -> str | None:
    matches = FROM_LINE.findall(body or "")
    return matches[-1].strip().lower() if matches else None


def for_me(messages: list[Message], me: str, since: datetime) -> list[Message]:
    """New messages addressed to `me` (or to all), excluding the ones `me` sent."""
    me = me.lower()
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
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
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
                mine = [m.created_at for m in messages if sender(m.body) == args.me.lower()]
                since = max(mine, default=started)
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
