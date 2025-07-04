package com.cursoandroid.queermap.util

import android.view.View
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import androidx.test.espresso.matcher.ViewMatchers.*
import java.util.concurrent.TimeoutException

fun waitFor(millis: Long): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()
        override fun getDescription(): String = "Espera $millis milisegundos"
        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadForAtLeast(millis)
        }
    }
}

fun waitForViewToBeClickable(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> = allOf(isDisplayed(), isEnabled(), isClickable())
        override fun getDescription() = "Espera que la vista esté visible, habilitada y lista para click"
        override fun perform(uiController: UiController, view: View) {
            val timeout = 5000L
            val interval = 100L
            var waited = 0L
            while (!(view.isShown && view.isClickable && view.isEnabled) && waited < timeout) {
                uiController.loopMainThreadForAtLeast(interval)
                waited += interval
            }
            if (!(view.isShown && view.isClickable && view.isEnabled)) {
                throw PerformException.Builder()
                    .withViewDescription("View with id ${view.id}")
                    .withCause(TimeoutException("La vista no estaba lista para click en $timeout ms"))
                    .build()
            }
        }
    }
}

fun waitUntilVisibleAndEnabledAndCompletelyDisplayed(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> = allOf(isDisplayed(), isEnabled(), isCompletelyDisplayed())
        override fun getDescription() = "Espera hasta que la vista esté visible, habilitada y completamente dibujada"
        override fun perform(uiController: UiController, view: View) {
            val timeout = 5000L
            val interval = 50L
            var waited = 0L
            while (!getConstraints().matches(view) && waited < timeout) {
                uiController.loopMainThreadForAtLeast(interval)
                waited += interval
            }
            if (!getConstraints().matches(view)) {
                throw PerformException.Builder()
                    .withActionDescription(description)
                    .withViewDescription(view.toString())
                    .withCause(TimeoutException("La vista no estaba lista en $timeout ms."))
                    .build()
            }
        }
    }
}
