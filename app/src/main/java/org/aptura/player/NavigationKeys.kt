package org.aptura.player

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object MediaLibrary : NavKey
@Serializable data object Onboarding : NavKey
@Serializable data object Settings : NavKey
@Serializable data object OpenSourceLicenses : NavKey
@Serializable data class PlayerKey(val videoUri: String) : NavKey
@Serializable data class MediaDetailsKey(val videoUri: String) : NavKey

/**
 * The player route to open for the playback notification's tap, or null when nothing should be pushed (audit FUNC-05).
 *
 * The route is for the video that is loaded in the player, not one read from the notification, and it is only a way to
 * look at it: opening the route does not load or restart anything (see `PlayerViewModel.openRouteVideo`). Nothing is
 * pushed when nothing is loaded, or when the player is already the screen the person is looking at.
 */
internal fun playerKeyToShow(requested: Boolean, currentVideoUri: String?, topOfStack: NavKey?): PlayerKey? =
    if (requested && !currentVideoUri.isNullOrBlank() && topOfStack !is PlayerKey) PlayerKey(currentVideoUri) else null
