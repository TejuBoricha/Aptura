"""Tests for pr_checks.py. Run with: python3 -m unittest discover -s tools/agents"""

import contextlib
import io
import sys
import unittest
import unittest.mock
import urllib.error
from datetime import datetime, timedelta, timezone
from pathlib import Path

import pr_checks
from pr_checks import PullRequest, check, issue_refs, is_hot, load_hot_patterns, unattributed

NOW = datetime(2026, 10, 28, 12, tzinfo=timezone.utc)
PATTERNS = load_hot_patterns()
ATTRIBUTED = [("a" * 40, "Do a thing\n\nAssisted-by: Codex CLI (gpt-5.5)\nRefs: #41")]


def pr(number, files, title="t", body="## Handoff\n", draft=False, age_days=0, commits=()):
    return PullRequest(number, title, body, draft, NOW - timedelta(days=age_days), list(files), list(commits))


def levels(findings):
    return sorted(f.level for f in findings)


class AttributionTest(unittest.TestCase):
    def test_assisted_by_and_human_authored_pass(self):
        self.assertEqual(unattributed([
            ("1" * 40, "Fix\n\nAssisted-by: OpenCode (DeepSeek V4.1 Flash Free)"),
            ("2" * 40, "Fix\n\nassisted-by: Claude Code"),
            ("3" * 40, "Typo\n\nHuman-authored: yes"),
        ]), [])

    def test_missing_or_empty_trailer_fails(self):
        self.assertEqual(unattributed([
            ("abcdef1234", "Fix\n\nCo-Authored-By: Someone <a@b>"),
            ("bcdefa1234", "Fix\n\nAssisted-by:"),
            ("cdefab1234", "Fix the Assisted-by: handling in prose"),
            ("defabc1234", "Fix\n\nAssisted-by:\nRefs: #4"),
        ]), ["abcdef1", "bcdefa1", "cdefab1", "defabc1"])

    def test_bot_commits_are_recognised(self):
        self.assertTrue(pr_checks.is_bot_commit({"author": {"login": "dependabot[bot]", "type": "Bot"}}))
        self.assertTrue(pr_checks.is_bot_commit({"author": {"login": "renovate[bot]", "type": "User"}}))
        self.assertFalse(pr_checks.is_bot_commit({"author": {"login": "TejuBoricha", "type": "User"}}))
        self.assertFalse(pr_checks.is_bot_commit({"author": None}))

    def test_unattributed_commit_blocks(self):
        findings = check(pr(5, ["README.md"]), [], [("deadbeef00", "Fix")], PATTERNS, NOW)
        self.assertEqual(levels(findings), ["error"])


class HotFileTest(unittest.TestCase):
    def test_patterns_cover_schema_database_and_ci(self):
        self.assertTrue(is_hot("app/schemas/org.aptura.player.data.database.ApturaDatabase/7.json", PATTERNS))
        self.assertTrue(is_hot("app/src/main/java/org/aptura/player/data/database/ApturaDatabase.kt", PATTERNS))
        self.assertTrue(is_hot(".github/workflows/ci.yml", PATTERNS))
        self.assertFalse(is_hot("app/src/main/java/org/aptura/player/ui/player/PlayerViewModel.kt", PATTERNS))

    def test_older_pr_on_same_hot_file_blocks_the_newer_one_only(self):
        migration = "app/schemas/org.aptura.player.data.database.ApturaDatabase/7.json"
        older, newer = pr(4, [migration]), pr(6, [migration])
        self.assertIn("error", levels(check(newer, [older], ATTRIBUTED, PATTERNS, NOW)))
        self.assertNotIn("error", levels(check(older, [newer], ATTRIBUTED, PATTERNS, NOW)))

    def test_abandoned_older_pr_only_warns(self):
        older = pr(4, ["gradle/libs.versions.toml"], age_days=30)
        findings = check(pr(6, ["gradle/libs.versions.toml"]), [older], ATTRIBUTED, PATTERNS, NOW)
        self.assertEqual(levels(findings), ["warning"])

    def test_every_hot_pattern_is_named_in_agents_md(self):
        agents = (Path(pr_checks.__file__).parents[2] / "AGENTS.md").read_text(encoding="utf-8")
        for pattern in PATTERNS:
            self.assertIn(pattern.removesuffix("/*").removesuffix("*"), agents, pattern)


class DuplicateWorkTest(unittest.TestCase):
    def test_issue_refs(self):
        self.assertEqual(issue_refs("Closes #12, #13 and #14. Refs: #9. - Issue: #20. Issue: none"),
                         {9, 12, 13, 14, 20})
        self.assertEqual(issue_refs("see #12 and PR #3"), set())

    def test_two_prs_for_the_same_issue(self):
        older = pr(4, ["a.kt"], body="## Handoff\nRefs #41")
        newer = pr(6, ["b.kt"], commits=["Fix\n\nRefs: #41"])
        self.assertIn("error", levels(check(newer, [older], ATTRIBUTED, PATTERNS, NOW)))

    def test_issue_named_only_in_the_older_prs_commits_still_counts(self):
        older = pr(4, ["a.kt"], body="## Handoff\n- Issue:", commits=["Fix\n\nAssisted-by: X\nRefs: #41"])
        newer = pr(6, ["b.kt"], commits=["Fix\n\nRefs: #41"])
        self.assertIn("error", levels(check(newer, [older], ATTRIBUTED, PATTERNS, NOW)))

    def test_likely_duplicate_is_flagged_even_when_every_shared_file_is_hot(self):
        files = ["app/build.gradle.kts", "gradle/libs.versions.toml"]
        findings = check(pr(6, files), [pr(4, files, age_days=30)], ATTRIBUTED, PATTERNS, NOW)
        self.assertTrue(any("same task" in f.message for f in findings))

    def test_ordinary_overlap_warns_and_flags_a_likely_duplicate(self):
        older = pr(4, ["ui/A.kt", "ui/B.kt", "ui/C.kt"])
        findings = check(pr(6, ["ui/A.kt", "ui/B.kt"]), [older], ATTRIBUTED, PATTERNS, NOW)
        self.assertEqual(levels(findings), ["warning", "warning"])

    def test_no_overlap_is_clean(self):
        self.assertEqual(check(pr(6, ["ui/A.kt"]), [pr(4, ["ui/B.kt"])], ATTRIBUTED, PATTERNS, NOW), [])


class FetchTest(unittest.TestCase):
    """fetch() with the GitHub API faked: renames and the base branch."""

    def setUp(self):
        stamp = "2026-10-28T12:00:00Z"
        pulls = {
            6: {"number": 6, "title": "t", "body": "", "draft": False, "updated_at": stamp, "base": {"ref": "main"}},
            4: {"number": 4, "title": "t", "body": "", "draft": False, "updated_at": stamp, "base": {"ref": "main"}},
            5: {"number": 5, "title": "t", "body": "", "draft": False, "updated_at": stamp,
                "base": {"ref": "release/1.0"}},
        }
        files = {6: [{"filename": "app/build.gradle.kts"}],
                 4: [{"filename": "data/db/ApturaDatabase.kt",
                      "previous_filename": "app/src/main/java/org/aptura/player/data/database/ApturaDatabase.kt"}],
                 5: [{"filename": "app/build.gradle.kts"}]}
        commit = {"sha": "f" * 40, "parents": [{}], "commit": {"message": "x\n\nAssisted-by: T"}}

        def fake_get(url, token):
            return pulls[int(url.rsplit("/", 1)[1])]

        def fake_pages(url, token, limit=10):
            if url.endswith("pulls?state=open"):
                return list(pulls.values())
            number = int(url.split("/pulls/")[1].split("/")[0])
            return files[number] if url.endswith("/files") else [commit]

        self.patches = [unittest.mock.patch.object(pr_checks, "_get", fake_get),
                        unittest.mock.patch.object(pr_checks, "_pages", fake_pages)]
        for patch in self.patches:
            patch.start()

    def tearDown(self):
        for patch in self.patches:
            patch.stop()

    def test_rename_counts_the_old_path_and_other_bases_are_ignored(self):
        pr6, others, _ = pr_checks.fetch("o/r", 6, None)
        self.assertEqual([o.number for o in others], [4])
        self.assertIn("app/src/main/java/org/aptura/player/data/database/ApturaDatabase.kt", others[0].files)


class OutageTest(unittest.TestCase):
    """Only a GitHub outage or rate limit may skip the checks; a permission or token problem must fail."""

    @staticmethod
    def http(code, remaining=None):
        headers = {"X-RateLimit-Remaining": remaining} if remaining is not None else {}
        return urllib.error.HTTPError("https://api.github.com/x", code, "msg", headers, None)

    def test_outages_and_rate_limits_skip(self):
        secondary = urllib.error.HTTPError("https://api.github.com/x", 403, "msg", {"Retry-After": "60"}, None)
        for error in (self.http(502), self.http(503), self.http(429), self.http(403, "0"), secondary,
                      urllib.error.URLError("unreachable"), TimeoutError()):
            self.assertTrue(pr_checks.is_outage(error), error)

    def test_permission_token_and_missing_pr_errors_fail(self):
        for error in (self.http(401), self.http(403, "4999"), self.http(403), self.http(404)):
            self.assertFalse(pr_checks.is_outage(error), error)


class MainFailureModeTest(unittest.TestCase):
    """main() end to end with GitHub's answer faked: the whole decision path, not only is_outage()."""

    def run_main(self, code, headers):
        def fake_urlopen(request, timeout=30):
            raise urllib.error.HTTPError(request.full_url, code, "msg", headers, io.BytesIO(b"{}"))

        with unittest.mock.patch.object(pr_checks.urllib.request, "urlopen", fake_urlopen), \
                unittest.mock.patch.object(pr_checks.time, "sleep", lambda seconds: None), \
                unittest.mock.patch.object(sys, "argv", ["pr_checks.py", "--repo", "o/r", "--pr", "5"]), \
                contextlib.redirect_stdout(io.StringIO()):
            return pr_checks.main()

    def test_bad_token_and_missing_permission_block(self):
        self.assertEqual(self.run_main(401, {}), 1)
        self.assertEqual(self.run_main(403, {"X-RateLimit-Remaining": "4999"}), 1)
        self.assertEqual(self.run_main(404, {}), 1)

    def test_outage_and_rate_limit_skip_without_blocking(self):
        self.assertEqual(self.run_main(503, {}), 0)
        self.assertEqual(self.run_main(403, {"X-RateLimit-Remaining": "0"}), 0)
        self.assertEqual(self.run_main(403, {"X-RateLimit-Remaining": "4000", "Retry-After": "60"}), 0)

    def test_permission_errors_are_not_retried(self):
        calls = []

        def fake_urlopen(request, timeout=30):
            calls.append(request.full_url)
            # A permission 403: no exhausted counter, no Retry-After. The old code retried every 403.
            raise urllib.error.HTTPError(request.full_url, 403, "msg", {"X-RateLimit-Remaining": "4999"},
                                         io.BytesIO(b"{}"))

        with unittest.mock.patch.object(pr_checks.urllib.request, "urlopen", fake_urlopen), \
                unittest.mock.patch.object(pr_checks.time, "sleep", lambda seconds: calls.append("sleep")):
            with self.assertRaises(urllib.error.HTTPError):
                pr_checks._get("https://api.github.com/x", None)
        self.assertEqual(calls, ["https://api.github.com/x"])


class HandoffTest(unittest.TestCase):
    def test_draft_without_handoff_warns(self):
        findings = check(pr(6, ["ui/A.kt"], body="WIP", draft=True), [], ATTRIBUTED, PATTERNS, NOW)
        self.assertEqual(levels(findings), ["warning"])


if __name__ == "__main__":
    unittest.main()
