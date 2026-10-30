package org.aptura.player.ui.library

import android.graphics.BitmapFactory
import android.os.Looper
import android.os.Process
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.aptura.player.core.media.ThumbnailBitmapCache
import org.aptura.player.core.media.ThumbnailRepository
import org.aptura.player.theme.ApturaColors
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

/**
 * Where pictures are read, decoded and made, and how much of that may happen at once. Reading, decoding and above all
 * making a picture block their thread and take the processor from the screen; a fast scroll through hundreds of videos
 * used to ask for dozens at the same moment (30 at once in a measured lap). Three at a time keep the screen smooth, and
 * the work that is still waiting is dropped, without ever starting, when its cell has scrolled away.
 */
internal object ThumbnailWork {
    const val PARALLEL = 3

    @OptIn(ExperimentalCoroutinesApi::class)
    val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(PARALLEL)
}

/** Where thumbnails are read and decoded. Replaced in tests whose graphics layer cannot decode off the main thread. */
internal val LocalThumbnailDecodeContext = staticCompositionLocalOf<CoroutineContext> { ThumbnailWork.dispatcher }

private const val THUMBNAIL_FADE_IN_MS = 120

/** A short pause after the scrolling slows down, before the pictures start, so a stop-and-go scroll does not start and stop work. */
private const val THUMBNAIL_SETTLE_MS = 60L

/** Faster than this many cells per second is a fling: nobody can see the pictures go by, so none are started. */
private const val FLING_CELLS_PER_SECOND = 12f

/** How long the scrolling must have been slower than a fling before the pictures start. */
private const val FLING_RELEASE_MS = 150L

/**
 * True while the grid is being flung. Pictures are not started during a fling (a cell is composed for the whole time a
 * few rows either side of the screen are, which is far longer than it is seen, so a delay alone does not help: 106 pieces
 * of work started in a measured hard fling). Everything on screen when it slows down gets its picture then.
 */
internal val LocalThumbnailsPaused = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(false) }

@Composable
internal fun rememberFlingGate(state: LazyGridState): State<Boolean> {
    val paused = remember(state) { mutableStateOf(false) }
    LaunchedEffect(state) {
        var lastIndex = state.firstVisibleItemIndex
        var lastNanos = Long.MIN_VALUE
        snapshotFlow { state.firstVisibleItemIndex }.collectLatest { index ->
            val now = withFrameNanos { it }
            if (lastNanos != Long.MIN_VALUE && now > lastNanos) {
                val cellsPerSecond = abs(index - lastIndex) * 1_000_000_000f / (now - lastNanos)
                paused.value = cellsPerSecond > FLING_CELLS_PER_SECOND
            }
            lastIndex = index
            lastNanos = now
            // No further move for this long: the fling is over (a new move cancels this wait).
            delay(FLING_RELEASE_MS)
            paused.value = false
        }
    }
    return paused
}

/** Runs [block] at background priority when it is on a worker thread, so the thread that draws the screen is served first. */
private inline fun <T> behindTheScreen(block: () -> T): T {
    if (Looper.myLooper() == Looper.getMainLooper()) return block()
    val before = Process.getThreadPriority(Process.myTid())
    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
    return try {
        block()
    } finally {
        Process.setThreadPriority(before)
    }
}

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
    val paused = LocalThumbnailsPaused.current
    val key = remember(uri, dateModifiedSec) { ThumbnailRepository.getThumbnailFileName(uri, dateModifiedSec) }
    var bitmap by remember(key) { mutableStateOf(ThumbnailBitmapCache[key]) }
    // Fully visible at once when the picture was already in memory; otherwise it fades in when it arrives.
    val alpha = remember(key) { Animatable(if (bitmap != null) 1f else 0f) }

    LaunchedEffect(key) {
        if (bitmap != null) return@LaunchedEffect
        snapshotFlow { paused.value }.first { fling -> !fling }
        delay(THUMBNAIL_SETTLE_MS)
        val worker = decodeContext as? CoroutineDispatcher ?: ThumbnailWork.dispatcher
        val decoded = withContext(decodeContext) {
            behindTheScreen {
                val file = ThumbnailRepository.getCachedThumbnailFile(context, uri, dateModifiedSec)
                    ?: ThumbnailRepository.getOrCreateThumbnail(context, uri, dateModifiedSec, worker)
                file?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
            }
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
