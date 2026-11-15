import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import struct
import tempfile
import unittest
from unittest import mock

spec = importlib.util.spec_from_file_location('check_showcase', Path(__file__).with_name('check_showcase.py'))
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)


def webp(kind, payload):
    chunk = kind + struct.pack('<I', len(payload)) + payload + (b'\0' if len(payload) % 2 else b'')
    return b'RIFF' + struct.pack('<I', 4 + len(chunk)) + b'WEBP' + chunk


class ShowcaseChecks(unittest.TestCase):
    def test_actual_repository_passes(self):
        self.assertEqual(check.audit()['errors'], [])

    def test_three_webp_header_formats(self):
        lossy = b'\0\0\0\x9d\x01\x2a' + struct.pack('<HH', 900, 1380)
        lossless = b'\x2f' + struct.pack('<I', 899 | (1379 << 14))
        extended = b'\0' * 4 + (899).to_bytes(3, 'little') + (1379).to_bytes(3, 'little')
        for kind, data in [(b'VP8 ', lossy), (b'VP8L', lossless), (b'VP8X', extended)]:
            with self.subTest(kind=kind):
                self.assertEqual(check.dimensions(webp(kind, data)), (900, 1380))

    def test_invalid_and_truncated_headers_fail(self):
        for data in [b'', b'not an image', webp(b'VP8 ', b'bad'), webp(b'VP8L', b'\x2f\0\0\0\0')[:-1]]:
            with self.subTest(data=data):
                with self.assertRaises(ValueError):
                    check.dimensions(data)

    def test_alt_text_and_active_markup_fail(self):
        parser = check.Markup()
        parser.feed('<img src="x.webp" alt=""><script>alert(1)</script>')
        self.assertEqual(len(parser.errors), 2)

    def test_text_input_hashes_ignore_windows_line_endings(self):
        with tempfile.TemporaryDirectory() as tmp:
            file = Path(tmp) / 'source.svg'
            file.write_bytes(b'<svg>\r\n</svg>\r\n')
            windows = check.source_digest(file)
            file.write_bytes(b'<svg>\n</svg>\n')
            self.assertEqual(windows, check.source_digest(file))

    def test_budget_guard_fails_at_a_lower_limit(self):
        with mock.patch.object(check, 'PRIMARY_BUDGET', 1):
            self.assertTrue(any('budget' in e for e in check.audit()['errors']))

    def test_clipped_distorted_and_empty_manifest_fail(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            path = root / 'docs/assets/showcase/manifest.json'
            path.parent.mkdir(parents=True)
            data = {'inputs': {}, 'outputs': [], 'placements': [{'label':'broken', 'x':0, 'y':0, 'width':100, 'height':50, 'sourceWidth':100, 'sourceHeight':100, 'canvas':{'width':90, 'height':90}}]}
            path.write_text(json.dumps(data), encoding='utf-8')
            errors = check.audit(root)['errors']
            for expected in ['no source', 'distorted', 'clipped']:
                self.assertTrue(any(expected in e for e in errors))

    def test_missing_link_anchor_and_escape_fail(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / 'README.md'
            source.write_text('# Present\n', encoding='utf-8')
            self.assertIsNone(check.local_link_error(root, source, '#present'))
            for link in ['missing.md', '#absent', '../outside.md']:
                self.assertIsNotNone(check.local_link_error(root, source, link))

    def test_corrupt_export_and_source_drift_fail(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for folder in ['docs/assets/showcase', 'tools/presentation']:
                (root / folder).mkdir(parents=True)
            manifest = json.loads((check.ROOT / 'docs/assets/showcase/manifest.json').read_text(encoding='utf-8'))
            entry = manifest['outputs'][0]
            file = root / entry['file']
            shutil.copyfile(check.ROOT / entry['file'], file)
            file.write_bytes(file.read_bytes() + b'corruption')
            minimal = {'inputs': {'tools/presentation/source.txt': hashlib.sha256(b'old').hexdigest()}, 'outputs': [entry], 'placements': []}
            (root / 'tools/presentation/source.txt').write_text('changed', encoding='utf-8')
            (root / 'docs/assets/showcase/manifest.json').write_text(json.dumps(minimal), encoding='utf-8')
            result = check.audit(root)
            self.assertEqual(result['status'], 'failed')
            self.assertTrue(any('source changed' in x for x in result['errors']))
            self.assertTrue(any('export differs' in x for x in result['errors']))


if __name__ == '__main__':
    unittest.main()
