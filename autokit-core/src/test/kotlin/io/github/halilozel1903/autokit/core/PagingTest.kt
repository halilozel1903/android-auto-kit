package io.github.halilozel1903.autokit.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PagingTest {

    @Test
    fun chunkSplitsEvenly() {
        assertEquals(listOf(listOf(1, 2), listOf(3, 4), listOf(5)), CarPaging.chunk(listOf(1, 2, 3, 4, 5), 2))
        assertEquals(emptyList(), CarPaging.chunk(emptyList<Int>(), 3))
        assertFailsWith<IllegalArgumentException> { CarPaging.chunk(listOf(1), 0) }
    }

    @Test
    fun pagesReserveASlotForMoreExceptOnTheLastPage() {
        val pages = CarPaging.pages((1..14).toList(), maxItemsPerPage = 6)
        assertEquals(listOf(5, 5, 4), pages.map { it.items.size })
        assertEquals(listOf(0, 5, 10), pages.map { it.start })
        assertTrue(pages[0].hasMore)
        assertFalse(pages[2].hasMore)
        assertTrue(pages[2].hasPrevious)
        assertEquals("Page 2 of 3", pages[1].label)
    }

    @Test
    fun aListThatFitsIsOnePage() {
        val pages = CarPaging.pages((1..6).toList(), maxItemsPerPage = 6)
        assertEquals(1, pages.size)
        assertEquals(6, pages[0].items.size)
        assertFalse(pages[0].hasMore)
    }

    @Test
    fun lastPageUsesEverySlot() {
        assertEquals(listOf(5, 6), CarPaging.pages((1..11).toList(), 6).map { it.items.size })
        assertEquals(listOf(5, 5, 2), CarPaging.pages((1..12).toList(), 6).map { it.items.size })
    }

    @Test
    fun pagesWithoutMoreSlotAreFull() {
        assertEquals(listOf(6, 6, 2), CarPaging.pages((1..14).toList(), 6, reserveMoreSlot = false).map { it.items.size })
    }

    @Test
    fun emptyListIsOneEmptyPage() {
        val pages = CarPaging.pages(emptyList<String>(), 6)
        assertEquals(1, pages.size)
        assertTrue(pages[0].items.isEmpty())
    }

    @Test
    fun pageIndexOfFindsTheRightPage() {
        assertEquals(0, CarPaging.pageIndexOf(4, 14, 6))
        assertEquals(1, CarPaging.pageIndexOf(5, 14, 6))
        assertEquals(2, CarPaging.pageIndexOf(13, 14, 6))
    }

    @Test
    fun pagedListAddsMoreRowsThatPointToTheNextPage() {
        val list = CarList("Audio guides", List(14) { CarRow("r$it", "Guide $it") })
        val pages = list.paged(maxItems = 6)
        assertEquals(3, pages.size)
        assertEquals(listOf(6, 6, 4), pages.map { it.rows.size })
        assertEquals("Audio guides", pages[0].title)
        assertEquals("Audio guides (2/3)", pages[1].title)
        val more = pages[0].rows.last()
        assertEquals("More", more.title)
        assertEquals(listOf("9 more"), more.texts)
        assertTrue(more.browsable)
        assertEquals(1, CarPaging.nextPageFor(more.id))
        assertNull(CarPaging.nextPageFor("r1"))
        pages.forEach { assertTrue(it.validate().isValid, it.validate().toString()) }
    }
}
