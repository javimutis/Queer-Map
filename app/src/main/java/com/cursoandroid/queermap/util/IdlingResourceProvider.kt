// app/src/main/java/com/cursoandroid/queermap/util/IdlingResourceProvider.kt
package com.cursoandroid.queermap.util

// Interfaz para abstraer el manejo del IdlingResource
interface IdlingResourceProvider {
    fun increment()
    fun decrement()
}