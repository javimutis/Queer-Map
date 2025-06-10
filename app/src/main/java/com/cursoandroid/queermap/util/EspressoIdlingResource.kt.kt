// Queermap/util/EspressoIdlingResource.kt
package com.cursoandroid.queermap.util

import android.util.Log
import androidx.test.espresso.idling.CountingIdlingResource

object EspressoIdlingResource {

    private const val RESOURCE = "GLOBAL"
    private const val TAG = "IdlingResourceLog"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
        Log.d(TAG, "EspressoIdlingResource: INCREMENTADO. ¿Ahora inactivo? ${countingIdlingResource.isIdleNow}")
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
            Log.d(TAG, "EspressoIdlingResource: DECREMENTADO. ¿Ahora inactivo? ${countingIdlingResource.isIdleNow}")
        } else {
            Log.w(TAG, "EspressoIdlingResource: Intento de DECREMENTAR cuando ya está inactivo (contador es 0).")
        }
    }
}