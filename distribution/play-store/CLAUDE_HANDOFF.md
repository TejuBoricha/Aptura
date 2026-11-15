# Approved artwork handoff

The owner explicitly approved **all of Preview 02** in this chat on 15 November 2026. The previous visual-review hold is lifted. Use the existing approved artwork; do not regenerate it or request the same design approval again.

The local branch is `agent/codex/store-listing-v2`. The canonical artwork and related documentation are staged, not committed or pushed. Complete the repository's normal commit/PR/CI workflow. Leave the unrelated, pre-existing untracked `docs/assets/brand/` proposals out of that commit.

Read `distribution/play-store/README.md` for the upload inventory, numeric order, editable sources, provenance and evidence. Upload PNGs from these locations:

- `distribution/play-store/phone-screenshots/`: eight 1080×1920 images.
- `distribution/play-store/tablet-screenshots/`: four 1920×1080 images, for the applicable tablet device categories.
- `distribution/play-store/feature-graphic-1024x500.png`: separate feature graphic.
- `distribution/play-store/icon-512x512.png`: the approved Fold icon, unchanged.
- Suggested descriptions: `distribution/play-store/metadata/screenshot-alt-text.csv`.

Replace the earlier listing images instead of appending another set. The review boards and SVG source files are not upload assets. Preserve the approved ordering, layout, text and logo. Check Console previews after upload. Report whether the edits were saved, submitted, or made publicly visible; those are separate states.

This handoff concerns listing artwork. Keep the app release, version, signing configuration and listing descriptions unchanged. Codex has not uploaded or published anything in Play Console.

## QA scorecard

Review follows `.agents/rules/qa-and-code-review.md`.

| Pillar | Assessment |
|---|---|
| Invariants and compliance | No application, permission, dependency, codec or data-flow changes; existing Fold icon retained. |
| Runtime boundaries and error handling | Not applicable to this artwork-only change; no runtime behavior modified. |
| Visual quality and resources | Owner-approved; native capture proportions preserved; full-size and thumbnail review; all copied PNGs match the approved exports byte for byte. Precision is the densest panel. |
| Verification | `tools/gate.ps1` passed: 1,304 unit/Robolectric tests, zero failures/errors, one skipped test; debug lint and guard suites passed. Release APK/AAB rebuilds were automatically skipped because this is an artwork/documentation change. Export and typography reports are in `review/`; local gate log is `build/review/listing-v2/integration-gate.log`. CI and physical-device testing have not been rerun for this integration. |

Self-contained SVG sources are included with the proposed assets, so rendering them does not depend on ignored build folders. The local compositor and original capture workflow remain under `build/review/listing-v2/`, with some inputs in `build/review/listing-v1/`.
