package io.github.halilozel1903.autokit.sample

import io.github.halilozel1903.autokit.core.CarLimits
import io.github.halilozel1903.autokit.core.paged
import io.github.halilozel1903.autokit.core.validate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Every Roadtrip screen fits the default host limits, so the car never has to clip it. */
class RoadtripDataTest {
    private val limits = CarLimits.Default

    @Test
    fun homeGridFits() = assertValid(RoadtripData.home.validate(limits).errors)

    @Test
    fun everyCategoryListFits() {
        RoadtripData.home.items.map { it.id }.filter { it != "guides" && it != "parking" }.forEach { category ->
            assertValid(RoadtripData.placesList(category).validate(limits).errors)
        }
    }

    @Test
    fun everyPlacePaneFits() {
        RoadtripData.places.forEach { assertValid(RoadtripData.placePane(it.id).validate(limits).errors) }
    }

    @Test
    fun messageAndRouteFit() {
        assertValid(RoadtripData.noParking.validate(limits).errors)
        assertValid(RoadtripData.route.validate(limits).errors)
        assertValid(RoadtripData.route.toCarList().validate(limits).errors)
    }

    @Test
    fun audioGuidesArePagedToFit() {
        val pages = RoadtripData.audioGuidesList().paged(limits.maxListItems)
        assertEquals(2, pages.size)
        pages.forEach { assertValid(it.validate(limits).errors) }
        assertEquals("Now playing · Roadtrip Radio · 6:45", pages[0].rows[1].texts.single())
    }

    @Test
    fun everyImageHasADrawable() {
        val names = RoadtripData.home.items.map { it.image.name } +
            RoadtripData.home.actions.mapNotNull { it.icon?.name } +
            RoadtripData.audioGuidesList().rows.mapNotNull { it.image?.name } +
            RoadtripData.route.toCarList().rows.mapNotNull { it.image?.name }
        names.forEach { assertNotNull(RoadtripImages.drawable(it), "No drawable for $it") }
    }

    private fun assertValid(errors: List<Any>) = assertTrue(errors.isEmpty(), errors.toString())
}
