#!/usr/bin/env python3
"""Warn about commits that exist only on this machine.

tools/gate.ps1 runs this last. A commit that no remote has is lost with the disk it sits on, and the situation is easy to
miss: a branch finished locally and never pushed, a commit made on a detached HEAD, a repository with no remote at all. This
is a warning, not a failure (a branch being worked on is expected to be ahead of its remote), so the gate prints it and still
passes; the point is that nobody discovers it by losing work.

"No remote has it" means: reachable from a local branch (or a detached HEAD) but from no remote-tracking branch, as of the
last `git fetch` or `git push`. Nothing here talks to the network, so a remote that moved since the last fetch is not seen.

Exit code 0: every local commit is on some remote. 1: some are not (listed). 2: the answer could not be found (not a git
repository, git missing), which is reported, never read as "all clear".

    python tools/gate/unpushed_commits.py [--repo PATH]
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

CLEAN = 0
UNPUSHED = 1
UNKNOWN = 2

# Enough to recognise the work; the count is always exact.
SHOWN_PER_BRANCH = 5


class GitError(Exception):
    pass


def git(repo: Path, *args: str) -> str:
    try:
        done = subprocess.run(
            ["git", "-C", str(repo), *args], capture_output=True, text=True, encoding="utf-8", errors="replace", check=False
        )
    except FileNotFoundError as error:
        raise GitError("git is not installed or not on PATH") from error
    if done.returncode != 0:
        raise GitError(f"git {' '.join(args)} failed: {done.stderr.strip() or done.stdout.strip() or done.returncode}")
    return done.stdout


def only_here(repo: Path, tip: str) -> list[str]:
    """Commits reachable from `tip` and from no remote-tracking branch, newest first, as `hash subject`."""
    return [line for line in git(repo, "log", tip, "--not", "--remotes", "--format=%h %s").splitlines() if line.strip()]


def find(repo: Path) -> tuple[bool, dict[str, list[str]]]:
    """Returns (the repository has a remote, {branch or HEAD: its local-only commits})."""
    git(repo, "rev-parse", "--git-dir")  # fails loudly outside a repository
    has_remote = bool(git(repo, "remote").split())
    found: dict[str, list[str]] = {}
    for branch in git(repo, "for-each-ref", "--format=%(refname:short)", "refs/heads").split():
        commits = only_here(repo, branch)
        if commits:
            found[branch] = commits
    # A detached HEAD is on no branch, so the loop above cannot see commits made there.
    detached = subprocess.run(["git", "-C", str(repo), "symbolic-ref", "-q", "HEAD"], capture_output=True, check=False).returncode != 0
    if detached:
        commits = only_here(repo, "HEAD")
        if commits:
            found["HEAD (detached)"] = commits
    return has_remote, found


def report(has_remote: bool, found: dict[str, list[str]]) -> tuple[int, str]:
    if not found:
        return CLEAN, "every local commit is on a remote (as of the last fetch or push)"
    lines = ["WARNING: commits that no remote has (as of the last fetch or push), so they exist only on this machine:"]
    if not has_remote:
        lines.append("  This repository has no remote at all.")
    for branch, commits in sorted(found.items()):
        lines.append(f"  {branch}: {len(commits)} commit(s)")
        lines.extend(f"    {commit}" for commit in commits[:SHOWN_PER_BRANCH])
        if len(commits) > SHOWN_PER_BRANCH:
            lines.append(f"    ... and {len(commits) - SHOWN_PER_BRANCH} more")
    lines.append("  Push them (git push -u origin <branch>) or keep a copy elsewhere before this disk is all that has them.")
    return UNPUSHED, "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", default=".", help="repository to check (default: the current directory)")
    args = parser.parse_args(argv)
    try:
        has_remote, found = find(Path(args.repo))
    except GitError as error:
        print(f"could not check for commits no remote has: {error}")
        return UNKNOWN
    code, text = report(has_remote, found)
    print(text)
    return code


if __name__ == "__main__":
    sys.exit(main())
