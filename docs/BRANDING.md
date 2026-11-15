# Aptura branding

The owner approved the **Fold** mark on 15 November 2026 after reviewing it inside the app and on Android system surfaces (DEC-027, #214). It replaces the earlier aperture/play symbol. The app name, package, UI palette and functional controls are unchanged.

## Canonical assets

| Source | Export / use |
|---|---|
| [`assets/logo-mark.svg`](assets/logo-mark.svg) | Transparent colour mark; `logo-mark.png` is 1024 × 1024 |
| [`assets/logo-monochrome.svg`](assets/logo-monochrome.svg) | Flat mark using `currentColor`; use ink on light backgrounds and white on dark backgrounds |
| [`assets/logo.svg`](assets/logo.svg) | Mark on its dark rounded plate; `logo.png` is 1024 × 1024 |
| [`assets/banner.svg`](assets/banner.svg) | README banner; `banner.png` is 1600 × 600 |
| [`../distribution/play-store/icon.svg`](../distribution/play-store/icon.svg) | Store icon; `icon-512x512.png` is a square, opaque-background, 32-bit PNG. Outer corners are not baked in |
| [`../distribution/play-store/feature-graphic.svg`](../distribution/play-store/feature-graphic.svg) | Feature graphic; `feature-graphic-1024x500.png` is an opaque 24-bit PNG |

SVGs are the editable sources. The mark, plated logo and store-icon vectors use paths, gradients and a vector clip; they contain no fonts or bitmap payloads. The banner and feature graphic use system text and embed the vector mark. Their PNG exports are the fixed-layout distribution artifacts; use the PNGs where exact typography is needed. They were rasterized with Sharp; inspect all new exports before replacing distribution files.

## Shape and placement

Fold has one circular body and one enclosed angular opening. Ice/cyan planes suggest a continuous folded strip; the flat version preserves the same silhouette and opening. Do not add outlines or decorative seams, stretch the circle, fill the opening, or use the logo in place of play/pause and other functional controls.

Keep external clear space of at least one quarter of the disc diameter in layouts. The colour-mark SVG's small canvas margin is export padding, not the full layout clear space. The flat symbol has been reviewed from 16 to 128 px. Prefer colour at 32 px or larger, where its planes are easier to distinguish. The existing in-app tile sizes remain 26/32/34/40/64 dp; retain the dark tile in both app themes for contrast.

The Android foreground uses a 108-unit viewport, centre (54,54) and radius 28.4766, matching the old mark's optical bounds inside the 66 dp safe circle. Android controls the outer adaptive-icon mask. Both launcher resources reference `ic_launcher_monochrome.xml` for monochrome tinting. The flat drawable contains one solid path with a transparent opening, with no colour gradients or plate. SVGs use the explicit even-odd fill/clip rule; the Android version also reverses the opening's contour so the clip works with either winding rule.

## Rollout and provenance

The app's shared `ApturaMark` uses the launcher foreground for onboarding, phone loading/empty headers, tablet sidebars and compact rails. System launcher, app information, Recents and cold launch obtain the app icon from the manifest. Repository screenshots containing the old mark are regenerated from the real Compose gallery, not patched with a logo overlay. Other playback and settings screenshots do not contain the brand mark and need no logo-only change.

Repository store assets are upload-ready files, not evidence of an updated Play Console listing. An existing published listing, developer profile or external avatar needs its own update. No release version or signing setting changes with this branding change.

Concept development used built-in image generation, followed by native vector reconstruction, optical adjustments, monochrome/size proofs and app/system rendering. No third-party stock vector paths were imported. Keyword similarity screening was preliminary; neither independent construction nor owner approval establishes worldwide uniqueness, trademark clearance or non-infringement. No such claim is made by this document.

## DEC-027 change impact

| Field | Impact |
|---|---|
| Change | Aperture/play identity → Fold; consistent SVG, raster, Android and screenshot assets |
| Reason | Explicit owner approval following context review, #214 |
| IDs | DEC-027 added; no existing identifiers renumbered |
| UX | Brand identity only; shared mark wrappers, dimensions, controls and navigation retained |
| Data | No schema, settings, file or migration change |
| Security/privacy | No permissions, dependencies, telemetry or data-flow change |
| Licensing | No runtime library change; no assertion of exclusive rights or legal clearance |
| Tests | Existing native Compose gallery, unit/lint/release gates; manual Android icon/context review |
| Claims | Existing offline/no-ads/no-tracking promises retained; no ranking, format or exclusivity claim added |
| Risk | OEM masks, themed icons and physical-device rendering require device verification; broad similarity screening is not clearance |
| Approvals | Owner approved the design and its replacement after context review on 15 November 2026 |
