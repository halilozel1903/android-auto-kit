package io.github.halilozel1903.autokit

import android.os.Looper
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.Action
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.testing.TestAppManager
import androidx.car.app.testing.TestCarContext
import androidx.car.app.testing.TestScreenManager
import io.github.halilozel1903.autokit.core.CarList
import io.github.halilozel1903.autokit.core.CarRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class KitScreenTest {
    private val carContext = TestCarContext.createCarContext(RuntimeEnvironment.getApplication())
    private val screenManager = carContext.getCarService(TestScreenManager::class.java)

    private class StopsScreen(carContext: androidx.car.app.CarContext, private val count: Int) : KitScreen(carContext) {
        override fun onCreateTemplate(): Template =
            templates.list(CarList("Stops", List(count) { CarRow("s$it", "Stop $it") }), headerAction) { row ->
                push(StopsScreen(carContext, count = 1))
                toast("Opening ${row.title}")
            }
    }

    private class BrokenScreen(carContext: androidx.car.app.CarContext) : KitScreen(carContext) {
        override fun onCreateTemplate(): Template = error("Server down")
    }

    @Test
    fun buildsTheTemplateWithTheHostDefaults() {
        val template = StopsScreen(carContext, count = 3).onGetTemplate() as ListTemplate
        assertEquals(3, template.singleList?.items?.size)
        // Not pushed onto a stack: the root screen shows the app icon.
        assertEquals(Action.TYPE_APP_ICON, template.header?.startHeaderAction?.type)
    }

    @Test
    fun rowClickPushesTheNextScreen() {
        val screen = StopsScreen(carContext, count = 2)
        screenManager.push(screen)
        val template = screen.onGetTemplate() as ListTemplate
        val row = template.singleList!!.items[0] as Row
        row.onClickDelegate!!.sendClick(TestCallback)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(screenManager.screensPushed.last() is StopsScreen)
        assertEquals(2, screenManager.screensPushed.size)
        assertEquals(listOf("Opening Stop 0"), carContext.getCarService(TestAppManager::class.java).toastsShown.map { it.toString() })
    }

    @Test
    fun exceptionsBecomeAnErrorMessage() {
        val template = BrokenScreen(carContext).onGetTemplate()
        assertTrue(template is MessageTemplate)
        assertEquals("Something went wrong", (template as MessageTemplate).header?.title.toString())
    }

    @Test
    fun longListsArePagedByCarListScreen() {
        val screen = CarListScreen(carContext, content = { CarList("Guides", List(9) { CarRow("g$it", "Guide $it") }) })
        val template = screen.onGetTemplate() as ListTemplate
        val rows = template.singleList!!.items.map { it as Row }
        assertTrue(rows.size <= screen.limits.maxListItems)
        if (screen.limits.maxListItems < 9) assertEquals("More", rows.last().title.toString())
    }

    private object TestCallback : OnDoneCallback
}
