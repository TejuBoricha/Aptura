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

The rest of the Data safety form, the target audience and the app-content answers are in `distribution/play-store/metadata/data_safety_answers.md`.

---

## 2b. What the Console asks for separately (audit of 2026-11-15)

Checked against the code, Google's help pages (read on 2026-11-15) and the project's tests. Items marked *recheck* come from earlier research that was not repeated; confirm them in the Console at submission, because the forms change.

### Store text: claims audit (spec/15 S3)

Every checkable claim in `distribution/play-store/metadata/full_description.txt` was compared with the code and holds: six accent colours (`AccentColor`), 0.25x to 2.0x speed and hold-to-2x (`PlaybackSpeedOptions`, `startHoldSpeedBoost`), a sleep timer of 1 to 720 minutes with a 10-second fade (`customSleepMinutes`, `SleepTimer.FADE_MS`), A-B repeat, frame stepping, Picture-in-Picture, multi-track audio, audio sync, subtitles, move/copy/rename, no `INTERNET` permission, and no analytics or advertising SDKs (`DependencyInvariants`).

One gap, not a false claim: the heading "Playback on your phone's own decoders" predates the built-in audio extension (ALAC, AC-3 and G.711 through FFmpeg, DEC-020, DEC-021, DEC-025). If the listing should say so, one accurate wording is "plus a small built-in open-source audio extension (FFmpeg, LGPL) for ALAC and AC-3 on phones that lack a decoder". Under `spec/11B` section 6, do not use brand names ("Dolby Digital", "DTS") as selling points, and do not claim E-AC-3, DTS or TrueHD.

### Photo and video permissions (`READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`)

Google's help page "Understanding Restricted Permissions with minimum scope alternatives" says apps "whose core functionality revolves around broad access to Photos and Videos may use" these permissions, and that this includes apps whose core function is not served by the system photo picker. `spec/10` section 6.1 records that a declaration is needed (*recheck*). Draft justification, to adapt to the form's wording:

> Aptura is a video player and library. Its core function is to find, list and play the videos on the device and in folders the person chooses: a library with folders and search, a continue-watching list, and queue playback. The system photo picker cannot provide a persistent library or folder browsing, and the app needs to see new videos as they appear. It reads video only and sends nothing off the device (it has no `INTERNET` permission). On Android 14 and later it also works when the person grants only selected videos (`READ_MEDIA_VISUAL_USER_SELECTED`).

### Foreground service (`mediaPlayback`)

Google's page "Understanding foreground service and full-screen intent requirements" asks, for each declared type, for a description of the functionality, the user impact if the task is deferred, and a link to a video demonstrating the feature; for media playback the video must show a user starting playback, which "must be user-initiated". The manifest declares `foregroundServiceType="mediaPlayback"`. Drafts:

- **Functionality:** Continues video or audio playback from the background after the person starts it, so playback survives leaving the app or turning the screen off.
- **Impact if deferred:** Audio and video would stop or stutter as soon as the app leaves the screen, and the sleep timer could not fade the sound out as the listing promises.
- **Demo video (to record, 30 to 60 seconds):** open Aptura, tap a video to start it, press Home so playback continues in the background, return, and stop playback. Upload it unlisted and paste the link.

### Other Console items

| Item | Answer / status |
|---|---|
| Advertising ID | **No.** No ads and no advertising SDKs; `DependencyInvariants` fails the unit tests if one reaches the release classpath. *Recheck* the exact question in the Console. |
| Target audience | The 13+ bands only (13 to 15, 16 to 17, 18+); not designed for children. Consistent with `spec/11B` C.8 (Brazil's children's digital law), but the setting does not decide that law's reach: what the app does does. |
| Content rating (IARC) | Answer truthfully: no user-generated content, no interaction, no ads, no purchases. Expected "Everyone". |
| Target API level | 36, set in `app/build.gradle.kts`. |
| Signing | `keystore_setup_guide.md` matches the property and variable names in `app/build.gradle.kts`. Keep the upload key backed up and outside the repository. Play App Signing is set up in the Console at the first upload (*recheck*). |
| New personal account testing | `spec/10` section 6.1 records at least 12 testers for 14 continuous days of closed testing before production (*recheck*). It is the longest lead time on the path, and it cannot start until the name and the signing key exist. |
| Developer verification | Recorded as a regional rollout (*recheck*, `spec/10` section 6.1 and `spec/15` S2); complete it early. |

---

## 3. Play Store Assets Checklist

- [x] **App Icon:** `distribution/play-store/icon-512x512.png`, 512 × 512 px 32-bit PNG with an opaque square background. Editable source: `icon.svg` in the same directory.
- [x] **Feature Graphic:** `distribution/play-store/feature-graphic-1024x500.png`, 1024 × 500 px 24-bit PNG. Editable source: `feature-graphic.svg` in the same directory.
- [x] **Upload screenshots:** six 1080 × 2400 phone captures under `distribution/play-store/phone-screenshots/` and two 2560 × 1600 tablet captures under `distribution/play-store/tablet-screenshots/`.
- [x] **Documentation screenshots:** 11 Compose gallery captures under `docs/assets/screenshots/`, including `onboarding-privacy.png`, phone portrait/landscape and tall-sheet renders, and `settings-tablet.png` / `library-tablet.png`. These retain their gallery dimensions and are not all store-sized portrait images.

- [x] **Privacy Policy URL:** `https://github.com/TejuBoricha/Aptura/blob/main/docs/PRIVACY.md`
- [x] **App Bundle (AAB):** Generated via `./gradlew bundleRelease` -> `app/build/outputs/bundle/release/app-release.aab`.

The Fold logo (DEC-027) is included in the repository icon, feature graphic and affected welcome/tablet captures. See [BRANDING.md](BRANDING.md). Replacing these files does not update an already-uploaded Console listing.

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
