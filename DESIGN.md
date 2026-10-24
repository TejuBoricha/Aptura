# Aptura — Design System & Visual Specification (`DESIGN.md`)

> **Package:** `org.aptura.player`  
> **Source of Truth:** [spec/06_UX_SCREEN_AND_STATE_SPECIFICATION.md](file:///c:/Users/tejas/Desktop/Mobile%20App/spec/06_UX_SCREEN_AND_STATE_SPECIFICATION.md)  
> **Target Framework:** Android Jetpack Compose (Material 3 baseline with bespoke media extensions)  
> **Purpose:** Authoritative UI/UX contract for AI-assisted code generation. Every screen, composable, and theme definition MUST conform to the tokens and rules in this document.

---

## 1. Aesthetic Vision & Philosophy

Aptura is a **precision optical instrument for personal media**. 
- **Distraction-Free Immersion:** In playback mode, content is the hero. The interface retreats into invisibility until summoned. The player is **always dark**, even when the app is in light mode.
- **Cinematic Dark-First, Not Dark-Only:** The dark palette remains the design baseline, engineered for OLED/AMOLED displays with true `#000000` black in the player. The app follows the system theme by default and can be pinned to Light or Dark.
- **Anti-Slop Guarantee:** Zero generic cookie-cutter templates, zero childish saturated gradients, zero rounded bubbly cards with no purpose. Every element is deliberate, refined, and tactile.

---

## 2. Color Tokens & Semantic Roles

Appearance is a user setting (`ThemeMode` = `System` / `Light` / `Dark`, plus one of five accents: Cyan, Blue, Violet, Amber, Rose). `ApturaTheme` swaps one complete palette at a time, so a token always means the same thing. The dark palette below is unchanged from the original specification; the light palette is its daylight counterpart. The player always resolves to the dark palette.

### 2.1 Surfaces & Backgrounds (Dark Mode Baseline)
| Token | Hex Value | Semantic Role / Usage |
|---|---|---|
| `SurfacePureBlack` | `#000000` | Fullscreen video background, letterbox fill, player scrim base |
| `SurfaceBase` | `#0D0E11` | Primary library and folder view background |
| `SurfaceElevated1` | `#16181D` | Standard media cards, list item surfaces, top app bar |
| `SurfaceElevated2` | `#20232A` | Dialogs, bottom sheets, search bar container |
| `SurfaceElevated3` | `#2B2F38` | Hover/pressed states, focused list items, active chips |
| `SurfaceRecessed` | `#00000080` | Recessed tracks: selector switches, folder chips |
| `SurfaceScrim` | `#000000CC` | 80% opacity overlay behind HUD controls during playback |
| `SurfaceGlass` | `#16181DB3` | 70% opacity blurred backdrop for quick-setting flyouts |

### 2.1b Light Palette (same token names)
| Token | Hex Value | Notes |
|---|---|---|
| `SurfacePureBlack` | `#FFFFFF` | The brightest surface in light mode (thumbnails, selected-chip labels) |
| `SurfaceBase` | `#F5F6F8` | Primary background |
| `SurfaceElevated1` | `#FFFFFF` | Cards and panels |
| `SurfaceElevated2` | `#FCFCFD` | Dialogs and sheets |
| `SurfaceElevated3` | `#EDEFF3` | Pressed/active states |
| `SurfaceRecessed` | `#0F172A0F` | 6% ink recessed track |
| `SurfaceScrim` | `#0A0C10CC` | Scrims stay dark over imagery |
| `SurfaceGlass` | `#FFFFFFB3` | Light glass |
| `BorderSubtle` | `#0F172A1F` | 12% ink hairline |
| `TextHighEmphasis` | `#111418` | 16:1 on the light base |
| `TextMediumEmphasis` | `#4B5563` | 7:1 on white |
| `TextLowEmphasis` | `#6B7280` | 4.8:1 on white |
| `TextOnAccent` | `#FFFFFF` | Light accents are dark enough for white labels |
| `TextOnScrim` | `#F0F4F8` | Fixed light text over dark scrims, in either mode |
| `StatusSuccess` | `#087A42` | Darkened for 4.5:1 on white |
| `StatusWarning` | `#9A6B00` | Darkened for 4.5:1 on white |
| `StatusError` | `#C62828` | Darkened for 4.5:1 on white |
| `StatusInfo` | `#0B6FA4` | Darkened for 4.5:1 on white |

### 2.2 Brand & Functional Accents
Each accent has a dark and a light primary/secondary variant; the active pair is part of the palette.

| Accent | Dark primary | Light primary | Role |
|---|---|---|---|
| Cyan (default) | `#00E5FF` | `#00707F` | **Aptura Electric Cyan** — active states, scrubber fill, selection |
| Blue | `#6EA8FF` | `#1D4ED8` | Cool secondary identity |
| Violet | `#B388FF` | `#6D28D9` | Expressive secondary identity |
| Amber | `#FFC857` | `#8A5300` | Warm secondary identity |
| Rose | `#FF6E9C` | `#B3124B` | Warm secondary identity |

`AccentSubtle` is the active primary at 15% alpha (dark) or 12% alpha (light). `BorderSubtle` is `#FFFFFF26` in dark and `#0F172A1F` in light.

### 2.3 Semantic State Colors
| Token | Hex Value (dark) | Role |
|---|---|---|
| `StatusSuccess` | `#00E676` | Verified format badges, completed scans, healthy states |
| `StatusWarning` | `#FFD600` | Unsupported audio channel fallback notice, high thermal warning |
| `StatusError` | `#FF5252` | Corrupted media files, decoder failure fallback states |
| `StatusInfo` | `#40C4FF` | Format discovery tags, informational badges |

### 2.4 Typography Color Scale
| Token | Hex Value (dark) | Contrast Ratio on `SurfaceBase` | Role |
|---|---|---|---|
| `TextHighEmphasis` | `#F0F4F8` | 16.2:1 (Exceeds WCAG AAA) | Video titles, primary labels, current timecode |
| `TextMediumEmphasis`| `#9BA3AF` | 7.8:1 (WCAG AAA) | Video duration, file resolution, folder item counts |
| `TextLowEmphasis` | `#606978` | 4.6:1 (WCAG AA) | Inactive icons, subtitle track codecs, disabled states |
| `TextOnAccent` | `#001217` | 17.5:1 on Cyan | Text inside primary cyan badges or action buttons |
| `TextOnScrim` | `#F0F4F8` | - | Text/icons on a dark scrim over thumbnails |

---

## 3. Typography Scale (Jetpack Compose `Typography`)

Font Family: **Inter** / Platform **Roboto** (Clean, high-legibility sans-serif with tabular numeric figures).

```kotlin
// Jetpack Compose TextStyle Definitions
val ApturaTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        color = ApturaColors.TextHighEmphasis
    ),
    headlineMedium = TextStyle( // Library Section Headers
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        color = ApturaColors.TextHighEmphasis
    ),
    titleMedium = TextStyle( // Video Item Titles in Grid/List
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        color = ApturaColors.TextHighEmphasis
    ),
    bodyMedium = TextStyle( // Metadata (Resolution, Codec, Size)
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.2.sp,
        color = ApturaColors.TextMediumEmphasis
    ),
    labelSmall = TextStyle( // Badges, Pills, Scrub Timecode
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum", // Tabular figures for jitter-free timers
        color = ApturaColors.TextMediumEmphasis
    )
)
```

---

## 4. Spacing, Layout & Touch Targets

- **Base Spacing Grid:** Multiples of `4.dp` / `8.dp` (`4.dp`, `8.dp`, `12.dp`, `16.dp`, `24.dp`, `32.dp`, `48.dp`).
- **Minimum Touch Target:** `48.dp x 48.dp` for every interactive control (buttons, icons, chips, slider thumbs).
- **Responsive Width Breakpoints:**
  - **Compact (<600dp, Phones portrait):** Single-column media list or 2-column card grid. Navigation bar at bottom.
  - **Medium (600–839dp, Foldables / Small Tablets):** 3-column media grid. Navigation rail or persistent bar.
  - **Expanded (≥840dp, Tablets / Desktops):** Two-pane layout (Folder navigation rail on left, media browser on right; 360dp fixed master pane, flexible detail).
- **System Gesture Zones:**
  - Honor `WindowInsets.systemGestures`.
  - Scrub bar thumb excludes bottom edge via `setSystemGestureExclusionRects` during immersive video playback.

---

## 5. Player HUD & Interaction States

### 5.1 Playback Scrub Bar (Timeline)
- **Track Height:** `4.dp` default, expands smoothly to `8.dp` on touch/drag.
- **Thumb Size:** `16.dp` diameter circle with `AccentPrimary` fill, scaling to `20.dp` while actively dragging.
- **Colors:** 
  - Played track: `AccentPrimary` (`#00E5FF`).
  - Buffered track: `AccentSubtle` (`#00E5FF26`).
  - Remaining track: `SurfaceElevated3` (`#2B2F38`).
- **Timecode Display:** Floating chip above thumb showing current position (`00:14:22 / 01:45:00`) in tabular figures.

### 5.2 HUD Visibility & Transitions
- **Auto-Hide Timeout:** **5 seconds** (`5000ms`, aligned with Media3 `DEFAULT_SHOW_TIMEOUT_MS`).
- **Enter Animation:** `fadeIn(animationSpec = tween(150))`.
- **Exit Animation:** `fadeOut(animationSpec = tween(250))`.
- **Double-Tap Seek Overlays:**
  - Left double-tap: Circular pulse icon with `-10s` indicator.
  - Right double-tap: Circular pulse icon with `+10s` indicator.
  - Multi-tap accumulation: Subsequent taps accumulate `20s`, `30s`, `40s`.

### 5.3 Swipe Gestures (Full-screen Video)
- **Left Vertical Swipe:** Brightness adjustment (0% to 100% overlay pill).
- **Right Vertical Swipe:** Media volume adjustment (0% to 100% overlay pill; hardware volume sync).
- **Horizontal Swipe:** High-precision scrub with fine-grained time delta indicator (`+0:15`, `-1:02`).

---

## 6. Component Style Guide (Jetpack Compose)

### 6.1 Media Card (Video Thumbnail)
```kotlin
// Visual Attributes:
// Shape: RoundedCornerShape(10.dp)
// Elevation: 0.dp (flat surface, uses SurfaceElevated1 border/fill)
// Aspect Ratio: 16:9 thumbnail preview
// Badges: Bottom-right duration pill (SurfacePureBlack at 85% alpha, 4.dp padding, 4.dp rounded corners)
// Progress Indicator: 3.dp accent line across thumbnail bottom for partially-watched videos
```

### 6.2 Format & Codec Pills
```kotlin
// Shape: RoundedCornerShape(4.dp)
// Padding: horizontal 6.dp, vertical 2.dp
// Border: 1.dp solid SurfaceElevated3
// Background: SurfaceElevated2
// Text: labelSmall, uppercase, fontFeatureSettings = "tnum"
// Examples: "4K", "10-BIT", "HEVC", "ASS", "5.1"
```

---

## 7. Explicit Design Guardrails (DOs & DON'Ts)

### ❌ NEVER DO:
1. **Never use saturated purple or pink gradients** as card or background fills.
2. **Never hardcode white backgrounds** (`#FFFFFF`) or bright gray panels. All surfaces belong to the `Surface*` dark scale.
3. **Never render text directly over video** without an alpha-controlled scrim (`SurfaceScrim`).
4. **Never create touch targets smaller than 48dp** on mobile screens.
5. **Never drop frame rates** for animations — use Compose `Animatable` or `animateFloatAsState` with hardware-accelerated transforms (`graphicsLayer`).
6. **Never use generic standard Android dialogs** with default light themes; always theme dialogs with `SurfaceElevated2` and `ApturaTypography`.

### ✅ ALWAYS DO:
1. **Always use tabular digits (`tnum`)** for timecodes, durations, bitrates, and counters to prevent UI jitter.
2. **Always respect edge-to-edge window insets** (`WindowInsets.safeDrawing`, `displayCutout`, `statusBars`, `navigationBars`).
3. **Always provide haptic feedback** (`LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)`) when crossing seek markers or scrubbing.
4. **Always ensure WCAG 2.2 AA (minimum 4.5:1) contrast** for all informative labels and icons.
