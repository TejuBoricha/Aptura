# 11B — Legal research findings: codecs, FFmpeg and licences (self-conducted)

**Date:** 2026-11-14 · **Scope:** the decoder decision (DEC-014, LEG-02) and what follows from it · **Answers:** briefs L1–L4 of
[11A](11A_LEGAL_BRIEFS.md) as far as primary sources allow; L5–L8 are not covered here (see §9).

> **What this is, and is not.** This is research and a reasoned position, done without a lawyer because the owner decided there would
> be none. It was written by an AI coding agent. **It is not legal advice and not a clearance.** Every claim carries an evidence
> grade, and §9 lists what could not be established. Where a conclusion depends on a judgement a court would make, it says so.

**Evidence grades.** **A** = primary: statute, licence text, court opinion, patent record, SEC filing, the licensor's or project's own
page. **B** = derived: reasoned from A sources, or a reputable secondary source. **C** = community material (forums, threads).
**U** = could not be verified. Source numbers `[S#]` are in §10.

---

## 1. Bottom line

1. **There are two separate problems, and the second matters more.** (i) *Copyright licences* (what we must do to be allowed to
   redistribute FFmpeg): manageable, and clearer than it looked. (ii) *Patents* (whether shipping a decoder for a codec infringes a
   live patent): depends on the codec, and the decision should be made codec by codec.
2. **The "GPL-3.0" label is not what it seemed.** The prebuilt decoder is built from upstream FFmpeg `release/6.0` by AndroidX
   Media's own script, with no GPL, version3 or nonfree option, so the binary is very probably LGPL-2.1+ plus Apache-2.0 `[A build
   files, B for the binary]`. The label is on Jellyfin's repository and POM, which add only build scripts. **The real licence
   obligation is LGPL-2.1 §6, which is satisfiable** (§3).
3. **What we ship today is not defensible and has already been published.** The public pre-release `v1.0.0` (4 downloads) bundles
   12 decoders, including AAC, AC-3, E-AC-3, DTS, TrueHD and MLP, FFmpeg linked *statically*, with no LGPL notice or licence text in
   the APKs, and its release notes say the opposite ("LGPL-clean via dynamically linked FFmpeg (FLAC, Opus, Vorbis)"). **Decided:**
   withdraw the binaries and correct the notes (§8, D6). **Not yet done**: the agent's permission layer blocked every change to the public
   release, so the owner needs to run the commands in appendix A.
4. **The platform already guarantees the common free codecs.** Android's CDD requires FLAC, MP3, Vorbis and Opus decoding on every
   compatible device `[A]`. Bundling them adds legal and security surface and no capability on our minSdk 26.
5. **Decisions** (§8): bundle only **ALAC and G.711 A-law/µ-law** (free, and not guaranteed by the platform); make **AC-3 and E-AC-3**
   the first add-back, under conditions that can be checked; **exclude DTS, TrueHD/MLP and AAC** and leave them to the platform and
   pass-through; re-check DTS on 2027-02-07.

## 2. Facts established (verified)

| Fact | Evidence |
|---|---|
| `v1.0.0` is a public GitHub **pre-release** with 5 binaries (APKs + AAB), 4 downloads, published 2026-10-30; its tag contains the Jellyfin decoder 1.5.0+1 | `gh release view`, `git show v1.0.0:gradle/libs.versions.toml` `[A]` |
| Aptura is public and Apache-2.0 | GitHub API, `LICENSE` `[A]` |
| Jellyfin's `build.sh` at tag `v1.5.0+1` enables `flac alac pcm_mulaw pcm_alaw mp3 aac ac3 eac3 dca mlp truehd` and calls AndroidX Media's `build_ffmpeg.sh`, which sets no GPL/nonfree/version3 option | `[S3][S6]` `[A]` |
| FFmpeg comes from `https://git.ffmpeg.org/ffmpeg.git` branch `release/6.0` (upstream, not a fork) | `[S4]` `[A]` |
| The Jellyfin repo and POM are labelled GPL-3.0; what Jellyfin adds to the AAR is publishing configuration | `[S5]` `[A]`; that nothing else of theirs is inside the AAR is `[B]` |
| The library in the APK contains 12 decoders (aac, aac_latm, ac3, alac, dca, eac3, flac, mlp, mp3, pcm_alaw, pcm_mulaw, truehd), 16 KB-aligned, FFmpeg linked statically into `libffmpegJNI.so` | our dump, all four ABIs `[A]` |
| The build record (`configure` line) is not present in the binary | our scan `[A]` |
| Release notes of `v1.0.0` claim "LGPL-clean via dynamically linked FFmpeg (FLAC, Opus, Vorbis)", "Official Production Release" and "Verified 4K 60fps and HDR10" | `[S31]` `[A]` |
| The APKs carry no LGPL notice or licence text and the app has no in-app licence screen; `NOTICE` exists only in the repository | repo search `[A]` |

## 3. Copyright and licences (brief L1)

**3.1 What LGPL-2.1 requires of us.** `[S2][A]` Linking a "work that uses the Library" with the Library "creates an executable that is a
derivative of the Library" (§5); §6 then lets us distribute it "under terms of your choice, provided that the terms permit
modification of the work for the customer's own use and reverse engineering for debugging such modifications". We must give
"prominent notice with each copy … that the Library is used in it", supply a copy of the licence, and do **one** of: (a) accompany the
work with the Library's complete source *and* the "work that uses the Library" as object code or source "so that the user can modify
the Library and then relink"; (b) use a suitable shared-library mechanism; (c) a written offer valid at least three years; (d) offer
equivalent access to copy from a designated place. §10 forbids further restrictions on recipients. §8 ends our rights
**automatically** on any violation.

**3.2 FFmpeg's own checklist** `[S1][A]` (not the only way, but "the easiest"): no `--enable-gpl` or `--enable-nonfree`; dynamic linking;
distribute the exact corresponding source on the same server as the binary; record the configure line; name FFmpeg correctly; mention
LGPLv2.1 in the about box; no reverse-engineering prohibition.

**3.3 Brief L1's "crux" is smaller than it was drafted.** 11A worried that a Play user "cannot relink" and that "a fully relinked
application requires our own (proprietary) object code". Both points fall away for this project:
- Aptura's whole source is **public under Apache-2.0**, so §6(a)'s "work that uses the Library, as object code and/or source" is
  satisfied by pointing at the repository tag. Apache-2.0 allows modification and does not forbid reverse engineering `[S21 A]`.
- LGPL-2.1 contains **no installation-information clause** (we read the full text; that concept belongs to version 3). The obligation
  is to supply materials so the user *can* relink, not to make the result installable on a locked device.
- Static linking is therefore not fatal, but dynamic linking remains the cleaner route and is what ADR-010 asks for.

**3.4 Google Play.** `[S27][A]` Under the Developer Distribution Agreement Google acts as our agent and merchant of record in listed
territories (§3.4), and **we** warrant that we hold or have permission for all rights in the product including third-party material
(§11.1–11.2) and **indemnify Google** against claims that the product infringes any IP right (§14.1(b)). So Play being "the
distributor" does not move the obligations: they stay with us. The extracted DDA text has no clause forbidding end-user reverse
engineering of apps; Google Play's *end-user* terms were not checked `[U]`.

**3.5 Are these licences enforceable? Yes.** *Jacobsen v. Katzer*, 535 F.3d 1373 (Fed. Cir. 2008) `[S23 A]`: open-source licence
conditions are enforceable through copyright, not merely as contract. *Artifex v. Hancom*, No. 3:16-cv-06982 (N.D. Cal.) `[S24 A]`: a
GPL claim survived a motion to dismiss as a contract claim. *SFC v. Best Buy / Westinghouse*, No. 1:09-cv-10155 (S.D.N.Y.) `[S25 A]`:
BusyBox/GPL enforcement against product distributors, including a contempt motion. Consequence: non-compliance is a real exposure, with
termination under §8. *VLC for iOS* was pulled from Apple's store in 2011 after a GPLv2 author objected `[S28 A/B]`. That is a **GPL**
dispute, and does not transfer to an LGPL library used under §6.

**3.6 Present gaps (all fixable, none optional):** no licence text or notice inside the APKs; no statement of the *exact* FFmpeg source
and configure line corresponding to the binary; the in-app/Settings text names codecs but not licences; the release notes misdescribe
the linking and the decoder list.

## 4. Patents (briefs L2, L3)

**4.1 Framework.** `[S22 A]` A US patent runs 20 years from its US filing date (35 U.S.C. §154(a)(2)); whoever "makes, uses, offers
to sell, or sells any patented invention, within the United States or imports" it infringes (§271(a)). Patents are **territorial**, so
status differs by country. The FFmpeg project itself says it does not know whether it uses patented algorithms, that "nobody could
answer with a definite no", and that companies profiting from patented codecs have been pursued `[S1 A]`. Two structural points:
(i) our DDA indemnity (3.4) means a claim against Google over our app comes back to us; (ii) **LGPL-2.1 §11** says that if a patent
licence "would not permit royalty-free redistribution of the Library by all those who receive copies … the only way you could satisfy
both it and this License would be to refrain entirely from distribution". A per-unit paid licence therefore **cannot be combined with
a redistributed LGPL FFmpeg**. Buying a pool licence is not an option for the bundle; the choice is "free and clear" or "not bundled".

**4.2 Codec by codec**

| Codec | Platform guarantee | Patent / licence status | Grade | Decision |
|---|---|---|---|---|
| **MP3** | CDD requires it | Fraunhofer IIS: the licensing programme ended 2017-04-23 "due to the fact that the last patent … expired" `[S16]` | A | not bundled (platform) |
| **FLAC, Vorbis** | CDD requires | open formats | B | not bundled (platform) |
| **Opus** | CDD requires | royalty-free grants from Xiph, Broadcom, Microsoft with defensive termination; Qualcomm, Huawei, France Telecom, Ericsson filed potentially royalty-bearing disclosures; the authors' counsel says no licence is needed for those `[S17]` | A (their position, not a guarantee) | not bundled (platform) |
| **ALAC** | **none** | Apple's reference code is Apache-2.0 (repo archived 2026-08-27) `[S20 A]`. Apache-2.0 §3 grants a patent licence for the contributor's own contribution `[S21 A]`; FFmpeg's decoder is an independent implementation, so the grant does not strictly cover it `[B]`. No assertion by Apple known `[U]` | B | **bundle** |
| **G.711 A-law / µ-law** | not in the CDD tables read | ITU-T 1972; any patent would have lapsed by age `[B, inferred from the date]` | B | **bundle** |
| **AC-3** | none | Dolby's 10-K (FY2011): remaining Dolby Digital patents "expire between now and 2017" `[S7 A]`; the last two named as US 6,449,368 (2017-03-14) and US 5,890,106 (2017-03-19) `[S12 C]`. **But** the FY2019/FY2021 10-Ks say "some of our relevant patents have expired, but others continue to apply" to DD "as it has been updated over time" `[S8 A]`, undefined | A/B | **first add-back**, conditions in D3 |
| **E-AC-3** | none | US 7,516,064 (Dolby, priority 2004-02-19), adjusted expiry **2026-01-30**, status "Expired - Lifetime"; EP/CN/JP/KR/CA counterparts expired; **BR and IL show only "IP Right Grant"** `[S10 A]`. Community calls it the last E-AC-3 patent `[S13 C]`. The standard (ETSI TS 102 366) was first published Feb 2005 `[S30 B]`, so any patent *essential* to it needed a priority before then and has run out by about 2025-26 `[B, inference]` | A for the record, B for "last" | **first add-back**, conditions in D3 |
| **DTS core / DTS-HD (`dca`)** | none | core patents reported expired (US 5,956,674 etc.; last EU 2025-03-21) `[S14 C]`. **US 7,548,853 (DTS, priority 2006-06-12) is active to 2027-02-06**; its first independent claim is a method of compressing audio (encoder side), and whether it reads on a decoder is **not established** `[S11 A / relevance U]`. Xperi publishes hundreds of patents that "may be applicable" with no expiry data `[S15 A]`; FFmpeg's `dca` also decodes the DTS-HD extensions | A/C | **exclude**; re-check 2027-02-07 |
| **TrueHD / MLP** | none | no primary expiry data found; Dolby-branded; Wikipedia gave nothing `[U]` | U | **exclude** |
| **AAC (LC/HE, LATM)** | CDD requires AAC | the Via LA pool is active and covers decoders; **Android OEMs must take their own licence** `[S18 A]`; Dolby (a licensor) states some AAC patents "have expired and others will expire over the next several years", Extended HE-AAC later `[S9 A]`; the ISO code notice warns open-source implementations "may require patent licenses" `[S18]` | A | **exclude** (platform) |

**4.3 What this does not show.** We cannot enumerate every patent on any codec (FFmpeg's own warning). We searched for enforcement
against free or open-source app distributors and found none, which is weak evidence only: absence of search results is not safety `[U]`.
Fedora, a distribution with legal review, still routes an E-AC-3 request through Fedora Legal and shows no recorded approval `[S29 A/B]`.

## 5. Platform codecs are the safe base (supports briefs L2/L3)

Android's Compatibility Definition (versions 8.0 to 17, §5.1.3) requires FLAC, MP3, Vorbis and Opus decoding `[S19 A]`; AAC is
required too (the AAC ELD row was read directly; the AAC-LC/HE rows were not individually extracted `[B]`). Device makers hold the codec
licences (Via LA says plainly that Google does not pay AAC fees on their behalf). An app that hands a stream to `MediaCodec` is not the
party that makes or sells the decoder. This is **common practice** and is not by itself authority; we found no case law either way `[U]`.
Pass-through of DTS/TrueHD to a receiver is likewise left to the device and route, and was **not tested** here.

## 6. Trademarks and wording (brief L4, part of L3)

`[S26 A]` *New Kids on the Block v. News America*, 971 F.2d 302 (9th Cir. 1992), and the Ninth Circuit's model instruction 15.26:
using another's mark to *identify* their product is permitted if (1) the product is not readily identifiable without the mark, (2) only
as much of the mark is used as is reasonably necessary, and (3) nothing suggests sponsorship or endorsement. Applied here: naming a
codec factually in track info, diagnostics or an error ("this track is DTS") is the paradigm case; **do not** use logos, badges or the
words "Dolby Digital"/"DTS" as a selling feature, never write "certified", "licensed" or "powered by", and do not claim support we do not
provide. "FFmpeg is a trademark of Fabrice Bellard" and must be spelled correctly `[S1 A]`. Dolby's own published rules for third
parties were not located (the page returned 404) `[U]`; non-US trademark law (for example EU) was not researched `[U]`.

## 7. Platform policy and regulation

Beyond the DDA terms in 3.4, nothing regulatory was found that is specific to bundling a decoder. Export controls on encryption do not
arise (no cryptography is shipped, not researched in depth `[U]`). Privacy statutes (L5), the app name (L6), the app licence and
F-Droid (L7) and store-claims policy (L8) are separate pieces of work.

## 8. Decisions and actions

| # | Decision | Basis |
|---|---|---|
| **D1** | Route (a), the self-built LGPL-only extension, stays the plan. | DEC-014; 3.1–3.3 |
| **D2** | The 1.0 bundle contains **only `alac`, `pcm_alaw`, `pcm_mulaw`** (`tools/ffmpeg/approved_decoders.txt`). | §5: the platform guarantees the rest; fewer decoders means less patent, security and maintenance surface |
| **D3** | **AC-3 and E-AC-3 are the first add-back** (`ac3`, `eac3`), added only when all hold: C1 the self-built extension exists and passes `verify_lgpl_symbols.py`; C2 the ETSI IPR declarations for TS 102 366 (ETSI SR 000 314) are checked and every declared patent family is confirmed expired, including the BR and IL members of US 7,516,064; C3 the check is written into this file with the date. | 4.2 |
| **D4** | **DTS, TrueHD, MLP are excluded.** Re-check DTS on or after **2027-02-07** (US 7,548,853 expiry) together with a fresh Xperi list; pass-through only until then. | 4.2 |
| **D5** | **AAC is excluded**; the platform decoders handle it. | 4.2, §5 |
| **D6** | **Withdraw the `v1.0.0` binaries and correct its notes.** *Not yet executed* (blocked, see appendix A). | 3.6, §2 |
| **D7** | Every FFmpeg we ship follows the LGPL package: dynamic libraries; the exact FFmpeg source, our build script and the configure line published beside the binary; the licence text and notice inside the app; an in-app open-source licences screen; no reverse-engineering prohibition. A test compares the licence list with the release dependency list. | 3.1–3.2, 3.6 |
| **D8** | Wording rules in §6 become standing rules for UI and store copy. | §6 |
| **D9** | Re-verify the patent rows at every release gate and on these dates: **2027-02-07** (DTS). | 4.3 |
| **D10** | Build FFmpeg **n9.0.2** (pinned by commit) for the first self-built release; 7.1.5 and 8.0.3 stay as CI canaries. Recorded as DEC-023. | CI run 43605271370: all nine builds passed the guard and compiled Media3's JNI |

## 9. Not established, and what would change the answer

- Patent status in **Brazil and Israel** for the E-AC-3 family, and the ETSI-declared patents for AC-3/E-AC-3 (D3 C2).
- Whether US 7,548,853, or any other DTS patent, reads on a decoder-only distribution.
- Any patent or licence terms for **TrueHD/MLP**.
- Whether Apple would assert anything on ALAC (no sign of it; the grant does not strictly reach an independent implementation).
- **Enforcement behaviour** of Dolby, Xperi and Via LA against free-software distributors.
- Google Play's **end-user** terms; **EU and non-US** trademark and patent law; **German and other European LGPL case law**.
- **Dolby's third-party trademark rules** (page not found).
- A lawyer would change the answer on: the AC-3/E-AC-3 add-back if a family member is unexpired; DTS-HD; whether platform delegation fully
  shelters us; and anything in a jurisdiction we did not research. None of those blocks D1, D2, D4, D5 or D6.
- Not covered at all: briefs L5 (privacy statutes), L6 (name clearance for "Aptura"), L7 (app licence, F-Droid), L8 (store-claims policy).

## 10. Sources (all opened 2026-11-14)

- `[S1]` FFmpeg, License and Legal Considerations: https://ffmpeg.org/legal.html
- `[S2]` GNU LGPL v2.1: https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
- `[S3]` Jellyfin `build.sh` at tag v1.5.0+1: https://raw.githubusercontent.com/jellyfin/jellyfin-androidx-media/v1.5.0%2B1/build.sh
- `[S4]` Jellyfin `.gitmodules`: https://raw.githubusercontent.com/jellyfin/jellyfin-androidx-media/master/.gitmodules
- `[S5]` Jellyfin decoder `build.gradle.kts` (POM licence): https://raw.githubusercontent.com/jellyfin/jellyfin-androidx-media/master/media3-ffmpeg-decoder/build.gradle.kts
- `[S6]` AndroidX Media 1.5.1 `build_ffmpeg.sh`: https://raw.githubusercontent.com/androidx/media/1.5.1/libraries/decoder_ffmpeg/src/main/jni/build_ffmpeg.sh
- `[S7]` Dolby 10-K FY2011: https://www.sec.gov/Archives/edgar/data/1308547/000119312511320293/d206326d10k.htm
- `[S8]` Dolby 10-K FY2019 and FY2021: https://www.sec.gov/Archives/edgar/data/1308547/000162828019014472/a10-kfy19q4.htm · https://www.sec.gov/Archives/edgar/data/1308547/000162828021023630/dlb-20210924.htm
- `[S9]` Dolby 10-K FY2025: https://www.sec.gov/Archives/edgar/data/1308547/000130854725000007/dlb-20250926.htm
- `[S10]` US 7,516,064: https://patents.google.com/patent/US7516064B2/en
- `[S11]` US 7,548,853: https://patents.google.com/patent/US7548853B2/en
- `[S12]` AVS Forum, AC-3 last patents: https://www.avsforum.com/threads/dolbys-last-patent-related-to-ac-3-expired-today-3-20-17.2789001/
- `[S13]` E-AC-3 threads: https://www.phoronix.com/news/Dolby-Digital-Plus-E-AC3-2026 · https://forum.doom9.org/showthread.php?t=184389
- `[S14]` Doom9, DTS expiry: https://forum.doom9.org/showthread.php?t=175103
- `[S15]` Xperi, DTS patents: https://xperi.com/dts-patents/
- `[S16]` Fraunhofer IIS, mp3: https://www.iis.fraunhofer.de/en/ff/amm/consumer-electronics/mp3.html · https://www.audioblog.iis.fraunhofer.com/mp3-software-patents-licenses
- `[S17]` Opus licence: https://opus-codec.org/license/
- `[S18]` Via LA, AAC: https://www.via-la.com/licensing-programs/aac/
- `[S19]` Android CDD §5.1.3: https://source.android.com/docs/compatibility/8.0/android-8.0-cdd · …/16/android-16-cdd · …/17/android-17-cdd
- `[S20]` Apple ALAC: https://github.com/macosforge/alac
- `[S21]` Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0.txt
- `[S22]` 35 U.S.C. §154, §271: https://www.govinfo.gov/link/uscode/35/154 · https://www.govinfo.gov/link/uscode/35/271
- `[S23]` Jacobsen v. Katzer, 535 F.3d 1373 (Fed. Cir. 2008): https://www.cafc.uscourts.gov/opinions-orders/08-1001.pdf
- `[S24]` Artifex Software v. Hancom, No. 3:16-cv-06982 (N.D. Cal.): https://www.govinfo.gov/content/pkg/USCOURTS-cand-3_16-cv-06982/pdf/USCOURTS-cand-3_16-cv-06982-0.pdf
- `[S25]` SFC v. Best Buy / Westinghouse, No. 1:09-cv-10155 (S.D.N.Y.): https://www.courtlistener.com/opinion/2157987/software-freedom-conservancy-inc-v-best-buy-co/
- `[S26]` New Kids on the Block v. News America, 971 F.2d 302 (9th Cir. 1992): https://www.courtlistener.com/opinion/9009089/new-kids-on-the-block-v-news-america-publishing-inc/ · https://www.ce9.uscourts.gov/jury-instructions/civil/chapter-15/15-26-defenses-nominative-fair-use/
- `[S27]` Google Play Developer Distribution Agreement: https://play.google/developer-distribution-agreement.html
- `[S28]` FSF on the VLC iOS removal: https://www.fsf.org/blogs/licensing/vlc-enforcement
- `[S29]` Fedora Discussion on E-AC-3: https://discussion.fedoraproject.org/t/dolby-digital-plus-e-ac3-patents-have-now-expired-time-to-add-it-to-fedora/180329
- `[S30]` ETSI TS 102 366: https://www.etsi.org/deliver/etsi_ts/102300_102399/102366/01.04.01_60/ts_102366v010401p.pdf (first published Feb 2005 per the IETF draft cited in the same search)
- `[S31]` Aptura v1.0.0 release: https://github.com/TejuBoricha/Aptura/releases/tag/v1.0.0

---

## Appendix A: withdrawing the `v1.0.0` binaries (D6), to be run by the owner

Why: §2 and 3.6. The files were downloaded 4 times. This is reversible: the tag and the source stay, and the same files can be rebuilt from the tag.

```
gh release delete-asset v1.0.0 app-arm64-v8a-release.apk --yes
gh release delete-asset v1.0.0 app-armeabi-v7a-release.apk --yes
gh release delete-asset v1.0.0 app-release.aab --yes
gh release delete-asset v1.0.0 app-universal-release.apk --yes
gh release delete-asset v1.0.0 app-x86_64-release.apk --yes
gh release edit v1.0.0 --notes-file <the text below saved to a file>
```

Replacement release notes:

> **Aptura v1.0.0 (pre-release): corrected notes.** This is a pre-release and is not suitable for general use. The original release notes made
> statements that were not accurate, and are replaced by this text.
>
> - The original notes called the bundled audio extension "LGPL-clean via dynamically linked FFmpeg (FLAC, Opus, Vorbis)". In fact it is a
>   prebuilt third-party library: FFmpeg 6.0 linked **statically**, with AAC, AC-3, E-AC-3, DTS, TrueHD and MLP decoders as well. The APKs
>   carry no LGPL notice or licence text.
> - "Official Production Release" and "Verified 4K 60fps and HDR10 hardware decoding" overstated what had been verified. See
>   `docs/audits/CURRENT_AUDIT.md` (SHIP-02, SHIP-03).
>
> A replacement build that fixes the library and the notices is in progress (DEC-014 and DEC-020 to DEC-022 in
> `spec/17_RISKS_ASSUMPTIONS_AND_DECISIONS.md`; research in `spec/11B_LEGAL_RESEARCH_FINDINGS.md`). Until then, please do not
> redistribute the attached files.
