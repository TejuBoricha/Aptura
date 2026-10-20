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
- **Error Taxonomy:** Map errors to the standard Aptura error taxonomy (`spec/04 FR-PLAY-003`). Never swallow exceptions silently or let unhandled exceptions crash the media session.

## 3. Media3 & Playback State Orchestration
- Keep the `Media3` player instance bound to a foreground `MediaSessionService` (`spec/09 §2`).
- The player session state machine must follow the exact states: `IDLE`, `BUFFERING`, `READY`, `ENDED`, and `ERROR` (`spec/09 §4`).
- Support process death and lifecycle state restoration by saving playback position at least every 5 seconds or upon `onPause` (`TST-REST-01`, `spec/12A §3.3`).

## 4. Test-Driven Verification
- Every functional module MUST include automated unit tests (`src/test/`) or instrumented tests (`src/androidTest/`).
- Use Turbine for testing `StateFlow` streams.
- Seek latency and AV-sync benchmarks must measure real clock timestamps against [spec/12A](file:///c:/Users/tejas/Desktop/Mobile%20App/spec/12A_B0_INSTRUMENTATION.md) harness contracts.
