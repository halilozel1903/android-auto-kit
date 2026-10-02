package io.github.halilozel1903.autokit.core

/**
 * An image by name. The Android layer turns the name into a `CarIcon` (usually a drawable resource)
 * through an image resolver, and the phone preview turns it into a painter. Keeping images as names
 * lets the model stay plain Kotlin and be shared by both.
 *
 * @property name a key that the resolver understands, such as `"ic_coffee"`.
 * @property tint an optional ARGB tint (for example `0xFFFFB74D.toInt()`), or `null` to keep the
 * image colors (hosts may still tint monochrome icons).
 */
public data class CarImage(
    val name: String,
    val tint: Int? = null,
)

/**
 * A button: in a template header (needs an [icon]), in a pane or under a message.
 *
 * @property id identifies the action in click callbacks.
 * @property primary highlights the action. At most one action per template can be primary.
 */
public data class CarAction(
    val id: String,
    val title: String,
    val icon: CarImage? = null,
    val primary: Boolean = false,
)

/**
 * One row of a list or a pane.
 *
 * @property texts lines under the title. Hosts show at most two.
 * @property browsable shows a chevron: the row opens another screen.
 */
public data class CarRow(
    val id: String,
    val title: String,
    val texts: List<String> = emptyList(),
    val image: CarImage? = null,
    val browsable: Boolean = false,
)

/** A screen with a title and a list of rows (`ListTemplate`). */
public data class CarList(
    val title: String,
    val rows: List<CarRow>,
    val actions: List<CarAction> = emptyList(),
    val emptyMessage: String? = null,
)

/** One tile of a grid. Grid items always have an image. */
public data class CarGridItem(
    val id: String,
    val title: String,
    val image: CarImage,
    val text: String? = null,
)

/** A screen of tiles (`GridTemplate`). */
public data class CarGrid(
    val title: String,
    val items: List<CarGridItem>,
    val actions: List<CarAction> = emptyList(),
    val emptyMessage: String? = null,
)

/**
 * A detail screen (`PaneTemplate`): a few non-clickable rows, an optional large image and up to two
 * buttons.
 *
 * @property headerActions icon actions in the header (bookmark, share and so on).
 * @property actions buttons in the pane body.
 */
public data class CarPane(
    val title: String,
    val rows: List<CarRow>,
    val actions: List<CarAction> = emptyList(),
    val image: CarImage? = null,
    val headerActions: List<CarAction> = emptyList(),
)

/** A short message with an optional icon and buttons (`MessageTemplate`): errors, empty states, confirmations. */
public data class CarMessage(
    val title: String,
    val message: String,
    val icon: CarImage? = null,
    val actions: List<CarAction> = emptyList(),
)
