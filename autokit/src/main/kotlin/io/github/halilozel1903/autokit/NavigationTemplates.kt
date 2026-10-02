package io.github.halilozel1903.autokit

import androidx.car.app.model.ActionStrip
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import io.github.halilozel1903.autokit.core.CarImage
import io.github.halilozel1903.autokit.core.DistanceFormat
import io.github.halilozel1903.autokit.core.DistanceUnit
import io.github.halilozel1903.autokit.core.DistanceUnits
import io.github.halilozel1903.autokit.core.FormattedDistance
import io.github.halilozel1903.autokit.core.ManeuverFormat
import io.github.halilozel1903.autokit.core.ManeuverIcons
import io.github.halilozel1903.autokit.core.ManeuverType
import io.github.halilozel1903.autokit.core.NavigationStep
import io.github.halilozel1903.autokit.core.Route
import java.util.TimeZone

// Turn-by-turn templates. NavigationTemplate is only available to apps in the
// androidx.car.app.category.NAVIGATION category that hold the androidx.car.app.NAVIGATION_TEMPLATES
// permission and draw their own map on the surface; other apps use KitTemplates.route (a list).

/** A Car App Library `Distance` for a rounded distance. */
public fun FormattedDistance.toCarDistance(): Distance = Distance.create(
    value,
    when (unit) {
        DistanceUnit.METERS -> Distance.UNIT_METERS
        DistanceUnit.KILOMETERS -> if (decimals > 0) Distance.UNIT_KILOMETERS_P1 else Distance.UNIT_KILOMETERS
        DistanceUnit.FEET -> Distance.UNIT_FEET
        DistanceUnit.MILES -> if (decimals > 0) Distance.UNIT_MILES_P1 else Distance.UNIT_MILES
    },
)

/** The Car App Library maneuver type. Roundabouts are counterclockwise unless [clockwiseRoundabouts]. */
public fun ManeuverType.toCarManeuverType(clockwiseRoundabouts: Boolean = false): Int = when (this) {
    ManeuverType.DEPART -> Maneuver.TYPE_DEPART
    ManeuverType.STRAIGHT -> Maneuver.TYPE_STRAIGHT
    ManeuverType.TURN_LEFT -> Maneuver.TYPE_TURN_NORMAL_LEFT
    ManeuverType.TURN_RIGHT -> Maneuver.TYPE_TURN_NORMAL_RIGHT
    ManeuverType.SLIGHT_LEFT -> Maneuver.TYPE_TURN_SLIGHT_LEFT
    ManeuverType.SLIGHT_RIGHT -> Maneuver.TYPE_TURN_SLIGHT_RIGHT
    ManeuverType.SHARP_LEFT -> Maneuver.TYPE_TURN_SHARP_LEFT
    ManeuverType.SHARP_RIGHT -> Maneuver.TYPE_TURN_SHARP_RIGHT
    ManeuverType.U_TURN_LEFT -> Maneuver.TYPE_U_TURN_LEFT
    ManeuverType.U_TURN_RIGHT -> Maneuver.TYPE_U_TURN_RIGHT
    ManeuverType.KEEP_LEFT -> Maneuver.TYPE_KEEP_LEFT
    ManeuverType.KEEP_RIGHT -> Maneuver.TYPE_KEEP_RIGHT
    ManeuverType.MERGE -> Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED
    ManeuverType.EXIT_LEFT -> Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT
    ManeuverType.EXIT_RIGHT -> Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT
    ManeuverType.ROUNDABOUT ->
        if (clockwiseRoundabouts) Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW else Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW
    ManeuverType.FERRY -> Maneuver.TYPE_FERRY_BOAT
    ManeuverType.DESTINATION -> Maneuver.TYPE_DESTINATION
    ManeuverType.DESTINATION_LEFT -> Maneuver.TYPE_DESTINATION_LEFT
    ManeuverType.DESTINATION_RIGHT -> Maneuver.TYPE_DESTINATION_RIGHT
}

/** A `Maneuver` with the library's arrow icon (and the exit number for roundabouts). */
public fun KitTemplates.maneuver(step: NavigationStep, clockwiseRoundabouts: Boolean = false): Maneuver {
    val builder = Maneuver.Builder(step.maneuver.toCarManeuverType(clockwiseRoundabouts))
    icon(CarImage(ManeuverIcons.nameFor(step.maneuver)))?.let { builder.setIcon(it) }
    if (step.maneuver == ManeuverType.ROUNDABOUT) builder.setRoundaboutExitNumber((step.roundaboutExit ?: 1).coerceAtLeast(1))
    return builder.build()
}

/** A `Step` whose cue is the instruction without distance ("Turn right onto Coast Road"). */
public fun KitTemplates.step(
    step: NavigationStep,
    format: ManeuverFormat = ManeuverFormat.English,
    clockwiseRoundabouts: Boolean = false,
): Step {
    val builder = Step.Builder(format.instruction(step)).setManeuver(maneuver(step, clockwiseRoundabouts))
    step.road?.takeIf { it.isNotBlank() }?.let { builder.setRoad(it) }
    return builder.build()
}

/**
 * `RoutingInfo` for step [stepIndex] of [route], [metersToStep] away, with the following step as the
 * next step.
 */
public fun KitTemplates.routingInfo(
    route: Route,
    stepIndex: Int,
    metersToStep: Double = route.steps[stepIndex].distanceMeters,
    units: DistanceUnits = DistanceUnits.METRIC,
    format: ManeuverFormat = ManeuverFormat.English,
    clockwiseRoundabouts: Boolean = false,
): RoutingInfo {
    require(stepIndex in route.steps.indices) { "stepIndex $stepIndex is outside the route" }
    val builder = RoutingInfo.Builder().setCurrentStep(
        step(route.steps[stepIndex], format, clockwiseRoundabouts),
        DistanceFormat.format(metersToStep.coerceAtLeast(0.0), units).toCarDistance(),
    )
    route.steps.getOrNull(stepIndex + 1)?.let { builder.setNextStep(step(it, format, clockwiseRoundabouts)) }
    return builder.build()
}

/**
 * A `NavigationTemplate` for step [stepIndex] of [route]. [actionStrip] is required by the template
 * (for example a "Stop" action). With [remainingSeconds] the template also shows the arrival time
 * and remaining distance to the destination.
 */
public fun KitTemplates.navigation(
    route: Route,
    stepIndex: Int,
    actionStrip: ActionStrip,
    metersToStep: Double = route.steps[stepIndex].distanceMeters,
    remainingSeconds: Long? = null,
    units: DistanceUnits = DistanceUnits.METRIC,
    format: ManeuverFormat = ManeuverFormat.English,
    nowMillis: Long = System.currentTimeMillis(),
): NavigationTemplate {
    val builder = NavigationTemplate.Builder()
        .setNavigationInfo(routingInfo(route, stepIndex, metersToStep, units, format))
        .setActionStrip(actionStrip)
    if (remainingSeconds != null) {
        val remainingMeters = route.steps.drop(stepIndex + 1).sumOf { it.distanceMeters } + metersToStep.coerceAtLeast(0.0)
        val arrival = DateTimeWithZone.create(nowMillis + remainingSeconds * 1000, TimeZone.getDefault())
        builder.setDestinationTravelEstimate(
            TravelEstimate.Builder(DistanceFormat.format(remainingMeters, units).toCarDistance(), arrival)
                .setRemainingTimeSeconds(remainingSeconds.coerceAtLeast(0))
                .build(),
        )
    }
    return builder.build()
}
