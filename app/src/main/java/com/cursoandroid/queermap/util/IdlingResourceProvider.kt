package com.cursoandroid.queermap.util

// Interfaz para abstraer el manejo del IdlingResource
interface IdlingResourceProvider {
    fun increment()
    fun decrement()
}