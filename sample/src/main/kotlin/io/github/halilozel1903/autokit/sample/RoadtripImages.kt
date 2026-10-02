package io.github.halilozel1903.autokit.sample

import android.content.Context
import io.github.halilozel1903.autokit.ImageResolver
import io.github.halilozel1903.autokit.KitIcons

/** Image names used by [RoadtripData] and their drawables. */
object RoadtripImages {
    const val HEADSET = "headset"
    const val COFFEE = "coffee"
    const val FUEL = "fuel"
    const val LANDSCAPE = "landscape"
    const val RESTAURANT = "restaurant"
    const val PARKING = "parking"
    const val NAVIGATION = "navigation"
    const val SEARCH = "search"
    const val STAR = "star"
    const val SHUFFLE = "shuffle"
    const val NEXT = "next"

    val drawables: Map<String, Int> = mapOf(
        HEADSET to R.drawable.ic_headset,
        COFFEE to R.drawable.ic_coffee,
        FUEL to R.drawable.ic_fuel,
        LANDSCAPE to R.drawable.ic_landscape,
        RESTAURANT to R.drawable.ic_restaurant,
        PARKING to R.drawable.ic_parking,
        NAVIGATION to R.drawable.ic_navigation,
        SEARCH to R.drawable.ic_search,
        STAR to R.drawable.ic_star,
        SHUFFLE to R.drawable.ic_shuffle,
        NEXT to R.drawable.ic_skip_next,
    )

    /** The drawable for a name, including the library's maneuver and now playing icons. */
    fun drawable(name: String): Int? = drawables[name] ?: KitIcons.drawable(name)

    /** The resolver the car screens use. */
    fun resolver(context: Context): ImageResolver = ImageResolver.drawables(context, drawables)
}
