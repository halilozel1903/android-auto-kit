package io.github.halilozel1903.autokit.core

/**
 * A reason a car screen would be rejected by the Car App Library or clipped by the host.
 *
 * [path] points at the offending element, for example `"list.rows[7]"` or `"pane.actions"`.
 */
public sealed class ValidationError {
    public abstract val path: String

    /** A human readable description, used in exception messages and logs. */
    public abstract val message: String

    /** More rows, tiles or steps than the template allows. */
    public data class TooManyItems(override val path: String, val count: Int, val max: Int) : ValidationError() {
        override val message: String get() = "$path has $count items, the limit is $max"
    }

    /** More buttons than the template allows. */
    public data class TooManyActions(override val path: String, val count: Int, val max: Int) : ValidationError() {
        override val message: String get() = "$path has $count actions, the limit is $max"
    }

    /** More than one primary action in one template. */
    public data class MultiplePrimaryActions(override val path: String, val count: Int) : ValidationError() {
        override val message: String get() = "$path has $count primary actions, only one is allowed"
    }

    /** A header action without an icon. Hosts require icons for header actions. */
    public data class MissingActionIcon(override val path: String, val actionId: String) : ValidationError() {
        override val message: String get() = "$path action \"$actionId\" needs an icon"
    }

    /** More lines under a row title than hosts show. */
    public data class TooManyRowTexts(override val path: String, val count: Int, val max: Int) : ValidationError() {
        override val message: String get() = "$path has $count lines of text, the limit is $max"
    }

    /** A title or text longer than the configured limit. */
    public data class TextTooLong(override val path: String, val length: Int, val max: Int) : ValidationError() {
        override val message: String get() = "$path is $length characters long, the limit is $max"
    }

    /** A required title or text that is empty or only whitespace. */
    public data class BlankText(override val path: String) : ValidationError() {
        override val message: String get() = "$path must not be blank"
    }

    /** Two rows, tiles or actions with the same id, so click callbacks can't tell them apart. */
    public data class DuplicateId(override val path: String, val id: String) : ValidationError() {
        override val message: String get() = "$path contains the id \"$id\" more than once"
    }

    /** A template that needs content (a pane without rows, a route without steps). */
    public data class EmptyContent(override val path: String) : ValidationError() {
        override val message: String get() = "$path must not be empty"
    }

    /** A value outside its allowed range (a negative distance, a roundabout without an exit number). */
    public data class InvalidValue(override val path: String, val reason: String) : ValidationError() {
        override val message: String get() = "$path: $reason"
    }
}

/** The outcome of validating a car screen. */
public data class ValidationResult(val errors: List<ValidationError>) {
    val isValid: Boolean get() = errors.isEmpty()

    /** Throws a [CarModelException] listing every error, or does nothing when valid. */
    public fun orThrow() {
        if (errors.isNotEmpty()) throw CarModelException(errors)
    }

    override fun toString(): String =
        if (isValid) "ValidationResult(valid)" else "ValidationResult(${errors.joinToString("; ") { it.message }})"
}

/** Thrown by [ValidationResult.orThrow] and by the Android builders in strict mode. */
public class CarModelException(public val errors: List<ValidationError>) :
    IllegalArgumentException(errors.joinToString(separator = "\n", prefix = "Invalid car screen:\n") { "- ${it.message}" })

private class Checker(private val limits: CarLimits) {
    val errors = mutableListOf<ValidationError>()

    fun title(path: String, value: String) {
        if (value.isBlank()) errors += ValidationError.BlankText(path)
        else if (value.length > limits.maxTitleLength) errors += ValidationError.TextTooLong(path, value.length, limits.maxTitleLength)
    }

    fun text(path: String, value: String) {
        if (value.isBlank()) errors += ValidationError.BlankText(path)
        else if (value.length > limits.maxTextLength) errors += ValidationError.TextTooLong(path, value.length, limits.maxTextLength)
    }

    fun count(path: String, count: Int, max: Int) {
        if (count > max) errors += ValidationError.TooManyItems(path, count, max)
    }

    fun ids(path: String, ids: List<String>) {
        val seen = HashSet<String>()
        ids.filterNot { seen.add(it) }.distinct().forEach { errors += ValidationError.DuplicateId(path, it) }
    }

    fun row(path: String, row: CarRow) {
        title("$path.title", row.title)
        if (row.texts.size > limits.maxRowTexts) errors += ValidationError.TooManyRowTexts(path, row.texts.size, limits.maxRowTexts)
        row.texts.forEachIndexed { i, t -> text("$path.texts[$i]", t) }
    }

    fun actions(path: String, actions: List<CarAction>, max: Int, requireIcons: Boolean) {
        if (actions.size > max) errors += ValidationError.TooManyActions(path, actions.size, max)
        val primary = actions.count { it.primary }
        if (primary > 1) errors += ValidationError.MultiplePrimaryActions(path, primary)
        actions.forEachIndexed { i, action ->
            if (requireIcons && action.icon == null) errors += ValidationError.MissingActionIcon(path, action.id)
            // An icon-only header action may have an empty title: it is used for accessibility only.
            if (!requireIcons || action.title.isNotEmpty()) title("$path[$i].title", action.title)
        }
        ids(path, actions.map { it.id })
    }
}

/** Checks the list against [limits]. */
public fun CarList.validate(limits: CarLimits = CarLimits.Default): ValidationResult = with(Checker(limits)) {
    title("list.title", title)
    count("list.rows", rows.size, limits.maxListItems)
    rows.forEachIndexed { i, row -> row("list.rows[$i]", row) }
    ids("list.rows", rows.map { it.id })
    actions("list.actions", actions, limits.maxHeaderActions, requireIcons = true)
    ValidationResult(errors.toList())
}

/** Checks the grid against [limits]. */
public fun CarGrid.validate(limits: CarLimits = CarLimits.Default): ValidationResult = with(Checker(limits)) {
    title("grid.title", title)
    count("grid.items", items.size, limits.maxGridItems)
    items.forEachIndexed { i, item ->
        title("grid.items[$i].title", item.title)
        item.text?.let { text("grid.items[$i].text", it) }
        if (item.image.name.isBlank()) errors += ValidationError.BlankText("grid.items[$i].image")
    }
    ids("grid.items", items.map { it.id })
    actions("grid.actions", actions, limits.maxHeaderActions, requireIcons = true)
    ValidationResult(errors.toList())
}

/** Checks the pane against [limits]. A pane needs at least one row. */
public fun CarPane.validate(limits: CarLimits = CarLimits.Default): ValidationResult = with(Checker(limits)) {
    title("pane.title", title)
    if (rows.isEmpty()) errors += ValidationError.EmptyContent("pane.rows")
    count("pane.rows", rows.size, limits.maxPaneRows)
    rows.forEachIndexed { i, row -> row("pane.rows[$i]", row) }
    actions("pane.actions", actions, limits.maxPaneActions, requireIcons = false)
    actions("pane.headerActions", headerActions, limits.maxHeaderActions, requireIcons = true)
    ValidationResult(errors.toList())
}

/** Checks the message against [limits]. Message text may be longer than row text. */
public fun CarMessage.validate(limits: CarLimits = CarLimits.Default): ValidationResult = with(Checker(limits)) {
    title("message.title", title)
    if (message.isBlank()) errors += ValidationError.BlankText("message.message")
    actions("message.actions", actions, limits.maxMessageActions, requireIcons = false)
    ValidationResult(errors.toList())
}

/** Checks the route: at least one step, finite non-negative distances and exit numbers for roundabouts. */
public fun Route.validate(limits: CarLimits = CarLimits.Default): ValidationResult = with(Checker(limits)) {
    title("route.destination", destination)
    if (steps.isEmpty()) errors += ValidationError.EmptyContent("route.steps")
    steps.forEachIndexed { i, step ->
        val path = "route.steps[$i]"
        if (!step.distanceMeters.isFinite() || step.distanceMeters < 0) {
            errors += ValidationError.InvalidValue("$path.distanceMeters", "must be a finite distance of 0 or more, was ${step.distanceMeters}")
        }
        if (step.maneuver == ManeuverType.ROUNDABOUT && (step.roundaboutExit ?: 0) < 1) {
            errors += ValidationError.InvalidValue("$path.roundaboutExit", "a roundabout needs an exit number of 1 or more")
        }
        step.road?.let { text("$path.road", it) }
    }
    ValidationResult(errors.toList())
}
