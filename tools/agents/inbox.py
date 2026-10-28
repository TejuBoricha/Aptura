#!/usr/bin/env python3
"""Wait for a message from another agent on a pull request, so the owner does not have to relay it.

Agents talk on the pull request itself (docs/AGENT_WORKFLOW.md, "Talking to each other"). A message is any PR
comment, review or review comment whose first line is `To: <agent>` (several names separated by commas, or `all`),
signed at the end with `From: <agent>`. Everything posts through the owner's GitHub account, so these two lines,
not the GitHub author, say who is talking to whom.

After asking another agent something, run this and continue when it returns:

    python3 tools/agents/inbox.py --pr 4 --me opencode

It checks every --interval seconds and prints each new message addressed to you (exit 0). It also returns when the
pull request is merged or closed (exit 3), and gives up after --timeout seconds (exit 2). Cloud agents that receive
GitHub events directly (Claude Code on the web) do not need it. Standard library only.
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


@dataclass
class Message:
    kind: str
    created_at: datetime
    url: str
    body: str


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


def fetch_messages(repo: str, number: int, token: str | None) -> list[Message]:
    base = f"{pr_checks.API}/repos/{repo}"
    found: list[Message] = []
    for c in pr_checks._pages(f"{base}/issues/{number}/comments", token, limit=5):
        found.append(Message("comment", pr_checks._parse_time(c["created_at"]), c["html_url"], c["body"] or ""))
    for r in pr_checks._pages(f"{base}/pulls/{number}/reviews", token, limit=5):
        if r.get("submitted_at"):
            found.append(Message("review", pr_checks._parse_time(r["submitted_at"]), r["html_url"], r["body"] or ""))
    for c in pr_checks._pages(f"{base}/pulls/{number}/comments", token, limit=5):
        found.append(Message("review comment", pr_checks._parse_time(c["created_at"]), c["html_url"], c["body"] or ""))
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", "TejuBoricha/Aptura"))
    parser.add_argument("--pr", type=int, required=True)
    parser.add_argument("--me", required=True, help="your agent name, as others write it after To:")
    parser.add_argument("--since", help="ISO time to read from, e.g. 2026-10-28T15:00:00Z (default: now)")
    parser.add_argument("--interval", type=int, default=60)
    parser.add_argument("--timeout", type=int, default=4 * 3600)
    args = parser.parse_args()
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    since = pr_checks._parse_time(args.since) if args.since else datetime.now(timezone.utc).replace(microsecond=0)
    deadline = time.monotonic() + args.timeout
    print(f"Waiting for messages to '{args.me}' on #{args.pr} since {since:%Y-%m-%dT%H:%M:%SZ} ...", flush=True)

    while True:
        try:
            new = for_me(fetch_messages(args.repo, args.pr, token), args.me, since)
            state = pr_checks._get(f"{pr_checks.API}/repos/{args.repo}/pulls/{args.pr}", token)
        except Exception as error:  # A network blip should not end a long wait; report it and keep going.
            print(f"(could not check GitHub: {error}; trying again)", flush=True)
            new, state = [], {}
        if new:
            for m in new:
                print(f"\n--- {m.kind} {m.created_at:%Y-%m-%d %H:%M}Z from {sender(m.body) or 'unknown'} ---\n"
                      f"{m.url}\n\n{m.body}\n")
            return 0
        if state.get("state") == "closed":
            print(f"#{args.pr} was {'merged' if state.get('merged') else 'closed'}; nothing more to wait for.")
            return 3
        if time.monotonic() >= deadline:
            print(f"No message after {args.timeout} s. Check the pull request, or ask the owner.")
            return 2
        time.sleep(args.interval)


if __name__ == "__main__":
    sys.exit(main())
