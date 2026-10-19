# W8 — Interface constants, defaults, and store quality thresholds

**Accessed:** 2026-10-19 · **Method:** desk research only, no device or user testing.
**Purpose:** apply the standing rule (doc 17 §1.3) to the **numbers** in the specification — the seek steps, timeouts, preset ladders and quality thresholds that were originally chosen by judgement rather than established from practice. Every value below is either aligned to a documented default or explicitly recorded as *not settled*.

**Evidence labels:** `[V1]` official vendor documentation or source · `[V2]` reputable secondary reporting · `[R]` user signal.

---

## 1. What this note audited

The rule was applied to behaviour in W6 (auto-advance) and W7 (decoder labels). This pass covers the remaining invented constants:

| Constant | Was | Where |
|---|---|---|
| Forward/back seek steps | 10 s / 30 s, asymmetric | FR-CTRL-002 (and a contradicting `±5/10/30 s` in FR-PLAY-005) |
| Control auto-hide timeout | 3 s | FR-CTRL-001, doc 06 §118 |
| Playback-speed presets | "0.25×–3×" (unspecified ladder) | FR-CTRL-004 |
| Sleep-timer durations | "N minutes" (unspecified ladder) | FR-CTRL-009 |
| Store quality thresholds | crash + ANR only | doc 05 §1 vitals context |

---

## 2. Seek steps — practice is settled, and we were the outlier

| Product | Default skip | Configurable | Source |
|---|---|---|---|
| **VLC for Android** | **±10 s, symmetric**; double-tap and the optional rewind/forward buttons are both 10 s | — | `[V1]` docs.videolan.me |
| **YouTube** | **10 s, symmetric** | **5 / 10 / 15 / 20 / 30 / 60 s** (Settings → General → "Double-tap to seek") | `[V1]` support.google.com · `[V2]` |
| **Netflix** | **±10 s, symmetric** | — | `[V2]` |
| **Disney+** | **±10 s, symmetric** | — | `[V2]` |
| **HBO Max** | 15 s (outlier) | — | `[V2]` |
| **Tubi** | ±30 s (outlier) | — | `[V2]` |
| **Hulu** | **−10 s / +30 s — asymmetric** | — | `[V2]` |

**Verdict.** Symmetric **±10 s** is the settled default for both the closest comparable (VLC, a local-file player) and the largest streaming product (YouTube), and industry press has argued the case on usability grounds `[V2, Vulture 2021: "Ten seconds forwards and backwards across the board"]`. Our asymmetric 10/30 was **the Hulu pattern** — and Hulu's asymmetry exists to clear ad-break recaps, a rationale with no analogue in a local-file player. **Change to symmetric ±10 s, per-direction configurable, with the documented 5/10/15/20/30/60 ladder.**

**Multi-tap accumulation is also documented practice, and we had not specified it.** VLC: *"You can also tap multiple times to seek more than 10 seconds such as, 3 Taps ‣ 20 seconds, 4 Taps ‣ 30 seconds and so on"* `[V1]`. YouTube behaves identically and displays the accumulated delta `[V2]`. Adopted.

**Internal defect found in passing:** FR-CTRL-002 said *"defaults 10 s/30 s"* while FR-PLAY-005 said *"configurable ±5/10/30 s defaults"*. Two requirements disagreed about the same control. FR-PLAY-005 now defers to FR-CTRL-002.

---

## 3. Control auto-hide timeout — our own library had the answer

- **Media3 `PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS = 5_000`** `[V1, source + API reference]`: *"The time between the last user interaction and the controls being automatically hidden, in milliseconds."*
- Media3 also documents `PlayerView` defaults of `use_controller = true`, `hide_on_touch = true`, `auto_show = true`, `show_rewind_button = true`.

**Verdict.** Our 3 s deviated from the default of the exact library we selected (ADR). **Change to 5 s**, which means the app inherits the tested behaviour of its own playback surface rather than overriding it with an invented number. Configurability (2–10 s, or never-while-locked) is retained — that is the part practice does *not* settle.

---

## 4. Playback speed — name the ladder, and keep the reset we already had

- **YouTube** supports 0.25 / 0.5 / 0.75 / 1 / 1.25 / 1.5 / 1.75 / 2× `[V1, YouTube blog]`, plus custom speeds. The same source reports **usage**: when the feature is used, >85% of the time it is used to go *faster*; on mobile the most-used speeds are **1.5×, then 2×, then 1.25×**, and the most-used *custom* speed is 1.1×. It also records standing requests for 3×/3.5×/4× that YouTube has not shipped.
- **Netflix** ships only **0.5 / 0.75 / 1.25 / 1.5×** — deliberately capped at 1.5× — and requires the member to choose a speed **per title** rather than inheriting the last one `[V1, Netflix]`.

**Verdict.** Two changes: (1) **name the ladder** — the specification said "0.25×–3×", which does not tell an implementer what to build; adopt the documented eight-value YouTube ladder as the presets. (2) Our existing rule *"speed resets to default on new items unless 'remember' is set"* already matches Netflix's documented per-title reset semantics — **no change, and worth recording that it was right for a stated reason**.

Our custom range (0.1×–4.0×) exceeds every streaming product. That is **justified rather than deviant**: a local-file player has no creator-relations constraint, and YouTube's own blog documents that users ask for 3–4×.

---

## 5. Sleep timer — structure was right, ladder was missing

| Product | Options | Source |
|---|---|---|
| **YouTube** | **10 / 15 / 20 / 30 / 45 / 60 min, or "End of video"**; on firing it shows a prompt to **extend** the timer | `[V1]` support.google.com · `[V2]` |
| **Netflix** | 15 / 30 / 45 min, or end of the current title | `[V2]` |
| **Spotify** | 5 / 10 / 15 / 30 / 45 / 60 min, or end of track | `[V2]` |
| **Podcast apps** | 15 / 30 / 45 / 60 min, or end of episode, **with a volume fade over several seconds** | `[V2]` |

**Verdict.** Our structure — Off / end-of-item / N minutes, with a fade SHOULD — was already aligned. Adopt the **YouTube ladder (10/15/20/30/45/60 + end-of-item)** as the named presets, and adopt the **extend affordance on expiry**: a fired sleep timer that can only stop is a dead end, and the largest product already solves it.

---

## 6. Store quality thresholds — Play's documented bad-behaviour values `[V1]`

Reviewed 2026-10-19 against `developer.android.com/google/play/vitals`. These are the values at which Google reduces visibility, so they are the industry's *floor*, not a target.

| Vital | Overall | Per phone model |
|---|---|---|
| User-perceived crash rate | **1.09%** | 8% |
| User-perceived ANR rate | **0.47%** | 8% |
| Excessive partial wake locks | **5%** | — |
| Excessive battery usage (Wear) | 1% | — |
| Bitmap memory usage | **200 MB** (foreground / user-perceived / background) | 400 MB cached |
| Memory usage (anon RSS + swap) | 4–6 GB device → **2.00 GB** foreground, 1.00 GB services | — |
| DEX optimisation (apps >10 MB code) | **≥25%** optimisation/obfuscation/shrinking required | — |

Also relevant: Android vitals flags **"emerging issues"** after a problem affects devices for **over 7 days**, giving **21 days** to address it before broader impact.

**Verdict.**
- Doc 05's crash/ANR targets were **already stricter than the flags** (≥99.7% crash-free sessions vs a 1.09% user-perceived flag) — correct posture, retained.
- NFR-PERF-008 memory ceilings (250–500 MB) are far stricter than the 2.00 GB foreground flag on a 4–6 GB device — also correct, now stated with the comparison.
- **Two thresholds were missing entirely and are now recorded:** excessive **partial wake locks (5%)** and **bitmap memory (200 MB)**. The wake-lock value matters directly because playback holds a foreground service and wake locks (FR-PLAT-003/005, NFR-REL-008).
- The **DEX optimisation ≥25%** requirement is already satisfied by the R8 + shrinking build in doc 08 — recorded so a reviewer does not have to rediscover why R8 is mandatory rather than optional.

---

## 7. What practice does **not** settle

Recorded honestly, because the rule (doc 17 §1.3) permits designing from scratch only when practice is *absent*, *contradictory*, or *demonstrably worse* — and these cases must be named rather than assumed.

| Constant | Why practice does not settle it | Status |
|---|---|---|
| **Default library sort order** and default view density | Vendors differ (name-first, date-added-first, and grid-vs-list are all shipped by someone); no help centre states a rationale | **Open — doc 04 Q14.** A one-line decision needed, or a small test |
| **Resume completion threshold** (we chose ≥95% → restart) | Not published by any vendor; internal tuning | Our value stands as reasoning, labelled as such |
| **Near-start threshold** (we chose ≤5%) | Same | Our value stands as reasoning |
| **Auto-hide configurability range** (we chose 2–10 s) | Media3 documents the default but not a sensible user-facing range | Our range stands; the *default* is now aligned |
| **Horizontal-swipe seek sensitivity** | Not documented; a hardware/platform tuning problem | Remains a spike/beta-tune item (RSK-015 family) |

---

## 8. Limits of this evidence

- **No device testing.** Nothing here measures how these defaults *feel*; it establishes only what shipping products document as their defaults.
- **Help-centre values can lag the app.** YouTube's sleep timer was first an experimental Premium feature `[V2]` before reaching the documented help page — the documented set is treated as current, not as guaranteed-identical on every build.
- **Netflix/Hulu/Disney+ values are `[V2]`** (press and how-to reporting), not vendor documentation. VLC, YouTube and Media3 are `[V1]`. Conclusions that rest on symmetry across many products are robust to one wrong `[V2]` row; no conclusion here rests on a single `[V2]` source.
- **Play thresholds are a snapshot** (reviewed 2026-10-19) and Google revises them; doc 15 re-checks at launch.
- **Not audited here:** gesture swipe tuning, scrubber preview behaviour, grid/list density, pull-to-refresh semantics. These need device work, not desk work.

---

## 9. Sources (accessed 2026-10-19)

**Official vendor documentation / source `[V1]`**
- Media3 `PlayerControlView` — `DEFAULT_SHOW_TIMEOUT_MS = 5_000` (API reference): https://developer.android.com/reference/androidx/media3/ui/PlayerControlView
- Media3 `PlayerControlView` source (`DEFAULT_SHOW_TIMEOUT_MS = 5_000`): https://github.com/androidx/media/blob/release/libraries/ui/src/main/java/androidx/media3/ui/PlayerControlView.java
- Media3 `PlayerView` defaults (`use_controller`, `hide_on_touch`, `auto_show`, `show_rewind_button`): https://developer.android.com/reference/androidx/media3/ui/PlayerView
- VLC for Android — Video settings (double-tap = 10 s forward/back; multi-tap accumulation; 10 s seek buttons): https://docs.videolan.me/vlc-android-user/more/settings/extra_video.html
- YouTube Help — Set a sleep timer: https://support.google.com/youtube/answer/15397997
- YouTube Blog — "Need for Speed: How YouTubers watch video using playback speeds" (2022-08-25; the 0.25–2× ladder, usage shares, custom-speed data): https://blog.youtube/inside-youtube/youtube-watch-video-playback-speeds-trends
- Netflix — Player Control Tests (playback-speed announcement; 0.5/0.75/1.25/1.5× cap and per-title reset rationale): https://about.netflix.com/en/news/player-control-tests
- Android Developers — Android vitals (bad-behaviour thresholds: crash 1.09%, ANR 0.47%, partial wake locks 5%, bitmap memory 200 MB, DEX optimisation 25%, emerging-issues 7-day/21-day rule): https://developer.android.com/google/play/vitals
- Android Developers — Android vitals: Crashes (overall 1.09% / per-device 8%): https://developer.android.com/google/play/vitals/crash
- Android Developers — Android vitals: ANRs (overall 0.47% / per-device 8%): https://developer.android.com/google/play/vitals/anr

**Secondary reporting `[V2]`**
- How-To Geek — "How to Skip Back and Forward 10 Seconds in YouTube and Other Apps" (Netflix ±10 s; Hulu −10/+30 s; Disney+ ±10 s): https://www.howtogeek.com/667493/how-to-skip-back-and-forward-10-seconds-in-youtube-and-other-apps
- Vulture — "All Skip Back and Forward Buttons Should Be 10 Seconds Long" (HBO Max 15 s; Tubi ±30 s; editorial case for symmetric 10 s): https://www.vulture.com/article/skip-backwards-forwards-buttons-streaming.html
- TechCrunch — YouTube sleep timer test (10/15/20/30/45/60 or end of video; extend prompt): https://techcrunch.com/2024/08/09/youtube-is-testing-a-sleep-timer-on-its-premium-tier
- The Verge — Netflix sleep timer (15/30/45 or end of title): https://www.theverge.com/2021/1/29/22256308/netflix-timer-test-feature-android-global-sleep-kids-ios-tv-desktop
