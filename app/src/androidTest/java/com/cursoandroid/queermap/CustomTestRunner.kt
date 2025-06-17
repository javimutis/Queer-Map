package com.cursoandroid.queermap // ESTE PAQUETE DEBE SER EL applicationID

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        // Asegúrate de que HiltTestApplication es la que se usa para la aplicación.
        // Si HiltTestApplication está en el mismo paquete que la aplicación principal,
        // esta línea es correcta. Si la moviste al paquete 'test', tendrías que cambiarla,
        // pero Hilt usualmente espera que HiltTestApplication esté en el paquete principal.
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}