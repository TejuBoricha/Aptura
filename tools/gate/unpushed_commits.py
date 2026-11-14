#!/usr/bin/env python3
"""Warn about commits that exist only on this machine.

tools/gate.ps1 runs this last. A commit that no remote has is lost with the disk it sits on, and the situation is easy to
miss: a branch finished locally and never pushed, a commit made on a detached HEAD, a repository with no remote at all. This
is a warning, not a failure (a branch being worked on is expected to be ahead of its remote), so the gate prints it and still
passes; the point is that nobody discovers it by losing work.

"No remote has it" means: reachable from a local branch (or a detached HEAD) but from no remote-tracking branch, as of the
last `git fetch` or `git push`, and with no equivalent change on one either. A rebase merge (this project's way of merging)
gives a commit a new hash and deletes the branch, so the old local commit is on no remote by hash while its change is safely
on `main`; `git cherry` recognises that by patch, and such commits are reported as safe to delete instead of as a warning. A
squash merge combines commits into one new patch, so those are still reported. Nothing here talks to the network, so a remote
that moved since the last fetch is not seen.

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

Commit = tuple[str, str]  # (full hash, "short-hash subject")


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


def only_here(repo: Path, tip: str) -> list[Commit]:
    """Commits reachable from `tip` and from no remote-tracking branch, newest first."""
    out = git(repo, "log", tip, "--not", "--remotes", "--format=%H%x09%h %s")
    return [(line.split("\t", 1)[0], line.split("\t", 1)[1]) for line in out.splitlines() if "\t" in line]


def remote_refs(repo: Path) -> list[str]:
    """Remote-tracking branches, without the symbolic `origin/HEAD`."""
    refs = git(repo, "for-each-ref", "--format=%(refname)", "refs/remotes").split()
    return [ref for ref in refs if not ref.endswith("/HEAD")]


def already_there_by_patch(repo: Path, tip: str, commits: list[Commit]) -> set[str]:
    """Hashes of `commits` whose change is on some remote-tracking branch under another hash (`git cherry` marks them `-`)."""
    wanted = {full for full, _ in commits}
    covered: set[str] = set()
    for ref in remote_refs(repo):
        for line in git(repo, "cherry", ref, tip).splitlines():
            mark, _, full = line.partition(" ")
            if mark == "-" and full in wanted:
                covered.add(full)
    return covered


def find(repo: Path) -> tuple[bool, dict[str, list[str]], dict[str, list[str]]]:
    """Returns (the repository has a remote, {branch: commits no remote has}, {branch: commits a remote has under another hash})."""
    git(repo, "rev-parse", "--git-dir")  # fails loudly outside a repository
    has_remote = bool(git(repo, "remote").split())
    names = {tip: tip for tip in git(repo, "for-each-ref", "--format=%(refname:short)", "refs/heads").split()}
    # A detached HEAD is on no branch, so the branch loop cannot see commits made there.
    detached = subprocess.run(["git", "-C", str(repo), "symbolic-ref", "-q", "HEAD"], capture_output=True, check=False).returncode != 0
    if detached:
        names["HEAD"] = "HEAD (detached)"
    found: dict[str, list[str]] = {}
    safe: dict[str, list[str]] = {}
    for tip, name in names.items():
        commits = only_here(repo, tip)
        if not commits:
            continue
        covered = already_there_by_patch(repo, tip, commits)
        lost = [text for full, text in commits if full not in covered]
        merged = [text for full, text in commits if full in covered]
        if lost:
            found[name] = lost
        if merged:
            safe[name] = merged
    return has_remote, found, safe


def report(has_remote: bool, found: dict[str, list[str]], safe: dict[str, list[str]]) -> tuple[int, str]:
    lines: list[str] = []
    if found:
        lines.append("WARNING: commits that no remote has (as of the last fetch or push), so they exist only on this machine:")
        if not has_remote:
            lines.append("  This repository has no remote at all.")
        for branch, commits in sorted(found.items()):
            lines.append(f"  {branch}: {len(commits)} commit(s)")
            lines.extend(f"    {commit}" for commit in commits[:SHOWN_PER_BRANCH])
            if len(commits) > SHOWN_PER_BRANCH:
                lines.append(f"    ... and {len(commits) - SHOWN_PER_BRANCH} more")
        lines.append("  Push them (git push -u origin <branch>) or keep a copy elsewhere before this disk is all that has them.")
    if safe:
        lines.append("Already on a remote under a different hash (a rebase merge), safe to delete locally with `git branch -D <branch>`:")
        lines.extend(f"  {branch}: {len(commits)} commit(s)" for branch, commits in sorted(safe.items()))
    if not found:
        lines.insert(0, "every local commit is on a remote (as of the last fetch or push)")
        return CLEAN, "\n".join(lines)
    return UNPUSHED, "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", default=".", help="repository to check (default: the current directory)")
    args = parser.parse_args(argv)
    try:
        has_remote, found, safe = find(Path(args.repo))
    except GitError as error:
        print(f"could not check for commits no remote has: {error}")
        return UNKNOWN
    code, text = report(has_remote, found, safe)
    print(text)
    return code


if __name__ == "__main__":
    sys.exit(main())
