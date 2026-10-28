---
trigger: always_on
description: Architectural and legal invariants that MUST NEVER be violated in Aptura code or configuration.
---

# Aptura Architectural & Legal Invariants

These invariants are frozen and binding across all development phases. Any pull request, code edit, or dependency addition that violates these rules is an automatic build failure.

## 1. Network & Privacy Invariants
1. **ZERO `android.permission.INTERNET`:**
   - The `AndroidManifest.xml` must NEVER declare or request the `INTERNET` permission (`DEC-012`).
   - Aptura R1 is strictly offline-only. No third-party HTTP client library (OkHttp, Retrofit, Ktor client, Volley, Apache HttpClient, gRPC) may be in the release dependency graph or in the shipped code. Coil is used WITHOUT any `coil-network-*` artifact for this reason (Coil 2 bundled OkHttp, and R8 did not strip it).
   - Media3/ExoPlayer's own `HttpURLConnection`-based data source is part of the platform playback engine; it cannot open a connection without `INTERNET`, which is why invariant 1.1 matters.
2. **ZERO Third-Party Telemetry & Analytics SDKs:**
   - No Firebase Analytics, Crashlytics, Mixpanel, Datadog, Sentry, or third-party ad networks (`ADR-007`).
   - Diagnostics must be 100% local-only (`spec/14`), user-initiated, and redactable.
3. **No Gratuitous System Permissions:**
   - No `POST_NOTIFICATIONS` permission prompt is needed or allowed in R1 because media-session playback notifications are platform-exempt (`FR-PLAT-002`, `W10`). Do not request it.

### How these are enforced (do not weaken; extend)
These are checked against what actually ships — the **release** variant — not against our own source files:
- **Merged manifest** (`MergedManifestInvariantsTest`): the merged release manifest (ours plus every library's) must request exactly the approved permission set and expose exactly the approved components, each gated by its system permission. A permission or exported component added by a library fails the build until a person reviews it and adds it to `ManifestInvariants` with the reason.
- **Resolved dependency graph** (`DependencyInvariantsTest`): the release runtime classpath, including transitive libraries, must contain no telemetry/crash-reporting/advertising SDK and no HTTP client stack. `DependencyInvariants.approvedExceptions` is empty; an exception needs a reason and a bounded `onlyRequiredBy`.
- **Shipped code** (`verifyReleaseApk`, run automatically by `assembleRelease` and therefore by CI): R8's mapping file is read for classes that survived under forbidden packages (R8 renames classes, so searching the dex for library names is not enough), and the dex text is searched as a second net.
- Source-level guards (`PlatformInvariantsTest`) remain as fast checks on `AndroidManifest.xml`, but they are not the authority.

## 2. Storage & Permissions Invariants
1. **Never `MANAGE_EXTERNAL_STORAGE`:**
   - Under no circumstances will Aptura request `MANAGE_EXTERNAL_STORAGE` (All Files Access) (`DEC-011`).
   - Media access is governed exclusively through `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` (Android 13+) and `READ_EXTERNAL_STORAGE` (pre-13), supplemented by the Storage Access Framework (SAF) folder picker (`ACTION_OPEN_DOCUMENT_TREE`) for custom directories and SD cards.
2. **Read-Only Media Safety:**
   - Aptura never alters, mutates, or rewires user media files without explicit, platform-standard user confirmation (`DEC-001`).

## 3. Licensing & Codec Boundaries
1. **Zero GPL Linkage in Store Builds:**
   - All distributed store artifacts must be **LGPL-clean** (`DEC-009`).
   - FFmpeg audio extension must be compiled strictly under `--enable-lgpl --disable-gpl --disable-nonfree` and dynamically linked as shared libraries (`.so`) (`Brief L1`).
2. **No Bundled Software Video Decoders:**
   - Bundled software video decoders (e.g. bundled FFmpeg video decoders or libvpx video decoders) carry unresolved patent pool royalties (`ADR-010`). R1 relies exclusively on platform hardware/software decoders via `MediaCodec`.
   - Bundled custom decoding in R1 is strictly confined to the self-built **audio** extension (FLAC, Vorbis, Opus, etc.).

## 4. Execution Model Invariants
1. **Solo AI Developer Protocol:**
   - Execution is sequential by the AI Developer (Antigravity) pair-programming with the Product Owner.
   - Code changes must be accompanied by automated test assertions against frozen contracts ([spec/12A](../../spec/12A_B0_INSTRUMENTATION.md)).
