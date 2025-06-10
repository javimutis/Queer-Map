// com.cursoandroid.queermap.util.EspressoIdlingResource.kt
package com.cursoandroid.queermap.util

import androidx.test.espresso.idling.CountingIdlingResource

object EspressoIdlingResource : IdlingResourceProvider { // <-- Debe ser un object e implementar la interfaz

    private const val RESOURCE = "GLOBAL"

    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    override fun increment() {
        countingIdlingResource.increment()
    }

    override fun decrement() {
        countingIdlingResource.decrement()
    }
}