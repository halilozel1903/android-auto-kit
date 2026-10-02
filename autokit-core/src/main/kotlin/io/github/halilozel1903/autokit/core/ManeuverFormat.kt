package io.github.halilozel1903.autokit.core

/**
 * Turns steps into text. [English] is built in; implement the interface (or subclass
 * [EnglishManeuverFormat]) for other languages.
 */
public interface ManeuverFormat {

    /** The instruction on its own: "Turn right onto Elm Street". */
    public fun instruction(step: NavigationStep): String

    /** The instruction with the distance: "In 300 m, turn right onto Elm Street". */
    public fun cue(step: NavigationStep, units: DistanceUnits = DistanceUnits.METRIC): String

    public companion object {
        /** English instructions. */
        public val English: ManeuverFormat = EnglishManeuverFormat()
    }
}

/**
 * English instructions. Departures never get a distance ("Start on Main Street"); maneuvers closer than
 * [nowThresholdMeters] read "Now, turn right".
 */
public open class EnglishManeuverFormat(
    private val nowThresholdMeters: Double = 20.0,
) : ManeuverFormat {

    override fun instruction(step: NavigationStep): String = phrase(step).replaceFirstChar { it.uppercaseChar() }

    override fun cue(step: NavigationStep, units: DistanceUnits): String {
        val phrase = phrase(step)
        return when {
            step.maneuver == ManeuverType.DEPART -> instruction(step)
            step.distanceMeters < nowThresholdMeters -> "Now, $phrase"
            else -> "In ${DistanceFormat.text(step.distanceMeters, units)}, $phrase"
        }
    }

    /** The lower case phrase for a step, without distance. */
    protected open fun phrase(step: NavigationStep): String {
        val road = step.road?.takeIf { it.isNotBlank() }
        val onto = road?.let { " onto $it" }.orEmpty()
        return when (step.maneuver) {
            ManeuverType.DEPART -> if (road != null) "start on $road" else "start the route"
            ManeuverType.STRAIGHT -> "continue straight" + (road?.let { " on $it" }.orEmpty())
            ManeuverType.TURN_LEFT -> "turn left$onto"
            ManeuverType.TURN_RIGHT -> "turn right$onto"
            ManeuverType.SLIGHT_LEFT -> "bear left$onto"
            ManeuverType.SLIGHT_RIGHT -> "bear right$onto"
            ManeuverType.SHARP_LEFT -> "make a sharp left$onto"
            ManeuverType.SHARP_RIGHT -> "make a sharp right$onto"
            ManeuverType.U_TURN_LEFT, ManeuverType.U_TURN_RIGHT -> "make a U-turn$onto"
            ManeuverType.KEEP_LEFT -> "keep left$onto"
            ManeuverType.KEEP_RIGHT -> "keep right$onto"
            ManeuverType.MERGE -> "merge$onto"
            ManeuverType.EXIT_LEFT -> "take the exit on the left" + (road?.let { " toward $it" }.orEmpty())
            ManeuverType.EXIT_RIGHT -> "take the exit on the right" + (road?.let { " toward $it" }.orEmpty())
            ManeuverType.ROUNDABOUT -> "at the roundabout, take the ${ordinal(step.roundaboutExit ?: 1)} exit$onto"
            ManeuverType.FERRY -> "take the ferry" + (road?.let { " to $it" }.orEmpty())
            ManeuverType.DESTINATION -> if (road != null) "arrive at $road" else "arrive at your destination"
            ManeuverType.DESTINATION_LEFT -> (road ?: "your destination") + " is on the left"
            ManeuverType.DESTINATION_RIGHT -> (road ?: "your destination") + " is on the right"
        }
    }

    public companion object {
        /** "1st", "2nd", "3rd", "4th", "11th", "21st". */
        public fun ordinal(number: Int): String {
            val suffix = when {
                number % 100 in 11..13 -> "th"
                number % 10 == 1 -> "st"
                number % 10 == 2 -> "nd"
                number % 10 == 3 -> "rd"
                else -> "th"
            }
            return "$number$suffix"
        }
    }
}
