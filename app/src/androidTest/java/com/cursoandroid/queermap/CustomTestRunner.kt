package com.cursoandroid.queermap

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDexTestRunner // <--- ¡Esta importación es clave!
import dagger.hilt.android.testing.HiltTestApplication

// Un custom runner para configurar la clase de aplicación instrumentada para las pruebas.
class CustomTestRunner : MultiDexTestRunner() {

    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}