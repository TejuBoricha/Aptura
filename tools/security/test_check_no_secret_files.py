import os
import shutil
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import check_no_secret_files as check  # noqa: E402

SCRIPT = os.path.join(HERE, "check_no_secret_files.py")


class FindForbiddenTest(unittest.TestCase):
    def test_flags_every_key_and_certificate_type(self):
        names = [
            "upload-keystore.jks", "release.keystore", "key.p12", "cert.pfx", "key.pem",
            "AuthKey_ABC.p8", "server.key", "id_rsa", "id_ecdsa", "id_ed25519", ".env", ".env.production",
            "local.properties",
        ]
        self.assertEqual(check.find_forbidden(names), names)

    def test_finds_them_in_any_directory_and_any_case(self):
        paths = ["keystore/release.jks", "app/Release.KEYSTORE", "a/b/c/.ENV", "x/ID_RSA"]
        self.assertEqual(check.find_forbidden(paths), paths)

    def test_allows_documentation_and_placeholders(self):
        paths = [
            "distribution/play-store/metadata/keystore_setup_guide.md", "docs/key.md", "README.md",
            ".env.example", "config/.env.sample", "app/src/main/res/values/keys.xml", "tools/pem_notes.txt",
            "gradle/libs.versions.toml", "app/proguard-rules.pro",
        ]
        self.assertEqual(check.find_forbidden(paths), [])

    def test_empty_input_finds_nothing(self):
        self.assertEqual(check.find_forbidden([]), [])


def git(cwd, *args):
    subprocess.run(
        ["git", "-c", "user.name=t", "-c", "user.email=t@example.com", "-c", "commit.gpgsign=false", *args],
        cwd=cwd, check=True, capture_output=True,
    )


@unittest.skipUnless(shutil.which("git"), "git is required")
class CommandLineTest(unittest.TestCase):
    """Runs the script the way CI does, against a throwaway repository, to show it fails when it should."""

    def setUp(self):
        self.repo = tempfile.mkdtemp(prefix="secretcheck")
        git(self.repo, "init", "-q")
        with open(os.path.join(self.repo, ".gitignore"), "w") as f:
            f.write("*.jks\n")
        with open(os.path.join(self.repo, "README.md"), "w") as f:
            f.write("hello\n")
        git(self.repo, "add", ".")
        git(self.repo, "commit", "-q", "-m", "init")

    def tearDown(self):
        shutil.rmtree(self.repo, ignore_errors=True)

    def run_check(self, cwd=None):
        return subprocess.run([sys.executable, SCRIPT], cwd=cwd or self.repo, capture_output=True, text=True)

    def test_clean_repository_passes(self):
        result = self.run_check()
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_force_added_ignored_key_fails_and_is_named(self):
        # The case .gitignore cannot stop: `git add -f` on a file the rules ignore.
        with open(os.path.join(self.repo, "release.jks"), "w") as f:
            f.write("not a real key\n")
        git(self.repo, "add", "-f", "release.jks")
        git(self.repo, "commit", "-q", "-m", "oops")
        result = self.run_check()
        self.assertEqual(result.returncode, 1)
        self.assertIn("release.jks", result.stderr)
        self.assertNotIn("not a real key", result.stdout + result.stderr)

    def test_key_in_a_subdirectory_fails(self):
        os.makedirs(os.path.join(self.repo, "keystore"))
        with open(os.path.join(self.repo, "keystore", "upload.keystore"), "w") as f:
            f.write("x\n")
        git(self.repo, "add", "-f", "keystore/upload.keystore")
        result = self.run_check()
        self.assertEqual(result.returncode, 1)
        self.assertIn("keystore/upload.keystore", result.stderr)

    def test_an_untracked_ignored_key_is_not_reported(self):
        with open(os.path.join(self.repo, "local.jks"), "w") as f:
            f.write("x\n")
        self.assertEqual(self.run_check().returncode, 0)

    def test_outside_a_repository_it_refuses_to_pass(self):
        outside = tempfile.mkdtemp(prefix="notarepo")
        try:
            result = self.run_check(cwd=outside)
            self.assertEqual(result.returncode, 2)
        finally:
            shutil.rmtree(outside, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()
