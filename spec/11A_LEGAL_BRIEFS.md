# 11A — Legal Briefs (L1–L8) — prepared for counsel

**Version:** 0.2 (draft — **not legal advice**, and no position below has been reviewed by a lawyer); v0.1 contained L1–L2 only; v0.2 adds the **batch-B** briefs **L3–L8** (§4) in the same sendable format and updates the engagement notes
**Date:** 2026-10-19
**Status:** Draft, docs-only. These are **briefs to send to counsel**, written so that answering them is cheap: the facts first, our reading second (stated so it can be corrected), then numbered questions each with the decision it changes. Companion to doc 11, numbered `11A` following the `02A`/`08A` precedent (the §22 deliverable set 00–18 is unchanged).
**Consumes:** 11 §1.2/§2/§3/§5 (component + patent registers, LGPL process, L1–L8), 04 (FR-AUD-007, FR-SET-008), 10 §4.1/§6.2/§6.3 (data inventory, statutes, mirrored legal list), 15 (store-listing step, its S3), 08B (OD-6 — the name decision L6 depends on), `research/licensing.md` (W3).
**Feeds:** doc 11 register verdicts, the build-phase license CI gate, doc 00 §5 / L-item statuses.
**Scope of this document:** **L1–L8 in full.** L1/L2 (§2–§3) are the capability gates, sendable as-is since v0.1. **Batch B (§4)** adds the copy, statute and naming briefs: L3 DTS/TrueHD wording · L4 Dolby brand usage in UI text · L5 statute applicability · L6 name clearance and copycat response · L7 app-licence/distribution model · L8 store-copy claims policy. None of batch B gates a capability; the two owner-decision dependencies are named where they bind (OD-5 sharpens L5; OD-6 starts L6).

---

## 0. How to use this document

1. **Send in two batches.** **Batch A — §2 and §3** — as they stand (or after trimming §2.1's build detail, which is written for a lawyer who does not need to read FFmpeg's configure flags — but which does establish exactly what we ship): these gate capabilities. **Batch B — §4** — when convenient: L3/L4/L7/L8 can go immediately, L5 benefits from the launch-region answer (OD-5), and L6 cannot *start* without candidate names (OD-6), though its process questions can be answered now.
2. **What we are asking for is deliberately small:** a written position we can act on, not a full legal opinion. A short memo answering the numbered questions is sufficient for every decision below.
3. **Every question carries "what changes on the answer"** so counsel can see the stakes and skip anything immaterial.
4. **Our readings are hypotheses.** Where this document says "we read §6 as…", that is *our* reading for correction — not a claim about what the licence says.
5. **Every item has a designed posture if the answer narrows it** (L1 §2.6, L2 §3.5, each batch-B brief's closing note). **None of L1–L8 is on the R1 critical path** (doc 16 §7). Say that to counsel: it changes the advice from "urgent" to "correct", and good counsel will price it accordingly.

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

## 4. Batch B — the copy, statute and naming briefs (L3–L8)

Sent when the moment is right rather than urgently: **none of these gates a capability**, and two gain precision from owner decisions — **L5** from the launch-region answer (OD-5; our provisional set is India, Brazil, SE Asia, MENA + US/EU, ASM-009) and **L6** from candidate names (OD-6). They are written now so nothing has to be drafted under time pressure, and so the copy they govern (doc 06 §7, doc 15's listing) is never written twice.

| # | Brief | Gates | Ready to send? |
|---|---|---|---|
| L3 | DTS/TrueHD passthrough copy | doc 07 §7/§13 copy; doc 06 §7 strings | **Now** |
| L4 | Dolby brand usage in UI text | doc 06 §7 copy; doc 15 listing | **Now** |
| L5 | Statute applicability notes | Privacy page, store forms, doc 10 §6.2 statuses | Now on the provisional region set; sharpen after OD-5 |
| L6 | Name/trademark clearance + copycat response | doc 15 listing, `applicationId` pinning | **Process questions now; clearance waits for OD-6 names** |
| L7 | Own-app licence & distribution (F-Droid) | OQ-1 (decision support) | Now — explicitly lowest priority |
| L8 | Store-copy claims policy | doc 15 S3 gate; TST-VIS | Now — as a scoping question; may need no counsel at all |

### 4.1 Brief L3 — DTS / TrueHD passthrough wording, and why we never decode them

**Priority: medium** (doc 11 §5). **Gates:** the copy that doc 07 §7/§13 and doc 06 §7 will freeze. **Applies to:** DTS/DTS-HD and TrueHD. (AC-3/E-AC-3 are L2's subject and are *not* covered here.)

**The facts**

| Fact | Detail |
|---|---|
| The policy we would freeze | DTS/DTS-HD and TrueHD audio are **passthrough-only** in our product; **bundled decode of either is permanently excluded (X)** in the plan (doc 07 §7) — no app-side licence path was found for DTS `[U]`, and TrueHD is Dolby-branded |
| What happens at play time (FR-AUD-007) | (a) try platform decode; (b) if absent, route **bitstream passthrough** where device + route support it (the route matrix is RQ-04's open engineering item); (c) otherwise an honest error class + "play video without audio" |
| What the user would see | A track line naming the audio format, the honest outcome, and help text explaining device dependence. We never show brand logos; we never say the app "supports" or "decodes" DTS/TrueHD |
| Where our uncertainty is | We treat *factual codec naming* as acceptable descriptive use, but we cannot certify that judgment ourselves. We also need the rule for **Atmos** — a carriage label over E-AC-3/TrueHD — which we currently never mention anywhere, deliberately |

**Our reading (for correction).** Passthrough is a *device and route* capability that we merely route to; the app makes no decoding claim and no promotional use of any brand. Factual, non-promotional naming of the format a file *is* should be nominative-descriptive use; the line we must not cross is any wording implying the app provides the capability. We would rather agree the copy rule once — with your approval — than litigate strings at each release.

**Questions for counsel**

| # | Question | What changes on the answer |
|---|---|---|
| Q1 | Confirm the **passthrough-only, never-decode** posture for DTS/DTS-HD/TrueHD — and that the AC-3/E-AC-3 question is properly separate (L2) | Freezes doc 07 §7's rows; gives RQ-04's policy table its legal shape |
| Q2 | May our UI **name the codec factually** ("DTS", "Dolby TrueHD") in track info, error and help text — no licence, no logo/badge, no marketing — or must we use a neutral descriptor (e.g. "high-definition audio (not decodable on this device)")? | Chooses the copy pattern for doc 06 §7 and the help pages; a "no names" answer costs clarity (doc 14's troubleshooting flows) but no capability |
| Q3 | Is **Atmos** ever sayable in our surfaces (it appears in file metadata and users may search for it) — or does the safe rule remain "never in our strings", including playback stats? | Confirms or changes the doc 07 §13 banned list and the stats-surface silence rule |
| Q4 | For settings/help text describing passthrough availability ("your device or receiver may decode or pass through this format…"), is any qualifier or disclaimer expected, or is neutrality enough? | Settles the help-page template (written once, reused in error states) |
| Q5 | If a future release (R1.x+) added a **licensed** decode path (none planned), what changes then — marks, notices, royalty flow? | Not needed now; keeps the option priced if the owner ever asks |

**Posture if the answer narrows us.** Worst case (no codec names at all) costs clarity, not capability: playback behaviour is unchanged; only track-info and error wording changes, and the descriptor scheme is a single copy pass. Nothing here is on the launch critical path **as long as the question is answered before doc 06's copy freeze** (Release Gate B).

### 4.2 Brief L4 — Dolby brand usage in UI text

**Priority: medium** (doc 11 §5). **Gates:** doc 06 §7 copy strings and the doc 15 store listing. **Interacts with:** L2 (if AC-3/E-AC-3 add-back ever ships) and L3 (codec naming).

**The facts**

| Fact | Detail |
|---|---|
| Labels at stake | "Dolby", "Dolby Digital", "Dolby Digital Plus", "Dolby Vision", "Dolby Atmos" |
| Where they *could* appear in our surfaces | Playback stats / diagnostics (actual decode path), HDR honesty messaging (Dolby Vision is tier C — device/profile-dependent), track info, error/help text, store listing, support articles |
| What we never do | Decode DV or Atmos ourselves; show Dolby logos or badges; market "Dolby Vision/Atmos support" — already on the banned-claims list (doc 07 §13; doc 02 §7 #2) |
| The tension | Our honesty commitment (DH-8: explain what actually happened — no false HDR badge, no fake capability) collides with the safest reading of brand rules, which is *silence*. The boundary should be drawn by someone qualified, once |
| User content | A file's own metadata/title strings may contain "Dolby" — displayed verbatim as the user's content |

**Our reading (for correction).** Trademark questions are about *use in commerce implying authorisation*, so the sharp edge sits where we look authoritative: a compatibility table, a store listing, a badge. A stats line stating what the device's own path did is closer to a factual report about the device than a claim by us — but "closer" is exactly the kind of judgement we are not qualified to make, which is why this brief exists.

**Questions for counsel**

| # | Question | What changes on the answer |
|---|---|---|
| Q1 | For each surface, may Dolby format names appear factually and without a licence: (a) in-app stats/diagnostics; (b) error/help text; (c) the store listing; (d) support articles? Answer per surface | Decides four copy surfaces at once — the most specific thing we need |
| Q2 | Are there mandatory non-use rules to encode (never adjacent to our logo, never as a badge, never in the listing title/screenshots, …)? | Produces a lint list for doc 07 §13 / doc 06 §7 |
| Q3 | File metadata containing "Dolby" (titles, track names) — display verbatim, or filter? (We assume verbatim: it is the user's content.) | Prevents a filter that would harm the library experience |
| Q4 | Our honesty layer wants to say "Dolby Vision playback depends on your device" — is that sayable, or is generic "HDR" silence required? | Resolves honesty-vs-silence explicitly; picks the stats/HDR copy |
| Q5 | If L2 ever clears AC-3/E-AC-3 decode, does codec naming in an audio settings surface change (any notice/mark obligations)? | Pre-answers the R1.x add-back's copy cost |

**Posture if the answer is "silence".** Brand names leave *our* strings (never the user's metadata): stats say "dynamic HDR (device path)"; help text describes capability without formats. The honesty stance survives in substance — the user still learns what happened — at some cost in clarity and searchability. No capability is affected.

### 4.3 Brief L5 — statute applicability notes

**Priority: medium** (doc 11 §5). **Gates:** the privacy page, store forms (data safety, rating), doc 10 §6.2's status column. **Dependency:** answerable now on the provisional region set (India, Brazil, SE Asia, MENA + US/EU — ASM-009, provisional; OD-5 pending); refresh if that changes.

**The facts**

| Fact | Detail |
|---|---|
| Architecture | Offline-only: **no `INTERNET` permission in the shipping configuration**, no accounts, no backend, no analytics or crash SDKs, no ad IDs (DEC-012, ADR-007) |
| Data that exists (doc 10 §4.1 — the authoritative inventory) | On-device only: media files (read-only), an app-private library DB (names/paths/durations/codec info), watch history, favorites/playlists, thumbnails, settings, a ≤7-day log ring, and **user-initiated** diagnostics bundles that are redacted and shared only by user action |
| What never exists | Transfers to us, identifiers, accounts, contacts/location, ad data, media content or subtitle text in any external artifact |
| Regimes flagged (doc 10 §6.2, all ⚖️ review) | GDPR; CCPA/CPRA; India DPDP (Rules phasing to 2027-05-13); LGPD; COPPA; EAA (microenterprise caveats noted); anti-circumvention and export controls — the last two already resolved by design |
| What we plan to publish | An in-app privacy page mirroring the inventory; a Play data-safety form matching it exactly; a permission-rationale screen. What we do **not** know is whether any of this is *legally required* for our shape of app, or merely trust-and-formality |

**Our reading (for correction).** With no server and no identifiers, most of this register looks like "applicability low, statement still wise". But that is a research-level judgement (W4), and several regimes turn on definitions we are not qualified to apply — controller/processor, children-directed, consumer-facing, microenterprise. We ask for an **applicability memo**, not a compliance build-out.

**Questions for counsel**

| # | Question | What changes on the answer |
|---|---|---|
| Q1 | GDPR: with fully on-device processing and no transfers, does controller/processor analysis apply at all — and is any record-keeping or DPIA wording expected? | Decides whether our privacy page is a legal artifact or a trust artifact (we believe trust + store consistency) |
| Q2 | India DPDP: for an offline app, which phased duties (notice/consent, children's provisions) attach at R1, and does the phasing timeline change our launch posture? | Permission-rationale and privacy-text wording; possibly nothing |
| Q3 | LGPD: is a Portuguese privacy text needed at launch, given Brazil is in the provisional first set? | Localization gate in doc 15 |
| Q4 | COPPA/rating: we plan "not directed to children", truthful IARC answers, no UGC, no ads. Any affirmative duty we are missing for a player that *can* display any local video? | Listing/rating posture; possibly nothing |
| Q5 | EAA: does the microenterprise carve-out plausibly apply to a free app with no revenue, and is any accessibility *statement* required — or do our accessibility commitments stay purely product policy? | Statement template in doc 15 (we keep the commitments either way) |
| Q6 | CCPA/CPRA: confirm no sale/share notices are needed, and whether a privacy policy is required or merely advised for store-form consistency | Store forms and privacy page |
| Q7 | What record should we keep of these applicability determinations, and at what cadence should they be revisited (per release? per region expansion?)? | doc 10's maintenance model and the release-gate checklist |

**Posture.** Every possible answer is cheap: the product's data posture does not change; only the paperwork and wording do. This brief's output is also the answer to doc 10 §8 S5 (privacy-page language for the DPDP/GDPR interplay).

### 4.4 Brief L6 — name/trademark clearance and copycat response

**Priority: medium** (doc 11 §5). **Gates:** the store listing and `applicationId` pinning (08B OD-6: changing the id later means a **new listing** — installs, reviews and ranking lost). **Dependency:** the process questions can be answered now; **clearance waits for OD-6's two-or-three candidate names.**

**The facts**

| Fact | Detail |
|---|---|
| Where the name is used | Play listing (title/subtitle), app icon, in-app About, support articles, a future FOSS channel (L7) |
| Timeline pressure | Name freeze pins `applicationId`; a later rename is a relaunch in practice (08B OD-6) |
| Search budget assumed | None formal; this brief asks what "enough" buys and costs, so the owner can decide |
| Copycat context | doc 10 §8 S6 (response playbook) is an open item; we would use Play's complaint routes, whose strength usually tracks a registered right |

**Our reading (for correction).** For a free app at R1, the realistic package is: (1) a **knockout clearance** per finalist (store search plus right-checks on the classes an app occupies) so a name can be frozen; (2) a registered mark only if the owner wants a durable complaint lever; (3) an evidence folder assembled *before* a copycat appears (listing URLs, screenshots, icon comparison, first-use dates) so complaints are one-hour jobs. We are unsure which classes and jurisdictions matter for an app-only product distributed from Play.

**Questions for counsel**

| # | Question | What changes on the answer |
|---|---|---|
| Q1 | For two or three finalists (supplied under OD-6), what search scope do you advise — jurisdictions, classes, depth — and will you run it or should we commission a search house? | Selects the search package, its price and the name-freeze date |
| Q2 | Is registration (which marks/classes/locations) worth doing *before* launch for a free app, or is a later filing on traction the better buy? | Adds or removes a filing budget line; sets copycat-lever strength |
| Q3 | What is the response ladder for a copycat listing (report → IP complaint → escalation), and what evidence should we pre-assemble? | Produces the doc 10 S6 playbook skeleton |
| Q4 | Does anything beyond the word mark need clearance — icon style, store subtitle phrasing? | Scopes the search to visual/wordmark aspects |
| Q5 | If a finalist touches another product's mark in an adjacent category (media tools), what risk threshold should trigger a rename *before* we publish? | Gives the owner a go/no-go rule at name freeze |

**Posture.** If every finalist fails clearance, the fallback is a descriptive/generic name (weaker mark, faster clearance) — an owner decision made with numbers, not hope. This item gates no engineering work except `applicationId` pinning.

### 4.5 Brief L7 — own-app licence and distribution model (incl. F-Droid)

**Priority: low** (doc 11 §5 — post-spec). **Gates:** OQ-1 only. **Decision support, not a gate:** R1 is Play-only (DEC-009).

**The facts**

| Fact | Detail |
|---|---|
| The open decision (OQ-1) | Proprietary vs FOSS (e.g. Apache-2.0) vs copyleft (GPLv3) — or a sequence (proprietary first, FOSS later) |
| The one bundled third-party library | The FFmpeg **audio** extension — LGPL-2.1, dynamically linked — compatible with a proprietary app *if L1's mechanics hold*; also compatible with a FOSS app |
| F-Droid (deferred channel) | Requires a FOSS app licence, FOSS-clean dependencies and reproducible builds; our no-`INTERNET`, no-analytics profile is friendly to it. Whether our own LGPL build satisfies F-Droid's criteria is exactly what we have not verified |
| What an app licence does *not* change | The L1–L6 analyses (bundled-library compliance, patents, brand copy, statutes, name) are invariant under the choice |

**Our reading (for correction).** This is primarily an owner strategy decision (community, contribution, trust) with legal consequences, not a legal question. Counsel's value is a short compatibility matrix plus a warning list (copyleft interactions with future proprietary ambitions; contributor and licence-header hygiene).

**Questions for counsel**

| # | Question | What changes on the answer |
|---|---|---|
| Q1 | For each option (proprietary / Apache-2.0 / GPLv3 / sequence), what constraints arise with the LGPL audio extension and Play distribution? | The OQ-1 decision matrix for the owner |
| Q2 | F-Droid: what would our LGPL extension require there (build provenance, reproducibility, source distribution), and does shipping there create obligations back to Play users? | Whether F-Droid stays deferred or gets costed now |
| Q3 | If we accept outside contributions later, what should exist *before* the first one (licence headers, CLA or DCO, licence clarity)? | Contribution hygiene — cheap now, expensive later |
| Q4 | Does a FOSS release affect name/trademark posture (forks, modified builds using the name) — interacting with L6? | Whether a trademark policy ships alongside the code licence |
| Q5 | Does open-sourcing change any L5 conclusion? (We believe not — the data flows are identical.) | Closes the loop; prevents double work |

**Posture.** No shipping consequence either way; the brief can wait until the owner wants OQ-1 decided. If it is never sent, nothing in R1 changes.

### 4.6 Brief L8 — store-copy claims policy (and why this may not be a counsel brief at all)

**Priority: gate-each-release** (doc 11 §5). **Gates:** doc 15's store-listing step (its S3 claims audit; TST-VIS in doc 12). **Honest scope note:** most of L8 is *our* audit process — doc 07 §13's claims discipline (banned list, name-the-cell rule, container ≠ codec) is already product policy. The only legal content is whether any claim *categories* need counsel's blessing once.

**The facts**

| Fact | Detail |
|---|---|
| Existing discipline | doc 07 §13: copy consumes validated tiers only; banned patterns list; "no data collection" not usable as a slogan; comparative claims excluded |
| Existing process | doc 15 S3: listing copy audited against doc 07 §13 before submission (Release Gate B); screenshots from real UI; TST-VIS covers in-app and store |
| The legal residual | Claim wording that could be challenged: the "no data collection / works offline" class, pricing/"free" wording, anything comparative. One review of the *policy* would let the per-release audit stay internal |

**Questions for counsel**

| # | Question | What changes on the answer |
|---|---|---|
| Q1 | Would you review the **claims policy once** (doc 07 §13 + doc 15's checklist) and bless the categories, so per-release audits remain internal? (Our preferred model.) | Sets whether every release needs counsel — this item's entire cost model |
| Q2 | Are there claim classes we have not flagged (pricing/"free" wording, offline/privacy slogans, accessibility claims, platform-compatibility phrasing) that deserve standing rules? | Adds lint rules to doc 15 / doc 07 §13 |
| Q3 | Is the Play **data-safety form's** "no data collected" wording something you want to approve once (same claim family)? | Assigns that gate's ownership |
| Q4 | If a claim is ever challenged, what evidence record should we have kept (doc 13's internal-claims evidence cells)? | Links claims discipline to evidence retention |

**Posture.** If the answer is "this is entirely your audit, we see no legal issue", that is a useful, on-the-record result — the gate stays internal with a counsel-blessed policy.

---

## 5. Engagement notes

| Item | Detail |
|---|---|
| **Deliverable we need** | A short written position answering the numbered questions, with a clear yes/no/"depends on X" per question. A memo is enough; no opinion letter required for any item |
| **Priority order** | **Batch A: L1 before L2** (doc 11 §5) — L1 gates a build decision; L2 gates an optional add-back with a working default. **Batch B (§4) is not urgent** — L3/L4 must land before doc 06's copy freeze, L5 sharpens after OD-5, L6 *starts* at OD-6, L7/L8 are cheap now and annoying later; none gates launch. If a single engagement is simpler, send §2–§4 together and let counsel triage |
| **What we explicitly do not need** | A general survey of open-source licensing, or a comparison of how other players resolve §6. Our questions are specific and the fallbacks are already designed |
| **Facts that will age** | Everything in §3.2 is `recheck`-flagged; the patent rows are re-verified at every release gate (doc 11 §2). Do not treat any date in this brief as settled |
| **Coverage** | **Nothing is unbriefed.** v0.1 listed L3–L8 as follow-on items; v0.2 briefs them all (§4). Remaining dependencies are owner-side: **OD-5** (launch regions) sharpens L5; **OD-6** (candidate names) starts L6 |

---

## 6. Boundaries

- **This document is not legal advice**, and nothing in it has been reviewed by a qualified lawyer. Its readings exist to be corrected — that is why they are written down rather than assumed.
- **Nothing here changes a shipping decision.** L1/L2 have designed fallbacks (§2.6, §3.5); the batch-B items cost wording, forms or paperwork — never product behaviour. The register's verdicts (doc 11 §1) remain **Conditional ⚖️** until counsel answers.
- **No expiry date or pool statement is treated as established fact**; where our research is secondary or silent, it is labelled `[R]`/`[U]` and carried as a question rather than a premise.
