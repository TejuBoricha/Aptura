# 14 — Diagnostics, Telemetry, and Support Plan

**Version:** 0.1 (draft)
**Date:** 2026-10-19
**Status:** Draft. This document owns the `EV-*` diagnostic event namespace (defined in 00 §4) and the support/report workflow. It must remain *consistent with* doc 10 §4 (authoritative data inventory): **nothing here adds a data flow that doc 10 does not describe.**
**Consumes:** 04 (FR-DIAG-001…005, FR-SET-007/008), 05 (NFR-PRIV/OPS), 09 (no-backend rationale), 10 (privacy inventory, THR register), 12 (QA feedback loop).
**Feeds:** 15 (store support requirements), 18 (privacy-proof traceability).

---

## 0. The R1 constraint that shapes everything

R1 ships with **no `INTERNET` permission, no analytics SDK, no crash-reporting service, no remote config, no account** (ADR-007, ADR-011, DEC-008). Consequences, stated honestly:

1. **There is no automatic telemetry of any kind.** The app cannot phone home even if it wanted to — the permission does not exist.
2. **All diagnostics are local** and leave the device only when the user explicitly exports them through the Android share sheet (user-initiated, per-report, previewable).
3. **We have real blind spots** (crash clustering, device-mix visibility, funnel metrics). This document names how we compensate instead of pretending they don't exist, and pre-registers the conditions for ever changing this ( §9).

---

## 1. Principles

| # | Principle | Implication |
|---|---|---|
| D1 | **Local-first diagnostics** | Everything useful must be available on-device: stats overlay, error codes, event log, crash log |
| D2 | **User-initiated sharing only** | One export path: preview → share sheet. No background transmission, ever, in R1 |
| D3 | **Report = exactly what the preview shows** | The share artifact is generated from the previewed bundle; no hidden fields |
| D4 | **No media content, ever** | Not frames, not audio, not subtitle text dumps, not file bytes — in any artifact |
| D5 | **Paths and names are sensitive** | Excluded by default; opt-in per report with an explicit warning (a filename can itself be sensitive) |
| D6 | **Support must not require sending files** | Canned flows never ask users to upload copyrighted media; reproduction requests use *attributes* (container/codecs/profile), not content |
| D7 | **Boring, bounded logs** | Ring-buffered, size-capped, time-capped; no unbounded growth, no battery cost |
| D8 | **Consistency with doc 10** | Any addition here is a doc 10 change first (inventory + threat review), then an implementation change |

---

## 2. Local diagnostics design

| Component | Behavior | Bounds `[T]` |
|---|---|---|
| **Event log** | Structured `EV-*` records (§3) written to app-private storage | Ring buffer: last 5 sessions or 2 MB, whichever first |
| **Crash log** | Uncaught-exception capture (local only), stored as plain text + stack | Last 3 crashes retained |
| **Session marker** | Rotating session ID (non-persistent across reinstalls, not a device identifier); lets a report stitch events | Generated per process start |
| **Log levels** | Release builds: INFO for lifecycle events, WARN/ERROR for anomalies; DEBUG gated behind in-app toggle with a persistent "diagnostics on" reminder | — |
| **Retention** | Events/crashes older than 30 days or beyond ring bounds are dropped | 30 days `[T]` |
| **Deletion** | "Delete diagnostic data" in Settings (SCR-014/015) clears everything instantly; no hidden copies | — |
| **Post-crash prompt** | On next launch after a crash: quiet, dismissible card "Last session ended unexpectedly — share details?" with preview-first flow (SCR-015). Never nags; "not now" is respected; no repeat after second dismissal in a row | — |

**Stats overlay (SCR-010e / FR-DIAG-001)** remains the power-user surface: decode path (hw/sw/platform-sw), codecs, resolution/fps, dropped frames, A/V offset, HDR path. Copyable as text (goes into report bundle if the user chooses).

---

## 3. `EV-*` event registry (v0.1 starter set)

Schema per event: `name` · `category` · `fields` (all fields are enumerated, structural attributes — never file content or full paths). ID ranges are reserved for build-phase expansion; new events must pass a doc 10 privacy review before landing.

| ID | Event | Category | Key fields (structural only) |
|---|---|---|---|
| EV-001 | app.start | APP | cold/warm, first-run flag, build type |
| EV-002 | app.migration | APP | from/to schema version, duration, outcome |
| EV-003 | lib.scan.start | LIB | trigger (first-run/refresh/event), scope count estimate |
| EV-004 | lib.scan.progress | LIB | stage (enumerate/extract/thumb), items processed, elapsed |
| EV-005 | lib.scan.end | LIB | found/added/removed counts, duration, cancel flag, errors |
| EV-006 | lib.scan.file_error | LIB | stage, error class (permission/corrupt/IO), extension only |
| EV-007 | lib.identity.resolve | LIB | match outcome (exact/heuristic/miss), no names |
| EV-008 | lib.thumb_fail | LIB | error class, tier attempted |
| EV-009 | play.session.start | PLAY | container, video codec/profile, audio codec, resolution class, duration bucket |
| EV-010 | play.decode.elect | PLAY | requested path, elected path (hw/sw/platform-sw/ffmpeg-audio), fallback chain, notice shown |
| EV-011 | play.error | PLAY | error code (FR taxonomy doc 07 §err), stage (prepare/decode/render/audio) |
| EV-012 | play.session.end | PLAY | reason (user/complete/error/kill), position bucket, drop count |
| EV-013 | play.seek | PLAY | target bucket, method (keyframe/exact), latency ms |
| EV-014 | play.sync_shift | PLAY | p95 offset bucket, drift direction |
| EV-015 | sub.load | SUB | format, encoding detected vs. override, cue count bucket, timing shift bucket |
| EV-016 | sub.font_fallback | SUB | requested family class, fallback class (no family names) |
| EV-017 | aud.track.elect | AUD | codec, passthrough vs. predicted decode, output route class |
| EV-018 | plat.lifecycle | PLAT | state transition (bg/fg/pip/killed), foreground-service flags |
| EV-019 | set.migration | SET | preference version from/to, outcome |
| EV-020 | diag.export | DIAG | bundle generated, sections included (user's own action) |

Reserved ranges: `EV-021…049` PLAY/decoder depth; `EV-050…069` LIB/scan depth; `EV-070…089` SUB/AUD; `EV-090…099` APP/PLAT/SET. Registry is versioned with the schema doc in the build repo.

---

## 4. Report bundle specification

**Generation:** SCR-015 → "Create report" → preview screen → share sheet (`FileProvider`). Nothing is generated or shared without the user pressing share.

**Default contents (all visible in preview):**

| Section | Content |
|---|---|
| App context | Version, build hash, build type, install age bucket |
| Device context | Model, OS version, ABI, screen class, locale — answered from Build/settings, never a persistent device ID |
| Playback diagnostics | Stats-overlay text of last 1–3 sessions (EV-010/012/013/014 derived) |
| Error history | Last N `play.error` entries with codes and timestamps |
| Log excerpt | Last-session `EV-*` events (redacted per §3 schema — no names/paths) |
| Settings snapshot | Non-sensitive toggles only (decode prefs, subtitle style categories) — never contained in a secret store since none exists |

**Opt-ins (default OFF, explicit warning each):** filenames/paths of the affected file(s); recent-search strings; the current subtitle file **name** (not content). The warning text states plainly that names can be sensitive.

**Excluded, structurally impossible:** media bytes, subtitle content, frame captures, contacts, accounts (none exist), location (never requested), advertising IDs (never requested).

---

## 5. Self-help surfaces (what users can fix without contacting anyone)

1. **Error-code lookup** (SCR-016): every `play.error` code maps to a Help page: what happened, common causes, the 1–3 actions that actually apply.
2. **Stats overlay** for enthusiasts diagnosing their own device quirks.
3. **"Copy diagnostics"** one-tap from the error screen (SCR-010x) — text-only, for pasting into a forum/email.
4. **In-app troubleshooting flows** (later, R1.x candidate): guided checks per code (e.g., "this file's audio is DTS → here is what your device can do", doc 07 tier text).

---

## 6. Support workflow (no backend)

| Channel | Role |
|---|---|
| Store review responses | Public hygiene; convert repeated themes into doc 12 QA fixtures/regression tickets |
| Support email alias (store-listing contact — required by both stores) | Primary direct channel; templated intake based on §4 sections (attachments welcome only as user-generated report bundles) |
| Community space (decide at launch: forum vs. discussions) | Peer help; official staff only to route |
| Beta track feedback (closed testing cohort — also serves the Play 12-tester requirement, doc 10) | Highest-signal channel pre-launch |

**Support rules:** never ask for unpaid-user media uploads (D6); reproduction requests specify *attributes*; every confirmed defect gets a fixture + regression case (doc 12 §10 regression law). No SLA promises publicly in R1; internal response target 72 h `[T]` during rollout watch (doc 12).

**Feedback → roadmap loop:** themes are logged against KPI/feedback buckets (doc 03 KPIs, doc 16 prioritization); severity from doc 12 §10; privacy-sensitive reports are deleted after triage per the retention rule.

---

## 7. Blind spots and compensation (stated honestly)

| Blind spot (no telemetry) | Compensation |
|---|---|
| Crash rate/clustering unknown between store vitals snapshots | Play vitals (crash/ANR) as coarse aggregate; beta cohort reports; local crash prompt opt-in exports |
| Device-mix unknown | Doc 12 device classes guide priorities; store vitals device breakdown (aggregated by Google) |
| Feature usage unknown | Deliberate: we optimize for stated JTBDs (doc 03), not observed funnels; usability studies at build phase (optional, consent-based, out of product) |
| Failure reproduction relies on user reports | Error taxonomy precision (codes are specific), report bundle sufficiency (EV schema designed for reproduction without content) |

This is a *choice* consistent with the trust positioning (DEC-004/DEC-008) with real costs — documented so nobody later pretends we "just forgot" telemetry.

---

## 8. Store-facing requirements served by this doc

- **Play:** data-safety form consistency (doc 10 §5: collect-nothing claim) — the share-sheet export is a user action, not collection by us; vitals remain Play's own aggregate; store listing support contact.
- **App store listings:** support URL/email; privacy policy page hosted where? (R1 needs a privacy policy URL — static page, no analytics on it; owner task, doc 15).

---

## 9. Preconditions for EVER adding automatic diagnostics (future releases only)

None of these exist in R1. Any future motion toward opt-in crash upload requires, in order:
1. New ADR (adds `INTERNET` permission — reverses ADR-011's posture) + doc 10 update (inventory, threat model, data-safety forms).
2. Consent UX spec with preview + revoke + delete, default OFF, per-upload preview (never background blanket consent).
3. Statute review (doc 10 §6.2) for the launch regions.
4. Product decision that the benefit justifies the trust cost (explicitly *not assumed*; default remains none).

Until all four exist, this section is a no-op and §1 D2 stands.

---

## 10. Open questions

| # | Question | Owner |
|---|---|---|
| Q14-1 | Community channel choice (forum vs. GitHub discussions) — affects moderation cost | 15/16 |
| Q14-2 | Privacy-policy static page hosting (domain, no-analytics commitment) | 15 |
| Q14-3 | Retention numbers §2 (`[T]`) — validate against real log volumes at build phase | 14 |
| Q14-4 | Whether the post-crash prompt should even ship in R1 (value vs. nag risk) | 14/06 |
| Q14-5 | Email alias staffing model | 16 |
