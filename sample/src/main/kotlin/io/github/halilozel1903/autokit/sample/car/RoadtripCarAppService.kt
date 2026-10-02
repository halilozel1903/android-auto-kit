package io.github.halilozel1903.autokit.sample.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import io.github.halilozel1903.autokit.KitCarAppService

/**
 * The entry point Android Auto binds to (see the manifest). Host validation, the session and the
 * root screen come from [KitCarAppService].
 */
class RoadtripCarAppService : KitCarAppService() {
    override fun onCreateRootScreen(carContext: CarContext, intent: Intent): Screen = HomeScreen(carContext)
}
