package io.github.halilozel1903.autokit.core

/** The maneuver at the end of a [NavigationStep]. Left and right are from the driver's view. */
public enum class ManeuverType {
    DEPART,
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    U_TURN_LEFT,
    U_TURN_RIGHT,
    KEEP_LEFT,
    KEEP_RIGHT,
    MERGE,
    EXIT_LEFT,
    EXIT_RIGHT,

    /** Needs [NavigationStep.roundaboutExit]. */
    ROUNDABOUT,
    FERRY,
    DESTINATION,
    DESTINATION_LEFT,
    DESTINATION_RIGHT,
}

/**
 * One instruction of a route.
 *
 * @property distanceMeters distance from the previous maneuver (or the start) to this one.
 * @property road the road the driver is on after the maneuver, or the destination name.
 * @property roundaboutExit the exit to take, 1 for the first exit. Required for [ManeuverType.ROUNDABOUT].
 */
public data class NavigationStep(
    val maneuver: ManeuverType,
    val distanceMeters: Double,
    val road: String? = null,
    val roundaboutExit: Int? = null,
)

/** A list of steps to a destination. */
public data class Route(
    val destination: String,
    val steps: List<NavigationStep>,
) {
    /** The sum of all step distances. */
    val totalDistanceMeters: Double get() = steps.sumOf { it.distanceMeters }

    /** The distance from the start of step [index]'s leg to the destination. */
    public fun remainingMeters(fromStep: Int): Double {
        require(fromStep in steps.indices) { "fromStep $fromStep is outside the route" }
        return steps.drop(fromStep).sumOf { it.distanceMeters }
    }

    /**
     * The route as a list screen: one row per step with the spoken style cue as the title
     * ("In 300 m, turn right") and the road as the text. Row ids are `"step:<index>"`.
     */
    public fun toCarList(
        title: String = "Route to $destination",
        units: DistanceUnits = DistanceUnits.METRIC,
        format: ManeuverFormat = ManeuverFormat.English,
    ): CarList = CarList(
        title = title,
        rows = steps.mapIndexed { index, step ->
            CarRow(
                id = stepRowId(index),
                title = format.cue(step, units),
                texts = listOfNotNull(
                    listOfNotNull(step.road, DistanceFormat.format(step.distanceMeters, units).text.takeIf { step.maneuver != ManeuverType.DEPART })
                        .joinToString(" · ")
                        .ifEmpty { null },
                ),
                image = CarImage(ManeuverIcons.nameFor(step.maneuver)),
            )
        },
    )

    public companion object {
        /** The row id [toCarList] uses for step [index]. */
        public fun stepRowId(index: Int): String = "step:$index"
    }
}

/**
 * Names of the maneuver icons that ship with the Android library (`autokit_maneuver_*` drawables).
 * The phone preview can use the same names.
 */
public object ManeuverIcons {
    public fun nameFor(type: ManeuverType): String = "autokit_maneuver_" + when (type) {
        ManeuverType.DEPART -> "depart"
        ManeuverType.STRAIGHT, ManeuverType.MERGE, ManeuverType.FERRY -> "straight"
        ManeuverType.TURN_LEFT -> "turn_left"
        ManeuverType.TURN_RIGHT -> "turn_right"
        ManeuverType.SLIGHT_LEFT, ManeuverType.EXIT_LEFT -> "slight_left"
        ManeuverType.SLIGHT_RIGHT, ManeuverType.EXIT_RIGHT -> "slight_right"
        ManeuverType.SHARP_LEFT -> "sharp_left"
        ManeuverType.SHARP_RIGHT -> "sharp_right"
        ManeuverType.U_TURN_LEFT -> "u_turn_left"
        ManeuverType.U_TURN_RIGHT -> "u_turn_right"
        ManeuverType.KEEP_LEFT -> "keep_left"
        ManeuverType.KEEP_RIGHT -> "keep_right"
        ManeuverType.ROUNDABOUT -> "roundabout"
        ManeuverType.DESTINATION, ManeuverType.DESTINATION_LEFT, ManeuverType.DESTINATION_RIGHT -> "destination"
    }
}
