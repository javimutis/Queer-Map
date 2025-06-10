package com.cursoandroid.queermap

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// Un custom runner para configurar la clase de aplicación instrumentada para las pruebas.
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        // Asegúrate de que HiltTestApplication se use para las pruebas.
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}