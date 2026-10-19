# 12A — B0.0 Instrumentation Build Order

**Version:** 0.1 (draft — **nothing in this document exists yet: no fixture, no script, no harness**)
**Date:** 2026-10-19
**Status:** Draft, docs-only. This is the **build order for B0.0** — the instrumentation stage `08A` §2 named but left as one sentence per item. A sentence cannot be built; this document fixes, per artifact: what it contains, how it is identified, what it must produce, and how its output is adjudicated. **No commands, scripts or code appear here** — the generator scripts are authored at build time and recorded with each fixture (doc 12 §5); embedding unverified command lines would be exactly the invented-constant failure 17 §1.3 forbids.
**Companion status:** numbered `12A` following the `02A`/`08A`/`08B`/`11A` precedent — the master prompt's deliverable set (00–18) and doc 18's 24/24 audit are unchanged.
**Authority split:** doc 12 §5 (corpus composition rules) and §7 (harness ownership, "implementation owned here") are the plan; **12A is the build order under them**. Doc 07 owns tier definitions and cell semantics; doc 08 + `08A` own spike hypotheses and adjudication; doc 13 owns measurement methodology, metric IDs and statistics; `08B` §3.4.1 owns the device-side intake record. **Where this document and doc 12 disagree, doc 12 wins and this document is corrected.**
**Consumes:** 07 (§2 tiers; §5–§9 cells), 12 (§5 corpora, §7 programs, §10 regression law), 13 (MB-03/04/07/08/10/11/12; M1–M10; §5 statistics), 04 (FR-PLAY-004 taxonomy, FR-PLAY-012/013, FR-SUB-001/005, FR-AUD-002/007), 08 + `08A` (per-spike fixtures), 17 (change process).
**Produces:** the concrete artifact set every spike's *Fixtures* line, and every TST suite's corpus reference, resolves to.

---

## 1. Why this exists, and the rules it is built on

`08A` §2 found that B0 has an unlisted stage zero and that "the corpus and the seek harness are themselves multi-week engineering artifacts `[E]`". B0.0 is also the **only part of B0 that can start today**: it needs a build machine, not devices, not owner answers, not counsel. This document is its work order.

Rules the whole document obeys:

1. **Counts are `[T]` targets; composition rules are binding.** When time forces a trade, drop count first, never a rule (doc 12 §1 spirit).
2. **No third-party copyrighted content, ever** (doc 12 §5). Sources are our own recordings and CC0/CC-BY media only; every fixture carries a license note.
3. **Checksum-pinned before any run** (`08A` S1). A fixture that changed after a spike ran invalidates that spike, not just the fixture.
4. **Fixtures are immutable once released to a spike.** Corrections get a new ID; the old ID is retired, never edited.
5. **The adversarial and D-row corpora exist to fail honestly.** Their expected outcome is the *right error or the right notice* (FR-PLAY-004 taxonomy) in bounded time — not playback.

---

## 2. Fixture corpus v1

### 2.1 Identification, manifest, storage

**Naming:** `<corpus>_<cell>_<nn>_<variant>.<ext>` — e.g. `a_A05_02_faststart.mkv`, `adv_media_07_truncated-mid.mkv`. Corpus prefixes: `a_` (A matrix), `b_` (B set), `cd_` (C/D samples), `adv_media_`, `adv_sub_`, `meta_`, `scale_`, `inst_` (instrumented seek/sync).

**Manifest:** one checked-in CSV + checksums, the corpus's index of record. Fields:

| Field | Content |
|---|---|
| `id` | Naming-scheme ID (unique, immutable) |
| `cell` | Doc 07 reference (container/video/audio/subtitle, with tier) |
| `properties` | Container · video codec/profile/bit depth/fps · audio codec/channels · subtitle format · duration · resolution |
| `duration_s`, `size_bytes`, `sha256` | Measured at build time |
| `source_type` | `own-recording` \| `CC0` \| `CC-BY` — nothing else |
| `source_ref`, `license_note` | Where it came from; the licence obligation (CC-BY attribution text) recorded verbatim |
| `gen_script_ref` | Path + commit of the generator script at build time; parameters recorded beside it |
| `pinned` | Date the checksum was frozen |
| `notes` | What the fixture is *for* when it is not obvious (e.g. "bait" cases) |

**Storage and governance:** build-phase repo, LFS or equivalent; CI verifies checksums; **no personal media in CI, ever** (doc 10); growth driven by the regression law (doc 12 §10) — every fixed adversarial bug becomes a permanent fixture.

### 2.2 The A-cell matrix — validated core (≥3 files per cell, doc 12 §5)

Combination cells derived from doc 07's A-tier rows (§5 containers, §6 video, §7 audio, §8 subtitles). A cell is a *realistic file shape*, not a cross-product entry.

| Cell | Shape | Feeds |
|---|---|---|
| A01 | MP4 / H.264 High 8-bit / AAC-LC, 1080p — plain + fast-start variants | RQ-01, TST-CMPT baseline, NFR-PERF-003 |
| A02 | MP4 / H.264 / AAC-LC, **fragmented MP4** | RQ-01, seek (sidx behaviour) |
| A03 | MOV / H.264 / **HE-AAC**, phone-capture shape, rotation metadata present | RQ-01, FR-PLAY-010 |
| A04 | MKV / H.264 High 8-bit / AAC-LC | RQ-01, the library workhorse |
| A05 | MKV / HEVC Main 8-bit / AAC-LC | RQ-01, CDD-floor pairing |
| A06 | MKV / HEVC Main 8-bit / **E-AC-3** | RQ-04, FR-AUD-007 passthrough path |
| A07 | MKV / HEVC Main 8-bit / **AC-3** | RQ-04, add-back candidate cell (doc 11) |
| A08 | MKV / H.264 / **MP3** | RQ-01, legacy pairing |
| A09 | MKV / H.264 / **FLAC** | RQ-01, lossless path |
| A10 | WebM / VP9 8-bit / Opus | RQ-01, web-ecosystem cell |
| A11 | MKV / VP9 8-bit / **Vorbis** | RQ-01 |
| A12 | MP4 / H.264 / AAC-LC + **SRT sidecar** (naming variants per FR-SUB-001) | RQ-06, TST-SUB |
| A13 | MKV / HEVC Main 8-bit + **ASS embedded** | **RQ-06** (display A / fidelity B) |
| A14 | MP4 / H.264 + **WebVTT (wvtt) embedded** | RQ-06, TST-SUB |
| A15 | MKV / H.264 + **TTML sidecar** | RQ-06, TST-SUB |

**Cross-cutting rules (binding):**
- **FPS coverage across the matrix: 23.976 / 24 / 25 / 30 / 50 / 60** `[T]` — this is what FR-PLAY-012's AC (`TST-RATE-01…04`) plays back.
- Durations 10–120 s, except: one **≥60 min** A-cell file `[T]` for seek/resume realism; one **4K HEVC** file for MB-10/TST-4K (class-qualified, not part of A-cell commitments).
- Keyframe-interval variation: at least one cell includes a **long-GOP** file and one a dense-keyframe file (MB-07's keyframe/non-keyframe split needs both).
- Every file has auditable clean audio (the AV-sync rig must be able to lock onto it).
- Minimum total: **15 cells × ≥3 = ≥45 files** `[T]`.

### 2.3 The B-cell set — expected, degrade honestly (2–3 files each)

| Cell | Shape | Feeds |
|---|---|---|
| B01–B02 | MPEG-TS / H.264+AAC-LC; TS / HEVC+AAC-LC | RQ-01 B-tier behaviour, TST-STALL |
| B03–B04 | 3GP / H.263 / AMR-NB; 3GP / H.264 / AMR-WB | Legacy phone-capture path |
| B05 | MP4 + **HEVC Main10**: HDR10, HLG and SDR-10-bit variants | **RQ-03**, FR-PLAY-014, TST-HDR |
| B06 | WebM / **VP9 Profile 2** 10-bit | RQ-03 |
| B07–B08 | MP4 / **AV1 Main** 8-bit and 10-bit | RQ-03 (API-14+ vs older fleet) |
| B09 | Audio-only set: WAV/PCM, M4A/AAC, MP3, FLAC, OGG/Opus — *in video containers* | doc 07 §5 B-cell, player-UI path |
| B10 | MKV with **chapters + embedded cover art** | Container features (MKV B) |
| B11 | TS with embedded **CEA-608/708** | TST-SUB, broadcast-capture path |
| B12 | **VFR** screen-recording capture | FR-PLAY-008, TST-VFR |
| B13 | MKV / HEVC with **P3 wide-color** signalling | doc 07 §9 B-cell |
| B14 | MKV / HEVC / E-AC-3 **5.1 layout** | FR-AUD-002 channel handling |

Minimum total: **≥28 files** `[T]`.

### 2.4 C/D sample set — the honest-error corpus

One file per row (pair where noted). **Their expected outcome is the correct taxonomy error or notice in bounded time — never a spinner, never a crash.** D rows prove the error path *exists*; they are not playback attempts.

- **C rows:** MPEG-2 in MPEG-PS (`.mpg`) · Xvid/ASP AVI · H.264 FLV · Theora OGV · VC-1/WMV3 (`.wmv`) · interlaced MPEG-2 TS · PGS in MKV · VobSub IDX+SUB (pair) · SMI · MPL2 · Teletext TS · HDR10+ MKV · Dolby Vision P8 MP4 · Dolby Vision P7 MP4 (fallback path documented).
- **D rows:** WTV · RM/RMVB · MXF · SBS-3D MKV · ATRAC audio · DSD audio.
- **X cells (doc 07 §10): no fixtures at all** — bundled decoders and ProRes are never enabled, so there is nothing to test; their only trace is copy discipline (doc 07 §13).

Minimum total: **≈20 files** `[T]`.

### 2.5 Adversarial media (`adv_media_*`; doc 12 §5)

Truncated header · truncated mid-packet · zero-byte · 1-byte · absurd declared duration · backwards timestamps · packet storm (peak-bitrate spike) · max-track-count file (≥8 audio, ≥8 subtitle tracks) · fMP4 missing index · MKV with unknown EBML elements · corrupt CRC · video track with 0×0 dimensions · negative timestamps · duplicate track IDs · declared-duration/file-length mismatch.

Expected behaviour for every item: **classification + clean teardown, bounded** — asserted by RQ-10's diagnosability table.

Minimum total: **≥15 files** `[T]`. Every fixed adversarial bug becomes a permanent fixture (regression law).

### 2.6 Subtitle corpus (`adv_sub_*`, doc 12 §5)

| Group | Coverage |
|---|---|
| Encodings | UTF-8 · UTF-8-BOM · UTF-16 LE/BE ±BOM · UTF-32 ±BOM · Windows-125x family · Shift-JIS · GB-class · KOI-class |
| Scripts | Latin · RTL (Arabic, Hebrew) · Devanagari · Thai · CJK |
| ASS tiers | Simple · positioned · karaoke (`\k`) · heavy styling/fonts — the RQ-06 fidelity ladder |
| **Bait** | Wrong-charset files that only look right after re-encoding — they exist to prove the FR-SUB-005 re-encoding UI |
| Sidecar naming | `basename.srt`, `basename.en.srt`, `basename.forced.srt`, `subs/` folder form, mismatched-case basename (FR-SUB-001 discovery) |
| Adversarial | 10⁵-cue file · overlapping cues · zero-length cues · malformed tags · BOM storms · mixed encodings in one file · cue at t=0 · cue beyond media duration · negative timing · 10k-char single line |

Minimum total: **≥30 files** `[T]`. Cap-and-degrade is asserted; never OOM, never hang (doc 12 §5).

### 2.7 Metadata bombs (`meta_*`)

Multi-MB title/comment fields · oversized embedded artwork (tens of MB) · 10k-character filename · deep path · Unicode confusable filename · invalid-UTF-8 filename bytes. Parsing bounded; UI never drops into jank (doc 12 §5). Minimum **≥6 items** `[T]`.

### 2.8 Scale libraries (`scale_*`)

**1k / 10k / 50k items** `[T]` (doc 12 §7.2). Construction rule: N distinct tiny masters cloned to count, **each clone carrying unique metadata** (so enumeration and extraction are realistic, not a cache hit); plus a **mixed-size 10k** variant (long tail of larger files) for thumbnail stress, and one **SD-resident** placement for the slow-storage variant (MB-04 makes it mandatory). Disk budget is a build-time measurement, not a guess — record it in the manifest `[T]`.

Feeds: MB-03 scan/scroll, MB-04 throughput, MB-11 memory, TST-LIBV budgets, the "no phantom-dataset" assertion, RQ-05/RQ-11.

### 2.9 Instrumented fixtures (`inst_*`) — seek, sync, rate

- **Frame-tagged video:** selected A-cell shapes re-rendered with **burned-in timecode/frame counter** (doc 12 §7.1's "frame-tagged video") so the capture rig can verify the frame actually on screen, not just the player's claim.
- **Pinned seek positions** per instrumented file: **≥8 positions** `[T]` — keyframe and non-keyframe, near-start / mid / near-end (MB-07's split).
- **Impulse A/V fixtures:** visual flash + audio click pairs on a fixed cadence for the sync rig.
- The ≥60 min file from §2.2 doubles as the endurance/seek-realism fixture.

---

## 3. Harness specifications

Each harness is **owned by doc 12 (implementation); methodology owned by doc 13**. Below is the *contract*: what goes in, what comes out, and how the output is adjudicated. Field names are the record shape, not a frozen schema — the implementer may rename, but not drop, a field.

### 3.1 Seek & first-frame harness (TST-SEEK · MB-07 · NFR-PERF-003/004)

**In:** fixture × pinned positions (§2.9) × device × build; cold and warm process states, N≥5 runs per cell (doc 13 M5).
**Out (per run):** `fixture_id · device_record_id · build_id · position_requested_us · position_landed_us · landed_on_keyframe (bool) · seek_latency_ms · first_frame_latency_ms · decode_path {hw, sw-platform, mixed} · dropped_frames_during_landing · render_note · thermal_state · timestamp`.
**Ground truth:** the frame-tagged capture (§2.9) — "first frame" means the rig saw the requested timecode, not that the player reported ready.
**Adjudication:** doc 08 RQ-01/RQ-03 pass/fail; NFR-PERF-003/004 targets; doc 07 keyframe-related tier notes. Raw captures retained (`08A` S4).

### 3.2 AV-sync harness (TST-SYNC · MB-08)

**In:** impulse A/V fixtures; ≥30 min sessions `[T]` (MB-08); the screen/audio capture rig — **acquisition vs rental is doc 13 Q13-5 and is not resolved here**; the harness spec must not assume which it is.
**Out:** drift trajectory (offset vs time), **p95 offset**, max |offset|, sample cadence, at start / mid / end; per device class and per container/codec cell.
**Adjudication:** doc 13 MB-08 ("internal + possible claim, bounded wording"); any claim requires the doc 13 statistical bar first.

### 3.3 State-restoration matrix (TST-REST)

The ≥12 kill points (doc 12 §7.3) with **defined expected restore states** — a missing expectation is a harness defect, not a test result:

| # | Kill moment | Expected restore state |
|---|---|---|
| 1–3 | Mid-scan (enumerate / extract / thumbnail) | Scan resumes **incrementally from journal**; no full restart; progress and cancel semantics intact |
| 4–6 | Mid-playback (internal / SD / SAF source) | Position restored within the doc 05 resume tolerance, or honest restart with notice — **never silently from zero** |
| 7 | Mid-thumbnail queue (large backlog) | Queue resumes; no duplicated or orphaned thumb jobs |
| 8–9 | Mid-migration (data-model/schema step) | Migration completes atomically on next launch; no partial store |
| 10 | Mid-playlist edit (reorder while persisting) | Last consistent edit survives; queue/play-mode intact |
| 11 | Mid-library-write (tag/artwork cache) | Cache self-heals; no corrupt entry presented |
| 12 | Immediately after a completed action | **Nothing to restore** — must not falsely "resume" |

**Out (per point):** `killpoint_id · trigger_method · operation_time · relaunch_time · survives (bool) · restored_state · honest_notice_shown (bool) · journal_consistent (bool)`.
**Adjudication:** RQ-07; "resume from zero without telling the user" is a failure (doc 12 §7.3).

### 3.4 Scan-at-scale harness (TST-LIBV · MB-03/04)

**In:** scale sets (§2.8); screen-on; battery-saver as a **separate** scenario (MB-04); SD variant mandatory.
**Out (per stage):** `enumerate_ms · extract_ms · thumbnail_ms · items_per_stage · ui_frame_budget_p95 (TST-LIBV) · cancel_latency_ms · phantom_dataset_check (bool) · incremental_streaming_check (bool)`.
**Adjudication:** MB-03/04/11; RQ-11; NFR-PERF-001/005/007.

**Capture retention (all harnesses):** raw captures retained per `08A` S4; every artifact includes the device lab-matrix record ID (`08B` §3.4.1) and the fixture checksum current at run time.

---

## 4. B0.0 done criteria

B0.0 has two halves, because one half does not need hardware:

**B0.0-a — corpus (build machine only; can finish with no devices, no answers):**
1. Every ≥ minimum count met, or the shortfall recorded with the reason;
2. Checksums pinned; licence manifest complete; **no source outside own-recording/CC0/CC-BY**;
3. Every artifact traceable to at least one RQ/TST/MB ID (§5);
4. Adversarial and D-row expected behaviours written down *before* the first run.

**B0.0-b — harness readiness (needs the first lab device):**
5. Each harness produces its §3 artifact on a reference device, with fixture checksums and device-record IDs populated;
6. One **dry run** of each harness executed and discarded (doc 13 M4 warm-up convention) — a harness that has never run is not evidence that it works.

**Explicitly not B0.0:** no spike verdicts, no tier promotions, no claims — those are the spikes' jobs (`08A` S10).

---

## 5. Artifact → consumer map

| Artifact | Spikes | Suites / metrics |
|---|---|---|
| A-cell matrix (§2.2) | RQ-01, RQ-03, RQ-04 | TST-CMPT, MB-03/09/10/12, TST-RATE-01…04, TST-HDR |
| B set (§2.3) | RQ-01, RQ-03, RQ-04 | TST-CMPT, TST-STALL, TST-VFR |
| C/D set (§2.4) | RQ-01 | TST-CMPT (error-path half), doc 07 §13 copy audit |
| Adversarial media (§2.5) | RQ-01, RQ-10 | TST-DCRASH, stability |
| Subtitle corpus (§2.6) | RQ-06 | TST-SUB (31+ cases) |
| Metadata bombs (§2.7) | RQ-01, RQ-05 | TST-LIBV, stability |
| Scale sets (§2.8) | RQ-05, RQ-11 | TST-LIBV, MB-03/04/11, RQ-07 kill points 1–3 |
| Instrumented fixtures (§2.9) | RQ-01, RQ-03, RQ-12 | TST-SEEK/SYNC/RATE, MB-07/08 |
| Harness contracts (§3) | RQ-01, 03, 05, 07, 11, 12 | MB-03/04/07/08/11/12 |

---

## 6. Boundaries

- **Nothing here has been built, run, or measured.** No fixture exists, no script exists, no harness exists; every count is an `[T]` target.
- **No code, no commands.** Generation is specified by *properties*; scripts are authored and recorded at build time (doc 12 §5). Unverified command lines were deliberately not embedded.
- **No device claims.** B0.0-a is build-machine work; B0.0-b explicitly waits for the lab (`08B` §3).
- **Corpus v1 is not corpus final.** It grows by the regression law (doc 12 §10); v1's job is to be *sufficient to start the spike program honestly*, not complete.
- **Where doc 12 §5/§7 and this document disagree, doc 12 wins** and this document is corrected.

---

*End of 12A v0.1. Next revision triggers: first corpus build (record actual counts/sizes), doc 12 §5 amendments, any spike finding that a fixture class was missing — and the same applies to every harness contract above.*
