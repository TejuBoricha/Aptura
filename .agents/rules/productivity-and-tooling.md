# Tooling, Skills & Productivity Rule

This rule is a permanent project invariant. At all times, the developer must proactively discover, install, and leverage the most effective tools, skills, plugins, and knowledge bases to maximize engineering velocity, precision, and code quality.

---

## 1. Proactive Skill & Plugin Utilization
- **Always Check and Apply Relevant Skills:** Before implementing, auditing, or refactoring code in any technical domain, always identify and apply the corresponding specialized skill:
  - **Jetpack Compose UI & Rendering:** Use `jetpack-compose-performance` (phase deferral, stable keys, lambda modifiers, backwards writes prevention).
  - **Screen Layouts & Multi-Window:** Use `adaptive` (Material 3 window size classes, supporting pane patterns, foldables/split-screen).
  - **Android Platform & APIs:** Use `android-cli` (`android docs search/fetch`, `android info`, `android sdk`).
  - **Testing & Verification:** Use `testing-setup` and automated unit/instrumentation test suites.
- **Authoritative Docs First:** When dealing with platform contracts (Media3, MediaSessionService, Storage Access Framework, WindowInsets), query authoritative documentation via `android docs search` or official specs instead of guessing API behaviors.

---

## 2. Autonomous Verification & Zero Idle Loops
- **Proactive Validation:** Never deliver code without immediate verification. Run `./gradlew.bat test` or the appropriate test harness after every code modification.
- **Leverage Custom Toolchains:** Always use dedicated project automation under `tools/` (e.g. `tools/corpus/` for synthetic test media and `tools/ffmpeg/` for LGPL symbol auditing) to prevent repetitive manual work.
- **Continuous Tool Discovery:** When entering a new development domain (profiling, R8 shrinking, benchmark harnessing), check available tool catalogs (`android skills list`) and install relevant skills immediately.

---

## 3. High-Leverage Development Discipline
- **Tabular & Structured Figures:** Always use tabular digits (`tnum`) for changing values to eliminate layout jitter.
- **UDF & Architectural Purity:** Strictly separate UI, Presentation, Domain, and Data layers. Domain logic must remain 100% pure Kotlin.
- **Memory & Context Continuity:** Keep `.agents/MEMORY.md` updated after major milestones to preserve project state across session boundaries.
