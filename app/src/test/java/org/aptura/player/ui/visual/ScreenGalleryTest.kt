package org.aptura.player.ui.visual

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.aptura.player.core.media.ThumbnailRepository
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.library.MediaFormatters
import org.aptura.player.ui.library.MediaLibraryUiState
import org.aptura.player.ui.library.VideoCardUiModel
import org.junit.Assume.assumeTrue
import org.junit.Rule
import java.io.File

/**
 * Base for the screen gallery: renders real screens with representative data and writes PNGs, so a design review can
 * look at every surface without a device. It is a review tool, not a regression test: it runs only when
 * `APTURA_GALLERY_DIR` names an output directory, e.g.
 *
 *     APTURA_GALLERY_DIR=/tmp/gallery ./gradlew :app:testDebugUnitTest --tests '*Gallery*'
 *
 * Sheets and dialogs are separate windows, so every Compose root is captured and layered over the screen beneath it.
 */
abstract class GalleryBase {

    @get:Rule
    val composeRule = createComposeRule()

    private val outputDir: File? = System.getenv("APTURA_GALLERY_DIR")?.let(::File)

    protected fun requireGallery() {
        assumeTrue("set APTURA_GALLERY_DIR to render the gallery", outputDir != null)
    }

    protected fun themed(mode: ThemeMode, accent: AccentColor = AccentColor.CYAN, content: @Composable () -> Unit) {
        requireGallery()
        // Robolectric's native graphics aborts the JVM when a bitmap is decoded on a worker thread, so Coil does all of
        // its fetching and decoding on the main looper here.
        coil3.SingletonImageLoader.setUnsafe { ctx ->
            coil3.ImageLoader.Builder(ctx)
                .coroutineContext(kotlinx.coroutines.Dispatchers.Main.immediate)
                .fetcherCoroutineContext(kotlinx.coroutines.Dispatchers.Main.immediate)
                .decoderCoroutineContext(kotlinx.coroutines.Dispatchers.Main.immediate)
                .build()
        }
        seedThumbnails()
        composeRule.setContent {
            ApturaTheme(themeMode = mode, accentColor = accent) { content() }
        }
        settle()
    }

    protected fun settle() {
        // Coil decodes off the main thread; give it a few turns of the looper before the pixels are read.
        repeat(8) {
            composeRule.waitForIdle()
            Thread.sleep(60)
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
    }

    protected fun shoot(name: String) {
        val dir = outputDir ?: return
        dir.mkdirs()
        val views = composeRule.onAllNodes(isRoot()).fetchSemanticsNodes()
            .mapNotNull { (it.root as? ViewRootForTest)?.view }
            .distinct()
            .filter { it.width > 0 && it.height > 0 }
        require(views.isNotEmpty()) { "nothing to capture for $name" }
        val base = views.maxBy { it.width * it.height }
        val bitmap = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        base.draw(canvas)
        views.filter { it !== base }.forEach { window ->
            val fullScreen = window.width >= base.width && window.height >= base.height * 9 / 10
            if (!fullScreen) {
                // A dialog window: dim the screen as Android does, then centre the dialog.
                canvas.drawColor(0x99000000.toInt())
                canvas.save()
                canvas.translate(
                    ((base.width - window.width) / 2).toFloat(),
                    ((base.height - window.height) / 2).toFloat()
                )
                window.draw(canvas)
                canvas.restore()
            } else {
                window.draw(canvas)
            }
        }
        writeImage(bitmap, File(dir, "$name.png"), "png")
    }

    /** Encodes on the JVM: Robolectric's native Bitmap.compress aborts the test process. */
    private fun writeImage(bitmap: Bitmap, file: File, format: String) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val image = java.awt.image.BufferedImage(bitmap.width, bitmap.height, java.awt.image.BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, bitmap.width, bitmap.height, pixels, 0, bitmap.width)
        file.parentFile?.mkdirs()
        javax.imageio.ImageIO.write(image, format, file)
    }

    // ---- Representative data ---------------------------------------------------------------------------------

    protected val sampleItems: List<DomainMediaItem> by lazy {
        val now = 1_760_000_000L
        listOf(
            sample("Blade Runner 2049.mkv", 9_840_000, 4_200_000_000, "Movies", 2_100_000, 3840, 2160, now),
            sample("The Grand Budapest Hotel.mp4", 5_940_000, 1_800_000_000, "Movies", 0, 1920, 1080, now - 10),
            sample("Severance S02E03.mkv", 3_120_000, 820_000_000, "Series", 1_400_000, 1920, 1080, now - 20),
            sample("Severance S02E04.mkv", 2_980_000, 790_000_000, "Series", 0, 1920, 1080, now - 30),
            sample("Planet Earth III - Coasts.mp4", 3_300_000, 2_300_000_000, "Documentaries", 0, 3840, 2160, now - 40),
            sample("Family trip – Lisbon 2024.mp4", 412_000, 310_000_000, "Camera", 0, 1920, 1080, now - 50),
            sample("Interview_raw_take_final_v2_really_final_export.mov", 1_250_000, 1_100_000_000, "Camera", 0, 3840, 2160, now - 60),
            sample("Arrival.mkv", 6_960_000, 3_100_000_000, "Movies", 6_500_000, 1920, 800, now - 70),
        )
    }

    private fun sample(
        file: String,
        durationMs: Long,
        size: Long,
        folder: String,
        position: Long,
        w: Int,
        h: Int,
        modified: Long
    ) = DomainMediaItem(
        uri = "content://media/external/video/media/${file.hashCode().toUInt()}",
        title = file,
        durationMs = durationMs,
        sizeBytes = size,
        mimeType = if (file.endsWith(".mkv")) "video/x-matroska" else "video/mp4",
        width = w,
        height = h,
        frameRate = 23.976f,
        audioTrackCount = 2,
        subtitleTrackCount = 3,
        dateAddedSec = modified,
        dateModifiedSec = modified,
        folderPath = "/storage/emulated/0/$folder",
        playbackPositionMs = position,
        lastPlayedTimestamp = if (position > 0) modified * 1000 else 0L
    )

    protected fun card(item: DomainMediaItem) = VideoCardUiModel(
        uri = item.uri,
        title = MediaFormatters.cleanTitle(item.title),
        durationFormatted = MediaFormatters.formatDuration(item.durationMs),
        fileSizeFormatted = MediaFormatters.formatFileSize(item.sizeBytes),
        folderName = item.folderPath.substringAfterLast('/'),
        progressFraction = MediaFormatters.calculateProgressFraction(item.playbackPositionMs, item.durationMs),
        isCompleted = item.isCompleted,
        isMissing = item.isMissing,
        rawItem = item
    )

    protected fun librarySuccess(
        items: List<DomainMediaItem> = sampleItems,
        selectedFolder: String? = null,
        query: String = "",
        continueWatching: Boolean = true,
        limited: Boolean = false
    ): MediaLibraryUiState.Success {
        val folders = sampleItems.map { it.folderPath }.distinct()
        val cards = items.map(::card)
        return MediaLibraryUiState.Success(
            videos = cards,
            folders = folders,
            selectedFolder = selectedFolder,
            searchQuery = query,
            isLimitedMode = limited,
            continueWatching = if (continueWatching) cards.filter { it.progressFraction > 0f } else emptyList(),
            libraryVideoCount = sampleItems.size,
            libraryDurationMs = sampleItems.sumOf { it.durationMs },
            librarySizeBytes = sampleItems.sumOf { it.sizeBytes },
            folderCounts = sampleItems.groupingBy { it.folderPath }.eachCount()
        )
    }

    /** Paints a cinematic stand-in frame for each sample, cached where the real thumbnail loader looks first. */
    private fun seedThumbnails() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val palettes = listOf(
            intArrayOf(0xFF0F2027.toInt(), 0xFFFF7E3D.toInt(), 0xFF2C5364.toInt()),
            intArrayOf(0xFFF4C4D3.toInt(), 0xFFB5446E.toInt(), 0xFF3B1F2B.toInt()),
            intArrayOf(0xFF0B3D2E.toInt(), 0xFFE8EDE6.toInt(), 0xFF1E5945.toInt()),
            intArrayOf(0xFF08263A.toInt(), 0xFF9CC7D8.toInt(), 0xFF0E4A63.toInt()),
            intArrayOf(0xFF1A3A5E.toInt(), 0xFF3FA7D6.toInt(), 0xFFF2E3B3.toInt()),
            intArrayOf(0xFFFFB347.toInt(), 0xFFFFCC80.toInt(), 0xFF5D4157.toInt()),
            intArrayOf(0xFF232526.toInt(), 0xFF414345.toInt(), 0xFFB0A090.toInt()),
            intArrayOf(0xFF141E30.toInt(), 0xFF6E7F99.toInt(), 0xFF243B55.toInt()),
        )
        sampleItems.forEachIndexed { index, item ->
            val file = ThumbnailRepository.getThumbnailFile(context, item.uri, item.dateModifiedSec)
            if (file.exists()) return@forEachIndexed
            val colors = palettes[index % palettes.size]
            val bmp = Bitmap.createBitmap(480, 270, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = LinearGradient(0f, 0f, 480f, 270f, colors, null, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, 480f, 270f, paint)
            paint.shader = RadialGradient(
                120f + index * 37 % 240, 90f, 180f,
                intArrayOf(0x66FFFFFF, 0x00FFFFFF), null, Shader.TileMode.CLAMP
            )
            c.drawCircle(120f + index * 37 % 240, 90f, 180f, paint)
            paint.shader = null
            paint.color = 0x55000000
            c.drawRect(0f, 190f + (index % 3) * 12, 480f, 270f, paint)
            writeImage(bmp, file, "jpg")
        }
        // Robolectric's native graphics caches its JNI classes on first use. If the first decode happens on Coil's
        // worker thread the lookup fails and the JVM aborts, so decode once here, on the test thread.
        sampleItems.firstOrNull()?.let {
            android.graphics.BitmapFactory.decodeFile(
                ThumbnailRepository.getThumbnailFile(context, it.uri, it.dateModifiedSec).path
            )
        }
    }
}
