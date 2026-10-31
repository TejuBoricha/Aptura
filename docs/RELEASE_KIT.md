# Aptura Release Preparation Kit (Play Store & GitHub)

This document contains production-ready metadata, store copy, privacy declarations, and release checklists for publishing **Aptura** to Google Play and GitHub Releases.

---

## 1. Google Play Console Store Listing

### App Details
- **App Name:** `Aptura Video Player` *(20 / 30 characters)*
- **Short Description:**
  `100% offline, privacy-first video player with zero permissions and fluid UI.` *(76 / 80 characters)*
- **Category:** Video Players & Editors
- **Content Rating:** Everyone (PEGI 3, ESRB Everyone, zero objectionable content, zero online interaction)
- **Default Language:** English (United States) (`en-US`)

### Full Description (Play Store formatted)
```text
Aptura is an elegant, offline-first video player engineered for privacy, speed, and smooth Material 3 aesthetics.

🔒 100% OFFLINE & PRIVATE BY DESIGN
Aptura contains zero network stack and requires NO internet permission (zero android.permission.INTERNET). Your files, watch history, bookmarks, and settings never leave your device.
• Zero Analytics or Telemetry
• Zero Third-Party Tracking or Ads
• Zero Account Creation Required
• Scoped Storage Safe (Standard MediaStore & user-selected SAF folders only)

⚡ HARDWARE-ACCELERATED PLAYBACK
Built on modern Android Media3 architecture:
• Hardware decoding for 4K, 60fps, and HDR10 content.
• Fine-grained playback speed control (0.25x to 3.0x in 0.05x increments).
• Seamless A-B looping and frame-precise scrubbing.
• Picture-in-Picture (PiP) support with background playback.

🎨 BESPOKE MATERIAL 3 DESIGN
Immerse yourself in clean dark elegance:
• True OLED dark themes designed to conserve battery and eliminate eye fatigue.
• Distinct Accent Themes: Amber, Emerald, Rose, Violet, Aptura Blue, and Champagne Gold.
• Fluid, gesture-driven HUD: swipe for volume, brightness, and horizontal seeking.

⏱️ INTELLIGENT SLEEP TIMER
Fall asleep to your favorite shows without waking up to a dead battery:
• Monotonic sleep timer (5 to 720 minutes) with a gradual, 10-second volume fade-out.
• "At the end of this video" automatic pause option.
• Background audio and notification countdown that survives app minimization.

🎧 ADVANCED AUDIO & SUBTITLES
• Multi-track audio selection with Bluetooth audio sync offset (±1000 ms).
• Sidecar subtitle support (.srt, .vtt) with customizable timing, font sizing, and styling.
• High-resolution PCM audio compatibility (24-bit, 32-bit float, and 5.1 multichannel).

📁 EFFICIENT IN-APP MEDIA MANAGEMENT
• Organize, move, copy, and rename videos directly within your storage.
• Direct folder-level paste and folder destination selection.
• Fast search, filtering, and customizable Grid / List layouts.

Aptura is free and open-source under the Apache-2.0 license.
Source code: https://github.com/TejuBoricha/Aptura
```

---

## 2. Play Console Data Safety Questionnaire Answers

Google Play requires a completed Data Safety form:

| Question | Value / Answer | Notes |
|---|---|---|
| **Does your app collect or share any user data?** | **No** | Zero data is collected, logged, or transmitted. |
| **Is all user data encrypted in transit?** | **Not applicable** | App does not connect to the internet (no `INTERNET` permission). |
| **Do you provide a way for users to request data deletion?** | **Not applicable** | No user data is stored off the device. |
| **Is this app designed for children?** | **No** (Target: General / 13+) | |

---

## 3. Play Store Assets Checklist

- [x] **App Icon:** 512 × 512 px 32-bit PNG with alpha.
- [x] **Feature Graphic:** 1024 × 500 px JPG or 24-bit PNG.
- [x] **Screenshots:** 11 freshly rendered Compose gallery captures available under `docs/assets/screenshots/`:
  - Phone screenshots (1080 × 2400 / 16:9): `library-phone.png`, `library-phone-dark.png`, `player-hud.png`, `player-tools.png`, `player-audio.png`, `player-subtitles.png`, `player-queue.png`, `details.png`.
  - 7-inch & 10-inch Tablet screenshots: `settings-tablet.png`, `library-tablet.png`.
- [x] **Privacy Policy URL:** `https://github.com/TejuBoricha/Aptura/blob/main/docs/PRIVACY.md`
- [x] **App Bundle (AAB):** Generated via `./gradlew bundleRelease` -> `app/build/outputs/bundle/release/app-release.aab`.

---

## 4. GitHub Release (v1.0.0)

### Release Title
`Aptura 1.0.0: Privacy-First Offline Android Video Player`

### Tag Name
`v1.0.0`

### Release Description Template
```markdown
# Aptura 1.0.0

Aptura is an offline-first, privacy-focused Android video player built with Jetpack Compose, Material 3, and Media3/ExoPlayer.

### Highlights
- **100% Offline & Zero Network Permission:** Zero `android.permission.INTERNET`, zero telemetry, zero analytics SDKs.
- **Hardware-Accelerated Playback:** Verified 4K 60fps and HDR10 hardware decoding via `MediaCodec`.
- **Intelligent Sleep Timer:** Monotonic countdown with a 10-second audio fade that survives background playback.
- **Pure Material 3 Aesthetics:** OLED dark surfaces with customizable accent themes (including Honey-Amber and Champagne Gold).
- **Audio & Subtitle Sync:** Millisecond-accurate audio/subtitle delay adjustment with Bluetooth compensation.
- **In-App Media Management:** Scoped storage file operations (move, copy, paste, rename) without `MANAGE_EXTERNAL_STORAGE`.

### Assets
Download the APK suited for your device architecture, or grab the universal APK:
- `app-arm64-v8a-release.apk` (Recommended for modern phones & tablets)
- `app-armeabi-v7a-release.apk` (32-bit ARM devices)
- `app-x86_64-release.apk` (Emulators & Intel/AMD Android devices)
- `app-universal-release.apk` (Works on all architectures)

*Release builds are signed and minified with R8.*
```

---

## 5. Verification Commands

| Action | Command | Output |
|---|---|---|
| **Headless UI User Journeys** | `.\tools\test-flow.ps1` | Validates Compose UI flows on device without screenshots |
| **All Connected Tests** | `.\tools\test-flow.ps1 -All` | Runs all connected tests (flows, playback, E2E) |
| **Quality Gate** | `.\tools\gate.ps1` | Full compilation, unit tests, Lint, release R8 assembly |
| **Build Play Store Bundle** | `.\gradlew bundleRelease` | Generates `app/build/outputs/bundle/release/app-release.aab` |
| **Build GitHub APKs** | `.\gradlew assembleRelease` | Generates ABI-split APKs under `app/build/outputs/apk/release/` |
