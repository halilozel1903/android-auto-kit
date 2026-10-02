package io.github.halilozel1903.autokit.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ValidationTest {
    private val icon = CarImage("ic_search")

    private fun rows(count: Int) = List(count) { CarRow(id = "r$it", title = "Row $it") }

    @Test
    fun validListPasses() {
        val list = CarList("Stops", rows(6), actions = listOf(CarAction("search", "Search", icon)))
        assertTrue(list.validate().isValid)
    }

    @Test
    fun tooManyRowsIsReportedWithPathAndLimit() {
        val result = CarList("Stops", rows(8)).validate()
        assertEquals(listOf<ValidationError>(ValidationError.TooManyItems("list.rows", 8, 6)), result.errors)
    }

    @Test
    fun hostLimitsAllowLongerLists() {
        val limits = CarLimits.fromHost(listItems = 100, gridItems = 0, paneRows = -1)
        assertTrue(CarList("Stops", rows(40)).validate(limits).isValid)
        assertEquals(CarLimits.DEFAULT_GRID_ITEMS, limits.maxGridItems)
        assertEquals(CarLimits.DEFAULT_PANE_ROWS, limits.maxPaneRows)
    }

    @Test
    fun headerActionsNeedIconsAndAreLimitedToTwo() {
        val list = CarList(
            "Stops",
            rows(1),
            actions = listOf(CarAction("a", "A", icon), CarAction("b", "B"), CarAction("c", "C", icon)),
        )
        val errors = list.validate().errors
        assertTrue(ValidationError.TooManyActions("list.actions", 3, 2) in errors)
        assertTrue(ValidationError.MissingActionIcon("list.actions", "b") in errors)
    }

    @Test
    fun rowTextsAreLimited() {
        val list = CarList("Stops", listOf(CarRow("r", "Row", texts = listOf("one", "two", "three"))))
        assertEquals(listOf<ValidationError>(ValidationError.TooManyRowTexts("list.rows[0]", 3, 2)), list.validate().errors)
    }

    @Test
    fun blankAndLongTextAreReported() {
        val list = CarList(" ", listOf(CarRow("r", "x".repeat(81), texts = listOf(""))))
        val errors = list.validate().errors
        assertTrue(ValidationError.BlankText("list.title") in errors)
        assertTrue(ValidationError.TextTooLong("list.rows[0].title", 81, 80) in errors)
        assertTrue(ValidationError.BlankText("list.rows[0].texts[0]") in errors)
    }

    @Test
    fun duplicateIdsAreReportedOnce() {
        val list = CarList("Stops", listOf(CarRow("a", "A"), CarRow("a", "B"), CarRow("a", "C")))
        assertEquals(listOf<ValidationError>(ValidationError.DuplicateId("list.rows", "a")), list.validate().errors)
    }

    @Test
    fun gridIsLimitedToSixTiles() {
        val grid = CarGrid("Explore", List(7) { CarGridItem("g$it", "Tile $it", icon) })
        assertEquals(listOf<ValidationError>(ValidationError.TooManyItems("grid.items", 7, 6)), grid.validate().errors)
    }

    @Test
    fun paneNeedsRowsAndAtMostOnePrimaryAction() {
        val pane = CarPane(
            "Lookout",
            rows = emptyList(),
            actions = listOf(CarAction("go", "Go", primary = true), CarAction("save", "Save", primary = true)),
        )
        val errors = pane.validate().errors
        assertTrue(ValidationError.EmptyContent("pane.rows") in errors)
        assertTrue(ValidationError.MultiplePrimaryActions("pane.actions", 2) in errors)
    }

    @Test
    fun paneRowsAndActionsAreLimited() {
        val pane = CarPane("Lookout", rows(5), actions = List(3) { CarAction("a$it", "Action $it") })
        val errors = pane.validate().errors
        assertTrue(ValidationError.TooManyItems("pane.rows", 5, 4) in errors)
        assertTrue(ValidationError.TooManyActions("pane.actions", 3, 2) in errors)
    }

    @Test
    fun messageNeedsText() {
        val errors = CarMessage("Parking", "  ").validate().errors
        assertEquals(listOf<ValidationError>(ValidationError.BlankText("message.message")), errors)
    }

    @Test
    fun routeChecksDistancesAndRoundaboutExits() {
        val route = Route(
            "Bay",
            listOf(
                NavigationStep(ManeuverType.DEPART, 0.0),
                NavigationStep(ManeuverType.ROUNDABOUT, 200.0),
                NavigationStep(ManeuverType.TURN_LEFT, -5.0),
            ),
        )
        val paths = route.validate().errors.map { it.path }
        assertEquals(listOf("route.steps[1].roundaboutExit", "route.steps[2].distanceMeters"), paths)
        assertTrue(Route("Bay", emptyList()).validate().errors.contains(ValidationError.EmptyContent("route.steps")))
    }

    @Test
    fun orThrowListsEveryError() {
        val error = assertFailsWith<CarModelException> { CarList("Stops", rows(7)).validate().orThrow() }
        assertEquals(1, error.errors.size)
        assertTrue(error.message!!.contains("list.rows has 7 items, the limit is 6"))
    }

    @Test
    fun limitsRejectNonsense() {
        assertFailsWith<IllegalArgumentException> { CarLimits(maxListItems = 0) }
    }
}
