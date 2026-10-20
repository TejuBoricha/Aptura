# Continuous Critical Evolution & Competitive Parity Mandate

**Status:** Permanent Project Rule & Invariant  
**Applies to:** All UI reviews, screenshot inspections, feature development, testing sessions, and architectural milestones.

---

## 1. The Core Directive: Beyond "Does It Work?"

Never limit analysis, inspection, or code reviews to verifying whether existing code executes without crashing. A basic implementation that simply "works" is a Minimum Viable Product (MVP), not a market-leading media player.

Whenever inspecting screenshots, reviewing UI components, testing features, or auditing any part of the codebase, the developer MUST evaluate the application through the **Critical Product Lens**:
1. **What is missing?** What settings, toggles, controls, or visual indicators would a user expect to see on this screen?
2. **What can be customized?** Can the user adjust increments, fine-tune timing, customize styles, or tailor the experience to their hardware and preferences?
3. **What do tier-1 competitors offer?** What have VLC, MX Player Pro, Next Player, Just Player, Nova, and MPV already solved and standardized?
4. **Where are the usability friction points?** Are buttons too close together? Do headers lack icons? Does text shift dynamically? Are values legible against high-contrast or busy backgrounds?
5. **What edge cases are unhandled?** Headphone disconnects, Bluetooth audio latency, missing subtitles, unusual aspect ratios, corrupted frames, 10,000-item libraries, low-storage devices.

---

## 2. Mandatory Audit Taxonomy

For every screen and feature domain, proactively evaluate and propose enhancements across these four core pillars:

### A. Advanced Audio Controls & Sound Enhancements
- **Delay Customization:** Fine-tuning beyond coarse steppers (e.g. ±10ms fine steps, slider scrubber, manual numeric ms entry, Bluetooth A/V lag compensation presets).
- **Equalizer & Frequency Bands:** Multi-band equalizer (5-band or 10-band: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz) with hardware-accelerated `android.media.audiofx.Equalizer`.
- **Audio Presets:** Standard curated presets (Flat, Bass Boost, Vocal/Speech Clarity, Rock, Pop, Classical, Jazz, Heavy Metal, Podcast).
- **Sound Enhancement Effects:**
  - `BassBoost` (strength 0–1000).
  - `Virtualizer` (surround sound simulation 0–1000).
  - `LoudnessEnhancer` (audio boost beyond 100% for quiet dialogue tracks).
- **Audio Routing & Output:** Audio balance (Left/Right stereo panning), night mode / speech normalizer (compress dynamic range for late-night viewing).

### B. Video Picture Adjustments & Visual Customizations
- **Color & Picture Adjustments:** Software Brightness boost (above 100%), Contrast, Saturation, and Hue adjustment via ColorMatrix / shader processing.
- **Eye Comfort & Night Mode:** Blue light filter, warm tint scrim, or true OLED black dimmer.
- **Aspect Ratio Mastery:** Fit, Fill, Zoom / Crop, 16:9, 4:3, 18:9, 21:9, and free-form custom pan & pinch-to-zoom.
- **Orientation & Display Modes:** Sensor auto-rotate, locked portrait, locked landscape, reverse landscape, and notch/cutout avoidance toggle.

### C. Subtitle Deep Customization
- **Timing & Synchronization:** Fine delay adjustments (±10ms, ±50ms, manual entry), time-stretch calibration for 23.976fps vs 24/25fps drift.
- **Visual Styling:** Text size (Very Small to Extra Large), font family selection, text color, outline / stroke thickness, drop shadow, and background box opacity.
- **Screen Placement:** Custom vertical positioning offset (percentage from bottom of screen to avoid covering crucial scene details or HUD scrims).
- **Encoding & External Files:** Character encoding selector (UTF-8, ISO-8859, Windows-1252), Storage Access Framework (SAF) external `.srt` / `.vtt` / `.ass` subtitle file loader.

### D. Playback Controls & Power Delighters
- **A-B Repeat:** Setting Point A and Point B to loop a specific segment (language learning, tutorials, sports analysis).
- **Frame-by-Frame Stepping:** Micro-step backward/forward by 1 frame while paused.
- **Long-Press 2x Speed Preview:** Press-and-hold anywhere on screen to smoothly accelerate playback to 2.0x, releasing restores base speed.
- **Fine Speed Controls:** Slider / granular selector from 0.25x to 4.0x in 0.05x steps, with pitch preservation toggle.
- **Skip Silence:** Automatic fast-forward or silence jump during quiet intervals.
- **Customizable Gestures:** Configurable double-tap seek delta (5s, 10s, 15s, 30s), swipe sensitivity, and gesture lock mode.

---

## 3. The Continuous Execution Practice

This rule is **NOT** a one-time audit checklist. It is a permanent operational standard:
1. **In Every Screenshot Inspection:** When reviewing visual captures, document not only what was verified, but also list "Proactive Polish & Future Enhancement Opportunities".
2. **In Every Feature Implementation:** When building a component, architect the state and interfaces to accommodate customization (e.g. exposing sliders, parameters, and presets rather than hardcoded magic numbers).
3. **In Every Code Review / Gate:** Ensure changes move the app toward category leadership, never settling for bare-minimum compliance.
