# W6 — Auto-advance / end-of-item defaults: industry practice

**Accessed:** 2026-10-19 · **Purpose:** close doc 04 Q12 (auto-advance default) with evidence rather than preference.
**Scope:** documented default behaviour and option surface only. No hands-on testing was performed; nothing here is a measurement of user behaviour.

**Evidence labels:** `[V1]` official vendor documentation · `[V2]` reputable secondary reporting · `[I]` inference from documented option wording (flagged as such).

---

## 1. Why this file exists

The spec originally set the end-of-item default to **stop with an Up next affordance**, justified by a reversibility argument (a user who wanted auto-advance is annoyed once; a user who did not want it is interrupted). That argument was reasoning, not evidence. This note tests it against what shipping products actually do.

**Result: the reasoning was wrong in one direction and right in another.**
- Wrong: every comparable product defaults auto-advance **ON** when a next item exists.
- Right: the *reversibility* instinct is still correct — which is why every one of them pairs the default with a **cancellable countdown**, not an instant cut.

---

## 2. What each product does

### 2.1 Netflix — auto-advance ON by default, cancellable, credits-aware
- Autoplay Next Episode is a per-profile toggle under Playback settings `[V1]` (help.netflix.com/en/node/121518).
- Default state is **on**: "The autoplay feature is enabled by default on Netflix" `[V2]` (groovypost.com, howto article) — secondary source, but consistent with the existence of a "Next episode isn't playing automatically" troubleshooting page `[V1]` (help.netflix.com/en/node/100365), which only makes sense as a default-on failure mode.
- Known user complaint: it begins during/over the credits and can clip the last ~30 s `[V2]` (webapps.stackexchange.com/questions/45430). This is the cost of defaulting ON without enough countdown courtesy.

### 2.2 YouTube — auto-advance ON by default, cancellable, inactivity-guarded
- Autoplay is a first-class toggle in the player and in Settings → Autoplay `[V1]` (support.google.com/youtube/answer/6327615).
- Default is **on for adults**; explicitly **off by default for users aged 13–17**, and parent-controllable for supervised accounts `[V1]`. This is a notable exception and a direct precedent for age-gating a default.
- Cancellable: "tap **Cancel** at the end of a video" `[V1]`.
- **Inactivity guard:** autoplay stops automatically after **30 minutes inactive on mobile network** and **after 4 hours on Wi-Fi** `[V1]`.
- Settings are **per-device**, not account-wide `[V1]`.

### 2.3 Plex — the most explicit model, and the closest to ours
Plex documents the full end-of-item design, including defaults `[V1]` (support.plex.tv articles: HTPC settings, Android TV settings, Plex Mobile, Roku, and the Post Play screen article):
- **Post Play screen** with an **Up Next** item and a countdown that auto-plays when it expires, unless the user interacts to dismiss it.
- **Countdown defaults: 10 s on mobile, 15 s on TV.** Options: Immediate / 5 / 10 / 15 / 30 / 60 s.
- **AutoPlay Up Next** is a user preference (Player Experience settings); turning it off leaves the Post Play screen waiting for a choice.
- **Passout Protection** — default **2 hours**: if there has been no user interaction for more than 2 hours, auto-play will not continue to the next item. Options: Never / 1 h / 2 h (default) / 3 h.
- **When Post Play appears at all:** only if the completed item was **> 5 minutes** and **not a trailer**; it is *not* shown for curated playlists.
- **When auto-advance is suppressed:** user opted out, **or** videos have played without intervention for > 2 h **and** the completed item was > 20 min. In that case Up Next is still shown and pre-selected, so one click plays it.
- Single movie / end of queue: no Up Next item; the screen is simpler.

### 2.4 Infuse (Apple TV/iOS) — continuous playback ON, scoped to TV shows
- A **Continuous Playback** setting exists; "Continuous playback ON = auto-select next episode" `[V1]` (Firecore community, developer post, 2021 — describing v7.0.8).
- Users report running it as **"TV Shows Only"** `[V2]` (Reddit r/appletv) — i.e. the useful default is *scoped by content type*, not global.
- Notable complaint in the same threads: when auto-advance was added, it broke the "return to the episode so I can delete it" flow for people who delete after watching `[V2]`. **Direct evidence that auto-advance interacts with post-playback actions.**

### 2.5 MX Player (Amazon MX Player) — continuous play is the shipped behaviour
- Documented option names: **"Playalone"** and **"Back to list"** under Local Player Settings → Player. The support article's instruction to *stop* continuous playing is: uncheck **Playalone**, check **Back to list** `[V1]` (support.mxplayer.in article 43000660740).
- Therefore **Playalone checked = keep playing items one after another**; the article exists because users want to *turn it off*, implying it is the default `[I]`.
- Also documented: tapping and holding the next/fast-forward button cycles loop options `[V2]` (XDA thread) — a hidden gesture, not a visible mode control.

### 2.6 VLC — advances through a playlist, but repeat is OFF by default
- VLC's loop/repeat control cycles **off → loop all → loop one**; it is **grey/inactive by default** `[V2]` (multiple how-to sources describing the default state and how to disable unwanted repeat).
- A playlist continues to the next item as normal playlist behaviour; VLC does not add a countdown or an auto-advance concept beyond playlist order.
- **Interpretation:** VLC treats "advance" as intrinsic to a playlist and "repeat" as the opt-in. It has no Post-Play-style surface.

### 2.7 Emby — auto-advance is account-wide, and that was contested
- "Automatically play next episode" is a user setting; a forum thread requests per-device control and the maintainer declines, arguing per-device adds complexity and that account-wide "catches the most scenarios" `[V2]` (emby.media community topic 91301).
- Useful as evidence that **scope of the setting** (device vs account vs content-type) is itself a design decision with tradeoffs, not a detail.

### 2.8 Swiftfin (Jellyfin, Apple TV) — the counter-example, and its cost
- A 2026 client review states: "Enable Auto-play next episode with a ten-second delay; **the default off setting is the source of half the 'can you make it play the next one' requests**" `[V2]` (blog.frazile.com, 2026-09-07).
- This is the single most directly relevant data point for our decision: a local/self-hosted player that defaulted **off** and generated support load for it.

---

## 3. Cross-product pattern

| Behaviour | Netflix | YouTube | Plex | Infuse | MX | VLC | Emby |
|---|---|---|---|---|---|---|---|
| Auto-advance default when a next item exists | **ON** | **ON** (off for 13–17) | **ON** (toggle available) | **ON** ("Continuous Playback") | **ON** `[I]` | n/a (playlist order) | **ON** |
| Cancellable countdown | Yes (credits) | Yes ("Cancel") | **Yes, 10 s mobile / 15 s TV** | Yes | Not documented | No | Not documented |
| Inactivity / passout guard | No | **Yes (30 min / 4 h)** | **Yes (2 h default)** | Not documented | No | n/a | No |
| Suppressed for short items | No | No | **Yes (< 5 min, trailers)** | Scoped by content type | No | n/a | No |
| Setting scope | Per profile | **Per device** | Per client | Per device | Per app | Per app | Account-wide (contested) |
| Interacts with post-playback action (delete) | No | No | Yes (Post Play) | **Yes (broke delete flow)** | Yes ("Back to list") | n/a | No |

**The consensus method, stated plainly:**

> Default auto-advance **ON** when — and only when — a next item exists. Make it a **countdown**, never an instant cut. Let the user **cancel** it. **Suppress it after prolonged inactivity** so it cannot play all night. Keep the setting **visible and cheap to change**, and do not let it destroy post-playback actions (like delete).

---

## 4. What this means for our product

### 4.1 The default should be ON, scoped to "a next item exists"
Our product has a natural scope that the streamers do not: **a single file opened from a file manager has no queue at all.** That case needs no policy — it stops, because there is nothing to advance to. The default therefore only applies when the user started playback from a folder, playlist, or series context, which is exactly when they signalled intent to watch more than one item.

This resolves the original reversibility concern without defaulting to stop:
- Single file → stops (no queue, no decision needed).
- Folder/playlist/series → continues, with a cancellable countdown.

### 4.2 Adopt Plex's countdown, because it is the only documented mobile number
**10 s on mobile**, configurable (Immediate / 5 / 10 / 15 / 30 / 60). Not an instant cut — that is what generates the Netflix "it clipped my credits" complaint.

### 4.3 Adopt the inactivity guard, at Plex's default
**Suppress auto-advance after 2 hours without user interaction.** This is the single cheapest protection against the worst failure mode of defaulting ON: the app playing unattended for hours. YouTube's 30 min / 4 h equivalents confirm the pattern is not Plex-specific.

### 4.4 Do NOT adopt the "<5 min" suppression rule
Plex hides Post Play for items under 5 minutes because its content is long-form. Our users legitimately watch short clips, and auto-advancing a folder of short videos is a *feature*, not a bug. Rejected deliberately, with reason.

### 4.5 Keep the setting per-app, not per-device-account
We have no account (DEC-004/012), so Emby's account-wide problem does not arise. Per-app is the only option.

### 4.6 Protect post-playback actions
Infuse's regression is instructive: adding auto-advance broke "return to the episode so I can delete it". Our ENDED surface must keep **replay / delete-from-library / next** reachable, and auto-advance must not be the only way forward.

### 4.7 Note the age-gating precedent
YouTube defaults autoplay **off** for 13–17. We have no accounts and no age signal, so we cannot replicate this. Recorded as a known limitation rather than silently ignored — our app has no way to know the viewer is a minor.

---

## 5. Limits of this evidence

- **No product publishes its default *rationale*.** Defaults are inferred from help-centre instructions ("to stop X, uncheck Y") and from secondary reporting. `[I]` labels mark where inference is doing the work — most notably MX Player's "Playalone".
- **No usage data.** None of this says what fraction of users change the setting, or how many are annoyed. The Swiftfin quote is an anecdote from a blog, not a measurement.
- **Streaming services are not clean comparables.** Netflix and YouTube optimise for continued engagement; we do not. Their defaults are still informative because the *interaction pattern* (countdown + cancel + guard) is what users have been trained on — but the incentive difference is real and is why Plex (a personal-library product, closest to our use case) is weighted most heavily.
- **No hands-on testing.** Nothing here was verified on a device.
- **Date-sensitive.** Defaults change between versions; all sources accessed 2026-10-19.

---

## 6. Sources (accessed 2026-10-19)

**Official vendor documentation `[V1]`**
- Netflix — How to autoplay the next episode: https://help.netflix.com/en/node/121518
- Netflix — Next episode isn't playing automatically: https://help.netflix.com/en/node/100365
- YouTube — Autoplay videos: https://support.google.com/youtube/answer/6327615
- YouTube — How YouTube Works, Autoplay: https://www.youtube.com/howyoutubeworks/user-settings/autoplay/
- YouTube Kids — settings overview (autoplay default off for kids): https://support.google.com/youtubekids/answer/7348846
- Plex — HTPC settings (countdown + Passout Protection defaults): https://support.plex.tv/articles/htpc-settings/
- Plex — Settings: Android TV: https://support.plex.tv/articles/settings-android-tv/
- Plex — Settings: Plex Mobile: https://support.plex.tv/articles/205671068-settings-plex-mobile/
- Plex — Settings: Plex for Roku: https://support.plex.tv/articles/204275243-settings-plex-for-roku/
- Plex — Play Queue Post-Play screen (when it shows / when auto-advance is suppressed): https://support.plex.tv/articles/202605013-play-queue-post-play-screen/
- Plex — Player Experience (defaults labelled): https://forums.plex.tv/t/player-experience/857990
- MX Player — How to stop/start continuous video playing one after another: https://support.mxplayer.in/support/solutions/articles/43000660740-how-to-stop-start-continuous-video-playing-one-after-another-
- Infuse/Firecore — Continuous playback behaviour (developer post): https://community.firecore.com/t/option-to-stop-gui-advancing-to-next-show/33110

**Secondary reporting and community `[V2]`**
- GroovyPost — Netflix autoplay enabled by default: https://www.groovypost.com/howto/change-netflix-settings-less-frustrating-viewing-experience-devices
- WebApps SE — Netflix auto-advance clipping the last 30 s: https://webapps.stackexchange.com/questions/45430/how-to-stop-netflix-playing-next-episode-automatically
- Engadget — YouTube autoplay settings by platform: https://www.engadget.com/2268943/where-to-find-youtubes-autoplay-settings-and-how-to-turn-it-off-and-on/
- How-To Geek — autoplay defaults across apps: https://www.howtogeek.com/autoplay-turn-it-off-on-your-favorite-apps/
- Reddit r/appletv — Infuse continuous playback set to "TV Shows Only": https://www.reddit.com/r/appletv/comments/1jfjjvj/infuse_auto_play_next_episode_not_working_on/
- Firecore community — auto-advance broke the delete-after-watch flow: https://community.firecore.com/t/option-to-stop-gui-advancing-to-next-show/33110
- Emby community — auto-advance setting scope, account vs device: https://emby.media/community/topic/91301-auto-play-next-episode-device-specific-option/
- blog.frazile.com — Swiftfin "default off is the source of half the requests": https://blog.frazile.com/p/jellyfin-apple-clients
- XDA — MX Player loop options via long-press: https://xdaforums.com/t/q-disabling-auto-next.2933405
