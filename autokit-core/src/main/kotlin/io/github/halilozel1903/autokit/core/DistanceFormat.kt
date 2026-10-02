package io.github.halilozel1903.autokit.core

import java.util.Locale
import kotlin.math.roundToLong

/** The unit system for distances. */
public enum class DistanceUnits { METRIC, IMPERIAL }

/** The unit of a [FormattedDistance]. */
public enum class DistanceUnit(public val symbol: String) {
    METERS("m"),
    KILOMETERS("km"),
    FEET("ft"),
    MILES("mi"),
}

/**
 * A distance rounded the way navigation apps show it.
 *
 * @property value the rounded value in [unit].
 * @property decimals digits after the decimal point (0 or 1). The Android layer maps one decimal to
 * `Distance.UNIT_KILOMETERS_P1` / `UNIT_MILES_P1`.
 */
public data class FormattedDistance(
    val value: Double,
    val unit: DistanceUnit,
    val decimals: Int,
) {
    /** "300 m", "1.2 km", "500 ft", "12 mi". */
    val text: String
        get() = (if (decimals == 0) value.roundToLong().toString() else String.format(Locale.ROOT, "%.${decimals}f", value)) + " " + unit.symbol
}

/**
 * Rounds distances for display:
 *
 * - metric: below 100 m to 10 m, below 1 km to 50 m, below 10 km to 0.1 km, then whole kilometers.
 * - imperial: below 0.1 mi in feet (to 10 ft below 100 ft, else to 50 ft), below 10 mi to 0.1 mi,
 *   then whole miles.
 *
 * A value that rounds up to the next unit switches unit ("995 m" becomes "1.0 km").
 */
public object DistanceFormat {
    private const val METERS_PER_MILE = 1609.344
    private const val FEET_PER_METER = 3.2808399

    public fun format(meters: Double, units: DistanceUnits = DistanceUnits.METRIC): FormattedDistance {
        require(meters.isFinite() && meters >= 0) { "meters must be a finite distance of 0 or more, was $meters" }
        return when (units) {
            DistanceUnits.METRIC -> metric(meters)
            DistanceUnits.IMPERIAL -> imperial(meters)
        }
    }

    /** Shortcut for `format(meters, units).text`. */
    public fun text(meters: Double, units: DistanceUnits = DistanceUnits.METRIC): String = format(meters, units).text

    private fun metric(meters: Double): FormattedDistance {
        if (meters < 100) {
            val rounded = roundTo(meters, 10.0)
            if (rounded < 100) return FormattedDistance(rounded, DistanceUnit.METERS, 0)
        }
        if (meters < 1000) {
            val rounded = roundTo(meters, 50.0)
            if (rounded < 1000) return FormattedDistance(rounded, DistanceUnit.METERS, 0)
        }
        val km = meters / 1000
        val tenths = roundTo(km, 0.1)
        if (tenths < 10) return FormattedDistance(tenths, DistanceUnit.KILOMETERS, 1)
        return FormattedDistance(roundTo(km, 1.0), DistanceUnit.KILOMETERS, 0)
    }

    private fun imperial(meters: Double): FormattedDistance {
        val miles = meters / METERS_PER_MILE
        if (miles < 0.1) {
            val feet = meters * FEET_PER_METER
            val rounded = if (feet < 100) roundTo(feet, 10.0) else roundTo(feet, 50.0)
            if (rounded < 528) return FormattedDistance(rounded, DistanceUnit.FEET, 0)
        }
        val tenths = roundTo(miles, 0.1).coerceAtLeast(0.1)
        if (tenths < 10) return FormattedDistance(tenths, DistanceUnit.MILES, 1)
        return FormattedDistance(roundTo(miles, 1.0), DistanceUnit.MILES, 0)
    }

    private fun roundTo(value: Double, step: Double): Double {
        // Round half up on the step count; dividing by 0.1 is inexact, so round the count first.
        val count = Math.floor(value / step + 0.5 + 1e-9)
        return if (step < 1) Math.round(count * step * 10) / 10.0 else count * step
    }
}
