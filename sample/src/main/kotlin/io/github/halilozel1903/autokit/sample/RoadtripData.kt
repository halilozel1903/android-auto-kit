package io.github.halilozel1903.autokit.sample

import io.github.halilozel1903.autokit.KitIcons
import io.github.halilozel1903.autokit.core.CarAction
import io.github.halilozel1903.autokit.core.CarGrid
import io.github.halilozel1903.autokit.core.CarGridItem
import io.github.halilozel1903.autokit.core.CarImage
import io.github.halilozel1903.autokit.core.CarList
import io.github.halilozel1903.autokit.core.CarMessage
import io.github.halilozel1903.autokit.core.CarPane
import io.github.halilozel1903.autokit.core.CarRow
import io.github.halilozel1903.autokit.core.DistanceFormat
import io.github.halilozel1903.autokit.core.ManeuverType
import io.github.halilozel1903.autokit.core.MediaItem
import io.github.halilozel1903.autokit.core.MediaQueue
import io.github.halilozel1903.autokit.core.NavigationStep
import io.github.halilozel1903.autokit.core.Route

/**
 * The fictional Roadtrip app: places along a coastal drive, audio guides and the route to the
 * destination. The car screens and the phone preview both read this model, so they always match.
 */
object RoadtripData {

    // Category colors, used as image tints in the car and in the preview.
    private const val BLUE = 0xFF8AB4F8.toInt()
    private const val ORANGE = 0xFFFFB74D.toInt()
    private const val GREEN = 0xFF81C995.toInt()
    private const val CYAN = 0xFF78D9EC.toInt()
    private const val RED = 0xFFF28B82.toInt()
    private const val PURPLE = 0xFFC58AF9.toInt()

    val routeAction = CarAction("route", "Route", CarImage(RoadtripImages.NAVIGATION))
    val searchAction = CarAction("search", "Search", CarImage(RoadtripImages.SEARCH))

    // Home grid

    val home = CarGrid(
        title = "Roadtrip",
        items = listOf(
            CarGridItem("guides", "Audio guides", CarImage(RoadtripImages.HEADSET, BLUE), "8 stories"),
            CarGridItem("coffee", "Coffee", CarImage(RoadtripImages.COFFEE, ORANGE), "3 nearby"),
            CarGridItem("fuel", "Fuel", CarImage(RoadtripImages.FUEL, GREEN), "2 nearby"),
            CarGridItem("viewpoints", "Viewpoints", CarImage(RoadtripImages.LANDSCAPE, CYAN), "3 nearby"),
            CarGridItem("food", "Food", CarImage(RoadtripImages.RESTAURANT, RED), "3 nearby"),
            CarGridItem("parking", "Parking", CarImage(RoadtripImages.PARKING, PURPLE), "None nearby"),
        ),
        actions = listOf(routeAction),
    )

    // Places

    data class Place(
        val id: String,
        val category: String,
        val name: String,
        val kind: String,
        val distanceMeters: Double,
        val minutes: Int,
        val rating: Double,
        val reviews: Int,
        val hours: String,
        val address: String,
    )

    val places = listOf(
        Place("lookout", "viewpoints", "Lookout Point", "Scenic overlook · Free entry", 2_400.0, 4, 4.8, 1_284, "Open 24 hours", "Coast Road 112, Lighthouse Bay"),
        Place("cliffs", "viewpoints", "Seven Cliffs", "Cliff walk · 20 min loop", 6_800.0, 9, 4.6, 532, "Open until 9 PM", "Cliff Drive 4, Lighthouse Bay"),
        Place("sunset", "viewpoints", "Sunset Point", "Beach viewpoint", 14_200.0, 17, 4.7, 2_010, "Open 24 hours", "Lighthouse Lane 1, Lighthouse Bay"),
        Place("driftwood", "coffee", "Driftwood Coffee", "Espresso bar · Drive-through", 900.0, 2, 4.5, 318, "Open until 6 PM", "Harbor Street 9"),
        Place("tidepool", "coffee", "Tidepool Café", "Café · Pastries", 3_100.0, 5, 4.7, 642, "Open until 5 PM", "Coast Road 40"),
        Place("beacon", "coffee", "Beacon Roasters", "Roastery", 11_500.0, 14, 4.4, 201, "Open until 4 PM", "Lighthouse Lane 22"),
        Place("coastfuel", "fuel", "Coast Fuel", "Fuel · EV chargers · Shop", 1_700.0, 3, 4.1, 96, "Open 24 hours", "Coast Road 3"),
        Place("bayfuel", "fuel", "Bay Station", "Fuel · Car wash", 9_300.0, 11, 4.0, 74, "Open until 11 PM", "Cliff Drive 80"),
        Place("market", "food", "Fishermen's Market", "Seafood · Market hall", 1_200.0, 3, 4.6, 1_720, "Open until 7 PM", "Harbor Street 1"),
        Place("saltpine", "food", "Salt & Pine", "Bistro · Terrace", 5_600.0, 8, 4.5, 488, "Open until 10 PM", "Coast Road 77"),
        Place("keeper", "food", "The Keeper's Table", "Restaurant · Sea view", 13_900.0, 16, 4.8, 903, "Open until 11 PM", "Lighthouse Lane 5"),
    )

    fun place(id: String): Place = places.first { it.id == id }

    fun categoryTitle(category: String): String = home.items.first { it.id == category }.title

    private fun categoryImage(category: String): CarImage = home.items.first { it.id == category }.image

    /** The places of a category, nearest first. */
    fun placesList(category: String): CarList = CarList(
        title = categoryTitle(category),
        rows = places.filter { it.category == category }.sortedBy { it.distanceMeters }.map { place ->
            CarRow(
                id = place.id,
                title = place.name,
                texts = listOf("${DistanceFormat.text(place.distanceMeters)} · ${place.minutes} min · ${place.rating} ★"),
                image = categoryImage(category),
                browsable = true,
            )
        },
        actions = listOf(searchAction),
        emptyMessage = "Nothing nearby",
    )

    /** The detail pane of a place. */
    fun placePane(id: String, saved: Boolean = false): CarPane {
        val place = place(id)
        return CarPane(
            title = place.name,
            rows = listOf(
                CarRow("kind", place.kind, texts = listOf(place.hours)),
                CarRow("distance", "${DistanceFormat.text(place.distanceMeters)} away", texts = listOf("About ${place.minutes} min along the coast")),
                CarRow("rating", "Rated ${place.rating} ★", texts = listOf("%,d reviews".format(java.util.Locale.US, place.reviews))),
                CarRow("address", place.address),
            ),
            actions = listOf(
                CarAction("navigate", "Navigate", CarImage(RoadtripImages.NAVIGATION), primary = true),
                CarAction("save", if (saved) "Saved" else "Save", CarImage(RoadtripImages.STAR)),
            ),
            image = categoryImage(place.category),
        )
    }

    val noParking = CarMessage(
        title = "Parking",
        message = "No free parking within 5 km. Lookout Point has roadside spots 2.4 km ahead.",
        icon = CarImage(RoadtripImages.PARKING, PURPLE),
        actions = listOf(CarAction("lookout", "Show Lookout Point", primary = true), CarAction("back", "Back")),
    )

    // Audio guides

    private fun guide(id: String, title: String, minutes: Int, seconds: Int) = MediaItem(
        id = id,
        title = title,
        artist = "Roadtrip Radio",
        durationMillis = (minutes * 60L + seconds) * 1000,
        image = CarImage(RoadtripImages.HEADSET, BLUE),
    )

    val audioGuides = MediaQueue(
        items = listOf(
            guide("welcome", "Welcome to the Coast Road", 3, 12),
            guide("lighthouse", "The Old Lighthouse", 6, 45),
            guide("harbor", "Harbor Stories", 5, 20),
            guide("whales", "Whales of the Bay", 7, 5),
            guide("market", "Fishermen's Market", 4, 10),
            guide("cliffs", "Legends of the Cliffs", 5, 55),
            guide("sunset", "Sunset Point", 3, 40),
            guide("ferry", "The Last Ferry", 6, 15),
        ),
    ).skipTo("lighthouse")

    /** The audio guides as the car shows them, with shuffle and next in the header. */
    fun audioGuidesList(queue: MediaQueue = audioGuides): CarList = queue
        .toCarList("Audio guides", nowPlayingImage = CarImage(KitIcons.NOW_PLAYING))
        .copy(
            actions = listOf(
                CarAction("shuffle", if (queue.isShuffled) "Shuffle off" else "Shuffle", CarImage(RoadtripImages.SHUFFLE)),
                CarAction("next", "Next", CarImage(RoadtripImages.NEXT)),
            ),
        )

    // Route

    val route = Route(
        destination = "Lighthouse Bay",
        steps = listOf(
            NavigationStep(ManeuverType.DEPART, 0.0, "Harbor Street"),
            NavigationStep(ManeuverType.TURN_RIGHT, 300.0, "Coast Road"),
            NavigationStep(ManeuverType.ROUNDABOUT, 1_200.0, "Cliff Drive", roundaboutExit = 2),
            NavigationStep(ManeuverType.SLIGHT_LEFT, 4_500.0, "Lighthouse Lane"),
            NavigationStep(ManeuverType.DESTINATION_RIGHT, 850.0, "Lighthouse Bay"),
        ),
    )
}
