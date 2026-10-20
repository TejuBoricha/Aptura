# Mandatory QA & Code Review Gate

This rule is a permanent project invariant. Every code modification, architectural milestone, and task delivery MUST undergo an explicit, comprehensive **QA Lead & Code Review Gate** before being committed or presented as complete.

---

## 1. The 4-Pillar Review Protocol

Before any commit, the AI Developer must evaluate the changes against the following 4 pillars and include a formal scorecard in the response:

### Pillar 1: Invariants & Compliance Audit
- [ ] **Zero Network (`DEC-012`):** Verify `AndroidManifest.xml` and codebase contain ZERO `android.permission.INTERNET`, socket connections, or HTTP clients.
- [ ] **Zero Telemetry SDKs (`ADR-007`):** Verify no third-party crash or analytics SDKs (Firebase, Sentry, Mixpanel, Datadog) exist. Diagnostics must be 100% local-only.
- [ ] **Storage Safety (`DEC-011`):** Scoped storage only. Verify `MANAGE_EXTERNAL_STORAGE` is NEVER declared or requested.
- [ ] **Licensing & Codec Boundaries (`DEC-009`, `ADR-010`):** All distributed code must be LGPL-clean. No bundled software video decoders.
- [ ] **Permission Minimalism (`FR-PLAT-002`, `W10`):** `POST_NOTIFICATIONS` must NEVER be requested for media playback notifications.

### Pillar 2: Boundary, Edge-Case & Error Taxonomy Audit
- [ ] **Adversarial & Corrupted Input Handling:** Ensure graceful degradation for 0-byte files, corrupted headers, missing metadata, and truncated streams.
- [ ] **Resource Leak Prevention:** Verify all `Cursor` instances, database connections, coroutine scopes, and `Player` sessions are properly closed/released.
- [ ] **Concurrency & Thread Safety:** Verify thread-safe collections, mutexes on shared buffers, and lifecycle cancellation handling.
- [ ] **Error Taxonomy Compliance (`spec/04 FR-PLAY-003`):** Every failure must map to a defined, honest error state rather than crashing or hanging on a spinner.

### Pillar 3: Performance, Resources & Taste Audit
- [ ] **Explicit Coroutine Dispatchers:** `Dispatchers.IO` for disk/database/MediaStore queries; `Dispatchers.Main` for UI state observations. Never block the Main thread.
- [ ] **Tabular Figures (`tnum`):** All dynamic numeric labels (playback timecodes, durations, bitrates, file sizes) MUST specify `fontFeatureSettings = "tnum"`.
- [ ] **Compose Anti-Slop:** Adhere to [DESIGN.md](file:///c:/Users/tejas/Desktop/Mobile%20App/DESIGN.md) tokens, luminance elevation over heavy shadows, and spring animations.
- [ ] **State Completeness:** Every UI component must natively support Loading/Buffering, Empty, and Error states.

### Pillar 4: QA Test Execution & Static Analysis Scorecard
- [ ] **Automated Tests:** All unit test suites executed and passing (100% green) via `./gradlew.bat test`.
- [ ] **Negative & Boundary Test Coverage:** Assertions must test failure paths, boundary thresholds, and edge cases, not just happy paths.
- [ ] **Static Analysis (Lint):** Run `./gradlew.bat lintDebug` to verify zero fatal errors, zero resource leaks, and zero security warnings.

### Pillar 5: Proactive Evolution & Competitive Gap Audit
- [ ] **Beyond "Does It Work?":** Never settle for an MVP. For every screen, component, and screenshot inspected, evaluate what features, settings, customization opportunities, and edge cases are missing.
- [ ] **Competitive Parity:** Benchmark against VLC, MX Player Pro, Next Player, and Just Player (equalizer presets, bass/virtualizer, software contrast/brightness/night mode, fine-grained delays, A-B repeat, frame stepping, 2x long-press).
- [ ] **Customizability:** Expose granular controls (sliders, fine steppers, presets) rather than hardcoded magic numbers.

---

## 2. The Anti-Confirmation-Bias Mandate (Preventing False "PASS" Verdicts)

A test suite that only asserts happy paths is a dangerous false positive. To prevent code from passing unit tests while harboring fatal bugs in production:

### The 3-Point Pre-Mortem Falsification Checklist
Before writing tests or claiming a feature is complete, the developer MUST explicitly challenge the implementation against three failure dimensions:
1. **Scale Falsification (The 10k Multiplier):**
   - *"What happens if this list contains 10,000 items instead of 2?"*
   - Never allow loops containing database queries, disk I/O, or IPC calls (N+1 query pattern).
   - Never pass unbounded lists into SQL `IN (...)` clauses; chunk into batches of ≤500 items to avoid SQLite's 999 parameter limit.
2. **Interruption & Asymmetry Falsification:**
   - *"What happens if an asynchronous event fires out of order, or an error occurs mid-stream?"*
   - Verify that subsequent lifecycle events (e.g. `ExoPlayer.STATE_IDLE`) do NOT clobber active error states.
   - Verify that hardware resources (`Player`, `Cursor`, coroutine scopes) are properly detached on `release()` or cancellation.
3. **Framework Contract Falsification:**
   - *"What platform behavior is being assumed that is unmocked or behaves differently on real Android?"*
   - Never assume Android framework classes (`Uri`, `Context`, `Build`) work on local JVM without explicit Robolectric or test mocks.
   - Validate platform exemptions (e.g. media session notification exemption) before designing UI prompts.

### Automated Structural Invariant Tests
Whenever an architectural or legal invariant is established, it MUST be locked by an automated test that inspects the source files directly (e.g. `ArchitectureDecouplingTest.kt`, `PlatformInvariantsTest.kt`). If an invariant is broken by any commit or dependency, the CI build MUST fail automatically.

---

## 3. Review Artifact Delivery
Every phase commit message and handoff summary must explicitly reference this QA & Code Review Gate.

