# 11A — Legal Briefs (L1, L2) — prepared for counsel

**Version:** 0.1 (draft — **not legal advice**, and no position below has been reviewed by a lawyer)
**Date:** 2026-10-19
**Status:** Draft, docs-only. These are **briefs to send to counsel**, written so that answering them is cheap: the facts first, our reading second (stated so it can be corrected), then numbered questions each with the decision it changes. Companion to doc 11, numbered `11A` following the `02A`/`08A` precedent (the §22 deliverable set 00–18 is unchanged).
**Consumes:** 11 §1.2/§2/§3/§5 (component + patent registers, LGPL process, L1–L8), 04 (FR-AUD-007, FR-SET-008), 10 §6.3 (mirrored legal list), `research/licensing.md` (W3).
**Feeds:** doc 11 register verdicts, the build-phase license CI gate, doc 00 §5 / L-item statuses.
**Scope of this document:** **L1 and L2 only** — the two items that gate a *product* capability (L3–L8 are copy, statute and naming matters, handled separately; §5).

---

## 0. How to use this document

1. **Send §2 and §3 to counsel** as they stand (or after trimming §2.1's build detail, which is written for a lawyer who does not need to read FFmpeg's configure flags — but which does establish exactly what we ship).
2. **What we are asking for is deliberately small:** a written position we can act on, not a full legal opinion. A short memo answering the numbered questions is sufficient for every decision below.
3. **Every question carries "what changes on the answer"** so counsel can see the stakes and skip anything immaterial.
4. **Our readings are hypotheses.** Where this document says "we read §6 as…", that is *our* reading for correction — not a claim about what the licence says.
5. **Both items have a safe fallback already designed in** (§2.6, §3.5). Neither is on the R1 critical path (doc 16 §7). Say that to counsel: it changes the advice from "urgent" to "correct", and good counsel will price it accordingly.

**Two facts to keep in front of any reader:**

- **R1 ships no bundled video decode and no bundled AAC.** The only bundled third-party decoder set in the plan is an FFmpeg **audio** decode subset built by us (§2.1), and it ships only if L1 clears.
- **R1 ships no DRM and no runtime-downloaded codec** (DEC-006, ADR-011) — both are excluded at the design level, which removes whole categories of question from this brief.

---

## 1. The product context counsel needs (short form)

- An Android-only, local-file video player (no accounts, no backend, **no `INTERNET` permission in the shipping configuration** — DEC-012).
- Distribution: **Google Play only** at R1; a possible FOSS channel (F-Droid) is deferred and would be a separate build decision (DEC-009, OQ-1).
- Free at launch; no ads; no analytics or crash-reporting SDK (ADR-007).
- Playback uses **platform decoders** (MediaCodec/Codec2) by default. Platform decode is the device maker's compliance domain; we bundle no video decoder.
- The one bundled media library under review is a **self-built FFmpeg audio extension** (§2).

---

## 2. Brief L1 — LGPL-2.1 §6 relinking for a Play-distributed app

**Priority: highest** (doc 11 §5). **Blocks:** the bundled FFmpeg audio extension (ADR-010). **Fallback exists** (§2.6), so this is not a launch gate.

### 2.1 The facts — exactly what we intend to ship

| Fact | Detail |
|---|---|
| Library | FFmpeg, built from upstream source, **configured without `--enable-gpl` and without `--enable-nonfree`** so the result is **LGPL-2.1-or-later** |
| Scope of the subset | **Audio decode only**, restricted to a *license-cleared codec list* (doc 11 §2). No video decode, no encoders, no AAC (ADR-010) |
| Linkage | **Dynamic** — a shared library (`.so`) loaded by the app; explicitly *not* statically linked, and we treat static linking as a trap to avoid |
| Packaging | Android App Bundle (`.aab`) uploaded to Play; Play generates **per-device APK splits**, signed by Play (app signing), installed by the user from Play |
| Distribution | Google Play only. No side-loading channel at R1, no runtime download of codecs (ADR-011), no `INTERNET` permission in the app |
| Our build | Reproducible from pinned source; the exact configure flags, build scripts and a `changes.diff` against upstream are retained as release artifacts |
| In-app | Licence text + notices in "About → open source" (FR-SET-008); the source offer is referenced from there |
| Our intent | Comply fully with LGPL-2.1 §6, including the corresponding-source obligation, *and* satisfy whatever "work that uses the library" mechanics apply to an app that is not itself LGPL |

### 2.2 Our reading of the obligation (for correction)

We read LGPL-2.1 §6 as requiring, in substance: (a) the library remains under LGPL and its source (including our modifications) is made available; (b) the application that *uses* the library may be under its own terms, provided it is a "work that uses the library"; and (c) the recipient must be able to **relink** the application with a modified version of the library and use the result — which in practice means dynamic linking plus either relinkable object files or an equivalent mechanism, with a written offer as an alternative where applicable.

### 2.3 Why we are asking rather than assuming

Three features of our distribution make the §6 mechanics non-obvious in a way that a desktop-software reading of the licence does not settle:

1. **The user cannot relink a Play-delivered app.** A per-device APK split is signed; a user cannot substitute a `.so` and re-sign the app the way they could rebuild a desktop program.
2. **Play is the effective distributor** of the compiled artifact, not us.
3. **Object files can be published, but a *fully relinked application* requires our own (proprietary) object code** — the classic §6 doubt, and the one we will not resolve by reading the FAQ of a different project.

### 2.4 The compliance package we propose to implement (say if it is wrong)

1. **Corresponding source:** exact FFmpeg version + our `changes.diff` + complete build scripts, published at a stable, no-account, no-payment URL.
2. **Relinkability:** publish the relinkable objects (the app-side `.o`/intermediate objects needed to relink against a modified library) **or** whatever counsel specifies as the accepted mechanism.
3. **Notices:** LGPL-2.1 text, attributions, and a clear statement of the library's version and modifications, in-app and on the store listing.
4. **No static linking; no GPL/nonfree configuration** — enforced as a CI gate that fails the build if a forbidden flag or a non-cleared decoder appears (doc 11 §3.4).
5. **Written offer** where the licence permits that alternative to shipping the objects directly.

### 2.5 Questions for counsel (each with what changes on the answer)

| # | Question | What changes on the answer |
|---|---|---|
| Q1 | Does the package in §2.4 satisfy LGPL-2.1 §6 for an app distributed **exclusively through Google Play as an AAB with per-device splits**? | **Positive** → the audio extension ships and the build config is frozen. **Negative** → §2.6 |
| Q2 | For the purposes of §6, is Google Play the "distributor" of the binary, and does that change what *we* must provide to the end user? | Determines whether obligations attach to the store listing/artifacts as well as to us |
| Q3 | Is publishing relinkable objects (§2.4 item 2) sufficient, given that the recipient cannot practically re-sign and install a modified app on a non-rooted device? If not, what is the accepted compliance mechanism for store-distributed Android apps? | This is the crux. **If "objects are sufficient"** → proceed as designed. **If not** → §2.6 |
| Q4 | Does the **audio-only** scope or the **dynamic-only, non-static** linkage change the analysis (for instance, is there a lighter-touch obligation set for a dynamically-linked, unmodified-API use)? | Could reduce the package to source offer + notices only |
| Q5 | If we ship **without** the extension, are we left with any obligation because an earlier release or a beta build included it (source offer already published, notices, retention)? | Determines whether the fallback is clean and what must be kept online |
| Q6 | How should the source offer be hosted and for how long (in-app link, store-listing link, repository, a permanent URL) to satisfy the obligation without creating a maintenance trap? | Chooses between OQ-3 hosting options |
| Q7 | If we modify FFmpeg (patch) rather than use it verbatim, does anything in §2.4 change beyond publishing `changes.diff`? | Determines how much engineering freedom we have in the extension |

### 2.6 Fallback (already designed, so counsel can price this as "nice to have")

If the answer to Q3 is that the mechanics cannot be satisfied for a store-distributed app at acceptable cost: **ship without the bundled audio extension.** Platform/device codecs remain; FR-AUD-007's bundled add-back is dropped; the compatibility tiers in doc 07 degrade honestly and the product loses a compatibility *add-on*, not a headline claim (doc 11 §3.5). **No other part of R1 depends on this item.**

### 2.7 What we are *not* asking counsel to opine on here

FFmpeg's internal patent status (that is L2 and doc 11 §2), the app's own licence model (L7), the F-Droid channel (deferred), or any claim about how other apps have resolved §6 — we are asking for our position, not a survey.

### 2.8 Assets we can provide with this brief

Source archive + version pin · configure flags + build scripts · decoder list actually compiled in · `changes.diff` · the CI deny-list rules · a description of the AAB→splits delivery path with the signing chain (so Q2/Q3 can be answered precisely rather than from general Android knowledge).

---

## 3. Brief L2 — AC-3 / E-AC-3 bundled decode (patent expiry and pool posture)

**Priority: high** (doc 11 §5). **Blocks:** the FR-AUD-007 add-back (an R1.x candidate, not R1). **Fallback is the current default** (§3.5), so this gates an *addition*, never the launch.

### 3.1 The facts

| Fact | Detail |
|---|---|
| What we would bundle | **Decode only**, inside the same self-built FFmpeg audio extension as L1 — no encoder, no branding, no Dolby marks in our UI beyond factual device-path descriptions (L4 covers copy) |
| Why | AC-3/E-AC-3 audio is common in downloaded content; without bundled decode, playback depends on what each device will decode or pass through, which our research shows is device-dependent |
| Where it goes | Only after both L2 clearing **and** the L1 mechanism (they are independent: L2 is patent exposure, L1 is licence mechanics) |
| Our default today | **No bundled AC-3/E-AC-3 decode.** The product ships platform-only for these codecs |

### 3.2 What our research found (for verification — all of it is `recheck`-flagged)

- **AC-3:** patents reported expired in 2017 `[V1]`.
- **E-AC-3:** the last US patent reported expired **2026-01-30** `[R, recheck]` — i.e. very recent, and sourced from secondary reporting rather than a licensor statement.
- **Licensing pools:** no current pool contact or rate card found for either codec in our research `[U]` — "pool silence", which we treat as *unknown*, not as *permission*.

### 3.3 Our reading (for correction)

If both codec families are, as of our shipping date, unencumbered by live patents in our distribution territories, bundled **decode-only** use for these two codecs presents low risk — lower than for any codec we currently exclude. We are specifically unsure whether "no licensor has told us anything" is a basis for comfort, or merely a gap in our research.

### 3.4 Questions for counsel

| # | Question | What changes on the answer |
|---|---|---|
| Q1 | Is it correct that AC-3 decode is unencumbered in **our distribution territories** as of the shipping date, and on what basis should we record that (rather than relying on a 2017 expiry summary)? | **Positive** → AC-3 may join the cleared decoder list |
| Q2 | Is the reported **2026-01-30** expiry of the last US E-AC-3 patent confirmed, and are there **non-US** counterparts (EP/CN/JP/etc.), continuations or divisions that remain live? | Decides whether E-AC-3 ships with AC-3 or stays excluded |
| Q3 | Does the **absence of a reachable licensor** ("pool silence") need to be treated as a live obligation, or does established practice treat expired-patent subject matter as free regardless of pool activity? | Determines whether we can act on Q1/Q2 without a licensor statement |
| Q4 | Does **decode-only** bundling (no encoder, no Dolby marks, no pass-through claims, no brand use in UI copy) differ from a general "product that decodes AC-3" analysis for these codecs? | Confirms scope of any residual obligation |
| Q5 | What ongoing verification do you advise — a per-release patent recheck (our current plan, doc 11 §2), or something lighter/heavier? | Sets the maintenance cost of keeping the capability |
| Q6 | If only AC-3 clears and E-AC-3 does not, is shipping **one of the two** coherent from a risk and messaging standpoint? | Decides whether the add-back is partial |

### 3.5 Fallback and posture

The default posture (platform-only for AC-3/E-AC-3) **already excludes bundled decode**, so a negative answer changes nothing in the shipping product — it simply closes a candidate. FR-AUD-007's "honest UX for every outcome" requirement applies either way, and no store copy in the package depends on this outcome (doc 07 §13).

---

## 4. Engagement notes

| Item | Detail |
|---|---|
| **Deliverable we need** | A short written position answering the numbered questions, with a clear yes/no/"depends on X" per question. A memo is enough; no opinion letter required for either item |
| **Priority order** | **L1 before L2** (doc 11 §5) — L1 gates a build decision; L2 gates an optional add-back with a working default |
| **What we explicitly do not need** | A general survey of open-source licensing, or a comparison of how other players resolve §6. Our questions are specific and the fallbacks are already designed |
| **Facts that will age** | Everything in §3.2 is `recheck`-flagged; the patent rows are re-verified at every release gate (doc 11 §2). Do not treat any date in this brief as settled |
| **Follow-on items (not in this brief)** | L3 DTS/TrueHD passthrough wording · L4 Dolby brand usage in UI copy · L5 statute applicability (GDPR/DPDP/LGPD/EAA/COPPA) · L6 name/trademark clearance (needs OD-6) · L7 own-app licence/distribution model · L8 per-release store-copy audit |

---

## 5. Boundaries

- **This document is not legal advice**, and nothing in it has been reviewed by a qualified lawyer. Its readings exist to be corrected — that is why they are written down rather than assumed.
- **Nothing here changes a shipping decision.** Both items have a designed fallback; the register's verdicts (doc 11 §1) remain **Conditional ⚖️** until counsel answers.
- **No expiry date or pool statement is treated as established fact**; where our research is secondary or silent, it is labelled `[R]`/`[U]` and carried as a question rather than a premise.
