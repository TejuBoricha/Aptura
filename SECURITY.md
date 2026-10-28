# Security policy

## Supported versions

Aptura is pre-release. Security fixes land on `main` and in the next tagged build. There are no maintained release branches yet.

| Version | Supported |
|---|---|
| `main` (1.0.0 pre-release) | Yes |
| Older commits / forks | No |

## Reporting a vulnerability

Please do not open a public issue.

1. Use **Security → Report a vulnerability** on GitHub: <https://github.com/TejuBoricha/Aptura/security/advisories/new>.
2. If private reporting is unavailable, contact the maintainer through GitHub ([@TejuBoricha](https://github.com/TejuBoricha)) and ask for a private channel. Do not include exploit details in a public message.

Include:

- affected version or commit;
- Android version and device;
- reproduction steps or a proof of concept;
- impact: what an attacker gains, and what access they need;
- any suggested fix.

You will get an acknowledgement as soon as possible, normally within a few days. This is a volunteer project, so no bounty is offered. Credit is given in the advisory and release notes unless you prefer to stay anonymous.

## What is in scope

- The manifest and permission model, including any way to add network capability or broad storage access.
- Exported components and intent handling (`ACTION_VIEW`, share, open-with).
- File handling: SAF tree access, rename/move/delete, subtitle import (path traversal, zip bombs, malformed archives), MIME and extension confusion.
- Data exposure through diagnostics, logs, notifications, PiP, or the recents thumbnail.
- Redaction failures that leak paths, file names, email addresses or IP addresses.
- Dependency vulnerabilities that affect the shipped release build.
- Release-build invariant bypasses (a network or telemetry library that survives R8).

## What is out of scope

- Crashes or playback failures caused by a specific codec, GPU or OEM firmware, unless they expose data or bypass a permission.
- Attacks that require a rooted device, a compromised OS, or physical access to an unlocked device.
- Denial of service from intentionally enormous media files, unless it corrupts data or bypasses a limit.
- Issues in third-party apps that receive a file through the share sheet.
- Reports generated solely by a scanner without a reproduction path.

## Design guarantees

Aptura's security posture is deliberately small:

- no `INTERNET` permission and no network stack in the release build;
- no telemetry, ads or accounts;
- scoped storage only, no `MANAGE_EXTERNAL_STORAGE`;
- no Android backup (`allowBackup="false"`);
- local diagnostics with redaction and an explicit delete action;
- release builds verified by reading the R8 mapping and the shipped dex.

These are enforced by tests and the release build, so a regression is a security issue, not only a bug.
