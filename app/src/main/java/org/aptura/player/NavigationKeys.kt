package org.aptura.player

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object MediaLibrary : NavKey
@Serializable data class PlayerKey(val videoUri: String) : NavKey

