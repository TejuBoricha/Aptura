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
- Briefs L5 to L8 are researched in Appendix C; its own limits apply (India's data law, Brazil's LGPD and the EU Accessibility Act were not read; the name search could not see India after 2026-03-27).

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
| Only the approved decoders (D2) | `alac`, `pcm_alaw`, `pcm_mulaw` | the same two, plus `FfmpegAudioPlaybackTest.theBundle_decodesExactlyTheApprovedFormats` on a device |
| No restriction on modification or reverse engineering of Aptura (section 6) | Aptura is public under Apache-2.0; a build links whatever bundle `gradle/ffmpeg-bundle.properties` pins | stated in `NOTICE`; not enforced by a test |
| Java half under its own licence | Five AndroidX Media 1.5.1 classes copied byte for byte; the software video renderer left out (ADR-010) | `VendoredMedia3Test` |

**A finding that was not in the research:** reading the licences of the whole release dependency graph showed that `androidx.datastore:datastore-preferences-external-protobuf`
repackages Protocol Buffers under **BSD-3-Clause**, which asks for its copyright notice and disclaimer to be reproduced with a binary, and that `NOTICE` named only three
of the eleven library families on the classpath. The BSD text is now in the app (`licenses/`), `NOTICE` names every family, and `NoticeCoverageTest` keeps it
current. Grade: **A** for the licence of the artifact (its POM) and for the text (the protobuf repository); that BSD-3-Clause requires reproduction in binary form
is the licence's own wording.

**Behaviour change to be aware of (D2, D4, D5):** a device that cannot decode AC-3, E-AC-3, DTS or TrueHD itself no longer plays those audio tracks, where the
prebuilt library used to decode them in software. The player marks such tracks as unsupported and moves to another track when there is one.

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
- **Unregistered use.** A web search found APTURA Group, Inc. (Indianapolis, security systems); its US application APTURA TECHNOLOGIES (97608636, classes 35 and 37) was expressly abandoned on 2022-12-09 (an aggregator site, `[B]`). No app called Aptura was found on Google Play or the App Store by web search `[B/U]`.
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
- **Not researched:** India's Digital Personal Data Protection Act 2023 and Rules, Brazil's LGPD, the EU Accessibility Act, other US state laws, and any non-English law.

### C.4 L7: the app's licence and F-Droid

Aptura is Apache-2.0 and public. Apache-2.0 allows modification and does not forbid reverse engineering, which is what LGPL-2.1 section 6 needs (section 3.3). **F-Droid `[A]`:** its Inclusion Policy requires FLOSS and building "from publicly accessible source code"; "all binary dependencies including JAR files must originate either from source compilation or Debian repository downloads", and "applications can download prebuilt FLOSS binaries with specific conditions from trusted Maven repositories". The way the app now gets its FFmpeg libraries (a SHA-256-pinned zip from a GitHub release, DEC-024) is none of those, so **an F-Droid build would have to build FFmpeg from the published source** (the scripts in `tools/ffmpeg/` and the source tarball exist for that); F-Droid's build documentation says nothing should be built in the prebuild step, so the mechanism is `[U]`. This does not affect Google Play. Contributor hygiene (a DCO or licence headers before outside contributions) was not researched.

### C.5 L8: store-copy claims

Google Play's Metadata policy `[A]` bars metadata that is "misleading ... or inappropriate", requires the title and description to "accurately describe your app's functionality" and screenshots to "reflect your app's actual experience", and forbids ranking claims ("#1", "App of the year"), "best game ever"-style claims, "data comparison of apps or brands", brand logos without permission and "unattributed or anonymous user testimonials". The project's own discipline (doc 07 section 13, doc 15 S3) already forbids the same families. **Reading `[B]`:** nothing new is needed; the store text must not claim codec support the device decides (the swap makes this sharper: AC-3, E-AC-3, DTS and TrueHD are the device's), and the Data safety answer "no data collected" is consistent with Google's own definition only while the build stays free of `INTERNET` and SDKs, which tests already enforce. Whether a lawyer should bless the policy once (the brief's Q1) is a choice; no source found makes it a requirement `[U]`.

### C.6 What this appendix changes

D3 is narrowed from "AC-3 and E-AC-3" to **AC-3 only** (C.1); its condition C2 is met for AC-3. Nothing in D1, D2, D4 or D5 changes. New open items: the name decision (C.2), the controller question (C.3), the F-Droid path (C.4), and the E-AC-3 patents to about 2030 (C.1).
