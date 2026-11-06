#!/usr/bin/env python3
"""Fail when a signing key, private key or environment file is tracked by git (audit finding SEC-01).

.gitignore stops accidents, but `git add -f` and files that were tracked before a rule existed get past it. This is the
check that does not depend on anyone remembering: it lists what git actually tracks and fails on a forbidden name.

It checks names only, never file contents, so it cannot leak what it finds. It sees the current tree, not history. A key
that was already pushed has to be treated as compromised and replaced; removing the file does not undo that.

Usage: python3 tools/security/check_no_secret_files.py        (run from anywhere inside the repository)
"""
from __future__ import annotations

import fnmatch
import posixpath
import subprocess
import sys

# Matched against the lower-cased file name, never the directory, so `docs/keystore_setup_guide.md` is fine.
FORBIDDEN_NAMES = (
    "*.jks",
    "*.keystore",
    "*.p12",
    "*.pfx",
    "*.pem",
    "*.p8",
    "*.key",
    "id_rsa",
    "id_dsa",
    "id_ecdsa",
    "id_ed25519",
    "*.env",
    ".env.*",
    "local.properties",
    "keystore.properties",
    "signing.properties",
)

# Committed on purpose, with placeholder values only.
ALLOWED_NAMES = (".env.example", ".env.sample", ".env.template")


def find_forbidden(paths):
    """Return the paths whose file name is forbidden, in the order given."""
    found = []
    for path in paths:
        name = posixpath.basename(path).lower()
        if name in ALLOWED_NAMES:
            continue
        if any(fnmatch.fnmatchcase(name, pattern) for pattern in FORBIDDEN_NAMES):
            found.append(path)
    return found


def tracked_files():
    # `git ls-files` lists only the current directory and below, so always ask from the repository root. Otherwise a
    # run from a subdirectory would check part of the tree and still report a pass.
    root = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"], check=True, capture_output=True
    ).stdout.decode("utf-8", errors="surrogateescape").strip()
    out = subprocess.run(
        ["git", "ls-files", "-z"], check=True, capture_output=True, cwd=root
    ).stdout.decode("utf-8", errors="surrogateescape")
    return [p for p in out.split("\0") if p]


def main():
    try:
        paths = tracked_files()
    except (OSError, subprocess.CalledProcessError) as error:
        # A check that cannot look must not report a pass.
        print(f"check_no_secret_files: could not list tracked files: {error}", file=sys.stderr)
        return 2
    if not paths:
        print("check_no_secret_files: git lists no tracked files; refusing to report a pass", file=sys.stderr)
        return 2
    bad = find_forbidden(paths)
    if bad:
        print("Tracked files that must never be committed (SEC-01):", file=sys.stderr)
        for path in bad:
            print(f"  {path}", file=sys.stderr)
        print(
            "\nRemove them from git with `git rm --cached <file>` and keep them outside the repository.\n"
            "If a key was ever pushed, treat it as compromised: create a new one and revoke the old one.",
            file=sys.stderr,
        )
        return 1
    print(f"check_no_secret_files: {len(paths)} tracked files checked, none forbidden")
    return 0


if __name__ == "__main__":
    sys.exit(main())
