"""Guard ensuring every GitHub Action in .github/workflows/ is pinned to a 40-char commit SHA (SEC-10)."""

from pathlib import Path
import re
import unittest

REPO_ROOT = Path(__file__).resolve().parents[2]
WORKFLOWS_DIR = REPO_ROOT / ".github" / "workflows"

USES_PATTERN = re.compile(r"^\s*(?:-\s*)?uses:\s*(.+?)\s*$")
PINNED_SHA_PATTERN = re.compile(r"^[^@\s]+@[0-9a-f]{40}(?:\s+#.*)?$")


def find_unpinned_actions(workflow_text: str, filename: str = "<string>") -> list[str]:
    """Return human-readable violations for any external action not pinned to a 40-char hex SHA."""
    violations: list[str] = []
    for line_no, raw_line in enumerate(workflow_text.splitlines(), start=1):
        match = USES_PATTERN.match(raw_line)
        if not match:
            continue
        spec = match.group(1).strip().strip("\"'")
        # Ignore repository-local actions (./...) if ever added; check all third-party/remote actions.
        if spec.startswith("./"):
            continue
        if not PINNED_SHA_PATTERN.match(spec):
            violations.append(f"{filename}:{line_no}: unpinned action '{spec}' (must use 40-char commit SHA)")
    return violations


class WorkflowActionPinsTest(unittest.TestCase):
    def test_all_workflows_pin_external_actions_to_40_char_shas(self) -> None:
        workflow_files = sorted(
            list(WORKFLOWS_DIR.glob("*.yml")) + list(WORKFLOWS_DIR.glob("*.yaml"))
        )
        self.assertTrue(workflow_files, f"No workflow files found in {WORKFLOWS_DIR}")
        all_violations: list[str] = []
        for wf in workflow_files:
            all_violations.extend(
                find_unpinned_actions(wf.read_text(encoding="utf-8"), wf.name)
            )
        self.assertEqual(
            [],
            all_violations,
            "Found unpinned GitHub Actions (SEC-10):\n" + "\n".join(all_violations),
        )

    def test_unpinned_tag_or_branch_ref_fails(self) -> None:
        sample = """
        steps:
          - uses: actions/checkout@v7
          - uses: some/action@main
        """
        violations = find_unpinned_actions(sample, "sample.yml")
        self.assertEqual(2, len(violations))

    def test_short_or_uppercase_or_non_hex_sha_fails(self) -> None:
        sample = """
        steps:
          - uses: actions/checkout@3d3c42e5aac
          - uses: actions/setup-java@ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ
        """
        violations = find_unpinned_actions(sample, "bad-sha.yml")
        self.assertEqual(2, len(violations))

    def test_pinned_40_char_sha_with_version_comment_passes(self) -> None:
        sample = """
        steps:
          - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7
        """
        self.assertEqual([], find_unpinned_actions(sample, "good.yml"))


if __name__ == "__main__":
    unittest.main()
