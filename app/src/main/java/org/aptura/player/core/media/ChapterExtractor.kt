@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.core.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.CommentFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aptura.player.domain.model.Chapter
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Extracts embedded chapter markers from media containers (MKV, MP4, MOV) and Media3 metadata (FEAT-10, FR-CTRL-011).
 *
 * Supports:
 * 1. Media3 ID3 ChapterFrame extraction from live playback streams.
 * 2. Matroska (MKV) EBML Chapters element parsing (EBML ID 0x1043A770).
 * 3. MP4 / MOV Apple Nero chapter list atom (moov.udta.chpl).
 */
object ChapterExtractor {

    private const val MAX_SCAN_BYTES = 16 * 1024 * 1024L // Scan up to 16 MB of header

    /**
     * Extracts chapters from a media URI asynchronously.
     */
    suspend fun extractChapters(context: Context, uri: Uri): List<Chapter> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                extractFromStream(BufferedInputStream(stream, 64 * 1024))
            } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Extracts chapters from an input stream by inspecting container headers.
     */
    fun extractFromStream(stream: InputStream): List<Chapter> {
        val header = ByteArray(8)
        stream.mark(16)
        val read = stream.read(header)
        stream.reset()
        if (read < 4) return emptyList()

        return when {
            // Matroska / WebM EBML Header: 0x1A, 0x45, 0xDF, 0xA3
            header[0] == 0x1A.toByte() && header[1] == 0x45.toByte() &&
                header[2] == 0xDF.toByte() && header[3] == 0xA3.toByte() -> {
                extractMkvChapters(stream)
            }
            // MP4 / MOV: usually has box length + 'ftyp' or 'moov'
            (header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() &&
                header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()) ||
            (header[4] == 'm'.code.toByte() && header[5] == 'o'.code.toByte() &&
                header[6] == 'o'.code.toByte() && header[7] == 'v'.code.toByte()) -> {
                extractMp4Chapters(stream)
            }
            else -> emptyList()
        }
    }

    /**
     * Extracts chapters from Media3 ID3 metadata.
     */
    fun extractFromMetadata(metadata: Metadata): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        for (i in 0 until metadata.length()) {
            val entry = metadata.get(i)
            if (entry is ChapterFrame) {
                var title = entry.chapterId
                for (j in 0 until entry.subFrameCount) {
                    when (val sub = entry.getSubFrame(j)) {
                        is TextInformationFrame -> {
                            if (sub.values.isNotEmpty()) {
                                title = sub.values.first()
                                break
                            }
                        }
                        is CommentFrame -> {
                            if (sub.text.isNotBlank()) {
                                title = sub.text
                                break
                            }
                        }
                    }
                }
                chapters.add(
                    Chapter(
                        id = entry.chapterId.ifBlank { "chap_${chapters.size + 1}" },
                        title = title.ifBlank { "Chapter ${chapters.size + 1}" },
                        startTimeMs = entry.startTimeMs.toLong().coerceAtLeast(0L),
                        endTimeMs = entry.endTimeMs.toLong().takeIf { it > entry.startTimeMs }
                    )
                )
            }
        }
        return chapters.sortedBy { it.startTimeMs }
    }

    // =========================================================================
    // Matroska (MKV) EBML Chapter Parser
    // =========================================================================

    private const val MATROSKA_ID_CHAPTERS = 0x1043A770L
    private const val MATROSKA_ID_EDITION_ENTRY = 0x45B9L
    private const val MATROSKA_ID_CHAPTER_ATOM = 0xB6L
    private const val MATROSKA_ID_CHAPTER_TIME_START = 0x91L
    private const val MATROSKA_ID_CHAPTER_TIME_END = 0x92L
    private const val MATROSKA_ID_CHAPTER_DISPLAY = 0x80L
    private const val MATROSKA_ID_CHAP_STRING = 0x85L

    private fun extractMkvChapters(stream: InputStream): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        try {
            var scanned = 0L
            while (scanned < MAX_SCAN_BYTES) {
                val elementId = readEbmlId(stream) ?: break
                val elementSize = readEbmlSize(stream) ?: break
                scanned += 8

                if (elementId == MATROSKA_ID_CHAPTERS) {
                    val chapterData = ByteArray(elementSize.coerceAtMost(2 * 1024 * 1024).toInt())
                    var offset = 0
                    while (offset < chapterData.size) {
                        val r = stream.read(chapterData, offset, chapterData.size - offset)
                        if (r <= 0) break
                        offset += r
                    }
                    parseMkvChaptersPayload(chapterData, chapters)
                    break
                } else if (elementId == 0x18538067L) {
                    // Segment: container for top-level elements, step inside
                    continue
                } else {
                    // Skip element payload
                    skipFully(stream, elementSize)
                    scanned += elementSize
                }
            }
        } catch (_: Throwable) {
            // Stream parse failed; return whatever chapters were recovered
        }
        return chapters.sortedBy { it.startTimeMs }
    }

    internal fun parseMkvChaptersPayload(data: ByteArray, outChapters: MutableList<Chapter>) {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        while (buffer.hasRemaining()) {
            val id = readEbmlIdFromBuffer(buffer) ?: break
            val size = readEbmlSizeFromBuffer(buffer) ?: break
            if (id == MATROSKA_ID_EDITION_ENTRY || id == MATROSKA_ID_CHAPTER_ATOM) {
                // Parse nested atom
                if (id == MATROSKA_ID_CHAPTER_ATOM) {
                    parseChapterAtom(buffer, size.toInt(), outChapters)
                }
            } else {
                val skip = size.toInt().coerceAtMost(buffer.remaining())
                buffer.position(buffer.position() + skip)
            }
        }
    }

    private fun parseChapterAtom(buffer: ByteBuffer, atomLength: Int, outChapters: MutableList<Chapter>) {
        val endPos = (buffer.position() + atomLength).coerceAtMost(buffer.limit())
        var startTimeNs = 0L
        var endTimeNs: Long? = null
        var title: String? = null

        while (buffer.position() < endPos) {
            val id = readEbmlIdFromBuffer(buffer) ?: break
            val size = readEbmlSizeFromBuffer(buffer)?.toInt() ?: break
            val safeSize = size.coerceAtMost(endPos - buffer.position())

            when (id) {
                MATROSKA_ID_CHAPTER_TIME_START -> {
                    startTimeNs = readUint(buffer, safeSize)
                }
                MATROSKA_ID_CHAPTER_TIME_END -> {
                    endTimeNs = readUint(buffer, safeSize)
                }
                MATROSKA_ID_CHAPTER_DISPLAY -> {
                    val displayEnd = buffer.position() + safeSize
                    while (buffer.position() < displayEnd) {
                        val subId = readEbmlIdFromBuffer(buffer) ?: break
                        val subSize = readEbmlSizeFromBuffer(buffer)?.toInt() ?: break
                        if (subId == MATROSKA_ID_CHAP_STRING) {
                            val strBytes = ByteArray(subSize.coerceAtMost(displayEnd - buffer.position()))
                            buffer.get(strBytes)
                            title = String(strBytes, Charsets.UTF_8).trim()
                        } else {
                            buffer.position(buffer.position() + subSize.coerceAtMost(displayEnd - buffer.position()))
                        }
                    }
                }
                MATROSKA_ID_CHAPTER_ATOM -> {
                    // Recursive sub-chapter
                    parseChapterAtom(buffer, safeSize, outChapters)
                }
                else -> {
                    buffer.position(buffer.position() + safeSize)
                }
            }
        }

        val startMs = (startTimeNs / 1_000_000L).coerceAtLeast(0L)
        val endMs = endTimeNs?.let { (it / 1_000_000L).takeIf { ms -> ms > startMs } }
        val chapterIndex = outChapters.size + 1
        outChapters.add(
            Chapter(
                id = "mkv_ch_$chapterIndex",
                title = title?.ifBlank { "Chapter $chapterIndex" } ?: "Chapter $chapterIndex",
                startTimeMs = startMs,
                endTimeMs = endMs
            )
        )
    }

    private fun readUint(buffer: ByteBuffer, size: Int): Long {
        var value = 0L
        for (i in 0 until size) {
            if (!buffer.hasRemaining()) break
            value = (value shl 8) or (buffer.get().toLong() and 0xFF)
        }
        return value
    }

    private fun readEbmlId(stream: InputStream): Long? {
        val first = stream.read()
        if (first < 0) return null
        var mask = 0x80
        var length = 1
        while (length <= 4 && (first and mask) == 0) {
            mask = mask shr 1
            length++
        }
        if (length > 4) return null
        var id = first.toLong()
        for (i in 1 until length) {
            val b = stream.read()
            if (b < 0) return null
            id = (id shl 8) or b.toLong()
        }
        return id
    }

    private fun readEbmlSize(stream: InputStream): Long? {
        val first = stream.read()
        if (first < 0) return null
        var mask = 0x80
        var length = 1
        while (length <= 8 && (first and mask) == 0) {
            mask = mask shr 1
            length++
        }
        if (length > 8) return null
        var size = (first and mask.inv()).toLong()
        for (i in 1 until length) {
            val b = stream.read()
            if (b < 0) return null
            size = (size shl 8) or b.toLong()
        }
        return size
    }

    private fun readEbmlIdFromBuffer(buffer: ByteBuffer): Long? {
        if (!buffer.hasRemaining()) return null
        val first = buffer.get().toInt() and 0xFF
        var mask = 0x80
        var length = 1
        while (length <= 4 && (first and mask) == 0) {
            mask = mask shr 1
            length++
        }
        if (length > 4 || buffer.remaining() < length - 1) return null
        var id = first.toLong()
        for (i in 1 until length) {
            val b = buffer.get().toInt() and 0xFF
            id = (id shl 8) or b.toLong()
        }
        return id
    }

    private fun readEbmlSizeFromBuffer(buffer: ByteBuffer): Long? {
        if (!buffer.hasRemaining()) return null
        val first = buffer.get().toInt() and 0xFF
        var mask = 0x80
        var length = 1
        while (length <= 8 && (first and mask) == 0) {
            mask = mask shr 1
            length++
        }
        if (length > 8 || buffer.remaining() < length - 1) return null
        var size = (first and mask.inv()).toLong()
        for (i in 1 until length) {
            val b = buffer.get().toInt() and 0xFF
            size = (size shl 8) or b.toLong()
        }
        return size
    }

    // =========================================================================
    // MP4 / MOV Chapter Parser (moov.udta.chpl)
    // =========================================================================

    private fun extractMp4Chapters(stream: InputStream): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        try {
            var scanned = 0L
            while (scanned < MAX_SCAN_BYTES) {
                val header = ByteArray(8)
                val read = stream.read(header)
                if (read < 8) break
                scanned += 8

                val boxSize = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL
                val boxType = String(header, 4, 4, Charsets.US_ASCII)
                val payloadSize = (if (boxSize == 1L) {
                    // 64-bit large box
                    val large = ByteArray(8)
                    stream.read(large)
                    scanned += 8
                    ByteBuffer.wrap(large).order(ByteOrder.BIG_ENDIAN).long - 16
                } else if (boxSize == 0L) {
                    MAX_SCAN_BYTES - scanned
                } else {
                    boxSize - 8
                }).coerceAtLeast(0L)

                when (boxType) {
                    "moov", "udta" -> {
                        // Step inside container box
                        continue
                    }
                    "chpl" -> {
                        // Nero chapter list atom
                        val payload = ByteArray(payloadSize.coerceAtMost(512 * 1024).toInt())
                        var readTotal = 0
                        while (readTotal < payload.size) {
                            val r = stream.read(payload, readTotal, payload.size - readTotal)
                            if (r <= 0) break
                            readTotal += r
                        }
                        parseMp4ChplPayload(payload, chapters)
                        break
                    }
                    else -> {
                        skipFully(stream, payloadSize)
                        scanned += payloadSize
                    }
                }
            }
        } catch (_: Throwable) {
            // Stream parse failed
        }
        return chapters.sortedBy { it.startTimeMs }
    }

    internal fun parseMp4ChplPayload(data: ByteArray, outChapters: MutableList<Chapter>) {
        if (data.size < 8) return
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        val version = buffer.get().toInt() and 0xFF
        buffer.get() // flags
        buffer.get()
        buffer.get()

        val count: Int
        if (version == 1) {
            buffer.int // reserved (4 bytes)
            count = buffer.int
        } else {
            buffer.get() // reserved (1 byte)
            count = buffer.get().toInt() and 0xFF
        }

        for (i in 0 until count) {
            if (buffer.remaining() < 9) break
            val timestamp = buffer.long
            val startMs = timestamp / 10_000L // 100ns units -> ms
            val titleLen = buffer.get().toInt() and 0xFF
            val titleBytes = ByteArray(titleLen.coerceAtMost(buffer.remaining()))
            buffer.get(titleBytes)
            val title = String(titleBytes, Charsets.UTF_8).trim()

            val chapterNum = outChapters.size + 1
            outChapters.add(
                Chapter(
                    id = "mp4_ch_$chapterNum",
                    title = title.ifBlank { "Chapter $chapterNum" },
                    startTimeMs = startMs.coerceAtLeast(0L),
                    endTimeMs = null
                )
            )
        }
    }

    private fun skipFully(stream: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = stream.skip(remaining)
            if (skipped <= 0) {
                if (stream.read() == -1) break
                remaining -= 1
            } else {
                remaining -= skipped
            }
        }
    }
}
