# Inside Aptura

Aptura plays and organises videos already on your Android device. It is a personal library with precise playback controls. There is no streaming catalogue or account to sign into.

## Library and continuity

- **Local indexing:** MediaStore and user-selected SAF folder trees, including accessible SD-card and USB volumes. WorkManager reconciles the index when files change.
- **Browse your way:** grid or list, compact/comfortable/large item sizes, detail levels, natural title ordering, search and sorting. Layout adapts to phone, foldable and tablet windows.
- **Resume:** saved progress, Continue watching, Up Next and a reorderable queue. A video in Continue watching is the same file shown in Library, with the same title and thumbnail.
- **File actions:** rename, copy, move, share, open with another app, hide and restore. Selection enables batch operations. Where supported, Move to trash is the primary delete action; permanent deletion requires explicit confirmation.
- **Transfers:** named-file progress and cancellation, with journalled recovery for interrupted copy/move work. A denied source deletion after copying does not mean the originals disappeared.
- **Maintenance:** extra folders, rescan, thumbnail-cache cleanup, watch-history clearing and removal of missing-file records. Clearing a missing record does not delete an unavailable disk file.

## Playback

- Media3/ExoPlayer, using Android's available video decoders. Auto/Hardware/Software strategy is configurable globally or per video; Software means a decoder supplied by the device.
- Eight speeds from **0.25× to 2.0×**, plus hold-to-2× while playing.
- Double-tap seek; horizontal swipe seeking; vertical volume/brightness gestures; orientation lock.
- Picture-in-Picture where Android supports it, background listening and audio-only playback.
- Sleep timer with a fade, custom duration or end-of-video choice.
- A–B repeat, frame stepping and named bookmarks.
- Fit/fill/zoom and aspect choices, software picture adjustments and playback statistics.

## Sound and subtitles

- Audio track selection with language, channel layout and codec information.
- Five-band equalizer and presets, bass boost, virtualizer and dialogue boost. Effect availability and output depend on the device and audio route.
- Audio sync with explicit earlier/later language. Positive delay moves sound later; a late Bluetooth route needs an earlier/negative correction.
- Embedded tracks and imported `.srt`, `.vtt`, `.ass`, `.ssa`, `.ttml`, `.sub` and `.zip` subtitle files. Rendering support depends on the file's format and contents.
- Subtitle delay, tap-to-sync, size, colour and background style, with persistent defaults.
- Subtitle search is an explicit browser handoff. Aptura itself has no internet permission.

## Appearance and privacy

System, light and dark app themes; **Cyan, Blue, Violet, Amber, Rose and Gold** accents, all available to everyone. Playback and player sheets remain dark in every app theme.

No account, ads, analytics SDK or automatic diagnostic upload. Video access uses Android's scoped permissions and system folder picker. Optional screen security blocks screenshots and hides the Recents preview; it is not file encryption or a password-protected vault. Local diagnostics are redacted before a report is shared through Android's chooser. See [the privacy policy](PRIVACY.md).

## UI gallery

These links open larger native UI crops without marketing copy. Most are Compose renders with representative demo state; the player is an Android emulator capture. They demonstrate appearance, not physical-device audio, HDR or accessibility verification. Sample media illustrates personal files, not supplied entertainment content.

- [Phone library](assets/showcase/native-library.webp)
- [Tablet library](assets/showcase/native-tablet.webp)
- [Actual video player](assets/showcase/native-player.webp)
- [Audio controls](assets/showcase/native-audio.webp)
- [Subtitle preview](assets/showcase/native-captions.webp)
- [Loop, frame step and bookmarks](assets/showcase/native-precision.webp)
- [Appearance settings](assets/showcase/native-appearance.webp)

## Formats and boundaries

Device codecs determine which video and audio streams play. Aptura does not bundle a software video decoder or promise universal format support. The self-built LGPL audio extension supplies **ALAC, AC-3 and G.711 A-law/mu-law** when the device has no decoder of its own for them. Other formats, including E-AC-3, DTS and TrueHD, remain the device's responsibility. Settings includes a device-specific codec report.

No SMB/NAS/DLNA streaming, casting, cloud synchronisation or online metadata catalogue. Files and folders must be available through Android's local storage/provider access. There is no iOS build.

The [device checklist](DEVICE_CHECKLIST.md) and [active audit](audits/CURRENT_AUDIT.md) distinguish tested behaviour from remaining hardware work. Bluetooth, TalkBack, HDR output and recent physical-device regression checks remain part of release readiness.
