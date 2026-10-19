# W7 — Decoder labelling: how shipping players name and explain decode options

**Accessed:** 2026-10-19 · **Purpose:** settle doc 04 **Q13** / doc 02A **CS-4** (decoder label language) against documented practice.
**Scope:** the *labelling and placement* of decode controls only. No hands-on testing; nothing here measures comprehension.

**Evidence labels:** `[V1]` official vendor documentation or source · `[V2]` reputable secondary / source-derived · `[R]` user-generated signal (evidence of pain, never proof of behaviour).

---

## 1. The question

Our spec left open whether decode-override controls should use **technical** language (`HW` / `SW` / `Auto`) or **outcome** language ("Best quality / Most compatible"). Q13 framed it as a choice between the two. The research says it is a false choice.

---

## 2. What each product ships

### 2.1 VLC for Android — technical label **plus** plain-language guidance, `Automatic` default
- The setting is called **Hardware Acceleration**, with four options `[V1]` (docs.videolan.me/vlc-user/android/3.X/…/general_settings.html):
  - **Automatic — (default)** "Let VLC for Android decide when to use hardware acceleration."
  - **Disabled** — with the guidance *"Use this if you experience frequent instability in video playback, especially on slower devices."*
  - **Decoding acceleration** — "hardware acceleration for video decoding only."
  - **Full acceleration.**
- The default is confirmed in source: `HW_ACCELERATION_AUTOMATIC = -1` is the fallback when the preference is unreadable `[V1]` (vlc-android `VLCOptions.kt`).
- **Every option carries a when-to-use sentence.** The user is never shown a bare token.

### 2.2 VLC — bounded fallback with restore is also the shipped pattern
- The player tracks `mDisabledHardwareAcceleration` ("whether fallback from HW acceleration to SW decoding was done") and **restores the previous value afterwards** `[V2, source-derived]` (VideoPlayerActivity in the vlc-android lineage).
- Relevant to FR-PLAY-003: automatic fallback that is *bounded* and does not silently rewrite the user's setting is the established behaviour, not our invention.

### 2.3 MX Player — the jargon counter-example, with the support cost on display
- Ships bare **`HW` / `HW+` / `SW`** as the user-facing vocabulary.
- MX's own support site must therefore carry a **tree of articles explaining its own labels** `[V1]`:
  - "What are SW decoder and HW decoder?"
  - "What are the differences between HW and HW+?"
  - "Why my device cannot use HW+ decoder?"
  - "HW+ decoder crashes soon after starting playback."
  - "How to start a video using specific decoder?" (documents a *long-press → Play…* per-item chooser, separate from the default setting)
- Users still ask what the abbreviations mean `[R]` (videohelp forum thread titled literally "HW, HW+, SW full forms?").
- Third-party explainer articles exist purely to decode the labels `[V2]` (contentbase "MX Player HW vs HW+ vs SW Decoder Modes Explained", 2026-01-14 — which opens by noting users "are not alone" in being confused, and warns these "are not 'quality settings' in the usual sense").
- **Interpretation `[I]`:** a vendor needing a support tree, plus third-party explainers, plus forum threads asking for the expansion of its own abbreviations, is direct evidence that bare technical tokens *shift work onto the user and the support channel*.

### 2.4 nPlayer — technical label plus a paragraph per option
- **Decoder** → *QuickTime / Hardware / Software*, each explained `[V1]` (help.nplayer.tv): hardware "is played first … if the video has a codec which can be decoded with hardware decoder"; software "uses CPU, so playback performance may vary depending on CPU performance. And CPU usage is higher … which may increase battery drain."

### 2.5 mpv-style clients — the same pattern, one line each
- The documented "Hardware decoding mode" list is *Auto / HW+ / HW / SW* — but each entry carries an outcome sentence `[V1]` (aniyomi.org player-sheets guide, describing mpv semantics):
  - Auto — "Automatically select best decoder."
  - HW+ — "Slightly better performance than HW, at the cost of not allowing some filters."
  - HW — "Uses hardware to decode video, providing better performance and battery efficiency."
  - SW — "Uses software to decode video, has less performance than HW but comes with better compatibility for older phones."

### 2.6 Platform-SDK vendors treat decoder selection as automatic and device-specific
- A commercial Android SDK vendor documents a decoder-selection layer with **blacklists per device** because "the player will encounter … media decoder and playback differences firsthand" and device-specific decoder defects are normal `[V2]` (Dolby OptiView / THEOplayer, decoder-selection extension).
- **Implication:** the *normal* user-facing posture is **expose a default that works and a bounded manual override**, not a codec-selection console.

---

## 3. The settled pattern

| Element | What shipping products do |
|---|---|
| Default | **Automatic / Auto** — never a forced mode |
| Label form | Technical term **paired with** a plain-language outcome sentence |
| Bare tokens alone | Only MX; it carries the documented support cost |
| Placement | A **global default** in settings *plus* a **per-item override** (MX's long-press chooser; VLC's settings toggle) |
| Fallback | Automatic and **bounded**, with the user's setting **restored** (VLC) |
| Per-option copy | Always says *when to use it*, not just what it is |

**Answer to Q13:** it is not "technical **or** outcome". The industry-settled solution is **technical name + when-to-use description**, with `Auto` as the default and both a global setting and a per-item override. Bare `HW/HW+/SW` is the one pattern to avoid, and its cost is documented by the vendor that chose it.

---

## 4. Limits of this evidence

- **No comprehension testing.** Nothing here measures whether users *understand* the paired labels better. CS-4's test remains worthwhile; what changed is that the design now has a defensible default rather than an open question.
- **Help-centre copy is prescriptive, not observed.** We are reading what vendors tell users, not what users do.
- **No screenshots inspected.** Placement claims come from documentation, not from the app's UI.
- **MX's in-app strings were not enumerated** — its full settings tree remains `[U]` (doc 02A §13).
- **Date-sensitive.** All sources accessed 2026-10-19.

---

## 5. Sources (accessed 2026-10-19)

**Official vendor documentation / source `[V1]`**
- VLC for Android — General Settings (Hardware Acceleration: Automatic default / Disabled / Decoding / Full, with guidance): https://docs.videolan.me/vlc-user/android/3.X/en/more/settings/general_settings.html
- VLC for Android — VLCOptions.kt (`HW_ACCELERATION_AUTOMATIC = -1`): https://github.com/videolan/vlc-android/blob/master/application/resources/src/main/java/org/videolan/resources/VLCOptions.kt
- VLC for Android — Video settings ("Resume played videos", three options): https://docs.videolan.me/vlc-user/android/3.X/en/more/settings/extra_video.html
- MX Player support — What are SW decoder and HW decoder?: https://support.mxplayer.in/support/solutions/articles/43000608820-what-are-sw-decoder-and-hw-decoder-
- MX Player support — How to start a video using specific decoder?: https://support.mxplayer.in/support/solutions/articles/43000608831-how-to-start-a-video-using-specific-decoder-
- nPlayer help — Decoder Setting (HW/SW): https://help.nplayer.tv/en/36-2/playing/decoderqthwsw/index.html
- Aniyomi — player sheets, "Hardware decoding mode" (Auto/HW+/HW/SW with descriptions): https://aniyomi.org/docs/guides/video-player/sheets

**Source-derived / secondary `[V2]`**
- vlc-android lineage `VideoPlayerActivity` (fallback flag + restore): https://github.com/xhochy/vlc-android/blob/master/vlc-android/src/org/videolan/vlc/gui/video/VideoPlayerActivity.java
- Dolby OptiView — THEOplayer Android decoder selection extension (device-specific decoder defects, blacklists): https://optiview.dolby.com/resources/blog/playback/decoding-the-decoders-theoplayer-android-sdk-decoder-selection-extension
- contentbase — "MX Player HW vs HW+ vs SW Decoder Modes Explained" (2026-01-14): https://contentbase.com/blog/mx-player/decoder-modes/

**User signals `[R]`**
- videohelp forum — "HW, HW+, SW full forms?": https://forum.videohelp.com/threads/404086-HW-HW-SW-full-forms
