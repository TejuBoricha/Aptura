## What changed

<!-- A short description of the behavior change, not a list of files. -->

## Why

<!-- The problem this solves. Link the issue if there is one. -->

## Verification

- [ ] `tools/gate.ps1` passes locally
- [ ] Unit / Robolectric tests added or updated
- [ ] Instrumented tests run, if the change needs a device
- [ ] Gallery render regenerated, if a screen or sheet changed
- [ ] Not verified on a physical device (explain what remains)

## Areas touched

- [ ] Library / scanning
- [ ] Playback
- [ ] Audio / subtitles
- [ ] Settings / appearance
- [ ] File actions
- [ ] Diagnostics / privacy
- [ ] Documentation / tooling

## Screenshots

<!-- Before/after or after-only. Say whether these are Robolectric renders or device captures. -->

## Checklist

- [ ] The change keeps the zero-network, zero-telemetry and scoped-storage invariants.
- [ ] No new permission was added without discussion.
- [ ] No software video decoder was bundled.
- [ ] User-visible copy is sentence case and states the consequence of destructive actions.
- [ ] Documentation was updated when a contract, screen or workflow changed.
