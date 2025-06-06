package com.cursoandroid.queermap // ¡IMPORTANTE: Asegúrate de que este paquete coincida con tu applicationId!

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        // Le decimos al runner que use HiltTestApplication para nuestras pruebas
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}