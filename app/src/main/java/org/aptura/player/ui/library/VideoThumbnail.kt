package org.aptura.player.ui.library

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aptura.player.core.media.ThumbnailBitmapCache
import org.aptura.player.core.media.ThumbnailRepository
import org.aptura.player.theme.ApturaColors
import kotlin.coroutines.CoroutineContext

/** Where thumbnails are read and decoded. Replaced in tests whose graphics layer cannot decode off the main thread. */
internal val LocalThumbnailDecodeContext = staticCompositionLocalOf<CoroutineContext> { Dispatchers.IO }

private const val THUMBNAIL_FADE_IN_MS = 120

/**
 * The cached frame for a video, generated on first use. While it loads (or when a frame cannot be read) a film glyph
 * on the elevated surface stands in, so an unreadable file never looks like a black hole in the grid.
 *
 * A picture that is already decoded in [ThumbnailBitmapCache] is drawn in the first frame, with no animation, so a grid
 * that returns to the screen does not flicker. A picture that has to be read first fades in instead of popping.
 */
@Composable
internal fun VideoThumbnail(card: VideoCardUiModel, modifier: Modifier = Modifier) =
    VideoThumbnail(card.uri, card.rawItem.dateModifiedSec, card.title, modifier)

@Composable
internal fun VideoThumbnail(uri: String, dateModifiedSec: Long, contentDescription: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val decodeContext = LocalThumbnailDecodeContext.current
    val key = remember(uri, dateModifiedSec) { ThumbnailRepository.getThumbnailFileName(uri, dateModifiedSec) }
    var bitmap by remember(key) { mutableStateOf(ThumbnailBitmapCache[key]) }
    // Fully visible at once when the picture was already in memory; otherwise it fades in when it arrives.
    val alpha = remember(key) { Animatable(if (bitmap != null) 1f else 0f) }

    LaunchedEffect(key) {
        if (bitmap != null) return@LaunchedEffect
        val decoded = withContext(decodeContext) {
            val file = ThumbnailRepository.getCachedThumbnailFile(context, uri, dateModifiedSec)
                ?: ThumbnailRepository.getOrCreateThumbnail(context, uri, dateModifiedSec)
            file?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
        }
        if (decoded != null) {
            ThumbnailBitmapCache.put(key, decoded)
            bitmap = decoded
            alpha.animateTo(1f, tween(THUMBNAIL_FADE_IN_MS))
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val shown = bitmap
        if (shown == null) {
            Icon(
                imageVector = Icons.Outlined.Movie,
                contentDescription = null,
                tint = ApturaColors.TextLowEmphasis,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Image(
                bitmap = shown,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha.value }
            )
        }
    }
}
