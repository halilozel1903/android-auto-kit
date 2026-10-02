package io.github.halilozel1903.autokit.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClippingTest {
    private val icon = CarImage("ic")

    @Test
    fun ellipsizeKeepsShortTextAndCutsLongText() {
        assertEquals("Coast Road", ellipsize("Coast Road", 10))
        assertEquals("Coast…", ellipsize("Coast Road", 7))
        assertEquals("…", ellipsize("Coast Road", 1))
    }

    @Test
    fun clippedListFitsTheLimits() {
        val list = CarList(
            "Stops",
            List(9) { CarRow("r$it", "Row $it", texts = listOf("a", "b", "c")) },
            actions = listOf(CarAction("x", "No icon"), CarAction("a", "A", icon), CarAction("b", "B", icon), CarAction("c", "C", icon)),
        )
        val clipped = list.clippedTo()
        assertTrue(clipped.validate().isValid)
        assertEquals(6, clipped.rows.size)
        assertEquals(listOf("a", "b"), clipped.rows.first().texts)
        assertEquals(listOf("a", "b"), clipped.actions.map { it.id })
    }

    @Test
    fun clippedPaneKeepsOnePrimaryAction() {
        val pane = CarPane(
            "Lookout",
            List(6) { CarRow("r$it", "Row $it", browsable = true) },
            actions = listOf(CarAction("go", "Go", primary = true), CarAction("save", "Save", primary = true)),
        )
        val clipped = pane.clippedTo()
        assertTrue(clipped.validate().isValid)
        assertEquals(4, clipped.rows.size)
        assertEquals(listOf(true, false), clipped.actions.map { it.primary })
        assertTrue(clipped.rows.none { it.browsable })
    }

    @Test
    fun clippedGridAndMessageFit() {
        val grid = CarGrid("Explore", List(10) { CarGridItem("g$it", "Tile $it", icon, text = "x".repeat(200)) })
        val clippedGrid = grid.clippedTo()
        assertTrue(clippedGrid.validate().isValid)
        assertEquals(120, clippedGrid.items.first().text!!.length)

        val message = CarMessage("Parking", "None nearby", actions = List(4) { CarAction("a$it", "A$it") })
        assertEquals(2, message.clippedTo().actions.size)
    }
}
