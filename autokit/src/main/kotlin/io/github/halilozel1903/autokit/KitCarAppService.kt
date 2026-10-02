package io.github.halilozel1903.autokit

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator

/**
 * A `CarAppService` without the boilerplate. Implement [onCreateRootScreen]:
 *
 * ```kotlin
 * class RoadtripService : KitCarAppService() {
 *     override fun onCreateRootScreen(carContext: CarContext, intent: Intent): Screen = HomeScreen(carContext)
 * }
 * ```
 *
 * Host validation allows every host in debuggable builds (so the Desktop Head Unit and emulators
 * connect) and only the known Android Auto and Android Automotive hosts in release builds
 * ([allowedHostsRes], the Car App Library's own allowlist by default).
 */
public abstract class KitCarAppService : CarAppService() {

    /** Allow any host while the app is debuggable. Override to `false` to test the release allowlist. */
    protected open val allowAllHostsInDebug: Boolean get() = true

    /** An array resource of allowed hosts (package name and certificate digest pairs). */
    protected open val allowedHostsRes: Int get() = androidx.car.app.R.array.hosts_allowlist_sample

    /** The first screen of every session. */
    protected abstract fun onCreateRootScreen(carContext: CarContext, intent: Intent): Screen

    override fun createHostValidator(): HostValidator {
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return if (debuggable && allowAllHostsInDebug) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext).addAllowedHosts(allowedHostsRes).build()
        }
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session = KitSession { carContext, intent -> onCreateRootScreen(carContext, intent) }
}

/**
 * A `Session` that shows the screen [rootScreen] creates. Subclass it to handle `onNewIntent` (deep
 * links) or configuration changes.
 */
public open class KitSession(
    private val rootScreen: (carContext: CarContext, intent: Intent) -> Screen,
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen = rootScreen(carContext, intent)
}
