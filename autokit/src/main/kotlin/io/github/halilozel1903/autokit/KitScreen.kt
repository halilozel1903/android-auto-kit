package io.github.halilozel1903.autokit

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.Template
import androidx.car.app.versioning.CarAppApiLevels
import io.github.halilozel1903.autokit.core.CarLimits

/**
 * A `Screen` with the usual helpers:
 *
 * - [templates] builds templates from the core model with the host's API level and content limits.
 * - [headerAction] is `Action.BACK` when there is a screen to go back to and `Action.APP_ICON` on the root.
 * - [push], [pushForResult], [pop], [popToRoot], [popTo], [replaceWith] and [toast] wrap the screen
 *   manager and toasts.
 * - Exceptions while building a template show an error message instead of crashing the car screen
 *   (rethrown in [strict] mode).
 *
 * ```kotlin
 * class StopsScreen(carContext: CarContext) : KitScreen(carContext) {
 *     override fun onCreateTemplate() = templates.list(stops.toCarList(), headerAction) { row ->
 *         push(StopScreen(carContext, row.id))
 *     }
 * }
 * ```
 */
public abstract class KitScreen(carContext: CarContext) : Screen(carContext) {

    /** Resolves `CarImage` names to icons. The library's own icons are always available. */
    protected open val imageResolver: ImageResolver get() = ImageResolver.None

    /** Throw on content that doesn't fit the host instead of clipping it. Handy in debug builds and tests. */
    protected open val strict: Boolean get() = false

    /** The host's car API level (the latest level when no host is attached, as in unit tests). */
    public val carApiLevel: Int get() = carContext.carApiLevelOrLatest()

    /** The host's list, grid and pane limits, read once per screen. */
    public val limits: CarLimits by lazy { carContext.hostLimits() }

    /** Template builders for this screen's host. */
    public val templates: KitTemplates by lazy { KitTemplates(carContext, carApiLevel, imageResolver, limits, strict) }

    /** `Action.BACK` when a screen is below this one, `Action.APP_ICON` on the root screen. */
    public val headerAction: Action get() = if (screenManager.stackSize > 1) Action.BACK else Action.APP_ICON

    /** Builds this screen's template. Called again after [refresh]. */
    public abstract fun onCreateTemplate(): Template

    final override fun onGetTemplate(): Template = try {
        onCreateTemplate()
    } catch (e: RuntimeException) {
        if (strict) throw e
        Log.e(TAG, "Could not build the template of ${javaClass.simpleName}", e)
        onTemplateError(e)
    }

    /** The template shown when [onCreateTemplate] throws. */
    protected open fun onTemplateError(error: RuntimeException): Template = templates.error(error, headerAction = headerAction)

    /** Shows [screen] on top of this one. */
    public fun push(screen: Screen) {
        screenManager.push(screen)
    }

    /** Shows [screen] and calls [onResult] with what it passes to [finishWithResult] (or `null`). */
    public fun pushForResult(screen: Screen, onResult: (Any?) -> Unit) {
        screenManager.pushForResult(screen) { result -> onResult(result) }
    }

    /** Goes back one screen. */
    public fun pop() {
        screenManager.pop()
    }

    /** Goes back to the first screen. */
    public fun popToRoot() {
        screenManager.popToRoot()
    }

    /** Goes back to the screen with [marker] (see `Screen.setMarker`). */
    public fun popTo(marker: String) {
        screenManager.popTo(marker)
    }

    /** Shows [screen] instead of this one, so back skips this screen. */
    public fun replaceWith(screen: Screen) {
        screenManager.push(screen)
        screenManager.remove(this)
    }

    /** Sets the result for [pushForResult] and closes this screen. */
    public fun finishWithResult(result: Any?) {
        setResult(result)
        finish()
    }

    /** Shows a short (or long) toast on the car screen. */
    public fun toast(text: CharSequence, long: Boolean = false) {
        CarToast.makeText(carContext, text, if (long) CarToast.LENGTH_LONG else CarToast.LENGTH_SHORT).show()
    }

    /** Asks the host for a new template. Hosts limit how often a screen may refresh while driving. */
    public fun refresh() {
        invalidate()
    }

    private companion object {
        const val TAG = "AutoKit"
    }
}

/** The host's car API level, or the latest level when it isn't known yet (as in unit tests). */
public fun CarContext.carApiLevelOrLatest(): Int = try {
    carAppApiLevel
} catch (e: IllegalStateException) {
    CarAppApiLevels.getLatest()
}

/** The host's content limits from `ConstraintManager` (car API level 2+), else the conservative defaults. */
public fun CarContext.hostLimits(): CarLimits {
    if (carApiLevelOrLatest() < CarAppApiLevels.LEVEL_2) return CarLimits.Default
    return try {
        val constraints = getCarService(ConstraintManager::class.java)
        CarLimits.fromHost(
            listItems = constraints.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST),
            gridItems = constraints.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID),
            paneRows = constraints.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PANE),
        )
    } catch (e: RuntimeException) {
        CarLimits.Default
    }
}
