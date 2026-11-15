"""Dependency-free checks for README links, source drift and presentation exports.

WebP dimensions follow developers.google.com/speed/webp/docs/riff_container.
This reads image headers; it does not replace visual review or decode pixels.
"""
from __future__ import annotations

import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import struct
import sys
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[2]
PRIMARY_BUDGET = 1_200_000


def source_digest(path: Path) -> str:
    data = path.read_bytes()
    if path.suffix in {'.svg', '.mjs'}:
        data = data.replace(b'\r\n', b'\n')
    return hashlib.sha256(data).hexdigest()


def dimensions(data: bytes) -> tuple[int, int]:
    if data.startswith(b'\x89PNG\r\n\x1a\n') and len(data) >= 24:
        return struct.unpack('>II', data[16:24])
    if len(data) < 20 or data[:4] != b'RIFF' or data[8:12] != b'WEBP':
        raise ValueError('unsupported or truncated image header')
    if int.from_bytes(data[4:8], 'little') + 8 != len(data):
        raise ValueError('truncated or inconsistent RIFF size')
    offset = 12
    while offset + 8 <= len(data):
        kind = data[offset:offset + 4]
        size = int.from_bytes(data[offset + 4:offset + 8], 'little')
        chunk = data[offset + 8:offset + 8 + size]
        if len(chunk) != size:
            raise ValueError('truncated WebP chunk')
        if kind == b'VP8X' and len(chunk) >= 10:
            return (1 + int.from_bytes(chunk[4:7], 'little'), 1 + int.from_bytes(chunk[7:10], 'little'))
        if kind == b'VP8 ' and len(chunk) >= 10 and chunk[3:6] == b'\x9d\x01\x2a':
            return (int.from_bytes(chunk[6:8], 'little') & 0x3fff, int.from_bytes(chunk[8:10], 'little') & 0x3fff)
        if kind == b'VP8L' and len(chunk) >= 5 and chunk[0] == 0x2f:
            bits = int.from_bytes(chunk[1:5], 'little')
            return (1 + (bits & 0x3fff), 1 + ((bits >> 14) & 0x3fff))
        offset += 8 + size + size % 2
    raise ValueError('WebP dimensions not found')


class Markup(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []
        self.errors = []
        self.pictures = 0

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag in {'script', 'iframe', 'style'} or any(k.startswith('on') or k == 'style' for k in attrs):
            self.errors.append('unsupported active/styled README markup')
        if tag == 'picture':
            self.pictures += 1
        for attr in ['src', 'href', 'srcset']:
            if attrs.get(attr):
                self.links.append(attrs[attr])
        if tag == 'img' and len(attrs.get('alt', '').strip()) < 12:
            self.errors.append('image needs meaningful alternative text')


def local_link_error(root: Path, source: Path, link: str) -> str | None:
    url = urlsplit(link)
    if url.scheme or url.netloc:
        return None
    target = (source.parent / unquote(url.path)).resolve() if url.path else source.resolve()
    if not target.is_relative_to(root.resolve()):
        return f'link escapes repository: {link}'
    if not target.exists():
        return f'missing local link: {link}'
    if url.fragment and target.suffix == '.md':
        headings = re.findall(r'^#{1,6}\s+(.+?)\s*$', target.read_text(encoding='utf-8'), re.M)
        slugs = {re.sub(r'[^\w\- ]', '', h.lower()).replace(' ', '-') for h in headings}
        if unquote(url.fragment) not in slugs:
            return f'missing Markdown anchor: {link}'
    return None


def audit(root: Path = ROOT) -> dict:
    errors, count, primary = [], 0, 0
    manifest_path = root / 'docs/assets/showcase/manifest.json'
    try:
        manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
        if not manifest['inputs'] or not manifest['outputs'] or not manifest['placements']:
            errors.append('manifest has no source, exports or native placements to check')
        if len({e['file'] for e in manifest['outputs']}) != len(manifest['outputs']):
            errors.append('duplicate exports in manifest')
        for file, expected in manifest['inputs'].items():
            p = (root / file).resolve()
            if not p.is_relative_to(root.resolve()) or not p.is_file():
                errors.append(f'missing or unsafe input: {file}')
            elif source_digest(p) != expected:
                errors.append(f'source changed; regenerate showcase: {file}')
        for entry in manifest['outputs']:
            file = entry['file']
            p = (root / file).resolve()
            if not p.is_relative_to(root.resolve()) or not p.is_file():
                errors.append(f'missing or unsafe export: {file}')
                continue
            data = p.read_bytes()
            if hashlib.sha256(data).hexdigest() != entry['sha256'] or len(data) != entry['bytes']:
                errors.append(f'export differs from manifest: {file}')
            if dimensions(data) != (entry['width'], entry['height']):
                errors.append(f'export dimensions differ: {file}')
            if not entry.get('native') and not entry.get('social'):
                primary += len(data)
            count += 1
        if primary > PRIMARY_BUDGET:
            errors.append('six primary responsive images exceed the 1.2 MB budget')
        for rect in manifest['placements']:
            if abs(rect['width'] / rect['height'] - rect['sourceWidth'] / rect['sourceHeight']) > 0.00001:
                errors.append(f'distorted native UI: {rect["label"]}')
            if min(rect['x'], rect['y']) < 5 or rect['x'] + rect['width'] > rect['canvas']['width'] - 5 or rect['y'] + rect['height'] > rect['canvas']['height'] - 5:
                errors.append(f'clipped native UI: {rect["label"]}')
    except (OSError, ValueError, KeyError, TypeError, ZeroDivisionError) as exc:
        errors.append(f'cannot validate manifest: {exc}')
    for rel in ['README.md', 'docs/FEATURES.md', 'docs/BUILDING.md', 'docs/PRESENTATION.md']:
        source = root / rel
        try:
            content = source.read_text(encoding='utf-8')
        except OSError as exc:
            errors.append(str(exc))
            continue
        prose = re.sub(r'```[\s\S]*?```', '', content)
        prose = re.sub(r'`[^`\n]+`', '', prose)
        markup = Markup()
        markup.feed(prose)
        errors.extend(f'{rel}: {e}' for e in markup.errors)
        links = markup.links + re.findall(r'(?<!!)\[[^\]\n]+\]\(([^)\s]+)\)', prose)
        links += re.findall(r'!\[[^\]\n]*\]\(([^)\s]+)\)', prose)
        for link in links:
            issue = local_link_error(root, source, link)
            if issue:
                errors.append(f'{rel}: {issue}')
        if rel == 'README.md' and markup.pictures != 3:
            errors.append('README must have three deliberate responsive picture groups')
    return dict(status='passed' if not errors else 'failed', exports=count, primary_image_bytes=primary, errors=errors)


if __name__ == '__main__':
    result = audit()
    print(json.dumps(result, indent=2))
    raise SystemExit(result['status'] != 'passed')
