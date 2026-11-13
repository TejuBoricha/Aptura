package org.aptura.player.data.scanner

import org.aptura.player.data.database.MediaItemEntity

/**
 * True when the person has done anything to this row that a rescan must never throw away.
 *
 * Audit DATA-04: the scan worker used to treat a missing row as disposable when only three columns were empty (position,
 * completed, custom title). A hidden video, a chosen playback speed or decoder, an audio or subtitle delay, a resize
 * mode, or a last-played time did not count, so one scan that did not see the file (an ejected SD card, a revoked grant,
 * a MediaStore hiccup) deleted them. A hidden private video then came back visible.
 *
 * This must list every column in [MediaItemEntity.withUserDataFrom]; `MissingRowPurgeTest` fails when a new column is
 * added to the entity without being classified here or in the test's scanner-owned list.
 */
internal fun MediaItemEntity.hasUserData(): Boolean =
    playbackPositionMs > 0L ||
        isCompleted ||
        lastPlayedTimestamp > 0L ||
        resizeMode != DEFAULT_RESIZE_MODE ||
        playbackSpeed != DEFAULT_PLAYBACK_SPEED ||
        decoderOverridden ||
        decoderStrategy != 0 ||
        audioDelayOverridden ||
        audioDelayMs != 0L ||
        subtitleDelayMs != 0L ||
        !customTitle.isNullOrBlank() ||
        isHidden

/**
 * A row the worker may delete when the file is gone: an internal MediaStore row that nothing of the person's is attached
 * to. It is cheap to recreate, so deleting it only keeps the library tidy. Everything else is kept, shown as missing, so
 * the person's data comes back if the file does.
 */
internal fun isPurgeableMissingRow(item: MediaItemEntity, hasStoragePermission: Boolean): Boolean =
    item.isMissing &&
        hasStoragePermission &&
        item.uri.startsWith("content://media/") &&
        !item.hasUserData()

private const val DEFAULT_RESIZE_MODE = "FIT"
private const val DEFAULT_PLAYBACK_SPEED = 1.0f
