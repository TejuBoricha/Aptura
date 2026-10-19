# 15 — Monetization, Store Operations, and Maintenance

**Version:** 0.1 (draft)
**Date:** 2026-10-19
**Status:** Draft. Monetization content is **hypotheses only** — nothing is implemented, priced, or promised in the spec phase (DEC-004). Operations content defines the release/rollout/maintenance *plan*; every policy-sensitive item carries "recheck before release" because Play rules and verification programs move.
**Consumes:** 02 §8.1 (pricing anchors, RevenueCat 2025), W4 (`research/market_compliance.md`), 07 §13 (claims discipline), 10 (compliance register), 11 (SBOM/LGPL ops), 12 (release gates), 14 (support channels).
**Feeds:** 16 (roadmap resources), 18 (readiness checklist).

---

## Part A — Monetization

### 1. Principles (fixed by product decisions)

1. **Launch monetization = free, no ads** (DEC-004). No ad SDK exists in the dependency graph (doc 11), no data monetization ever — monetizing user attention or data contradicts the entire positioning (doc 01/03).
2. **Core playback is never paywalled in any scenario we're considering.** A video player that degrades playback to upsell is a different, distrusted product.
3. **Any paid unlock must work with no account and no backend** (RQ-08) or it doesn't ship — Play Billing offline resilience, restore flows via store, regional price tiers; store handles receipts.
4. **No dark patterns:** no trial pressure UI, no countdowns, no feature-teasing banners in the player. Optional prompts, once, dismissible, at natural moments (never mid-playback).
5. **Trust metrics outrank revenue metrics** in every trade-off (KPI framework, doc 03).

### 2. Options analysis (all hypotheses; nothing selected)

| Option | Shape | Strengths | Risks | Verdict posture |
|---|---|---|---|---|
| **A. Fully free** | Everything free; optional donations off-store or via Play IAP later | Max trust/install velocity; zero billing surface; simplest ops | No revenue; sustainability depends on owner intent | **Default baseline** |
| **B. One-time "Pro" unlock (R1.x candidate)** | Free core forever; paid: power features (e.g., advanced subtitle styling packs, automated library rules, extra themes) — **never** core playback, subtitles-for-foreign-content basics | Aligns with pay-once preference seen in this category (pricing anchors in doc 02 §8.1); no subscription fatigue | Requires Play Billing integration (no backend), regional pricing, refund handling; must not fragment perception | Leading candidate **if** retention/trust KPIs (doc 03) hold |
| **C. Subscription** | Recurring | Revenue scale on paper (RevenueCat 2025 benchmarks cited in doc 02 §8.1 show where subscriptions work) | Category convention is pay-once; subscriptions in a local-file utility invite resentment; refunds/chargebacks; ops complexity | **Not recommended for R1/R1.x** |
| **D. Ads / data** | — | — | Contradicts DEC-004/008 and the pain-point evidence (doc 02 §5: ads are a top complaint) | **Excluded permanently by principle** |
| **E. Donations** | "Buy me a coffee" links; Play allows link-out in limited forms | Low friction, voluntary | Almost no revenue signal at scale in this category `[E]`; store policy constraints on external links `[recheck]` | **MAY** as passive add-on |

**Decision trigger for B:** after R1 stable + KPI evidence (doc 03) + OD-3 owner choice. Until then, nothing implemented; spec keeps the door open without billing code in scope.

### 3. If B is pursued (pre-registered requirements checklist)

- Billing via Play Billing Library (current required version `[recheck]`), product ID scheme, price tiers by region (use store auto-conversion with manual overrides for key markets per ASM-009).
- Offline: entitlement cached locally, re-validated opportunistically on next online store interaction — but note: **R1 app has no `INTERNET` permission**; entitlement validation happens through Play Billing's own surface (Play app/binder), not our network stack `[recheck: confirm Play Billing behavior without INTERNET permission — spike RQ-08]`.
- Restore: "Restore purchases" in Settings; store-account based (no our-account exists).
- Refunds: store processes; our UI explains in one line.
- Family sharing eligibility decision `[recheck]`.
- Accessibility: purchase flow fully TalkBack-navigable; no purchase pressure for core tasks.

---

## Part B — Store operations, release, rollout

### 4. Launch prerequisites (checklist; owners assigned at build phase)

| # | Item | Source | Gate |
|---|---|---|---|
| S1 | Play Console account, identity verification, Play App Signing setup, upload-key escrow | Play policies `[V1, recheck]` | Pre-build |
| S2 | **Developer verification** — regional rollout noted in W4 (BR/ID/SG/TH from 2026-09-30, global through 2027); complete early | W4 `[V2, recheck]` | Pre-launch |
| S3 | Store listing: title/short/long description — **claims audit** (doc 07 §13) before submission; screenshots from real UI (doc 06 SCR set); feature graphic | TST-VIS (doc 12) | Gate B |
| S4 | Privacy policy URL (static, no-analytics page; Q14-2) + data-safety form matching doc 10 §4 inventory exactly | Docs 10/14 | Gate B |
| S5 | Content rating questionnaire (IARC) | Play flow | Gate B |
| S6 | Contact email + support surface live (doc 14 §6) | Doc 14 | Gate B |
| S7 | Target-API deadline compliance (**API 36**, W4/`[V1, recheck]`) | Platform | Gate B |
| S8 | **Closed testing: ≥12 testers / 14 continuous days** if required by new-account policy at our launch window `[V2, recheck]` — plan assumes it's required | Play policy | Pre-production |
| S9 | SBOM + license notices generation wired into release build (doc 11 §4) | Doc 11 | Every RC |
| S10 | LGPL source-offer hosting live (OQ-3) — **blocking issue only if** bundled audio ext ships (L1 pending) | Doc 11 | Conditional |

### 5. Release pipeline (Android R1)

```
dev build (internal signing)
→ alpha track: team only (dogfood, ~2 weeks)
→ closed testing: recruited cohort (also serves S8), 2-4 weeks, crash/vitals watch
→ open testing (optional gate: +2 weeks if signal unclear)
→ production staged rollout: 5% → 20% → 50% → 100%, 48-72h between stages [T]
   ├─ halt criteria per doc 12 Gate C (vitals thresholds)
   └─ halt = pause rollout, fix, new patch through same gates
→ hotfix path: patch releases skip none of the Gates (A/B reduced to regression scope + full Gate C watch)
```

**Release cadence:** calm-by-design. Target roughly monthly feature patches during R1 stabilization, then as-needed. **No forced-update walls, no update nags in the player** (an update prompt is allowed at most once per version, dismissible, in Settings/Library context only).

**Versioning & artifacts:** semantic version, versionCode discipline, build hash in About (SCR-016) and reports (doc 14); reproducibility notes kept with release records.

**Store presence discipline:** changelogs in plain language (same tone rules as doc 06 §7); review responses templated but human (doc 14 §6).

### 6. Rollback & incident playbooks

| Scenario | Response |
|---|---|
| Bad release caught at staged rollout | Pause rollout; assess; ship fixed patch (preferred) — full "rollback to previous version" is rarely possible on Play, so **pause + forward-fix** is the standing model |
| Data-affecting bug (history/favorites corruption) | Immediate rollout pause + hotfix priority; communication via release notes; local backup/export path exists? (no cloud — document affected scope honestly) |
| Security/Vulnerability report | Triage per doc 10 incident response; patch timeline per NFR-SEC; coordinated disclosure policy statement prepared at build phase |
| Store policy strike/removal | Appeal with compliance evidence (doc 10 register as the paper trail); meanwhile communicate via support channels; distribution is store-only (no sideload channel to fall back on — stated as risk in doc 17) |
| LGPL ⚖️ item lands negative at build (L1 rejected) | Execute doc 11 §3.5 fallback: drop bundled audio decode; **no store emergency** — product degrades gracefully, no relicensing scramble |

### 7. Post-launch maintenance model

| Stream | Cadence | Notes |
|---|---|---|
| Dependency updates (Media3, Compose, AndroidX, Hilt, Coil) | Monthly batch + security emergency | Read release notes; run full Gate-A-equivalent regression before shipping |
| Android OS behavior changes (annual) | Per beta season | Threat-model and storage/permission reviews updated (docs 09/10) |
| Android TV / foldable / iOS exploration | R1.x/R2 gates | Trigger points in doc 16 |
| Patent/license rechecks | Per release gate + quarterly (doc 11 §2) | Rate tables move; recheck flags honored |
| minSdk policy | Only raised with evidence (device data vs. maintenance cost) + announcement lead time | Recheck each annual cycle; floor currently 26 |
| Support load | Tracked (doc 14 §6); recurring themes → QA fixtures (doc 12) → roadmap (doc 16) | 72h target during rollout watch `[T]` |
| Device lab currency | ≥1 device per OS band kept current; retire per NFR-OPS policy | Cost tracked in doc 16 |

### 7.1 End-of-life, unpublishing, and continuity

If the app is ever discontinued or the listing is removed:

- **Nothing remote breaks.** There is no backend to shut down; installed copies keep working offline indefinitely — the local-first decision (doc 09 §11) doubles as the EOL strategy.
- **Notice:** ≥6 months ahead of any planned sunset: one dismissible in-app notice (never a nag), final release notes with rationale, and the privacy-policy page kept online for a stated horizon.
- **LGPL obligations persist:** if the bundled audio extension ever shipped, the corresponding-source offer must remain available for as long as binaries are distributed plus a reasonable period `[recheck with counsel]` (doc 11 §3).
- **User data:** entirely local; uninstall removes it. No account deletion exists to coordinate. If a future release adds any export utility, it outlives the app.
- **Store semantics:** unpublish vs. remove behaviors and legacy-device installability `[recheck at build phase]`.

### 8. Cost posture (operations-level `[E]`; owner-invisible until OD-4 answered)

Recurring: Play Console registration (one-time fee `[recheck]`), developer-verification program fees (if any `[recheck]`), device lab upkeep, signing-key operational care, support time, optional privacy-policy hosting charge. No servers, no per-user marginal cost — the no-backend decision (doc 09) keeps ops costs near-zero and flat. This is a deliberate commercial feature, not just an architecture choice.

---

## 9. Cross-checks with the rest of the package

- No monetization surface may contradict the no-ads/no-tracking proof set (docs 10/14) — any future billing addition re-checks data-safety forms.
- Store copy consumes doc 07 tiers only (no capability beyond validated cells; TST-VIS gate).
- Rollout gates (doc 12 §9, Gate C) are the *only* rollout authority; this doc adds process, not new thresholds.
- Privacy policy page includes: no analytics on the page, no cookies beyond hosting defaults `[recheck]`.

---

## 10. Open questions

| # | Question | Owner |
|---|---|---|
| OQ15-1 | OD-3 decision (free vs. one-time unlock intent) — affects R1.x planning only | Owner |
| OQ15-2 | Community/support surface choice (Q14-1 dependency) | 15 |
| OQ15-3 | Privacy-policy hosting provider with no-analytics guarantee | 15 |
| OQ15-4 | Play Billing Library minimum version at our build window `[recheck]` | build phase |
| OQ15-5 | Whether donations link (option E) is worth the store-policy attention at R1 | 15 |
