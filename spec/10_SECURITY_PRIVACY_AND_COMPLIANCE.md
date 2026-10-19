# 10 — Security, Privacy, and Compliance

**Version:** 0.1 (draft)
**Date:** 2026-10-19
**Status:** Draft. Threat model is design-level; **no penetration test or audit has occurred**. Compliance items reference platform policies and statutes as researched (W2/W4) and are labeled with review status; ⚖️ items require qualified legal review before shipping decisions (doc 11 mirrors this list).
**Consumes:** 04 (FRs), 05 (NFR-SEC/PRIV), 09 (architecture), `research/platform.md`, `research/market_compliance.md`.
**Feeds:** 11 (legal items), 14 (diagnostics), 15 (store forms), 17 (registers), 18 (gates).

---

## 1. Security posture in one paragraph

The app plays untrusted local files on a device we don't control, keeps a private local library, and — in R1 — **has no network capability at all** (ADR-011). That removes the largest attack classes (remote exploitation, exfiltration, account takeover) by construction. What remains: parser-level abuse via media/subtitle files, malicious intents from other apps, local data exposure to other apps on the device, supply-chain risk in our dependencies, and platform-policy misuse (FGS/notifications). The design response is: use vendor-maintained decoders where possible, cap and isolate our own parsing work, validate every external input at the boundary, keep app data app-private, and keep the permission/foreground surface minimal and truthful.

---

## 2. Assets, adversaries, and trust boundaries

**Assets:** user media files (confidentiality of *existence* and content), library metadata (names/paths — potentially sensitive), watch history/progress, user preferences, app integrity (updates, not being spoofed), user device resources (battery/storage/CPU).

**Adversaries:** malicious media/subtitle files (drive-by targeting of parsers); malicious or careless apps sending intents; local malware reading app-private data (only via OS compromise); supply-chain attackers; scammers misusing our brand (out-of-scope response; store reporting playbook in doc 15); *us* as the dark side (i.e., avoiding patterns that harm users — anti-abuse design).

**Trust boundaries:** (1) OS/device — trusted as deployed, patched by vendor; (2) MediaStore/SAF — semi-trusted providers, validate everything; (3) other apps — untrusted; (4) the media files themselves — untrusted bytes; (5) our build pipeline — must be hardened (supply chain).

---

## 3. Threat register (design-level)

Each THR: **scenario → impact → v1 mitigation → residual risk.** Likelihood labels are `[E]` pre-implementation estimates, not measurements.

| ID | Scenario | Impact | Mitigation (R1 design) | Residual risk |
|---|---|---|---|---|
| THR-001 | Crafted container/codec file exploits a platform or engine decoder | Crash, possible RCE in worst case | Decode via platform/vendor-maintained paths; Media3 fuzzed by upstream; our parser use is bounded; decoder crash containment (FR-PLAY-016); quick dependency updates | Platform bug classes remain (vendor patch latency) |
| THR-002 | Malicious subtitle file (huge cue counts, pathological tags, encoding bombs) | Hang, OOM, UI freeze | Parser caps + watchdog + isolation of parse work (FR-SUB-007, doc 09 §8); no scripting/execution semantics (tags are drawn, not run) | Novel pathological inputs — covered by fuzz corpus (doc 12) |
| THR-003 | Malicious embedded metadata (tags/artwork) | Parser abuse; icon/artwork bombs | Platform metadata extraction; bounded artwork decode; no auto-actions from metadata | Low |
| THR-004 | Malicious intents/URIs from other apps (`VIEW`/share) | Open wrong content; probe app behavior; crash | Validate scheme/type/flags; read-only open; no auto-index of arbitrary trees; explicit, visible context of what's opening (FR-LIB-020) | Low-medium (fuzzing doc 12) |
| THR-005 | Path traversal / provider trickery via crafted URIs | Reading unintended files the sender could access? (Confused-deputy) | Use `ContentResolver` API only; never construct paths from untrusted input; scoped storage; refuse unknown schemes | Low |
| THR-006 | Local malware reads app data (library/history) | Privacy loss | App-private storage; no world-readable files; `allowBackup=false` (doc 09 §10); no external caches | Root-level compromise out of scope |
| THR-007 | Shoulder-surf / recents-thumbnail exposure of viewed content | Privacy embarrassment | Hidden-folder exclusion (FR-LIB-005); honest "not a vault" messaging; `FLAG_SECURE` option = **MAY R2**; recents snapshot policy = R2 review | User-visible tradeoff documented |
| THR-008 | Supply-chain compromise (dependency/build tooling) | Backdoor, data theft | Pinned versions; SBOM (doc 11); quarterly CVE review; minimal dependency surface; offline signing keys; reproducible-ish CI goal (NFR-OPS-001) | Ecosystem-level attacks (mitigate by cadence) |
| THR-009 | FGS/notification misuse patterns (battery drain, notification spam) | User harm, Play policy action | Strict FGS lifecycle (FR-PLAT-003); no boot auto-start; no sticky notifications; media notification only during playback | Low |
| THR-010 | Over-broad permissions; policy mis-declaration | Rejection, user distrust | Minimal permission set + rationale UX (NFR-PRIV-004); no `MANAGE_EXTERNAL_STORAGE` (policy-invalid use, W2); declaration reviewed at launch gate | Declaration approval outcome (launch gate Q10) |
| THR-011 | Future network feature re-introduces attack surface | Broad | R1 has **no `INTERNET` permission** — capability absent, not disabled; R2 network work requires this threat model's update as a gate (FR-NET-001) | Future-phase discipline |
| THR-012 | Resource-abuse by hostile files (scan/thumbnail bombs, huge cue trees) | Battery/storage/CPU harm | Budgets, caps, cancellation, battery gates (NFR-RES-*) | Low |
| THR-013 | Deceptive UX (dark patterns) accidentally introduced | Trust erosion | Design principles doc 01; no ads/nags; review checklist in doc 06; "no silent machinery" (doc 09 §1) | Low |
| THR-014 | Backup/D2D leakage of library data | Privacy loss | `allowBackup=false`; revisit with user-data-only export design (doc 09 §10) | Low |
| THR-015 | Brand spoofing / fake "our app" on stores | User harm (external) | Store-report playbook in doc 15; trademark decision (OD-6) | External, ongoing |

**Out of scope (documented):** device-level malware with root, OS zero-days, hardware attacks, side-channel research. No DRM circumvention exists in-product (DEC-006) — protected files fail with honest messaging; we do not market anti-DRM capabilities (anti-circumvention hygiene).

---

## 4. Privacy: data inventory and principles

### 4.1 Data inventory (this is the authoritative list for store forms)

| Data | Where it lives | Purpose | Retention | User control | Sensitive? |
|---|---|---|---|---|---|
| Media files | User storage (untouched) | Playback | User's own | — (we never copy/modify; read-only hash-verified tests) | User content |
| Library records (names, paths, sizes, durations, codec info) | App-private DB | Browsing, search | Until item removed/reset | Per-folder exclusion; reset | **Potentially sensitive** (paths/name patterns) |
| Watch history & resume positions | App-private DB | Continue watching, resume | Until cleared | Clear history (per item/global) | Potentially sensitive |
| Favorites, playlists, per-video overrides | App-private DB | User features | Until deleted | Delete in-app | Potentially sensitive |
| Thumbnails | App cache | Browsing visuals | LRU, 150 MB default | Clear cache | Low (derived imagery) |
| Settings | App-private DataStore | Behavior | Until reset | Reset scopes | Low |
| Local logs | App-private files | Diagnostics | ≤7 days / ≤10 MB ring | Clear in-app | Low (redaction rules §4.3) |
| Diagnostics bundles (user-initiated) | Created on device; shared only by user action | Support | User-controlled file | Preview before share | Must be redacted (§4.3) |
| Crash/quality telemetry | **None collected by us in R1**; Play vitals are Google-side aggregates | Ops | — | OS/store level | — |

**Never collected (by design):** contacts, location, IMEI/ad IDs, installed-app lists, media content, subtitle text, filenames/paths in any external artifact by default, accounts (none exist).

### 4.2 Principles mapped to mechanisms

| Principle | Mechanism |
|---|---|
| Data minimization | Local-only inventory above; no identifiers |
| Purpose limitation | Local processing; no secondary use exists |
| Storage limitation | Rings/LRU/caps; clear actions everywhere (NFR-PRIV-006) |
| Integrity & confidentiality | App-private storage; no network; no backup |
| Transparency | In-app privacy page mirrors this table; store forms match; "no account" stated (FR-SET-004) |
| User control | Clear/reset/delete everywhere; denial-of-permission degraded mode (FR-PLAT-006) |

### 4.3 Redaction rules (apply to logs, bundles, screenshots in reports)
Filenames/paths excluded by default; opt-in attach of one specific filename requires explicit per-share action (FR-DIAG-002). Subtitle text, frame content, and watch history never included. Device info limited to class/API/codec classes/app version.

### 4.4 Permissions matrix (R1)

| Permission | Why | When requested | Denial behavior | Policy note |
|---|---|---|---|---|
| `READ_MEDIA_VIDEO` (33+) / `READ_EXTERNAL_STORAGE` (≤32) | Library access | After rationale screen (FR-LIB-002) | Degraded library (picker/SAF); open-with always works | Play declaration required `[V1/W2]` — launch gate |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (34+) | Background audio continuation (optional feature) | At first "continue as audio" use | Feature unavailable; foreground playback unaffected | Type-correct FGS mandatory `[V1]` |
| `POST_NOTIFICATIONS` (33+) | Only if non-media notifications ever added | — | n/a in R1 — media-session notifications are exempt `[V1]`; do not request in R1 `[I]` | Revisit with any new notification |
| `WAKE_LOCK` | Keep screen awake during visible playback | Manifest (normal) | — | Scoped to playback states (FR-PLAT-005) |
| **`INTERNET`** | **Not present in R1** | — | — | Strongest privacy proof; proof tests TST-PRIV-NET-04 |

---

## 5. Security engineering practices (build-phase commitments)

- **Input boundaries:** every external byte (file, subtitle, intent, provider row) treated as untrusted; validation at module boundaries (doc 09 rules).
- **Fuzzing program (post-R1 build):** subtitle parsers, sidecar matcher, intent handlers, metadata ingestion → corpus + harness in doc 12; findings tracked as security defects.
- **Dependency discipline:** pinned; SBOM per release (CISA 2026 v2.1 fields; CycloneDX/SPDX — W3); quarterly review; CVE watch on Media3/FFmpeg/Compose.
- **Secrets:** none in repo; signing keys offline; Play App Signing; CI least-privilege (NFR-SEC-003).
- **Release integrity:** R8; Play App Signing; staged rollout (NFR-OPS-002).
- **Incident response (no-server version):** severity classes (S1 exploitable-in-product / S2 privacy-affecting / S3 quality); S1: halt rollout, patch, store-notes advisory; user notifications only via release notes/docs (no push channel exists); dependency-CVE triage within policy `[E]`; postmortems for S1/S2.
- **Anti-abuse self-checks:** no dark patterns; no notification spam; no battery abuse (THR-009/013 review before release, doc 18 gate).

---

## 6. Compliance register (R1 scope)

### 6.1 Platform policies (Google Play; iOS only at its gate)

| Item | Requirement (as researched) | Our stance | Status |
|---|---|---|---|
| Photo/video permissions | `READ_MEDIA_VIDEO` needs declaration; broad storage must not be used for media access `[V1/W2]` | Declaration drafted at launch gate; degraded mode built (FR-PLAT-006) | **Launch gate Q10** |
| `MANAGE_EXTERNAL_STORAGE` | Media-file access listed as invalid use `[V1]` | Never used | Resolved by design |
| Data safety form | Must match actual collection | "No data collected/shared" per §4.1; re-verify at each release | Gate: doc 15 checklist |
| Foreground service | Correct types; no boot-time media FGS start (15+) `[V1]` | FR-PLAT-003; no boot auto-start | Resolved by design |
| Target API level | Target 36 deadline 2026-08-31 (extension to 2026-11-01) `[V1/W2]` | Plan: target current at launch | Gate |
| Vitals thresholds | Crash/ANR flags (1.09%/8%, 0.47%/8%) `[V1]` | Stricter internal targets (NFR-REL-001/002) | Monitored |
| Content rating (IARC) | Questionnaire at listing | Likely "3+/Everyone"; no UGC, no ads | Gate: doc 15 |
| Account deletion policy | Apps with accounts must provide deletion | **N/A — no accounts** (documented rationale) | Resolved by design |
| Testing requirements | New personal accounts: ≥12 testers × 14 days closed testing `[V1/W4]` | Ops plan in doc 15/16 | Ops gate |
| Developer verification | Regional rollout (BR/ID/SG/TH from 2026-09-30; global 2027) `[V1/W4]` | Register early; affects non-Play distributions too | Ops gate |
| AAB requirement | AAB for Play `[V1]` | Build config | Resolved by design |

### 6.2 Statutes & frameworks (applies to an on-device-only, no-account app — scope notes as researched)

| Regime | Relevance to us | Required actions | Status |
|---|---|---|---|
| GDPR (EU) | On-device processing only; no personal-data transfer to us; no tracking | Privacy page mirroring §4.1; no DPIA expected (no systematic monitoring/special categories) `[I]` — legal check | ⚖️ review |
| CCPA/CPRA (California) | No sale/share; no collection by us | Inventory statements; no "Do Not Sell" flows needed if claims hold `[I]` | ⚖️ review |
| India DPDP Act (Rules notified 2025-11-13; duties phasing to 2027-05-13) `[V1/W4]` | Local processing; permission UX; children's provisions (no targeted processing) | Consent clarity for permissions; no children targeting | ⚖️ review |
| LGPD (Brazil) | Similar local-only posture | Portuguese privacy text at launch locale set | ⚖️ review |
| COPPA (US, 2025 amendments) | Not directed to children; no collection | Store family policy + rating truthful | ⚖️ review |
| EAA (EU accessibility, consumer products) | Applicability to free/FOSS apps has carve-outs (microenterprise caveat noted in W4) | Accessibility commitments already core (NFR-A11Y-*) regardless of applicability | ⚖️ review |
| Anti-circumvention (DMCA §1201-class) | No DRM removal/circumvention features exist | Protected content → honest unsupported messaging; no marketing implying otherwise | Resolved by design |
| Export/crypto | No custom cryptography | Platform APIs only | Resolved by design |

### 6.3 Consolidated legal-review list (⚖️ — mirrored in doc 11)
LGPL relinking mechanics for bundled FFmpeg `.so`; AC-3/E-AC-3 decode add-back; DTS/TrueHD policy wording; Dolby-brand usage in UI text; statute applicability notes above; trademark/name clearance (OD-6); store copy wording from doc 07 §13.

---

## 7. Privacy-by-design checklist (release gate, doc 18)

- [ ] No `INTERNET` permission in the shipped manifest (proof test).
- [ ] No third-party analytics/crash SDKs.
- [ ] Store forms (data safety, rating) match §4.1 exactly.
- [ ] Redaction verified on real user-initiated bundles (fixtures).
- [ ] Deletion paths verified (history/cache/reset) with hashes.
- [ ] Permission flows: rationale → request → degraded mode all exercised.
- [ ] FGS lifecycle events audited (start/stop only in playing states).
- [ ] Logs contain no filenames/paths/subtitle text (fixture scan).

## 8. Open questions

| # | Question | Owner |
|---|---|---|
| S1 | `FLAG_SECURE`/recents-snapshot policy (usability vs exposure) — R2 review | 06/10 |
| S2 | Whether a "vault" mode is ever in scope (currently: honest no; hidden-folders only) | 15/16 |
| S3 | Fuzzing scope depth for R1 vs post-launch hardening phase | 12 |
| S4 | Incident-channel mechanics without any server (release-notes advisories only) — acceptable? | 15 |
| S5 | DPDP/ GDPR interplay documentation language for the privacy page (legal drafting) | ⚖️/15 |
| S6 | Copycat-app response playbook details | 15 |
