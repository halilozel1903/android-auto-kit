package io.github.halilozel1903.autokit.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaQueueTest {
    private val items = listOf("a", "b", "c", "d").map { MediaItem(it, "Track ${it.uppercase()}", artist = "Band", durationMillis = 225_000) }
    private val queue = MediaQueue(items)

    @Test
    fun startsAtTheFirstItem() {
        assertEquals("a", queue.current?.id)
        assertTrue(queue.hasNext)
        assertFalse(queue.hasPrevious)
        assertNull(MediaQueue(emptyList()).current)
    }

    @Test
    fun nextStopsAtTheEndWithRepeatOff() {
        val end = queue.next().next().next()
        assertEquals("d", end.current?.id)
        assertFalse(end.hasNext)
        assertEquals(end, end.next())
    }

    @Test
    fun repeatAllWrapsBothWays() {
        val all = queue.withRepeat(RepeatMode.ALL)
        assertEquals("d", all.previous().current?.id)
        assertEquals("a", all.skipTo("d").next().current?.id)
    }

    @Test
    fun repeatOneReplaysOnlyWhenTheTrackEnds() {
        val one = queue.withRepeat(RepeatMode.ONE)
        assertEquals("a", one.next(userInitiated = false).current?.id)
        assertEquals("b", one.next(userInitiated = true).current?.id)
    }

    @Test
    fun previousRestartsAfterThreeSeconds() {
        assertTrue(queue.shouldRestartOnPrevious(positionMillis = 3_500))
        assertFalse(queue.shouldRestartOnPrevious(positionMillis = 1_000))
        assertEquals("b", queue.skipTo("c").previous().current?.id)
        assertEquals(queue, queue.previous())
    }

    @Test
    fun skipToUnknownIdKeepsTheQueue() {
        assertEquals(queue, queue.skipTo("zzz"))
    }

    @Test
    fun shuffleKeepsTheCurrentItemFirstAndOffRestoresTheOrder() {
        val shuffled = queue.skipTo("b").withShuffle(true, Random(7))
        assertTrue(shuffled.isShuffled)
        assertEquals(1, shuffled.playOrder.first())
        assertEquals(setOf(0, 1, 2, 3), shuffled.playOrder.toSet())
        assertEquals(items, shuffled.items)
        val order = shuffled.upNext().map { it.id }
        assertEquals(3, order.size)
        assertEquals(order.first(), shuffled.next().current?.id)
        val plain = shuffled.withShuffle(false)
        assertFalse(plain.isShuffled)
        assertEquals("b", plain.current?.id)
        assertEquals(listOf("c", "d"), plain.upNext().map { it.id })
    }

    @Test
    fun upNextWrapsWithRepeatAll() {
        assertEquals(listOf("d", "a", "b"), queue.skipTo("c").withRepeat(RepeatMode.ALL).upNext(3).map { it.id })
        assertEquals(listOf("d"), queue.skipTo("c").upNext().map { it.id })
    }

    @Test
    fun enqueueAndPlayNext() {
        val e = MediaItem("e", "Track E")
        assertEquals(listOf("b", "c", "d", "e"), queue.enqueue(e).upNext().map { it.id })
        assertEquals(listOf("e", "b", "c", "d"), queue.playNext(e).upNext().map { it.id })
        assertEquals("e", MediaQueue(emptyList()).enqueue(e).current?.id)
    }

    @Test
    fun playNextAlsoWorksWhileShuffled() {
        val shuffled = queue.withShuffle(true, Random(3))
        val e = MediaItem("e", "Track E")
        val next = shuffled.playNext(e)
        assertEquals("a", next.current?.id)
        assertEquals("e", next.next().current?.id)
    }

    @Test
    fun removingTheCurrentItemMovesToTheNext() {
        val removed = queue.skipTo("b").remove("b")
        assertEquals("c", removed.current?.id)
        assertEquals(listOf("a", "c", "d"), removed.items.map { it.id })
        assertEquals("c", queue.skipTo("d").remove("d").current?.id)
        assertEquals("c", queue.skipTo("c").remove("a").current?.id)
        assertNull(MediaQueue(items.take(1)).remove("a").current)
    }

    @Test
    fun moveKeepsTheCurrentItem() {
        val moved = queue.skipTo("b").move(from = 3, to = 0)
        assertEquals(listOf("d", "a", "b", "c"), moved.items.map { it.id })
        assertEquals("b", moved.current?.id)
    }

    @Test
    fun invalidQueuesAreRejected() {
        assertFailsWith<IllegalArgumentException> { MediaQueue(items, currentIndex = 4) }
        assertFailsWith<IllegalArgumentException> { MediaQueue(items + items.first()) }
    }

    @Test
    fun toCarListMarksTheCurrentItem() {
        val list = queue.skipTo("b").toCarList("Audio guides", nowPlayingImage = CarImage("ic_equalizer"))
        assertEquals(4, list.rows.size)
        assertEquals(listOf("Band · 3:45"), list.rows[0].texts)
        assertEquals(listOf("Now playing · Band · 3:45"), list.rows[1].texts)
        assertEquals("ic_equalizer", list.rows[1].image?.name)
        assertTrue(list.validate().isValid)
    }

    @Test
    fun durations() {
        assertEquals("0:00", MediaFormat.duration(-5))
        assertEquals("3:45", MediaFormat.duration(225_000))
        assertEquals("1:02:03", MediaFormat.duration(3_723_000))
        assertEquals("15:00", MediaFormat.totalDuration(items))
        assertEquals("", MediaFormat.subtitle(MediaItem("x", "X")))
    }
}
