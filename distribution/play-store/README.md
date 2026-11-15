# Approved Aptura store listing artwork

The owner approved **all of Preview 02, “Every frame. All yours.”, on 15 November 2026**. This is the canonical approved set. The earlier instruction to wait for visual approval is superseded. The existing Fold logo is retained.

## Upload files and order

Upload the PNGs in numeric filename order. Use the eight files in `phone-screenshots/` for phones and the four files in `tablet-screenshots/` for tablet categories. Phone exports are 1080 × 1920; tablet exports are 1920 × 1080. Both are RGB PNGs without alpha.

The feature image is `feature-graphic-1024x500.png` (1024 × 500, RGB PNG). The existing `icon-512x512.png` is unchanged. Suggested accessible descriptions, including the feature image, are in `metadata/screenshot-alt-text.csv`.

Replace the previous image set rather than appending these to it. The approved sequence is:

| Phone | Story |
|---|---|
| 01 | Local library — Every frame. All yours. |
| 02 | Playback speed and audio-only playback |
| 03 | Equalizer and sound |
| 04 | Subtitle style and readability |
| 05 | Six accents and themes |
| 06 | Saved progress and resume |
| 07 | Loop, frame stepping and bookmarks |
| 08 | Offline architecture and privacy |

The tablet sequence is dark library, light library, list view and settings. Review the Console's previews for each chosen device category and confirm ordering and image descriptions after upload. No Console upload or publication has been performed by Codex.

## Sources and product accuracy

Self-contained editable SVG layouts are in `editable/phone/`, `editable/tablet/` and `feature-graphic.svg`. The approved PNGs are the fixed-layout reference. SVGs embed the captures and artwork; Arial and Georgia are system fonts and are not embedded.

UI comes from Aptura's real Compose components with illustrative demo state, plus an actual Android player capture. Crops preserve the native proportions. The folded material background and sample scenes were generated artwork; the app controls were not generated or repainted.

There are eight distinct demo file titles and images. The same file correctly retains its title and thumbnail in both Continue watching and Library. The imagery illustrates personal local files, not an included streaming catalogue. Metadata is illustrative.

## Review and verification

`review/` contains the approved overview, four-tablet board, thumbnail proof, compared creative directions and export/type reports. These boards and JSON files are not upload assets.

The 13 screenshot/feature exports passed size, RGB and no-alpha checks. Native UI occupies approximately 51%, 55.5% and 56.5% of the first three phone canvases. System-font estimates checked 83 text elements for canvas overflow, alongside full-size and thumbnail visual review. The precision panel remains the densest panel; inspect it full-size.

Asset requirements were checked against [Google Play preview asset guidance](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en). Technical checks and owner approval do not mean the assets have been reviewed by Google.

The local authoring workspace remains at `build/review/listing-v2/` and reuses some captures from `build/review/listing-v1/`. The canonical SVGs here are self-contained and do not require those ignored build folders to render. No temporary capture fixture remains in application source.

QA review follows `.agents/rules/qa-and-code-review.md`: runtime/privacy/storage/codec invariants and error boundaries have no code changes; image proportions, typography, source fidelity and export formats were reviewed. Physical device behavior was not retested for this artwork-only change. The local gate result belongs in the eventual PR verification section.

## Handoff to Claude

All visual assets are owner-approved. Use these canonical PNGs without regenerating or redesigning them. Complete the repository's normal commit, PR, CI and merge workflow for the current asset changes, then update the Play Console artwork using the sequence above. Keep the app release, version, signing configuration and listing text unchanged. Report separately what was integrated in Git and what was saved or submitted in Play Console; do not claim an upload from local file replacement alone.
