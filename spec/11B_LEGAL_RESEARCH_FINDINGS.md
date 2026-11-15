# 11B — Legal research findings: codecs, FFmpeg and licences (self-conducted)

**Date:** 2026-11-14 · **Scope:** the decoder decision (DEC-014, LEG-02) and what follows from it · **Answers:** briefs L1–L4 of
[11A](11A_LEGAL_BRIEFS.md) as far as primary sources allow; L5–L8 are covered in Appendix C (added 2026-11-15, with what could not be established).

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
   the APKs, and its release notes say the opposite ("LGPL-clean via dynamically linked FFmpeg (FLAC, Opus, Vorbis)"). **Done 2026-11-14:**
   the five binaries were deleted from the release and its notes replaced (appendix A). The tag and the source stay.
4. **The platform already guarantees the common free codecs.** Android's CDD requires FLAC, MP3, Vorbis and Opus decoding on every
   compatible device `[A]`. Bundling them adds legal and security surface and no capability on our minSdk 26.
5. **Decisions** (§8): bundle only **ALAC and G.711 A-law/µ-law** (free, and not guaranteed by the platform); make **AC-3** the first add-back (decided 2026-11-15; **E-AC-3 stays out**: Dolby decoder patents run to about 2030, Appendix C); **exclude DTS, TrueHD/MLP and AAC** and leave them to the platform and
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

**Status 2026-11-14 (DEC-025, appendix B):** the first three gaps are closed in the repository by the swap PR; the release-notes gap was closed
by appendix A. Nothing here has been run on a physical device.

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
| **E-AC-3** | none | US 7,516,064 (Dolby, priority 2004-02-19), adjusted expiry **2026-01-30**, status "Expired - Lifetime"; EP/CN/JP/KR/CA counterparts expired; **BR and IL show only "IP Right Grant"** `[S10 A]`. Community calls it the last E-AC-3 patent `[S13 C]`. The standard (ETSI TS 102 366) was first published Feb 2005 `[S30 B]`, so any patent *essential* to it needed a priority before then and has run out by about 2025-26 `[B, inference]` | A for the record, B for "last" | **held** (decided 2026-11-15): later Dolby decoder patents to about 2030, Appendix C.1 |
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
| **D3** | **AC-3 is the first add-back** (`ac3` only), added when: C1 the self-built extension exists and passes `verify_lgpl_symbols.py` (met); C2 the ETSI IPR declarations for TS 102 366 are checked and every declared family is confirmed expired (met for AC-3 on 2026-11-15: Appendix C.1); C3 the check is written into this file with the date (Appendix C). **E-AC-3 (`eac3`) is held**: later Dolby decoder patents (for example US 8,891,776, in force to about 2030) are not on ETSI's list and cover decoding E-AC-3 features FFmpeg implements. Decided by the owner 2026-11-15. | 4.2, Appendix C |
| **D4** | **DTS, TrueHD, MLP are excluded.** Re-check DTS on or after **2027-02-07** (US 7,548,853 expiry) together with a fresh Xperi list; pass-through only until then. | 4.2 |
| **D5** | **AAC is excluded**; the platform decoders handle it. | 4.2, §5 |
| **D6** | **Withdraw the `v1.0.0` binaries and correct its notes.** **Done 2026-11-14** (appendix A). | 3.6, §2 |
| **D7** | Every FFmpeg we ship follows the LGPL package: dynamic libraries; the exact FFmpeg source, our build script and the configure line published beside the binary; the licence text and notice inside the app; an in-app open-source licences screen; no reverse-engineering prohibition. A test compares the licence list with the release dependency list. | 3.1–3.2, 3.6 |
| **D8** | Wording rules in §6 become standing rules for UI and store copy. | §6 |
| **D9** | Re-verify the patent rows at every release gate and on these dates: **2027-02-07** (DTS). | 4.3 |
| **D10** | Build FFmpeg **n9.0.2** (pinned by commit) for the first self-built release; 7.1.5 and 8.0.3 stay as CI canaries. Recorded as DEC-023. | CI run 43605271370: all nine builds passed the guard and compiled Media3's JNI |

## 9. Not established, and what would change the answer

- Whether FFmpeg's E-AC-3 decoder falls within the claims of the later Dolby decoder patents (Appendix C.1), and any patent outside the US-claims search that found them. (The ETSI-declared AC-3 families are checked; Brazil and Israel no longer decide E-AC-3.)
- Whether US 7,548,853, or any other DTS patent, reads on a decoder-only distribution.
- Any patent or licence terms for **TrueHD/MLP**.
- Whether Apple would assert anything on ALAC (no sign of it; the grant does not strictly reach an independent implementation).
- **Enforcement behaviour** of Dolby, Xperi and Via LA against free-software distributors.
- Google Play's **end-user** terms; **EU and non-US** trademark and patent law; **German and other European LGPL case law**.
- **Dolby's third-party trademark rules** (page not found).
- A lawyer would change the answer on: the AC-3/E-AC-3 add-back if a family member is unexpired; DTS-HD; whether platform delegation fully
  shelters us; and anything in a jurisdiction we did not research. None of those blocks D1, D2, D4, D5 or D6.
- Briefs L5 to L8 are researched in Appendix C; its own limits apply (Brazil's LGPD was not read and its children's digital law only in part, C.8; the EU Accessibility Act is C.6 and India's data law C.7; the name search could not see India after 2026-03-27).

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

## Appendix A: withdrawing the `v1.0.0` binaries (D6), done 2026-11-14

Why: §2 and 3.6. The files had been downloaded 4 times, so copies exist that cannot be recalled. The withdrawal is reversible: the tag and the source stay, and files can be rebuilt from the tag (not byte for byte).

**What happened:** the agent's permission layer first blocked every change to the public release, including a text-only edit. When the owner asked directly for the notes to be changed and the files removed, both went through. The release now has the corrected notes and 0 assets; it is still flagged as a pre-release.

The commands that were run, for the record:

```
gh release delete-asset v1.0.0 app-arm64-v8a-release.apk --yes
gh release delete-asset v1.0.0 app-armeabi-v7a-release.apk --yes
gh release delete-asset v1.0.0 app-release.aab --yes
gh release delete-asset v1.0.0 app-universal-release.apk --yes
gh release delete-asset v1.0.0 app-x86_64-release.apk --yes
gh release edit v1.0.0 --notes-file <the text below saved to a file>   # the first line of the page now reads "Aptura v1.0.0 (pre-release): corrected notes"
```

The replacement release notes (the page carries a close variant that cites DEC-014 and DEC-020 to DEC-024):

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

---

## Appendix B: the swap (LEG-02, DEC-025), done 2026-11-14

The prebuilt decoder is replaced by the self-built bundle (`ffmpeg-audio-n9.0.2-r1`, zip SHA-256 `64c49649c419d7e46dff94038e5e8ddd80cdc790ef23c60d18eefff3dfb6a22d`).
Each obligation of section 3 and decision D7 now has a part of the build and a check that holds it. This is the project's own account of what it
did, not a clearance.

| Obligation | What was done | What fails if it stops being true |
|---|---|---|
| Prominent notice that the library is used (LGPL-2.1 section 6; FFmpeg checklist: mention LGPLv2.1 in the about box) | Settings > Open-source licences opens with the FFmpeg/LGPLv2.1 notice and the trademark line | `LicenseDocumentsTest` (the notice text, the link to the pinned release) |
| A copy of the licence | The LGPL-2.1 text, FFmpeg's licensing overview, Apache-2.0 and the BSD-3-Clause text are packaged into the app from the bundle, `LICENSE`, `NOTICE` and `licenses/` | `LicenseDocumentsTest` (every listed text exists, every packaged text is listed) |
| Exact corresponding source on the same server as the binary | The release holds the zip, the FFmpeg source tarball and the build records; `NOTICE` and the screen link to it | `LicenseDocumentsTest` compares the link with the URL the build downloads |
| Dynamic linking; no `--enable-gpl`, `--enable-nonfree` or `--enable-version3`; configure line recorded | Three shared libraries plus the JNI glue; `config.h` and `BUILD-INFO.txt` per ABI inside the bundle | `verify_lgpl_symbols.py` in CI when the bundle is built; `LgplBuildVerificationTest` on the records the app links |
| Only the approved decoders (D2; AC-3 added by D3 on 2026-11-15) | `alac`, `pcm_alaw`, `pcm_mulaw` in the first bundle (r1), plus `ac3` from r2 | the same two (the pin's `decoders=` and `approved_decoders.txt`), plus `FfmpegAudioPlaybackTest.theBundle_decodesExactlyTheApprovedFormats` on a device |
| No restriction on modification or reverse engineering of Aptura (section 6) | Aptura is public under Apache-2.0; a build links whatever bundle `gradle/ffmpeg-bundle.properties` pins | stated in `NOTICE`; not enforced by a test |
| Java half under its own licence | Five AndroidX Media 1.5.1 classes copied byte for byte; the software video renderer left out (ADR-010) | `VendoredMedia3Test` |

**A finding that was not in the research:** reading the licences of the whole release dependency graph showed that `androidx.datastore:datastore-preferences-external-protobuf`
repackages Protocol Buffers under **BSD-3-Clause**, which asks for its copyright notice and disclaimer to be reproduced with a binary, and that `NOTICE` named only three
of the eleven library families on the classpath. The BSD text is now in the app (`licenses/`), `NOTICE` names every family, and `NoticeCoverageTest` keeps it
current. Grade: **A** for the licence of the artifact (its POM) and for the text (the protobuf repository); that BSD-3-Clause requires reproduction in binary form
is the licence's own wording.

**Behaviour change to be aware of (D2, D4, D5):** a device that cannot decode E-AC-3, DTS or TrueHD itself no longer plays those audio tracks (AC-3 is restored from bundle r2, D3), where the
prebuilt library used to decode them all in software. The player marks such tracks as unsupported and moves to another track when there is one.

**Not established:** playback on any physical device; the Play-delivered split-APK path; relinking with a modified FFmpeg (the mechanism, changing the pin, exists but
was not exercised with a modified library); everything listed in section 9, which this change does not touch.


---

## Appendix C: briefs L5 to L8 and DEC-021 C2, researched 2026-11-14 and 2026-11-15

Same rules as the rest of this file: self-conducted, **not legal advice and not a clearance**, evidence graded A/B/C/U, and what could not be established is listed. Raw notes and the downloaded ETSI file are not in the repository (the file is 149 MB); the figures below can be regenerated from the sources named.

### C.1 DEC-021 C2: the ETSI declarations for TS 102 366 (AC-3, E-AC-3)

**Source `[A]`:** ETSI's open IPR export `https://docbox.etsi.org/IPR/Open/ISLD-export.zip` (information statements and licensing declarations; 4,999,898 rows; file dated 2026-11-14), downloaded by the owner in a browser because the server refused scripted requests. ETSI states it checks none of the declarations.

| Fact | Result |
|---|---|
| Declarations naming TS 102 366 | **One:** `ISLD-200412-001`, Dolby Laboratories Inc., signed 2004-11-08 (before the standard's first publication) |
| Patents and family members listed | 2,927 (96 basis patents, 2,831 family members) in 29 jurisdictions: DE 430, US 335, JP 282, EP 227, AT 207, AU 201, DK 197, CA 192, ES 189, SG 155, WO 137, KR 119, BR 88, CN 21 and a tail |
| Application dates (2,738 rows have one) | 1985-11-14 to 2002-10-03; latest in the US 1999-07-08 (US 6,246,345 B1); latest in EP, DE, CA, AU, BR 2000-04-11 |
| Nominal term end (20 years from filing) | Latest **2022-10-03**; no dated row ends after 2026-11-15 |
| Rows without a date | 189, European national validations of EP families whose dates are 2000 or earlier |

**Reading `[B]`.** For AC-3 itself (the body of TS 102 366), every family Dolby declared to ETSI ended by 2022 at the latest, which agrees with Dolby's FY2011 10-K (remaining Dolby Digital patents expire by 2017, S7). **Two limits.** (1) Only 27 of the 335 US rows carry a patent number; US applications filed before 1995-06-08 get the longer of 20 years from filing or 17 years from grant, so a late-issued old US patent among the other ~308 is theoretically possible and is not shown by this data. (2) **E-AC-3 is not covered by ETSI's list.** The declaration (2004) predates E-AC-3's inclusion in TS 102 366, and no other declaration names the standard.

**Later Dolby decoder patents `[A]` for the records, `[B]` for the reading.** The USPTO's Patent Public Search (2026-11-15), Dolby patents whose *claims* name "enhanced AC-3", "E-AC-3" or "Dolby Digital Plus", found 16 families (23 documents), filed 2010 to 2019. Two bear on a software decoder:
- **US 8,891,776** (and continuation 9,620,132), "Decoding of multichannel audio encoded bit streams using adaptive hybrid transformation", PCT filed **2010-10-28**, so nominally in force until **2030-10-28** or later with term adjustment. Claim 1 is a method of decoding a frame block by block in a single pass, in which, for a channel that used the adaptive hybrid transform (AHT), the first block of the frame yields all the channel's hybrid coefficients and an inverse secondary transform is applied once; dependent claim 2 says the frame "complies with enhanced AC-3 bit stream syntax". That is the way the E-AC-3 standard has to be decoded, and FFmpeg n9.0.2 implements AHT (`channel_uses_aht`, `pre_mantissa`, `idct6` in `eac3dec.c`).
- **US 8,214,223** (and 8,868,433, 9,311,921), "Audio decoder and decoding method using efficient downmixing", PCT filed 2011-02-03, in force to about 2031. Claim 1 requires time-domain downmixing **with cross-fading when the downmix settings change**. FFmpeg n9.0.2's `ac3dec.c`, `ac3dec.h`, `ac3dec_float.c`, `ac3dsp.c` and `eac3dec.c` contain no cross-fading (no hit for "crossfade", "cross-fade" or "fade").
The base E-AC-3 patent US 7,516,064 (adjusted expiry 2026-01-30) is not the end of the story: these later patents are why **E-AC-3 is not treated as free**, and why the Brazilian and Israeli counterparts of US 7,516,064 (D3's C2) stopped being the deciding question. Whether FFmpeg's decoder falls within any claim is a claim-construction judgement for counsel; this appendix does not make it.

**Source check `[A]` (code), exact tarball in the bundle.** In FFmpeg n9.0.2 the AHT and the rest of the E-AC-3 decoder are in `eac3dec.c`, which `ac3dec_float.c` includes only `#if CONFIG_EAC3_DECODER`; the calls into it from `ac3dec.c` are behind `CONFIG_EAC3_DECODER` as well (`else if (CONFIG_EAC3_DECODER)`, `if (CONFIG_EAC3_DECODER && !blk)`), and the Makefile links `eac3_data.o` only for that decoder. A build with `--enable-decoder=ac3` and without `eac3` therefore compiles the E-AC-3 tools out. The built libraries are checked for this again when the new bundle is made (recorded in DEC-026).

**Owner's decision, 2026-11-15:** add **AC-3 only**. E-AC-3, DTS (US 7,548,853 to 2027-02-06) and TrueHD/MLP stay out. DTS is re-checked on or after 2027-02-07. TrueHD/MLP: a USPTO claim-text search for "TrueHD" and "Meridian Lossless Packing" found no patent claiming them (4 unrelated hits), which proves nothing about patents that do not use those words `[U]`.

**Consequence for DEC-021.** C2 is met for AC-3 on the evidence above (still research, still not a clearance); E-AC-3 stays closed because of live decoder patents to about 2030, not because of Brazil or Israel.

### C.2 L6: the name "Aptura"

**Method `[A]` for the data, `[B]` for the reading.** WIPO Global Brand Database (`branddb.wipo.int`), 2026-11-14: an exact-phrase search for "Aptura", phonetic searches limited to Nice classes 9 and 42, and full reading of the nearest records. The earlier choice of the name (spec/08B OD-6) rested on "0 Play Store collisions" only; no register had been searched.

- **Identical word, 14 records.** Live: Direct Supply, Inc. (US 3,854,534 and Canada; classes 35, 37, 39, 42: freight logistics, building construction, warehousing, architectural and interior design); Kulicke and Soffa Industries (US 8,296,095, registered 2026-06-09, and SG, JP, KR; class 7: die-attach and flip-chip bonding machines); ONE CWJ LTD (GB, registered 2026-09-18; class 36: private equity). Ended or expired: DataCard (class 9, 2001), Mine Safety Appliances (class 9, 2003), Philips (classes 9 and 11, 2005), a Japanese filing (2005), Atlantic Fish Co (class 42, ended 2026-07-27). **No live identical mark in class 9, 38 or 41.**
- **One letter away, in software classes.** EPTURA (Eptura, Inc., Atlanta): registered US 7,531,296 (classes 9 and 42, 2024-10-08) and in the EU, UK, Australia, Canada (9, 42, 45), with many EPTURA-formative filings pending; the goods are facility, workplace and asset-management software and SaaS for enterprises. APTURE (1888104 Alberta Ltd., Calgary): pending US 99794537 (filed 2026-04-29; non-final office action 2026-09-04) and Canada 2447254, classes 9 and 42: business-management, AI-assistant and optimisation software and SaaS for project management; and APTURE LIMITED (New Zealand, registered 2025-07-01, class 9): camera and intercom interfaces and "computer software for controlling the operation of audio and video devices". Others: OPTURA (US pending, 9/35/42), Aptora (EU pending, filed 2026-10-08, 9/35/42), AVTURA (GB, 9/42/45).
- **Unregistered use.** A web search found APTURA Group, Inc. (Indianapolis, security systems); its US application APTURA TECHNOLOGIES (97608636, classes 35 and 37) was expressly abandoned on 2022-12-09 (an aggregator site, `[B]`). No app called Aptura was found on Google Play or the App Store by web search `[B/U]`. The owner also searched Google Play by hand on 2026-11-15 and found none (one store, one day, reported to the agent and not independently checked `[B/U]`); Play titles are not unique, so this says nothing about trademarks.
- **The other finalists, same search.** Pellucis: 0 exact records (a phonetic search was not run). Caelis: 10 live or pending records, including a French registration of "Caelis" covering classes 9, 11, 37, 41 and 42: worse than Aptura's position.
- **Reading `[B]`.** No identical live right in the field a free offline video player occupies; the exposure is in **likelihood of confusion with near names** (EPTURA most, APTURE next) if the owner files a trademark or if a rights holder complains to Google, and that turns on a comparison of goods and customers that only a court, an examiner or counsel can make. The goods differ sharply (enterprise facility software; business and AI software; camera control hardware) from a consumer media player. The risk is real but modest.
- **Limits.** The database is incomplete: a US record found elsewhere (97608636) was not returned by the exact-phrase search. Freshness per office: US to 2026-10-28, EU 2026-11-13, CA 2026-11-11, GB 2026-11-12, AU 2026-05-07, **India only to 2026-03-27**. Not searched: common-law use, domains, social handles, logos, the primary USPTO, EUIPO and IP India search tools.
- **Options for the owner.** (a) Keep Aptura and accept the residual risk, filing no trademark yet. (b) Keep it and buy a professional clearance search before the first Play upload, because changing the `applicationId` after publishing means a new listing. (c) Move to another finalist, after a phonetic search on it (Pellucis is the cleanest on the exact search).

### C.3 L5: privacy statutes

Texts `[A]`: GDPR from EUR-Lex (Cellar), the consolidated ePrivacy Directive, 16 CFR 312.2 (eCFR), Cal. Civ. Code 1798.140, Google Play's Data safety page.

- **GDPR applies to "the processing of personal data" (Art. 2(1)) by a controller or processor with an EU establishment or targeting EU data subjects (Art. 3).** A controller is whoever "determines the purposes and means of the processing" (Art. 4(7)). Processing by a person "in the course of a purely personal or household activity" is outside it (Art. 2(2)(c)), but Recital 18 adds: "this Regulation applies to controllers or processors which provide the means for processing personal data for such personal or household activities". The EDPB's Guidelines 07/2020 test control by what an entity "actually exerts influence on" about purposes and means, not by access to data. **Unresolved `[U]`:** whether an app developer who designs on-device processing, and never sees the data, is a controller. The sources neither say yes nor no. The practical cost of being wrong is small: no data leaves the device and a privacy policy already exists.
- **ePrivacy Art. 5(3)** requires consent for "storing of information, or the gaining of access to information already stored, in the terminal equipment" unless "strictly necessary in order for the provider of an information society service explicitly requested by the subscriber or user to provide the service". Its wording is not limited to personal data; the library database and settings Aptura keeps on the device are within the wording, and are what the person asked the app to do. The EDPB's Guidelines 2/2023 (second version 2024-10-07) were not read beyond their summary `[U]`.
- **Google Play `[A]`:** "All developers that have an app published on Google Play must complete the Data safety form", "even developers with apps that do not collect any user data", and a privacy policy link is required; "'Collect' means transmitting data from your app off a user's device", and data "only processed locally" need not be disclosed. Aptura's `docs/PRIVACY.md` is that policy.
- **COPPA `[A]`:** "Collects or collection means the gathering of any personal information from a child by any means"; an "operator" is one who "collects or maintains personal information from or about the users". An app that gathers nothing from anyone has nothing for the rule to act on `[B]`.
- **CCPA/CPRA `[A]`:** a "business" needs annual gross revenue over $25 million, or buys, sells or shares the personal information of 100,000 or more consumers or households, or earns 50 percent of revenue from selling or sharing it. Aptura meets none `[B]`.
- **Not researched:** Brazil's LGPD (only its children's digital law, the ECA Digital, was read: C.8), other US state laws, and any non-English law. (The EU Accessibility Act and India's Digital Personal Data Protection Act and Rules were read afterwards: C.6 and C.7.)

### C.4 L7: the app's licence and F-Droid

Aptura is Apache-2.0 and public. Apache-2.0 allows modification and does not forbid reverse engineering, which is what LGPL-2.1 section 6 needs (section 3.3). **F-Droid `[A]`:** its Inclusion Policy requires FLOSS and building "from publicly accessible source code"; "all binary dependencies including JAR files must originate either from source compilation or Debian repository downloads", and "applications can download prebuilt FLOSS binaries with specific conditions from trusted Maven repositories". The way the app now gets its FFmpeg libraries (a SHA-256-pinned zip from a GitHub release, DEC-024) is none of those, so **an F-Droid build would have to build FFmpeg from the published source** (the scripts in `tools/ffmpeg/` and the source tarball exist for that); F-Droid's build documentation says nothing should be built in the prebuild step, so the mechanism is `[U]`. This does not affect Google Play. Contributor hygiene (a DCO or licence headers before outside contributions) was not researched.

### C.5 L8: store-copy claims

Google Play's Metadata policy `[A]` bars metadata that is "misleading ... or inappropriate", requires the title and description to "accurately describe your app's functionality" and screenshots to "reflect your app's actual experience", and forbids ranking claims ("#1", "App of the year"), "best game ever"-style claims, "data comparison of apps or brands", brand logos without permission and "unattributed or anonymous user testimonials". The project's own discipline (doc 07 section 13, doc 15 S3) already forbids the same families. **Reading `[B]`:** nothing new is needed; the store text must not claim codec support the device decides (the swap makes this sharper: E-AC-3, DTS and TrueHD are the device's, and AC-3 is too unless the phone has no decoder for it, when the bundled audio extension plays it, DEC-021), and the Data safety answer "no data collected" is consistent with Google's own definition only while the build stays free of `INTERNET` and SDKs, which tests already enforce. Whether a lawyer should bless the policy once (the brief's Q1) is a choice; no source found makes it a requirement `[U]`.

### C.6 L5: the EU Accessibility Act (Directive (EU) 2019/882), researched 2026-11-15

Answers brief L5 Q5 of [11A](11A_LEGAL_BRIEFS.md): does the Act reach a free app with no revenue, does the microenterprise carve-out apply, and is an accessibility statement required? Same rules as the rest of this appendix: self-conducted, **not legal advice and not a clearance**.

**Sources.** `[A]` The Directive as published (OJ L 151, 7.6.2019, p. 70), English, from EUR-Lex's Cellar (`publications.europa.eu/resource/celex/32019L0882`); Directive 2006/123/EC (`…/celex/32006L0123`) Art. 4 and recital 34; Directive 2010/13/EU as consolidated 2018-12-18 (`…/celex/02010L0013-20181218`) Art. 1(1); the EU's SPARQL endpoint (`publications.europa.eu/webapi/rdf/sparql`) for the Directive's legal status. `[B]` The Commission's EAA page (`commission.europa.eu/…/european-accessibility-act-eaa_en`), the European Union of the Deaf's transposition toolkit (`eud.eu/eud/toolkits/european-accessibility-act-toolkit-for-transposition/`), and Taylor Wessing's "Access to audiovisual media services under the European Accessibility Act" (2025-07-07, `taylorwessing.com/en/interface/2025/media-hot-topics-2025/access-to-audiovisual-media-services-under-the-european-accessibility-act`).

| Question | What the text says `[A]` | Reading `[B]` |
|---|---|---|
| Is an app a covered **product**? | Art. 2(1): consumer general purpose computer hardware systems and their operating systems, payment and self-service terminals, consumer terminal equipment used for electronic communications or for accessing audiovisual media services, e-readers. "Operating system" (Art. 3(38)) is software that "presents a default interface to the user when no application program is running"; the terminal equipment in 3(7) is "equipment the main purpose of which is to provide access to audiovisual media services". | A video player app is none of these. |
| Is it a covered **service**? | Art. 2(2) lists electronic communications, "services providing access to audiovisual media services" (b), parts of passenger transport, consumer banking, "e-books and dedicated software", and e-commerce. (b) is defined in Art. 3(6) as "services transmitted by electronic communications networks which are used to identify, select, receive information on, and view audiovisual media services", and an audiovisual media service (Art. 3(5), Directive 2010/13/EU Art. 1(1)(a)(i)) is one whose principal purpose is "providing programmes, under the editorial responsibility of a media service provider, to the general public … by means of electronic communications networks", a "programme" being an item "within a schedule or a catalogue established by a media service provider" (Art. 1(1)(b)). E-commerce (Art. 3(30)) means services "with a view to concluding a consumer contract". | A player that opens files already on the device is not transmitted over a network and plays no media service provider's programme; Aptura has no `INTERNET` permission (DEC-012) and sells nothing (DEC-004). The Commission, the Deaf union and the law firm all describe (b) as the websites and apps of TV channels and on-demand platforms, and electronic programme guides. |
| The soft spot | Recital 31: such services "could include websites, online applications, set-top box-based applications, downloadable applications, mobile device-based services including mobile applications and related media players as well as connected television services". | Recitals guide interpretation and are not operative; "related media players" ties the player to an audiovisual media service. Not decided by any source found: a recording of a broadcast on the device, or a `content://` file served by another app that streams a media service `[U]`. |
| Is it a **service** at all? | Art. 3(3) borrows Directive 2006/123/EC Art. 4(1): "any self-employed economic activity, normally provided for remuneration". Its recital 34: remuneration is "consideration for the services", assessed "case by case". | A free app with no ads, no sale and no account has no consideration, so there may be no service and no "service provider" (Art. 3(4)). CJEU case law on indirect payment (for example advertising-funded services) was not read `[U]`. |
| **Microenterprise** | Art. 3(23): fewer than 10 persons and turnover or balance sheet not above EUR 2 million. Art. 4(5): "Microenterprises providing services shall be exempt from complying with the accessibility requirements referred to in paragraph 3 of this Article and any obligations relating to the compliance with those requirements"; paragraph 3 covers Sections III and IV of Annex I. | A sole developer is a microenterprise if they are an enterprise at all, and is outside the Act if they are not. **The exemption is for services only.** For products Art. 4(5) says nothing and Art. 14(4) only excuses microenterprises from writing down the disproportionate-burden assessment; that is why the first row, not this one, carries the weight. |
| An accessibility **statement**? | The Act has no "accessibility statement"; that term belongs to Directive (EU) 2016/2102, which covers public sector bodies (recital 46). The nearest duty is Art. 13(2) with Annex V: a service provider puts "information assessing how the service meets the accessibility requirements" in its general terms and conditions, with a general description of the service in accessible formats. | That is an "obligation relating to the compliance" from which microenterprises are exempt (Art. 4(5)). None is required on this reading. |
| Out of scope, what then? | Recital 90: "For products and services not falling under the scope of this Directive, the accessibility requirements of this Directive are not binding." | Aptura's accessibility work (NFR-A11Y-*, TalkBack in `docs/DEVICE_CHECKLIST.md`) is product policy and stays so. |

**Reading `[B]`.** To be bound, Aptura would have to be a *service* (it probably is not: no remuneration), *within Art. 2(2)(b)* (it is not on the definitions: no network transmission, no media service provider), and run by a provider that is *not a microenterprise* (a sole developer is one). Those conditions are cumulative, so each answer favouring Aptura is a separate reason, and the one real uncertainty (recital 31's "related media players") sits in a condition that two others would still have to fail. As a *product* it is not covered at all. The Act therefore adds no obligation, and no statement is required.

**Re-run this section if** (a) a paid unlock or a donation purchase is added (DEC-004 keeps option B of doc 15 as an unselected hypothesis): that creates remuneration and possibly an e-commerce service, and leaves the microenterprise exemption as the main shelter; (b) the app ever receives or lists media services itself (the `INTERNET` invariant currently rules it out); (c) the operator reaches 10 persons or EUR 2 million; (d) the Commission's review under Art. 33 (due by 2030-06-28) adds products or services.

**Not established `[U]`.**
- **Corrigenda.** The EU's record lists five (`32019L0882R(01)` to `R(05)`, dated 2019-08-13, 2020-01-29, 2022-05-19, **2026-10-19** and **2026-10-29**). The text read is the original Journal text. The corrigenda themselves could not be fetched: EUR-Lex answered scripted requests with a browser challenge (HTTP 202, empty body), no workaround was tried, and no consolidated text was retrievable. They may correct only other language versions, but that is a guess. To close this, open `https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L0882R(04)` and `…R(05)` in a browser.
- **Amendments.** The EU's record shows no act amending the Directive on 2026-11-15 `[A]` (the query returned no rows). Proposals not yet adopted were not searched in the legislative trackers; a general web search found none.
- **National laws.** The Directive binds through each Member State's transposing law. None was read; Germany only through the law firm's summary, which says audiovisual-access services are enforced there under the State Media Treaty by the state media authorities, not under the general accessibility law `[B]`.
- The harmonised standard (EN 301 549) and what conformity to it would take; enforcement practice; accessibility law outside the EU (UK, US and others).

### C.7 L5: India's Digital Personal Data Protection Act 2023 and Rules 2025, researched 2026-11-15

Answers brief L5 Q2 of [11A](11A_LEGAL_BRIEFS.md): for an offline app, which phased duties attach at R1, and does the phasing change the launch posture? Same rules as the rest of this appendix: self-conducted, **not legal advice and not a clearance**. Of the three statutes left open by C.3, this one has a date.

**Sources.** `[A]` all from the Ministry of Electronics and Information Technology (`meity.gov.in`), as published in the Gazette of India, text taken from the PDFs by a scraping service's PDF parser and checked against that extraction: the Act (No. 22 of 2023, `…/static/uploads/2024/06/2bf1f0e9f04e6fb4f8fef35e82c42aa5.pdf`); commencement notification G.S.R. 843(E) of 13 November 2025 (`…/2025/11/c56ceae6c383460ca69577428d36828b.pdf`); the Rules, G.S.R. 846(E) (`…/2025/11/53450e6e5dc0bfa85ebd78686cadad39.pdf`); the corrigendum G.S.R. 892(E) of 10 December 2025 (`…/2025/12/3c7ebbae0e5456f493f486e6845df86b.pdf`); and MeitY's own list of documents for the Rules (`meity.gov.in/documents/act-and-policies/digital-personal-data-protection-rules-2025-gDOxUjMtQWa`), read on 2026-11-15. `[B]` SCC Times on the Removal of Difficulties Order (`scconline.com/blog/post/2026/10/15/dpdp-removal-of-difficulties-order-2026`) and press and law-firm reports of a proposal to shorten the timetable.

**What is in force when `[A]`.** G.S.R. 843(E) brings into force on publication (the Gazette issue is dated 13 November 2025) the definitions (§2), the Board provisions (§§18 to 26), §§35 and 38 to 43, and §44(1) and (3); **one year later (13 November 2026, two days before this research)** only §6(9) and §27(1)(d), the consent-manager provisions; and **eighteen months later (13 May 2027)** §§3 to 5, §6(1) to (8) and (10), §§7 to 17 and the penalty provisions. So the applicability section (§3) and every duty of a data fiduciary start on 2027-05-13. Rules 1, 2 and 17 to 21 are in force, Rule 4 from one year, Rules 3, 5 to 16, 22 and 23 from eighteen months (Rule 1(2) to (4)). Law firms count from 14 November (the e-gazette identifier carries that date); the Gazette issue itself says 13 November, so the dates above could each move by a day.

| Question | What the text says `[A]` | Reading `[B]` |
|---|---|---|
| Who is bound? | §2(i): a "Data Fiduciary" is "any person who alone or in conjunction with other persons determines the purpose and means of processing of personal data". §2(x): "processing" is "a wholly or partly automated operation or set of operations performed on digital personal data", including collection, storage, retrieval and use. §2(t): personal data is "any data about an individual who is identifiable by or in relation to such data". | Whether a developer who designs on-device processing and never receives the data "determines the purpose and means" is **the same open question as GDPR in C.3** `[U]`; no source found answers it. |
| Does the user's own use count? | §3(c)(i): the Act does not apply to "personal data processed by an individual for any personal or domestic purpose". | That shelters the person using the app. It says nothing about the developer; the Act has no counterpart to GDPR recital 18, which names those who "provide the means". |
| Reach | §3(a): processing "within the territory of India"; §3(b): also processing outside India "in connection with any activity related to offering of goods or services to Data Principals within the territory of India". | A worldwide Play listing includes India, so staying out of reach by geography is not a plan. |
| What it presupposes | §8(5): "personal data in its possession or under its control". Rule 7(1): breach notice goes "through her user account or any mode of communication registered by her with the Data Fiduciary". Notice and consent attach to collection (§§5, 6). | Every duty assumes the fiduciary holds data or has a channel to the person. Aptura has no accounts, receives nothing and cannot contact anyone, so there is nothing to perform these duties on. |
| Children | §2(f): a child is "an individual who has not completed the age of eighteen years". §9(1): verifiable consent of the parent before processing; §9(3): no "tracking or behavioural monitoring of children or targeted advertising directed at children". Rule 10(1): "adopt appropriate technical and organisational measures to ensure that verifiable consent of the parent is obtained before the processing of any personal data of a child". The exemptions (Fourth Schedule) are for clinical, educational, crèche and transport settings, email-only accounts and a few safety purposes. | A general-audience player is used by people under 18. Aptura never tracks or advertises. **If** the developer were a fiduciary, this is the sharpest edge: the library and watch history of a minor on the device is arguably personal data, no exemption covers a media player, and no verification mechanism fits an anonymous offline app. Hence the weight on the first row. |
| Retention and logs | Rule 6(1)(e): "retain such logs and personal data for a period of one year"; Rule 8(3): retain personal data and logs "for a minimum period of one year from the date of such processing". The retention periods of Rule 8(1) (Third Schedule) apply to an e-commerce entity, an online gaming intermediary and a social media intermediary with "not less than" two crore, fifty lakh and two crore registered users in India. | If Aptura were a fiduciary, a one-year log duty would collide with its 7-day log ring (doc 10 §4.1): one more sign the Rules were written for entities that hold user data on servers. The Third Schedule classes are not Aptura. |
| Exposure if wrong | The Schedule to the Act: up to "two hundred and fifty crore rupees" for failing to take reasonable security safeguards (§8(5)), "two hundred crore rupees" for failing to give breach notice (§8(6)) or breaching the children's duties (§9), "fifty crore rupees" for any other breach. | Statutory ceilings, not forecasts; the Board's practice is not known. |

**Reading `[B]`.** As built (no network, no accounts, no collection by the developer; DEC-012), the Act gives the developer nothing to do at release and nothing to do on 2027-05-13. The protection is the design, not a document: `docs/PRIVACY.md` is a trust and store-form text, not a DPDP notice (a notice with an itemised list of data and a withdrawal route only means something for a fiduciary). On brief Q2: no phased duty attaches, and the timetable does not change the launch posture. If the controller question went the other way, the first duty date would still be 2027-05-13.

**Changes since the Rules were published.**
- **Corrigendum G.S.R. 892(E)** `[A]`: typographical only. It changes "of this Gazette" to "in the Official Gazette" in the commencement wording of the Rules, "Department" to "Departments", "everybody" to "every body", "(18 or 2013)" to "(18 of 2013)", and "(a) to (f)" to "(a) to (g)" on its page 38. No date and no operative rule changes.
- **Digital Personal Data Protection (Removal of Difficulties) Order, 2026**, dated 5 October 2026, in effect 6 October `[B]`: under §43(1), two word-level edits as SCC Times reports them: §9(1) to read "child or of a person with disability", and §10(2)(c)(ii) "audit" to "data audit". Neither touches the analysis. A commercial blog described it as resolving consent rules for persons with disabilities, which overstates it. The Order's own text was not read.
- **A proposal to shorten the eighteen months** to twelve, aimed at large companies and significant data fiduciaries, was reported in January 2026. MeitY's own document list shows no amending notification, and guides dated August to October 2026 still give 2027-05-13. Not found adopted `[B/U]`: **check the Gazette before relying on the date.**

**Re-run this section if** the app gains a server, analytics, a crash SDK, accounts, sync or backup to a service; an advertisement; any feature aimed at children; or a way for the developer to receive user data. A small case sits just inside that line: a person who emails a diagnostics bundle to the developer hands over data, and the developer then holds it `[B]`.

**Not established `[U]`.** The controller question above. How the Data Protection Board will act (it exists, but no decision was looked for). The text of the 2026 Order itself, and whether the shortening proposal was ever adopted. The Rules' schedules other than the Third and Fourth (consent managers, the State's standards, Board terms, the Seventh Schedule's purposes) were not read. The interplay with the Information Technology Act. The duty to offer a notice in English or an Eighth Schedule language, which binds only a fiduciary. State laws, case law (none found), and Indian law outside data protection.

### C.8 L5: Brazil, the children's digital law (ECA Digital, Lei 15.211/2025) and the LGPD, researched 2026-11-15 (partial)

Answers brief L5 Q3 of [11A](11A_LEGAL_BRIEFS.md) only in part. Same rules as the rest of this appendix: self-conducted, **not legal advice and not a clearance**. **The LGPD itself (Lei 13.709/2018) was not read**, and Q3 (is a Portuguese privacy text needed at launch) is unanswered. The ECA Digital was read, and it is new to this register: nothing earlier in spec/ mentions it.

**Why the LGPD is missing `[U]`.** Every official route failed on 2026-11-15: Planalto's addresses (`planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm` and the consolidated `l13709compilado.htm`) answer the scraping tool with the site's home page; ANPD's English PDF (`gov.br/anpd/…/lgpd-en-lei-no-13-709-capa.pdf`) returns HTTP 401, "restricted content"; the Chamber of Deputies' copy timed out (HTTP 504). No workaround was tried and nothing was filled in from memory. What to read once a copy is in hand: Arts. 3 and 4(I) (reach, and the exemption for a person's private non-economic use), Art. 5 (definitions, including controller), Art. 14 (children), Art. 52 (penalties), and the ANPD regulation for small-scale processing agents. The Consumer Code's duty to give product information in Portuguese (Lei 8.078/1990 Art. 31) and the Marco Civil da Internet are the likelier source of any Portuguese-text requirement than the LGPD, and were not read either.

**Sources.** `[A]` Lei 15.211 of 17 September 2025, from the Imprensa Nacional's Diário Oficial page (`in.gov.br/en/web/dou/-/lei-n-15.211-de-17-de-setembro-de-2025-656579619`), text taken by a scraping service and checked against that extraction (the page does not include the law's last article, so its date of effect is not in the text read). `[B]` Agência Senado, "ECA Digital … entra em vigor" (`www12.senado.leg.br/noticias/materias/2026/03/17/eca-digital-para-protecao-on-line-de-criancas-e-adolescentes-entra-em-vigor`, 2026-03-17), which reports the law took effect in March 2026 after six months to adapt. Quotations are in Portuguese; the English around them is mine.

| Question | What the text says `[A]` | Reading `[B]` |
|---|---|---|
| Reach | Art. 1: the law "aplica-se a todo produto ou serviço de tecnologia da informação direcionado a crianças e a adolescentes no País ou de acesso provável por eles, independentemente de sua localização, desenvolvimento, fabricação, oferta, comercialização e operação". | A developer abroad is not outside it, and a product need not be aimed at children. Everything turns on "acesso provável". |
| What is a covered product | Art. 2(I): "produto ou serviço fornecido a distância, por meio eletrônico e provido em virtude de requisição individual, tais como aplicações de internet, programas de computador" (and other software, operating systems, app stores, connected games). | An app downloaded from a store is plausibly supplied "a distância, por meio eletrônico" on individual request. Do not assume that being offline takes it out. |
| "Likely access" | Art. 1, single paragraph, three conditions joined by "e": "suficiente probabilidade de uso e atratividade … por crianças e adolescentes"; "considerável facilidade ao acesso e utilização … por crianças e adolescentes"; and "significativo grau de risco à privacidade, à segurança ou ao desenvolvimento biopsicossocial de crianças e de adolescentes", especially for products "que tenham por finalidade permitir a interação social e o compartilhamento de informações em larga escala". | A general-purpose player plausibly meets the first two. The third is not met as built: no social interaction, no sharing, nothing leaves the device (DEC-012). **So Aptura is outside the law on the text, but only if the three conditions are cumulative (the "e" says so) and the authority reads "significativo grau de risco" as written.** The regulation (Art. 37) and the authority's guidance (Arts. 5(3), 34) were not read `[U]`. |
| If it were covered | Art. 39: the main duties (Arts. 6, 17, 18, 19, 20, 27, 28, 29, 31, 32, 40) apply "moduladas de acordo com o grau de interferência do fornecedor do produto ou serviço sobre os conteúdos veiculados disponibilizados, o número de usuários e o porte do fornecedor". Art. 40: suppliers "deverão manter representante legal no País". Art. 14: suppliers of covered products "deverão adotar medidas técnicas e organizacionais para garantir o recebimento das informações de idade" that the operating system and store must provide. Art. 35(II): a fine of "até 10% (dez por cento) do faturamento do grupo econômico no Brasil", "limitada, no total, a R$ 50.000.000,00 (cinquenta milhões de reais) por infração". | The duties scale down with size and with how little the supplier controls content, which favours a small offline player; the legal-representative duty (Art. 40) would still be an odd fit for a sole developer abroad. The fine is a ceiling, not a forecast. |
| Who carries age assurance | Art. 12: "Os provedores de lojas de aplicações de internet e de sistemas operacionais de terminais deverão" assess age, offer parental supervision, and provide an age signal through an API "aos provedores de aplicações de internet" ("fornecimento de sinal de idade aos provedores de aplicações de internet"). | The heavy part falls on the store and the operating system (Google, for Play and Android), not on the app. How Play will present that signal to a developer was not looked up `[U]`. |
| Adult content | Art. 9: a supplier that offers content or a service "cuja oferta ou acesso seja impróprio, inadequado ou proibido para menores de 18 (dezoito) anos de idade" must take effective measures to keep minors out. | Aptura offers no content; it plays the person's own files. Whether a player that can open any local file is caught is not answered by anything found `[U]`. |

**Reading `[B]`.** The ECA Digital is the nearest Brazilian rule to a real risk for a general video player, because it reaches products "likely" to be used by minors and does not depend on collecting data. The design (no network, no accounts, no sharing) is what keeps Aptura on the right side of the third condition. This is a thinner shelter than the one that protects against the privacy statutes, because it rests on how an authority that has barely begun to act will read one phrase.

**Re-run this section if** the app gains any social, sharing or online feature, sends any data off the device, shows advertising, is aimed at children, or is entered in a children's or family programme; or when the Art. 37 regulation or the authority's guidance appears. Answer the Play Console's age-rating and audience questions truthfully and consistently with this reading.

**Not established `[U]`.** The LGPD and Q3 (above). The law's date of effect from its own text. The regulation and the authority's guidance. Whether the authority treats the three conditions as cumulative in practice. Art. 9 for a player that opens any file. How Play delivers the Art. 12 age signal. The Consumer Code and the Marco Civil. Everything outside Brazilian federal law.

### C.9 What this appendix changes

D3 is narrowed from "AC-3 and E-AC-3" to **AC-3 only** (C.1); its condition C2 is met for AC-3. Nothing in D1, D2, D4 or D5 changes. C.6 adds no obligation under the EU Accessibility Act, and C.7 finds no duty under India's data protection law as built. C.8 finds that Brazil's children's digital law (ECA Digital) is the nearest of the three to a real risk, because it reaches products "likely" to be used by minors; on its text Aptura is outside it as built, on a thinner shelter than the privacy statutes give, and the LGPD itself is still unread. New open items: the name decision (C.2), the controller question, now for GDPR and for India (C.3, C.7), the F-Droid path (C.4), the E-AC-3 patents to about 2030 (C.1), the two recent EAA corrigenda (C.6), the date check for India's timetable (C.7), and the LGPD text, brief Q3 and the ECA Digital's regulation (C.8).
