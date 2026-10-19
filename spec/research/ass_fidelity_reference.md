# W11 — ASS/SSA Fidelity Reference (sources for the doc 07 §8.1 bar)

**Accessed:** 2026-10-19 · **Feeds:** doc 07 §8.1 (the RQ-06 quality bar), doc 08 §6 (T6/RQ-06 row), 08A (RQ-06 card, RB-5), 12A §2.6.

Purpose: the external anchors for the **SSA/ASS fidelity quality bar** — what the format can say, what the reference renderer is, and what the native Media3 path is currently known to do. The bar itself is a scope decision (doc 07 §8.1); this note fixes only the facts it stands on.

## S1 — The native path's known state (why a bar is needed)

**ExoPlayer issue #8435 — "Support SSA/ASS styling"** · https://github.com/google/ExoPlayer/issues/8435 · opened 2021-01-07, **verified open** on access · labels `enhancement` / `low priority` `[V1]`
- Request text: *"Currently there is only 'minimal' support for ASS subtitles since styling was removed from the initial PR"* (#2281).
- Maintainer (icbaker, 2021-01-07): *"we don't currently have any capacity to improve ExoPlayer's SSA/ASS styling support"*; later comments explain the structural reason — ExoPlayer renders subtitles as **cues** (Canvas/WebView) while libass renders **a frame from a script**; cue machinery and styled ASS do not fit together.
- 2025 direction: a libass-based renderer **will not be merged into the main Media3 repository** (native-dependency policy); the maintainer-suggested route is libass driving a **custom video effect** with a bitmap overlay — *"a design like this will have to exist as an extension maintained outside the main media3 repository."*
- Community proof of route: `peerless2012/libass-android` (a plugin exposing `buildWithAssSupport(...)`); a 2026-09-12 comment still shows the gap in the wild: *"the ass subtitles are poor and I can't read them as they are white with no borders"* `[R]`.
- **Consequence for us:** the native ceiling is a *known-small* subset, and the else-branch (libass integration) has a documented shape and maintenance model. **Recheck before B0 execution** — the issue may change state.

## S2 — What an ASS script can say (the taxonomy the bar's classes are cut from)

**Aegisub manual — "ASS Override Tags"** · https://aegisub.org/docs/latest/ass_tags/ `[V1]`
- The page's own claim: *"a list of every tag supported by the Advanced Substation Alpha format."*
- Line-level tags (property of the whole line): `\an`/`\a`, `\pos`, `\move`, `\org`, `\fad`/`\fade`, `\clip`/`\iclip` — `\pos`/`\move` mutually exclusive.
- Inline styling: `\b`, `\i`, `\u`, `\s`, `\fn`, `\fs`, `\fscx`/`\fscy`, `\fsp`, `\c`/`\1c`–`\4c`, `\alpha`/`\1a`–`\4a`, `\bord`/`\xbord`/`\ybord`, `\shad`/`\xshad`/`\yshad`, `\be`, `\blur`.
- Transforms: `\frx`/`\fry`/`\frz`/`\fr`, `\fax`/`\fay`, `\t(...)` — animation is limited to a listed tag set; for `\clip`/`\iclip` only the **rectangle** form is animateable.
- Karaoke: `\k`, `\K`/`\kf` (sweep), `\ko`, `\kt` (relative timing).
- Structure: `\N` / `\n` / `\h`; `\q` wrap style; `\r` reset.
- Drawings: `\p`/`\pbo` + commands `m`, `n`, `l`, `b`, `s`, `p`, `c`; vector clips use the same command set.
- The manual notes unrecognized override text is *silently ignored* by design — a parser falling back to plain text is the format's own contract, which is why the bar's plain-text floor is fair rather than inventive.

## S3 — The reference renderer

**libass README** · https://github.com/libass/libass `[V1]`
- *"libass is a portable subtitle renderer for the ASS/SSA … format. It is mostly compatible with VSFilter."* Latest release **0.17.5 (2026-06-24)**; ISC license (doc 11's register already carries the conditional-entry row).
- The reference is itself a *compatibility target* ("mostly compatible"), not a perfect oracle — the bar calibrates against it without promising parity.
- Renderer landscape (already recorded, not re-derived here): **VLC and mpv use libass** (02A §3.6, §11.2 — VLC `[V2 source-derived]`, mpv `[V1/R]`); Just Player / Next Player show the Media3-native ceiling in shipped apps ("limited" / "Media3", 02A §11.2).

## S4 — Where these facts are used
- **doc 07 §8.1** — the bar: plain-text floor, Classes E/F/X, adjudication rule, freezing rule.
- **doc 08 §6.1/§6 T6, RQ-06** — pass criterion now written; calibration order (reference renders first).
- **08A RQ-06 card + RB-5** — procedure and the resolved blocker.
- **12A §2.6** — fixture tiers mapped to bar classes.

**Recheck before B0 (all volatile):** issue #8435 state; libass release; the competitor rendering claims in 02A §11.2.
