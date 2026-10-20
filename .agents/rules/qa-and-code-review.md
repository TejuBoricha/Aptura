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

---

## 2. Review Artifact Delivery
Every phase commit message and handoff summary must explicitly reference this QA & Code Review Gate.
