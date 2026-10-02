package io.github.halilozel1903.autokit.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NavigationFormatTest {
    private val format = ManeuverFormat.English

    @Test
    fun metricDistances() {
        assertEquals("0 m", DistanceFormat.text(3.0))
        assertEquals("50 m", DistanceFormat.text(47.0))
        assertEquals("300 m", DistanceFormat.text(312.0))
        assertEquals("350 m", DistanceFormat.text(330.0))
        assertEquals("1.0 km", DistanceFormat.text(995.0))
        assertEquals("1.2 km", DistanceFormat.text(1_234.0))
        assertEquals("9.9 km", DistanceFormat.text(9_940.0))
        assertEquals("10 km", DistanceFormat.text(9_960.0))
        assertEquals("12 km", DistanceFormat.text(12_400.0))
    }

    @Test
    fun formattedDistanceKeepsValueUnitAndDecimals() {
        assertEquals(FormattedDistance(1.2, DistanceUnit.KILOMETERS, 1), DistanceFormat.format(1_234.0))
        assertEquals(FormattedDistance(300.0, DistanceUnit.METERS, 0), DistanceFormat.format(312.0))
    }

    @Test
    fun imperialDistances() {
        assertEquals("80 ft", DistanceFormat.text(24.0, DistanceUnits.IMPERIAL))
        assertEquals("500 ft", DistanceFormat.text(150.0, DistanceUnits.IMPERIAL))
        assertEquals("0.1 mi", DistanceFormat.text(165.0, DistanceUnits.IMPERIAL))
        assertEquals("1.2 mi", DistanceFormat.text(1_931.0, DistanceUnits.IMPERIAL))
        assertEquals("25 mi", DistanceFormat.text(40_234.0, DistanceUnits.IMPERIAL))
    }

    @Test
    fun negativeDistancesAreRejected() {
        assertFailsWith<IllegalArgumentException> { DistanceFormat.format(-1.0) }
        assertFailsWith<IllegalArgumentException> { DistanceFormat.format(Double.NaN) }
    }

    @Test
    fun cuesIncludeTheDistance() {
        assertEquals("In 300 m, turn right", format.cue(NavigationStep(ManeuverType.TURN_RIGHT, 300.0)))
        assertEquals(
            "In 1.2 km, turn left onto Coast Road",
            format.cue(NavigationStep(ManeuverType.TURN_LEFT, 1_200.0, road = "Coast Road")),
        )
        assertEquals(
            "In 0.2 mi, keep right onto I-5 N",
            format.cue(NavigationStep(ManeuverType.KEEP_RIGHT, 320.0, road = "I-5 N"), DistanceUnits.IMPERIAL),
        )
    }

    @Test
    fun closeManeuversSayNow() {
        assertEquals("Now, turn right", format.cue(NavigationStep(ManeuverType.TURN_RIGHT, 10.0)))
    }

    @Test
    fun instructions() {
        assertEquals("Start on Main Street", format.cue(NavigationStep(ManeuverType.DEPART, 0.0, "Main Street")))
        assertEquals("Start the route", format.instruction(NavigationStep(ManeuverType.DEPART, 0.0)))
        assertEquals("Continue straight on Coast Road", format.instruction(NavigationStep(ManeuverType.STRAIGHT, 5.0, "Coast Road")))
        assertEquals(
            "At the roundabout, take the 2nd exit onto Harbor Way",
            format.instruction(NavigationStep(ManeuverType.ROUNDABOUT, 400.0, "Harbor Way", roundaboutExit = 2)),
        )
        assertEquals("Make a U-turn", format.instruction(NavigationStep(ManeuverType.U_TURN_LEFT, 50.0)))
        assertEquals("Take the exit on the right toward Airport", format.instruction(NavigationStep(ManeuverType.EXIT_RIGHT, 50.0, "Airport")))
        assertEquals("Arrive at your destination", format.instruction(NavigationStep(ManeuverType.DESTINATION, 50.0)))
        assertEquals("Lighthouse Bay is on the left", format.instruction(NavigationStep(ManeuverType.DESTINATION_LEFT, 50.0, "Lighthouse Bay")))
    }

    @Test
    fun ordinals() {
        assertEquals(
            listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd", "101st"),
            listOf(1, 2, 3, 4, 11, 12, 13, 21, 22, 101).map { EnglishManeuverFormat.ordinal(it) },
        )
    }

    @Test
    fun routeAsList() {
        val route = Route(
            "Lighthouse Bay",
            listOf(
                NavigationStep(ManeuverType.DEPART, 0.0, "Main Street"),
                NavigationStep(ManeuverType.TURN_RIGHT, 300.0, "Coast Road"),
                NavigationStep(ManeuverType.DESTINATION_RIGHT, 1_234.0, "Lighthouse Bay"),
            ),
        )
        assertEquals(1_534.0, route.totalDistanceMeters)
        assertEquals(1_234.0, route.remainingMeters(2))
        val list = route.toCarList()
        assertEquals("Route to Lighthouse Bay", list.title)
        assertEquals(listOf("Start on Main Street", "In 300 m, turn right onto Coast Road", "In 1.2 km, Lighthouse Bay is on the right"), list.rows.map { it.title })
        assertEquals(listOf("Main Street"), list.rows[0].texts)
        assertEquals(listOf("Coast Road · 300 m"), list.rows[1].texts)
        assertEquals("autokit_maneuver_turn_right", list.rows[1].image?.name)
        assertEquals("step:1", list.rows[1].id)
    }

    @Test
    fun everyManeuverHasAnIconName() {
        ManeuverType.entries.forEach { assertEquals(true, ManeuverIcons.nameFor(it).startsWith("autokit_maneuver_")) }
    }
}
