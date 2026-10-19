# W10 — Adjacent platform contracts: notifications, gesture exclusion, captions

**Accessed:** 2026-10-19 · **Method:** desk research only. No device testing.
**Purpose:** W9 audited the platform contracts wrapped around the APIs we *call*. W10 audits three contracts that govern things *adjacent* to playback — a permission we were asking for unnecessarily, a gesture mechanism we had specified in the opposite direction, and a presentation standard we had left as unfalsifiable prose.

This pass began as an attempt to falsify the Part 28 claim that desk research was exhausted. It failed to falsify it again. **One layer was checked and found genuinely clean** — accessibility constants (48dp targets, WCAG 2.2 AA, reduce-motion) are already specified in NFR-A11Y-002 and doc 01 §4, with nothing to add. That is recorded because a pass that only ever finds problems is not measuring anything.

---

## 1. The notification permission — we were asking for a prompt we do not need

**Documented exemption.** Android 13's notification runtime permission page lists exemptions, and the first entry is: *"**Media sessions** — Notifications related to media sessions are exempt from this behavior change."* The Android 13 behaviour-changes page repeats it: *"Notifications related to media sessions and apps that self-manage phone calls are exempt."* `[V1]`

**What we had.** Doc 04 FR-PLAT-002 said media notifications are "exempt from POST_NOTIFICATIONS per platform" — **correct**. Doc 05 NFR-PRIV said we request *"FGS/notification permissions when background audio is enabled"* — **which asks for a runtime permission the exemption makes unnecessary.** Two requirements, two answers, one control. Same class of defect as the FR-PLAY-005/FR-CTRL-002 seek-step contradiction found in Part 27.

**Why this matters more than a normal inconsistency.** For a product whose differentiator is *verifiable permission minimalism* (DEC-008, DEC-012, DH-3), an unnecessary permission prompt is not neutral — it is the exact cost we are positioning against. We would have shown a system dialog that our own research file proves is avoidable.

**Decision:** **declare nothing, request nothing.** No `POST_NOTIFICATIONS` in the manifest, no runtime request. The playback notification is a media-session notification and appears without it.

**The load-bearing condition — this is the part that needs writing down.** The exemption attaches to *media-session notifications*. It is preserved only while we post **no non-media notification**. A future "scan complete" or "storage low" notification would silently convert a permission-free product into one that needs the permission — and would fail on devices where it was never granted. So the rule is recorded as an invariant, not a preference: **R1 posts no non-media notifications.** Anyone adding one is changing the permission model, not adding a feature.

**Verified:** the FGS requirement is separate and stays. Apps still must post a notification when starting a foreground service; they simply do not need `POST_NOTIFICATIONS` for the exempt one. Doc 05 keeps `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and drops the notification permission.

**Bonus finding — media controls are derived from `PlaybackState`.** For apps targeting Android 13+, *"the system derives media controls from PlaybackState actions … the system will show a richer set of controls that are technically consistent between phones and tablet devices."* Before 13, *"the system displayed up to five actions from the MediaStyle notification in the order in which they were added."* `[V1]` So on 13+ the **`PlaybackState` action set is the control surface**, not the notification's button list. This is the platform's own answer to "which transport controls exist", and it belongs in FR-PLAT-002 rather than being left to the build.

---

## 2. Gesture exclusion — our requirement pointed the opposite way

**Documented mechanism.** Android 10 (API 29) added `View.setSystemGestureExclusionRects()` (and `Window.setSystemGestureExclusionRects()`), with Compose's `Modifier.systemGestureExclusion()` as the modern equivalent. Documented guidance: *"To maintain functionality of elements on the left and right edges of the screen, opt out of the back gesture selectively by indicating to the system which regions need to receive touch input."* `[V1]`

**The hard limit.** From the `View` reference: *"the system will put a limit of **200dp on the vertical extent of the exclusions** it takes into account."* AndroidX's own `SystemGestureExclusionHelper` states the overflow behaviour precisely: *"exclusion rects are limited to 200dp total per side of the screen. When this limit is exceeded the exclusion rects will be honored starting from the **bottom** of the screen until the total dp for that side reaches the limit. Additional rects past this limit will be ignored."* `[V1/V2]`

**The exemption that matters most to us.** The same reference adds: *"**The limit does not apply while the navigation bar is `SYSTEM_UI_FLAG_IMMERSIVE_STICKY` stickily hidden**"* — which is exactly the state a fullscreen video player sits in. So the cap constrains us when system bars are *visible* (controls up), and not when we are truly immersive. `[V1]`

**Also documented:** mandatory system gesture insets (`getMandatorySystemGestureInsets()`) cannot be excluded by anyone; and *"It is not necessary for an app to register exclusion rects for broadly spanning regions such as the entirety of a ScrollView or for simple press and release click targets such as Button."* `DrawerLayout` and `SeekBar` are noted as opting out automatically. `[V1]`

**Finding.** FR-CTRL-013 said *"no interactive control inside reserved system-gesture zones."* That is a **rule against the documented solution**. The docs name our exact case as the one that warrants exclusion — *"interacting with a view requires a precision touch gesture in a small area in either the X or Y dimension, such as an edge swipe or **dragging a SeekBar thumb**."* A scrubber that is forbidden from reaching the screen edge is a worse player, and we were forbidding it for a reason the platform disagrees with. Doc 06 §6's *"never under system gesture zone"* is correct for **subtitles** (non-interactive, must stay legible) and wrong as a general prohibition on controls.

**Change.** FR-CTRL-013 is restated around the mechanism: content extends edge-to-edge, precision controls MAY occupy the edge, and the edge is claimed with exclusion rects inside the documented 200dp cap per side — accepting that beyond the cap the system honours rects from the bottom upward and ignores the rest. The prohibition is narrowed to what the platform actually forbids: nothing may be placed inside **mandatory** gesture insets, and simple tap targets should not be edge-hugging because they gain nothing and cost the user the back gesture.

---

## 3. Caption presentation — the standard exists, and "legibility-first" hid the absence of it

**Documented industry standard.** Netflix publishes its Timed Text Style Guides publicly, and they are the de-facto reference for subtitle presentation (a university library's guide is explicitly *"largely informed by the Netflix Timed Text Style Guide"*). Presentation constants, English (US) and general requirements `[V1]`:

| Parameter | Value |
|---|---|
| Characters per line | **42** (Latin scripts); Japanese 13 full-width horizontal / 11 vertical |
| Lines | **2 maximum**; usually 1 |
| Reading speed | **up to 20 cps** adult, **17 cps** children (English US); templates 17/15; Hindi/Marathi 22; Korean 14 |
| Duration per event | **min 5/6 s** (~0.83 s), **max 7 s** |
| Font / colour | proportional sans, **white** |
| Size | *"relative to video resolution and ability to fit 42 characters across the screen"* |
| Line breaking | break **after** punctuation, **before** conjunctions/prepositions; prefer a bottom-heavy pyramid; avoid 1–2 words alone on the top line |
| Placement | centre-justified, top or bottom, positioned *to avoid overlap with on-screen text* |

A professional subtitling house's public guide adds the operating headroom: **17 cps typical, 22 cps absolute**, with *"up to a maximum of 10% of the subtitles … between 17.1 and 22.0"*, minimum duration 21 frames, and a **2-frame minimum gap** between events. `[V1]`

**Finding — and the correct framing.** These are *authoring* guides, not player rendering specs; a player cannot and should not enforce them. But they define the **envelope the author calculated against**, and a player can silently invalidate that calculation:

- re-wrapping a line breaks the author's line-break logic and can push a compliant event past its reading speed;
- scaling text so that 42 characters no longer fit across the screen makes an authored-compliant cue unreadable;
- forcing a background box can cover on-screen text the author positioned around;
- repositioning subtitles can put them on top of the very on-screen text the guide tells authors to avoid.

So the correct requirement is not "make subtitles legible" but: **defaults MUST NOT invalidate the author's assumptions** — 2-line maximum preserved, no re-wrapping of an already-wrapped cue, white-on-video legible default, bottom-centre within the safe area, position overridable and never moved onto declared on-screen-text regions. That is falsifiable, and doc 13 can verify it (fit 42 characters at default size; count CPS-equivalent against the 17–22 cps band on fixtures).

**Second finding — remove a hedge we never justified.** FR-SUB-008 said *"respect for platform caption preferences **where feasible**."* Doc 02A records that Just Player — our closest aligned comparable — delegates styling to `CaptioningManager`/system preferences, and that it does so because **Media3's `SubtitleView` does it automatically**. A hedge with no named conflict is exactly what the §1.3 rule forbids. **Change:** delegation to system caption preferences is the *default* for plain-text/CC tracks (it is free, it is what our own library does, and it is what accessibility users expect); embedded ASS/SSA styling keeps its existing separation-of-concerns policy; in-app overrides are additive user choice, not replacements.

---

## 4. Limits

- **No device testing.** Whether an OEM build honours the media-session exemption, and whether exclusion rects behave as documented across skins, is unverified here. Both are now routed to **RQ-16**.
- **The exemption is quoted from Google's documentation, not probed at runtime.** The failure mode (no visible notification) is silent and would only appear in the field.
- **Caption constants vary by language** (22 cps Hindi/Marathi, 14 cps Korean, 13/11 full-width Japanese). The 42-character and 2-line rules are the stable ones; reading-speed bands are language-specific and must not be hard-coded as a single global number.
- **Netflix's guides govern content delivered *to Netflix*.** They are cited as evidence of the industry envelope, not as a standard we are obliged to follow.
- **Delegation to `CaptioningManager` was not tested.** Whether Media3's `SubtitleView` preserves system font scale, background and edge style for every track type we support is a build-phase question.

---

## 5. Sources (accessed 2026-10-19)

**Official vendor documentation / source `[V1]`**
- Android — Notification runtime permission (**Exemptions → Media sessions**: "Notifications related to media sessions are exempt from this behavior change"): https://developer.android.com/develop/ui/views/notifications/notification-permission
- Android — Compose notification permission (same exemptions section, FGS note): https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Android — Behaviour changes: all apps, Android 13 (media-session exemption; media controls derived from `PlaybackState`): https://developer.android.com/about/versions/13/behavior-changes-all
- Android — Behaviour changes: apps targeting Android 13 (notification permission affects FGS appearance; media controls derived from `PlaybackState`; pre-13 five-action ordering): https://developer.android.com/about/versions/13/behavior-changes-13
- Android — Ensure compatibility with gesture navigation (`setSystemGestureExclusionRects`, edge-to-edge, selective opt-out, DrawerLayout/SeekBar automatic behaviour): https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav
- Android — `View.setSystemGestureExclusionRects` (200dp vertical limit; **limit does not apply while the navigation bar is `SYSTEM_UI_FLAG_IMMERSIVE_STICKY` stickily hidden**; precision-gesture rationale naming SeekBar thumb drag; "not necessary … for simple press and release click targets"): https://developer.android.com/reference/android/view/View#setSystemGestureExclusionRects(java.util.List%3Candroid.graphics.Rect%3E)
- Android — `WindowInsets.getSystemGestureInsets` / mandatory insets cannot be excluded; 200dp note: https://developer.android.com/reference/android/view/WindowInsets
- AndroidX — `Modifier.systemGestureExclusion()` (current API; `excludeFromSystemGesture` deprecated): https://developer.android.com/reference/kotlin/androidx/compose/foundation/systemGestureExclusion.modifier
- Netflix — Timed Text Style Guide: General Requirements (min 5/6 s, max 7 s; 2 lines maximum; centre-justified top/bottom placement; avoid on-screen text overlap): https://partnerhelp.netflixstudios.com/hc/en-us/articles/215758617-Timed-Text-Style-Guide-General-Requirements
- Netflix — English (USA) Timed Text Style Guide (42 characters per line; reading speed 20 cps adult / 17 cps children; pyramid line breaking): https://partnerhelp.netflixstudios.com/hc/en-us/articles/217350977-English-USA-Timed-Text-Style-Guide
- Netflix — Timed Text Style Guide: Subtitle Templates (42 cpl; ⅚ s–7 s; font size "relative to video resolution and ability to fit 42 characters across the screen"; template reading speed 17/15 cps): https://partnerhelp.netflixstudios.com/hc/en-us/articles/219375728-Timed-Text-Style-Guide-Subtitle-Templates
- Netflix — Japanese / Hindi / Korean / Marathi Timed Text Style Guides (language-specific limits: 13/11 full-width characters; 22 cps; 14 cps): https://partnerhelp.netflixstudios.com/hc/en-us/articles/215767517-Japanese-Timed-Text-Style-Guide · https://partnerhelp.netflixstudios.com/hc/en-us/articles/115003196707-Hindi-Timed-Text-Style-Guide · https://partnerhelp.netflixstudios.com/hc/en-us/articles/216001127-Korean-Timed-Text-Style-Guide

**Source / prior art `[V2]`**
- AndroidX `SystemGestureExclusionHelper.java` (200dp **per side**, overflow honoured **from the bottom** upward, remainder ignored): https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/pdf/pdf-viewer/src/main/java/androidx/pdf/util/SystemGestureExclusionHelper.java
- Android CTS — "limit the system exclusion rect less than 200dp" (the limit is enforced and CTS-tested — it is a platform invariant, not a suggestion): https://android.googlesource.com/platform/cts/

**Third-party professional guidance `[R]`**
- OOONA/AVTpro Subtitling Style Guide (42 cpl; 2 lines max; 17 cps typical, 22 cps absolute with ≤10% over; min 21 frames; 2-frame minimum gap; background recommended "to enhance visibility"): https://avtpro.ooona.net/wp-content/uploads/2023/06/AVTpro_StyleGuide_Id.pdf
- UTK Libraries Timed Text Style Guide ("largely informed by the Netflix Timed Text Style Guide"): https://utk-timed-text-guide.readthedocs.io/en/latest/contents/1_timed_text_style_guide.html
