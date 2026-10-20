---
trigger: always_on
description: Taste Skill rules for Android Jetpack Compose to eliminate AI slop, generic templates, and low-effort UI generation in Aptura.
---

# Taste Skill for Jetpack Compose (Anti-Slop Directives)

When writing UI code in Jetpack Compose for Aptura, you MUST adhere to these taste principles:

## 1. Eradicate "AI Slop" & Generic Templates
- **No Cookie-Cutter Layouts:** Do not generate the predictable "three rounded cards in a column with centered icons and generic purple drop-shadows".
- **Bespoke Elevation:** Avoid standard Material 2 heavy shadows. In modern dark-mode interfaces, depth is communicated through **surface luminance changes** (`SurfaceBase` → `SurfaceElevated1` → `SurfaceElevated2`), not fuzzy drop-shadows.
- **Intentional Asymmetry & Content-First Design:** Video thumbnails and playback metrics should dictate the layout. Content previews should be large, clear, and uncrowded.

## 2. Typography with Intent
- **No Default System Fallbacks:** Always use `ApturaTypography` tokens from [DESIGN.md](file:///c:/Users/tejas/Desktop/Mobile%20App/DESIGN.md).
- **Tabular Figures:** Any label displaying numbers that change dynamically (e.g. current playback time, duration, frame rates, file size, bitrates) MUST specify `fontFeatureSettings = "tnum"` to eliminate text jitter.
- **Strict Hierarchy:** Never display more than two primary font weights on a single card. Let size and semantic color (`TextHighEmphasis` vs `TextMediumEmphasis`) establish the hierarchy.

## 3. Micro-Interactions & Tactile Feedback
- **Haptic Touch:** Scrubber scrubbing, speed selector steps, and playback toggles must trigger subtle, precise platform haptics (`HapticFeedbackType`).
- **Responsive Scrims:** HUD controls must never float nakedly over video. Always back them with an ergonomically tapered gradient scrim that ensures 100% legibility regardless of the underlying video frame.
- **Spring Animations:** When animating control toggles (PiP transition, scrubber expansion, volume/brightness overlays), use spring stiffness and damping rather than rigid linear easings.

## 4. State Completeness
- Never generate a UI component with only a "happy path". Every composable must natively handle:
  1. **Loading / Buffering state** (subtle pulsating skeleton or lightweight indeterminate track, never an aggressive full-screen spinner).
  2. **Empty state** (helpful, non-condescending empty library view with explicit folder-add affordance).
  3. **Error / Corrupted state** (honest, non-crashing banner with actionable retry or fallback details per `spec/04 FR-PLAY-003`).
