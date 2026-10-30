package org.aptura.player.data.playback

/**
 * What the player did since playback was last asked for, as short lines ("+120ms load-done 2048B 14ms"): the order and
 * the timing of its steps, kept so that a video that never starts can say where it stopped (the owner's Samsung S24:
 * state "buffering" at 0:00, and nothing else to go on). Bounded, and made only of names and numbers: never a file name
 * or an address.
 */
class PlaybackTrace(
    private val capacity: Int = 48,
    private val clockNanos: () -> Long = System::nanoTime
) {
    private val lines = ArrayDeque<String>()
    private var startedAt = clockNanos()

    @Synchronized
    fun restart() {
        lines.clear()
        startedAt = clockNanos()
    }

    @Synchronized
    fun note(what: String) {
        if (lines.size >= capacity) lines.removeFirst()
        lines.addLast("+${(clockNanos() - startedAt) / 1_000_000L}ms $what")
    }

    /** The newest lines that fit in [maxChars], oldest first. */
    @Synchronized
    fun summary(maxChars: Int = 1_800): String {
        val joined = lines.joinToString(" | ")
        return if (joined.length <= maxChars) joined else "…" + joined.takeLast(maxChars - 1)
    }
}

/**
 * Where the threads that play video were, at the moment it was clear that playback had not started. Only threads that
 * belong to playback are described, and only by class and method names (a frame is "ProgressiveMediaPeriod.run", never
 * an argument), so a stuck read, a blocked wait or a codec that never answers shows up by name.
 */
object PlaybackThreads {

    private val interesting = listOf("ExoPlayer", "Loader", "Playback", "MediaCodec", "AudioTrack", "Media3", "Aptura")

    fun summarise(
        threads: Map<Thread, Array<StackTraceElement>>,
        maxThreads: Int = 6,
        framesPerThread: Int = 7,
        maxChars: Int = 1_800
    ): String {
        val described = threads.entries
            .filter { (thread, _) -> interesting.any { thread.name.contains(it, ignoreCase = true) } }
            .sortedBy { it.key.name }
            .take(maxThreads)
            .joinToString(" || ") { (thread, stack) ->
                val frames = stack.take(framesPerThread).joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}" }
                "${thread.name} [${thread.state}]: ${frames.ifEmpty { "(no frames)" }}"
            }
        val result = described.ifEmpty { "(no playback threads)" }
        return if (result.length <= maxChars) result else result.take(maxChars - 1) + "…"
    }

    fun ofThisProcess(): String = summarise(Thread.getAllStackTraces())
}
