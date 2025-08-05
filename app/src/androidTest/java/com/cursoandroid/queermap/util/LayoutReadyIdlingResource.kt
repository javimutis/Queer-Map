// app/src/androidTest/java/com/cursoandroid/queermap/util/LayoutReadyIdlingResource.kt
package com.cursoandroid.queermap.util

import android.view.View
import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

class LayoutReadyIdlingResource(
    private val viewProvider: () -> View?,
    private val name: String = "LayoutReadyIdlingResource"
) : IdlingResource {

    @Volatile
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private val isIdle = AtomicBoolean(false)

    override fun getName(): String = name

    override fun isIdleNow(): Boolean {
        val view = viewProvider()
        val currentIdleState = view?.isLayoutRequested == false // Idle if no layout is requested or view is null
        if (isIdle.get() != currentIdleState) {
            isIdle.set(currentIdleState)
            if (currentIdleState) {
                resourceCallback?.onTransitionToIdle()
            }
        }
        return currentIdleState
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = callback
    }
}