package io.github.halilozel1903.autokit

import androidx.car.app.CarContext
import androidx.car.app.model.Template
import io.github.halilozel1903.autokit.core.CarAction
import io.github.halilozel1903.autokit.core.CarGrid
import io.github.halilozel1903.autokit.core.CarGridItem
import io.github.halilozel1903.autokit.core.CarList
import io.github.halilozel1903.autokit.core.CarMessage
import io.github.halilozel1903.autokit.core.CarPane
import io.github.halilozel1903.autokit.core.CarPaging
import io.github.halilozel1903.autokit.core.CarRow
import io.github.halilozel1903.autokit.core.paged

/**
 * A list screen from a [CarList] without writing a `Screen` class. Lists longer than the host allows
 * are paged: the last row of a full page is "More" and opens the next page.
 *
 * ```kotlin
 * push(CarListScreen(carContext, content = { stops.toCarList() }) { row -> push(StopScreen(carContext, row.id)) })
 * ```
 *
 * [content] runs on every refresh, so it can read changing state.
 */
public open class CarListScreen(
    carContext: CarContext,
    private val content: KitScreen.() -> CarList,
    private val images: ImageResolver = ImageResolver.None,
    private val pageIndex: Int = 0,
    private val moreTitle: String = "More",
    private val onAction: (KitScreen.(CarAction) -> Unit)? = null,
    private val onRowClick: (KitScreen.(CarRow) -> Unit)? = null,
) : KitScreen(carContext) {

    override val imageResolver: ImageResolver get() = images

    override fun onCreateTemplate(): Template {
        val pages = content().paged(maxItems = limits.maxListItems.coerceAtLeast(2), moreTitle = moreTitle)
        val page = pages[pageIndex.coerceIn(pages.indices)]
        val paged = pages.size > 1
        return templates.list(
            list = page,
            headerAction = headerAction,
            onAction = onAction?.let { action -> { a: CarAction -> action.invoke(this, a) } },
            onRowClick = if (onRowClick == null && !paged) {
                null
            } else {
                { row ->
                    val next = CarPaging.nextPageFor(row.id)
                    if (next != null) {
                        push(CarListScreen(carContext, content, images, next, moreTitle, onAction, onRowClick))
                    } else {
                        onRowClick?.invoke(this, row)
                    }
                }
            },
        )
    }
}

/** A grid screen from a [CarGrid]. [content] runs on every refresh. */
public open class CarGridScreen(
    carContext: CarContext,
    private val content: KitScreen.() -> CarGrid,
    private val images: ImageResolver = ImageResolver.None,
    private val onAction: (KitScreen.(CarAction) -> Unit)? = null,
    private val onItemClick: (KitScreen.(CarGridItem) -> Unit)? = null,
) : KitScreen(carContext) {

    override val imageResolver: ImageResolver get() = images

    override fun onCreateTemplate(): Template = templates.grid(
        grid = content(),
        headerAction = headerAction,
        onAction = onAction?.let { action -> { a: CarAction -> action.invoke(this, a) } },
        onItemClick = onItemClick?.let { click -> { item: CarGridItem -> click.invoke(this, item) } },
    )
}

/** A detail screen from a [CarPane]. [content] runs on every refresh. */
public open class CarPaneScreen(
    carContext: CarContext,
    private val content: KitScreen.() -> CarPane,
    private val images: ImageResolver = ImageResolver.None,
    private val onAction: (KitScreen.(CarAction) -> Unit)? = null,
) : KitScreen(carContext) {

    override val imageResolver: ImageResolver get() = images

    override fun onCreateTemplate(): Template = templates.pane(
        pane = content(),
        headerAction = headerAction,
        onAction = onAction?.let { action -> { a: CarAction -> action.invoke(this, a) } },
    )
}

/** A message screen from a [CarMessage]. [content] runs on every refresh. */
public open class CarMessageScreen(
    carContext: CarContext,
    private val content: KitScreen.() -> CarMessage,
    private val images: ImageResolver = ImageResolver.None,
    private val onAction: (KitScreen.(CarAction) -> Unit)? = null,
) : KitScreen(carContext) {

    override val imageResolver: ImageResolver get() = images

    override fun onCreateTemplate(): Template = templates.message(
        message = content(),
        headerAction = headerAction,
        onAction = onAction?.let { action -> { a: CarAction -> action.invoke(this, a) } },
    )
}
