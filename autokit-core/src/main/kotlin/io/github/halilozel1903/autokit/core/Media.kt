package io.github.halilozel1903.autokit.core

import java.util.Locale
import kotlin.random.Random

/** A track, episode or audio guide. */
public data class MediaItem(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMillis: Long? = null,
    val image: CarImage? = null,
)

/** What happens at the end of a track or of the queue. */
public enum class RepeatMode {
    /** Stop after the last item. */
    OFF,

    /** Play the current item again. */
    ONE,

    /** Start over after the last item. */
    ALL,
}

/**
 * An immutable play queue with shuffle and repeat. Every operation returns a new queue, so it is safe
 * to keep in a screen and swap on clicks.
 *
 * Shuffle keeps [items] in their original order and plays them in [playOrder] (a permutation of the
 * item indices that starts with the current item), so turning shuffle off resumes the original order.
 *
 * @property currentIndex index into [items] of the current item, or -1 when the queue is empty.
 */
public data class MediaQueue(
    val items: List<MediaItem>,
    val currentIndex: Int = if (items.isEmpty()) -1 else 0,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playOrder: List<Int> = items.indices.toList(),
) {
    init {
        require(if (items.isEmpty()) currentIndex == -1 else currentIndex in items.indices) {
            "currentIndex $currentIndex is outside the queue of ${items.size} items"
        }
        require(playOrder.size == items.size && playOrder.toSet() == items.indices.toSet()) {
            "playOrder must be a permutation of the item indices"
        }
        require(items.map { it.id }.toSet().size == items.size) { "Media item ids must be unique" }
    }

    /** The item that is playing or paused, or `null` for an empty queue. */
    val current: MediaItem? get() = items.getOrNull(currentIndex)

    /** Whether the play order is shuffled. */
    val isShuffled: Boolean get() = playOrder != items.indices.toList()

    private val position: Int get() = playOrder.indexOf(currentIndex)

    /** Whether [next] moves to another item (false at the end of the queue with repeat off). */
    val hasNext: Boolean get() = items.isNotEmpty() && (repeatMode != RepeatMode.OFF || position < items.size - 1)

    /** Whether [previous] moves to another item. */
    val hasPrevious: Boolean get() = items.isNotEmpty() && (repeatMode == RepeatMode.ALL || position > 0)

    /**
     * The queue after the current item finished or the user pressed next. Repeat [RepeatMode.ONE]
     * only applies to items finishing on their own ([userInitiated] false); a press on next always
     * moves on. At the end with repeat off the queue stays on the last item.
     */
    public fun next(userInitiated: Boolean = true): MediaQueue {
        if (items.isEmpty()) return this
        if (repeatMode == RepeatMode.ONE && !userInitiated) return this
        val nextPosition = when {
            position < items.size - 1 -> position + 1
            repeatMode == RepeatMode.OFF -> return this
            else -> 0
        }
        return copy(currentIndex = playOrder[nextPosition])
    }

    /** The queue after pressing previous. At the start the queue wraps with repeat all, else stays. */
    public fun previous(): MediaQueue {
        if (items.isEmpty()) return this
        val previousPosition = when {
            position > 0 -> position - 1
            repeatMode == RepeatMode.ALL -> items.size - 1
            else -> return this
        }
        return copy(currentIndex = playOrder[previousPosition])
    }

    /** Whether previous should restart the current item instead (the usual behavior after 3 seconds). */
    public fun shouldRestartOnPrevious(positionMillis: Long, thresholdMillis: Long = 3_000): Boolean =
        current != null && positionMillis > thresholdMillis

    /** The queue with the item [id] current, or this queue when there is no such item. */
    public fun skipTo(id: String): MediaQueue {
        val index = items.indexOfFirst { it.id == id }
        return if (index < 0) this else copy(currentIndex = index)
    }

    /** The next [count] items in play order after the current one (wrapping with repeat all). */
    public fun upNext(count: Int = items.size): List<MediaItem> {
        require(count >= 0) { "count must not be negative" }
        if (items.isEmpty()) return emptyList()
        val after = playOrder.drop(position + 1) + if (repeatMode == RepeatMode.ALL) playOrder.take(position) else emptyList()
        return after.take(count).map { items[it] }
    }

    /** The queue with [mode]. */
    public fun withRepeat(mode: RepeatMode): MediaQueue = copy(repeatMode = mode)

    /**
     * The queue with shuffle on or off. Turning it on keeps the current item first and shuffles the
     * rest with [random]; turning it off restores the original order.
     */
    public fun withShuffle(enabled: Boolean, random: Random = Random.Default): MediaQueue {
        if (!enabled) return copy(playOrder = items.indices.toList())
        if (items.isEmpty()) return this
        val rest = items.indices.filter { it != currentIndex }.shuffled(random)
        return copy(playOrder = listOf(currentIndex) + rest)
    }

    /** The queue with [item] added at the end (of the items and of the play order). */
    public fun enqueue(item: MediaItem): MediaQueue {
        if (items.isEmpty()) return MediaQueue(listOf(item), repeatMode = repeatMode)
        return copy(items = items + item, playOrder = playOrder + items.size)
    }

    /** The queue with [item] playing right after the current one. */
    public fun playNext(item: MediaItem): MediaQueue {
        if (items.isEmpty()) return MediaQueue(listOf(item), repeatMode = repeatMode)
        val newIndex = currentIndex + 1
        val newItems = items.toMutableList().apply { add(newIndex, item) }
        val shifted = playOrder.map { if (it >= newIndex) it + 1 else it }.toMutableList()
        shifted.add(shifted.indexOf(currentIndex) + 1, newIndex)
        return copy(items = newItems, playOrder = shifted)
    }

    /**
     * The queue without the item [id]. Removing the current item makes the next one in play order
     * current (or the previous one at the end).
     */
    public fun remove(id: String): MediaQueue {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return this
        if (items.size == 1) return MediaQueue(emptyList(), repeatMode = repeatMode)
        val order = playOrder.filter { it != index }.map { if (it > index) it - 1 else it }
        val newCurrent = if (index != currentIndex) {
            if (currentIndex > index) currentIndex - 1 else currentIndex
        } else {
            val pos = playOrder.indexOf(index)
            // The item that followed the removed one now sits at the same position.
            order[pos.coerceAtMost(order.size - 1)]
        }
        return copy(items = items.filterIndexed { i, _ -> i != index }, currentIndex = newCurrent, playOrder = order)
    }

    /** The queue with the item at [from] moved to [to] (indices into [items]); shuffle is turned off. */
    public fun move(from: Int, to: Int): MediaQueue {
        require(from in items.indices && to in items.indices) { "from and to must be item indices" }
        if (from == to) return this
        val currentId = current?.id
        val newItems = items.toMutableList().apply { add(to, removeAt(from)) }
        return MediaQueue(newItems, currentIndex = newItems.indexOfFirst { it.id == currentId }, repeatMode = repeatMode)
    }

    /**
     * The queue as a list screen: one row per item in play order, the current one marked with
     * [nowPlayingLabel]. Row ids are the media item ids.
     */
    public fun toCarList(title: String, nowPlayingLabel: String = "Now playing", nowPlayingImage: CarImage? = null): CarList =
        CarList(
            title = title,
            rows = playOrder.map { index ->
                val item = items[index]
                val subtitle = MediaFormat.subtitle(item)
                val isCurrent = index == currentIndex
                CarRow(
                    id = item.id,
                    title = item.title,
                    texts = listOfNotNull(
                        if (isCurrent) listOfNotNull(nowPlayingLabel, subtitle.ifEmpty { null }).joinToString(" · ") else subtitle.ifEmpty { null },
                    ),
                    image = if (isCurrent && nowPlayingImage != null) nowPlayingImage else item.image,
                )
            },
        )
}

/** Formatting for media rows. */
public object MediaFormat {

    /** "3:45", "1:02:03". Negative values count as zero. */
    public fun duration(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0) / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(Locale.ROOT, hours, minutes, seconds) else "%d:%02d".format(Locale.ROOT, minutes, seconds)
    }

    /** "Artist · 3:45", "3:45", "Artist" or "". */
    public fun subtitle(item: MediaItem): String =
        listOfNotNull(item.artist?.takeIf { it.isNotBlank() }, item.durationMillis?.let { duration(it) }).joinToString(" · ")

    /** The total length of [items] with a known duration, e.g. "1:12:30". */
    public fun totalDuration(items: List<MediaItem>): String = duration(items.sumOf { it.durationMillis ?: 0L })
}
