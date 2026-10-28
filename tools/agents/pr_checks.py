#!/usr/bin/env python3
"""Coordination checks for pull requests written by AI agents (AGENTS.md, docs/AGENT_WORKFLOW.md).

Runs in CI on every pull request, inside the required `verify` job, so a failure blocks the merge. It checks:

* every commit names the agent that helped (`Assisted-by:`) or says a person wrote it (`Human-authored: yes`);
* no older open pull request already changes the same hot file (tools/agents/hot-files.txt);
* no older open pull request already works on the same issue.

It also warns, without failing, about ordinary file overlap, likely duplicate work and a draft with no Handoff block.
"Older" means a lower pull request number: the first one goes in first and the later one waits, so two pull requests
never block each other. Pull requests untouched for STALE_DAYS only warn, so an abandoned draft cannot block forever.

Standard library only, so it runs on any runner and on a developer machine:

    GITHUB_TOKEN=... python3 tools/agents/pr_checks.py --repo TejuBoricha/Aptura --pr 4
"""

from __future__ import annotations

import argparse
import fnmatch
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from pathlib import Path

HOT_FILES = Path(__file__).with_name("hot-files.txt")
STALE_DAYS = 14
RETRIES = 3
API = "https://api.github.com"

# `[ \t]*`, not `\s*`: an empty "Assisted-by:" must not borrow the next line as its value.
ATTRIBUTION = re.compile(r"^(assisted-by:[ \t]*\S|human-authored:[ \t]*yes\b)", re.IGNORECASE | re.MULTILINE)
ISSUE_REF = re.compile(
    r"\b(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?|refs?|issue)\b\s*:?\s*((?:#\d+[\s,]*(?:and\s+)?)+)", re.IGNORECASE
)


@dataclass
class PullRequest:
    number: int
    title: str
    body: str
    draft: bool
    updated_at: datetime
    files: list[str] = field(default_factory=list)
    commit_messages: list[str] = field(default_factory=list)


@dataclass
class Finding:
    level: str  # "error" or "warning"
    message: str


def load_hot_patterns(path: Path = HOT_FILES) -> list[str]:
    lines = path.read_text(encoding="utf-8").splitlines()
    return [line.strip() for line in lines if line.strip() and not line.lstrip().startswith("#")]


def is_hot(path: str, patterns: list[str]) -> bool:
    return any(fnmatch.fnmatchcase(path, pattern) for pattern in patterns)


def issue_refs(text: str) -> set[int]:
    refs: set[int] = set()
    for match in ISSUE_REF.finditer(text or ""):
        refs.update(int(n) for n in re.findall(r"#(\d+)", match.group(1)))
    return refs


def unattributed(messages: list[tuple[str, str]]) -> list[str]:
    """Short SHAs of commits with neither trailer. Each item is (sha, message); merge commits are excluded upstream."""
    return [sha[:7] for sha, message in messages if not ATTRIBUTION.search(message)]


def check(pr: PullRequest, others: list[PullRequest], shas_and_messages: list[tuple[str, str]],
          patterns: list[str], now: datetime) -> list[Finding]:
    findings: list[Finding] = []

    missing = unattributed(shas_and_messages)
    if missing:
        findings.append(Finding("error",
            f"Commits without an attribution trailer: {', '.join(missing)}. End each commit message with "
            f"`Assisted-by: <Tool> (<model>)`, or `Human-authored: yes` if a person wrote it, then rebase the "
            f"branch to reword them (see AGENTS.md)."))

    my_hot = {f for f in pr.files if is_hot(f, patterns)}
    my_issues = issue_refs(f"{pr.title}\n{pr.body}\n" + "\n".join(pr.commit_messages))
    stale_before = now - timedelta(days=STALE_DAYS)

    for other in sorted(others, key=lambda o: o.number):
        if other.number == pr.number:
            continue
        earlier = other.number < pr.number
        stale = other.updated_at < stale_before
        blocking = earlier and not stale
        shared_hot = sorted(my_hot & set(other.files))
        shared_issues = sorted(my_issues & issue_refs(f"{other.title}\n{other.body}\n" + "\n".join(other.commit_messages)))
        shared_files = sorted(set(pr.files) & set(other.files))

        if shared_hot:
            if blocking:
                findings.append(Finding("error",
                    f"#{other.number} is already changing hot file(s) {', '.join(shared_hot)}. Only one open pull "
                    f"request may change these. Wait for #{other.number} to merge or close, then rebase and push."))
            else:
                why = "it has been idle for over two weeks" if stale else "this pull request is older"
                findings.append(Finding("warning",
                    f"#{other.number} also changes hot file(s) {', '.join(shared_hot)} ({why}, so it does not block "
                    f"this one)."))
        if shared_issues:
            refs = ", ".join(f"#{n}" for n in shared_issues)
            if blocking:
                findings.append(Finding("error",
                    f"#{other.number} is already working on {refs}. Two pull requests for the same task is "
                    f"duplicate work: close one, or link a different issue."))
            else:
                findings.append(Finding("warning", f"#{other.number} also references {refs}."))
        ordinary = [f for f in shared_files if f not in shared_hot]
        if ordinary:
            findings.append(Finding("warning",
                f"#{other.number} also changes {', '.join(ordinary[:5])}"
                f"{' and more' if len(ordinary) > 5 else ''}. Whichever merges second must rebase and re-run CI."))
        if len(pr.files) >= 2 and len(shared_files) * 2 >= len(pr.files):
            findings.append(Finding("warning",
                f"#{other.number} changes {len(shared_files)} of this pull request's {len(pr.files)} files. "
                f"Check that the two are not the same task."))

    if pr.draft and "## handoff" not in (pr.body or "").lower():
        findings.append(Finding("warning",
            "Draft pull request without a `## Handoff` block. Add one so another agent can continue the work "
            "(docs/AGENT_WORKFLOW.md)."))
    return findings


# ---- GitHub I/O -------------------------------------------------------------------------------------------------

def _get(url: str, token: str | None):
    headers = {
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "aptura-pr-checks",
    }
    if token:  # Optional for a local run against a public repository (60 requests an hour without one).
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, headers=headers)
    for attempt in range(RETRIES):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            # 403/429 cover GitHub's rate limits; 5xx is a GitHub outage. Anything else is a real error.
            if error.code not in (403, 429, 500, 502, 503, 504) or attempt == RETRIES - 1:
                raise
        except urllib.error.URLError:
            if attempt == RETRIES - 1:
                raise
        time.sleep(5 * (attempt + 1))


def _pages(url: str, token: str | None, limit: int = 10):
    for page in range(1, limit + 1):
        sep = "&" if "?" in url else "?"
        batch = _get(f"{url}{sep}per_page=100&page={page}", token)
        yield from batch
        if len(batch) < 100:
            return


def _parse_time(value: str) -> datetime:
    return datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)


def fetch(repo: str, number: int, token: str | None) -> tuple[PullRequest, list[PullRequest], list[tuple[str, str]]]:
    def files(n: int) -> list[str]:
        paths: list[str] = []
        for f in _pages(f"{API}/repos/{repo}/pulls/{n}/files", token, limit=30):
            paths.append(f["filename"])
            if f.get("previous_filename"):  # A rename changes the old path too, e.g. moving a migration away.
                paths.append(f["previous_filename"])
        return paths

    def commits(n: int) -> list[dict]:
        return [c for c in _pages(f"{API}/repos/{repo}/pulls/{n}/commits", token, limit=3)
                if len(c.get("parents", [])) < 2]

    def as_pr(raw: dict) -> PullRequest:
        n = raw["number"]
        return PullRequest(n, raw["title"] or "", raw["body"] or "", bool(raw.get("draft")),
                           _parse_time(raw["updated_at"]), files(n), [c["commit"]["message"] for c in commits(n)])

    raw_pr = _get(f"{API}/repos/{repo}/pulls/{number}", token)
    pr = as_pr(raw_pr)
    shas_and_messages = [(c["sha"], c["commit"]["message"]) for c in commits(number)]
    # Only pull requests into the same branch compete: a stacked PR or a release backport is a different queue.
    base = raw_pr["base"]["ref"]
    others = [as_pr(raw) for raw in _pages(f"{API}/repos/{repo}/pulls?state=open", token, limit=3)
              if raw["number"] != number and raw["base"]["ref"] == base]
    return pr, others, shas_and_messages


def report(findings: list[Finding]) -> int:
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    lines = ["### Agent coordination checks", ""]
    if not findings:
        lines.append("All clear: commits are attributed and no other open pull request overlaps.")
    for f in findings:
        print(f"::{f.level}::{f.message}")
        lines.append(f"- **{'Blocking' if f.level == 'error' else 'Note'}:** {f.message}")
    if summary:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write("\n".join(lines) + "\n")
    else:
        print("\n".join(lines))
    return 1 if any(f.level == "error" for f in findings) else 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"))
    parser.add_argument("--pr", type=int)
    args = parser.parse_args()
    number = args.pr
    if number is None and os.environ.get("GITHUB_EVENT_PATH"):
        with open(os.environ["GITHUB_EVENT_PATH"], encoding="utf-8") as handle:
            number = json.load(handle).get("pull_request", {}).get("number")
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if not (args.repo and number):
        print("::error::Need --repo and --pr, or a pull_request event.")
        return 2
    try:
        pr, others, shas_and_messages = fetch(args.repo, number, token)
    except (urllib.error.URLError, TimeoutError) as error:
        # Fail open on a GitHub outage: these are coordination checks, and blocking every open pull request on an
        # API hiccup would cost more than a rare duplicate. The tests and build in the same job still gate the code.
        print(f"::warning::Agent coordination checks skipped: could not reach the GitHub API ({error}). "
              f"Push again or re-run the job to check this pull request.")
        return 0
    return report(check(pr, others, shas_and_messages, load_hot_patterns(), datetime.now(timezone.utc)))


if __name__ == "__main__":
    sys.exit(main())
