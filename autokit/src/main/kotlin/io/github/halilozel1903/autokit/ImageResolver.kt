package io.github.halilozel1903.autokit

import android.content.Context
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import io.github.halilozel1903.autokit.core.CarImage
import io.github.halilozel1903.autokit.core.ManeuverIcons
import io.github.halilozel1903.autokit.core.ManeuverType

/**
 * Turns a [CarImage] name into a [CarIcon]. Return `null` for names you don't know; resolvers can be
 * chained with [then].
 */
public fun interface ImageResolver {
    public fun resolve(image: CarImage): CarIcon?

    /** A resolver that tries this one first and [other] second. */
    public fun then(other: ImageResolver): ImageResolver = ImageResolver { image -> resolve(image) ?: other.resolve(image) }

    public companion object {
        /** Resolves nothing. */
        public val None: ImageResolver = ImageResolver { null }

        /**
         * Resolves names through an explicit map of drawable resources, for example
         * `mapOf("ic_coffee" to R.drawable.ic_coffee)`. The image tint is applied as a custom car color.
         */
        public fun drawables(context: Context, drawables: Map<String, Int>): ImageResolver {
            val appContext = context.applicationContext ?: context
            return ImageResolver { image -> drawables[image.name]?.let { carIcon(appContext, it, image.tint) } }
        }

        /**
         * Resolves the icons that ship with the library: the maneuver arrows used by route lists
         * ([ManeuverIcons]) and [KitIcons.NOW_PLAYING].
         */
        public fun builtIn(context: Context): ImageResolver = drawables(context, KitIcons.drawables)
    }
}

/** Names and resources of the icons that ship with the library. */
public object KitIcons {
    /** Equalizer bars, used for the current item of a media queue. */
    public const val NOW_PLAYING: String = "autokit_now_playing"

    internal val drawables: Map<String, Int> = mapOf(
        NOW_PLAYING to R.drawable.autokit_now_playing,
        "autokit_maneuver_depart" to R.drawable.autokit_maneuver_depart,
        "autokit_maneuver_straight" to R.drawable.autokit_maneuver_straight,
        "autokit_maneuver_turn_left" to R.drawable.autokit_maneuver_turn_left,
        "autokit_maneuver_turn_right" to R.drawable.autokit_maneuver_turn_right,
        "autokit_maneuver_slight_left" to R.drawable.autokit_maneuver_slight_left,
        "autokit_maneuver_slight_right" to R.drawable.autokit_maneuver_slight_right,
        "autokit_maneuver_sharp_left" to R.drawable.autokit_maneuver_sharp_left,
        "autokit_maneuver_sharp_right" to R.drawable.autokit_maneuver_sharp_right,
        "autokit_maneuver_u_turn_left" to R.drawable.autokit_maneuver_u_turn_left,
        "autokit_maneuver_u_turn_right" to R.drawable.autokit_maneuver_u_turn_right,
        "autokit_maneuver_keep_left" to R.drawable.autokit_maneuver_keep_left,
        "autokit_maneuver_keep_right" to R.drawable.autokit_maneuver_keep_right,
        "autokit_maneuver_roundabout" to R.drawable.autokit_maneuver_roundabout,
        "autokit_maneuver_destination" to R.drawable.autokit_maneuver_destination,
    )

    /** The drawable resource for a maneuver arrow. */
    public fun maneuverDrawable(type: ManeuverType): Int = drawables.getValue(ManeuverIcons.nameFor(type))

    /** The drawable resource for a built-in icon name, or `null`. */
    public fun drawable(name: String): Int? = drawables[name]
}

/** A [CarIcon] for a drawable resource, optionally tinted with an ARGB color. */
public fun carIcon(context: Context, drawableRes: Int, tint: Int? = null): CarIcon {
    val builder = CarIcon.Builder(IconCompat.createWithResource(context, drawableRes))
    if (tint != null) builder.setTint(CarColor.createCustom(tint, tint))
    return builder.build()
}
