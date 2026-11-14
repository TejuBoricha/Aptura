"""Tests for unpushed_commits.py, run against real throwaway git repositories (a bare "origin" and a clone), so they exercise
git itself and not a stand-in. They also prove the failure paths: a repository that cannot be read is reported, never read as
"all clear"."""

from __future__ import annotations

import contextlib
import io
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

import unpushed_commits as tool

# Isolate from the machine's git configuration (signing, hooks, default branch name) so the tests mean the same everywhere.
ENV = {
    **os.environ,
    "GIT_CONFIG_NOSYSTEM": "1",
    "GIT_CONFIG_GLOBAL": os.devnull,
    "GIT_AUTHOR_NAME": "Test",
    "GIT_AUTHOR_EMAIL": "test@example.invalid",
    "GIT_COMMITTER_NAME": "Test",
    "GIT_COMMITTER_EMAIL": "test@example.invalid",
}


def run(repo: Path, *args: str) -> str:
    done = subprocess.run(["git", "-C", str(repo), *args], capture_output=True, text=True, env=ENV, check=False)
    if done.returncode != 0:
        raise AssertionError(f"git {' '.join(args)} failed: {done.stderr}")
    return done.stdout


def commit(repo: Path, subject: str) -> None:
    (repo / "file.txt").write_text(subject + "\n", encoding="utf-8")
    run(repo, "add", "file.txt")
    run(repo, "-c", "commit.gpgsign=false", "commit", "-q", "-m", subject)


def check(repo: Path) -> tuple[int, str]:
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        code = tool.main(["--repo", str(repo)])
    return code, out.getvalue()


class UnpushedCommitsTest(unittest.TestCase):
    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        root = Path(self._tmp.name)
        self.origin = root / "origin.git"
        self.work = root / "work"
        run(root, "init", "-q", "--bare", "-b", "main", str(self.origin))
        run(root, "init", "-q", "-b", "main", str(self.work))
        run(self.work, "remote", "add", "origin", str(self.origin))
        commit(self.work, "first")
        run(self.work, "push", "-q", "-u", "origin", "main")

    def test_everything_pushed_is_clean(self):
        code, text = check(self.work)
        self.assertEqual(tool.CLEAN, code, text)
        self.assertNotIn("WARNING", text)

    def test_a_local_commit_on_a_pushed_branch_is_reported_with_its_subject(self):
        commit(self.work, "only on my disk")
        code, text = check(self.work)
        self.assertEqual(tool.UNPUSHED, code)
        self.assertIn("main: 1 commit(s)", text)
        self.assertIn("only on my disk", text)

    def test_pushing_it_clears_the_warning(self):
        commit(self.work, "soon pushed")
        self.assertEqual(tool.UNPUSHED, check(self.work)[0])
        run(self.work, "push", "-q", "origin", "main")
        self.assertEqual(tool.CLEAN, check(self.work)[0])

    def test_a_branch_that_was_never_pushed_is_reported(self):
        run(self.work, "switch", "-q", "-c", "feature")
        commit(self.work, "feature work")
        code, text = check(self.work)
        self.assertEqual(tool.UNPUSHED, code)
        self.assertIn("feature: 1 commit(s)", text)
        self.assertNotIn("main:", text)

    def test_a_new_branch_without_commits_of_its_own_is_clean(self):
        run(self.work, "switch", "-q", "-c", "empty")
        self.assertEqual(tool.CLEAN, check(self.work)[0])

    def test_a_commit_made_on_a_detached_head_is_reported(self):
        run(self.work, "switch", "-q", "--detach")
        commit(self.work, "detached work")
        code, text = check(self.work)
        self.assertEqual(tool.UNPUSHED, code)
        self.assertIn("HEAD (detached)", text)
        self.assertIn("detached work", text)

    def test_a_branch_is_clean_when_any_remote_branch_has_its_commits(self):
        # Pushed under another name: the commits are safe on the remote, so nothing is lost.
        run(self.work, "switch", "-q", "-c", "local-name")
        commit(self.work, "kept elsewhere")
        run(self.work, "push", "-q", "origin", "local-name:other-name")
        self.assertEqual(tool.CLEAN, check(self.work)[0])

    def test_many_commits_are_counted_exactly_but_only_a_few_listed(self):
        for i in range(tool.SHOWN_PER_BRANCH + 3):
            commit(self.work, f"change {i}")
        code, text = check(self.work)
        self.assertEqual(tool.UNPUSHED, code)
        self.assertIn(f"main: {tool.SHOWN_PER_BRANCH + 3} commit(s)", text)
        self.assertIn("... and 3 more", text)

    def test_a_repository_with_no_remote_says_so(self):
        alone = Path(self._tmp.name) / "alone"
        run(Path(self._tmp.name), "init", "-q", "-b", "main", str(alone))
        commit(alone, "never pushed anywhere")
        code, text = check(alone)
        self.assertEqual(tool.UNPUSHED, code)
        self.assertIn("no remote at all", text)

    def test_a_folder_that_is_not_a_repository_is_unknown_not_clean(self):
        plain = Path(self._tmp.name) / "plain"
        plain.mkdir()
        code, text = check(plain)
        self.assertEqual(tool.UNKNOWN, code)
        self.assertIn("could not check", text)

    def test_a_missing_folder_is_unknown_not_clean(self):
        code, text = check(Path(self._tmp.name) / "does-not-exist")
        self.assertEqual(tool.UNKNOWN, code)
        self.assertIn("could not check", text)


if __name__ == "__main__":
    unittest.main()
