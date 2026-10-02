package io.github.halilozel1903.autokit.sample.car

import androidx.car.app.CarContext
import androidx.car.app.model.Template
import io.github.halilozel1903.autokit.CarListScreen
import io.github.halilozel1903.autokit.CarMessageScreen
import io.github.halilozel1903.autokit.ImageResolver
import io.github.halilozel1903.autokit.KitScreen
import io.github.halilozel1903.autokit.core.CarPaging
import io.github.halilozel1903.autokit.core.EnglishManeuverFormat
import io.github.halilozel1903.autokit.core.ManeuverFormat
import io.github.halilozel1903.autokit.core.MediaQueue
import io.github.halilozel1903.autokit.core.paged
import io.github.halilozel1903.autokit.sample.RoadtripData
import io.github.halilozel1903.autokit.sample.RoadtripImages

/** Screens of the sample share the Roadtrip image names. */
abstract class RoadtripScreen(carContext: CarContext) : KitScreen(carContext) {
    override val imageResolver: ImageResolver get() = RoadtripImages.resolver(carContext)
}

/** The root screen: a grid of categories, with the route in the header. */
class HomeScreen(carContext: CarContext) : RoadtripScreen(carContext) {
    override fun onCreateTemplate(): Template = templates.grid(
        grid = RoadtripData.home,
        headerAction = headerAction,
        onAction = { action -> if (action.id == "route") push(RouteScreen(carContext)) },
    ) { item ->
        when (item.id) {
            "guides" -> push(AudioGuidesScreen(carContext))
            "parking" -> push(noParkingScreen(carContext))
            else -> push(placesScreen(carContext, item.id))
        }
    }
}

/** A category's places as a list; rows open the place details. Built with the kit's [CarListScreen]. */
fun placesScreen(carContext: CarContext, category: String): CarListScreen = CarListScreen(
    carContext = carContext,
    content = { RoadtripData.placesList(category) },
    images = RoadtripImages.resolver(carContext),
    onAction = { toast("Search is not part of the sample") },
    onRowClick = { row -> push(PlaceScreen(carContext, row.id)) },
)

/** An empty state as a message, with a way out. */
fun noParkingScreen(carContext: CarContext): CarMessageScreen = CarMessageScreen(
    carContext = carContext,
    content = { RoadtripData.noParking },
    images = RoadtripImages.resolver(carContext),
) { action ->
    when (action.id) {
        "lookout" -> replaceWith(PlaceScreen(carContext, "lookout"))
        else -> pop()
    }
}

/** A place: details in a pane, with Navigate and Save buttons. */
class PlaceScreen(carContext: CarContext, private val placeId: String) : RoadtripScreen(carContext) {
    private var saved = false

    override fun onCreateTemplate(): Template = templates.pane(RoadtripData.placePane(placeId, saved), headerAction) { action ->
        when (action.id) {
            "navigate" -> push(RouteScreen(carContext))
            "save" -> {
                saved = !saved
                toast(if (saved) "Saved to your trip" else "Removed from your trip")
                refresh()
            }
        }
    }
}

/**
 * Audio guides: a media queue as a list. Lists longer than the host allows are paged with a
 * "More" row; tapping a guide plays it, the header has shuffle and next.
 */
class AudioGuidesScreen(carContext: CarContext, private val pageIndex: Int = 0) : RoadtripScreen(carContext) {

    override fun onCreateTemplate(): Template {
        val queue = Player.queue
        val pages = RoadtripData.audioGuidesList(queue).paged(maxItems = limits.maxListItems.coerceAtLeast(2))
        val page = pages[pageIndex.coerceIn(pages.indices)]
        return templates.list(
            list = page,
            headerAction = headerAction,
            onAction = { action ->
                Player.queue = when (action.id) {
                    "shuffle" -> queue.withShuffle(!queue.isShuffled)
                    else -> queue.next()
                }
                toast("Playing ${Player.queue.current?.title}")
                refresh()
            },
        ) { row ->
            val nextPage = CarPaging.nextPageFor(row.id)
            if (nextPage != null) {
                push(AudioGuidesScreen(carContext, nextPage))
            } else {
                Player.queue = queue.skipTo(row.id)
                toast("Playing ${Player.queue.current?.title}")
                refresh()
            }
        }
    }

    /** The sample doesn't play audio; it only keeps the queue so the screens can show it. */
    object Player {
        var queue: MediaQueue = RoadtripData.audioGuides
    }
}

/** The route as a navigation style list (POI apps can't use the NavigationTemplate). */
class RouteScreen(carContext: CarContext) : RoadtripScreen(carContext) {
    private val format: ManeuverFormat = EnglishManeuverFormat()

    override fun onCreateTemplate(): Template = templates.route(
        route = RoadtripData.route,
        format = format,
        headerAction = headerAction,
    ) { index -> toast(format.instruction(RoadtripData.route.steps[index])) }
}
