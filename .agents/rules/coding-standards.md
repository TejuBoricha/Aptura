---
trigger: always_on
description: Coding standards, Kotlin conventions, StateFlow architecture, and testing discipline for Aptura.
---

# Aptura Coding & Engineering Standards

## 1. Architecture: Unidirectional Data Flow (UDF)
- Follow standard modern Android clean architecture:
  - **`ui/`**: Jetpack Compose Composables, UI state definitions, and screen event handlers.
  - **`presentation/`**: `ViewModel` instances exposing immutable `StateFlow<UiState>` and accepting user intents.
  - **`domain/`**: Pure Kotlin use cases and business logic models (free of Android framework dependencies).
  - **`data/`**: Room database DAOs, MediaStore repositories, and Media3 player session controllers.
- **State Immutability:** UI state data classes MUST be `@Immutable` or `@Stable`. Never expose `MutableStateFlow` outside a `ViewModel`.

## 2. Kotlin & Coroutines Hygiene
- **Explicit Dispatchers:** Always inject or specify `CoroutineDispatcher` (e.g. `Dispatchers.IO` for MediaStore queries and Room writes, `Dispatchers.Main` for player state observations).
- **Structured Concurrency:** Use `viewModelScope` in ViewModels and lifecycle-aware collection in Compose (`collectAsStateWithLifecycle()`).
- **No GlobalScope:** Never use `GlobalScope` or unbounded thread spawning.
- **ZERO Polling Loops:** NEVER use `while (isActive) delay(...)` in ViewModels or Domain classes. State tracking and checkpointing MUST be pure event-driven (e.g., triggered on incoming Flow position deltas, pause, or lifecycle events). Polling loops deadlock virtual time in coroutine unit tests (`advanceUntilIdle`).
- **Error Taxonomy:** Map errors to the standard Aptura error taxonomy (`spec/04 FR-PLAY-003`). Never swallow exceptions silently or let unhandled exceptions crash the media session.

## 3. Media3 & Playback State Orchestration
- Keep the `Media3` player instance bound to a foreground `MediaSessionService` (`spec/09 §2`).
- The player session state machine must follow the exact states: `IDLE`, `BUFFERING`, `READY`, `ENDED`, and `ERROR` (`spec/09 §4`).
- Support process death and lifecycle state restoration by saving playback position at least every 5 seconds or upon `onPause` (`TST-REST-01`, `spec/12A §3.3`).
- **OptIn Discipline:** Media3 API calls require `@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)`. Do not confuse with `@kotlin.OptIn`, which causes Android Lint `UnsafeOptInUsageError` failures.

## 4. Test-Driven Verification & Quality Gates
- Every functional module MUST include automated unit tests (`src/test/`) or instrumented tests (`src/androidTest/`).
- **Fail-Fast Test Timeouts:** All unit tests must be bounded by explicit timeouts (configured in Gradle `testOptions.unitTests.all { timeout.set(Duration.ofSeconds(30)) }`).
- **No Protected API Access in Tests:** Never call framework `protected` methods (e.g., `ViewModel.onCleared()`) directly in unit tests. Use test doubles, cancellation of test scopes, or explicit `@VisibleForTesting fun clearForTesting()`.
- Use Turbine for testing `StateFlow` streams. Always cancel open turbine blocks before test exit.
- **Mandatory Quality Gate:** Never declare a feature complete or commit code without executing the atomic verification script (`tools/gate.ps1` or `./gradlew testDebugUnitTest lintDebug`).

