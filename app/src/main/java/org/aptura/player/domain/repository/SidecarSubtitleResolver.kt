package org.aptura.player.domain.repository

import org.aptura.player.domain.model.DomainExternalSubtitle

/**
 * Finds subtitle files that belong to a video (same-name `.srt`/`.vtt`/`.ass` next to it, in `subs/`, ...).
 * Pure domain contract: the Android implementation lives in the data layer.
 *
 * Implementations are called off the main thread. The first returned entry with `isDefault = true`
 * is auto-enabled; at most one entry may be a default.
 */
interface SidecarSubtitleResolver {
    suspend fun resolve(mediaUri: String): List<DomainExternalSubtitle>
}
