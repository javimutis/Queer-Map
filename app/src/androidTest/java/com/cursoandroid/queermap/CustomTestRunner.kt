package com.cursoandroid.queermap

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication // Esta importación es la CLAVE

class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        // Asegúrate de que HiltTestApplication se use para las pruebas.
        // Esta línea NO DEBE CAMBIAR, porque se refiere a la clase que Hilt usará para los tests.
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}