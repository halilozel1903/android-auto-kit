package io.github.halilozel1903.autokit

import android.content.Context
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.versioning.CarAppApiLevels
import io.github.halilozel1903.autokit.core.CarAction
import io.github.halilozel1903.autokit.core.CarGrid
import io.github.halilozel1903.autokit.core.CarGridItem
import io.github.halilozel1903.autokit.core.CarImage
import io.github.halilozel1903.autokit.core.CarLimits
import io.github.halilozel1903.autokit.core.CarList
import io.github.halilozel1903.autokit.core.CarMessage
import io.github.halilozel1903.autokit.core.CarPane
import io.github.halilozel1903.autokit.core.CarRow
import io.github.halilozel1903.autokit.core.DistanceUnits
import io.github.halilozel1903.autokit.core.ManeuverFormat
import io.github.halilozel1903.autokit.core.MediaItem
import io.github.halilozel1903.autokit.core.MediaQueue
import io.github.halilozel1903.autokit.core.Route
import io.github.halilozel1903.autokit.core.ValidationResult
import io.github.halilozel1903.autokit.core.clippedTo
import io.github.halilozel1903.autokit.core.validate

/**
 * Builds Car App Library templates from the `autokit-core` model.
 *
 * - Content is validated against [limits]. In [strict] mode invalid content throws a
 *   `CarModelException`; otherwise it is clipped to fit and a warning is logged, so a long server list
 *   never crashes the car screen.
 * - Hosts with car API level 7 or newer get the new `Header` (title, start action and icon actions).
 *   Older hosts get the deprecated title, header action and action strip, so one call works on every car.
 * - Images are resolved through [images], then through the icons that ship with the library.
 *
 * `KitScreen` creates one for you (`templates`); create one yourself in plain `Screen`s.
 *
 * @param carApiLevel the host's car API level, usually `carContext.carAppApiLevel`.
 */
public class KitTemplates(
    context: Context,
    public val carApiLevel: Int,
    images: ImageResolver = ImageResolver.None,
    public val limits: CarLimits = CarLimits.Default,
    public val strict: Boolean = false,
) {
    /** Builds templates for the host [carContext] is connected to. */
    public constructor(
        carContext: CarContext,
        images: ImageResolver = ImageResolver.None,
        limits: CarLimits = CarLimits.Default,
        strict: Boolean = false,
    ) : this(carContext, carContext.carApiLevelOrLatest(), images, limits, strict)

    private val images: ImageResolver = images.then(ImageResolver.builtIn(context))

    /** Whether the host understands `Header` (car API level 7+). */
    public val supportsHeader: Boolean get() = carApiLevel >= CarAppApiLevels.LEVEL_7

    /** Resolves [image] with the configured resolvers, or `null`. */
    public fun icon(image: CarImage?): CarIcon? = image?.let { images.resolve(it) }

    // List

    /**
     * A `ListTemplate` with one row per [CarRow].
     *
     * @param headerAction `Action.BACK`, `Action.APP_ICON` or `null`.
     * @param onAction called for header actions (they need icons).
     * @param onRowClick called for row clicks; `null` makes rows plain text. Browsable rows only show
     * their chevron when there is a click handler.
     */
    @Suppress("DEPRECATION")
    public fun list(
        list: CarList,
        headerAction: Action? = Action.BACK,
        onAction: ((CarAction) -> Unit)? = null,
        onRowClick: ((CarRow) -> Unit)? = null,
    ): ListTemplate {
        val content = prepare("list", list.validate(limits)) { list.clippedTo(limits) }
        val items = ItemList.Builder()
        content.rows.forEach { items.addItem(row(it, onRowClick)) }
        items.setNoItemsMessage(content.emptyMessage ?: "Nothing here yet")
        val builder = ListTemplate.Builder().setSingleList(items.build())
        if (supportsHeader) {
            builder.setHeader(header(content.title, headerAction, content.actions, onAction))
        } else {
            legacyHeader(content.title, headerAction, content.actions, onAction, builder::setTitle, builder::setHeaderAction, builder::setActionStrip)
        }
        return builder.build()
    }

    /** A list that shows a loading spinner. */
    @Suppress("DEPRECATION")
    public fun loading(title: String, headerAction: Action? = Action.BACK): ListTemplate {
        val builder = ListTemplate.Builder().setLoading(true)
        if (supportsHeader) {
            builder.setHeader(header(title, headerAction, emptyList(), null))
        } else {
            legacyHeader(title, headerAction, emptyList(), null, builder::setTitle, builder::setHeaderAction, builder::setActionStrip)
        }
        return builder.build()
    }

    /**
     * A media queue as a list: items in play order, the current one marked "Now playing" with an
     * equalizer icon. Row clicks report the [MediaItem].
     */
    public fun mediaQueue(
        queue: MediaQueue,
        title: String,
        headerAction: Action? = Action.BACK,
        nowPlayingLabel: String = "Now playing",
        actions: List<CarAction> = emptyList(),
        onAction: ((CarAction) -> Unit)? = null,
        onItemClick: ((MediaItem) -> Unit)? = null,
    ): ListTemplate {
        val carList = queue.toCarList(title, nowPlayingLabel, CarImage(KitIcons.NOW_PLAYING)).copy(actions = actions)
        val byId = queue.items.associateBy { it.id }
        return this.list(carList, headerAction, onAction, onItemClick?.let { click -> { row: CarRow -> byId[row.id]?.let(click) } })
    }

    /**
     * A route as a navigation style list: one row per step with a maneuver arrow, the cue as the
     * title ("In 300 m, turn right") and the road as the text. Works in any app category; use
     * [routingInfo] inside a `NavigationTemplate` in navigation apps.
     */
    public fun route(
        route: Route,
        title: String = "Route to ${route.destination}",
        units: DistanceUnits = DistanceUnits.METRIC,
        format: ManeuverFormat = ManeuverFormat.English,
        headerAction: Action? = Action.BACK,
        actions: List<CarAction> = emptyList(),
        onAction: ((CarAction) -> Unit)? = null,
        onStepClick: ((index: Int) -> Unit)? = null,
    ): ListTemplate {
        val carList = route.toCarList(title, units, format).copy(actions = actions)
        val stepIndex = carList.rows.withIndex().associate { (index, row) -> row.id to index }
        return this.list(carList, headerAction, onAction, onStepClick?.let { click -> { row: CarRow -> stepIndex[row.id]?.let(click) } })
    }

    // Grid

    /** A `GridTemplate` with one tile per [CarGridItem]. Unresolved images fall back to the app icon. */
    @Suppress("DEPRECATION")
    public fun grid(
        grid: CarGrid,
        headerAction: Action? = Action.BACK,
        onAction: ((CarAction) -> Unit)? = null,
        onItemClick: ((CarGridItem) -> Unit)? = null,
    ): GridTemplate {
        val content = prepare("grid", grid.validate(limits)) { grid.clippedTo(limits) }
        val items = ItemList.Builder()
        content.items.forEach { item ->
            val tile = GridItem.Builder()
                .setTitle(item.title)
                .setImage(icon(item.image) ?: missingImage(item.image), GridItem.IMAGE_TYPE_LARGE)
            item.text?.let { tile.setText(it) }
            if (onItemClick != null) tile.setOnClickListener { onItemClick(item) }
            items.addItem(tile.build())
        }
        items.setNoItemsMessage(content.emptyMessage ?: "Nothing here yet")
        val builder = GridTemplate.Builder().setSingleList(items.build())
        if (supportsHeader) {
            builder.setHeader(header(content.title, headerAction, content.actions, onAction))
        } else {
            legacyHeader(content.title, headerAction, content.actions, onAction, builder::setTitle, builder::setHeaderAction, builder::setActionStrip)
        }
        return builder.build()
    }

    // Pane

    /**
     * A `PaneTemplate`: rows of details, an optional large image (car API level 4+) and up to two
     * buttons. The primary action is highlighted on car API level 4+.
     */
    @Suppress("DEPRECATION")
    public fun pane(
        pane: CarPane,
        headerAction: Action? = Action.BACK,
        onAction: ((CarAction) -> Unit)? = null,
    ): PaneTemplate {
        val content = prepare("pane", pane.validate(limits)) { pane.clippedTo(limits) }
        val body = Pane.Builder()
        content.rows.forEach { body.addRow(row(it.copy(browsable = false), null)) }
        content.actions.forEach { body.addAction(bodyAction(it, onAction)) }
        if (carApiLevel >= CarAppApiLevels.LEVEL_4) icon(content.image)?.let { body.setImage(it) }
        val builder = PaneTemplate.Builder(body.build())
        if (supportsHeader) {
            builder.setHeader(header(content.title, headerAction, content.headerActions, onAction))
        } else {
            legacyHeader(content.title, headerAction, content.headerActions, onAction, builder::setTitle, builder::setHeaderAction, builder::setActionStrip)
        }
        return builder.build()
    }

    // Message

    /** A `MessageTemplate` for empty states, errors and confirmations. */
    public fun message(
        message: CarMessage,
        headerAction: Action? = Action.BACK,
        onAction: ((CarAction) -> Unit)? = null,
    ): MessageTemplate {
        val content = prepare("message", message.validate(limits)) { message.clippedTo(limits) }
        val builder = MessageTemplate.Builder(content.message)
        icon(content.icon)?.let { builder.setIcon(it) }
        content.actions.forEach { builder.addAction(bodyAction(it, onAction)) }
        applyMessageHeader(builder, content.title, headerAction)
        return builder.build()
    }

    /** An error message with the exception attached as a debug message (shown on debug hosts only). */
    public fun error(
        error: Throwable,
        title: String = "Something went wrong",
        message: String = "Please try again in a moment.",
        headerAction: Action? = Action.BACK,
    ): MessageTemplate {
        val builder = MessageTemplate.Builder(message).setIcon(CarIcon.ERROR).setDebugMessage(error)
        applyMessageHeader(builder, title, headerAction)
        return builder.build()
    }

    @Suppress("DEPRECATION")
    private fun applyMessageHeader(builder: MessageTemplate.Builder, title: String, headerAction: Action?) {
        if (supportsHeader) {
            builder.setHeader(header(title, headerAction, emptyList(), null))
        } else {
            builder.setTitle(title)
            headerAction?.let { builder.setHeaderAction(it) }
        }
    }

    // Building blocks

    /** A `Row` for [row]. Pane rows must not have a click handler. */
    public fun row(row: CarRow, onClick: ((CarRow) -> Unit)?): Row {
        val builder = Row.Builder().setTitle(row.title)
        row.texts.forEach { builder.addText(it) }
        icon(row.image)?.let { builder.setImage(it) }
        if (onClick != null) {
            builder.setOnClickListener { onClick(row) }
            if (row.browsable) builder.setBrowsable(true)
        }
        return builder.build()
    }

    /** A header `Header` (car API level 7+) with icon-only end actions. */
    public fun header(title: String, startAction: Action?, actions: List<CarAction>, onAction: ((CarAction) -> Unit)?): Header {
        val builder = Header.Builder().setTitle(title)
        startAction?.let { builder.setStartHeaderAction(it) }
        actions.mapNotNull { iconAction(it, onAction) }.forEach { builder.addEndHeaderAction(it) }
        return builder.build()
    }

    /** An `ActionStrip` of icon actions for templates on older hosts (or map and navigation templates). */
    public fun actionStrip(actions: List<CarAction>, onAction: ((CarAction) -> Unit)?): ActionStrip? {
        val built = actions.mapNotNull { iconAction(it, onAction) }
        if (built.isEmpty()) return null
        val builder = ActionStrip.Builder()
        built.forEach { builder.addAction(it) }
        return builder.build()
    }

    /** A titled body `Action` (pane and message buttons); primary actions are flagged on car API level 4+. */
    public fun bodyAction(action: CarAction, onAction: ((CarAction) -> Unit)?): Action {
        val builder = Action.Builder().setTitle(action.title)
        icon(action.icon)?.let { builder.setIcon(it) }
        builder.setOnClickListener { onAction?.invoke(action) }
        if (action.primary && carApiLevel >= CarAppApiLevels.LEVEL_4) builder.setFlags(Action.FLAG_PRIMARY)
        return builder.build()
    }

    private fun iconAction(action: CarAction, onAction: ((CarAction) -> Unit)?): Action? {
        val icon = icon(action.icon)
        if (icon == null) {
            Log.w(TAG, "Header action \"${action.id}\" has no resolvable icon and is skipped")
            return null
        }
        return Action.Builder().setIcon(icon).setOnClickListener { onAction?.invoke(action) }.build()
    }

    private fun legacyHeader(
        title: String,
        headerAction: Action?,
        actions: List<CarAction>,
        onAction: ((CarAction) -> Unit)?,
        setTitle: (CharSequence) -> Any,
        setHeaderAction: (Action) -> Any,
        setActionStrip: (ActionStrip) -> Any,
    ) {
        setTitle(title)
        headerAction?.let { setHeaderAction(it) }
        actionStrip(actions, onAction)?.let { setActionStrip(it) }
    }

    private fun missingImage(image: CarImage): CarIcon {
        Log.w(TAG, "No image for \"${image.name}\", using the app icon")
        return CarIcon.APP_ICON
    }

    private inline fun <T> prepare(what: String, result: ValidationResult, clip: () -> T): T {
        if (!result.isValid) {
            if (strict) result.orThrow()
            Log.w(TAG, "Clipping $what to fit the host: $result")
        }
        return clip()
    }

    private companion object {
        const val TAG = "AutoKit"
    }
}
