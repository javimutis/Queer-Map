package com.cursoandroid.queermap

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Esta es la clase Application custom que Hilt usará para tus tests.
// Necesita tener la misma anotación @HiltAndroidApp que tu aplicación de producción
// para que Hilt genere el código necesario.
@HiltAndroidApp
class HiltTestApplication : Application() {
    // Puedes dejarla vacía, Hilt la usará para sus propósitos de testing.
}