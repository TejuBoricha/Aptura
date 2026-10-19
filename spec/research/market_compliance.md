# Market, Monetization & Compliance — Research Notes

- Input to: `spec/01_PRODUCT_VISION_AND_SCOPE.md` (privacy-first, local-file video player; iOS + Android; no accounts; plays user-owned files).
- Compiled: 2026-10-18. All sources accessed 2026-10-18 unless stated otherwise.
- Sibling doc: `spec/research/competitors.md` (not merged here; this file adds market/store stats, monetization benchmarks, compliance and distribution economics).
- Label legend: `[V1]` official primary source (store listing, platform policy, statute, vendor doc) · `[V2]` verified secondary (report, law-firm tracker) · `[R]` reviewer/user sentiment (anecdotal) · `[I]` inference (reasoning shown) · `[E]` estimate (assumptions stated) · `[U]` unknown / not verified this pass.
- Caveats: Play install counts are buckets ("500M+"), ratings are self-selected and point-in-time; store pricing pages shift — re-verify at spec time. Several searches returned no results and some Apple help URLs 404'd on 2026-10-18; those gaps are marked `[U]` and listed in Open questions.

Method & source quality:
- Store statistics were read directly from the Play/App Store listings (not third-party trackers); platform obligations from official policy/help pages; legal baselines from the statutes themselves (EUR-Lex, FTC, OAG, Planalto) plus one law-firm tracker (DLA Piper) used only for the DPDP Rules timeline, cross-checked against PRS for the Act's legislative facts.
- Monetization benchmarks come from a single large secondary source (RevenueCat, 75k apps / $10B+ tracked revenue) [V2]; public price points of comparable apps [V1] anchor them to this niche.
- Nothing was inferred from memory where a source was needed: unreachable pages are marked [U] and enumerated at the end of this file. `[R]` items are individual user comments and must not be generalized.

## 1. Market context — store snapshots (Android + iOS)

### Android (Google Play, accessed 2026-10-18)

| App | Publisher / model | Store signals | Label |
|---|---|---|---|
| VLC for Android (`org.videolan.vlc`) | VideoLAN; free, no ads, no IAP per listing | 4.4★; 2.01M reviews; 500M+ downloads | [V1: play.google.com/…?id=org.videolan.vlc] |
| Amazon MX Player (`com.mxtech.videoplayer.ad`) | Amazon Mobile LLC; "Contains ads", in-app purchases; now OTT-first positioning (MX Originals, "8K/4K" playback claims, MX Fatafat micro-drama) | 4.3★; 14.3M reviews; 1B+ downloads | [V1: play.google.com/…?id=com.mxtech.videoplayer.ad] |
| Just (Video) Player (`com.brouken.player`) | Marcel Dopita (indie); free, donation-funded (PayPal/BTC/LTC links in README); ExoPlayer/Media3 + ffmpeg extension; self-described "No ads, tracking or excessive permissions"; also on F-Droid, Accrescent, Obtainium, GitHub, Galaxy Store, AppGallery, Mi, Aptoide, Amazon; Unlicense; ~2.7k GitHub stars | 3.8★; 3.42K reviews; 1M+ downloads | [V1: play.google.com/…?id=com.brouken.player; github.com/moneytoo/Player] |
| MX Player Pro (`com.mxtech.videoplayer.pro`) | — | Play URL returned 404 on 2026-10-18; possible delisting — unconfirmed, do not cite as available | [U] |

- Scale read: the free/no-frills Android player niche can reach 1M+ downloads as an indie single-dev app (Just Player) while the brand leader sits at 1B+ (MX) and the FOSS reference at 500M+ (VLC) [V1 listings]. These are install floors, not active users [I].
- MX Player's Android listing is now dominated by streaming/OTT features (ads + in-app purchases), i.e., the biggest "player" brand monetizes as an ad-supported media service, not via the local-file tool [V1 listing; see §2].

### iOS (App Store US, accessed 2026-10-18)

| App | Publisher / price | Store signals | Label |
|---|---|---|---|
| Infuse (`id1136220934`) | Firecore LLC; free with IAP; Pro $1.99/mo · $16.99/yr · $99.99 lifetime; 7-day trial; "updates for life" with Pro | 4.7★; 28K ratings; 4+; 207.1 MB; iOS 18+ required | [V1: apps.apple.com/…/id1136220934; firecore.com/infuse] |
| nPlayer (`id1116905928`) | Newin Inc.; paid upfront $4.99 | 4.7★; 1.9K ratings; 16+; #14 Entertainment (US); 64 MB | [V1: apps.apple.com/…/id1116905928] |
| VLC for iOS (`id650377962`) | VideoLAN; Free | 3.7★; 6K ratings; #32 Photo & Video (US) | [V1: apps.apple.com/…/id650377962] |
| Outplayer (`id1449923287`) | Stephen Salerno; free with one-time purchases ("Outplayer Advanced"); free tier has "no playback restrictions and no ads" | 4.6★; 507 ratings; 4+; 41.3 MB; iOS 17+; App Privacy: "Data Not Collected" | [V1: apps.apple.com/…/id1449923287; outplayer.app] |
| Infuse 8 (`id1564091373`) | — | URL returned 404; canonical listing is `id1136220934` | [U] |

- Rating asymmetry on iOS: paid/one-time apps cluster at 4.6–4.7★ (Infuse, nPlayer, Outplayer) while the major free app (VLC iOS) sits at 3.7★ [V1 listings]. Consistent with — but not proof of — an iOS willingness to pay for polish; no review-text analysis performed on VLC iOS this pass [I]/[U].
- Engine/positioning notes for Infuse: marketing centers "formats other players choke on — MKV, MP4, ISO, VIDEO_TS, BDMV — without transcoding," Dolby Vision (P5/P8), HDR10/HDR10+, HLG, Atmos (E-AC3) passthrough; SMB/NFS/FTP/WebDAV, Plex/Emby/Jellyfin, major cloud drives; "No ads, no tracking, and no harvesting of library info…ever!" [V1: firecore.com/infuse].
- Android format-limit reference (contrast to Infuse's "plays it all"): Just Player README documents upstream ExoPlayer limits — WMV and Theora not handled, historically AVI issues, 10-bit AVC device-limited [V1: github.com/moneytoo/Player]. iOS-specific "format complaint" themes were not systematically captured — [U].

## 2. Regional dynamics, local-vs-streaming, market size

- MX Player is now "Amazon MX Player" and is being folded into Prime Video: support copy states the service brings "free and paid streaming under a single destination," with free access to MX Originals/Vdesi/Fatafat or a Prime subscription for more [V1: support.mxplayer.in; mxplayer.in]. The flagship "player" brand has fully pivoted to AVOD/SVOD economics [I].
- India is a primary market for that OTT strategy (Hindi/Tamil/Telugu/Bhojpuri catalog, regional marketing copy) [V1: mxplayer.in]. For our product, India is therefore a candidate launch region driven by download volume, but it is also the most price-sensitive tier for any paid feature [I].
- Regional subscription shape (RevenueCat 2025 benchmark): IN/SEA relies on weekly plans (36% of subscriptions) more than other regions; MEA 35%; yearly plans dominate APAC overall (47%) and North America (43%) [V2: revenuecat.com/state-of-subscription-apps-2025]. If we ever add subscriptions, India-price ladders skew low/weekly [I].
- Local vs streaming: the local-file niche is where no major commercial vendor is now investing (MX left it for OTT; Infuse monetizes LAN/library power users; VLC/Just Player serve it as FOSS) [V1 listings + firecore.com/infuse + github.com/moneytoo/Player]. This is the strategic gap our spec assumes [I].
- Market size: no credible public sizing for "local-file video player" was located this pass (multiple web searches returned no results; no analyst report captured) [U]. Usable proxies only: VLC for Android 500M+ downloads and Amazon MX Player 1B+ downloads on Play [V1]. A bottom-up TAM (archived-media households × eligible devices) is deferred to the spec [I].

## 3. iOS landscape deep-dive

- Monetization anchor: Infuse's ladder — $1.99/mo, $16.99/yr, $99.99 lifetime, 7-day trial — is the island's premium reference point; subscription unlocks major-version updates ("v9, v10, and beyond") [V1: firecore.com/infuse].
- One-time still works: nPlayer has sustained a $4.99 upfront price with 1.9K ratings at 4.7★ [V1]; Outplayer runs free + a single "Advanced" unlock, with the free tier unrestricted and ad-free [V1: apps.apple.com/…/id1449923287; outplayer.app]. This validates a no-ads, no-subscription model as a viable iOS position [I].
- Privacy is a marketing surface on iOS: Outplayer's App Store label reads "Data Not Collected" [V1 listing]; Firecore markets "no ads, no tracking, no harvesting" [V1: firecore.com/infuse]. Our design (no accounts, no telemetry by default) is consistent with both [I].
- Format/feature bar set by Infuse: ISO/BDMV/VIDEO_TS, Dolby Vision P5/P8, Atmos passthrough, SMB/NFS/WebDAV, Plex/Emby/Jellyfin, cloud drives [V1: firecore.com/infuse]. Competing on format coverage alone means matching a 15-year engine — differentiation should be privacy + simplicity instead [I].
- iOS "paid-app culture": best proxy in hand is the paid/one-time cohort's high ratings vs free VLC's lower rating [V1 listings]; formal willingness-to-pay research not captured [U].

## 4. Monetization benchmarks & mechanics

### Observed models in this niche

- Paid upfront: nPlayer, $4.99 [V1 App Store].
- Freemium + one-time unlock: Outplayer ("Outplayer Advanced", free tier unrestricted/no ads) [V1 App Store + site].
- Subscription + lifetime: Infuse Pro ($1.99/mo · $16.99/yr · $99.99 lifetime) [V1 firecore.com/infuse].
- Ads + IAP (service pivot): Amazon MX Player ("Contains ads", IAP; free AVOD + Prime bundling) [V1 Play listing + support.mxplayer.in].
- Donations (FOSS): Just Player (PayPal/BTC/LTC) [V1 github.com/moneytoo/Player]; VLC listing claims free/no ads/no IAP [V1 Play listing].

### Conversion & revenue benchmarks (RevenueCat State of Subscription Apps 2025; 75,000 apps, $10B+ tracked revenue) [V2]

- Download → trial: median 6.2%; p90 20.3%; 82% of trial starts occur the day of install (other quotes: 80–90% Day 0).
- Trial → paid: Media & Entertainment median 43.8% (Travel highest at 48.7%); trials of 17–32 days show the best median conversion (45.7%).
- Download → paid at Day 35: freemium median 2.18% vs hard-paywall median 12.11%; high-price apps 2.7% vs low-price 1.5% median.
- Retention: ~30% of annual subscriptions are canceled in the first month; 1-year retention ~44.1% for annual plans vs under ~10% for weekly.
- Revenue per install (D60): overall median $0.31; Photo & Video's newly launched apps have the highest 2-year success rate (27.57% reach $1,000; 8.75% reach $10,000); median days to first $1,000 is 60 across categories.
- Hybrid mix is common: 35% of apps combine subscriptions with consumables or lifetime purchases.
- Median consumer subscription app price: $29.99 (annual framing).

### Our-app estimate (explicit assumptions — treat as placeholder, validate internally)

- [E] Assumptions: freemium local-file player; one-time "Pro" unlock $4.99–$9.99 (Outplayer-style); year-1 downloads 100K–1M (Just Player's 1M+ across stores as upper anchor [V1]); year-1 payer conversion 0.5–2.0% of installs (bracketed by RevenueCat freemium Day-35 median 2.18% [V2], discounted for a utility category with one-time pricing [I]).
- [E] Resulting gross revenue ≈ $0.03–$0.20 per install; after 15–30% store commission ≈ $0.02–$0.17 net per install. Low confidence; top-decile Photo & Video apps ($10K/2yr per RevenueCat [V2]) imply meaningful upside only with better funnel execution.

### Billing rules, refunds, pricing tools

- Google Play: digital content/features must be sold with Google Play's billing system [V1: support.google.com/googleplay/android-developer/answer/112622]. Play Billing Library v8+ is required for new apps/updates by 2026-08-31, with an extension available to 2026-11-01; v9 migration is the forward path [V1: developer.android.com/google/play/billing/deprecation-faq].
- Apple: digital goods must use in-app purchase (App Review Guideline 3.1.1) [V1: developer.apple.com/app-store/review/guidelines].
- Refunds: Play lets users request refunds and developers can issue them in Play Console per policy windows [V1: support.google.com/googleplay/answer/2479637]; Apple handles refund requests centrally (developers cannot approve individual requests) [V1: support.apple.com/en-us/118223]. Chargeback/refund-abuse exposure is mostly platform-mediated (revenue reversal + possible fee implications); no public chargeback-rate data captured [I]/[U].
- Regional pricing: Play auto-converts a base price into local currencies, applies "locally relevant pricing patterns" and tax in select countries, supports per-country overrides and sub-dollar price points in 20 emerging markets; pricing templates were retired 2025-10-27 (existing ones unlinked; per-product prices now authoritative) [V1: support.google.com/googleplay/android-developer/answer/6334373]. Apple's equivalent tooling was not verified — two App Store Connect pricing help URLs returned 404 on 2026-10-18 [U].
- Promotions: Play supports sales for paid apps and promo codes [V1: support.google.com/googleplay/android-developer/answer/6334373].

### Ad tolerance (thin evidence)

- [R] Direct user quote (App Store review surfaced on Firecore's site): "i hope the developers never have ads on this app because it will make it lose its credibility."
- [V1] FOSS players position on ad-free/clean: Just Player ("No ads, tracking or excessive permissions") and VLC's listing claims (free, no ads) [V1: github.com/moneytoo/Player; Play listing].
- No representative survey of ad tolerance for player apps was captured; treat "users pay to avoid ads" as plausible but unquantified [U]/[I].

## 5. Compliance landscape

At-a-glance applicability for our design (no accounts, no telemetry by default, local files only):

| Regime | Triggers | Core obligations to plan for | Label |
|---|---|---|---|
| Play Data safety | Any Play-distributed app | Accurate declaration; "no data" only if SDKs truly don't transmit | [V1: answer/10787469] |
| Play storage policy | File access on Android 11+ | Avoid MANAGE_EXTERNAL_STORAGE; use MediaStore/SAF | [V1: answer/10467955] |
| Apple privacy labels | Any App Store app | Label matches SDK reality; "Data Not Collected" possible | [V1: app-privacy-details] |
| GDPR (EU/EEA) | EU users, even account-free | Lawful basis + notice for any off-device data (crash logs); opt-in diagnostics | [V1: Reg 2016/679; firebase.com] |
| CCPA/CPRA (California) | CA users + threshold business | Notice at collection; opt-out mechanisms; verify thresholds | [V1: oag.ca.gov] |
| DPDP (India) | Users in India (also extra-territorial) | Consent/notice; children <18 protections; breach reporting; duties effective 2027-05-13 | [V2: DLA Piper; PRS] |
| LGPD (Brazil) | Users in Brazil | Legal basis + rights handling; ANPD oversight | [V1: planalto.gov.br] |
| EAA (EU) | Covered products/services (verify scope) | Accessibility of service UX; microenterprise carve-out to confirm | [V1: Directive 2019/882] |
| COPPA (US) | Services for under-13s | No targeting of children; parental consent if collecting | [V1: ftc.gov] |
| IARC | All store apps | Age-rating questionnaire (free, multi-region) | [V1: globalratings.com] |

### Google Play requirements

- Data safety form: mandatory; must disclose data collected/shared, encryption in transit, and deletion paths; consumers see a summary [V1: support.google.com/googleplay/android-developer/answer/10787469]. A no-account app that ships zero analytics/crash SDKs can declare "no data collected"; any crash SDK that transmits device IDs changes that answer [I].
- Storage permissions: `MANAGE_EXTERNAL_STORAGE` is restricted to core use cases with a Play declaration/approval process; standard players should use MediaStore / Storage Access Framework pickers, which require no such permission — do not chase the broad permission [V1: support.google.com/googleplay/android-developer/answer/10467955 + I].
- Content ratings: IARC questionnaire is mandatory for Play; single questionnaire yields multi-region ratings at no cost; IARC reports 19M+ apps rated, 3M+ developers, ratings used by 4B+ people [V1: support.google.com/googleplay/android-developer/answer/9859655; globalratings.com].
- New personal developer accounts (created after 2023-11-13): must run closed testing with ≥12 testers for ≥14 continuous days before production access [V1: support.google.com/googleplay/android-developer/answer/14151465].
- Developer verification (anti-sideload tightening): APIs and advanced flow in Aug 2026; protections begin 2026-09-30 in Brazil, Indonesia, Singapore, Thailand for installs via participating stores on certified Android 7+ devices; global rollout in 2027 [V1: developer.android.com/developer-verification]. Implication: distribution outside Play gets more regulated over 2026–27; verify F-Droid/Accrescent interplay during spec [I]/[U].

### Apple App Store requirements

- App Privacy Details ("nutrition labels") are required; "Data Not Collected" is a valid label and is used by Outplayer [V1: developer.apple.com/app-store/app-privacy-details; apps.apple.com/…/id1449923287]. Labels must match SDK reality (e.g., crash SDKs) [I].
- IAP required for digital content (Guideline 3.1.1) [V1: developer.apple.com/app-store/review/guidelines]. Age ratings assigned via questionnaire (4+ achievable for players; Infuse/Outplayer show 4+) [V1 listings].

### GDPR (EU/EEA)

- Applies to EU users without any account; "online identifiers" (IP, device IDs) count as personal data [V1: eur-lex.europa.eu Reg 2016/679, Recital 30]. A no-account player still needs a lawful basis + notice for anything that leaves the device (e.g., crash reports) [I].
- Consent must be freely given, specific, informed, unambiguous (Art. 7); silence/pre-ticked boxes invalid [V1: Reg 2016/679, Recital 32].
- Practical pattern: crash reporting opt-in, off by default; vendors ship this — Firebase Crashlytics documents "Enable Opt-in Reporting" for Android and iOS; Crashlytics retains crash data ~90 days; Google acts as processor under its Data Processing & Security Terms and is DPF-certified for transfers [V1: firebase.google.com/support/privacy].
- Align the privacy notice with actual behavior (retention, processors, DPO/contact, rights) [I].

### CCPA / CPRA (California)

- CCPA/CPRA obligations attach to "businesses" meeting thresholds serving CA residents; core duties: notice at collection, opt-out of sale/share, deletion/correction, non-discrimination [V1: oag.ca.gov/privacy/ccpa; CPRA effective 2023-01-01 per firebase.google.com/support/privacy]. An app that collects nothing and sells nothing should verify threshold applicability; typical indie devs fall below thresholds — confirm with counsel [I].

### India — DPDP Act 2023 + DPDP Rules 2025

- Act published 2023-08-11; Rules notified 2025-11-13; phased implementation: data-protection authority provisions immediate, consent-manager provisions from 2026-11-13, substantive compliance obligations from 2027-05-13 [V2: intelligence.dlapiper.com Data Protection India (last modified 2026-02-13); prsindia.org/billtrack/digital-personal-data-protection-bill-2023].
- Key features: consent + notice; purpose/storage limits; breach notification to the Data Protection Board and affected data principals; children (<18) require verifiable parental consent and cannot be tracked/behaviorally monitored/targeted with ads; penalties up to ₹250 crore [V2: DLA Piper; PRS].
- Implication: a no-account player with opt-in-only diagnostics is low-risk; ensure any future kid-directed features are not added without a compliance review [I].

### Brazil — LGPD

- Lei 13.709/2018 governs processing in Brazil / offering services to users in Brazil; consent is one of several legal bases; ANPD is the authority [V1: planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm]. Minimal-footprint design (no accounts, opt-in diagnostics) keeps exposure low [I].

### EU Accessibility Act (EAA, Directive 2019/882)

- Obligations for covered services apply from 2025-06-28; scope includes listed product/service categories (e.g., e-commerce, consumer banking, e-books, transport services, audiovisual media services) [V1: eur-lex.europa.eu Directive 2019/882; EC page].
- The Directive contains microenterprise provisions (service-provider carve-out) — verify exact applicability (staff/turnover thresholds and whether a paid app feature counts as e-commerce) during legal review [V1 directive + U].
- Regardless of applicability, baseline accessibility (captions/subtitle styling, contrast, screen-reader labels, large targets) is cheap and matches user expectations [I].

### Children (COPPA, US) & age gating

- COPPA (16 CFR Part 312) applies to services directed to children under 13 or with actual knowledge of collecting their data; parental consent required before collection; 2025 rule amendments published 2025-04-22; FTC issued a 2026-02-25 policy statement encouraging age-verification tech [V1: ftc.gov COPPA rule page].
- Our player is a general-audience utility: don't target children; keep no collection so parental-consent mechanics stay moot; revisit if listed in Kids/Families programs [I].

## 6. Distribution economics

- Play service fees: Google's fee page states 97% of developers pay no service fee and 99% of developers subject to fees are eligible for rates of 15% or less [V1: support.google.com/googleplay/android-developer/answer/9858738]. Free apps like ours pay nothing but also earn nothing through the store [I].
- Apple: Apple Developer Program membership $99/yr (standard); App Store Small Business Program offers a 15% commission for developers earning up to $1M in prior-year proceeds (vs standard 30%) [V1: developer.apple.com/programs; developer.apple.com/app-store/small-business-program].
- Android App Bundles: required for new Play apps since Aug 2021 (TV apps since June 2023); compressed download cap 4 GB; bundles generate per-device split APKs [V1: developer.android.com/guide/app-bundle]. Sideloading consequence: partial installs of sideloaded bundle-based apps fail on all Google-certified devices and on Android 10+ if split APKs are missing — so FOSS/sideload users need GitHub/F-Droid APK builds, not raw Play artifacts [V1: developer.android.com/guide/app-bundle + I].
- F-Droid: inclusion requires FLOSS licensing, reproducible source builds, and rejection of tracking/ads; no store fees [V1: f-droid.org/docs/Inclusion_Policy]. Just Player's multi-channel pattern (Play + F-Droid + Accrescent + Obtainium + GitHub + Galaxy/AppGallery/Aptoide/Amazon) is the reference for reaching privacy audiences [V1: github.com/moneytoo/Player].
- Combined with §5 developer verification (2026–2027), plan for: Play as the revenue channel; one or two FLOSS channels as trust/credibility channels; keep an APK sideload path for power users [I].

## 7. Implications for the spec (handoff bullets)

- Position: "no account, no ads, no tracking, plays your files" is a living, validated position (VLC listing claims; Just Player tagline; Infuse review quote) — make it the core promise, not a setting [V1 listings + R].
- Monetize like Outplayer: free with unrestricted playback + one-time unlock; skip ads (evidence: MX's AVOD pivot reshaped its incentives away from the local-file tool; users explicitly fear ads) [V1 listings + R + I].
- Keep a donation path if any part is FOSS/open (Just Player precedent) [V1 README].
- Do not require MANAGE_EXTERNAL_STORAGE; design around SAF/MediaStore from day one [V1 Play policy + I].
- Ship privacy labels/data safety to match reality: target "Data Not Collected"; if crash reports are added, make them opt-in and declare device IDs [V1 firebase + Play data safety + I].
- Pricing band: $4.99–$9.99 one-time (nPlayer $4.99; Outplayer unlock) with regional ladders via Play per-country overrides; revisit only if sync/server features emerge (Infuse model) [V1 + I].
- Compliance checklist for spec: IARC rating; Play data safety; Apple privacy labels; GDPR notice + opt-in diagnostics; DPDP/LGPD/CCPA screeners; accessibility baseline [I].

## Key numbers at a glance

| Metric | Value | Label |
|---|---|---|
| VLC Android installs / rating | 500M+ / 4.4★ (2.01M reviews) | [V1] |
| Amazon MX Player installs / rating | 1B+ / 4.3★ (14.3M reviews), ads + IAP | [V1] |
| Just Player installs / rating | 1M+ / 3.8★ (3.42K reviews), free + donations | [V1] |
| iOS paid-tier ratings | 4.6–4.7★ (Infuse/nPlayer/Outplayer) vs VLC iOS 3.7★ | [V1] |
| Infuse Pro pricing | $1.99/mo · $16.99/yr · $99.99 lifetime (7-day trial) | [V1] |
| nPlayer pricing | $4.99 one-time | [V1] |
| Download → trial (median / p90) | 6.2% / 20.3%; ~82% start day of install | [V2] |
| Trial → paid (Media & Ent. median) | 43.8% | [V2] |
| Download → paid at Day 35 | freemium 2.18% / hard paywall 12.11% | [V2] |
| Annual plan churn (first month) / Y1 retention | ~30% / 44.1% | [V2] |
| Revenue per install (D60, median) | $0.31 | [V2] |
| Median consumer subscription price | $29.99 | [V2] |
| Play service fee exposure | 97% of devs pay none; 99% of fee-subject devs eligible ≤15% | [V1] |
| Apple commission programs | Standard 30%; Small Business Program 15% ≤$1M proceeds | [V1] |
| Play Billing deadline | v8+ required by 2026-08-31 (extension to 2026-11-01) | [V1] |
| Developer verification rollout | BR/ID/SG/TH from 2026-09-30; global 2027 | [V1] |
| India DPDP Rules | Notified 2025-11-13; substantive duties from 2027-05-13 | [V2] |

## Open questions

1. Apple regional-pricing/lifetime-price tooling specifics (ASC help URLs 404'd on 2026-10-18) — need a working source for auto-converted price tiers.
2. MX Player Pro fate (Play URL 404): delisted under Amazon, or moved? Affects "abandoned local-player leader" narrative [U].
3. iOS format-complaint themes (e.g., MKV/DV issues, subtitle complaints) — need structured review mining (App Store reviews, Reddit) to confirm our wedge.
4. A defensible bottom-up market size for local-file players (households, NAS owners, traveling users) — currently [U].
5. Ad-tolerance quantification for players beyond anecdote — any survey/benchmark (e.g., consumer panels) to cite.
6. Chargeback/refund-risk magnitude for one-time unlocks on Play vs App Store (any platform transparency reports?).
7. Whether the EAA applies to a paid feature inside a player app; exact microenterprise thresholds — legal review.
8. DPDP Rules detail for a no-account utility (children provisions, breach duties) ahead of 2027-05-13 enforcement — legal review.
9. Developer-verification impact on F-Droid/Accrescent/Firebase-less builds post-2026 — confirm with platform announcements.
10. Conversion benchmarks specific to video/utility one-time unlocks (RevenueCat is subscription-centric).
11. VLC iOS rating gap (3.7 vs paid 4.6–4.7): confirm whether complaints are about quality, ads (none) or file management, before citing as "paid culture" evidence.
12. Play $25 registration fee and current (2026) fee schedules — re-verify on Play Console pages at spec time [U].

Unreachable sources on 2026-10-18 (state-of-record): App Store Connect pricing help (two URLs returned 404); Amazon MX Player support knowledge-base article counts read from index only; MX Player Pro listing (404); meity.gov.in DPDP page (403); several web searches returned no results (MX/Prime pricing details, Play fee schedule query).

## Sources (accessed 2026-10-18)

Store & product pages
1. VLC for Android — https://play.google.com/store/apps/details?id=org.videolan.vlc [V1]
2. Amazon MX Player — https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad [V1]
3. Just (Video) Player — https://play.google.com/store/apps/details?id=com.brouken.player [V1]
4. Just Player source/README — https://github.com/moneytoo/Player [V1]
5. Infuse — https://apps.apple.com/us/app/infuse/id1136220934 [V1]
6. Infuse product page — https://firecore.com/infuse [V1]
7. nPlayer — https://apps.apple.com/us/app/nplayer/id1116905928 [V1]
8. VLC for iOS — https://apps.apple.com/us/app/vlc-media-player/id650377962 [V1]
9. Outplayer — https://apps.apple.com/us/app/outplayer/id1449923287 [V1]
10. Outplayer site — https://outplayer.app [V1]
11. Amazon MX Player support (Prime Video merge) — https://support.mxplayer.in [V1]
12. Amazon MX Player site — https://www.mxplayer.in [V1]

Platform policies & docs
13. Play Data safety — https://support.google.com/googleplay/android-developer/answer/10787469 [V1]
14. Play MANAGE_EXTERNAL_STORAGE — https://support.google.com/googleplay/android-developer/answer/10467955 [V1]
15. Play billing requirement — https://support.google.com/googleplay/android-developer/answer/112622 [V1]
16. Play service fees — https://support.google.com/googleplay/android-developer/answer/9858738 [V1]
17. Play Billing deprecation FAQ — https://developer.android.com/google/play/billing/deprecation-faq [V1]
18. Play content ratings — https://support.google.com/googleplay/android-developer/answer/9859655 [V1]
19. Play testing requirements — https://support.google.com/googleplay/android-developer/answer/14151465 [V1]
20. Play refunds — https://support.google.com/googleplay/answer/2479637 [V1]
21. Play pricing — https://support.google.com/googleplay/android-developer/answer/6334373 [V1]
22. Android App Bundles — https://developer.android.com/guide/app-bundle [V1]
23. Android developer verification — https://developer.android.com/developer-verification [V1]
24. Apple App Review Guidelines — https://developer.apple.com/app-store/review/guidelines [V1]
25. Apple App Privacy Details — https://developer.apple.com/app-store/app-privacy-details [V1]
26. Apple Small Business Program — https://developer.apple.com/app-store/small-business-program [V1]
27. Apple Developer Program — https://developer.apple.com/programs [V1]
28. Apple refunds — https://support.apple.com/en-us/118223 [V1]
29. IARC — https://www.globalratings.com [V1]
30. F-Droid Inclusion Policy — https://f-droid.org/docs/Inclusion_Policy [V1]
31. Firebase privacy & security (Crashlytics opt-in, retention) — https://firebase.google.com/support/privacy [V1]

Law & regulation
32. GDPR Regulation (EU) 2016/679 — https://eur-lex.europa.eu/eli/reg/2016/679/oj [V1]
33. CCPA — https://oag.ca.gov/privacy/ccpa [V1]
34. LGPD (Lei 13.709/2018) — https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm [V1]
35. COPPA Rule — https://www.ftc.gov/legal-library/browse/rules/childrens-online-privacy-protection-rule-coppa [V1]
36. EAA Directive 2019/882 — https://eur-lex.europa.eu/eli/dir/2019/882/oj [V1]
37. India DPDP overview — https://prsindia.org/billtrack/digital-personal-data-protection-bill-2023 [V2]
38. India DPDP tracker (rules timeline) — https://www.dlapiperdataprotection.com/index.html?t=law&c=IN [V2]

Benchmarks
39. RevenueCat State of Subscription Apps 2025 — https://www.revenuecat.com/state-of-subscription-apps-2025/ [V2]
