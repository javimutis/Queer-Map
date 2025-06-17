package com.cursoandroid.queermap.util

import androidx.test.espresso.idling.CountingIdlingResource
import javax.inject.Singleton

@Singleton
object EspressoIdlingResource : IdlingResourceProvider {

    private const val RESOURCE = "GLOBAL"

    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    override fun increment() {
        countingIdlingResource.increment()
    }

    override fun decrement() {
        countingIdlingResource.decrement()
    }
}