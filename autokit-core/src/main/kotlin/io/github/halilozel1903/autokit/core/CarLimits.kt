package io.github.halilozel1903.autokit.core

/**
 * The limits a car screen is checked and clipped against.
 *
 * The list, grid and pane defaults (6, 6 and 4) are the Car App Library's own fallback content limits,
 * the values every host accepts. The Android layer reads the real limits from the host at runtime
 * (`ConstraintManager`, car API level 2+), and many cars allow more rows. Header actions need an icon and are limited to two.
 * Text lengths are not enforced by hosts (long text is truncated on screen); the kit uses them to
 * catch text that would not be readable at a glance.
 */
public data class CarLimits(
    val maxListItems: Int = DEFAULT_LIST_ITEMS,
    val maxGridItems: Int = DEFAULT_GRID_ITEMS,
    val maxPaneRows: Int = DEFAULT_PANE_ROWS,
    val maxPaneActions: Int = 2,
    val maxHeaderActions: Int = 2,
    val maxMessageActions: Int = 2,
    val maxRowTexts: Int = 2,
    val maxTitleLength: Int = 80,
    val maxTextLength: Int = 120,
) {
    init {
        require(maxListItems >= 1 && maxGridItems >= 1 && maxPaneRows >= 1) { "Item limits must be at least 1" }
        require(maxPaneActions >= 0 && maxHeaderActions >= 0 && maxMessageActions >= 0) { "Action limits must not be negative" }
        require(maxRowTexts >= 1 && maxTitleLength >= 1 && maxTextLength >= 1) { "Text limits must be at least 1" }
    }

    public companion object {
        public const val DEFAULT_LIST_ITEMS: Int = 6
        public const val DEFAULT_GRID_ITEMS: Int = 6
        public const val DEFAULT_PANE_ROWS: Int = 4

        /** The conservative defaults. */
        public val Default: CarLimits = CarLimits()

        /**
         * Limits with the host's content limits. Values below 1 (unknown) keep the defaults.
         */
        public fun fromHost(listItems: Int, gridItems: Int, paneRows: Int): CarLimits = CarLimits(
            maxListItems = listItems.takeIf { it >= 1 } ?: DEFAULT_LIST_ITEMS,
            maxGridItems = gridItems.takeIf { it >= 1 } ?: DEFAULT_GRID_ITEMS,
            maxPaneRows = paneRows.takeIf { it >= 1 } ?: DEFAULT_PANE_ROWS,
        )
    }
}

/** Shortens [text] to [maxLength] characters, ending with an ellipsis when it was cut. */
public fun ellipsize(text: String, maxLength: Int): String {
    require(maxLength >= 1) { "maxLength must be at least 1" }
    if (text.length <= maxLength) return text
    if (maxLength == 1) return "…"
    return text.take(maxLength - 1).trimEnd() + "…"
}

private fun CarLimits.clipRow(row: CarRow): CarRow = row.copy(
    title = ellipsize(row.title, maxTitleLength),
    texts = row.texts.take(maxRowTexts).map { ellipsize(it, maxTextLength) },
)

private fun CarLimits.clipActions(actions: List<CarAction>, max: Int): List<CarAction> {
    var primarySeen = false
    return actions.take(max).map { action ->
        val primary = action.primary && !primarySeen
        if (action.primary) primarySeen = true
        action.copy(title = ellipsize(action.title, maxTitleLength), primary = primary)
    }
}

/**
 * Returns a copy that fits [limits]: extra rows and actions are dropped, extra lines are dropped and
 * long text is ellipsized. Use it when showing content you don't control (search results, a server
 * feed); use [validate] in tests to find content that would be clipped.
 */
public fun CarList.clippedTo(limits: CarLimits = CarLimits.Default): CarList = copy(
    title = ellipsize(title, limits.maxTitleLength),
    rows = rows.take(limits.maxListItems).map { limits.clipRow(it) },
    actions = limits.clipActions(actions.filter { it.icon != null }, limits.maxHeaderActions),
)

/** Returns a copy that fits [limits]. See [CarList.clippedTo]. */
public fun CarGrid.clippedTo(limits: CarLimits = CarLimits.Default): CarGrid = copy(
    title = ellipsize(title, limits.maxTitleLength),
    items = items.take(limits.maxGridItems).map { item ->
        item.copy(
            title = ellipsize(item.title, limits.maxTitleLength),
            text = item.text?.let { ellipsize(it, limits.maxTextLength) },
        )
    },
    actions = limits.clipActions(actions.filter { it.icon != null }, limits.maxHeaderActions),
)

/** Returns a copy that fits [limits]. See [CarList.clippedTo]. */
public fun CarPane.clippedTo(limits: CarLimits = CarLimits.Default): CarPane = copy(
    title = ellipsize(title, limits.maxTitleLength),
    rows = rows.take(limits.maxPaneRows).map { limits.clipRow(it).copy(browsable = false) },
    actions = limits.clipActions(actions, limits.maxPaneActions),
    headerActions = limits.clipActions(headerActions.filter { it.icon != null }, limits.maxHeaderActions),
)

/** Returns a copy that fits [limits]. See [CarList.clippedTo]. */
public fun CarMessage.clippedTo(limits: CarLimits = CarLimits.Default): CarMessage = copy(
    title = ellipsize(title, limits.maxTitleLength),
    actions = limits.clipActions(actions, limits.maxMessageActions),
)
