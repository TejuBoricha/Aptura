# Aptura Release Preparation Kit (Play Store & GitHub)

This document contains production-ready metadata, store copy, privacy declarations, and release checklists for publishing **Aptura** to Google Play and GitHub Releases.

---

## 1. Google Play Console Store Listing

### App Details
- **App Name:** `Aptura Video Player` *(20 / 30 characters)*
- **Short and full description, and the release notes:** the text lives in `distribution/play-store/metadata/` (`short_description.txt`, `full_description.txt`, `release_notes_v1.0.0.txt`). It is not copied here, because two copies drifted apart and the older one made claims the app does not meet (audit finding SHIP-01). Check every claim against the code before editing it.
- **Category:** Video Players & Editors
- **Content Rating:** Everyone (PEGI 3, ESRB Everyone, zero objectionable content, zero online interaction)
- **Default Language:** English (United States) (`en-US`)

### Full description

See `distribution/play-store/metadata/full_description.txt`. Play limits: short description 80 characters, full description 4,000, release notes 500.

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
- **Hardware-accelerated playback:** uses the phone's `MediaCodec` decoders. A 4K HEVC run and an HDR10 clip played on one Galaxy S24 (HDR10 was a partial pass); what plays depends on the device.
- **Intelligent Sleep Timer:** Monotonic countdown with a 10-second audio fade that keeps counting while the app is in the background.
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
| **Quality Gate** | `.\tools\gate.ps1 -Full` | Full compilation, unit tests, Lint, release R8 assembly (`-Full` forces the R8 builds, which the plain gate skips for changes that cannot affect them) |
| **Build Play Store Bundle** | `.\gradlew bundleRelease` | Generates `app/build/outputs/bundle/release/app-release.aab` |
| **Build GitHub APKs** | `.\gradlew assembleRelease` | Generates ABI-split APKs under `app/build/outputs/apk/release/` |
