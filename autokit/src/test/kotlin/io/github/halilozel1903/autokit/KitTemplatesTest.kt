package io.github.halilozel1903.autokit

import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.GridItem
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.versioning.CarAppApiLevels
import io.github.halilozel1903.autokit.core.CarAction
import io.github.halilozel1903.autokit.core.CarGrid
import io.github.halilozel1903.autokit.core.CarGridItem
import io.github.halilozel1903.autokit.core.CarImage
import io.github.halilozel1903.autokit.core.CarList
import io.github.halilozel1903.autokit.core.CarMessage
import io.github.halilozel1903.autokit.core.CarModelException
import io.github.halilozel1903.autokit.core.CarPane
import io.github.halilozel1903.autokit.core.CarRow
import io.github.halilozel1903.autokit.core.DistanceFormat
import io.github.halilozel1903.autokit.core.DistanceUnits
import io.github.halilozel1903.autokit.core.ManeuverType
import io.github.halilozel1903.autokit.core.MediaItem
import io.github.halilozel1903.autokit.core.MediaQueue
import io.github.halilozel1903.autokit.core.NavigationStep
import io.github.halilozel1903.autokit.core.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class KitTemplatesTest {
    private val context = RuntimeEnvironment.getApplication()
    private val images = ImageResolver.drawables(context, mapOf("search" to R.drawable.autokit_maneuver_straight))
    private val modern = KitTemplates(context, CarAppApiLevels.LEVEL_7, images)
    private val legacy = KitTemplates(context, CarAppApiLevels.LEVEL_1, images)

    private fun rows(count: Int) = List(count) { CarRow("r$it", "Row $it", texts = listOf("Text $it")) }

    @Test
    fun listUsesHeaderOnModernHosts() {
        val list = CarList("Stops", rows(3), actions = listOf(CarAction("search", "Search", CarImage("search"))))
        val template = modern.list(list) {}
        assertEquals("Stops", template.header?.title.toString())
        assertEquals(Action.TYPE_BACK, template.header?.startHeaderAction?.type)
        assertEquals(1, template.header?.endHeaderActions?.size)
        assertEquals(3, template.singleList?.items?.size)
        val row = template.singleList!!.items[0] as Row
        assertEquals("Row 0", row.title.toString())
        assertEquals("Text 0", row.texts[0].toString())
        assertNotNull(row.onClickDelegate)
    }

    @Test
    @Suppress("DEPRECATION")
    fun listUsesTitleAndActionStripOnOldHosts() {
        val list = CarList("Stops", rows(2), actions = listOf(CarAction("search", "Search", CarImage("search"))))
        val template = legacy.list(list, headerAction = Action.APP_ICON)
        // Car App Library 1.7 also derives a Header from the legacy fields, so only the legacy fields are checked.
        assertEquals("Stops", template.title.toString())
        assertEquals(Action.TYPE_APP_ICON, template.headerAction?.type)
        assertEquals(1, template.actionStrip?.actions?.size)
        val row = template.singleList!!.items[0] as Row
        assertNull("Rows without a click handler are plain", row.onClickDelegate)
    }

    @Test
    fun longListsAreClippedUnlessStrict() {
        val list = CarList("Stops", rows(9))
        assertEquals(6, modern.list(list).singleList?.items?.size)
        val strict = KitTemplates(context, CarAppApiLevels.LEVEL_7, images, strict = true)
        assertThrows(CarModelException::class.java) { strict.list(list) }
    }

    @Test
    fun browsableRowsNeedAClickHandler() {
        val list = CarList("Stops", listOf(CarRow("a", "A", browsable = true)))
        assertTrue((modern.list(list) {}.singleList!!.items[0] as Row).isBrowsable)
        // Without a handler the chevron is dropped instead of failing the build.
        assertEquals(false, (modern.list(list).singleList!!.items[0] as Row).isBrowsable)
    }

    @Test
    fun headerActionsWithoutIconsAreSkipped() {
        val list = CarList("Stops", rows(1), actions = listOf(CarAction("x", "No icon")))
        assertEquals(0, modern.list(list).header?.endHeaderActions?.size)
    }

    @Test
    fun gridItemsHaveImagesEvenWhenUnresolved() {
        val grid = CarGrid(
            "Explore",
            listOf(CarGridItem("a", "Search", CarImage("search"), text = "Nearby"), CarGridItem("b", "Missing", CarImage("nope"))),
        )
        val template = modern.grid(grid) {}
        val items = template.singleList!!.items.map { it as GridItem }
        assertEquals("Search", items[0].title.toString())
        assertEquals("Nearby", items[0].text.toString())
        assertNotNull(items[1].image)
        assertEquals("Explore", template.header?.title.toString())
    }

    @Test
    fun paneHasRowsAndActions() {
        val pane = CarPane(
            "Lookout Point",
            rows(2),
            actions = listOf(CarAction("go", "Navigate", primary = true), CarAction("save", "Save")),
        )
        var clicked: String? = null
        val template = modern.pane(pane) { clicked = it.id }
        assertEquals(2, template.pane.rows.size)
        assertEquals(2, template.pane.actions.size)
        assertEquals(Action.FLAG_PRIMARY, template.pane.actions[0].flags and Action.FLAG_PRIMARY)
        assertNull(clicked)
        assertEquals("Lookout Point", template.header?.title.toString())
    }

    @Test
    @Suppress("DEPRECATION")
    fun messageOnOldAndNewHosts() {
        val message = CarMessage("Parking", "No parking nearby", actions = listOf(CarAction("retry", "Try again")))
        assertEquals("No parking nearby", modern.message(message).message.toString())
        assertEquals("Parking", modern.message(message).header?.title.toString())
        assertEquals("Parking", legacy.message(message).title.toString())
        assertEquals(1, legacy.message(message).actions.size)
    }

    @Test
    fun mediaQueueMarksTheCurrentItem() {
        val queue = MediaQueue(listOf(MediaItem("a", "Harbor"), MediaItem("b", "Lighthouse", artist = "Guide", durationMillis = 61_000)))
            .skipTo("b")
        var played: MediaItem? = null
        val template = modern.mediaQueue(queue, "Audio guides") { played = it }
        val row = template.singleList!!.items[1] as Row
        assertEquals("Now playing · Guide · 1:01", row.texts[0].toString())
        assertNotNull(row.image)
        assertNull(played)
    }

    @Test
    fun routeListHasManeuverIcons() {
        val route = Route(
            "Lighthouse Bay",
            listOf(NavigationStep(ManeuverType.DEPART, 0.0, "Main Street"), NavigationStep(ManeuverType.TURN_RIGHT, 300.0, "Coast Road")),
        )
        val template = modern.route(route)
        val rows = template.singleList!!.items.map { it as Row }
        assertEquals("In 300 m, turn right onto Coast Road", rows[1].title.toString())
        assertTrue(rows.all { it.image != null })
        assertEquals("Route to Lighthouse Bay", template.header?.title.toString())
    }

    @Test
    fun navigationTemplateForNavigationApps() {
        val route = Route(
            "Lighthouse Bay",
            listOf(
                NavigationStep(ManeuverType.ROUNDABOUT, 400.0, "Harbor Way", roundaboutExit = 2),
                NavigationStep(ManeuverType.DESTINATION_RIGHT, 1_200.0, "Lighthouse Bay"),
            ),
        )
        val strip = ActionStrip.Builder().addAction(Action.Builder().setTitle("Stop").setOnClickListener {}.build()).build()
        val template = modern.navigation(route, 0, strip, metersToStep = 320.0, remainingSeconds = 300)
        val info = template.navigationInfo as RoutingInfo
        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, info.currentStep?.maneuver?.type)
        assertEquals(2, info.currentStep?.maneuver?.roundaboutExitNumber)
        assertEquals("At the roundabout, take the 2nd exit onto Harbor Way", info.currentStep?.cue.toString())
        assertEquals(300.0, info.currentDistance!!.displayDistance, 0.0)
        assertNotNull(info.nextStep)
        assertNotNull(template.destinationTravelEstimate)
    }

    @Test
    fun distancesMapToCarUnits() {
        assertEquals(androidx.car.app.model.Distance.UNIT_KILOMETERS_P1, DistanceFormat.format(1_234.0).toCarDistance().displayUnit)
        assertEquals(androidx.car.app.model.Distance.UNIT_METERS, DistanceFormat.format(300.0).toCarDistance().displayUnit)
        assertEquals(androidx.car.app.model.Distance.UNIT_FEET, DistanceFormat.format(50.0, DistanceUnits.IMPERIAL).toCarDistance().displayUnit)
        assertEquals(androidx.car.app.model.Distance.UNIT_MILES, DistanceFormat.format(40_000.0, DistanceUnits.IMPERIAL).toCarDistance().displayUnit)
    }

    @Test
    fun everyManeuverHasABuiltInIcon() {
        ManeuverType.entries.forEach { assertNotNull(KitIcons.maneuverDrawable(it)) }
        assertNotNull(ImageResolver.builtIn(context).resolve(CarImage(KitIcons.NOW_PLAYING)))
    }
}
