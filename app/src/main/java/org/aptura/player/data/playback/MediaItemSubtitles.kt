@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.MediaItem

/**
 * Returns a copy of this item that carries [configs] as its subtitles, and that ExoPlayer will actually rebuild.
 *
 * Why not just `buildUpon().setSubtitleConfigurations(...)`: when a playlist item is replaced, ExoPlayer first asks the
 * item's existing media source whether it can take the new item "in place" ([androidx.media3.exoplayer.source.MediaSource.canUpdateMediaItem]).
 * A progressive source compares only the URI, the image duration and the custom cache key, and a merging source (the
 * one that holds the video plus its side-loaded subtitles) asks its first source. A change to subtitles alone therefore
 * counted as "the same item": the item was swapped, no subtitle source was created, and an imported subtitle never
 * appeared (audit FUNC-02). The same held for sidecars baked into the next item in the queue.
 *
 * Giving every distinct set of subtitles its own custom cache key makes the two items differ, so ExoPlayer builds the
 * sources again. The app has no cache, so the key has no other effect. An item with no subtitles keeps no key.
 */
internal fun MediaItem.withSubtitleConfigurations(configs: List<MediaItem.SubtitleConfiguration>): MediaItem =
    buildUpon()
        .setSubtitleConfigurations(configs)
        .setCustomCacheKey(subtitleRebuildKey(configs))
        .build()

/** A stable key for a set of subtitles: the same subtitles give the same key, any difference gives another. */
internal fun subtitleRebuildKey(configs: List<MediaItem.SubtitleConfiguration>): String? =
    if (configs.isEmpty()) null
    else configs.joinToString(separator = "|", prefix = "aptura-subtitles:") {
        "${it.uri}#${it.label}#${it.language}#${it.mimeType}#${it.selectionFlags}"
    }
